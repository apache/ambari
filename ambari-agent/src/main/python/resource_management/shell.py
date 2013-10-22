import logging
import subprocess
from exceptions import Fail


def checked_call(command, log_stdout=False, 
         cwd=None, env=None, preexec_fn=None):
  return _call(command, log_stdout, True, cwd, env, preexec_fn)

def call(command, log_stdout=False, 
         cwd=None, env=None, preexec_fn=None):
  return _call(command, log_stdout, False, cwd, env, preexec_fn)
  

def _call(command, log_stdout=False, throw_on_failure=True, 
         cwd=None, env=None, preexec_fn=None):
  """
  Execute shell command
  
  @param command: list/tuple of arguments (recommended as more safe - don't need to escape) 
  or string of the command to execute
  @param log_stdout: boolean, whether command output should be logged of not
  @param throw_on_failure: if true, when return code is not zero exception is thrown
  
  @return: retrun_code, stdout, stderr
  """
  
  if isinstance(command, (list, tuple)):
    shell = False
  else:
    shell = True
  
  proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                          cwd=cwd, env=env, shell=shell,
                          preexec_fn=preexec_fn)
  out = proc.communicate()[0]
  code = proc.wait()
  
  if throw_on_failure and code:
    err_msg = ("Execution of '%s' returned %d: Error: %s") % (command, code, out)
    raise Fail(err_msg)
  
  if log_stdout:
    _log.info("%s.\n%s" % (command, out))
  
  return code, out
    
def _log():
  return logging.getLogger("resource_management.provider")