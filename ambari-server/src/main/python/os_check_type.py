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

import sys
from common_functions import OSCheck

def main(argv=None):
  # Same logic that was in "os_type_check.sh"
  if len(sys.argv) != 2:
    print("Usage: <cluster_os>")
    raise Exception("Error in number of arguments. Usage: <cluster_os>")
    pass

  cluster_os = sys.argv[1]
  current_os = OSCheck.get_os_family() + OSCheck.get_os_major_version()

  # If agent/server have the same {"family","main_version"} - then ok.
  print(("Cluster primary/cluster OS type is %s and local/current OS type is %s" % (
    cluster_os, current_os)))
  if current_os == cluster_os:
    sys.exit(0)
  else:
    raise Exception("Local OS is not compatible with cluster primary OS. Please perform manual bootstrap on this host.")


if __name__ == "__main__":
  main()
