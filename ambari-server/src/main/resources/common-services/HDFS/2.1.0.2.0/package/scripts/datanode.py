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
import datanode_upgrade
from hdfs_datanode import datanode
from resource_management import *
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, FILE_TYPE_XML
from hdfs import hdfs
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst
from utils import get_hdfs_binary

class DataNode(Script):

  def get_stack_to_component(self):
    return {"HDP": "hadoop-hdfs-datanode"}

  def get_hdfs_binary(self):
    """
    Get the name or path to the hdfs binary depending on the stack and version.
    """
    import params
    stack_to_comp = self.get_stack_to_component()
    if params.stack_name in stack_to_comp:
      return get_hdfs_binary(stack_to_comp[params.stack_name])
    return "hdfs"

  def install(self, env):
    import params
    self.install_packages(env, params.exclude_packages)
    env.set_params(params)

  def configure(self, env):
    import params
    env.set_params(params)
    hdfs("datanode")
    datanode(action="configure")

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    datanode(action="start")

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    # pre-upgrade steps shutdown the datanode, so there's no need to call

    hdfs_binary = self.get_hdfs_binary()
    if upgrade_type == "rolling":
      stopped = datanode_upgrade.pre_rolling_upgrade_shutdown(hdfs_binary)
      if not stopped:
        datanode(action="stop")
    else:
      datanode(action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    datanode(action = "status")


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class DataNodeDefault(DataNode):

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing DataNode Stack Upgrade pre-restart")
    import params
    env.set_params(params)
    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      conf_select.select(params.stack_name, "hadoop", params.version)
      hdp_select.select("hadoop-hdfs-datanode", params.version)

  def post_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing DataNode Stack Upgrade post-restart")
    import params
    env.set_params(params)
    hdfs_binary = self.get_hdfs_binary()
    # ensure the DataNode has started and rejoined the cluster
    datanode_upgrade.post_upgrade_check(hdfs_binary)

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
    props_empty_check = ['dfs.datanode.keytab.file',
                         'dfs.datanode.kerberos.principal']
    props_read_check = ['dfs.datanode.keytab.file']
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
          if ('hdfs-site' not in security_params or
                  'dfs.datanode.keytab.file' not in security_params['hdfs-site'] or
                  'dfs.datanode.kerberos.principal' not in security_params['hdfs-site']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.hdfs_user,
                                security_params['hdfs-site']['dfs.datanode.keytab.file'],
                                security_params['hdfs-site']['dfs.datanode.kerberos.principal'],
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

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class DataNodeWindows(DataNode):
  def install(self, env):
    import install_params
    self.install_packages(env, install_params.exclude_packages)

if __name__ == "__main__":
  DataNode().execute()
