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

__all__ = ["copy_tarballs_to_hdfs", ]
import os
import re
import tempfile
import uuid
from resource_management.libraries.functions.format import format
from resource_management.libraries.resources.copy_from_local import CopyFromLocal
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management.core.resources.system import Execute
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core import shell


# For a given stack, define a component, such as tez or hive, with a 2-tuple that defines
# (a, b), where
# a = source file to use
# b = destination folder to copy file to in HDFS.
# {{ hdp_stack_version }} is dynamically interpreted based on the version
SOURCE_TO_DESTINATION = {"HDP":
  {
    "tez":
      ("/usr/hdp/{{ hdp_stack_version }}/tez/lib/tez.tar.gz",
       "/hdp/apps/{{ hdp_stack_version }}/tez/"),
    "hive":
      ("/usr/hdp/{{ hdp_stack_version }}/hive/hive.tar.gz",
       "/hdp/apps/{{ hdp_stack_version }}/hive/"),
    "pig":
      ("/usr/hdp/{{ hdp_stack_version }}/pig/pig.tar.gz",
       "/hdp/apps/{{ hdp_stack_version }}/pig/"),
    "hadoop-streaming":
      ("/usr/hdp/{{ hdp_stack_version }}/hadoop-mapreduce/hadoop-streaming.jar",
       "/hdp/apps/{{ hdp_stack_version }}/mapreduce/"),
    "sqoop":
      ("/usr/hdp/{{ hdp_stack_version }}/sqoop/sqoop.tar.gz",
       "/hdp/apps/{{ hdp_stack_version }}/sqoop/"),
    "mapreduce":
      ("/usr/hdp/{{ hdp_stack_version }}/hadoop/mapreduce.tar.gz",
       "/hdp/apps/{{ hdp_stack_version }}/mapreduce/")
  }
}

"""
This file provides helper methods needed for the versioning of RPMs. Specifically, it does dynamic variable
interpretation to replace strings like {{ hdp_stack_version }}  where the value of the
variables cannot be determined ahead of time, but rather, depends on what files are found.

It assumes that {{ hdp_stack_version }} is constructed as ${major.minor.patch.rev}-${build_number}
E.g., 998.2.2.1.0-998
Please note that "-${build_number}" is optional.
"""


def _get_tar_source_and_dest_folder(stack_name, tarball_prefix):
  """
  :param stack_name: Stack name, such as "HDP"
  :param tarball_prefix: Prefix of the tarball must be one of tez, hive, mr, pig
  :return: Returns a tuple of (source_file, destination_folder) after verifying the properties
  """
  if stack_name is None or stack_name not in SOURCE_TO_DESTINATION:
    Logger.warning("Did not find stack_name %s in dictionary." % str(stack_name))
    return None, None

  if tarball_prefix is None or tarball_prefix not in SOURCE_TO_DESTINATION[stack_name]:
    Logger.warning("Did not find tarball prefix %s in dictionary for stack %s." % (str(tarball_prefix), str(stack_name)))
    return None, None

  (source_file, destination_folder) = SOURCE_TO_DESTINATION[stack_name][tarball_prefix]

  if source_file.find("/") == -1:
    Logger.warning("The tar file path %s is not valid" % str(source_file))
    return None, None

  if not destination_folder.endswith("/"):
    destination_folder = destination_folder + "/"

  return source_file, destination_folder


def _copy_files(source_and_dest_pairs, file_owner, group_owner, kinit_if_needed):
  """
  :param source_and_dest_pairs: List of tuples (x, y), where x is the source file in the local file system,
  and y is the destination file path in HDFS
  :param file_owner: Owner to set for the file copied to HDFS (typically hdfs account)
  :param group_owner: Owning group to set for the file copied to HDFS (typically hadoop group)
  :param kinit_if_needed: kinit command if it is needed, otherwise an empty string
  :return: Returns 0 if at least one file was copied and no exceptions occurred, and 1 otherwise.

  Must kinit before calling this function.
  """
  import params

  return_value = 1
  if source_and_dest_pairs and len(source_and_dest_pairs) > 0:
    return_value = 0
    for (source, destination) in source_and_dest_pairs:
      try:
        destination_dir = os.path.dirname(destination)

        params.HdfsDirectory(destination_dir,
                             action="create",
                             owner=file_owner,
                             hdfs_user=params.hdfs_user,   # this will be the user to run the commands as
                             mode=0555
        )

        # Because CopyFromLocal does not guarantee synchronization, it's possible for two processes to first attempt to
        # copy the file to a temporary location, then process 2 fails because the temporary file was already created by
        # process 1, so process 2 tries to clean up by deleting the temporary file, and then process 1
        # cannot finish the copy to the final destination, and both fail!
        # For this reason, the file name on the destination must be unique, and we then rename it to the intended value.
        # The rename operation is synchronized by the Namenode.
        orig_dest_file_name = os.path.split(destination)[1]
        unique_string = str(uuid.uuid4())[:8]
        new_dest_file_name = orig_dest_file_name + "." + unique_string
        new_destination = os.path.join(destination_dir, new_dest_file_name)
        CopyFromLocal(source,
                      mode=0444,
                      owner=file_owner,
                      group=group_owner,
                      user=params.hdfs_user,               # this will be the user to run the commands as
                      dest_dir=destination_dir,
                      dest_file=new_dest_file_name,
                      kinnit_if_needed=kinit_if_needed,
                      hdfs_user=params.hdfs_user,
                      hadoop_bin_dir=params.hadoop_bin_dir,
                      hadoop_conf_dir=params.hadoop_conf_dir
        )

        mv_command = format("fs -mv {new_destination} {destination}")
        ExecuteHadoop(mv_command,
                      user=params.hdfs_user,
                      bin_dir=params.hadoop_bin_dir,
                      conf_dir=params.hadoop_conf_dir
        )
      except Exception, e:
        Logger.error("Failed to copy file. Source: %s, Destination: %s. Error: %s" % (source, destination, e.message))
        return_value = 1
  return return_value


