#!/usr/bin/env ambari-python-wrap
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
import shlex

from ambari_commons import import_utils


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
STACKS_DIR = os.path.join(SCRIPT_DIR, "../../../stacks")
PARENT_FILE = os.environ.get(
  "BASE_SERVICE_ADVISOR", os.path.join(STACKS_DIR, "service_advisor.py")
)
with open(PARENT_FILE, "rb") as stream:
  service_advisor = import_utils.load_module(
    "service_advisor",
    stream,
    PARENT_FILE,
    (".py", "rb", import_utils.PY_SOURCE),
  )


_ABSOLUTE_PATH_PATTERN = re.compile(r"/[A-Za-z0-9_./+@=-]*", re.ASCII)
_JAVA_OPTION_PATTERN = re.compile(r"-[A-Za-z0-9_./,:+@%=-]+", re.ASCII)
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


def _safe_absolute_path(value):
  return (
    isinstance(value, str)
    and os.path.isabs(value)
    and not value.startswith("//")
    and os.path.normpath(value) == value
    and _ABSOLUTE_PATH_PATTERN.fullmatch(value) is not None
  )


def _dedicated_directory(value):
  if not _safe_absolute_path(value):
    return False
  protected = {
    "/", "/bin", "/etc", "/lib", "/lib64", "/opt", "/run", "/sbin",
    "/srv", "/tmp", "/usr", "/var", "/var/lib", "/var/log", "/var/run",
  }
  return value not in protected and not value.startswith(
    ("/boot/", "/dev/", "/proc/", "/root/", "/sys/", "/usr/")
  )


def _safe_java_options(value):
  if not isinstance(value, str) or len(value) > 4096 or any(
    character in value for character in ("\x00", "\r", "\n")
  ):
    return False
  try:
    tokens = tuple(shlex.split(value, posix=True))
  except ValueError:
    return False
  if len(tokens) > 128:
    return False
  managed_prefixes = (
    "-Djava.rmi.server.hostname=",
    "-Djava.security.auth.login.config=",
    "-Djetty.port=",
    "-Dlog4j.configurationFile=",
    "-Dsolr.kerberos.",
    "-Dsolr.solr.home=",
    "-Dzookeeper.",
    "-DzkHost=",
  )
  return all(
    _JAVA_OPTION_PATTERN.fullmatch(token) is not None
    and not token.startswith(managed_prefixes)
    for token in tokens
  )


