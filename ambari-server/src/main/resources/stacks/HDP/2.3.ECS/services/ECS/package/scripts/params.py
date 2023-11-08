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

from resource_management.libraries.functions.version import format_stack_version, compare_versions
from resource_management import *
import os
import itertools
import re
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import stack_select

config = Script.get_config()

jdk_name = default("/ambariLevelParams/jdk_name", None)
java_home = config['ambariLevelParams']['java_home']
java_version = int(config['ambariLevelParams']['java_version'])
jdk_location = config['ambariLevelParams']['jdk_location']

#hadoop_conf_dir = "/etc/hadoop/conf"
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
kinit_path_local = functions.get_kinit_path()

stack_version_unformatted = str(config['clusterLevelParams']['stack_version'])
stack_version_formatted = format_stack_version(stack_version_unformatted)
hadoop_bin_dir = stack_select.get_hadoop_dir("bin")

smoke_user =  config['configurations']['cluster-env']['smokeuser']
smoke_hdfs_user_dir = format("/user/{smoke_user}")
smoke_hdfs_user_mode = 0o770

java64_home = config['ambariLevelParams']['java_home']
java_version = int(config['ambariLevelParams']['java_version'])

hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
security_enabled = config['configurations']['cluster-env']['security_enabled']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hadoop_dir = "/etc/hadoop"
user_group = config['configurations']['cluster-env']['user_group']
hadoop_env_sh_template = config['configurations']['hadoop-env']['content']
tmp_dir = Script.get_tmp_dir()
hadoop_java_io_tmpdir = os.path.join(tmp_dir, "hadoop_java_io_tmpdir")

hdfs_tmp_dir = config['configurations']['hadoop-env']['hdfs_tmp_dir']

hdfs_principal_name = default('/configurations/hadoop-env/hdfs_principal_name', None)
hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
dfs_type = default("/clusterLevelParams/dfs_type", "")

ambari_libs_dir = "/var/lib/ambari-agent/lib"

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete/copyfromlocal hdfs directories/files we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
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
