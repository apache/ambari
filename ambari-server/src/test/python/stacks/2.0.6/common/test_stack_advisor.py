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
    self.maxDiff = None
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
      {"message": "Value is less than the recommended default of 512", "level": "WARN"},
      {"message": "Value should be integer", "level": "ERROR"},
      {"message": "Value should be set", "level": "ERROR"}
    ]
    self.assertValidationResult(expectedItems, result)

  def test_validationMinMax(self):

    configurations = {
      "mapred-site": {
        "properties": {
          "mapreduce.task.io.sort.mb": "4096",
          "some_float_value": "0.5",
          "no_min_or_max_attribute_property": "STRING_VALUE"
        }
      }
    }
    recommendedDefaults = {
      "mapred-site": {
        "properties": {
          "mapreduce.task.io.sort.mb": "2047",
          "some_float_value": "0.8",
          "no_min_or_max_attribute_property": "STRING_VALUE"
        },
        "property_attributes": {
          'mapreduce.task.io.sort.mb': {'maximum': '2047'},
          'some_float_value': {'minimum': '0.8'}
        }
      }
    }
    items = []
    self.stackAdvisor.validateMinMax(items, recommendedDefaults, configurations)

    expectedItems = [
      {
        'message': 'Value is greater than the recommended maximum of 2047 ',
        'level': 'WARN',
        'config-type':  'mapred-site',
        'config-name': 'mapreduce.task.io.sort.mb',
        'type': 'configuration'
      },
      {
        'message': 'Value is less than the recommended minimum of 0.8 ',
        'level': 'WARN',
        'config-type':  'mapred-site',
        'config-name': 'some_float_value',
        'type': 'configuration'
      }
    ]
    self.assertEquals(expectedItems, items)

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
      "amMemory": 512,
      "referenceHost": hosts["items"][0]["Hosts"]
    }

    # Test - Cluster data with 1 host
    result = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, None)
    self.assertEquals(result, expected)

    # Test - Cluster data with 2 hosts - pick minimum memory
    servicesList.append("YARN")
    services = services = {"services":
                  [{"StackServices":
                      {"service_name" : "YARN",
                       "service_version" : "2.6.0.2.2"
                      },
                    "components":[
                      {
                        "StackServiceComponents":{
                          "advertise_version":"true",
                          "cardinality":"1+",
                          "component_category":"SLAVE",
                          "component_name":"NODEMANAGER",
                          "custom_commands":[

                          ],
                          "display_name":"NodeManager",
                          "is_client":"false",
                          "is_master":"false",
                          "service_name":"YARN",
                          "stack_name":"HDP",
                          "stack_version":"2.2",
                          "hostnames":[
                            "host1",
                            "host2"
                          ]
                        },
                        "dependencies":[
                        ]
                      }
                      ],
                    }],
                "configurations": {}
    }
    hosts["items"][0]["Hosts"]["host_name"] = "host1"
    hosts["items"].append({
        "Hosts": {
            "cpu_count" : 4,
            "total_mem" : 500000,
            "host_name" : "host2",
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
        })
    expected["referenceHost"] = hosts["items"][1]["Hosts"]
    expected["referenceNodeManagerHost"] = hosts["items"][1]["Hosts"]
    expected["amMemory"] = 170.66666666666666
    expected["containers"] = 3.0
    expected["cpu"] = 4
    expected["totalAvailableRam"] = 512
    expected["mapMemory"] = 170
    expected["minContainerSize"] = 256
    expected["reduceMemory"] = 170.66666666666666
    expected["ram"] = 0
    expected["ramPerContainer"] = 170.66666666666666
    expected["reservedRam"] = 1
    result = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, services)
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
      "amMemory": 3072,
      "referenceHost": hosts["items"][0]["Hosts"]
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
      "totalAvailableRam": 512,
      "containers": 3,
      "ramPerContainer": 170.66666666666666,
      "mapMemory": 170,
      "reduceMemory": 170.66666666666666,
      "amMemory": 170.66666666666666
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

  def test_getHostsWithComponent(self):
    services = {"services":
                  [{"StackServices":
                      {"service_name" : "HDFS",
                       "service_version" : "2.6.0.2.2"
                      },
                    "components":[
                      {
                        "href":"/api/v1/stacks/HDP/versions/2.2/services/HDFS/components/DATANODE",
                        "StackServiceComponents":{
                          "advertise_version":"true",
                          "cardinality":"1+",
                          "component_category":"SLAVE",
                          "component_name":"DATANODE",
                          "custom_commands":[

                          ],
                          "display_name":"DataNode",
                          "is_client":"false",
                          "is_master":"false",
                          "service_name":"HDFS",
                          "stack_name":"HDP",
                          "stack_version":"2.2",
                          "hostnames":[
                            "host1",
                            "host2"
                          ]
                        },
                        "dependencies":[

                        ]
                      },
                      {
                        "href":"/api/v1/stacks/HDP/versions/2.2/services/HDFS/components/JOURNALNODE",
                        "StackServiceComponents":{
                          "advertise_version":"true",
                          "cardinality":"0+",
                          "component_category":"SLAVE",
                          "component_name":"JOURNALNODE",
                          "custom_commands":[

                          ],
                          "display_name":"JournalNode",
                          "is_client":"false",
                          "is_master":"false",
                          "service_name":"HDFS",
                          "stack_name":"HDP",
                          "stack_version":"2.2",
                          "hostnames":[
                            "host1"
                          ]
                        },
                        "dependencies":[
                          {
                            "href":"/api/v1/stacks/HDP/versions/2.2/services/HDFS/components/JOURNALNODE/dependencies/HDFS_CLIENT",
                            "Dependencies":{
                              "component_name":"HDFS_CLIENT",
                              "dependent_component_name":"JOURNALNODE",
                              "dependent_service_name":"HDFS",
                              "stack_name":"HDP",
                              "stack_version":"2.2"
                            }
                          }
                        ]
                      },
                      {
                        "href":"/api/v1/stacks/HDP/versions/2.2/services/HDFS/components/NAMENODE",
                        "StackServiceComponents":{
                          "advertise_version":"true",
                          "cardinality":"1-2",
                          "component_category":"MASTER",
                          "component_name":"NAMENODE",
                          "custom_commands":[
                            "DECOMMISSION",
                            "REBALANCEHDFS"
                          ],
                          "display_name":"NameNode",
                          "is_client":"false",
                          "is_master":"true",
                          "service_name":"HDFS",
                          "stack_name":"HDP",
                          "stack_version":"2.2",
                          "hostnames":[
                            "host2"
                          ]
                        },
                        "dependencies":[

                        ]
                      },
                      ],
                    }],
                "configurations": {}
    }
    hosts = {
      "items" : [
        {
          "href" : "/api/v1/hosts/host1",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "host1",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "host1",
            "rack_info" : "/default-rack",
            "total_mem" : 2097152
          }
        },
        {
          "href" : "/api/v1/hosts/host2",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "host2",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "host2",
            "rack_info" : "/default-rack",
            "total_mem" : 1048576
          }
        },
        ]
    }

    datanodes = self.stackAdvisor.getHostsWithComponent("HDFS", "DATANODE", services, hosts)
    self.assertEquals(len(datanodes), 2)
    self.assertEquals(datanodes, hosts["items"])
    datanode = self.stackAdvisor.getHostWithComponent("HDFS", "DATANODE", services, hosts)
    self.assertEquals(datanode, hosts["items"][0])
    namenodes = self.stackAdvisor.getHostsWithComponent("HDFS", "NAMENODE", services, hosts)
    self.assertEquals(len(namenodes), 1)
    # [host2]
    self.assertEquals(namenodes, [hosts["items"][1]])
    namenode = self.stackAdvisor.getHostWithComponent("HDFS", "NAMENODE", services, hosts)
    # host2
    self.assertEquals(namenode, hosts["items"][1])

    # not installed
    nodemanager = self.stackAdvisor.getHostWithComponent("YARN", "NODEMANAGER", services, hosts)
    self.assertEquals(nodemanager, None)

    # unknown component
    unknown_component = self.stackAdvisor.getHostWithComponent("YARN", "UNKNOWN", services, hosts)
    self.assertEquals(nodemanager, None)
    # unknown service
    unknown_component = self.stackAdvisor.getHostWithComponent("UNKNOWN", "NODEMANAGER", services, hosts)
    self.assertEquals(nodemanager, None)

  def test_mergeValidators(self):
    childValidators = {
      "HDFS": {"hdfs-site": "validateHDFSConfigurations2.3"},
      "HIVE": {"hiveserver2-site": "validateHiveServer2Configurations2.3"},
      "HBASE": {"hbase-site": "validateHBASEConfigurations2.3",
                "newconf": "new2.3"},
      "NEWSERVICE" : {"newserviceconf": "abc2.3"}
    }
    parentValidators = {
      "HDFS": {"hdfs-site": "validateHDFSConfigurations2.2",
               "hadoop-env": "validateHDFSConfigurationsEnv2.2"},
      "YARN": {"yarn-env": "validateYARNEnvConfigurations2.2"},
      "HIVE": {"hiveserver2-site": "validateHiveServer2Configurations2.2",
               "hive-site": "validateHiveConfigurations2.2",
               "hive-env": "validateHiveConfigurationsEnv2.2"},
      "HBASE": {"hbase-site": "validateHBASEConfigurations2.2",
                "hbase-env": "validateHBASEEnvConfigurations2.2"},
      "MAPREDUCE2": {"mapred-site": "validateMapReduce2Configurations2.2"},
      "TEZ": {"tez-site": "validateTezConfigurations2.2"}
    }
    expected = {
      "HDFS": {"hdfs-site": "validateHDFSConfigurations2.3",
               "hadoop-env": "validateHDFSConfigurationsEnv2.2"},
      "YARN": {"yarn-env": "validateYARNEnvConfigurations2.2"},
      "HIVE": {"hiveserver2-site": "validateHiveServer2Configurations2.3",
               "hive-site": "validateHiveConfigurations2.2",
               "hive-env": "validateHiveConfigurationsEnv2.2"},
      "HBASE": {"hbase-site": "validateHBASEConfigurations2.3",
                "hbase-env": "validateHBASEEnvConfigurations2.2",
                "newconf": "new2.3"},
      "MAPREDUCE2": {"mapred-site": "validateMapReduce2Configurations2.2"},
      "TEZ": {"tez-site": "validateTezConfigurations2.2"},
      "NEWSERVICE" : {"newserviceconf": "abc2.3"}
    }

    self.stackAdvisor.mergeValidators(parentValidators, childValidators)
    self.assertEquals(expected, parentValidators)
