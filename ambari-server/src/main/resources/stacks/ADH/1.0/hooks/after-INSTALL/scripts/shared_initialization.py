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

import ambari_simplejson as json
from resource_management.core.logger import Logger
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.fcntl_based_process_lock import FcntlBasedProcessLock
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.script import Script


def setup_stack_symlinks():
  """
  Invokes <stack-selector-tool> set all against a calculated fully-qualified, "normalized" version based on a
  stack version, such as "2.3". This should always be called after a component has been
  installed to ensure that all HDP pointers are correct. The stack upgrade logic does not
  interact with this since it's done via a custom command and will not trigger this hook.
  :return:
  """
  import params
  if params.stack_version_formatted != "" and compare_versions(params.stack_version_formatted, '2.2') >= 0:
    # try using the exact version first, falling back in just the stack if it's not defined
    # which would only be during an intial cluster installation
    version = params.current_version if params.current_version is not None else params.stack_version_unformatted

    if not params.upgrade_suspended:
      if params.host_sys_prepped:
        Logger.warning("Skipping running stack-selector-tool for stack {0} as its a sys_prepped host. This may cause symlink pointers not to be created for HDP componets installed later on top of an already sys_prepped host.".format(version))
        return
      # On parallel command execution this should be executed by a single process at a time.
      with FcntlBasedProcessLock(params.stack_select_lock_file, enabled = params.is_parallel_execution_enabled, skip_fcntl_failures = True):
        stack_select.select_all(version)

def setup_config():
  import params
  stackversion = params.stack_version_unformatted
  Logger.info("FS Type: {0}".format(params.dfs_type))

  is_hadoop_conf_dir_present = False
  if hasattr(params, "hadoop_conf_dir") and params.hadoop_conf_dir is not None and os.path.exists(params.hadoop_conf_dir):
    is_hadoop_conf_dir_present = True
  else:
    Logger.warning("Parameter hadoop_conf_dir is missing or directory does not exist. This is expected if this host does not have any Hadoop components.")

  if is_hadoop_conf_dir_present and (params.has_namenode or stackversion.find('Gluster') >= 0 or params.dfs_type == 'HCFS'):
    # create core-site only if the hadoop config diretory exists
    XmlConfig("core-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['core-site'],
              configuration_attributes=params.config['configuration_attributes']['core-site'],
              owner=params.hdfs_user,
              group=params.user_group,
              only_if=format("ls {hadoop_conf_dir}"))


def load_version(struct_out_file):
  """
  Load version from file.  Made a separate method for testing
  """
  json_version = None
  try:
    if os.path.exists(struct_out_file):
      with open(struct_out_file, 'r') as fp:
        json_info = json.load(fp)
        json_version = json_info['version']
  except:
    pass

  return json_version
  

def link_configs(struct_out_file):
  """
  Links configs, only on a fresh install of HDP-2.3 and higher
  """
  import params

  if not Script.is_stack_greater_or_equal("2.3"):
    Logger.info("Can only link configs for HDP-2.3 and higher.")
    return

  json_version = load_version(struct_out_file)

  if not json_version:
    Logger.info("Could not load 'version' from {0}".format(struct_out_file))
    return

  # On parallel command execution this should be executed by a single process at a time.
  with FcntlBasedProcessLock(params.link_configs_lock_file, enabled = params.is_parallel_execution_enabled, skip_fcntl_failures = True):
    for k, v in conf_select.get_package_dirs().iteritems():
      conf_select.convert_conf_directories_to_symlinks(k, json_version, v)