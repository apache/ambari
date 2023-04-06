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
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.format import format
from resource_management.core.resources.system import Execute


class ServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)
    for superset_host in params.config['clusterHostInfo']['superset_hosts']:
      Execute(format(
        "curl -s -o /dev/null -w'%{{http_code}}' --negotiate -u: -k {superset_host}:{params.superset_webserver_port}/health | grep 200"),
        tries=10,
        try_sleep=3,
        logoutput=True)

if __name__ == "__main__":
  ServiceCheck().execute()
