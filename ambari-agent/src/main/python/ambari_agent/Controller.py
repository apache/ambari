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
import AmbariConfig
import pprint
from Heartbeat import Heartbeat
from Register import Register
from ActionQueue import ActionQueue
from optparse import OptionParser
from wsgiref.simple_server import ServerHandler

logger = logging.getLogger()

class Controller(threading.Thread):

  def __init__(self, config):
    threading.Thread.__init__(self)
    logger.debug('Initializing Controller RPC thread.')
    self.lock = threading.Lock()
    self.safeMode = True
    self.credential = None
    self.config = config
    #Disabled security until we have fix for AMBARI-157
    #if(config.get('controller', 'user')!=None and config.get('controller', 'password')!=None):
    #  self.credential = { 'user' : config.get('controller', 'user'),
    #                      'password' : config.get('controller', 'password')
    #  }
    self.hostname = socket.gethostname()
    self.registerUrl = config.get('server', 'url') + \
      '/agent/register/' + self.hostname
    self.heartbeatUrl = config.get('server', 'url') + \
       '/agent/heartbeat/' + self.hostname
     
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
        req = urllib2.Request(self.registerUrl, data, {'Content-Type': 
                                                      'application/json'})
        stream = urllib2.urlopen(req)
        response = stream.read()
        stream.close()
        ret = json.loads(response)
        logger.info("Registered with the server with " + pprint.pformat(ret))
        registered = True
        pass
      except Exception, err:
        logger.info("Unable to connect to: " + self.registerUrl, exc_info = True)
        """ sleep for 30 seconds and then retry again """
        time.sleep(30)
        pass
      pass  
    return ret
  
  def heartbeatWithServer(self):
    retry = False
    #TODO make sure the response id is monotonically increasing
    id = 0
    while True:
      try:
        if retry==False:
          data = json.dumps(self.heartbeat.build(id))
        req = urllib2.Request(self.heartbeatUrl, data, {'Content-Type': 'application/json'})
        f = urllib2.urlopen(req)
        response = f.read()
        f.close()
        data = json.loads(response)
        id=int(data['responseId'])
        logger.info("HeartBeat Response from Server: \n" + pprint.pformat(data))
        retry=False
      except Exception, err:
        retry=True
        if "code" in err:
          logger.error(err.code)
        else:
          logger.error("Unable to connect to: "+self.heartbeatUrl,exc_info=True)
      if self.actionQueue.isIdle():
        time.sleep(30)
      else:
        time.sleep(1) 
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
  formatter = logging.Formatter("%(asctime)s %(filename)s:%(lineno)d - %(message)s")
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
