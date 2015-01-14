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
from ambari_commons import OSCheck
import status_params
config = Script.get_config()

ams_collector_conf_dir = "/etc/ambari-metrics-collector/conf"
ams_monitor_conf_dir = "/etc/ambari-metrics-monitor/conf/"
ams_user = status_params.ams_user
#RPM versioning support
rpm_version = default("/configurations/hadoop-env/rpm_version", None)

#hadoop params
if rpm_version is not None:
  #RPM versioning support
  rpm_version = default("/configurations/hadoop-env/rpm_version", None)

if rpm_version is not None:
  hadoop_native_lib = format("/usr/hdp/current/hadoop-client/lib/native/")
  hadoop_bin_dir = format("/usr/hdp/current/hadoop/bin")
  daemon_script = format('/usr/hdp/current/hbase/bin/hbase-daemon.sh')
  region_mover = format('/usr/hdp/current/hbase/bin/region_mover.rb')
  region_drainer = format('/usr/hdp/current/hbase/bin/draining_servers.rb')
  hbase_cmd = format('/usr/hdp/current/hbase/bin/hbase')
else:
  hadoop_native_lib = format("/usr/lib/hadoop/lib/native")
  hadoop_bin_dir = "/usr/bin"
  daemon_script = "/usr/lib/hbase/bin/hbase-daemon.sh"
  region_mover = "/usr/lib/hbase/bin/region_mover.rb"
  region_drainer = "/usr/lib/hbase/bin/draining_servers.rb"
  hbase_cmd = "/usr/lib/hbase/bin/hbase"

hadoop_conf_dir = "/etc/hadoop/conf"
hbase_conf_dir = "/etc/ams-hbase/conf"
