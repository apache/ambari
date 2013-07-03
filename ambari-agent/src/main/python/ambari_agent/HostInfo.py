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

import os
import glob
import logging
import pwd
import re
import time
import subprocess
import threading
import AmbariConfig
from PackagesAnalyzer import PackagesAnalyzer
from HostCheckReportFileHandler import HostCheckReportFileHandler
from Hardware import Hardware

logger = logging.getLogger()


class HostInfo:
  # List of project names to be used to find alternatives folders etc.
  DEFAULT_PROJECT_NAMES = [
    "hadoop*", "hadoop", "hbase", "hcatalog", "hive", "ganglia", "nagios",
    "oozie", "sqoop", "hue", "zookeeper", "mapred", "hdfs", "flume",
    "ambari_qa", "hadoop_deploy", "rrdcached", "hcat"
  ]

  # List of live services checked for on the host
  DEFAULT_LIVE_SERVICES = [
    "ntpd"
  ]

  # Set of default users (need to be replaced with the configured user names)
  DEFAULT_USERS = [
    "nagios", "hive", "ambari-qa", "oozie", "hbase", "hcat", "mapred",
    "hdfs", "rrdcached", "zookeeper", "mysql", "flume", "sqoop", "sqoop2",
    "hue", "yarn"
  ]

  # Filters used to identify processed
  PROC_FILTER = [
    "hadoop", "zookeeper"
  ]

  # Default set of directories that are checked for existence of files and folders
  DEFAULT_DIRS = [
    "/etc", "/var/run", "/var/log", "/usr/lib", "/var/lib", "/var/tmp", "/tmp", "/var"
  ]

  # Packages that are used to find repos (then repos are used to find other packages)
  PACKAGES = [
    "hadoop", "zookeeper", "webhcat", "*-manager-server-db", "*-manager-daemons"
  ]

  # Additional packages to look for (search packages that start with these)
  ADDITIONAL_PACKAGES = [
    "rrdtool", "rrdtool-python", "nagios", "ganglia", "gmond", "gweb"
  ]

  # ignores packages from repos whose names start with these strings
  IGNORE_PACKAGES_FROM_REPOS = [
    "ambari", "installed"
  ]

  IGNORE_REPOS = [
    "ambari", "HDP-UTILS"
  ]

  # default timeout for async invoked processes
  TIMEOUT_SECONDS = 60
  RESULT_UNAVAILABLE = "unable_to_determine"
  event = threading.Event()

  def __init__(self, config=None):
    self.packages = PackagesAnalyzer()
    self.reportFileHandler = HostCheckReportFileHandler(config)

  def dirType(self, path):
    if not os.path.exists(path):
      return 'not_exist'
    elif os.path.islink(path):
      return 'sym_link'
    elif os.path.isdir(path):
      return 'directory'
    elif os.path.isfile(path):
      return 'file'
    return 'unknown'

  def rpmInfo(self, rpmList):
    config = AmbariConfig.config

    try:
      for rpmName in config.get('heartbeat', 'rpms').split(','):
        rpmName = rpmName.strip()
        rpm = {}
        rpm['name'] = rpmName

        try:
          osStat = subprocess.Popen(["rpm", "-q", rpmName], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
          out, err = osStat.communicate()
          if (0 != osStat.returncode or 0 == len(out.strip())):
            rpm['installed'] = False
          else:
            rpm['installed'] = True
            rpm['version'] = out.strip()
        except:
          rpm['available'] = False

        rpmList.append(rpm)
    except:
      pass

  def hadoopVarRunCount(self):
    if not os.path.exists('/var/run/hadoop'):
      return 0
    pids = glob.glob('/var/run/hadoop/*/*.pid')
    return len(pids)

  def hadoopVarLogCount(self):
    if not os.path.exists('/var/log/hadoop'):
      return 0
    logs = glob.glob('/var/log/hadoop/*/*.log')
    return len(logs)

  def etcAlternativesConf(self, projects, etcResults):
    if not os.path.exists('/etc/alternatives'):
      return []
    projectRegex = "'" + '|'.join(projects) + "'"
    files = [f for f in os.listdir('/etc/alternatives') if re.match(projectRegex, f)]
    for conf in files:
      result = {}
      filePath = os.path.join('/etc/alternatives', conf)
      if os.path.islink(filePath):
        realConf = os.path.realpath(filePath)
        result['name'] = conf
        result['target'] = realConf
        etcResults.append(result)

  def checkLiveServices(self, services, result):
    for service in services:
      svcCheckResult = {}
      svcCheckResult['name'] = service
      svcCheckResult['status'] = "UNKNOWN"
      svcCheckResult['desc'] = ""
      try:
        osStat = subprocess.Popen(["service", service, "status"], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        out, err = osStat.communicate()
        if 0 != osStat.returncode:
          svcCheckResult['status'] = "Unhealthy"
          svcCheckResult['desc'] = out
          if len(out) == 0:
            svcCheckResult['desc'] = err
        else:
          svcCheckResult['status'] = "Healthy"
      except Exception, e:
        svcCheckResult['status'] = "Unhealthy"
        svcCheckResult['desc'] = repr(e)
      result.append(svcCheckResult)

  def checkUsers(self, users, results):
    f = open('/etc/passwd', 'r')
    for userLine in f:
      fields = userLine.split(":")
      if fields[0] in users:
        result = {}
        homeDir = fields[5]
        result['name'] = fields[0]
        result['homeDir'] = fields[5]
        result['status'] = "Available";
        if not os.path.exists(homeDir):
          result['status'] = "Invalid home directory";
        results.append(result)

  def osdiskAvailableSpace(self, path):
    diskInfo = {}
    try:
      df = subprocess.Popen(["df", "-kPT", path], stdout=subprocess.PIPE)
      dfdata = df.communicate()[0]
      return Hardware.extractMountInfo(dfdata.splitlines()[-1])
    except:
      pass
    return diskInfo

  def checkFolders(self, basePaths, projectNames, existingUsers, dirs):
    foldersToIgnore = []
    for user in existingUsers:
      foldersToIgnore.append(user['homeDir'])
    try:
      for dirName in basePaths:
        for project in projectNames:
          path = os.path.join(dirName.strip(), project.strip())
          if not path in foldersToIgnore and os.path.exists(path):
            obj = {}
            obj['type'] = self.dirType(path)
            obj['name'] = path
            dirs.append(obj)
    except:
      pass

  def javaProcs(self, list):
    try:
      pids = [pid for pid in os.listdir('/proc') if pid.isdigit()]
      for pid in pids:
        cmd = open(os.path.join('/proc', pid, 'cmdline'), 'rb').read()
        cmd = cmd.replace('\0', ' ')
        if not 'AmbariServer' in cmd:
          if 'java' in cmd:
            dict = {}
            dict['pid'] = int(pid)
            dict['hadoop'] = False
            for filter in self.PROC_FILTER:
              if filter in cmd:
                dict['hadoop'] = True
            dict['command'] = cmd.strip()
            for line in open(os.path.join('/proc', pid, 'status')):
              if line.startswith('Uid:'):
                uid = int(line.split()[1])
                dict['user'] = pwd.getpwuid(uid).pw_name
            list.append(dict)
    except:
      pass
    pass

  def getReposToRemove(self, repos, ignoreList):
    reposToRemove = []
    for repo in repos:
      addToRemoveList = True
      for ignoreRepo in ignoreList:
        if self.packages.nameMatch(ignoreRepo, repo):
          addToRemoveList = False
          continue
      if addToRemoveList:
        reposToRemove.append(repo)
    return reposToRemove

  """ Return various details about the host
  componentsMapped: indicates if any components are mapped to this host
  commandsInProgress: indicates if any commands are in progress
  """
  def register(self, dict, componentsMapped=True, commandsInProgress=True):
    dict['hostHealth'] = {}

    java = []
    self.javaProcs(java)
    dict['hostHealth']['activeJavaProcs'] = java

    dict['hostHealth']['diskStatus'] = [self.osdiskAvailableSpace("/")]

    rpms = []
    self.rpmInfo(rpms)
    dict['rpms'] = rpms

    liveSvcs = []
    self.checkLiveServices(self.DEFAULT_LIVE_SERVICES, liveSvcs)
    dict['hostHealth']['liveServices'] = liveSvcs

    # If commands are in progress or components are already mapped to this host
    # Then do not perform certain expensive host checks
    if componentsMapped or commandsInProgress:
      dict['existingRepos'] = [self.RESULT_UNAVAILABLE]
      dict['installedPackages'] = []
      dict['alternatives'] = []
      dict['stackFoldersAndFiles'] = []
      dict['existingUsers'] = []

    else:
      etcs = []
      self.etcAlternativesConf(self.DEFAULT_PROJECT_NAMES, etcs)
      dict['alternatives'] = etcs

      existingUsers = []
      self.checkUsers(self.DEFAULT_USERS, existingUsers)
      dict['existingUsers'] = existingUsers

      dirs = []
      self.checkFolders(self.DEFAULT_DIRS, self.DEFAULT_PROJECT_NAMES, existingUsers, dirs)
      dict['stackFoldersAndFiles'] = dirs

      installedPackages = []
      availablePackages = []
      self.packages.allInstalledPackages(installedPackages)
      self.packages.allAvailablePackages(availablePackages)

      repos = []
      self.packages.getInstalledRepos(self.PACKAGES, installedPackages + availablePackages,
                                      self.IGNORE_PACKAGES_FROM_REPOS, repos)
      repos = self.getReposToRemove(repos, self.IGNORE_REPOS)
      dict['existingRepos'] = repos

      packagesInstalled = self.packages.getInstalledPkgsByRepo(repos, installedPackages)
      additionalPkgsInstalled = self.packages.getInstalledPkgsByNames(
        self.ADDITIONAL_PACKAGES, installedPackages)
      allPackages = list(set(packagesInstalled + additionalPkgsInstalled))
      dict['installedPackages'] = self.packages.getPackageDetails(installedPackages, allPackages)
      self.reportFileHandler.writeHostCheckFile(dict)
      pass

    # The time stamp must be recorded at the end
    dict['hostHealth']['agentTimeStampAtReporting'] = int(time.time() * 1000)

    pass


def main(argv=None):
  h = HostInfo()
  struct = {}
  h.register(struct)
  print struct


if __name__ == '__main__':
  main()
