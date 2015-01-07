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
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML

from oozie import oozie
from oozie_service import oozie_service

         
class OozieServer(Script):

  def get_stack_to_component(self):
    return {"HDP": "oozie-server"}

  def install(self, env):
    self.install_packages(env)
    
  def configure(self, env):
    import params
    env.set_params(params)

    oozie(is_server=True)
    
  def start(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    #TODO remove this when config command will be implemented
    self.configure(env)
    oozie_service(action='start')

    self.save_component_version_to_structured_out(params.stack_name)
    
  def stop(self, env, rolling_restart=False):
    import params
    env.set_params(params)
    oozie_service(action='stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_process_status(status_params.pid_file)


  def security_status(self, env):

    import status_params
    env.set_params(status_params)

    if status_params.security_enabled:
      expectations = {
        "oozie-site":
          build_expectations('oozie-site',
                             {
                               "oozie.authentication.type": "kerberos",
                               "oozie.service.AuthorizationService.security.enabled": "true",
                               "oozie.service.HadoopAccessorService.kerberos.enabled": "true"
                             },
                             [
                               "local.realm",
                               "oozie.authentication.kerberos.principal",
                               "oozie.authentication.kerberos.keytab",
                               "oozie.service.HadoopAccessorService.kerberos.principal",
                               "oozie.service.HadoopAccessorService.keytab.file"
                             ],
                             None)
      }

      security_params = get_params_from_filesystem(status_params.conf_dir,
                                                   {'oozie-site.xml': FILE_TYPE_XML})
      result_issues = validate_security_config_properties(security_params, expectations)
      if not result_issues: # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ('oozie-site' not in security_params
              or 'oozie.authentication.kerberos.principal' not in security_params['oozie-site']
              or 'oozie.authentication.kerberos.keytab' not in security_params['oozie-site']
              or 'oozie.service.HadoopAccessorService.kerberos.principal' not in security_params['oozie-site']
              or 'oozie.service.HadoopAccessorService.keytab.file' not in security_params['oozie-site']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out({"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.oozie_user,
                                security_params['oozie-site']['oozie.authentication.kerberos.keytab'],
                                security_params['oozie-site']['oozie.authentication.kerberos.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.oozie_user,
                                security_params['oozie-site']['oozie.service.HadoopAccessorService.keytab.file'],
                                security_params['oozie-site']['oozie.service.HadoopAccessorService.kerberos.principal'],
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
  OozieServer().execute()
