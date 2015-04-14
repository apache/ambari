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

Ambari Agent

"""

from resource_management import *

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

#RPM versioning support
rpm_version = default("/configurations/cluster-env/rpm_version", None)

#hadoop params
if rpm_version:
  hadoop_bin_dir = "/usr/bigtop/current/hadoop-client/bin"
  hadoop_home = '/usr/bigtop/current/hadoop-client'
  pig_bin_dir = '/usr/bigtop/current/pig-client/bin'
else:
  hadoop_bin_dir = "/usr/bin"
  hadoop_home = '/usr'
  pig_bin_dir = ""

hadoop_conf_dir = "/etc/hadoop/conf"
pig_conf_dir = "/etc/pig/conf"
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
smokeuser = config['configurations']['cluster-env']['smokeuser']
user_group = config['configurations']['cluster-env']['user_group']
security_enabled = config['configurations']['cluster-env']['security_enabled']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
pig_env_sh_template = config['configurations']['pig-env']['content']

# not supporting 32 bit jdk.
java64_home = config['hostLevelParams']['java_home']

pig_properties = config['configurations']['pig-properties']['content']

log4j_props = config['configurations']['pig-log4j']['content']
