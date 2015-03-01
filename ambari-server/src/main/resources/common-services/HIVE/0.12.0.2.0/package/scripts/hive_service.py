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
import sys
import os
import time
from resource_management.core import shell

def hive_service(name, action='start', rolling_restart=False):

  import params

  if name == 'metastore':
    pid_file = format("{hive_pid_dir}/{hive_metastore_pid}")
    cmd = format(
      "env HADOOP_HOME={hadoop_home} JAVA_HOME={java64_home} {start_metastore_path} {hive_log_dir}/hive.out {hive_log_dir}/hive.log {pid_file} {hive_server_conf_dir} {hive_log_dir}")
  elif name == 'hiveserver2':
    pid_file = format("{hive_pid_dir}/{hive_pid}")
    cmd = format(
      "env JAVA_HOME={java64_home} {start_hiveserver2_path} {hive_log_dir}/hive-server2.out {hive_log_dir}/hive-server2.log {pid_file} {hive_server_conf_dir} {hive_log_dir}")

  process_id_exists_command = format("ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1")

  if action == 'start':
    if name == 'hiveserver2':
      check_fs_root()

    demon_cmd = format("{cmd}")

    # upgrading hiveserver2 (rolling_restart) means that there is an existing,
    # de-registering hiveserver2; the pid will still exist, but the new
    # hiveserver is spinning up on a new port, so the pid will be re-written
    if rolling_restart:
      process_id_exists_command = None

    if params.security_enabled:
      hive_kinit_cmd = format("{kinit_path_local} -kt {hive_server2_keytab} {hive_principal}; ")
      Execute(hive_kinit_cmd, user=params.hive_user)

    # need tuple to run as sudo if (need if UMASK is not 022)
    oldmask = os.umask (022)
    os.umask (oldmask)
    if oldmask == 027:
      Execute(tuple(demon_cmd.split()), user=params.hive_user,
        environment={'HADOOP_HOME': params.hadoop_home}, path=params.execute_path,
        not_if=process_id_exists_command,
        sudo=True )
    else:
      Execute(demon_cmd, user=params.hive_user,
        environment={'HADOOP_HOME': params.hadoop_home}, path=params.execute_path,
        not_if=process_id_exists_command )      

    if params.hive_jdbc_driver == "com.mysql.jdbc.Driver" or \
       params.hive_jdbc_driver == "org.postgresql.Driver" or \
       params.hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver":
      
      db_connection_check_command = format(
        "{java64_home}/bin/java -cp {check_db_connection_jar}:{target} org.apache.ambari.server.DBConnectionVerification '{hive_jdbc_connection_url}' {hive_metastore_user_name} {hive_metastore_user_passwd!p} {hive_jdbc_driver}")
      
      Execute(db_connection_check_command,
              path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin', tries=5, try_sleep=10)
      
    # AMBARI-5800 - wait for the server to come up instead of just the PID existance
    if name == 'hiveserver2':
      SOCKET_WAIT_SECONDS = 120
      address=params.hostname
      port=int(params.hive_server_port)
      
      start_time = time.time()
      end_time = start_time + SOCKET_WAIT_SECONDS

      is_service_socket_valid = False
      print "Waiting for the Hive server to start..."
      if params.security_enabled:
        kinitcmd=format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal}; ")
      else:
        kinitcmd=None
      while time.time() < end_time:
        try:
          check_thrift_port_sasl(address, port, params.hive_server2_authentication,
                                 params.hive_server_principal, kinitcmd, params.smokeuser,
                                 transport_mode=params.hive_transport_mode)
          is_service_socket_valid = True
          break
        except Exception, e:
          time.sleep(5)

      elapsed_time = time.time() - start_time
      
      if is_service_socket_valid == False: 
        raise Fail("Connection to Hive server %s on port %s failed after %d seconds" % (address, port, elapsed_time))
      
      print "Successfully connected to Hive at %s on port %s after %d seconds" % (address, port, elapsed_time)    
            
  elif action == 'stop':

    daemon_kill_cmd = format("{sudo} kill `cat {pid_file}`")
    daemon_hard_kill_cmd = format("{sudo} kill -9 `cat {pid_file}`")

    Execute(daemon_kill_cmd,
      not_if = format("! ({process_id_exists_command})")
    )

    wait_time = 5
    Execute(daemon_hard_kill_cmd,
      not_if = format("! ({process_id_exists_command}) || ( sleep {wait_time} && ! ({process_id_exists_command}) )")
    )

    # check if stopped the process, else fail the task
    Execute(format("! ({process_id_exists_command})"),
      tries=20,
      try_sleep=3,
    )

    File(pid_file,
         action = "delete"
    )

def check_fs_root():
  import params  
  fs_root_url = format("{fs_root}{hive_apps_whs_dir}")
  metatool_cmd = format("hive --config {hive_server_conf_dir} --service metatool")
  cmd = format("{metatool_cmd} -listFSRoot 2>/dev/null | grep hdfs:// | grep -v '.db$'")
  code, out = shell.call(cmd, user=params.hive_user, env={'PATH' : params.execute_path })
  if code == 0 and fs_root_url.strip() != out.strip():
    cmd = format("{metatool_cmd} -updateLocation {fs_root}{hive_apps_whs_dir} {out}")
    Execute(cmd,
            environment= {'PATH' : params.execute_path },
            user=params.hive_user)
