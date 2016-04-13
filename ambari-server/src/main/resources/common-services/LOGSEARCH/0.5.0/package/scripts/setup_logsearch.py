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

import random
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.libraries.functions.format import format
from resource_management.core.source import InlineTemplate, Template


def setup_logsearch():
  import params

  Directory([params.logsearch_log_dir, params.logsearch_pid_dir, params.logsearch_dir, params.logsearch_server_conf],
            mode=0755,
            cd_access='a',
            owner=params.logsearch_user,
            group=params.logsearch_group,
            create_parents=True
            )

  File(params.logsearch_log,
       mode=0644,
       owner=params.logsearch_user,
       group=params.logsearch_group,
       content=''
       )

  File(format("{logsearch_server_conf}/logsearch.properties"),
       content=Template("logsearch.properties.j2"),
       owner=params.logsearch_user
       )

  File(format("{logsearch_server_conf}/log4j.xml"),
       content=InlineTemplate(params.logsearch_app_log4j_content),
       owner=params.logsearch_user
       )

  File(format("{logsearch_server_conf}/logsearch-env.sh"),
       content=InlineTemplate(params.logsearch_env_content),
       mode=0755,
       owner=params.logsearch_user
       )

  File(format("{logsearch_server_conf}/solr_configsets/hadoop_logs/conf/solrconfig.xml"),
       content=InlineTemplate(params.logsearch_service_logs_solrconfig_content),
       owner=params.logsearch_user
       )

  File(format("{logsearch_server_conf}/solr_configsets/audit_logs/conf/solrconfig.xml"),
       content=InlineTemplate(params.logsearch_audit_logs_solrconfig_content),
       owner=params.logsearch_user
       )

  random_num = random.random()

  upload_configuration_dir_to_zk('hadoop_logs', random_num)

  upload_configuration_dir_to_zk('history', random_num)

  upload_configuration_dir_to_zk('audit_logs', random_num)

  Execute(("chmod", "-R", "ugo+r", format("{logsearch_server_conf}/solr_configsets")),
          sudo=True
          )


def upload_configuration_dir_to_zk(config_set, random_num):
  import params

  tmp_config_set_folder = format('{tmp_dir}/solr_config_{config_set}_{random_num}')
  zk_cli_prefix = format(
    'export JAVA_HOME={java64_home} ; {cloud_scripts}/zkcli.sh -zkhost {zookeeper_quorum}{logsearch_solr_znode}')

  Execute(format('{zk_cli_prefix} -cmd downconfig -confdir {tmp_config_set_folder} -confname {config_set}'),
          only_if=format("{zk_cli_prefix} -cmd get /configs/{config_set}"),
          )

  Execute(format(
    '{zk_cli_prefix} -cmd upconfig -confdir {logsearch_server_conf}/solr_configsets/{config_set}/conf -confname {config_set}'),
    not_if=format("test -d {tmp_config_set_folder}"),
  )
