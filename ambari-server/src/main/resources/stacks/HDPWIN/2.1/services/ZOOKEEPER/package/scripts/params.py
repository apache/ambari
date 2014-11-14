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

# notused zookeeper_home_dir = os.environ["ZOOKEEPER_HOME"]
zookeeper_conf_dir = os.environ["ZOOKEEPER_CONF_DIR"]
hdp_root = os.environ["HADOOP_NODE_INSTALL_ROOT"]
zk_user = "hadoop"

# notused zk_log_dir = config['configurations']['zookeeper-env']['zk_log_dir']
#todo zk_data_dir must be dynamically changed
zk_data_dir = "c:\\\\hadoop\\\\zookeeper"
zk_data_dir = config['configurations']['zookeeper-env']['zk_data_dir']

tickTime = config['configurations']['zookeeper-env']['tickTime']
initLimit = config['configurations']['zookeeper-env']['initLimit']
syncLimit = config['configurations']['zookeeper-env']['syncLimit']
clientPort = config['configurations']['zookeeper-env']['clientPort']

if 'zoo.cfg' in config['configurations']:
  zoo_cfg_properties_map = config['configurations']['zoo.cfg']
else:
  zoo_cfg_properties_map = {}
zoo_cfg_properties_map_length = len(zoo_cfg_properties_map)

zookeeper_hosts = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_hosts.sort()
hostname = config['hostname']

_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')

#log4j.properties
if (('zookeeper-log4j' in config['configurations']) and ('content' in config['configurations']['zookeeper-log4j'])):
  log4j_props = config['configurations']['zookeeper-log4j']['content']
else:
  log4j_props = None
