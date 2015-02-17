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
import subprocess
import json
import re
import os
from resource_management import *
from resource_management.libraries.functions.ranger_functions import Rangeradmin
from resource_management.core.logger import Logger

def setup_ranger_hive():
  import params

  if params.has_ranger_admin:
    File(params.ranger_downloaded_custom_connector,
         content = DownloadSource(params.ranger_driver_curl_source),
    )

    if not os.path.isfile(params.ranger_driver_curl_target):
      Execute(('cp', '--remove-destination', params.ranger_downloaded_custom_connector, params.ranger_driver_curl_target),
              path=["/bin", "/usr/bin/"],
              sudo=True)     

    try:
      command = 'hdp-select status hive-server2'
      return_code, hdp_output = shell.call(command, timeout=20)
    except Exception, e:
      Logger.error(str(e))
      raise Fail('Unable to execute hdp-select command to retrieve the version.')

    if return_code != 0:
      raise Fail('Unable to determine the current version because of a non-zero return code of {0}'.format(str(return_code)))

    hdp_version = re.sub('hive-server2 - ', '', hdp_output).strip()
    match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+-[0-9]+', hdp_version)

    if match is None:
      raise Fail('Failed to get extracted version')

    file_path = '/usr/hdp/'+ hdp_version +'/ranger-hive-plugin/install.properties'
    if not os.path.isfile(file_path):
      raise Fail('Ranger Hive plugin install.properties file does not exist at {0}'.format(file_path))

    ranger_hive_dict = ranger_hive_properties()
    hive_repo_data = hive_repo_properties()

    write_properties_to_file(file_path, ranger_hive_dict)

    if params.enable_ranger_hive:
      cmd = format('cd /usr/hdp/{hdp_version}/ranger-hive-plugin/ && sh enable-hive-plugin.sh')
      ranger_adm_obj = Rangeradmin(url=ranger_hive_dict['POLICY_MGR_URL'])
      response_code, response_recieved = ranger_adm_obj.check_ranger_login_urllib2(ranger_hive_dict['POLICY_MGR_URL'] + '/login.jsp', 'test:test')

      if response_code is not None and response_code == 200:
        ambari_ranger_admin, ambari_ranger_password = ranger_adm_obj.create_ambari_admin_user(params.ambari_ranger_admin, params.ambari_ranger_password, params.admin_uname_password)
        ambari_username_password_for_ranger = ambari_ranger_admin + ':' + ambari_ranger_password
        if ambari_ranger_admin != '' and ambari_ranger_password != '':
          repo = ranger_adm_obj.get_repository_by_name_urllib2(ranger_hive_dict['REPOSITORY_NAME'], 'hive', 'true', ambari_username_password_for_ranger)

          if repo and repo['name'] ==  ranger_hive_dict['REPOSITORY_NAME']:
            Logger.info('Hive Repository exist')
          else:
            response = ranger_adm_obj.create_repository_urllib2(hive_repo_data,ambari_username_password_for_ranger, params.policy_user)
            if response is not None:
              Logger.info('Hive Repository created in Ranger Admin')
            else:
              Logger.info('Hive Repository creation failed in Ranger Admin')
        else:
          Logger.info('Ambari admin username and password are blank ')
      else:
        Logger.info('Ranger service is not started on given host')
    else:
      cmd = format('cd /usr/hdp/{hdp_version}/ranger-hive-plugin/ && sh disable-hive-plugin.sh')
    
    Execute(cmd, environment={'JAVA_HOME': params.java64_home}, logoutput=True)
  else:
    Logger.info('Ranger admin not installed')


def write_properties_to_file(file_path, value):
  for key in value:
    modify_config(file_path, key, value[key])


