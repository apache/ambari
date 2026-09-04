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

import queue
from concurrent.futures import FIRST_COMPLETED, ThreadPoolExecutor, wait

import logging
import threading
import os
import json
import time
import signal
import re

from .AgentException import AgentException
from ambari_agent.BackgroundCommandExecutionHandle import (
  BackgroundCommandExecutionHandle,
)
from ambari_agent.models.commands import AgentCommand, CommandStatus
from ambari_commons.str_utils import split_on_chunks


logger = logging.getLogger()
installScriptHash = -1

MAX_SYMBOLS_PER_LOG_MESSAGE = 7900

PASSWORD_REPLACEMENT = "[PROTECTED]"
SENSITIVE_NAME_PATTERN = (
  r"(?:password|passwd|passphrase|secret|token|credential|encryption[_-]?key)"
)
QUOTED_SECRET_PATTERN = re.compile(
  r"((?:u)?(['\"])[^'\"\r\n]*"
  + SENSITIVE_NAME_PATTERN
  + r"[^'\"\r\n]*\2\s*:\s*(?:u)?)(['\"])(.+?)\3",
  re.IGNORECASE,
)
ASSIGNED_SECRET_PATTERN = re.compile(
  r"(\b[\w.-]*" + SENSITIVE_NAME_PATTERN + r"[\w.-]*\s*=\s*)([^\s,;]+)",
  re.IGNORECASE,
)
SECRET_OPTION_PATTERN = re.compile(
  r"(--?[\w-]*" + SENSITIVE_NAME_PATTERN + r"[\w-]*(?:=|\s+))([^\s]+)",
  re.IGNORECASE,
)


def hide_passwords(text):
  """Redact common credential assignments from command output."""
  if text is None:
    return None

  text = QUOTED_SECRET_PATTERN.sub(
    lambda match: "{}{}{}{}".format(
      match.group(1), match.group(3), PASSWORD_REPLACEMENT, match.group(3)
    ),
    text,
  )
  text = ASSIGNED_SECRET_PATTERN.sub(
    lambda match: match.group(1) + PASSWORD_REPLACEMENT, text
  )
  return SECRET_OPTION_PATTERN.sub(
    lambda match: match.group(1) + PASSWORD_REPLACEMENT, text
  )


