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

from contextlib import nullcontext

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.core.signal_utils import TerminateStrategy
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script

import infra_solr_utils


CHECK_TIMEOUT_SECONDS = 30


class InfraServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    if not params.infra_solr_hosts:
      raise Fail("Infra Solr service check requires at least one server host")
    infra_solr_utils.validate_executable("/usr/bin/curl", "curl executable")

    cache_context = nullcontext(None)
    if params.security_enabled:
      infra_solr_utils.validate_executable(
        params.kinit_path_local, "kinit executable"
      )
      infra_solr_utils.validate_keytab(
        params.smoke_user_keytab, "Infra Solr service-check keytab"
      )
      infra_solr_utils.validate_principal(
        params.smokeuser_principal, "Infra Solr service-check principal"
      )
      cache_context = PrivateKerberosCache(
        params.smokeuser,
        params.user_group,
        temp_dir=params.tmp_dir,
        prefix="ambari-infra-solr-check-",
      )

    with cache_context as cache:
      environment = None
      if cache is not None:
        cache.kinit(
          params.kinit_path_local,
          params.smoke_user_keytab,
          params.smokeuser_principal,
          timeout=30,
        )
        environment = cache.environment

      failures = []
      protocol = "https" if params.infra_solr_ssl_enabled else "http"
      for host in params.infra_solr_hosts:
        url = (
          f"{protocol}://{host}:{params.infra_solr_port}"
          "/solr/admin/info/system?wt=json"
        )
        command = [
          "/usr/bin/curl",
          "--disable",
          "--silent",
          "--show-error",
          "--fail",
          "--output",
          "/dev/null",
          "--connect-timeout",
          "5",
          "--max-time",
          str(CHECK_TIMEOUT_SECONDS),
        ]
        if params.security_enabled:
          command.extend(("--negotiate", "--user", ":"))
        command.extend(("--url", url))
        options = {}
        if environment is not None:
          options["environment"] = environment
        try:
          Execute(
            tuple(command),
            user=params.smokeuser,
            timeout=CHECK_TIMEOUT_SECONDS + 5,
            timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_GROUP,
            logoutput=True,
            **options,
          )
          return
        except Exception as error:
          failures.append(f"{host}: {error}")
          Logger.warning(f"Infra Solr service check failed for {host}: {error}")
      raise Fail("Connection to all Infra Solr servers failed: " + "; ".join(failures))


if __name__ == "__main__":
  InfraServiceCheck().execute()
