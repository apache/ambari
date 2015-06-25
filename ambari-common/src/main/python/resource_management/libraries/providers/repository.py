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
from resource_management.core.resources import Execute
from resource_management.core.resources import File
from resource_management.core.providers import Provider
from resource_management.core.source import InlineTemplate
from resource_management.core.source import StaticFile
from resource_management.libraries.functions.format import format
from resource_management.core.environment import Environment
from resource_management.core.shell import checked_call
from resource_management.core import sudo
import re

REPO_TEMPLATE_FOLDER = 'data'

class RhelSuseRepositoryProvider(Provider):
  def action_create(self):
    with Environment.get_instance_copy() as env:
      repo_file_name = self.resource.repo_file_name
      repo_dir = get_repo_dir()
      new_content = InlineTemplate(self.resource.repo_template, repo_id=self.resource.repo_id, repo_file_name=self.resource.repo_file_name,
                             base_url=self.resource.base_url, mirror_list=self.resource.mirror_list)
      repo_file_path = format("{repo_dir}/{repo_file_name}.repo")
      if self.resource.append_to_file and os.path.isfile(repo_file_path):
        content = sudo.read_file(repo_file_path) + '\n' + new_content.get_content()
      else:
        content = new_content
        
      File(repo_file_path, 
           content=content
      )
  
  def action_remove(self):
    with Environment.get_instance_copy() as env:
      repo_file_name = self.resource.repo_file_name
      repo_dir = get_repo_dir()

      File(format("{repo_dir}/{repo_file_name}.repo"),
           action = "delete")
    
  
def get_repo_dir():
  if OSCheck.is_redhat_family():
    return '/etc/yum.repos.d'
  elif OSCheck.is_suse_family():
    return '/etc/zypp/repos.d'

class UbuntuRepositoryProvider(Provider):
  package_type = "deb"
  repo_dir = "/etc/apt/sources.list.d"
  update_cmd = ['apt-get', 'update', '-qq', '-o', 'Dir::Etc::sourcelist=sources.list.d/{repo_file_name}', '-o', 'Dir::Etc::sourceparts=-', '-o', 'APT::Get::List-Cleanup=0']
  missing_pkey_regex = "The following signatures couldn't be verified because the public key is not available: NO_PUBKEY ([A-Z0-9]+)"
  add_pkey_cmd = "apt-key adv --recv-keys --keyserver keyserver.ubuntu.com {pkey}"

  def action_create(self):
    with Environment.get_instance_copy() as env:
      with tempfile.NamedTemporaryFile() as tmpf:
        repo_file_name = format("{repo_file_name}.list",repo_file_name = self.resource.repo_file_name)
        repo_file_path = format("{repo_dir}/{repo_file_name}", repo_dir = self.repo_dir)

        new_content = InlineTemplate(self.resource.repo_template, package_type=self.package_type,
                                      base_url=self.resource.base_url,
                                      components=' '.join(self.resource.components)).get_content()
        old_content = ''
        if self.resource.append_to_file and os.path.isfile(repo_file_path):
            old_content = sudo.read_file(repo_file_path) + '\n'

        File(tmpf.name, content=old_content+new_content)

        if not os.path.isfile(repo_file_path) or not filecmp.cmp(tmpf.name, repo_file_path):
          File(repo_file_path,
               content = StaticFile(tmpf.name)
          )
          
          update_cmd_formatted = [format(x) for x in self.update_cmd]
          # this is time expensive
          retcode, out = checked_call(update_cmd_formatted, sudo=True)
          
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
        update_cmd_formatted = [format(x) for x in self.update_cmd]
        Execute(update_cmd_formatted)
