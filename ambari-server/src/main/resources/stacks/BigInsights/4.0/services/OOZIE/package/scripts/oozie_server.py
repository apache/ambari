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

import oozie_server_upgrade

from resource_management.core import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions import format
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.script import Script
from resource_management.libraries.functions import check_process_status
from resource_management.libraries.functions import compare_versions
from resource_management.libraries.functions import format_stack_version
from resource_management.libraries.functions.security_commons import build_expectations
from resource_management.libraries.functions.security_commons import cached_kinit_executor
from resource_management.libraries.functions.security_commons import get_params_from_filesystem
from resource_management.libraries.functions.security_commons import validate_security_config_properties
from resource_management.libraries.functions.security_commons import FILE_TYPE_XML

from oozie import oozie
from oozie_service import oozie_service


class OozieServer(Script):

  def install(self, env):
    self.install_packages(env)


  def configure(self, env, upgrade_type=None):
    import params

    #TODO: needed?
    if upgrade_type == "nonrolling" and params.upgrade_direction == Direction.UPGRADE and \
            params.version and compare_versions(format_stack_version(params.version), '4.1.0.0') >= 0:
      # In order for the "/usr/hdp/current/oozie-<client/server>" point to the new version of
      # oozie, we need to create the symlinks both for server and client.
      # This is required as both need to be pointing to new installed oozie version.

      # Sets the symlink : eg: /usr/hdp/current/oozie-client -> /usr/hdp/2.3.x.y-<version>/oozie
      # Sets the symlink : eg: /usr/hdp/current/oozie-server -> /usr/hdp/2.3.x.y-<version>/oozie
      stack_select.select_packages(params.version)

    env.set_params(params)
    oozie(is_server=True)


  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    #TODO remove this when config command will be implemented
    self.configure(env)

    # preparing the WAR file must run after configure since configure writes out
    # oozie-env.sh which is needed to have the right environment directories setup!
    if upgrade_type is not None:
      oozie_server_upgrade.prepare_warfile();

    oozie_service(action='start', upgrade_type=upgrade_type)


  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    oozie_service(action='stop', upgrade_type=upgrade_type)


  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.pid_file)


  def security_status(self, env):

    import status_params
    env.set_params(status_params)

    if status_params.security_enabled:
      expectations = {
        "oozie-site":
          build_expectations('oozie-site',
                             {
                               "oozie.authentication.type": "kerberos",
                               "oozie.service.AuthorizationService.security.enabled": "true",
                               "oozie.service.HadoopAccessorService.kerberos.enabled": "true"
                             },
                             [
                               "local.realm",
                               "oozie.authentication.kerberos.principal",
                               "oozie.authentication.kerberos.keytab",
                               "oozie.service.HadoopAccessorService.kerberos.principal",
                               "oozie.service.HadoopAccessorService.keytab.file"
                             ],
                             None)
      }

      security_params = get_params_from_filesystem(status_params.conf_dir,
                                                   {'oozie-site.xml': FILE_TYPE_XML})
      result_issues = validate_security_config_properties(security_params, expectations)
      if not result_issues: # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ('oozie-site' not in security_params
              or 'oozie.authentication.kerberos.principal' not in security_params['oozie-site']
              or 'oozie.authentication.kerberos.keytab' not in security_params['oozie-site']
              or 'oozie.service.HadoopAccessorService.kerberos.principal' not in security_params['oozie-site']
              or 'oozie.service.HadoopAccessorService.keytab.file' not in security_params['oozie-site']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out({"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.oozie_user,
                                security_params['oozie-site']['oozie.authentication.kerberos.keytab'],
                                security_params['oozie-site']['oozie.authentication.kerberos.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.oozie_user,
                                security_params['oozie-site']['oozie.service.HadoopAccessorService.keytab.file'],
                                security_params['oozie-site']['oozie.service.HadoopAccessorService.kerberos.principal'],
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


  def pre_upgrade_restart(self, env, upgrade_type=None):
    """
    Performs the tasks surrounding the Oozie startup when a rolling upgrade
    is in progress. This includes backing up the configuration, updating
    the database, preparing the WAR, and installing the sharelib in HDFS.
    :param env:
    :return:
    """
    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # is not at least IOP 4.0.0.0
    if not params.version or compare_versions(format_stack_version(params.version), '4.0.0.0') < 0:
      return

    Logger.info("Executing Oozie Server Rolling Upgrade pre-restart")

    oozie_server_upgrade.backup_configuration()

    stack_select.select_packages(params.version)
    #Execute(format("stack-select set oozie-server {version}"))

    oozie_server_upgrade.restore_configuration()
    #oozie_server_upgrade.prepare_libext_directory()
    oozie_server_upgrade.upgrade_oozie()


if __name__ == "__main__":
  OozieServer().execute()
