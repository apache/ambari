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

import sys
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import re
from ambari_commons import subprocess32
from ambari_commons import os_utils
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.system import Execute, File
from resource_management.core.source import StaticFile
from resource_management.core import shell

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

    params.HdfsResource(format("/user/{smokeuser}"),
                        type="directory",
                        action="create_on_execute",
                        owner=params.smokeuser,
                        mode=params.smoke_hdfs_user_mode,
                        )

    if params.stack_version_formatted_major and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.stack_version_formatted_major):
      path_to_distributed_shell_jar = format("{stack_root}/current/hadoop-yarn-client/hadoop-yarn-applications-distributedshell.jar")
    else:
      path_to_distributed_shell_jar = "/usr/lib/hadoop-yarn/hadoop-yarn-applications-distributedshell*.jar"
    yarn_distrubuted_shell_check_params = ["yarn org.apache.hadoop.yarn.applications.distributedshell.Client",
                                           "-shell_command", "ls", "-num_containers", "{number_of_nm}",
                                           "-jar", "{path_to_distributed_shell_jar}", "-timeout", "300000",
                                           "--queue", "{service_check_queue_name}"]
    yarn_distrubuted_shell_check_cmd = format(" ".join(yarn_distrubuted_shell_check_params))

    if params.security_enabled:
      kinit_cmd = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser_principal};")
      smoke_cmd = format("{kinit_cmd} {yarn_distrubuted_shell_check_cmd}")
    else:
      smoke_cmd = yarn_distrubuted_shell_check_cmd

    #return_code, out = shell.checked_call(smoke_cmd,
    #                                      path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    #                                      user=params.smokeuser,
    #                                      )

    #m = re.search("appTrackingUrl=(.*),\s", out)
    #app_url = m.group(1)

    #splitted_app_url = str(app_url).split('/')

    #for item in splitted_app_url:
    #  if "application" in item:
    #    application_name = item

    # Find out the active RM from RM list
    # Raise an exception if the active rm cannot be determined
    #active_rm_webapp_address = self.get_active_rm_webapp_address()
    #Logger.info("Active Resource Manager web app address is : " + active_rm_webapp_address);

    # Verify job state from active resource manager via rest api
    #info_app_url = params.scheme + "://" + active_rm_webapp_address + "/ws/v1/cluster/apps/" + application_name
    #get_app_info_cmd = "curl --negotiate -u : -ks --location-trusted --connect-timeout " + CURL_CONNECTION_TIMEOUT + " " + info_app_url

    #return_code, stdout, _ = get_user_call_output(get_app_info_cmd,
    #                                              user=params.smokeuser,
    #                                              path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
    #                                              )

    #try:
    #  json_response = json.loads(stdout)
    #except Exception as e:
    #  raise Fail(format("Response from YARN API was not a valid JSON. Response: {stdout}"))

    #if json_response is None or 'app' not in json_response or \
    #        'state' not in json_response['app'] or 'finalStatus' not in json_response['app']:
    #  raise Fail("Application " + app_url + " returns invalid data.")

    #if json_response['app']['state'] != "FINISHED" or json_response['app']['finalStatus'] != "SUCCEEDED":
    #  raise Fail("Application " + app_url + " state/status is not valid. Should be FINISHED/SUCCEEDED.")

  def get_active_rm_webapp_address(self):
    import params
    active_rm_webapp_address = None
    rm_webapp_addresses = params.rm_webapp_addresses_list
    if rm_webapp_addresses is not None and len(rm_webapp_addresses) > 0:
      for rm_webapp_address in rm_webapp_addresses:
        rm_state_url = params.scheme + "://" + rm_webapp_address + "/ws/v1/cluster/info"
        get_cluster_info_cmd = "curl --negotiate -u : -ks --location-trusted --connect-timeout " + CURL_CONNECTION_TIMEOUT + " " + rm_state_url
        try:
          return_code, stdout, _ = get_user_call_output(get_cluster_info_cmd,
                                                        user=params.smokeuser,
                                                        path='/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin',
                                                        )
          json_response = json.loads(stdout)
          if json_response is not None and 'clusterInfo' in json_response \
            and json_response['clusterInfo']['haState'] == "ACTIVE":
              active_rm_webapp_address = rm_webapp_address
              break
        except Exception as e:
          Logger.warning(format("Cluster info is not available from calling {get_cluster_info_cmd}"))

    if active_rm_webapp_address is None:
      raise Fail('Resource Manager state is not available. Failed to determine the active Resource Manager web application address from {0}'.format(','.join(rm_webapp_addresses)));
    return active_rm_webapp_address

if __name__ == "__main__":
  ServiceCheck().execute()
