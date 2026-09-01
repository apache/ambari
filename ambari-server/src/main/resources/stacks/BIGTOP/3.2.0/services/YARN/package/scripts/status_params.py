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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import format
from functions import (
  parse_port,
  validate_runtime_directory_prefix,
  validate_unix_name,
)

config = Script.get_config()
root_user = "root"

mapred_user = validate_unix_name(
  config["configurations"]["mapred-env"]["mapred_user"],
  "mapred-env/mapred_user",
)
yarn_user = validate_unix_name(
  config["configurations"]["yarn-env"]["yarn_user"], "yarn-env/yarn_user"
)
yarn_ats_user = validate_unix_name(
  config["configurations"]["yarn-env"]["yarn_ats_user"],
  "yarn-env/yarn_ats_user",
)
user_group = validate_unix_name(
  config["configurations"]["cluster-env"]["user_group"],
  "cluster-env/user_group",
)
yarn_pid_dir_prefix = validate_runtime_directory_prefix(
  config["configurations"]["yarn-env"]["yarn_pid_dir_prefix"],
  "yarn-env/yarn_pid_dir_prefix",
)
mapred_pid_dir_prefix = validate_runtime_directory_prefix(
  config["configurations"]["mapred-env"]["mapred_pid_dir_prefix"],
  "mapred-env/mapred_pid_dir_prefix",
)
yarn_pid_dir = format("{yarn_pid_dir_prefix}/{yarn_user}")
mapred_pid_dir = format("{mapred_pid_dir_prefix}/{mapred_user}")

resourcemanager_pid_file = format(
  "{yarn_pid_dir}/hadoop-{yarn_user}-resourcemanager.pid"
)
nodemanager_pid_file = format("{yarn_pid_dir}/hadoop-{yarn_user}-nodemanager.pid")
yarn_historyserver_pid_file = format(
  "{yarn_pid_dir}/hadoop-{yarn_user}-timelineserver.pid"
)

# registry dns service
registry_dns_bind_port = parse_port(
  config["configurations"]["yarn-env"]["registry.dns.bind-port"],
  "yarn-env/registry.dns.bind-port",
)
registry_dns_needs_privileged_access = registry_dns_bind_port < 1024

yarn_registry_dns_pid_file = format(
  "{yarn_pid_dir_prefix}/{yarn_user}/hadoop-{yarn_user}-registrydns.pid"
)
yarn_registry_dns_secure_pid_file = format(
  "{yarn_pid_dir_prefix}/{root_user}/hadoop-{yarn_user}-{root_user}-registrydns.pid"
)
yarn_registry_dns_wrapper_pid_file = format(
  "{yarn_pid_dir_prefix}/{root_user}/privileged-{root_user}-registrydns.pid"
)

if registry_dns_needs_privileged_access:
  yarn_registry_dns_in_use_pid_file = yarn_registry_dns_secure_pid_file
else:
  yarn_registry_dns_in_use_pid_file = yarn_registry_dns_pid_file

mapred_historyserver_pid_file = format(
  "{mapred_pid_dir}/hadoop-{mapred_user}-historyserver.pid"
)

yarn_timelinereader_pid_file = format(
  "{yarn_pid_dir}/hadoop-{yarn_user}-timelinereader.pid"
)

hadoop_home = stack_select.get_hadoop_dir("home")

# ATSv2 backend properties
yarn_hbase_user = format("{yarn_ats_user}")  # Use yarn_ats_user.
yarn_hbase_pid_dir_prefix = validate_runtime_directory_prefix(
  config["configurations"]["yarn-hbase-env"]["yarn_hbase_pid_dir_prefix"],
  "yarn-hbase-env/yarn_hbase_pid_dir_prefix",
)
yarn_hbase_pid_dir = format("{yarn_hbase_pid_dir_prefix}/{yarn_hbase_user}")
