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
import tempfile
import tarfile
import contextlib
from stacks.utils.RMFTestCase import *
import os

origin_exists = os.path.exists
@patch.object(os.path, "exists", new=MagicMock(
  side_effect=lambda *args: origin_exists(args[0])
  if args[0][-2:] == "j2" else True))
class TestMapreduceClient(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/client.py",
                       classname = "Client",
                       command = "configure",
                       config_file="default.json"
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop/mapred/userlogs',
      mode = 01777,
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/mapred',
      owner = 'mapred',
      recursive = True,
      mode = 0755,
      ignore_failures=True,
    )
    self.assertResourceCalled('Directory', '/hadoop/mapred1',
      owner = 'mapred',
      recursive = True,
      mode = 0755,
      ignore_failures=True,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.exclude',
      owner = 'mapred',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.include',
      owner = 'mapred',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-queue-acls.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-client.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-server.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()

  def test_configure_secured(self):

    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/client.py",
      classname = "Client",
      command = "configure",
      config_file="secured.json"
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop/mapred/userlogs',
      mode = 01777,
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/mapred',
      owner = 'mapred',
      recursive = True,
      mode = 0755,
      ignore_failures=True,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.exclude',
      owner = 'mapred',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred.include',
      owner = 'mapred',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/usr/lib/hadoop/bin/task-controller',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 06050,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-queue-acls.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-client.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-server.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()

  @patch.object(tarfile,"open", new = MagicMock())
  @patch.object(tempfile,"mkdtemp", new = MagicMock(return_value='/tmp/123'))
  @patch.object(contextlib,"closing", new = MagicMock())
  @patch("os.path.exists", new = MagicMock(return_value=True))
  def test_generate_configs_default(self):
    self.executeScript("1.3.2/services/MAPREDUCE/package/scripts/client.py",
                       classname = "Client",
                       command = "generate_configs",
                       config_file="default.json"
    )
    self.assertResourceCalled('Directory', '/tmp',
                              recursive = True,
                              )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              conf_dir = '/tmp/123',
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
                              configurations = self.getConfig()['configurations']['core-site'],
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              conf_dir = '/tmp/123',
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site'],
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('File', '/tmp/123/log4j.properties',
                              content = InlineTemplate("log4jproperties\nline2log4jproperties\nline2\nambari.jobhistory.database=jdbc:postgresql://c6401.ambari.apache.org/ambarirca\nambari.jobhistory.driver=org.postgresql.Driver\nambari.jobhistory.user=mapred\nambari.jobhistory.password=mapred\nambari.jobhistory.logger=${hadoop.root.logger}\n\nlog4j.appender.JHA=org.apache.ambari.log4j.hadoop.mapreduce.jobhistory.JobHistoryAppender\nlog4j.appender.JHA.database=jdbc:postgresql://c6401.ambari.apache.org/ambarirca\nlog4j.appender.JHA.driver=org.postgresql.Driver\nlog4j.appender.JHA.user=mapred\nlog4j.appender.JHA.password=mapred\n\nlog4j.logger.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger=DEBUG,JHA\nlog4j.additivity.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger=true\n\n"),
                              )
    self.assertResourceCalled('PropertiesFile', '/tmp/123/runtime.properties',
                              properties = UnknownConfigurationMock(),
    )
    self.assertResourceCalled('PropertiesFile', '/tmp/123/startup.properties',
                              properties = UnknownConfigurationMock(),
    )
    self.assertResourceCalled('Directory', '/tmp/123',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()
