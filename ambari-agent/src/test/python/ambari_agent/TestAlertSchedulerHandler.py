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

import copy
import os

from ambari_agent.AlertSchedulerHandler import AlertSchedulerHandler
from ambari_agent.alerts.metric_alert import MetricAlert
from ambari_agent.alerts.ams_alert import AmsAlert
from ambari_agent.alerts.port_alert import PortAlert
from ambari_agent.alerts.web_alert import WebAlert

from AmbariConfig import AmbariConfig

from mock.mock import Mock, MagicMock, patch
from unittest import TestCase

TEST_PATH = os.path.join('ambari_agent', 'dummy_files')

class TestAlertSchedulerHandler(TestCase):

  def setUp(self):
    self.config = AmbariConfig()

  def test_load_definitions(self):
    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None)

    definitions = scheduler._AlertSchedulerHandler__load_definitions()

    self.assertEquals(len(definitions), 1)

  @patch("ambari_commons.network.reconfigure_urllib2_opener")
  def test_job_context_injector(self, reconfigure_urllib2_opener_mock):
    self.config.use_system_proxy_setting = lambda: False
    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    scheduler._job_context_injector(self.config)

    self.assertTrue(reconfigure_urllib2_opener_mock.called)

    reconfigure_urllib2_opener_mock.reset_mock()

    self.config.use_system_proxy_setting = lambda: True
    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    scheduler._job_context_injector(self.config)
    self.assertFalse(reconfigure_urllib2_opener_mock.called)


  def test_json_to_callable_metric(self):
    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    json_definition = {
      'source': {
        'type': 'METRIC'
      }
    }

    callable_result = scheduler._AlertSchedulerHandler__json_to_callable('cluster', 'host', 'host', copy.deepcopy(json_definition))

    self.assertTrue(callable_result is not None)
    self.assertTrue(isinstance(callable_result, MetricAlert))
    self.assertEquals(callable_result.alert_meta, json_definition)
    self.assertEquals(callable_result.alert_source_meta, json_definition['source'])

  def test_json_to_callable_ams(self):
    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    json_definition = {
      'source': {
        'type': 'AMS'
      }
    }

    callable_result = scheduler._AlertSchedulerHandler__json_to_callable('cluster', 'host', 'host', copy.deepcopy(json_definition))

    self.assertTrue(callable_result is not None)
    self.assertTrue(isinstance(callable_result, AmsAlert))
    self.assertEquals(callable_result.alert_meta, json_definition)
    self.assertEquals(callable_result.alert_source_meta, json_definition['source'])

  def test_json_to_callable_port(self):
    json_definition = {
      'source': {
        'type': 'PORT'
      }
    }

    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    callable_result = scheduler._AlertSchedulerHandler__json_to_callable('cluster', 'host', 'host', copy.deepcopy(json_definition))

    self.assertTrue(callable_result is not None)
    self.assertTrue(isinstance(callable_result, PortAlert))
    self.assertEquals(callable_result.alert_meta, json_definition)
    self.assertEquals(callable_result.alert_source_meta, json_definition['source'])

  def test_json_to_callable_web(self):

    json_definition = {
      'source': {
        'type': 'WEB'
      }
    }

    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    callable_result = scheduler._AlertSchedulerHandler__json_to_callable('cluster', 'host', 'host', copy.deepcopy(json_definition))

    self.assertTrue(callable_result is not None)
    self.assertTrue(isinstance(callable_result, WebAlert))
    self.assertEquals(callable_result.alert_meta, json_definition)
    self.assertEquals(callable_result.alert_source_meta, json_definition['source'])

  def test_json_to_callable_none(self):
    json_definition = {
      'source': {
        'type': 'SOMETHING'
      }
    }

    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    callable_result = scheduler._AlertSchedulerHandler__json_to_callable('cluster', 'host', 'host', copy.deepcopy(json_definition))

    self.assertTrue(callable_result is None)

  def test_execute_alert_noneScheduler(self):
    execution_commands = []

    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    scheduler._AlertSchedulerHandler__scheduler = None
    alert_mock = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)

    scheduler.execute_alert(execution_commands)

    self.assertFalse(alert_mock.collect.called)

  def test_execute_alert_noneCommands(self):
    execution_commands = None

    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    alert_mock = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)

    scheduler.execute_alert(execution_commands)

    self.assertFalse(alert_mock.collect.called)

  def test_execute_alert_emptyCommands(self):
    execution_commands = []

    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    alert_mock = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)

    scheduler.execute_alert(execution_commands)

    self.assertFalse(alert_mock.collect.called)

  def test_execute_alert(self):
    execution_commands = [
      {
        'clusterName': 'cluster',
        'hostName': 'host',
        'publicHostName' : 'host',
        'alertDefinition': {
          'name': 'alert1'
        }
      }
    ]

    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    alert_mock = MagicMock()
    alert_mock.collect = Mock()
    alert_mock.set_helpers = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)
    scheduler._AlertSchedulerHandler__config_maps = {
      'cluster': {}
    }

    scheduler.execute_alert(execution_commands)

    scheduler._AlertSchedulerHandler__json_to_callable.assert_called_with('cluster', 'host', 'host', {'name': 'alert1'})
    self.assertTrue(alert_mock.collect.called)

  def test_execute_alert_from_extension(self):
    execution_commands = [
      {
        'clusterName': 'cluster',
        'hostName': 'host',
        'publicHostName' : 'host',
        'alertDefinition': {
          'name': 'alert1'
        }
      }
    ]

    scheduler = AlertSchedulerHandler('wrong_path', 'wrong_path', 'wrong_path', TEST_PATH, 'wrong_path', None, self.config, None)
    alert_mock = MagicMock()
    alert_mock.collect = Mock()
    alert_mock.set_helpers = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)
    scheduler._AlertSchedulerHandler__config_maps = {
      'cluster': {}
    }

    scheduler.execute_alert(execution_commands)

    scheduler._AlertSchedulerHandler__json_to_callable.assert_called_with('cluster', 'host', 'host', {'name': 'alert1'})
    self.assertTrue(alert_mock.collect.called)

  def test_load_definitions(self):
    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    scheduler._AlertSchedulerHandler__config_maps = {
      'cluster': {}
    }

    definitions = scheduler._AlertSchedulerHandler__load_definitions()

    alert_def = definitions[0]
    self.assertTrue(isinstance(alert_def, PortAlert))

  def test_load_definitions_noFile(self):
    scheduler = AlertSchedulerHandler('wrong_path', 'wrong_path', 'wrong_path', 'wrong_path', 'wrong_path', None, self.config, None)
    scheduler._AlertSchedulerHandler__config_maps = {
      'cluster': {}
    }

    definitions = scheduler._AlertSchedulerHandler__load_definitions()

    self.assertEquals(definitions, [])

  def test_start(self):
    execution_commands = [
      {
        'clusterName': 'cluster',
        'hostName': 'host',
        'publicHostName' : 'host',
        'alertDefinition': {
          'name': 'alert1'
        }
      }
    ]

    scheduler = AlertSchedulerHandler(TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    alert_mock = MagicMock()
    alert_mock.interval = Mock(return_value=5)
    alert_mock.collect = Mock()
    alert_mock.set_helpers = Mock()
    scheduler.schedule_definition = MagicMock()
    scheduler._AlertSchedulerHandler__scheduler = MagicMock()
    scheduler._AlertSchedulerHandler__scheduler.running = False
    scheduler._AlertSchedulerHandler__scheduler.start = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)
    scheduler._AlertSchedulerHandler__config_maps = {
      'cluster': {}
    }

    scheduler.start()

    self.assertTrue(scheduler._AlertSchedulerHandler__scheduler.start.called)
    scheduler.schedule_definition.assert_called_with(alert_mock)
