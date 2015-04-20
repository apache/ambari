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
import os
import json
import urllib2, base64, httplib
from StringIO import StringIO as BytesIO
from resource_management.core.resources.system import File, Directory, Execute
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.core.source import DownloadSource
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions.format import format
from resource_management.core.shell import as_sudo
from resource_management.libraries.functions.ranger_functions import Rangeradmin

def kms():
  import params

  if params.has_ranger_admin:

    File(params.downloaded_custom_connector,
      content = DownloadSource(params.driver_curl_source)
    )

    Directory(params.java_share_dir,
      mode=0755
    )

    if not os.path.isfile(params.driver_curl_target):
      Execute(('cp', '--remove-destination', params.downloaded_custom_connector, params.driver_curl_target),
              path=["/bin", "/usr/bin/"],
              sudo=True)

    XmlConfig("kms-acls.xml",
      conf_dir=params.kms_config_dir,
      configurations=params.config['configurations']['kms-acls'],
      configuration_attributes=params.config['configuration_attributes']['kms-acls'],
      owner=params.kms_user,
      group=params.kms_group
    )

    XmlConfig("kms-site.xml",
      conf_dir=params.kms_config_dir,
      configurations=params.config['configurations']['kms-site'],
      configuration_attributes=params.config['configuration_attributes']['kms-site'],
      owner=params.kms_user,
      group=params.kms_group
    )

    File(os.path.join(params.kms_config_dir, "kms-log4j.properties"),
      owner=params.kms_user,
      group=params.kms_group,
      content=params.kms_log4j
    )

    repo_data = kms_repo_properties()

    ranger_adm_obj = Rangeradmin(url=params.policymgr_mgr_url)
    response_code, response_recieved = ranger_adm_obj.check_ranger_login_urllib2(params.policymgr_mgr_url + '/login.jsp', 'test:test')
    if response_code is not None and response_code == 200:
      ambari_ranger_admin, ambari_ranger_password = ranger_adm_obj.create_ambari_admin_user(params.ambari_ranger_admin, params.ambari_ranger_password, params.admin_uname_password)
      ambari_username_password_for_ranger = ambari_ranger_admin + ':' + ambari_ranger_password
    else:
      raise Fail('Ranger service is not started on given host')      

    if ambari_ranger_admin != '' and ambari_ranger_password != '':  
      get_repo_flag = get_repo(params.policymgr_mgr_url, params.repo_name, ambari_username_password_for_ranger)
      if not get_repo_flag:
        create_repo(params.policymgr_mgr_url, repo_data, ambari_username_password_for_ranger)
    else:
      raise Fail('Ambari admin username and password not available')

    file_path = format('{kms_home}/install.properties')
    ranger_kms_dict = ranger_kms_properties()
    write_properties_to_file(file_path, ranger_kms_dict)

    env_dict = {'JAVA_HOME': params.java_home, 'RANGER_HOME': params.kms_home}
    setup_sh = format("cd {kms_home} && ") + as_sudo([format('{kms_home}/setup.sh')])
    Execute(setup_sh, environment=env_dict, logoutput=True)
  

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

