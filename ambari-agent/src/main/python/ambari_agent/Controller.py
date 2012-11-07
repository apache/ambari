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
from Heartbeat import Heartbeat
from Register import Register
from ActionQueue import ActionQueue
from optparse import OptionParser
from wsgiref.simple_server import ServerHandler
import security
from NetUtil import NetUtil

logger = logging.getLogger()


class Controller(threading.Thread):

  def __init__(self, config):
    threading.Thread.__init__(self)
    logger.debug('Initializing Controller RPC thread.')
    self.lock = threading.Lock()
    self.safeMode = True
    self.credential = None
    self.config = config
    self.hostname = socket.gethostname()
    server_secured_url = 'https://' + config.get('server', 'hostname') + ':' + config.get('server', 'secured_url_port')
    self.registerUrl = server_secured_url + '/agent/register/' + self.hostname
    self.heartbeatUrl = server_secured_url + '/agent/heartbeat/' + self.hostname
    self.netutil = NetUtil()
     
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
    while registered == False:
      try:
        data = json.dumps(self.register.build(id))
        logger.info("Registering with the server " + pprint.pformat(data))
        req = urllib2.Request(self.registerUrl, data, {'Content-Type': 
                                                      'application/json'})
        stream = security.secured_url_open(req)
        response = stream.read()
        stream.close()
        ret = json.loads(response)
        logger.info("Registered with the server with " + pprint.pformat(ret))
        registered = True
        pass
      except Exception, err:
        delay = self.netutil.CONNECT_SERVER_RETRY_INTERVAL_SEC
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
      logger.info("No commands from the server.")
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
    #TODO make sure the response id is monotonically increasing
    id = 0
    while True:
      try:
        if self.DEBUG_STOP_HEARTBITTING:
          return

        if not retry:
          data = json.dumps(self.heartbeat.build(id))
          pass
        else:
          self.DEBUG_HEARTBEAT_RETRIES += 1

        req = urllib2.Request(self.heartbeatUrl, data, {'Content-Type':
                                                        'application/json'})
        f = security.secured_url_open(req)
        response = f.read()
        f.close()
        response = json.loads(response)
        id=int(response['responseId'])
        
        if 'executionCommands' in response.keys():
          self.addToQueue(response['executionCommands'])
          pass
        else:
          logger.info("No commands sent from the Server.")
          pass
        retry=False
        self.DEBUG_SUCCESSFULL_HEARTBEATS += 1
        self.DEBUG_HEARTBEAT_RETRIES = 0
      except Exception, err:
        retry=True
        if "code" in err:
          logger.error(err.code)
        else:
          logger.error("Unable to connect to: "+ 
                       self.heartbeatUrl,exc_info=True)
      if self.actionQueue.isIdle():
        time.sleep(self.netutil.HEARTBEAT_IDDLE_INTERVAL_SEC)
      else:
        time.sleep(self.netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC)
    pass

  def run(self):
    opener = urllib2.build_opener()
    urllib2.install_opener(opener)
    
    registerResponse = self.registerWithServer()
    message = registerResponse['response']
    logger.info("Response from server = " + message)
    self.heartbeatWithServer()
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
