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
import glob
from urlparse import urlparse

from resource_management.libraries.script.script import Script
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.version import compare_versions
from resource_management.core.resources.service import ServiceConfig
from resource_management.core.resources.system import File, Execute, Directory
from resource_management.core.source import StaticFile, Template, DownloadSource, InlineTemplate
from resource_management.core.shell import as_user
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.format import format
from resource_management.core.exceptions import Fail
from resource_management.core.shell import as_sudo
from resource_management.core.shell import quote_bash_args
from resource_management.core.logger import Logger
from resource_management.core import utils

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst

from setup_atlas_hive import setup_atlas_hive


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

  if name == "hiveserver2":
    if params.hive_execution_engine == "tez":
      # Init the tez app dir in hadoop
      script_file = __file__.replace('/', os.sep)
      cmd_file = os.path.normpath(os.path.join(os.path.dirname(script_file), "..", "files", "hiveTezSetup.cmd"))

      Execute("cmd /c " + cmd_file, logoutput=True, user=params.hadoop_user)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hive(name=None):
  import params

  if name == 'hiveserver2':
    # HDP 2.1.* or lower
    if params.hdp_stack_version_major != "" and compare_versions(params.hdp_stack_version_major, "2.2.0.0") < 0:
      params.HdfsResource(params.webhcat_apps_dir,
                            type="directory",
                            action="create_on_execute",
                            owner=params.webhcat_user,
                            mode=0755
                          )
    
    # Create webhcat dirs.
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

    # ****** Begin Copy Tarballs ******
    # *********************************
    # HDP 2.2 or higher, copy mapreduce.tar.gz to HDFS
    if params.hdp_stack_version_major != "" and compare_versions(params.hdp_stack_version_major, '2.2') >= 0:
      copy_to_hdfs("mapreduce", params.user_group, params.hdfs_user, host_sys_prepped=params.host_sys_prepped)
      copy_to_hdfs("tez", params.user_group, params.hdfs_user, host_sys_prepped=params.host_sys_prepped)

    # Always copy pig.tar.gz and hive.tar.gz using the appropriate mode.
    # This can use a different source and dest location to account for both HDP 2.1 and 2.2
    copy_to_hdfs("pig",
                 params.user_group,
                 params.hdfs_user,
                 file_mode=params.tarballs_mode,
                 custom_source_file=params.pig_tar_source,
                 custom_dest_file=params.pig_tar_dest_file,
                 host_sys_prepped=params.host_sys_prepped)
    copy_to_hdfs("hive",
                 params.user_group,
                 params.hdfs_user,
                 file_mode=params.tarballs_mode,
                 custom_source_file=params.hive_tar_source,
                 custom_dest_file=params.hive_tar_dest_file,
                 host_sys_prepped=params.host_sys_prepped)

    wildcard_tarballs = ["sqoop", "hadoop_streaming"]
    for tarball_name in wildcard_tarballs:
      source_file_pattern = eval("params." + tarball_name + "_tar_source")
      dest_dir = eval("params." + tarball_name + "_tar_dest_dir")

      if source_file_pattern is None or dest_dir is None:
        continue

      source_files = glob.glob(source_file_pattern) if "*" in source_file_pattern else [source_file_pattern]
      for source_file in source_files:
        src_filename = os.path.basename(source_file)
        dest_file = os.path.join(dest_dir, src_filename)

        copy_to_hdfs(tarball_name,
                     params.user_group,
                     params.hdfs_user,
                     file_mode=params.tarballs_mode,
                     custom_source_file=source_file,
                     custom_dest_file=dest_file,
                     host_sys_prepped=params.host_sys_prepped)
    # ******* End Copy Tarballs *******
    # *********************************
    
    # if warehouse directory is in DFS
    if not params.whs_dir_protocol or params.whs_dir_protocol == urlparse(params.default_fs).scheme:
      # Create Hive Metastore Warehouse Dir
      params.HdfsResource(params.hive_apps_whs_dir,
                           type="directory",
                            action="create_on_execute",
                            owner=params.hive_user,
                            mode=0777
      )
    else:
      Logger.info(format("Not creating warehouse directory '{hive_apps_whs_dir}', as the location is not in DFS."))

    # Create Hive User Dir
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
            configurations=params.hive_site_config,
            configuration_attributes=params.config['configuration_attributes']['hive-site'],
            owner=params.hive_user,
            group=params.user_group,
            mode=0644)

  setup_atlas_hive()
  
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

  if (name == 'metastore' or name == 'hiveserver2') and not os.path.exists(params.target):
    jdbc_connector()

  File(format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
       content = DownloadSource(format("{jdk_location}{check_db_connection_jar_name}")),
       mode = 0644,
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

      # HACK: in cases with quoted passwords and as_user (which does the quoting as well) !p won't work for hiding passwords.
      # Fixing it with the hack below:
      quoted_hive_metastore_user_passwd = quote_bash_args(quote_bash_args(params.hive_metastore_user_passwd))
      if quoted_hive_metastore_user_passwd[0] == "'" and quoted_hive_metastore_user_passwd[-1] == "'" \
          or quoted_hive_metastore_user_passwd[0] == '"' and quoted_hive_metastore_user_passwd[-1] == '"':
        quoted_hive_metastore_user_passwd = quoted_hive_metastore_user_passwd[1:-1]
      Logger.sensitive_strings[repr(check_schema_created_cmd)] = repr(check_schema_created_cmd.replace(
          format("-passWord {quoted_hive_metastore_user_passwd}"), "-passWord " + utils.PASSWORDS_HIDE_STRING))

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
         content = DownloadSource(params.driver_curl_source))

    # maybe it will be more correcvly to use db type
    if params.sqla_db_used:
      untar_sqla_type2_driver = ('tar', '-xvf', params.downloaded_custom_connector, '-C', params.tmp_dir)

      Execute(untar_sqla_type2_driver, sudo = True)

      Execute(format("yes | {sudo} cp {jars_path_in_archive} {hive_lib}"))

      Directory(params.jdbc_libs_dir,
                recursive=True)

      Execute(format("yes | {sudo} cp {libs_path_in_archive} {jdbc_libs_dir}"))

      Execute(format("{sudo} chown -R {hive_user}:{user_group} {hive_lib}/*"))

    else:
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
