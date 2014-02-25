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

  Group(params.user_group)
  Group(params.smoke_user_group)
  Group(params.proxyuser_group)
  User(params.smoke_user,
       gid=params.user_group,
       groups=[params.proxyuser_group]
  )
  User(params.tez_user,
      gid=params.user_group,
      groups=[params.proxyuser_group]
  )
  smoke_user_dirs = format(
    "/tmp/hadoop-{smoke_user},/tmp/hsperfdata_{smoke_user},/home/{smoke_user},/tmp/{smoke_user},/tmp/sqoop-{smoke_user}")
  set_uid(params.smoke_user, smoke_user_dirs)

  if params.has_hbase_masters:
    User(params.hbase_user,
         gid = params.user_group,
         groups=[params.user_group])
    hbase_user_dirs = format(
      "/home/{hbase_user},/tmp/{hbase_user},/usr/bin/{hbase_user},/var/log/{hbase_user},{hbase_tmp_dir}")
    set_uid(params.hbase_user, hbase_user_dirs)

  if params.has_nagios:
    Group(params.nagios_group)
    User(params.nagios_user,
         gid=params.nagios_group)

  if params.has_oozie_server:
    User(params.oozie_user,
         gid = params.user_group)

  if params.has_hcat_server_host:
    User(params.webhcat_user,
         gid = params.user_group)
    User(params.hcat_user,
         gid = params.user_group)

  if params.has_hive_server_host:
    User(params.hive_user,
         gid = params.user_group)

  if params.has_resourcemanager:
    User(params.yarn_user,
         gid = params.user_group)

  if params.has_ganglia_server:
    Group(params.gmetad_user)
    Group(params.gmond_user)
    User(params.gmond_user,
         gid=params.user_group,
        groups=[params.gmond_user])
    User(params.gmetad_user,
         gid=params.user_group,
        groups=[params.gmetad_user])

  User(params.hdfs_user,
        gid=params.user_group,
        groups=[params.user_group]
  )
  User(params.mapred_user,
       gid=params.user_group,
       groups=[params.user_group]
  )
  if params.has_zk_host:
    User(params.zk_user,
         gid=params.user_group)

  if params.has_storm_server:
    User(params.storm_user,
         gid=params.user_group,
         groups=[params.user_group]
    )

  if params.has_falcon_server:
    User(params.falcon_user,
         gid=params.user_group,
         groups=[params.user_group]
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

def install_packages():
  Package("unzip")
  Package("net-snmp")
  
  if System.get_instance().os_family != "suse":
    Package("net-snmp-utils")
