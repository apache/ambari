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

def oozie(is_server=False # TODO: see if see can remove this
              ):
  import params

  if is_server:
    params.HdfsDirectory(params.oozie_hdfs_user_dir,
                         action="create",
                         owner=params.oozie_user,
                         mode=params.oozie_hdfs_user_mode
    )
  Directory( params.conf_dir,
             create_parents = True,
             owner = params.oozie_user,
             group = params.user_group
  )
  XmlConfig( "oozie-site.xml",
    conf_dir = params.conf_dir,
    configurations = params.config['configurations']['oozie-site'],
    configuration_attributes=params.config['configuration_attributes']['oozie-site'],
    owner = params.oozie_user,
    group = params.user_group,
    mode = 0664
  )
  File(format("{conf_dir}/oozie-env.sh"),
    owner=params.oozie_user,
    content=InlineTemplate(params.oozie_env_sh_template)
  )

  if params.security_enabled:
    tomcat_conf_dir = format("{tomcat_conf_secure}")
  else:
    tomcat_conf_dir = format("{tomcat_conf}")

  File(format("{tomcat_conf_dir}/catalina.properties"),
    content = Template("catalina.properties.j2"),
    owner = params.oozie_user,
    group = params.user_group,
    mode = 0755
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

  environment = {
    "no_proxy": format("{ambari_server_hostname}")
  }

  if params.jdbc_driver_name == "com.mysql.jdbc.Driver" or \
     params.jdbc_driver_name == "org.postgresql.Driver" or \
     params.jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
    Execute(format("/bin/sh -c 'cd /usr/lib/ambari-agent/ &&\
    curl -kf -x \"\" \
    --retry 5 {jdk_location}{check_db_connection_jar_name}\
     -o {check_db_connection_jar_name}'"),
      not_if  = format("[ -f {check_db_connection_jar} ]"),
      environment=environment
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
  
  oozie_server_directorties = [params.oozie_pid_dir, params.oozie_log_dir, params.oozie_tmp_dir, params.oozie_data_dir, params.oozie_lib_dir, params.oozie_webapps_dir, params.oozie_webapps_conf_dir, params.oozie_server_dir]
  Directory( oozie_server_directorties,
    owner = params.oozie_user,
    mode = 0755,
    create_parents = True
  )

  cmd1 = "sh"

  if params.jdbc_driver_name=="com.mysql.jdbc.Driver" or params.jdbc_driver_name=="oracle.jdbc.driver.OracleDriver":
    cmd1 += format(" && cp {jdbc_driver_jar} {oozie_lib_dir}")

  no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1")
  Execute( [cmd1],
    not_if  = no_op_test
  )

# the version of hadoop-auth jar files in bigtop 0.8 oozie is wrong
def correct_hadoop_auth_jar_files():

  hadoop_auth_jar_file = "/usr/lib/hadoop/hadoop-auth-2.4.1.jar"

  if not os.path.exists(hadoop_auth_jar_file):
    raise Fail("Could not find %s" % (hadoop_auth_jar_file))

  commands = ' '.join(
    (
      "if [ -f /usr/lib/oozie/lib/hadoop-auth-2.0.2-alpha.jar ];",
      "then",
      "rm -rf /usr/lib/oozie/lib/hadoop-auth-2.0.2-alpha.jar;",
      "cp " + hadoop_auth_jar_file + " /usr/lib/oozie/lib;",
      "fi"
    )
  )
  Execute(commands)

  commands = ' '.join(
    (
      "if [ -f /usr/lib/oozie/libtools/hadoop-auth-2.0.2-alpha.jar ];",
      "then",
      "rm -rf /usr/lib/oozie/libtools/hadoop-auth-2.0.2-alpha.jar;",
      "cp " + hadoop_auth_jar_file + " /usr/lib/oozie/libtools;",
      "fi"
    )
  )
  Execute(commands)

