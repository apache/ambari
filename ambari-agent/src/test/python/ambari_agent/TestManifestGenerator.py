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
import os, sys, StringIO

from unittest import TestCase
from ambari_agent import manifestGenerator
import ambari_agent.AmbariConfig
import tempfile
import json
import shutil
from ambari_agent.AmbariConfig import AmbariConfig
from mock.mock import patch, MagicMock, call


class TestManifestGenerator(TestCase):

  def setUp(self):
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out

    self.dir = tempfile.mkdtemp()
    self.config = AmbariConfig()
    jsonCommand = file('../../main/python/ambari_agent/test.json').read()
    self.parsedJson = json.loads(jsonCommand)


  def tearDown(self):
    shutil.rmtree(self.dir)

    # enable stdout
    sys.stdout = sys.__stdout__


  def testWriteImports(self):
    tmpFileName = tempfile.mkstemp(dir=self.dir, text=True)[1]
    print tmpFileName
    tmpFile = file(tmpFileName, 'r+')

    manifestGenerator.writeImports(tmpFile, '../../main/puppet/modules', self.config.getImports())
    tmpFile.seek(0)
    print tmpFile.read()
    tmpFile.close()


    pass

  @patch.object(manifestGenerator, 'writeImports')
  @patch.object(manifestGenerator, 'writeNodes')
  @patch.object(manifestGenerator, 'writeParams')
  @patch.object(manifestGenerator, 'writeTasks')
  def testGenerateManifest(self, writeTasksMock, writeParamsMock, writeNodesMock, writeImportsMock):
    tmpFileName = tempfile.mkstemp(dir=self.dir, text=True)[1]
    self.parsedJson['roleParams'] = 'role param'
    manifestGenerator.generateManifest(self.parsedJson, tmpFileName, '../../main/puppet/modules', self.config.getConfig())

    self.assertTrue(writeParamsMock.called)
    self.assertTrue(writeNodesMock.called)
    self.assertTrue(writeImportsMock.called)
    self.assertTrue(writeTasksMock.called)

    print file(tmpFileName).read()

    def raiseTypeError():
      raise TypeError()
    writeNodesMock.side_effect = raiseTypeError
    manifestGenerator.generateManifest(self.parsedJson, tmpFileName, '../../main/puppet/modules', self.config.getConfig())
    pass

  def testEscape(self):
    shouldBe = '\\\'\\\\'
    result = manifestGenerator.escape('\'\\')
    self.assertEqual(result, shouldBe)


  def test_writeNodes(self):
    tmpFileName = tempfile.mkstemp(dir=self.dir, text=True)[1]
    tmpFile = file(tmpFileName, 'r+')

    clusterHostInfo = self.parsedJson['clusterHostInfo']
    clusterHostInfo['zookeeper_hosts'] = ["h1.hortonworks.com", "h2.hortonworks.com"]
    manifestGenerator.writeNodes(tmpFile, clusterHostInfo)
    tmpFile.seek(0)
    print tmpFile.read()
    tmpFile.close()
    os.remove(tmpFileName)

  def test_writeNodes_failed(self):
    tmpFileName = tempfile.mkstemp(dir=self.dir, text=True)[1]
    tmpFile = file(tmpFileName, 'r+')

    clusterHostInfo = self.parsedJson['clusterHostInfo']
    clusterHostInfo.update({u'ZOOKEEPER':[None]})
    clusterHostInfo['zookeeper_hosts'] = ["h1.hortonworks.com", "h2.hortonworks.com"]
    self.assertRaises(TypeError, manifestGenerator.writeNodes, tmpFile, clusterHostInfo)
    tmpFile.seek(0)
    print tmpFile.read()
    tmpFile.close()
    os.remove(tmpFileName)

  def test_writeHostAttributes(self):
    tmpFileName = tempfile.mkstemp(dir=self.dir, text=True)[1]
    tmpFile = file(tmpFileName, 'r+')

    hostAttributes = {'HostAttr1' : '1', 'HostAttr2' : '2'}
    manifestGenerator.writeHostAttributes(tmpFile, hostAttributes)
    tmpFile.seek(0)
    print tmpFile.read()
    tmpFile.close()
    os.remove(tmpFileName)


  def test_writeTasks(self):
    tmpFileName = tempfile.mkstemp(dir=self.dir, text=True)[1]
    tmpFile = file(tmpFileName, 'r+')
    roles = [{'role' : 'ZOOKEEPER_SERVER',
              'cmd' : 'NONE',
              'roleParams' : {'someRoleParams': '-x'}}]
    clusterHostInfo = self.parsedJson['clusterHostInfo']
    clusterHostInfo['zookeeper_hosts'] = ["h1.hortonworks.com", "h2.hortonworks.com"]
    manifestGenerator.writeTasks(tmpFile, roles, self.config, clusterHostInfo, "h1.hortonworks.com")
    tmpFile.seek(0)
    print tmpFile.read()
    tmpFile.close()
    os.remove(tmpFileName)
