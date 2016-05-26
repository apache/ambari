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
__all__ = ["handle_mounted_dirs", ]
import os
import re

from resource_management.libraries.functions.file_system import get_mount_point_for_dir, get_and_cache_mount_points
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory
from resource_management.core.exceptions import Fail
from resource_management.libraries.script.script import Script

DIR_TO_MOUNT_HEADER = """
# This file keeps track of the last known mount-point for each dir.
# It is safe to delete, since it will get regenerated the next time that the component of the service starts.
# However, it is not advised to delete this file since Ambari may
# re-create a dir that used to be mounted on a drive but is now mounted on the root.
# Comments begin with a hash (#) symbol
# dir,mount_point
"""

def get_dir_to_mount_from_file(history_filename):
  """
  :return: Returns a dictionary by parsing the dir_mount_file file,
  where the key is each dir, and the value is its last known mount point.
  """
  dir_to_mount = {}

  if history_filename is not None and os.path.exists(str(history_filename)):
    try:
      with open(str(history_filename), "r") as f:
        for line in f:
          # Ignore comments
          if line and len(line) > 0 and line[0] == "#":
            continue
          line = line.strip()
          line_array = line.split(",")
          if line_array and len(line_array) == 2:
            dir_to_mount[line_array[0]] = line_array[1]
    except Exception, e:
      Logger.error("Encountered error while attempting to read dir mount mount values from file %s" %
                   str(history_filename))
  return dir_to_mount


def handle_mounted_dirs(func, dirs_string, history_filename, update_cache=True):
  """
  This function determine which dir paths can be created.
  There are 2 uses cases:
  1. Customers that have many dirs, each one on a separate mount point that corresponds to a different drive.
  2. Developers that are using a sandbox VM and all dirs are mounted on the root.

  The goal is to avoid forcefully creating a dir when a user's drive fails. In this scenario, the
  mount point for a dir changes from something like /hadoop/hdfs/data/data1 to /
  If Ambari forcefully creates the directory when it doesn't exist and drive became unmounted, then Ambari will soon
  fill up the root drive, which is bad. Instead, we should not create the directory and let HDFS handle the failure
  based on its tolerance of missing directories.

  This function relies on the history_file parameter to parse a file that contains
  a mapping from a dir, and its last known mount point.
  After determining which dirs can be created if they don't exist, it recalculates the mount points and
  writes to the file again.
  :param func: Function that will be called if a directory will be created. This function
               will be called as func(dir)
  :param update_cache: Bool indicating whether to update the global cache of mount points
  :return: Returns a history_filename content
  """
  
  Directory(os.path.dirname(history_filename),
              create_parents = True,
              mode=0755,
   )

  # Get the dirs that Ambari knows about and their last known mount point
  prev_dir_to_mount_point = get_dir_to_mount_from_file(history_filename)

  # Dictionary from dir to the mount point that will be written to the history file.
  # If a dir becomes unmounted, we should still keep its original value.
  # If a dir was previously on / and is now mounted on a drive, we should store that too.
  dir_to_mount_point = prev_dir_to_mount_point.copy()

  # This should typically be False for customers, but True the first time.
  allowed_to_create_any_dir = False

  if history_filename is None:
    allowed_to_create_any_dir = True
    Logger.warning("handle_mounted_dirs is allowed to create any directory since history_file.file property is null.")
  else:
    if not os.path.exists(history_filename):
      allowed_to_create_any_dir = True
      Logger.warning("handle_mounted_dirs is allowed to create any directory since history_file property has file %s and it does not exist." % history_filename)

  valid_dirs = []                # dirs that have been normalized
  error_messages = []                 # list of error messages to report at the end
  dirs_unmounted = set()         # set of dirs that have become unmounted
  valid_existing_dirs = []

  dirs_string = ",".join([re.sub(r'^\[.+\]', '', dfs_dir.strip()) for dfs_dir in dirs_string.split(",")])
  for dir in dirs_string.split(","):
    if dir is None or dir.strip() == "":
      continue

    dir = dir.strip()
    valid_dirs.append(dir)
    
    if os.path.isdir(dir):
      valid_existing_dirs.append(dir)

  used_mounts = set([get_mount_point_for_dir(dir) for dir in valid_existing_dirs])
  
  for dir in valid_dirs:
    if not dir in valid_existing_dirs:
      may_create_this_dir = allowed_to_create_any_dir
      last_mount_point_for_dir = None
      
      curr_mount_point = get_mount_point_for_dir(dir)

      # This means that create_this_dir will stay false if the directory became unmounted.
      # In other words, allow creating if it was already on /, or it's currently not on /
      is_non_root_dir = (curr_mount_point is not None and curr_mount_point != "/")

      # Determine if should be allowed to create the dir directory.
      # Either first time, became unmounted, or was just mounted on a drive
      if not may_create_this_dir:
        last_mount_point_for_dir = prev_dir_to_mount_point[dir] if dir in prev_dir_to_mount_point else None

        if last_mount_point_for_dir is None:
          may_create_this_dir = (is_non_root_dir or Script.get_config()['configurations']['cluster-env']['create_dirs_on_root'])
        else:
          may_create_this_dir = (last_mount_point_for_dir == "/" or is_non_root_dir)

      if may_create_this_dir and Script.get_config()['configurations']['cluster-env']['ignore_bad_mounts']:
        Logger.warning("Not creating {0} as cluster-env/ignore_bad_mounts is enabled.".format(dir))
        may_create_this_dir = False
        
      if may_create_this_dir and curr_mount_point in used_mounts:
        message = "Trying to create another directory on the following mount: " + curr_mount_point
        if Script.get_config()['configurations']['cluster-env']['one_dir_per_partition']:
          raise Fail(message + " . Please turn off cluster-env/one_dir_per_partition or handle the situation manually.")
        else:
          Logger.warning(message)
          
      if may_create_this_dir:
        Logger.info("Forcefully creating directory: {0}".format(dir))

        # Call the function
        func(dir)
      else:
        # Additional check that wasn't allowed to create this dir and became unmounted.
        if last_mount_point_for_dir is not None:
          dirs_unmounted.add(dir)
          msg = "Directory {0} does not exist and became unmounted from {1} .".format(dir, last_mount_point_for_dir)
          error_messages.append(msg)
  pass

  # This is set to false during unit tests.
  if update_cache:
    get_and_cache_mount_points(refresh=True)

  # Update all dirs (except the unmounted ones) with their current mount points.
  for dir in valid_dirs:
    # At this point, the directory may or may not exist
    if os.path.isdir(dir) and dir not in dirs_unmounted:
      curr_mount_point = get_mount_point_for_dir(dir)
      dir_to_mount_point[dir] = curr_mount_point
      func(dir)

  if error_messages and len(error_messages) > 0:
    header = " ERROR ".join(["*****"] * 6)
    header = "\n" + "\n".join([header, ] * 3) + "\n"
    msg = " ".join(error_messages) + \
          " Please remount the dir(s) and run this command again. To ignore this failure and allow writing to the " \
          "root partition, either update the contents of {0}, or delete that file.".format(history_filename)
    Logger.error(header + msg + header)

  dir_to_mount = DIR_TO_MOUNT_HEADER
  for kv in dir_to_mount_point.iteritems():
    dir_to_mount += kv[0] + "," + kv[1] + "\n"

  return dir_to_mount

