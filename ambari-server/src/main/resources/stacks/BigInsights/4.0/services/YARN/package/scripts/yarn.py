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


def yarn(name = None):
  import params


  if name in ["nodemanager","historyserver"]:
    if params.yarn_log_aggregation_enabled:
      params.HdfsResource(params.yarn_nm_app_log_dir,
                           action="create_on_execute",
                           type="directory",
                           owner=params.yarn_user,
                           group=params.user_group,
                           mode=0777,
                           recursive_chmod=True
      )
    params.HdfsResource("/mapred",
                         type="directory",
                         action="create_on_execute",
                         owner=params.mapred_user
    )
    params.HdfsResource("/mapred/system",
                         type="directory",
                         action="create_on_execute",
                         owner=params.hdfs_user
    )
    params.HdfsResource(params.mapreduce_jobhistory_intermediate_done_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.mapred_user,
                         group=params.user_group,
                         mode=0777
    )

    params.HdfsResource(params.mapreduce_jobhistory_done_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.mapred_user,
                         group=params.user_group,
                         mode=0777
    )
    params.HdfsResource(None, action="execute")
    Directory(params.jhs_leveldb_state_store_dir,
              owner=params.mapred_user,
              group=params.user_group,
              create_parents=True,
              cd_access="a",
              )

  if name == "nodemanager":
    Directory(params.nm_local_dirs.split(',') + params.nm_log_dirs.split(','),
              owner=params.yarn_user,
              group=params.user_group,
              create_parents=True,
              cd_access="a",
              ignore_failures=True,
              mode=0775
              )

    Execute(('chown', '-R', params.yarn_user, params.nm_local_dirs),
            only_if=format("test -d {nm_local_dirs}"),
            sudo=True)


    if params.security_enabled:
      smokeuser_directories = [os.path.join(dir, 'usercache' ,params.smokeuser)
                               for dir in params.nm_local_dirs.split(',')]
      for directory in smokeuser_directories:
        Execute(('chown', '-R', params.smokeuser, directory),
                only_if=format("test -d {directory}"),
                sudo=True,
        )
  Directory([params.yarn_pid_dir_prefix, params.yarn_pid_dir, params.yarn_log_dir],
            owner=params.yarn_user,
            group=params.user_group,
            create_parents=True,
            cd_access = 'a',
  )

  Directory([params.mapred_pid_dir_prefix, params.mapred_pid_dir, params.mapred_log_dir_prefix, params.mapred_log_dir],
            owner=params.mapred_user,
            group=params.user_group,
            create_parents=True,
            cd_access = 'a',
  )
  Directory([params.yarn_log_dir_prefix],
            owner=params.yarn_user,
            create_parents=True,
            ignore_failures=True,
            cd_access = 'a',
  )

  XmlConfig("core-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['core-site'],
            configuration_attributes=params.config['configuration_attributes']['core-site'],
            owner=params.hdfs_user,
            group=params.user_group,
            mode=0644
  )

  XmlConfig("mapred-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['mapred-site'],
            configuration_attributes=params.config['configuration_attributes']['mapred-site'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0644
  )

  XmlConfig("yarn-site.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['yarn-site'],
            configuration_attributes=params.config['configuration_attributes']['yarn-site'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0644
  )

  XmlConfig("capacity-scheduler.xml",
            conf_dir=params.hadoop_conf_dir,
            configurations=params.config['configurations']['capacity-scheduler'],
            configuration_attributes=params.config['configuration_attributes']['capacity-scheduler'],
            owner=params.yarn_user,
            group=params.user_group,
            mode=0644
  )

  if name == 'resourcemanager':
    File(params.yarn_job_summary_log,
       owner=params.yarn_user,
       group=params.user_group
    )
    if params.node_labels_dir:
      params.HdfsResource(params.node_labels_dir,
                           type="directory",
                           action="create_on_execute",
                           owner=params.yarn_user,
                           group=params.user_group,
                           mode=0700
      )
      params.HdfsResource(None, action="execute")
  elif name == 'apptimelineserver':
    Directory(params.ats_leveldb_dir,
       owner=params.yarn_user,
       group=params.user_group,
       create_parents=True,
       cd_access="a",
    )

  File(params.rm_nodes_exclude_path,
       owner=params.yarn_user,
       group=params.user_group
  )

  File(format("{limits_conf_dir}/yarn.conf"),
       mode=0644,
       content=Template('yarn.conf.j2')
  )

  File(format("{limits_conf_dir}/mapreduce.conf"),
       mode=0644,
       content=Template('mapreduce.conf.j2')
  )

  File(format("{hadoop_conf_dir}/yarn-env.sh"),
       owner=params.yarn_user,
       group=params.user_group,
       mode=0755,
       content=InlineTemplate(params.yarn_env_sh_template)
  )

  container_executor = format("{yarn_container_bin}/container-executor")
  File(container_executor,
       group=params.yarn_executor_container_group,
       mode=06050
  )

  File(format("{hadoop_conf_dir}/container-executor.cfg"),
       group=params.user_group,
       mode=0644,
       content=Template('container-executor.cfg.j2')
  )


  if params.security_enabled:
    tc_mode = 0644
    tc_owner = "root"
  else:
    tc_mode = None
    tc_owner = params.hdfs_user

  File(format("{hadoop_conf_dir}/mapred-env.sh"),
       owner=tc_owner,
       content=InlineTemplate(params.mapred_env_sh_template)
  )

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

  if "mapred-site" in params.config['configurations']:
    XmlConfig("mapred-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['mapred-site'],
              configuration_attributes=params.config['configuration_attributes']['mapred-site'],
              owner=params.mapred_user,
              group=params.user_group
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
