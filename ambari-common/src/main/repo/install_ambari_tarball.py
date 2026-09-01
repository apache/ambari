#!/usr/bin/env python3
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
import os
import sys
import logging
import subprocess
import argparse
import configparser
import shutil
import tarfile

USAGE = "%(prog)s [OPTION]... URL"
DESCRIPTION = "URL should point to full tar.gz location e.g.: https://public-repo-1.hortonworks.com/something/ambari-server.tar.gz"

logger = logging.getLogger("install_ambari_tarball")

PREINST_SCRIPT = "preinst"
PRERM_SCRIPT = "prerm"
POSTINST_SCRIPT = "postinst"
POSTRM_SCRIPT = "postrm"
OS_CHECK = "os_check.py"
OS_PACKAGE_DEPENDENCIES = "dependencies.properties"
OS_FAMILY_DESCRIPTION = "resources/os_family.json"
RPM_DEPENDENCIES_PROPERTY = "rpm.dependency.list"
DEB_DEPENDENCIES_PROPERTY = "deb.dependency.list"

FILES_TO_DOWNLOAD = [
  PREINST_SCRIPT,
  PRERM_SCRIPT,
  POSTINST_SCRIPT,
  POSTRM_SCRIPT,
  OS_CHECK,
  OS_FAMILY_DESCRIPTION,
  OS_PACKAGE_DEPENDENCIES,
]

ROOT_FOLDER_ENV_VARIABLE = "RPM_INSTALL_PREFIX"


def dependency_property_names(property_prefix, os_family, os_version):
  family_versions = [
    f"{os_family}{version}" for version in range(int(os_version), 0, -1)
  ]
  return [
    f"{property_prefix}.{postfix}" if postfix else property_prefix
    for postfix in family_versions + [os_family, ""]
  ]


def dependency_expressions(packages_string):
  normalized = re.sub(r"Requires\s*:", "", packages_string)
  normalized = normalized.replace("\\n", "")
  return [expression.strip() for expression in normalized.split(",") if expression.strip()]


def rpm_dependency_alternatives(expression):
  expression = expression.strip()
  if expression.startswith("(") and expression.endswith(")"):
    expression = expression[1:-1].strip()
  return [alternative.strip() for alternative in re.split(r"\s+or\s+", expression)]


def deb_dependency_parts(expression):
  match = re.fullmatch(
    r"(?P<name>[A-Za-z0-9][A-Za-z0-9+.-]*)"
    r"(?:\s*\(\s*(?P<operator><<|<=|=|>=|>>)\s*(?P<version>[^)]+)\s*\))?",
    expression,
  )
  if match is None:
    raise ValueError(f"Unsupported Debian dependency expression: {expression}")
  return match.group("name"), match.group("operator"), match.group("version")


