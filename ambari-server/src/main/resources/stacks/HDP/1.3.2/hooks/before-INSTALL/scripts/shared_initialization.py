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

def setup_users():
  """
  Creates users before cluster installation
  """
  import params

  Group(params.user_group,
         ignore_failures = params.ignore_groupsusers_create
  )
  Group(params.smoke_user_group,
         ignore_failures = params.ignore_groupsusers_create
  )
  Group(params.proxyuser_group,
         ignore_failures = params.ignore_groupsusers_create
  )
  User(params.smoke_user,
       gid=params.user_group,
       groups=[params.proxyuser_group],
       ignore_failures = params.ignore_groupsusers_create
  )
  
  smoke_user_dirs = format(
    "/tmp/hadoop-{smoke_user},/tmp/hsperfdata_{smoke_user},/home/{smoke_user},/tmp/{smoke_user},/tmp/sqoop-{smoke_user}")
  set_uid(params.smoke_user, smoke_user_dirs)

  if params.has_hbase_masters:
    User(params.hbase_user,
         gid = params.user_group,
         groups=[params.user_group],
         ignore_failures = params.ignore_groupsusers_create
    )
    hbase_user_dirs = format(
      "/home/{hbase_user},/tmp/{hbase_user},/usr/bin/{hbase_user},/var/log/{hbase_user},{hbase_tmp_dir}")
    set_uid(params.hbase_user, hbase_user_dirs)

  if params.has_nagios:
    Group(params.nagios_group,
         ignore_failures = params.ignore_groupsusers_create
    )
    User(params.nagios_user,
         gid=params.nagios_group,
         ignore_failures = params.ignore_groupsusers_create
    )

  if params.has_oozie_server:
    User(params.oozie_user,
         gid = params.user_group,
         ignore_failures = params.ignore_groupsusers_create
    )

  if params.has_hcat_server_host:
    User(params.webhcat_user,
         gid = params.user_group,
         ignore_failures = params.ignore_groupsusers_create
    )
    User(params.hcat_user,
         gid = params.user_group,
         ignore_failures = params.ignore_groupsusers_create
    )

  if params.has_hive_server_host:
    User(params.hive_user,
         gid = params.user_group,
         ignore_failures = params.ignore_groupsusers_create
    )

  if params.has_resourcemanager:
    User(params.yarn_user,
         gid = params.user_group,
         ignore_failures = params.ignore_groupsusers_create
    )

  if params.has_ganglia_server:
    Group(params.gmetad_user,
         ignore_failures = params.ignore_groupsusers_create
    )
    Group(params.gmond_user,
         ignore_failures = params.ignore_groupsusers_create
    )
    User(params.gmond_user,
         gid=params.user_group,
         groups=[params.gmond_user],
         ignore_failures = params.ignore_groupsusers_create
    )
    User(params.gmetad_user,
         gid=params.user_group,
         groups=[params.gmetad_user],
         ignore_failures = params.ignore_groupsusers_create
    )

  User(params.hdfs_user,
        gid=params.user_group,
        groups=[params.user_group],
        ignore_failures = params.ignore_groupsusers_create
  )
  User(params.mapred_user,
       gid=params.user_group,
       groups=[params.user_group],
       ignore_failures = params.ignore_groupsusers_create
  )
  if params.has_zk_host:
    User(params.zk_user,
         gid=params.user_group,
         ignore_failures = params.ignore_groupsusers_create
    )

def set_uid(user, user_dirs):
  """
  user_dirs - comma separated directories
  """
  File("/tmp/changeUid.sh",
       content=StaticFile("changeToSecureUid.sh"),
       mode=0555)
  Execute(format("/tmp/changeUid.sh {user} {user_dirs} 2>/dev/null"),
          not_if = format("test $(id -u {user}) -gt 1000"))

def setup_java():
  """
  Installs jdk using specific params, that comes from ambari-server
  """
  import params

  jdk_curl_target = format("{artifact_dir}/{jdk_name}")
  java_dir = os.path.dirname(params.java_home)
  java_exec = format("{java_home}/bin/java")

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
    install_cmd = format("mkdir -p {java_dir} ; chmod +x {jdk_curl_target}; cd {java_dir} ; echo A | {jdk_curl_target} -noregister > /dev/null 2>&1")
  elif params.jdk_name.endswith(".gz"):
    install_cmd = format("mkdir -p {java_dir} ; cd {java_dir} ; tar -xf {jdk_curl_target} > /dev/null 2>&1")

  Execute(install_cmd,
          path = ["/bin","/usr/bin/"],
          not_if = format("test -e {java_exec}")
  )

def install_packages():
  Package(['unzip'])
