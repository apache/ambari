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
import StringIO
import sys, pprint
from resource_management.libraries.script import Script
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger
from mock.mock import patch, MagicMock
from stacks.utils.RMFTestCase import *

class TestScript(RMFTestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out

  @patch("__builtin__.open")
  def test_structured_out(self, open_mock):
    script = Script()
    script.stroutfile = ''
    self.assertEqual(Script.structuredOut, {})

    script.put_structured_out({"1": "1"})
    self.assertEqual(Script.structuredOut, {"1": "1"})
    self.assertTrue(open_mock.called)

    script.put_structured_out({"2": "2"})
    self.assertEqual(open_mock.call_count, 2)
    self.assertEqual(Script.structuredOut, {"1": "1", "2": "2"})

    #Overriding
    script.put_structured_out({"1": "3"})
    self.assertEqual(open_mock.call_count, 3)
    self.assertEqual(Script.structuredOut, {"1": "3", "2": "2"})

  @patch("__builtin__.open")
  def test_status_commands_clear_structured_out(self, open_mock):
    """
    Tests that status commands will clear any stored structured output from prior status commands.
    :param open_mock: 
    :return: 
    """
    class MagicFile(object):
      def read(self):
        return "{}"

      def write(self, data):
        pass

      def __exit__(self, exc_type, exc_val, exc_tb):
        pass

      def __enter__(self):
        return self

    sys.argv = ["", "status", "foo.py", "", "", "INFO", ""]
    open_mock.side_effect = [MagicFile()]

    try:
      with Environment(".", test_mode=True) as env:
        script = Script()
        Script.structuredOut = { "version" : "old_version" }
        script.execute()
    except:
      pass

    self.assertTrue(open_mock.called)
    self.assertEquals({}, Script.structuredOut)


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__
