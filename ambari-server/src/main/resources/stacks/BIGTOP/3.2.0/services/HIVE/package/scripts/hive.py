#!/usr/bin/env python3
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
from contextlib import nullcontext
from urllib.parse import urlparse

# Ambari Commons & Resource Management Imports
from ambari_commons.constants import SERVICE
from resource_management.core import shell
from resource_management.core.resources.system import File, Execute, Directory
from resource_management.core.logger import Logger
from resource_management.core.source import (
  StaticFile,
  Template,
  DownloadSource,
  InlineTemplate,
)
from resource_management.core.utils import PasswordString
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.generate_logfeeder_input_config import (
  generate_logfeeder_input_config,
)
from resource_management.libraries.functions.get_config import get_config
from resource_management.libraries.functions.get_user_call_output import (
  get_user_call_output,
)
from resource_management.libraries.functions.is_empty import is_empty
from resource_management.libraries.functions.security_commons import (
  update_credential_provider_path,
)
from resource_management.libraries.functions.setup_atlas_hook import setup_atlas_hook
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.lzo_utils import install_lzo_if_needed
from resource_management.libraries.functions.private_kerberos_cache import (
  PrivateKerberosCache,
)


def hive(name=None):
  import params

  install_lzo_if_needed()

  fill_conf_dir(params.hive_conf_dir)

  params.hive_site_config = update_credential_provider_path(
    params.hive_site_config,
    "hive-site",
    os.path.join(params.hive_conf_dir, "hive-site.jceks"),
    params.hive_user,
    params.user_group,
  )

  XmlConfig(
    "hive-site.xml",
    conf_dir=params.hive_conf_dir,
    configurations=params.hive_site_config,
    configuration_attributes=params.config["configurationAttributes"]["hive-site"],
    owner="root",
    group=params.user_group,
    mode=0o644,
  )

  # Generate atlas-application.properties.xml file
  if params.enable_atlas_hook:
    atlas_hook_filepath = os.path.join(params.hive_conf_dir, params.atlas_hook_filename)
    setup_atlas_hook(
      SERVICE.HIVE,
      params.hive_atlas_application_properties,
      atlas_hook_filepath,
      "root",
      params.user_group,
    )

  File(
    format("{hive_conf_dir}/hive-env.sh"),
    owner="root",
    group=params.user_group,
    content=InlineTemplate(params.hive_env_sh_template),
    mode=0o644,
  )

  # On some OS this folder could be not exists, so we will create it before pushing there files
  Directory(params.limits_conf_dir, create_parents=True, owner="root", group="root")

  File(
    os.path.join(params.limits_conf_dir, "hive.conf"),
    owner="root",
    group="root",
    mode=0o644,
    content=Template("hive.conf.j2"),
  )
  if params.security_enabled:
    File(
      os.path.join(params.hive_conf_dir, "zkmigrator_jaas.conf"),
      owner="root",
      group=params.user_group,
      content=Template("zkmigrator_jaas.conf.j2"),
      mode=0o640,
    )

  File(
    format("/usr/lib/ambari-agent/{check_db_connection_jar_name}"),
    content=DownloadSource(format("{jdk_location}/{check_db_connection_jar_name}")),
    mode=0o644,
  )

  if params.hive_jdbc_target is not None and not os.path.exists(
    params.hive_jdbc_target
  ):
    jdbc_connector(params.hive_jdbc_target, params.hive_previous_jdbc_jar)

  if name != "client":
    setup_non_client()
  if name == "hiveserver2":
    setup_hiveserver2()
  if name == "metastore":
    setup_metastore()


