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
from ambari_agent.RepoInstaller import RepoInstaller
import tempfile
import json
import shutil
from ambari_agent.AmbariConfig import AmbariConfig
from mock.mock import patch, MagicMock, call

class TestRepoInstaller(TestCase):

  def setUp(self):
    self.dir = tempfile.mkdtemp()
    jsonCommand = file('../../main/python/ambari_agent/test.json').read()
    self.parsedJson= json.loads(jsonCommand)
    self.config = AmbariConfig().getConfig()
    self.repoInstaller = RepoInstaller(self.parsedJson, self.dir, '../../main/puppet/modules', 1, self.config)

    pass

  def tearDown(self):
    shutil.rmtree(self.dir)
    pass


  @patch.object(RepoInstaller, 'prepareReposInfo')
  @patch.object(RepoInstaller, 'generateFiles')
  def testInstallRepos(self, generateFilesMock, prepareReposInfoMock):
    result = self.repoInstaller.installRepos()
    self.assertTrue(prepareReposInfoMock.called)
    self.assertTrue(generateFilesMock.called)
    print('installRepos result: ' + result.__str__())
    pass
