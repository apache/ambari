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

__all__ = ["copy_to_hdfs", ]

import os
import uuid

from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.default import default
from resource_management.core.logger import Logger

STACK_VERSION_PATTERN = "{{ stack_version }}"

TARBALL_MAP = {
  "HDP": {
    "tez":       ("/usr/hdp/%s/tez/lib/tez.tar.gz" % STACK_VERSION_PATTERN,
                  "/hdp/apps/%s/tez/tez.tar.gz"    % STACK_VERSION_PATTERN),

    "hive":      ("/usr/hdp/%s/hive/hive.tar.gz"   % STACK_VERSION_PATTERN,
                  "/hdp/apps/%s/hive/hive.tar.gz"  % STACK_VERSION_PATTERN),

    "pig":       ("/usr/hdp/%s/pig/pig.tar.gz"     % STACK_VERSION_PATTERN,
                  "/hdp/apps/%s/pig/pig.tar.gz"    % STACK_VERSION_PATTERN),

    "hadoop_streaming": ("/usr/hdp/%s/hadoop/hadoop-streaming.jar"     % STACK_VERSION_PATTERN,
                         "/hdp/apps/%s/mapreduce/hadoop-streaming.jar" % STACK_VERSION_PATTERN),

    "sqoop":     ("/usr/hdp/%s/sqoop/sqoop.tar.gz"  % STACK_VERSION_PATTERN,
                  "/hdp/apps/%s/sqoop/sqoop.tar.gz" % STACK_VERSION_PATTERN),

    "mapreduce": ("/usr/hdp/%s/hadoop/mapreduce.tar.gz"     % STACK_VERSION_PATTERN,
                  "/hdp/apps/%s/mapreduce/mapreduce.tar.gz" % STACK_VERSION_PATTERN)
  }
}

def copy_to_hdfs(name, user_group, owner, file_mode=0444, custom_source_file=None, custom_dest_file=None, force_execute=False):
  """
  :param name: Tarball name, e.g., tez, hive, pig, sqoop.
  :param user_group: Group to own the directory.
  :param owner: File owner
  :param file_mode: File permission
  :param custom_source_file: Override the source file path
  :param custom_dest_file: Override the destination file path
  :param force_execute: If true, will execute the HDFS commands immediately, otherwise, will defer to the calling function.
  :return: Will return True if successful, otherwise, False.
  """
  import params

  if params.stack_name is None or params.stack_name.upper() not in TARBALL_MAP:
    Logger.error("Cannot copy %s tarball to HDFS because stack %s does not support this operation." % (str(name), str(params.stack_name)))
    return -1

  if name is None or name.lower() not in TARBALL_MAP[params.stack_name.upper()]:
    Logger.warning("Cannot copy tarball to HDFS because %s is not supported in stack for this operation." % (str(name), str(params.stack_name)))
    return -1

  (source_file, dest_file) = TARBALL_MAP[params.stack_name.upper()][name.lower()]

  if custom_source_file is not None:
    source_file = custom_source_file

  if custom_dest_file is not None:
    dest_file = custom_dest_file

  upgrade_direction = default("/commandParams/upgrade_direction", None)
  is_rolling_upgrade = upgrade_direction is not None
  current_version = default("/hostLevelParams/current_version", None)
  if is_rolling_upgrade:
    # This is the version going to. In the case of a downgrade, it is the lower version.
    current_version = default("/commandParams/version", None)

  if current_version is None:
    message_suffix = " during rolling %s" % str(upgrade_direction) if is_rolling_upgrade else ""
    Logger.warning("Cannot copy %s tarball because unable to determine current version%s." % (str(name), message_suffix))
    return False

  source_file = source_file.replace(STACK_VERSION_PATTERN, current_version)
  dest_file = dest_file.replace(STACK_VERSION_PATTERN, current_version)

  if not os.path.exists(source_file):
    Logger.warning("WARNING. Cannot copy %s tarball because file does not exist: %s . It is possible that this component is not installed on this host." % (str(name), str(source_file)))
    return False

  # Because CopyFromLocal does not guarantee synchronization, it's possible for two processes to first attempt to
  # copy the file to a temporary location, then process 2 fails because the temporary file was already created by
  # process 1, so process 2 tries to clean up by deleting the temporary file, and then process 1
  # cannot finish the copy to the final destination, and both fail!
  # For this reason, the file name on the destination must be unique, and we then rename it to the intended value.
  # The rename operation is synchronized by the Namenode.

  #unique_string = str(uuid.uuid4())[:8]
  #temp_dest_file = dest_file + "." + unique_string

  # The logic above cannot be used until fast-hdfs-resource.jar supports the mv command, or it switches
  # to WebHDFS.


  # If the directory already exists, it is a NO-OP
  dest_dir = os.path.dirname(dest_file)
  params.HdfsResource(dest_dir,
                      type="directory",
                      action="create_on_execute",
                      owner=owner,
                      mode=0555
  )

  # If the file already exists, it is a NO-OP
  params.HdfsResource(dest_file,
                      type="file",
                      action="create_on_execute",
                      source=source_file,
                      group=user_group,
                      owner=owner,
                      mode=0444
  )
  Logger.info("Will attempt to copy %s tarball from %s to DFS at %s." % (name, source_file, dest_file))

  # For improved performance, force_execute should be False so that it is delayed and combined with other calls.
  # If still want to run the command now, set force_execute to True
  if force_execute:
    params.HdfsResource(None, action="execute")

  return True