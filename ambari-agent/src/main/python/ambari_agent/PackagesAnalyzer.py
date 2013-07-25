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
import logging
import pwd
import shell
import subprocess
from threading import Thread
import threading

logger = logging.getLogger()

class PackagesAnalyzer:

  # default timeout for async invoked processes
  TIMEOUT_SECONDS = 10
  event = threading.Event()

  def launch_subprocess(self, command):
    return subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

  def watchdog_func(self, command):
    self.event.wait(self.TIMEOUT_SECONDS)
    if command.returncode is None:
      logger.error("Task timed out and will be killed")
      shell.kill_process_with_children(command.pid)
    pass

  def subprocessWithTimeout(self, command):
    osStat = self.launch_subprocess(command)
    logger.debug("Launching watchdog thread")
    self.event.clear()
    thread = Thread(target=self.watchdog_func, args=(osStat, ))
    thread.start()

    out, err = osStat.communicate()
    result = {}
    result['out'] = out
    result['err'] = err
    result['retCode'] = osStat.returncode

    self.event.set()
    thread.join()
    return result

  # Get all installed package whose name starts with the
  # strings contained in pkgName
  def installedPkgsByName(self, allInstalledPackages,
                          pkgName, installedPkgs):
    for item in allInstalledPackages:
      if item[0].find(pkgName) == 0:
        installedPkgs.append(item[0])

  def hasZypper(self):
    try:
      result = self.subprocessWithTimeout(["which", "zypper"])
      if 0 == result['retCode']:
        return True
      else:
        return False
    except:
      pass

  # All installed packages in systems supporting yum
  def allInstalledPackages(self, allInstalledPackages):
    if self.hasZypper():
      return self.lookUpZypperPackages(
        ["zypper", "search", "--installed-only", "--details"],
        allInstalledPackages)
    else:
      return self.lookUpYumPackages(
        ["yum", "list", "installed"],
        'Installed Packages',
        allInstalledPackages)

  # All available packages in systems supporting yum
  def allAvailablePackages(self, allAvailablePackages):
    if self.hasZypper():
      return self.lookUpZypperPackages(
        ["zypper", "search", "--uninstalled-only", "--details"],
        allAvailablePackages)
    else:
      return self.lookUpYumPackages(
        ["yum", "list", "available"],
        'Available Packages',
        allAvailablePackages)

  def lookUpYumPackages(self, command, skipTill, allPackages):
    try:
      result = self.subprocessWithTimeout(command)
      if 0 == result['retCode']:
        lines = result['out'].split('\n')
        lines = [line.strip() for line in lines]
        items = []
        skipIndex = 3
        for index in range(len(lines)):
          if skipTill in lines[index]:
            skipIndex = index + 1
            break

        for line in lines[skipIndex:]:
          items = items + line.strip(' \t\n\r').split()

        for i in range(0, len(items), 3):
          if items[i + 2].find('@') == 0:
            items[i + 2] = items[i + 2][1:]
          allPackages.append(items[i:i + 3])
    except:
      pass

  def lookUpZypperPackages(self, command, allPackages):
    try:
      result = self.subprocessWithTimeout(command)
      if 0 == result['retCode']:
        lines = result['out'].split('\n')
        lines = [line.strip() for line in lines]
        items = []
        for index in range(len(lines)):
          if "--+--" in lines[index]:
            skipIndex = index + 1
            break

        for line in lines[skipIndex:]:
          items = line.strip(' \t\n\r').split('|')
          allPackages.append([items[1].strip(), items[3].strip(), items[5].strip()])
    except:
      pass

  def nameMatch(self, lookupName, actualName):
    tokens = actualName.strip().split()
    for token in tokens:
      if token.lower().find(lookupName.lower()) == 0:
        return True
    return False

  # Gets all installed repos by name based on repos that provide any package
  # contained in hintPackages
  # Repos starting with value in ignoreRepos will not be returned
  def getInstalledRepos(self, hintPackages, allPackages, ignoreRepos, repoList):
    allRepos = []
    for hintPackage in hintPackages:
      for item in allPackages:
        if 0 == item[0].find(hintPackage):
          if not item[2] in allRepos:
            allRepos.append(item[2])
        elif hintPackage[0] == '*':
          if item[0].find(hintPackage[1:]) > 0:
            if not item[2] in allRepos:
              allRepos.append(item[2])

    for repo in allRepos:
      ignore = False
      for ignoredRepo in ignoreRepos:
        if self.nameMatch(ignoredRepo, repo):
          ignore = True
      if not ignore:
        repoList.append(repo)

  # Get all the installed packages from the repos listed in repos
  def getInstalledPkgsByRepo(self, repos, ignorePackages, installedPackages):
    packagesFromRepo = []
    packagesToRemove = []
    for repo in repos:
      subResult = []
      for item in installedPackages:
        if repo == item[2]:
          subResult.append(item[0])
      packagesFromRepo = list(set(packagesFromRepo + subResult))

    for package in packagesFromRepo:
      keepPackage = True
      for ignorePackage in ignorePackages:
        if self.nameMatch(ignorePackage, package):
          keepPackage = False
          break
      if keepPackage:
        packagesToRemove.append(package)
    return packagesToRemove

  # Gets all installed packages that start with names in pkgNames
  def getInstalledPkgsByNames(self, pkgNames, installedPackages):
    packages = []
    for pkgName in pkgNames:
      subResult = []
      self.installedPkgsByName(installedPackages, pkgName, subResult)
      packages = list(set(packages + subResult))
    return packages

  # Gets the name, version, and repoName for the packages
  def getPackageDetails(self, installedPackages, foundPackages):
    packageDetails = []
    for package in foundPackages:
      pkgDetail = {}
      for installedPackage in installedPackages:
        if package == installedPackage[0]:
          pkgDetail['name'] = installedPackage[0]
          pkgDetail['version'] = installedPackage[1]
          pkgDetail['repoName'] = installedPackage[2]
      packageDetails.append(pkgDetail)
    return packageDetails
