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

from resource_management.core.resources.system import Directory, Execute, File
from resource_management.libraries.functions.format import format
from resource_management.core.source import InlineTemplate, Template
from resource_management.libraries.resources.properties_file import PropertiesFile


def setup_logsearch():
  import params

  Directory([params.logsearch_log_dir, params.logsearch_pid_dir],
            mode=0755,
            cd_access='a',
            owner=params.logsearch_user,
            group=params.user_group,
            create_parents=True
            )

  Directory([params.logsearch_dir, params.logsearch_server_conf, params.logsearch_config_set_dir],
            mode=0755,
            cd_access='a',
            owner=params.logsearch_user,
            group=params.user_group,
            create_parents=True,
            recursive_ownership=True
            )

  Directory(params.logsearch_server_keys_folder,
            cd_access='a',
            mode=0755,
            owner=params.logsearch_user,
            group=params.user_group)

  File(format("{logsearch_server_keys_folder}/ks_pass.txt"),
       content=params.logsearch_keystore_password,
       mode=0600,
       owner=params.logsearch_user,
       group=params.user_group
       )

  File(format("{logsearch_server_keys_folder}/ts_pass.txt"),
       content=params.logsearch_truststore_password,
       mode=0600,
       owner=params.logsearch_user,
       group=params.user_group
       )

  File(params.logsearch_log,
       mode=0644,
       owner=params.logsearch_user,
       group=params.user_group,
       content=''
       )

  PropertiesFile(format("{logsearch_server_conf}/logsearch.properties"),
                 properties=params.logsearch_properties
                 )

  File(format("{logsearch_server_conf}/HadoopServiceConfig.json"),
       content=Template("HadoopServiceConfig.json.j2"),
       owner=params.logsearch_user,
       group=params.user_group
       )

  File(format("{logsearch_server_conf}/log4j.xml"),
       content=InlineTemplate(params.logsearch_app_log4j_content),
       owner=params.logsearch_user,
       group=params.user_group
       )

  File(format("{logsearch_server_conf}/logsearch-env.sh"),
       content=InlineTemplate(params.logsearch_env_content),
       mode=0755,
       owner=params.logsearch_user,
       group=params.user_group
       )

  File(format("{logsearch_server_conf}/logsearch-admin.json"),
       content=InlineTemplate(params.logsearch_admin_content),
       owner=params.logsearch_user,
       group=params.user_group
       )

  File(format("{logsearch_config_set_dir}/hadoop_logs/conf/solrconfig.xml"),
       content=InlineTemplate(params.logsearch_service_logs_solrconfig_content),
       owner=params.logsearch_user,
       group=params.user_group
       )

  File(format("{logsearch_config_set_dir}/audit_logs/conf/solrconfig.xml"),
       content=InlineTemplate(params.logsearch_audit_logs_solrconfig_content),
       owner=params.logsearch_user,
       group=params.user_group
       )

  if params.security_enabled:
    File(format("{logsearch_jaas_file}"),
         content=Template("logsearch_jaas.conf.j2"),
         owner=params.logsearch_user
         )

  Execute(("chmod", "-R", "ugo+r", format("{logsearch_server_conf}/solr_configsets")),
          sudo=True
          )
