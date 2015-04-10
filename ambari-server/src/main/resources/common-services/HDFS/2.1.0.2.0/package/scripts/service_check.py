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
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst


class HdfsServiceCheck(Script):
  pass

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HdfsServiceCheckDefault(HdfsServiceCheck):
  def service_check(self, env):
    import params

    env.set_params(params)
    unique = functions.get_unique_id_and_date()
    dir = '/tmp'
    tmp_file = format("{dir}/{unique}")

    safemode_command = format("dfsadmin -fs {namenode_address} -safemode get | grep OFF")

    create_dir_cmd = format("fs -mkdir {dir}")
    chmod_command = format("fs -chmod 777 {dir}")
    test_dir_exists = as_user(format("{hadoop_bin_dir}/hadoop --config {hadoop_conf_dir} fs -test -e {dir}"), params.hdfs_user)
    cleanup_cmd = format("fs -rm {tmp_file}")
    #cleanup put below to handle retries; if retrying there wil be a stale file
    #that needs cleanup; exit code is fn of second command
    create_file_cmd = format(
      "{cleanup_cmd}; hadoop --config {hadoop_conf_dir} fs -put /etc/passwd {tmp_file}")
    test_cmd = format("fs -test -e {tmp_file}")
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
    ExecuteHadoop(create_dir_cmd,
                  user=params.hdfs_user,
                  logoutput=True,
                  not_if=test_dir_exists,
                  conf_dir=params.hadoop_conf_dir,
                  try_sleep=3,
                  tries=5,
                  bin_dir=params.hadoop_bin_dir
    )
    ExecuteHadoop(chmod_command,
                  user=params.hdfs_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir,
                  try_sleep=3,
                  tries=5,
                  bin_dir=params.hadoop_bin_dir
    )
    ExecuteHadoop(create_file_cmd,
                  user=params.hdfs_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir,
                  try_sleep=3,
                  tries=5,
                  bin_dir=params.hadoop_bin_dir
    )
    ExecuteHadoop(test_cmd,
                  user=params.hdfs_user,
                  logoutput=True,
                  conf_dir=params.hadoop_conf_dir,
                  try_sleep=3,
                  tries=5,
                  bin_dir=params.hadoop_bin_dir
    )
    if params.has_journalnode_hosts:
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
        check_zkfc_process_cmd = format(
          "ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1")
        Execute(check_zkfc_process_cmd,
                logoutput=True,
                try_sleep=3,
                tries=5
        )

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HdfsServiceCheckWindows(HdfsServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)

    unique = functions.get_unique_id_and_date()

    #Hadoop uses POSIX-style paths, separator is always /
    dir = '/tmp'
    tmp_file = dir + '/' + unique

    #commands for execution
    hadoop_cmd = "cmd /C %s" % (os.path.join(params.hadoop_home, "bin", "hadoop.cmd"))
    create_dir_cmd = "%s fs -mkdir %s" % (hadoop_cmd, dir)
    own_dir = "%s fs -chmod 777 %s" % (hadoop_cmd, dir)
    test_dir_exists = "%s fs -test -e %s" % (hadoop_cmd, dir)
    cleanup_cmd = "%s fs -rm %s" % (hadoop_cmd, tmp_file)
    create_file_cmd = "%s fs -put %s %s" % (hadoop_cmd, os.path.join(params.hadoop_conf_dir, "core-site.xml"), tmp_file)
    test_cmd = "%s fs -test -e %s" % (hadoop_cmd, tmp_file)

    hdfs_cmd = "cmd /C %s" % (os.path.join(params.hadoop_home, "bin", "hdfs.cmd"))
    safemode_command = "%s dfsadmin -safemode get | %s OFF" % (hdfs_cmd, params.grep_exe)

    Execute(safemode_command, logoutput=True, try_sleep=3, tries=20)
    Execute(create_dir_cmd, user=params.hdfs_user,logoutput=True, ignore_failures=True)
    Execute(own_dir, user=params.hdfs_user,logoutput=True)
    Execute(test_dir_exists, user=params.hdfs_user,logoutput=True)
    Execute(create_file_cmd, user=params.hdfs_user,logoutput=True)
    Execute(test_cmd, user=params.hdfs_user,logoutput=True)
    Execute(cleanup_cmd, user=params.hdfs_user,logoutput=True)

if __name__ == "__main__":
  HdfsServiceCheck().execute()
