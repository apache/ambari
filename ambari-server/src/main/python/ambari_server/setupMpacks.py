#!/usr/bin/env python

'''
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
'''

import os
import shutil
import json
import ast

from ambari_commons.exceptions import FatalException
from ambari_commons.inet_utils import download_file
from ambari_commons.logging_utils import print_info_msg, print_error_msg
from ambari_commons.os_utils import copy_file
from ambari_server.serverConfiguration import get_ambari_properties, get_ambari_version, get_stack_location, \
  get_common_services_location, get_mpacks_staging_location, get_server_temp_location, get_extension_location

from resource_management.core import sudo
from resource_management.libraries.functions.tar_archive import extract_archive, get_archive_root_dir
from resource_management.libraries.functions.version import compare_versions
from ambari_server.setupActions import INSTALL_MPACK_ACTION, UPGRADE_MPACK_ACTION

MPACKS_REPLAY_LOG_FILENAME = "mpacks_replay.log"
MPACKS_CACHE_DIRNAME = "cache"
STACK_DEFINITIONS_RESOURCE_NAME = "stack-definitions"
SERVICE_DEFINITIONS_RESOURCE_NAME = "service-definitions"
MPACKS_RESOURCE_NAME = "mpacks"

class _named_dict(dict):
  """
  Allow to get dict items using attribute notation, eg dict.attr == dict['attr']
  """
  def __init__(self, _dict):

    def repl_list(_list):
      for i, e in enumerate(_list):
        if isinstance(e, list):
          _list[i] = repl_list(e)
        if isinstance(e, dict):
          _list[i] = _named_dict(e)
      return _list

    dict.__init__(self, _dict)
    for key, value in self.iteritems():
      if isinstance(value, dict):
        self[key] = _named_dict(value)
      if isinstance(value, list):
        self[key] = repl_list(value)

  def __getattr__(self, item):
    if item in self:
      return self[item]
    else:
      dict.__getattr__(self, item)

def _get_temp_dir():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1
  tmpdir = get_server_temp_location(properties)
  if not os.path.exists(tmpdir):
    sudo.makedirs(tmpdir, 0755)
  return tmpdir

def download_mpack(mpack_path):
  """
  Download management pack
  :param mpack_path: Path to management pack
  :return: Path where the management pack was downloaded
  """
  # Download management pack to a temp location
  tmpdir = _get_temp_dir()
  archive_filename = os.path.basename(mpack_path)
  tmp_archive_path = os.path.join(tmpdir, archive_filename)

  print_info_msg("Download management pack to temp location {0}".format(tmp_archive_path))
  if os.path.exists(tmp_archive_path):
    os.remove(tmp_archive_path)
  if os.path.exists(mpack_path):
    # local path
    copy_file(mpack_path, tmp_archive_path)
  else:
    # remote path
    download_file(mpack_path, tmp_archive_path)
  return tmp_archive_path

def expand_mpack(archive_path):
  """
  Expand management pack
  :param archive_path: Local path to management pack
  :return: Path where the management pack was expanded
  """
  tmpdir = _get_temp_dir()
  archive_root_dir = get_archive_root_dir(archive_path)
  if not archive_root_dir:
    print_error_msg("Malformed management pack. Root directory missing!")
    raise FatalException(-1, 'Malformed management pack. Root directory missing!')

  # Expand management pack in temp directory
  tmp_root_dir = os.path.join(tmpdir, archive_root_dir)
  print_info_msg("Expand management pack at temp location {0}".format(tmp_root_dir))
  if os.path.exists(tmp_root_dir):
    sudo.rmtree(tmp_root_dir)
  extract_archive(archive_path, tmpdir)

  if not os.path.exists(tmp_root_dir):
    print_error_msg("Malformed management pack. Failed to expand management pack!")
    raise FatalException(-1, 'Malformed management pack. Failed to expand management pack!')
  return tmp_root_dir

