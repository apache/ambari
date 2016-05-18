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
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import urllib2, base64, httplib
from StringIO import StringIO as BytesIO
from datetime import datetime
from resource_management.core.resources.system import File, Directory, Execute
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.resources.modify_properties_file import ModifyPropertiesFile
from resource_management.core.source import DownloadSource, InlineTemplate
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.ranger_functions import Rangeradmin
from resource_management.libraries.functions.ranger_functions_v2 import RangeradminV2
from resource_management.libraries.functions.decorator import safe_retry
from resource_management.core.utils import PasswordString
from resource_management.core.shell import as_sudo
import re
import time
import socket

def password_validation(password, key):
  import params
  if password.strip() == "":
    raise Fail("Blank password is not allowed for {0} property. Please enter valid password.".format(key))
  if re.search("[\\\`'\"]",password):
    raise Fail("{0} password contains one of the unsupported special characters like \" ' \ `".format(key))
  else:
    Logger.info("Password validated")

def setup_kms_db(stack_version=None):
  import params

  if params.has_ranger_admin:

    kms_home = params.kms_home
    version = params.version
    if stack_version is not None:
      kms_home = format("{stack_root}/{stack_version}/ranger-kms")
      version = stack_version

    password_validation(params.kms_master_key_password, 'KMS master key')

    copy_jdbc_connector(stack_version=version)

    env_dict = {'RANGER_KMS_HOME':kms_home, 'JAVA_HOME': params.java_home}
    if params.db_flavor.lower() == 'sqla':
      env_dict = {'RANGER_KMS_HOME':kms_home, 'JAVA_HOME': params.java_home, 'LD_LIBRARY_PATH':params.ld_library_path}

    dba_setup = format('ambari-python-wrap {kms_home}/dba_script.py -q')
    db_setup = format('ambari-python-wrap {kms_home}/db_setup.py')

    if params.create_db_user:
      Logger.info('Setting up Ranger KMS DB and DB User')
      Execute(dba_setup, environment=env_dict, logoutput=True, user=params.kms_user, tries=5, try_sleep=10)
    else:
      Logger.info('Separate DBA property not set. Assuming Ranger KMS DB and DB User exists!')
    Execute(db_setup, environment=env_dict, logoutput=True, user=params.kms_user, tries=5, try_sleep=10)

def setup_java_patch():
  import params

  if params.has_ranger_admin:

    kms_home = params.kms_home
    setup_java_patch = format('ambari-python-wrap {kms_home}/db_setup.py -javapatch')

    env_dict = {'RANGER_KMS_HOME':kms_home, 'JAVA_HOME': params.java_home}
    if params.db_flavor.lower() == 'sqla':
      env_dict = {'RANGER_KMS_HOME':kms_home, 'JAVA_HOME': params.java_home, 'LD_LIBRARY_PATH':params.ld_library_path}

    Execute(setup_java_patch, environment=env_dict, logoutput=True, user=params.kms_user, tries=5, try_sleep=10)

    kms_lib_path = format('{kms_home}/ews/webapp/lib/')
    files = os.listdir(kms_lib_path)
    hadoop_jar_files = []

    for x in files:
      if x.startswith('hadoop-common') and x.endswith('.jar'):
        hadoop_jar_files.append(x)

    if len(hadoop_jar_files) != 0:
      for f in hadoop_jar_files:
        Execute((format('{java_home}/bin/jar'),'-uf', format('{kms_home}/ews/webapp/lib/{f}'), format('{kms_home}/ews/webapp/META-INF/services/org.apache.hadoop.crypto.key.KeyProviderFactory')),
          user=params.kms_user)

        File(format('{kms_home}/ews/webapp/lib/{f}'), owner=params.kms_user, group=params.kms_group)


def do_keystore_setup(cred_provider_path, credential_alias, credential_password): 
  import params

  if cred_provider_path is not None:
    java_bin = format('{java_home}/bin/java')
    file_path = format('jceks://file{cred_provider_path}')
    cmd = (java_bin, '-cp', params.cred_lib_path, 'org.apache.ranger.credentialapi.buildks', 'create', credential_alias, '-value', PasswordString(credential_password), '-provider', file_path)
    Execute(cmd,
            environment={'JAVA_HOME': params.java_home}, 
            logoutput=True, 
            sudo=True,
    )

    File(cred_provider_path,
      owner = params.kms_user,
      group = params.kms_group,
      mode = 0640
    )

