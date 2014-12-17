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
import sys
import os.path
import glob

from resource_management import *
from resource_management.core.resources.system import Execute
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.dynamic_variable_interpretation import copy_tarballs_to_hdfs


def webhcat():
  import params

  if params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, "2.2.0.0") < 0:
    params.HdfsDirectory(params.webhcat_apps_dir,
                         action="create_delayed",
                         owner=params.webhcat_user,
                         mode=0755
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
            mode=0755,
            group=params.user_group,
            recursive=True)

  Directory(params.templeton_log_dir,
            owner=params.webhcat_user,
            mode=0755,
            group=params.user_group,
            recursive=True)

  Directory(params.config_dir,
            recursive=True,
            owner=params.webhcat_user,
            group=params.user_group)

  if params.security_enabled:
    kinit_if_needed = format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name};")
  else:
    kinit_if_needed = ""

  if kinit_if_needed:
    Execute(kinit_if_needed,
            user=params.webhcat_user,
            path='/bin'
    )

  # TODO, these checks that are specific to HDP 2.2 and greater should really be in a script specific to that stack.
  if params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, "2.2.0.0") >= 0:
    copy_tarballs_to_hdfs('hive', params.webhcat_user, params.hdfs_user, params.user_group)
    copy_tarballs_to_hdfs('pig', params.webhcat_user, params.hdfs_user, params.user_group)
    copy_tarballs_to_hdfs('hadoop-streaming', params.webhcat_user, params.hdfs_user, params.user_group)
    copy_tarballs_to_hdfs('sqoop', params.webhcat_user, params.hdfs_user, params.user_group)
  else:
    CopyFromLocal(params.hadoop_streeming_jars,
                  owner=params.webhcat_user,
                  mode=0755,
                  dest_dir=params.webhcat_apps_dir,
                  kinnit_if_needed=kinit_if_needed,
                  hdfs_user=params.hdfs_user,
                  hadoop_bin_dir=params.hadoop_bin_dir,
                  hadoop_conf_dir=params.hadoop_conf_dir
    )

    if (os.path.isfile(params.pig_tar_file)):
      CopyFromLocal(params.pig_tar_file,
                    owner=params.webhcat_user,
                    mode=0755,
                    dest_dir=params.webhcat_apps_dir,
                    kinnit_if_needed=kinit_if_needed,
                    hdfs_user=params.hdfs_user,
                    hadoop_bin_dir=params.hadoop_bin_dir,
                    hadoop_conf_dir=params.hadoop_conf_dir
      )

    CopyFromLocal(params.hive_tar_file,
                  owner=params.webhcat_user,
                  mode=0755,
                  dest_dir=params.webhcat_apps_dir,
                  kinnit_if_needed=kinit_if_needed,
                  hdfs_user=params.hdfs_user,
                  hadoop_bin_dir=params.hadoop_bin_dir,
                  hadoop_conf_dir=params.hadoop_conf_dir
    )

    if (len(glob.glob(params.sqoop_tar_file)) > 0):
      CopyFromLocal(params.sqoop_tar_file,
                    owner=params.webhcat_user,
                    mode=0755,
                    dest_dir=params.webhcat_apps_dir,
                    kinnit_if_needed=kinit_if_needed,
                    hdfs_user=params.hdfs_user,
                    hadoop_bin_dir=params.hadoop_bin_dir,
                    hadoop_conf_dir=params.hadoop_conf_dir
      )

  XmlConfig("webhcat-site.xml",
            conf_dir=params.config_dir,
            configurations=params.config['configurations']['webhcat-site'],
            configuration_attributes=params.config['configuration_attributes']['webhcat-site'],
            owner=params.webhcat_user,
            group=params.user_group,
            )

  File(format("{config_dir}/webhcat-env.sh"),
       owner=params.webhcat_user,
       group=params.user_group,
       content=InlineTemplate(params.webhcat_env_sh_template)
  )
