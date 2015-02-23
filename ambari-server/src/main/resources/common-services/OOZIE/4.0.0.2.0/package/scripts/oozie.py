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

from resource_management.core.resources import Directory
from resource_management.core.resources import File
from resource_management.core.resources.system import Execute
from resource_management.core.source import DownloadSource
from resource_management.core.source import InlineTemplate
from resource_management.core.source import Template
from resource_management.libraries.functions import format
from resource_management.libraries.functions import compare_versions
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.core.resources.packaging import Package


# TODO: see if see can remove this
def oozie(is_server=False):
  import params

  if is_server:
    params.HdfsDirectory(params.oozie_hdfs_user_dir,
                         action="create",
                         owner=params.oozie_user,
                         mode=params.oozie_hdfs_user_mode
    )
  Directory(params.conf_dir,
             recursive = True,
             owner = params.oozie_user,
             group = params.user_group
  )
  XmlConfig("oozie-site.xml",
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

  if params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, '2.2') >= 0:
    File(format("{params.conf_dir}/adminusers.txt"),
      mode=0644,
      group=params.user_group,
      owner=params.oozie_user,
      content=Template('adminusers.txt.j2', oozie_user=params.oozie_user)
    )
  else:
    File ( format("{params.conf_dir}/adminusers.txt"),
           owner = params.oozie_user,
           group = params.user_group
    )

  if params.jdbc_driver_name == "com.mysql.jdbc.Driver" or \
     params.jdbc_driver_name == "com.microsoft.sqlserver.jdbc.SQLServerDriver" or \
     params.jdbc_driver_name == "org.postgresql.Driver" or \
     params.jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
    File(format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
      content = DownloadSource(format("{jdk_location}{check_db_connection_jar_name}")),
    )
  pass

  oozie_ownership()
  
  if is_server:      
    oozie_server_specific()
  
def oozie_ownership():
  import params
  
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
  
def oozie_server_specific():
  import params
  
  File(params.pid_file,
    action="delete",
    not_if="ls {pid_file} >/dev/null 2>&1 && !(ps `cat {pid_file}` >/dev/null 2>&1)"
  )
  
  oozie_server_directories = [format("{oozie_home}/{oozie_tmp_dir}"), params.oozie_pid_dir, params.oozie_log_dir, params.oozie_tmp_dir, params.oozie_data_dir, params.oozie_lib_dir, params.oozie_webapps_dir, params.oozie_webapps_conf_dir, params.oozie_server_dir]
  Directory( oozie_server_directories,
    owner = params.oozie_user,
    group = params.user_group,
    mode = 0755,
    recursive = True,
    cd_access="a",
  )
  
  Directory(params.oozie_libext_dir,
            recursive=True,
  )
  
  configure_cmds = []  
  configure_cmds.append(('tar','-xvf',format('{oozie_home}/oozie-sharelib.tar.gz'),'-C',params.oozie_home))
  configure_cmds.append(('cp', params.ext_js_path, params.oozie_libext_dir))
  configure_cmds.append(('chown', format('{oozie_user}:{user_group}'), format('{oozie_libext_dir}/{ext_js_file}')))
  configure_cmds.append(('chown', '-RL', format('{oozie_user}:{user_group}'), params.oozie_webapps_conf_dir))
  
  no_op_test = format("ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1")
  Execute( configure_cmds,
    not_if  = no_op_test,
    sudo = True,
  )

  if params.jdbc_driver_name=="com.mysql.jdbc.Driver" or \
     params.jdbc_driver_name == "com.microsoft.sqlserver.jdbc.SQLServerDriver" or \
     params.jdbc_driver_name=="oracle.jdbc.driver.OracleDriver":
    File(params.downloaded_custom_connector,
         content = DownloadSource(params.driver_curl_source),
    )


    Execute(('cp', '--remove-destination', params.downloaded_custom_connector, params.target),
            #creates=params.target, TODO: uncomment after ranger_hive_plugin will not provide jdbc
            path=["/bin", "/usr/bin/"],
            sudo = True)

  #falcon el extension
  if params.has_falcon_host:
    Execute(format('sudo cp {falcon_home}/oozie/ext/falcon-oozie-el-extension-*.jar {oozie_libext_dir}'),
      not_if  = no_op_test,
    )
    Execute(format('sudo chown {oozie_user}:{user_group} {oozie_libext_dir}/falcon-oozie-el-extension-*.jar'),
      not_if  = no_op_test,
    )
  if params.lzo_enabled:
    Package(params.lzo_packages_for_current_host)
    Execute(format('sudo cp {hadoop_lib_home}/hadoop-lzo*.jar {oozie_lib_dir}'),
      not_if  = no_op_test,
    )

  Execute(format("cd {oozie_tmp_dir} && {oozie_setup_sh} prepare-war"),
    user = params.oozie_user,
    not_if  = no_op_test
  )

  if params.hdp_stack_version != "" and compare_versions(params.hdp_stack_version, '2.2') >= 0:
    # Create hive-site and tez-site configs for oozie
    Directory(params.hive_conf_dir,
        recursive = True,
        owner = params.oozie_user,
        group = params.user_group
    )
    if 'hive-site' in params.config['configurations']:
      XmlConfig("hive-site.xml",
        conf_dir=params.hive_conf_dir,
        configurations=params.config['configurations']['hive-site'],
        configuration_attributes=params.config['configuration_attributes']['hive-site'],
        owner=params.oozie_user,
        group=params.user_group,
        mode=0644
    )
    if 'tez-site' in params.config['configurations']:
      XmlConfig( "tez-site.xml",
        conf_dir = params.hive_conf_dir,
        configurations = params.config['configurations']['tez-site'],
        configuration_attributes=params.config['configuration_attributes']['tez-site'],
        owner = params.oozie_user,
        group = params.user_group,
        mode = 0664
    )
  pass
  
