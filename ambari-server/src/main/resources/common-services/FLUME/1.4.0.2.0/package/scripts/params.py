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
from ambari_commons import OSCheck
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.default import default
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import get_kinit_path
from ambari_commons.ambari_metrics_helper import select_metric_collector_hosts_from_hostnames

if OSCheck.is_windows_family():
  from params_windows import *
else:
  from params_linux import *

config = Script.get_config()
stack_root = Script.get_stack_root()

stack_name = default("/hostLevelParams/stack_name", None)

# New Cluster Stack Version that is defined during the RESTART of a Stack Upgrade
version = default("/commandParams/version", None)

user_group = config['configurations']['cluster-env']['user_group']
proxyuser_group =  config['configurations']['hadoop-env']['proxyuser_group']

security_enabled = config['configurations']['cluster-env']['security_enabled']
if security_enabled :
    _hostname_lowercase = config['hostname'].lower()
    flume_jaas_princ = config['configurations']['flume-env']['flume_principal_name']
    flume_keytab_path = config['configurations']['flume-env']['flume_keytab_path']

stack_version_unformatted = config['hostLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

# hadoop default parameters
flume_bin = '/usr/bin/flume-ng'
flume_hive_home = '/usr/lib/hive'
flume_hcat_home = '/usr/lib/hive-hcatalog'

# hadoop parameters for stack supporting rolling upgrade
if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
  flume_bin = format('{stack_root}/current/flume-server/bin/flume-ng')
  flume_hive_home = format('{stack_root}/current/hive-metastore')
  flume_hcat_home = format('{stack_root}/current/hive-webhcat')

java_home = config['hostLevelParams']['java_home']
flume_log_dir = config['configurations']['flume-env']['flume_log_dir']
flume_run_dir = config['configurations']['flume-env']['flume_run_dir']
ambari_state_file = format("{flume_run_dir}/ambari-state.txt")

if (('flume-conf' in config['configurations']) and('content' in config['configurations']['flume-conf'])):
  flume_conf_content = config['configurations']['flume-conf']['content']
else:
  flume_conf_content = None

if (('flume-log4j' in config['configurations']) and ('content' in config['configurations']['flume-log4j'])):
  flume_log4j_content = config['configurations']['flume-log4j']['content']
else:
  flume_log4j_content = None

targets = default('/commandParams/flume_handler', None)
flume_command_targets = [] if targets is None else targets.split(',')

flume_env_sh_template = config['configurations']['flume-env']['content']

ganglia_server_hosts = default('/clusterHostInfo/ganglia_server_host', [])
ganglia_server_host = None
if 0 != len(ganglia_server_hosts):
  ganglia_server_host = ganglia_server_hosts[0]

hostname = None
if config.has_key('hostname'):
  hostname = config['hostname']

set_instanceId = "false"
cluster_name = config["clusterName"]

if 'cluster-env' in config['configurations'] and \
        'metrics_collector_external_hosts' in config['configurations']['cluster-env']:
  ams_collector_hosts = config['configurations']['cluster-env']['metrics_collector_external_hosts']
  set_instanceId = "true"
else:
  ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))

has_metric_collector = not len(ams_collector_hosts) == 0
metric_collector_port = None
if has_metric_collector:
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
  if default("/configurations/ams-site/timeline.metrics.service.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
    metric_collector_protocol = 'https'
  else:
    metric_collector_protocol = 'http'
  metric_truststore_path= default("/configurations/ams-ssl-client/ssl.client.truststore.location", "")
  metric_truststore_type= default("/configurations/ams-ssl-client/ssl.client.truststore.type", "")
  metric_truststore_password= default("/configurations/ams-ssl-client/ssl.client.truststore.password", "")
  pass
metrics_report_interval = default("/configurations/ams-site/timeline.metrics.sink.report.interval", 60)
metrics_collection_period = default("/configurations/ams-site/timeline.metrics.sink.collection.period", 10)

host_in_memory_aggregation = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", True)
host_in_memory_aggregation_port = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888)

# Cluster Zookeeper quorum
zookeeper_quorum = None
if not len(default("/clusterHostInfo/zookeeper_hosts", [])) == 0:
  if 'zoo.cfg' in config['configurations'] and 'clientPort' in config['configurations']['zoo.cfg']:
    zookeeper_clientPort = config['configurations']['zoo.cfg']['clientPort']
  else:
    zookeeper_clientPort = '2181'
  zookeeper_quorum = (':' + zookeeper_clientPort + ',').join(config['clusterHostInfo']['zookeeper_hosts'])
  # last port config
  zookeeper_quorum += ':' + zookeeper_clientPort

# smokeuser
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
smokeuser = config['configurations']['cluster-env']['smokeuser']
smokeuser_principal = config['configurations']['cluster-env']['smokeuser_principal_name']
smoke_user_keytab = config['configurations']['cluster-env']['smokeuser_keytab']
