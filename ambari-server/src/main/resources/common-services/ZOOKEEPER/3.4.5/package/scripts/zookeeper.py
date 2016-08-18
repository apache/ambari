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
import sys

from resource_management import *
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.version import compare_versions, format_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def zookeeper(type = None, upgrade_type=None):
  import params

  if type == 'server':
    # This path may be missing after Ambari upgrade. We need to create it. We need to do this before any configs will
    # be applied.
    if upgrade_type is None and not os.path.exists(os.path.join(params.stack_root,"/current/zookeeper-server")) and params.current_version\
      and check_stack_feature(StackFeature.ROLLING_UPGRADE, format_stack_version(params.version)):
      conf_select.select(params.stack_name, "zookeeper", params.current_version)
      stack_select.select("zookeeper-server", params.version)

  Directory(params.config_dir,
            owner=params.zk_user,
            create_parents = True,
            group=params.user_group
  )

  File(os.path.join(params.config_dir, "zookeeper-env.sh"),
       content=InlineTemplate(params.zk_env_sh_template),
       owner=params.zk_user,
       group=params.user_group
  )
  

  configFile("zoo.cfg", template_name="zoo.cfg.j2")
  configFile("configuration.xsl", template_name="configuration.xsl.j2")

  Directory(params.zk_pid_dir,
            owner=params.zk_user,
            create_parents = True,
            group=params.user_group,
            mode=0755,
  )

  Directory(params.zk_log_dir,
            owner=params.zk_user,
            create_parents = True,
            group=params.user_group,
            mode=0755,
  )

  Directory(params.zk_data_dir,
            owner=params.zk_user,
            create_parents = True,
            cd_access="a",
            group=params.user_group,
            mode=0755,
  )

  if type == 'server':
    myid = str(sorted(params.zookeeper_hosts).index(params.hostname) + 1)

    File(os.path.join(params.zk_data_dir, "myid"),
         mode = 0644,
         content = myid
    )

  if (params.log4j_props != None):
    File(os.path.join(params.config_dir, "log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.zk_user,
         content=InlineTemplate(params.log4j_props)
    )
  elif (os.path.exists(os.path.join(params.config_dir, "log4j.properties"))):
    File(os.path.join(params.config_dir, "log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.zk_user
    )

  if params.security_enabled:
    if type == "server":
      configFile("zookeeper_jaas.conf", template_name="zookeeper_jaas.conf.j2")
      configFile("zookeeper_client_jaas.conf", template_name="zookeeper_client_jaas.conf.j2")
    else:
      configFile("zookeeper_client_jaas.conf", template_name="zookeeper_client_jaas.conf.j2")

  File(os.path.join(params.config_dir, "zoo_sample.cfg"),
       owner=params.zk_user,
       group=params.user_group
  )

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def zookeeper(type = None, upgrade_type=None):
  import params
  configFile("zoo.cfg", template_name="zoo.cfg.j2", mode="f")
  configFile("configuration.xsl", template_name="configuration.xsl.j2", mode="f")

  ServiceConfig(params.zookeeper_win_service_name,
                action="change_user",
                username = params.zk_user,
                password = Script.get_password(params.zk_user))


  Directory(params.zk_data_dir,
            owner=params.zk_user,
            mode="(OI)(CI)F",
            create_parents = True
  )
  if (params.log4j_props != None):
    File(os.path.join(params.config_dir, "log4j.properties"),
         mode="f",
         owner=params.zk_user,
         content=params.log4j_props
    )
  elif (os.path.exists(os.path.join(params.config_dir, "log4j.properties"))):
    File(os.path.join(params.config_dir, "log4j.properties"),
         mode="f",
         owner=params.zk_user
    )
  if type == 'server':
    myid = str(sorted(params.zookeeper_hosts).index(params.hostname) + 1)
    File(os.path.join(params.zk_data_dir, "myid"),
         owner=params.zk_user,
         mode = "f",
         content = myid
    )

def configFile(name, template_name=None, mode=None):
  import params

  File(os.path.join(params.config_dir, name),
       content=Template(template_name),
       owner=params.zk_user,
       group=params.user_group,
       mode=mode
  )



