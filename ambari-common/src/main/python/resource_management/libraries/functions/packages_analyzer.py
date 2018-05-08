#!/usr/bin/env python

"""
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
"""

import re
import sys
import logging
from ambari_commons import subprocess32
from threading import Thread
import threading
from ambari_commons import OSCheck, OSConst
from ambari_commons import shell
from ambari_commons.constants import AMBARI_SUDO_BINARY
from resource_management.core.logger import Logger
from resource_management.core import shell as rmf_shell
from resource_management.core.exceptions import Fail

__all__ = ["installedPkgsByName", "allInstalledPackages", "allAvailablePackages", "nameMatch",
           "getInstalledRepos", "getInstalledPkgsByRepo", "getInstalledPkgsByNames", "getPackageDetails"]

LIST_INSTALLED_PACKAGES_UBUNTU = "COLUMNS=9999 ; for i in $(dpkg -l |grep ^ii |awk -F' ' '{print $2}'); do      apt-cache showpkg \"$i\"|head -3|grep -v '^Versions'| tr -d '()' | awk '{ print $1\" \"$2 }'|sed -e 's/^Package: //;' | paste -d ' ' - -;  done"
LIST_AVAILABLE_PACKAGES_UBUNTU = "packages=`for  i in $(ls -1 /var/lib/apt/lists  | grep %s ) ; do grep ^Package: /var/lib/apt/lists/$i |  awk '{print $2}' ; done` ; for i in $packages; do      apt-cache showpkg \"$i\"|head -3|grep -v '^Versions'| tr -d '()' | awk '{ print $1\" \"$2 }'|sed -e 's/^Package: //;' | paste -d ' ' - -;  done"
GREP_REPO_EXCLUDE_SYSTEM = "-v \"ubuntu.com\""
logger = logging.getLogger()

# default timeout for async invoked processes
TIMEOUT_SECONDS = 40


def _launch_subprocess(command):
  isShell = not isinstance(command, (list, tuple))
  return subprocess32.Popen(command, stdout=subprocess32.PIPE, stderr=subprocess32.PIPE, shell=isShell, close_fds=True)


def subprocessWithTimeout(command):
  event = threading.Event()

  def watchdog_func(command):
    event.wait(TIMEOUT_SECONDS)
    if command.returncode is None:
      logger.error("Task timed out and will be killed")
      shell.kill_process_with_children(command.pid)
    pass

  osStat = _launch_subprocess(command)
  logger.debug("Launching watchdog thread")

  event.clear()

  thread = Thread(target=watchdog_func, args=(osStat, ))
  thread.start()

  out, err = osStat.communicate()
  result = {}
  result['out'] = out
  result['err'] = err
  result['retCode'] = osStat.returncode

  event.set()
  thread.join()
  return result


def installedPkgsByName(allInstalledPackages,
                        pkgName, installedPkgs):
  """
  Get all installed package whose name starts with the
  strings contained in pkgName
  """
  for item in allInstalledPackages:
    if item[0].find(pkgName) == 0:
      installedPkgs.append(item[0])


def allInstalledPackages(allInstalledPackages):
  """
  All installed packages in system
  """
  if OSCheck.is_suse_family():
    return _lookUpZypperPackages(
      [AMBARI_SUDO_BINARY, "zypper", "--no-gpg-checks", "search", "--installed-only", "--details"],
      allInstalledPackages)
  elif OSCheck.is_redhat_family():
    return _lookUpYumPackages(
      [AMBARI_SUDO_BINARY, "yum", "list", "installed"],
      'Installed Packages',
      allInstalledPackages)
  elif OSCheck.is_ubuntu_family():
     return _lookUpAptPackages(
      LIST_INSTALLED_PACKAGES_UBUNTU,
      allInstalledPackages)


def get_available_packages_in_repos(repositories):
  """
  Gets all (both installed and available) packages that are available at given repositories.
  :param repositories: from command configs like config['repositoryFile']['repositories']
  :return: installed and available packages from these repositories
  """

  available_packages = []
  installed_packages = []
  available_packages_in_repos = []
  repo_ids = [repository['repoId'] for repository in repositories]
  if OSCheck.is_ubuntu_family():
    allInstalledPackages(installed_packages)
    repo_urls = [repository['baseUrl'] for repository in repositories]
    repo_urls = [repo_url.replace("http://","") for repo_url in repo_urls]
    repo_urls = [repo_url.replace("/","_") for repo_url in repo_urls]
    for url in repo_urls:
      _lookUpAptPackages(
        LIST_AVAILABLE_PACKAGES_UBUNTU % url,
        available_packages)
      for package in installed_packages:
        if url in package[2]:
          available_packages_in_repos.append(package[0])
    for package in available_packages:
      available_packages_in_repos.append(package[0])
  elif OSCheck.is_suse_family():
    for repo in repo_ids:
      _lookUpZypperPackages([AMBARI_SUDO_BINARY, "zypper", "--no-gpg-checks", "search", "--details", "--repo", repo],
                            available_packages)
    available_packages_in_repos += [package[0] for package in available_packages]
  elif OSCheck.is_redhat_family():
    for repo in repo_ids:
      _lookUpYumPackages([AMBARI_SUDO_BINARY, "yum", "list", "available", "--disablerepo=*", "--enablerepo=" + repo],
                         'Available Packages', available_packages)
      _lookUpYumPackages([AMBARI_SUDO_BINARY, "yum", "list", "installed", "--disablerepo=*", "--enablerepo=" + repo],
                         'Installed Packages', installed_packages)
    available_packages_in_repos += [package[0] for package in available_packages + installed_packages]
  return available_packages_in_repos


