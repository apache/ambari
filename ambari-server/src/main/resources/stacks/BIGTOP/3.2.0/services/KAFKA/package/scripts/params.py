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
import os
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.stack_features import get_stack_feature_version
from resource_management.libraries.functions.default import default
from utils import get_bare_principal
import status_params
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import upgrade_summary
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.get_not_managed_resources import get_not_managed_resources

# server configurations
config = Script.get_config()
tmp_dir = Script.get_tmp_dir()
stack_root = Script.get_stack_root()
stack_name = default("/clusterLevelParams/stack_name", None)
retryAble = default("/commandParams/command_retry_enabled", False)

# Version being upgraded/downgraded to
version = default("/commandParams/version", None)

stack_version_unformatted = config['clusterLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)
upgrade_direction = default("/commandParams/upgrade_direction", None)

# get the correct version to use for checking stack features
version_for_stack_feature_checks = get_stack_feature_version(config)

# When downgrading the 'version' is pointing to the downgrade-target version
# downgrade_from_version provides the source-version the downgrade is happening from
downgrade_from_version = upgrade_summary.get_downgrade_from_version("KAFKA")

hostname = config['agentLevelParams']['hostname']

# default kafka parameters
kafka_home = '/usr/lib/kafka'
kafka_start_cmd = kafka_home + "/bin/kafka-server-start.sh " + kafka_home + "/config/server.properties"
kafka_stop_cmd = kafka_home + "/bin/kafka-server-stop.sh " + kafka_home + "/config/server.properties"
kafka_bin = kafka_home + '/bin/kafka'
conf_dir = "/etc/kafka/conf"
limits_conf_dir = "/etc/security/limits.d"

# Used while upgrading the stack in a kerberized cluster and running kafka-acls.sh
zookeeper_connect = default("/configurations/kafka-broker/zookeeper.connect", None)

kafka_user_nofile_limit = default('/configurations/kafka-env/kafka_user_nofile_limit', 128000)
kafka_user_nproc_limit = default('/configurations/kafka-env/kafka_user_nproc_limit', 65536)

kafka_delete_topic_enable = default('/configurations/kafka-broker/delete.topic.enable', True)

# parameters for 2.2+
if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
  kafka_home = os.path.join(stack_root,  "current", "kafka-broker")
  kafka_bin = os.path.join(kafka_home, "bin", "kafka")
  conf_dir = os.path.join(kafka_home, "config")

kafka_user = config['configurations']['kafka-env']['kafka_user']
kafka_log_dir = config['configurations']['kafka-env']['kafka_log_dir']
kafka_pid_dir = status_params.kafka_pid_dir
kafka_pid_file = kafka_pid_dir + "/kafka.pid"
kafka_err_file = kafka_log_dir + "/kafka.err"
# This is hardcoded on the kafka bash process lifecycle on which we have no control over
kafka_managed_pid_dir = "/var/run/kafka"
kafka_managed_log_dir = "/var/log/kafka"
user_group = config['configurations']['cluster-env']['user_group']
java64_home = config['ambariLevelParams']['java_home']
kafka_env_sh_template = config['configurations']['kafka-env']['content']
kafka_jaas_conf_template = default("/configurations/kafka_jaas_conf/content", None)
kafka_client_jaas_conf_template = default("/configurations/kafka_client_jaas_conf/content", None)
kafka_hosts = config['clusterHostInfo']['kafka_broker_hosts']
kafka_hosts.sort()

zookeeper_hosts = config['clusterHostInfo']['zookeeper_server_hosts']
zookeeper_hosts.sort()
secure_acls = default("/configurations/kafka-broker/zookeeper.set.acl", False)
kafka_security_migrator = os.path.join(kafka_home, "bin", "zookeeper-security-migration.sh")

all_hosts = default("/clusterHostInfo/all_hosts", [])
all_racks = default("/clusterHostInfo/all_racks", [])

#Kafka log4j
kafka_log_maxfilesize = default('/configurations/kafka-log4j/kafka_log_maxfilesize',256)
kafka_log_maxbackupindex = default('/configurations/kafka-log4j/kafka_log_maxbackupindex',20)
controller_log_maxfilesize = default('/configurations/kafka-log4j/controller_log_maxfilesize',256)
controller_log_maxbackupindex = default('/configurations/kafka-log4j/controller_log_maxbackupindex',20)

if (('kafka-log4j' in config['configurations']) and ('content' in config['configurations']['kafka-log4j'])):
    log4j_props = config['configurations']['kafka-log4j']['content']
else:
    log4j_props = None

if 'ganglia_server_hosts' in config['clusterHostInfo'] and \
    len(config['clusterHostInfo']['ganglia_server_hosts'])>0:
  ganglia_installed = True
  ganglia_server = config['clusterHostInfo']['ganglia_server_hosts'][0]
  ganglia_report_interval = 60
else:
  ganglia_installed = False

metric_collector_port = ""
metric_collector_protocol = ""
metric_truststore_path= default("/configurations/ams-ssl-client/ssl.client.truststore.location", "")
metric_truststore_type= default("/configurations/ams-ssl-client/ssl.client.truststore.type", "")
metric_truststore_password= default("/configurations/ams-ssl-client/ssl.client.truststore.password", "")

set_instanceId = "false"
cluster_name = config["clusterName"]

if 'cluster-env' in config['configurations'] and \
        'metrics_collector_external_hosts' in config['configurations']['cluster-env']:
  ams_collector_hosts = config['configurations']['cluster-env']['metrics_collector_external_hosts']
  set_instanceId = "true"
else:
  ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))

