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
    self.maxDiff = None
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
    configurations = {
        "yarn-site": {
            "properties": {
                "yarn.scheduler.minimum-allocation-mb": "256",
                "yarn.scheduler.maximum-allocation-mb": "2048",
                },
            }
    }
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
          # tez.am.resource.memory.mb must be <= yarn.scheduler.maximum-allocation-mb
          "tez.am.resource.memory.mb": "2048",
          "tez.task.resource.memory.mb": "768",
          "tez.runtime.io.sort.mb": "307",
          "tez.runtime.unordered.output.buffer.size-mb": "57",
          'tez.session.am.dag.submit.timeout.secs': '600'
        }
      },
      'yarn-site': {
        'properties': {
          'yarn.scheduler.minimum-allocation-mb': '256',
          'yarn.scheduler.maximum-allocation-mb': '2048'
        }
      }
    }
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, None, None)
    self.assertEquals(configurations, expected)

  def test_recommendTezConfigurations_amMemoryMoreThan3072(self):
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "2048",
          },
        }
    }
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
          # tez.am.resource.memory.mb must be <= yarn.scheduler.maximum-allocation-mb
          "tez.am.resource.memory.mb": "2048",
          "tez.task.resource.memory.mb": "768",
          "tez.runtime.io.sort.mb": "307",
          "tez.runtime.unordered.output.buffer.size-mb": "57",
          'tez.session.am.dag.submit.timeout.secs': '600'
        }
      },
      'yarn-site': {
        'properties': {
          'yarn.scheduler.minimum-allocation-mb': '256',
          'yarn.scheduler.maximum-allocation-mb': '2048'
        }
      }
    }
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, None, None)
    self.assertEquals(configurations, expected)

  def test_recommendTezConfigurations_mapMemoryLessThan768(self):
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "2048",
          },
        }
    }
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
          # tez.am.resource.memory.mb must be <= yarn.scheduler.maximum-allocation-mb
          "tez.am.resource.memory.mb": "2048",
          "tez.task.resource.memory.mb": "760",
          "tez.runtime.io.sort.mb": "304",
          "tez.runtime.unordered.output.buffer.size-mb": "57",
          'tez.session.am.dag.submit.timeout.secs': '600'
        }
      },
      'yarn-site': {
        'properties': {
          'yarn.scheduler.minimum-allocation-mb': '256',
          'yarn.scheduler.maximum-allocation-mb': '2048'
        }
      }
    }
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, None, None)
    self.assertEquals(configurations, expected)


  def test_validateHDFSConfigurations(self):
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
          "yarn.scheduler.maximum-allocation-vcores": "4",
          "yarn.scheduler.minimum-allocation-vcores": "1",
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
          "yarn.scheduler.maximum-allocation-vcores": "2",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.scheduler.maximum-allocation-mb": "1280",
          "yarn.nodemanager.resource.cpu-vcores": "2"
        },
        "property_attributes": {
          'yarn.nodemanager.resource.memory-mb': {'maximum': '1877'},
          'yarn.nodemanager.resource.cpu-vcores': {'maximum': '2'},
          'yarn.scheduler.minimum-allocation-vcores': {'maximum': '2'},
          'yarn.scheduler.maximum-allocation-vcores': {'maximum': '2'},
          'yarn.scheduler.minimum-allocation-mb': {'maximum': '1280'},
          'yarn.scheduler.maximum-allocation-mb': {'maximum': '1280'}
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

    # Test host NodeManager CPU cores
    hosts["items"][2]["Hosts"]["cpu_count"] = 6
    services["changed-configurations"].remove({
          "type": "yarn-site",
          "name": "yarn.nodemanager.resource.cpu-vcores"
        })
    configurations["yarn-site"]["properties"].pop("yarn.nodemanager.resource.cpu-vcores", None)
    expected["yarn-site"]["properties"]["yarn.nodemanager.resource.cpu-vcores"] = '4'
    expected["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-vcores"] = '1'
    expected["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-vcores"] = '4'
    expected["yarn-site"]["property_attributes"]["yarn.nodemanager.resource.cpu-vcores"]["maximum"] = '12'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.minimum-allocation-vcores"]["maximum"] = '4'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.maximum-allocation-vcores"]["maximum"] = '4'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Test host NodeManager CPU cores and 'yarn.nodemanager.resource.percentage-physical-cpu-limit'
    hosts["items"][2]["Hosts"]["cpu_count"] = 10
    configurations["yarn-site"]["properties"]["yarn.nodemanager.resource.percentage-physical-cpu-limit"] = '0.5'
    services["changed-configurations"].append({
          "type": "yarn-site",
          "name": "yarn.nodemanager.resource.percentage-physical-cpu-limit"
        })
    expected["yarn-site"]["properties"]["yarn.nodemanager.resource.cpu-vcores"] = '5'
    expected["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-vcores"] = '1'
    expected["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-vcores"] = '5'
    expected["yarn-site"]["properties"]["yarn.nodemanager.resource.percentage-physical-cpu-limit"] = '0.5'
    expected["yarn-site"]["property_attributes"]["yarn.nodemanager.resource.cpu-vcores"]["maximum"] = '20'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.minimum-allocation-vcores"]["maximum"] = '5'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.maximum-allocation-vcores"]["maximum"] = '5'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)
    
    # Test - with no 'changed-configurations', we should get updated 'maximum's.
    services.pop("changed-configurations", None)
    services.pop("configurations", None)
    services["configurations"] = {"yarn-site": {"properties": {"yarn.nodemanager.resource.memory-mb": '4321', "yarn.nodemanager.resource.cpu-vcores": '9'}}}
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.minimum-allocation-vcores"]["maximum"] = '9'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.maximum-allocation-vcores"]["maximum"] = '9'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.maximum-allocation-mb"]["maximum"] = '4321'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.minimum-allocation-mb"]["maximum"] = '4321'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)


  def test_recommendHiveConfigurationAttributes(self):
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
      },
      "hive-site": {
        "properties": {
          "hive.server2.authentication": "none"
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
      'capacity-scheduler': {
        'properties': {
          'yarn.scheduler.capacity.root.queues': 'queue1,queue2'
        }
      },
      'yarn-site': {
        'properties': {
          'yarn.scheduler.minimum-allocation-mb': '256',
          'yarn.scheduler.maximum-allocation-mb': '8192'
        }
      },
      'hive-env': {
        'properties': {
          'cost_based_optimizer': 'On',
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
          'hive.cbo.enable': 'true',
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
          'hive.tez.java.opts': '-server -Xmx615m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps',
          'hive.txn.manager': 'org.apache.hadoop.hive.ql.lockmgr.DummyTxnManager',
          'hive.vectorized.execution.enabled': 'true',
          'hive.vectorized.execution.reduce.enabled': 'false',
          'hive.security.metastore.authorization.manager': 'org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider',
          'hive.security.authorization.manager': 'org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdConfOnlyAuthorizerFactory',
          "hive.server2.authentication": "none"
        },
       'property_attributes': {
         'hive.auto.convert.join.noconditionaltask.size': {'maximum': '805306368'},
         'hive.server2.authentication.pam.services': {'delete': 'true'}, 
         'hive.server2.custom.authentication.class': {'delete': 'true'}, 
         'hive.server2.authentication.ldap.baseDN': {'delete': 'true'}, 
         'hive.server2.authentication.kerberos.principal': {'delete': 'true'}, 
         'hive.server2.authentication.kerberos.keytab': {'delete': 'true'}, 
         'hive.server2.authentication.ldap.url': {'delete': 'true'},
         'hive.server2.tez.default.queues': {
           'entries': [{'value': 'queue1', 'label': 'queue1 queue'}, {'value': 'queue2', 'label': 'queue2 queue'}]
          }
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
      "configurations": configurations,
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

    #test recommendations
    configurations["hive-site"]["properties"]["hive.cbo.enable"] = "false"
    configurations["hive-env"]["properties"]["hive_security_authorization"] = "sqlstdauth"
    services["changed-configurations"] = [{"type": "hive-site", "name": "hive.cbo.enable"},
                                          {"type": "hive-env", "name": "hive_security_authorization"}]
    expected["hive-env"]["properties"]["hive_security_authorization"] = "sqlstdauth"
    expected["hive-site"]["properties"]["hive.cbo.enable"] = "false"
    expected["hive-site"]["properties"]["hive.stats.fetch.partition.stats"]="false"
    expected["hive-site"]["properties"]["hive.stats.fetch.column.stats"]="false"
    expected["hive-site"]["properties"]["hive.security.authorization.enabled"]="true"
    expected["hive-site"]["properties"]["hive.server2.enable.doAs"]="false"
    expected["hive-site"]["properties"]["hive.security.metastore.authorization.manager"]=\
      "org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider,org.apache.hadoop.hive.ql.security.authorization.MetaStoreAuthzAPIAuthorizerEmbedOnly"
    expected["hiveserver2-site"]["properties"]["hive.security.authorization.enabled"]="true"
    expected["hiveserver2-site"]["properties"]["hive.security.authorization.manager"]="org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory"
    expected["hiveserver2-site"]["properties"]["hive.security.authenticator.manager"]="org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"

    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)


    # test 'hive_security_authorization'=='sqlstdauth' => 'hive.server2.enable.doAs'=='false'
    configurations["hive-env"]["properties"]["hive_security_authorization"] = "none"
    expected["hive-env"]["properties"]["hive_security_authorization"] = "none"
    expected["hive-site"]["properties"]["hive.security.authorization.enabled"]="false"
    expected["hive-site"]["properties"]["hive.server2.enable.doAs"]="true"
    expected["hive-site"]["properties"]["hive.security.metastore.authorization.manager"]=\
      "org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider"
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # test 'hive.server2.tez.default.queues' leaf queues
    configurations['capacity-scheduler']['properties'] = {
            "yarn.scheduler.capacity.maximum-am-resource-percent": "0.2",
            "yarn.scheduler.capacity.maximum-applications": "10000",
            "yarn.scheduler.capacity.node-locality-delay": "40",
            "yarn.scheduler.capacity.queue-mappings-override.enable": "false",
            "yarn.scheduler.capacity.resource-calculator": "org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator",
            "yarn.scheduler.capacity.root.accessible-node-labels": "*",
            "yarn.scheduler.capacity.root.acl_administer_queue": "*",
            "yarn.scheduler.capacity.root.capacity": "100",
            "yarn.scheduler.capacity.root.default.a.a1.acl_administer_queue": "*",
            "yarn.scheduler.capacity.root.default.a.a1.acl_submit_applications": "*",
            "yarn.scheduler.capacity.root.default.a.a1.capacity": "75",
            "yarn.scheduler.capacity.root.default.a.a1.maximum-capacity": "100",
            "yarn.scheduler.capacity.root.default.a.a1.minimum-user-limit-percent": "100",
            "yarn.scheduler.capacity.root.default.a.a1.ordering-policy": "fifo",
            "yarn.scheduler.capacity.root.default.a.a1.state": "RUNNING",
            "yarn.scheduler.capacity.root.default.a.a1.user-limit-factor": "1",
            "yarn.scheduler.capacity.root.default.a.a2.acl_administer_queue": "*",
            "yarn.scheduler.capacity.root.default.a.a2.acl_submit_applications": "*",
            "yarn.scheduler.capacity.root.default.a.a2.capacity": "25",
            "yarn.scheduler.capacity.root.default.a.a2.maximum-capacity": "25",
            "yarn.scheduler.capacity.root.default.a.a2.minimum-user-limit-percent": "100",
            "yarn.scheduler.capacity.root.default.a.a2.ordering-policy": "fifo",
            "yarn.scheduler.capacity.root.default.a.a2.state": "RUNNING",
            "yarn.scheduler.capacity.root.default.a.a2.user-limit-factor": "1",
            "yarn.scheduler.capacity.root.default.a.acl_administer_queue": "*",
            "yarn.scheduler.capacity.root.default.a.acl_submit_applications": "*",
            "yarn.scheduler.capacity.root.default.a.capacity": "50",
            "yarn.scheduler.capacity.root.default.a.maximum-capacity": "100",
            "yarn.scheduler.capacity.root.default.a.minimum-user-limit-percent": "100",
            "yarn.scheduler.capacity.root.default.a.ordering-policy": "fifo",
            "yarn.scheduler.capacity.root.default.a.queues": "a1,a2",
            "yarn.scheduler.capacity.root.default.a.state": "RUNNING",
            "yarn.scheduler.capacity.root.default.a.user-limit-factor": "1",
            "yarn.scheduler.capacity.root.default.acl_submit_applications": "*",
            "yarn.scheduler.capacity.root.default.b.acl_administer_queue": "*",
            "yarn.scheduler.capacity.root.default.b.acl_submit_applications": "*",
            "yarn.scheduler.capacity.root.default.b.capacity": "50",
            "yarn.scheduler.capacity.root.default.b.maximum-capacity": "50",
            "yarn.scheduler.capacity.root.default.b.minimum-user-limit-percent": "100",
            "yarn.scheduler.capacity.root.default.b.ordering-policy": "fifo",
            "yarn.scheduler.capacity.root.default.b.state": "RUNNING",
            "yarn.scheduler.capacity.root.default.b.user-limit-factor": "1",
            "yarn.scheduler.capacity.root.default.capacity": "100",
            "yarn.scheduler.capacity.root.default.maximum-capacity": "100",
            "yarn.scheduler.capacity.root.default.queues": "a,b",
            "yarn.scheduler.capacity.root.default.state": "RUNNING",
            "yarn.scheduler.capacity.root.default.user-limit-factor": "1",
            "yarn.scheduler.capacity.root.queues": "default"
          }
    expected['hive-site']['properties']['hive.server2.tez.default.queues'] = 'default.a.a1,default.a.a2,default.b'
    expected['hive-site']['property_attributes']['hive.server2.tez.default.queues'] = {
           'entries': [{'value': 'default.a.a1', 'label': 'default.a.a1 queue'}, {'value': 'default.a.a2', 'label': 'default.a.a2 queue'}, {'value': 'default.b', 'label': 'default.b queue'}]
          }
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hive-site']['property_attributes']['hive.server2.tez.default.queues'], expected['hive-site']['property_attributes']['hive.server2.tez.default.queues'])
    self.assertEquals(configurations['hive-site']['properties']['hive.server2.tez.default.queues'], expected['hive-site']['properties']['hive.server2.tez.default.queues'])

  def test_recommendMapredConfigurationAttributesWithPigService(self):
    configurations = {
      "mapred-site": {
        "properties": {
          "mapreduce.map.memory.mb": "1024",
          "mapreduce.reduce.memory.mb": "682",
          "yarn.app.mapreduce.am.command-opts": "-Xmx546m -Dhdp.version=${hdp.version}",
          "mapreduce.reduce.java.opts": "-Xmx546m",
          "yarn.app.mapreduce.am.resource.mb": "682",
          "mapreduce.map.java.opts": "-Xmx546m",
          "mapreduce.task.io.sort.mb": "273"
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.resource.memory-mb": "2048",
          "yarn.scheduler.minimum-allocation-mb": "100",
          "yarn.scheduler.maximum-allocation-mb": "2048",
          "yarn.nodemanager.resource.cpu-vcores": "2"
        },
        }
    }
    clusterData = {
      "cpu": 4,
      "containers" : 7,
      "ramPerContainer": 256,
      "totalAvailableRam": 4096,
    }
    expected = {
      "yarn-env": {
        "properties": {
          "min_user_id": "500"
        }
      },
      "mapred-site": {
        "properties": {
          "mapreduce.map.memory.mb": "1536",
          "mapreduce.reduce.memory.mb": "1536",
          "yarn.app.mapreduce.am.command-opts": "-Xmx80m -Dhdp.version=${hdp.version}",
          "mapreduce.reduce.java.opts": "-Xmx1228m",
          "yarn.app.mapreduce.am.resource.mb": "100",
          "mapreduce.map.java.opts": "-Xmx1228m",
          "mapreduce.task.io.sort.mb": "859"
        },
        "property_attributes": {
          'mapreduce.task.io.sort.mb': {'maximum': '2047'},
          'yarn.app.mapreduce.am.resource.mb': {'maximum': '1792',
                                                'minimum': '100'},
          'mapreduce.map.memory.mb': {'maximum': '1792',
                                      'minimum': '100'},
          'mapreduce.reduce.memory.mb': {'maximum': '1792',
                                         'minimum': '100'}
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.resource.memory-mb": "1792",
          "yarn.scheduler.minimum-allocation-mb": "100",
          "yarn.scheduler.maximum-allocation-vcores": "1",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.scheduler.maximum-allocation-mb": "1792",
          "yarn.nodemanager.resource.cpu-vcores": "1"
        },
        "property_attributes": {
          'yarn.nodemanager.resource.memory-mb': {'maximum': '1877'},
          'yarn.nodemanager.resource.cpu-vcores': {'maximum': '2'},
          'yarn.scheduler.minimum-allocation-vcores': {'maximum': '1'},
          'yarn.scheduler.maximum-allocation-vcores': {'maximum': '1'},
          'yarn.scheduler.minimum-allocation-mb': {'maximum': '1792'},
          'yarn.scheduler.maximum-allocation-mb': {'maximum': '1792'}
        }
      }
    }
    services = {
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/PIG",
          "StackServices": {
            "service_name": "PIG",
            "service_version": "2.6.0.2.2",
            "stack_name": "HDP",
            "stack_version": "2.2"
          }, "components": [
          {
            "StackServiceComponents": {
              "advertise_version": "true",
              "cardinality": "0+",
              "component_category": "CLIENT",
              "component_name": "PIG",
              "display_name": "Pig",
              "is_client": "true",
              "is_master": "false",
              "hostnames": []
            },
            "dependencies": []
          }
        ]
        },
        {
          "href" : "/api/v1/stacks/HDP/versions/2.2/services/MAPREDUCE2",
          "StackServices" : {
            "service_name" : "MAPREDUCE2",
            "service_version" : "2.6.0.2.2",
            "stack_name" : "HDP",
            "stack_version" : "2.2"
          },
          "components" : [ {
                             "href" : "/api/v1/stacks/HDP/versions/2.2/services/MAPREDUCE2/components/HISTORYSERVER",
                             "StackServiceComponents" : {
                               "advertise_version" : "true",
                               "cardinality" : "1",
                               "component_category" : "MASTER",
                               "component_name" : "HISTORYSERVER",
                               "custom_commands" : [ ],
                               "display_name" : "History Server",
                               "is_client" : "false",
                               "is_master" : "true",
                               "service_name" : "MAPREDUCE2",
                               "stack_name" : "HDP",
                               "stack_version" : "2.2",
                               "hostnames" : [ "c6402.ambari.apache.org" ]
                             },
                             "auto_deploy" : {
                               "enabled" : "true",
                               "location" : "YARN/RESOURCEMANAGER"
                             },
                             "dependencies" : [ {
                                                  "href" : "/api/v1/stacks/HDP/versions/2.2/services/MAPREDUCE2/components/HISTORYSERVER/dependencies/HDFS_CLIENT",
                                                  "Dependencies" : {
                                                    "component_name" : "HDFS_CLIENT",
                                                    "dependent_component_name" : "HISTORYSERVER",
                                                    "dependent_service_name" : "MAPREDUCE2",
                                                    "stack_name" : "HDP",
                                                    "stack_version" : "2.2"
                                                  }
                                                } ]
                           }]},
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
          "name": "yarn.scheduler.minimum-allocation-mb"
        },
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

    self.stackAdvisor.recommendMapReduce2Configurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

  def test_recommendMapredConfigurationAttributes(self):
    configurations = {
      "mapred-site": {
        "properties": {
          "mapreduce.map.memory.mb": "1024",
          "mapreduce.reduce.memory.mb": "682",
          "yarn.app.mapreduce.am.command-opts": "-Xmx546m -Dhdp.version=${hdp.version}",
          "mapreduce.reduce.java.opts": "-Xmx546m",
          "yarn.app.mapreduce.am.resource.mb": "682",
          "mapreduce.map.java.opts": "-Xmx546m",
          "mapreduce.task.io.sort.mb": "273"
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.resource.memory-mb": "1280",
          "yarn.scheduler.minimum-allocation-mb": "100",
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
      "mapred-site": {
        "properties": {
          "mapreduce.map.memory.mb": "100",
          "mapreduce.reduce.memory.mb": "200",
          "yarn.app.mapreduce.am.command-opts": "-Xmx80m -Dhdp.version=${hdp.version}",
          "mapreduce.reduce.java.opts": "-Xmx160m",
          "yarn.app.mapreduce.am.resource.mb": "100",
          "mapreduce.map.java.opts": "-Xmx80m",
          "mapreduce.task.io.sort.mb": "56"
        },
        "property_attributes": {
          'mapreduce.task.io.sort.mb': {'maximum': '2047'},
          'yarn.app.mapreduce.am.resource.mb': {'maximum': '1280',
                                                'minimum': '100'},
          'mapreduce.map.memory.mb': {'maximum': '1280',
                                      'minimum': '100'},
          'mapreduce.reduce.memory.mb': {'maximum': '1280',
                                         'minimum': '100'}
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.resource.memory-mb": "1280",
          "yarn.scheduler.minimum-allocation-mb": "100",
          "yarn.scheduler.maximum-allocation-vcores": "1",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.scheduler.maximum-allocation-mb": "1280",
          "yarn.nodemanager.resource.cpu-vcores": "1"
        },
        "property_attributes": {
          'yarn.nodemanager.resource.memory-mb': {'maximum': '1877'},
          'yarn.nodemanager.resource.cpu-vcores': {'maximum': '2'},
          'yarn.scheduler.minimum-allocation-vcores': {'maximum': '1'},
          'yarn.scheduler.maximum-allocation-vcores': {'maximum': '1'},
          'yarn.scheduler.minimum-allocation-mb': {'maximum': '1280'},
          'yarn.scheduler.maximum-allocation-mb': {'maximum': '1280'}
        }
      }
    }
    services = {
      "services": [
        {
          "href" : "/api/v1/stacks/HDP/versions/2.2/services/MAPREDUCE2",
          "StackServices" : {
            "service_name" : "MAPREDUCE2",
            "service_version" : "2.6.0.2.2",
            "stack_name" : "HDP",
            "stack_version" : "2.2"
          },
          "components" : [ {
                             "href" : "/api/v1/stacks/HDP/versions/2.2/services/MAPREDUCE2/components/HISTORYSERVER",
                             "StackServiceComponents" : {
                               "advertise_version" : "true",
                               "cardinality" : "1",
                               "component_category" : "MASTER",
                               "component_name" : "HISTORYSERVER",
                               "custom_commands" : [ ],
                               "display_name" : "History Server",
                               "is_client" : "false",
                               "is_master" : "true",
                               "service_name" : "MAPREDUCE2",
                               "stack_name" : "HDP",
                               "stack_version" : "2.2",
                               "hostnames" : [ "c6402.ambari.apache.org" ]
                             },
                             "auto_deploy" : {
                               "enabled" : "true",
                               "location" : "YARN/RESOURCEMANAGER"
                             },
                             "dependencies" : [ {
                                                  "href" : "/api/v1/stacks/HDP/versions/2.2/services/MAPREDUCE2/components/HISTORYSERVER/dependencies/HDFS_CLIENT",
                                                  "Dependencies" : {
                                                    "component_name" : "HDFS_CLIENT",
                                                    "dependent_component_name" : "HISTORYSERVER",
                                                    "dependent_service_name" : "MAPREDUCE2",
                                                    "stack_name" : "HDP",
                                                    "stack_version" : "2.2"
                                                  }
                                                } ]
        }]},
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
          "name": "yarn.scheduler.minimum-allocation-mb"
        },
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

    self.stackAdvisor.recommendMapReduce2Configurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    configurations["yarn-site"]["properties"]["yarn.scheduler.minimum-allocation-mb"] = "700"

    expected = {
        "yarn-env": {
            "properties": {
                "min_user_id": "500"
            }
        },
        "mapred-site": {
            "properties": {
                "mapreduce.map.memory.mb": "700",
                "mapreduce.reduce.memory.mb": "1280",
                "yarn.app.mapreduce.am.command-opts": "-Xmx560m -Dhdp.version=${hdp.version}",
                "mapreduce.reduce.java.opts": "-Xmx1024m",
                "yarn.app.mapreduce.am.resource.mb": "700",
                "mapreduce.map.java.opts": "-Xmx560m",
                "mapreduce.task.io.sort.mb": "392"
            },
            "property_attributes": {
                'mapreduce.task.io.sort.mb': {'maximum': '2047'},
                'yarn.app.mapreduce.am.resource.mb': {'maximum': '1280',
                                                      'minimum': '700'},
                'mapreduce.map.memory.mb': {'maximum': '1280',
                                            'minimum': '700'},
                'mapreduce.reduce.memory.mb': {'maximum': '1280',
                                               'minimum': '700'}
            }
        },
        "yarn-site": {
            "properties": {
                "yarn.nodemanager.resource.memory-mb": "1280",
                "yarn.scheduler.minimum-allocation-mb": "700",
                "yarn.scheduler.maximum-allocation-vcores": "1",
                "yarn.scheduler.minimum-allocation-vcores": "1",
                "yarn.scheduler.maximum-allocation-mb": "1280",
                "yarn.nodemanager.resource.cpu-vcores": "1"
            },
            "property_attributes": {
                'yarn.nodemanager.resource.memory-mb': {'maximum': '1877'},
                'yarn.nodemanager.resource.cpu-vcores': {'maximum': '2'},
                'yarn.scheduler.minimum-allocation-vcores': {'maximum': '1'},
                'yarn.scheduler.maximum-allocation-vcores': {'maximum': '1'},
                'yarn.scheduler.minimum-allocation-mb': {'maximum': '1280'},
                'yarn.scheduler.maximum-allocation-mb': {'maximum': '1280'}
            }
        }
    }
    self.stackAdvisor.recommendMapReduce2Configurations(configurations, clusterData, services, hosts)
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

  def test_recommendHbaseSiteConfigurations(self):
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
    services = {
      "services" : [
      ],
      "configurations": {
        "hbase-env": {
          "properties": {
            "phoenix_sql_enabled": "true",
            "hbase_max_direct_memory_size": ""
          }
        },
        "hbase-site": {
          "properties": {
            "hbase.rpc.controllerfactory.class": "",
            "phoenix.functions.allowUserDefinedFunctions": "",
            "hbase.bucketcache.ioengine": "",
            "hbase.bucketcache.size": "",
            "hbase.bucketcache.percentage.in.combinedcache": "",
            "hbase.coprocessor.regionserver.classes": ""
          }
        }
      }
    }
    expected = {
      "hbase-site": {
        "properties": {
          "hbase.regionserver.wal.codec": "org.apache.hadoop.hbase.regionserver.wal.IndexedWALEditCodec",
          "phoenix.functions.allowUserDefinedFunctions": "true",
          "hbase.regionserver.global.memstore.size": "0.4",
          "hbase.coprocessor.region.classes": "org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint"
        },
        'property_attributes': {
          "hbase.bucketcache.size": {
            "delete": "true"
          },
          "hbase.bucketcache.percentage.in.combinedcache": {
            "delete": "true"
          },
          "hbase.coprocessor.regionserver.classes": {
            "delete": "true"
          },
          "hbase.bucketcache.ioengine": {
            "delete": "true"
          }
        }
      },
      "hbase-env": {
        "properties": {
          "hbase_master_heapsize": "8192",
          "hbase_regionserver_heapsize": "8192",
        },
        "property_attributes": {
          "hbase_max_direct_memory_size": {
            "delete": "true"
          }
        }
      }
    }

    clusterData = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, None)
    self.assertEquals(clusterData['hbaseRam'], 8)

    # Test when phoenix_sql_enabled = true
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

    # Test when phoenix_sql_enabled = false
    services['configurations']['hbase-env']['properties']['phoenix_sql_enabled'] = 'false'
    expected['hbase-site']['properties']['hbase.regionserver.wal.codec'] = 'org.apache.hadoop.hbase.regionserver.wal.WALCellCodec'
    expected['hbase-site']['property_attributes']['hbase.rpc.controllerfactory.class'] = {'delete': 'true'}
    expected['hbase-site']['property_attributes']['hbase.coprocessor.regionserver.classes'] = {'delete': 'true'}
    expected['hbase-site']['property_attributes']['phoenix.functions.allowUserDefinedFunctions'] = {'delete': 'true'}
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

    # Test hbase_master_heapsize maximum
    hosts['items'][0]['Hosts']['host_name'] = 'host1'
    services['services'].append({"StackServices":
                          {"service_name" : "HBASE",
                           "service_version" : "2.6.0.2.2"
                           },
                      "components":[
                        {
                          "href":"/api/v1/stacks/HDP/versions/2.2/services/HBASE/components/HBASE_MASTER",
                          "StackServiceComponents":{
                            "advertise_version":"true",
                            "cardinality":"1+",
                            "component_name":"HBASE_MASTER",
                            "custom_commands":[],
                            "display_name":"DataNode",
                            "is_client":"false",
                            "is_master":"false",
                            "service_name":"HBASE",
                            "stack_name":"HDP",
                            "stack_version":"2.2",
                            "hostnames":[
                              "host1"
                            ]
                          },
                          "dependencies":[]
                        }]})
    services['configurations']['hbase-env']['properties']['phoenix_sql_enabled'] = 'false'
    expected['hbase-site']['properties']['hbase.regionserver.wal.codec'] = 'org.apache.hadoop.hbase.regionserver.wal.WALCellCodec'
    expected['hbase-site']['property_attributes']['hbase.rpc.controllerfactory.class'] = {'delete': 'true'}
    expected['hbase-site']['property_attributes']['hbase.coprocessor.regionserver.classes'] = {'delete': 'true'}
    expected['hbase-site']['property_attributes']['phoenix.functions.allowUserDefinedFunctions'] = {'delete': 'true'}
    expected['hbase-env']['property_attributes']['hbase_master_heapsize'] = {'maximum': '49152'}
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Test when hbase.security.authentication = kerberos
    services['configurations']['hbase-site']['properties']['hbase.security.authentication'] = 'kerberos'
    expected['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint'
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

    # Test when hbase.security.authentication = simple
    services['configurations']['hbase-site']['properties']['hbase.security.authentication'] = 'simple'
    expected['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint'
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

    # Test when hbase.security.authentication = kerberos AND class already there
    configurations['hbase-site']['properties'].pop('hbase.coprocessor.region.classes', None)
    services['configurations']['hbase-site']['properties']['hbase.security.authentication'] = 'kerberos'
    services['configurations']['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'a.b.c.d'
    expected['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'a.b.c.d,org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint'
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

    # Test when hbase.security.authentication = kerberos AND authorization = true
    configurations['hbase-site']['properties'].pop('hbase.coprocessor.region.classes', None)
    services['configurations']['hbase-site']['properties'].pop('hbase.coprocessor.region.classes', None)
    services['configurations']['hbase-site']['properties']['hbase.security.authentication'] = 'kerberos'
    services['configurations']['hbase-site']['properties']['hbase.security.authorization'] = 'true'
    expected['hbase-site']['properties']['hbase.coprocessor.master.classes'] = "org.apache.hadoop.hbase.security.access.AccessController"
    expected['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'org.apache.hadoop.hbase.security.access.AccessController,org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint'
    expected['hbase-site']['properties']['hbase.coprocessor.regionserver.classes'] = "org.apache.hadoop.hbase.security.access.AccessController"
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

    # Test - default recommendations should have certain configs deleted. HAS TO BE LAST TEST.
    services["configurations"] = {"hbase-site": {"properties": {"phoenix.functions.allowUserDefinedFunctions": '', "hbase.rpc.controllerfactory.class": ''}}}
    configurations = {}
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['hbase-site']['property_attributes']['phoenix.functions.allowUserDefinedFunctions'], {'delete': 'true'})
    self.assertEquals(configurations['hbase-site']['property_attributes']['hbase.rpc.controllerfactory.class'], {'delete': 'true'})
    self.assertEquals(configurations['hbase-site']['properties']['hbase.regionserver.wal.codec'], "org.apache.hadoop.hbase.regionserver.wal.WALCellCodec")


  def test_recommendHDFSConfigurations(self):
    configurations = {
      'ranger-hdfs-plugin-properties':{
        "properties": {"ranger-hdfs-plugin-enabled":"Yes"}
      },
      'hdfs-site': {
        "properties": {"dfs.datanode.data.dir": "/path/1,/path/2,/path/3,/path/4"}
      }
    }
    clusterData = {
      "totalAvailableRam": 2048,
      "hBaseInstalled": True,
      "hbaseRam": 112,
      "reservedRam": 128
    }
    expected = {
      'hadoop-env': {
        'properties': {
          'namenode_heapsize': '1024',
          'namenode_opt_newsize' : '128',
          'namenode_opt_maxnewsize' : '128'
        },
        'property_attributes': {
          'dtnode_heapsize': {'maximum': '2048'},
          'namenode_heapsize': {'maximum': '10240'}
        }
      },
      'hdfs-site': {
        'properties': {
          'dfs.datanode.max.transfer.threads': '16384',
          'dfs.namenode.safemode.threshold-pct': '1.000',
          'dfs.datanode.failed.volumes.tolerated': '1',
          'dfs.namenode.handler.count': '25',
          'dfs.datanode.data.dir': '/path/1,/path/2,/path/3,/path/4'
        },
        'property_attributes': {
          'dfs.datanode.failed.volumes.tolerated': {'maximum': '4'}
        }
      },
      'ranger-hdfs-plugin-properties': {
        'properties': {
          'ranger-hdfs-plugin-enabled': 'Yes'
        }
      }
    }
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
                "configurations": configurations
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
            "total_mem" : 10485760
          }
        },
      ]
    }

    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)
    # Test 1 - namenode heapsize depends on # of datanodes
    datanode_hostnames = services["services"][0]["components"][0]["StackServiceComponents"]["hostnames"] # datanode hostnames
    for i in xrange(10):
      hostname = "datanode" + `i`
      datanode_hostnames.append(hostname)
      hosts['items'].append(
        {
          "href" : "/api/v1/hosts/" + hostname,
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : hostname,
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : hostname,
            "rack_info" : "/default-rack",
            "total_mem" : 2097152
          }
        }
      )
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations["hadoop-env"]["properties"]["namenode_heapsize"], "3072")
    self.assertEquals(configurations["hadoop-env"]["properties"]["namenode_opt_maxnewsize"], "384")
    self.assertEquals(configurations["hadoop-env"]["properties"]["namenode_opt_maxnewsize"], "384")
    # Test 2 - add more datanodes
    for i in xrange(11,30):
      hostname = "datanode" + `i`
      datanode_hostnames.append(hostname)
      hosts['items'].append(
        {
          "href" : "/api/v1/hosts/" + hostname,
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : hostname,
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : hostname,
            "rack_info" : "/default-rack",
            "total_mem" : 2097152
          }
        }
      )
    # namenode_heapsize depends on number of disks used used by datanode
    configurations["hdfs-site"]["properties"]["dfs.datanode.data.dir"] = "/path1,/path2,/path3,/path4"
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations["hadoop-env"]["properties"]["namenode_heapsize"], "9984")
    self.assertEquals(configurations["hadoop-env"]["properties"]["namenode_opt_maxnewsize"], "1248")
    self.assertEquals(configurations["hadoop-env"]["properties"]["namenode_opt_maxnewsize"], "1248")
    # Test 3 - more datanodes than host can handle
    for i in xrange(31, 90):
      hostname = "datanode" + `i`
      datanode_hostnames.append(hostname)
      hosts['items'].append(
        {
          "href" : "/api/v1/hosts/" + hostname,
          "Hosts" : {
            "cpu_count" : 1,
            "host_name" : hostname,
            "os_arch" : "x86_64",
            "os_type" : "centos6",
            "ph_cpu_count" : 1,
            "public_host_name" : hostname,
            "rack_info" : "/default-rack",
            "total_mem" : 2097152
          }
        }
      )
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations["hadoop-env"]["properties"]["namenode_heapsize"], "10112")
    self.assertEquals(configurations["hadoop-env"]["properties"]["namenode_opt_maxnewsize"], "1264")
    self.assertEquals(configurations["hadoop-env"]["properties"]["namenode_opt_maxnewsize"], "1264")

    # Test 4 - KMS empty test from previous call
    self.assertTrue("dfs.encryption.key.provider.uri" not in configurations["hdfs-site"]["properties"])
    
    # Test 5 - Calculated from hosts install location
    services["services"].append(
                    {"StackServices":
                          {"service_name" : "RANGER_KMS",
                           "service_version" : "2.6.0.2.2"
                           },
                      "components":[
                        {
                          "href":"/api/v1/stacks/HDP/versions/2.2/services/RANGER_KMS/components/RANGER_KMS_SERVER",
                          "StackServiceComponents":{
                            "advertise_version":"true",
                            "cardinality":"1+",
                            "component_category":"SLAVE",
                            "component_name":"RANGER_KMS_SERVER",
                            "custom_commands":[

                            ],
                            "display_name":"RANGER_KMS_SERVER",
                            "is_client":"false",
                            "is_master":"false",
                            "service_name":"RANGER_KMS",
                            "stack_name":"HDP",
                            "stack_version":"2.2",
                            "hostnames":[
                              "host1"
                            ]
                          },
                          "dependencies":[

                          ]
                        }
                       ]
                     })
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEqual("kms://http@host1:9292/kms", configurations["hdfs-site"]["properties"]["dfs.encryption.key.provider.uri"])

    # Test 6 - Multiple RANGER_KMS_SERVERs
    services["services"][len(services["services"])-1]["components"][0]["StackServiceComponents"]["hostnames"].append("host2")
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEqual("kms://http@host1,host2:9292/kms", configurations["hdfs-site"]["properties"]["dfs.encryption.key.provider.uri"])

    # Test 6 - Multiple RANGER_KMS_SERVERs and custom port
    configurations["kms-env"] = {"properties": {"kms_port": "1111"}}
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEqual("kms://http@host1,host2:1111/kms", configurations["hdfs-site"]["properties"]["dfs.encryption.key.provider.uri"])

    # Test 7 - Override by API caller
    configurations["hadoop-env"] = {"properties": {"keyserver_host": "myhost1", "keyserver_port": "2222"}}
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEqual("kms://http@myhost1:2222/kms", configurations["hdfs-site"]["properties"]["dfs.encryption.key.provider.uri"])

    # Test - 'https' in KMS URL
    configurations["ranger-kms-site"] = {"properties": {"ranger.service.https.attrib.ssl.enabled": "true"}}
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEqual("kms://https@myhost1:2222/kms", configurations["hdfs-site"]["properties"]["dfs.encryption.key.provider.uri"])

    # Test 8 - Dynamic maximum for 'dfs.namenode.handler.count'
    hosts['items'][1]['Hosts']['cpu_count'] = 9
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEqual(str(9 * 25), configurations["hdfs-site"]["property_attributes"]["dfs.namenode.handler.count"]['maximum'])

    # Test 9 - Dynamic maximum for 'dfs.namenode.handler.count'
    configurations["hdfs-site"]["property_attributes"].pop("dfs.namenode.handler.count", None)
    hosts['items'][1]['Hosts']['cpu_count'] = 4
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertTrue("dfs.namenode.handler.count" not in configurations["hdfs-site"]["property_attributes"])

  def test_validateTezConfigurationsEnv(self):
    configurations = {
        "yarn-site": {
            "properties": {
                "yarn.scheduler.minimum-allocation-mb": "100",
                "yarn.scheduler.maximum-allocation-mb": "2048"
            }
        }
    }

    recommendedDefaults = {'tez.task.resource.memory.mb': '1024',
                           'tez.runtime.io.sort.mb' : '256',
                           'tez.runtime.unordered.output.buffer.size-mb' : '256',
                           'tez.am.resource.memory.mb' : '1024'}

    properties = {'tez.task.resource.memory.mb': '2050',
                  'tez.runtime.io.sort.mb' : '256',
                  'tez.runtime.unordered.output.buffer.size-mb' : '256',
                  'tez.am.resource.memory.mb' : '2050'}


    res_expected = [{'config-name': 'tez.am.resource.memory.mb',
                 'config-type': 'tez-site',
                 'level': 'WARN',
                 'message': "tez.am.resource.memory.mb should be less than YARN max allocation size (2048)",
                 'type': 'configuration',
                 'level': 'WARN'},
                    {'config-name': 'tez.task.resource.memory.mb',
                 'config-type': 'tez-site',
                 'level': 'WARN',
                 'message': "tez.task.resource.memory.mb should be less than YARN max allocation size (2048)",
                 'type': 'configuration',
                 'level': 'WARN'}]

    res = self.stackAdvisor.validateTezConfigurations(properties, recommendedDefaults, configurations, '', '')
    self.assertEquals(res, res_expected)


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

  def test_validateYARNConfigurationsEnv(self):
    configurations = {}

    # 1) ok: No yarn_cgroups_enabled
    recommendedDefaults = {'namenode_heapsize': '1024',
                           'namenode_opt_newsize' : '256',
                           'namenode_opt_maxnewsize' : '256'}
    properties = {}
    res_expected = []

    res = self.stackAdvisor.validateYARNEnvConfigurations(properties, recommendedDefaults, configurations, '', '')
    self.assertEquals(res, res_expected)

    # 2) ok: yarn_cgroups_enabled=false, but security enabled
    properties['yarn_cgroups_enabled'] = 'false'
    configurations = {
      "core-site": {
        "properties": {
          "hadoop.security.authentication": "kerberos",
          "hadoop.security.authorization": "true"
        }
      }
    }
    res_expected = []
    res = self.stackAdvisor.validateYARNEnvConfigurations(properties, recommendedDefaults, configurations, '', '')
    self.assertEquals(res, res_expected)

    # 3) ok: yarn_cgroups_enabled=true, but security enabled
    properties['yarn_cgroups_enabled'] = 'true'
    res_expected = []
    res = self.stackAdvisor.validateYARNEnvConfigurations(properties, recommendedDefaults, configurations, '', '')
    self.assertEquals(res, res_expected)

    # 4) fail: yarn_cgroups_enabled=true, but security disabled
    configurations['core-site']['properties']['hadoop.security.authorization'] = 'false'
    res_expected = [{'config-type': 'yarn-env',
                     'message': 'CPU Isolation should only be enabled if security is enabled',
                     'type': 'configuration',
                     'config-name': 'yarn_cgroups_enabled',
                     'level': 'WARN'}]
    res = self.stackAdvisor.validateYARNEnvConfigurations(properties, recommendedDefaults, configurations, '', '')
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
                     'level': 'WARN'},
                    {'config-type': 'mapred-site',
                     'message': 'yarn.app.mapreduce.am.command-opts Xmx should be less than yarn.app.mapreduce.am.resource.mb (410)',
                     'type': 'configuration',
                     'config-name': 'yarn.app.mapreduce.am.command-opts',
                     'level': 'WARN'}]

    res = self.stackAdvisor.validateMapReduce2Configurations(properties, recommendedDefaults, {}, '', '')
    self.assertEquals(res, res_expected)

  def test_validateHiveConfigurationsEnv(self):
    properties = {"hive_security_authorization": "None"}
    configurations = {"hive-site": {
                        "properties": {"hive.security.authorization.enabled": "true"}
                      },
                      "hive-env": {
                        "properties": {"hive_security_authorization": "None"}
                      }
    }

    res_expected = [
      {
        "config-type": "hive-env",
        "message": "hive_security_authorization should not be None if hive.security.authorization.enabled is set",
        'type': 'configuration',
        "config-name": "hive_security_authorization",
        "level": "ERROR"
      }
    ]

    res = self.stackAdvisor.validateHiveConfigurationsEnv(properties, {}, configurations, {}, {})
    self.assertEquals(res, res_expected)

    pass

  def test_validateHiveConfigurations(self):
    properties = {"hive_security_authorization": "None",
                  "hive.exec.orc.default.stripe.size": "8388608",
                  'hive.tez.container.size': '2048',
                  'hive.tez.java.opts': '-Xmx300m',
                  'hive.auto.convert.join.noconditionaltask.size': '1100000000'}
    recommendedDefaults = {'hive.tez.container.size': '1024',
                           'hive.tez.java.opts': '-Xmx256m',
                           'hive.auto.convert.join.noconditionaltask.size': '1000000000'}
    configurations = {
      "hive-site": {
        "properties": {"hive.security.authorization.enabled": "true"}
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

    pass

  def test_recommendYarnCGroupConfigurations(self):
    servicesList = ["YARN"]
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
        "yarn-env": {
          "properties": {
            "yarn_cgroups_enabled": "true"
          }
        }
      }
    }
    expected = {
      "yarn-env": {
        "properties": {
          "min_user_id": "500"
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.container-executor.group": "hadoop",
          "yarn.nodemanager.container-executor.class": "org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor",
          "yarn.nodemanager.linux-container-executor.cgroups.mount-path": "/cgroup",
          "yarn.nodemanager.container-executor.cgroups.mount": "true",
          "yarn.nodemanager.resource.memory-mb": "39424",
          "yarn.scheduler.minimum-allocation-mb": "3584",
          "yarn.scheduler.maximum-allocation-vcores": "4",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.nodemanager.resource.cpu-vcores": "4",
          "yarn.nodemanager.container-executor.cgroups.hierarchy": " /yarn",
          "yarn.scheduler.maximum-allocation-mb": "39424",
          "yarn.nodemanager.container-executor.resources-handler.class": "org.apache.hadoop.yarn.server.nodemanager.util.CgroupsLCEResourcesHandler"
        },
        "property_attributes": {
          "yarn.scheduler.minimum-allocation-vcores": {
            "maximum": "4"
          },
          "yarn.scheduler.maximum-allocation-vcores": {
            "maximum": "4"
          },
          "yarn.nodemanager.resource.memory-mb": {
            "maximum": "49152"
          },
          "yarn.scheduler.minimum-allocation-mb": {
            "maximum": "39424"
          },
          "yarn.nodemanager.resource.cpu-vcores": {
            "maximum": "12"
          },
          "yarn.scheduler.maximum-allocation-mb": {
            "maximum": "39424"
          }
        }
      }
    }

    clusterData = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, None)
    self.assertEquals(clusterData['hbaseRam'], 8)

    # Test when yarn_cgroups_enabled = true
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Test when yarn_cgroups_enabled = false
    services['configurations']['yarn-env']['properties']['yarn_cgroups_enabled'] = 'false'
    expected = {
      "yarn-env": {
        "properties": {
          "min_user_id": "500"
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.container-executor.group": "hadoop",
          "yarn.nodemanager.container-executor.class": "org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor",
          "yarn.nodemanager.linux-container-executor.cgroups.mount-path": "/cgroup",
          "yarn.nodemanager.container-executor.cgroups.mount": "true",
          "yarn.nodemanager.resource.memory-mb": "39424",
          "yarn.scheduler.minimum-allocation-mb": "3584",
          "yarn.scheduler.maximum-allocation-vcores": "4",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.nodemanager.resource.cpu-vcores": "4",
          "yarn.nodemanager.container-executor.cgroups.hierarchy": " /yarn",
          "yarn.scheduler.maximum-allocation-mb": "39424",
          "yarn.nodemanager.container-executor.resources-handler.class": "org.apache.hadoop.yarn.server.nodemanager.util.CgroupsLCEResourcesHandler"
        },
        "property_attributes": {
          "yarn.nodemanager.container-executor.cgroups.mount": {
            "delete": "true"
          },
          "yarn.nodemanager.container-executor.cgroups.hierarchy": {
            "delete": "true"
          },
          "yarn.nodemanager.linux-container-executor.cgroups.mount-path": {
            "delete": "true"
          },
          "yarn.scheduler.minimum-allocation-vcores": {
            "maximum": "4"
          },
          "yarn.scheduler.maximum-allocation-vcores": {
            "maximum": "4"
          },
          "yarn.nodemanager.resource.memory-mb": {
            "maximum": "49152"
          },
          "yarn.scheduler.minimum-allocation-mb": {
            "maximum": "39424"
          },
          "yarn.nodemanager.resource.cpu-vcores": {
            "maximum": "12"
          },
          "yarn.scheduler.maximum-allocation-mb": {
            "maximum": "39424"
          },
          "yarn.nodemanager.container-executor.resources-handler.class": {
            "delete": "true"
          }
        }
      }
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)
