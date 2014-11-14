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

"""

import sys
import service_mapping
from resource_management import *
from oozie import oozie
from ambari_commons.inet_utils import force_download_file

class OozieServer(Script):
  def install(self, env):
    import params
    if not check_windows_service_exists(service_mapping.oozie_server_win_service_name):
      self.install_packages(env)
    force_download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc4.jar"),
      os.path.join(params.oozie_root, "extra_libs", "sqljdbc4.jar")
    )
    webapps_sqljdbc_path = os.path.join(params.oozie_home, "oozie-server", "webapps", "oozie", "WEB-INF", "lib", "sqljdbc4.jar")
    if os.path.isfile(webapps_sqljdbc_path):
      force_download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc4.jar"),
        webapps_sqljdbc_path
      )
    force_download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc4.jar"),
      os.path.join(params.oozie_home, "share", "lib", "oozie", "sqljdbc4.jar")
    )
    force_download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc4.jar"),
      os.path.join(params.oozie_home, "temp", "WEB-INF", "lib", "sqljdbc4.jar")
    )

  def configure(self, env):
    oozie()

  def start(self, env):
    import params
    env.set_params(params)
    self.configure(env)
    Service(service_mapping.oozie_server_win_service_name, action="start")

  def stop(self, env):
    import params
    env.set_params(params)
    Service(service_mapping.oozie_server_win_service_name, action="stop")

  def status(self, env):
    import params
    check_windows_service_status(service_mapping.oozie_server_win_service_name)

if __name__ == "__main__":
  OozieServer().execute()
