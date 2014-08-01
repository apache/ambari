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

__all__ = ["Provider", "find_provider"]

from resource_management.core.exceptions import Fail
from resource_management.libraries.providers import PROVIDERS as LIBRARY_PROVIDERS


class Provider(object):
  def __init__(self, resource):
    self.resource = resource

  def action_nothing(self):
    pass

  def __repr__(self):
    return self.__unicode__()

  def __unicode__(self):
    return u"%s[%s]" % (self.__class__.__name__, self.resource)


PROVIDERS = dict(
  redhat=dict(
    Package="resource_management.core.providers.package.yumrpm.YumProvider",
  ),
  suse=dict(
    Package="resource_management.core.providers.package.zypper.ZypperProvider",
  ),
  debian=dict(
    Package="resource_management.core.providers.package.apt.AptProvider",
  ),
  default=dict(
    File="resource_management.core.providers.system.FileProvider",
    Directory="resource_management.core.providers.system.DirectoryProvider",
    Link="resource_management.core.providers.system.LinkProvider",
    Execute="resource_management.core.providers.system.ExecuteProvider",
    ExecuteScript="resource_management.core.providers.system.ExecuteScriptProvider",
    Mount="resource_management.core.providers.mount.MountProvider",
    User="resource_management.core.providers.accounts.UserProvider",
    Group="resource_management.core.providers.accounts.GroupProvider",
    Service="resource_management.core.providers.service.ServiceProvider",
  ),
)


def find_provider(env, resource, class_path=None):
  if not class_path:
    providers = [PROVIDERS, LIBRARY_PROVIDERS]
    for provider in providers:
      if resource in provider[env.system.os_family]:
        class_path = provider[env.system.os_family][resource]
        break
      if resource in provider["default"]:
        class_path = provider["default"][resource]
        break

  try:
    mod_path, class_name = class_path.rsplit('.', 1)
  except ValueError:
    raise Fail("Unable to find provider for %s as %s" % (resource, class_path))
  mod = __import__(mod_path, {}, {}, [class_name])
  return getattr(mod, class_name)
