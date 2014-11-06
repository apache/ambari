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

def webhcat_service(action='start'):
  import params

  cmd = format('env HADOOP_HOME={hadoop_home} {webhcat_bin_dir}/webhcat_server.sh')

  if action == 'start':
    demon_cmd = format('{cmd} start')
    no_op_test = format('ls {webhcat_pid_file} >/dev/null 2>&1 && ps -p `cat {webhcat_pid_file}` >/dev/null 2>&1')
    Execute(demon_cmd,
            user=params.webhcat_user,
            not_if=no_op_test
    )
  elif action == 'stop':
    demon_cmd = format('{cmd} stop')
    Execute(demon_cmd,
            user=params.webhcat_user
    )
    Execute(format('rm -f {webhcat_pid_file}'))
