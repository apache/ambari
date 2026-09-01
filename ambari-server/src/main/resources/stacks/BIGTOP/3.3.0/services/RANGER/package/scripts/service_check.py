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

from resource_management.libraries.script import Script
from resource_management.core.resources.system import Execute
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
import os
from urllib.parse import urlsplit


class RangerServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    self.check_ranger_admin_service(
      params.ranger_external_url,
      params.upgrade_marker_file,
      params.security_enabled,
    )

  def check_ranger_admin_service(
    self, ranger_external_url, upgrade_marker_file, security_enabled
  ):
    if self.is_ru_rangeradmin_in_progress(upgrade_marker_file):
      Logger.info(
        "Ranger admin process not running - skipping as stack upgrade is in progress"
      )
    else:
      try:
        parsed_url = urlsplit(ranger_external_url)
        hostname = parsed_url.hostname
        port = parsed_url.port
      except (TypeError, ValueError) as error:
        raise Fail("Ranger Admin URL is invalid") from error
      if (
        parsed_url.scheme not in ("http", "https")
        or not hostname
        or parsed_url.username is not None
        or parsed_url.password is not None
        or parsed_url.query
        or parsed_url.fragment
        or (port is not None and not 1 <= port <= 65535)
      ):
        raise Fail("Ranger Admin URL is invalid")
      command = [
        "/usr/bin/curl",
        "--fail",
        "--silent",
        "--show-error",
        "--output",
        "/dev/null",
        "--connect-timeout",
        "5",
        "--max-time",
        "20",
      ]
      if security_enabled:
        command.extend(("--negotiate", "--user", ":"))
      command.append(ranger_external_url.rstrip("/") + "/login.jsp")
      Execute(
        tuple(command),
        tries=10,
        try_sleep=3,
        timeout=25,
      )

  def is_ru_rangeradmin_in_progress(self, upgrade_marker_file):
    return os.path.isfile(upgrade_marker_file)


if __name__ == "__main__":
  RangerServiceCheck().execute()
