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
import uuid

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import Execute, File
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


def _ruby_quote(value):
  value = str(value)
  return "'" + value.replace("\\", "\\\\").replace("'", "\\'").replace(
    "\r", "\\r"
  ).replace("\n", "\\n") + "'"


def _hosts(params):
  configured_hosts = params.hbase_excluded_hosts or params.hbase_included_hosts
  hosts = []
  for host in str(configured_hosts or "").split(","):
    host = host.strip()
    if host and host not in hosts:
      hosts.append(host)
  if not hosts:
    raise Fail("HBase decommission requires at least one RegionServer host")
  return hosts


def _shell_command(params, hosts, recommission):
  if recommission:
    commands = [f"recommission_regionserver {_ruby_quote(host)}" for host in hosts]
  else:
    server_array = ", ".join(_ruby_quote(host) for host in hosts)
    commands = [f"decommission_regionservers [{server_array}], false"]
  return "\n".join((*commands, "exit", ""))


def _execute_hbase_shell(params, command_file, environment=None):
  command = [params.hbase_cmd, "--config", params.hbase_conf_dir]
  command.extend(("shell", "-n", command_file))
  Execute(
    tuple(command),
    user=params.hbase_user,
    environment=environment,
    logoutput=True,
    timeout=120,
    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
  )


def _move_regions(params, host, operation, environment=None):
  if operation not in ("load", "unload"):
    raise Fail(f"Unsupported HBase RegionMover operation: {operation}")
  command = [params.hbase_cmd, "--config", params.hbase_conf_dir]
  command.extend(
    (
      "org.apache.hadoop.hbase.util.RegionMover",
      "--maxthreads",
      "24",
      "--operation",
      operation,
      "--regionserverhost",
      str(host),
    )
  )
  Execute(
    tuple(command),
    user=params.hbase_user,
    environment=environment,
    logoutput=True,
    timeout=params.hbase_region_mover_timeout,
    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
  )


def _with_kerberos(params, prefix, operation):
  if not params.security_enabled:
    return operation(None)
  required = (
    params.kinit_path_local,
    params.master_keytab_path,
    params.master_jaas_princ,
  )
  if not all(str(value or "").strip() for value in required):
    raise Fail("Secure HBase administration requires a master principal and keytab")
  with PrivateKerberosCache(
    params.hbase_user,
    params.user_group,
    temp_dir=params.exec_tmp_dir,
    prefix=prefix,
  ) as kerberos_cache:
    kerberos_cache.kinit(
      params.kinit_path_local,
      params.master_keytab_path,
      params.master_jaas_princ,
      timeout=30,
    )
    return operation(kerberos_cache.environment)


def move_regions(params, host, operation):
  return _with_kerberos(
    params,
    "ambari-hbase-region-mover-",
    lambda environment: _move_regions(params, host, operation, environment),
  )


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hbase_decommission(env):
  import params

  env.set_params(params)
  hosts = _hosts(params)
  command_file = os.path.join(
    params.exec_tmp_dir, f"hbase-decommission-{uuid.uuid4().hex}.hbase"
  )
  File(
    command_file,
    content=_shell_command(params, hosts, params.hbase_drain_only),
    owner=params.hbase_user,
    group=params.user_group,
    mode=0o600,
  )
  try:
    def operation(environment):
      _execute_hbase_shell(params, command_file, environment)
      if not params.hbase_drain_only:
        for host in hosts:
          _move_regions(params, host, "unload", environment)

    _with_kerberos(params, "ambari-hbase-decommission-", operation)
  finally:
    File(command_file, action="delete")
