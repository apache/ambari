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

import falcon_server_upgrade

from resource_management.core.logger import Logger
from resource_management.libraries.script import Script
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import check_process_status
from resource_management.libraries.functions.security_commons import build_expectations
from resource_management.libraries.functions.security_commons import cached_kinit_executor
from resource_management.libraries.functions.security_commons import get_params_from_filesystem
from resource_management.libraries.functions.security_commons import validate_security_config_properties
from resource_management.libraries.functions.security_commons import FILE_TYPE_PROPERTIES
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature, Direction

from falcon import falcon
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl


class FalconServer(Script):
  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    falcon('server', action='config', upgrade_type=upgrade_type)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    falcon('server', action='start', upgrade_type=upgrade_type)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    falcon('server', action='stop', upgrade_type=upgrade_type)

    # if performing an upgrade (ROLLING / NON_ROLLING), backup some directories after stopping falcon
    if upgrade_type is not None:
      falcon_server_upgrade.post_stop_backup()

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class FalconServerLinux(FalconServer):
  def get_component_name(self):
    return "falcon-server"

  def install(self, env):
    import params
    self.install_packages(env)
    env.set_params(params)

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.server_pid_file)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # the stack does not support rolling upgrade
    if not (params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version)):
      return

    Logger.info("Executing Falcon Server Stack Upgrade pre-restart")
    conf_select.select(params.stack_name, "falcon", params.version)
    stack_select.select("falcon-server", params.version)

    falcon_server_upgrade.pre_start_restore()

  def security_status(self, env):
    import status_params
    env.set_params(status_params)
    if status_params.security_enabled:
      props_value_check = {"*.falcon.authentication.type": "kerberos",
                           "*.falcon.http.authentication.type": "kerberos"}
      props_empty_check = ["*.falcon.service.authentication.kerberos.principal",
                           "*.falcon.service.authentication.kerberos.keytab",
                           "*.falcon.http.authentication.kerberos.principal",
                           "*.falcon.http.authentication.kerberos.keytab"]
      props_read_check = ["*.falcon.service.authentication.kerberos.keytab",
                          "*.falcon.http.authentication.kerberos.keytab"]
      falcon_startup_props = build_expectations('startup', props_value_check, props_empty_check,
                                                  props_read_check)

      falcon_expectations ={}
      falcon_expectations.update(falcon_startup_props)

      security_params = get_params_from_filesystem('/etc/falcon/conf',
                                                   {'startup.properties': FILE_TYPE_PROPERTIES})
      result_issues = validate_security_config_properties(security_params, falcon_expectations)
      if not result_issues: # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ( 'startup' not in security_params
               or '*.falcon.service.authentication.kerberos.keytab' not in security_params['startup']
               or '*.falcon.service.authentication.kerberos.principal' not in security_params['startup']) \
            or '*.falcon.http.authentication.kerberos.keytab' not in security_params['startup'] \
            or '*.falcon.http.authentication.kerberos.principal' not in security_params['startup']:
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.falcon_user,
                                security_params['startup']['*.falcon.service.authentication.kerberos.keytab'],
                                security_params['startup']['*.falcon.service.authentication.kerberos.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.falcon_user,
                                security_params['startup']['*.falcon.http.authentication.kerberos.keytab'],
                                security_params['startup']['*.falcon.http.authentication.kerberos.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
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
    else:
      self.put_structured_out({"securityState": "UNSECURED"})

  def get_log_folder(self):
    import params
    return params.falcon_log_dir
  
  def get_user(self):
    import params
    return params.falcon_user

  def get_pid_files(self):
    import status_params
    return [status_params.server_pid_file]

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class FalconServerWindows(FalconServer):

  def install(self, env):
    import params
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_exists

    if not check_windows_service_exists(params.falcon_win_service_name):
      self.install_packages(env)

  def status(self, env):
    import status_params
    from resource_management.libraries.functions.windows_service_utils import check_windows_service_status

    check_windows_service_status(status_params.falcon_win_service_name)

if __name__ == "__main__":
  FalconServer().execute()
