#!/usr/bin/env python3

"""
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""

import json
from unittest import TestCase
from unittest.mock import MagicMock, patch

from ambari_agent.AmbariStompConnection import ConnectionIsAlreadyClosed
from ambari_agent.CommandStatusDict import CommandStatusDict
from ambari_agent.models.commands import CommandStatus


class TestCommandStatusDict(TestCase):
  def setUp(self):
    self.initializer = MagicMock()
    self.initializer.is_registered = True
    self.initializer.config.command_update_output = True
    self.initializer.config.log_max_symbols_size = 1000
    self.initializer.server_responses_listener.listener_functions_on_success = {}
    self.initializer.server_responses_listener.listener_functions_on_error = {}

    def register_callback(correlation_id, on_success=None, on_error=None):
      if on_success is not None:
        self.initializer.server_responses_listener.listener_functions_on_success[
          correlation_id
        ] = on_success
      if on_error is not None:
        self.initializer.server_responses_listener.listener_functions_on_error[
          correlation_id
        ] = on_error
      return lambda: discard_callback(correlation_id)

    def discard_callback(correlation_id):
      self.initializer.server_responses_listener.listener_functions_on_success.pop(
        correlation_id, None
      )
      self.initializer.server_responses_listener.listener_functions_on_error.pop(
        correlation_id, None
      )

    self.initializer.server_responses_listener.register_response_callback.side_effect = (
      register_callback
    )
    self.initializer.server_responses_listener.discard_response_callback.side_effect = (
      discard_callback
    )
    self.sent_reports = {}
    self.next_correlation_id = 0
    self.initializer.connection.send.side_effect = self._send
    self.statuses = CommandStatusDict(self.initializer)

  def _send(self, message, destination, log_message_function, presend_hook):
    correlation_id = self.next_correlation_id
    self.next_correlation_id += 1
    presend_hook(correlation_id)
    self.sent_reports[correlation_id] = message["clusters"]
    return correlation_id

  @staticmethod
  def _command(task_id, cluster_id="1"):
    return {
      "clusterId": cluster_id,
      "commandType": "EXECUTION_COMMAND",
      "taskId": task_id,
    }

  @staticmethod
  def _report(task_id, marker=None, cluster_id="1"):
    report = {
      "clusterId": cluster_id,
      "status": CommandStatus.completed,
      "taskId": task_id,
    }
    if marker is not None:
      report["marker"] = marker
    return report

  def _queue_terminal_report(self, task_id, marker=None, cluster_id="1"):
    self.statuses.queue_report_sending(
      task_id,
      self._command(task_id, cluster_id),
      self._report(task_id, marker, cluster_id),
    )

  def test_put_terminal_status_is_cleared_only_after_success_ack(self):
    command = self._command(1)
    report = self._report(1)

    self.statuses.put_command_status(command, report)

    self.assertIn(1, self.statuses.current_state)
    self.initializer.server_responses_listener.listener_functions_on_success[0](
      {}, {"status": "OK"}
    )
    self.assertNotIn(1, self.statuses.current_state)

  def test_split_ack_callbacks_clear_only_their_own_reports_out_of_order(self):
    for task_id in (1, 2, 3):
      self._queue_terminal_report(task_id, marker="x" * 32)

    with patch.object(CommandStatusDict, "MAX_REPORT_SIZE", 110):
      self.statuses.report()

    self.assertEqual(len(self.sent_reports), 3)
    task_by_correlation = {
      correlation_id: next(iter(batch.values()))[0]["taskId"]
      for correlation_id, batch in self.sent_reports.items()
    }
    callbacks = (
      self.initializer.server_responses_listener.listener_functions_on_success
    )

    callbacks[2]({}, {"status": "OK"})
    self.assertNotIn(task_by_correlation[2], self.statuses.current_state)
    self.assertIn(task_by_correlation[0], self.statuses.current_state)
    self.assertIn(task_by_correlation[1], self.statuses.current_state)

    callbacks[0]({}, {"status": "OK"})
    callbacks[1]({}, {"status": "OK"})
    self.assertEqual(self.statuses.current_state, {})

  def test_failed_split_does_not_block_successful_split_cleanup(self):
    for task_id in (1, 2):
      self._queue_terminal_report(task_id, marker="x" * 32)

    successful_send = self._send

    def fail_second_send(*args, **kwargs):
      if self.next_correlation_id == 1:
        raise ConnectionIsAlreadyClosed("closed")
      return successful_send(*args, **kwargs)

    self.initializer.connection.send.side_effect = fail_second_send
    with patch.object(CommandStatusDict, "MAX_REPORT_SIZE", 110):
      self.statuses.report()

    self.initializer.server_responses_listener.listener_functions_on_success[0](
      {}, {"status": "OK"}
    )
    self.assertEqual(set(self.statuses.current_state), {2})
    self.assertEqual(self.statuses.pending_batches, {})

  def test_duplicate_ack_is_idempotent(self):
    self.statuses.put_command_status(self._command(1), self._report(1))

    self.statuses.acknowledge_batch(0)
    self.statuses.acknowledge_batch(0)

    self.assertEqual(self.statuses.current_state, {})

  def test_stale_ack_does_not_clear_a_newer_report_for_the_same_task(self):
    self.statuses.put_command_status(self._command(1), self._report(1, "old"))
    self.statuses.put_command_status(self._command(1), self._report(1, "new"))

    self.statuses.acknowledge_batch(0)

    self.assertEqual(self.statuses.current_state[1][1]["marker"], "new")
    self.statuses.acknowledge_batch(1)
    self.assertEqual(self.statuses.current_state, {})

  def test_stale_ack_does_not_clear_identical_new_generation(self):
    report = self._report(1, "same")
    self.statuses.put_command_status(self._command(1), report)
    self.statuses.put_command_status(self._command(1), report.copy())

    self.statuses.acknowledge_batch(0)

    self.assertIn(1, self.statuses.current_state)
    self.statuses.acknowledge_batch(1)
    self.assertEqual(self.statuses.current_state, {})

  def test_unexpected_send_failure_removes_registered_callbacks(self):
    def fail_after_presend(*args, **kwargs):
      kwargs["presend_hook"](7)
      raise RuntimeError("send failed")

    self.initializer.connection.send.side_effect = fail_after_presend

    with self.assertRaisesRegex(RuntimeError, "send failed"):
      self.statuses.put_command_status(self._command(1), self._report(1))

    self.assertEqual(self.statuses.pending_batches, {})
    self.assertEqual(
      self.initializer.server_responses_listener.listener_functions_on_success, {}
    )
    self.assertEqual(
      self.initializer.server_responses_listener.listener_functions_on_error, {}
    )
    self.assertIn(1, self.statuses.current_state)

  def test_error_response_discards_pending_batch_but_keeps_report(self):
    self.statuses.put_command_status(self._command(1), self._report(1))

    self.initializer.server_responses_listener.listener_functions_on_error[0](
      {}, {"status": "ERROR"}
    )

    self.assertEqual(self.statuses.pending_batches, {})
    self.assertIn(1, self.statuses.current_state)

  def test_unregistered_agent_does_not_register_none_correlation(self):
    self.initializer.is_registered = False

    self.statuses.put_command_status(self._command(1), self._report(1))

    self.initializer.connection.send.assert_not_called()
    self.assertEqual(
      self.initializer.server_responses_listener.listener_functions_on_success, {}
    )

  def test_auto_execution_status_is_cleaned_without_waiting_for_an_ack(self):
    command = self._command(1)
    command.update({"commandId": "auto-1", "commandType": "AUTO_EXECUTION_COMMAND"})
    self.statuses.queue_report_sending(1, command, self._report(1))

    report = self.statuses.generate_report()

    self.assertEqual({}, report)
    self.assertNotIn(1, self.statuses.current_state)
    self.assertNotIn(1, self.statuses.report_revisions)
    self.assertNotIn(1, self.statuses.reported_reports)

  def test_disabled_command_output_suppresses_periodic_in_progress_report(self):
    self.statuses.command_update_output = False
    report = self._report(1)
    report["status"] = CommandStatus.in_progress
    self.statuses.queue_report_sending(1, self._command(1), report)

    self.assertEqual({}, self.statuses.generate_report())
    self.assertIn(1, self.statuses.current_state)

  def test_single_oversized_report_is_truncated_without_an_empty_batch(self):
    oversized = self._report(1)
    oversized["stdout"] = "prefix" + "x" * 200

    batches = list(self.statuses.split_reports({"1": [oversized]}, 150))

    self.assertEqual(1, len(batches))
    self.assertTrue(all(batch for batch in batches))
    self.assertTrue(self.statuses.size_approved(batches[0], 150))
    compact = batches[0]["1"][0]
    self.assertTrue(compact["stdout"].startswith(self.statuses.TRUNCATION_NOTICE))
    self.assertTrue(compact["stdout"].endswith("x"))

  def test_truncated_terminal_report_is_cleared_by_its_ack(self):
    oversized = self._report(1)
    oversized["stdout"] = "x" * 200
    self.statuses.queue_report_sending(1, self._command(1), oversized)

    with patch.object(CommandStatusDict, "MAX_REPORT_SIZE", 150):
      self.statuses.report()

    self.assertTrue(self.statuses.size_approved(self.sent_reports[0], 150))
    self.statuses.acknowledge_batch(0)
    self.assertNotIn(1, self.statuses.current_state)

  def test_retry_of_same_revision_supersedes_old_pending_callback(self):
    self._queue_terminal_report(1)

    self.statuses.report()
    self.statuses.report()

    self.assertEqual({1}, set(self.statuses.pending_batches))
    self.assertEqual(
      {1},
      set(
        self.initializer.server_responses_listener.listener_functions_on_success
      ),
    )
    self.assertEqual(
      {1},
      set(self.initializer.server_responses_listener.listener_functions_on_error),
    )

  def test_new_in_progress_revision_supersedes_old_pending_callback(self):
    command = self._command(1)
    first = self._report(1, marker="first")
    first["status"] = CommandStatus.in_progress
    second = self._report(1, marker="second")
    second["status"] = CommandStatus.in_progress

    self.statuses.put_command_status(command, first)
    self.statuses.put_command_status(command, second)

    self.assertEqual({1}, set(self.statuses.pending_batches))
    self.assertEqual(
      {1},
      set(
        self.initializer.server_responses_listener.listener_functions_on_success
      ),
    )

  def test_multibyte_report_size_is_measured_in_bytes(self):
    report = {"value": "\u4f60\u597d"}
    encoded_size = len(json.dumps(report).encode("utf-8"))

    self.assertTrue(self.statuses.size_approved(report, encoded_size))
    self.assertFalse(self.statuses.size_approved(report, encoded_size - 1))

  def test_command_output_fields_are_redacted_from_transport_log(self):
    message = {
      "clusters": {
        "1": [
          {
            "stdout": "stdout-secret",
            "stderr": "stderr-secret",
            "structuredOut": '{"password": "structured-secret"}',
            "status": CommandStatus.completed,
          }
        ]
      }
    }

    logged = CommandStatusDict.log_sending(message)

    self.assertEqual("...", logged["clusters"]["1"][0]["stdout"])
    self.assertEqual("...", logged["clusters"]["1"][0]["stderr"])
    self.assertEqual("...", logged["clusters"]["1"][0]["structuredOut"])
