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
from ambari_agent.FileUtil import writeFile, createStructure, deleteStructure
import os, errno

class TestFileUtil(TestCase):
  def test_createStructure(self):
    action = { 'clusterId' : 'abc', 'role' : 'hdfs', 'workDirComponent' : 'abc-hdfs' }
    result = {}
    result = createStructure(action, result)
    self.assertEqual(result['exitCode'], 0, 'Create cluster structure failed.')

#  def test_writeFile(self):
    configFile = {
      "data"       : "test",
      "owner"      : os.getuid(),
      "group"      : os.getgid() ,
      "permission" : 0700,
      "path"       : "/tmp/ambari_file_test/_file_write_test",
      "umask"      : 022
    }
    action = { 
      'clusterId' : 'abc', 
      'role' : 'hdfs', 
      'workDirComponent' : 'abc-hdfs',
      'file' : configFile 
    }
    result = { }
    result = writeFile(action, result)
    self.assertEqual(result['exitCode'], 0, 'WriteFile test with uid/gid failed.')

#  def test_deleteStructure(self):
    result = { }
    action = { 'clusterId' : 'abc', 'role' : 'hdfs', 'workDirComponent' : 'abc-hdfs' }
    result = deleteStructure(action, result)
    self.assertEqual(result['exitCode'], 0, 'Delete cluster structure failed.')

