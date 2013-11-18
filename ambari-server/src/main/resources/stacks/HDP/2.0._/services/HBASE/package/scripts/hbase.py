#!/usr/bin/env python2.6
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

from resource_management import *
import sys

def hbase(type=None # 'master' or 'regionserver' or 'client'
              ):
  import params
  
  Directory( params.conf_dir,
      owner = params.hbase_user,
      group = params.user_group,
      recursive = True
  )
  
  XmlConfig( "hbase-site.xml",
            conf_dir = params.conf_dir,
            configurations = params.config['configurations']['hbase-site'],
            owner = params.hbase_user,
            group = params.user_group
  )

  XmlConfig( "hdfs-site.xml",
            conf_dir = params.conf_dir,
            configurations = params.config['configurations']['hdfs-site'],
            owner = params.hbase_user,
            group = params.user_group
  )
  
  if 'hbase-policy' in params.config['configurations']:
    XmlConfig( "hbase-policy.xml",
      configurations = params.config['configurations']['hbase-policy'],
      owner = params.hbase_user,
      group = params.user_group
    )
  # Manually overriding ownership of file installed by hadoop package
  else: 
    File( format("{conf_dir}/hbase-policy.xml"),
      owner = params.hbase_user,
      group = params.user_group
    )
  
  hbase_TemplateConfig( 'hbase-env.sh')     
       
  hbase_TemplateConfig( params.metric_prop_file_name,
    tag = 'GANGLIA-MASTER' if type == 'master' else 'GANGLIA-RS'
  )

  hbase_TemplateConfig( 'regionservers')

  if params.security_enabled:
    hbase_TemplateConfig( format("hbase_{type}_jaas.conf"))
  
  if type != "client":
    Directory( params.pid_dir,
      owner = params.hbase_user,
      recursive = True
    )
  
    Directory ( [params.tmp_dir, params.log_dir],
      owner = params.hbase_user,
      recursive = True
    )    

def hbase_TemplateConfig(name, 
                         tag=None
                         ):
  import params

  TemplateConfig( format("{conf_dir}/{name}"),
      owner = params.hbase_user,
      template_tag = tag
  )
