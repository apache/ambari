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
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst

DiskInfo = collections.namedtuple('DiskInfo', 'total used free')
MIN_FREE_SPACE = 5000000000L   # 5GB

# the location where HDP installs components when using HDP 2.2+
HDP_HOME_DIR = "/usr/hdp"

# the location where HDP installs components when using HDP 2.0 to 2.1
HDP_HOME_LEGACY_DIR = "/usr/lib"

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return None

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def execute(parameters=None, host_name=None):
  """
  Performs advanced disk checks under Linux
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  # Check usage of root partition
  try:
    disk_usage = _get_disk_usage()
    result_code, label = _get_warnings_for_partition(disk_usage)
  except NotImplementedError, platform_error:
    return 'CRITICAL', [str(platform_error)]

  # determine the location of HDP home; if it exists then we should also
  # check it in addition to /
  hdp_home = None
  if os.path.isdir(HDP_HOME_DIR):
    hdp_home = HDP_HOME_DIR
  elif os.path.isdir(HDP_HOME_LEGACY_DIR):
    hdp_home = HDP_HOME_LEGACY_DIR

  if result_code == 'OK' and hdp_home:
    # Root partition seems to be OK, let's check HDP home
    try:
      disk_usage = _get_disk_usage(hdp_home)
      result_code_usr_hdp, label_usr_hdp = _get_warnings_for_partition(disk_usage)
      if result_code_usr_hdp != 'OK':
        label = "{0}. Insufficient space at {1}: {2}".format(label, hdp_home, label_usr_hdp)
        result_code = 'WARNING'
    except NotImplementedError, platform_error:
      return 'CRITICAL', [str(platform_error)]
    pass

  return result_code, [label]


def _get_warnings_for_partition(disk_usage):
  if disk_usage is None or disk_usage.total == 0:
    return 'CRITICAL', ['Unable to determine the disk usage']

  result_code = 'OK'
  percent = disk_usage.used / float(disk_usage.total) * 100
  if percent > 80:
    result_code = 'CRITICAL'
  elif percent > 50:
    result_code = 'WARNING'

  label = 'Capacity Used: [{0:.2f}%, {1}], Capacity Total: [{2}]'.format(
    percent, _get_formatted_size(disk_usage.used),
    _get_formatted_size(disk_usage.total))

  if result_code == 'OK':
    # Check absolute disk space value
    if disk_usage.free < MIN_FREE_SPACE:
      result_code = 'WARNING'
      label += '. Total free space is less than {0}'.format(_get_formatted_size(MIN_FREE_SPACE))

  return result_code, label


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def execute(parameters=None, host_name=None):
  """
  Performs simplified disk checks under Windows
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  try:
    disk_usage = _get_disk_usage()
    result = _get_warnings_for_partition(disk_usage)
  except NotImplementedError, platform_error:
    result = ('CRITICAL', [str(platform_error)])
  return result


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def _get_disk_usage(path='/'):
  """
  returns a named tuple that contains the total, used, and free disk space
  in bytes. Linux implementation.
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

  return DiskInfo(total=total, used=used, free=free)


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def _get_disk_usage(path=None):
  """
  returns a named tuple that contains the total, used, and free disk space
  in bytes. Windows implementation
  """
  import string
  import ctypes

  used = 0
  total = 0
  free = 0
  drives = []
  bitmask = ctypes.windll.kernel32.GetLogicalDrives()
  for letter in string.uppercase:
    if bitmask & 1:
      drives.append(letter)
    bitmask >>= 1
  for drive in drives:
    free_bytes = ctypes.c_ulonglong(0)
    total_bytes = ctypes.c_ulonglong(0)
    ctypes.windll.kernel32.GetDiskFreeSpaceExW(ctypes.c_wchar_p(drive + ":\\"),
                                               None, ctypes.pointer(total_bytes),
                                               ctypes.pointer(free_bytes))
    total += total_bytes.value
    free += free_bytes.value
    used += total_bytes.value - free_bytes.value

  return DiskInfo(total=total, used=used, free=free)


def _get_formatted_size(bytes):
  """
  formats the supplied bytes 
  """
  if bytes < 1000:
    return '%i' % bytes + ' B'
  elif 1000 <= bytes < 1000000:
    return '%.1f' % (bytes / 1000.0) + ' KB'
  elif 1000000 <= bytes < 1000000000:
    return '%.1f' % (bytes / 1000000.0) + ' MB'
  elif 1000000000 <= bytes < 1000000000000:
    return '%.1f' % (bytes / 1000000000.0) + ' GB'
  else:
    return '%.1f' % (bytes / 1000000000000.0) + ' TB'

if __name__ == '__main__':
    print _get_disk_usage(os.getcwd())