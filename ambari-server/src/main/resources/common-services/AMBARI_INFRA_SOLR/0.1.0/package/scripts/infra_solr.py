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

import sys
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.core.resources.zkmigrator import ZkMigrator
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.script.script import Script

from collection import backup_collection, restore_collection
from migrate import migrate_index
from setup_infra_solr import setup_infra_solr, setup_solr_znode_env

class InfraSolr(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    setup_infra_solr(name = 'server')

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)

    setup_solr_znode_env()
    start_cmd = format('{solr_bindir}/solr start -cloud -noprompt -s {infra_solr_datadir} -Dsolr.kerberos.name.rules=\'{infra_solr_kerberos_name_rules}\' >> {infra_solr_log} 2>&1') \
            if params.security_enabled else format('{solr_bindir}/solr start -cloud -noprompt -s {infra_solr_datadir} >> {infra_solr_log} 2>&1')
    Execute(
      start_cmd,
      environment={'SOLR_INCLUDE': format('{infra_solr_conf}/infra-solr-env.sh')},
      user=params.infra_solr_user
    )

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    try:
      Execute(format('{solr_bindir}/solr stop -all >> {infra_solr_log}'),
              environment={'SOLR_INCLUDE': format('{infra_solr_conf}/infra-solr-env.sh')},
              user=params.infra_solr_user,
              only_if=format("test -f {prev_infra_solr_pidfile}")
              )

      File(params.prev_infra_solr_pidfile,
           action="delete"
           )
    except:
      Logger.warning("Could not stop solr:" + str(sys.exc_info()[1]) + "\n Trying to kill it")
      self.kill_process(params.prev_infra_solr_pidfile, params.infra_solr_user, params.infra_solr_log_dir)

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.infra_solr_pidfile)

  def kill_process(self, pid_file, user, log_dir):
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

  def disable_security(self, env):
    import params
    if not params.infra_solr_znode:
      Logger.info("Skipping reverting ACL")
      return
    zkmigrator = ZkMigrator(
      zk_host=params.zk_quorum,
      java_exec=params.java_exec,
      java_home=params.java64_home,
      jaas_file=params.infra_solr_jaas_file,
      user=params.infra_solr_user)
    zkmigrator.set_acls(params.infra_solr_znode, 'world:anyone:crdwa')

  def backup(self, env):
    backup_collection(env)

  def restore(self, env):
    restore_collection(env)

  def migrate(self, env):
    migrate_index(env)

if __name__ == "__main__":
  InfraSolr().execute()