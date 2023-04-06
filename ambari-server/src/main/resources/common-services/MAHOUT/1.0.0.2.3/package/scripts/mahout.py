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

from resource_management.core.resources import Directory
from resource_management.core.resources import File
from resource_management.libraries.functions import format
from resource_management.libraries.functions import is_empty
from resource_management.libraries.functions import lzo_utils
from resource_management.libraries.resources import XmlConfig


def mahout():
  import params

  # ensure that matching LZO libraries are installed for Mahout
  lzo_utils.install_lzo_if_needed()

  Directory( params.mahout_conf_dir,
             create_parents = True,
             owner = params.mahout_user,
             group = params.user_group
  )

  XmlConfig("yarn-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['yarn-site'],
            configuration_attributes=params.config['configurationAttributes']['yarn-site'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0o644
  )

  if not is_empty(params.log4j_props):
    File(format("{params.mahout_conf_dir}/log4j.properties"),
         mode=0o644,
         group=params.user_group,
         owner=params.mahout_user,
         content=params.log4j_props
    )
  elif (os.path.exists(format("{params.mahout_conf_dir}/log4j.properties"))):
    File(format("{params.mahout_conf_dir}/log4j.properties"),
         mode=0o644,
         group=params.user_group,
         owner=params.mahout_user
    )