def setup_hiveserver2():
  import params

  File(
    params.start_hiveserver2_path,
    owner="root",
    group=params.user_group,
    mode=0o750,
    content=Template(format("{start_hiveserver2_script}")),
  )

  File(
    os.path.join(params.hive_conf_dir, "hadoop-metrics2-hiveserver2.properties"),
    owner="root",
    group=params.user_group,
    content=Template("hadoop-metrics2-hiveserver2.properties.j2"),
    mode=0o640,
  )
  XmlConfig(
    "hiveserver2-site.xml",
    conf_dir=params.hive_conf_dir,
    configurations=params.config["configurations"]["hiveserver2-site"],
    configuration_attributes=params.config["configurationAttributes"][
      "hiveserver2-site"
    ],
    owner="root",
    group=params.user_group,
    mode=0o640,
  )

  # if warehouse directory is in DFS
  if (
    not params.whs_dir_protocol
    or params.whs_dir_protocol == urlparse(params.default_fs).scheme
  ):
    if not is_empty(params.tez_hook_proto_base_directory):
      params.HdfsResource(
        params.tez_hook_proto_base_directory,
        type="directory",
        action="create_on_execute",
        owner=params.hive_user,
        mode=0o1775,
      )

    if not is_empty(params.hive_hook_proto_base_directory):
      params.HdfsResource(
        params.hive_hook_proto_base_directory,
        type="directory",
        action="create_on_execute",
        owner=params.hive_user,
        mode=0o1777,
      )

      dag_meta = params.tez_hook_proto_base_directory + "dag_meta"
      params.HdfsResource(
        dag_meta,
        type="directory",
        action="create_on_execute",
        owner=params.hive_user,
        mode=0o1777,
      )

      dag_data = params.tez_hook_proto_base_directory + "dag_data"
      params.HdfsResource(
        dag_data,
        type="directory",
        action="create_on_execute",
        owner=params.hive_user,
        mode=0o1777,
      )

      app_data = params.tez_hook_proto_base_directory + "app_data"
      params.HdfsResource(
        app_data,
        type="directory",
        action="create_on_execute",
        owner=params.hive_user,
        mode=0o1777,
      )

  if not is_empty(params.hive_exec_scratchdir) and not urlparse(
    params.hive_exec_scratchdir
  ).path.startswith("/tmp"):
    params.HdfsResource(
      params.hive_exec_scratchdir,
      type="directory",
      action="create_on_execute",
      owner=params.hive_user,
      group=params.hdfs_user,
      mode=0o1777,
    )  # Hive scratch space is shared, but the sticky bit protects per-user data.

  if (
    params.hive_repl_cmrootdir is not None and params.hive_repl_cmrootdir.strip() != ""
  ):
    params.HdfsResource(
      params.hive_repl_cmrootdir,
      type="directory",
      action="create_on_execute",
      owner=params.hive_user,
      group=params.user_group,
      mode=0o1777,
    )
  if params.hive_repl_rootdir is not None and params.hive_repl_rootdir.strip() != "":
    params.HdfsResource(
      params.hive_repl_rootdir,
      type="directory",
      action="create_on_execute",
      owner=params.hive_user,
      group=params.user_group,
      mode=0o700,
    )

  params.HdfsResource(None, action="execute")

  generate_logfeeder_input_config(
    "hive", Template("input.config-hive.json.j2", extra_imports=[default])
  )


