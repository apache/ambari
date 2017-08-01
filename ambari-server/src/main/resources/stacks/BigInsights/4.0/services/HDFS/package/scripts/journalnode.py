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
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.version import compare_versions, \
  format_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML

from utils import service
from hdfs import hdfs
import journalnode_upgrade


class JournalNode(Script):

  def install(self, env):
    import params

    self.install_packages(env)
    env.set_params(params)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_stack_version(params.version), '4.0.0.0') >= 0:
      stack_select.select_packages(params.version)
      #Execute(format("stack-select set hadoop-hdfs-journalnode {version}"))

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)
    Directory(params.hadoop_pid_dir_prefix,
              mode=0755,
              owner=params.hdfs_user,
              group=params.user_group
    )
    service(
      action="start", name="journalnode", user=params.hdfs_user,
      create_pid_dir=True,
      create_log_dir=True
    )

  def post_upgrade_restart(self, env, upgrade_type=None):
    if upgrade_type == "nonrolling":
      return

    Logger.info("Executing Stack Upgrade post-restart")
    import params
    env.set_params(params)
    journalnode_upgrade.post_upgrade_check()

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    service(
      action="stop", name="journalnode", user=params.hdfs_user,
      create_pid_dir=True,
      create_log_dir=True
    )

  def configure(self, env):
    import params

    Directory(params.jn_edits_dir,
              create_parents=True,
              cd_access="a",
              owner=params.hdfs_user,
              group=params.user_group
    )
    env.set_params(params)
    hdfs()
    pass

  def status(self, env):
    import status_params

    env.set_params(status_params)
    check_process_status(status_params.journalnode_pid_file)

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
    props_empty_check = ['dfs.journalnode.keytab.file',
                         'dfs.journalnode.kerberos.principal']
    props_read_check = ['dfs.journalnode.keytab.file']
    hdfs_site_expectations = build_expectations('hdfs-site', props_value_check, props_empty_check,
                                                props_read_check)

    hdfs_expectations = {}
    hdfs_expectations.update(hdfs_site_expectations)
    hdfs_expectations.update(core_site_expectations)

    security_params = get_params_from_filesystem(status_params.hadoop_conf_dir,
                                                 {'core-site.xml': FILE_TYPE_XML})
    if 'core-site' in security_params and 'hadoop.security.authentication' in security_params['core-site'] and \
        security_params['core-site']['hadoop.security.authentication'].lower() == 'kerberos':
      result_issues = validate_security_config_properties(security_params, hdfs_expectations)
      if not result_issues:  # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ('hdfs-site' not in security_params or
                  'dfs.journalnode.kerberos.keytab.file' not in security_params['hdfs-site'] or
                  'dfs.journalnode.kerberos.principal' not in security_params['hdfs-site']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.hdfs_user,
                                security_params['hdfs-site']['dfs.journalnode.kerberos.keytab.file'],
                                security_params['hdfs-site']['dfs.journalnode.kerberos.principal'],
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


if __name__ == "__main__":
  JournalNode().execute()
