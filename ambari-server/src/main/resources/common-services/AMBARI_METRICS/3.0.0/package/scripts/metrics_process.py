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
import time

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.libraries.functions import safe_process


_AMS_PROCESS_TOKENS = {
  "collector": (
    "-Dproc_ams-metrics-collector",
    "org.apache.ambari.metrics.AMSApplicationServer",
  ),
  "monitor": (
    "/usr/lib/python3.9/site-packages/resource_monitoring/main.py",
  ),
  "grafana": ("/usr/lib/ambari-metrics-grafana/bin/grafana-server",),
}

_HBASE_PROCESS_TOKENS = {
  "master": ("org.apache.hadoop.hbase.master.HMaster",),
  "regionserver": ("org.apache.hadoop.hbase.regionserver.HRegionServer",),
}


def ams_process_tokens(component):
  try:
    return _AMS_PROCESS_TOKENS[component]
  except KeyError as error:
    raise Fail(f"Unsupported Ambari Metrics component: {component}") from error


def hbase_process_tokens(role):
  try:
    return _HBASE_PROCESS_TOKENS[role]
  except KeyError as error:
    raise Fail(f"Unsupported AMS HBase role: {role}") from error


def hbase_pid_file(pid_dir, user, role):
  hbase_process_tokens(role)
  return os.path.join(pid_dir, f"hbase-{user}-{role}.pid")


def _read_or_discover(pid_file, user, group, tokens):
  pid = safe_process.read_pid(pid_file)
  if pid is not None:
    identity = safe_process.inspect_process(pid, user, tokens)
    if identity is not None and safe_process.is_process_running(
      pid, user, tokens, identity=identity
    ):
      return safe_process.secure_pid_file_for_identity(
        pid_file,
        identity,
        user,
        tokens,
        owner=user,
        group=group,
        mode=0o640,
      )
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      pid,
      expected_user=user,
      expected_cmdline=tokens,
    )

  identity = safe_process.discover_running_process(user, tokens)
  if identity is None:
    return None
  return safe_process.create_pid_file_for_identity(
    pid_file,
    identity,
    user,
    tokens,
    owner=user,
    group=group,
    mode=0o640,
  )


def read_or_discover_ams_process(pid_file, user, group, component):
  return _read_or_discover(pid_file, user, group, ams_process_tokens(component))


def read_or_discover_hbase_process(pid_file, user, group, role):
  return _read_or_discover(pid_file, user, group, hbase_process_tokens(role))


def _wait_for_process(reader, description, attempts=15, sleep_seconds=1):
  for attempt in range(attempts):
    identity = reader()
    if identity is not None:
      return identity
    if attempt + 1 < attempts:
      time.sleep(sleep_seconds)
  raise Fail(f"{description} did not start with a valid process identity")


def wait_for_ams_process(
  pid_file, user, group, component, attempts=15, sleep_seconds=1
):
  return _wait_for_process(
    lambda: read_or_discover_ams_process(pid_file, user, group, component),
    f"Ambari Metrics {component}",
    attempts,
    sleep_seconds,
  )


def wait_for_hbase_process(
  pid_file, user, group, role, attempts=15, sleep_seconds=1
):
  return _wait_for_process(
    lambda: read_or_discover_hbase_process(pid_file, user, group, role),
    f"AMS HBase {role}",
    attempts,
    sleep_seconds,
  )


def check_ams_process_status(pid_file, user, group, component):
  identity = read_or_discover_ams_process(pid_file, user, group, component)
  if identity is None:
    raise ComponentIsNotRunning(f"Ambari Metrics {component} is not running")
  return identity


def check_hbase_process_status(pid_file, user, group, role):
  identity = read_or_discover_hbase_process(pid_file, user, group, role)
  if identity is None:
    raise ComponentIsNotRunning(f"AMS HBase {role} is not running")
  return identity


def _stop_process(identity, pid_file, user, tokens, wait_attempts):
  if identity is None:
    return False
  safe_process.terminate_process(
    identity,
    user,
    tokens,
    term_wait_attempts=wait_attempts,
    term_wait_sleep=1,
    kill_wait_attempts=10,
    kill_wait_sleep=1,
  )
  safe_process.remove_pid_file_if_stopped(
    pid_file,
    identity.pid,
    expected_user=user,
    expected_cmdline=tokens,
  )
  return True


def stop_ams_process(pid_file, user, group, component, wait_attempts=15):
  tokens = ams_process_tokens(component)
  identity = read_or_discover_ams_process(pid_file, user, group, component)
  return _stop_process(identity, pid_file, user, tokens, wait_attempts)


def stop_hbase_process(pid_file, user, group, role, wait_attempts=30):
  tokens = hbase_process_tokens(role)
  identity = read_or_discover_hbase_process(pid_file, user, group, role)
  return _stop_process(identity, pid_file, user, tokens, wait_attempts)
