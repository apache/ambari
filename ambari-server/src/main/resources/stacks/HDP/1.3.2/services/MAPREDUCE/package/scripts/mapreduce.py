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
  mapred_userlogs_dir = os.path.join(mapred_log_dir, "userlogs")

  Directory(mapred_log_dir,
            recursive=True,
            owner=params.mapred_user,
            group=params.user_group
  )
  Directory(mapred_userlogs_dir,
            recursive=True,
            mode = 01777
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

  if params.security_enabled:
    tc_mode = 0644
    tc_owner = "root"
  else:
    tc_mode = None
    tc_owner = params.hdfs_user

  if params.security_enabled:
    File(os.path.join(params.hadoop_bin, "task-controller"),
         owner="root",
         group=params.mapred_tt_group,
         mode=06050
    )
    File(os.path.join(params.hadoop_conf_dir, 'taskcontroller.cfg'),
         owner = tc_owner,
         mode = tc_mode,
         group = params.mapred_tt_group,
         content=Template("taskcontroller.cfg.j2")
    )
  else:
    File(os.path.join(params.hadoop_conf_dir, 'taskcontroller.cfg'),
         owner=tc_owner,
         content=Template("taskcontroller.cfg.j2")
    )

  if "capacity-scheduler" in params.config['configurations']:
    XmlConfig("capacity-scheduler.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations'][
                'capacity-scheduler'],
              configuration_attributes=params.config['configuration_attributes']['capacity-scheduler'],
              owner=params.hdfs_user,
              group=params.user_group
    )

  if "mapred-queue-acls" in params.config['configurations']:
    XmlConfig("mapred-queue-acls.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations'][
                'mapred-queue-acls'],
              configuration_attributes=params.config['configuration_attributes']['mapred-queue-acls'],
              owner=params.mapred_user,
              group=params.user_group
    )
  elif os.path.exists(
    os.path.join(params.hadoop_conf_dir, "mapred-queue-acls.xml")):
    File(os.path.join(params.hadoop_conf_dir, "mapred-queue-acls.xml"),
         owner=params.mapred_user,
         group=params.user_group
    )

  if "mapred-site" in params.config['configurations']:
    XmlConfig("mapred-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['mapred-site'],
              configuration_attributes=params.config['configuration_attributes']['mapred-site'],
              owner=params.mapred_user,
              group=params.user_group
    )

  if os.path.exists(os.path.join(params.hadoop_conf_dir, 'fair-scheduler.xml')):
    File(os.path.join(params.hadoop_conf_dir, 'fair-scheduler.xml'),
         owner=params.mapred_user,
         group=params.user_group
    )

  if os.path.exists(
    os.path.join(params.hadoop_conf_dir, 'ssl-client.xml.example')):
    File(os.path.join(params.hadoop_conf_dir, 'ssl-client.xml.example'),
         owner=params.mapred_user,
         group=params.user_group
    )

  if os.path.exists(
    os.path.join(params.hadoop_conf_dir, 'ssl-server.xml.example')):
    File(os.path.join(params.hadoop_conf_dir, 'ssl-server.xml.example'),
         owner=params.mapred_user,
         group=params.user_group
    )
