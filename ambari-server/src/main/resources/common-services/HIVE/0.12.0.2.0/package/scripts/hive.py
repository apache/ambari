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
from resource_management.libraries.functions import copy_tarball
from resource_management.libraries.functions.get_config import get_config
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
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
from resource_management.libraries.functions.setup_atlas_hook import has_atlas_in_cluster, setup_atlas_hook
from resource_management.libraries.functions.security_commons import update_credential_provider_path
from resource_management.libraries.functions.lzo_utils import install_lzo_if_needed
from ambari_commons.constants import SERVICE

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def hive(name=None):
  import params
  
  install_lzo_if_needed()

  hive_client_conf_path = format("{stack_root}/current/{component_directory}/conf")
  # Permissions 644 for conf dir (client) files, and 600 for conf.server
  mode_identified = 0644 if params.hive_config_dir == hive_client_conf_path else 0600

  Directory(params.hive_etc_dir_prefix,
            mode=0755
  )

  # We should change configurations for client as well as for server.
  # The reason is that stale-configs are service-level, not component.
  Logger.info("Directories to fill with configs: %s" % str(params.hive_conf_dirs_list))
  for conf_dir in params.hive_conf_dirs_list:
    fill_conf_dir(conf_dir)

  params.hive_site_config = update_credential_provider_path(params.hive_site_config,
                                                     'hive-site',
                                                     os.path.join(params.hive_config_dir, 'hive-site.jceks'),
                                                     params.hive_user,
                                                     params.user_group
                                                     )
  XmlConfig("hive-site.xml",
            conf_dir=params.hive_config_dir,
            configurations=params.hive_site_config,
            configuration_attributes=params.config['configurationAttributes']['hive-site'],
            owner=params.hive_user,
            group=params.user_group,
            mode=mode_identified)

  # Generate atlas-application.properties.xml file
  if params.enable_atlas_hook:
    atlas_hook_filepath = os.path.join(params.hive_config_dir, params.atlas_hook_filename)
    setup_atlas_hook(SERVICE.HIVE, params.hive_atlas_application_properties, atlas_hook_filepath, params.hive_user, params.user_group)
  
  File(format("{hive_config_dir}/hive-env.sh"),
       owner=params.hive_user,
       group=params.user_group,
       content=InlineTemplate(params.hive_env_sh_template),
       mode=mode_identified
  )

  # On some OS this folder could be not exists, so we will create it before pushing there files
  Directory(params.limits_conf_dir,
            create_parents = True,
            owner='root',
            group='root'
            )

  File(os.path.join(params.limits_conf_dir, 'hive.conf'),
       owner='root',
       group='root',
       mode=0644,
       content=Template("hive.conf.j2")
       )
  if params.security_enabled:
    File(os.path.join(params.hive_config_dir, 'zkmigrator_jaas.conf'),
         owner=params.hive_user,
         group=params.user_group,
         content=Template("zkmigrator_jaas.conf.j2")
         )

  File(format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
       content = DownloadSource(format("{jdk_location}/{check_db_connection_jar_name}")),
       mode = 0644,
  )

  if name != "client":
    setup_non_client()
  if name == 'hiveserver2':
    setup_hiveserver2()
  if name == 'metastore':
    setup_metastore()

