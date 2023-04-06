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

import collections
import logging
import platform
from host_info import HostInfo
from mock.mock import patch, MagicMock
from unittest import TestCase

logger = logging.getLogger()

class TestHostInfo(TestCase):

  @patch("psutil.cpu_count")
  @patch("psutil.cpu_times_percent")
  def testCpuTimes(self, cp_mock, count_mock):
    count_mock.return_value = 1

    cp = cp_mock.return_value
    cp.user = 0.1
    cp.system = 0.1
    cp.idle = 0.7
    cp.nice = 0.1
    cp.iowait = 0
    cp.irq = 0
    cp.softirq = 0
    hostinfo = HostInfo(MagicMock())

    if platform.system() != "Windows":
      with patch("os.getloadavg") as avg_mock:
        avg_mock.return_value  = [13, 13, 13]
        cpu = hostinfo.get_cpu_times()
        self.assertEqual(cpu['load_one'], 13)
        self.assertEqual(cpu['load_five'], 13)
        self.assertEqual(cpu['load_fifteen'], 13)
    else:
      cpu = hostinfo.get_cpu_times()

    self.assertAlmostEqual(cpu['cpu_user'], 0.1)
    self.assertAlmostEqual(cpu['cpu_system'], 0.1)
    self.assertAlmostEqual(cpu['cpu_idle'], 0.7)
    self.assertAlmostEqual(cpu['cpu_nice'], 0.1)
    self.assertAlmostEqual(cpu['cpu_wio'], 0)
    self.assertAlmostEqual(cpu['cpu_intr'], 0)
    self.assertAlmostEqual(cpu['cpu_sintr'], 0)


  @patch("psutil.swap_memory")
  @patch("psutil.virtual_memory")
  def testMemInfo(self, vm_mock, sw_mock):
    
    vm = vm_mock.return_value
    vm.free = 2312043
    vm.shared = 1243
    vm.buffers = 23435
    vm.cached = 23545
    vm.available = 2312043
    
    sw = sw_mock.return_value
    sw.free = 2341234
    
    hostinfo = HostInfo(MagicMock())
    
    mem = hostinfo.get_mem_info()
    
    self.assertAlmostEqual(mem['mem_free'], 2257)
    self.assertAlmostEqual(mem['mem_shared'], 1)
    self.assertAlmostEqual(mem['mem_buffered'], 22)
    self.assertAlmostEqual(mem['mem_cached'], 22)
    self.assertAlmostEqual(mem['swap_free'], 2286)


  @patch("psutil.process_iter")
  def testProcessInfo(self, process_iter_mock):

    def side_effect_running():
      return 'running'

    class Proc:
      def status(self):
        return 'some_status'

    p1 = Proc()
    p1.status = side_effect_running
    p2 = Proc()
    p2.status = side_effect_running
    p3 = Proc()
    p4 = Proc()

    processes = [p1, p2, p3, p4]

    process_iter_mock.return_value = processes

    hostinfo = HostInfo(MagicMock())

    procs = hostinfo.get_process_info()

    self.assertEqual(procs['proc_run'], 2)
    self.assertEqual(procs['proc_total'], len(processes))

  @patch("psutil.disk_usage")
  @patch("psutil.disk_partitions")
  def testCombinedDiskUsage(self, dp_mock, du_mock):
    
    dp_mock.__iter__.return_value = ['a', 'b', 'c']
    
    hostinfo = HostInfo(MagicMock())
    
    cdu = hostinfo.get_combined_disk_usage()
    self.assertEqual(cdu['disk_total'], "0.00")
    self.assertEqual(cdu['disk_used'], "0.00")
    self.assertEqual(cdu['disk_free'], "0.00")
    self.assertEqual(cdu['disk_percent'], 0)

  @patch("psutil.disk_io_counters")
  def testDiskIOCounters(self, io_mock):

    Counters = collections.namedtuple('sdiskio', ['read_count', 'write_count',
                                                  'read_bytes', 'write_bytes',
                                                  'read_time', 'write_time'])
    io_mock.return_value = Counters(0, 1, 2, 3, 4, 5)

    c = MagicMock()
    c.get_disk_metrics_skip_pattern.return_value = None
    hostinfo = HostInfo(c)

    disk_counters = hostinfo.get_combined_disk_io_counters()

    self.assertEqual(disk_counters['read_count'], 0)
    self.assertEqual(disk_counters['write_count'], 1)
    self.assertEqual(disk_counters['read_bytes'], 2)
    self.assertEqual(disk_counters['write_bytes'], 3)
    self.assertEqual(disk_counters['read_time'], 4)
    self.assertEqual(disk_counters['write_time'], 5)

  @patch("psutil.disk_io_counters")
  def test_get_disk_io_counters_per_disk(self, io_counters_mock):
    Counters = collections.namedtuple('sdiskio', ['read_count', 'write_count',
                                                  'read_bytes', 'write_bytes',
                                                  'read_time', 'write_time',
                                                  'busy_time', 'read_merged_count',
                                                  'write_merged_count'
                                                  ])

    disk_counters1 = Counters(read_count = 0, write_count = 1,
                            read_bytes = 2, write_bytes = 3,
                            read_time = 4, write_time = 5,
                            busy_time = 6, read_merged_count = 7,
                            write_merged_count = 8
    )

    disk_counters2 = Counters(read_count = 9, write_count = 10,
                            read_bytes = 11, write_bytes = 12,
                            read_time = 13, write_time = 14,
                            busy_time = 15, read_merged_count = 16,
                            write_merged_count = 17
    )

    counters_per_disk = { 'sdb1' : disk_counters2, 'sda1' : disk_counters1 }
    io_counters_mock.return_value = counters_per_disk

    hostinfo = HostInfo(MagicMock())

    disk_counter_per_disk = hostinfo.get_disk_io_counters_per_disk()

    # Assert for sdisk_sda1
    self.assertEqual(disk_counter_per_disk['sdisk_sda1_read_count'], 0)
    self.assertEqual(disk_counter_per_disk['sdisk_sda1_write_count'], 1)
    self.assertEqual(disk_counter_per_disk['sdisk_sda1_read_bytes'], 2)
    self.assertEqual(disk_counter_per_disk['sdisk_sda1_write_bytes'], 3)
    self.assertEqual(disk_counter_per_disk['sdisk_sda1_read_time'], 4)
    self.assertEqual(disk_counter_per_disk['sdisk_sda1_write_time'], 5)
    self.assertEqual(disk_counter_per_disk['sdisk_sda1_busy_time'], 6)
    self.assertEqual(disk_counter_per_disk['sdisk_sda1_read_merged_count'], 7)
    self.assertEqual(disk_counter_per_disk['sdisk_sda1_write_merged_count'], 8)

    # Assert for sdb1

    self.assertEqual(disk_counter_per_disk['sdisk_sdb1_read_count'], 9)
    self.assertEqual(disk_counter_per_disk['sdisk_sdb1_write_count'], 10)
    self.assertEqual(disk_counter_per_disk['sdisk_sdb1_read_bytes'], 11)
    self.assertEqual(disk_counter_per_disk['sdisk_sdb1_write_bytes'], 12)
    self.assertEqual(disk_counter_per_disk['sdisk_sdb1_read_time'], 13)
    self.assertEqual(disk_counter_per_disk['sdisk_sdb1_write_time'], 14)
    self.assertEqual(disk_counter_per_disk['sdisk_sdb1_busy_time'], 15)
    self.assertEqual(disk_counter_per_disk['sdisk_sdb1_read_merged_count'], 16)
    self.assertEqual(disk_counter_per_disk['sdisk_sdb1_write_merged_count'], 17)

  @patch.object(HostInfo, "get_virtual_network_interfaces", new = MagicMock(return_value = ['etc11', 'etc2']))
  @patch("psutil.net_io_counters")
  def test_get_network_info_virtual_devices(self, net_io_counters):
    Stats = collections.namedtuple('interface', ['bytes_sent', 'bytes_recv',
                                                  'packets_sent', 'packets_recv'
                                                  ])

    net_stats = Stats(bytes_sent = 0, bytes_recv = 1,
                          packets_sent = 2, packets_recv = 3
    )

    all_net_stats = { 'etc11' : net_stats, 'etc2' : net_stats }
    net_io_counters.return_value = all_net_stats

    c = MagicMock()

    #skip virtual devices
    c.get_virtual_interfaces_skip.return_value = 'True'
    hostinfo = HostInfo(c)
    network_info = hostinfo.get_network_info()

    self.assertEqual(network_info, {})

    #do not skip virtual devices
    c.get_virtual_interfaces_skip.return_value = 'False'
    hostinfo = HostInfo(c)
    network_info = hostinfo.get_network_info()

    #len({'bytes_in': ..., 'pkts_in': ..., 'pkts_out': ..., 'bytes_out': ...}) == 4
    self.assertEqual(len(network_info), 4)

  @patch("psutil.net_io_counters")
  def test_get_network_info_skip_by_pattern(self, net_io_counters):
    Stats = collections.namedtuple('interface', ['bytes_sent', 'bytes_recv',
                                                  'packets_sent', 'packets_recv'
                                                  ])

    net_stats = Stats(bytes_sent = 0, bytes_recv = 1,
                          packets_sent = 2, packets_recv = 3
    )

    all_net_stats = { 'etc11' : net_stats, 'etc2' : net_stats }
    net_io_counters.return_value = all_net_stats

    c = MagicMock()

    #skip all by pattern
    c.get_virtual_interfaces_skip.return_value = 'False'
    c.get_network_interfaces_skip_pattern.return_value = "^etc\d*$"
    hostinfo = HostInfo(c)
    network_info = hostinfo.get_network_info()

    self.assertEqual(network_info, {})

    #skip one by pattern
    c.get_network_interfaces_skip_pattern.return_value = "^etc\d{1}$"
    hostinfo = HostInfo(c)
    network_info = hostinfo.get_network_info()

    self.assertEqual(len(network_info), 4)

    all_net_stats = { 'etc2' : net_stats }
    net_io_counters.return_value = all_net_stats
    c.get_network_interfaces_skip_pattern.return_value = "^etc\d{1}$"
    hostinfo = HostInfo(c)
    network_info = hostinfo.get_network_info()

    self.assertEqual(network_info, {})

    #skip by 'None' pattern
    c.get_network_interfaces_skip_pattern.return_value = "None"
    hostinfo = HostInfo(c)
    network_info = hostinfo.get_network_info()

    self.assertEqual(len(network_info), 4)

  @patch.object(HostInfo, "get_virtual_network_interfaces", new = MagicMock(return_value = ['etc11', 'etc2']))
  @patch("psutil.net_io_counters")
  def test_get_network_info_skip_by_pattern_and_virtual(self, net_io_counters):
    Stats = collections.namedtuple('interface', ['bytes_sent', 'bytes_recv',
                                                  'packets_sent', 'packets_recv'
                                                  ])

    net_stats = Stats(bytes_sent = 0, bytes_recv = 1,
                          packets_sent = 2, packets_recv = 3
    )

    all_net_stats = { 'etc11' : net_stats, 'etc2' : net_stats, 'etc333' : net_stats }
    net_io_counters.return_value = all_net_stats

    c = MagicMock()

    #skip only one by pattern and other as virtual
    c.get_virtual_interfaces_skip.return_value = 'True'
    c.get_network_interfaces_skip_pattern.return_value = "^etc\d{3}$"
    hostinfo = HostInfo(c)
    network_info = hostinfo.get_network_info()

    self.assertEqual(network_info, {})

  @patch("os.path.isdir")
  @patch("os.listdir")
  @patch("os.readlink")
  @patch("os.path.islink")
  def test_get_virtual_network_interfaces(self, islink, readlink, listdir, isdir):
    hostinfo = HostInfo(MagicMock())

    #virtual net device is present
    isdir.return_value = True
    listdir.return_value = ['virtual_net_dev']
    readlink.return_value = "../..devices/virtual/int6"
    islink.return_value = True

    virtual_net_devices = hostinfo.get_virtual_network_interfaces()
    self.assertEqual(virtual_net_devices, ['virtual_net_dev'])

    #virtual net device is not present
    isdir.return_value = True
    listdir.return_value = ['virtual_net_dev']
    readlink.return_value = "../..devices/pp01.000/virtio1/int6"
    islink.return_value = True

    virtual_net_devices = hostinfo.get_virtual_network_interfaces()
    self.assertEqual(virtual_net_devices, [])

    #symlinks not present
    isdir.return_value = True
    listdir.return_value = ['virtual_net_dev']
    readlink.return_value = "../..devices/virtual/int6"
    islink.return_value = False

    virtual_net_devices = hostinfo.get_virtual_network_interfaces()
    self.assertEqual(virtual_net_devices, [])

    #sysfs not available
    isdir.return_value = False
    virtual_net_devices = hostinfo.get_virtual_network_interfaces()

    self.assertEqual(virtual_net_devices, [])



