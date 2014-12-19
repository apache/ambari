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
import os

from resource_management import *
import sys

def hbase(name=None # 'master' or 'regionserver' or 'client'
              ):
  import params

  Directory(params.hbase_conf_dir,
      owner = params.hbase_user,
      group = params.user_group,
      recursive = True
  )

  Directory (params.hbase_tmp_dir,
             owner = params.hbase_user,
             recursive = True
  )

  Directory (os.path.join(params.local_dir, "jars"),
             owner = params.hbase_user,
             group = params.user_group,
             mode=0775,
             recursive = True
  )

  XmlConfig("hbase-site.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['ams-hbase-site'],
            configuration_attributes=params.config['configuration_attributes']['ams-hbase-site'],
            owner = params.hbase_user,
            group = params.user_group
  )

  if 'ams-hbase-policy' in params.config['configurations']:
    XmlConfig("hbase-policy.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['ams-hbase-policy'],
            configuration_attributes=params.config['configuration_attributes']['ams-hbase-policy'],
            owner = params.hbase_user,
            group = params.user_group
    )
  # Manually overriding ownership of file installed by hadoop package
  else: 
    File( format("{params.hbase_conf_dir}/hbase-policy.xml"),
      owner = params.hbase_user,
      group = params.user_group
    )

  File(format("{hbase_conf_dir}/hbase-env.sh"),
       owner = params.hbase_user,
       content=InlineTemplate(params.hbase_env_sh_template)
  )

  # Metrics properties
  File(os.path.join(params.hbase_conf_dir, "hadoop-metrics2-hbase.properties"),
         owner = params.hbase_user,
         group = params.user_group,
         content=Template("hadoop-metrics2-hbase.properties.j2")
    )
       
  # hbase_TemplateConfig( params.metric_prop_file_name,
  #   tag = 'GANGLIA-MASTER' if name == 'master' else 'GANGLIA-RS'
  # )

  hbase_TemplateConfig('regionservers')

  if params.security_enabled:
    hbase_TemplateConfig( format("hbase_{name}_jaas.conf"))
  
  if name != "client":
    Directory( params.hbase_pid_dir,
      owner = params.hbase_user,
      recursive = True
    )
  
    Directory (params.hbase_log_dir,
      owner = params.hbase_user,
      recursive = True
    )

  if (params.hbase_log4j_props != None):
    File(format("{params.hbase_conf_dir}/log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.hbase_user,
         content=params.hbase_log4j_props
    )
  elif (os.path.exists(format("{params.hbase_conf_dir}/log4j.properties"))):
    File(format("{params.hbase_conf_dir}/log4j.properties"),
      mode=0644,
      group=params.user_group,
      owner=params.hbase_user
    )


def hbase_TemplateConfig(name, tag=None):
  import params

  TemplateConfig( format("{hbase_conf_dir}/{name}"),
      owner = params.hbase_user,
      template_tag = tag
  )
