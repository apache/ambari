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

config = Script.get_config()

hadoop_pid_dir_prefix = config['configurations']['hadoop-env']['hadoop_pid_dir_prefix']
hdfs_user = config['configurations']['hadoop-env']['hdfs_user']
hadoop_pid_dir = format("{hadoop_pid_dir_prefix}/{hdfs_user}")
datanode_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-datanode.pid")
namenode_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-namenode.pid")
snamenode_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-secondarynamenode.pid")
journalnode_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-journalnode.pid")
zkfc_pid_file = format("{hadoop_pid_dir}/hadoop-{hdfs_user}-zkfc.pid")
