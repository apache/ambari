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

import threading
import logging
import os

from ambari_agent.FileCache import FileCache
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.ClusterConfigurationCache import ClusterConfigurationCache
from ambari_agent.ClusterTopologyCache import ClusterTopologyCache
from ambari_agent.ClusterMetadataCache import ClusterMetadataCache
from ambari_agent.ClusterHostLevelParamsCache import ClusterHostLevelParamsCache
from ambari_agent.ClusterAlertDefinitionsCache import ClusterAlertDefinitionsCache
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.CommandStatusDict import CommandStatusDict
from ambari_agent.CustomServiceOrchestrator import CustomServiceOrchestrator
from ambari_agent.RecoveryManager import RecoveryManager
from ambari_agent.AlertSchedulerHandler import AlertSchedulerHandler
from ambari_stomp.adapter.websocket import ConnectionIsAlreadyClosed

logger = logging.getLogger(__name__)

class InitializerModule:
  """
  - Instantiate some singleton classes or widely used instances along with providing their dependencies.
  - Reduce cross modules dependencies.
  - Make other components code cleaner.
  - Provide an easier way to mock some dependencies.
  """
  def __init__(self):
    self.initConfigs()
    self.init()

  def initConfigs(self):
    """
    Initialize every property got from ambari-agent.ini
    """
    self.config = AmbariConfig.get_resolved_config()

    self.server_hostname = self.config.get('server', 'hostname')
    self.secured_url_port = self.config.get('server', 'secured_url_port')

    self.cache_dir = self.config.get('agent', 'cache_dir', default='/var/lib/ambari-agent/cache')
    self.command_reports_interval = int(self.config.get('agent', 'command_reports_interval', default='5'))
    self.alert_reports_interval = int(self.config.get('agent', 'alert_reports_interval', default='5'))

    self.cluster_cache_dir = os.path.join(self.cache_dir, FileCache.CLUSTER_CACHE_DIRECTORY)
    self.recovery_cache_dir = os.path.join(self.cache_dir, FileCache.RECOVERY_CACHE_DIRECTORY)
    self.alerts_cachedir = os.path.join(self.cache_dir, FileCache.ALERTS_CACHE_DIRECTORY)
    self.stacks_dir = os.path.join(self.cache_dir, FileCache.STACKS_CACHE_DIRECTORY)
    self.common_services_dir = os.path.join(self.cache_dir, FileCache.COMMON_SERVICES_DIRECTORY)
    self.extensions_dir = os.path.join(self.cache_dir, FileCache.EXTENSIONS_CACHE_DIRECTORY)
    self.host_scripts_dir = os.path.join(self.cache_dir, FileCache.HOST_SCRIPTS_CACHE_DIRECTORY)

    self.host_status_report_interval = int(self.config.get('heartbeat', 'state_interval_seconds', '60'))

  def init(self):
    """
    Initialize properties
    """
    self.stop_event = threading.Event()

    self.is_registered = False

    self.metadata_cache = ClusterMetadataCache(self.cluster_cache_dir)
    self.topology_cache = ClusterTopologyCache(self.cluster_cache_dir, self.config)
    self.configurations_cache = ClusterConfigurationCache(self.cluster_cache_dir)
    self.host_level_params_cache = ClusterHostLevelParamsCache(self.cluster_cache_dir)
    self.alert_definitions_cache = ClusterAlertDefinitionsCache(self.cluster_cache_dir)

    self.file_cache = FileCache(self.config)

    self.customServiceOrchestrator = CustomServiceOrchestrator(self)

    self.recovery_manager = RecoveryManager(self.recovery_cache_dir)
    self.commandStatuses = CommandStatusDict(self)
    self.action_queue = ActionQueue(self)
    self.alert_scheduler_handler = AlertSchedulerHandler(self)

  @property
  def connection(self):
    try:
      return self._connection
    except AttributeError:
      """
      Can be a result of race condition:
      begin sending X -> got disconnected by HeartbeatThread -> continue sending X
      """
      raise ConnectionIsAlreadyClosed("Connection to server is not established")
