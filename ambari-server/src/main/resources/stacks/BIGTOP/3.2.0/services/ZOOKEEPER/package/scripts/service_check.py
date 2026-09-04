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

from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)
from resource_management.libraries.script.script import Script

import zookeeper_cli
import zookeeper_utils


class ZookeeperServiceCheck(Script):
  """BIGTOP ZooKeeper service-check entry point."""


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ZookeeperServiceCheckLinux(ZookeeperServiceCheck):
  def service_check(self, env):
    import params

    env.set_params(params)
    cache_context = nullcontext(None)
    if params.security_enabled:
      zookeeper_utils.validate_keytab(
        params.smoke_user_keytab, "ZooKeeper service-check keytab"
      )
      cache_context = PrivateKerberosCache(
        params.smokeuser,
        params.user_group,
        temp_dir=params.tmp_dir,
        prefix="ambari-zookeeper-service-check-",
      )

    with cache_context as kerberos_cache:
      command_environment = {
        "JAVA_HOME": params.java64_home,
        "ZOOCFGDIR": params.config_dir,
        "ZOOCFG": "zoo.cfg",
      }
      if kerberos_cache is not None:
        command_environment = kerberos_cache.merge_environment(
          command_environment
        )
        kerberos_cache.kinit(
          params.kinit_path_local,
          params.smoke_user_keytab,
          params.smokeuser_principal,
          timeout=60,
        )
      zookeeper_cli.verify_ensemble(
        params.zookeeper_hosts,
        params.client_port,
        params.zk_cli_script,
        params.smokeuser,
        params.user_group,
        params.tmp_dir,
        command_environment,
      )


if __name__ == "__main__":
  ZookeeperServiceCheck().execute()
