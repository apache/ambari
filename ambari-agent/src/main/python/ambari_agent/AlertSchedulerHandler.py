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

import json
import logging
import os
import threading

from apscheduler.executors.pool import ThreadPoolExecutor
from apscheduler.schedulers.background import BackgroundScheduler
from ambari_agent.alerts.collector import AlertCollector
from ambari_agent.alerts.metric_alert import MetricAlert
from ambari_agent.alerts.port_alert import PortAlert
from ambari_agent.alerts.script_alert import ScriptAlert
from ambari_agent.alerts.web_alert import WebAlert
from ambari_agent.alerts.recovery_alert import RecoveryAlert
from ambari_agent.ExitHelper import ExitHelper
from ambari_agent.FileCache import FileCache
from ambari_agent.Utils import Utils

logger = logging.getLogger(__name__)


class AlertSchedulerHandler:
  TYPE_PORT = "PORT"
  TYPE_METRIC = "METRIC"
  TYPE_SCRIPT = "SCRIPT"
  TYPE_WEB = "WEB"
  TYPE_RECOVERY = "RECOVERY"

  def __init__(self, initializer_module, in_minutes=True):
    self.initializer_module = initializer_module
    self.cachedir = initializer_module.config.alerts_cachedir
    self.stacks_dir = initializer_module.config.stacks_dir
    self.common_services_dir = initializer_module.config.common_services_dir
    self.extensions_dir = initializer_module.config.extensions_dir
    self.host_scripts_dir = initializer_module.config.host_scripts_dir
    self.configuration_builder = initializer_module.configuration_builder

    self._cluster_configuration = initializer_module.configurations_cache
    self.alert_definitions_cache = initializer_module.alert_definitions_cache

    self.config = initializer_module.config

    # the amount of time, in seconds, that an alert can run after it's scheduled time
    alert_grace_period = int(self.config.get("agent", "alert_grace_period", 5))

    self._alert_grace_period = alert_grace_period

    self._collector = AlertCollector()
    self._scheduler_lock = threading.RLock()
    self.__scheduler = self._create_scheduler()
    self.__in_minutes = in_minutes
    self.recovery_manger = initializer_module.recovery_manager

    # register python exit handler
    ExitHelper().register(self.exit_handler)

  def _create_scheduler(self):
    return BackgroundScheduler(
      executors={"default": ThreadPoolExecutor(max_workers=3)},
      job_defaults={
        "coalesce": True,
        "misfire_grace_time": self._alert_grace_period,
      },
    )

  def exit_handler(self):
    """
    Exit handler
    """
    self.stop()

  def update_definitions(self, event_type):
    """
    Updates the persisted alert definitions JSON.
    :return:
    """
    # prune out things we don't want to store
    alert_definitions = []
    for cluster_id, command in self.alert_definitions_cache.items():
      command_copy = Utils.get_mutable_copy(command)
      alert_definitions.append(command_copy)

    if event_type == "CREATE":
      # reschedule all jobs, creating new instances
      self.reschedule_all()
    else:
      # reschedule only the jobs that have changed
      self.reschedule()

  def __make_function(self, alert_def):
    return alert_def.collect

  def start(self):
    """loads definitions from file and starts the scheduler"""
    with self._scheduler_lock:
      if self.__scheduler is not None and self.__scheduler.running:
        logger.info("[AlertScheduler] Start ignored; scheduler is already running.")
        return

      if self.__scheduler is None:
        self.__scheduler = self._create_scheduler()
      scheduler = self.__scheduler
      try:
        alert_callables = self.__load_definitions()

        for _callable in alert_callables:
          self.schedule_definition(_callable)

        logger.info(
          "[AlertScheduler] Starting {0}; currently running: {1}".format(
            str(self.__scheduler), str(self.__scheduler.running)
          )
        )
        scheduler.start()
      except Exception:
        if scheduler.running:
          scheduler.shutdown(wait=False)
        self.__scheduler = None
        logger.exception("[AlertScheduler] Failed to start the alert scheduler.")
        raise

  def stop(self):
    with self._scheduler_lock:
      scheduler = self.__scheduler
      self.__scheduler = None
      if scheduler is not None and scheduler.running:
        scheduler.shutdown(wait=True)

      logger.info("[AlertScheduler] Stopped the alert scheduler.")

  def reschedule(self):
    """
    Removes jobs that are scheduled where their UUID no longer is valid.
    Schedules jobs where the definition UUID is not currently scheduled.
    """
    with self._scheduler_lock:
      if self.__scheduler is None:
        logger.info("[AlertScheduler] Reschedule ignored; scheduler is stopped.")
        return
      self._reschedule_locked()

  def _reschedule_locked(self):
    jobs_scheduled = 0
    jobs_removed = 0

    definitions = self.__load_definitions()
    enabled_definitions = {}
    for definition in definitions:
      definition_uuid = definition.get_uuid()
      if definition.is_enabled():
        enabled_definitions[definition_uuid] = definition
      else:
        self._collector.remove_by_uuid(definition_uuid)

    scheduled_jobs = self.__scheduler.get_jobs()

    self.initializer_module.alert_status_reporter.reported_alerts.clear()

    for scheduled_job in scheduled_jobs:
      if scheduled_job.id not in enabled_definitions:
        jobs_removed += 1
        logger.info(f"[AlertScheduler] Unscheduling {scheduled_job.name}")
        self._collector.remove_by_uuid(scheduled_job.name)
        self.__scheduler.remove_job(scheduled_job.id)

    for definition_uuid, definition in enabled_definitions.items():
      jobs_scheduled += 1
      self.schedule_definition(definition)

    logger.info(
      "[AlertScheduler] Reschedule Summary: {0} rescheduled, {1} unscheduled".format(
        str(jobs_scheduled), str(jobs_removed)
      )
    )

  def reschedule_all(self):
    """
    Removes jobs that are scheduled where their UUID no longer is valid.
    Schedules jobs where the definition UUID is not currently scheduled.
    """
    with self._scheduler_lock:
      if self.__scheduler is None:
        logger.info("[AlertScheduler] Reschedule ignored; scheduler is stopped.")
        return
      self._reschedule_all_locked()

  def _reschedule_all_locked(self):
    logger.info("[AlertScheduler] Rescheduling all jobs...")

    jobs_scheduled = 0
    jobs_removed = 0

    definitions = self.__load_definitions()
    scheduled_jobs = self.__scheduler.get_jobs()

    # unschedule all scheduled jobs
    for scheduled_job in scheduled_jobs:
      jobs_removed += 1
      logger.info(f"[AlertScheduler] Unscheduling {scheduled_job.name}")
      self._collector.remove_by_uuid(scheduled_job.name)
      self.__scheduler.remove_job(scheduled_job.id)

    # for every definition, schedule a job
    for definition in definitions:
      if self.schedule_definition(definition):
        jobs_scheduled += 1

    logger.info(
      "[AlertScheduler] Reschedule Summary: {0} unscheduled, {1} rescheduled".format(
        str(jobs_removed), str(jobs_scheduled)
      )
    )

  def collector(self):
    """gets the collector for reporting to the server"""
    return self._collector

  def __load_definitions(self):
    """
    Loads all alert definitions from a file. All clusters are stored in
    a single file. This wil also populate the cluster-to-hash dictionary.
    :return:
    """
    definitions = []
    for cluster_id, command_json in self.alert_definitions_cache.items():
      clusterName = (
        "" if not "clusterName" in command_json else command_json["clusterName"]
      )
      hostName = "" if not "hostName" in command_json else command_json["hostName"]
      publicHostName = (
        "" if not "publicHostName" in command_json else command_json["publicHostName"]
      )
      clusterHash = None if not "hash" in command_json else command_json["hash"]

      # cache the cluster and cluster hash after loading the JSON
      if clusterName != "" and clusterHash is not None:
        logger.info(
          f"[AlertScheduler] Caching cluster {clusterName} with alert hash {clusterHash}"
        )

      for definition in command_json["alertDefinitions"]:
        alert = self.__json_to_callable(
          clusterName, hostName, publicHostName, Utils.get_mutable_copy(definition)
        )

        if alert is None:
          continue

        alert.set_helpers(
          self._collector, self._cluster_configuration, self.configuration_builder
        )

        definitions.append(alert)

    return definitions

  def __json_to_callable(self, clusterName, hostName, publicHostName, json_definition):
    """
    converts the json that represents all aspects of a definition
    and makes an object that extends BaseAlert that is used for individual
    """
    alert = None

    try:
      source = json_definition["source"]
      source_type = source.get("type", "")

      if logger.isEnabledFor(logging.DEBUG):
        logger.debug(
          "[AlertScheduler] Creating job type=%s name=%s uuid=%s",
          source_type,
          json_definition.get("name"),
          json_definition.get("uuid"),
        )

      if source_type == AlertSchedulerHandler.TYPE_METRIC:
        alert = MetricAlert(json_definition, source, self.config)
      elif source_type == AlertSchedulerHandler.TYPE_PORT:
        alert = PortAlert(json_definition, source, self.config)
      elif source_type == AlertSchedulerHandler.TYPE_SCRIPT:
        source["stacks_directory"] = self.stacks_dir
        source["common_services_directory"] = self.common_services_dir
        source["extensions_directory"] = self.extensions_dir
        source["host_scripts_directory"] = self.host_scripts_dir
        alert = ScriptAlert(json_definition, source, self.config)
      elif source_type == AlertSchedulerHandler.TYPE_WEB:
        alert = WebAlert(json_definition, source, self.config)
      elif source_type == AlertSchedulerHandler.TYPE_RECOVERY:
        alert = RecoveryAlert(
          json_definition, source, self.config, self.recovery_manger
        )

      if alert is not None:
        alert.set_cluster(
          clusterName, json_definition["clusterId"], hostName, publicHostName
        )

    except Exception:
      logger.exception(
        "[AlertScheduler] Unable to load an invalid alert definition. It will be skipped."
      )

    return alert

  def schedule_definition(self, definition):
    """
    Schedule a definition (callable). Scheduled jobs are given the UUID
    as their name so that they can be identified later on.
    <p/>
    This function can be called with a definition that is disabled; it will
    simply NOOP.
    """
    with self._scheduler_lock:
      if self.__scheduler is None:
        logger.info("[AlertScheduler] Schedule ignored; scheduler is stopped.")
        return False

      # NOOP if the definition is disabled; don't schedule it
      if not definition.is_enabled():
        logger.info(
          "[AlertScheduler] The alert {0} with UUID {1} is disabled and will not be scheduled".format(
            definition.get_name(), definition.get_uuid()
          )
        )
        return False

      interval = {}
      if self.__in_minutes:
        interval["minutes"] = definition.interval()
      else:
        interval["seconds"] = definition.interval()

      definition_uuid = definition.get_uuid()
      self.__scheduler.add_job(
        self.__make_function(definition),
        trigger="interval",
        id=definition_uuid,
        name=definition_uuid,
        replace_existing=True,
        **interval,
      )

      logger.info(
        "[AlertScheduler] Scheduling {0} with UUID {1}".format(
          definition.get_name(), definition.get_uuid()
        )
      )
      return True

  def get_job_count(self):
    """
    Gets the number of jobs currently scheduled. This is mainly used for
    test verification of scheduling.
    """
    with self._scheduler_lock:
      if self.__scheduler is None:
        return 0

      return len(self.__scheduler.get_jobs())

  def execute_alert(self, execution_commands):
    """
    Executes an alert immediately, ignoring any scheduled jobs. The existing
    jobs remain untouched. The result of this is stored in the alert
    collector for tranmission during the next heartbeat
    """
    if self.__scheduler is None or execution_commands is None:
      return

    for execution_command in execution_commands:
      try:
        alert_definition = execution_command["alertDefinition"]

        clusterName = (
          ""
          if not "clusterName" in execution_command
          else execution_command["clusterName"]
        )
        hostName = (
          "" if not "hostName" in execution_command else execution_command["hostName"]
        )
        publicHostName = (
          ""
          if not "publicHostName" in execution_command
          else execution_command["publicHostName"]
        )

        alert = self.__json_to_callable(
          clusterName, hostName, publicHostName, alert_definition
        )

        if alert is None:
          continue

        logger.info(
          "[AlertScheduler] Executing on-demand alert {0} ({1})".format(
            alert.get_name(), alert.get_uuid()
          )
        )

        alert.set_helpers(
          self._collector, self._cluster_configuration, self.configuration_builder
        )
        alert.collect()
      except:
        logger.exception(
          "[AlertScheduler] Unable to execute the alert outside of the job scheduler"
        )
