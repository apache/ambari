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

import threading
from unittest import TestCase
from unittest.mock import ANY, MagicMock

from ambari_agent.ComponentStatusExecutor import ComponentStatusExecutor
from ambari_agent import Constants


class TestComponentStatusExecutor(TestCase):
  def create_executor(self):
    initializer_module = MagicMock()
    initializer_module.config.status_commands_run_interval = 10
    initializer_module.stop_event = threading.Event()
    initializer_module.is_registered = True
    initializer_module.server_responses_listener.register_response_callback.return_value = (
      lambda: None
    )
    return ComponentStatusExecutor(initializer_module), initializer_module

  @staticmethod
  def configure_send_correlations(initializer_module, *correlation_ids):
    correlations = iter(correlation_ids)

    def send(*_args, **kwargs):
      correlation_id = next(correlations)
      kwargs["presend_hook"](correlation_id)
      return correlation_id

    initializer_module.connection.send.side_effect = send

  def test_complete_snapshot_is_marked_only_after_server_ack(self):
    executor, initializer_module = self.create_executor()
    self.configure_send_correlations(initializer_module, "correlation-1")
    reports = {
      "1": [
        {
          "serviceName": "HDFS",
          "componentName": "NAMENODE",
          "command": "STATUS",
          "status": "STARTED",
          "clusterId": "1",
        }
      ]
    }

    executor.send_updates_to_server(reports, snapshot_complete=True)

    initializer_module.connection.send.assert_called_once_with(
      message={"clusters": reports, "snapshotComplete": True},
      destination=Constants.COMPONENT_STATUS_REPORTS_ENDPOINT,
      presend_hook=ANY,
    )
    self.assertFalse(executor.component_status_snapshot_complete)

    register_call = (
      initializer_module.server_responses_listener.register_response_callback.call_args
    )
    self.assertEqual("correlation-1", register_call.args[0])
    callback = register_call.kwargs["on_success"]
    callback({}, {})
    self.assertTrue(executor.component_status_snapshot_complete)

  def test_ack_from_snapshot_before_forced_refresh_is_ignored(self):
    executor, initializer_module = self.create_executor()
    self.configure_send_correlations(
      initializer_module, "scan-1", "forced-refresh"
    )
    reports = {
      "1": [
        {
          "serviceName": "HDFS",
          "componentName": "NAMENODE",
          "command": "STATUS",
          "status": "STARTED",
          "clusterId": "1",
        }
      ]
    }

    executor.send_updates_to_server(reports, snapshot_complete=True)
    first_register_call = (
      initializer_module.server_responses_listener.register_response_callback.call_args_list[
        0
      ]
    )
    self.assertEqual("scan-1", first_register_call.args[0])
    stale_callback = first_register_call.kwargs["on_success"]
    executor.force_send_component_statuses()
    stale_callback({}, {})

    self.assertFalse(executor.component_status_snapshot_complete)
    self.assertEqual({}, dict(executor.reported_component_status))
