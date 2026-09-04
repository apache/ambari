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

import ipaddress
import math
import os
import re
import shlex
from urllib.parse import urlsplit

from resource_management.core.exceptions import Fail


_SAFE_PATH_PATTERN = re.compile(r"/[A-Za-z0-9._+@/-]+", re.ASCII)
_SAFE_NAME_PATTERN = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,254}", re.ASCII)
_SAFE_JVM_TOKEN_PATTERN = re.compile(r"-[A-Za-z0-9._+,:=/@%-]+", re.ASCII)
_SAFE_PRINCIPAL_PATTERN = re.compile(r"[A-Za-z0-9._/@-]+", re.ASCII)
_PROTECTED_DIRECTORIES = {
  "/",
  "/bin",
  "/boot",
  "/dev",
  "/etc",
  "/home",
  "/lib",
  "/lib64",
  "/opt",
  "/proc",
  "/root",
  "/run",
  "/sbin",
  "/srv",
  "/sys",
  "/tmp",
  "/usr",
  "/var",
  "/var/lib",
  "/var/log",
  "/var/run",
}


def parse_bool(value, label):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise Fail(f"{label} must be true or false")


def parse_positive_int(value, label, maximum=None):
  if isinstance(value, bool):
    raise Fail(f"{label} must be a positive integer")
  if isinstance(value, int):
    parsed = value
  elif isinstance(value, str) and re.fullmatch(r"[0-9]+", value.strip(), re.ASCII):
    parsed = int(value)
  else:
    raise Fail(f"{label} must be a positive integer")
  if parsed <= 0 or (maximum is not None and parsed > maximum):
    suffix = f" no greater than {maximum}" if maximum is not None else ""
    raise Fail(f"{label} must be a positive integer{suffix}")
  return parsed


def parse_port(value, label):
  return parse_positive_int(value, label, maximum=65535)


def parse_finite_float(value, label, minimum=0.0, maximum=None):
  if isinstance(value, bool):
    raise Fail(f"{label} must be numeric")
  try:
    parsed = float(value)
  except (TypeError, ValueError) as error:
    raise Fail(f"{label} must be numeric") from error
  if (
    not math.isfinite(parsed)
    or parsed < minimum
    or (maximum is not None and parsed > maximum)
  ):
    raise Fail(f"{label} is outside its supported range")
  return parsed


def validate_name(value, label):
  if not isinstance(value, str) or not _SAFE_NAME_PATTERN.fullmatch(value):
    raise Fail(f"{label} contains unsupported characters")
  return value


def validate_single_line(value, label, maximum_length=4096):
  if value is None:
    return ""
  value = str(value)
  if (
    not value
    or len(value) > maximum_length
    or any(character in value for character in "\r\n\x00")
  ):
    raise Fail(f"{label} must be a non-empty single-line value")
  return value


def validate_principal(value, label):
  if not isinstance(value, str) or not _SAFE_PRINCIPAL_PATTERN.fullmatch(value):
    raise Fail(f"{label} is invalid")
  return value


def validate_absolute_path(value, label, allowed_roots=None):
  if not isinstance(value, str) or not value:
    raise Fail(f"{label} must be a non-empty absolute path")
  normalized = os.path.normpath(value)
  if (
    not os.path.isabs(value)
    or normalized != value
    or normalized in _PROTECTED_DIRECTORIES
    or not _SAFE_PATH_PATTERN.fullmatch(value)
  ):
    raise Fail(f"{label} must be a canonical, service-specific absolute path")
  if allowed_roots and not any(
    normalized.startswith(f"{os.path.normpath(root)}/") for root in allowed_roots
  ):
    raise Fail(f"{label} is outside the allowed directory roots")
  return normalized


def local_filesystem_path(value, label):
  if not isinstance(value, str) or not value:
    raise Fail(f"{label} must be a local absolute path")
  parsed = urlsplit(value)
  if parsed.scheme:
    if parsed.scheme != "file" or parsed.netloc not in ("", "localhost"):
      raise Fail(f"{label} must use the local file filesystem")
    value = parsed.path
  return validate_absolute_path(value, label)


def parse_host_port(value, label):
  if not isinstance(value, str) or not value.strip():
    raise Fail(f"{label} must be host:port")
  endpoint = value.strip()
  parsed = urlsplit(f"//{endpoint}")
  try:
    host = parsed.hostname
    port = parsed.port
  except ValueError as error:
    raise Fail(f"{label} contains an invalid port") from error
  if (
    not host
    or port is None
    or parsed.path
    or parsed.query
    or parsed.fragment
    or parsed.username is not None
    or parsed.password is not None
  ):
    raise Fail(f"{label} must be host:port; IPv6 addresses must be bracketed")
  return validate_host(host, f"{label} host"), parse_port(port, f"{label} port")


def validate_host(value, label="Host"):
  if not isinstance(value, str) or not value.strip():
    raise Fail(f"{label} must not be empty")
  host = value.strip()
  if host.startswith("[") or host.endswith("]"):
    if not (host.startswith("[") and host.endswith("]")):
      raise Fail(f"{label} is invalid")
    host = host[1:-1]
  if ":" in host:
    try:
      ipaddress.IPv6Address(host)
    except ipaddress.AddressValueError as error:
      raise Fail(f"{label} is invalid") from error
  elif re.fullmatch(r"[0-9.]+", host, re.ASCII):
    try:
      ipaddress.IPv4Address(host)
    except ipaddress.AddressValueError as error:
      raise Fail(f"{label} is invalid") from error
  elif (
    len(host) > 253
    or any(
      not re.fullmatch(
        r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?", label_part,
        re.ASCII,
      )
      for label_part in host.split(".")
    )
  ):
    raise Fail(f"{label} is invalid")
  return host


def validate_jvm_arguments(value, label):
  if not isinstance(value, str):
    raise Fail(f"{label} must be a string")
  try:
    arguments = shlex.split(value)
  except ValueError as error:
    raise Fail(f"{label} is invalid") from error
  if not arguments or any(
    not _SAFE_JVM_TOKEN_PATTERN.fullmatch(argument) for argument in arguments
  ):
    raise Fail(f"{label} contains unsupported arguments")
  return " ".join(arguments)


def validate_classpath(value, label):
  if value in (None, ""):
    return ""
  if not isinstance(value, str):
    raise Fail(f"{label} must be a string")
  entries = value.split(":")
  for entry in entries:
    path = entry[:-2] if entry.endswith("/*") else entry
    validate_absolute_path(path, label)
  return value


def url_host(host):
  host = validate_host(host, "Metrics Collector host")
  return f"[{host}]" if ":" in host else host
