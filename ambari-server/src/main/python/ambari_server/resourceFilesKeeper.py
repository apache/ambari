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
import hashlib

import os, sys
import zipfile
import glob
import pprint
from xml.dom import minidom


class KeeperException(Exception):
  pass

class ResourceFilesKeeper():
  """
  This class incapsulates all utility methods for resource files maintenance.
  """

  HOOKS_DIR="hooks"
  PACKAGE_DIR="package"
  STACKS_DIR="stacks"
  CUSTOM_ACTIONS_DIR="custom_actions"

  # For these directories archives are created
  ARCHIVABLE_DIRS = [HOOKS_DIR, PACKAGE_DIR]

  HASH_SUM_FILE=".hash"
  ARCHIVE_NAME="archive.zip"

  PYC_EXT=".pyc"
  METAINFO_XML = "metainfo.xml"

  BUFFER = 1024 * 32

  # Change that to True to see debug output at stderr
  DEBUG=False

  def __init__(self, resources_dir, verbose=False, nozip=False):
    """
      nozip = create only hash files and skip creating zip archives
    """
    self.resources_dir = resources_dir
    self.verbose = verbose
    self.nozip = nozip


  def perform_housekeeping(self):
    """
    Performs housekeeping operations on resource files
    """
    self.update_directory_archieves()
    # probably, later we will need some additional operations


  def update_directory_archieves(self):
    """
    Please see AMBARI-4481 for more details
    """
    stacks_root = os.path.join(self.resources_dir, self.STACKS_DIR)
    self.dbg_out("Updating archives for stack dirs at {0}...".format(stacks_root))
    valid_stacks = self.list_stacks(stacks_root)
    self.dbg_out("Stacks: {0}".format(pprint.pformat(valid_stacks)))
    # Iterate over stack directories
    for stack_dir in valid_stacks:
      for root, dirs, _ in os.walk(stack_dir):
        for d in dirs:
          if d in self.ARCHIVABLE_DIRS:
            full_path = os.path.abspath(os.path.join(root, d))
            self.update_directory_archive(full_path)


    custom_actions_root = os.path.join(self.resources_dir,
                                       self.CUSTOM_ACTIONS_DIR)
    self.dbg_out("Updating archive for custom_actions dir at {0}...".format(
                                       custom_actions_root))
    self.update_directory_archive(custom_actions_root)



  def list_stacks(self, stacks_root):
    """
    Builds a list of stack directories
    """
    valid_stacks = [] # Format: <stack_dir, ignore(True|False)>
    glob_pattern = "{0}/*/*".format(stacks_root)
    try:
      stack_dirs = glob.glob(glob_pattern)
      for directory in stack_dirs:
        metainfo_file = os.path.join(directory, self.METAINFO_XML)
        if os.path.exists(metainfo_file):
          valid_stacks.append(directory)
      return valid_stacks
    except Exception, err:
      raise KeeperException("Can not list stacks: {0}".format(str(err)))


  def update_directory_archive(self, directory):
    """
    If hash sum for directory is not present or differs from saved value,
    recalculates hash sum and creates directory archive
    """
    cur_hash = self.count_hash_sum(directory)
    saved_hash = self.read_hash_sum(directory)
    if cur_hash != saved_hash:
      if not self.nozip:
        self.zip_directory(directory)
      self.write_hash_sum(directory, cur_hash)


  def count_hash_sum(self, directory):
    """
    Recursively counts hash sum of all files in directory and subdirectories.
    Files and directories are processed in alphabetical order.
    Ignores previously created directory archives and files containing
    previously calculated hashes. Compiled pyc files are also ignored
    """
    try:
      sha1 = hashlib.sha1()
      file_list = []
      for root, dirs, files in os.walk(directory):
        for f in files:
          if not self.is_ignored(f):
            full_path = os.path.abspath(os.path.join(root, f))
            file_list.append(full_path)
      file_list.sort()
      for path in file_list:
        self.dbg_out("Counting hash of {0}".format(path))
        with open(path, 'rb') as fh:
          while True:
            data = fh.read(self.BUFFER)
            if not data:
              break
            sha1.update(data)
      return sha1.hexdigest()
    except Exception, err:
      raise KeeperException("Can not calculate directory "
                            "hash: {0}".format(str(err)))


  def read_hash_sum(self, directory):
    """
    Tries to read a hash sum from previously generated file. Returns string
    containing hash or None
    """
    hash_file = os.path.join(directory, self.HASH_SUM_FILE)
    if os.path.isfile(hash_file):
      try:
        with open(hash_file) as fh:
          return fh.readline().strip()
      except Exception, err:
        raise KeeperException("Can not read file {0} : {1}".format(hash_file,
                                                                   str(err)))
    else:
      return None


  def write_hash_sum(self, directory, new_hash):
    """
    Tries to read a hash sum from previously generated file. Returns string
    containing hash or None
    """
    hash_file = os.path.join(directory, self.HASH_SUM_FILE)
    try:
      with open(hash_file, "w") as fh:
        fh.write(new_hash)
    except Exception, err:
      raise KeeperException("Can not write to file {0} : {1}".format(hash_file,
                                                                   str(err)))


  def zip_directory(self, directory):
    """
    Packs entire directory into zip file. Hash file is also packaged
    into archive
    """
    self.dbg_out("creating archive for directory {0}".format(directory))
    try:
      zf = zipfile.ZipFile(os.path.join(directory, self.ARCHIVE_NAME), "w")
      abs_src = os.path.abspath(directory)
      for root, dirs, files in os.walk(directory):
        for filename in files:
          # Avoid zipping previous archive and hash file and binary pyc files
          if not self.is_ignored(filename):
            absname = os.path.abspath(os.path.join(root, filename))
            arcname = absname[len(abs_src) + 1:]
            self.dbg_out('zipping %s as %s' % (os.path.join(root, filename),
                                        arcname))
            zf.write(absname, arcname)
      zf.close()
    except Exception, err:
      raise KeeperException("Can not create zip archive of "
                            "directory {0} : {1}".format(directory, str(err)))


  def is_ignored(self, filename):
    """
    returns True if filename is ignored when calculating hashing or archiving
    """
    return filename in [self.HASH_SUM_FILE, self.ARCHIVE_NAME] or \
           filename.endswith(self.PYC_EXT)


  def dbg_out(self, text):
    if self.DEBUG:
      sys.stderr.write("{0}\n".format(text))
    if not self.DEBUG and self.verbose:
      print text


def main(argv=None):
  """
  This method is called by maven during rpm creation.
  Params:
    1: Path to resources root directory
  """
  path = argv[1]
  resource_files_keeper = ResourceFilesKeeper(path, nozip=True)
  resource_files_keeper.perform_housekeeping()


if __name__ == '__main__':
  main(sys.argv)