def create_hive_hdfs_dirs():
  import params

  # Create webhcat dirs.
  if params.hcat_hdfs_user_dir != params.webhcat_hdfs_user_dir:
    params.HdfsResource(
      params.hcat_hdfs_user_dir,
      type="directory",
      action="create_on_execute",
      owner=params.webhcat_user,
      mode=params.hcat_hdfs_user_mode,
    )

  params.HdfsResource(
    params.webhcat_hdfs_user_dir,
    type="directory",
    action="create_on_execute",
    owner=params.webhcat_user,
    mode=params.webhcat_hdfs_user_mode,
  )

  # Create Hive User Dir
  params.HdfsResource(
    params.hive_hdfs_user_dir,
    type="directory",
    action="create_on_execute",
    owner=params.hive_user,
    mode=params.hive_hdfs_user_mode,
  )

  # if warehouse directory is in DFS
  if (
    not params.whs_dir_protocol
    or params.whs_dir_protocol == urlparse(params.default_fs).scheme
  ):
    # Create Hive Metastore Warehouse Dir
    external_dir = params.hive_metastore_warehouse_external_dir
    managed_dir = params.hive_metastore_warehouse_dir
    params.HdfsResource(
      external_dir,
      type="directory",
      action="create_on_execute",
      owner=params.hive_user,
      group=params.user_group,
      mode=0o1777,
    )
    params.HdfsResource(
      managed_dir,
      type="directory",
      action="create_on_execute",
      owner=params.hive_user,
      group=params.user_group,
      mode=0o770,
    )

    if __is_hdfs_acls_enabled():
      cache_context = nullcontext(None)
      if params.security_enabled:
        cache_context = PrivateKerberosCache(
          params.hdfs_user,
          params.user_group,
          params.tmp_dir,
          "ambari-hive-hdfs-acl-",
        )
      with cache_context as cache:
        environment = None
        if cache is not None:
          cache.kinit(
            params.kinit_path_local,
            params.hdfs_user_keytab,
            params.hdfs_principal_name,
          )
          environment = cache.environment
        for directory in (external_dir, managed_dir):
          Execute(
            (
              "hdfs",
              "dfs",
              "-setfacl",
              "-m",
              f"default:user:{params.hive_user}:rwx",
              directory,
            ),
            user=params.hdfs_user,
            environment=environment,
            path=params.execute_path,
          )
    else:
      Logger.info(
        format(
          "Could not set default ACLs for HDFS directories {external_dir} and "
          "{managed_dir} because ACL inheritance is not enabled"
        )
      )
  else:
    Logger.info(
      format(
        "Not creating warehouse directory '{hive_metastore_warehouse_dir}' "
        "because the location is not in DFS"
      )
    )

  # Create Tez History dir
  if (
    not params.whs_dir_protocol
    or params.whs_dir_protocol == urlparse(params.default_fs).scheme
  ):
    if not is_empty(params.tez_hook_proto_base_directory):
      params.HdfsResource(
        params.tez_hook_proto_base_directory,
        type="directory",
        action="create_on_execute",
        owner=params.hive_user,
        mode=0o1775,
      )

  params.HdfsResource(None, action="execute")


def __is_hdfs_acls_enabled():
  import params

  hdfs_protocol = params.fs_root.startswith("hdfs://")

  return_code, stdout, _ = get_user_call_output(
    "hdfs getconf -confKey dfs.namenode.acls.enabled", user=params.hdfs_user
  )
  acls_enabled = stdout == "true"
  return_code, stdout, _ = get_user_call_output(
    "hdfs getconf -confKey dfs.namenode.posix.acl.inheritance.enabled",
    user=params.hdfs_user,
  )
  acls_inheritance_enabled = stdout == "true"

  return hdfs_protocol and acls_enabled and acls_inheritance_enabled


def setup_non_client():
  import params

  Directory(
    params.hive_pid_dir,
    create_parents=True,
    owner=params.hive_user,
    group=params.user_group,
    mode=0o2750,
  )
  Directory(
    params.hive_log_dir,
    create_parents=True,
    owner=params.hive_user,
    group=params.user_group,
    mode=0o750,
  )
  Directory(
    params.hive_var_lib,
    create_parents=True,
    owner=params.hive_user,
    group=params.user_group,
    mode=0o750,
  )


