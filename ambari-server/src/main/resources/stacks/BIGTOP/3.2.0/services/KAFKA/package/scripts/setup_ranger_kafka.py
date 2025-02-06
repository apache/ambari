#!/usr/bin/env python3
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

import os

from resource_management.core.logger import Logger
from resource_management.core.resources import File, Execute, Link
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.setup_ranger_plugin_xml import (
  setup_configuration_file_for_required_plugins,
)


def setup_ranger_kafka():
  import params

  if params.enable_ranger_kafka:
    from resource_management.libraries.functions.setup_ranger_plugin_xml import (
      setup_ranger_plugin,
    )

    if params.retryAble:
      Logger.info(
        "Kafka: Setup ranger: command retry enables thus retrying if ranger admin is down !"
      )
    else:
      Logger.info(
        "Kafka: Setup ranger: command retry not enabled thus skipping if ranger admin is down !"
      )

    if params.has_namenode and params.xa_audit_hdfs_is_enabled:
      try:
        params.HdfsResource(
          "/ranger/audit",
          type="directory",
          action="create_on_execute",
          owner=params.hdfs_user,
          group=params.hdfs_user,
          mode=0o755,
          recursive_chmod=True,
        )
        params.HdfsResource(
          "/ranger/audit/kafka",
          type="directory",
          action="create_on_execute",
          owner=params.kafka_user,
          group=params.kafka_user,
          mode=0o700,
          recursive_chmod=True,
        )
        params.HdfsResource(None, action="execute")
        if params.is_ranger_kms_ssl_enabled:
          Logger.info(
            "Ranger KMS is ssl enabled, configuring ssl-client for hdfs audits."
          )
          setup_configuration_file_for_required_plugins(
            component_user=params.kafka_user,
            component_group=params.user_group,
            create_core_site_path=params.conf_dir,
            configurations=params.config["configurations"]["ssl-client"],
            configuration_attributes=params.config["configurationAttributes"][
              "ssl-client"
            ],
            file_name="ssl-client.xml",
          )
        else:
          Logger.info(
            "Ranger KMS is not ssl enabled, skipping ssl-client for hdfs audits."
          )
      except Exception as err:
        Logger.exception(
          f"Audit directory creation in HDFS for KAFKA Ranger plugin failed with error:\n{err}"
        )

    setup_ranger_plugin(
      "kafka-broker",
      params.service_name,
      params.previous_jdbc_jar,
      params.downloaded_custom_connector,
      params.driver_curl_source,
      params.driver_curl_target,
      params.java64_home,
      params.repo_name,
      params.kafka_ranger_plugin_repo,
      params.ranger_env,
      params.ranger_plugin_properties,
      params.policy_user,
      params.policymgr_mgr_url,
      params.enable_ranger_kafka,
      conf_dict=params.conf_dir,
      component_user=params.kafka_user,
      component_group=params.user_group,
      cache_service_list=["kafka"],
      plugin_audit_properties=params.ranger_kafka_audit,
      plugin_audit_attributes=params.ranger_kafka_audit_attrs,
      plugin_security_properties=params.ranger_kafka_security,
      plugin_security_attributes=params.ranger_kafka_security_attrs,
      plugin_policymgr_ssl_properties=params.ranger_kafka_policymgr_ssl,
      plugin_policymgr_ssl_attributes=params.ranger_kafka_policymgr_ssl_attrs,
      component_list=["kafka-broker"],
      audit_db_is_enabled=params.xa_audit_db_is_enabled,
      credential_file=params.credential_file,
      xa_audit_db_password=params.xa_audit_db_password,
      ssl_truststore_password=params.ssl_truststore_password,
      ssl_keystore_password=params.ssl_keystore_password,
      api_version="v2",
      skip_if_rangeradmin_down=not params.retryAble,
      is_security_enabled=params.kerberos_security_enabled,
      is_stack_supports_ranger_kerberos=params.stack_supports_ranger_kerberos,
      component_user_principal=params.kafka_jaas_principal
      if params.kerberos_security_enabled
      else None,
      component_user_keytab=params.kafka_keytab_path
      if params.kerberos_security_enabled
      else None,
      plugin_home=params.ranger_plugin_home,
    )

    if (
      params.enable_ranger_kafka
      and params.stack_supports_kafka_env_include_ranger_script
    ):
      Execute(
        (
          "cp",
          "--remove-destination",
          params.setup_ranger_env_sh_source,
          params.setup_ranger_env_sh_target,
        ),
        not_if=format("test -f {setup_ranger_env_sh_target}"),
        sudo=True,
      )
      File(
        params.setup_ranger_env_sh_target,
        owner=params.kafka_user,
        group=params.user_group,
        mode=0o755,
      )
    elif not params.stack_supports_kafka_env_include_ranger_script:
      File(format("{params.setup_ranger_env_sh_target}"), action="delete")
    if (
      params.stack_supports_core_site_for_ranger_plugin
      and params.enable_ranger_kafka
      and params.kerberos_security_enabled
    ):
      # sometimes this is a link for missing /etc/hdp directory, just remove link/file and create regular file.
      Execute(("rm", "-f", os.path.join(params.conf_dir, "core-site.xml")), sudo=True)

      if params.has_namenode and params.stack_supports_kafka_env_include_ranger_script:
        Logger.info(
          "Stack supports core-site.xml creation for Ranger plugin and Namenode is installed, creating create core-site.xml from namenode configurations"
        )
        setup_configuration_file_for_required_plugins(
          component_user=params.kafka_user,
          component_group=params.user_group,
          create_core_site_path=params.conf_dir,
          configurations=params.config["configurations"]["core-site"],
          configuration_attributes=params.config["configurationAttributes"][
            "core-site"
          ],
          file_name="core-site.xml",
          xml_include_file=params.mount_table_xml_inclusion_file_full_path,
          xml_include_file_content=params.mount_table_content,
        )
      elif (
        params.has_namenode
        and not params.stack_supports_kafka_env_include_ranger_script
      ):
        Logger.info(
          "Stack supports core-site.xml creation for Ranger plugin and create core-site and hdfs-site in the ranger-kafka-plugin-impl diretory."
        )

        Link(
          format("{ranger_kafka_plugin_impl_path}/core-site.xml"),
          only_if=os.path.islink(
            format("{ranger_kafka_plugin_impl_path}/core-site.xml")
          ),
          action="delete",
        )
        setup_configuration_file_for_required_plugins(
          component_user=params.kafka_user,
          component_group=params.user_group,
          create_core_site_path=params.ranger_kafka_plugin_impl_path,
          configurations=params.config["configurations"]["core-site"],
          configuration_attributes=params.config["configurationAttributes"][
            "core-site"
          ],
          file_name="core-site.xml",
          xml_include_file=params.mount_table_xml_inclusion_file_full_path,
          xml_include_file_content=params.mount_table_content,
        )

        Link(
          format("{ranger_kafka_plugin_impl_path}/hdfs-site.xml"),
          only_if=os.path.islink(
            format("{ranger_kafka_plugin_impl_path}/hdfs-site.xml")
          ),
          action="delete",
        )
        setup_configuration_file_for_required_plugins(
          component_user=params.kafka_user,
          component_group=params.user_group,
          create_core_site_path=params.ranger_kafka_plugin_impl_path,
          configurations=params.config["configurations"]["hdfs-site"],
          configuration_attributes=params.config["configurationAttributes"][
            "hdfs-site"
          ],
          file_name="hdfs-site.xml",
          xml_include_file=params.mount_table_xml_inclusion_file_full_path,
          xml_include_file_content=params.mount_table_content,
        )
        Link(format("{ranger_kafka_plugin_impl_path}/conf"), to=format("{conf_dir}"))
      else:
        Logger.info(
          "Stack supports core-site.xml creation for Ranger plugin and Namenode is not installed, creating create core-site.xml from default configurations"
        )
        setup_configuration_file_for_required_plugins(
          component_user=params.kafka_user,
          component_group=params.user_group,
          create_core_site_path=params.conf_dir,
          configurations={
            "hadoop.security.authentication": "kerberos"
            if params.kerberos_security_enabled
            else "simple"
          },
          configuration_attributes={},
          file_name="core-site.xml",
        )
        Link(format("{ranger_kafka_plugin_impl_path}/conf"), to=format("{conf_dir}"))
    else:
      Logger.info(
        "Stack does not support core-site.xml creation for Ranger plugin, skipping core-site.xml configurations"
      )
  else:
    Logger.info("Ranger Kafka plugin is not enabled")
