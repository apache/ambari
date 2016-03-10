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

import os.path

from resource_management.core.environment import Environment
from resource_management.core.source import InlineTemplate
from resource_management.core.source import Template
from resource_management.core.resources import Execute
from resource_management.core.resources.service import Service
from resource_management.core.resources.service import ServiceConfig
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import File
from resource_management.libraries.script import Script
from resource_management.libraries.resources import PropertiesFile
from resource_management.libraries.functions import format

from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

@OsFamilyFuncImpl(os_family = OsFamilyImpl.DEFAULT)
def falcon(type, action = None, upgrade_type=None):
  import params

  if action == 'config':
    Directory(params.falcon_pid_dir,
      owner = params.falcon_user,
      recursive = True,
      mode = 0755,
      cd_access = "a",
    )

    Directory(params.falcon_log_dir,
      owner = params.falcon_user,
      recursive = True,
      mode = 0755,
      cd_access = "a",
    )

    Directory(params.falcon_webapp_dir,
      owner = params.falcon_user,
      recursive = True)

    Directory(params.falcon_home,
      owner = params.falcon_user,
      recursive = True)

    Directory(params.etc_prefix_dir,
      mode = 0755,
      recursive = True)

    Directory(params.falcon_conf_dir,
      owner = params.falcon_user,
      recursive = True)

    File(params.falcon_conf_dir + '/falcon-env.sh',
      content = InlineTemplate(params.falcon_env_sh_template),
      owner = params.falcon_user,
      group=params.user_group,
    )

    File(params.falcon_conf_dir + '/client.properties',
      content = Template('client.properties.j2'),
      mode = 0644,
      owner = params.falcon_user)

    PropertiesFile(params.falcon_conf_dir + '/runtime.properties',
      properties = params.falcon_runtime_properties,
      mode = 0644,
      owner = params.falcon_user)

    PropertiesFile(params.falcon_conf_dir + '/startup.properties',
      properties = params.falcon_startup_properties,
      mode = 0644,
      owner = params.falcon_user)

    if params.falcon_graph_storage_directory:
      Directory(params.falcon_graph_storage_directory,
        owner = params.falcon_user,
        group = params.user_group,
        mode = 0775,
        recursive = True,
        cd_access = "a")

    if params.falcon_graph_serialize_path:
      Directory(params.falcon_graph_serialize_path,
        owner = params.falcon_user,
        group = params.user_group,
        mode = 0775,
        recursive = True,
        cd_access = "a")

  if type == 'server':
    if action == 'config':
      if params.store_uri[0:4] == "hdfs":
        params.HdfsResource(params.store_uri,
          type = "directory",
          action = "create_on_execute",
          owner = params.falcon_user,
          mode = 0755)
      elif params.store_uri[0:4] == "file":
        Directory(params.store_uri[7:],
          owner = params.falcon_user,
          recursive = True)

      # TODO change to proper mode
      params.HdfsResource(params.falcon_apps_dir,
        type = "directory",
        action = "create_on_execute",
        owner = params.falcon_user,
        mode = 0777)

      if params.falcon_store_uri[0:4] == "hdfs":
        params.HdfsResource(params.falcon_store_uri,
          type = "directory",
          action = "create_on_execute",
          owner = params.falcon_user,
          mode = 0755)
      elif params.falcon_store_uri[0:4] == "file":
        Directory(params.falcon_store_uri[7:],
          owner = params.falcon_user,
          recursive = True)

      if params.supports_hive_dr:
        params.HdfsResource(params.dfs_data_mirroring_dir,
          type = "directory",
          action = "create_on_execute",
          owner = params.falcon_user,
          group = params.proxyuser_group,
          recursive_chown = True,
          recursive_chmod = True,
          mode = 0770,
          source = params.local_data_mirroring_dir)

      params.HdfsResource(None, action = "execute")
      Directory(params.falcon_local_dir,
        owner = params.falcon_user,
        recursive = True,
        cd_access = "a")

      if params.falcon_embeddedmq_enabled == True:
        Directory(
          os.path.abspath(os.path.join(params.falcon_embeddedmq_data, "..")),
          owner = params.falcon_user,
          recursive = True)

        Directory(params.falcon_embeddedmq_data,
          owner = params.falcon_user,
          recursive = True)

    # although Falcon's falcon-config.sh will use 'which hadoop' to figure
    # this out, in an upgraded cluster, it's possible that 'which hadoop'
    # still points to older binaries; it's safer to just pass in the
    # hadoop home directory to use
    environment_dictionary = { "HADOOP_HOME" : params.hadoop_home_dir }

    if action == 'start':
      Execute(format('{falcon_home}/bin/falcon-start -port {falcon_port}'),
        user = params.falcon_user,
        path = params.hadoop_bin_dir,
        environment=environment_dictionary)

    if action == 'stop':
      Execute(format('{falcon_home}/bin/falcon-stop'),
        user = params.falcon_user,
        path = params.hadoop_bin_dir,
        environment=environment_dictionary)

      File(params.server_pid_file, action = 'delete')


@OsFamilyFuncImpl(os_family = OSConst.WINSRV_FAMILY)
def falcon(type, action = None, upgrade_type=None):
  import params

  if action == 'config':
    env = Environment.get_instance()
    # These 2 parameters are used in ../templates/client.properties.j2
    env.config.params["falcon_host"] = params.falcon_host
    env.config.params["falcon_port"] = params.falcon_port
    File(os.path.join(params.falcon_conf_dir, 'falcon-env.sh'),
      content = InlineTemplate(params.falcon_env_sh_template))

    File(os.path.join(params.falcon_conf_dir, 'client.properties'),
      content = Template('client.properties.j2'))

    PropertiesFile(os.path.join(params.falcon_conf_dir, 'runtime.properties'),
      properties = params.falcon_runtime_properties)

    PropertiesFile(os.path.join(params.falcon_conf_dir, 'startup.properties'),
      properties = params.falcon_startup_properties)

  if type == 'server':
    ServiceConfig(params.falcon_win_service_name,
      action = "change_user",
      username = params.falcon_user,
      password = Script.get_password(params.falcon_user))

    if action == 'start':
      Service(params.falcon_win_service_name, action = "start")

    if action == 'stop':
      Service(params.falcon_win_service_name, action = "stop")