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

from resource_management import *
directories_list = [] #direcotries list for mkdir
chmod_map = {} #(mode,recursive):dir_list map
chown_map = {} #(owner,group,recursive):dir_list map
class HdfsDirectoryProvider(Provider):
  def action_create_delayed(self):
    global delayed_directories
    global chmod_map
    global chown_map

    if not self.resource.dir_name:
      return

    dir_name = self.resource.dir_name
    dir_owner = self.resource.owner
    dir_group = self.resource.group
    dir_mode = oct(self.resource.mode)[1:] if self.resource.mode else None
    directories_list.append(self.resource.dir_name)

    recursive_chown_str = "-R" if self.resource.recursive_chown else ""
    recursive_chmod_str = "-R" if self.resource.recursive_chmod else ""
    # grouping directories by mode/owner/group to modify them in one 'chXXX' call
    if dir_mode:
      chmod_key = (dir_mode,recursive_chmod_str)
      if chmod_map.has_key(chmod_key):
        chmod_map[chmod_key].append(dir_name)
      else:
        chmod_map[chmod_key] = [dir_name]

    if dir_owner:
      owner_key = (dir_owner,dir_group,recursive_chown_str)
      if chown_map.has_key(owner_key):
        chown_map[owner_key].append(dir_name)
      else:
        chown_map[owner_key] = [dir_name]

  def action_create(self):
    global delayed_directories
    global chmod_map
    global chown_map

    self.action_create_delayed()

    hdp_conf_dir = self.resource.conf_dir
    hdp_hdfs_user = self.resource.hdfs_user
    secured = self.resource.security_enabled
    keytab_file = self.resource.keytab
    kinit_path = self.resource.kinit_path_local
    bin_dir = self.resource.bin_dir

    chmod_commands = []
    chown_commands = []

    for chmod_key, chmod_dirs in chmod_map.items():
      mode = chmod_key[0]
      recursive = chmod_key[1]
      chmod_dirs_str = ' '.join(chmod_dirs)
      chmod_commands.append(format("hadoop --config {hdp_conf_dir} fs -chmod {recursive} {mode} {chmod_dirs_str}"))

    for chown_key, chown_dirs in chown_map.items():
      owner = chown_key[0]
      group = chown_key[1]
      recursive = chown_key[2]
      chown_dirs_str = ' '.join(chown_dirs)
      if owner:
        chown = owner
        if group:
          chown = format("{owner}:{group}")
        chown_commands.append(format("hadoop --config {hdp_conf_dir} fs -chown {recursive} {chown} {chown_dirs_str}"))

    if secured:
        Execute(format("{kinit_path} -kt {keytab_file} {hdfs_principal_name}"),
                user=hdp_hdfs_user)
    #create all directories in one 'mkdir' call
    dir_list_str = ' '.join(directories_list)
    #for hadoop 2 we need to specify -p to create directories recursively
    parent_flag = '`rpm -q hadoop | grep -q "hadoop-1" || echo "-p"`'

    Execute(format('hadoop --config {hdp_conf_dir} fs -mkdir {parent_flag} {dir_list_str} && {chmod_cmd} && {chown_cmd}',
                   chmod_cmd=' && '.join(chmod_commands),
                   chown_cmd=' && '.join(chown_commands)),
            user=hdp_hdfs_user,
            path=bin_dir,
            not_if=format("su - {hdp_hdfs_user} -c 'export PATH=$PATH:{bin_dir} ; "
                          "hadoop --config {hdp_conf_dir} fs -ls {dir_list_str}'")
    )

    directories_list[:] = []
    chmod_map.clear()
    chown_map.clear()
