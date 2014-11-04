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

import os
import logging
import pwd
import shell
import subprocess
from threading import Thread
import threading
from ambari_commons import OSCheck, OSConst, Firewall

LIST_INSTALLED_PACKAGES_UBUNTU = "for i in $(dpkg -l |grep ^ii |awk -F' ' '{print $2}'); do      apt-cache showpkg \"$i\"|head -3|grep -v '^Versions'| tr -d '()' | awk '{ print $1\" \"$2 }'|sed -e 's/^Package: //;' | paste -d ' ' - -;  done"
LIST_AVAILABLE_PACKAGES_UBUNTU = "packages=`for  i in $(ls -1 /var/lib/apt/lists  | grep -v \"ubuntu.com\") ; do grep ^Package: /var/lib/apt/lists/$i |  awk '{print $2}' ; done` ; for i in $packages; do      apt-cache showpkg \"$i\"|head -3|grep -v '^Versions'| tr -d '()' | awk '{ print $1\" \"$2 }'|sed -e 's/^Package: //;' | paste -d ' ' - -;  done"

logger = logging.getLogger()

class PackagesAnalyzer:

  # default timeout for async invoked processes
  TIMEOUT_SECONDS = 40
  event = threading.Event()

  def launch_subprocess(self, command):
    isShell = not isinstance(command, (list, tuple))
    return subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=isShell)

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

  # All installed packages in systems supporting yum
  def allInstalledPackages(self, allInstalledPackages):
    osType = OSCheck.get_os_family()
    
    if osType == OSConst.SUSE_FAMILY:
      return self.lookUpZypperPackages(
        ["zypper", "search", "--installed-only", "--details"],
        allInstalledPackages)
    elif osType == OSConst.REDHAT_FAMILY:
      return self.lookUpYumPackages(
        ["yum", "list", "installed"],
        'Installed Packages',
        allInstalledPackages)
    elif osType == OSConst.UBUNTU_FAMILY:
       return self.lookUpAptPackages(
        LIST_INSTALLED_PACKAGES_UBUNTU,
        allInstalledPackages)   

  def allAvailablePackages(self, allAvailablePackages):
    osType = OSCheck.get_os_family()
    
    if osType == OSConst.SUSE_FAMILY:
      return self.lookUpZypperPackages(
        ["zypper", "search", "--uninstalled-only", "--details"],
        allAvailablePackages)
    elif osType == OSConst.REDHAT_FAMILY:
      return self.lookUpYumPackages(
        ["yum", "list", "available"],
        'Available Packages',
        allAvailablePackages)
    elif osType == OSConst.UBUNTU_FAMILY:
       return self.lookUpAptPackages(
        LIST_AVAILABLE_PACKAGES_UBUNTU,
        allAvailablePackages)   
      
  def lookUpAptPackages(self, command, allPackages):   
    try:
      result = self.subprocessWithTimeout(command)
      if 0 == result['retCode']:
        for x in result['out'].split('\n'):
          if x.strip():
            allPackages.append(x.split(' '))
      
    except:
      pass

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
