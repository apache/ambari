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
from resource_management.core.logger import Logger
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script

import kerberos_utils


class KerberosServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    principal = params.smoke_test_principal
    keytab = params.smoke_test_keytab_file
    credentials_configured = bool(principal and keytab)
    if credentials_configured:
      kerberos_utils.validate_principal(principal, "Kerberos smoke principal")
      kerberos_utils.validate_user(params.smoke_user, "Kerberos smoke user")
      if params.default_group:
        kerberos_utils.validate_user(
          params.default_group, "Kerberos smoke user group"
        )
      credentials_available = kerberos_utils.keytab_is_regular_file(keytab)
    else:
      credentials_available = False

    if not credentials_available:
      if params.manage_identities:
        raise Fail(
          Logger.filter_text(
            "Failed to execute kinit test because the smoke principal or "
            "regular keytab file is unavailable"
          )
        )
      Logger.info(
        "Skipping the Kerberos service check because Ambari does not manage "
        "identities and smoke credentials are unavailable"
      )
      return

    Logger.info(f"Performing kinit using {principal}")
    with PrivateKerberosCache(
      params.smoke_user,
      params.default_group,
      prefix="ambari-kerberos-service-check-",
    ) as kerberos_cache:
      kerberos_cache.kinit(
        params.kinit_path_local,
        keytab,
        principal,
        timeout=params.service_check_timeout,
      )


if __name__ == "__main__":
  KerberosServiceCheck().execute()
