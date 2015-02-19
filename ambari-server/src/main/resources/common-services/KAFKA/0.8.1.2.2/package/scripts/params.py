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
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions
from resource_management.libraries.functions.default import default
from resource_management.core.logger import Logger

import status_params

# server configurations
config = Script.get_config()

stack_name = default("/hostLevelParams/stack_name", None)

version = default("/commandParams/version", None)

stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

if hdp_stack_version != "" and compare_versions(hdp_stack_version, '2.2') >= 0:
    kafka_home = '/usr/hdp/current/kafka-broker/'
    kafka_bin = kafka_home+'bin/kafka'
else:
    kafka_home = '/usr/lib/kafka/'
    kafka_bin = kafka_home+'/bin/kafka'


conf_dir = "/etc/kafka/conf"
kafka_user = config['configurations']['kafka-env']['kafka_user']
kafka_log_dir = config['configurations']['kafka-env']['kafka_log_dir']
kafka_pid_dir = status_params.kafka_pid_dir
kafka_pid_file = kafka_pid_dir+"/kafka.pid"
hostname = config['hostname']
user_group = config['configurations']['cluster-env']['user_group']
java64_home = config['hostLevelParams']['java_home']
kafka_env_sh_template = config['configurations']['kafka-env']['content']
kafka_hosts = config['clusterHostInfo']['kafka_broker_hosts']
kafka_hosts.sort()

zookeeper_hosts = config['clusterHostInfo']['zookeeper_hosts']
zookeeper_hosts.sort()

if (('kafka-log4j' in config['configurations']) and ('content' in config['configurations']['kafka-log4j'])):
    log4j_props = config['configurations']['kafka-log4j']['content']
else:
    log4j_props = None

if 'ganglia_server_host' in config['clusterHostInfo'] and \
    len(config['clusterHostInfo']['ganglia_server_host'])>0:
  ganglia_installed = True
  ganglia_server = config['clusterHostInfo']['ganglia_server_host'][0]
  ganglia_report_interval = 60
else:
  ganglia_installed = False

kafka_metrics_reporters=""
metric_collector_host = ""
metric_collector_port = ""

if ganglia_installed:
  kafka_metrics_reporters = "kafka.ganglia.KafkaGangliaMetricsReporter"

ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0

if has_metric_collector:
  metric_collector_host = ams_collector_hosts[0]
  metric_collector_port = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
  if metric_collector_port and metric_collector_port.find(':') != -1:
    metric_collector_port = metric_collector_port.split(':')[1]

  if not len(kafka_metrics_reporters) == 0:
      kafka_metrics_reporters = kafka_metrics_reporters + ','

  kafka_metrics_reporters = kafka_metrics_reporters + "org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter"


# Security-related params
security_enabled = config['configurations']['cluster-env']['security_enabled']
