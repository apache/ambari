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

import os
import logging
import urlparse

from resource_management.libraries.functions import file_system
from resource_management.libraries.functions import mounted_dirs_helper

RESULT_STATE_OK = 'OK'
RESULT_STATE_WARNING = 'WARNING'
RESULT_STATE_CRITICAL = 'CRITICAL'
RESULT_STATE_UNKNOWN = 'UNKNOWN'

DFS_DATA_DIR = '{{hdfs-site/dfs.datanode.data.dir}}'
DATA_STORAGE_TAGS = ['[DISK]','[SSD]','[RAM_DISK]','[ARCHIVE]']
DATA_DIR_MOUNT_FILE = "/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist"

logger = logging.getLogger()


def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (DFS_DATA_DIR, DATA_DIR_MOUNT_FILE)


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running

  DataNode directories can be of the following formats and each needs to be supported:
    /grid/dn/archive0
    [SSD]/grid/dn/archive0
    [ARCHIVE]file:///grid/dn/archive0
  """
  warnings = []
  errors = []

  if configurations is None:
    return (RESULT_STATE_UNKNOWN, ['There were no configurations supplied to the script.'])

  # Check required properties
  if DFS_DATA_DIR not in configurations:
    return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script'.format(DFS_DATA_DIR)])

  dfs_data_dir = configurations[DFS_DATA_DIR]

  if dfs_data_dir is None:
    return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script and the value is null'.format(DFS_DATA_DIR)])

  # This follows symlinks and will return False for a broken link (even in the middle of the linked list)
  data_dir_mount_file_exists = True
  if not os.path.exists(DATA_DIR_MOUNT_FILE):
    data_dir_mount_file_exists = False
    warnings.append("{0} was not found.".format(DATA_DIR_MOUNT_FILE))

  normalized_data_dirs = set()            # data dirs that have been normalized
  data_dirs_not_exist = set()        # data dirs that do not exist
  data_dirs_unknown = set()          # data dirs for which could not determine mount
  data_dirs_on_root = set()          # set of data dirs that are on root mount
  data_dirs_on_mount = set()         # set of data dirs that are mounted on a device
  data_dirs_unmounted = []           # list of data dirs that are known to have become unmounted

  # transform each data directory into something that we can use
  for data_dir in dfs_data_dir.split(","):
    if data_dir is None or data_dir.strip() == "":
      continue

    data_dir = data_dir.strip()

    # filter out data storage tags
    for tag in DATA_STORAGE_TAGS:
      if data_dir.startswith(tag):
        data_dir = data_dir.replace(tag, "")
        continue

    # parse the path in case it contains a URI scheme
    data_dir = urlparse.urlparse(data_dir).path

    normalized_data_dirs.add(data_dir)

  # Sort the data dirs, which is needed for deterministic behavior when running the unit tests.
  normalized_data_dirs = sorted(normalized_data_dirs)
  for data_dir in normalized_data_dirs:
    # This follows symlinks and will return False for a broken link (even in the middle of the linked list)
    if os.path.isdir(data_dir):
      curr_mount_point = file_system.get_mount_point_for_dir(data_dir)
      curr_mount_point = curr_mount_point.strip() if curr_mount_point else curr_mount_point

      if curr_mount_point is not None and curr_mount_point != "":
        if curr_mount_point == "/":
          data_dirs_on_root.add(data_dir)
        else:
          data_dirs_on_mount.add(data_dir)
      else:
        data_dirs_unknown.add(data_dir)
    else:
      data_dirs_not_exist.add(data_dir)

  # To keep the messages consistent for all hosts, sort the sets into lists
  normalized_data_dirs = sorted(normalized_data_dirs)
  data_dirs_not_exist = sorted(data_dirs_not_exist)
  data_dirs_unknown = sorted(data_dirs_unknown)
  data_dirs_on_root = sorted(data_dirs_on_root)

  if data_dirs_not_exist:
    errors.append("The following data dir(s) were not found: {0}\n".format("\n".join(data_dirs_not_exist)))

  if data_dirs_unknown:
    errors.append("Cannot find the mount point for the following data dir(s):\n{0}".format("\n".join(data_dirs_unknown)))

  if data_dir_mount_file_exists:
    # This dictionary contains the expected values of <data_dir, mount_point>
    # Hence, we only need to analyze the data dirs that are currently on the root partition
    # and report an error if they were expected to be on a mount.
    #
    # If one of the data dirs is not present in the file, it means that DataNode has not been restarted after
    # the configuration was changed on the server, so we cannot make any assertions about it.
    expected_data_dir_to_mount = mounted_dirs_helper.get_dir_to_mount_from_file(DATA_DIR_MOUNT_FILE)
    for data_dir in data_dirs_on_root:
      if data_dir in expected_data_dir_to_mount and expected_data_dir_to_mount[data_dir] != "/":
        data_dirs_unmounted.append(data_dir)

    if len(data_dirs_unmounted) > 0:
      errors.append("Detected data dir(s) that became unmounted and are now writing to the root partition:\n{0}".format("\n".join(data_dirs_unmounted)))
  else:
    # Couldn't make guarantees about the expected value of mount points, so rely on this strategy that is likely to work.
    # It will report false positives (aka false alarms) if the user actually intended to have
    # 1+ data dirs on a mount and 1+ data dirs on the root partition.
    if len(data_dirs_on_mount) >= 1 and len(data_dirs_on_root) >= 1:
      errors.append("Detected at least one data dir on a mount point, but these are writing to the root partition:\n{0}".format("\n".join(data_dirs_on_root)))

  # Determine the status based on warnings and errors.
  if len(errors) == 0:
    status = RESULT_STATE_OK
    messages = []

    # Check for warnings
    if len(warnings) > 0:
      status = RESULT_STATE_WARNING
      messages += warnings

    if len(normalized_data_dirs) > 0:
      messages.append("The following data dir(s) are valid:\n{0}".format("\n".join(normalized_data_dirs)))
    else:
      messages.append("There are no data directories to analyze.")

    return (status, ["\n".join(messages)])
  else:
    # Report errors
    return (RESULT_STATE_CRITICAL, ["\n".join(errors)])