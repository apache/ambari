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

import copy
import os
from unittest import TestCase


class TestManagedDependencyStackAdvisor(TestCase):
  def setUp(self):
    from ambari_commons import import_utils

    test_directory = os.path.dirname(os.path.abspath(__file__))
    stacks_path = os.path.join(test_directory, "../../../main/resources/stacks")
    ambari_configuration_path = os.path.join(stacks_path, "ambari_configuration.py")
    with open(ambari_configuration_path, "rb") as fp:
      import_utils.load_module(
        "ambari_configuration",
        fp,
        ambari_configuration_path,
        (".py", "rb", import_utils.PY_SOURCE),
      )
    advisor_path = os.path.join(stacks_path, "stack_advisor.py")
    with open(advisor_path, "rb") as fp:
      module = import_utils.load_module(
        "stack_advisor", fp, advisor_path, (".py", "rb", import_utils.PY_SOURCE)
      )
    self.advisor = module.DefaultStackAdvisor()

  def test_hbase_only_accepts_authorized_managed_hdfs_and_zookeeper(self):
    services = self._services({"HDFS_CLIENT", "ZOOKEEPER_SERVER"})

    self.assertEqual([], self.advisor.validateRequiredComponentsPresent(services))
    response = self.advisor.createComponentLayoutRecommendations(
      services, self._hosts()
    )

    components = {
      component["name"]
      for group in response["blueprint"]["host_groups"]
      for component in group["components"]
    }
    self.assertIn("HBASE_MASTER", components)
    self.assertNotIn("NAMENODE", components)
    self.assertNotIn("ZOOKEEPER_SERVER", components)

  def test_managed_hdfs_preserves_local_zookeeper_and_unrelated_services(self):
    services = self._services({"HDFS_CLIENT"}, include_local_zookeeper=True)
    services["services"].extend(
      [
        self._service(
          "HDFS",
          [self._component("HDFS_CLIENT", "CLIENT", ["host1"], [])],
        ),
        self._service(
          "HIVE",
          [
            self._component(
              "HIVE_SERVER",
              "MASTER",
              ["host1"],
              [self._dependency("HDFS_CLIENT", "cluster")],
            )
          ],
        ),
      ]
    )
    original = copy.deepcopy(services)

    self.assertEqual([], self.advisor.validateRequiredComponentsPresent(services))
    self.advisor.createComponentLayoutRecommendations(services, self._hosts())
    self.assertEqual(original, services)

  def test_unmanaged_and_malformed_claims_do_not_suppress_validation(self):
    services = self._services({"HDFS_CLIENT"})
    issues = self.advisor.validateRequiredComponentsPresent(services)
    self.assertEqual(1, len(issues))
    self.assertIn("ZOOKEEPER_SERVER", issues[0]["message"])

    services["managed_dependency_plan"]["satisfied_components"].append(
      "NAMENODE"
    )
    issues = self.advisor.validateRequiredComponentsPresent(services)
    self.assertEqual(2, len(issues))

  def _services(self, satisfied, include_local_zookeeper=False):
    hbase = self._service(
      "HBASE",
      [
        self._component(
          "HBASE_MASTER",
          "MASTER",
          ["host1"],
          [
            self._dependency("HDFS_CLIENT", "host"),
            self._dependency("ZOOKEEPER_SERVER", "cluster"),
          ],
        )
      ],
    )
    services = {
      "Versions": {"stack_name": "BIGTOP", "stack_version": "3.2.0"},
      "services": [hbase],
      "configurations": {},
      "managed_dependency_plan": {
        "consumer_service": "HBASE",
        "satisfied_components": sorted(satisfied),
      },
    }
    if include_local_zookeeper:
      services["services"].append(
        self._service(
          "ZOOKEEPER",
          [
            self._component(
              "ZOOKEEPER_SERVER", "MASTER", ["host1"], []
            )
          ],
        )
      )
    return services

  def _service(self, name, components):
    return {
      "StackServices": {"service_name": name},
      "components": components,
    }

  def _component(self, name, category, hosts, dependencies):
    return {
      "StackServiceComponents": {
        "component_name": name,
        "display_name": name,
        "component_category": category,
        "is_master": category == "MASTER",
        "hostnames": hosts,
        "cardinality": "1",
      },
      "dependencies": dependencies,
    }

  def _dependency(self, component_name, scope):
    return {
      "Dependencies": {
        "component_name": component_name,
        "scope": scope,
        "type": "inclusive",
      }
    }

  def _hosts(self):
    return {
      "items": [
        {
          "Hosts": {
            "host_name": "host1",
            "maintenance_state": "OFF",
            "cpu_count": 2,
            "total_mem": 4 * 1024 * 1024,
            "disk_info": [],
          }
        }
      ]
    }
