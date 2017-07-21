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
import sys

from resource_management.core.resources.system import Execute, File
from resource_management.core.logger import Logger
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.version import format_stack_version
from setup_solr import setup_solr
from setup_ranger_solr import setup_ranger_solr
from resource_management.libraries.functions import solr_cloud_util


class Solr(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    setup_solr(name = 'server')

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)

    if params.security_enabled:
      solr_kinit_cmd = format("{kinit_path_local} -kt {solr_kerberos_keytab} {solr_kerberos_principal}; ")
      Execute(solr_kinit_cmd, user=params.solr_user)

    if params.is_supported_solr_ranger:
       setup_ranger_solr() #Ranger Solr Plugin related call

    if params.restart_during_downgrade:
      solr_env = {'SOLR_INCLUDE': format('{solr_conf}/solr.in.sh')}
    else:
      solr_env = {'SOLR_INCLUDE': format('{solr_conf}/solr-env.sh')}
    Execute(
      format('{solr_bindir}/solr start -cloud -noprompt -s {solr_datadir} >> {solr_log} 2>&1'),
      environment=solr_env,
      user=params.solr_user
    )

    if 'ranger-env' in params.config['configurations'] and params.audit_solr_enabled:
      solr_cloud_util.upload_configuration_to_zk(
        zookeeper_quorum=params.zookeeper_quorum,
        solr_znode=params.solr_znode,
        config_set=params.ranger_solr_config_set,
        config_set_dir=params.ranger_solr_conf,
        tmp_dir=params.tmp_dir,
        java64_home=params.java64_home,
        jaas_file=params.solr_jaas_file,
        retry=30, interval=5)

      solr_cloud_util.create_collection(
        zookeeper_quorum=params.zookeeper_quorum,
        solr_znode=params.solr_znode,
        collection=params.ranger_solr_collection_name,
        config_set=params.ranger_solr_config_set,
        java64_home=params.java64_home,
        shards=params.ranger_solr_shards,
        replication_factor=int(params.replication_factor),
        jaas_file=params.solr_jaas_file)


  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    try:
      env = format('{solr_conf}/solr-env.sh')
      if not os.path.exists(env):
        old_env = format('{solr_conf}/solr.in.sh')
        if os.path.exists(old_env):
          env = old_env
        else:
          self.configure(env)

      no_op_test = format("! ((`SOLR_INCLUDE={env} {solr_bindir}/solr status | grep process | wc -l`))")
      Execute(format('{solr_bindir}/solr stop -all >> {solr_log}'),
              environment={'SOLR_INCLUDE': env},
              user=params.solr_user,
              not_if=no_op_test
              )

      File(params.solr_pidfile,
           action="delete"
           )
    except:
      Logger.warning("Could not stop solr:" + str(sys.exc_info()[1]) + "\n Trying to kill it")
      self.kill_process(params.solr_pidfile, params.solr_user, params.solr_log_dir)

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.solr_pidfile)

  def kill_process(self, pid_file, user, log_dir):
    import params
    """
    Kill the process by pid file, then check the process is running or not. If the process is still running after the kill
    command, it will try to kill with -9 option (hard kill)
    """
    pid = get_user_call_output(format("cat {pid_file}"), user=user, is_checked_call=False)[1]
    process_id_exists_command = format("ls {pid_file} >/dev/null 2>&1 && ps -p {pid} >/dev/null 2>&1")

    kill_cmd = format("{sudo} kill {pid}")
    Execute(kill_cmd,
          not_if=format("! ({process_id_exists_command})"))
    wait_time = 5

    hard_kill_cmd = format("{sudo} kill -9 {pid}")
    Execute(hard_kill_cmd,
          not_if=format("! ({process_id_exists_command}) || ( sleep {wait_time} && ! ({process_id_exists_command}) )"),
          ignore_failures=True)
    try:
      Execute(format("! ({process_id_exists_command})"),
            tries=20,
            try_sleep=3,
            )
    except:
      show_logs(log_dir, user)
      raise

    File(pid_file,
       action="delete"
       )


if __name__ == "__main__":
  Solr().execute()
