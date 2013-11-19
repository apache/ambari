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

import subprocess, os
import tempfile
from unittest import TestCase
from ambari_agent.Hardware import Hardware
from mock.mock import MagicMock, patch, ANY
import mock.mock
from AmbariConfig import AmbariConfig

class TestHardware(TestCase):
  def test_build(self):
    hardware = Hardware(AmbariConfig().getConfig())
    result = hardware.get()
    osdisks = hardware.osdisks()
    for dev_item in result['mounts']:
      self.assertTrue(dev_item['available'] >= 0)
      self.assertTrue(dev_item['used'] >= 0)
      self.assertTrue(dev_item['percent'] != None)
      self.assertTrue(dev_item['device'] != None)
      self.assertTrue(dev_item['mountpoint'] != None)
      self.assertTrue(dev_item['type'] != None)
      self.assertTrue(dev_item['size'] > 0)

    for os_disk_item in osdisks:
      self.assertTrue(os_disk_item['available'] >= 0)
      self.assertTrue(os_disk_item['used'] >= 0)
      self.assertTrue(os_disk_item['percent'] != None)
      self.assertTrue(os_disk_item['device'] != None)
      self.assertTrue(os_disk_item['mountpoint'] != None)
      self.assertTrue(os_disk_item['type'] != None)
      self.assertTrue(os_disk_item['size'] > 0)

    self.assertTrue(len(result['mounts']) == len(osdisks))


  @patch.object(subprocess, "Popen")
  @patch.object(Hardware, "facterLib")
  @patch("os.path.exists")
  def test_facterInfo(self, os_path_exists_mock, hardware_facterLib_mock, subprocess_popen_mock):
    config = AmbariConfig().getConfig()
    tmp_dir = tempfile.gettempdir()
    config.set("puppet", "facter_home", tmp_dir)
    hardware = Hardware(config)
    facter = MagicMock()
    facter.communicate.return_value = ["memoryfree => 1 GB\n memorysize => 25 MB\n memorytotal => 300 KB\n "
                                        + "physicalprocessorcount => 25\n is_virtual => true\n", "no errors"]
    facter.returncode = 0
    os.environ['RUBYLIB'] = tmp_dir;
    subprocess_popen_mock.return_value = facter
    os_path_exists_mock.return_value = True
    hardware_facterLib_mock.return_value = "bla bla bla"
    facterInfo = hardware.facterInfo()

    self.assertEquals(facterInfo['memoryfree'], 1048576L)
    self.assertEquals(facterInfo['memorysize'], 25600L)
    self.assertEquals(facterInfo['memorytotal'], 300L)
    self.assertEquals(facterInfo['physicalprocessorcount'], 25)
    self.assertTrue(facterInfo['is_virtual'])
    self.assertEquals(subprocess_popen_mock.call_args[1]['env']['RUBYLIB'],
                      tmp_dir + ":" + "bla bla bla")

    facter.communicate.return_value = ["memoryfree => 1 G\n memorysize => 25 M\n memorytotal => 300 K\n "
                                         + "someinfo => 12 Byte\n ssh_name_key => Aa06Fdd\n", "no errors"]
    facterInfo = hardware.facterInfo()
    facter.returncode = 1
    self.assertEquals(facterInfo['memoryfree'], 1048576L)
    self.assertEquals(facterInfo['memorysize'], 25600L)
    self.assertEquals(facterInfo['memorytotal'], 300L)
    self.assertEquals(facterInfo['someinfo'], '12 Byte')
    self.assertFalse(facterInfo.has_key('ssh_name_key'))

    facter.communicate.return_value = ["memoryfree => 1024 M B\n memorytotal => 1024 Byte" , "no errors"]

    facterInfo = hardware.facterInfo()

    self.assertEquals(facterInfo['memoryfree'], 1L)
    self.assertEquals(facterInfo['memorytotal'], 1L)

    os_path_exists_mock.return_value = False
    facterInfo = hardware.facterInfo()

    self.assertEquals(facterInfo, {})


  @patch("os.path.exists")
  def test_facterBin(self, ps_path_exists_mock):
    hardware = Hardware(AmbariConfig().getConfig())
    ps_path_exists_mock.return_value = False
    result = hardware.facterBin("bla bla bla")
    self.assertEquals(result, "facter")

    ps_path_exists_mock.return_value = True
    result = hardware.facterBin("bla bla bla")
    self.assertEquals(result, "bla bla bla/bin/facter")


  @patch("os.path.exists")
  @patch.dict('os.environ', {"PATH": ""})
  @patch.object(subprocess, "Popen")
  @patch.object(Hardware, "facterInfo")
  def test_configureEnviron(self, hrdware_facterinfo_mock, subproc_popen, os_path_exists_mock):
    config = AmbariConfig().getConfig()
    tmpdir = tempfile.gettempdir()
    config.set("puppet", "ruby_home", tmpdir)
    hardware = Hardware(config)
    os_path_exists_mock.return_value = True
    result = hardware.configureEnviron({'PATH': ""})

    self.assertEquals(result['PATH'], tmpdir + "/bin:")
    self.assertEquals(result['MY_RUBY_HOME'], tmpdir)
    config.remove_option("puppet", "ruby_home")


  def test_facterLib(self):
    hardware = Hardware(AmbariConfig().getConfig())
    facterLib = hardware.facterLib("/home")
    self.assertEquals(facterLib, "/home/lib/")


  def test_extractMountInfo(self):
    outputLine = "device type size used available percent mountpoint"
    result = Hardware.extractMountInfo(outputLine)

    self.assertEquals(result['device'], 'device')
    self.assertEquals(result['type'], 'type')
    self.assertEquals(result['size'], 'size')
    self.assertEquals(result['used'], 'used')
    self.assertEquals(result['available'], 'available')
    self.assertEquals(result['percent'], 'percent')
    self.assertEquals(result['mountpoint'], 'mountpoint')

    outputLine = ""
    result = Hardware.extractMountInfo(outputLine)

    self.assertEquals(result, None)

    outputLine = "device type size used available percent"
    result = Hardware.extractMountInfo(outputLine)

    self.assertEquals(result, None)

    outputLine = "device type size used available percent mountpoint info"
    result = Hardware.extractMountInfo(outputLine)

    self.assertEquals(result, None)




