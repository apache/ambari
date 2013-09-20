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

import json
import logging
import os
import shutil

logger = logging.getLogger()

class ActualConfigHandler:
  CONFIG_NAME = 'config.json'

  def __init__(self, config):
    self.config = config;

  def findRunDir(self):
    runDir = '/var/run/ambari-agent'
    if self.config.has_option('agent', 'prefix'):
      runDir = self.config.get('agent', 'prefix')
    if not os.path.exists(runDir):
      runDir = '/tmp'
    return runDir

  def write_actual(self, configTags):
    runDir = self.findRunDir()
    conf_file = open(os.path.join(runDir, self.CONFIG_NAME), 'w')
    json.dump(configTags, conf_file)
    conf_file.close()

  def copy_to_component(self, componentName):
    runDir = self.findRunDir()
    srcfile = os.path.join(runDir, self.CONFIG_NAME)
    if os.path.isfile(srcfile):
      dstfile = os.path.join(runDir, componentName + "_" + self.CONFIG_NAME)
      shutil.copy(srcfile, dstfile)

  def read_file(self, filename):
    runDir = self.findRunDir()
    fullname = os.path.join(runDir, filename)
    if os.path.isfile(fullname):
      res = None
      conf_file = open(os.path.join(runDir, filename), 'r')
      try:
        res = json.load(conf_file)
        if (0 == len(res)):
          res = None
      except Exception, e:
        logger.error("Error parsing " + filename + ": " + repr(e))
        res = None
        pass
      conf_file.close()

      return res
    return None

  def read_actual(self):
    return self.read_file(self.CONFIG_NAME)

  def read_actual_component(self, componentName):
    return self.read_file(componentName + "_" + self.CONFIG_NAME)
    

