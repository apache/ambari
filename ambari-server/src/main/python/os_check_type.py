#!/usr/bin/ambari-python-wrap

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
from ambari_commons import OSCheck

def main(argv=None):
  # Same logic that was in "os_type_check.sh"

  current_os = get_os_type()
  #log the current os type value to be used during bootstrap
  print current_os

def get_os_type():
  return OSCheck.get_os_family() + OSCheck.get_os_major_version()

if __name__ == "__main__":
  main()