class Ambari_Infra_SolrServiceAdvisor(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.initialize_logger("Ambari_Infra_SolrServiceAdvisor")
    self.mastersWithMultipleInstances.add("INFRA_SOLR")
    self.cardinalitiesDict["INFRA_SOLR"] = {"min": 1}
    self.heap_size_properties = {
      "INFRA_SOLR": [
        {
          "config-name": "infra-solr-env",
          "property": "infra_solr_maxmem",
          "default": "2048",
        }
      ]
    }

  def getServiceComponentLayoutValidations(self, services, hosts):
    return self.getServiceComponentCardinalityValidations(
      services, hosts, "AMBARI_INFRA_SOLR"
    )

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    mount_properties = [
      ("infra_solr_datadir", "INFRA_SOLR", "/ambari-infra-solr", "single"),
    ]
    self.updateMountProperties(
      "infra-solr-env", mount_properties, configurations, services, hosts
    )

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    validator = AmbariInfraSolrValidator()
    return validator.validateListOfConfigUsingMethod(
      configurations,
      recommendedDefaults,
      services,
      hosts,
      validator.validators,
    )


class AmbariInfraSolrValidator(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.validators = [
      ("infra-solr-env", self.validate_environment),
      ("infra-solr-security-json", self.validate_security),
    ]

  def validate_environment(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    ranges = {
      "infra_solr_port": (1, 65535),
      "infra_solr_jmx_port": (1, 65535),
      "infra_solr_minmem": (512, 32768),
      "infra_solr_maxmem": (512, 32768),
      "infra_solr_java_stack_size": (1, 128),
      "infra_solr_user_nofile_limit": (1024, 1048576),
      "infra_solr_user_nproc_limit": (1024, 1048576),
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
      parsed.get("infra_solr_minmem") is not None
      and parsed.get("infra_solr_maxmem") is not None
      and parsed["infra_solr_minmem"] > parsed["infra_solr_maxmem"]
    ):
      items.append(
        {
          "config-name": "infra_solr_minmem",
          "item": self.getErrorItem(
            "Infra Solr minimum heap must not exceed maximum heap"
          ),
        }
      )

    port = parsed.get("infra_solr_port")
    if port is not None and port == parsed.get("infra_solr_jmx_port"):
      items.append(
        {
          "config-name": "infra_solr_jmx_port",
          "item": self.getErrorItem(
            "Infra Solr HTTP and JMX ports must be different"
          ),
        }
      )

    directories = (
      "infra_solr_datadir",
      "infra_solr_log_dir",
      "infra_solr_pid_dir",
    )
    directory_values = []
    for name in directories:
      value = properties.get(name)
      directory_values.append(value)
      if not _dedicated_directory(value):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(
              f"{name} must be a dedicated absolute directory"
            ),
          }
        )
    if len(set(directory_values)) != len(directory_values):
      items.append(
        {
          "config-name": "infra_solr_datadir",
          "item": self.getErrorItem(
            "Infra Solr data, log, and PID directories must be distinct"
          ),
        }
      )

    znode = properties.get("infra_solr_znode")
    normalized_znode = znode.rstrip("/") if isinstance(znode, str) else "/"
    if (
      normalized_znode == "/"
      or _ZNODE_PATTERN.fullmatch(normalized_znode) is None
    ):
      items.append(
        {
          "config-name": "infra_solr_znode",
          "item": self.getErrorItem("Infra Solr ZooKeeper znode is invalid"),
        }
      )

    for name in (
      "infra_solr_ssl_enabled",
      "infra_solr_jmx_enabled",
      "infra_solr_zookeeper_external_enabled",
    ):
      if str(properties.get(name, "")).lower() not in ("true", "false"):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} must be true or false"),
          }
        )
    user = properties.get("infra_solr_user")
    if not isinstance(user, str) or _USER_PATTERN.fullmatch(user) is None:
      items.append(
        {
          "config-name": "infra_solr_user",
          "item": self.getErrorItem("Infra Solr user is invalid"),
        }
      )
    if not _safe_java_options(properties.get("infra_solr_extra_java_opts", "")):
      items.append(
        {
          "config-name": "infra_solr_extra_java_opts",
          "item": self.getErrorItem(
            "Infra Solr extra Java options are unsafe or override managed values"
          ),
        }
      )
    for name in (
      "infra_solr_keystore_location",
      "infra_solr_truststore_location",
    ):
      if not _safe_absolute_path(properties.get(name)):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(f"{name} must be a safe absolute path"),
          }
        )
    if str(properties.get("infra_solr_ssl_enabled", "false")).lower() == "true":
      for name in (
        "infra_solr_keystore_password",
        "infra_solr_truststore_password",
      ):
        value = properties.get(name)
        if not isinstance(value, str) or not value or any(
          character in value for character in ("\x00", "\r", "\n")
        ):
          items.append(
            {
              "config-name": name,
              "item": self.getErrorItem(
                f"{name} is required when Infra Solr SSL is enabled"
              ),
            }
          )
    return self.toConfigurationValidationProblems(items, "infra-solr-env")

  def validate_security(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    roles = {
      name: properties.get(name)
      for name in (
        "infra_solr_role_ranger_admin",
        "infra_solr_role_ranger_audit",
        "infra_solr_role_dev",
      )
    }
    if any(
      not isinstance(value, str) or _USER_PATTERN.fullmatch(value) is None
      for value in roles.values()
    ):
      items.append(
        {
          "config-name": "infra_solr_role_dev",
          "item": self.getErrorItem(
            "Every Infra Solr security role must be a valid role name"
          ),
        }
      )
    elif len(set(roles.values())) != len(roles):
      items.append(
        {
          "config-name": "infra_solr_role_dev",
          "item": self.getErrorItem("Infra Solr security roles must be unique"),
        }
      )
    audit_users = tuple(
      user.strip()
      for user in str(
        properties.get("infra_solr_ranger_audit_service_users", "")
      ).split(",")
      if user.strip()
    )
    if any(_USER_PATTERN.fullmatch(user) is None for user in audit_users):
      items.append(
        {
          "config-name": "infra_solr_ranger_audit_service_users",
          "item": self.getErrorItem(
            "Every Ranger audit service user must be a valid user name"
          ),
        }
      )
    elif len(set(audit_users)) != len(audit_users):
      items.append(
        {
          "config-name": "infra_solr_ranger_audit_service_users",
          "item": self.getErrorItem(
            "Ranger audit service users must be unique"
          ),
        }
      )
    if str(properties.get("infra_solr_security_manually_managed", "")).lower() not in (
      "true",
      "false",
    ):
      items.append(
        {
          "config-name": "infra_solr_security_manually_managed",
          "item": self.getErrorItem(
            "infra_solr_security_manually_managed must be true or false"
          ),
        }
      )
    return self.toConfigurationValidationProblems(
      items, "infra-solr-security-json"
    )
