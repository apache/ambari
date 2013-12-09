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
from threading import Thread
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
# how many parallel bootstraps may be run at a time
MAX_PARALLEL_BOOTSTRAPS = 20
# How many seconds to wait between polling parallel bootstraps
POLL_INTERVAL_SEC = 1
DEBUG=False


class HostLog:
  """ Provides per-host logging. """

  def __init__(self, log_file):
    self.log_file = log_file

  def write(self, log_text):
    """
     Writes log to file. Closes file after each write to make content accessible
     for poller in ambari-server
    """
    logFile = open(self.log_file, "a+")
    text = str(log_text)
    if not text.endswith("\n"):
      text += "\n"
    logFile.write(text)
    logFile.close()


class SCP:
  """ SCP implementation that is thread based. The status can be returned using
   status val """
  def __init__(self, user, sshkey_file, host, inputFile, remote, bootdir, host_log):
    self.user = user
    self.sshkey_file = sshkey_file
    self.host = host
    self.inputFile = inputFile
    self.remote = remote
    self.bootdir = bootdir
    self.host_log = host_log
    pass


  def run(self):
    scpcommand = ["scp",
                  "-o", "ConnectTimeout=60",
                  "-o", "BatchMode=yes",
                  "-o", "StrictHostKeyChecking=no",
                  "-i", self.sshkey_file, self.inputFile, self.user + "@" +
                                                         self.host + ":" + self.remote]
    if DEBUG:
      self.host_log.write("Running scp command " + ' '.join(scpcommand))
    scpstat = subprocess.Popen(scpcommand, stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE)
    log = scpstat.communicate()
    log = "STDOUT\n" + log[0] + "\nSTDERR\n" + log[1]
    self.host_log.write(log)
    self.host_log.write("scp " + self.inputFile + " done for host " + self.host +
                 ", exitcode=" + str(scpstat.returncode))
    return scpstat.returncode



class SSH:
  """ Ssh implementation of this """
  def __init__(self, user, sshkey_file, host, command, bootdir, host_log, errorMessage = None):
    self.user = user
    self.sshkey_file = sshkey_file
    self.host = host
    self.command = command
    self.bootdir = bootdir
    self.errorMessage = errorMessage
    self.host_log = host_log
    pass


  def run(self):
    sshcommand = ["ssh",
                  "-o", "ConnectTimeOut=60",
                  "-o", "StrictHostKeyChecking=no",
                  "-o", "BatchMode=yes",
                  "-tt", # Should prevent "tput: No value for $TERM and no -T specified" warning
                  "-i", self.sshkey_file,
                  self.user + "@" + self.host, self.command]
    if DEBUG:
      self.host_log.write("Running ssh command " + ' '.join(sshcommand))
    sshstat = subprocess.Popen(sshcommand, stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE)
    log = sshstat.communicate()
    errorMsg = log[1]
    if self.errorMessage and sshstat.returncode != 0:
      errorMsg = self.errorMessage + "\n" + errorMsg
    log = "STDOUT\n" + log[0] + "\nSTDERR\n" + errorMsg
    self.host_log.write(log)

    self.host_log.write("SSH command execution finished for host " + self.host +
                 ", exitcode=" + str(sshstat.returncode))
    return sshstat.returncode



