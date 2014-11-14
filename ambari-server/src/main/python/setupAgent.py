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
from ambari_commons import OSCheck

AMBARI_PASSPHRASE_VAR = "AMBARI_PASSPHRASE"


def execOsCommand(osCommand, tries=1, try_sleep=0):
  for i in range(0, tries):
    if i>0:
      time.sleep(try_sleep)
    
    osStat = subprocess.Popen(osCommand, stdout=subprocess.PIPE)
    log = osStat.communicate(0)
    ret = {"exitstatus": osStat.returncode, "log": log}
    
    if ret['exitstatus'] == 0:
      break
      
  return ret


def installAgent(projectVersion):
  """ Run install and make sure the agent install alright """
  # The command doesn't work with file mask ambari-agent*.rpm, so rename it on agent host
  if OSCheck.is_suse_family():
    Command = ["zypper", "--no-gpg-checks", "install", "-y", "ambari-agent-" + projectVersion]
  elif OSCheck.is_ubuntu_family():
    # add * to end of version in case of some test releases
    Command = ["apt-get", "install", "-y", "--allow-unauthenticated", "ambari-agent=" + projectVersion + "*"]
  else:
    Command = ["yum", "-y", "install", "--nogpgcheck", "ambari-agent-" + projectVersion]
  return execOsCommand(Command, tries=3, try_sleep=10)


def configureAgent(server_hostname):
  """ Configure the agent so that it has all the configs knobs properly installed """
  osCommand = ["sed", "-i.bak", "s/hostname=localhost/hostname=" + server_hostname +
                                "/g", "/etc/ambari-agent/conf/ambari-agent.ini"]
  execOsCommand(osCommand)
  return


def runAgent(passPhrase, expected_hostname):
  os.environ[AMBARI_PASSPHRASE_VAR] = passPhrase
  agent_retcode = subprocess.call("/usr/sbin/ambari-agent restart --expected-hostname=" +
                                  expected_hostname, shell=True)
  for i in range(3):
    time.sleep(1)
    ret = execOsCommand(["tail", "-20", "/var/log/ambari-agent/ambari-agent.log"])
    if (not ret is None) and (0 == ret['exitstatus']):
      try:
        log = ret['log']
      except Exception:
        log = "Log not found"
      print log
      return agent_retcode
  return agent_retcode

def tryStopAgent():
  subprocess.call("/usr/sbin/ambari-agent stop", shell=True)

def getOptimalVersion(initialProjectVersion):
  optimalVersion = initialProjectVersion
  ret = findNearestAgentPackageVersion(optimalVersion)

  if ret["exitstatus"] == 0 and ret["log"][0].strip() != "" \
     and ret["log"][0].strip() == initialProjectVersion:
    optimalVersion = ret["log"][0].strip()
    retcode = 0
  else:
    ret = getAvaliableAgentPackageVersions()
    retcode = 1
    optimalVersion = ret["log"]

  return {"exitstatus": retcode, "log": optimalVersion}


def findNearestAgentPackageVersion(projectVersion):
  if projectVersion == "":
    projectVersion = "  "
  if OSCheck.is_suse_family():
    Command = ["bash", "-c", "zypper --no-gpg-checks -q search -s --match-exact ambari-agent | grep '" + projectVersion +
                                 "' | cut -d '|' -f 4 | head -n1 | sed -e 's/-\w[^:]*//1' "]
  elif OSCheck.is_ubuntu_family():
    if projectVersion == "  ":
      Command = ["bash", "-c", "apt-cache -q show ambari-agent |grep 'Version\:'|cut -d ' ' -f 2|tr -d '\\n'|sed -s 's/[-|~][A-Za-z0-9]*//'"]
    else:
      Command = ["bash", "-c", "apt-cache -q show ambari-agent |grep 'Version\:'|cut -d ' ' -f 2|grep '" +
               projectVersion + "'|tr -d '\\n'|sed -s 's/[-|~][A-Za-z0-9]*//'"]
  else:
    Command = ["bash", "-c", "yum -q list all ambari-agent | grep '" + projectVersion +
                              "' | sed -re 's/\s+/ /g' | cut -d ' ' -f 2 | head -n1 | sed -e 's/-\w[^:]*//1' "]
  return execOsCommand(Command)


