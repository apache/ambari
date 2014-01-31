#!/usr/bin/env python2.6
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
  import params

  Directory(params.templeton_pid_dir,
            owner=params.webhcat_user,
            mode=0755,
            group=params.user_group,
            recursive=True)

  Directory(params.templeton_log_dir,
            owner=params.webhcat_user,
            mode=0755,
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

  copyFromLocal(path='/usr/lib/hadoop/contrib/streaming/hadoop-streaming*.jar',
                owner=params.webhcat_user,
                mode=0755,
                dest_dir=format("{webhcat_apps_dir}/hadoop-streaming.jar"),
                kinnit_if_needed=kinit_if_needed
  )

  copyFromLocal(path='/usr/share/HDP-webhcat/pig.tar.gz',
                owner=params.webhcat_user,
                mode=0755,
                dest_dir=format("{webhcat_apps_dir}/pig.tar.gz"),
  )

  copyFromLocal(path='/usr/share/HDP-webhcat/hive.tar.gz',
                owner=params.webhcat_user,
                mode=0755,
                dest_dir=format("{webhcat_apps_dir}/hive.tar.gz")
  )


def copyFromLocal(path=None, owner=None, group=None, mode=None, dest_dir=None, kinnit_if_needed=""):
  import params

  copy_cmd = format("fs -copyFromLocal {path} {dest_dir}")
  unless_cmd = format("{kinnit_if_needed} hadoop fs -ls {dest_dir} >/dev/null 2>&1")

  ExecuteHadoop(copy_cmd,
                not_if=unless_cmd,
                user=owner,
                conf_dir=params.hadoop_conf_dir)

  if not owner:
    chown = None
  else:
    if not group:
      chown = owner
    else:
      chown = format('{owner}:{group}')

  if not chown:
    chown_cmd = format("fs -chown {chown} {dest_dir}")

    ExecuteHadoop(copy_cmd,
                  user=owner,
                  conf_dir=params.hadoop_conf_dir)

  if not mode:
    chmod_cmd = format('fs -chmod {mode} {dest_dir}')

    ExecuteHadoop(chmod_cmd,
                  user=owner,
                  conf_dir=params.hadoop_conf_dir)
