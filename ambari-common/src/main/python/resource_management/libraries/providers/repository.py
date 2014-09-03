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

import os
import filecmp
import tempfile
from ambari_commons import OSCheck
from resource_management import *

class RhelSuseRepositoryProvider(Provider):
  def action_create(self):
    with Environment.get_instance_copy() as env:
      repo_file_name = self.resource.repo_file_name
      repo_dir = repos_dirs[env.system.os_family]
      repo_template = self.resource.repo_template
      File(format("{repo_dir}/{repo_file_name}.repo"),
        content = Template(repo_template, repo_id=self.resource.repo_id, repo_file_name=self.resource.repo_file_name, base_url=self.resource.base_url, mirror_list=self.resource.mirror_list)
      )
  
  def action_remove(self):
    with Environment.get_instance_copy() as env:
      repo_file_name = self.resource.repo_file_name
      repo_dir = repos_dirs[env.system.os_family]

      File(format("{repo_dir}/{repo_file_name}.repo"),
           action = "delete")
    
  
repos_dirs = {
  'redhat': '/etc/yum.repos.d',
  'suse': '/etc/zypp/repos.d'
}


class UbuntuRepositoryProvider(Provider):
  package_type = "deb"
  repo_dir = "/etc/apt/sources.list.d"
  update_cmd = 'apt-get update -qq -o Dir::Etc::sourcelist="sources.list.d/{repo_file_name}" -o APT::Get::List-Cleanup="0"'
  missing_pkey_regex = "The following signatures couldn't be verified because the public key is not available: NO_PUBKEY (.+)"
  add_pkey_cmd = "apt-key adv --recv-keys --keyserver keyserver.ubuntu.com {pkey}"

  def action_create(self):
    with Environment.get_instance_copy() as env:
      with tempfile.NamedTemporaryFile() as tmpf:
        File(tmpf.name,
          content = Template(self.resource.repo_template,
              package_type=self.package_type, base_url=self.resource.base_url, components=' '.join(self.resource.components))
        )
        
        repo_file_name = format("{repo_file_name}.list",repo_file_name = self.resource.repo_file_name)
        repo_file_path = format("{repo_dir}/{repo_file_name}", repo_dir = self.repo_dir)
        
        if not os.path.isfile(repo_file_path) or not filecmp.cmp(tmpf.name, repo_file_path):
          File(repo_file_path,
               content = StaticFile(tmpf.name)
          )
          
          # this is time expensive
          retcode, out = checked_call(format(self.update_cmd))
          
          # add public keys for new repos
          missing_pkeys = set(re.findall(self.missing_pkey_regex, out))
          for pkey in missing_pkeys:
            Execute(format(self.add_pkey_cmd),
                    timeout = 15, # in case we are on the host w/o internet (using localrepo), we should ignore hanging
                    ignore_failures = True
            )
  
  def action_remove(self):
    with Environment.get_instance_copy() as env:
      repo_file_name = format("{repo_file_name}.list",repo_file_name = self.resource.repo_file_name)
      repo_file_path = format("{repo_dir}/{repo_file_name}", repo_dir = self.repo_dir)
      
      if os.path.isfile(repo_file_path):
        File(repo_file_path,
             action = "delete")
        
        # this is time expensive
        Execute(format(self.update_cmd))
