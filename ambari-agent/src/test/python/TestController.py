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

from unittest import TestCase
from ambari_agent.Register import Register
from ambari_agent.Controller import Controller
from ambari_agent.Heartbeat import Heartbeat
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent import AmbariConfig
from ambari_agent.NetUtil import NetUtil
import socket, ConfigParser, logging
import os, pprint, json, sys
from threading import Thread
import time
import Queue


BAD_URL = 'http://localhost:54222/badurl/'

logger = logging.getLogger()

class TestController(TestCase):

  def setUp(self):
    logger.disabled = True
    self.defaulttimeout = -1.0
    if hasattr(socket, 'getdefaulttimeout'):
      # get the default timeout on sockets
      self.defaulttimeout = socket.getdefaulttimeout()


  def tearDown(self):
    if self.defaulttimeout is not None and self.defaulttimeout > 0 and hasattr(socket, 'setdefaulttimeout'):
      # Set the default timeout on sockets
      socket.setdefaulttimeout(self.defaulttimeout)
    logger.disabled = False


  def test_reregister_loop(self):
    class ControllerMock(Controller):
      def __init__(self, config, range=0):
        self.repeatRegistration = False
        self.range = range

      callCounter = 0

      def registerAndHeartbeat(self):
        if self.callCounter < 3:
          self.repeatRegistration = True;
          self.callCounter += 1
        else:
          self.repeatRegistration = False;

    config = ConfigParser.RawConfigParser()
    mock = ControllerMock(config)
    mock.run()
    self.assertEquals(mock.callCounter, 3)
    pass


  def test_nonincremental_ids1(self):
    '''
      test to make sure nothing we act appropriately on getting non incremental reponse ids
    '''
    #timings adjustment
    netutil = NetUtil()
    netutil.HEARTBEAT_IDDLE_INTERVAL_SEC=0.05
    netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC=0.05
    #building fake responces
    responces = Queue.Queue()
    responce1 = {
      'responseId':8,
      'executionCommands':[],
      'statusCommands':[],
      'restartAgent':'False',
      }
    responce1 = json.dumps(responce1)

    responce2 = {
      'responseId':11,
      'executionCommands':[],
      'statusCommands':[],
      'restartAgent':'False',
      }
    responce2 = json.dumps(responce2)
    responces.put(responce1)
    responces.put(responce2)
    #building heartbeat object
    testsPath = os.path.dirname(os.path.realpath(__file__))
    dictPath = testsPath + os.sep + '..' + os.sep + '..' + os.sep + 'main' + os.sep + 'python' + os.sep + 'ambari_agent' + os.sep + 'servicesToPidNames.dict'
    AmbariConfig.config.set('services','serviceToPidMapFile', dictPath)
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    heartbeat = Heartbeat(actionQueue)
    # testing controller with our heartbeat
    controller = self.ControllerMock_fake_restartAgent(AmbariConfig.config, responces)
    controller.heartbeat = heartbeat
    controller.actionQueue = actionQueue
    controller.logger = logger
    controller.netutil = netutil
    controller.heartbeatWithServer()
    restarts = controller.restartCount
    self.assertEquals(restarts, 1, "Agent should restart on non incremental responce ids")
    pass


  def test_nonincremental_ids2(self):
    '''
      test to make sure nothing we act appropriately on getting incremental reponse ids
    '''
    #timings adjustment
    netutil = NetUtil()
    netutil.HEARTBEAT_IDDLE_INTERVAL_SEC=0.05
    netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC=0.05
    #building fake responces
    responces = Queue.Queue()
    responce1 = {
      'responseId':8,
      'executionCommands':[],
      'statusCommands':[],
      'restartAgent':'False',
      }
    responce1 = json.dumps(responce1)

    responce2 = {
      'responseId':9,
      'executionCommands':[],
      'statusCommands':[],
      'restartAgent':'False',
      }
    responce2 = json.dumps(responce2)
    responces.put(responce1)
    responces.put(responce2)
    #building heartbeat object
    testsPath = os.path.dirname(os.path.realpath(__file__))
    dictPath = testsPath + os.sep + '..' + os.sep + '..' + os.sep + 'main' + os.sep + 'python' + os.sep + 'ambari_agent' + os.sep + 'servicesToPidNames.dict'
    AmbariConfig.config.set('services','serviceToPidMapFile', dictPath)
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    heartbeat = Heartbeat(actionQueue)
    # testing controller with our heartbeat
    controller = self.ControllerMock_fake_restartAgent(AmbariConfig.config, responces)
    controller.heartbeat = heartbeat
    controller.actionQueue = actionQueue
    controller.logger = logger
    controller.netutil = netutil
    controller.heartbeatWithServer()
    restarts = controller.restartCount
    self.assertEquals(restarts, 0, "Agent should not restart on incremental responce ids")
    pass


  def test_reregister(self):
    '''
      test to make sure if we can get a re register command, we register with the server
    '''
    #timings adjustment
    netutil = NetUtil()
    netutil.HEARTBEAT_IDDLE_INTERVAL_SEC=0.05
    netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC=0.05
    #building fake responces
    responces = Queue.Queue()
    responce1 = {
      'responseId':8,
      'executionCommands':[],
      'statusCommands':[],
      'restartAgent':'true',
      }
    responce1 = json.dumps(responce1)
    responces.put(responce1)
    #building heartbeat object
    testsPath = os.path.dirname(os.path.realpath(__file__))
    dictPath = testsPath + os.sep + '..' + os.sep + '..' + os.sep + 'main' + os.sep + 'python' + os.sep + 'ambari_agent' + os.sep + 'servicesToPidNames.dict'
    AmbariConfig.config.set('services','serviceToPidMapFile', dictPath)
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    heartbeat = Heartbeat(actionQueue)
    # testing controller with our heartbeat
    controller = self.ControllerMock_fake_restartAgent(AmbariConfig.config, responces)
    controller.heartbeat = heartbeat
    controller.actionQueue = actionQueue
    controller.logger = logger
    controller.netutil = netutil
    controller.heartbeatWithServer()
    restarts = controller.restartCount
    self.assertEquals(restarts, 1, "Agent should restart if we get a re register command")


  def test_heartbeat_retries(self):
    netutil = NetUtil()
    netutil.HEARTBEAT_IDDLE_INTERVAL_SEC=0.05
    netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC=0.05
    #building heartbeat object
    testsPath = os.path.dirname(os.path.realpath(__file__))
    dictPath = testsPath + os.sep + '..' + os.sep + '..' + os.sep + 'main' + os.sep + 'python' + os.sep + 'ambari_agent' + os.sep + 'servicesToPidNames.dict'
    AmbariConfig.config.set('services','serviceToPidMapFile', dictPath)
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    heartbeat = Heartbeat(actionQueue)
    # testing controller with our heartbeat and wrong url
    controller = self.ControllerMock_failure_sendRequest(AmbariConfig.config)
    controller.heartbeat = heartbeat
    controller.actionQueue = actionQueue
    controller.logger = logger
    controller.netutil = netutil
    thread = Thread(target =  controller.heartbeatWithServer)
    thread.start()
    time.sleep(0.5)

    # I have to stop the thread anyway, so I'll check results later
    threadWasAlive = thread.isAlive()
    successfull_heartbits0 = controller.DEBUG_SUCCESSFULL_HEARTBEATS
    heartbeat_retries0 = controller.DEBUG_HEARTBEAT_RETRIES
    # Stopping thread
    controller.DEBUG_STOP_HEARTBITTING = True
    time.sleep(0.3)
    # Checking results before thread stop
    self.assertEquals(threadWasAlive, True, "Heartbeat should be alive now")
    self.assertEquals(successfull_heartbits0, 0, "Heartbeat should not have any success")
    self.assertEquals(heartbeat_retries0 > 1, True, "Heartbeat should retry connecting")
    # Checking results after thread stop
    self.assertEquals(thread.isAlive(), False, "Heartbeat should stop now")
    self.assertEquals(controller.DEBUG_SUCCESSFULL_HEARTBEATS, 0, "Heartbeat should not have any success")


  def test_status_command_on_registration(self):
    '''
    test to make sure if we get a status check command from the server, we are able to evaluate and register at the server
    '''
    #timings adjustment
    netutil = NetUtil()
    netutil.HEARTBEAT_IDDLE_INTERVAL_SEC=0.05
    netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC=0.05
    #building fake registration responce
    responces = Queue.Queue()
    responce1 = {
      'response':'OK',
      'responseId':8,
      'statusCommands':[{
        'clusterName' : "c1",
        'commandType' : "STATUS_COMMAND",
        'componentName' : "NAMENODE",
        'serviceName' : "HDFS",
        }],
      }
    responce1 = json.dumps(responce1)
    responces.put(responce1)
    #building heartbeat object
    testsPath = os.path.dirname(os.path.realpath(__file__))
    dictPath = testsPath + os.sep + '..' + os.sep + '..' + os.sep + 'main' + os.sep + 'python' + os.sep + 'ambari_agent' + os.sep + 'servicesToPidNames.dict'
    AmbariConfig.config.set('services','serviceToPidMapFile', dictPath)
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    heartbeat = Heartbeat(actionQueue)
    # testing controller with our heartbeat
    controller = self.ControllerMock_fake_restartAgent(AmbariConfig.config, responces)
    controller.heartbeat = heartbeat
    controller.actionQueue = actionQueue
    controller.logger = logger
    controller.netutil = netutil
    controller.registerWithServer()
    # If test does not hang, registration is successful
    # So, checking queue
    queue = controller.actionQueue.getCommandQueue()
    self.assertEquals(queue.qsize(), 1, "Status command should be queued once")
    # Checking parsed status command
    command = queue.get()
    self.assertEquals(command['clusterName'], 'c1')
    self.assertEquals(command['commandType'], 'STATUS_COMMAND')
    self.assertEquals(command['componentName'], 'NAMENODE')
    self.assertEquals(command['serviceName'], 'HDFS')


  class ControllerMock_fake_restartAgent(Controller):
    def __init__(self, config, responces, range=3):
      self.repeatRegistration = False
      self.responces = responces
      self.heartbeatUrl = "fakeurl"
      self.registerUrl = "fakeregisterurl"
      self.responseId = 7
      self.register = Register()
      self.range = range
      
    def restartAgent(self):
      self.restartCount += 1
      pass

    restartCount = 0

    def sendRequest(self, url, data):
      responce = self.responces.get(block=False)
      if self.responces.empty():
        self.DEBUG_STOP_HEARTBITTING = True # Because we have nothing to reply next time
      return responce


  class ControllerMock_failure_sendRequest(Controller):
    def __init__(self, config, range=0):
      self.repeatRegistration = False
      self.heartbeatUrl = "fakeurl"
      self.registerUrl = "fakeregisterurl"
      self.responseId = 7
      self.register = Register()
      self.range = range

    def restartAgent(self):
      self.restartCount += 1
      pass

    restartCount = 0

    def sendRequest(self, url, data):
      raise Exception("Fake exception")
