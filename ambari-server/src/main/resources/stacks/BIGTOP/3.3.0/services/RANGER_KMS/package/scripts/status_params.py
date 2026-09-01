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

from resource_management.libraries.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

ranger_kms_pid_dir = default(
  "/configurations/kms-env/ranger_kms_pid_dir", "/var/run/ranger_kms"
)
ranger_kms_pid_file = format("{ranger_kms_pid_dir}/rangerkms.pid")
kms_user = default("/configurations/kms-env/kms_user", "kms")
kms_group = default("/configurations/kms-env/kms_group", "kms")
