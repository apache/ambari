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

"""

import hdfs_process

from resource_management.libraries.script.script import Script
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries import functions
from resource_management.libraries.functions.format import format
from resource_management.core.logger import Logger
from resource_management.core.source import StaticFile
from resource_management.core.resources.system import Execute, File


class HdfsServiceCheck(Script):
  pass


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HdfsServiceCheckDefault(HdfsServiceCheck):
  def service_check(self, env):
    import params

    env.set_params(params)
    unique = functions.get_unique_id_and_date()
    hdfs_dir = params.hdfs_tmp_dir
    tmp_file = format("{hdfs_dir}/{unique}")

    """
    Ignore checking safemode, because this command is unable to get safemode state
    when 1 namenode is down in an HA setup (see more in HDFS-8277). Directly
    test HDFS availability by file system operations is consistent in both HA and
    non-HA environment.
    """
    if params.security_enabled:
      Execute(
        (
          params.kinit_path_local,
          "-kt",
          params.hdfs_user_keytab,
          params.hdfs_principal_name,
        ),
        user=params.hdfs_user,
      )
    params.HdfsResource(
      hdfs_dir, type="directory", action="create_on_execute", mode=0o1777
    )
    params.HdfsResource(
      tmp_file,
      type="file",
      action="delete_on_execute",
    )

    params.HdfsResource(None, action="execute")

    if params.has_journalnode_hosts:
      if params.security_enabled:
        for host in params.journalnode_hosts:
          if params.https_only:
            uri = format("https://{host}:{journalnode_port}")
          else:
            uri = format("http://{host}:{journalnode_port}")
          response, errmsg, time_millis = curl_krb_request(
            params.tmp_dir,
            params.smoke_user_keytab,
            params.smokeuser_principal,
            uri,
            "jn_service_check",
            params.kinit_path_local,
            False,
            None,
            params.smoke_user,
          )
          if not response:
            Logger.error(f"Cannot access WEB UI on: {uri}. Error : {errmsg}")
            return 1
      else:
        journalnode_port = params.journalnode_port
        checkWebUIFileName = "checkWebUI.py"
        checkWebUIFilePath = format("{tmp_dir}/{checkWebUIFileName}")
        comma_sep_jn_hosts = ",".join(params.journalnode_hosts)

        checkWebUICmd = (
          "ambari-python-wrap",
          checkWebUIFilePath,
          "-m",
          comma_sep_jn_hosts,
          "-p",
          str(journalnode_port),
          "-s",
          str(params.https_only),
          "-o",
          str(params.script_https_protocol),
        )
        File(checkWebUIFilePath, content=StaticFile(checkWebUIFileName), mode=0o775)

        Execute(
          checkWebUICmd, logoutput=True, try_sleep=3, tries=5, user=params.smoke_user
        )

    if params.is_namenode_master:
      if params.has_zkfc_hosts:
        pid_dir = format("{hadoop_pid_dir_prefix}/{hdfs_user}")
        pid_file = format("{pid_dir}/hadoop-{hdfs_user}-zkfc.pid")
        hdfs_process.wait_for_component_status(
          pid_file,
          params.hdfs_user,
          "zkfc",
          attempts=5,
          sleep_seconds=3,
        )


if __name__ == "__main__":
  HdfsServiceCheck().execute()
