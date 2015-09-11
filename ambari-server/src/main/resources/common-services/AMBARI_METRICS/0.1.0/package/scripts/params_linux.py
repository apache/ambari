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
from resource_management.libraries.functions import conf_select
from ambari_commons import OSCheck
from ambari_commons.constants import AMBARI_SUDO_BINARY

config = Script.get_config()

ams_collector_conf_dir = "/etc/ambari-metrics-collector/conf"
ams_monitor_conf_dir = "/etc/ambari-metrics-monitor/conf/"
ams_user = config['configurations']['ams-env']['ambari_metrics_user']
#RPM versioning support
rpm_version = default("/configurations/hadoop-env/rpm_version", None)

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

dfs_type = default("/commandParams/dfs_type", "")