class Bootstrap(threading.Thread):
  """ Bootstrap the agent on a separate host"""
  TEMP_FOLDER = "/tmp"
  OS_CHECK_SCRIPT_FILENAME = "os_type_check.sh"
  AMBARI_REPO_FILENAME = "ambari.repo"
  SETUP_SCRIPT_FILENAME = "setupAgent.py"
  PASSWORD_FILENAME = "host_pass"

  def __init__(self, host, shared_state):
    threading.Thread.__init__(self)
    self.host = host
    self.shared_state = shared_state
    self.status = {
      "start_time": None,
      "return_code": None,
    }
    log_file = os.path.join(self.shared_state.bootdir, self.host + ".log")
    self.host_log = HostLog(log_file)
    self.daemon = True


  def getRemoteName(self, filename):
    full_name = os.path.join(self.TEMP_FOLDER, filename)
    remote_files = self.shared_state.remote_files
    if not remote_files.has_key(full_name):
      remote_files[full_name] = self.generateRandomFileName(full_name)
    return remote_files[full_name]


  def generateRandomFileName(self, filename):
    if filename is None:
      return self.getUtime()
    else:
      name, ext = os.path.splitext(filename)
      return str(name) + str(self.getUtime()) + str(ext)


  # This method is needed  to implement the descriptor protocol (make object
  # to pass self reference to mockups)
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
    return os.path.join(self.shared_state.script_dir, self.OS_CHECK_SCRIPT_FILENAME)

  def getOsCheckScriptRemoteLocation(self):
    return self.getRemoteName(self.OS_CHECK_SCRIPT_FILENAME)

  def getUtime(self):
    return int(time.time())

  def getPasswordFile(self):
    return self.getRemoteName(self.PASSWORD_FILENAME)

  def hasPassword(self):
    password_file = self.shared_state.password_file
    return password_file is not None and password_file != 'null'


  def copyOsCheckScript(self):
    # Copying the os check script file
    fileToCopy = self.getOsCheckScript()
    target = self.getOsCheckScriptRemoteLocation()
    params = self.shared_state
    scp = SCP(params.user, params.sshkey_file, self.host, fileToCopy,
              target, params.bootdir, self.host_log)
    result = scp.run()
    self.host_log.write("Copying os type check script finished")
    return result


  def getMoveRepoFileWithPasswordCommand(self, targetDir):
    return "sudo -S mv " + str(self.getRemoteName(self.AMBARI_REPO_FILENAME)) \
           + " " + os.path.join(str(targetDir), self.AMBARI_REPO_FILENAME) + \
           " < " + str(self.getPasswordFile())


  def getMoveRepoFileWithoutPasswordCommand(self, targetDir):
    return "sudo mv " + str(self.getRemoteName(self.AMBARI_REPO_FILENAME)) \
           + " " + os.path.join(str(targetDir), self.AMBARI_REPO_FILENAME)

  def getMoveRepoFileCommand(self, targetDir):
    if self.hasPassword():
      return self.getMoveRepoFileWithPasswordCommand(targetDir)
    else:
      return self.getMoveRepoFileWithoutPasswordCommand(targetDir)


  def copyNeededFiles(self):
    # Copying the files
    fileToCopy = self.getRepoFile()
    target = self.getRemoteName(self.AMBARI_REPO_FILENAME)

    self.host_log.write("Copying repo file to 'tmp' folder...")
    params = self.shared_state
    scp = SCP(params.user, params.sshkey_file, self.host, fileToCopy,
              target, params.bootdir, self.host_log)
    retcode1 = scp.run()

    # Move file to repo dir
    self.host_log.write("Moving file to repo dir...")
    targetDir = self.getRepoDir()
    command = self.getMoveRepoFileCommand(targetDir)
    ssh = SSH(params.user, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode2 = ssh.run()

    self.host_log.write("Copying setup script file...")
    fileToCopy = params.setup_agent_file
    target = self.getRemoteName(self.SETUP_SCRIPT_FILENAME)
    scp = SCP(params.user, params.sshkey_file, self.host, fileToCopy,
              target, params.bootdir, self.host_log)
    retcode3 = scp.run()

    self.host_log.write("Copying files finished")
    return max(retcode1, retcode2, retcode3)


  def getAmbariVersion(self):
    ambari_version = self.shared_state.ambari_version
    if ambari_version is None or ambari_version == "null":
      return ""
    else:
      return ambari_version

  def getAmbariPort(self):
    server_port = self.shared_state.server_port
    if server_port is None or server_port == "null":
      return "null"
    else:
      return server_port

  def getRunSetupWithPasswordCommand(self, expected_hostname):
    setupFile = self.getRemoteName(self.SETUP_SCRIPT_FILENAME)
    passphrase = os.environ[AMBARI_PASSPHRASE_VAR_NAME]
    server = self.shared_state.ambari_server
    version = self.getAmbariVersion()
    port = self.getAmbariPort()
    passwordFile = self.getPasswordFile()
    return "sudo -S python " + str(setupFile) + " " + str(expected_hostname) + \
           " " + str(passphrase) + " " + str(server) + " " + str(version) + \
           " " + str(port) + " < " + str(passwordFile)


  def getRunSetupWithoutPasswordCommand(self, expected_hostname):
    setupFile=self.getRemoteName(self.SETUP_SCRIPT_FILENAME)
    passphrase=os.environ[AMBARI_PASSPHRASE_VAR_NAME]
    server=self.shared_state.ambari_server
    version=self.getAmbariVersion()
    port=self.getAmbariPort()
    return "sudo python " + str(setupFile) + " " + str(expected_hostname) + \
           " " + str(passphrase) + " " + str(server) + " " + str(version) + \
           " " + str(port)


  def getRunSetupCommand(self, expected_hostname):
    if self.hasPassword():
      return self.getRunSetupWithPasswordCommand(expected_hostname)
    else:
      return self.getRunSetupWithoutPasswordCommand(expected_hostname)


  def runOsCheckScript(self):
    params = self.shared_state
    self.host_log.write("Running os type check...")
    command = "chmod a+x %s && %s %s" % \
              (self.getOsCheckScriptRemoteLocation(),
               self.getOsCheckScriptRemoteLocation(),  params.cluster_os_type)

    ssh = SSH(params.user, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode = ssh.run()
    self.host_log.write("Running os type check  finished")
    return retcode


  def runSetupAgent(self):
    params = self.shared_state
    self.host_log.write("Running setup agent...")
    command = self.getRunSetupCommand(self.host)
    ssh = SSH(params.user, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode = ssh.run()
    self.host_log.write("Setting up agent finished")
    return retcode


  def createDoneFile(self, retcode):
    """ Creates .done file for current host. These files are later read from Java code.
    If .done file for any host is not created, the bootstrap will hang or fail due to timeout"""
    params = self.shared_state
    doneFilePath = os.path.join(params.bootdir, self.host + ".done")
    if not os.path.exists(doneFilePath):
      doneFile = open(doneFilePath, "w+")
      doneFile.write(str(retcode))
      doneFile.close()


  def checkSudoPackage(self):
    """ Checking 'sudo' package on remote host """
    params = self.shared_state
    command = "rpm -qa | grep sudo"
    ssh = SSH(params.user, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log,
              errorMessage="Error: Sudo command is not available. " \
                           "Please install the sudo command.")
    retcode = ssh.run()
    self.host_log.write("Checking 'sudo' package finished")
    return retcode


  def copyPasswordFile(self):
    # Copy the password file
    self.host_log.write("Copying password file to 'tmp' folder...")
    params = self.shared_state
    scp = SCP(params.user, params.sshkey_file, self.host, params.password_file,
              self.getPasswordFile(), params.bootdir, self.host_log)
    retcode1 = scp.run()

    self.copied_password_file = True

    # Change password file mode to 600
    self.host_log.write("Changing password file mode...")
    command = "chmod 600 " + self.getPasswordFile()
    ssh = SSH(params.user, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode2 = ssh.run()

    self.host_log.write("Copying password file finished")
    return max(retcode1, retcode2)


  def changePasswordFileModeOnHost(self):
    # Change password file mode to 600
    self.host_log.write("Changing password file mode...")
    params = self.shared_state
    command = "chmod 600 " + self.getPasswordFile()
    ssh = SSH(params.user, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode = ssh.run()
    self.host_log.write("Change password file mode on host finished")
    return retcode


  def deletePasswordFile(self):
    # Deleting the password file
    self.host_log.write("Deleting password file...")
    params = self.shared_state
    command = "rm " + self.getPasswordFile()
    ssh = SSH(params.user, params.sshkey_file, self.host, command,
              params.bootdir, self.host_log)
    retcode = ssh.run()
    self.host_log.write("Deleting password file finished")
    return retcode

  def try_to_execute(self, action):
    try:
      last_retcode = action()
    except Exception, e:
      self.host_log.write("Traceback: " + traceback.format_exc())
      last_retcode = 177
    return last_retcode

  def run(self):
    """ Copy files and run commands on remote host """
    self.status["start_time"] = time.time()
    # Population of action queue
    action_queue = [self.copyOsCheckScript,
                    self.runOsCheckScript,
                    self.checkSudoPackage
    ]
    if self.hasPassword():
      action_queue.extend([self.copyPasswordFile,
                           self.changePasswordFileModeOnHost])
    action_queue.extend([
      self.copyNeededFiles,
      self.runSetupAgent,
    ])

    # Execution of action queue
    last_retcode = 0
    while action_queue and last_retcode == 0:
      action = action_queue.pop(0)
      last_retcode = self.try_to_execute(action)
    # Checking execution result
    if last_retcode != 0:
      message = "ERROR: Bootstrap of host {0} fails because previous action " \
        "finished with non-zero exit code ({1})".format(self.host, last_retcode)
      self.host_log.write(message)
      logging.error(message)
    # Try to delete password file
    if self.hasPassword() and self.copied_password_file:
      retcode = self.try_to_execute(self.deletePasswordFile)
      if retcode != 0:
        message = "WARNING: failed to delete password file " \
          "at {0}. Please delete it manually".format(self.getPasswordFile())
        self.host_log.write(message)
        logging.warn(message)

    self.createDoneFile(last_retcode)
    self.status["return_code"] = last_retcode



  def getStatus(self):
    return self.status

  def interruptBootstrap(self):
    """
    Thread is not really interrupted (moreover, Python seems to have no any
    stable/portable/official api to do that: _Thread__stop only marks thread
    as stopped). The bootstrap thread is marked as a daemon at init, and will
    exit when the main parallel bootstrap thread exits.
    All we need to do now is a proper logging and creating .done file
    """
    self.host_log.write("Bootstrap timed out")
    self.createDoneFile(199)



class PBootstrap:
  """ BootStrapping the agents on a list of hosts"""
  def __init__(self, hosts, sharedState):
    self.hostlist = hosts
    self.sharedState = sharedState
    pass

  def run_bootstrap(self, host):
    bootstrap = Bootstrap(host, self.sharedState)
    bootstrap.start()
    return bootstrap

  def run(self):
    """ Run up to MAX_PARALLEL_BOOTSTRAPS at a time in parallel """
    logging.info("Executing parallel bootstrap")
    queue = list(self.hostlist)
    queue.reverse()
    running_list = []
    finished_list = []
    while queue or running_list: # until queue is not empty or not all parallel bootstraps are
      # poll running bootstraps
      for bootstrap in running_list:
        if bootstrap.getStatus()["return_code"] is not None:
          finished_list.append(bootstrap)
        else:
          starttime = bootstrap.getStatus()["start_time"]
          elapsedtime = time.time() - starttime
          if elapsedtime > HOST_BOOTSTRAP_TIMEOUT:
            # bootstrap timed out
            logging.warn("Bootstrap at host {0} timed out and will be "
                            "interrupted".format(bootstrap.host))
            bootstrap.interruptBootstrap()
            finished_list.append(bootstrap)
      # Remove finished from the running list
      running_list[:] = [b for b in running_list if not b in finished_list]
      # Start new bootstraps from the queue
      free_slots = MAX_PARALLEL_BOOTSTRAPS - len(running_list)
      for i in range(free_slots):
        if queue:
          next_host = queue.pop()
          bootstrap = self.run_bootstrap(next_host)
          running_list.append(bootstrap)
      time.sleep(POLL_INTERVAL_SEC)
    logging.info("Finished parallel bootstrap")



class SharedState:
  def __init__(self, user, sshkey_file, script_dir, boottmpdir, setup_agent_file,
               ambari_server, cluster_os_type, ambari_version, server_port,
               password_file = None):
    self.hostlist_to_remove_password_file = None
    self.user = user
    self.sshkey_file = sshkey_file
    self.bootdir = boottmpdir
    self.script_dir = script_dir
    self.setup_agent_file = setup_agent_file
    self.ambari_server = ambari_server
    self.cluster_os_type = cluster_os_type
    self.ambari_version = ambari_version
    self.password_file = password_file
    self.statuses = None
    self.server_port = server_port
    self.remote_files = {}
    self.ret = {}
    pass


def main(argv=None):
  scriptDir = os.path.realpath(os.path.dirname(argv[0]))
  onlyargs = argv[1:]
  if len(onlyargs) < 3:
    sys.stderr.write("Usage: <comma separated hosts> "
                     "<tmpdir for storage> <user> <sshkey_file> <agent setup script>"
                     " <ambari-server name> <cluster os type> <ambari version> <ambari port> <passwordFile>\n")
    sys.exit(2)
    pass
  #Parse the input
  hostList = onlyargs[0].split(",")
  bootdir =  onlyargs[1]
  user = onlyargs[2]
  sshkey_file = onlyargs[3]
  setupAgentFile = onlyargs[4]
  ambariServer = onlyargs[5]
  cluster_os_type = onlyargs[6]
  ambariVersion = onlyargs[7]
  server_port = onlyargs[8]
  passwordFile = onlyargs[9]

  # ssh doesn't like open files
  subprocess.Popen(["chmod", "600", sshkey_file], stdout=subprocess.PIPE)

  if passwordFile is not None and passwordFile != 'null':
    subprocess.Popen(["chmod", "600", passwordFile], stdout=subprocess.PIPE)
  
  logging.info("BootStrapping hosts " + pprint.pformat(hostList) +
               " using " + scriptDir + " cluster primary OS: " + cluster_os_type +
               " with user '" + user + "' sshKey File " + sshkey_file + " password File " + passwordFile +\
               " using tmp dir " + bootdir + " ambari: " + ambariServer +"; server_port: " + server_port +\
               "; ambari version: " + ambariVersion)
  sharedState = SharedState(user, sshkey_file, scriptDir, bootdir, setupAgentFile,
                       ambariServer, cluster_os_type, ambariVersion,
                       server_port, passwordFile)
  pbootstrap = PBootstrap(hostList, sharedState)
  pbootstrap.run()
  return 0 # Hack to comply with current usage

if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  main(sys.argv)
