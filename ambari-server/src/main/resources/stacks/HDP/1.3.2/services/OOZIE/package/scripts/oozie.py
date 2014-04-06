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

"""

import os
from resource_management import *

def oozie(is_server=False
              ):
  import params

  if is_server:
    params.HdfsDirectory(params.oozie_hdfs_user_dir,
                         action="create",
                         owner=params.oozie_user,
                         mode=params.oozie_hdfs_user_mode
    )
  XmlConfig( "oozie-site.xml",
    conf_dir = params.conf_dir, 
    configurations = params.config['configurations']['oozie-site'],
    owner = params.oozie_user,
    group = params.user_group,
    mode = 0664
  )
  
  Directory( params.conf_dir,
    owner = params.oozie_user,
    group = params.user_group
  )
  
  TemplateConfig( format("{conf_dir}/oozie-env.sh"),
    owner = params.oozie_user
  )

  if (params.log4j_props != None):
    File(format("{params.conf_dir}/oozie-log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.oozie_user,
         content=params.log4j_props
    )
  elif (os.path.exists(format("{params.conf_dir}/oozie-log4j.properties"))):
    File(format("{params.conf_dir}/oozie-log4j.properties"),
         mode=0644,
         group=params.user_group,
         owner=params.oozie_user
    )

  if params.jdbc_driver_name == "com.mysql.jdbc.Driver" or \
     params.jdbc_driver_name == "org.postgresql.Driver" or \
     params.jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
    Execute(format("/bin/sh -c 'cd /usr/lib/ambari-agent/ &&\
    curl -kf --retry 5 {jdk_location}{check_db_connection_jar_name}\
     -o {check_db_connection_jar_name}'"),
      not_if  = format("[ -f {check_db_connection_jar} ]")
    )
    
  oozie_ownership( )
  
  if is_server:      
    oozie_server_specific( )
  
def oozie_ownership(
):
  import params
  
  File ( format("{conf_dir}/adminusers.txt"),
    owner = params.oozie_user,
    group = params.user_group
  )

  File ( format("{conf_dir}/hadoop-config.xml"),
    owner = params.oozie_user,
    group = params.user_group
  )

  File ( format("{conf_dir}/oozie-default.xml"),
    owner = params.oozie_user,
    group = params.user_group
  )

  Directory ( format("{conf_dir}/action-conf"),
    owner = params.oozie_user,
    group = params.user_group
  )

  File ( format("{conf_dir}/action-conf/hive.xml"),
    owner = params.oozie_user,
    group = params.user_group
  )
  
def oozie_server_specific(
):
  import params

  File(params.pid_file,
    action="delete",
    not_if="ls {pid_file} >/dev/null 2>&1 && !(ps `cat {pid_file}` >/dev/null 2>&1)"
  )
  
  oozie_server_directorties = [params.oozie_pid_dir, params.oozie_log_dir, params.oozie_tmp_dir, params.oozie_data_dir, params.oozie_lib_dir, params.oozie_webapps_dir]            
  Directory( oozie_server_directorties,
    owner = params.oozie_user,
    mode = 0755,
    recursive = True
  )
       
  cmd1 = "cd /usr/lib/oozie && tar -xvf oozie-sharelib.tar.gz"
  cmd2 =  format("cd /usr/lib/oozie && mkdir -p {oozie_tmp_dir}")
  
  # this is different for HDP2
  cmd3 = format("cd /usr/lib/oozie && chown {oozie_user}:{user_group} {oozie_tmp_dir}")
  if params.jdbc_driver_name=="com.mysql.jdbc.Driver" or params.jdbc_driver_name=="oracle.jdbc.driver.OracleDriver":
    cmd3 += format(" && mkdir -p {oozie_libext_dir} && cp {jdbc_driver_jar} {oozie_libext_dir}")
    
  # this is different for HDP2
  cmd4 = format("cd {oozie_tmp_dir} && /usr/lib/oozie/bin/oozie-setup.sh -hadoop 0.20.200 {hadoop_jar_location} -extjs {ext_js_path} {jar_option} {jar_path}")
  
  no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps `cat {pid_file}` >/dev/null 2>&1")
  Execute( [cmd1, cmd2, cmd3],
    not_if  = no_op_test
  )
  Execute( cmd4,
    user = params.oozie_user,
    not_if  = no_op_test
  )
  
