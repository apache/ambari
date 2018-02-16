#!/usr/bin/env python

'''
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
'''
import ambari_simplejson as json
import logging
import os
from ambari_commons import subprocess32
import pprint
import threading
import platform
from threading import Thread
import time
from BackgroundCommandExecutionHandle import BackgroundCommandExecutionHandle
from resource_management.libraries.functions.log_process_information import log_process_information
from ambari_commons.os_check import OSConst, OSCheck
from Grep import Grep
import sys
from ambari_commons import shell
from ambari_commons.shell import shellRunner


logger = logging.getLogger()

class PythonExecutor(object):
  """
  Performs functionality for executing python scripts.
  Warning: class maintains internal state. As a result, instances should not be
  used as a singleton for a concurrent execution of python scripts
  """
  NO_ERROR = "none"

  def __init__(self, tmpDir, config):
    self.grep = Grep()
    self.event = threading.Event()
    self.python_process_has_been_killed = False
    self.tmpDir = tmpDir
    self.config = config
    pass


  def open_subprocess32_files(self, tmpoutfile, tmperrfile, override_output_files, backup_log_files = True):
    if override_output_files: # Recreate files, existing files are backed up if backup_log_files is True
      if backup_log_files:
        self.back_up_log_file_if_exists(tmpoutfile)
        self.back_up_log_file_if_exists(tmperrfile)
      tmpout =  open(tmpoutfile, 'w')
      tmperr =  open(tmperrfile, 'w')
    else: # Append to files
      tmpout =  open(tmpoutfile, 'a')
      tmperr =  open(tmperrfile, 'a')
    return tmpout, tmperr

  def back_up_log_file_if_exists(self, file_path):
    if os.path.isfile(file_path):
      counter = 0
      while True:
        # Find backup name that is not used yet (saves logs
        # from multiple command retries)
        backup_name = file_path + "." + str(counter)
        if not os.path.isfile(backup_name):
          break
        counter += 1
      os.rename(file_path, backup_name)

  def run_file(self, script, script_params, tmpoutfile, tmperrfile,
               timeout, tmpstructedoutfile, callback, task_id,
               override_output_files = True, backup_log_files = True, handle = None,
               log_info_on_failure = True):
    """
    Executes the specified python file in a separate subprocess32.
    Method returns only when the subprocess32 is finished.
    Params arg is a list of script parameters
    Timeout meaning: how many seconds should pass before script execution
    is forcibly terminated
    override_output_files option defines whether stdout/stderr files will be
    recreated or appended.
    The structured out file, however, is preserved during multiple invocations that use the same file.
    """
    pythonCommand = self.python_command(script, script_params)
    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("Running command %s", pprint.pformat(pythonCommand))

    if handle is None:
      tmpout, tmperr = self.open_subprocess32_files(tmpoutfile, tmperrfile, override_output_files, backup_log_files)

      process = self.launch_python_subprocess32(pythonCommand, tmpout, tmperr)
      # map task_id to pid
      callback(task_id, process.pid)
      logger.debug("Launching watchdog thread")
      self.event.clear()
      self.python_process_has_been_killed = False
      thread = Thread(target =  self.python_watchdog_func, args = (process, timeout))
      thread.start()
      # Waiting for the process to be either finished or killed
      process.communicate()
      self.event.set()
      thread.join()
      result = self.prepare_process_result(process.returncode, tmpoutfile, tmperrfile, tmpstructedoutfile, timeout=timeout)

      if log_info_on_failure and result['exitcode']:
        self.on_failure(pythonCommand, result)

      return result
    else:
      holder = Holder(pythonCommand, tmpoutfile, tmperrfile, tmpstructedoutfile, handle)

      background = BackgroundThread(holder, self)
      background.start()
      return {"exitcode": 777}

  def on_failure(self, pythonCommand, result):
    """
    Log some useful information after task failure.
    """
    pass
    #logger.info("Command %s failed with exitcode=%s", pprint.pformat(pythonCommand), result['exitcode'])
    #log_process_information(logger)

  def prepare_process_result(self, returncode, tmpoutfile, tmperrfile, tmpstructedoutfile, timeout=None):
    out, error, structured_out = self.read_result_from_files(tmpoutfile, tmperrfile, tmpstructedoutfile)

    if self.python_process_has_been_killed:
      error = str(error) + "\n Python script has been killed due to timeout" + \
              (" after waiting %s secs" % str(timeout) if timeout else "")
      returncode = 999
    result = self.condenseOutput(out, error, returncode, structured_out)
    logger.debug("Result: %s", result)
    return result

  def read_result_from_files(self, out_path, err_path, structured_out_path):
    out = open(out_path, 'r').read()
    error = open(err_path, 'r').read()
    try:
      with open(structured_out_path, 'r') as fp:
        structured_out = json.load(fp)
    except Exception:
      if os.path.exists(structured_out_path):
        errMsg = 'Unable to read structured output from ' + structured_out_path
        structured_out = {
          'msg' : errMsg
        }
        logger.warn(structured_out)
      else:
        structured_out = {}
    return out, error, structured_out

  def preexec_fn(self):
    os.setpgid(0, 0)

  def launch_python_subprocess32(self, command, tmpout, tmperr):
    """
    Creates subprocess32 with given parameters. This functionality was moved to separate method
    to make possible unit testing
    """
    close_fds = None if OSCheck.get_os_family() == OSConst.WINSRV_FAMILY else True
    command_env = dict(os.environ)
    if OSCheck.get_os_family() == OSConst.WINSRV_FAMILY:
      command_env["PYTHONPATH"] = os.pathsep.join(sys.path)
      for k, v in command_env.iteritems():
        command_env[k] = str(v)

    return subprocess32.Popen(command,
      stdout=tmpout,
      stderr=tmperr, close_fds=close_fds, env=command_env, preexec_fn=self.preexec_fn)

  def isSuccessfull(self, returncode):
    return not self.python_process_has_been_killed and returncode == 0

  def python_command(self, script, script_params):
    #we need manually pass python executable on windows because sys.executable will return service wrapper
    python_binary = os.environ['PYTHON_EXE'] if 'PYTHON_EXE' in os.environ else sys.executable
    python_command = [python_binary, script] + script_params
    return python_command

  def condenseOutput(self, stdout, stderr, retcode, structured_out):
    log_lines_count = self.config.get('heartbeat', 'log_lines_count')

    result = {
      "exitcode": retcode,
      "stdout": self.grep.tail(stdout, log_lines_count) if log_lines_count else stdout,
      "stderr": self.grep.tail(stderr, log_lines_count) if log_lines_count else stderr,
      "structuredOut" : structured_out
    }

    return result

  def python_watchdog_func(self, python, timeout):
    self.event.wait(timeout)
    if python.returncode is None:
      logger.error("subprocess32 timed out and will be killed")
      shell.kill_process_with_children(python.pid)
      self.python_process_has_been_killed = True
    pass

