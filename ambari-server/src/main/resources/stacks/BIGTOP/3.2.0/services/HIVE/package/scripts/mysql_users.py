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
from resource_management.core.resources.system import Execute
from resource_management.core.utils import PasswordString


def mysql_adduser():
  import params

  user = _sql_identifier(params.hive_metastore_user_name, "Hive database user")
  database = _sql_identifier(params.hive_db_schema_name, "Hive database schema")
  password = _sql_literal(params.hive_metastore_user_passwd)
  daemon_name = get_daemon_name()
  was_running = _service_is_running(daemon_name)

  action = "restart" if was_running else "start"
  Execute(("service", daemon_name, action), sudo=True, logoutput=True)
  try:
    statement = (
      f"CREATE DATABASE IF NOT EXISTS `{database}`; "
      f"CREATE USER IF NOT EXISTS '{user}'@'%' IDENTIFIED BY '{password}'; "
      f"ALTER USER '{user}'@'%' IDENTIFIED BY '{password}'; "
      f"GRANT ALL PRIVILEGES ON `{database}`.* TO '{user}'@'%'; "
      "FLUSH PRIVILEGES;"
    )
    Execute(
      ("mysql", "-u", "root", "-e", PasswordString(statement)),
      sudo=True,
      tries=3,
      try_sleep=5,
      logoutput=False,
    )
  finally:
    if not was_running:
      Execute(("service", daemon_name, "stop"), sudo=True, logoutput=True)


def mysql_deluser():
  import params

  user = _sql_identifier(params.hive_metastore_user_name, "Hive database user")
  daemon_name = get_daemon_name()
  was_running = _service_is_running(daemon_name)
  if not was_running:
    Execute(("service", daemon_name, "start"), sudo=True, logoutput=True)
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
    )
  finally:
    if not was_running:
      Execute(("service", daemon_name, "stop"), sudo=True, logoutput=True)


def _service_is_running(daemon_name):
  return_code, _ = shell.call(
    ("service", daemon_name, "status"),
    sudo=True,
  )
  return return_code == 0


def _sql_identifier(value, description):
  value = str(value or "")
  if not re.fullmatch(r"[A-Za-z0-9_$.-]+", value):
    raise Fail(f"{description} contains unsupported characters")
  return value


def _sql_literal(value):
  return str(value or "").replace("\\", "\\\\").replace("'", "''")
