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
import zipfile
from contextlib import closing
from resource_management.core.resources.system import Execute

def archive_dir(output_filename, input_dir):
  Execute(('tar', '-zcvf', output_filename, input_dir),
    sudo = True,
    tries = 3,
    try_sleep = 1,
  )

def archive_directory_dereference(archive, directory):
  """
  Creates an archive of the specified directory. This will ensure that
  symlinks are not included, but instead are followed for recursive inclusion.
  :param archive:   the name of the archive to create, including path
  :param directory:   the directory to include
  :return:  None
  """

  Execute(('tar', '-zcvhf', archive, directory),
    sudo = True,
    tries = 3,
    try_sleep = 1,
  )

def untar_archive(archive, directory):
  """
  :param directory:   can be a symlink and is followed
  """
  Execute(('tar','-xvf',archive,'-C',directory+"/"),
    sudo = True,
    tries = 3,
    try_sleep = 1,
  )

def extract_archive(archive, directory):
  if archive.endswith('.tar.gz') or path.endswith('.tgz'):
    mode = 'r:gz'
  elif archive.endswith('.tar.bz2') or path.endswith('.tbz'):
    mode = 'r:bz2'
  else:
    raise ValueError, "Could not extract `%s` as no appropriate extractor is found" % path
  with closing(tarfile.open(archive, mode)) as tar:
    tar.extractall(directory)

def get_archive_root_dir(archive):
  if archive.endswith('.tar.gz') or path.endswith('.tgz'):
    mode = 'r:gz'
  elif archive.endswith('.tar.bz2') or path.endswith('.tbz'):
    mode = 'r:bz2'
  else:
    raise ValueError, "Could not extract `%s` as no appropriate extractor is found" % path
  root_dir = None
  with closing(tarfile.open(archive, mode)) as tar:
    names = tar.getnames()
    if names:
      root_dir = os.path.commonprefix(names)
  return root_dir
