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
import urllib2

import logging
from unittest import TestCase
from only_for_platform import get_platform, os_distro_value, PLATFORM_WINDOWS

from ambari_commons.os_check import OSCheck

from mock.mock import patch, MagicMock

with patch("platform.linux_distribution", return_value = os_distro_value):
  from ambari_commons import OSCheck
  from core.application_metric_map import ApplicationMetricMap
  from core.config_reader import Configuration
  from core.emitter import Emitter
  from core.stop_handler import bind_signal_handlers

logger = logging.getLogger()

class TestEmitter(TestCase):

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("urllib2.urlopen")
  def testJavaHomeAvailableCheck(self, url_open_mock):
    url_open_mock.return_value = MagicMock()
    url_open_mock.return_value.getcode.return_value = 200
    self.assertEqual(urllib2.urlopen(None, None).getcode(), 200)
    url_open_mock.reset_mock()

    stop_handler = bind_signal_handlers()

    config = Configuration()
    application_metric_map = ApplicationMetricMap("host","10.10.10.10")
    application_metric_map.clear()
    application_metric_map.put_metric("APP1", {"metric1":1}, 1)
    emitter = Emitter(config, application_metric_map, stop_handler)
    emitter.submit_metrics()
    
    self.assertEqual(url_open_mock.call_count, 1)
    self.assertUrlData(url_open_mock)


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("urllib2.urlopen")
  def testRetryFetch(self, url_open_mock):
    stop_handler = bind_signal_handlers()

    config = Configuration()
    application_metric_map = ApplicationMetricMap("host","10.10.10.10")
    application_metric_map.clear()
    application_metric_map.put_metric("APP1", {"metric1":1}, 1)
    emitter = Emitter(config, application_metric_map, stop_handler)
    emitter.RETRY_SLEEP_INTERVAL = .001
    emitter.submit_metrics()
    
    self.assertEqual(url_open_mock.call_count, 3)
    self.assertUrlData(url_open_mock)
    
  def assertUrlData(self, url_open_mock):
    self.assertEqual(len(url_open_mock.call_args), 2)
    data = url_open_mock.call_args[0][0].data
    self.assertTrue(data is not None)
    
    metrics = json.loads(data)
    self.assertEqual(len(metrics['metrics']), 1)
    self.assertEqual(metrics['metrics'][0]['metricname'],'metric1')
    self.assertEqual(metrics['metrics'][0]['starttime'],1)
    pass
