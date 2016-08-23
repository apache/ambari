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
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.libraries.functions import format
from resource_management.libraries.functions import check_process_status
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.security_commons import build_expectations
from resource_management.libraries.functions.security_commons import cached_kinit_executor
from resource_management.libraries.functions.security_commons import get_params_from_filesystem
from resource_management.libraries.functions.security_commons import validate_security_config_properties
from resource_management.libraries.functions.security_commons import FILE_TYPE_XML
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions import StackFeature

from accumulo_configuration import setup_conf_dir
from accumulo_service import accumulo_service

class AccumuloScript(Script):

  # a mapping between the component named used by these scripts and the name
  # which is used by <stack-selector-tool>
  COMPONENT_TO_STACK_SELECT_MAPPING = {
    "gc" : "accumulo-gc",
    "master" : "accumulo-master",
    "monitor" : "accumulo-monitor",
    "tserver" : "accumulo-tablet",
    "tracer" : "accumulo-tracer"
  }

  def __init__(self, component):
    import status_params
    env.set_params(status_params)

    self.component = component
    self.pid_file = format("{pid_dir}/accumulo-{accumulo_user}-{component}.pid")


  def get_component_name(self):
    """
    Gets the <stack-selector-tool> component name given the script component
    :return:  the name of the component on the stack which is used by
              <stack-selector-tool>
    """
    if self.component not in self.COMPONENT_TO_STACK_SELECT_MAPPING:
      return None

    stack_component = self.COMPONENT_TO_STACK_SELECT_MAPPING[self.component]
    return stack_component


  def install(self, env):
    self.install_packages(env)


  def configure(self, env):
    import params
    env.set_params(params)

    setup_conf_dir(name=self.component)


  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # for security

    accumulo_service( self.component, action = 'start')


  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    accumulo_service( self.component, action = 'stop')


  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(self.pid_file)


  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)

    # this function should not execute if the version can't be determined or
    # the stack does not support rolling upgrade
    if not (params.stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.stack_version_formatted)):
      return

    if self.component not in self.COMPONENT_TO_STACK_SELECT_MAPPING:
      Logger.info("Unable to execute an upgrade for unknown component {0}".format(self.component))
      raise Fail("Unable to execute an upgrade for unknown component {0}".format(self.component))

    stack_component = self.COMPONENT_TO_STACK_SELECT_MAPPING[self.component]

    Logger.info("Executing Accumulo Upgrade pre-restart for {0}".format(stack_component))
    conf_select.select(params.stack_name, "accumulo", params.version)
    stack_select.select(stack_component, params.version)

    # some accumulo components depend on the client, so update that too
    stack_select.select("accumulo-client", params.version)


  def security_status(self, env):
    import status_params

    env.set_params(status_params)

    props_value_check = {}
    props_empty_check = ['general.kerberos.keytab',
                         'general.kerberos.principal']
    props_read_check = ['general.kerberos.keytab']
    accumulo_site_expectations = build_expectations('accumulo-site',
      props_value_check, props_empty_check, props_read_check)

    accumulo_expectations = {}
    accumulo_expectations.update(accumulo_site_expectations)

    security_params = get_params_from_filesystem(status_params.conf_dir,
      {'accumulo-site.xml': FILE_TYPE_XML})

    result_issues = validate_security_config_properties(security_params, accumulo_expectations)
    if not result_issues:  # If all validations passed successfully
      try:
        # Double check the dict before calling execute
        if ( 'accumulo-site' not in security_params
             or 'general.kerberos.keytab' not in security_params['accumulo-site']
             or 'general.kerberos.principal' not in security_params['accumulo-site']):
          self.put_structured_out({"securityState": "UNSECURED"})
          self.put_structured_out(
            {"securityIssuesFound": "Keytab file or principal are not set property."})
          return

        cached_kinit_executor(status_params.kinit_path_local,
          status_params.accumulo_user,
          security_params['accumulo-site']['general.kerberos.keytab'],
          security_params['accumulo-site']['general.kerberos.principal'],
          status_params.hostname,
          status_params.tmp_dir,
          30)

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
      
  def get_log_folder(self):
    import params
    return params.log_dir

  def get_pid_files(self):
    import status_params
    env.set_params(status_params)

    return [self.pid_file]

  def get_user(self):
    import params
    return params.accumulo_user

if __name__ == "__main__":
  AccumuloScript().fail_with_error('component unspecified')