def read_mpack_metadata(mpack_dir):
  """
  Read management pack metadata
  :param mpack_dir: Path where the expanded management pack is location
  :return: Management pack metadata
  """
  # Read mpack metadata
  mpack_metafile = os.path.join(mpack_dir, "mpack.json")
  if not os.path.exists(mpack_metafile):
    print_error_msg("Malformed management pack. Metadata file missing!")
    return None

  mpack_metadata = _named_dict(json.load(open(mpack_metafile, "r")))
  return mpack_metadata

def get_mpack_properties():
  """
  Read ambari properties required for management packs
  :return: (stack_location, service_definitions_location, mpacks_staging_location)
  """
  # Get ambari config properties
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1
  stack_location = get_stack_location(properties)
  extension_location = get_extension_location(properties)
  service_definitions_location = get_common_services_location(properties)
  mpacks_staging_location = get_mpacks_staging_location(properties)
  ambari_version = get_ambari_version(properties)
  return stack_location, extension_location, service_definitions_location, mpacks_staging_location

def _get_mpack_name_version(mpack_path):
  """
  Get mpack name and version
  :param mpack_path: Path to mpack
  """
  if not mpack_path:
    print_error_msg("Management pack not specified!")
    raise FatalException(-1, 'Management pack not specified!')

  # Download management pack to a temp location
  tmp_archive_path = download_mpack(mpack_path)
  if not (tmp_archive_path and os.path.exists(tmp_archive_path)):
    print_error_msg("Management pack could not be downloaded!")
    raise FatalException(-1, 'Management pack could not be downloaded!')

  # Expand management pack in temp directory
  tmp_root_dir = expand_mpack(tmp_archive_path)

  # Read mpack metadata
  mpack_metadata = read_mpack_metadata(tmp_root_dir)
  if not mpack_metadata:
    raise FatalException(-1, 'Malformed management pack {0}. Metadata file missing!'.format(mpack_path))

  return (mpack_metadata.name, mpack_metadata.version)

def create_symlink(src_dir, dest_dir, file_name, force=False):
  """
  Helper function to create symbolic link (dest_dir/file_name -> src_dir/file_name)
  :param src_dir: Source directory
  :param dest_dir: Destination directory
  :param file_name: File name
  :param force: Remove existing symlink
  """
  src_path = os.path.join(src_dir, file_name)
  dest_link = os.path.join(dest_dir, file_name)
  if force and os.path.islink(dest_link):
    sudo.unlink(dest_link)
  sudo.symlink(src_path, dest_link)
  print_info_msg("Symlink: " + dest_link)

def remove_symlinks(stack_location, service_definitions_location, staged_mpack_dir):
  """
  Helper function to remove all symbolic links pointed to a management pack
  :param stack_location: Path to stacks folder
                         (/var/lib/ambari-server/resources/stacks)
  :param service_definitions_location: Path to service_definitions folder
                                      (/var/lib/ambari-server/resources/common-services)
  :param staged_mpack_dir: Path to management pack staging location
                           (/var/lib/ambari-server/resources/mpacks/mpack_name-mpack_version)
  """
  for location in [stack_location, service_definitions_location]:
    for root, dirs, files in os.walk(location):
      for name in files:
        file = os.path.join(root, name)
        if os.path.islink(file) and staged_mpack_dir in os.path.realpath(file):
          print_info_msg("Removing symlink {0}".format(file))
          sudo.unlink(file)
      for name in dirs:
        dir = os.path.join(root, name)
        if os.path.islink(dir) and staged_mpack_dir in os.path.realpath(dir):
          print_info_msg("Removing symlink {0}".format(dir))
          sudo.unlink(dir)

def purge_stacks_and_mpacks(purge_list, replay_mode=False):
  """
  Purge all stacks and management packs
  :param replay_mode: Flag to indicate if purging in replay mode
  """
  # Get ambari mpacks config properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location = get_mpack_properties()

  print_info_msg("Purging existing stack definitions and management packs")

  if not purge_list:
    print_info_msg("Nothing to purge")
    return

  # Don't delete default stack_advisor.py (stacks/stack_advisor.py)
  if STACK_DEFINITIONS_RESOURCE_NAME in purge_list and os.path.exists(stack_location):
    print_info_msg("Purging stack location: " + stack_location)
    for file in sorted(os.listdir(stack_location)):
      path = os.path.join(stack_location, file)
      if(os.path.isdir(path)):
        sudo.rmtree(path)

  if SERVICE_DEFINITIONS_RESOURCE_NAME in purge_list and os.path.exists(service_definitions_location):
    print_info_msg("Purging service definitions location: " + service_definitions_location)
    sudo.rmtree(service_definitions_location)

  # Don't purge mpacks staging directory in replay mode
  if MPACKS_RESOURCE_NAME in purge_list and not replay_mode and os.path.exists(mpacks_staging_location):
    print_info_msg("Purging mpacks staging location: " + mpacks_staging_location)
    sudo.rmtree(mpacks_staging_location)
    sudo.makedir(mpacks_staging_location, 0755)

