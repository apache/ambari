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
from resource_management import *

def oozie_service(action = 'start', rolling_restart=False):
  """
  Starts or stops the Oozie service
  :param action: 'start' or 'stop'
  :param rolling_restart: if True, then most of the pre-startup checks are
  skipped since a variation of them was performed during the rolling upgrade
  :return:
  """
  import params

  if params.security_enabled:
    if params.oozie_principal is None:
      oozie_principal_with_host = 'missing_principal'
    else:
      oozie_principal_with_host = params.oozie_principal.replace("_HOST", params.hostname)
    kinit_if_needed = format("{kinit_path_local} -kt {oozie_keytab} {oozie_principal_with_host};")
  else:
    kinit_if_needed = ""

  no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1")
  
  if action == 'start':
    start_cmd = format("cd {oozie_tmp_dir} && {oozie_home}/bin/oozie-start.sh")
    
    if params.jdbc_driver_name == "com.mysql.jdbc.Driver" or \
       params.jdbc_driver_name == "com.microsoft.sqlserver.jdbc.SQLServerDriver" or \
       params.jdbc_driver_name == "org.postgresql.Driver" or \
       params.jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
      db_connection_check_command = format("{java_home}/bin/java -cp {check_db_connection_jar}:{target} org.apache.ambari.server.DBConnectionVerification '{oozie_jdbc_connection_url}' {oozie_metastore_user_name} {oozie_metastore_user_passwd!p} {jdbc_driver_name}")
    else:
      db_connection_check_command = None

    if not rolling_restart:
      cmd1 =  format("cd {oozie_tmp_dir} && {oozie_home}/bin/ooziedb.sh create -sqlfile oozie.sql -run")
      cmd2 =  format("{kinit_if_needed} {put_shared_lib_to_hdfs_cmd} ; hadoop --config {hadoop_conf_dir} dfs -chmod -R 755 {oozie_hdfs_user_dir}/share")

      if not os.path.isfile(params.target) and params.jdbc_driver_name == "org.postgresql.Driver":
        print format("ERROR: jdbc file {target} is unavailable. Please, follow next steps:\n" \
          "1) Download postgresql-9.0-801.jdbc4.jar.\n2) Create needed directory: mkdir -p {oozie_home}/libserver/\n" \
          "3) Copy postgresql-9.0-801.jdbc4.jar to newly created dir: cp /path/to/jdbc/postgresql-9.0-801.jdbc4.jar " \
          "{oozie_home}/libserver/\n4) Copy postgresql-9.0-801.jdbc4.jar to libext: cp " \
          "/path/to/jdbc/postgresql-9.0-801.jdbc4.jar {oozie_home}/libext/\n")
        exit(1)

      if db_connection_check_command:
        Execute( db_connection_check_command, tries=5, try_sleep=10)

      Execute( cmd1, user = params.oozie_user, not_if = no_op_test,
        ignore_failures = True )

      not_if_command = as_user(format("{kinit_if_needed} hadoop --config {hadoop_conf_dir} dfs -ls /user/oozie/share | awk 'BEGIN {{count=0;}} /share/ {{count++}} END {{if (count > 0) {{exit 0}} else {{exit 1}}}}'"),
                               params.oozie_user)
      Execute( cmd2, user = params.oozie_user, not_if = not_if_command,
        path = params.execute_path )
    
    Execute( start_cmd, user = params.oozie_user, not_if = no_op_test )

  elif action == 'stop':
    stop_cmd  = format("cd {oozie_tmp_dir} && {oozie_home}/bin/oozie-stop.sh")
    Execute(stop_cmd, only_if  = no_op_test, user = params.oozie_user)
    File(params.pid_file, action = "delete")

  
  
