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
import unittest
import StringIO
import socket
import os, sys
from mock.mock import patch
from mock.mock import MagicMock
from mock.mock import create_autospec
import os, errno, tempfile
from ambari_agent import StackVersionsFileHandler
import logging

stackVersionsFileHandler = \
      StackVersionsFileHandler.StackVersionsFileHandler("/tmp")
dummyVersionsFile = os.path.dirname(os.path.abspath(__file__))+ os.sep +'dummy_files' + os.sep + 'dummy_current_stack'

class TestStackVersionsFileHandler(TestCase):

  logger = logging.getLogger()

  @patch.object(stackVersionsFileHandler, 'touch_file')
  def test_read_stack_version(self, touch_method):
    stackVersionsFileHandler.versionsFilePath = dummyVersionsFile
    result = stackVersionsFileHandler.read_stack_version("GANGLIA_SERVER")
    self.assertEquals(result, '{"stackName":"HDP","stackVersion":"1.2.2"}')
    result = stackVersionsFileHandler.read_stack_version("NOTEXISTING")
    self.assertEquals(result, stackVersionsFileHandler.DEFAULT_VER)
    self.assertTrue(touch_method.called)


  @patch.object(stackVersionsFileHandler, 'touch_file')
  def test_read_all_stack_versions(self, touch_method):
    stackVersionsFileHandler.versionsFilePath = dummyVersionsFile
    result = stackVersionsFileHandler.read_all_stack_versions()
    self.assertEquals(len(result.keys()), 3)
    self.assertEquals(result["HCATALOG"],
          '{"stackName":"HDP","stackVersion":"1.2.2"}')
    self.assertTrue(touch_method.called)


  def test_extract(self):
    s = '   GANGLIA_SERVER	\t  {"stackName":"HDP","stackVersion":"1.3.0"}  '
    comp, ver = stackVersionsFileHandler.extract(s)
    self.assertEqual(comp, "GANGLIA_SERVER")
    self.assertEqual(ver, '{"stackName":"HDP","stackVersion":"1.3.0"}')
    # testing wrong value
    s = "   GANGLIA_SERVER	"
    comp, ver = stackVersionsFileHandler.extract(s)
    self.assertEqual(comp, stackVersionsFileHandler.DEFAULT_VER)
    self.assertEqual(ver, stackVersionsFileHandler.DEFAULT_VER)


  def test_touch_file(self):
    tmpfile = tempfile.mktemp()
    stackVersionsFileHandler.versionsFilePath = tmpfile
    stackVersionsFileHandler.touch_file()
    result = os.path.isfile(tmpfile)
    self.assertEqual(result, True)


  def test_write_stack_version(self):
    #saving old values
    oldFilePathValue = stackVersionsFileHandler.versionsFilePath
    oldversionsFileDir = stackVersionsFileHandler.versionsFileDir
    oldVerFile = stackVersionsFileHandler.VER_FILE
    #preparations and invocation
    tmpfile = tempfile.mktemp()
    stackVersionsFileHandler.versionsFilePath = tmpfile
    stackVersionsFileHandler.VER_FILE = \
      os.path.basename(tmpfile)
    stackVersionsFileHandler.versionsFileDir = \
      os.path.dirname(tmpfile)
    stackVersionsFileHandler.touch_file()
    stackVersionsFileHandler.write_stack_version(
      "GANGLIA_SERVER", '"stackVersion":"1.3.0"')
    # Checking if backup file exists
    expectedBackupFile = tmpfile + ".bak"
    self.assertTrue(os.path.isfile(expectedBackupFile))
    os.remove(expectedBackupFile)
    # Checking content of created file
    content = stackVersionsFileHandler.read_all_stack_versions()
    self.assertEquals(len(content), 1)
    self.assertEqual(content['GANGLIA_SERVER'], '"stackVersion":"1.3.0"')
    self.assertTrue(os.path.isfile(tmpfile))
    os.remove(tmpfile)
    # Restoring old values
    stackVersionsFileHandler.versionsFilePath = oldFilePathValue
    stackVersionsFileHandler.versionsFileDir = oldversionsFileDir
    stackVersionsFileHandler.VER_FILE = oldVerFile

if __name__ == "__main__":
  unittest.main(verbosity=2)
