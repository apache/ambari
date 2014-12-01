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

from subprocess import call
import sys
import os
import shutil

def build():
  path = os.path.dirname(os.path.abspath(__file__))
  build_path = path + os.sep + 'build'
  build_out_path = path + os.sep + 'build.out'
  build_out = open(build_out_path, 'wb')

  # Delete old build dir if exists
  if (os.path.exists(build_path)):
    shutil.rmtree(build_path)
  pass

  cwd = os.getcwd()
  os.chdir(path)

  print 'Executing make at location: %s ' % path

  if sys.platform.startswith("win"):
    # Windows
    returncode = call(['make.bat', 'build'], stdout=build_out, stderr=build_out)
  else:
    # Unix based
    returncode = call(['make', 'build'], stdout=build_out, stderr=build_out)
  pass

  os.chdir(cwd)

  if returncode != 0:
    print 'psutil build failed. Please find build output at: %s' % build_out_path
  pass

if __name__ == '__main__':
  build()