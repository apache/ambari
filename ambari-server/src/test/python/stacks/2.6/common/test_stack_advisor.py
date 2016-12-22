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
                        'properties': {'druid.processing.numThreads': '3', 'druid.server.http.numThreads': '40'}},
                        'druid-broker': {
                          'properties': {'druid.processing.numThreads': '3', 'druid.server.http.numThreads': '40'}},
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
                        'properties': {'druid.processing.numThreads': '3', 'druid.server.http.numThreads': '40'}},
                        'druid-broker': {
                          'properties': {'druid.processing.numThreads': '3', 'druid.server.http.numThreads': '40'}},
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
                        'properties': {'druid.processing.numThreads': '3', 'druid.server.http.numThreads': '40'}},
                        'druid-broker': {
                          'properties': {'druid.processing.numThreads': '3', 'druid.server.http.numThreads': '40'}},
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
            "total_mem": 1922680,
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
                        'properties': {'druid.processing.numThreads': '2', 'druid.server.http.numThreads': '40'}},
                        'druid-broker': {
                          'properties': {'druid.processing.numThreads': '1', 'druid.server.http.numThreads': '40'}},
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
                                                              'druid.broker.jvm.heap.memory': {'maximum': '1877'}}}}
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

def load_json(self, filename):
  file = os.path.join(self.testDirectory, filename)
  with open(file, 'rb') as f:
    data = json.load(f)
  return data
