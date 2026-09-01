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

from resource_management.core.exceptions import Fail
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.security_commons import (
  build_expectations,
  get_params_from_filesystem,
  validate_security_config_properties,
  FILE_TYPE_XML,
)

from ambari_commons.constants import UPGRADE_TYPE_ROLLING
from ambari_commons.os_family_impl import OsFamilyImpl

from hbase import hbase
from hbase_service import check_hbase_process_status, hbase_service
import upgrade
from setup_ranger_hbase import setup_ranger_hbase
from hbase_decommission import hbase_decommission, move_regions
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


class HbaseRegionServer(Script):
  def install(self, env):
    import params

    env.set_params(params)
    self.install_packages(env)

  def configure(self, env):
    import params

    env.set_params(params)
    hbase(name="regionserver")

  def decommission(self, env):
    raise Fail("RegionServer decommission must be issued through HBase Master")

  def graceful_stop(self, env, upgrade_type=None):
    import params

    # Mark Draining ZNode
    params.hbase_drain_only = False
    params.hbase_excluded_hosts = params.hostname
    env.set_params(params)
    hbase_decommission(env)

    # Stop RegionServer
    hbase_service("regionserver", action="stop")

    # Remove from Draining ZNode to make host useable on restarting regionserver
    params.hbase_drain_only = True
    env.set_params(params)
    hbase_decommission(env)

  def graceful_start(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    # Start RegionServer
    hbase_service("regionserver", action="start")

    move_regions(params, params.hostname, "load")


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HbaseRegionServerDefault(HbaseRegionServer):
  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    upgrade.prestart(env)

  def post_upgrade_restart(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    upgrade.post_regionserver(env)

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env)  # for security
    setup_ranger_hbase(upgrade_type=upgrade_type, service_name="hbase-regionserver")

    if upgrade_type == UPGRADE_TYPE_ROLLING and len(params.rs_hosts) > 5:
      self.graceful_start(env)
    else:
      hbase_service("regionserver", action="start")

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    if upgrade_type == UPGRADE_TYPE_ROLLING and len(params.rs_hosts) > 5:
      self.graceful_stop(env)
    else:
      hbase_service("regionserver", action="stop")

  def status(self, env):
    import status_params

    env.set_params(status_params)

    check_hbase_process_status(
      status_params.regionserver_pid_file,
      status_params.hbase_user,
      status_params.user_group,
      "regionserver",
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
        "hbase.regionserver.keytab.file",
        "hbase.regionserver.kerberos.principal",
      ]
      props_read_check = ["hbase.regionserver.keytab.file"]
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
            or "hbase.regionserver.keytab.file" not in security_params["hbase-site"]
            or "hbase.regionserver.kerberos.principal"
            not in security_params["hbase-site"]
          ):
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal are not set property."}
            )
            return

          keytab = security_params["hbase-site"][
            "hbase.regionserver.keytab.file"
          ]
          principal = security_params["hbase-site"][
            "hbase.regionserver.kerberos.principal"
          ].replace("_HOST", status_params.hostname.lower())
          with PrivateKerberosCache(
            status_params.hbase_user,
            status_params.user_group,
            temp_dir=status_params.tmp_dir,
            prefix="ambari-hbase-regionserver-status-",
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

    return [status_params.regionserver_pid_file]


if __name__ == "__main__":
  HbaseRegionServer().execute()
