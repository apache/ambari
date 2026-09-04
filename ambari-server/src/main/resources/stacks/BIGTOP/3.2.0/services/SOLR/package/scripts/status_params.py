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

from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import stack_select

import solr_utils

config = Script.get_config()

component_directory = stack_select.get_package_name(default_package="solr-server")

solr_port = str(
  solr_utils.bounded_int(
    default("configurations/solr-env/solr_port", "8983"),
    "Solr port",
    1,
    65535,
  )
)
solr_piddir = solr_utils.validate_service_directory(
  default("configurations/solr-env/solr_pid_dir", "/var/run/solr"),
  "Solr PID directory",
)
solr_pidfile = format("{solr_piddir}/solr-{solr_port}.pid")
solr_user = solr_utils.validate_user(
  default("configurations/solr-env/solr_user", "solr"), "Solr user"
)
solr_datadir = solr_utils.validate_service_directory(
  default("configurations/solr-env/solr_datadir", "/var/lib/solr/data"),
  "Solr data directory",
)
user_group = solr_utils.validate_user(
  default("configurations/cluster-env/user_group", "hadoop"), "Hadoop group"
)

security_enabled = solr_utils.as_bool(
  config["configurations"]["cluster-env"]["security_enabled"],
  "Cluster security setting",
)
kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
