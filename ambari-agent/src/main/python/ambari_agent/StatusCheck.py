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

import logging
import os
import re
import string
from ambari_commons.shell import shellRunner


logger = logging.getLogger()


class StatusCheck:
    
  USER_PATTERN='{USER}'
  firstInit = True

  def listFiles(self, dir):
    basedir = dir
    logger.debug("Files in " + os.path.abspath(dir) + ": ")
    subdirlist = []
    try:
      if os.path.isdir(dir):
        for item in os.listdir(dir):
            if os.path.isfile(item) and item.endswith('.pid'):
              self.pidFilesDict[item.split(os.sep).pop()] = os.getcwd() + os.sep + item
            else:
              subdirlist.append(os.path.join(basedir, item))
        for subdir in subdirlist:
            self.listFiles(subdir)
      else:
        if dir.endswith('.pid'):
          self.pidFilesDict[dir.split(os.sep).pop()] = dir
    except OSError as e:
      logger.info(e.strerror + ' to ' + e.filename)
      
  def fillDirValues(self):
    try:
      for pidVar in self.pidPathVars:
        pidVarName = pidVar['var']
        pidDefaultvalue = pidVar['defaultValue']
        if self.globalConfig.has_key(pidVarName):
          self.pidPathes.append(self.globalConfig[pidVarName])
        else:
          self.pidPathes.append(pidDefaultvalue)
    except Exception as e:
        logger.error("Error while filling directories values " + str(e))
        
  def __init__(self, serviceToPidDict, pidPathVars, globalConfig,
    servicesToLinuxUser):
    
    self.serToPidDict = serviceToPidDict.copy()
    self.pidPathVars = pidPathVars
    self.pidPathes = []
    self.sh = shellRunner()
    self.pidFilesDict = {}
    self.globalConfig = globalConfig
    self.servicesToLinuxUser = servicesToLinuxUser
    
    self.fillDirValues()
    
    for pidPath in self.pidPathes:
      self.listFiles(pidPath)

    for service, pid in self.serToPidDict.items():
      if self.servicesToLinuxUser.has_key(service):
        linuxUserKey = self.servicesToLinuxUser[service]
        if self.globalConfig.has_key(linuxUserKey):
          self.serToPidDict[service] = string.replace(pid, self.USER_PATTERN,
            self.globalConfig[linuxUserKey])
      else:
        if self.USER_PATTERN in pid:
          logger.error('There is no linux user mapping for component: ' + service)

    if StatusCheck.firstInit:
      logger.info('Service to pid dictionary: ' + str(self.serToPidDict))
      StatusCheck.firstInit = False
    else:
      logger.debug('Service to pid dictionary: ' + str(self.serToPidDict))

  def getIsLive(self, pidPath):

    if not pidPath:
      return False

    isLive = False
    pid = -1
    try:
      pidFile = open(pidPath, 'r')
      pid = int(pidFile.readline())
    except IOError, e:
      logger.warn("Can not open file " + str(pidPath) + " due to " + str(e))
      return isLive
    res = self.sh.run(['ps -p', str(pid), '-f'])
    lines = res['output'].strip().split(os.linesep)
    try:
      procInfo = lines[1]
      isLive = not procInfo == None
    except IndexError:
      logger.info("Process is dead. Checking " + str(pidPath))
    return isLive

  def getStatus(self, serviceCode):
    try:
      pidPath = None
      pidPattern = self.serToPidDict[serviceCode]
      logger.debug('pidPattern: ' + pidPattern)
    except KeyError as e:
      logger.warn('There is no mapping for ' + serviceCode)
      return None
    try:
      for pidFile in self.pidFilesDict.keys():
        if re.match(pidPattern, pidFile):
          pidPath = self.pidFilesDict[pidFile]          
      logger.debug('pidPath: ' + str(pidPath))
      result = self.getIsLive(pidPath)
      return result
    except KeyError:
      logger.info('Pid file was not found')
      return False

  def getSerToPidDict(self):
    return self.serToPidDict

