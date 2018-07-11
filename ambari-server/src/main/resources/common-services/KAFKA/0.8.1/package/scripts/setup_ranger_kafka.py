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
from resource_management.core.resources import File, Execute
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.setup_ranger_plugin_xml import setup_configuration_file_for_required_plugins

def setup_ranger_kafka():
  import params

  if params.enable_ranger_kafka:

    from resource_management.libraries.functions.setup_ranger_plugin_xml import setup_ranger_plugin

    if params.retryAble:
      Logger.info("Kafka: Setup ranger: command retry enables thus retrying if ranger admin is down !")
    else:
      Logger.info("Kafka: Setup ranger: command retry not enabled thus skipping if ranger admin is down !")

    if params.xml_configurations_supported and params.enable_ranger_kafka and params.xa_audit_hdfs_is_enabled:
      if params.has_namenode:
        params.HdfsResource("/ranger/audit",
                           type="directory",
                           action="create_on_execute",
                           owner=params.hdfs_user,
                           group=params.hdfs_user,
                           mode=0755,
                           recursive_chmod=True
        )
        params.HdfsResource("/ranger/audit/kafka",
                           type="directory",
                           action="create_on_execute",
                           owner=params.kafka_user,
                           group=params.kafka_user,
                           mode=0700,
                           recursive_chmod=True
        )
        params.HdfsResource(None, action="execute")

    setup_ranger_plugin('kafka-broker', 'kafka', params.previous_jdbc_jar,
                        params.downloaded_custom_connector, params.driver_curl_source,
                        params.driver_curl_target, params.java64_home,
                        params.repo_name, params.kafka_ranger_plugin_repo,
                        params.ranger_env, params.ranger_plugin_properties,
                        params.policy_user, params.policymgr_mgr_url,
                        params.enable_ranger_kafka, conf_dict=params.conf_dir,
                        component_user=params.kafka_user, component_group=params.user_group, cache_service_list=['kafka'],
                        plugin_audit_properties=params.ranger_kafka_audit, plugin_audit_attributes=params.ranger_kafka_audit_attrs,
                        plugin_security_properties=params.ranger_kafka_security, plugin_security_attributes=params.ranger_kafka_security_attrs,
                        plugin_policymgr_ssl_properties=params.ranger_kafka_policymgr_ssl, plugin_policymgr_ssl_attributes=params.ranger_kafka_policymgr_ssl_attrs,
                        component_list=['kafka-broker'], audit_db_is_enabled=params.xa_audit_db_is_enabled,
                        credential_file=params.credential_file, xa_audit_db_password=params.xa_audit_db_password, 
                        ssl_truststore_password=params.ssl_truststore_password, ssl_keystore_password=params.ssl_keystore_password,
                        api_version = 'v2', skip_if_rangeradmin_down= not params.retryAble,
                        is_security_enabled = params.kerberos_security_enabled,
                        is_stack_supports_ranger_kerberos = params.stack_supports_ranger_kerberos,
                        component_user_principal=params.kafka_jaas_principal if params.kerberos_security_enabled else None,
                        component_user_keytab=params.kafka_keytab_path if params.kerberos_security_enabled else None)
    
    if params.enable_ranger_kafka: 
      Execute(('cp', '--remove-destination', params.setup_ranger_env_sh_source, params.setup_ranger_env_sh_target),
        not_if=format("test -f {setup_ranger_env_sh_target}"),
        sudo=True
      )
      File(params.setup_ranger_env_sh_target,
        owner = params.kafka_user,
        group = params.user_group,
        mode = 0755
      )
    if params.stack_supports_core_site_for_ranger_plugin and params.enable_ranger_kafka and params.kerberos_security_enabled:
      if params.has_namenode:
        Logger.info("Stack supports core-site.xml creation for Ranger plugin and Namenode is installed, creating create core-site.xml from namenode configurations")
        setup_configuration_file_for_required_plugins(component_user = params.kafka_user, component_group = params.user_group,
                                             create_core_site_path = params.conf_dir, configurations = params.config['configurations']['core-site'],
                                             configuration_attributes = params.config['configurationAttributes']['core-site'], file_name='core-site.xml')
      else:
        Logger.info("Stack supports core-site.xml creation for Ranger plugin and Namenode is not installed, creating create core-site.xml from default configurations")
        setup_configuration_file_for_required_plugins(component_user = params.kafka_user, component_group = params.user_group,
                                             create_core_site_path = params.conf_dir, configurations = { 'hadoop.security.authentication' : 'kerberos' if params.kerberos_security_enabled else 'simple' },
                                             configuration_attributes = {}, file_name='core-site.xml')

    else:
      Logger.info("Stack does not support core-site.xml creation for Ranger plugin, skipping core-site.xml configurations")
  else:
    Logger.info('Ranger Kafka plugin is not enabled')
