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

from ambari_agent import NetUtil
from mock.mock import MagicMock, patch
import unittest
import threading
from ambari_commons import OSCheck
from only_for_platform import only_for_platform, get_platform, PLATFORM_LINUX, PLATFORM_WINDOWS

if get_platform() != PLATFORM_WINDOWS:
  os_distro_value = ('Suse','11','Final')
else:
  os_distro_value = ('win2012serverr2','6.3','WindowsServer')

class TestNetUtil(unittest.TestCase):

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("urlparse.urlparse")
  @patch("httplib.HTTPSConnection")
  def test_checkURL(self, httpsConMock, parseMock):

    NetUtil.logger = MagicMock()
    parseMock.return_value = [1, 2]
    ca_connection = MagicMock()
    response = MagicMock()
    response.status = 200
    ca_connection.getresponse.return_value = response
    httpsConMock.return_value = ca_connection

    # test 200
    netutil = NetUtil.NetUtil()
    self.assertTrue(netutil.checkURL("url")[0])

    # test fail
    response.status = 404
    self.assertFalse(netutil.checkURL("url")[0])

    # test Exception
    response.status = 200
    httpsConMock.side_effect = Exception("test")
    self.assertFalse(netutil.checkURL("url")[0])

  @only_for_platform(PLATFORM_LINUX)
  @patch("time.sleep")
  @patch.object(threading._Event, "wait")
  def test_try_to_connect(self, event_mock,
                            sleepMock):
    event_mock.return_value = False
    netutil = NetUtil.NetUtil()
    checkURL = MagicMock(name="checkURL")
    checkURL.return_value = True, "test"
    netutil.checkURL = checkURL

    # one successful get
    self.assertEqual((0, True), netutil.try_to_connect("url", 10))

    # got successful after N retries
    gets = [[True, ""], [False, ""], [False, ""]]

    def side_effect(*args):
      return gets.pop()
    checkURL.side_effect = side_effect
    self.assertEqual((2, True), netutil.try_to_connect("url", 10))

    # max retries
    checkURL.side_effect = None
    checkURL.return_value = False, "test"
    self.assertEqual((5,False), netutil.try_to_connect("url", 5))
