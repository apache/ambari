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
from resource_management.libraries import functions

def hive(name=None):
  import params
  XmlConfig("hive-site.xml",
            conf_dir = params.hive_conf_dir,
            configurations = params.config['configurations']['hive-site'],
            owner=params.hive_user,
            configuration_attributes=params.config['configuration_attributes']['hive-site']
  )
  if name in ["hiveserver2","metastore"]:
    Execute(format("cmd /c hadoop fs -mkdir -p {hive_warehouse_dir}"), logoutput=True, user=params.hadoop_user)

  if name == 'metastore':
    if params.init_metastore_schema:
      check_schema_created_cmd = format('cmd /c "{hive_bin}\\hive.cmd --service schematool -info '
                                        '-dbType {hive_metastore_db_type} '
                                        '-userName {hive_metastore_user_name} '
                                        '-passWord {hive_metastore_user_passwd!p}'
                                        '&set EXITCODE=%ERRORLEVEL%&exit /B %EXITCODE%"', #cmd "feature", propagate the process exit code manually
                                        hive_bin=params.hive_bin,
                                        hive_metastore_db_type=params.hive_metastore_db_type,
                                        hive_metastore_user_name=params.hive_metastore_user_name,
                                        hive_metastore_user_passwd=params.hive_metastore_user_passwd)
      try:
        Execute(check_schema_created_cmd)
      except Fail:
        create_schema_cmd = format('cmd /c {hive_bin}\\hive.cmd --service schematool -initSchema '
                                   '-dbType {hive_metastore_db_type} '
                                   '-userName {hive_metastore_user_name} '
                                   '-passWord {hive_metastore_user_passwd!p}',
                                   hive_bin=params.hive_bin,
                                   hive_metastore_db_type=params.hive_metastore_db_type,
                                   hive_metastore_user_name=params.hive_metastore_user_name,
                                   hive_metastore_user_passwd=params.hive_metastore_user_passwd)
        Execute(create_schema_cmd,
                user = params.hive_user,
                logoutput=True
      )