def process_stack_definitions_artifact(artifact, artifact_source_dir, options):
  """
  Process stack-definitions artifacts
  :param artifact: Artifact metadata
  :param artifact_source_dir: Location of artifact in the management pack
  :param options: Command line options
  """
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location = get_mpack_properties()
  for file in sorted(os.listdir(artifact_source_dir)):
    if os.path.isfile(os.path.join(artifact_source_dir, file)):
      # Example: /var/lib/ambari-server/resources/stacks/stack_advisor.py
      create_symlink(artifact_source_dir, stack_location, file, options.force)
    else:
      src_stack_dir = os.path.join(artifact_source_dir, file)
      dest_stack_dir = os.path.join(stack_location, file)
      if not os.path.exists(dest_stack_dir):
        sudo.makedir(dest_stack_dir, 0755)
      for file in sorted(os.listdir(src_stack_dir)):
        if os.path.isfile(os.path.join(src_stack_dir, file)):
          create_symlink(src_stack_dir, dest_stack_dir, file, options.force)
        else:
          src_stack_version_dir = os.path.join(src_stack_dir, file)
          dest_stack_version_dir = os.path.join(dest_stack_dir, file)
          if not os.path.exists(dest_stack_version_dir):
            sudo.makedir(dest_stack_version_dir, 0755)
          for file in sorted(os.listdir(src_stack_version_dir)):
            if file == "services":
              src_stack_services_dir = os.path.join(src_stack_version_dir, file)
              dest_stack_services_dir = os.path.join(dest_stack_version_dir, file)
              if not os.path.exists(dest_stack_services_dir):
                sudo.makedir(dest_stack_services_dir, 0755)
              for file in sorted(os.listdir(src_stack_services_dir)):
                create_symlink(src_stack_services_dir, dest_stack_services_dir, file, options.force)
            else:
              create_symlink(src_stack_version_dir, dest_stack_version_dir, file, options.force)

def process_extension_definitions_artifact(artifact, artifact_source_dir, options):
  """
  Process extension-definitions artifacts
  Creates references for everything under the artifact_source_dir in the extension_location directory.
  This links all files, creates the extension name and extension version directories.
  Under the extension version directory, it creates links for all files and directories other than the
  services directory.  It creates links from the services directory to each service in the extension version.
  :param artifact: Artifact metadata
  :param artifact_source_dir: Location of artifact in the management pack
  :param options: Command line options
  """
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location = get_mpack_properties()
  if not os.path.exists(extension_location):
    sudo.makedir(extension_location, 0755)
  for file in sorted(os.listdir(artifact_source_dir)):
    if os.path.isfile(os.path.join(artifact_source_dir, file)):
      # Example: /var/lib/ambari-server/resources/extensions/README.txt
      create_symlink(artifact_source_dir, extension_location, file, options.force)
    else:
      src_extension_dir = os.path.join(artifact_source_dir, file)
      dest_extension_dir = os.path.join(extension_location, file)
      if not os.path.exists(dest_extension_dir):
        sudo.makedir(dest_extension_dir, 0755)
      for file in sorted(os.listdir(src_extension_dir)):
        create_symlink(src_extension_dir, dest_extension_dir, file, options.force)

