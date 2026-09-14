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

"""

import ConfigParser
import os
import re

from ambari_commons import OSCheck
from ambari_commons.ambari_metrics_helper import select_metric_collector_hosts_from_hostnames
from resource_management.core.logger import Logger
from resource_management.libraries import functions
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
from resource_management.libraries.functions.substitute_vars import substitute_vars
from resource_management.libraries.resources.hdfs_resource import HdfsResource

import status_params
from functions import calc_xmn_from_xms
from functions import check_append_heap_property
from functions import trim_heap_property

if OSCheck.is_windows_family():
  from params_windows import *
else:
  from params_linux import *
# server configurations
config = Script.get_config()
exec_tmp_dir = Script.get_tmp_dir()

def get_combined_memory_mb(value1, value2):
  try:
    part1 = int(value1.strip()[:-1]) if value1.lower().strip()[-1:] == 'm' else int(value1)
    part2 = int(value2.strip()[:-1]) if value2.lower().strip()[-1:] == 'm' else int(value2)
    return str(part1 + part2) + 'm'
  except:
    return None
pass

#AMBARI_METRICS data
ams_pid_dir = status_params.ams_collector_pid_dir
is_ams_distributed = config['configurations']['ams-site']['timeline.metrics.service.operation.mode'] == 'distributed'
ams_collector_script = "/usr/sbin/ambari-metrics-collector"
ams_collector_pid_dir = status_params.ams_collector_pid_dir
ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))
ams_collector_list = default("/clusterHostInfo/metrics_collector_hosts", [])
embedded_mode_multiple_instances = False

if not is_ams_distributed and len(ams_collector_list) > 1:
  embedded_mode_multiple_instances = True

set_instanceId = "false"
cluster_name = config["clusterName"]
if 'cluster-env' in config['configurations'] and \
    'metrics_collector_external_hosts' in config['configurations']['cluster-env']:
  ams_collector_hosts = config['configurations']['cluster-env']['metrics_collector_external_hosts']
  set_instanceId = "true"
else:
  ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))

metric_collector_host = select_metric_collector_hosts_from_hostnames(ams_collector_hosts)

if 'cluster-env' in config['configurations'] and \
    'metrics_collector_external_port' in config['configurations']['cluster-env']:
  metric_collector_port = config['configurations']['cluster-env']['metrics_collector_external_port']
else:
  metric_collector_web_address = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
  if metric_collector_web_address.find(':') != -1:
    metric_collector_port = metric_collector_web_address.split(':')[1]
  else:
    metric_collector_port = '6188'

failover_strategy_blacklisted_interval_seconds = default("/configurations/ams-env/failover_strategy_blacklisted_interval", "600")
failover_strategy = default("/configurations/ams-site/failover.strategy", "round-robin")
if default("/configurations/ams-site/timeline.metrics.service.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
  metric_collector_https_enabled = True
  metric_collector_protocol = 'https'
else:
  metric_collector_https_enabled = False
  metric_collector_protocol = 'http'
metric_truststore_path= default("/configurations/ams-ssl-client/ssl.client.truststore.location", "")
metric_truststore_type= default("/configurations/ams-ssl-client/ssl.client.truststore.type", "")
metric_truststore_password= default("/configurations/ams-ssl-client/ssl.client.truststore.password", "")
metric_truststore_ca_certs='ca.pem'

metric_truststore_alias_list = []
for host in ams_collector_hosts.split(","):
  metric_truststore_alias = default("/configurations/ams-ssl-client/{host}.ssl.client.truststore.alias", None)
  if not metric_truststore_alias:
    metric_truststore_alias = host
  metric_truststore_alias_list.append(metric_truststore_alias)

agent_cache_dir = config['agentLevelParams']['agentCacheDir']
service_package_folder = config['serviceLevelParams']['service_package_folder']
stack_name = default("/clusterLevelParams/stack_name", None)
dashboards_dirs = []
# Stack specific
dashboards_dirs.append(os.path.join(agent_cache_dir, service_package_folder,
                                   'files', 'grafana-dashboards', stack_name))
# Default
dashboards_dirs.append(os.path.join(agent_cache_dir, service_package_folder,
                                   'files', 'grafana-dashboards', 'default'))

# Custom services
dashboards_dirs.append(os.path.join(agent_cache_dir, 'dashboards', 'grafana-dashboards'))

def get_grafana_dashboard_defs():
  dashboard_defs = []
  for dashboards_dir in dashboards_dirs:
    if os.path.exists(dashboards_dir):
      for root, dirs, files in os.walk(dashboards_dir):
        for file in files:
          if 'grafana' in file:
            dashboard_defs.append(os.path.join(root, file))
  return dashboard_defs

# find ambari version for grafana dashboards
def get_ambari_version():
  ambari_version = None
  AMBARI_AGENT_CONF = '/etc/ambari-agent/conf/ambari-agent.ini'
  ambari_agent_config = ConfigParser.RawConfigParser()
  if os.path.exists(AMBARI_AGENT_CONF):
    try:
      ambari_agent_config.read(AMBARI_AGENT_CONF)
      data_dir = ambari_agent_config.get('agent', 'prefix')
      ver_file = os.path.join(data_dir, 'version')
      f = open(ver_file, "r")
      ambari_version = f.read().strip()
      f.close()
    except Exception, e:
      Logger.info('Unable to determine ambari version from version file.')
      Logger.debug('Exception: %s' % str(e))
      # No hostname script identified in the ambari agent conf
      pass
    pass
  return ambari_version

hostname = config['agentLevelParams']['hostname']

ams_collector_log_dir = config['configurations']['ams-env']['metrics_collector_log_dir']
ams_collector_conf_dir = "/etc/ambari-metrics-collector/conf"
ams_monitor_log_dir = config['configurations']['ams-env']['metrics_monitor_log_dir']

ams_monitor_dir = "/usr/lib/python2.6/site-packages/resource_monitoring"
ams_monitor_conf_dir = "/etc/ambari-metrics-monitor/conf"
ams_monitor_pid_dir = status_params.ams_monitor_pid_dir
ams_monitor_script = "/usr/sbin/ambari-metrics-monitor"

ams_grafana_script = "/usr/sbin/ambari-metrics-grafana"
ams_grafana_home_dir = '/usr/lib/ambari-metrics-grafana'
ams_grafana_log_dir = default("/configurations/ams-grafana-env/metrics_grafana_log_dir", '/var/log/ambari-metrics-grafana')
ams_grafana_pid_dir = status_params.ams_grafana_pid_dir
ams_grafana_conf_dir = '/etc/ambari-metrics-grafana/conf'
ams_grafana_data_dir = default("/configurations/ams-grafana-env/metrics_grafana_data_dir", '/var/lib/ambari-metrics-grafana')
ams_grafana_admin_user = config['configurations']['ams-grafana-env']['metrics_grafana_username']
ams_grafana_admin_pwd = config['configurations']['ams-grafana-env']['metrics_grafana_password']

metrics_grafana_hosts = default('/clusterHostInfo/metrics_grafana_hosts', None)
ams_grafana_host = None
if metrics_grafana_hosts:
  ams_grafana_host = metrics_grafana_hosts[0]
ams_grafana_port = default("/configurations/ams-grafana-ini/port", 3000)
ams_grafana_protocol = default("/configurations/ams-grafana-ini/protocol", 'http')
ams_grafana_cert_file = default("/configurations/ams-grafana-ini/cert_file", '/etc/ambari-metrics/conf/ams-grafana.crt')
ams_grafana_cert_key = default("/configurations/ams-grafana-ini/cert_key", '/etc/ambari-metrics/conf/ams-grafana.key')
ams_grafana_ca_cert = default("/configurations/ams-grafana-ini/ca_cert", None)

grafana_connect_attempts = max(int(default('/configurations/ams-grafana-env/metrics_grafana_connect_attempts', 15)), 1)
grafana_connect_retry_delay = max(int(default('/configurations/ams-grafana-env/metrics_grafana_connect_retry_delay', 20)), 1)

ams_hbase_home_dir = "/usr/lib/ams-hbase/"

ams_hbase_init_check_enabled = default("/configurations/ams-site/timeline.metrics.hbase.init.check.enabled", True)

#hadoop params

hbase_excluded_hosts = config['commandParams']['excluded_hosts']
hbase_drain_only = config['commandParams']['mark_draining_only']
hbase_included_hosts = config['commandParams']['included_hosts']

hbase_user = status_params.hbase_user
smokeuser = config['configurations']['cluster-env']['smokeuser']
hbase_root_dir = config['configurations']['ams-hbase-site']['hbase.rootdir']
hbase_pid_dir = status_params.hbase_pid_dir

is_hbase_distributed = config['configurations']['ams-hbase-site']['hbase.cluster.distributed']
is_local_fs_rootdir = hbase_root_dir.startswith('file://')

# security is disabled for embedded mode, when HBase is backed by file
security_enabled = False if not is_hbase_distributed else config['configurations']['cluster-env']['security_enabled']

# this is "hadoop-metrics.properties" for 1.x stacks
metric_prop_file_name = "hadoop-metrics2-hbase.properties"

java_home = config['ambariLevelParams']['java_home']
ambari_java_home = default("/ambariLevelParams/ambari_java_home", None)
# not supporting 32 bit jdk.
java64_home = ambari_java_home if ambari_java_home is not None else java_home
ambari_java_version = default("/ambariLevelParams/ambari_java_version", None)
if ambari_java_version:
  java_version = expect("/ambariLevelParams/ambari_java_version", int)
else :
  java_version = expect("/ambariLevelParams/java_version", int)

metrics_collector_heapsize = default('/configurations/ams-env/metrics_collector_heapsize', "512")
metrics_report_interval = default("/configurations/ams-site/timeline.metrics.sink.report.interval", 60)
metrics_collection_period = default("/configurations/ams-site/timeline.metrics.sink.collection.period", 10)
skip_disk_metrics_patterns = default("/configurations/ams-env/timeline.metrics.skip.disk.metrics.patterns", None)
skip_network_interfaces_patterns = default("/configurations/ams-env/timeline.metrics.skip.network.interfaces.patterns", None)
skip_virtual_interfaces = default("/configurations/ams-env/timeline.metrics.skip.virtual.interfaces", False)

hbase_log_dir = config['configurations']['ams-hbase-env']['hbase_log_dir']
hbase_classpath_additional = default("/configurations/ams-hbase-env/hbase_classpath_additional", None)
master_heapsize = config['configurations']['ams-hbase-env']['hbase_master_heapsize']
regionserver_heapsize = config['configurations']['ams-hbase-env']['hbase_regionserver_heapsize']

# Check if hbase java options already have appended "m". If Yes, remove the trailing m.
metrics_collector_heapsize = check_append_heap_property(str(metrics_collector_heapsize), "m")
master_heapsize = check_append_heap_property(str(master_heapsize), "m")
regionserver_heapsize = check_append_heap_property(str(regionserver_heapsize), "m")

host_in_memory_aggregation = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", False)
host_in_memory_aggregation_port = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888)
is_aggregation_https_enabled = False
if default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
  host_in_memory_aggregation_protocol = 'https'
  is_aggregation_https_enabled = True
else:
  host_in_memory_aggregation_protocol = 'http'
host_in_memory_aggregation_jvm_arguments = default("/configurations/ams-env/timeline.metrics.host.inmemory.aggregation.jvm.arguments",
                                                   "-Xmx256m -Xms128m -XX:PermSize=68m")

regionserver_xmn_max = default('/configurations/ams-hbase-env/hbase_regionserver_xmn_max', None)
if regionserver_xmn_max:
  regionserver_xmn_max = int(trim_heap_property(str(regionserver_xmn_max), "m"))
  regionserver_xmn_percent = expect("/configurations/ams-hbase-env/hbase_regionserver_xmn_ratio", float)
  regionserver_xmn_size = calc_xmn_from_xms(regionserver_heapsize, regionserver_xmn_percent, regionserver_xmn_max)
else:
  regionserver_xmn_size = config['configurations']['ams-hbase-env']['regionserver_xmn_size']
pass

hbase_master_xmn_size = config['configurations']['ams-hbase-env']['hbase_master_xmn_size']
hbase_master_maxperm_size = config['configurations']['ams-hbase-env']['hbase_master_maxperm_size']

# Check if hbase java options already have appended "m". If Yes, remove the trailing m.
hbase_master_maxperm_size = check_append_heap_property(str(hbase_master_maxperm_size), "m")
hbase_master_xmn_size = check_append_heap_property(str(hbase_master_xmn_size), "m")
regionserver_xmn_size = check_append_heap_property(str(regionserver_xmn_size), "m")

# Choose heap size for embedded mode as sum of master + regionserver
if not is_hbase_distributed:
  hbase_heapsize = get_combined_memory_mb(master_heapsize, regionserver_heapsize)
  if hbase_heapsize is None:
    hbase_heapsize = master_heapsize
else:
  hbase_heapsize = master_heapsize

max_open_files_limit = default("/configurations/ams-hbase-env/max_open_files_limit", "32768")

cluster_zookeeper_quorum_hosts = ",".join(config['clusterHostInfo']['zookeeper_server_hosts'])
if 'zoo.cfg' in config['configurations'] and 'clientPort' in config['configurations']['zoo.cfg']:
  cluster_zookeeper_clientPort = config['configurations']['zoo.cfg']['clientPort']
else:
  cluster_zookeeper_clientPort = '2181'

if not is_hbase_distributed:
  zookeeper_quorum_hosts = hostname
  zookeeper_clientPort = '61181'
else:
  zookeeper_quorum_hosts = cluster_zookeeper_quorum_hosts
  zookeeper_clientPort = cluster_zookeeper_clientPort

ams_checkpoint_dir = config['configurations']['ams-site']['timeline.metrics.aggregator.checkpoint.dir']
_hbase_tmp_dir = config['configurations']['ams-hbase-site']['hbase.tmp.dir']
hbase_tmp_dir = substitute_vars(_hbase_tmp_dir, config['configurations']['ams-hbase-site'])
_zookeeper_data_dir = config['configurations']['ams-hbase-site']['hbase.zookeeper.property.dataDir']
zookeeper_data_dir = substitute_vars(_zookeeper_data_dir, config['configurations']['ams-hbase-site'])
# TODO UPGRADE default, update site during upgrade
_local_dir_conf = default('/configurations/ams-hbase-site/hbase.local.dir', "${hbase.tmp.dir}/local")
local_dir = substitute_vars(_local_dir_conf, config['configurations']['ams-hbase-site'])

phoenix_max_global_mem_percent = default('/configurations/ams-site/phoenix.query.maxGlobalMemoryPercentage', '20')
phoenix_client_spool_dir = default('/configurations/ams-site/phoenix.spool.directory', '/tmp')
phoenix_server_spool_dir = default('/configurations/ams-hbase-site/phoenix.spool.directory', '/tmp')
# Substitute vars if present
phoenix_client_spool_dir = substitute_vars(phoenix_client_spool_dir, config['configurations']['ams-hbase-site'])
phoenix_server_spool_dir = substitute_vars(phoenix_server_spool_dir, config['configurations']['ams-hbase-site'])

client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
regionserver_jaas_config_file = format("{hbase_conf_dir}/hbase_regionserver_jaas.conf")

rs_hosts = ["localhost"]

smoke_test_user = config['configurations']['cluster-env']['smokeuser']
smokeuser_permissions = "RWXCA"
service_check_data = functions.get_unique_id_and_date()
user_group = config['configurations']['cluster-env']["user_group"]
hadoop_user = "hadoop"

kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
monitor_kinit_cmd = ""
klist_path_local = functions.get_klist_path(default('/configurations/kerberos-env/executable_search_paths', None))
klist_cmd = ""

if security_enabled:
  _hostname_lowercase = config['agentLevelParams']['hostname'].lower()
  client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
  smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
  smoke_user_princ = config['configurations']['cluster-env']['smokeuser_principal_name']
  smoke_user = config['configurations']['cluster-env']['smokeuser']
  hbase_user_keytab = config['configurations']['ams-hbase-env']['hbase_user_keytab']

  ams_collector_jaas_config_file = format("{hbase_conf_dir}/ams_collector_jaas.conf")
  ams_collector_keytab_path = config['configurations']['ams-hbase-security-site']['hbase.myclient.keytab']
  ams_collector_jaas_princ = config['configurations']['ams-hbase-security-site']['hbase.myclient.principal'].replace('_HOST',_hostname_lowercase)

  ams_zookeeper_jaas_config_file = format("{hbase_conf_dir}/ams_zookeeper_jaas.conf")
  ams_zookeeper_keytab = config['configurations']['ams-hbase-security-site']['ams.zookeeper.keytab']
  ams_zookeeper_principal_name = config['configurations']['ams-hbase-security-site']['ams.zookeeper.principal'].replace('_HOST',_hostname_lowercase)

  master_jaas_config_file = format("{hbase_conf_dir}/hbase_master_jaas.conf")
  master_keytab_path = config['configurations']['ams-hbase-security-site']['hbase.master.keytab.file']
  master_jaas_princ = config['configurations']['ams-hbase-security-site']['hbase.master.kerberos.principal'].replace('_HOST',_hostname_lowercase)

  regionserver_jaas_config_file = format("{hbase_conf_dir}/hbase_regionserver_jaas.conf")
  regionserver_keytab_path = config['configurations']['ams-hbase-security-site']['hbase.regionserver.keytab.file']
  regionserver_jaas_princ = config['configurations']['ams-hbase-security-site']['hbase.regionserver.kerberos.principal'].replace('_HOST',_hostname_lowercase)

  # Monitor SPNEGO configs
  ams_monitor_keytab = None
  if (('ams-hbase-security-site' in config['configurations']) and ('ams.monitor.keytab' in config['configurations']['ams-hbase-security-site'])):
    ams_monitor_keytab = config['configurations']['ams-hbase-security-site']['ams.monitor.keytab']

  ams_monitor_principal = None
  if (('ams-hbase-security-site' in config['configurations']) and ('ams.monitor.principal' in config['configurations']['ams-hbase-security-site'])):
    ams_monitor_principal = config['configurations']['ams-hbase-security-site']['ams.monitor.principal']

  if ams_monitor_keytab and ams_monitor_principal:
    monitor_kinit_cmd = '%s -kt %s %s' % (kinit_path_local, ams_monitor_keytab, ams_monitor_principal.replace('_HOST',_hostname_lowercase))
    klist_cmd = '%s' % klist_path_local

#Ambari metrics log4j settings
ams_hbase_log_maxfilesize = default('configurations/ams-hbase-log4j/ams_hbase_log_maxfilesize',256)
ams_hbase_log_maxbackupindex = default('configurations/ams-hbase-log4j/ams_hbase_log_maxbackupindex',20)
ams_hbase_security_log_maxfilesize = default('configurations/ams-hbase-log4j/ams_hbase_security_log_maxfilesize',256)
ams_hbase_security_log_maxbackupindex = default('configurations/ams-hbase-log4j/ams_hbase_security_log_maxbackupindex',20)
ams_log_max_backup_size = default('configurations/ams-log4j/ams_log_max_backup_size',80)
ams_log_number_of_backup_files = default('configurations/ams-log4j/ams_log_number_of_backup_files',60)

#log4j.properties
if (('ams-hbase-log4j' in config['configurations']) and ('content' in config['configurations']['ams-hbase-log4j'])):
  hbase_log4j_props = config['configurations']['ams-hbase-log4j']['content']
else:
  hbase_log4j_props = None

if (('ams-log4j' in config['configurations']) and ('content' in config['configurations']['ams-log4j'])):
  log4j_props = config['configurations']['ams-log4j']['content']
else:
  log4j_props = None

hbase_env_sh_template = config['configurations']['ams-hbase-env']['content']
ams_env_sh_template = config['configurations']['ams-env']['content']
ams_grafana_env_sh_template = config['configurations']['ams-grafana-env']['content']
ams_grafana_ini_template = config['configurations']['ams-grafana-ini']['content']

hbase_staging_dir = default("/configurations/ams-hbase-site/hbase.bulkload.staging.dir", "/amshbase/staging")
skip_create_hbase_root_dir = default("/configurations/ams-site/timeline.metrics.skip.create.hbase.root.dir", False)
hbase_wal_dir = default("/configurations/ams-hbase-site/hbase.wal.dir", None)
if hbase_wal_dir and re.search("^file://|/", hbase_wal_dir): #If wal dir is on local file system, create it.
  hbase_wal_dir = re.sub("^file://|/", "", hbase_wal_dir, count=1)

#for create_hdfs_directory
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']


clusterHostInfoDict = config["clusterHostInfo"]
min_hadoop_sink_version = default("/configurations/ams-env/min_ambari_metrics_hadoop_sink_version", "2.7.0.0")

hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']
dfs_type = default("/clusterLevelParams/dfs_type", "")

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user=hdfs_user,
  hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled = security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs,
  immutable_paths = get_not_managed_resources(),
  dfs_type = dfs_type
 )



