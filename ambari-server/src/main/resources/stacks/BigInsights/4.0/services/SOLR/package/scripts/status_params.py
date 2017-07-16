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

from resource_management import *
import status_params

config = Script.get_config()

security_enabled = config['configurations']['cluster-env']['security_enabled']
solr_conf_dir='/usr/iop/current/solr-server/conf'
solr_user = config['configurations']['solr-env']['solr_user']
hostname = config['hostname']
kinit_path_local = functions.get_kinit_path()
tmp_dir = Script.get_tmp_dir()
solr_pid_dir = config['configurations']['solr-env']['solr_pid_dir']
solr_port = config['configurations']['solr-env']['solr_port']
solr_pid_file = format("{solr_pid_dir}/solr-{solr_port}.pid")