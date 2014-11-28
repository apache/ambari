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

# os.chown replacement
def chown(path, owner, group):
  if owner:
    shell.checked_call(["chown", owner, path], sudo=True)
  if group:
    shell.checked_call(["chgrp", group, path], sudo=True)
    
# os.chmod replacement
def chmod(path, mode):
  shell.checked_call(["chmod", oct(mode), path], sudo=True)
  
# os.makedirs replacement
def makedirs(path, mode):
  shell.checked_call(["mkdir", "-p", path], sudo=True)
  chmod(path, mode)
  
# os.makedir replacement
def makedir(path, mode):
  shell.checked_call(["mkdir", path], sudo=True)
  chmod(path, mode)
  
# os.symlink replacement
def symlink(source, link_name):
  shell.checked_call(["ln","-sf", source, link_name], sudo=True)
  
# os.link replacement
def link(source, link_name):
  shell.checked_call(["ln", "-f", source, link_name], sudo=True)
  
# os unlink
def unlink(path):
  shell.checked_call(["rm","-f", path], sudo=True)
  
# fp.write replacement
def create_file(filename, content):
  """
  if content is None, create empty file
  """
  tmpf = tempfile.NamedTemporaryFile()
  
  if content:
    with open(tmpf.name, "wb") as fp:
      fp.write(content)
  
  with tmpf:    
    shell.checked_call(["cp", "-f", tmpf.name, filename], sudo=True)
    
  # set default files mode
  chmod(filename, 0644)
    
# fp.read replacement
def read_file(filename):
  tmpf = tempfile.NamedTemporaryFile()
  shell.checked_call(["cp", "-f", filename, tmpf.name], sudo=True)
  
  with tmpf:
    with open(tmpf.name, "rb") as fp:
      return fp.read()
