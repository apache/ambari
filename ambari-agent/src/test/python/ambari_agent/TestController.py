#!/usr/bin/env python
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
import ssl
import unittest, threading
import sys
from mock.mock import patch, MagicMock, call, Mock
import logging
import platform
from threading import Event

with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  from ambari_agent import Controller, ActionQueue
  from ambari_agent import hostname
  from ambari_agent.Controller import AGENT_AUTO_RESTART_EXIT_CODE
  from common_functions import OSCheck

@patch.object(platform, "linux_distribution", new = ('Suse','11','Final'))
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
    self.controller.netutil.MINIMUM_INTERVAL_BETWEEN_HEARTBEATS = 0.1
    self.controller.netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC = 0.1


  @patch("json.dumps")
  @patch("time.sleep")
  @patch("pprint.pformat")
  @patch.object(Controller, "randint")
  def test_registerWithServer(self, randintMock, pformatMock, sleepMock,
                              dumpsMock):

    out = StringIO.StringIO()
    sys.stdout = out

    register = MagicMock()
    self.controller.register = register

    self.controller.sendRequest = MagicMock()

    dumpsMock.return_value = "request"
    self.controller.sendRequest.return_value = '{"log":"Error text", "exitstatus":"1"}'

    self.assertEqual({u'exitstatus': u'1', u'log': u'Error text'}, self.controller.registerWithServer())

    self.controller.sendRequest.return_value = '{"responseId":1}'
    self.assertEqual({"responseId":1}, self.controller.registerWithServer())

    self.controller.sendRequest.return_value = '{"responseId":1, "statusCommands": "commands", "log":"", "exitstatus":"0"}'
    self.controller.addToStatusQueue = MagicMock(name="addToStatusQueue")
    self.controller.isRegistered = False
    self.assertEqual({'exitstatus': '0', 'responseId': 1, 'log': '', 'statusCommands': 'commands'}, self.controller.registerWithServer())
    self.controller.addToStatusQueue.assert_called_with("commands")

    calls = []

    def side_effect(*args):
      if len(calls) == 0:
        calls.append(1)
        raise Exception("test")
      return "request"

    self.controller.sendRequest.return_value = '{"responseId":1}'

    dumpsMock.side_effect = side_effect
    self.controller.isRegistered = False
    self.assertEqual({"responseId":1}, self.controller.registerWithServer())
    self.assertTrue(randintMock.called)
    self.assertTrue(sleepMock.called)

    sys.stdout = sys.__stdout__

    self.controller.sendRequest = Controller.Controller.sendRequest
    self.controller.addToStatusQueue = Controller.Controller.addToStatusQueue


  @patch("pprint.pformat")
  def test_addToQueue(self, pformatMock):

    actionQueue = MagicMock()
    self.controller.actionQueue = actionQueue
    self.controller.addToQueue(None)
    self.assertFalse(actionQueue.put.called)
    self.controller.addToQueue("cmd")
    self.assertTrue(actionQueue.put.called)


  @patch("pprint.pformat")
  def test_addToStatusQueue(self, pformatMock):

    actionQueue = MagicMock()
    self.controller.actionQueue = actionQueue
    self.controller.addToStatusQueue(None)
    self.assertFalse(actionQueue.put_status.called)
    self.controller.addToStatusQueue("cmd")
    self.assertTrue(actionQueue.put_status.called)


  @patch("urllib2.build_opener")
  @patch("urllib2.install_opener")
  @patch.object(Controller, "ActionQueue")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_run(self, get_os_version_mock, get_os_type_mock, ActionQueue_mock, installMock, buildMock):
    aq = MagicMock()
    ActionQueue_mock.return_value = aq
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"

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
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_repeatRegistration(self, get_os_version_mock, get_os_type_mock,
                              run_mock, installMock, buildMock):

    registerAndHeartbeat = MagicMock(name="registerAndHeartbeat")
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"

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

    self.controller.isRegistered = True
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

    listener1 = MagicMock()
    listener2 = MagicMock()
    self.controller.registration_listeners.append(listener1)
    self.controller.registration_listeners.append(listener2)
    self.controller.isRegistered = True
    self.controller.registerAndHeartbeat()
    registerWithServer.assert_called_once_with()
    heartbeatWithServer.assert_called_once_with()
    self.assertTrue(listener1.called)
    self.assertTrue(listener2.called)

    self.controller.registerWithServer = \
      Controller.Controller.registerWithServer
    self.controller.heartbeatWithServer = \
      Controller.Controller.registerWithServer


  @patch("time.sleep")
  def test_registerAndHeartbeat_check_registration_listener(self, sleepMock):
    registerWithServer = MagicMock(name="registerWithServer")
    registerWithServer.return_value = {"response":"resp"}
    self.controller.registerWithServer = registerWithServer
    heartbeatWithServer = MagicMock(name="heartbeatWithServer")
    self.controller.heartbeatWithServer = heartbeatWithServer

    self.controller.isRegistered = True
    self.controller.registerAndHeartbeat()
    registerWithServer.assert_called_once_with()
    heartbeatWithServer.assert_called_once_with()

    self.controller.registerWithServer = \
      Controller.Controller.registerWithServer
    self.controller.heartbeatWithServer = \
      Controller.Controller.registerWithServer


  @patch("os._exit")
  def test_restartAgent(self, os_exit_mock):

    self.controller.restartAgent()
    self.assertTrue(os_exit_mock.called)
    self.assertTrue(os_exit_mock.call_args[0][0] == AGENT_AUTO_RESTART_EXIT_CODE)


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


  @patch.object(threading._Event, "wait")
  @patch("time.sleep")
  @patch("json.loads")
  @patch("json.dumps")
  def test_heartbeatWithServer(self, dumpsMock, loadsMock, sleepMock, event_mock):

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
      self.controller.DEBUG_STOP_HEARTBEATING = True
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
        self.controller.DEBUG_STOP_HEARTBEATING = True
      return "data"

    # exception, retry, successful and stop
    sendRequest.side_effect = retry
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    self.assertEqual(1, self.controller.DEBUG_SUCCESSFULL_HEARTBEATS)

    # retry registration
    response["registrationCommand"] = "true"
    sendRequest.side_effect = one_heartbeat
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    self.assertTrue(self.controller.repeatRegistration)

    # components are not mapped
    response["registrationCommand"] = "false"
    response["hasMappedComponents"] = False
    sendRequest.side_effect = one_heartbeat
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    self.assertFalse(self.controller.hasMappedComponents)

    # components are mapped
    response["hasMappedComponents"] = True
    sendRequest.side_effect = one_heartbeat
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    self.assertTrue(self.controller.hasMappedComponents)

    # components are mapped
    del response["hasMappedComponents"]
    sendRequest.side_effect = one_heartbeat
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    self.assertTrue(self.controller.hasMappedComponents)

    # wrong responseId => restart
    response = {"responseId":"2", "restartAgent":"false"}
    loadsMock.return_value = response

    restartAgent = MagicMock(name="restartAgent")
    self.controller.restartAgent = restartAgent
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    restartAgent.assert_called_once_with()

    # executionCommands
    self.controller.responseId = 1
    addToQueue = MagicMock(name="addToQueue")
    self.controller.addToQueue = addToQueue
    response["executionCommands"] = "executionCommands"
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    addToQueue.assert_has_calls([call("executionCommands")])

    # statusCommands
    self.controller.responseId = 1
    addToStatusQueue = MagicMock(name="addToStatusQueue")
    self.controller.addToStatusQueue = addToStatusQueue
    response["statusCommands"] = "statusCommands"
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    addToStatusQueue.assert_has_calls([call("statusCommands")])

    # restartAgent command
    self.controller.responseId = 1
    self.controller.DEBUG_STOP_HEARTBEATING = False
    response["restartAgent"] = "true"
    restartAgent = MagicMock(name="restartAgent")
    self.controller.restartAgent = restartAgent
    self.controller.heartbeatWithServer()

    restartAgent.assert_called_once_with()

    # actionQueue not idle
    self.controller.responseId = 1
    self.controller.DEBUG_STOP_HEARTBEATING = False
    actionQueue.isIdle.return_value = False
    response["restartAgent"] = "false"
    self.controller.heartbeatWithServer()

    sleepMock.assert_called_with(
      self.controller.netutil.MINIMUM_INTERVAL_BETWEEN_HEARTBEATS)

    sys.stdout = sys.__stdout__
    self.controller.sendRequest = Controller.Controller.sendRequest
    self.controller.sendRequest = Controller.Controller.addToQueue
    self.controller.sendRequest = Controller.Controller.addToStatusQueue


  @patch("pprint.pformat")
  @patch("time.sleep")
  @patch("json.loads")
  @patch("json.dumps")
  def test_certSigningFailed(self, dumpsMock, loadsMock, sleepMock, pformatMock):
    register = MagicMock()
    self.controller.register = register

    dumpsMock.return_value = "request"
    response = {"responseId":1,}
    loadsMock.return_value = response

    self.controller.sendRequest = Mock(side_effect=ssl.SSLError())

    self.controller.repeatRegistration=True
    self.controller.registerWithServer()

    #Conroller thread and the agent stop if the repeatRegistration flag is False
    self.assertFalse(self.controller.repeatRegistration)

if __name__ == "__main__":
  unittest.main(verbosity=2)




