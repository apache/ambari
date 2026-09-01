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
PARENT_FILE = os.environ.get(
  "BASE_SERVICE_ADVISOR",
  os.path.join(SCRIPT_DIR, "../../../../../stacks/service_advisor.py"),
)
with open(PARENT_FILE, "rb") as parent_file:
  service_advisor = import_utils.load_module(
    "service_advisor",
    parent_file,
    PARENT_FILE,
    (".py", "rb", import_utils.PY_SOURCE),
  )


def _property(configurations, services, config_type, name):
  if config_type in configurations and name in configurations[config_type].get(
    "properties", {}
  ):
    return configurations[config_type]["properties"][name]
  return (
    services.get("configurations", {})
    .get(config_type, {})
    .get("properties", {})
    .get(name)
  )


class SparkServiceAdvisor(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.heap_size_properties = {
      "SPARK_JOBHISTORYSERVER": [
        {
          "config-name": "spark-env",
          "property": "spark_daemon_memory",
          "default": "2048m",
        }
      ]
    }

  def getServiceComponentLayoutValidations(self, services, hosts):
    return self.getServiceComponentCardinalityValidations(services, hosts, "SPARK")

  def isComponentUsingCardinalityForLayout(self, componentName):
    return componentName == "SPARK_THRIFTSERVER"

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    recommender = SparkRecommender()
    recommender.recommend(configurations, clusterData, services, hosts)

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    validator = SparkValidator()
    return validator.validateListOfConfigUsingMethod(
      configurations,
      recommendedDefaults,
      services,
      hosts,
      validator.validators,
    )


class SparkRecommender(service_advisor.ServiceAdvisor):
  def recommend(self, configurations, clusterData, services, hosts):
    put_spark_property = self.putProperty(configurations, "spark-defaults", services)
    queue = self.recommendYarnQueue(services, "spark-defaults", "spark.yarn.queue")
    if queue is not None:
      put_spark_property("spark.yarn.queue", queue)
    kerberos_enabled = _property(
      configurations,
      services,
      "spark-defaults",
      "spark.history.kerberos.enabled",
    )
    if str(kerberos_enabled).lower() == "true":
      put_spark_property("spark.acls.enable", "true")
      put_spark_property("spark.history.ui.acls.enable", "true")


class SparkValidator(service_advisor.ServiceAdvisor):
  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.validators = [
      ("spark-defaults", self.validate_spark_defaults),
      ("spark-env", self.validate_spark_env),
      ("spark-hive-site-override", self.validate_thrift),
    ]

  def _integer_item(self, properties, name, minimum, maximum):
    try:
      value = int(properties[name])
      if minimum <= value <= maximum:
        return None
    except (KeyError, TypeError, ValueError):
      pass
    return self.getErrorItem(f"{name} must be an integer between {minimum} and {maximum}")

  def validate_spark_defaults(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    items = [
      {
        "config-name": "spark.yarn.queue",
        "item": self.validatorYarnQueue(
          properties,
          recommendedDefaults,
          "spark.yarn.queue",
          services,
        ),
      },
      {
        "config-name": "spark.history.ui.port",
        "item": self._integer_item(
          properties, "spark.history.ui.port", 1, 65535
        ),
      },
    ]
    history_dir = properties.get("spark.history.fs.logDirectory", "")
    if not history_dir.startswith("hdfs://") or any(
      character.isspace() for character in history_dir
    ):
      items.append(
        {
          "config-name": "spark.history.fs.logDirectory",
          "item": self.getErrorItem(
            "Spark history directory must be an hdfs:// URI"
          ),
        }
      )
    return self.toConfigurationValidationProblems(items, "spark-defaults")

  def validate_spark_env(self, properties, recommendedDefaults, configurations, services, hosts):
    items = [
      {
        "config-name": "spark_daemon_memory",
        "item": self._integer_item(
          properties, "spark_daemon_memory", 256, 1048576
        ),
      }
    ]
    for name in ("spark_log_dir", "spark_pid_dir"):
      value = properties.get(name, "")
      if not value.startswith("/") or ".." in value.split("/"):
        items.append(
          {
            "config-name": name,
            "item": self.getErrorItem(
              f"{name} must be a normalized absolute path"
            ),
          }
        )
    return self.toConfigurationValidationProblems(items, "spark-env")

  def validate_thrift(self, properties, recommendedDefaults, configurations, services, hosts):
    mode = str(properties.get("hive.server2.transport.mode", "")).lower()
    items = []
    if mode not in ("binary", "http"):
      items.append(
        {
          "config-name": "hive.server2.transport.mode",
          "item": self.getErrorItem(
            "Transport mode must be binary or http"
          ),
        }
      )
    port_name = (
      "hive.server2.thrift.http.port"
      if mode == "http"
      else "hive.server2.thrift.port"
    )
    items.append(
      {
        "config-name": port_name,
        "item": self._integer_item(properties, port_name, 1, 65535),
      }
    )
    return self.toConfigurationValidationProblems(items, "spark-hive-site-override")
