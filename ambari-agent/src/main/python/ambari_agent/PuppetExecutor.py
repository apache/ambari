#!/usr/bin/env python2.6

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
import json
import os.path
import logging
import subprocess
import pprint
import threading
from threading import Thread

from shell import shellRunner
import manifestGenerator
from RepoInstaller import RepoInstaller
from Grep import Grep
import shell

JAVANOTVALID_MSG = "Cannot access JDK! Make sure you have permission to execute {0}/bin/java"

logger = logging.getLogger()

class PuppetExecutor:

  """ Class that executes the commands that come from the server using puppet.
  This is the class that provides the pluggable point for executing the puppet"""

  grep = Grep()
  NO_ERROR = "none"

  def __init__(self, puppetModule, puppetInstall, facterInstall, tmpDir, config):
    self.puppetModule = puppetModule
    self.puppetInstall = puppetInstall
    self.facterInstall = facterInstall
    self.tmpDir = tmpDir
    self.reposInstalled = False
    self.config = config
    self.modulesdir = self.puppetModule + "/modules"
    self.event = threading.Event()
    self.last_puppet_has_been_killed = False
    self.sh = shellRunner()
    self.puppet_timeout = config.get("puppet", "timeout_seconds")

  def configureEnviron(self, environ):
    if not self.config.has_option("puppet", "ruby_home"):
      return environ
    ruby_home = self.config.get("puppet", "ruby_home")
    if os.path.exists(ruby_home):
      """Only update ruby home if the config is configured"""
      path = os.environ["PATH"]
      if not ruby_home in path:
        environ["PATH"] = ruby_home + os.path.sep + "bin"  + ":"+environ["PATH"] 
      environ["MY_RUBY_HOME"] = ruby_home
    return environ
    
  def getPuppetBinary(self):
    puppetbin = os.path.join(self.puppetInstall, "bin", "puppet") 
    if os.path.exists(puppetbin):
      return puppetbin
    else:
      logger.info("Using default puppet on the host : " + puppetbin 
                  + " does not exist.")
      return "puppet"

  def discardInstalledRepos(self):
    """
    Makes agent to forget about installed repos.
    So the next call of generate_repo_manifests() will definitely
    install repos again
    """
    self.reposInstalled = False

  def generate_repo_manifests(self, command, tmpDir, modulesdir, taskId):
    # Hack to only create the repo files once
    manifest_list = []
    if not self.reposInstalled:
      repoInstaller = RepoInstaller(command, tmpDir, modulesdir, taskId, self.config)
      manifest_list = repoInstaller.generate_repo_manifests()
    return manifest_list

  def puppetCommand(self, sitepp):
    modules = self.puppetModule
    puppetcommand = [self.getPuppetBinary(), "apply", "--confdir=" + modules, "--detailed-exitcodes", sitepp]
    return puppetcommand
  
  def facterLib(self):
    return self.facterInstall + "/lib/"
    pass
  
  def puppetLib(self):
    return self.puppetInstall + "/lib"
    pass

  def condenseOutput(self, stdout, stderr, retcode):
    grep = self.grep
    if stderr == self.NO_ERROR:
      result = grep.tail(stdout, grep.OUTPUT_LAST_LINES)
    else:
      result = grep.grep(stdout, "fail", grep.ERROR_LAST_LINES_BEFORE, grep.ERROR_LAST_LINES_AFTER)
      result = grep.cleanByTemplate(result, "warning")
      if result is None: # Second try
       result = grep.grep(stdout, "err", grep.ERROR_LAST_LINES_BEFORE, grep.ERROR_LAST_LINES_AFTER)
       result = grep.cleanByTemplate(result, "warning")
    filteredresult = grep.filterMarkup(result)
    return filteredresult

  def isSuccessfull(self, returncode):
    return not self.last_puppet_has_been_killed and (returncode == 0 or returncode == 2)

  def run_manifest(self, command, file, tmpoutfile, tmperrfile):
    result = {}
    taskId = 0
    if command.has_key("taskId"):
      taskId = command['taskId']
    puppetEnv = os.environ
    #Install repos
    repo_manifest_list = self.generate_repo_manifests(command, self.tmpDir, self.modulesdir, taskId)
    puppetFiles = list(repo_manifest_list)
    puppetFiles.append(file)
    #Run all puppet commands, from manifest generator and for repos installation
    #Appending outputs and errors, exitcode - maximal from all
    for puppetFile in puppetFiles:
      self.runPuppetFile(puppetFile, result, puppetEnv, tmpoutfile, tmperrfile)
      # Check if one of the puppet command fails and error out
      if not self.isSuccessfull(result["exitcode"]):
        break

    if self.isSuccessfull(result["exitcode"]):
      # Check if all the repos were installed or not and reset the flag
      self.reposInstalled = True

    logger.info("ExitCode : "  + str(result["exitcode"]))
    return result
  
  def isJavaAvailable(self, command):
    javaExecutablePath = "{0}/bin/java".format(command)
    return not self.sh.run([javaExecutablePath, '-version'])['exitCode']

  def runCommand(self, command, tmpoutfile, tmperrfile):
    # After installing we must have jdk available for start/stop/smoke
    if command['roleCommand'] != "INSTALL":
      java64_home = None
      if ('global' in command['configurations']) and ('java64_home' in command['configurations']['global']):
        java64_home = str(command['configurations']['global']['java64_home']).strip()
      if java64_home is None or not self.isJavaAvailable(java64_home):
        if java64_home is None:
          errMsg = "Cannot access JDK! Make sure java64_home is specified in global config"
        else:
          errMsg = JAVANOTVALID_MSG.format(java64_home)
        return {'stdout': '', 'stderr': errMsg, 'exitcode': 1}
      pass
    pass

    taskId = 0
    if command.has_key("taskId"):
      taskId = command['taskId']
    siteppFileName = os.path.join(self.tmpDir, "site-" + str(taskId) + ".pp")
    errMsg = manifestGenerator.generateManifest(command, siteppFileName,
                                                self.modulesdir, self.config)
    if not errMsg:
      result = self.run_manifest(command, siteppFileName, tmpoutfile, tmperrfile)
    else:
      result = {'stdout': '', 'stderr': errMsg, 'exitcode': 1}
    return result

  def runPuppetFile(self, puppetFile, result, puppetEnv, tmpoutfile, tmperrfile):
    """ Run the command and make sure the output gets propagated"""
    puppetcommand = self.puppetCommand(puppetFile)
    rubyLib = ""
    if os.environ.has_key("RUBYLIB"):
      rubyLib = os.environ["RUBYLIB"]
      logger.debug("RUBYLIB from Env " + rubyLib)
    if not (self.facterLib() in rubyLib):
      rubyLib = rubyLib + ":" + self.facterLib()
    if not (self.puppetLib() in rubyLib):
      rubyLib = rubyLib + ":" + self.puppetLib()
    tmpout =  open(tmpoutfile, 'w')
    tmperr =  open(tmperrfile, 'w')
    puppetEnv["RUBYLIB"] = rubyLib
    puppetEnv = self.configureEnviron(puppetEnv)
    logger.debug("Setting RUBYLIB as: " + rubyLib)
    logger.info("Running command " + pprint.pformat(puppetcommand))
    puppet = self.lauch_puppet_subprocess(puppetcommand, tmpout, tmperr, puppetEnv)
    logger.info("Command started with PID: " + str(puppet.pid))
    logger.debug("Launching watchdog thread")
    self.event.clear()
    self.last_puppet_has_been_killed = False
    thread = Thread(target =  self.puppet_watchdog_func, args = (puppet, ))
    thread.start()
    # Waiting for process to finished or killed
    puppet.communicate()
    self.event.set()
    thread.join()
    # Building results
    error = self.NO_ERROR
    returncode = 0
    if not self.isSuccessfull(puppet.returncode):
      returncode = puppet.returncode
      error = open(tmperrfile, 'r').read()
      logging.error("Error running puppet: \n" + str(error))
      pass
    if self.last_puppet_has_been_killed:
      error = str(error) + "\n Puppet has been killed due to timeout"
      returncode = 999
    if result.has_key("stderr"):
      result["stderr"] = result["stderr"] + os.linesep + str(error)
    else:
      result["stderr"] = str(error)
    puppetOutput = open(tmpoutfile, 'r').read()
    logger.debug("Output from puppet :\n" + puppetOutput)
    logger.info("Puppet execution process with pid %s exited with code %s." %
                (str(puppet.pid), str(returncode)))
    if result.has_key("exitcode"):
      result["exitcode"] = max(returncode, result["exitcode"])
    else:
      result["exitcode"] = returncode
    condensed = self.condenseOutput(puppetOutput, error, returncode)
    if result.has_key("stdout"):
      result["stdout"] = result["stdout"] + os.linesep + str(condensed)
    else:
      result["stdout"] = str(condensed)
    return result

  def lauch_puppet_subprocess(self, puppetcommand, tmpout, tmperr, puppetEnv):
    """
    Creates subprocess with given parameters. This functionality was moved to separate method
    to make possible unit testing
    """
    return subprocess.Popen(puppetcommand,
      stdout=tmpout,
      stderr=tmperr,
      env=puppetEnv)

  def puppet_watchdog_func(self, puppet):
    self.event.wait(float(self.puppet_timeout))
    if puppet.returncode is None:
      logger.error("Task timed out, killing process with PID: " + str(puppet.pid))
      shell.kill_process_with_children(puppet.pid)
      self.last_puppet_has_been_killed = True
    pass


def main():
  logging.basicConfig(level=logging.DEBUG)    
  #test code
  jsonFile = open('test.json', 'r')
  jsonStr = jsonFile.read() 
  # Below is for testing only.
  
  puppetInstance = PuppetExecutor("/home/centos/ambari_repo_info/ambari-agent/src/main/puppet/",
                                  "/usr/",
                                  "/root/workspace/puppet-install/facter-1.6.10/",
                                  "/tmp")
  jsonFile = open('test.json', 'r')
  jsonStr = jsonFile.read() 
  parsedJson = json.loads(jsonStr)
  result = puppetInstance.runCommand(parsedJson, '/tmp/out.txt', '/tmp/err.txt')
  logger.debug(result)
  
if __name__ == '__main__':
  main()

