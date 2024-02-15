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

import json
import logging
import time

from unittest import TestCase
from only_for_platform import get_platform, PLATFORM_WINDOWS
from mock.mock import patch, MagicMock
from security import CachedHTTPConnection
from blacklisted_set import BlacklistedSet
from spnego_kerberos_auth import SPNEGOKerberosAuth

if get_platform() != PLATFORM_WINDOWS:
  os_distro_value = ('Suse','11','Final')
else:
  os_distro_value = ('win2012serverr2','6.3','WindowsServer')

with patch("platform.linux_distribution", return_value = os_distro_value):
  from ambari_commons import OSCheck
  from application_metric_map import ApplicationMetricMap
  from config_reader import Configuration
  from emitter import Emitter
  from stop_handler import bind_signal_handlers

logger = logging.getLogger()

class TestEmitter(TestCase):

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(CachedHTTPConnection, "create_connection", new = MagicMock())
  @patch.object(CachedHTTPConnection, "request")
  @patch.object(CachedHTTPConnection, "getresponse")
  def test_submit_metrics(self, getresponse_mock, request_mock):
    request_mock.return_value = MagicMock()
    getresponse_mock.return_value = MagicMock()
    getresponse_mock.return_value.status = 200

    stop_handler = bind_signal_handlers()

    config = Configuration()
    application_metric_map = ApplicationMetricMap("host","10.10.10.10")
    application_metric_map.clear()
    application_metric_map.put_metric("APP1", {"metric1":1}, 1)
    emitter = Emitter(config, application_metric_map, stop_handler)
    emitter.submit_metrics()

    self.assertEqual(request_mock.call_count, 1)
    self.assertUrlData(request_mock)


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(CachedHTTPConnection, "create_connection", new = MagicMock())
  @patch.object(CachedHTTPConnection, "getresponse", new = MagicMock())
  @patch.object(CachedHTTPConnection, "request")
  def testRetryFetchAndRoundRobin(self, request_mock):
    stop_handler = bind_signal_handlers()

    request_mock.return_value = MagicMock()

    config = Configuration()
    application_metric_map = ApplicationMetricMap("host","10.10.10.10")
    application_metric_map.clear()
    application_metric_map.put_metric("APP1", {"metric1":1}, 1)
    emitter = Emitter(config, application_metric_map, stop_handler)
    emitter.RETRY_SLEEP_INTERVAL = .001
    emitter.submit_metrics()

    self.assertEqual(request_mock.call_count, 3)
    self.assertUrlData(request_mock)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(CachedHTTPConnection, "create_connection", new = MagicMock())
  @patch.object(SPNEGOKerberosAuth, "authenticate_handshake")
  @patch.object(CachedHTTPConnection, "getresponse")
  @patch.object(CachedHTTPConnection, "request")
  def test_spnego_negotiation(self, request_mock, getresponse_mock, auth_mock):
    request_mock.return_value = MagicMock()
    getresponse_mock.return_value.status = 401
    getresponse_mock.return_value.getheader.return_value = "Negotiate   "

    auth_mock.return_value.status = 200

    stop_handler = bind_signal_handlers()

    config = Configuration()
    application_metric_map = ApplicationMetricMap("host","10.10.10.10")
    application_metric_map.clear()
    application_metric_map.put_metric("APP1", {"metric1":1}, 1)
    emitter = Emitter(config, application_metric_map, stop_handler)
    emitter.submit_metrics()


    self.assertEqual(request_mock.call_count, 1)
  def assertUrlData(self, request_mock):
    self.assertEqual(len(request_mock.call_args), 2)
    data = request_mock.call_args[0][2]
    self.assertTrue(data is not None)

    metrics = json.loads(data)
    self.assertEqual(len(metrics['metrics']), 1)
    self.assertEqual(metrics['metrics'][0]['metricname'],'metric1')
    self.assertEqual(metrics['metrics'][0]['starttime'],1)
    pass

  def test_blacklisted_set(self):
    hosts = ["1", "2", "3", "4"]
    sleep_time = 1
    bs = BlacklistedSet(hosts, sleep_time)
    bs.blacklist("4")
    counter = 0
    for host in bs:
      counter = counter + 1
    self.assertEqual(counter, 3)
    time.sleep(sleep_time)
    counter = 0
    for host in bs:
      counter = counter + 1
    self.assertEqual(counter, 4)
    bs.blacklist("1")
    bs.blacklist("2")
    counter = 0
    for host in bs:
      counter = counter + 1
    self.assertEqual(counter, 2)


