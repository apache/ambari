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

# Python Imports
import os
import glob
import traceback
from urlparse import urlparse

# Ambari Commons & Resource Management Imports
from ambari_commons.constants import SERVICE
from resource_management.core import utils
from resource_management.core.resources.system import File, Execute, Directory
from resource_management.core.logger import Logger
from resource_management.core.shell import as_user, quote_bash_args
from resource_management.core.source import StaticFile, Template, DownloadSource, InlineTemplate
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.copy_tarball import copy_to_hdfs
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.generate_logfeeder_input_config import generate_logfeeder_input_config
from resource_management.libraries.functions.get_config import get_config
from resource_management.libraries.functions.get_user_call_output import get_user_call_output
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.security_commons import update_credential_provider_path
from resource_management.libraries.functions.setup_atlas_hook import setup_atlas_hook
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.resources.hdfs_resource import HdfsResource
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.lzo_utils import install_lzo_if_needed


def hive(name=None):
  import params

  install_lzo_if_needed()

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
            conf_dir = params.hive_config_dir,
            configurations = params.hive_site_config,
            configuration_attributes = params.config['configurationAttributes']['hive-site'],
            owner = params.hive_user,
            group = params.user_group,
            mode = 0644)

  # Generate atlas-application.properties.xml file
  if params.enable_atlas_hook:
    atlas_hook_filepath = os.path.join(params.hive_config_dir, params.atlas_hook_filename)
    setup_atlas_hook(SERVICE.HIVE, params.hive_atlas_application_properties, atlas_hook_filepath, params.hive_user, params.user_group)

  File(format("{hive_config_dir}/hive-env.sh"),
       owner=params.hive_user,
       group=params.user_group,
       content=InlineTemplate(params.hive_env_sh_template),
       mode=0755
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

  if params.hive_jdbc_target is not None and not os.path.exists(params.hive_jdbc_target):
    jdbc_connector(params.hive_jdbc_target, params.hive_previous_jdbc_jar)

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

  # ****** Begin Copy Tarballs ******
  # *********************************
  #  if copy tarball to HDFS feature  supported copy mapreduce.tar.gz and tez.tar.gz to HDFS
  if params.stack_version_formatted_major and check_stack_feature(StackFeature.COPY_TARBALL_TO_HDFS, params.stack_version_formatted_major):
    copy_to_hdfs("mapreduce", params.user_group, params.hdfs_user, skip=params.sysprep_skip_copy_tarballs_hdfs)
    copy_to_hdfs("tez", params.user_group, params.hdfs_user, skip=params.sysprep_skip_copy_tarballs_hdfs)

  # Always copy pig.tar.gz and hive.tar.gz using the appropriate mode.
  # This can use a different source and dest location to account
  copy_to_hdfs("pig",
               params.user_group,
               params.hdfs_user,
               file_mode=params.tarballs_mode,
               custom_source_file=params.pig_tar_source,
               custom_dest_file=params.pig_tar_dest_file,
               skip=params.sysprep_skip_copy_tarballs_hdfs)
  copy_to_hdfs("hive",
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

      copy_to_hdfs(tarball_name,
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
    if not is_empty(params.tez_hook_proto_base_directory):
      params.HdfsResource(params.tez_hook_proto_base_directory,
                          type = "directory",
                          action = "create_on_execute",
                          owner = params.hive_user,
                          mode = 01775
                          )

    if not is_empty(params.hive_hook_proto_base_directory):
        params.HdfsResource(params.hive_hook_proto_base_directory,
                            type = "directory",
                            action = "create_on_execute",
                            owner = params.hive_user,
                            mode = 01777
                            )

        dag_meta = params.tez_hook_proto_base_directory + "dag_meta"
        params.HdfsResource(dag_meta,
                            type = "directory",
                            action = "create_on_execute",
                            owner = params.hive_user,
                            mode = 01777
                            )

        dag_data = params.tez_hook_proto_base_directory + "dag_data"
        params.HdfsResource(dag_data,
                            type = "directory",
                            action = "create_on_execute",
                            owner = params.hive_user,
                            mode = 01777
                            )

        app_data = params.tez_hook_proto_base_directory + "app_data"
        params.HdfsResource(app_data,
                            type = "directory",
                            action = "create_on_execute",
                            owner = params.hive_user,
                            mode = 01777
                            )

  if not is_empty(params.hive_exec_scratchdir) and not urlparse(params.hive_exec_scratchdir).path.startswith("/tmp"):
    params.HdfsResource(params.hive_exec_scratchdir,
                         type="directory",
                         action="create_on_execute",
                         owner=params.hive_user,
                         group=params.hdfs_user,
                         mode=0777) # Hive expects this dir to be writeable by everyone as it is used as a temp dir

  if params.hive_repl_cmrootdir is not None and params.hive_repl_cmrootdir.strip() != "":
    params.HdfsResource(params.hive_repl_cmrootdir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = params.hive_user,
                        group=params.user_group,
                        mode = 01777)
  if params.hive_repl_rootdir is not None and params.hive_repl_rootdir.strip() != "":
    params.HdfsResource(params.hive_repl_rootdir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = params.hive_user,
                        group=params.user_group,
                        mode = 0700)

  params.HdfsResource(None, action="execute")

  generate_logfeeder_input_config('hive', Template("input.config-hive.json.j2", extra_imports=[default]))

def create_hive_hdfs_dirs():
  import params

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

  # Create Hive User Dir
  params.HdfsResource(params.hive_hdfs_user_dir,
                       type="directory",
                        action="create_on_execute",
                        owner=params.hive_user,
                        mode=params.hive_hdfs_user_mode
  )

  # if warehouse directory is in DFS
  if not params.whs_dir_protocol or params.whs_dir_protocol == urlparse(params.default_fs).scheme:
    # Create Hive Metastore Warehouse Dir
    external_dir = params.hive_metastore_warehouse_external_dir
    managed_dir = params.hive_metastore_warehouse_dir
    params.HdfsResource(external_dir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = params.hive_user,
                        group = params.user_group,
                        mode = 01777
    )
    params.HdfsResource(managed_dir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = params.hive_user,
                        group = params.user_group,
                        mode = 0770
    )
    
    if __is_hdfs_acls_enabled():
      if params.security_enabled:
        kinit_cmd = format("{kinit_path_local} -kt {hdfs_user_keytab} {hdfs_principal_name}; ")
        Execute(kinit_cmd, user=params.hdfs_user)

      Execute(format("hdfs dfs -setfacl -m default:user:{hive_user}:rwx {external_dir}"),
              user = params.hdfs_user)
      Execute(format("hdfs dfs -setfacl -m default:user:{hive_user}:rwx {managed_dir}"),
              user = params.hdfs_user)
    else:
      Logger.info(format("Could not set default ACLs for HDFS directories {external_dir} and {managed_dir} as ACLs are not enabled!"))
  else:
    Logger.info(format("Not creating warehouse directory '{hive_metastore_warehouse_dir}', as the location is not in DFS."))

  # Create Tez History dir
  if not params.whs_dir_protocol or params.whs_dir_protocol == urlparse(params.default_fs).scheme:
      if not is_empty(params.tez_hook_proto_base_directory):
          params.HdfsResource(params.tez_hook_proto_base_directory,
                              type = "directory",
                              action = "create_on_execute",
                              owner = params.hive_user,
                              mode = 01775
                              )

  params.HdfsResource(None, action = "execute")

def __is_hdfs_acls_enabled():
  import params
  
  hdfs_protocol = params.fs_root.startswith("hdfs://")
  
  return_code, stdout, _ = get_user_call_output("hdfs getconf -confKey dfs.namenode.acls.enabled",
                                                user = params.hdfs_user)
  acls_enabled = stdout == "true"
  return_code, stdout, _ = get_user_call_output("hdfs getconf -confKey dfs.namenode.posix.acl.inheritance.enabled",
                                                user = params.hdfs_user)
  acls_inheritance_enabled = stdout == "true"
  
  return hdfs_protocol and acls_enabled and acls_inheritance_enabled

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

  if params.hive_repl_cmrootdir is not None and params.hive_repl_cmrootdir.strip() != "":
    params.HdfsResource(params.hive_repl_cmrootdir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = params.hive_user,
                        group=params.user_group,
                        mode = 01777)
  if params.hive_repl_rootdir is not None and params.hive_repl_rootdir.strip() != "":
    params.HdfsResource(params.hive_repl_rootdir,
                        type = "directory",
                        action = "create_on_execute",
                        owner = params.hive_user,
                        group=params.user_group,
                        mode = 0700)
  params.HdfsResource(None, action="execute")

  generate_logfeeder_input_config('hive', Template("input.config-hive.json.j2", extra_imports=[default]))

def refresh_yarn():
  import params

  if params.enable_ranger_hive or not params.doAs:
    return

  YARN_REFRESHED_FILE = "/etc/hive/yarn.refreshed"

  if os.path.isfile(YARN_REFRESHED_FILE):
    Logger.info("Yarn already refreshed")
    return
  
  if params.security_enabled:
    Execute(params.yarn_kinit_cmd, user = params.yarn_user)
  Execute("yarn rmadmin -refreshSuperUserGroupsConfiguration", user = params.yarn_user)
  Execute("touch " + YARN_REFRESHED_FILE, user = "root")

def create_hive_metastore_schema():
  import params
  
  SYS_DB_CREATED_FILE = "/etc/hive/sys.db.created"

  if os.path.isfile(SYS_DB_CREATED_FILE):
    Logger.info("Sys DB is already created")
    return

  create_hive_schema_cmd = format("export HIVE_CONF_DIR={hive_server_conf_dir} ; "
                                  "{hive_schematool_bin}/schematool -initSchema "
                                  "-dbType hive "
                                  "-metaDbType {hive_metastore_db_type} "
                                  "-userName {hive_metastore_user_name} "
                                  "-passWord {hive_metastore_user_passwd!p} "
                                  "-verbose")

  check_hive_schema_created_cmd = as_user(format("export HIVE_CONF_DIR={hive_server_conf_dir} ; "
                                          "{hive_schematool_bin}/schematool -info "
                                          "-dbType hive "
                                          "-metaDbType {hive_metastore_db_type} "
                                          "-userName {hive_metastore_user_name} "
                                          "-passWord {hive_metastore_user_passwd!p} "
                                          "-verbose"), params.hive_user)

  # HACK: in cases with quoted passwords and as_user (which does the quoting as well) !p won't work for hiding passwords.
  # Fixing it with the hack below:
  quoted_hive_metastore_user_passwd = quote_bash_args(quote_bash_args(params.hive_metastore_user_passwd))
  if quoted_hive_metastore_user_passwd.startswith("'") and quoted_hive_metastore_user_passwd.endswith("'") \
      or quoted_hive_metastore_user_passwd.startswith('"') and quoted_hive_metastore_user_passwd.endswith('"'):
    quoted_hive_metastore_user_passwd = quoted_hive_metastore_user_passwd[1:-1]
  Logger.sensitive_strings[repr(create_hive_schema_cmd)] = repr(create_hive_schema_cmd.replace(
      format("-passWord {quoted_hive_metastore_user_passwd}"), "-passWord " + utils.PASSWORDS_HIDE_STRING))
  Logger.sensitive_strings[repr(check_hive_schema_created_cmd)] = repr(check_hive_schema_created_cmd.replace(
      format("-passWord {quoted_hive_metastore_user_passwd}"), "-passWord " + utils.PASSWORDS_HIDE_STRING))

  try:
    if params.security_enabled:
      hive_kinit_cmd = format("{kinit_path_local} -kt {hive_server2_keytab} {hive_principal}; ")
      Execute(hive_kinit_cmd, user=params.hive_user)

    Execute(create_hive_schema_cmd,
            not_if = check_hive_schema_created_cmd,
            user = params.hive_user
    )
    Execute("touch " + SYS_DB_CREATED_FILE, user = "root")
    Logger.info("Sys DB is set up")
  except:
    Logger.error("Could not create Sys DB.")
    Logger.error(traceback.format_exc())

def create_metastore_schema():
  import params

  if params.sysprep_skip_hive_schema_create:
    Logger.info("Skipping creation of Hive Metastore schema as host is sys prepped")
    return

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
  # hive_client_conf_path = os.path.realpath(format("{stack_root}/current/{component_directory}/conf"))
  component_conf_dir = os.path.realpath(component_conf_dir)
  # mode_identified_for_file = 0644 if component_conf_dir == hive_client_conf_path else 0600
  # mode_identified_for_dir = 0755 if component_conf_dir == hive_client_conf_path else 0700

  mode_identified_for_file = 0644
  mode_identified_for_dir = 0755

  Directory(component_conf_dir,
            owner=params.hive_user,
            group=params.user_group,
            create_parents = True,
            mode=mode_identified_for_dir
  )

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
       mode=0755
  )

  # Create properties files under conf dir
  #   llap-daemon-log4j2.properties
  #   llap-cli-log4j2.properties
  #   hive-log4j2.properties
  #   hive-exec-log4j2.properties
  #   beeline-log4j2.properties

  llap_daemon_log4j_filename = 'llap-daemon-log4j2.properties'
  File(format("{component_conf_dir}/{llap_daemon_log4j_filename}"),
       mode=mode_identified_for_file,
       group=params.user_group,
       owner=params.hive_user,
       content=InlineTemplate(params.llap_daemon_log4j))

  llap_cli_log4j2_filename = 'llap-cli-log4j2.properties'
  File(format("{component_conf_dir}/{llap_cli_log4j2_filename}"),
       mode=mode_identified_for_file,
       group=params.user_group,
       owner=params.hive_user,
       content=InlineTemplate(params.llap_cli_log4j2))

  hive_log4j2_filename = 'hive-log4j2.properties'
  File(format("{component_conf_dir}/{hive_log4j2_filename}"),
       mode=mode_identified_for_file,
       group=params.user_group,
       owner=params.hive_user,
       content=InlineTemplate(params.hive_log4j2))

  hive_exec_log4j2_filename = 'hive-exec-log4j2.properties'
  File(format("{component_conf_dir}/{hive_exec_log4j2_filename}"),
       mode=mode_identified_for_file,
       group=params.user_group,
       owner=params.hive_user,
       content=InlineTemplate(params.hive_exec_log4j2))

  beeline_log4j2_filename = 'beeline-log4j2.properties'
  File(format("{component_conf_dir}/{beeline_log4j2_filename}"),
       mode=mode_identified_for_file,
       group=params.user_group,
       owner=params.hive_user,
       content=InlineTemplate(params.beeline_log4j2))

  XmlConfig("beeline-site.xml",
            conf_dir=component_conf_dir,
            configurations=params.beeline_site_config,
            owner=params.hive_user,
            group=params.user_group,
            mode=mode_identified_for_file)

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
