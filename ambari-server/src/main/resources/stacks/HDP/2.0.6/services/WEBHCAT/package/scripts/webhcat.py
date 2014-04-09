#!/usr/bin/env python
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
from resource_management import *
import sys


def webhcat():
  from . import params

  params.HdfsDirectory(params.webhcat_apps_dir,
                       action="create_delayed",
                       owner=params.webhcat_user,
                       mode=0o755
  )
  if params.hcat_hdfs_user_dir != params.webhcat_hdfs_user_dir:
    params.HdfsDirectory(params.hcat_hdfs_user_dir,
                         action="create_delayed",
                         owner=params.hcat_user,
                         mode=params.hcat_hdfs_user_mode
    )
  params.HdfsDirectory(params.webhcat_hdfs_user_dir,
                       action="create_delayed",
                       owner=params.webhcat_user,
                       mode=params.webhcat_hdfs_user_mode
  )
  params.HdfsDirectory(None, action="create")

  Directory(params.templeton_pid_dir,
            owner=params.webhcat_user,
            mode=0o755,
            group=params.user_group,
            recursive=True)

  Directory(params.templeton_log_dir,
            owner=params.webhcat_user,
            mode=0o755,
            group=params.user_group,
            recursive=True)

  Directory(params.config_dir,
            owner=params.webhcat_user,
            group=params.user_group)

  XmlConfig("webhcat-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['webhcat-site'],
            owner=params.webhcat_user,
            group=params.user_group,
  )

  File(format("{config_dir}/webhcat-env.sh"),
       owner=params.webhcat_user,
       group=params.user_group,
       content=Template('webhcat-env.sh.j2')
  )

  if params.security_enabled:
    kinit_if_needed = format("{kinit_path_local} -kt {smoke_user_keytab} {smokeuser};")
  else:
    kinit_if_needed = ""

  if kinit_if_needed:
    Execute(kinit_if_needed,
            user=params.webhcat_user,
            path='/bin'
    )

  CopyFromLocal('/usr/lib/hadoop-mapreduce/hadoop-streaming-*.jar',
                owner=params.webhcat_user,
                mode=0o755,
                dest_dir=params.webhcat_apps_dir,
                kinnit_if_needed=kinit_if_needed,
                hdfs_user=params.hdfs_user
  )

  CopyFromLocal('/usr/share/HDP-webhcat/pig.tar.gz',
                owner=params.webhcat_user,
                mode=0o755,
                dest_dir=params.webhcat_apps_dir,
                kinnit_if_needed=kinit_if_needed,
                hdfs_user=params.hdfs_user
  )

  CopyFromLocal('/usr/share/HDP-webhcat/hive.tar.gz',
                owner=params.webhcat_user,
                mode=0o755,
                dest_dir=params.webhcat_apps_dir,
                kinnit_if_needed=kinit_if_needed,
                hdfs_user=params.hdfs_user
  )
