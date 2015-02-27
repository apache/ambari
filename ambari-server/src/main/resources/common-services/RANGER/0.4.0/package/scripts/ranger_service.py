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
from resource_management import *

def ranger_service(name):
  import params
  
  if name == 'ranger_admin':
    no_op_test = format('ps -ef | grep proc_rangeradmin | grep -v grep')
    Execute(format('{params.ranger_start}'), user=params.unix_user, not_if=no_op_test)
  elif name == 'ranger_usersync':
    no_op_test = format('ps -ef | grep proc_rangerusersync | grep -v grep')
    # Usersync requires to be run as root.
    Execute((params.usersync_start,), 
            not_if=no_op_test,
            sudo=True,
    )