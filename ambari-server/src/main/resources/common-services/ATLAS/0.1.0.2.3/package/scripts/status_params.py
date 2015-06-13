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
import os
from resource_management import Script
from resource_management.libraries.functions import  get_kinit_path, format
from resource_management.libraries.functions.default import default


config = Script.get_config()

conf_dir = os.environ['METADATA_CONF'] if 'METADATA_CONF' in os.environ else '/etc/atlas/conf'
pid_dir = config['configurations']['atlas-env']['metadata_pid_dir']
pid_file = format("{pid_dir}/atlas.pid")
metadata_user = config['configurations']['atlas-env']['metadata_user']

# Security related/required params
hostname = config['hostname']
security_enabled = config['configurations']['cluster-env']['security_enabled']
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
tmp_dir = Script.get_tmp_dir()
