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

Ambari Agent

"""

import os
import tempfile
import re
import subprocess

from ambari_commons.constants import AMBARI_SUDO_BINARY
from ambari_commons.shell import process_executor
from resource_management.core.providers.package import PackageProvider
from resource_management.core import shell
from resource_management.core import sudo
from resource_management.core.shell import string_cmd_from_args_list
from resource_management.core.logger import Logger


INSTALL_CMD_ENV = {'DEBIAN_FRONTEND':'noninteractive'}
INSTALL_CMD = {
  True: ['/usr/bin/apt-get', '-o', "Dpkg::Options::=--force-confdef", '--allow-unauthenticated', '--assume-yes', 'install'],
  False: ['/usr/bin/apt-get', '-q', '-o', "Dpkg::Options::=--force-confdef", '--allow-unauthenticated', '--assume-yes', 'install'],
}
REMOVE_CMD = {
  True: ['/usr/bin/apt-get', '-y', 'remove'],
  False: ['/usr/bin/apt-get', '-y', '-q', 'remove'],
}
REPO_UPDATE_CMD = ['/usr/bin/apt-get', 'update','-qq']

EMPTY_FILE = "/dev/null"
APT_SOURCES_LIST_DIR = "/etc/apt/sources.list.d"

CHECK_CMD = "dpkg --get-selections | grep -v deinstall | awk '{print $1}' | grep ^%s$"
VERIFY_DEPENDENCY_CMD = ['/usr/bin/apt-get', '-qq', 'check']

REPO_URL_EXCLUDE = "ubuntu.com"
CONFIGURATION_DUMP_CMD = [AMBARI_SUDO_BINARY, "apt-config", "dump"]
ALL_AVAILABLE_PACKAGES_DUMP_CMD = [AMBARI_SUDO_BINARY, "apt-cache", "dump"]
ALL_INSTALLED_PACKAGES_CMD = [AMBARI_SUDO_BINARY, "dpkg", "-l"]

# base command output sample:
# -----------------------------
#
# select | 2.6.3.0-63 | http://host/ubuntu16/2.x/BUILDS/2.6.3.0-63 main amd64 Packages
# select | 2.6.3.0-57 | http://host/ubuntu16/2.x/BUILDS/2.6.3.0-57 main amd64 Packages
# select | 2.6.3.0-55 | http://host/ubuntu16/2.x/BUILDS/2.6.3.0-55 main amd64 Packages

# repository update we doing at RepositoryProvider
LIST_ALL_SELECT_TOOL_PACKAGES_CMD = "apt-cache madison {pkg_name} 2>/dev/null|grep '^{pkg_name}' | cut -d '|' -f 2"
SELECT_TOOL_VERSION_PATTERN = re.compile("(\d{1,2}\.\d{1,2}\.\d{1,2}\.\d{1,2}-*\d*).*")  # xx.xx.xx.xx(-xxxx)


def replace_underscores(function_to_decorate):
  def wrapper(*args):
    self = args[0]
    name = args[1].replace("_", "-")
    return function_to_decorate(self, name, *args[2:])
  return wrapper


class AptProvider(PackageProvider):

  def __parse_select_tool_version(self, v):
    """
    :type v str
    """
    matches = SELECT_TOOL_VERSION_PATTERN.findall(v.strip())
    return matches[0] if matches else None

  def normalize_select_tool_versions(self, versions):
    """
    Function expect output from get_all_package_versions

    :type versions str|list|set
    :rtype list
    """
    if isinstance(versions, str):
      versions = [versions]

    return [self.__parse_select_tool_version(i) for i in versions]

  def __config_reader(self, stream):
    """
    apt-config dump command parser

    Function consumes io.TextIOBase compatible objects as input and return iterator with parsed items

    :type stream collections.Iterable
    :return tuple(key, value)

    Usage:
      for key, value in __config_reader(text_stream):
        ...

    Parsing subject:

       PROPERTY "";
       PROPERTY::ITEM1:: "value";
       .....

    """
    for line in stream:
      key, value = line.strip().split(" ", 1)
      key = key.strip("::")
      value = value.strip(";").strip("\"").strip()
      if not value:
        continue

      yield key, value

  def __packages_reader(self, stream):
    """
    apt-cache dump command parser

    Function consumes io.TextIOBase compatible objects as input and return iterator with parsed items

    :type stream collections.Iterable
    :return tuple(package name, version, parsed repo list file)

    Usage:
      for package, version, repo_file_path in __packages_reader(text_stream):
        ...

    Parsing subject:

        Package: test_package
     Version: 0.1.1-0
         File: /var/lib/apt/lists/some_site_dists_apt_main_binary-amd64_Packages.gz
     Description Language:
                     File: /var/lib/apt/lists/some_site_dists_apt_main_binary-amd64_Packages.gz
                      MD5: 000000000000000000000000000
    """
    fields = {"Package": 0, "Version": 1, "File": 2}
    field_names = fields.keys()
    field_count = len(field_names)
    item_set = [None] * field_count

    for line in stream:
      line = line.strip()

      if not line:
        continue

      values = line.split(":", 1)
      if len(values) != 2:
        continue

      field, value = values
      value = value[1:]

      if field in field_names:
        if field == "File":
          value = value.rpartition("/")[2]
        elif field == "Package":
          item_set = [None] * field_count  # reset fields which were parsed before new block
        item_set[fields[field]] = value
      else:
        continue

      if None not in item_set:
        yield item_set
        item_set = [None] * field_count

  def __packages_installed_reader(self, stream):
    """
    dpkg -l command parser

    Function consumes io.TextIOBase compatible objects as input and return iterator with parsed items

    :type stream collections.Iterable
    :return tuple(package name, version)

    Usage:
      for package, version in __packages_installed_reader(text_stream):
        ...

    Parsing subject:

      ||/ Name                              Version               Architecture          Description
      +++-=================================-=====================-=====================-======================
      ii  package1                           version1                all                   description1
      ii  package2                           version2                all                   description2
    """
    for line in stream:
      line = line.lstrip()

      if line[:2] != "ii":
        continue

      line = line[2:].lstrip()
      data = line.partition(" ")
      pkg_name = data[0]
      version = data[2].strip().partition(" ")[0]

      if pkg_name and version:
        yield pkg_name, version

  def _lookup_packages(self, command):
    """
    :type command list[str]|str
    """
    packages = []
    result = self._call_with_timeout(command)

    if result and 0 == result['retCode']:
      for x in result['out'].split('\n'):
        if x.strip():
          packages.append(x.split(' '))

    return packages

  def all_installed_packages(self, from_unknown_repo=False):
    """
    Return all installed packages in the system except packages in REPO_URL_EXCLUDE

    :arg from_unknown_repo return packages from unknown repos
    :type from_unknown_repo bool

    :return result_type formatted list of packages
    """
    packages = []
    available_packages = self.all_available_packages(result_type=dict, group_by_index=0)

    with process_executor(ALL_INSTALLED_PACKAGES_CMD, error_callback=self._executor_error_handler) as output:
      for package, version in self.__packages_installed_reader(output):
        if not from_unknown_repo and package in available_packages:
          packages.append(available_packages[package])

        if package not in available_packages:
          packages.append([package, version, "installed"])  # case, when some package not belongs to any known repo

    return packages

  def all_available_packages(self, result_type=list, group_by_index=-1):
    """
    Return all available packages in the system except packages in REPO_URL_EXCLUDE

    :arg result_type Could be list or dict, defines type of returning value
    :arg group_by_index index of element in the __packages_reader result, which would be used as key
    :return result_type formatted list of packages, including installed and available in repos

    :type result_type type
    :type group_by_index int
    :rtype list|dict
    """
    if result_type is not list and result_type is not dict:
      raise TypeError("result_type argument must be list or dict only")
    packages = result_type()

    with process_executor(ALL_AVAILABLE_PACKAGES_DUMP_CMD, error_callback=self._executor_error_handler) as output:
      for pkg_item in self.__packages_reader(output):
        if REPO_URL_EXCLUDE not in pkg_item[2]:
          if result_type is list:
            packages.append(pkg_item)
          elif result_type is dict:
            packages[pkg_item[group_by_index]] = pkg_item

    return packages

  def get_available_packages_in_repos(self, repos):
    """
    Gets all (both installed and available) packages that are available at given repositories.
    :type repos resource_management.libraries.functions.repository_util.CommandRepository
    :return: installed and available packages from these repositories
    """

    filtered_packages = []
    packages = self.all_available_packages()
    repo_ids = []

    for repo in repos.items:
      repo_ids.append(repo.base_url.replace("http://", "").replace("/", "_"))

    if repos.feat.scoped:
      Logger.info("Looking for matching packages in the following repositories: {0}".format(", ".join(repo_ids)))
      for repo_id in repo_ids:
        for package in packages:
          if repo_id in package[2]:
            filtered_packages.append(package[0])

      return filtered_packages
    else:
      Logger.info("Packages will be queried using all available repositories on the system.")
      return [package[0] for package in packages]

  def get_all_package_versions(self, pkg_name):
    """
    :type pkg_name str
    """
    command = LIST_ALL_SELECT_TOOL_PACKAGES_CMD.replace("{pkg_name}", pkg_name)
    result = self._call_with_timeout(command)

    if result["retCode"] == 0:
      return result["out"].split(os.linesep)

    return None

  def package_manager_configuration(self):
    """
    Reading apt configuration

    :return dict with apt properties
    """
    with process_executor(CONFIGURATION_DUMP_CMD, error_callback=self._executor_error_handler) as output:
      configuration = list(self.__config_reader(output))

    return dict(configuration)

  def get_installed_package_version(self, package_name):
    code, out, err = self.checked_call("dpkg -s {0} | grep Version | awk '{{print $2}}'".format(package_name), stderr=subprocess.PIPE)
    return out

  def verify_dependencies(self):
    """
    Verify that we have no dependency issues in package manager. Dependency issues could appear because of aborted or terminated
    package installation process or invalid packages state after manual modification of packages list on the host

    :return True if no dependency issues found, False if dependency issue present
    :rtype bool
    """
    code, out = self.checked_call(VERIFY_DEPENDENCY_CMD, sudo=True)
    pattern = re.compile("has missing dependency|E:")

    if code or (out and pattern.search(out)):
      err_msg = Logger.filter_text("Failed to verify package dependencies. Execution of '%s' returned %s. %s" % (VERIFY_DEPENDENCY_CMD, code, out))
      Logger.error(err_msg)
      return False

    return True

  @replace_underscores
  def install_package(self, name, use_repos={}, skip_repos=set(), is_upgrade=False):
    if is_upgrade or use_repos or not self._check_existence(name):
      cmd = INSTALL_CMD[self.get_logoutput()]
      copied_sources_files = []
      is_tmp_dir_created = False
      if use_repos:
        if 'base' in use_repos:
          use_repos = set([v for k,v in use_repos.items() if k != 'base'])
        else:
          cmd = cmd + ['-o', 'Dir::Etc::SourceList=%s' % EMPTY_FILE]
          use_repos = set(use_repos.values())

        if use_repos:
          is_tmp_dir_created = True
          apt_sources_list_tmp_dir = tempfile.mkdtemp(suffix="-ambari-apt-sources-d")
          Logger.info("Temporary sources directory was created: %s" % apt_sources_list_tmp_dir)

          for repo in use_repos:
            new_sources_file = os.path.join(apt_sources_list_tmp_dir, repo + '.list')
            Logger.info("Temporary sources file will be copied: %s" % new_sources_file)
            sudo.copy(os.path.join(APT_SOURCES_LIST_DIR, repo + '.list'), new_sources_file)
            copied_sources_files.append(new_sources_file)
          cmd = cmd + ['-o', 'Dir::Etc::SourceParts=%s' % apt_sources_list_tmp_dir]

      cmd = cmd + [name]
      Logger.info("Installing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      self.checked_call_with_retries(cmd, sudo=True, env=INSTALL_CMD_ENV, logoutput=self.get_logoutput())

      if is_tmp_dir_created:
        for temporary_sources_file in copied_sources_files:
          Logger.info("Removing temporary sources file: %s" % temporary_sources_file)
          os.remove(temporary_sources_file)
        Logger.info("Removing temporary sources directory: %s" % apt_sources_list_tmp_dir)
        os.rmdir(apt_sources_list_tmp_dir)
    else:
      Logger.info("Skipping installation of existing package %s" % (name))
      

  def is_locked_output(self, out):
    return "Unable to lock the administration directory" in out

  def is_repo_error_output(self, out):
    return "Failure when receiving data from the peer" in out

  def get_repo_update_cmd(self):
    return REPO_UPDATE_CMD

  @replace_underscores
  def upgrade_package(self, name, use_repos={}, skip_repos=set(), is_upgrade=True):
    return self.install_package(name, use_repos, skip_repos, is_upgrade)

  @replace_underscores
  def remove_package(self, name):
    if self._check_existence(name):
      cmd = REMOVE_CMD[self.get_logoutput()] + [name]
      Logger.info("Removing package %s ('%s')" % (name, string_cmd_from_args_list(cmd)))
      self.checked_call_with_retries(cmd, sudo=True, logoutput=self.get_logoutput())
    else:
      Logger.info("Skipping removal of non-existing package %s" % (name))

  @replace_underscores
  def _check_existence(self, name): 
    """
    For regexp names:
    If only part of packages were installed during early canceling.
    Let's say:
    1. install hbase-2-3-.*
    2. Only hbase-2-3-1234 is installed, but is not hbase-2-3-1234-regionserver yet.
    3. We cancel the apt-get
    
    In that case this is bug of packages we require.
    And hbase-2-3-*-regionserver should be added to metainfo.xml.
    
    Checking existence should never fail in such a case for hbase-2-3-.*, otherwise it
    gonna break things like removing packages and some other things.
    
    Note: this method SHOULD NOT use apt-get (apt.cache is using dpkg not apt). Because a lot of issues we have, when customer have
    apt-get in inconsistant state (locked, used, having invalid repo). Once packages are installed
    we should not rely on that.
    """
    code, out = shell.call(CHECK_CMD % name)
    return not bool(code)