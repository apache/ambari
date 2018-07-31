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
from ambari_commons.ambari_metrics_helper import select_metric_collector_hosts_from_hostnames

config = Script.get_config()
ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))
if 'cluster-env' in config['configurations'] and \
    'metrics_collector_external_hosts' in config['configurations']['cluster-env']:
  ams_collector_hosts = config['configurations']['cluster-env']['metrics_collector_external_hosts']
  set_instanceId = "true"
else:
  ams_collector_hosts = ",".join(default("/clusterHostInfo/metrics_collector_hosts", []))

has_metric_collector = not len(ams_collector_hosts) == 0
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

  sink_home = os.environ["SINK_HOME"]
  timeline_plugin_url = "file:"+nturl2path.pathname2url(os.path.join(sink_home, "hadoop-sink", "ambari-metrics-hadoop-sink.jar"))
  pass

hadoop_conf_dir = os.environ["HADOOP_CONF_DIR"]
hbase_conf_dir = os.environ["HBASE_CONF_DIR"]
hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
