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

import time
import os
import tempfile
import shutil
import stat
from resource_management.core import shell
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail
from ambari_commons.os_check import OSCheck
import subprocess

if os.geteuid() == 0:
  def chown(path, owner, group):
    uid = owner.pw_uid if owner else -1
    gid = group.gr_gid if group else -1
    if uid != -1 or gid != -1:
      return os.chown(path, uid, gid)
  
  def chmod(path, mode):
    return os.chmod(path, mode)
  
  mode_to_stat = {"a+x": stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH, "a+rx": stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH | stat.S_IRUSR | stat.S_IRGRP | stat.S_IROTH, "u+x": stat.S_IXUSR, "g+x": stat.S_IXGRP,  "o+x": stat.S_IXOTH}
  def chmod_extended(path, mode):
    if mode in mode_to_stat:
      st = os.stat(path)
      os.chmod(path, st.st_mode | mode_to_stat[mode])
    else:
      shell.checked_call(["chmod", mode, path])
      
  def copy(src, dst):
    shutil.copy(src, dst)
    
  def makedirs(path, mode):
    os.makedirs(path, mode)
  
  def makedir(path, mode):
    os.mkdir(path)
    
  def symlink(source, link_name):
    os.symlink(source, link_name)
    
  def link(source, link_name):
    os.link(source, link_name)
    
  def unlink(path):
    os.unlink(path)

  def rmtree(path):
    shutil.rmtree(path)
    
  def create_file(filename, content, encoding=None):
    """
    if content is None, create empty file
    """
    with open(filename, "wb") as fp:
      if content:
        content = content.encode(encoding) if encoding else content
        fp.write(content)
      
  def read_file(filename, encoding=None):
    with open(filename, "rb") as fp:
      content = fp.read()
        
    content = content.decode(encoding) if encoding else content
    return content
      
  def path_exists(path):
    return os.path.exists(path)
  
  def path_isdir(path):
    return os.path.isdir(path)
  
  def path_lexists(path):
    return os.path.lexists(path)
  
  def readlink(path):
    return os.readlink(path)
  
  def path_isfile(path):
    return os.path.isfile(path)

  def stat(path):
    class Stat:
      def __init__(self, path):
        stat_val = os.stat(path)
        self.st_uid, self.st_gid, self.st_mode = stat_val.st_uid, stat_val.st_gid, stat_val.st_mode & 07777
    return Stat(path)
  
  def kill(pid, signal):
    os.kill(pid, signal)
    
    
else:

  # os.chown replacement
  def chown(path, owner, group):
    owner = owner.pw_name if owner else ""
    group = group.gr_name if group else ""
    if owner or group:
      shell.checked_call(["chown", owner+":"+group, path], sudo=True)
      
  # os.chmod replacement
  def chmod(path, mode):
    shell.checked_call(["chmod", oct(mode), path], sudo=True)
    
  def chmod_extended(path, mode):
    shell.checked_call(["chmod", mode, path], sudo=True)
    
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
    
  # shutil.rmtree
  def rmtree(path):
    shell.checked_call(["rm","-rf", path], sudo=True)
    
  # fp.write replacement
  def create_file(filename, content, encoding=None):
    """
    if content is None, create empty file
    """
    content = content if content else ""
    content = content.encode(encoding) if encoding else content
    
    tmpf_name = tempfile.gettempdir() + os.sep + tempfile.template + str(time.time())
    
    try:
      with open(tmpf_name, "wb") as fp:
        fp.write(content)
        
      shell.checked_call(["cp", "-f", tmpf_name, filename], sudo=True)
    finally:
      os.unlink(tmpf_name)
      
  # fp.read replacement
  def read_file(filename, encoding=None):
    tmpf = tempfile.NamedTemporaryFile()
    shell.checked_call(["cp", "-f", filename, tmpf.name], sudo=True)
    
    with tmpf:
      with open(tmpf.name, "rb") as fp:
        content = fp.read()
        
    content = content.decode(encoding) if encoding else content
    return content
      
  # os.path.exists
  def path_exists(path):
    return (shell.call(["test", "-e", path], sudo=True)[0] == 0)
  
  # os.path.isdir
  def path_isdir(path):
    return (shell.call(["test", "-d", path], sudo=True)[0] == 0)
  
  # os.path.lexists
  def path_lexists(path):
    return (shell.call(["test", "-L", path], sudo=True)[0] == 0)
  
  # os.readlink
  def readlink(path):
    return shell.checked_call(["readlink", path], sudo=True)[1].strip()
  
  # os.path.isfile
  def path_isfile(path):
    return (shell.call(["test", "-f", path], sudo=True)[0] == 0)

  # os.stat
  def stat(path):
    class Stat:
      def __init__(self, path):
        cmd = ["stat", "-c", "%u %g %a", path]
        code, out, err = shell.checked_call(cmd, sudo=True, stderr=subprocess.PIPE)
        values = out.split(' ')
        if len(values) != 3:
          raise Fail("Execution of '{0}' returned unexpected output. {2}\n{3}".format(cmd, code, err, out))
        uid_str, gid_str, mode_str = values
        self.st_uid, self.st_gid, self.st_mode = int(uid_str), int(gid_str), int(mode_str, 8)
  
    return Stat(path)
  
  # os.kill replacement
  def kill(pid, signal):
    try:
      shell.checked_call(["kill", "-"+str(signal), str(pid)], sudo=True)
    except Fail as ex:
      raise OSError(str(ex))
    
  # shutil.copy replacement
  def copy(src, dst):
    shell.checked_call(["sudo", "cp", "-r", src, dst], sudo=True)