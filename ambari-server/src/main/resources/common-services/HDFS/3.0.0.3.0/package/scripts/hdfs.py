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

from resource_management.libraries.script.script import Script
from resource_management.core.resources.system import Directory, File, Link
from resource_management.core.resources import Package
from resource_management.core.source import Template
from resource_management.core.resources.service import ServiceConfig
from resource_management.libraries.resources.xml_config import XmlConfig
import os
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hdfs(name=None):
  import params

  if params.create_lib_snappy_symlinks:
    install_snappy()
  
  # On some OS this folder could be not exists, so we will create it before pushing there files
  Directory(params.limits_conf_dir,
            create_parents = True,
            owner='root',
            group='root'
  )

  File(os.path.join(params.limits_conf_dir, 'hdfs.conf'),
       owner='root',
       group='root',
       mode=0644,
       content=Template("hdfs.conf.j2")
  )

  if params.security_enabled:
    tc_mode = 0644
    tc_owner = "root"
  else:
    tc_mode = None
    tc_owner = params.hdfs_user

  if "hadoop-policy" in params.config['configurations']:
    XmlConfig("hadoop-policy.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['hadoop-policy'],
              configuration_attributes=params.config['configuration_attributes']['hadoop-policy'],
              owner=params.hdfs_user,
              group=params.user_group
    )

  if "ssl-client" in params.config['configurations']:
    XmlConfig("ssl-client.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['ssl-client'],
              configuration_attributes=params.config['configuration_attributes']['ssl-client'],
              owner=params.hdfs_user,
              group=params.user_group
    )

    Directory(params.hadoop_conf_secure_dir,
              create_parents = True,
              owner='root',
              group=params.user_group,
              cd_access='a',
              )

    XmlConfig("ssl-client.xml",
              conf_dir=params.hadoop_conf_secure_dir,
              configurations=params.config['configurations']['ssl-client'],
              configuration_attributes=params.config['configuration_attributes']['ssl-client'],
              owner=params.hdfs_user,
              group=params.user_group
    )

  if "ssl-server" in params.config['configurations']:
    XmlConfig("ssl-server.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['ssl-server'],
              configuration_attributes=params.config['configuration_attributes']['ssl-server'],
              owner=params.hdfs_user,
              group=params.user_group
    )

  XmlConfig("hdfs-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner=params.hdfs_user,
            group=params.user_group
  )

  XmlConfig("core-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['core-site'],
            configuration_attributes=params.config['configuration_attributes']['core-site'],
            owner=params.hdfs_user,
            group=params.user_group,
            mode=0644
  )

  File(os.path.join(params.hadoop_conf_dir, 'slaves'),
       owner=tc_owner,
       content=Template("slaves.j2")
  )
  
  if params.lzo_enabled and len(params.lzo_packages) > 0:
      Package(params.lzo_packages,
              retry_on_repo_unavailability=params.agent_stack_retry_on_unavailability,
              retry_count=params.agent_stack_retry_count)
      
def install_snappy():
  import params
  Directory([params.so_target_dir_x86, params.so_target_dir_x64],
            create_parents = True,
  )    
  Link(params.so_target_x86,
       to=params.so_src_x86,
  )
  Link(params.so_target_x64,
       to=params.so_src_x64,
  )

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hdfs(component=None):
  import params
  if component == "namenode":
    directories = params.dfs_name_dir.split(",")
    Directory(directories,
              owner=params.hdfs_user,
              mode="(OI)(CI)F",
              create_parents = True
    )
    File(params.exclude_file_path,
         content=Template("exclude_hosts_list.j2"),
         owner=params.hdfs_user,
         mode="f",
         )
  if params.service_map.has_key(component):
    service_name = params.service_map[component]
    ServiceConfig(service_name,
                  action="change_user",
                  username=params.hdfs_user,
                  password=Script.get_password(params.hdfs_user))

  if "hadoop-policy" in params.config['configurations']:
    XmlConfig("hadoop-policy.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['hadoop-policy'],
              owner=params.hdfs_user,
              mode="f",
              configuration_attributes=params.config['configuration_attributes']['hadoop-policy']
    )

  XmlConfig("hdfs-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            owner=params.hdfs_user,
            mode="f",
            configuration_attributes=params.config['configuration_attributes']['hdfs-site']
  )
