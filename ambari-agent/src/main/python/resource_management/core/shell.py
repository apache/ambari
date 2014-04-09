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

__all__ = ["checked_call", "call", "quote_bash_args"]

import string
import subprocess
import threading
from multiprocessing import Queue
from .exceptions import Fail
from .exceptions import ExecuteTimeoutException
from resource_management.core.logger import Logger

def checked_call(command, logoutput=False, 
         cwd=None, env=None, preexec_fn=None, user=None, wait_for_finish=True, timeout=None):
  return _call(command, logoutput, True, cwd, env, preexec_fn, user, wait_for_finish, timeout)

def call(command, logoutput=False, 
         cwd=None, env=None, preexec_fn=None, user=None, wait_for_finish=True, timeout=None):
  return _call(command, logoutput, False, cwd, env, preexec_fn, user, wait_for_finish, timeout)
            
def _call(command, logoutput=False, throw_on_failure=True, 
         cwd=None, env=None, preexec_fn=None, user=None, wait_for_finish=True, timeout=None):
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
    command = ' '.join(quote_bash_args(x) for x in command)

  if user:
    command = ["su", "-", user, "-c", command]
  else:
    command = ["/bin/bash","--login","-c", command]

  proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                          cwd=cwd, env=env, shell=False,
                          preexec_fn=preexec_fn)

  if not wait_for_finish:
    return None, None
  
  if timeout:
    q = Queue()
    t = threading.Timer( timeout, on_timeout, [proc, q] )
    t.start()
    
  out = proc.communicate()[0].decode().strip('\n')
  
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
    err_msg = ("Execution of '%s' returned %d. %s") % (command[-1], code, out)
    raise Fail(err_msg)
  
  return code, out

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
