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

import os
import logging
import threading
import copy

import json

from collections import defaultdict
from ambari_agent.Grep import Grep

from ambari_agent import Constants
from ambari_agent.models.commands import CommandStatus, AgentCommand
from ambari_agent.AmbariStompConnection import ConnectionIsAlreadyClosed

logger = logging.getLogger()


class CommandStatusDict:
  """
  Holds results for all commands that are being executed or have finished
  execution (but are not yet reported). Implementation is thread-safe.
  Dict format:
    task_id -> (command, cmd_report)
  """

  # 2MB is a max message size on the server side
  MAX_REPORT_SIZE = 1950000
  TRUNCATION_NOTICE = "...[truncated by Ambari Agent to fit the STOMP message limit]..."

  def __init__(self, initializer_module):
    """
    callback_action is called every time when status of some command is
    updated
    """
    self.current_state = {}  # Contains all statuses
    self.lock = threading.RLock()
    self.initializer_module = initializer_module
    self.command_update_output = initializer_module.config.command_update_output
    self.server_responses_listener = initializer_module.server_responses_listener
    self.log_max_symbols_size = initializer_module.config.log_max_symbols_size
    self.reported_reports = set()
    self.pending_batches = {}
    self.report_revisions = {}
    self.next_report_revision = 0

  def delete_command_data(self, key):
    # delete stale data about this command
    with self.lock:
      self.reported_reports.discard(key)
      self.current_state.pop(key, None)
      self.report_revisions.pop(key, None)

  def put_command_status(self, command, report):
    """
    Stores new version of report for command (replaces previous)
    """
    with self.lock:
      key = command["taskId"]
      # delete stale data about this command
      self.delete_command_data(key)
      self.queue_report_sending(key, command, report)

      report_dict = {command["clusterId"]: [report]}
      if report.get("status") != CommandStatus.in_progress:
        self.reported_reports.add(key)
      self.force_update_to_server(report_dict)

  def queue_report_sending(self, key, command, report):
    with self.lock:
      self.next_report_revision += 1
      self.current_state[key] = (command, report)
      self.report_revisions[key] = self.next_report_revision
      self.reported_reports.discard(key)

  def force_update_to_server(self, reports_dict, expected_revisions=None):
    if not self.initializer_module.is_registered:
      return False, None

    registered_correlation_ids = []

    def register_callbacks(correlation_id):
      report_snapshot = copy.deepcopy(reports_dict)
      superseded_correlation_ids = []
      with self.lock:
        report_revisions = {}
        for cluster_reports in report_snapshot.values():
          for report in cluster_reports:
            task_id = report.get("taskId")
            current = self.current_state.get(task_id)
            current_revision = self.report_revisions.get(task_id)
            if current is not None:
              if expected_revisions is not None:
                expected_revision = expected_revisions.get(task_id)
                if current_revision == expected_revision:
                  report_revisions[task_id] = expected_revision
              elif current[1] == report:
                report_revisions[task_id] = current_revision
        if report_revisions:
          for pending_id, pending_batch in tuple(self.pending_batches.items()):
            _, pending_revisions = pending_batch
            for task_id in report_revisions:
              if task_id in pending_revisions:
                pending_revisions.pop(task_id, None)
            if not pending_revisions:
              self.pending_batches.pop(pending_id, None)
              superseded_correlation_ids.append(pending_id)
        self.pending_batches[correlation_id] = (
          report_snapshot,
          report_revisions,
        )
      for pending_id in superseded_correlation_ids:
        self.server_responses_listener.discard_response_callback(pending_id)
      registered_correlation_ids.append(correlation_id)
      return self.server_responses_listener.register_response_callback(
        correlation_id,
        on_success=lambda headers, message, pending_id=correlation_id: self.acknowledge_batch(
          pending_id
        ),
        on_error=lambda headers, message, pending_id=correlation_id: self.discard_batch(
          pending_id
        ),
      )

    try:
      correlation_id = self.initializer_module.connection.send(
        message={"clusters": reports_dict},
        destination=Constants.COMMANDS_STATUS_REPORTS_ENDPOINT,
        log_message_function=CommandStatusDict.log_sending,
        presend_hook=register_callbacks,
      )
      return True, correlation_id
    except ConnectionIsAlreadyClosed:
      for correlation_id in registered_correlation_ids:
        self._discard_failed_send(correlation_id)
      return False, None
    except Exception:
      for correlation_id in registered_correlation_ids:
        self._discard_failed_send(correlation_id)
      raise

  def report(self):
    report = self.generate_report()

    if report:
      for splitted_report in self.split_reports(
        report, CommandStatusDict.MAX_REPORT_SIZE
      ):
        revisions = {
          item["taskId"]: self.generated_report_revisions.get(item["taskId"])
          for cluster_reports in splitted_report.values()
          for item in cluster_reports
          if item.get("taskId") in self.generated_report_revisions
        }
        self.force_update_to_server(splitted_report, revisions)

  def split_reports(self, result_reports, size):
    part = {}
    for cluster_id, cluster_reports in result_reports.items():
      for report in cluster_reports:
        candidate = copy.deepcopy(part)
        candidate.setdefault(cluster_id, []).append(report)
        if self.size_approved(candidate, size):
          part = candidate
          continue

        if part:
          yield part
          part = {}

        single_report = {cluster_id: [report]}
        if not self.size_approved(single_report, size):
          logger.warning(
            "Command status report for task %s exceeds the %s byte message limit; "
            "truncating oversized output fields",
            report.get("taskId"),
            size,
          )
          yield {cluster_id: [self.fit_single_report(cluster_id, report, size)]}
        else:
          part = single_report

    if part:
      yield part

  def size_approved(self, report, size):
    report_json = json.dumps(report).encode("utf-8")
    return len(report_json) <= size

  def fit_single_report(self, cluster_id, report, size):
    compact = copy.deepcopy(report)
    for field in ("stdout", "stderr"):
      self._truncate_text_field(cluster_id, compact, field, size, preserve_tail=True)

    if not self.size_approved({cluster_id: [compact]}, size):
      structured_out = compact.get("structuredOut")
      if isinstance(structured_out, str):
        compact["structuredOut"] = "{}"
      elif structured_out is not None:
        compact["structuredOut"] = {}

    if not self.size_approved({cluster_id: [compact]}, size):
      preserved_fields = (
        "clusterId",
        "status",
        "taskId",
        "role",
        "actionId",
        "serviceName",
        "roleCommand",
        "exitCode",
      )
      compact = {field: compact[field] for field in preserved_fields if field in compact}
      compact["stderr"] = self.TRUNCATION_NOTICE
      for field, value in tuple(compact.items()):
        if isinstance(value, str):
          self._truncate_text_field(
            cluster_id, compact, field, size, preserve_tail=False
          )

    if not self.size_approved({cluster_id: [compact]}, size):
      raise ValueError(
        f"Command status metadata for task {report.get('taskId')} exceeds {size} bytes"
      )
    return compact

  def _truncate_text_field(
    self, cluster_id, report, field, size, preserve_tail
  ):
    value = report.get(field)
    if not isinstance(value, str) or self.size_approved({cluster_id: [report]}, size):
      return

    low = 0
    high = len(value)
    best = ""
    while low <= high:
      retained_length = (low + high) // 2
      retained = value[-retained_length:] if preserve_tail and retained_length else value[:retained_length]
      candidate = self.TRUNCATION_NOTICE + retained
      report[field] = candidate
      if self.size_approved({cluster_id: [report]}, size):
        best = candidate
        low = retained_length + 1
      else:
        high = retained_length - 1
    report[field] = best

  def get_command_status(self, taskId):
    with self.lock:
      c = copy.copy(self.current_state[taskId][1])
    return c

  def generate_report(self):
    """
    Generates status reports about commands that are IN_PROGRESS, COMPLETE or
    FAILED. Statuses for COMPLETE or FAILED commands are forgotten after
    generation
    """
    self.generated_report_revisions = {}

    with self.lock:
      result_reports = defaultdict(lambda: [])
      auto_execution_keys = []
      for key, item in self.current_state.items():
        command = item[0]
        report = item[1]
        cluster_id = report["clusterId"]
        if command["commandType"] in AgentCommand.EXECUTION_COMMAND_GROUP:
          if (report["status"]) != CommandStatus.in_progress:
            result_reports[cluster_id].append(report)
            self.reported_reports.add(key)
            self.generated_report_revisions[key] = self.report_revisions.get(key)
          elif self.command_update_output:
            in_progress_report = self.generate_in_progress_report(command, report)
            result_reports[cluster_id].append(in_progress_report)
            self.generated_report_revisions[key] = self.report_revisions.get(key)
        elif command["commandType"] == AgentCommand.auto_execution:
          logger.debug("AUTO_EXECUTION_COMMAND task deleted %s", command["commandId"])
          # AUTO_EXECUTION_COMMAND is local-only and is never placed in a
          # server batch, so there can be no ACK to drive normal cleanup.
          auto_execution_keys.append(key)

      for key in auto_execution_keys:
        self.current_state.pop(key, None)
        self.report_revisions.pop(key, None)
        self.reported_reports.discard(key)
      return result_reports

  def acknowledge_batch(self, correlation_id):
    with self.lock:
      pending_batch = self.pending_batches.pop(correlation_id, None)
      if pending_batch is None:
        return
      result_reports, report_revisions = pending_batch
      self.clear_reported_reports(result_reports, report_revisions)

  def discard_batch(self, correlation_id):
    with self.lock:
      self.pending_batches.pop(correlation_id, None)

  def _discard_failed_send(self, correlation_id):
    self.discard_batch(correlation_id)
    self.server_responses_listener.discard_response_callback(correlation_id)

  def clear_pending_batches(self):
    """Release batches whose response callbacks were discarded on reconnect."""
    with self.lock:
      self.pending_batches.clear()

  def clear_reported_reports(self, result_reports, expected_revisions=None):
    with self.lock:
      keys_to_remove = set()
      reports_by_task_id = {
        report.get("taskId"): report
        for cluster_reports in result_reports.values()
        for report in cluster_reports
      }
      for key in tuple(self.reported_reports):
        current = self.current_state.get(key)
        if expected_revisions is not None:
          current_matches = current is not None and self.report_revisions.get(
            key
          ) == expected_revisions.get(key)
        else:
          current_matches = current is not None and current[1] == reports_by_task_id.get(
            key
          )
        if current_matches:
          del self.current_state[key]
          self.report_revisions.pop(key, None)
          keys_to_remove.add(key)

      self.reported_reports = self.reported_reports.difference(keys_to_remove)

  def generate_in_progress_report(self, command, report):
    """
    Reads stdout/stderr for IN_PROGRESS command from disk file
    and populates other fields of report.
    """
    files_to_read = [report["tmpout"], report["tmperr"], report["structuredOut"]]
    files_content = ["...", "...", "{}"]

    for i in range(len(files_to_read)):
      filename = files_to_read[i]
      if os.path.exists(filename):
        with open(filename, "r") as fp:
          files_content[i] = fp.read()

    tmpout, tmperr, tmpstructuredout = files_content

    grep = Grep()
    output = grep.tail_by_symbols(
      grep.tail(tmpout, Grep.OUTPUT_LAST_LINES), self.log_max_symbols_size
    )
    err = grep.tail_by_symbols(
      grep.tail(tmperr, Grep.OUTPUT_LAST_LINES), self.log_max_symbols_size
    )
    inprogress = self.generate_report_template(command)
    inprogress.update(
      {
        "stdout": output,
        "stderr": err,
        "structuredOut": tmpstructuredout,
        "exitCode": 777,
        "status": CommandStatus.in_progress,
      }
    )
    return inprogress

  def generate_report_template(self, command):
    """
    Generates stub dict for command.
    Other fields should be populated manually
    """
    stub = {
      "role": command["role"],
      "actionId": command["commandId"],
      "taskId": command["taskId"],
      "clusterId": command["clusterId"],
      "serviceName": command["serviceName"],
      "roleCommand": command["roleCommand"],
    }
    return stub

  @staticmethod
  def log_sending(message_dict):
    """
    Returned dictionary will be used while logging sent component status.
    Used because full dict is too big for logs and should be shortened
    """
    try:
      for cluster_id in message_dict["clusters"]:
        for command_status in message_dict["clusters"][cluster_id]:
          for output_field in ("stdout", "stderr", "structuredOut"):
            if output_field in command_status:
              command_status[output_field] = "..."
    except KeyError:
      pass

    return message_dict
