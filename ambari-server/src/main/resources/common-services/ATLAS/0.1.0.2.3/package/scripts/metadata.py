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
import random
from resource_management import Directory, Fail, Logger, File, \
    InlineTemplate, PropertiesFile, StaticFile, XmlConfig
from resource_management.libraries.functions import format
from resource_management.libraries.resources.template_config import TemplateConfig
from resource_management.libraries.functions import solr_cloud_util


def metadata(type='server'):
    import params

    Directory([params.pid_dir],
              mode=0755,
              cd_access='a',
              owner=params.metadata_user,
              group=params.user_group,
              create_parents = True
    )

    Directory(params.conf_dir,
              mode=0755,
              cd_access='a',
              owner=params.metadata_user,
              group=params.user_group,
              create_parents = True
    )

    Directory(params.log_dir,
              mode=0755,
              cd_access='a',
              owner=params.metadata_user,
              group=params.user_group,
              create_parents = True
    )

    Directory(params.data_dir,
              mode=0644,
              cd_access='a',
              owner=params.metadata_user,
              group=params.user_group,
              create_parents = True
    )

    Directory(params.expanded_war_dir,
              mode=0644,
              cd_access='a',
              owner=params.metadata_user,
              group=params.user_group,
              create_parents = True
    )

    File(format("{expanded_war_dir}/atlas.war"),
         content = StaticFile(format('{metadata_home}/server/webapp/atlas.war'))
    )

    PropertiesFile(format('{conf_dir}/{conf_file}'),
         properties = params.application_properties,
         mode=0644,
         owner=params.metadata_user,
         group=params.user_group
    )

    File(format("{conf_dir}/atlas-env.sh"),
         owner=params.metadata_user,
         group=params.user_group,
         mode=0755,
         content=InlineTemplate(params.metadata_env_content)
    )

    File(format("{conf_dir}/atlas-log4j.xml"),
         mode=0644,
         owner=params.metadata_user,
         group=params.user_group,
         content=InlineTemplate(params.metadata_log4j_content)
    )

    File(format("{conf_dir}/users-credentials.properties"),
         mode=0644,
         owner=params.metadata_user,
         group=params.user_group,
         content=StaticFile('users-credentials.properties')
    )

    File(format("{conf_dir}/policy-store.txt"),
         mode=0644,
         owner=params.metadata_user,
         group=params.user_group,
         content=StaticFile('policy-store.txt')
    )

    if type == 'server':
      random_num = random.random()

      upload_conf_set('basic_configs', random_num)

      create_collection('vertex_index', 'basic_configs')
      create_collection('edge_index', 'basic_configs')
      create_collection('fulltext_index', 'basic_configs')

    if params.security_enabled:
        TemplateConfig(format(params.atlas_jaas_file),
                         owner=params.metadata_user)

def upload_conf_set(config_set, random_num):
  import params
  tmp_config_set_folder = format('{tmp_dir}/solr_config_{config_set}_{random_num}')

  solr_cloud_util.upload_configuration_to_zk(
      zookeeper_quorum=params.zookeeper_quorum,
      solr_znode=params.logsearch_solr_znode,
      config_set_dir=format("{logsearch_solr_dir}/server/solr/configsets/{config_set}/conf"),
      config_set=config_set,
      tmp_config_set_dir=tmp_config_set_folder,
      java64_home=params.java64_home,
      user=params.metadata_user,
      group=params.user_group)

def create_collection(collection, config_set):
  import params

  solr_cloud_util.create_collection(
      zookeeper_quorum=params.zookeeper_quorum,
      solr_znode=params.logsearch_solr_znode,
      collection = collection,
      config_set=config_set,
      java64_home=params.java64_home,
      user=params.metadata_user,
      group=params.user_group,
      shards=params.atlas_solr_shards,
      replication_factor = params.logsearch_solr_replication_factor)