def setup_metastore():
  import params

  if params.hive_metastore_site_supported:
    hivemetastore_site_config = get_config("hivemetastore-site")
    if hivemetastore_site_config:
      XmlConfig(
        "hivemetastore-site.xml",
        conf_dir=params.hive_conf_dir,
        configurations=params.config["configurations"]["hivemetastore-site"],
        configuration_attributes=params.config["configurationAttributes"][
          "hivemetastore-site"
        ],
        owner="root",
        group=params.user_group,
        mode=0o640,
      )
  File(
    os.path.join(params.hive_conf_dir, "hadoop-metrics2-hivemetastore.properties"),
    owner="root",
    group=params.user_group,
    content=Template("hadoop-metrics2-hivemetastore.properties.j2"),
    mode=0o640,
  )

  File(
    params.start_metastore_path,
    owner="root",
    group=params.user_group,
    mode=0o750,
    content=StaticFile("startMetastore.sh"),
  )

  if (
    params.hive_repl_cmrootdir is not None and params.hive_repl_cmrootdir.strip() != ""
  ):
    params.HdfsResource(
      params.hive_repl_cmrootdir,
      type="directory",
      action="create_on_execute",
      owner=params.hive_user,
      group=params.user_group,
      mode=0o1777,
    )
  if params.hive_repl_rootdir is not None and params.hive_repl_rootdir.strip() != "":
    params.HdfsResource(
      params.hive_repl_rootdir,
      type="directory",
      action="create_on_execute",
      owner=params.hive_user,
      group=params.user_group,
      mode=0o700,
    )
  params.HdfsResource(None, action="execute")

  generate_logfeeder_input_config(
    "hive", Template("input.config-hive.json.j2", extra_imports=[default])
  )


def refresh_yarn():
  import params

  if params.enable_ranger_hive or not params.doAs:
    return

  YARN_REFRESHED_FILE = "/etc/hive/yarn.refreshed"

  if os.path.isfile(YARN_REFRESHED_FILE):
    Logger.info("Yarn already refreshed")
    return

  cache_context = nullcontext(None)
  if params.security_enabled:
    cache_context = PrivateKerberosCache(
      params.yarn_user,
      params.user_group,
      params.tmp_dir,
      "ambari-hive-yarn-refresh-",
    )
  with cache_context as cache:
    environment = None
    if cache is not None:
      cache.kinit(
        params.kinit_path_local,
        params.yarn_keytab,
        params.yarn_principal_name,
      )
      environment = cache.environment
    Execute(
      ("yarn", "rmadmin", "-refreshSuperUserGroupsConfiguration"),
      user=params.yarn_user,
      environment=environment,
      path=params.execute_path,
    )
  File(YARN_REFRESHED_FILE, owner="root", group="root", mode=0o644)


def create_hive_metastore_schema():
  import params

  SYS_DB_CREATED_FILE = "/etc/hive/sys.db.created"

  if os.path.isfile(SYS_DB_CREATED_FILE):
    File(SYS_DB_CREATED_FILE, action="delete")

  cache_context = _hive_kerberos_cache(params, "ambari-hive-sys-schema-")
  with cache_context as cache:
    environment = _initialize_hive_cache(params, cache)
    info_command = _schema_tool_command(params, "-info", meta_db_type=True)
    return_code, _ = shell.call(
      info_command,
      user=params.hive_user,
      env={"HIVE_CONF_DIR": params.hive_conf_dir, **(environment or {})},
      timeout=120,
      shell=False,
    )
    if return_code != 0:
      Execute(
        _schema_tool_command(params, "-initSchema", meta_db_type=True),
        user=params.hive_user,
        environment={"HIVE_CONF_DIR": params.hive_conf_dir, **(environment or {})},
        timeout=300,
      )

  File(SYS_DB_CREATED_FILE, owner="root", group="root", mode=0o644)
  Logger.info("Sys DB is set up")


def create_metastore_schema():
  import params

  if params.sysprep_skip_hive_schema_create:
    Logger.info("Skipping creation of Hive Metastore schema as host is sys prepped")
    return

  cache_context = _hive_kerberos_cache(params, "ambari-hive-metastore-schema-")
  with cache_context as cache:
    environment = _initialize_hive_cache(params, cache)
    command_environment = {
      "HIVE_CONF_DIR": params.hive_conf_dir,
      **(environment or {}),
    }
    return_code, _ = shell.call(
      _schema_tool_command(params, "-info"),
      user=params.hive_user,
      env=command_environment,
      timeout=120,
      shell=False,
    )
    if return_code != 0:
      Execute(
        _schema_tool_command(params, "-initSchema"),
        user=params.hive_user,
        environment=command_environment,
        timeout=300,
      )


def _hive_kerberos_cache(params, prefix):
  if not params.security_enabled:
    return nullcontext(None)
  return PrivateKerberosCache(
    params.hive_user,
    params.user_group,
    params.tmp_dir,
    prefix,
  )


