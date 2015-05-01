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
import nturl2path
config = Script.get_config()
ams_collector_hosts = default("/clusterHostInfo/metrics_collector_hosts", [])
has_metric_collector = not len(ams_collector_hosts) == 0
if has_metric_collector:
  metric_collector_host = ams_collector_hosts[0]
  metric_collector_port = default("/configurations/ams-site/timeline.metrics.service.webapp.address", "0.0.0.0:6188")
  if metric_collector_port and metric_collector_port.find(':') != -1:
    metric_collector_port = metric_collector_port.split(':')[1]
  pass
  sink_home = os.environ["SINK_HOME"]
  timeline_plugin_url = "file:"+nturl2path.pathname2url(os.path.join(sink_home, "hadoop-sink", "ambari-metrics-hadoop-sink.jar"))



hadoop_conf_dir = os.environ["HADOOP_CONF_DIR"]
hbase_conf_dir = os.environ["HBASE_CONF_DIR"]
hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
