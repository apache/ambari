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
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.core.shell import as_user
from ambari_commons import OSCheck
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.libraries.functions.expect import expect
from resource_management.libraries.functions.version import format_stack_version, get_major_version

config = Script.get_config()

ams_collector_conf_dir = "/etc/ambari-metrics-collector/conf"
ams_monitor_conf_dir = "/etc/ambari-metrics-monitor/conf/"
ams_user = config['configurations']['ams-env']['ambari_metrics_user']
#RPM versioning support
rpm_version = default("/configurations/hadoop-env/rpm_version", None)

ams_grafana_pid_dir = config['configurations']['ams-grafana-env']['metrics_grafana_pid_dir']

stack_version_unformatted = config['clusterLevelParams']['stack_version']
stack_version_formatted = format_stack_version(stack_version_unformatted)

major_stack_version = get_major_version(stack_version_formatted)

#hadoop params
if rpm_version is not None:
  #RPM versioning support
  rpm_version = default("/configurations/hadoop-env/rpm_version", None)

hadoop_native_lib = format("/usr/lib/ams-hbase/lib/hadoop-native")
hadoop_bin_dir = "/usr/bin"
daemon_script = "/usr/lib/ams-hbase/bin/hbase-daemon.sh"
region_mover = "/usr/lib/ams-hbase/bin/region_mover.rb"
region_drainer = "/usr/lib/ams-hbase/bin/draining_servers.rb"
hbase_cmd = "/usr/lib/ams-hbase/bin/hbase"

hadoop_conf_dir = conf_select.get_hadoop_conf_dir()
hbase_conf_dir = "/etc/ams-hbase/conf"

limits_conf_dir = "/etc/security/limits.d"
sudo = AMBARI_SUDO_BINARY

dfs_type = default("/clusterLevelParams/dfs_type", "")

hbase_regionserver_shutdown_timeout = expect('/configurations/ams-hbase-env/hbase_regionserver_shutdown_timeout', int,
                                             30)

grafana_pid_file = format("{ams_grafana_pid_dir}/grafana-server.pid")
grafana_process_exists_cmd = as_user(format("test -f {grafana_pid_file} && ps -p `cat {grafana_pid_file}`"), ams_user)
