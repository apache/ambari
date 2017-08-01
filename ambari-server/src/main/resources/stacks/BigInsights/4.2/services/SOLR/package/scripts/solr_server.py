#!/usr/bin/env python
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
from resource_management import *
from resource_management.libraries.functions import stack_select
from solr_service import solr_service
from solr import solr

class SolrServer(Script):
  def install(self, env):
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    solr(type='server', upgrade_type=upgrade_type)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)
    if params.version and compare_versions(format_stack_version(params.version), '4.1.0.0') >= 0:
      stack_select.select_packages(params.version)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)
    solr_service(action = 'start')

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    solr_service(action = 'stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.solr_pid_file)

  def security_status(self, env):
    import status_params
    env.set_params(status_params)
    if status_params.security_enabled:
      props_value_check = {"solr.hdfs.security.kerberos.enabled":"true"}
      props_empty_check = ["solr.hdfs.security.kerberos.keytabfile",
                           "solr.hdfs.security.kerberos.principal"]
      props_read_check = ["solr.hdfs.security.kerberos.keytabfile"]
      solr_site_props = build_expectations('solr-site', props_value_check, props_empty_check, props_read_check)

      solr_expectations = {}
      solr_expectations.update(solr_site_props)

      security_params = get_params_from_filesystem(status_params.solr_conf_dir,
                                                   {'solr-site.xml': FILE_TYPE_XML})
      result_issues = validate_security_config_properties(security_params,solr_expectations)

      if not result_issues: # If all validations passed successfully
        try:
          if 'solr-site' not in security_params \
            or 'solr.hdfs.security.kerberos.keytabfile' not in security_params['solr-site'] \
            or 'solr.hdfs.security.kerberos.principal' not in security_params['solr-site']:
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out({"securityIssuesFound": "Keytab file or principal are not set property."})
            return
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.solr_user,
                                security_params['solr-site']['solr.hdfs.security.kerberos.keytabfile'],
                                security_params['solr-site']['solr.hdfs.security.kerberos.principal'],
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
  SolrServer().execute()
