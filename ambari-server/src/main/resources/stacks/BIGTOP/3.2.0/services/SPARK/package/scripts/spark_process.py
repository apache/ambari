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

import spark_utils


HISTORY_SERVER_MAIN_CLASS = "org.apache.spark.deploy.history.HistoryServer"
THRIFT_SERVER_MAIN_CLASS = "org.apache.spark.sql.hive.thriftserver.HiveThriftServer2"


def expected_process_tokens(component, conf_file=None):
  if component == "jobhistoryserver":
    spark_utils.validate_absolute_path(conf_file, "Spark defaults file")
    return (HISTORY_SERVER_MAIN_CLASS, "--properties-file", conf_file)
  if component == "sparkthriftserver":
    spark_utils.validate_absolute_path(conf_file, "Spark defaults file")
    return (THRIFT_SERVER_MAIN_CLASS, "--properties-file", conf_file)
  raise Fail(f"Unsupported Spark component: {component}")


def validate_pid_file(component, pid_file):
  expected_name = {
    "jobhistoryserver": "spark_history_server.pid",
    "sparkthriftserver": "spark_thrift_server.pid",
  }.get(component)
  if expected_name is None:
    raise Fail(f"Unsupported Spark component: {component}")
  spark_utils.validate_absolute_path(pid_file, "Spark PID file")
  spark_utils.validate_service_directory(os.path.dirname(pid_file), "Spark PID directory")
  if os.path.basename(pid_file) != expected_name:
    raise Fail(f"Spark {component} PID file must be named {expected_name}")


def _publish(component, pid_file, identity, user, group, conf_file):
  expected = expected_process_tokens(component, conf_file)
  return safe_process.publish_pid_file_for_identity(
    pid_file,
    identity,
    expected_user=user,
    expected_cmdline=expected,
    owner=user,
    group=group,
    mode=0o640,
  )


def read_or_recover_process(component, pid_file, user, group, conf_file=None):
  validate_pid_file(component, pid_file)
  expected = expected_process_tokens(component, conf_file)
  recorded_pid = safe_process.read_pid(pid_file)
  if recorded_pid is not None:
    identity = safe_process.read_running_process(pid_file, user, expected)
    if identity is not None:
      return _publish(component, pid_file, identity, user, group, conf_file)
    safe_process.remove_pid_file_if_stopped(
      pid_file,
      recorded_pid,
      expected_user=user,
      expected_cmdline=expected,
    )
  identity = safe_process.discover_running_process(user, expected)
  if identity is None:
    return None
  return _publish(component, pid_file, identity, user, group, conf_file)


def wait_for_started_process(component, pid_file, user, group, conf_file=None):
  validate_pid_file(component, pid_file)
  expected = expected_process_tokens(component, conf_file)
  identity = safe_process.wait_for_discovered_process(
    user, expected, attempts=30, sleep_seconds=1
  )
  try:
    return _publish(component, pid_file, identity, user, group, conf_file)
  except Exception:
    try:
      rollback_started_process(component, pid_file, identity, user, conf_file)
    except Exception as error:
      Logger.warning(
        f"Could not roll back Spark {component} after PID publication failure: {error}"
      )
    raise


def rollback_started_process(component, pid_file, identity, user, conf_file=None):
  validate_pid_file(component, pid_file)
  expected = expected_process_tokens(component, conf_file)
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


def stop_process(component, pid_file, user, group, conf_file=None):
  expected = expected_process_tokens(component, conf_file)
  identity = read_or_recover_process(component, pid_file, user, group, conf_file)
  if identity is None:
    return False
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
  return True
