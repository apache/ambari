#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import json
import logging
import copy
import os
import time
import threading
import pprint

from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.LiveStatus import LiveStatus


logger = logging.getLogger()

"""
RecoveryManager has the following capabilities:
* Store data needed for execution commands extracted from STATUS command
* Generate INSTALL command
* Generate START command
"""


class RecoveryManager:
  COMMAND_TYPE = "commandType"
  PAYLOAD_LEVEL = "payloadLevel"
  COMPONENT_NAME = "componentName"
  ROLE = "role"
  TASK_ID = "taskId"
  CLUSTER_ID = "clusterId"
  DESIRED_STATE = "desiredState"
  HAS_STALE_CONFIG = "hasStaleConfigs"
  EXECUTION_COMMAND_DETAILS = "executionCommandDetails"
  ROLE_COMMAND = "roleCommand"
  COMMAND_ID = "commandId"
  PAYLOAD_LEVEL_DEFAULT = "DEFAULT"
  PAYLOAD_LEVEL_MINIMAL = "MINIMAL"
  PAYLOAD_LEVEL_EXECUTION_COMMAND = "EXECUTION_COMMAND"
  STARTED = "STARTED"
  INSTALLED = "INSTALLED"
  INIT = "INIT"  # TODO: What is the state when machine is reset
  INSTALL_FAILED = "INSTALL_FAILED"
  COMPONENT_UPDATE_KEY_FORMAT = "{0}_UPDATE_TIME"
  COMMAND_REFRESH_DELAY_SEC = 600 #10 minutes

  FILENAME = "recovery.json"

  default_action_counter = {
    "lastAttempt": 0,
    "count": 0,
    "lastReset": 0,
    "lifetimeCount": 0,
    "warnedLastAttempt": False,
    "warnedLastReset": False,
    "warnedThresholdReached": False
  }

  default_component_status = {
    "current": "",
    "desired": "",
    "stale_config": False
  }

  def __init__(self, cache_dir, recovery_enabled=False, auto_start_only=False, auto_install_start=False):
    self.recovery_enabled = recovery_enabled
    self.auto_start_only = auto_start_only
    self.auto_install_start = auto_install_start
    self.max_count = 6
    self.window_in_min = 60
    self.retry_gap = 5
    self.max_lifetime_count = 12

    self.id = int(time.time())
    self.allowed_desired_states = [self.STARTED, self.INSTALLED]
    self.allowed_current_states = [self.INIT, self.INSTALLED]
    self.enabled_components = []
    self.statuses = {}
    self.__status_lock = threading.RLock()
    self.__command_lock = threading.RLock()
    self.__active_command_lock = threading.RLock()
    self.__cache_lock = threading.RLock()
    self.active_command_count = 0
    self.cluster_id = None

    if not os.path.exists(cache_dir):
      try:
        os.makedirs(cache_dir)
      except:
        logger.critical("[RecoveryManager] Could not create the cache directory {0}".format(cache_dir))

    self.__actions_json_file = os.path.join(cache_dir, self.FILENAME)

    self.actions = {}

    self.update_config(6, 60, 5, 12, recovery_enabled, auto_start_only, auto_install_start, "")

    pass

  def on_execution_command_start(self):
    with self.__active_command_lock:
      self.active_command_count += 1
    pass

  def on_execution_command_finish(self):
    with self.__active_command_lock:
      self.active_command_count -= 1
    pass

  def has_active_command(self):
    return self.active_command_count > 0

  def enabled(self):
    return self.recovery_enabled

  def get_current_status(self, component):
    if component in self.statuses:
      return self.statuses[component]["current"]
    pass

  def get_desired_status(self, component):
    if component in self.statuses:
      return self.statuses[component]["desired"]
    pass

  def update_config_staleness(self, component, is_config_stale):
    """
    Updates staleness of config
    """
    if component not in self.statuses:
      self.__status_lock.acquire()
      try:
        if component not in self.statuses:
          component_status = copy.deepcopy(self.default_component_status)
          component_status["stale_config"] = is_config_stale
          self.statuses[component] = component_status
      finally:
        self.__status_lock.release()
      pass

    self.statuses[component]["stale_config"] = is_config_stale
    pass

  def handle_status_change(self, component, component_status):
    if not self.enabled() or not self.configured_for_recovery(component):
      return

    if component_status == LiveStatus.LIVE_STATUS:
        self.update_current_status(component, component_status)
    else:
        if (self.get_current_status(component) != self.INSTALL_FAILED):
          self.update_current_status(component, component_status)

  def update_current_status(self, component, state):
    """
    Updates the current status of a host component managed by the agent
    """
    if component not in self.statuses:
      self.__status_lock.acquire()
      try:
        if component not in self.statuses:
          component_status = copy.deepcopy(self.default_component_status)
          component_status["current"] = state
          self.statuses[component] = component_status
          logger.info("New status, current status is set to %s for %s", self.statuses[component]["current"], component)
      finally:
        self.__status_lock.release()
      pass

    if self.statuses[component]["current"] != state:
      logger.info("current status is set to %s for %s", state, component)
    self.statuses[component]["current"] = state
    pass


  def update_desired_status(self, component, state):
    """
    Updates the desired status of a host component managed by the agent
    """
    if component not in self.statuses:
      self.__status_lock.acquire()
      try:
        if component not in self.statuses:
          component_status = copy.deepcopy(self.default_component_status)
          component_status["desired"] = state
          self.statuses[component] = component_status
          logger.info("New status, desired status is set to %s for %s", self.statuses[component]["desired"], component)
      finally:
        self.__status_lock.release()
      pass

    if self.statuses[component]["desired"] != state:
      logger.info("desired status is set to %s for %s", state, component)
    self.statuses[component]["desired"] = state
    pass

  """
  Whether specific components are enabled for recovery.
  """
  def configured_for_recovery(self, component):
    if len(self.enabled_components) > 0 and component in self.enabled_components:
      return True

    return False

  def requires_recovery(self, component):
    """
    Recovery is allowed for:
    INISTALLED --> STARTED
    INIT --> INSTALLED --> STARTED
    RE-INSTALLED (if configs do not match)
    """
    if not self.enabled():
      return False

    if not self.configured_for_recovery(component):
      return False

    if component not in self.statuses:
      return False

    status = self.statuses[component]
    if self.auto_start_only or self.auto_install_start:
      if status["current"] == status["desired"]:
        return False
      if status["desired"] not in self.allowed_desired_states:
        return False
    else:
      if status["current"] == status["desired"] and status['stale_config'] == False:
        return False

    if status["desired"] not in self.allowed_desired_states or status["current"] not in self.allowed_current_states:
      return False

    logger.info("%s needs recovery, desired = %s, and current = %s.", component, status["desired"], status["current"])
    return True
    pass



  def get_recovery_status(self):
    """
    Creates a status in the form
    {
      "summary" : "RECOVERABLE|DISABLED|PARTIALLY_RECOVERABLE|UNRECOVERABLE",
      "component_reports" : [
        {
          "name": "component_name",
          "numAttempts" : "x",
          "limitReached" : "true|false"
          "status" : "REQUIRES_RECOVERY|RECOVERY_COMMAND_REQUESTED|RECOVERY_COMMAND_ISSUED|NO_RECOVERY_NEEDED"
        }
      ]
    }
    """
    report = {}
    report["summary"] = "DISABLED"
    if self.enabled():
      report["summary"] = "RECOVERABLE"
      num_limits_reached = 0
      recovery_states = []
      report["componentReports"] = recovery_states
      self.__status_lock.acquire()
      try:
        for component in self.actions.keys():
          action = self.actions[component]
          recovery_state = {}
          recovery_state["name"] = component
          recovery_state["numAttempts"] = action["lifetimeCount"]
          recovery_state["limitReached"] = self.max_lifetime_count <= action["lifetimeCount"]
          recovery_states.append(recovery_state)
          if recovery_state["limitReached"] == True:
            num_limits_reached += 1
          pass
      finally:
        self.__status_lock.release()

      if num_limits_reached > 0:
        report["summary"] = "PARTIALLY_RECOVERABLE"
        if num_limits_reached == len(recovery_states):
          report["summary"] = "UNRECOVERABLE"

    return report
    pass

  def get_recovery_commands(self):
    """
    This method computes the recovery commands for the following transitions
    INSTALLED --> STARTED
    INIT --> INSTALLED
    INSTALLED_FAILED --> INSTALLED
    INSTALLED_FAILED --> STARTED
    """
    commands = []
    for component in self.statuses.keys():
      if self.requires_recovery(component) and self.may_execute(component):
        status = copy.deepcopy(self.statuses[component])
        command = None
        if self.auto_start_only:
          if status["desired"] == self.STARTED:
            if status["current"] == self.INSTALLED:
              command = self.get_start_command(component)
        elif self.auto_install_start:
          if status["desired"] == self.STARTED:
            if status["current"] == self.INSTALLED:
              command = self.get_start_command(component)
            elif status["current"] == self.INSTALL_FAILED:
              command = self.get_install_command(component)
          elif status["desired"] == self.INSTALLED:
            if status["current"] == self.INSTALL_FAILED:
              command = self.get_install_command(component)
        else:
          # START, INSTALL, RESTART
          if status["desired"] != status["current"]:
            if status["desired"] == self.STARTED:
              if status["current"] == self.INSTALLED:
                command = self.get_start_command(component)
              elif status["current"] == self.INIT:
                command = self.get_install_command(component)
              elif status["current"] == self.INSTALL_FAILED:
                command = self.get_install_command(component)
            elif status["desired"] == self.INSTALLED:
              if status["current"] == self.INIT:
                command = self.get_install_command(component)
              elif status["current"] == self.INSTALL_FAILED:
                command = self.get_install_command(component)
              elif status["current"] == self.STARTED:
                command = self.get_stop_command(component)
          else:
            if status["current"] == self.INSTALLED:
              command = self.get_install_command(component)
            elif status["current"] == self.STARTED:
              command = self.get_restart_command(component)

        if command:
          self.execute(component)
          logger.info("Created recovery command %s for component %s",
                    command[self.ROLE_COMMAND], command[self.ROLE])
          commands.append(command)
    return commands


  def may_execute(self, action):
    """
    Check if an action can be executed
    """
    if not action or action.strip() == "":
      return False

    if action not in self.actions:
      self.__status_lock.acquire()
      try:
        self.actions[action] = copy.deepcopy(self.default_action_counter)
      finally:
        self.__status_lock.release()
    return self._execute_action_chk_only(action)
    pass


  def execute(self, action):
    """
    Executed an action
    """
    if not action or action.strip() == "":
      return False

    if action not in self.actions:
      self.__status_lock.acquire()
      try:
        self.actions[action] = copy.deepcopy(self.default_action_counter)
      finally:
        self.__status_lock.release()
    return self._execute_action_(action)
    pass


  def _execute_action_(self, action_name):
    """
    _private_ implementation of [may] execute
    """
    action_counter = self.actions[action_name]
    now = self._now_()
    executed = False
    seconds_since_last_attempt = now - action_counter["lastAttempt"]
    if action_counter["lifetimeCount"] < self.max_lifetime_count:
      #reset if window_in_sec seconds passed since last attempt
      if seconds_since_last_attempt > self.window_in_sec:
        action_counter["count"] = 0
        action_counter["lastReset"] = now
        action_counter["warnedLastReset"] = False
      if action_counter["count"] < self.max_count:
        if seconds_since_last_attempt > self.retry_gap_in_sec:
          action_counter["count"] += 1
          action_counter["lifetimeCount"] +=1
          if self.retry_gap > 0:
            action_counter["lastAttempt"] = now
          action_counter["warnedLastAttempt"] = False
          if action_counter["count"] == 1:
            action_counter["lastReset"] = now
          executed = True
        else:
          if action_counter["warnedLastAttempt"] == False:
            action_counter["warnedLastAttempt"] = True
            logger.warn(
              "%s seconds has not passed since last occurrence %s seconds back for %s. " +
              "Will silently skip execution without warning till retry gap is passed",
              self.retry_gap_in_sec, seconds_since_last_attempt, action_name)
          else:
            logger.debug(
              "%s seconds has not passed since last occurrence %s seconds back for %s",
              self.retry_gap_in_sec, seconds_since_last_attempt, action_name)
      else:
        sec_since_last_reset = now - action_counter["lastReset"]
        if sec_since_last_reset > self.window_in_sec:
          action_counter["count"] = 1
          action_counter["lifetimeCount"] +=1
          if self.retry_gap > 0:
            action_counter["lastAttempt"] = now
          action_counter["lastReset"] = now
          action_counter["warnedLastReset"] = False
          executed = True
        else:
          if action_counter["warnedLastReset"] == False:
            action_counter["warnedLastReset"] = True
            logger.warn("%s occurrences in %s minutes reached the limit for %s. " +
                        "Will silently skip execution without warning till window is reset",
                        action_counter["count"], self.window_in_min, action_name)
          else:
            logger.debug("%s occurrences in %s minutes reached the limit for %s",
                         action_counter["count"], self.window_in_min, action_name)
    else:
      if action_counter["warnedThresholdReached"] == False:
        action_counter["warnedThresholdReached"] = True
        logger.warn("%s occurrences in agent life time reached the limit for %s. " +
                    "Will silently skip execution without warning till window is reset",
                    action_counter["lifetimeCount"], action_name)
      else:
        logger.error("%s occurrences in agent life time reached the limit for %s",
                     action_counter["lifetimeCount"], action_name)
    self._dump_actions()
    return executed
    pass


  def _dump_actions(self):
    """
    Dump recovery actions to FS
    """
    self.__cache_lock.acquire()
    try:
      with open(self.__actions_json_file, 'w') as f:
        json.dump(self.actions, f, indent=2)
    except Exception, exception:
      logger.exception("Unable to dump actions to {0}".format(self.__actions_json_file))
      return False
    finally:
      self.__cache_lock.release()

    return True
    pass


  def _load_actions(self):
    """
    Loads recovery actions from FS
    """
    self.__cache_lock.acquire()

    try:
      if os.path.isfile(self.__actions_json_file):
        with open(self.__actions_json_file, 'r') as fp:
          return json.load(fp)
    except Exception, exception:
      logger.warning("Unable to load recovery actions from {0}.".format(self.__actions_json_file))
    finally:
      self.__cache_lock.release()

    return {}
    pass


  def get_actions_copy(self):
    """
    :return:  recovery actions copy
    """
    self.__status_lock.acquire()
    try:
      return copy.deepcopy(self.actions)
    finally:
      self.__status_lock.release()
    pass


  def is_action_info_stale(self, action_name):
    """
    Checks if the action info is stale
    :param action_name:
    :return: if the action info for action_name: is stale
    """
    if action_name in self.actions:
      action_counter = self.actions[action_name]
      now = self._now_()
      seconds_since_last_attempt = now - action_counter["lastAttempt"]
      return seconds_since_last_attempt > self.window_in_sec
    return False
    pass

  def _execute_action_chk_only(self, action_name):
    """
    _private_ implementation of [may] execute check only
    """
    action_counter = self.actions[action_name]
    now = self._now_()
    seconds_since_last_attempt = now - action_counter["lastAttempt"]

    if action_counter["lifetimeCount"] < self.max_lifetime_count:
      if action_counter["count"] < self.max_count:
        if seconds_since_last_attempt > self.retry_gap_in_sec:
          return True
      else:
        sec_since_last_reset = now - action_counter["lastReset"]
        if sec_since_last_reset > self.window_in_sec:
          return True

    return False
    pass

  def _now_(self):
    return int(time.time())
    pass


  def update_recovery_config(self, dictionary):
    """
    TODO: Server sends the recovery configuration - call update_config after parsing
    "recoveryConfig": {
      "type" : "DEFAULT|AUTO_START|AUTO_INSTALL_START|FULL",
      "maxCount" : 10,
      "windowInMinutes" : 60,
      "retryGap" : 0,
      "components" : "a,b"
      }
    """

    recovery_enabled = False
    auto_start_only = False
    auto_install_start = False
    max_count = 6
    window_in_min = 60
    retry_gap = 5
    max_lifetime_count = 12
    enabled_components = ""


    if dictionary and "recoveryConfig" in dictionary:
      if logger.isEnabledFor(logging.INFO):
        logger.info("RecoverConfig = %s", pprint.pformat(dictionary["recoveryConfig"]))
      config = dictionary["recoveryConfig"]
      if "type" in config:
        if config["type"] in ["AUTO_INSTALL_START", "AUTO_START", "FULL"]:
          recovery_enabled = True
          if config["type"] == "AUTO_START":
            auto_start_only = True
          elif config["type"] == "AUTO_INSTALL_START":
            auto_install_start = True

      if "maxCount" in config:
        max_count = self._read_int_(config["maxCount"], max_count)
      if "windowInMinutes" in config:
        window_in_min = self._read_int_(config["windowInMinutes"], window_in_min)
      if "retryGap" in config:
        retry_gap = self._read_int_(config["retryGap"], retry_gap)
      if 'maxLifetimeCount' in config:
        max_lifetime_count = self._read_int_(config['maxLifetimeCount'], max_lifetime_count)

      if 'components' in config:
        enabled_components = config['components']

    self.update_config(max_count, window_in_min, retry_gap, max_lifetime_count, recovery_enabled, auto_start_only,
                       auto_install_start, enabled_components)
    pass

  """
  Update recovery configuration with the specified values.

  max_count - Configured maximum count of recovery attempt allowed per host component in a window.
  window_in_min - Configured window size in minutes.
  retry_gap - Configured retry gap between tries per host component
  max_lifetime_count - Configured maximum lifetime count of recovery attempt allowed per host component.
  recovery_enabled - True or False. Indicates whether recovery is enabled or not.
  auto_start_only - True if AUTO_START recovery type was specified. False otherwise.
  auto_install_start - True if AUTO_INSTALL_START recovery type was specified. False otherwise.
  enabled_components - CSV of componenents enabled for auto start.
  """
  def update_config(self, max_count, window_in_min, retry_gap, max_lifetime_count, recovery_enabled,
                    auto_start_only, auto_install_start, enabled_components):
    """
    Update recovery configuration, recovery is disabled if configuration values
    are not correct
    """
    self.recovery_enabled = False;
    if max_count <= 0:
      logger.warn("Recovery disabled: max_count must be a non-negative number")
      return

    if window_in_min <= 0:
      logger.warn("Recovery disabled: window_in_min must be a non-negative number")
      return

    if retry_gap < 1:
      logger.warn("Recovery disabled: retry_gap must be a positive number and at least 1")
      return
    if retry_gap >= window_in_min:
      logger.warn("Recovery disabled: retry_gap must be smaller than window_in_min")
      return
    if max_lifetime_count < 0 or max_lifetime_count < max_count:
      logger.warn("Recovery disabled: max_lifetime_count must more than 0 and >= max_count")
      return

    self.max_count = max_count
    self.window_in_min = window_in_min
    self.retry_gap = retry_gap
    self.window_in_sec = window_in_min * 60
    self.retry_gap_in_sec = retry_gap * 60
    self.auto_start_only = auto_start_only
    self.auto_install_start = auto_install_start
    self.max_lifetime_count = max_lifetime_count
    self.enabled_components = []

    self.allowed_desired_states = [self.STARTED, self.INSTALLED]
    self.allowed_current_states = [self.INIT, self.INSTALL_FAILED, self.INSTALLED, self.STARTED]

    if self.auto_start_only:
      self.allowed_desired_states = [self.STARTED]
      self.allowed_current_states = [self.INSTALLED]
    elif self.auto_install_start:
      self.allowed_desired_states = [self.INSTALLED, self.STARTED]
      self.allowed_current_states = [self.INSTALL_FAILED, self.INSTALLED]

    if enabled_components is not None and len(enabled_components) > 0:
      components = enabled_components.split(",")
      for component in components:
        if len(component.strip()) > 0:
          self.enabled_components.append(component.strip())

    self.recovery_enabled = recovery_enabled
    if self.recovery_enabled:
      logger.info(
        "==> Auto recovery is enabled with maximum %s in %s minutes with gap of %s minutes between and"
        " lifetime max being %s. Enabled components - %s",
        self.max_count, self.window_in_min, self.retry_gap, self.max_lifetime_count,
        ', '.join(self.enabled_components))
    pass


  def get_unique_task_id(self):
    self.id += 1
    return self.id

  def process_execution_command_result(self, command, status):
    """
    Update current status for the components depending on command and its status.
    """
    if not self.enabled():
      return

    if not command.has_key(self.ROLE_COMMAND) or not self.configured_for_recovery(command['role']):
      return

    if status == ActionQueue.COMPLETED_STATUS:
      if command[self.ROLE_COMMAND] == ActionQueue.ROLE_COMMAND_START:
        self.update_current_status(command[self.ROLE], LiveStatus.LIVE_STATUS)
        #self.update_config_staleness(command['role'], False)
        logger.info("After EXECUTION_COMMAND (START), with taskId=" + str(command['taskId']) +
                    ", current state of " + command[self.ROLE] + " to " +
                     self.get_current_status(command[self.ROLE]) )
      elif command['roleCommand'] == ActionQueue.ROLE_COMMAND_STOP or command[self.ROLE_COMMAND] == ActionQueue.ROLE_COMMAND_INSTALL:
        self.update_current_status(command[self.ROLE], LiveStatus.DEAD_STATUS)
        logger.info("After EXECUTION_COMMAND (STOP/INSTALL), with taskId=" + str(command['taskId']) +
                    ", current state of " + command[self.ROLE] + " to " +
                     self.get_current_status(command[self.ROLE]) )
      elif command[self.ROLE_COMMAND] == ActionQueue.ROLE_COMMAND_CUSTOM_COMMAND:
        if command.has_key('custom_command') and command['custom_command'] == ActionQueue.CUSTOM_COMMAND_RESTART:
          self.update_current_status(command['role'], LiveStatus.LIVE_STATUS)
          #self.update_config_staleness(command['role'], False)
          logger.info("After EXECUTION_COMMAND (RESTART), current state of " + command[self.ROLE] + " to " +
                       self.get_current_status(command[self.ROLE]) )
    elif status == ActionQueue.FAILED_STATUS:
      if command[self.ROLE_COMMAND] == ActionQueue.ROLE_COMMAND_INSTALL:
        self.update_current_status(command[self.ROLE], self.INSTALL_FAILED)
        logger.info("After EXECUTION_COMMAND (INSTALL), with taskId=" + str(command['taskId']) +
                    ", current state of " + command[self.ROLE] + " to " +
                    self.get_current_status(command[self.ROLE]))

  def process_execution_command(self, command):
    """
    Change desired state of the component depending on the execution command triggered.
    """
    if not self.enabled():
      return

    if not self.COMMAND_TYPE in command or not command[self.COMMAND_TYPE] == ActionQueue.EXECUTION_COMMAND:
      return

    if not self.ROLE in command:
      return

    if command[self.ROLE_COMMAND] in (ActionQueue.ROLE_COMMAND_INSTALL, ActionQueue.ROLE_COMMAND_STOP) \
        and self.configured_for_recovery(command[self.ROLE]):
      self.update_desired_status(command[self.ROLE], LiveStatus.DEAD_STATUS)
      logger.info("Received EXECUTION_COMMAND (STOP/INSTALL), desired state of " + command[self.ROLE] + " to " +
                   self.get_desired_status(command[self.ROLE]) )
    elif command[self.ROLE_COMMAND] == ActionQueue.ROLE_COMMAND_START \
        and self.configured_for_recovery(command[self.ROLE]):
      self.update_desired_status(command[self.ROLE], LiveStatus.LIVE_STATUS)
      logger.info("Received EXECUTION_COMMAND (START), desired state of " + command[self.ROLE] + " to " +
                   self.get_desired_status(command[self.ROLE]) )
    elif command.has_key('custom_command') and \
            command['custom_command'] == ActionQueue.CUSTOM_COMMAND_RESTART \
            and self.configured_for_recovery(command[self.ROLE]):
      self.update_desired_status(command[self.ROLE], LiveStatus.LIVE_STATUS)
      logger.info("Received EXECUTION_COMMAND (RESTART), desired state of " + command[self.ROLE] + " to " +
                   self.get_desired_status(command[self.ROLE]) )

  def get_command(self, component, command_name):
    """
    Get command dictionary by component name and command_name
    """
    if self.has_active_command():
      logger.info("Recovery is paused, tasks waiting in pipeline for this host.")
      return None

    if self.enabled():
      command_id = self.get_unique_task_id()
      command = {
        self.CLUSTER_ID: self.cluster_id,
        self.ROLE_COMMAND: command_name,
        self.COMMAND_TYPE: ActionQueue.AUTO_EXECUTION_COMMAND,
        self.TASK_ID: command_id,
        self.ROLE: component,
        self.COMMAND_ID: command_id
      }
      return command
    else:
      logger.info("Recovery is not enabled. START command will not be computed.")

    return None

  def get_restart_command(self, component):
    command = self.get_command(component, "CUSTOM_COMMAND")

    if command is not None:
      command[self.ROLE_COMMAND] = "CUSTOM_COMMAND"
      command['custom_command'] = 'RESTART'

    return command

  def get_install_command(self, component):
    return self.get_command(component, "INSTALL")

  def get_stop_command(self, component):
    return self.get_command(component, "STOP")

  def get_start_command(self, component):
    return self.get_command(component, "START")

  def _read_int_(self, value, default_value=0):
    int_value = default_value
    try:
      int_value = int(value)
    except ValueError:
      pass
    return int_value


def main(argv=None):
  cmd_mgr = RecoveryManager('/tmp')
  pass


if __name__ == '__main__':
  main()
