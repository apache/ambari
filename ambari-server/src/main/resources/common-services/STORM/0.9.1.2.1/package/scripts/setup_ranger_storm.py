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
from resource_management.core.logger import Logger

def setup_ranger_storm(rolling_upgrade = False):
  import params

  if params.has_ranger_admin and params.security_enabled:

    if params.xml_configurations_supported:
      from resource_management.libraries.functions.setup_ranger_plugin_xml import setup_ranger_plugin
    else:
      from resource_management.libraries.functions.setup_ranger_plugin import setup_ranger_plugin
    
    hdp_version = None
    if rolling_upgrade:
      hdp_version = params.version

    setup_ranger_plugin('storm-nimbus', 'storm',
                        params.downloaded_custom_connector, params.driver_curl_source,
                        params.driver_curl_target, params.java64_home,
                        params.repo_name, params.storm_ranger_plugin_repo,
                        params.ranger_env, params.ranger_plugin_properties,
                        params.policy_user, params.policymgr_mgr_url,
                        params.enable_ranger_storm, conf_dict=params.conf_dir,
                        component_user=params.storm_user, component_group=params.user_group, cache_service_list=['storm'],
                        plugin_audit_properties=params.config['configurations']['ranger-storm-audit'], plugin_audit_attributes=params.config['configuration_attributes']['ranger-storm-audit'],
                        plugin_security_properties=params.config['configurations']['ranger-storm-security'], plugin_security_attributes=params.config['configuration_attributes']['ranger-storm-security'],
                        plugin_policymgr_ssl_properties=params.config['configurations']['ranger-storm-policymgr-ssl'], plugin_policymgr_ssl_attributes=params.config['configuration_attributes']['ranger-storm-policymgr-ssl'],
                        component_list=['storm-client', 'storm-nimbus'], audit_db_is_enabled=params.xa_audit_db_is_enabled,
                        credential_file=params.credential_file, xa_audit_db_password=params.xa_audit_db_password, 
                        ssl_truststore_password=params.ssl_truststore_password, ssl_keystore_password=params.ssl_keystore_password,
                        hdp_version_override = hdp_version)
  else:
    Logger.info('Ranger admin not installed')