class Utils:
  verbose = False

  @staticmethod
  def os_call(command, logoutput=None, env=None):
    process_env = os.environ.copy()
    if env:
      process_env.update(env)
    shell = not isinstance(command, list)
    print_output = logoutput == True or (logoutput == None and Utils.verbose)

    if not print_output:
      stdout = subprocess.PIPE
      stderr = subprocess.STDOUT
    else:
      stdout = stderr = None

    logger.info(f"Running '{command}'")
    proc = subprocess.Popen(
      command,
      shell=shell,
      stdout=stdout,
      stderr=stderr,
      env=process_env,
      universal_newlines=True,
    )

    if not print_output:
      out = proc.communicate()[0].strip("\n")
    else:
      proc.wait()
      out = None

    code = proc.returncode

    if code:
      err_msg = ("Execution of '%s'\n returned %d. %s") % (command, code, out)
      raise OsCallFailure(err_msg)

    return out

  @staticmethod
  def dependency_is_installed(expression, is_rpm):
    if is_rpm:
      Utils.os_call(["rpm", "-q", "--whatprovides", expression], logoutput=False)
      return True

    name, operator, required_version = deb_dependency_parts(expression)
    installed_version = Utils.os_call(
      ["dpkg-query", "-W", "-f=${Version}", name], logoutput=False
    )
    if operator is not None:
      Utils.os_call(
        ["dpkg", "--compare-versions", installed_version, operator, required_version],
        logoutput=False,
      )
    return True

  @staticmethod
  def install_dependency(expression, is_rpm):
    alternatives = rpm_dependency_alternatives(expression) if is_rpm else [expression]
    logger.info("Checking dependency %s", expression)

    for alternative in alternatives:
      try:
        if Utils.dependency_is_installed(alternative, is_rpm):
          logger.info("Dependency %s is already satisfied", alternative)
          return
      except OsCallFailure:
        pass

    failures = []
    for alternative in alternatives:
      if is_rpm:
        package_match = re.fullmatch(
          r"([A-Za-z0-9][A-Za-z0-9+_.:-]*)(?:\s*[<>=]+\s*.+)?", alternative
        )
        install_target = package_match.group(1) if package_match else alternative
        install_cmd = ["sudo", "yum", "-y", "install", install_target]
      else:
        install_target = deb_dependency_parts(alternative)[0]
        install_cmd = ["sudo", "apt-get", "-y", "install", install_target]

      logger.info("Installing dependency candidate %s", install_target)
      try:
        Utils.os_call(install_cmd)
        if Utils.dependency_is_installed(alternative, is_rpm):
          return
      except OsCallFailure as exception:
        failures.append(str(exception))

    detail = "; ".join(failures) if failures else "installed version is incompatible"
    raise OsCallFailure(f"Unable to satisfy dependency {expression}: {detail}")

  @staticmethod
  def install_package(name):
    from os_check import OSCheck

    is_rpm = not OSCheck.is_ubuntu_family()
    try:
      Utils.install_dependency(name, is_rpm)
    except ValueError as exception:
      raise OsCallFailure(str(exception)) from exception


class FakePropertiesHeader(object):
  """
  Hacky class to parse properties file without sections.
  see http://stackoverflow.com/questions/2819696/module-to-use-when-parsing-properties-file-in-python/2819788#2819788
  """

  FAKE_SECTION_NAME = "section"

  def __init__(self, fp):
    self.fp = fp
    self.sechead = f"[{FakePropertiesHeader.FAKE_SECTION_NAME}]\n"

  def readline(self):
    if self.sechead:
      try:
        return self.sechead
      finally:
        self.sechead = None
    else:
      return self.fp.readline()


class OsCallFailure(RuntimeError):
  pass


