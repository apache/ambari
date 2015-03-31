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
from mock.mock import patch, MagicMock

class TestHDP206StackAdvisor(TestCase):

  def setUp(self):
    import imp
    import os

    testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    hdp206StackAdvisorPath = os.path.join(testDirectory, '../../../../../main/resources/stacks/HDP/2.0.6/services/stack_advisor.py')
    hdp206StackAdvisorClassName = 'HDP206StackAdvisor'
    with open(stackAdvisorPath, 'rb') as fp:
      stack_advisor = imp.load_module( 'stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE) )
    with open(hdp206StackAdvisorPath, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp206StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    clazz = getattr(stack_advisor_impl, hdp206StackAdvisorClassName)
    self.stackAdvisor = clazz()

    # substitute method in the instance
    self.get_system_min_uid_real = self.stackAdvisor.get_system_min_uid
    self.stackAdvisor.get_system_min_uid = self.get_system_min_uid_magic

  @patch('__builtin__.open')
  @patch('os.path.exists')
  def get_system_min_uid_magic(self, exists_mock, open_mock):
    class MagicFile(object):
      def read(self):
        return """
        #test line UID_MIN 200
        UID_MIN 500
        """

      def __exit__(self, exc_type, exc_val, exc_tb):
        pass

      def __enter__(self):
        return self

    exists_mock.return_value = True
    open_mock.return_value = MagicFile()
    return self.get_system_min_uid_real()



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
      "GANGLIA_MONITOR": ["host1", "host2"]
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

  def test_validationNamenodeAndSecondaryNamenode2Hosts_noMessagesForSameHost(self):
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

    expectedItems = [
      {"message": "Host is not used", "level": "ERROR", "host": "host2"}
    ]
    self.assertValidationResult(expectedItems, result)

  def test_validationCardinalityALL(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [
          {"name": "GANGLIA_MONITOR", "display_name": "Ganglia Monitor", "cardinality": "ALL", "category": "SLAVE", "is_master": False, "hostnames": ["host1"]},
          {"name": "GANGLIA_SERVER", "display_name": "Ganglia Server", "cardinality": "1-2", "category": "MASTER", "is_master": True, "hostnames": ["host2", "host1"]}
        ]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.validateComponentLayout(services, hosts)

    expectedItems = [
      {"message": "Ganglia Monitor component should be installed on all hosts in cluster.", "level": "ERROR"}
    ]
    self.assertValidationResult(expectedItems, result)

  def test_validationCardinalityExactAmount(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [
          {"name": "GANGLIA_MONITOR", "display_name": "Ganglia Monitor", "cardinality": "2", "category": "SLAVE", "is_master": False, "hostnames": ["host1"]},
          {"name": "GANGLIA_SERVER", "display_name": "Ganglia Server", "cardinality": "2", "category": "MASTER", "is_master": True, "hostnames": ["host2", "host1"]}
        ]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.validateComponentLayout(services, hosts)

    expectedItems = [
      {"message": "Exactly 2 Ganglia Monitor components should be installed in cluster.", "level": "ERROR"}
    ]
    self.assertValidationResult(expectedItems, result)

  def test_validationCardinalityAtLeast(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [
          {"name": "GANGLIA_MONITOR", "display_name": "Ganglia Monitor", "cardinality": "1+", "category": "SLAVE", "is_master": False, "hostnames": ["host1"]},
          {"name": "GANGLIA_SERVER", "display_name": "Ganglia Server", "cardinality": "3+", "category": "MASTER", "is_master": True, "hostnames": ["host2", "host1"]}
        ]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.validateComponentLayout(services, hosts)

    expectedItems = [
      {"message": "At least 3 Ganglia Server components should be installed in cluster.", "level": "ERROR"}
    ]
    self.assertValidationResult(expectedItems, result)

  def test_validationWarnMessagesIfLessThanDefault(self):
    servicesInfo = [
      {
        "name": "YARN",
        "components": []
      }
    ]
    services = self.prepareServices(servicesInfo)
    services["configurations"] = {"yarn-site":{"properties":{"yarn.nodemanager.resource.memory-mb": "0",
                                                             "yarn.scheduler.minimum-allocation-mb": "str"}}}
    hosts = self.prepareHosts([])
    result = self.stackAdvisor.validateConfigurations(services, hosts)

    expectedItems = [
      {"message": "Value is less than the recommended default of 2048", "level": "WARN"},
      {"message": "Value should be integer", "level": "ERROR"},
      {"message": "Value should be set", "level": "ERROR"}
    ]
    self.assertValidationResult(expectedItems, result)

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

    expectedItems = [
      {"message": "Host is not used", "host": "host1", "level": "ERROR"}
    ]
    self.assertValidationResult(expectedItems, result)

  def test_validationCardinality01TwoHostsAssigned(self):
    servicesInfo = [
      {
        "name": "GANGLIA",
        "components": [
          {"name": "GANGLIA_SERVER", "display_name": "Ganglia Server", "cardinality": "0-1", "category": "MASTER", "is_master": True, "hostnames": ["host1", "host2"]}
        ]
      }
    ]
    services = self.prepareServices(servicesInfo)
    hosts = self.prepareHosts(["host1", "host2"])
    result = self.stackAdvisor.validateComponentLayout(services, hosts)

    expectedItems = [
      {"message": "Between 0 and 1 Ganglia Server components should be installed in cluster.", "level": "ERROR"}
    ]
    self.assertValidationResult(expectedItems, result)

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

    expectedItems = [
      {"message": "Host is not used", "host": "host2", "level": "ERROR"}
    ]
    self.assertValidationResult(expectedItems, result)

  def test_getConfigurationClusterSummary_withHBaseAnd6gbRam(self):
    servicesList = ["HBASE"]
    components = []
    hosts = {
      "items" : [
        {
          "Hosts" : {
            "cpu_count" : 8,
            "total_mem" : 6291456,
            "disk_info" : [
              {"mountpoint" : "/"},
              {"mountpoint" : "/dev/shm"},
              {"mountpoint" : "/vagrant"},
              {"mountpoint" : "/"},
              {"mountpoint" : "/dev/shm"},
              {"mountpoint" : "/"},
              {"mountpoint" : "/dev/shm"},
              {"mountpoint" : "/vagrant"}
            ]
          }
        }
      ]
    }
    expected = {
      "hBaseInstalled": True,
      "components": components,
      "cpu": 8,
      "disk": 8,
      "ram": 6,
      "reservedRam": 2,
      "hbaseRam": 1,
      "minContainerSize": 512,
      "totalAvailableRam": 3072,
      "containers": 6,
      "ramPerContainer": 512,
      "mapMemory": 512,
      "reduceMemory": 512,
      "amMemory": 512
    }

    result = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, None)

    self.assertEquals(result, expected)

  def test_getConfigurationClusterSummary_withHBaseAnd48gbRam(self):
    servicesList = ["HBASE"]
    components = []
    hosts = {
      "items" : [
        {
          "Hosts" : {
            "cpu_count" : 6,
            "total_mem" : 50331648,
            "disk_info" : [
              {"mountpoint" : "/"},
              {"mountpoint" : "/dev/shm"},
              {"mountpoint" : "/vagrant"},
              {"mountpoint" : "/"},
              {"mountpoint" : "/dev/shm"},
              {"mountpoint" : "/vagrant"}
            ]
          }
        }
      ]
    }
    expected = {
      "hBaseInstalled": True,
      "components": components,
      "cpu": 6,
      "disk": 6,
      "ram": 48,
      "reservedRam": 6,
      "hbaseRam": 8,
      "minContainerSize": 2048,
      "totalAvailableRam": 34816,
      "containers": 11,
      "ramPerContainer": 3072,
      "mapMemory": 3072,
      "reduceMemory": 3072,
      "amMemory": 3072
    }

    result = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, None)

    self.assertEquals(result, expected)

  def test_recommendYARNConfigurations(self):
    configurations = {}
    clusterData = {
      "containers" : 5,
      "ramPerContainer": 256
    }
    expected = {
      "yarn-env": {
        "properties": {
          "min_user_id": "500"
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.resource.memory-mb": "1280",
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "1280"
        }
      }
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, None, None)
    self.assertEquals(configurations, expected)

  def test_recommendMapReduce2Configurations_mapMemoryLessThan2560(self):
    configurations = {}
    clusterData = {
      "mapMemory": 567,
      "reduceMemory": 345.6666666666666,
      "amMemory": 123.54
    }
    expected = {
      "mapred-site": {
        "properties": {
          "yarn.app.mapreduce.am.resource.mb": "123",
          "yarn.app.mapreduce.am.command-opts": "-Xmx99m",
          "mapreduce.map.memory.mb": "567",
          "mapreduce.reduce.memory.mb": "345",
          "mapreduce.map.java.opts": "-Xmx454m",
          "mapreduce.reduce.java.opts": "-Xmx277m",
          "mapreduce.task.io.sort.mb": "227"
        }
      }
    }

    self.stackAdvisor.recommendMapReduce2Configurations(configurations, clusterData, None, None)
    self.assertEquals(configurations, expected)

  def test_getConfigurationClusterSummary_noHostsWithoutHBase(self):
    servicesList = []
    components = []
    hosts = {
      "items" : []
    }
    result = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, None)

    expected = {
      "hBaseInstalled": False,
      "components": components,
      "cpu": 0,
      "disk": 0,
      "ram": 0,
      "reservedRam": 1,
      "hbaseRam": 1,
      "minContainerSize": 256,
      "totalAvailableRam": 2048,
      "containers": 3,
      "ramPerContainer": 682.6666666666666,
      "mapMemory": 682,
      "reduceMemory": 682.6666666666666,
      "amMemory": 682.6666666666666
    }

    self.assertEquals(result, expected)

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
        except KeyError:
          nextComponent["StackServiceComponents"]["hostnames"] = []
        try:
          nextComponent["StackServiceComponents"]["display_name"] = component["display_name"]
        except KeyError:
          nextComponent["StackServiceComponents"]["display_name"] = component["name"]
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

  def assertValidationResult(self, expectedItems, result):
    actualItems = []
    for item in result["items"]:
      next = {"message": item["message"], "level": item["level"]}
      try:
        next["host"] = item["host"]
      except KeyError, err:
        pass
      actualItems.append(next)
    self.checkEqual(expectedItems, actualItems)

  def test_recommendHbaseEnvConfigurations(self):
    servicesList = ["HBASE"]
    configurations = {}
    components = []
    hosts = {
      "items" : [
        {
          "Hosts" : {
            "cpu_count" : 6,
            "total_mem" : 50331648,
            "disk_info" : [
              {"mountpoint" : "/"},
              {"mountpoint" : "/dev/shm"},
              {"mountpoint" : "/vagrant"},
              {"mountpoint" : "/"},
              {"mountpoint" : "/dev/shm"},
              {"mountpoint" : "/vagrant"}
            ]
          }
        }
      ]
    }
    expected = {
      "hbase-env": {
        "properties": {
          "hbase_master_heapsize": "8192",
          "hbase_regionserver_heapsize": "8192",
          }
      }
    }

    clusterData = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, None)
    self.assertEquals(clusterData['hbaseRam'], 8)

    self.stackAdvisor.recommendHbaseEnvConfigurations(configurations, clusterData, None, None)
    self.assertEquals(configurations, expected)

  def test_recommendHDFSConfigurations(self):
    configurations = {}
    clusterData = {
      "totalAvailableRam": 2048
    }
    expected = {
      'hadoop-env': {
        'properties': {
          'namenode_heapsize': '1024',
          'namenode_opt_newsize' : '256',
          'namenode_opt_maxnewsize' : '256'
        }
      }
    }

    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, '', '')
    self.assertEquals(configurations, expected)

  def test_validateHDFSConfigurationsEnv(self):
    configurations = {}

    # 1) ok: namenode_heapsize > recommended
    recommendedDefaults = {'namenode_heapsize': '1024',
                           'namenode_opt_newsize' : '256',
                           'namenode_opt_maxnewsize' : '256'}
    properties = {'namenode_heapsize': '2048',
                  'namenode_opt_newsize' : '300',
                  'namenode_opt_maxnewsize' : '300'}
    res_expected = []

    res = self.stackAdvisor.validateHDFSConfigurationsEnv(properties, recommendedDefaults, configurations, '', '')
    self.assertEquals(res, res_expected)

    # 2) fail: namenode_heapsize, namenode_opt_maxnewsize < recommended
    properties['namenode_heapsize'] = '1022'
    properties['namenode_opt_maxnewsize'] = '255'
    res_expected = [{'config-type': 'hadoop-env',
                     'message': 'Value is less than the recommended default of 1024',
                     'type': 'configuration',
                     'config-name': 'namenode_heapsize',
                     'level': 'WARN'},
                    {'config-name': 'namenode_opt_maxnewsize',
                     'config-type': 'hadoop-env',
                     'level': 'WARN',
                     'message': 'Value is less than the recommended default of 256',
                     'type': 'configuration'}]

    res = self.stackAdvisor.validateHDFSConfigurationsEnv(properties, recommendedDefaults, configurations, '', '')
    self.assertEquals(res, res_expected)
