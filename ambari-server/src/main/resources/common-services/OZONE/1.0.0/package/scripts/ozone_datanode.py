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
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Execute, File
from resource_management.core.logger import Logger
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from ozone import ozone_service, setup_ozone_client
from ozone_service import ozone_service_check

class OzoneDatanode(Script):

  def get_component_name(self):
    return "ozone-datanode"

  def install(self, env):
    import params
    env.set_params(params)

    Logger.info("Installing Ozone DataNode")
    self.install_packages(env)
    setup_ozone_client()

  def configure(self, env, upgrade_type=None, config_dir=None):
    import params
    env.set_params(params)
    
    Logger.info("Configuring Ozone DataNode")
    ozone_service(name='datanode', action='configure')

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    
    Logger.info("Starting Ozone DataNode")
    self.configure(env)
    ozone_service(name='datanode', action='start')

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    
    Logger.info("Stopping Ozone DataNode")
    ozone_service(name='datanode', action='stop')

  def status(self, env):
    import params
    env.set_params(params)
    
    check_process_status(params.ozone_datanode_pid_file)

  def security_status(self, env):
    import status_params

    env.set_params(status_params)
    if status_params.security_enabled:
      props_value_check = {"ozone.security.enabled": "true"}
      props_empty_check = ["ozone.om.kerberos.keytab.file",
                           "ozone.om.kerberos.principal"]
      props_read_check = None
      ozone_site_expectations = build_expectations('ozone-site', props_value_check, props_empty_check,
                                                   props_read_check)

      ozone_expectations = {}
      ozone_expectations.update(ozone_site_expectations)

      security_params = get_params_from_filesystem(status_params.ozone_conf_dir,
                                                   {'ozone-site.xml': FILE_TYPE_XML})
      result_issues = validate_security_config_properties(security_params, ozone_expectations)
      if not result_issues:
        try:
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.ozone_user,
                                status_params.ozone_datanode_keytab_path,
                                status_params.ozone_datanode_kerberos_principal,
                                status_params.hostname,
                                status_params.tmp_dir)
          self.put_structured_out({"securityState": "SECURED_KERBEROS"})
        except Exception as e:
          self.put_structured_out({"securityState": "ERROR"})
          self.put_structured_out({"securityIssuesFound": str(e)})
      else:
        issues = []
        for cf in result_issues:
          issues.append("Configuration file %s did not pass the validation. Reason: %s" % (cf, result_issues[cf]))
        self.put_structured_out({"securityIssuesFound": ". ".join(issues)})
        self.put_structured_out({"securityState": "UNSECURED"})
    else:
      self.put_structured_out({"securityState": "UNSECURED"})

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Ozone DataNode Stack Upgrade pre-restart")
    import params
    env.set_params(params)

  def post_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Ozone DataNode Stack Upgrade post-restart")
    import params
    env.set_params(params)

  def get_log_folder(self):
    import params
    return params.ozone_log_dir

  def get_user(self):
    import params
    return params.ozone_user

  def get_pid_files(self):
    import params
    return [params.ozone_datanode_pid_file]

if __name__ == "__main__":
  OzoneDatanode().execute()
