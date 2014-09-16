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

from resource_management import *

config = Script.get_config()

#RPM versioning support
rpm_version = default("/configurations/hadoop-env/rpm_version", None)

#hadoop params
if rpm_version is not None:
  zoo_conf_dir = format('/usr/hdp/{rpm_version}/etc/zookeeper')
  sqoop_conf_dir = format('/usr/hdp/{rpm_version}/sqoop/conf')
  sqoop_lib = format('/usr/hdp/{rpm_version}/sqoop/lib')
  hbase_home = format('/usr/hdp/{rpm_version}/hbase')
  hive_home = format('/usr/hdp/{rpm_version}/hive')
else:
  zoo_conf_dir = "/etc/zookeeper"
  sqoop_conf_dir = "/usr/lib/sqoop/conf"
  sqoop_lib = "/usr/lib/sqoop/lib"
  hbase_home = "/usr"
  hive_home = "/usr"

security_enabled = config['configurations']['cluster-env']['security_enabled']
smokeuser = config['configurations']['cluster-env']['smokeuser']
user_group = config['configurations']['cluster-env']['user_group']
sqoop_env_sh_template = config['configurations']['sqoop-env']['content']

sqoop_user = config['configurations']['sqoop-env']['sqoop_user']

smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
