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

Ambari Agent

"""

import re
import sys
from resource_management.libraries.functions.version import compare_versions
from resource_management import *
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
from resource_management.libraries.functions.get_user_call_output import get_user_call_output

CURL_CONNECTION_TIMEOUT = '5'


class ServiceCheck(Script):
  def service_check(self, env):
    import params
    env.set_params(params)

    if params.stack_version != "" and compare_versions(params.stack_version, '4.0') >= 0:
      path_to_distributed_shell_jar = "/usr/iop/current/hadoop-yarn-client/hadoop-yarn-applications-distributedshell.jar"
    else:
      path_to_distributed_shell_jar = "/usr/lib/hadoop-yarn/hadoop-yarn-applications-distributedshell*.jar"

    yarn_distrubuted_shell_check_cmd = format("yarn org.apache.hadoop.yarn.applications.distributedshell.Client "
                                              "-shell_command ls -num_containers {number_of_nm} -jar {path_to_distributed_shell_jar}")

    if params.security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal};")
      smoke_cmd = format("{kinit_cmd} {yarn_distrubuted_shell_check_cmd}")
    else:
      smoke_cmd = yarn_distrubuted_shell_check_cmd

    return_code, out = shell.checked_call(smoke_cmd,
                                          path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                                          user=params.smokeuser,
                                          )

    m = re.search("appTrackingUrl=(.*),\s", out)
    app_url = m.group(1)

    splitted_app_url = str(app_url).split('/')

    for item in splitted_app_url:
      if "application" in item:
        application_name = item

    json_response_received = False
    for rm_host in params.rm_hosts:
      info_app_url = params.scheme + "://" + rm_host + ":" + params.rm_active_port + "/ws/v1/cluster/apps/" + application_name

      get_app_info_cmd = "curl --negotiate -u : -ksL --connect-timeout " + CURL_CONNECTION_TIMEOUT + " " + info_app_url

      return_code, stdout, _ = get_user_call_output(get_app_info_cmd,
                                            user=params.smokeuser,
                                            path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                                            )

      try:
        json_response = json.loads(stdout)

        json_response_received = True

        if json_response['app']['state'] != "FINISHED" or json_response['app']['finalStatus'] != "SUCCEEDED":
          raise Exception("Application " + app_url + " state/status is not valid. Should be FINISHED/SUCCEEDED.")
      except Exception as e:
        pass

    if not json_response_received:
      raise Exception("Could not get json response from YARN API")

if __name__ == "__main__":
  ServiceCheck().execute()
