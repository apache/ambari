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
from resource_management.libraries.functions.setup_ranger_plugin_xml import setup_configuration_file_for_required_plugins
from resource_management.core.resources import File
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.format import format

def setup_ranger_knox(upgrade_type=None):
  import params

  if params.enable_ranger_knox:

    stack_version = None
    if upgrade_type is not None:
      stack_version = params.version

    if params.retryAble:
      Logger.info("Knox: Setup ranger: command retry enables thus retrying if ranger admin is down !")
    else:
      Logger.info("Knox: Setup ranger: command retry not enabled thus skipping if ranger admin is down !")

    if params.xml_configurations_supported and params.enable_ranger_knox and params.xa_audit_hdfs_is_enabled:
      if params.has_namenode:
        params.HdfsResource("/ranger/audit",
                           type="directory",
                           action="create_on_execute",
                           owner=params.hdfs_user,
                           group=params.hdfs_user,
                           mode=0o755,
                           recursive_chmod=True
        )
        params.HdfsResource("/ranger/audit/knox",
                           type="directory",
                           action="create_on_execute",
                           owner=params.knox_user,
                           group=params.knox_user,
                           mode=0o700,
                           recursive_chmod=True
        )
        params.HdfsResource(None, action="execute")

        if params.namenode_hosts is not None and len(params.namenode_hosts) > 1:
          Logger.info('Ranger Knox plugin is enabled in NameNode HA environment along with audit to Hdfs enabled, creating hdfs-site.xml')
          XmlConfig("hdfs-site.xml",
            conf_dir=params.knox_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configurationAttributes']['hdfs-site'],
            owner=params.knox_user,
            group=params.knox_group,
            mode=0o644
          )
        else:
          File(format('{knox_conf_dir}/hdfs-site.xml'), action="delete")

    if params.xml_configurations_supported:
      api_version=None
      if params.stack_supports_ranger_kerberos:
        api_version='v2'
      from resource_management.libraries.functions.setup_ranger_plugin_xml import setup_ranger_plugin
      setup_ranger_plugin('knox-server', 'knox', params.previous_jdbc_jar,
                          params.downloaded_custom_connector, params.driver_curl_source,
                          params.driver_curl_target, params.java_home,
                          params.repo_name, params.knox_ranger_plugin_repo,
                          params.ranger_env, params.ranger_plugin_properties,
                          params.policy_user, params.policymgr_mgr_url,
                          params.enable_ranger_knox, conf_dict=params.knox_conf_dir,
                          component_user=params.knox_user, component_group=params.knox_group, cache_service_list=['knox'],
                          plugin_audit_properties=params.config['configurations']['ranger-knox-audit'], plugin_audit_attributes=params.config['configurationAttributes']['ranger-knox-audit'],
                          plugin_security_properties=params.config['configurations']['ranger-knox-security'], plugin_security_attributes=params.config['configurationAttributes']['ranger-knox-security'],
                          plugin_policymgr_ssl_properties=params.config['configurations']['ranger-knox-policymgr-ssl'], plugin_policymgr_ssl_attributes=params.config['configurationAttributes']['ranger-knox-policymgr-ssl'],
                          component_list=['knox-server'], audit_db_is_enabled=params.xa_audit_db_is_enabled,
                          credential_file=params.credential_file, xa_audit_db_password=params.xa_audit_db_password,
                          ssl_truststore_password=params.ssl_truststore_password, ssl_keystore_password=params.ssl_keystore_password,
                          stack_version_override = stack_version, skip_if_rangeradmin_down= not params.retryAble,api_version=api_version,
                          is_security_enabled = params.security_enabled,
                          is_stack_supports_ranger_kerberos = params.stack_supports_ranger_kerberos,
                          component_user_principal=params.knox_principal_name if params.security_enabled else None,
                          component_user_keytab=params.knox_keytab_path if params.security_enabled else None)
    else:
      from resource_management.libraries.functions.setup_ranger_plugin import setup_ranger_plugin
      setup_ranger_plugin('knox-server', 'knox', params.previous_jdbc_jar,
                        params.downloaded_custom_connector, params.driver_curl_source,
                        params.driver_curl_target, params.java_home,
                        params.repo_name, params.knox_ranger_plugin_repo,
                        params.ranger_env, params.ranger_plugin_properties,
                        params.policy_user, params.policymgr_mgr_url,
                        params.enable_ranger_knox, conf_dict=params.knox_conf_dir,
                        component_user=params.knox_user, component_group=params.knox_group, cache_service_list=['knox'],
                        plugin_audit_properties=params.config['configurations']['ranger-knox-audit'], plugin_audit_attributes=params.config['configurationAttributes']['ranger-knox-audit'],
                        plugin_security_properties=params.config['configurations']['ranger-knox-security'], plugin_security_attributes=params.config['configurationAttributes']['ranger-knox-security'],
                        plugin_policymgr_ssl_properties=params.config['configurations']['ranger-knox-policymgr-ssl'], plugin_policymgr_ssl_attributes=params.config['configurationAttributes']['ranger-knox-policymgr-ssl'],
                        component_list=['knox-server'], audit_db_is_enabled=params.xa_audit_db_is_enabled,
                        credential_file=params.credential_file, xa_audit_db_password=params.xa_audit_db_password,
                        ssl_truststore_password=params.ssl_truststore_password, ssl_keystore_password=params.ssl_keystore_password,
                        stack_version_override = stack_version, skip_if_rangeradmin_down= not params.retryAble)
    if params.stack_supports_core_site_for_ranger_plugin and params.enable_ranger_knox and params.security_enabled:
      if params.has_namenode:
        Logger.info("Stack supports core-site.xml creation for Ranger plugin and Namenode is installed, creating create core-site.xml from namenode configurations")
        setup_configuration_file_for_required_plugins(component_user = params.knox_user, component_group = params.knox_group,
                                             create_core_site_path = params.knox_conf_dir, configurations = params.config['configurations']['core-site'],
                                             configuration_attributes = params.config['configurationAttributes']['core-site'], file_name = 'core-site.xml')
      else:
        Logger.info("Stack supports core-site.xml creation for Ranger plugin and Namenode is not installed, creating create core-site.xml from default configurations")
        setup_configuration_file_for_required_plugins(component_user = params.knox_user, component_group = params.knox_group,
                                             create_core_site_path = params.knox_conf_dir, configurations = { 'hadoop.security.authentication' : 'kerberos' if params.security_enabled else 'simple' },
                                             configuration_attributes = {}, file_name = 'core-site.xml')
    else:
      Logger.info("Stack does not support core-site.xml creation for Ranger plugin, skipping core-site.xml configurations")

  else:
    Logger.info('Ranger Knox plugin is not enabled')
