#!/usr/bin/env python
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

from functions import get_port_from_url
from resource_management import *
import status_params

# server configurations
config = Script.get_config()

conf_dir = "/etc/nagios"
nagios_var_dir = "/var/nagios"
nagios_rw_dir = "/var/nagios/rw"
plugins_dir = "/usr/lib64/nagios/plugins"
nagios_obj_dir = "/etc/nagios/objects"
check_result_path = "/var/nagios/spool/checkresults"
nagios_log_dir = "/var/log/nagios"
nagios_log_archives_dir = format("{nagios_log_dir}/archives")
nagios_host_cfg = format("{nagios_obj_dir}/hadoop-hosts.cfg")
nagios_lookup_daemon_str = "/usr/sbin/nagios"
nagios_pid_dir = status_params.nagios_pid_dir
nagios_pid_file = status_params.nagios_pid_file
nagios_resource_cfg = format("{conf_dir}/resource.cfg")
nagios_hostgroup_cfg = format("{nagios_obj_dir}/hadoop-hostgroups.cfg")
nagios_servicegroup_cfg = format("{nagios_obj_dir}/hadoop-servicegroups.cfg")
nagios_service_cfg = format("{nagios_obj_dir}/hadoop-services.cfg")
nagios_command_cfg = format("{nagios_obj_dir}/hadoop-commands.cfg")
eventhandlers_dir = "/usr/lib/nagios/eventhandlers"
nagios_principal_name = default("nagios_principal_name", "nagios")
hadoop_ssl_enabled = False

namenode_metadata_port = get_port_from_url(config['configurations']['core-site']['fs.defaultFS'])
oozie_server_port = "11000"
# different to HDP1    
namenode_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.namenode.http-address'])
# different to HDP1  
snamenode_port = get_port_from_url(config['configurations']['hdfs-site']["dfs.namenode.secondary.http-address"])

hbase_master_rpc_port = "60000"
rm_port = get_port_from_url(config['configurations']['yarn-site']['yarn.resourcemanager.webapp.address'])
nm_port = "8042"
hs_port = get_port_from_url(config['configurations']['mapred-site']['mapreduce.jobhistory.webapp.address'])
journalnode_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.journalnode.http-address'])
datanode_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.datanode.http.address'])
flume_port = "4159"
hive_metastore_port = config['configurations']['global']['hive_metastore_port'] #"9083"
hive_server_port = "10000"
templeton_port = config['configurations']['webhcat-site']['templeton.port'] #"50111"
hbase_rs_port = "60030"
storm_ui_port = config['configurations']['storm-site']['ui.port']
drpc_port = config['configurations']['storm-site']['drpc.port']
nimbus_port = config['configurations']['storm-site']['nimbus.thrift.port']
supervisor_port = "56431"
storm_rest_api_port = "8745"
falcon_port = config['configurations']['global']['falcon_port']
ahs_port = get_port_from_url(config['configurations']['yarn-site']['yarn.timeline-service.webapp.address'])

# this is different for HDP1
nn_metrics_property = "FSNamesystem"
clientPort = config['configurations']['global']['clientPort'] #ZK 


java64_home = config['hostLevelParams']['java_home']
_authentication = config['configurations']['core-site']['hadoop.security.authentication']
security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')

