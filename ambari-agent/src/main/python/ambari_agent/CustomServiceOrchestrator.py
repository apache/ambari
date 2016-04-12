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
import ambari_simplejson as json
import sys
from ambari_commons import shell
import threading

from FileCache import FileCache
from AgentException import AgentException
from PythonExecutor import PythonExecutor
from PythonReflectiveExecutor import PythonReflectiveExecutor
import Constants
import hostname


logger = logging.getLogger()

class CustomServiceOrchestrator():
  """
  Executes a command for custom service. stdout and stderr are written to
  tmpoutfile and to tmperrfile respectively.
  """

  SCRIPT_TYPE_PYTHON = "PYTHON"
  COMMAND_NAME_STATUS = "STATUS"
  COMMAND_NAME_SECURITY_STATUS = "SECURITY_STATUS"
  CUSTOM_ACTION_COMMAND = 'ACTIONEXECUTE'
  CUSTOM_COMMAND_COMMAND = 'CUSTOM_COMMAND'

  PRE_HOOK_PREFIX="before"
  POST_HOOK_PREFIX="after"

  HOSTS_LIST_KEY = "all_hosts"
  PING_PORTS_KEY = "all_ping_ports"
  RACKS_KEY = "all_racks"
  IPV4_ADDRESSES_KEY = "all_ipv4_ips"

  AMBARI_SERVER_HOST = "ambari_server_host"
  DONT_DEBUG_FAILURES_FOR_COMMANDS = [COMMAND_NAME_SECURITY_STATUS, COMMAND_NAME_STATUS]
  REFLECTIVELY_RUN_COMMANDS = [COMMAND_NAME_SECURITY_STATUS, COMMAND_NAME_STATUS] # -- commands which run a lot and often (this increases their speed)

  def __init__(self, config, controller):
    self.config = config
    self.tmp_dir = config.get('agent', 'prefix')
    self.exec_tmp_dir = Constants.AGENT_TMP_DIR
    self.file_cache = FileCache(config)
    self.status_commands_stdout = os.path.join(self.tmp_dir,
                                               'status_command_stdout.txt')
    self.status_commands_stderr = os.path.join(self.tmp_dir,
                                               'status_command_stderr.txt')
    self.public_fqdn = hostname.public_hostname(config)
    # cache reset will be called on every agent registration
    controller.registration_listeners.append(self.file_cache.reset)

    # Clean up old status command files if any
    try:
      os.unlink(self.status_commands_stdout)
      os.unlink(self.status_commands_stderr)
    except OSError:
      pass # Ignore fail
    self.commands_in_progress_lock = threading.RLock()
    self.commands_in_progress = {}

  def map_task_to_process(self, task_id, processId):
    with self.commands_in_progress_lock:
      logger.debug('Maps taskId=%s to pid=%s' % (task_id, processId))
      self.commands_in_progress[task_id] = processId

  def cancel_command(self, task_id, reason):
    with self.commands_in_progress_lock:
      if task_id in self.commands_in_progress.keys():
        pid = self.commands_in_progress.get(task_id)
        self.commands_in_progress[task_id] = reason
        logger.info("Canceling command with task_id - {tid}, " \
                    "reason - {reason} . Killing process {pid}"
                    .format(tid=str(task_id), reason=reason, pid=pid))
        shell.kill_process_with_children(pid)
      else: 
        logger.warn("Unable to find pid by taskId = %s" % task_id)

  def get_py_executor(self, forced_command_name):
    """
    Wrapper for unit testing
    :return:
    """
    if forced_command_name in self.REFLECTIVELY_RUN_COMMANDS:
      return PythonReflectiveExecutor(self.tmp_dir, self.config)
    else:
      return PythonExecutor(self.tmp_dir, self.config)

  def runCommand(self, command, tmpoutfile, tmperrfile, forced_command_name=None,
                 override_output_files=True, retry=False):
    """
    forced_command_name may be specified manually. In this case, value, defined at
    command json, is ignored.
    """
    try:
      script_type = command['commandParams']['script_type']
      script = command['commandParams']['script']
      timeout = int(command['commandParams']['command_timeout'])

      if 'hostLevelParams' in command and 'jdk_location' in command['hostLevelParams']:
        server_url_prefix = command['hostLevelParams']['jdk_location']
      else:
        server_url_prefix = command['commandParams']['jdk_location']
        
      task_id = "status"
      
      try:
        task_id = command['taskId']
        command_name = command['roleCommand']
      except KeyError:
        pass  # Status commands have no taskId

      if forced_command_name is not None:  # If not supplied as an argument
        command_name = forced_command_name

      if command_name == self.CUSTOM_ACTION_COMMAND:
        base_dir = self.file_cache.get_custom_actions_base_dir(server_url_prefix)
        script_tuple = (os.path.join(base_dir, 'scripts', script), base_dir)
        hook_dir = None
      else:
        if command_name == self.CUSTOM_COMMAND_COMMAND:
          command_name = command['hostLevelParams']['custom_command']

        # forces a hash challenge on the directories to keep them updated, even
        # if the return type is not used
        self.file_cache.get_host_scripts_base_dir(server_url_prefix)          
        hook_dir = self.file_cache.get_hook_base_dir(command, server_url_prefix)
        base_dir = self.file_cache.get_service_base_dir(command, server_url_prefix)
        
        script_path = self.resolve_script_path(base_dir, script)
        script_tuple = (script_path, base_dir)

      tmpstrucoutfile = os.path.join(self.tmp_dir,
                                    "structured-out-{0}.json".format(task_id))

      # We don't support anything else yet
      if script_type.upper() != self.SCRIPT_TYPE_PYTHON:
        message = "Unknown script type {0}".format(script_type)
        raise AgentException(message)

      # Execute command using proper interpreter
      handle = None
      if command.has_key('__handle'):
        handle = command['__handle']
        handle.on_background_command_started = self.map_task_to_process
        del command['__handle']

      json_path = self.dump_command_to_json(command, retry)
      pre_hook_tuple = self.resolve_hook_script_path(hook_dir,
          self.PRE_HOOK_PREFIX, command_name, script_type)
      post_hook_tuple = self.resolve_hook_script_path(hook_dir,
          self.POST_HOOK_PREFIX, command_name, script_type)
      py_file_list = [pre_hook_tuple, script_tuple, post_hook_tuple]
      # filter None values
      filtered_py_file_list = [i for i in py_file_list if i]

      logger_level = logging.getLevelName(logger.level)

      # Executing hooks and script
      ret = None
      from ActionQueue import ActionQueue
      if command.has_key('commandType') and command['commandType'] == ActionQueue.BACKGROUND_EXECUTION_COMMAND and len(filtered_py_file_list) > 1:
        raise AgentException("Background commands are supported without hooks only")

      python_executor = self.get_py_executor(forced_command_name)
      for py_file, current_base_dir in filtered_py_file_list:
        log_info_on_failure = not command_name in self.DONT_DEBUG_FAILURES_FOR_COMMANDS
        script_params = [command_name, json_path, current_base_dir, tmpstrucoutfile, logger_level, self.exec_tmp_dir]
        ret = python_executor.run_file(py_file, script_params,
                               tmpoutfile, tmperrfile, timeout,
                               tmpstrucoutfile, self.map_task_to_process,
                               task_id, override_output_files, handle = handle, log_info_on_failure=log_info_on_failure)
        # Next run_file() invocations should always append to current output
        override_output_files = False
        if ret['exitcode'] != 0:
          break

      if not ret: # Something went wrong
        raise AgentException("No script has been executed")

      # if canceled and not background command
      if handle is None:
        cancel_reason = self.command_canceled_reason(task_id)
        if cancel_reason:
          ret['stdout'] += cancel_reason
          ret['stderr'] += cancel_reason

          with open(tmpoutfile, "a") as f:
            f.write(cancel_reason)
          with open(tmperrfile, "a") as f:
            f.write(cancel_reason)

    except Exception, e: # We do not want to let agent fail completely
      exc_type, exc_obj, exc_tb = sys.exc_info()
      message = "Caught an exception while executing "\
        "custom service command: {0}: {1}; {2}".format(exc_type, exc_obj, str(e))
      logger.exception(message)
      ret = {
        'stdout' : message,
        'stderr' : message,
        'structuredOut' : '{}',
        'exitcode': 1,
      }
    return ret

  def command_canceled_reason(self, task_id):
    with self.commands_in_progress_lock:
      if self.commands_in_progress.has_key(task_id):#Background command do not push in this collection (TODO)
        logger.debug('Pop with taskId %s' % task_id)
        pid = self.commands_in_progress.pop(task_id)
        if not isinstance(pid, int):
          return '\nCommand aborted. ' + pid
    return None

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
    return res

  def requestComponentSecurityState(self, command):
    """
     Determines the current security state of the component
     A command will be issued to trigger the security_status check and the result of this check will
     returned to the caller. If the component lifecycle script has no security_status method the
     check will return non zero exit code and "UNKNOWN" will be returned.
    """
    override_output_files=True # by default, we override status command output
    if logger.level == logging.DEBUG:
      override_output_files = False
    security_check_res = self.runCommand(command, self.status_commands_stdout,
                                         self.status_commands_stderr, self.COMMAND_NAME_SECURITY_STATUS,
                                         override_output_files=override_output_files)
    result = 'UNKNOWN'

    if security_check_res is None:
      logger.warn("The return value of the security_status check was empty, the security status is unknown")
    elif 'exitcode' not in security_check_res:
      logger.warn("Missing 'exitcode' value from the security_status check result, the security status is unknown")
    elif security_check_res['exitcode'] != 0:
      logger.debug("The 'exitcode' value from the security_status check result indicated the check routine failed to properly execute, the security status is unknown")
    elif 'structuredOut' not in security_check_res:
      logger.warn("Missing 'structuredOut' value from the security_status check result, the security status is unknown")
    elif 'securityState' not in security_check_res['structuredOut']:
      logger.warn("Missing 'securityState' value from the security_status check structuredOut data set, the security status is unknown")
    else:
      result = security_check_res['structuredOut']['securityState']

    return result

  def resolve_script_path(self, base_dir, script):
    """
    Encapsulates logic of script location determination.
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


  def dump_command_to_json(self, command, retry=False):
    """
    Converts command to json file and returns file path
    """
    # Perform few modifications to stay compatible with the way in which
    public_fqdn = self.public_fqdn
    command['public_hostname'] = public_fqdn
    # Add cache dir to make it visible for commands
    command["hostLevelParams"]["agentCacheDir"] = self.config.get('agent', 'cache_dir')
    command["agentConfigParams"] = {"agent": {"parallel_execution": self.config.get_parallel_exec_option()}}
    # Now, dump the json file
    command_type = command['commandType']
    from ActionQueue import ActionQueue  # To avoid cyclic dependency
    if command_type == ActionQueue.STATUS_COMMAND:
      # These files are frequently created, that's why we don't
      # store them all, but only the latest one
      file_path = os.path.join(self.tmp_dir, "status_command.json")
    else:
      task_id = command['taskId']
      if 'clusterHostInfo' in command and command['clusterHostInfo'] and not retry:
        command['clusterHostInfo'] = self.decompressClusterHostInfo(command['clusterHostInfo'])
      file_path = os.path.join(self.tmp_dir, "command-{0}.json".format(task_id))
      if command_type == ActionQueue.AUTO_EXECUTION_COMMAND:
        file_path = os.path.join(self.tmp_dir, "auto_command-{0}.json".format(task_id))

    # Json may contain passwords, that's why we need proper permissions
    if os.path.isfile(file_path):
      os.unlink(file_path)
    with os.fdopen(os.open(file_path, os.O_WRONLY | os.O_CREAT,
                           0600), 'w') as f:
      content = json.dumps(command, sort_keys = False, indent = 4)
      f.write(content)
    return file_path

  def decompressClusterHostInfo(self, clusterHostInfo):
    info = clusterHostInfo.copy()
    #Pop info not related to host roles
    hostsList = info.pop(self.HOSTS_LIST_KEY)
    pingPorts = info.pop(self.PING_PORTS_KEY)
    racks = info.pop(self.RACKS_KEY)
    ipv4_addresses = info.pop(self.IPV4_ADDRESSES_KEY)

    ambariServerHost = info.pop(self.AMBARI_SERVER_HOST)

    decompressedMap = {}

    for k,v in info.items():
      # Convert from 1-3,5,6-8 to [1,2,3,5,6,7,8]
      indexes = self.convertRangeToList(v)
      # Convert from [1,2,3,5,6,7,8] to [host1,host2,host3...]
      decompressedMap[k] = [hostsList[i] for i in indexes]

    #Convert from ['1:0-2,4', '42:3,5-7'] to [1,1,1,42,1,42,42,42]
    pingPorts = self.convertMappedRangeToList(pingPorts)
    racks = self.convertMappedRangeToList(racks)
    ipv4_addresses = self.convertMappedRangeToList(ipv4_addresses)

    #Convert all elements to str
    pingPorts = map(str, pingPorts)

    #Add ping ports to result
    decompressedMap[self.PING_PORTS_KEY] = pingPorts
    #Add hosts list to result
    decompressedMap[self.HOSTS_LIST_KEY] = hostsList
    #Add racks list to result
    decompressedMap[self.RACKS_KEY] = racks
    #Add ips list to result
    decompressedMap[self.IPV4_ADDRESSES_KEY] = ipv4_addresses
    #Add ambari-server host to result
    decompressedMap[self.AMBARI_SERVER_HOST] = ambariServerHost

    return decompressedMap

  # Converts from 1-3,5,6-8 to [1,2,3,5,6,7,8]
  def convertRangeToList(self, list):

    resultList = []

    for i in list:

      ranges = i.split(',')

      for r in ranges:
        rangeBounds = r.split('-')
        if len(rangeBounds) == 2:

          if not rangeBounds[0] or not rangeBounds[1]:
            raise AgentException("Broken data in given range, expected - ""m-n"" or ""m"", got : " + str(r))


          resultList.extend(range(int(rangeBounds[0]), int(rangeBounds[1]) + 1))
        elif len(rangeBounds) == 1:
          resultList.append((int(rangeBounds[0])))
        else:
          raise AgentException("Broken data in given range, expected - ""m-n"" or ""m"", got : " + str(r))

    return resultList

  #Converts from ['1:0-2,4', '42:3,5-7'] to [1,1,1,42,1,42,42,42]
  def convertMappedRangeToList(self, list):

    resultDict = {}

    for i in list:
      valueToRanges = i.split(":")
      if len(valueToRanges) <> 2:
        raise AgentException("Broken data in given value to range, expected format - ""value:m-n"", got - " + str(i))
      value = valueToRanges[0]
      rangesToken = valueToRanges[1]

      for r in rangesToken.split(','):

        rangeIndexes = r.split('-')

        if len(rangeIndexes) == 2:

          if not rangeIndexes[0] or not rangeIndexes[1]:
            raise AgentException("Broken data in given value to range, expected format - ""value:m-n"", got - " + str(r))

          start = int(rangeIndexes[0])
          end = int(rangeIndexes[1])

          for k in range(start, end + 1):
            resultDict[k] = value if not value.isdigit() else int(value)


        elif len(rangeIndexes) == 1:
          index = int(rangeIndexes[0])

          resultDict[index] = value if not value.isdigit() else int(value)


    resultList = dict(sorted(resultDict.items())).values()

    return resultList

