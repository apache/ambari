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
__all__ = ["Group", "User"]

from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument


class Group(Resource):
  action = ForcedListArgument(default="create")
  group_name = ResourceArgument(default=lambda obj: obj.name)
  gid = ResourceArgument()
  password = ResourceArgument()

  actions = Resource.actions + ["create", "remove"]


class User(Resource):
  action = ForcedListArgument(default="create")
  username = ResourceArgument(default=lambda obj: obj.name)
  comment = ResourceArgument()
  uid = ResourceArgument()
  gid = ResourceArgument()
  """
  If the user exists, and there are some groups, appends to existant
  """
  groups = ForcedListArgument(default=[]) # supplementary groups
  home = ResourceArgument()
  shell = ResourceArgument()
  password = ResourceArgument()
  system = BooleanArgument(default=False)
  """
  On some envs, with ldap enabled, fetching information
  for all the groups - grp.getgrall() - is pretty slow.
  This parameter enables us fetching information about only local users (which is fast).
  """
  fetch_nonlocal_groups = BooleanArgument(default=True)

  actions = Resource.actions + ["create", "remove"]
