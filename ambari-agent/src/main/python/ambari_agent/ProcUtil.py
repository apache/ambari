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

import sys
import os

def get_proc_status(pid):
  pid = int(pid)
  path = ("/proc/%d/status" % pid)
  if not os.path.exists(path):
    return None
  status_file = open(path)
  lines = status_file.readlines()
  for line in lines:
    if line.startswith("State:"):
      return line.split(":",1)[1].strip().split(' ')[0].split(" ",1)[0]
  return None
    
if __name__ == '__main__':
  state = get_proc_status(sys.argv[1])
  print state
