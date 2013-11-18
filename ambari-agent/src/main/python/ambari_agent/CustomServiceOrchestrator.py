#!/usr/bin/env python2.6

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

import logging
import os
import json, pprint
import sys

from FileCache import FileCache
from AgentException import AgentException
from PythonExecutor import PythonExecutor
from AmbariConfig import AmbariConfig


logger = logging.getLogger()

class CustomServiceOrchestrator():
  """
  Executes a command for custom service. stdout and stderr are written to
  tmpoutfile and to tmperrfile respectively.
  """

  SCRIPT_TYPE_PYTHON = "PYTHON"

  def __init__(self, config):
    self.config = config
    self.tmp_dir = config.get('agent', 'prefix')
    self.file_cache = FileCache(config)
    self.python_executor = PythonExecutor(self.tmp_dir, config)


  def runCommand(self, command, tmpoutfile, tmperrfile):
    try:
      component_name = command['role']
      stack_name = command['hostLevelParams']['stack_name']
      stack_version = command['hostLevelParams']['stack_version']
      script_type = command['commandParams']['script_type']
      script = command['commandParams']['script']
      command_name = command['roleCommand']
      timeout = int(command['commandParams']['command_timeout'])
      metadata_folder = command['commandParams']['service_metadata_folder']
      base_dir = self.file_cache.get_service_base_dir(
          stack_name, stack_version, metadata_folder, component_name)
      script_path = self.resolve_script_path(base_dir, script, script_type)
      if script_type.upper() == self.SCRIPT_TYPE_PYTHON:
        json_path = self.dump_command_to_json(command)
        script_params = [command_name, json_path, base_dir]
        ret = self.python_executor.run_file(
          script_path, script_params, tmpoutfile, tmperrfile, timeout)
      else:
        message = "Unknown script type {0}".format(script_type)
        raise AgentException(message)
    except Exception: # We do not want to let agent fail completely
      exc_type, exc_obj, exc_tb = sys.exc_info()
      message = "Catched an exception while executing "\
        "custom service command: {0}: {1}".format(exc_type, exc_obj)
      logger.error(message)
      ret = {
        'stdout' : message,
        'stderr' : message,
        'exitcode': 1,
      }
    return ret


  def resolve_script_path(self, base_dir, script, script_type):
    """
    Incapsulates logic of script location determination.
    """
    path = os.path.join(base_dir, "package", script)
    if not os.path.exists(path):
      message = "Script {0} does not exist".format(path)
      raise AgentException(message)
    return path


  def dump_command_to_json(self, command):
    """
    Converts command to json file and returns file path
    """
    task_id = command['taskId']
    file_path = os.path.join(self.tmp_dir, "command-{0}.json".format(task_id))
    # Command json contains passwords, that's why we need proper permissions
    with os.fdopen(os.open(file_path, os.O_WRONLY | os.O_CREAT,0600), 'w') as f:
      content = json.dumps(command, sort_keys = False, indent = 4)
      f.write(content)
    return file_path
