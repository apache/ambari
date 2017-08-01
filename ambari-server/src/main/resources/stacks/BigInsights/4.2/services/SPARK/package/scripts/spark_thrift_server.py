#!/usr/bin/python
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
import socket
import os
from resource_management import *
from resource_management.libraries.functions import stack_select
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core import shell
from resource_management.libraries.functions import Direction
from spark import *


class ThriftServer(Script):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    if params.version and compare_versions(format_stack_version(params.version), '4.0.0.0') >= 0:
      stack_select.select_packages(params.version)

  def install(self, env):
    self.install_packages(env)
    import params
    env.set_params(params)
    self.configure(env)

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    daemon_cmd = format('{spark_thrift_server_stop}')
    Execute(daemon_cmd,
            user=params.hive_user,
            environment={'JAVA_HOME': params.java_home}
    )
    if os.path.isfile(params.spark_thrift_server_pid_file):
      os.remove(params.spark_thrift_server_pid_file)


  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    # TODO this looks wrong, maybe just call spark(env)
    self.configure(env)

    if params.security_enabled:
        hive_kerberos_keytab = params.config['configurations']['hive-site']['hive.metastore.kerberos.keytab.file']
        hive_principal = params.config['configurations']['hive-site']['hive.metastore.kerberos.principal'].replace('_HOST', socket.getfqdn().lower())
        hive_kinit_cmd = format("{kinit_path_local} -kt {hive_kerberos_keytab} {hive_principal}; ")
        Execute(hive_kinit_cmd, user=params.hive_user)

    # FIXME! TODO! remove this after soft link bug is fixed:
    #if not os.path.islink('/usr/iop/current/spark'):
    #  iop_version = get_iop_version()
    #  cmd = 'ln -s /usr/iop/' + iop_version + '/spark /usr/iop/current/spark'
    #  Execute(cmd)

    daemon_cmd = format('{spark_thrift_server_start} --conf spark.ui.port={params.spark_thriftserver_ui_port}')
    no_op_test = format(
      'ls {spark_thrift_server_pid_file} >/dev/null 2>&1 && ps -p `cat {spark_thrift_server_pid_file}` >/dev/null 2>&1')
    if upgrade_type is not None and params.upgrade_direction == Direction.DOWNGRADE and not params.security_enabled:
      Execute(daemon_cmd,
              user=params.spark_user,
              environment={'JAVA_HOME': params.java_home},
              not_if=no_op_test
              )
    else:
      Execute(daemon_cmd,
              user=params.hive_user,
              environment={'JAVA_HOME': params.java_home},
              not_if=no_op_test
      )

  def status(self, env):
    import status_params

    env.set_params(status_params)
    pid_file = format("{spark_thrift_server_pid_file}")
    # Recursively check all existing gmetad pid files
    check_process_status(pid_file)

  # Note: This function is not called from start()/install()
  def configure(self, env):
    import params

    env.set_params(params)
    spark(env)

if __name__ == "__main__":
  ThriftServer().execute()
