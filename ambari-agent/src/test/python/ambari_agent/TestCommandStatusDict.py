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
import tempfile
from unittest import TestCase
from ambari_agent.CommandStatusDict import CommandStatusDict
import os
import logging
import json, pprint
from mock.mock import patch, MagicMock, call

class TestCommandStatusDict:#(TestCase):

  logger = logging.getLogger()

  def test_put_command_status(self):
    execution_command = {
      'commandType': 'EXECUTION_COMMAND',
      'commandId': '1-1',
      'clusterName': u'cc',
      'exitCode': 777,
      'role': u'DATANODE',
      'roleCommand': u'INSTALL',
      'serviceName': u'HDFS',
      'taskId': 5
    }
    status_command = {
      'componentName': 'DATANODE',
      'commandType': 'STATUS_COMMAND',
      }
    callback_mock = MagicMock()
    commandStatuses = CommandStatusDict(callback_action = callback_mock)
    commandStatuses.put_command_status(status_command, None)
    self.assertEqual(callback_mock.call_count, 0)
    commandStatuses.put_command_status(execution_command, None)
    self.assertEqual(callback_mock.call_count, 1)

  def test_put_and_generate(self):
    callback_mock = MagicMock()
    commandStatuses = CommandStatusDict(callback_action = callback_mock)
    command_in_progress1 = {
      'commandType': 'EXECUTION_COMMAND',
      'commandId': '1-1',
      'clusterName': u'cc',
      'exitCode': 777,
      'role': u'DATANODE',
      'roleCommand': u'INSTALL',
      'serviceName': u'HDFS',
      'stderr': '',
      'stdout': "notice: /Stage[1]/Hdp::Iptables/Service[iptables]/ensure: ensure changed 'running' to 'stopped'\nnotice: /Stage[1]/Hdp/File[/tmp/changeUid.sh]/ensure: defined content as '{md5}32b994a2e970f8acc3c91c198b484654'\nnotice: /Stage[1]/Hdp::Snappy::Package/Hdp::Package[snappy]/Hdp::Package::Process_pkg[snappy]/Package[snappy]/ensure: created\nnotice: /Stage[1]/Hdp::Snmp/Hdp::Package[snmp]/Hdp::Package::Process_pkg[snmp]/Package[net-snmp-utils]/ensure: created",
      'taskId': 5
    }
    command_in_progress1_report = {
      'status': 'IN_PROGRESS',
      'taskId': 5
    }
    command_in_progress2 = {
      'commandType': 'EXECUTION_COMMAND',
      'commandId': '1-1',
      'role': u'DATANODE',
      'roleCommand': u'INSTALL',
      'taskId': 6,
      'clusterName': u'cc',
      'serviceName': u'HDFS',
    }
    command_in_progress2_report = {
      'status': 'IN_PROGRESS',
      'taskId': 6
    }
    finished_command = {
      'commandType': 'EXECUTION_COMMAND',
      'role': u'DATANODE',
      'roleCommand': u'INSTALL',
      'commandId': '1-1',
      'taskId': 4,
      'clusterName': u'cc',
      'serviceName': u'HDFS',
    }
    finished_command_report = {
      'status': 'COMPLETE',
      'taskId': 4,
    }
    failed_command = {
      'commandType': 'EXECUTION_COMMAND',
      'role': u'DATANODE',
      'roleCommand': u'INSTALL',
      'commandId': '1-1',
      'taskId': 3,
      'clusterName': u'cc',
      'serviceName': u'HDFS',
    }
    failed_command_report = {
      'status': 'FAILED',
      'taskId': 3,
    }
    status_command = {
      'componentName': 'DATANODE',
      'commandType': 'STATUS_COMMAND',
    }
    status_command_report = {
      'componentName': 'DATANODE',
      'status': 'HEALTHY'
    }
    commandStatuses.put_command_status(command_in_progress1, command_in_progress1_report)
    commandStatuses.put_command_status(command_in_progress2, command_in_progress2_report)
    commandStatuses.put_command_status(finished_command, finished_command_report)
    commandStatuses.put_command_status(failed_command, failed_command_report)
    commandStatuses.put_command_status(status_command, status_command_report)
    report = commandStatuses.generate_report()
    expected = \
      {'componentStatus': [{'status': 'HEALTHY', 'componentName': 'DATANODE'}],
       'reports': [{'status': 'FAILED', 'taskId': 3},
                   {'status': 'COMPLETE', 'taskId': 4},
                   {'status': 'IN_PROGRESS', 'stderr': '...',
                    'stdout': '...', 'clusterName': u'cc',
                    'structuredOut' : '{}',
                    'roleCommand': u'INSTALL', 'serviceName': u'HDFS',
                    'role': u'DATANODE', 'actionId': '1-1', 'taskId': 5,
                    'exitCode': 777},
                   {'status': 'IN_PROGRESS',
                    'stderr': '...',
                    'stdout': '...',
                    'structuredOut' : '{}',
                    'clusterName': u'cc',
                    'roleCommand': u'INSTALL',
                    'serviceName': u'HDFS',
                    'role': u'DATANODE',
                    'actionId': '1-1',
                    'taskId': 6,
                    'exitCode': 777}]
      }
    self.assertEquals(report, expected)

  @patch("__builtin__.open")
  def test_structured_output(self, open_mock):
    callback_mock = MagicMock()
    commandStatuses = CommandStatusDict(callback_action = callback_mock)
    file_mock = MagicMock(name = 'file_mock')
    file_mock.__enter__.return_value = file_mock
    file_mock.read.return_value = '{"var1":"test1", "var2":"test2"}'
    open_mock.return_value = file_mock
    command_in_progress1 = {
      'commandType': 'EXECUTION_COMMAND',
      'commandId': '1-1',
      'clusterName': u'cc',
      'exitCode': 777,
      'role': u'DATANODE',
      'roleCommand': u'INSTALL',
      'serviceName': u'HDFS',
      'stderr': '',
      'stdout': "notice: /Stage[1]/Hdp::Iptables/Service[iptables]/ensure: ensure changed 'running' to 'stopped'\nnotice: /Stage[1]/Hdp/File[/tmp/changeUid.sh]/ensure: defined content as '{md5}32b994a2e970f8acc3c91c198b484654'\nnotice: /Stage[1]/Hdp::Snappy::Package/Hdp::Package[snappy]/Hdp::Package::Process_pkg[snappy]/Package[snappy]/ensure: created\nnotice: /Stage[1]/Hdp::Snmp/Hdp::Package[snmp]/Hdp::Package::Process_pkg[snmp]/Package[net-snmp-utils]/ensure: created",
      'taskId': 5
    }
    command_in_progress1_report = {
      'status': 'IN_PROGRESS',
      'taskId': 5,
      'structuredOut' : 'structured_out.tmp',
      }
    commandStatuses.put_command_status(command_in_progress1, command_in_progress1_report)
    report = commandStatuses.generate_report()
    expected = \
      {'componentStatus': [],
       'reports': [{'status': 'IN_PROGRESS', 'stderr': '...',
                    'stdout': '...', 'clusterName': u'cc',
                    'structuredOut': '{"var1":"test1", "var2":"test2"}',
                    'roleCommand': u'INSTALL',
                    'serviceName': u'HDFS',
                    'role': u'DATANODE',
                    'actionId': '1-1', 'taskId': 5, 'exitCode': 777}]
      }
    self.assertEquals(report, expected)
