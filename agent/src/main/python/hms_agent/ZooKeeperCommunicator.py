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
import logging.handlers
import signal
import simplejson
import socket
import sys, traceback
import time
import threading
import zookeeper

from optparse import OptionParser
from Runner import Runner

logger = logging.getLogger()
options = None
args = None
ZOOKEEPER_OPEN_ACL_UNSAFE = {"perms":0x1f, "scheme":"world", "id" :"anyone"}
timeout = 10
connected = False
conn_cv = threading.Condition()
session_time = 100000

class ZooKeeperCommunicator(threading.Thread):
  hmsZkPrefix = "/clusters"

  def __init__(self, zkservers, credential):
    threading.Thread.__init__(self)
    logger.debug('Initializing ZooKeeperCommunicator thread.')
    zookeeper.set_debug_level(zookeeper.LOG_LEVEL_DEBUG)
    self.zh = None
    self.zkservers = zkservers
    self.lock = threading.Lock()
    self.acl = [ZOOKEEPER_OPEN_ACL_UNSAFE]
    self.safeMode = True
    self.credential = credential

  def auth_callback(self, zh, result_code):
    conn_cv.acquire()
    conn_cv.notify()
    conn_cv.release()

  def conn_callback(self, zh, *args):
    conn_cv.acquire()
    conn_cv.notify()
    conn_cv.release()

  def start(self):
    conn_cv.acquire()
    self.zh = zookeeper.init(self.zkservers, self.conn_callback, session_time)
    conn_cv.wait()
    conn_cv.release()
    if self.credential!=None:
      print "credential: "+self.credential
      conn_cv.acquire()
      zookeeper.add_auth(self.zh, "digest", self.credential, self.auth_callback)
      conn_cv.wait()
      conn_cv.release()
    logger.info("ZooKeeper connection established.")

  def __del__(self):
    zookeeper.close(self.zh)

  def locate(self):
    hostname = socket.gethostname()
    try:
      children = sorted(zookeeper.get_children(self.zh, self.hmsZkPrefix))
      for child in children:
        znode = self.hmsZkPrefix + '/' + child + '/' + hostname
        if zookeeper.exists(self.zh, znode, None)!=None:
          self.znode = znode
          self.actionNode = znode + '/action'
          self.statusNode = '/status'
          stat, acl = zookeeper.get_acl(self.zh, self.statusNode)
          self.acl = acl
          if zookeeper.OK == self.aget():
            self.safeMode = False
          break
    except:
      self.safeMode = True
    if self.safeMode != False:
      logger.warn("Can not locate " + hostname + " in zookeeper, sleep " + str(timeout) + " seconds.")
    if self.lock.locked():
      self.lock.release()
      
  def update(self, zh, node, object):
    buffer = simplejson.dumps(object)
    if zookeeper.exists(zh, node, None) != None:
      zookeeper.delete(zh, node, 0)
    zookeeper.create(zh, node, buffer, self.acl, 0)

  def enqueue(self, zh, node, object):
    buffer = simplejson.dumps(object)
    zookeeper.create(zh, node, buffer, self.acl, zookeeper.SEQUENCE)

  def launch(self, zh, workLogNode, actionNode, statusNode):
    state = {}
    data = zookeeper.get(zh, actionNode, 0)
    jsonp = simplejson.loads(data[0])
    state['cmdPath'] = jsonp['cmdPath']
    state['actionPath'] = actionNode
    state['actionId'] = jsonp['actionId']
    state['host'] = self.znode
    state['status']='STARTING'
    self.update(zh, workLogNode, state)

    logger.info("Launch: "+simplejson.dumps(jsonp))
    dispatcher = Runner()
    try:
      result = dispatcher.run(jsonp)
      logger.info("Result: "+simplejson.dumps(result))
      if "exit_code" in result and result['exit_code']==0:
        state['status']='SUCCEEDED'
      else:
        state['status']='FAILED'
    except:
      logger.exception('Execution error: '+actionNode)
      state['status']='FAILED'
    self.update(zh, workLogNode, state)
    self.enqueue(zh, statusNode, state)

  def aget(self):
    return zookeeper.aget_children(self.zh, self.actionNode, self.queue_watcher, self.queue_callback)

  def queue_watcher(self, zh, event, state, path):
    if zookeeper.OK != self.aget():
      logger.error('Fail to monitor action queue for: '+self.actionNode)
      self.safeMode = True
    
  def queue_callback(self, zh, rc, data):
    if zookeeper.OK == rc:
      try:
        for child in sorted(data):
          action = self.actionNode + '/' + child
          workLog = self.actionNode + '/' + child + '/worklog'
          statusLog = self.statusNode + '/status-'
          """ Launch the task if the task has not been executed """
          if zookeeper.exists(zh, workLog, None) == None:
            self.launch(zh, workLog, action, statusLog)
          else:
            """ If task has been previous launched, check for partial execution """
            buffer = zookeeper.get(zh, workLog, 0)
            state = simplejson.loads(buffer[0])
            """ If task is incompleted in execution, launch again """
            if 'status' in state and state['status'] == 'STARTING':
              logger.info('Relaunch '+child)
              self.launch(zh, workLog, action, statusLog)
            else:
              """ If the task has been launched, and completed, update status queue """
              if zookeeper.exists(zh, statusLog, None) == None:
                logger.info('Update status.')
                self.update(zh, statusLog, state)
      except NoNodeException, err:
        """ Skip no node exception """
      except Exception, err:
        logger.exception(err)
    else:
      if zookeeper.NONODE == rc:
        self.safeMode = True
    if self.lock.locked():
      self.lock.release()

  def run(self):
    self.locate()
    while True:
      try:
        self.lock.acquire()
        if self.safeMode == True:
          time.sleep(timeout)
          zookeeper.close(self.zh)
          conn_cv.acquire()
          self.zh = zookeeper.init(self.zkservers, self.conn_callback, session_time)
          conn_cv.wait()
          conn_cv.release()
          self.locate()
        if self.safeMode == False:
          if zookeeper.OK != zookeeper.aget_children(self.zh, self.actionNode, self.queue_watcher, None):
            logger.error('Fail to monitor action queue for: '+self.actionNode+', activate safe mode.')
            self.safeMode = True
      except Exception, err:
        logger.exception(err)

def main(argv=None):
  # Allow Ctrl-C
  signal.signal(signal.SIGINT, signal.SIG_DFL)

  parser = OptionParser()
  parser.add_option('-v', '--verbose',
    dest='verbose',
    default=False,
    action='store_true',
    help='Verbose logging. (default: %default)')
  parser.add_option('--zkservers',
    dest='zkservers',
    default='localhost:2181',
    help='Comma-separated list of host:port pairs. (default: %default)')
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

  logger.info('Starting Zookeeper python example: %s' % ' '.join(sys.argv))

  zc = ZooKeeperCommunicator("localhost:2181")
  zc.start()
  zc.join()


if __name__ == '__main__':
  main()