nagios_keytab_path = default("nagios_keytab_path", "/etc/security/keytabs/nagios.service.keytab")
kinit_path_local = functions.get_kinit_path([default("kinit_path_local",None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

dfs_ha_enabled = False
dfs_ha_nameservices = default("/configurations/hdfs-site/dfs.nameservices", None)
dfs_ha_namenode_ids = default(format("/configurations/hdfs-site/dfs.ha.namenodes.{dfs_ha_nameservices}"), None)
if dfs_ha_namenode_ids:
  dfs_ha_namemodes_ids_list = dfs_ha_namenode_ids.split(",")
  dfs_ha_namenode_ids_array_len = len(dfs_ha_namemodes_ids_list)
  if dfs_ha_namenode_ids_array_len > 1:
    dfs_ha_enabled = True

nn_ha_host_port_map = {}
if dfs_ha_enabled:
  for nn_id in dfs_ha_namemodes_ids_list:
    nn_host = config['configurations']['hdfs-site'][format('dfs.namenode.rpc-address.{dfs_ha_nameservices}.{nn_id}')]
    nn_ha_host_port_map[nn_host.split(":")[0]] = nn_host.split(":")[1]
else:
  nn_ha_host_port_map[config['clusterHostInfo']['namenode_host'][0]] = namenode_metadata_port

ganglia_port = "8651"
ganglia_collector_slaves_port = "8660"
ganglia_collector_namenode_port = "8661"
ganglia_collector_jobtracker_port = "8662"
ganglia_collector_hbase_port = "8663"
ganglia_collector_rm_port = "8664"
ganglia_collector_nm_port = "8660"
ganglia_collector_hs_port = "8666"
  
all_ping_ports = config['clusterHostInfo']['all_ping_ports']

if System.get_instance().os_family == "suse":
  nagios_p1_pl = "/usr/lib/nagios/p1.pl"
  htpasswd_cmd = "htpasswd2"
  nagios_httpd_config_file = format("/etc/apache2/conf.d/nagios.conf")
else:
  nagios_p1_pl = "/usr/bin/p1.pl"
  htpasswd_cmd = "htpasswd"
  nagios_httpd_config_file = format("/etc/httpd/conf.d/nagios.conf")
  
nagios_user = config['configurations']['global']['nagios_user']
nagios_group = config['configurations']['global']['nagios_group']
nagios_web_login = config['configurations']['global']['nagios_web_login']
nagios_web_password = config['configurations']['global']['nagios_web_password']
user_group = config['configurations']['global']['user_group']
nagios_contact = config['configurations']['global']['nagios_contact']

namenode_host = default("/clusterHostInfo/namenode_host", None)
_snamenode_host = default("/clusterHostInfo/snamenode_host", None)
_jtnode_host = default("/clusterHostInfo/jtnode_host", None)
_slave_hosts = default("/clusterHostInfo/slave_hosts", None)
_journalnode_hosts = default("/clusterHostInfo/journalnode_hosts", None)
_zkfc_hosts = default("/clusterHostInfo/zkfc_hosts", None)
_rm_host = default("/clusterHostInfo/rm_host", None)
_nm_hosts = default("/clusterHostInfo/nm_hosts", None)
_hs_host = default("/clusterHostInfo/hs_host", None)
_zookeeper_hosts = default("/clusterHostInfo/zookeeper_hosts", None)
_flume_hosts = default("/clusterHostInfo/flume_hosts", None)
_nagios_server_host = default("/clusterHostInfo/nagios_server_host",None)
_ganglia_server_host = default("/clusterHostInfo/ganglia_server_host",None)
_app_timeline_server_hosts = default("/clusterHostInfo/app_timeline_server_hosts",None)
_nimbus_host = default("/clusterHostInfo/nimbus_hosts",None)
_drpc_host = default("/clusterHostInfo/drpc_server_hosts",None)
_supervisor_hosts = default("/clusterHostInfo/supervisor_hosts",None)
_storm_ui_host = default("/clusterHostInfo/storm_ui_server_hosts",None)
_storm_rest_api_hosts = default("/clusterHostInfo/storm_rest_api_hosts",None)
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts",None)
_hive_server_host = default("/clusterHostInfo/hive_server_host",None)
_oozie_server = default("/clusterHostInfo/oozie_server",None)
_webhcat_server_host = default("/clusterHostInfo/webhcat_server_host",None)
_falcon_host = default("/clusterHostInfo/falcon_server_hosts", None)
# can differ on HDP1
#_mapred_tt_hosts = _slave_hosts
#if hbase_rs_hosts not given it is assumed that region servers on same nodes as slaves
_hbase_rs_hosts = default("/clusterHostInfo/hbase_rs_hosts", _slave_hosts)
_hue_server_host = default("/clusterHostInfo/hue_server_host", None)
all_hosts = config['clusterHostInfo']['all_hosts']


hostgroup_defs = {
    'namenode' : namenode_host,
    'snamenode' : _snamenode_host,
    'slaves' : _slave_hosts,
    # HDP1
    #'tasktracker-servers' : _mapred_tt_hosts,
    'agent-servers' : all_hosts,
    'nagios-server' : _nagios_server_host,
    'jobtracker' : _jtnode_host,
    'ganglia-server' : _ganglia_server_host,
    'flume-servers' : _flume_hosts,
    'zookeeper-servers' : _zookeeper_hosts,
    'hbasemasters' : hbase_master_hosts,
    'hiveserver' : _hive_server_host,
    'region-servers' : _hbase_rs_hosts,
    'oozie-server' : _oozie_server,
    'webhcat-server' : _webhcat_server_host,
    'hue-server' : _hue_server_host,
    'resourcemanager' : _rm_host,
    'nodemanagers' : _nm_hosts,
    'historyserver2' : _hs_host,
    'journalnodes' : _journalnode_hosts,
    'nimbus' : _nimbus_host,
    'drpc-server' : _drpc_host,
    'storm_ui' : _storm_ui_host,
    'supervisors' : _supervisor_hosts,
    'storm_rest_api' : _storm_rest_api_hosts,
    'falcon-server' : _falcon_host,
    'ats-servers' : _app_timeline_server_hosts
}