def setup_hiveserver2():
  import params
  
  File(params.start_hiveserver2_path,
       mode=0755,
       content=Template(format('{start_hiveserver2_script}'))
  )

  File(os.path.join(params.hive_server_conf_dir, "hadoop-metrics2-hiveserver2.properties"),
       owner=params.hive_user,
       group=params.user_group,
       content=Template("hadoop-metrics2-hiveserver2.properties.j2"),
       mode=0600
  )
  XmlConfig("hiveserver2-site.xml",
            conf_dir=params.hive_server_conf_dir,
            configurations=params.config['configurations']['hiveserver2-site'],
            configuration_attributes=params.config['configurationAttributes']['hiveserver2-site'],
            owner=params.hive_user,
            group=params.user_group,
            mode=0600)
  
  # copy tarball to HDFS feature not supported
  if not (params.stack_version_formatted_major and check_stack_feature(StackFeature.COPY_TARBALL_TO_HDFS, params.stack_version_formatted_major)):  
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
                         owner=params.webhcat_user,
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
  #  if copy tarball to HDFS feature  supported copy mapreduce.tar.gz and tez.tar.gz to HDFS
  if params.stack_version_formatted_major and check_stack_feature(StackFeature.COPY_TARBALL_TO_HDFS, params.stack_version_formatted_major):
    copy_tarball.copy_to_hdfs("mapreduce", params.user_group, params.hdfs_user, skip=params.sysprep_skip_copy_tarballs_hdfs)
    copy_tarball.copy_to_hdfs("tez", params.user_group, params.hdfs_user, skip=params.sysprep_skip_copy_tarballs_hdfs)

  # Always copy pig.tar.gz and hive.tar.gz using the appropriate mode.
  # This can use a different source and dest location to account
  copy_tarball.copy_to_hdfs("pig",
               params.user_group,
               params.hdfs_user,
               file_mode=params.tarballs_mode,
               custom_source_file=params.pig_tar_source,
               custom_dest_file=params.pig_tar_dest_file,
               skip=params.sysprep_skip_copy_tarballs_hdfs)
  copy_tarball.copy_to_hdfs("hive",
               params.user_group,
               params.hdfs_user,
               file_mode=params.tarballs_mode,
               custom_source_file=params.hive_tar_source,
               custom_dest_file=params.hive_tar_dest_file,
               skip=params.sysprep_skip_copy_tarballs_hdfs)

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

      copy_tarball.copy_to_hdfs(tarball_name,
                   params.user_group,
                   params.hdfs_user,
                   file_mode=params.tarballs_mode,
                   custom_source_file=source_file,
                   custom_dest_file=dest_file,
                   skip=params.sysprep_skip_copy_tarballs_hdfs)
  # ******* End Copy Tarballs *******
  # *********************************
  
  # if warehouse directory is in DFS
  if not params.whs_dir_protocol or params.whs_dir_protocol == urlparse(params.default_fs).scheme:
    # Create Hive Metastore Warehouse Dir
    params.HdfsResource(params.hive_apps_whs_dir,
                         type="directory",
                          action="create_on_execute",
                          owner=params.hive_user,
                          group=params.user_group,
                          mode=params.hive_apps_whs_mode
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

  if params.hive_repl_cmrootdir:
    params.HdfsResource(params.hive_repl_cmrootdir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = params.hive_user,
                        group=params.user_group,
                        mode = 01777)
  if params.hive_repl_rootdir:
    params.HdfsResource(params.hive_repl_rootdir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = params.hive_user,
                        group=params.user_group,
                        mode = 0700)

  params.HdfsResource(None, action="execute")
  
def setup_non_client():
  import params
  
  Directory(params.hive_pid_dir,
            create_parents = True,
            cd_access='a',
            owner=params.hive_user,
            group=params.user_group,
            mode=0755)
  Directory(params.hive_log_dir,
            create_parents = True,
            cd_access='a',
            owner=params.hive_user,
            group=params.user_group,
            mode=0755)
  Directory(params.hive_var_lib,
            create_parents = True,
            cd_access='a',
            owner=params.hive_user,
            group=params.user_group,
            mode=0755)

  if params.hive_jdbc_target is not None and not os.path.exists(params.hive_jdbc_target):
    jdbc_connector(params.hive_jdbc_target, params.hive_previous_jdbc_jar)
  if params.hive2_jdbc_target is not None and not os.path.exists(params.hive2_jdbc_target):
    jdbc_connector(params.hive2_jdbc_target, params.hive2_previous_jdbc_jar)
    
def setup_metastore():
  import params
  
  if params.hive_metastore_site_supported:
    hivemetastore_site_config = get_config("hivemetastore-site")
    if hivemetastore_site_config:
      XmlConfig("hivemetastore-site.xml",
                conf_dir=params.hive_server_conf_dir,
                configurations=params.config['configurations']['hivemetastore-site'],
                configuration_attributes=params.config['configurationAttributes']['hivemetastore-site'],
                owner=params.hive_user,
                group=params.user_group,
                mode=0600)
  
  File(os.path.join(params.hive_server_conf_dir, "hadoop-metrics2-hivemetastore.properties"),
       owner=params.hive_user,
       group=params.user_group,
       content=Template("hadoop-metrics2-hivemetastore.properties.j2"),
       mode=0600
  )

  File(params.start_metastore_path,
       mode=0755,
       content=StaticFile('startMetastore.sh')
  )

  if not is_empty(params.hive_exec_scratchdir):
    dirPathStr = urlparse(params.hive_exec_scratchdir).path
    pathComponents = dirPathStr.split("/")
    if dirPathStr.startswith("/tmp") and len(pathComponents) > 2:
      Directory (params.hive_exec_scratchdir,
                 owner = params.hive_user,
                 create_parents = True,
                 mode=0777)

  if params.hive_repl_cmrootdir:
    params.HdfsResource(params.hive_repl_cmrootdir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = params.hive_user,
                        group=params.user_group,
                        mode = 01777)
  if params.hive_repl_rootdir:
    params.HdfsResource(params.hive_repl_rootdir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = params.hive_user,
                        group=params.user_group,
                        mode = 0700)
  if params.hive_repl_cmrootdir or params.hive_repl_rootdir:
    params.HdfsResource(None, action="execute")

def create_metastore_schema():
  import params

  create_schema_cmd = format("export HIVE_CONF_DIR={hive_server_conf_dir} ; "
                             "{hive_schematool_bin}/schematool -initSchema "
                             "-dbType {hive_metastore_db_type} "
                             "-userName {hive_metastore_user_name} "
                             "-passWord {hive_metastore_user_passwd!p} -verbose")

  check_schema_created_cmd = as_user(format("export HIVE_CONF_DIR={hive_server_conf_dir} ; "
                                    "{hive_schematool_bin}/schematool -info "
                                    "-dbType {hive_metastore_db_type} "
                                    "-userName {hive_metastore_user_name} "
                                    "-passWord {hive_metastore_user_passwd!p} -verbose"), params.hive_user)

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
    
"""
Writes configuration files required by Hive.
"""
def fill_conf_dir(component_conf_dir):
  import params
  hive_client_conf_path = os.path.realpath(format("{stack_root}/current/{component_directory}/conf"))
  component_conf_dir = os.path.realpath(component_conf_dir)
  mode_identified_for_file = 0644 if component_conf_dir == hive_client_conf_path else 0600
  mode_identified_for_dir = 0755 if component_conf_dir == hive_client_conf_path else 0700
  Directory(component_conf_dir,
            owner=params.hive_user,
            group=params.user_group,
            create_parents = True,
            mode=mode_identified_for_dir
  )

  if 'mapred-site' in params.config['configurations']:
    XmlConfig("mapred-site.xml",
              conf_dir=component_conf_dir,
              configurations=params.config['configurations']['mapred-site'],
              configuration_attributes=params.config['configurationAttributes']['mapred-site'],
              owner=params.hive_user,
              group=params.user_group,
              mode=mode_identified_for_file)

  File(format("{component_conf_dir}/hive-default.xml.template"),
       owner=params.hive_user,
       group=params.user_group,
       mode=mode_identified_for_file
  )

  File(format("{component_conf_dir}/hive-env.sh.template"),
       owner=params.hive_user,
       group=params.user_group,
       mode=mode_identified_for_file
  )

  # Create hive-log4j.properties and hive-exec-log4j.properties
  # in /etc/hive/conf and not in /etc/hive2/conf
  if params.log4j_version == '1':
    log4j_exec_filename = 'hive-exec-log4j.properties'
    if (params.log4j_exec_props != None):
      File(format("{component_conf_dir}/{log4j_exec_filename}"),
           mode=mode_identified_for_file,
           group=params.user_group,
           owner=params.hive_user,
           content=InlineTemplate(params.log4j_exec_props)
      )
    elif (os.path.exists("{component_conf_dir}/{log4j_exec_filename}.template")):
      File(format("{component_conf_dir}/{log4j_exec_filename}"),
           mode=mode_identified_for_file,
           group=params.user_group,
           owner=params.hive_user,
           content=StaticFile(format("{component_conf_dir}/{log4j_exec_filename}.template"))
      )

    log4j_filename = 'hive-log4j.properties'
    if (params.log4j_props != None):
      File(format("{component_conf_dir}/{log4j_filename}"),
           mode=mode_identified_for_file,
           group=params.user_group,
           owner=params.hive_user,
           content=InlineTemplate(params.log4j_props)
      )
    elif (os.path.exists("{component_conf_dir}/{log4j_filename}.template")):
      File(format("{component_conf_dir}/{log4j_filename}"),
           mode=mode_identified_for_file,
           group=params.user_group,
           owner=params.hive_user,
           content=StaticFile(format("{component_conf_dir}/{log4j_filename}.template"))
      )

  if params.parquet_logging_properties is not None:
    File(format("{component_conf_dir}/parquet-logging.properties"),
      mode = mode_identified_for_file,
      group = params.user_group,
      owner = params.hive_user,
      content = params.parquet_logging_properties)


def jdbc_connector(target, hive_previous_jdbc_jar):
  """
  Shared by Hive Batch, Hive Metastore, and Hive Interactive
  :param target: Target of jdbc jar name, which could be for any of the components above.
  """
  import params

  if not params.jdbc_jar_name:
    return

  if params.hive_jdbc_driver in params.hive_jdbc_drivers_list and params.hive_use_existing_db:
    environment = {
      "no_proxy": format("{ambari_server_hostname}")
    }

    if hive_previous_jdbc_jar and os.path.isfile(hive_previous_jdbc_jar):
      File(hive_previous_jdbc_jar, action='delete')

    # TODO: should be removed after ranger_hive_plugin will not provide jdbc
    if params.prepackaged_jdbc_name != params.jdbc_jar_name:
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
                create_parents = True)

      Execute(format("yes | {sudo} cp {libs_path_in_archive} {jdbc_libs_dir}"))

      Execute(format("{sudo} chown -R {hive_user}:{user_group} {hive_lib}/*"))

    else:
      Execute(('cp', '--remove-destination', params.downloaded_custom_connector, target),
            #creates=target, TODO: uncomment after ranger_hive_plugin will not provide jdbc
            path=["/bin", "/usr/bin/"],
            sudo = True)

  else:
    #for default hive db (Mysql)
    File(params.downloaded_custom_connector, content = DownloadSource(params.driver_curl_source))
    Execute(('cp', '--remove-destination', params.downloaded_custom_connector, target),
            #creates=target, TODO: uncomment after ranger_hive_plugin will not provide jdbc
            path=["/bin", "/usr/bin/"],
            sudo=True
    )
  pass

  File(target,
       mode = 0644,
  )
  
@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def hive(name=None):
  import params

  XmlConfig("hive-site.xml",
            conf_dir = params.hive_conf_dir,
            configurations = params.config['configurations']['hive-site'],
            owner=params.hive_user,
            configuration_attributes=params.config['configurationAttributes']['hive-site']
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
