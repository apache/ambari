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

from resource_management.core.resources.system import Directory, File
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import is_empty

def mahout():
  import params

  Directory( params.mahout_conf_dir,
             create_parents = True,
             owner = params.mahout_user,
             group = params.user_group
  )

  XmlConfig("yarn-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['yarn-site'],
            configuration_attributes=params.config['configuration_attributes']['yarn-site'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0644
  )

  if not is_empty(params.log4j_props):
    File(format("{params.mahout_conf_dir}/log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.mahout_user,
         content=params.log4j_props
    )
  elif (os.path.exists(format("{params.mahout_conf_dir}/log4j.properties"))):
    File(format("{params.mahout_conf_dir}/log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.mahout_user
    )