def process_service_definitions_artifact(artifact, artifact_source_dir, options):
  """
  Process service-definitions artifact
  :param artifact: Artifact metadata
  :param artifact_source_dir: Location of artifact in the management pack
  :param options: Command line options
  """
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location = get_mpack_properties()
  for file in sorted(os.listdir(artifact_source_dir)):
    src_service_definitions_dir = os.path.join(artifact_source_dir, file)
    dest_service_definitions_dir = os.path.join(service_definitions_location, file)
    if not os.path.exists(dest_service_definitions_dir):
      sudo.makedir(dest_service_definitions_dir, 0755)
    for file in sorted(os.listdir(src_service_definitions_dir)):
      create_symlink(src_service_definitions_dir, dest_service_definitions_dir, file, options.force)

def process_stack_addon_service_definitions_artifact(artifact, artifact_source_dir, options):
  """
  Process stack addon service definitions artifact
  :param artifact: Artifact metadata
  :param artifact_source_dir: Location of artifact in the management pack
  :param options: Command line options
  """
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location = get_mpack_properties()
  service_versions_map = None
  if "service_versions_map" in artifact:
    service_versions_map = artifact.service_versions_map
  if not service_versions_map:
    print_error_msg("Must provide service versions map for stack-addon-service-definitions artifact!")
    raise FatalException(-1, 'Must provide service versions map for stack-addon-service-definition artifact!')
  for service_name in sorted(os.listdir(artifact_source_dir)):
    source_service_path = os.path.join(artifact_source_dir, service_name)
    for service_version in sorted(os.listdir(source_service_path)):
      source_service_version_path = os.path.join(source_service_path, service_version)
      for service_version_entry in service_versions_map:
        if service_name == service_version_entry.service_name and service_version == service_version_entry.service_version:
          applicable_stacks = service_version_entry.applicable_stacks
          for applicable_stack in applicable_stacks:
            stack_name = applicable_stack.stack_name
            stack_version = applicable_stack.stack_version
            dest_stack_path = os.path.join(stack_location, stack_name)
            dest_stack_version_path = os.path.join(dest_stack_path, stack_version)
            dest_stack_services_path = os.path.join(dest_stack_version_path, "services")
            dest_link = os.path.join(dest_stack_services_path, service_name)
            if os.path.exists(dest_stack_path) and os.path.exists(dest_stack_version_path):
              if not os.path.exists(dest_stack_services_path):
                sudo.makedir(dest_stack_services_path, 0755)
              if options.force and os.path.islink(dest_link):
                sudo.unlink(dest_link)
              sudo.symlink(source_service_version_path, dest_link)

def search_mpacks(mpack_name, max_mpack_version=None):
  """
  Search for all "mpack_name" management packs installed.
  If "max_mpack_version" is specified return only management packs < max_mpack_version
  :param mpack_name: Management pack name
  :param max_mpack_version: Max management pack version number
  :return: List of management pack
  """
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location = get_mpack_properties()
  results = []
  if os.path.exists(mpacks_staging_location) and os.path.isdir(mpacks_staging_location):
    staged_mpack_dirs = sorted(os.listdir(mpacks_staging_location))
    for dir in staged_mpack_dirs:
      if dir == MPACKS_CACHE_DIRNAME:
        continue
      staged_mpack_dir = os.path.join(mpacks_staging_location, dir)
      if os.path.isdir(staged_mpack_dir):
        staged_mpack_metadata = read_mpack_metadata(staged_mpack_dir)
        if not staged_mpack_metadata:
          print_error_msg("Skipping malformed management pack in directory {0}.".format(staged_mpack_dir))
          continue
        staged_mpack_name = staged_mpack_metadata.name
        staged_mpack_version = staged_mpack_metadata.version
        if mpack_name == staged_mpack_name and \
             (not max_mpack_version or compare_versions(staged_mpack_version, max_mpack_version, format=True) < 0):
          results.append((staged_mpack_name, staged_mpack_version))
  return results

def uninstall_mpacks(mpack_name, max_mpack_version=None):
  """
  Uninstall all "mpack_name" management packs.
  If "max_mpack_version" is specified uninstall only management packs < max_mpack_version
  :param mpack_name: Management pack name
  :param max_mpack_version: Max management pack version number
  """
  results = search_mpacks(mpack_name, max_mpack_version)
  for result in results:
    uninstall_mpack(result[0], result[1])

