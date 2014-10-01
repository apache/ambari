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

from resource_management import *



def setup_jce():
  import params
  
  if not params.jdk_name:
    return
  
  environment = {
    "no_proxy": format("{ambari_server_hostname}")
  }
  
  if params.jce_policy_zip is not None:
    jce_curl_target = format("{artifact_dir}/{jce_policy_zip}")
    download_jce = format("mkdir -p {artifact_dir}; \
    curl -kf -x \"\" --retry 10 \
    {jce_location}/{jce_policy_zip} -o {jce_curl_target}")
    Execute( download_jce,
             path = ["/bin","/usr/bin/"],
             not_if =format("test -e {jce_curl_target}"),
             ignore_failures = True,
             environment = environment
    )
  elif params.security_enabled:
    # Something weird is happening
    raise Fail("Security is enabled, but JCE policy zip is not specified.")
  
  if params.security_enabled:
    security_dir = format("{java_home}/jre/lib/security")
    extract_cmd = format("rm -f local_policy.jar; rm -f US_export_policy.jar; unzip -o -j -q {jce_curl_target}")
    Execute(extract_cmd,
            only_if = format("test -e {security_dir} && test -f {jce_curl_target}"),
            cwd  = security_dir,
            path = ['/bin/','/usr/bin']
    )
