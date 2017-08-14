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

def solr_service(action='start'):
  import params
  cmd = format("{solr_home}/bin/solr")

  if action == 'start':

    if params.security_enabled:
      if params.solr_principal is None:
        solr_principal_with_host = 'missing_principal'
      else:
        solr_principal_with_host = params.solr_principal.replace("_HOST", params.hostname)
      kinit_cmd = format("{kinit_path_local} -kt {solr_keytab} {solr_principal_with_host};")
      Execute(kinit_cmd,user=params.solr_user)

    Execute (params.solr_home+'/server/scripts/cloud-scripts/zkcli.sh -zkhost ' + params.zookeeper_hosts_list + ' -cmd makepath ' + params.zookeeper_chroot, user=params.solr_user, ignore_failures=True )

    if (params.upgrade_direction is not None and params.upgrade_direction == Direction.UPGRADE) or (compare_versions(format_stack_version(params.version), '4.2.0.0') >= 0):
      solr_home_dir = params.solr_data_dir
    else:
      solr_home_dir = params.lib_dir + "/data"

    daemon_cmd = format("SOLR_INCLUDE={solr_conf_dir}/solr.in.sh {cmd} start -c -s {solr_home_dir} -V")
    no_op_test = format("ls {solr_pid_file} >/dev/null 2>&1 && ps `cat {solr_pid_file}` >/dev/null 2>&1")
    Execute(daemon_cmd,
            not_if=no_op_test,
            user=params.solr_user
    )
  elif action == 'stop':
    daemon_cmd = format("SOLR_INCLUDE={solr_conf_dir}/solr.in.sh {cmd} stop")
    no_op_test = format("! ((`SOLR_INCLUDE={solr_conf_dir}/solr.in.sh {cmd} status |grep process |wc -l`))")
    rm_pid = format("rm -f {solr_pid_file}")
    Execute(daemon_cmd,
            not_if=no_op_test,
            user=params.solr_user
    )
    Execute(rm_pid)
