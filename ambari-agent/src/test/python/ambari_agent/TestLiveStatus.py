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
from mock.mock import patch
import pprint
from ambari_agent import StatusCheck


class TestLiveStatus(TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  @patch.object(ActualConfigHandler.ActualConfigHandler, "read_actual_component")
  def test_build(self, read_actual_component_mock):
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
    # Test build status with forsed_component_status
    ## Alive
    livestatus = LiveStatus('c1', 'HDFS', 'HDFS_CLIENT', { }, config, {})
    result = livestatus.build(forsed_component_status = LiveStatus.LIVE_STATUS)
    self.assertTrue(len(result) > 0, 'Livestatus should not be empty')
    self.assertTrue(result['status'], LiveStatus.LIVE_STATUS)
    ## Dead
    livestatus = LiveStatus('c1', 'HDFS', 'HDFS_CLIENT', { }, config, {})
    result = livestatus.build(forsed_component_status = LiveStatus.DEAD_STATUS)
    self.assertTrue(len(result) > 0, 'Livestatus should not be empty')
    self.assertTrue(result['status'], LiveStatus.DEAD_STATUS)

    livestatus = LiveStatus('c1', 'TEZ', 'TEZ_CLIENT', { }, config, {})
    result = livestatus.build(forsed_component_status = LiveStatus.LIVE_STATUS)
    self.assertTrue(len(result) > 0, 'Livestatus should not be empty')
    self.assertTrue(result['status'], LiveStatus.LIVE_STATUS)

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
    result = livestatus.build(forsed_component_status = "STARTED")
    result_str = pprint.pformat(result)
    self.assertEqual(result_str,
                     "{'clusterName': '',\n "
                     "'componentName': 'SOME_UNKNOWN_COMPONENT',\n "
                     "'configurationTags': 'actual_component',\n "
                     "'msg': '',\n 'serviceName': 'SOME_UNKNOWN_SERVICE',\n "
                     "'stackVersion': '',\n 'status': 'STARTED'}")
    self.assertFalse(getStatus_mock.called)


