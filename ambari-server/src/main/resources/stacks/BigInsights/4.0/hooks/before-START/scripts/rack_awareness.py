#!/usr/bin/env python

'''
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
'''
from resource_management.core.resources import File, Directory
from resource_management.core.source import StaticFile, Template
from resource_management.libraries.functions import format

import os

def create_topology_mapping():
  import params
  
  path=params.net_topology_mapping_data_file_path 
  parent_dir=os.path.dirname(path) 
  # only create the parent directory and set its permission if it does not exist
  if not os.path.exists(parent_dir): 
    Directory(parent_dir, 
              create_parents = True,
              owner=params.hdfs_user, 
              group=params.user_group) 

  # placing the mappings file in the same folder where the topology script is located
  File(path,
       content=Template("topology_mappings.data.j2"),
       owner=params.hdfs_user,
       group=params.user_group,
       # if there is no hadoop components, don't create the script
       only_if=format("test -d {net_topology_script_dir}"),
  )

def create_topology_script():
  import params

  path=params.net_topology_script_file_path
  parent_dir=os.path.dirname(path) 
  # only create the parent directory and set its permission if it does not exist 
  if not os.path.exists(parent_dir): 
    Directory(parent_dir, 
              create_parents = True,
              owner=params.hdfs_user, 
              group=params.user_group) 

  # installing the topology script to the specified location
  File(path,
       content=StaticFile('topology_script.py'),
       mode=0755,
       only_if=format("test -d {net_topology_script_dir}"),
  )

  
def create_topology_script_and_mapping():
  import params
  if params.has_hadoop_env:
    create_topology_mapping()
    create_topology_script()