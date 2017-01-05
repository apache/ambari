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
import traceback

# Local Imports
from resource_management.core.environment import Environment
from resource_management.core.source import InlineTemplate
from resource_management.core.source import Template
from resource_management.core.source import  DownloadSource
from resource_management.core.resources import Execute
from resource_management.core.resources.service import Service
from resource_management.core.resources.service import ServiceConfig
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import File
from resource_management.libraries.functions import get_user_call_output
from resource_management.libraries.script import Script
from resource_management.libraries.resources import PropertiesFile
from resource_management.libraries.functions import format
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.functions import get_user_call_output
from resource_management.libraries.functions.setup_atlas_hook import has_atlas_in_cluster, setup_atlas_hook, install_atlas_hook_packages, setup_atlas_jar_symlinks
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.setup_atlas_hook import has_atlas_in_cluster, setup_atlas_hook, install_atlas_hook_packages, setup_atlas_jar_symlinks
from resource_management.libraries.functions.stack_features import check_stack_feature
from ambari_commons.constants import SERVICE
from resource_management.core.logger import Logger
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl


@OsFamilyFuncImpl(os_family = OsFamilyImpl.DEFAULT)
def falcon(type, action = None, upgrade_type=None):
  import params

  if action == 'config':
    Directory(params.falcon_pid_dir,
      owner = params.falcon_user,
      create_parents = True,
      mode = 0755,
      cd_access = "a",
    )

    Directory(params.falcon_log_dir,
      owner = params.falcon_user,
      create_parents = True,
      mode = 0755,
      cd_access = "a",
    )

    Directory(params.falcon_webapp_dir,
      owner = params.falcon_user,
      create_parents = True)

    Directory(params.falcon_home,
      owner = params.falcon_user,
      create_parents = True)

    Directory(params.etc_prefix_dir,
      mode = 0755,
      create_parents = True)

    Directory(params.falcon_conf_dir,
      owner = params.falcon_user,
      create_parents = True)

    File(params.falcon_conf_dir + '/falcon-env.sh',
      content = InlineTemplate(params.falcon_env_sh_template),
      owner = params.falcon_user,
      group=params.user_group,
    )

    PropertiesFile(params.falcon_conf_dir + '/client.properties',
      properties = params.falcon_client_properties,
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

    File(params.falcon_conf_dir + '/log4j.properties',
      content = InlineTemplate(params.falcon_log4j),
      group = params.user_group,
      mode = 0644,
      owner = params.falcon_user)

    if params.falcon_graph_storage_directory:
      Directory(params.falcon_graph_storage_directory,
        owner = params.falcon_user,
        group = params.user_group,
        mode = 0775,
        create_parents = True,
        cd_access = "a")

    if params.falcon_graph_serialize_path:
      Directory(params.falcon_graph_serialize_path,
        owner = params.falcon_user,
        group = params.user_group,
        mode = 0775,
        create_parents = True,
        cd_access = "a")

    # Generate atlas-application.properties.xml file
    if params.falcon_atlas_support and params.enable_atlas_hook:
      # If Atlas is added later than Falcon, this package will be absent.
      install_atlas_hook_packages(params.atlas_plugin_package, params.atlas_ubuntu_plugin_package, params.host_sys_prepped,
                                  params.agent_stack_retry_on_unavailability, params.agent_stack_retry_count)

      atlas_hook_filepath = os.path.join(params.falcon_conf_dir, params.atlas_hook_filename)
      setup_atlas_hook(SERVICE.FALCON, params.falcon_atlas_application_properties, atlas_hook_filepath, params.falcon_user, params.user_group)

      # Falcon 0.10 uses FALCON_EXTRA_CLASS_PATH.
      # Setup symlinks for older versions.
      if params.current_version_formatted and check_stack_feature(StackFeature.FALCON_ATLAS_SUPPORT_2_3, params.current_version_formatted):
        setup_atlas_jar_symlinks("falcon", params.falcon_webinf_lib)

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
          create_parents = True)

      # TODO change to proper mode
      params.HdfsResource(params.falcon_apps_dir,
        type = "directory",
        action = "create_on_execute",
        owner = params.falcon_user,
        mode = 0777)

      # In HDP 2.4 and earlier, the data-mirroring directory was copied to HDFS.
      if params.supports_data_mirroring:
        params.HdfsResource(params.dfs_data_mirroring_dir,
          type = "directory",
          action = "create_on_execute",
          owner = params.falcon_user,
          group = params.proxyuser_group,
          recursive_chown = True,
          recursive_chmod = True,
          mode = 0770,
          source = params.local_data_mirroring_dir)

      # Falcon Extensions were supported in HDP 2.5 and higher.
      effective_version = params.stack_version_formatted if upgrade_type is None else format_stack_version(params.version)
      supports_falcon_extensions = effective_version and check_stack_feature(StackFeature.FALCON_EXTENSIONS, effective_version)

      if supports_falcon_extensions:
        params.HdfsResource(params.falcon_extensions_dest_dir,
                            type = "directory",
                            action = "create_on_execute",
                            owner = params.falcon_user,
                            group = params.proxyuser_group,
                            recursive_chown = True,
                            recursive_chmod = True,
                            mode = 0755,
                            source = params.falcon_extensions_source_dir)
        # Create the extensons HiveDR store
        params.HdfsResource(os.path.join(params.falcon_extensions_dest_dir, "mirroring"),
                            type = "directory",
                            action = "create_on_execute",
                            owner = params.falcon_user,
                            group = params.proxyuser_group,
                            mode = 0770)

      # At least one HDFS Dir should be created, so execute the change now.
      params.HdfsResource(None, action = "execute")

      Directory(params.falcon_local_dir,
        owner = params.falcon_user,
        create_parents = True,
        cd_access = "a")

      if params.falcon_embeddedmq_enabled == True:
        Directory(
          os.path.abspath(os.path.join(params.falcon_embeddedmq_data, "..")),
          owner = params.falcon_user,
          create_parents = True)

        Directory(params.falcon_embeddedmq_data,
          owner = params.falcon_user,
          create_parents = True)

    # although Falcon's falcon-config.sh will use 'which hadoop' to figure
    # this out, in an upgraded cluster, it's possible that 'which hadoop'
    # still points to older binaries; it's safer to just pass in the
    # hadoop home directory to use
    environment_dictionary = { "HADOOP_HOME" : params.hadoop_home_dir }

    pid = get_user_call_output.get_user_call_output(format("cat {server_pid_file}"), user=params.falcon_user, is_checked_call=False)[1]
    process_exists = format("ls {server_pid_file} && ps -p {pid}")

    if action == 'start':
      try:
        Execute(format('{falcon_home}/bin/falcon-config.sh server falcon'),
          user = params.falcon_user,
          path = params.hadoop_bin_dir,
          environment=environment_dictionary,
          not_if = process_exists,
        )
      except:
        show_logs(params.falcon_log_dir, params.falcon_user)
        raise

      if not os.path.exists(params.target_jar_file):
        try :
          File(params.target_jar_file,
           content = DownloadSource(params.bdb_resource_name),
           mode = 0755)
        except :
           exc_msg = traceback.format_exc()
           exception_message = format("Caught Exception while downloading {bdb_resource_name}:\n{exc_msg}")
           Logger.error(exception_message)

        if not os.path.isfile(params.target_jar_file) :
          error_message = """
If you are using bdb as the Falcon graph db store, please run
ambari-server setup --jdbc-db=bdb --jdbc-driver=<path to je5.0.73.jar>
on the ambari server host.  Otherwise falcon startup will fail.
Otherwise please configure Falcon to use HBase as the backend as described
in the Falcon documentation.
"""
          Logger.error(error_message)
      try:
        Execute(format('{falcon_home}/bin/falcon-start -port {falcon_port}'),
          user = params.falcon_user,
          path = params.hadoop_bin_dir,
          environment=environment_dictionary,
          not_if = process_exists,
        )
      except:
        show_logs(params.falcon_log_dir, params.falcon_user)
        raise

    if action == 'stop':
      try:
        Execute(format('{falcon_home}/bin/falcon-stop'),
          user = params.falcon_user,
          path = params.hadoop_bin_dir,
          environment=environment_dictionary)
      except:
        show_logs(params.falcon_log_dir, params.falcon_user)
        raise
      
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

    PropertiesFile(os.path.join(params.falcon_conf_dir, 'runtime.properties'),
      properties = params.falcon_runtime_properties)

    PropertiesFile(os.path.join(params.falcon_conf_dir, 'startup.properties'),
      properties = params.falcon_startup_properties)

    PropertiesFile(os.path.join(params.falcon_conf_dir, 'client.properties'),
      properties = params.falcon_client_properties)

  if type == 'server':
    ServiceConfig(params.falcon_win_service_name,
      action = "change_user",
      username = params.falcon_user,
      password = Script.get_password(params.falcon_user))

    if action == 'start':
      Service(params.falcon_win_service_name, action = "start")

    if action == 'stop':
      Service(params.falcon_win_service_name, action = "stop")
