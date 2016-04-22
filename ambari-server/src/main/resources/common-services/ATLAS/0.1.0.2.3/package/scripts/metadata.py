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
from resource_management import Directory, Fail, Logger, File, \
    InlineTemplate, PropertiesFile, StaticFile, XmlConfig
from resource_management.libraries.functions import format
from resource_management.libraries.resources.template_config import TemplateConfig


def metadata():
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

    Directory(params.atlas_hbase_log_dir,
              mode=0755,
              cd_access='a',
              owner=params.metadata_user,
              group=params.user_group,
              create_parents = True
              )

    Directory(params.atlas_hbase_data_dir,
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

    # hbase-site for embedded hbase used by Atlas
    XmlConfig( "hbase-site.xml",
           conf_dir = params.atlas_hbase_conf_dir,
           configurations = params.config['configurations']['atlas-hbase-site'],
           configuration_attributes=params.config['configuration_attributes']['atlas-hbase-site'],
           owner = params.metadata_user,
           group = params.user_group
           )

    if params.security_enabled:
        TemplateConfig(format(params.atlas_jaas_file),
                         owner=params.metadata_user)
