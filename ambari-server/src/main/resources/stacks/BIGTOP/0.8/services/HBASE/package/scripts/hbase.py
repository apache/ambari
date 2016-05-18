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

  Directory( params.hbase_conf_dir,
      owner = params.hbase_user,
      group = params.user_group,
      create_parents = True
  )

  Directory (params.tmp_dir,
             owner = params.hbase_user,
             create_parents = True
  )

  Directory (os.path.join(params.local_dir, "jars"),
             owner = params.hbase_user,
             group = params.user_group,
             mode=0775,
             create_parents = True
  )

  XmlConfig( "hbase-site.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['hbase-site'],
            configuration_attributes=params.config['configuration_attributes']['hbase-site'],
            owner = params.hbase_user,
            group = params.user_group
  )

  XmlConfig( "hdfs-site.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner = params.hbase_user,
            group = params.user_group
  )

  XmlConfig("hdfs-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner=params.hdfs_user,
            group=params.user_group
  )

  if 'hbase-policy' in params.config['configurations']:
    XmlConfig( "hbase-policy.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['hbase-policy'],
            configuration_attributes=params.config['configuration_attributes']['hbase-policy'],
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
       
  hbase_TemplateConfig( params.metric_prop_file_name,
    tag = 'GANGLIA-MASTER' if name == 'master' else 'GANGLIA-RS'
  )

  hbase_TemplateConfig( 'regionservers')

  if params.security_enabled:
    hbase_TemplateConfig( format("hbase_{name}_jaas.conf"))
  
  if name != "client":
    Directory( params.pid_dir,
      owner = params.hbase_user,
      create_parents = True
    )
  
    Directory (params.log_dir,
      owner = params.hbase_user,
      create_parents = True
    )

  if (params.log4j_props != None):
    File(format("{params.hbase_conf_dir}/log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.hbase_user,
         content=params.log4j_props
    )
  elif (os.path.exists(format("{params.hbase_conf_dir}/log4j.properties"))):
    File(format("{params.hbase_conf_dir}/log4j.properties"),
      mode=0644,
      group=params.user_group,
      owner=params.hbase_user
    )
  if name in ["master","regionserver"]:
    params.HdfsDirectory(params.hbase_hdfs_root_dir,
                         action="create_delayed",
                         owner=params.hbase_user
    )
    params.HdfsDirectory(params.hbase_staging_dir,
                         action="create_delayed",
                         owner=params.hbase_user,
                         mode=0711
    )
    params.HdfsDirectory(None, action="create")

def hbase_TemplateConfig(name, 
                         tag=None
                         ):
  import params

  TemplateConfig( format("{hbase_conf_dir}/{name}"),
      owner = params.hbase_user,
      template_tag = tag
  )