def modify_config(filepath, variable, setting):
  var_found = False
  already_set = False
  V=str(variable)
  S=str(setting)
  # use quotes if setting has spaces #
  if ' ' in S:
    S = '%s' % S

  for line in fileinput.input(filepath, inplace = 1):
    # process lines that look like config settings #
    if not line.lstrip(' ').startswith('#') and '=' in line:
      _infile_var = str(line.split('=')[0].rstrip(' '))
      _infile_set = str(line.split('=')[1].lstrip(' ').rstrip())
      # only change the first matching occurrence #
      if var_found == False and _infile_var.rstrip(' ') == V:
        var_found = True
        # don't change it if it is already set #
        if _infile_set.lstrip(' ') == S:
          already_set = True
        else:
          line = "%s=%s\n" % (V, S)

    sys.stdout.write(line)

  # Append the variable if it wasn't found #
  if not var_found:
    with open(filepath, "a") as f:
      f.write("%s=%s\n" % (V, S))
  elif already_set == True:
    pass
  else:
    pass

  return

def ranger_hive_properties():
  import params

  ranger_hive_properties = dict()

  ranger_hive_properties['POLICY_MGR_URL'] = params.policymgr_mgr_url
  ranger_hive_properties['SQL_CONNECTOR_JAR'] = params.sql_connector_jar
  ranger_hive_properties['XAAUDIT.DB.FLAVOUR'] = params.xa_audit_db_flavor
  ranger_hive_properties['XAAUDIT.DB.DATABASE_NAME'] = params.xa_audit_db_name
  ranger_hive_properties['XAAUDIT.DB.USER_NAME'] = params.xa_audit_db_user
  ranger_hive_properties['XAAUDIT.DB.PASSWORD'] = params.xa_audit_db_password
  ranger_hive_properties['XAAUDIT.DB.HOSTNAME'] = params.xa_db_host
  ranger_hive_properties['REPOSITORY_NAME'] = params.repo_name
  ranger_hive_properties['XAAUDIT.DB.IS_ENABLED'] = params.db_enabled

  ranger_hive_properties['XAAUDIT.HDFS.IS_ENABLED'] = params.hdfs_enabled
  ranger_hive_properties['XAAUDIT.HDFS.DESTINATION_DIRECTORY'] = params.hdfs_dest_dir
  ranger_hive_properties['XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY'] = params.hdfs_buffer_dir
  ranger_hive_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY'] = params.hdfs_archive_dir
  ranger_hive_properties['XAAUDIT.HDFS.DESTINTATION_FILE'] = params.hdfs_dest_file
  ranger_hive_properties['XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS'] = params.hdfs_dest_flush_int_sec
  ranger_hive_properties['XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS'] = params.hdfs_dest_rollover_int_sec
  ranger_hive_properties['XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS'] = params.hdfs_dest_open_retry_int_sec
  ranger_hive_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FILE'] = params.hdfs_buffer_file
  ranger_hive_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS'] = params.hdfs_buffer_flush_int_sec
  ranger_hive_properties['XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS'] = params.hdfs_buffer_rollover_int_sec
  ranger_hive_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT'] = params.hdfs_archive_max_file_count

  ranger_hive_properties['SSL_KEYSTORE_FILE_PATH'] = params.ssl_keystore_file
  ranger_hive_properties['SSL_KEYSTORE_PASSWORD'] = params.ssl_keystore_password
  ranger_hive_properties['SSL_TRUSTSTORE_FILE_PATH'] = params.ssl_truststore_file
  ranger_hive_properties['SSL_TRUSTSTORE_PASSWORD'] = params.ssl_truststore_password
   
  ranger_hive_properties['UPDATE_XAPOLICIES_ON_GRANT_REVOKE'] = params.grant_revoke

  return ranger_hive_properties

def hive_repo_properties():
  import params

  config_dict = dict()
  config_dict['username'] = params.repo_config_username
  config_dict['password'] = params.repo_config_password
  config_dict['jdbc.driverClassName'] = params.jdbc_driver_class_name
  if params.security_enabled:
    config_dict['jdbc.url'] = format("{params.hive_url}/default;principal={params.hive_principal}")
  else:  
    config_dict['jdbc.url'] = params.hive_url
  config_dict['commonNameForCertificate'] = params.common_name_for_certificate

  repo= dict()
  repo['isActive'] = "true"
  repo['config'] = json.dumps(config_dict)
  repo['description'] = "hive repo"
  repo['name'] = params.repo_name
  repo['repositoryType'] = "Hive"
  repo['assetType'] = '3'

  data = json.dumps(repo)

  return data
