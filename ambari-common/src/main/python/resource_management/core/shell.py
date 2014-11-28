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

SUDO_ENVIRONMENT_PLACEHOLDER = "{ENV_PLACEHOLDER}"

def checked_call(command, logoutput=False, 
         cwd=None, env={}, preexec_fn=None, user=None, wait_for_finish=True, timeout=None, path=None, output_file=None, sudo=False):
  return _call(command, logoutput, True, cwd, env, preexec_fn, user, wait_for_finish, timeout, path, output_file, sudo)

def call(command, logoutput=False, 
         cwd=None, env={}, preexec_fn=None, user=None, wait_for_finish=True, timeout=None, path=None, output_file=None, sudo=False):
  return _call(command, logoutput, False, cwd, env, preexec_fn, user, wait_for_finish, timeout, path, output_file, sudo)
            
def _call(command, logoutput=False, throw_on_failure=True, 
         cwd=None, env={}, preexec_fn=None, user=None, wait_for_finish=True, timeout=None, path=None, output_file=None, sudo=False):
  """
  Execute shell command
  
  @param command: list/tuple of arguments (recommended as more safe - don't need to escape) 
  or string of the command to execute
  @param logoutput: boolean, whether command output should be logged of not
  @param throw_on_failure: if true, when return code is not zero exception is thrown
  
  @return: retrun_code, stdout
  """
  # convert to string and escape
  if isinstance(command, (list, tuple)):
    command = string_cmd_from_args_list(command)
  elif sudo:
    # Since ambari user sudoer privileges may be restricted,
    # without having /bin/bash permission.
    # Running interpreted shell commands in scope of 'sudo' is not possible.
    #   
    # In that case while passing string,
    # any bash symbols eventually added to command like && || ; < > | << >> would cause problems.
    #
    # In case of need to create more complicated commands with sudo use as_sudo(command) function.
    err_msg = Logger.get_protected_text(("String command '%s' cannot be run as sudo. Please supply the command as a tuple of arguments") % (command))
    raise Fail(err_msg)

  # In case we will use sudo, we have to put all the environment inside the command, 
  # since Popen environment gets reset within sudo.
  environment_str = reduce(lambda str,x: '{0} {1}={2}'.format(str,x,quote_bash_args(env[x])), env,'')
  command = command.replace(SUDO_ENVIRONMENT_PLACEHOLDER, environment_str, 1) # replace placeholder from as_sudo / as_user if present
   
  bash_run_command = command if not sudo else "/usr/bin/sudo {0} -Hi {1}".format(environment_str, command)
  
  if user:
    # Outter environment gets reset within su. That's why we can't use environment passed to Popen.
    su_export_command = "export {0} ; ".format(environment_str) if environment_str else ""
    subprocess_command = ["/usr/bin/sudo","-Hi","su", "-", user, "-s", "/bin/bash", "-c", su_export_command + bash_run_command]
  else:
    subprocess_command = ["/bin/bash","--login","-c", bash_run_command]
    
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
    err_msg = Logger.get_protected_text(("Execution of '%s' returned %d. %s") % (command, code, out))
    raise Fail(err_msg)
  
  return code, out

def as_sudo(command, env=SUDO_ENVIRONMENT_PLACEHOLDER):
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
    err_msg = Logger.get_protected_text(("String command '%s' cannot be run as sudo. Please supply the command as a tuple/list of arguments") % (command))
    raise Fail(err_msg)
  
  if env != SUDO_ENVIRONMENT_PLACEHOLDER:
    env = reduce(lambda str,x: '{0} {1}={2}'.format(str,x,quote_bash_args(env[x])), env, '')
  
  return "/usr/bin/sudo {0} -Hi {1}".format(env, command)

def as_user(command, user , env=SUDO_ENVIRONMENT_PLACEHOLDER):
  if isinstance(command, (list, tuple)):
    command = string_cmd_from_args_list(command)
    
  if env != SUDO_ENVIRONMENT_PLACEHOLDER:
    env = reduce(lambda str,x: '{0} {1}={2}'.format(str,x,quote_bash_args(env[x])), env, '')
    
  export_command = "export {0} ; ".format(env)
  
  result_command = "/usr/bin/sudo -Hi su - {0} -s /bin/bash -c {1}".format(user, quote_bash_args(export_command + command))
  return result_command

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