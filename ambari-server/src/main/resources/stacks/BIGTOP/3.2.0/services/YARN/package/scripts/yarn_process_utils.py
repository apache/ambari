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

import re
import time

from resource_management.core.exceptions import ComponentIsNotRunning, Fail
from resource_management.libraries.functions import safe_process


_PROCESS_CLASSES = {
  "resourcemanager": "org.apache.hadoop.yarn.server.resourcemanager.ResourceManager",
  "nodemanager": "org.apache.hadoop.yarn.server.nodemanager.NodeManager",
  "timelineserver": (
    "org.apache.hadoop.yarn.server.applicationhistoryservice."
    "ApplicationHistoryServer"
  ),
  "timelinereader": (
    "org.apache.hadoop.yarn.server.timelineservice.reader.TimelineReaderServer"
  ),
  "registrydns": "org.apache.hadoop.registry.server.dns.RegistryDNSServer",
  "historyserver": "org.apache.hadoop.mapreduce.v2.hs.JobHistoryServer",
  "master": "org.apache.hadoop.hbase.master.HMaster",
  "regionserver": "org.apache.hadoop.hbase.regionserver.HRegionServer",
}

_REGISTRY_DNS_SECURE_CLASS = (
  "org.apache.hadoop.registry.server.dns.PrivilegedRegistryDNSStarter"
)


def validate_path_segment(value, label):
  if not isinstance(value, str) or not re.fullmatch(
    r"[A-Za-z0-9][A-Za-z0-9._-]{0,254}", value
  ):
    raise Fail(f"{label} must be a single safe path segment")
  return value


def expected_cmdline(component, privileged=False):
  if component == "registrydns" and privileged:
    process_class = _REGISTRY_DNS_SECURE_CLASS
  else:
    try:
      process_class = _PROCESS_CLASSES[component]
    except KeyError as error:
      raise ValueError(
        f"Unsupported YARN process component {component}"
      ) from error
  return (f"-Dproc_{component}", process_class)


def _process_description(component, privileged):
  return f"privileged {component}" if privileged else component


def read_running_process(pid_file, expected_user, component, privileged=False):
  return safe_process.read_running_process(
    pid_file, expected_user, expected_cmdline(component, privileged)
  )


def recover_running_process(
  pid_file,
  expected_user,
  component,
  owner,
  group,
  privileged=False,
):
  pid = safe_process.read_pid(pid_file)
  if pid is not None:
    identity = read_running_process(
      pid_file, expected_user, component, privileged
    )
    if identity is not None:
      return identity
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      pid,
      expected_user,
      expected_cmdline(component, privileged),
    )

  identity = safe_process.discover_running_process(
    expected_user, expected_cmdline(component, privileged)
  )
  if identity is None:
    return None
  return safe_process.create_pid_file_for_identity(
    pid_file,
    identity,
    expected_user,
    expected_cmdline(component, privileged),
    owner,
    group,
    mode=0o640,
  )


def check_component_status(
  pid_file,
  expected_user,
  component,
  owner,
  group,
  privileged=False,
):
  identity = recover_running_process(
    pid_file,
    expected_user,
    component,
    owner,
    group,
    privileged,
  )
  if identity is None:
    description = _process_description(component, privileged)
    raise ComponentIsNotRunning(f"YARN {description} is not running")
  return identity


def remove_stale_pid_file(
  pid_file, expected_user, component, privileged=False
):
  pid = safe_process.read_pid(pid_file)
  if pid is None:
    return False
  identity = read_running_process(
    pid_file, expected_user, component, privileged
  )
  if identity is not None:
    return False
  return safe_process.remove_pid_file_if_stopped(
    pid_file,
    pid,
    expected_user,
    expected_cmdline(component, privileged),
  )


def wait_for_running_process(
  pid_file,
  expected_user,
  component,
  owner,
  group,
  privileged=False,
  attempts=10,
  sleep_seconds=1,
):
  for attempt in range(attempts):
    identity = recover_running_process(
      pid_file,
      expected_user,
      component,
      owner,
      group,
      privileged,
    )
    if identity is not None:
      return identity
    if attempt + 1 < attempts:
      time.sleep(sleep_seconds)
  description = _process_description(component, privileged)
  raise Fail(f"YARN {description} did not start with a valid process")


def stop_process(
  pid_file,
  expected_user,
  component,
  owner,
  group,
  privileged=False,
):
  identity = recover_running_process(
    pid_file,
    expected_user,
    component,
    owner,
    group,
    privileged,
  )
  if identity is None:
    return False

  safe_process.terminate_process(
    identity,
    expected_user,
    expected_cmdline(component, privileged),
  )
  safe_process.remove_pid_file_if_stopped(
    pid_file,
    identity.pid,
    expected_user,
    expected_cmdline(component, privileged),
  )
  return True
