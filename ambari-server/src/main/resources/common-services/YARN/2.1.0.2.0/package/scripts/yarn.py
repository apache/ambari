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
import os
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def yarn(name = None):
  import params
  XmlConfig("mapred-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['mapred-site'],
            owner=params.yarn_user,
            mode='f'
  )
  XmlConfig("yarn-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['yarn-site'],
            owner=params.yarn_user,
            mode='f',
            configuration_attributes=params.config['configuration_attributes']['yarn-site']
  )
  XmlConfig("capacity-scheduler.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['capacity-scheduler'],
            owner=params.yarn_user,
            mode='f'
  )

  if params.service_map.has_key(name):
    service_name = params.service_map[name]

    ServiceConfig(service_name,
                  action="change_user",
                  username = params.yarn_user,
                  password = Script.get_password(params.yarn_user))


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def yarn(name = None):
  import params
  if name == "historyserver":
    if params.yarn_log_aggregation_enabled:
      params.HdfsResource(params.yarn_nm_app_log_dir,
                           action="create_on_execute",
                           type="directory",
                           owner=params.yarn_user,
                           group=params.user_group,
                           mode=0777,
                           recursive_chmod=True
      )

    # create the /tmp folder with proper permissions if it doesn't exist yet
    if params.entity_file_history_directory.startswith('/tmp'):
        params.HdfsResource('/tmp',
                            action="create_on_execute",
                            type="directory",
                            owner=params.hdfs_user,
                            mode=0777,
        )

    params.HdfsResource(params.entity_file_history_directory,
                           action="create_on_execute",
                           type="directory",
                           owner=params.yarn_user,
                           group=params.user_group
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
    params.HdfsResource(params.mapreduce_jobhistory_done_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.mapred_user,
                         group=params.user_group,
                         change_permissions_for_parents=True,
                         mode=0777
    )
    params.HdfsResource(None, action="execute")
    Directory(params.jhs_leveldb_state_store_dir,
              owner=params.mapred_user,
              group=params.user_group,
              recursive=True,
              cd_access="a",
              )
    Execute(("chown", "-R", format("{mapred_user}:{user_group}"), params.jhs_leveldb_state_store_dir),
            sudo = True,
    )

  if name == "nodemanager":

    # First start after enabling/disabling security
    if params.toggle_nm_security:
      Directory(params.nm_local_dirs_list + params.nm_log_dirs_list,
                action='delete'
      )

      # If yarn.nodemanager.recovery.dir exists, remove this dir
      if params.yarn_nodemanager_recovery_dir:
        Directory(InlineTemplate(params.yarn_nodemanager_recovery_dir).get_content(),
                  action='delete'
        )

      # Setting NM marker file
      if params.security_enabled:
        Directory(params.nm_security_marker_dir)
        File(params.nm_security_marker,
             content="Marker file to track first start after enabling/disabling security. "
                     "During first start yarn local, log dirs are removed and recreated"
             )
      elif not params.security_enabled:
        File(params.nm_security_marker, action="delete")


    if not params.security_enabled or params.toggle_nm_security:
      Directory(params.nm_local_dirs_list + params.nm_log_dirs_list,
                owner=params.yarn_user,
                group=params.user_group,
                recursive=True,
                cd_access="a",
                ignore_failures=True,
                mode=0775
                )
      Execute(("chmod", "-R", "755") + tuple(params.nm_local_dirs_list),
                sudo=True,
      )

  if params.yarn_nodemanager_recovery_dir:
    Directory(InlineTemplate(params.yarn_nodemanager_recovery_dir).get_content(),
              owner=params.yarn_user,
              group=params.user_group,
              recursive=True,
              mode=0755,
              cd_access = 'a',
    )

  Directory([params.yarn_pid_dir_prefix, params.yarn_pid_dir, params.yarn_log_dir],
            owner=params.yarn_user,
            group=params.user_group,
            recursive=True,
            cd_access = 'a',
  )

  Directory([params.mapred_pid_dir_prefix, params.mapred_pid_dir, params.mapred_log_dir_prefix, params.mapred_log_dir],
            owner=params.mapred_user,
            group=params.user_group,
            recursive=True,
            cd_access = 'a',
  )
  Directory([params.yarn_log_dir_prefix],
            owner=params.yarn_user,
            recursive=True,
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

  # During RU, Core Masters and Slaves need hdfs-site.xml
  # TODO, instead of specifying individual configs, which is susceptible to breaking when new configs are added,
  # RU should rely on all available in /usr/hdp/<version>/hadoop/conf
  if 'hdfs-site' in params.config['configurations']:
    XmlConfig("hdfs-site.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['hdfs-site'],
              configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
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
    if not is_empty(params.node_label_enable) and params.node_label_enable or is_empty(params.node_label_enable) and params.node_labels_dir:
      params.HdfsResource(params.node_labels_dir,
                           type="directory",
                           action="create_on_execute",
                           change_permissions_for_parents=True,
                           owner=params.yarn_user,
                           group=params.user_group,
                           mode=0700
      )
      params.HdfsResource(None, action="execute")


  elif name == 'apptimelineserver':
    Directory(params.ats_leveldb_dir,
       owner=params.yarn_user,
       group=params.user_group,
       recursive=True,
       cd_access="a",
    )

    # if HDP stack is greater than/equal to 2.2, mkdir for state store property (added in 2.2)
    if (Script.is_hdp_stack_greater_or_equal("2.2")):
      Directory(params.ats_leveldb_state_store_dir,
       owner=params.yarn_user,
       group=params.user_group,
       recursive=True,
       cd_access="a",
      )
    # app timeline server 1.5 directories
    if not is_empty(params.entity_groupfs_store_dir):
      parent_path = os.path.dirname(params.entity_groupfs_store_dir)
      params.HdfsResource(parent_path,
                          type="directory",
                          action="create_on_execute",
                          change_permissions_for_parents=True,
                          owner=params.yarn_user,
                          group=params.user_group,
                          mode=0755
                          )
      params.HdfsResource(params.entity_groupfs_store_dir,
                          type="directory",
                          action="create_on_execute",
                          owner=params.yarn_user,
                          group=params.user_group,
                          mode=params.entity_groupfs_store_dir_mode
                          )
    if not is_empty(params.entity_groupfs_active_dir):
      parent_path = os.path.dirname(params.entity_groupfs_active_dir)
      params.HdfsResource(parent_path,
                          type="directory",
                          action="create_on_execute",
                          change_permissions_for_parents=True,
                          owner=params.yarn_user,
                          group=params.user_group,
                          mode=0755
                          )
      params.HdfsResource(params.entity_groupfs_active_dir,
                          type="directory",
                          action="create_on_execute",
                          owner=params.yarn_user,
                          group=params.user_group,
                          mode=params.entity_groupfs_active_dir_mode
                          )
    params.HdfsResource(None, action="execute")

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
      mode=params.container_executor_mode
  )

  File(format("{hadoop_conf_dir}/container-executor.cfg"),
      group=params.user_group,
      mode=0644,
      content=Template('container-executor.cfg.j2')
  )

  Directory(params.cgroups_dir,
            group=params.user_group,
            recursive=True,
            mode=0755,
            cd_access="a")

  if params.security_enabled:
    tc_mode = 0644
    tc_owner = "root"
  else:
    tc_mode = None
    tc_owner = params.hdfs_user

  File(format("{hadoop_conf_dir}/mapred-env.sh"),
       owner=tc_owner,
       mode=0755,
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
  if "ssl-client" in params.config['configurations']:
    XmlConfig("ssl-client.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['ssl-client'],
              configuration_attributes=params.config['configuration_attributes']['ssl-client'],
              owner=params.hdfs_user,
              group=params.user_group
    )

    Directory(params.hadoop_conf_secure_dir,
              recursive=True,
              owner='root',
              group=params.user_group,
              cd_access='a',
              )

    XmlConfig("ssl-client.xml",
              conf_dir=params.hadoop_conf_secure_dir,
              configurations=params.config['configurations']['ssl-client'],
              configuration_attributes=params.config['configuration_attributes']['ssl-client'],
              owner=params.hdfs_user,
              group=params.user_group
    )

  if "ssl-server" in params.config['configurations']:
    XmlConfig("ssl-server.xml",
              conf_dir=params.hadoop_conf_dir,
              configurations=params.config['configurations']['ssl-server'],
              configuration_attributes=params.config['configuration_attributes']['ssl-server'],
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
