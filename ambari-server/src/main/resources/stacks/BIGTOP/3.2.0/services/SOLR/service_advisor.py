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

# Python imports
from ambari_commons import import_utils
import os
import re

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../../../stacks/")
PARENT_FILE = os.path.join(STACKS_DIR, "service_advisor.py")
if "BASE_SERVICE_ADVISOR" in os.environ:
  PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]

try:
  with open(PARENT_FILE, "rb") as fp:
    service_advisor = import_utils.load_module(
      "service_advisor", fp, PARENT_FILE, (".py", "rb", import_utils.PY_SOURCE)
    )
except Exception as error:
  raise RuntimeError(f"Failed to load parent service advisor {PARENT_FILE}") from error


_USER_PATTERN = re.compile(r"[A-Za-z_][A-Za-z0-9_.-]*\$?", re.ASCII)
_ZNODE_PATTERN = re.compile(
  r"/(?:[A-Za-z0-9_.-]+(?:/[A-Za-z0-9_.-]+)*)?", re.ASCII
)


def _integer(value):
  if isinstance(value, bool):
    return None
  try:
    return int(value)
  except (TypeError, ValueError):
    return None


def _dedicated_directory(value):
  if (
    not isinstance(value, str)
    or not os.path.isabs(value)
    or value.startswith("//")
    or os.path.normpath(value) != value
    or any(ord(character) < 32 for character in value)
  ):
    return False
  protected = {
    "/", "/bin", "/etc", "/lib", "/lib64", "/opt", "/run", "/sbin",
    "/srv", "/tmp", "/usr", "/var", "/var/lib", "/var/log", "/var/run",
  }
  return value not in protected and not value.startswith(
    ("/boot/", "/dev/", "/proc/", "/root/", "/sys/", "/usr/")
  )


def _safe_absolute_path(value):
  return (
    isinstance(value, str)
    and os.path.isabs(value)
    and value != "/"
    and not value.startswith("//")
    and os.path.normpath(value) == value
    and not any(ord(character) < 32 for character in value)
  )


