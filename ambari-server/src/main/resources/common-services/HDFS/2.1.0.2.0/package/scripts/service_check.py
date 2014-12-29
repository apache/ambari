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


class HdfsServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    unique = functions.get_unique_id_and_date()
    dir = '/tmp'
    tmp_file = format("{dir}/{unique}")

    safemode_command = "dfsadmin -safemode get | grep OFF"

    if params.security_enabled:
      Execute(format("{kinit_path_local} -kt {smoke_user_keytab} {smoke_user}"),
        user=params.smoke_user
      )
    ExecuteHadoop(safemode_command,
                  user=params.smoke_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir,
                  try_sleep=3,
                  tries=20,
                  bin_dir=params.hadoop_bin_dir
    )
    params.HdfsResource(dir,
                        type="directory",
                        action="create_delayed",
                        mode=0777
    )
    #cleanup put below to handle retries; if retrying there wil be a stale file
    #that needs cleanup; exit code is fn of second command
    params.HdfsResource(tmp_file,
                        type="directory",
                        action="delete_delayed",
    )
    params.HdfsResource(tmp_file,
                        type="directory",
                        source="/etc/passwd",
                        action="create_delayed"
    )
    params.HdfsResource(None, action="execute")

    if params.has_journalnode_hosts:
      journalnode_port = params.journalnode_port
      checkWebUIFileName = "checkWebUI.py"
      checkWebUIFilePath = format("{tmp_dir}/{checkWebUIFileName}")
      comma_sep_jn_hosts = ",".join(params.journalnode_hosts)
      checkWebUICmd = format("python {checkWebUIFilePath} -m {comma_sep_jn_hosts} -p {journalnode_port}")
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
        check_zkfc_process_cmd = format(
          "ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1")
        Execute(check_zkfc_process_cmd,
                logoutput=True,
                try_sleep=3,
                tries=5
        )


if __name__ == "__main__":
  HdfsServiceCheck().execute()
