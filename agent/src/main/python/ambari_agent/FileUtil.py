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

from pwd import getpwnam
from grp import getgrnam
import getpass
import os
import sys, traceback

def writeFile(parts):
  try:
    user=parts[1]
    if isinstance(user, str):
      user=getpwnam(user)[2]
    group=parts[2]
    if isinstance(group, str):
      group=getgrnam(group)[2]
    permission=parts[3]
    filename=parts[4]
    content=parts[5]

    f = open(filename, 'w')
    f.write(content)
    f.close()
    os.chmod(filename, permission)
    os.chown(filename, user, group)
    return { 'exit_code' : 0 }
  except Exception, err:
    return { 'exit_code' : 1, 'stderr' : traceback.format_exc() }

def main():
  data = [ 'ambari-write-file', os.getuid(), os.getgid() , 0700, "/tmp/_file_write_test", "This is a test" ]
  print writeFile(data)
  data = [ 'ambari-write-file', 'root', 'staff', 0777, "/tmp/_file_write_test2", "This is a test" ]
  print writeFile(data)

if __name__ == "__main__":
  main()
