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

import sys
import os
import json
import tempfile
import hashlib
from datetime import datetime
import ambari_simplejson as json  # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

from ambari_commons import constants

from resource_management.libraries.resources.xml_config import XmlConfig

from resource_management.libraries.script.script import Script
from resource_management.core.resources.system import Execute, File
from resource_management.core import shell
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.format import format
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management.libraries.functions.security_commons import (
  build_expectations,
  cached_kinit_executor,
  get_params_from_filesystem,
  validate_security_config_properties,
  FILE_TYPE_XML,
)

from resource_management.core.exceptions import Fail
from resource_management.core.shell import as_user
from resource_management.core.logger import Logger


from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst

from hdfs_router import router


from hdfs import hdfs, reconfig
import hdfs_rebalance
from utils import (
  initiate_safe_zkfc_failover,
  get_hdfs_binary,
  get_dfsrouteradmin_base_command,
)
from resource_management.libraries.functions.namenode_ha_utils import (
  get_hdfs_cluster_id_from_jmx,
)

# The hash algorithm to use to generate digests/hashes
HASH_ALGORITHM = hashlib.sha224


class Router(Script):
  def get_hdfs_binary(self):
    """
    Get the name or path to the hdfs binary depending on the component name.
    """
    return get_hdfs_binary("hadoop-hdfs-dfsrouter")

  def install(self, env):
    import params

    env.set_params(params)
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params

    env.set_params(params)
    hdfs("router")
    hdfs_binary = self.get_hdfs_binary()
    router(action="configure", hdfs_binary=hdfs_binary, env=env)
    XmlConfig(
      "hdfs-site.xml",
      conf_dir=params.hadoop_conf_dir,
      configurations=params.router_hdfs_site,
      configuration_attributes=params.config["configurationAttributes"]["hdfs-site"],
      mode=0o644,
      owner=params.hdfs_user,
      group=params.user_group,
    )
    XmlConfig(
      "core-site.xml",
      conf_dir=params.hadoop_conf_dir,
      configurations=params.router_core_site,
      configuration_attributes=params.config["configurationAttributes"]["core-site"],
      mode=0o644,
      owner=params.hdfs_user,
      group=params.user_group,
    )

  def save_configs(self, env):
    import params

    env.set_params(params)
    hdfs()

  def reload_configs(self, env):
    import params

    env.set_params(params)
    Logger.info("RELOAD CONFIGS")
    reconfig("router", params.router_address)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    hdfs_binary = self.get_hdfs_binary()
    router(action="start", hdfs_binary=hdfs_binary, env=env)

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    hdfs_binary = self.get_hdfs_binary()
    router(action="stop", hdfs_binary=hdfs_binary, env=env)

  def status(self, env):
    import status_params

    env.set_params(status_params)
    router(action="status", env=env)

  def get_log_folder(self):
    import params

    return params.hdfs_log_dir

  def get_user(self):
    import params

    return params.hdfs_user

  def get_pid_files(self):
    import status_params

    return [status_params.router_pid_file]


def _print(line):
  sys.stdout.write(line)
  sys.stdout.flush()


if __name__ == "__main__":
  Router().execute()
