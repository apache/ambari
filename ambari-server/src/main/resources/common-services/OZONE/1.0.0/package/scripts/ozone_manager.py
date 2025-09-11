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

class OzoneManager(Script):

  def get_component_name(self):
    return "ozone-manager"

  def install(self, env):
    import params
    env.set_params(params)

    Logger.info("Installing Ozone Manager")
    self.install_packages(env)
    setup_ozone_client()

  def configure(self, env, upgrade_type=None, config_dir=None):
    import params
    env.set_params(params)
    
    Logger.info("Configuring Ozone Manager")
    ozone_service(name='om', action='configure')

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    
    Logger.info("Starting Ozone Manager")
    self.configure(env)
    ozone_service(name='om', action='start')

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    
    Logger.info("Stopping Ozone Manager")
    ozone_service(name='om', action='stop')

  def status(self, env):
    import params
    env.set_params(params)
    
    check_process_status(params.ozone_manager_pid_file)

  def decommission(self, env):
    import params
    env.set_params(params)
    
    Logger.info("Decommissioning Ozone DataNodes")
    
    # Get the list of hosts to decommission from the command parameters
    hosts_to_decommission = params.slave_hosts if hasattr(params, 'slave_hosts') else []
    
    if hosts_to_decommission:
      # Create exclude file with hosts to decommission
      exclude_file_path = format("{ozone_conf_dir}/dfs.exclude")
      Logger.info(format("Creating exclude file at {exclude_file_path}"))
      
      File(exclude_file_path,
           content='\n'.join(hosts_to_decommission),
           owner=params.ozone_user,
           group=params.user_group,
           mode=0644)
      
      # Execute ozone admin command to refresh nodes
      refresh_cmd = format("{ozone_bin_dir}/ozone admin -refreshNodes")
      Execute(refresh_cmd,
              user=params.ozone_user,
              logoutput=True)
      
      Logger.info("Decommission command executed successfully")
    else:
      Logger.warning("No hosts provided for decommissioning")

  def recommission(self, env):
    import params
    env.set_params(params)
    
    Logger.info("Recommissioning Ozone DataNodes")
    
    # Get the list of hosts to recommission from the command parameters
    hosts_to_recommission = params.slave_hosts if hasattr(params, 'slave_hosts') else []
    
    if hosts_to_recommission:
      # Remove hosts from exclude file or create empty exclude file
      exclude_file_path = format("{ozone_conf_dir}/dfs.exclude")
      Logger.info(format("Updating exclude file at {exclude_file_path}"))
      
      # Read current exclude file if it exists
      current_excluded = set()
      try:
        with open(exclude_file_path, 'r') as f:
          current_excluded = set(line.strip() for line in f if line.strip())
      except IOError:
        pass  # File doesn't exist, which is fine
      
      # Remove recommissioned hosts from excluded set
      updated_excluded = current_excluded - set(hosts_to_recommission)
      
      # Write updated exclude file
      File(exclude_file_path,
           content='\n'.join(updated_excluded),
           owner=params.ozone_user,
           group=params.user_group,
           mode=0644)
      
      # Execute ozone admin command to refresh nodes
      refresh_cmd = format("{ozone_bin_dir}/ozone admin -refreshNodes")
      Execute(refresh_cmd,
              user=params.ozone_user,
              logoutput=True)
      
      Logger.info("Recommission command executed successfully")
    else:
      Logger.warning("No hosts provided for recommissioning")

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
                                status_params.ozone_manager_keytab_path,
                                status_params.ozone_manager_kerberos_principal,
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
    Logger.info("Executing Ozone Manager Stack Upgrade pre-restart")
    import params
    env.set_params(params)

  def post_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Ozone Manager Stack Upgrade post-restart")
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
    return [params.ozone_manager_pid_file]

if __name__ == "__main__":
  OzoneManager().execute()
