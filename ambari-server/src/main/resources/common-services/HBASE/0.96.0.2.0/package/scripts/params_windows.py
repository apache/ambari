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
import os
import status_params

# server configurations
config = Script.get_config()
hbase_conf_dir = os.environ["HBASE_CONF_DIR"]
hbase_bin_dir = os.path.join(os.environ["HBASE_HOME"],'bin')
hbase_executable = os.path.join(hbase_bin_dir,"hbase.cmd")
hdp_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"],".."))
hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
hbase_user = hadoop_user

#decomm params
region_drainer = os.path.join(hbase_bin_dir,"draining_servers.rb")
region_mover = os.path.join(hbase_bin_dir,"region_mover.rb")
hbase_excluded_hosts = config['commandParams']['excluded_hosts']
hbase_drain_only = config['commandParams']['mark_draining_only']

service_map = {
  'master' : status_params.hbase_master_win_service_name,
  'regionserver' : status_params.hbase_regionserver_win_service_name
}
