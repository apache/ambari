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
import tempfile
from resource_management import *

def setup_users():
  """
  Creates users before cluster installation
  """
  import params

  for group in params.group_list:
    Group(group,
          )

  for user in params.user_list:
    User(user,
         gid=params.user_to_gid_dict[user],
         groups=params.user_to_groups_dict[user],
         fetch_nonlocal_groups=False
         )

def setup_java():
  """
  Install jdk using specific params.
  Install ambari jdk as well if the stack and ambari jdk are different.
  """
  import params
  __setup_java(custom_java_home=params.java_home, custom_jdk_name=params.jdk_name)
  if params.ambari_java_home and params.ambari_java_home != params.java_home:
    __setup_java(custom_java_home=params.ambari_java_home, custom_jdk_name=params.ambari_jdk_name)

def __setup_java(custom_java_home, custom_jdk_name):
  """
  Installs jdk using specific params, that comes from ambari-server
  """
  import params
  java_exec = format("{custom_java_home}/bin/java")

  if not os.path.isfile(java_exec):
    if not params.jdk_name: # if custom jdk is used.
      raise Fail(format("Unable to access {java_exec}. Confirm you have copied jdk to this host."))

    jdk_curl_target = format("{tmp_dir}/{custom_jdk_name}")
    java_dir = os.path.dirname(params.java_home)

    Directory(params.artifact_dir,
              create_parents = True,
              )

    File(jdk_curl_target,
         content = DownloadSource(format("{jdk_location}/{custom_jdk_name}")),
         not_if = format("test -f {jdk_curl_target}")
         )

    File(jdk_curl_target,
         mode = 0755,
         )

    tmp_java_dir = tempfile.mkdtemp(prefix="jdk_tmp_", dir=params.tmp_dir)

    try:
      if params.jdk_name.endswith(".bin"):
        chmod_cmd = ("chmod", "+x", jdk_curl_target)
        install_cmd = format("cd {tmp_java_dir} && echo A | {jdk_curl_target} -noregister && {sudo} cp -rp {tmp_java_dir}/* {java_dir}")
      elif params.jdk_name.endswith(".gz"):
        chmod_cmd = ("chmod","a+x", java_dir)
        install_cmd = format("cd {tmp_java_dir} && tar -xf {jdk_curl_target} && {sudo} cp -rp {tmp_java_dir}/* {java_dir}")

      Directory(java_dir
                )

      Execute(chmod_cmd,
              sudo = True,
              )

      Execute(install_cmd,
              )

    finally:
      Directory(tmp_java_dir, action="delete")

    File(format("{custom_java_home}/bin/java"),
         mode=0755,
         cd_access="a",
         )
    Execute(('chmod', '-R', '755', params.java_home),
            sudo = True,
            )
