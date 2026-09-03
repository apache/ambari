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

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions import safe_process

import infra_solr_utils


def process_tokens(port, solr_home):
  validated_port = infra_solr_utils.bounded_int(port, "Infra Solr port", 1, 65535)
  infra_solr_utils.validate_service_directory(solr_home, "Infra Solr data directory")
  return (
    f"-Djetty.port={validated_port}",
    f"-Dsolr.solr.home={solr_home}",
    "-jar",
    "start.jar",
  )


def validate_pid_file(pid_file, port):
  validated_port = infra_solr_utils.bounded_int(
    port, "Infra Solr port", 1, 65535
  )
  infra_solr_utils.validate_absolute_path(pid_file, "Infra Solr PID file")
  infra_solr_utils.validate_service_directory(
    os.path.dirname(pid_file), "Infra Solr PID directory"
  )
  expected_name = f"solr-{validated_port}.pid"
  if os.path.basename(pid_file) != expected_name:
    raise Fail(f"Infra Solr PID file must be named {expected_name}")


def _publish(pid_file, identity, user, group, expected):
  return safe_process.publish_pid_file_for_identity(
    pid_file,
    identity,
    expected_user=user,
    expected_cmdline=expected,
    owner=user,
    group=group,
    mode=0o640,
  )


def read_or_recover_process(pid_file, user, group, port, solr_home):
  validate_pid_file(pid_file, port)
  expected = process_tokens(port, solr_home)
  recorded_pid = safe_process.read_pid(pid_file)
  if recorded_pid is not None:
    identity = safe_process.read_running_process(pid_file, user, expected)
    if identity is not None:
      return _publish(pid_file, identity, user, group, expected)
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      recorded_pid,
      expected_user=user,
      expected_cmdline=expected,
    )
  identity = safe_process.discover_running_process(user, expected)
  if identity is None:
    return None
  return _publish(pid_file, identity, user, group, expected)


def wait_for_started_process(pid_file, user, group, port, solr_home):
  validate_pid_file(pid_file, port)
  expected = process_tokens(port, solr_home)
  identity = safe_process.wait_for_discovered_process(
    user, expected, attempts=30, sleep_seconds=1
  )
  try:
    return _publish(pid_file, identity, user, group, expected)
  except Exception:
    try:
      terminate_identity(identity, pid_file, user, port, solr_home)
    except Exception as cleanup_error:
      Logger.warning(
        f"Infra Solr PID publication cleanup failed: {cleanup_error}"
      )
    raise


def terminate_identity(identity, pid_file, user, port, solr_home):
  expected = process_tokens(port, solr_home)
  safe_process.terminate_process(
    identity,
    user,
    expected,
    term_wait_attempts=30,
    term_wait_sleep=1,
    kill_wait_attempts=10,
    kill_wait_sleep=1,
  )
  safe_process.remove_pid_file_if_stopped(
    pid_file,
    identity.pid,
    expected_user=user,
    expected_cmdline=expected,
  )


def stop_process(pid_file, user, group, port, solr_home):
  identity = read_or_recover_process(
    pid_file, user, group, port, solr_home
  )
  if identity is None:
    return False
  terminate_identity(identity, pid_file, user, port, solr_home)
  return True
