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

import unittest
import StringIO
import sys

from mock.mock import MagicMock, patch

import checkWebUI

class TestMain(unittest.TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  @patch("optparse.OptionParser.parse_args")
  @patch('urllib.urlopen')
  def test_check_web_ui(self, urlopen_mock, parse_args_mock):
      
    #Positive scenario
    options = MagicMock()
    options.hosts = 'host1,host2'
    options.port = '10000' 
    get_code_mock = MagicMock()
    get_code_mock.getcode.return_value = 200
    urlopen_mock.return_value = get_code_mock
    
    parse_args_mock.return_value = (options, MagicMock)
    checkWebUI.main()
    
    #Negative scenario
    options = MagicMock()
    options.hosts = 'host1,host2'
    options.port = '10000' 
    get_code_mock = MagicMock()
    get_code_mock.getcode.return_value = 404
    urlopen_mock.return_value = get_code_mock
    
    parse_args_mock.return_value = (options, MagicMock)
    try:
      checkWebUI.main()
    except SystemExit, e:
      self.assertEqual(e.code, 1)

if __name__ == "__main__":
  unittest.main()
