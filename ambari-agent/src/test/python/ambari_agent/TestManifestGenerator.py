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
import os, sys, StringIO
from ambari_agent.AgentException import AgentException

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
    
  def testConvertRangeToList(self):
    
    rangesList = ["1-3", "4", "6", "7-9"]
    list = manifestGenerator.convertRangeToList(rangesList)
    self.assertEqual(sorted(list), sorted([1,2,3,4,6,7,8,9]))
    
    rangesList = ["5", "4"]
    list = manifestGenerator.convertRangeToList(rangesList)
    self.assertEqual(list, [5,4])

    exceptionWasTrown = False
    try:
      rangesList = ["0", "-2"]
      list = manifestGenerator.convertRangeToList(rangesList)
    except AgentException, err:
      #Expected
      exceptionWasTrown = True
      
    self.assertTrue(exceptionWasTrown)
    
    exceptionWasTrown = False
    try:
      rangesList = ["0", "-"]
      list = manifestGenerator.convertRangeToList(rangesList)
    except AgentException, err:
      #Expected
      exceptionWasTrown = True
    self.assertTrue(exceptionWasTrown)
    
    exceptionWasTrown = False
    try:
      rangesList = ["0", "2-"]
      list = manifestGenerator.convertRangeToList(rangesList)
    except AgentException, err:
      #Expected
      exceptionWasTrown = True
    self.assertTrue(exceptionWasTrown)
    
  def testConvertMappedRangeToList(self):
    mappedRangedList = ["1:0-2,5", "2:3,4"]
    list = manifestGenerator.convertMappedRangeToList(mappedRangedList)
    self.assertEqual(list, [1,1,1,2,2,1])
    
    mappedRangedList = ["7:0"]
    list = manifestGenerator.convertMappedRangeToList(mappedRangedList)
    self.assertEqual(list, [7])
    
    exceptionWasTrown = False
    mappedRangedList = ["7:0-"]
    try:
      list = manifestGenerator.convertMappedRangeToList(mappedRangedList)
    except AgentException, err:
      #Expected
      exceptionWasTrown = True
    self.assertTrue(exceptionWasTrown)
    
    
    exceptionWasTrown = False
    mappedRangedList = ["7:-"]
    try:
      list = manifestGenerator.convertMappedRangeToList(mappedRangedList)
    except AgentException, err:
      #Expected
      exceptionWasTrown = True
    self.assertTrue(exceptionWasTrown)
    
    exceptionWasTrown = False
    mappedRangedList = ["7:-1"]
    try:
      list = manifestGenerator.convertMappedRangeToList(mappedRangedList)
    except AgentException, err:
      #Expected
      exceptionWasTrown = True
    self.assertTrue(exceptionWasTrown)
    
  def testDecompressClusterHostInfo(self):

    all_hosts_key = "all_hosts"
    all_ping_ports_key = "all_ping_ports"
    ambari_server_host_key = "ambari_server_host"
    info = { "jtnode_host"        : ["5"],
             "hbase_master_hosts" : ["5"],
             all_hosts_key: ["h8", "h9", "h5", "h4", "h7", "h6", "h1", "h3", "h2", "h10"],
             "namenode_host"      : ["6"],
             "mapred_tt_hosts"    : ["0", "7-9", "2","3", "5"],
             "slave_hosts"        : ["3", "0", "1", "5-9"],
             "snamenode_host"     : ["8"],
             all_ping_ports_key: ["8670:1,5-8", "8673:9", "8672:0,4", "8671:2,3"],
             "hbase_rs_hosts"     : ["3", "1", "5", "8", "9"],
             ambari_server_host_key: ["h0"]
    }

    decompressedInfo = manifestGenerator.decompressClusterHostInfo(info)

    self.assertTrue(decompressedInfo.has_key(all_hosts_key))

    self.assertEquals(info.pop(all_hosts_key), decompressedInfo.get(all_hosts_key))

    self.assertEquals(['8672', '8670', '8671', '8671', '8672',
                       '8670', '8670', '8670', '8670', '8673'],
                      decompressedInfo.get(all_ping_ports_key))

    self.assertEquals(["h0"], decompressedInfo.get(ambari_server_host_key))

    for k,v in decompressedInfo.items():
      if k is all_ping_ports_key:
        continue # Skip checking this list
      # Check that list contains only host names
      non_converted = [x for x in v if not x.startswith("h")]
      self.assertEquals(0, len(non_converted))

