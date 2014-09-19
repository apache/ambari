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
import json

with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  from ambari_agent import Controller, ActionQueue, Register
  from ambari_agent import hostname
  from ambari_agent.Controller import AGENT_AUTO_RESTART_EXIT_CODE
  from ambari_commons import OSCheck

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
    #config.get.return_value = "something"
    config.get.return_value = "5"

    self.controller = Controller.Controller(config)
    self.controller.netutil.MINIMUM_INTERVAL_BETWEEN_HEARTBEATS = 0.1
    self.controller.netutil.HEARTBEAT_NOT_IDDLE_INTERVAL_SEC = 0.1


  @patch("json.dumps")
  @patch("time.sleep")
  @patch("pprint.pformat")
  @patch.object(Controller, "randint")
  @patch.object(Controller, "LiveStatus")
  def test_registerWithServer(self, LiveStatus_mock, randintMock, pformatMock, sleepMock,
                              dumpsMock):

    out = StringIO.StringIO()
    sys.stdout = out

    LiveStatus_mock.SERVICES = ["foo"]
    LiveStatus_mock.CLIENT_COMPONENTS = ["foo"]
    LiveStatus_mock.COMPONENTS = ["foo"]

    register = MagicMock()
    self.controller.register = register

    self.controller.sendRequest = MagicMock()

    dumpsMock.return_value = '{"valid_object": true}'
    self.controller.sendRequest.return_value = {"log":"Error text", "exitstatus":"1"}

    self.assertEqual({u'exitstatus': u'1', u'log': u'Error text'}, self.controller.registerWithServer())
    self.assertEqual(LiveStatus_mock.SERVICES, [])
    self.assertEqual(LiveStatus_mock.CLIENT_COMPONENTS, [])
    self.assertEqual(LiveStatus_mock.COMPONENTS, [])

    self.controller.sendRequest.return_value = {"responseId":1}
    self.assertEqual({"responseId":1}, self.controller.registerWithServer())

    self.controller.sendRequest.return_value = {"responseId":1, "statusCommands": "commands", "log":"", "exitstatus":"0"}
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

    self.controller.sendRequest.return_value = {"responseId":1}

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
  @patch.object(Controller, "LiveStatus")
  def test_addToStatusQueue(self, LiveStatus_mock, pformatMock):
    LiveStatus_mock.SERVICES = ["foo"]
    LiveStatus_mock.CLIENT_COMPONENTS = ["foo"]
    LiveStatus_mock.COMPONENTS = ["foo"]
    commands = json.loads('[{"clusterName":"dummy_cluster"}]')
    actionQueue = MagicMock()
    self.controller.actionQueue = actionQueue
    updateComponents = Mock()
    self.controller.updateComponents = updateComponents
    self.controller.addToStatusQueue(None)
    self.assertFalse(actionQueue.put_status.called)
    self.assertFalse(updateComponents.called)
    self.controller.addToStatusQueue(commands)
    self.assertTrue(actionQueue.put_status.called)
    self.assertFalse(updateComponents.called)
    LiveStatus_mock.SERVICES = []
    LiveStatus_mock.CLIENT_COMPONENTS = []
    LiveStatus_mock.COMPONENTS = []
    self.controller.addToStatusQueue(commands)
    self.assertTrue(updateComponents.called)
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
    actionQueue = MagicMock(name="actionQueue")
    self.controller.actionQueue = actionQueue

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
    actionQueue = MagicMock(name="actionQueue")
    self.controller.actionQueue = actionQueue

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
    actionQueue = MagicMock(name="actionQueue")
    self.controller.actionQueue = actionQueue

    self.controller.isRegistered = True
    self.controller.registerAndHeartbeat()
    registerWithServer.assert_called_once_with()
    heartbeatWithServer.assert_called_once_with()

    self.controller.registerWithServer = \
      Controller.Controller.registerWithServer
    self.controller.heartbeatWithServer = \
      Controller.Controller.registerWithServer


  @patch("time.sleep")
  @patch.object(Controller.Controller, "sendRequest")
  def test_registerWithIOErrors(self, sendRequestMock, sleepMock):
    # Check that server continues to heartbeat after connection errors
    registerMock = MagicMock(name="Register")
    registerMock.build.return_value = {}
    actionQueue = MagicMock()
    actionQueue.isIdle.return_value = True
    self.controller.actionQueue = actionQueue
    self.controller.register = registerMock
    self.controller.responseId = 1
    self.controller.TEST_IOERROR_COUNTER = 1
    self.controller.isRegistered = False
    def util_throw_IOErrors(*args, **kwargs):
      """
      Throws IOErrors 10 times and then stops heartbeats/registrations
      """
      if self.controller.TEST_IOERROR_COUNTER == 10:
        self.controller.isRegistered = True
      self.controller.TEST_IOERROR_COUNTER += 1
      raise IOError("Sample error")
    actionQueue.isIdle.return_value = False
    sendRequestMock.side_effect = util_throw_IOErrors
    self.controller.registerWithServer()
    self.assertTrue(sendRequestMock.call_count > 5)


  @patch("os._exit")
  def test_restartAgent(self, os_exit_mock):

    self.controller.restartAgent()
    self.assertTrue(os_exit_mock.called)
    self.assertTrue(os_exit_mock.call_args[0][0] == AGENT_AUTO_RESTART_EXIT_CODE)


  @patch("urllib2.Request")
  @patch.object(Controller, "security")
  def test_sendRequest(self, security_mock, requestMock):

    conMock = MagicMock()
    security_mock.CachedHTTPSConnection.return_value = conMock
    url = "http://ambari.apache.org:8081/agent"
    data = "data"
    requestMock.return_value = "request"

    self.controller.cachedconnect = None

    conMock.request.return_value = '{"valid_object": true}'
    actual = self.controller.sendRequest(url, data)
    expected = json.loads('{"valid_object": true}')
    self.assertEqual(actual, expected)
    
    security_mock.CachedHTTPSConnection.assert_called_once_with(
      self.controller.config)
    requestMock.called_once_with(url, data,
      {'Content-Type': 'application/json'})

    conMock.request.return_value = '{invalid_object}'

    try:
      self.controller.sendRequest(url, data)
      self.fail("Should throw exception!")
    except IOError, e: # Expected
      self.assertEquals('Response parsing failed! Request data: ' + data +
                        '; Response: {invalid_object}', str(e))

    exceptionMessage = "Connection Refused"
    conMock.request.side_effect = Exception(exceptionMessage)
    try:
      self.controller.sendRequest(url, data)
      self.fail("Should throw exception!")
    except IOError, e: # Expected
      self.assertEquals('Request to ' + url + ' failed due to ' +
                        exceptionMessage, str(e))


  @patch.object(threading._Event, "wait")
  @patch("time.sleep")
  @patch("json.dumps")
  def test_heartbeatWithServer(self, dumpsMock, sleepMock, event_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    hearbeat = MagicMock()
    self.controller.heartbeat = hearbeat

    dumpsMock.return_value = "data"

    sendRequest = MagicMock(name="sendRequest")
    self.controller.sendRequest = sendRequest

    self.controller.responseId = 1
    response = {"responseId":"2", "restartAgent":"false"}
    sendRequest.return_value = response

    def one_heartbeat(*args, **kwargs):
      self.controller.DEBUG_STOP_HEARTBEATING = True
      return response

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
      return response

    # exception, retry, successful and stop
    sendRequest.side_effect = retry
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    self.assertEqual(1, self.controller.DEBUG_SUCCESSFULL_HEARTBEATS)

    # retry registration
    self.controller.responseId = 2
    response["registrationCommand"] = "true"
    sendRequest.side_effect = one_heartbeat
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    self.assertTrue(self.controller.repeatRegistration)

    # components are not mapped
    self.controller.responseId = 2
    response["registrationCommand"] = "false"
    response["hasMappedComponents"] = False
    sendRequest.side_effect = one_heartbeat
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    self.assertFalse(self.controller.hasMappedComponents)

    # components are mapped
    self.controller.responseId = 2
    response["hasMappedComponents"] = True
    sendRequest.side_effect = one_heartbeat
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    self.assertTrue(self.controller.hasMappedComponents)

    # components are mapped
    self.controller.responseId = 2
    del response["hasMappedComponents"]
    sendRequest.side_effect = one_heartbeat
    self.controller.DEBUG_STOP_HEARTBEATING = False
    self.controller.heartbeatWithServer()

    self.assertTrue(self.controller.hasMappedComponents)

    # wrong responseId => restart
    self.controller.responseId = 2
    response = {"responseId":"2", "restartAgent":"false"}

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

    # Check that server continues to heartbeat after connection errors
    self.controller.responseId = 1
    self.controller.TEST_IOERROR_COUNTER = 1
    sendRequest.reset()
    def util_throw_IOErrors(*args, **kwargs):
      """
      Throws IOErrors 100 times and then stops heartbeats/registrations
      """
      if self.controller.TEST_IOERROR_COUNTER == 10:
        self.controller.DEBUG_STOP_HEARTBEATING = True
      self.controller.TEST_IOERROR_COUNTER += 1
      raise IOError("Sample error")
    self.controller.DEBUG_STOP_HEARTBEATING = False
    actionQueue.isIdle.return_value = False
    sendRequest.side_effect = util_throw_IOErrors
    self.controller.heartbeatWithServer()
    self.assertTrue(sendRequest.call_count > 5)

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

  @patch.object(Controller, "LiveStatus")
  def test_updateComponents(self, LiveStatus_mock):
    LiveStatus_mock.SERVICES = []
    LiveStatus_mock.CLIENT_COMPONENTS = []
    LiveStatus_mock.COMPONENTS = []
    self.controller.componentsUrl = "foo_url/"
    sendRequest = Mock()
    self.controller.sendRequest = sendRequest
    self.controller.sendRequest.return_value = {"clusterName":"dummy_cluster_name",
                                                "stackName":"dummy_stack_name",
                                                "stackVersion":"dummy_stack_version",
                                                "components":{"PIG":{"PIG":"CLIENT"},
                                                "MAPREDUCE":{"MAPREDUCE_CLIENT":"CLIENT",
                                                "JOBTRACKER":"MASTER","TASKTRACKER":"SLAVE"}}}
    self.controller.updateComponents("dummy_cluster_name")
    sendRequest.assert_called_with('foo_url/dummy_cluster_name', None)
    services_expected = [u'MAPREDUCE', u'PIG']
    client_components_expected = [
      {'serviceName':u'MAPREDUCE','componentName':u'MAPREDUCE_CLIENT'},
      {'serviceName':u'PIG','componentName':u'PIG'}
    ]
    components_expected = [
      {'serviceName':u'MAPREDUCE','componentName':u'TASKTRACKER'},
      {'serviceName':u'MAPREDUCE','componentName':u'JOBTRACKER'}
    ]
    self.assertEquals(LiveStatus_mock.SERVICES, services_expected)
    self.assertEquals(LiveStatus_mock.CLIENT_COMPONENTS, client_components_expected)
    self.assertEquals(LiveStatus_mock.COMPONENTS, components_expected)


if __name__ == "__main__":
  unittest.main(verbosity=2)




