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
HOST_BOOTSTRAP_TIMEOUT = 300

class SCP(threading.Thread):
  """ SCP implementation that is thread based. The status can be returned using
   status val """
  def __init__(self, user, sshKeyFile, host, inputFile, remote, bootdir):
    self.user = user
    self.sshKeyFile = sshKeyFile
    self.host = host
    self.inputFile = inputFile
    self.remote = remote
    self.bootdir = bootdir
    self.ret = {"exitstatus" : -1, "log" : "FAILED"}
    threading.Thread.__init__(self)
    self.daemon = True
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
                  "-i", self.sshKeyFile, self.inputFile, self.user + "@" +
                   self.host + ":" + self.remote]
    logging.info("Running scp command " + ' '.join(scpcommand))
    scpstat = subprocess.Popen(scpcommand, stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE)
    log = scpstat.communicate()
    self.ret["exitstatus"] = scpstat.returncode
    self.ret["log"] = "STDOUT\n" + log[0] + "\nSTDERR\n" + log[1]
    logFilePath = os.path.join(self.bootdir, self.host + ".log")
    self.writeLogToFile(logFilePath)
    logging.info("scp " + self.inputFile + " done for host " + self.host + ", exitcode=" + str(scpstat.returncode))
    pass

  def writeLogToFile(self, logFilePath):
    logFile = open(logFilePath, "a+")
    logFile.write(self.ret["log"])
    logFile.close
    pass

class SSH(threading.Thread):
  """ Ssh implementation of this """
  def __init__(self, user, sshKeyFile, host, command, bootdir, errorMessage = None):
    self.user = user
    self.sshKeyFile = sshKeyFile
    self.host = host
    self.command = command
    self.bootdir = bootdir
    self.errorMessage = errorMessage
    self.ret = {"exitstatus" : -1, "log": "FAILED"}
    threading.Thread.__init__(self)
    self.daemon = True
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
                  self.user + "@" + self.host, self.command]
    logging.info("Running ssh command " + ' '.join(sshcommand))
    sshstat = subprocess.Popen(sshcommand, stdout=subprocess.PIPE,
                                  stderr=subprocess.PIPE)
    log = sshstat.communicate()
    self.ret["exitstatus"] = sshstat.returncode
    errorMsg = log[1]
    if self.errorMessage and sshstat.returncode != 0:
      errorMsg = self.errorMessage + "\n" + errorMsg
    self.ret["log"] = "STDOUT\n" + log[0] + "\nSTDERR\n" + errorMsg
    logFilePath = os.path.join(self.bootdir, self.host + ".log")
    self.writeLogToFile(logFilePath)

    logging.info("SSH command execution finished for host " + self.host + ", exitcode=" + str(sshstat.returncode))
    pass

  def writeLogToFile(self, logFilePath):
    logFile = open(logFilePath, "a+")
    logFile.write(self.ret["log"])
    logFile.close
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
  def __init__(self, hosts, user, sshKeyFile, bootdir, errorMessage = None, command=None, perHostCommands=None):
    '''
      Executes some command on all hosts via ssh. If command is equal for all
      hosts, it should be passed as a "command" argument to PSSH constructor.
      If command differs for different hosts, it should be passed as a
      "perHostCommands" argument to PSSH constructor. "perHostCommands" is
      expected to be a dictionary "hostname" -> "command", containing as many
      entries as "hosts" list contains.
    '''

    # Checking arguments
    # Had to include this check because wrong usage may be hard to notice without
    # multinode cluster
    if ((command is None) == (perHostCommands is None) or  # No any or both arguments are defined
          (not isinstance(command, basestring)) and # "command" argument is not a string
            (not isinstance(perHostCommands, dict) # "perHostCommands" argument is not a dictionary
              or len(perHostCommands) != len(hosts))): # or does not contain commands for all hosts
      raise "PSSH constructor received invalid parameters. Please " \
            "read PSSH constructor docstring"
    self.hosts = hosts
    self.user = user
    self.sshKeyFile = sshKeyFile

    self.command = command
    self.perHostCommands = perHostCommands
    self.bootdir = bootdir
    self.errorMessage = errorMessage
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
        if self.command is not None:
          ssh = SSH(self.user, self.sshKeyFile, host, self.command, self.bootdir, self.errorMessage)
        else:
          ssh = SSH(self.user, self.sshKeyFile, host, self.perHostCommands[host], self.bootdir, self.errorMessage)
        ssh.start()
        chunkstats.append(ssh)
        pass
      # wait for the ssh's to complete
      starttime = time.time()
      for chunkstat in chunkstats:
        elapsedtime = time.time() - starttime
        if elapsedtime < HOST_BOOTSTRAP_TIMEOUT:
          timeout = HOST_BOOTSTRAP_TIMEOUT - elapsedtime
        else:
          timeout = 0.0
        chunkstat.join(timeout)
        self.ret[chunkstat.getHost()] = chunkstat.getStatus()
      pass
    pass
