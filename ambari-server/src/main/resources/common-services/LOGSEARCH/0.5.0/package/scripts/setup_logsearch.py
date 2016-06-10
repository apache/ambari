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
from resource_management.libraries.functions import solr_cloud_util
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.libraries.functions.format import format
from resource_management.core.source import InlineTemplate, Template


def setup_logsearch():
  import params

  Directory([params.logsearch_log_dir, params.logsearch_pid_dir, params.logsearch_dir,
             params.logsearch_server_conf, params.logsearch_config_set_dir],
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

  File(format("{logsearch_server_conf}/logsearch-admin.json"),
       content=InlineTemplate(params.logsearch_admin_content),
       owner=params.logsearch_user
       )

  File(format("{logsearch_config_set_dir}/hadoop_logs/conf/solrconfig.xml"),
       content=InlineTemplate(params.logsearch_service_logs_solrconfig_content),
       owner=params.logsearch_user
       )

  File(format("{logsearch_config_set_dir}/audit_logs/conf/solrconfig.xml"),
       content=InlineTemplate(params.logsearch_audit_logs_solrconfig_content),
       owner=params.logsearch_user
       )

  random_num = random.random()

  upload_conf_set(format('{logsearch_solr_collection_service_logs}'), random_num)

  upload_conf_set('history', random_num)

  upload_conf_set(format('{logsearch_solr_collection_audit_logs}'), random_num)

  Execute(("chmod", "-R", "ugo+r", format("{logsearch_server_conf}/solr_configsets")),
          sudo=True
          )


def upload_conf_set(config_set, random_num):
  import params
  tmp_config_set_folder = format('{tmp_dir}/solr_config_{config_set}_{random_num}')

  solr_cloud_util.upload_configuration_to_zk(
    zookeeper_quorum=params.zookeeper_quorum,
    solr_znode=params.logsearch_solr_znode,
    config_set_dir=format("{logsearch_server_conf}/solr_configsets/{config_set}/conf"),
    config_set=config_set,
    tmp_config_set_dir=tmp_config_set_folder,
    java64_home=params.java64_home,
    user=params.logsearch_solr_user)
