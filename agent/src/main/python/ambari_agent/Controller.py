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
from Heartbeat import Heartbeat
from ActionQueue import ActionQueue

from optparse import OptionParser

logger = logging.getLogger()

class Controller(threading.Thread):

  def __init__(self, url, credential=None):
    threading.Thread.__init__(self)
    logger.debug('Initializing Controller RPC thread.')
    self.lock = threading.Lock()
    self.safeMode = True
    self.credential = credential
    self.url = url + '/agent/controller/heartbeat/' + socket.gethostname()

  def start(self):
    self.actionQueue = ActionQueue()
    self.actionQueue.start()
    self.heartbeat = Heartbeat(self.actionQueue)
    logger.info("Controller connection established.")

  def __del__(self):
    logger.info("Controller connection disconnected.")

  def run(self):
    id='unknown'
    if self.credential!=None:
      auth_handler = urllib2.HTTPBasicAuthHandler()
      auth_handler.add_password(realm="Controller",
                                uri=self.url,
                                user=self.credential['user'],
                                passwd=self.credential['password'])
      opener = urllib2.build_opener(auth_handler)
      urllib2.install_opener(opener)
    while True:
      try:
        data = json.dumps(self.heartbeat.build(id))
        logger.info(data)
        req = urllib2.Request(self.url, data, {'Content-Type': 'application/json'})
        f = urllib2.urlopen(req)
        response = f.read()
        f.close()
        data = json.loads(response)
        id=data['responseId']
        self.actionQueue.put(data)
      except URLError, err:
        logger.error(err.code)
      if self.actionQueue.isIdle():
        time.sleep(30)
      else:
        time.sleep(1)

def main(argv=None):
  # Allow Ctrl-C
  signal.signal(signal.SIGINT, signal.SIG_DFL)

  parser = OptionParser()
  parser.add_option('-v', '--verbose',
    dest='verbose',
    default=False,
    action='store_true',
    help='Verbose logging. (default: %default)')
  parser.add_option('--controller',
    dest='url',
    default='http://localhost:4080',
    help='Controller URL. (default: %default)')
  global options
  global args
  (options, args) = parser.parse_args()

  if options.verbose:
    logger.setLevel(logging.DEBUG)
  else:
    logger.setLevel(logging.INFO)
  formatter = logging.Formatter("%(asctime)s %(filename)s:%(lineno)d - %(message)s")
  stream_handler = logging.StreamHandler()
  stream_handler.setFormatter(formatter)
  logger.addHandler(stream_handler)

  logger.info('Starting Controller RPC Thread: %s' % ' '.join(sys.argv))

  credential = { 'user' : 'controller', 'password' : 'controller' }
  controller = Controller(options.url, credential)
  controller.start()
  controller.run()


if __name__ == '__main__':
  main()
