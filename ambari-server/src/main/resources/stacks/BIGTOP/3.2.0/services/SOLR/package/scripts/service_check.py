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

import os
import uuid

from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.libraries.script.script import Script


def create_private_kerberos_cache(user, group):
  cache_dir = os.path.join(
    "/tmp", f"ambari-solr-service-check-{uuid.uuid4().hex}"
  )
  try:
    os.mkdir(cache_dir, 0o700)
  except FileExistsError as error:
    raise Fail(f"Kerberos cache directory collision at {cache_dir}") from error
  except OSError as error:
    raise Fail(
      f"Could not create Kerberos cache directory {cache_dir}"
    ) from error

  cache_path = os.path.join(cache_dir, "krb5cc")
  try:
    Directory(
      cache_dir,
      owner=user,
      group=group,
      mode=0o700,
    )
  except Exception as ownership_error:
    try:
      Directory(cache_dir, action="delete")
    except Exception as rollback_error:
      raise Fail(
        f"Could not configure Kerberos cache directory {cache_dir}: "
        f"{ownership_error}; rollback also failed: {rollback_error}"
      ) from ownership_error
    raise
  return cache_dir, cache_path


def cleanup_private_kerberos_cache(cache_dir, cache_path):
  cleanup_errors = []
  try:
    File(cache_path, action="delete")
  except Exception as error:
    cleanup_errors.append(error)
  try:
    Directory(cache_dir, action="delete")
  except Exception as error:
    cleanup_errors.append(error)
  if cleanup_errors:
    cleanup_details = "; ".join(str(error) for error in cleanup_errors)
    raise Fail(
      f"Could not remove Kerberos cache directory {cache_dir}: "
      f"{cleanup_details}"
    ) from cleanup_errors[0]


class ServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)

    Logger.info("Service Check ...")
    if "solr-env" not in params.config["configurations"]:
      raise Fail("Solr service check requires solr-env configuration")
    if not getattr(params, "solr_hosts", None):
      raise Fail("Solr service check requires at least one Solr server host")

    ccache_path = None
    ccache_dir = None
    kerberos_environment = None
    service_check_error = None
    try:
      solr_protocol = "https" if params.solr_ssl_enabled else "http"
      solr_host = params.solr_hosts[0]  # choose the first solr host
      solr_port = params.solr_port
      solr_url = (
        f"{solr_protocol}://{solr_host}:{solr_port}"
        "/solr/admin/info/system?wt=json"
      )

      if params.security_enabled:
        ccache_dir, ccache_path = create_private_kerberos_cache(
          params.smokeuser,
          params.user_group,
        )
        ccache_name = f"FILE:{ccache_path}"
        kerberos_environment = {"KRB5CCNAME": ccache_name}
        Execute(
          (
            str(params.kinit_path_local),
            "-c",
            ccache_name,
            "-kt",
            str(params.smoke_user_keytab),
            str(params.smokeuser_principal),
          ),
          environment=kerberos_environment,
          user=params.smokeuser,
          logoutput=True,
        )

      curl_argv = [
        "curl",
        "--disable",
        "--silent",
        "--show-error",
        "--fail",
        "--output",
        "/dev/null",
        "--connect-timeout",
        "5",
        "--max-time",
        "10",
      ]
      if params.security_enabled:
        curl_argv.extend(("--negotiate", "--user", ":"))
      curl_argv.append(solr_url)
      execute_options = {}
      if kerberos_environment is not None:
        execute_options["environment"] = kerberos_environment
      Execute(
        tuple(curl_argv),
        tries=20,
        try_sleep=3,
        user=params.smokeuser,
        logoutput=True,
        **execute_options,
      )
    except Exception as error:
      service_check_error = error
      raise
    finally:
      if ccache_dir is not None:
        try:
          cleanup_private_kerberos_cache(ccache_dir, ccache_path)
        except Exception as cleanup_error:
          if service_check_error is not None:
            raise Fail(
              f"Solr service check failed: {service_check_error}; "
              f"Kerberos cache cleanup also failed: {cleanup_error}"
            ) from service_check_error
          raise


if __name__ == "__main__":
  ServiceCheck().execute()
