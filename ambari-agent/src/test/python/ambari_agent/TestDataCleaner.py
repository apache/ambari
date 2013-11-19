#!/usr/bin/env python2.6
# -*- coding: utf-8 -*-

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
from mock.mock import patch, MagicMock, call, Mock
from ambari_agent import DataCleaner


class TestDataCleaner(unittest.TestCase):

  def setUp(self):
    self.test_dir = [('/test_path', [],
                      ['errors-12.txt','output-12.txt','site-12.pp','site-13.pp','site-15.pp','version'])]
    self.config = MagicMock()
    self.config.get.side_effect = [2592000,3600 + 1,"/test_path"]
    DataCleaner.logger = MagicMock()

  def test_init_success(self):
    config = MagicMock()
    config.get.return_value = 2592000
    DataCleaner.logger.reset_mock()
    cleaner = DataCleaner.DataCleaner(config)
    self.assertFalse(DataCleaner.logger.warn.called)


  def test_init_warn(self):
    config = MagicMock()
    config.get.return_value = 10
    DataCleaner.logger.reset_mock()
    cleaner = DataCleaner.DataCleaner(config)
    self.assertTrue(DataCleaner.logger.warn.called)
    self.assertTrue(cleaner.file_max_age == 3600)

  @patch('os.walk')
  @patch('time.time')
  @patch('os.path.getmtime')
  @patch('os.remove')
  def test_cleanup_success(self,remMock,mtimeMock,timeMock,walkMock):
    self.config.reset_mock()
    DataCleaner.logger.reset_mock()

    walkMock.return_value = iter(self.test_dir)
    timeMock.return_value = 2592000 + 2
    mtimeMock.side_effect = [1,1,1,2,1,1]

    cleaner = DataCleaner.DataCleaner(self.config)
    cleaner.cleanup()

    self.assertTrue(len(remMock.call_args_list) == 4)
    remMock.assert_any_call('/test_path/errors-12.txt');
    remMock.assert_any_call('/test_path/output-12.txt');
    remMock.assert_any_call('/test_path/site-12.pp');
    remMock.assert_any_call('/test_path/site-15.pp');
    pass

  @patch('os.walk')
  @patch('time.time')
  @patch('os.path.getmtime')
  @patch('os.remove')
  def test_cleanup_remove_error(self,remMock,mtimeMock,timeMock,walkMock):
    self.config.reset_mock()
    DataCleaner.logger.reset_mock()

    walkMock.return_value = iter(self.test_dir)
    timeMock.return_value = 2592000 + 2
    mtimeMock.side_effect = [1,1,1,2,1,1]

    def side_effect(arg):
      if arg == '/test_path/site-15.pp':
        raise Exception("Can't remove file")

    remMock.side_effect = side_effect

    cleaner = DataCleaner.DataCleaner(self.config)
    cleaner.cleanup()

    self.assertTrue(len(remMock.call_args_list) == 4)
    self.assertTrue(DataCleaner.logger.error.call_count == 1)
    pass

if __name__ == "__main__":
  suite = unittest.TestLoader().loadTestsFromTestCase(TestDataCleaner)
  unittest.TextTestRunner(verbosity=2).run(suite)
