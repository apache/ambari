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

from shell import shellRunner
import logging
import logging.handlers
import sys
import os

logger = logging.getLogger()





class StatusCheck:

  def get_pair(self, line):
    key, sep, value = line.strip().partition("=")
    return key, value

  def listFiles(self, dir):
    basedir = dir
    logger.debug("Files in " + os.path.abspath(dir) + ": ")
    subdirlist = []
    try:
      if os.path.isdir(dir):
        for item in os.listdir(dir):
            if os.path.isfile(item) and item.endswith('.pid'):
              self.pidFilesDict[item.split(os.sep).pop()] = item
            else:
                subdirlist.append(os.path.join(basedir, item))
        for subdir in subdirlist:
            self.listFiles(subdir)
      else:
        if dir.endswith('.pid'):
          self.pidFilesDict[dir.split(os.sep).pop()] = dir
    except OSError as e:
      logger.info(e.strerror + ' to ' + e.filename)

  def __init__(self, path, mappingFilePath):
    if not os.path.isdir(path):
      raise ValueError("Path argument must be valid directory")

    if not os.path.exists(mappingFilePath):
      raise IOError("File with services to pid mapping doesn't exist")
    self.path = path
    self.mappingFilePath = mappingFilePath
    self.sh = shellRunner()
    self.pidFilesDict = {}
    self.listFiles(self.path)


    with open(self.mappingFilePath) as fd:    
      self.serToPidDict = dict(self.get_pair(line) for line in fd)

  def getIsLive(self, pidPath):
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
      logger.info('Process is dead')
    return isLive

  def getStatus(self, serviceCode):
    try:
      pidName = self.serToPidDict[serviceCode]
      logger.info( 'pidName: ' + pidName)
    except KeyError as e:
      logger.warn('There is no mapping for ' + serviceCode)
      return None
    try:
      pidPath = self.pidFilesDict[pidName]
      logger.info('pidPath: ' + pidPath)
      result = self.getIsLive(self.pidFilesDict[pidName])
      return result
    except KeyError:
      logger.info('Pid file was not found')
      return False

  def getSerToPidDict(self):
    return self.serToPidDict

