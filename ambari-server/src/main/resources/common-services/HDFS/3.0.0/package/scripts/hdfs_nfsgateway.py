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

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources import Directory
from resource_management.core import shell
from utils import service
import subprocess,os

# NFS GATEWAY is always started by root using jsvc due to rpcbind bugs
# on Linux such as CentOS6.2. https://bugzilla.redhat.com/show_bug.cgi?id=731542

def prepare_rpcbind():
  Logger.info("check if native nfs server is running")
  p, output = shell.call("pgrep nfsd")
  if p == 0 :
    Logger.info("native nfs server is running. shutting it down...")
    # shutdown nfs
    shell.call("service nfs stop")
    shell.call("service nfs-kernel-server stop")
    Logger.info("check if the native nfs server is down...")
    p, output = shell.call("pgrep nfsd")
    if p == 0 :
      raise Fail("Failed to shutdown native nfs service")

  Logger.info("check if rpcbind or portmap is running")
  p, output = shell.call("pgrep rpcbind")
  q, output = shell.call("pgrep portmap")

  if p!=0 and q!=0 :
    Logger.info("no portmap or rpcbind running. starting one...")
    p, output = shell.call(("service", "rpcbind", "start"), sudo=True)
    q, output = shell.call(("service", "portmap", "start"), sudo=True)
    if p!=0 and q!=0 :
      raise Fail("Failed to start rpcbind or portmap")

  Logger.info("now we are ready to start nfs gateway")


def nfsgateway(action=None, format=False):
  import params

  if action== "start":
    prepare_rpcbind()

  if action == "configure":
    Directory(params.nfs_file_dump_dir,
              owner = params.hdfs_user,
              group = params.user_group,
    )
  elif action == "start" or action == "stop":
    service(
      action=action,
      name="nfs3",
      user=params.root_user,
      create_pid_dir=True,
      create_log_dir=True
    )