def uninstall_mpack(mpack_name, mpack_version):
  """
  Uninstall specific management pack
  :param mpack_name: Management pack name
  :param mpack_version: Management pack version
  """
  print_info_msg("Uninstalling management pack {0}-{1}".format(mpack_name, mpack_version))
  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location = get_mpack_properties()
  found = False
  if os.path.exists(mpacks_staging_location) and os.path.isdir(mpacks_staging_location):
    staged_mpack_dirs = sorted(os.listdir(mpacks_staging_location))
    for dir in staged_mpack_dirs:
      if dir == MPACKS_CACHE_DIRNAME:
        continue
      staged_mpack_dir = os.path.join(mpacks_staging_location, dir)
      if os.path.isdir(staged_mpack_dir):
        staged_mpack_metadata = read_mpack_metadata(staged_mpack_dir)
        if not staged_mpack_metadata:
          print_error_msg("Skipping malformed management pack {0}-{1}. Metadata file missing!".format(
                  staged_mpack_name, staged_mpack_version))
          continue
        staged_mpack_name = staged_mpack_metadata.name
        staged_mpack_version = staged_mpack_metadata.version
        if mpack_name == staged_mpack_name and compare_versions(staged_mpack_version, mpack_version, format=True) == 0:
          print_info_msg("Removing management pack staging location {0}".format(staged_mpack_dir))
          sudo.rmtree(staged_mpack_dir)
          remove_symlinks(stack_location, service_definitions_location, staged_mpack_dir)
          found = True
          break
  if not found:
    print_error_msg("Management pack {0}-{1} is not installed!".format(mpack_name, mpack_version))
  else:
    print_info_msg("Management pack {0}-{1} successfully uninstalled!".format(mpack_name, mpack_version))

def validate_mpack_prerequisites(mpack_metadata):
  """
  Validate management pack prerequisites
  :param mpack_name: Management pack metadata
  """
  # Get ambari config properties
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1
  stack_location = get_stack_location(properties)
  current_ambari_version = get_ambari_version(properties)
  fail = False

  mpack_prerequisites = mpack_metadata.prerequisites
  if "min_ambari_version" in mpack_prerequisites:
    min_ambari_version = mpack_prerequisites.min_ambari_version
    if(compare_versions(min_ambari_version, current_ambari_version, format=True) > 0):
      print_error_msg("Prerequisite failure! Current Ambari Version = {0}, "
                      "Min Ambari Version = {1}".format(current_ambari_version, min_ambari_version))
      fail = True
  if "max_ambari_version" in mpack_prerequisites:
    max_ambari_version = mpack_prerequisites.max_ambari_version
    if(compare_versions(max_ambari_version, current_ambari_version, format=True) < 0):
      print_error_msg("Prerequisite failure! Current Ambari Version = {0}, "
                      "Max Ambari Version = {1}".format(current_ambari_version, max_ambari_version))
  if "min_stack_versions" in mpack_prerequisites:
    min_stack_versions = mpack_prerequisites.min_stack_versions
    stack_found = False
    for min_stack_version in min_stack_versions:
      stack_name = min_stack_version.stack_name
      stack_version = min_stack_version.stack_version
      stack_dir = os.path.join(stack_location, stack_name, stack_version)
      if os.path.exists(stack_dir) and os.path.isdir(stack_dir):
        stack_found = True
        break
    if not stack_found:
      print_error_msg("Prerequisite failure! Min applicable stack not found")
      fail = True

  if fail:
    raise FatalException(-1, "Prerequisites for management pack {0}-{1} failed!".format(
            mpack_metadata.name, mpack_metadata.version))

