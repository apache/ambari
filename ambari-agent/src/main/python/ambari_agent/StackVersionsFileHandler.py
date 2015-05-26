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

import os.path
import logging
import threading
import traceback
import shutil

logger = logging.getLogger(__name__)

class StackVersionsFileHandler:

  VER_FILE = "current-stack"
  DEFAULT_VER = ""

  def __init__(self, versionsFileDir):
    self.versionsFileDir = versionsFileDir
    self.versionsFilePath = os.path.join(versionsFileDir, self.VER_FILE)
    self._lock = threading.RLock()

  def read_stack_version(self, component):
    try :
      self.touch_file()
      for line in open(self.versionsFilePath):
        comp, ver = self.extract(line)
        if comp == component:
          return ver
      return self.DEFAULT_VER
    except Exception, err:
      logger.error("Can't read versions file: %s " % err.message)
      traceback.print_exc()
      return self.DEFAULT_VER


  def read_all_stack_versions(self):
    result = {}
    try :
      self.touch_file()
      for line in open(self.versionsFilePath):
        comp, ver = self.extract(line)
        if comp != self.DEFAULT_VER:
          result[comp] = ver
      return result
    except Exception, err:
      logger.error("Can't read stack versions file: %s " % err.message)
      traceback.print_exc()
      return {}


  def write_stack_version(self, component, newVersion):
    self._lock.acquire()
    try:
      values = self.read_all_stack_versions()
      values[component] = newVersion
      logger.info("Backing up old stack versions file")
      backup = os.path.join(self.versionsFileDir, self.VER_FILE + ".bak")
      shutil.move(self.versionsFilePath, backup)
      logger.info("Writing new stack versions file")
      with open (self.versionsFilePath, 'w') as f:
        for key in values:
          f.write ("%s\t%s\n" % (key, values[key]))

    except Exception, err:
      logger.error("Can't write new stack version (%s %s) :%s " % (component,
            newVersion, err.message))
      traceback.print_exc()
    finally:
      self._lock.release()


  def extract(self, statement):
    '''
    Extracts <Component>, <HDPstack version> values from lines like
    GANGLIA	StackVersion-1.3.0
    '''
    parts = statement.strip().split()
    if len(parts) != 2:
      logger.warn("Wrong stack versions file statement format: %s" % statement)
      return self.DEFAULT_VER, self.DEFAULT_VER
    else:
      return parts[0], parts[1]


  def touch_file(self):
    '''
     Called to create file when it does not exist
    '''
    if not os.path.isfile(self.versionsFilePath):
      logger.info("Creating stacks versions file at %s" % self.versionsFilePath)
      open(self.versionsFilePath, 'w').close()


