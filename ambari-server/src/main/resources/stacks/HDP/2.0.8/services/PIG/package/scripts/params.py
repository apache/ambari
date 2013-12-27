#!/usr/bin/env python2.6
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

Ambari Agent

"""

from resource_management import *

# server configurations
config = Script.get_config()

pig_conf_dir = "/etc/pig/conf"
hadoop_conf_dir = "/etc/hadoop/conf"
hdfs_user = config['configurations']['global']['hdfs_user']
smokeuser = config['configurations']['global']['smokeuser']
user_group = config['configurations']['global']['user_group']

# not supporting 32 bit jdk.
java64_home = config['configurations']['global']['java64_home']
hadoop_home = "/usr"