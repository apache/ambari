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

import os

from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.default import default
from resource_management.libraries.script.script import Script

import infra_solr_utils


config = Script.get_config()
infra_solr_port = infra_solr_utils.bounded_int(
  default("/configurations/infra-solr-env/infra_solr_port", 8886),
  "Infra Solr port",
  1,
  65535,
)
infra_solr_piddir = infra_solr_utils.validate_service_directory(
  default(
    "/configurations/infra-solr-env/infra_solr_pid_dir",
    "/var/run/ambari-infra-solr",
  ),
  "Infra Solr PID directory",
)
infra_solr_pidfile = os.path.join(infra_solr_piddir, f"solr-{infra_solr_port}.pid")
infra_solr_datadir = infra_solr_utils.validate_service_directory(
  default(
    "/configurations/infra-solr-env/infra_solr_datadir",
    "/var/lib/ambari-infra-solr/data",
  ),
  "Infra Solr data directory",
)
infra_solr_user = infra_solr_utils.validate_user(
  default("/configurations/infra-solr-env/infra_solr_user", "infra-solr"),
  "Infra Solr user",
)
user_group = infra_solr_utils.validate_user(
  config["configurations"]["cluster-env"]["user_group"],
  "Infra Solr group",
)
security_enabled = infra_solr_utils.as_bool(
  config["configurations"]["cluster-env"]["security_enabled"],
  "Cluster security setting",
)
kinit_path_local = get_kinit_path(
  default("/configurations/kerberos-env/executable_search_paths", None)
)
