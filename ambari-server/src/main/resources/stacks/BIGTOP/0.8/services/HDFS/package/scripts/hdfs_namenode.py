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
  #we need this directory to be present before any action(HA manual steps for
  #additional namenode)
  if action == "configure":
    create_name_dirs(params.dfs_name_dir)

  if action == "start":
    if do_format:
      format_namenode()
      pass

    File(params.exclude_file_path,
         content=Template("exclude_hosts_list.j2"),
         owner=params.hdfs_user,
         group=params.user_group
    )

    service(
      action="start", name="namenode", user=params.hdfs_user,
      create_pid_dir=True,
      create_log_dir=True
    )
    if params.dfs_ha_enabled:
      dfs_check_nn_status_cmd = format("su -s /bin/bash - {hdfs_user} -c 'export PATH=$PATH:{hadoop_bin_dir} ; hdfs --config {hadoop_conf_dir} haadmin -ns {dfs_ha_nameservices} -getServiceState {namenode_id} | grep active > /dev/null'")
    else:
      dfs_check_nn_status_cmd = None

    namenode_safe_mode_off = format("su -s /bin/bash - {hdfs_user} -c 'export PATH=$PATH:{hadoop_bin_dir} ; hadoop --config {hadoop_conf_dir} dfsadmin -safemode get' | grep 'Safe mode is OFF'")

    if params.security_enabled:
      Execute(format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name}"),
              user = params.hdfs_user)
    Execute(namenode_safe_mode_off,
            tries=40,
            try_sleep=10,
            only_if=dfs_check_nn_status_cmd #skip when HA not active
    )
    create_hdfs_directories(dfs_check_nn_status_cmd)
  if action == "stop":
    service(
      action="stop", name="namenode", 
      user=params.hdfs_user
    )

  if action == "decommission":
    decommission()

def create_name_dirs(directories):
  import params

  dirs = directories.split(",")
  Directory(dirs,
            mode=0o755,
            owner=params.hdfs_user,
            group=params.user_group,
            create_parents = True
  )


def create_hdfs_directories(check):
  import params

  params.HdfsDirectory("/tmp",
                       action="create_delayed",
                       owner=params.hdfs_user,
                       mode=0o777
  )
  params.HdfsDirectory(params.smoke_hdfs_user_dir,
                       action="create_delayed",
                       owner=params.smoke_user,
                       mode=params.smoke_hdfs_user_mode
  )
  params.HdfsDirectory(None, action="create",
                       only_if=check #skip creation when HA not active
  )

def format_namenode(force=None):
  import params

  old_mark_dir = params.namenode_formatted_old_mark_dir
  mark_dir = params.namenode_formatted_mark_dir
  dfs_name_dir = params.dfs_name_dir
  hdfs_user = params.hdfs_user
  hadoop_conf_dir = params.hadoop_conf_dir

  if not params.dfs_ha_enabled:
    if force:
      ExecuteHadoop('namenode -format',
                    kinit_override=True,
                    bin_dir=params.hadoop_bin_dir,
                    conf_dir=hadoop_conf_dir,
                    logoutput=True)
    else:
      File(format("{tmp_dir}/checkForFormat.sh"),
           content=StaticFile("checkForFormat.sh"),
           mode=0o755)
      Execute(format(
        "{tmp_dir}/checkForFormat.sh {hdfs_user} {hadoop_conf_dir} "
        "{hadoop_bin_dir} {old_mark_dir} {mark_dir} {dfs_name_dir}"),
              not_if=format("test -d {old_mark_dir} || test -d {mark_dir}"),
              path="/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin"
      )
    
      Directory(mark_dir,
        create_parents = True
      )


def decommission():
  import params

  hdfs_user = params.hdfs_user
  conf_dir = params.hadoop_conf_dir
  user_group = params.user_group
  nn_kinit_cmd = params.nn_kinit_cmd
  
  File(params.exclude_file_path,
       content=Template("exclude_hosts_list.j2"),
       owner=hdfs_user,
       group=user_group
  )

  if params.hdfs_include_file:
    File(params.include_file_path,
         content=Template("include_hosts_list.j2"),
         owner=params.hdfs_user,
         group=params.user_group
         )
    pass

  if not params.update_files_only:
    Execute(nn_kinit_cmd,
            user=hdfs_user
    )

    if params.dfs_ha_enabled:
      # due to a bug in hdfs, refreshNodes will not run on both namenodes so we
      # need to execute each command scoped to a particular namenode
      nn_refresh_cmd = format('dfsadmin -fs hdfs://{namenode_rpc} -refreshNodes')
    else:
      nn_refresh_cmd = format('dfsadmin -refreshNodes')
    ExecuteHDFS(nn_refresh_cmd,
                user=hdfs_user,
                conf_dir=conf_dir,
                kinit_override=True,
                bin_dir=params.hadoop_bin_dir)
