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

from ambari_commons.inet_utils import download_file
from resource_management import *
from resource_management.libraries import Hook


#Hook for hosts with only client without other components
class AfterInstallHook(Hook):

  def hook(self, env):
    import params
    env.set_params(params)

    XmlConfig("core-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['core-site'],
              owner=params.hdfs_user,
              mode="f",
              configuration_attributes=params.config['configuration_attributes']['core-site']
    )
    download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc4.jar"),
                  os.path.join(params.hadoop_common_dir, "sqljdbc4.jar")
    )
    download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc_auth.dll"),
                  os.path.join(params.hadoop_common_dir, "sqljdbc_auth.dll")
    )
    download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc4.jar"),
                  os.path.join(params.hbase_lib_dir, "sqljdbc4.jar")
    )
    download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc_auth.dll"),
                  os.path.join(params.hadoop_common_bin, "sqljdbc_auth.dll")
    )
    download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "metrics-sink-1.0.0.jar"),
                  os.path.join(params.hadoop_common_dir, "metrics-sink-1.0.0.jar")
    )
    download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "metrics-sink-1.0.0.jar"),
                  os.path.join(params.hbase_lib_dir, "metrics-sink-1.0.0.jar")
    )

    File(format("{params.hadoop_install_root}/cluster.properties"),
           content=Template("cluster.properties.j2"),
           owner=params.hdfs_user,
           mode="f"
    )

if __name__ == "__main__":
  AfterInstallHook().execute()
