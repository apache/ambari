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

import os, imp, json
from unittest import TestCase
from mock.mock import patch, MagicMock


class TestHAWQ200ServiceAdvisor(TestCase):

  testDirectory = os.path.dirname(os.path.abspath(__file__))
  serviceAdvisorPath = '../../../../main/resources/common-services/HAWQ/2.0.0/service_advisor.py'
  hawq200ServiceAdvisorPath = os.path.join(testDirectory, serviceAdvisorPath)
  with open(hawq200ServiceAdvisorPath, 'rb') as fp:
    service_advisor_impl = imp.load_module('service_advisor_impl', fp, hawq200ServiceAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))

  def setUp(self):
    serviceAdvisorClass = getattr(self.service_advisor_impl, 'HAWQ200ServiceAdvisor')
    self.serviceAdvisor = serviceAdvisorClass()

  def fqdn_mock_result(value=None):
      return 'c6401.ambari.apache.org' if value is None else value

  def load_json(self, filename):
    file = os.path.join(self.testDirectory, "../configs", filename)
    with open(file, 'rb') as f:
      data = json.load(f)
    return data

  def prepareHosts(self, hostsNames):
    hosts = { "items": [] }
    for hostName in hostsNames:
      nextHost = {"Hosts":{"host_name" : hostName}}
      hosts["items"].append(nextHost)
    return hosts

  def getHosts(self, componentsList, componentName):
    hosts = [component["StackServiceComponents"] for component in componentsList
             if component["StackServiceComponents"]["component_name"] == componentName]
    return hosts[0] if len(hosts) > 0 else []

  def getHostsFromRecommendations(self, recommendations, componentName):
    hostGroups = [hostgroup["name"] for hostgroup in recommendations["blueprint"]["host_groups"] if
                  {"name": componentName} in hostgroup["components"]]
    return set([host["fqdn"] for hostgroup in recommendations["blueprint_cluster_binding"]["host_groups"] if
                hostgroup["name"] in hostGroups for host in hostgroup["hosts"]])

  def getComponentsListFromServices(self, services):
    componentsListList = [service["components"] for service in services["services"]]
    return [item for sublist in componentsListList for item in sublist]

  def getComponentsFromServices(self, services):
    componentsList = self.getComponentsListFromServices(services)
    return [component["StackServiceComponents"]["component_name"] for component in componentsList]

  def getComponentsFromRecommendations(self, recommendations):
    componentsListList = [hostgroup["components"] for hostgroup in
                          recommendations["recommendations"]["blueprint"]["host_groups"]]
    return [item["name"] for sublist in componentsListList for item in sublist]

  def insertHAWQServiceAdvisorInfo(self, services):
    for service in services["services"]:
      if service["StackServices"]["service_name"] == 'HAWQ':
        service["StackServices"]["advisor_name"] = "HAWQ200ServiceAdvisor"
        service["StackServices"]["advisor_path"] = self.hawq200ServiceAdvisorPath

  @patch("socket.getfqdn")
  def test_getHostsForMasterComponent(self, getfqdn_mock):
    getfqdn_mock.return_value = "c6401.ambari.apache.org"

    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "HAWQ"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "HAWQMASTER",
                "hostnames": [
                  "c6403.ambari.apache.org"
                ]
              }
            },
            {
              "StackServiceComponents": {
                "component_name": "HAWQSTANDBY",
                "hostnames": [
                ]
              }
            }
          ]
        }
      ]
    }

    hostsList = ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org", "c6404.ambari.apache.org"]

    component = {
      "StackServiceComponents": {
        "component_name": "HAWQSTANDBY"
      }
    }

    # Case 1:
    # Ambari Server is placed on c6401.ambari.apache.org
    # HAWQMASTER is placed on c6403.ambari.apache.org
    # There are 4 available hosts in the cluster
    # Recommend HAWQSTANDBY on next available host, c6402.ambari.apache.org
    standbyHosts = self.serviceAdvisor.getHostsForMasterComponent(services, None, component, hostsList)
    self.assertEquals(standbyHosts, ["c6402.ambari.apache.org"])

    # Case 2:
    # Ambari Server is placed on c6401.ambari.apache.org
    # HAWQMASTER is placed on c6402.ambari.apache.org
    # There are 4 available hosts in the cluster
    # Recommend HAWQSTANDBY on next available host, c6403.ambari.apache.org
    services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"] = ["c6402.ambari.apache.org"]
    standbyHosts = self.serviceAdvisor.getHostsForMasterComponent(services, None, component, hostsList)
    self.assertEquals(standbyHosts, ["c6403.ambari.apache.org"])

    # Case 3:
    # Ambari Server is placed on c6401.ambari.apache.org
    # HAWQMASTER is placed on c6402.ambari.apache.org
    # There are 2 available hosts in the cluster
    # Recommend HAWQSTANDBY on a host which does not have HAWQMASTER, c6401.ambari.apache.org
    hostsList = ["c6401.ambari.apache.org", "c6402.ambari.apache.org"]
    standbyHosts = self.serviceAdvisor.getHostsForMasterComponent(services, None, component, hostsList)
    self.assertEquals(standbyHosts, ["c6401.ambari.apache.org"])

    # Case 4:
    # Ambari Server is placed on c6401.ambari.apache.org
    # HAWQMASTER is placed on c6401.ambari.apache.org
    # There is 1 available host in the cluster
    # Do not recommend HAWQSTANDBY on a single node cluster
    hostsList = ["c6401.ambari.apache.org"]
    services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"] = ["c6401.ambari.apache.org"]
    standbyHosts = self.serviceAdvisor.getHostsForMasterComponent(services, None, component, hostsList)
    self.assertEquals(standbyHosts, [])

    # Case 5:
    # Ambari Server is placed on c6401.ambari.apache.org
    # HAWQMASTER is placed on c6402.ambari.apache.org
    # HAWQSTANDBY is placed on c6401.ambari.apache.org
    # There are 3 available host in the cluster
    # Do not change HAWQSTANDBY host according to recommendation since HAWQSTANDBY has already been assigned a host
    hostsList = ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"]
    services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"] = ["c6402.ambari.apache.org"]
    services["services"][0]["components"][1]["StackServiceComponents"]["hostnames"] = ["c6401.ambari.apache.org"]
    standbyHosts = self.serviceAdvisor.getHostsForMasterComponent(services, None, component, hostsList)
    self.assertEquals(standbyHosts, ["c6401.ambari.apache.org"])


  def test_getServiceConfigurationRecommendations(self):

    configurations = {
      "hawq-sysctl-env": {
        "properties": {
          "vm.overcommit_memory": 1,
          "vm.overcommit_ratio": 50
        }
      },
      "hawq-site": {
        "properties": {
          "hawq_rm_memory_limit_perseg": "65535MB",
          "hawq_rm_nvcore_limit_perseg": "16",
          "hawq_global_rm_type": "yarn"
        }
      }
    }

    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "HAWQ",
            "service_version": "2.0",
            "stack_name": "HDP",
            "stack_version": "2.3"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "HAWQMASTER",
                "hostnames": [
                  "c6401.ambari.apache.org"
                ]
              }
            },
            {
              "StackServiceComponents": {
                "component_name": "HAWQSEGMENT",
                "hostnames": [
                  "c6402.ambari.apache.org",
                  "c6404.ambari.apache.org",
                ]
              }
            }
          ]
        }
      ],
      "configurations": configurations
    }

    hosts = {
      "items": [
        {
          "Hosts": {
            "host_name": "c6401.ambari.apache.org",
            "cpu_count" : 2,
            "total_mem": 33554432
          }
        },
        {
          "Hosts": {
            "host_name": "c6402.ambari.apache.org",
            "cpu_count" : 4,
            "total_mem": 33554433
          }
        },
        {
          "Hosts": {
            "host_name": "c6403.ambari.apache.org",
            "cpu_count" : 1,
            "total_mem": 33554434
          }
        },
        {
          "Hosts": {
            "host_name": "c6404.ambari.apache.org",
            "cpu_count" : 2,
            "total_mem": 33554435
          }
        }
      ]
    }

    ## Test if hawq_rm_nvcore_limit_perseg is set correctly

    # Case 1:
    # HAWQ Hosts Core Count: c6401.ambari.apache.org - 2, c6402.ambari.apache.org - 4, c6404.ambari.apache.org - 2
    # hawq_global_rm_type: yarn
    # Non HAWQ Hosts Core Count: c6401.ambari.apache.org - 1
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-site"]["properties"]["hawq_rm_nvcore_limit_perseg"], "2")

    ## Test if vm.overcommit_memory is set correctly

    # Case 1: All machines have total_mem above 32GB (total_mem >= 33554432)
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-sysctl-env"]["properties"]["vm.overcommit_memory"], "2")

    # Case 2: One machine has total_mem below 32GB
    hosts["items"][0]["Hosts"]["total_mem"] = 33554431
    services["configurations"]["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"] = "65535MB"
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-sysctl-env"]["properties"]["vm.overcommit_memory"], "1")

    ## Test if hawq_rm_memory_limit_perseg is set correctly

    # Case 1: Minimum host memory is ~ 2 GB (2048MB), recommended val must be .75% of 2GB as vm.overcommit_memory = 1 and in MB
    hosts["items"][0]["Hosts"]["total_mem"] = 2097152
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "1536MB")

    # Case 2: Minimum host memory is ~ 16 GB, recommended val must be .75% of 16GB as vm.overcommit_memory = 1 and in GB
    hosts["items"][0]["Hosts"]["total_mem"] = 16777216
    hosts["items"][1]["Hosts"]["total_mem"] = 26777216
    hosts["items"][3]["Hosts"]["total_mem"] = 36777216
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "12GB")

    # Case 2: Minimum host memory is ~ 64 GB, recommended val must be .75% of 32GB as vm.overcommit_memory = 2 and in GB
    hosts["items"][0]["Hosts"]["total_mem"] = 67108864
    hosts["items"][1]["Hosts"]["total_mem"] = 77108864
    hosts["items"][3]["Hosts"]["total_mem"] = 87108864
    services["configurations"]["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"] = "65535MB"
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "24GB")

    # Case 4: Minimum host memory is ~ 512 GB, recommended val must be .85% of 256GB as vm.overcommit_memory = 2 and in GB
    hosts["items"][0]["Hosts"]["total_mem"] = 536870912
    hosts["items"][1]["Hosts"]["total_mem"] = 636870912
    hosts["items"][3]["Hosts"]["total_mem"] = 736870912
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "218GB")

    # Case 5: Minimum host memory is ~ 1024 GB, recommended val must be .95% of 512GB as vm.overcommit_memory = 2 and in GB
    hosts["items"][0]["Hosts"]["total_mem"] = 1073741824
    hosts["items"][1]["Hosts"]["total_mem"] = 2073741824
    hosts["items"][3]["Hosts"]["total_mem"] = 3073741824
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "436GB")

    # Case 6: Minimum host memory is ~ 1024 GB, vm.overcommit_ratio = 75, vm.overcommit_memory = 2
    # recommended val must be .95% of (1024*75)/100 and in GB
    hosts["items"][0]["Hosts"]["total_mem"] = 1073741824
    hosts["items"][1]["Hosts"]["total_mem"] = 2073741824
    hosts["items"][3]["Hosts"]["total_mem"] = 3073741824
    services["configurations"]["hawq-sysctl-env"]["properties"]["vm.overcommit_ratio"] = 75
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "730GB")

    ## Test if the properties are set to visible / invisible based on the value of hawq_global_rm_type

    # Case 1: When hawq_global_rm_type is yarn
    services["configurations"]["hawq-site"]["properties"]["hawq_global_rm_type"] = "yarn"
    properties_visible_status = {"hawq_rm_memory_limit_perseg": "false",
                  "hawq_rm_nvcore_limit_perseg": "false",
                  "hawq_rm_yarn_app_name":"true",
                  "hawq_rm_yarn_queue_name": "true",
                  "hawq_rm_yarn_scheduler_address": "true",
                  "hawq_rm_yarn_address": "true"}
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    for property, status in properties_visible_status.iteritems():
      self.assertEqual(configurations["hawq-site"]["property_attributes"][property]["visible"], status)

    # Case 2: When hawq_global_rm_type is none
    services["configurations"]["hawq-site"]["properties"]["hawq_global_rm_type"] = "none"
    properties_visible_status = {"hawq_rm_memory_limit_perseg": "true",
                                 "hawq_rm_nvcore_limit_perseg": "true",
                                 "hawq_rm_yarn_app_name": "false",
                                 "hawq_rm_yarn_queue_name": "false",
                                 "hawq_rm_yarn_scheduler_address": "false",
                                 "hawq_rm_yarn_address": "false"}
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    for property, status in properties_visible_status.iteritems():
      self.assertEqual(configurations["hawq-site"]["property_attributes"][property]["visible"], status)

    ## Test if vm.overcommit_ratio is set to visible / invisible based on the value of vm.overcommit_memory

    # Case 1: vm.overcommit_ratio should be invisible when overcommit_memory is set as 0
    services["configurations"]["hawq-sysctl-env"]["properties"]["vm.overcommit_memory"] = 0
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-sysctl-env"]["property_attributes"]["vm.overcommit_ratio"]["visible"], "false")

    # Case 2: vm.overcommit_ratio should be invisible when overcommit_memory is set as 1
    services["configurations"]["hawq-sysctl-env"]["properties"]["vm.overcommit_memory"] = 1
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-sysctl-env"]["property_attributes"]["vm.overcommit_ratio"]["visible"], "false")

    # Case 3: vm.overcommit_ratio should be visible when overcommit_memory is set as 2
    services["configurations"]["hawq-sysctl-env"]["properties"]["vm.overcommit_memory"] = 2
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-sysctl-env"]["property_attributes"]["vm.overcommit_ratio"]["visible"], "true")

  def test_createComponentLayoutRecommendations_hawq_3_Hosts(self):
    """ Test that HAWQSTANDBY is recommended on a 3-node cluster """

    services = self.load_json("services-hawq-3-hosts.json")
    componentNames = self.getComponentsFromServices(services)
    self.assertTrue('HAWQSTANDBY' in componentNames)

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    self.insertHAWQServiceAdvisorInfo(services)
    recommendations = self.serviceAdvisor.recommendComponentLayout(services, hosts)
    recommendedComponents = self.getComponentsFromRecommendations(recommendations)
    self.assertTrue('HAWQMASTER' in recommendedComponents)
    self.assertTrue('HAWQSTANDBY' in recommendedComponents)
    self.assertTrue('HAWQSEGMENT' in recommendedComponents)

    # make sure master components are not collocated
    componentsListList = [hostgroup["components"] for hostgroup in
                          recommendations["recommendations"]["blueprint"]["host_groups"]]
    for sublist in componentsListList:
      hostComponents = [item["name"] for item in sublist]
      self.assertFalse({'HAWQMASTER', 'HAWQSTANDBY'}.issubset(hostComponents))

  def test_createComponentLayoutRecommendations_hawq_1_Host(self):

    services = self.load_json("services-hawq-3-hosts.json")
    componentNames = self.getComponentsFromServices(services)
    self.assertTrue('HAWQSTANDBY' in componentNames)

    hosts = self.load_json("hosts-1-host.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 1)

    self.insertHAWQServiceAdvisorInfo(services)
    recommendations = self.serviceAdvisor.recommendComponentLayout(services, hosts)
    recommendedComponents = self.getComponentsFromRecommendations(recommendations)
    self.assertTrue('HAWQMASTER' in recommendedComponents)
    self.assertFalse('HAWQSTANDBY' in recommendedComponents)
    self.assertTrue('HAWQSEGMENT' in recommendedComponents)

  def test_createComponentLayoutRecommendations_no_hawq_3_Hosts(self):
    """ Test no failures when there are no HAWQ components """

    services = self.load_json("services-nohawq-3-hosts.json")
    componentNames = self.getComponentsFromServices(services)
    self.assertFalse('HAWQMASTER' in componentNames)
    self.assertFalse('HAWQSTANDBY' in componentNames)
    self.assertFalse('HAWQSEGMENT' in componentNames)

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    self.insertHAWQServiceAdvisorInfo(services)
    recommendations = self.serviceAdvisor.recommendComponentLayout(services, hosts)
    recommendedComponents = self.getComponentsFromRecommendations(recommendations)
    self.assertFalse('HAWQMASTER' in recommendedComponents)
    self.assertFalse('HAWQSTANDBY' in recommendedComponents)
    self.assertFalse('HAWQSEGMENT' in recommendedComponents)

  def test_createComponentLayoutRecommendations_hawqsegment_cluster_install(self):
    """ Test that HAWQSEGMENT gets recommended correctly during Cluster Install Wizard, when HAWQ is selected for installation """

    hosts = self.prepareHosts(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])
    services =  {
                  "services" : [
                    {
                      "StackServices" : {
                        "service_name" : "HDFS"
                      },
                      "components" : [
                        {
                          "StackServiceComponents" : {
                            "cardinality" : "1+",
                            "component_category" : "SLAVE",
                            "component_name" : "DATANODE",
                            "hostnames" : []
                          }
                        }
                      ]
                    },
                    {
                      "StackServices" : {
                        "service_name" : "HAWQ"
                      },
                      "components" : [
                        {
                          "StackServiceComponents" : {
                            "cardinality" : "1+",
                            "component_category" : "SLAVE",
                            "component_name" : "HAWQSEGMENT",
                            "hostnames" : []
                          }
                        }
                      ]
                    }
                  ]
                }

    hawqSegmentHosts = {"c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"}
    self.insertHAWQServiceAdvisorInfo(services)
    recommendations = self.serviceAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostNames = self.getHostsFromRecommendations(recommendations, "HAWQSEGMENT")
    self.assertEquals(set(hostNames), hawqSegmentHosts)

  def test_createComponentLayoutRecommendations_hawqsegment_add_service_wizard_to_be_installed(self):
    """ Test that HAWQSEGMENT gets recommended correctly during Add Service Wizard, when HAWQ is selected for installation """

    hosts = self.prepareHosts(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])
    services =  {
                  "services" : [
                    {
                      "StackServices" : {
                        "service_name" : "HDFS"
                      },
                      "components" : [
                        {
                          "StackServiceComponents" : {
                            "cardinality" : "1+",
                            "component_category" : "SLAVE",
                            "component_name" : "DATANODE",
                            "hostnames" : ["c6401.ambari.apache.org", "c6403.ambari.apache.org"]
                          }
                        }
                      ]
                    },
                    {
                      "StackServices" : {
                        "service_name" : "HAWQ"
                      },
                      "components" : [
                        {
                          "StackServiceComponents" : {
                            "cardinality" : "1+",
                            "component_category" : "SLAVE",
                            "component_name" : "HAWQSEGMENT",
                            "hostnames" : []
                          }
                        }
                      ]
                    }
                  ]
                }

    hawqSegmentHosts = {"c6401.ambari.apache.org", "c6403.ambari.apache.org"}
    self.insertHAWQServiceAdvisorInfo(services)
    recommendations = self.serviceAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostNames = self.getHostsFromRecommendations(recommendations, "HAWQSEGMENT")
    self.assertEquals(set(hostNames), hawqSegmentHosts)

  def test_createComponentLayoutRecommendations_hawqsegment_add_service_wizard_already_installed(self):
    """ Test that HAWQSEGMENT does not get recommended during Add Service Wizard, when HAWQ has already been installed """

    hosts = self.prepareHosts(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])
    services =  {
                  "services" : [
                    {
                      "StackServices" : {
                        "service_name" : "HDFS"
                      },
                      "components" : [
                        {
                          "StackServiceComponents" : {
                            "cardinality" : "1+",
                            "component_category" : "SLAVE",
                            "component_name" : "DATANODE",
                            "hostnames" : ["c6401.ambari.apache.org", "c6403.ambari.apache.org"]
                          }
                        }
                      ]
                    },
                    {
                      "StackServices" : {
                        "service_name" : "HAWQ"
                      },
                      "components" : [
                        {
                          "StackServiceComponents" : {
                            "cardinality" : "1+",
                            "component_category" : "SLAVE",
                            "component_name" : "HAWQSEGMENT",
                            "hostnames" : ["c6402.ambari.apache.org"]
                          }
                        }
                      ]
                    },
                    {
                      "StackServices" : {
                        "service_name" : "PXF"
                      },
                      "components" : [
                        {
                          "StackServiceComponents" : {
                            "cardinality" : "1+",
                            "component_category" : "SLAVE",
                            "component_name" : "PXF",
                            "hostnames" : []
                          }
                        }
                      ]
                    }
                  ]
                }

    hawqSegmentHosts = {"c6402.ambari.apache.org"}
    self.insertHAWQServiceAdvisorInfo(services)
    recommendations = self.serviceAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostNames = self.getHostsFromRecommendations(recommendations, "HAWQSEGMENT")
    self.assertEquals(set(hostNames), hawqSegmentHosts)

  def test_getComponentLayoutValidations_hawqsegment_not_co_located_with_datanode(self):
    """ Test validation warning for HAWQ segment not colocated with DATANODE """

    services = self.load_json("services-normal-hawq-3-hosts.json")
    hosts = self.load_json("hosts-3-hosts.json")
    componentsList = self.getComponentsListFromServices(services)

    hawqsegmentComponent = self.getHosts(componentsList, "HAWQSEGMENT")
    hawqsegmentComponent["hostnames"] = ['c6401.ambari.apache.org']
    datanodeComponent = self.getHosts(componentsList, "DATANODE")
    datanodeComponent["hostnames"] = ['c6402.ambari.apache.org']

    self.insertHAWQServiceAdvisorInfo(services)
    validations = self.serviceAdvisor.getComponentLayoutValidations(services, hosts)
    expected = {
      'type': 'host-component',
      'level': 'WARN',
      'component-name': 'HAWQSEGMENT',
      'message': 'HAWQ Segment must be installed on all DataNodes. The following 2 host(s) do not satisfy the colocation recommendation: c6401.ambari.apache.org, c6402.ambari.apache.org',
    }
    self.assertEquals(validations[0], expected)

    datanodeComponent["hostnames"] = ['c6401.ambari.apache.org']
    validations = self.serviceAdvisor.getComponentLayoutValidations(services, hosts)
    self.assertEquals(len(validations), 0)


  @patch('socket.getfqdn', side_effect=fqdn_mock_result)
  def test_getComponentLayoutValidations_hawq_3_Hosts(self, socket_mock):
    """ Test layout validations for HAWQ components on a 3-node cluster """

    # case-1: normal placement, no warnings
    services = self.load_json("services-normal-hawq-3-hosts.json")
    componentsList = self.getComponentsListFromServices(services)
    hawqMasterHosts = self.getHosts(componentsList, "HAWQMASTER")["hostnames"]
    hawqStandbyHosts = self.getHosts(componentsList, "HAWQSTANDBY")["hostnames"]
    self.assertEquals(len(hawqMasterHosts), 1)
    self.assertEquals(len(hawqStandbyHosts), 1)
    self.assertNotEquals(hawqMasterHosts[0], hawqStandbyHosts[0])

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    self.insertHAWQServiceAdvisorInfo(services)
    validations = self.serviceAdvisor.getComponentLayoutValidations(services, hosts)
    self.assertEquals(len(validations), 0)

    # case-2: HAWQ masters are collocated
    services = self.load_json("services-master_standby_colo-3-hosts.json")
    componentsList = self.getComponentsListFromServices(services)
    hawqMasterHosts = self.getHosts(componentsList, "HAWQMASTER")["hostnames"]
    hawqStandbyHosts = self.getHosts(componentsList, "HAWQSTANDBY")["hostnames"]
    self.assertEquals(len(hawqMasterHosts), 1)
    self.assertEquals(len(hawqStandbyHosts), 1)
    self.assertEquals(hawqMasterHosts[0], hawqStandbyHosts[0])

    self.insertHAWQServiceAdvisorInfo(services)
    validations = self.serviceAdvisor.getComponentLayoutValidations(services, hosts)
    self.assertEquals(len(validations), 1)
    expected = {
      'component-name': 'HAWQSTANDBY',
      'message': 'HAWQ Master and HAWQ Standby Master cannot be deployed on the same host.',
      'type': 'host-component',
      'host': 'c6403.ambari.apache.org',
      'level': 'ERROR'
    }
    self.assertEquals(validations[0], expected)

    # case-3: HAWQ Master and Ambari Server are collocated
    services = self.load_json("services-master_ambari_colo-3-hosts.json")
    componentsList = self.getComponentsListFromServices(services)
    hawqMasterHosts = self.getHosts(componentsList, "HAWQMASTER")["hostnames"]
    hawqStandbyHosts = self.getHosts(componentsList, "HAWQSTANDBY")["hostnames"]
    self.assertEquals(len(hawqMasterHosts), 1)
    self.assertEquals(len(hawqStandbyHosts), 1)
    self.assertNotEquals(hawqMasterHosts[0], hawqStandbyHosts[0])
    self.assertEquals(hawqMasterHosts[0], "c6401.ambari.apache.org")

    self.insertHAWQServiceAdvisorInfo(services)
    validations = self.serviceAdvisor.getComponentLayoutValidations(services, hosts)
    self.assertEquals(len(validations), 1)
    expected = {
      'component-name': 'HAWQMASTER',
      'message': 'The default Postgres port (5432) on the Ambari Server conflicts with the default HAWQ Masters port. '  +
                 'If you are using port 5432 for Postgres, you must either deploy the HAWQ Master on a different host ' +
                 'or configure a different port for the HAWQ Masters in the HAWQ Configuration page.',
      'type': 'host-component',
      'host': 'c6401.ambari.apache.org',
      'level': 'WARN'
    }
    self.assertEquals(validations[0], expected)

    # case-4: HAWQ Standby and Ambari Server are collocated
    services = self.load_json("services-standby_ambari_colo-3-hosts.json")
    componentsList = self.getComponentsListFromServices(services)
    hawqMasterHosts = self.getHosts(componentsList, "HAWQMASTER")["hostnames"]
    hawqStandbyHosts = self.getHosts(componentsList, "HAWQSTANDBY")["hostnames"]
    self.assertEquals(len(hawqMasterHosts), 1)
    self.assertEquals(len(hawqStandbyHosts), 1)
    self.assertNotEquals(hawqMasterHosts[0], hawqStandbyHosts[0])
    self.assertEquals(hawqStandbyHosts[0], "c6401.ambari.apache.org")

    self.insertHAWQServiceAdvisorInfo(services)
    validations = self.serviceAdvisor.getComponentLayoutValidations(services, hosts)
    self.assertEquals(len(validations), 1)
    expected = {
      'component-name': 'HAWQSTANDBY',
      'message': 'The default Postgres port (5432) on the Ambari Server conflicts with the default HAWQ Masters port. '  +
                 'If you are using port 5432 for Postgres, you must either deploy the HAWQ Standby Master on a different host ' +
                 'or configure a different port for the HAWQ Masters in the HAWQ Configuration page.',
      'type': 'host-component',
      'host': 'c6401.ambari.apache.org',
      'level': 'WARN'
    }
    self.assertEquals(validations[0], expected)


  @patch('socket.getfqdn', side_effect=fqdn_mock_result)
  def test_getComponentLayoutValidations_nohawq_3_Hosts(self, socket_mock):
    """ Test no failures when there are no HAWQ components on a 3-node cluster """

    # normal placement, no warnings
    services = self.load_json("services-normal-nohawq-3-hosts.json")
    componentsList = self.getComponentsListFromServices(services)
    hawqMasterHosts = self.getHosts(componentsList, "HAWQMASTER")
    hawqStandbyHosts = self.getHosts(componentsList, "HAWQSTANDBY")
    self.assertEquals(len(hawqMasterHosts), 0)
    self.assertEquals(len(hawqStandbyHosts), 0)

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    self.insertHAWQServiceAdvisorInfo(services)
    validations = self.serviceAdvisor.getComponentLayoutValidations(services, hosts)
    self.assertEquals(len(validations), 0)

  def test_recommendHAWQConfigurations(self):

    hosts = {
      "items": [
        {
          "Hosts": {
            "host_name": "c6401.ambari.apache.org",
            "cpu_count" : 2,
            "total_mem": 33554432
          }
        },
        {
          "Hosts": {
            "host_name": "c6402.ambari.apache.org",
            "cpu_count" : 4,
            "total_mem": 33554433
          }
        },
        {
          "Hosts": {
            "host_name": "c6403.ambari.apache.org",
            "cpu_count" : 1,
            "total_mem": 33554434
          }
        },
        {
          "Hosts": {
            "host_name": "c6404.ambari.apache.org",
            "cpu_count" : 2,
            "total_mem": 33554435
          }
        }
      ]
    }

    # original cluster data with 3 segments
    services = self.load_json("services-normal-hawq-3-hosts.json")
    componentsList = self.getComponentsListFromServices(services)
    hawqSegmentComponent = self.getHosts(componentsList, "HAWQSEGMENT")

    # setup default configuration values
    services["configurations"]["hawq-site"] = {"properties": {"default_hash_table_bucket_number": "24",
                                                              "hawq_rm_nvseg_perquery_limit": "512",
                                                              "hawq_rm_yarn_address": "localhost:8032",
                                                              "hawq_rm_yarn_scheduler_address": "localhost:8030",
                                                              "hawq_global_rm_type":  "none"}}

    services["configurations"]["hdfs-client"] = {"properties": {"output.replace-datanode-on-failure": "true"}}
    services["configurations"]["hawq-sysctl-env"] = {"properties": {}}

    services["configurations"]["yarn-site"] = {"properties": {"yarn.resourcemanager.address": "host1:8050",
                                                              "yarn.resourcemanager.scheduler.address": "host1:8030"}}
    services["services"].append({"StackServices" : {"service_name" : "YARN"}, "components":[]})
    configurations = {}
    clusterData = {}
    self.insertHAWQServiceAdvisorInfo(services)

    # Test 1 - with 3 segments
    self.assertEquals(len(hawqSegmentComponent["hostnames"]), 3)
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations["hawq-site"]["properties"]["default_hash_table_bucket_number"], str(3 * 6))
    self.assertEquals(configurations["hdfs-client"]["properties"]["output.replace-datanode-on-failure"], "false")

    # check derived properties
    self.assertEquals(configurations["hawq-site"]["properties"]["hawq_rm_yarn_address"], "host1:8050")
    self.assertEquals(configurations["hawq-site"]["properties"]["hawq_rm_yarn_scheduler_address"], "host1:8030")

    # Test 2 - with 100 segments
    hawqSegmentComponent["hostnames"] = ["host" + str(i) for i in range(100)]
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations["hawq-site"]["properties"]["default_hash_table_bucket_number"], str(100 * 5))
    self.assertEquals(configurations["hdfs-client"]["properties"]["output.replace-datanode-on-failure"], "true")

    # Test 3 - with 512 segments
    hawqSegmentComponent["hostnames"] = ["host" + str(i) for i in range(512)]
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations["hawq-site"]["properties"]["default_hash_table_bucket_number"], "512")
    self.assertEquals(configurations["hdfs-client"]["properties"]["output.replace-datanode-on-failure"], "true")

    # Test 4 - with 513 segments
    hawqSegmentComponent["hostnames"] = ["host" + str(i) for i in range(513)]
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations["hawq-site"]["properties"]["default_hash_table_bucket_number"], "512")
    self.assertEquals(configurations["hdfs-client"]["properties"]["output.replace-datanode-on-failure"], "true")

    # Test 5 - with no segments
    configurations = {}
    services["configurations"]["hawq-site"] = {"properties":{"hawq_global_rm_type": "none"}}
    hawqSegmentComponent["hostnames"] = []
    self.serviceAdvisor.getServiceConfigurationRecommendations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations["hdfs-client"]["properties"]["output.replace-datanode-on-failure"], "false")
    self.assertTrue("default_hash_table_bucket_number" not in configurations["hawq-site"])


  def test_validateHAWQSiteConfigurations(self):
    services = self.load_json("services-hawq-3-hosts.json")
    # setup default configuration values
    # Test hawq_rm_yarn_address and hawq_rm_scheduler_address are set correctly
    configurations = services["configurations"]
    configurations["hawq-site"] = {"properties": {"hawq_rm_yarn_address": "localhost:8032",
                                                  "hawq_rm_yarn_scheduler_address": "localhost:8030"}}
    configurations["yarn-site"] = {"properties": {"yarn.resourcemanager.address": "host1:8050",
                                                  "yarn.resourcemanager.scheduler.address": "host1:8030"}}
    services["services"].append({"StackServices" : {"service_name" : "YARN"}, "components":[]})
    properties = configurations["hawq-site"]["properties"]
    defaults = {}
    hosts = {}

    expected_warnings = {
      'hawq_rm_yarn_address': {
        'config-type': 'hawq-site',
        'message': 'Expected value: host1:8050 (this property should have the same value as the property yarn.resourcemanager.address in yarn-site)',
        'type': 'configuration',
        'config-name': 'hawq_rm_yarn_address',
        'level': 'WARN'
      },
      'hawq_rm_yarn_scheduler_address': {
        'config-type': 'hawq-site',
        'message': 'Expected value: host1:8030 (this property should have the same value as the property yarn.resourcemanager.scheduler.address in yarn-site)',
        'type': 'configuration',
        'config-name': 'hawq_rm_yarn_scheduler_address',
        'level': 'WARN'
      }
    }

    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    problems_dict = {}
    for problem in problems:
      problems_dict[problem['config-name']] = problem
    self.assertEqual(len(problems), 2)
    self.assertEqual(problems_dict, expected_warnings)

    # Test hawq_master_directory multiple directories validation
    configurations["hawq-site"] = {"properties": {"hawq_master_directory": "/data/hawq/master",
                                                  "hawq_segment_directory": "/data/hawq/segment"}}
    properties = configurations["hawq-site"]["properties"]
    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    problems_dict = {}
    self.assertEqual(len(problems), 0)
    expected_warnings = {}
    self.assertEqual(problems_dict, expected_warnings)

    configurations["hawq-site"] = {"properties": {"hawq_master_directory": "/data/hawq/master1,/data/hawq/master2",
                                                  "hawq_segment_directory": "/data/hawq/segment1 /data/hawq/segment2"}}
    properties = configurations["hawq-site"]["properties"]
    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    problems_dict = {}
    for problem in problems:
      problems_dict[problem['config-name']] = problem
    self.assertEqual(len(problems), 2)
    expected_warnings = {
      'hawq_master_directory': {
        'config-type': 'hawq-site',
        'message': 'Multiple directories for HAWQ Master directory are not allowed.',
        'type': 'configuration',
        'config-name': 'hawq_master_directory',
        'level': 'ERROR'
      },
      'hawq_segment_directory': {
        'config-type': 'hawq-site',
        'message': 'Multiple directories for HAWQ Segment directory are not allowed.',
        'type': 'configuration',
        'config-name': 'hawq_segment_directory',
        'level': 'ERROR'
      }
    }
    self.assertEqual(problems_dict, expected_warnings)

    # Test hawq_global_rm_type validation
    services = {
                 "services" : [
                   {
                     "StackServices" : {
                     "service_name" : "HAWQ"
                     },
                     "components": []
                   } ],
                 "configurations":
                   {
                     "hawq-site": {
                       "properties": {
                         "hawq_global_rm_type": "yarn"
                       }
                     }
                   }
                }
    properties = services["configurations"]["hawq-site"]["properties"]

    # case 1: hawq_global_rm_type is set as yarn, but YARN service is not installed. Validation error expected.
    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, services["configurations"], services, hosts)
    self.assertEqual(len(problems), 1)
    expected = {
      "config-type": "hawq-site",
      "message": "hawq_global_rm_type must be set to none if YARN service is not installed",
      "type": "configuration",
      "config-name": "hawq_global_rm_type",
      "level": "ERROR"
    }
    self.assertEqual(problems[0], expected)

    # case 2: hawq_global_rm_type is set as yarn, and YARN service is installed. No validation errors expected.
    services["services"].append({"StackServices" : {"service_name" : "YARN"}, "components":[]})

    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, services["configurations"], services, hosts)
    self.assertEqual(len(problems), 0)

    # Test HAWQ Master port conflict with Ambari Server Postgres port

    # case 1: HAWQ Master is placed on Ambari Server and HAWQ Master port is same as Ambari Server Postgres Port
    self.serviceAdvisor.isHawqMasterComponentOnAmbariServer = MagicMock(return_value=True)
    configurations = {
      "hawq-site": {
        "properties":
          {"hawq_master_address_port": "5432"}
      }
    }
    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    expected = {
      "config-name": "hawq_master_address_port",
      "config-type": "hawq-site",
      "level": "WARN",
      "message": "The default Postgres port (5432) on the Ambari Server conflicts with the default HAWQ Masters port. "
                 "If you are using port 5432 for Postgres, you must either deploy the HAWQ Masters on a different host "
                 "or configure a different port for the HAWQ Masters in the HAWQ Configuration page.",
      "type": "configuration"}
    self.assertEqual(problems[0], expected)

    # case 2: HAWQ Master is placed on Ambari Server and HAWQ Master port is different from  Ambari Server Postgres Port
    self.serviceAdvisor.isHawqMasterComponentOnAmbariServer = MagicMock(return_value=True)
    configurations["hawq-site"]["properties"]["hawq_master_address_port"] = "10432"
    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)

    # case 3: HAWQ Master is not placed on Ambari Server and HAWQ Master port is same as  Ambari Server Postgres Port
    self.serviceAdvisor.isHawqMasterComponentOnAmbariServer = MagicMock(return_value=False)
    configurations["hawq-site"]["properties"]["hawq_master_address_port"] = "5432"
    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)

    # case 4: HAWQ Master is not placed on Ambari Server and HAWQ Master port is different from  Ambari Server Postgres Port
    self.serviceAdvisor.isHawqMasterComponentOnAmbariServer = MagicMock(return_value=False)
    configurations["hawq-site"]["properties"]["hawq_master_address_port"] = "10432"
    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)

    # -------- test query limits warning ----------
    services = {
      "services":  [
        { "StackServices": {"service_name": "HAWQ"},
          "components": [{
            "StackServiceComponents": {
              "component_name": "HAWQSEGMENT",
              "hostnames": []
            }}]
          }],
      "configurations": {}
    }
    # setup default configuration values
    configurations = services["configurations"]
    configurations["hawq-site"] = {"properties": {"default_hash_table_bucket_number": "600",
                                                  "hawq_rm_nvseg_perquery_limit": "500"}}
    properties = configurations["hawq-site"]["properties"]
    defaults = {}
    hosts = {}

    expected = {
      'config-type': 'hawq-site',
      'message': 'Default buckets for Hash Distributed tables parameter value should not be greater than the value of Virtual Segments Limit per Query (Total) parameter, currently set to 500.',
      'type': 'configuration',
      'config-name': 'default_hash_table_bucket_number',
      'level': 'ERROR'
    }
    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    self.assertEqual(problems[0], expected)

    configurations["hawq-site"] = {"properties": {"default_hash_table_bucket_number": "500",
                                                  "hawq_rm_nvseg_perquery_limit": "500"}}
    properties = configurations["hawq-site"]["properties"]
    problems = self.serviceAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)


  def test_validateHAWQHdfsClientConfigurations(self):
    services = {
      "services":  [
        { "StackServices": {"service_name": "HAWQ"},
          "components": [{
            "StackServiceComponents": {
              "component_name": "HAWQSEGMENT",
              "hostnames": []
            }}]
          }],
      "configurations": {}
    }
    # setup default configuration values
    configurations = services["configurations"]
    configurations["hdfs-client"] = {"properties": {"output.replace-datanode-on-failure": "true"}}
    properties = configurations["hdfs-client"]["properties"]
    defaults = {}
    hosts = {}

    # 1. Try with no hosts
    expected = {
        'config-type': 'hdfs-client',
        'message': 'output.replace-datanode-on-failure should be set to false (unchecked) for clusters with 3 or less HAWQ Segments',
        'type': 'configuration',
        'config-name': 'output.replace-datanode-on-failure',
        'level': 'WARN'
    }

    problems = self.serviceAdvisor.validateHAWQHdfsClientConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    self.assertEqual(problems[0], expected)

    # 2. Try with 3 hosts
    services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"] = ["host1", "host2", "host3"]
    problems = self.serviceAdvisor.validateHAWQHdfsClientConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    self.assertEqual(problems[0], expected)

    # 3. Try with 4 hosts - default value
    services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"] = ["host1", "host2", "host3", "host4"]
    problems = self.serviceAdvisor.validateHAWQHdfsClientConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)

    # 4. Try with 4 hosts
    properties = {"output.replace-datanode-on-failure": "false"}
    expected = {
      'config-type': 'hdfs-client',
      'message': 'output.replace-datanode-on-failure should be set to true (checked) for clusters with more than 3 HAWQ Segments',
      'type': 'configuration',
      'config-name': 'output.replace-datanode-on-failure',
      'level': 'WARN'
    }
    problems = self.serviceAdvisor.validateHAWQHdfsClientConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    self.assertEqual(problems[0], expected)
