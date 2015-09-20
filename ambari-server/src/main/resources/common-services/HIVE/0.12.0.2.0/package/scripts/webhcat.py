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
import sys
import os.path
from resource_management import *
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.version import compare_versions
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def webhcat():
  import params
  XmlConfig("webhcat-site.xml",
            conf_dir=params.hcat_config_dir,
            configurations=params.config['configurations']['webhcat-site']
  )
  # Manually overriding service logon user & password set by the installation package
  ServiceConfig(params.webhcat_server_win_service_name,
                action="change_user",
                username = params.hcat_user,
                password = Script.get_password(params.hcat_user))


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def webhcat():
  import params

  from setup_atlas_hive import setup_atlas_hive

  Directory(params.templeton_pid_dir,
            owner=params.webhcat_user,
            mode=0755,
            group=params.user_group,
            recursive=True)

  Directory(params.templeton_log_dir,
            owner=params.webhcat_user,
            mode=0755,
            group=params.user_group,
            recursive=True)

  Directory(params.config_dir,
            recursive=True,
            owner=params.webhcat_user,
            group=params.user_group,
            cd_access="a")

  if params.security_enabled:
    kinit_if_needed = format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name};")
  else:
    kinit_if_needed = ""

  if kinit_if_needed:
    Execute(kinit_if_needed,
            user=params.webhcat_user,
            path='/bin'
    )

  # Replace _HOST with hostname in relevant principal-related properties
  webhcat_site = params.config['configurations']['webhcat-site'].copy()
  for prop_name in ['templeton.hive.properties', 'templeton.kerberos.principal']:
    if prop_name in webhcat_site:
      webhcat_site[prop_name] = webhcat_site[prop_name].replace("_HOST", params.hostname)

  XmlConfig("webhcat-site.xml",
            conf_dir=params.config_dir,
            configurations=webhcat_site,
            configuration_attributes=params.config['configuration_attributes']['webhcat-site'],
            owner=params.webhcat_user,
            group=params.user_group,
            )

  # if we're in an upgrade of a secure cluster, make sure hive-site and yarn-site are created
  if Script.is_hdp_stack_greater_or_equal("2.3") and params.version:
    XmlConfig("hive-site.xml",
      conf_dir = format("/usr/hdp/{version}/hive/conf"),
      configurations = params.hive_site_config,
      configuration_attributes = params.config['configuration_attributes']['hive-site'],
      owner = params.hive_user,
      group = params.user_group,
      )

    XmlConfig("yarn-site.xml",
      conf_dir = format("/usr/hdp/{version}/hadoop/conf"),
      configurations = params.config['configurations']['yarn-site'],
      configuration_attributes = params.config['configuration_attributes']['yarn-site'],
      owner = params.yarn_user,
      group = params.user_group,    
  )
  

  File(format("{config_dir}/webhcat-env.sh"),
       owner=params.webhcat_user,
       group=params.user_group,
       content=InlineTemplate(params.webhcat_env_sh_template)
  )
  
  Directory(params.webhcat_conf_dir,
       cd_access='a',
       recursive=True
  )

  log4j_webhcat_filename = 'webhcat-log4j.properties'
  if (params.log4j_webhcat_props != None):
    File(format("{config_dir}/{log4j_webhcat_filename}"),
         mode=0644,
         group=params.user_group,
         owner=params.webhcat_user,
         content=params.log4j_webhcat_props
    )
  elif (os.path.exists("{config_dir}/{log4j_webhcat_filename}.template")):
    File(format("{config_dir}/{log4j_webhcat_filename}"),
         mode=0644,
         group=params.user_group,
         owner=params.webhcat_user,
         content=StaticFile(format("{config_dir}/{log4j_webhcat_filename}.template"))
    )

  setup_atlas_hive(configuration_directory=params.config_dir)
