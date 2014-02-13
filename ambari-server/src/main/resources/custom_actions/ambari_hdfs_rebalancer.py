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


class HdfsRebalance(Script):
  def actionexecute(self, env):
    config = Script.get_config()

    hdfs_user = config['configurations']['global']['hdfs_user']
    conf_dir = "/etc/hadoop/conf"

    _authentication = config['configurations']['core-site']['hadoop.security.authentication']
    security_enabled = ( not is_empty(_authentication) and _authentication == 'kerberos')

    threshold = config['commandParams']['threshold']

    if security_enabled:
      kinit_path_local = functions.get_kinit_path(
        [default('kinit_path_local', None), "/usr/bin", "/usr/kerberos/bin", "/usr/sbin"])
      principal = config['commandParams']['principal']
      keytab = config['commandParams']['keytab']
      Execute(format("{kinit_path_local}  -kt {keytab} {principal}"))

    ExecuteHadoop(format('balancer -threshold {threshold}'),
                  user=hdfs_user,
                  conf_dir=conf_dir,
                  logoutput=True
    )

    structured_output_example = {
      'result': 'Rebalancer completed.'
    }

    self.put_structured_out(structured_output_example)


if __name__ == "__main__":
  HdfsRebalance().execute()
