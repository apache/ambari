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

def oozie_service(action = 'start'): # 'start' or 'stop'
  import params

  kinit_if_needed = format("{kinit_path_local} -kt {oozie_keytab} {oozie_principal};") if params.security_enabled else ""
  no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")
  
  if action == 'start':
    start_cmd = format("cd {oozie_tmp_dir} && /usr/lib/oozie/bin/oozie-start.sh")
    
    if params.jdbc_driver_name == "com.mysql.jdbc.Driver" or \
       params.jdbc_driver_name == "org.postgresql.Driver" or \
       params.jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
      db_connection_check_command = format("{java_home}/bin/java -cp {check_db_connection_jar}:{jdbc_driver_jar} org.apache.ambari.server.DBConnectionVerification {oozie_jdbc_connection_url} {oozie_metastore_user_name} {oozie_metastore_user_passwd!p} {jdbc_driver_name}")
    else:
      db_connection_check_command = None
      
    cmd1 =  format("cd {oozie_tmp_dir} && /usr/lib/oozie/bin/ooziedb.sh create -sqlfile oozie.sql -run")
    cmd2 =  format("{kinit_if_needed} hadoop dfs -put /usr/lib/oozie/share {oozie_hdfs_user_dir} ; hadoop dfs -chmod -R 755 {oozie_hdfs_user_dir}/share")
      
    if db_connection_check_command:
      Execute( db_connection_check_command)
                  
    Execute( cmd1,
      user = params.oozie_user,
      not_if  = no_op_test,
      ignore_failures = True
    ) 
    
    Execute( cmd2,
      user = params.oozie_user,       
      not_if = format("{kinit_if_needed} hadoop dfs -ls /user/oozie/share | awk 'BEGIN {{count=0;}} /share/ {{count++}} END {{if (count > 0) {{exit 0}} else {{exit 1}}}}'")
    )
    
    Execute( start_cmd,
      user = params.oozie_user,
      not_if  = no_op_test,
    )
  elif action == 'stop':
    stop_cmd  = format("su - {oozie_user} -c  'cd {oozie_tmp_dir} && /usr/lib/oozie/bin/oozie-stop.sh' && rm -f {pid_file}")
    Execute( stop_cmd,
      only_if  = no_op_test
    )

  
  
