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

from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script

class InfraServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    Logger.info('Infra Service Check ...')
    if "infra-solr-env" in params.config['configurations'] \
      and params.infra_solr_hosts is not None \
      and len(params.infra_solr_hosts) > 0:
      solr_protocol = "https" if params.infra_solr_ssl_enabled else "http"
      solr_host = params.infra_solr_hosts[0] # choose the first solr host
      solr_port = params.infra_solr_port
      solr_url = format("{solr_protocol}://{solr_host}:{solr_port}/solr/#/")

      smokeuser_kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal};") if params.security_enabled else ""
      smoke_infra_solr_cmd = format("{smokeuser_kinit_cmd} curl -s -o /dev/null -w'%{{http_code}}' --negotiate -u: -k {solr_url} | grep 200")
      Execute(smoke_infra_solr_cmd,
              tries = 40,
              try_sleep=3,
              user=params.smokeuser,
              logoutput=True)

if __name__ == "__main__":
  InfraServiceCheck().execute()