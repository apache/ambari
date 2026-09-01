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
import re
from urllib.parse import urlsplit

from ambari_commons import import_utils


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../../../stacks/")
PARENT_FILE = os.environ.get(
  "BASE_SERVICE_ADVISOR", os.path.join(STACKS_DIR, "service_advisor.py")
)
with open(PARENT_FILE, "rb") as fp:
  service_advisor = import_utils.load_module(
    "service_advisor", fp, PARENT_FILE, (".py", "rb", import_utils.PY_SOURCE)
  )


def _integer(value):
  if isinstance(value, bool):
    return None
  try:
    return int(value)
  except (TypeError, ValueError):
    return None


def _safe_hdfs_uri(value):
  if not isinstance(value, str) or not value or any(
    character.isspace() or ord(character) < 32 or ord(character) == 127
    for character in value
  ):
    return False
  try:
    parsed = urlsplit(value)
    port = parsed.port
  except ValueError:
    return False
  return (
    parsed.scheme == "hdfs"
    and re.fullmatch(
      r"(?:[A-Za-z0-9_.-]+(?::[0-9]+)?|\[[0-9A-Fa-f:]+\](?::[0-9]+)?)?",
      parsed.netloc,
      re.ASCII,
    )
    is not None
    and (port is None or 1 <= port <= 65535)
    and parsed.path.startswith("/")
    and not parsed.query
    and not parsed.fragment
    and not parsed.username
    and not parsed.password
    and all(part != ".." for part in parsed.path.split("/"))
  )


def _dedicated_directory(value):
  protected_trees = (
    "/boot",
    "/dev",
    "/etc",
    "/home",
    "/proc",
    "/root",
    "/sys",
    "/usr",
  )
  return (
    isinstance(value, str)
    and os.path.isabs(value)
    and os.path.normpath(value) == value
    and value
    not in ("/", "/data", "/run", "/tmp", "/var", "/var/log", "/var/run")
    and not any(
      value == root or value.startswith(root + os.path.sep)
      for root in protected_trees
    )
  )


class FlinkServiceAdvisor(service_advisor.ServiceAdvisor):
  def getServiceComponentLayoutValidations(self, services, hosts):
    return self.getServiceComponentCardinalityValidations(
      services, hosts, "FLINK"
    )

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    validator = FlinkValidator()
    return validator.validateListOfConfigUsingMethod(
      configurations, recommendedDefaults, services, hosts, validator.validators
    )


class FlinkValidator(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.validators = [
      ("flink-conf", self.validate_flink_conf),
      ("flink-env", self.validate_flink_env),
    ]

  def validate_flink_conf(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    integer_ranges = {
      "historyserver.web.port": (1, 65535),
      "historyserver.archive.fs.refresh-interval": (1, 2147483647),
    }
    for property_name, (minimum, maximum) in integer_ranges.items():
      value = _integer(properties.get(property_name))
      if value is None or not minimum <= value <= maximum:
        items.append(
          {
            "config-name": property_name,
            "item": self.getErrorItem(
              f"{property_name} must be between {minimum} and {maximum}"
            ),
          }
        )

    archive_properties = {
      "jobmanager.archive.fs.dir": [
        properties.get("jobmanager.archive.fs.dir")
      ],
      "historyserver.archive.fs.dir": str(
        properties.get("historyserver.archive.fs.dir", "")
      ).split(","),
    }
    for property_name, entries in archive_properties.items():
      if not entries or any(not _safe_hdfs_uri(entry.strip()) for entry in entries):
        items.append(
          {
            "config-name": property_name,
            "item": self.getErrorItem(
              f"{property_name} must contain only absolute hdfs:// URIs"
            ),
          }
        )
    return self.toConfigurationValidationProblems(items, "flink-conf")

  def validate_flink_env(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    for property_name in ("flink_log_dir", "flink_pid_dir"):
      if not _dedicated_directory(properties.get(property_name)):
        items.append(
          {
            "config-name": property_name,
            "item": self.getErrorItem(
              f"{property_name} must be a dedicated absolute directory"
            ),
          }
        )
    return self.toConfigurationValidationProblems(items, "flink-env")
