#!/usr/bin/env python3
"""
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
"""

import io
import sys
import unittest
import logging
import signal
import os
import socket
import tempfile
import threading
import configparser
import ambari_agent.hostname as hostname
import resource

from ambari_commons import OSCheck
from only_for_platform import (
  not_for_platform,
  os_distro_value,
)
from unittest.mock import MagicMock, patch, call

with patch.object(
  OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value)
):
  from ambari_agent import NetUtil
  from ambari_agent import main
  from ambari_agent.AmbariConfig import AmbariConfig
  from ambari_agent.PingPortListener import PingPortListener
  import ambari_agent.HeartbeatHandlers as HeartbeatHandlers
  from ambari_commons.os_check import OSConst, OSCheck
  from ambari_agent.ExitHelper import ExitHelper


class TestMain(unittest.TestCase):
  def setUp(self):
    # disable stdout
    out = io.StringIO()
    sys.stdout = out

  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  def test_signal_handler_sets_shared_stop_event(self):
    stop_event = threading.Event()
    HeartbeatHandlers._handler = stop_event

    HeartbeatHandlers.signal_handler("signum", "frame")

    self.assertTrue(stop_event.is_set())

  @patch.object(main.logger, "addHandler")
  @patch.object(main.logger, "setLevel")
  @patch("logging.handlers.RotatingFileHandler")
  @patch("logging.basicConfig")
  def test_setup_logging(
    self, basicConfig_mock, rfh_mock, setLevel_mock, addHandler_mock
  ):
    # Testing silent mode
    main.setup_logging(
      logging.getLogger(), "/var/log/ambari-agent/ambari-agent.log", 20
    )
    self.assertTrue(addHandler_mock.called)
    setLevel_mock.assert_called_with(logging.INFO)

    addHandler_mock.reset_mock()
    setLevel_mock.reset_mock()

    # Testing verbose mode
    main.setup_logging(
      logging.getLogger(), "/var/log/ambari-agent/ambari-agent.log", 10
    )
    self.assertTrue(addHandler_mock.called)
    setLevel_mock.assert_called_with(logging.DEBUG)

  @patch("os.path.exists")
  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(main.logger, "setLevel")
  @patch("logging.basicConfig")
  def test_update_log_level(self, basicConfig_mock, setLevel_mock, os_path_exists_mock):
    os_path_exists_mock.return_value = False
    config = AmbariConfig().getConfig()

    # Testing with default setup (config file does not contain loglevel entry)
    # Log level should not be changed
    config.set("agent", "loglevel", None)
    main.update_log_level(config)
    self.assertFalse(setLevel_mock.called)

    setLevel_mock.reset_mock()

    # Testing debug mode
    config.set("agent", "loglevel", "DEBUG")
    main.update_log_level(config)
    setLevel_mock.assert_called_with(logging.DEBUG)
    setLevel_mock.reset_mock()

    # Testing any other mode
    config.set("agent", "loglevel", "INFO")
    main.update_log_level(config)
    setLevel_mock.assert_called_with(logging.INFO)

    setLevel_mock.reset_mock()

    config.set("agent", "loglevel", "WRONG")
    main.update_log_level(config)
    setLevel_mock.assert_called_with(logging.INFO)

  @patch.object(resource, "setrlimit")
  @patch.object(resource, "getrlimit", return_value=(100, 200))
  def test_update_open_files_ulimit(self, _getrlimit_mock, setrlimit_mock):
    config = MagicMock()
    config.get_ulimit_open_files.return_value = 150

    main.update_open_files_ulimit(config)

    setrlimit_mock.assert_called_once_with(resource.RLIMIT_NOFILE, (100, 150))

  @patch("signal.signal")
  def test_bind_signal_handlers(self, signal_mock):
    stop_event = threading.Event()

    returned_event = main.bind_signal_handlers(os.getpid(), stop_event)

    # Check if on SIGINT/SIGTERM agent is configured to terminate
    signal_mock.assert_any_call(signal.SIGINT, HeartbeatHandlers.signal_handler)
    signal_mock.assert_any_call(signal.SIGTERM, HeartbeatHandlers.signal_handler)
    signal_mock.assert_any_call(
      signal.SIGUSR1, HeartbeatHandlers.log_thread_stack_traces
    )
    self.assertIs(stop_event, returned_event)

  @patch("ambari_commons.os_check.linux_distribution")
  @patch("os.path.exists")
  @patch("configparser.RawConfigParser.read")
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

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch("ambari_commons.shell.shellRunnerLinux.run")
  @patch("sys.exit")
  @patch("os.path.isfile")
  @patch("os.path.isdir")
  @patch("hostname.hostname")
  def test_perform_prestart_checks(
    self, hostname_mock, isdir_mock, isfile_mock, exit_mock, shell_mock
  ):
    main.config = AmbariConfig().getConfig()
    shell_mock.return_value = {"exitCode": 0}

    # Check expected hostname test
    hostname_mock.return_value = "test.hst"

    main.perform_prestart_checks("another.hst")
    self.assertTrue(exit_mock.called)

    exit_mock.reset_mock()

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

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch("time.sleep")
  @patch("os.path.exists")
  def test_daemonize_and_stop(self, exists_mock, sleep_mock):
    from ambari_commons.shell import shellRunnerLinux

    oldpid = main.agent_pidfile
    pid = str(os.getpid())
    _, tmpoutfile = tempfile.mkstemp()
    main.agent_pidfile = tmpoutfile

    # Test daemonization
    main.daemonize()
    saved = open(main.agent_pidfile, "r").read()
    self.assertEqual(pid, saved)

    main.GRACEFUL_STOP_TRIES = 1
    with patch("ambari_commons.shell.shellRunnerLinux.run") as kill_mock:
      # Reuse pid file when testing agent stop
      # Testing normal exit
      exists_mock.return_value = False
      kill_mock.side_effect = [
        {"exitCode": 0, "output": "", "error": ""},
        {"exitCode": 1, "output": "", "error": ""},
      ]
      try:
        main.stop_agent()
        raise Exception("main.stop_agent() should raise sys.exit(0).")
      except SystemExit as e:
        self.assertEqual(0, e.code)

      kill_mock.assert_has_calls(
        [
          call(["ambari-sudo.sh", "kill", "-15", pid]),
          call(["ambari-sudo.sh", "kill", "-0", pid]),
        ]
      )

      # Restore
      kill_mock.reset_mock()
      kill_mock.side_effect = [
        {"exitCode": 0, "output": "", "error": ""},
        {"exitCode": 0, "output": "", "error": ""},
        {"exitCode": 0, "output": "", "error": ""},
      ]

      # Testing exit when failed to remove pid file
      exists_mock.return_value = True
      try:
        main.stop_agent()
        raise Exception("main.stop_agent() should raise sys.exit(0).")
      except SystemExit as e:
        self.assertEqual(0, e.code)

      kill_mock.assert_has_calls(
        [
          call(["ambari-sudo.sh", "kill", "-15", pid]),
          call(["ambari-sudo.sh", "kill", "-0", pid]),
          call(["ambari-sudo.sh", "kill", "-9", pid]),
        ]
      )

    # Restore
    main.agent_pidfile = oldpid
    os.remove(tmpoutfile)

  @patch("os.rmdir")
  @patch("os.path.join")
  @patch("builtins.open")
  @patch.object(configparser, "ConfigParser")
  @patch("sys.exit")
  @patch("os.walk")
  @patch("os.remove")
  def test_reset(
    self,
    os_remove_mock,
    os_walk_mock,
    sys_exit_mock,
    config_parser_mock,
    open_mock,
    os_path_join_mock,
    os_rmdir_mock,
  ):
    # Agent config update
    config_mock = MagicMock()
    os_walk_mock.return_value = [("/", ("",), ("file1.txt", "file2.txt"))]
    config_parser_mock.return_value = config_mock
    config_mock.get.side_effect = ["old_host", "/keys"]
    main.reset_agent(["test", "reset", "new_hostname"])
    self.assertEqual(config_mock.get.call_count, 2)
    self.assertEqual(config_mock.set.call_count, 1)
    self.assertEqual(os_remove_mock.call_count, 2)
    open_mock.assert_any_call(main.configFile, "w", encoding="utf-8")

    self.assertTrue(sys_exit_mock.called)

  @patch("os.rmdir")
  @patch("os.path.join")
  @patch("builtins.open")
  @patch.object(configparser, "ConfigParser")
  @patch("sys.exit")
  @patch("os.walk")
  @patch("os.remove")
  def test_reset_invalid_path(
    self,
    os_remove_mock,
    os_walk_mock,
    sys_exit_mock,
    config_parser_mock,
    open_mock,
    os_path_join_mock,
    os_rmdir_mock,
  ):
    # Agent config file cannot be accessed
    config_mock = MagicMock()
    os_walk_mock.return_value = [("/", ("",), ("file1.txt", "file2.txt"))]
    config_parser_mock.return_value = config_mock
    config_mock.get.return_value = "old_host"
    open_mock.side_effect = Exception("Invalid Path!")
    main.reset_agent(["test", "reset", "new_hostname"])

    self.assertTrue(sys_exit_mock.called)

  @patch.object(OSCheck, "os_distribution", new=MagicMock(return_value=os_distro_value))
  @patch.object(hostname, "server_hostnames", return_value=["host1", "host2"])
  @patch.object(socket, "gethostbyname", return_value="192.0.2.10")
  @patch.object(main, "resolve_ambari_config")
  @patch.object(main, "perform_prestart_checks")
  @patch.object(main, "daemonize")
  @patch.object(main, "update_log_level")
  @patch.object(NetUtil.NetUtil, "try_to_connect")
  @patch.object(PingPortListener, "start")
  @patch.object(PingPortListener, "__init__")
  @patch.object(ExitHelper, "exit")
  @patch.object(main, "run_threads")
  @patch.object(main, "config")
  @patch.object(sys, "argv", ["ambari-agent"])
  def test_main(
    self,
    config_mock,
    run_threads_mock,
    exithelper_exit_mock,
    ping_port_init_mock,
    ping_port_start_mock,
    try_to_connect_mock,
    update_log_level_mock,
    daemonize_mock,
    perform_prestart_checks_mock,
    resolve_ambari_config_mock,
    socket_mock,
    server_hostnames_mock,
  ):
    ping_port_init_mock.return_value = None
    config_mock.has_option.return_value = False
    config_mock.use_system_proxy_setting.return_value = False
    config_mock.get_ulimit_open_files.return_value = 65536
    config_mock.get_api_url.side_effect = lambda host: f"https://{host}:8441"
    try_to_connect_mock.side_effect = [(0, False, False), (0, True, False)]
    options = MagicMock(expected_hostname="test.hst", home_dir="")
    initializer_module = MagicMock()
    initializer_module.stop_event = threading.Event()

    active_server = main.main(options, initializer_module)

    self.assertEqual("host2", active_server)
    initializer_module.init.assert_called_once_with()
    config_mock.load.assert_called_once_with({"agent": {"prefix": "/home/ambari"}})
    resolve_ambari_config_mock.assert_called_once_with()
    perform_prestart_checks_mock.assert_called_once_with("test.hst")
    daemonize_mock.assert_called_once_with()
    update_log_level_mock.assert_called_once_with(config_mock)
    self.assertEqual(2, try_to_connect_mock.call_count)
    run_threads_mock.assert_called_once_with(initializer_module)
    ping_port_start_mock.assert_called_once_with()
    exithelper_exit_mock.assert_called_once_with()

  def test_run_threads_stops_scheduler_and_joins_every_worker(self):
    initializer_module = MagicMock()
    initializer_module.stop_event.is_set.return_value = True

    main.run_threads(initializer_module)

    initializer_module.action_queue.interrupt.assert_called_once_with()
    initializer_module.alert_scheduler_handler.stop.assert_called_once_with()
    for worker in (
      initializer_module.action_queue,
      initializer_module.command_status_reporter,
      initializer_module.component_status_executor,
      initializer_module.host_status_reporter,
      initializer_module.alert_status_reporter,
      initializer_module.heartbeat_thread,
    ):
      worker.start.assert_called_once_with()
      worker.join.assert_called_once_with()
