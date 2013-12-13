from resource_management.core.exceptions import ComponentIsNotRunning
__all__ = ["check_process_status"]

import os, logging

log = logging.getLogger('resource_management')

def check_process_status(pid_file):
  """
  Function checks whether process is running.
  Process is considered running, if pid file exists, and process with
  a pid, mentioned in pid file is running
  If process is not running, will throw ComponentIsNotRunning exception

  @param pid_file: path to service pid file
  """
  if not pid_file or not os.path.isfile(pid_file):
    raise ComponentIsNotRunning()
  with open(pid_file, "r") as f:
    try:
      pid = int(f.read())
    except:
      log.debug("Pid file {0} does not exist".format(pid_file))
      raise ComponentIsNotRunning()
    try:
      # Kill will not actually kill the process
      # From the doc:
      # If sig is 0, then no signal is sent, but error checking is still
      # performed; this can be used to check for the existence of a
      # process ID or process group ID.
      os.kill(pid, 0)
    except OSError:
      log.debug("Process with pid {0} is not running. Stale pid file"
                " at {1}".format(pid, pid_file))
      raise ComponentIsNotRunning()
  pass
