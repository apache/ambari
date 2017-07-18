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

import os
from resource_management.core.resources.system import Execute, File, Directory
from resource_management.core.source import StaticFile, InlineTemplate, Template
from resource_management.core.resources.system import Execute
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
import jnbg_helpers as helpers

class PyClient(Script):
  def install(self, env):
    import py_client_params as params
    from jkg_toree_params import user, group, sh_scripts_dir, sh_scripts, sh_scripts_user

    # Setup bash scripts for execution
    for sh_script in sh_scripts:
      File(sh_scripts_dir + os.sep + sh_script,
           content=StaticFile(sh_script),
           mode=0750
          )
    for sh_script in sh_scripts_user:
      File(sh_scripts_dir + os.sep + sh_script,
           content=StaticFile(sh_script),
           mode=0755
          )

    self.install_packages(env)
    self.configure(env)

    # Create user and group if they don't exist
    helpers.create_linux_user(user, group)

    # Run install commands for Python client defined in params
    for command in params.commands: Execute(command, logoutput=True)

  def status(self, env):
    raise ClientComponentHasNoStatus()

  def configure(self, env):
    import py_client_params as params
    env.set_params(params)

if __name__ == "__main__":
  PyClient().execute()
