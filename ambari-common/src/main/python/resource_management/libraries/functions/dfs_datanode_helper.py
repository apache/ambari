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

Ambari Agent

"""
__all__ = ["handle_dfs_data_dir", ]
import os

from resource_management.libraries.functions.file_system import get_mount_point_for_dir, get_and_cache_mount_points
from resource_management.core.logger import Logger

DATA_DIR_TO_MOUNT_HEADER = """
# This file keeps track of the last known mount-point for each DFS data dir.
# It is safe to delete, since it will get regenerated the next time that the DataNode starts.
# However, it is not advised to delete this file since Ambari may
# re-create a DFS data dir that used to be mounted on a drive but is now mounted on the root.
# Comments begin with a hash (#) symbol
# data_dir,mount_point
"""

def get_data_dir_to_mount_from_file(params):
  """
  :return: Returns a dictionary by parsing the data_dir_mount_file file,
  where the key is each DFS data dir, and the value is its last known mount point.
  """
  data_dir_to_mount = {}

  if params.data_dir_mount_file is not None and os.path.exists(str(params.data_dir_mount_file)):
    try:
      with open(str(params.data_dir_mount_file), "r") as f:
        for line in f:
          # Ignore comments
          if line and len(line) > 0 and line[0] == "#":
            continue
          line = line.strip()
          line_array = line.split(",")
          if line_array and len(line_array) == 2:
            data_dir_to_mount[line_array[0]] = line_array[1]
    except Exception, e:
      Logger.error("Encountered error while attempting to read DFS data dir mount mount values from file %s" %
                   str(params.data_dir_mount_file))
  return data_dir_to_mount


def handle_dfs_data_dir(func, params, update_cache=True):
  """
  This function determine which DFS data dir paths can be created.
  There are 2 uses cases:
  1. Customers that have many DFS data dirs, each one on a separate mount point that corresponds to a different drive.
  2. Developers that are using a sandbox VM and all DFS data dirs are mounted on the root.

  The goal is to avoid forcefully creating a DFS data dir when a user's drive fails. In this scenario, the
  mount point for a DFS data dir changes from something like /hadoop/hdfs/data/data1 to /
  If Ambari forcefully creates the directory when it doesn't exist and drive became unmounted, then Ambari will soon
  fill up the root drive, which is bad. Instead, we should not create the directory and let HDFS handle the failure
  based on its tolerance of missing directories.

  This function relies on the dfs.datanode.data.dir.mount.file parameter to parse a file that contains
  a mapping from a DFS data dir, and its last known mount point.
  After determining which DFS data dirs can be created if they don't exist, it recalculates the mount points and
  writes to the file again.
  :param func: Function that will be called if a directory will be created. This function
               will be called as func(data_dir, params)
  :param params: parameters to pass to function pointer
  :param update_cache: Bool indicating whether to update the global cache of mount points
  :return: Returns a data_dir_mount_file content
  """

  # Get the data dirs that Ambari knows about and their last known mount point
  prev_data_dir_to_mount_point = get_data_dir_to_mount_from_file(params)

  # Dictionary from data dir to the mount point that will be written to the history file.
  # If a data dir becomes unmounted, we should still keep its original value.
  # If a data dir was previously on / and is now mounted on a drive, we should store that too.
  data_dir_to_mount_point = prev_data_dir_to_mount_point.copy()

  # This should typically be False for customers, but True the first time.
  allowed_to_create_any_dir = False

  if params.data_dir_mount_file is None:
    allowed_to_create_any_dir = True
    Logger.warning("DataNode is allowed to create any data directory since dfs.datanode.data.dir.mount.file property is null.")
  else:
    if not os.path.exists(params.data_dir_mount_file):
      allowed_to_create_any_dir = True
      Logger.warning("DataNode is allowed to create any data directory since dfs.datanode.data.dir.mount.file property has file %s and it does not exist." % params.data_dir_mount_file)

  valid_data_dirs = []                # data dirs that have been normalized
  error_messages = []                 # list of error messages to report at the end
  data_dirs_unmounted = set()         # set of data dirs that have become unmounted

  for data_dir in params.dfs_data_dir.split(","):
    if data_dir is None or data_dir.strip() == "":
      continue

    data_dir = data_dir.strip()
    valid_data_dirs.append(data_dir)

    if not os.path.isdir(data_dir):
      may_create_this_dir = allowed_to_create_any_dir
      last_mount_point_for_dir = None

      # Determine if should be allowed to create the data_dir directory.
      # Either first time, became unmounted, or was just mounted on a drive
      if not may_create_this_dir:
        last_mount_point_for_dir = prev_data_dir_to_mount_point[data_dir] if data_dir in prev_data_dir_to_mount_point else None

        if last_mount_point_for_dir is None:
          # Couldn't retrieve any information about where this dir used to be mounted, so allow creating the directory to be safe.
          may_create_this_dir = True
        else:
          curr_mount_point = get_mount_point_for_dir(data_dir)

          # This means that create_this_dir will stay false if the directory became unmounted.
          # In other words, allow creating if it was already on /, or it's currently not on /
          if last_mount_point_for_dir == "/" or (curr_mount_point is not None and curr_mount_point != "/"):
            may_create_this_dir = True

      if may_create_this_dir:
        Logger.info("Forcefully creating directory: {0}".format(data_dir))

        # Call the function
        func(data_dir, params)
      else:
        # Additional check that wasn't allowed to create this dir and became unmounted.
        if last_mount_point_for_dir is not None:
          data_dirs_unmounted.add(data_dir)
          msg = "Directory {0} does not exist and became unmounted from {1} .".format(data_dir, last_mount_point_for_dir)
          error_messages.append(msg)
  pass

  # This is set to false during unit tests.
  if update_cache:
    get_and_cache_mount_points(refresh=True)

  # Update all data dirs (except the unmounted ones) with their current mount points.
  for data_dir in valid_data_dirs:
    # At this point, the directory may or may not exist
    if os.path.isdir(data_dir) and data_dir not in data_dirs_unmounted:
      curr_mount_point = get_mount_point_for_dir(data_dir)
      data_dir_to_mount_point[data_dir] = curr_mount_point

  if error_messages and len(error_messages) > 0:
    header = " ERROR ".join(["*****"] * 6)
    header = "\n" + "\n".join([header, ] * 3) + "\n"
    msg = " ".join(error_messages) + \
          " Please remount the data dir(s) and run this command again. To ignore this failure and allow writing to the " \
          "root partition, either update the contents of {0}, or delete that file.".format(params.data_dir_mount_file)
    Logger.error(header + msg + header)

  data_dir_to_mount = DATA_DIR_TO_MOUNT_HEADER
  for kv in data_dir_to_mount_point.iteritems():
    data_dir_to_mount += kv[0] + "," + kv[1] + "\n"

  return data_dir_to_mount

