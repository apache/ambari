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
import json
from resource_management.libraries.resources.repository import Repository

class RepoInstaller():    
  @classmethod
  def install_repos(cls, config):
    cls._alter_repo("create", config['hostLevelParams']['repo_info'])
    
    if 'service_repo_info' in config['hostLevelParams']:
      cls._alter_repo("create", config['hostLevelParams']['service_repo_info'])
      
  @classmethod
  def remove_repos(cls, config):
    cls._alter_repo("remove", config['hostLevelParams']['repo_info'])
    
    if 'service_repo_info' in config['hostLevelParams']:
      cls._alter_repo("remove", config['hostLevelParams']['service_repo_info'])
      
  @staticmethod
  def _alter_repo(action, repo_string):
    """
    @param action: "delete" or "create"
    @param repo_string: e.g. "[{\"baseUrl\":\"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.0\",\"osType\":\"centos6\",\"repoId\":\"HDP-2.0._\",\"repoName\":\"HDP\",\"defaultBaseUrl\":\"http://public-repo-1.hortonworks.com/HDP/centos6/2.x/updates/2.0.6.0\"}]"
    """
    repo_dicts = json.loads(repo_string)
    
    if not isinstance(repo_dicts, list):
      repo_dicts = [repo_dicts]
      
    for repo in repo_dicts:   
      if not 'baseUrl' in repo:
        repo['baseUrl'] = None
      if not 'mirrorsList' in repo:
        repo['mirrorsList'] = None
      
      Repository(repo['repoId'],
                 action = action,
                 base_url = repo['baseUrl'],
                 mirror_list = repo['mirrorsList'],
                 repo_file_name = repo['repoName'])