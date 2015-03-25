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
import os
import subprocess

from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

if OSCheck.is_windows_family():
  import urllib2

  from ambari_commons.exceptions import FatalException
  from ambari_commons.inet_utils import force_download_file
  from ambari_commons.os_utils import run_os_command


AMBARI_PASSPHRASE_VAR = "AMBARI_PASSPHRASE"
PROJECT_VERSION_DEFAULT = "DEFAULT"

def _init_ambari_agent_symlink():
  installationDrive = os.path.splitdrive(__file__.replace('/', os.sep))[0]
  return os.path.join(installationDrive, os.sep, "ambari", "ambari-agent")

AMBARI_AGENT_INSTALL_SYMLINK = _init_ambari_agent_symlink()
INSTALL_MARKER_OK = "ambari-agent.installed"

def _ret_init(ret):
  if not ret:
    ret = {'exitstatus': 0, 'log': ('', '')}
  return ret

def _ret_append_stdout(ret, stdout):
  temp_stdout = ret['log'][0]
  temp_stderr = ret['log'][1]
  if stdout:
    if temp_stdout:
      temp_stdout += "\n"
    temp_stdout += stdout
  ret['log'] = (temp_stdout, temp_stderr)

def _ret_append_stderr(ret, stderr):
  temp_stdout = ret['log'][0]
  temp_stderr = ret['log'][1]
  if stderr:
    if temp_stderr:
      temp_stderr += "\n"
    temp_stderr += stderr
  ret['log'] = (temp_stdout, temp_stderr)

def _ret_merge(ret, retcode, stdout, stderr):
  ret['exitstatus'] = retcode
  temp_stdout = ret['log'][0]
  temp_stderr = ret['log'][1]
  if stdout:
    if temp_stdout:
      temp_stdout += "\n"
    temp_stdout += stdout
  if stderr:
    if temp_stderr:
      temp_stderr += "\n"
    temp_stderr += stderr
  ret['log'] = (temp_stdout, temp_stderr)
  return ret

def _ret_merge2(ret, ret2):
  return _ret_merge(ret, ret2['exitstatus'], ret['log'][0], ret['log'][1])


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def execOsCommand(osCommand, tries=1, try_sleep=0, ret=None, cwd=None):
  ret = _ret_init(ret)

  for i in range(0, tries):
    if i > 0:
      time.sleep(try_sleep)

    retcode, stdout, stderr = run_os_command(osCommand, cwd=cwd)
    _ret_merge(ret, retcode, stdout, stderr)
    if retcode == 0:
      break

    _ret_append_stdout("\nRetrying " + str(osCommand))

  return ret

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def execOsCommand(osCommand, tries=1, try_sleep=0, ret=None, cwd=None):
  ret = _ret_init(ret)

  for i in range(0, tries):
    if i>0:
      time.sleep(try_sleep)

    osStat = subprocess.Popen(osCommand, stdout=subprocess.PIPE, cwd=cwd)
    log = osStat.communicate(0)
    ret = {"exitstatus": osStat.returncode, "log": log}

    if ret['exitstatus'] == 0:
      break

  return ret

def _download_file(url, destFilePath, progress_function=None, ret=None):
  ret = _ret_init(ret)

  if os.path.exists(destFilePath):
    _ret_append_stdout(ret, "\nFile {0} already exists, assuming it was downloaded before".format(destFilePath))
  else:
    try:
      #Intrinsically reliable and resumable. Downloads to a temp file and renames the tem file to the destination file
      # upon successful termination.
      force_download_file(url, destFilePath, 16 * 1024, progress_function)
    except FatalException, e:
      _ret_merge(ret, e.code, None, "Failed to download {0} -> {1} : {2}".format(url, destFilePath, e.reason))
    except urllib2.URLError, ue:
      _ret_merge(ret, 2, None, "Failed to download {0} -> {1} : {2}".format(url, destFilePath, ue.reason))
  return ret


