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
from unittest import TestCase

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
          "tez.am.java.opts": "-server -Xmx1600m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC",
          "tez.task.resource.memory.mb": "768",
          "tez.runtime.io.sort.mb": "307",
          "tez.runtime.unordered.output.buffer.size-mb": "57"
        }
      }
    }
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData)
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
          "tez.am.java.opts": "-server -Xmx2480m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC",
          "tez.task.resource.memory.mb": "768",
          "tez.runtime.io.sort.mb": "307",
          "tez.runtime.unordered.output.buffer.size-mb": "57"
        }
      }
    }
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData)
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
          "tez.am.java.opts": "-server -Xmx1600m -Djava.net.preferIPv4Stack=true -XX:+UseNUMA -XX:+UseParallelGC",
          "tez.task.resource.memory.mb": "760",
          "tez.runtime.io.sort.mb": "304",
          "tez.runtime.unordered.output.buffer.size-mb": "57"
        }
      }
    }
    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData)
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
      }
    }
    expected = []  # No warnings
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = []  # No warnings
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = [{'config-name': 'dfs.http.policy',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "Invalid property value: WRONG_VALUE. Valid values are ['HTTP_ONLY', 'HTTPS_ONLY', 'HTTP_AND_HTTPS']",
                 'type': 'configuration'}]
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = [ ]
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = []
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = []
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = []
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = []
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = []
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = [{'config-name': 'dfs.data.transfer.protection',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "dfs.data.transfer.protection property can not be used when dfs.http.policy is "
                            "set to any value other then HTTPS_ONLY. Tip: When dfs.http.policy property is not defined, it defaults to HTTP_ONLY",
                 'type': 'configuration'}]
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = [{'config-name': 'dfs.data.transfer.protection',
                 'config-type': 'hdfs-site',
                 'level': 'WARN',
                 'message': "Invalid property value: WRONG_VALUE. Valid values are ['authentication', 'integrity', 'privacy'].",
                 'type': 'configuration'}]
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
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
      }
    }
    expected = []  # No warnings
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations)
    self.assertEquals(validation_problems, expected)

  def test_recommendYARNConfigurations(self):
    configurations = {}
    clusterData = {
      "cpu": 4,
      "containers" : 5,
      "ramPerContainer": 256
    }
    expected = {
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.resource.memory-mb": "1280",
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "1280",
          "yarn.nodemanager.resource.cpu-vcores": "4"
        }
      }
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData)
    self.assertEquals(configurations, expected)
