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

from resource_management import *


def ams(name=None):
  import params

  if name == 'collector':
    Directory(params.ams_collector_conf_dir,
              owner=params.ams_user,
              group=params.user_group,
              recursive=True
    )

    XmlConfig("ams-site.xml",
              conf_dir=params.ams_collector_conf_dir,
              configurations=params.config['configurations']['ams-site'],
              configuration_attributes=params.config['configuration_attributes']['ams-site'],
              owner=params.ams_user,
              group=params.user_group
    )

    XmlConfig( "hbase-site.xml",
               conf_dir = params.ams_collector_conf_dir,
               configurations = params.config['configurations']['ams-hbase-site'],
               configuration_attributes=params.config['configuration_attributes']['ams-hbase-site'],
               owner = params.ams_user,
               group = params.user_group
    )

    if (params.log4j_props != None):
      File(format("{params.ams_collector_conf_dir}/log4j.properties"),
           mode=0644,
           group=params.user_group,
           owner=params.ams_user,
           content=params.log4j_props
      )

    File(format("{ams_collector_conf_dir}/ams-env.sh"),
         owner=params.ams_user,
         content=InlineTemplate(params.ams_env_sh_template)
    )

    pass

  elif name == 'monitor':
    Directory(params.ams_monitor_conf_dir,
              owner=params.ams_user,
              group=params.user_group,
              recursive=True
    )

    TemplateConfig(
      format("{ams_monitor_conf_dir}/metric_monitor.ini"),
      owner=params.ams_user,
      group=params.user_group,
      template_tag=None
    )

    TemplateConfig(
      format("{ams_monitor_conf_dir}/metric_groups.conf"),
      owner=params.ams_user,
      group=params.user_group,
      template_tag=None
    )

    # TODO
    pass

  pass
