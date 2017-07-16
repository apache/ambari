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
import fileinput
import shutil
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core import shell

def hbase(name=None # 'master' or 'regionserver' or 'client'
              ):
  import params

  #Directory( params.hbase_conf_dir_prefix,
  #    mode=0755
  #)

  # On some OS this folder could be not exists, so we will create it before pushing there files
  Directory(params.limits_conf_dir,
            create_parents=True,
            owner='root',
            group='root'
            )

  File(os.path.join(params.limits_conf_dir, 'hbase.conf'),
       owner='root',
       group='root',
       mode=0644,
       content=Template("hbase.conf.j2")
       )

  Directory( params.hbase_conf_dir,
      owner = params.hbase_user,
      group = params.user_group,
      create_parents = True
  )

  '''Directory (params.tmp_dir,
             owner = params.hbase_user,
             mode=0775,
             create_parents = True,
             cd_access="a",
  )

  Directory (params.local_dir,
             owner = params.hbase_user,
             group = params.user_group,
             mode=0775,
             create_parents = True
  )

  Directory (os.path.join(params.local_dir, "jars"),
             owner = params.hbase_user,
             group = params.user_group,
             mode=0775,
             create_parents = True
  )'''

  parent_dir = os.path.dirname(params.tmp_dir)
  # In case if we have several placeholders in path
  while ("${" in parent_dir):
    parent_dir = os.path.dirname(parent_dir)
  if parent_dir != os.path.abspath(os.sep) :
    Directory (parent_dir,
          create_parents = True,
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

    #XmlConfig("hdfs-site.xml",
    #        conf_dir=params.hadoop_conf_dir,
    #        configurations=params.config['configurations']['hdfs-site'],
    #        configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
    #        owner=params.hdfs_user,
    #        group=params.user_group
    #)

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
       content=InlineTemplate(params.hbase_env_sh_template)
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
      create_parents = True
    )

    Directory (params.log_dir,
      owner = params.hbase_user,
      create_parents = True
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
  if name in ["master","regionserver"]:
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
    params.HdfsResource(params.hbase_hdfs_user_dir,
                         type="directory",
                          action="create_on_execute",
                          owner=params.hbase_user,
                          mode=params.hbase_hdfs_user_mode
    )
    params.HdfsResource(None, action="execute")

# create java-opts in etc/hbase/conf dir for iop.version
  File(format("{params.hbase_conf_dir}/java-opts"),
         mode=0644,
         group=params.user_group,
         owner=params.hbase_user,
         content=params.hbase_javaopts_properties
    )

def hbase_TemplateConfig(name,
                         tag=None
                         ):
  import params

  TemplateConfig( format("{hbase_conf_dir}/{name}"),
      owner = params.hbase_user,
      template_tag = tag
  )

def get_iop_version():
  try:
    command = 'iop-select status hadoop-client'
    return_code, iop_output = shell.call(command, timeout=20)
  except Exception, e:
    Logger.error(str(e))
    raise Fail('Unable to execute iop-select command to retrieve the version.')

  if return_code != 0:
    raise Fail(
      'Unable to determine the current version because of a non-zero return code of {0}'.format(str(return_code)))

  iop_version = re.sub('hadoop-client - ', '', iop_output)
  match = re.match('[0-9]+.[0-9]+.[0-9]+.[0-9]+', iop_version)

  if match is None:
    raise Fail('Failed to get extracted version')

  return iop_version
