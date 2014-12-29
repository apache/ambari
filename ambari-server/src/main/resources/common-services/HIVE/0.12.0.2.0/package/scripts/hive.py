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
from install_jars import install_tez_jars
import glob
import sys
import os


def hive(name=None):
  import params

  if name == 'hiveserver2':
    if params.hdp_stack_version_major != "" and compare_versions(params.hdp_stack_version_major, '2.2') >=0:
      params.HdfsResource(InlineTemplate(params.mapreduce_tar_destination).get_content(),
                          type="file",
                          action="create_delayed",
                          source=params.mapreduce_tar_source,
                          group=params.user_group,
                          mode=params.tarballs_mode
      )
      params.HdfsResource(InlineTemplate(params.tez_tar_destination).get_content(),
                          type="file",
                          action="create_delayed",
                          source=params.tez_tar_source,
                          group=params.user_group,
                          mode=params.tarballs_mode
      )
    else:
      install_tez_jars()
        
    if params.hdp_stack_version_major != "" and compare_versions(params.hdp_stack_version_major, "2.2.0.0") < 0:
      params.HdfsResource(params.webhcat_apps_dir,
                           type="directory",
                           action="create_delayed",
                           owner=params.webhcat_user,
                           mode=0755
      )
  
    if params.hcat_hdfs_user_dir != params.webhcat_hdfs_user_dir:
      params.HdfsResource(params.hcat_hdfs_user_dir,
                           type="directory",
                           action="create_delayed",
                           owner=params.hcat_user,
                           mode=params.hcat_hdfs_user_mode
      )
    params.HdfsResource(params.webhcat_hdfs_user_dir,
                         type="directory",
                         action="create_delayed",
                         owner=params.webhcat_user,
                         mode=params.webhcat_hdfs_user_mode
    )
  
    for src_filepath in glob.glob(params.hadoop_streaming_tar_source):
      src_filename = os.path.basename(src_filepath)
      params.HdfsResource(InlineTemplate(params.hadoop_streaming_tar_destination_dir).get_content() + '/' + src_filename,
                          type="file",
                          action="create_delayed",
                          source=src_filepath,
                          group=params.user_group,
                          mode=params.tarballs_mode
      )
  
    if (os.path.isfile(params.pig_tar_source)):
      params.HdfsResource(InlineTemplate(params.pig_tar_destination).get_content(),
                          type="file",
                          action="create_delayed",
                          source=params.pig_tar_source,
                          group=params.user_group,
                          mode=params.tarballs_mode
      )
  
    params.HdfsResource(InlineTemplate(params.hive_tar_destination).get_content(),
                        type="file",
                        action="create_delayed",
                        source=params.hive_tar_source,
                        group=params.user_group,
                        mode=params.tarballs_mode
    )

    for src_filepath in glob.glob(params.sqoop_tar_source):
      src_filename = os.path.basename(src_filepath)
      params.HdfsResource(InlineTemplate(params.sqoop_tar_destination_dir).get_content() + '/' + src_filename,
                          type="file",
                          action="create_delayed",
                          source=src_filepath,
                          group=params.user_group,
                          mode=params.tarballs_mode
      )
      
    params.HdfsResource(params.hive_apps_whs_dir,
                         type="directory",
                         action="create_delayed",
                         owner=params.hive_user,
                         mode=0777
    )
    params.HdfsResource(params.hive_hdfs_user_dir,
                         type="directory",
                         action="create_delayed",
                         owner=params.hive_user,
                         mode=params.hive_hdfs_user_mode
    )
    params.HdfsResource(None, action="execute")

  Directory(params.hive_conf_dir_prefix,
            mode=0755
  )

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
  
  if params.hive_specific_configs_supported and name == 'hiveserver2':
    XmlConfig("hiveserver2-site.xml",
              conf_dir=params.hive_server_conf_dir,
              configurations=params.config['configurations']['hiveserver2-site'],
              configuration_attributes=params.config['configuration_attributes']['hiveserver2-site'],
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
          not_if=format("[ -f {check_db_connection_jar} ]"),
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
            recursive=True
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
            recursive=True,
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

  if params.hive_jdbc_driver in params.hive_jdbc_drivers_list and params.hive_use_existing_db:
    environment = {
      "no_proxy": format("{ambari_server_hostname}")
    }

    # TODO: should be removed after ranger_hive_plugin will not provide jdbc
    Execute(('rm', '-f', params.prepackaged_ojdbc_symlink),
            path=["/bin", "/usr/bin/"],
            sudo = True)

    Execute(('curl', '-kf', '-x', "", '--retry', '10', params.driver_curl_source, '-o',
             params.downloaded_custom_connector),
            not_if=format("test -f {downloaded_custom_connector}"),
            path=["/bin", "/usr/bin/"],
            environment=environment,
            sudo = True)


    Execute(('cp', '--remove-destination', params.downloaded_custom_connector, params.target),
            #creates=params.target, TODO: uncomment after ranger_hive_plugin will not provide jdbc
            path=["/bin", "/usr/bin/"],
            sudo = True)

  else:
    #for default hive db (Mysql)
    Execute(('cp', '--remove-destination', format('/usr/share/java/{jdbc_jar_name}'), params.target),
            #creates=params.target, TODO: uncomment after ranger_hive_plugin will not provide jdbc
            path=["/bin", "/usr/bin/"],
            sudo=True
    )
