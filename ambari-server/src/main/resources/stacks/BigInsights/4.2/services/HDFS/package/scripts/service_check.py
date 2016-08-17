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

from resource_management import *
from resource_management.core.shell import as_user
from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.core.logger import Logger

class HdfsServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    unique = functions.get_unique_id_and_date()
    dir = '/tmp'
    tmp_file = format("{dir}/{unique}")

    safemode_command = format("dfsadmin -fs {namenode_address} -safemode get | grep OFF")

    if params.security_enabled:
      Execute(format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name}"),
        user=params.hdfs_user
      )
    ExecuteHadoop(safemode_command,
                  user=params.hdfs_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir,
                  try_sleep=3,
                  tries=20,
                  bin_dir=params.hadoop_bin_dir
    )
    params.HdfsResource(dir,
                        type="directory",
                        action="create_on_execute",
                        mode=0777
    )
    params.HdfsResource(tmp_file,
                        type="file",
                        action="delete_on_execute",
    )

    params.HdfsResource(tmp_file,
                        type="file",
                        source="/etc/passwd",
                        action="create_on_execute"
    )
    params.HdfsResource(None, action="execute")

    if params.has_journalnode_hosts:
      if params.security_enabled:
        for host in params.journalnode_hosts:
          if params.https_only:
            uri = format("https://{host}:{journalnode_port}")
          else:
            uri = format("http://{host}:{journalnode_port}")
          response, errmsg, time_millis = curl_krb_request(params.tmp_dir, params.smoke_user_keytab,
                                                           params.smokeuser_principal, uri, "jn_service_check",
                                                           params.kinit_path_local, False, None, params.smoke_user)
          if not response:
            Logger.error("Cannot access WEB UI on: {0}. Error : {1}", uri, errmsg)
            return 1
      else:
        journalnode_port = params.journalnode_port
        checkWebUIFileName = "checkWebUI.py"
        checkWebUIFilePath = format("{tmp_dir}/{checkWebUIFileName}")
        comma_sep_jn_hosts = ",".join(params.journalnode_hosts)
        checkWebUICmd = format("python {checkWebUIFilePath} -m {comma_sep_jn_hosts} -p {journalnode_port} -s {https_only}")
        File(checkWebUIFilePath,
             content=StaticFile(checkWebUIFileName),
             mode=0775)

        Execute(checkWebUICmd,
                logoutput=True,
                try_sleep=3,
                tries=5,
                user=params.smoke_user
        )

    if params.is_namenode_master:
      if params.has_zkfc_hosts:
        pid_dir = format("{hadoop_pid_dir_prefix}/{hdfs_user}")
        pid_file = format("{pid_dir}/hadoop-{hdfs_user}-zkfc.pid")
        check_zkfc_process_cmd = as_user(format(
          "ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1"), user=params.hdfs_user)
        Execute(check_zkfc_process_cmd,
                logoutput=True,
                try_sleep=3,
                tries=5
        )


if __name__ == "__main__":
  HdfsServiceCheck().execute()