def _install_mpack(options, replay_mode=False):
  """
  Install management pack
  :param options: Command line options
  :param replay_mode: Flag to indicate if executing command in replay mode
  """

  mpack_path = options.mpack_path
  if not mpack_path:
    print_error_msg("Management pack not specified!")
    raise FatalException(-1, 'Management pack not specified!')

  print_info_msg("Installing management pack {0}".format(mpack_path))

  # Download management pack to a temp location
  tmp_archive_path = download_mpack(mpack_path)
  if not (tmp_archive_path and os.path.exists(tmp_archive_path)):
    print_error_msg("Management pack could not be downloaded!")
    raise FatalException(-1, 'Management pack could not be downloaded!')

  # Expand management pack in temp directory
  tmp_root_dir = expand_mpack(tmp_archive_path)

  # Read mpack metadata
  mpack_metadata = read_mpack_metadata(tmp_root_dir)
  if not mpack_metadata:
    raise FatalException(-1, 'Malformed management pack {0}. Metadata file missing!'.format(mpack_path))

  # Validate management pack prerequisites
  # Skip validation in replay mode
  if not replay_mode:
    validate_mpack_prerequisites(mpack_metadata)

  # Purge previously installed stacks and management packs
  if options.purge and options.purge_list:
    purge_stacks_and_mpacks(options.purge_list.split(","), replay_mode)

  # Get ambari mpack properties
  stack_location, extension_location, service_definitions_location, mpacks_staging_location = get_mpack_properties()
  mpacks_cache_location = os.path.join(mpacks_staging_location, MPACKS_CACHE_DIRNAME)
  # Create directories
  if not os.path.exists(stack_location):
    sudo.makedir(stack_location, 0755)
  if not os.path.exists(extension_location):
    sudo.makedir(extension_location, 0755)
  if not os.path.exists(service_definitions_location):
    sudo.makedir(service_definitions_location, 0755)
  if not os.path.exists(mpacks_staging_location):
    sudo.makedir(mpacks_staging_location, 0755)
  if not os.path.exists(mpacks_cache_location):
    sudo.makedir(mpacks_cache_location, 0755)

  # Stage management pack (Stage at /var/lib/ambari-server/resources/mpacks/mpack_name-mpack_version)
  mpack_name = mpack_metadata.name
  mpack_version = mpack_metadata.version
  mpack_dirname = mpack_name + "-" + mpack_version
  mpack_staging_dir = os.path.join(mpacks_staging_location, mpack_dirname)
  mpack_archive_path = os.path.join(mpacks_cache_location, os.path.basename(tmp_archive_path))

  print_info_msg("Stage management pack {0}-{1} to staging location {2}".format(
          mpack_name, mpack_version, mpack_staging_dir))
  if os.path.exists(mpack_staging_dir):
    if options.force:
      print_info_msg("Force removing previously installed management pack from {0}".format(mpack_staging_dir))
      sudo.rmtree(mpack_staging_dir)
    else:
      error_msg = "Management pack {0}-{1} already installed!".format(mpack_name, mpack_version)
      print_error_msg(error_msg)
      raise FatalException(-1, error_msg)
  shutil.move(tmp_root_dir, mpack_staging_dir)
  shutil.move(tmp_archive_path, mpack_archive_path)

  # Process setup steps for all artifacts (stack-definitions, extension-definitions,
  # service-definitions, stack-addon-service-definitions) in the management pack
  for artifact in mpack_metadata.artifacts:
    # Artifact name (Friendly name)
    artifact_name = artifact.name
    # Artifact type (stack-definitions, extension-definitions, service-definitions, etc)
    artifact_type = artifact.type
    # Artifact directory with contents of the artifact
    artifact_source_dir = os.path.join(mpack_staging_dir, artifact.source_dir)

    print_info_msg("Processing artifact {0} of type {1} in {2}".format(
            artifact_name, artifact_type, artifact_source_dir))
    if artifact.type == "stack-definitions":
      process_stack_definitions_artifact(artifact, artifact_source_dir, options)
    elif artifact.type == "extension-definitions":
      process_extension_definitions_artifact(artifact, artifact_source_dir, options)
    elif artifact.type == "service-definitions":
      process_service_definitions_artifact(artifact, artifact_source_dir, options)
    elif artifact.type == "stack-addon-service-definitions":
      process_stack_addon_service_definitions_artifact(artifact, artifact_source_dir, options)
    else:
      print_info_msg("Unknown artifact {0} of type {1}".format(artifact_name, artifact_type))

  print_info_msg("Management pack {0}-{1} successfully installed!".format(mpack_name, mpack_version))
  return mpack_name, mpack_version, mpack_staging_dir, mpack_archive_path

