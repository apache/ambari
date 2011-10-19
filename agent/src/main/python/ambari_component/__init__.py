#!/usr/bin/env python

# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Ambari Plugin Library"""

from __future__ import generators

__version__ = "0.1.0"
__author__ = [
    "see http://incubator.apache.org/ambari/team-list.html"
]
__license__ = "Apache License v2.0"
__contributors__ = "see http://incubator.apache.org/ambari"

import logging
import logging.handlers
import sys
import time
import signal
from ConfigWriter import ConfigWriter

def copySh(config, options):
  result = ConfigWriter().shell(config, options)
  return result

def copyXml(config, options):
  result = ConfigWriter().xml(config, options)
  return result

def copyProperties(config, options):
  result = ConfigWriter().plist(config, options)
  return result

def install(cluster, role, packages):
  return package.install(cluster, role, packages)
