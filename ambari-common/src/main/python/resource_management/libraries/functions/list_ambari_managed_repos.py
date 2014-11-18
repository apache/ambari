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

__all__ = ["list_ambari_managed_repos"]
import os
import glob
from ambari_commons.os_check import OSCheck
from resource_management.core.exceptions import Fail

# TODO : get it dynamically from the server
repository_names = ["HDP", "HDP-UTILS"]


def list_ambari_managed_repos():
  """
  Lists all repositories that are present at host
  """
  if OSCheck.is_ubuntu_family():
    repo_dir = '/etc/apt/sources.list.d/'
  elif OSCheck.is_redhat_family():  # Centos/RHEL 5/6
    repo_dir = '/etc/yum.repos.d/'
  elif OSCheck.is_suse_family():
    repo_dir = '/etc/zypp/repos.d/'
  else:
    raise Fail('Can not dermine repo dir')
  repos = []
  for name in repository_names:
    # List all files that match pattern
    files = glob.glob(os.path.join(repo_dir, name) + '*')
    for f in files:
      filename = os.path.basename(f)
      # leave out extension
      reponame = os.path.splitext(filename)[0]
      repos.append(reponame)
  # get uniq strings
  seen = set()
  uniq = [s for s in repos if not (s in seen or seen.add(s))]
  return uniq
