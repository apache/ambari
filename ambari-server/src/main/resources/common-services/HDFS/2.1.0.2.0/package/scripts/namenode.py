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

from resource_management import Script
from resource_management.core.resources.system import Execute
from resource_management.core import shell
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
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
from hdfs_namenode import namenode
from hdfs import hdfs
import hdfs_rebalance
from utils import stop_zkfc_during_ru


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

  def install(self, env):
    import params
    self.install_packages(env, params.exclude_packages)
    env.set_params(params)
    #TODO we need this for HA because of manual steps
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hdfs("namenode")
    namenode(action="configure", env=env)

  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env)
    namenode(action="start", rolling_restart=rolling_restart, env=env)

  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    if rolling_restart and params.dfs_ha_enabled:
      if params.dfs_ha_automatic_failover_enabled:
        stop_zkfc_during_ru()
      else:
        raise Fail("Rolling Upgrade - dfs.ha.automatic-failover.enabled must be enabled to perform a rolling restart")
    namenode(action="stop", rolling_restart=rolling_restart, env=env)

  def status(self, env):
    import status_params
    env.set_params(status_params)
    namenode(action="status", rolling_restart=False, env=env)

  def decommission(self, env):
    import params
    env.set_params(params)
    namenode(action="decommission")

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class NameNodeDefault(NameNode):

  def get_stack_to_component(self):
    return {"HDP": "hadoop-hdfs-namenode"}

  def prepare_rolling_upgrade(self, env):
    namenode_upgrade.prepare_rolling_upgrade()

  def finalize_rolling_upgrade(self, env):
    namenode_upgrade.finalize_rolling_upgrade()

  def pre_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      conf_select.select(params.stack_name, "hadoop", params.version)
      hdp_select.select("hadoop-hdfs-namenode", params.version)

  def post_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade post-restart")
    import params
    env.set_params(params)

    Execute("hdfs dfsadmin -report -live",
            user=params.hdfs_user
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
      # TODO: params.tmp_dir=/var/lib/ambari-agent/data/tmp. However hdfs user doesn't have access to this path.
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
      return 1.0 - current.bytesLeftToMove/first.bytesLeftToMove


    def startRebalancingProcess(threshold, rebalance_env):
      rebalanceCommand = format('hdfs --config {hadoop_conf_dir} balancer -threshold {threshold}')
      return as_user(rebalanceCommand, params.hdfs_user, env=rebalance_env)

    command = startRebalancingProcess(threshold, rebalance_env)

    basedir = os.path.join(env.config.basedir, 'scripts')
    if(threshold == 'DEBUG'): #FIXME TODO remove this on PROD
      basedir = os.path.join(env.config.basedir, 'scripts', 'balancer-emulator')
      command = ['python','hdfs-command.py']

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

    if params.security_enabled and os.path.exists(ccache_file_path):
      # Delete the kerberos credentials cache (ccache) file
      os.remove(ccache_file_path)

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class NameNodeWindows(NameNode):
  def install(self, env):
    import install_params
    self.install_packages(env, install_params.exclude_packages)
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
