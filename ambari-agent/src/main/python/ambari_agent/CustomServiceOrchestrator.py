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

import logging
import os
import json, pprint
import sys

from FileCache import FileCache
from AgentException import AgentException
from PythonExecutor import PythonExecutor
from AmbariConfig import AmbariConfig
import hostname
from LiveStatus import LiveStatus
import manifestGenerator


logger = logging.getLogger()

class CustomServiceOrchestrator():
  """
  Executes a command for custom service. stdout and stderr are written to
  tmpoutfile and to tmperrfile respectively.
  """

  SCRIPT_TYPE_PYTHON = "PYTHON"
  COMMAND_NAME_STATUS = "STATUS"
  CUSTOM_ACTION_COMMAND = 'ACTIONEXECUTE'
  CUSTOM_COMMAND_COMMAND = 'CUSTOM_COMMAND'

  PRE_HOOK_PREFIX="before"
  POST_HOOK_PREFIX="after"


  def __init__(self, config, controller):
    self.config = config
    self.tmp_dir = config.get('agent', 'prefix')
    self.file_cache = FileCache(config)
    self.python_executor = PythonExecutor(self.tmp_dir, config)
    self.status_commands_stdout = os.path.join(self.tmp_dir,
                                               'status_command_stdout.txt')
    self.status_commands_stderr = os.path.join(self.tmp_dir,
                                               'status_command_stderr.txt')
    self.public_fqdn = hostname.public_hostname()
    # cache reset will be called on every agent registration
    controller.registration_listeners.append(self.file_cache.reset)
    # Clean up old status command files if any
    try:
      os.unlink(self.status_commands_stdout)
      os.unlink(self.status_commands_stderr)
    except OSError:
      pass # Ignore fail


  def runCommand(self, command, tmpoutfile, tmperrfile, forsed_command_name = None,
                 override_output_files = True):
    """
    forsed_command_name may be specified manually. In this case, value, defined at
    command json, is ignored.
    """
    try:
      script_type = command['commandParams']['script_type']
      script = command['commandParams']['script']
      timeout = int(command['commandParams']['command_timeout'])
      server_url_prefix = command['hostLevelParams']['jdk_location']
      task_id = "status"
      try:
        task_id = command['taskId']
        command_name = command['roleCommand']
      except KeyError:
        pass # Status commands have no taskId

      if forsed_command_name is not None: # If not supplied as an argument
        command_name = forsed_command_name

      if command_name == self.CUSTOM_ACTION_COMMAND:
        base_dir = self.file_cache.get_custom_actions_base_dir(server_url_prefix)
        script_tuple = (os.path.join(base_dir, script) , base_dir)
        hook_dir = None
      else:
        if command_name == self.CUSTOM_COMMAND_COMMAND:
          command_name = command['hostLevelParams']['custom_command']
        hook_dir = self.file_cache.get_hook_base_dir(command, server_url_prefix)
        base_dir = self.file_cache.get_service_base_dir(command, server_url_prefix)
        script_path = self.resolve_script_path(base_dir, script, script_type)
        script_tuple = (script_path, base_dir)


      tmpstrucoutfile = os.path.join(self.tmp_dir,
                                    "structured-out-{0}.json".format(task_id))
      if script_type.upper() != self.SCRIPT_TYPE_PYTHON:
      # We don't support anything else yet
        message = "Unknown script type {0}".format(script_type)
        raise AgentException(message)
      # Execute command using proper interpreter
      json_path = self.dump_command_to_json(command)
      pre_hook_tuple = self.resolve_hook_script_path(hook_dir,
          self.PRE_HOOK_PREFIX, command_name, script_type)
      post_hook_tuple = self.resolve_hook_script_path(hook_dir,
          self.POST_HOOK_PREFIX, command_name, script_type)
      py_file_list = [pre_hook_tuple, script_tuple, post_hook_tuple]
      # filter None values
      filtered_py_file_list = [i for i in py_file_list if i]

      # Executing hooks and script
      ret = None
      for py_file, current_base_dir in filtered_py_file_list:
        script_params = [command_name, json_path, current_base_dir]
        ret = self.python_executor.run_file(py_file, script_params,
                               tmpoutfile, tmperrfile, timeout,
                               tmpstrucoutfile, override_output_files)
        # Next run_file() invocations should always append to current output
        override_output_files = False
        if ret['exitcode'] != 0:
          break

      if not ret: # Something went wrong
        raise AgentException("No script has been executed")

    except Exception: # We do not want to let agent fail completely
      exc_type, exc_obj, exc_tb = sys.exc_info()
      message = "Catched an exception while executing "\
        "custom service command: {0}: {1}".format(exc_type, exc_obj)
      logger.exception(message)
      ret = {
        'stdout' : message,
        'stderr' : message,
        'structuredOut' : '{}',
        'exitcode': 1,
      }
    return ret


  def requestComponentStatus(self, command):
    """
     Component status is determined by exit code, returned by runCommand().
     Exit code 0 means that component is running and any other exit code means that
     component is not running
    """
    override_output_files=True # by default, we override status command output
    if logger.level == logging.DEBUG:
      override_output_files = False
    res = self.runCommand(command, self.status_commands_stdout,
                          self.status_commands_stderr, self.COMMAND_NAME_STATUS,
                          override_output_files=override_output_files)
    if res['exitcode'] == 0:
      return LiveStatus.LIVE_STATUS
    else:
      return LiveStatus.DEAD_STATUS


  def resolve_script_path(self, base_dir, script, script_type):
    """
    Incapsulates logic of script location determination.
    """
    path = os.path.join(base_dir, script)
    if not os.path.exists(path):
      message = "Script {0} does not exist".format(path)
      raise AgentException(message)
    return path


  def resolve_hook_script_path(self, stack_hooks_dir, prefix, command_name, script_type):
    """
    Returns a tuple(path to hook script, hook base dir) according to string prefix
    and command name. If script does not exist, returns None
    """
    if not stack_hooks_dir:
      return None
    hook_dir = "{0}-{1}".format(prefix, command_name)
    hook_base_dir = os.path.join(stack_hooks_dir, hook_dir)
    hook_script_path = os.path.join(hook_base_dir, "scripts", "hook.py")
    if not os.path.isfile(hook_script_path):
      logger.debug("Hook script {0} not found, skipping".format(hook_script_path))
      return None
    return hook_script_path, hook_base_dir


  def dump_command_to_json(self, command):
    """
    Converts command to json file and returns file path
    """
    # Perform few modifications to stay compatible with the way in which
    # site.pp files are generated by manifestGenerator.py
    public_fqdn = self.public_fqdn
    command['public_hostname'] = public_fqdn
    # Now, dump the json file
    command_type = command['commandType']
    from ActionQueue import ActionQueue  # To avoid cyclic dependency
    if command_type == ActionQueue.STATUS_COMMAND:
      # These files are frequently created, thats why we don't
      # store them all, but only the latest one
      file_path = os.path.join(self.tmp_dir, "status_command.json")
    else:
      task_id = command['taskId']
      command['clusterHostInfo'] = manifestGenerator.decompressClusterHostInfo(command['clusterHostInfo'])
      file_path = os.path.join(self.tmp_dir, "command-{0}.json".format(task_id))
    # Json may contain passwords, that's why we need proper permissions
    if os.path.isfile(file_path):
      os.unlink(file_path)
    with os.fdopen(os.open(file_path, os.O_WRONLY | os.O_CREAT,
                           0600), 'w') as f:
      content = json.dumps(command, sort_keys = False, indent = 4)
      f.write(content)
    return file_path
