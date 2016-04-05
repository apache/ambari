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

import json
import os
import socket
from unittest import TestCase
from mock.mock import patch, MagicMock


class TestHDP23StackAdvisor(TestCase):

  def setUp(self):
    import imp
    self.maxDiff = None
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    hdp206StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.0.6/services/stack_advisor.py')
    hdp21StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.1/services/stack_advisor.py')
    hdp22StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.2/services/stack_advisor.py')
    hdp23StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.3/services/stack_advisor.py')
    hdp23StackAdvisorClassName = 'HDP23StackAdvisor'
    with open(stackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp206StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp206StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp21StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp21StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp22StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp22StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp23StackAdvisorPath, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp23StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    clazz = getattr(stack_advisor_impl, hdp23StackAdvisorClassName)
    self.stackAdvisor = clazz()

    # substitute method in the instance
    self.get_system_min_uid_real = self.stackAdvisor.get_system_min_uid
    self.stackAdvisor.get_system_min_uid = self.get_system_min_uid_magic

  def load_json(self, filename):
    file = os.path.join(self.testDirectory, filename)
    with open(file, 'rb') as f:
      data = json.load(f)
    return data

  def prepareHosts(self, hostsNames):
    hosts = { "items": [] }
    for hostName in hostsNames:
      nextHost = {"Hosts":{"host_name" : hostName}}
      hosts["items"].append(nextHost)
    return hosts

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


  def test_createComponentLayoutRecommendations_hawq_1_Host(self):
    """ Test that HAWQSTANDBY is not recommended on a single node cluster """

    services = self.load_json("services-hawq-1-host.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    componentNames = [component["StackServiceComponents"]["component_name"] for component in componentsList]
    self.assertTrue('HAWQSTANDBY' in componentNames)

    hosts = self.load_json("hosts-1-host.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 1)

    recommendations = self.stackAdvisor.createComponentLayoutRecommendations(services, hosts)

    recommendedComponentsListList = [hostgroup["components"] for hostgroup in recommendations["blueprint"]["host_groups"]]
    recommendedComponents = [item["name"] for sublist in recommendedComponentsListList for item in sublist]
    self.assertTrue('HAWQMASTER' in recommendedComponents)
    self.assertFalse('HAWQSTANDBY' in recommendedComponents)
    self.assertTrue('HAWQSEGMENT' in recommendedComponents)


  def test_createComponentLayoutRecommendations_hawq_3_Hosts(self):
    """ Test that HAWQSTANDBY is recommended on a 3-node cluster """

    services = self.load_json("services-hawq-3-hosts.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    componentNames = [component["StackServiceComponents"]["component_name"] for component in componentsList]
    self.assertTrue('HAWQSTANDBY' in componentNames)

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    recommendations = self.stackAdvisor.createComponentLayoutRecommendations(services, hosts)

    recommendedComponentsListList = [hostgroup["components"] for hostgroup in recommendations["blueprint"]["host_groups"]]
    recommendedComponents = [item["name"] for sublist in recommendedComponentsListList for item in sublist]
    self.assertTrue('HAWQMASTER' in recommendedComponents)
    self.assertTrue('HAWQSTANDBY' in recommendedComponents)
    self.assertTrue('HAWQSEGMENT' in recommendedComponents)

    # make sure master components are not collocated
    for sublist in recommendedComponentsListList:
      hostComponents = [item["name"] for item in sublist]
      self.assertFalse(set(['HAWQMASTER', 'HAWQSTANDBY']).issubset(hostComponents))


  def test_createComponentLayoutRecommendations_no_hawq_3_Hosts(self):
    """ Test no failures when there are no HAWQ components """

    services = self.load_json("services-nohawq-3-hosts.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    componentNames = [component["StackServiceComponents"]["component_name"] for component in componentsList]
    self.assertFalse('HAWQMASTER' in componentNames)
    self.assertFalse('HAWQSTANDBY' in componentNames)
    self.assertFalse('HAWQSEGMENT' in componentNames)

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    recommendations = self.stackAdvisor.createComponentLayoutRecommendations(services, hosts)

    recommendedComponentsListList = [hostgroup["components"] for hostgroup in recommendations["blueprint"]["host_groups"]]
    recommendedComponents = [item["name"] for sublist in recommendedComponentsListList for item in sublist]
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

    hawqSegmentHosts = set(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])
    recommendations = self.stackAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostGroups = [ hostgroup["name"] for hostgroup in recommendations["blueprint"]["host_groups"] if {"name": "HAWQSEGMENT"} in hostgroup["components"] ]
    hostNames = [ host["fqdn"] for hostgroup in recommendations["blueprint_cluster_binding"]["host_groups"] if hostgroup["name"] in hostGroups for host in hostgroup["hosts"] ]
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

    hawqSegmentHosts = set(["c6401.ambari.apache.org", "c6403.ambari.apache.org"])
    recommendations = self.stackAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostGroups = [ hostgroup["name"] for hostgroup in recommendations["blueprint"]["host_groups"] if {"name": "HAWQSEGMENT"} in hostgroup["components"] ]
    hostNames = [ host["fqdn"] for hostgroup in recommendations["blueprint_cluster_binding"]["host_groups"] if hostgroup["name"] in hostGroups for host in hostgroup["hosts"] ]
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

    hawqSegmentHosts = set(["c6402.ambari.apache.org"])
    recommendations = self.stackAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostGroups = [ hostgroup["name"] for hostgroup in recommendations["blueprint"]["host_groups"] if {"name": "HAWQSEGMENT"} in hostgroup["components"] ]
    hostNames = [ host["fqdn"] for hostgroup in recommendations["blueprint_cluster_binding"]["host_groups"] if hostgroup["name"] in hostGroups for host in hostgroup["hosts"] ]
    self.assertEquals(set(hostNames), hawqSegmentHosts)

  def test_createComponentLayoutRecommendations_pxf_cluster_install(self):
    """ Test that PXF gets recommended correctly during Cluster Install Wizard, when PXF is selected for installation """

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
                            "component_category" : "MASTER",
                            "component_name" : "NAMENODE",
                            "is_master": "true",
                            "hostnames" : []
                          }
                        },
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

    pxfHosts = set(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])
    recommendations = self.stackAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostGroups = [ hostgroup["name"] for hostgroup in recommendations["blueprint"]["host_groups"] if {"name": "PXF"} in hostgroup["components"] ]
    hostNames = [ host["fqdn"] for hostgroup in recommendations["blueprint_cluster_binding"]["host_groups"] if hostgroup["name"] in hostGroups for host in hostgroup["hosts"] ]
    self.assertEquals(set(hostNames), pxfHosts)

  def test_createComponentLayoutRecommendations_pxf_add_service_wizard_to_be_installed(self):
    """ Test that PXF gets recommended correctly during Add Service Wizard, when PXF is selected for installation """

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
                            "component_category" : "MASTER",
                            "component_name" : "NAMENODE",
                            "is_master": "true",
                            "hostnames" : ["c6401.ambari.apache.org"]
                          }
                        },
                        {
                          "StackServiceComponents" : {
                            "cardinality" : "1+",
                            "component_category" : "SLAVE",
                            "component_name" : "DATANODE",
                            "hostnames" : ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
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

    pxfHosts = set(["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"])
    recommendations = self.stackAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostGroups = [ hostgroup["name"] for hostgroup in recommendations["blueprint"]["host_groups"] if {"name": "PXF"} in hostgroup["components"] ]
    hostNames = [ host["fqdn"] for hostgroup in recommendations["blueprint_cluster_binding"]["host_groups"] if hostgroup["name"] in hostGroups for host in hostgroup["hosts"] ]
    self.assertEquals(set(hostNames), pxfHosts)

  def test_createComponentLayoutRecommendations_pxf_add_service_wizard_already_installed(self):
    """ Test that PXF does not get recommended during Add Service Wizard, when PXF has already been installed """

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
                            "component_category" : "MASTER",
                            "component_name" : "NAMENODE",
                            "is_master": "true",
                            "hostnames" : ["c6401.ambari.apache.org"]
                          }
                        },
                        {
                          "StackServiceComponents" : {
                            "cardinality" : "1+",
                            "component_category" : "SLAVE",
                            "component_name" : "DATANODE",
                            "hostnames" : ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
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
                            "hostnames" : ["c6402.ambari.apache.org"]
                          }
                        }
                      ]
                    },
                    {
                      "StackServices" : {
                        "service_name" : "HBASE"
                      },
                      "components" : [
                        {
                          "StackServiceComponents" : {
                            "cardinality" : "1+",
                            "component_category" : "MASTER",
                            "component_name" : "HBASE_MASTER",
                            "is_master": "true",
                            "hostnames" : []
                          }
                        }
                      ]
                    }
                  ]
                }

    pxfHosts = set(["c6402.ambari.apache.org"])
    recommendations = self.stackAdvisor.createComponentLayoutRecommendations(services, hosts)
    hostGroups = [ hostgroup["name"] for hostgroup in recommendations["blueprint"]["host_groups"] if {"name": "PXF"} in hostgroup["components"] ]
    hostNames = [ host["fqdn"] for hostgroup in recommendations["blueprint_cluster_binding"]["host_groups"] if hostgroup["name"] in hostGroups for host in hostgroup["hosts"] ]
    self.assertEquals(set(hostNames), pxfHosts)

  def fqdn_mock_result(value=None):
      return 'c6401.ambari.apache.org' if value is None else value


  def __getHosts(self, componentsList, componentName):
    return [component["StackServiceComponents"] for component in componentsList if component["StackServiceComponents"]["component_name"] == componentName][0]


  def test_getComponentLayoutValidations_hawqsegment_not_co_located_with_datanode(self):
    """ Test validation warning for HAWQ segment not colocated with DATANODE """

    services = self.load_json("services-normal-hawq-3-hosts.json")
    hosts = self.load_json("hosts-3-hosts.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]

    hawqsegmentComponent = self.__getHosts(componentsList, "HAWQSEGMENT")
    hawqsegmentComponent["hostnames"] = ['c6401.ambari.apache.org']
    datanodeComponent = self.__getHosts(componentsList, "DATANODE")
    datanodeComponent["hostnames"] = ['c6402.ambari.apache.org']

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
    expected = {
      'type': 'host-component',
      'level': 'WARN',
      'component-name': 'HAWQSEGMENT',
      'message': 'HAWQ Segment must be installed on all DataNodes. The following 2 host(s) do not satisfy the colocation recommendation: c6401.ambari.apache.org, c6402.ambari.apache.org',
    }
    self.assertEquals(validations[0], expected)

    datanodeComponent["hostnames"] = ['c6401.ambari.apache.org']
    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
    self.assertEquals(len(validations), 0)


  def test_getComponentLayoutValidations_pxf_not_co_located_with_nn(self):
    """ Test warning is generated when PXF is not co-located with NAMENODE """

    services = self.load_json("services-hawq-pxf-hdfs.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    nameNodeComponent = self.__getHosts(componentsList, "NAMENODE")
    dataNodeComponent = self.__getHosts(componentsList, "DATANODE")
    pxfComponent = self.__getHosts(componentsList, "PXF")
    nameNodeComponent["hostnames"] = ["c6401.ambari.apache.org"]
    dataNodeComponent["hostnames"] = ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
    pxfComponent["hostnames"] = ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    validations = [validation for validation in self.stackAdvisor.getComponentLayoutValidations(services, hosts) if validation["component-name"] == "PXF"]
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
    nameNodeComponent = self.__getHosts(componentsList, "NAMENODE")
    dataNodeComponent = self.__getHosts(componentsList, "DATANODE")
    pxfComponent = self.__getHosts(componentsList, "PXF")
    nameNodeComponent["hostnames"] = ["c6401.ambari.apache.org"]
    dataNodeComponent["hostnames"] = ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
    pxfComponent["hostnames"] = ["c6401.ambari.apache.org"]

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    validations = [validation for validation in self.stackAdvisor.getComponentLayoutValidations(services, hosts) if validation["component-name"] == "PXF"]
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
    nameNodeComponent = self.__getHosts(componentsList, "NAMENODE")
    dataNodeComponent = self.__getHosts(componentsList, "DATANODE")
    pxfComponent = self.__getHosts(componentsList, "PXF")
    nameNodeComponent["hostnames"] = ["c6401.ambari.apache.org"]
    dataNodeComponent["hostnames"] = ["c6402.ambari.apache.org"]
    pxfComponent["hostnames"] = ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"]

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    validations = [validation for validation in self.stackAdvisor.getComponentLayoutValidations(services, hosts) if validation["component-name"] == "PXF"]
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
    nameNodeComponent = self.__getHosts(componentsList, "NAMENODE")
    dataNodeComponent = self.__getHosts(componentsList, "DATANODE")
    pxfComponent = self.__getHosts(componentsList, "PXF")
    nameNodeComponent["hostnames"] = ["c6401.ambari.apache.org"]
    dataNodeComponent["hostnames"] = ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
    pxfComponent["hostnames"] = ["c6401.ambari.apache.org", "c6402.ambari.apache.org", "c6403.ambari.apache.org"]

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    validations = [validation for validation in self.stackAdvisor.getComponentLayoutValidations(services, hosts) if validation["component-name"] == "PXF"]
    self.assertEquals(len(validations), 0)


  @patch('socket.getfqdn', side_effect=fqdn_mock_result)
  def test_getComponentLayoutValidations_hawq_3_Hosts(self, socket_mock):
    """ Test layout validations for HAWQ components on a 3-node cluster """

    # case-1: normal placement, no warnings
    services = self.load_json("services-normal-hawq-3-hosts.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    hawqMasterHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQMASTER"]
    hawqStandbyHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQSTANDBY"]
    self.assertEquals(len(hawqMasterHosts[0]), 1)
    self.assertEquals(len(hawqStandbyHosts[0]), 1)
    self.assertNotEquals(hawqMasterHosts[0][0], hawqStandbyHosts[0][0])

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
    self.assertEquals(len(validations), 0)

    # case-2: HAWQ masters are collocated
    services = self.load_json("services-master_standby_colo-3-hosts.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    hawqMasterHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQMASTER"]
    hawqStandbyHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQSTANDBY"]
    self.assertEquals(len(hawqMasterHosts[0]), 1)
    self.assertEquals(len(hawqStandbyHosts[0]), 1)
    self.assertEquals(hawqMasterHosts[0][0], hawqStandbyHosts[0][0])

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
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
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    hawqMasterHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQMASTER"]
    hawqStandbyHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQSTANDBY"]
    self.assertEquals(len(hawqMasterHosts[0]), 1)
    self.assertEquals(len(hawqStandbyHosts[0]), 1)
    self.assertNotEquals(hawqMasterHosts[0][0], hawqStandbyHosts[0][0])
    self.assertEquals(hawqMasterHosts[0][0], "c6401.ambari.apache.org")

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
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
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    hawqMasterHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQMASTER"]
    hawqStandbyHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQSTANDBY"]
    self.assertEquals(len(hawqMasterHosts[0]), 1)
    self.assertEquals(len(hawqStandbyHosts[0]), 1)
    self.assertNotEquals(hawqMasterHosts[0][0], hawqStandbyHosts[0][0])
    self.assertEquals(hawqStandbyHosts[0][0], "c6401.ambari.apache.org")

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
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
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    hawqMasterHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQMASTER"]
    hawqStandbyHosts = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQSTANDBY"]
    self.assertEquals(len(hawqMasterHosts), 0)
    self.assertEquals(len(hawqStandbyHosts), 0)

    hosts = self.load_json("hosts-3-hosts.json")
    hostsList = [host["Hosts"]["host_name"] for host in hosts["items"]]
    self.assertEquals(len(hostsList), 3)

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
    self.assertEquals(len(validations), 0)


  @patch('socket.getfqdn', side_effect=fqdn_mock_result)
  def test_getComponentLayoutValidations_sparkts_no_hive(self, socket_mock):
    """ Test SparkTS is picked when Hive is not installed """

    hosts = self.load_json("sparkts-host.json")
    services = self.load_json("services-sparkts.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]

    sparkTS = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "SPARK_THRIFTSERVER"]
    hiveMetaStore = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HIVE_METASTORE"]
    self.assertEquals(len(sparkTS), 1)
    self.assertEquals(len(hiveMetaStore), 0)

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
    expected = {'component-name': 'SPARK_THRIFTSERVER', 'message': 'SPARK_THRIFTSERVER requires HIVE_METASTORE to be selected/deployed.', 'type': 'host-component', 'level': 'ERROR'}
    self.assertEquals(validations[0], expected)


  @patch('socket.getfqdn', side_effect=fqdn_mock_result)
  def test_getComponentLayoutValidations_sparkts_with_hive(self, socket_mock):
    """ Test SparkTS is picked when Hive is installed """

    hosts = self.load_json("sparkts-host.json")
    services = self.load_json("services-sparkts-hive.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]

    sparkTS = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "SPARK_THRIFTSERVER"]
    hiveMetaStore = [component["StackServiceComponents"]["hostnames"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HIVE_METASTORE"]
    self.assertEquals(len(sparkTS), 1)
    self.assertEquals(len(hiveMetaStore), 1)

    validations = self.stackAdvisor.getComponentLayoutValidations(services, hosts)
    self.assertEquals(len(validations), 0)


  def test_recommendHDFSConfigurations(self):
    configurations = {}
    clusterData = {
      "totalAvailableRam": 2048,
      "hBaseInstalled": True,
      "hbaseRam": 112,
      "reservedRam": 128
    }
    services = {
      "services":
        [
          {
            "StackServices": {
              "service_name" : "HDFS",
              "service_version" : "2.6.0.2.2"
            }
          }
        ],
      "Versions": {
        "stack_version": "2.3"
      },
      "configurations": {
        "hdfs-site": {
          "properties": {
            "dfs.namenode.inode.attributes.provider.class": "org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer"
          }
        },
        "ranger-hdfs-plugin-properties": {
          "properties": {
            "ranger-hdfs-plugin-enabled": "No"
          }
        }
      }
    }

    # Test with Ranger HDFS plugin disabled
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['hdfs-site']['property_attributes']['dfs.namenode.inode.attributes.provider.class'], {'delete': 'true'}, "Test with Ranger HDFS plugin is disabled")

    # Test with Ranger HDFS plugin is enabled
    configurations['hdfs-site']['properties'] = {}
    configurations['hdfs-site']['property_attributes'] = {}
    services['configurations']['ranger-hdfs-plugin-properties']['properties']['ranger-hdfs-plugin-enabled'] = 'Yes'
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['hdfs-site']['properties']['dfs.namenode.inode.attributes.provider.class'], 'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer', "Test with Ranger HDFS plugin is enabled")

    # Test 1 for dfs.allow.truncate with no HAWQ and dfs.allow.truncate not set
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, None)
    self.assertTrue('dfs.allow.truncate' not in configurations['hdfs-site']['properties'])

    # Test 2 for dfs.allow.truncate with HAWQ and dfs.allow.truncate not set
    services["services"].append({"StackServices" : {"service_name" : "HAWQ"}, "components":[]})
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['hdfs-site']['properties']['dfs.allow.truncate'], 'true')

    # Test 3 for dfs.allow.truncate with no HAWQ and dfs.allow.truncate set to false
    services["services"].remove({"StackServices" : {"service_name" : "HAWQ"}, "components":[]})
    configurations['hdfs-site']['properties']['dfs.allow.truncate'] = 'false'
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['hdfs-site']['properties']['dfs.allow.truncate'], 'false')

    # Test 4 for dfs.allow.truncate with HAWQ and dfs.allow.truncate set to false
    services["services"].append({"StackServices" : {"service_name" : "HAWQ"}, "components":[]})
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['hdfs-site']['properties']['dfs.allow.truncate'], 'true')

  def test_recommendYARNConfigurations(self):
    configurations = {}
    servicesList = ["YARN"]
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
            ],
            "public_host_name" : "c6401.ambari.apache.org",
            "host_name" : "c6401.ambari.apache.org"
          }
        }
      ]
    }
    services = {
      "services" : [ {
        "StackServices":{
          "service_name": "YARN",
        },
        "Versions": {
          "stack_version": "2.3"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            }
          }
        ]
      }
      ],
      "configurations": {
        "yarn-site": {
          "properties": {
            "yarn.authorization-provider": "org.apache.ranger.authorization.yarn.authorizer.RangerYarnAuthorizer"
          }
        },
        "ranger-yarn-plugin-properties": {
          "properties": {
            "ranger-yarn-plugin-enabled": "No"
          }
        }
      }
    }

    clusterData = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, None)
    # Test with Ranger YARN plugin disabled
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['yarn-site']['property_attributes']['yarn.authorization-provider'], {'delete': 'true'}, "Test with Ranger HDFS plugin is disabled")

    # Test with Ranger YARN plugin is enabled
    configurations['yarn-site']['properties'] = {}
    configurations['yarn-site']['property_attributes'] = {}
    services['configurations']['ranger-yarn-plugin-properties']['properties']['ranger-yarn-plugin-enabled'] = 'Yes'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['yarn-site']['properties']['yarn.authorization-provider'], 'org.apache.ranger.authorization.yarn.authorizer.RangerYarnAuthorizer', "Test with Ranger YARN plugin enabled")


  def test_recommendKAFKAConfigurations(self):
    configurations = {}
    clusterData = {
      "totalAvailableRam": 2048,
      "hBaseInstalled": True,
      "hbaseRam": 112,
      "reservedRam": 128
    }
    services = {
      "services":
        [
          {
            "StackServices": {
              "service_name" : "KAFKA",
              "service_version" : "2.6.0.2.2"
            }
          },
          {
            "StackServices": {
              "service_name": "AMBARI_METRICS"
            },
            "components": [{
              "StackServiceComponents": {
                "component_name": "METRICS_COLLECTOR",
                "hostnames": ["host1"]
              }

            }, {
              "StackServiceComponents": {
                "component_name": "METRICS_MONITOR",
                "hostnames": ["host1"]
              }

            }]
          }
        ],
      "Versions": {
        "stack_version": "2.3"
      },
      "configurations": {
        "core-site": {
          "properties": { },
        },
        "kafka-broker": {
          "properties": {
            "authorizer.class.name" : "kafka.security.auth.SimpleAclAuthorizer"
          },
          "property_attributes": {}
        },
        "ranger-kafka-plugin-properties": {
          "properties": {
            "ranger-kafka-plugin-enabled": "No"
          }
        },
        "kafka-log4j": {
          "properties": {
            "content": "kafka.logs.dir=logs"
          }
        }
      }
    }

    # Test authorizer.class.name with Ranger Kafka plugin disabled in non-kerberos environment
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['kafka-broker']['property_attributes']['authorizer.class.name'], {'delete': 'true'}, "Test authorizer.class.name with Ranger Kafka plugin is disabled in non-kerberos environment")

    # Test authorizer.class.name with Ranger Kafka plugin disabled in kerberos environment
    configurations['kafka-broker']['properties'] = {}
    configurations['kafka-broker']['property_attributes'] = {}
    services['configurations']['kafka-broker']['properties']['security.inter.broker.protocol'] = 'PLAINTEXTSASL'
    services['configurations']['kafka-broker']['properties']['authorizer.class.name'] = 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer'
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['kafka-broker']['properties']['authorizer.class.name'], 'kafka.security.auth.SimpleAclAuthorizer' , "Test authorizer.class.name with Ranger Kafka plugin disabled in kerberos environment")

    # Test authorizer.class.name with Ranger Kafka plugin enabled in non-kerberos environment
    configurations['kafka-broker']['properties'] = {}
    configurations['kafka-broker']['property_attributes'] = {}
    del services['configurations']['kafka-broker']['properties']['security.inter.broker.protocol']
    services['configurations']['kafka-broker']['properties']['authorizer.class.name'] = 'kafka.security.auth.SimpleAclAuthorizer'
    services['configurations']['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'] = 'Yes'
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['kafka-broker']['properties']['authorizer.class.name'], 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer', "Test authorizer.class.name with Ranger Kafka plugin enabled in kerberos environment")

    # Test authorizer.class.name with Ranger Kafka plugin enabled in kerberos environment
    configurations['kafka-broker']['properties'] = {}
    configurations['kafka-broker']['property_attributes'] = {}
    services['configurations']['kafka-broker']['properties']['security.inter.broker.protocol'] = 'PLAINTEXTSASL'
    services['configurations']['kafka-broker']['properties']['authorizer.class.name'] = 'kafka.security.auth.SimpleAclAuthorizer'
    services['configurations']['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'] = 'Yes'
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['kafka-broker']['properties']['authorizer.class.name'], 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer', "Test authorizer.class.name with Ranger Kafka plugin enabled in kerberos environment")

    # Test kafka-log4j content when Ranger plugin for Kafka is enabled

    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    log4jContent = services['configurations']['kafka-log4j']['properties']['content']
    newRangerLog4content = "\nlog4j.appender.rangerAppender=org.apache.log4j.DailyRollingFileAppender\nlog4j.appender.rangerAppender.DatePattern='.'yyyy-MM-dd-HH\n" \
                     "log4j.appender.rangerAppender.File=${kafka.logs.dir}/ranger_kafka.log\nlog4j.appender.rangerAppender.layout" \
                     "=org.apache.log4j.PatternLayout\nlog4j.appender.rangerAppender.layout.ConversionPattern=%d{ISO8601} %p [%t] %C{6} (%F:%L) - %m%n\n" \
                     "log4j.logger.org.apache.ranger=INFO, rangerAppender"
    expectedLog4jContent = log4jContent + newRangerLog4content
    self.assertEquals(configurations['kafka-log4j']['properties']['content'], expectedLog4jContent, "Test kafka-log4j content when Ranger plugin for Kafka is enabled")

    # Test kafka.metrics.reporters when AMBARI_METRICS is present in services
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEqual(configurations['kafka-broker']['properties']['kafka.metrics.reporters'],
                                              'org.apache.hadoop.metrics2.sink.kafka.KafkaTimelineMetricsReporter')

  def test_recommendHBASEConfigurations(self):
    configurations = {}
    clusterData = {
      "totalAvailableRam": 2048,
      "hBaseInstalled": True,
      "hbaseRam": 112,
      "reservedRam": 128
    }
    expected = {
      "hbase-site": {
        "properties": {
          "hbase.bucketcache.size": "92160",
          "hbase.bucketcache.percentage.in.combinedcache": "1.0000",
          "hbase.regionserver.global.memstore.size": "0.4",
          "hfile.block.cache.size": "0.4",
          "hbase.coprocessor.region.classes": "org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint",
          "hbase.coprocessor.master.classes": "",
          "hbase.coprocessor.regionserver.classes": "",
          "hbase.rpc.controllerfactory.class": "org.apache.hadoop.hbase.ipc.controller.ServerRpcControllerFactory",
          "hbase.region.server.rpc.scheduler.factory.class": "org.apache.hadoop.hbase.ipc.PhoenixRpcSchedulerFactory",
          'hbase.regionserver.wal.codec': 'org.apache.hadoop.hbase.regionserver.wal.IndexedWALEditCodec',
          "hbase.bucketcache.ioengine": "offheap",
          "phoenix.functions.allowUserDefinedFunctions": "true"
        },
        "property_attributes": {
          "hbase.coprocessor.regionserver.classes": {
            "delete": "true"
          },
          "hbase.bucketcache.percentage.in.combinedcache": {
            "delete": "true"
          }
        }
      },
      "hbase-env": {
        "properties": {
          "hbase_master_heapsize": "1024",
          "hbase_max_direct_memory_size": "94208",
          "hbase_regionserver_heapsize": "20480"
        }
      }
    }
    services = {
      "services":
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
                  "host1"
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
      "Versions": {
        "stack_version": "2.3"
      },
      "configurations": {
        "yarn-site": {
          "properties": {
            "yarn.scheduler.minimum-allocation-mb": "256",
            "yarn.scheduler.maximum-allocation-mb": "2048"
            }
          },
        "hbase-env": {
          "properties": {
            "phoenix_sql_enabled": "true"
          }
        },
        "hbase-site": {
          "properties": {
            "hbase.coprocessor.regionserver.classes": ""
          }
        }
      }
    }

    # Test
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

    # Test
    clusterData['hbaseRam'] = '4'
    expected["hbase-site"]["property_attributes"]["hbase.bucketcache.size"] = {"delete": "true"}
    expected["hbase-site"]["property_attributes"]["hbase.bucketcache.ioengine"] = {"delete": "true"}
    expected["hbase-site"]["property_attributes"]["hbase.bucketcache.percentage.in.combinedcache"] = {"delete": "true"}
    expected["hbase-env"]["property_attributes"] = {"hbase_max_direct_memory_size" : {"delete": "true"}}
    expected["hbase-env"]["properties"]["hbase_master_heapsize"] = "1024"
    expected["hbase-env"]["properties"]["hbase_regionserver_heapsize"] = "4096"
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

    # Test - default recommendations should have certain configs deleted. HAS TO BE LAST TEST.
    services["configurations"] = {"hbase-site": {"properties": {"phoenix.functions.allowUserDefinedFunctions": '', "hbase.rpc.controllerfactory.class": '', "hbase.region.server.rpc.scheduler.factory.class": ''}}}
    configurations = {}
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['hbase-site']['property_attributes']['phoenix.functions.allowUserDefinedFunctions'], {'delete': 'true'})
    self.assertEquals(configurations['hbase-site']['property_attributes']['hbase.rpc.controllerfactory.class'], {'delete': 'true'})
    self.assertEquals(configurations['hbase-site']['property_attributes']['hbase.region.server.rpc.scheduler.factory.class'], {'delete': 'true'})
    self.assertEquals(configurations['hbase-site']['properties']['hbase.regionserver.wal.codec'], "org.apache.hadoop.hbase.regionserver.wal.WALCellCodec")


  def test_recommendHiveConfigurations(self):
    self.maxDiff = None
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "8192",
        },
      }
    }
    clusterData = {
      "cpu": 4,
      "mapMemory": 3000,
      "amMemory": 2000,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 256
    }
    expected = {
      'yarn-site': {
        'properties': {
          'yarn.scheduler.minimum-allocation-mb': '256',
          'yarn.scheduler.maximum-allocation-mb': '8192'
        }
      },
      'hive-env': {
        'properties': {
          'hive_exec_orc_storage_strategy': 'SPEED',
          'hive_security_authorization': 'None',
          'hive_timeline_logging_enabled': 'true',
          'hive_txn_acid': 'off'
        }
      },
      'hive-site': {
        'properties': {
          'hive.server2.enable.doAs': 'true',
          'hive.server2.tez.default.queues': "queue1,queue2",
          'hive.server2.tez.initialize.default.sessions': 'false',
          'hive.server2.tez.sessions.per.default.queue': '1',
          'hive.auto.convert.join.noconditionaltask.size': '268435456',
          'hive.compactor.initiator.on': 'false',
          'hive.compactor.worker.threads': '0',
          'hive.compute.query.using.stats': 'true',
          'hive.enforce.bucketing': 'false',
          'hive.exec.dynamic.partition.mode': 'strict',
          'hive.exec.failure.hooks': 'org.apache.hadoop.hive.ql.hooks.ATSHook',
          'hive.exec.orc.compression.strategy': 'SPEED',
          'hive.exec.orc.default.compress': 'ZLIB',
          'hive.exec.orc.default.stripe.size': '67108864',
          'hive.exec.orc.encoding.strategy': 'SPEED',
          'hive.exec.post.hooks': 'org.apache.hadoop.hive.ql.hooks.ATSHook',
          'hive.exec.pre.hooks': 'org.apache.hadoop.hive.ql.hooks.ATSHook',
          'hive.exec.reducers.bytes.per.reducer': '67108864',
          'hive.execution.engine': 'mr',
          'hive.optimize.index.filter': 'true',
          'hive.optimize.sort.dynamic.partition': 'false',
          'hive.prewarm.enabled': 'false',
          'hive.prewarm.numcontainers': '3',
          'hive.security.authorization.enabled': 'false',
          'hive.server2.use.SSL': 'false',
          'hive.stats.fetch.column.stats': 'true',
          'hive.stats.fetch.partition.stats': 'true',
          'hive.support.concurrency': 'false',
          'hive.tez.auto.reducer.parallelism': 'true',
          'hive.tez.container.size': '768',
          'hive.tez.dynamic.partition.pruning': 'true',
          'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps',
          'hive.txn.manager': 'org.apache.hadoop.hive.ql.lockmgr.DummyTxnManager',
          'hive.vectorized.execution.enabled': 'true',
          'hive.vectorized.execution.reduce.enabled': 'false',
          'hive.security.metastore.authorization.manager': 'org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider',
          'hive.security.authorization.manager': 'org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdConfOnlyAuthorizerFactory'
        },
       'property_attributes': {
         'hive.auto.convert.join.noconditionaltask.size': {'maximum': '805306368'},
         'hive.server2.authentication.pam.services': {'delete': 'true'},
         'hive.server2.custom.authentication.class': {'delete': 'true'},
         'hive.server2.authentication.kerberos.principal': {'delete': 'true'},
         'hive.server2.authentication.kerberos.keytab': {'delete': 'true'},
         'hive.server2.authentication.ldap.url': {'delete': 'true'},
         'hive.server2.tez.default.queues': {
           'entries': [{'value': 'queue1', 'label': 'queue1 queue'}, {'value': 'queue2', 'label': 'queue2 queue'}]
          },
         'atlas.cluster.name': {'delete': 'true'},
         'atlas.rest.address': {'delete': 'true'},
         'datanucleus.rdbms.datastoreAdapterClassName': {'delete': 'true'}
        }
      },
      'hiveserver2-site': {
        'properties': {
        },
        'property_attributes': {
         'hive.security.authorization.manager': {'delete': 'true'},
         'hive.security.authenticator.manager': {'delete': 'true'}
        }
      }
    }
    services = {
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/YARN",
          "StackServices": {
            "service_name": "YARN",
            "service_version": "2.6.0.2.2",
            "stack_name": "HDP",
            "stack_version": "2.2"
          },
          "components": [
            {
              "StackServiceComponents": {
                "advertise_version": "false",
                "cardinality": "1",
                "component_category": "MASTER",
                "component_name": "APP_TIMELINE_SERVER",
                "display_name": "App Timeline Server",
                "is_client": "false",
                "is_master": "true",
                "hostnames": []
              },
              "dependencies": []
            },
            {
              "StackServiceComponents": {
                "advertise_version": "true",
                "cardinality": "1+",
                "component_category": "SLAVE",
                "component_name": "NODEMANAGER",
                "display_name": "NodeManager",
                "is_client": "false",
                "is_master": "false",
                "hostnames": [
                  "c6403.ambari.apache.org"
                ]
              },
              "dependencies": []
            },
            {
              "StackServiceComponents": {
                "advertise_version": "true",
                "cardinality": "1-2",
                "component_category": "MASTER",
                "component_name": "RESOURCEMANAGER",
                "display_name": "ResourceManager",
                "is_client": "false",
                "is_master": "true",
                "hostnames": []
              },
              "dependencies": []
            },
            {
              "StackServiceComponents": {
                "advertise_version": "true",
                "cardinality": "1+",
                "component_category": "CLIENT",
                "component_name": "YARN_CLIENT",
                "display_name": "YARN Client",
                "is_client": "true",
                "is_master": "false",
                "hostnames": []
              },
              "dependencies": []
            }
          ]
        },
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler" :"yarn.scheduler.capacity.root.queues=queue1,queue2"
          }
        },
        "hive-env": {
          "properties": {
          }
        },
        "hive-site": {
          "properties": {
            "hive.server2.authentication": "none",
            "hive.server2.authentication.ldap.url": "",
            "hive.server2.authentication.ldap.baseDN": "",
            "hive.server2.authentication.kerberos.keytab": "",
            "hive.server2.authentication.kerberos.principal": "",
            "hive.server2.authentication.pam.services": "",
            "hive.server2.custom.authentication.class": "",
            "hive.cbo.enable": "true"
          }
        },
        "hiveserver2-site": {
          "properties": {
            "hive.security.authorization.manager": "",
            "hive.security.authenticator.manager": ""
          }
        }
      },
      "changed-configurations": [ ]

    }
    hosts = {
      "items" : [
        {
          "href" : "/api/v1/hosts/c6401.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6401.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6401.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        },
        {
          "href" : "/api/v1/hosts/c6402.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6402.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6402.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        },
        {
          "href" : "/api/v1/hosts/c6403.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6403.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6403.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }
      ]
    }

    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Test JDK1.7
    services['ambari-server-properties'] = {'java.home': '/usr/jdk64/jdk1.7.3_23'}
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Test JDK1.8
    services['ambari-server-properties'] = {'java.home': '/usr/jdk64/jdk1.8_44'}
    expected['hive-site']['properties']['hive.tez.java.opts'] = "-server -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseG1GC -XX:+ResizeTLAB -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps"
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Test JDK1.9
    services['ambari-server-properties'] = {'java.home': '/usr/jdk64/jdk1.9.2_44'}
    expected['hive-site']['properties']['hive.tez.java.opts'] = "-server -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseG1GC -XX:+ResizeTLAB -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps"
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

  @patch('os.path.exists')
  @patch('os.path.isdir')
  @patch('os.listdir')
  def test_recommendTezConfigurations(self, os_listdir_mock, os_isdir_mock, os_exists_mock):

    os_exists_mock.return_value = True
    os_isdir_mock.return_value = True
    os_listdir_mock.return_value = ['TEZ{0.7.0.2.3.0.0-2155}']

    self.maxDiff = None
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "8192",
        },
      },
      "capacity-scheduler": {
        "properties": {
          "yarn.scheduler.capacity.root.queues": "queue1,queue2"
        }
      }
    }
    clusterData = {
      "cpu": 4,
      "mapMemory": 3000,
      "amMemory": 2000,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 256
    }
    expected = {
      "capacity-scheduler": {
        "properties": {
          "yarn.scheduler.capacity.root.queues": "queue1,queue2"
        }
      },
      "tez-site": {
        "properties": {
          "tez.task.resource.memory.mb": "768",
          "tez.am.launch.cmd-opts": "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA -XX:+UseParallelGC",
          "tez.task.launch.cmd-opts": "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA -XX:+UseParallelGC",
          "tez.runtime.io.sort.mb": "307",
          "tez.session.am.dag.submit.timeout.secs": "600",
          "tez.runtime.unordered.output.buffer.size-mb": "57",
          "tez.am.resource.memory.mb": "4000"
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "8192"
        }
      }
    }
    services = {
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/YARN",
          "StackServices": {
            "service_name": "YARN",
            "service_version": "2.6.0.2.2",
            "stack_name": "HDP",
            "stack_version": "2.2"
          },
          "components": [
            {
              "StackServiceComponents": {
                "advertise_version": "false",
                "cardinality": "1",
                "component_category": "MASTER",
                "component_name": "APP_TIMELINE_SERVER",
                "display_name": "App Timeline Server",
                "is_client": "false",
                "is_master": "true",
                "hostnames": []
              },
              "dependencies": []
            },
            {
              "StackServiceComponents": {
                "advertise_version": "true",
                "cardinality": "1+",
                "component_category": "SLAVE",
                "component_name": "NODEMANAGER",
                "display_name": "NodeManager",
                "is_client": "false",
                "is_master": "false",
                "hostnames": [
                  "c6403.ambari.apache.org"
                ]
              },
              "dependencies": []
            },
            {
              "StackServiceComponents": {
                "advertise_version": "true",
                "cardinality": "1-2",
                "component_category": "MASTER",
                "component_name": "RESOURCEMANAGER",
                "display_name": "ResourceManager",
                "is_client": "false",
                "is_master": "true",
                "hostnames": []
              },
              "dependencies": []
            },
            {
              "StackServiceComponents": {
                "advertise_version": "true",
                "cardinality": "1+",
                "component_category": "CLIENT",
                "component_name": "YARN_CLIENT",
                "display_name": "YARN Client",
                "is_client": "true",
                "is_master": "false",
                "hostnames": []
              },
              "dependencies": []
            }
          ]
        },
      ],
      "configurations": configurations,
      "changed-configurations": [ ],
      "ambari-server-properties": {}
    }
    hosts = {
      "items" : [
        {
          "href" : "/api/v1/hosts/c6401.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6401.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6401.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        },
        {
          "href" : "/api/v1/hosts/c6402.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6402.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6402.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        },
        {
          "href" : "/api/v1/hosts/c6403.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : "c6403.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6403.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }
      ]
    }

    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    server_host = socket.getfqdn()
    tez_ui_url =  "http://" + server_host + ":8080/#/main/views/TEZ/0.7.0.2.3.0.0-2155/TEZ_CLUSTER_INSTANCE"

    # Test JDK1.7
    services['ambari-server-properties'] = {'java.home': '/usr/jdk64/jdk1.7.3_23'}
    expected['tez-site']['properties']['tez.tez-ui.history-url.base'] = tez_ui_url
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Test JDK1.8
    services['ambari-server-properties'] = {'java.home': '/usr/jdk64/jdk1.8_44'}
    expected['tez-site']['properties']['tez.am.launch.cmd-opts'] = "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA -XX:+UseG1GC -XX:+ResizeTLAB"
    expected['tez-site']['properties']['tez.task.launch.cmd-opts'] = "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA -XX:+UseG1GC -XX:+ResizeTLAB"
    expected['tez-site']['properties']['tez.tez-ui.history-url.base'] = tez_ui_url
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Test JDK1.9
    services['ambari-server-properties'] = {'java.home': '/usr/jdk64/jdk1.9.2_44'}
    expected['tez-site']['properties']['tez.am.launch.cmd-opts'] = "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA -XX:+UseG1GC -XX:+ResizeTLAB"
    expected['tez-site']['properties']['tez.task.launch.cmd-opts'] = "-XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps -XX:+UseNUMA -XX:+UseG1GC -XX:+ResizeTLAB"
    expected['tez-site']['properties']['tez.tez-ui.history-url.base'] = tez_ui_url
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

  def test_recommendHAWQConfigurations(self):

    # original cluster data with 3 segments
    services = self.load_json("services-normal-hawq-3-hosts.json")
    componentsListList = [service["components"] for service in services["services"]]
    componentsList = [item for sublist in componentsListList for item in sublist]
    hawqSegmentComponent = [component["StackServiceComponents"] for component in componentsList if component["StackServiceComponents"]["component_name"] == "HAWQSEGMENT"][0]

    # setup default configuration values
    services["configurations"]["hawq-site"] = {"properties": {"default_hash_table_bucket_number": "24",
                                                              "hawq_rm_nvseg_perquery_limit": "512",
                                                              "hawq_rm_yarn_address": "localhost:8032",
                                                              "hawq_rm_yarn_scheduler_address": "localhost:8030"}}

    services["configurations"]["hdfs-client"] = {"properties": {"output.replace-datanode-on-failure": "true"}}

    services["configurations"]["yarn-site"] = {"properties": {"yarn.resourcemanager.address": "host1:8050",
                                                              "yarn.resourcemanager.scheduler.address": "host1:8030"}}
    services["services"].append({"StackServices" : {"service_name" : "YARN"}, "components":[]})
    configurations = {}
    clusterData = {}

    # Test 1 - with 3 segments
    self.assertEquals(len(hawqSegmentComponent["hostnames"]), 3)
    self.stackAdvisor.recommendHAWQConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations["hawq-site"]["properties"]["default_hash_table_bucket_number"], str(3 * 6))
    self.assertEquals(configurations["hdfs-client"]["properties"]["output.replace-datanode-on-failure"], "false")

    # check derived properties
    self.assertEquals(configurations["hawq-site"]["properties"]["hawq_rm_yarn_address"], "host1:8050")
    self.assertEquals(configurations["hawq-site"]["properties"]["hawq_rm_yarn_scheduler_address"], "host1:8030")

    # Test 2 - with 100 segments
    hawqSegmentComponent["hostnames"] = ["host" + str(i) for i in range(100)]
    self.stackAdvisor.recommendHAWQConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations["hawq-site"]["properties"]["default_hash_table_bucket_number"], str(100 * 5))
    self.assertEquals(configurations["hdfs-client"]["properties"]["output.replace-datanode-on-failure"], "true")

    # Test 3 - with 512 segments
    hawqSegmentComponent["hostnames"] = ["host" + str(i) for i in range(512)]
    self.stackAdvisor.recommendHAWQConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations["hawq-site"]["properties"]["default_hash_table_bucket_number"], "512")
    self.assertEquals(configurations["hdfs-client"]["properties"]["output.replace-datanode-on-failure"], "true")

    # Test 4 - with 513 segments
    hawqSegmentComponent["hostnames"] = ["host" + str(i) for i in range(513)]
    self.stackAdvisor.recommendHAWQConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations["hawq-site"]["properties"]["default_hash_table_bucket_number"], "512")
    self.assertEquals(configurations["hdfs-client"]["properties"]["output.replace-datanode-on-failure"], "true")

    # Test 5 - with no segments
    configurations = {}
    services["configurations"]["hawq-site"] = {"properties":{'hawq-site': {'properties': {}}}}
    hawqSegmentComponent["hostnames"] = []
    self.stackAdvisor.recommendHAWQConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, {'hdfs-client': {'properties': {'output.replace-datanode-on-failure': 'false'}},
                                       'hawq-site': {'properties': {}}})

  def test_validateHiveConfigurations(self):
    properties = {"hive_security_authorization": "None",
                  "hive.exec.orc.default.stripe.size": "8388608",
                  'hive.tez.container.size': '2048',
                  'hive.tez.java.opts': '-server -Xmx546m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps',
                  'hive.auto.convert.join.noconditionaltask.size': '1100000000'}
    recommendedDefaults = {'hive.tez.container.size': '1024',
                           'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps',
                           'hive.auto.convert.join.noconditionaltask.size': '1000000000'}
    configurations = {
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true", 'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true'}
      },
      "hive-env": {
        "properties": {"hive_security_authorization": "None"}
      }
    }
    services = {
      "services": []
    }

    # Test for 'ranger-hive-plugin-properties' not being in configs
    res_expected = []
    res = self.stackAdvisor.validateHiveConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

  # This test intentionally calls all validate methods with
  # incorrect parameters (empty configs)
  def test_noRiskyDictLookups(self):
    properties = {}
    recommendedDefaults = {}
    configurations = {"core-site": {"properties": {}}}
    services = {
      "services": [],
      "Versions": {
        "stack_name": "HDP",
        "stack_version": "2.3"
      },
      "configurations": configurations
    }

    hosts = {
      "items" : [
        {
          "href" : "/api/v1/hosts/c6401.ambari.apache.org",
          "Hosts" : {
            "cpu_count" : 1,
            "disk_info" : [
              {
                "available" : "4564632",
                "used" : "5230344",
                "percent" : "54%",
                "size" : "10319160",
                "type" : "ext4",
                "mountpoint" : "/"
              },
              {
                "available" : "1832436",
                "used" : "0",
                "percent" : "0%",
                "size" : "1832436",
                "type" : "tmpfs",
                "mountpoint" : "/dev/shm"
              }
            ],
            "host_name" : "c6401.ambari.apache.org",
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : "c6401.ambari.apache.org",
            "rack_info" : "/default-rack",
            "total_mem" : 1922680
          }
        }
      ]
    }

    def return_c6401_hostname(services, service_name, component_name):
      return ["c6401.ambari.apache.org"]
    self.stackAdvisor.getComponentHostNames = return_c6401_hostname

    validators = self.stackAdvisor.getServiceConfigurationValidators()

    # Setting up empty configs and services info
    for serviceName, validator in validators.items():
      services["services"].extend([{"StackServices": {"service_name": serviceName},
                                    "components": []}])
      for siteName in validator.keys():
        configurations[siteName] = {"properties": {}}

    # Emulate enabled RANGER
    services["services"].extend([{"StackServices": {"service_name": "RANGER"},
                                "components": []}])
    configurations["ranger-hbase-plugin-properties"] = {
      "ranger-hbase-plugin-enabled": "Yes"
    }

    exceptionThrown = False
    try:
      recommendations = self.stackAdvisor.recommendConfigurations(services, hosts)
    except Exception as e:
      exceptionThrown = True
    self.assertTrue(exceptionThrown)

    pass

  def test_recommendRangerConfigurations(self):
    clusterData = {}
    # Recommend for not existing DB_FLAVOR and http enabled, HDP-2.3
    services = {
      "Versions" : {
        "stack_version" : "2.3",
        },
      "services":  [
        {
          "StackServices": {
            "service_name": "RANGER",
            "service_version": "0.5.0.2.3"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "RANGER_ADMIN",
                "hostnames": ["host1"]
              }
            }
          ]
        },
        ],
      "configurations": {
        "admin-properties": {
          "properties": {
            "DB_FLAVOR": "NOT_EXISTING",
            }
        },
        "ranger-admin-site": {
          "properties": {
            "ranger.service.http.port": "7777",
            "ranger.service.http.enabled": "true",
            }
        }
      },
      "ambari-server-properties": {
        "ambari.ldap.isConfigured" : "true",
        "authentication.ldap.bindAnonymously" : "false",
        "authentication.ldap.baseDn" : "dc=apache,dc=org",
        "authentication.ldap.groupNamingAttr" : "cn",
        "authentication.ldap.primaryUrl" : "c6403.ambari.apache.org:389",
        "authentication.ldap.userObjectClass" : "posixAccount",
        "authentication.ldap.secondaryUrl" : "c6403.ambari.apache.org:389",
        "authentication.ldap.usernameAttribute" : "uid",
        "authentication.ldap.dnAttribute" : "dn",
        "authentication.ldap.useSSL" : "false",
        "authentication.ldap.managerPassword" : "/etc/ambari-server/conf/ldap-password.dat",
        "authentication.ldap.groupMembershipAttr" : "memberUid",
        "authentication.ldap.groupObjectClass" : "posixGroup",
        "authentication.ldap.managerDn" : "uid=hdfs,ou=people,ou=dev,dc=apache,dc=org"
      }
    }

    expected = {
      'admin-properties': {
        'properties': {
          'policymgr_external_url': 'http://host1:7777',
          'SQL_CONNECTOR_JAR': '/usr/share/java/mysql-connector-java.jar'
        }
      },
      'ranger-ugsync-site': {
        'properties': {
          'ranger.usersync.group.objectclass': 'posixGroup',
          'ranger.usersync.group.nameattribute': 'cn',
          'ranger.usersync.group.memberattributename': 'memberUid',
          'ranger.usersync.ldap.binddn': 'uid=hdfs,ou=people,ou=dev,dc=apache,dc=org',
          'ranger.usersync.ldap.user.nameattribute': 'uid',
          'ranger.usersync.ldap.user.objectclass': 'posixAccount',
          'ranger.usersync.ldap.url': 'ldap://c6403.ambari.apache.org:389',
          'ranger.usersync.ldap.searchBase': 'dc=apache,dc=org'
        }
      },
      'ranger-admin-site': {
        'properties': {
          "ranger.audit.solr.zookeepers": "NONE"
        }
      },
      'ranger-env': {
        'properties': {
          'ranger-storm-plugin-enabled': 'No',
        }
      }
    }

    recommendedConfigurations = {}
    self.stackAdvisor.recommendRangerConfigurations(recommendedConfigurations, clusterData, services, None)
    self.assertEquals(recommendedConfigurations, expected)

    # Recommend ranger.audit.solr.zookeepers when solrCloud is disabled
    services['configurations']['ranger-env'] = {
      "properties": {
        "is_solrCloud_enabled": "false"
      }
    }

    recommendedConfigurations = {}
    self.stackAdvisor.recommendRangerConfigurations(recommendedConfigurations, clusterData, services, None)
    self.assertEquals(recommendedConfigurations['ranger-admin-site']['properties']['ranger.audit.solr.zookeepers'], 'NONE')

  def test_validateHDFSConfigurations(self):
    properties = {}
    recommendedDefaults = {}
    configurations = {
      "core-site": {"properties": {}},
    }
    services = self.load_json("services-hawq-3-hosts.json")
    services["configurations"]["hdfs-site"] = {}
    services["configurations"]["hdfs-site"]["properties"] = {}
    hosts = {}

    expected_warning = {
        'config-type':'hdfs-site',
        'message':'HAWQ requires dfs.allow.truncate in hdfs-site.xml set to True.',
        'type':'configuration',
        'config-name':'dfs.allow.truncate',
        'level':'WARN'
    }

    # Test following cases:
    # when HAWQ is being installed and dfs.allow.truncate is not set at all, warning
    # when HAWQ is being installed and dfs.allow.truncate is set to True, no warning
    # when HAWQ is being installed and dfs.allow.truncate is not set to True, warning
    # when HAWQ is not installed and dfs.allow.truncate is not set at all, no warning
    # when HAWQ is not installed and dfs.allow.truncate is set to True, no warning
    # when HAWQ is not installed and dfs.allow.truncate is not set to True, no warning
    # 1
    problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    self.assertEqual(problems[0], expected_warning)

    # 2
    services["configurations"]["hdfs-site"]["properties"]["dfs.allow.truncate"] = "True"
    problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)

    # 3
    services["configurations"]["hdfs-site"]["properties"]["dfs.allow.truncate"] = "false"
    problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    self.assertEqual(problems[0], expected_warning)

    del services["configurations"]["hdfs-site"]["properties"]["dfs.allow.truncate"]
    servicesElementWithoutHAWQ = [service for service in services["services"] if service["StackServices"]["service_name"] != "HAWQ"]
    services["services"] = servicesElementWithoutHAWQ

    # 4
    problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)

    # 5
    services["configurations"]["hdfs-site"]["properties"]["dfs.allow.truncate"] = "True"
    problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)

    # 6
    services["configurations"]["hdfs-site"]["properties"]["dfs.allow.truncate"] = "false"
    problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)


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

    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    problems_dict = {}
    for problem in problems:
      problems_dict[problem['config-name']] = problem
    self.assertEqual(len(problems), 2)
    self.assertEqual(problems_dict, expected_warnings)

    # Test hawq_master_directory multiple directories validation
    configurations["hawq-site"] = {"properties": {"hawq_master_directory": "/data/hawq/master",
                                                  "hawq_segment_directory": "/data/hawq/segment"}}
    properties = configurations["hawq-site"]["properties"]
    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    problems_dict = {}
    self.assertEqual(len(problems), 0)
    expected_warnings = {}
    self.assertEqual(problems_dict, expected_warnings)

    configurations["hawq-site"] = {"properties": {"hawq_master_directory": "/data/hawq/master1,/data/hawq/master2",
                                                  "hawq_segment_directory": "/data/hawq/segment1 /data/hawq/segment2"}}
    properties = configurations["hawq-site"]["properties"]
    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
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
    """
    Validation error expected is as below:
                    [ {
                          "config-type": "hawq-site",
                          "message": "hawq_global_rm_type must be set to none if YARN service is not installed",
                          "type": "configuration",
                          "config-name": "hawq_global_rm_type",
                          "level": "ERROR"
                    } ]
    """
    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, services["configurations"], services, hosts)
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

    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, services["configurations"], services, hosts)
    self.assertEqual(len(problems), 0)

    # Test HAWQ Master port conflict with Ambari Server Postgres port

    # case 1: HAWQ Master is placed on Ambari Server and HAWQ Master port is same as Ambari Server Postgres Port
    self.stackAdvisor.isHawqMasterComponentOnAmbariServer = MagicMock(return_value=True)
    configurations = {
      "hawq-site": {
        "properties":
          {"hawq_master_address_port": "5432"}
      }
    }
    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
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
    self.stackAdvisor.isHawqMasterComponentOnAmbariServer = MagicMock(return_value=True)
    configurations["hawq-site"]["properties"]["hawq_master_address_port"] = "10432"
    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)

    # case 3: HAWQ Master is not placed on Ambari Server and HAWQ Master port is same as  Ambari Server Postgres Port
    self.stackAdvisor.isHawqMasterComponentOnAmbariServer = MagicMock(return_value=False)
    configurations["hawq-site"]["properties"]["hawq_master_address_port"] = "5432"
    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 0)

    # case 4: HAWQ Master is not placed on Ambari Server and HAWQ Master port is different from  Ambari Server Postgres Port
    self.stackAdvisor.isHawqMasterComponentOnAmbariServer = MagicMock(return_value=False)
    configurations["hawq-site"]["properties"]["hawq_master_address_port"] = "10432"
    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
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
    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    self.assertEqual(problems[0], expected)

    configurations["hawq-site"] = {"properties": {"default_hash_table_bucket_number": "500",
                                                  "hawq_rm_nvseg_perquery_limit": "500"}}
    properties = configurations["hawq-site"]["properties"]
    problems = self.stackAdvisor.validateHAWQSiteConfigurations(properties, defaults, configurations, services, hosts)
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

    problems = self.stackAdvisor.validateHAWQHdfsClientConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    self.assertEqual(problems[0], expected)

    # 2. Try with 3 hosts
    services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"] = ["host1", "host2", "host3"]
    problems = self.stackAdvisor.validateHAWQHdfsClientConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    self.assertEqual(problems[0], expected)

    # 3. Try with 4 hosts - default value
    services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"] = ["host1", "host2", "host3", "host4"]
    problems = self.stackAdvisor.validateHAWQHdfsClientConfigurations(properties, defaults, configurations, services, hosts)
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
    problems = self.stackAdvisor.validateHAWQHdfsClientConfigurations(properties, defaults, configurations, services, hosts)
    self.assertEqual(len(problems), 1)
    self.assertEqual(problems[0], expected)
