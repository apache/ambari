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
from ambari_agent.StatusCheck import StatusCheck
import AmbariConfig
import logging
from mock.mock import patch, Mock


USERNAME_LENGTH=10
USERNAME_CHARS=string.ascii_uppercase +string.ascii_lowercase + string.digits + '-_'

PID_DIR='/pids_dir'

COMPONENT_LIVE = 'LIVE_COMPONENT'
COMPONENT_LIVE_PID = 'live_{USER}_comp.pid'

COMPONENT_DEAD = 'DEAD_COMPONENT'
COMPONENT_DEAD_PID = 'dead_{USER}_comp.pid'

class TestStatusCheck(TestCase):

  logger = logging.getLogger()

  def generateUserName(self):
    return ''.join(random.choice(USERNAME_CHARS) for x in range(USERNAME_LENGTH))

  def setUp(self):

    self.pidPathesVars = [
      {'var' : '',
      'defaultValue' : PID_DIR}
    ]

    self.serviceToPidDict = {
      COMPONENT_LIVE : COMPONENT_LIVE_PID,
      COMPONENT_DEAD : COMPONENT_DEAD_PID
    }

    live_user = self.generateUserName()
    self.logger.info('Live user: ' + live_user)
    self.live_pid_file_name = string.replace(COMPONENT_LIVE_PID, '{USER}', live_user)
    self.live_pid_full_path = PID_DIR + os.sep + self.live_pid_file_name

    dead_user = self.generateUserName()
    self.logger.info('Dead user: ' + live_user)
    self.dead_pid_file_name = string.replace(COMPONENT_DEAD_PID, '{USER}', dead_user)
    self.dead_pid_full_path = PID_DIR + os.sep + self.dead_pid_file_name

    self.pidFilesDict = {self.live_pid_file_name : self.live_pid_full_path,
                         self.dead_pid_file_name : self.dead_pid_full_path}

    self.is_live_values = {self.live_pid_full_path : True,
                      self.dead_pid_full_path : False}

    
  # Ensure that status checker return True for running process
  @patch.object(StatusCheck, 'getIsLive')
  def test_live(self, get_is_live_mock):

    statusCheck = StatusCheck(self.serviceToPidDict, self.pidPathesVars,{},AmbariConfig.linuxUserPattern)

    statusCheck.pidFilesDict = self.pidFilesDict
    
    get_is_live_mock.side_effect = lambda pid_path : self.is_live_values[pid_path]
    
    status = statusCheck.getStatus(COMPONENT_LIVE)
    self.assertEqual(status, True)

  # Ensure that status checker return False for dead process
  @patch.object(StatusCheck, 'getIsLive')
  def test_dead(self, get_is_live_mock):
    statusCheck = StatusCheck(self.serviceToPidDict, self.pidPathesVars,{},AmbariConfig.linuxUserPattern)

    statusCheck.pidFilesDict = self.pidFilesDict
    
    get_is_live_mock.side_effect = lambda pid_path : self.is_live_values[pid_path]
    status = statusCheck.getStatus(COMPONENT_DEAD)
    self.assertEqual(status, False)
