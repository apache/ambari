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
import os
import re
from resource_management.core.logger import Logger
from resource_management.core.resources.system import File, Directory, Execute
from resource_management.core.source import DownloadSource, InlineTemplate
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.resources.modify_properties_file import ModifyPropertiesFile
from resource_management.libraries.resources.properties_file import PropertiesFile
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.core.utils import PasswordString
from resource_management.core.shell import as_sudo
from ambari_commons.constants import UPGRADE_TYPE_NON_ROLLING, UPGRADE_TYPE_ROLLING

# This file contains functions used for setup/configure of Ranger Admin and Ranger Usersync.
# The design is to mimic what is done by the setup.sh script bundled by Ranger component currently.

def ranger(name=None, upgrade_type=None):
  """
  parameter name: name of ranger service component
  """
  if name == 'ranger_admin':
    setup_ranger_admin(upgrade_type=upgrade_type)

  if name == 'ranger_usersync':
    setup_usersync(upgrade_type=upgrade_type)

def setup_ranger_admin(upgrade_type=None):
  import params
 
  if upgrade_type is None:
    if params.restart_type.lower() == "rolling_upgrade":
      upgrade_type = UPGRADE_TYPE_ROLLING
    elif params.restart_type.lower() == "nonrolling_upgrade":
      upgrade_type = UPGRADE_TYPE_NON_ROLLING

  ranger_home = params.ranger_home
  ranger_conf = params.ranger_conf

  Directory(ranger_conf,
    owner = params.unix_user,
    group = params.unix_group,
    create_parents = True
  )

  copy_jdbc_connector()

