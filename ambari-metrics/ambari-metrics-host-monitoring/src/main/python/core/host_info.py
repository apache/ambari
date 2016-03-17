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
import psutil
import os
import platform
import time
import threading
import socket
import operator

logger = logging.getLogger()
cached_hostname = None

def bytes2human(n):
  bytes = float(n)
  gigabytes = bytes / 1073741824
  return '%.2f' % gigabytes
pass

class HostInfo():

  def __init__(self, config):
    self.__last_network_io_time = 0
    self.__last_network_data = {}
    self.__last_network_lock = threading.Lock()
    self.__last_disk_io_time = 0
    self.__last_disk_data = {}
    self.__host_static_info = self.get_host_static_info()
    self.__config = config

  def get_cpu_times(self):
    """
    Return cpu stats at current time
    """
    cpu_times = psutil.cpu_times_percent()
    cpu_count = self.__host_static_info.get('cpu_num', 1)

    result = {
      'cpu_num': int(cpu_count),
      'cpu_user': cpu_times.user if hasattr(cpu_times, 'user') else 0,
      'cpu_system': cpu_times.system if hasattr(cpu_times, 'system') else 0,
      'cpu_idle': cpu_times.idle if hasattr(cpu_times, 'idle') else 0,
      'cpu_nice': cpu_times.nice if hasattr(cpu_times, 'nice') else 0,
      'cpu_wio': cpu_times.iowait if hasattr(cpu_times, 'iowait') else 0,
      'cpu_intr': cpu_times.irq if hasattr(cpu_times, 'irq') else 0,
      'cpu_sintr': cpu_times.softirq if hasattr(cpu_times, 'softirq') else 0,
      'cpu_steal': cpu_times.steal if hasattr(cpu_times, 'steal') else 0
    }
    if platform.system() != "Windows":
      load_avg = os.getloadavg()
      result.update({
        'load_one' : load_avg[0] if len(load_avg) > 0 else '',
        'load_five' : load_avg[1] if len(load_avg) > 1 else '',
        'load_fifteen' : load_avg[2] if len(load_avg) > 2 else ''
      })
    return result

  pass

  def get_process_info(self):
    """
    Return processes statistics at current time
    """

    STATUS_RUNNING = "running"

    proc_stats = psutil.process_iter()

    proc_run = 0
    proc_total = 0
    for proc in proc_stats:
      proc_total += 1
      try:
        if STATUS_RUNNING == proc.status():
          proc_run += 1
      except (psutil.NoSuchProcess, psutil.AccessDenied) as e:
        #NOP
        pass
    pass

    return {
      'proc_run': proc_run,
      'proc_total': proc_total
    }
  pass

  def get_mem_info(self):
    """
    Return memory statistics at current time
    """

    mem_stats = psutil.virtual_memory()
    swap_stats = psutil.swap_memory()
    mem_total = self.__host_static_info.get('mem_total')
    swap_total = self.__host_static_info.get('swap_total')

    bytes2kilobytes = lambda x: x / 1024

    return {
      'mem_total': bytes2kilobytes(mem_total) if mem_total else 0,
      'mem_used': bytes2kilobytes(mem_stats.used - mem_stats.cached) if hasattr(mem_stats, 'used') and hasattr(mem_stats, 'cached') else 0, # Used memory w/o cached
      'mem_free': bytes2kilobytes(mem_stats.available) if hasattr(mem_stats, 'available') else 0, # the actual amount of available memory
      'mem_shared': bytes2kilobytes(mem_stats.shared) if hasattr(mem_stats, 'shared') else 0,
      'mem_buffered': bytes2kilobytes(mem_stats.buffers) if hasattr(mem_stats, 'buffers') else 0,
      'mem_cached': bytes2kilobytes(mem_stats.cached) if hasattr(mem_stats, 'cached') else 0,
      'swap_free': bytes2kilobytes(swap_stats.free) if hasattr(swap_stats, 'free') else 0,
      'swap_used': bytes2kilobytes(swap_stats.used) if hasattr(swap_stats, 'used') else 0,
      'swap_total': bytes2kilobytes(swap_total) if swap_total else 0,
      'swap_in': bytes2kilobytes(swap_stats.sin) if hasattr(swap_stats, 'sin') else 0,
      'swap_out': bytes2kilobytes(swap_stats.sout) if hasattr(swap_stats, 'sout') else 0,
      # todo: cannot send string
      #'part_max_used' : disk_usage.get("max_part_used")[0],
    }
  pass

  def get_network_info(self):
    """
    Return network counters
    """

    with self.__last_network_lock:
      current_time = time.time()
      delta = current_time - self.__last_network_io_time
      self.__last_network_io_time = current_time

    if delta <= 0:
      delta = float("inf")

    net_stats = psutil.net_io_counters(True)
    new_net_stats = {}
    for interface, values in net_stats.iteritems():
      if interface != 'lo':
        new_net_stats = {'bytes_out': new_net_stats.get('bytes_out', 0) + values.bytes_sent,
                         'bytes_in': new_net_stats.get('bytes_in', 0) + values.bytes_recv,
                         'pkts_out': new_net_stats.get('pkts_out', 0) + values.packets_sent,
                         'pkts_in': new_net_stats.get('pkts_in', 0) + values.packets_recv
        }

    with self.__last_network_lock:
      result = dict((k, (v - self.__last_network_data.get(k, 0)) / delta) for k, v in new_net_stats.iteritems())
      result = dict((k, 0 if v < 0 else v) for k, v in result.iteritems())
      self.__last_network_data = new_net_stats

    return result
  pass

  # Faster version
  def get_combined_disk_usage(self):
    combined_disk_total = 0
    combined_disk_used = 0
    combined_disk_free = 0
    combined_disk_percent = 0
    max_percent_usage = ('', 0)

    partition_count = 0
    for part in psutil.disk_partitions(all=False):
      if os.name == 'nt':
        if 'cdrom' in part.opts or part.fstype == '':
          # skip cd-rom drives with no disk in it; they may raise
          # ENOENT, pop-up a Windows GUI error for a non-ready
          # partition or just hang.
          continue
        pass
      pass
      usage = psutil.disk_usage(part.mountpoint)

      combined_disk_total += usage.total if hasattr(usage, 'total') else 0
      combined_disk_used += usage.used if hasattr(usage, 'used') else 0
      combined_disk_free += usage.free if hasattr(usage, 'free') else 0
      if hasattr(usage, 'percent'):
        combined_disk_percent += usage.percent
        partition_count += 1

      if hasattr(usage, 'percent') and max_percent_usage[1] < int(usage.percent):
        max_percent_usage = (part.mountpoint, usage.percent)
      pass
    pass

    if partition_count > 0:
      combined_disk_percent /= partition_count

    return { "disk_total" : bytes2human(combined_disk_total),
             "disk_used"  : bytes2human(combined_disk_used),
             "disk_free"  : bytes2human(combined_disk_free),
             "disk_percent" : combined_disk_percent
             # todo: cannot send string
             #"max_part_used" : max_percent_usage }
           }
  pass

  def get_host_static_info(self):

    boot_time = psutil.boot_time()
    cpu_count_logical = psutil.cpu_count()
    swap_stats = psutil.swap_memory()
    mem_info = psutil.virtual_memory()

    # No ability to store strings
    return {
      'cpu_num' : cpu_count_logical,
      'swap_total' : swap_stats.total,
      'boottime' : boot_time,
      # 'machine_type' : platform.processor(),
      # 'os_name' : platform.system(),
      # 'os_release' : platform.release(),
      'mem_total' : mem_info.total
    }

  def get_combined_disk_io_counters(self):
    # read_count: number of reads
    # write_count: number of writes
    # read_bytes: number of bytes read
    # write_bytes: number of bytes written
    # read_time: time spent reading from disk (in milliseconds)
    # write_time: time spent writing to disk (in milliseconds)

    current_time = time.time()
    delta = current_time - self.__last_disk_io_time
    self.__last_disk_io_time = current_time

    if delta <= 0:
      delta = float("inf")

    io_counters = psutil.disk_io_counters()

    new_disk_stats = {
      'read_count' : io_counters.read_count if hasattr(io_counters, 'read_count') else 0,
      'write_count' : io_counters.write_count if hasattr(io_counters, 'write_count') else 0,
      'read_bytes' : io_counters.read_bytes if hasattr(io_counters, 'read_bytes') else 0,
      'write_bytes' : io_counters.write_bytes if hasattr(io_counters, 'write_bytes') else 0,
      'read_time' : io_counters.read_time if hasattr(io_counters, 'read_time') else 0,
      'write_time' : io_counters.write_time if hasattr(io_counters, 'write_time') else 0
    }
    if not self.__last_disk_data:
      self.__last_disk_data = new_disk_stats
    read_bps = (new_disk_stats['read_bytes'] - self.__last_disk_data['read_bytes']) / delta
    write_bps = (new_disk_stats['write_bytes'] - self.__last_disk_data['write_bytes']) / delta
    self.__last_disk_data = new_disk_stats
    new_disk_stats['read_bps'] = read_bps
    new_disk_stats['write_bps'] = write_bps
    return new_disk_stats

  def get_disk_io_counters_per_disk(self):
    # Return a normalized disk name with the counters per disk
    disk_io_counters = psutil.disk_io_counters(True)
    per_disk_io_counters = {}

    sortByKey = lambda x: sorted(x.items(), key=operator.itemgetter(0))

    disk_counter = 0
    if disk_io_counters:
      # Sort disks lexically, best chance for similar disk topologies getting
      # aggregated correctly
      disk_io_counters = sortByKey(disk_io_counters)
      for item in disk_io_counters:
        disk_counter += 1
        disk = item[0]
        logger.debug('Adding disk counters for %s' % str(disk))
        sdiskio = item[1]
        prefix = 'disk_{0}_'.format(disk_counter)
        counter_dict = {
          prefix + 'read_count' : sdiskio.read_count if hasattr(sdiskio, 'read_count') else 0,
          prefix + 'write_count' : sdiskio.write_count if hasattr(sdiskio, 'write_count') else 0,
          prefix + 'read_bytes' : sdiskio.read_bytes if hasattr(sdiskio, 'read_bytes') else 0,
          prefix + 'write_bytes' : sdiskio.write_bytes if hasattr(sdiskio, 'write_bytes') else 0,
          prefix + 'read_time' : sdiskio.read_time if hasattr(sdiskio, 'read_time') else 0,
          prefix + 'write_time' : sdiskio.write_time if hasattr(sdiskio, 'write_time') else 0
        }
        # Optional platform specific attributes
        if hasattr(sdiskio, 'busy_time'):
          counter_dict[ prefix + 'busy_time' ] = sdiskio.busy_time
        if hasattr(sdiskio, 'read_merged_count'):
          counter_dict[ prefix + 'read_merged_count' ] = sdiskio.read_merged_count
        if hasattr(sdiskio, 'write_merged_count'):
          counter_dict[ prefix + 'write_merged_count' ] = sdiskio.write_merged_count

        per_disk_io_counters.update(counter_dict)
      pass
    pass
    # Send total disk count as a metric
    per_disk_io_counters[ 'disk_num' ] = disk_counter

    return per_disk_io_counters

  def get_hostname(self):
    global cached_hostname
    if cached_hostname is not None:
      return cached_hostname

    try:
      cached_hostname = self.__config.get_hostname_config()
      if not cached_hostname:
        cached_hostname = socket.getfqdn().lower()
    except:
      cached_hostname = socket.getfqdn().lower()
    logger.info('Cached hostname: %s' % cached_hostname)
    return cached_hostname

  def get_ip_address(self):
    return socket.gethostbyname(socket.getfqdn())
