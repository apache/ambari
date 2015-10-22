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
__all__ = ["setup_ranger_plugin"]


import os
from datetime import datetime
from resource_management.libraries.functions.ranger_functions import Rangeradmin
from resource_management.core.resources import File, Directory, Execute
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_hdp_version import get_hdp_version
from resource_management.core.logger import Logger
from resource_management.core.source import DownloadSource, InlineTemplate
from resource_management.libraries.functions.ranger_functions_v2 import RangeradminV2
from resource_management.core.utils import PasswordString

def setup_ranger_plugin(component_select_name, service_name,
                        component_downloaded_custom_connector, component_driver_curl_source,
                        component_driver_curl_target, java_home,
                        repo_name, plugin_repo_dict,
                        ranger_env_properties, plugin_properties,
                        policy_user, policymgr_mgr_url,
                        plugin_enabled, conf_dict, component_user, component_group,
                        cache_service_list, plugin_audit_properties, plugin_audit_attributes,
                        plugin_security_properties, plugin_security_attributes,
                        plugin_policymgr_ssl_properties, plugin_policymgr_ssl_attributes,
                        component_list, audit_db_is_enabled, credential_file, 
                        xa_audit_db_password, ssl_truststore_password,
                        ssl_keystore_password, api_version=None, hdp_version_override = None, skip_if_rangeradmin_down = True):

  if audit_db_is_enabled:
    File(component_downloaded_custom_connector,
      content = DownloadSource(component_driver_curl_source),
      mode = 0644
    )

    Execute(('cp', '--remove-destination', component_downloaded_custom_connector, component_driver_curl_target),
      path=["/bin", "/usr/bin/"],
      sudo=True
    )

    File(component_driver_curl_target, mode=0644)

  hdp_version = get_hdp_version(component_select_name)
  if hdp_version_override is not None:
    hdp_version = hdp_version_override

  component_conf_dir = conf_dict
  
  if plugin_enabled:

    if api_version == 'v2' and api_version is not None:
      ranger_adm_obj = RangeradminV2(url=policymgr_mgr_url, skip_if_rangeradmin_down=skip_if_rangeradmin_down)
    else:
      ranger_adm_obj = Rangeradmin(url=policymgr_mgr_url, skip_if_rangeradmin_down=skip_if_rangeradmin_down)

    ranger_adm_obj.create_ranger_repository(service_name, repo_name, plugin_repo_dict,
                                            ranger_env_properties['ranger_admin_username'], ranger_env_properties['ranger_admin_password'],
                                            ranger_env_properties['admin_username'], ranger_env_properties['admin_password'],
                                            policy_user)

    current_datetime = datetime.now()
    
    File(format('{component_conf_dir}/ranger-security.xml'),
      owner = component_user,
      group = component_group,
      mode = 0644,
      content = InlineTemplate(format('<ranger>\n<enabled>{current_datetime}</enabled>\n</ranger>'))
    )

    Directory([os.path.join('/etc', 'ranger', repo_name), os.path.join('/etc', 'ranger', repo_name, 'policycache')],
      owner = component_user,
      group = component_group,
      mode=0775,
      recursive = True,
      cd_access = 'a'
    )

    for cache_service in cache_service_list:
      File(os.path.join('/etc', 'ranger', repo_name, 'policycache',format('{cache_service}_{repo_name}.json')),
        owner = component_user,
        group = component_group,
        mode = 0644
      )

    XmlConfig(format('ranger-{service_name}-audit.xml'),
      conf_dir=component_conf_dir,
      configurations=plugin_audit_properties,
      configuration_attributes=plugin_audit_attributes,
      owner = component_user,
      group = component_group,
      mode=0744)

    XmlConfig(format('ranger-{service_name}-security.xml'),
      conf_dir=component_conf_dir,
      configurations=plugin_security_properties,
      configuration_attributes=plugin_security_attributes,
      owner = component_user,
      group = component_group,
      mode=0744)

    if str(service_name).lower() == 'yarn' :
      XmlConfig("ranger-policymgr-ssl-yarn.xml",
        conf_dir=component_conf_dir,
        configurations=plugin_policymgr_ssl_properties,
        configuration_attributes=plugin_policymgr_ssl_attributes,
        owner = component_user,
        group = component_group,
        mode=0744) 
    else :
      XmlConfig("ranger-policymgr-ssl.xml",
        conf_dir=component_conf_dir,
        configurations=plugin_policymgr_ssl_properties,
        configuration_attributes=plugin_policymgr_ssl_attributes,
        owner = component_user,
        group = component_group,
        mode=0744) 

    #This should be done by rpm
    #setup_ranger_plugin_jar_symblink(hdp_version, service_name, component_list)

    setup_ranger_plugin_keystore(service_name, audit_db_is_enabled, hdp_version, credential_file,
              xa_audit_db_password, ssl_truststore_password, ssl_keystore_password,
              component_user, component_group, java_home)

  else:
    File(format('{component_conf_dir}/ranger-security.xml'),
      action="delete"      
    )    


def setup_ranger_plugin_jar_symblink(hdp_version, service_name, component_list):

  jar_files = os.listdir(format('/usr/hdp/{hdp_version}/ranger-{service_name}-plugin/lib'))

  for jar_file in jar_files:
    for component in component_list:
      Execute(('ln','-sf',format('/usr/hdp/{hdp_version}/ranger-{service_name}-plugin/lib/{jar_file}'),format('/usr/hdp/current/{component}/lib/{jar_file}')),
      not_if=format('ls /usr/hdp/current/{component}/lib/{jar_file}'),
      only_if=format('ls /usr/hdp/{hdp_version}/ranger-{service_name}-plugin/lib/{jar_file}'),
      sudo=True)

def setup_ranger_plugin_keystore(service_name, audit_db_is_enabled, hdp_version, credential_file, xa_audit_db_password,
                                ssl_truststore_password, ssl_keystore_password, component_user, component_group, java_home):

  cred_lib_path = format('/usr/hdp/{hdp_version}/ranger-{service_name}-plugin/install/lib/*')
  cred_setup_prefix = (format('/usr/hdp/{hdp_version}/ranger-{service_name}-plugin/ranger_credential_helper.py'), '-l', cred_lib_path)

  if audit_db_is_enabled:
    cred_setup = cred_setup_prefix + ('-f', credential_file, '-k', 'auditDBCred', '-v', PasswordString(xa_audit_db_password), '-c', '1')
    Execute(cred_setup, environment={'JAVA_HOME': java_home}, logoutput=True, sudo=True)

  cred_setup = cred_setup_prefix + ('-f', credential_file, '-k', 'sslKeyStore', '-v', PasswordString(ssl_keystore_password), '-c', '1')
  Execute(cred_setup, environment={'JAVA_HOME': java_home}, logoutput=True, sudo=True)

  cred_setup = cred_setup_prefix + ('-f', credential_file, '-k', 'sslTrustStore', '-v', PasswordString(ssl_truststore_password), '-c', '1')
  Execute(cred_setup, environment={'JAVA_HOME': java_home}, logoutput=True, sudo=True)

  File(credential_file,
    owner = component_user,
    group = component_group,
    mode = 0640
  )
