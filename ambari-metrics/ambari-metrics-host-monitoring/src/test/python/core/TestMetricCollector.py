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
import logging
from unittest import TestCase
from mock.mock import patch

from core.application_metric_map import ApplicationMetricMap
from core.metric_collector import MetricsCollector
from core.event_definition import HostMetricCollectEvent
from core.host_info import HostInfo

logger = logging.getLogger()

class TestMetricCollector(TestCase):
  
  @patch.object(HostInfo, "get_cpu_times")
  @patch.object(ApplicationMetricMap, "__init__")
  def testCollectEvent(self, amm_mock, host_info_mock):
    amm_mock.return_value = None
    host_info_mock.return_value = {'metric_name' : 'metric_value'}

    metric_collector = MetricsCollector(None, amm_mock, host_info_mock)

    group_config = {'collect_every' : 1, 'metrics' : 'cpu'}
    
    e = HostMetricCollectEvent(group_config, 'cpu')
    
    metric_collector.process_event(e)
    
    self.assertEqual(amm_mock.put_metric.call_count, 1)
