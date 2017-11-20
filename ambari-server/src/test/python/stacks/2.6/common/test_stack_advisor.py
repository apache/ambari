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
from mock.mock import patch
import socket

class TestHDP26StackAdvisor(TestCase):
  def setUp(self):
    import imp
    self.maxDiff = None
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    hdp206StackAdvisorPath = os.path.join(self.testDirectory,
                                          '../../../../../main/resources/stacks/HDP/2.0.6/services/stack_advisor.py')
    hdp21StackAdvisorPath = os.path.join(self.testDirectory,
                                         '../../../../../main/resources/stacks/HDP/2.1/services/stack_advisor.py')
    hdp22StackAdvisorPath = os.path.join(self.testDirectory,
                                         '../../../../../main/resources/stacks/HDP/2.2/services/stack_advisor.py')
    hdp23StackAdvisorPath = os.path.join(self.testDirectory,
                                         '../../../../../main/resources/stacks/HDP/2.3/services/stack_advisor.py')
    hdp24StackAdvisorPath = os.path.join(self.testDirectory,
                                         '../../../../../main/resources/stacks/HDP/2.4/services/stack_advisor.py')
    hdp25StackAdvisorPath = os.path.join(self.testDirectory,
                                         '../../../../../main/resources/stacks/HDP/2.5/services/stack_advisor.py')
    hdp26StackAdvisorPath = os.path.join(self.testDirectory,
                                         '../../../../../main/resources/stacks/HDP/2.6/services/stack_advisor.py')
    hdp26StackAdvisorClassName = 'HDP26StackAdvisor'

    with open(stackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp206StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp206StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp21StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp21StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp22StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp22StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp23StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp23StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp24StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp24StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp25StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp25StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp26StackAdvisorPath, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp26StackAdvisorPath,
                                           ('.py', 'rb', imp.PY_SOURCE))
    clazz = getattr(stack_advisor_impl, hdp26StackAdvisorClassName)
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

  def test_recommendDruidConfigurations_withMysql(self):
    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 4,
            "total_mem": 50331648,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          },
        }
      ]
    }

    services = {
      "Versions": {
        "parent_stack_version": "2.5",
        "stack_name": "HDP",
        "stack_version": "2.6",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.5", "2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [{
        "StackServices": {
          "service_name": "DRUID",
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "DRUID_COORDINATOR",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_OVERLORD",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_BROKER",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_HISTORICAL",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_MIDDLEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          }
        ]
      }
      ],
      "configurations": {
        "druid-common": {
          "properties": {
            "database_name": "druid",
            "metastore_hostname": "c6401.ambari.apache.org",
            "druid.metadata.storage.type": "mysql",
            "druid.extensions.loadList": "[\"postgresql-metadata-storage\"]"
          }
        }
      }
    }

    clusterData = {
      "cpu": 4,
      "mapMemory": 30000,
      "amMemory": 20000,
      "reduceMemory": 20560,
      "containers": 30,
      "ramPerContainer": 512,
      "referenceNodeManagerHost": {
        "total_mem": 10240 * 1024
      }
    }

    configurations = {
    }

    self.stackAdvisor.recommendDruidConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations,
                      {'druid-historical': {
                        'properties': {'druid.processing.numThreads': '3',
                                       'druid.server.http.numThreads': '40',
                                       'druid.processing.numMergeBuffers': '2',
                                       'druid.processing.buffer.sizeBytes': '1073741824'}},
                        'druid-broker': {
                          'properties': {'druid.processing.numThreads': '3',
                                         'druid.server.http.numThreads': '40',
                                         'druid.processing.numMergeBuffers': '2',
                                         'druid.processing.buffer.sizeBytes': '1073741824'}},
                        'druid-common': {'properties': {'druid.extensions.loadList': '["mysql-metadata-storage"]',
                                                        'druid.metadata.storage.connector.port': '3306',
                                                        'druid.metadata.storage.connector.connectURI': 'jdbc:mysql://c6401.ambari.apache.org:3306/druid?createDatabaseIfNotExist=true',
                                                        'druid.zk.service.host': ''}},
                        'druid-env': {'properties': {},
                                      'property_attributes': {'druid.coordinator.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.overlord.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.middlemanager.jvm.heap.memory': {
                                                                'maximum': '49152'},
                                                              'druid.historical.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.broker.jvm.heap.memory': {'maximum': '49152'}}}}
                      )

  def test_recommendSPARK2Configurations_SecurityEnabledZeppelinInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.property1": "value1"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK2"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.superusers": "zeppelin_user",
          "livy.property1": "value1"
        }
      },
      "spark2-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark2-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }

    self.stackAdvisor.recommendSPARK2Configurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARK2Configurations_SecurityNotEnabledZeppelinInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "false",
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.property1": "value1"
        }
      },
      "zeppelin-env": {
        "properties": {
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK2"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "false",
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.property1": "value1"
        }
      },
      "spark2-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark2-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "zeppelin-env": {
        "properties": {
        }
      }
    }

    self.stackAdvisor.recommendSPARK2Configurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARK2Configurations_SecurityEnabledZeppelinInstalledExistingValue(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.property1": "value1",
          "livy.superusers": "livy_user"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK2"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.property1": "value1",
          "livy.superusers": "livy_user,zeppelin_user"
        }
      },
      "spark2-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark2-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }

    self.stackAdvisor.recommendSPARK2Configurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendSPARK2Configurations_SecurityEnabledZeppelinNotInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.property1": "value1"
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "SPARK2"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true"
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.property1": "value1"
        }
      },
      "spark2-defaults": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      },
      "spark2-thrift-sparkconf": {
        "properties": {
          "spark.yarn.queue": "default"
        }
      }
    }

    self.stackAdvisor.recommendSPARK2Configurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendZEPPELINConfigurations_SecurityEnabledSPARKInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.property1": "value1"
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.property1": "value1"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "ZEPPELIN"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.superusers": "zeppelin_user",
          "livy.property1": "value1"
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.superusers": "zeppelin_user",
          "livy.property1": "value1"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }

    self.stackAdvisor.recommendZEPPELINConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendZEPPELINConfigurations_SecurityNotEnabledSparkInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "false",
        }
      },
      "livy-conf": {
        "properties": {
        }
      },
      "livy2-conf": {
        "properties": {
        }
      },
      "zeppelin-env": {
        "properties": {
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "ZEPPELIN"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "false",
        }
      },
      "livy-conf": {
        "properties": {
        }
      },
      "livy2-conf": {
        "properties": {
        }
      },
      "zeppelin-env": {
        "properties": {
        }
      }
    }

    self.stackAdvisor.recommendZEPPELINConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendZEPPELINConfigurations_SecurityEnabledZeppelinInstalledExistingValue(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.superusers": "livy_user, hdfs"
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.superusers": "livy2_user"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "ZEPPELIN"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        }
      },
      "livy-conf": {
        "properties": {
          "livy.superusers": "livy_user,hdfs,zeppelin_user"
        }
      },
      "livy2-conf": {
        "properties": {
          "livy.superusers": "livy2_user,zeppelin_user"
        }
      },
      "zeppelin-env": {
        "properties": {
          "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
        }
      }
    }

    self.stackAdvisor.recommendZEPPELINConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendZEPPELINConfigurations_SecurityEnabledSparkNotInstalled(self):
    configurations = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        },
        "zeppelin-env": {
          "properties": {
            "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
          }
        }
      }
    }
    services = {"configurations": configurations}
    services['services'] = [
      {
        "StackServices": {
          "service_name": "ZEPPELIN"
        },
      }
    ]
    clusterData = {
      "cpu": 4,
      "containers": 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      "cluster-env": {
        "properties": {
          "security_enabled": "true",
        },
        "zeppelin-env": {
          "properties": {
            "zeppelin.server.kerberos.principal": "zeppelin_user@REALM"
          }
        }
      }
    }

    self.stackAdvisor.recommendZEPPELINConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendDruidConfigurations_WithPostgresql(self):
    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 4,
            "total_mem": 50331648,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          }
        }
      ]
    }

    services = {
      "Versions": {
        "parent_stack_version": "2.5",
        "stack_name": "HDP",
        "stack_version": "2.6",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.5", "2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [{
        "StackServices": {
          "service_name": "DRUID",
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "DRUID_COORDINATOR",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_OVERLORD",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_BROKER",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_HISTORICAL",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_MIDDLEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          }
        ]
      }
      ],
      "configurations": {
        "druid-common": {
          "properties": {
            "database_name": "druid",
            "metastore_hostname": "c6401.ambari.apache.org",
            "druid.metadata.storage.type": "postgresql",
            "druid.extensions.loadList": "[\"mysql-metadata-storage\"]",
            "druid.extensions.pullList": "[]"
          }
        }
      }
    }

    clusterData = {
    }

    configurations = {
    }

    self.stackAdvisor.recommendDruidConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations,
                      {'druid-historical': {
                        'properties': {'druid.processing.numThreads': '3',
                                       'druid.server.http.numThreads': '40',
                                       'druid.processing.numMergeBuffers': '2',
                                       'druid.processing.buffer.sizeBytes': '1073741824'}},
                        'druid-broker': {
                          'properties': {'druid.processing.numThreads': '3',
                                         'druid.server.http.numThreads': '40',
                                         'druid.processing.numMergeBuffers': '2',
                                         'druid.processing.buffer.sizeBytes': '1073741824'}},
                        'druid-common': {'properties': {'druid.extensions.loadList': '["postgresql-metadata-storage"]',
                                                        'druid.metadata.storage.connector.port': '5432',
                                                        'druid.metadata.storage.connector.connectURI': 'jdbc:postgresql://c6401.ambari.apache.org:5432/druid',
                                                        'druid.zk.service.host': ''}},
                        'druid-env': {'properties': {},
                                      'property_attributes': {'druid.coordinator.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.overlord.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.middlemanager.jvm.heap.memory': {
                                                                'maximum': '49152'},
                                                              'druid.historical.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.broker.jvm.heap.memory': {'maximum': '49152'}}}}
                      )

  def test_recommendDruidConfigurations_WithDerby(self):
    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 4,
            "total_mem": 50331648,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          }
        }
      ]
    }

    services = {
      "Versions": {
        "parent_stack_version": "2.5",
        "stack_name": "HDP",
        "stack_version": "2.6",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.5", "2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [{
        "StackServices": {
          "service_name": "DRUID",
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "DRUID_COORDINATOR",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_OVERLORD",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_BROKER",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_HISTORICAL",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_MIDDLEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          }
        ]
      }
      ],
      "configurations": {
        "druid-common": {
          "properties": {
            "database_name": "druid",
            "metastore_hostname": "c6401.ambari.apache.org",
            "druid.metadata.storage.type": "derby",
            "druid.extensions.loadList": "[\"mysql-metadata-storage\"]",
            "druid.extensions.pullList": "[]"
          }
        }
      }
    }

    clusterData = {
    }

    configurations = {
    }

    self.stackAdvisor.recommendDruidConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations,
                      {'druid-historical': {
                        'properties': {'druid.processing.numThreads': '3',
                                       'druid.server.http.numThreads': '40',
                                       'druid.processing.numMergeBuffers': '2',
                                       'druid.processing.buffer.sizeBytes': '1073741824'}},
                        'druid-broker': {
                          'properties': {'druid.processing.numThreads': '3',
                                         'druid.server.http.numThreads': '40',
                                         'druid.processing.numMergeBuffers': '2',
                                         'druid.processing.buffer.sizeBytes': '1073741824'}},
                        'druid-common': {'properties': {'druid.extensions.loadList': '[]',
                                                        'druid.metadata.storage.connector.port': '1527',
                                                        'druid.metadata.storage.connector.connectURI': 'jdbc:derby://c6401.ambari.apache.org:1527/druid;create=true',
                                                        'druid.zk.service.host': ''}},
                        'druid-env': {'properties': {},
                                      'property_attributes': {'druid.coordinator.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.overlord.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.middlemanager.jvm.heap.memory': {
                                                                'maximum': '49152'},
                                                              'druid.historical.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.broker.jvm.heap.memory': {'maximum': '49152'}}}}
                      )



  def test_recommendDruidConfigurations_property_existence_check(self):
      # Test for https://issues.apache.org/jira/browse/AMBARI-19144
      hosts = {
        "items": [
          {
            "Hosts": {
              "cpu_count": 4,
              "total_mem": 50331648,
              "disk_info": [
                {"mountpoint": "/"},
                {"mountpoint": "/dev/shm"},
                {"mountpoint": "/vagrant"},
                {"mountpoint": "/"},
                {"mountpoint": "/dev/shm"},
                {"mountpoint": "/vagrant"}
              ],
              "public_host_name": "c6401.ambari.apache.org",
              "host_name": "c6401.ambari.apache.org"
            }
          }
        ]
      }

      services = {
        "Versions": {
          "parent_stack_version": "2.5",
          "stack_name": "HDP",
          "stack_version": "2.6",
          "stack_hierarchy": {
            "stack_name": "HDP",
            "stack_versions": ["2.5", "2.4", "2.3", "2.2", "2.1", "2.0.6"]
          }
        },
        "services": [{
        }
        ],
        "configurations": {
        }
      }

      clusterData = {
      }

      configurations = {
      }

      self.stackAdvisor.recommendDruidConfigurations(configurations, clusterData, services, hosts)
      self.assertEquals(configurations,
                        {}
                        )

  def test_recommendDruidConfigurations_heterogeneous_hosts(self):
    hosts = {
      "items": [
        {
          "href": "/api/v1/hosts/c6401.ambari.apache.org",
          "Hosts": {
            "cpu_count": 4,
            "total_mem": 50331648,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          }
        }, {
          "href": "/api/v1/hosts/c6402.ambari.apache.org",
          "Hosts": {
            "cpu_count": 1,
            "total_mem": 622680,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6402.ambari.apache.org",
            "host_name": "c6402.ambari.apache.org"
          }
        },
        {
          "href": "/api/v1/hosts/c6403.ambari.apache.org",
          "Hosts": {
            "cpu_count": 3,
            "total_mem": 3845360,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6403.ambari.apache.org",
            "host_name": "c6403.ambari.apache.org"
          }
        }
      ]
    }

    services = {
      "Versions": {
        "parent_stack_version": "2.5",
        "stack_name": "HDP",
        "stack_version": "2.6",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.5", "2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [{
        "StackServices": {
          "service_name": "DRUID",
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "DRUID_COORDINATOR",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_OVERLORD",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_BROKER",
              "hostnames": ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_HISTORICAL",
              "hostnames": ["c6401.ambari.apache.org", "c6403.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_MIDDLEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          }
        ]
      }
      ],
      "configurations": {
        "druid-common": {
          "properties": {
            "database_name": "druid",
            "metastore_hostname": "c6401.ambari.apache.org",
            "druid.metadata.storage.type": "derby",
            "druid.extensions.loadList": "[\"mysql-metadata-storage\"]",
            "druid.extensions.pullList": "[]"
          }
        }
      }
    }

    clusterData = {
    }

    configurations = {
    }

    self.stackAdvisor.recommendDruidConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations,
                      {'druid-historical': {
                        'properties': {'druid.processing.numThreads': '2',
                                       'druid.server.http.numThreads': '40',
                                       'druid.processing.numMergeBuffers': '2',
                                       'druid.processing.buffer.sizeBytes': '134217728'}},
                        'druid-broker': {
                          'properties': {'druid.processing.numThreads': '1',
                                         'druid.server.http.numThreads': '40',
                                         'druid.processing.numMergeBuffers': '2',
                                         'druid.processing.buffer.sizeBytes': '67108864'}},
                        'druid-common': {'properties': {'druid.extensions.loadList': '[]',
                                                        'druid.metadata.storage.connector.port': '1527',
                                                        'druid.metadata.storage.connector.connectURI': 'jdbc:derby://c6401.ambari.apache.org:1527/druid;create=true',
                                                        'druid.zk.service.host': ''
                                                        }},
                        'druid-env': {'properties': {},
                                      'property_attributes': {'druid.coordinator.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.overlord.jvm.heap.memory': {'maximum': '49152'},
                                                              'druid.middlemanager.jvm.heap.memory': {
                                                                'maximum': '49152'},
                                                              'druid.historical.jvm.heap.memory': {'maximum': '3755'},
                                                              'druid.broker.jvm.heap.memory': {'maximum': '1024'}}}}
                      )

  def test_recommendDruidConfigurations_low_mem_hosts(self):
    hosts = {
      "items": [
        {
          "href": "/api/v1/hosts/c6401.ambari.apache.org",
          "Hosts": {
            "cpu_count": 8,
            "total_mem": 102400,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          }
        }, {
          "href": "/api/v1/hosts/c6402.ambari.apache.org",
          "Hosts": {
            "cpu_count": 4,
            "total_mem": 204800,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6402.ambari.apache.org",
            "host_name": "c6402.ambari.apache.org"
          }
        },
        {
          "href": "/api/v1/hosts/c6403.ambari.apache.org",
          "Hosts": {
            "cpu_count": 6,
            "total_mem": 409600,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6403.ambari.apache.org",
            "host_name": "c6403.ambari.apache.org"
          }
        }
      ]
    }

    services = {
      "Versions": {
        "parent_stack_version": "2.5",
        "stack_name": "HDP",
        "stack_version": "2.6",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.5", "2.4", "2.3", "2.2", "2.1", "2.0.6"]
        }
      },
      "services": [{
        "StackServices": {
          "service_name": "DRUID",
        },
        "components": [
          {
            "StackServiceComponents": {
              "component_name": "DRUID_COORDINATOR",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_OVERLORD",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_BROKER",
              "hostnames": ["c6402.ambari.apache.org", "c6403.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_HISTORICAL",
              "hostnames": ["c6401.ambari.apache.org", "c6403.ambari.apache.org"]
            },
          },
          {
            "StackServiceComponents": {
              "component_name": "DRUID_MIDDLEMANAGER",
              "hostnames": ["c6401.ambari.apache.org"]
            },
          }
        ]
      }
      ],
      "configurations": {
        "druid-common": {
          "properties": {
            "database_name": "druid",
            "metastore_hostname": "c6401.ambari.apache.org",
            "druid.metadata.storage.type": "derby",
            "druid.extensions.loadList": "[\"mysql-metadata-storage\"]",
            "druid.extensions.pullList": "[]"
          }
        }
      }
    }

    clusterData = {
    }

    configurations = {
    }

    self.stackAdvisor.recommendDruidConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations,
                    {'druid-historical': {
                      'properties': {'druid.processing.numThreads': '5',
                                     'druid.server.http.numThreads': '40',
                                     'druid.processing.numMergeBuffers': '2',
                                     'druid.processing.buffer.sizeBytes': '14680064'}},
                      'druid-broker': {
                        'properties': {'druid.processing.numThreads': '3',
                                       'druid.server.http.numThreads': '40',
                                       'druid.processing.numMergeBuffers': '2',
                                       'druid.processing.buffer.sizeBytes': '41943040'}},
                      'druid-common': {'properties': {'druid.extensions.loadList': '[]',
                                                      'druid.metadata.storage.connector.port': '1527',
                                                      'druid.metadata.storage.connector.connectURI': 'jdbc:derby://c6401.ambari.apache.org:1527/druid;create=true',
                                                      'druid.zk.service.host': ''
                                                      }},
                      'druid-env': {'properties': {},
                                    'property_attributes': {'druid.coordinator.jvm.heap.memory': {'maximum': '1024'},
                                                            'druid.overlord.jvm.heap.memory': {'maximum': '1024'},
                                                            'druid.middlemanager.jvm.heap.memory': {
                                                              'maximum': '1024'},
                                                            'druid.historical.jvm.heap.memory': {'maximum': '1024'},
                                                            'druid.broker.jvm.heap.memory': {'maximum': '1024'}}}}
                    )


  def test_recommendAtlasConfigurations(self):
    configurations = {
      "application-properties": {
        "properties": {
          "atlas.sso.knox.providerurl": "",
          "atlas.graph.index.search.solr.zookeeper-url": "",
          "atlas.audit.hbase.zookeeper.quorum": "",
          "atlas.graph.storage.hostname": "",
          "atlas.kafka.bootstrap.servers": "",
          "atlas.kafka.zookeeper.connect": "",
          "atlas.authorizer.impl": "simple"
        }
      },
      "infra-solr-env": {
        "properties": {
          "infra_solr_znode": "/infra-solr"
        }
      },
      "ranger-atlas-plugin-properties": {
        "properties": {
          "ranger-atlas-plugin-enabled":"No"
        }
      },
      "atlas-env": {
        "properties": {
          "atlas_server_max_new_size": "600",
          "atlas_server_xmx": "2048"
        }
      }
    }

    clusterData = {}

    expected = {
      "application-properties": {
        "properties": {
          "atlas.sso.knox.providerurl": "https://c6401.ambari.apache.org:8443/gateway/knoxsso/api/v1/websso",
          "atlas.graph.index.search.solr.zookeeper-url": "",
          "atlas.audit.hbase.zookeeper.quorum": "",
          "atlas.graph.storage.hostname": "",
          "atlas.kafka.bootstrap.servers": "",
          "atlas.kafka.zookeeper.connect": "",
          "atlas.authorizer.impl": "simple",
          'atlas.proxyusers': 'knox'
        }
      },
      "infra-solr-env": {
        "properties": {
          "infra_solr_znode": "/infra-solr"
        }
      },
      "ranger-atlas-plugin-properties": {
        "properties": {
          "ranger-atlas-plugin-enabled":"No"
        }
      },
      "atlas-env": {
        "properties": {
          "atlas_server_max_new_size": "600",
          "atlas_server_xmx": "2048"
        }
      }
    }

    services = {
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.6/services/KNOX",
          "StackServices": {
            "service_name": "KNOX",
            "service_version": "0.9.0.2.5",
            "stack_name": "HDP",
            "stack_version": "2.6"
          },
          "components": [
            {
              "href": "/api/v1/stacks/HDP/versions/2.6/services/KNOX/components/KNOX_GATEWAY",
              "StackServiceComponents": {
                "advertise_version": "false",
                "cardinality": "1+",
                "component_category": "MASTER",
                "component_name": "KNOX_GATEWAY",
                "display_name": "Knox Gateway",
                "is_client": "false",
                "is_master": "true",
                "hostnames": ["c6401.ambari.apache.org"]
              },
              "dependencies": []
            }
          ]
        }
      ],
      "configurations": configurations
    }

    self.stackAdvisor.recommendAtlasConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendRangerConfigurations(self):
    clusterData = {}
    services = {
      "Versions" : {
        "stack_version" : "2.6",
        },
      "services":  [
        {
          "StackServices": {
            "service_name": "RANGER",
          "service_version": "0.7.0.2.6"
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
        "ranger-ugsync-site": {
          "properties": {
            "ranger.usersync.ldap.deltasync": "true",
            "ranger.usersync.group.searchenabled": "false"
            }
        }
      }
    }

    expected = {
      'ranger-admin-site': {
        'properties': {
          'ranger.audit.solr.zookeepers': 'NONE', 
          'ranger.audit.source.type': 'solr'
        }
      }, 
      'admin-properties': {
        'properties': {
          'policymgr_external_url': 'http://host1:6080'
        }
      }, 
      'ranger-tagsync-site': {
        'properties': {}
      }, 
      'tagsync-application-properties': {
        'properties': {} 
      }, 
      'ranger-env': {
        'properties': {
          'ranger-storm-plugin-enabled': 'No'
        }
      }, 
      'ranger-ugsync-site': {
        'properties': {
          'ranger.usersync.group.searchenabled': 'true'
        }
      }
    }

    recommendedConfigurations = {}

    self.stackAdvisor.recommendRangerConfigurations(recommendedConfigurations, clusterData, services, None)
    self.assertEquals(recommendedConfigurations, expected)

  def test_recommendRangerKMSConfigurations(self):
    clusterData = {}
    services = {
      "ambari-server-properties": {
        "ambari-server.user": "root"
        },
      "Versions": {
        "stack_version" : "2.6",
        },
      "services": [
        {
          "StackServices": {
            "service_name": "RANGER_KMS",
            "service_version": "0.7.0.2.6"
          },
          "components": [
            {
              "StackServiceComponents": {
                "component_name": "RANGER_KMS_SERVER",
                "hostnames": ["host1"]
              }
            }
          ]
        }
      ],
      "configurations": {
        'ranger-kms-site': {
          'properties': {
            "ranger.service.https.attrib.ssl.enabled": "true",
            "ranger.service.https.port": "9393"
          }
        }
      }
    }

    expected = {
      'kms-site': {
        'properties': {},
        'property_attributes': {
          'hadoop.kms.proxyuser.HTTP.users': {'delete': 'true'},
          'hadoop.kms.proxyuser.root.hosts': {'delete': 'true'},
          'hadoop.kms.proxyuser.root.users': {'delete': 'true'},
          'hadoop.kms.proxyuser.HTTP.hosts': {'delete': 'true'}
        }
      },
      'core-site': {
        'properties': {}
      },
      'kms-properties': {
        'properties': {}
      },
      'ranger-kms-audit': {
        'properties': {}
      },
      'kms-env': {
        'properties': {
          'kms_port': '9393'
        }
      },
      'dbks-site': {
        'properties': {}
      }
    }

    recommendedConfigurations = {}

    self.stackAdvisor.recommendRangerKMSConfigurations(recommendedConfigurations, clusterData, services, None)
    self.assertEquals(recommendedConfigurations, expected)

  def test_recommendHDFSConfigurations(self):
    ambariHostName = socket.getfqdn()
    configurations = {
      "ranger-hdfs-plugin-properties": {
        "properties": {
          "ranger-hdfs-plugin-enabled": "Yes",
          "REPOSITORY_CONFIG_USERNAME":"hadoop"
        }
      },
      "hadoop-env":{
        "properties":{
          "hdfs_user":"custom_hdfs"
        }
      }
    }
    clusterData = {
      "totalAvailableRam": 2048,
      "hBaseInstalled": True,
      "hbaseRam": 112,
      "reservedRam": 128
    }

    hosts = {
      "items": [
        {
          "Hosts": {
            "disk_info": [{
              "size": '8',
              "mountpoint": "/"
            }]
          }
        }]}

    services = {
      "services":
        [
          {
            "StackServices": {
              "service_name" : "HDFS",
              "service_version" : "2.7.0.2.6"
            },
            "components": [
            ]
          }
        ],
      "Versions": {
        "stack_version": "2.6"
      },
      "configurations": configurations,
      "ambari-server-properties": {"ambari-server.user":"ambari_user"}
    }


    expected = {
      'core-site': {
        'properties': {
          'hadoop.proxyuser.ambari_user.groups': '*',
          'hadoop.proxyuser.custom_hdfs.groups': '*',
          'hadoop.proxyuser.custom_hdfs.hosts': '*',
          'hadoop.proxyuser.ambari_user.hosts': ambariHostName
        },
        'property_attributes': {
          'hadoop.security.key.provider.path': {
            'delete': 'true'
          }
        }
      },
      'hadoop-env': {
        'properties': {
          'hdfs_user': 'custom_hdfs',
          'namenode_heapsize': '1024',
          'namenode_opt_maxnewsize': '128',
          'namenode_opt_newsize': '128'
        }
      },
      'hdfs-site': {
        'properties': {
          'dfs.datanode.data.dir': '/hadoop/hdfs/data',
          'dfs.datanode.failed.volumes.tolerated': '0',
          'dfs.datanode.max.transfer.threads': '16384',
          'dfs.namenode.name.dir': '/hadoop/hdfs/namenode',
          'dfs.namenode.handler.count': '100',
          'dfs.namenode.checkpoint.dir': '/hadoop/hdfs/namesecondary',
          'dfs.namenode.safemode.threshold-pct': '1.000',
          'dfs.namenode.inode.attributes.provider.class': 'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer',
          'dfs.datanode.du.reserved': '1073741824'
        },
        'property_attributes': {
          'dfs.datanode.failed.volumes.tolerated': {
            'maximum': '1'
          },
          'dfs.encryption.key.provider.uri': {
            'delete': 'true'
          }
        }
      },
      'ranger-hdfs-plugin-properties': {
        'properties': {
          'ranger-hdfs-plugin-enabled': 'Yes',
          'REPOSITORY_CONFIG_USERNAME': 'custom_hdfs'
        }
      }
    }

    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations,expected)
    configurations['hadoop-env']['properties']['hdfs_user'] = 'hadoop'
    expected['hadoop-env']['properties']['hdfs_user'] = 'hadoop'
    expected['ranger-hdfs-plugin-properties']['properties']['REPOSITORY_CONFIG_USERNAME'] = 'hadoop'
    expected['core-site']['properties']['hadoop.proxyuser.hadoop.hosts'] = '*'
    expected['core-site']['properties']['hadoop.proxyuser.hadoop.groups'] = '*'
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations,expected)


  def test_recommendHiveConfigurations(self):
    configurations = {
      "hive-env" : {
        "properties" : {
          "hive.atlas.hook" : "false",
          "hive_user": "custom_hive",
          "hive_security_authorization": "Ranger"
        }
      },
      "ranger-env" : {
        "properties" : {
          "ranger-hive-plugin-enabled" : "Yes"
        }
      },
      "cluster-env" : {
        "properties" : {
          "security_enabled" : "false"
        }
      },
      "ranger-hive-plugin-properties" : {
        "properties" : {
          "REPOSITORY_CONFIG_USERNAME": "hive"
        }
      },
      "hive-atlas-application.properties" : {
        "properties": {}
      },
      "druid-coordinator": {
        "properties": {'druid.port': 8081}
      },
      "druid-broker": {
        "properties": {'druid.port': 8082}
      },
      "druid-common": {
        "properties": {
          "database_name": "druid",
          "metastore_hostname": "c6401.ambari.apache.org",
          "druid.metadata.storage.type": "mysql",
          'druid.metadata.storage.connector.port': '3306',
          'druid.metadata.storage.connector.user': 'druid',
          'druid.metadata.storage.connector.connectURI': 'jdbc:mysql://c6401.ambari.apache.org:3306/druid?createDatabaseIfNotExist=true'
        }
      }
    }

    clusterData = {
      "cpu": 4,
      "mapMemory": 3000,
      "amMemory": 2000,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }

    hosts = {
      "items": [
      {
        "Hosts": {
          "cpu_count": 6,
          "total_mem": 50331648,
          "disk_info": [
            {"mountpoint": "/"},
            {"mountpoint": "/dev/shm"},
            {"mountpoint": "/vagrant"},
            {"mountpoint": "/"},
            {"mountpoint": "/dev/shm"},
            {"mountpoint": "/vagrant"}
          ],
          "public_host_name": "c6401.ambari.apache.org",
          "host_name": "c6401.ambari.apache.org"
        },
      }
      ]
    }

    services = {
      "services":
        [
          {
            "StackServices" : {
             "service_name" : "YARN"
            },
            "components" : []
          },
          {
            "StackServices" : {
              "service_name" : "HIVE",
              "service_version" : "1.2.1.2.6"
            },
            "components": []
          },
          {
            "StackServices" : {
              "service_name" : "ATLAS",
              "service_version": "0.8.0"
            },
            "components": []
          },
          {
            "StackServices" : {
              "service_name" : "RANGER",
              "service_version": "0.7.0"
            },
            "components": []
          },
          {
            "StackServices": {
              "service_name": "DRUID",
            },
            "components": [
              {
                "StackServiceComponents": {
                  "component_name": "DRUID_COORDINATOR",
                  "hostnames": ["c6401.ambari.apache.org"]
                },
              },
              {
                "StackServiceComponents": {
                  "component_name": "DRUID_OVERLORD",
                  "hostnames": ["c6401.ambari.apache.org"]
                },
              },
              {
                "StackServiceComponents": {
                  "component_name": "DRUID_BROKER",
                  "hostnames": ["c6401.ambari.apache.org"]
                },
              },
              {
                "StackServiceComponents": {
                  "component_name": "DRUID_ROUTER",
                  "hostnames": ["c6401.ambari.apache.org"]
                },
              }
            ]
          }
        ],
      "Versions": {
        "stack_name" : "HDP",
        "stack_version": "2.6"
      },
      "changed-configurations": [],
      "configurations": configurations,
      "ambari-server-properties": {"ambari-server.user":"ambari_user"}
    }

    expected = {
      'yarn-env': {
        'properties': {
          'min_user_id': '500',
          'apptimelineserver_heapsize': '8072',
          'service_check.queue.name': 'default'
        }
      },
      'ranger-hive-plugin-properties': {
        'properties': {
          'REPOSITORY_CONFIG_USERNAME': 'custom_hive'
        }
      },
      'webhcat-site': {
        'properties': {
          'templeton.hadoop.queue.name': 'default'
        }
      },
      'hive-interactive-env': {
        'properties': {
          'enable_hive_interactive': 'false'
        },
        'property_attributes': {
          'num_llap_nodes': {
            'read_only': 'true'
          }
        }
      },
      'hive-env': {
        'properties': {
          'hive.atlas.hook': 'true',
          'hive_security_authorization': 'Ranger',
          'hive_exec_orc_storage_strategy': 'SPEED',
          'hive_timeline_logging_enabled': 'true',
          'hive_txn_acid': 'off'
        }
      },
      'hiveserver2-site': {
        'properties': {
          'hive.security.authorization.enabled': 'true',
          'hive.conf.restricted.list': 'hive.security.authenticator.manager,hive.security.authorization.manager,hive.security.metastore.authorization.manager,hive.security.metastore.authenticator.manager,hive.users.in.admin.role,hive.server2.xsrf.filter.enabled,hive.security.authorization.enabled',
          'hive.security.authenticator.manager': 'org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator',
          'hive.security.authorization.manager': 'org.apache.ranger.authorization.hive.authorizer.RangerHiveAuthorizerFactory'
        }
      },
      'hive-site': {
        'properties': {
          'hive.tez.container.size': '768',
          'hive.exec.orc.default.stripe.size': '67108864',
          'hive.execution.engine': 'mr',
          'hive.vectorized.execution.reduce.enabled': 'false',
          'hive.compactor.worker.threads': '0',
          'hive.compactor.initiator.on': 'false',
          'hive.exec.pre.hooks': 'org.apache.hadoop.hive.ql.hooks.ATSHook',
          'hive.compute.query.using.stats': 'true',
          'hive.exec.orc.default.compress': 'ZLIB',
          'hive.exec.orc.encoding.strategy': 'SPEED',
          'hive.server2.tez.initialize.default.sessions': 'false',
          'hive.security.authorization.enabled': 'true',
          'hive.exec.post.hooks': 'org.apache.hadoop.hive.ql.hooks.ATSHook,org.apache.atlas.hive.hook.HiveHook',
          'hive.server2.tez.default.queues': 'default',
          'hive.prewarm.enabled': 'false',
          'hive.exec.orc.compression.strategy': 'SPEED',
          'hive.optimize.index.filter': 'true',
          'hive.auto.convert.join.noconditionaltask.size': '214748364',
          'hive.vectorized.execution.enabled': 'true',
          'hive.exec.reducers.bytes.per.reducer': '67108864',
          'hive.txn.manager': 'org.apache.hadoop.hive.ql.lockmgr.DummyTxnManager',
          'hive.server2.tez.sessions.per.default.queue': '1',
          'hive.prewarm.numcontainers': '3',
          'hive.tez.dynamic.partition.pruning': 'true',
          'hive.tez.auto.reducer.parallelism': 'true',
          'hive.server2.use.SSL': 'false',
          'hive.exec.failure.hooks': 'org.apache.hadoop.hive.ql.hooks.ATSHook',
          'hive.support.concurrency': 'false',
          'hive.tez.java.opts': '-server -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps',
          'hive.security.metastore.authorization.manager': 'org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider',
          'hive.exec.dynamic.partition.mode': 'strict',
          'hive.optimize.sort.dynamic.partition': 'false',
          'hive.server2.enable.doAs': 'false'
        },
        'property_attributes': {
          'hive.tez.container.size': {
            'minimum': '256',
            'maximum': '768'
          },
          'atlas.cluster.name': {
            'delete': 'true'
          },
          'hive.server2.tez.default.queues': {
            'entries': [
              {
                'value': 'default',
                'label': 'default queue'
              }
            ]
          },
          'datanucleus.rdbms.datastoreAdapterClassName': {
            'delete': 'true'
          },
          'hive.auto.convert.join.noconditionaltask.size': {
            'maximum': '644245094'
          },
          'atlas.rest.address': {
            'delete': 'true'
          },
          'hive.server2.authentication.pam.services': {
            'delete': 'true'
          },
          'hive.server2.custom.authentication.class': {
            'delete': 'true'
          },
          'hive.server2.authentication.kerberos.principal': {
            'delete': 'true'
          },
          'hive.server2.authentication.kerberos.keytab': {
            'delete': 'true'
          },
          'hive.server2.authentication.ldap.url': {
            'delete': 'true'
          }
        }
      },
      'hive-interactive-site': {
        'properties': {
          'hive.druid.broker.address.default': 'c6401.ambari.apache.org:8082',
          'hive.druid.coordinator.address.default': 'c6401.ambari.apache.org:8081',
          'hive.druid.metadata.db.type': 'mysql',
          'hive.druid.metadata.uri': 'jdbc:mysql://c6401.ambari.apache.org:3306/druid?createDatabaseIfNotExist=true',
          'hive.druid.metadata.username': 'druid'
        }
      },
      'yarn-site': {
        'properties': {
          'hadoop.registry.rm.enabled': 'false',
          'yarn.timeline-service.entity-group-fs-store.group-id-plugin-classes': '',
          'yarn.scheduler.minimum-allocation-vcores': '1',
          'yarn.scheduler.maximum-allocation-vcores': '4',
          'yarn.nodemanager.resource.memory-mb': '768',
          'yarn.nodemanager.local-dirs': '/hadoop/yarn/local,/dev/shm/hadoop/yarn/local,/vagrant/hadoop/yarn/local',
          'yarn.nodemanager.log-dirs': '/hadoop/yarn/log,/dev/shm/hadoop/yarn/log,/vagrant/hadoop/yarn/log',
          'yarn.timeline-service.entity-group-fs-store.app-cache-size': '10',
          'yarn.scheduler.minimum-allocation-mb': '256',
          'yarn.timeline-service.entity-group-fs-store.group-id-plugin-classpath': '',
          'yarn.resourcemanager.monitor.capacity.preemption.total_preemption_per_round': '1.0',
          'yarn.nodemanager.resource.cpu-vcores': '4',
          'yarn.scheduler.maximum-allocation-mb': '768',
          'yarn.nodemanager.linux-container-executor.group': 'hadoop',
          'yarn.timeline-service.leveldb-state-store.path': '/hadoop/yarn/timeline',
          'yarn.timeline-service.leveldb-timeline-store.path': '/hadoop/yarn/timeline'
        },
        'property_attributes': {
          'yarn.authorization-provider': {
            'delete': 'true'
          }
        }
      },
      'hive-atlas-application.properties' : {
        'properties' : {},
        'property_attributes' : {
            'atlas.jaas.ticketBased-KafkaClient.loginModuleControlFlag': {'delete': 'true'},
            'atlas.jaas.ticketBased-KafkaClient.loginModuleName': {'delete': 'true'},
            'atlas.jaas.ticketBased-KafkaClient.option.useTicketCache': {'delete': 'true'}
        }
      }
    }

    recommendedConfigurations = {}
    self.stackAdvisor.recommendHIVEConfigurations(recommendedConfigurations, clusterData, services, hosts)
    self.assertEquals(recommendedConfigurations, expected)

    services['configurations']['hive-env']['properties']['hive_user'] = 'hive'
    expected['ranger-hive-plugin-properties']['properties']['REPOSITORY_CONFIG_USERNAME'] = 'hive'
    services['configurations']['cluster-env']['properties']['security_enabled'] = 'true'
    expected['hive-atlas-application.properties']['properties']['atlas.jaas.ticketBased-KafkaClient.loginModuleControlFlag'] = 'required'
    expected['hive-atlas-application.properties']['properties']['atlas.jaas.ticketBased-KafkaClient.loginModuleName'] = 'com.sun.security.auth.module.Krb5LoginModule'
    expected['hive-atlas-application.properties']['properties']['atlas.jaas.ticketBased-KafkaClient.option.useTicketCache'] = 'true'
    del expected['hive-atlas-application.properties']['property_attributes']
    expected['core-site'] = {
      'properties': {}
    }

    # case there is router in the stack
    services['configurations']['druid-router'] = {}
    services['configurations']['druid-router']['properties'] = {}
    services['configurations']['druid-router']['properties']['druid.port'] = 8083
    expected['hive-interactive-site']['properties']['hive.druid.broker.address.default'] = 'c6401.ambari.apache.org:8083'

    recommendedConfigurations = {}
    self.stackAdvisor.recommendHIVEConfigurations(recommendedConfigurations, clusterData, services, hosts)
    self.assertEquals(recommendedConfigurations, expected)

    # case there are not druid-common configs present
    del services['configurations']['druid-common']
    expected['hive-interactive-site']['properties']['hive.druid.broker.address.default'] = 'c6401.ambari.apache.org:8083'
    expected['hive-interactive-site']['properties']['hive.druid.metadata.uri'] = ''
    expected['hive-interactive-site']['properties']['hive.druid.metadata.username'] = ''
    expected['hive-interactive-site']['properties']['hive.druid.metadata.db.type'] = ''

    recommendedConfigurations = {}
    self.stackAdvisor.recommendHIVEConfigurations(recommendedConfigurations, clusterData, services, hosts)
    self.assertEquals(recommendedConfigurations, expected)


  def test_recommendHBASEConfigurations(self):
    configurations = {
      "ranger-hbase-plugin-properties": {
        "properties": {
          "ranger-hbase-plugin-enabled": "Yes",
          "REPOSITORY_CONFIG_USERNAME":"hbase"
        }
      },
      "hbase-env":{
        "properties":{
          "hbase_user":"custom_hbase"
        }
      }
    }

    services = {
      "services": [{
            "StackServices": {
              "service_name" : "HBASE",
              "service_version" : "1.1.2.2.6"
            },
            "components": [
            ]
          }
        ],
      "Versions": {
        "stack_name" : "HDP",
        "stack_version": "2.6"
      },
      "configurations": configurations,
      "ambari-server-properties": {"ambari-server.user":"ambari_user"}
    }


    clusterData = {
      "totalAvailableRam": 2048,
      "hBaseInstalled": True,
      "hbaseRam": 112,
      "reservedRam": 128
    }
    expected = {
      'hbase-site': {
        'properties': {
          'hbase.regionserver.wal.codec': 'org.apache.hadoop.hbase.regionserver.wal.WALCellCodec',
          'hbase.master.ui.readonly': 'false',
          'hbase.security.authorization': 'true',
          'hbase.bucketcache.percentage.in.combinedcache': '1.0000',
          'hbase.regionserver.global.memstore.size': '0.4',
          'hfile.block.cache.size': '0.4',
          'hbase.coprocessor.region.classes': 'org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint,org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor',
          'hbase.bucketcache.size': '92160',
          'hbase.coprocessor.regionserver.classes': 'org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor',
          'hbase.coprocessor.master.classes': 'org.apache.ranger.authorization.hbase.RangerAuthorizationCoprocessor',
          'hbase.bucketcache.ioengine': 'offheap'
        },
        'property_attributes': {
          'hbase.bucketcache.percentage.in.combinedcache': {
            'delete': 'true'
          },
          'hbase.region.server.rpc.scheduler.factory.class': {
            'delete': 'true'
          }
        }
      },
      'ranger-hbase-plugin-properties': {
        'properties': {
          'REPOSITORY_CONFIG_USERNAME': 'custom_hbase',
          'ranger-hbase-plugin-enabled': 'Yes'
        }
      },
      'hbase-env': {
        'properties': {
          'hbase_user': 'custom_hbase',
          'hbase_master_heapsize': '1024',
          'hbase_regionserver_heapsize': '20480',
          'hbase_max_direct_memory_size': '94208'
        }
      },
      'core-site': {
        'properties': {}
      }
    }
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)
    configurations['hbase-env']['properties']['hbase_user'] = 'hbase'
    expected['hbase-env']['properties']['hbase_user'] = 'hbase'
    expected['ranger-hbase-plugin-properties']['properties']['REPOSITORY_CONFIG_USERNAME'] = 'hbase'
    self.stackAdvisor.recommendHBASEConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendYARNConfigurations(self):
    configurations = {
      "yarn-env": {
        "properties": {
          "yarn_user" : "custom_yarn"
        }
      },
      "ranger-yarn-plugin-properties": {
        "properties": {
          "ranger-yarn-plugin-enabled" : "Yes",
          "REPOSITORY_CONFIG_USERNAME":"yarn"
        }
      }
    }
    services = {
      "services" : [{
        "StackServices": {
          "service_name" : "YARN",
          "service_version" : "2.7.3.2.6"
        },
        "components": []
      }
      ],
      "changed-configurations": [
      ],
      "configurations": configurations
    }


    clusterData = {
      "cpu": 4,
      "containers" : 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      'yarn-env': {
        'properties': {
          'yarn_user': 'custom_yarn',
          'service_check.queue.name': 'default',
          'min_user_id': '500',
          'apptimelineserver_heapsize': '2048'
        }
      },
      'ranger-yarn-plugin-properties': {
        'properties': {
          'ranger-yarn-plugin-enabled': 'Yes',
          'REPOSITORY_CONFIG_USERNAME': 'custom_yarn'
        }
      },
      'yarn-site': {
        'properties': {
          'hadoop.registry.rm.enabled': 'false',
          'yarn.timeline-service.entity-group-fs-store.group-id-plugin-classes': '',
          'yarn.authorization-provider': 'org.apache.ranger.authorization.yarn.authorizer.RangerYarnAuthorizer',
          'yarn.acl.enable': 'true',
          'yarn.scheduler.minimum-allocation-vcores': '1',
          'yarn.scheduler.maximum-allocation-vcores': '4',
          'yarn.nodemanager.resource.memory-mb': '1280',
          'yarn.scheduler.minimum-allocation-mb': '256',
          'yarn.timeline-service.entity-group-fs-store.group-id-plugin-classpath': '',
          'yarn.resourcemanager.monitor.capacity.preemption.total_preemption_per_round': '1.0',
          'yarn.nodemanager.resource.cpu-vcores': '4',
          'yarn.scheduler.maximum-allocation-mb': '1280',
          'yarn.nodemanager.linux-container-executor.group': 'hadoop',
          'yarn.nodemanager.local-dirs': '/hadoop/yarn/local,/dev/shm/hadoop/yarn/local,/vagrant/hadoop/yarn/local',
          'yarn.nodemanager.log-dirs': '/hadoop/yarn/log,/dev/shm/hadoop/yarn/log,/vagrant/hadoop/yarn/log',
          'yarn.timeline-service.entity-group-fs-store.app-cache-size': '7',
          'yarn.timeline-service.leveldb-state-store.path': '/hadoop/yarn/timeline',
          'yarn.timeline-service.leveldb-timeline-store.path': '/hadoop/yarn/timeline'

        }
      }
    }

    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 6,
            "total_mem": 4096,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          },
        }
      ]
    }

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    configurations['yarn-env']['properties']['yarn_user'] = 'yarn'
    expected['yarn-env']['properties']['yarn_user'] = 'yarn'
    expected['ranger-yarn-plugin-properties']['properties']['REPOSITORY_CONFIG_USERNAME'] = 'yarn'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)



  def test_recommendYARNConfigurations_for_ats_heapsize_and_cache(self):
    configurations = {
      "yarn-env": {
        "properties": {
          "yarn_user" : "custom_yarn"
        }
      },
      "ranger-yarn-plugin-properties": {
        "properties": {
          "ranger-yarn-plugin-enabled" : "Yes",
          "REPOSITORY_CONFIG_USERNAME":"yarn"
        }
      }
    }
    services = {
      "services" : [{
        "StackServices": {
          "service_name" : "YARN",
          "service_version" : "2.7.3.2.6"
        },
        "components": []
      }
      ],
      "changed-configurations": [
      ],
      "configurations": configurations
    }


    clusterData = {
      "cpu": 4,
      "containers" : 5,
      "ramPerContainer": 256,
      "yarnMinContainerSize": 256
    }
    expected = {
      'yarn-env': {
        'properties': {
          'yarn_user': 'custom_yarn',
          'service_check.queue.name': 'default',
          'min_user_id': '500',
          'apptimelineserver_heapsize': '1024'
        }
      },
      'ranger-yarn-plugin-properties': {
        'properties': {
          'ranger-yarn-plugin-enabled': 'Yes',
          'REPOSITORY_CONFIG_USERNAME': 'custom_yarn'
        }
      },
      'yarn-site': {
        'properties': {
          'hadoop.registry.rm.enabled': 'false',
          'yarn.timeline-service.entity-group-fs-store.group-id-plugin-classes': '',
          'yarn.authorization-provider': 'org.apache.ranger.authorization.yarn.authorizer.RangerYarnAuthorizer',
          'yarn.acl.enable': 'true',
          'yarn.scheduler.minimum-allocation-vcores': '1',
          'yarn.scheduler.maximum-allocation-vcores': '4',
          'yarn.nodemanager.resource.memory-mb': '1280',
          'yarn.scheduler.minimum-allocation-mb': '256',
          'yarn.timeline-service.entity-group-fs-store.group-id-plugin-classpath': '',
          'yarn.resourcemanager.monitor.capacity.preemption.total_preemption_per_round': '1.0',
          'yarn.nodemanager.resource.cpu-vcores': '4',
          'yarn.scheduler.maximum-allocation-mb': '1280',
          'yarn.nodemanager.linux-container-executor.group': 'hadoop',
          'yarn.nodemanager.local-dirs': '/hadoop/yarn/local,/dev/shm/hadoop/yarn/local,/vagrant/hadoop/yarn/local',
          'yarn.nodemanager.log-dirs': '/hadoop/yarn/log,/dev/shm/hadoop/yarn/log,/vagrant/hadoop/yarn/log',
          'yarn.timeline-service.entity-group-fs-store.app-cache-size': '3',
          'yarn.timeline-service.leveldb-state-store.path': '/hadoop/yarn/timeline',
          'yarn.timeline-service.leveldb-timeline-store.path': '/hadoop/yarn/timeline'

        }
      }
    }

    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 6,
            "total_mem": 2048,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          },
        }
      ]
    }




    '''
    Test 1 :
    I/P:
       - 'changed-configurations' is empty (doesnt have 'yarn.timeline-service.entity-group-fs-store.app-cache-size')
       - 'host_mem' = 2048
    O/P :
       -  Config value recommended for:
           - yarn.timeline-service.entity-group-fs-store.app-cache-size = 3
           - apptimelineserver_heapsize = 1024
    '''

    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)



    '''
    Test 2 :
    I/P:
       - 'changed-configurations' is empty (doesnt have 'yarn.timeline-service.entity-group-fs-store.app-cache-size')
       - 'host_mem' = 4096
    O/P :
       -  Config value recommended for:
           - yarn.timeline-service.entity-group-fs-store.app-cache-size = 7
           - apptimelineserver_heapsize = 2048
    '''
    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 6,
            "total_mem": 4096,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          },
        }
      ]
    }

    expected['yarn-env']['properties']['apptimelineserver_heapsize'] = '2048'
    expected['yarn-site']['properties']['yarn.timeline-service.entity-group-fs-store.app-cache-size'] = '7'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)



    '''
    Test 3 :
    I/P:
       - 'changed-configurations' is empty (doesnt have 'yarn.timeline-service.entity-group-fs-store.app-cache-size')
       - 'host_mem' = 8192
    O/P :
       -  Config value recommended for:
           - yarn.timeline-service.entity-group-fs-store.app-cache-size = 10
           - apptimelineserver_heapsize = 4096
    '''
    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 6,
            "total_mem": 8192,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          },
        }
      ]
    }

    expected['yarn-env']['properties']['apptimelineserver_heapsize'] = '4096'
    expected['yarn-site']['properties']['yarn.timeline-service.entity-group-fs-store.app-cache-size'] = '10'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)



    '''
    Test 4 :
    I/P:
       - 'changed-configurations' has 'yarn.timeline-service.entity-group-fs-store.app-cache-size'
       - 'host_mem' = 2048
    O/P :
       -  Config value recommended for:
           - apptimelineserver_heapsize = 4096
    '''

    services["changed-configurations"] = [
      {
        u'old_value': u'10',
        u'type': u'yarn-site',
        u'name': u'yarn.timeline-service.entity-group-fs-store.app-cache-size'
      }
    ]

    services["configurations"] = {
      "yarn-env": {
        "properties": {
          "yarn_user" : "custom_yarn",
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.timeline-service.entity-group-fs-store.app-cache-size" : "7"
        }
      },
      "ranger-yarn-plugin-properties": {
        "properties": {
          "ranger-yarn-plugin-enabled" : "Yes",
          "REPOSITORY_CONFIG_USERNAME":"yarn"
        }
      }
    }

    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 6,
            "total_mem": 4096,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          },
        }
      ]
    }



    '''
    Test 5 :
    I/P:
       - 'changed-configurations' has 'yarn.timeline-service.entity-group-fs-store.app-cache-size'
       - 'host_mem' = 4096
    O/P :
       -  Config value recommended for:
           - apptimelineserver_heapsize = 2048
    '''

    services["changed-configurations"] = [
      {
        u'old_value': u'10',
        u'type': u'yarn-site',
        u'name': u'yarn.timeline-service.entity-group-fs-store.app-cache-size'
      }
    ]

    services["configurations"] = {
      "yarn-env": {
        "properties": {
          "yarn_user" : "custom_yarn",
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.timeline-service.entity-group-fs-store.app-cache-size" : "7"
        }
      },
      "ranger-yarn-plugin-properties": {
        "properties": {
          "ranger-yarn-plugin-enabled" : "Yes",
          "REPOSITORY_CONFIG_USERNAME":"yarn"
        }
      }
    }

    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 6,
            "total_mem": 4096,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          },
        }
      ]
    }


    expected['yarn-env']['properties']['apptimelineserver_heapsize'] = '2048'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)



    '''
    Test 6 :
    I/P:
       - 'changed-configurations' has 'yarn.timeline-service.entity-group-fs-store.app-cache-size'
       - 'host_mem' = 8196
    O/P :
       -  Config value recommended for:
           - Shouldn't have yarn.timeline-service.entity-group-fs-store.app-cache-size
           - apptimelineserver_heapsize = 4572
    '''

    services["changed-configurations"] = [
      {
        u'old_value': u'10',
        u'type': u'yarn-site',
        u'name': u'yarn.timeline-service.entity-group-fs-store.app-cache-size'
      }
    ]

    services["configurations"] = {
      "yarn-env": {
        "properties": {
          "yarn_user" : "custom_yarn",
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.timeline-service.entity-group-fs-store.app-cache-size" : "3"
        }
      },
      "ranger-yarn-plugin-properties": {
        "properties": {
          "ranger-yarn-plugin-enabled" : "Yes",
          "REPOSITORY_CONFIG_USERNAME":"yarn"
        }
      }
    }

    hosts = {
      "items": [
        {
          "Hosts": {
            "cpu_count": 6,
            "total_mem": 16392,
            "disk_info": [
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"},
              {"mountpoint": "/"},
              {"mountpoint": "/dev/shm"},
              {"mountpoint": "/vagrant"}
            ],
            "public_host_name": "c6401.ambari.apache.org",
            "host_name": "c6401.ambari.apache.org"
          },
        }
      ]
    }


    expected['yarn-env']['properties']['apptimelineserver_heapsize'] = '4572'
    self.stackAdvisor.recommendYARNConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)


  def test_recommendKAFKAConfigurations(self):
    configurations = {
      "kafka-env": {
        "properties": {
          "kafka_user" : "custom_kafka"
        }
      },
      "ranger-kafka-plugin-properties": {
        "properties": {
          "ranger-kafka-plugin-enabled" : "Yes",
          "REPOSITORY_CONFIG_USERNAME":"kafka"
        }
      }
    }
    clusterData = []
    services = {
      "services" : [{
        "StackServices": {
          "service_name" : "KAFKA",
          "service_version" : "0.10.0.2.6"
        },
        "components": []
      }
      ],
      "configurations": configurations
    }

    expected = {
      'kafka-env': {
        'properties': {
          'kafka_user': 'custom_kafka'
        }
      },
      'kafka-log4j': {
        'properties': {}
      },
      'kafka-broker': {
        'properties': {},
        'property_attributes': {
          'principal.to.local.class': {
            'delete': 'true'
          },
          'super.users': {
            'delete': 'true'
          },
          'security.inter.broker.protocol': {
            'delete': 'true'
          },
          'authorizer.class.name': {
            'delete': 'true'
          }
        }
      },
      'ranger-kafka-plugin-properties': {
        'properties': {
          'ranger-kafka-plugin-enabled': 'Yes',
          'REPOSITORY_CONFIG_USERNAME': 'custom_kafka'
        }
      }
    }

    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)
    configurations['kafka-env']['properties']['kafka_user'] = 'kafka'
    expected['kafka-env']['properties']['kafka_user'] = 'kafka'
    expected['ranger-kafka-plugin-properties']['properties']['REPOSITORY_CONFIG_USERNAME'] = 'kafka'
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

def load_json(self, filename):
  file = os.path.join(self.testDirectory, filename)
  with open(file, 'rb') as f:
    data = json.load(f)
  return data