def get_replay_log_file():
  """
  Helper function to get mpack replay log file path
  :return: mpack replay log file path
  """
  stack_location, extension_location, service_definitions_location, mpacks_staging_location = get_mpack_properties()
  replay_log_file = os.path.join(mpacks_staging_location, MPACKS_REPLAY_LOG_FILENAME)
  return replay_log_file

def add_replay_log(mpack_command, mpack_archive_path, purge, force, verbose):
  """
  Helper function to add mpack replay log entry
  :param mpack_command: mpack command
  :param mpack_archive_path: mpack archive path (/var/lib/ambari-server/resources/mpacks/mpack.tar.gz)
  :param purge: purge command line option
  :param force: force command line option
  :param verbose: verbose command line option
  """
  replay_log_file = get_replay_log_file()
  log = { 'mpack_command' : mpack_command, 'mpack_path' : mpack_archive_path, 'purge' : purge, 'force' : force, 'verbose' : verbose }
  with open(replay_log_file, "a") as replay_log:
    replay_log.write("{0}\n".format(log))

def install_mpack(options, replay_mode=False):
  """
  Install management pack
  :param options: Command line options
  :param replay_mode: Flag to indicate if executing command in replay mode
  """
  # Force install when replaying logs
  if replay_mode:
    options.force = True
  (mpack_name, mpack_version, mpack_staging_dir, mpack_archive_path) = _install_mpack(options, replay_mode)
  if not replay_mode:
    add_replay_log(INSTALL_MPACK_ACTION, mpack_archive_path, options.purge, options.force, options.verbose)

def upgrade_mpack(options, replay_mode=False):
  """
  Upgrade management pack
  :param options: command line options
  :param replay_mode: Flag to indicate if executing command in replay mode
  """
  mpack_path = options.mpack_path
  if options.purge:
    print_error_msg("Purge is not supported with upgrade_mpack action!")
    raise FatalException(-1, "Purge is not supported with upgrade_mpack action!")

  if not mpack_path:
    print_error_msg("Management pack not specified!")
    raise FatalException(-1, 'Management pack not specified!')

  (mpack_name, mpack_version) = _get_mpack_name_version(mpack_path)
  results = search_mpacks(mpack_name, mpack_version)
  if not results:
    print_error_msg("No management packs found that can be upgraded!")
    raise FatalException(-1, 'No management packs found that can be upgraded!')

  print_info_msg("Upgrading management pack {0}".format(mpack_path))

  # Force install new management pack version
  options.force = True
  (mpack_name, mpack_version, mpack_staging_dir, mpack_archive_path) = _install_mpack(options, replay_mode)

  # Uninstall old management packs
  uninstall_mpacks(mpack_name, mpack_version)

  print_info_msg("Management pack {0}-{1} successfully upgraded!".format(mpack_name, mpack_version))
  if not replay_mode:
    add_replay_log(UPGRADE_MPACK_ACTION, mpack_archive_path, options.purge, options.force, options.verbose)

def replay_mpack_logs():
  """
  Replay mpack logs during ambari-server upgrade
  """
  replay_log_file = get_replay_log_file()
  if os.path.exists(replay_log_file):
    with open(replay_log_file, "r") as f:
      for replay_log in f:
        replay_log = replay_log.strip()
        print_info_msg("===========================================================================================")
        print_info_msg("Executing Mpack Replay Log :")
        print_info_msg(replay_log)
        print_info_msg("===========================================================================================")
        replay_options = _named_dict(ast.literal_eval(replay_log))
        if replay_options.mpack_command == INSTALL_MPACK_ACTION:
          install_mpack(replay_options, replay_mode=True)
        elif replay_options.mpack_command == UPGRADE_MPACK_ACTION:
          upgrade_mpack(replay_options, replay_mode=True)
        else:
          error_msg = "Invalid mpack command {0} in mpack replay log {1}!".format(replay_options.mpack_command, replay_log_file)
          print_error_msg(error_msg)
          raise FatalException(-1, error_msg)
  else:
    print_info_msg("No mpack replay logs found. Skipping replaying mpack commands")



