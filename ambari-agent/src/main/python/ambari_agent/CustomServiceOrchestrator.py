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
      # TODO: Adjust variables
      service_name = command['serviceName']
      component_name = command['role']
      stack_name = command['stackName'] # TODO: add at the server side
      stack_version = command['stackVersion'] # TODO: add at the server side
      script_type = command['scriptType'] # TODO: add at the server side
      script = command['script']
      command_name = command['roleCommand']
      timeout = int(command['timeout']) # TODO: add at the server side
      base_dir = self.file_cache.get_service_base_dir(
          stack_name, stack_version, service_name, component_name)
      script_path = self.resolve_script_path(base_dir, script, script_type)
      if script_type == self.SCRIPT_TYPE_PYTHON:
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
        'exitCode': 1,
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
    command_id = command['commandId']
    file_path = os.path.join(self.tmp_dir, "command-{0}.json".format(command_id))
    with open(file_path, "w") as f:
      content = json.dumps(command)
      f.write(content)
    return file_path


def main():
  """
  May be used for manual testing if needed
  """
  config = AmbariConfig().getConfig()
  orchestrator = CustomServiceOrchestrator(config)
  config.set('agent', 'prefix', "/tmp")
  command = {
    "serviceName" : "HBASE",
    "role" : "HBASE_MASTER",
    "stackName" : "HDP",
    "stackVersion" : "1.2.0",
    "scriptType" : "PYTHON",
    "script" : "/tmp/1.py",
    "roleCommand" : "START",
    "timeout": 600
  }

  result = orchestrator.runCommand(command, "/tmp/out-1.txt", "/tmp/err-1.txt")
  pprint.pprint(result)
  pass



if __name__ == "__main__":
  main()