pass    

class PSCP:
  """Run SCP in parallel for a given list of hosts"""
  def __init__(self, hosts, user, sshKeyFile, inputfile, remote, bootdir):
    self.hosts = hosts
    self.user = user
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
        scp = SCP(self.user, self.sshKeyFile, host, self.inputfile, self.remote, self.bootdir)
        scp.start()
        chunkstats.append(scp)
        pass
      # wait for the scp's to complete
      starttime = time.time()
      for chunkstat in chunkstats:
        elapsedtime = time.time() - starttime
        if elapsedtime < HOST_BOOTSTRAP_TIMEOUT:
          timeout = HOST_BOOTSTRAP_TIMEOUT - elapsedtime
        else:
          timeout = 0.0
        chunkstat.join(timeout)
        self.ret[chunkstat.getHost()] = chunkstat.getStatus()
      pass
    pass
pass    
    
class BootStrap:
  TEMP_FOLDER = "/tmp"
  OS_CHECK_SCRIPT_FILENAME = "os_type_check.sh"
  AMBARI_REPO_FILENAME = "ambari.repo"
  SETUP_SCRIPT_FILENAME = "setupAgent.py"
  PASSWORD_FILENAME = "host_pass"

  """ BootStrapping the agents on a list of hosts"""
  def __init__(self, hosts, user, sshkeyFile, scriptDir, boottmpdir, setupAgentFile, ambariServer, cluster_os_type,\
               ambariVersion, server_port, passwordFile = None):
    self.hostlist = hosts
    self.successive_hostlist = hosts
    self.hostlist_to_remove_password_file = None
    self.user = user
    self.sshkeyFile = sshkeyFile
    self.bootdir = boottmpdir
    self.scriptDir = scriptDir
    self.setupAgentFile = setupAgentFile
    self.ambariServer = ambariServer
    self.cluster_os_type = cluster_os_type
    self.ambariVersion = ambariVersion
    self.passwordFile = passwordFile
    self.statuses = None
    self.server_port = server_port
    self.remote_files = {}
    pass

  def getRemoteName(self, filename):
    full_name = os.path.join(self.TEMP_FOLDER, filename)
    if not self.remote_files.has_key(full_name):
      self.remote_files[full_name] = self.generateRandomFileName(full_name)

    return self.remote_files[full_name]

  def generateRandomFileName(self, filename):
    if filename is None:
      return self.getUtime()
    else:
      name, ext = os.path.splitext(filename)
      return str(name) + str(self.getUtime()) + str(ext)

  # This method is needed  to implement the descriptor protocol (make object  to pass self reference to mockups)
  def __get__(self, obj, objtype):
    def _call(*args, **kwargs):
      self(obj, *args, **kwargs)
    return _call

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
    return os.path.join(self.getRepoDir(), self.AMBARI_REPO_FILENAME)

  def getOsCheckScript(self):
    return os.path.join(self.scriptDir, self.OS_CHECK_SCRIPT_FILENAME)

  def getOsCheckScriptRemoteLocation(self):
    return self.getRemoteName(self.OS_CHECK_SCRIPT_FILENAME)

  def getUtime(self):
    return int(time.time())

  def getPasswordFile(self):
    return self.getRemoteName(self.PASSWORD_FILENAME)

  def hasPassword(self):
    return self.passwordFile is not None and self.passwordFile != 'null'


  def copyOsCheckScript(self):
    try:
      # Copying the os check script file
      fileToCopy = self.getOsCheckScript()
      target = self.getOsCheckScriptRemoteLocation()
      pscp = PSCP(self.successive_hostlist, self.user, self.sshkeyFile, fileToCopy, target, self.bootdir)
      pscp.run()
      out = pscp.getstatus()
      # Preparing report about failed hosts
      self.successive_hostlist = skip_failed_hosts(out)
      failed = get_difference(self.hostlist, self.successive_hostlist)
      logging.info("Parallel scp returns for os type check script. Failed hosts are: " + str(failed))
      #updating statuses
      self.statuses = out

      if not failed:
        retstatus = 0
      else:
        retstatus = 1
      return retstatus

    except Exception, e:
      logging.info("Traceback " + traceback.format_exc())
      pass

    pass

  def copyNeededFiles(self):
    try:
      # Copying the files
      fileToCopy = self.getRepoFile()
      target = self.getRepoFile()
      logging.info("Copying repo file to 'tmp' folder...")
      pscp = PSCP(self.successive_hostlist, self.user, self.sshkeyFile, fileToCopy, target, self.bootdir)
      pscp.run()
      out = pscp.getstatus()
      # Preparing report about failed hosts
      failed_current = get_difference(self.successive_hostlist, skip_failed_hosts(out))
      self.successive_hostlist = skip_failed_hosts(out)
      failed = get_difference(self.hostlist, self.successive_hostlist)
      logging.info("Parallel scp returns for copying repo file. All failed hosts are: " + str(failed) +
                   ". Failed on last step: " + str(failed_current))
      #updating statuses
      self.statuses = unite_statuses(self.statuses, out)

      logging.info("Moving repo file...")

      target = self.getRemoteName(self.SETUP_SCRIPT_FILENAME)
      pscp = PSCP(self.successive_hostlist, self.user, self.sshkeyFile, self.setupAgentFile, target, self.bootdir)
      pscp.run()
      out = pscp.getstatus()
      # Preparing report about failed hosts
      failed_current = get_difference(self.successive_hostlist, skip_failed_hosts(out))
      self.successive_hostlist = skip_failed_hosts(out)
      failed = get_difference(self.hostlist, self.successive_hostlist)
      logging.info("Parallel scp returns for agent script. All failed hosts are: " + str(failed) +
                   ". Failed on last step: " + str(failed_current))
      #updating statuses
      self.statuses = unite_statuses(self.statuses, out)

      if not failed:
        retstatus = 0
      else:
        retstatus = 1
      return retstatus

    except Exception, e:
      logging.info("Traceback " + traceback.format_exc())
      pass

    pass

  def getAmbariVersion(self):
    if self.ambariVersion is None or self.ambariVersion == "null":
      return ""
    else:
      return self.ambariVersion

  def getAmbariPort(self):
    if self.server_port is None or self.server_port == "null":
      return "null"
    else:
      return self.server_port    
    
  def getRunSetupWithPasswordCommand(self, expected_hostname):
    setupFile = self.getRemoteName(self.SETUP_SCRIPT_FILENAME)
    passphrase = os.environ[AMBARI_PASSPHRASE_VAR_NAME]
    server = self.ambariServer
    version = self.getAmbariVersion()
    port = self.getAmbariPort()
    passwordFile = self.getPasswordFile()
    return "sudo -S python " + str(setupFile) + " " + str(expected_hostname) +\
           " " + str(passphrase) + " " + str(server) + " " + str(version) +\
           " " + str(port) + " < " + str(passwordFile)

  def getRunSetupWithoutPasswordCommand(self, expected_hostname):
    setupFile=self.getRemoteName(self.SETUP_SCRIPT_FILENAME)
    passphrase=os.environ[AMBARI_PASSPHRASE_VAR_NAME]
    server=self.ambariServer
    version=self.getAmbariVersion()
    port=self.getAmbariPort()

    return "sudo python " + str(setupFile) + " " + str(expected_hostname) +\
           " " + str(passphrase) + " " + str(server) + " " + str(version) +\
           " " + str(port)

  def getRunSetupCommand(self, expected_hostname):
    if self.hasPassword():
      return self.getRunSetupWithPasswordCommand(expected_hostname)
    else:
      return self.getRunSetupWithoutPasswordCommand(expected_hostname)

  def runOsCheckScript(self):
    logging.info("Running os type check...")
    command = "chmod a+x %s && %s %s" % \
           (self.getOsCheckScriptRemoteLocation(),
            self.getOsCheckScriptRemoteLocation(),  self.cluster_os_type)

    pssh = PSSH(self.successive_hostlist, self.user, self.sshkeyFile, self.bootdir, command=command)
    pssh.run()
    out = pssh.getstatus()

    # Preparing report about failed hosts
    failed_current = get_difference(self.successive_hostlist, skip_failed_hosts(out))
    self.successive_hostlist = skip_failed_hosts(out)
    failed = get_difference(self.hostlist, self.successive_hostlist)
    logging.info("Parallel ssh returns for OS check. All failed hosts are: " + str(failed) +
                 ". Failed on last step: " + str(failed_current))

    #updating statuses
    self.statuses = unite_statuses(self.statuses, out)

    if not failed:
      retstatus = 0
    else:
      retstatus = 1
    return retstatus

  def runSetupAgent(self):
    logging.info("Running setup agent...")
    perHostCommands = {}
    for expected_hostname in self.successive_hostlist:
      perHostCommands[expected_hostname] = self.getRunSetupCommand(expected_hostname)
    pssh = PSSH(self.successive_hostlist, self.user, self.sshkeyFile, self.bootdir, perHostCommands=perHostCommands)
    pssh.run()
    out = pssh.getstatus()

    # Preparing report about failed hosts
    failed_current = get_difference(self.successive_hostlist, skip_failed_hosts(out))
    self.successive_hostlist = skip_failed_hosts(out)
    failed = get_difference(self.hostlist, self.successive_hostlist)
    logging.info("Parallel ssh returns for setup agent. All failed hosts are: " + str(failed) +
                 ". Failed on last step: " + str(failed_current))

    #updating statuses
    self.statuses = unite_statuses(self.statuses, out)

    if not failed:
      retstatus = 0
    else:
      retstatus = 1
    return retstatus

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

  def checkSudoPackage(self):
    try:
      """ Checking 'sudo' package on remote hosts """
      command = "rpm -qa | grep sudo"
      pssh = PSSH(self.successive_hostlist, self.user, self.sshkeyFile, self.bootdir,\
                  errorMessage="Error: Sudo command is not available. Please install the sudo command.",\
                  command=command)
      pssh.run()
      out = pssh.getstatus()
      # Preparing report about failed hosts
      failed_current = get_difference(self.successive_hostlist, skip_failed_hosts(out))
      self.successive_hostlist = skip_failed_hosts(out)
      failed = get_difference(self.hostlist, self.successive_hostlist)
      logging.info("Parallel ssh returns for checking 'sudo' package. All failed hosts are: " + str(failed) +
                   ". Failed on last step: " + str(failed_current))
      #updating statuses
      self.statuses = unite_statuses(self.statuses, out)

      if not failed:
        retstatus = 0
      else:
        retstatus = 1
      return retstatus

    except Exception, e:
      logging.info("Traceback " + traceback.format_exc())
      pass
    pass

  def copyPasswordFile(self):
    try:
      # Copying the password file
      logging.info("Copying password file to 'tmp' folder...")
      pscp = PSCP(self.successive_hostlist, self.user, self.sshkeyFile, self.passwordFile, self.getPasswordFile(), self.bootdir)
      pscp.run()
      out = pscp.getstatus()
      # Preparing report about failed hosts
      failed_current = get_difference(self.successive_hostlist, skip_failed_hosts(out))
      self.successive_hostlist = skip_failed_hosts(out)
      self.hostlist_to_remove_password_file = self.successive_hostlist
      failed = get_difference(self.hostlist, self.successive_hostlist)
      logging.warn("Parallel scp returns for copying password file. All failed hosts are: " + str(failed) +
                   ". Failed on last step: " + str(failed_current))
      #updating statuses
      self.statuses = unite_statuses(self.statuses, out)

      # Change password file mode to 600
      logging.info("Changing password file mode...")
      targetDir = self.getRepoDir()
      command = "chmod 600 " + self.getPasswordFile()
      pssh = PSSH(self.successive_hostlist, self.user, self.sshkeyFile, self.bootdir, command=command)
      pssh.run()
      out = pssh.getstatus()
      # Preparing report about failed hosts
      failed_current = get_difference(self.successive_hostlist, skip_failed_hosts(out))
      self.successive_hostlist = skip_failed_hosts(out)
      failed = get_difference(self.hostlist, self.successive_hostlist)
      logging.warning("Parallel scp returns for copying password file. All failed hosts are: " + str(failed) +
                   ". Failed on last step: " + str(failed_current))
      #updating statuses
      self.statuses = unite_statuses(self.statuses, out)

      if not failed:
        retstatus = 0
      else:
        retstatus = 1
      return retstatus

    except Exception, e:
      logging.info("Traceback " + traceback.format_exc())
      return 1

  def changePasswordFileModeOnHost(self):
    try:
      # Change password file mode to 600
      logging.info("Changing password file mode...")
      targetDir = self.getRepoDir()
      command = "chmod 600 " + self.getPasswordFile()
      pssh = PSSH(self.successive_hostlist, self.user, self.sshkeyFile, self.bootdir, command=command)
      pssh.run()
      out = pssh.getstatus()
      # Preparing report about failed hosts
      failed_current = get_difference(self.successive_hostlist, skip_failed_hosts(out))
      self.successive_hostlist = skip_failed_hosts(out)
      failed = get_difference(self.hostlist, self.successive_hostlist)
      logging.warning("Parallel scp returns for copying password file. All failed hosts are: " + str(failed) +
                      ". Failed on last step: " + str(failed_current))
      #updating statuses
      self.statuses = unite_statuses(self.statuses, out)

      if not failed:
        retstatus = 0
      else:
        retstatus = 1
      return retstatus

    except Exception, e:
      logging.info("Traceback " + traceback.format_exc())
      return 1

  def deletePasswordFile(self):
    try:
      # Deleting the password file
      logging.info("Deleting password file...")
      targetDir = self.getRepoDir()
      command = "rm " + self.getPasswordFile()
      pssh = PSSH(self.hostlist_to_remove_password_file, self.user, self.sshkeyFile, self.bootdir, command=command)
      pssh.run()
      out = pssh.getstatus()
      # Preparing report about failed hosts
      failed_current = get_difference(self.hostlist_to_remove_password_file, skip_failed_hosts(out))
      self.successive_hostlist = skip_failed_hosts(out)
      failed = get_difference(self.hostlist, self.successive_hostlist)
      logging.warn("Parallel scp returns for deleting password file. All failed hosts are: " + str(failed) +
                   ". Failed on last step: " + str(failed_current))
      #updating statuses
      self.statuses = unite_statuses(self.statuses, out)

      if not failed:
        retstatus = 0
      else:
        retstatus = 1
      return retstatus

    except Exception, e:
      logging.info("Traceback " + traceback.format_exc())
      return 1

  def run(self):
    """ Copyfiles and run commands on remote hosts """
    ret1 = self.copyOsCheckScript()
    logging.info("Copying os type check script finished")
    ret2 = self.runOsCheckScript()
    logging.info("Running os type check  finished")
    ret3 = self.checkSudoPackage()
    logging.info("Checking 'sudo' package finished")
    ret4 = 0
    ret5 = 0
    if self.hasPassword():
      ret4 = self.copyPasswordFile()
      logging.info("Copying password file finished")
      ret5 = self.changePasswordFileModeOnHost()
      logging.info("Change password file mode on host finished")
    ret6 = self.copyNeededFiles()
    logging.info("Copying files finished")
    ret7 = self.runSetupAgent()
    logging.info("Setting up agent finished")
    ret8 = 0
    if self.hasPassword() and self.hostlist_to_remove_password_file is not None:
      ret8 = self.deletePasswordFile()
      logging.info("Deleting password file finished")
    retcode = max(ret1, ret2, ret3, ret4, ret5, ret6, ret7, ret8)
    self.createDoneFiles()
    return retcode


