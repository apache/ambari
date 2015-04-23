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
import sys
import fileinput
import subprocess
import json
import re
import os
from resource_management import *
from resource_management.libraries.functions.ranger_functions import Rangeradmin
from resource_management.core.logger import Logger
from resource_management.libraries.functions.version import format_hdp_stack_version, compare_versions

def setup_ranger_hbase():
  import params
  
  if params.has_ranger_admin:
    File(params.downloaded_custom_connector,
         content = DownloadSource(params.driver_curl_source)
    )

    Execute(('cp', '--remove-destination', params.downloaded_custom_connector, params.driver_curl_target),
            path=["/bin", "/usr/bin/"],
            not_if=format("test -f {driver_curl_target}"),
            sudo=True)

    hdp_version = get_hdp_version('hbase-client')
    file_path = format('/usr/hdp/{hdp_version}/ranger-hbase-plugin/install.properties')
    
    if not os.path.isfile(file_path):
      raise Fail(format('Ranger HBase plugin install.properties file does not exist at {file_path}'))
    
    ModifyPropertiesFile(file_path,
      properties = params.config['configurations']['ranger-hbase-plugin-properties']
    )

    if params.enable_ranger_hbase:
      cmd = ('enable-hbase-plugin.sh',)
      
      ranger_adm_obj = Rangeradmin(url=params.policymgr_mgr_url)
      ranger_adm_obj.create_ranger_repository('hbase', params.repo_name, params.hbase_ranger_plugin_repo,
                                              params.ambari_ranger_admin, params.ambari_ranger_password, 
                                              params.admin_uname, params.admin_password, 
                                              params.policy_user)
    else:
      cmd = ('disable-hbase-plugin.sh',)
      
    cmd_env = {'JAVA_HOME': params.java64_home, 'PWD': format('/usr/hdp/{hdp_version}/ranger-hbase-plugin'), 'PATH': format('/usr/hdp/{hdp_version}/ranger-hbase-plugin')}
    
    Execute(cmd, 
					environment=cmd_env, 
					logoutput=True,
					sudo=True,
		)                    
  else:
    Logger.info('Ranger admin not installed')