class Holder:
  def __init__(self, command, out_file, err_file, structured_out_file, handle):
    self.command = command
    self.out_file = out_file
    self.err_file = err_file
    self.structured_out_file = structured_out_file
    self.handle = handle

class BackgroundThread(threading.Thread):
  def __init__(self, holder, pythonExecutor):
    threading.Thread.__init__(self)
    self.holder = holder
    self.pythonExecutor = pythonExecutor

  def run(self):
    process_out, process_err = self.pythonExecutor.open_subprocess32_files(self.holder.out_file, self.holder.err_file, True)

    logger.debug("Starting process command %s", self.holder.command)
    process = self.pythonExecutor.launch_python_subprocess32(self.holder.command, process_out, process_err)

    logger.debug("Process has been started. Pid = %s", process.pid)

    self.holder.handle.pid = process.pid
    self.holder.handle.status = BackgroundCommandExecutionHandle.RUNNING_STATUS
    self.holder.handle.on_background_command_started(self.holder.handle.command['taskId'], process.pid)

    process.communicate()

    self.holder.handle.exitCode = process.returncode
    process_condensed_result = self.pythonExecutor.prepare_process_result(process.returncode, self.holder.out_file, self.holder.err_file, self.holder.structured_out_file)
    logger.debug("Calling callback with args %s", process_condensed_result)
    self.holder.handle.on_background_command_complete_callback(process_condensed_result, self.holder.handle)
    logger.debug("Exiting from thread for holder pid %s", self.holder.handle.pid)
