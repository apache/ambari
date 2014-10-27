#!/usr/bin/env python

"""
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
"""

import collections
import os
import platform

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return None
  

def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  disk_usage = None
  try:
    disk_usage = _get_disk_usage()
  except NotImplementedError, platform_error:
    return (('CRITICAL', [str(platform_error)]))
    pass
  
  disk_usage = _get_disk_usage()
  if disk_usage is None or disk_usage.total == 0:
    return (('CRITICAL', ['Unable to determine the disk usage']))
  
  result_code = 'OK'
  percent = disk_usage.used / float(disk_usage.total) * 100
  if percent > 50:
    result_code = 'WARNING'
  elif percent > 80:
    result_code = 'CRTICAL'
    
  label = 'Capacity Used: [{0:.2f}%, {1}], Capacity Total: [{2}]'.format( 
      percent, _get_formatted_size(disk_usage.used), 
      _get_formatted_size(disk_usage.total) )
  
  return ((result_code, [label]))


def _get_disk_usage(path='/'):
  """
  returns a named tuple that contains the total, used, and free disk space
  in bytes
  """
  used = 0
  total = 0
  free = 0
  
  if 'statvfs' in dir(os):
    disk_stats = os.statvfs(path)
    free = disk_stats.f_bavail * disk_stats.f_frsize
    total = disk_stats.f_blocks * disk_stats.f_frsize
    used = (disk_stats.f_blocks - disk_stats.f_bfree) * disk_stats.f_frsize
  else:
    raise NotImplementedError("{0} is not a supported platform for this alert".format(platform.platform()))
  
  DiskInfo = collections.namedtuple('DiskInfo', 'total used free')
  return DiskInfo(total=total, used=used, free=free)


def _get_formatted_size(bytes):
  """
  formats the supplied bytes 
  """  
  if bytes < 1000:
    return '%i' % bytes + ' B'
  elif 1000 <= bytes < 1000000:
    return '%.1f' % (bytes/1000.0) + ' KB'
  elif 1000000 <= bytes < 1000000000:
    return '%.1f' % (bytes / 1000000.0) + ' MB'
  elif 1000000000 <= bytes < 1000000000000:
    return '%.1f' % (bytes/1000000000.0) + ' GB'
  else:
    return '%.1f' % (bytes/1000000000000.0) + ' TB'

if __name__ == '__main__':
    print _get_disk_usage(os.getcwd())