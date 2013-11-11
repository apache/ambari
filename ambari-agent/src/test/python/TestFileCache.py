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
    isdir_mock.return_value = True
    base = fileCache.get_service_base_dir("HDP", "2.0.7",
                                          "HBASE", "REGION_SERVER")
    self.assertEqual(base, "/var/lib/ambari-agent/cache/stacks/HDP/2.0.7/services/HBASE")


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


