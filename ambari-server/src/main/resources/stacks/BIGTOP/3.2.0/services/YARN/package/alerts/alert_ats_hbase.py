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

import logging
import json
import math
import os
import socket
import stat
import subprocess
import time

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions.stack_tools import get_stack_root
from resource_management.libraries.functions.check_process_status import (
  check_process_status,
)
from resource_management.core.exceptions import ComponentIsNotRunning
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)

CRITICAL_RESULT_CODE = "CRITICAL"
OK_RESULT_CODE = "OK"
UNKNOWN_STATUS_CODE = "UNKNOWN"

OK_MESSAGE = "The HBase application reported a '{0}' state in {1:.3f}s"
CRITICAL_MESSAGE_WITH_STATE = (
  "The HBase application reported a '{0}' state. Check took {1:.3f}s"
)
CRITICAL_MESSAGE = "ats-hbase service information could not be retrieved"


SECURITY_ENABLED_KEY = "{{cluster-env/security_enabled}}"
STACK_ROOT = "{{cluster-env/stack_root}}"


ATS_PRINCIPAL_KEY = "{{yarn-env/yarn_ats_principal_name}}"
ATS_KEYTAB_KEY = "{{yarn-env/yarn_ats_user_keytab}}"
ATS_HBASE_USER_KEY = "{{yarn-env/yarn_ats_user}}"
ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY = (
  "{{yarn-hbase-env/is_hbase_system_service_launch}}"
)
USE_EXTERNAL_HBASE_KEY = "{{yarn-hbase-env/use_external_hbase}}"
ATS_HBASE_PID_DIR_PREFIX = "{{yarn-hbase-env/yarn_hbase_pid_dir_prefix}}"

ATS_HBASE_APP_NOT_FOUND_KEY = "Service ats-hbase not found"

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = "{{kerberos-env/executable_search_paths}}"


CHECK_COMMAND_TIMEOUT_KEY = "check.command.timeout"
CHECK_COMMAND_TIMEOUT_DEFAULT = 120.0


logger = logging.getLogger("ambari_alerts")

_HBASE_PROCESS_CLASSES = {
  "master": "org.apache.hadoop.hbase.master.HMaster",
  "regionserver": "org.apache.hadoop.hbase.regionserver.HRegionServer",
}


def _parse_boolean(value, label):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise Fail(f"{label} must be true or false")


def resolve_stack_root(configured_stack_root):
  if not isinstance(configured_stack_root, str) or not configured_stack_root.strip():
    raise Fail("cluster-env/stack_root is required for the ATS HBase alert")
  configured_stack_root = configured_stack_root.strip()
  if configured_stack_root.startswith("{"):
    try:
      stack_roots = json.loads(configured_stack_root)
    except json.JSONDecodeError as error:
      raise Fail("cluster-env/stack_root must be valid JSON") from error
    if not isinstance(stack_roots, dict) or "BIGTOP" not in stack_roots:
      raise Fail("cluster-env/stack_root must define BIGTOP")
    stack_root = get_stack_root("BIGTOP", configured_stack_root)
  else:
    stack_root = configured_stack_root
  if (
    not isinstance(stack_root, str)
    or not os.path.isabs(stack_root)
    or os.path.normpath(stack_root) != stack_root
    or stack_root == os.sep
  ):
    raise Fail("BIGTOP stack root must be a safe absolute directory")
  return stack_root


def _path_prefixes(path):
  current = os.sep
  yield current
  for part in [part for part in path.split(os.sep) if part]:
    current = os.path.join(current, part)
    yield current


def _validate_trusted_path(path, final_regular=False):
  final_metadata = None
  for candidate in _path_prefixes(path):
    try:
      metadata = os.lstat(candidate)
    except OSError as error:
      raise Fail(f"Trusted ATS HBase command path is missing: {candidate}") from error
    is_final = candidate == path
    if stat.S_ISLNK(metadata.st_mode):
      if metadata.st_uid != 0:
        raise Fail(f"ATS HBase command symlink must be root-owned: {candidate}")
      if is_final:
        raise Fail("ATS HBase command must resolve to a regular file")
      continue
    if is_final and final_regular:
      if not stat.S_ISREG(metadata.st_mode):
        raise Fail("ATS HBase command must be a regular file")
      final_metadata = metadata
    elif not stat.S_ISDIR(metadata.st_mode):
      raise Fail(f"ATS HBase command parent must be a directory: {candidate}")
    if metadata.st_uid != 0 or metadata.st_mode & 0o022:
      raise Fail(
        "ATS HBase command path must be root-owned and non-writable: "
        f"{candidate}"
      )
  return final_metadata


