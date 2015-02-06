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
import fileinput
import shutil
import os
from resource_management import *
from resource_management.core.logger import Logger

def setup_ranger():
  import params

  if check_db_connnection():

    environment = {"no_proxy": format("{params.ambari_server_hostname}")}

    Execute(('curl', '-kf', '-x', "", '--retry', '10', params.driver_curl_source, '-o',
            params.downloaded_custom_connector),
            not_if=format("test -f {params.downloaded_custom_connector}"),
            path=["/bin", "/usr/bin/"],
            environment=environment,
            sudo=True)

    if not os.path.isfile(params.driver_curl_target):
      Execute(('cp', '--remove-destination', params.downloaded_custom_connector, params.driver_curl_target),
              path=["/bin", "/usr/bin/"],
              sudo=True)                        

    file_path = params.ranger_home + '/install.properties'

    if os.path.isfile(file_path):
      shutil.copyfile(file_path, params.ranger_home + '/install-bk.properties')
    else:
      raise Fail('Ranger admin install.properties file doesnot exist')

    write_properties_to_file(file_path, admin_properties())
    ##if db flavor == oracle - set oracle home env variable
    if params.db_flavor.lower() == 'oracle' and params.oracle_home:
      env_dict = {'JAVA_HOME': params.java_home, 'ORACLE_HOME':params.oracle_home}
    else: 
      env_dict = {'JAVA_HOME': params.java_home}
    cmd = format('cd {params.ranger_home} && {params.ranger_home}/setup.sh')
    
    try:
      Execute(cmd, environment=env_dict, logoutput=True)
    except Exception, e:
      if os.path.isfile(params.ranger_home + '/install-bk.properties'):
        os.remove(file_path)
        os.rename(params.ranger_home + '/install-bk.properties', file_path)
      raise Fail('Ranger installation Failed, {0}'.format(str(e)))

    do_post_installation()

    if os.path.isfile(params.ranger_home + '/install-bk.properties'):
      os.remove(file_path)
      os.rename(params.ranger_home + '/install-bk.properties', file_path)
    else:
      raise Fail('Ranger admin install.properties backup file doesnot exist')

def do_post_installation():
  import params

  Logger.info('Performing Ranger post installation')

  file_path = params.ranger_conf + '/ranger_webserver.properties'
  ranger_site = dict()
  ranger_site['http.service.port'] = params.http_service_port
  ranger_site['https.service.port'] = params.https_service_port
  ranger_site['https.attrib.keystoreFile'] = params.https_attrib_keystoreFile
  ranger_site['https.attrib.keystorePass'] = params.https_attrib_keystorePass
  ranger_site['https.attrib.keyAlias'] = params.https_attrib_keyAlias
  ranger_site['https.attrib.clientAuth'] = params.https_attrib_clientAuth
  write_properties_to_file(file_path, ranger_site)

  ranger_site.clear()

  file_path = params.ranger_conf + '/xa_system.properties'
  ranger_site['http.enabled'] = params.http_enabled
  write_properties_to_file(file_path, ranger_site)
  Logger.info('Performing Ranger post installation DONE')

def setup_usersync():
  import params

  file_path = params.usersync_home + '/install.properties'
  write_properties_to_file(file_path, usersync_properties())

  cmd = format('cd {params.usersync_home} && {params.usersync_home}/setup.sh')
  Execute(cmd, environment={'JAVA_HOME': params.java_home}, logoutput=True)

def write_properties_to_file(file_path, value):
  for key in value:
    modify_config(file_path, key, value[key])

def modify_config(filepath, variable, setting):
  var_found = False
  already_set = False
  V = str(variable)
  S = str(setting)

  if ' ' in S:
    S = '%s' % S

  for line in fileinput.input(filepath, inplace=1):
    if not line.lstrip(' ').startswith('#') and '=' in line:
      _infile_var = str(line.split('=')[0].rstrip(' '))
      _infile_set = str(line.split('=')[1].lstrip(' ').rstrip())
      if var_found == False and _infile_var.rstrip(' ') == V:
        var_found = True
        if _infile_set.lstrip(' ') == S:
          already_set = True
        else:
          line = "%s=%s\n" % (V, S)

    sys.stdout.write(line)

  if not var_found:
    with open(filepath, "a") as f:
      f.write("%s=%s\n" % (V, S))
  elif already_set == True:
    pass
  else:
    pass

  return