class Installer:
  def __init__(self, archive_url, root_folder, verbose, skip_dependencies):
    splited_url = archive_url.split("/")
    self.archive_name = splited_url[-1]
    self.base_url = "/".join(splited_url[0:-1])
    self.root_folder = root_folder
    self.verbose = verbose
    self.skip_dependencies = skip_dependencies

  def download_files(self, files_list):
    for name in files_list:
      dirname = os.path.dirname(name)
      if dirname:
        Utils.os_call(["mkdir", "-p", dirname])

      url = f"{self.base_url}/{name}"
      logger.info(f"Downloading {url}")
      Utils.os_call(["wget", "-O", name, url])

  def run(self):
    self.download_files(
      [self.archive_name] + FILES_TO_DOWNLOAD
    )  # [self.archive_name] +

    self.check_dependencies()
    self.run_script(PRERM_SCRIPT, ["remove"])  # in case we are upgrading
    self.run_script(POSTRM_SCRIPT, ["remove"])  # in case we are upgrading

    self.run_script(PREINST_SCRIPT, ["install"])
    self.remove_replaced_library_roots()
    self.extract_archive()
    self.run_script(POSTINST_SCRIPT, ["configure"])

  def remove_replaced_library_roots(self):
    archive_roots = set()
    with tarfile.open(self.archive_name, "r:*") as archive:
      for member in archive.getmembers():
        name = member.name.lstrip("./")
        for library_root in (
          "usr/lib/ambari-agent/lib",
          "usr/lib/ambari-server/lib",
        ):
          if name == library_root or name.startswith(f"{library_root}/"):
            archive_roots.add(library_root)

    installation_root = os.path.realpath(self.root_folder)
    for library_root in archive_roots:
      target = os.path.realpath(os.path.join(installation_root, library_root))
      if os.path.commonpath((installation_root, target)) != installation_root:
        raise ValueError(f"Archive library root escapes installation root: {target}")
      if os.path.islink(target) or os.path.isfile(target):
        os.unlink(target)
      elif os.path.isdir(target):
        shutil.rmtree(target)

  def check_dependencies(self):
    from os_check import OSCheck

    os_family = OSCheck.get_os_family()
    os_version = OSCheck.get_os_major_version()

    is_rpm = not OSCheck.is_ubuntu_family()
    property_prefix = RPM_DEPENDENCIES_PROPERTY if is_rpm else DEB_DEPENDENCIES_PROPERTY

    cp = configparser.ConfigParser()
    with open(OS_PACKAGE_DEPENDENCIES, encoding="utf-8") as fp:
      cp.read_file(FakePropertiesHeader(fp))

    properties = dict(cp.items(FakePropertiesHeader.FAKE_SECTION_NAME))

    packages_string = None
    for property_name in dependency_property_names(
      property_prefix, os_family, os_version
    ):
      if property_name in properties:
        packages_string = properties[property_name]
        break

    if packages_string is None:
      err_msg = "No os dependencies found. "
      if self.skip_dependencies:
        logger.warning(err_msg)
        return
      else:
        raise Exception(err_msg)

    dependencies = dependency_expressions(packages_string)

    if self.skip_dependencies:
      var = input(
        "Please confirm you have the following dependencies installed "
        f"{', '.join(dependencies)} (y/n): "
      )
      if var.lower() != "y" and var.lower() != "yes":
        raise Exception("User canceled the installation.")
      return

    for dependency in dependencies:
      Utils.install_dependency(dependency, is_rpm)

  def run_script(self, script_name, args):
    bash_args = []
    if self.verbose:
      bash_args.append("-x")

    Utils.os_call(
      ["bash"] + bash_args + [script_name] + args,
      env={ROOT_FOLDER_ENV_VARIABLE: self.root_folder},
      logoutput=True,
    )


class TargzInstaller(Installer):
  def extract_archive(self):
    Utils.os_call(
      [
        "tar",
        "--no-same-owner",
        "-xvf",
        self.archive_name,
        "-C",
        self.root_folder + os.sep,
      ],
      logoutput=False,
    )


class Runner:
  def parse_opts(self, arguments=None):
    parser = argparse.ArgumentParser(usage=USAGE, description=DESCRIPTION)
    parser.add_argument(
      "-v",
      "--verbose",
      dest="verbose",
      action="store_true",
      help="sets output level to more detailed",
    )
    parser.add_argument(
      "-r",
      "--root-folder",
      dest="root_folder",
      default="/",
      help="root folder to install Ambari to. E.g.: /opt",
    )
    parser.add_argument(
      "-d",
      "--dependencies-skip",
      dest="skip_dependencies",
      action="store_true",
      help="the script won't install the package dependencies. Please make sure to install them manually.",
    )
    parser.add_argument("url", help="Ambari tarball URL")

    self.options = parser.parse_args(arguments)
    self.url = self.options.url

  @staticmethod
  def setup_logger(verbose):
    logging_level = logging.DEBUG if verbose else logging.INFO
    logger.setLevel(logging_level)

    formatter = logging.Formatter("%(asctime)s %(levelname)s %(message)s")
    stdout_handler = logging.StreamHandler(sys.stdout)
    stdout_handler.setLevel(logging_level)
    stdout_handler.setFormatter(formatter)
    logger.addHandler(stdout_handler)

  def run(self):
    self.parse_opts()
    Runner.setup_logger(self.options.verbose)
    Utils.verbose = self.options.verbose

    # TODO: check if ends with tar.gz?
    targz_installer = TargzInstaller(
      self.url,
      self.options.root_folder,
      self.options.verbose,
      self.options.skip_dependencies,
    )
    targz_installer.run()


if __name__ == "__main__":
  Runner().run()
