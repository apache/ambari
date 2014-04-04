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
import StringIO
import sys
from mock.mock import MagicMock, patch, ANY
import unittest
import logging
import signal
import ConfigParser
import os
import tempfile
from optparse import OptionParser

with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  from ambari_agent import NetUtil, security
  from ambari_agent import ProcessHelper, main
  from ambari_agent import ProcessHelper, main
  from ambari_agent.AmbariConfig import AmbariConfig
  from ambari_agent.PingPortListener import PingPortListener
  from ambari_agent.Controller import Controller
  from ambari_agent.DataCleaner import DataCleaner


class TestMain(unittest.TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__


  @patch("os._exit")
  @patch("os.getpid")
  @patch.object(ProcessHelper, "stopAgent")
  def test_signal_handler(self, stopAgent_mock, os_getpid_mock, os_exit_mock):
    # testing exit of children
    main.agentPid = 4444
    os_getpid_mock.return_value = 5555
    main.signal_handler("signum", "frame")
    self.assertTrue(os_exit_mock.called)

    os_exit_mock.reset_mock()

    # testing exit of main process
    os_getpid_mock.return_value = main.agentPid
    main.signal_handler("signum", "frame")
    self.assertFalse(os_exit_mock.called)
    self.assertTrue(stopAgent_mock.called)


  @patch.object(main.logger, "addHandler")
  @patch.object(main.logger, "setLevel")
  @patch("logging.handlers.RotatingFileHandler")
  def test_setup_logging(self, rfh_mock, setLevel_mock, addHandler_mock):
    # Testing silent mode
    main.setup_logging(False)
    self.assertTrue(addHandler_mock.called)
    setLevel_mock.assert_called_with(logging.INFO)

    addHandler_mock.reset_mock()
    setLevel_mock.reset_mock()

    # Testing verbose mode
    main.setup_logging(True)
    self.assertTrue(addHandler_mock.called)
    setLevel_mock.assert_called_with(logging.DEBUG)


  @patch.object(main.logger, "setLevel")
  @patch("logging.basicConfig")
  def test_update_log_level(self, basicConfig_mock, setLevel_mock):
    config = AmbariConfig().getConfig()

    # Testing with default setup (config file does not contain loglevel entry)
    # Log level should not be changed
    config.set('agent', 'loglevel', None)
    main.update_log_level(config)
    self.assertFalse(setLevel_mock.called)

    setLevel_mock.reset_mock()

    # Testing debug mode
    config.set('agent', 'loglevel', 'DEBUG')
    main.update_log_level(config)
    setLevel_mock.assert_called_with(logging.DEBUG)
    setLevel_mock.reset_mock()

    # Testing any other mode
    config.set('agent', 'loglevel', 'INFO')
    main.update_log_level(config)
    setLevel_mock.assert_called_with(logging.INFO)

    setLevel_mock.reset_mock()

    config.set('agent', 'loglevel', 'WRONG')
    main.update_log_level(config)
    setLevel_mock.assert_called_with(logging.INFO)


  @patch("signal.signal")
  def test_bind_signal_handlers(self, signal_mock):
    main.bind_signal_handlers()
    # Check if on SIGINT/SIGTERM agent is configured to terminate
    signal_mock.assert_any_call(signal.SIGINT, main.signal_handler)
    signal_mock.assert_any_call(signal.SIGTERM, main.signal_handler)
    # Check if on SIGUSR1 agent is configured to fall into debug
    signal_mock.assert_any_call(signal.SIGUSR1, main.debug)


  @patch("os.path.exists")
  @patch("ConfigParser.RawConfigParser.read")
  def test_resolve_ambari_config(self, read_mock, exists_mock):
    # Trying case if conf file exists
    exists_mock.return_value = True
    main.resolve_ambari_config()
    self.assertTrue(read_mock.called)

    exists_mock.reset_mock()
    read_mock.reset_mock()

    # Trying case if conf file does not exist
    exists_mock.return_value = False
    main.resolve_ambari_config()
    self.assertFalse(read_mock.called)


  @patch("sys.exit")
  @patch("os.path.isfile")
  @patch("os.path.isdir")
  @patch("hostname.hostname")
  def test_perform_prestart_checks(self, hostname_mock, isdir_mock, isfile_mock, exit_mock):
    main.config = AmbariConfig().getConfig()

    # Check expected hostname test
    hostname_mock.return_value = "test.hst"

    main.perform_prestart_checks("another.hst")
    self.assertTrue(exit_mock.called)

    exit_mock.reset_mock()

    # Trying case if there is another instance running
    isfile_mock.return_value = True
    isdir_mock.return_value = True
    main.perform_prestart_checks(None)
    self.assertTrue(exit_mock.called)

    isfile_mock.reset_mock()
    isdir_mock.reset_mock()
    exit_mock.reset_mock()

    # Trying case if agent prefix dir does not exist
    isfile_mock.return_value = False
    isdir_mock.return_value = False
    main.perform_prestart_checks(None)
    self.assertTrue(exit_mock.called)

    isfile_mock.reset_mock()
    isdir_mock.reset_mock()
    exit_mock.reset_mock()

    # Trying normal case
    isfile_mock.return_value = False
    isdir_mock.return_value = True
    main.perform_prestart_checks(None)
    self.assertFalse(exit_mock.called)


  @patch("time.sleep")
  @patch("os.kill")
  @patch("os._exit")
  @patch("os.path.exists")
  def test_daemonize_and_stop(self, exists_mock, _exit_mock, kill_mock, sleep_mock):
    oldpid = ProcessHelper.pidfile
    pid = str(os.getpid())
    _, tmpoutfile = tempfile.mkstemp()
    ProcessHelper.pidfile = tmpoutfile

    # Test daemonization
    main.daemonize()
    saved = open(ProcessHelper.pidfile, 'r').read()
    self.assertEqual(pid, saved)

    # Reuse pid file when testing agent stop
    # Testing normal exit
    exists_mock.return_value = False
    main.stop_agent()
    kill_mock.assert_called_with(int(pid), signal.SIGTERM)
    _exit_mock.assert_called_with(0)

    # Restore
    kill_mock.reset_mock()
    _exit_mock.reset_mock()

    # Testing exit when failed to remove pid file
    exists_mock.return_value = True
    main.stop_agent()
    kill_mock.assert_any_call(int(pid), signal.SIGTERM)
    kill_mock.assert_any_call(int(pid), signal.SIGKILL)
    _exit_mock.assert_called_with(1)

    # Restore
    ProcessHelper.pidfile = oldpid
    os.remove(tmpoutfile)


  @patch.object(main, "setup_logging")
  @patch.object(main, "bind_signal_handlers")
  @patch.object(main, "stop_agent")
  @patch.object(main, "resolve_ambari_config")
  @patch.object(main, "perform_prestart_checks")
  @patch.object(main, "daemonize")
  @patch.object(main, "update_log_level")
  @patch.object(NetUtil.NetUtil, "try_to_connect")
  @patch.object(Controller, "__init__")
  @patch.object(Controller, "start")
  @patch.object(Controller, "join")
  @patch("optparse.OptionParser.parse_args")
  @patch.object(DataCleaner,"start")
  @patch.object(DataCleaner,"__init__")
  @patch.object(PingPortListener,"start")
  @patch.object(PingPortListener,"__init__")
  def test_main(self, ping_port_init_mock, ping_port_start_mock, data_clean_init_mock,data_clean_start_mock,
                parse_args_mock, join_mock, start_mock, Controller_init_mock, try_to_connect_mock,
                update_log_level_mock, daemonize_mock, perform_prestart_checks_mock,
                resolve_ambari_config_mock, stop_mock, bind_signal_handlers_mock, setup_logging_mock):
    data_clean_init_mock.return_value = None
    Controller_init_mock.return_value = None
    ping_port_init_mock.return_value = None
    options = MagicMock()
    parse_args_mock.return_value = (options, MagicMock)

    #testing call without command-line arguments
    main.main()

    self.assertTrue(setup_logging_mock.called)
    self.assertTrue(bind_signal_handlers_mock.called)
    self.assertTrue(stop_mock.called)
    self.assertTrue(resolve_ambari_config_mock.called)
    self.assertTrue(perform_prestart_checks_mock.called)
    self.assertTrue(daemonize_mock.called)
    self.assertTrue(update_log_level_mock.called)
    try_to_connect_mock.assert_called_once_with(ANY, -1, ANY)
    self.assertTrue(start_mock.called)
    self.assertTrue(data_clean_init_mock.called)
    self.assertTrue(data_clean_start_mock.called)
    self.assertTrue(ping_port_init_mock.called)
    self.assertTrue(ping_port_start_mock.called)

    perform_prestart_checks_mock.reset_mock()

    # Testing call with --expected-hostname parameter
    options.expected_hostname = "test.hst"
    main.main()
    perform_prestart_checks_mock.assert_called_once_with(options.expected_hostname)
