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
  ret = {"exitstatus": osStat.returncode, "log": log}
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

def installAgent(projectVersion):
  """ Run yum install and make sure the agent install alright """
  # The command doesn't work with file mask ambari-agent*.rpm, so rename it on agent host
  rpmCommand = ["yum", "-y", "install", "--nogpgcheck", "ambari-agent" + projectVersion]
  return execOsCommand(rpmCommand)

def configureAgent(server_hostname):
  """ Configure the agent so that it has all the configs knobs properly installed """
  osCommand = ["sed", "-i.bak", "s/hostname=localhost/hostname=" + server_hostname +\
                                "/g", "/etc/ambari-agent/conf/ambari-agent.ini"]
  execOsCommand(osCommand)
  return

def runAgent(passPhrase, expected_hostname):
  os.environ[AMBARI_PASSPHRASE_VAR] = passPhrase
  agent_retcode = subprocess.call("/usr/sbin/ambari-agent restart --expected-hostname=" +\
                                  expected_hostname, shell=True)
  # need this, because, very rarely,
  # main.py(ambari-agent) starts a bit later then it should be started
  time.sleep(1)
  try:
    ret = execOsCommand(["tail", "-20", "/var/log/ambari-agent/ambari-agent.log"])
    try:
      log = ret['log']
    except Exception:
      log = "Log not found"
    print log
    if not 0 == ret['exitstatus']:
      return ret['exitstatus']

    return agent_retcode
  except (Exception), e:
    return 1


def getOptimalVersion(initialProjectVersion):
  optimalVersion = initialProjectVersion
  if is_suse():
    ret = findNearestAgentPackageVersionSuse(optimalVersion)
  else:
    ret = findNearestAgentPackageVersion(optimalVersion)

  if ret["exitstatus"] == 0 and ret["log"][0].strip() != "" and ret["log"][0].strip() == initialProjectVersion:
    optimalVersion = ret["log"][0].strip()
    retcode = 0
  else:
    if is_suse():
        ret = getAvaliableAgentPackageVersionsSuse()
    else:
        ret = getAvaliableAgentPackageVersions()
    retcode = 1
    optimalVersion = ret["log"]

  return {"exitstatus": retcode, "log": optimalVersion}


def findNearestAgentPackageVersionSuse(projectVersion):
  if projectVersion == "":
    projectVersion = "  "
  zypperCommand = ["bash", "-c", "zypper search -s --match-exact ambari-agent | grep '" + projectVersion +\
                                 "' | cut -d '|' -f 4 | head -n1 | sed -e 's/-\w[^:]*//1' "]
  return execOsCommand(zypperCommand)

def findNearestAgentPackageVersion(projectVersion):
  if projectVersion == "":
    projectVersion = "  "
  yumCommand = ["bash", "-c", "yum list all ambari-agent | grep '" + projectVersion +\
                              "' | sed -re 's/\s+/ /g' | cut -d ' ' -f 2 | head -n1 | sed -e 's/-\w[^:]*//1' "]
  return execOsCommand(yumCommand)

def isAgentPackageAlreadyInstalled(projectVersion):
    yumCommand = ["bash", "-c", "rpm -qa | grep ambari-agent"+projectVersion]
    ret = execOsCommand(yumCommand)
    res = False
    if ret["exitstatus"] == 0 and ret["log"][0].strip() != "":
        res = True
    return res

def getAvaliableAgentPackageVersions():
    yumCommand = ["bash", "-c",
        """yum list all ambari-agent | grep -E '^ambari-agent' | sed -re 's/\s+/ /g' | cut -d ' ' -f 2 | tr '\\n' ', ' | sed -e 's/-\w[^:]*//1' """]
    return execOsCommand(yumCommand)

def getAvaliableAgentPackageVersionsSuse():
    yumCommand = ["bash", "-c",
        """zypper search -s --match-exact ambari-agent | grep ambari-agent | sed -re 's/\s+/ /g' | cut -d '|' -f 4 | tr '\\n' ', ' | sed -e 's/-\w[^:]*//1' """]
    return execOsCommand(yumCommand)

def checkServerReachability(host, port):
  ret = {}
  s = socket.socket() 
  try: 
   s.connect((host, port)) 
   return
  except Exception:
   ret["exitstatus"] = 1
   ret["log"] = "Host registration aborted. Ambari Agent host cannot reach Ambari Server '" +\
                host+":"+str(port) + "'. "+\
  		        "Please check the network connectivity between the Ambari Agent host and the Ambari Server"
   sys.exit(ret)
  pass

def main(argv=None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  # Parse the input
  onlyargs = argv[1:]
  expected_hostname = onlyargs[0]
  passPhrase = onlyargs[1]
  hostname = onlyargs[2]
  projectVersion = None
  server_port = 8080
  if len(onlyargs) > 3:
    projectVersion = onlyargs[3]
  if len(onlyargs) > 4:
    server_port = onlyargs[4]
  try:
    server_port = int(server_port)
  except (Exception), e:
    server_port = 8080

  checkServerReachability(hostname, server_port)

  if projectVersion is None or projectVersion == "null" or projectVersion == "{ambariVersion}" or projectVersion == "":
    retcode = getOptimalVersion("")
  else:
    retcode = getOptimalVersion(projectVersion)


  if retcode["exitstatus"] == 0 and retcode["log"] != None and retcode["log"] != "" and retcode["log"][0].strip() != "":
      availiableProjectVersion = "-" + retcode["log"].strip()
      if not isAgentPackageAlreadyInstalled(availiableProjectVersion):
          if is_suse():
            ret = installAgentSuse(availiableProjectVersion)
          else:
            ret = installAgent(availiableProjectVersion)
          if (not ret["exitstatus"]== 0):
            sys.exit(ret)
  elif retcode["exitstatus"] == 1 and retcode["log"][0].strip() != "":
      sys.exit({"exitstatus": 1, "log": "Desired version ("+projectVersion+") of ambari-agent package"
                                        " is not available."
                                        " Repository has following "
                                        "versions of ambari-agent:"+retcode["log"][0].strip()})
  else:
      sys.exit(retcode)

  configureAgent(hostname)
  sys.exit(runAgent(passPhrase, expected_hostname))

if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  main(sys.argv)

