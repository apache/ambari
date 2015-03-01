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
import os.path

from resource_management import *
from resource_management.core.exceptions import ComponentIsNotRunning

from utils import service, safe_zkfc_op


def namenode(action=None, do_format=True, rolling_restart=False, env=None):
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

    Directory(params.hadoop_pid_dir_prefix,
              mode=0755,
              owner=params.hdfs_user,
              group=params.user_group
    )

    if params.dfs_ha_enabled:
      # if the current host is the standby NameNode in an HA deployment
      if params.hostname == params.dfs_ha_namenode_standby:
        # run the bootstrap command, to start the NameNode in standby mode
        # this requires that the active NameNode is already up and running,
        # so this execute should be re-tried upon failure, up to a timeout
        Execute("hdfs namenode -bootstrapStandby",
          user = params.hdfs_user, tries=50)

    options = "-rollingUpgrade started" if rolling_restart else ""

    if rolling_restart:    
      # Must start Zookeeper Failover Controller if it exists on this host because it could have been killed in order to initiate the failover.
      safe_zkfc_op(action, env)

    service(
      action="start",
      name="namenode",
      user=params.hdfs_user,
      options=options,
      create_pid_dir=True,
      create_log_dir=True
    )


    if params.security_enabled:
      Execute(format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name}"),
              user = params.hdfs_user)

    if params.dfs_ha_enabled:
      dfs_check_nn_status_cmd = as_user(format("hdfs --config {hadoop_conf_dir} haadmin -getServiceState {namenode_id} | grep active"), params.hdfs_user, env={'PATH':params.hadoop_bin_dir})
    else:
      dfs_check_nn_status_cmd = None

    namenode_safe_mode_off = format("hadoop dfsadmin -safemode get | grep 'Safe mode is OFF'")

    # If HA is enabled and it is in standby, then stay in safemode, otherwise, leave safemode.
    leave_safe_mode = True
    if dfs_check_nn_status_cmd is not None:
      code, out = shell.call(dfs_check_nn_status_cmd) # If active NN, code will be 0
      if code != 0:
        leave_safe_mode = False

    if leave_safe_mode:
      # First check if Namenode is not in 'safemode OFF' (equivalent to safemode ON), if so, then leave it
      code, out = shell.call(namenode_safe_mode_off)
      if code != 0:
        leave_safe_mode_cmd = format("hdfs --config {hadoop_conf_dir} dfsadmin -safemode leave")
        Execute(leave_safe_mode_cmd,
                user=params.hdfs_user,
                path=[params.hadoop_bin_dir],
        )

    # Verify if Namenode should be in safemode OFF
    Execute(namenode_safe_mode_off,
            tries=40,
            try_sleep=10,
            path=[params.hadoop_bin_dir],
            user=params.hdfs_user,
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
            mode=0755,
            owner=params.hdfs_user,
            group=params.user_group,
            recursive=True,
            cd_access="a",
  )


def create_hdfs_directories(check):
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
  params.HdfsDirectory(None, action="create",
                       only_if=check #skip creation when HA not active
  )

def format_namenode(force=None):
  import params

  old_mark_dir = params.namenode_formatted_old_mark_dirs
  mark_dir = params.namenode_formatted_mark_dirs
  dfs_name_dir = params.dfs_name_dir
  hdfs_user = params.hdfs_user
  hadoop_conf_dir = params.hadoop_conf_dir

  if not params.dfs_ha_enabled:
    if force:
      ExecuteHadoop('namenode -format',
                    kinit_override=True,
                    bin_dir=params.hadoop_bin_dir,
                    conf_dir=hadoop_conf_dir)
    else:
      if not is_namenode_formatted(params):
        Execute(format("yes Y | hdfs --config {hadoop_conf_dir} namenode -format"),
                user = params.hdfs_user,
                path = [params.hadoop_bin_dir]
        )
        for m_dir in mark_dir:
          Directory(m_dir,
            recursive = True
          )
  else:
    if params.dfs_ha_namenode_active is not None:
      if params.hostname == params.dfs_ha_namenode_active:
        # check and run the format command in the HA deployment scenario
        # only format the "active" namenode in an HA deployment
        Execute(format("yes Y | hdfs --config {hadoop_conf_dir} namenode -format"),
                user = params.hdfs_user,
                path = [params.hadoop_bin_dir]
        )
        for m_dir in mark_dir:
          Directory(m_dir,
            recursive = True
        )


def is_namenode_formatted(params):
  old_mark_dirs = params.namenode_formatted_old_mark_dirs
  mark_dirs = params.namenode_formatted_mark_dirs
  nn_name_dirs = params.dfs_name_dir.split(',')
  marked = False
  # Check if name directories have been marked as formatted
  for mark_dir in mark_dirs:
    if os.path.isdir(mark_dir):
      marked = True
      print format("{mark_dir} exists. Namenode DFS already formatted")
    
  # Ensure that all mark dirs created for all name directories
  if marked:
    for mark_dir in mark_dirs:
      Directory(mark_dir,
        recursive = True
      )      
    return marked  
  
  # Move all old format markers to new place
  for old_mark_dir in old_mark_dirs:
    if os.path.isdir(old_mark_dir):
      for mark_dir in mark_dirs:
        Execute(('cp', '-ar', old_mark_dir, mark_dir),
                sudo = True
        )
        marked = True
      Directory(old_mark_dir,
        action = "delete"
      )    
    elif os.path.isfile(old_mark_dir):
      for mark_dir in mark_dirs:
        Directory(mark_dir,
                  recursive = True,
        )
      Directory(old_mark_dir,
        action = "delete"
      )
      marked = True
      
  # Check if name dirs are not empty
  for name_dir in nn_name_dirs:
    try:
      Execute(format("ls {name_dir} | wc -l  | grep -q ^0$"),
      )
      marked = False
    except Exception:
      marked = True
      print format("ERROR: Namenode directory(s) is non empty. Will not format the namenode. List of non-empty namenode dirs {nn_name_dirs}")
      break
       
  return marked    
      
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
  
  if not params.update_exclude_file_only:
    Execute(nn_kinit_cmd,
            user=hdfs_user
    )

    if params.dfs_ha_enabled:
      # due to a bug in hdfs, refreshNodes will not run on both namenodes so we
      # need to execute each command scoped to a particular namenode
      nn_refresh_cmd = format('dfsadmin -fs hdfs://{namenode_rpc} -refreshNodes')
    else:
      nn_refresh_cmd = format('dfsadmin -refreshNodes')
    ExecuteHadoop(nn_refresh_cmd,
                  user=hdfs_user,
                  conf_dir=conf_dir,
                  kinit_override=True,
                  bin_dir=params.hadoop_bin_dir)
