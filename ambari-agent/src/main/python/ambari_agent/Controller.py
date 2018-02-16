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

import multiprocessing
import logging
import ambari_simplejson as json
import sys
import os
import socket
import time
import threading
import urllib2
import pprint
from random import randint
import re
from ambari_commons import subprocess32
import functools

import hostname
import security
import ssl
import AmbariConfig

from ambari_agent.Heartbeat import Heartbeat
from ambari_agent.Register import Register
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.FileCache import FileCache
from ambari_agent.NetUtil import NetUtil
from ambari_agent.LiveStatus import LiveStatus
from ambari_agent.AlertSchedulerHandler import AlertSchedulerHandler
from ambari_agent.ClusterConfigurationCache import  ClusterConfigurationCache
from ambari_agent.ClusterTopologyCache import ClusterTopologyCache
from ambari_agent.RecoveryManager import  RecoveryManager
from ambari_agent.HeartbeatHandlers import HeartbeatStopHandlers, bind_signal_handlers
from ambari_agent.ExitHelper import ExitHelper
from ambari_agent.StatusCommandsExecutor import MultiProcessStatusCommandsExecutor, SingleProcessStatusCommandsExecutor
from ambari_commons.network import reconfigure_urllib2_opener
from resource_management.libraries.functions.version import compare_versions
from ambari_commons.os_utils import get_used_ram

logger = logging.getLogger(__name__)

AGENT_AUTO_RESTART_EXIT_CODE = 77

AGENT_RAM_OVERUSE_MESSAGE = "Ambari-agent RAM usage {used_ram} MB went above {config_name}={max_ram} MB. Restarting ambari-agent to clean the RAM."

