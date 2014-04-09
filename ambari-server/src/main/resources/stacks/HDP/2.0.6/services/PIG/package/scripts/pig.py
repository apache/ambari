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

Ambari Agent

"""
import os

from resource_management import *

def pig():
  import params

  Directory( params.pig_conf_dir,
    owner = params.hdfs_user,
    group = params.user_group
  )

  pig_TemplateConfig( ['pig-env.sh','pig.properties'])

  if (params.log4j_props != None):
    File(format("{params.pig_conf_dir}/log4j.properties"),
      mode=0644,
      group=params.user_group,
      owner=params.hdfs_user,
      content=params.log4j_props
    )
  elif (os.path.exists(format("{params.pig_conf_dir}/log4j.properties"))):
    File(format("{params.pig_conf_dir}/log4j.properties"),
      mode=0644,
      group=params.user_group,
      owner=params.hdfs_user
    )

def pig_TemplateConfig(name):
  import params
  
  if not isinstance(name, list):
    name = [name]
    
  for x in name:
    TemplateConfig( format("{pig_conf_dir}/{x}"),
        owner = params.hdfs_user
    )
  
