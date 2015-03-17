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

import os

from unittest import TestCase
from mock.mock import patch
from resource_management import *
from resource_management.libraries.resources.execute_hadoop\
  import ExecuteHadoop

@patch.object(System, "os_family", new = 'redhat')
class TestExecuteHadoopResource(TestCase):
  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_default_args(self, execute_mock):
    '''
    Test if default arguments are passed to Execute
    '''
    with Environment() as env:
      ExecuteHadoop("command",
                    conf_dir="conf_dir",
                    user="user",
                    logoutput=True,
      )
      self.assertEqual(execute_mock.call_count, 1)
      self.assertEqual(execute_mock.call_args[0][0].command,'hadoop --config conf_dir command')
      self.assertEqual(execute_mock.call_args[0][0].arguments,
                       {'logoutput': True,
                        'tries': 1,
                        'user': 'user',
                        'try_sleep': 0,
                        'path': [],
                        'environment': {}})


  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_unknown_conf(self, execute_mock):
    '''
    Test when UnknownConfiguration passed
    '''
    with Environment() as env:
      ExecuteHadoop("command",
                    kinit_path_local=UnknownConfiguration(name="kinit_path_local"),
                    conf_dir="conf_dir",
                    user="user",
                    keytab=UnknownConfiguration(name="keytab"),
                    security_enabled=False,
                    principal=UnknownConfiguration(name="principal")
                    )
      self.assertEqual(execute_mock.call_count, 1)
      self.assertEqual(execute_mock.call_args[0][0].command,'hadoop --config conf_dir command')
      self.assertEqual(execute_mock.call_args[0][0].arguments,
                       {'logoutput': None,
                        'tries': 1,
                        'user': 'user',
                        'try_sleep': 0,
                        'path': [],
                        'environment': {}})


  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_defined_args(self, execute_mock):
    '''
    Test if defined arguments are passed to Execute
    '''
    with Environment("/") as env:
      ExecuteHadoop("command",
                    action="run",
                    kinit_path_local="path",
                    conf_dir="conf_dir",
                    user="user",
                    tries=2,
                    keytab="keytab",
                    security_enabled=False,
                    kinit_override=False,
                    try_sleep=2,
                    logoutput=True,
                    principal="principal"
      )
      self.assertEqual(execute_mock.call_count, 1)
      self.assertEqual(execute_mock.call_args[0][0].command,'hadoop --config conf_dir command')
      self.assertEqual(execute_mock.call_args[0][0].arguments,
                       {'logoutput': True,
                        'tries': 2,
                        'user': 'user',
                        'try_sleep': 2,
                        'path': [],
                        'environment': {}})


  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_command_list(self, execute_mock):
    '''
    Test for "command" passed as List
    '''
    with Environment("/") as env:
      ExecuteHadoop(["command1","command2"],
                    action="run",
                    kinit_path_local="path",
                    conf_dir="conf_dir",
                    user="user",
                    keytab="keytab"
      )
      self.assertEqual(execute_mock.call_count, 2)
      self.assertEqual(execute_mock.call_args_list[0][0][0].command,
                       'hadoop --config conf_dir command1')
      self.assertEqual(execute_mock.call_args_list[1][0][0].command,
                       'hadoop --config conf_dir command2')
      self.assertEqual(execute_mock.call_args_list[0][0][0].arguments,
                       {'logoutput': None,
                        'tries': 1,
                        'user': 'user',
                        'environment': {},
                        'try_sleep': 0,
                        'path': []})
      self.assertEqual(execute_mock.call_args_list[1][0][0].arguments,
                       {'logoutput': None,
                        'tries': 1,
                        'user': 'user',
                        'try_sleep': 0,
                        'path': [],
                        'environment': {}})


  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_command_tuple(self, execute_mock):
    '''
    Test for "command" passed as Tuple
    '''
    with Environment("/") as env:
      ExecuteHadoop(("command1","command2","command3"),
                    action="run",
                    kinit_path_local="path",
                    conf_dir="conf_dir",
                    user="user",
                    keytab="keytab"
      )
      self.assertEqual(execute_mock.call_count, 1)
      self.assertEqual(execute_mock.call_args[0][0].command,
                       'hadoop --config conf_dir command1 command2 command3')


  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_secured(self, execute_mock):
    '''
    Test security_enabled=True behaviour
    '''
    with Environment("/") as env:
      ExecuteHadoop("command",
                    action="run",
                    kinit_path_local="path",
                    conf_dir="conf_dir",
                    user="user",
                    principal="principal",
                    tries=1,
                    keytab="keytab",
                    security_enabled=True,
                    kinit_override=False,
                    try_sleep=0,
                    logoutput=True
      )
      self.assertEqual(execute_mock.call_count, 2)
      self.assertEqual(str(execute_mock.call_args_list[0][0][0]),
                       'Execute[\'path -kt keytab principal\']')
      self.assertEqual(execute_mock.call_args_list[0][0][0].command,
                       'path -kt keytab principal')
      self.assertEqual(execute_mock.call_args_list[0][0][0].arguments,
                       {'path': ['/bin'], 'user': 'user'})
      self.assertEqual(execute_mock.call_args_list[1][0][0].command,
                       'hadoop --config conf_dir command')
      self.assertEqual(execute_mock.call_args_list[1][0][0].arguments,
                       {'logoutput': True,
                        'tries': 1,
                        'user': 'user',
                        'try_sleep': 0,
                        'path': [],
                        'environment': {}})


  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_secured_kinit_override(self, execute_mock):
    '''
    Test security_enabled=True and kinit_override=True behaviour
    '''
    with Environment("/") as env:
      ExecuteHadoop("command",
                    action="run",
                    kinit_path_local="path",
                    conf_dir="conf_dir",
                    user="user",
                    tries=1,
                    keytab="keytab",
                    security_enabled=True,
                    kinit_override=True,
                    try_sleep=0,
                    logoutput=True
      )
      self.assertEqual(execute_mock.call_count, 1)
      self.assertEqual(execute_mock.call_args_list[0][0][0].command,
                       'hadoop --config conf_dir command')


  @patch("resource_management.core.providers.system.ExecuteProvider")
  def test_run_secured_principal(self, execute_mock):
    '''
    Test with "principal" argument
    '''
    with Environment("/") as env:
      ExecuteHadoop("command",
                    action="run",
                    kinit_path_local="path",
                    conf_dir="conf_dir",
                    user="user",
                    tries=1,
                    keytab="keytab",
                    security_enabled=True,
                    kinit_override=False,
                    try_sleep=0,
                    logoutput=True,
                    principal="principal")
      self.assertEqual(execute_mock.call_count, 2)
      self.assertEqual(execute_mock.call_args_list[0][0][0].command,
                       'path -kt keytab principal')
      self.assertEqual(execute_mock.call_args_list[1][0][0].command,
                       'hadoop --config conf_dir command')
