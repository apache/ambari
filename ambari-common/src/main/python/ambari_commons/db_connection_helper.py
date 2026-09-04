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
import struct
import subprocess
import time

from resource_management.core.exceptions import Fail


DB_CONNECTION_VERIFICATION_CLASS = (
  "org.apache.ambari.server.DBConnectionVerification"
)
MAX_PASSWORD_BYTES = 1024 * 1024


def _run_db_connection_verification(command, payload, environment):
  return subprocess.run(
    command,
    input=payload,
    stdout=subprocess.PIPE,
    stderr=subprocess.STDOUT,
    env=environment,
    check=False,
  )


def verify_db_connection(
  java_exec,
  classpath,
  connection_url,
  username,
  password,
  driver,
  environment=None,
  java_options=None,
  tries=1,
  try_sleep=0,
):
  """Run DBConnectionVerification without exposing the password in argv."""
  arguments = (java_exec, classpath, connection_url, username, driver)
  if not all(isinstance(value, str) and value for value in arguments):
    raise ValueError("DB connection arguments must be non-empty strings")
  if not isinstance(password, str):
    raise TypeError("DB password must be a string")
  if not isinstance(tries, int) or tries < 1:
    raise ValueError("tries must be a positive integer")
  if java_options is None:
    java_options = []
  if not isinstance(java_options, (list, tuple)) or not all(
    isinstance(option, str) and option for option in java_options
  ):
    raise ValueError("java_options must contain non-empty strings")

  credential = bytearray(password, "utf-8")
  if len(credential) > MAX_PASSWORD_BYTES:
    credential[:] = b"\0" * len(credential)
    raise ValueError(
      f"UTF-8 DB password exceeds the {MAX_PASSWORD_BYTES}-byte limit"
    )

  payload = bytearray(struct.pack(">I", len(credential)))
  payload.extend(credential)
  command = [
    java_exec,
    "-cp",
    classpath,
    *java_options,
    DB_CONNECTION_VERIFICATION_CLASS,
    connection_url,
    username,
    driver,
  ]
  process_environment = os.environ.copy()
  if environment:
    process_environment.update(environment)

  last_return_code = None
  last_output = ""
  try:
    for attempt in range(tries):
      try:
        result = _run_db_connection_verification(
          command, payload, process_environment
        )
        last_return_code = result.returncode
        last_output = result.stdout.decode("utf-8", errors="replace").strip()
        if password:
          last_output = last_output.replace(password, "[REDACTED]")
        if result.returncode == 0:
          return last_output
      except OSError as err:
        last_output = err.__class__.__name__

      if attempt + 1 < tries:
        time.sleep(try_sleep)

    detail = (
      f" (exit code {last_return_code})"
      if last_return_code is not None
      else ""
    )
    message = f"DB connection verification failed{detail}"
    if last_output:
      message += f": {last_output}"
    raise Fail(message)
  finally:
    credential[:] = b"\0" * len(credential)
    payload[:] = b"\0" * len(payload)
