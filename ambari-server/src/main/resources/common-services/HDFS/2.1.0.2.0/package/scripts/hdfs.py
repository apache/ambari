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
from resource_management.core.resources.system import Execute, Directory, File, Link
from resource_management.core.resources import Package
from resource_management.core.source import Template
from resource_management.core.resources.service import ServiceConfig
from resource_management.libraries.resources.xml_config import XmlConfig

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions.format import format
import os
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from resource_management.libraries.functions.lzo_utils import install_lzo_if_needed

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
    File(os.path.join(params.hadoop_conf_dir, 'hdfs_dn_jaas.conf'),
         owner=params.hdfs_user,
         group=params.user_group,
         content=Template("hdfs_dn_jaas.conf.j2")
    )
    File(os.path.join(params.hadoop_conf_dir, 'hdfs_nn_jaas.conf'),
         owner=params.hdfs_user,
         group=params.user_group,
         content=Template("hdfs_nn_jaas.conf.j2")
    )
    if params.dfs_ha_enabled:
      File(os.path.join(params.hadoop_conf_dir, 'hdfs_jn_jaas.conf'),
           owner=params.hdfs_user,
           group=params.user_group,
           content=Template("hdfs_jn_jaas.conf.j2")
      )

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
  
  install_lzo_if_needed()
      
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

class ConfigStatusParser():
    def __init__(self):
        self.reconfig_successful = False

    def handle_new_line(self, line, is_stderr):
        if is_stderr:
            return

        if line.startswith('SUCCESS: Changed property'):
            self.reconfig_successful = True

        Logger.info('[reconfig] %s' % (line))

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def reconfig(componentName, componentAddress):
    import params

    if params.security_enabled:
        Execute(params.nn_kinit_cmd,
                user=params.hdfs_user
                )

    nn_reconfig_cmd = format('hdfs --config {hadoop_conf_dir} dfsadmin -reconfig {componentName} {componentAddress} start')

    Execute (nn_reconfig_cmd,
             user=params.hdfs_user,
             logoutput=True,
             path=params.hadoop_bin_dir
             )

    nn_reconfig_cmd = format('hdfs --config {hadoop_conf_dir} dfsadmin -reconfig {componentName} {componentAddress} status')
    config_status_parser = ConfigStatusParser()
    Execute (nn_reconfig_cmd,
             user=params.hdfs_user,
             logoutput=False,
             path=params.hadoop_bin_dir,
             on_new_line=config_status_parser.handle_new_line
             )


    if not config_status_parser.reconfig_successful:
        Logger.info('Reconfiguration failed')
        raise Fail('Reconfiguration failed!')

    Logger.info('Reconfiguration successfully completed.')

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

    if params.hdfs_include_file:
      File(params.include_file_path,
         content=Template("include_hosts_list.j2"),
         owner=params.hdfs_user,
         mode="f",
         )
      pass
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
