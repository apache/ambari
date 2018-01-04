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
from mock.mock import patch
import unittest

class TestHDP23StackAdvisor(TestCase):

  def setUp(self):
    import imp
    self.maxDiff = None
    if 'util' in dir(unittest): unittest.util._MAX_LENGTH=2000
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


  def fqdn_mock_result(value=None):
      return 'c6401.ambari.apache.org' if value is None else value


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
    expected = {'component-name': 'SPARK_THRIFTSERVER', 'message': 'Spark Thrift Server requires HIVE_METASTORE to be present in the cluster.', 'type': 'host-component', 'level': 'ERROR'}
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
    configurations = {
      "hdfs-site": {
        "properties": {
          "dfs.namenode.inode.attributes.provider.class": "org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer",
        }
      },
      "ranger-hdfs-plugin-properties": {
        "properties": {
          "ranger-hdfs-plugin-enabled": "No"
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
              "service_version" : "2.6.0.2.2"
            },
            "components": [
            ]
          }
        ],
      "Versions": {
        "stack_version": "2.3"
      },
      "configurations": configurations,
      "ambari-server-properties": {"ambari-server.user":"ambari_user"}
    }

    # Test with Ranger HDFS plugin disabled
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hdfs-site']['property_attributes']['dfs.namenode.inode.attributes.provider.class'], {'delete': 'true'}, "Test with Ranger HDFS plugin is disabled")

    # Test with Ranger HDFS plugin is enabled
    configurations['hdfs-site']['properties'] = {}
    configurations['hdfs-site']['property_attributes'] = {}
    services['configurations']['ranger-hdfs-plugin-properties']['properties']['ranger-hdfs-plugin-enabled'] = 'Yes'
    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hdfs-site']['properties']['dfs.namenode.inode.attributes.provider.class'], 'org.apache.ranger.authorization.hadoop.RangerHdfsAuthorizer', "Test with Ranger HDFS plugin is enabled")

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
      "context" : {
        "call_type" : "recommendConfigurations"
      },
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
              "service_name": "RANGER",
              "service_version": "0.5.0.2.3"
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
          },
          {
            "StackServices": {
              "service_name": "ZOOKEEPER"
            },
            "components": [{
              "StackServiceComponents": {
                "component_name": "ZOOKEEPER_SERVER",
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
          "properties": {}
        },
        "cluster-env": {
          "properties": {
            "security_enabled" : "true"
          },
          "property_attributes": {}
        },
        "kafka-broker": {
          "properties": {
            "authorizer.class.name" : "kafka.security.auth.SimpleAclAuthorizer"
          },
          "property_attributes": {}
        },
        "ranger-kafka-plugin-properties": {
          "properties": {
            "ranger-kafka-plugin-enabled": "No",
            "zookeeper.connect": ""
          }
        },
        "kafka-log4j": {
          "properties": {
            "content": "kafka.logs.dir=logs"
          }
        },
        "zoo.cfg" : {
          "properties": {
            "clientPort": "2181"
          }
        }
      }
    }

    # Test authorizer.class.name with Ranger Kafka plugin disabled in non-kerberos environment
    services['configurations']['cluster-env']['properties']['security_enabled'] = "false"
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['kafka-broker']['property_attributes']['authorizer.class.name'], {'delete': 'true'}, "Test authorizer.class.name with Ranger Kafka plugin is disabled in non-kerberos environment")

    # Test authorizer.class.name with Ranger Kafka plugin disabled in kerberos environment
    services['configurations']['cluster-env']['properties']['security_enabled'] = "true"
    configurations['kafka-broker']['properties'] = {}
    configurations['kafka-broker']['property_attributes'] = {}
    services['configurations']['kafka-broker']['properties']['security.inter.broker.protocol'] = 'PLAINTEXTSASL'
    services['configurations']['kafka-broker']['properties']['authorizer.class.name'] = 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer'
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['kafka-broker']['properties']['authorizer.class.name'], 'kafka.security.auth.SimpleAclAuthorizer' , "Test authorizer.class.name with Ranger Kafka plugin disabled in kerberos environment")

    # Advise 'PLAINTEXTSASL' for secure cluster by default
    services['configurations']['cluster-env']['properties']['security_enabled'] = "true"
    configurations['kafka-broker']['properties'] = {}
    configurations['kafka-broker']['property_attributes'] = {}
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEqual(configurations['kafka-broker']['properties']['security.inter.broker.protocol'], 'PLAINTEXTSASL')

    # Secure security.inter.broker.protocol values should be retained by stack advisor
    services['configurations']['cluster-env']['properties']['security_enabled'] = "true"
    configurations['kafka-broker']['properties'] = {}
    configurations['kafka-broker']['property_attributes'] = {}
    for proto in ('PLAINTEXTSASL', 'SASL_PLAINTEXT', 'SASL_SSL'):
      services['configurations']['kafka-broker']['properties']['security.inter.broker.protocol'] = proto
      self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
      self.assertEqual(configurations['kafka-broker']['properties']['security.inter.broker.protocol'], proto)

    # Test authorizer.class.name with Ranger Kafka plugin enabled in non-kerberos environment
    services['configurations']['cluster-env']['properties']['security_enabled'] = "false"
    configurations['kafka-broker']['properties'] = {}
    configurations['kafka-broker']['property_attributes'] = {}
    del services['configurations']['kafka-broker']['properties']['security.inter.broker.protocol']
    services['configurations']['kafka-broker']['properties']['authorizer.class.name'] = 'kafka.security.auth.SimpleAclAuthorizer'
    services['configurations']['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'] = 'Yes'
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['kafka-broker']['properties']['authorizer.class.name'], 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer', "Test authorizer.class.name with Ranger Kafka plugin enabled in kerberos environment")

    services['configurations']['cluster-env']['properties']['security_enabled'] = "false"
    configurations['kafka-broker']['properties'] = {}
    configurations['kafka-broker']['property_attributes'] = {}
    services['configurations']['kafka-broker']['properties']['security.inter.broker.protocol'] = 'PLAINTEXTSASL'
    services['configurations']['kafka-broker']['properties']['authorizer.class.name'] = 'kafka.security.auth.SimpleAclAuthorizer'
    services['configurations']['ranger-kafka-plugin-properties']['properties']['ranger-kafka-plugin-enabled'] = 'Yes'
    self.stackAdvisor.recommendKAFKAConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations['kafka-broker']['properties']['authorizer.class.name'], 'org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer', "Test authorizer.class.name with Ranger Kafka plugin enabled in kerberos environment")
    self.assertEquals(configurations['ranger-kafka-plugin-properties']['properties']['zookeeper.connect'], 'host1:2181')
    self.assertTrue('security.inter.broker.protocol' not in configurations['kafka-broker']['properties'])

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
          'hive_txn_acid': 'off',
          'hive.atlas.hook': 'false'
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
         'hive.auto.convert.join.noconditionaltask.size': {'maximum': '644245094'},
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
         'datanucleus.rdbms.datastoreAdapterClassName': {'delete': 'true'},
         'hive.tez.container.size': {'maximum': '8192', 'minimum': '256'}
        }
      },
      'hiveserver2-site': {
        'properties': {
        },
        'property_attributes': {
         'hive.security.authorization.manager': {'delete': 'true'},
         'hive.security.authenticator.manager': {'delete': 'true'}
        }
      },
      'webhcat-site': {
        'properties': {
          'templeton.hadoop.queue.name': 'queue2'
        }
      }
    }
    services = {
      "Versions": {
        "parent_stack_version": "2.2",
        "stack_name": "HDP",
        "stack_version": "2.3",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.2", "2.1", "2.0.6"]
        }
      },
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
            "hive.atlas.hook": "false"
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

  def test_recommendHiveConfigurations_with_atlas(self):
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
          'hive_txn_acid': 'off',
          'hive.atlas.hook': 'true'
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
          'hive.exec.dynamic.partition.mode': 'strict',
          'hive.exec.failure.hooks': 'org.apache.hadoop.hive.ql.hooks.ATSHook',
          'hive.exec.orc.compression.strategy': 'SPEED',
          'hive.exec.orc.default.compress': 'ZLIB',
          'hive.exec.orc.default.stripe.size': '67108864',
          'hive.exec.orc.encoding.strategy': 'SPEED',
          'hive.exec.post.hooks': 'org.apache.hadoop.hive.ql.hooks.ATSHook,org.apache.atlas.hive.hook.HiveHook',
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
          'hive.auto.convert.join.noconditionaltask.size': {'maximum': '644245094'},
          'hive.tez.container.size': {'maximum': '8192', 'minimum': '256'},
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
      },
      'webhcat-site': {
        'properties': {
          'templeton.hadoop.queue.name': 'queue2'
        }
      }
    }
    services = {
      "Versions": {
        "parent_stack_version": "2.2",
        "stack_name": "HDP",
        "stack_version": "2.3",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.2", "2.1", "2.0.6"]
        }
      },
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
          "href": "/api/v1/stacks/HDP/versions/2.2/services/ATLAS",
          "StackServices": {
            "service_name": "ATLAS",
            "service_version": "2.6.0.2.2",
            "stack_name": "HDP",
            "stack_version": "2.3"
          },
          "components": [
            {
              "StackServiceComponents": {
                "advertise_version": "false",
                "cardinality": "1",
                "component_category": "MASTER",
                "component_name": "ATLAS_SERVER",
                "display_name": "Atlas Server",
                "is_client": "false",
                "is_master": "true",
                "hostnames": []
              },
              "dependencies": []
            }
          ]
        }
      ],
      "configurations": {
        "capacity-scheduler": {
          "properties": {
            "capacity-scheduler" :"yarn.scheduler.capacity.root.queues=queue1,queue2"
          }
        },
        "hive-env": {
          "properties": {
            "hive.atlas.hook": "false"
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
          "tez.runtime.io.sort.mb": "202",
          "tez.session.am.dag.submit.timeout.secs": "600",
          "tez.runtime.unordered.output.buffer.size-mb": "57",
          "tez.am.resource.memory.mb": "4000",
          "tez.queue.name": "queue2",
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
      "Versions": {
        "parent_stack_version": "2.2",
        "stack_name": "HDP",
        "stack_version": "2.3",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.2", "2.1", "2.0.6"]
        }
      },
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
    for host in hosts["items"]:
      if server_host == host["Hosts"]["host_name"]:
        server_host = host["Hosts"]["public_host_name"]
    tez_ui_url =  "http://" + server_host + ":8080/#/main/view/TEZ/tez_cluster_instance"

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
      "Versions": {
        "parent_stack_version": "2.2",
        "stack_name": "HDP",
        "stack_version": "2.3",
        "stack_hierarchy": {
          "stack_name": "HDP",
          "stack_versions": ["2.2", "2.1", "2.0.6"]
        }
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
        {
          "href": "/api/v1/stacks/HDP/versions/2.3/services/KNOX",
          "StackServices": {
            "service_name": "KNOX",
            "service_version": "0.9.0.2.3",
            "stack_name": "HDP",
            "stack_version": "2.3"
          },
          "components": [
            {
              "href": "/api/v1/stacks/HDP/versions/2.3/services/KNOX/components/KNOX_GATEWAY",
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
            "ranger.sso.providerurl": "",
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
          'policymgr_external_url': 'http://host1:7777'
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
          "ranger.audit.solr.zookeepers": "NONE",
          "ranger.audit.source.type": "solr",
          "ranger.sso.providerurl": "https://c6401.ambari.apache.org:8443/gateway/knoxsso/api/v1/websso"
        }
      },
      'ranger-env': {
        'properties': {
          'ranger-storm-plugin-enabled': 'No',
        }
      },
      'ranger-knox-security': {'properties': {}}
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

  def test_recommendRangerKMSConfigurations(self):
    clusterData = {}
    services = {
      "ambari-server-properties": {
        "ambari-server.user": "root"
        },
      "Versions": {
        "stack_version" : "2.3",
        },
      "services": [
        {
          "StackServices": {
            "service_name": "RANGER_KMS",
            "service_version": "0.5.0.2.3"
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
        "kms-env": {
          "properties": {
            "kms_user": "kmsname"
            }
        },
        "core-site": {
          "properties": {
            "fs.defaultFS": "hdfs://host1:8020"
          }
        },
        'ranger-kms-audit': {
          'properties': {
          }
        },
        'kms-properties': {
          'properties': {
            'DB_FLAVOR': 'ORACLE',
            'db_host' : 'c6401.ambari.apache.org:1521:XE',
            'db_name' : "XE"
          }
        },
        'cluster-env': {
          'properties': {
            'security_enabled': 'false'
          }
        }
      },
      "forced-configurations": []
    }
    expected = {
      'kms-properties': {
        'properties': {}
      },
      'dbks-site': {
        'properties': {
          "ranger.ks.jpa.jdbc.driver" : "oracle.jdbc.driver.OracleDriver",
          "ranger.ks.jpa.jdbc.url" : "jdbc:oracle:thin:@c6401.ambari.apache.org:1521:XE"
        }
      },
      'core-site': {
        'properties': {
        }
      },
      'ranger-kms-audit': {
          'properties': {
          }
      },
      'kms-site': {
        'properties': {
        },
        'property_attributes': {
        'hadoop.kms.proxyuser.HTTP.hosts': {'delete': 'true'},
        'hadoop.kms.proxyuser.HTTP.users': {'delete': 'true'},
        'hadoop.kms.proxyuser.root.hosts': {'delete': 'true'},
        'hadoop.kms.proxyuser.root.users': {'delete': 'true'}
        }
      }
    }

    # non kerberized cluster. There should be no proxyuser configs
    recommendedConfigurations = {}
    self.stackAdvisor.recommendRangerKMSConfigurations(recommendedConfigurations, clusterData, services, None)
    self.assertEquals(recommendedConfigurations, expected)

    # kerberized cluster
    services['services'].append({
      "StackServices": {
        "service_name": "KERBEROS"
      }
    })
    services['configurations']['cluster-env']['properties']['security_enabled'] = "true"
    services['configurations']['cluster-env']['properties']['ambari_principal_name'] = "ambari-cl1@EXAMPLE.COM"

    expected = {
      'kms-properties': {
        'properties': {}
      },
      'dbks-site': {
        'properties': {
          "ranger.ks.jpa.jdbc.driver" : "oracle.jdbc.driver.OracleDriver",
          "ranger.ks.jpa.jdbc.url" : "jdbc:oracle:thin:@c6401.ambari.apache.org:1521:XE"
        }
      },
      'core-site': {
        'properties': {
          'hadoop.proxyuser.kmsname.groups': '*'
        }
      },
      'ranger-kms-audit': {
          'properties': {
          }
      },
      'kms-site': {
        'properties': {
        'hadoop.kms.proxyuser.HTTP.hosts': '*',
        'hadoop.kms.proxyuser.HTTP.users': '*',
        'hadoop.kms.proxyuser.ambari-cl1.hosts': '*',
        'hadoop.kms.proxyuser.ambari-cl1.users': '*'
        }
      }
    }

    # on kerberized cluster property should be recommended
    recommendedConfigurations = {}
    self.stackAdvisor.recommendRangerKMSConfigurations(recommendedConfigurations, clusterData, services, None)
    self.assertEquals(recommendedConfigurations, expected)

    recommendedConfigurations = {}
    services['changed-configurations'] = [
      {
        'type': 'kms-env',
        'name': 'kms_user',
        'old_value': 'kmsname'
      }
    ]
    services['configurations']['kms-env']['properties']['kms_user'] = 'kmsnew'

    expected['core-site'] = {
      'properties': {
        'hadoop.proxyuser.kmsnew.groups': '*'
      },
      'property_attributes': {
        'hadoop.proxyuser.kmsname.groups': {
          'delete': 'true'
        }
      }
    }

    # kms_user was changed, old property should be removed
    self.stackAdvisor.recommendRangerKMSConfigurations(recommendedConfigurations, clusterData, services, None)
    self.assertEquals(recommendedConfigurations, expected)

  def test_recommendStormConfigurations(self):
    self.maxDiff = None
    configurations = {
      "storm-site": {
        "properties": {
          "storm.topology.submission.notifier.plugin.class": "foo"
        }
      },
      "ranger-storm-plugin-properties": {
        "properties": {
          "ranger-storm-plugin-enabled": "No"
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
      'storm-site': {
        'properties': {
          'storm.topology.submission.notifier.plugin.class': 'foo,org.apache.atlas.storm.hook.StormAtlasHook',
        },
        "property_attributes":{
          'nimbus.authorizer': {'delete':'true'}
        }
      },
      "ranger-storm-plugin-properties": {
        "properties": {
          "ranger-storm-plugin-enabled": "No"
        }
      },
      "storm-env": {
        "properties": {
          "storm.atlas.hook": "true"
        }
      }
    }
    services = {
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/ATLAS",
          "StackServices": {
            "service_name": "ATLAS",
            "service_version": "2.6.0.2.2",
            "stack_name": "HDP",
            "stack_version": "2.3"
          },
          "components": [
            {
              "StackServiceComponents": {
                "advertise_version": "false",
                "cardinality": "1",
                "component_category": "MASTER",
                "component_name": "ATLAS_SERVER",
                "display_name": "Atlas Server",
                "is_client": "false",
                "is_master": "true",
                "hostnames": []
              },
              "dependencies": []
            }
          ]
        },
      ],
      "configurations": {
        "storm-site": {
          "properties": {
            "storm.topology.submission.notifier.plugin.class": "foo"
          },
          "property-attributes":{}
        },
        "ranger-storm-plugin-properties": {
          "properties": {
            "ranger-storm-plugin-enabled": "No"
          }
        },
        "storm-env": {
          "properties": {
          "storm.atlas.hook": "false"
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
        }
      ]
    }

    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    services['ambari-server-properties'] = {'java.home': '/usr/jdk64/jdk1.7.3_23'}
    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    services["services"] = []
    services["configurations"]["storm-site"]["properties"]["storm.topology.submission.notifier.plugin.class"] = "org.apache.atlas.storm.hook.StormAtlasHook"
    self.stackAdvisor.recommendStormConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(True, "storm.topology.submission.notifier.plugin.class" in configurations["storm-site"]["property_attributes"])

  def test_recommendSqoopConfigurations(self):
    self.maxDiff = None
    configurations = {
      "sqoop-site": {
        "properties": {
          "sqoop.job.data.publish.class": "foo"
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
      'sqoop-site': {
        'properties': {
          'sqoop.job.data.publish.class': 'org.apache.atlas.sqoop.hook.SqoopHook',
        }
      },
      'sqoop-env': {
        'properties': {
          'sqoop.atlas.hook': 'true'
        }
      }
    }
    services = {
      "services": [
        {
          "href": "/api/v1/stacks/HDP/versions/2.2/services/ATLAS",
          "StackServices": {
            "service_name": "ATLAS",
            "service_version": "2.6.0.2.2",
            "stack_name": "HDP",
            "stack_version": "2.3"
          },
          "components": [
            {
              "StackServiceComponents": {
                "advertise_version": "false",
                "cardinality": "1",
                "component_category": "MASTER",
                "component_name": "ATLAS_SERVER",
                "display_name": "Atlas Server",
                "is_client": "false",
                "is_master": "true",
                "hostnames": []
              },
              "dependencies": []
            }
          ]
        },
      ],
      "configurations": {
        "sqoop-site": {
          "properties": {
            "sqoop.job.data.publish.class": "foo"
          }
        },
        "sqoop-env": {
          "properties": {
            "sqoop.atlas.hook": "false"
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
        }
      ]
    }

    self.stackAdvisor.recommendSqoopConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

    services['ambari-server-properties'] = {'java.home': '/usr/jdk64/jdk1.7.3_23'}
    self.stackAdvisor.recommendSqoopConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

  def test_validateRangerConfigurationsEnv(self):
    properties = {
      "ranger-kafka-plugin-enabled": "Yes",
      }
    recommendedDefaults = {
      "ranger-kafka-plugin-enabled": "No",
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
              "service_name" : "RANGER"
            }
          }
        ],
      "configurations": {
        "cluster-env": {
          "properties": {
            "security_enabled" : "false"
          },
          "property_attributes": {}
        }
      }
    }

    # Test with ranger plugin enabled, validation fails
    res_expected = [{'config-type': 'ranger-env', 'message': 'Ranger Kafka plugin should not be enabled in non-kerberos environment.', 'type': 'configuration', 'config-name': 'ranger-kafka-plugin-enabled', 'level': 'WARN'}]
    res = self.stackAdvisor.validateRangerConfigurationsEnv(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)

    # Test for security_enabled is true
    services['configurations']['cluster-env']['properties']['security_enabled'] = "true"
    configurations['cluster-env']['properties']['security_enabled'] = "true"
    res_expected = []
    res = self.stackAdvisor.validateRangerConfigurationsEnv(properties, recommendedDefaults, configurations, services, {})
    self.assertEquals(res, res_expected)
