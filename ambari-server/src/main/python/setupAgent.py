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

def installAgentSuse(projectVersion):
  """ Run zypper install and make sure the agent install alright """
  zypperCommand = ["zypper", "install", "-y", "ambari-agent" + projectVersion]
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

def installAgent(projectVersion):
  """ Run yum install and make sure the agent install alright """
  # The command doesn't work with file mask ambari-agent*.rpm, so rename it on agent host
  rpmCommand = ["yum", "-y", "install", "--nogpgcheck", "ambari-agent" + projectVersion]
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

    ret = execOsCommand(["tail", "-20", "/var/log/ambari-agent/ambari-agent.log"])
    if not 0 == ret['exitstatus']:
      return ret['exitstatus']
    print ret['log']

    return 0
  except (Exception), e:
    return 1

def getOptimalVersion(initialProjectVersion):
  if initialProjectVersion == "":
    return initialProjectVersion
  optimalVersion = initialProjectVersion

  if is_suse():
    ret = checkAgentPackageAvailabilitySuse(optimalVersion)
  else:
    ret = checkAgentPackageAvailability(optimalVersion)

  # Specified package version found
  if ret["exitstatus"] == 0:
    return optimalVersion

  # Specified package version not found; find nearest version
  optimalVersion = optimalVersion + "."

  if is_suse():
    ret = findNearestAgentPackageVersionSuse(optimalVersion)
  else:
    ret = findNearestAgentPackageVersion(optimalVersion)

  if ret["exitstatus"] == 0 and ret["log"][0].strip() != "":
    optimalVersion = ret["log"][0].strip()
  else:
    optimalVersion = ""

  return optimalVersion

def checkAgentPackageAvailabilitySuse(projectVersion):
  zypperCommand = ["bash", "-c", "zypper search -s --match-exact ambari-agent | grep ' " + projectVersion + " '"]
  return execOsCommand(zypperCommand)

def checkAgentPackageAvailability(projectVersion):
  yumCommand = ["bash", "-c", "yum list available ambari-agent | grep ' " + projectVersion + " '"]
  return execOsCommand(yumCommand)

def findNearestAgentPackageVersionSuse(projectVersion):
  zypperCommand = ["bash", "-c", "zypper search -s --match-exact ambari-agent | grep ' " + projectVersion + "' | cut -d '|' -f 4 | head -n1"]
  return execOsCommand(zypperCommand)

def findNearestAgentPackageVersion(projectVersion):
  yumCommand = ["bash", "-c", "yum list available ambari-agent | grep ' " + projectVersion + "' | sed -re 's/\s+/ /g' | cut -d ' ' -f 2 | head -n1"]
  return execOsCommand(yumCommand)

def main(argv=None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  # Parse the input
  onlyargs = argv[1:]
  passPhrase = onlyargs[0]
  hostName = onlyargs[1]
  projectVersion = None
  if len(onlyargs) > 2:
    projectVersion = onlyargs[2]

  if projectVersion is None or projectVersion == "null":
    projectVersion = ""

  projectVersion = getOptimalVersion(projectVersion)
  if projectVersion != "":
    projectVersion = "-" + projectVersion

  if is_suse():
    installAgentSuse(projectVersion)
  else:
    installPreReq()
    installAgent(projectVersion)

  configureAgent(hostName)
  sys.exit(runAgent(passPhrase))

if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  main(sys.argv)

