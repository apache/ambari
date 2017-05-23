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

def setup_ranger_atlas(upgrade_type=None):
  import params

  if params.enable_ranger_atlas:

    from resource_management.libraries.functions.setup_ranger_plugin_xml import setup_ranger_plugin

    if params.retry_enabled:
      Logger.info("ATLAS: Setup ranger: command retry enables thus retrying if ranger admin is down !")
    else:
      Logger.info("ATLAS: Setup ranger: command retry not enabled thus skipping if ranger admin is down !")

    if params.enable_ranger_atlas and params.xa_audit_hdfs_is_enabled:
      if params.has_namenode:
        params.HdfsResource("/ranger/audit",
                            type="directory",
                            action="create_on_execute",
                            owner=params.metadata_user,
                            group=params.user_group,
                            mode=0755,
                            recursive_chmod=True
        )
        params.HdfsResource("/ranger/audit/atlas",
                            type="directory",
                            action="create_on_execute",
                            owner=params.metadata_user,
                            group=params.user_group,
                            mode=0700,
                            recursive_chmod=True
        )
        params.HdfsResource(None, action="execute")

    setup_ranger_plugin('atlas-server', 'atlas',None,
                        params.downloaded_custom_connector, params.driver_curl_source,
                        params.driver_curl_target, params.java64_home,
                        params.repo_name, params.atlas_ranger_plugin_repo,
                        params.ranger_env, params.ranger_plugin_properties,
                        params.policy_user, params.policymgr_mgr_url,
                        params.enable_ranger_atlas, conf_dict=params.conf_dir,
                        component_user=params.metadata_user, component_group=params.user_group, cache_service_list=['atlas'],
                        plugin_audit_properties=params.config['configurations']['ranger-atlas-audit'], plugin_audit_attributes=params.config['configuration_attributes']['ranger-atlas-audit'],
                        plugin_security_properties=params.config['configurations']['ranger-atlas-security'], plugin_security_attributes=params.config['configuration_attributes']['ranger-atlas-security'],
                        plugin_policymgr_ssl_properties=params.config['configurations']['ranger-atlas-policymgr-ssl'], plugin_policymgr_ssl_attributes=params.config['configuration_attributes']['ranger-atlas-policymgr-ssl'],
                        component_list=['atlas-server'], audit_db_is_enabled=False,
                        credential_file=params.credential_file, xa_audit_db_password=None,
                        ssl_truststore_password=params.ssl_truststore_password, ssl_keystore_password=params.ssl_keystore_password,
                        api_version = 'v2', skip_if_rangeradmin_down = not params.retry_enabled, is_security_enabled = params.security_enabled,
                        is_stack_supports_ranger_kerberos = params.stack_supports_ranger_kerberos,
                        component_user_principal=params.atlas_jaas_principal if params.security_enabled else None,
                        component_user_keytab=params.atlas_keytab_path if params.security_enabled else None)
  else:
    Logger.info('Ranger Atlas plugin is not enabled')