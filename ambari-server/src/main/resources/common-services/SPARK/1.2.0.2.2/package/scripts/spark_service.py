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

from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions import format
from resource_management.core.resources.system import File, Execute

def spark_service(action):
  import params
  
  if action == 'start':
    if params.security_enabled:
      spark_kinit_cmd = format("{kinit_path_local} -kt {spark_kerberos_keytab} {spark_principal}; ")
      Execute(spark_kinit_cmd, user=params.spark_user)

    # Spark 1.3.1.2.3, and higher, which was included in HDP 2.3, does not have a dependency on Tez, so it does not
    # need to copy the tarball, otherwise, copy it.
    if params.hdp_stack_version and compare_versions(params.hdp_stack_version, '2.3.0.0') < 0:
      resource_created = copy_to_hdfs("tez", params.user_group, params.hdfs_user)
      if resource_created:
        params.HdfsResource(None, action="execute")

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