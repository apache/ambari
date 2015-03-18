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
import string
import random
import os
from unittest import TestCase
from ambari_commons import OSCheck
from ambari_agent.StatusCheck import StatusCheck
import logging

from mock.mock import patch
from mock.mock import MagicMock

from only_for_platform import only_for_platform, get_platform, PLATFORM_LINUX, PLATFORM_WINDOWS

if get_platform() != PLATFORM_WINDOWS:
  os_distro_value = ('Suse','11','Final')
else:
  os_distro_value = ('win2012serverr2','6.3','WindowsServer')

USERNAME_LENGTH=10
USERNAME_CHARS=string.ascii_uppercase +string.ascii_lowercase + string.digits + '-_'

PID_DIR='/pids_dir'

COMPONENT_LIVE = 'LIVE_COMPONENT'
COMPONENT_LIVE_PID = 'live_' + StatusCheck.USER_PATTERN + '_comp.pid'

COMPONENT_DEAD = 'DEAD_COMPONENT'
COMPONENT_DEAD_PID = 'dead_' + StatusCheck.USER_PATTERN + '_comp.pid'

class TestStatusCheck(TestCase):

  logger = logging.getLogger()

  def generateUserName(self):
    return ''.join(random.choice(USERNAME_CHARS) for x in range(USERNAME_LENGTH))

  def setUp(self):

    self.pidPathVars = [
      {'var' : '',
      'defaultValue' : PID_DIR}
    ]

    self.serviceToPidDict = {
      COMPONENT_LIVE : COMPONENT_LIVE_PID,
      COMPONENT_DEAD : COMPONENT_DEAD_PID
    }

    live_user = self.generateUserName()
    self.logger.info('Live user: ' + live_user)
    self.live_pid_file_name = string.replace(COMPONENT_LIVE_PID, StatusCheck.USER_PATTERN, live_user)
    self.live_pid_full_path = PID_DIR + os.sep + self.live_pid_file_name

    dead_user = self.generateUserName()
    self.logger.info('Dead user: ' + live_user)
    self.dead_pid_file_name = string.replace(COMPONENT_DEAD_PID, StatusCheck.USER_PATTERN, dead_user)
    self.dead_pid_full_path = PID_DIR + os.sep + self.dead_pid_file_name

    self.pidFilesDict = {self.live_pid_file_name : self.live_pid_full_path,
                         self.dead_pid_file_name : self.dead_pid_full_path}

    self.is_live_values = {self.live_pid_full_path : True,
                      self.dead_pid_full_path : False}
    
    self.servicesToLinuxUser = {COMPONENT_LIVE : 'live_user',
                                COMPONENT_DEAD : 'dead_user'}

    self.globalConfig = {'live_user' : live_user,
                         'dead_user' : dead_user}

    
  # Ensure that status checker return True for running process
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(StatusCheck, 'getIsLive')
  def test_live(self, get_is_live_mock):

    statusCheck = StatusCheck(self.serviceToPidDict, self.pidPathVars,
      self.globalConfig, self.servicesToLinuxUser)

    self.assertTrue(StatusCheck.USER_PATTERN in self.serviceToPidDict[COMPONENT_LIVE])
    self.assertTrue(StatusCheck.USER_PATTERN in self.serviceToPidDict[COMPONENT_DEAD])

    statusCheck.pidFilesDict = self.pidFilesDict
    
    get_is_live_mock.side_effect = lambda pid_path : self.is_live_values[pid_path]
    
    status = statusCheck.getStatus(COMPONENT_LIVE)
    self.assertEqual(status, True)

  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(logger, 'info')
  def test_dont_relog_serToPidDict(self, logger_info_mock):
    TestStatusCheck.timesLogged = 0

    def my_side_effect(*args, **kwargs):
      TestStatusCheck.timesLogged += args[0].find('Service to pid dictionary: ')+1
      

    logger_info_mock.side_effect = my_side_effect
    
    # call this three times
    statusCheck = StatusCheck(self.serviceToPidDict, self.pidPathVars,
      self.globalConfig, self.servicesToLinuxUser)
    statusCheck = StatusCheck(self.serviceToPidDict, self.pidPathVars,
      self.globalConfig, self.servicesToLinuxUser)
    statusCheck = StatusCheck(self.serviceToPidDict, self.pidPathVars,
      self.globalConfig, self.servicesToLinuxUser)
    # logged not more then once
    self.assert_(TestStatusCheck.timesLogged <= 1, "test_dont_relog_serToPidDict logged more then once")

  # Ensure that status checker return True for running process even if multiple
  # pids for a service component exist
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(StatusCheck, 'getIsLive')
  def test_live_if_multiple_pids(self, get_is_live_mock):

    one_more_pid_file_name = string.replace(COMPONENT_LIVE_PID, StatusCheck.USER_PATTERN,
      'any_other_linux_user')
    one_more_pid_full_path = PID_DIR + os.sep + one_more_pid_file_name

    self.pidFilesDict[one_more_pid_file_name] = one_more_pid_full_path
    self.is_live_values[one_more_pid_full_path] = False

    statusCheck = StatusCheck(self.serviceToPidDict, self.pidPathVars,
      self.globalConfig, self.servicesToLinuxUser)

    statusCheck.pidFilesDict = self.pidFilesDict

    get_is_live_mock.side_effect = lambda pid_path : self.is_live_values[pid_path]

    status = statusCheck.getStatus(COMPONENT_LIVE)
    self.assertEqual(status, True)
    
  # Ensure that status checker prints error message if there is no linux user
  # for service, which pid depends on user
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(StatusCheck, 'getIsLive')
  @patch.object(logger, "error")
  def test_no_user_mapping(self, error_mock, get_is_live_mock):

    
    badServiceToPidDict = self.serviceToPidDict.copy()
    badServiceToPidDict['BAD_COMPONENT'] = 'prefix' + StatusCheck.USER_PATTERN

    statusCheck = StatusCheck(badServiceToPidDict, self.pidPathVars,
      self.globalConfig, self.servicesToLinuxUser)

    statusCheck.pidFilesDict = self.pidFilesDict

    get_is_live_mock.side_effect = lambda pid_path : self.is_live_values[pid_path]

    status = statusCheck.getStatus(COMPONENT_LIVE)
    self.assertTrue(error_mock.called)

  # Ensure that status checker return False for dead process
  @patch.object(OSCheck, "os_distribution", new = MagicMock(return_value = os_distro_value))
  @patch.object(StatusCheck, 'getIsLive')
  def test_dead(self, get_is_live_mock):
    statusCheck = StatusCheck(self.serviceToPidDict, self.pidPathVars,
      self.globalConfig, self.servicesToLinuxUser)

    statusCheck.pidFilesDict = self.pidFilesDict
    
    get_is_live_mock.side_effect = lambda pid_path : self.is_live_values[pid_path]
    status = statusCheck.getStatus(COMPONENT_DEAD)
    self.assertEqual(status, False)
