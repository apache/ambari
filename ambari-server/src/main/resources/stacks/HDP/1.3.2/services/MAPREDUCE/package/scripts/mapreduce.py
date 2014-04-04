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
import os


def mapreduce(name=None):
  import params


  if name in ["jobtracker","historyserver"]:
    params.HdfsDirectory("/mapred",
                         action="create_delayed",
                         owner=params.mapred_user
    )
    params.HdfsDirectory("/mapred/system",
                         action="create_delayed",
                         owner=params.mapred_user
    )
    params.HdfsDirectory("/mapred/history",
                         action="create_delayed",
                         owner=params.mapred_user
    )
    params.HdfsDirectory(params.mapreduce_jobhistory_intermediate_done_dir,
                         action="create_delayed",
                         owner=params.mapred_user,
                         group=params.user_group,
                         mode=0777
    )
    params.HdfsDirectory(params.mapreduce_jobhistory_done_dir,
                         action="create_delayed",
                         owner=params.mapred_user,
                         group=params.user_group,
                         mode=0777
    )
    params.HdfsDirectory(None, action="create")

  Directory(params.mapred_pid_dir,
            owner=params.mapred_user,
            group=params.user_group,
            recursive=True
  )

  mapred_log_dir = os.path.join(params.mapred_log_dir_prefix, params.mapred_user)
  Directory(mapred_log_dir,
            recursive=True,
            owner=params.mapred_user,
            group=params.user_group
  )

  if name == 'jobtracker':
    File(os.path.join(mapred_log_dir, 'hadoop-mapreduce.jobsummary.log'),
         owner=params.mapred_user,
         group=params.user_group,
         mode=0664
    )

  Directory(params.mapred_local_dir.split(','),
            owner=params.mapred_user,
            mode=0755,
            recursive=True,
            ignore_failures=True
  )

  File(params.exclude_file_path,
            owner=params.mapred_user,
            group=params.user_group,
  )

  File(params.mapred_hosts_file_path,
            owner=params.mapred_user,
            group=params.user_group,
  )
