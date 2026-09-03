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
"""

import os
import socket
import uuid

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import File
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.fcntl_based_process_lock import (
  FcntlBasedProcessLock,
)
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.functions.stack_features import check_stack_feature

from hbase_service import read_or_discover_hbase_process


def prestart(env):
  import params

  if params.version and check_stack_feature(
    StackFeature.ROLLING_UPGRADE, params.version
  ):
    select_hbase_packages(params)


def select_hbase_packages(params):
  stack_select.select_packages(params.version)
  select_phoenix_packages(params)


def select_phoenix_packages(params):
  if params.phoenix_enabled:
    version = getattr(params, "version", None) or getattr(
      params, "repository_version", None
    )
    if not version:
      raise Fail("Phoenix package selection requires a stack version")
    with FcntlBasedProcessLock(
      params.stack_select_lock_file,
      enabled=params.is_parallel_execution_enabled,
      skip_fcntl_failures=True,
    ):
      stack_select.select("phoenix-client", version)
      stack_select.select("phoenix-server", version)


def post_regionserver(env):
  import params

  env.set_params(params)
  command_file = os.path.join(
    params.exec_tmp_dir, f"hbase-status-{uuid.uuid4().hex}.hbase"
  )
  File(
    command_file,
    owner=params.hbase_user,
    group=params.user_group,
    mode=0o600,
    content='status "simple"\nexit\n',
  )
  try:
    wait_for_regionserver_registration(params, command_file)
  finally:
    File(command_file, action="delete")


def is_region_server_process_running(params):
  return (
    read_or_discover_hbase_process(
      params.regionserver_pid_file,
      params.hbase_user,
      params.user_group,
      "regionserver",
    )
    is not None
  )


def _hbase_status_output(params, command_file):
  command = (
    params.hbase_cmd,
    "--config",
    params.hbase_conf_dir,
    "shell",
    "-n",
    command_file,
  )
  if not params.security_enabled:
    return shell.checked_call(
      command,
      user=params.hbase_user,
      timeout=60,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )[1]

  required = (
    params.kinit_path_local,
    params.hbase_user_keytab,
    params.hbase_principal_name,
  )
  if not all(str(value or "").strip() for value in required):
    raise Fail("Secure HBase registration check requires a principal and keytab")
  with PrivateKerberosCache(
    params.hbase_user,
    params.user_group,
    temp_dir=params.exec_tmp_dir,
    prefix="ambari-hbase-upgrade-status-",
  ) as kerberos_cache:
    kerberos_cache.kinit(*required, timeout=30)
    return shell.checked_call(
      command,
      user=params.hbase_user,
      env=kerberos_cache.environment,
      timeout=60,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )[1]


def _server_is_registered(output, hostname):
  output = str(output or "").lower()
  if not output:
    raise Fail("Unable to retrieve status information from the HBase shell")
  if f"{hostname.lower()}:" in output:
    return True
  try:
    ip_address = socket.gethostbyname(hostname)
  except socket.error:
    Logger.warning(
      f"Unable to resolve the IP address of {hostname}; registration will be "
      "checked by hostname only."
    )
    return False
  return f"{ip_address.lower()}:" in output


@retry(times=30, sleep_time=30, err_class=Fail)
def wait_for_regionserver_registration(params, command_file):
  if not is_region_server_process_running(params):
    raise Fail("RegionServer process is not running")
  output = _hbase_status_output(params, command_file)
  if not _server_is_registered(output, params.hostname):
    raise Fail(
      f"The RegionServer named {params.hostname} has not yet registered with "
      "the HBase Master"
    )