def _create_agent_symlink(symlinkPath, agentInstallDir, ret):
  ret = _ret_init(ret)

  symLinkCreationAttempts = 0
  while (symLinkCreationAttempts < 1000):
    # Handle contention from other bootstrap processes
    try:
      os.rmdir(symlinkPath)
    except OSError:
      #It's ok to attempt to delete a non-existing link
      pass

    try:
      os.symlink(agentInstallDir, symlinkPath)
      if os.readlink(symlinkPath) == agentInstallDir:
        break
    except OSError:
      pass

    symLinkCreationAttempts += 1
  if symLinkCreationAttempts == 1000:
    _ret_merge(ret, 1000, '',
               'Failed creating the symbolic link {0} because of contention.'.format(AMBARI_AGENT_INSTALL_SYMLINK))
  return ret


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def installAgent(url, downloadDir, projectVersion, ret=None):
  """ Download the agent msi and install it
  :param url:
  :param projectVersion:
  :return: {"exitstatus": exit code, "log": log records string}
  """
  ret = _ret_init(ret)

  installationDrive = os.path.splitdrive(__file__.replace('/', os.sep))[0]

  agentInstallDir = os.path.join(installationDrive, os.sep, "ambari", "ambari-agent-" + projectVersion)
  agentInstallMarkerFile = os.path.join(agentInstallDir, INSTALL_MARKER_OK)

  if not os.path.exists(agentInstallMarkerFile):
    destMsiFilePath = os.path.join(downloadDir, "ambari-agent-{0}.msi".format(projectVersion))
    ret = _download_file(url, destMsiFilePath, ret=ret)
    if ret['exitstatus'] != 0:
      return ret

    #ambari-agent-<version>.msi downloaded, proceed to the installation
    installLogPath = os.path.join(downloadDir, "ambari-agent-{0}.install.log".format(projectVersion))
    installCmd = [
      "cmd",
      "/c",
      "start",
      "/wait",
      "msiexec",
      "/i", destMsiFilePath,
      "AGENT_INSTALL_DIRECTORY=" + agentInstallDir,
      "/qn",
      "/Lv", installLogPath]
    ret = execOsCommand(installCmd, tries=3, try_sleep=10, ret=ret)
    if ret['exitstatus'] != 0:
      #TODO Check if the product was already installed. Only machine reimage can repair a broken installation.
      return ret

    try:
      if os.readlink(AMBARI_AGENT_INSTALL_SYMLINK) != agentInstallDir:
        ret = _create_agent_symlink(AMBARI_AGENT_INSTALL_SYMLINK, agentInstallDir, ret)
    except OSError:
      ret = _create_agent_symlink(AMBARI_AGENT_INSTALL_SYMLINK, agentInstallDir, ret)
    if ret['exitstatus'] != 0:
      return ret

    try:
      open(agentInstallMarkerFile, "w+").close()
    except IOError:
      pass

  return ret

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
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


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def configureAgent(server_hostname, cwd, ret=None):
  #Customize ambari-agent.ini & register the Ambari Agent service
  agentSetupCmd = ["cmd", "/c", "ambari-agent.cmd", "setup", "--hostname=" + server_hostname]
  return execOsCommand(agentSetupCmd, tries=3, try_sleep=10, cwd=AMBARI_AGENT_INSTALL_SYMLINK, ret=ret)

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def configureAgent(server_hostname, user_run_as):
  """ Configure the agent so that it has all the configs knobs properly installed """
  osCommand = ["sed", "-i.bak", "s/hostname=localhost/hostname=" + server_hostname +
                                "/g", "/etc/ambari-agent/conf/ambari-agent.ini"]
  ret = execOsCommand(osCommand)
  if ret['exitstatus'] != 0:
    return ret
  osCommand = ["sed", "-i.bak", "s/run_as_user=.*$/run_as_user=" + user_run_as +
                                "/g", "/etc/ambari-agent/conf/ambari-agent.ini"]
  ret = execOsCommand(osCommand)
  return ret


