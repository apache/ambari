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

"""

import platform, os, sys

path = os.path.abspath(__file__)
path = os.path.join(os.path.dirname(os.path.dirname(path)), "psutil", "build")

IS_WINDOWS = platform.system() == "Windows"

if not IS_WINDOWS:
  for dir in os.walk(path).next()[1]:
    if 'lib' in dir:
      sys.path.insert(1, os.path.join(path, dir))

try:
  import psutil
except ImportError:
  print 'psutil binaries need to be built by running, psutil/build.py ' \
        'manually or by running a, mvn clean package, command.'
