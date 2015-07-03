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
          "hbase.bucketcache.percentage.in.combinedcache": "0.9184",
          "hbase.regionserver.global.memstore.size": "0.4",
          "hfile.block.cache.size": "0.4",
          "hbase.coprocessor.region.classes": "org.apache.hadoop.hbase.security.access.SecureBulkLoadEndpoint",
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
          "hbase_master_heapsize": "114688",
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
    expected["hbase-env"]["properties"]["hbase_master_heapsize"] = "4096"
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

