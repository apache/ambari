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

import logging
import threading

from ambari_agent import Constants
from ambari_agent.LiveStatus import LiveStatus
from collections import defaultdict
from ambari_stomp.adapter.websocket import ConnectionIsAlreadyClosed

logger = logging.getLogger(__name__)

class ComponentStatusExecutor(threading.Thread):
  def __init__(self, initializer_module):
    self.initializer_module = initializer_module
    self.status_commands_run_interval = initializer_module.config.status_commands_run_interval
    self.metadata_cache = initializer_module.metadata_cache
    self.topology_cache = initializer_module.topology_cache
    self.customServiceOrchestrator = initializer_module.customServiceOrchestrator
    self.stop_event = initializer_module.stop_event
    self.recovery_manager = initializer_module.recovery_manager
    self.reported_component_status = defaultdict(lambda:defaultdict(lambda:defaultdict(lambda:None))) # component statuses which were received by server
    threading.Thread.__init__(self)

  def run(self):
    """
    Run an endless loop which executes all status commands every 'status_commands_run_interval' seconds.
    """
    if self.status_commands_run_interval == 0:
      logger.warn("ComponentStatusExecutor is turned off. Some functionality might not work correctly.")
      return

    while not self.stop_event.is_set():
      try:
        self.clean_not_existing_clusters_info()
        cluster_reports = defaultdict(lambda:[])

        for cluster_id in self.topology_cache.get_cluster_ids():
          # TODO: check if we can make clusters immutable too
          try:
            topology_cache = self.topology_cache[cluster_id]
            metadata_cache = self.metadata_cache[cluster_id]
          except KeyError:
            # multithreading: if cluster was deleted during iteration
            continue

          if not 'status_commands_to_run' in metadata_cache:
            continue

          status_commands_to_run = metadata_cache.status_commands_to_run

          if not 'components' in topology_cache:
            continue

          current_host_id =  self.topology_cache.get_current_host_id(cluster_id)

          if current_host_id is None:
            continue

          cluster_components = topology_cache.components
          for component_dict in cluster_components:
            for command_name in status_commands_to_run:

              if self.stop_event.is_set():
                break

              # cluster was already removed
              if not cluster_id in self.topology_cache.get_cluster_ids():
                break

              # check if component is installed on current host
              if not current_host_id in component_dict.hostIds:
                break

              service_name = component_dict.serviceName
              component_name = component_dict.componentName

              # do not run status commands for the component which is starting/stopping or doing other action
              if self.customServiceOrchestrator.commandsRunningForComponent(cluster_id, component_name):
                logger.info("Skipping status command for {0}. Since command for it is running".format(component_name))
                continue

              command_dict = {
                'serviceName': service_name,
                'role': component_name,
                'clusterId': cluster_id,
                'commandType': 'STATUS_COMMAND',
              }

              component_status_result = self.customServiceOrchestrator.requestComponentStatus(command_dict)
              status = LiveStatus.LIVE_STATUS if component_status_result['exitcode'] == 0 else LiveStatus.DEAD_STATUS

              # if exec command for component started to run after status command completion
              if self.customServiceOrchestrator.commandsRunningForComponent(cluster_id, component_name):
                logger.info("Skipped status command result for {0}. Since command for it is running".format(component_name))
                continue

              # log if status command failed
              if status == LiveStatus.DEAD_STATUS:
                stderr = component_status_result['stderr']
                if not "ComponentIsNotRunning" in stderr and not "ClientComponentHasNoStatus" in stderr:
                  logger.info("Status command for {0} failed:\n{1}".format(component_name, stderr))

              result = {
                'serviceName': service_name,
                'componentName': component_name,
                'command': command_name,
                'status': status,
                'clusterId': cluster_id,
              }

              if status != self.reported_component_status[cluster_id][component_name][command_name]:
                logging.info("Status for {0} has changed to {1}".format(component_name, status))
                cluster_reports[cluster_id].append(result)
                self.recovery_manager.handle_status_change(component_name, status)

        self.send_updates_to_server(cluster_reports)
      except ConnectionIsAlreadyClosed: # server and agent disconnected during sending data. Not an issue
        pass
      except:
        logger.exception("Exception in ComponentStatusExecutor. Re-running it")

      self.stop_event.wait(self.status_commands_run_interval)
    logger.info("ComponentStatusExecutor has successfully finished")

  def send_updates_to_server(self, cluster_reports):
    if not cluster_reports or not self.initializer_module.is_registered:
      return

    self.initializer_module.connection.send(message={'clusters': cluster_reports}, destination=Constants.COMPONENT_STATUS_REPORTS_ENDPOINT)

    for cluster_id, reports in cluster_reports.iteritems():
      for report in reports:
        component_name = report['componentName']
        command = report['command']
        status = report['status']

        self.reported_component_status[cluster_id][component_name][command] = status

  def clean_not_existing_clusters_info(self):
    """
    This needs to be done to remove information about clusters which where deleted (e.g. ambari-server reset)
    """
    for cluster_id in self.reported_component_status.keys():
      if not cluster_id in self.topology_cache.get_cluster_ids():
        del self.reported_component_status[cluster_id]
