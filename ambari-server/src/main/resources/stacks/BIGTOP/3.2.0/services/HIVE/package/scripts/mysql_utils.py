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

# Local Imports
import mysql_users

# Ambari Commons & Resource Management Imports
from resource_management.core.resources.system import Directory, File
from resource_management.core.source import InlineTemplate


def mysql_configure():
  import params

  Directory(
    params.mysql_conf_dir,
    owner="root",
    group="root",
    mode=0o755,
    create_parents=True,
  )
  File(
    f"{params.mysql_conf_dir}/99-ambari-hive.cnf",
    owner="root",
    group="root",
    mode=0o644,
    content=InlineTemplate("[mysqld]\nbind-address = 0.0.0.0\n"),
  )

  mysql_users.mysql_adduser()