#Windows-specific
def runAgentService(ret=None):
  ret = _ret_init(ret)

  #Invoke ambari-agent restart as a child process
  agentRestartCmd = ["cmd", "/c", "ambari-agent.cmd", "restart"]
  return execOsCommand(agentRestartCmd, tries=3, try_sleep=10, cwd=AMBARI_AGENT_INSTALL_SYMLINK, ret=ret)

#Linux-specific
def runAgent(passPhrase, expected_hostname, user_run_as, verbose):
  os.environ[AMBARI_PASSPHRASE_VAR] = passPhrase
  vo = ""
  if verbose:
    vo = " -v"
  cmd = ['su', user_run_as, '-l', '-c', '/usr/sbin/ambari-agent restart --expected-hostname=%1s %2s' % (expected_hostname, vo)]
  log = ""
  p = subprocess.Popen(cmd, stdout=subprocess.PIPE)
  p.communicate()
  agent_retcode = p.returncode
  for i in range(3):
    time.sleep(1)
    ret = execOsCommand(["tail", "-20", "/var/log/ambari-agent/ambari-agent.log"])
    if (0 == ret['exitstatus']):
      try:
        log = ret['log']
      except Exception:
        log = "Log not found"
      print log
      break
  return {"exitstatus": agent_retcode, "log": log}
 
def tryStopAgent():
  verbose = False
  cmds = ["bash", "-c", "ps aux | grep 'AmbariAgent.py' | grep ' \-v'"]
  cmdl = ["bash", "-c", "ps aux | grep 'AmbariAgent.py' | grep ' \--verbose'"]
  if execOsCommand(cmds)["exitstatus"] == 0 or execOsCommand(cmdl)["exitstatus"] == 0:
    verbose = True
  subprocess.call("/usr/sbin/ambari-agent stop", shell=True)
  return verbose

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def getOptimalVersion(initialProjectVersion):
  if initialProjectVersion == "null" or initialProjectVersion == "{ambariVersion}" or \
          initialProjectVersion == PROJECT_VERSION_DEFAULT or not initialProjectVersion:
    #Extract the project version form the current script path
    scriptPath = os.path.dirname(__file__.replace('/', os.sep))
    optimalVersion = os.path.split(scriptPath)[1]
  else:
    optimalVersion = initialProjectVersion
  return optimalVersion

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
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
    ret = {"exitstatus": 0, "log": ""}
  except Exception:
    ret["exitstatus"] = 1
    ret["log"] = "Host registration aborted. Ambari Agent host cannot reach Ambari Server '" +\
                host+":"+str(port) + "'. " +\
                "Please check the network connectivity between the Ambari Agent host and the Ambari Server"
  return ret

#  Command line syntax help - Windows
# IsOptional  Index     Description
#               0        ambari-agent.msi URL
#               1        Server host name
#      X        2        Project version (Ambari)

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def parseArguments(argv=None):
  if argv is None:  # make sure that arguments was passed
    return {"exitstatus": 2, "log": "No arguments were passed"}
  args = argv[1:]  # shift path to script
  if len(args) < 2:
    return {"exitstatus": 1, "log": "Not all required arguments were passed"}

  agentUrl = args[0]
  serverHostname = args[1]
  projectVersion = PROJECT_VERSION_DEFAULT

  if len(args) > 2:
    projectVersion = args[2]

  parsed_args = (agentUrl, serverHostname, projectVersion)
  return {"exitstatus": 0, "log": ("", ""), "parsed_args": parsed_args}

#  Command line syntax help - Linux
# IsOptional  Index     Description
#               0        Expected host name
#               1        Password
#               2        Host name
#               3        User to run agent as
#      X        4        Project Version (Ambari)
#      X        5        Server port


