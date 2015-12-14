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
import tarfile

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import conf_select, tar_archive
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions import Direction
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, validate_security_config_properties, get_params_from_filesystem, \
  FILE_TYPE_XML
from resource_management.core.resources.system import File, Execute, Directory, Link
from resource_management.core.resources.service import Service
from resource_management.core.logger import Logger

from ambari_commons import OSConst, OSCheck
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

if OSCheck.is_windows_family():
  from resource_management.libraries.functions.windows_service_utils import check_windows_service_status

import upgrade
from knox import knox, update_knox_logfolder_permissions
from knox_ldap import ldap
from setup_ranger_knox import setup_ranger_knox


class KnoxGateway(Script):
  def get_stack_to_component(self):
    return {"HDP": "knox-server"}

  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

    File(os.path.join(params.knox_conf_dir, 'topologies', 'sandbox.xml'),
         action = "delete",
    )

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    knox()
    ldap()

  def configureldap(self, env):
    import params
    env.set_params(params)
    ldap()



@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class KnoxGatewayWindows(KnoxGateway):
  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    # setup_ranger_knox(env)
    Service(params.knox_gateway_win_service_name, action="start")

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    Service(params.knox_gateway_win_service_name, action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_windows_service_status(status_params.knox_gateway_win_service_name)

  def startdemoldap(self, env):
    import params
    env.set_params(params)
    self.configureldap(env)
    Service(params.knox_ldap_win_service_name, action="start")

  def stopdemoldap(self, env):
    import params
    env.set_params(params)
    Service(params.knox_ldap_win_service_name, action="stop")



@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class KnoxGatewayDefault(KnoxGateway):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:

      absolute_backup_dir = None
      if params.upgrade_direction and params.upgrade_direction == Direction.UPGRADE:
        Logger.info("Backing up directories. Initial conf folder: %s" % os.path.realpath(params.knox_conf_dir))

        # This will backup the contents of the conf directory into /tmp/knox-upgrade-backup/knox-conf-backup.tar
        absolute_backup_dir = upgrade.backup_data()

      # conf-select will change the symlink to the conf folder.
      conf_select.select(params.stack_name, "knox", params.version)
      hdp_select.select("knox-server", params.version)

      # Extract the tar of the old conf folder into the new conf directory
      if absolute_backup_dir is not None and params.upgrade_direction and params.upgrade_direction == Direction.UPGRADE:
        conf_tar_source_path = os.path.join(absolute_backup_dir, upgrade.BACKUP_CONF_ARCHIVE)
        if os.path.exists(conf_tar_source_path):
          extract_dir = os.path.realpath(params.knox_conf_dir)
          conf_tar_dest_path = os.path.join(extract_dir, upgrade.BACKUP_CONF_ARCHIVE)
          Logger.info("Copying %s into %s file." % (upgrade.BACKUP_CONF_ARCHIVE, conf_tar_dest_path))
          Execute(('cp', conf_tar_source_path, conf_tar_dest_path),
                  sudo = True,
          )

          tar_archive.untar_archive(conf_tar_source_path, extract_dir)
          
          File(conf_tar_dest_path,
               action = "delete",
          )

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    daemon_cmd = format('{knox_bin} start')
    no_op_test = format('ls {knox_pid_file} >/dev/null 2>&1 && ps -p `cat {knox_pid_file}` >/dev/null 2>&1')
    setup_ranger_knox(upgrade_type=upgrade_type)
    # Used to setup symlink, needed to update the knox managed symlink, in case of custom locations
    if os.path.islink(params.knox_managed_pid_symlink):
      Link(params.knox_managed_pid_symlink,
           to = params.knox_pid_dir,
      )

    update_knox_logfolder_permissions()

    Execute(daemon_cmd,
            user=params.knox_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=no_op_test
    )

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    daemon_cmd = format('{knox_bin} stop')

    update_knox_logfolder_permissions()

    Execute(daemon_cmd,
            environment={'JAVA_HOME': params.java_home},
            user=params.knox_user,
    )
    File(params.knox_pid_file,
         action="delete",
    )

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.knox_pid_file)

  def startdemoldap(self, env):
    import params
    env.set_params(params)
    self.configureldap(env)
    daemon_cmd = format('{ldap_bin} start')
    no_op_test = format('ls {ldap_pid_file} >/dev/null 2>&1 && ps -p `cat {ldap_pid_file}` >/dev/null 2>&1')
    Execute(daemon_cmd,
            user=params.knox_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=no_op_test
    )

  def stopdemoldap(self, env):
    import params
    env.set_params(params)
    self.configureldap(env)
    daemon_cmd = format('{ldap_bin} stop')
    Execute(daemon_cmd,
            environment={'JAVA_HOME': params.java_home},
            user=params.knox_user,
            )
    File(params.ldap_pid_file,
      action = "delete"
    )

  def security_status(self, env):
    import status_params
    env.set_params(status_params)

    if status_params.security_enabled:
      expectations = {}
      expectations.update(build_expectations(
        'krb5JAASLogin',
        None,
        ['keytab', 'principal'],
        None
      ))
      expectations.update(build_expectations(
        'gateway-site',
        {
          "gateway.hadoop.kerberos.secured" : "true"
        },
        None,
        None
      ))

      security_params = {
        "krb5JAASLogin":
          {
            'keytab': status_params.knox_keytab_path,
            'principal': status_params.knox_principal_name
          }
      }
      security_params.update(get_params_from_filesystem(status_params.knox_conf_dir,
        {"gateway-site.xml" : FILE_TYPE_XML}))

      result_issues = validate_security_config_properties(security_params, expectations)
      if not result_issues:  # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ( 'krb5JAASLogin' not in security_params
               or 'keytab' not in security_params['krb5JAASLogin']
               or 'principal' not in security_params['krb5JAASLogin']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out({"securityIssuesFound": "Keytab file and principal are not set."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.knox_user,
                                security_params['krb5JAASLogin']['keytab'],
                                security_params['krb5JAASLogin']['principal'],
                                status_params.hostname,
                                status_params.temp_dir)
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


if __name__ == "__main__":
  KnoxGateway().execute()
