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
import ambari_simplejson as json
import sys
import os
import socket
import time
import threading
import urllib2
import pprint
from random import randint
import subprocess

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
from ambari_agent.ClusterConfiguration import  ClusterConfiguration
from ambari_agent.RecoveryManager import  RecoveryManager
from ambari_agent.HeartbeatHandlers import HeartbeatStopHandlers, bind_signal_handlers
from ambari_agent.ExitHelper import ExitHelper
from resource_management.libraries.functions.version import compare_versions

logger = logging.getLogger(__name__)

AGENT_AUTO_RESTART_EXIT_CODE = 77

class Controller(threading.Thread):

  def __init__(self, config, heartbeat_stop_callback = None, range=30):
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
    self.serverHostname = hostname.server_hostname(config)
    server_secured_url = 'https://' + self.serverHostname + \
                         ':' + config.get('server', 'secured_url_port')
    self.registerUrl = server_secured_url + '/agent/v1/register/' + self.hostname
    self.heartbeatUrl = server_secured_url + '/agent/v1/heartbeat/' + self.hostname
    self.componentsUrl = server_secured_url + '/agent/v1/components/'
    self.netutil = NetUtil(heartbeat_stop_callback)
    self.responseId = -1
    self.repeatRegistration = False
    self.isRegistered = False
    self.cachedconnect = None
    self.range = range
    self.hasMappedComponents = True
    # Event is used for synchronizing heartbeat iterations (to make possible
    # manual wait() interruption between heartbeats )
    self.heartbeat_stop_callback = heartbeat_stop_callback
    # List of callbacks that are called at agent registration
    self.registration_listeners = []

    # pull config directory out of config
    cache_dir = config.get('agent', 'cache_dir')
    if cache_dir is None:
      cache_dir = '/var/lib/ambari-agent/cache'

    stacks_cache_dir = os.path.join(cache_dir, FileCache.STACKS_CACHE_DIRECTORY)
    common_services_cache_dir = os.path.join(cache_dir, FileCache.COMMON_SERVICES_DIRECTORY)
    host_scripts_cache_dir = os.path.join(cache_dir, FileCache.HOST_SCRIPTS_CACHE_DIRECTORY)
    alerts_cache_dir = os.path.join(cache_dir, FileCache.ALERTS_CACHE_DIRECTORY)
    cluster_config_cache_dir = os.path.join(cache_dir, FileCache.CLUSTER_CONFIGURATION_CACHE_DIRECTORY)
    recovery_cache_dir = os.path.join(cache_dir, FileCache.RECOVERY_CACHE_DIRECTORY)

    self.recovery_manager = RecoveryManager(recovery_cache_dir)

    self.cluster_configuration = ClusterConfiguration(cluster_config_cache_dir)

    self.move_data_dir_mount_file()

    self.alert_scheduler_handler = AlertSchedulerHandler(alerts_cache_dir,
      stacks_cache_dir, common_services_cache_dir, host_scripts_cache_dir,
      self.cluster_configuration, config, self.recovery_manager)

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
        prettyData = pprint.pformat(data)

        try:
          server_ip = socket.gethostbyname(self.hostname)
          logger.info("Registering with %s (%s) (agent=%s)", self.hostname, server_ip, prettyData)
        except socket.error:
          logger.warn("Unable to determine the IP address of '%s', agent registration may fail (agent=%s)",
                      self.hostname, prettyData)

        ret = self.sendRequest(self.registerUrl, data)
        prettyData = pprint.pformat(ret)
        logger.debug("Registration response is %s", prettyData)

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
        self.cluster_configuration.update_configurations_from_heartbeat(ret)
        self.recovery_manager.update_configuration_from_registration(ret)
        self.config.update_configuration_from_registration(ret)
        logger.debug("Updated config:" + str(self.config))

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
        delay = randint(0, self.range)
        logger.error("Unable to connect to: " + self.registerUrl, exc_info=True)
        logger.error("Error:" + str(ex))
        logger.warn(""" Sleeping for {0} seconds and then trying again """.format(delay,))
        time.sleep(delay)

    return ret

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
      self.actionQueue.put(commands)

  def addToStatusQueue(self, commands):
    if not commands:
      logger.debug("No status commands received from %s", self.serverHostname)
    else:
      if not LiveStatus.SERVICES:
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
    hb_interval = self.config.get('heartbeat', 'state_interval')

    while not self.DEBUG_STOP_HEARTBEATING:
      try:
        if not retry:
          data = json.dumps(
              self.heartbeat.build(self.responseId, int(hb_interval), self.hasMappedComponents))
        else:
          self.DEBUG_HEARTBEAT_RETRIES += 1

        if logger.isEnabledFor(logging.DEBUG):
          logger.debug("Sending Heartbeat (id = %s): %s", self.responseId, data)

        response = self.sendRequest(self.heartbeatUrl, data)
        exitStatus = 0
        if 'exitstatus' in response.keys():
          exitStatus = int(response['exitstatus'])

        if exitStatus != 0:
          raise Exception(response)

        serverId = int(response['responseId'])

        logger.info('Heartbeat response received (id = %s)', serverId)

        if 'hasMappedComponents' in response.keys():
          self.hasMappedComponents = response['hasMappedComponents'] is not False

        if 'hasPendingTasks' in response.keys():
          self.recovery_manager.set_paused(response['hasPendingTasks'])


        if 'registrationCommand' in response.keys():
          # check if the registration command is None. If none skip
          if response['registrationCommand'] is not None:
            logger.info("RegistrationCommand received - repeat agent registration")
            self.isRegistered = False
            self.repeatRegistration = True
            return

        if serverId != self.responseId + 1:
          logger.error("Error in responseId sequence - restarting")
          self.restartAgent()
        else:
          self.responseId = serverId

        # if the response contains configurations, update the in-memory and
        # disk-based configuration cache (execution and alert commands have this)
        self.cluster_configuration.update_configurations_from_heartbeat(response)

        response_keys = response.keys()
        if 'cancelCommands' in response_keys:
          self.cancelCommandInQueue(response['cancelCommands'])

        if 'executionCommands' in response_keys:
          execution_commands = response['executionCommands']
          self.recovery_manager.process_execution_commands(execution_commands)
          self.addToQueue(execution_commands)

        if 'statusCommands' in response_keys:
          # try storing execution command details and desired state
          self.addToStatusQueue(response['statusCommands'])

        if not self.actionQueue.tasks_in_progress_or_pending():
          recovery_commands = self.recovery_manager.get_recovery_commands()
          for recovery_command in recovery_commands:
            logger.info("Adding recovery command %s for component %s",
                        recovery_command['roleCommand'], recovery_command['role'])
            self.addToQueue([recovery_command])

        if 'alertDefinitionCommands' in response_keys:
          self.alert_scheduler_handler.update_definitions(response)

        if 'alertExecutionCommands' in response_keys:
          self.alert_scheduler_handler.execute_alert(response['alertExecutionCommands'])

        if "true" == response['restartAgent']:
          logger.error("Received the restartAgent command")
          self.restartAgent()
        else:
          logger.debug("No commands sent from %s", self.serverHostname)

        if retry:
          logger.info("Reconnected to %s", self.heartbeatUrl)

        retry = False
        certVerifFailed = False
        self.DEBUG_SUCCESSFULL_HEARTBEATS += 1
        self.DEBUG_HEARTBEAT_RETRIES = 0
        self.heartbeat_stop_callback.reset_heartbeat()
      except ssl.SSLError:
        self.repeatRegistration=False
        self.isRegistered = False
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
        delay = randint(0, self.range)
        time.sleep(delay)

      # Sleep for some time
      timeout = self.netutil.HEARTBEAT_IDDLE_INTERVAL_SEC \
                - self.netutil.MINIMUM_INTERVAL_BETWEEN_HEARTBEATS
      if 0 == self.heartbeat_stop_callback.wait(timeout, self.netutil.MINIMUM_INTERVAL_BETWEEN_HEARTBEATS):
        # Stop loop when stop event received
        logger.info("Stop event received")
        self.DEBUG_STOP_HEARTBEATING=True

  def run(self):
    self.actionQueue = ActionQueue(self.config, controller=self)
    self.actionQueue.start()
    self.register = Register(self.config)
    self.heartbeat = Heartbeat(self.actionQueue, self.config, self.alert_scheduler_handler.collector())

    opener = urllib2.build_opener()
    urllib2.install_opener(opener)

    while True:
      self.repeatRegistration = False
      self.registerAndHeartbeat()
      if not self.repeatRegistration:
        break

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

        time.sleep(self.netutil.HEARTBEAT_IDDLE_INTERVAL_SEC)
        self.heartbeatWithServer()

  def restartAgent(self):
    ExitHelper().exit(AGENT_AUTO_RESTART_EXIT_CODE)


  def sendRequest(self, url, data):
    response = None

    try:
      if self.cachedconnect is None: # Lazy initialization
        self.cachedconnect = security.CachedHTTPSConnection(self.config)
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
    logger.debug("Updating components map of cluster " + cluster_name)

    # May throw IOError on server connection error
    response = self.sendRequest(self.componentsUrl + cluster_name, None)
    logger.debug("Response from %s was %s", self.serverHostname, str(response))

    for service, components in response['components'].items():
      LiveStatus.SERVICES.append(service)
      for component, category in components.items():
        if category == 'CLIENT':
          LiveStatus.CLIENT_COMPONENTS.append({"serviceName": service, "componentName": component})
        else:
          LiveStatus.COMPONENTS.append({"serviceName": service, "componentName": component})
    logger.debug("Components map updated")
    logger.debug("LiveStatus.SERVICES" + str(LiveStatus.SERVICES))
    logger.debug("LiveStatus.CLIENT_COMPONENTS" + str(LiveStatus.CLIENT_COMPONENTS))
    logger.debug("LiveStatus.COMPONENTS" + str(LiveStatus.COMPONENTS))

  def move_data_dir_mount_file(self):
    """
    In Ambari 2.1.2, we moved the dfs_data_dir_mount.hist to a static location
    because /etc/hadoop/conf points to a symlink'ed location that would change during
    Stack Upgrade.
    """
    try:
      if compare_versions(self.version, "2.1.2") >= 0:
        source_file = "/etc/hadoop/conf/dfs_data_dir_mount.hist"
        destination_file = "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist"
        if os.path.exists(source_file) and not os.path.exists(destination_file):
          command = "mkdir -p %s" % os.path.dirname(destination_file)
          logger.info("Moving Data Dir Mount History file. Executing command: %s" % command)
          return_code = subprocess.call(command, shell=True)
          logger.info("Return code: %d" % return_code)

          command = "mv %s %s" % (source_file, destination_file)
          logger.info("Moving Data Dir Mount History file. Executing command: %s" % command)
          return_code = subprocess.call(command, shell=True)
          logger.info("Return code: %d" % return_code)
    except Exception, e:
      logger.info("Exception in move_data_dir_mount_file(). Error: {0}".format(str(e)))

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
