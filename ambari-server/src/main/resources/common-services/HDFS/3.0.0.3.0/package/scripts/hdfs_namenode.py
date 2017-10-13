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
import time

from ambari_commons import constants

from resource_management.core import shell
from resource_management.core.source import Template
from resource_management.core.resources.system import File, Execute, Directory
from resource_management.core.resources.service import Service
from resource_management.libraries.functions import namenode_ha_utils
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.resources.execute_hadoop import ExecuteHadoop
from resource_management.libraries.functions import Direction
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl
from utils import get_dfsadmin_base_command
from utils import set_up_zkfc_security

if OSCheck.is_windows_family():
  from resource_management.libraries.functions.windows_service_utils import check_windows_service_status

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger

from utils import service, safe_zkfc_op, is_previous_fs_image
from setup_ranger_hdfs import setup_ranger_hdfs, create_ranger_audit_hdfs_directories

import namenode_upgrade

def wait_for_safemode_off(hdfs_binary, afterwait_sleep=0, execute_kinit=False, retries=115, sleep_seconds=10):
  """
  During NonRolling (aka Express Upgrade), after starting NameNode, which is still in safemode, and then starting
  all of the DataNodes, we need for NameNode to receive all of the block reports and leave safemode.
  If HA is present, then this command will run individually on each NameNode, which checks for its own address.
  """
  import params

  sleep_minutes = int(sleep_seconds * retries / 60)

  Logger.info("Waiting up to {0} minutes for the NameNode to leave Safemode...".format(sleep_minutes))

  if params.security_enabled and execute_kinit:
    kinit_command = format("{params.kinit_path_local} -kt {params.hdfs_user_keytab} {params.hdfs_principal_name}")
    Execute(kinit_command, user=params.hdfs_user, logoutput=True)

  try:
    # Note, this fails if namenode_address isn't prefixed with "params."

    dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary, use_specific_namenode=True)
    is_namenode_safe_mode_off = dfsadmin_base_command + " -safemode get | grep 'Safe mode is OFF'"

    # Wait up to 30 mins
    Execute(is_namenode_safe_mode_off, tries=retries, try_sleep=sleep_seconds,
      user=params.hdfs_user, logoutput=True)

    # Wait a bit more since YARN still depends on block reports coming in.
    # Also saw intermittent errors with HBASE service check if it was done too soon.
    time.sleep(afterwait_sleep)
  except Fail:
    Logger.error("The NameNode is still in Safemode. Please be careful with commands that need Safemode OFF.")

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def namenode(action=None, hdfs_binary=None, do_format=True, upgrade_type=None,
    upgrade_suspended=False, env=None):

  if action is None:
    raise Fail('"action" parameter is required for function namenode().')

  if action in ["start", "stop"] and hdfs_binary is None:
    raise Fail('"hdfs_binary" parameter is required for function namenode().')

  if action == "configure":
    import params
    #we need this directory to be present before any action(HA manual steps for
    #additional namenode)
    create_name_dirs(params.dfs_name_dir)

    # set up failover /  secure zookeper ACLs, this feature is supported from HDP 2.6 ownwards
    set_up_zkfc_security(params)
  elif action == "start":
    Logger.info("Called service {0} with upgrade_type: {1}".format(action, str(upgrade_type)))
    setup_ranger_hdfs(upgrade_type=upgrade_type)
    import params

    File(params.exclude_file_path,
         content=Template("exclude_hosts_list.j2"),
         owner=params.hdfs_user,
         group=params.user_group
    )

    if do_format and not params.hdfs_namenode_format_disabled:
      format_namenode()
      pass

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

    if upgrade_type == constants.UPGRADE_TYPE_ROLLING and params.dfs_ha_enabled:
      # Most likely, ZKFC is up since RU will initiate the failover command. However, if that failed, it would have tried
      # to kill ZKFC manually, so we need to start it if not already running.
      safe_zkfc_op(action, env)

    options = ""
    if upgrade_type == constants.UPGRADE_TYPE_ROLLING:
      if params.upgrade_direction == Direction.UPGRADE:
        options = "-rollingUpgrade started"
      elif params.upgrade_direction == Direction.DOWNGRADE:
        options = "-rollingUpgrade downgrade"
    elif upgrade_type == constants.UPGRADE_TYPE_NON_ROLLING:
      is_previous_image_dir = is_previous_fs_image()
      Logger.info("Previous file system image dir present is {0}".format(str(is_previous_image_dir)))

      if params.upgrade_direction == Direction.UPGRADE:
        options = "-rollingUpgrade started"
      elif params.upgrade_direction == Direction.DOWNGRADE:
        options = "-rollingUpgrade downgrade"
    elif upgrade_type == constants.UPGRADE_TYPE_HOST_ORDERED:
      # nothing special to do for HOU - should be very close to a normal restart
      pass
    elif upgrade_type is None and upgrade_suspended is True:
      # the rollingUpgrade flag must be passed in during a suspended upgrade when starting NN
      if os.path.exists(namenode_upgrade.get_upgrade_in_progress_marker()):
        options = "-rollingUpgrade started"
      else:
        Logger.info("The NameNode upgrade marker file {0} does not exist, yet an upgrade is currently suspended. "
                    "Assuming that the upgrade of NameNode has not occurred yet.".format(namenode_upgrade.get_upgrade_in_progress_marker()))

    Logger.info("Options for start command are: {0}".format(options))

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

    # ___Scenario___________|_Expected safemode state__|_Wait for safemode OFF____|
    # no-HA                 | ON -> OFF                | Yes                      |
    # HA and active         | ON -> OFF                | Yes                      |
    # HA and standby        | no change                | No                       |
    # RU with HA on active  | ON -> OFF                | Yes                      |
    # RU with HA on standby | ON -> OFF                | Yes                      |
    # EU with HA on active  | ON -> OFF                | No                       |
    # EU with HA on standby | ON -> OFF                | No                       |
    # EU non-HA             | ON -> OFF                | No                       |

    # because we do things like create directories after starting NN,
    # the vast majority of the time this should be True - it should only
    # be False if this is HA and we are the Standby NN
    ensure_safemode_off = True

    # True if this is the only NameNode (non-HA) or if its the Active one in HA
    is_active_namenode = True

    if params.dfs_ha_enabled:
      Logger.info("Waiting for the NameNode to broadcast whether it is Active or Standby...")

      if is_this_namenode_active() is False:
        # we are the STANDBY NN
        is_active_namenode = False

        # we are the STANDBY NN and this restart is not part of an upgrade
        if upgrade_type is None:
          ensure_safemode_off = False


    # During an Express Upgrade, NameNode will not leave SafeMode until the DataNodes are started,
    # so always disable the Safemode check
    if upgrade_type == constants.UPGRADE_TYPE_NON_ROLLING:
      ensure_safemode_off = False

    # some informative logging separate from the above logic to keep things a little cleaner
    if ensure_safemode_off:
      Logger.info("Waiting for this NameNode to leave Safemode due to the following conditions: HA: {0}, isActive: {1}, upgradeType: {2}".format(
        params.dfs_ha_enabled, is_active_namenode, upgrade_type))
    else:
      Logger.info("Skipping Safemode check due to the following conditions: HA: {0}, isActive: {1}, upgradeType: {2}".format(
        params.dfs_ha_enabled, is_active_namenode, upgrade_type))


    # wait for Safemode to end
    if ensure_safemode_off:
      if params.rolling_restart and params.rolling_restart_safemode_exit_timeout:
        calculated_retries = int(params.rolling_restart_safemode_exit_timeout) / 30
        wait_for_safemode_off(hdfs_binary, afterwait_sleep=30, retries=calculated_retries, sleep_seconds=30)
      else:
        wait_for_safemode_off(hdfs_binary)

    # Always run this on the "Active" NN unless Safemode has been ignored
    # in the case where safemode was ignored (like during an express upgrade), then
    # NN will be in SafeMode and cannot have directories created
    if is_active_namenode and ensure_safemode_off:
      create_hdfs_directories()
      create_ranger_audit_hdfs_directories()
    else:
      Logger.info("Skipping creation of HDFS directories since this is either not the Active NameNode or we did not wait for Safemode to finish.")

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
def namenode(action=None, hdfs_binary=None, do_format=True, upgrade_type=None,
    upgrade_suspended=False, env=None):

  if action is None:
    raise Fail('"action" parameter is required for function namenode().')

  if action in ["start", "stop"] and hdfs_binary is None:
    raise Fail('"hdfs_binary" parameter is required for function namenode().')

  if action == "configure":
    pass
  elif action == "start":
    import params
    #TODO: Replace with format_namenode()
    namenode_format_marker = os.path.join(params.hadoop_conf_dir,"NN_FORMATTED")
    if not os.path.exists(namenode_format_marker):
      hadoop_cmd = "cmd /C %s" % (os.path.join(params.hadoop_home, "bin", "hadoop.cmd"))
      Execute("%s namenode -format" % (hadoop_cmd), logoutput=True)
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
            create_parents = True,
            cd_access="a",
  )


