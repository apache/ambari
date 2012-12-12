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

AMBARI_PASSPHRASE_VAR = "AMBARI_PASSPHRASE"

def execOsCommand(osCommand):
  """ Run yum install and make sure the puppet install alright """
  osStat = subprocess.Popen(osCommand, stdout=subprocess.PIPE)
  log = osStat.communicate(0)
  ret = {}
  ret["exitstatus"] = osStat.returncode
  ret["log"] = log
  return ret

def installPreReq():
  """ Adds hdp repo
  rpmCommand = ["rpm", "-Uvh", "http://public-repo-1.hortonworks.com/HDP-1.1.1.16/repos/centos6/hdp-release-1.1.1.16-1.el6.noarch.rpm"]
  execOsCommand(rpmCommand)
  """
  yumCommand = ["yum", "-y", "install", "epel-release"]
  execOsCommand(yumCommand)

def installPuppet():
  """ Run yum install and make sure the puppet install alright """
  osCommand = ["useradd", "-G", "puppet", "puppet"]
  execOsCommand(osCommand)
  yumCommand = ["yum", "-y", "install", "puppet"]
  return execOsCommand(yumCommand)

def installAgent():
  """ Run yum install and make sure the agent install alright """
  # TODO replace rpm with yum -y
  rpmCommand = ["yum", "install", "-y", "--nogpgcheck", "/tmp/ambari-agent*.rpm"]
  return execOsCommand(rpmCommand)

def configureAgent():
  """ Configure the agent so that it has all the configs knobs properly 
  installed """
  return

def runAgent(passPhrase):
  os.environ[AMBARI_PASSPHRASE_VAR] = passPhrase
  subprocess.call("/usr/sbin/ambari-agent start", shell=True)

def main(argv=None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  """ Parse the input"""
  onlyargs = argv[1:]
  passPhrase = onlyargs[0]
  installPreReq()
  # installPuppet()
  installAgent()
  configureAgent()
  runAgent(passPhrase)
  
if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  main(sys.argv)

