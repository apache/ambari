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

import os
import tempfile
from resource_management.core import shell

def get_user_call_output(command, user, is_checked_call=True, **call_kwargs):
  """
  This function eliminates only output of command inside the su, ignoring the su ouput itself.
  This is useful since some users have motd messages setup by default on su -l. 
  
  @return: code, stdout, stderr
  """
  command_string = shell.string_cmd_from_args_list(command) if isinstance(command, (list, tuple)) else command
  out_files = []
  
  try:
    out_files.append(tempfile.NamedTemporaryFile())
    out_files.append(tempfile.NamedTemporaryFile())
    
    # other user should be able to write to it
    for f in out_files:
      os.chmod(f.name, 0666)
    
    command_string += " 1>" + out_files[0].name
    command_string += " 2>" + out_files[1].name
    
    func = shell.checked_call if is_checked_call else shell.call
    func_result = func(shell.as_user(command_string, user), **call_kwargs)
    
    files_output = []
    for f in out_files:
      files_output.append(f.read())
    
    return func_result[0], files_output[0], files_output[1]
  finally:
    for f in out_files:
      f.close()
      
  