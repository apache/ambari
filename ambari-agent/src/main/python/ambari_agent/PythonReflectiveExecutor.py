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

from PythonExecutor import PythonExecutor
from resource_management.core.exceptions import ClientComponentHasNoStatus, ComponentIsNotRunning

import imp
import sys
import os
import pprint
import logging
import copy

logger = logging.getLogger()

class PythonReflectiveExecutor(PythonExecutor):
  """
  Some commands like STATUS, SECURITY_STATUS commands are run a lot, and need to be run really fast.
  Otherwise agent will hang waiting for them to complete every X seconds.
  
  Running the commands not in new proccess, but reflectively makes this really fast.
  """
  
  def __init__(self, tmpDir, config):
    super(PythonReflectiveExecutor, self).__init__(tmpDir, config)
    
  def run_file(self, script, script_params, tmpoutfile, tmperrfile,
               timeout, tmpstructedoutfile, callback, task_id,
               override_output_files = True, backup_log_files = True,
               handle = None, log_info_on_failure=True):
    pythonCommand = self.python_command(script, script_params)
    if logger.isEnabledFor(logging.DEBUG):
      logger.debug("Running command reflectively %s", pprint.pformat(pythonCommand))
    
    script_dir = os.path.dirname(script)
    self.open_subprocess_files(tmpoutfile, tmperrfile, override_output_files, backup_log_files)
    returncode = 1

    try:
      current_context = PythonContext(script_dir, pythonCommand)
      PythonReflectiveExecutor.last_context = current_context
      with current_context:
        imp.load_source('__main__', script)
    except SystemExit as e:
      returncode = e.code
      if returncode:
        logger.debug("Reflective command failed with return_code=" + str(e))
    except (ClientComponentHasNoStatus, ComponentIsNotRunning):
      logger.debug("Reflective command failed with exception:", exc_info=1)
    except Exception:
      logger.info("Reflective command failed with exception:", exc_info=1)
    else: 
      returncode = 0
      
    return self.prepare_process_result(returncode, tmpoutfile, tmperrfile, tmpstructedoutfile, timeout=timeout)
  
class PythonContext:
  """
  Sets and resets some context like imports, pythonpath, args.
  Also it disable logging into ambari-agent.log for reflectively called scripts.
  """
  def __init__(self, script_dir, pythonCommand):
    self.script_dir = script_dir
    self.pythonCommand = pythonCommand
    self.is_reverted = False
    self.is_forced_revert = False
    
  def __enter__(self):
    self.old_sys_path = copy.copy(sys.path)
    self.old_agv = copy.copy(sys.argv)
    self.old_sys_modules = copy.copy(sys.modules)
    self.old_logging_disable = logging.root.manager.disable
    
    logging.disable(logging.ERROR)
    sys.path.insert(0, self.script_dir)
    sys.argv = self.pythonCommand[1:]

  def __exit__(self, exc_type, exc_val, exc_tb):
    self.revert(is_forced_revert=False)
    return False
  
  def revert(self, is_forced_revert=True):
    if not self.is_reverted:
      self.is_forced_revert = is_forced_revert
      self.is_reverted = True
      sys.path = self.old_sys_path
      sys.argv = self.old_agv
      logging.disable(self.old_logging_disable)
      self.revert_sys_modules(self.old_sys_modules)

  def revert_sys_modules(self, value):
    sys.modules.update(value)
    
    for k in copy.copy(sys.modules):
      if not k in value:
        del sys.modules[k]