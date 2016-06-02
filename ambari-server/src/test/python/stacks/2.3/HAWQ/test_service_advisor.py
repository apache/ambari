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
import imp
from unittest import TestCase
from mock.mock import patch


class TestHAWQ200ServiceAdvisor(TestCase):

  def setUp(self):
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    hawq200ServiceAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/common-services/HAWQ/2.0.0/service_advisor.py')

    with open(stackAdvisorPath, 'rb') as fp:
      stack_advisor = imp.load_module('stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hawq200ServiceAdvisorPath, 'rb') as fp:
      service_advisor = imp.load_module('stack_advisor_impl', fp, hawq200ServiceAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))

    stackAdvisorClass = getattr(stack_advisor, 'DefaultStackAdvisor')
    self.stackAdvisor = stackAdvisorClass()

    serviceAdvisorClass = getattr(service_advisor, 'HAWQ200ServiceAdvisor')
    self.serviceAdvisor = serviceAdvisorClass()


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
    self.stackAdvisor.loadServiceAdvisors(services)
    standbyHosts = self.serviceAdvisor.getHostsForMasterComponent(self.stackAdvisor, services, None, component, hostsList, None)
    self.assertEquals(standbyHosts, ["c6402.ambari.apache.org"])

    # Case 2:
    # Ambari Server is placed on c6401.ambari.apache.org
    # HAWQMASTER is placed on c6402.ambari.apache.org
    # There are 4 available hosts in the cluster
    # Recommend HAWQSTANDBY on next available host, c6403.ambari.apache.org
    services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"] = ["c6402.ambari.apache.org"]
    standbyHosts = self.serviceAdvisor.getHostsForMasterComponent(self.stackAdvisor, services, None, component, hostsList, None)
    self.assertEquals(standbyHosts, ["c6403.ambari.apache.org"])

    # Case 3:
    # Ambari Server is placed on c6401.ambari.apache.org
    # HAWQMASTER is placed on c6402.ambari.apache.org
    # There are 2 available hosts in the cluster
    # Recommend HAWQSTANDBY on a host which does not have HAWQMASTER, c6401.ambari.apache.org
    hostsList = ["c6401.ambari.apache.org", "c6402.ambari.apache.org"]
    standbyHosts = self.serviceAdvisor.getHostsForMasterComponent(self.stackAdvisor, services, None, component, hostsList, None)
    self.assertEquals(standbyHosts, ["c6401.ambari.apache.org"])

    # Case 4:
    # Ambari Server is placed on c6401.ambari.apache.org
    # HAWQMASTER is placed on c6401.ambari.apache.org
    # There is 1 available host in the cluster
    # Do not recommend HAWQSTANDBY on a single node cluster
    hostsList = ["c6401.ambari.apache.org"]
    services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"] = ["c6401.ambari.apache.org"]
    standbyHosts = self.serviceAdvisor.getHostsForMasterComponent(self.stackAdvisor, services, None, component, hostsList, None)
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
    standbyHosts = self.serviceAdvisor.getHostsForMasterComponent(self.stackAdvisor, services, None, component, hostsList, None)
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
          "hawq_rm_memory_limit_perseg": "67108864KB",
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
    # Do not recommend hawq_rm_nvcore_limit_perseg when rm type is yarn
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-site"]["properties"]["hawq_rm_nvcore_limit_perseg"], "16")

    # Case 2:
    # HAWQ Hosts Core Count: c6401.ambari.apache.org - 2, c6402.ambari.apache.org - 4, c6404.ambari.apache.org - 2
    # hawq_global_rm_type: none
    # Non HAWQ Hosts Core Count: c6401.ambari.apache.org - 1
    # Recommend hawq_rm_nvcore_limit_perseg when rm type is none
    configurations["hawq-site"]["properties"]["hawq_global_rm_type"] = "none"
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-site"]["properties"]["hawq_rm_nvcore_limit_perseg"], "2")


    ## Test if vm.overcommit_memory is set correctly

    # Case 1: All machines have total_mem above 32GB (total_mem >= 33554432)
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-sysctl-env"]["properties"]["vm.overcommit_memory"], "2")

    # Case 2: One machine has total_mem below 32GB
    hosts["items"][0]["Hosts"]["total_mem"] = 33554431
    services["configurations"]["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"] = "67108864KB"
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-sysctl-env"]["properties"]["vm.overcommit_memory"], "1")

    ## Test if hawq_rm_memory_limit_perseg is set correctly

    # Case 1: Minimum host memory is ~ 2 GB (2048MB), recommended val must be .75% of 2GB as vm.overcommit_memory = 1 and in MB
    hosts["items"][0]["Hosts"]["total_mem"] = 2097152
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "1536MB")

    # Case 2: Minimum host memory is ~ 16 GB, recommended val must be .75% of 16GB as vm.overcommit_memory = 1 and in GB
    hosts["items"][0]["Hosts"]["total_mem"] = 16777216
    hosts["items"][1]["Hosts"]["total_mem"] = 26777216
    hosts["items"][3]["Hosts"]["total_mem"] = 36777216
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "12GB")

    # Case 2: Minimum host memory is ~ 64 GB, recommended val must be .75% of 32GB as vm.overcommit_memory = 2 and in GB
    hosts["items"][0]["Hosts"]["total_mem"] = 67108864
    hosts["items"][1]["Hosts"]["total_mem"] = 77108864
    hosts["items"][3]["Hosts"]["total_mem"] = 87108864
    services["configurations"]["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"] = "67108864KB"
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "24GB")

    # Case 4: Minimum host memory is ~ 512 GB, recommended val must be .85% of 256GB as vm.overcommit_memory = 2 and in GB
    hosts["items"][0]["Hosts"]["total_mem"] = 536870912
    hosts["items"][1]["Hosts"]["total_mem"] = 636870912
    hosts["items"][3]["Hosts"]["total_mem"] = 736870912
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "218GB")

    # Case 5: Minimum host memory is ~ 1024 GB, recommended val must be .95% of 512GB as vm.overcommit_memory = 2 and in GB
    hosts["items"][0]["Hosts"]["total_mem"] = 1073741824
    hosts["items"][1]["Hosts"]["total_mem"] = 2073741824
    hosts["items"][3]["Hosts"]["total_mem"] = 3073741824
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "436GB")

    # Case 6: Minimum host memory is ~ 1024 GB, vm.overcommit_ratio = 75, vm.overcommit_memory = 2
    # recommended val must be .95% of (1024*75)/100 and in GB
    hosts["items"][0]["Hosts"]["total_mem"] = 1073741824
    hosts["items"][1]["Hosts"]["total_mem"] = 2073741824
    hosts["items"][3]["Hosts"]["total_mem"] = 3073741824
    services["configurations"]["hawq-sysctl-env"]["properties"]["vm.overcommit_ratio"] = 75
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEqual(configurations["hawq-site"]["properties"]["hawq_rm_memory_limit_perseg"], "730GB")
