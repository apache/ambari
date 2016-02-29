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
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hbase(name=None):
  import params
  XmlConfig("hbase-site.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['hbase-site'],
            configuration_attributes=params.config['configuration_attributes']['hbase-site']
  )

  if params.service_map.has_key(name):
    # Manually overriding service logon user & password set by the installation package
    service_name = params.service_map[name]
    ServiceConfig(service_name,
                  action="change_user",
                  username = params.hbase_user,
                  password = Script.get_password(params.hbase_user))

# name is 'master' or 'regionserver' or 'queryserver' or 'client'
@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hbase(name=None):
  import params

  Directory( params.etc_prefix_dir,
      mode=0755
  )

  Directory( params.hbase_conf_dir,
      owner = params.hbase_user,
      group = params.user_group,
      recursive = True
  )

  Directory(params.java_io_tmpdir,
      recursive = True,
      mode=0777,
  )

  parent_dir = os.path.dirname(params.tmp_dir)
  # In case if we have several placeholders in path
  while ("${" in parent_dir):
    parent_dir = os.path.dirname(parent_dir)
  if parent_dir != os.path.abspath(os.sep) :
    Directory (parent_dir,
          recursive = True,
          cd_access="a",
    )
    Execute(("chmod", "1777", parent_dir), sudo=True)

  XmlConfig( "hbase-site.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['hbase-site'],
            configuration_attributes=params.config['configuration_attributes']['hbase-site'],
            owner = params.hbase_user,
            group = params.user_group
  )

  XmlConfig( "core-site.xml",
             conf_dir = params.hbase_conf_dir,
             configurations = params.config['configurations']['core-site'],
             configuration_attributes=params.config['configuration_attributes']['core-site'],
             owner = params.hbase_user,
             group = params.user_group
  )

  if 'hdfs-site' in params.config['configurations']:
    XmlConfig( "hdfs-site.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner = params.hbase_user,
            group = params.user_group
    )

    XmlConfig("hdfs-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['hdfs-site'],
            configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
            owner=params.hdfs_user,
            group=params.user_group
    )

  if 'hbase-policy' in params.config['configurations']:
    XmlConfig( "hbase-policy.xml",
            conf_dir = params.hbase_conf_dir,
            configurations = params.config['configurations']['hbase-policy'],
            configuration_attributes=params.config['configuration_attributes']['hbase-policy'],
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
       content=InlineTemplate(params.hbase_env_sh_template),
       group = params.user_group,
  )

  # On some OS this folder could be not exists, so we will create it before pushing there files
  Directory(params.limits_conf_dir,
            recursive=True,
            owner='root',
            group='root'
            )

  File(os.path.join(params.limits_conf_dir, 'hbase.conf'),
       owner='root',
       group='root',
       mode=0644,
       content=Template("hbase.conf.j2")
       )

  hbase_TemplateConfig( params.metric_prop_file_name,
    tag = 'GANGLIA-MASTER' if name == 'master' else 'GANGLIA-RS'
  )

  hbase_TemplateConfig( 'regionservers')

  if params.security_enabled:
    hbase_TemplateConfig( format("hbase_{name}_jaas.conf"))
  
  if name != "client":
    Directory( params.pid_dir,
      owner = params.hbase_user,
      recursive = True,
      cd_access = "a",
      mode = 0755,
    )
  
    Directory (params.log_dir,
      owner = params.hbase_user,
      recursive = True,
      cd_access = "a",
      mode = 0755,
    )

  if (params.log4j_props != None):
    File(format("{params.hbase_conf_dir}/log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.hbase_user,
         content=params.log4j_props
    )
  elif (os.path.exists(format("{params.hbase_conf_dir}/log4j.properties"))):
    File(format("{params.hbase_conf_dir}/log4j.properties"),
      mode=0644,
      group=params.user_group,
      owner=params.hbase_user
    )
  if name == "master":
    params.HdfsResource(params.hbase_hdfs_root_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.hbase_user
    )
    params.HdfsResource(params.hbase_staging_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.hbase_user,
                         mode=0711
    )
    params.HdfsResource(None, action="execute")

  if params.phoenix_enabled:
    Package(params.phoenix_package,
            retry_on_repo_unavailability=params.agent_stack_retry_on_unavailability,
            retry_count=params.agent_stack_retry_count)

def hbase_TemplateConfig(name, tag=None):
  import params

  TemplateConfig( format("{hbase_conf_dir}/{name}"),
      owner = params.hbase_user,
      template_tag = tag
  )
