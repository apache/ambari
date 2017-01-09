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
import socket

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

  @patch('os.path.exists')
  @patch('os.path.isdir')
  @patch('os.listdir')
  def test_recommendTezConfigurations(self, os_listdir_mock, os_isdir_mock, os_exists_mock):

    os_exists_mock.return_value = True
    os_isdir_mock.return_value = True
    os_listdir_mock.return_value = ['TEZ{0.7.0.2.3.0.0-2155}']

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
          'tez.queue.name': 'default',
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
    services = {
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

    server_host = socket.getfqdn()
    for host in hosts["items"]:
      if server_host == host["Hosts"]["host_name"]:
        server_host = host["Hosts"]["public_host_name"]
    tez_ui_url =  "http://" + server_host + ":8080/#/main/views/TEZ/0.7.0.2.3.0.0-2155/TEZ_CLUSTER_INSTANCE"

    services['ambari-server-properties'] = {'api.ssl': 'false'}
    expected['tez-site']['properties']['tez.tez-ui.history-url.base'] = tez_ui_url

    self.stackAdvisor.recommendTezConfigurations(configurations, clusterData, services, hosts)
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
          'tez.queue.name': 'default',
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
    services = {
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
          'tez.queue.name': 'default',
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
    services = {
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


  def test_validateHDFSConfigurations(self):
    recommendedDefaults = {
      'dfs.datanode.du.reserved': '1024'
    }

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
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                     }],
                "configurations": {}
                }
    expected = []  # No warnings
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Unsecured cluster, unsecure ports
    properties = {  # hdfs-site
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                       },
                 }],
                "configurations": {}
            }
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, invalid dfs.http.policy value
    properties = {  # hdfs-site
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                   },
             }],
             "configurations": {}
        }
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, https address not defined
    properties = {  # hdfs-site
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
             }],
            "configurations": {}
        }
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, https address defined and secure
    properties = {  # hdfs-site
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
             }],
             "configurations": {}
        }
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, https address defined and non secure
    properties = {  # hdfs-site
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
             }],
             "configurations": {}
        }
    validation_problems = self.stackAdvisor.validateHDFSConfigurations(properties, recommendedDefaults, configurations, services, None)
    self.assertEquals(validation_problems, expected)

    # TEST CASE: Secure cluster, dfs.http.policy=HTTPS_ONLY, non secure dfs port, https property not defined
    properties = {  # hdfs-site
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
                    'dfs.datanode.du.reserved': '1024',
                    'dfs.datanode.data.dir': '/hadoop/hdfs/data',
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
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "HDFS"
        },
        },
      {
        "StackServices": {
          "service_name": "YARN"
        },
        },
      {
        "StackServices": {
          "service_name": "SLIDER"
        },
        }
    ]
    clusterData = {
      "cpu": 4,
      "containers" : 5,
      "ramPerContainer": 256
    }
    expected = {
      "yarn-env": {
        "properties": {
          "min_user_id": "500",
          'service_check.queue.name': 'default'
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.linux-container-executor.group": "hadoop",
          "yarn.nodemanager.resource.memory-mb": "1280",
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "1280",
          "yarn.scheduler.maximum-allocation-vcores": "4",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.nodemanager.resource.cpu-vcores": "4",
          "hadoop.registry.rm.enabled": "true"
        }
      }
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARKConfigurations(self):
    configurations = {}
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256
    }
    expected = {
      "spark-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      }
    }

    self.stackAdvisor.recommendSparkConfigurations(configurations, clusterData, services, None)
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
          "min_user_id": "500",
          'service_check.queue.name': 'default'
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.linux-container-executor.group": "hadoop",
          "yarn.nodemanager.resource.memory-mb": "1280",
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-vcores": "2",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.scheduler.maximum-allocation-mb": "1280",
          "yarn.nodemanager.resource.cpu-vcores": "2",
          "hadoop.registry.rm.enabled": "false"
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
          "name": "yarn.nodemanager.resource.memory-mb",
          "old_value": "512"
        },
        {
          "type": "yarn-site",
          "name": "yarn.scheduler.minimum-allocation-mb",
          "old_value": "512"
        },
        {
          "type": "yarn-site",
          "name": "yarn.scheduler.maximum-allocation-mb",
          "old_value": "512"
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
          "name": "yarn.nodemanager.resource.percentage-physical-cpu-limit",
          "old_value": "6"
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

    services.pop("changed-configurations", None)
    services["changed-configurations"] = [{
        "type": "yarn-site",
        "name": "yarn.nodemanager.resource.memory-mb",
        "old_value": "1280"
    }]
    services.pop("configurations", None)
    services["configurations"] = {"yarn-site": {"properties": {"yarn.nodemanager.resource.memory-mb": '4321'}}}

    expected["yarn-site"]["properties"]["yarn.nodemanager.resource.memory-mb"] = '4321'
    expected["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-mb"] = '4321'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.maximum-allocation-mb"]["maximum"] = '4321'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.minimum-allocation-mb"]["maximum"] = '4321'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    services["changed-configurations"].append({
        "type": "yarn-site",
        "name": "yarn.nodemanager.resource.cpu-vcores",
        "old_value": "7"
    })
    services.pop("configurations", None)
    services["configurations"] = {"yarn-site": {"properties": {"yarn.nodemanager.resource.cpu-vcores": '9', "yarn.nodemanager.resource.memory-mb": '4321'}}}
    expected["yarn-site"]["properties"]["yarn.nodemanager.resource.cpu-vcores"] = '9'
    expected["yarn-site"]["properties"]["yarn.scheduler.maximum-allocation-vcores"] = '9'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.maximum-allocation-vcores"]["maximum"] = '9'
    expected["yarn-site"]["property_attributes"]["yarn.scheduler.minimum-allocation-vcores"]["maximum"] = '9'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

  def test_multipleDependsOn(self):
    configurations = {
      "yarn-env": {
        "properties": {
          "min_user_id": "500"
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.resource.memory-mb": "1280",
          "yarn.scheduler.minimum-allocation-mb": "350",
          "yarn.scheduler.maximum-allocation-mb": "1000",
        },
      },
      "mapred-site": {
        "properties": {
          "mapreduce.map.memory.mb": "0",
          "mapreduce.reduce.memory.mb": "111"
        }
      }
    }
    clusterData = {
      "cpu": 4,
      "containers" : 5,
      "ramPerContainer": 256
    }

    services = {
      "configurations": configurations,
      "services": [],
      "changed-configurations": [
        {
          "type": "yarn-site",
          "name": "yarn.scheduler.maximum-allocation-mb",
          "old_value": "512"
        },
      ]

    }
    hosts = {}

    # immitate recommend-configuration-dependencies request with only "yarn.scheduler.maximum-allocation-mb" in "changed-configurations"
    self.stackAdvisor.allRequestedProperties = {'yarn-site': ['yarn.scheduler.maximum-allocation-mb'], 'mapred-site': ['mapreduce.map.memory.mb']}

    self.stackAdvisor.recommendMapReduce2Configurations(configurations, clusterData, services, hosts)

    # changed-configurations contain only "yarn.scheduler.maximum-allocation-mb".
    # Ensure that user provided value (350) for "yarn.scheduler.minimum-allocation-mb" is used.
    # The recommended default for "yarn.scheduler.minimum-allocation-mb" is 256.
    self.assertEquals(configurations['mapred-site']['properties']['mapreduce.map.memory.mb'], '350') # should not be 256

    # assert that not requested property was not changed
    self.assertEquals(configurations['mapred-site']['properties']['mapreduce.reduce.memory.mb'], '111')

  def test_recommendHiveConfigurationAttributes(self):
    self.maxDiff = None
    configurations = {
      "yarn-site": {
        "properties": {
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "8192"
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
          'hive.auto.convert.join.noconditionaltask.size': '214748364',
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
          'hive.metastore.uris' : 'thrift://c6402.ambari.apache.org:9083'
        },
       'property_attributes': {
         'hive.auto.convert.join.noconditionaltask.size': {'maximum': '644245094'},
         'hive.tez.container.size': {'maximum': '8192', 'minimum': '256'},
         'hive.server2.authentication.pam.services': {'delete': 'true'},
         'hive.server2.custom.authentication.class': {'delete': 'true'},
         'hive.server2.authentication.kerberos.principal': {'delete': 'true'},
         'hive.server2.authentication.kerberos.keytab': {'delete': 'true'},
         'hive.server2.authentication.ldap.url': {'delete': 'true'},
         'hive.server2.tez.default.queues': {
           "entries": [
             {
               "value": "queue1",
               "label": "queue1 queue"
             },
             {
               "value": "queue2",
               "label": "queue2 queue"
             }
           ]
          }
        }
      },
      'hiveserver2-site': {
        'properties': {
        },
        'property_attributes': {
         'hive.security.authorization.manager': {'delete': 'true'},
         'hive.security.authenticator.manager': {'delete': 'true'},
         'hive.conf.restricted.list': {'delete': 'true'}
        }
      },
      'webhcat-site': {
        'properties': {
          'templeton.hadoop.queue.name': 'queue2'
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
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/HIVE",
          "StackServices": {
            "service_name": "HIVE",
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
                "component_name": "HIVE_METASTORE",
                "display_name": "HiveServer2",
                "is_client": "false",
                "is_master": "true",
                "hostnames": [
                  "c6402.ambari.apache.org"
                ]
              },
              "dependencies": []
            }
            ,
          ],
        },
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "yarn.scheduler.capacity.root.queues": "queue1,queue2"
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
            "hive.security.authenticator.manager": "",
            "hive.conf.restricted.list": ""
          }
        }
      },
      "changed-configurations": [ ]
    }

    hiveService = {
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/HIVE",
          "StackServices": {
            "service_name": "HIVE",
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
                "component_name": "HIVE_SERVER",
                "display_name": "HiveServer2",
                "is_client": "false",
                "is_master": "true",
                "hostnames": [
                  "c6402.ambari.apache.org"
                ]
              },
              "dependencies": []
            },
            {
              "StackServiceComponents": {
                "advertise_version": "true",
                "cardinality": "1+",
                "component_category": "SLAVE",
                "component_name": "HIVE_CLIENT",
                "display_name": "Hive Client",
                "is_client": "true",
                "is_master": "false",
                "hostnames": [
                  "c6402.ambari.apache.org",
                  "c6403.ambari.apache.org"
                ]
              },
              "dependencies": []
            }
          ]
        },
      ],
      "configurations": {
        "hive-env": {
          "properties": {
            "hive.heapsize": "200",
            "hive.metastore.heapsize": "200",
            "hive.client.heapsize": "200"
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
            "hive.security.authenticator.manager": "",
            "hive.conf.restricted.list": ""
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

    #test recommendations
    services["configurations"]["hive-site"]["properties"]["hive.cbo.enable"] = "false"
    services["configurations"]["hive-env"]["properties"]["hive_security_authorization"] = "sqlstdauth"
    services["changed-configurations"] = [{"type": "hive-env", "name": "hive_security_authorization"}]
    expected["hive-env"]["properties"]["hive_security_authorization"] = "sqlstdauth"
    expected["hive-site"]["properties"]["hive.stats.fetch.partition.stats"]="false"
    expected["hive-site"]["properties"]["hive.stats.fetch.column.stats"]="false"
    expected["hive-site"]["properties"]["hive.security.authorization.enabled"]="true"
    expected["hive-site"]["properties"]["hive.server2.enable.doAs"]="false"
    expected["hive-site"]["properties"]["hive.security.metastore.authorization.manager"]="org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider,org.apache.hadoop.hive.ql.security.authorization.MetaStoreAuthzAPIAuthorizerEmbedOnly"
    expected["hiveserver2-site"]["properties"]["hive.security.authorization.enabled"]="true"
    expected["hiveserver2-site"]["properties"]["hive.security.authorization.manager"]="org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory"
    expected["hiveserver2-site"]["properties"]["hive.security.authenticator.manager"]="org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
    expected["hiveserver2-site"]["properties"]["hive.conf.restricted.list"]="hive.security.authenticator.manager,hive.security.authorization.manager,hive.security.metastore.authorization.manager,hive.security.metastore.authenticator.manager,hive.users.in.admin.role,hive.server2.xsrf.filter.enabled,hive.security.authorization.enabled"

    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)


    # test 'hive_security_authorization'=='sqlstdauth' => 'hive.server2.enable.doAs'=='false'
    services["configurations"]["hive-env"]["properties"]["hive_security_authorization"] = "none"
    expected["hive-env"]["properties"]["hive_security_authorization"] = "none"
    expected["hive-site"]["properties"]["hive.security.authorization.enabled"]="false"
    expected["hive-site"]["properties"]["hive.server2.enable.doAs"]="true"
    expected["hive-site"]["properties"]["hive.security.metastore.authorization.manager"]=\
      "org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider"
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # test 'hive.server2.tez.default.queues' leaf queues
    services["configurations"]['capacity-scheduler']['properties'] = {
            "capacity-scheduler" : "yarn.scheduler.capacity.maximum-am-resource-percent=0.2\n"
                                   "yarn.scheduler.capacity.maximum-applications=10000\n"
                                   "yarn.scheduler.capacity.node-locality-delay=40\n"
                                   "yarn.scheduler.capacity.queue-mappings-override.enable=false\n"
                                   "yarn.scheduler.capacity.resource-calculator=org.apache.hadoop.yarn.util.resource.DefaultResourceCalculator\n"
                                   "yarn.scheduler.capacity.root.accessible-node-labels=*\n"
                                   "yarn.scheduler.capacity.root.acl_administer_queue=*\n"
                                   "yarn.scheduler.capacity.root.capacity=100\n"
                                   "yarn.scheduler.capacity.root.default.a.a1.acl_administer_queue=*\n"
                                   "yarn.scheduler.capacity.root.default.a.a1.acl_submit_applications=*\n"
                                   "yarn.scheduler.capacity.root.default.a.a1.capacity=75\n"
                                   "yarn.scheduler.capacity.root.default.a.a1.maximum-capacity=100\n"
                                   "yarn.scheduler.capacity.root.default.a.a1.minimum-user-limit-percent=100\n"
                                   "yarn.scheduler.capacity.root.default.a.a1.ordering-policy=fifo\n"
                                   "yarn.scheduler.capacity.root.default.a.a1.state=RUNNING\n"
                                   "yarn.scheduler.capacity.root.default.a.a1.user-limit-factor=1\n"
                                   "yarn.scheduler.capacity.root.default.a.a2.acl_administer_queue=*\n"
                                   "yarn.scheduler.capacity.root.default.a.a2.acl_submit_applications=*\n"
                                   "yarn.scheduler.capacity.root.default.a.a2.capacity=25\n"
                                   "yarn.scheduler.capacity.root.default.a.a2.maximum-capacity=25\n"
                                   "yarn.scheduler.capacity.root.default.a.a2.minimum-user-limit-percent=100\n"
                                   "yarn.scheduler.capacity.root.default.a.a2.ordering-policy=fifo\n"
                                   "yarn.scheduler.capacity.root.default.a.a2.state=RUNNING\n"
                                   "yarn.scheduler.capacity.root.default.a.a2.user-limit-factor=1\n"
                                   "yarn.scheduler.capacity.root.default.a.acl_administer_queue=*\n"
                                   "yarn.scheduler.capacity.root.default.a.acl_submit_applications=*\n"
                                   "yarn.scheduler.capacity.root.default.a.capacity=50\n"
                                   "yarn.scheduler.capacity.root.default.a.maximum-capacity=100\n"
                                   "yarn.scheduler.capacity.root.default.a.minimum-user-limit-percent=100\n"
                                   "yarn.scheduler.capacity.root.default.a.ordering-policy=fifo\n"
                                   "yarn.scheduler.capacity.root.default.a.queues=a1,a2\n"
                                   "yarn.scheduler.capacity.root.default.a.state=RUNNING\n"
                                   "yarn.scheduler.capacity.root.default.a.user-limit-factor=1\n"
                                   "yarn.scheduler.capacity.root.default.acl_submit_applications=*\n"
                                   "yarn.scheduler.capacity.root.default.b.acl_administer_queue=*\n"
                                   "yarn.scheduler.capacity.root.default.b.acl_submit_applications=*\n"
                                   "yarn.scheduler.capacity.root.default.b.capacity=50\n"
                                   "yarn.scheduler.capacity.root.default.b.maximum-capacity=50\n"
                                   "yarn.scheduler.capacity.root.default.b.minimum-user-limit-percent=100\n"
                                   "yarn.scheduler.capacity.root.default.b.ordering-policy=fifo\n"
                                   "yarn.scheduler.capacity.root.default.b.state=RUNNING\n"
                                   "yarn.scheduler.capacity.root.default.b.user-limit-factor=1\n"
                                   "yarn.scheduler.capacity.root.default.capacity=100\n"
                                   "yarn.scheduler.capacity.root.default.maximum-capacity=100\n"
                                   "yarn.scheduler.capacity.root.default.queues=a,b\n"
                                   "yarn.scheduler.capacity.root.default.state=RUNNING\n"
                                   "yarn.scheduler.capacity.root.default.user-limit-factor=1\n"
                                   "yarn.scheduler.capacity.root.queues=default"}

    expected['hive-site']['properties']['hive.server2.tez.default.queues'] = 'a1,a2,b'
    expected['hive-site']['property_attributes']['hive.server2.tez.default.queues'] = {
           'entries': [{'value': 'a1', 'label': 'a1 queue'}, {'value': 'a2', 'label': 'a2 queue'}, {'value': 'b', 'label': 'b queue'}]
          }
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hive-site']['property_attributes']['hive.server2.tez.default.queues'], expected['hive-site']['property_attributes']['hive.server2.tez.default.queues'])
    self.assertEquals(configurations['hive-site']['properties']['hive.server2.tez.default.queues'], expected['hive-site']['properties']['hive.server2.tez.default.queues'])

    # Hive heapsize properties
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, hiveService, hosts)

    # Recommended default values
    self.assertEquals(configurations["hive-env"]["properties"]["hive.metastore.heapsize"], "512")
    self.assertEquals(configurations["hive-env"]["properties"]["hive.heapsize"], "703")
    self.assertEquals(configurations["hive-env"]["properties"]["hive.client.heapsize"], "1024")

    # Recommended attributes for maximum values, minimum values defined in stack definition
    self.assertEquals(configurations["hive-env"]["property_attributes"]["hive.heapsize"]["maximum"], "1877")
    self.assertEquals(configurations["hive-env"]["property_attributes"]["hive.metastore.heapsize"]["maximum"], "1877")
    self.assertEquals(configurations["hive-env"]["property_attributes"]["hive.client.heapsize"]["maximum"], "1877")

    # test 'hive_security_authorization'=='ranger'
    services["configurations"]["hive-env"]["properties"]["hive_security_authorization"] = "ranger"
    expected["hiveserver2-site"]["properties"]["hive.security.authenticator.manager"] = "org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator"
    expected["hiveserver2-site"]["properties"]["hive.security.authorization.manager"] = "com.xasecure.authorization.hive.authorizer.XaSecureHiveAuthorizerFactory"
    expected["hiveserver2-site"]["properties"]["hive.security.authorization.enabled"] = "true"
    expected["hiveserver2-site"]["properties"]["hive.conf.restricted.list"]="hive.security.authenticator.manager,hive.security.authorization.manager,hive.security.metastore.authorization.manager,hive.security.metastore.authenticator.manager,hive.users.in.admin.role,hive.server2.xsrf.filter.enabled,hive.security.authorization.enabled"
    self.stackAdvisor.recommendHIVEConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hiveserver2-site'], expected["hiveserver2-site"])


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
      },
      "cluster-env": {
        "properties": {
          "user_group": "hadoopcustom",
          }
      }
    }
    clusterData = {
      "cpu": 4,
      "containers" : 7,
      "ramPerContainer": 256,
      "totalAvailableRam": 4096,
    }
    expected = {
      "cluster-env": {
        "properties": {
          "user_group": "hadoopcustom"
        }
      },
      "yarn-env": {
        "properties": {
          "min_user_id": "500",
          'service_check.queue.name': 'default'
        }
      },
      "mapred-site": {
        "properties": {
          'mapreduce.job.queuename': 'default',
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
          "yarn.nodemanager.linux-container-executor.group": "hadoopcustom",
          "yarn.nodemanager.resource.memory-mb": "1792",
          "yarn.scheduler.minimum-allocation-mb": "100",
          "yarn.scheduler.maximum-allocation-vcores": "1",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.scheduler.maximum-allocation-mb": "1792",
          "yarn.nodemanager.resource.cpu-vcores": "1",
          "hadoop.registry.rm.enabled": "false"
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
          "name": "yarn.scheduler.minimum-allocation-mb",
          "old_value": "512"
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
          "min_user_id": "500",
          'service_check.queue.name': 'default'
        }
      },
      "mapred-site": {
        "properties": {
          'mapreduce.job.queuename': 'default',
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
          "yarn.nodemanager.linux-container-executor.group": "hadoop",
          "yarn.nodemanager.resource.memory-mb": "1280",
          "yarn.scheduler.minimum-allocation-mb": "100",
          "yarn.scheduler.maximum-allocation-vcores": "1",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.scheduler.maximum-allocation-mb": "1280",
          "yarn.nodemanager.resource.cpu-vcores": "1",
          "hadoop.registry.rm.enabled": "false"
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
          "name": "yarn.scheduler.minimum-allocation-mb",
          "old_value": "512"
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
                "min_user_id": "500",
                'service_check.queue.name': 'default'
            }
        },
        "mapred-site": {
            "properties": {
                'mapreduce.job.queuename': 'default',
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
                "yarn.nodemanager.linux-container-executor.group": "hadoop",
                "yarn.nodemanager.resource.memory-mb": "1280",
                "yarn.scheduler.minimum-allocation-mb": "700",
                "yarn.scheduler.maximum-allocation-vcores": "1",
                "yarn.scheduler.minimum-allocation-vcores": "1",
                "yarn.scheduler.maximum-allocation-mb": "1280",
                "yarn.nodemanager.resource.cpu-vcores": "1",
                "hadoop.registry.rm.enabled": "false"
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
      }],
      "configurations": []
    }
    hosts = {
      "items": [{
        "Hosts": {
          "host_name": "host1",

        }
      }]
    }

    # 1-node cluster
    expected = {
      "ams-hbase-env": {
        "properties": {
          "hbase_master_xmn_size": "192",
          "hbase_master_heapsize": "512",
          "hbase_regionserver_heapsize": "768"
        }
      },
      "ams-grafana-env": {
        "properties" : {},
        "property_attributes": {
          "metrics_grafana_password": {
            "visible": "false"
          }
        }
      },
      "ams-env": {
        "properties": {
          "metrics_collector_heapsize": "512",
        }
      },
      "ams-hbase-site": {
        "properties": {
          "phoenix.coprocessor.maxMetaDataCacheSize": "20480000",
          "hbase.regionserver.global.memstore.lowerLimit": "0.3",
          "hbase.regionserver.global.memstore.upperLimit": "0.35",
          "hbase.hregion.memstore.flush.size": "134217728",
          "hfile.block.cache.size": "0.3",
          "hbase.cluster.distributed": "false",
          "hbase.rootdir": "file:///var/lib/ambari-metrics-collector/hbase",
          "hbase.tmp.dir": "/var/lib/ambari-metrics-collector/hbase-tmp",
          "hbase.zookeeper.property.clientPort": "61181",
        }
      },
      "ams-site": {
        "properties": {
          "timeline.metrics.cluster.aggregate.splitpoints": "master.FileSystem.MetaHlogSplitTime_75th_percentile",
          "timeline.metrics.host.aggregate.splitpoints": "master.FileSystem.MetaHlogSplitTime_75th_percentile",
          "timeline.metrics.service.handler.thread.count": "20",
          'timeline.metrics.service.webapp.address': '0.0.0.0:6188',
          'timeline.metrics.service.watcher.disabled': 'false',
          'timeline.metrics.cache.size': '100',
          'timeline.metrics.cache.commit.interval': '10'
        }
      }
    }
    self.stackAdvisor.recommendAmsConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # 100-nodes cluster, but still only 1 sink (METRICS_COLLECTOR)
    for i in range(2, 201):
      hosts['items'].extend([{
        "Hosts": {
          "host_name": "host" + str(i)
          }
      }])

    services['services'] = [
      {
        "StackServices": {
          "service_name": "AMBARI_METRICS"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "METRICS_COLLECTOR",
              "hostnames": ["host1"]
            }
          },
          {
            "StackServiceComponents": {
              "component_name": "METRICS_MONITOR",
              "hostnames": ["host" + str(i) for i in range(1, 201)]
            }
          }
        ]
      }
    ]

    expected["ams-site"]['properties']['timeline.metrics.cache.size'] = '500'
    expected["ams-site"]['properties']['timeline.metrics.cache.commit.interval'] = '7'
    expected["ams-hbase-env"]['properties']['hbase_master_heapsize'] = '1408'
    expected["ams-hbase-env"]['properties']['hbase_master_xmn_size'] = '320'
    expected["ams-env"]['properties']['metrics_collector_heapsize'] = '512'

    self.stackAdvisor.recommendAmsConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Still 100 nodes, but with HDFS and YARN services installed on all nodes
    services['services'] = [
      {
        "StackServices": {
          "service_name": "HDFS"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "NAMENODE",
              "hostnames": ["host1"]
            }
          },
          {
            "StackServiceComponents": {
              "component_name": "DATANODE",
              "hostnames": ["host" + str(i) for i in range(1, 201)]
            }
          }
        ]
      },
      {
        "StackServices": {
          "service_name": "YARN"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "RESOURCEMANAGER",
              "hostnames": ["host1"]
            }
          },
          {
            "StackServiceComponents": {
              "component_name": "NODEMANAGER",
              "hostnames": ["host" + str(i) for i in range(1, 201)]
            }
          }
        ]
      },
      {
        "StackServices": {
          "service_name": "AMBARI_METRICS"
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "METRICS_COLLECTOR",
              "hostnames": ["host1"]
            }
          },
          {
            "StackServiceComponents": {
              "component_name": "METRICS_MONITOR",
              "hostnames": ["host" + str(i) for i in range(1, 201)]
            }
          }
        ]
      }

    ]
    expected["ams-site"]['properties']['timeline.metrics.host.aggregate.splitpoints'] = 'master.Server.numDeadRegionServers'
    expected["ams-site"]['properties']['timeline.metrics.cluster.aggregate.splitpoints'] = 'master.Server.numDeadRegionServers'

    expected["ams-site"]['properties']['timeline.metrics.cache.size'] = '500'
    expected["ams-site"]['properties']['timeline.metrics.cache.commit.interval'] = '7'
    expected["ams-hbase-env"]['properties']['hbase_master_heapsize'] = '2432'
    expected["ams-hbase-env"]['properties']['hbase_master_xmn_size'] = '512'
    expected["ams-env"]['properties']['metrics_collector_heapsize'] = '640'

    self.stackAdvisor.recommendAmsConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Test splitpoints, AMS embedded mode
    services['changed-configurations'] = [
      {
        "type": "ams-hbase-env",
        "name": "hbase_master_heapsize",
        "old_value": "1024"
      }
    ]

    services['configurations'] = {
      'core-site': {'properties': {}},
      'ams-site': {'properties': {}},
      'ams-hbase-site': {'properties': {}},
      'ams-hbase-env': {'properties': {}}
    }

    # Embedded mode, 512m master heapsize, no splitpoints recommended
    services["configurations"]['ams-hbase-env']['properties']['hbase_master_heapsize'] = '512'
    services["configurations"]['ams-hbase-site']['properties']['hbase.regionserver.global.memstore.upperLimit'] = '0.4'
    services["configurations"]['ams-hbase-site']['properties']['hbase.hregion.memstore.flush.size'] = '134217728'

    expected['ams-site']['properties']['timeline.metrics.host.aggregate.splitpoints'] = 'master.Server.numDeadRegionServers'
    expected['ams-site']['properties']['timeline.metrics.cluster.aggregate.splitpoints'] = 'master.Server.numDeadRegionServers'
    expected['ams-hbase-env']['properties']['hbase_master_heapsize'] = '512'

    self.stackAdvisor.recommendAmsConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Embedded mode, 4096m master heapsize, some splitpoints recommended
    services["configurations"]['ams-hbase-env']['properties']['hbase_master_heapsize'] = '4096'
    expected['ams-site']['properties']['timeline.metrics.host.aggregate.splitpoints'] = 'dfs.datanode.WriteBlockOpNumOps,' \
                                                                                        'mapred.ShuffleMetrics.ShuffleOutputsFailed,' \
                                                                                        'read_bps,' \
                                                                                        'rpcdetailed.rpcdetailed.GetContainerStatusesAvgTime'
    expected['ams-site']['properties']['timeline.metrics.cluster.aggregate.splitpoints'] = 'master.Server.numDeadRegionServers'
    expected['ams-hbase-env']['properties']['hbase_master_heapsize'] = '4096'
    self.stackAdvisor.recommendAmsConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Embedded mode, 8192m master heapsize, more splitpoints recommended
    services["configurations"]['ams-hbase-env']['properties']['hbase_master_heapsize'] = '8192'
    expected['ams-hbase-env']['properties']['hbase_master_heapsize'] = '8192'
    self.stackAdvisor.recommendAmsConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(len(configurations['ams-site']['properties']['timeline.metrics.host.aggregate.splitpoints'].split(',')), 13)
    self.assertEquals(len(configurations['ams-site']['properties']['timeline.metrics.cluster.aggregate.splitpoints'].split(',')), 2)

    # Test splitpoints, AMS distributed mode
    services['changed-configurations'] = [
      {
        "type": "ams-hbase-env",
        "name": "hbase_regionserver_heapsize",
        "old_value": "512"
      }
    ]
    services["configurations"]['ams-site']['properties']['timeline.metrics.service.operation.mode'] = 'distributed'
    services["configurations"]["core-site"]["properties"]["fs.defaultFS"] = 'hdfs://host1:8020'
    expected['ams-hbase-site']['properties']['hbase.cluster.distributed'] = 'true'
    expected['ams-hbase-site']['properties']['hbase.rootdir'] = '/user/ams/hbase'
    expected['ams-hbase-site']['properties']['hbase.zookeeper.property.clientPort'] = '2181'
    expected['ams-hbase-env']['properties']['hbase_master_heapsize'] = '512'
    expected['ams-hbase-site']['properties']['dfs.client.read.shortcircuit'] = 'true'

    # Distributed mode, low memory, no splitpoints recommended
    services["configurations"]['ams-hbase-env']['properties']['hbase_regionserver_heapsize'] = '512'
    expected['ams-site']['properties']['timeline.metrics.host.aggregate.splitpoints'] = 'master.Server.numDeadRegionServers'
    expected['ams-site']['properties']['timeline.metrics.cluster.aggregate.splitpoints'] = 'master.Server.numDeadRegionServers'
    expected['ams-hbase-env']['properties']['hbase_regionserver_heapsize'] = '512'
    expected["ams-hbase-env"]['properties']['hbase_master_xmn_size'] = '102'
    expected['ams-hbase-env']['properties']['regionserver_xmn_size'] = '384'
    expected['ams-site']['properties']['timeline.metrics.service.watcher.disabled'] = 'true'
    self.stackAdvisor.recommendAmsConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    # Distributed mode, more memory, more splitpoints recommended
    services["configurations"]['ams-hbase-env']['properties']['hbase_regionserver_heapsize'] = '8192'
    expected['ams-hbase-env']['properties']['hbase_regionserver_heapsize'] = '8192'
    self.stackAdvisor.recommendAmsConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(len(configurations['ams-site']['properties']['timeline.metrics.host.aggregate.splitpoints'].split(',')), 13)
    self.assertEquals(len(configurations['ams-site']['properties']['timeline.metrics.cluster.aggregate.splitpoints'].split(',')), 2)

    # 2000-nodes cluster
    for i in range(202, 2001):
        hosts['items'].extend([{
            "Hosts": {
                "host_name": "host" + str(i)
            }
        }])

    services['services'] = [
        {
            "StackServices": {
                "service_name": "AMBARI_METRICS"
            },
            "components": [
                {
                    "StackServiceComponents": {
                        "component_name": "METRICS_COLLECTOR",
                        "hostnames": ["host1"]
                    }
                },
                {
                    "StackServiceComponents": {
                        "component_name": "METRICS_MONITOR",
                        "hostnames": ["host" + str(i) for i in range(1, 2001)]
                    }
                }
            ]
        }
    ]

    self.stackAdvisor.recommendAmsConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations["ams-site"]['properties']['timeline.metrics.cache.size'], '700')
    self.assertEquals(configurations["ams-site"]['properties']['timeline.metrics.cache.commit.interval'], '5')

  def test_recommendHbaseConfigurations(self):
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
        "hbase-site": {
          "properties": {
            "hbase.superuser": "hbase"
          }
        },
        "hbase-env": {
          "properties": {
            "hbase_user": "hbase123"
          }
        }
      }
    }
    expected = {
      'hbase-site': {
        'properties': {
          'hbase.superuser': 'hbase123'
        }
      },
      "hbase-env": {
        "properties": {
          "hbase_master_heapsize": "1024",
          "hbase_regionserver_heapsize": "8192",
          }
      }
    }

    clusterData = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, None)
    self.assertEquals(clusterData['hbaseRam'], 8)

    self.stackAdvisor.recommendHbaseConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendKnoxConfigurations(self):
    servicesList = ["KNOX"]
    configurations = {}
    components = []

    services_without_auth_provider_ranger_plugin_enabled = {
      "services" : [
      ],
      "configurations": {
        "ranger-env": {
          "properties": {
            "ranger-knox-plugin-enabled" : "Yes"
          }
        },
        "ranger-knox-plugin-properties": {
          "properties": {

          }
        },
        "topology": {
          "properties": {
            "content" : "<topology> <gateway>  </gateway> </topology>"
          }
        }
      }
    }
    services_without_auth_provider_ranger_plugin_disabled = {
      "services" : [
      ],
      "configurations": {
        "ranger-env": {
          "properties": {
            "ranger-knox-plugin-enabled" : "No"
          }
        },
        "ranger-knox-plugin-properties": {
          "properties": {

          }
        },
        "topology": {
          "properties": {
            "content" : "<topology> <gateway>  </gateway> </topology>"
          }
        }
      }
    }
    services_with_auth_provider_ranger_plugin_disabled = {
      "services" : [
      ],
      "configurations": {
        "ranger-env": {
          "properties": {
            "ranger-knox-plugin-enabled" : "No"
          }
        },
        "ranger-knox-plugin-properties": {
          "properties": {

          }
        },
        "topology": {
          "properties": {
            "content" : "<topology> <gateway> <provider> <role>aaa</role><name>r</name><enabled>t</enabled></provider>"
                        " <provider><role>authorization</role><name>XASecurePDPKnox</name><enabled>true</enabled> </provider>"
                        "<provider><role>bbb</role><name>y</name><enabled>u</enabled></provider> </gateway> </topology>"
          }
        }
      }
    }
    services_with_auth_provider_ranger_plugin_enabled = {
      "services" : [
      ],
      "configurations": {
        "ranger-env": {
          "properties": {
            "ranger-knox-plugin-enabled" : "Yes"
          }
        },
        "ranger-knox-plugin-properties": {
          "properties": {

          }
        },
        "topology": {
          "properties": {
            "content" : "<topology> <gateway> <provider><role>authorization</role><name>AclsAuthz</name><enabled>true</enabled></provider> </gateway> </topology>"
          }
        }
      }
    }
    expected1 = {'ranger-knox-plugin-properties':
                  {'properties':
                     {'ranger-knox-plugin-enabled': 'Yes'}},
                'topology':
                  {'properties':
                     {'content': '<topology> <gateway>  <provider><role>authorization</role><name>XASecurePDPKnox</name><enabled>true</enabled></provider></gateway> </topology>'}}}

    expected2 = {'ranger-knox-plugin-properties':
                   {'properties':
                      {'ranger-knox-plugin-enabled': 'No'}},
                 'topology':
                   {'properties':
                      {'content': '<topology> <gateway>  <provider><role>authorization</role><name>AclsAuthz</name><enabled>true</enabled></provider></gateway> </topology>'}}}
    expected3 = {'ranger-knox-plugin-properties':
                   {'properties':
                      {'ranger-knox-plugin-enabled': 'No'}},
                 'topology':
                   {'properties':
                      {'content': '<topology> <gateway> <provider> <role>aaa</role><name>r</name><enabled>t</enabled></provider> <provider><role>authorization</role><name>AclsAuthz</name><enabled>true</enabled> </provider><provider><role>bbb</role><name>y</name><enabled>u</enabled></provider> </gateway> </topology>'}}}

    expected4 = {'ranger-knox-plugin-properties':
                   {'properties':
                      {'ranger-knox-plugin-enabled': 'Yes'}},
                 'topology':
                   {'properties':
                      {'content': '<topology> <gateway> <provider><role>authorization</role><name>XASecurePDPKnox</name><enabled>true</enabled></provider> </gateway> </topology>'}}}

    self.stackAdvisor.recommendKnoxConfigurations(configurations, None, services_without_auth_provider_ranger_plugin_enabled, None)
    self.assertEquals(configurations, expected1)

    self.stackAdvisor.recommendKnoxConfigurations(configurations, None, services_without_auth_provider_ranger_plugin_disabled, None)
    self.assertEquals(configurations, expected2)

    self.stackAdvisor.recommendKnoxConfigurations(configurations, None, services_with_auth_provider_ranger_plugin_disabled, None)
    self.assertEquals(configurations, expected3)

    self.stackAdvisor.recommendKnoxConfigurations(configurations, None, services_with_auth_provider_ranger_plugin_enabled, None)
    self.assertEquals(configurations, expected4)


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
        {
          "StackServices": {
            "service_name": "RANGER",
            "service_version": "0.4.0"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "RANGER_ADMIN",
                "hostnames": ["host1"]
              }
            }
          ]
        }
      ],
      "Versions": {
        "stack_version": "2.2"
      },
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
            "hbase.coprocessor.regionserver.classes": "",
            "hbase.coprocessor.region.classes": "{{hbase_coprocessor_region_classes}}"
          }
        },
        "ranger-hbase-plugin-properties": {
          "properties": {
            "ranger-hbase-plugin-enabled" : "No"
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
          "hbase.coprocessor.region.classes": "org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint",
          "hbase.coprocessor.regionserver.classes": "",
          "hbase.coprocessor.master.classes": "",
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
          "hbase_master_heapsize": "1024",
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
    self.assertEquals(configurations, expected, "Test when Phoenix sql is enabled")

    # Test when phoenix_sql_enabled = false
    services['configurations']['hbase-env']['properties']['phoenix_sql_enabled'] = 'false'
    expected['hbase-site']['properties']['hbase.regionserver.wal.codec'] = 'org.apache.hadoop.hbase.regionserver.wal.WALCellCodec'
    expected['hbase-site']['property_attributes']['hbase.rpc.controllerfactory.class'] = {'delete': 'true'}
    expected['hbase-site']['property_attributes']['hbase.coprocessor.regionserver.classes'] = {'delete': 'true'}
    expected['hbase-site']['property_attributes']['phoenix.functions.allowUserDefinedFunctions'] = {'delete': 'true'}
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected, "Test when Phoenix sql is disabled")

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
    self.assertEquals(configurations, expected, "Test with Phoenix disabled")

    # Test when hbase.security.authentication = kerberos
    services['configurations']['hbase-site']['properties']['hbase.security.authentication'] = 'kerberos'
    expected['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint'
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected, "Test with Kerberos enabled")

    # Test when hbase.security.authentication = simple
    services['configurations']['hbase-site']['properties']['hbase.security.authentication'] = 'simple'
    expected['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint'
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected, "Test with Kerberos disabled")

    # Test when Ranger plugin HBase is enabled in non-kerberos environment
    configurations['hbase-site']['properties'].pop('hbase.coprocessor.region.classes', None)
    configurations['hbase-site']['properties'].pop('hbase.coprocessor.master.classes', None)
    configurations['hbase-site']['properties'].pop('hbase.coprocessor.regionserver.classes', None)
    services['configurations']['ranger-hbase-plugin-properties']['properties']['ranger-hbase-plugin-enabled'] = 'Yes'
    services['configurations']['hbase-site']['properties']['hbase.security.authentication'] = 'simple'
    services['configurations']['hbase-site']['properties']['hbase.security.authorization'] = 'false'
    services['configurations']['hbase-site']['properties']['hbase.coprocessor.region.classes'] = ''
    services['configurations']['hbase-site']['properties']['hbase.coprocessor.master.classes'] = ''

    expected['hbase-site']['properties']['hbase.security.authorization'] = "true"
    expected['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor'
    expected['hbase-site']['properties']['hbase.coprocessor.master.classes'] = 'com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor'
    expected['hbase-site']['properties']['hbase.coprocessor.regionserver.classes'] = 'com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor'
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected, "Test when Ranger plugin HBase is enabled in non-kerberos environment")

    # Test when hbase.security.authentication = kerberos AND class already there
    configurations['hbase-site']['properties'].pop('hbase.coprocessor.region.classes', None)
    configurations['hbase-site']['properties'].pop('hbase.coprocessor.master.classes', None)
    configurations['hbase-site']['properties'].pop('hbase.coprocessor.regionserver.classes', None)
    configurations['hbase-site']['properties'].pop('hbase.security.authorization', None)
    services['configurations']['ranger-hbase-plugin-properties']['properties']['ranger-hbase-plugin-enabled'] = 'No'
    services['configurations']['hbase-site']['properties']['hbase.security.authorization'] = 'false'
    services['configurations']['hbase-site']['properties']['hbase.security.authentication'] = 'kerberos'
    services['configurations']['hbase-site']['properties']['hbase.coprocessor.master.classes'] = ''
    services['configurations']['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'a.b.c.d, {{hbase_coprocessor_region_classes}}'
    expected['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'a.b.c.d,org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint'
    expected['hbase-site']['properties']['hbase.coprocessor.master.classes'] = ''
    expected['hbase-site']['properties']['hbase.coprocessor.regionserver.classes'] = ''
    del expected['hbase-site']['properties']['hbase.security.authorization']
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected, "Test with Kerberos enabled and hbase.coprocessor.region.classes predefined")

    # Test when hbase.security.authentication = kerberos AND authorization = true
    configurations['hbase-site']['properties'].pop('hbase.coprocessor.region.classes', None)
    services['configurations']['hbase-site']['properties'].pop('hbase.coprocessor.region.classes', None)
    services['configurations']['hbase-site']['properties']['hbase.security.authentication'] = 'kerberos'
    services['configurations']['hbase-site']['properties']['hbase.security.authorization'] = 'true'
    expected['hbase-site']['properties']['hbase.coprocessor.master.classes'] = "org.apache.hadoop.hbase.security.access.AccessController"
    expected['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'org.apache.hadoop.hbase.security.access.AccessController,org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint'
    expected['hbase-site']['properties']['hbase.coprocessor.regionserver.classes'] = "org.apache.hadoop.hbase.security.access.AccessController"
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected, "Test with Kerberos enabled and authorization is true")

    # Test when Ranger plugin HBase is enabled in kerberos environment
    configurations['hbase-site']['properties'].pop('hbase.coprocessor.region.classes', None)
    services['configurations']['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'org.apache.hadoop.hbase.security.access.AccessController'
    services['configurations']['hbase-site']['properties']['hbase.coprocessor.master.classes'] = 'org.apache.hadoop.hbase.security.access.AccessController'
    services['configurations']['hbase-site']['properties']['hbase.security.authentication'] = 'kerberos'
    services['configurations']['hbase-site']['properties']['hbase.security.authorization'] = 'false'
    services['configurations']['ranger-hbase-plugin-properties']['properties']['ranger-hbase-plugin-enabled'] = 'Yes'
    expected['hbase-site']['properties']['hbase.security.authorization']  = 'true'
    expected['hbase-site']['properties']['hbase.coprocessor.master.classes'] = 'com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor'
    expected['hbase-site']['properties']['hbase.coprocessor.regionserver.classes'] = "com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor"
    expected['hbase-site']['properties']['hbase.coprocessor.region.classes'] = 'org.apache.hadoop.hbase.security.token.TokenProvider,org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,com.xasecure.authorization.hbase.XaSecureAuthorizationCoprocessor'
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected, "Test with Kerberos enabled and HBase ranger plugin enabled")

    # Test - default recommendations should have certain configs deleted. HAS TO BE LAST TEST.
    services["configurations"] = {"hbase-site": {"properties": {"phoenix.functions.allowUserDefinedFunctions": '', "hbase.rpc.controllerfactory.class": ''}}}
    configurations = {}
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['hbase-site']['property_attributes']['phoenix.functions.allowUserDefinedFunctions'], {'delete': 'true'})
    self.assertEquals(configurations['hbase-site']['property_attributes']['hbase.rpc.controllerfactory.class'], {'delete': 'true'})
    self.assertEquals(configurations['hbase-site']['properties']['hbase.regionserver.wal.codec'], "org.apache.hadoop.hbase.regionserver.wal.WALCellCodec")


  def test_recommendStormConfigurations(self):
    configurations = {}
    clusterData = {}
    services = {
      "services":
        [
          {
            "StackServices": {
              "service_name" : "STORM",
              "service_version" : "2.6.0.2.2"
            }
          },
          {
            "StackServices": {
              "service_name": "RANGER",
              "service_version": "0.4.0"
            },
            "components": [
              {
                "StackServiceComponents": {
                  "component_name": "RANGER_ADMIN",
                  "hostnames": ["host1"]
                }
              }
            ]
          }
        ],
      "Versions": {
        "stack_version": "2.2"
      },
      "configurations": {
        "storm-site": {
          "properties": {
            "nimbus.authorizer" : "backtype.storm.security.auth.authorizer.SimpleACLAuthorizer"
          },
          "property_attributes": {}
        },
        "ranger-storm-plugin-properties": {
          "properties": {
            "ranger-storm-plugin-enabled": "No"
          }
        }
      }
    }

    # Test nimbus.authorizer with Ranger Storm plugin disabled in non-kerberos environment
    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['storm-site']['property_attributes']['nimbus.authorizer'], {'delete': 'true'}, "Test nimbus.authorizer with Ranger Storm plugin disabled in non-kerberos environment")

    # Test nimbus.authorizer with Ranger Storm plugin enabled in non-kerberos environment
    configurations['storm-site']['properties'] = {}
    configurations['storm-site']['property_attributes'] = {}
    services['configurations']['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled'] = 'Yes'
    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['storm-site']['property_attributes']['nimbus.authorizer'], {'delete': 'true'}, "Test nimbus.authorizer with Ranger Storm plugin enabled in non-kerberos environment")

    # Test nimbus.authorizer with Ranger Storm plugin being enabled in kerberos environment
    configurations['storm-site']['properties'] = {}
    configurations['storm-site']['property_attributes'] = {}
    services['configurations']['storm-site']['properties']['nimbus.authorizer'] = ''
    services['configurations']['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled'] = 'Yes'
    services['configurations']['storm-site']['properties']['storm.zookeeper.superACL'] = 'sasl:{{storm_bare_jaas_principal}}'
    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['storm-site']['properties']['nimbus.authorizer'], 'com.xasecure.authorization.storm.authorizer.XaSecureStormAuthorizer', "Test nimbus.authorizer with Ranger Storm plugin enabled in kerberos environment")

    # Test nimbus.authorizer with Ranger Storm plugin being disabled in kerberos environment
    configurations['storm-site']['properties'] = {}
    configurations['storm-site']['property_attributes'] = {}
    services['configurations']['ranger-storm-plugin-properties']['properties']['ranger-storm-plugin-enabled'] = 'No'
    services['configurations']['storm-site']['properties']['storm.zookeeper.superACL'] = 'sasl:{{storm_bare_jaas_principal}}'
    services['configurations']['storm-site']['properties']['nimbus.authorizer'] = 'com.xasecure.authorization.storm.authorizer.XaSecureStormAuthorizer'
    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['storm-site']['properties']['nimbus.authorizer'], 'backtype.storm.security.auth.authorizer.SimpleACLAuthorizer', "Test nimbus.authorizer with Ranger Storm plugin being disabled in kerberos environment")


  def test_recommendHDFSConfigurations(self):
    configurations = {
      'ranger-hdfs-plugin-properties':{
        "properties": {"ranger-hdfs-plugin-enabled":"Yes"}
      },
      'hdfs-site': {
        "properties": {"dfs.datanode.data.dir": "/path/1,/path/2,/path/3,/path/4"}
      },
      "hadoop-env": {
        "properties": {"hdfs_user": "hdfs"}
      }
    }
    clusterData = {
      "totalAvailableRam": 2048,
      "hBaseInstalled": True,
      "hbaseRam": 112,
      "reservedRam": 128
    }
    ambariHostName = socket.getfqdn()
    expected = {
      'hadoop-env': {
        'properties': {
          'namenode_heapsize': '1024',
          'namenode_opt_newsize' : '128',
          'namenode_opt_maxnewsize' : '128',
          "hdfs_user": "hdfs"
        },
        'property_attributes': {
          'dtnode_heapsize': {'maximum': '2048'},
          'namenode_heapsize': {'maximum': '10240'}
        }
      },
      'hdfs-site': {
        'properties': {
          'dfs.datanode.du.reserved': '10240000000',
          'dfs.datanode.max.transfer.threads': '16384',
          'dfs.namenode.safemode.threshold-pct': '1.000',
          'dfs.datanode.failed.volumes.tolerated': '1',
          'dfs.namenode.handler.count': '25',
          'dfs.datanode.data.dir': '/path/1,/path/2,/path/3,/path/4',
          'dfs.namenode.name.dir': '/hadoop/hdfs/namenode',
          'dfs.namenode.checkpoint.dir': '/hadoop/hdfs/namesecondary'
        },
        'property_attributes': {
          'dfs.datanode.failed.volumes.tolerated': {'maximum': '4'},
          'dfs.encryption.key.provider.uri': {'delete': 'true'}
        }
      },
      'ranger-hdfs-plugin-properties': {
        'properties': {
          'ranger-hdfs-plugin-enabled': 'Yes'
        }
      },
      "core-site": {
        "properties": {
          "hadoop.proxyuser.hdfs.hosts": "*",
          "hadoop.proxyuser.hdfs.groups": "*",
          "hadoop.proxyuser.ambari_user.hosts": ambariHostName,
          "hadoop.proxyuser.ambari_user.groups": "*"
          },
        'property_attributes': {
          'hadoop.security.key.provider.path': {'delete': 'true'}
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
                "configurations": configurations,
                "ambari-server-properties": {"ambari-server.user":"ambari_user"}
                }
    # One host has bigger volume size. Minimum should be used for the calculations of dfs.datanode.du.reserved
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
            "total_mem" : 2097152,
            "disk_info": [{
              "size": '80000000',
              "mountpoint": "/"
            }]
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
            "total_mem" : 10485760,
            "disk_info": [{
              "size": '80000000000',
              "mountpoint": "/"
            }]
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
            "total_mem" : 2097152,
            "disk_info": [{
              "size": '80000000',
              "mountpoint": "/"
            }]
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
            "total_mem" : 2097152,
            "disk_info": [{
              "size": '80000000',
              "mountpoint": "/"
            }]
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
            "total_mem" : 2097152,
            "disk_info": [{
              "size": '80000000',
              "mountpoint": "/"
            }]
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
    self.assertEqual("kms://http@host1;host2:9292/kms", configurations["hdfs-site"]["properties"]["dfs.encryption.key.provider.uri"])

    # Test 6 - Multiple RANGER_KMS_SERVERs and custom port
    configurations["kms-env"] = {"properties": {"kms_port": "1111"}}
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEqual("kms://http@host1;host2:1111/kms", configurations["hdfs-site"]["properties"]["dfs.encryption.key.provider.uri"])

    # Test 7 - Override by API caller
    configurations["hadoop-env"] = {"properties": {"keyserver_host": "myhost1", "keyserver_port": "2222"}}
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEqual("kms://http@host1;host2:1111/kms", configurations["hdfs-site"]["properties"]["dfs.encryption.key.provider.uri"])

    # Test - 'https' in KMS URL
    configurations["ranger-kms-site"] = {"properties": {"ranger.service.https.attrib.ssl.enabled": "true"}}
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEqual("kms://https@host1;host2:1111/kms", configurations["hdfs-site"]["properties"]["dfs.encryption.key.provider.uri"])

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
                           'tez.am.resource.memory.mb' : '1024',
                           'tez.tez-ui.history-url.base' : 'https://host:8443/#/main/views/TEZ/0.7.0.2.3.0.0-2155/TEZ_CLUSTER_INSTANCE'}

    properties = {'tez.task.resource.memory.mb': '2050',
                  'tez.runtime.io.sort.mb' : '256',
                  'tez.runtime.unordered.output.buffer.size-mb' : '256',
                  'tez.am.resource.memory.mb' : '2050',
                  'tez.tez-ui.history-url.base' : 'http://host:8080/#/main/views/TEZ/0.7.0.2.3.0.0-2155/TEZ_CLUSTER_INSTANCE'}


    res_expected = [{'config-name': 'tez.queue.name',
                     'config-type': 'tez-site',
                     'level': 'ERROR',
                     'message': 'Value should be set',
                     'type': 'configuration'},
                    {'config-name': 'tez.tez-ui.history-url.base',
                     'config-type': 'tez-site',
                     'level': 'WARN',
                     'message': "It is recommended to set value https://host:8443/#/main/views/TEZ/0.7.0.2.3.0.0-2155/TEZ_CLUSTER_INSTANCE for property tez.tez-ui.history-url.base",
                     'type': 'configuration'},
                    {'config-name': 'tez.am.resource.memory.mb',
                     'config-type': 'tez-site',
                     'level': 'WARN',
                     'message': "tez.am.resource.memory.mb should be less than YARN max allocation size (2048)",
                     'type': 'configuration'},
                    {'config-name': 'tez.task.resource.memory.mb',
                     'config-type': 'tez-site',
                     'level': 'WARN',
                     'message': "tez.task.resource.memory.mb should be less than YARN max allocation size (2048)",
                     'type': 'configuration'}]

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
    services = {}
    services['configurations'] = configurations

    # 1) ok: No yarn_cgroups_enabled
    recommendedDefaults = {'namenode_heapsize': '1024',
                           'namenode_opt_newsize' : '256',
                           'namenode_opt_maxnewsize' : '256'}
    properties = {}
    properties['service_check.queue.name'] = 'default'
    res_expected = []

    res = self.stackAdvisor.validateYARNEnvConfigurations(properties, recommendedDefaults, configurations, services, '')
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
    res = self.stackAdvisor.validateYARNEnvConfigurations(properties, recommendedDefaults, configurations, services, '')
    self.assertEquals(res, res_expected)

    # 3) ok: yarn_cgroups_enabled=true, but security enabled
    properties['yarn_cgroups_enabled'] = 'true'
    res_expected = []
    res = self.stackAdvisor.validateYARNEnvConfigurations(properties, recommendedDefaults, configurations, services, '')
    self.assertEquals(res, res_expected)

    # 4) fail: yarn_cgroups_enabled=true, but security disabled
    configurations['core-site']['properties']['hadoop.security.authorization'] = 'false'
    res_expected = [{'config-type': 'yarn-env',
                     'message': 'CPU Isolation should only be enabled if security is enabled',
                     'type': 'configuration',
                     'config-name': 'yarn_cgroups_enabled',
                     'level': 'WARN'}]
    res = self.stackAdvisor.validateYARNEnvConfigurations(properties, recommendedDefaults, configurations, services, '')
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
                    {'config-name': 'mapreduce.job.queuename',
                     'config-type': 'mapred-site',
                     'level': 'ERROR',
                     'message': 'Value should be set',
                     'type': 'configuration'},
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

    # 2) fail: hive_security_authorization=Ranger but ranger plugin is disabled in ranger-env
    properties = {"hive_security_authorization": "Ranger"}
    configurations = {
      "ranger-env":{
        "properties":{
          "ranger-hive-plugin-enabled":"No",
        }
      },
      "hive-env":{
        "properties":{
          "hive_security_authorization": "Ranger",
        }
      }
    }
    services = {
      "configurations": configurations
    }
    res_expected = []

    services['configurations']['ranger-env']['properties']['ranger-hive-plugin-enabled'] = 'No'
    res_expected = [{'config-type': 'hive-env',
                     'message': 'ranger-env/ranger-hive-plugin-enabled must be enabled when hive_security_authorization is set to Ranger',
                     'type': 'configuration',
                     'config-name': 'hive_security_authorization',
                     'level': 'WARN'}]

    res = self.stackAdvisor.validateHiveConfigurationsEnv(properties, {}, configurations, services, {})
    self.assertEquals(res, res_expected)


  def test_validateHiveConfigurations(self):
    properties = {"hive_security_authorization": "None",
                  "hive.server2.authentication": "LDAP",
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
    res_expected = [{'config-type': 'hive-site', 'message': 'According to LDAP value for hive.server2.authentication, '
                   'you should add hive.server2.authentication.ldap.Domain property, if you are using AD, if not, '
                   'then hive.server2.authentication.ldap.baseDN!', 'type': 'configuration', 'config-name':
                  'hive.server2.authentication', 'level': 'WARN'}]
    res = self.stackAdvisor.validateHiveConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    pass

  def test_validateHiveServer2Configurations(self):
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
        "properties": {"hive_security_authorization": "ranger"}
      }
    }
    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "RANGER",
          },
        }
      ],
    }

    # Test with ranger plugin enabled, validation fails
    res_expected = [{'config-type': 'hiveserver2-site', 'message': 'If Ranger Hive Plugin is enabled. hive.security.authorization.manager under hiveserver2-site needs to be set to com.xasecure.authorization.hive.authorizer.XaSecureHiveAuthorizerFactory', 'type': 'configuration', 'config-name': 'hive.security.authorization.manager', 'level': 'WARN'}, {'config-type': 'hiveserver2-site', 'message': 'If Ranger Hive Plugin is enabled. hive.security.authenticator.manager under hiveserver2-site needs to be set to org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator', 'type': 'configuration', 'config-name': 'hive.security.authenticator.manager', 'level': 'WARN'}, {'config-type': 'hiveserver2-site', 'message': 'If Ranger Hive Plugin is enabled. hive.conf.restricted.list under hiveserver2-site needs to contain missing value hive.security.authorization.enabled,hive.security.authorization.manager,hive.security.authenticator.manager', 'type': 'configuration', 'config-name': 'hive.conf.restricted.list', 'level': 'WARN'}]
    res = self.stackAdvisor.validateHiveServer2Configurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

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
        },
        "core-site": {
          "properties": {
            "hadoop.security.authentication": ""
          }
        }
      }
    }
    expected = {
      "yarn-env": {
        "properties": {
          "min_user_id": "500",
          "service_check.queue.name": "default"
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.linux-container-executor.group": "hadoop",
          "yarn.nodemanager.container-executor.class": "org.apache.hadoop.yarn.server.nodemanager.LinuxContainerExecutor",
          "yarn.nodemanager.linux-container-executor.cgroups.mount-path": "/cgroup",
          "yarn.nodemanager.linux-container-executor.cgroups.mount": "true",
          "yarn.nodemanager.resource.memory-mb": "39424",
          "yarn.scheduler.minimum-allocation-mb": "3584",
          "yarn.scheduler.maximum-allocation-vcores": "4",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.nodemanager.resource.cpu-vcores": "4",
          "yarn.nodemanager.linux-container-executor.cgroups.hierarchy": "/yarn",
          "yarn.scheduler.maximum-allocation-mb": "39424",
          "yarn.nodemanager.linux-container-executor.resources-handler.class": "org.apache.hadoop.yarn.server.nodemanager.util.CgroupsLCEResourcesHandler",
          "hadoop.registry.rm.enabled": "false",
          "yarn.timeline-service.leveldb-state-store.path": "/hadoop/yarn/timeline",
          "yarn.timeline-service.leveldb-timeline-store.path": "/hadoop/yarn/timeline",
          "yarn.nodemanager.local-dirs": "/hadoop/yarn/local,/dev/shm/hadoop/yarn/local,/vagrant/hadoop/yarn/local",
          "yarn.nodemanager.log-dirs": "/hadoop/yarn/log,/dev/shm/hadoop/yarn/log,/vagrant/hadoop/yarn/log"
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
          "min_user_id": "500",
          'service_check.queue.name': 'default'
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.nodemanager.linux-container-executor.group": "hadoop",
          "yarn.nodemanager.container-executor.class": "org.apache.hadoop.yarn.server.nodemanager.DefaultContainerExecutor",
          "yarn.nodemanager.linux-container-executor.cgroups.mount-path": "/cgroup",
          "yarn.nodemanager.linux-container-executor.cgroups.mount": "true",
          "yarn.nodemanager.resource.memory-mb": "39424",
          "yarn.scheduler.minimum-allocation-mb": "3584",
          "yarn.scheduler.maximum-allocation-vcores": "4",
          "yarn.scheduler.minimum-allocation-vcores": "1",
          "yarn.nodemanager.resource.cpu-vcores": "4",
          "yarn.nodemanager.linux-container-executor.cgroups.hierarchy": "/yarn",
          "yarn.scheduler.maximum-allocation-mb": "39424",
          "yarn.nodemanager.linux-container-executor.resources-handler.class": "org.apache.hadoop.yarn.server.nodemanager.util.CgroupsLCEResourcesHandler",
          "hadoop.registry.rm.enabled": "false",
          "yarn.timeline-service.leveldb-state-store.path": "/hadoop/yarn/timeline",
          "yarn.timeline-service.leveldb-timeline-store.path": "/hadoop/yarn/timeline",
          "yarn.nodemanager.local-dirs": "/hadoop/yarn/local,/dev/shm/hadoop/yarn/local,/vagrant/hadoop/yarn/local",
          "yarn.nodemanager.log-dirs": "/hadoop/yarn/log,/dev/shm/hadoop/yarn/log,/vagrant/hadoop/yarn/log"
        },
        "property_attributes": {
          "yarn.nodemanager.linux-container-executor.cgroups.mount": {
            "delete": "true"
          },
          "yarn.nodemanager.linux-container-executor.cgroups.hierarchy": {
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
          "yarn.nodemanager.linux-container-executor.resources-handler.class": {
            "delete": "true"
          }
        }
      }
    }
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

  def test_validateHDFSRangerPluginConfigurations(self):
    configurations = {}
      # 1) ok: ranger plugin is enabled in ranger-env and ranger-hdfs-plugin-properties
    recommendedDefaults = {}
    properties = {}
    configurations = {
      "ranger-env":{
        "properties":{
          "ranger-hdfs-plugin-enabled":"Yes",
          }
      },
      "ranger-hdfs-plugin-properties":{
        "properties":{
          "ranger-hdfs-plugin-enabled":"Yes",
        }
      }
    }
    services = {
      "configurations": configurations
    }
    res_expected = []

    res = self.stackAdvisor.validateHDFSRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    # 2) fail: ranger plugin is disabled in ranger-env
    services['configurations']['ranger-env']['properties']['ranger-hdfs-plugin-enabled'] = 'No'
    res_expected = [{'config-type': 'ranger-hdfs-plugin-properties',
                     'message': 'ranger-hdfs-plugin-properties/ranger-hdfs-plugin-enabled must correspond ranger-env/ranger-hdfs-plugin-enabled',
                     'type': 'configuration',
                     'config-name': 'ranger-hdfs-plugin-enabled',
                     'level': 'WARN'}]

    res = self.stackAdvisor.validateHDFSRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

  def test_validateYARNRangerPluginConfigurations(self):
    configurations = {}
    # 1) ok: ranger plugin is enabled in ranger-env and ranger-yarn-plugin-properties
    recommendedDefaults = {}
    properties = {}
    configurations = {
      "ranger-env":{
        "properties":{
          "ranger-yarn-plugin-enabled":"Yes",
          }
      },
      "ranger-yarn-plugin-properties":{
        "properties":{
          "ranger-yarn-plugin-enabled":"Yes",
          }
      }
    }
    services = {
      "configurations": configurations
    }
    res_expected = []

    res = self.stackAdvisor.validateYARNRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    # 2) fail: ranger plugin is disabled in ranger-env
    services['configurations']['ranger-env']['properties']['ranger-yarn-plugin-enabled'] = 'No'
    res_expected = [{'config-type': 'ranger-yarn-plugin-properties',
                     'message': 'ranger-yarn-plugin-properties/ranger-yarn-plugin-enabled must correspond ranger-env/ranger-yarn-plugin-enabled',
                     'type': 'configuration',
                     'config-name': 'ranger-yarn-plugin-enabled',
                     'level': 'WARN'}]

    res = self.stackAdvisor.validateYARNRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

  def test_validateHBASERangerPluginConfigurations(self):
    configurations = {}
    # 1) ok: ranger plugin is enabled in ranger-env and ranger-hbase-plugin-properties
    recommendedDefaults = {}
    properties = {}
    configurations = {
      "ranger-env":{
        "properties":{
          "ranger-hbase-plugin-enabled":"Yes",
          }
      },
      "ranger-hbase-plugin-properties":{
        "properties":{
          "ranger-hbase-plugin-enabled":"Yes",
          }
      }
    }
    services = {
      "configurations": configurations
    }
    res_expected = []

    res = self.stackAdvisor.validateHBASERangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    # 2) fail: ranger plugin is disabled in ranger-env
    services['configurations']['ranger-env']['properties']['ranger-hbase-plugin-enabled'] = 'No'
    res_expected = [{'config-type': 'ranger-hbase-plugin-properties',
                     'message': 'ranger-hbase-plugin-properties/ranger-hbase-plugin-enabled must correspond ranger-env/ranger-hbase-plugin-enabled',
                     'type': 'configuration',
                     'config-name': 'ranger-hbase-plugin-enabled',
                     'level': 'WARN'}]

    res = self.stackAdvisor.validateHBASERangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

  def test_validateKnoxRangerPluginConfigurations(self):
    configurations = {}
    # 1) ok: ranger plugin is enabled in ranger-env and ranger-knox-plugin-properties
    recommendedDefaults = {}
    properties = {}
    configurations = {
      "ranger-env":{
        "properties":{
          "ranger-knox-plugin-enabled":"Yes",
          }
      },
      "ranger-knox-plugin-properties":{
        "properties":{
          "ranger-knox-plugin-enabled":"Yes",
          }
      }
    }
    services = {
      "configurations": configurations
    }
    res_expected = []

    res = self.stackAdvisor.validateKnoxRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    # 2) fail: ranger plugin is disabled in ranger-env
    services['configurations']['ranger-env']['properties']['ranger-knox-plugin-enabled'] = 'No'
    res_expected = [{'config-type': 'ranger-knox-plugin-properties',
                     'message': 'ranger-knox-plugin-properties/ranger-knox-plugin-enabled must correspond ranger-env/ranger-knox-plugin-enabled',
                     'type': 'configuration',
                     'config-name': 'ranger-knox-plugin-enabled',
                     'level': 'WARN'}]

    res = self.stackAdvisor.validateKnoxRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

  def test_validateKafkaRangerPluginConfigurations(self):
    configurations = {}
    # 1) ok: ranger plugin is enabled in ranger-env and ranger-kafka-plugin-properties
    recommendedDefaults = {}
    properties = {}
    configurations = {
      "ranger-env":{
        "properties":{
          "ranger-kafka-plugin-enabled":"Yes",
          }
      },
      "ranger-kafka-plugin-properties":{
        "properties":{
          "ranger-kafka-plugin-enabled":"Yes",
          }
      },
      "cluster-env": {
        "properties": {
          "security_enabled" : "true"
        }
      }
    }
    services = {
      "services":
      [
        {
          "StackServices": {
           "service_name" : "RANGER"
          }
        }
      ],      
      "configurations": configurations
    }
    res_expected = []
    res = self.stackAdvisor.validateKafkaRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    # 2) fail: ranger plugin is disabled in ranger-env
    services['configurations']['ranger-env']['properties']['ranger-kafka-plugin-enabled'] = 'No'
    res_expected = [{'config-type': 'ranger-kafka-plugin-properties',
                     'message': 'ranger-kafka-plugin-properties/ranger-kafka-plugin-enabled must correspond ranger-env/ranger-kafka-plugin-enabled',
                     'type': 'configuration',
                     'config-name': 'ranger-kafka-plugin-enabled',
                     'level': 'WARN'}]

    # Test to check security_enabled is false
    services['configurations']['cluster-env']['properties']['security_enabled'] = "false"
    res_expected.append({'config-type': 'ranger-kafka-plugin-properties', 'message': 'Ranger Kafka plugin should not be enabled in non-kerberos environment.', 'type': 'configuration', 'config-name': 'ranger-kafka-plugin-enabled', 'level': 'WARN'})
    res = self.stackAdvisor.validateKafkaRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    res = self.stackAdvisor.validateKafkaRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

  def test_validateStormRangerPluginConfigurations(self):
    configurations = {}
    # 1) ok: ranger plugin is enabled in ranger-env and ranger-storm-plugin-properties
    recommendedDefaults = {}
    properties = {}
    configurations = {
      "ranger-env":{
        "properties":{
          "ranger-storm-plugin-enabled":"Yes",
          }
      },
      "ranger-storm-plugin-properties":{
        "properties":{
          "ranger-storm-plugin-enabled":"Yes",
          }
      },
      "cluster-env": {
        "properties": {
          "security_enabled" : "true"
        }
      }
    }
    services = {
      "services":
        [
          {
            "StackServices": {
              "service_name" : "RANGER"
            }
          }
        ],
      "configurations": configurations
    }
    res_expected = []
    res = self.stackAdvisor.validateStormRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    # 2) fail: ranger plugin is disabled in ranger-env
    services['configurations']['ranger-env']['properties']['ranger-storm-plugin-enabled'] = 'No'
    res_expected = [{'config-type': 'ranger-storm-plugin-properties',
                     'message': 'ranger-storm-plugin-properties/ranger-storm-plugin-enabled must correspond ranger-env/ranger-storm-plugin-enabled',
                     'type': 'configuration',
                     'config-name': 'ranger-storm-plugin-enabled',
                     'level': 'WARN'}]

    res = self.stackAdvisor.validateStormRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    # Test to check security_enabled is false
    services['configurations']['cluster-env']['properties']['security_enabled'] = "false"
    res_expected.append({'config-type': 'ranger-storm-plugin-properties', 'message': 'Ranger Storm plugin should not be enabled in non-kerberos environment.', 'type': 'configuration', 'config-name': 'ranger-storm-plugin-enabled', 'level': 'WARN'})
    res = self.stackAdvisor.validateStormRangerPluginConfigurations(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

  def test_recommendRangerConfigurations(self):
    clusterData = {}
    # Recommend ranger-storm-plugin-enabled=No on non-kerberos cluster
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
        "cluster-env": {
          "properties": {
            "security_enabled": "false",
          }
        },
      },
    }

    expected = {
      'admin-properties': {'properties': {'policymgr_external_url': 'http://host1:6080'}}, 'ranger-env': {'properties': {'ranger-storm-plugin-enabled': 'No'}}
    }

    recommendedConfigurations = {}
    self.stackAdvisor.recommendRangerConfigurations(recommendedConfigurations, clusterData, services, None)
    self.assertEquals(recommendedConfigurations, expected)

  def test_validateRangerConfigurationsEnv(self):
    properties = {
      "ranger-storm-plugin-enabled": "Yes",
    }
    recommendedDefaults = {
      "ranger-storm-plugin-enabled": "No",
    }
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "false",
          }
      }
    }
    services = {
      "services":
        [
          {
            "StackServices": {
              "service_name" : "STORM"
            }
          }
        ]
    }

    # Test with ranger plugin enabled, validation fails
    res_expected = [{'config-type': 'ranger-env', 'message': 'Ranger Storm plugin should not be enabled in non-kerberos environment.', 'type': 'configuration', 'config-name': 'ranger-storm-plugin-enabled', 'level': 'WARN'}]

    res = self.stackAdvisor.validateRangerConfigurationsEnv(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

  def test_validateSparkDefaults(self):
    properties = {}
    recommendedDefaults = {
      "spark.yarn.queue": "default",
    }
    configurations = {}
    services = {
      "services":
        [
          {
            "StackServices": {
              "service_name": "SPARK"
            }
          }
        ]
    }

    # Test with ranger plugin enabled, validation fails
    res_expected = [{'config-type': 'spark-defaults', 'message': 'Value should be set', 'type': 'configuration', 'config-name': 'spark.yarn.queue', 'level': 'ERROR'}]

    res = self.stackAdvisor.validateSparkDefaults(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)
