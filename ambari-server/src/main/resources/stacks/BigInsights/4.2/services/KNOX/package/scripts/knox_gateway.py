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

from resource_management import *
import os
import tarfile



from resource_management.libraries.functions import stack_select

from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, validate_security_config_properties, get_params_from_filesystem, \
  FILE_TYPE_XML
from resource_management.core.resources.system import File, Execute, Directory, Link
        
import sys


#if OSCheck.is_windows_family():
#  from resource_management.libraries.functions.windows_service_utils import check_windows_service_status

import upgrade
from knox import knox, update_knox_logfolder_permissions
from knox_ldap import ldap

from setup_ranger_knox import setup_ranger_knox

class KnoxGateway(Script):
  def install(self, env):
    self.install_packages(env)
    import params
    env.set_params(params)
    
    File(format('{knox_conf_dir}/topologies/sandbox.xml'),
         action = "delete",
    )

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    knox()
    ldap()
    


#@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
#class KnoxGatewayWindows(KnoxGateway):
#  def start(self, env):
#    import params
#    env.set_params(params)
#    self.configure(env)
#    # setup_ranger_knox(env)
#    Service(params.knox_gateway_win_service_name, action="start")
#
#  def stop(self, env):
#    import params
#    env.set_params(params)
#    Service(params.knox_gateway_win_service_name, action="stop")
#
#  def status(self, env):
#    import status_params
#    env.set_params(status_params)
#    check_windows_service_status(status_params.knox_gateway_win_service_name)
#
#  def startdemoldap(self, env):
#    import params
#    env.set_params(params)
#    self.configureldap(env)
#    Service(params.knox_ldap_win_service_name, action="start")
#
#  def stopdemoldap(self, env):
#    import params
#    env.set_params(params)
#    Service(params.knox_ldap_win_service_name, action="stop")



#@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
#class KnoxGatewayDefault(KnoxGateway):
#  def get_component_name(self):
#    return {"HDP": "knox-server"}

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    if params.version and compare_versions(format_stack_version(params.version), '4.0.0.0') >= 0:

      absolute_backup_dir = None
      if params.upgrade_direction and params.upgrade_direction == Direction.UPGRADE:
        Logger.info("Backing up directories. Initial conf folder: %s" % os.path.realpath(params.knox_conf_dir))

        # This will backup the contents of the conf directory into /tmp/knox-upgrade-backup/knox-conf-backup.tar
        absolute_backup_dir = upgrade.backup_data()

      # conf-select will change the symlink to the conf folder.
#     hdp_select.select("knox-server", params.version)
      stack_select.select_packages(params.version)

      # Extract the tar of the old conf folder into the new conf directory
      if absolute_backup_dir is not None and params.upgrade_direction and params.upgrade_direction == Direction.UPGRADE:
        conf_tar_source_path = os.path.join(absolute_backup_dir, upgrade.BACKUP_CONF_ARCHIVE)
        data_tar_source_path = os.path.join(absolute_backup_dir, upgrade.BACKUP_DATA_ARCHIVE)
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
          extract_dir = os.path.realpath(params.knox_data_dir+"-"+params.version+"/security")
          if not os.path.exists(extract_dir):
            Directory(extract_dir,
                       owner = params.knox_user,
                       group = params.knox_group,
                       create_parents = True
            )

          data_tar_dest_path = os.path.join(extract_dir, upgrade.BACKUP_DATA_ARCHIVE)
          Logger.info("Copying %s into %s file." % (upgrade.BACKUP_DATA_ARCHIVE, data_tar_dest_path + "/security"))
          Execute(('cp', data_tar_source_path, data_tar_dest_path ),
                  sudo = True,
          )

          tar_archive.untar_archive(data_tar_source_path, extract_dir)
          
          File(data_tar_dest_path,
               action = "delete",
          )

          populate_topology_template = format('{sudo} cp /usr/iop/{version}/etc/knox/conf.dist/topologies/* {knox_conf_dir}/topologies')
          Logger.info("Prepare to populate topologies template via command: {0}".format(populate_topology_template))
          Execute(populate_topology_template)


  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    daemon_cmd = format('{knox_bin} start')
    populate_topology = format('cd {knox_conf_dir}/topologies/; {sudo} ambari-python-wrap ./generate_template.py ' + params.HAServers  + ' ; {sudo} chmod 640 *.xml; {sudo} chown knox:knox *.xml')
    no_op_test = format('ls {knox_pid_file} >/dev/null 2>&1 && ps -p `cat {knox_pid_file}` >/dev/null 2>&1')
    
    setup_ranger_knox(upgrade_type=upgrade_type)
    # Used to setup symlink, needed to update the knox managed symlink, in case of custom locations
    if os.path.islink(params.knox_managed_pid_symlink):
      Link(params.knox_managed_pid_symlink,
           to = params.knox_pid_dir,
      )

    if os.path.islink(params.knox_managed_logs_symlink):
      Link(params.knox_managed_logs_symlink,
           to = params.knox_logs_dir,
      )

    update_knox_logfolder_permissions()
        
    Execute(populate_topology)
    Execute(daemon_cmd,
            user=params.knox_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=no_op_test
    )

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
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


  def configureldap(self, env):
    import params
    env.set_params(params)
    ldap()

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
    Execute (format("rm -f {ldap_pid_file}"))

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
