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

from resource_management.core.exceptions import ClientComponentHasNoStatus
from resource_management.core.logger import Logger
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import default
import kerberos_utils
from ambari_commons.kerberos.kerberos_common import (
  write_krb5_conf,
  write_keytab_file,
  delete_keytab_file,
  find_missing_keytabs,
)


class KerberosClient(Script):
  def install(self, env):
    install_packages = kerberos_utils.as_bool(
      default("/configurations/kerberos-env/install_packages", True),
      "kerberos-env/install_packages",
    )
    if install_packages:
      self.install_packages(env)
    else:
      Logger.info(
        "Kerberos client packages are not being installed; manual installation "
        "is required."
      )

    self.configure(env)

  def configure(self, env, upgrade_type=None, config_dir=None):
    import params

    env.set_params(params)
    if params.manage_krb5_conf:
      kerberos_utils.validate_managed_file(
        params.krb5_conf_path, "krb5.conf path", suffix="/krb5.conf"
      )
      write_krb5_conf(params)

  def status(self, env):
    raise ClientComponentHasNoStatus()

  def set_keytab(self, env):
    import params

    env.set_params(params)
    kerberos_utils.validate_keytab_records(
      params.kerberos_command_params, require_content=True
    )

    def output_hook(principal, keytab_file_path):
      if principal is not None:
        curr_content = dict(Script.structuredOut or {})
        keytabs = dict(curr_content.get("keytabs") or {})
        keytabs[principal.replace("_HOST", params.hostname)] = (
          keytab_file_path
        )
        curr_content["keytabs"] = keytabs
        self.put_structured_out(curr_content)

    write_keytab_file(params, output_hook)

  def remove_keytab(self, env):
    import params

    env.set_params(params)
    kerberos_utils.validate_keytab_records(params.kerberos_command_params)

    def output_hook(principal, keytab_file_path):
      if principal is not None:
        curr_content = dict(Script.structuredOut or {})
        removed_keytabs = dict(curr_content.get("removedKeytabs") or {})
        removed_keytabs[principal.replace("_HOST", params.hostname)] = (
          keytab_file_path
        )
        curr_content["removedKeytabs"] = removed_keytabs
        self.put_structured_out(curr_content)

    delete_keytab_file(params, output_hook)

  def check_keytabs(self, env):
    import params

    env.set_params(params)
    kerberos_utils.validate_keytab_records(params.kerberos_command_params)

    def output_hook(missing_keytabs):
      curr_content = dict(Script.structuredOut or {})
      curr_content["missing_keytabs"] = missing_keytabs
      self.put_structured_out(curr_content)

    find_missing_keytabs(params, output_hook)


if __name__ == "__main__":
  KerberosClient().execute()
