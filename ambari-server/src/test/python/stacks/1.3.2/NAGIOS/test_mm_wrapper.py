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

import os, sys
import pprint
import subprocess
from unittest import TestCase
from mock.mock import Mock, MagicMock, patch
import mm_wrapper

class TestOrWrapper(TestCase):

  dummy_ignore_file = """
vm-4.vm HIVE HIVE_METASTORE
vm-5.vm GANGLIA GANGLIA_MONITOR
vm-4.vm YARN NODEMANAGER
vm-3.vm YARN NODEMANAGER
vm-3.vm HBASE HBASE_REGIONSERVER
vm-4.vm HBASE HBASE_REGIONSERVER
vm-4.vm STORM STORM_REST_API
vm-4.vm HDFS DATANODE
vm-4.vm STORM SUPERVISOR
vm-4.vm STORM NIMBUS
vm-4.vm STORM STORM_UI_SERVER
vm-3.vm STORM SUPERVISOR
vm-4.vm HDFS SECONDARY_NAMENODE
vm-3.vm FLUME FLUME_HANDLER
vm-4.vm GANGLIA GANGLIA_SERVER
vm-4.vm HIVE HIVE_SERVER
vm-4.vm ZOOKEEPER ZOOKEEPER_SERVER
vm-4.vm WEBHCAT WEBHCAT_SERVER
vm-3.vm HBASE HBASE_MASTER
vm-4.vm GANGLIA GANGLIA_MONITOR
vm-3.vm GANGLIA GANGLIA_MONITOR
vm-3.vm HDFS NAMENODE
vm-4.vm HIVE MYSQL_SERVER
vm-4.vm YARN APP_TIMELINE_SERVER
vm-4.vm FALCON FALCON_SERVER
vm-3.vm HDFS DATANODE
vm-4.vm YARN RESOURCEMANAGER
vm-4.vm OOZIE OOZIE_SERVER
vm-4.vm MAPREDUCE2 HISTORYSERVER
vm-4.vm STORM DRPC_SERVER
vm-4.vm FLUME FLUME_HANDLER
vm-3.vm ZOOKEEPER ZOOKEEPER_SERVER
"""

  default_empty_check_result = {
    'message': 'No checks have been run (no hostnames provided)',
    'retcode': -1,
    'real_retcode': None
  }


  @patch("__builtin__.open")
  def test_ignored_host_list(self, open_mock):
    # Check with empty file content
    open_mock.return_value.__enter__.return_value.read.return_value = ""
    lst = mm_wrapper.ignored_host_list('STORM', 'SUPERVISOR')
    self.assertEqual(pprint.pformat(lst), '[]')
    # Check with dummy content
    open_mock.return_value.__enter__.return_value.read.return_value = self.dummy_ignore_file
    lst = mm_wrapper.ignored_host_list('STORM', 'SUPERVISOR')
    self.assertEqual(pprint.pformat(lst), "['vm-4.vm', 'vm-3.vm']")
    # Check if service name/comp name are not defined
    open_mock.return_value.__enter__.return_value.read.return_value = self.dummy_ignore_file
    lst = mm_wrapper.ignored_host_list('', '')
    self.assertEqual(pprint.pformat(lst), "[]")


  @patch("sys.exit")
  def test_print_usage(self, exit_mock):
    mm_wrapper.print_usage()
    self.assertTrue(exit_mock.called)
    self.assertEqual(exit_mock.call_args_list[0][0][0], 1)


  def test_get_real_component(self):
    with patch.dict(os.environ, {'NAGIOS__SERVICEHOST_COMPONENT': 'SUPERVISOR'}, clear=True):
      component = mm_wrapper.get_real_component()
      self.assertEqual(component, 'SUPERVISOR')
    with patch.dict(os.environ, {'NAGIOS__SERVICEHOST_COMPONENT': 'JOBHISTORY'}, clear=True):
      component = mm_wrapper.get_real_component()
      self.assertEqual(component, 'MAPREDUCE2')


  @patch("mm_wrapper.print_usage")
  def test_parse_args(self, print_usage_mock):
    args = ['or', 'h1', 'h2', '--', 'prog', '-h', '^^', '-opt', 'yet', 'another', 'opt']
    mode, hostnames, command_line = mm_wrapper.parse_args(args)
    self.assertEquals(mode, mm_wrapper.OR)
    self.assertEquals(hostnames, ['h1', 'h2'])
    self.assertEquals(command_line, ['prog', '-h', '^^', '-opt', 'yet', 'another', 'opt'])

    args = ['and', 'h1', 'h2', '--', 'prog', '-h', '^^', '-opt', 'yet', 'another', 'opt']
    mode, hostnames, command_line = mm_wrapper.parse_args(args)
    self.assertEquals(mode, mm_wrapper.AND)
    self.assertEquals(hostnames, ['h1', 'h2'])
    self.assertEquals(command_line, ['prog', '-h', '^^', '-opt', 'yet', 'another', 'opt'])

    args = ['env_only', 'h1', 'h2', '--', 'prog', '-h', '^^', '-opt', 'yet', 'another', 'opt']
    mode, hostnames, command_line = mm_wrapper.parse_args(args)
    self.assertEquals(mode, mm_wrapper.ENV_ONLY)
    self.assertEquals(hostnames, ['h1', 'h2'])
    self.assertEquals(command_line, ['prog', '-h', '^^', '-opt', 'yet', 'another', 'opt'])

    # Check wrong usage
    args = []
    mm_wrapper.parse_args(args)
    self.assertTrue(print_usage_mock.called)


  @patch("mm_wrapper.ignored_host_list")
  @patch("mm_wrapper.work_in_or_mode")
  @patch("mm_wrapper.work_in_and_mode")
  @patch("mm_wrapper.work_in_env_only_mode")
  @patch("mm_wrapper.work_in_filter_mm_mode")
  @patch("mm_wrapper.work_in_legacy_check_wrapper_mode")
  def test_do_work(self, work_in_legacy_check_wrapper_mode, work_in_filter_mm_mode_mock,
                   work_in_env_only_mode_mock, work_in_and_mode_mock,
                   work_in_or_mode_mock,
                   ignored_host_list_mock):
    hostnames = ['h1', 'h2', 'h3', 'h4']
    ignored_host_list_mock.return_value = ['h2', 'h3']
    command_line = ['prog', '-h', '^^', '-opt', 'yet', 'another', 'opt']
    work_in_or_mode_mock.return_value = {
      'message': "or_mode mode result",
      'retcode': 0,
      'real_retcode': None
    }
    work_in_and_mode_mock.return_value = {
      'message': "and_mode mode result",
      'retcode': 0,
      'real_retcode': None
    }
    work_in_env_only_mode_mock.return_value = {
      'message': "env_only mode result",
      'retcode': 0,
      'real_retcode': None
    }
    work_in_filter_mm_mode_mock.return_value = {
      'message': "filter_mm mode result",
      'retcode': 0,
      'real_retcode': None
    }
    work_in_legacy_check_wrapper_mode.return_value = {
      'message': "legacy_check_wrapper mode result",
      'retcode': 0,
      'real_retcode': None
    }
    result = mm_wrapper.do_work(mm_wrapper.OR, hostnames, command_line)
    self.assertEquals(str(result), "(['or_mode mode result'], 0)")

    result = mm_wrapper.do_work(mm_wrapper.AND, hostnames, command_line)
    self.assertEquals(str(result), "(['and_mode mode result'], 0)")

    result = mm_wrapper.do_work(mm_wrapper.ENV_ONLY, hostnames, command_line)
    self.assertEquals(str(result), "(['env_only mode result'], 0)")

    result = mm_wrapper.do_work(mm_wrapper.FILTER_MM, hostnames, command_line)
    self.assertEquals(str(result), "(['filter_mm mode result'], 0)")

    result = mm_wrapper.do_work(mm_wrapper.LEGACY_CHECK_WRAPPER, hostnames, command_line)
    self.assertEquals(str(result), "(['legacy_check_wrapper mode result'], 0)")

    # Check behaviour when real_retcode is defined
    work_in_or_mode_mock.return_value = {
      'message': "or_mode mode result",
      'retcode': 0,
      'real_retcode': 1
    }
    result = mm_wrapper.do_work(mm_wrapper.OR, hostnames, command_line)
    self.assertEquals(str(result), "(['or_mode mode result', 'AMBARIPASSIVE=1'], 0)")


  @patch("mm_wrapper.check_output")
  def test_work_in_or_mode(self, check_output_mock):
    hostnames = ['h1', 'h2', 'h3', 'h4']
    ignored_hosts = ['h2', 'h3']
    command_line = ['prog', '-h', '^^', '-opt', 'yet', 'another', 'opt']
    custom_env = {'MM_HOSTS': ignored_hosts}

    # Normal usage
    check_output_mock.return_value = 'Dummy message'
    result = mm_wrapper.work_in_or_mode(hostnames, ignored_hosts, command_line,
                                        custom_env,
                                        self.default_empty_check_result)
    self.assertEquals(str(result),
                      "{'message': 'Dummy message', 'real_retcode': None, 'retcode': 0}")
    self.assertEquals(check_output_mock.call_count, 1)  # Exited on first success
    self.assertEquals(check_output_mock.call_args[1]['env']['MM_HOSTS'], ignored_hosts)
    for check_tupple in zip(check_output_mock.call_args_list, hostnames):
      self.assertEquals(check_tupple[0][0][0], ['prog', '-h', check_tupple[1], '-opt', 'yet', 'another', 'opt'])

    check_output_mock.reset_mock()

    # Failed all checks
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(3, 'dummy cmd')
    error.output = 'dummy output2'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output3'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(2, 'dummy cmd')
    error.output = 'dummy output4'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects

    result = mm_wrapper.work_in_or_mode(hostnames, ignored_hosts, command_line,
                                        custom_env,
                                        self.default_empty_check_result)
    self.assertEquals(check_output_mock.call_count, 4)
    self.assertEquals(str(result),
                      "{'message': 'dummy output4', 'real_retcode': None, 'retcode': 2}")

    check_output_mock.reset_mock()

    # Failed all but MM host component checks
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(0, 'dummy cmd')
    error.output = 'dummy output2'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(2, 'dummy cmd')
    error.output = 'dummy output3'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(3, 'dummy cmd')
    error.output = 'dummy output4'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects

    result = mm_wrapper.work_in_or_mode(hostnames, ignored_hosts, command_line,
                                        custom_env,
                                        self.default_empty_check_result)
    self.assertEquals(check_output_mock.call_count, 4)
    self.assertEquals(str(result),
                      "{'message': 'dummy output4', 'real_retcode': None, 'retcode': 3}")

    check_output_mock.reset_mock()

    # Components check only for one check is successful
    ignored_hosts = []
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(0, 'dummy cmd')
    error.output = 'dummy output2'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(2, 'dummy cmd')
    error.output = 'dummy output3'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(3, 'dummy cmd')
    error.output = 'dummy output4'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects

    result = mm_wrapper.work_in_or_mode(hostnames, ignored_hosts, command_line,
                                        custom_env,
                                        self.default_empty_check_result)
    self.assertEquals(check_output_mock.call_count, 2)
    self.assertEquals(str(result),
                      "{'message': 'dummy output2', 'real_retcode': None, 'retcode': 0}")


  @patch("mm_wrapper.check_output")
  def test_work_in_and_mode(self, check_output_mock):
    hostnames = ['h1', 'h2', 'h3', 'h4']
    ignored_hosts = ['h2', 'h3']
    command_line = ['prog', '-h', '^^', '-opt', 'yet', 'another', 'opt']
    custom_env = {'MM_HOSTS': ignored_hosts}

    # Normal usage
    check_output_mock.return_value = 'Dummy message'
    result = mm_wrapper.work_in_and_mode(hostnames, ignored_hosts, command_line,
                                        custom_env,
                                        self.default_empty_check_result)
    self.assertEquals(str(result),
                      "{'message': 'Dummy message', 'real_retcode': None, 'retcode': 0}")
    self.assertEquals(check_output_mock.call_count, 4)
    self.assertEquals(check_output_mock.call_args[1]['env']['MM_HOSTS'], ignored_hosts)
    for check_tupple in zip(check_output_mock.call_args_list, hostnames):
      self.assertEquals(check_tupple[0][0][0], ['prog', '-h', check_tupple[1], '-opt', 'yet', 'another', 'opt'])

    check_output_mock.reset_mock()

    # Failed all checks
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(3, 'dummy cmd')
    error.output = 'dummy output2'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output3'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(2, 'dummy cmd')
    error.output = 'dummy output4'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects
    result = mm_wrapper.work_in_and_mode(hostnames, ignored_hosts, command_line,
                                        custom_env,
                                        self.default_empty_check_result)
    self.assertEquals(check_output_mock.call_count, 4)
    self.assertEquals(str(result),
                      "{'message': 'dummy output4', 'real_retcode': None, 'retcode': 2}")

    check_output_mock.reset_mock()

    # Failed all but MM host component checks
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(0, 'dummy cmd')
    error.output = 'dummy output2'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(2, 'dummy cmd')
    error.output = 'dummy output3'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(3, 'dummy cmd')
    error.output = 'dummy output4'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects
    result = mm_wrapper.work_in_and_mode(hostnames, ignored_hosts, command_line,
                                        custom_env,
                                        self.default_empty_check_result)
    self.assertEquals(check_output_mock.call_count, 4)
    self.assertEquals(str(result),
                      "{'message': 'dummy output4', 'real_retcode': None, 'retcode': 3}")

    check_output_mock.reset_mock()

    # Components check only for one check is successful
    ignored_hosts = []
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(0, 'dummy cmd')
    error.output = 'dummy output2'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(2, 'dummy cmd')
    error.output = 'dummy output3'
    check_output_side_effects.append(error)

    error = subprocess.CalledProcessError(3, 'dummy cmd')
    error.output = 'dummy output4'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects
    result = mm_wrapper.work_in_and_mode(hostnames, ignored_hosts, command_line,
                                        custom_env,
                                        self.default_empty_check_result)
    self.assertEquals(check_output_mock.call_count, 4)
    self.assertEquals(str(result),
                      "{'message': 'dummy output4', 'real_retcode': None, 'retcode': 3}")


  @patch("mm_wrapper.check_output")
  def test_work_in_env_only_mode(self, check_output_mock):
    hostnames = ['h1', 'h2', 'h3', 'h4']
    ignored_hosts = ['h2', 'h3']
    command_line = ['prog', '-h', '^^', '-opt', 'yet', 'another', 'opt']
    custom_env = {'MM_HOSTS' : ignored_hosts}

    # Normal usage
    check_output_mock.return_value = 'Dummy message'
    result = mm_wrapper.work_in_env_only_mode(hostnames, command_line, custom_env)
    self.assertEquals(str(result),
                      "{'message': 'Dummy message', 'real_retcode': None, 'retcode': 0}")
    self.assertEquals(check_output_mock.call_count, 1)
    self.assertEquals(check_output_mock.call_args[1]['env']['MM_HOSTS'], ignored_hosts)
    self.assertEquals(check_output_mock.call_args[0][0],
                      ['prog', '-h', 'h1', 'h2', 'h3', 'h4', '-opt', 'yet', 'another', 'opt'])

    check_output_mock.reset_mock()

    # Failed all checks
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects
    result = mm_wrapper.work_in_env_only_mode(hostnames, command_line, custom_env)
    self.assertEquals(check_output_mock.call_count, 1)
    self.assertEquals(str(result),
                      "{'message': 'dummy output1', 'real_retcode': None, 'retcode': 1}")

    check_output_mock.reset_mock()


  @patch("mm_wrapper.check_output")
  def test_work_in_filter_mm_mode(self, check_output_mock):
    hostnames = ['h1', 'h2', 'h3', 'h4']
    ignored_hosts = ['h2', 'h3']
    command_line = ['prog', '-h', '^^', '-opt', 'yet', 'another', 'opt']
    custom_env = {'MM_HOSTS' : ignored_hosts}

    # Normal usage
    check_output_mock.return_value = 'Dummy message'
    result = mm_wrapper.work_in_filter_mm_mode(hostnames, ignored_hosts, command_line,
                                               custom_env,
                                               self.default_empty_check_result)
    self.assertEquals(str(result),
                      "{'message': 'Dummy message', 'real_retcode': None, 'retcode': 0}")
    self.assertEquals(check_output_mock.call_count, 1)
    self.assertEquals(check_output_mock.call_args[1]['env']['MM_HOSTS'], ignored_hosts)
    self.assertEquals(check_output_mock.call_args[0][0],
                      ['prog', '-h', 'h1', 'h4', '-opt', 'yet', 'another', 'opt'])

    check_output_mock.reset_mock()

    # Failed all checks
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects
    result = mm_wrapper.work_in_filter_mm_mode(hostnames, ignored_hosts, command_line,
                                               custom_env,
                                               self.default_empty_check_result)
    self.assertEquals(check_output_mock.call_count, 1)
    self.assertEquals(str(result),
                      "{'message': 'dummy output1', 'real_retcode': None, 'retcode': 1}")

    check_output_mock.reset_mock()

    # All host components are in MM
    ignored_hosts = hostnames
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects
    result = mm_wrapper.work_in_filter_mm_mode(hostnames, ignored_hosts, command_line,
                                               custom_env,
                                               self.default_empty_check_result)
    self.assertEquals(check_output_mock.call_count, 0)
    self.assertEquals(str(result),
                      "{'message': 'No checks have been run (no hostnames provided)', "
                      "'real_retcode': None, 'retcode': -1}")

    check_output_mock.reset_mock()


  @patch("mm_wrapper.check_output")
  @patch.dict(os.environ, {'NAGIOS_HOSTNAME': 'h2'}, clear=True)
  def test_work_in_legacy_check_wrapper_mode(self, check_output_mock):
    command_line = ['prog', '-opt', 'yet', 'another', 'opt']
    ignored_hosts = []
    custom_env = {'MM_HOSTS': ignored_hosts}

    # Normal usage
    ignored_hosts = []
    check_output_mock.return_value = 'Dummy message'
    result = mm_wrapper.work_in_legacy_check_wrapper_mode(ignored_hosts, command_line,
                                               custom_env)
    self.assertEquals(str(result),
                      "{'message': 'Dummy message', 'real_retcode': None, 'retcode': 0}")
    self.assertEquals(check_output_mock.call_count, 1)
    self.assertEquals(check_output_mock.call_args[1]['env']['MM_HOSTS'], ignored_hosts)
    self.assertEquals(check_output_mock.call_args[0][0],
                      ['prog', '-opt', 'yet', 'another', 'opt'])

    check_output_mock.reset_mock()

    # Failed check on host that is not in MM state
    ignored_hosts = ['h3']
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects
    result = mm_wrapper.work_in_legacy_check_wrapper_mode(ignored_hosts, command_line,
                                               custom_env)
    self.assertEquals(check_output_mock.call_count, 1)
    self.assertEquals(str(result),
                      "{'message': 'dummy output1', 'real_retcode': None, 'retcode': 1}")

    check_output_mock.reset_mock()

    # Failed check on host that is in MM state
    ignored_hosts = ['h2']
    check_output_side_effects = []
    error = subprocess.CalledProcessError(1, 'dummy cmd')
    error.output = 'dummy output1'
    check_output_side_effects.append(error)

    check_output_mock.side_effect = check_output_side_effects
    result = mm_wrapper.work_in_legacy_check_wrapper_mode(ignored_hosts, command_line,
                                               custom_env)
    self.assertEquals(check_output_mock.call_count, 1)
    self.assertEquals(str(result),
                      "{'message': 'dummy output1', 'real_retcode': 1, 'retcode': 0}")

    check_output_mock.reset_mock()

