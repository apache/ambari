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

import socket
import time
import sys
import logging
import pprint
import os
import subprocess
import threading
import traceback
from pprint import pformat

def installAgent():
  """ Run yum install and make sure the agent install alright """
  # TODO replace echo with yum
  yumcommand = ["echo", "install", "ambari-agent"]
  yumstat = subprocess.Popen(yumcommand, stdout=subprocess.PIPE)
  log = yumstat.communicate(0)
  ret = {}
  ret["exitstatus"] = yumstat.returncode
  ret["log"] = log
  return ret

def configureAgent():
  """ Configure the agent so that it has all the configs knobs properly 
  installed """
  return

def main(argv=None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  installAgent()
  configureAgent()
  
if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  main(sys.argv)

