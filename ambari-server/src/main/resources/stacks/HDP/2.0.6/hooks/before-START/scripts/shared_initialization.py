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

def setup_hadoop():
  """
  Setup hadoop files and directories
  """
  import params

  Execute(("setenforce","0"),
          only_if="test -f /selinux/enforce",
          sudo=True,
  )

  if params.current_service == "HDFS":
    install_snappy()

  #directories
  if params.has_namenode:
    Directory(params.hdfs_log_dir_prefix,
              recursive=True,
              owner='root',
              group=params.user_group,
              mode=0775,
              cd_access='a',
    )
    Directory(params.hadoop_pid_dir_prefix,
              recursive=True,
              owner='root',
              group='root',
              cd_access='a',
    )
    Directory(params.hadoop_tmp_dir,
              recursive=True,
              owner=params.hdfs_user,
              cd_access='a',
              )
  #files
    if params.security_enabled:
      tc_owner = "root"
    else:
      tc_owner = params.hdfs_user

    File(os.path.join(params.hadoop_conf_dir, 'commons-logging.properties'),
         owner=tc_owner,
         content=Template('commons-logging.properties.j2')
    )

    health_check_template_name = "health_check"
    File(os.path.join(params.hadoop_conf_dir, health_check_template_name),
         owner=tc_owner,
         content=Template(health_check_template_name + ".j2")
    )

    log4j_filename = os.path.join(params.hadoop_conf_dir, "log4j.properties")
    if (params.log4j_props != None):
      File(log4j_filename,
           mode=0644,
           group=params.user_group,
           owner=params.hdfs_user,
           content=params.log4j_props
      )
    elif (os.path.exists(format("{params.hadoop_conf_dir}/log4j.properties"))):
      File(log4j_filename,
           mode=0644,
           group=params.user_group,
           owner=params.hdfs_user,
      )

    File(os.path.join(params.hadoop_conf_dir, "hadoop-metrics2.properties"),
         owner=params.hdfs_user,
         content=Template("hadoop-metrics2.properties.j2")
    )

def setup_database():
  """
  Load DB
  """
  import params
  db_driver_download_url = None
  
  if params.server_db_name == 'oracle' and params.oracle_driver_url != "":
    db_driver_download_url = params.oracle_driver_symlink_url
  elif params.server_db_name == 'mysql' and params.mysql_driver_url != "":
    db_driver_download_url = params.mysql_driver_symlink_url

  if db_driver_download_url:
    File(format("{hadoop_lib_home}/{db_driver_filename}"),
         content = DownloadSource(db_driver_download_url),
    )


def setup_configs():
  """
  Creates configs for services HDFS mapred
  """
  import params

  if params.has_namenode:
    File(params.task_log4j_properties_location,
         content=StaticFile("task-log4j.properties"),
         mode=0755
    )

    if os.path.exists(os.path.join(params.hadoop_conf_dir, 'configuration.xsl')):
      File(os.path.join(params.hadoop_conf_dir, 'configuration.xsl'),
           owner=params.hdfs_user,
           group=params.user_group
      )
    if os.path.exists(os.path.join(params.hadoop_conf_dir, 'masters')):
      File(os.path.join(params.hadoop_conf_dir, 'masters'),
                owner=params.hdfs_user,
                group=params.user_group
      )

  generate_include_file()


def generate_include_file():
  import params

  if params.has_namenode and params.dfs_hosts and params.has_slaves:
    include_hosts_list = params.slave_hosts
    File(params.dfs_hosts,
         content=Template("include_hosts_list.j2"),
         owner=params.hdfs_user,
         group=params.user_group
    )


def install_snappy():
  import params

  snappy_so = "libsnappy.so"
  so_target_dir_x86 = format("{hadoop_lib_home}/native/Linux-i386-32")
  so_target_dir_x64 = format("{hadoop_lib_home}/native/Linux-amd64-64")
  so_target_x86 = format("{so_target_dir_x86}/{snappy_so}")
  so_target_x64 = format("{so_target_dir_x64}/{snappy_so}")
  so_src_dir_x86 = format("{hadoop_home}/lib")
  so_src_dir_x64 = format("{hadoop_home}/lib64")
  so_src_x86 = format("{so_src_dir_x86}/{snappy_so}")
  so_src_x64 = format("{so_src_dir_x64}/{snappy_so}")
  if params.has_namenode:
    Directory([so_target_dir_x86, so_target_dir_x64],
              recursive=True,
    )    
    Link(so_target_x86,
         to=so_src_x86,
    )
    Link(so_target_x64,
         to=so_src_x64,
    )

def create_javahome_symlink():
  if os.path.exists("/usr/jdk/jdk1.6.0_31") and not os.path.exists("/usr/jdk64/jdk1.6.0_31"):
    Directory("/usr/jdk64/",
         recursive=True,
    )
    Link("/usr/jdk/jdk1.6.0_31",
         to="/usr/jdk64/jdk1.6.0_31",
    )

