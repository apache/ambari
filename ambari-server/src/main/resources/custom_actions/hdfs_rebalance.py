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
import os
import json


class HdfsRebalance(Script):
  def actionexecute(self, env):

    config = Script.get_config()

    hdfs_user = config['configurations']['global']['hdfs_user']
    conf_dir = config['configurations']['global']['hadoop_conf_dir']

    security_enabled = config['configurations']['global']['security_enabled']
    kinit_path_local = functions.get_kinit_path(
      [default('kinit_path_local'), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])

    threshold = config['commandParams']['threshold']
    principal = config['commandParams']['principal']
    keytab = config['commandParams']['keytab']

    if security_enabled:
      Execute(format("{kinit_path_local}  -kt {keytab} {principal}"))

    ExecuteHadoop(format('balancer -threshold {threshold}'),
      user=hdfs_user,
      conf_dir=conf_dir
    )

    structured_output_example = {
      'user' : hdfs_user,
      'conf_dir' : conf_dir,
      'principal' : principal,
      'keytab' : keytab
      }

    self.put_structured_out(structured_output_example)

if __name__ == "__main__":

  HdfsRebalance().execute()
