#!/usr/bin/env python3

"""
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
"""

import copy
import os

from ambari_agent.AlertSchedulerHandler import AlertSchedulerHandler
from ambari_agent.alerts.metric_alert import MetricAlert
from ambari_agent.alerts.ams_alert import AmsAlert
from ambari_agent.alerts.port_alert import PortAlert
from ambari_agent.alerts.web_alert import WebAlert

from ambari_agent.InitializerModule import InitializerModule

from ambari_agent.AmbariConfig import AmbariConfig

from unittest.mock import ANY, Mock, MagicMock, patch
from unittest import TestCase

TEST_PATH = os.path.join("ambari_agent", "dummy_files")


class TestAlertSchedulerHandler(TestCase):
  def setUp(self):
    self.config = AmbariConfig()

  def test_scheduled_job_applies_context_before_collect(self):
    initializer_module = InitializerModule()
    initializer_module.init()
    scheduler = AlertSchedulerHandler(initializer_module)
    scheduler._job_context_injector = Mock()
    alert = MagicMock()

    scheduled_callable = scheduler._AlertSchedulerHandler__make_function(alert)
    scheduled_callable()

    scheduler._job_context_injector.assert_called_once_with(scheduler.config)
    alert.collect.assert_called_once_with()

  def test_schedule_definition_uses_stable_id_and_minute_interval(self):
    initializer_module = InitializerModule()
    initializer_module.init()
    scheduler = AlertSchedulerHandler(initializer_module)
    scheduler._AlertSchedulerHandler__scheduler = MagicMock()
    definition = MagicMock()
    definition.is_enabled.return_value = True
    definition.get_uuid.return_value = "alert-uuid"
    definition.interval.return_value = 5

    scheduler.schedule_definition(definition)

    scheduler._AlertSchedulerHandler__scheduler.add_job.assert_called_once_with(
      ANY,
      trigger="interval",
      id="alert-uuid",
      name="alert-uuid",
      replace_existing=True,
      minutes=5,
    )

  def test_schedule_definition_uses_second_interval(self):
    initializer_module = InitializerModule()
    initializer_module.init()
    scheduler = AlertSchedulerHandler(initializer_module, in_minutes=False)
    scheduler._AlertSchedulerHandler__scheduler = MagicMock()
    definition = MagicMock()
    definition.is_enabled.return_value = True
    definition.get_uuid.return_value = "alert-uuid"
    definition.interval.return_value = 15

    scheduler.schedule_definition(definition)

    self.assertEqual(
      scheduler._AlertSchedulerHandler__scheduler.add_job.call_args.kwargs["seconds"],
      15,
    )

  def test_schedule_definition_skips_disabled_alert(self):
    initializer_module = InitializerModule()
    initializer_module.init()
    scheduler = AlertSchedulerHandler(initializer_module)
    scheduler._AlertSchedulerHandler__scheduler = MagicMock()
    definition = MagicMock()
    definition.is_enabled.return_value = False

    scheduler.schedule_definition(definition)

    scheduler._AlertSchedulerHandler__scheduler.add_job.assert_not_called()

  def test_reschedule_replaces_existing_definition_by_uuid(self):
    initializer_module = InitializerModule()
    initializer_module.init()
    scheduler = AlertSchedulerHandler(initializer_module)
    scheduled_job = MagicMock()
    scheduled_job.id = "alert-uuid"
    scheduled_job.name = "alert-uuid"
    scheduler._AlertSchedulerHandler__scheduler = MagicMock()
    scheduler._AlertSchedulerHandler__scheduler.get_jobs.return_value = [scheduled_job]
    definition = MagicMock()
    definition.get_uuid.return_value = "alert-uuid"
    definition.is_enabled.return_value = True
    scheduler._AlertSchedulerHandler__load_definitions = Mock(return_value=[definition])
    scheduler.schedule_definition = Mock(return_value=True)

    scheduler.reschedule()

    scheduler.schedule_definition.assert_called_once_with(definition)
    scheduler._AlertSchedulerHandler__scheduler.remove_job.assert_not_called()

  def test_reschedule_removes_job_for_disabled_definition(self):
    initializer_module = InitializerModule()
    initializer_module.init()
    scheduler = AlertSchedulerHandler(initializer_module)
    scheduled_job = MagicMock()
    scheduled_job.id = "alert-uuid"
    scheduled_job.name = "alert-uuid"
    scheduler._AlertSchedulerHandler__scheduler = MagicMock()
    scheduler._AlertSchedulerHandler__scheduler.get_jobs.return_value = [scheduled_job]
    scheduler._collector = MagicMock()
    definition = MagicMock()
    definition.get_uuid.return_value = "alert-uuid"
    definition.is_enabled.return_value = False
    scheduler._AlertSchedulerHandler__load_definitions = Mock(return_value=[definition])
    scheduler.schedule_definition = Mock(return_value=True)

    scheduler.reschedule()

    scheduler._AlertSchedulerHandler__scheduler.remove_job.assert_called_once_with(
      "alert-uuid"
    )
    scheduler.schedule_definition.assert_not_called()
    scheduler._collector.remove_by_uuid.assert_called_with("alert-uuid")

  def test_stop_shuts_down_running_scheduler_and_prepares_clean_restart(self):
    initializer_module = InitializerModule()
    initializer_module.init()
    scheduler = AlertSchedulerHandler(initializer_module)
    running_scheduler = MagicMock()
    running_scheduler.running = True
    replacement_scheduler = MagicMock()
    scheduler._AlertSchedulerHandler__scheduler = running_scheduler
    scheduler._create_scheduler = Mock(return_value=replacement_scheduler)

    scheduler.stop()

    running_scheduler.shutdown.assert_called_once_with(wait=False)
    scheduler._create_scheduler.assert_called_once_with()
    self.assertIs(
      scheduler._AlertSchedulerHandler__scheduler, replacement_scheduler
    )

  @patch("ambari_commons.network.reconfigure_urllib2_opener")
  def test_job_context_injector(self, reconfigure_urllib2_opener_mock):
    self.config.use_system_proxy_setting = lambda: False

    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    scheduler._job_context_injector(self.config)

    self.assertTrue(reconfigure_urllib2_opener_mock.called)

    reconfigure_urllib2_opener_mock.reset_mock()

    self.config.use_system_proxy_setting = lambda: True

    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    scheduler._job_context_injector(self.config)
    self.assertFalse(reconfigure_urllib2_opener_mock.called)

  def test_json_to_callable_metric(self):
    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    json_definition = {"source": {"type": "METRIC"}}

    callable_result = scheduler._AlertSchedulerHandler__json_to_callable(
      "cluster", "host", "host", copy.deepcopy(json_definition)
    )

    self.assertTrue(callable_result is not None)
    self.assertTrue(isinstance(callable_result, MetricAlert))
    self.assertEqual(callable_result.alert_meta, json_definition)
    self.assertEqual(callable_result.alert_source_meta, json_definition["source"])

  def test_json_to_callable_ams(self):
    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    json_definition = {"source": {"type": "AMS"}}

    callable_result = scheduler._AlertSchedulerHandler__json_to_callable(
      "cluster", "host", "host", copy.deepcopy(json_definition)
    )

    self.assertTrue(callable_result is not None)
    self.assertTrue(isinstance(callable_result, AmsAlert))
    self.assertEqual(callable_result.alert_meta, json_definition)
    self.assertEqual(callable_result.alert_source_meta, json_definition["source"])

  def test_json_to_callable_port(self):
    json_definition = {"source": {"type": "PORT"}}

    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    callable_result = scheduler._AlertSchedulerHandler__json_to_callable(
      "cluster", "host", "host", copy.deepcopy(json_definition)
    )

    self.assertTrue(callable_result is not None)
    self.assertTrue(isinstance(callable_result, PortAlert))
    self.assertEqual(callable_result.alert_meta, json_definition)
    self.assertEqual(callable_result.alert_source_meta, json_definition["source"])

  def test_json_to_callable_web(self):
    json_definition = {"source": {"type": "WEB"}}
    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    callable_result = scheduler._AlertSchedulerHandler__json_to_callable(
      "cluster", "host", "host", copy.deepcopy(json_definition)
    )

    self.assertTrue(callable_result is not None)
    self.assertTrue(isinstance(callable_result, WebAlert))
    self.assertEqual(callable_result.alert_meta, json_definition)
    self.assertEqual(callable_result.alert_source_meta, json_definition["source"])

  def test_json_to_callable_none(self):
    json_definition = {"source": {"type": "SOMETHING"}}

    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    callable_result = scheduler._AlertSchedulerHandler__json_to_callable(
      "cluster", "host", "host", copy.deepcopy(json_definition)
    )

    self.assertTrue(callable_result is None)

  def test_execute_alert_noneScheduler(self):
    execution_commands = []

    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    scheduler._AlertSchedulerHandler__scheduler = None
    alert_mock = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)

    scheduler.execute_alert(execution_commands)

    self.assertFalse(alert_mock.collect.called)

  def test_execute_alert_noneCommands(self):
    execution_commands = None

    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    alert_mock = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)

    scheduler.execute_alert(execution_commands)

    self.assertFalse(alert_mock.collect.called)

  def test_execute_alert_emptyCommands(self):
    execution_commands = []

    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    alert_mock = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)

    scheduler.execute_alert(execution_commands)

    self.assertFalse(alert_mock.collect.called)

  def test_execute_alert(self):
    execution_commands = [
      {
        "clusterName": "cluster",
        "hostName": "host",
        "publicHostName": "host",
        "alertDefinition": {"name": "alert1"},
      }
    ]

    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)

    # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    alert_mock = MagicMock()
    alert_mock.collect = Mock()
    alert_mock.set_helpers = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)
    scheduler._AlertSchedulerHandler__config_maps = {"cluster": {}}

    scheduler.execute_alert(execution_commands)

    scheduler._AlertSchedulerHandler__json_to_callable.assert_called_with(
      "cluster", "host", "host", {"name": "alert1"}
    )
    self.assertTrue(alert_mock.collect.called)

  def test_execute_alert_from_extension(self):
    execution_commands = [
      {
        "clusterName": "cluster",
        "hostName": "host",
        "publicHostName": "host",
        "alertDefinition": {"name": "alert1"},
      }
    ]

    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    #'wrong_path', 'wrong_path', 'wrong_path', TEST_PATH, 'wrong_path', None, self.config, None)
    alert_mock = MagicMock()
    alert_mock.collect = Mock()
    alert_mock.set_helpers = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)
    scheduler._AlertSchedulerHandler__config_maps = {"cluster": {}}

    scheduler.execute_alert(execution_commands)

    scheduler._AlertSchedulerHandler__json_to_callable.assert_called_with(
      "cluster", "host", "host", {"name": "alert1"}
    )
    self.assertTrue(alert_mock.collect.called)

  def test_load_definitions(self):
    definitions = {"alertDefinitions": [{"source": {"type": "PORT"}}]}
    initializer_module = InitializerModule()
    initializer_module.init()
    initializer_module.alert_definitions_cache.rewrite_cluster_cache("0", definitions)

    scheduler = AlertSchedulerHandler(
      initializer_module
    )  # (TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    scheduler._AlertSchedulerHandler__config_maps = {"cluster": {}}

    definitions = scheduler._AlertSchedulerHandler__load_definitions()

    alert_def = definitions[0]
    self.assertTrue(isinstance(alert_def, PortAlert))

  def test_load_definitions_noFile(self):
    initializer_module = InitializerModule()
    initializer_module.init()
    initializer_module.alert_definitions_cache.rewrite_cluster_cache(
      "0", {"alertDefinitions": []}
    )

    scheduler = AlertSchedulerHandler(initializer_module)
    # ('wrong_path', 'wrong_path', 'wrong_path', 'wrong_path', 'wrong_path', None, self.config, None)
    scheduler._AlertSchedulerHandler__config_maps = {"cluster": {}}

    definitions = scheduler._AlertSchedulerHandler__load_definitions()

    self.assertEqual(definitions, [])

  def __test_start(self):
    execution_commands = [
      {
        "clusterName": "cluster",
        "hostName": "host",
        "publicHostName": "host",
        "alertDefinition": {"name": "alert1"},
      }
    ]

    initializer_module = InitializerModule()
    initializer_module.init()

    scheduler = AlertSchedulerHandler(initializer_module)
    # TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, TEST_PATH, None, self.config, None)
    alert_mock = MagicMock()
    alert_mock.interval = Mock(return_value=5)
    alert_mock.collect = Mock()
    alert_mock.set_helpers = Mock()
    scheduler.schedule_definition = MagicMock()
    scheduler._AlertSchedulerHandler__scheduler = MagicMock()
    scheduler._AlertSchedulerHandler__scheduler.running = False
    scheduler._AlertSchedulerHandler__scheduler.start = Mock()
    scheduler._AlertSchedulerHandler__json_to_callable = Mock(return_value=alert_mock)
    scheduler._AlertSchedulerHandler__config_maps = {"cluster": {}}

    scheduler.start()

    self.assertTrue(scheduler._AlertSchedulerHandler__scheduler.start.called)
    scheduler.schedule_definition.assert_called_with(alert_mock)
