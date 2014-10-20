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

from resource_management import *
import status_params

# server configurations
config = Script.get_config()

#RPM versioning support
rpm_version = default("/configurations/cluster-env/rpm_version", None)


if rpm_version:
    kafka_home = '/usr/hdp/current/kafka-broker/'
    kafka_bin = kafka_home+'bin/kafka'
else:
    kafka_home = '/usr/lib/kafka/'
    kafka_bin = kafka_home+'/bin/kafka'


conf_dir = "/etc/kafka/conf"
kafka_user = config['configurations']['kafka-env']['kafka_user']
log_dir = config['configurations']['kafka-env']['kafka_log_dir']
pid_dir = status_params.kafka_pid_dir
pid_file = pid_dir+"/kafka.pid"
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
