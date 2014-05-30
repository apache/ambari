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
ganglia_conf_dir = default("/configurations/global/ganglia_conf_dir", "/etc/ganglia/hdp")
ganglia_dir = "/etc/ganglia"
ganglia_runtime_dir = config['configurations']['global']["ganglia_runtime_dir"]
ganglia_shell_cmds_dir = "/usr/libexec/hdp/ganglia"

gmetad_user = config['configurations']['global']["gmetad_user"]
gmond_user = config['configurations']['global']["gmond_user"]

gmond_app_str = default("/configurations/global/enabled_app_servers", None)
gmond_apps = [] if gmond_app_str is None else gmond_app_str.split(',')
gmond_apps = [x.strip() for x in gmond_apps]
gmond_allowed_apps = ["Application1", "Application2", "Application3"]
gmond_apps = set(gmond_apps) & set(gmond_allowed_apps)

webserver_group = "apache"
rrdcached_default_base_dir = "/var/lib/ganglia/rrds"
rrdcached_base_dir = config['configurations']['global']["rrdcached_base_dir"]
rrdcached_timeout = default("/configurations/global/rrdcached_timeout", 3600)
rrdcached_delay = default("/configurations/global/rrdcached_delay", 1800)
rrdcached_write_threads = default("/configurations/global/rrdcached_write_threads", 10)

ganglia_server_host = config["clusterHostInfo"]["ganglia_server_host"][0]

hostname = config["hostname"]
namenode_host = set(default("/clusterHostInfo/namenode_host", []))
jtnode_host = set(default("/clusterHostInfo/jtnode_host", []))
rm_host = set(default("/clusterHostInfo/rm_host", []))
hs_host = set(default("/clusterHostInfo/hs_host", []))
hbase_master_hosts = set(default("/clusterHostInfo/hbase_master_hosts", []))
# datanodes are marked as slave_hosts
slave_hosts = set(default("/clusterHostInfo/slave_hosts", []))
tt_hosts = set(default("/clusterHostInfo/mapred_tt_hosts", []))
nm_hosts = set(default("/clusterHostInfo/nm_hosts", []))
hbase_rs_hosts = set(default("/clusterHostInfo/hbase_rs_hosts", []))
flume_hosts = set(default("/clusterHostInfo/flume_hosts", []))
jn_hosts = set(default("/clusterHostInfo/journalnode_hosts", []))
nimbus_server_hosts = set(default("/clusterHostInfo/nimbus_hosts", []))
supervisor_server_hosts = set(default("/clusterHostInfo/supervisor_hosts", []))

pure_slave = not hostname in (namenode_host | jtnode_host | rm_host | hs_host | \
                              hbase_master_hosts | slave_hosts | tt_hosts | hbase_rs_hosts | \
                              flume_hosts | nm_hosts | jn_hosts | nimbus_server_hosts | \
                              supervisor_server_hosts)
is_namenode_master = hostname in namenode_host
is_jtnode_master = hostname in jtnode_host
is_rmnode_master = hostname in rm_host
is_hsnode_master = hostname in hs_host
is_hbase_master = hostname in hbase_master_hosts
is_slave = hostname in slave_hosts
is_tasktracker = hostname in tt_hosts
is_nodemanager = hostname in nm_hosts
is_hbase_rs = hostname in hbase_rs_hosts
is_flume = hostname in flume_hosts
is_ganglia_server_host = (hostname == ganglia_server_host)
is_jn_host = hostname in jn_hosts
is_nimbus_host = hostname in nimbus_server_hosts
is_supervisor_host = hostname in supervisor_server_hosts

has_namenodes = not len(namenode_host) == 0
has_jobtracker = not len(jtnode_host) == 0
has_resourcemanager = not len(rm_host) == 0
has_historyserver = not len(hs_host) == 0
has_hbase_masters = not len(hbase_master_hosts) == 0
has_slaves = not len(slave_hosts) == 0
has_tasktracker = not len(tt_hosts) == 0
has_nodemanager = not len(nm_hosts) == 0
has_hbase_rs = not len(hbase_rs_hosts) == 0
has_flume = not len(flume_hosts) == 0
has_journalnode = not len(jn_hosts) == 0
has_nimbus_server = not len(nimbus_server_hosts) == 0
has_supervisor_server = not len(supervisor_server_hosts) == 0

ganglia_cluster_names = {
  "jtnode_host": [("HDPJournalNode", 8654)],
  "flume_hosts": [("HDPFlumeServer", 8655)],
  "hbase_rs_hosts": [("HDPHBaseRegionServer", 8656)],
  "nm_hosts": [("HDPNodeManager", 8657)],
  "mapred_tt_hosts": [("HDPTaskTracker", 8658)],
  "slave_hosts": [("HDPDataNode", 8659), ("HDPSlaves", 8660)],
  "namenode_host": [("HDPNameNode", 8661)],
  "jtnode_host": [("HDPJobTracker", 8662)],
  "hbase_master_hosts": [("HDPHBaseMaster", 8663)],
  "rm_host": [("HDPResourceManager", 8664)],
  "hs_host": [("HDPHistoryServer", 8666)],
  "nimbus_hosts": [("HDPNimbus", 8649)],
  "supervisor_hosts": [("HDPSupervisor", 8650)],
  "Application1": [("Application1", 8667)],
  "Application2": [("Application2", 8668)],
  "Application3": [("Application3", 8669)]
}

ganglia_clusters = []

for key in ganglia_cluster_names:
  property_name = format("/clusterHostInfo/{key}")
  hosts = set(default(property_name, []))
  if not len(hosts) == 0:
    for x in ganglia_cluster_names[key]:
      ganglia_clusters.append(x)
if len(gmond_apps) > 0:
  for gmond_app in gmond_apps:
    for x in ganglia_cluster_names[gmond_app]:
      ganglia_clusters.append(x)

if System.get_instance().os_family == "suse":
  rrd_py_path = '/srv/www/cgi-bin'
else:
  rrd_py_path = '/var/www/cgi-bin'