class SolrServiceAdvisor(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.initialize_logger("SolrServiceAdvisor")
    self.mastersWithMultipleInstances.add("SOLR_SERVER")
    self.cardinalitiesDict["SOLR_SERVER"] = {"min": 1}
    self.heap_size_properties = {
      "SOLR_SERVER": [
        {
          "config-name": "solr-env",
          "property": "solr_maxmem",
          "default": "2048",
        }
      ]
    }

  def getServiceComponentLayoutValidations(self, services, hosts):
    return self.getServiceComponentCardinalityValidations(services, hosts, "SOLR")

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    self.updateMountProperties(
      "solr-env",
      [("solr_datadir", "SOLR_SERVER", "/solr", "single")],
      configurations,
      services,
      hosts,
    )

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    validator = SolrValidator()
    return validator.validateListOfConfigUsingMethod(
      configurations,
      recommendedDefaults,
      services,
      hosts,
      validator.validators,
    )


class SolrValidator(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.validators = [
      ("solr-env", self.validate_environment),
      ("solr-security-json", self.validate_security),
    ]

  def validate_environment(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    ranges = {
      "solr_port": (1, 65535),
      "solr_jmx_port": (1, 65535),
      "solr_minmem": (512, 32768),
      "solr_maxmem": (512, 32768),
      "solr_java_stack_size": (1, 128),
      "solr_user_nofile_limit": (1024, 1048576),
      "solr_user_nproc_limit": (1024, 1048576),
    }
    parsed = {}
    for name, (minimum, maximum) in ranges.items():
      parsed[name] = _integer(properties.get(name))
      if (
        parsed[name] is None
        or parsed[name] < minimum
        or parsed[name] > maximum
      ):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(
              f"{name} must be an integer between {minimum} and {maximum}"
            ),
          }
        )

    if (
      parsed.get("solr_minmem") is not None
      and parsed.get("solr_maxmem") is not None
      and parsed["solr_minmem"] > parsed["solr_maxmem"]
    ):
      items.append(
        {
          "config-name": "solr_minmem",
          "item": self.getErrorItem(
            "Solr minimum heap must not exceed maximum heap"
          ),
        }
      )
    if parsed.get("solr_port") == parsed.get("solr_jmx_port"):
      items.append(
        {
          "config-name": "solr_jmx_port",
          "item": self.getErrorItem("Solr HTTP and JMX ports must be different"),
        }
      )

    directories = ("solr_datadir", "solr_log_dir", "solr_pid_dir")
    values = []
    for name in directories:
      value = properties.get(name)
      values.append(value)
      if not _dedicated_directory(value):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(
              f"{name} must be a dedicated absolute directory"
            ),
          }
        )
    if len(set(values)) != len(values):
      items.append(
        {
          "config-name": "solr_datadir",
          "item": self.getErrorItem(
            "Solr data, log, and PID directories must be distinct"
          ),
        }
      )

    znode = properties.get("solr_znode")
    normalized_znode = znode.rstrip("/") if isinstance(znode, str) else "/"
    if (
      normalized_znode == "/"
      or _ZNODE_PATTERN.fullmatch(normalized_znode) is None
    ):
      items.append(
        {
          "config-name": "solr_znode",
          "item": self.getErrorItem("Solr ZooKeeper znode is invalid"),
        }
      )

    for name in (
      "solr_ssl_enabled",
      "solr_jmx_enabled",
      "solr_zookeeper_external_enabled",
    ):
      if str(properties.get(name, "")).strip().lower() not in ("true", "false"):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} must be true or false"),
          }
        )
    if _USER_PATTERN.fullmatch(str(properties.get("solr_user", ""))) is None:
      items.append(
        {
          "config-name": "solr_user",
          "item": self.getErrorItem("Solr user is invalid"),
        }
      )
    for name in ("solr_keystore_location", "solr_truststore_location"):
      if not _safe_absolute_path(properties.get(name)):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} must be a safe absolute path"),
          }
        )
    if str(properties.get("solr_ssl_enabled", "false")).strip().lower() == "true":
      for name in ("solr_keystore_password", "solr_truststore_password"):
        value = properties.get(name)
        if not isinstance(value, str) or not value or any(
          character in value for character in ("\x00", "\r", "\n")
        ):
          items.append(
            {
              "config-name": name,
              "item": self.getErrorItem(
                f"{name} is required when Solr SSL is enabled"
              ),
            }
          )
    return self.toConfigurationValidationProblems(items, "solr-env")

  def validate_security(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    roles = tuple(
      properties.get(name)
      for name in (
        "solr_role_ranger_admin",
        "solr_role_ranger_audit",
        "solr_role_dev",
      )
    )
    if any(
      not isinstance(role, str) or _USER_PATTERN.fullmatch(role) is None
      for role in roles
    ):
      items.append(
        {
          "config-name": "solr_role_dev",
          "item": self.getErrorItem(
            "Every Solr security role must be a valid role name"
          ),
        }
      )
    elif len(set(roles)) != len(roles):
      items.append(
        {
          "config-name": "solr_role_dev",
          "item": self.getErrorItem("Solr security roles must be unique"),
        }
      )

    audit_users = tuple(
      user.strip()
      for user in str(properties.get("solr_ranger_audit_service_users", "")).split(",")
      if user.strip()
    )
    if any(_USER_PATTERN.fullmatch(user) is None for user in audit_users):
      items.append(
        {
          "config-name": "solr_ranger_audit_service_users",
          "item": self.getErrorItem(
            "Every Ranger audit service user must be a valid user name"
          ),
        }
      )
    elif len(set(audit_users)) != len(audit_users):
      items.append(
        {
          "config-name": "solr_ranger_audit_service_users",
          "item": self.getErrorItem(
            "Ranger audit service users must be unique"
          ),
        }
      )
    elif {"atlas", "knox", "logsearch", "nifi", "storm"}.intersection(
      audit_users
    ):
      items.append(
        {
          "config-name": "solr_ranger_audit_service_users",
          "item": self.getErrorItem(
            "Ranger audit users contain services unavailable in BIGTOP"
          ),
        }
      )
    manual_management = str(
      properties.get("solr_security_manually_managed", "")
    ).strip().lower()
    if manual_management not in (
      "true",
      "false",
    ):
      items.append(
        {
          "config-name": "solr_security_manually_managed",
          "item": self.getErrorItem(
            "solr_security_manually_managed must be true or false"
          ),
        }
      )
    return self.toConfigurationValidationProblems(items, "solr-security-json")
