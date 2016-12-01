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
# Python Imports
import os

# Local Imports
from metadata import metadata
from resource_management import Fail
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.core.resources.system import Execute, File
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.security_commons import build_expectations, \
  get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_PROPERTIES
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.functions.stack_features import check_stack_feature, get_stack_feature_version
from resource_management.libraries.functions.constants import StackFeature
from resource_management.core.resources.system import Directory
from resource_management.core.logger import Logger
from setup_ranger_atlas import setup_ranger_atlas


class MetadataServer(Script):

  def get_component_name(self):
    return "atlas-server"

  def install(self, env):
    import params
    env.set_params(params)

    Directory(format("{expanded_war_dir}/atlas"),
              action = "delete",
    )

    self.install_packages(env)

  def configure(self, env, upgrade_type=None, config_dir=None):
    import params
    env.set_params(params)
    metadata()

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if check_stack_feature(StackFeature.ATLAS_UPGRADE_SUPPORT, params.version):
      conf_select.select(params.stack_name, "atlas", params.version)
      stack_select.select("atlas-server", params.version)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)

    daemon_cmd = format('source {params.conf_dir}/atlas-env.sh ; {params.metadata_start_script}')
    no_op_test = format('ls {params.pid_file} >/dev/null 2>&1 && ps -p `cat {params.pid_file}` >/dev/null 2>&1')
    atlas_hbase_setup_command = format("cat {atlas_hbase_setup} | hbase shell -n")
    atlas_kafka_setup_command = format("bash {atlas_kafka_setup}")
    secure_atlas_hbase_setup_command = format("kinit -kt {hbase_user_keytab} {hbase_principal_name}; ") + atlas_hbase_setup_command
    # in case if principal was distributed across several hosts, pattern need to be replaced to right one
    secure_atlas_kafka_setup_command = format("kinit -kt {kafka_keytab} {kafka_principal_name}; ").replace("_HOST", params.hostname) + atlas_kafka_setup_command

    if params.stack_supports_atlas_ranger_plugin:
      Logger.info('Atlas plugin is enabled, configuring Atlas plugin.')
      setup_ranger_atlas(upgrade_type=upgrade_type)
    else:
      Logger.info('Atlas plugin is not supported or enabled.')

    try:
      effective_version = get_stack_feature_version(params.config)

      if check_stack_feature(StackFeature.ATLAS_HBASE_SETUP, effective_version):
        if params.security_enabled and params.has_hbase_master:
          Execute(secure_atlas_hbase_setup_command,
                  tries = 5,
                  try_sleep = 10,
                  user=params.hbase_user
          )
        elif params.enable_ranger_hbase and not params.security_enabled:
          Execute(atlas_hbase_setup_command,
                  tries = 5,
                  try_sleep = 10,
                  user=params.hbase_user
          )

      if check_stack_feature(StackFeature.ATLAS_UPGRADE_SUPPORT, effective_version) and params.security_enabled:
        try:
          Execute(secure_atlas_kafka_setup_command,
                  user=params.kafka_user,
                  tries=5,
                  try_sleep=10
          )
        except Fail:
          pass  # do nothing and do not block Atlas start, fail logs would be available via Execute internals

      Execute(daemon_cmd,
              user=params.metadata_user,
              not_if=no_op_test
      )
    except:
      show_logs(params.log_dir, params.metadata_user)
      raise

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    daemon_cmd = format('source {params.conf_dir}/atlas-env.sh; {params.metadata_stop_script}')

    # If the pid dir doesn't exist, this means either
    # 1. The user just added Atlas service and issued a restart command (stop+start). So stop should be a no-op
    # since there's nothing to stop.
    # OR
    # 2. The user changed the value of the pid dir config and incorrectly issued a restart command.
    # In which case the stop command cannot do anything since Ambari doesn't know which process to kill.
    # The start command will spawn another instance.
    # The user should have issued a stop, changed the config, and then started it.
    if not os.path.isdir(params.pid_dir):
      Logger.info("*******************************************************************")
      Logger.info("Will skip the stop command since this is the first time stopping/restarting Atlas "
                  "and the pid dir does not exist, %s\n" % params.pid_dir)
      return

    try:
      Execute(daemon_cmd,
              user=params.metadata_user,
      )
    except:
      show_logs(params.log_dir, params.metadata_user)
      raise

    File(params.pid_file, action="delete")

  def status(self, env):
    import status_params

    env.set_params(status_params)
    check_process_status(status_params.pid_file)

  def security_status(self, env):
    import status_params

    env.set_params(status_params)

    file_name_key = 'applicaton'
    props_value_check = {'atlas.authentication.method': 'kerberos',
                         'atlas.http.authentication.enabled': 'true',
                         'atlas.http.authentication.type': 'kerberos'}
    props_empty_check = ['atlas.authentication.principal',
                         'atlas.authentication.keytab',
                         'atlas.http.authentication.kerberos.principal',
                         'atlas.http.authentication.kerberos.keytab']
    props_read_check = ['atlas.authentication.keytab',
                        'atlas.http.authentication.kerberos.keytab']

    if check_stack_feature(StackFeature.ATLAS_UPGRADE_SUPPORT, status_params.version_for_stack_feature_checks):
      file_name_key = 'atlas-application'
      props_value_check = {'atlas.authentication.method.kerberos': 'true',
                           'atlas.solr.kerberos.enable': 'true'}
      props_empty_check = ['atlas.authentication.principal',
                           'atlas.authentication.keytab',
                           'atlas.authentication.method.kerberos.principal',
                           'atlas.authentication.method.kerberos.keytab']
      props_read_check = ['atlas.authentication.keytab',
                          'atlas.authentication.method.kerberos.keytab']

    atlas_site_expectations = build_expectations(file_name_key,
                                                 props_value_check,
                                                 props_empty_check,
                                                 props_read_check)

    atlas_expectations = {}
    atlas_expectations.update(atlas_site_expectations)

    security_params = get_params_from_filesystem(status_params.conf_dir,
                                                 {status_params.conf_file: FILE_TYPE_PROPERTIES})
    result_issues = validate_security_config_properties(security_params, atlas_expectations)

    if not result_issues:  # If all validations passed successfully
      try:
        # Double check the dict before calling execute
        if ( file_name_key not in security_params
             or 'atlas.authentication.keytab' not in security_params[file_name_key]
             or 'atlas.authentication.principal' not in security_params[file_name_key]):
          self.put_structured_out({"securityState": "UNSECURED"})
          self.put_structured_out(
            {"securityIssuesFound": "Atlas service keytab file or principal are not set property."})
          return

        if check_stack_feature(StackFeature.ATLAS_UPGRADE_SUPPORT, status_params.version_for_stack_feature_checks):
          if ( file_name_key not in security_params
               or 'atlas.authentication.method.kerberos.keytab' not in security_params[file_name_key]
               or 'atlas.authentication.method.kerberos.principal' not in security_params[file_name_key]):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Method Authentication keytab file or principal are not set property."})
            return
        else:
          if ( file_name_key not in security_params
               or 'atlas.http.authentication.kerberos.keytab' not in security_params[file_name_key]
               or 'atlas.http.authentication.kerberos.principal' not in security_params[file_name_key]):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "HTTP Authentication keytab file or principal are not set property."})
            return

        self.put_structured_out({"securityState": "SECURED_KERBEROS"})
      except Exception as e:
        self.put_structured_out({"securityState": "ERROR"})
        self.put_structured_out({"securityStateErrorInfo": str(e)})
    else:
      issues = []
      for cf in result_issues:
        issues.append("Configuration file %s did not pass the validation. Reason: %s" % (cf, result_issues[cf]))
      self.put_structured_out({"securityIssuesFound": ". ".join(issues)})
      self.put_structured_out({"securityState": "UNSECURED"})

  def get_log_folder(self):
    import params

    return params.log_dir

  def get_user(self):
    import params

    return params.metadata_user


  def get_pid_files(self):
    import status_params
    return [status_params.pid_file]

if __name__ == "__main__":
  MetadataServer().execute()
