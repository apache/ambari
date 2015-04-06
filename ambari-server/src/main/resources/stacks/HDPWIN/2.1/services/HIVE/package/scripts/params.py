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

# server configurations
config = Script.get_config()

# This is expected to be of the form #.#.#.#
stack_version_unformatted = str(config['hostLevelParams']['stack_version'])
hdp_stack_version = format_hdp_stack_version(stack_version_unformatted)

hdp_root = os.path.abspath(os.path.join(os.environ["HADOOP_HOME"],".."))
hive_conf_dir = os.environ["HIVE_CONF_DIR"]
hive_home = os.environ["HIVE_HOME"]
hive_lib_dir = os.environ["HIVE_LIB_DIR"]
hive_log_dir = os.environ["HIVE_LOG_DIR"]
hive_opts = os.environ["HIVE_OPTS"]
hcat_home = os.environ["HCAT_HOME"]
hcat_config_dir = os.environ["WEBHCAT_CONF_DIR"]

hive_env_sh_template = config['configurations']['hive-env']['content']
hive_warehouse_dir = config['configurations']['hive-site']['hive.metastore.warehouse.dir']
hive_user = "hadoop"
hadoop_user = "hadoop"
hcat_user = "hadoop"

hive_bin = os.path.join(hive_home, "bin")
hive_metastore_db_type = config['configurations']['hive-env']['hive_database_type']
hive_metastore_user_name = config['configurations']['hive-site']['javax.jdo.option.ConnectionUserName']
hive_metastore_user_passwd = config['configurations']['hive-site']['javax.jdo.option.ConnectionPassword']

######## Metastore Schema
if hdp_stack_version != "" and compare_versions(hdp_stack_version, "2.1.0.0") < 0:
  init_metastore_schema = False
else:
  init_metastore_schema = True

