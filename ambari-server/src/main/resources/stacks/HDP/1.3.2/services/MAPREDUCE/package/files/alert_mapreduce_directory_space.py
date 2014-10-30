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

RESULT_CODE_OK = 'OK'
RESULT_CODE_CRITICAL = 'CRITICAL'
RESULT_CODE_UNKNOWN = 'UNKNOWN'

MAPREDUCE_LOCAL_DIR_KEY = '{{mapred-site/mapred.local.dir}}'

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (MAPREDUCE_LOCAL_DIR_KEY,)
  

def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """
  if parameters is None:
    return (('UNKNOWN', ['There were no parameters supplied to the script.']))

  mapreduce_local_directories = None
  if MAPREDUCE_LOCAL_DIR_KEY in parameters:
    mapreduce_local_directories = parameters[MAPREDUCE_LOCAL_DIR_KEY]

  if MAPREDUCE_LOCAL_DIR_KEY is None:
    return (('UNKNOWN', ['The MapReduce Local Directory is required.']))

  directory_list = mapreduce_local_directories.split(",")
  for directory in directory_list:
    disk_usage = None
    try:
      disk_usage = _get_disk_usage(directory)
    except NotImplementedError, platform_error:
      return (RESULT_CODE_UNKNOWN, [str(platform_error)])

    if disk_usage is None or disk_usage.total == 0:
      return (RESULT_CODE_UNKNOWN, ['Unable to determine the disk usage.'])

    percent = disk_usage.used / float(disk_usage.total) * 100

    if percent > 85:
      message = 'The disk usage of {0} is {1:d}%'.format(directory,percent)
      return (RESULT_CODE_CRITICAL, [message])

  return (RESULT_CODE_OK, ["All MapReduce local directories have sufficient space."])


def _get_disk_usage(path):
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
