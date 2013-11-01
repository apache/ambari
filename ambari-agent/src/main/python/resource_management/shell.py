import logging
import subprocess
from exceptions import Fail

log = logging.getLogger("resource_management.provider")

def checked_call(command, logoutput=False, 
         cwd=None, env=None, preexec_fn=None):
  return _call(command, logoutput, True, cwd, env, preexec_fn)

def call(command, logoutput=False, 
         cwd=None, env=None, preexec_fn=None):
  return _call(command, logoutput, False, cwd, env, preexec_fn)
  

def _call(command, logoutput=False, throw_on_failure=True, 
         cwd=None, env=None, preexec_fn=None):
  """
  Execute shell command
  
  @param command: list/tuple of arguments (recommended as more safe - don't need to escape) 
  or string of the command to execute
  @param logoutput: boolean, whether command output should be logged of not
  @param throw_on_failure: if true, when return code is not zero exception is thrown
  
  @return: retrun_code, stdout
  """
  
  shell = not isinstance(command, (list, tuple))
  
  proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                          cwd=cwd, env=env, shell=shell,
                          preexec_fn=preexec_fn)
  
  out = proc.communicate()[0]
  code = proc.returncode
  
  if logoutput and out and out!="":
    log.info(out)
  
  if throw_on_failure and code:
    err_msg = ("Execution of '%s' returned %d. %s") % (command, code, out)
    raise Fail(err_msg)
  
  return code, out