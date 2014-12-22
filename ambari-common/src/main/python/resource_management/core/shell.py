#!/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Ambari Agent

"""
import os

__all__ = ["checked_call", "call", "quote_bash_args", "as_user", "as_sudo"]

import string
import subprocess
import threading
from multiprocessing import Queue
from exceptions import Fail
from exceptions import ExecuteTimeoutException
from resource_management.core.logger import Logger

EXPORT_PLACEHOLDER = "[RMF_EXPORT_PLACEHOLDER]"
ENV_PLACEHOLDER = "[RMF_ENV_PLACEHOLDER]"

PLACEHOLDERS_TO_STR = {
  EXPORT_PLACEHOLDER: "export {env_str} > /dev/null ; ",
  ENV_PLACEHOLDER: "{env_str}"
}

def checked_call(command, verbose=False, logoutput=False,
         cwd=None, env=None, preexec_fn=None, user=None, wait_for_finish=True, timeout=None, path=None, output_file=None, sudo=False):
  """
  Execute the process and throw an exception on failure.
  @return: return_code, stdout
  """
  return _call(command, verbose, logoutput, True, cwd, env, preexec_fn, user, wait_for_finish, timeout, path, output_file, sudo)

def call(command, verbose=False, logoutput=False,
         cwd=None, env=None, preexec_fn=None, user=None, wait_for_finish=True, timeout=None, path=None, output_file=None, sudo=False):
  """
  Execute the process despite failures.
  @return: return_code, stdout
  """
  return _call(command, verbose, logoutput, False, cwd, env, preexec_fn, user, wait_for_finish, timeout, path, output_file, sudo)

def _call(command, verbose=False, logoutput=False, throw_on_failure=True,
         cwd=None, env=None, preexec_fn=None, user=None, wait_for_finish=True, timeout=None, path=None, output_file=None, sudo=False):
  """
  Execute shell command
  
  @param command: list/tuple of arguments (recommended as more safe - don't need to escape) 
  or string of the command to execute
  @param logoutput: boolean, whether command output should be logged of not
  @param throw_on_failure: if true, when return code is not zero exception is thrown
  
  @return: return_code, stdout
  """

  command_alias = string_cmd_from_args_list(command) if isinstance(command, (list, tuple)) else command
  
  # Append current PATH to env['PATH']
  env = add_current_path_to_env(env)
  # Append path to env['PATH']
  if path:
    path = os.pathsep.join(path) if isinstance(path, (list, tuple)) else path
    env['PATH'] = os.pathsep.join([env['PATH'], path])

  # prepare command cmd
  if sudo:
    command = as_sudo(command, env=env)
  elif user:
    command = as_user(command, user, env=env)
    
  # convert to string and escape
  if isinstance(command, (list, tuple)):
    command = string_cmd_from_args_list(command)
    
  # replace placeholder from as_sudo / as_user if present
  env_str = get_environment_str(env)
  for placeholder, replacement in PLACEHOLDERS_TO_STR.iteritems():
    command = command.replace(placeholder, replacement.format(env_str=env_str))
  
  if verbose:
    Logger.info("Running: " + command)

  # --noprofile is used to preserve PATH set for ambari-agent
  subprocess_command = ["/bin/bash","--login","--noprofile","-c", command]
  proc = subprocess.Popen(subprocess_command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                          cwd=cwd, env=env, shell=False,
                          preexec_fn=preexec_fn)

  if not wait_for_finish:
    return None, None
  
  if timeout:
    q = Queue()
    t = threading.Timer( timeout, on_timeout, [proc, q] )
    t.start()
    
  out = proc.communicate()[0].strip('\n')
  
  if timeout:
    if q.empty():
      t.cancel()
    # timeout occurred
    else:
      raise ExecuteTimeoutException()
   
  code = proc.returncode
  
  if logoutput and out:
    Logger.info(out)
  
  if throw_on_failure and code:
    err_msg = Logger.filter_text(("Execution of '%s' returned %d. %s") % (command_alias, code, out))
    raise Fail(err_msg)
  
  return code, out

def as_sudo(command, env=None):
  """
  command - list or tuple of arguments.
  env - when run as part of Execute resource, this SHOULD NOT be used.
  It automatically gets replaced later by call, checked_call. This should be used in not_if, only_if
  """
  if isinstance(command, (list, tuple)):
    command = string_cmd_from_args_list(command)
  else:
    # Since ambari user sudoer privileges may be restricted,
    # without having /bin/bash permission, and /bin/su permission.
    # Running interpreted shell commands in scope of 'sudo' is not possible.
    #   
    # In that case while passing string,
    # any bash symbols eventually added to command like && || ; < > | << >> would cause problems.
    err_msg = Logger.filter_text(("String command '%s' cannot be run as sudo. Please supply the command as a tuple of arguments") % (command))
    raise Fail(err_msg)

  env = get_environment_str(add_current_path_to_env(env)) if env else ENV_PLACEHOLDER
  return "/usr/bin/sudo {0} -H -E {1}".format(env, command)

def as_user(command, user, env=None):
  if isinstance(command, (list, tuple)):
    command = string_cmd_from_args_list(command)

  export_env = "export {0} ; ".format(get_environment_str(add_current_path_to_env(env))) if env else EXPORT_PLACEHOLDER
  return "/usr/bin/sudo su {0} -l -s /bin/bash -c {1}".format(user, quote_bash_args(export_env + command))

def add_current_path_to_env(env):
  result = {} if not env else env
  
  if not 'PATH' in result:
    result['PATH'] = os.environ['PATH']
    
  # don't append current env if already there
  if not set(os.environ['PATH'].split(os.pathsep)).issubset(result['PATH'].split(os.pathsep)):
    result['PATH'] = os.pathsep.join([os.environ['PATH'], result['PATH']])
  
  return result
  
def get_environment_str(env):
  return reduce(lambda str,x: '{0} {1}={2}'.format(str,x,quote_bash_args(env[x])), env, '')

def string_cmd_from_args_list(command):
  return ' '.join(quote_bash_args(x) for x in command)

def on_timeout(proc, q):
  q.put(True)
  if proc.poll() == None:
    try:
      proc.terminate()
    except:
      pass

def quote_bash_args(command):
  if not command:
    return "''"
  valid = set(string.ascii_letters + string.digits + '@%_-+=:,./')
  for char in command:
    if char not in valid:
      return "'" + command.replace("'", "'\"'\"'") + "'"
  return command