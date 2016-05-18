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

class TestHDP21StackAdvisor(TestCase):

  def setUp(self):
    import imp

    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    hdp206StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.0.6/services/stack_advisor.py')
    hdp21StackAdvisorPath = os.path.join(self.testDirectory, '../../../../../main/resources/stacks/HDP/2.1/services/stack_advisor.py')
    hdp21StackAdvisorClassName = 'HDP21StackAdvisor'
    with open(stackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor', fp, stackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp206StackAdvisorPath, 'rb') as fp:
      imp.load_module('stack_advisor_impl', fp, hdp206StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    with open(hdp21StackAdvisorPath, 'rb') as fp:
      stack_advisor_impl = imp.load_module('stack_advisor_impl', fp, hdp21StackAdvisorPath, ('.py', 'rb', imp.PY_SOURCE))
    clazz = getattr(stack_advisor_impl, hdp21StackAdvisorClassName)
    self.stackAdvisor = clazz()

  def test_recommendOozieConfigurations_noFalconServer(self):
    configurations = {}
    clusterData = {
      "components" : []
    }
    expected = {
      "oozie-site": {"properties":{}},
      "oozie-env": {"properties":{}}
    }

    self.stackAdvisor.recommendOozieConfigurations(configurations, clusterData, {"configurations":{}}, None)
    self.assertEquals(configurations, expected)

  def test_recommendOozieConfigurations_withFalconServer(self):
    configurations = {
      "falcon-env" : {
        "properties" : {
          "falcon_user" : "falcon"
        }
      }
    }

    services = {
      "services": [
        {
          "StackServices": {
            "service_name": "FALCON"
          }, "components": []
        },],
      "configurations": configurations
    }

    clusterData = {
      "components" : ["FALCON_SERVER"]
    }
    expected = {
      "oozie-site": {
        "properties": {
          "oozie.services.ext": "org.apache.oozie.service.JMSAccessorService," +
                                "org.apache.oozie.service.PartitionDependencyManagerService," +
                                "org.apache.oozie.service.HCatAccessorService",
          "oozie.service.ProxyUserService.proxyuser.falcon.groups" : "*",
          "oozie.service.ProxyUserService.proxyuser.falcon.hosts" : "*"
        }
      },
      "falcon-env" : {
        "properties" : {
          "falcon_user" : "falcon"
        }
      },
      "oozie-env": {
        "properties": {}
      }
    }

    self.stackAdvisor.recommendOozieConfigurations(configurations, clusterData, services, None)
    self.assertEquals(configurations, expected)

  def test_recommendHiveConfigurations_mapMemoryLessThan2048(self):
    configurations = {}
    clusterData = {
      "mapMemory": 567,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 1024
    }
    expected = {
      "hive-site": {
        "properties": {
          "hive.auto.convert.join.noconditionaltask.size": "718624085",
          "hive.tez.java.opts": "-server -Xmx1645m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps",
          "hive.tez.container.size": "2056"
        }
      },
      "hive-env": {
        "properties": {}
      }
    }

    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData, {"configurations": {}, "services": []}, None)
    self.maxDiff = None
    self.assertEquals(configurations, expected)

  def test_recommendHiveConfigurations_mapMemoryMoreThan2048(self):
    configurations = {}
    clusterData = {
      "mapMemory": 3000,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 1024
    }
    expected = {
      "hive-site": {
        "properties": {
          "hive.auto.convert.join.noconditionaltask.size": "1048576000",
          "hive.tez.java.opts": "-server -Xmx2401m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps",
          "hive.tez.container.size": "3000"
        }
      },
      "hive-env": {
        "properties": {}
      }
    }

    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData, {"configurations":{}, "services": []}, None)
    self.assertEquals(configurations, expected)

  def test_createComponentLayoutRecommendations_mastersIn10nodes(self):
    services = json.load(open(os.path.join(self.testDirectory, 'services.json')))
    hosts = json.load(open(os.path.join(self.testDirectory, 'hosts.json')))

    expected_layout = [
      [u'NAMENODE', u'GANGLIA_SERVER', u'ZOOKEEPER_SERVER', u'DRPC_SERVER', u'NIMBUS', u'STORM_REST_API', u'STORM_UI_SERVER', u'MYSQL_SERVER'],
      [u'SECONDARY_NAMENODE', u'HISTORYSERVER', u'APP_TIMELINE_SERVER', u'RESOURCEMANAGER', u'ZOOKEEPER_SERVER'],
      [u'HIVE_METASTORE', u'HIVE_SERVER', u'WEBHCAT_SERVER', u'HBASE_MASTER', u'OOZIE_SERVER', u'ZOOKEEPER_SERVER', u'FALCON_SERVER']
    ]

    masterComponents = [component['StackServiceComponents']['component_name'] for service in services["services"] for component in service["components"]
                        if self.stackAdvisor.isMasterComponent(component)]

    recommendation = self.stackAdvisor.createComponentLayoutRecommendations(services, hosts)

    groups = []
    for host_group in recommendation['blueprint']['host_groups']:
      components = [component['name'] for component in host_group['components'] if component['name'] in masterComponents]
      if len(components) > 0:
        groups.append(components)

    def sort_nested_lists(list):
      result_list = []
      for sublist in list:
        result_list.append(sorted(sublist))
      return sorted(result_list)

    self.assertEquals(sort_nested_lists(expected_layout), sort_nested_lists(groups))

  def test_recommendHiveConfigurations_jdbcUrl(self):
    services = {
      "services" : [
        {
          "StackServices" : {
            "service_name" : "HIVE",
          },
          "components" : [ {
            "StackServiceComponents" : {
              "component_name" : "HIVE_METASTORE",
              "service_name" : "HIVE",
              "hostnames" : ["example.com"]
            }
          }]
        }
      ],
      "configurations": {}
    }

    hosts = json.load(open(os.path.join(self.testDirectory, 'hosts.json')))
    clusterData = {
      "mapMemory": 3000,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 256
    }
    configurations = {
      "hive-site": {
        "properties": {
          "javax.jdo.option.ConnectionDriverName": "",
          "ambari.hive.db.schema.name": "hive_name",
          "javax.jdo.option.ConnectionURL": ""
        }
      },
      "hive-env": {
        "properties": {
          "hive_database": "New MySQL Database"
        }
      }
    }
    services['configurations'] = configurations
    hosts = {
      "items": [
        {
          "Hosts": {
            "host_name": "example.com"
          }
        }
      ]
    }

    # new mysql
    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionURL'], "jdbc:mysql://example.com/hive_name?createDatabaseIfNotExist=true")
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionDriverName'], "com.mysql.jdbc.Driver")

    # existing Mysql
    services['configurations']['hive-env']['properties']['hive_database'] = 'Existing MySQL Database'
    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionURL'], "jdbc:mysql://example.com/hive_name")
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionDriverName'], "com.mysql.jdbc.Driver")

    # existing postgres
    services['configurations']['hive-env']['properties']['hive_database'] = 'Existing PostgreSQL Database'
    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionURL'], "jdbc:postgresql://example.com:5432/hive_name")
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionDriverName'], "org.postgresql.Driver")

    # existing oracle
    services['configurations']['hive-env']['properties']['hive_database'] = 'Existing Oracle Database'
    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionURL'], "jdbc:oracle:thin:@//example.com:1521/hive_name")
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionDriverName'], "oracle.jdbc.driver.OracleDriver")

    # existing sqla
    services['configurations']['hive-env']['properties']['hive_database'] = 'Existing SQL Anywhere Database'
    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionURL'], "jdbc:sqlanywhere:host=example.com;database=hive_name")
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionDriverName'], "sap.jdbc4.sqlanywhere.IDriver")

    # existing Mysql / MariaDB
    services['configurations']['hive-env']['properties']['hive_database'] = 'Existing MySQL / MariaDB Database'
    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionURL'], "jdbc:mysql://example.com/hive_name")
    self.assertEquals(configurations['hive-site']['properties']['javax.jdo.option.ConnectionDriverName'], "com.mysql.jdbc.Driver")

  def test_recommendHiveConfigurations_containersRamIsLess(self):
    configurations = {}
    clusterData = {
      "mapMemory": 3000,
      "reduceMemory": 2056,
      "containers": 3,
      "ramPerContainer": 256
    }
    expected = {
      "hive-site": {
        "properties": {
          "hive.auto.convert.join.noconditionaltask.size": "268435456",
          "hive.tez.java.opts": "-server -Xmx615m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC -XX:+PrintGCDetails -verbose:gc -XX:+PrintGCTimeStamps",
          "hive.tez.container.size": "768"
        }
      },
      "hive-env": {
        "properties": {}
      }
    }

    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData, {"configurations":{}, "services": []}, None)
    self.assertEquals(configurations, expected)

  def test_recommendHbaseConfigurations(self):
    servicesList = ["HBASE"]
    configurations = {}
    components = []
    host_item = {
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
    hosts = {
      "items" : [host_item for i in range(1, 600)]
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
          "hbase_master_heapsize": "8192",
          "hbase_regionserver_heapsize": "8192",
          }
      }
    }

    clusterData = self.stackAdvisor.getConfigurationClusterSummary(servicesList, hosts, components, None)
    self.assertEquals(clusterData['hbaseRam'], 8)

    self.stackAdvisor.recommendHbaseConfigurations(configurations, clusterData, services, hosts)
    self.assertEquals(configurations, expected)

  def test_recommendHDFSConfigurations(self):
    configurations = {
      "hadoop-env": {
        "properties": {
          "hdfs_user": "hdfs"
        }
      }
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
      "services": [
        {
          "StackServices": {
            "service_name": "HDFS"
          }, "components": []
        }],
      "configurations": configurations,
      "ambari-server-properties": {"ambari-server.user":"ambari_user"}
    }

    clusterData = {
      "totalAvailableRam": 2048
    }
    expected = {
      'hadoop-env': {
        'properties': {
          'namenode_heapsize': '1024',
          'namenode_opt_newsize' : '256',
          'namenode_opt_maxnewsize' : '256',
          'hdfs_user' : "hdfs"
        }
      },
      "core-site": {
        "properties": {
          "hadoop.proxyuser.hdfs.hosts": "*",
          "hadoop.proxyuser.hdfs.groups": "*",
          "hadoop.proxyuser.ambari_user.hosts": "*",
          "hadoop.proxyuser.ambari_user.groups": "*"
        }
      },
      "hdfs-site": {
        "properties": {
          'dfs.datanode.data.dir': '/hadoop/hdfs/data',
          'dfs.datanode.du.reserved': '1024'
        }
      }
    }

    self.stackAdvisor.recommendHDFSConfigurations(configurations, clusterData, services, hosts)
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

  def test_validateHiveConfigurations(self):
    configurations = {'yarn-site': {'properties': {'yarn.scheduler.maximum-allocation-mb': '4096'}}}

    # 1) ok: hive.tez.container.size > recommended
    recommendedDefaults = {'hive.tez.container.size': '1024',
                           'hive.tez.java.opts': '-Xmx256m',
                           'hive.auto.convert.join.noconditionaltask.size': '1000000000'}
    properties = {'hive.tez.container.size': '2048',
                  'hive.tez.java.opts': '-Xmx300m',
                  'hive.auto.convert.join.noconditionaltask.size': '1100000000'}
    res_expected = []

    res = self.stackAdvisor.validateHiveConfigurations(properties, recommendedDefaults, configurations, '', '')
    self.assertEquals(res, res_expected)

    # 2) fail: yarn.scheduler.maximum-allocation-mb < hive.tez.container.size
    configurations = {'yarn-site': {'properties': {'yarn.scheduler.maximum-allocation-mb': '256'}}}
    res_expected = [{'config-type': 'hive-site',
                     'message': 'hive.tez.container.size is greater than the maximum container size specified in yarn.scheduler.maximum-allocation-mb',
                     'type': 'configuration',
                     'config-name': 'hive.tez.container.size',
                     'level': 'WARN'},
                    ]

    res = self.stackAdvisor.validateHiveConfigurations(properties, recommendedDefaults, configurations, '', '')
    self.assertEquals(res, res_expected)