def copy_tarballs_to_hdfs(tarball_prefix, hdp_select_component_name, component_user, file_owner, group_owner):
  """
  :param tarball_prefix: Prefix of the tarball must be one of tez, hive, mr, pig
  :param hdp_select_component_name: Component name to get the status to determine the version
  :param component_user: User that will execute the Hadoop commands, usually smokeuser
  :param file_owner: Owner of the files copied to HDFS (typically hdfs user)
  :param group_owner: Group owner of the files copied to HDFS (typically hadoop group)
  :return: Returns 0 on success, 1 if no files were copied, and in some cases may raise an exception.

  In order to call this function, params.py must have all of the following,
  hdp_stack_version, kinit_path_local, security_enabled, hdfs_user, hdfs_principal_name, hdfs_user_keytab,
  hadoop_bin_dir, hadoop_conf_dir, and HdfsDirectory as a partial function.
  """
  import params

  if not hasattr(params, "stack_name") or params.stack_name is None:
    Logger.warning("Could not find stack_name in params")
    return 1

  if not hasattr(params, "hdp_stack_version") or params.hdp_stack_version is None:
    Logger.warning("Could not find hdp_stack_version in params")
    return 1

  source_file, destination_folder = _get_tar_source_and_dest_folder(params.stack_name, tarball_prefix)
  if not source_file or not destination_folder:
    Logger.warning("Could not retrieve properties for tarball with prefix: %s" % str(tarball_prefix))
    return 1

  # Ubuntu returns: "stdin: is not a tty", as subprocess output.
  tmpfile = tempfile.NamedTemporaryFile()
  out = None
  with open(tmpfile.name, 'r+') as file:
    get_hdp_version_cmd = '/usr/bin/hdp-select status %s > %s' % (hdp_select_component_name, tmpfile.name)
    code, stdoutdata = shell.call(get_hdp_version_cmd)
    out = file.read()
  pass
  if code != 0 or out is None:
    Logger.warning("Could not verify HDP version by calling '%s'. Return Code: %s, Output: %s." %
                   (get_hdp_version_cmd, str(code), str(out)))
    return 1

  matches = re.findall(r"([\d\.]+\-\d+)", out)
  hdp_version = matches[0] if matches and len(matches) > 0 else None

  if not hdp_version:
    Logger.error("Could not parse HDP version from output of hdp-select: %s" % str(out))
    return 1

  source_file = source_file.replace("{{ hdp_stack_version }}", hdp_version)
  if not os.path.exists(source_file):
    Logger.warning("Could not find file: %s" % str(source_file))
    return 1

  file_name = os.path.basename(source_file)
  destination_file = os.path.join(destination_folder, file_name)
  destination_file = destination_file.replace("{{ hdp_stack_version }}", hdp_version)

  does_hdfs_file_exist_cmd = "fs -ls %s" % destination_file

  kinit_if_needed = ""
  if params.security_enabled:
    kinit_if_needed = format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name};")

  if kinit_if_needed:
    Execute(kinit_if_needed,
            user=component_user,
            path='/bin'
    )

  does_hdfs_file_exist = False
  try:
    ExecuteHadoop(does_hdfs_file_exist_cmd,
                  user=component_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir,
                  bin_dir=params.hadoop_bin_dir
    )
    does_hdfs_file_exist = True
  except Fail:
    pass

  if not does_hdfs_file_exist:
    source_and_dest_pairs = [(source_file, destination_file), ]
    return _copy_files(source_and_dest_pairs, file_owner, group_owner, kinit_if_needed)
  return 1
