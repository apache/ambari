#!/usr/bin/env python2.6

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

import logging
import os
import json
from shell import shellRunner
from manifestGenerator import writeImports


PUPPET_EXT=".pp"

logger = logging.getLogger()

class RepoInstaller:
  def __init__(self, parsedJson, path, modulesdir, taskId):
    self.parsedJson = parsedJson
    self.path = path
    self.modulesdir = modulesdir
    self.taskId = taskId
    self.sh = shellRunner()

  def prepareReposInfo(self):
    params = {}
    self.repoInfoList = []
    if self.parsedJson.has_key('hostLevelParams'):
      params = self.parsedJson['hostLevelParams']
    if params.has_key('repo_info'):
      self.repoInfoList = params['repo_info']

  def generateFiles(self):
    repoPuppetFiles = []
    for repo in self.repoInfoList:
      repoFile = open(self.path + os.sep + repo['repoId'] + '-' + str(self.taskId) + PUPPET_EXT, 'w+')
      writeImports(repoFile, self.modulesdir, inputFileName='imports.txt')
      
      baseUrl = ''
      mirrorList = ''
      
      if repo.has_key('baseUrl'):
        baseUrl = repo['baseUrl']

      if repo.has_key('mirrorsList'):
        mirrorList = repo['mirrorsList']

      repoFile.write('node /default/ {')
      repoFile.write('class{ "hdp-repos::process_repo" : ' + ' os_type => "' + repo['osType'] +
      '", repo_id => "' + repo['repoId'] + '", base_url => "' + baseUrl +
      '", mirror_list => "' + mirrorList +'", repo_name => "' + repo['repoName'] + '" }' )
      repoFile.write('}')
      repoFile.close()
      repoPuppetFiles.append(repoFile.name)

    return repoPuppetFiles

  def installRepos(self):
    self.prepareReposInfo()
    repoPuppetFiles = self.generateFiles()
    return repoPuppetFiles

def main():
  #Test code
  jsonFile = open('test.json', 'r')
  jsonStr = jsonFile.read() 
  parsedJson = json.loads(jsonStr)
  repoInstaller = RepoInstaller(parsedJson, '/tmp', '/home/centos/ambari_ws/ambari-agent/src/main/puppet/modules')
  repoInstaller.installRepos()
  
if __name__ == '__main__':
  main()


