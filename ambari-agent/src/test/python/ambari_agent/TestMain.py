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
import unittest
import logging
import signal
import os
import socket
import tempfile
import ConfigParser

from ambari_commons import OSCheck
from only_for_platform import get_platform, not_for_platform, only_for_platform, PLATFORM_WINDOWS, PLATFORM_LINUX
from mock.mock import MagicMock, patch, ANY, Mock, call

if get_platform() != PLATFORM_WINDOWS:
  os_distro_value = ('Suse','11','Final')
else:
  os_distro_value = ('win2012serverr2','6.3','WindowsServer')

with patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value)):
  from ambari_agent import NetUtil, security
  from ambari_agent import ProcessHelper, main
  from ambari_agent.AmbariConfig import AmbariConfig
  from ambari_agent.PingPortListener import PingPortListener
  from ambari_agent.Controller import Controller
  from ambari_agent.DataCleaner import DataCleaner
  import ambari_agent.HeartbeatHandlers as HeartbeatHandlers
  from ambari_commons.os_check import OSConst, OSCheck
  from ambari_agent.ExitHelper import ExitHelper
  if get_platform() != PLATFORM_WINDOWS:
    from ambari_commons.shell import shellRunnerLinux

class TestMain(unittest.TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  @only_for_platform(PLATFORM_LINUX)
  @patch("ambari_agent.HeartbeatHandlers.HeartbeatStopHandlersLinux")
  @patch("sys.exit")
  @patch("os.getpid")
  @patch.object(ProcessHelper, "stopAgent")
  def test_signal_handler(self, stopAgent_mock, os_getpid_mock, sys_exit_mock, heartbeat_handler_mock):
    # testing exit of children
    main.agentPid = 4444
    os_getpid_mock.return_value = 5555
    HeartbeatHandlers.signal_handler("signum", "frame")
    heartbeat_handler_mock.set_stop.assert_called()
    sys_exit_mock.reset_mock()

    # testing exit of main process
    os_getpid_mock.return_value = main.agentPid
    HeartbeatHandlers.signal_handler("signum", "frame")
    heartbeat_handler_mock.set_stop.assert_called()


  @patch.object(main.logger, "addHandler")
  @patch.object(main.logger, "setLevel")
  @patch("logging.handlers.RotatingFileHandler")
  @patch("logging.basicConfig")
  def test_setup_logging(self, basicConfig_mock, rfh_mock, setLevel_mock, addHandler_mock):
    # Testing silent mode
    main.setup_logging(logging.getLogger(), '/var/log/ambari-agent/ambari-agent.log', 20)
    self.assertTrue(addHandler_mock.called)
    setLevel_mock.assert_called_with(logging.INFO)

    addHandler_mock.reset_mock()
    setLevel_mock.reset_mock()

    # Testing verbose mode
    main.setup_logging(logging.getLogger(), '/var/log/ambari-agent/ambari-agent.log', 10)
    self.assertTrue(addHandler_mock.called)
    setLevel_mock.assert_called_with(logging.DEBUG)


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
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

  @only_for_platform(PLATFORM_LINUX)
  @patch("signal.signal")
  def test_bind_signal_handlers(self, signal_mock):
    main.bind_signal_handlers(os.getpid())
    # Check if on SIGINT/SIGTERM agent is configured to terminate
    signal_mock.assert_any_call(signal.SIGINT, HeartbeatHandlers.signal_handler)
    signal_mock.assert_any_call(signal.SIGTERM, HeartbeatHandlers.signal_handler)
    # Check if on SIGUSR1 agent is configured to fall into debug
    signal_mock.assert_any_call(signal.SIGUSR1, HeartbeatHandlers.debug)


  @patch("platform.linux_distribution")
  @patch("os.path.exists")
  @patch("ConfigParser.RawConfigParser.read")
  def test_resolve_ambari_config(self, read_mock, exists_mock, platform_mock):
    platform_mock.return_value = "Linux"
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


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
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

    if OSCheck.get_os_family() != OSConst.WINSRV_FAMILY:
      # Trying case if there is another instance running, only valid for linux
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

  @not_for_platform(PLATFORM_WINDOWS)
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch("time.sleep")
  @patch.object(shellRunnerLinux,"run")
  @patch("os.path.exists")
  def test_daemonize_and_stop(self, exists_mock, kill_mock, sleep_mock):
    oldpid = ProcessHelper.pidfile
    pid = str(os.getpid())
    _, tmpoutfile = tempfile.mkstemp()
    ProcessHelper.pidfile = tmpoutfile

    # Test daemonization
    main.daemonize()
    saved = open(ProcessHelper.pidfile, 'r').read()
    self.assertEqual(pid, saved)

    kill_mock.side_effect = [{'exitCode': 0, 'output': '', 'error': ''},
                             {'exitCode': 1, 'output': '', 'error': ''}]

    main.GRACEFUL_STOP_TRIES = 1
    # Reuse pid file when testing agent stop
    # Testing normal exit
    exists_mock.return_value = False
    try:
      main.stop_agent()
      raise Exception("main.stop_agent() should raise sys.exit(0).")
    except SystemExit as e:
      self.assertEquals(0, e.code);
      
    kill_mock.assert_has_calls([call(['ambari-sudo.sh', 'kill', '-15', pid]),
                                call(['ambari-sudo.sh', 'kill', '-0', pid])])

    # Restore
    kill_mock.reset_mock()
    kill_mock.side_effect = [{'exitCode': 0, 'output': '', 'error': ''},
                             {'exitCode': 0, 'output': '', 'error': ''},
                             {'exitCode': 0, 'output': '', 'error': ''}]

    # Testing exit when failed to remove pid file
    exists_mock.return_value = True
    
    try:
      main.stop_agent()
      raise Exception("main.stop_agent() should raise sys.exit(0).")
    except SystemExit as e:
      self.assertEquals(1, e.code);

    kill_mock.assert_has_calls([call(['ambari-sudo.sh', 'kill', '-15', pid]),
                                call(['ambari-sudo.sh', 'kill', '-0', pid]),
                                call(['ambari-sudo.sh', 'kill', '-9', pid])])
    # Restore
    ProcessHelper.pidfile = oldpid
    os.remove(tmpoutfile)

  @patch("os.rmdir")
  @patch("os.path.join")
  @patch('__builtin__.open')
  @patch.object(ConfigParser, "ConfigParser")
  @patch("sys.exit")
  @patch("os.walk")
  @patch("os.remove")
  def test_reset(self, os_remove_mock, os_walk_mock, sys_exit_mock, config_parser_mock, open_mock, os_path_join_mock, os_rmdir_mock):
    # Agent config update
    config_mock = MagicMock()
    os_walk_mock.return_value = [('/', ('',), ('file1.txt', 'file2.txt'))]
    config_parser_mock.return_value= config_mock
    config_mock.get('server', 'hostname').return_value = "old_host"
    main.reset_agent(["test", "reset", "new_hostname"])
    self.assertEqual(config_mock.get.call_count, 3)
    self.assertEqual(config_mock.set.call_count, 1)
    self.assertEqual(os_remove_mock.call_count, 2)

    self.assertTrue(sys_exit_mock.called)

  @patch("os.rmdir")
  @patch("os.path.join")
  @patch('__builtin__.open')
  @patch.object(ConfigParser, "ConfigParser")
  @patch("sys.exit")
  @patch("os.walk")
  @patch("os.remove")
  def test_reset_invalid_path(self, os_remove_mock, os_walk_mock, sys_exit_mock,
      config_parser_mock, open_mock, os_path_join_mock, os_rmdir_mock):

    # Agent config file cannot be accessed
    config_mock = MagicMock()
    os_walk_mock.return_value = [('/', ('',), ('file1.txt', 'file2.txt'))]
    config_parser_mock.return_value= config_mock
    config_mock.get('server', 'hostname').return_value = "old_host"
    open_mock.side_effect = Exception("Invalid Path!")
    try:
      main.reset_agent(["test", "reset", "new_hostname"])
      self.fail("Should have thrown exception!")
    except:
      self.assertTrue(True)

    self.assertTrue(sys_exit_mock.called)


  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(socket, "gethostbyname")
  @patch.object(main, "setup_logging")
  @patch.object(main, "bind_signal_handlers")
  @patch.object(main, "stop_agent")
  @patch.object(AmbariConfig, "getConfigFile")
  @patch.object(main, "perform_prestart_checks")
  @patch.object(main, "daemonize")
  @patch.object(main, "update_log_level")
  @patch.object(NetUtil.NetUtil, "try_to_connect")
  @patch.object(Controller, "__init__")
  @patch.object(Controller, "is_alive")
  @patch.object(Controller, "start")
  @patch("optparse.OptionParser.parse_args")
  @patch.object(DataCleaner,"start")
  @patch.object(DataCleaner,"__init__")
  @patch.object(PingPortListener,"start")
  @patch.object(PingPortListener,"__init__")
  @patch.object(ExitHelper,"execute_cleanup")
  @patch.object(ExitHelper, "exit")
  def test_main(self, exithelper_exit_mock, cleanup_mock, ping_port_init_mock, ping_port_start_mock, data_clean_init_mock,data_clean_start_mock,
                parse_args_mock, start_mock, Controller_is_alive_mock, Controller_init_mock, try_to_connect_mock,
                update_log_level_mock, daemonize_mock, perform_prestart_checks_mock,
                ambari_config_mock,
                stop_mock, bind_signal_handlers_mock,
                setup_logging_mock, socket_mock):
    data_clean_init_mock.return_value = None
    Controller_init_mock.return_value = None
    Controller_is_alive_mock.return_value = False
    ping_port_init_mock.return_value = None
    options = MagicMock()
    parse_args_mock.return_value = (options, MagicMock)
    try_to_connect_mock.return_value = (0, True)
    # use default unix config
    ambari_config_mock.return_value = os.path.abspath("../../../conf/unix/ambari-agent.ini")
    #testing call without command-line arguments

    main.main()

    self.assertTrue(setup_logging_mock.called)
    #self.assertTrue(resolve_ambari_config_mock.called)
    self.assertTrue(perform_prestart_checks_mock.called)
    if OSCheck.get_os_family() != OSConst.WINSRV_FAMILY:
      self.assertTrue(daemonize_mock.called)
    self.assertTrue(update_log_level_mock.called)
    try_to_connect_mock.assert_called_once_with(ANY, -1, ANY)
    self.assertTrue(start_mock.called)
    self.assertTrue(data_clean_init_mock.called)
    self.assertTrue(data_clean_start_mock.called)
    self.assertTrue(ping_port_init_mock.called)
    self.assertTrue(ping_port_start_mock.called)
    self.assertTrue(exithelper_exit_mock.called)
    perform_prestart_checks_mock.reset_mock()

    # Testing call with --expected-hostname parameter
    options.expected_hostname = "test.hst"
    main.main()
    perform_prestart_checks_mock.assert_called_once_with(options.expected_hostname)
