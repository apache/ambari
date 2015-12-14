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

__all__ = ["File", "Directory", "Link", "Execute", "ExecuteScript", "Mount"]

import subprocess
from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument


class File(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  backup = ResourceArgument()
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  content = ResourceArgument()
  # whether to replace files with different content
  replace = ResourceArgument(default=True)
  encoding = ResourceArgument()
  """
  Grants x-bit for all the folders up-to the file
  
  u - user who is owner
  g - user from group
  o - other users
  a - all
  
  The letters can be combined together.
  """
  cd_access = ResourceArgument()

  actions = Resource.actions + ["create", "delete"]


class Directory(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  mode = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  follow = BooleanArgument(default=True)  # follow links?
  """
  this works for 'create', 'delete' is anyway recursive
  recursive means only "mkdir -p", it does NOT perform recursive chown/chmod
  """
  recursive = BooleanArgument(default=False)
  """
  Grants x-bit for all the folders up-to the directory
  
  u - user who is owner
  g - user from group
  o - other users
  a - all
  
  The letters can be combined together.
  """
  cd_access = ResourceArgument()

  actions = Resource.actions + ["create", "delete"]


class Link(Resource):
  action = ForcedListArgument(default="create")
  path = ResourceArgument(default=lambda obj: obj.name)
  to = ResourceArgument(required=True)
  hard = BooleanArgument(default=False)

  actions = Resource.actions + ["create", "delete"]


class Execute(Resource):
  action = ForcedListArgument(default="run")
  
  """
  Recommended:
  command = ('rm','-f','myfile')
  Not recommended:
  command = 'rm -f myfile'
  
  The first one helps to stop escaping issues
  """
  command = ResourceArgument(default=lambda obj: obj.name)
  
  creates = ResourceArgument()
  """
  cwd won't work for:
  - commands run as sudo
  - commands run as user (which uses sudo as well)
  
  This is because non-interactive sudo commands doesn't support that.
  """
  cwd = ResourceArgument()
  # this runs command with a specific env variables, env={'JAVA_HOME': '/usr/jdk'}
  environment = ResourceArgument(default={})
  user = ResourceArgument()
  group = ResourceArgument()
  returns = ForcedListArgument(default=0)
  tries = ResourceArgument(default=1)
  try_sleep = ResourceArgument(default=0) # seconds
  path = ForcedListArgument(default=[])
  actions = Resource.actions + ["run"]
  # TODO: handle how this is logged / tested?
  """
  A one-argument function, which will be executed,
  once new line comes into command output.
  
  The only parameter of this function is a new line which comes to output.
  """
  on_new_line = ResourceArgument()
  """
  True           -  log it in INFO mode
  False          -  never log it
  None (default) -  log it in DEBUG mode
  """
  logoutput = ResourceArgument(default=None)

  """
  if on_timeout is not set leads to failing after x seconds,
  otherwise calls on_timeout
  """
  timeout = ResourceArgument() # seconds
  on_timeout = ResourceArgument()
  """
  Wait for command to finish or not. 
  
  NOTE:
  In case of False, since any command results are skipped, it disables some functionality: 
  - non-zero return code failure
  - logoutput
  - tries
  - try_sleep
  """
  wait_for_finish = BooleanArgument(default=True)
  """
  For calling more advanced commands use as_sudo(command) option.
  Example:
  command1 = as_sudo(["cat,"/etc/passwd"]) + " | grep user"
  command2 = as_sudo(["ls", "/root/example.txt") + " && " + as_sudo(["rm","-f","example.txt"])
  """
  sudo = BooleanArgument(default=False)
  """
  subprocess.PIPE - enable output gathering
  None - disable output to gathering, and output to Python out straightly (even if logoutput is False)
  subprocess.STDOUT - redirect to stdout (not valid as value for stdout agument)
  {int fd} - redirect to file with descriptor.
  {string filename} - redirects to a file with name.
  """
  stdout = ResourceArgument(default=subprocess.PIPE)
  stderr = ResourceArgument(default=subprocess.STDOUT)

class ExecuteScript(Resource):
  action = ForcedListArgument(default="run")
  code = ResourceArgument(required=True)
  cwd = ResourceArgument()
  environment = ResourceArgument()
  interpreter = ResourceArgument(default="/bin/bash")
  user = ResourceArgument()
  group = ResourceArgument()

  actions = Resource.actions + ["run"]


class Mount(Resource):
  action = ForcedListArgument(default="mount")
  mount_point = ResourceArgument(default=lambda obj: obj.name)
  device = ResourceArgument()
  fstype = ResourceArgument()
  options = ResourceArgument(default=["defaults"])
  dump = ResourceArgument(default=0)
  passno = ResourceArgument(default=2)

  actions = Resource.actions + ["mount", "umount", "remount", "enable",
                                "disable"]
