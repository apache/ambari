#!/usr/bin/env python
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

from resource_management.libraries.resources import HdfsResource
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.get_architecture import get_architecture

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

architecture = get_architecture()

stack_name = default("/hostLevelParams/stack_name", None)
stack_root = Script.get_stack_root()

# This is expected to be of the form #.#.#.#
stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

# New Cluster Stack Version that is defined during the RESTART of a Rolling Upgrade
version = default("/commandParams/version", None)

# default hadoop parameters
hadoop_home = stack_select.get_hadoop_dir("home")
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")
hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
tez_etc_dir = "/etc/tez"
config_dir = "/etc/tez/conf"
tez_examples_jar = "/usr/lib/tez/tez-mapreduce-examples*.jar"

# hadoop parameters for stacks that support rolling_upgrade
if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
  tez_examples_jar = "{stack_root}/current/tez-client/tez-examples*.jar"

# tez only started linking <stack-root>/x.x.x.x/tez-client/conf in config_versioning
if stack_version_formatted and check_stack_feature(StackFeature.CONFIG_VERSIONING, stack_version_formatted):
  # !!! use realpath for now since the symlink exists but is broken and a
  # broken symlink messes with the DirectoryProvider class
  config_path = os.path.join(stack_root, "current/tez-client/conf")
  config_dir = os.path.realpath(config_path)

# Heap dump related
heap_dump_enabled = default('/configurations/tez-env/enable_heap_dump', None)
heap_dump_opts = "" # Empty if 'heap_dump_enabled' is False.
if heap_dump_enabled:
  heap_dump_path = default('/configurations/tez-env/heap_dump_location', "/tmp")
  heap_dump_opts = " -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="+heap_dump_path

kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']

java64_home = config['hostLevelParams']['java_home']

tez_user = config['configurations']['tez-env']['tez_user']
user_group = config['configurations']['cluster-env']['user_group']
tez_env_sh_template = config['configurations']['tez-env']['content']

hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

dfs_type = default("/commandParams/dfs_type", "")

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete/copyfromlocal hdfs directories/files we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs,
  immutable_paths = get_not_managed_resources(),
  dfs_type = dfs_type
)



