# !/usr/bin/env python

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
"""

import logging
import os
import signal
from ambari_commons import subprocess32
import threading
from contextlib import contextmanager

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl
from resource_management.core import sudo

logger = logging.getLogger()

threadLocal = threading.local()

# default timeout for async invoked processes
TIMEOUT_SECONDS = 300

tempFiles = []


def noteTempFile(filename):
  tempFiles.append(filename)


def getTempFiles():
  return tempFiles


class _dict_to_object:
  def __init__(self, entries):
    self.__dict__.update(entries)

  def __getitem__(self, item):
    return self.__dict__[item]


# windows specific code
@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def kill_process_with_children(parent_pid):
  shellRunnerWindows().run(["taskkill", "/F", "/T", "/PID", "{0}".format(parent_pid)])


class shellRunner(object):
  def run(self, script, user=None):
    pass

  def runPowershell(self, file=None, script_block=None, args=[]):
    raise NotImplementedError()


def launch_subprocess(command):
  """
  Process launch helper

  :param command Command to execute
  :type command list[str]|str
  :return Popen object
  """
  is_shell = not isinstance(command, (list, tuple))
  return subprocess32.Popen(command, stdout=subprocess32.PIPE, stderr=subprocess32.PIPE, shell=is_shell, close_fds=True)


def watchdog_func(event, cmd, exec_timeout):
  """
  Watchdog function for subprocess executors

  :type event Event
  :type cmd Popen
  :type exec_timeout int


  Usage example:
      event = threading.Event()

      cmd = Popen(...)

      thread = threading.Thread(target=watchdog_func, args=(event, cmd, execution_timeout,))
      thread.start()

      ....cmd.communicate() or any another processing....

      event.set()
      thread.join()
      ......result code....
  """
  event.wait(exec_timeout)
  if cmd.returncode is None:
    logger.error("Task timed out and will be killed")
    kill_process_with_children(cmd.pid)


def subprocess_with_timeout(command, execution_timeout=None):
  """
  Run command with limited time for execution, after timeout command would be killed

  :param command Command to execute
  :param execution_timeout execution time limit in seconds. Defaulting to TIMEOUT_SECONDS global constant

  :type command list[str]|str
  :type execution_timeout int
  :rtype dict
  """
  event = threading.Event()

  if execution_timeout is None:
    execution_timeout = TIMEOUT_SECONDS

  os_stat = launch_subprocess(command)
  logger.debug("Launching watchdog thread")

  event.clear()

  thread = threading.Thread(target=watchdog_func, args=(event, os_stat, execution_timeout,))
  thread.start()

  out, err = os_stat.communicate()

  result = {
    "out": out,
    "err": err,
    "retCode": os_stat.returncode
  }

  event.set()
  thread.join()
  return result


@contextmanager
def process_executor(command, timeout=None, error_callback=None):
  """
  Context manager for command execution

  :type command list|str
  :type timeout None|int
  :type error_callback func

  :return stdout stream

  Usage example:

   Option 1. Basic
     with process_executor(["ls", "-la]) as stdout:
       for line in stdout:
         print line

   Option 2. Extended
     def error_handler(command, error_log, exit_code):
       print "Command '{}' failed".format(command)
       print "Exit Code: {}   StdOut: {} \n".format(exit_code, "\n".join(error_log))

     with process_executor(["ls", "-la], timeout=10, error_callback=error_handler) as stdout:
       for line in stdout:
         print line

  """
  if not timeout:
    timeout = TIMEOUT_SECONDS

  event = threading.Event()
  cmd = launch_subprocess(command)

  thread = threading.Thread(target=watchdog_func, args=(event, cmd, timeout,))
  thread.start()

  yield cmd.stdout

  exit_code = cmd.poll()
  event.set()
  thread.join()

  if exit_code is None:
    kill_process_with_children(cmd.pid)

  if error_callback and exit_code and exit_code > 0:
    error_callback(command, cmd.stderr.readlines(), exit_code)


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class shellRunnerWindows(shellRunner):
  # Run any command
  def run(self, script, user=None):
    logger.warn("user argument ignored on windows")
    code = 0
    if isinstance(script, list):
      cmd = " ".join(script)
    else:
      cmd = script
    p = subprocess32.Popen(cmd, stdout=subprocess32.PIPE,
                         stderr=subprocess32.PIPE, shell=False)
    out, err = p.communicate()
    code = p.wait()
    logger.debug("Exitcode for %s is %d" % (cmd, code))
    return {'exitCode': code, 'output': out, 'error': err}

  def runPowershell(self, file=None, script_block=None, args=[]):
    logger.warn("user argument ignored on windows")
    code = 0
    cmd = None
    if file:
      cmd = ['powershell', '-WindowStyle', 'Hidden', '-File', file] + args
    elif script_block:
      cmd = ['powershell', '-WindowStyle', 'Hidden', '-Command', script_block] + args
    p = subprocess32.Popen(cmd, stdout=subprocess32.PIPE,
                         stderr=subprocess32.PIPE, shell=False)
    out, err = p.communicate()
    code = p.wait()
    logger.debug("Exitcode for %s is %d" % (cmd, code))
    return _dict_to_object({'exitCode': code, 'output': out, 'error': err})


def get_all_children(base_pid):
  """
  Return all child PIDs of base_pid process

  :param base_pid starting PID to scan for children
  :return tuple of the following: pid, binary name, command line incl. binary

  :type base_pid int
  :rtype list[(int, str, str)]
  """
  parent_pid_path_pattern = "/proc/{0}/task/{0}/children"
  comm_path_pattern = "/proc/{0}/comm"
  cmdline_path_pattern = "/proc/{0}/cmdline"

  def read_children(pid):
    try:
      with open(parent_pid_path_pattern.format(pid), "r") as f:
        return [int(item) for item in f.readline().strip().split(" ")]
    except (IOError, ValueError):
      return []

  def read_command(pid):
    try:
      with open(comm_path_pattern.format(pid), "r") as f:
        return f.readline().strip()
    except IOError:
      return ""

  def read_cmdline(pid):
    try:
      with open(cmdline_path_pattern.format(pid), "r") as f:
        return f.readline().strip()
    except IOError:
      return ""

  pids = []
  scan_pending = [int(base_pid)]

  while scan_pending:
    curr_pid = scan_pending.pop(0)
    children = read_children(curr_pid)

    pids.append((curr_pid, read_command(curr_pid), read_cmdline(curr_pid)))
    scan_pending.extend(children)

  return pids


def is_pid_exists(pid):
  """
  Check if process with PID still exist (not counting it real state)

  :type pid int
  :rtype bool
  """
  pid_path = "/proc/{0}"
  try:
    return os.path.exists(pid_path.format(pid))
  except (OSError, IOError):
    logger.debug("Failed to check PID existence")
    return False


def get_existing_pids(pids):
  """
  Check if process with pid still exists (not counting it real state).

  Optimized to check PID list at once.

  :param pids list of PIDs to filter
  :return list of still existing PID

  :type pids list[int]
  :rtype list[int]
  """

  existing_pid_list = []

  try:
    all_existing_pid_list = [int(item) for item in os.listdir("/proc") if item.isdigit()]
  except (OSError, IOError):
    logger.debug("Failed to check PIDs existence")
    return existing_pid_list

  for pid_item in pids:
    if pid_item in all_existing_pid_list:
      existing_pid_list.append(pid_item)

  return existing_pid_list


def wait_for_process_list_kill(pids, timeout=5, check_step_time=0.1):
  """
  Process tree waiter

  :type pids list[int]
  :type timeout int|float
  :type check_step_time int|float

  :param pids list of PIDs to watch
  :param timeout how long wait till giving up, seconds. Set 0 for nowait or None for infinite time
  :param check_step_time how often scan for existing PIDs, seconds
  """
  from threading import Thread, Event
  import time

  stop_waiting = Event()

  def _wait_loop():
    while not stop_waiting.is_set() and get_existing_pids(pids):
      time.sleep(check_step_time)

  if timeout == 0:  # no need for loop if no timeout is set
    return

  th = Thread(target=_wait_loop)
  stop_waiting.clear()

  th.start()
  th.join(timeout=timeout)
  stop_waiting.set()

  th.join()


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def kill_process_with_children(base_pid):
  """
  Process tree killer

  :type base_pid int
  """
  exception_list = ["apt-get", "apt", "yum", "zypper", "zypp"]
  signals_to_post = {
    "SIGTERM": signal.SIGTERM,
    "SIGKILL": signal.SIGKILL
  }
  full_child_pids = get_all_children(base_pid)
  all_child_pids = [item[0] for item in full_child_pids if item[1].lower() not in exception_list and item[0] != os.getpid()]
  error_log = []

  for sig_name, sig in signals_to_post.items():
    # we need to kill processes from the bottom of the tree
    pids_to_kill = sorted(get_existing_pids(all_child_pids), reverse=True)
    for pid in pids_to_kill:
      try:
        sudo.kill(pid, sig)
      except OSError as e:
        error_log.append((sig_name, pid, repr(e)))

    if pids_to_kill:
      wait_for_process_list_kill(pids_to_kill)
      still_existing_pids = get_existing_pids(pids_to_kill)
      if still_existing_pids:
        logger.warn("These PIDs {0} did not respond to {1} signal. Detailed commands list:\n {2}".format(
          ", ".join([str(i) for i in still_existing_pids]),
          sig_name,
          "\n".join([i[2] for i in full_child_pids if i[0] in still_existing_pids])
        ))

  if get_existing_pids(all_child_pids) and error_log:  # we're unable to kill all requested PIDs
    logger.warn("Process termination error log:\n")
    for error_item in error_log:
      logger.warn("PID: {0}, Process: {1}, Exception message: {2}".format(*error_item))


def _changeUid():
  try:
    os.setuid(threadLocal.uid)
  except Exception:
    logger.warn("can not switch user for running command.")


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class shellRunnerLinux(shellRunner):
  # Run any command
  def run(self, script, user=None):
    import pwd

    try:
      if user != None:
        user = pwd.getpwnam(user)[2]
      else:
        user = os.getuid()
      threadLocal.uid = user
    except Exception:
      logger.warn("can not switch user for RUN_COMMAND.")

    cmd = script
    
    if isinstance(script, list):
      cmd = " ".join(script)

    cmd_list = ["/bin/bash","--login","--noprofile","-c", cmd]
    p = subprocess32.Popen(cmd_list, preexec_fn=_changeUid, stdout=subprocess32.PIPE,
                         stderr=subprocess32.PIPE, shell=False, close_fds=True)
    out, err = p.communicate()
    code = p.wait()
    logger.debug("Exitcode for %s is %d" % (cmd, code))
    return {'exitCode': code, 'output': out, 'error': err}


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def changeUid():
  #No Windows implementation
  pass

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def changeUid():
  _changeUid()
