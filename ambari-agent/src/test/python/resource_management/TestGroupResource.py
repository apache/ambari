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
from mock.mock import patch, MagicMock
from resource_management.core import Environment, Fail
from resource_management.core.resources import Group
from resource_management.core.system import System

import subprocess
import grp


@patch.object(System, "platform", new = 'redhat')
class TestGroupResource(TestCase):

  @patch.object(grp, "getgrnam")
  @patch.object(subprocess, "Popen")
  def test_action_create_nonexistent(self, popen_mock, getgrnam_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    popen_mock.return_value = subproc_mock
    getgrnam_mock.side_effect = KeyError()

    with Environment('/') as env:
      Group('hadoop',
            action='create',
            password='secure'
      )
    

    self.assertEqual(popen_mock.call_count, 1)
    popen_mock.assert_called_with(['/bin/bash', '--login', '-c', 'groupadd -p secure hadoop'], shell=False, preexec_fn=None, stderr=-2, stdout=-1, env=None, cwd=None)
    getgrnam_mock.assert_called_with('hadoop')


  @patch.object(grp, "getgrnam")
  @patch.object(subprocess, "Popen")
  def test_action_create_existent(self, popen_mock, getgrnam_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    popen_mock.return_value = subproc_mock
    getgrnam_mock.return_value = "mapred"

    with Environment('/') as env:
      Group('mapred',
            action='create',
            gid=2,
            password='secure'
      )
    

    self.assertEqual(popen_mock.call_count, 1)
    popen_mock.assert_called_with(['/bin/bash', '--login', '-c', 'groupmod -p secure -g 2 mapred'], shell=False, preexec_fn=None, stderr=-2, stdout=-1, env=None, cwd=None)
    getgrnam_mock.assert_called_with('mapred')


  @patch.object(grp, "getgrnam")
  @patch.object(subprocess, "Popen")
  def test_action_create_fail(self, popen_mock, getgrnam_mock):
    subproc_mock = MagicMock()
    subproc_mock.returncode = 1
    popen_mock.return_value = subproc_mock
    getgrnam_mock.return_value = "mapred"

    try:
      with Environment('/') as env:
        Group('mapred',
              action='create',
              gid=2,
              password='secure'
        )
      
      self.fail("Action 'create' should fail when checked_call fails")
    except Fail:
      pass
    self.assertEqual(popen_mock.call_count, 1)
    popen_mock.assert_called_with(['/bin/bash', '--login', '-c', 'groupmod -p secure -g 2 mapred'], shell=False, preexec_fn=None, stderr=-2, stdout=-1, env=None, cwd=None)
    getgrnam_mock.assert_called_with('mapred')


  @patch.object(grp, "getgrnam")
  @patch.object(subprocess, "Popen")
  def test_action_remove(self, popen_mock, getgrnam_mock):

    subproc_mock = MagicMock()
    subproc_mock.returncode = 0
    popen_mock.return_value = subproc_mock
    getgrnam_mock.return_value = "mapred"

    with Environment('/') as env:
      Group('mapred',
            action='remove'
      )
    

    self.assertEqual(popen_mock.call_count, 1)
    popen_mock.assert_called_with(['/bin/bash', '--login', '-c', 'groupdel mapred'], shell=False, preexec_fn=None, stderr=-2, stdout=-1, env=None, cwd=None)
    getgrnam_mock.assert_called_with('mapred')


  @patch.object(grp, "getgrnam")
  @patch.object(subprocess, "Popen")
  def test_action_remove_fail(self, popen_mock, getgrnam_mock):

    subproc_mock = MagicMock()
    subproc_mock.returncode = 1
    popen_mock.return_value = subproc_mock
    getgrnam_mock.return_value = "mapred"

    try:
      with Environment('/') as env:
        Group('mapred',
              action='remove'
        )
      
      self.fail("Action 'delete' should fail when checked_call fails")
    except Fail:
      pass

    self.assertEqual(popen_mock.call_count, 1)
    popen_mock.assert_called_with(['/bin/bash', '--login', '-c', 'groupdel mapred'], shell=False, preexec_fn=None, stderr=-2, stdout=-1, env=None, cwd=None)
    getgrnam_mock.assert_called_with('mapred')