def resolve_yarn_executable(stack_root):
  yarn_executable = os.path.join(
    stack_root, "current", "hadoop-yarn-client", "bin", "yarn"
  )
  _validate_trusted_path(yarn_executable, final_regular=True)
  resolved_executable = os.path.realpath(yarn_executable)
  if (
    not os.path.isabs(resolved_executable)
    or os.path.normpath(resolved_executable) != resolved_executable
  ):
    raise Fail("ATS HBase command must resolve to a canonical absolute path")
  metadata = _validate_trusted_path(resolved_executable, final_regular=True)
  if metadata.st_mode & 0o111 == 0:
    raise Fail("ATS HBase command must be executable")
  return yarn_executable


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (
    SECURITY_ENABLED_KEY,
    KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY,
    ATS_PRINCIPAL_KEY,
    ATS_KEYTAB_KEY,
    ATS_HBASE_USER_KEY,
    STACK_ROOT,
    USE_EXTERNAL_HBASE_KEY,
    ATS_HBASE_PID_DIR_PREFIX,
    ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY,
  )


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return (
      UNKNOWN_STATUS_CODE,
      ["There were no configurations supplied to the script."],
    )

  result_code = None
  if host_name is None:
    host_name = socket.getfqdn()

  try:
    use_external_hbase = _parse_boolean(
      configurations.get(USE_EXTERNAL_HBASE_KEY, False),
      "yarn-hbase-env/use_external_hbase",
    )

    if use_external_hbase:
      return (OK_RESULT_CODE, ["use_external_hbase set to true."])

    is_hbase_system_service_launch = _parse_boolean(
      configurations.get(ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY, False),
      "yarn-hbase-env/is_hbase_system_service_launch",
    )

    yarn_hbase_user = "yarn-ats"
    if ATS_HBASE_USER_KEY in configurations:
      yarn_hbase_user = configurations[ATS_HBASE_USER_KEY]

    if not is_hbase_system_service_launch:
      yarn_hbase_pid_dir_prefix = ""
      if ATS_HBASE_PID_DIR_PREFIX in configurations:
        yarn_hbase_pid_dir_prefix = configurations[ATS_HBASE_PID_DIR_PREFIX]
      else:
        return (
          UNKNOWN_STATUS_CODE,
          ["The yarn_hbase_pid_dir_prefix is a required parameter."],
        )
      yarn_hbase_pid_dir = format("{yarn_hbase_pid_dir_prefix}/{yarn_hbase_user}")
      master_pid_file = format(
        "{yarn_hbase_pid_dir}/hbase-{yarn_hbase_user}-master.pid"
      )
      rs_pid_file = format(
        "{yarn_hbase_pid_dir}/hbase-{yarn_hbase_user}-regionserver.pid"
      )

      master_process_running = is_monitor_process_live(
        master_pid_file, yarn_hbase_user, "master"
      )
      rs_process_running = is_monitor_process_live(
        rs_pid_file, yarn_hbase_user, "regionserver"
      )

      alert_state = (
        OK_RESULT_CODE
        if master_process_running and rs_process_running
        else CRITICAL_RESULT_CODE
      )

      alert_label = (
        "ATS embedded HBase is running on {0}"
        if master_process_running and rs_process_running
        else "ATS embedded HBase is NOT running on {0}"
      )
      alert_label = alert_label.format(host_name)

      return (alert_state, [alert_label])
    else:
      security_enabled = _parse_boolean(
        configurations.get(SECURITY_ENABLED_KEY, False),
        "cluster-env/security_enabled",
      )

      check_command_timeout = CHECK_COMMAND_TIMEOUT_DEFAULT
      if CHECK_COMMAND_TIMEOUT_KEY in parameters:
        check_command_timeout = float(parameters[CHECK_COMMAND_TIMEOUT_KEY])
      if not math.isfinite(check_command_timeout) or check_command_timeout <= 0:
        raise Fail("Check command timeout must be positive and finite")

      if security_enabled:
        if (
          ATS_PRINCIPAL_KEY not in configurations
          or ATS_KEYTAB_KEY not in configurations
        ):
          raise Fail("ATS Kerberos principal and keytab are required")
        ats_hbase_app_principal = configurations[ATS_PRINCIPAL_KEY]
        ats_hbase_app_keytab = configurations[ATS_KEYTAB_KEY]

        # Get the configured Kerberos executable search paths, if any
        if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
          kerberos_executable_search_paths = configurations[
            KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY
          ]
        else:
          kerberos_executable_search_paths = None

        kinit_path_local = get_kinit_path(kerberos_executable_search_paths)
      start_time = time.time()
      stack_root = resolve_stack_root(configurations.get(STACK_ROOT))
      yarn_executable = resolve_yarn_executable(stack_root)
      if security_enabled:
        with PrivateKerberosCache(
          yarn_hbase_user,
          prefix="ambari-ats-hbase-alert-",
        ) as kerberos_cache:
          kerberos_cache.kinit(
            kinit_path_local,
            ats_hbase_app_keytab,
            ats_hbase_app_principal,
            timeout=10,
          )
          output = get_ats_hbase_status(
            yarn_executable,
            yarn_hbase_user,
            check_command_timeout,
            kerberos_cache.environment,
          )
      else:
        output = get_ats_hbase_status(
          yarn_executable, yarn_hbase_user, check_command_timeout
        )
      # Call for getting JSON
      ats_hbase_app_info = make_valid_json(output)

      if ats_hbase_app_info is None:
        alert_label = CRITICAL_MESSAGE
        result_code = CRITICAL_RESULT_CODE
        return (result_code, [alert_label])

      if "state" not in ats_hbase_app_info:
        return (
          UNKNOWN_STATUS_CODE,
          ["ATS HBase service response did not contain a state"],
        )

      retrieved_ats_hbase_app_state = ats_hbase_app_info["state"].upper()

      if retrieved_ats_hbase_app_state in ["STABLE"]:
        result_code = OK_RESULT_CODE
        total_time = time.time() - start_time
        alert_label = OK_MESSAGE.format(retrieved_ats_hbase_app_state, total_time)
      else:
        result_code = CRITICAL_RESULT_CODE
        total_time = time.time() - start_time
        alert_label = CRITICAL_MESSAGE_WITH_STATE.format(
          retrieved_ats_hbase_app_state, total_time
        )
  except Exception as error:
    logger.exception("ATS HBase alert failed")
    alert_label = f"ATS HBase service information could not be retrieved: {error}"
    result_code = CRITICAL_RESULT_CODE
  return (result_code, [alert_label])