def main(argv=None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  onlyargs = argv[1:]
  if len(onlyargs) < 3:
    sys.stderr.write("Usage: <comma separated hosts> "
                     "<tmpdir for storage> <user> <sshkeyFile> <agent setup script>"
                     " <ambari-server name> <cluster os type> <ambari version> <ambari port> <passwordFile>\n")
    sys.exit(2)
    pass
  #Parse the input
  hostList = onlyargs[0].split(",")
  bootdir =  onlyargs[1]
  user = onlyargs[2]
  sshKeyFile = onlyargs[3]
  setupAgentFile = onlyargs[4]
  ambariServer = onlyargs[5]
  cluster_os_type = onlyargs[6]
  ambariVersion = onlyargs[7]
  server_port = onlyargs[8]
  passwordFile = onlyargs[9]

  # ssh doesn't like open files
  stat = subprocess.Popen(["chmod", "600", sshKeyFile], stdout=subprocess.PIPE)

  if passwordFile != None and passwordFile != 'null':
    stat = subprocess.Popen(["chmod", "600", passwordFile], stdout=subprocess.PIPE)
  
  logging.info("BootStrapping hosts " + pprint.pformat(hostList) +
               "using " + scriptDir + " cluster primary OS: " + cluster_os_type +
               " with user '" + user + "' sshKey File " + sshKeyFile + " password File " + passwordFile +\
               " using tmp dir " + bootdir + " ambari: " + ambariServer +"; server_port: " + server_port +\
               "; ambari version: " + ambariVersion)
  bootstrap = BootStrap(hostList, user, sshKeyFile, scriptDir, bootdir, setupAgentFile,\
                        ambariServer, cluster_os_type, ambariVersion, server_port, passwordFile)
  ret = bootstrap.run()
  #return  ret
  return 0 # Hack to comply with current usage
  
if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  main(sys.argv)
