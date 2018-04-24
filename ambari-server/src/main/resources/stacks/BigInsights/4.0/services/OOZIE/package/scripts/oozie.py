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
from resource_management.core.shell import as_user


# TODO: see if see can remove this
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
             create_parents = True,
             owner = params.oozie_user,
             group = params.user_group
  )
  XmlConfig("oozie-site.xml",
    conf_dir = params.conf_dir,
    configurations = params.oozie_site,
    configuration_attributes=params.config['configuration_attributes']['oozie-site'],
    owner = params.oozie_user,
    group = params.user_group,
    mode = 0660
  )
  File(format("{conf_dir}/oozie-env.sh"),
    owner=params.oozie_user,
    content=InlineTemplate(params.oozie_env_sh_template),
    group=params.user_group,
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

  File(format("{params.conf_dir}/adminusers.txt"),
    mode=0644,
    group=params.user_group,
    owner=params.oozie_user,
    content=Template('adminusers.txt.j2', oozie_user=params.oozie_user)
  )

  environment = {
    "no_proxy": format("{ambari_server_hostname}")
  }

  if params.jdbc_driver_name == "com.mysql.jdbc.Driver" or \
     params.jdbc_driver_name == "org.postgresql.Driver" or \
     params.jdbc_driver_name == "oracle.jdbc.driver.OracleDriver":
    File(format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
      content = DownloadSource(format("{jdk_location}/{check_db_connection_jar_name}")),
    )
  pass

  oozie_ownership()

  if is_server:
    oozie_server_specific()

def download_database_library_if_needed():
  """
  Downloads the library to use when connecting to the Oozie database, if
  necessary. The library will be downloaded to 'params.target' unless
  otherwise specified.
  :param target_directory: the location where the database library will be
  downloaded to.
  :return:
  """
  import params
  # check to see if the JDBC driver name is in the list of ones that need to
  # be downloaded
  if params.jdbc_driver_name=="com.mysql.jdbc.Driver" or \
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


def oozie_ownership():
  import params

  File ( format("{conf_dir}/hadoop-config.xml"),
    owner = params.oozie_user,
    group = params.user_group
  )

  File ( format("{conf_dir}/oozie-default.xml"),
    owner = params.oozie_user,
    group = params.user_group,
    mode  = 0644
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
    create_parents = True,
    cd_access="a",
  )

  Directory(params.oozie_libext_dir,
            create_parents=True,
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
  #configure_cmds.append(('tar','-xvf',format('{oozie_home}/oozie-sharelib.tar.gz'),'-C',params.oozie_home))
  #configure_cmds.append(('cp', params.ext_js_path, params.oozie_libext_dir))
  #configure_cmds.append(('chown', format('{oozie_user}:{user_group}'), format('{oozie_libext_dir}/{ext_js_file}')))
  configure_cmds.append(('chown', '-RL', format('{oozie_user}:{user_group}'), params.oozie_webapps_conf_dir))


  Execute( configure_cmds,
    not_if  = no_op_test,
    sudo = True,
  )

  if params.jdbc_driver_name=="com.mysql.jdbc.Driver" or \
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
    Execute(format('rm -rf {oozie_libext_dir}/falcon-oozie-el-extension.jar'),)
    if params.security_enabled:
      Execute(format('/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab {hdfs_principal_name}'))
    Execute(format('hadoop fs -get /user/falcon/temp/falcon-oozie-el-extension.jar {oozie_libext_dir}'),
      not_if  = no_op_test,
    )
    Execute(format('{sudo} chown {oozie_user}:{user_group} {oozie_libext_dir}/falcon-oozie-el-extension.jar'),
      not_if  = no_op_test,
    )
  #if params.lzo_enabled and len(params.lzo_packages_for_current_host) > 0:
  #  Package(params.lzo_packages_for_current_host)
  #  Execute(format('{sudo} cp {hadoop_lib_home}/hadoop-lzo*.jar {oozie_lib_dir}'),
  #    not_if  = no_op_test,
  #  )

  prepare_war_cmd_file = format("{oozie_home}/.prepare_war_cmd")
  prepare_war_cmd = format("cd {oozie_tmp_dir} && {oozie_setup_sh} prepare-war {oozie_secure}")
  skip_prepare_war_cmd = format("test -f {prepare_war_cmd_file} && [[ `cat {prepare_war_cmd_file}` == '{prepare_war_cmd}' ]]")

  Execute(prepare_war_cmd,    # time-expensive
    user = params.oozie_user,
    not_if  = format("{no_op_test} || {skip_recreate_sharelib} && {skip_prepare_war_cmd}")
  )
  File(hashcode_file,
       content = hashcode,
       mode = 0644,
  )
  File(prepare_war_cmd_file,
       content = prepare_war_cmd,
       mode = 0644,
  )

  # Create hive-site and tez-site configs for oozie
  Directory(params.hive_conf_dir,
        create_parents = True,
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
        mode=0640
  )
  '''if 'tez-site' in params.config['configurations']:
      XmlConfig( "tez-site.xml",
        conf_dir = params.hive_conf_dir,
        configurations = params.config['configurations']['tez-site'],
        configuration_attributes=params.config['configuration_attributes']['tez-site'],
        owner = params.oozie_user,
        group = params.user_group,
        mode = 0664
  )'''
  Execute(('chown', '-R', format("{oozie_user}:{user_group}"), params.oozie_server_dir),
          sudo=True
  )
