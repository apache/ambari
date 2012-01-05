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

def getFilePath(action, fileName=""):
  #Change the method signature to take the individual action fields
  pathComp=""
  if 'clusterId' in action:
    pathComp = action['clusterId']
  if 'role' in action:
    pathComp = pathComp + "-" + action['role'] 
  path = os.path.join(AmbariConfig.config.get('agent','prefix'),
                      "clusters", 
                      pathComp)
  fullPathName=""
  if fileName != "":
    fullPathName=os.path.join(path, fileName)
  else:
    fileInfo = action['file']
    fullPathName=os.path.join(path, fileInfo['path'])
  return fullPathName
  
def appendToFile(data,absolutePath):
  f = open(absolutePath, 'a')
  f.write(data)
  f.close()

def writeFile(action, result, fileName=""):
  fileInfo = action['file']
  pathComp=""
  if 'clusterId' in action:
    pathComp = action['clusterId']
  if 'role' in action:
    pathComp = pathComp + "-" + action['role'] 
  try:
    path = os.path.join(AmbariConfig.config.get('agent','prefix'),
                        "clusters", 
                        pathComp)
    user=getpass.getuser()
    if 'owner' in fileInfo:
      user=fileInfo['owner']
    group=os.getgid()
    if 'group' in fileInfo:
      group=fileInfo['group']
    fullPathName=""
    if fileName != "":
      fullPathName=os.path.join(path, fileName)
    else:
      fullPathName=os.path.join(path, fileInfo['path'])
    logger.debug("path in writeFile: %s" % fullPathName)
    content=fileInfo['data']
    try:
      if isinstance(user, int)!=True:
        user=getpwnam(user)[2]
      if isinstance(group, int)!=True:
        group=getgrnam(group)[2]
    except Exception:
      logger.warn("can not find user uid/gid: (%s/%s) for writing %s" % (user, group, fullPathName))
    if 'permission' in fileInfo:
      if fileInfo['permission'] is not None:
        permission=fileInfo['permission']
    else:
      permission=0750
    oldMask = os.umask(0)
    if 'umask' in fileInfo:
      if fileInfo['umask'] is not None: 
        umask=int(fileInfo['umask'])
    else:
      umask=oldMask 
    os.umask(int(umask))
    prefix = os.path.dirname(fullPathName)
    try:
      os.makedirs(prefix)
    except OSError as err:
      if err.errno == errno.EEXIST:
        pass
      else:
        raise
    f = open(fullPathName, 'w')
    f.write(content)
    f.close()
    if os.getuid()==0:
      os.chmod(fullPathName, permission)
      os.chown(fullPathName, user, group)
    os.umask(oldMask)
    result['exitCode'] = 0
  except Exception, err:
    traceback.print_exc()
    result['exitCode'] = 1
    result['error'] = traceback.format_exc()
  return result

def createStructure(action, result):
  try:
    workdir = action['workDirComponent']
    path = AmbariConfig.config.get('agent','prefix')+"/clusters/"+workdir
    shutil.rmtree(path, 1)
    os.makedirs(path+"/stack")
    os.makedirs(path+"/logs")
    os.makedirs(path+"/data")
    os.makedirs(path+"/pkgs")
    os.makedirs(path+"/config")
    result['exitCode'] = 0
  except Exception, err:
    traceback.print_exc()
    result['exitCode'] = 1
    result['error'] = traceback.format_exc()
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
    result['error'] = traceback.format_exc()
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
