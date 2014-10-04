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


def _write_data_dir_to_mount_in_file(new_data_dir_to_mount_point):
  """
  :param new_data_dir_to_mount_point: Dictionary to write to the data_dir_mount_file file, where
  the key is each DFS data dir, and the value is its current mount point.
  :return: Returns True on success, False otherwise.
  """
  import params

  # Overwrite the existing file, or create it if doesn't exist
  if params.data_dir_mount_file:
    try:
      with open(str(params.data_dir_mount_file), "w") as f:
        f.write("# This file keeps track of the last known mount-point for each DFS data dir.\n")
        f.write("# It is safe to delete, since it will get regenerated the next time that the DataNode starts.\n")
        f.write("# However, it is not advised to delete this file since Ambari may \n")
        f.write("# re-create a DFS data dir that used to be mounted on a drive but is now mounted on the root.\n")
        f.write("# Comments begin with a hash (#) symbol\n")
        f.write("# data_dir,mount_point\n")
        for kv in new_data_dir_to_mount_point.iteritems():
          f.write(kv[0] + "," + kv[1] + "\n")
    except Exception, e:
      Logger.error("Encountered error while attempting to save DFS data dir mount mount values to file %s" %
                   str(params.data_dir_mount_file))
      return False
  return True


def _get_data_dir_to_mount_from_file():
  """
  :return: Returns a dictionary by parsing the data_dir_mount_file file,
  where the key is each DFS data dir, and the value is its last known mount point.
  """
  import params
  data_dir_to_mount = {}

  if params.data_dir_mount_file is not None and os.path.exists(str(params.data_dir_mount_file)):
    try:
      with open(str(params.data_dir_mount_file), "r") as f:
        for line in f:
          # Ignore comments
          if line and len(line) > 0 and line[0] == "#":
            continue
          line = line.strip().lower()
          line_array = line.split(",")
          if line_array and len(line_array) == 2:
            data_dir_to_mount[line_array[0]] = line_array[1]
    except Exception, e:
      Logger.error("Encountered error while attempting to read DFS data dir mount mount values from file %s" %
                   str(params.data_dir_mount_file))
  return data_dir_to_mount


def handle_dfs_data_dir(func, params):
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
  """
  prev_data_dir_to_mount_point = _get_data_dir_to_mount_from_file()

  allowed_to_create_any_dir = params.data_dir_mount_file is None or not os.path.exists(params.data_dir_mount_file)

  valid_data_dirs = []
  for data_dir in params.dfs_data_dir.split(","):
    if data_dir is None or data_dir.strip() == "":
      continue

    data_dir = data_dir.strip().lower()
    valid_data_dirs.append(data_dir)

    if not os.path.isdir(data_dir):
      create_this_dir = allowed_to_create_any_dir
      # Determine if should be allowed to create the data_dir directory
      if not create_this_dir:
        last_mount_point_for_dir = prev_data_dir_to_mount_point[data_dir] if data_dir in prev_data_dir_to_mount_point else None
        if last_mount_point_for_dir is None:
          # Couldn't retrieve any information about where this dir used to be mounted, so allow creating the directory
          # to be safe.
          create_this_dir = True
        else:
          curr_mount_point = get_mount_point_for_dir(data_dir)

          # This means that create_this_dir will stay false if the directory became unmounted.
          if last_mount_point_for_dir == "/" or (curr_mount_point is not None and curr_mount_point != "/"):
            create_this_dir = True

      if create_this_dir:
        Logger.info("Forcefully creating directory: %s" % str(data_dir))

        # Call the function
        func(data_dir, params)
      else:
        Logger.warning("Directory %s does not exist and became unmounted." % str(data_dir))

  # Refresh the known mount points
  get_and_cache_mount_points(refresh=True)

  new_data_dir_to_mount_point = {}
  for data_dir in valid_data_dirs:
    # At this point, the directory may or may not exist
    if os.path.isdir(data_dir):
      curr_mount_point = get_mount_point_for_dir(data_dir)
      new_data_dir_to_mount_point[data_dir] = curr_mount_point

  # Save back to the file
  _write_data_dir_to_mount_in_file(new_data_dir_to_mount_point)