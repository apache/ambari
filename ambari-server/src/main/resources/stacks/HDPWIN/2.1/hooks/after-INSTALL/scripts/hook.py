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
from resource_management.libraries import Hook


#Hook for hosts with only client without other components
class AfterInstallHook(Hook):
  def hook(self, env):
    import params
    env.set_params(params)

    #The SQL Server JDBC driver needs to end up in HADOOP_COMMOON_DIR\share\hadoop\common\lib
    try:
      ensure_jdbc_driver_is_in_classpath(params.hadoop_common_dir,
                                         params.config["hostLevelParams"]["agentCacheDir"],
                                         params.config['ambariLevelParams']['jdk_location'],
                                         ["sqljdbc4.jar", "sqljdbc_auth.dll"])
    except Exception as e:
      raise Fail("Unable to deploy the required JDBC driver in the class path. Error info: {0}".format(e.message))

    XmlConfig("core-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['core-site'],
              owner=params.hdfs_user,
              mode="f",
              configuration_attributes=params.config['configurationAttributes']['core-site']
    )

    File(format("{params.hadoop_install_root}/cluster.properties"),
           content=Template("cluster.properties.j2"),
           owner=params.hdfs_user,
           mode="f"
    )

    File(format("{params.hadoop_install_root}/Run-SmokeTests.cmd"),
         content=Template("Run-SmokeTests.cmd"),
         owner=params.hdfs_user,
         mode="f"
    )

    File(format("{params.hadoop_install_root}/Run-SmokeTests.ps1"),
         content=Template("Run-SmokeTests.ps1"),
         owner=params.hdfs_user,
         mode="f"
    )

if __name__ == "__main__":
  AfterInstallHook().execute()
