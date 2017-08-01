#!/usr/bin/python
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
import os
from resource_management import *
from resource_management.libraries.functions import stack_select
#from resource_management.libraries.functions.version import compare_versions, format_stack_version
from resource_management.core.exceptions import ComponentIsNotRunning
from resource_management.core.logger import Logger
from resource_management.core import shell
from spark import *


class JobHistoryServer(Script):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    if params.version and compare_versions(format_stack_version(params.version), '4.0.0.0') >= 0:
      stack_select.select_packages(params.version)
      #Execute(format("stack-select set spark-historyserver {version}"))

  def install(self, env):
    self.install_packages(env)
    import params

    env.set_params(params)
    self.configure(env)

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    daemon_cmd = format('{spark_history_server_stop}')
    Execute(daemon_cmd,
            user=params.spark_user,
            environment={'JAVA_HOME': params.java_home}
    )
    if os.path.isfile(params.spark_history_server_pid_file):
      os.remove(params.spark_history_server_pid_file)


  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    self.create_historyServer_directory()
    self.copy_spark_yarn_jar()

    if params.security_enabled:
      spark_kinit_cmd = format("{kinit_path_local} -kt {spark_kerberos_keytab} {spark_principal}; ")
      Execute(spark_kinit_cmd, user=params.spark_user)

    # FIXME! TODO! remove this after soft link bug is fixed:
    #if not os.path.islink('/usr/iop/current/spark'):
    #  iop_version = get_iop_version()
    #  cmd = 'ln -s /usr/iop/' + iop_version + '/spark /usr/iop/current/spark'
    #  Execute(cmd)

    daemon_cmd = format('{spark_history_server_start}')
    no_op_test = format(
      'ls {spark_history_server_pid_file} >/dev/null 2>&1 && ps -p `cat {spark_history_server_pid_file}` >/dev/null 2>&1')
    Execute(daemon_cmd,
            user=params.spark_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=no_op_test
    )

  def status(self, env):
    import status_params

    env.set_params(status_params)
    pid_file = format("{spark_history_server_pid_file}")
    # Recursively check all existing pid files
    check_process_status(pid_file)

  def create_historyServer_directory(self):
    import params

    params.HdfsResource(params.spark_hdfs_user_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.spark_user,
                         group=params.user_group,
                         mode=params.spark_hdfs_user_mode)

    params.HdfsResource(params.spark_eventlog_dir_default,
                         type="directory",
                         action="create_on_execute",
                         owner=params.spark_user,
                         group=params.user_group,
                         mode=params.spark_eventlog_dir_mode)

    params.HdfsResource(None, action="execute")

  def copy_spark_yarn_jar(self):
    import params

    jar_src_file = params.spark_jar_src_dir + "/" + params.spark_jar_src_file
    jar_dst_file = params.spark_jar_hdfs_dir + "/" + params.spark_jar_src_file
    jar_dst_path = params.spark_jar_hdfs_dir

    # Remove to enable refreshing jars during restart
    hdfs_remove_cmd = "dfs -rm -R -skipTrash %s" % jar_dst_path

    try:
      ExecuteHadoop(hdfs_remove_cmd,
                    user=params.hdfs_user,
                    logoutput=True,
                    conf_dir=params.hadoop_conf_dir,
                    bin_dir=params.hadoop_bin_dir)
    except Fail:
      pass

    params.HdfsResource(jar_dst_path,
                        type="directory",
                        action="create_on_execute",
                        owner=params.spark_user,
                        group=params.user_group,
                        mode=params.spark_jar_hdfs_dir_mode)

    params.HdfsResource(None, action="execute")

    params.HdfsResource(InlineTemplate(jar_dst_file).get_content(),
                        type="file",
                        action="create_on_execute",
                        source=jar_src_file,
                        owner=params.spark_user,
                        group=params.user_group,
                        mode=params.spark_jar_file_mode)

    params.HdfsResource(None, action="execute")

  def configure(self, env):
    import params

    env.set_params(params)
    spark(env)

if __name__ == "__main__":
  JobHistoryServer().execute()
