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

from unittest import TestCase
from mock.mock import patch
import unittest
import platform

with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  from ambari_agent import hostname
  from ambari_agent.Hardware import Hardware
  from ambari_agent.Facter import Facter
  from common_functions import OSCheck

@patch.object(platform,"linux_distribution", new = ('Suse','11','Final'))
class TestHardware(TestCase):
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_build(self, get_os_version_mock, get_os_type_mock):
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    hardware = Hardware()
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

  def test_extractMountInfo(self):
    outputLine = "device type size used available percent mountpoint"
    result = Hardware.extractMountInfo(outputLine)

    self.assertEqual(result['device'], 'device')
    self.assertEqual(result['type'], 'type')
    self.assertEqual(result['size'], 'size')
    self.assertEqual(result['used'], 'used')
    self.assertEqual(result['available'], 'available')
    self.assertEqual(result['percent'], 'percent')
    self.assertEqual(result['mountpoint'], 'mountpoint')

    outputLine = ""
    result = Hardware.extractMountInfo(outputLine)

    self.assertEqual(result, None)

    outputLine = "device type size used available percent"
    result = Hardware.extractMountInfo(outputLine)

    self.assertEqual(result, None)

    outputLine = "device type size used available percent mountpoint info"
    result = Hardware.extractMountInfo(outputLine)

    self.assertEqual(result, None)

  @patch.object(hostname,"hostname")
  @patch.object(Facter, "getFqdn")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_fqdnDomainHostname(self, get_os_version_mock, get_os_type_mock, facter_getFqdn_mock, hostname_mock):
    facter_getFqdn_mock.return_value = "ambari.apache.org"
    hostname_mock.return_value = 'ambari'
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    result = Facter().facterInfo()

    self.assertEqual(result['hostname'], "ambari")
    self.assertEqual(result['domain'], "apache.org")
    self.assertEqual(result['fqdn'], (result['hostname'] + '.' + result['domain']))

  @patch.object(Facter, "setDataUpTimeOutput")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_uptimeSecondsHoursDays(self, get_os_version_mock, get_os_type_mock, facter_setDataUpTimeOutput_mock):
    # 3 days + 1 hour + 13 sec
    facter_setDataUpTimeOutput_mock.return_value = "262813.00 123.45"
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    result = Facter().facterInfo()

    self.assertEqual(result['uptime_seconds'], '262813')
    self.assertEqual(result['uptime_hours'], '73')
    self.assertEqual(result['uptime_days'], '3')

  @patch.object(Facter, "setMemInfoOutput")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_facterMemInfoOutput(self, get_os_version_mock, get_os_type_mock, facter_setMemInfoOutput_mock):

    facter_setMemInfoOutput_mock.return_value = '''
MemTotal:        1832392 kB
MemFree:          868648 kB
HighTotal:             0 kB
HighFree:              0 kB
LowTotal:        1832392 kB
LowFree:          868648 kB
SwapTotal:       2139592 kB
SwapFree:        1598676 kB
    '''

    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    result = Facter().facterInfo()

    self.assertEqual(result['memorysize'], 1832392)
    self.assertEqual(result['memorytotal'], 1832392)
    self.assertEqual(result['memoryfree'], 868648)
    self.assertEqual(result['swapsize'], '2.04 GB')
    self.assertEqual(result['swapfree'], '1.52 GB')

  @patch.object(Facter, "setDataIfConfigOutput")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_facterDataIfConfigOutput(self, get_os_version_mock, get_os_type_mock, facter_setDataIfConfigOutput_mock):

    facter_setDataIfConfigOutput_mock.return_value = '''
eth0      Link encap:Ethernet  HWaddr 08:00:27:C9:39:9E
          inet addr:10.0.2.15  Bcast:10.0.2.255  Mask:255.255.255.0
          inet6 addr: fe80::a00:27ff:fec9:399e/64 Scope:Link
          UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1
          RX packets:7575 errors:0 dropped:0 overruns:0 frame:0
          TX packets:3463 errors:0 dropped:0 overruns:0 carrier:0
          collisions:0 txqueuelen:1000
          RX bytes:9383574 (8.9 MiB)  TX bytes:231609 (226.1 KiB)

eth1      Link encap:Ethernet  HWaddr 08:00:27:9A:9A:45
          inet addr:192.168.64.101  Bcast:192.168.64.255  Mask:255.255.255.0
          inet6 addr: fe80::a00:27ff:fe9a:9a45/64 Scope:Link
          UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1
          RX packets:180 errors:0 dropped:0 overruns:0 frame:0
          TX packets:89 errors:0 dropped:0 overruns:0 carrier:0
          collisions:0 txqueuelen:1000
          RX bytes:18404 (17.9 KiB)  TX bytes:17483 (17.0 KiB)

lo        Link encap:Local Loopback
          inet addr:127.0.0.1  Mask:255.0.0.0
          inet6 addr: ::1/128 Scope:Host
          UP LOOPBACK RUNNING  MTU:16436  Metric:1
          RX packets:0 errors:0 dropped:0 overruns:0 frame:0
          TX packets:0 errors:0 dropped:0 overruns:0 carrier:0
          collisions:0 txqueuelen:0
          RX bytes:0 (0.0 b)  TX bytes:0 (0.0 b)
    '''

    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    result = Facter().facterInfo()

    self.assertEqual(result['ipaddress'], '10.0.2.15')
    self.assertEqual(result['netmask'], '255.255.255.0')
    self.assertEqual(result['interfaces'], 'eth0,eth1,lo')

if __name__ == "__main__":
  unittest.main()