def get_ats_hbase_status(yarn_executable, user, timeout, environment=None):
  _, output, _ = shell.checked_call(
    (yarn_executable, "app", "-status", "ats-hbase"),
    user=user,
    stderr=subprocess.PIPE,
    timeout=timeout,
    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    logoutput=False,
    env=environment,
  )
  return output


def make_valid_json(output):
  if ATS_HBASE_APP_NOT_FOUND_KEY in output:
    return None

  decoder = json.JSONDecoder()
  for offset, character in enumerate(output):
    if character != "{":
      continue
    try:
      value, _ = decoder.raw_decode(output[offset:])
    except json.JSONDecodeError:
      continue
    if isinstance(value, dict):
      return value
  raise Fail("Could not find a JSON object in the ATS HBase service response")


@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def is_monitor_process_live(pid_file, expected_user, component):
  """
  Gets whether the Metrics Monitor represented by the specified file is running.
  :param pid_file: the PID file of the monitor to check
  :param expected_user: operating system user that must own the process
  :param component: embedded HBase component name
  :return: True if the process is running, False otherwise
  """
  live = False

  try:
    check_process_status(
      pid_file,
      expected_user,
      (f"-Dproc_{component}", _HBASE_PROCESS_CLASSES[component]),
    )
    live = True
  except ComponentIsNotRunning:
    pass

  return live
