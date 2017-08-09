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
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from hbase import hbase
from hbase_service import hbase_service
from hbase_decommission import hbase_decommission
import upgrade
from setup_ranger_hbase import setup_ranger_hbase
from ambari_commons import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl


class HbaseMaster(Script):
  def configure(self, env):
    import params
    env.set_params(params)
    hbase(name='master')

  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

  def decommission(self, env):
    import params
    env.set_params(params)
    hbase_decommission(env)


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HbaseMasterWindows(HbaseMaster):
  def start(self, env):
    import status_params
    self.configure(env)
    Service(status_params.hbase_master_win_service_name, action="start")

  def stop(self, env):
    import status_params
    env.set_params(status_params)
    Service(status_params.hbase_master_win_service_name, action="stop")

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_windows_service_status(status_params.hbase_master_win_service_name)



@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HbaseMasterDefault(HbaseMaster):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    upgrade.prestart(env)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env) # for security
    setup_ranger_hbase(upgrade_type=upgrade_type, service_name="hbase-master")
    hbase_service('master', action = 'start')
    
  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    hbase_service('master', action = 'stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)

    check_process_status(status_params.hbase_master_pid_file)

  def security_status(self, env):
    import status_params

    env.set_params(status_params)
    if status_params.security_enabled:
      props_value_check = {"hbase.security.authentication" : "kerberos",
                           "hbase.security.authorization": "true"}
      props_empty_check = ['hbase.master.keytab.file',
                           'hbase.master.kerberos.principal']
      props_read_check = ['hbase.master.keytab.file']
      hbase_site_expectations = build_expectations('hbase-site', props_value_check, props_empty_check,
                                                  props_read_check)

      hbase_expectations = {}
      hbase_expectations.update(hbase_site_expectations)

      security_params = get_params_from_filesystem(status_params.hbase_conf_dir,
                                                   {'hbase-site.xml': FILE_TYPE_XML})
      result_issues = validate_security_config_properties(security_params, hbase_expectations)
      if not result_issues:  # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ( 'hbase-site' not in security_params
               or 'hbase.master.keytab.file' not in security_params['hbase-site']
               or 'hbase.master.kerberos.principal' not in security_params['hbase-site']):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.hbase_user,
                                security_params['hbase-site']['hbase.master.keytab.file'],
                                security_params['hbase-site']['hbase.master.kerberos.principal'],
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
      
  def get_log_folder(self):
    import params
    return params.log_dir
  
  def get_user(self):
    import params
    return params.hbase_user

  def get_pid_files(self):
    import status_params
    return [status_params.hbase_master_pid_file]

if __name__ == "__main__":
  HbaseMaster().execute()
