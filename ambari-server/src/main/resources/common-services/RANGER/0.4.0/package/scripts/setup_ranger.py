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


def setup_ranger(env):
  import params

  env.set_params(params)

  if check_db_connnection(env):

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

    write_properties_to_file(file_path, params.config['configurations']['admin-properties'])

    cmd = format('cd {ranger_home} && {ranger_home}/setup.sh')

    try:
      opt = Execute(cmd, environment={'JAVA_HOME': params.java_home}, logoutput=True)
    except Exception, e:
      if os.path.isfile(params.ranger_home + '/install-bk.properties'):
        os.remove(file_path)
        os.rename(params.ranger_home + '/install-bk.properties', file_path)
      raise Fail('Ranger installation Failed, {0}'.format(str(e)))

    do_post_installation(env)

    if os.path.isfile(params.ranger_home + '/install-bk.properties'):
      os.remove(file_path)
      os.rename(params.ranger_home + '/install-bk.properties', file_path)
    else:
      raise Fail('Ranger admin install.properties backup file doesnot exist')


def do_post_installation(env):
  import params

  env.set_params(params)
  Logger.info('Performing Ranger post installation..')

  file_path = params.ranger_conf + '/ranger_webserver.properties'
  d = dict()
  d['http.service.port'] = params.config['configurations']['ranger-site']['HTTP_SERVICE_PORT']
  d['https.service.port'] = params.config['configurations']['ranger-site']['HTTPS_SERVICE_PORT']
  d['https.attrib.keystoreFile'] = params.config['configurations']['ranger-site']['HTTPS_KEYSTORE_FILE']
  d['https.attrib.keystorePass'] = params.config['configurations']['ranger-site']['HTTPS_KEYSTORE_PASS']
  d['https.attrib.keyAlias'] = params.config['configurations']['ranger-site']['HTTPS_KEY_ALIAS']
  d['https.attrib.clientAuth'] = params.config['configurations']['ranger-site']['HTTPS_CLIENT_AUTH']
  write_properties_to_file(file_path, d)

  d.clear();

  file_path = params.ranger_conf + '/xa_system.properties'
  d['http.enabled'] = params.config['configurations']['ranger-site']['HTTP_ENABLED']
  write_properties_to_file(file_path, d)
  Logger.info('Performing Ranger post installation..DONE')


def setup_usersync(env):
  import params

  env.set_params(params)

  file_path = params.usersync_home + '/install.properties'
  write_properties_to_file(file_path, usersync_properties(params))

  cmd = format('cd {usersync_home} && {usersync_home}/setup.sh')
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


def usersync_properties(params):
  d = dict()

  d['POLICY_MGR_URL'] = params.config['configurations']['admin-properties']['policymgr_external_url']

  d['SYNC_SOURCE'] = params.config['configurations']['usersync-properties']['SYNC_SOURCE']
  d['MIN_UNIX_USER_ID_TO_SYNC'] = params.config['configurations']['usersync-properties']['MIN_UNIX_USER_ID_TO_SYNC']
  d['SYNC_INTERVAL'] = params.config['configurations']['usersync-properties']['SYNC_INTERVAL']
  d['SYNC_LDAP_URL'] = params.config['configurations']['usersync-properties']['SYNC_LDAP_URL']
  d['SYNC_LDAP_BIND_DN'] = params.config['configurations']['usersync-properties']['SYNC_LDAP_BIND_DN']
  d['SYNC_LDAP_BIND_PASSWORD'] = params.config['configurations']['usersync-properties']['SYNC_LDAP_BIND_PASSWORD']
  d['CRED_KEYSTORE_FILENAME'] = params.config['configurations']['usersync-properties']['CRED_KEYSTORE_FILENAME']
  d['SYNC_LDAP_USER_SEARCH_BASE'] = params.config['configurations']['usersync-properties']['SYNC_LDAP_USER_SEARCH_BASE']
  d['SYNC_LDAP_USER_SEARCH_SCOPE'] = params.config['configurations']['usersync-properties'][
    'SYNC_LDAP_USER_SEARCH_SCOPE']
  d['SYNC_LDAP_USER_OBJECT_CLASS'] = params.config['configurations']['usersync-properties'][
    'SYNC_LDAP_USER_OBJECT_CLASS']
  d['SYNC_LDAP_USER_SEARCH_FILTER'] = params.config['configurations']['usersync-properties'][
    'SYNC_LDAP_USER_SEARCH_FILTER']
  d['SYNC_LDAP_USER_NAME_ATTRIBUTE'] = params.config['configurations']['usersync-properties'][
    'SYNC_LDAP_USER_NAME_ATTRIBUTE']
  d['SYNC_LDAP_USER_GROUP_NAME_ATTRIBUTE'] = params.config['configurations']['usersync-properties'][
    'SYNC_LDAP_USER_GROUP_NAME_ATTRIBUTE']
  d['SYNC_LDAP_USERNAME_CASE_CONVERSION'] = params.config['configurations']['usersync-properties'][
    'SYNC_LDAP_USERNAME_CASE_CONVERSION']
  d['SYNC_LDAP_GROUPNAME_CASE_CONVERSION'] = params.config['configurations']['usersync-properties'][
    'SYNC_LDAP_GROUPNAME_CASE_CONVERSION']
  d['logdir'] = params.config['configurations']['usersync-properties']['logdir']

  return d


def check_db_connnection(env):
  import params

  env.set_params(params)

  db_root_password = params.config['configurations']['admin-properties']["db_root_password"]
  db_root_user = params.config['configurations']['admin-properties']["db_root_user"]
  db_host = params.config['configurations']['admin-properties']['db_host']
  sql_command_invoker = params.config['configurations']['admin-properties']['SQL_COMMAND_INVOKER']

  Logger.info('Checking MYSQL root password')

  cmd_str = "\"" + sql_command_invoker + "\"" + " -u " + db_root_user + " --password=" + db_root_password + " -h " + db_host + " -s -e \"select version();\""
  status, output = get_status_output(cmd_str)

  if status == 0:
    Logger.info('Checking MYSQL root password DONE')
    return True
  else:
    Logger.info(
      'Ranger Admin installation Failed! Ranger requires DB client installed on Ranger Host and DB server running on DB Host')
    sys.exit(1)


def get_status_output(cmd):
  import subprocess

  ret = subprocess.call(cmd, shell=True)
  return ret, ret
