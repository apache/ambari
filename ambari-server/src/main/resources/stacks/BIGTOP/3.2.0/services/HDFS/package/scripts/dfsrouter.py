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

from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.script.script import Script
from resource_management.core.logger import Logger
from hdfs_router import router
from hdfs import hdfs, reconfig


class Router(Script):
  def install(self, env):
    import params

    env.set_params(params)
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params

    env.set_params(params)
    hdfs("router")
    router(action="configure", env=env)
    XmlConfig(
      "hdfs-site.xml",
      conf_dir=params.hadoop_conf_dir,
      configurations=params.router_hdfs_site,
      configuration_attributes=params.config["configurationAttributes"]["hdfs-site"],
      mode=0o644,
      owner=params.hdfs_user,
      group=params.user_group,
    )
    XmlConfig(
      "core-site.xml",
      conf_dir=params.hadoop_conf_dir,
      configurations=params.router_core_site,
      configuration_attributes=params.config["configurationAttributes"]["core-site"],
      mode=0o644,
      owner=params.hdfs_user,
      group=params.user_group,
    )

  def save_configs(self, env):
    import params

    env.set_params(params)
    hdfs()

  def reload_configs(self, env):
    import params

    env.set_params(params)
    Logger.info("RELOAD CONFIGS")
    reconfig("router", params.router_address)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    router(action="start", env=env)

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    router(action="stop", env=env)

  def status(self, env):
    import status_params

    env.set_params(status_params)
    router(action="status", env=env)

  def get_log_folder(self):
    import params

    return params.hdfs_log_dir

  def get_user(self):
    import params

    return params.hdfs_user

  def get_pid_files(self):
    import status_params

    return [status_params.router_pid_file]


if __name__ == "__main__":
  Router().execute()
