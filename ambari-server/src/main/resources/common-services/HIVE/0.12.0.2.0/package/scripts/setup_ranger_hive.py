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
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.ranger_functions_v2 import RangeradminV2
from resource_management.libraries.functions.setup_ranger_plugin_xml import generate_ranger_service_config

def setup_ranger_hive(upgrade_type = None):
  import params

  if params.enable_ranger_hive:

    stack_version = None

    if upgrade_type is not None:
      stack_version = params.version

    if params.retryAble:
      Logger.info("Hive: Setup ranger: command retry enables thus retrying if ranger admin is down !")
    else:
      Logger.info("Hive: Setup ranger: command retry not enabled thus skipping if ranger admin is down !")

    if params.xml_configurations_supported and params.xa_audit_hdfs_is_enabled:
      params.HdfsResource("/ranger/audit",
                         type="directory",
                         action="create_on_execute",
                         owner=params.hdfs_user,
                         group=params.hdfs_user,
                         mode=0o755,
                         recursive_chmod=True
      )
      params.HdfsResource("/ranger/audit/hiveServer2",
                         type="directory",
                         action="create_on_execute",
                         owner=params.hive_user,
                         group=params.hive_user,
                         mode=0o700,
                         recursive_chmod=True
      )
      params.HdfsResource(None, action="execute")

    if params.xml_configurations_supported:
      api_version=None
      if params.stack_supports_ranger_kerberos:
        api_version='v2'
      from resource_management.libraries.functions.setup_ranger_plugin_xml import setup_ranger_plugin
      setup_ranger_plugin('hive-server2', 'hive', params.ranger_previous_jdbc_jar,
                          params.ranger_downloaded_custom_connector, params.ranger_driver_curl_source,
                          params.ranger_driver_curl_target, params.java64_home,
                          params.repo_name, params.hive_ranger_plugin_repo,
                          params.ranger_env, params.ranger_plugin_properties,
                          params.policy_user, params.policymgr_mgr_url,
                          params.enable_ranger_hive, conf_dict=params.hive_server_conf_dir,
                          component_user=params.hive_user, component_group=params.user_group, cache_service_list=['hiveServer2'],
                          plugin_audit_properties=params.config['configurations']['ranger-hive-audit'], plugin_audit_attributes=params.config['configurationAttributes']['ranger-hive-audit'],
                          plugin_security_properties=params.config['configurations']['ranger-hive-security'], plugin_security_attributes=params.config['configurationAttributes']['ranger-hive-security'],
                          plugin_policymgr_ssl_properties=params.config['configurations']['ranger-hive-policymgr-ssl'], plugin_policymgr_ssl_attributes=params.config['configurationAttributes']['ranger-hive-policymgr-ssl'],
                          component_list=['hive-client', 'hive-metastore', 'hive-server2'], audit_db_is_enabled=params.xa_audit_db_is_enabled,
                          credential_file=params.credential_file, xa_audit_db_password=params.xa_audit_db_password,
                          ssl_truststore_password=params.ssl_truststore_password, ssl_keystore_password=params.ssl_keystore_password,
                          stack_version_override = stack_version, skip_if_rangeradmin_down= not params.retryAble, api_version=api_version,
                          is_security_enabled = params.security_enabled,
                          is_stack_supports_ranger_kerberos = params.stack_supports_ranger_kerberos,
                          component_user_principal=params.hive_principal if params.security_enabled else None,
                          component_user_keytab=params.hive_server2_keytab if params.security_enabled else None)
    else:
      from resource_management.libraries.functions.setup_ranger_plugin import setup_ranger_plugin
      setup_ranger_plugin('hive-server2', 'hive', params.ranger_previous_jdbc_jar,
                        params.ranger_downloaded_custom_connector, params.ranger_driver_curl_source,
                        params.ranger_driver_curl_target, params.java64_home,
                        params.repo_name, params.hive_ranger_plugin_repo,
                        params.ranger_env, params.ranger_plugin_properties,
                        params.policy_user, params.policymgr_mgr_url,
                        params.enable_ranger_hive, conf_dict=params.hive_server_conf_dir,
                        component_user=params.hive_user, component_group=params.user_group, cache_service_list=['hiveServer2'],
                        plugin_audit_properties=params.config['configurations']['ranger-hive-audit'], plugin_audit_attributes=params.config['configurationAttributes']['ranger-hive-audit'],
                        plugin_security_properties=params.config['configurations']['ranger-hive-security'], plugin_security_attributes=params.config['configurationAttributes']['ranger-hive-security'],
                        plugin_policymgr_ssl_properties=params.config['configurations']['ranger-hive-policymgr-ssl'], plugin_policymgr_ssl_attributes=params.config['configurationAttributes']['ranger-hive-policymgr-ssl'],
                        component_list=['hive-client', 'hive-metastore', 'hive-server2'], audit_db_is_enabled=params.xa_audit_db_is_enabled,
                        credential_file=params.credential_file, xa_audit_db_password=params.xa_audit_db_password,
                        ssl_truststore_password=params.ssl_truststore_password, ssl_keystore_password=params.ssl_keystore_password,
                        stack_version_override = stack_version, skip_if_rangeradmin_down= not params.retryAble)
  else:
    Logger.info('Ranger Hive plugin is not enabled')

