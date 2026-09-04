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
PARENT_FILE = os.path.join(STACKS_DIR, "service_advisor.py")

if "BASE_SERVICE_ADVISOR" in os.environ:
  PARENT_FILE = os.environ["BASE_SERVICE_ADVISOR"]
with open(PARENT_FILE, "rb") as fp:
  service_advisor = import_utils.load_module(
    "service_advisor", fp, PARENT_FILE, (".py", "rb", import_utils.PY_SOURCE)
  )


def _positive_int(value, default=None):
  if isinstance(value, bool):
    return default
  if isinstance(value, int):
    parsed = value
  elif isinstance(value, str):
    normalized = value.strip()
    if not normalized.isascii() or not normalized.isdecimal():
      return default
    parsed = int(normalized)
  else:
    return default
  return parsed if parsed > 0 else default


def _effective_properties(configurations, services, config_type):
  service_configurations = (services or {}).get("configurations") or {}
  current_config = service_configurations.get(config_type) or {}
  current = current_config.get("properties") or {}
  updated_config = (configurations or {}).get(config_type) or {}
  updated = updated_config.get("properties") or {}
  effective = dict(current)
  effective.update(updated)
  return effective


def _clamp_memory(value, minimum, maximum):
  return max(minimum, min(value, maximum))


class TezServiceAdvisor(service_advisor.ServiceAdvisor):
  def getServiceComponentLayoutValidations(self, services, hosts):
    return self.getServiceComponentCardinalityValidations(services, hosts, "TEZ")

  def getServiceConfigurationRecommendations(
    self, configurations, clusterData, services, hosts
  ):
    recommender = TezRecommender()
    recommender.recommendBigtopConfigurations(
      configurations, clusterData, services, hosts
    )

  def getServiceConfigurationsValidationItems(
    self, configurations, recommendedDefaults, services, hosts
  ):
    validator = TezValidator()
    return validator.validateListOfConfigUsingMethod(
      configurations, recommendedDefaults, services, hosts, validator.validators
    )


class TezRecommender(service_advisor.ServiceAdvisor):
  """Recommend Tez 0.10 resource values for the BIGTOP YARN runtime."""

  def recommendBigtopConfigurations(
    self, configurations, clusterData, services, hosts
  ):
    yarn_properties = _effective_properties(configurations, services, "yarn-site")
    if not {
      "yarn.scheduler.minimum-allocation-mb",
      "yarn.scheduler.maximum-allocation-mb",
    }.issubset(yarn_properties):
      self.calculateYarnAllocationSizes(configurations, services, hosts)
      yarn_properties = _effective_properties(
        configurations, services, "yarn-site"
      )

    minimum = _positive_int(
      yarn_properties.get("yarn.scheduler.minimum-allocation-mb"), 1024
    )
    maximum = _positive_int(
      yarn_properties.get("yarn.scheduler.maximum-allocation-mb"), 8192
    )
    maximum = max(minimum, maximum)

    cluster_data = clusterData or {}
    am_memory = _positive_int(cluster_data.get("amMemory"), 2048)
    if am_memory < 3072:
      am_memory *= 2
    am_memory = _clamp_memory(am_memory, minimum, maximum)

    map_memory = _positive_int(cluster_data.get("mapMemory"), 1536)
    reduce_memory = _positive_int(cluster_data.get("reduceMemory"), 1536)
    task_memory = max(map_memory, reduce_memory)
    ram_per_container = _positive_int(
      cluster_data.get("ramPerContainer"), maximum
    )
    task_memory = min(task_memory, ram_per_container)

    hive_properties = _effective_properties(configurations, services, "hive-site")
    hive_container_size = _positive_int(
      hive_properties.get("hive.tez.container.size")
    )
    if hive_container_size is not None:
      task_memory = hive_container_size
    task_memory = _clamp_memory(task_memory, minimum, maximum)

    put_tez_property = self.putProperty(configurations, "tez-site", services)
    put_tez_property("tez.am.resource.memory.mb", am_memory)
    put_tez_property("tez.task.resource.memory.mb", task_memory)
    put_tez_property("tez.runtime.io.sort.mb", max(1, int(task_memory * 0.264)))
    put_tez_property(
      "tez.runtime.unordered.output.buffer.size-mb",
      max(1, int(task_memory * 0.075)),
    )
    put_tez_property("tez.session.am.dag.submit.timeout.secs", 600)

    recommended_queue = self.recommendYarnQueue(
      services, "tez-site", "tez.queue.name"
    )
    if recommended_queue is not None:
      put_tez_property("tez.queue.name", recommended_queue)

    tez_properties = _effective_properties(configurations, services, "tez-site")
    if tez_properties.get("tez.runtime.sorter.class") == "LEGACY":
      put_tez_attribute = self.putPropertyAttribute(configurations, "tez-site")
      put_tez_attribute("tez.runtime.io.sort.mb", "maximum", 1800)


