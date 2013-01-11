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
import stat
from pprint import pformat
import re

AMBARI_PASSPHRASE_VAR = "AMBARI_PASSPHRASE"


def execOsCommand(osCommand):
  osStat = subprocess.Popen(osCommand, stdout=subprocess.PIPE)
  log = osStat.communicate(0)
  ret = {}
  ret["exitstatus"] = osStat.returncode
  ret["log"] = log
  return ret

def is_suse():
  if os.path.isfile("/etc/issue"):
    if "suse" in open("/etc/issue").read().lower():
      return True
  return False

def installPreReqSuse():
  """ required for ruby deps """
  # remove once in the repo
  zypperCommand = ["zypper", "install", "-y",
    "http://download.opensuse.org/repositories/home:/eclipseagent:/puppet/SLE_11_SP1/x86_64/ruby-augeas-0.4.1-26.3.x86_64.rpm",
    "http://download.opensuse.org/repositories/home:/eclipseagent:/puppet/SLE_11_SP1/x86_64/ruby-shadow-1.4.1-2.2.x86_64.rpm"]
  return execOsCommand(zypperCommand)

def installAgentSuse():
  """ Run zypper install and make sure the agent install alright """
  zypperCommand = ["zypper", "install", "-y", "ambari-agent"]
  return execOsCommand(zypperCommand)

def installPreReq():
  """ required for ruby deps """
  checkepel = ["yum", "repolist", "enabled"]
  retval = execOsCommand(checkepel)
  logval = str(retval["log"])
  if not "epel" in logval:
    yumCommand = ["yum", "-y", "install", "epel-release"]
  else:
    yumCommand = ["echo", "Epel already exists"]
  return execOsCommand(yumCommand)

def installAgent():
  """ Run yum install and make sure the agent install alright """
  # The command doesn't work with file mask ambari-agent*.rpm, so rename it on agent host
  rpmCommand = ["yum", "-y", "install", "--nogpgcheck", "ambari-agent"]
  return execOsCommand(rpmCommand)

def configureAgent(host):
  """ Configure the agent so that it has all the configs knobs properly installed """
  osCommand = ["sed", "-i.bak", "s/hostname=localhost/hostname=" + host + "/g", "/etc/ambari-agent/conf/ambari-agent.ini"]
  execOsCommand(osCommand)

  return

def runAgent(passPhrase):
  os.environ[AMBARI_PASSPHRASE_VAR] = passPhrase
  subprocess.call("/usr/sbin/ambari-agent start", shell=True)
  try:
    # print this to the log.  despite the directory, machine.py works with Python 2.4
    ret = execOsCommand(["python", "/usr/lib/python2.6/site-packages/ambari_agent/machine.py"])
    if not 0 == ret['exitstatus']:
      return ret['exitstatus']
    print ret['log']

    ret = execOsCommand(["tail", "-20", "/var/log/ambari-agent/ambari-agent.log"])
    if not 0 == ret['exitstatus']:
      return ret['exitstatus']
    print ret['log']

    return 0
  except (Exception), e:
    return 1

def main(argv=None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  # Parse the input
  onlyargs = argv[1:]
  passPhrase = onlyargs[0]
  hostName = onlyargs[1]

  if is_suse():
    installPreReqSuse()
    installAgentSuse()
  else:
    installPreReq()
    installAgent()

  configureAgent(hostName)
  sys.exit(runAgent(passPhrase))
  
if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  main(sys.argv)

