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

__all__ = ["select", "create"]

import version
from resource_management.core import shell

TEMPLATE = "conf-select {0} --package {1} --stack-version {2} --conf-version 0"

def _valid(stack_name, package, ver):
  if stack_name != "HDP":
    return False

  if version.compare_versions(version.format_hdp_stack_version(ver), "2.3.0.0") < 0:
    return False

  return True

def create(stack_name, package, version):
  """
  Creates a config version for the specified package
  :stack_name: the name of the stack
  :package: the name of the package, as-used by conf-select
  :version: the version number to create
  """

  if not _valid(stack_name, package, version):
    return

  shell.call(TEMPLATE.format("create-conf-dir", package, version))


def select(stack_name, package, version, try_create=True):
  """
  Selects a config version for the specified package.  Currently only works if the version is
  for HDP-2.3 or higher
  :stack_name: the name of the stack
  :package: the name of the package, as-used by conf-select
  :version: the version number to create
  :try_create: optional argument to attempt to create the directory before setting it
  """

  if not _valid(stack_name, package, version):
    return

  if try_create:
    create(stack_name, package, version)

  shell.call(TEMPLATE.format("set-conf-dir", package, version), logoutput=True)

