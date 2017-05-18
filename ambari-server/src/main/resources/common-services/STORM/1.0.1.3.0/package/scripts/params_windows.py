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

from status_params import *
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.default import default

# server configurations
config = Script.get_config()

stack_is_hdp23_or_further = Script.is_stack_greater_or_equal("2.3")

stack_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"],".."))
conf_dir = os.environ["STORM_CONF_DIR"]
hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
storm_user = hadoop_user

security_enabled = config['configurations']['cluster-env']['security_enabled']
default_topology_max_replication_wait_time_sec = default('/configurations/storm-site/topology.max.replication.wait.time.sec.default', -1)
nimbus_hosts = default("/clusterHostInfo/nimbus_hosts", [])
default_topology_min_replication_count = default('/configurations/storm-site/topology.min.replication.count.default', 1)

#Calculate topology.max.replication.wait.time.sec and topology.min.replication.count
if len(nimbus_hosts) > 1:
  # for HA Nimbus
  actual_topology_max_replication_wait_time_sec = -1
  actual_topology_min_replication_count = len(nimbus_hosts) / 2 + 1
else:
  # for non-HA Nimbus
  actual_topology_max_replication_wait_time_sec = default_topology_max_replication_wait_time_sec
  actual_topology_min_replication_count = default_topology_min_replication_count

if stack_is_hdp23_or_further:
  if security_enabled:
    storm_thrift_transport = config['configurations']['storm-site']['_storm.thrift.secure.transport']
  else:
    storm_thrift_transport = config['configurations']['storm-site']['_storm.thrift.nonsecure.transport']

service_map = {
  "nimbus" : nimbus_win_service_name,
  "supervisor" : supervisor_win_service_name,
  "ui" : ui_win_service_name
}
