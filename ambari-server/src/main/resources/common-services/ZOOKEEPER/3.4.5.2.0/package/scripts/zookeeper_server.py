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

import re

from resource_management import *
from resource_management.libraries.functions import get_unique_id_and_date
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.version import compare_versions, format_hdp_stack_version
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_JAAS_CONF
from resource_management.libraries.functions.format import format
from resource_management.core.shell import call

from zookeeper import zookeeper
from zookeeper_service import zookeeper_service


@retry(times=10, sleep_time=2, err_class=Fail)
def call_and_match_output(command, regex_expression, err_message):
  """
  Call the command and performs a regex match on the output for the specified expression.
  :param command: Command to call
  :param regex_expression: Regex expression to search in the output
  """
  # TODO Rolling Upgrade, does this work in Ubuntu? If it doesn't see dynamic_variable_interpretation.py to see how stdout was redirected
  # to a temporary file, which was then read.
  code, out = call(command)
  if not (out and re.search(regex_expression, out, re.IGNORECASE)):
    raise Fail(err_message)


class ZookeeperServer(Script):

  def get_stack_to_component(self):
    return {"HDP": "zookeeper-server"}

  def install(self, env):
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    zookeeper(type='server')

  def pre_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_hdp_stack_version(params.version), '2.2.0.0') >= 0:
      Execute(format("hdp-select set zookeeper-server {version}"))

  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    self.configure(env)
    zookeeper_service(action = 'start')

    self.save_component_version_to_structured_out(params.stack_name)

  def post_rolling_restart(self, env):
    Logger.info("Executing Rolling Upgrade post-restart")
    import params
    env.set_params(params)

    # Ensure that a quorum is still formed.
    unique = get_unique_id_and_date()
    create_command = format("echo 'create /{unique} mydata' | {zk_cli_shell}")
    list_command = format("echo 'ls /' | {zk_cli_shell}")
    delete_command = format("echo 'delete /{unique} ' | {zk_cli_shell}")

    quorum_err_message = "Failed to establish zookeeper quorum"
    call_and_match_output(create_command, 'Created', quorum_err_message)
    call_and_match_output(list_command, r"\[.*?" + unique + ".*?\]", quorum_err_message)
    call(delete_command)

  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    zookeeper_service(action = 'stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.zk_pid_file)

  def security_status(self, env):
    import status_params

    env.set_params(status_params)

    if status_params.security_enabled:
      # Expect the following files to be available in status_params.config_dir:
      #   zookeeper_jaas.conf
      #   zookeeper_client_jaas.conf
      try:
        props_value_check = None
        props_empty_check = ['Server/keyTab', 'Server/principal']
        props_read_check = ['Server/keyTab']
        zk_env_expectations = build_expectations('zookeeper_jaas', props_value_check, props_empty_check,
                                                 props_read_check)

        zk_expectations = {}
        zk_expectations.update(zk_env_expectations)

        security_params = get_params_from_filesystem(status_params.config_dir,
                                                   {'zookeeper_jaas.conf': FILE_TYPE_JAAS_CONF})

        result_issues = validate_security_config_properties(security_params, zk_expectations)
        if not result_issues:  # If all validations passed successfully
          # Double check the dict before calling execute
          if ( 'zookeeper_jaas' not in security_params
               or 'Server' not in security_params['zookeeper_jaas']
               or 'keyTab' not in security_params['zookeeper_jaas']['Server']
               or 'principal' not in security_params['zookeeper_jaas']['Server']):
            self.put_structured_out({"securityState": "ERROR"})
            self.put_structured_out({"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.zk_user,
                                security_params['zookeeper_jaas']['Server']['keyTab'],
                                security_params['zookeeper_jaas']['Server']['principal'],
                                status_params.hostname,
                                status_params.tmp_dir,
                                30)
          self.put_structured_out({"securityState": "SECURED_KERBEROS"})
        else:
          issues = []
          for cf in result_issues:
            issues.append("Configuration file %s did not pass the validation. Reason: %s" % (cf, result_issues[cf]))
          self.put_structured_out({"securityIssuesFound": ". ".join(issues)})
          self.put_structured_out({"securityState": "UNSECURED"})
      except Exception as e:
        self.put_structured_out({"securityState": "ERROR"})
        self.put_structured_out({"securityStateErrorInfo": str(e)})
    else:
      self.put_structured_out({"securityState": "UNSECURED"})


if __name__ == "__main__":
  ZookeeperServer().execute()
