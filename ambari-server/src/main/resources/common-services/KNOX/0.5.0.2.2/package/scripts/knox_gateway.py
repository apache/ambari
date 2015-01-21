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
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, validate_security_config_properties, get_params_from_filesystem, \
  FILE_TYPE_XML
import sys
import upgrade

from knox import knox
from ldap import ldap
from setup_ranger_knox import setup_ranger_knox

class KnoxGateway(Script):

  def get_stack_to_component(self):
    return {"HDP": "knox-server"}

  def install(self, env):
    self.install_packages(env)
    import params
    env.set_params(params)
    
    File(format('{knox_conf_dir}/topologies/sandbox.xml'),
         action = "delete",
    )

  def configure(self, env):
    import params
    env.set_params(params)
    knox()
    ldap()

  def pre_rolling_restart(self, env):
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      upgrade.backup_data()
      Execute(format("hdp-select set knox-server {version}"))


  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env)
    daemon_cmd = format('{knox_bin} start')
    no_op_test = format('ls {knox_pid_file} >/dev/null 2>&1 && ps -p `cat {knox_pid_file}` >/dev/null 2>&1')
    setup_ranger_knox(env)
    Execute(daemon_cmd,
            user=params.knox_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=no_op_test
    )

  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env)
    daemon_cmd = format('{knox_bin} stop')
    Execute(daemon_cmd,
            environment={'JAVA_HOME': params.java_home},
            user=params.knox_user,
    )
    Execute (format("rm -f {knox_pid_file}"))


  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.knox_pid_file)


  def configureldap(self, env):
    import params
    env.set_params(params)
    ldap()

  def startdemoldap(self, env):
    import params
    env.set_params(params)
    self.configureldap(env)
    daemon_cmd = format('{ldap_bin} start')
    no_op_test = format('ls {ldap_pid_file} >/dev/null 2>&1 && ps -p `cat {ldap_pid_file}` >/dev/null 2>&1')
    Execute(daemon_cmd,
            user=params.knox_user,
            environment={'JAVA_HOME': params.java_home},
            not_if=no_op_test
    )

  def stopdemoldap(self, env):
    import params
    env.set_params(params)
    self.configureldap(env)
    daemon_cmd = format('{ldap_bin} stop')
    Execute(daemon_cmd,
            environment={'JAVA_HOME': params.java_home},
            user=params.knox_user,
            )
    Execute (format("rm -f {ldap_pid_file}"))

  def security_status(self, env):
    import status_params

    env.set_params(status_params)

    if status_params.security_enabled:
      expectations = {}
      expectations.update(build_expectations(
        'krb5JAASLogin',
        None,
        ['keytab', 'principal'],
        None
      ))
      expectations.update(build_expectations(
        'gateway-site',
        {
          "gateway.hadoop.kerberos.secured" : "true"
        },
        None,
        None
      ))

      security_params = {
        "krb5JAASLogin":
          {
            'keytab': status_params.knox_keytab_path,
            'principal': status_params.knox_principal_name
          }
      }
      security_params.update(get_params_from_filesystem(status_params.knox_conf_dir,
        {"gateway-site.xml" : FILE_TYPE_XML}))

      result_issues = validate_security_config_properties(security_params, expectations)
      if not result_issues:  # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ( 'krb5JAASLogin' not in security_params
               or 'keytab' not in security_params['krb5JAASLogin']
               or 'principal' not in security_params['krb5JAASLogin']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out({"securityIssuesFound": "Keytab file and principal are not set."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.knox_user,
                                security_params['krb5JAASLogin']['keytab'],
                                security_params['krb5JAASLogin']['principal'],
                                status_params.hostname,
                                status_params.temp_dir)
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
  KnoxGateway().execute()
