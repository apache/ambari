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


from resource_management.core import shell
from resource_management.core.source import Template
from resource_management.core.resources.system import File, Execute, Directory
from resource_management.core.resources.service import Service
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl

if OSCheck.is_windows_family():
  from resource_management.libraries.functions.windows_service_utils import check_windows_service_status

from resource_management.core.shell import as_user
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger

from utils import service, safe_zkfc_op
from setup_ranger_hdfs import setup_ranger_hdfs

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def namenode(action=None, do_format=True, rolling_restart=False, env=None):
  if action == "configure":
    import params
    #we need this directory to be present before any action(HA manual steps for
    #additional namenode)
    create_name_dirs(params.dfs_name_dir)
  elif action == "start":
    setup_ranger_hdfs(rolling_upgrade = rolling_restart)
    import params
    if do_format:
      format_namenode()
      pass

    File(params.exclude_file_path,
         content=Template("exclude_hosts_list.j2"),
         owner=params.hdfs_user,
         group=params.user_group
    )

    if params.dfs_ha_enabled and \
      params.dfs_ha_namenode_standby is not None and \
      params.hostname == params.dfs_ha_namenode_standby:
        # if the current host is the standby NameNode in an HA deployment
        # run the bootstrap command, to start the NameNode in standby mode
        # this requires that the active NameNode is already up and running,
        # so this execute should be re-tried upon failure, up to a timeout
        success = bootstrap_standby_namenode(params)
        if not success:
          raise Fail("Could not bootstrap standby namenode")

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

    is_namenode_safe_mode_off = format("hdfs dfsadmin -fs {namenode_address} -safemode get | grep 'Safe mode is OFF'")
    if params.dfs_ha_enabled:
      is_active_namenode_cmd = as_user(format("hdfs --config {hadoop_conf_dir} haadmin -getServiceState {namenode_id} | grep active"), params.hdfs_user, env={'PATH':params.hadoop_bin_dir})
    else:
      is_active_namenode_cmd = None

    # During normal operations, if HA is enabled and it is in standby, then no need to check safemode staus.
    # During Rolling Upgrade, both namenodes must eventually leave safemode, and Ambari can wait for this.

    # ___Scenario_________|_Expected safemode state___|_Wait for safemode OFF____|
    # 1 (HA and active)   | ON -> OFF                 | Yes                      |
    # 2 (HA and standby)  | no change (yes during RU) | no check (yes during RU) |
    # 3 (no-HA)           | ON -> OFF                 | Yes                      |
    check_for_safemode_off = False
    msg = ""
    if params.dfs_ha_enabled:
      code, out = shell.call(is_active_namenode_cmd, logoutput=True) # If active NN, code will be 0
      if code == 0: # active
        check_for_safemode_off = True
        msg = "Must wait to leave safemode since High Availability is enabled and this is the Active NameNode."
      elif rolling_restart:
        check_for_safemode_off = True
        msg = "Must wait to leave safemode since High Availability is enabled during a Rolling Upgrade"
    else:
      msg = "Must wait to leave safemode since High Availability is not enabled."
      check_for_safemode_off = True

    if not msg:
      msg = "Will remain in the current safemode state."
    Logger.info(msg)

    if check_for_safemode_off:
      # First check if Namenode is not in 'safemode OFF' (equivalent to safemode ON). If safemode is OFF, no change.
      # If safemode is ON, first wait for NameNode to leave safemode on its own (if that doesn't happen within 30 seconds, then
      # force NameNode to leave safemode).
      Logger.info("Checking the NameNode safemode status since may need to transition from ON to OFF.")

      try:
        # Wait up to 30 mins
        Execute(is_namenode_safe_mode_off,
                tries=180,
                try_sleep=10,
                user=params.hdfs_user,
                logoutput=True
        )
      except Fail:
        Logger.error("NameNode is still in safemode, please be careful with commands that need safemode OFF.")

    # Always run this on non-HA, or active NameNode during HA.
    create_hdfs_directories(is_active_namenode_cmd)

  elif action == "stop":
    import params
    service(
      action="stop", name="namenode", 
      user=params.hdfs_user
    )
  elif action == "status":
    import status_params
    check_process_status(status_params.namenode_pid_file)
  elif action == "decommission":
    decommission()

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def namenode(action=None, do_format=True, rolling_restart=False, env=None):
  if action == "configure":
    pass
  elif action == "start":
    import params
    #TODO: Replace with format_namenode()
    namenode_format_marker = os.path.join(params.hadoop_conf_dir,"NN_FORMATTED")
    if not os.path.exists(namenode_format_marker):
      hadoop_cmd = "cmd /C %s" % (os.path.join(params.hadoop_home, "bin", "hadoop.cmd"))
      Execute("%s namenode -format" % (hadoop_cmd))
      open(namenode_format_marker, 'a').close()
    Service(params.namenode_win_service_name, action=action)
  elif action == "stop":
    import params
    Service(params.namenode_win_service_name, action=action)
  elif action == "status":
    import status_params
    check_windows_service_status(status_params.namenode_win_service_name)
  elif action == "decommission":
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

  params.HdfsResource("/tmp",
                       type="directory",
                       action="create_on_execute",
                       owner=params.hdfs_user,
                       mode=0777,
                       only_if=check
  )
  params.HdfsResource(params.smoke_hdfs_user_dir,
                       type="directory",
                       action="create_on_execute",
                       owner=params.smoke_user,
                       mode=params.smoke_hdfs_user_mode,
                       only_if=check
  )
  params.HdfsResource(None, 
                      action="execute",
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
    if params.dfs_ha_namenode_active is not None and \
       params.hostname == params.dfs_ha_namenode_active:
      # check and run the format command in the HA deployment scenario
      # only format the "active" namenode in an HA deployment
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

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
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
      nn_refresh_cmd = format('dfsadmin -fs {namenode_address} -refreshNodes')
    ExecuteHadoop(nn_refresh_cmd,
                  user=hdfs_user,
                  conf_dir=conf_dir,
                  kinit_override=True,
                  bin_dir=params.hadoop_bin_dir)

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def decommission():
  import params
  hdfs_user = params.hdfs_user
  conf_dir = params.hadoop_conf_dir

  File(params.exclude_file_path,
       content=Template("exclude_hosts_list.j2"),
       owner=hdfs_user
  )

  if params.dfs_ha_enabled:
    # due to a bug in hdfs, refreshNodes will not run on both namenodes so we
    # need to execute each command scoped to a particular namenode
    nn_refresh_cmd = format('cmd /c hadoop dfsadmin -fs hdfs://{namenode_rpc} -refreshNodes')
  else:
    nn_refresh_cmd = format('cmd /c hadoop dfsadmin -refreshNodes')
  Execute(nn_refresh_cmd, user=hdfs_user)


def bootstrap_standby_namenode(params):
  try:
    iterations = 50
    bootstrap_cmd = "hdfs namenode -bootstrapStandby -nonInteractive"
    Logger.info("Boostrapping standby namenode: %s" % (bootstrap_cmd))
    for i in range(iterations):
      Logger.info('Try %d out of %d' % (i+1, iterations))
      code, out = shell.call(bootstrap_cmd, logoutput=False, user=params.hdfs_user)
      if code == 0:
        Logger.info("Standby namenode bootstrapped successfully")
        return True
      elif code == 5:
        Logger.info("Standby namenode already bootstrapped")
        return True
      else:
        Logger.warning('Bootstrap standby namenode failed with %d error code. Will retry' % (code))
  except Exception as ex:
    Logger.error('Bootstrap standby namenode threw an exception. Reason %s' %(str(ex)))
  return False
