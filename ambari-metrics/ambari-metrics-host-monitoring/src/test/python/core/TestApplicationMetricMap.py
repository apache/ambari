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
from unittest import TestCase
from application_metric_map import ApplicationMetricMap

logger = logging.getLogger()

class TestApplicationMetricMap(TestCase):
  
  def testApplicationMetricMap(self):
    application_metric_map = ApplicationMetricMap("host1", "10.10.10.10")
    
    application_id = application_metric_map.format_app_id("A","1")
    timestamp = int(round(1415390657.3806491 * 1000))
    
    metrics = {}
    metrics.update({"b" : 'bv'})
    
    application_metric_map.put_metric(application_id, metrics, timestamp)
    application_metric_map.put_metric(application_id, metrics, timestamp + 1)
    application_metric_map.put_metric(application_id, metrics, timestamp + 2)
    application_metric_map.put_metric(application_id, metrics, timestamp + 3)
    
    p = json.loads(application_metric_map.flatten(application_id))
    self.assertEqual(len(p['metrics']), 1)
    self.assertEqual(p['metrics'][0]['metricname'], "b")
#     self.assertEqual(p['metrics'][0]['appid'], application_id)
    self.assertEqual(p['metrics'][0]['hostname'], "host1")
    self.assertEqual(len(p['metrics'][0]['metrics']), 4)
    self.assertEqual(p['metrics'][0]['metrics'][str(timestamp)], 'bv')
    
    self.assertEqual(application_metric_map.get_start_time(application_id, "b"), timestamp)
    
    metrics = {}
    metrics.update({"b" : 'bv'})
    metrics.update({"a" : 'av'})
    application_metric_map.put_metric(application_id, metrics, timestamp)
    p = json.loads(application_metric_map.flatten(application_id))
    self.assertEqual(len(p['metrics']), 2)
    self.assertTrue((p['metrics'][0]['metricname'] == 'a' and p['metrics'][1]['metricname'] == 'b') or 
                    (p['metrics'][1]['metricname'] == 'a' and p['metrics'][0]['metricname'] == 'b'))
    
    
  def testEmptyMapReturnNone(self):
    application_metric_map = ApplicationMetricMap("host", "10.10.10.10")
    self.assertTrue(application_metric_map.flatten() == None)

  def testFlattenAndClear(self):
    application_metric_map = ApplicationMetricMap("host", "10.10.10.10")
    application_metric_map.put_metric("A1", { "a" : "b" }, int(round(1415390657.3806491 * 1000)))
    json_data = json.loads(application_metric_map.flatten('A1', True))
    self.assertEqual(len(json_data['metrics']), 1)
    self.assertTrue(json_data['metrics'][0]['metricname'] == 'a')
    self.assertFalse(application_metric_map.app_metric_map)