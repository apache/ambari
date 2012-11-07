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

class SCP(threading.Thread):
  """ SCP implementation that is thread based. The status can be returned using
   status val """
  def __init__(self, sshKeyFile, host, inputFile, remote):
    self.sshKeyFile = sshKeyFile
    self.host = host
    self.inputFile = inputFile
    self.remote = remote
    self.ret = {"exitstatus" : -1, "log" : "FAILED"}
    threading.Thread.__init__(self)
    pass
  
  def getStatus(self):
    return self.ret
    pass
  
  def getHost(self):
    return self.host
  
  def run(self):
    scpcommand = ["scp", "-o", "ConnectTimeout=3", "-o",
                   "StrictHostKeyChecking=no",
                  "-i", self.sshKeyFile, self.inputFile, "root@" +
                   self.host + ":" + self.remote]
    scpstat = subprocess.Popen(scpcommand, stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE)
    log = scpstat.communicate()
    self.ret["exitstatus"] = scpstat.returncode
    self.ret["log"] = log[0] + "\n" + log[1]
    pass

class SSH(threading.Thread):
  """ Ssh implementation of this """
  def __init__(self, sshKeyFile, host, commands):
    self.sshKeyFile = sshKeyFile
    self.host = host
    self.commands = commands
    self.ret = {"exitstatus" : -1, "log": "FAILED"}
    threading.Thread.__init__(self)
    pass
  
  def getHost(self):
    return self.host
  
  def getStatus(self):
    return self.ret
  
  def run(self):
    sshcommand = ["ssh", "-o", "ConnectTimeOut=3", "-o",
                   "StrictHostKeyChecking=no", "-i", self.sshKeyFile,
                    self.host, ";".join(self.commands)]
    sshstat = subprocess.Popen(sshcommand, stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE)
    log = sshstat.communicate()
    self.ret["exitstatus"] = sshstat.returncode
    self.ret["log"] = log[0] + "\n" + log[1]
    pass
pass

def splitlist(hosts, n):
  return [hosts[i:i+n] for i in range(0, len(hosts), n)]


class PSSH:
  """Run SSH in parallel for a given list of hosts"""
  def __init__(self, hosts, sshKeyFile, commands):
    self.hosts = hosts
    self.sshKeyFile = sshKeyFile
    self.commands = commands
    self.ret = {}
    pass
    
  def getstatus(self):
    return self.ret
    pass
  
  def run(self):
    """ Run 20 at a time in parallel """
    for chunk in splitlist(self.hosts, 20):
      chunkstats = []
      for host in chunk:
        ssh = SSH(self.sshKeyFile, host, self.commands)
        ssh.start()
        chunkstats.append(ssh)
        pass
      """ wait for the ssh's to complete """
      for chunkstat in chunkstats:
        chunkstat.join()
        self.ret[chunkstat.getHost()] = chunkstat.getStatus()
      pass
    pass
pass    

class PSCP:
  """Run SCP in parallel for a given list of hosts"""
  def __init__(self, hosts, sshKeyFile, inputfile, remote):
    self.hosts = hosts
    self.sshKeyFile = sshKeyFile
    self.inputfile = inputfile
    self.remote = remote
    self.ret = {}
    pass
    
  def getstatus(self):
    return self.ret
    pass
  
  def run(self):
    """ Run 20 at a time in parallel """
    for chunk in splitlist(self.hosts, 20):
      chunkstats = []
      for host in chunk:
        scp = SCP(self.sshKeyFile, host, self.inputfile, self.remote)
        scp.start()
        chunkstats.append(scp)
        pass
      """ wait for the scp's to complete """
      for chunkstat in chunkstats:
        chunkstat.join()
        self.ret[chunkstat.getHost()] = chunkstat.getStatus()
      pass
    
    pass
pass    
    
class BootStrap:
  """ BootStrapping the agents on a list of hosts"""
  def __init__(self, hosts, sshkeyFile, scriptDir, boottmpdir):
    self.hostlist = hosts
    self.sshkeyFile = sshkeyFile
    self.bootdir = boottmpdir
    self.scriptDir = scriptDir
    pass
  
  def getRepoFile(self):
    """ Ambari repo file for Ambari."""
    return "/etc/yum.repos.d/ambari.repo"
  
  def getSetupScript(self):
    return os.path.join(self.scriptDir, "setupAgent.py")
    
  def runSetupAgent(self):
    commands = ["export AMBARI_PASSPHRASE=" + os.environ[AMBARI_PASSPHRASE_VAR], "/tmp/setupAgent.py"]
    pssh = PSSH(self.hostlist, self.sshkeyFile, commands)
    pssh.run()
    out = pssh.getstatus()
    logging.info("Parallel ssh returns " + pprint.pformat(out))

    """ Test code for setting env var on agent host before starting setupAgent.py
    commands = ["export AMBARI_PASSPHRASE=" + os.environ[AMBARI_PASSPHRASE_VAR], "set"]
    pssh = PSSH(self.hostlist, self.sshkeyFile, commands)
    pssh.run()
    out = pssh.getstatus()
    logging.info("Look for AMBARI_PASSPHRASE in out " + pprint.pformat(out))
    """

  def copyNeededFiles(self):
    try:
      """Copying the files """
      fileToCopy = self.getRepoFile()
      pscp = PSCP(self.hostlist, self.sshkeyFile, fileToCopy, "/etc/yum.repos.d")
      pscp.run()
      out = pscp.getstatus()
      logging.info("Parallel scp return " + pprint.pformat(out))
    except Exception as e:
      logging.info("Traceback " + traceback.format_exc())
      pass
       
    pass
  
  def run(self):
    """ Copy files and run commands on remote hosts """
    self.copyNeededFiles()
    self.runSetupAgent()
    pass
  pass
  
  
def main(argv=None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  onlyargs = argv[1:]
  if (len(onlyargs) < 3):
    sys.stderr.write("Usage: <comma separated host> " \
      "<sshkeyFile> <tmpdir for usage>\n")
    sys.exit(2)
    pass
  """ Parse the input"""
  hostList = onlyargs[0].split(",")
  sshKeyFile = onlyargs[1]
  bootdir =  onlyargs[2]
  logging.info("BootStrapping hosts " + pprint.pformat(hostList) +
               "using " + scriptDir + 
              " with sshKey File " + sshKeyFile + " using tmp dir " + bootdir)
  bootstrap = BootStrap(hostList, sshKeyFile, scriptDir, bootdir)
  bootstrap.run()
  
if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  main(sys.argv)