has_metric_collector = not len(ams_collector_hosts) == 0

if has_metric_collector:
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

  host_in_memory_aggregation = str(default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation", True)).lower()
  host_in_memory_aggregation_port = default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.port", 61888)
  is_aggregation_https_enabled = False
  if default("/configurations/ams-site/timeline.metrics.host.inmemory.aggregation.http.policy", "HTTP_ONLY") == "HTTPS_ONLY":
    host_in_memory_aggregation_protocol = 'https'
    is_aggregation_https_enabled = True
  else:
    host_in_memory_aggregation_protocol = 'http'
  pass

# Security-related params
kerberos_security_enabled = config['configurations']['cluster-env']['security_enabled']
kafka_kerberos_merge_advertised_listeners = default('/configurations/kafka-env/kerberos_merge_advertised_listeners', True)

kafka_kerberos_enabled = (('security.inter.broker.protocol' in config['configurations']['kafka-broker']) and
                          (config['configurations']['kafka-broker']['security.inter.broker.protocol'] in ("PLAINTEXTSASL", "SASL_PLAINTEXT", "SASL_SSL")))

kafka_other_sasl_enabled = not kerberos_security_enabled and check_stack_feature(StackFeature.KAFKA_LISTENERS, stack_version_formatted) and \
                           check_stack_feature(StackFeature.KAFKA_EXTENDED_SASL_SUPPORT, format_stack_version(version_for_stack_feature_checks)) and \
                           (("SASL_PLAINTEXT" in config['configurations']['kafka-broker']['listeners']) or
                            ("PLAINTEXTSASL" in config['configurations']['kafka-broker']['listeners']) or #to support backward compability (we'll replace this anyway before we write it to server.properties)
                            ("SASL_SSL" in config['configurations']['kafka-broker']['listeners']))

if kerberos_security_enabled and stack_version_formatted != "" and 'kafka_principal_name' in config['configurations']['kafka-env'] \
  and check_stack_feature(StackFeature.KAFKA_KERBEROS, stack_version_formatted):
    _hostname_lowercase = config['agentLevelParams']['hostname'].lower()
    _kafka_principal_name = config['configurations']['kafka-env']['kafka_principal_name']
    kafka_jaas_principal = _kafka_principal_name.replace('_HOST',_hostname_lowercase)
    kafka_keytab_path = config['configurations']['kafka-env']['kafka_keytab']
    kafka_bare_jaas_principal = get_bare_principal(_kafka_principal_name)
    kafka_kerberos_params = "-Djava.security.auth.login.config="+ conf_dir +"/kafka_jaas.conf"
elif kafka_other_sasl_enabled:
  kafka_kerberos_params = "-Djava.security.auth.login.config="+ conf_dir +"/kafka_jaas.conf"
else:
    kafka_kerberos_params = ''
    kafka_jaas_principal = None
    kafka_keytab_path = None

namenode_hosts = default("/clusterHostInfo/namenode_hosts", [])
has_namenode = not len(namenode_hosts) == 0

hdfs_user = config['configurations']['hadoop-env']['hdfs_user'] if has_namenode else None
hdfs_user_keytab = config['configurations']['hadoop-env']['hdfs_user_keytab'] if has_namenode else None
hdfs_principal_name = config['configurations']['hadoop-env']['hdfs_principal_name'] if has_namenode else None
hdfs_site = config['configurations']['hdfs-site'] if has_namenode else None
default_fs = config['configurations']['core-site']['fs.defaultFS'] if has_namenode else None
hadoop_bin_dir = stack_select.get_hadoop_dir("bin") if has_namenode else None
hadoop_conf_dir = conf_select.get_hadoop_conf_dir() if has_namenode else None
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
dfs_type = default("/clusterLevelParams/dfs_type", "")
mount_table_xml_inclusion_file_full_path = None
mount_table_content = None
if 'viewfs-mount-table' in config['configurations']:
  xml_inclusion_file_name = 'viewfs-mount-table.xml'
  mount_table = config['configurations']['viewfs-mount-table']

  if 'content' in mount_table and mount_table['content'].strip():
    mount_table_xml_inclusion_file_full_path = os.path.join(conf_dir, xml_inclusion_file_name)
    mount_table_content = mount_table['content']

import functools
#create partial functions with common arguments for every HdfsResource call
#to create/delete hdfs directory/file/copyfromlocal we need to call params.HdfsResource in code
HdfsResource = functools.partial(
  HdfsResource,
  user = hdfs_user,
  hdfs_resource_ignore_file = "/var/lib/ambari-agent/data/.hdfs_resource_ignore",
  security_enabled = kerberos_security_enabled,
  keytab = hdfs_user_keytab,
  kinit_path_local = kinit_path_local,
  hadoop_bin_dir = hadoop_bin_dir,
  hadoop_conf_dir = hadoop_conf_dir,
  principal_name = hdfs_principal_name,
  hdfs_site = hdfs_site,
  default_fs = default_fs,
  immutable_paths = get_not_managed_resources(),
  dfs_type = dfs_type,
)
