#!/usr/bin/env python2.6
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
import status_params

# server configurations
config = Script.get_config()

config_dir = "/etc/zookeeper/conf"
zk_user =  config['configurations']['global']['zk_user']
hostname = config['hostname']
zk_bin = '/usr/lib/zookeeper/bin'
user_group = config['configurations']['global']['user_group']

smoke_script = "/usr/lib/zookeeper/bin/zkCli.sh"

zk_log_dir = config['configurations']['global']['zk_log_dir']
zk_data_dir = config['configurations']['global']['zk_data_dir']
zk_pid_dir = status_params.zk_pid_dir
zk_pid_file = status_params.zk_pid_file
zk_server_heapsize = "-Xmx1024m"

tickTime = config['configurations']['global']['tickTime']
initLimit = config['configurations']['global']['initLimit']
syncLimit = config['configurations']['global']['syncLimit']
clientPort = config['configurations']['global']['clientPort']

if 'zoo.cfg' in config['configurations']:
  zoo_cfg_properties_map = config['configurations']['zoo.cfg']
else:
  zoo_cfg_properties_map = {}
zoo_cfg_properties_map_length = len(zoo_cfg_properties_map)

zk_principal_name = default("zookeeper_principal_name", "zookeeper@EXAMPLE.COM")
zk_principal = zk_principal_name.replace('_HOST',hostname.lower())

java64_home = config['hostLevelParams']['java_home']

zookeeper_hosts = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_hosts.sort()

keytab_path = "/etc/security/keytabs"
zk_keytab_path = format("{keytab_path}/zk.service.keytab")
zk_server_jaas_file = format("{config_dir}/zookeeper_jaas.conf")
zk_client_jaas_file = format("{config_dir}/zookeeper_client_jaas.conf")
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')

smoke_user_keytab = config['configurations']['global']['smokeuser_keytab']
smokeuser = config['configurations']['global']['smokeuser']
kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

#log4j.properties
if (('zookeeper-log4j' in config['configurations']) and ('content' in config['configurations']['zookeeper-log4j'])):
  log4j_props = config['configurations']['zookeeper-log4j']['content']
else:
  log4j_props = None
