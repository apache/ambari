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

  def run_file(self, script, script_params, tmpoutfile, tmperrfile, timeout,
               tmpstructedoutfile, override_output_files = True):
    """
    Executes the specified python file in a separate subprocess.
    Method returns only when the subprocess is finished.
    Params arg is a list of script parameters
    Timeout meaning: how many seconds should pass before script execution
    is forcibly terminated
    override_output_files option defines whether stdout/stderr files will be
    recreated or appended
    """
    if override_output_files: # Recreate files
      tmpout =  open(tmpoutfile, 'w')
      tmperr =  open(tmperrfile, 'w')
    else: # Append to files
      tmpout =  open(tmpoutfile, 'a')
      tmperr =  open(tmperrfile, 'a')
    script_params += [tmpstructedoutfile]
    pythonCommand = self.python_command(script, script_params)
    logger.info("Running command " + pprint.pformat(pythonCommand))
    process = self.launch_python_subprocess(pythonCommand, tmpout, tmperr)
    logger.debug("Launching watchdog thread")
    self.event.clear()
    self.python_process_has_been_killed = False
    thread = Thread(target =  self.python_watchdog_func, args = (process, timeout))
    thread.start()
    # Waiting for the process to be either finished or killed
    process.communicate()
    self.event.set()
    thread.join()
    # Building results
    error = self.NO_ERROR
    returncode = process.returncode
    out = open(tmpoutfile, 'r').read()
    error = open(tmperrfile, 'r').read()

    try:
      with open(tmpstructedoutfile, 'r') as fp:
        structured_out = json.load(fp)
    except Exception:
      if os.path.exists(tmpstructedoutfile):
        errMsg = 'Unable to read structured output from ' + tmpstructedoutfile
        structured_out = {
          'msg' : errMsg
        }
        logger.warn(structured_out)
      else:
        structured_out = '{}'

    if self.python_process_has_been_killed:
      error = str(error) + "\n Python script has been killed due to timeout"
      returncode = 999
    result = self.condenseOutput(out, error, returncode, structured_out)
    logger.info("Result: %s" % result)
    return result


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