# if upgrade_type is not None:
#    ranger_home = format("/usr/iop/current/ranger-admin")
#    ranger_conf = format("/usr/iop/current/ranger-admin/conf")

  File(format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
    content = DownloadSource(format("{jdk_location}/{check_db_connection_jar_name}")),
    mode = 0644,
  )

  cp = format("{check_db_connection_jar}")
  if params.db_flavor.lower() == 'sqla':
    cp = cp + os.pathsep + format("{ranger_home}/ews/lib/{jdbc_jar_name}")
  else:
    cp = cp + os.pathsep + format("{driver_curl_target}")
  cp = cp + os.pathsep + format("{ranger_home}/ews/webapp/WEB-INF/lib/*")

  db_connection_check_command = format(
    "{java_home}/bin/java -cp {cp} org.apache.ambari.server.DBConnectionVerification '{ranger_jdbc_connection_url}' {ranger_db_user} {ranger_db_password!p} {ranger_jdbc_driver}")

  env_dict = {}
  if params.db_flavor.lower() == 'sqla':
    env_dict = {'LD_LIBRARY_PATH':params.ld_lib_path}

  Execute(db_connection_check_command, path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin', tries=5, try_sleep=10, environment=env_dict)

  Execute(('ln','-sf', format('{ranger_home}/ews/webapp/WEB-INF/classes/conf'), format('{ranger_home}/conf')),
    not_if=format("ls {ranger_home}/conf"),
    only_if=format("ls {ranger_home}/ews/webapp/WEB-INF/classes/conf"),
    sudo=True)

  if upgrade_type is not None:
    src_file = format('{ranger_home}/ews/webapp/WEB-INF/classes/conf.dist/ranger-admin-default-site.xml')
    dst_file = format('{ranger_home}/conf/ranger-admin-default-site.xml')
    Execute(('cp', '-f', src_file, dst_file), sudo=True)

    src_file = format('{ranger_home}/ews/webapp/WEB-INF/classes/conf.dist/security-applicationContext.xml')
    dst_file = format('{ranger_home}/conf/security-applicationContext.xml')

    Execute(('cp', '-f', src_file, dst_file), sudo=True)

  Execute(('chown','-R',format('{unix_user}:{unix_group}'), format('{ranger_home}/')), sudo=True)
  Directory(params.admin_log_dir,
    owner = params.unix_user,
    group = params.unix_group
  )

  if os.path.isfile(params.ranger_admin_default_file):
    File(params.ranger_admin_default_file, owner=params.unix_user, group=params.unix_group)

  if os.path.isfile(params.security_app_context_file):
    File(params.security_app_context_file, owner=params.unix_user, group=params.unix_group)
  
  Execute(('ln','-sf', format('{ranger_home}/ews/ranger-admin-services.sh'),'/usr/bin/ranger-admin'),
    not_if=format("ls /usr/bin/ranger-admin"),
    only_if=format("ls {ranger_home}/ews/ranger-admin-services.sh"),
    sudo=True)

  XmlConfig("ranger-admin-site.xml",
    conf_dir=ranger_conf,
    configurations=params.config['configurations']['ranger-admin-site'],
    configuration_attributes=params.config['configuration_attributes']['ranger-admin-site'],
    owner=params.unix_user,
    group=params.unix_group,
    mode=0644)

  Directory(os.path.join(ranger_conf,'ranger_jaas'),
    mode=0700,
    owner=params.unix_user,
    group=params.unix_group,
  )
  
  do_keystore_setup(upgrade_type=upgrade_type)

  create_core_site_xml(ranger_conf)


#def setup_ranger_db(upgrade_type=None):
def setup_ranger_db(stack_version=None):
  import params
  
  File(params.downloaded_custom_connector,
    content = DownloadSource(params.driver_curl_source),
    mode = 0644
  )

  Directory(params.java_share_dir,
    mode=0755,
    create_parents = True,
    cd_access="a"
  )

  if params.db_flavor.lower() != 'sqla':
    Execute(('cp', '--remove-destination', params.downloaded_custom_connector, params.driver_curl_target),
      path=["/bin", "/usr/bin/"],
      sudo=True)

    File(params.driver_curl_target, mode=0644)

  ranger_home = params.ranger_home
  version = params.version

  if stack_version is not None:
    ranger_home = format("{stack_root}/{stack_version}/ranger-admin")
    version = stack_version

  copy_jdbc_connector(stack_version=version)

  ModifyPropertiesFile(format("{ranger_home}/install.properties"),
    properties = params.config['configurations']['admin-properties'],
    owner = params.unix_user,
  )

  if params.db_flavor.lower() == 'sqla':
    ModifyPropertiesFile(format("{ranger_home}/install.properties"),
      properties = {'SQL_CONNECTOR_JAR': format('{ranger_home}/ews/lib/{jdbc_jar_name}')},
      owner = params.unix_user,
    )

  env_dict = {'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME':params.java_home}
  if params.db_flavor.lower() == 'sqla':
    env_dict = {'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME':params.java_home, 'LD_LIBRARY_PATH':params.ld_lib_path}

  # User wants us to setup the DB user and DB?
  if params.create_db_dbuser:
    Logger.info('Setting up Ranger DB and DB User')
    dba_setup = format('python {ranger_home}/dba_script.py -q')
    Execute(dba_setup, 
            environment=env_dict,
            logoutput=True,
            user=params.unix_user,
    )
  else:
    Logger.info('Separate DBA property not set. Assuming Ranger DB and DB User exists!')

  db_setup = format('python {ranger_home}/db_setup.py')
  Execute(db_setup, 
          environment=env_dict,
          logoutput=True,
          user=params.unix_user,
  )


#def setup_java_patch(upgrade_type=None):
def setup_java_patch(stack_version=None):
  import params

  ranger_home = params.ranger_home
#  if upgrade_type is not None:
  if stack_version is not None:
#    ranger_home = format("/usr/iop/current/ranger-admin")
    ranger_home = format("{stack_root}/{stack_version}/ranger-admin")


  env_dict = {'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME':params.java_home}
  if params.db_flavor.lower() == 'sqla':
    env_dict = {'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME':params.java_home, 'LD_LIBRARY_PATH':params.ld_lib_path}
  
  setup_java_patch = format('python {ranger_home}/db_setup.py -javapatch')
  Execute(setup_java_patch, 
          environment=env_dict,
          logoutput=True,
          user=params.unix_user,
  )


def do_keystore_setup(upgrade_type=None):
  import params

  ranger_home = params.ranger_home
  cred_lib_path = params.cred_lib_path
  cred_setup_prefix = params.cred_setup_prefix

  if upgrade_type is not None:
    ranger_home = format("/usr/iop/current/ranger-admin")
    cred_lib_path = os.path.join(ranger_home,"cred","lib","*")
    cred_setup_prefix = (format('{ranger_home}/ranger_credential_helper.py'), '-l', cred_lib_path)

  if not is_empty(params.ranger_credential_provider_path):    
    jceks_path = params.ranger_credential_provider_path
    cred_setup = cred_setup_prefix + ('-f', jceks_path, '-k', params.ranger_jpa_jdbc_credential_alias, '-v', PasswordString(params.ranger_ambari_db_password), '-c', '1')

    Execute(cred_setup, 
            environment={'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME': params.java_home}, 
            logoutput=True, 
            sudo=True
    )
    File(params.ranger_credential_provider_path,
      owner = params.unix_user,
      group = params.unix_group,
      mode = 0640
    )

  if not is_empty(params.ranger_credential_provider_path) and (params.ranger_audit_source_type).lower() == 'db' and not is_empty(params.ranger_ambari_audit_db_password):
    jceks_path = params.ranger_credential_provider_path
    cred_setup = cred_setup_prefix + ('-f', jceks_path, '-k', params.ranger_jpa_audit_jdbc_credential_alias, '-v', PasswordString(params.ranger_ambari_audit_db_password), '-c', '1')
    Execute(cred_setup, 
            environment={'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME': params.java_home}, 
            logoutput=True, 
            sudo=True
    )

    File(params.ranger_credential_provider_path,
      owner = params.unix_user,
      group = params.unix_group,
      mode = 0640
    )

def password_validation(password):
  import params
  if password.strip() == "":
    raise Fail("Blank password is not allowed for Bind user. Please enter valid password.")
  if re.search("[\\\`'\"]",password):
    raise Fail("LDAP/AD bind password contains one of the unsupported special characters like \" ' \ `")
  else:
    Logger.info("password validated")

def copy_jdbc_connector(stack_version=None):
  import params

  if params.jdbc_jar_name is None and params.driver_curl_source.endswith("/None"):
    error_message = format("{db_flavor} jdbc driver cannot be downloaded from {jdk_location}\nPlease run 'ambari-server setup --jdbc-db={db_flavor} --jdbc-driver={{path_to_jdbc}}' on ambari-server host.")
    raise Fail(error_message)

#  if params.driver_curl_source and not params.driver_curl_source.endswith("/None"):
#    if params.previous_jdbc_jar and os.path.isfile(params.previous_jdbc_jar):
#      File(params.previous_jdbc_jar, action='delete')

  File(params.downloaded_custom_connector,
    content = DownloadSource(params.driver_curl_source),
    mode = 0644
  )

  ranger_home = params.ranger_home
  if stack_version is not None:
    ranger_home = format("{stack_root}/{stack_version}/ranger-admin")

  driver_curl_target = format("{ranger_home}/ews/lib/{jdbc_jar_name}")

  if params.db_flavor.lower() == 'sqla':
    Execute(('tar', '-xvf', params.downloaded_custom_connector, '-C', params.tmp_dir), sudo = True)

    Execute(('cp', '--remove-destination', params.jar_path_in_archive, os.path.join(ranger_home, 'ews', 'lib')),
      path=["/bin", "/usr/bin/"],
      sudo=True)

    File(os.path.join(ranger_home, 'ews', 'lib', 'sajdbc4.jar'), mode=0644)

    Directory(params.jdbc_libs_dir,
      cd_access="a",
      create_parents=True)

    Execute(as_sudo(['yes', '|', 'cp', params.libs_path_in_archive, params.jdbc_libs_dir], auto_escape=False),
            path=["/bin", "/usr/bin/"])
  else:
    Execute(('cp', '--remove-destination', params.downloaded_custom_connector, os.path.join(ranger_home, 'ews', 'lib')),
      path=["/bin", "/usr/bin/"],
      sudo=True)

    File(os.path.join(ranger_home, 'ews', 'lib',params.jdbc_jar_name), mode=0644)

  ModifyPropertiesFile(format("{ranger_home}/install.properties"),
    properties = params.config['configurations']['admin-properties'],
    owner = params.unix_user,
  )

  if params.db_flavor.lower() == 'sqla':
    ModifyPropertiesFile(format("{ranger_home}/install.properties"),
      properties = {'SQL_CONNECTOR_JAR': format('{ranger_home}/ews/lib/sajdbc4.jar')},
      owner = params.unix_user,
    )
  else:
    ModifyPropertiesFile(format("{ranger_home}/install.properties"),
      properties = {'SQL_CONNECTOR_JAR': format('{driver_curl_target}')},
       owner = params.unix_user,
    )

def setup_usersync(upgrade_type=None):
  import params

  usersync_home = params.usersync_home
  ranger_home = params.ranger_home
  ranger_ugsync_conf = params.ranger_ugsync_conf

  if not is_empty(params.ranger_usersync_ldap_ldapbindpassword) and params.ug_sync_source == 'org.apache.ranger.ldapusersync.process.LdapUserGroupBuilder':
    password_validation(params.ranger_usersync_ldap_ldapbindpassword)

  Directory(params.ranger_pid_dir,
    mode=0750,
    owner = params.unix_user,
    group = params.unix_group
  )  

  Directory(params.usersync_log_dir,
    owner = params.unix_user,
    group = params.unix_group
  )
  
  Directory(format("{ranger_ugsync_conf}/"),
       owner = params.unix_user
  )

  if upgrade_type is not None:
    src_file = format('{usersync_home}/conf.dist/ranger-ugsync-default.xml')
    dst_file = format('{usersync_home}/conf/ranger-ugsync-default.xml')
    Execute(('cp', '-f', src_file, dst_file), sudo=True)

    src_file = format('{usersync_home}/conf.dist/log4j.xml')
    dst_file = format('{usersync_home}/conf/log4j.xml')
    Execute(('cp', '-f', src_file, dst_file), sudo=True)

  XmlConfig("ranger-ugsync-site.xml",
    conf_dir=ranger_ugsync_conf,
    configurations=params.config['configurations']['ranger-ugsync-site'],
    configuration_attributes=params.config['configuration_attributes']['ranger-ugsync-site'],
    owner=params.unix_user,
    group=params.unix_group,
    mode=0644)

  if os.path.isfile(params.ranger_ugsync_default_file):
    File(params.ranger_ugsync_default_file, owner=params.unix_user, group=params.unix_group)

  if os.path.isfile(params.usgsync_log4j_file):
    File(params.usgsync_log4j_file, owner=params.unix_user, group=params.unix_group)

  if os.path.isfile(params.cred_validator_file):
    File(params.cred_validator_file, group=params.unix_group, mode=04555)

  cred_file = format('{ranger_home}/ranger_credential_helper.py')
  if os.path.isfile(format('{usersync_home}/ranger_credential_helper.py')):
    cred_file = format('{usersync_home}/ranger_credential_helper.py')

  cred_lib = os.path.join(usersync_home,"lib","*")
  cred_setup_prefix = (cred_file, '-l', cred_lib)

  cred_setup = cred_setup_prefix + ('-f', params.ugsync_jceks_path, '-k', 'usersync.ssl.key.password', '-v', PasswordString(params.ranger_usersync_keystore_password), '-c', '1')
  Execute(cred_setup, environment={'JAVA_HOME': params.java_home}, logoutput=True, sudo=True)

  cred_setup = cred_setup_prefix + ('-f', params.ugsync_jceks_path, '-k', 'ranger.usersync.ldap.bindalias', '-v', PasswordString(params.ranger_usersync_ldap_ldapbindpassword), '-c', '1')
  Execute(cred_setup, environment={'JAVA_HOME': params.java_home}, logoutput=True, sudo=True)

  cred_setup = cred_setup_prefix + ('-f', params.ugsync_jceks_path, '-k', 'usersync.ssl.truststore.password', '-v', PasswordString(params.ranger_usersync_truststore_password), '-c', '1')
  Execute(cred_setup, environment={'JAVA_HOME': params.java_home}, logoutput=True, sudo=True)

  File(params.ugsync_jceks_path,
       owner = params.unix_user,
       group = params.unix_group,
       mode = 0640
  )
  
  File([params.usersync_start, params.usersync_stop],
       owner = params.unix_user,
       group = params.unix_group
  )

  File(params.usersync_services_file,
    mode = 0755,
  )

  Execute(('ln','-sf', format('{usersync_services_file}'),'/usr/bin/ranger-usersync'),
    not_if=format("ls /usr/bin/ranger-usersync"),
    only_if=format("ls {usersync_services_file}"),
    sudo=True)

  if not os.path.isfile(params.ranger_usersync_keystore_file):
    cmd = format("{java_home}/bin/keytool -genkeypair -keyalg RSA -alias selfsigned -keystore '{ranger_usersync_keystore_file}' -keypass {ranger_usersync_keystore_password!p} -storepass {ranger_usersync_keystore_password!p} -validity 3600 -keysize 2048 -dname '{default_dn_name}'")

    Execute(cmd, logoutput=True, user = params.unix_user)

    File(params.ranger_usersync_keystore_file,
        owner = params.unix_user,
        group = params.unix_group,
        mode = 0640
    )

  create_core_site_xml(ranger_ugsync_conf)


def create_core_site_xml(conf_dir):
  import params

  if params.stack_supports_ranger_kerberos:
    if params.has_namenode:
      XmlConfig("core-site.xml",
                conf_dir=conf_dir,
                configurations=params.config['configurations']['core-site'],
                configuration_attributes=params.config['configuration_attributes']['core-site'],
                owner=params.unix_user,
                group=params.unix_group,
                mode=0644
      )
    else:
      Logger.warning('HDFS service not installed. Creating core-site.xml file.')
      XmlConfig("core-site.xml",
        conf_dir=conf_dir,
        configurations=params.core_site_property,
        configuration_attributes={},
        owner=params.unix_user,
        group=params.unix_group,
        mode=0644
      )
