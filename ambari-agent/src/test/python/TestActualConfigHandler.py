#!/usr/bin/env python2.6

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
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.ActualConfigHandler import ActualConfigHandler
import os
import logging
import json

class TestActualConfigHandler(TestCase):

  logger = logging.getLogger()

  def test_read_write(self):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)
    handler = ActualConfigHandler(config)
    
    tags = { "global": "version1", "core-site": "version2" }
    handler.write_actual(tags)
    output = handler.read_actual()
    self.assertEquals(tags, output)
    os.remove(os.path.join(tmpdir, ActualConfigHandler.CONFIG_NAME))

  def test_read_empty(self):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set('agent', 'prefix', tmpdir)
    handler = ActualConfigHandler(config)

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
    handler = ActualConfigHandler(config)

    tags1 = { "global": "version1", "core-site": "version2" }
    handler.write_actual(tags1)
    handler.copy_to_component('FOO')

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
