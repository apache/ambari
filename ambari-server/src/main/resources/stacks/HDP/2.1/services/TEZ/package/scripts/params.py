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

from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management import *

# server configurations
config = Script.get_config()

# This is expected to be of the form #.#.#.#
hdp_stack_version = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(hdp_stack_version)
stack_is_hdp22_or_further = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0

if stack_is_hdp22_or_further:
  hadoop_bin_dir = "/usr/hdp/current/hadoop-client/bin"
else:
  hadoop_bin_dir = "/usr/bin"
hadoop_conf_dir = "/etc/hadoop/conf"

kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
security_enabled = config['configurations']['cluster-env']['security_enabled']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']

config_dir = "/etc/tez/conf"

hadoop_home = '/usr'
java64_home = config['hostLevelParams']['java_home']

tez_user = config['configurations']['tez-env']['tez_user']
user_group = config['configurations']['cluster-env']['user_group']
tez_env_sh_template = config['configurations']['tez-env']['content']

import functools
# Create partial functions with common arguments for every HdfsDirectory call
# to create hdfs directory we need to call params.HdfsDirectory in code
HdfsDirectory = functools.partial(
  HdfsDirectory,
  conf_dir=hadoop_conf_dir,
  hdfs_user=hdfs_principal_name if security_enabled else hdfs_user,
  security_enabled=security_enabled,
  keytab=hdfs_user_keytab,
  kinit_path_local=kinit_path_local,
  bin_dir=hadoop_bin_dir
)
