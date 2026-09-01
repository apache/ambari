#!/usr/bin/env python3
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

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.show_logs import show_logs

import zookeeper_process
import zookeeper_utils


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def zookeeper_service(action="start", upgrade_type=None):
  if action == "start":
    import params

    identity = zookeeper_process.read_or_recover_process(
      params.zk_pid_file,
      params.zk_user,
      params.user_group,
      params.zk_config_file,
    )
    if identity is not None:
      Logger.info(f"ZooKeeper Server is already running with pid {identity.pid}")
      return

    if params.security_enabled:
      zookeeper_utils.validate_keytab(
        params.zk_keytab_path, "ZooKeeper server keytab"
      )
    zookeeper_utils.validate_executable(
      params.zk_server_script, "ZooKeeper server launcher"
    )

    try:
      Execute(
        (params.zk_server_script, "start-foreground"),
        user=params.zk_user,
        environment={
          "JAVA_HOME": params.java64_home,
          "ZOOCFGDIR": params.config_dir,
          "ZOOCFG": "zoo.cfg",
        },
        wait_for_finish=False,
      )
      zookeeper_process.wait_for_started_process(
        params.zk_pid_file,
        params.zk_user,
        params.user_group,
        params.zk_config_file,
      )
    except Exception:
      show_logs(params.zk_log_dir, params.zk_user)
      raise
    return

  if action == "stop":
    import status_params as params

    stopped = zookeeper_process.stop_process(
      params.zk_pid_file,
      params.zk_user,
      params.user_group,
      params.zk_config_file,
    )
    if not stopped:
      Logger.info("ZooKeeper Server is not running")
    return

  raise Fail(f"Unsupported ZooKeeper service action: {action}")
