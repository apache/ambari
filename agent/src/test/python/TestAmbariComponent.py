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
import ambari_component
import os
import shutil

class TestAmbariComponent(TestCase):

  def setUp(self):
    global oldCwd, tmp
    tmp = "/tmp/config/hadoop"
    oldCwd = os.getcwd()
    os.chdir("/tmp")
    if not os.path.exists(tmp):
      os.makedirs(tmp)

  def tearDown(self):
    global oldCwd, tmp
    shutil.rmtree(tmp)
    os.chdir(oldCwd)

  def test_copySh(self):
    result = ambari_component.copySh(os.getuid(), os.getgid(), 0700, 'hadoop/hadoop-env', 
      {
        'HADOOP_CONF_DIR'      : '/etc/hadoop',
        'HADOOP_NAMENODE_OPTS' : '-Dsecurity.audit.logger=INFO,DRFAS'
      }
    )
    self.assertEqual(result['exitCode'], 0)

  def test_copyProperties(self):
    result = ambari_component.copyProperties(os.getuid(), os.getgid(), 0700, 'hadoop/hadoop-metrics2',
      {
        '*.period':'60'
      }
    )
    self.assertEqual(result['exitCode'], 0)

  def test_copyXml(self):
    result = ambari_component.copyXml(os.getuid(), os.getgid(), 0700, 'hadoop/core-site',
      {
        'local.realm'     : '${KERBEROS_REALM}',
        'fs.default.name' : 'hdfs://localhost:8020'
      }
    )
    self.assertEqual(result['exitCode'], 0)
