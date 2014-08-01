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

Ambari Agent

"""

from resource_management.core.base import Fail
from resource_management.core.providers import Provider


class PackageProvider(Provider):
  def __init__(self, *args, **kwargs):
    super(PackageProvider, self).__init__(*args, **kwargs)   
  
  def install_package(self, name, version):
    raise NotImplementedError()
  def remove_package(self, name):
    raise NotImplementedError()
  def upgrade_package(self, name, version):
    raise NotImplementedError()

  def action_install(self):
    package_name = self.get_package_name_with_version()
    self.install_package(package_name)

  def action_upgrade(self):
    package_name = self.get_package_name_with_version()
    self.upgrade_package(package_name)

  def action_remove(self):
    package_name = self.get_package_name_with_version()
    self.remove_package(package_name)

  def get_package_name_with_version(self):
    if self.resource.version:
      return self.resource.package_name + '-' + self.resource.version
    else:
      return self.resource.package_name
    
