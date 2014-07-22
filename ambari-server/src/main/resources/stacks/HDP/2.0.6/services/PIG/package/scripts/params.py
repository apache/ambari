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

pig_conf_dir = "/etc/pig/conf"
hadoop_conf_dir = "/etc/hadoop/conf"
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
smokeuser = config['configurations']['hadoop-env']['smokeuser']
user_group = config['configurations']['hadoop-env']['user_group']
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')
smoke_user_keytab = config['configurations']['hadoop-env']['smokeuser_keytab']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
pig_env_sh_template = config['configurations']['pig-env']['content']

# not supporting 32 bit jdk.
java64_home = config['hostLevelParams']['java_home']
hadoop_home = "/usr"

# pig.properties - if not in the JSON command, then we need to esnure some 
# basic properties are set; this is a safety mechanism
if (('pig-properties' in config['configurations']) and ('pig-content' in config['configurations']['pig-properties'])):
  pig_properties = config['configurations']['pig-properties']['pig-content']
else:
  pig_properties = """hcat.bin=/usr/bin/hcat
pig.location.check.strict=false"""

# log4j.properties
if (('pig-log4j' in config['configurations']) and ('content' in config['configurations']['pig-log4j'])):
  log4j_props = config['configurations']['pig-log4j']['content']
else:
  log4j_props = None
