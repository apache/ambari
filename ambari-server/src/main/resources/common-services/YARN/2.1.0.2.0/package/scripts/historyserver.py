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

Ambari Agent

"""

from resource_management import *
from resource_management.libraries.functions.dynamic_variable_interpretation import copy_tarballs_to_hdfs
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML

from yarn import yarn
from service import service

class HistoryServer(Script):

  def get_stack_to_component(self):
    return {"HDP": "hadoop-mapreduce-historyserver"}

  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    yarn(name="historyserver")

  def pre_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      Execute(format("hdp-select set hadoop-mapreduce-historyserver {version}"))
      copy_tarballs_to_hdfs('mapreduce', 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)

  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env) # FOR SECURITY
    copy_tarballs_to_hdfs('mapreduce', 'hadoop-mapreduce-historyserver', params.mapred_user, params.hdfs_user, params.user_group)
    service('historyserver', action='start', serviceName='mapreduce')


  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    service('historyserver', action='stop', serviceName='mapreduce')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.mapred_historyserver_pid_file)

  def security_status(self, env):
    import status_params
    env.set_params(status_params)
    if status_params.security_enabled:
      expectations = {}
      expectations.update(build_expectations('mapred-site',
                                             None,
                                             [
                                               'mapreduce.jobhistory.keytab',
                                               'mapreduce.jobhistory.principal',
                                               'mapreduce.jobhistory.webapp.spnego-keytab-file',
                                               'mapreduce.jobhistory.webapp.spnego-principal'
                                             ],
                                             None))

      security_params = get_params_from_filesystem(status_params.hadoop_conf_dir,
                                                   {'mapred-site.xml': FILE_TYPE_XML})
      result_issues = validate_security_config_properties(security_params, expectations)
      if not result_issues: # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ( 'mapred-site' not in security_params or
               'mapreduce.jobhistory.keytab' not in security_params['mapred-site'] or
               'mapreduce.jobhistory.principal' not in security_params['mapred-site'] or
               'mapreduce.jobhistory.webapp.spnego-keytab-file' not in security_params['mapred-site'] or
               'mapreduce.jobhistory.webapp.spnego-principal' not in security_params['mapred-site']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal not set."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.mapred_user,
                                security_params['mapred-site']['mapreduce.jobhistory.keytab'],
                                security_params['mapred-site']['mapreduce.jobhistory.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.mapred_user,
                                security_params['mapred-site']['mapreduce.jobhistory.webapp.spnego-keytab-file'],
                                security_params['mapred-site']['mapreduce.jobhistory.webapp.spnego-principal'],
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
  HistoryServer().execute()
