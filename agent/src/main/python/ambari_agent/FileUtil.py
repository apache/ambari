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

logger = logging.getLogger()

def writeFile(action, result):
  fileInfo = action['file']
  try:
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
    return result
  except Exception, err:
    result['exitCode'] = 1
    result['stderr'] = traceback.format_exc()
    return result

def main():
  config = {
    "data"       : "test", 
    "owner"      : os.getuid(), 
    "group"      : os.getgid() , 
    "permission" : 0700, 
    "path"       : "/tmp/ambari_file_test/_file_write_test", 
    "umask"      : 022 
  }
  action = { 'file' : config }
  result = { }
  print writeFile(action, result)
  config = { 
    "data"       : "test", 
    "owner"      : "eyang", 
    "group"      : "staff", 
    "permission" : "0700", 
    "path"       : "/tmp/ambari_file_test/_file_write_test", 
    "umask"      : "022" 
  }
  result = { }
  action = { 'file' : config }
  print writeFile(action, result)

if __name__ == "__main__":
  main()
