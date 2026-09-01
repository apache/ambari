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

import functools
import os

import tez_utils
from resource_management.core.exceptions import Fail
from resource_management.core.shell import quote_bash_args
from resource_management.libraries.resources import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import (
  get_not_managed_resources,
)
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.copy_tarball import (
  get_sysprep_skip_copy_tarballs_hdfs,
)

# server configurations
config = Script.get_config()
tmp_dir = tez_utils.validate_absolute_path(Script.get_tmp_dir(), "agent temp directory")

stack_name = config["clusterLevelParams"]["stack_name"]
stack_root = tez_utils.validate_absolute_path(
  Script.get_stack_root(), "stack root directory"
)

# This is expected to be of the form #.#.#.#
stack_version_unformatted = config["clusterLevelParams"]["stack_version"]
stack_version_formatted = format_stack_version(stack_version_unformatted)
tez_utils.validate_bigtop_stack(stack_name, stack_version_formatted)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)
if version is not None:
  tez_utils.validate_bigtop_stack(stack_name, format_stack_version(version))

component_directory = stack_select.get_package_name(default_package="tez-client")

# default hadoop parameters
hadoop_home = tez_utils.validate_absolute_path(
  stack_select.get_hadoop_dir("home"), "Hadoop home directory"
)
hadoop_bin_dir = tez_utils.validate_absolute_path(
  stack_select.get_hadoop_dir("bin"), "Hadoop binary directory"
)
hadoop_conf_dir = tez_utils.validate_absolute_path(
  conf_select.get_hadoop_conf_dir(), "Hadoop configuration directory"
)
tez_home = "/usr/lib/tez"
tez_conf_dir = "/etc/tez/conf"

# hadoop parameters for stacks that support rolling_upgrade
if stack_version_formatted and check_stack_feature(
  StackFeature.ROLLING_UPGRADE, stack_version_formatted
):
  tez_home = format("{stack_root}/current/{component_directory}")
tez_home = tez_utils.validate_absolute_path(tez_home, "Tez home directory")

tez_examples_jar_pattern = format("{tez_home}/tez-examples*.jar")

# Heap dump related
heap_dump_enabled = tez_utils.as_bool(
  default("/configurations/tez-env/enable_heap_dump", False),
  "tez-env/enable_heap_dump",
)
heap_dump_opts = ""  # Empty if 'heap_dump_enabled' is False.
if heap_dump_enabled:
  heap_dump_path = tez_utils.validate_absolute_path(
    default("/configurations/tez-env/heap_dump_location", "/tmp"),
    "tez-env/heap_dump_location",
  )
  heap_dump_opts = " -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=" + heap_dump_path

kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
security_enabled = tez_utils.as_bool(
  config["configurations"]["cluster-env"]["security_enabled"],
  "cluster-env/security_enabled",
)
smokeuser = tez_utils.validate_user(
  config["configurations"]["cluster-env"]["smokeuser"],
  "cluster-env/smokeuser",
)
smokeuser_principal = config["configurations"]["cluster-env"][
  "smokeuser_principal_name"
]
smoke_user_keytab = config["configurations"]["cluster-env"]["smokeuser_keytab"]
if security_enabled:
  smokeuser_principal = tez_utils.validate_principal(
    smokeuser_principal, "cluster-env/smokeuser_principal_name"
  )
  tez_utils.validate_absolute_path(
    smoke_user_keytab, "cluster-env/smokeuser_keytab"
  )
hdfs_user = tez_utils.validate_user(
  config["configurations"]["hadoop-env"]["hdfs_user"], "hadoop-env/hdfs_user"
)
hdfs_principal_name = config["configurations"]["hadoop-env"]["hdfs_principal_name"]
hdfs_user_keytab = config["configurations"]["hadoop-env"]["hdfs_user_keytab"]
if security_enabled:
  hdfs_principal_name = tez_utils.validate_principal(
    hdfs_principal_name, "hadoop-env/hdfs_principal_name"
  )
  tez_utils.validate_absolute_path(hdfs_user_keytab, "hadoop-env/hdfs_user_keytab")

java64_home = tez_utils.validate_absolute_path(
  config["ambariLevelParams"]["java_home"], "ambariLevelParams/java_home"
)
tez_conf_dir_shell = quote_bash_args(tez_conf_dir)
hadoop_home_shell = quote_bash_args(hadoop_home)
java64_home_shell = quote_bash_args(java64_home)

user_group = tez_utils.validate_user(
  config["configurations"]["cluster-env"]["user_group"],
  "cluster-env/user_group",
)
tez_env_sh_template = config["configurations"]["tez-env"]["content"]
if not isinstance(tez_env_sh_template, str) or not tez_env_sh_template.strip():
  raise Fail("tez-env/content must contain the managed tez-env.sh template")

tez_lib_base_dir_path = os.path.join(
  "/", stack_name.lower(), "apps", stack_version_formatted, "tez"
)
tez_lib_uris = os.path.join(tez_lib_base_dir_path, "tez.tar.gz")
hdfs_site = config["configurations"]["hdfs-site"]
default_fs = config["configurations"]["core-site"]["fs.defaultFS"]

dfs_type = default("/clusterLevelParams/dfs_type", "")

# create partial functions with common arguments for every HdfsResource call
# to create/delete/copyfromlocal hdfs directories/files we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file="/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled=security_enabled,
  keytab=hdfs_user_keytab,
  kinit_path_local=kinit_path_local,
  hadoop_bin_dir=hadoop_bin_dir,
  hadoop_conf_dir=hadoop_conf_dir,
  principal_name=hdfs_principal_name,
  hdfs_site=hdfs_site,
  default_fs=default_fs,
  immutable_paths=get_not_managed_resources(),
  dfs_type=dfs_type,
)

tez_site_config = dict(config["configurations"]["tez-site"])
sysprep_skip_copy_tarballs_hdfs = get_sysprep_skip_copy_tarballs_hdfs()
