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

AMBARI_PASSPHRASE_VAR_NAME = "AMBARI_PASSPHRASE"

class SCP(threading.Thread):
  """ SCP implementation that is thread based. The status can be returned using
   status val """
  def __init__(self, sshKeyFile, host, inputFile, remote, bootdir):
    self.sshKeyFile = sshKeyFile
    self.host = host
    self.inputFile = inputFile
    self.remote = remote
    self.bootdir = bootdir
    self.ret = {"exitstatus" : -1, "log" : "FAILED"}
    threading.Thread.__init__(self)
    pass
  
  def getStatus(self):
    return self.ret
    pass
  
  def getHost(self):
    return self.host
  
  def run(self):
    scpcommand = ["scp",
                  "-o", "ConnectTimeout=60",
                  "-o", "BatchMode=yes",
                  "-o", "StrictHostKeyChecking=no",
                  "-i", self.sshKeyFile, self.inputFile, "root@" +
                   self.host + ":" + self.remote]
    logging.info("Running scp command " + ' '.join(scpcommand))
    scpstat = subprocess.Popen(scpcommand, stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE)
    log = scpstat.communicate()
    self.ret["exitstatus"] = scpstat.returncode
    self.ret["log"] = "STDOUT\n" + log[0] + "\nSTDERR\n" + log[1]
    logFilePath = os.path.join(self.bootdir, self.host + ".log")
    logFile = open(logFilePath, "a+")
    logFile.write(self.ret["log"])
    logFile.close
    logging.info("scp " + self.inputFile + " done for host " + self.host + ", exitcode=" + str(scpstat.returncode))
    pass

class SSH(threading.Thread):
  """ Ssh implementation of this """
  def __init__(self, sshKeyFile, host, command, bootdir):
    self.sshKeyFile = sshKeyFile
    self.host = host
    self.command = command
    self.bootdir = bootdir
    self.ret = {"exitstatus" : -1, "log": "FAILED"}
    threading.Thread.__init__(self)
    pass
  
  def getHost(self):
    return self.host
  
  def getStatus(self):
    return self.ret
  
  def run(self):
    sshcommand = ["ssh",
                  "-o", "ConnectTimeOut=60",
                  "-o", "StrictHostKeyChecking=no",
                  "-o", "BatchMode=yes",
                  "-tt", # Should prevent "tput: No value for $TERM and no -T specified" warning
                  "-i", self.sshKeyFile,
                  "root@" + self.host, self.command]
    logging.info("Running ssh command " + ' '.join(sshcommand))
    sshstat = subprocess.Popen(sshcommand, stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE)
    log = sshstat.communicate()
    self.ret["exitstatus"] = sshstat.returncode
    self.ret["log"] = "STDOUT\n" + log[0] + "\nSTDERR\n" + log[1]
    logFilePath = os.path.join(self.bootdir, self.host + ".log")
    logFile = open(logFilePath, "a+")
    logFile.write(self.ret["log"])
    logFile.close

    doneFilePath = os.path.join(self.bootdir, self.host + ".done")
    doneFile = open(doneFilePath, "w+")
    doneFile.write(str(sshstat.returncode))
    doneFile.close()

    logging.info("Setup agent done for host " + self.host + ", exitcode=" + str(sshstat.returncode))
    pass
pass


def splitlist(hosts, n):
  return [hosts[i:i+n] for i in range(0, len(hosts), n)]

def skip_failed_hosts(statuses):
  """ Takes a dictionary <hostname, hoststatus> and returns list of hosts whose status is SUCCESS"""
  res = list(key for key, value in statuses.iteritems() if value["exitstatus"] == 0)
  return res

def unite_statuses(statuses, update):
  """ Takes two dictionaries <hostname, hoststatus> and returns dictionary with united entries (returncode is set
  to the max value per host, logs per host are concatenated)"""
  result = {}
  for key, value in statuses.iteritems():
    if key in update:
      upd_status = update[key]
      res_status = {
        "exitstatus" : max(value["exitstatus"], upd_status["exitstatus"]),
        "log" : value["log"] + "\n" + upd_status["log"]
      }
      result[key] = res_status
    else:
      result[key] = value
  return result

def get_difference(list1, list2):
  """Takes two lists and returns list filled by elements of list1 that are absent at list2.
  Duplicates are removed too"""
  #res =
  s1 = set(list1)
  s2 = set(list2)
  return list(s1- s2)

class PSSH:
  """Run SSH in parallel for a given list of hosts"""
  def __init__(self, hosts, sshKeyFile, command, bootdir):
    self.hosts = hosts
    self.sshKeyFile = sshKeyFile
    self.command = command
    self.bootdir = bootdir
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
        ssh = SSH(self.sshKeyFile, host, self.command, self.bootdir)
        ssh.start()
        chunkstats.append(ssh)
        pass
      # wait for the ssh's to complete
      for chunkstat in chunkstats:
        chunkstat.join()
        self.ret[chunkstat.getHost()] = chunkstat.getStatus()
      pass
    pass
pass    

class PSCP:
  """Run SCP in parallel for a given list of hosts"""
  def __init__(self, hosts, sshKeyFile, inputfile, remote, bootdir):
    self.hosts = hosts
    self.sshKeyFile = sshKeyFile
    self.inputfile = inputfile
    self.remote = remote
    self.bootdir = bootdir
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
        scp = SCP(self.sshKeyFile, host, self.inputfile, self.remote, self.bootdir)
        scp.start()
        chunkstats.append(scp)
        pass
      # wait for the scp's to complete
      for chunkstat in chunkstats:
        chunkstat.join()
        self.ret[chunkstat.getHost()] = chunkstat.getStatus()
      pass
    
    pass