def admin_properties():
  import params

  admin_properties = dict()

  admin_properties['DB_FLAVOR'] = params.db_flavor
  admin_properties['SQL_COMMAND_INVOKER'] = params.sql_command_invoker
  admin_properties['SQL_CONNECTOR_JAR'] = params.sql_connector_jar
  admin_properties['db_root_user'] = params.db_root_user
  admin_properties['db_root_password'] = params.db_root_password
  admin_properties['db_host'] = params.db_host
  admin_properties['db_name'] = params.db_name
  admin_properties['db_user'] = params.db_user
  admin_properties['db_password'] = params.db_password
  admin_properties['audit_db_name'] = params.audit_db_name
  admin_properties['audit_db_user'] = params.audit_db_user
  admin_properties['audit_db_password'] = params.audit_db_password
  admin_properties['policymgr_external_url'] = params.policymgr_external_url
  admin_properties['policymgr_http_enabled'] = params.policymgr_http_enabled
  admin_properties['authentication_method'] = params.authentication_method
  admin_properties['remoteLoginEnabled'] = params.remoteLoginEnabled
  admin_properties['authServiceHostName'] = params.authServiceHostName
  admin_properties['authServicePort'] = params.authServicePort
  admin_properties['xa_ldap_url'] = params.xa_ldap_url
  admin_properties['xa_ldap_userDNpattern'] = params.xa_ldap_userDNpattern
  admin_properties['xa_ldap_groupSearchBase'] = params.xa_ldap_groupSearchBase
  admin_properties['xa_ldap_groupSearchFilter'] = params.xa_ldap_groupSearchFilter
  admin_properties['xa_ldap_groupRoleAttribute'] = params.xa_ldap_groupRoleAttribute
  admin_properties['xa_ldap_ad_domain'] = params.xa_ldap_ad_domain
  admin_properties['xa_ldap_ad_url'] = params.xa_ldap_ad_url

  return admin_properties

def usersync_properties():
  import params
  
  usersync_properties = dict()

  usersync_properties['POLICY_MGR_URL'] = params.policymgr_external_url

  usersync_properties['SYNC_SOURCE'] = params.sync_source
  usersync_properties['MIN_UNIX_USER_ID_TO_SYNC'] = params.min_unix_user_id_to_sync
  usersync_properties['SYNC_INTERVAL'] = params.sync_interval
  usersync_properties['SYNC_LDAP_URL'] = params.sync_ldap_url
  usersync_properties['SYNC_LDAP_BIND_DN'] = params.sync_ldap_bind_dn
  usersync_properties['SYNC_LDAP_BIND_PASSWORD'] = params.sync_ldap_bind_password
  usersync_properties['CRED_KEYSTORE_FILENAME'] = params.cred_keystore_filename
  usersync_properties['SYNC_LDAP_USER_SEARCH_BASE'] = params.sync_ldap_user_search_base
  usersync_properties['SYNC_LDAP_USER_SEARCH_SCOPE'] = params.sync_ldap_user_search_scope
  usersync_properties['SYNC_LDAP_USER_OBJECT_CLASS'] = params.sync_ldap_user_object_class
  usersync_properties['SYNC_LDAP_USER_SEARCH_FILTER'] = params.sync_ldap_user_search_filter
  usersync_properties['SYNC_LDAP_USER_NAME_ATTRIBUTE'] = params.sync_ldap_user_name_attribute
  usersync_properties['SYNC_LDAP_USER_GROUP_NAME_ATTRIBUTE'] = params.sync_ldap_user_group_name_attribute
  usersync_properties['SYNC_LDAP_USERNAME_CASE_CONVERSION'] = params.sync_ldap_username_case_conversion
  usersync_properties['SYNC_LDAP_GROUPNAME_CASE_CONVERSION'] = params.sync_ldap_groupname_case_conversion
  usersync_properties['logdir'] = params.logdir

  return usersync_properties

def check_db_connnection():
  import params

  db_root_password = params.db_root_password
  db_root_user = params.db_root_user
  db_host = params.db_host
  sql_command_invoker = params.sql_command_invoker
  db_flavor = params.db_flavor
  cmd_str = ""
  Logger.info('Checking DB connection')

  if db_flavor and db_flavor.lower() == 'mysql':
    cmd_str = "\"" + sql_command_invoker + "\"" + " -u " + db_root_user + " --password=" + db_root_password + " -h " + db_host + " -s -e \"select version();\""
  elif db_flavor and db_flavor.lower() == 'oracle':
    cmd_str = sql_command_invoker +" " +  db_root_user + "/" + db_root_password + "@" + db_host + " AS SYSDBA"
  status, output = get_status_output(cmd_str)

  if status == 0:
    Logger.info('Checking DB connection DONE')
    return True
  else:
    Logger.info(
      'Ranger Admin installation Failed! Ranger requires DB client installed on Ranger Host and DB server running on DB Host')
    sys.exit(1)

def get_status_output(cmd):
  import subprocess

  ret = subprocess.call(cmd, shell=True)
  return ret, ret
