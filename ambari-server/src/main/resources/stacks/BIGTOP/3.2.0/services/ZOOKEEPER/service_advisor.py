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


class ZookeeperServiceAdvisor(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.initialize_logger("ZookeeperServiceAdvisor")
    self.mastersWithMultipleInstances.add("ZOOKEEPER_SERVER")
    self.cardinalitiesDict["ZOOKEEPER_SERVER"] = {"min": 3}
    self.heap_size_properties = {
      "ZOOKEEPER_SERVER": [
        {
          "config-name": "zookeeper-env",
          "property": "zk_server_heapsize",
          "default": "1024",
        }
      ]
    }

  def getServiceComponentLayoutValidations(self, services, hosts):
    return self.getServiceComponentCardinalityValidations(
      services, hosts, "ZOOKEEPER"
    )

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    mount_properties = [
      ("dataDir", "ZOOKEEPER_SERVER", "/hadoop/zookeeper", "single")
    ]
    self.updateMountProperties(
      "zoo.cfg", mount_properties, configurations, services, hosts
    )

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    validator = ZookeeperValidator()
    return validator.validateListOfConfigUsingMethod(
      configurations, recommendedDefaults, services, hosts, validator.validators
    )


class ZookeeperValidator(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.validators = [
      ("zoo.cfg", self.validate_zoo_cfg),
      ("zookeeper-env", self.validate_zookeeper_env),
    ]

  def validate_zoo_cfg(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    integer_ranges = {
      "tickTime": (1, None),
      "initLimit": (1, None),
      "syncLimit": (1, None),
      "clientPort": (1, 65535),
      "autopurge.snapRetainCount": (3, None),
      "autopurge.purgeInterval": (0, None),
      "admin.serverPort": (1, 65535),
    }
    for property_name, (minimum, maximum) in integer_ranges.items():
      value = _integer(properties.get(property_name))
      invalid = value is None or value < minimum
      if maximum is not None:
        invalid = invalid or value > maximum
      if invalid:
        expected = f"at least {minimum}"
        if maximum is not None:
          expected = f"between {minimum} and {maximum}"
        items.append(
          {
            "config-name": property_name,
            "item": self.getErrorItem(
              f"{property_name} must be an integer {expected}"
            ),
          }
        )

    data_dir = properties.get("dataDir")
    if (
      not isinstance(data_dir, str)
      or not os.path.isabs(data_dir)
      or os.path.normpath(data_dir) != data_dir
      or data_dir in ("/", "/data", "/var", "/var/lib")
      or any(
        data_dir == root or data_dir.startswith(root + os.path.sep)
        for root in (
          "/boot",
          "/dev",
          "/etc",
          "/home",
          "/proc",
          "/root",
          "/sys",
          "/usr",
        )
      )
    ):
      items.append(
        {
          "config-name": "dataDir",
          "item": self.getErrorItem(
            "dataDir must be a dedicated absolute directory"
          ),
        }
      )

    whitelist = properties.get("4lw.commands.whitelist", "")
    commands = {
      command.strip() for command in str(whitelist).split(",") if command.strip()
    }
    if "*" in commands:
      items.append(
        {
          "config-name": "4lw.commands.whitelist",
          "item": self.getErrorItem(
            "Do not enable every ZooKeeper four-letter command"
          ),
        }
      )
    elif "ruok" not in commands:
      items.append(
        {
          "config-name": "4lw.commands.whitelist",
          "item": self.getErrorItem(
            "The Ambari ZooKeeper port alert requires the ruok command"
          ),
        }
      )

    client_port = _integer(properties.get("clientPort"))
    admin_enabled = str(properties.get("admin.enableServer", "true")).lower()
    admin_port = _integer(properties.get("admin.serverPort"))
    if admin_enabled not in ("true", "false"):
      items.append(
        {
          "config-name": "admin.enableServer",
          "item": self.getErrorItem(
            "admin.enableServer must be true or false"
          ),
        }
      )
    if admin_enabled == "true" and client_port == admin_port:
      items.append(
        {
          "config-name": "admin.serverPort",
          "item": self.getErrorItem(
            "admin.serverPort must differ from clientPort"
          ),
        }
      )

    return self.toConfigurationValidationProblems(items, "zoo.cfg")

  def validate_zookeeper_env(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = []
    heap_size = _integer(properties.get("zk_server_heapsize"))
    if heap_size is None or not 256 <= heap_size <= 32768:
      items.append(
        {
          "config-name": "zk_server_heapsize",
          "item": self.getErrorItem(
            "zk_server_heapsize must be between 256 and 32768 MB"
          ),
        }
      )

    for property_name in ("zk_log_dir", "zk_pid_dir"):
      path = properties.get(property_name)
      if (
        not isinstance(path, str)
        or not os.path.isabs(path)
        or os.path.normpath(path) != path
        or path in ("/", "/data", "/var", "/var/log", "/var/run")
        or any(
          path == root or path.startswith(root + os.path.sep)
          for root in (
            "/boot",
            "/dev",
            "/etc",
            "/home",
            "/proc",
            "/root",
            "/sys",
            "/usr",
          )
        )
      ):
        items.append(
          {
            "config-name": property_name,
            "item": self.getErrorItem(
              f"{property_name} must be a dedicated absolute directory"
            ),
          }
        )

    return self.toConfigurationValidationProblems(items, "zookeeper-env")