def ranger_kms_properties():
  import params

  ranger_kms_properties = dict()

  ranger_kms_properties['DB_FLAVOR'] = params.db_flavor
  ranger_kms_properties['SQL_COMMAND_INVOKER'] = params.sql_command_invoker
  ranger_kms_properties['SQL_CONNECTOR_JAR'] = params.sql_connector_jar
  ranger_kms_properties['db_root_user'] = params.db_root_user
  ranger_kms_properties['db_root_password'] = params.db_root_password
  ranger_kms_properties['db_host'] = params.db_host
  ranger_kms_properties['db_name'] = params.db_name
  ranger_kms_properties['db_user'] = params.db_user
  ranger_kms_properties['db_password'] = params.db_password
  ranger_kms_properties['KMS_MASTER_KEY_PASSWD'] = params.kms_master_key_password

  ranger_kms_properties['POLICY_MGR_URL'] = params.policymgr_mgr_url
  ranger_kms_properties['REPOSITORY_NAME'] = params.repo_name

  ranger_kms_properties['XAAUDIT.DB.IS_ENABLED'] = str(params.db_enabled).lower()
  ranger_kms_properties['XAAUDIT.DB.FLAVOUR'] = params.xa_audit_db_flavor
  ranger_kms_properties['XAAUDIT.DB.DATABASE_NAME'] = params.xa_audit_db_name
  ranger_kms_properties['XAAUDIT.DB.USER_NAME'] = params.xa_audit_db_user
  ranger_kms_properties['XAAUDIT.DB.PASSWORD'] = params.xa_audit_db_password
  ranger_kms_properties['XAAUDIT.DB.HOSTNAME'] = params.xa_db_host

  ranger_kms_properties['XAAUDIT.SOLR.IS_ENABLED'] = str(params.solr_enabled).lower()
  ranger_kms_properties['XAAUDIT.SOLR.MAX_QUEUE_SIZE'] = params.solr_max_queue_size
  ranger_kms_properties['XAAUDIT.SOLR.MAX_FLUSH_INTERVAL_MS'] = params.solr_max_flush_interval
  ranger_kms_properties['XAAUDIT.SOLR.SOLR_URL'] = params.solr_url

  ranger_kms_properties['XAAUDIT.HDFS.IS_ENABLED'] = str(params.hdfs_enabled).lower()
  ranger_kms_properties['XAAUDIT.HDFS.DESTINATION_DIRECTORY'] = params.hdfs_dest_dir
  ranger_kms_properties['XAAUDIT.HDFS.LOCAL_BUFFER_DIRECTORY'] = params.hdfs_buffer_dir
  ranger_kms_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_DIRECTORY'] = params.hdfs_archive_dir
  ranger_kms_properties['XAAUDIT.HDFS.DESTINTATION_FILE'] = params.hdfs_dest_file
  ranger_kms_properties['XAAUDIT.HDFS.DESTINTATION_FLUSH_INTERVAL_SECONDS'] = params.hdfs_dest_flush_int_sec
  ranger_kms_properties['XAAUDIT.HDFS.DESTINTATION_ROLLOVER_INTERVAL_SECONDS'] = params.hdfs_dest_rollover_int_sec
  ranger_kms_properties['XAAUDIT.HDFS.DESTINTATION_OPEN_RETRY_INTERVAL_SECONDS'] = params.hdfs_dest_open_retry_int_sec
  ranger_kms_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FILE'] = params.hdfs_buffer_file
  ranger_kms_properties['XAAUDIT.HDFS.LOCAL_BUFFER_FLUSH_INTERVAL_SECONDS'] = params.hdfs_buffer_flush_int_sec
  ranger_kms_properties['XAAUDIT.HDFS.LOCAL_BUFFER_ROLLOVER_INTERVAL_SECONDS'] = params.hdfs_buffer_rollover_int_sec
  ranger_kms_properties['XAAUDIT.HDFS.LOCAL_ARCHIVE_MAX_FILE_COUNT'] = params.hdfs_archive_max_file_count

  ranger_kms_properties['SSL_KEYSTORE_FILE_PATH'] = params.ssl_keystore_file
  ranger_kms_properties['SSL_KEYSTORE_PASSWORD'] = params.ssl_keystore_password
  ranger_kms_properties['SSL_TRUSTSTORE_FILE_PATH'] = params.ssl_truststore_file
  ranger_kms_properties['SSL_TRUSTSTORE_PASSWORD'] = params.ssl_truststore_password

  return ranger_kms_properties

def kms_repo_properties():
  import params

  config_dict = dict()
  config_dict['username'] = 'kms'
  config_dict['password'] = 'kms'
  config_dict['provider'] = 'http://' + params.kms_host_name + ':9292/kms'
  
  repo= dict()
  repo['isEnabled'] = "true"
  repo['configs'] = config_dict
  repo['description'] = "kms repo"
  repo['name'] = params.repo_name
  repo['type'] = "kms"

  data = json.dumps(repo)

  return data

def create_repo(url, data, usernamepassword):
  try:
    base_url = url + '/service/public/v2/api/service'
    base64string = base64.encodestring('{0}'.format(usernamepassword)).replace('\n', '')
    headers = {
      'Accept': 'application/json',
      "Content-Type": "application/json"
    }
    request = urllib2.Request(base_url, data, headers)
    request.add_header("Authorization", "Basic {0}".format(base64string))
    result = urllib2.urlopen(request)
    response_code = result.getcode()
    response = json.loads(json.JSONEncoder().encode(result.read()))
    if response_code == 200:
      Logger.info('Repository created Successfully')
    else:
      Logger.info('Repository not created')
  except urllib2.URLError, e:
    raise Fail('Repository creation failed, {0}'.format(str(e)))  

def get_repo(url, name, usernamepassword):
  try:
    base_url = url + '/service/public/v2/api/service?serviceName=' + name + '&serviceType=kms&isEnabled=true'
    request = urllib2.Request(base_url)
    base64string = base64.encodestring(usernamepassword).replace('\n', '')
    request.add_header("Content-Type", "application/json")
    request.add_header("Accept", "application/json")
    request.add_header("Authorization", "Basic {0}".format(base64string))
    result = urllib2.urlopen(request)
    response_code = result.getcode()
    response = json.loads(result.read())
    if response_code == 200 and len(response) > 0:
      for repo in response:
        if repo.get('name') == name and repo.has_key('name'):
          Logger.info('KMS repository exist')
          return True
        else:
          Logger.info('KMS repository doesnot exist')
          return False
    else:
      Logger.info('KMS repository doesnot exist')
      return False
  except urllib2.URLError, e:
    raise Fail('Get repository failed, {0}'.format(str(e))) 
