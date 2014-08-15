'''
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
'''

import socket
from unittest import TestCase

class TestHDP206StackAdvisor(TestCase):

  def setUp(self):
    import imp
    import os

    testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(testDirectory, '../../../../../main/resources/stacks/HDP/stack_advisor.py')
    hdp206StackAdvisorPath = os.path.join(testDirectory, '../../../../../main/resources/stacks/HDP/2.0.6/services/stack_advisor.py')
    hdp206StackAdvisorClassName = 'HDP206StackAdvisor'
    with open(stackAdvisorPath, 'rb') as fp:
      stack_advisor = imp.load_module( 'stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE) )
    with open(hdp206StackAdvisorPath, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp206StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    clazz = getattr(stack_advisor_impl, hdp206StackAdvisorClassName)
    self.stackAdvisor = clazz()

  def test_recommendationCardinalityALL(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [{"name": "GANGLIA_MONITOR", "cardinality": "ALL", "category": "SLAVE", "is_master": False}]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.recommendComponentLayout(services, hosts)

    expectedComponentsHostsMap = {
      "GANGLIA_MONITOR": ["host1", "host2"]
    }
    self.assertHostLayout(expectedComponentsHostsMap, result)

  def test_recommendationAssignmentNotChanged(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [{"name": "GANGLIA_MONITOR", "cardinality": "ALL", "category": "SLAVE", "is_master": False, "hostnames": ["host1"]}]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.recommendComponentLayout(services, hosts)

    expectedComponentsHostsMap = {
      "GANGLIA_MONITOR": ["host1"]
    }
    self.assertHostLayout(expectedComponentsHostsMap, result)

  def test_recommendationIsNotPreferableOnAmbariServer(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [{"name": "GANGLIA_SERVER", "cardinality": "ALL", "category": "MASTER", "is_master": True}]
      }
    ]
    services = self.prepareServices(servicesInfo)
    localhost = socket.getfqdn()
    hosts = self.prepareHosts([localhost, "host2"])
    result = self.stackAdvisor.recommendComponentLayout(services, hosts)

    expectedComponentsHostsMap = {
      "GANGLIA_SERVER": ["host2"]
    }
    self.assertHostLayout(expectedComponentsHostsMap, result)

  def test_validationNamenodeAndSecondaryNamenode2Hosts(self):
    servicesInfo = [
      {
        "name": "HDFS",
        "components": [
          {"name": "NAMENODE", "cardinality": "1-2", "category": "MASTER", "is_master": True, "hostnames": ["host1"]},
          {"name": "SECONDARY_NAMENODE", "cardinality": "1", "category": "MASTER", "is_master": True, "hostnames": ["host1"]}]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.validateComponentLayout(services, hosts)

    expectedMessages = [
      "NameNode and Secondary NameNode cannot be hosted on same machine",
      "NameNode and Secondary NameNode cannot be hosted on same machine",
      "Host is not used"
    ]
    self.assertValidationMessages(expectedMessages, result)

  def test_validationCardinalityALL(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [
          {"name": "GANGLIA_MONITOR", "cardinality": "ALL", "category": "SLAVE", "is_master": False, "hostnames": ["host1"]},
          {"name": "GANGLIA_SERVER", "cardinality": "1-2", "category": "MASTER", "is_master": True, "hostnames": ["host2", "host1"]}
        ]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.validateComponentLayout(services, hosts)

    expectedMessages = [
      "Cardinality violation, cardinality=ALL, hosts count=1"
    ]
    self.assertValidationMessages(expectedMessages, result)

  def test_validationHostIsNotUsedForNonValuableComponent(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [
          {"name": "GANGLIA_MONITOR", "cardinality": "ALL", "category": "SLAVE", "is_master": False, "hostnames": ["host1", "host2"]},
          {"name": "GANGLIA_SERVER", "cardinality": "1", "category": "MASTER", "is_master": True, "hostnames": ["host2"]}
        ]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.validateComponentLayout(services, hosts)

    expectedMessages = [
      "Host is not used"
    ]
    self.assertValidationMessages(expectedMessages, result)

  def test_validationCardinality01TwoHostsAssigned(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [
          {"name": "GANGLIA_SERVER", "cardinality": "0-1", "category": "MASTER", "is_master": True, "hostnames": ["host1", "host2"]}
        ]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.validateComponentLayout(services, hosts)

    expectedMessages = [
      "Cardinality violation, cardinality=0-1, hosts count=2"
    ]
    self.assertValidationMessages(expectedMessages, result)

  def test_validationHostIsNotUsed(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [
          {"name": "GANGLIA_SERVER", "cardinality": "1", "category": "MASTER", "is_master": True, "hostnames": ["host1"]}
        ]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.validateComponentLayout(services, hosts)

    expectedMessages = [
      "Host is not used"
    ]
    self.assertValidationMessages(expectedMessages, result)


  def prepareHosts(self, hostsNames):
    hosts = { "items": [] }
    for hostName in hostsNames:
      nextHost = {"Hosts":{"host_name" : hostName}}
      hosts["items"].append(nextHost)
    return hosts

  def prepareServices(self, servicesInfo):
    services = { "Versions" : { "stack_name" : "HDP", "stack_version" : "2.0.6" } }
    services["services"] = []

    for serviceInfo in servicesInfo:
      nextService = {"StackServices":{"service_name" : serviceInfo["name"]}}
      nextService["components"] = []
      for component in serviceInfo["components"]:
        nextComponent = {
          "StackServiceComponents": {
            "component_name": component["name"],
            "cardinality": component["cardinality"],
            "component_category": component["category"],
            "is_master": component["is_master"]
          }
        }
        try:
          nextComponent["StackServiceComponents"]["hostnames"] = component["hostnames"]
        except KeyError, err:
          nextComponent["StackServiceComponents"]["hostnames"] = []
        nextService["components"].append(nextComponent)
      services["services"].append(nextService)

    return services

  def assertHostLayout(self, componentsHostsMap, recommendation):
    blueprintMapping = recommendation["recommendations"]["blueprint"]["host_groups"]
    bindings = recommendation["recommendations"]["blueprint_cluster_binding"]["host_groups"]

    actualComponentHostsMap = {}
    for hostGroup in blueprintMapping:
      hostGroupName = hostGroup["name"]
      hostsInfos = [binding["hosts"] for binding in bindings if binding["name"] == hostGroupName][0]
      hosts = [info["fqdn"] for info in hostsInfos]

      for component in hostGroup["components"]:
        componentName = component["name"]
        try:
          actualComponentHostsMap[componentName]
        except KeyError, err:
          actualComponentHostsMap[componentName] = []
        for host in hosts:
          if host not in actualComponentHostsMap[componentName]:
            actualComponentHostsMap[componentName].append(host)

    for componentName in componentsHostsMap.keys():
      expectedHosts = componentsHostsMap[componentName]
      actualHosts = actualComponentHostsMap[componentName]
      self.checkEqual(expectedHosts, actualHosts)

  def checkEqual(self, l1, l2):
    if not len(l1) == len(l2) or not sorted(l1) == sorted(l2):
      raise AssertionError("list1={0}, list2={1}".format(l1, l2))

  def assertValidationMessages(self, expectedMessages, result):
    realMessages = [item["message"] for item in result["items"]]
    self.checkEqual(expectedMessages, realMessages)


