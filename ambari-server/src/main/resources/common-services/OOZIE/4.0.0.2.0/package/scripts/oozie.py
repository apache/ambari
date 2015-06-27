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
import hashlib
import os

from resource_management.core.resources.service import ServiceConfig
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.source import DownloadSource
from resource_management.core.source import InlineTemplate
from resource_management.core.source import Template
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.script.script import Script
from resource_management.core.resources.packaging import Package
from resource_management.core.shell import as_user
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from ambari_commons.inet_utils import download_file

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def oozie(is_server=False):
  import params

  from status_params import oozie_server_win_service_name

  XmlConfig("oozie-site.xml",
            conf_dir=params.oozie_conf_dir,
            configurations=params.config['configurations']['oozie-site'],
            owner=params.oozie_user,
            mode='f',
            configuration_attributes=params.config['configuration_attributes']['oozie-site']
  )

  File(os.path.join(params.oozie_conf_dir, "oozie-env.cmd"),
       owner=params.oozie_user,
       content=InlineTemplate(params.oozie_env_cmd_template)
  )

  Directory(params.oozie_tmp_dir,
            owner=params.oozie_user,
            recursive = True,
  )

  if is_server:
    # Manually overriding service logon user & password set by the installation package
    ServiceConfig(oozie_server_win_service_name,
                  action="change_user",
                  username = params.oozie_user,
                  password = Script.get_password(params.oozie_user))

  download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc4.jar"),
                      os.path.join(params.oozie_root, "extra_libs", "sqljdbc4.jar")
  )
  webapps_sqljdbc_path = os.path.join(params.oozie_home, "oozie-server", "webapps", "oozie", "WEB-INF", "lib", "sqljdbc4.jar")
  if os.path.isfile(webapps_sqljdbc_path):
    download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc4.jar"),
                        webapps_sqljdbc_path
    )
  download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc4.jar"),
                      os.path.join(params.oozie_home, "share", "lib", "oozie", "sqljdbc4.jar")
  )
  download_file(os.path.join(params.config['hostLevelParams']['jdk_location'], "sqljdbc4.jar"),
                      os.path.join(params.oozie_home, "temp", "WEB-INF", "lib", "sqljdbc4.jar")
  )

# TODO: see if see can remove this
@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def oozie(is_server=False):
  import params

  if is_server:
    params.HdfsResource(params.oozie_hdfs_user_dir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.oozie_user,
                         mode=params.oozie_hdfs_user_mode
    )
    params.HdfsResource(None, action="execute")
  Directory(params.conf_dir,
             recursive = True,
             owner = params.oozie_user,
             group = params.user_group
  )
  XmlConfig("oozie-site.xml",
    conf_dir = params.conf_dir,
    configurations = params.oozie_site,
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
      content=Template('adminusers.txt.j2', oozie_admin_users=params.oozie_admin_users)
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
  
  no_op_test = as_user(format("ls {pid_file} >/dev/null 2>&1 && ps -p `cat {pid_file}` >/dev/null 2>&1"), user=params.oozie_user)
  
  File(params.pid_file,
    action="delete",
    not_if=no_op_test
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
  
  hashcode_file = format("{oozie_home}/.hashcode")
  hashcode = hashlib.md5(format('{oozie_home}/oozie-sharelib.tar.gz')).hexdigest()
  skip_recreate_sharelib = format("test -f {hashcode_file} && test -d {oozie_home}/share && [[ `cat {hashcode_file}` == '{hashcode}' ]]")

  untar_sharelib = ('tar','-xvf',format('{oozie_home}/oozie-sharelib.tar.gz'),'-C',params.oozie_home)
  
  Execute( untar_sharelib,    # time-expensive
    not_if  = format("{no_op_test} || {skip_recreate_sharelib}"), 
    sudo = True,
  )
    
  configure_cmds = []
  configure_cmds.append(('cp', params.ext_js_path, params.oozie_libext_dir))
  configure_cmds.append(('chown', format('{oozie_user}:{user_group}'), format('{oozie_libext_dir}/{ext_js_file}')))
  configure_cmds.append(('chown', '-RL', format('{oozie_user}:{user_group}'), params.oozie_webapps_conf_dir))
  
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
            
    File ( params.target,
      owner = params.oozie_user,
      group = params.user_group
    )

  #falcon el extension
  if params.has_falcon_host:
    Execute(format('{sudo} cp {falcon_home}/oozie/ext/falcon-oozie-el-extension-*.jar {oozie_libext_dir}'),
      not_if  = no_op_test,
    )
    Execute(format('{sudo} chown {oozie_user}:{user_group} {oozie_libext_dir}/falcon-oozie-el-extension-*.jar'),
      not_if  = no_op_test,
    )
  if params.lzo_enabled and len(params.all_lzo_packages) > 0:
    Package(params.all_lzo_packages)
    Execute(format('{sudo} cp {hadoop_lib_home}/hadoop-lzo*.jar {oozie_lib_dir}'),
      not_if  = no_op_test,
    )

  Execute(format("cd {oozie_tmp_dir} && {oozie_setup_sh} prepare-war {oozie_secure}"),    # time-expensive
    user = params.oozie_user,
    not_if  = format("{no_op_test} || {skip_recreate_sharelib}")
  )
  File(hashcode_file,
       content = hashcode,
       mode = 0644,
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
  Execute(('chown', '-R', format("{oozie_user}:{user_group}"), params.oozie_server_dir), 
          sudo=True
  )