@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def parseArguments(argv=None):
  if argv is None:  # make sure that arguments was passed
    return {"exitstatus": 2, "log": "No arguments were passed"}
  args = argv[1:]  # shift path to script
  if len(args) < 3:
    return {"exitstatus": 1, "log": "Not all required arguments were passed"}

  expected_hostname = args[0]
  passPhrase = args[1]
  hostname = args[2]
  user_run_as = args[3]
  projectVersion = ""
  server_port = 8080

  if len(args) > 4:
    projectVersion = args[4]

  if len(args) > 5:
    try:
      server_port = int(args[5])
    except (Exception):
      server_port = 8080

  parsed_args = (expected_hostname, passPhrase, hostname, user_run_as, projectVersion, server_port)
  return {"exitstatus": 0, "log": "", "parsed_args": parsed_args}


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def run_setup(argv=None):
  """
  if the Agent is not downloaded or the download was interrupted
    download the Agent msi package
  install the Agent from the msi package
  customize the Agent configuration
  register the Ambari Agent Windows service
  if JDK is not installed on the local machine
    download and install JDK
  set the machine-wide JAVA_HOME environment variable
  if the Agent service is running from a previous session
    stop the Agent service
  create/switch the Agent dir symbolic link to the new version
  start the Agent service
  """

  # Parse passed arguments
  retcode = parseArguments(argv)
  if (retcode["exitstatus"] != 0):
    return retcode

  (agent_url, server_hostname, projectVersion) = retcode["parsed_args"]

  availableProjectVersion = getOptimalVersion(projectVersion)

  retcode = installAgent(agent_url, os.getcwd(), availableProjectVersion, retcode)
  if (not retcode["exitstatus"] == 0):
    return retcode

  retcode = configureAgent(server_hostname, retcode)
  if retcode['exitstatus'] != 0:
    return retcode

  #TODO Install the JDK
  #install_jdk(jdk_url, java_home_dir, jdk_name, ret)

  return runAgentService(retcode)

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def run_setup(argv=None):
  # Parse passed arguments
  retcode = parseArguments(argv)
  if (retcode["exitstatus"] != 0):
    return retcode

  (expected_hostname, passPhrase, hostname, user_run_as, projectVersion, server_port) = retcode["parsed_args"]

  retcode = checkServerReachability(hostname, server_port)
  if (retcode["exitstatus"] != 0):
    return retcode

  if projectVersion == "null" or projectVersion == "{ambariVersion}" or projectVersion == "":
    retcode = getOptimalVersion("")
  else:
    retcode = getOptimalVersion(projectVersion)
  if retcode["exitstatus"] == 0 and retcode["log"] != None and retcode["log"] != "" and retcode["log"][0].strip() != "":
    availiableProjectVersion = retcode["log"].strip()
    if not isAgentPackageAlreadyInstalled(availiableProjectVersion):
      retcode = installAgent(availiableProjectVersion)
      if (not retcode["exitstatus"] == 0):
        return retcode
  elif retcode["exitstatus"] == 1 and retcode["log"][0].strip() != "":
    return {"exitstatus": 1, "log": "Desired version ("+projectVersion+") of ambari-agent package"
                                        " is not available."
                                        " Repository has following "
                                        "versions of ambari-agent:"+retcode["log"][0].strip()}
  else:
    return retcode

  retcode = configureAgent(hostname, user_run_as)
  if retcode['exitstatus'] != 0:
    return retcode
  return runAgent(passPhrase, expected_hostname, user_run_as, verbose)


@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def main(argv=None):
  try:
    exitcode = run_setup(argv)
  except Exception, e:
    exitcode = {"exitstatus": -1, "log": str(e)}
  return exitcode

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def main(argv=None):
  #Try stop agent and check --verbose option if agent already run
  global verbose
  verbose = tryStopAgent()
  if verbose:
    exitcode = run_setup(argv)
  else:
    try:
      exitcode = run_setup(argv)
    except Exception, e:
      exitcode = {"exitstatus": -1, "log": str(e)}
  return exitcode

if __name__ == '__main__':
  logging.basicConfig(level=logging.DEBUG)
  ret = main(sys.argv)
  retcode = ret["exitstatus"]
  if 0 != retcode:
    print ret["log"]
  sys.exit(retcode)
