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

import datetime
import os.path
import logging
import traceback
import ConfigParser;

logger = logging.getLogger()

class HostCheckReportFileHandler:

  HOST_CHECK_FILE = "hostcheck.result"

  def __init__(self, config):
    if config != None:
      hostCheckFileDir = config.get('agent', 'prefix')
      self.hostCheckFilePath = os.path.join(hostCheckFileDir, self.HOST_CHECK_FILE)

  def writeHostCheckFile(self, hostInfo):
    if self.hostCheckFilePath == None:
      return

    try:
      logger.info("Host check report at " + self.hostCheckFilePath)
      config = ConfigParser.RawConfigParser()
      config.add_section('metadata')
      config.set('metadata', 'created', str(datetime.datetime.now()))

      if 'existingUsers' in hostInfo.keys():
        items = []
        items2 = []
        for itemDetail in hostInfo['existingUsers']:
          items.append(itemDetail['name'])
          items2.append(itemDetail['homeDir'])
        config.add_section('users')
        config.set('users', 'usr_list', ','.join(items))
        config.set('users', 'usr_homedir_list', ','.join(items2))

      if 'alternatives' in hostInfo.keys():
        items = []
        items2 = []
        for itemDetail in hostInfo['alternatives']:
          items.append(itemDetail['name'])
          items2.append(itemDetail['target'])
        config.add_section('alternatives')
        config.set('alternatives', 'symlink_list', ','.join(items))
        config.set('alternatives', 'target_list', ','.join(items2))

      if 'stackFoldersAndFiles' in hostInfo.keys():
        items = []
        for itemDetail in hostInfo['stackFoldersAndFiles']:
          items.append(itemDetail['name'])
        config.add_section('directories')
        config.set('directories', 'dir_list', ','.join(items))

      if 'hostHealth' in hostInfo.keys():
        if 'activeJavaProcs' in hostInfo['hostHealth'].keys():
          items = []
          for itemDetail in hostInfo['hostHealth']['activeJavaProcs']:
            items.append(itemDetail['pid'])
          config.add_section('processes')
          config.set('processes', 'proc_list', ','.join(map(str, items)))

      if 'installedPackages' in hostInfo.keys():
        items = []
        for itemDetail in hostInfo['installedPackages']:
          items.append(itemDetail['name'])
        config.add_section('packages')
        config.set('packages', 'pkg_list', ','.join(map(str, items)))

      if 'existingRepos' in hostInfo.keys():
        config.add_section('repositories')
        config.set('repositories', 'repo_list', ','.join(hostInfo['existingRepos']))

      self.removeFile()
      self.touchFile()
      with open(self.hostCheckFilePath, 'wb') as configfile:
        config.write(configfile)
    except Exception, err:
      logger.error("Can't write host check file at %s :%s " % (self.hostCheckFilePath, err.message))
      traceback.print_exc()

  def removeFile(self):
    if os.path.isfile(self.hostCheckFilePath):
      logger.info("Removing old host check file at %s" % self.hostCheckFilePath)
      os.remove(self.hostCheckFilePath)

  def touchFile(self):
    if not os.path.isfile(self.hostCheckFilePath):
      logger.info("Creating host check file at %s" % self.hostCheckFilePath)
      open(self.hostCheckFilePath, 'w').close()


