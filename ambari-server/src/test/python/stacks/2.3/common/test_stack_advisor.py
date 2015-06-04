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
    configurations = {
        "yarn-site": {
            "properties": {
                "yarn.scheduler.minimum-allocation-mb": "256",
                "yarn.scheduler.maximum-allocation-mb": "2048",
                },
            }
    }
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
          "hbase.bucketcache.ioengine": "offheap"
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
          "hbase_regionserver_heapsize": "20480",
          "hbase_max_direct_memory_size": "94208"
        }
      },
      "yarn-site": {
        "properties": {
          "yarn.scheduler.minimum-allocation-mb": "256",
          "yarn.scheduler.maximum-allocation-mb": "2048"
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
