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

__all__ = ["non_blocking_call", "checked_call", "call", "quote_bash_args", "as_user", "as_sudo"]

import os
import select
import sys
import logging
import string
import inspect
import subprocess
import threading
import traceback
from exceptions import Fail
from exceptions import ExecuteTimeoutException
from resource_management.core.logger import Logger
from ambari_commons.constants import AMBARI_SUDO_BINARY

# use quiet=True calls from this folder (logs get too messy duplicating the resources with its commands)
RMF_FOLDER = 'resource_management/'
EXPORT_PLACEHOLDER = "[RMF_EXPORT_PLACEHOLDER]"
ENV_PLACEHOLDER = "[RMF_ENV_PLACEHOLDER]"

PLACEHOLDERS_TO_STR = {
  EXPORT_PLACEHOLDER: "export {env_str} > /dev/null ; ",
  ENV_PLACEHOLDER: "{env_str}"
}

def log_function_call(function):
  def inner(command, **kwargs):
    caller_filename = inspect.getouterframes(inspect.currentframe())[1][1]
    # quiet = can be False/True or None -- which means undefined yet
    quiet = kwargs['quiet'] if 'quiet' in kwargs else None
    is_internal_call = RMF_FOLDER in caller_filename
    
    if quiet == False or (quiet == None and not is_internal_call):
      command_alias = string_cmd_from_args_list(command) if isinstance(command, (list, tuple)) else command
      log_msg = Logger.get_function_repr("{0}['{1}']".format(function.__name__, command_alias), kwargs)
      Logger.info(log_msg)
      
    # logoutput=False - never log
    # logoutput=True - log in INFO level
    # logouput=None - log in DEBUG level
    # logouput=not-specified - log in DEBUG level, not counting internal calls
    kwargs['logoutput'] = ('logoutput' in kwargs and kwargs['logoutput'] and Logger.logger.isEnabledFor(logging.INFO)) or \
      ('logoutput' in kwargs and kwargs['logoutput']==None and Logger.logger.isEnabledFor(logging.DEBUG)) or \
      (not 'logoutput' in kwargs and not is_internal_call and Logger.logger.isEnabledFor(logging.DEBUG))
       
    result = function(command, **kwargs)
    
    if quiet == False or (quiet == None and not is_internal_call):
      log_msg = "{0} returned {1}".format(function.__name__, result)
      Logger.info(log_msg)
      
    return result
    
  return inner

@log_function_call
def checked_call(command, quiet=False, logoutput=None,
         cwd=None, env=None, preexec_fn=None, user=None, wait_for_finish=True, timeout=None, path=None, sudo=False, on_new_line=None):
  """
  Execute the shell command and throw an exception on failure.
  @throws Fail
  @return: return_code, output
  """
  return _call(command, logoutput, True, cwd, env, preexec_fn, user, wait_for_finish, timeout, path, sudo, on_new_line)

@log_function_call
def call(command, quiet=False, logoutput=None,
         cwd=None, env=None, preexec_fn=None, user=None, wait_for_finish=True, timeout=None, path=None, sudo=False, on_new_line=None):
  """
  Execute the shell command despite failures.
  @return: return_code, output
  """
  return _call(command, logoutput, False, cwd, env, preexec_fn, user, wait_for_finish, timeout, path, sudo, on_new_line)

@log_function_call
def non_blocking_call(command, quiet=False,
         cwd=None, env=None, preexec_fn=None, user=None, timeout=None, path=None, sudo=False):
  """
  Execute the shell command and don't wait until it's completion
  
  @return: process object -- Popen instance 
  (use proc.stdout.readline to read output in cycle, don't foget to proc.stdout.close(),
  to get return code use proc.wait() and after that proc.returncode)
  """
  return _call(command, False, True, cwd, env, preexec_fn, user, False, timeout, path, sudo, None)

