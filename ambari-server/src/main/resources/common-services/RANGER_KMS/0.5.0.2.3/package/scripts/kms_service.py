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

from resource_management.core.resources.system import Execute
from resource_management.core import shell
from resource_management.libraries.functions.format import format
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger

def kms_service(action='start'):
  import params

  env_dict = {'JAVA_HOME': params.java_home}
  if params.db_flavor.lower() == 'sqla':
    env_dict = {'JAVA_HOME': params.java_home, 'LD_LIBRARY_PATH': params.ld_library_path}

  if action == 'start':
    no_op_test = format('ps -ef | grep proc_rangerkms | grep -v grep')
    cmd = format('{kms_home}/ranger-kms start')
    Execute(cmd, not_if=no_op_test, environment=env_dict, user=format('{kms_user}'))
  elif action == 'stop':
    cmd = format('{kms_home}/ranger-kms stop')
    Execute(cmd, environment=env_dict, user=format('{kms_user}'))
