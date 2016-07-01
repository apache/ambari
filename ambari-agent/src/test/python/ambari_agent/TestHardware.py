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
from ambari_agent import main
main.MEMORY_LEAK_DEBUG_FILEPATH = "/tmp/memory_leak_debug.out"
from unittest import TestCase
from mock.mock import patch, MagicMock
import unittest
import platform
import socket
from only_for_platform import not_for_platform, PLATFORM_WINDOWS
from ambari_agent import hostname
from ambari_agent.Hardware import Hardware
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.Facter import Facter, FacterLinux
from ambari_commons import OSCheck
from glob import glob

@not_for_platform(PLATFORM_WINDOWS)
@patch.object(platform,"linux_distribution", new = ('Suse','11','Final'))
@patch.object(socket, "getfqdn", new = MagicMock(return_value = "ambari.apache.org"))
@patch.object(socket, "gethostbyname", new = MagicMock(return_value = "192.168.1.1"))
@patch.object(FacterLinux, "setDataIfConfigShortOutput", new = MagicMock(return_value ='''Iface   MTU Met    RX-OK RX-ERR RX-DRP RX-OVR    TX-OK TX-ERR TX-DRP TX-OVR Flg
eth0   1500   0     9986      0      0      0     5490      0      0      0 BMRU
eth1   1500   0        0      0      0      0        6      0      0      0 BMRU
eth2   1500   0        0      0      0      0        6      0      0      0 BMRU
lo    16436   0        2      0      0      0        2      0      0      0 LRU'''))
class TestHardware(TestCase):
 
  @patch.object(Hardware, "osdisks", new = MagicMock(return_value=[]))
  @patch.object(Hardware, "_chk_mount", new = MagicMock(return_value=True))
  @patch.object(FacterLinux, "get_ip_address_by_ifname", new = MagicMock(return_value=None))
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

  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  @patch("subprocess.Popen")
  @patch("subprocess.Popen.communicate")
  def test_osdisks_remote(self, communicate_mock, popen_mock,
                          get_os_version_mock, get_os_type_mock):
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    Hardware.osdisks()
    popen_mock.assert_called_with(['timeout', '10', "df","-kPT"], stdout=-1)
    config = AmbariConfig()
    Hardware.osdisks(config)
    popen_mock.assert_called_with(['timeout', '10', "df","-kPT"], stdout=-1)
    config.add_section(AmbariConfig.AMBARI_PROPERTIES_CATEGORY)
    config.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_KEY, "true")
    Hardware.osdisks(config)
    popen_mock.assert_called_with(['timeout', '10', "df","-kPT"], stdout=-1)
    config.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_KEY, "false")
    Hardware.osdisks(config)
    popen_mock.assert_called_with(['timeout', '10', "df","-kPT", "-l"], stdout=-1)
    config.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY, "0")
    Hardware.osdisks(config)
    popen_mock.assert_called_with(['timeout', '10', "df","-kPT","-l"], stdout=-1)
    config.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY, "1")
    Hardware.osdisks(config)
    popen_mock.assert_called_with(["timeout","1","df","-kPT","-l"], stdout=-1)
    config.set(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY, "2")
    Hardware.osdisks(config)
    popen_mock.assert_called_with(["timeout","2","df","-kPT","-l"], stdout=-1)


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

  @patch.object(FacterLinux, "get_ip_address_by_ifname", new = MagicMock(return_value=None))
  @patch.object(hostname,"hostname")
  @patch.object(FacterLinux, "getFqdn")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_fqdnDomainHostname(self, get_os_version_mock, get_os_type_mock, facter_getFqdn_mock, hostname_mock):
    facter_getFqdn_mock.return_value = "ambari.apache.org"
    hostname_mock.return_value = 'ambari'
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    result = Facter().facterInfo()

    self.assertEquals(result['hostname'], "ambari")
    self.assertEquals(result['domain'], "apache.org")
    self.assertEquals(result['fqdn'], (result['hostname'] + '.' + result['domain']))

  @patch.object(FacterLinux, "get_ip_address_by_ifname", new = MagicMock(return_value=None))
  @patch.object(FacterLinux, "setDataUpTimeOutput")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_uptimeSecondsHoursDays(self, get_os_version_mock, get_os_type_mock, facter_setDataUpTimeOutput_mock):
    # 3 days + 1 hour + 13 sec
    facter_setDataUpTimeOutput_mock.return_value = "262813.00 123.45"
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    result = Facter().facterInfo()

    self.assertEquals(result['uptime_seconds'], '262813')
    self.assertEquals(result['uptime_hours'], '73')
    self.assertEquals(result['uptime_days'], '3')

  @patch.object(FacterLinux, "get_ip_address_by_ifname", new = MagicMock(return_value=None))
  @patch.object(FacterLinux, "setMemInfoOutput")
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

    self.assertEquals(result['memorysize'], 1832392)
    self.assertEquals(result['memorytotal'], 1832392)
    self.assertEquals(result['memoryfree'], 868648)
    self.assertEquals(result['swapsize'], '2.04 GB')
    self.assertEquals(result['swapfree'], '1.52 GB')

  @patch("fcntl.ioctl")
  @patch("socket.socket")
  @patch("struct.pack")
  @patch("socket.inet_ntoa")
  @patch.object(FacterLinux, "get_ip_address_by_ifname")
  @patch.object(Facter, "getIpAddress")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_facterDataIfConfigOutput(self, get_os_version_mock, get_os_type_mock,
                                    getIpAddress_mock, get_ip_address_by_ifname_mock, inet_ntoa_mock, struct_pack_mock,
                                    socket_socket_mock, fcntl_ioctl_mock):
    getIpAddress_mock.return_value = "10.0.2.15"
    get_ip_address_by_ifname_mock.return_value = "10.0.2.15"
    inet_ntoa_mock.return_value = "255.255.255.0"

    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    result = Facter().facterInfo()

    self.assertTrue(inet_ntoa_mock.called)
    self.assertTrue(get_ip_address_by_ifname_mock.called)
    self.assertTrue(getIpAddress_mock.called)
    self.assertEquals(result['ipaddress'], '10.0.2.15')
    self.assertEquals(result['netmask'], '255.255.255.0')
    self.assertEquals(result['interfaces'], 'eth0,eth1,eth2,lo')

  @patch("fcntl.ioctl")
  @patch("socket.socket")
  @patch("struct.pack")
  @patch("socket.inet_ntoa")
  @patch.object(FacterLinux, "get_ip_address_by_ifname")
  @patch.object(Facter, "getIpAddress")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  def test_facterDataIfConfigOutputNone(self, get_os_version_mock, get_os_type_mock,
                                    getIpAddress_mock, get_ip_address_by_ifname_mock, inet_ntoa_mock, struct_pack_mock,
                                    socket_socket_mock, fcntl_ioctl_mock):
    getIpAddress_mock.return_value = "10.0.2.15"
    get_ip_address_by_ifname_mock.return_value = ""
    inet_ntoa_mock.return_value = "255.255.255.0"

    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    result = Facter().facterInfo()

    self.assertTrue(get_ip_address_by_ifname_mock.called)
    self.assertEquals(result['netmask'], None)


  @patch.object(FacterLinux, "get_ip_address_by_ifname", new = MagicMock(return_value=None))
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_version")
  def test_facterDataOperatingsystemVsFamily(self, get_os_version_mock, get_os_family_mock, get_os_type_mock):
    get_os_type_mock.return_value = "some_type_of_os"
    get_os_version_mock.return_value = "11"
    get_os_family_mock.return_value = "redhat"

    result = Facter().facterInfo()
    self.assertEquals(result['operatingsystem'], 'some_type_of_os')
    self.assertEquals(result['osfamily'], 'redhat')

    get_os_family_mock.return_value = "ubuntu"
    result = Facter().facterInfo()
    self.assertEquals(result['operatingsystem'], 'some_type_of_os')
    self.assertEquals(result['osfamily'], 'ubuntu')

    get_os_family_mock.return_value = "suse"
    result = Facter().facterInfo()
    self.assertEquals(result['operatingsystem'], 'some_type_of_os')
    self.assertEquals(result['osfamily'], 'suse')

    get_os_family_mock.return_value = "My_new_family"
    result = Facter().facterInfo()
    self.assertEquals(result['operatingsystem'], 'some_type_of_os')
    self.assertEquals(result['osfamily'], 'My_new_family')


  @patch("os.path.exists")
  @patch("os.path.isdir")
  @patch("json.loads")
  @patch("glob.glob")
  @patch("__builtin__.open")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_version")
  @patch.object(FacterLinux, "resolve_ambari_config")
  def test_system_resource_overrides(self, resolve_ambari_config, get_os_version_mock, get_os_type_mock,
                                     open_mock, glob_mock, json_mock, isdir, exists):
    get_os_type_mock.return_value = "suse"
    get_os_version_mock.return_value = "11"
    config = MagicMock()
    config.get.return_value = '/etc/custom_resource_overrides'
    config.has_option.return_value = True
    resolve_ambari_config.return_value = config
    isdir.return_value = True
    exists.return_value = True
    open_mock.return_value.read = "1"
    file_handle = open_mock.return_value.__enter__.return_value
    file_handle.read.return_value = '1'
    glob_mock.side_effect = \
      [
        [
          "/etc/custom_resource_overrides/1.json",
          "/etc/custom_resource_overrides/2.json"
          ]
      ]
    json_data = json_mock.return_value
    json_data.items.return_value = [('key', 'value')]
    json_data.__getitem__.return_value = 'value'

    facter = Facter()
    facter.config = config
    result = facter.getSystemResourceOverrides()

    isdir.assert_called_with('/etc/custom_resource_overrides')
    exists.assert_called_with('/etc/custom_resource_overrides')
    glob_mock.assert_called_with('/etc/custom_resource_overrides/*.json')
    self.assertTrue(config.has_option.called)
    self.assertTrue(config.get.called)
    self.assertTrue(glob_mock.called)
    self.assertEquals(2, file_handle.read.call_count)
    self.assertEquals(2, open_mock.call_count)
    self.assertEquals(2, json_mock.call_count)
    self.assertEquals('value', result['key'])



if __name__ == "__main__":
  unittest.main()

