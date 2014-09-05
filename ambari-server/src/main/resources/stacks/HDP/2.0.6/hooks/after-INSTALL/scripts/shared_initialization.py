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

def setup_hadoop_env():
  import params
  if params.has_namenode:
    if params.security_enabled:
      tc_owner = "root"
    else:
      tc_owner = params.hdfs_user
    Directory(params.hadoop_conf_empty_dir,
              recursive=True,
              owner='root',
              group='root'
    )
    Link(params.hadoop_conf_dir,
         to=params.hadoop_conf_empty_dir,
         not_if=format("ls {hadoop_conf_dir}")
    )
    File(os.path.join(params.hadoop_conf_dir, 'hadoop-env.sh'),
         owner=tc_owner,
         content=InlineTemplate(params.hadoop_env_sh_template)
    )

def setup_config():
  import params
  if params.has_namenode:
    XmlConfig("core-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['core-site'],
              configuration_attributes=params.config['configuration_attributes']['core-site'],
              owner=params.hdfs_user,
              group=params.user_group
  )