def kms(upgrade_type=None):
  import params

  if params.has_ranger_admin:

    Directory(params.kms_conf_dir,
      owner = params.kms_user,
      group = params.kms_group,
      create_parents = True
    )

    if upgrade_type is not None:
      copy_jdbc_connector(stack_version=params.version)

    File(format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
      content = DownloadSource(format("{jdk_location}{check_db_connection_jar_name}")),
      mode = 0644,
    )

    cp = format("{check_db_connection_jar}")
    if params.db_flavor.lower() == 'sqla':
      cp = cp + os.pathsep + format("{kms_home}/ews/webapp/lib/sajdbc4.jar")
    else:
      cp = cp + os.pathsep + format("{kms_home}/ews/webapp/lib/{jdbc_jar_name}")

    db_connection_check_command = format(
      "{java_home}/bin/java -cp {cp} org.apache.ambari.server.DBConnectionVerification '{ranger_kms_jdbc_connection_url}' {db_user} {db_password!p} {ranger_kms_jdbc_driver}")
    
    env_dict = {}
    if params.db_flavor.lower() == 'sqla':
      env_dict = {'LD_LIBRARY_PATH':params.ld_library_path}

    Execute(db_connection_check_command, path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin', tries=5, try_sleep=10, environment=env_dict)

    if params.xa_audit_db_is_enabled:
      File(params.downloaded_connector_path,
        content = DownloadSource(params.driver_source),
        mode = 0644
      )

      Execute(('cp', '--remove-destination', params.downloaded_connector_path, params.driver_target),
          path=["/bin", "/usr/bin/"],
          sudo=True)

      File(params.driver_target, mode=0644)

    Directory(os.path.join(params.kms_home, 'ews', 'webapp', 'WEB-INF', 'classes', 'lib'),
        mode=0755,
        owner=params.kms_user,
        group=params.kms_group        
      )

    Execute(('cp',format('{kms_home}/ranger-kms-initd'),'/etc/init.d/ranger-kms'),
    not_if=format('ls /etc/init.d/ranger-kms'),
    only_if=format('ls {kms_home}/ranger-kms-initd'),
    sudo=True)

    File('/etc/init.d/ranger-kms',
      mode = 0755
    )

    Directory(format('{kms_home}/'),
              owner = params.kms_user,
              group = params.kms_group,
              recursive_ownership = True,
    )

    Directory(params.kms_log_dir,
      owner = params.kms_user,
      group = params.kms_group,
      cd_access = 'a',
      create_parents=True,
      mode=0755
    )

    File(format('{kms_conf_dir}/ranger-kms-env-logdir.sh'),
      content = format("export RANGER_KMS_LOG_DIR={kms_log_dir}"),
      owner = params.kms_user,
      group = params.kms_group,
      mode=0755
    )

    Execute(('ln','-sf', format('{kms_home}/ranger-kms'),'/usr/bin/ranger-kms'),
      not_if=format('ls /usr/bin/ranger-kms'),
      only_if=format('ls {kms_home}/ranger-kms'),
      sudo=True)

    File('/usr/bin/ranger-kms', mode = 0755)

    Execute(('ln','-sf', format('{kms_home}/ranger-kms'),'/usr/bin/ranger-kms-services.sh'),
      not_if=format('ls /usr/bin/ranger-kms-services.sh'),
      only_if=format('ls {kms_home}/ranger-kms'),
      sudo=True)

    File('/usr/bin/ranger-kms-services.sh', mode = 0755)

    Execute(('ln','-sf', format('{kms_home}/ranger-kms-initd'),format('{kms_home}/ranger-kms-services.sh')),
      not_if=format('ls {kms_home}/ranger-kms-services.sh'),
      only_if=format('ls {kms_home}/ranger-kms-initd'),
      sudo=True)

    File(format('{kms_home}/ranger-kms-services.sh'), mode = 0755)

    Directory(params.kms_log_dir,
      owner = params.kms_user,
      group = params.kms_group,
      mode = 0775
    )

    do_keystore_setup(params.credential_provider_path, params.jdbc_alias, params.db_password)
    do_keystore_setup(params.credential_provider_path, params.masterkey_alias, params.kms_master_key_password)
    if params.stack_support_kms_hsm and params.enable_kms_hsm:
      do_keystore_setup(params.credential_provider_path, params.hms_partition_alias, unicode(params.hms_partition_passwd))

    XmlConfig("dbks-site.xml",
      conf_dir=params.kms_conf_dir,
      configurations=params.config['configurations']['dbks-site'],
      configuration_attributes=params.config['configuration_attributes']['dbks-site'],
      owner=params.kms_user,
      group=params.kms_group,
      mode=0644
    )

    XmlConfig("ranger-kms-site.xml",
      conf_dir=params.kms_conf_dir,
      configurations=params.config['configurations']['ranger-kms-site'],
      configuration_attributes=params.config['configuration_attributes']['ranger-kms-site'],
      owner=params.kms_user,
      group=params.kms_group,
      mode=0644
    )

    XmlConfig("kms-site.xml",
      conf_dir=params.kms_conf_dir,
      configurations=params.config['configurations']['kms-site'],
      configuration_attributes=params.config['configuration_attributes']['kms-site'],
      owner=params.kms_user,
      group=params.kms_group,
      mode=0644
    )

    File(os.path.join(params.kms_conf_dir, "kms-log4j.properties"),
      owner=params.kms_user,
      group=params.kms_group,
      content=params.kms_log4j,
      mode=0644
    )
    if params.security_enabled:
      # core-site.xml linking required by setup for HDFS encryption
      XmlConfig("core-site.xml",
        conf_dir=params.kms_conf_dir,
        configurations=params.config['configurations']['core-site'],
        configuration_attributes=params.config['configuration_attributes']['core-site'],
        owner=params.kms_user,
        group=params.kms_group,
        mode=0644
      )

def copy_jdbc_connector(stack_version=None):
  import params

  kms_home = params.kms_home
  if stack_version is not None:
    kms_home = format("{stack_root}/{stack_version}/ranger-kms")

  File(params.downloaded_custom_connector,
    content = DownloadSource(params.driver_curl_source),
    mode = 0644
  )

  Directory(os.path.join(kms_home, 'ews', 'lib'),
    mode=0755
  )

  if params.db_flavor.lower() == 'sqla':
    Execute(('tar', '-xvf', params.downloaded_custom_connector, '-C', params.tmp_dir), sudo = True)

    Execute(('cp', '--remove-destination', params.jar_path_in_archive, os.path.join(kms_home, 'ews', 'webapp', 'lib')),
      path=["/bin", "/usr/bin/"],
      sudo=True)

    Directory(params.jdbc_libs_dir,
      cd_access="a",
      create_parents=True)

    Execute(as_sudo(['yes', '|', 'cp', params.libs_path_in_archive, params.jdbc_libs_dir], auto_escape=False),
      path=["/bin", "/usr/bin/"])

    File(os.path.join(kms_home, 'ews', 'webapp', 'lib', 'sajdbc4.jar'), mode=0644)
  else:
    Execute(('cp', '--remove-destination', params.downloaded_custom_connector, os.path.join(kms_home, 'ews', 'webapp', 'lib')),
      path=["/bin", "/usr/bin/"],
      sudo=True)

    File(os.path.join(kms_home, 'ews', 'webapp', 'lib', params.jdbc_jar_name), mode=0644)

  ModifyPropertiesFile(format("{kms_home}/install.properties"),
    properties = params.config['configurations']['kms-properties'],
    owner = params.kms_user
  )

  if params.db_flavor.lower() == 'sqla':
    ModifyPropertiesFile(format("{kms_home}/install.properties"),
      properties = {'SQL_CONNECTOR_JAR': format('{kms_home}/ews/webapp/lib/sajdbc4.jar')},
      owner = params.kms_user,
    )
  else:
    ModifyPropertiesFile(format("{kms_home}/install.properties"),
      properties = {'SQL_CONNECTOR_JAR': format('{driver_curl_target}')},
      owner = params.kms_user,
    )

def enable_kms_plugin():

  import params

  if params.has_ranger_admin:

    if params.stack_supports_ranger_kerberos and params.security_enabled:
      ranger_flag = check_ranger_service_support_kerberos()
    else:
      ranger_flag = check_ranger_service()

    if not ranger_flag:
      Logger.error('Error in Get/Create service for Ranger Kms.')

    current_datetime = datetime.now()

    File(format('{kms_conf_dir}/ranger-security.xml'),
      owner = params.kms_user,
      group = params.kms_group,
      mode = 0644,
      content = InlineTemplate(format('<ranger>\n<enabled>{current_datetime}</enabled>\n</ranger>'))
    )

    Directory([os.path.join('/etc', 'ranger', params.repo_name), os.path.join('/etc', 'ranger', params.repo_name, 'policycache')],
      owner = params.kms_user,
      group = params.kms_group,
      mode=0775,
      create_parents = True
    )
    
    File(os.path.join('/etc', 'ranger', params.repo_name, 'policycache',format('kms_{repo_name}.json')),
      owner = params.kms_user,
      group = params.kms_group,
      mode = 0644        
    )

    XmlConfig("ranger-kms-audit.xml",
      conf_dir=params.kms_conf_dir,
      configurations=params.config['configurations']['ranger-kms-audit'],
      configuration_attributes=params.config['configuration_attributes']['ranger-kms-audit'],
      owner=params.kms_user,
      group=params.kms_group,
      mode=0744)

    XmlConfig("ranger-kms-security.xml",
      conf_dir=params.kms_conf_dir,
      configurations=params.config['configurations']['ranger-kms-security'],
      configuration_attributes=params.config['configuration_attributes']['ranger-kms-security'],
      owner=params.kms_user,
      group=params.kms_group,
      mode=0744)

    XmlConfig("ranger-policymgr-ssl.xml",
      conf_dir=params.kms_conf_dir,
      configurations=params.config['configurations']['ranger-kms-policymgr-ssl'],
      configuration_attributes=params.config['configuration_attributes']['ranger-kms-policymgr-ssl'],
      owner=params.kms_user,
      group=params.kms_group,
      mode=0744)

    if params.xa_audit_db_is_enabled:
      cred_setup = params.cred_setup_prefix + ('-f', params.credential_file, '-k', 'auditDBCred', '-v', PasswordString(params.xa_audit_db_password), '-c', '1')
      Execute(cred_setup, environment={'JAVA_HOME': params.java_home}, logoutput=True, sudo=True)

    cred_setup = params.cred_setup_prefix + ('-f', params.credential_file, '-k', 'sslKeyStore', '-v', PasswordString(params.ssl_keystore_password), '-c', '1')
    Execute(cred_setup, environment={'JAVA_HOME': params.java_home}, logoutput=True, sudo=True)

    cred_setup = params.cred_setup_prefix + ('-f', params.credential_file, '-k', 'sslTrustStore', '-v', PasswordString(params.ssl_truststore_password), '-c', '1')
    Execute(cred_setup, environment={'JAVA_HOME': params.java_home}, logoutput=True, sudo=True)

    File(params.credential_file,
      owner = params.kms_user,
      group = params.kms_group,
      mode = 0640
      )

def setup_kms_jce():
  import params

  if params.jce_name is not None:
    Directory(params.jce_source_dir,
      create_parents = True
    )

    jce_target = format('{jce_source_dir}/{jce_name}')

    File(jce_target,
      content = DownloadSource(format('{jdk_location}/{jce_name}')),
      mode = 0644,
    )

    File([format("{java_home}/jre/lib/security/local_policy.jar"), format("{java_home}/jre/lib/security/US_export_policy.jar")],
      action = "delete",
    )

    unzip_cmd = ("unzip", "-o", "-j", "-q", jce_target, "-d", format("{java_home}/jre/lib/security"))

    Execute(unzip_cmd,
      only_if = format("test -e {java_home}/jre/lib/security && test -f {jce_target}"),
      path = ['/bin/','/usr/bin'],
      sudo = True
    )
  else:
    Logger.warning("Required jce policy zip is not available, need to setup manually")

  
def check_ranger_service():
  import params

  ranger_adm_obj = Rangeradmin(url=params.policymgr_mgr_url)
  ambari_username_password_for_ranger = format("{ambari_ranger_admin}:{ambari_ranger_password}")
  response_code = ranger_adm_obj.check_ranger_login_urllib2(params.policymgr_mgr_url)

  if response_code is not None and response_code == 200:
    user_resp_code = ranger_adm_obj.create_ambari_admin_user(params.ambari_ranger_admin, params.ambari_ranger_password, params.admin_uname_password)
    if user_resp_code is not None and user_resp_code == 200:
      get_repo_flag = get_repo(params.policymgr_mgr_url, params.repo_name, ambari_username_password_for_ranger)
      if not get_repo_flag:
        return create_repo(params.policymgr_mgr_url, json.dumps(params.kms_ranger_plugin_repo), ambari_username_password_for_ranger)
      else:
        return True
    else:
      return False
  else:
    Logger.error('Ranger service is not reachable')
    return False

@safe_retry(times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=False)
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
    result = urllib2.urlopen(request, timeout=20)
    response_code = result.getcode()
    response = json.loads(json.JSONEncoder().encode(result.read()))
    if response_code == 200:
      Logger.info('Repository created Successfully')
      return True
    else:
      Logger.info('Repository not created')
      return False
  except urllib2.URLError, e:
    if isinstance(e, urllib2.HTTPError):
      raise Fail("Error creating service. Http status code - {0}. \n {1}".format(e.code, e.read()))
    else:
      raise Fail("Error creating service. Reason - {0}.".format(e.reason))
  except socket.timeout as e:
    raise Fail("Error creating service. Reason - {0}".format(e))

@safe_retry(times=5, sleep_time=8, backoff_factor=1.5, err_class=Fail, return_on_fail=False)
def get_repo(url, name, usernamepassword):
  try:
    base_url = url + '/service/public/v2/api/service?serviceName=' + name + '&serviceType=kms&isEnabled=true'
    request = urllib2.Request(base_url)
    base64string = base64.encodestring(usernamepassword).replace('\n', '')
    request.add_header("Content-Type", "application/json")
    request.add_header("Accept", "application/json")
    request.add_header("Authorization", "Basic {0}".format(base64string))
    result = urllib2.urlopen(request, timeout=20)
    response_code = result.getcode()
    response = json.loads(result.read())
    if response_code == 200 and len(response) > 0:
      for repo in response:
        if repo.get('name').lower() == name.lower() and repo.has_key('name'):
          Logger.info('KMS repository exist')
          return True
        else:
          Logger.info('KMS repository doesnot exist')
          return False
    else:
      Logger.info('KMS repository doesnot exist')
      return False
  except urllib2.URLError, e:
    if isinstance(e, urllib2.HTTPError):
      raise Fail("Error getting {0} service. Http status code - {1}. \n {2}".format(name, e.code, e.read()))
    else:
      raise Fail("Error getting {0} service. Reason - {1}.".format(name, e.reason))
  except socket.timeout as e:
    raise Fail("Error creating service. Reason - {0}".format(e))

def check_ranger_service_support_kerberos():
  import params

  ranger_adm_obj = RangeradminV2(url=params.policymgr_mgr_url)
  response_code = ranger_adm_obj.check_ranger_login_curl(params.kms_user, params.rangerkms_keytab, params.rangerkms_principal, params.policymgr_mgr_url, True)

  if response_code is not None and response_code[0] == 200:
    get_repo_name_response = ranger_adm_obj.get_repository_by_name_curl(params.kms_user, params.rangerkms_keytab, params.rangerkms_principal, params.repo_name, 'kms', 'true')
    if get_repo_name_response is not None:
      Logger.info('KMS repository {0} exist'.format(get_repo_name_response['name']))
      return True
    else:
      create_repo_response = ranger_adm_obj.create_repository_curl(params.kms_user, params.rangerkms_keytab, params.rangerkms_principal, params.repo_name, json.dumps(params.kms_ranger_plugin_repo), None)
      if create_repo_response is not None and len(create_repo_response) > 0:
        return True
      else:
        return False
  else:
    Logger.error('Ranger service is not reachable')
    return False
