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

import json

from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.libraries.script.script import Script


class VictoriaMetricsServiceCheck(Script):
  def service_check(self, env):
    import params

    env.set_params(params)
    url, use_auth = self._query_url(params)
    command = "curl --fail --silent --show-error --connect-timeout 10 --max-time 30"
    environment = {}
    if use_auth:
      command += ' --user "$VICTORIAMETRICS_AUTH"'
      environment["VICTORIAMETRICS_AUTH"] = "{0}:{1}".format(
        params.api_username, params.api_password
      )
    command += " " + shell.quote_bash_args(url)

    result, output = shell.call(
      command,
      user=params.victoriametrics_user,
      env=environment,
      quiet=True,
    )
    if result != 0:
      raise Fail("VictoriaMetrics query service check failed")
    try:
      response = json.loads(output)
    except ValueError as exception:
      raise Fail("VictoriaMetrics returned invalid JSON: {0}".format(exception))
    if response.get("status") != "success":
      raise Fail("VictoriaMetrics query service check did not succeed")

  @staticmethod
  def _query_url(params):
    if params.vmauth_hosts:
      return (
        "http://{0}:{1}/api/v1/query?query=1".format(
          params.vmauth_hosts[0], params.vmauth_http_port
        ),
        params.require_authentication,
      )
    if params.deployment_mode == "single" and params.server_hosts:
      return (
        "http://{0}:{1}/api/v1/query?query=1".format(
          params.server_hosts[0], params.server_http_port
        ),
        False,
      )
    if params.vmselect_hosts:
      return (
        "http://{0}:{1}/select/{2}/prometheus/api/v1/query?query=1".format(
          params.vmselect_hosts[0], params.vmselect_http_port, params.tenant_id
        ),
        False,
      )
    raise Fail("No VictoriaMetrics query endpoint is assigned")


if __name__ == "__main__":
  VictoriaMetricsServiceCheck().execute()
