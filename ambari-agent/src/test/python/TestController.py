#!/usr/bin/env python2.6
# -*- coding: utf-8 -*-

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

import StringIO
import unittest
from ambari_agent import Controller, ActionQueue
from ambari_agent import hostname
import sys
from mock.mock import patch, MagicMock, call
import logging

class TestController(unittest.TestCase):

  logger = logging.getLogger()

  @patch("threading.Thread")
  @patch("threading.Lock")
  @patch.object(Controller, "NetUtil")
  @patch.object(hostname, "hostname")
  def setUp(self, hostname_method, NetUtil_mock, lockMock, threadMock):

    Controller.logger = MagicMock()
    lockMock.return_value = MagicMock()
    NetUtil_mock.return_value = MagicMock()
    hostname_method.return_value = "test_hostname"


    config = MagicMock()
    config.get.return_value = "something"

    self.controller = Controller.Controller(config)
    self.controller.netutil.HEARTBEAT_IDDLE_INTERVAL_SEC = 0.1
    self.controller.netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC = 0.1


  @patch("json.dumps")
  @patch("json.loads")
  @patch("time.sleep")
  @patch("pprint.pformat")
  @patch.object(Controller, "randint")
  def test_registerWithServer(self, randintMock, pformatMock, sleepMock,
                              loadsMock, dumpsMock):

    out = StringIO.StringIO()
    sys.stdout = out

    register = MagicMock()
    self.controller.register = register

    sendRequest = MagicMock()
    self.controller.sendRequest = sendRequest

    dumpsMock.return_value = "request"
    response = {"responseId":1,}
    loadsMock.return_value = response

    self.assertEqual(response, self.controller.registerWithServer())

    response["statusCommands"] = "commands"
    self.controller.addToQueue = MagicMock(name="addToQueue")

    self.assertEqual(response, self.controller.registerWithServer())
    self.controller.addToQueue.assert_called_with("commands")

    calls = []

    def side_effect(*args):
      if len(calls) == 0:
        calls.append(1)
        raise Exception("test")
      return "request"

    del response["statusCommands"]

    dumpsMock.side_effect = side_effect
    self.assertEqual(response, self.controller.registerWithServer())
    self.assertTrue(randintMock.called)
    self.assertTrue(sleepMock.called)

    sys.stdout = sys.__stdout__

    self.controller.sendRequest = Controller.Controller.sendRequest
    self.controller.addToQueue = Controller.Controller.addToQueue


  @patch("pprint.pformat")
  def test_addToQueue(self, pformatMock):

    actionQueue = MagicMock()
    self.controller.actionQueue = actionQueue
    self.controller.addToQueue(None)
    self.assertFalse(actionQueue.put.called)
    self.controller.addToQueue("cmd")
    self.assertTrue(actionQueue.put.called)


  @patch("urllib2.build_opener")
  @patch("urllib2.install_opener")
  @patch.object(Controller, "ActionQueue")
  def test_run(self, ActionQueue_mock, installMock, buildMock):
    aq = MagicMock()
    ActionQueue_mock.return_value = aq

    buildMock.return_value = "opener"
    registerAndHeartbeat  = MagicMock("registerAndHeartbeat")
    calls = []
    def side_effect():
      if len(calls) == 0:
        self.controller.repeatRegistration = True
      calls.append(1)
    registerAndHeartbeat.side_effect = side_effect
    self.controller.registerAndHeartbeat = registerAndHeartbeat

    # repeat registration
    self.controller.run()

    self.assertTrue(buildMock.called)
    installMock.called_once_with("opener")
    self.assertEqual(2, registerAndHeartbeat.call_count)

    # one call, +1
    registerAndHeartbeat.side_effect = None
    self.controller.run()
    self.assertEqual(3, registerAndHeartbeat.call_count)

    # Action queue should be started during calls
    self.assertTrue(ActionQueue_mock.called)
    self.assertTrue(aq.start.called)


  @patch("urllib2.build_opener")
  @patch("urllib2.install_opener")
  @patch.object(ActionQueue.ActionQueue, "run")
  def test_repeatRegistration(self, run_mock, installMock, buildMock):

    registerAndHeartbeat = MagicMock(name="registerAndHeartbeat")

    self.controller.registerAndHeartbeat = registerAndHeartbeat
    self.controller.run()
    self.assertTrue(installMock.called)
    self.assertTrue(buildMock.called)
    self.controller.registerAndHeartbeat.assert_called_once_with()

    calls = []
    def switchBool():
      if len(calls) == 0:
        self.controller.repeatRegistration = True
        calls.append(1)
      self.controller.repeatRegistration = False

    registerAndHeartbeat.side_effect = switchBool
    self.controller.run()
    self.assertEqual(2, registerAndHeartbeat.call_count)

    self.controller.registerAndHeartbeat = \
      Controller.Controller.registerAndHeartbeat


  @patch("time.sleep")
  def test_registerAndHeartbeatWithException(self, sleepMock):

    registerWithServer = MagicMock(name="registerWithServer")
    registerWithServer.return_value = {"response":"resp"}
    self.controller.registerWithServer = registerWithServer
    heartbeatWithServer = MagicMock(name="heartbeatWithServer")
    self.controller.heartbeatWithServer = heartbeatWithServer

    Controller.Controller.__sendRequest__ = MagicMock(side_effect=Exception())

    self.controller.registerAndHeartbeat()
    registerWithServer.assert_called_once_with()
    heartbeatWithServer.assert_called_once_with()

    self.controller.registerWithServer =\
    Controller.Controller.registerWithServer
    self.controller.heartbeatWithServer =\
    Controller.Controller.registerWithServer

  @patch("time.sleep")
  def test_registerAndHeartbeat(self, sleepMock):

    registerWithServer = MagicMock(name="registerWithServer")
    registerWithServer.return_value = {"response":"resp"}
    self.controller.registerWithServer = registerWithServer
    heartbeatWithServer = MagicMock(name="heartbeatWithServer")
    self.controller.heartbeatWithServer = heartbeatWithServer

    self.controller.registerAndHeartbeat()
    registerWithServer.assert_called_once_with()
    heartbeatWithServer.assert_called_once_with()

    self.controller.registerWithServer = \
      Controller.Controller.registerWithServer
    self.controller.heartbeatWithServer = \
      Controller.Controller.registerWithServer


  @patch.object(Controller, "ProcessHelper")
  def test_restartAgent(self, ProcessHelper_mock):

    self.controller.restartAgent()
    self.assertTrue(ProcessHelper_mock.restartAgent.called)


  @patch("urllib2.Request")
  @patch.object(Controller, "security")
  def test_sendRequest(self, security_mock, requestMock):

    conMock = MagicMock()
    conMock.request.return_value = "response"
    security_mock.CachedHTTPSConnection.return_value = conMock
    url = "url"
    data = "data"
    requestMock.return_value = "request"

    self.controller.cachedconnect = None

    self.assertEqual("response", self.controller.sendRequest(url, data))
    security_mock.CachedHTTPSConnection.assert_called_once_with(
      self.controller.config)
    requestMock.called_once_with(url, data,
      {'Content-Type': 'application/json'})


  @patch("time.sleep")
  @patch("json.loads")
  @patch("json.dumps")
  def test_heartbeatWithServer(self, dumpsMock, loadsMock, sleepMock):

    out = StringIO.StringIO()
    sys.stdout = out

    hearbeat = MagicMock()
    self.controller.heartbeat = hearbeat

    dumpsMock.return_value = "data"

    sendRequest = MagicMock(name="sendRequest")
    self.controller.sendRequest = sendRequest

    self.controller.responseId = 1
    response = {"responseId":"2", "restartAgent":"false"}
    loadsMock.return_value = response

    def one_heartbeat(*args, **kwargs):
      self.controller.DEBUG_STOP_HEARTBITTING = True
      return "data"

    sendRequest.side_effect = one_heartbeat

    actionQueue = MagicMock()
    actionQueue.isIdle.return_value = True

    # one successful request, after stop
    self.controller.actionQueue = actionQueue
    self.controller.heartbeatWithServer()
    self.assertTrue(sendRequest.called)

    calls = []
    def retry(*args, **kwargs):
      if len(calls) == 0:
        calls.append(1)
        response["responseId"] = "3"
        raise Exception()
      if len(calls) > 0:
        self.controller.DEBUG_STOP_HEARTBITTING = True
      return "data"

    # exception, retry, successful and stop
    sendRequest.side_effect = retry
    self.controller.DEBUG_STOP_HEARTBITTING = False
    self.controller.heartbeatWithServer()

    self.assertEqual(1, self.controller.DEBUG_SUCCESSFULL_HEARTBEATS)

    # retry registration
    response["registrationCommand"] = "true"
    sendRequest.side_effect = one_heartbeat
    self.controller.DEBUG_STOP_HEARTBITTING = False
    self.controller.heartbeatWithServer()

    self.assertTrue(self.controller.repeatRegistration)

    # wrong responseId => restart
    response = {"responseId":"2", "restartAgent":"false"}
    loadsMock.return_value = response

    restartAgent = MagicMock(name="restartAgent")
    self.controller.restartAgent = restartAgent
    self.controller.DEBUG_STOP_HEARTBITTING = False
    self.controller.heartbeatWithServer()

    restartAgent.assert_called_once_with()

    # executionCommands, statusCommands
    self.controller.responseId = 1
    addToQueue = MagicMock(name="addToQueue")
    self.controller.addToQueue = addToQueue
    response["executionCommands"] = "executionCommands"
    response["statusCommands"] = "statusCommands"
    self.controller.DEBUG_STOP_HEARTBITTING = False
    self.controller.heartbeatWithServer()

    addToQueue.assert_has_calls([call("executionCommands"),
                                 call("statusCommands")])

    # restartAgent command
    self.controller.responseId = 1
    self.controller.DEBUG_STOP_HEARTBITTING = False
    response["restartAgent"] = "true"
    restartAgent = MagicMock(name="restartAgent")
    self.controller.restartAgent = restartAgent
    self.controller.heartbeatWithServer()

    restartAgent.assert_called_once_with()

    # actionQueue not idle
    self.controller.responseId = 1
    self.controller.DEBUG_STOP_HEARTBITTING = False
    actionQueue.isIdle.return_value = False
    response["restartAgent"] = "false"
    self.controller.heartbeatWithServer()

    sleepMock.assert_called_with(
      self.controller.netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC)

    sys.stdout = sys.__stdout__
    self.controller.sendRequest = Controller.Controller.sendRequest
    self.controller.sendRequest = Controller.Controller.addToQueue

if __name__ == "__main__":
  unittest.main(verbosity=2)




