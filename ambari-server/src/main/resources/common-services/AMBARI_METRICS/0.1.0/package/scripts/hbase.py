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
from ambari_commons import OSConst
from resource_management import *
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hbase(name=None, action = None):
  import params
  Directory(params.hbase_conf_dir,
            owner = params.hadoop_user,
            recursive = True
  )
  Directory(params.hbase_tmp_dir,
             recursive = True,
             owner = params.hadoop_user
  )

  Directory (os.path.join(params.local_dir, "jars"),
             owner = params.hadoop_user,
             recursive = True
  )

  XmlConfig("hbase-site.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['ams-hbase-site'],
            configuration_attributes=params.config['configuration_attributes']['ams-hbase-site'],
            owner = params.hadoop_user
  )

  if 'ams-hbase-policy' in params.config['configurations']:
    XmlConfig("hbase-policy.xml",
              conf_dir = params.hbase_conf_dir,
              configurations = params.config['configurations']['ams-hbase-policy'],
              configuration_attributes=params.config['configuration_attributes']['ams-hbase-policy'],
              owner = params.hadoop_user
    )
  # Manually overriding ownership of file installed by hadoop package
  else:
    File(os.path.join(params.hbase_conf_dir, "hbase-policy.xml"),
          owner = params.hadoop_user
    )

  # Metrics properties
  File(os.path.join(params.hbase_conf_dir, "hadoop-metrics2-hbase.properties"),
       owner = params.hbase_user,
       content=Template("hadoop-metrics2-hbase.properties.j2")
  )

  hbase_TemplateConfig('regionservers', user=params.hadoop_user)

  if params.security_enabled:
    hbase_TemplateConfig(format("hbase_{name}_jaas.conf"), user=params.hadoop_user)

  if name != "client":
    Directory (params.hbase_log_dir,
               owner = params.hadoop_user,
               recursive = True
    )

  if (params.hbase_log4j_props != None):
    File(os.path.join(params.hbase_conf_dir, "log4j.properties"),
         owner=params.hadoop_user,
         content=params.hbase_log4j_props
    )
  elif (os.path.exists(os.path.join(params.hbase_conf_dir,"log4j.properties"))):
    File(os.path.join(params.hbase_conf_dir,"log4j.properties"),
         owner=params.hadoop_user
    )

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hbase(name=None # 'master' or 'regionserver' or 'client'
          , action=None):
  import params

  Directory(params.hbase_conf_dir,
      owner = params.hbase_user,
      group = params.user_group,
      recursive = True
  )

  Execute(('chown', '-R', params.hbase_user, params.hbase_conf_dir),
          sudo=True
          )

  Directory (params.hbase_tmp_dir,
             owner = params.hbase_user,
             cd_access="a",
             recursive = True
  )

  Execute(('chown', '-R', params.hbase_user, params.hbase_tmp_dir),
          sudo=True
          )

  Directory (os.path.join(params.local_dir, "jars"),
             owner = params.hbase_user,
             group = params.user_group,
             cd_access="a",
             mode=0775,
             recursive = True
  )

  merged_ams_hbase_site = {}
  merged_ams_hbase_site.update(params.config['configurations']['ams-hbase-site'])
  if params.security_enabled:
    merged_ams_hbase_site.update(params.config['configurations']['ams-hbase-security-site'])

  if not params.is_hbase_distributed:
    File(format("{hbase_conf_dir}/core-site.xml"),
         action='delete',
         owner=params.hbase_user)

    File(format("{hbase_conf_dir}/hdfs-site.xml"),
         action='delete',
         owner=params.hbase_user)

  XmlConfig("hbase-site.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = merged_ams_hbase_site,
            configuration_attributes=params.config['configuration_attributes']['ams-hbase-site'],
            owner = params.hbase_user,
            group = params.user_group
  )

  # Phoenix spool file dir if not /tmp
  if not os.path.exists(params.phoenix_server_spool_dir):
    Directory(params.phoenix_server_spool_dir,
              owner=params.ams_user,
              mode = 0755,
              group=params.user_group,
              cd_access="a",
              recursive=True
    )
  pass

  if 'ams-hbase-policy' in params.config['configurations']:
    XmlConfig("hbase-policy.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['ams-hbase-policy'],
            configuration_attributes=params.config['configuration_attributes']['ams-hbase-policy'],
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

  # Metrics properties
  File(os.path.join(params.hbase_conf_dir, "hadoop-metrics2-hbase.properties"),
         owner = params.hbase_user,
         group = params.user_group,
         content=Template("hadoop-metrics2-hbase.properties.j2")
    )

  # hbase_TemplateConfig( params.metric_prop_file_name,
  #   tag = 'GANGLIA-MASTER' if name == 'master' else 'GANGLIA-RS'
  # )

  hbase_TemplateConfig('regionservers', user=params.hbase_user)

  if params.security_enabled:
    hbase_TemplateConfig( format("hbase_{name}_jaas.conf"), user=params.hbase_user)
    hbase_TemplateConfig( format("hbase_client_jaas.conf"), user=params.hbase_user)
    hbase_TemplateConfig( format("ams_zookeeper_jaas.conf"), user=params.hbase_user)

  if name != "client":
    Directory( params.hbase_pid_dir,
               owner = params.hbase_user,
               recursive = True,
               cd_access = "a",
               mode = 0755,
    )

    Directory (params.hbase_log_dir,
               owner = params.hbase_user,
               recursive = True,
               cd_access = "a",
               mode = 0755,
    )

  if name == "master":

    if not params.is_local_fs_rootdir:
      # If executing Stop All, HDFS is probably down
      if action != 'stop':

        params.HdfsResource(params.hbase_root_dir,
                             type="directory",
                             action="create_on_execute",
                             owner=params.hbase_user,
                             mode=0775,
                             dfs_type=params.dfs_type
        )

        params.HdfsResource(params.hbase_staging_dir,
                             type="directory",
                             action="create_on_execute",
                             owner=params.hbase_user,
                             mode=0711,
                             dfs_type=params.dfs_type
        )

        params.HdfsResource(None, action="execute")

      if params.is_hbase_distributed:
        #Workaround for status commands not aware of operating mode
        File(format("{params.hbase_pid_dir}/distributed_mode"), action="create", mode=0644, owner=params.hbase_user)

      pass

    else:

      local_root_dir = params.hbase_root_dir
      #cut protocol name
      if local_root_dir.startswith("file://"):
        local_root_dir = local_root_dir[7:]
        #otherwise assume dir name is provided as is

      Directory(local_root_dir,
                owner = params.hbase_user,
                cd_access="a",
                recursive = True
      )

      Execute(('chown', '-R', params.hbase_user, local_root_dir),
              sudo=True
              )

      File(format("{params.hbase_pid_dir}/distributed_mode"), action="delete", owner=params.hbase_user)

  if params.hbase_log4j_props is not None:
    File(format("{params.hbase_conf_dir}/log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.hbase_user,
         content=params.hbase_log4j_props
    )
  elif os.path.exists(format("{params.hbase_conf_dir}/log4j.properties")):
    File(format("{params.hbase_conf_dir}/log4j.properties"),
      mode=0644,
      group=params.user_group,
      owner=params.hbase_user
    )

def hbase_TemplateConfig(name, tag=None, user=None):
  import params

  TemplateConfig( os.path.join(params.hbase_conf_dir, name),
      owner = user,
      template_tag = tag
  )
