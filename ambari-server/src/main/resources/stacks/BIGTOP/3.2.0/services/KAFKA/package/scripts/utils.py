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

from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.setup_ranger_plugin_xml import (
  require_external_ranger_credentials,
)


_CONFIG_SEGMENT_PATTERN = re.compile(
  r"[A-Za-z0-9][A-Za-z0-9._-]{0,254}", re.ASCII
)


def as_bool(value, name):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "true":
      return True
    if normalized == "false":
      return False
  raise Fail(f"{name} must be true or false")


def http_policy_scheme(value, name):
  policy = str(value).strip().upper()
  if policy == "HTTP_ONLY":
    return "http"
  if policy == "HTTPS_ONLY":
    return "https"
  raise Fail(f"{name} must be HTTP_ONLY or HTTPS_ONLY")


def as_yes_no(value, name):
  if isinstance(value, bool):
    return value
  if isinstance(value, str):
    normalized = value.strip().lower()
    if normalized == "yes":
      return True
    if normalized == "no":
      return False
  raise Fail(f"{name} must be Yes or No")


def validate_config_segment(value, name):
  if (
    not isinstance(value, str)
    or _CONFIG_SEGMENT_PATTERN.fullmatch(value) is None
  ):
    raise Fail(f"{name} must be a single filesystem-safe configuration segment")
  return value


def ranger_environment(configurations, has_ranger_admin):
  if has_ranger_admin:
    managed_environment = configurations.get("ranger-env")
    if not isinstance(managed_environment, dict):
      raise Fail("Managed Ranger integration requires the ranger-env configuration")
    return managed_environment

  external = require_external_ranger_credentials(
    configurations.get("ranger-kafka-plugin-properties", {})
  )
  return {
    "admin_username": external["external_admin_username"],
    "admin_password": external["external_admin_password"],
    "ranger_admin_username": external["external_ranger_admin_username"],
    "ranger_admin_password": external["external_ranger_admin_password"],
  }


def get_bare_principal(normalized_principal_name):
  """
  Given a normalized principal name (nimbus/c6501.ambari.apache.org@EXAMPLE.COM) returns just the
  primary component (nimbus)
  :param normalized_principal_name: a string containing the principal name to process
  :return: a string containing the primary component value or None if not valid
  """

  if not normalized_principal_name:
    return None
  match = re.fullmatch(r"([^/@]+)(?:/[^@]+)?(?:@[^@]+)?", normalized_principal_name)
  return match.group(1) if match else None
