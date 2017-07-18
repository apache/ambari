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

Ambari Agent

"""
import os
from resource_management import *
from resource_management.core.source import InlineTemplate, StaticFile
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def titan(type = None, upgrade_type=None):
    import params
    import params_server
    if type == 'server':
        File(format("{params.titan_server_conf_dir}/gremlin-server.yaml"),
             mode=0644,
             group=params.user_group,
             owner=params.titan_user,
             content=InlineTemplate(params.gremlin_server_configs)
             )
        credentials_file = format("{params.titan_data_dir}/credentials.kryo")
        if not os.path.isfile(credentials_file):
             File(credentials_file,
                  mode=0644,
                  group=params.user_group,
                  owner=params.titan_user,
                  content=""
                  )
        credentials_property_file = format("{params.titan_conf_dir}/tinkergraph-empty.properties")
        if not os.path.isfile(credentials_property_file):
             File(credentials_property_file,
                  mode=0644,
                  group=params.user_group,
                  owner=params.titan_user,
                  content=StaticFile("tinkergraph-empty.properties")
                  )
        Directory(params.titan_log_dir,
                  create_parents=True,
                  owner=params.titan_user,
                  group=params.user_group,
                  mode=0775
                  )
        Directory(params_server.titan_pid_dir,
                  create_parents=True,
                  owner=params.titan_user,
                  group=params.user_group,
                  mode=0775
                  )
        File(format("{params.titan_bin_dir}/gremlin-server-script.sh"),
             mode=0755,
             group='root',
             owner='root',
             content = StaticFile("gremlin-server-script.sh")
             )

    Directory(params.titan_conf_dir,
              create_parents = True,
              owner=params.titan_user,
              group=params.user_group
              )

    File(format("{params.titan_conf_dir}/titan-env.sh"),
             mode=0644,
             group=params.user_group,
             owner=params.titan_user,
             content=InlineTemplate(params.titan_env_props)
             )
    jaas_client_file = format('{titan_solr_client_jaas_file}')

    if not os.path.isfile(jaas_client_file) and params.security_enabled:
        File(jaas_client_file,
             owner   = params.titan_user,
             group   = params.user_group,
             mode    = 0644,
             content = Template('titan_solr_client_jaas.conf.j2')
             )

# SparkGraphComputer
    Directory(params.titan_conf_hadoop_graph_dir,
              create_parents = True,
              owner=params.titan_user,
              group=params.user_group
              )

    File(format("{params.titan_conf_hadoop_graph_dir}/hadoop-gryo.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.titan_user,
         content=InlineTemplate(params.titan_hadoop_gryo_props)
         )

    File(format("{params.titan_conf_hadoop_graph_dir}/hadoop-hbase-read.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.titan_user,
         content=InlineTemplate(params.hadoop_hbase_read_props)
         )

    # titan-hbase-solr_properties is always set to a default even if it's not in the payload
    File(format("{params.titan_conf_dir}/titan-hbase-solr.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.titan_user,
         content=InlineTemplate(params.titan_hbase_solr_props)
         )

    if (params.log4j_console_props != None):
        File(format("{params.titan_conf_dir}/log4j-console.properties"),
             mode=0644,
             group=params.user_group,
             owner=params.titan_user,
             content=InlineTemplate(params.log4j_console_props)
             )
    elif (os.path.exists(format("{params.titan_conf_dir}/log4j-console.properties"))):
        File(format("{params.titan_conf_dir}/log4j-console.properties"),
             mode=0644,
             group=params.user_group,
             owner=params.titan_user
             )
    # Change titan ext directory for multiple user access
    Directory(params.titan_ext_dir,
               create_parents = True,
               owner=params.titan_user,
                     group=params.user_group,
               mode=0775
               )
