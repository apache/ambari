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
import re
import getpass
from copy import copy
from resource_management.libraries.functions.version import compare_versions
from resource_management import *


def setup_users():
  """
  Creates users before cluster installation
  """
  import params

  if not params.host_sys_prepped and not params.ignore_groupsusers_create:
    for group in params.group_list:
      Group(group,
      )


    for user in params.user_list:
      if params.override_uid == "true":
        User(user,
            uid = get_uid(user),
            gid = params.user_to_gid_dict[user],
            groups = params.user_to_groups_dict[user],
        )
      else:
        User(user,
            gid = params.user_to_gid_dict[user],
            groups = params.user_to_groups_dict[user],
        )

    if params.override_uid == "true":
      set_uid(params.smoke_user, params.smoke_user_dirs)
    else:
      Logger.info('Skipping setting uid for smoke user as host is sys prepped')
  else:
    Logger.info('Skipping creation of User and Group as host is sys prepped or ignore_groupsusers_create flag is on')
    pass


  if params.has_hbase_masters:
    Directory (params.hbase_tmp_dir,
               owner = params.hbase_user,
               mode=0775,
               create_parents = True,
               cd_access="a",
    )
    if not params.host_sys_prepped and params.override_uid == "true":
      set_uid(params.hbase_user, params.hbase_user_dirs)
    else:
      Logger.info('Skipping setting uid for hbase user as host is sys prepped')      
      pass

  if not params.host_sys_prepped:
    if params.has_namenode:
      create_dfs_cluster_admins()
  else:
    Logger.info('Skipping setting dfs cluster admin as host is sys prepped')


def create_dfs_cluster_admins():
  """
  dfs.cluster.administrators support format <comma-delimited list of usernames><space><comma-delimited list of group names>
  """
  import params

  groups_list = create_users_and_groups(params.dfs_cluster_administrators_group)

  User(params.hdfs_user,
    groups = params.user_to_groups_dict[params.hdfs_user] + groups_list,
    ignore_failures = params.ignore_groupsusers_create
  )
def create_users_and_groups(user_and_groups):

  import params

  parts = re.split('\s', user_and_groups)
  if len(parts) == 1:
    parts.append("")

  users_list = parts[0].split(",") if parts[0] else []
  groups_list = parts[1].split(",") if parts[1] else []

  if users_list:
    User(users_list,
         ignore_failures = params.ignore_groupsusers_create
    )

  if groups_list:
    Group(copy(groups_list),
          ignore_failures = params.ignore_groupsusers_create
    )
  return groups_list
    
def set_uid(user, user_dirs):
  """
  user_dirs - comma separated directories
  """
  import params

  File(format("{tmp_dir}/changeUid.sh"),
       content=StaticFile("changeToSecureUid.sh"),
       mode=0555)
  ignore_groupsusers_create_str = str(params.ignore_groupsusers_create).lower()
  uid = get_uid(user)
  Execute(format("{tmp_dir}/changeUid.sh {user} {user_dirs} {uid}"),
          not_if = format("(test $(id -u {user}) -gt 1000) || ({ignore_groupsusers_create_str})"))

def get_uid(user):
  import params
  import commands
  user_str = str(user) + "_uid"
  service_env = [ serviceEnv for serviceEnv in params.config['configurations'] if user_str in params.config['configurations'][serviceEnv]]
  
  if service_env and params.config['configurations'][service_env[0]][user_str]:
    service_env_str = str(service_env[0])
    uid = params.config['configurations'][service_env_str][user_str]
    if len(service_env) > 1:
      Logger.warning("Multiple values found for %s, using %s"  % (user_str, uid))
    return uid 
  else:
    if user == params.smoke_user:
      return 0
    File(format("{tmp_dir}/changeUid.sh"),
       content=StaticFile("changeToSecureUid.sh"),
       mode=0555)
    ignore_groupsusers_create_str = str(params.ignore_groupsusers_create).lower()
    newUid=commands.getoutput(format("{tmp_dir}/changeUid.sh {user}"))
    return int(newUid)
    
def setup_hadoop_env():
  import params
  if params.has_namenode:
    if params.security_enabled:
      tc_owner = "root"
    else:
      tc_owner = params.hdfs_user

    Directory(params.hadoop_dir, mode=0755)

    # IOP < 4.0 used a conf -> conf.empty symlink for /etc/hadoop/
    if Script.is_stack_less_than("4.0"):
      Directory(params.hadoop_conf_empty_dir,
              create_parents = True,
              owner = 'root',
              group = params.user_group
      )
      Link(params.hadoop_conf_dir,
         to=params.hadoop_conf_empty_dir,
         not_if=format("ls {hadoop_conf_dir}")
      )
      
    # write out hadoop-env.sh, but only if the directory exists
    if os.path.exists(params.hadoop_conf_dir):
      File(os.path.join(params.hadoop_conf_dir, 'hadoop-env.sh'),
         owner=tc_owner, group=params.user_group,
         content=InlineTemplate(params.hadoop_env_sh_template)
      )
      
    # Create tmp dir for java.io.tmpdir
    # Handle a situation when /tmp is set to noexec
    Directory(params.hadoop_java_io_tmpdir,
              owner=params.hdfs_user,
              group=params.user_group,
              mode=0777
    )
  
def setup_java():
  """
  Installs jdk using specific params, that comes from ambari-server
  """
  import params

  java_exec = format("{java_home}/bin/java")

  if not os.path.isfile(java_exec):

    jdk_curl_target = format("{tmp_dir}/{jdk_name}")
    java_dir = os.path.dirname(params.java_home)
    tmp_java_dir = format("{tmp_dir}/jdk")

    if not params.jdk_name:
      return

    Directory(params.artifact_dir,
              create_parents = True,
              )

    File(jdk_curl_target,
         content = DownloadSource(format("{jdk_location}/{jdk_name}")),
         not_if = format("test -f {jdk_curl_target}")
    )

    if params.jdk_name.endswith(".bin"):
      chmod_cmd = ("chmod", "+x", jdk_curl_target)
      install_cmd = format("mkdir -p {tmp_java_dir} && cd {tmp_java_dir} && echo A | {jdk_curl_target} -noregister && {sudo} cp -rp {tmp_java_dir}/* {java_dir}")
    elif params.jdk_name.endswith(".gz"):
      chmod_cmd = ("chmod","a+x", java_dir)
      install_cmd = format("mkdir -p {tmp_java_dir} && cd {tmp_java_dir} && tar -xf {jdk_curl_target} && {sudo} cp -rp {tmp_java_dir}/* {java_dir}")

    Directory(java_dir
    )

    Execute(chmod_cmd,
            sudo = True,
            )

    Execute(install_cmd,
            )

    File(format("{java_home}/bin/java"),
         mode=0755,
         cd_access="a",
         )

    Execute(("chgrp","-R", params.user_group, params.java_home),
            sudo = True,
            )
    Execute(("chown","-R", getpass.getuser(), params.java_home),
            sudo = True,
            )
