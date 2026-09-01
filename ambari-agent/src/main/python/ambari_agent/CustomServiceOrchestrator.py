#!/usr/bin/env python3

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

import os
import sys
import uuid
import logging
import shutil
import tempfile
import threading
import json
from collections import defaultdict
from configparser import NoOptionError

from ambari_commons import shell
from ambari_commons.constants import AGENT_TMP_DIR
from ambari_commons.credential_store_helper import (
  create_credential_store_entry,
  credential_store_lock,
)
from resource_management.libraries.functions.log_process_information import (
  log_process_information,
)
from resource_management.core.encryption import ensure_decrypted, is_encrypted
from resource_management.core import shell as rmf_shell

from ambari_agent.models.commands import AgentCommand
from ambari_agent.Utils import Utils

from ambari_agent.AgentException import AgentException
from ambari_agent.PythonExecutor import PythonExecutor


logger = logging.getLogger()


class CustomServiceOrchestrator(object):
  """
  Executes a command for custom service. stdout and stderr are written to
  tmpoutfile and to tmperrfile respectively.
  """

  SCRIPT_TYPE_PYTHON = "PYTHON"
  COMMAND_TYPE = "commandType"
  COMMAND_NAME_STATUS = "STATUS"
  CUSTOM_ACTION_COMMAND = "ACTIONEXECUTE"
  CUSTOM_COMMAND_COMMAND = "CUSTOM_COMMAND"

  HOSTS_LIST_KEY = "all_hosts"
  PING_PORTS_KEY = "all_ping_ports"
  RACKS_KEY = "all_racks"
  IPV4_ADDRESSES_KEY = "all_ipv4_ips"

  AMBARI_SERVER_HOST = "ambari_server_host"
  AMBARI_SERVER_PORT = "ambari_server_port"
  AMBARI_SERVER_USE_SSL = "ambari_server_use_ssl"

  FREQUENT_COMMANDS = [COMMAND_NAME_STATUS]
  DONT_DEBUG_FAILURES_FOR_COMMANDS = FREQUENT_COMMANDS
  DONT_BACKUP_LOGS_FOR_COMMANDS = FREQUENT_COMMANDS

  # Path where hadoop credential JARS will be available
  DEFAULT_CREDENTIAL_SHELL_LIB_PATH = "/var/lib/ambari-agent/cred/lib"
  DEFAULT_CREDENTIAL_CONF_DIR = "/var/lib/ambari-agent/cred/conf"
  # The property name used by the hadoop credential provider
  CREDENTIAL_PROVIDER_PROPERTY_NAME = "hadoop.security.credential.provider.path"

  # Property name for credential store class path
  CREDENTIAL_STORE_CLASS_PATH_NAME = "credentialStoreClassPath"

  def __init__(self, initializer_module):
    self.initializer_module = initializer_module
    self.configuration_builder = initializer_module.configuration_builder
    self.host_level_params_cache = initializer_module.host_level_params_cache
    self.config = initializer_module.config
    self.hooks_orchestrator = initializer_module.hooks_orchestrator
    self.tmp_dir = self.config.get("agent", "prefix")
    self.force_https_protocol = self.config.get_force_https_protocol_name()
    self.ca_cert_file_path = self.config.get_ca_cert_file_path()
    self.exec_tmp_dir = AGENT_TMP_DIR
    self.file_cache = initializer_module.file_cache
    self.status_commands_stdout = os.path.join(
      self.tmp_dir, "status_command_stdout_{0}.txt"
    )
    self.status_commands_stderr = os.path.join(
      self.tmp_dir, "status_command_stderr_{0}.txt"
    )
    self.status_structured_out = os.path.join(
      self.tmp_dir, "status_structured-out-{0}.json"
    )

    # Construct the hadoop credential lib JARs path
    self.credential_shell_lib_path = os.path.join(
      self.config.get(
        "security", "credential_lib_dir", self.DEFAULT_CREDENTIAL_SHELL_LIB_PATH
      ),
      "*",
    )

    self.credential_conf_dir = self.config.get(
      "security", "credential_conf_dir", self.DEFAULT_CREDENTIAL_CONF_DIR
    )

    self.commands_in_progress_lock = threading.RLock()
    self.commands_in_progress = {}

    # save count (not boolean) for parallel execution cases
    self.commands_for_component_in_progress = defaultdict(
      lambda: defaultdict(lambda: 0)
    )
    self.encryption_key = None

  def map_task_to_process(self, task_id, processId):
    with self.commands_in_progress_lock:
      logger.debug("Maps taskId=%s to pid=%s", task_id, processId)
      self.commands_in_progress[(task_id, threading.get_ident())] = processId

  def cancel_command(self, task_id, reason):
    with self.commands_in_progress_lock:
      running = [
        (execution_key, process_id)
        for execution_key, process_id in self.commands_in_progress.items()
        if execution_key[0] == task_id and isinstance(process_id, int)
      ]
      for execution_key, _process_id in running:
        self.commands_in_progress[execution_key] = reason

    if not running:
      logger.warning(f"Unable to find process associated with taskId = {task_id}")
      return

    log_process_information(logger)
    for _execution_key, process_id in running:
      logger.info(
        "Canceling command with taskId=%s, reason=%s; killing process %s",
        task_id,
        reason,
        process_id,
      )
      try:
        shell.kill_process_with_children(process_id)
      except Exception:
        logger.exception(
          "Failed to terminate process group for command taskId=%s", task_id
        )

  def cancel_all_commands(self, reason):
    with self.commands_in_progress_lock:
      running = [
        (execution_key, process_id)
        for execution_key, process_id in self.commands_in_progress.items()
        if isinstance(process_id, int)
      ]
      for execution_key, _process_id in running:
        self.commands_in_progress[execution_key] = reason

    for execution_key, process_id in running:
      logger.info(
        "Canceling running command taskId=%s during Agent shutdown; killing "
        "process group rooted at pid=%s",
        execution_key[0],
        process_id,
      )
      try:
        shell.kill_process_with_children(process_id)
      except Exception:
        logger.exception(
          "Failed to terminate process group for command taskId=%s", execution_key[0]
        )

  def get_py_executor(self, forced_command_name):
    """
    Wrapper for unit testing
    :return:
    """
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

  @staticmethod
  def command_requires_encryption_key(value):
    if is_encrypted(value):
      return True
    if isinstance(value, dict):
      return any(
        CustomServiceOrchestrator.command_requires_encryption_key(item)
        for item in value.values()
      )
    if isinstance(value, (list, tuple)):
      return any(
        CustomServiceOrchestrator.command_requires_encryption_key(item)
        for item in value
      )
    return False

  def commandsRunningForComponent(self, clusterId, componentName):
    with self.commands_in_progress_lock:
      return self.commands_for_component_in_progress[clusterId][componentName] > 0

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
    credential_value_names = {}
    if (
      "serviceLevelParams" in commandJson
      and "configuration_credentials" in commandJson["serviceLevelParams"]
    ):
      for config_type, password_properties in commandJson["serviceLevelParams"][
        "configuration_credentials"
      ].items():
        if config_type in commandJson["configurations"]:
          value_names = []
          config = commandJson["configurations"][config_type]
          credentials = {}
          for key_name, value_name in password_properties.items():
            if key_name == value_name:
              if value_name in config:
                # password name is the alias
                credentials[key_name] = config[value_name]
                value_names.append(value_name)  # Gather the value_name for deletion
            else:
              keyname_keyconfig = key_name.split(":")
              key_name = keyname_keyconfig[0]
              # if the key is in another configuration (cross reference),
              # get the value of the key from that configuration
              if len(keyname_keyconfig) > 1:
                if keyname_keyconfig[1] not in commandJson["configurations"]:
                  continue
                key_config = commandJson["configurations"][keyname_keyconfig[1]]
              else:
                key_config = config
              if key_name in key_config and value_name in config:
                # password name points to the alias
                credentials[key_config[key_name]] = config[value_name]
                value_names.append(value_name)  # Gather the value_name for deletion
          if len(credentials) > 0:
            configtype_credentials[config_type] = credentials
            credential_value_names[config_type] = value_names
            logger.info(f"Identifying config {config_type} for CS: ")
    return configtype_credentials, credential_value_names

  def generateJceks(self, commandJson):
    """
    Generates the JCEKS file with passwords for the service specified in commandJson

    :param commandJson: command JSON
    :return: An exit value from the external process that generated the JCEKS file. None if
    there are no passwords in the JSON.
    """
    cmd_result = None
    roleCommand = None
    if "roleCommand" in commandJson:
      roleCommand = commandJson["roleCommand"]
    task_id = None
    if "taskId" in commandJson:
      task_id = commandJson["taskId"]

    logger.info(
      f"Generating the JCEKS file: roleCommand={roleCommand} and taskId = {task_id}"
    )

    # Set up the variables for the external command to generate a JCEKS file
    ambari_java_home = commandJson["ambariLevelParams"]["ambari_java_home"]
    java_bin = f"{ambari_java_home}/bin/java"

    cs_lib_path = self.credential_shell_lib_path
    serviceName = commandJson["serviceName"]

    # Gather the password values and remove them from the configuration
    configtype_credentials, credential_value_names = self.getConfigTypeCredentials(
      commandJson
    )

    # CS is enabled but no config property is available for this command
    if len(configtype_credentials) == 0:
      logger.info(
        "Credential store is enabled but no property are found that can be encrypted."
      )
      commandJson["credentialStoreEnabled"] = "false"
    # CS is enabled and config properties are available
    else:
      commandJson["credentialStoreEnabled"] = "true"

    for config_type, credentials in list(configtype_credentials.items()):
      config = commandJson["configurations"][config_type]
      if "role" in commandJson and commandJson["role"]:
        roleName = commandJson["role"]
        file_path = os.path.join(
          self.getProviderDirectory(roleName), f"{config_type}.jceks"
        )
      else:
        file_path = os.path.join(
          self.getProviderDirectory(serviceName), f"{config_type}.jceks"
        )
      provider_path = f"jceks://file{file_path}"
      logger.info(f"provider_path={provider_path}")
      credential_group = commandJson.get("configurations", {}).get(
        "cluster-env", {}
      ).get("user_group")
      if not credential_group:
        raise AgentException(
          "cluster-env/user_group is required for a private credential store"
        )
      provider_directory = os.path.dirname(file_path)
      os.makedirs(provider_directory, mode=0o750, exist_ok=True)
      shutil.chown(provider_directory, group=credential_group)
      os.chmod(provider_directory, 0o750)
      with credential_store_lock(provider_path):
        staging_directory = tempfile.mkdtemp(
          prefix=".jceks-", dir=provider_directory
        )
        temporary_path = os.path.join(
          staging_directory, os.path.basename(file_path)
        )
        temporary_provider_path = f"jceks://file{temporary_path}"
        try:
          for alias, pwd in credentials.items():
            pwd = ensure_decrypted(pwd, self.encryption_key)
            cmd_result = create_credential_store_entry(
              java_bin,
              cs_lib_path,
              alias,
              temporary_provider_path,
              pwd,
            )
            if cmd_result != 0:
              raise AgentException(
                "Credential store helper failed to create "
                f"{file_path} with exit code {cmd_result}"
              )

          if not os.path.isfile(temporary_path):
            raise AgentException(
              f"Credential store helper did not create expected store {temporary_path}"
            )
          shutil.chown(temporary_path, group=credential_group)
          os.chmod(temporary_path, 0o640)
          file_descriptor = os.open(temporary_path, os.O_RDONLY)
          try:
            os.fsync(file_descriptor)
          finally:
            os.close(file_descriptor)
          checksum_path = os.path.join(
            provider_directory, f".{os.path.basename(file_path)}.crc"
          )
          directory_descriptor = os.open(provider_directory, os.O_RDONLY)
          try:
            try:
              os.unlink(checksum_path)
            except FileNotFoundError:
              pass
            os.fsync(directory_descriptor)
            os.replace(temporary_path, file_path)
            os.fsync(directory_descriptor)
          finally:
            os.close(directory_descriptor)
        finally:
          shutil.rmtree(staging_directory, ignore_errors=True)
      # Add JCEKS provider path instead
      config[self.CREDENTIAL_PROVIDER_PROPERTY_NAME] = provider_path
      config[self.CREDENTIAL_STORE_CLASS_PATH_NAME] = cs_lib_path

    for config_type, value_names in credential_value_names.items():
      config = commandJson["configurations"][config_type]
      for value_name in value_names:
        config.pop(value_name, None)

    return cmd_result

  def runCommand(
    self,
    command_header,
    tmpoutfile,
    tmperrfile,
    forced_command_name=None,
    override_output_files=True,
    retry=False,
    is_status_command=False,
    tmpstrucoutfile=None,
    cancel_event=None,
  ):
    """
    forced_command_name may be specified manually. In this case, value, defined at
    command json, is ignored.
    """
    incremented_commands_for_component = False

    ret = None
    json_path = None
    handle = None

    try:
      command = self.generate_command(command_header)
      script_type = command["commandParams"]["script_type"]
      script = command["commandParams"]["script"]
      timeout = int(command["commandParams"]["command_timeout"])
      cluster_id = str(command["clusterId"])

      # Status commands have no taskId nor roleCommand
      if not is_status_command:
        task_id = command["taskId"]
        command_name = command["roleCommand"]
      else:
        task_id = "status"
        command_name = None

      if forced_command_name is not None:  # If not supplied as an argument
        command_name = forced_command_name

      if command_name and command_name == self.CUSTOM_ACTION_COMMAND:
        base_dir = self.file_cache.get_custom_actions_base_dir(command)
        script_tuple = (os.path.join(base_dir, "scripts", script), base_dir)
      else:
        if command_name == self.CUSTOM_COMMAND_COMMAND:
          command_name = command["commandParams"]["custom_command"]

        # forces a hash challenge on the directories to keep them updated, even
        # if the return type is not used
        self.file_cache.get_host_scripts_base_dir(command)
        base_dir = self.file_cache.get_service_base_dir(command)
        script_path = self.resolve_script_path(base_dir, script)
        script_tuple = (script_path, base_dir)

      if not tmpstrucoutfile:
        tmpstrucoutfile = os.path.join(self.tmp_dir, f"structured-out-{task_id}.json")

      # We don't support anything else yet
      if script_type.upper() != self.SCRIPT_TYPE_PYTHON:
        message = f"Unknown script type {script_type}"
        raise AgentException(message)

      # Execute command using proper interpreter
      if "__handle" in command:
        handle = command["__handle"]
        handle.on_background_command_started = self.map_task_to_process
        del command["__handle"]

      # If command contains credentialStoreEnabled, then
      # generate the JCEKS file for the configurations.
      credential_store_enabled = False
      if (
        "serviceLevelParams" in command
        and "credentialStoreEnabled" in command["serviceLevelParams"]
      ):
        credential_store_enabled = command["serviceLevelParams"][
          "credentialStoreEnabled"
        ]

      if credential_store_enabled and command_name != self.COMMAND_NAME_STATUS:
        if (
          "commandBeingRetried" not in command["agentLevelParams"]
          or command["agentLevelParams"]["commandBeingRetried"] != "true"
        ):
          self.generateJceks(command)
        else:
          logger.info(
            "Skipping generation of jceks files as this is a retry of the command"
          )

      json_path = self.dump_command_to_json(command, retry, is_status_command)
      hooks = self.hooks_orchestrator.resolve_hooks(command, command_name)
      """:type hooks ambari_agent.CommandHooksOrchestrator.ResolvedHooks"""

      py_file_list = []
      if hooks:
        py_file_list.extend(hooks.pre_hooks)

      py_file_list.append(script_tuple)

      if hooks:
        py_file_list.extend(hooks.post_hooks)

      # filter None values
      filtered_py_file_list = [i for i in py_file_list if i]

      logger_level = logging.getLevelName(logger.level)

      # Executing hooks and script
      ret = None

      if (
        "commandType" in command
        and command["commandType"] == AgentCommand.background_execution
        and len(filtered_py_file_list) > 1
      ):
        raise AgentException("Background commands are supported without hooks only")

      command_env = (
        {"AGENT_ENCRYPTION_KEY": self.encryption_key}
        if self.encryption_key and self.command_requires_encryption_key(command)
        else None
      )

      python_executor = self.get_py_executor(forced_command_name)
      backup_log_files = command_name not in self.DONT_BACKUP_LOGS_FOR_COMMANDS
      try:
        log_out_files = (
          self.config.get("logging", "log_out_files", default=None) is not None
        )
      except NoOptionError:
        log_out_files = None

      if cluster_id != "-1" and cluster_id != "null" and not is_status_command:
        with self.commands_in_progress_lock:
          self.commands_for_component_in_progress[cluster_id][command["role"]] += 1
        incremented_commands_for_component = True

        if "serviceName" in command:
          service_component_name = command["serviceName"] + "/" + command["role"]
          # reset status which was reported, so agent re-reports it after command finished
          self.initializer_module.component_status_executor.reported_component_status[
            cluster_id
          ][service_component_name]["STATUS"] = None

      for py_file, current_base_dir in filtered_py_file_list:
        log_info_on_failure = command_name not in self.DONT_DEBUG_FAILURES_FOR_COMMANDS
        script_params = [
          command_name,
          json_path,
          current_base_dir,
          tmpstrucoutfile,
          logger_level,
          self.exec_tmp_dir,
          self.force_https_protocol,
          self.ca_cert_file_path,
        ]

        if log_out_files:
          script_params.append("-o")

        ret = python_executor.run_file(
          py_file,
          script_params,
          tmpoutfile,
          tmperrfile,
          timeout,
          tmpstrucoutfile,
          self.map_task_to_process,
          task_id,
          override_output_files,
          backup_log_files=backup_log_files,
          handle=handle,
          log_info_on_failure=log_info_on_failure,
          env=command_env,
          cancel_event=cancel_event,
        )
        # Next run_file() invocations should always append to current output
        override_output_files = False
        if ret["exitcode"] != 0:
          break

      if not ret:
        raise AgentException("No script has been executed")

      # if canceled and not background command
      if handle is None:
        cancel_reason = self.command_canceled_reason(task_id)
        if cancel_reason is not None:
          ret["stdout"] += cancel_reason
          ret["stderr"] += cancel_reason

          with open(tmpoutfile, "a") as f:
            f.write(cancel_reason)
          with open(tmperrfile, "a") as f:
            f.write(cancel_reason)

    except Exception as e:
      exc_type, exc_obj, exc_tb = sys.exc_info()
      message = f"Caught an exception while executing custom service command: {exc_type}: {exc_obj}; {e}"
      logger.exception(message)
      ret = {
        "stdout": message,
        "stderr": message,
        "structuredOut": "{}",
        "exitcode": 1,
      }
    finally:
      if incremented_commands_for_component:
        with self.commands_in_progress_lock:
          self.commands_for_component_in_progress[cluster_id][command["role"]] -= 1

      if handle is None and "task_id" in locals():
        with self.commands_in_progress_lock:
          self.commands_in_progress.pop(
            (task_id, threading.get_ident()), None
          )

      if json_path:
        if is_status_command:
          try:
            os.unlink(json_path)
          except OSError:
            pass  # Ignore failure
        else:
          self.conditionally_remove_command_file(json_path, ret)

    return ret

  def command_canceled_reason(self, task_id):
    with self.commands_in_progress_lock:
      execution_key = (task_id, threading.get_ident())
      if execution_key in self.commands_in_progress:
        logger.debug("Pop with taskId %s", task_id)
        pid = self.commands_in_progress.pop(execution_key)
        if not isinstance(pid, int):
          reason = pid
          if reason:
            return f"\nCommand aborted. Reason: '{reason}'"
          else:
            return "\nCommand aborted."
    return None

  def generate_command(self, command_header):
    cluster_id = str(command_header["clusterId"])

    if cluster_id != "-1" and cluster_id != "null":
      service_name = command_header["serviceName"]
      component_name = command_header["role"]
    else:
      cluster_id = None
      service_name = None
      component_name = None

    required_config_timestamp = (
      command_header["requiredConfigTimestamp"]
      if "requiredConfigTimestamp" in command_header
      else None
    )

    command_dict = self.configuration_builder.get_configuration(
      cluster_id, service_name, component_name, required_config_timestamp
    )

    # remove data populated from topology to avoid merge and just override
    if "clusterHostInfo" in command_header:
      del command_dict["clusterHostInfo"]

    command = Utils.update_nested(Utils.get_mutable_copy(command_dict), command_header)

    # topology needs to be decompressed if and only if it originates from command header
    if "clusterHostInfo" in command_header and command_header["clusterHostInfo"]:
      command["clusterHostInfo"] = self.decompress_cluster_host_info(
        command["clusterHostInfo"]
      )

    return command

  def requestComponentStatus(self, command_header, command_name="STATUS"):
    """
    Component status is determined by exit code, returned by runCommand().
    Exit code 0 means that component is running and any other exit code means that
    component is not running
    """
    override_output_files = True
    if logger.level == logging.DEBUG:
      override_output_files = False

    # make sure status commands that run in parallel don't use the same files
    status_commands_stdout = self.status_commands_stdout.format(uuid.uuid4())
    status_commands_stderr = self.status_commands_stderr.format(uuid.uuid4())
    status_structured_out = self.status_structured_out.format(uuid.uuid4())

    try:
      res = self.runCommand(
        command_header,
        status_commands_stdout,
        status_commands_stderr,
        command_name,
        override_output_files=override_output_files,
        is_status_command=True,
        tmpstrucoutfile=status_structured_out,
      )
    finally:
      try:
        os.unlink(status_commands_stdout)
        os.unlink(status_commands_stderr)
        os.unlink(status_structured_out)
      except OSError:
        pass  # Ignore failure

    return res

  def resolve_script_path(self, base_dir, script):
    """
    Encapsulates logic of script location determination.
    """
    path = os.path.join(base_dir, script)
    if not os.path.exists(path):
      message = f"Script {path} does not exist"
      raise AgentException(message)
    return path

  def dump_command_to_json(self, command, retry=False, is_status_command=False):
    """
    Converts command to json file and returns file path
    """
    # Now, dump the json file
    command_type = command["commandType"]

    if is_status_command:
      # make sure status commands that run in parallel don't use the same files
      file_path = os.path.join(self.tmp_dir, f"status_command_{uuid.uuid4()}.json")
    else:
      task_id = command["taskId"]
      file_path = os.path.join(self.tmp_dir, f"command-{task_id}.json")
      if command_type == AgentCommand.auto_execution:
        file_path = os.path.join(self.tmp_dir, f"auto_command-{task_id}.json")

    # JSON may contain passwords. Write a private file and publish it atomically
    # so retries cannot expose stale suffixes or follow a substituted symlink.
    descriptor, temporary_path = tempfile.mkstemp(
      prefix=f".{os.path.basename(file_path)}-", dir=self.tmp_dir, text=True
    )
    try:
      os.fchmod(descriptor, 0o600)
      with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
        descriptor = None
        json.dump(command, stream, sort_keys=False, indent=4)
        stream.flush()
        os.fsync(stream.fileno())
      os.replace(temporary_path, file_path)
      temporary_path = None
      directory_descriptor = os.open(self.tmp_dir, os.O_RDONLY)
      try:
        os.fsync(directory_descriptor)
      finally:
        os.close(directory_descriptor)
    finally:
      if descriptor is not None:
        os.close(descriptor)
      if temporary_path is not None:
        try:
          os.unlink(temporary_path)
        except OSError:
          pass
    return file_path

  def decompress_cluster_host_info(self, cluster_host_info):
    info = cluster_host_info.copy()
    hosts_list = info.pop(self.HOSTS_LIST_KEY)
    ping_ports = info.pop(self.PING_PORTS_KEY)
    racks = info.pop(self.RACKS_KEY)
    ipv4_addresses = info.pop(self.IPV4_ADDRESSES_KEY)

    ambari_server_host = info.pop(self.AMBARI_SERVER_HOST)
    ambari_server_port = info.pop(self.AMBARI_SERVER_PORT)
    ambari_server_use_ssl = info.pop(self.AMBARI_SERVER_USE_SSL)

    decompressed_map = {}

    for k, v in info.items():
      # Convert from 1-3,5,6-8 to [1,2,3,5,6,7,8]
      indexes = self.convert_range_to_list(v)
      # Convert from [1,2,3,5,6,7,8] to [host1,host2,host3...]
      decompressed_map[k] = [hosts_list[i] for i in indexes]

    # Convert from ['1:0-2,4', '42:3,5-7'] to [1,1,1,42,1,42,42,42]
    ping_ports = self.convert_mapped_range_to_list(ping_ports)
    racks = self.convert_mapped_range_to_list(racks)
    ipv4_addresses = self.convert_mapped_range_to_list(ipv4_addresses)

    ping_ports = list(map(str, ping_ports))

    decompressed_map[self.PING_PORTS_KEY] = ping_ports
    decompressed_map[self.HOSTS_LIST_KEY] = hosts_list
    decompressed_map[self.RACKS_KEY] = racks
    decompressed_map[self.IPV4_ADDRESSES_KEY] = ipv4_addresses
    decompressed_map[self.AMBARI_SERVER_HOST] = ambari_server_host
    decompressed_map[self.AMBARI_SERVER_PORT] = ambari_server_port
    decompressed_map[self.AMBARI_SERVER_USE_SSL] = ambari_server_use_ssl

    return decompressed_map

  def convert_range_to_list(self, range_to_convert):
    """
    Converts from 1-3,5,6-8 to [1,2,3,5,6,7,8]

    :type range_to_convert list
    """
    result_list = []

    for i in range_to_convert:
      ranges = i.split(",")

      for r in ranges:
        range_bounds = r.split("-")
        if len(range_bounds) == 2:
          if not range_bounds[0] or not range_bounds[1]:
            raise AgentException(
              "Broken data in given range, expected - "
              "m-n"
              " or "
              "m"
              ", got: " + str(r)
            )

          result_list.extend(
            list(range(int(range_bounds[0]), int(range_bounds[1]) + 1))
          )
        elif len(range_bounds) == 1:
          result_list.append((int(range_bounds[0])))
        else:
          raise AgentException(
            "Broken data in given range, expected - "
            "m-n"
            " or "
            "m"
            ", got: " + str(r)
          )

    return result_list

  def convert_mapped_range_to_list(self, range_to_convert):
    """
    Converts from ['1:0-2,4', '42:3,5-7'] to [1,1,1,42,1,42,42,42]

    :type range_to_convert list
    """
    result_dict = {}

    for i in range_to_convert:
      value_to_ranges = i.split(":")
      if len(value_to_ranges) != 2:
        raise AgentException(
          "Broken data in given value to range, expected format - "
          "value:m-n"
          ", got - " + str(i)
        )
      value = value_to_ranges[0]
      ranges_token = value_to_ranges[1]

      for r in ranges_token.split(","):
        range_indexes = r.split("-")

        if len(range_indexes) == 2:
          if not range_indexes[0] or not range_indexes[1]:
            raise AgentException(
              "Broken data in given value to range, expected format - "
              "value:m-n"
              ", got - " + str(r)
            )

          start = int(range_indexes[0])
          end = int(range_indexes[1])

          for k in range(start, end + 1):
            result_dict[k] = value if not value.isdigit() else int(value)

        elif len(range_indexes) == 1:
          index = int(range_indexes[0])
          result_dict[index] = value if not value.isdigit() else int(value)

    return list(dict(sorted(result_dict.items())).values())

  def conditionally_remove_command_file(self, command_json_path, command_result):
    """
    Conditionally remove the specified command JSON file if it exists and if the configured
    agent/command_file_retention_policy indicates to do so.

    :param command_json_path:  the absolute path to the command JSON file
    :param command_result: the result structure containing the exit code for the command execution
    :rtype: bool
    :return: True, if the command JSON file was removed; False otherwise
    """
    removed_command_file = False

    if os.path.exists(command_json_path):
      command_file_retention_policy = self.config.command_file_retention_policy

      if (
        command_file_retention_policy
        == self.config.COMMAND_FILE_RETENTION_POLICY_REMOVE
      ):
        remove_command_file = True
        logger.info(
          "Removing %s due to the command_file_retention_policy, %s",
          command_json_path,
          command_file_retention_policy,
        )
      elif (
        command_file_retention_policy
        == self.config.COMMAND_FILE_RETENTION_POLICY_REMOVE_ON_SUCCESS
      ):
        if command_result and ("exitcode" in command_result):
          exit_code = command_result["exitcode"]
          if exit_code == 0:
            remove_command_file = True
            logger.info(
              "Removing %s due to the command_file_retention_policy, %s, and exit code, %d",
              command_json_path,
              command_file_retention_policy,
              exit_code,
            )
          else:
            remove_command_file = False
            logger.info(
              "Not removing %s due to the command_file_retention_policy, %s, and exit code, %d",
              command_json_path,
              command_file_retention_policy,
              exit_code,
            )
        else:
          remove_command_file = False
          logger.info(
            "Not Removing %s due to the command_file_retention_policy, %s, and a missing exit code value",
            command_json_path,
            command_file_retention_policy,
          )
      else:
        remove_command_file = False

      if remove_command_file:
        try:
          os.remove(command_json_path)
          removed_command_file = True
        except OSError as e:
          logger.error(
            "Failed to remove %s due to error: %s", command_json_path, str(e)
          )

    return removed_command_file
