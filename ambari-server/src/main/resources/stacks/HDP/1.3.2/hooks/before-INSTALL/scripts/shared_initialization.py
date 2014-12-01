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

def setup_java():
  """
  Installs jdk using specific params, that comes from ambari-server
  """
  import params

  jdk_curl_target = format("{artifact_dir}/{jdk_name}")
  java_dir = os.path.dirname(params.java_home)
  java_exec = format("{java_home}/bin/java")
  tmp_java_dir = format("{tmp_dir}/jdk")

  if not params.jdk_name:
    return

  environment = {
    "no_proxy": format("{ambari_server_hostname}")
  }

  Execute(format("mkdir -p {artifact_dir} ; curl -kf -x \"\" "
                 "--retry 10 {jdk_location}/{jdk_name} -o {jdk_curl_target}"),
          path = ["/bin","/usr/bin/"],
          not_if = format("test -e {java_exec}"),
          environment = environment)

  if params.jdk_name.endswith(".bin"):
    chmod_cmd = ("chmod", "+x", jdk_curl_target)
    install_cmd = format("mkdir -p {tmp_java_dir} && cd {tmp_java_dir} && echo A | {jdk_curl_target} -noregister && sudo cp -r {tmp_java_dir}/* {java_dir}")
  elif params.jdk_name.endswith(".gz"):
    chmod_cmd = ("chmod","a+x", java_dir)
    install_cmd = format("mkdir -p {tmp_java_dir} && cd {tmp_java_dir} && tar -xf {jdk_curl_target} && sudo cp -r {tmp_java_dir}/* {java_dir}")

  Directory(java_dir
  )
  
  Execute(chmod_cmd,
          not_if = format("test -e {java_exec}"),
          sudo = True    
  )

  Execute(install_cmd,
          not_if = format("test -e {java_exec}")
  )

def install_packages():
  Package(['unzip'])