class ActionQueue(threading.Thread):
  """Action Queue for the agent. We pick one command at a time from the queue
  and execute it
  Note: Action and command terms in this and related classes are used interchangeably
  """

  # How much time(in seconds) we need wait for new incoming execution command before checking status command queue
  EXECUTION_COMMAND_WAIT_TIME = 2

  def __init__(self, initializer_module):
    super(ActionQueue, self).__init__()
    self.commandQueue = queue.Queue()
    self.backgroundCommandQueue = queue.Queue()
    self.commandStatuses = initializer_module.commandStatuses
    self.config = initializer_module.config
    self.recovery_manager = initializer_module.recovery_manager
    self.configTags = {}
    self.stop_event = initializer_module.stop_event
    self.tmpdir = self.config.get("agent", "prefix")
    self.customServiceOrchestrator = initializer_module.customServiceOrchestrator
    self.parallel_execution = self.config.get_parallel_exec_option()
    self.recovery_command_lock = threading.RLock()
    self.active_recovery_task_ids = set()
    self.active_recovery_commands = {}
    self.component_status_executor = initializer_module.component_status_executor
    self.max_concurrent_actions = self.config.get_max_parallel_actions()
    self.worker_pool = None
    self.worker_futures = set()
    self.synchronous_action_count = 0
    self.command_controls = {}
    self.task_registry = {}
    self.task_generations = {}
    self.pending_cancellations = {}
    self.background_handles = set()
    if self.parallel_execution == 1:
      logger.info(
        "Parallel execution is enabled with at most %s concurrent actions",
        self.max_concurrent_actions,
      )
    self.lock = threading.RLock()

  def put(self, commands):
    commands = list(commands)
    with self.recovery_command_lock:
      if any(
        command.get("commandType") == AgentCommand.execution for command in commands
      ):
        self.preempt_recovery_commands()

      for command in commands:
        if "serviceName" not in command:
          command["serviceName"] = "null"
        if "clusterId" not in command:
          command["clusterId"] = "null"

        logger.info(
          "Adding {commandType} for role {role} for service {serviceName} of cluster_id {clusterId} to the queue".format(
            **command
          )
        )

        if command["commandType"] == AgentCommand.background_execution:
          command = self.create_command_handle(command)
          control = self._register_command(command)
          command["__handle"].control = control
          with self.lock:
            self.background_handles.add(command["__handle"])
          self.backgroundCommandQueue.put(command)
        else:
          self._register_command(command)
          self.commandQueue.put(command)

  def preempt_recovery_commands(self):
    """Stop hidden recovery work so server-issued commands can run immediately."""
    queued_recovery_commands = []
    with self.commandQueue.mutex:
      retained_commands = []
      for command in self.commandQueue.queue:
        if (
          command is not None
          and command.get("commandType") == AgentCommand.auto_execution
        ):
          queued_recovery_commands.append(command)
        else:
          retained_commands.append(command)
      self.commandQueue.queue.clear()
      self.commandQueue.queue.extend(retained_commands)

    active_recovery_commands = tuple(self.active_recovery_commands.values())
    queued_recovery_task_ids = [
      command["taskId"] for command in queued_recovery_commands
    ]
    active_recovery_task_ids = [
      command["taskId"] for command, _control in active_recovery_commands
    ]
    if queued_recovery_task_ids or active_recovery_task_ids:
      logger.info(
        "Preempting auto recovery for server-issued commands. Queued task IDs: %s; active task IDs: %s",
        queued_recovery_task_ids,
        active_recovery_task_ids,
      )

    reason = "Preempted by a server-issued command"
    for command in queued_recovery_commands:
      control = self._control_for(command)
      control["reason"] = reason
      control["state"] = "CANCELLING"
      control["cancel_event"].set()
      self._finish_control(command, control)

    for command, control in active_recovery_commands:
      with self.lock:
        control["reason"] = reason
        control["state"] = "CANCELLING"
        control["cancel_event"].set()
      self.customServiceOrchestrator.cancel_command(command["taskId"], reason)

  def has_queued_server_command(self):
    with self.commandQueue.mutex:
      return any(
        command is not None
        and command.get("commandType") == AgentCommand.execution
        for command in self.commandQueue.queue
      )

  def interrupt(self):
    self._cancel_all_tasks("Ambari Agent is stopping")
    self.commandQueue.put(None)

  @staticmethod
  def _task_key(task_id):
    return str(task_id)

  def _register_command(self, command):
    with self.lock:
      task_id = command.get("taskId")
      task_key = self._task_key(task_id)
      generation = self.task_generations.get(task_key, 0) + 1
      self.task_generations[task_key] = generation
      control = {
        "cancel_event": threading.Event(),
        "generation": generation,
        "reason": None,
        "state": "QUEUED",
        "task_id": task_id,
      }
      pending_reason = self.pending_cancellations.pop(task_key, None)
      if pending_reason is not None:
        control["reason"] = pending_reason
        control["state"] = "CANCELLING"
        control["cancel_event"].set()
      self.command_controls[id(command)] = control
      self.task_registry.setdefault(task_key, []).append(control)
    return control

  def _control_for(self, command):
    with self.lock:
      return self.command_controls.get(id(command)) or self._register_command(command)

  def _set_control_state(self, control, state):
    with self.lock:
      control["state"] = state

  def _finish_control(self, command, control):
    with self.lock:
      control["state"] = "FINISHED"
      self.command_controls.pop(id(command), None)
      task_key = self._task_key(control["task_id"])
      task_controls = self.task_registry.get(task_key, [])
      if control in task_controls:
        task_controls.remove(control)
      if not task_controls:
        self.task_registry.pop(task_key, None)
        self.task_generations.pop(task_key, None)

  def _has_newer_generation(self, task_id, current_control):
    with self.lock:
      latest_generation = self.task_generations.get(self._task_key(task_id), 0)
      return latest_generation > current_control["generation"]

  @staticmethod
  def _is_retryable(command):
    return (
      command.get("commandParams", {}).get("command_retry_enabled") == "true"
    )

  def cancel(self, commands):
    for command in commands:
      logger.info(f"Canceling command with taskId = {str(command['target_task_id'])}")

      task_id = command["target_task_id"]
      reason = command["reason"]

      with self.lock:
        controls = tuple(
          self.task_registry.get(self._task_key(task_id), ())
        )
        if not controls:
          self.pending_cancellations[self._task_key(task_id)] = reason
        for control in controls:
          control["reason"] = reason
          control["state"] = "CANCELLING"
          control["cancel_event"].set()

      self.customServiceOrchestrator.cancel_command(task_id, reason)

  def _cleanup_finished_workers(self):
    with self.lock:
      finished = {future for future in self.worker_futures if future.done()}
      self.worker_futures.difference_update(finished)
    for future in finished:
      try:
        future.result()
      except Exception:
        logger.exception("Parallel action worker failed")

  def _wait_for_workers(self):
    while True:
      with self.lock:
        active = tuple(self.worker_futures)
      if not active:
        return
      wait(active)
      self._cleanup_finished_workers()

  def _wait_for_worker_slot(self):
    """Keep the executor submission queue empty and make shutdown interruptible."""
    while not self.stop_event.is_set():
      self._cleanup_finished_workers()
      with self.lock:
        active = tuple(self.worker_futures)
      if self._running_action_count() < self.max_concurrent_actions:
        return True
      if active:
        wait(active, timeout=0.5, return_when=FIRST_COMPLETED)
      else:
        self.stop_event.wait(0.5)
    return False

  def _run_synchronous_command(self, command):
    with self.lock:
      self.synchronous_action_count += 1
    try:
      self.process_command(command)
    finally:
      with self.lock:
        self.synchronous_action_count -= 1

  def _cancel_all_tasks(self, reason):
    with self.lock:
      controls = tuple(
        control
        for task_controls in self.task_registry.values()
        for control in task_controls
        if control["state"] != "FINISHED"
      )
      for control in controls:
        control["reason"] = reason
        control["state"] = "CANCELLING"
        control["cancel_event"].set()
    self.customServiceOrchestrator.cancel_all_commands(reason)

  def _finish_queued_commands(self):
    for command_queue in (self.commandQueue, self.backgroundCommandQueue):
      while True:
        try:
          command = command_queue.get_nowait()
        except queue.Empty:
          break
        if command is None:
          continue
        control = self._control_for(command)
        control["reason"] = "Ambari Agent is stopping"
        control["cancel_event"].set()
        self.process_command(command)

  def _wait_for_background_commands(self):
    while True:
      with self.lock:
        handles = tuple(self.background_handles)
      if not handles:
        return
      for handle in handles:
        if handle.thread is not None:
          handle.thread.join()
        else:
          with self.lock:
            self.background_handles.discard(handle)

  def _running_background_command_count(self):
    with self.lock:
      return sum(
        1
        for handle in self.background_handles
        if handle.thread is not None and handle.thread.is_alive()
      )

  def _running_action_count(self):
    self._cleanup_finished_workers()
    with self.lock:
      worker_count = len(self.worker_futures)
      synchronous_count = self.synchronous_action_count
    return worker_count + synchronous_count + self._running_background_command_count()

  def run(self):
    if self.parallel_execution == 1:
      self.worker_pool = ThreadPoolExecutor(
        max_workers=self.max_concurrent_actions,
        thread_name_prefix="ambari-action",
      )
    try:
      while not self.stop_event.is_set():
        try:
          self._cleanup_finished_workers()
          self.process_background_queue_safe_empty()
          self.fill_recovery_commands()
          try:
            if self.parallel_execution == 0:
              command = self.commandQueue.get(True, self.EXECUTION_COMMAND_WAIT_TIME)
              if command is None:
                break
              if not self._wait_for_worker_slot():
                control = self._control_for(command)
                control["reason"] = "Ambari Agent is stopping"
                control["state"] = "CANCELLING"
                control["cancel_event"].set()
              self._run_synchronous_command(command)
            else:
              command = self.commandQueue.get(True, self.EXECUTION_COMMAND_WAIT_TIME)
              if command is None:
                break
              if self._is_retryable(command):
                if not self._wait_for_worker_slot():
                  control = self._control_for(command)
                  control["reason"] = "Ambari Agent is stopping"
                  control["state"] = "CANCELLING"
                  control["cancel_event"].set()
                  self.process_command(command)
                  break
                logger.info(
                  "Submitting command id=%s taskId=%s to the bounded action pool",
                  command["commandId"],
                  command["taskId"],
                )
                future = self.worker_pool.submit(self.process_command, command)
                with self.lock:
                  self.worker_futures.add(future)
              else:
                self._wait_for_workers()
                if not self._wait_for_worker_slot():
                  control = self._control_for(command)
                  control["reason"] = "Ambari Agent is stopping"
                  control["state"] = "CANCELLING"
                  control["cancel_event"].set()
                self._run_synchronous_command(command)
          except queue.Empty:
            pass
        except Exception:
          logger.exception("ActionQueue thread failed with exception. Re-running it")
    finally:
      self._cancel_all_tasks("Ambari Agent is stopping")
      self._finish_queued_commands()
      self._wait_for_workers()
      self._wait_for_background_commands()
      if self.worker_pool is not None:
        self.worker_pool.shutdown(wait=True, cancel_futures=True)
    logger.info("ActionQueue thread has successfully finished")

  def fill_recovery_commands(self):
    with self.recovery_command_lock:
      if self.recovery_manager.enabled() and not self.tasks_in_progress_or_pending():
        self.put(self.recovery_manager.get_recovery_commands())

  def process_background_queue_safe_empty(self):
    while self._running_action_count() < self.max_concurrent_actions:
      try:
        command = self.backgroundCommandQueue.get_nowait()
        if "__handle" in command and command["__handle"].status is None:
          self.process_command(command)
      except queue.Empty:
        break

  def create_command_handle(self, command):
    if "__handle" in command:
      raise AgentException("Command already has __handle")

    command["__handle"] = BackgroundCommandExecutionHandle(
      command, command["commandId"], None, self.on_background_command_complete_callback
    )
    return command

  def process_command(self, command):
    # make sure we log failures
    command_type = command["commandType"]
    control = self._control_for(command)
    self._set_control_state(
      control, "CANCELLING" if control["cancel_event"].is_set() else "RUNNING"
    )
    background_handle = command.get("__handle")
    logger.debug("Took an element of Queue (command type = %s).", command_type)
    is_recovery_command = command_type == AgentCommand.auto_execution
    skip_recovery_command = False
    if is_recovery_command:
      with self.recovery_command_lock:
        if self.has_queued_server_command():
          logger.info(
            "Skipping auto recovery task %s because a server-issued command is queued",
            command["taskId"],
          )
          control["reason"] = "Preempted by a server-issued command"
          control["state"] = "CANCELLING"
          control["cancel_event"].set()
          skip_recovery_command = True
        else:
          self.active_recovery_task_ids.add(command["taskId"])
          self.active_recovery_commands[id(command)] = (command, control)

    try:
      if skip_recovery_command:
        return
      if command_type in AgentCommand.AUTO_EXECUTION_COMMAND_GROUP:
        try:
          if self.recovery_manager.enabled():
            self.recovery_manager.on_execution_command_start()
            self.recovery_manager.process_execution_command(command)

          self.execute_command(command, control)
        finally:
          if self.recovery_manager.enabled():
            self.recovery_manager.on_execution_command_finish()
      else:
        logger.error(
          "Unrecognized command type=%s taskId=%s commandId=%s",
          command_type,
          command.get("taskId"),
          command.get("commandId"),
        )
    except Exception:
      logger.exception(f"Exception while processing {command_type} command")
    finally:
      if is_recovery_command:
        with self.recovery_command_lock:
          self.active_recovery_commands.pop(id(command), None)
          if not any(
            active_command["taskId"] == command["taskId"]
            for active_command, _control in self.active_recovery_commands.values()
          ):
            self.active_recovery_task_ids.discard(command["taskId"])

      background_started = (
        command_type == AgentCommand.background_execution
        and background_handle is not None
        and background_handle.thread is not None
      )
      if not background_started:
        self._finish_control(command, control)
        if background_handle is not None:
          with self.lock:
            self.background_handles.discard(background_handle)

  def tasks_in_progress_or_pending(self):
    with self.lock:
      has_active_tasks = any(
        control["state"] != "FINISHED"
        for controls in self.task_registry.values()
        for control in controls
      )
    return (
      not self.commandQueue.empty()
      or has_active_tasks
      or self.recovery_manager.has_active_command()
    )

  def execute_command(self, command, control=None):
    """
    Executes commands of type EXECUTION_COMMAND
    """
    cluster_id = command["clusterId"]
    command_id = command["commandId"]
    command_type = command["commandType"]

    num_attempts = 0
    retry_duration = 0  # even with 0 allow one attempt
    retry_able = False
    delay = 1
    log_command_output = True
    command_canceled = False
    status = CommandStatus.failed
    command_result = {"stdout": "", "stderr": "", "exitcode": -signal.SIGTERM}

    message = (
      "Executing command with id = {commandId}, taskId = {taskId} for role = {role} of "
      "cluster_id {cluster}.".format(
        commandId=str(command_id),
        taskId=str(command["taskId"]),
        role=command["role"],
        cluster=cluster_id,
      )
    )
    logger.info(message)

    taskId = command["taskId"]
    if control is None:
      control = self._control_for(command)
    cancel_event = control["cancel_event"]
    # Preparing 'IN_PROGRESS' report
    in_progress_status = self.commandStatuses.generate_report_template(command)
    # The path of the files that contain the output log and error log use a prefix that the agent advertises to the
    # server. The prefix is defined in agent-config.ini
    if command_type != AgentCommand.auto_execution:
      in_progress_status.update(
        {
          "tmpout": self.tmpdir + os.sep + "output-" + str(taskId) + ".txt",
          "tmperr": self.tmpdir + os.sep + "errors-" + str(taskId) + ".txt",
          "structuredOut": self.tmpdir
          + os.sep
          + "structured-out-"
          + str(taskId)
          + ".json",
          "status": CommandStatus.in_progress,
        }
      )
    else:
      in_progress_status.update(
        {
          "tmpout": self.tmpdir + os.sep + "auto_output-" + str(taskId) + ".txt",
          "tmperr": self.tmpdir + os.sep + "auto_errors-" + str(taskId) + ".txt",
          "structuredOut": self.tmpdir
          + os.sep
          + "auto_structured-out-"
          + str(taskId)
          + ".json",
          "status": CommandStatus.in_progress,
        }
      )

    self.commandStatuses.put_command_status(command, in_progress_status)

    if "commandParams" in command:
      if "max_duration_for_retries" in command["commandParams"]:
        retry_duration = int(command["commandParams"]["max_duration_for_retries"])
      if (
        "command_retry_enabled" in command["commandParams"]
        and command_type != AgentCommand.auto_execution
      ):
        #  for AgentCommand.auto_execution command retry_able should be always false
        retry_able = command["commandParams"]["command_retry_enabled"] == "true"
      if "log_output" in command["commandParams"]:
        log_command_output = command["commandParams"]["log_output"] != "false"

    logger.info(
      "Command execution metadata - taskId = {taskId}, retry enabled = {retryAble}, max retry duration (sec)"
      " = {retryDuration}, log_output = {log_command_output}".format(
        taskId=taskId,
        retryAble=retry_able,
        retryDuration=retry_duration,
        log_command_output=log_command_output,
      )
    )

    while retry_duration >= 0:
      if cancel_event.is_set() or self.stop_event.is_set():
        logger.info(f"Command with taskId = {taskId} canceled")
        command_canceled = True
        break

      num_attempts += 1
      start = 0
      if retry_able:
        start = int(time.time())
      # running command
      command_result = self.customServiceOrchestrator.runCommand(
        command,
        in_progress_status["tmpout"],
        in_progress_status["tmperr"],
        override_output_files=num_attempts == 1,
        retry=num_attempts > 1,
        cancel_event=cancel_event,
      )
      if cancel_event.is_set() or self.stop_event.is_set():
        logger.info(f"Command with taskId = {taskId} canceled during execution")
        command_canceled = True
        break
      end = 1
      if retry_able:
        end = int(time.time())
      retry_duration -= end - start

      # dumping results
      if command_type == AgentCommand.background_execution:
        logger.info(
          "Command is background command, quit retrying. Exit code: {exitCode}, retryAble: {retryAble}, retryDuration (sec): {retryDuration}, last delay (sec): {delay}".format(
            cid=taskId,
            exitCode=command_result["exitcode"],
            retryAble=retry_able,
            retryDuration=retry_duration,
            delay=delay,
          )
        )
        if command_result["exitcode"] == 777:
          return
        status = CommandStatus.failed
        break
      else:
        if command_result["exitcode"] == 0:
          status = CommandStatus.completed
        else:
          status = CommandStatus.failed
          if (command_result["exitcode"] == -signal.SIGTERM) or (
            command_result["exitcode"] == -signal.SIGKILL
          ):
            logger.info(f"Command with taskId = {taskId} was canceled!")
            command_canceled = True
            break

      if status != CommandStatus.completed and retry_able and retry_duration > 0:
        delay = self.get_retry_delay(delay)
        if delay > retry_duration:
          delay = retry_duration
        retry_duration -= delay  # allow one last attempt
        command_result["stderr"] += (
          "\n\nCommand failed. Retrying command execution ...\n\n"
        )
        logger.info(f"Retrying command with taskId = {taskId} after a wait of {delay}")
        if "agentLevelParams" not in command:
          command["agentLevelParams"] = {}

        command["agentLevelParams"]["commandBeingRetried"] = "true"
        cancel_event.wait(delay)

        continue
      else:
        logger.info(
          "Quit retrying for command with taskId = {cid}. Status: {status}, retryAble: {retryAble}, retryDuration (sec): {retryDuration}, last delay (sec): {delay}".format(
            cid=taskId,
            status=status,
            retryAble=retry_able,
            retryDuration=retry_duration,
            delay=delay,
          )
        )
        break

    # do not fail task which was rescheduled from server
    if command_canceled:
      if self._has_newer_generation(taskId, control):
        logger.info(
          "Command with taskId = %s was rescheduled by server; suppressing the "
          "failure report for the canceled generation",
          taskId,
        )
        return
      status = CommandStatus.failed
      command_result.setdefault("stdout", "")
      cancellation_reason = control.get("reason") or "Command canceled"
      command_result.setdefault("stderr", "")
      if cancellation_reason not in command_result["stderr"]:
        if command_result["stderr"]:
          command_result["stderr"] += "\n"
        command_result["stderr"] += cancellation_reason
      if command_result.get("exitcode", 0) == 0:
        command_result["exitcode"] = -signal.SIGTERM

    # final result to stdout
    command_result["stdout"] += (
      "\n\nCommand completed successfully!\n"
      if status == CommandStatus.completed
      else "\n\nCommand failed after " + str(num_attempts) + " tries\n"
    )
    logger.info(
      f"Command with taskId = {taskId} completed successfully!"
      if status == CommandStatus.completed
      else f"Command with taskId = {taskId} failed after {num_attempts} tries"
    )

    role_result = self.commandStatuses.generate_report_template(command)
    role_result.update(
      {
        "stdout": command_result["stdout"],
        "stderr": command_result["stderr"],
        "exitCode": command_result["exitcode"],
        "status": status,
      }
    )

    if (
      self.config.has_option("logging", "log_command_executes")
      and int(self.config.get("logging", "log_command_executes")) == 1
      and log_command_output
    ):
      if role_result["stdout"] != "":
        logger.info(
          "Begin command output log for command with id = "
          + str(command["taskId"])
          + ", role = "
          + command["role"]
          + ", roleCommand = "
          + command["roleCommand"]
        )
        self.log_command_output(role_result["stdout"], str(command["taskId"]))
        logger.info(
          "End command output log for command with id = "
          + str(command["taskId"])
          + ", role = "
          + command["role"]
          + ", roleCommand = "
          + command["roleCommand"]
        )

      if role_result["stderr"] != "":
        logger.info(
          "Begin command stderr log for command with id = "
          + str(command["taskId"])
          + ", role = "
          + command["role"]
          + ", roleCommand = "
          + command["roleCommand"]
        )
        self.log_command_output(role_result["stderr"], str(command["taskId"]))
        logger.info(
          "End command stderr log for command with id = "
          + str(command["taskId"])
          + ", role = "
          + command["role"]
          + ", roleCommand = "
          + command["roleCommand"]
        )

    if role_result["stdout"] == "":
      role_result["stdout"] = "None"
    if role_result["stderr"] == "":
      role_result["stderr"] = "None"

    # let ambari know name of custom command

    if "commandParams" in command and "custom_command" in command["commandParams"]:
      role_result["customCommand"] = command["commandParams"]["custom_command"]

    if "structuredOut" in command_result:
      role_result["structuredOut"] = str(json.dumps(command_result["structuredOut"]))
    else:
      role_result["structuredOut"] = ""

    self.recovery_manager.process_execution_command_result(command, status)
    self.commandStatuses.put_command_status(command, role_result)

    cluster_id = str(command["clusterId"])

    if cluster_id != "-1" and cluster_id != "null":
      service_name = command["serviceName"]
      if service_name != "null":
        component_name = command["role"]
        self.component_status_executor.check_component_status(
          cluster_id, service_name, component_name, "STATUS", report=True
        )

  def log_command_output(self, text, taskId):
    """
    Logs a message as multiple enumerated log messages every of which is not larger than MAX_SYMBOLS_PER_LOG_MESSAGE.

    If logs are redirected to syslog (syslog_enabled=1), this is very useful for logging big messages.
    As syslog usually truncates long messages.
    """
    chunks = split_on_chunks(hide_passwords(text), MAX_SYMBOLS_PER_LOG_MESSAGE)
    if len(chunks) > 1:
      for i in range(len(chunks)):
        logger.info(
          f"Cmd log for taskId={taskId} and chunk {i + 1}/{len(chunks)} of log for command: \n"
          + chunks[i]
        )
    else:
      logger.info(f"Cmd log for taskId={taskId}: " + chunks[0])

  def get_retry_delay(self, last_delay):
    """
    Returns exponentially growing delay. The idea being if number of retries is high then the reason to retry
    is probably a host or environment specific issue requiring longer waits
    """
    return last_delay * 2

  def on_background_command_complete_callback(self, process_condensed_result, handle):
    try:
      logger.debug(
        "Completing background taskId=%s pid=%s exitCode=%s",
        handle.command.get("taskId"),
        handle.pid,
        process_condensed_result.get("exitcode"),
      )
      status = CommandStatus.completed if handle.exitCode == 0 else CommandStatus.failed

      aborted_postfix = self.customServiceOrchestrator.command_canceled_reason(
        handle.command["taskId"]
      )
      if (
        not aborted_postfix
        and handle.control is not None
        and handle.control["cancel_event"].is_set()
      ):
        cancellation_reason = handle.control.get("reason") or "Command canceled"
        aborted_postfix = f"\nCommand aborted. Reason: '{cancellation_reason}'"
      if aborted_postfix:
        status = CommandStatus.failed
        logger.debug("Set status to: %s , reason = %s", status, aborted_postfix)
      else:
        aborted_postfix = ""

      if handle.control is not None and self._has_newer_generation(
        handle.command["taskId"], handle.control
      ):
        logger.info(
          "Suppressing stale background result for rescheduled taskId=%s",
          handle.command["taskId"],
        )
        return

      role_result = self.commandStatuses.generate_report_template(handle.command)
      role_result.update(
        {
          "stdout": process_condensed_result["stdout"] + aborted_postfix,
          "stderr": process_condensed_result["stderr"] + aborted_postfix,
          "exitCode": process_condensed_result["exitcode"],
          "structuredOut": str(json.dumps(process_condensed_result["structuredOut"]))
          if "structuredOut" in process_condensed_result
          else "",
          "status": status,
        }
      )

      self.commandStatuses.put_command_status(handle.command, role_result)
    finally:
      if handle.control is not None:
        self._finish_control(handle.command, handle.control)
      with self.lock:
        self.background_handles.discard(handle)
