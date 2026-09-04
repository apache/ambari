#!/usr/bin/env python3
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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.security_commons import (
  build_expectations,
  get_params_from_filesystem,
  validate_security_config_properties,
  FILE_TYPE_XML,
)
from hbase import hbase
from hbase_service import check_hbase_process_status, hbase_service
from hbase_decommission import hbase_decommission
import upgrade
from setup_ranger_hbase import setup_ranger_hbase
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


class HbaseMaster(Script):
  def configure(self, env):
    import params

    env.set_params(params)
    hbase(name="master")

  def install(self, env):
    import params

    env.set_params(params)
    self.install_packages(env)
    upgrade.select_phoenix_packages(params)

  def decommission(self, env):
    import params

    env.set_params(params)
    hbase_decommission(env)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HbaseMasterDefault(HbaseMaster):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    upgrade.prestart(env)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)  # for security
    setup_ranger_hbase(upgrade_type=upgrade_type, service_name="hbase-master")
    hbase_service("master", action="start")

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    hbase_service("master", action="stop")

  def status(self, env):
    import status_params

    env.set_params(status_params)

    check_hbase_process_status(
      status_params.hbase_master_pid_file,
      status_params.hbase_user,
      status_params.user_group,
      "master",
    )

  def security_status(self, env):
    import status_params

    env.set_params(status_params)
    if status_params.security_enabled:
      props_value_check = {
        "hbase.security.authentication": "kerberos",
        "hbase.security.authorization": "true",
      }
      props_empty_check = [
        "hbase.master.keytab.file",
        "hbase.master.kerberos.principal",
      ]
      props_read_check = ["hbase.master.keytab.file"]
      hbase_site_expectations = build_expectations(
        "hbase-site", props_value_check, props_empty_check, props_read_check
      )

      hbase_expectations = {}
      hbase_expectations.update(hbase_site_expectations)

      security_params = get_params_from_filesystem(
        status_params.hbase_conf_dir, {"hbase-site.xml": FILE_TYPE_XML}
      )
      result_issues = validate_security_config_properties(
        security_params, hbase_expectations
      )
      if not result_issues:  # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if (
            "hbase-site" not in security_params
            or "hbase.master.keytab.file" not in security_params["hbase-site"]
            or "hbase.master.kerberos.principal" not in security_params["hbase-site"]
          ):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal are not set property."}
            )
            return

          keytab = security_params["hbase-site"]["hbase.master.keytab.file"]
          principal = security_params["hbase-site"][
            "hbase.master.kerberos.principal"
          ].replace("_HOST", status_params.hostname.lower())
          with PrivateKerberosCache(
            status_params.hbase_user,
            status_params.user_group,
            temp_dir=status_params.tmp_dir,
            prefix="ambari-hbase-master-status-",
          ) as kerberos_cache:
            kerberos_cache.kinit(
              status_params.kinit_path_local, keytab, principal, timeout=30
            )
          self.put_structured_out({"securityState": "SECURED_KERBEROS"})
        except Exception as e:
          self.put_structured_out({"securityState": "ERROR"})
          self.put_structured_out({"securityStateErrorInfo": str(e)})
      else:
        issues = []
        for cf in result_issues:
          issues.append(
            f"Configuration file {cf} did not pass the validation. Reason: {result_issues[cf]}"
          )
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
