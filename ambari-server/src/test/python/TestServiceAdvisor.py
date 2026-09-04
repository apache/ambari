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

from ambari_commons import import_utils
import json
import os
from unittest import TestCase


class TestServiceAdvisor(TestCase):
  test_directory = os.path.dirname(os.path.abspath(__file__))
  resources_path = os.path.join(test_directory, "../../main/resources")

  ambari_configuration_path = os.path.abspath(
    os.path.join(resources_path, "stacks/ambari_configuration.py")
  )
  with open(ambari_configuration_path, "rb") as fp:
    import_utils.load_module(
      "ambari_configuration",
      fp,
      ambari_configuration_path,
      (".py", "rb", import_utils.PY_SOURCE),
    )

  stack_advisor_path = os.path.join(resources_path, "stacks/stack_advisor.py")
  with open(stack_advisor_path, "rb") as fp:
    import_utils.load_module(
      "stack_advisor", fp, stack_advisor_path, (".py", "rb", import_utils.PY_SOURCE)
    )

  serviceAdvisorPath = os.path.join(resources_path, "stacks/service_advisor.py")
  with open(serviceAdvisorPath, "rb") as fp:
    service_advisor_impl = import_utils.load_module(
      "service_advisor_impl", fp, serviceAdvisorPath, (".py", "rb", import_utils.PY_SOURCE)
    )

  victoria_metrics_advisor_path = os.path.join(
    resources_path,
    "stacks/BIGTOP/3.2.0/services/VICTORIAMETRICS/service_advisor.py",
  )
  with open(victoria_metrics_advisor_path, "rb") as fp:
    victoria_metrics_advisor_impl = import_utils.load_module(
      "victoria_metrics_service_advisor",
      fp,
      victoria_metrics_advisor_path,
      (".py", "rb", import_utils.PY_SOURCE),
    )

  def setUp(self):
    serviceAdvisorClass = getattr(self.service_advisor_impl, "ServiceAdvisor")
    self.serviceAdvisor = serviceAdvisorClass()

  def load_json(self, filename):
    file = os.path.join(self.test_directory, filename)
    with open(file, "rb") as f:
      data = json.load(f)
    return data

  def test_getServiceComponentCardinalityValidations(self):
    """Test getServiceComponentCardinalityValidations"""
    services = self.load_json("hdfs.json")
    hosts = self.load_json("validation-hosts.json")

    validations = self.serviceAdvisor.getServiceComponentCardinalityValidations(
      services, hosts, "HDFS"
    )
    self.assertEqual(len(validations), 1)
    expected = {
      "type": "host-component",
      "level": "ERROR",
      "component-name": "DATANODE",
      "message": "You have selected 0 DataNode components. Please consider that at least 1 DataNode components should be installed in cluster.",
    }
    self.assertEqual(validations[0], expected)

    validations = self.serviceAdvisor.getServiceComponentCardinalityValidations(
      services, hosts, "HBASE"
    )
    self.assertEqual(len(validations), 0)

    services["services"][0]["components"][0]["StackServiceComponents"][
      "hostnames"
    ].append("c7402.ambari.apache.org")

    validations = self.serviceAdvisor.getServiceComponentCardinalityValidations(
      services, hosts, "HDFS"
    )
    self.assertEqual(len(validations), 0)

  def test_victoria_metrics_single_mode_layout(self):
    advisor = self.victoria_metrics_advisor_impl.VictoriaMetricsServiceAdvisor()
    services = self._victoria_metrics_services("single")

    self.assertEqual(
      ["host1"],
      advisor.getHostsForMasterComponent(
        services,
        {},
        self._component("VICTORIAMETRICS_SERVER", "MASTER", "0-1"),
        ["host1"],
      ),
    )
    self.assertEqual(
      [],
      advisor.getHostsForMasterComponent(
        services, {}, self._component("VMINSERT", "MASTER", "0+"), ["host1"]
      ),
    )
    self.assertEqual(
      [],
      advisor.getHostsForSlaveComponent(
        services,
        {},
        self._component("VMSTORAGE", "SLAVE", "0+"),
        ["host1"],
        ["host1"],
      ),
    )
    self.assertEqual(
      ["host1"],
      advisor.getHostsForSlaveComponent(
        services,
        {},
        self._component("VMAGENT", "SLAVE", "0+"),
        ["host1"],
        ["host1"],
      ),
    )

  def test_victoria_metrics_cluster_mode_layout(self):
    advisor = self.victoria_metrics_advisor_impl.VictoriaMetricsServiceAdvisor()
    services = self._victoria_metrics_services("cluster")

    self.assertEqual(
      [],
      advisor.getHostsForMasterComponent(
        services,
        {},
        self._component("VICTORIAMETRICS_SERVER", "MASTER", "0-1"),
        ["host1"],
      ),
    )
    self.assertEqual(
      ["host1"],
      advisor.getHostsForMasterComponent(
        services, {}, self._component("VMSELECT", "MASTER", "0+"), ["host1"]
      ),
    )
    self.assertEqual(
      ["host1"],
      advisor.getHostsForSlaveComponent(
        services,
        {},
        self._component("VMSTORAGE", "SLAVE", "0+"),
        ["host1"],
        ["host1"],
      ),
    )

  def test_victoria_metrics_layout_preserves_explicit_assignments(self):
    advisor = self.victoria_metrics_advisor_impl.VictoriaMetricsServiceAdvisor()
    component = self._component("VMINSERT", "MASTER", "0+", ["host1"])

    self.assertEqual(
      ["host1"],
      advisor.getHostsForMasterComponent(
        self._victoria_metrics_services("single"), {}, component, ["host1"]
      ),
    )

  @staticmethod
  def _victoria_metrics_services(mode):
    return {
      "configurations": {
        "victoriametrics": {"properties": {"deployment_mode": mode}}
      }
    }

  @staticmethod
  def _component(name, category, cardinality, hostnames=None):
    return {
      "StackServiceComponents": {
        "component_name": name,
        "component_category": category,
        "is_master": category == "MASTER",
        "cardinality": cardinality,
        "hostnames": hostnames or [],
      }
    }