def allAvailablePackages(allAvailablePackages):
  if OSCheck.is_suse_family():
    return _lookUpZypperPackages(
      [AMBARI_SUDO_BINARY, "zypper", "--no-gpg-checks", "search", "--uninstalled-only", "--details"],
      allAvailablePackages)
  elif OSCheck.is_redhat_family():
    return _lookUpYumPackages(
      [AMBARI_SUDO_BINARY, "yum", "list", "available"],
      'Available Packages',
      allAvailablePackages)
  elif OSCheck.is_ubuntu_family():
     return _lookUpAptPackages(
       LIST_AVAILABLE_PACKAGES_UBUNTU % GREP_REPO_EXCLUDE_SYSTEM,
      allAvailablePackages)

# ToDo: add execution via sudo for ubuntu (currently Ubuntu is not supported)
def _lookUpAptPackages(command, allPackages):
  try:
    result = subprocessWithTimeout(command)
    if 0 == result['retCode']:
      for x in result['out'].split('\n'):
        if x.strip():
          allPackages.append(x.split(' '))

  except:
    logger.error("Unexpected error:", sys.exc_info()[0])


def _lookUpYumPackages(command, skipTill, allPackages):
  try:
    result = subprocessWithTimeout(command)
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
        if '.' in items[i]:
          items[i] = items[i][:items[i].rindex('.')]
        if items[i + 2].find('@') == 0:
          items[i + 2] = items[i + 2][1:]
        allPackages.append(items[i:i + 3])
  except:
    logger.error("Unexpected error:", sys.exc_info()[0])


def _lookUpZypperPackages(command, allPackages):
  try:
    result = subprocessWithTimeout(command)
    if 0 == result['retCode']:
      lines = result['out'].split('\n')
      lines = [line.strip() for line in lines]
      for index in range(len(lines)):
        if "--+--" in lines[index]:
          skipIndex = index + 1
          break

      for line in lines[skipIndex:]:
        items = line.strip(' \t\n\r').split('|')
        allPackages.append([items[1].strip(), items[3].strip(), items[5].strip()])
  except:
    logger.error("Unexpected error:", sys.exc_info()[0])


def nameMatch(lookupName, actualName):
  tokens = actualName.strip().split()
  for token in tokens:
    if lookupName.lower() in token.lower():
      return True
  return False


def getInstalledRepos(hintPackages, allPackages, ignoreRepos, repoList):
  """
  Gets all installed repos by name based on repos that provide any package
  contained in hintPackages
  Repos starting with value in ignoreRepos will not be returned
  hintPackages must be regexps.
  """
  allRepos = []
  for hintPackage in hintPackages:
    for item in allPackages:
      if re.match(hintPackage, item[0]) and not item[2] in allRepos:
        allRepos.append(item[2])

  for repo in allRepos:
    ignore = False
    for ignoredRepo in ignoreRepos:
      if nameMatch(ignoredRepo, repo):
        ignore = True
    if not ignore:
      repoList.append(repo)


def getInstalledPkgsByRepo(repos, ignorePackages, installedPackages):
  """
  Get all the installed packages from the repos listed in repos
  """
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
      if nameMatch(ignorePackage, package):
        keepPackage = False
        break
    if keepPackage:
      packagesToRemove.append(package)
  return packagesToRemove


def getInstalledPkgsByNames(pkgNames, installedPackages):
  """
  Gets all installed packages that start with names in pkgNames
  """
  packages = []
  for pkgName in pkgNames:
    subResult = []
    installedPkgsByName(installedPackages, pkgName, subResult)
    packages = list(set(packages + subResult))
  return packages


def getPackageDetails(installedPackages, foundPackages):
  """
  Gets the name, version, and repoName for the packages
  """
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

def getReposToRemove(repos, ignoreList):
  reposToRemove = []
  for repo in repos:
    addToRemoveList = True
    for ignoreRepo in ignoreList:
      if nameMatch(ignoreRepo, repo):
        addToRemoveList = False
        continue
    if addToRemoveList:
      reposToRemove.append(repo)
  return reposToRemove

def getInstalledPackageVersion(package_name):
  if OSCheck.is_ubuntu_family():
    code, out = rmf_shell.checked_call("dpkg -s {0} | grep Version | awk '{{print $2}}'".format(package_name))
  else:
    code, out = rmf_shell.checked_call("rpm -q --queryformat '%{{version}}-%{{release}}' {0} | sed -e 's/\.el[0-9]//g'".format(package_name))

  return out


def verifyDependencies():
  """
  Verify that we have no dependency issues in package manager. Dependency issues could appear because of aborted or terminated
  package installation process or invalid packages state after manual modification of packages list on the host
   :return True if no dependency issues found, False if dependency issue present
  :rtype bool
  """
  check_str = None
  cmd = None

  if OSCheck.is_redhat_family():
    cmd = ['/usr/bin/yum', '-d', '0', '-e', '0', 'check', 'dependencies']
    check_str = "has missing requires|Error:"
  elif OSCheck.is_suse_family():
    cmd = ['/usr/bin/zypper', '--quiet', '--non-interactive', 'verify', '--dry-run']
    check_str = "\d+ new package(s)? to install"
  elif OSCheck.is_ubuntu_family():
    cmd = ['/usr/bin/apt-get', '-qq', 'check']
    check_str = "has missing dependency|E:"

  if check_str is None or cmd is None:
    raise Fail("Unsupported OSFamily on the Agent Host")

  code, out = rmf_shell.checked_call(cmd, sudo=True)

  output_regex = re.compile(check_str)

  if code or (out and output_regex.search(out)):
    err_msg = Logger.filter_text("Failed to verify package dependencies. Execution of '%s' returned %s. %s" % (cmd, code, out))
    Logger.error(err_msg)
    return False

  return True
