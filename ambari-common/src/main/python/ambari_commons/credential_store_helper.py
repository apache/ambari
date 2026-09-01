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

from contextlib import contextmanager
import fcntl
import os
import re
import shutil
import struct
import subprocess
import tempfile

from resource_management.core.resources.system import File, Execute
from resource_management.core.exceptions import Fail
from resource_management.core.shell import checked_call
from resource_management.core.source import DownloadSource

credential_util_cmd = "org.apache.ambari.server.credentialapi.CredentialUtil"
credential_util_jar = "CredentialUtil.jar"
credential_store_create_cmd = (
  "org.apache.ambari.tools.credential.CredentialStoreCreate"
)
credential_store_create_lib_path = "/var/lib/ambari-agent/cred/lib/*"
max_credential_bytes = 1024 * 1024
local_jceks_prefix = "jceks://file"


def _local_credential_store_path(provider_path):
  if provider_path.startswith(local_jceks_prefix):
    return provider_path[len(local_jceks_prefix) :]
  return None


def _checksum_path(store_path):
  return os.path.join(
    os.path.dirname(store_path), f".{os.path.basename(store_path)}.crc"
  )


@contextmanager
def credential_store_lock(provider_path):
  """Serialize local JCEKS updates without placing secrets in the lock file."""
  store_path = _local_credential_store_path(provider_path)
  if store_path is None:
    yield
    return

  lock_path = os.path.join(
    os.path.dirname(store_path), f".{os.path.basename(store_path)}.lock"
  )
  descriptor = os.open(lock_path, os.O_CREAT | os.O_RDWR, 0o600)
  try:
    fcntl.flock(descriptor, fcntl.LOCK_EX)
    yield
  finally:
    fcntl.flock(descriptor, fcntl.LOCK_UN)
    os.close(descriptor)


def _run_credential_store_create(command, payload):
  return subprocess.run(command, input=payload, check=False).returncode


def _commit_local_credential_store(command, payload, provider_path):
  store_path = _local_credential_store_path(provider_path)
  store_directory = os.path.dirname(store_path)
  staging_directory = tempfile.mkdtemp(prefix=".jceks-", dir=store_directory)
  temporary_path = os.path.join(staging_directory, os.path.basename(store_path))
  try:
    if os.path.isfile(store_path):
      shutil.copy2(store_path, temporary_path)
    temporary_provider = f"{local_jceks_prefix}{temporary_path}"
    temporary_command = list(command)
    temporary_command[temporary_command.index(provider_path)] = temporary_provider
    result = _run_credential_store_create(temporary_command, payload)
    if result != 0 or not os.path.isfile(temporary_path):
      return result if result != 0 else 1

    descriptor = os.open(temporary_path, os.O_RDONLY)
    try:
      os.fsync(descriptor)
    finally:
      os.close(descriptor)

    directory_descriptor = os.open(store_directory, os.O_RDONLY)
    try:
      try:
        os.unlink(_checksum_path(store_path))
      except FileNotFoundError:
        pass
      os.fsync(directory_descriptor)
      os.replace(temporary_path, store_path)
      os.fsync(directory_descriptor)
    finally:
      os.close(directory_descriptor)
    return 0
  finally:
    shutil.rmtree(staging_directory, ignore_errors=True)


def create_credential_store_entry(
  java_bin,
  cs_lib_path,
  alias,
  provider_path,
  password,
  overwrite=False,
):
  """Create a credential without exposing it in the process argument list."""
  if not isinstance(password, str):
    raise TypeError("password must be a string")

  credential = bytearray(password, "utf-8")
  if len(credential) > max_credential_bytes:
    credential[:] = b"\0" * len(credential)
    raise ValueError(
      f"UTF-8 credential exceeds the {max_credential_bytes}-byte limit"
    )

  classpath_entries = [credential_store_create_lib_path]
  classpath_entries.extend(cs_lib_path.split(os.pathsep))
  classpath = os.pathsep.join(dict.fromkeys(filter(None, classpath_entries)))
  cmd = [
    java_bin,
    "-cp",
    classpath,
    credential_store_create_cmd,
    "create",
    alias,
    "-provider",
    provider_path,
  ]
  if overwrite:
    cmd.append("-f")

  payload = bytearray(struct.pack(">I", len(credential)))
  payload.extend(credential)
  try:
    if _local_credential_store_path(provider_path) is None:
      return _run_credential_store_create(cmd, payload)
    with credential_store_lock(provider_path):
      return _commit_local_credential_store(cmd, payload, provider_path)
  finally:
    credential[:] = b"\0" * len(credential)
    payload[:] = b"\0" * len(payload)


def removeloglines(lines):
  regex = re.compile(r"^(([0-1][0-9])|([2][0-3])):([0-5][0-9])(:[0-5][0-9])[,]\d{1,3}")
  cleanlines = [x for x in lines if not regex.match(x)]
  return cleanlines


def downloadjar(cs_lib_path, jdk_location):
  # Try to download CredentialUtil.jar from ambari-server resources
  credential_util_dir = cs_lib_path.split("*")[0].split(":")[
    -1
  ]  # Remove the trailing '*' and get the last directory if an entire path is passed
  credential_util_path = os.path.join(credential_util_dir, credential_util_jar)
  credential_util_url = jdk_location + "/" + credential_util_jar
  File(
    credential_util_path,
    content=DownloadSource(credential_util_url),
    mode=0o755,
  )


def get_password_from_credential_store(
  alias, provider_path, cs_lib_path, java_home, jdk_location
):
  downloadjar(cs_lib_path, jdk_location)

  # Execute a get command on the CredentialUtil CLI to get the password for the specified alias
  java_bin = f"{java_home}/bin/java"
  cmd = (
    java_bin,
    "-cp",
    cs_lib_path,
    credential_util_cmd,
    "get",
    alias,
    "-provider",
    provider_path,
  )
  cmd_result, std_out_msg = checked_call(cmd, quiet=True)
  std_out_lines = std_out_msg.split("\n")
  return std_out_lines[-1]  # Get the last line of the output, to skip warnings if any.


def list_aliases_from_credential_store(
  provider_path, cs_lib_path, java_home, jdk_location
):
  downloadjar(cs_lib_path, jdk_location)

  # Execute a get command on the CredentialUtil CLI to list all the aliases
  java_bin = f"{java_home}/bin/java"
  cmd = (
    java_bin,
    "-cp",
    cs_lib_path,
    credential_util_cmd,
    "list",
    "-provider",
    provider_path,
  )
  cmd_result, std_out_msg = checked_call(cmd, quiet=True)
  std_out_lines = std_out_msg.split("\n")
  return removeloglines(std_out_lines)[
    1:
  ]  # Get the last line of the output, to skip warnings if any.


def delete_alias_from_credential_store(
  alias, provider_path, cs_lib_path, java_home, jdk_location
):
  downloadjar(cs_lib_path, jdk_location)

  # Execute the creation and overwrite password
  java_bin = f"{java_home}/bin/java"
  cmd = (
    java_bin,
    "-cp",
    cs_lib_path,
    credential_util_cmd,
    "delete",
    alias,
    "-provider",
    provider_path,
    "-f",
  )
  Execute(cmd)


def create_password_in_credential_store(
  alias, provider_path, cs_lib_path, java_home, jdk_location, password
):
  java_bin = f"{java_home}/bin/java"
  cmd_result = create_credential_store_entry(
    java_bin,
    cs_lib_path,
    alias,
    provider_path,
    password,
    overwrite=True,
  )
  if cmd_result != 0:
    raise Fail(
      f"Credential store update failed with exit code {cmd_result}"
    )
