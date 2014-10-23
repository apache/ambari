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
import glob
import re

from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.resources.copy_from_local import CopyFromLocal
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management.core.resources.system import Execute
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger

"""
This file provides helper methods needed for the versioning of RPMs. Specifically, it does dynamic variable
interpretation to replace strings like {{ hdp_stack_version }} and {{ component_version }} where the value of the
variables cannot be determined ahead of time, but rather, depends on what files are found.

It assumes that {{ hdp_stack_version }} is constructed as ${major.minor.patch.rev}-${build_number}
E.g., 998.2.2.1.0-998
Please note that "-${build_number}" is optional.
Whereas {{ component_version }} is up to the Component to define, may be 3.0.1 or 301.
"""

# These values must be the suffix of the properties in cluster-env.xml
TAR_SOURCE_SUFFIX = "_tar_source"
TAR_DESTINATION_FOLDER_SUFFIX = "_tar_destination_folder"


def _get_tar_source_and_dest_folder(tarball_prefix):
  """
  :param tarball_prefix: Prefix of the tarball must be one of tez, hive, mr, pig
  :return: Returns a tuple of (x, y) after verifying the properties
  """
  component_tar_source_file = default("/configurations/cluster-env/%s%s" % (tarball_prefix.lower(), TAR_SOURCE_SUFFIX), None)
  # E.g., /usr/hdp/current/hadoop-client/tez-{{ component_version }}.{{ hdp_stack_version }}.tar.gz

  component_tar_destination_folder = default("/configurations/cluster-env/%s%s" % (tarball_prefix.lower(), TAR_DESTINATION_FOLDER_SUFFIX), None)
  # E.g., hdfs:///hdp/apps/{{ hdp_stack_version }}/mapreduce/

  if not component_tar_source_file or not component_tar_destination_folder:
    Logger.warning("Did not find %s tar source file and destination folder properties in cluster-env.xml" %
                   tarball_prefix)
    return None, None

  if component_tar_source_file.find("/") == -1:
    Logger.warning("The tar file path %s is not valid" % str(component_tar_source_file))
    return None, None

  if not component_tar_destination_folder.endswith("/"):
    component_tar_destination_folder = component_tar_destination_folder + "/"

  if not component_tar_destination_folder.startswith("hdfs://"):
    return None, None

  return component_tar_source_file, component_tar_destination_folder


def _create_regex_pattern(file_path, hdp_stack_version):
  """
  :param file_path: Input file path
  :param hdp_stack_version: Stack version, such as 2.2.0.0
  :return: Returns an expression that uses file system regex that can be used with ls and hadoop fs -ls
  """
  # Perform the variable interpretation
  file_path_pattern = file_path
  if "{{ component_version }}" in file_path_pattern:
    file_path_pattern = file_path_pattern.replace("{{ component_version }}", "*")

  # IMPORTANT, the build version was used in HDP 2.2, but may not be needed in future versions.
  if "{{ hdp_stack_version }}" in file_path_pattern:
    file_path_pattern = file_path_pattern.replace("{{ hdp_stack_version }}", hdp_stack_version + "*")   # the trailing "*" is the optional build number
  return file_path_pattern


