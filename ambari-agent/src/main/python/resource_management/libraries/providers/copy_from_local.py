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

from resource_management import *

class CopyFromLocalProvider(Provider):
  def action_run(self):
    path = self.resource.path
    dest_dir = self.resource.dest_dir
    kinnit_if_needed = self.resource.kinnit_if_needed
    stub_path = self.resource.stub_path
    owner = self.resource.owner
    group = self.resource.group
    mode = self.resource.mode
    hadoop_conf_path = self.resource.hadoop_conf_dir

    copy_cmd = format("fs -copyFromLocal {path} {dest_dir}")
    unless_cmd = format("{kinnit_if_needed} hadoop fs -ls {dest_dir} >/dev/null 2>&1")

    ExecuteHadoop(copy_cmd,
                  not_if=unless_cmd,
                  user=owner,
                  conf_dir=hadoop_conf_path,
                  )

    if not owner:
      chown = None
    else:
      if not group:
        chown = owner
      else:
        chown = format('{owner}:{group}')

    if chown:
      chown_cmd = format("fs -chown {chown} {dest_dir}")

      ExecuteHadoop(chown_cmd,
                    user=owner,
                    conf_dir=hadoop_conf_path)
    pass

    if mode:
      chmod_cmd = format('fs -chmod {mode} {dest_dir}')

      ExecuteHadoop(chmod_cmd,
                    user=owner,
                    conf_dir=hadoop_conf_path)
    pass
