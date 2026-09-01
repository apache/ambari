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

import socket

from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from resource_management.core import shell
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


def post_upgrade_check():
  """
  Checks that the NodeManager has rejoined the cluster.
  This function will obtain the Kerberos ticket if security is enabled.
  :return:
  """
  import params

  Logger.info(
    'NodeManager executing "yarn node -list -states=RUNNING" to verify the node has rejoined the cluster...'
  )
  try:
    if params.security_enabled:
      if not params.nodemanager_principal_name or not params.nodemanager_keytab:
        raise Fail("NodeManager principal and keytab are required in a secure cluster")
      with PrivateKerberosCache(params.yarn_user, params.user_group) as cache:
        cache.kinit(
          params.kinit_path_local,
          params.nodemanager_keytab,
          params.nodemanager_principal_name,
        )
        _check_nodemanager_startup(cache.environment)
    else:
      _check_nodemanager_startup({})
  except Fail:
    show_logs(params.yarn_log_dir, params.yarn_user)
    raise


@retry(times=30, sleep_time=10, err_class=Fail)
def _check_nodemanager_startup(command_environment):
  """
  Checks that a NodeManager is in a RUNNING state in the cluster via
  "yarn node -list -states=RUNNING" command. Once the NodeManager is found to be
  alive this method will return, otherwise it will raise a Fail(...) and retry
  automatically.
  :return:
  """
  import params
  command = (
    f"{params.yarn_container_bin}/yarn",
    "--config",
    params.hadoop_conf_dir,
    "node",
    "-list",
    "-states=RUNNING",
  )
  _, yarn_output = shell.checked_call(
    command,
    user=params.yarn_user,
    env=command_environment,
    timeout=60,
  )

  hostname = params.hostname.lower()
  nodemanager_address = params.nm_address.lower()
  configured_host, configured_port = _split_node_id(nodemanager_address)
  expected_hosts = {
    hostname.rstrip("."),
    configured_host.rstrip("."),
    *_resolve_host_aliases(hostname),
  }
  running_nodes = _running_node_ids(yarn_output)
  matching_nodes = {
    node_id
    for node_id in running_nodes
    if _node_matches(node_id, expected_hosts, configured_port)
  }

  if matching_nodes:
    Logger.info(
      f"NodeManager with ID '{nodemanager_address}' has rejoined the cluster."
    )
    return
  else:
    raise Fail(
      f"NodeManager with ID '{nodemanager_address}' was not found in the list "
      f"of running NodeManagers. Command output was:\n{yarn_output}"
    )


def _split_node_id(node_id):
  value = str(node_id).strip().lower()
  if value.startswith("["):
    closing_bracket = value.find("]")
    if closing_bracket <= 1 or value[closing_bracket + 1 : closing_bracket + 2] != ":":
      raise Fail(f"Invalid NodeManager node ID: {node_id!r}")
    host = value[1:closing_bracket]
    port = value[closing_bracket + 2 :]
  else:
    host, separator, port = value.rpartition(":")
    if not separator:
      raise Fail(f"Invalid NodeManager node ID: {node_id!r}")
  if not host or not port.isdigit() or not 1 <= int(port) <= 65535:
    raise Fail(f"Invalid NodeManager node ID: {node_id!r}")
  return host, port


def _resolve_host_aliases(hostname):
  try:
    address_info = socket.getaddrinfo(hostname, None, type=socket.SOCK_STREAM)
  except socket.gaierror as error:
    Logger.warning(f"Could not resolve NodeManager hostname {hostname!r}: {error}")
    return set()
  return {
    address[4][0].lower().rstrip(".")
    for address in address_info
    if address[4] and address[4][0]
  }


def _running_node_ids(yarn_output):
  running_nodes = set()
  for line in str(yarn_output).splitlines():
    columns = line.split()
    if len(columns) < 2 or columns[1].upper() != "RUNNING":
      continue
    node_id = columns[0].lower()
    _split_node_id(node_id)
    running_nodes.add(node_id)
  return running_nodes


def _node_matches(node_id, expected_hosts, expected_port):
  host, port = _split_node_id(node_id)
  return host.rstrip(".") in expected_hosts and port == expected_port