def isAgentPackageAlreadyInstalled(projectVersion):
    if OSCheck.is_ubuntu_family():
      Command = ["bash", "-c", "dpkg-query -W -f='${Status} ${Version}\n' ambari-agent | grep -v deinstall | grep " + projectVersion]
    else:
      Command = ["bash", "-c", "rpm -qa | grep ambari-agent-"+projectVersion]
    ret = execOsCommand(Command)
    res = False
    if ret["exitstatus"] == 0 and ret["log"][0].strip() != "":
        res = True
    return res


def getAvaliableAgentPackageVersions():
  if OSCheck.is_suse_family():
    Command = ["bash", "-c",
        "zypper --no-gpg-checks -q search -s --match-exact ambari-agent | grep ambari-agent | sed -re 's/\s+/ /g' | cut -d '|' -f 4 | tr '\\n' ', ' | sed -s 's/[-|~][A-Za-z0-9]*//g'"]
  elif OSCheck.is_ubuntu_family():
    Command = ["bash", "-c",
        "apt-cache -q show ambari-agent|grep 'Version\:'|cut -d ' ' -f 2| tr '\\n' ', '|sed -s 's/[-|~][A-Za-z0-9]*//g'"]
  else:
    Command = ["bash", "-c",
        "yum -q list all ambari-agent | grep -E '^ambari-agent' | sed -re 's/\s+/ /g' | cut -d ' ' -f 2 | tr '\\n' ', ' | sed -s 's/[-|~][A-Za-z0-9]*//g'"]
  return execOsCommand(Command)


def checkServerReachability(host, port):
  ret = {}
  s = socket.socket()
  try:
   s.connect((host, port))
   return
  except Exception:
   ret["exitstatus"] = 1
   ret["log"] = "Host registration aborted. Ambari Agent host cannot reach Ambari Server '" +\
                host+":"+str(port) + "'. " +\
                "Please check the network connectivity between the Ambari Agent host and the Ambari Server"
   sys.exit(ret)
  pass


#  Command line syntax help
# IsOptional  Index     Description
#               0        Expected host name
#               1        Password
#               2        Host name
#      X        3        Project Version (Ambari)
#      X        4        Server port


def parseArguments(argv=None):
  if argv is None:  # make sure that arguments was passed
     sys.exit(1)
  args = argv[1:]  # shift path to script
  if len(args) < 3:
    sys.exit({"exitstatus": 1, "log": "Was passed not all required arguments"})

  expected_hostname = args[0]
  passPhrase = args[1]
  hostname = args[2]
  projectVersion = ""
  server_port = 8080

  if len(args) > 3:
    projectVersion = args[3]

  if len(args) > 4:
    try:
      server_port = int(args[4])
    except (Exception):
      server_port = 8080

  return expected_hostname, passPhrase, hostname, projectVersion, server_port


def main(argv=None):
  # Parse passed arguments
  expected_hostname, passPhrase, hostname,\
  projectVersion, server_port = parseArguments(argv)

  checkServerReachability(hostname, server_port)

  if projectVersion == "null" or projectVersion == "{ambariVersion}" or projectVersion == "":
    retcode = getOptimalVersion("")
  else:
    retcode = getOptimalVersion(projectVersion)

  tryStopAgent()

  if retcode["exitstatus"] == 0 and retcode["log"] != None and retcode["log"] != "" and retcode["log"][0].strip() != "":
      availiableProjectVersion = retcode["log"].strip()
      if not isAgentPackageAlreadyInstalled(availiableProjectVersion):
        ret = installAgent(availiableProjectVersion)
        if (not ret["exitstatus"] == 0):
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
