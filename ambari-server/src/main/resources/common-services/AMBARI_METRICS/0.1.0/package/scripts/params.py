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

from functions import calc_xmn_from_xms
from functions import check_append_heap_property
from functions import trim_heap_property
from resource_management.core.logger import Logger
from resource_management import *
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources
import status_params
from ambari_commons import OSCheck
import ConfigParser
import os

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

ams_collector_script = "/usr/sbin/ambari-metrics-collector"
ams_collector_pid_dir = status_params.ams_collector_pid_dir
ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
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

agent_cache_dir = config['hostLevelParams']['agentCacheDir']
service_package_folder = config['commandParams']['service_package_folder']
dashboards_dir = os.path.join(agent_cache_dir, service_package_folder, 'files', 'grafana-dashboards')

def get_grafana_dashboard_defs():
  dashboard_defs = []
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


if 'cluster-env' in config['configurations'] and \
    'metrics_collector_vip_host' in config['configurations']['cluster-env']:
  metric_collector_host = config['configurations']['cluster-env']['metrics_collector_vip_host']
else:
  metric_collector_host = ams_collector_hosts[0]
if 'cluster-env' in config['configurations'] and \
    'metrics_collector_vip_port' in config['configurations']['cluster-env']:
  metric_collector_port = config['configurations']['cluster-env']['metrics_collector_vip_port']
else:
  metric_collector_web_address = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "localhost:6188")
  if metric_collector_web_address.find(':') != -1:
    metric_collector_port = metric_collector_web_address.split(':')[1]
  else:
    metric_collector_port = '6188'

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

ams_hbase_home_dir = "/usr/lib/ams-hbase/"

ams_hbase_normalizer_enabled = default("/configurations/ams-hbase-site/hbase.normalizer.enabled", None)
ams_hbase_fifo_compaction_enabled = default("/configurations/ams-site/timeline.metrics.hbase.fifo.compaction.enabled", None)
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
is_ams_distributed = config['configurations']['ams-site']['timeline.metrics.service.operation.mode'] == 'distributed'

# security is disabled for embedded mode, when HBase is backed by file
security_enabled = False if not is_hbase_distributed else config['configurations']['cluster-env']['security_enabled']

# this is "hadoop-metrics.properties" for 1.x stacks
metric_prop_file_name = "hadoop-metrics2-hbase.properties"

# not supporting 32 bit jdk.
java64_home = config['hostLevelParams']['java_home']
java_version = int(config['hostLevelParams']['java_version'])

metrics_collector_heapsize = default('/configurations/ams-env/metrics_collector_heapsize', "512")
host_sys_prepped = default("/hostLevelParams/host_sys_prepped", False)
metrics_report_interval = default("/configurations/ams-site/timeline.metrics.sink.report.interval", 60)
metrics_collection_period = default("/configurations/ams-site/timeline.metrics.sink.collection.period", 10)

hbase_log_dir = config['configurations']['ams-hbase-env']['hbase_log_dir']
hbase_classpath_additional = default("/configurations/ams-hbase-env/hbase_classpath_additional", None)
master_heapsize = config['configurations']['ams-hbase-env']['hbase_master_heapsize']
regionserver_heapsize = config['configurations']['ams-hbase-env']['hbase_regionserver_heapsize']

# Check if hbase java options already have appended "m". If Yes, remove the trailing m.
metrics_collector_heapsize = check_append_heap_property(str(metrics_collector_heapsize), "m")
master_heapsize = check_append_heap_property(str(master_heapsize), "m")
regionserver_heapsize = check_append_heap_property(str(regionserver_heapsize), "m")

regionserver_xmn_max = default('/configurations/ams-hbase-env/hbase_regionserver_xmn_max', None)
if regionserver_xmn_max:
  regionserver_xmn_max = int(trim_heap_property(str(regionserver_xmn_max), "m"))
  regionserver_xmn_percent = config['configurations']['ams-hbase-env']['hbase_regionserver_xmn_ratio']
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

if not is_hbase_distributed:
  zookeeper_quorum_hosts = 'localhost'
  zookeeper_clientPort = '61181'
else:
  zookeeper_quorum_hosts = ",".join(config['clusterHostInfo']['zookeeper_hosts'])
  if 'zoo.cfg' in config['configurations'] and 'clientPort' in config['configurations']['zoo.cfg']:
    zookeeper_clientPort = config['configurations']['zoo.cfg']['clientPort']
  else:
    zookeeper_clientPort = '2181'

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

kinit_cmd = ""

if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  client_jaas_config_file = format("{hbase_conf_dir}/hbase_client_jaas.conf")
  smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
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

#for create_hdfs_directory
hostname = config["hostname"]
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name']
kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))



hdfs_site = config['configurations']['hdfs-site']
default_fs = config['configurations']['core-site']['fs.defaultFS']

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
  immutable_paths = get_not_managed_resources()
 )



