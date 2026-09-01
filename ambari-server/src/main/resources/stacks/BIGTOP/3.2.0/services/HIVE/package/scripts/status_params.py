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
import re

# Ambari Commons & Resource Management Imports
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.script.script import Script

config = Script.get_config()
stack_version_unformatted = config["clusterLevelParams"]["stack_version"]
stack_version_formatted_major = format_stack_version(stack_version_unformatted)

def validated_path(value, description):
  value = str(value or "")
  if not re.fullmatch(r"/[A-Za-z0-9._/+:-]+", value):
    raise Fail(f"{description} contains unsupported path characters")
  normalized = os.path.normpath(value)
  if normalized == "/" or normalized != value.rstrip("/"):
    raise Fail(f"{description} must be a normalized non-root absolute path")
  return normalized


hive_pid_dir = validated_path(
  config["configurations"]["hive-env"]["hive_pid_dir"], "Hive PID directory"
)
hive_pid = format("{hive_pid_dir}/hive-server.pid")
hive_metastore_pid = format("{hive_pid_dir}/hive.pid")

SERVICE_FILE_TEMPLATES = [
  "/etc/init.d/{0}",
  "/etc/systemd/system/{0}.service",
  "/lib/systemd/system/{0}.service",
  "/usr/lib/systemd/system/{0}.service",
]
POSSIBLE_DAEMON_NAMES = ["mariadb", "mysqld", "mysql"]

hive_user = config["configurations"]["hive-env"]["hive_user"]
webhcat_user = config["configurations"]["hive-env"]["webhcat_user"]
user_group = config["configurations"]["cluster-env"]["user_group"]

# hcat_pid_dir
hcat_pid_dir = validated_path(
  config["configurations"]["hive-env"]["hcat_pid_dir"], "WebHCat PID directory"
)
webhcat_pid_file = format("{hcat_pid_dir}/webhcat.pid")