def _initialize_hive_cache(params, cache):
  if cache is None:
    return None
  cache.kinit(
    params.kinit_path_local,
    params.hive_metastore_keytab_path,
    params.hive_metastore_principal_with_host,
  )
  return cache.environment


def _schema_tool_command(params, action, meta_db_type=False):
  command = [
    os.path.join(params.hive_bin_dir, "schematool"),
    action,
    "-dbType",
    "hive" if meta_db_type else params.hive_metastore_db_type,
  ]
  if meta_db_type:
    command.extend(("-metaDbType", params.hive_metastore_db_type))
  command.extend(
    (
      "-userName",
      params.hive_metastore_user_name,
      "-passWord",
      PasswordString(params.hive_metastore_user_passwd),
      "-verbose",
    )
  )
  return tuple(command)


"""
Writes configuration files required by Hive.
"""


def fill_conf_dir(component_conf_dir):
  import params

  component_conf_dir = os.path.realpath(component_conf_dir)
  mode_identified_for_file = 0o644
  mode_identified_for_dir = 0o755

  Directory(
    component_conf_dir,
    owner="root",
    group=params.user_group,
    create_parents=True,
    mode=mode_identified_for_dir,
  )

  XmlConfig(
    "mapred-site.xml",
    conf_dir=component_conf_dir,
    configurations=params.config["configurations"]["mapred-site"],
    configuration_attributes=params.config["configurationAttributes"]["mapred-site"],
    owner="root",
    group=params.user_group,
    mode=mode_identified_for_file,
  )

  hive_log4j2_filename = "hive-log4j2.properties"
  File(
    format("{component_conf_dir}/{hive_log4j2_filename}"),
    mode=mode_identified_for_file,
    group=params.user_group,
    owner="root",
    content=InlineTemplate(params.hive_log4j2),
  )

  hive_exec_log4j2_filename = "hive-exec-log4j2.properties"
  File(
    format("{component_conf_dir}/{hive_exec_log4j2_filename}"),
    mode=mode_identified_for_file,
    group=params.user_group,
    owner="root",
    content=InlineTemplate(params.hive_exec_log4j2),
  )

  beeline_log4j2_filename = "beeline-log4j2.properties"
  File(
    format("{component_conf_dir}/{beeline_log4j2_filename}"),
    mode=mode_identified_for_file,
    group=params.user_group,
    owner="root",
    content=InlineTemplate(params.beeline_log4j2),
  )

  XmlConfig(
    "beeline-site.xml",
    conf_dir=component_conf_dir,
    configurations=params.beeline_site_config,
    owner="root",
    group=params.user_group,
    mode=mode_identified_for_file,
  )

  if params.parquet_logging_properties is not None:
    File(
      format("{component_conf_dir}/parquet-logging.properties"),
      mode=mode_identified_for_file,
      group=params.user_group,
      owner="root",
      content=params.parquet_logging_properties,
    )


def jdbc_connector(target, hive_previous_jdbc_jar):
  """
  Install the JDBC driver used by HiveServer2 and the Hive Metastore.
  """
  import params

  if not params.jdbc_jar_name:
    return

  if hive_previous_jdbc_jar and os.path.isfile(hive_previous_jdbc_jar):
    File(hive_previous_jdbc_jar, action="delete")

  if params.using_system_mariadb_driver:
    Execute(
      ("cp", "--remove-destination", params.mariadb_jdbc_driver_jar, target),
      path=["/bin", "/usr/bin/"],
      sudo=True,
    )
  else:
    File(
      params.downloaded_custom_connector,
      content=DownloadSource(params.driver_curl_source),
      owner="root",
      group="root",
      mode=0o600,
    )
    Execute(
      ("cp", "--remove-destination", params.downloaded_custom_connector, target),
      path=["/bin", "/usr/bin/"],
      sudo=True,
    )

  File(
    target,
    owner="root",
    group="root",
    mode=0o644,
  )
