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

import logging
from host_info import HostInfo

from unittest import TestCase
from mock.mock import patch

logger = logging.getLogger()

class TestHostInfo(TestCase):
  
  @patch("os.getloadavg")
  @patch("psutil.cpu_times")
  def testCpuTimes(self, cp_mock, avg_mock):
    
    cp = cp_mock.return_value
    cp.user = "user"
    cp.system = "system"
    cp.idle = "idle"
    cp.nice = "nice"
    cp.iowait = "iowait"
    cp.irq = "irq"
    cp.softirq = "softirq"
    avg_mock.return_value  = [13, 13, 13]
    
    hostinfo = HostInfo()
    
    cpu = hostinfo.get_cpu_times()
    
    self.assertEqual(cpu['cpu_user'], 'user')
    self.assertEqual(cpu['cpu_system'], 'system')
    self.assertEqual(cpu['cpu_idle'], 'idle')
    self.assertEqual(cpu['cpu_nice'], 'nice')
    self.assertEqual(cpu['cpu_wio'], 'iowait')
    self.assertEqual(cpu['cpu_intr'], 'irq')
    self.assertEqual(cpu['cpu_sintr'], 'softirq')
    self.assertEqual(cpu['load_one'], 13)
    self.assertEqual(cpu['load_five'], 13)
    self.assertEqual(cpu['load_fifteen'], 13)
    
  @patch("psutil.disk_usage")
  @patch("psutil.disk_partitions")
  @patch("psutil.swap_memory")
  @patch("psutil.virtual_memory")
  def testMemInfo(self, vm_mock, sw_mock, dm_mock, du_mock):
    
    vm = vm_mock.return_value
    vm.free = "free"
    vm.shared = "shared"
    vm.buffers = "buffers"
    vm.cached = "cached"
    
    sw = sw_mock.return_value
    sw.free = "free"
    
    hostinfo = HostInfo()
    
    cpu = hostinfo.get_mem_info()
    
    self.assertEqual(cpu['mem_free'], 'free')
    self.assertEqual(cpu['mem_shared'], 'shared')
    self.assertEqual(cpu['mem_buffered'], 'buffers')
    self.assertEqual(cpu['mem_cached'], 'cached')
    self.assertEqual(cpu['swap_free'], 'free')

  @patch("psutil.disk_usage")
  @patch("psutil.disk_partitions")
  def testCombinedDiskUsage(self, dp_mock, du_mock):
    
    dp_mock.__iter__.return_value = ['a', 'b', 'c']
    
    hostinfo = HostInfo()
    
    cdu = hostinfo.get_combined_disk_usage()
    self.assertEqual(cdu['disk_total'], "0.00")
    self.assertEqual(cdu['disk_used'], "0.00")
    self.assertEqual(cdu['disk_free'], "0.00")
    self.assertEqual(cdu['disk_percent'], "0.00")
