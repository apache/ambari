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

if OSCheck.is_windows_family():
  from params_windows import *
else:
  from params_linux import *

hbase_pid_dir = config['configurations']['ams-hbase-env']['hbase_pid_dir']
hbase_user = ams_user
ams_collector_pid_dir = config['configurations']['ams-env']['metrics_collector_pid_dir']
ams_monitor_pid_dir = config['configurations']['ams-env']['metrics_monitor_pid_dir']
ams_grafana_pid_dir = config['configurations']['ams-grafana-env']['metrics_grafana_pid_dir']

security_enabled = config['configurations']['cluster-env']['security_enabled']
ams_hbase_conf_dir = format("{hbase_conf_dir}")

kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
hostname = config['hostname']
tmp_dir = Script.get_tmp_dir()
