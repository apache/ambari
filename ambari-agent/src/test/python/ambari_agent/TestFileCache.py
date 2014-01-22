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
import ConfigParser
import os

import pprint

from unittest import TestCase
import threading
import tempfile
import time
from threading import Thread

from PythonExecutor import PythonExecutor
from CustomServiceOrchestrator import CustomServiceOrchestrator
from FileCache import FileCache
from AmbariConfig import AmbariConfig
from mock.mock import MagicMock, patch
import StringIO
import sys
from ambari_agent import AgentException
from AgentException import AgentException


class TestFileCache(TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out
    # generate sample config
    tmpdir = tempfile.gettempdir()
    self.config = ConfigParser.RawConfigParser()
    self.config.add_section('agent')
    self.config.set('agent', 'prefix', tmpdir)
    self.config.set('agent', 'cache_dir', "/var/lib/ambari-agent/cache")


  @patch("os.path.isdir")
  def test_get_service_base_dir(self, isdir_mock):
    fileCache = FileCache(self.config)
    # Check existing dir case
    isdir_mock.return_value = True
    service_subpath = "HDP/2.1.1/services/ZOOKEEPER/package"
    base = fileCache.get_service_base_dir(service_subpath)
    self.assertEqual(base, "/var/lib/ambari-agent/cache/stacks/HDP/2.1.1/"
                           "services/ZOOKEEPER/package")
    # Check absent dir case
    isdir_mock.return_value = False
    try:
      fileCache.get_service_base_dir(service_subpath)
      self.fail("Should throw an exception")
    except AgentException:
      pass # Expected




  @patch("os.path.isdir")
  def test_get_hook_base_dir(self, isdir_mock):
    fileCache = FileCache(self.config)
    # Check missing parameter
    command = {
      'commandParams' : {
      }
    }
    base = fileCache.get_hook_base_dir(command)
    self.assertEqual(base, None)

    # Check existing dir case
    isdir_mock.return_value = True
    command = {
      'commandParams' : {
        'hooks_folder' : 'HDP/2.1.1/hooks'
      }
    }
    base = fileCache.get_hook_base_dir(command)
    self.assertEqual(base, "/var/lib/ambari-agent/cache/stacks/HDP/2.1.1/hooks")

    # Check absent dir case
    isdir_mock.return_value = False
    try:
      fileCache.get_hook_base_dir(command)
      self.fail("Should throw an exception")
    except AgentException:
      pass # Expected


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


