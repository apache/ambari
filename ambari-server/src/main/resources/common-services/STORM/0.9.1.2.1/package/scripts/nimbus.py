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
from resource_management.libraries.functions import check_process_status
from resource_management.libraries.script import Script
from resource_management.libraries.functions import format
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from storm import storm
from service import service
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_JAAS_CONF
from setup_ranger_storm import setup_ranger_storm
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.core.resources.service import Service

class Nimbus(Script):
  def get_stack_to_component(self):
    return {"HDP": "storm-nimbus"}

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    storm("nimbus")


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class NimbusDefault(Nimbus):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      conf_select.select(params.stack_name, "storm", params.version)
      hdp_select.select("storm-client", params.version)
      hdp_select.select("storm-nimbus", params.version)


  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    setup_ranger_storm(upgrade_type=upgrade_type)
    service("nimbus", action="start")


  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    service("nimbus", action="stop")


  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.pid_nimbus)


  def security_status(self, env):
    import status_params
    env.set_params(status_params)
    if status_params.security_enabled:
      # Expect the following files to be available in status_params.config_dir:
      #   storm_jaas.conf
      try:
        props_value_check = None
        props_empty_check = ['StormServer/keyTab', 'StormServer/principal']
        props_read_check = ['StormServer/keyTab']
        storm_env_expectations = build_expectations('storm_jaas', props_value_check, props_empty_check,  props_read_check)
        storm_expectations = {}
        storm_expectations.update(storm_env_expectations)
        security_params = get_params_from_filesystem(status_params.conf_dir, {'storm_jaas.conf': FILE_TYPE_JAAS_CONF})
        result_issues = validate_security_config_properties(security_params, storm_expectations)
        if not result_issues:  # If all validations passed successfully
          # Double check the dict before calling execute
          if ( 'storm_jaas' not in security_params
               or 'StormServer' not in security_params['storm_jaas']
               or 'keyTab' not in security_params['storm_jaas']['StormServer']
               or 'principal' not in security_params['storm_jaas']['StormServer']):
            self.put_structured_out({"securityState": "ERROR"})
            self.put_structured_out({"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.storm_user,
                                security_params['storm_jaas']['StormServer']['keyTab'],
                                security_params['storm_jaas']['StormServer']['principal'],
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


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class NimbusWindows(Nimbus):
  def start(self, env):
    import status_params
    env.set_params(status_params)
    Service(status_params.nimbus_win_service_name, action="start")

  def stop(self, env):
    import status_params
    env.set_params(status_params)
    Service(status_params.nimbus_win_service_name, action="stop")

  def status(self, env):
    import status_params
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_status
    env.set_params(status_params)
    check_windows_service_status(status_params.nimbus_win_service_name)

if __name__ == "__main__":
  Nimbus().execute()
