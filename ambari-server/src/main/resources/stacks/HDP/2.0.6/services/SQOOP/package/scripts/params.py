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

config = Script.get_config()

hdp_stack_version = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(hdp_stack_version)
stack_is_hdp22_or_further = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0

#hadoop params
if stack_is_hdp22_or_further:
  sqoop_conf_dir = '/etc/sqoop/conf'
  sqoop_lib = '/usr/hdp/current/sqoop-client/lib'
  hadoop_home = '/usr/hdp/current/hbase-client'
  hbase_home = '/usr/hdp/current/hbase-client'
  hive_home = '/usr/hdp/current/hive-client'
  sqoop_bin_dir = '/usr/hdp/current/sqoop-client/bin/'
else:
  sqoop_conf_dir = "/usr/lib/sqoop/conf"
  sqoop_lib = "/usr/lib/sqoop/lib"
  hadoop_home = '/usr/lib/hadoop'
  hbase_home = "/usr/lib/hbase"
  hive_home = "/usr/lib/hive"
  sqoop_bin_dir = "/usr/bin"

zoo_conf_dir = "/etc/zookeeper"
security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser = config['configurations']['cluster-env']['smokeuser']
user_group = config['configurations']['cluster-env']['user_group']
sqoop_env_sh_template = config['configurations']['sqoop-env']['content']

sqoop_user = config['configurations']['sqoop-env']['sqoop_user']

smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
