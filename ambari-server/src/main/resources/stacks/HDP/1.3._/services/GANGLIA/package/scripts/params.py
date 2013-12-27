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

config = Script.get_config()

user_group = config['configurations']['global']["user_group"]
ganglia_conf_dir = config['configurations']['global']["ganglia_conf_dir"]
ganglia_dir = "/etc/ganglia"
ganglia_runtime_dir = config['configurations']['global']["ganglia_runtime_dir"]
ganglia_shell_cmds_dir = "/usr/libexec/hdp/ganglia"

gmetad_user = config['configurations']['global']["gmetad_user"]
gmond_user = config['configurations']['global']["gmond_user"]

webserver_group = "apache"
rrdcached_default_base_dir = "/var/lib/ganglia/rrds"
rrdcached_base_dir = config['configurations']['global']["rrdcached_base_dir"]

ganglia_server_host = config["clusterHostInfo"]["ganglia_server_host"][0]

hostname = config["hostname"]
namenode_host = default("/clusterHostInfo/namenode_host", [])
jtnode_host = default("/clusterHostInfo/jtnode_host", [])
rm_host = default("/clusterHostInfo/rm_host", [])
hs_host = default("/clusterHostInfo/hs_host", [])
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts", [])
slave_hosts = default("/clusterHostInfo/slave_hosts", [])

is_namenode_master = hostname in namenode_host
is_jtnode_master = hostname in jtnode_host
is_rmnode_master = hostname in rm_host
is_hsnode_master = hostname in hs_host
is_hbase_master = hostname in hbase_master_hosts
is_slave = hostname in slave_hosts

has_namenodes = not len(namenode_host) == 0
has_jobtracker = not len(jtnode_host) == 0
has_resourcemanager = not len(rm_host) == 0
has_histroryserver = not len(hs_host) == 0
has_hbase_masters = not len(hbase_master_hosts) == 0
has_slaves = not len(slave_hosts) == 0

if System.get_instance().platform == "suse":
  rrd_py_path = '/srv/www/cgi-bin'
else:
  rrd_py_path = '/var/www/cgi-bin'
