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


import StringIO
import sys, logging, pprint
from ambari_agent import AgentException
from resource_management.libraries.script import Script
from resource_management.core.environment import Environment
from mock.mock import MagicMock, patch

class TestScript(TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out



  @patch("resource_management.core.providers.package.PackageProvider")
  def test_install_packages(self, package_provider_mock):
    no_such_entry_config = {
    }
    empty_config = {
      'hostLevelParams' : {
        'package_list' : ''
      }
    }
    dummy_config = {
      'hostLevelParams' : {
        'package_list' : "[{\"type\":\"rpm\",\"name\":\"hbase\"},"
                         "{\"type\":\"rpm\",\"name\":\"yet-another-package\"}]"
      }
    }

    # Testing config without any keys
    with Environment(".") as env:
      script = Script()
      Script.config = no_such_entry_config
      script.install_packages(env)
    self.assertEquals(len(env.resource_list), 0)

    # Testing empty package list
    with Environment(".") as env:
      script = Script()
      Script.config = empty_config
      script.install_packages(env)
    self.assertEquals(len(env.resource_list), 0)

    # Testing installing of a list of packages
    with Environment(".") as env:
      Script.config = dummy_config
      script.install_packages("env")
    resource_dump = pprint.pformat(env.resource_list)
    self.assertEqual(resource_dump, "[Package['hbase'], Package['yet-another-package']]")


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


