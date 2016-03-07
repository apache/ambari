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
from alerts.script_alert import ScriptAlert
from mock.mock import Mock, MagicMock, patch
import os

from AmbariConfig import AmbariConfig

DUMMY_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'dummy_files')

class TestScriptAlert(TestCase):

  def setUp(self):
    self.config = AmbariConfig()

  def test_collect(self):
    alert_meta = {
      'name': 'alert1',
      'label': 'label1',
      'serviceName': 'service1',
      'componentName': 'component1',
      'uuid': '123',
      'enabled': 'true'
    }
    alert_source_meta = {
      'stacks_directory': DUMMY_PATH,
      'path': os.path.join(DUMMY_PATH, 'test_script.py'),
      'common_services_directory': DUMMY_PATH,
      'host_scripts_directory': DUMMY_PATH,
    }
    cluster = 'c1'
    host = 'host1'
    expected_text = 'bar is 12, baz is asd'

    def collector_side_effect(clus, data):
      self.assertEquals(data['name'], alert_meta['name'])
      self.assertEquals(data['label'], alert_meta['label'])
      #self.assertEquals(data['text'], expected_text)
      self.assertEquals(data['service'], alert_meta['serviceName'])
      self.assertEquals(data['component'], alert_meta['componentName'])
      self.assertEquals(data['uuid'], alert_meta['uuid'])
      self.assertEquals(data['enabled'], alert_meta['enabled'])
      self.assertEquals(data['cluster'], cluster)
      self.assertEquals(clus, cluster)

    mock_collector = MagicMock()
    mock_collector.put = Mock(side_effect=collector_side_effect)

    alert = ScriptAlert(alert_meta, alert_source_meta, self.config)
    alert.set_helpers(mock_collector, {'foo-site/bar': 12, 'foo-site/baz': 'asd'})
    alert.set_cluster(cluster, host)

    alert.collect()
