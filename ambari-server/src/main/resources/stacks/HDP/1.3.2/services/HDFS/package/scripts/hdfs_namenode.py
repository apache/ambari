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


def namenode(action=None, do_format=True):
  import params

  if action == "configure":
    create_name_dirs(params.dfs_name_dir)

  if action == "start":
    if do_format:
      format_namenode()
      pass
    service(
      action="start", name="namenode", user=params.hdfs_user,
      create_pid_dir=True,
      create_log_dir=True
    )

    namenode_safe_mode_off = format("su -s /bin/bash - {hdfs_user} -c 'hadoop dfsadmin -safemode get' | grep 'Safe mode is OFF'")
    if params.security_enabled:
      Execute(format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name}"),
              user = params.hdfs_user)
    Execute(namenode_safe_mode_off,
            tries=40,
            try_sleep=10
    )
    create_hdfs_directories()

  if action == "stop":
    service(
      action="stop", name="namenode", 
      user=params.hdfs_user,
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

def create_hdfs_directories():
  import params

  params.HdfsDirectory("/tmp",
                       action="create_delayed",
                       owner=params.hdfs_user,
                       mode=0777
  )
  params.HdfsDirectory(params.smoke_hdfs_user_dir,
                       action="create_delayed",
                       owner=params.smoke_user,
                       mode=params.smoke_hdfs_user_mode
  )
  params.HdfsDirectory(None, action="create")

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
      File(format("{tmp_dir}/checkForFormat.sh"),
           content=StaticFile("checkForFormat.sh"),
           mode=0755)
      Execute(format(
        "sh {tmp_dir}/checkForFormat.sh {hdfs_user} {hadoop_conf_dir} {mark_dir} "
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
