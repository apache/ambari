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
from resource_management.core.logger import Logger
from resource_management.core.resources.system import File, Directory, Execute
from resource_management.core.source import DownloadSource, InlineTemplate
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.resources.modify_properties_file import ModifyPropertiesFile
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.is_empty import is_empty

# This file contains functions used for setup/configure of Ranger Admin and Ranger Usersync.
# The design is to mimic what is done by the setup.sh script bundled by Ranger component currently.

def ranger(name=None, rolling_upgrade=False):
  """
  parameter name: name of ranger service component
  """
  if name == 'ranger_admin':
    setup_ranger_admin(rolling_upgrade=rolling_upgrade)

  if name == 'ranger_usersync':
    setup_usersync()

def setup_ranger_admin(rolling_upgrade=False):
  import params

  ranger_home = params.ranger_home
  ranger_conf = params.ranger_conf

  if rolling_upgrade:
    ranger_home = format("/usr/hdp/{version}/ranger-admin")
    ranger_conf = format("/usr/hdp/{version}/ranger-admin/conf")

  File(format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
    content = DownloadSource(format("{jdk_location}{check_db_connection_jar_name}")),
  )

  cp = format("{check_db_connection_jar}")
  cp = cp + os.pathsep + format("{driver_curl_target}")
  cp = cp + os.pathsep + format("{ranger_home}/ews/webapp/WEB-INF/lib/*")

  db_connection_check_command = format(
    "{java_home}/bin/java -cp {cp} org.apache.ambari.server.DBConnectionVerification '{ranger_jdbc_connection_url}' {ranger_db_user} {ranger_db_password!p} {ranger_jdbc_driver}")

  Execute(db_connection_check_command, path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin', tries=5, try_sleep=10)

  Execute(('ln','-sf', format('{ranger_home}/ews/webapp/WEB-INF/classes/conf'), format('{ranger_home}/conf')),
    not_if=format("ls {ranger_home}/conf"),
    only_if=format("ls {ranger_home}/ews/webapp/WEB-INF/classes/conf"),
    sudo=True)

  if rolling_upgrade:
    src_file = format('{ranger_home}/ews/webapp/WEB-INF/classes/conf.dist/ranger-admin-default-site.xml')
    dst_file = format('{ranger_home}/conf/ranger-admin-default-site.xml')
    Execute(('cp', '-f', src_file, dst_file), sudo=True)

    src_file = format('{ranger_home}/ews/webapp/WEB-INF/classes/conf.dist/security-applicationContext.xml')
    dst_file = format('{ranger_home}/conf/security-applicationContext.xml')

    Execute(('cp', '-f', src_file, dst_file), sudo=True)

  Execute(('chown','-R',format('{unix_user}:{unix_group}'), format('{ranger_home}/')), sudo=True)

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

  do_keystore_setup(rolling_upgrade=rolling_upgrade)


def setup_ranger_db(rolling_upgrade=False):
  import params
  
  File(params.downloaded_custom_connector,
    content = DownloadSource(params.driver_curl_source)
  )

  Directory(params.java_share_dir,
    mode=0755
  )

  if not os.path.isfile(params.driver_curl_target):
    Execute(('cp', '--remove-destination', params.downloaded_custom_connector, params.driver_curl_target),
      path=["/bin", "/usr/bin/"],
      not_if=format("test -f {driver_curl_target}"),
      sudo=True)

  ranger_home = params.ranger_home
  if rolling_upgrade:
    ranger_home = format("/usr/hdp/{version}/ranger-admin")

  if not os.path.isfile(os.path.join(params.ranger_home, 'ews', 'lib',params.jdbc_jar_name)):
    Execute(('cp', '--remove-destination', params.downloaded_custom_connector, os.path.join(params.ranger_home, 'ews', 'lib')),
      path=["/bin", "/usr/bin/"],
      sudo=True)

  ModifyPropertiesFile(format("{ranger_home}/install.properties"),
    properties = params.config['configurations']['admin-properties']
  )

  # User wants us to setup the DB user and DB?
  if params.create_db_dbuser:
    Logger.info('Setting up Ranger DB and DB User')
    dba_setup = format('python {ranger_home}/dba_script.py -q')
    Execute(dba_setup, environment={'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME': params.java_home}, logoutput=True)
  else:
    Logger.info('Separate DBA property not set. Assuming Ranger DB and DB User exists!')

  db_setup = format('python {ranger_home}/db_setup.py')
  Execute(db_setup, environment={'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME': params.java_home}, logoutput=True)


def setup_java_patch(rolling_upgrade=False):
  import params

  ranger_home = params.ranger_home
  if rolling_upgrade:
    ranger_home = format("/usr/hdp/{version}/ranger-admin")

  setup_java_patch = format('python {ranger_home}/db_setup.py -javapatch')
  Execute(setup_java_patch, environment={'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME': params.java_home}, logoutput=True)


def do_keystore_setup(rolling_upgrade=False): 
  import params

  ranger_home = params.ranger_home
  cred_lib_path = params.cred_lib_path
  cred_setup_prefix = params.cred_setup_prefix

  if rolling_upgrade:
    ranger_home = format("/usr/hdp/{version}/ranger-admin")
    cred_lib_path = os.path.join(ranger_home,"cred","lib","*")
    cred_setup_prefix = format('python {ranger_home}/ranger_credential_helper.py -l "{cred_lib_path}"')

  if not is_empty(params.ranger_credential_provider_path):    
    jceks_path = params.ranger_credential_provider_path
    cred_setup = format('{cred_setup_prefix} -f {jceks_path} -k "{ranger_jpa_jdbc_credential_alias}" -v {ranger_ambari_db_password!p} -c 1')

    Execute(cred_setup, environment={'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME': params.java_home}, logoutput=True)

    File(params.ranger_credential_provider_path,
      owner = params.unix_user,
      group = params.unix_group
    )

  if not is_empty(params.ranger_credential_provider_path) and (params.ranger_audit_source_type).lower() == 'db' and not is_empty(params.ranger_ambari_audit_db_password):
    jceks_path = params.ranger_credential_provider_path
    cred_setup = format('{cred_setup_prefix} -f {jceks_path} -k "{ranger_jpa_audit_jdbc_credential_alias}" -v {ranger_ambari_audit_db_password!p} -c 1')

    Execute(cred_setup, environment={'RANGER_ADMIN_HOME':ranger_home, 'JAVA_HOME': params.java_home}, logoutput=True)

    File(params.ranger_credential_provider_path,
      owner = params.unix_user,
      group = params.unix_group
    )

 
def setup_usersync():
  import params

  Directory(params.ranger_pid_dir,
    mode=0750,
    owner = params.unix_user,
    group = params.unix_group
  )  

  Directory(params.usersync_log_dir,
    owner = params.unix_user,
    group = params.unix_group
  )

  XmlConfig("ranger-ugsync-site.xml",
    conf_dir=params.ranger_ugsync_conf,
    configurations=params.config['configurations']['ranger-ugsync-site'],
    configuration_attributes=params.config['configuration_attributes']['ranger-ugsync-site'],
    owner=params.unix_user,
    group=params.unix_group,
    mode=0644)

  cred_lib = os.path.join(params.usersync_home,"lib","*")

  cred_setup = format('python {ranger_home}/ranger_credential_helper.py -l "{cred_lib}" -f {ugsync_jceks_path} -k "usersync.ssl.key.password" -v {ranger_usersync_keystore_password!p} -c 1')
  Execute(cred_setup, environment={'RANGER_ADMIN_HOME':params.ranger_home, 'JAVA_HOME': params.java_home}, logoutput=True)

  cred_setup = format('python {ranger_home}/ranger_credential_helper.py -l "{cred_lib}" -f {ugsync_jceks_path} -k "ranger.usersync.ldap.bindalias" -v {ranger_usersync_ldap_ldapbindpassword!p} -c 1')
  Execute(cred_setup, environment={'RANGER_ADMIN_HOME':params.ranger_home, 'JAVA_HOME': params.java_home}, logoutput=True)

  cred_setup = format('python {ranger_home}/ranger_credential_helper.py -l "{cred_lib}" -f {ugsync_jceks_path} -k "usersync.ssl.truststore.password" -v {ranger_usersync_truststore_password!p} -c 1')
  Execute(cred_setup, environment={'RANGER_ADMIN_HOME':params.ranger_home, 'JAVA_HOME': params.java_home}, logoutput=True)

  File(params.ugsync_jceks_path,
       owner = params.unix_user,
       group = params.unix_group
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

    Execute(cmd, logoutput=True)

    File(params.ranger_usersync_keystore_file,
        owner = params.unix_user,
        group = params.unix_group
    )
