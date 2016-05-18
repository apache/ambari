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
from unittest import TestCase


class TestHAWQ200ServiceAdvisor(TestCase):

  def setUp(self):
    import imp
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    hawq200ServiceAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/common-services/HAWQ/2.0.0/service_advisor.py')

    with open(stackAdvisorPath, 'rb') as fp:
      stack_advisor = imp.load_module('stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hawq200ServiceAdvisorPath, 'rb') as fp:
      service_advisor = imp.load_module('stack_advisor_impl', fp, hawq200ServiceAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))

    stackAdvisorClass = getattr(stack_advisor, 'StackAdvisor')
    self.stackAdvisor = stackAdvisorClass()

    serviceAdvisorClass = getattr(service_advisor, 'HAWQ200ServiceAdvisor')
    self.serviceAdvisor = serviceAdvisorClass()

  def test_getServiceConfigurationRecommendations(self):

    configurations = {
      "hawq-sysctl-env": {
        "properties": {
          "vm.overcommit_memory": "1"
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
            "total_mem": 33554432
          }
        },
        {
          "Hosts": {
            "host_name": "c6402.ambari.apache.org",
            "total_mem": 33554433
          }
        },
        {
          "Hosts": {
            "host_name": "c6403.ambari.apache.org",
            "total_mem": 33554434
          }
        },
        {
          "Hosts": {
            "host_name": "c6404.ambari.apache.org",
            "total_mem": 33554435
          }
        }
      ]
    }

    ## Test if vm.overcommit_memory is set correctly

    # Case 1: All machines have total_mem above 32GB (total_mem >= 33554432)
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-sysctl-env"]["properties"]["vm.overcommit_memory"], "2")

    # Case 2: One machine has total_mem below 32GB
    hosts["items"][0]["Hosts"]["total_mem"] = 33554431
    self.serviceAdvisor.getServiceConfigurationRecommendations(self.stackAdvisor, configurations, None, services, hosts)
    self.assertEquals(configurations["hawq-sysctl-env"]["properties"]["vm.overcommit_memory"], "1")