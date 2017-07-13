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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from jkg_toree_params import py_executable, py_venv_pathprefix, py_venv_restrictive, venv_owner, ambarisudo
import jnbg_helpers as helpers

# Server configurations
config = Script.get_config()
stack_root = Script.get_stack_root()

package_dir = helpers.package_dir()
cmd_file_name = "pythonenv_setup.sh"
cmd_file_path = format("{package_dir}files/{cmd_file_name}")

# Sequence of commands executed in py_client.py
commands = [ambarisudo + ' ' +
            cmd_file_path + ' ' +
            py_executable + ' ' +
            py_venv_pathprefix + ' ' +
            venv_owner]
