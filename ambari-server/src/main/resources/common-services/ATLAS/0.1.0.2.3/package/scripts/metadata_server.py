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
from metadata import metadata
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import hdp_select
from resource_management import Execute, check_process_status, Script
from resource_management.libraries.functions import format
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.security_commons import build_expectations, \
  get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_PROPERTIES

class MetadataServer(Script):

  def get_stack_to_component(self):
    return {"HDP": "atlas-server"}

  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params
    env.set_params(params)
    metadata()

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.3.0.0') >= 0:
      # conf_select.select(params.stack_name, "atlas", params.version)
      hdp_select.select("atlas-server", params.version)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env)

    daemon_cmd = format('source {params.conf_dir}/atlas-env.sh ; {params.metadata_start_script}')
    no_op_test = format('ls {params.pid_file} >/dev/null 2>&1 && ps -p `cat {params.pid_file}` >/dev/null 2>&1')
    Execute(daemon_cmd,
            user=params.metadata_user,
            not_if=no_op_test
    )

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    daemon_cmd = format('source {params.conf_dir}/atlas-env.sh; {params.metadata_stop_script}')
    Execute(daemon_cmd,
            user=params.metadata_user,
    )
    Execute (format("rm -f {params.pid_file}"))

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.pid_file)

  def security_status(self, env):
    import status_params

    env.set_params(status_params)

    props_value_check = {'atlas.authentication.method': 'kerberos',
                         'atlas.http.authentication.enabled': 'true',
                         'atlas.http.authentication.type': 'kerberos'}
    props_empty_check = ['atlas.authentication.principal',
                         'atlas.authentication.keytab',
                         'atlas.http.authentication.kerberos.principal',
                         'atlas.http.authentication.kerberos.keytab']
    props_read_check = ['atlas.authentication.keytab',
                        'atlas.http.authentication.kerberos.keytab']
    atlas_site_expectations = build_expectations('application',
                                                    props_value_check,
                                                    props_empty_check,
                                                    props_read_check)

    atlas_expectations = {}
    atlas_expectations.update(atlas_site_expectations)

    security_params = get_params_from_filesystem(status_params.conf_dir,
                                                 {'application.properties': FILE_TYPE_PROPERTIES})
    result_issues = validate_security_config_properties(security_params, atlas_expectations)
    if not result_issues:  # If all validations passed successfully
      try:
        # Double check the dict before calling execute
        if ( 'application' not in security_params
             or 'atlas.authentication.keytab' not in security_params['application']
             or 'atlas.authentication.principal' not in security_params['application']):
          self.put_structured_out({"securityState": "UNSECURED"})
          self.put_structured_out(
            {"securityIssuesFound": "Atlas service keytab file or principal are not set property."})
          return

        if ( 'application' not in security_params
             or 'atlas.http.authentication.kerberos.keytab' not in security_params['application']
             or 'atlas.http.authentication.kerberos.principal' not in security_params['application']):
          self.put_structured_out({"securityState": "UNSECURED"})
          self.put_structured_out(
            {"securityIssuesFound": "HTTP Authentication keytab file or principal are not set property."})
          return

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

if __name__ == "__main__":
  MetadataServer().execute()
