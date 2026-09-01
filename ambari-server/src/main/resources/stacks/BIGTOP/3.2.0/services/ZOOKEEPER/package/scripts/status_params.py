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

import os

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.version import format_stack_version

import zookeeper_utils


config = Script.get_config()
zookeeper_utils.validate_bigtop_stack(
  config["clusterLevelParams"]["stack_name"],
  format_stack_version(config["clusterLevelParams"]["stack_version"]),
)

SERVER_ROLE_DIRECTORY_MAP = {
  "ZOOKEEPER_SERVER": "zookeeper-server",
  "ZOOKEEPER_CLIENT": "zookeeper-client",
}
component_directory = Script.get_component_from_role(
  SERVER_ROLE_DIRECTORY_MAP, "ZOOKEEPER_CLIENT"
)

zk_user = zookeeper_utils.validate_user(
  config["configurations"]["zookeeper-env"]["zk_user"],
  "zookeeper-env/zk_user",
)
user_group = zookeeper_utils.validate_user(
  config["configurations"]["cluster-env"]["user_group"],
  "cluster-env/user_group",
)
zk_pid_dir = zookeeper_utils.validate_service_directory(
  config["configurations"]["zookeeper-env"]["zk_pid_dir"],
  "zookeeper-env/zk_pid_dir",
)
zk_pid_file = os.path.join(zk_pid_dir, "zookeeper_server.pid")
config_dir = "/etc/zookeeper/conf"
zk_config_file = os.path.join(config_dir, "zoo.cfg")
