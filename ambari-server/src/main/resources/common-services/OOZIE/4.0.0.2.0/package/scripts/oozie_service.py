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
from resource_management.core.shell import as_user
from resource_management.libraries.providers.hdfs_resource import WebHDFSUtil
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def oozie_service(action='start', upgrade_type=None):
  import params

  if action == 'start':
    cmd = format("cmd /C \"cd /d {oozie_tmp_dir} && {oozie_home}\\bin\\ooziedb.cmd create -sqlfile oozie.sql -run\"")
    Execute(cmd, user=params.oozie_user, ignore_failures=True)
    Service(params.oozie_server_win_service_name, action="start")
  elif action == 'stop':
    Service(params.oozie_server_win_service_name, action="stop")

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def oozie_service(action = 'start', upgrade_type=None):
  """
  Starts or stops the Oozie service
  :param action: 'start' or 'stop'
  :param upgrade_type: type of upgrade, either "rolling" or "non_rolling"
  skipped since a variation of them was performed during the rolling upgrade
  :return:
  """
  import params

  environment={'OOZIE_CONFIG': params.conf_dir}

  if params.security_enabled:
    if params.oozie_principal is None:
      oozie_principal_with_host = 'missing_principal'
    else:
      oozie_principal_with_host = params.oozie_principal.replace("_HOST", params.hostname)
    kinit_if_needed = format("{kinit_path_local} -kt {oozie_keytab} {oozie_principal_with_host};")
  else:
    kinit_if_needed = ""

  no_op_test = as_user(format("ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1"), user=params.oozie_user)
  
  if action == 'start':
    start_cmd = format("cd {oozie_tmp_dir} && {oozie_home}/bin/oozie-start.sh")
    
    if params.jdbc_driver_name == "com.mysql.jdbc.Driver" or \
       params.jdbc_driver_name == "com.microsoft.sqlserver.jdbc.SQLServerDriver" or \
       params.jdbc_driver_name == "org.postgresql.Driver" or \
       params.jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
      db_connection_check_command = format("{java_home}/bin/java -cp {check_db_connection_jar}:{target} org.apache.ambari.server.DBConnectionVerification '{oozie_jdbc_connection_url}' {oozie_metastore_user_name} {oozie_metastore_user_passwd!p} {jdbc_driver_name}")
    else:
      db_connection_check_command = None

    if upgrade_type is None:
      if not os.path.isfile(params.target) and params.jdbc_driver_name == "org.postgresql.Driver":
        print format("ERROR: jdbc file {target} is unavailable. Please, follow next steps:\n" \
          "1) Download postgresql-9.0-801.jdbc4.jar.\n2) Create needed directory: mkdir -p {oozie_home}/libserver/\n" \
          "3) Copy postgresql-9.0-801.jdbc4.jar to newly created dir: cp /path/to/jdbc/postgresql-9.0-801.jdbc4.jar " \
          "{oozie_home}/libserver/\n4) Copy postgresql-9.0-801.jdbc4.jar to libext: cp " \
          "/path/to/jdbc/postgresql-9.0-801.jdbc4.jar {oozie_home}/libext/\n")
        exit(1)

      if db_connection_check_command:
        Execute( db_connection_check_command, 
                 tries=5, 
                 try_sleep=10,
                 user=params.oozie_user,
        )

      Execute( format("cd {oozie_tmp_dir} && {oozie_home}/bin/ooziedb.sh create -sqlfile oozie.sql -run"), 
               user = params.oozie_user, not_if = no_op_test,
               ignore_failures = True 
      )
      
      if params.security_enabled:
        Execute(kinit_if_needed,
                user = params.oozie_user,
        )
      
      
      if params.host_sys_prepped:
        print "Skipping creation of oozie sharelib as host is sys prepped"
        hdfs_share_dir_exists = True # skip time-expensive hadoop fs -ls check
      elif WebHDFSUtil.is_webhdfs_available(params.is_webhdfs_enabled, params.default_fs):
        # check with webhdfs is much faster than executing hadoop fs -ls. 
        util = WebHDFSUtil(params.hdfs_site, params.oozie_user, params.security_enabled)
        list_status = util.run_command(params.hdfs_share_dir, 'GETFILESTATUS', method='GET', ignore_status_codes=['404'], assertable_result=False)
        hdfs_share_dir_exists = ('FileStatus' in list_status)
      else:
        # have to do time expensive hadoop fs -ls check.
        hdfs_share_dir_exists = shell.call(format("{kinit_if_needed} hadoop --config {hadoop_conf_dir} dfs -ls {hdfs_share_dir} | awk 'BEGIN {{count=0;}} /share/ {{count++}} END {{if (count > 0) {{exit 0}} else {{exit 1}}}}'"),
                                 user=params.oozie_user)[0]
                                 
      if not hdfs_share_dir_exists:                      
        Execute( params.put_shared_lib_to_hdfs_cmd, 
                 user = params.oozie_user,
                 path = params.execute_path 
        )
        params.HdfsResource(format("{oozie_hdfs_user_dir}/share"),
                             type="directory",
                             action="create_on_execute",
                             mode=0755,
                             recursive_chmod=True,
        )
        params.HdfsResource(None, action="execute")
        

    # start oozie
    Execute( start_cmd, environment=environment, user = params.oozie_user,
      not_if = no_op_test )

  elif action == 'stop':
    stop_cmd  = format("cd {oozie_tmp_dir} && {oozie_home}/bin/oozie-stop.sh")

    # stop oozie
    Execute(stop_cmd, environment=environment, only_if  = no_op_test,
      user = params.oozie_user)

    File(params.pid_file, action = "delete")
