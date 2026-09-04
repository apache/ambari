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
import re
import uuid

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.resources.system import File
from resource_management.core.signal_utils import TerminateStrategy

import zookeeper_utils


_CONNECTION_PATTERN = re.compile(r"[A-Za-z0-9_.-]+:[0-9]+", re.ASCII)
_ERROR_MARKERS = ("KeeperErrorCode =", "Exception in thread", "ERROR [")


def run_cli_command(
  cli_path,
  connection,
  command,
  user,
  group,
  temp_dir,
  environment,
  timeout=60,
):
  zookeeper_utils.validate_executable(cli_path, "ZooKeeper CLI")
  zookeeper_utils.validate_absolute_path(temp_dir, "ZooKeeper CLI temp directory")
  zookeeper_utils.validate_user(user, "ZooKeeper CLI user")
  zookeeper_utils.validate_user(group, "ZooKeeper CLI group")
  if _CONNECTION_PATTERN.fullmatch(connection) is None:
    raise Fail("ZooKeeper CLI connection must be a host and port")
  port = int(connection.rsplit(":", 1)[1])
  if not 1 <= port <= 65535:
    raise Fail("ZooKeeper CLI connection port must be between 1 and 65535")
  if (
    not isinstance(command, str)
    or not command
    or any(ord(character) < 32 or ord(character) == 127 for character in command)
  ):
    raise Fail("ZooKeeper CLI command must contain one printable line")

  command_file = os.path.join(
    temp_dir, f"ambari-zookeeper-cli-{uuid.uuid4().hex}.txt"
  )
  File(
    command_file,
    content=command + "\n",
    owner=user,
    group=group,
    mode=0o600,
    replace=False,
  )
  operation_error = None
  try:
    _, output = shell.checked_call(
      (
        "/bin/bash",
        "-c",
        'exec "$1" -server "$2" < "$3"',
        "ambari-zookeeper-cli",
        cli_path,
        connection,
        command_file,
      ),
      user=user,
      env=dict(environment or {}),
      timeout=timeout,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )
  except Exception as error:
    operation_error = error
    raise
  finally:
    try:
      File(command_file, action="delete")
    except Exception as cleanup_error:
      if operation_error is not None:
        raise Fail(
          f"ZooKeeper CLI failed: {operation_error}; command input cleanup "
          f"also failed: {cleanup_error}"
        ) from operation_error
      raise

  if isinstance(output, bytes):
    output = output.decode("utf-8", errors="replace")
  if not isinstance(output, str):
    raise Fail("ZooKeeper CLI returned a non-text response")
  if any(marker in output for marker in _ERROR_MARKERS):
    raise Fail(f"ZooKeeper CLI command failed for {connection}")
  return output


def verify_ensemble(
  hosts,
  client_port,
  cli_path,
  user,
  group,
  temp_dir,
  environment,
):
  if not hosts:
    raise Fail("ZooKeeper service check requires at least one server")
  check_id = uuid.uuid4().hex
  node_path = f"/ambari-zookeeper-service-check-{check_id}"
  node_data = f"ambari-zookeeper-data-{check_id}"
  first_connection = f"{hosts[0]}:{client_port}"
  operation_error = None
  try:
    create_output = run_cli_command(
      cli_path,
      first_connection,
      f"create {node_path} {node_data}",
      user,
      group,
      temp_dir,
      environment,
    )
    if f"Created {node_path}" not in create_output:
      raise Fail("ZooKeeper did not confirm service-check znode creation")

    for host in hosts:
      connection = f"{host}:{client_port}"
      get_output = run_cli_command(
        cli_path,
        connection,
        f"get {node_path}",
        user,
        group,
        temp_dir,
        environment,
      )
      if node_data not in {line.strip() for line in get_output.splitlines()}:
        raise Fail(f"ZooKeeper service-check data was not returned by {host}")
  except Exception as error:
    operation_error = error
    raise
  finally:
    try:
      run_cli_command(
        cli_path,
        first_connection,
        f"delete {node_path}",
        user,
        group,
        temp_dir,
        environment,
      )
    except Exception as cleanup_error:
      if operation_error is not None:
        raise Fail(
          f"ZooKeeper service check failed: {operation_error}; "
          f"znode cleanup also failed: {cleanup_error}"
        ) from operation_error
      raise