class TezValidator(service_advisor.ServiceAdvisor):
  """Validate resource recommendations against BIGTOP YARN constraints."""

  def __init__(self, *args, **kwargs):
    super().__init__(*args, **kwargs)
    self.validators = [("tez-site", self.validateBigtopConfigurations)]

  def validateBigtopConfigurations(
    self, properties, recommendedDefaults, configurations, services, hosts
  ):
    validation_items = []
    memory_property_names = (
      "tez.am.resource.memory.mb",
      "tez.task.resource.memory.mb",
      "tez.runtime.io.sort.mb",
      "tez.runtime.unordered.output.buffer.size-mb",
    )
    memory_values = {}
    for property_name in memory_property_names:
      value = _positive_int(properties.get(property_name))
      memory_values[property_name] = value
      if property_name in properties and value is None:
        validation_item = self.getErrorItem(
          f"{property_name} must be a positive integer"
        )
      else:
        validation_item = self.validatorLessThenDefaultValue(
          properties, recommendedDefaults, property_name
        )
      validation_items.append(
        {
          "config-name": property_name,
          "item": validation_item,
        }
      )

    validation_items.append(
      {
        "config-name": "tez.queue.name",
        "item": self.validatorYarnQueue(
          properties, recommendedDefaults, "tez.queue.name", services
        ),
      }
    )

    yarn_properties = _effective_properties(configurations, services, "yarn-site")
    yarn_minimum = _positive_int(
      yarn_properties.get("yarn.scheduler.minimum-allocation-mb")
    )
    yarn_maximum = _positive_int(
      yarn_properties.get("yarn.scheduler.maximum-allocation-mb")
    )
    for property_name in (
      "tez.am.resource.memory.mb",
      "tez.task.resource.memory.mb",
    ):
      memory = memory_values[property_name]
      if (
        memory is not None
        and yarn_minimum is not None
        and memory < yarn_minimum
      ):
        validation_items.append(
          {
            "config-name": property_name,
            "item": self.getWarnItem(
              f"{property_name} should not be lower than the YARN minimum "
              f"allocation ({yarn_minimum} MB)"
            ),
          }
        )
      if (
        memory is not None
        and yarn_maximum is not None
        and memory > yarn_maximum
      ):
        validation_items.append(
          {
            "config-name": property_name,
            "item": self.getWarnItem(
              f"{property_name} should not exceed the YARN maximum "
              f"allocation ({yarn_maximum} MB)"
            ),
          }
        )

    task_memory = memory_values["tez.task.resource.memory.mb"]
    if task_memory is not None:
      for property_name in (
        "tez.runtime.io.sort.mb",
        "tez.runtime.unordered.output.buffer.size-mb",
      ):
        buffer_memory = memory_values[property_name]
        if buffer_memory is not None and buffer_memory >= task_memory:
          validation_items.append(
            {
              "config-name": property_name,
              "item": self.getErrorItem(
                f"{property_name} must be lower than "
                "tez.task.resource.memory.mb"
              ),
            }
          )

    return self.toConfigurationValidationProblems(validation_items, "tez-site")
