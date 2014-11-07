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
import json
import logging
import os
import subprocess
import pprint
import threading
from threading import Thread
import time
from BackgroundCommandExecutionHandle import BackgroundCommandExecutionHandle 

from Grep import Grep
import shell, sys


logger = logging.getLogger()

class PythonExecutor:
  """
  Performs functionality for executing python scripts.
  Warning: class maintains internal state. As a result, instances should not be
  used as a singleton for a concurrent execution of python scripts
  """
  NO_ERROR = "none"
  grep = Grep()
  event = threading.Event()
  python_process_has_been_killed = False

  def __init__(self, tmpDir, config):
    self.tmpDir = tmpDir
    self.config = config
    pass


  def open_subporcess_files(self, tmpoutfile, tmperrfile, override_output_files):
    if override_output_files: # Recreate files
      tmpout =  open(tmpoutfile, 'w')
      tmperr =  open(tmperrfile, 'w')
    else: # Append to files
      tmpout =  open(tmpoutfile, 'a')
      tmperr =  open(tmperrfile, 'a')
    return tmpout, tmperr
    
  def run_file(self, script, script_params, tmp_dir, tmpoutfile, tmperrfile,
               timeout, tmpstructedoutfile, logger_level, callback, task_id,
               override_output_files = True, handle = None):
    """
    Executes the specified python file in a separate subprocess.
    Method returns only when the subprocess is finished.
    Params arg is a list of script parameters
    Timeout meaning: how many seconds should pass before script execution
    is forcibly terminated
    override_output_files option defines whether stdout/stderr files will be
    recreated or appended
    """
    # need to remove this file for the following case:
    # status call 1 does not write to file; call 2 writes to file;
    # call 3 does not write to file, so contents are still call 2's result
    try:
      os.unlink(tmpstructedoutfile)
    except OSError:
      pass # no error

    script_params += [tmpstructedoutfile, logger_level, tmp_dir]
    pythonCommand = self.python_command(script, script_params)
    logger.info("Running command " + pprint.pformat(pythonCommand))
    if(handle == None) :
      tmpout, tmperr = self.open_subporcess_files(tmpoutfile, tmperrfile, override_output_files)
      
      process = self.launch_python_subprocess(pythonCommand, tmpout, tmperr)
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
      return self.prepare_process_result(process, tmpoutfile, tmperrfile, tmpstructedoutfile, timeout=timeout)
    else:
      holder = Holder(pythonCommand, tmpoutfile, tmperrfile, tmpstructedoutfile, handle)
      
      background = BackgroundThread(holder, self)
      background.start()
      return {"exitcode": 777}

  def prepare_process_result (self, process, tmpoutfile, tmperrfile, tmpstructedoutfile, timeout=None):
    out, error, structured_out = self.read_result_from_files(tmpoutfile, tmperrfile, tmpstructedoutfile)
    # Building results
    returncode = process.returncode

    if self.python_process_has_been_killed:
      error = str(error) + "\n Python script has been killed due to timeout" + \
              (" after waiting %s secs" % str(timeout) if timeout else "")
      returncode = 999
    result = self.condenseOutput(out, error, returncode, structured_out)
    logger.info("Result: %s" % result)
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
  
  def launch_python_subprocess(self, command, tmpout, tmperr):
    """
    Creates subprocess with given parameters. This functionality was moved to separate method
    to make possible unit testing
    """
    return subprocess.Popen(command,
      stdout=tmpout,
      stderr=tmperr, close_fds=True)
    
  def isSuccessfull(self, returncode):
    return not self.python_process_has_been_killed and returncode == 0

  def python_command(self, script, script_params):
    python_binary = sys.executable
    python_command = [python_binary, script] + script_params
    return python_command

  def condenseOutput(self, stdout, stderr, retcode, structured_out):
    log_lines_count = self.config.get('heartbeat', 'log_lines_count')
    
    grep = self.grep
    result = {
      "exitcode": retcode,
      "stdout": grep.tail(stdout, log_lines_count) if log_lines_count else stdout,
      "stderr": grep.tail(stderr, log_lines_count) if log_lines_count else stderr,
      "structuredOut" : structured_out
    }
    
    return result

  def python_watchdog_func(self, python, timeout):
    self.event.wait(timeout)
    if python.returncode is None:
      logger.error("Subprocess timed out and will be killed")
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
    process_out, process_err  = self.pythonExecutor.open_subporcess_files(self.holder.out_file, self.holder.err_file, True)
    
    logger.info("Starting process command %s" % self.holder.command)
    process = self.pythonExecutor.launch_python_subprocess(self.holder.command, process_out, process_err)
    
    logger.info("Process has been started. Pid = %s" % process.pid)
    
    self.holder.handle.pid = process.pid
    self.holder.handle.status = BackgroundCommandExecutionHandle.RUNNING_STATUS
    self.holder.handle.on_background_command_started(self.holder.handle.command['taskId'], process.pid)
    
    process.communicate()
    
    self.holder.handle.exitCode = process.returncode
    process_condenced_result = self.pythonExecutor.prepare_process_result(process, self.holder.out_file, self.holder.err_file, self.holder.structured_out_file)
    logger.info("Calling callback with args %s" % process_condenced_result)
    self.holder.handle.on_background_command_complete_callback(process_condenced_result, self.holder.handle)
    logger.info("Exiting from thread for holder pid %s" % self.holder.handle.pid)
    
  