def setup_ranger_hive_metastore_service():
  """
  Creates ranger hive service in ranger admin installed in same cluster for cluster depolyed in cloud env.
  """
  import params

  if params.has_ranger_admin and params.ranger_hive_metastore_lookup:

    repo_name = str(params.config['clusterName']) + '_hive'
    repo_name_value = params.config['configurations']['ranger-hive-security']['ranger.plugin.hive.service.name']
    if not is_empty(repo_name_value) and repo_name_value != "{{repo_name}}":
      repo_name = repo_name_value

    hive_ranger_plugin_config = {
      'username': params.config['configurations']['ranger-hive-plugin-properties']['REPOSITORY_CONFIG_USERNAME'],
      'password': params.config['configurations']['ranger-hive-plugin-properties']['REPOSITORY_CONFIG_PASSWORD'],
      'jdbc.driverClassName': params.config['configurations']['ranger-hive-plugin-properties']['jdbc.driverClassName'],
      'jdbc.url': 'none',
      'commonNameForCertificate': params.config['configurations']['ranger-hive-plugin-properties']['common.name.for.certificate'],
      'ambari.service.check.user': params.config['configurations']['ranger-hive-plugin-properties']['policy_user']
    }

    if params.security_enabled:
      hive_ranger_plugin_config['policy.download.auth.users'] = params.hive_user
      hive_ranger_plugin_config['tag.download.auth.users'] = params.hive_user
      hive_ranger_plugin_config['policy.grantrevoke.auth.users'] = params.hive_user

    custom_ranger_service_config = generate_ranger_service_config(params.config['configurations']['ranger-hive-plugin-properties'])
    if len(custom_ranger_service_config) > 0:
      hive_ranger_plugin_config.update(custom_ranger_service_config)

    hive_ranger_plugin_repo = {
      'isEnabled': 'true',
      'configs': hive_ranger_plugin_config,
      'description': 'Hive service',
      'name': repo_name,
      'type': 'hive'
    }

    ranger_admin_obj = RangeradminV2(url = params.config['configurations']['ranger-hive-security']['ranger.plugin.hive.policy.rest.url'], skip_if_rangeradmin_down = not params.retryAble)
    ranger_admin_obj.create_ranger_repository(
      component = 'hive',
      repo_name = repo_name,
      repo_properties = hive_ranger_plugin_repo,
      ambari_ranger_admin = params.config['configurations']['ranger-env']['ranger_admin_username'],
      ambari_ranger_password = params.config['configurations']['ranger-env']['ranger_admin_password'],
      admin_uname = params.config['configurations']['ranger-env']['admin_username'],
      admin_password = params.config['configurations']['ranger-env']['admin_password'],
      policy_user = params.config['configurations']['ranger-hive-plugin-properties']['policy_user'],
      is_security_enabled = params.security_enabled,
      is_stack_supports_ranger_kerberos = params.stack_supports_ranger_kerberos,
      component_user = params.hive_user,
      component_user_principal = params.hive_metastore_principal_with_host if params.security_enabled else None,
      component_user_keytab = params.hive_metastore_keytab_path if params.security_enabled else None)
