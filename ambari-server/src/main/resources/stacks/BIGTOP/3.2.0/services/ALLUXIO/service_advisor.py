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

from ambari_commons import import_utils


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../../../stacks/")
PARENT_FILE = os.environ.get(
  "BASE_SERVICE_ADVISOR", os.path.join(STACKS_DIR, "service_advisor.py")
)

try:
  with open(PARENT_FILE, "rb") as fp:
    service_advisor = import_utils.load_module(
      "service_advisor", fp, PARENT_FILE, (".py", "rb", import_utils.PY_SOURCE)
    )
except Exception as error:
  raise RuntimeError(
    f"Failed to load parent service advisor {PARENT_FILE}: {error}"
  ) from error


_USER_PATTERN = re.compile(r"[A-Za-z_][A-Za-z0-9_.-]*\$?", re.ASCII)
_DATA_SIZE_PATTERN = re.compile(
  r"[1-9][0-9]*(?:[KMGTPE]B)", re.IGNORECASE | re.ASCII
)
_PROTECTED_DIRECTORY_PREFIXES = (
  "/bin/",
  "/boot/",
  "/dev/",
  "/etc/",
  "/lib/",
  "/lib64/",
  "/proc/",
  "/root/",
  "/sbin/",
  "/sys/",
  "/tmp/",
  "/usr/",
)


def _integer(value):
  if isinstance(value, bool):
    return None
  if isinstance(value, int):
    return value
  if isinstance(value, str):
    normalized = value.strip()
    if normalized.isascii() and normalized.isdecimal():
      return int(normalized)
  return None


def _safe_absolute_path(value):
  return (
    isinstance(value, str)
    and os.path.isabs(value)
    and value != "/"
    and not value.startswith("//")
    and os.path.normpath(value) == value
    and not any(ord(character) < 32 for character in value)
  )


def _dedicated_directory(value):
  if not _safe_absolute_path(value):
    return False
  protected = {
    "/bin",
    "/etc",
    "/lib",
    "/lib64",
    "/opt",
    "/run",
    "/sbin",
    "/srv",
    "/tmp",
    "/usr",
    "/var",
    "/var/lib",
    "/var/log",
    "/var/run",
  }
  return value not in protected and not value.startswith(
    _PROTECTED_DIRECTORY_PREFIXES
  )


class AlluxioServiceAdvisor(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.initialize_logger("AlluxioServiceAdvisor")
    self.mastersWithMultipleInstances.add("ALLUXIO_MASTER")
    self.cardinalitiesDict["ALLUXIO_MASTER"] = {"min": 1}

  def getServiceComponentLayoutValidations(self, services, hosts):
    return self.getServiceComponentCardinalityValidations(
      services, hosts, "ALLUXIO"
    )

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    return None

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    validator = AlluxioValidator()
    return validator.validateListOfConfigUsingMethod(
      configurations,
      recommendedDefaults,
      services,
      hosts,
      validator.validators,
    )

  @staticmethod
  def isKerberosEnabled(services, configurations):
    if configurations and "core-site" in configurations:
      authentication = configurations["core-site"].get("properties", {}).get(
        "hadoop.security.authentication"
      )
      if authentication is not None:
        return str(authentication).strip().lower() == "kerberos"
    if services and "core-site" in services.get("configurations", {}):
      authentication = services["configurations"]["core-site"].get(
        "properties", {}
      ).get("hadoop.security.authentication")
      return str(authentication).strip().lower() == "kerberos"
    return False


class AlluxioValidator(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.validators = [
      ("alluxio-env", self.validate_environment),
      ("alluxio-site-properties", self.validate_site),
    ]

  def validate_environment(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    for name in ("alluxio_user", "alluxio_group"):
      value = str(properties.get(name, ""))
      if value == "root" or _USER_PATTERN.fullmatch(value) is None:
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} is invalid"),
          }
        )

    directories = ("alluxio_log_dir", "alluxio_pid_dir")
    values = []
    for name in directories:
      value = properties.get(name)
      values.append(value)
      if not _dedicated_directory(value):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} must be a safe absolute path"),
          }
        )
    if values[0] == values[1]:
      items.append(
        {
          "config-name": "alluxio_log_dir",
          "item": self.getErrorItem("Alluxio log and PID directories must differ"),
        }
      )
    return self.toConfigurationValidationProblems(items, "alluxio-env")

  def validate_site(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    port_names = (
      "alluxio.master.rpc.port",
      "alluxio.master.web.port",
      "alluxio.master.embedded.journal.port",
      "alluxio.worker.rpc.port",
      "alluxio.worker.web.port",
    )
    parsed_ports = {}
    for name in port_names:
      parsed_ports[name] = _integer(properties.get(name))
      if parsed_ports[name] is None or not 0 < parsed_ports[name] <= 65535:
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} must be between 1 and 65535"),
          }
        )

    seen_ports = {}
    for name, port in parsed_ports.items():
      if port is None:
        continue
      if port in seen_ports:
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(
              f"{name} must differ from {seen_ports[port]}"
            ),
          }
        )
      else:
        seen_ports[port] = name

    worker_memory = properties.get("alluxio.worker.memory", "")
    if not isinstance(worker_memory, str) or _DATA_SIZE_PATTERN.fullmatch(
      worker_memory.strip()
    ) is None:
      items.append(
        {
          "config-name": "alluxio.worker.memory",
          "item": self.getErrorItem(
            "alluxio.worker.memory must be a positive binary data size"
          ),
        }
      )

    for name, validator in (
      ("alluxio.underfs.hdfs.address", _safe_absolute_path),
      ("alluxio.master.metastore.dir", _dedicated_directory),
    ):
      if not validator(properties.get(name)):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} must be a safe absolute path"),
          }
        )
    return self.toConfigurationValidationProblems(items, "alluxio-site-properties")
