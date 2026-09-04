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

Ambari Agent

"""

import os

from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.libraries.script.script import Script
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)

import yarn_process_utils


class ServiceCheck(Script):
  def service_check(self, env):
    pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ServiceCheckDefault(ServiceCheck):
  def service_check(self, env):
    import params

    env.set_params(params)

    smokeuser = yarn_process_utils.validate_path_segment(
      params.smokeuser, "YARN service-check user"
    )

    params.HdfsResource(
      f"/user/{smokeuser}",
      type="directory",
      action="create_on_execute",
      owner=smokeuser,
      mode=params.smoke_hdfs_user_mode,
    )
    params.HdfsResource(None, action="execute")

    distributed_shell_jar = os.path.join(
      params.hadoop_yarn_home,
      "hadoop-yarn-applications-distributedshell.jar",
    )
    if not os.path.isfile(distributed_shell_jar):
      raise Fail(f"YARN distributed shell jar is missing: {distributed_shell_jar}")

    command = (
      f"{params.yarn_container_bin}/yarn",
      "--config",
      params.hadoop_conf_dir,
      "org.apache.hadoop.yarn.applications.distributedshell.Client",
      "--shell_command",
      "true",
      "--num_containers",
      str(params.number_of_nm),
      "--jar",
      distributed_shell_jar,
      "--timeout",
      "300000",
      "--queue",
      params.service_check_queue_name,
    )
    command_environment = {"PATH": params.execute_path}
    if params.security_enabled:
      with PrivateKerberosCache(
        smokeuser,
        params.user_group,
        prefix="ambari-yarn-check-",
      ) as kerberos_cache:
        kerberos_cache.kinit(
          params.kinit_path_local,
          params.smoke_user_keytab,
          params.smokeuser_principal,
        )
        self._execute_check(
          smokeuser,
          command,
          kerberos_cache.merge_environment(command_environment),
        )
    else:
      self._execute_check(smokeuser, command, command_environment)

  @staticmethod
  def _execute_check(smokeuser, command, command_environment):
    Execute(
      command,
      user=smokeuser,
      environment=command_environment,
      timeout=330,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      logoutput=True,
    )


if __name__ == "__main__":
  ServiceCheck().execute()
