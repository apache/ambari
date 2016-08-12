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

import imp
import json
import os
from unittest import TestCase


class TestPXF300ServiceAdvisor(TestCase):

  testDirectory = os.path.dirname(os.path.abspath(__file__))
  stack_advisor_path = os.path.join(testDirectory, '../../../../main/resources/stacks/stack_advisor.py')
  with open(stack_advisor_path, 'rb') as fp:
    imp.load_module('stack_advisor', fp, stack_advisor_path, ('.py', 'rb', imp.PY_SOURCE))

  serviceAdvisorPath = '../../../../main/resources/common-services/PXF/3.0.0/service_advisor.py'
  pxf300ServiceAdvisorPath = os.path.join(testDirectory, serviceAdvisorPath)
  with open(pxf300ServiceAdvisorPath, 'rb') as fp:
    service_advisor_impl = imp.load_module('service_advisor_impl', fp, pxf300ServiceAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))

  def setUp(self):
    serviceAdvisorClass = getattr(self.service_advisor_impl, 'PXF300ServiceAdvisor')
    self.serviceAdvisor = serviceAdvisorClass()
    self.PXF_PATH = "export HBASE_CLASSPATH=${HBASE_CLASSPATH}:/usr/lib/pxf/pxf-hbase.jar"

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
    return [component["StackServiceComponents"] for component in componentsList if component["StackServiceComponents"]["component_name"] == componentName][0]

  def insertPXFServiceAdvisorInfo(self, services):
    for service in services["services"]:
      if service["StackServices"]["service_name"] == 'PXF':
        service["StackServices"]["advisor_name"] = "PXF300ServiceAdvisor"
        service["StackServices"]["advisor_path"] = self.pxf300ServiceAdvisorPath

  def test_getServiceConfigurationRecommendations(self):
    services = {
      "configurations": {
        "hbase-env": {
          "properties": {
            "content": "# Some hbase-env content text"
          }
        }
      }
    }

    ## Test is PXF_PATH is being added to hbase-env content

    # Case 1: Test pxf-hbase.jar classpath line was added to content
    expected = "# Some hbase-env content text\n\n#Add pxf-hbase.jar to HBASE_CLASSPATH\n" + self.PXF_PATH
    self.serviceAdvisor.getServiceConfigurationRecommendations(services["configurations"], None, services, None)
    self.assertEquals(services["configurations"]["hbase-env"]["properties"]["content"], expected)

    # Case 2: Test pxf-hbase.jar classpath line is not added again if content already has it
    services["configurations"]["hbase-env"]["properties"]["content"] = self.PXF_PATH
    expected = self.PXF_PATH
    self.serviceAdvisor.getServiceConfigurationRecommendations(services["configurations"], None, services, None)
    self.assertEquals(services["configurations"]["hbase-env"]["properties"]["content"], expected)

  def test_getConfigurationsValidationItems(self):
    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "PXF",
            "service_version": "2.0",
            "stack_name": "HDP",
            "stack_version": "2.3"
          }
        },
        {
          "StackServices": {
            "service_name": "HBASE",
            "service_version": "2.0",
            "stack_name": "HDP",
            "stack_version": "2.3"
          }
        }
      ],
      "configurations": {
        "hbase-env": {
          "properties": {
            "content": "# Some hbase-env content text"
          }
        }
      }
    }
    properties = services["configurations"]

    ## Test if PXF_PATH present in hbase-env content

    # Case 1: Generate warning item if PXF_PATH is not present in hbase-env
    expected = [
      {
        "config-type": "hbase-env",
        "message": "HBASE_CLASSPATH must contain the location of pxf-hbase.jar",
        "type": "configuration",
        "config-name": "content",
        "level": "WARN"
      }
    ]
    items = self.serviceAdvisor.getServiceConfigurationsValidationItems(properties, properties, services, None)
    self.assertEquals(items, expected)

    # Case 2: No warning should be generated if PXF_PATH is present in hbase-env
    properties = services["configurations"]["hbase-env"]["properties"]["content"] = self.PXF_PATH
    items = self.serviceAdvisor.getServiceConfigurationsValidationItems(properties, properties, services, None)
    self.assertEquals(items, [])


  def test_createComponentLayoutRecommendations_pxf_cluster_install(self):
    """ Test that PXF gets recommended correctly during Cluster Install Wizard, when PXF is selected for installation """

    hosts = self.prepareHosts(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])
    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "HDFS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "cardinality": "1+",
                "component_category": "MASTER",
                "component_name": "NAMENODE",
                "is_master": "true",
                "hostnames": []
              }
            },
            {
              "StackServiceComponents": {
                "cardinality": "1+",
                "component_category": "SLAVE",
                "component_name": "DATANODE",
                "hostnames": []
              }
            }
          ]
        },
        {
          "StackServices": {
            "service_name": "PXF"
          },
          "components": [
            {
              "StackServiceComponents": {
                "cardinality": "1+",
                "component_category": "SLAVE",
                "component_name": "PXF",
                "hostnames": []
              }
            }
          ]
        }
      ]
    }

    pxfHosts = set(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])

    self.insertPXFServiceAdvisorInfo(services)
    recommendations = self.serviceAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostGroups = [hostgroup["name"] for hostgroup in recommendations["blueprint"]["host_groups"] if
                  {"name": "PXF"} in hostgroup["components"]]
    hostNames = [host["fqdn"] for hostgroup in recommendations["blueprint_cluster_binding"]["host_groups"] if
                 hostgroup["name"] in hostGroups for host in hostgroup["hosts"]]
    self.assertEquals(set(hostNames), pxfHosts)


  def test_createComponentLayoutRecommendations_pxf_add_service_wizard_to_be_installed(self):
    """ Test that PXF gets recommended correctly during Add Service Wizard, when PXF is selected for installation """

    hosts = self.prepareHosts(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])
    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "HDFS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "cardinality": "1+",
                "component_category": "MASTER",
                "component_name": "NAMENODE",
                "is_master": "true",
                "hostnames": ["c6401.ambari.apache.org"]
              }
            },
            {
              "StackServiceComponents": {
                "cardinality": "1+",
                "component_category": "SLAVE",
                "component_name": "DATANODE",
                "hostnames": ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
              }
            }
          ]
        },
        {
          "StackServices": {
            "service_name": "PXF"
          },
          "components": [
            {
              "StackServiceComponents": {
                "cardinality": "1+",
                "component_category": "SLAVE",
                "component_name": "PXF",
                "hostnames": []
              }
            }
          ]
        }
      ]
    }

    pxfHosts = set(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])

    self.insertPXFServiceAdvisorInfo(services)
    recommendations = self.serviceAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostGroups = [hostgroup["name"] for hostgroup in recommendations["blueprint"]["host_groups"] if
                  {"name": "PXF"} in hostgroup["components"]]
    hostNames = [host["fqdn"] for hostgroup in recommendations["blueprint_cluster_binding"]["host_groups"] if
                 hostgroup["name"] in hostGroups for host in hostgroup["hosts"]]
    self.assertEquals(set(hostNames), pxfHosts)


  def test_createComponentLayoutRecommendations_pxf_add_service_wizard_already_installed(self):
    """ Test that PXF does not get recommended during Add Service Wizard, when PXF has already been installed """

    hosts = self.prepareHosts(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])
    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "HDFS"
          },
          "components": [
            {
              "StackServiceComponents": {
                "cardinality": "1+",
                "component_category": "MASTER",
                "component_name": "NAMENODE",
                "is_master": "true",
                "hostnames": ["c6401.ambari.apache.org"]
              }
            },
            {
              "StackServiceComponents": {
                "cardinality": "1+",
                "component_category": "SLAVE",
                "component_name": "DATANODE",
                "hostnames": ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
              }
            }
          ]
        },
        {
          "StackServices": {
            "service_name": "PXF"
          },
          "components": [
            {
              "StackServiceComponents": {
                "cardinality": "1+",
                "component_category": "SLAVE",
                "component_name": "PXF",
                "hostnames": ["c6402.ambari.apache.org"]
              }
            }
          ]
        },
        {
          "StackServices": {
            "service_name": "HBASE"
          },
          "components": [
            {
              "StackServiceComponents": {
                "cardinality": "1+",
                "component_category": "MASTER",
                "component_name": "HBASE_MASTER",
                "is_master": "true",
                "hostnames": []
              }
            }
          ]
        }
      ]
    }

    pxfHosts = set(["c6402.ambari.apache.org"])

    self.insertPXFServiceAdvisorInfo(services)
    recommendations = self.serviceAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostGroups = [hostgroup["name"] for hostgroup in recommendations["blueprint"]["host_groups"] if
                  {"name": "PXF"} in hostgroup["components"]]
    hostNames = [host["fqdn"] for hostgroup in recommendations["blueprint_cluster_binding"]["host_groups"] if
                 hostgroup["name"] in hostGroups for host in hostgroup["hosts"]]
    self.assertEquals(set(hostNames), pxfHosts)

  def test_getComponentLayoutValidations_pxf_not_co_located_with_nn(self):
    """ Test warning is generated when PXF is not co-located with NAMENODE """

    services = self.load_json("services-hawq-pxf-hdfs.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    nameNodeComponent = self.getHosts(componentsList, "NAMENODE")
    dataNodeComponent = self.getHosts(componentsList, "DATANODE")
    pxfComponent = self.getHosts(componentsList, "PXF")
    nameNodeComponent["hostnames"] = ["c6401.ambari.apache.org"]
    dataNodeComponent["hostnames"] = ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
    pxfComponent["hostnames"] = ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    validations = [validation for validation in self.serviceAdvisor.getServiceComponentLayoutValidations(services, hosts) if validation["component-name"] == "PXF"]
    self.assertEquals(len(validations), 1)
    expected = {
      "type": 'host-component',
      "level": 'WARN',
      "component-name": 'PXF',
      "message": 'PXF must be installed on the NameNode, Standby NameNode and all DataNodes. The following 1 host(s) do not satisfy the colocation recommendation: c6401.ambari.apache.org'
    }
    self.assertEquals(validations[0], expected)


  def test_getComponentLayoutValidations_pxf_not_co_located_with_dn(self):
    """ Test warning is generated when PXF is not co-located with NAMENODE or DATANODE """

    services = self.load_json("services-hawq-pxf-hdfs.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    nameNodeComponent = self.getHosts(componentsList, "NAMENODE")
    dataNodeComponent = self.getHosts(componentsList, "DATANODE")
    pxfComponent = self.getHosts(componentsList, "PXF")
    nameNodeComponent["hostnames"] = ["c6401.ambari.apache.org"]
    dataNodeComponent["hostnames"] = ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
    pxfComponent["hostnames"] = ["c6401.ambari.apache.org"]

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    validations = [validation for validation in self.serviceAdvisor.getServiceComponentLayoutValidations(services, hosts) if validation["component-name"] == "PXF"]
    self.assertEquals(len(validations), 1)
    expected = {
      "type": 'host-component',
      "level": 'WARN',
      "component-name": 'PXF',
      "message": 'PXF must be installed on the NameNode, Standby NameNode and all DataNodes. The following 2 host(s) do not satisfy the colocation recommendation: c6402.ambari.apache.org, c6403.ambari.apache.org'
    }
    self.assertEquals(validations[0], expected)


  def test_getComponentLayoutValidations_pxf_not_co_located_with_nn_or_dn(self):
    """ Test warning is generated when PXF is not co-located with NAMENODE or DATANODE """

    services = self.load_json("services-hawq-pxf-hdfs.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    nameNodeComponent = self.getHosts(componentsList, "NAMENODE")
    dataNodeComponent = self.getHosts(componentsList, "DATANODE")
    pxfComponent = self.getHosts(componentsList, "PXF")
    nameNodeComponent["hostnames"] = ["c6401.ambari.apache.org"]
    dataNodeComponent["hostnames"] = ["c6402.ambari.apache.org"]
    pxfComponent["hostnames"] = ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"]

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    validations = [validation for validation in self.serviceAdvisor.getServiceComponentLayoutValidations(services, hosts) if validation["component-name"] == "PXF"]
    self.assertEquals(len(validations), 1)
    expected = {
      "type": 'host-component',
      "level": 'WARN',
      "component-name": 'PXF',
      "message": 'PXF must be installed on the NameNode, Standby NameNode and all DataNodes. The following 1 host(s) do not satisfy the colocation recommendation: c6403.ambari.apache.org'
    }
    self.assertEquals(validations[0], expected)


  def test_getComponentLayoutValidations_pxf_co_located_with_nn_and_dn(self):
    """ Test NO warning is generated when PXF is co-located with NAMENODE and DATANODE """

    services = self.load_json("services-hawq-pxf-hdfs.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    nameNodeComponent = self.getHosts(componentsList, "NAMENODE")
    dataNodeComponent = self.getHosts(componentsList, "DATANODE")
    pxfComponent = self.getHosts(componentsList, "PXF")
    nameNodeComponent["hostnames"] = ["c6401.ambari.apache.org"]
    dataNodeComponent["hostnames"] = ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
    pxfComponent["hostnames"] = ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"]

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    validations = [validation for validation in self.serviceAdvisor.getServiceComponentLayoutValidations(services, hosts) if validation["component-name"] == "PXF"]
    self.assertEquals(len(validations), 0)
