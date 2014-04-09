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

from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
import resource_management.core.source

@patch.object(resource_management.core.source, "InlineTemplate", new = MagicMock(return_value='InlineTemplateMock'))
class TestFlumeHandler(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.0.6/services/FLUME/package/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript("2.0.6/services/FLUME/package/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "start",
                       config_file="default.json"
    )

    self.assert_configure_default()

    self.assertResourceCalled('Execute', format('env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/bin/flume-ng agent '
      '--name a1 '
      '--conf /etc/flume/conf/a1 '
      '--conf-file /etc/flume/conf/a1/flume.conf '
      '-Dflume.monitoring.type=ganglia '
      '-Dflume.monitoring.hosts=c6401.ambari.apache.org:8655'),
      wait_for_finish = False)

    self.assertResourceCalled('Execute', 'pgrep -f /etc/flume/conf/a1/flume.conf > /var/run/flume/a1.pid',
      logoutput = True,
      tries = 5,
      try_sleep = 10)

    self.assertNoMoreResources()

  @patch("glob.glob")
  def test_stop_default(self, glob_mock):
    glob_mock.return_value = ['/var/run/flume/a1.pid']

    self.executeScript("2.0.6/services/FLUME/package/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "stop",
                       config_file="default.json"
    )

    self.assertTrue(glob_mock.called)

    self.assertResourceCalled('Execute', 'kill `cat /var/run/flume/a1.pid` > /dev/null 2>&1',
      ignore_failures = True)

    self.assertResourceCalled('File', '/var/run/flume/a1.pid', action = ['delete'])

    self.assertNoMoreResources()

  def assert_configure_default(self):

    self.assertResourceCalled('Directory', '/etc/flume/conf')

    self.assertResourceCalled('Directory', '/var/log/flume', owner = 'flume')

    self.assertResourceCalled('Directory', '/etc/flume/conf/a1')

    self.assertResourceCalled('PropertiesFile', '/etc/flume/conf/a1/flume.conf',
      mode = 0644,
      properties = buildFlumeTopology(
        self.getConfig()['configurations']['flume-conf']['content'])['a1'])

    self.assertResourceCalled('File',
      '/etc/flume/conf/a1/log4j.properties',
      content = Template('log4j.properties.j2', agent_name = 'a1'),
      mode = 0644)


def buildFlumeTopology(content):
  import os
  import ConfigParser
  import StringIO

  config = StringIO.StringIO()
  config.write('[dummy]\n')
  config.write(content)
  config.seek(0, os.SEEK_SET)

  cp = ConfigParser.ConfigParser()
  cp.readfp(config)

  result = {}
  agent_names = []

  for item in cp.items('dummy'):
    key = item[0]
    part0 = key.split('.')[0]
    if key.endswith(".sources"):
      agent_names.append(part0)

    if not result.has_key(part0):
      result[part0] = {}

    result[part0][key] = item[1]

  # trim out non-agents
  for k in result.keys():
    if not k in agent_names:
      del result[k]

  return result
