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

from ambari_agent.ComponentStatusExecutor import ComponentStatusExecutor
from ambari_agent import Constants
from mock.mock import MagicMock


class TestComponentStatusExecutor(TestCase):
  def create_executor(self):
    initializer_module = MagicMock()
    initializer_module.config.status_commands_run_interval = 10
    initializer_module.stop_event = threading.Event()
    initializer_module.is_registered = True
    initializer_module.server_responses_listener.listener_functions_on_success = {}
    initializer_module.connection.send.return_value = "correlation-1"
    return ComponentStatusExecutor(initializer_module), initializer_module

  def test_complete_snapshot_is_marked_only_after_server_ack(self):
    executor, initializer_module = self.create_executor()
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
    )
    self.assertFalse(executor.component_status_snapshot_complete)

    callback = initializer_module.server_responses_listener.listener_functions_on_success[
      "correlation-1"
    ]
    callback({}, {})
    self.assertTrue(executor.component_status_snapshot_complete)

  def test_ack_from_snapshot_before_forced_refresh_is_ignored(self):
    executor, initializer_module = self.create_executor()
    initializer_module.connection.send.side_effect = ["scan-1", "forced-refresh"]
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
    stale_callback = (
      initializer_module.server_responses_listener.listener_functions_on_success[
        "scan-1"
      ]
    )
    executor.force_send_component_statuses()
    stale_callback({}, {})

    self.assertFalse(executor.component_status_snapshot_complete)
    self.assertEqual({}, dict(executor.reported_component_status))
