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
from copy import copy

from resource_management import *



def setup_jce():
  import params
  
  if not params.jdk_name:
    return
  
  if params.jce_policy_zip is not None:
    jce_curl_target = format("{artifact_dir}/{jce_policy_zip}")
    Directory(params.artifact_dir,
         recursive = True,
    )
    File(jce_curl_target,
         content = DownloadSource(format("{jce_location}/{jce_policy_zip}")),
    )
  elif params.security_enabled:
    # Something weird is happening
    raise Fail("Security is enabled, but JCE policy zip is not specified.")
  
  if params.security_enabled:
    security_dir = format("{java_home}/jre/lib/security")
    
    File([format("{security_dir}/US_export_policy.jar"), format("{security_dir}/local_policy.jar")],
         action = "delete",
    )
    
    extract_cmd = ("unzip", "-o", "-j", "-q", jce_curl_target, "-d", security_dir) 
    Execute(extract_cmd,
            only_if = format("test -e {security_dir} && test -f {jce_curl_target}"),
            path = ['/bin/','/usr/bin'],
            sudo = True
    )

def setup_users():
  """
  Creates users before cluster installation
  """
  import params
  
  for group in params.group_list:
    Group(group,
        ignore_failures = params.ignore_groupsusers_create
    )
    
  for user in params.user_list:
    User(user,
        gid = params.user_to_gid_dict[user],
        groups = params.user_to_groups_dict[user],
        ignore_failures = params.ignore_groupsusers_create       
    )
           
  set_uid(params.smoke_user, params.smoke_user_dirs)

  if params.has_hbase_masters:
    Directory (params.hbase_tmp_dir,
               owner = params.hbase_user,
               mode=0775,
               recursive = True,
               cd_access="a",
    )
    set_uid(params.hbase_user, params.hbase_user_dirs)

  if params.has_namenode:
    create_dfs_cluster_admins()

def create_dfs_cluster_admins():
  """
  dfs.cluster.administrators support format <comma-delimited list of usernames><space><comma-delimited list of group names>
  """
  import params

  parts = re.split('\s', params.dfs_cluster_administrators_group)
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

  User(params.hdfs_user,
    groups = params.user_to_groups_dict[params.hdfs_user] + groups_list,
    ignore_failures = params.ignore_groupsusers_create
  )



    
def set_uid(user, user_dirs):
  """
  user_dirs - comma separated directories
  """
  import params

  File(format("{tmp_dir}/changeUid.sh"),
       content=StaticFile("changeToSecureUid.sh"),
       mode=0555)
  ignore_groupsusers_create_str = str(params.ignore_groupsusers_create).lower()
  Execute(format("{tmp_dir}/changeUid.sh {user} {user_dirs}"),
          not_if = format("(test $(id -u {user}) -gt 1000) || ({ignore_groupsusers_create_str})"))
    
def setup_hadoop_env():
  import params
  stackversion = params.stack_version_unformatted
  if params.has_namenode or stackversion.find('Gluster') >= 0:
    if params.security_enabled:
      tc_owner = "root"
    else:
      tc_owner = params.hdfs_user

    Directory(params.hadoop_dir,
              mode=0755
    )
    if stackversion.find('Gluster') >= 0:
        Directory(params.hadoop_conf_empty_dir,
              recursive=True,
              owner="root",
              group=params.user_group
        )
    else:
        Directory(params.hadoop_conf_empty_dir,
              recursive=True,
              owner=tc_owner,
              group=params.user_group
        )
    Link(params.hadoop_conf_dir,
         to=params.hadoop_conf_empty_dir,
         not_if=format("ls {hadoop_conf_dir}")
    )
    File(os.path.join(params.hadoop_conf_dir, 'hadoop-env.sh'),
         owner=tc_owner,
         group=params.user_group,
         content=InlineTemplate(params.hadoop_env_sh_template)
    )
