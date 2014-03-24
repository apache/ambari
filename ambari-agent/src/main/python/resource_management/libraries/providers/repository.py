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
from common_functions import OSCheck
from resource_management import *

class RhelSuseRepositoryProvider(Provider):
  def action_create(self):
    with Environment.get_instance_copy() as env:
      repo_file_name = self.resource.repo_file_name
      repo_dir = repos_dirs[env.system.os_family]
      
      File(format("{repo_dir}/{repo_file_name}.repo"),
        content = InlineTemplate("""[{{repo_id}}]
name={{repo_file_name}}
{% if mirror_list %}mirrorlist={{mirror_list}}{% else %}baseurl={{base_url}}{% endif %}
path=/
enabled=1
gpgcheck=0""", repo_id=self.resource.repo_id, repo_file_name=self.resource.repo_file_name, base_url=self.resource.base_url, mirror_list=self.resource.mirror_list)
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


class DebianRepositoryProvider(Provider):
  package_type = "deb"
  repo_dir = "/etc/apt/sources.list.d"
  update_cmd = 'apt-get update -o Dir::Etc::sourcelist="sources.list.d/{repo_file_name}" -o APT::Get::List-Cleanup="0"'

  def action_create(self):
    with Environment.get_instance_copy() as env:
      with tempfile.NamedTemporaryFile() as tmpf:
        File(tmpf.name,
          content = InlineTemplate("{{package_type}} {{base_url}} {{relase_name}} {{components}}", 
              package_type=self.package_type, base_url=self.resource.base_url, relase_name=env.system.os_release_name, components=' '.join(self.resource.components))
        )
        
        repo_file_name = format("{repo_file_name}.list",repo_file_name = self.resource.repo_file_name)
        repo_file_path = format("{repo_dir}/{repo_file_name}", repo_dir = self.repo_dir)
        
        if not os.path.isfile(repo_file_path) or not filecmp.cmp(tmpf.name, repo_file_path):
          File(repo_file_path,
               content = StaticFile(tmpf.name)
          )
          
          # this is time expensive
          Execute(format(self.update_cmd))
  
  def action_remove(self):
    with Environment.get_instance_copy() as env:
      repo_file_name = format("{repo_file_name}.list",repo_file_name = self.resource.repo_file_name)
      repo_file_path = format("{repo_dir}/{repo_file_name}", repo_dir = self.repo_dir)
      
      if os.path.isfile(repo_file_path):
        File(repo_file_path,
             action = "delete")
        
        # this is time expensive
        Execute(format(self.update_cmd))