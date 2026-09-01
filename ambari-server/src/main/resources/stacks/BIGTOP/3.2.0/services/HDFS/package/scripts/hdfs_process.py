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

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.libraries.functions import safe_process


_PROCESS_CLASSES = {
  "namenode": "org.apache.hadoop.hdfs.server.namenode.NameNode",
  "datanode": "org.apache.hadoop.hdfs.server.datanode.DataNode",
  "secondarynamenode": (
    "org.apache.hadoop.hdfs.server.namenode.SecondaryNameNode"
  ),
  "journalnode": "org.apache.hadoop.hdfs.qjournal.server.JournalNode",
  "zkfc": "org.apache.hadoop.hdfs.tools.DFSZKFailoverController",
  "dfsrouter": "org.apache.hadoop.hdfs.server.federation.router.Router",
  "nfs3": "org.apache.hadoop.hdfs.nfs.nfs3.PrivilegedNfsGatewayStarter",
}

_PRIVILEGED_PROCESS_CLASSES = {
  "datanode": "org.apache.hadoop.hdfs.server.datanode.SecureDataNodeStarter",
}


def expected_cmdline(component, privileged=False):
  try:
    process_class = (
      _PRIVILEGED_PROCESS_CLASSES[component]
      if privileged
      else _PROCESS_CLASSES[component]
    )
  except KeyError as error:
    qualifier = "privileged " if privileged else ""
    raise Fail(
      f"Unsupported HDFS {qualifier}process component {component}"
    ) from error
  return (f"-Dproc_{component}", process_class)


def recover_running_process(
  pid_file,
  expected_user,
  component,
  owner=None,
  group=None,
  privileged=False,
):
  tokens = expected_cmdline(component, privileged)
  pid = safe_process.read_pid(pid_file)
  if pid is not None:
    identity = safe_process.read_running_process(pid_file, expected_user, tokens)
    if identity is not None:
      return identity
    safe_process.remove_pid_file_if_stopped(
      pid_file, pid, expected_user, tokens
    )

  identity = safe_process.discover_running_process(expected_user, tokens)
  if identity is None or owner is None or group is None:
    return identity
  return safe_process.create_pid_file_for_identity(
    pid_file,
    identity,
    expected_user,
    tokens,
    owner,
    group,
    mode=0o640,
  )


def wait_for_running_process(
  pid_file,
  expected_user,
  component,
  owner,
  group,
  privileged=False,
  attempts=30,
  sleep_seconds=1,
):
  tokens = expected_cmdline(component, privileged)
  identity = safe_process.wait_for_running_process(
    pid_file,
    expected_user,
    tokens,
    attempts=attempts,
    sleep_seconds=sleep_seconds,
  )
  return safe_process.secure_pid_file_for_identity(
    pid_file,
    identity,
    expected_user,
    tokens,
    owner,
    group,
    mode=0o640,
  )


def check_component_status(
  pid_file, expected_user, component, privileged=False
):
  identity = safe_process.read_running_process(
    pid_file, expected_user, expected_cmdline(component, privileged)
  )
  if identity is None:
    raise ComponentIsNotRunning(f"HDFS {component} is not running")
  return identity


def wait_for_component_status(
  pid_file,
  expected_user,
  component,
  privileged=False,
  attempts=5,
  sleep_seconds=3,
):
  return safe_process.wait_for_running_process(
    pid_file,
    expected_user,
    expected_cmdline(component, privileged),
    attempts=attempts,
    sleep_seconds=sleep_seconds,
  )


def wait_for_process_stopped(
  identity,
  expected_user,
  component,
  privileged=False,
  attempts=6,
  sleep_seconds=10,
):
  return safe_process.wait_for_process_stopped(
    identity,
    expected_user,
    expected_cmdline(component, privileged),
    attempts,
    sleep_seconds,
  )


def terminate_process(identity, expected_user, component, privileged=False):
  safe_process.terminate_process(
    identity, expected_user, expected_cmdline(component, privileged)
  )


def remove_pid_file_if_stopped(
  pid_file, identity, expected_user, component, privileged=False
):
  return safe_process.remove_pid_file_if_stopped(
    pid_file,
    identity.pid,
    expected_user,
    expected_cmdline(component, privileged),
  )
