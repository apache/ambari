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
from os.path import dirname
from setuptools import find_packages, setup

AMBARI_COMMON_PYTHON_FOLDER = "ambari-common/src/main/python"
AMBARI_SERVER_TEST_PYTHON_FOLDER = "ambari-server/src/test/python"

def get_ambari_common_packages():
  return find_packages(AMBARI_COMMON_PYTHON_FOLDER, exclude=["*.tests", "*.tests.*", "tests.*", "tests"])

def get_ambari_server_stack_package():
  return ["stacks.utils"]

def create_package_dir_map():
  package_dirs = {}
  ambari_common_packages = get_ambari_common_packages()
  for ambari_common_package in ambari_common_packages:
    package_dirs[ambari_common_package] = AMBARI_COMMON_PYTHON_FOLDER + '/' + ambari_common_package.replace(".", "/")

  ambari_server_packages = get_ambari_server_stack_package()
  for ambari_server_package in ambari_server_packages:
    package_dirs[ambari_server_package] = AMBARI_SERVER_TEST_PYTHON_FOLDER + '/' + ambari_server_package.replace(".", "/")
  return package_dirs

def get_version():
  ambari_version = os.environ["AMBARI_VERSION"] if "AMBARI_VERSION" in os.environ else "3.0.0.dev0"
  print ambari_version
  return ambari_version

"""
Example usage:
- build package with specific version:
  export AMBARI_VERSION=2.7.1; python setup.py sdist -d "my/dist/location"
- build and install package with specific version:
  export AMBARI_VERSION=2.7.1; python setup.py sdist -d "my/dist/location" install
- build and upload package with specific version:
  export AMBARI_VERSION=2.7.1; python setup.py sdist -d "my/dist/location" upload -r "http://localhost:8080"
"""
setup(
  name = "ambari-python",
  version = get_version(),
  author = "Apache Software Foundation",
  author_email = "dev@ambari.apache.org",
  description = ("Framework for provison/manage/monitor Hadoop clusters"),
  license = "AP2",
  keywords = "hadoop, ambari",
  url = "https://ambari.apache.org",
  packages = get_ambari_common_packages() + get_ambari_server_stack_package(),
  package_dir = create_package_dir_map(),
  include_package_data = True,
  long_description="The Apache Ambari project is aimed at making Hadoop management simpler by developing software for provisioning, managing, and monitoring Apache Hadoop clusters. "
                   "Ambari provides an intuitive, easy-to-use Hadoop management web UI backed by its RESTful APIs."
)
