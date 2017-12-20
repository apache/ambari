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
from resource_management.libraries.functions.log_process_information import log_process_information
from resource_management.core.utils import PasswordString
import subprocess
import Constants
import hostname


logger = logging.getLogger()

class CustomServiceOrchestrator():
  """
  Executes a command for custom service. stdout and stderr are written to
  tmpoutfile and to tmperrfile respectively.
  """

  SCRIPT_TYPE_PYTHON = "PYTHON"
  COMMAND_TYPE = "commandType"
  COMMAND_NAME_STATUS = "STATUS"
  CUSTOM_ACTION_COMMAND = 'ACTIONEXECUTE'
  CUSTOM_COMMAND_COMMAND = 'CUSTOM_COMMAND'

  PRE_HOOK_PREFIX="before"
  POST_HOOK_PREFIX="after"

  HOSTS_LIST_KEY = "all_hosts"
  PING_PORTS_KEY = "all_ping_ports"
  RACKS_KEY = "all_racks"
  IPV4_ADDRESSES_KEY = "all_ipv4_ips"

  AMBARI_SERVER_HOST = "ambari_server_host"
  AMBARI_SERVER_PORT = "ambari_server_port"
  AMBARI_SERVER_USE_SSL = "ambari_server_use_ssl"

  FREQUENT_COMMANDS = [COMMAND_NAME_STATUS]
  DONT_DEBUG_FAILURES_FOR_COMMANDS = FREQUENT_COMMANDS
  REFLECTIVELY_RUN_COMMANDS = FREQUENT_COMMANDS # -- commands which run a lot and often (this increases their speed)
  DONT_BACKUP_LOGS_FOR_COMMANDS = FREQUENT_COMMANDS

  # Path where hadoop credential JARS will be available
  DEFAULT_CREDENTIAL_SHELL_LIB_PATH = '/var/lib/ambari-agent/cred/lib'
  DEFAULT_CREDENTIAL_CONF_DIR = '/var/lib/ambari-agent/cred/conf'
  DEFAULT_CREDENTIAL_SHELL_CMD = 'org.apache.hadoop.security.alias.CredentialShell'

  # The property name used by the hadoop credential provider
  CREDENTIAL_PROVIDER_PROPERTY_NAME = 'hadoop.security.credential.provider.path'

  # Property name for credential store class path
  CREDENTIAL_STORE_CLASS_PATH_NAME = 'credentialStoreClassPath'

  def __init__(self, config, controller):
    self.config = config
    self.tmp_dir = config.get('agent', 'prefix')
    self.force_https_protocol = config.get_force_https_protocol_name()
    self.ca_cert_file_path = config.get_ca_cert_file_path()
    self.exec_tmp_dir = Constants.AGENT_TMP_DIR
    self.file_cache = FileCache(config)
    self.status_commands_stdout = os.path.join(self.tmp_dir,
                                               'status_command_stdout.txt')
    self.status_commands_stderr = os.path.join(self.tmp_dir,
                                               'status_command_stderr.txt')
    self.public_fqdn = hostname.public_hostname(config)
    # cache reset will be called on every agent registration
    controller.registration_listeners.append(self.file_cache.reset)

    # Construct the hadoop credential lib JARs path
    self.credential_shell_lib_path = os.path.join(config.get('security', 'credential_lib_dir',
                                                             self.DEFAULT_CREDENTIAL_SHELL_LIB_PATH), '*')

    self.credential_conf_dir = config.get('security', 'credential_conf_dir', self.DEFAULT_CREDENTIAL_CONF_DIR)

    self.credential_shell_cmd = config.get('security', 'credential_shell_cmd', self.DEFAULT_CREDENTIAL_SHELL_CMD)

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
      logger.debug('Maps taskId=%s to pid=%s', task_id, processId)
      self.commands_in_progress[task_id] = processId

  def cancel_command(self, task_id, reason):
    with self.commands_in_progress_lock:
      if task_id in self.commands_in_progress.keys():
        pid = self.commands_in_progress.get(task_id)
        self.commands_in_progress[task_id] = reason
        logger.info("Canceling command with taskId = {tid}, " \
                    "reason - {reason} . Killing process {pid}"
                    .format(tid=str(task_id), reason=reason, pid=pid))
        log_process_information(logger)
        shell.kill_process_with_children(pid)
      else: 
        logger.warn("Unable to find process associated with taskId = %s" % task_id)

  def get_py_executor(self, forced_command_name):
    """
    Wrapper for unit testing
    :return:
    """
    if forced_command_name in self.REFLECTIVELY_RUN_COMMANDS:
      return PythonReflectiveExecutor(self.tmp_dir, self.config)
    else:
      return PythonExecutor(self.tmp_dir, self.config)

  def getProviderDirectory(self, service_name):
    """
    Gets the path to the service conf folder where the JCEKS file will be created.

    :param service_name: Name of the service, for example, HIVE
    :return: lower case path to the service conf folder
    """

    # The stack definition scripts of the service can move the
    # JCEKS file around to where it wants, which is usually
    # /etc/<service_name>/conf

    conf_dir = os.path.join(self.credential_conf_dir, service_name.lower())
    return conf_dir

  def getConfigTypeCredentials(self, commandJson):
    """
    Gets the affected config types for the service in this command
    with the password aliases and values.

    Input:
    {
        "config-type1" : {
          "password_key_name1":"password_value_name1",
          "password_key_name2":"password_value_name2",
            :
        },
        "config-type2" : {
          "password_key_name1":"password_value_name1",
          "password_key_name2":"password_value_name2",
            :
        },
           :
    }

    Output:
    {
        "config-type1" : {
          "alias1":"password1",
          "alias2":"password2",
            :
        },
        "config-type2" : {
          "alias1":"password1",
          "alias2":"password2",
            :
        },
           :
    }

    If password_key_name is the same as password_value_name, then password_key_name is the password alias itself.
    The value it points to is the password value.

    If password_key_name is not the same as the password_value_name, then password_key_name points to the alias.
    The value is pointed to by password_value_name.

    For example:
    Input:
    {
      "oozie-site" : {"oozie.service.JPAService.jdbc.password" : "oozie.service.JPAService.jdbc.password"},
      "admin-properties" {"db_user":"db_password", "ranger.jpa.jdbc.credential.alias:ranger-admin-site" : "db_password"}
    }

    Output:
    {
      "oozie-site" : {"oozie.service.JPAService.jdbc.password" : "MyOozieJdbcPassword"},
      "admin-properties" {"rangerdba" : "MyRangerDbaPassword", "rangeradmin":"MyRangerDbaPassword"},
    }

    :param commandJson:
    :return:
    """
    configtype_credentials = {}
    if 'configuration_credentials' in commandJson:
      for config_type, password_properties in commandJson['configuration_credentials'].items():
        if config_type in commandJson['configurations']:
          value_names = []
          config = commandJson['configurations'][config_type]
          credentials = {}
          for key_name, value_name in password_properties.items():
            if key_name == value_name:
              if value_name in config:
                # password name is the alias
                credentials[key_name] = config[value_name]
                value_names.append(value_name) # Gather the value_name for deletion
            else:
              keyname_keyconfig = key_name.split(':')
              key_name = keyname_keyconfig[0]
              # if the key is in another configuration (cross reference),
              # get the value of the key from that configuration
              if (len(keyname_keyconfig) > 1):
                if keyname_keyconfig[1] not in commandJson['configurations']:
                  continue
                key_config = commandJson['configurations'][keyname_keyconfig[1]]
              else:
                key_config = config
              if key_name in key_config and value_name in config:
                # password name points to the alias
                credentials[key_config[key_name]] = config[value_name]
                value_names.append(value_name) # Gather the value_name for deletion
          if len(credentials) > 0:
            configtype_credentials[config_type] = credentials
            logger.info("Identifying config {0} for CS: ".format(config_type))
          for value_name in value_names:
            # Remove the clear text password
            config.pop(value_name, None)
    return configtype_credentials

  def generateJceks(self, commandJson):
    """
    Generates the JCEKS file with passwords for the service specified in commandJson

    :param commandJson: command JSON
    :return: An exit value from the external process that generated the JCEKS file. None if
    there are no passwords in the JSON.
    """
    cmd_result = None
    roleCommand = None
    if 'roleCommand' in commandJson:
      roleCommand = commandJson['roleCommand']
    task_id = None
    if 'taskId' in commandJson:
      task_id = commandJson['taskId']

    logger.info('Generating the JCEKS file: roleCommand={0} and taskId = {1}'.format(roleCommand, task_id))

    # Set up the variables for the external command to generate a JCEKS file
    java_home = commandJson['hostLevelParams']['java_home']
    java_bin = '{java_home}/bin/java'.format(java_home=java_home)

    cs_lib_path = self.credential_shell_lib_path
    serviceName = commandJson['serviceName']

    # Gather the password values and remove them from the configuration
    configtype_credentials = self.getConfigTypeCredentials(commandJson)

    # CS is enabled but no config property is available for this command
    if len(configtype_credentials) == 0:
      logger.info("Credential store is enabled but no property are found that can be encrypted.")
      commandJson['credentialStoreEnabled'] = "false"

    for config_type, credentials in configtype_credentials.items():
      config = commandJson['configurations'][config_type]
      if 'role' in commandJson and commandJson['role']:
        roleName = commandJson['role']
        file_path = os.path.join(self.getProviderDirectory(roleName), "{0}.jceks".format(config_type))
      else:
        file_path = os.path.join(self.getProviderDirectory(serviceName), "{0}.jceks".format(config_type))
      if os.path.exists(file_path):
        os.remove(file_path)
      provider_path = 'jceks://file{file_path}'.format(file_path=file_path)
      logger.info('provider_path={0}'.format(provider_path))
      for alias, pwd in credentials.items():
        logger.debug("config={0}".format(config))
        protected_pwd = PasswordString(pwd)
        # Generate the JCEKS file
        cmd = (java_bin, '-cp', cs_lib_path, self.credential_shell_cmd, 'create',
               alias, '-value', protected_pwd, '-provider', provider_path)
        logger.info(cmd)
        cmd_result = subprocess.call(cmd)
        logger.info('cmd_result = {0}'.format(cmd_result))
        os.chmod(file_path, 0644) # group and others should have read access so that the service user can read
      # Add JCEKS provider path instead
      config[self.CREDENTIAL_PROVIDER_PROPERTY_NAME] = provider_path
      config[self.CREDENTIAL_STORE_CLASS_PATH_NAME] = cs_lib_path

    return cmd_result


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

      # Status commands have no taskId nor roleCommand
      task_id = command['taskId'] if 'taskId' in command else 'status'
      command_name = command['roleCommand'] if 'roleCommand' in command else None

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
        self.file_cache.get_custom_resources_subdir(command, server_url_prefix)

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

      # If command contains credentialStoreEnabled, then
      # generate the JCEKS file for the configurations.
      credentialStoreEnabled = False
      if 'credentialStoreEnabled' in command:
        credentialStoreEnabled = (command['credentialStoreEnabled'] == "true")

      if credentialStoreEnabled == True:
        if 'commandBeingRetried' not in command or command['commandBeingRetried'] != "true":
          self.generateJceks(command)
        else:
          logger.info("Skipping generation of jceks files as this is a retry of the command")


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
      backup_log_files = not command_name in self.DONT_BACKUP_LOGS_FOR_COMMANDS
      log_out_files = self.config.get("logging","log_out_files", default="0") != "0"
      
      for py_file, current_base_dir in filtered_py_file_list:
        log_info_on_failure = not command_name in self.DONT_DEBUG_FAILURES_FOR_COMMANDS
        script_params = [command_name, json_path, current_base_dir, tmpstrucoutfile, logger_level, self.exec_tmp_dir,
                         self.force_https_protocol, self.ca_cert_file_path]
        
        if log_out_files:
          script_params.append("-o")
        
        ret = python_executor.run_file(py_file, script_params,
                               tmpoutfile, tmperrfile, timeout,
                               tmpstrucoutfile, self.map_task_to_process,
                               task_id, override_output_files, backup_log_files = backup_log_files,
                               handle = handle, log_info_on_failure=log_info_on_failure)
        # Next run_file() invocations should always append to current output
        override_output_files = False
        if ret['exitcode'] != 0:
          break

      if not ret: # Something went wrong
        raise AgentException("No script has been executed")

      # if canceled and not background command
      if handle is None:
        cancel_reason = self.command_canceled_reason(task_id)
        if cancel_reason is not None:
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
        logger.debug('Pop with taskId %s', task_id)
        pid = self.commands_in_progress.pop(task_id)
        if not isinstance(pid, int):
          reason = pid
          if reason:
            return "\nCommand aborted. Reason: '{0}'".format(reason)
          else:
            return "\nCommand aborted."
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
    command["agentConfigParams"] = {
      "agent": {
        "parallel_execution": self.config.get_parallel_exec_option(),
        "use_system_proxy_settings": self.config.use_system_proxy_setting()
      }
    }
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
    ambariServerPort = info.pop(self.AMBARI_SERVER_PORT)
    ambariServerUseSsl = info.pop(self.AMBARI_SERVER_USE_SSL)

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
    #Add ambari-server properties to result
    decompressedMap[self.AMBARI_SERVER_HOST] = ambariServerHost
    decompressedMap[self.AMBARI_SERVER_PORT] = ambariServerPort
    decompressedMap[self.AMBARI_SERVER_USE_SSL] = ambariServerUseSsl

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

