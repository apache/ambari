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

import socket
from unittest import TestCase

class TestHDP21StackAdvisor(TestCase):

  def setUp(self):
    import imp
    import os

    testDirectory = os.path.dirname(os.path.abspath(__file__))
    stackAdvisorPath = os.path.join(testDirectory, '../../../../../main/resources/stacks/stack_advisor.py')
    hdp206StackAdvisorPath = os.path.join(testDirectory, '../../../../../main/resources/stacks/HDP/2.0.6/services/stack_advisor.py')
    hdp21StackAdvisorPath = os.path.join(testDirectory, '../../../../../main/resources/stacks/HDP/2.1/services/stack_advisor.py')
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
    }

    self.stackAdvisor.recommendOozieConfigurations(configurations, clusterData)
    self.assertEquals(configurations, expected)

  def test_recommendOozieConfigurations_withFalconServer(self):
    configurations = {}
    clusterData = {
      "components" : ["FALCON_SERVER"]
    }
    expected = {
      "oozie-site": {
        "properties": {
          "oozie.services.ext": "org.apache.oozie.service.JMSAccessorService," +
                                "org.apache.oozie.service.PartitionDependencyManagerService," +
                                "org.apache.oozie.service.HCatAccessorService"
        }
      }
    }

    self.stackAdvisor.recommendOozieConfigurations(configurations, clusterData)
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
          "hive.auto.convert.join.noconditionaltask.size": "718274560",
          "hive.tez.java.opts": "-server -Xmx1645m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC",
          "hive.tez.container.size": "2056"
        }
      }
    }

    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData)
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
          "hive.tez.java.opts": "-server -Xmx2400m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC",
          "hive.tez.container.size": "3000"
        }
      }
    }

    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData)
    self.assertEquals(configurations, expected)

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
          "hive.tez.java.opts": "-server -Xmx614m -Djava.net.preferIPv4Stack=true -XX:NewRatio=8 -XX:+UseNUMA -XX:+UseParallelGC",
          "hive.tez.container.size": "768"
        }
      }
    }

    self.stackAdvisor.recommendHiveConfigurations(configurations, clusterData)
    self.assertEquals(configurations, expected)
