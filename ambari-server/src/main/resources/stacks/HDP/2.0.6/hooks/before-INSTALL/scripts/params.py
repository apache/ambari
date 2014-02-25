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
from resource_management.core.system import System
import os

config = Script.get_config()

#users and groups
yarn_user = config['configurations']['global']['yarn_user']
hbase_user = config['configurations']['global']['hbase_user']
nagios_user = config['configurations']['global']['nagios_user']
oozie_user = config['configurations']['global']['oozie_user']
webhcat_user = config['configurations']['global']['hcat_user']
hcat_user = config['configurations']['global']['hcat_user']
hive_user = config['configurations']['global']['hive_user']
smoke_user =  config['configurations']['global']['smokeuser']
mapred_user = config['configurations']['global']['mapred_user']
hdfs_user = config['configurations']['global']['hdfs_user']
zk_user = config['configurations']['global']['zk_user']
gmetad_user = config['configurations']['global']["gmetad_user"]
gmond_user = config['configurations']['global']["gmond_user"]
storm_user = config['configurations']['global']['storm_user']
tez_user = 'tez'
falcon_user = config['configurations']['global']['falcon_user']

user_group = config['configurations']['global']['user_group']
proxyuser_group =  config['configurations']['global']['proxyuser_group']
nagios_group = config['configurations']['global']['nagios_group']
smoke_user_group =  "users"
mapred_tt_group = default("/configurations/mapred-site/mapreduce.tasktracker.group", user_group)

#hosts
hostname = config["hostname"]
rm_host = default("/clusterHostInfo/rm_host", [])
slave_hosts = default("/clusterHostInfo/slave_hosts", [])
hagios_server_hosts = default("/clusterHostInfo/nagios_server_host", [])
oozie_servers = default("/clusterHostInfo/oozie_server", [])
hcat_server_hosts = default("/clusterHostInfo/webhcat_server_host", [])
hive_server_host =  default("/clusterHostInfo/hive_server_host", [])
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
hs_host = default("/clusterHostInfo/hs_host", [])
jtnode_host = default("/clusterHostInfo/jtnode_host", [])
namenode_host = default("/clusterHostInfo/namenode_host", [])
zk_hosts = default("/clusterHostInfo/zookeeper_hosts", [])
ganglia_server_hosts = default("/clusterHostInfo/ganglia_server_host", [])
storm_server_hosts = default("/clusterHostInfo/nimbus_hosts", [])
falcon_host =  default('/clusterHostInfo/falcon_server_hosts', [])

has_resourcemanager = not len(rm_host) == 0
has_slaves = not len(slave_hosts) == 0
has_nagios = not len(hagios_server_hosts) == 0
has_oozie_server = not len(oozie_servers)  == 0
has_hcat_server_host = not len(hcat_server_hosts)  == 0
has_hive_server_host = not len(hive_server_host)  == 0
has_hbase_masters = not len(hbase_master_hosts) == 0
has_zk_host = not len(zk_hosts) == 0
has_ganglia_server = not len(ganglia_server_hosts) == 0
has_storm_server = not len(storm_server_hosts) == 0
has_falcon_server = not len(falcon_host) == 0

is_namenode_master = hostname in namenode_host
is_jtnode_master = hostname in jtnode_host
is_rmnode_master = hostname in rm_host
is_hsnode_master = hostname in hs_host
is_hbase_master = hostname in hbase_master_hosts
is_slave = hostname in slave_hosts
if has_ganglia_server:
  ganglia_server_host = ganglia_server_hosts[0]

hbase_tmp_dir = config['configurations']['hbase-site']['hbase.tmp.dir']
