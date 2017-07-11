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
import os
from resource_management import *
from resource_management.libraries.functions.validate import call_and_match_output

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

    Execute ('echo "Creating znode" ' + params.zookeeper_chroot)
    Execute (params.cloud_scripts + '/zkcli.sh -zkhost ' + params.zookeeper_hosts_list + ' -cmd makepath ' + params.zookeeper_chroot, user=params.solr_user, ignore_failures=True )

    # copy titan directory and jar for titan and solr integration
    if (('titan-env' in params.configuration_tags) and not (os.path.exists(params.solr_conf_trg_file))):
            Execute(("cp", "-r", params.titan_solr_conf_dir, params.solr_conf_trg_dir), sudo = True)
            Execute(("cp", params.titan_solr_jar_file, params.solr_jar_trg_file), sudo = True)
            Execute(("chmod", "644", params.solr_jar_trg_file), sudo=True)
            Execute(("mv", params.solr_solr_conf_dir, params.solr_titan_conf_dir), sudo = True)

    daemon_cmd = format("SOLR_INCLUDE={solr_conf_dir}/solr.in.sh {cmd} start -c -V")
    no_op_test = format("ls {solr_pid_file} >/dev/null 2>&1 && ps `cat {solr_pid_file}` >/dev/null 2>&1")
    Execute(daemon_cmd,
            not_if=no_op_test,
            user=params.solr_user
    )

    if not params.upgrade_direction: #Only do this for a fresh IOP 4.2 cluster
      # create collection for titan and solr integration
      if (('titan-env' in params.configuration_tags) and (os.path.exists(params.solr_conf_trg_file))):
          create_collection_cmd = format("SOLR_INCLUDE={solr_conf_dir}/solr.in.sh solr create -c {titan_solr_configset} -s 2 -d {titan_solr_configset}")
          create_collection_output = "success"
          create_collection_exists_output = format("Collection '{titan_solr_configset}' already exists!")
          call_and_match_output(create_collection_cmd, format("({create_collection_output})|({create_collection_exists_output})"), "Failed to create collection")

  elif action == 'stop':
    daemon_cmd = format("export SOLR_PID_DIR=" + params.pid_dir + "; SOLR_INCLUDE={solr_conf_dir}/solr.in.sh {cmd} stop -all")
    no_op_test = format("! ((`SOLR_INCLUDE={solr_conf_dir}/solr.in.sh {cmd} status |grep process |wc -l`))")
    rm_pid = format("rm -f {solr_pid_file}")
    Execute(daemon_cmd,
            not_if=no_op_test,
            user=params.solr_user
    )
    Execute(rm_pid)
