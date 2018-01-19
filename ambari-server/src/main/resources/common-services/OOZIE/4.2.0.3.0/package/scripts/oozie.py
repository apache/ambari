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
import re

# Resource Management Imports
from resource_management.core.resources.service import ServiceConfig
from resource_management.core.resources.system import Directory, Execute, File
from resource_management.core.source import DownloadSource
from resource_management.core.source import InlineTemplate
from resource_management.core.source import Template
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.oozie_prepare_war import prepare_war
from resource_management.libraries.functions.copy_tarball import get_current_version
from resource_management.libraries.resources.xml_config import XmlConfig
from resource_management.libraries.functions.lzo_utils import install_lzo_if_needed
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.security_commons import update_credential_provider_path
from resource_management.core.resources.packaging import Package
from resource_management.core.shell import as_user, as_sudo, call, checked_call
from resource_management.core.exceptions import Fail

from resource_management.libraries.functions.setup_atlas_hook import has_atlas_in_cluster, setup_atlas_hook
from ambari_commons.constants import SERVICE, UPGRADE_TYPE_NON_ROLLING, UPGRADE_TYPE_ROLLING
from resource_management.libraries.functions.constants import Direction

from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons import OSConst
from ambari_commons.inet_utils import download_file

from resource_management.core import Logger

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
            create_parents = True,
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
             create_parents = True,
             owner = params.oozie_user,
             group = params.user_group
  )

  params.oozie_site = update_credential_provider_path(params.oozie_site,
                                                      'oozie-site',
                                                      os.path.join(params.conf_dir, 'oozie-site.jceks'),
                                                      params.oozie_user,
                                                      params.user_group
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
    content=InlineTemplate(params.oozie_env_sh_template),
    group=params.user_group,
  )

  # On some OS this folder could be not exists, so we will create it before pushing there files
  Directory(params.limits_conf_dir,
            create_parents=True,
            owner='root',
            group='root'
  )

  File(os.path.join(params.limits_conf_dir, 'oozie.conf'),
       owner='root',
       group='root',
       mode=0644,
       content=Template("oozie.conf.j2")
  )

  if (params.log4j_props != None):
    File(format("{params.conf_dir}/oozie-log4j.properties"),
      mode=0644,
      group=params.user_group,
      owner=params.oozie_user,
      content=InlineTemplate(params.log4j_props)
    )
  elif (os.path.exists(format("{params.conf_dir}/oozie-log4j.properties"))):
    File(format("{params.conf_dir}/oozie-log4j.properties"),
      mode=0644,
      group=params.user_group,
      owner=params.oozie_user
    )

  if params.stack_version_formatted and check_stack_feature(StackFeature.OOZIE_ADMIN_USER, params.stack_version_formatted):
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
  
  if params.lzo_enabled:
    install_lzo_if_needed()
    Execute(format('{sudo} cp {hadoop_lib_home}/hadoop-lzo*.jar {oozie_lib_dir}'),
    )
  
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
    create_parents = True,
    cd_access="a",
  )
  
  Directory(params.oozie_libext_dir,
            create_parents = True,
  )
  
  hashcode_file = format("{oozie_home}/.hashcode")
  skip_recreate_sharelib = format("test -f {hashcode_file} && test -d {oozie_home}/share")

  untar_sharelib = ('tar','-xvf',format('{oozie_home}/oozie-sharelib.tar.gz'),'-C',params.oozie_home)

  Execute( untar_sharelib,    # time-expensive
    not_if  = format("{no_op_test} || {skip_recreate_sharelib}"), 
    sudo = True,
  )

  configure_cmds = []
  configure_cmds.append(('cp', params.ext_js_path, params.oozie_libext_dir))
  configure_cmds.append(('chown', format('{oozie_user}:{user_group}'), format('{oozie_libext_dir}/{ext_js_file}')))
  
  Execute( configure_cmds,
    not_if  = no_op_test,
    sudo = True,
  )
  
  Directory(params.oozie_webapps_conf_dir,
            owner = params.oozie_user,
            group = params.user_group,
            recursive_ownership = True,
            recursion_follow_links = True,
  )

  # download the database JAR
  download_database_library_if_needed()

  #falcon el extension
  if params.has_falcon_host:
    Execute(format('{sudo} cp {falcon_home}/oozie/ext/falcon-oozie-el-extension-*.jar {oozie_libext_dir}'),
      not_if  = no_op_test)

    Execute(format('{sudo} chown {oozie_user}:{user_group} {oozie_libext_dir}/falcon-oozie-el-extension-*.jar'),
      not_if  = no_op_test)

  prepare_war(params)

  File(hashcode_file,
       mode = 0644,
  )

  if params.stack_version_formatted and check_stack_feature(StackFeature.OOZIE_CREATE_HIVE_TEZ_CONFIGS, params.stack_version_formatted):
    # Create hive-site and tez-site configs for oozie
    Directory(params.hive_conf_dir,
        create_parents = True,
        owner = params.oozie_user,
        group = params.user_group
    )
    if 'hive-site' in params.config['configurations']:
      hive_site_config = update_credential_provider_path(params.config['configurations']['hive-site'],
                                                         'hive-site',
                                                         os.path.join(params.hive_conf_dir, 'hive-site.jceks'),
                                                         params.oozie_user,
                                                         params.user_group
                                                         )
      XmlConfig("hive-site.xml",
        conf_dir=params.hive_conf_dir,
        configurations=hive_site_config,
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

    # If Atlas is also installed, need to generate Atlas Hive hook (hive-atlas-application.properties file) in directory
    # {stack_root}/{current_version}/atlas/hook/hive/
    # Because this is a .properties file instead of an xml file, it will not be read automatically by Oozie.
    # However, should still save the file on this host so that can upload it to the Oozie Sharelib in DFS.
    if has_atlas_in_cluster():
      atlas_hook_filepath = os.path.join(params.hive_conf_dir, params.atlas_hook_filename)
      Logger.info("Has atlas in cluster, will save Atlas Hive hook into location %s" % str(atlas_hook_filepath))
      setup_atlas_hook(SERVICE.HIVE, params.hive_atlas_application_properties, atlas_hook_filepath, params.oozie_user, params.user_group)

  Directory(params.oozie_server_dir,
    owner = params.oozie_user,
    group = params.user_group,
    recursive_ownership = True,  
  )
  if params.security_enabled:
    File(os.path.join(params.conf_dir, 'zkmigrator_jaas.conf'),
         owner=params.oozie_user,
         group=params.user_group,
         content=Template("zkmigrator_jaas.conf.j2")
         )

def __parse_sharelib_from_output(output):
  """
  Return the parent directory of the first path from the output of the "oozie admin -shareliblist command $comp"
  Output will match pattern like:

  Potential errors
  [Available ShareLib]
  hive
    hdfs://server:8020/user/oozie/share/lib/lib_20160811235630/hive/file1.jar
    hdfs://server:8020/user/oozie/share/lib/lib_20160811235630/hive/file2.jar
  """
  if output is not None:
    pattern = re.compile(r"\[Available ShareLib\]\n\S*?\n(.*share.*)", re.IGNORECASE)
    m = pattern.search(output)
    if m and len(m.groups()) == 1:
      jar_path = m.group(1)
      # Remove leading/trailing spaces and get the containing directory
      sharelib_dir = os.path.dirname(jar_path.strip())
      return sharelib_dir
  return None

def copy_atlas_hive_hook_to_dfs_share_lib(upgrade_type=None, upgrade_direction=None):
  """
   If the Atlas Hive Hook direcotry is present, Atlas is installed, and this is the first Oozie Server,
  then copy the entire contents of that directory to the Oozie Sharelib in DFS, e.g.,
  /usr/$stack/$current_version/atlas/hook/hive/ -> hdfs:///user/oozie/share/lib/lib_$timetamp/hive

  :param upgrade_type: If in the middle of a stack upgrade, the type as UPGRADE_TYPE_ROLLING or UPGRADE_TYPE_NON_ROLLING
  :param upgrade_direction: If in the middle of a stack upgrade, the direction as Direction.UPGRADE or Direction.DOWNGRADE.
  """
  import params

  # Calculate the effective version since this code can also be called during EU/RU in the upgrade direction.
  effective_version = params.stack_version_formatted if upgrade_type is None else format_stack_version(params.version)
  if not check_stack_feature(StackFeature.ATLAS_HOOK_SUPPORT, effective_version):
    return
    
  # Important that oozie_server_hostnames is sorted by name so that this only runs on a single Oozie server.
  if not (len(params.oozie_server_hostnames) > 0 and params.hostname == params.oozie_server_hostnames[0]):
    Logger.debug("Will not attempt to copy Atlas Hive hook to DFS since this is not the first Oozie Server "
                 "sorted by hostname.")
    return

  if not has_atlas_in_cluster():
    Logger.debug("Will not attempt to copy Atlas Hve hook to DFS since Atlas is not installed on the cluster.")
    return

  if upgrade_type is not None and upgrade_direction == Direction.DOWNGRADE:
    Logger.debug("Will not attempt to copy Atlas Hve hook to DFS since in the middle of Rolling/Express upgrade "
                 "and performing a Downgrade.")
    return

  current_version = get_current_version()
  atlas_hive_hook_dir = format("{stack_root}/{current_version}/atlas/hook/hive/")
  if not os.path.exists(atlas_hive_hook_dir):
    Logger.error(format("ERROR. Atlas is installed in cluster but this Oozie server doesn't "
                        "contain directory {atlas_hive_hook_dir}"))
    return

  atlas_hive_hook_impl_dir = os.path.join(atlas_hive_hook_dir, "atlas-hive-plugin-impl")

  num_files = len([name for name in os.listdir(atlas_hive_hook_impl_dir) if os.path.exists(os.path.join(atlas_hive_hook_impl_dir, name))])
  Logger.info("Found %d files/directories inside Atlas Hive hook impl directory %s"% (num_files, atlas_hive_hook_impl_dir))

  # This can return over 100 files, so take the first 5 lines after "Available ShareLib"
  # Use -oozie http(s):localhost:{oozie_server_admin_port}/oozie as oozie-env does not export OOZIE_URL
  command = format(r'source {conf_dir}/oozie-env.sh ; oozie admin -oozie {oozie_base_url} -shareliblist hive | grep "\[Available ShareLib\]" -A 5')
  code, out = checked_call(command, user=params.oozie_user, tries=10, try_sleep=5, logoutput=True)

  hive_sharelib_dir = __parse_sharelib_from_output(out)

  if hive_sharelib_dir is None:
    raise Fail("Could not parse Hive sharelib from output.")

  Logger.info(format("Parsed Hive sharelib = {hive_sharelib_dir} and will attempt to copy/replace {num_files} files to it from {atlas_hive_hook_impl_dir}"))

  params.HdfsResource(hive_sharelib_dir,
                      type="directory",
                      action="create_on_execute",
                      source=atlas_hive_hook_impl_dir,
                      user=params.hdfs_user,
                      owner=params.oozie_user,
                      group=params.hdfs_user,
                      mode=0755,
                      recursive_chown=True,
                      recursive_chmod=True,
                      replace_existing_files=True
                      )

  Logger.info("Copying Atlas Hive hook properties file to Oozie Sharelib in DFS.")
  atlas_hook_filepath_source = os.path.join(params.hive_conf_dir, params.atlas_hook_filename)
  atlas_hook_file_path_dest_in_dfs = os.path.join(hive_sharelib_dir, params.atlas_hook_filename)
  params.HdfsResource(atlas_hook_file_path_dest_in_dfs,
                      type="file",
                      source=atlas_hook_filepath_source,
                      action="create_on_execute",
                      owner=params.oozie_user,
                      group=params.hdfs_user,
                      mode=0755,
                      replace_existing_files=True
                      )
  params.HdfsResource(None, action="execute")

  # Update the sharelib after making any changes
  # Use -oozie http(s):localhost:{oozie_server_admin_port}/oozie as oozie-env does not export OOZIE_URL
  Execute(format("source {conf_dir}/oozie-env.sh ; oozie admin -oozie {oozie_base_url} -sharelibupdate"),
          user=params.oozie_user,
          tries=5,
          try_sleep=5,
          logoutput=True,
  )


def download_database_library_if_needed(target_directory = None):
  """
  Downloads the library to use when connecting to the Oozie database, if
  necessary. The library will be downloaded to 'params.target' unless
  otherwise specified.
  :param target_directory: the location where the database library will be
  downloaded to.
  :return:
  """
  import params
  jdbc_drivers = ["com.mysql.jdbc.Driver",
    "com.microsoft.sqlserver.jdbc.SQLServerDriver",
    "oracle.jdbc.driver.OracleDriver","sap.jdbc4.sqlanywhere.IDriver"]

  # check to see if the JDBC driver name is in the list of ones that need to
  # be downloaded
  if params.jdbc_driver_name not in jdbc_drivers or not params.jdbc_driver_jar:
    return

  if params.previous_jdbc_jar and os.path.isfile(params.previous_jdbc_jar):
    File(params.previous_jdbc_jar, action='delete')

  # if the target directory is not specified
  if target_directory is None:
    target_jar_with_directory = params.target
  else:
    # create the full path using the supplied target directory and the JDBC JAR
    target_jar_with_directory = target_directory + os.path.sep + params.jdbc_driver_jar

  if not os.path.exists(target_jar_with_directory):
    File(params.downloaded_custom_connector,
      content = DownloadSource(params.driver_curl_source))

    if params.sqla_db_used:
      untar_sqla_type2_driver = ('tar', '-xvf', params.downloaded_custom_connector, '-C', params.tmp_dir)

      Execute(untar_sqla_type2_driver, sudo = True)

      Execute(format("yes | {sudo} cp {jars_path_in_archive} {oozie_libext_dir}"))

      Directory(params.jdbc_libs_dir,
                create_parents = True)

      Execute(format("yes | {sudo} cp {libs_path_in_archive} {jdbc_libs_dir}"))

      Execute(format("{sudo} chown -R {oozie_user}:{user_group} {oozie_libext_dir}/*"))

    else:
      Execute(('cp', '--remove-destination', params.downloaded_custom_connector, target_jar_with_directory),
        path=["/bin", "/usr/bin/"],
        sudo = True)

    File(target_jar_with_directory, owner = params.oozie_user,
      group = params.user_group)
