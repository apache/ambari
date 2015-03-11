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

def accumulo(name=None # 'master' or 'tserver' or 'client'
              ):
  import params

  Directory( params.accumulo_conf_dir,
      owner = params.accumulo_user,
      recursive = True
  )

  XmlConfig( "accumulo-site.xml",
            conf_dir = params.accumulo_conf_dir,
            configurations = params.config['configurations']['accumulo-site'],
            configuration_attributes=params.config['configuration_attributes']['accumulo-site'],
            owner = params.accumulo_user,
            mode = 0600
  )

  XmlConfig("hdfs-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner=params.hdfs_user,
    )

  if 'accumulo-policy' in params.config['configurations']:
    XmlConfig( "accumulo-policy.xml",
            conf_dir = params.accumulo_conf_dir,
            configurations = params.config['configurations']['accumulo-policy'],
            configuration_attributes=params.config['configuration_attributes']['accumulo-policy'],
            owner = params.accumulo_user,
    )
  else:
    File( format("{params.accumulo_conf_dir}/accumulo-policy.xml"),
      owner = params.accumulo_user,
    )

  Directory (params.log_dir,
    owner = params.accumulo_user,
    recursive = True
  )

  if (params.log4j_props != None):
    File(format("{params.accumulo_conf_dir}/log4j.properties"),
         mode=0644,
         owner=params.accumulo_user,
         content=params.log4j_props
    )
  elif (os.path.exists(format("{params.accumulo_conf_dir}/log4j.properties"))):
    File(format("{params.accumulo_conf_dir}/log4j.properties"),
      mode=0644,
      owner=params.accumulo_user
    )

  if name in ["master","tserver"]:
    params.HdfsDirectory(format("{params.accumulo_hdfs_root_dir}"),
                         action="create_delayed",
                         owner=params.accumulo_user,
    )
    params.HdfsDirectory(format("{params.accumulo_hdfs_stage_dir}"),
                         action="create_delayed",
                         owner=params.accumulo_user,
                         mode=0751
    )
    params.HdfsDirectory(None, action="create")

  accumulo_StaticFile("auditLog.xml")
  accumulo_StaticFile("generic_logger.xml")
  accumulo_StaticFile("monitor_logger.xml")
  accumulo_StaticFile("accumulo-metrics.xml")

  accumulo_StaticFile("tracers")
  accumulo_StaticFile("gc")
  accumulo_StaticFile("monitor")
  accumulo_StaticFile('slaves')
  accumulo_StaticFile('masters')

  accumulo_TemplateConfig('accumulo-env.sh')

def accumulo_TemplateConfig(name,
                         tag=None
                         ):
  import params

  TemplateConfig( format("{params.accumulo_conf_dir}/{name}"),
      owner = params.accumulo_user,
      template_tag = tag
  )

def accumulo_StaticFile(name):
    import params

    File(format("{params.accumulo_conf_dir}/{name}"),
      mode=0644,
      owner=params.accumulo_user,
      content=StaticFile(name)
    )
