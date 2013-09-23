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
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.ActionDependencyManager import ActionDependencyManager
import os, errno, time, pprint, tempfile, threading, sys
from mock.mock import patch, MagicMock, call

class TestActionDependencyManager(TestCase):

  dummy_RCO_file = os.path.join('dummy_files', 'test_rco_data.json')

  def setUp(self):
    self.config = AmbariConfig().getConfig()
    self.config.set('agent', 'prefix', os.getcwd())
    ActionDependencyManager.DEPS_FILE_NAME = self.dummy_RCO_file

  # TODO: disabled for now
  def disabled_test_init(self):
    """
    Tests config load
    """
    adm = ActionDependencyManager(self.config)
    deps_dump = pprint.pformat(adm.deps)
    expected = "{u'DATANODE-STOP': [u'JOBTRACKER-STOP',\n                    " \
               "u'TASKTRACKER-STOP',\n                    " \
               "u'RESOURCEMANAGER-STOP',\n                    " \
               "u'NODEMANAGER-STOP',\n                    " \
               "u'HISTORYSERVER-STOP',\n                    " \
               "u'HBASE_MASTER-STOP'],\n u'HBASE_MASTER-START': " \
               "[u'PEERSTATUS-START'],\n u'JOBTRACKER-START': " \
               "[u'PEERSTATUS-START'],\n u'RESOURCEMANAGER-START': " \
               "[u'NAMENODE-START', u'DATANODE-START'],\n " \
               "u'SECONDARY_NAMENODE-START': [u'DATANODE-START', " \
               "u'NAMENODE-START'],\n u'SECONDARY_NAMENODE-UPGRADE': " \
               "[u'NAMENODE-UPGRADE']}"
    self.assertEqual(deps_dump, expected)


  def test_is_action_group_available(self):
    adm = ActionDependencyManager(self.config)
    self.assertFalse(adm.is_action_group_available())
    adm.scheduled_action_groups.put(["test"])
    self.assertTrue(adm.is_action_group_available())


  def test_get_next_action_group(self):
    adm = ActionDependencyManager(self.config)
    test1 = ["test1"]
    test2 = ["test2"]
    adm.scheduled_action_groups.put(test1)
    adm.scheduled_action_groups.put(test2)
    adm.last_scheduled_group = test2
    self.assertTrue(adm.is_action_group_available())
    # Taking 1st
    self.assertEqual(test1, adm.get_next_action_group())
    self.assertTrue(len(adm.last_scheduled_group) == 1)
    self.assertTrue(adm.is_action_group_available())
    # Taking 2nd
    self.assertEqual(test2, adm.get_next_action_group())
    self.assertTrue(len(adm.last_scheduled_group) == 0)
    self.assertTrue(adm.last_scheduled_group is not test2)
    self.assertFalse(adm.is_action_group_available())


  @patch.object(ActionDependencyManager, "dump_info")
  @patch.object(ActionDependencyManager, "can_be_executed_in_parallel")
  def test_put_action(self, can_be_executed_in_parallel_mock, dump_info_mock):
    can_be_executed_in_parallel_mock.side_effect = [True, False, True, False,
                                                     True, True, True, False]
    adm = ActionDependencyManager(self.config)

    adm.put_actions(list(range(0, 8)))

    queue = []
    while adm.is_action_group_available():
      next = adm.get_next_action_group()
      queue.append(next)

    str = pprint.pformat(queue)
    expected = "[[0], [1, 2], [3, 4, 5, 6], [7]]"
    self.assertEqual(str, expected)


  # TODO: disabled for now
  def disabled_test_can_be_executed_in_parallel(self):
    adm = ActionDependencyManager(self.config)
    # empty group
    group = []
    install_command = {
      'role': 'DATANODE',
      'roleCommand': 'INSTALL',
      'commandType': ActionQueue.EXECUTION_COMMAND
    }
    upgrade_command = {
      'role': 'DATANODE',
      'roleCommand': 'UPGRADE',
      'commandType': ActionQueue.EXECUTION_COMMAND
    }
    start_command = {
      'role': 'DATANODE',
      'roleCommand': 'START',
      'commandType': ActionQueue.EXECUTION_COMMAND
    }
    stop_command = {
      'role': 'DATANODE',
      'roleCommand': 'STOP',
      'commandType': ActionQueue.EXECUTION_COMMAND
    }
    status_command = {
      'commandType': ActionQueue.STATUS_COMMAND
    }
    rm_start_command = {
      'role': 'RESOURCEMANAGER',
      'roleCommand': 'START',
      'commandType': ActionQueue.EXECUTION_COMMAND
    }
    hm_start_command = {
      'role': 'HBASE_MASTER',
      'roleCommand': 'START',
      'commandType': ActionQueue.EXECUTION_COMMAND
    }
    self.assertTrue(adm.can_be_executed_in_parallel(install_command, group))
    self.assertTrue(adm.can_be_executed_in_parallel(status_command, group))
    # multiple status commands
    group = []
    for i in range(0, 3):
      group.append(status_command)
    self.assertTrue(adm.can_be_executed_in_parallel(status_command, group))
    self.assertFalse(adm.can_be_executed_in_parallel(install_command, group))
    # new status command
    group = [install_command]
    self.assertFalse(adm.can_be_executed_in_parallel(status_command, group))
    # install/upgrade commands
    group = [install_command]
    self.assertFalse(adm.can_be_executed_in_parallel(install_command, group))
    self.assertFalse(adm.can_be_executed_in_parallel(upgrade_command, group))
    self.assertFalse(adm.can_be_executed_in_parallel(status_command, group))
    self.assertFalse(adm.can_be_executed_in_parallel(start_command, group))
    group = [upgrade_command]
    self.assertFalse(adm.can_be_executed_in_parallel(install_command, group))
    self.assertFalse(adm.can_be_executed_in_parallel(upgrade_command, group))
    self.assertFalse(adm.can_be_executed_in_parallel(status_command, group))
    self.assertFalse(adm.can_be_executed_in_parallel(start_command, group))
    # Other commands
    group = [start_command]
    self.assertFalse(adm.can_be_executed_in_parallel(install_command, group))
    self.assertFalse(adm.can_be_executed_in_parallel(upgrade_command, group))
    self.assertFalse(adm.can_be_executed_in_parallel(status_command, group))
    self.assertTrue(adm.can_be_executed_in_parallel(hm_start_command, group))
    # Check dependency processing
    group = [start_command]
    self.assertFalse(adm.can_be_executed_in_parallel(rm_start_command, group))
    group = [start_command]
    self.assertTrue(adm.can_be_executed_in_parallel(hm_start_command, group))
    # actions for the same component
    group = [start_command]
    self.assertFalse(adm.can_be_executed_in_parallel(stop_command, group))
    group = [stop_command]
    self.assertFalse(adm.can_be_executed_in_parallel(start_command, group))

