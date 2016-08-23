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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML

from hdfs_snamenode import snamenode
from hdfs import hdfs
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst

from resource_management.core.logger import Logger

class SNameNode(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hdfs("secondarynamenode")
    snamenode(action="configure")

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    snamenode(action="start")

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    snamenode(action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    snamenode(action="status")

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class SNameNodeDefault(SNameNode):

  def get_component_name(self):
    return "hadoop-hdfs-secondarynamenode"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      conf_select.select(params.stack_name, "hadoop", params.version)
      stack_select.select("hadoop-hdfs-secondarynamenode", params.version)

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
    props_empty_check = ['dfs.secondary.namenode.kerberos.internal.spnego.principal',
                         'dfs.secondary.namenode.keytab.file',
                         'dfs.secondary.namenode.kerberos.principal']
    props_read_check = ['dfs.secondary.namenode.keytab.file']
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
                  'dfs.secondary.namenode.keytab.file' not in security_params['hdfs-site'] or
                  'dfs.secondary.namenode.kerberos.principal' not in security_params['hdfs-site']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.hdfs_user,
                                security_params['hdfs-site']['dfs.secondary.namenode.keytab.file'],
                                security_params['hdfs-site'][
                                  'dfs.secondary.namenode.kerberos.principal'],
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
      
  def get_log_folder(self):
    import params
    return params.hdfs_log_dir
  
  def get_user(self):
    import params
    return params.hdfs_user

  def get_pid_files(self):
    import status_params
    return [status_params.snamenode_pid_file]

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class SNameNodeWindows(SNameNode):
  pass

if __name__ == "__main__":
  SNameNode().execute()
