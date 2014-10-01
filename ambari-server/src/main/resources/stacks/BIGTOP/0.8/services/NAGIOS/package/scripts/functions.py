#!/usr/bin/env python
"""
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

Ambari Agent

"""
from resource_management import *

# Gets if the java version is greater than 6
def is_jdk_greater_6(java64_home):
  import os
  import re
  java_bin = os.path.join(java64_home, 'bin', 'java')
  ver_check = shell.call([java_bin, '-version'])

  ver = ''
  if 0 != ver_check[0]:
    # java is not local, try the home name as a fallback
    ver = java64_home
  else:
    ver = ver_check[1]

  regex = re.compile('"1\.([0-9]*)\.0_([0-9]*)"', re.IGNORECASE)
  r = regex.search(ver)
  if r:
    strs = r.groups()
    if 2 == len(strs):
      minor = int(strs[0])
      if minor > 6:
        return True

  return False
