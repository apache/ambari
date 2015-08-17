#!/usr/bin/env python

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

from unittest import TestCase
from ambari_agent.LiveStatus import LiveStatus
from ambari_agent.AmbariConfig import AmbariConfig
import os, sys, StringIO
from ambari_agent import ActualConfigHandler
from mock.mock import patch, MagicMock
import pprint
from ambari_agent import StatusCheck
from ambari_commons import OSCheck
from only_for_platform import only_for_platform, get_platform, PLATFORM_LINUX, PLATFORM_WINDOWS

if get_platform() != PLATFORM_WINDOWS:
  os_distro_value = ('Suse','11','Final')
else:
  os_distro_value = ('win2012serverr2','6.3','WindowsServer')


class TestLiveStatus(TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out
    LiveStatus.SERVICES = [
      "HDFS", "MAPREDUCE", "GANGLIA", "HBASE",
      "ZOOKEEPER", "OOZIE",
      "KERBEROS", "TEMPLETON", "HIVE",
      "YARN", "MAPREDUCE2", "FLUME", "TEZ",
      "FALCON", "STORM"
    ]
    LiveStatus.CLIENT_COMPONENTS = [
      {"serviceName" : "HBASE",
       "componentName" : "HBASE_CLIENT"},
      {"serviceName" : "HDFS",
       "componentName" : "HDFS_CLIENT"},
      {"serviceName" : "MAPREDUCE",
       "componentName" : "MAPREDUCE_CLIENT"},
      {"serviceName" : "ZOOKEEPER",
       "componentName" : "ZOOKEEPER_CLIENT"},
      {"serviceName" : "OOZIE",
       "componentName" : "OOZIE_CLIENT"},
      {"serviceName" : "HCATALOG",
       "componentName" : "HCAT"},
      {"serviceName" : "HIVE",
       "componentName" : "HIVE_CLIENT"},
      {"serviceName" : "YARN",
       "componentName" : "YARN_CLIENT"},
      {"serviceName" : "MAPREDUCE2",
       "componentName" : "MAPREDUCE2_CLIENT"},
      {"serviceName" : "PIG",
       "componentName" : "PIG"},
      {"serviceName" : "SQOOP",
       "componentName" : "SQOOP"},
      {"serviceName" : "TEZ",
       "componentName" : "TEZ_CLIENT"},
      {"serviceName" : "FALCON",
       "componentName" : "FALCON_CLIENT"}
    ]
    LiveStatus.COMPONENTS = [
      {"serviceName" : "HDFS",
       "componentName" : "DATANODE"},
      {"serviceName" : "HDFS",
       "componentName" : "NAMENODE"},
      {"serviceName" : "HDFS",
       "componentName" : "SECONDARY_NAMENODE"},
      {"serviceName" : "HDFS",
       "componentName" : "JOURNALNODE"},
      {"serviceName" : "HDFS",
       "componentName" : "ZKFC"},
      {"serviceName" : "MAPREDUCE",
       "componentName" : "JOBTRACKER"},
      {"serviceName" : "MAPREDUCE",
       "componentName" : "TASKTRACKER"},
      {"serviceName" : "GANGLIA",
       "componentName" : "GANGLIA_SERVER"},
      {"serviceName" : "GANGLIA",
       "componentName" : "GANGLIA_MONITOR"},
      {"serviceName" : "HBASE",
       "componentName" : "HBASE_MASTER"},
      {"serviceName" : "HBASE",
       "componentName" : "HBASE_REGIONSERVER"},
      {"serviceName" : "FLUME",
       "componentName" : "FLUME_SERVER"},
      {"serviceName" : "ZOOKEEPER",
       "componentName" : "ZOOKEEPER_SERVER"},
      {"serviceName" : "OOZIE",
       "componentName" : "OOZIE_SERVER"},
      {"serviceName" : "HCATALOG",
       "componentName" : "HCATALOG_SERVER"},
      {"serviceName" : "KERBEROS",
       "componentName" : "KERBEROS_SERVER"},
      {"serviceName" : "HIVE",
       "componentName" : "HIVE_SERVER"},
      {"serviceName" : "HIVE",
       "componentName" : "HIVE_METASTORE"},
      {"serviceName" : "HIVE",
       "componentName" : "MYSQL_SERVER"},
      {"serviceName" : "WEBHCAT",
       "componentName" : "WEBHCAT_SERVER"},
      {"serviceName" : "YARN",
       "componentName" : "RESOURCEMANAGER"},
      {"serviceName" : "YARN",
       "componentName" : "NODEMANAGER"},
      {"serviceName" : "YARN",
       "componentName" : "APP_TIMELINE_SERVER"},
      {"serviceName" : "MAPREDUCE2",
       "componentName" : "HISTORYSERVER"},
      {"serviceName" : "FALCON",
       "componentName" : "FALCON_SERVER"},
      {"serviceName" : "STORM",
       "componentName" : "NIMBUS"},
      {"serviceName" : "STORM",
       "componentName" : "STORM_REST_API"},
      {"serviceName" : "STORM",
       "componentName" : "SUPERVISOR"},
      {"serviceName" : "STORM",
       "componentName" : "STORM_UI_SERVER"},
      {"serviceName" : "STORM",
       "componentName" : "DRPC_SERVER"}
    ]


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  @patch("os.path.isdir")
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActualConfigHandler.ActualConfigHandler, "read_actual_component")
  def test_build(self, read_actual_component_mock, isdir_mock):
    isdir_mock.return_value = False
    for component in LiveStatus.COMPONENTS:
      config = AmbariConfig().getConfig()
      config.set('agent', 'prefix', "ambari_agent" + os.sep + "dummy_files")
      livestatus = LiveStatus('', component['serviceName'], component['componentName'], {}, config, {})
      livestatus.versionsHandler.versionsFilePath = "ambari_agent" + os.sep + "dummy_files" + os.sep + "dummy_current_stack"
      result = livestatus.build()
      print "LiveStatus of {0}: {1}".format(component['serviceName'], str(result))
      self.assertEquals(len(result) > 0, True, 'Livestatus should not be empty')
      if component['componentName'] == 'GANGLIA_SERVER':
        self.assertEquals(result['stackVersion'],'{"stackName":"HDP","stackVersion":"1.2.2"}',
                      'Livestatus should contain component stack version')

    # Test build status for CLIENT component (in LiveStatus.CLIENT_COMPONENTS)
    read_actual_component_mock.return_value = "some tags"
    livestatus = LiveStatus('c1', 'HDFS', 'HDFS_CLIENT', { }, config, {})
    result = livestatus.build()
    self.assertTrue(len(result) > 0, 'Livestatus should not be empty')
    self.assertTrue(result.has_key('configurationTags'))
    # Test build status with forced_component_status
    ## Alive
    livestatus = LiveStatus('c1', 'HDFS', 'HDFS_CLIENT', { }, config, {})
    result = livestatus.build(forced_component_status = LiveStatus.LIVE_STATUS)
    self.assertTrue(len(result) > 0, 'Livestatus should not be empty')
    self.assertTrue(result['status'], LiveStatus.LIVE_STATUS)
    ## Dead
    livestatus = LiveStatus('c1', 'HDFS', 'HDFS_CLIENT', { }, config, {})
    result = livestatus.build(forced_component_status = LiveStatus.DEAD_STATUS)
    self.assertTrue(len(result) > 0, 'Livestatus should not be empty')
    self.assertTrue(result['status'], LiveStatus.DEAD_STATUS)

    livestatus = LiveStatus('c1', 'TEZ', 'TEZ_CLIENT', { }, config, {})
    result = livestatus.build(forced_component_status = LiveStatus.LIVE_STATUS)
    self.assertTrue(len(result) > 0, 'Livestatus should not be empty')
    self.assertTrue(result['status'], LiveStatus.LIVE_STATUS)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(ActualConfigHandler.ActualConfigHandler, "read_actual_component")
  @patch.object(StatusCheck.StatusCheck, "getStatus")
  def test_build_predefined(self, getStatus_mock, read_actual_component_mock):
    read_actual_component_mock.return_value = "actual_component"
    """
    Tests that if live status us defined (using default parameter),
    then no StatusCheck is executed
    """
    config = AmbariConfig().getConfig()
    config.set('agent', 'prefix', "ambari_agent" + os.sep + "dummy_files")
    livestatus = LiveStatus('', 'SOME_UNKNOWN_SERVICE',
                            'SOME_UNKNOWN_COMPONENT', {}, config, {})
    livestatus.versionsHandler.versionsFilePath = "ambari_agent" + \
                      os.sep + "dummy_files" + os.sep + "dummy_current_stack"
    result = livestatus.build(forced_component_status = "STARTED")
    result_str = pprint.pformat(result)
    self.assertEqual(result_str,
                     "{'clusterName': '',\n "
                     "'componentName': 'SOME_UNKNOWN_COMPONENT',\n "
                     "'configurationTags': 'actual_component',\n "
                     "'msg': '',\n 'serviceName': 'SOME_UNKNOWN_SERVICE',\n "
                     "'stackVersion': '',\n 'status': 'STARTED'}")
    self.assertFalse(getStatus_mock.called)


