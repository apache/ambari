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

from mysql_service import get_daemon_name
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.private_temporary_file import (
  private_temporary_file,
)


def mysql_adduser():
  import params

  user = _sql_identifier(params.hive_metastore_user_name, "Hive database user")
  database = _sql_identifier(params.hive_db_schema_name, "Hive database schema")
  password = _sql_literal(params.hive_metastore_user_passwd)
  daemon_name = get_daemon_name()
  was_running = _service_is_running(daemon_name)

  action = "restart" if was_running else "start"
  _service_command(daemon_name, action)
  try:
    statement = (
      f"CREATE DATABASE IF NOT EXISTS `{database}`; "
      f"CREATE USER IF NOT EXISTS '{user}'@'%' IDENTIFIED BY '{password}'; "
      f"ALTER USER '{user}'@'%' IDENTIFIED BY '{password}'; "
      f"GRANT ALL PRIVILEGES ON `{database}`.* TO '{user}'@'%'; "
      "FLUSH PRIVILEGES;"
    )
    with private_temporary_file(
      statement,
      "root",
      "root",
      temp_dir=params.tmp_dir,
      prefix="ambari-hive-mysql-",
    ) as sql_file:
      Execute(
        (
          "bash",
          "-c",
          'exec mysql -u root < "$1"',
          "ambari-hive-mysql",
          sql_file,
        ),
        sudo=True,
        tries=3,
        try_sleep=5,
        logoutput=False,
        timeout=120,
        timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
      )
  except Exception:
    if not was_running:
      _stop_service_without_masking(daemon_name)
    raise
  else:
    if not was_running:
      _service_command(daemon_name, "stop")


def mysql_deluser():
  import params

  user = _sql_identifier(params.hive_metastore_user_name, "Hive database user")
  daemon_name = get_daemon_name()
  was_running = _service_is_running(daemon_name)
  if not was_running:
    _service_command(daemon_name, "start")
  try:
    Execute(
      (
        "mysql",
        "-u",
        "root",
        "-e",
        f"DROP USER IF EXISTS '{user}'@'%'; FLUSH PRIVILEGES;",
      ),
      sudo=True,
      tries=3,
      try_sleep=5,
      timeout=120,
      timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    )
  except Exception:
    if not was_running:
      _stop_service_without_masking(daemon_name)
    raise
  else:
    if not was_running:
      _service_command(daemon_name, "stop")


def _service_is_running(daemon_name):
  return_code, _ = shell.call(
    ("service", daemon_name, "status"),
    sudo=True,
    timeout=30,
    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
    shell=False,
  )
  return return_code == 0


def _service_command(daemon_name, action):
  Execute(
    ("service", daemon_name, action),
    sudo=True,
    logoutput=True,
    timeout=120,
    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
  )


def _stop_service_without_masking(daemon_name):
  try:
    _service_command(daemon_name, "stop")
  except Exception as cleanup_error:
    Logger.error(
      f"Could not restore stopped MySQL service {daemon_name}: {cleanup_error}"
    )


def _sql_identifier(value, description):
  value = str(value or "")
  if not re.fullmatch(r"[A-Za-z0-9_$.-]+", value):
    raise Fail(f"{description} contains unsupported characters")
  return value


def _sql_literal(value):
  return str(value or "").replace("\\", "\\\\").replace("'", "''")
