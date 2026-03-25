#!/usr/bin/env python3
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

HBase Thrift Server - Manages the HBase Thrift service.
Uses hbase-daemon.sh start thrift -p <port> --infoport <infoport>
"""

from resource_management.core.resources.system import Execute
from resource_management.core.shell import as_sudo
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.core.resources import File
import upgrade


class HbaseThrift(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    upgrade.prestart(env)

  def configure(self, env):
    import params
    env.set_params(params)
    thrift_site_config = params.config['configurations'].get('hbase-thrift-site', {})
    hbase_site_config = params.config['configurations'].get('hbase-site', {})
    hbase_env_config = params.config['configurations'].get('hbase-env', {})

    from hbase import hbase
    hbase(name='regionserver')  # Use same config as regionserver for thrift

    # Build an effective hbase-site.xml for Thrift without mutating immutable Ambari config dicts.
    effective_hbase_site_config = dict(hbase_site_config)
    if not effective_hbase_site_config.get('hbase.thrift.kerberos.principal'):
      thrift_principal = (
        effective_hbase_site_config.get('hbase.master.kerberos.principal')
        or effective_hbase_site_config.get('hbase.regionserver.kerberos.principal')
        or hbase_env_config.get('hbase_principal_name')
      )
      if thrift_principal:
        effective_hbase_site_config['hbase.thrift.kerberos.principal'] = thrift_principal

    if not effective_hbase_site_config.get('hbase.thrift.keytab.file'):
      thrift_keytab = (
        effective_hbase_site_config.get('hbase.master.keytab.file')
        or effective_hbase_site_config.get('hbase.regionserver.keytab.file')
        or hbase_env_config.get('hbase_user_keytab')
      )
      if thrift_keytab:
        effective_hbase_site_config['hbase.thrift.keytab.file'] = thrift_keytab

    from resource_management.libraries.resources.xml_config import XmlConfig
    XmlConfig("hbase-site.xml",
              conf_dir=params.hbase_conf_dir,
              configurations=effective_hbase_site_config,
              configuration_attributes=params.config['configurationAttributes']['hbase-site'],
              owner=params.hbase_user,
              group=params.user_group)

    # Deploy hbase-thrift-site.xml (Thrift-specific config)
    if thrift_site_config:
      XmlConfig("hbase-thrift-site.xml",
                conf_dir=params.hbase_conf_dir,
                configurations=thrift_site_config,
                configuration_attributes=params.config['configurationAttributes'].get('hbase-thrift-site', {}),
                owner=params.hbase_user,
                group=params.user_group)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)

    role = 'thrift'
    cmd = format("{daemon_script} --config {hbase_conf_dir}")
    pid_file = format("{pid_dir}/hbase-{hbase_user}-{role}.pid")
    pid_expression = as_sudo(["cat", pid_file])
    no_op_test = as_sudo(["test", "-f", pid_file]) + format(" && ps -p `{pid_expression}` >/dev/null 2>&1")

    # Thrift requires port args: -p <port> --infoport <infoport>
    thrift_port = params.hbase_thrift_port
    thrift_info_port = params.hbase_thrift_info_port
    daemon_cmd = format("{cmd} start {role} -p {thrift_port} --infoport {thrift_info_port}")

    try:
      Execute(daemon_cmd,
              not_if=no_op_test,
              user=params.hbase_user
              )
    except:
      show_logs(params.log_dir, params.hbase_user)
      raise

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    role = 'thrift'
    cmd = format("{daemon_script} --config {hbase_conf_dir}")
    pid_file = format("{pid_dir}/hbase-{hbase_user}-{role}.pid")
    pid_expression = as_sudo(["cat", pid_file])
    no_op_test = as_sudo(["test", "-f", pid_file]) + format(" && ps -p `{pid_expression}` >/dev/null 2>&1")

    daemon_cmd = format("{cmd} stop {role}")
    shutdown_timeout = getattr(params, 'hbase_regionserver_shutdown_timeout', 30)

    try:
      Execute(daemon_cmd,
              user=params.hbase_user,
              only_if=no_op_test,
              timeout=shutdown_timeout,
              on_timeout=format("! ( {no_op_test} ) || {sudo} -H -E kill -9 `{pid_expression}`")
              )
    except:
      show_logs(params.log_dir, params.hbase_user)
      raise

    File(pid_file, action="delete")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.thrift_pid_file)

  def get_log_folder(self):
    import params
    return params.log_dir

  def get_user(self):
    import params
    return params.hbase_user

  def get_pid_files(self):
    import status_params
    return [status_params.thrift_pid_file]


if __name__ == "__main__":
  HbaseThrift().execute()
