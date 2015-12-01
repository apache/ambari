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

from resource_management.libraries.functions import format
from resource_management.libraries.script.script import Script

config = Script.get_config()

spark_user = config['configurations']['spark-env']['spark_user']
spark_group = config['configurations']['spark-env']['spark_group']
user_group = config['configurations']['cluster-env']['user_group']

if 'hive-env' in config['configurations']:
  hive_user = config['configurations']['hive-env']['hive_user']
else:
  hive_user = "hive"

spark_pid_dir = config['configurations']['spark-env']['spark_pid_dir']
spark_history_server_pid_file = format("{spark_pid_dir}/spark-{spark_user}-org.apache.spark.deploy.history.HistoryServer-1.pid")
spark_thrift_server_pid_file = format("{spark_pid_dir}/spark-{hive_user}-org.apache.spark.sql.hive.thriftserver.HiveThriftServer2-1.pid")
