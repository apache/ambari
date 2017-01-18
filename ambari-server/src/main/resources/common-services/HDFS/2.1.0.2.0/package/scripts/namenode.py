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
import json
import tempfile
from datetime import datetime
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

from ambari_commons import constants

from resource_management.libraries.script.script import Script
from resource_management.core.resources.system import Execute, File
from resource_management.core import shell
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML

from resource_management.core.exceptions import Fail
from resource_management.core.shell import as_user
from resource_management.core.logger import Logger


from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst


import namenode_upgrade
from hdfs_namenode import namenode, wait_for_safemode_off
from hdfs import hdfs
import hdfs_rebalance
from utils import initiate_safe_zkfc_failover, get_hdfs_binary, get_dfsadmin_base_command



# hashlib is supplied as of Python 2.5 as the replacement interface for md5
# and other secure hashes.  In 2.6, md5 is deprecated.  Import hashlib if
# available, avoiding a deprecation warning under 2.6.  Import md5 otherwise,
# preserving 2.4 compatibility.
try:
  import hashlib
  _md5 = hashlib.md5
except ImportError:
  import md5
  _md5 = md5.new

class NameNode(Script):

  def get_component_name(self):
    return "hadoop-hdfs-namenode"

  def get_hdfs_binary(self):
    """
    Get the name or path to the hdfs binary depending on the component name.
    """
    component_name = self.get_component_name()
    return get_hdfs_binary(component_name)

  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)
    #TODO we need this for HA because of manual steps
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hdfs("namenode")
    hdfs_binary = self.get_hdfs_binary()
    namenode(action="configure", hdfs_binary=hdfs_binary, env=env)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    hdfs_binary = self.get_hdfs_binary()
    namenode(action="start", hdfs_binary=hdfs_binary, upgrade_type=upgrade_type,
      upgrade_suspended=params.upgrade_suspended, env=env)

    # after starting NN in an upgrade, touch the marker file - but only do this for certain
    # upgrade types - not all upgrades actually tell NN about the upgrade (like HOU)
    if upgrade_type in (constants.UPGRADE_TYPE_ROLLING, constants.UPGRADE_TYPE_NON_ROLLING):
      # place a file on the system indicating that we've submitting the command that
      # instructs NN that it is now part of an upgrade
      namenode_upgrade.create_upgrade_marker()

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    hdfs_binary = self.get_hdfs_binary()
    if upgrade_type == constants.UPGRADE_TYPE_ROLLING and params.dfs_ha_enabled:
      if params.dfs_ha_automatic_failover_enabled:
        initiate_safe_zkfc_failover()
      else:
        raise Fail("Rolling Upgrade - dfs.ha.automatic-failover.enabled must be enabled to perform a rolling restart")
    namenode(action="stop", hdfs_binary=hdfs_binary, upgrade_type=upgrade_type, env=env)

  def status(self, env):
    import status_params
    env.set_params(status_params)
    namenode(action="status", env=env)

  def decommission(self, env):
    import params
    env.set_params(params)
    hdfs_binary = self.get_hdfs_binary()
    namenode(action="decommission", hdfs_binary=hdfs_binary)

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class NameNodeDefault(NameNode):

  def restore_snapshot(self, env):
    """
    Restore the snapshot during a Downgrade.
    """
    print "TODO AMBARI-12698"
    pass

  def prepare_express_upgrade(self, env):
    """
    During an Express Upgrade.
    If in HA, on the Active NameNode only, examine the directory dfs.namenode.name.dir and
    make sure that there is no "/previous" directory.

    Create a list of all the DataNodes in the cluster.
    hdfs dfsadmin -report > dfs-old-report-1.log

    hdfs dfsadmin -safemode enter
    hdfs dfsadmin -saveNamespace

    Copy the checkpoint files located in ${dfs.namenode.name.dir}/current into a backup directory.

    Finalize any prior HDFS upgrade,
    hdfs dfsadmin -finalizeUpgrade

    Prepare for a NameNode rolling upgrade in order to not lose any data.
    hdfs dfsadmin -rollingUpgrade prepare
    """
    import params
    Logger.info("Preparing the NameNodes for a NonRolling (aka Express) Upgrade.")

    if params.security_enabled:
      kinit_command = format("{params.kinit_path_local} -kt {params.hdfs_user_keytab} {params.hdfs_principal_name}")
      Execute(kinit_command, user=params.hdfs_user, logoutput=True)

    hdfs_binary = self.get_hdfs_binary()
    namenode_upgrade.prepare_upgrade_check_for_previous_dir()
    namenode_upgrade.prepare_upgrade_enter_safe_mode(hdfs_binary)
    namenode_upgrade.prepare_upgrade_save_namespace(hdfs_binary)
    namenode_upgrade.prepare_upgrade_backup_namenode_dir()
    namenode_upgrade.prepare_upgrade_finalize_previous_upgrades(hdfs_binary)

    # Call -rollingUpgrade prepare
    namenode_upgrade.prepare_rolling_upgrade(hdfs_binary)

  def prepare_rolling_upgrade(self, env):
    hfds_binary = self.get_hdfs_binary()
    namenode_upgrade.prepare_rolling_upgrade(hfds_binary)

  def wait_for_safemode_off(self, env):
    wait_for_safemode_off(self.get_hdfs_binary(), 30, True)

  def finalize_non_rolling_upgrade(self, env):
    hfds_binary = self.get_hdfs_binary()
    namenode_upgrade.finalize_upgrade(constants.UPGRADE_TYPE_NON_ROLLING, hfds_binary)

  def finalize_rolling_upgrade(self, env):
    hfds_binary = self.get_hdfs_binary()
    namenode_upgrade.finalize_upgrade(constants.UPGRADE_TYPE_ROLLING, hfds_binary)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    # When downgrading an Express Upgrade, the first thing we do is to revert the symlinks.
    # Therefore, we cannot call this code in that scenario.
    if upgrade_type != constants.UPGRADE_TYPE_NON_ROLLING or params.upgrade_direction != Direction.DOWNGRADE:
      conf_select.select(params.stack_name, "hadoop", params.version)

    stack_select.select("hadoop-hdfs-namenode", params.version)

  def post_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade post-restart")
    import params
    env.set_params(params)

    hdfs_binary = self.get_hdfs_binary()
    dfsadmin_base_command = get_dfsadmin_base_command(hdfs_binary)
    dfsadmin_cmd = dfsadmin_base_command + " -report -live"
    Execute(dfsadmin_cmd,
            user=params.hdfs_user,
            tries=60,
            try_sleep=10
    )

  def security_status(self, env):
    import status_params

    env.set_params(status_params)
    props_value_check = {"hadoop.security.authentication": "kerberos",
                         "hadoop.security.authorization": "true"}
    props_empty_check = ["hadoop.security.auth_to_local"]
    props_read_check = None
    core_site_expectations = build_expectations('core-site', props_value_check, props_empty_check,
                                                props_read_check)
    props_value_check = None
    props_empty_check = ['dfs.namenode.kerberos.internal.spnego.principal',
                         'dfs.namenode.keytab.file',
                         'dfs.namenode.kerberos.principal']
    props_read_check = ['dfs.namenode.keytab.file']
    hdfs_site_expectations = build_expectations('hdfs-site', props_value_check, props_empty_check,
                                                props_read_check)

    hdfs_expectations = {}
    hdfs_expectations.update(core_site_expectations)
    hdfs_expectations.update(hdfs_site_expectations)

    security_params = get_params_from_filesystem(status_params.hadoop_conf_dir,
                                                 {'core-site.xml': FILE_TYPE_XML,
                                                  'hdfs-site.xml': FILE_TYPE_XML})
    if 'core-site' in security_params and 'hadoop.security.authentication' in security_params['core-site'] and \
        security_params['core-site']['hadoop.security.authentication'].lower() == 'kerberos':
      result_issues = validate_security_config_properties(security_params, hdfs_expectations)
      if not result_issues:  # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ( 'hdfs-site' not in security_params
               or 'dfs.namenode.keytab.file' not in security_params['hdfs-site']
               or 'dfs.namenode.kerberos.principal' not in security_params['hdfs-site']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal are not set property."})
            return
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.hdfs_user,
                                security_params['hdfs-site']['dfs.namenode.keytab.file'],
                                security_params['hdfs-site']['dfs.namenode.kerberos.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
          self.put_structured_out({"securityState": "SECURED_KERBEROS"})
        except Exception as e:
          self.put_structured_out({"securityState": "ERROR"})
          self.put_structured_out({"securityStateErrorInfo": str(e)})
      else:
        issues = []
        for cf in result_issues:
          issues.append("Configuration file %s did not pass the validation. Reason: %s" % (cf, result_issues[cf]))
        self.put_structured_out({"securityIssuesFound": ". ".join(issues)})
        self.put_structured_out({"securityState": "UNSECURED"})
    else:
      self.put_structured_out({"securityState": "UNSECURED"})

  def rebalancehdfs(self, env):
    import params
    env.set_params(params)

    name_node_parameters = json.loads( params.name_node_params )
    threshold = name_node_parameters['threshold']
    _print("Starting balancer with threshold = %s\n" % threshold)

    rebalance_env = {'PATH': params.hadoop_bin_dir}

    if params.security_enabled:
      # Create the kerberos credentials cache (ccache) file and set it in the environment to use
      # when executing HDFS rebalance command. Use the md5 hash of the combination of the principal and keytab file
      # to generate a (relatively) unique cache filename so that we can use it as needed.
      # TODO: params.tmp_dir=/var/lib/ambari-agent/tmp. However hdfs user doesn't have access to this path.
      # TODO: Hence using /tmp
      ccache_file_name = "hdfs_rebalance_cc_" + _md5(format("{hdfs_principal_name}|{hdfs_user_keytab}")).hexdigest()
      ccache_file_path = os.path.join(tempfile.gettempdir(), ccache_file_name)
      rebalance_env['KRB5CCNAME'] = ccache_file_path

      # If there are no tickets in the cache or they are expired, perform a kinit, else use what
      # is in the cache
      klist_cmd = format("{klist_path_local} -s {ccache_file_path}")
      kinit_cmd = format("{kinit_path_local} -c {ccache_file_path} -kt {hdfs_user_keytab} {hdfs_principal_name}")
      if shell.call(klist_cmd, user=params.hdfs_user)[0] != 0:
        Execute(kinit_cmd, user=params.hdfs_user)

    def calculateCompletePercent(first, current):
      # avoid division by zero
      try:
        division_result = current.bytesLeftToMove/first.bytesLeftToMove
      except ZeroDivisionError:
        Logger.warning("Division by zero. Bytes Left To Move = {0}. Return 1.0".format(first.bytesLeftToMove))
        return 1.0
      return 1.0 - division_result


    def startRebalancingProcess(threshold, rebalance_env):
      rebalanceCommand = format('hdfs --config {hadoop_conf_dir} balancer -threshold {threshold}')
      return as_user(rebalanceCommand, params.hdfs_user, env=rebalance_env)

    command = startRebalancingProcess(threshold, rebalance_env)

    basedir = os.path.join(env.config.basedir, 'scripts')
    if(threshold == 'DEBUG'): #FIXME TODO remove this on PROD
      basedir = os.path.join(env.config.basedir, 'scripts', 'balancer-emulator')
      command = ['ambari-python-wrap','hdfs-command.py']

    _print("Executing command %s\n" % command)

    parser = hdfs_rebalance.HdfsParser()

    def handle_new_line(line, is_stderr):
      if is_stderr:
        return

      _print('[balancer] %s' % (line))
      pl = parser.parseLine(line)
      if pl:
        res = pl.toJson()
        res['completePercent'] = calculateCompletePercent(parser.initialLine, pl)

        self.put_structured_out(res)
      elif parser.state == 'PROCESS_FINISED' :
        _print('[balancer] %s' % ('Process is finished' ))
        self.put_structured_out({'completePercent' : 1})
        return

    Execute(command,
            on_new_line = handle_new_line,
            logoutput = False,
    )

    if params.security_enabled:
      # Delete the kerberos credentials cache (ccache) file
      File(ccache_file_path,
           action = "delete",
      )
      
  def get_log_folder(self):
    import params
    return params.hdfs_log_dir
  
  def get_user(self):
    import params
    return params.hdfs_user

  def get_pid_files(self):
    import status_params
    return [status_params.namenode_pid_file]

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class NameNodeWindows(NameNode):
  def install(self, env):
    import install_params
    self.install_packages(env)
    #TODO we need this for HA because of manual steps
    self.configure(env)

  def rebalancehdfs(self, env):
    from ambari_commons.os_windows import UserHelper, run_os_command_impersonated
    import params
    env.set_params(params)

    hdfs_username, hdfs_domain = UserHelper.parse_user_name(params.hdfs_user, ".")

    name_node_parameters = json.loads( params.name_node_params )
    threshold = name_node_parameters['threshold']
    _print("Starting balancer with threshold = %s\n" % threshold)

    def calculateCompletePercent(first, current):
      return 1.0 - current.bytesLeftToMove/first.bytesLeftToMove

    def startRebalancingProcess(threshold):
      rebalanceCommand = 'hdfs balancer -threshold %s' % threshold
      return ['cmd', '/C', rebalanceCommand]

    command = startRebalancingProcess(threshold)
    basedir = os.path.join(env.config.basedir, 'scripts')

    _print("Executing command %s\n" % command)

    parser = hdfs_rebalance.HdfsParser()
    returncode, stdout, err = run_os_command_impersonated(' '.join(command), hdfs_username, Script.get_password(params.hdfs_user), hdfs_domain)

    for line in stdout.split('\n'):
      _print('[balancer] %s %s' % (str(datetime.now()), line ))
      pl = parser.parseLine(line)
      if pl:
        res = pl.toJson()
        res['completePercent'] = calculateCompletePercent(parser.initialLine, pl)

        self.put_structured_out(res)
      elif parser.state == 'PROCESS_FINISED' :
        _print('[balancer] %s %s' % (str(datetime.now()), 'Process is finished' ))
        self.put_structured_out({'completePercent' : 1})
        break

    if returncode != None and returncode != 0:
      raise Fail('Hdfs rebalance process exited with error. See the log output')

def _print(line):
  sys.stdout.write(line)
  sys.stdout.flush()

if __name__ == "__main__":
  NameNode().execute()