def _call(command, logoutput=None, throw_on_failure=True,
         cwd=None, env=None, preexec_fn=None, user=None, wait_for_finish=True, timeout=None, path=None, sudo=False, on_new_line=None):
  """
  Execute shell command
  
  @param command: list/tuple of arguments (recommended as more safe - don't need to escape) 
  or string of the command to execute
  @param logoutput: boolean, whether command output should be logged of not
  @param throw_on_failure: if true, when return code is not zero exception is thrown
  """

  command_alias = string_cmd_from_args_list(command) if isinstance(command, (list, tuple)) else command
  
  # Append current PATH to env['PATH']
  env = _add_current_path_to_env(env)
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
  env_str = _get_environment_str(env)
  for placeholder, replacement in PLACEHOLDERS_TO_STR.iteritems():
    command = command.replace(placeholder, replacement.format(env_str=env_str))

  import pty
  master_fd, slave_fd = pty.openpty()

  # --noprofile is used to preserve PATH set for ambari-agent
  subprocess_command = ["/bin/bash","--login","--noprofile","-c", command]
  proc = subprocess.Popen(subprocess_command, bufsize=1, stdout=slave_fd, stderr=subprocess.STDOUT,
                          cwd=cwd, env=env, shell=False, close_fds=True,
                          preexec_fn=preexec_fn)
  
  if timeout:
    timeout_event = threading.Event()
    t = threading.Timer( timeout, _on_timeout, [proc, timeout_event] )
    t.start()
    
  if not wait_for_finish:
    return proc
    
  # in case logoutput==False, never log.    
  logoutput = logoutput==True and Logger.logger.isEnabledFor(logging.INFO) or logoutput==None and Logger.logger.isEnabledFor(logging.DEBUG)
  out = ""
  read_timeout = .04 # seconds

  try:
    while True:
      ready, _, _ = select.select([master_fd], [], [], read_timeout)
      if ready:
        line = os.read(master_fd, 512)
        if not line:
            break
          
        out += line
        try:
          if on_new_line:
            on_new_line(line)
        except Exception, err:
          err_msg = "Caused by on_new_line function failed with exception for input argument '{0}':\n{1}".format(line, traceback.format_exc())
          raise Fail(err_msg)
          
        if logoutput:
          _print(line)    
      elif proc.poll() is not None:
        break # proc exited
  finally:
    os.close(slave_fd)
    os.close(master_fd)

  proc.wait()  
  out = out.strip('\n')
  
  if timeout: 
    if not timeout_event.is_set():
      t.cancel()
    # timeout occurred
    else:
      raise ExecuteTimeoutException()
   
  code = proc.returncode
  
  if throw_on_failure and code:
    err_msg = Logger.filter_text(("Execution of '%s' returned %d. %s") % (command_alias, code, out))
    raise Fail(err_msg)
  
  return code, out

def as_sudo(command, env=None, auto_escape=True):
  """
  command - list or tuple of arguments.
  env - when run as part of Execute resource, this SHOULD NOT be used.
  It automatically gets replaced later by call, checked_call. This should be used in not_if, only_if
  """
  if isinstance(command, (list, tuple)):
    command = string_cmd_from_args_list(command, auto_escape=auto_escape)
  else:
    # Since ambari user sudoer privileges may be restricted,
    # without having /bin/bash permission, and /bin/su permission.
    # Running interpreted shell commands in scope of 'sudo' is not possible.
    #   
    # In that case while passing string,
    # any bash symbols eventually added to command like && || ; < > | << >> would cause problems.
    err_msg = Logger.filter_text(("String command '%s' cannot be run as sudo. Please supply the command as a tuple of arguments") % (command))
    raise Fail(err_msg)

  env = _get_environment_str(_add_current_path_to_env(env)) if env else ENV_PLACEHOLDER
  return "{0} {1} -H -E {2}".format(_get_sudo_binary(), env, command)

def as_user(command, user, env=None, auto_escape=True):
  if isinstance(command, (list, tuple)):
    command = string_cmd_from_args_list(command, auto_escape=auto_escape)

  export_env = "export {0} ; ".format(_get_environment_str(_add_current_path_to_env(env))) if env else EXPORT_PLACEHOLDER
  return "{0} su {1} -l -s /bin/bash -c {2}".format(_get_sudo_binary(), user, quote_bash_args(export_env + command))

def quote_bash_args(command):
  if not command:
    return "''"
  valid = set(string.ascii_letters + string.digits + '@%_-+=:,./')
  for char in command:
    if char not in valid:
      return "'" + command.replace("'", "'\"'\"'") + "'"
  return command

def _add_current_path_to_env(env):
  result = {} if not env else env
  
  if not 'PATH' in result:
    result['PATH'] = os.environ['PATH']
    
  # don't append current env if already there
  if not set(os.environ['PATH'].split(os.pathsep)).issubset(result['PATH'].split(os.pathsep)):
    result['PATH'] = os.pathsep.join([os.environ['PATH'], result['PATH']])
  
  return result

def _get_sudo_binary():
  return AMBARI_SUDO_BINARY
  
def _get_environment_str(env):
  return reduce(lambda str,x: '{0} {1}={2}'.format(str,x,quote_bash_args(env[x])), env, '')

def string_cmd_from_args_list(command, auto_escape=True):
  escape_func = lambda x:quote_bash_args(x) if auto_escape else lambda x:x
  return ' '.join(escape_func(x) for x in command)

def _on_timeout(proc, timeout_event):
  timeout_event.set()
  if proc.poll() == None:
    try:
      proc.terminate()
    except:
      pass
    
def _print(line):
  sys.stdout.write(line)
  sys.stdout.flush()