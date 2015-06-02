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
from status_params import *

config = Script.get_config()
falcon_runtime_properties = config['configurations']['falcon-runtime.properties']
falcon_startup_properties = config['configurations']['falcon-startup.properties']
falcon_env_sh_template = config['configurations']['falcon-env']['content']

falcon_host = config['clusterHostInfo']['falcon_server_hosts'][0]
falcon_port = config['configurations']['falcon-env']['falcon_port']

falcon_conf_dir = "."
falcon_data_dir = "."
falcon_home = None
falcon_log_dir = "."

if os.environ.has_key("HADOOP_HOME"):
  hdp_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"], ".."))

if os.environ.has_key("FALCON_CONF_DIR"):
  falcon_conf_dir = os.environ["FALCON_CONF_DIR"]
if os.environ.has_key("FALCON_DATA_DIR"):
  falcon_data_dir = os.environ["FALCON_DATA_DIR"]
if os.environ.has_key("FALCON_HOME"):
  falcon_home = os.environ["FALCON_HOME"]
if os.environ.has_key("FALCON_LOG_DIR"):
  falcon_log_dir = os.environ["FALCON_LOG_DIR"]
