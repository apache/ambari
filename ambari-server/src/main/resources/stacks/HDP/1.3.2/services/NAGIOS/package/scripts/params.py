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

from functions import is_jdk_greater_6
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
nagios_principal_name = default("/configurations/nagios-env/nagios_principal_name", "nagios")
hadoop_ssl_enabled = False

namenode_metadata_port = get_port_from_url(config['configurations']['core-site']['fs.default.name'])
oozie_server_port = get_port_from_url(config['configurations']['oozie-site']['oozie.base.url'])
# different to HDP2    
namenode_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.http.address'])
# different to HDP2  
snamenode_port = get_port_from_url(config['configurations']['hdfs-site']["dfs.secondary.http.address"])

hbase_master_rpc_port = default('/configurations/hbase-site/hbase.master.port', "60000")
hs_port = get_port_from_url(config['configurations']['mapred-site']['mapreduce.history.server.http.address'])
journalnode_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.journalnode.http-address'])
datanode_port = get_port_from_url(config['configurations']['hdfs-site']['dfs.datanode.http.address'])
flume_port = "4159"
hive_metastore_port = get_port_from_url(config['configurations']['hive-site']['hive.metastore.uris']) #"9083"
hive_server_port = default('/configurations/hive-site/hive.server2.thrift.port',"10000")
templeton_port = config['configurations']['webhcat-site']['templeton.port'] #"50111"
hbase_master_port = config['configurations']['hbase-site']['hbase.master.info.port'] #"60010"
hbase_rs_port = config['configurations']['hbase-site']['hbase.regionserver.info.port'] #"60030"

# this 4 is different for HDP2
jtnode_port = get_port_from_url(config['configurations']['mapred-site']['mapred.job.tracker.http.address'])
jobhistory_port = get_port_from_url(config['configurations']['mapred-site']['mapreduce.history.server.http.address'])
tasktracker_port = "50060"
mapred_local_dir = config['configurations']['mapred-site']['mapred.local.dir']

# this is different for HDP2
nn_metrics_property = "FSNamesystemMetrics"
clientPort = config['configurations']['zoo.cfg']['clientPort'] #ZK


java64_home = config['hostLevelParams']['java_home']
check_cpu_on = is_jdk_greater_6(java64_home)
security_enabled = config['configurations']['cluster-env']['security_enabled']

nagios_keytab_path = default("/configurations/nagios-env/nagios_keytab_path", "/etc/security/keytabs/nagios.service.keytab")
kinit_path_local = functions.get_kinit_path(["/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

if security_enabled:
  smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
  smokeuser = config['configurations']['cluster-env']['smokeuser']
  kinitcmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser}; ")
  hive_server_principal = default("configurations/hive-site/hive.server2.authentication.kerberos.principal","hive/_HOST@EXAMPLE.COM")
else:
  kinitcmd = ''
  hive_server_principal = ''
hive_server2_authentication = default("configurations/hive-site/hive.server2.authentication","NOSASL")

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
  
nagios_user = config['configurations']['nagios-env']['nagios_user']
nagios_group = config['configurations']['nagios-env']['nagios_group']
nagios_web_login = config['configurations']['nagios-env']['nagios_web_login']
nagios_web_password = config['configurations']['nagios-env']['nagios_web_password']
user_group = config['configurations']['cluster-env']['user_group']
nagios_contact = config['configurations']['nagios-env']['nagios_contact']

namenode_host = default("/clusterHostInfo/namenode_host", None)
_snamenode_host = default("/clusterHostInfo/snamenode_host", None)
_jtnode_host = default("/clusterHostInfo/jtnode_host", None)
_slave_hosts = default("/clusterHostInfo/slave_hosts", None)
_tt_hosts = default("/clusterHostInfo/mapred_tt_hosts", [])
_journalnode_hosts = default("/clusterHostInfo/journalnode_hosts", None)
_zkfc_hosts = default("/clusterHostInfo/zkfc_hosts", None)
_hs_host = default("/clusterHostInfo/hs_host", None)
_zookeeper_hosts = default("/clusterHostInfo/zookeeper_hosts", None)
_flume_hosts = default("/clusterHostInfo/flume_hosts", None)
_nagios_server_host = default("/clusterHostInfo/nagios_server_host",None)
_ganglia_server_host = default("/clusterHostInfo/ganglia_server_host",None)
hbase_master_hosts = default("/clusterHostInfo/hbase_master_hosts",None)
if type(hbase_master_hosts) is list:
  hbase_master_hosts_in_str = ','.join(hbase_master_hosts)
_hive_server_host = default("/clusterHostInfo/hive_server_host",None)
_oozie_server = default("/clusterHostInfo/oozie_server",None)
_webhcat_server_host = default("/clusterHostInfo/webhcat_server_host",None)
# can differ on HDP2
_mapred_tt_hosts = _tt_hosts
#if hbase_rs_hosts not given it is assumed that region servers on same nodes as slaves
_hbase_rs_hosts = default("/clusterHostInfo/hbase_rs_hosts", _slave_hosts)
_hue_server_host = default("/clusterHostInfo/hue_server_host", None)
all_hosts = config['clusterHostInfo']['all_hosts']


hostgroup_defs = {
    'namenode' : namenode_host,
    'snamenode' : _snamenode_host,
    'slaves' : _slave_hosts,
    # no in HDP2
    'tasktracker-servers' : _mapred_tt_hosts,
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
    'historyserver2' : _hs_host,
    'jobhistory': _hs_host,
    'journalnodes' : _journalnode_hosts
}
