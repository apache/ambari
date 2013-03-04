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
import unittest
import StringIO
import socket
import os, sys, pprint
from mock.mock import patch
from mock.mock import MagicMock
from mock.mock import create_autospec
import os, errno, tempfile
from ambari_agent import UpgradeExecutor
import logging
from ambari_agent import AmbariConfig
from ambari_agent.StackVersionsFileHandler import StackVersionsFileHandler

class TestUpgradeExecutor(TestCase):

  logger = logging.getLogger()

  @patch.object(StackVersionsFileHandler, 'write_stack_version')
  @patch('os.path.isdir')
  def test_perform_stack_upgrade(self, isdir_method, write_stack_version_method):
    executor = UpgradeExecutor.UpgradeExecutor('pythonExecutor',
      'puppetExecutor', AmbariConfig.AmbariConfig().getConfig())

    # Checking matching versions
    command = {
      'commandParams' :	{
        'source_stack_version' : 'HDP-1.3.0',
        'target_stack_version' : 'HDP-1.3.0',
       },
      'component' : 'HDFS'
    }
    result = executor.perform_stack_upgrade(command, 'tmpout', 'tmperr')
    self.assertTrue('matches current stack version' in result['stdout'])
    self.assertFalse(write_stack_version_method.called)
    # Checking unsupported update
    write_stack_version_method.reset()
    command = {
      'commandParams' :	{
        'source_stack_version' : 'HDP-1.0.1',
        'target_stack_version' : 'HDP-1.3.0',
      },
      'component' : 'HDFS'
    }
    isdir_method.return_value = False
    result = executor.perform_stack_upgrade(command, 'tmpout', 'tmperr')
    self.assertTrue('not supported' in result['stderr'])
    self.assertFalse(write_stack_version_method.called)
    # Checking successful result
    write_stack_version_method.reset()
    command = {
      'commandParams' :	{
        'source_stack_version' : 'HDP-1.0.1',
        'target_stack_version' : 'HDP-1.3.0',
      },
      'component' : 'HDFS'
    }
    isdir_method.return_value = True
    executor.execute_dir = lambda command, basedir, dir, tmpout, tmperr : \
      {
        'exitcode' : 0,
        'stdout'   : "output - %s" % dir,
        'stderr'   : "errors - %s" % dir,
      }
    result = executor.perform_stack_upgrade(command, 'tmpout', 'tmperr')
    self.assertTrue(write_stack_version_method.called)
    self.assertEquals(result['exitcode'],0)
    self.assertEquals(result['stdout'],'output - pre-upgrade.d\noutput - upgrade.d\noutput - post-upgrade.d')
    self.assertEquals(result['stderr'],'errors - pre-upgrade.d\nerrors - upgrade.d\nerrors - post-upgrade.d')
    # Checking failed result
    write_stack_version_method.reset()
    command = {
      'commandParams' :	{
        'source_stack_version' : 'HDP-1.0.1',
        'target_stack_version' : 'HDP-1.3.0',
      },
      'component' : 'HDFS'
    }
    isdir_method.return_value = True
    executor.execute_dir = lambda command, basedir, dir, tmpout, tmperr :\
    {
      'exitcode' : 1,
      'stdout'   : "output - %s" % dir,
      'stderr'   : "errors - %s" % dir,
      }
    result = executor.perform_stack_upgrade(command, 'tmpout', 'tmperr')
    self.assertTrue(write_stack_version_method.called)
    self.assertEquals(result['exitcode'],1)
    self.assertEquals(result['stdout'],'output - pre-upgrade.d')
    self.assertEquals(result['stderr'],'errors - pre-upgrade.d')


  def test_get_key_func(self):
    executor = UpgradeExecutor.UpgradeExecutor('pythonExecutor',
                 'puppetExecutor', AmbariConfig.AmbariConfig().getConfig())
    # Checking unparseable
    self.assertEqual(executor.get_key_func('fdsfds'), 999)
    self.assertEqual(executor.get_key_func('99dfsfd'), 999)
    self.assertEqual(executor.get_key_func('-fdfds'), 999)
    # checking parseable
    self.assertEqual(executor.get_key_func('99'), 99)
    self.assertEqual(executor.get_key_func('45-install'), 45)
    self.assertEqual(executor.get_key_func('33-install-staff'), 33)
    #checking sorting of full list
    testlist1 = ['7-fdfd', '10-erewfds', '11-fdfdfd', '1-hh', '20-kk', '01-tt']
    testlist1.sort(key = executor.get_key_func)
    self.assertEqual(testlist1,
        ['1-hh', '01-tt', '7-fdfd', '10-erewfds', '11-fdfdfd', '20-kk'])


  def test_split_stack_version(self):
    executor = UpgradeExecutor.UpgradeExecutor('pythonExecutor',
             'puppetExecutor', AmbariConfig.AmbariConfig().getConfig())
    result = executor.split_stack_version("HDP-1.2.1")
    self.assertEquals(result, ('HDP', '1', '2'))
    result = executor.split_stack_version("HDP-1.3")
    self.assertEquals(result, ('HDP', '1', '3'))
    result = executor.split_stack_version("ComplexStackVersion-1.3.4.2.2")
    self.assertEquals(result, ('ComplexStackVersion', '1', '3'))
    pass


  @patch('os.listdir')
  @patch('os.path.isdir')
  @patch.object(UpgradeExecutor.UpgradeExecutor, 'get_key_func')
  def test_execute_dir(self, get_key_func_method, isdir_method, listdir_method):
    pythonExecutor = MagicMock()
    puppetExecutor = MagicMock()

    command = {'debug': 'command'}
    isdir_method.return_value = True
    # Mocking sort() method of list
    class MyList(list):
      pass
    files = MyList(['first.py', 'second.pp', 'third.py', 'fourth.nm',
             'fifth-failing.py', 'six.py'])
    files.sort = lambda key: None
    listdir_method.return_value = files
    # fifth-failing.py will fail
    pythonExecutor.run_file.side_effect = [
      {'exitcode' : 0,
       'stdout'   : "stdout - first.py",
       'stderr'   : "stderr - first.py"},
      {'exitcode' : 0,
       'stdout'   : "stdout - third.py",
       'stderr'   : "stderr - third.py"},
      {'exitcode' : 1,
       'stdout'   : "stdout - fifth-failing.py",
       'stderr'   : "stderr - fifth-failing.py"},
      {'exitcode' : 0,
       'stdout'   : "stdout - six.py",
       'stderr'   : "stderr - six.py"},
    ]
    puppetExecutor.just_run_one_file.side_effect = [
      {'exitcode' : 0,
       'stdout'   : "stdout - second.pp",
       'stderr'   : "stderr - second.pp"},
    ]

    executor = UpgradeExecutor.UpgradeExecutor(pythonExecutor,
        puppetExecutor, AmbariConfig.AmbariConfig().getConfig())

    result= executor.execute_dir(command, 'basedir', 'dir', 'tmpout', 'tmperr')
    self.assertEquals(result['exitcode'],1)
    self.assertEquals(result['stdout'],"\nstdout - first.py\nstdout - second.pp\nstdout - third.py\nUnrecognized file type, skipping: basedir/dir/fourth.nm\nstdout - fifth-failing.py")
    self.assertEquals(result['stderr'],"\nstderr - first.py\nstderr - second.pp\nstderr - third.py\nNone\nstderr - fifth-failing.py")


  @patch('os.listdir')
  @patch('os.path.isdir')
  def test_execute_dir_ignore_badly_named(self, isdir_method, listdir_method):
    pythonExecutor = MagicMock()
    puppetExecutor = MagicMock()

    command = {'debug': 'command'}
    isdir_method.return_value = True
    files = ['00-first.py', 'badly-named.pp', '10-second.pp', '20-wrong.cpp']
    listdir_method.return_value = files
    # fifth-failing.py will fail
    pythonExecutor.run_file.side_effect = [
      {'exitcode' : 0,
       'stdout'   : "stdout - python.py",
       'stderr'   : "stderr - python.py"},
    ]
    puppetExecutor.just_run_one_file.side_effect = [
      {'exitcode' : 0,
       'stdout'   : "stdout - puppet.pp",
       'stderr'   : "stderr - puppet.pp"},
    ]

    executor = UpgradeExecutor.UpgradeExecutor(pythonExecutor,
        puppetExecutor, AmbariConfig.AmbariConfig().getConfig())

    result= executor.execute_dir(command, 'basedir', 'dir', 'tmpout', 'tmperr')
    self.assertEquals(result['exitcode'],0)
    self.assertEquals(result['stdout'],'\nstdout - python.py\nstdout - puppet.pp\nUnrecognized file type, skipping: basedir/dir/20-wrong.cpp')
    self.assertEquals(result['stderr'],'\nstderr - python.py\nstderr - puppet.pp\nNone')

if __name__ == "__main__":
  unittest.main(verbosity=2)