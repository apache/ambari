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

import os
import re
from os.path import dirname
from setuptools import find_packages, setup

AMBARI_COMMON_PYTHON_FOLDER = "ambari-common/src/main/python"


def get_ambari_common_packages():
  return find_packages(
    AMBARI_COMMON_PYTHON_FOLDER, exclude=["*.tests", "*.tests.*", "tests.*", "tests"]
  )


def create_package_dir_map():
  package_dirs = {}
  ambari_common_packages = get_ambari_common_packages()
  for ambari_common_package in ambari_common_packages:
    package_dirs[ambari_common_package] = (
      AMBARI_COMMON_PYTHON_FOLDER + "/" + ambari_common_package.replace(".", "/")
    )

  return package_dirs


__version__ = "3.1.0.0.dev0"


def normalize_version(version):
  version = version.strip()
  if version.endswith("-SNAPSHOT"):
    version = version[: -len("-SNAPSHOT")] + ".dev0"
  if not re.fullmatch(r"[0-9]+(?:\.[0-9]+)*(?:\.dev[0-9]+)?", version):
    raise ValueError(f"Unsupported Ambari Python package version: {version}")
  return version


def get_version():
  """
  Obtain ambari version during the build from pom.xml, which will be stored in PKG-INFO file.
  During installation from pip, pom.xml is not included in the distribution but PKG-INFO is, so it can be used
  instead of pom.xml file. If for some reason both are not exists use the default __version__ variable.
  All of these can be overridden by AMBARI_VERSION environment variable.
  """
  base_dir = dirname(__file__)
  if "AMBARI_VERSION" in os.environ:
    return normalize_version(os.environ["AMBARI_VERSION"])
  elif os.path.exists(os.path.join(base_dir, "pom.xml")):
    from xml.etree import ElementTree as et

    ns = "http://maven.apache.org/POM/4.0.0"
    et.register_namespace("", ns)
    tree = et.ElementTree()
    tree.parse(os.path.join(base_dir, "pom.xml"))
    root = tree.getroot()
    version_tag = root.find("{%s}version" % ns)
    version = version_tag.text if version_tag is not None else __version__
    property_match = re.fullmatch(r"\$\{([^}]+)\}", version or "")
    if property_match:
      property_tag = root.find(
        "{%s}properties/{%s}%s" % (ns, ns, property_match.group(1))
      )
      if property_tag is None or not property_tag.text:
        raise ValueError(f"Unable to resolve Maven version property {version}")
      version = property_tag.text
    return normalize_version(version)
  elif os.path.exists(os.path.join(base_dir, "PKG-INFO")):
    version_re = re.compile("^Version: (.+)$", re.M)
    with open(os.path.join(base_dir, "PKG-INFO"), encoding="utf-8") as f:
      match = version_re.search(f.read())
    return normalize_version(match.group(1)) if match is not None else __version__
  else:
    return __version__


"""
Example usage:
- build the source distribution from the locked build environment:
  python3 -m build --sdist --no-isolation
- install the source distribution into an isolated target:
  python3 -m pip install --no-build-isolation --target "my/site-packages" dist/ambari-python-*.tar.gz

Installing from pip:
- python3 -m pip install ambari-python==3.1.0.0.dev0
"""
setup(
  version=get_version(),
  packages=get_ambari_common_packages(),
  package_dir=create_package_dir_map(),
  include_package_data=True,
)
