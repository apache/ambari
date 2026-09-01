#!/usr/bin/env python3
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

HBase Thrift Server - Manages the HBase Thrift service.
"""

from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.script.script import Script

from hbase import hbase
from hbase_service import check_hbase_process_status, hbase_service
import upgrade


class HbaseThrift(Script):
  def install(self, env):
    import params

    env.set_params(params)
    self.install_packages(env)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    upgrade.prestart(env)

  def configure(self, env):
    import params

    env.set_params(params)
    thrift_site_config = params.config["configurations"].get(
      "hbase-thrift-site", {}
    )
    hbase_site_config = params.config["configurations"].get("hbase-site", {})
    hbase_env_config = params.config["configurations"].get("hbase-env", {})

    hbase(name="thrift")

    effective_hbase_site_config = dict(hbase_site_config)
    if not effective_hbase_site_config.get("hbase.thrift.kerberos.principal"):
      thrift_principal = (
        effective_hbase_site_config.get("hbase.master.kerberos.principal")
        or effective_hbase_site_config.get("hbase.regionserver.kerberos.principal")
        or hbase_env_config.get("hbase_principal_name")
      )
      if thrift_principal:
        effective_hbase_site_config["hbase.thrift.kerberos.principal"] = (
          thrift_principal
        )

    if not effective_hbase_site_config.get("hbase.thrift.keytab.file"):
      thrift_keytab = (
        effective_hbase_site_config.get("hbase.master.keytab.file")
        or effective_hbase_site_config.get("hbase.regionserver.keytab.file")
        or hbase_env_config.get("hbase_user_keytab")
      )
      if thrift_keytab:
        effective_hbase_site_config["hbase.thrift.keytab.file"] = thrift_keytab

    XmlConfig(
      "hbase-site.xml",
      conf_dir=params.hbase_conf_dir,
      configurations=effective_hbase_site_config,
      configuration_attributes=params.config["configurationAttributes"][
        "hbase-site"
      ],
      owner="root",
      group=params.user_group,
      mode=0o644,
    )

    if thrift_site_config:
      XmlConfig(
        "hbase-thrift-site.xml",
        conf_dir=params.hbase_conf_dir,
        configurations=thrift_site_config,
        configuration_attributes=params.config["configurationAttributes"].get(
          "hbase-thrift-site", {}
        ),
        owner="root",
        group=params.user_group,
        mode=0o644,
      )

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    hbase_service(
      "thrift",
      action="start",
      extra_args=(
        "-p",
        str(params.hbase_thrift_port),
        "--infoport",
        str(params.hbase_thrift_info_port),
      ),
    )

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    hbase_service("thrift", action="stop")

  def status(self, env):
    import status_params

    env.set_params(status_params)
    check_hbase_process_status(
      status_params.thrift_pid_file,
      status_params.hbase_user,
      status_params.user_group,
      "thrift",
    )

  def get_log_folder(self):
    import params

    return params.log_dir

  def get_user(self):
    import params

    return params.hbase_user

  def get_pid_files(self):
    import status_params

    return [status_params.thrift_pid_file]


if __name__ == "__main__":
  HbaseThrift().execute()
