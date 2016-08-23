#!/usr/bin/env python
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

import sys
from storm import storm
from service import service
from service_check import ServiceCheck
from resource_management.libraries.functions import check_process_status
from resource_management.libraries.script import Script
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from resource_management.core.resources.system import Link
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_JAAS_CONF
from setup_ranger_storm import setup_ranger_storm
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core.resources.service import Service


class UiServer(Script):

  def get_component_name(self):
    return "storm-client"

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    storm("ui")

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class UiServerWindows(UiServer):
  def start(self, env):
    import status_params
    env.set_params(status_params)
    self.configure(env)
    Service(status_params.ui_win_service_name, action="start")

  def stop(self, env):
    import status_params
    env.set_params(status_params)
    Service(status_params.ui_win_service_name, action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_status
    check_windows_service_status(status_params.ui_win_service_name)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class UiServerDefault(UiServer):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      conf_select.select(params.stack_name, "storm", params.version)
      stack_select.select("storm-client", params.version)

  def link_metrics_sink_jar(self):
    import params
    # Add storm metrics reporter JAR to storm-ui-server classpath.
    # Remove symlinks. They can be there, if you doing upgrade from HDP < 2.2 to HDP >= 2.2
    Link(format("{storm_lib_dir}/ambari-metrics-storm-sink.jar"),
         action="delete")
    # On old HDP 2.1 versions, this symlink may also exist and break EU to newer versions
    Link("/usr/lib/storm/lib/ambari-metrics-storm-sink.jar", action="delete")

    if check_stack_feature(StackFeature.STORM_METRICS_APACHE_CLASSES, params.version_for_stack_feature_checks):
      sink_jar = params.metric_collector_sink_jar
    else:
      sink_jar = params.metric_collector_legacy_sink_jar

    Execute(format("{sudo} ln -s {sink_jar} {storm_lib_dir}/ambari-metrics-storm-sink.jar"),
            not_if=format("ls {storm_lib_dir}/ambari-metrics-storm-sink.jar"),
            only_if=format("ls {sink_jar}")
            )

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    self.link_metrics_sink_jar()
    setup_ranger_storm(upgrade_type=upgrade_type)
    service("ui", action="start")

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    service("ui", action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.pid_ui)

  def security_status(self, env):
    import status_params

    env.set_params(status_params)

    if status_params.security_enabled:
      # Expect the following files to be available in status_params.config_dir:
      #   storm_jaas.conf

      try:
        props_value_check = None
        props_empty_check = ['storm_ui_principal_name', 'storm_ui_keytab']
        props_read_check = ['storm_ui_keytab']
        storm_env_expectations = build_expectations('storm_ui', props_value_check, props_empty_check,
                                                 props_read_check)

        storm_expectations = {}
        storm_expectations.update(storm_env_expectations)

        security_params = {}
        security_params['storm_ui'] = {}
        security_params['storm_ui']['storm_ui_principal_name'] = status_params.storm_ui_principal
        security_params['storm_ui']['storm_ui_keytab'] = status_params.storm_ui_keytab

        result_issues = validate_security_config_properties(security_params, storm_expectations)
        if not result_issues:  # If all validations passed successfully
          # Double check the dict before calling execute
          if ( 'storm_ui' not in security_params
               or 'storm_ui_principal_name' not in security_params['storm_ui']
               or 'storm_ui_keytab' not in security_params['storm_ui']):
            self.put_structured_out({"securityState": "ERROR"})
            self.put_structured_out({"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.storm_user,
                                security_params['storm_ui']['storm_ui_keytab'],
                                security_params['storm_ui']['storm_ui_principal_name'],
                                status_params.hostname,
                                status_params.tmp_dir)
          self.put_structured_out({"securityState": "SECURED_KERBEROS"})
        else:
          issues = []
          for cf in result_issues:
            issues.append("Configuration file %s did not pass the validation. Reason: %s" % (cf, result_issues[cf]))
          self.put_structured_out({"securityIssuesFound": ". ".join(issues)})
          self.put_structured_out({"securityState": "UNSECURED"})
      except Exception as e:
        self.put_structured_out({"securityState": "ERROR"})
        self.put_structured_out({"securityStateErrorInfo": str(e)})
    else:
      self.put_structured_out({"securityState": "UNSECURED"})
      
  def get_log_folder(self):
    import params
    return params.log_dir
  
  def get_user(self):
    import params
    return params.storm_user

  def get_pid_files(self):
    import status_params
    return [status_params.pid_ui]

if __name__ == "__main__":
  UiServer().execute()
