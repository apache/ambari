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

import sys
from resource_management import *

class BeforeStartHook(Hook):

  def hook(self, env):
    import params

    self.run_custom_hook('before-ANY')
    self.run_custom_hook('after-INSTALL')
    env.set_params(params)
    if params.has_metric_collector:
      File(os.path.join(params.hadoop_conf_dir, "hadoop-metrics2.properties"),
           owner=params.hadoop_user,
           content=Template("hadoop-metrics2.properties.j2")
      )
      File(os.path.join(params.hbase_conf_dir, "hadoop-metrics2-hbase.properties"),
           owner=params.hadoop_user,
           content=Template("hadoop-metrics2-hbase.properties.j2")
      )

if __name__ == "__main__":
  BeforeStartHook().execute()
