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
from resource_management.libraries import functions
import sys
import os
import glob
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from urlparse import urlparse


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hive(name=None):
  import params

  XmlConfig("hive-site.xml",
            conf_dir = params.hive_conf_dir,
            configurations = params.config['configurations']['hive-site'],
            owner=params.hive_user,
            configuration_attributes=params.config['configuration_attributes']['hive-site']
  )

  if name in ["hiveserver2","metastore"]:
    # Manually overriding service logon user & password set by the installation package
    service_name = params.service_map[name]
    ServiceConfig(service_name,
                  action="change_user",
                  username = params.hive_user,
                  password = Script.get_password(params.hive_user))
    Execute(format("cmd /c hadoop fs -mkdir -p {hive_warehouse_dir}"), logoutput=True, user=params.hadoop_user)

  if name == 'metastore':
    if params.init_metastore_schema:
      check_schema_created_cmd = format('cmd /c "{hive_bin}\\hive.cmd --service schematool -info '
                                        '-dbType {hive_metastore_db_type} '
                                        '-userName {hive_metastore_user_name} '
                                        '-passWord {hive_metastore_user_passwd!p}'
                                        '&set EXITCODE=%ERRORLEVEL%&exit /B %EXITCODE%"', #cmd "feature", propagate the process exit code manually
                                        hive_bin=params.hive_bin,
                                        hive_metastore_db_type=params.hive_metastore_db_type,
                                        hive_metastore_user_name=params.hive_metastore_user_name,
                                        hive_metastore_user_passwd=params.hive_metastore_user_passwd)
      try:
        Execute(check_schema_created_cmd)
      except Fail:
        create_schema_cmd = format('cmd /c {hive_bin}\\hive.cmd --service schematool -initSchema '
                                   '-dbType {hive_metastore_db_type} '
                                   '-userName {hive_metastore_user_name} '
                                   '-passWord {hive_metastore_user_passwd!p}',
                                   hive_bin=params.hive_bin,
                                   hive_metastore_db_type=params.hive_metastore_db_type,
                                   hive_metastore_user_name=params.hive_metastore_user_name,
                                   hive_metastore_user_passwd=params.hive_metastore_user_passwd)
        Execute(create_schema_cmd,
                user = params.hive_user,
                logoutput=True
        )


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hive(name=None):
  import params

  if name == 'hiveserver2':

    if params.hdp_stack_version_major != "" and compare_versions(params.hdp_stack_version_major, '2.2') >=0:
      params.HdfsResource(InlineTemplate(params.mapreduce_tar_destination).get_content(),
                          type="file",
                          action="create_on_execute",
                          source=params.mapreduce_tar_source,
                          group=params.user_group,
                          mode=params.tarballs_mode
      )
        
    if params.hdp_stack_version_major != "" and compare_versions(params.hdp_stack_version_major, "2.2.0.0") < 0:
      params.HdfsResource(params.webhcat_apps_dir,
                           type="directory",
                           action="create_on_execute",
                           owner=params.webhcat_user,
                           mode=0755
      )
  
    if params.hcat_hdfs_user_dir != params.webhcat_hdfs_user_dir:
      params.HdfsResource(params.hcat_hdfs_user_dir,
                           type="directory",
                           action="create_on_execute",
                           owner=params.hcat_user,
                           mode=params.hcat_hdfs_user_mode
      )
    params.HdfsResource(params.webhcat_hdfs_user_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.webhcat_user,
                         mode=params.webhcat_hdfs_user_mode
    )
  
    for src_filepath in glob.glob(params.hadoop_streaming_tar_source):
      src_filename = os.path.basename(src_filepath)
      params.HdfsResource(InlineTemplate(params.hadoop_streaming_tar_destination_dir).get_content() + '/' + src_filename,
                          type="file",
                          action="create_on_execute",
                          source=src_filepath,
                          group=params.user_group,
                          mode=params.tarballs_mode
      )
  
    if (os.path.isfile(params.pig_tar_source)):
      params.HdfsResource(InlineTemplate(params.pig_tar_destination).get_content(),
                          type="file",
                          action="create_on_execute",
                          source=params.pig_tar_source,
                          group=params.user_group,
                          mode=params.tarballs_mode
      )
  
    params.HdfsResource(InlineTemplate(params.hive_tar_destination).get_content(),
                        type="file",
                        action="create_on_execute",
                        source=params.hive_tar_source,
                        group=params.user_group,
                        mode=params.tarballs_mode
    )
 
    for src_filepath in glob.glob(params.sqoop_tar_source):
      src_filename = os.path.basename(src_filepath)
      params.HdfsResource(InlineTemplate(params.sqoop_tar_destination_dir).get_content() + '/' + src_filename,
                          type="file",
                          action="create_on_execute",
                          source=src_filepath,
                          group=params.user_group,
                          mode=params.tarballs_mode
      )
      
    params.HdfsResource(params.hive_apps_whs_dir,
                         type="directory",
                          action="create_on_execute",
                          owner=params.hive_user,
                          mode=0777
    )
    params.HdfsResource(params.hive_hdfs_user_dir,
                         type="directory",
                          action="create_on_execute",
                          owner=params.hive_user,
                          mode=params.hive_hdfs_user_mode
    )
    
    if not is_empty(params.hive_exec_scratchdir) and not urlparse(params.hive_exec_scratchdir).path.startswith("/tmp"):
      params.HdfsResource(params.hive_exec_scratchdir,
                           type="directory",
                           action="create_on_execute",
                           owner=params.hive_user,
                           group=params.hdfs_user,
                           mode=0777) # Hive expects this dir to be writeable by everyone as it is used as a temp dir
      
    params.HdfsResource(None, action="execute")

  Directory(params.hive_etc_dir_prefix,
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

  # On some OS this folder could be not exists, so we will create it before pushing there files
  Directory(params.limits_conf_dir,
            recursive=True,
            owner='root',
            group='root'
            )

  File(os.path.join(params.limits_conf_dir, 'hive.conf'),
       owner='root',
       group='root',
       mode=0644,
       content=Template("hive.conf.j2")
       )

  if name == 'metastore' or name == 'hiveserver2':
    jdbc_connector()

  File(format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
       content = DownloadSource(format("{jdk_location}{check_db_connection_jar_name}")),
  )

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

      check_schema_created_cmd = as_user(format("export HIVE_CONF_DIR={hive_server_conf_dir} ; "
                                        "{hive_bin}/schematool -info "
                                        "-dbType {hive_metastore_db_type} "
                                        "-userName {hive_metastore_user_name} "
                                        "-passWord {hive_metastore_user_passwd!p}"), params.hive_user)

      Execute(create_schema_cmd,
              not_if = check_schema_created_cmd,
              user = params.hive_user
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
            cd_access='a',
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
    
    File(params.downloaded_custom_connector,
         content = DownloadSource(params.driver_curl_source),
    )

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
    
  File(params.target,
       mode = 0644,
  )
