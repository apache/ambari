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

from pwd import getpwnam
from grp import getgrnam
import logging
import logging.handlers
import getpass
import os, errno
import sys, traceback
import ConfigParser
import shutil
import StringIO
import AmbariConfig

logger = logging.getLogger()

def writeFile(action, result):
  oldCwd = os.getcwd()
  fileInfo = action['file']
  try:
    path = AmbariConfig.config.get('agent','prefix')+"/clusters/"+action['clusterId']+"-"+action['role']
    logger.info("path: %s" % path)
    os.chdir(path)
    user=fileInfo['owner']
    group=fileInfo['group']
    filename=fileInfo['path']
    content=fileInfo['data']
    try:
      if isinstance(user, int)!=True:
        user=getpwnam(user)[2]
      if isinstance(group, int)!=True:
        group=getgrnam(group)[2]
    except Exception:
      logger.warn("can not find user uid/gid: (%s/%s) for writing %s" % (user, group, filename))
    permission=int(fileInfo['permission'])
    umask=int(fileInfo['umask'])
    oldMask = os.umask(0)
    os.umask(int(umask))
    prefix = os.path.dirname(filename)
    try:
      os.makedirs(prefix)
    except OSError as err:
      if err.errno == errno.EEXIST:
        pass
      else:
        raise
    f = open(filename, 'w')
    f.write(content)
    f.close()
    if os.getuid()==0:
      os.chmod(filename, permission)
      os.chown(filename, user, group)
    os.umask(oldMask)
    result['exitCode'] = 0
  except Exception, err:
    result['exitCode'] = 1
    result['stderr'] = traceback.format_exc()
  os.chdir(oldCwd)
  return result

def createStructure(action, result):
  try:
    workdir = action['workDirComponent']
    path = AmbariConfig.config.get('agent','prefix')+"/clusters/"+workdir
    os.makedirs(path+"/stack")
    os.makedirs(path+"/logs")
    os.makedirs(path+"/data")
    os.makedirs(path+"/pkgs")
    os.makedirs(path+"/config")
    result['exitCode'] = 0
  except Exception, err:
    result['exitCode'] = 1
    result['stderr'] = traceback.format_exc()
  return result

def deleteStructure(action, result):
  try:
    workdir = action['workDirComponent']
    path = AmbariConfig.config.get('agent','prefix')+"/clusters/"+workdir
    if os.path.exists(path):
      shutil.rmtree(path)
    result['exitCode'] = 0
  except Exception, err:
    result['exitCode'] = 1
    result['stderr'] = traceback.format_exc()
  return result

def main():

  action = { 'clusterId' : 'abc', 'role' : 'hdfs' }
  result = {}
  print createStructure(action, result)

  configFile = {
    "data"       : "test", 
    "owner"      : os.getuid(), 
    "group"      : os.getgid() , 
    "permission" : 0700, 
    "path"       : "/tmp/ambari_file_test/_file_write_test", 
    "umask"      : 022 
  }
  action = { 'file' : configFile }
  result = { }
  print writeFile(action, result)

  configFile = { 
    "data"       : "test", 
    "owner"      : "eyang", 
    "group"      : "staff", 
    "permission" : "0700", 
    "path"       : "/tmp/ambari_file_test/_file_write_test", 
    "umask"      : "022" 
  }
  result = { }
  action = { 'file' : configFile }
  print writeFile(action, result)

  print deleteStructure(action, result)

if __name__ == "__main__":
  main()
