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
from ambari_agent import hostname
from ambari_agent.Hardware import Hardware
from mock.mock import patch
from ambari_agent.Facter import Facter

class TestHardware(TestCase):
  def test_build(self):
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

  @patch.object(hostname,"hostname")
  @patch.object(Facter, "getFqdn")
  def test_fqdnDomainHostname(self, facter_getFqdn_mock, hostname_mock):
    facter_getFqdn_mock.return_value = "ambari.apache.org"
    hostname_mock.return_value = 'ambari'
    result = Facter().facterInfo()

    self.assertEquals(result['hostname'], "ambari")
    self.assertEquals(result['domain'], "apache.org")
    self.assertEquals(result['fqdn'], (result['hostname'] + '.' + result['domain']))

  @patch.object(Facter, "setDataUpTimeOutput")
  def test_uptimeSecondsHoursDays(self, facter_setDataUpTimeOutput_mock):
    # 3 days + 1 hour + 13 sec
    facter_setDataUpTimeOutput_mock.return_value = "262813.00 123.45"
    result = Facter().facterInfo()

    self.assertEquals(result['uptime_seconds'], '262813')
    self.assertEquals(result['uptime_hours'], '73')
    self.assertEquals(result['uptime_days'], '3')

  @patch.object(Facter, "setMemInfoOutput")
  def test_facterMemInfoOutput(self, facter_setMemInfoOutput_mock):

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

    result = Facter().facterInfo()

    self.assertEquals(result['memorysize'], 1832392)
    self.assertEquals(result['memorytotal'], 1832392)
    self.assertEquals(result['memoryfree'], 868648)
    self.assertEquals(result['swapsize'], '2.04 GB')
    self.assertEquals(result['swapfree'], '1.52 GB')

  @patch.object(Facter, "setDataIfConfigOutput")
  def test_facterDataIfConfigOutput(self, facter_setDataIfConfigOutput_mock):

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

    result = Facter().facterInfo()

    self.assertEquals(result['ipaddress'], '10.0.2.15')
    self.assertEquals(result['netmask'], '255.255.255.0')
    self.assertEquals(result['interfaces'], 'eth0,eth1,lo')


