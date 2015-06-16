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

from resource_management.libraries.functions.get_hdp_version import get_hdp_version
from resource_management.libraries.script.script import Script

config = Script.get_config()
hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
user_group = config["configurations"]["cluster-env"]["user_group"]
tez_user = hadoop_user
tez_home_dir = None
tez_conf_dir = "conf"

try:
  hadoop_classpath_prefix_template = config["configurations"]["tez-site"]["tez.cluster.additional.classpath.prefix"]
except KeyError:
  hadoop_classpath_prefix_template = ""

hdp_stack_version = ""

hdp_root = None
try:
  hdp_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"], ".."))
except:
  pass

def refresh_tez_state_dependent_params():
  global tez_home_dir, tez_conf_dir, hdp_stack_version
  tez_home_dir = os.environ["TEZ_HOME"]
  tez_conf_dir = os.path.join(tez_home_dir, "conf")
  # this is not available on INSTALL action because hdp-select is not available
  hdp_stack_version = get_hdp_version("tez")


if os.environ.has_key("TEZ_HOME"):
  refresh_tez_state_dependent_params()
