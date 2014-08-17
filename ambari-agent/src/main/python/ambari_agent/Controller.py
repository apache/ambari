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
import signal
import json
import sys
import os
import socket
import time
import threading
import urllib2
import pprint
from random import randint

import hostname
import AmbariConfig
from Heartbeat import Heartbeat
from Register import Register
from ActionQueue import ActionQueue
import security
from NetUtil import NetUtil
import ssl
from LiveStatus import LiveStatus
from AlertSchedulerHandler import AlertSchedulerHandler

logger = logging.getLogger()

AGENT_AUTO_RESTART_EXIT_CODE = 77

class Controller(threading.Thread):

  def __init__(self, config, range=30):
    threading.Thread.__init__(self)
    logger.debug('Initializing Controller RPC thread.')
    self.lock = threading.Lock()
    self.safeMode = True
    self.credential = None
    self.config = config
    self.hostname = hostname.hostname(config)
    self.serverHostname = config.get('server', 'hostname')
    server_secured_url = 'https://' + self.serverHostname + \
                         ':' + config.get('server', 'secured_url_port')
    self.registerUrl = server_secured_url + '/agent/v1/register/' + self.hostname
    self.heartbeatUrl = server_secured_url + '/agent/v1/heartbeat/' + self.hostname
    self.componentsUrl = server_secured_url + '/agent/v1/components/'
    self.netutil = NetUtil()
    self.responseId = -1
    self.repeatRegistration = False
    self.isRegistered = False
    self.cachedconnect = None
    self.range = range
    self.hasMappedComponents = True
    # Event is used for synchronizing heartbeat iterations (to make possible
    # manual wait() interruption between heartbeats )
    self.heartbeat_wait_event = threading.Event()
    # List of callbacks that are called at agent registration
    self.registration_listeners = []
    
    # pull config directory out of config
    cache_dir = config.get('agent', 'cache_dir')
    if cache_dir is None:
      cache_dir = '/var/lib/ambari-agent/cache'
      
    self.alert_scheduler_handler = AlertSchedulerHandler(
     os.path.join(cache_dir, 'alerts', 'alert_definitions.json'))


  def __del__(self):
    logger.info("Server connection disconnected.")
    pass
  
  def registerWithServer(self):
    LiveStatus.SERVICES = []
    LiveStatus.CLIENT_COMPONENTS = []
    LiveStatus.COMPONENTS = []
    id = -1
    ret = {}

    while not self.isRegistered:
      try:
        data = json.dumps(self.register.build(id))
        prettyData = pprint.pformat(data)

        try:
          server_ip = socket.gethostbyname(self.hostname)
          logger.info("Registering with %s (%s) (agent=%s)", self.hostname, server_ip, prettyData)
        except socket.error:
          logger.warn("Unable to determine the IP address of '%s', agent registration may fail (agent=%s)",
                      self.hostname, prettyData)

        ret = self.sendRequest(self.registerUrl, data)

        # exitstatus is a code of error which was rised on server side.
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

        logger.info("Registration Successful (response=%s)", pprint.pformat(ret))

        self.responseId = int(ret['responseId'])
        self.isRegistered = True
        if 'statusCommands' in ret.keys():
          logger.info("Got status commands on registration " + pprint.pformat(ret['statusCommands']) )
          self.addToStatusQueue(ret['statusCommands'])
          pass
        else:
          self.hasMappedComponents = False
        pass
      except ssl.SSLError:
        self.repeatRegistration=False
        self.isRegistered = False
        return
      except Exception:
        # try a reconnect only after a certain amount of random time
        delay = randint(0, self.range)
        logger.error("Unable to connect to: " + self.registerUrl, exc_info = True)
        """ Sleeping for {0} seconds and then retrying again """.format(delay)
        time.sleep(delay)
        pass
      pass
    return ret

  def cancelCommandInQueue(self, commands):
    """ Remove from the queue commands, kill the process if it's in progress """
    if commands:
      self.actionQueue.cancel(commands)
    pass
  
  def addToQueue(self, commands):
    """Add to the queue for running the commands """
    """ Put the required actions into the Queue """
    """ Verify if the action is to reboot or not """
    if not commands:
      logger.debug("No commands received from %s", self.serverHostname)
    else:
      """Only add to the queue if not empty list """
      self.actionQueue.put(commands)
    pass

  def addToStatusQueue(self, commands):
    if not commands:
      logger.debug("No status commands received from %s", self.serverHostname)
    else:
      if not LiveStatus.SERVICES:
        self.updateComponents(commands[0]['clusterName'])
      self.actionQueue.put_status(commands)
    pass

  # For testing purposes
  DEBUG_HEARTBEAT_RETRIES = 0
  DEBUG_SUCCESSFULL_HEARTBEATS = 0
  DEBUG_STOP_HEARTBEATING = False

  def heartbeatWithServer(self):
    self.DEBUG_HEARTBEAT_RETRIES = 0
    self.DEBUG_SUCCESSFULL_HEARTBEATS = 0
    retry = False
    certVerifFailed = False

    hb_interval = self.config.get('heartbeat', 'state_interval')

    #TODO make sure the response id is monotonically increasing
    id = 0
    while not self.DEBUG_STOP_HEARTBEATING:
      try:
        if not retry:
          data = json.dumps(
              self.heartbeat.build(self.responseId, int(hb_interval), self.hasMappedComponents))
          pass
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

        if logger.isEnabledFor(logging.DEBUG):
          logger.debug('Heartbeat response (id = %s): %s', serverId, pprint.pformat(response))
        else:
          logger.info('Heartbeat response received (id = %s)', serverId)

        if 'hasMappedComponents' in response.keys():
          self.hasMappedComponents = response['hasMappedComponents'] != False

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
          self.responseId=serverId

        if 'cancelCommands' in response.keys():
          self.cancelCommandInQueue(response['cancelCommands'])
          pass

        if 'executionCommands' in response.keys():
          self.addToQueue(response['executionCommands'])
          pass

        if 'statusCommands' in response.keys():
          self.addToStatusQueue(response['statusCommands'])
          pass

        if "true" == response['restartAgent']:
          logger.error("Received the restartAgent command")
          self.restartAgent()
        else:
          logger.info("No commands sent from %s", self.serverHostname)
          pass

        if retry:
          logger.info("Reconnected to %s", self.heartbeatUrl)

        retry=False
        certVerifFailed = False
        self.DEBUG_SUCCESSFULL_HEARTBEATS += 1
        self.DEBUG_HEARTBEAT_RETRIES = 0
        self.heartbeat_wait_event.clear()
      except ssl.SSLError:
        self.repeatRegistration=False
        self.isRegistered = False
        return
      except Exception, err:
        #randomize the heartbeat
        delay = randint(0, self.range)
        time.sleep(delay)

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

        self.cachedconnect = None # Previous connection is broken now
        retry=True

      # Sleep for some time
      timeout = self.netutil.HEARTBEAT_IDDLE_INTERVAL_SEC \
                - self.netutil.MINIMUM_INTERVAL_BETWEEN_HEARTBEATS
      self.heartbeat_wait_event.wait(timeout = timeout)
      # Sleep a bit more to allow STATUS_COMMAND results to be collected
      # and sent in one heartbeat. Also avoid server overload with heartbeats
      time.sleep(self.netutil.MINIMUM_INTERVAL_BETWEEN_HEARTBEATS)
    pass

  def run(self):
    self.actionQueue = ActionQueue(self.config, controller=self)
    self.actionQueue.start()
    self.register = Register(self.config)
    self.heartbeat = Heartbeat(self.actionQueue, self.config)

    opener = urllib2.build_opener()
    urllib2.install_opener(opener)

    while True:
      self.repeatRegistration = False
      self.registerAndHeartbeat()
      if not self.repeatRegistration:
        break

    pass

  def registerAndHeartbeat(self):
    registerResponse = self.registerWithServer()
    message = registerResponse['response']
    logger.info("Registration response from %s was %s", self.serverHostname, message)

    self.alert_scheduler_handler.start()

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
    os._exit(AGENT_AUTO_RESTART_EXIT_CODE)
    pass

  def sendRequest(self, url, data):
    response = None

    try:
      if self.cachedconnect is None: # Lazy initialization
        self.cachedconnect = security.CachedHTTPSConnection(self.config)
      req = urllib2.Request(url, data, {'Content-Type': 'application/json'})
      response = self.cachedconnect.request(req)
      return json.loads(response)
    except Exception, exception:
      if response is None:
        err_msg = 'Request to {0} failed due to {1}'.format(url, str(exception))
        return {'exitstatus': 1, 'log': err_msg}
      else:
        err_msg = ('Response parsing failed! Request data: ' + str(data)
            + '; Response: ' + str(response))
        logger.warn(err_msg)
        return {'exitstatus': 1, 'log': err_msg}

  def updateComponents(self, cluster_name):
    logger.info("Updating components map of cluster " + cluster_name)

    response = self.sendRequest(self.componentsUrl + cluster_name, None)
    logger.debug("Response from %s was %s", self.serverHostname, str(response))

    for service, components in response['components'].items():
      LiveStatus.SERVICES.append(service)
      for component, category in components.items():
        if category == 'CLIENT':
          LiveStatus.CLIENT_COMPONENTS.append({"serviceName": service, "componentName": component})
        else:
          LiveStatus.COMPONENTS.append({"serviceName": service, "componentName": component})
    logger.info("Components map updated")
    logger.debug("LiveStatus.SERVICES" + str(LiveStatus.SERVICES))
    logger.debug("LiveStatus.CLIENT_COMPONENTS" + str(LiveStatus.CLIENT_COMPONENTS))
    logger.debug("LiveStatus.COMPONENTS" + str(LiveStatus.COMPONENTS))
    pass

def main(argv=None):
  # Allow Ctrl-C
  signal.signal(signal.SIGINT, signal.SIG_DFL)

  logger.setLevel(logging.INFO)
  formatter = logging.Formatter("%(asctime)s %(filename)s:%(lineno)d - \
    %(message)s")
  stream_handler = logging.StreamHandler()
  stream_handler.setFormatter(formatter)
  logger.addHandler(stream_handler)

  logger.info('Starting Server RPC Thread: %s' % ' '.join(sys.argv))

  config = AmbariConfig.config
  collector = Controller(config)
  collector.start()
  collector.run()

if __name__ == '__main__':
  main()
