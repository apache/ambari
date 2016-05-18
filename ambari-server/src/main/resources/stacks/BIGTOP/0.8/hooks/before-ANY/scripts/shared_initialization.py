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
    set_uid(params.hbase_user, params.hbase_user_dirs)
    
def set_uid(user, user_dirs):
  """
  user_dirs - comma separated directories
  """
  import params

  File(format("{tmp_dir}/changeUid.sh"),
       content=StaticFile("changeToSecureUid.sh"),
       mode=0555)
  Execute(format("{tmp_dir}/changeUid.sh {user} {user_dirs} 2>/dev/null"),
          not_if = format("test $(id -u {user}) -gt 1000"))
    
def setup_hadoop_env():
  import params
  if params.has_namenode:
    if params.security_enabled:
      tc_owner = "root"
    else:
      tc_owner = params.hdfs_user
    Directory(params.hadoop_conf_empty_dir,
              create_parents = True,
              owner='root',
              group='root'
    )
    Link(params.hadoop_conf_dir,
         to=params.hadoop_conf_empty_dir,
         not_if=format("ls {hadoop_conf_dir}")
    )
    File(os.path.join(params.hadoop_conf_dir, 'hadoop-env.sh'),
         owner=tc_owner,
         content=InlineTemplate(params.hadoop_env_sh_template)
    )
