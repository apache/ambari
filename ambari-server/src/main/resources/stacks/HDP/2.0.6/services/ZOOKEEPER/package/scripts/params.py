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

from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management import *
import status_params

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

hdp_stack_version = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(hdp_stack_version)
stack_is_hdp22_or_further = hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0

#hadoop params
if stack_is_hdp22_or_further:
  zk_home = '/usr/hdp/current/zookeeper-client'
  zk_bin = '/usr/hdp/current/zookeeper-client/bin'
  smoke_script = '/usr/hdp/current/zookeeper-client/bin/zkCli.sh'
else:
  zk_home = '/usr'
  zk_bin = '/usr/lib/zookeeper/bin'
  smoke_script = "/usr/lib/zookeeper/bin/zkCli.sh"

config_dir = "/etc/zookeeper/conf"
zk_user =  config['configurations']['zookeeper-env']['zk_user']
hostname = config['hostname']
user_group = config['configurations']['cluster-env']['user_group']
zk_env_sh_template = config['configurations']['zookeeper-env']['content']

zk_log_dir = config['configurations']['zookeeper-env']['zk_log_dir']
zk_data_dir = config['configurations']['zoo.cfg']['dataDir']
zk_pid_dir = status_params.zk_pid_dir
zk_pid_file = status_params.zk_pid_file
zk_server_heapsize = "-Xmx1024m"

clientPort = config['configurations']['zoo.cfg']['clientPort']

if 'zoo.cfg' in config['configurations']:
  zoo_cfg_properties_map = config['configurations']['zoo.cfg']
else:
  zoo_cfg_properties_map = {}
zoo_cfg_properties_map_length = len(zoo_cfg_properties_map)

zk_principal_name = default("/configurations/zookeeper-env/zookeeper_principal_name", "zookeeper@EXAMPLE.COM")
zk_principal = zk_principal_name.replace('_HOST',hostname.lower())

java64_home = config['hostLevelParams']['java_home']

zookeeper_hosts = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_hosts.sort()

zk_keytab_path = config['configurations']['zookeeper-env']['zookeeper_keytab_path']
zk_server_jaas_file = format("{config_dir}/zookeeper_jaas.conf")
zk_client_jaas_file = format("{config_dir}/zookeeper_client_jaas.conf")
security_enabled = config['configurations']['cluster-env']['security_enabled']

smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
smokeuser = config['configurations']['cluster-env']['smokeuser']
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

#log4j.properties
if (('zookeeper-log4j' in config['configurations']) and ('content' in config['configurations']['zookeeper-log4j'])):
  log4j_props = config['configurations']['zookeeper-log4j']['content']
else:
  log4j_props = None
