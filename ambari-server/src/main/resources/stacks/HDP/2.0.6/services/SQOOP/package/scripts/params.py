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

_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')
smokeuser = config['configurations']['global']['smokeuser']
user_group = config['configurations']['global']['user_group']

sqoop_conf_dir = "/usr/lib/sqoop/conf"
hbase_home = "/usr"
hive_home = "/usr"
zoo_conf_dir = "/etc/zookeeper"
sqoop_lib = "/usr/lib/sqoop/lib"
sqoop_user = "sqoop"

keytab_path = config['configurations']['global']['keytab_path']
smoke_user_keytab = config['configurations']['global']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
