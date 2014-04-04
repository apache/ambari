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
import tempfile
from unittest import TestCase
import os
import logging
from mock.mock import patch

with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  from ambari_agent.AmbariConfig import AmbariConfig
  from ambari_agent.ActualConfigHandler import ActualConfigHandler


class TestActualConfigHandler(TestCase):

  logger = logging.getLogger()

  def test_read_write(self):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags = { "global": "version1", "core-site": "version2" }
    handler = ActualConfigHandler(config, tags)
    handler.write_actual(tags)
    output = handler.read_actual()
    self.assertEquals(tags, output)
    os.remove(os.path.join(tmpdir, ActualConfigHandler.CONFIG_NAME))

  def test_read_empty(self):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)
    handler = ActualConfigHandler(config, {})

    conf_file = open(os.path.join(tmpdir, ActualConfigHandler.CONFIG_NAME), 'w')
    conf_file.write("")
    conf_file.close()
    
    output = handler.read_actual()
    self.assertEquals(None, output)
    os.remove(os.path.join(tmpdir, ActualConfigHandler.CONFIG_NAME))

  def test_read_write_component(self):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags1 = { "global": "version1", "core-site": "version2" }
    handler = ActualConfigHandler(config, {})
    handler.write_actual(tags1)
    handler.write_actual_component('FOO', tags1)

    output1 = handler.read_actual_component('FOO')
    output2 = handler.read_actual_component('GOO')

    self.assertEquals(tags1, output1)
    self.assertEquals(None, output2)
    
    tags2 = { "global": "version1", "core-site": "version2" }
    handler.write_actual(tags2)

    output3 = handler.read_actual()
    output4 = handler.read_actual_component('FOO')
    self.assertEquals(tags2, output3)
    self.assertEquals(tags1, output4)
    os.remove(os.path.join(tmpdir, "FOO_" + ActualConfigHandler.CONFIG_NAME))
    os.remove(os.path.join(tmpdir, ActualConfigHandler.CONFIG_NAME))

  def test_write_actual_component_and_client_components(self):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags1 = { "global": "version1", "core-site": "version2" }
    tags2 = { "global": "version33", "core-site": "version33" }
    handler = ActualConfigHandler(config, {})
    handler.write_actual_component('HDFS_CLIENT', tags1)
    handler.write_actual_component('HBASE_CLIENT', tags1)
    self.assertEquals(tags1, handler.read_actual_component('HDFS_CLIENT'))
    self.assertEquals(tags1, handler.read_actual_component('HBASE_CLIENT'))
    handler.write_actual_component('DATANODE', tags2)
    self.assertEquals(tags2, handler.read_actual_component('DATANODE'))
    self.assertEquals(tags1, handler.read_actual_component('HDFS_CLIENT'))
    handler.write_client_components('HDFS', tags2)
    self.assertEquals(tags2, handler.read_actual_component('HDFS_CLIENT'))
    self.assertEquals(tags1, handler.read_actual_component('HBASE_CLIENT'))

    os.remove(os.path.join(tmpdir, "DATANODE_" + ActualConfigHandler.CONFIG_NAME))
    os.remove(os.path.join(tmpdir, "HBASE_CLIENT_" + ActualConfigHandler.CONFIG_NAME))
    os.remove(os.path.join(tmpdir, "HDFS_CLIENT_" + ActualConfigHandler.CONFIG_NAME))

  @patch.object(ActualConfigHandler, "write_file")
  def test_write_client_components(self, write_file_mock):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags0 = {"global": "version0", "core-site": "version0"}
    tags1 = {"global": "version1", "core-site": "version2"}
    tags2 = {"global": "version33", "core-site": "version33"}
    configTags = {'HDFS_CLIENT': tags0, 'HBASE_CLIENT': tags1}
    handler = ActualConfigHandler(config, configTags)
    self.assertEquals(tags0, handler.read_actual_component('HDFS_CLIENT'))
    self.assertEquals(tags1, handler.read_actual_component('HBASE_CLIENT'))
    handler.write_client_components('HDFS', tags2)
    self.assertEquals(tags2, handler.read_actual_component('HDFS_CLIENT'))
    self.assertEquals(tags1, handler.read_actual_component('HBASE_CLIENT'))
    self.assertTrue(write_file_mock.called)
    self.assertEqual(1, write_file_mock.call_count)

  @patch.object(ActualConfigHandler, "write_file")
  @patch.object(ActualConfigHandler, "read_file")
  def test_read_actual_component_inmemory(self, read_file_mock, write_file_mock):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)

    tags1 = { "global": "version1", "core-site": "version2" }
    read_file_mock.return_value = tags1

    handler = ActualConfigHandler(config, {})

    handler.write_actual_component('NAMENODE', tags1)
    self.assertTrue(write_file_mock.called)
    self.assertEquals(tags1, handler.read_actual_component('NAMENODE'))
    self.assertFalse(read_file_mock.called)
    self.assertEquals(tags1, handler.read_actual_component('DATANODE'))
    self.assertTrue(read_file_mock.called)
    self.assertEquals(1, read_file_mock.call_count)
    self.assertEquals(tags1, handler.read_actual_component('DATANODE'))
    self.assertEquals(1, read_file_mock.call_count)