def _populate_source_and_dests(tarball_prefix, source_file_pattern, component_tar_destination_folder, hdp_stack_version):
  """
  :param tarball_prefix: Prefix of the tarball must be one of tez, hive, mr, pig
  :param source_file_pattern: Regex pattern of the source file from the local file system
  :param component_tar_destination_folder: Destination folder to copy the file to in HDFS
  :param hdp_stack_version: Stack version number without the build version. E.g., 2.2.0.0
  :return: Returns a list of tuples (x, y), where x is the source file in the local file system,
  and y is the destination file path in HDFS
  """
  source_and_dest_pairs = []

  for file in glob.glob(source_file_pattern):
    file_base_name = os.path.basename(file)
    component_version = None
    hdp_build_version = None

    # Attempt to retrieve the hdp_build_version and component_version.
    # In case the build number (which is optional) has dots, attempt to match as many as possible.
    pattern = "%s-(.*)\\.%s-?([0-9\\.]*)\\..*" % (tarball_prefix, str(hdp_stack_version).replace(".", "\\."))
    m = re.search(pattern, file_base_name)
    if m and len(m.groups()) == 2:
      component_version = str(m.group(1))
      hdp_build_version = str(m.group(2))   # optional, so may be empty.

    missing_a_variable = False
    # The destination_file_path will be interpreted as well.
    destination_file_path = os.path.join(component_tar_destination_folder, file_base_name)

    if "{{ component_version }}" in destination_file_path:
      if component_version:
        destination_file_path = destination_file_path.replace("{{ component_version }}", component_version)
      else:
        missing_a_variable = True

    if "{{ hdp_stack_version }}" in destination_file_path:
      if hdp_build_version and hdp_build_version.strip() != "":
        destination_file_path = destination_file_path.replace("{{ hdp_stack_version }}", "%s-%s" %
                                                              (hdp_stack_version, hdp_build_version))
      else:
        destination_file_path = destination_file_path.replace("{{ hdp_stack_version }}", "%s" % hdp_stack_version)

    if missing_a_variable:
      print("WARNING. Could not identify Component version in file %s , "
            "so will not copy to HDFS." % str(file))
    else:
      source_and_dest_pairs.append((file, destination_file_path))
  return source_and_dest_pairs


def _copy_files(source_and_dest_pairs, file_owner, kinit_if_needed):
  """
  :param source_and_dest_pairs: List of tuples (x, y), where x is the source file in the local file system,
  and y is the destination file path in HDFS
  :param file_owner: Owner to set for the file copied to HDFS
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
                             mode=0777
        )

        CopyFromLocal(source,
                      mode=0755,
                      owner=file_owner,
                      dest_dir=destination_dir,
                      kinnit_if_needed=kinit_if_needed,
                      hdfs_user=params.hdfs_user,
                      hadoop_bin_dir=params.hadoop_bin_dir,
                      hadoop_conf_dir=params.hadoop_conf_dir
        )
      except:
        return_value = 1
  return return_value


def copy_tarballs_to_hdfs(tarball_prefix, component_user, file_owner):
  """
  :param tarball_prefix: Prefix of the tarball must be one of tez, hive, mr, pig
  :param component_user: User that will execute the Hadoop commands
  :param file_owner: Owner of the files copied to HDFS
  :return: Returns 0 on success, 1 if no files were copied, and in some cases may raise an exception.

  In order to call this function, params.py must have all of the following,
  hdp_stack_version, kinit_path_local, security_enabled, hdfs_user, hdfs_principal_name, hdfs_user_keytab,
  hadoop_bin_dir, hadoop_conf_dir, and HdfsDirectory as a partial function.
  """
  import params

  if not hasattr(params, "hdp_stack_version") or params.hdp_stack_version is None:
    Logger.warning("Could not find hdp_stack_version")
    return 1

  component_tar_source_file, component_tar_destination_folder = _get_tar_source_and_dest_folder(tarball_prefix)
  if not component_tar_source_file or not component_tar_destination_folder:
    return 1

  source_file_pattern = _create_regex_pattern(component_tar_source_file, params.hdp_stack_version)
  # This is just the last segment
  file_name_pattern = source_file_pattern.split('/')[-1:][0]
  tar_destination_folder_pattern = _create_regex_pattern(component_tar_destination_folder, params.hdp_stack_version)

  # Pattern for searching the file in HDFS. E.g. value, hdfs:///hdp/apps/2.2.0.0*/tez/tez-*.2.2.0.0*.tar.gz
  hdfs_file_pattern = os.path.join(tar_destination_folder_pattern, file_name_pattern)
  does_hdfs_file_exist_cmd = "fs -ls %s" % hdfs_file_pattern

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
    source_and_dest_pairs = _populate_source_and_dests(tarball_prefix, source_file_pattern,
                                                        component_tar_destination_folder, params.hdp_stack_version)
    return _copy_files(source_and_dest_pairs, file_owner, kinit_if_needed)
  return 1
