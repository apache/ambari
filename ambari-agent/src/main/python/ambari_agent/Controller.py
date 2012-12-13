#!/usr/bin/env python2.6

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
import logging.handlers
import signal
import json
import socket
import sys, traceback
import time
import threading
import urllib2
from urllib2 import Request, urlopen, URLError
import httplib
import ssl
import AmbariConfig
import pprint
import ProcessHelper
from Heartbeat import Heartbeat
from Register import Register
from ActionQueue import ActionQueue
from optparse import OptionParser
from wsgiref.simple_server import ServerHandler
import security
from NetUtil import NetUtil
from random import randrange, randint

logger = logging.getLogger()

class Controller(threading.Thread):

  def __init__(self, config, range=120):
    threading.Thread.__init__(self)
    logger.debug('Initializing Controller RPC thread.')
    self.lock = threading.Lock()
    self.safeMode = True
    self.credential = None
    self.config = config
    self.hostname = socket.gethostname()
    server_secured_url = 'https://' + config.get('server', 'hostname') + ':' + config.get('server', 'secured_url_port')
    self.registerUrl = server_secured_url + '/agent/v1/register/' + self.hostname
    self.heartbeatUrl = server_secured_url + '/agent/v1/heartbeat/' + self.hostname
    self.netutil = NetUtil()
    self.responseId = -1
    self.repeatRegistration = False
    self.cachedconnect = None
    self.range = range

  def start(self):
    self.actionQueue = ActionQueue(self.config)
    self.actionQueue.start()
    self.register = Register()
    self.heartbeat = Heartbeat(self.actionQueue)
    pass
  
  def __del__(self):
    logger.info("Server connection disconnected.")
    pass
  
  def registerWithServer(self):
    retry=False
    firstTime=True
    registered=False
    id = -1
    ret = {}

    while not registered:
      try:
        data = json.dumps(self.register.build(id))
        logger.info("Registering with the server " + pprint.pformat(data))
        response = self.sendRequest(self.registerUrl, data)
        ret = json.loads(response)

        logger.info("Registered with the server with " + pprint.pformat(ret))
        print("Registered with the server")
        self.responseId= int(ret['responseId'])
        registered = True
        if 'statusCommands' in ret.keys():
          logger.info("Got status commands on registration " + pprint.pformat(ret['statusCommands']) )
          self.addToQueue(ret['statusCommands'])
          pass
        pass
      except Exception, err:
        # try a reconnect only after a certain amount of random time
        delay = randint(0, self.range)
        logger.info("Unable to connect to: " + self.registerUrl, exc_info = True)
        """ Sleeping for {0} seconds and then retrying again """.format(delay)
        time.sleep(delay)
        pass
      pass  
    return ret
  
  
  def addToQueue(self, commands):
    """Add to the queue for running the commands """
    """ Put the required actions into the Queue """
    """ Verify if the action is to reboot or not """
    if not commands:
      logger.info("No commands from the server : " + pprint.pformat(commands))
    else:
      """Only add to the queue if not empty list """
      for command in commands:
        logger.info("Adding command to the action queue: \n" +
                     pprint.pformat(command))
        self.actionQueue.put(command)
        pass
      pass
    pass

  # For testing purposes
  DEBUG_HEARTBEAT_RETRIES = 0
  DEBUG_SUCCESSFULL_HEARTBEATS = 0
  DEBUG_STOP_HEARTBITTING = False

  def heartbeatWithServer(self):
    self.DEBUG_HEARTBEAT_RETRIES = 0
    self.DEBUG_SUCCESSFULL_HEARTBEATS = 0
    retry = False
    certVerifFailed = False

    #TODO make sure the response id is monotonically increasing
    id = 0
    while not self.DEBUG_STOP_HEARTBITTING:
      try:
        if not retry:
          data = json.dumps(self.heartbeat.build(self.responseId))
          pass
        else:
          self.DEBUG_HEARTBEAT_RETRIES += 1
        response = self.sendRequest(self.heartbeatUrl, data)
        response = json.loads(response)

        logger.info('Got server response: ' + pprint.pformat(response))
        
        serverId=int(response['responseId'])

        if 'registrationCommand' in response.keys():
          # check if the registration command is None. If none skip
          if response['registrationCommand'] is not None:
            logger.info("RegistrationCommand received - repeat agent registration")
            self.repeatRegistration = True
            return

        if serverId!=self.responseId+1:
          logger.error("Error in responseId sequence - restarting")
          self.restartAgent()
        else:
          self.responseId=serverId

        if 'executionCommands' in response.keys():
          self.addToQueue(response['executionCommands'])
          pass
        if 'statusCommands' in response.keys():
          self.addToQueue(response['statusCommands'])
          pass
        if "true" == response['restartAgent']:
          logger.error("Got restartAgent command")
          self.restartAgent()
        else:
          logger.info("No commands sent from the Server.")
          pass

        if retry:
          print("Reconnected to the server")
          logger.info("Reconnected to the server")
        retry=False
        certVerifFailed = False
        self.DEBUG_SUCCESSFULL_HEARTBEATS += 1
        self.DEBUG_HEARTBEAT_RETRIES = 0
      except Exception, err:
        #randomize the heartbeat
        delay = randint(0, self.range)
        time.sleep(delay)
        if "code" in err:
          logger.error(err.code)
        else:
          logger.error("Unable to connect to: " + self.heartbeatUrl + " due to " + str(err))
          logger.debug("Details: " + str(err), exc_info=True)
          if not retry:
            print("Connection to the server was lost. Reconnecting...")
          if 'certificate verify failed' in str(err) and not certVerifFailed:
            print("Server certificate verify failed. Did you regenerate server certificate?")
            certVerifFailed = True
        self.cachedconnect = None # Previous connection is broken now
        retry=True
      if self.actionQueue.isIdle():
        time.sleep(self.netutil.HEARTBEAT_IDDLE_INTERVAL_SEC)
      else:
        time.sleep(self.netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC)
    pass

  def run(self):
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
    logger.info("Response from server = " + message)
    time.sleep(self.netutil.HEARTBEAT_IDDLE_INTERVAL_SEC)
    self.heartbeatWithServer()

  def restartAgent(self):
    #stopping for now, restart will be added later
    ProcessHelper.stopAgent()
    pass

  def sendRequest(self, url, data):
    if self.cachedconnect is None: # Lazy initialization
      self.cachedconnect = security.CachedHTTPSConnection(self.config)
    req = urllib2.Request(url, data, {'Content-Type': 'application/json'})
    response = self.cachedconnect.request(req)
    return response

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
