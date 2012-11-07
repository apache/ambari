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
from ambari_agent.ServerStatus import ServerStatus
from ambari_agent.NetUtil import NetUtil
import ambari_agent.main
from threading import Thread
import time
from ambari_agent.Heartbeat import Heartbeat
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent import AmbariConfig
import socket
import os
import logging
from ambari_agent.Controller import Controller
import logging

NON_EXISTING_DOMAIN = 'non-existing-domain43342432.com'
BAD_URL = 'http://www.iana.org/domains/ex222ample/'

class TestConnectionRetries(TestCase):

  logger = logging.getLogger()

  def setUp(self):
    self.logger.disabled = True


  def test_url_checks(self):
    netutil = NetUtil()
    self.assertEquals(netutil.checkURL('http://www.iana.org/domains/example/'), True, "Good url - HTTP code 200")
    self.assertEquals(netutil.checkURL('https://www.iana.org/domains/example/'), True, "Good HTTPS url - HTTP code 200")
    self.assertEquals(netutil.checkURL('http://' + NON_EXISTING_DOMAIN), False, "Not existing domain")
    self.assertEquals(netutil.checkURL(BAD_URL), False, "Bad url")
    self.assertEquals(netutil.checkURL('http://192.168.253.177'), False, "Not reachable IP")

  def test_registration_retries(self):
    netutil = NetUtil()
    netutil.CONNECT_SERVER_RETRY_INTERVAL_SEC=0.1
    retries = netutil.try_to_connect(BAD_URL, 3)
    self.assertEquals(retries, 3)

  def test_infinit_registration_retries(self):
    netutil = NetUtil()
    netutil.CONNECT_SERVER_RETRY_INTERVAL_SEC=0.1
    thread = Thread(target = netutil.try_to_connect, args = (BAD_URL, -1))
    thread.start()
    time.sleep(0.5)
    # I have to stop the thread anyway, so I'll check results later
    threadWasAlive = thread.isAlive()
    netutil.DEBUG_STOP_RETRIES_FLAG = True
    time.sleep(1)
    # Checking results before thread stop
    self.assertEquals(threadWasAlive, True, "Thread should still be retrying to connect")
    # Checking results after thread stop
    self.assertEquals(thread.isAlive(), False, "Thread should stop now")

  def test_heartbeat_retries(self):
    netutil = NetUtil()
    netutil.HEARTBEAT_IDDLE_INTERVAL_SEC=0.1
    netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC=0.1
    #building heartbeat object
    testsPath = os.path.dirname(os.path.realpath(__file__))
    dictPath = testsPath + os.sep + '..' + os.sep + '..' + os.sep + 'main' + os.sep + 'python' + os.sep + 'ambari_agent' + os.sep + 'servicesToPidNames.dict'
    AmbariConfig.config.set('services','serviceToPidMapFile', dictPath)
    actionQueue = ActionQueue(AmbariConfig.AmbariConfig().getConfig())
    heartbeat = Heartbeat(actionQueue)
    # testing controller with our heartbeat and wrong url
    controller = Controller(AmbariConfig.config)
    controller.heartbeat = heartbeat
    controller.heartbeatUrl = BAD_URL
    controller.actionQueue = actionQueue
    controller.logger = self.logger
    controller.netutil = netutil
    thread = Thread(target =  controller.heartbeatWithServer)
    thread.start()
    time.sleep(1)

    # I have to stop the thread anyway, so I'll check results later
    threadWasAlive = thread.isAlive()
    successfull_heartbits0 = controller.DEBUG_SUCCESSFULL_HEARTBEATS
    heartbeat_retries0 = controller.DEBUG_HEARTBEAT_RETRIES
    # Stopping thread
    controller.DEBUG_STOP_HEARTBITTING = True
    time.sleep(1)
    # Checking results before thread stop
    self.assertEquals(threadWasAlive, True, "Heartbeat should be alive now")
    self.assertEquals(successfull_heartbits0, 0, "Heartbeat should not have any success")
    self.assertGreater(heartbeat_retries0, 1, "Heartbeat should retry connecting")
    # Checking results after thread stop
    self.assertEquals(thread.isAlive(), False, "Heartbeat should stop now")
    self.assertEquals(controller.DEBUG_SUCCESSFULL_HEARTBEATS, 0, "Heartbeat should not have any success")

  def tearDown(self):
    self.logger.disabled = False


