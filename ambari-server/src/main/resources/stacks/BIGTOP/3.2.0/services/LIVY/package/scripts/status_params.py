#!/usr/bin/env python3
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

from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script


config = Script.get_config()

livy_user = config["configurations"]["livy-env"]["livy_user"]
livy_group = config["configurations"]["livy-env"]["livy_group"]
user_group = config["configurations"]["cluster-env"]["user_group"]
livy_pid_dir = config["configurations"]["livy-env"]["livy_pid_dir"]
livy_server_pid_file = format("{livy_pid_dir}/livy-{livy_user}-server.pid")
