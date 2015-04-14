#!/usr/bin/env python

'''
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
'''
from resource_management import *

def spark_service(action):
  import params
  
  if action == 'start':
    if params.security_enabled:
      spark_kinit_cmd = format("{kinit_path_local} -kt {spark_kerberos_keytab} {spark_principal}; ")
      Execute(spark_kinit_cmd, user=params.spark_user)

    copy_tarballs_to_hdfs('tez', 'spark-historyserver', params.spark_user, params.hdfs_user, params.user_group)

    no_op_test = format(
      'ls {spark_history_server_pid_file} >/dev/null 2>&1 && ps -p `cat {spark_history_server_pid_file}` >/dev/null 2>&1')
    Execute(format('{spark_history_server_start}'),
            user=params.spark_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=no_op_test
    )
  elif action == 'stop':
    Execute(format('{spark_history_server_stop}'),
            user=params.spark_user,
            environment={'JAVA_HOME': params.java_home}
    )
    File(params.spark_history_server_pid_file,
         action="delete"
    )