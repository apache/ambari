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

import os
from unittest import TestCase
from mock.mock import patch, MagicMock


class TestHDP22StackAdvisor(TestCase):

  def setUp(self):
    import imp

    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    hdp206StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.0.6/services/stack_advisor.py')
    hdp21StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.1/services/stack_advisor.py')
    hdp22StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.2/services/stack_advisor.py')
    hdp22StackAdvisorClassName = 'HDP22StackAdvisor'
    with open(stackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp206StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp206StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp21StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp21StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp22StackAdvisorPath, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp22StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    clazz = getattr(stack_advisor_impl, hdp22StackAdvisorClassName)
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

  def test_recommendTezConfigurations(self):
    configurations = {}
    clusterData = {
      "mapMemory": 3000,
      "amMemory": 2000,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 256
    }
    expected = {
      "tez-site": {
        "properties": {
          "tez.am.resource.memory.mb": "4000",
          "tez.task.resource.memory.mb": "768",
          "tez.runtime.io.sort.mb": "307",
          "tez.runtime.unordered.output.buffer.size-mb": "57"
        }
      }
    }
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, None, None)
    self.assertEquals(configurations, expected)

  def test_recommendTezConfigurations_amMemoryMoreThan3072(self):
    configurations = {}
    clusterData = {
      "mapMemory": 4000,
      "amMemory": 3100,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 256
    }
    expected = {
      "tez-site": {
        "properties": {
          "tez.am.resource.memory.mb": "3100",
          "tez.task.resource.memory.mb": "768",
          "tez.runtime.io.sort.mb": "307",
          "tez.runtime.unordered.output.buffer.size-mb": "57"
        }
      }
    }
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, None, None)
    self.assertEquals(configurations, expected)

  def test_recommendTezConfigurations_mapMemoryLessThan768(self):
    configurations = {}
    clusterData = {
      "mapMemory": 760,
      "amMemory": 2000,
      "reduceMemory": 760,
      "containers": 3,
      "ramPerContainer": 256
    }
    expected = {
      "tez-site": {
        "properties": {
          "tez.am.resource.memory.mb": "4000",
          "tez.task.resource.memory.mb": "760",
          "tez.runtime.io.sort.mb": "304",
          "tez.runtime.unordered.output.buffer.size-mb": "57"
        }
      }
    }
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, None, None)
    self.assertEquals(configurations, expected)


  def test_validateHDFSConfigurations(self):
    self.maxDiff = None
    recommendedDefaults = None

    unsecure_cluster_core_site = {
      'hadoop.security.authentication': 'simple',
      'hadoop.security.authorization': 'false',
    }

    secure_cluster_core_site = {
      'hadoop.security.authentication': 'kerberos',
      'hadoop.security.authorization': 'true',
    }

    # TEST CASE: Unsecured cluster, secure ports
    properties = {  # hdfs-site
                    'dfs.datanode.address': '0.0.0.0:1019',
                    'dfs.datanode.http.address': '0.0.0.0:1022',
    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
      },
      'core-site': {
        'properties': unsecure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties':{
          'properties': {'ranger-hdfs-plugin-enabled':'Yes'}
      }
    }
    services = {"services":
                    [{"StackServices":
                          {"service_name" : "HDFS",
                           "service_version" : "2.6.0.2.2",
                           }
                     }]
                }
    expected = []  # No warnings
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Unsecured cluster, unsecure ports
    properties = {  # hdfs-site
                    'dfs.datanode.address': '0.0.0.0:55555',
                    'dfs.datanode.http.address': '0.0.0.0:55555',
                    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
        },
      'core-site': {
        'properties': unsecure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = []  # No warnings
    services = {"services":
                [{"StackServices":
                      {"service_name" : "HDFS",
                       "service_version" : "2.6.0.2.2",
                       }
                 }]
            }
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, invalid dfs.http.policy value
    properties = {  # hdfs-site
                    'dfs.http.policy': 'WRONG_VALUE',
                    'dfs.datanode.address': '0.0.0.0:1019',
                    'dfs.datanode.http.address': '0.0.0.0:1022',
    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
      },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = [{'config-name': 'dfs.http.policy',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "Invalid property value: WRONG_VALUE. Valid values are ['HTTP_ONLY', 'HTTPS_ONLY', 'HTTP_AND_HTTPS']",
                 'type': 'configuration'}]
    services = {"services":
            [{"StackServices":
                  {"service_name" : "HDFS",
                   "service_version" : "2.6.0.2.2",
                   }
             }]
        }
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, https address not defined
    properties = {  # hdfs-site
                    'dfs.http.policy': 'HTTPS_ONLY',
                    'dfs.datanode.address': '0.0.0.0:1019',
                    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
        },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = [ ]
    services = {"services":
            [{"StackServices":
                  {"service_name" : "HDFS",
                   "service_version" : "2.6.0.2.2",
                   }
             }]
        }
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, https address defined and secure
    properties = {  # hdfs-site
                    'dfs.http.policy': 'HTTPS_ONLY',
                    'dfs.datanode.address': '0.0.0.0:1019',
                    'dfs.datanode.https.address': '0.0.0.0:1022',
                    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
        },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = []
    services = {"services":
            [{"StackServices":
                  {"service_name" : "HDFS",
                   "service_version" : "2.6.0.2.2",
                   }
             }]
        }
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, https address defined and non secure
    properties = {  # hdfs-site
                    'dfs.http.policy': 'HTTPS_ONLY',
                    'dfs.datanode.address': '0.0.0.0:1019',
                    'dfs.datanode.https.address': '0.0.0.0:50475',
                  }
    configurations = {
      'hdfs-site': {
        'properties': properties,
      },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = []
    services = {"services":
            [{"StackServices":
                  {"service_name" : "HDFS",
                   "service_version" : "2.6.0.2.2",
                   }
             }]
        }
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, non secure dfs port, https property not defined
    properties = {  # hdfs-site
                    'dfs.http.policy': 'HTTPS_ONLY',
                    'dfs.datanode.address': '0.0.0.0:50010',
                 }
    configurations = {
      'hdfs-site': {
        'properties': properties,
      },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }

    }
    expected = [{'config-name': 'dfs.datanode.address',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "You set up datanode to use some non-secure ports. "
                            "If you want to run Datanode under non-root user in "
                            "a secure cluster, you should set all these properties "
                            "['dfs.datanode.address', 'dfs.datanode.https.address'] "
                            "to use non-secure ports (if property "
                            "dfs.datanode.https.address does not exist, just add it). "
                            "You may also set up property dfs.data.transfer.protection "
                            "('authentication' is a good default value). Also, set up "
                            "WebHDFS with SSL as described in manual in order to "
                            "be able to use HTTPS.",
                 'type': 'configuration'},
                {'config-name': 'dfs.datanode.https.address',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "You set up datanode to use some non-secure ports. "
                            "If you want to run Datanode under non-root user in "
                            "a secure cluster, you should set all these properties "
                            "['dfs.datanode.address', 'dfs.datanode.https.address'] "
                            "to use non-secure ports (if property dfs.datanode.https.address "
                            "does not exist, just add it). You may also set up property "
                            "dfs.data.transfer.protection ('authentication' is a good default value). "
                            "Also, set up WebHDFS with SSL as described in manual in "
                            "order to be able to use HTTPS.",
                 'type': 'configuration'}
    ]
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)


    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, non secure dfs port, https defined and secure
    properties = {  # hdfs-site
                    'dfs.http.policy': 'HTTPS_ONLY',
                    'dfs.datanode.address': '0.0.0.0:50010',
                    'dfs.datanode.https.address': '0.0.0.0:1022',
                    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
        },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = [{'config-name': 'dfs.datanode.address',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "You set up datanode to use some non-secure ports. "
                            "If you want to run Datanode under non-root user in "
                            "a secure cluster, you should set all these properties "
                            "['dfs.datanode.address', 'dfs.datanode.https.address'] "
                            "to use non-secure ports (if property dfs.datanode.https.address "
                            "does not exist, just add it). You may also set up property "
                            "dfs.data.transfer.protection ('authentication' is a good "
                            "default value). Also, set up WebHDFS with SSL as described "
                            "in manual in order to be able to use HTTPS.",
                 'type': 'configuration'},
                {'config-name': 'dfs.datanode.https.address',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "You set up datanode to use some non-secure ports. "
                            "If you want to run Datanode under non-root user in "
                            "a secure cluster, you should set all these properties "
                            "['dfs.datanode.address', 'dfs.datanode.https.address'] "
                            "to use non-secure ports (if property dfs.datanode.https.address "
                            "does not exist, just add it). You may also set up property "
                            "dfs.data.transfer.protection ('authentication' is a good default value). "
                            "Also, set up WebHDFS with SSL as described in manual in order to be "
                            "able to use HTTPS.",
                 'type': 'configuration'}
    ]
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, valid non-root configuration
    properties = {  # hdfs-site
                    'dfs.http.policy': 'HTTPS_ONLY',
                    'dfs.datanode.address': '0.0.0.0:50010',
                    'dfs.datanode.https.address': '0.0.0.0:50475',
                    'dfs.data.transfer.protection': 'authentication',
                    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
      },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = []
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTP_ONLY, insecure port
    properties = {  # hdfs-site
                    'dfs.http.policy': 'HTTP_ONLY',
                    'dfs.datanode.address': '0.0.0.0:1019',
                    'dfs.datanode.http.address': '0.0.0.0:50475',
                    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
      },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = [{'config-name': 'dfs.datanode.address',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "You have set up datanode to use some non-secure ports, "
                            "but dfs.http.policy is set to HTTP_ONLY. In a secure cluster, "
                            "Datanode forbids using non-secure ports if dfs.http.policy is not "
                            "set to HTTPS_ONLY. Please make sure that properties "
                            "['dfs.datanode.address', 'dfs.datanode.http.address'] use secure ports.",
                 'type': 'configuration'},
                {'config-name': 'dfs.datanode.http.address',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "You have set up datanode to use some non-secure ports, "
                            "but dfs.http.policy is set to HTTP_ONLY. In a secure cluster, "
                            "Datanode forbids using non-secure ports if dfs.http.policy is not "
                            "set to HTTPS_ONLY. Please make sure that properties "
                            "['dfs.datanode.address', 'dfs.datanode.http.address'] use secure ports.",
                 'type': 'configuration'}
                ]
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTP_ONLY, valid configuration
    properties = {  # hdfs-site
                    'dfs.http.policy': 'HTTP_ONLY',
                    'dfs.datanode.address': '0.0.0.0:1019',
                    'dfs.datanode.http.address': '0.0.0.0:1022',
                    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
        },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = []
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, absent dfs.http.policy (typical situation)
    properties = {  # hdfs-site
                    'dfs.datanode.address': '0.0.0.0:1019',
                    'dfs.datanode.http.address': '0.0.0.0:1022',
                    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
        },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = []
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTP_ONLY, misusage of dfs.data.transfer.protection warning
    properties = {  # hdfs-site
                    'dfs.http.policy': 'HTTP_ONLY',
                    'dfs.datanode.address': '0.0.0.0:1019',
                    'dfs.datanode.http.address': '0.0.0.0:1022',
                    'dfs.data.transfer.protection': 'authentication',
    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
        },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = [{'config-name': 'dfs.data.transfer.protection',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "dfs.data.transfer.protection property can not be used when dfs.http.policy is "
                            "set to any value other then HTTPS_ONLY. Tip: When dfs.http.policy property is not defined, it defaults to HTTP_ONLY",
                 'type': 'configuration'}]
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, wrong dfs.data.transfer.protection value
    properties = {  # hdfs-site
                    'dfs.http.policy': 'HTTPS_ONLY',
                    'dfs.datanode.address': '0.0.0.0:50010',
                    'dfs.datanode.https.address': '0.0.0.0:50475',
                    'dfs.data.transfer.protection': 'WRONG_VALUE',
                    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
      },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = [{'config-name': 'dfs.data.transfer.protection',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "Invalid property value: WRONG_VALUE. Valid values are ['authentication', 'integrity', 'privacy'].",
                 'type': 'configuration'}]
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Hadoop wire encryption enabled

    properties = {  # hdfs-site
                    'dfs.encrypt.data.transfer': 'true',  # Wire encryption
                    'dfs.datanode.address': '0.0.0.0:1019',
                    'dfs.datanode.http.address': '0.0.0.0:1022',
    }
    configurations = {
      'hdfs-site': {
        'properties': properties,
      },
      'core-site': {
        'properties': secure_cluster_core_site
      },
      'ranger-hdfs-plugin-properties': {
          'properties':{
              'ranger-hdfs-plugin-enabled':'Yes'
          }
      }
    }
    expected = []  # No warnings
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

  def test_recommendYARNConfigurations(self):
    configurations = {}
    clusterData = {
      "cpu": 4,
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
          "yarn.scheduler.maximum-allocation-mb": "1280",
          "yarn.nodemanager.resource.cpu-vcores": "4"
        }
      }
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, None, None)
    self.assertEquals(configurations, expected)

  def test_recommendYARNConfigurationAttributes(self):
    configurations = {
      "yarn-env": {
        "properties": {
          "min_user_id": "500"
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.resource.memory-mb": "1280",
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "1280",
          "yarn.nodemanager.resource.cpu-vcores": "2"
        },
      }
    }
    clusterData = {
      "cpu": 4,
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
          "yarn.scheduler.maximum-allocation-mb": "1280",
          "yarn.nodemanager.resource.cpu-vcores": "2"
        },
        "property_attributes": {
          'yarn.nodemanager.resource.memory-mb': {'max': '1877'},
          'yarn.nodemanager.resource.cpu-vcores': {'max': '4'},
          'yarn.scheduler.minimum-allocation-vcores': {'max': '2'},
          'yarn.scheduler.maximum-allocation-vcores': {'max': '2'},
          'yarn.scheduler.minimum-allocation-mb': {'max': '1280'},
          'yarn.scheduler.maximum-allocation-mb': {'max': '1280'}
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
      "changed-configurations": [
        {
          "type": "yarn-site",
          "name": "yarn.nodemanager.resource.memory-mb"
        },
        {
          "type": "yarn-site",
          "name": "yarn.scheduler.minimum-allocation-mb"
        },
        {
          "type": "yarn-site",
          "name": "yarn.scheduler.maximum-allocation-mb"
        },
        {
          "type": "yarn-site",
          "name": "yarn.nodemanager.resource.cpu-vcores"
        },
        {
          "type": "yarn-env",
          "name": "min_user_id"
        },
      ],
      "depended-configurations": [
        {
          "type" : "mapred-site",
          "name" : "yarn.app.mapreduce.am.admin-command-opts"
        }, {
          "type" : "yarn-site",
          "name" : "yarn.scheduler.maximum-allocation-mb"
        }, {
          "type" : "yarn-site",
          "name" : "yarn.scheduler.minimum-allocation-mb"
        }, {
          "type" : "mapred-site",
          "name" : "mapreduce.reduce.java.opts"
        }, {
          "type" : "mapred-site",
          "name" : "mapreduce.map.java.opts"
        }, {
          "type" : "mapred-site",
          "name" : "yarn.app.mapreduce.am.command-opts"
        }, {
          "type" : "mapred-site",
          "name" : "yarn.app.mapreduce.am.resource.mb"
        }, {
          "type" : "yarn-site",
          "name" : "yarn.nodemanager.resource.memory-mb"
        }, {
          "type" : "mapred-site",
          "name" : "mapreduce.task.io.sort.mb"
        }, {
          "type" : "mapred-site",
          "name" : "mapreduce.reduce.memory.mb"
        }, {
          "type" : "mapred-site",
          "name" : "mapreduce.map.memory.mb"
        }
      ]

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

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

  def test_recommendAmsConfigurations(self):
    configurations = {}
    clusterData = {}

    services = {
      "services":  [ {
        "StackServices": {
          "service_name": "AMBARI_METRICS"
        },"components": [{
          "StackServiceComponents": {
            "component_name": "METRICS_COLLECTOR",
            "hostnames": ["host1"]
          }

        }]
      }]
    }
    hosts = {
      "items": [{
        "Hosts": {
          "host_name": "host1",

        }
      }]
    }

    expected = {
      "ams-hbase-env": {
        "properties": {
          "hbase_master_heapsize": "512m",
          "hbase_regionserver_heapsize": "512m",
          }
      },
      "ams-env": {
        "properties": {
          "metrics_collector_heapsize": "512m",
        }
      },
      "ams-hbase-site": {
        "properties": {
          "hbase.regionserver.global.memstore.lowerLimit": "0.3",
          "hbase.regionserver.global.memstore.upperLimit": "0.35",
          "hfile.block.cache.size": "0.3",
          "hbase_master_xmn_size" : "128m"
        }
      },
      "ams-site": {
        "properties": {
          "timeline.metrics.host.aggregator.ttl": "86400"
        }
      }
    }
    self.stackAdvisor.recommendAmsConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)
    
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
      "totalAvailableRam": 2048,
      "hBaseInstalled": 111
    }
    expected = {
      'hadoop-env': {
        'properties': {
          'namenode_heapsize': '1024',
          'namenode_opt_newsize' : '256',
          'namenode_opt_maxnewsize' : '256'
        }
      },
      'hdfs-site': {
        'properties': {
          'dfs.datanode.max.transfer.threads': '16384'
        },
      }
    }
    services = {"services":
                    [{"StackServices":
                          {"service_name" : "HDFS",
                           "service_version" : "2.6.0.2.2",
                           }
                     }],
                "configurations": {
                    'ranger-hdfs-plugin-properties':{
                        "properties": {"ranger-hdfs-plugin-enabled":"Yes"}
                    }
                }
                }
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, '')
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

  def test_validateMR2XmxOptsEnv(self):

    recommendedDefaults = {'mapreduce.map.java.opts': '-Xmx500m',
                           'mapreduce.reduce.java.opts': '-Xmx600m',
                           'mapreduce.task.io.sort.mb': '100',
                           'mapreduce.map.memory.mb': '200',
                           'mapreduce.reduce.memory.mb': '300',
                           'yarn.app.mapreduce.am.resource.mb': '400',
                           'yarn.app.mapreduce.am.command-opts': '-Xmx546m -Dhdp.version=${hdp.version}'}
    properties = {'mapreduce.map.java.opts': '-Xmxm',
                  'mapreduce.reduce.java.opts': '-Xmx0m',
                  'mapreduce.task.io.sort.mb': '110',
                  'mapreduce.map.memory.mb': '210',
                  'mapreduce.reduce.memory.mb': '310',
                  'yarn.app.mapreduce.am.resource.mb': '410',
                  'yarn.app.mapreduce.am.command-opts': '-Xmx545m -Dhdp.version=${hdp.version}'}
    res_expected = [{'config-type': 'mapred-site',
                     'message': 'Invalid value format',
                     'type': 'configuration',
                     'config-name': 'mapreduce.map.java.opts',
                     'level': 'ERROR'},
                    {'config-type': 'mapred-site',
                     'message': 'Value is less than the recommended default of -Xmx600m',
                     'type': 'configuration',
                     'config-name': 'mapreduce.reduce.java.opts',
                     'level': 'WARN'},
                    {'config-type': 'mapred-site',
                     'message': 'Value is less than the recommended default of -Xmx546m',
                     'type': 'configuration',
                     'config-name': 'yarn.app.mapreduce.am.command-opts',
                     'level': 'WARN'}]

    res = self.stackAdvisor.validateMapReduce2Configurations(properties, recommendedDefaults, {}, '', '')
    self.assertEquals(res, res_expected)
