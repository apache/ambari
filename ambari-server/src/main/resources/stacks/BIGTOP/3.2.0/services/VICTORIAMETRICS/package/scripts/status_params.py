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

from resource_management.libraries.functions.default import default


role = default("/role", "VICTORIAMETRICS_SERVER")
component_names = {
  "VICTORIAMETRICS_SERVER": "server",
  "VMSTORAGE": "vmstorage",
  "VMINSERT": "vminsert",
  "VMSELECT": "vmselect",
  "VMAGENT": "vmagent",
  "VMAUTH": "vmauth",
}
component_name = component_names.get(role, "server")
victoriametrics_pid_dir = default(
  "/configurations/victoriametrics-env/victoriametrics_pid_dir",
  "/var/run/ambari-metrics/victoriametrics",
)
pid_file = os.path.join(victoriametrics_pid_dir, component_name + ".pid")
