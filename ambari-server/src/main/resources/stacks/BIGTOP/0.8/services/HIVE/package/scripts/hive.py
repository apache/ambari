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

from resource_management import *
import sys
import os


def hive(name=None):
  import params

  if name == 'hiveserver2':

    params.HdfsDirectory(params.hive_apps_whs_dir,
                         action="create_delayed",
                         owner=params.hive_user,
                         mode=0777
    )
    params.HdfsDirectory(params.hive_hdfs_user_dir,
                         action="create_delayed",
                         owner=params.hive_user,
                         mode=params.hive_hdfs_user_mode
    )
    params.HdfsDirectory(None, action="create")
  
  # We should change configurations for client as well as for server.
  # The reason is that stale-configs are service-level, not component.
  for conf_dir in params.hive_conf_dirs_list:
    fill_conf_dir(conf_dir)

  XmlConfig("hive-site.xml",
            conf_dir=params.hive_config_dir,
            configurations=params.config['configurations']['hive-site'],
            configuration_attributes=params.config['configuration_attributes']['hive-site'],
            owner=params.hive_user,
            group=params.user_group,
            mode=0644)

  File(format("{hive_config_dir}/hive-env.sh"),
       owner=params.hive_user,
       group=params.user_group,
       content=InlineTemplate(params.hive_env_sh_template)
  )

  if name == 'metastore' or name == 'hiveserver2':
    jdbc_connector()
    
  environment = {
    "no_proxy": format("{ambari_server_hostname}")
  }

  cmd = format("/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf -x \"\" "
               "--retry 5 "
               "{jdk_location}{check_db_connection_jar_name} "
               "-o {check_db_connection_jar_name}'")

  Execute(cmd,
          not_if=format("[ -f {check_db_connection_jar_name}]"),
          environment = environment)

  if name == 'metastore':
    File(params.start_metastore_path,
         mode=0755,
         content=StaticFile('startMetastore.sh')
    )
    if params.init_metastore_schema:
      create_schema_cmd = format("export HIVE_CONF_DIR={hive_server_conf_dir} ; "
                                 "{hive_bin}/schematool -initSchema "
                                 "-dbType {hive_metastore_db_type} "
                                 "-userName {hive_metastore_user_name} "
                                 "-passWord {hive_metastore_user_passwd!p}")

      check_schema_created_cmd = format("export HIVE_CONF_DIR={hive_server_conf_dir} ; "
                                        "{hive_bin}/schematool -info "
                                        "-dbType {hive_metastore_db_type} "
                                        "-userName {hive_metastore_user_name} "
                                        "-passWord {hive_metastore_user_passwd!p}")

      Execute(create_schema_cmd,
              not_if = check_schema_created_cmd
      )
  elif name == 'hiveserver2':
    File(params.start_hiveserver2_path,
         mode=0755,
         content=Template(format('{start_hiveserver2_script}'))
    )

  if name != "client":
    crt_directory(params.hive_pid_dir)
    crt_directory(params.hive_log_dir)
    crt_directory(params.hive_var_lib)

def fill_conf_dir(component_conf_dir):
  import params
  
  Directory(component_conf_dir,
            owner=params.hive_user,
            group=params.user_group,
            create_parents = True
  )

  XmlConfig("mapred-site.xml",
            conf_dir=component_conf_dir,
            configurations=params.config['configurations']['mapred-site'],
            configuration_attributes=params.config['configuration_attributes']['mapred-site'],
            owner=params.hive_user,
            group=params.user_group,
            mode=0644)


  crt_file(format("{component_conf_dir}/hive-default.xml.template"))
  crt_file(format("{component_conf_dir}/hive-env.sh.template"))

  log4j_exec_filename = 'hive-exec-log4j.properties'
  if (params.log4j_exec_props != None):
    File(format("{component_conf_dir}/{log4j_exec_filename}"),
         mode=0644,
         group=params.user_group,
         owner=params.hive_user,
         content=params.log4j_exec_props
    )
  elif (os.path.exists("{component_conf_dir}/{log4j_exec_filename}.template")):
    File(format("{component_conf_dir}/{log4j_exec_filename}"),
         mode=0644,
         group=params.user_group,
         owner=params.hive_user,
         content=StaticFile(format("{component_conf_dir}/{log4j_exec_filename}.template"))
    )

  log4j_filename = 'hive-log4j.properties'
  if (params.log4j_props != None):
    File(format("{component_conf_dir}/{log4j_filename}"),
         mode=0644,
         group=params.user_group,
         owner=params.hive_user,
         content=params.log4j_props
    )
  elif (os.path.exists("{component_conf_dir}/{log4j_filename}.template")):
    File(format("{component_conf_dir}/{log4j_filename}"),
         mode=0644,
         group=params.user_group,
         owner=params.hive_user,
         content=StaticFile(format("{component_conf_dir}/{log4j_filename}.template"))
    )


def crt_directory(name):
  import params

  Directory(name,
            create_parents = True,
            owner=params.hive_user,
            group=params.user_group,
            mode=0755)


def crt_file(name):
  import params

  File(name,
       owner=params.hive_user,
       group=params.user_group
  )


def jdbc_connector():
  import params

  if params.hive_jdbc_driver == "com.mysql.jdbc.Driver":
    cmd = format("hive mkdir -p {artifact_dir} ; cp /usr/share/java/{jdbc_jar_name} {target}")

    Execute(cmd,
            not_if=format("test -f {target}"),
            creates=params.target,
            environment= {'PATH' : params.execute_path },
            path=["/bin", "/usr/bin/"])
  elif params.hive_jdbc_driver == "org.postgresql.Driver":
    cmd = format("hive mkdir -p {artifact_dir} ; cp /usr/share/java/{jdbc_jar_name} {target}")

    Execute(cmd,
            not_if=format("test -f {target}"),
            creates=params.target,
            environment= {'PATH' : params.execute_path },
            path=["/bin", "usr/bin/"])

  elif params.hive_jdbc_driver == "oracle.jdbc.driver.OracleDriver":
    environment = {
      "no_proxy": format("{ambari_server_hostname}")
    }

    cmd = format(
      "mkdir -p {artifact_dir} ; "
      "curl -kf -x \"\" --retry 10 {driver_curl_source} -o {driver_curl_target} &&  "
      "cp {driver_curl_target} {target}")

    Execute(cmd,
            not_if=format("test -f {target}"),
            path=["/bin", "/usr/bin/"],
            environment=environment)