class Controller(threading.Thread):

  EXECUTION_COMMANDS = 'executionCommands'
  ALERT_DEFINITION_COMMANDS = 'alertDefinitionCommands'

  def __init__(self, config, server_hostname, heartbeat_stop_callback = None):
    threading.Thread.__init__(self)
    logger.debug('Initializing Controller RPC thread.')
    if heartbeat_stop_callback is None:
      heartbeat_stop_callback = HeartbeatStopHandlers()

    self.version = self.read_agent_version(config)
    self.lock = threading.Lock()
    self.safeMode = True
    self.credential = None
    self.config = config
    self.hostname = hostname.hostname(config)
    self.serverHostname = server_hostname
    server_secured_url = 'https://' + self.serverHostname + \
                         ':' + config.get('server', 'secured_url_port')
    self.registerUrl = server_secured_url + '/agent/v1/register/' + self.hostname
    self.heartbeatUrl = server_secured_url + '/agent/v1/heartbeat/' + self.hostname
    self.componentsUrl = server_secured_url + '/agent/v1/components/'
    self.netutil = NetUtil(self.config, heartbeat_stop_callback)
    self.responseId = -1
    self.repeatRegistration = False
    self.isRegistered = False
    self.cachedconnect = None
    self.max_reconnect_retry_delay = int(config.get('server','max_reconnect_retry_delay', default=30))
    self.hasMappedComponents = True
    self.statusCommandsExecutor = None

    # Event is used for synchronizing heartbeat iterations (to make possible
    # manual wait() interruption between heartbeats )
    self.heartbeat_stop_callback = heartbeat_stop_callback
    # List of callbacks that are called at agent registration
    self.registration_listeners = []

    # pull config directory out of config
    cache_dir = config.get('agent', 'cache_dir')
    if cache_dir is None:
      cache_dir = '/var/lib/ambari-agent/cache'

    self.max_ram_soft = int(config.get('agent','memory_threshold_soft_mb', default=0))
    self.max_ram_hard = int(config.get('agent','memory_threshold_hard_mb', default=0))

    stacks_cache_dir = os.path.join(cache_dir, FileCache.STACKS_CACHE_DIRECTORY)
    common_services_cache_dir = os.path.join(cache_dir, FileCache.COMMON_SERVICES_DIRECTORY)
    extensions_cache_dir = os.path.join(cache_dir, FileCache.EXTENSIONS_CACHE_DIRECTORY)
    host_scripts_cache_dir = os.path.join(cache_dir, FileCache.HOST_SCRIPTS_CACHE_DIRECTORY)
    alerts_cache_dir = os.path.join(cache_dir, FileCache.ALERTS_CACHE_DIRECTORY)
    cluster_cache_dir = os.path.join(cache_dir, FileCache.CLUSTER_CACHE_DIRECTORY)
    recovery_cache_dir = os.path.join(cache_dir, FileCache.RECOVERY_CACHE_DIRECTORY)

    self.heartbeat_idle_interval_min = int(self.config.get('heartbeat', 'idle_interval_min')) if self.config.get('heartbeat', 'idle_interval_min') else self.netutil.HEARTBEAT_IDLE_INTERVAL_DEFAULT_MIN_SEC
    if self.heartbeat_idle_interval_min < 1:
      self.heartbeat_idle_interval_min = 1

    self.heartbeat_idle_interval_max = int(self.config.get('heartbeat', 'idle_interval_max')) if self.config.get('heartbeat', 'idle_interval_max') else self.netutil.HEARTBEAT_IDLE_INTERVAL_DEFAULT_MAX_SEC

    if self.heartbeat_idle_interval_min > self.heartbeat_idle_interval_max:
      raise Exception("Heartbeat minimum interval={0} seconds can not be greater than the maximum interval={1} seconds !".format(self.heartbeat_idle_interval_min, self.heartbeat_idle_interval_max))

    self.get_heartbeat_interval = functools.partial(self.netutil.get_agent_heartbeat_idle_interval_sec, self.heartbeat_idle_interval_min, self.heartbeat_idle_interval_max)

    self.recovery_manager = RecoveryManager(recovery_cache_dir)

    self.cluster_configuration_cache = ClusterConfigurationCache(cluster_cache_dir)
    self.cluster_topology_cache = ClusterTopologyCache(cluster_cache_dir)

    self.move_data_dir_mount_file()

    if not config.use_system_proxy_setting():
      reconfigure_urllib2_opener(ignore_system_proxy=True)

    self.alert_scheduler_handler = AlertSchedulerHandler(alerts_cache_dir,
      stacks_cache_dir, common_services_cache_dir, extensions_cache_dir,
      host_scripts_cache_dir, self.cluster_configuration_cache, config,
      self.recovery_manager)

    self.alert_scheduler_handler.start()


  def read_agent_version(self, config):
    data_dir = config.get('agent', 'prefix')
    ver_file = os.path.join(data_dir, 'version')
    f = open(ver_file, "r")
    version = f.read().strip()
    f.close()
    return version


  def __del__(self):
    logger.info("Server connection disconnected.")

  def registerWithServer(self):
    """
    :return: returning from current method without setting self.isRegistered
    to True will lead to agent termination.
    """
    LiveStatus.SERVICES = []
    LiveStatus.CLIENT_COMPONENTS = []
    LiveStatus.COMPONENTS = []
    ret = {}

    while not self.isRegistered:
      try:
        data = json.dumps(self.register.build(self.version))
        try:
          server_ip = socket.gethostbyname(self.hostname)
          logger.info("Registering with %s (%s) (agent=%s)", self.hostname, server_ip, data)
        except socket.error:
          logger.warn("Unable to determine the IP address of '%s', agent registration may fail (agent=%s)",
                      self.hostname, data)

        ret = self.sendRequest(self.registerUrl, data)

        if logger.isEnabledFor(logging.DEBUG):
          logger.debug("Registration response is %s", pprint.pformat(ret))

        # exitstatus is a code of error which was raised on server side.
        # exitstatus = 0 (OK - Default)
        # exitstatus = 1 (Registration failed because different version of agent and server)
        exitstatus = 0
        if 'exitstatus' in ret.keys():
          exitstatus = int(ret['exitstatus'])

        if exitstatus == 1:
          # log - message, which will be printed to agents log
          if 'log' in ret.keys():
            log = ret['log']
            logger.error(log)
          self.isRegistered = False
          self.repeatRegistration = False
          return ret

        self.responseId = int(ret['responseId'])
        logger.info("Registration Successful (response id = %s)", self.responseId)

        self.isRegistered = True

        # always update cached cluster configurations on registration
        # must be prior to any other operation
        self.update_caches_from_heartbeat(ret)
        self.recovery_manager.update_configuration_from_registration(ret)
        self.config.update_configuration_from_registration(ret)
        logger.debug("Updated config: %s", self.config)

        # Start StatusCommandExecutor child process or restart it if already running
        # in order to receive up to date agent config.
        self.statusCommandsExecutor.relaunch("REGISTER_WITH_SERVER")

        if 'statusCommands' in ret.keys():
          logger.debug("Got status commands on registration.")
          self.addToStatusQueue(ret['statusCommands'])
        else:
          self.hasMappedComponents = False

        # always update alert definitions on registration
        self.alert_scheduler_handler.update_definitions(ret)
      except ssl.SSLError:
        self.repeatRegistration = False
        self.isRegistered = False
        return
      except Exception, ex:
        # try a reconnect only after a certain amount of random time
        delay = randint(0, self.max_reconnect_retry_delay)
        logger.error("Unable to connect to: " + self.registerUrl, exc_info=True)
        logger.error("Error:" + str(ex))
        logger.warn(""" Sleeping for {0} seconds and then trying again """.format(delay,))
        time.sleep(delay)

    return ret

  def update_caches_from_heartbeat(self, heartbeat):
    heartbeat_keys = heartbeat.keys()

    if self.EXECUTION_COMMANDS in heartbeat_keys:
      execution_commands = heartbeat[self.EXECUTION_COMMANDS]
      for command in execution_commands:
        if 'clusterName' in command and 'configurations' in command:
          cluster_name = command['clusterName']
          configurations = command['configurations']
          topology = command['clusterHostInfo']
          self.cluster_configuration_cache.update_cluster_cache(cluster_name, configurations)
          self.cluster_topology_cache.update_cluster_cache(cluster_name, topology)

          # TODO: use this once server part is ready.
          self.cluster_topology_cache.get_md5_hashsum(cluster_name)
          self.cluster_configuration_cache.get_md5_hashsum(cluster_name)


    if self.ALERT_DEFINITION_COMMANDS in heartbeat_keys:
      alert_definition_commands = heartbeat[self.ALERT_DEFINITION_COMMANDS]
      for command in alert_definition_commands:
        if 'clusterName' in command and 'configurations' in command:
          cluster_name = command['clusterName']
          configurations = command['configurations']
          self.cluster_configuration_cache.update_cluster_cache(cluster_name, configurations)

          # TODO: use this once server part is ready.
          self.cluster_configuration_cache.get_md5_hashsum(cluster_name)

  def cancelCommandInQueue(self, commands):
    """ Remove from the queue commands, kill the process if it's in progress """
    if commands:
      try:
        self.actionQueue.cancel(commands)
      except Exception, err:
        logger.error("Exception occurred on commands cancel: %s", err.message)

  def addToQueue(self, commands):
    """Add to the queue for running the commands """
    """ Put the required actions into the Queue """
    """ Verify if the action is to reboot or not """
    if not commands:
      logger.debug("No commands received from %s", self.serverHostname)
    else:
      """Only add to the queue if not empty list """
      logger.info("Adding %s commands. Heartbeat id = %s", len(commands), self.responseId)
      if 'clusterName' in commands[0].keys():
        self.updateComponents(commands[0]['clusterName'])
      self.actionQueue.put(commands)

  def addToStatusQueue(self, commands):
    if not commands:
      logger.debug("No status commands received from %s", self.serverHostname)
    else:
      logger.info("Adding %s status commands. Heartbeat id = %s", len(commands), self.responseId)
      if 'clusterName' in commands[0].keys():
        self.updateComponents(commands[0]['clusterName'])
      self.recovery_manager.process_status_commands(commands)
      self.actionQueue.put_status(commands)
    pass

  # For testing purposes
  DEBUG_HEARTBEAT_RETRIES = 0
  DEBUG_SUCCESSFULL_HEARTBEATS = 0
  DEBUG_STOP_HEARTBEATING = False

  def trigger_heartbeat(self):
    self.heartbeat_stop_callback.set_heartbeat()

  def heartbeatWithServer(self):
    self.DEBUG_HEARTBEAT_RETRIES = 0
    self.DEBUG_SUCCESSFULL_HEARTBEATS = 0
    retry = False
    certVerifFailed = False
    state_interval = int(self.config.get('heartbeat', 'state_interval_seconds', '60'))

    # last time when state was successfully sent to server
    last_state_timestamp = 0.0

    # in order to be able to check from logs that heartbeats processing
    # still running we log a message. However to avoid generating too
    # much log when the heartbeat runs at a higher rate (e.g. 1 second intervals)
    # we log the message at the same interval as 'state interval'
    heartbeat_running_msg_timestamp = 0.0

    # Prevent excessive logging by logging only at specific intervals
    getrecoverycommands_timestamp = 0.0
    getrecoverycommands_interval = self.netutil.HEARTBEAT_IDLE_INTERVAL_DEFAULT_MAX_SEC

    heartbeat_interval = self.netutil.HEARTBEAT_IDLE_INTERVAL_DEFAULT_MAX_SEC

    while not self.DEBUG_STOP_HEARTBEATING:
      current_time = time.time()
      logging_level = logging.DEBUG
      if current_time - heartbeat_running_msg_timestamp > state_interval:
        # log more steps every minute or so
        logging_level = logging.INFO
        heartbeat_running_msg_timestamp = current_time

      try:
        logger.log(logging_level, "Heartbeat (response id = %s) with server is running...", self.responseId)

        send_state = False
        if not retry:
          if current_time - last_state_timestamp > state_interval:
            send_state = True

          logger.log(logging_level, "Building heartbeat message")

          data = json.dumps(self.heartbeat.build(self.responseId, send_state, self.hasMappedComponents))
        else:
          self.DEBUG_HEARTBEAT_RETRIES += 1

        if logger.isEnabledFor(logging.DEBUG):
          logger.log(logging_level, "Sending Heartbeat (id = %s): %s", self.responseId, data)
        else:
          logger.log(logging_level, "Sending Heartbeat (id = %s)", self.responseId)

        response = self.sendRequest(self.heartbeatUrl, data)

        exitStatus = 0
        if 'exitstatus' in response.keys():
          exitStatus = int(response['exitstatus'])

        if exitStatus != 0:
          raise Exception(response)

        serverId = int(response['responseId'])

        logger.log(logging_level, 'Heartbeat response received (id = %s)', serverId)

        cluster_size = int(response['clusterSize']) if 'clusterSize' in response.keys() else -1

        # TODO: this needs to be revised if hosts can be shared across multiple clusters
        heartbeat_interval = self.get_heartbeat_interval(cluster_size) \
          if cluster_size > 0 \
          else self.netutil.HEARTBEAT_IDLE_INTERVAL_DEFAULT_MAX_SEC

        logger.log(logging_level, "Heartbeat interval is %s seconds", heartbeat_interval)

        if 'hasMappedComponents' in response.keys():
          self.hasMappedComponents = response['hasMappedComponents'] is not False

        if 'hasPendingTasks' in response.keys():
          has_pending_tasks = bool(response['hasPendingTasks'])
          self.recovery_manager.set_paused(has_pending_tasks)

        if 'registrationCommand' in response.keys():
          # check if the registration command is None. If none skip
          if response['registrationCommand'] is not None:
            logger.info("RegistrationCommand received - repeat agent registration")
            self.isRegistered = False
            self.repeatRegistration = True
            return

        used_ram = get_used_ram()/1000
        # dealing with a possible memory leaks
        if self.max_ram_soft and used_ram >= self.max_ram_soft and not self.actionQueue.tasks_in_progress_or_pending():
          logger.error(AGENT_RAM_OVERUSE_MESSAGE.format(used_ram=used_ram, config_name="memory_threshold_soft_mb", max_ram=self.max_ram_soft))
          self.restartAgent()
        if self.max_ram_hard and used_ram >= self.max_ram_hard:
          logger.error(AGENT_RAM_OVERUSE_MESSAGE.format(used_ram=used_ram, config_name="memory_threshold_hard_mb", max_ram=self.max_ram_hard))
          self.restartAgent()

        if serverId != self.responseId + 1:
          logger.error("Error in responseId sequence - received responseId={0} from server while expecting {1} - restarting..."
              .format(serverId, self.responseId + 1))

          self.restartAgent()
        else:
          self.responseId = serverId
          if send_state:
            last_state_timestamp = current_time

        # if the response contains configurations, update the in-memory and
        # disk-based configuration cache (execution and alert commands have this)
        logger.log(logging_level, "Updating configurations from heartbeat")
        self.update_caches_from_heartbeat(response)

        response_keys = response.keys()

        # there's case when canceled task can be processed in Action Queue.execute before adding rescheduled task to queue
        # this can cause command failure instead result suppression
        # so canceling and putting rescheduled commands should be executed atomically
        if 'cancelCommands' in response_keys or 'executionCommands' in response_keys:
          logger.log(logging_level, "Adding cancel/execution commands")
        with self.actionQueue.lock:
          if 'cancelCommands' in response_keys:
            self.cancelCommandInQueue(response['cancelCommands'])

          if 'executionCommands' in response_keys:
            execution_commands = response['executionCommands']
            self.recovery_manager.process_execution_commands(execution_commands)
            self.addToQueue(execution_commands)

        if 'statusCommands' in response_keys:
          # try storing execution command details and desired state
          self.addToStatusQueue(response['statusCommands'])

        if current_time - getrecoverycommands_timestamp > getrecoverycommands_interval:
          getrecoverycommands_timestamp = current_time
          if not self.actionQueue.tasks_in_progress_or_pending():
            logger.log(logging_level, "Adding recovery commands")
            recovery_commands = self.recovery_manager.get_recovery_commands()
            for recovery_command in recovery_commands:
              logger.info("Adding recovery command %s for component %s",
                          recovery_command['roleCommand'], recovery_command['role'])
              self.addToQueue([recovery_command])

        if 'alertDefinitionCommands' in response_keys:
          logger.log(logging_level, "Updating alert definitions")
          self.alert_scheduler_handler.update_definitions(response)

        if 'alertExecutionCommands' in response_keys:
          logger.log(logging_level, "Executing alert commands")
          self.alert_scheduler_handler.execute_alert(response['alertExecutionCommands'])

        if response['restartAgent']:
          logger.error("Received the restartAgent command")
          self.restartAgent()
        else:
          logger.debug("No commands sent from %s", self.serverHostname)

        if retry:
          logger.info("Reconnected to %s", self.heartbeatUrl)

        if "recoveryConfig" in response:
          # update the list of components enabled for recovery
          logger.log(logging_level, "Updating recovery config")
          self.recovery_manager.update_configuration_from_registration(response)

        retry = False
        certVerifFailed = False
        self.DEBUG_SUCCESSFULL_HEARTBEATS += 1
        self.DEBUG_HEARTBEAT_RETRIES = 0
        self.heartbeat_stop_callback.reset_heartbeat()
      except ssl.SSLError:
        self.repeatRegistration=False
        self.isRegistered = False
        logger.exception("SSLError while trying to heartbeat.")
        return
      except Exception, err:
        if "code" in err:
          logger.error(err.code)
        else:
          logException = False
          if logger.isEnabledFor(logging.DEBUG):
            logException = True

          exceptionMessage = str(err)
          errorMessage = "Unable to reconnect to {0} (attempts={1}, details={2})".format(self.heartbeatUrl, self.DEBUG_HEARTBEAT_RETRIES, exceptionMessage)

          if not retry:
            errorMessage = "Connection to {0} was lost (details={1})".format(self.serverHostname, exceptionMessage)

          logger.error(errorMessage, exc_info=logException)

          if 'certificate verify failed' in str(err) and not certVerifFailed:
            logger.warn("Server certificate verify failed. Did you regenerate server certificate?")
            certVerifFailed = True

        self.cachedconnect = None  # Previous connection is broken now
        retry = True

        #randomize the heartbeat
        delay = randint(0, self.max_reconnect_retry_delay)
        logger.info("Waiting {0} seconds before reconnecting to {1}".format(delay, self.heartbeatUrl))
        time.sleep(delay)

      # Sleep for some time
      timeout = heartbeat_interval - self.netutil.MINIMUM_INTERVAL_BETWEEN_HEARTBEATS
      logger.log(logging_level, "Waiting %s for next heartbeat", timeout)

      if 0 == self.heartbeat_stop_callback.wait(timeout, self.netutil.MINIMUM_INTERVAL_BETWEEN_HEARTBEATS):
        # Stop loop when stop event received
        logger.info("Stop event received")
        self.DEBUG_STOP_HEARTBEATING=True

      logger.log(logging_level, "Wait for next heartbeat over")

  def run(self):
    try:
      self.actionQueue = ActionQueue(self.config, controller=self)
      if self.config.get_multiprocess_status_commands_executor_enabled():
        self.statusCommandsExecutor = MultiProcessStatusCommandsExecutor(self.config, self.actionQueue)
      else:
        self.statusCommandsExecutor = SingleProcessStatusCommandsExecutor(self.config, self.actionQueue)
      ExitHelper().register(self.statusCommandsExecutor.kill, "CLEANUP_KILLING", can_relaunch=False)
      self.actionQueue.start()
      self.register = Register(self.config)
      self.heartbeat = Heartbeat(self.actionQueue, self.config, self.alert_scheduler_handler.collector())

      opener = urllib2.build_opener()
      urllib2.install_opener(opener)

      while True:
        self.repeatRegistration = False
        self.registerAndHeartbeat()
        if not self.repeatRegistration:
          logger.info("Finished heartbeating and registering cycle")
          break
    except:
      logger.exception("Controller thread failed with exception:")
      raise

    logger.info("Controller thread has successfully finished")

  def registerAndHeartbeat(self):
    registerResponse = self.registerWithServer()

    if "response" in registerResponse:
      message = registerResponse["response"]
      logger.info("Registration response from %s was %s", self.serverHostname, message)

      if self.isRegistered:
        # Clearing command queue to stop executing "stale" commands
        # after registration
        logger.info('Resetting ActionQueue...')
        self.actionQueue.reset()

        # Process callbacks
        for callback in self.registration_listeners:
          callback()

        time.sleep(self.netutil.HEARTBEAT_IDLE_INTERVAL_DEFAULT_MAX_SEC)
        self.heartbeatWithServer()
      else:
        logger.info("Registration response from %s didn't contain 'response' as a key".format(self.serverHostname))

  def restartAgent(self):
    ExitHelper().exit(AGENT_AUTO_RESTART_EXIT_CODE)


  def sendRequest(self, url, data):
    response = None

    try:
      if self.cachedconnect is None: # Lazy initialization
        self.cachedconnect = security.CachedHTTPSConnection(self.config, self.serverHostname)
      req = urllib2.Request(url, data, {'Content-Type': 'application/json',
                                        'Accept-encoding': 'gzip'})
      response = self.cachedconnect.request(req)
      return json.loads(response)
    except Exception, exception:
      if response is None:
        raise IOError('Request to {0} failed due to {1}'.format(url, str(exception)))
      else:
        raise IOError('Response parsing failed! Request data: ' + str(data)
                      + '; Response: ' + str(response))


  def updateComponents(self, cluster_name):
    if LiveStatus.SERVICES:
      return

    logger.debug("Updating components map of cluster %s", cluster_name)

    # May throw IOError on server connection error
    response = self.sendRequest(self.componentsUrl + cluster_name, None)
    logger.debug("Response from %s was %s", self.serverHostname, response)

    services, client_components, server_components = [], [], []
    for service, components in response['components'].items():
      services.append(service)
      for component, category in components.items():
        service_component = {"serviceName": service, "componentName": component}
        if category == 'CLIENT':
          client_components.append(service_component)
        else:
          server_components.append(service_component)

    LiveStatus.SERVICES = services
    LiveStatus.CLIENT_COMPONENTS = client_components
    LiveStatus.COMPONENTS = server_components

    logger.debug("Components map updated")
    logger.debug("LiveStatus.SERVICES %s", LiveStatus.SERVICES)
    logger.debug("LiveStatus.CLIENT_COMPONENTS %s", LiveStatus.CLIENT_COMPONENTS)
    logger.debug("LiveStatus.COMPONENTS %s", LiveStatus.COMPONENTS)

  def get_status_commands_executor(self):
    return self.statusCommandsExecutor

  def move_data_dir_mount_file(self):
    """
    In Ambari 2.1.2, we moved the dfs_data_dir_mount.hist to a static location
    because /etc/hadoop/conf points to a symlink'ed location that would change during
    Stack Upgrade.
    """
    try:
      version = self.get_version()
      logger.debug("Ambari Agent version {0}".format(version))
      if compare_versions(version, "2.1.2") >= 0:
        source_file = "/etc/hadoop/conf/dfs_data_dir_mount.hist"
        destination_file = "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist"
        if os.path.exists(source_file) and not os.path.exists(destination_file):
          command = "mkdir -p %s" % os.path.dirname(destination_file)
          logger.info("Moving Data Dir Mount History file. Executing command: %s" % command)
          return_code = subprocess32.call(command, shell=True)
          logger.info("Return code: %d" % return_code)

          command = "mv %s %s" % (source_file, destination_file)
          logger.info("Moving Data Dir Mount History file. Executing command: %s" % command)
          return_code = subprocess32.call(command, shell=True)
          logger.info("Return code: %d" % return_code)
    except Exception, e:
      logger.error("Exception in move_data_dir_mount_file(). Error: {0}".format(str(e)))

  def get_version(self):
    version = self.version
    matches = re.findall(r"[\d+.]+",version)
    if not matches:
      logger.warning("No version match result, use original version {0}".format(version))
      return version
    else:
      return matches[0]

def main(argv=None):
  # Allow Ctrl-C

  formatter = logging.Formatter("%(asctime)s %(filename)s:%(lineno)d - \
    %(message)s")
  stream_handler = logging.StreamHandler()
  stream_handler.setFormatter(formatter)
  logger.addHandler(stream_handler)

  logger.info('Starting Server RPC Thread: %s' % ' '.join(sys.argv))

  config = AmbariConfig.config
  heartbeat_stop_callback = bind_signal_handlers()
  collector = Controller(config, heartbeat_stop_callback)
  collector.start()
  collector.run()

if __name__ == '__main__':
  main()