def create_hdfs_directories():
  import params

  params.HdfsResource(params.hdfs_tmp_dir,
                       type="directory",
                       action="create_on_execute",
                       owner=params.hdfs_user,
                       mode=0777,
  )
  params.HdfsResource(params.smoke_hdfs_user_dir,
                       type="directory",
                       action="create_on_execute",
                       owner=params.smoke_user,
                       mode=params.smoke_hdfs_user_mode,
  )
  params.HdfsResource(None,
                      action="execute",
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
                    bin_dir=params.hadoop_bin_dir,
                    conf_dir=hadoop_conf_dir,
                    logoutput=True)
    else:
      if not is_namenode_formatted(params):
        Execute(format("hdfs --config {hadoop_conf_dir} namenode -format -nonInteractive"),
                user = params.hdfs_user,
                path = [params.hadoop_bin_dir],
                logoutput=True
        )
        for m_dir in mark_dir:
          Directory(m_dir,
            create_parents = True
          )
  else:
    if params.dfs_ha_namenode_active is not None and \
       params.hostname == params.dfs_ha_namenode_active:
      # check and run the format command in the HA deployment scenario
      # only format the "active" namenode in an HA deployment
      if force:
        ExecuteHadoop('namenode -format',
                      bin_dir=params.hadoop_bin_dir,
                      conf_dir=hadoop_conf_dir,
                      logoutput=True)
      else:
        nn_name_dirs = params.dfs_name_dir.split(',')
        if not is_namenode_formatted(params):
          try:
            Execute(format("hdfs --config {hadoop_conf_dir} namenode -format -nonInteractive"),
                    user = params.hdfs_user,
                    path = [params.hadoop_bin_dir],
                    logoutput=True
            )
          except Fail:
            # We need to clean-up mark directories, so we can re-run format next time.
            for nn_name_dir in nn_name_dirs:
              Execute(format("rm -rf {nn_name_dir}/*"),
                      user = params.hdfs_user,
              )
            raise
          for m_dir in mark_dir:
            Directory(m_dir,
              create_parents = True
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
      Logger.info(format("{mark_dir} exists. Namenode DFS already formatted"))

  # Ensure that all mark dirs created for all name directories
  if marked:
    for mark_dir in mark_dirs:
      Directory(mark_dir,
        create_parents = True
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
                  create_parents = True,
        )
      Directory(old_mark_dir,
        action = "delete"
      )
      marked = True

  if marked:
    return True

  # Check if name dirs are not empty
  for name_dir in nn_name_dirs:
    code, out = shell.call(("ls", name_dir))
    dir_exists_and_valid = bool(not code)

    if not dir_exists_and_valid: # situations if disk exists but is crashed at the moment (ls: reading directory ...: Input/output error)
      Logger.info(format("NameNode will not be formatted because the directory {name_dir} is missing or cannot be checked for content. {out}"))
      return True

    try:
      Execute(format("ls {name_dir} | wc -l  | grep -q ^0$"),
      )
    except Fail:
      Logger.info(format("NameNode will not be formatted since {name_dir} exists and contains content"))
      return True

  return False

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
      nn_refresh_cmd = format('dfsadmin -fs {namenode_address} -refreshNodes')
    ExecuteHadoop(nn_refresh_cmd,
                  user=hdfs_user,
                  conf_dir=conf_dir,
                  bin_dir=params.hadoop_bin_dir)

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def refreshProxyUsers():
  import params

  if params.security_enabled:
    Execute(params.nn_kinit_cmd,
            user=params.hdfs_user
            )

  if params.dfs_ha_enabled:
    # due to a bug in hdfs, refreshNodes will not run on both namenodes so we
    # need to execute each command scoped to a particular namenode
    nn_refresh_cmd = format('dfsadmin -fs hdfs://{namenode_rpc} -refreshSuperUserGroupsConfiguration')
  else:
    nn_refresh_cmd = format('dfsadmin -fs {namenode_address} -refreshSuperUserGroupsConfiguration')
  ExecuteHadoop(nn_refresh_cmd,
                user=params.hdfs_user,
                conf_dir=params.hadoop_conf_dir,
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

  if params.hdfs_include_file:
    File(params.include_file_path,
         content=Template("include_hosts_list.j2"),
         owner=params.hdfs_user
         )
    pass

  if params.dfs_ha_enabled:
    # due to a bug in hdfs, refreshNodes will not run on both namenodes so we
    # need to execute each command scoped to a particular namenode
    nn_refresh_cmd = format('cmd /c hadoop dfsadmin -fs hdfs://{namenode_rpc} -refreshNodes')
  else:
    nn_refresh_cmd = format('cmd /c hadoop dfsadmin -fs {namenode_address} -refreshNodes')
  Execute(nn_refresh_cmd, user=hdfs_user)


def bootstrap_standby_namenode(params, use_path=False):
  mark_dirs = params.namenode_bootstrapped_mark_dirs
  bin_path = os.path.join(params.hadoop_bin_dir, '') if use_path else ""
  try:
    iterations = 50
    bootstrapped = False
    bootstrap_cmd = format("{bin_path}hdfs namenode -bootstrapStandby -nonInteractive")
    # Blue print based deployments start both NN in parallel and occasionally
    # the first attempt to bootstrap may fail. Depending on how it fails the
    # second attempt may not succeed (e.g. it may find the folder and decide that
    # bootstrap succeeded). The solution is to call with -force option but only
    # during initial start
    if params.command_phase == "INITIAL_START":
      # force bootstrap in INITIAL_START phase
      bootstrap_cmd = format("{bin_path}hdfs namenode -bootstrapStandby -nonInteractive -force")
    elif is_namenode_bootstrapped(params):
      # Once out of INITIAL_START phase bootstrap only if we couldnt bootstrap during cluster deployment
      return True
    Logger.info("Boostrapping standby namenode: %s" % (bootstrap_cmd))
    for i in range(iterations):
      Logger.info('Try %d out of %d' % (i+1, iterations))
      code, out = shell.call(bootstrap_cmd, logoutput=False, user=params.hdfs_user)
      if code == 0:
        Logger.info("Standby namenode bootstrapped successfully")
        bootstrapped = True
        break
      elif code == 5:
        Logger.info("Standby namenode already bootstrapped")
        bootstrapped = True
        break
      else:
        Logger.warning('Bootstrap standby namenode failed with %d error code. Will retry' % (code))
  except Exception as ex:
    Logger.error('Bootstrap standby namenode threw an exception. Reason %s' %(str(ex)))
  if bootstrapped:
    for mark_dir in mark_dirs:
      Directory(mark_dir,
                create_parents = True
                )
  return bootstrapped

def is_namenode_bootstrapped(params):
  mark_dirs = params.namenode_bootstrapped_mark_dirs
  nn_name_dirs = params.dfs_name_dir.split(',')
  marked = False
  # Check if name directories have been marked as formatted
  for mark_dir in mark_dirs:
    if os.path.isdir(mark_dir):
      marked = True
      Logger.info(format("{mark_dir} exists. Standby Namenode already bootstrapped"))
      break

  # Ensure that all mark dirs created for all name directories
  if marked:
    for mark_dir in mark_dirs:
      Directory(mark_dir,
                create_parents = True
                )

  return marked


@retry(times=125, sleep_time=5, backoff_factor=2, err_class=Fail)
def is_this_namenode_active():
  """
  Gets whether the current NameNode is Active. This function will wait until the NameNode is
  listed as being either Active or Standby before returning a value. This is to ensure that
  that if the other NameNode is Active, we ensure that this NameNode has fully loaded and
  registered in the event that the other NameNode is going to be restarted. This prevents
  a situation where we detect the other NameNode as Active before this NameNode has fully booted.
  If the other Active NameNode is then restarted, there can be a loss of service if this
  NameNode has not entered Standby.
  """
  import params

  # returns ([('nn1', 'c6401.ambari.apache.org:50070')], [('nn2', 'c6402.ambari.apache.org:50070')], [])
  #                  0                                           1                                   2
  # or
  # returns ([], [('nn1', 'c6401.ambari.apache.org:50070')], [('nn2', 'c6402.ambari.apache.org:50070')], [])
  #          0                                              1                                             2
  #
  namenode_states = namenode_ha_utils.get_namenode_states(params.hdfs_site, params.security_enabled,
    params.hdfs_user, times=5, sleep_time=5, backoff_factor=2)

  # unwraps [('nn1', 'c6401.ambari.apache.org:50070')]
  active_namenodes = [] if len(namenode_states[0]) < 1 else namenode_states[0]

  # unwraps [('nn2', 'c6402.ambari.apache.org:50070')]
  standby_namenodes = [] if len(namenode_states[1]) < 1 else namenode_states[1]

  # check to see if this is the active NameNode
  for entry in active_namenodes:
    if params.namenode_id in entry:
      return True

  # if this is not the active NameNode, then we must wait for it to register as standby
  for entry in standby_namenodes:
    if params.namenode_id in entry:
      return False

  # this this point, this NameNode is neither active nor standby - we must wait to ensure it
  # enters at least one of these roles before returning a verdict - the annotation will catch
  # this failure and retry the fuction automatically
  raise Fail(format("The NameNode {namenode_id} is not listed as Active or Standby, waiting..."))
