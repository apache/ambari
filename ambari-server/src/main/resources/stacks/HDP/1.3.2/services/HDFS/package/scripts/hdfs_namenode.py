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
from utils import service
from utils import hdfs_directory
import urlparse


def namenode(action=None, format=True):
  import params

  if action == "configure":
    create_name_dirs(params.dfs_name_dir)

  if action == "start":
    if format:
      format_namenode()
      pass
    service(
      action="start", name="namenode", user=params.hdfs_user,
      keytab=params.dfs_namenode_keytab_file,
      create_pid_dir=True,
      create_log_dir=True,
      principal=params.dfs_namenode_kerberos_principal
    )

    # TODO: extract creating of dirs to different services
    create_app_directories()
    create_user_directories()

  if action == "stop":
    service(
      action="stop", name="namenode", user=params.hdfs_user,
      keytab=params.dfs_namenode_keytab_file,
      principal=params.dfs_namenode_kerberos_principal
    )

  if action == "decommission":
    decommission()

def create_name_dirs(directories):
  import params

  dirs = directories.split(",")
  Directory(dirs,
            mode=0755,
            owner=params.hdfs_user,
            group=params.user_group,
            recursive=True
  )


def create_app_directories():
  import params

  hdfs_directory(name="/tmp",
                 owner=params.hdfs_user,
                 mode="777"
  )
  #mapred directories
  if params.has_jobtracker:
    hdfs_directory(name="/mapred",
                   owner=params.mapred_user
    )
    hdfs_directory(name="/mapred/system",
                   owner=params.mapred_user
    )
    #hbase directories
  if len(params.hbase_master_hosts) != 0:
    hdfs_directory(name=params.hbase_hdfs_root_dir,
                   owner=params.hbase_user
    )
    hdfs_directory(name=params.hbase_staging_dir,
                   owner=params.hbase_user,
                   mode="711"
    )
    #hive directories
  if len(params.hive_server_host) != 0:
    hdfs_directory(name=params.hive_apps_whs_dir,
                   owner=params.hive_user,
                   mode="777"
    )
  if len(params.hcat_server_hosts) != 0:
    hdfs_directory(name=params.webhcat_apps_dir,
                   owner=params.webhcat_user,
                   mode="755"
    )
  if len(params.hs_host) != 0:
    hdfs_directory(name=params.mapreduce_jobhistory_intermediate_done_dir,
                   owner=params.mapred_user,
                   group=params.user_group,
                   mode="777"
    )

    hdfs_directory(name=params.mapreduce_jobhistory_done_dir,
                   owner=params.mapred_user,
                   group=params.user_group,
                   mode="777"
    )

  pass


def create_user_directories():
  import params

  hdfs_directory(name=params.smoke_hdfs_user_dir,
                 owner=params.smoke_user,
                 mode=params.smoke_hdfs_user_mode
  )

  if params.has_hive_server_host:
    hdfs_directory(name=params.hive_hdfs_user_dir,
                   owner=params.hive_user,
                   mode=params.hive_hdfs_user_mode
    )

  if params.has_hcat_server_host:
    if params.hcat_hdfs_user_dir != params.webhcat_hdfs_user_dir:
      hdfs_directory(name=params.hcat_hdfs_user_dir,
                     owner=params.hcat_user,
                     mode=params.hcat_hdfs_user_mode
      )
    hdfs_directory(name=params.webhcat_hdfs_user_dir,
                   owner=params.webhcat_user,
                   mode=params.webhcat_hdfs_user_mode
    )

  if params.has_oozie_server:
    hdfs_directory(name=params.oozie_hdfs_user_dir,
                   owner=params.oozie_user,
                   mode=params.oozie_hdfs_user_mode
    )


def format_namenode(force=None):
  import params

  mark_dir = params.namenode_formatted_mark_dir
  dfs_name_dir = params.dfs_name_dir
  hdfs_user = params.hdfs_user
  hadoop_conf_dir = params.hadoop_conf_dir

  if True:
    if force:
      ExecuteHadoop('namenode -format',
                    kinit_override=True)
    else:
      File('/tmp/checkForFormat.sh',
           content=StaticFile("checkForFormat.sh"),
           mode=0755)
      Execute(format(
        "sh /tmp/checkForFormat.sh {hdfs_user} {hadoop_conf_dir} {mark_dir} "
        "{dfs_name_dir}"),
              not_if=format("test -d {mark_dir}"),
              path="/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin")
    Execute(format("mkdir -p {mark_dir}"))


def decommission():
  import params

  hdfs_user = params.hdfs_user
  conf_dir = params.hadoop_conf_dir

  File(params.exclude_file_path,
       content=Template("exclude_hosts_list.j2"),
       owner=hdfs_user,
       group=params.user_group
  )

  if params.update_exclude_file_only == False:
    ExecuteHadoop('dfsadmin -refreshNodes',
                  user=hdfs_user,
                  conf_dir=conf_dir,
                  kinit_override=True)
    pass