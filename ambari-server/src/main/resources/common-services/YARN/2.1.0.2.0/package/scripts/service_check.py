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

from resource_management.libraries.functions.version import compare_versions
from resource_management import *
import sys
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import re
import subprocess
from ambari_commons import os_utils
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.libraries.functions.get_user_call_output import get_user_call_output

CURL_CONNECTION_TIMEOUT = '5'

class ServiceCheck(Script):
  def service_check(self, env):
    pass


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ServiceCheckWindows(ServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)

    yarn_exe = os_utils.quote_path(os.path.join(params.yarn_home, "bin", "yarn.cmd"))

    run_yarn_check_cmd = "cmd /C %s node -list" % yarn_exe

    component_type = 'rm'
    if params.hadoop_ssl_enabled:
      component_address = params.rm_webui_https_address
    else:
      component_address = params.rm_webui_address

    #temp_dir = os.path.abspath(os.path.join(params.hadoop_home, os.pardir)), "/tmp"
    temp_dir = os.path.join(os.path.dirname(params.hadoop_home), "temp")
    validateStatusFileName = "validateYarnComponentStatusWindows.py"
    validateStatusFilePath = os.path.join(temp_dir, validateStatusFileName)
    python_executable = sys.executable
    validateStatusCmd = "%s %s %s -p %s -s %s" % (python_executable, validateStatusFilePath, component_type, component_address, params.hadoop_ssl_enabled)

    if params.security_enabled:
      kinit_cmd = "%s -kt %s %s;" % (params.kinit_path_local, params.smoke_user_keytab, params.smokeuser)
      smoke_cmd = kinit_cmd + ' ' + validateStatusCmd
    else:
      smoke_cmd = validateStatusCmd

    File(validateStatusFilePath,
         content=StaticFile(validateStatusFileName)
    )

    Execute(smoke_cmd,
            tries=3,
            try_sleep=5,
            logoutput=True
    )

    Execute(run_yarn_check_cmd, logoutput=True)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ServiceCheckDefault(ServiceCheck):
  def service_check(self, env):
    import params
    env.set_params(params)

    if params.hdp_stack_version_major != "" and compare_versions(params.hdp_stack_version_major, '2.2') >= 0:
      path_to_distributed_shell_jar = "/usr/hdp/current/hadoop-yarn-client/hadoop-yarn-applications-distributedshell.jar"
    else:
      path_to_distributed_shell_jar = "/usr/lib/hadoop-yarn/hadoop-yarn-applications-distributedshell*.jar"

    yarn_distrubuted_shell_check_params = ["yarn org.apache.hadoop.yarn.applications.distributedshell.Client",
                                           "-shell_command", "ls", "-num_containers", "{number_of_nm}",
                                           "-jar", "{path_to_distributed_shell_jar}",
                                           "-jar", "{path_to_distributed_shell_jar}", "-timeout", "300000"]

    yarn_distrubuted_shell_check_cmd = format(" ".join(yarn_distrubuted_shell_check_params))

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
    for rm_webapp_address in params.rm_webapp_addresses_list:
      info_app_url = params.scheme + "://" + rm_webapp_address + "/ws/v1/cluster/apps/" + application_name

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
