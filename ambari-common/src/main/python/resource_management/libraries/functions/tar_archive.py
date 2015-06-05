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
import tarfile
from contextlib import closing

def archive_dir(output_filename, input_dir):
  with closing(tarfile.open(output_filename, "w:gz")) as tar:
    try:
      tar.add(input_dir, arcname=os.path.basename("."))
    finally:
      tar.close()


def archive_directory_dereference(archive, directory):
  """
  Creates an archive of the specified directory. This will ensure that
  symlinks are not included, but instead are followed for recursive inclusion.
  :param archive:   the name of the archive to create, including path
  :param directory:   the directory to include
  :return:  None
  """
  tarball = None
  try:
    # !!! dereference must be TRUE since the conf is a symlink and we want
    # its contents instead of the actual symlink
    tarball = tarfile.open(archive, mode="w", dereference=True)

    # tar the files, chopping off everything in front of directory
    # /foo/bar/conf/a, /foo/bar/conf/b, /foo/bar/conf/dir/c
    # becomes
    # a
    # b
    # dir/c
    tarball.add(directory, arcname=os.path.relpath(directory, start=directory))
  finally:
    if tarball:
      tarball.close()