pass    
    
class BootStrap:
  """ BootStrapping the agents on a list of hosts"""
  def __init__(self, hosts, sshkeyFile, scriptDir, boottmpdir, setupAgentFile, ambariServer):
    self.hostlist = hosts
    self.successive_hostlist = hosts
    self.sshkeyFile = sshkeyFile
    self.bootdir = boottmpdir
    self.scriptDir = scriptDir
    self.setupAgentFile = setupAgentFile
    self.ambariServer = ambariServer
    self.statuses = None
    pass

  def is_suse(self):
    if os.path.isfile("/etc/issue"):
      if "suse" in open("/etc/issue").read().lower():
        return True
    return False

  def getRepoDir(self):
    """ Ambari repo file for Ambari."""
    if self.is_suse():
      return "/etc/zypp/repos.d"
    else:
      return "/etc/yum.repos.d"
  
  def getRepoFile(self):
    """ Ambari repo file for Ambari."""
    return os.path.join(self.getRepoDir(), "ambari.repo")

  def getSetupScript(self):
    return os.path.join(self.scriptDir, "setupAgent.py")

  def copyNeededFiles(self):
    try:
      # Copying the files
      fileToCopy = self.getRepoFile()
      targetDir = self.getRepoDir()
      pscp = PSCP(self.hostlist, self.sshkeyFile, fileToCopy, targetDir, self.bootdir)
      pscp.run()
      out = pscp.getstatus()
      # Prepearing report about failed hosts
      self.successive_hostlist = skip_failed_hosts(out)
      failed = get_difference(self.hostlist, self.successive_hostlist)
      logging.info("Parallel scp returns for repo file. Failed hosts are: " + str(failed))
      #updating statuses
      self.statuses = out

      pscp = PSCP(self.successive_hostlist, self.sshkeyFile, self.setupAgentFile, "/tmp", self.bootdir)
      pscp.run()
      out = pscp.getstatus()
      # Prepearing report about failed hosts
      failed_current = get_difference(self.successive_hostlist, skip_failed_hosts(out))
      self.successive_hostlist = skip_failed_hosts(out)
      failed = get_difference(self.hostlist, self.successive_hostlist)
      logging.info("Parallel scp returns for agent script. All failed hosts are: " + str(failed) +
                   ". Failed on last step: " + str(failed_current))
      #updating statuses
      self.statuses = unite_statuses(self.statuses, out)
      retstatus = 0
      if not failed: 
        retstatus = 0
      else:
        retstatus = 1
      return retstatus

    except Exception, e:
      logging.info("Traceback " + traceback.format_exc())
      pass

    pass

  def runSetupAgent(self):
    logging.info("Running setup agent...")
    command = "python /tmp/setupAgent.py " + os.environ[AMBARI_PASSPHRASE_VAR_NAME] + " " + self.ambariServer
    pssh = PSSH(self.successive_hostlist, self.sshkeyFile, command, self.bootdir)
    pssh.run()
    out = pssh.getstatus()

    # Prepearing report about failed hosts
    failed_current = get_difference(self.successive_hostlist, skip_failed_hosts(out))
    self.successive_hostlist = skip_failed_hosts(out)
    failed = get_difference(self.hostlist, self.successive_hostlist)
    logging.info("Parallel ssh returns for setup agent. All failed hosts are: " + str(failed) +
                 ". Failed on last step: " + str(failed_current))

    #updating statuses
    self.statuses = unite_statuses(self.statuses, out)
    retstatus = 0 
    if not failed:
      retstatus = 0
    else:
      retstatus = 1
    pass

  def createDoneFiles(self):
    """ Creates .done files for every host. These files are later read from Java code.
    If .done file for any host is not created, the bootstrap will hang or fail due to timeout"""
    for key, value in self.statuses.iteritems():
      doneFilePath = os.path.join(self.bootdir, key + ".done")
      if not os.path.exists(doneFilePath):
        doneFile = open(doneFilePath, "w+")
        doneFile.write(str(value["exitstatus"]))
        doneFile.close()
    pass

  def run(self):
    """ Copy files and run commands on remote hosts """
    ret1 = self.copyNeededFiles()
    logging.info("Copying files finished")
    ret2 = self.runSetupAgent()
    logging.info("Running ssh command finished")
    retcode = max(ret1, ret2)
    self.createDoneFiles()
    return retcode
    pass
  pass
  
  
def main(argv=None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  onlyargs = argv[1:]
  if len(onlyargs) < 3:
    sys.stderr.write("Usage: <comma separated hosts> "
                     "<tmpdir for storage> <sshkeyFile> <agent setup script> <ambari-server name>\n")
    sys.exit(2)
    pass
  #Parse the input
  hostList = onlyargs[0].split(",")
  bootdir =  onlyargs[1]
  sshKeyFile = onlyargs[2]
  setupAgentFile = onlyargs[3]
  ambariServer = onlyargs[4]

  # ssh doesn't like open files
  stat = subprocess.Popen(["chmod", "600", sshKeyFile], stdout=subprocess.PIPE)
  
  logging.info("BootStrapping hosts " + pprint.pformat(hostList) +
               "using " + scriptDir + 
              " with sshKey File " + sshKeyFile + " using tmp dir " + bootdir + " ambari: " + ambariServer)
  bootstrap = BootStrap(hostList, sshKeyFile, scriptDir, bootdir, setupAgentFile, ambariServer)
  ret = bootstrap.run()
  #return  ret
  return 0 # Hack to comply with current usage
  
if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  main(sys.argv)
