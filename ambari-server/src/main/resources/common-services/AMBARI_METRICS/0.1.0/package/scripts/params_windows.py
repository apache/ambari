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


config = Script.get_config()

hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
ams_user = hadoop_user

try:
  ams_collector_conf_dir = os.environ["COLLECTOR_CONF_DIR"]
  ams_collector_home_dir = os.environ["COLLECTOR_HOME"]
  hbase_cmd = os.path.join(os.environ["COLLECTOR_HOME"], "hbase", "bin", "hbase.cmd")
  hbase_conf_dir = os.path.join(os.environ["COLLECTOR_HOME"], "hbase", "conf")
except:
  ams_collector_conf_dir = None
  ams_collector_home_dir = None
  hbase_cmd = None
  hbase_conf_dir = None

try:
  ams_monitor_conf_dir = os.environ["MONITOR_CONF_DIR"]
  ams_monitor_home_dir = os.environ["MONITOR_HOME"]
except:
  ams_monitor_conf_dir = None
  ams_monitor_home_dir = None

hadoop_native_lib = os.path.join(os.environ["HADOOP_HOME"], "bin")
hadoop_bin_dir = os.path.join(os.environ["HADOOP_HOME"], "bin")
hadoop_conf_dir = os.path.join(os.environ["HADOOP_HOME"], "conf")

from service_mapping import *
