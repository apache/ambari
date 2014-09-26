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

class CopyFromLocalProvider(Provider):
  def action_run(self):
    path = self.resource.path
    dest_dir = self.resource.dest_dir
    dest_file = self.resource.dest_file
    kinnit_if_needed = self.resource.kinnit_if_needed
    owner = self.resource.owner
    group = self.resource.group
    mode = self.resource.mode
    hdfs_usr=self.resource.hdfs_user
    hadoop_conf_path = self.resource.hadoop_conf_dir
    bin_dir = self.resource.hadoop_bin_dir


    if dest_file:
      copy_cmd = format("fs -copyFromLocal {path} {dest_dir}/{dest_file}")
      dest_path = dest_dir + dest_file if dest_dir.endswith(os.sep) else dest_dir + os.sep + dest_file
    else:
      dest_file_name = os.path.split(path)[1]
      copy_cmd = format("fs -copyFromLocal {path} {dest_dir}")
      dest_path = dest_dir + os.sep + dest_file_name
    # Need to run unless as resource user
    su_cmd = 'su - {0} -c'.format(owner)
    unless_cmd = format("{su_cmd} '{kinnit_if_needed} export PATH=$PATH:{bin_dir} ; hadoop fs -ls {dest_path}' >/dev/null 2>&1")

    ExecuteHadoop(copy_cmd,
                  not_if=unless_cmd,
                  user=owner,
                  bin_dir=bin_dir,
                  conf_dir=hadoop_conf_path
                  )

    if not owner:
      chown = None
    else:
      if not group:
        chown = owner
      else:
        chown = format('{owner}:{group}')

    if chown:
      chown_cmd = format("fs -chown {chown} {dest_path}")

      ExecuteHadoop(chown_cmd,
                    user=hdfs_usr,
                    bin_dir=bin_dir,
                    conf_dir=hadoop_conf_path)
    pass

    if mode:
      dir_mode = oct(mode)[1:]
      chmod_cmd = format('fs -chmod {dir_mode} {dest_path}')

      ExecuteHadoop(chmod_cmd,
                    user=hdfs_usr,
                    bin_dir=bin_dir,
                    conf_dir=hadoop_conf_path)
    pass
