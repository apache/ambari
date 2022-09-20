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

import glob
import os

from resource_management.core import shell, sudo
from resource_management.core.logger import Logger
from resource_management.core.resources import Directory
from resource_management.core.resources.system import Execute, File
from resource_management.core.source import Template, InlineTemplate
from resource_management.libraries import XmlConfig
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.check_process_status import \
  check_process_status
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.generate_logfeeder_input_config import generate_logfeeder_input_config
from resource_management.libraries.functions.stack_features import \
  check_stack_feature
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.script.script import Script


class Master(Script):
  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)

    self.create_zeppelin_log_dir(env)

    if params.spark_version:
      Execute('echo spark_version:' + str(params.spark_version) + ' detected for spark_home: '
              + params.spark_home + ' >> ' + params.zeppelin_log_file, user=params.zeppelin_user)
    if params.spark2_version:
      Execute('echo spark2_version:' + str(params.spark2_version) + ' detected for spark2_home: '
              + params.spark2_home + ' >> ' + params.zeppelin_log_file, user=params.zeppelin_user)

  def create_zeppelin_dir(self, params):
    params.HdfsResource(format("/user/{zeppelin_user}"),
                        type="directory",
                        action="create_on_execute",
                        owner=params.zeppelin_user,
                        recursive_chown=True,
                        recursive_chmod=True
                        )
    params.HdfsResource(format("/user/{zeppelin_user}/test"),
                        type="directory",
                        action="create_on_execute",
                        owner=params.zeppelin_user,
                        recursive_chown=True,
                        recursive_chmod=True
                        )
    params.HdfsResource(format("/apps/zeppelin"),
                        type="directory",
                        action="create_on_execute",
                        owner=params.zeppelin_user,
                        recursive_chown=True,
                        recursive_chmod=True
                        )

    spark_deps_full_path = self.get_zeppelin_spark_dependencies()[0]
    spark_dep_file_name = os.path.basename(spark_deps_full_path)

    params.HdfsResource(None, action="execute")

  def create_zeppelin_log_dir(self, env):
    import params
    env.set_params(params)
    Directory([params.zeppelin_log_dir],
              owner=params.zeppelin_user,
              group=params.zeppelin_group,
              cd_access="a",
              create_parents=True,
              mode=0755
              )

  def create_zeppelin_hdfs_conf_dir(self, env):
    import params
    env.set_params(params)
    Directory([params.external_dependency_conf],
              owner=params.zeppelin_user,
              group=params.zeppelin_group,
              cd_access="a",
              create_parents=True,
              mode=0755
              )

  def chown_zeppelin_pid_dir(self, env):
    import params
    env.set_params(params)
    Execute(("chown", "-R", format("{zeppelin_user}") + ":" + format("{zeppelin_group}"), params.zeppelin_pid_dir),
            sudo=True)

  def configure(self, env):
    import params
    import status_params
    env.set_params(params)
    env.set_params(status_params)
    self.create_zeppelin_log_dir(env)

    # create the pid and zeppelin dirs
    Directory([params.zeppelin_pid_dir, params.zeppelin_dir],
              owner=params.zeppelin_user,
              group=params.zeppelin_group,
              cd_access="a",
              create_parents=True,
              mode=0755
    )
    self.chown_zeppelin_pid_dir(env)

    XmlConfig("zeppelin-site.xml",
              conf_dir=params.conf_dir,
              configurations=params.config['configurations']['zeppelin-site'],
              owner=params.zeppelin_user,
              group=params.zeppelin_group
              )
    # write out zeppelin-env.sh
    env_content = InlineTemplate(params.zeppelin_env_content)
    File(format("{params.conf_dir}/zeppelin-env.sh"), content=env_content,
         owner=params.zeppelin_user, group=params.zeppelin_group)

    # write out shiro.ini
    shiro_ini_content = InlineTemplate(params.shiro_ini_content)
    File(format("{params.conf_dir}/shiro.ini"), content=shiro_ini_content,
         owner=params.zeppelin_user, group=params.zeppelin_group)

    # write out log4j.properties
    File(format("{params.conf_dir}/log4j.properties"), content=params.log4j_properties_content,
         owner=params.zeppelin_user, group=params.zeppelin_group)

    self.create_zeppelin_hdfs_conf_dir(env)

    generate_logfeeder_input_config('zeppelin', Template("input.config-zeppelin.json.j2", extra_imports=[default]))

    if len(params.hbase_master_hosts) > 0 and params.is_hbase_installed:
      # copy hbase-site.xml
      XmlConfig("hbase-site.xml",
              conf_dir=params.external_dependency_conf,
              configurations=params.config['configurations']['hbase-site'],
              configuration_attributes=params.config['configurationAttributes']['hbase-site'],
              owner=params.zeppelin_user,
              group=params.zeppelin_group,
              mode=0644)

      XmlConfig("hdfs-site.xml",
                conf_dir=params.external_dependency_conf,
                configurations=params.config['configurations']['hdfs-site'],
                configuration_attributes=params.config['configurationAttributes']['hdfs-site'],
                owner=params.zeppelin_user,
                group=params.zeppelin_group,
                mode=0644)

      XmlConfig("core-site.xml",
                conf_dir=params.external_dependency_conf,
                configurations=params.config['configurations']['core-site'],
                configuration_attributes=params.config['configurationAttributes']['core-site'],
                owner=params.zeppelin_user,
                group=params.zeppelin_group,
                mode=0644,
                xml_include_file=params.mount_table_xml_inclusion_file_full_path)

      if params.mount_table_content:
        File(params.mount_table_xml_inclusion_file_full_path,
             owner=params.zeppelin_user,
             group=params.zeppelin_group,
             content=params.mount_table_content,
             mode=0644
        )

  def check_and_copy_notebook_in_hdfs(self, params):
    notebook_dir = params.zeppelin_notebook_dir
    if notebook_dir.startswith("/") or '://' in notebook_dir:
      notebook_directory = notebook_dir
    else:
      notebook_directory = "/user/" + format("{zeppelin_user}") + "/" + notebook_dir

    if not self.is_directory_exists_in_HDFS(notebook_directory, params.zeppelin_user):
      params.HdfsResource(format("{notebook_directory}"),
                          type="directory",
                          action="create_on_execute",
                          owner=params.zeppelin_user,
                          recursive_chown=True,
                          recursive_chmod=True
                          )

      params.HdfsResource(format("{notebook_directory}"),
                          type="directory",
                          action="create_on_execute",
                          source=params.local_notebook_dir,
                          owner=params.zeppelin_user,
                          recursive_chown=True,
                          recursive_chmod=True
                          )


  def stop(self, env, upgrade_type=None):
    import params
    self.create_zeppelin_log_dir(env)
    self.chown_zeppelin_pid_dir(env)
    Execute(params.zeppelin_dir + '/bin/zeppelin-daemon.sh stop >> ' + params.zeppelin_log_file,
            user=params.zeppelin_user)

  def start(self, env, upgrade_type=None):
    import params
    import status_params
    self.configure(env)

    Execute(("chown", "-R", format("{zeppelin_user}") + ":" + format("{zeppelin_group}"), "/etc/zeppelin"),
            sudo=True)
    Execute(("chown", "-R", format("{zeppelin_user}") + ":" + format("{zeppelin_group}"), format("{local_notebook_dir}")), sudo=True)

    if params.security_enabled:
      zeppelin_kinit_cmd = format("{kinit_path_local} -kt {zeppelin_kerberos_keytab} {zeppelin_kerberos_principal}; ")
      Execute(zeppelin_kinit_cmd, user=params.zeppelin_user)

    if 'zeppelin.notebook.storage' in params.config['configurations']['zeppelin-site'] \
        and params.config['configurations']['zeppelin-site']['zeppelin.notebook.storage'] == 'org.apache.zeppelin.notebook.repo.FileSystemNotebookRepo':
      self.check_and_copy_notebook_in_hdfs(params)

    zeppelin_spark_dependencies = self.get_zeppelin_spark_dependencies()
    if zeppelin_spark_dependencies and os.path.exists(zeppelin_spark_dependencies[0]):
      self.create_zeppelin_dir(params)

    if params.conf_stored_in_hdfs:
      if not self.is_directory_exists_in_HDFS(self.get_zeppelin_conf_FS_directory(params), params.zeppelin_user):
        # hdfs dfs -mkdir {zeppelin's conf directory}
        params.HdfsResource(self.get_zeppelin_conf_FS_directory(params),
                            type="directory",
                            action="create_on_execute",
                            owner=params.zeppelin_user,
                            recursive_chown=True,
                            recursive_chmod=True
                            )

    # if first_setup:
    if not glob.glob(params.conf_dir + "/interpreter.json") and \
      not os.path.exists(params.conf_dir + "/interpreter.json"):
      self.create_interpreter_json()

    if params.zeppelin_interpreter_config_upgrade == True:
      self.reset_interpreter_settings(upgrade_type)
      self.update_zeppelin_interpreter()

    Execute(params.zeppelin_dir + '/bin/zeppelin-daemon.sh restart >> '
            + params.zeppelin_log_file, user=params.zeppelin_user)
    pidfile = glob.glob(os.path.join(status_params.zeppelin_pid_dir,
                                     'zeppelin-' + params.zeppelin_user + '*.pid'))[0]
    Logger.info(format("Pid file is: {pidfile}"))

  def status(self, env):
    import status_params
    env.set_params(status_params)

    try:
        pid_file = glob.glob(status_params.zeppelin_pid_dir + '/zeppelin-' +
                             status_params.zeppelin_user + '*.pid')[0]
    except IndexError:
        pid_file = ''
    check_process_status(pid_file)

  def reset_interpreter_settings(self, upgrade_type):
    import json
    import interpreter_json_template
    interpreter_json_template = json.loads(interpreter_json_template.template)['interpreterSettings']
    config_data = self.get_interpreter_settings()
    interpreter_settings = config_data['interpreterSettings']

    if upgrade_type is not None:
      current_interpreters_keys = interpreter_settings.keys()
      for key in current_interpreters_keys:
        interpreter_data = interpreter_settings[key]
        if interpreter_data["name"] == "sh" and interpreter_data["group"] == "sh":
          del interpreter_settings[key]

    for setting_key in interpreter_json_template.keys():
      if setting_key not in interpreter_settings:
        interpreter_settings[setting_key] = interpreter_json_template[
          setting_key]
      else:
        templateGroups = interpreter_json_template[setting_key]['interpreterGroup']
        groups = interpreter_settings[setting_key]['interpreterGroup']

        templateProperties = interpreter_json_template[setting_key]['properties']
        properties = interpreter_settings[setting_key]['properties']

        templateOptions = interpreter_json_template[setting_key]['option']
        options = interpreter_settings[setting_key]['option']

        # search for difference in groups from current interpreter and template interpreter
        # if any group exists in template but doesn't exist in current interpreter, it will be added
        group_names = []
        for group in groups:
          group_names.append(group['name'])

        for template_group in templateGroups:
          if not template_group['name'] in group_names:
            groups.append(template_group)


        # search for difference in properties from current interpreter and template interpreter
        # if any property exists in template but doesn't exist in current interpreter, it will be added
        for template_property in templateProperties:
          if not template_property in properties:
            properties[template_property] = templateProperties[template_property]


        # search for difference in options from current interpreter and template interpreter
        # if any option exists in template but doesn't exist in current interpreter, it will be added
        for template_option in templateOptions:
          if not template_option in options:
            options[template_option] = templateOptions[template_option]

    self.set_interpreter_settings(config_data)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, format_stack_version(params.version)):
      stack_select.select_packages(params.version)

  def get_zeppelin_conf_FS_directory(self, params):
    hdfs_interpreter_config = params.config['configurations']['zeppelin-site']['zeppelin.config.fs.dir']

    # if it doesn't start from "/" or doesn't contains "://" as in hdfs://, file://, etc then make it a absolute path
    if not (hdfs_interpreter_config.startswith("/") or '://' in hdfs_interpreter_config):
      hdfs_interpreter_config = "/user/" + format("{zeppelin_user}") + "/" + hdfs_interpreter_config

    return hdfs_interpreter_config

  def get_zeppelin_conf_FS(self, params):
    return self.get_zeppelin_conf_FS_directory(params) + "/interpreter.json"

  def is_directory_exists_in_HDFS(self, path, as_user):
    import params
    kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
    if params.security_enabled:
      kinit_if_needed = format("{kinit_path_local} -kt {zeppelin_kerberos_keytab} {zeppelin_kerberos_principal};")
    else:
      kinit_if_needed = ''
    #-d: if the path is a directory, return 0.
    path_exists = shell.call(format("{kinit_if_needed} hdfs --config {hadoop_conf_dir} dfs -test -d {path};echo $?"),
                             user=as_user)[1]

    # if there is no kerberos setup then the string will contain "-bash: kinit: command not found"
    if "\n" in path_exists:
      path_exists = path_exists.split("\n").pop()

    # '1' means it does not exists
    if path_exists == '0':
      return True
    else:
      return False

  def is_file_exists_in_HDFS(self, path, as_user):
    import params
    kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
    if params.security_enabled:
      kinit_if_needed = format("{kinit_path_local} -kt {zeppelin_kerberos_keytab} {zeppelin_kerberos_principal};")
    else:
      kinit_if_needed = ''

    #-f: if the path is a file, return 0.
    path_exists = shell.call(format("{kinit_if_needed} hdfs --config {hadoop_conf_dir} dfs -test -f {path};echo $?"),
                             user=as_user)[1]

    # if there is no kerberos setup then the string will contain "-bash: kinit: command not found"
    if "\n" in path_exists:
      path_exists = path_exists.split("\n").pop()

    # '1' means it does not exists
    if path_exists == '0':
      #-z: if the file is zero length, return 0.
      path_exists = shell.call(format("{kinit_if_needed} hdfs --config {hadoop_conf_dir} dfs -test -z {path};echo $?"),
                               user=as_user)[1]

      if "\n" in path_exists:
        path_exists = path_exists.split("\n").pop()
      if path_exists != '0':
        return True

    return False

  def copy_interpreter_from_HDFS_to_FS(self, params):
    if params.conf_stored_in_hdfs:
      zeppelin_conf_fs = self.get_zeppelin_conf_FS(params)

      if self.is_file_exists_in_HDFS(zeppelin_conf_fs, params.zeppelin_user):
        # copy from hdfs to /etc/zeppelin/conf/interpreter.json
        kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
        if params.security_enabled:
          kinit_if_needed = format("{kinit_path_local} -kt {zeppelin_kerberos_keytab} {zeppelin_kerberos_principal};")
        else:
          kinit_if_needed = ''
        interpreter_config = os.path.join(params.conf_dir, "interpreter.json")
        shell.call(format("rm {interpreter_config};"
            "{kinit_if_needed} hdfs --config {hadoop_conf_dir} dfs -get {zeppelin_conf_fs} {interpreter_config}"),
            user=params.zeppelin_user)
        return True
    return False

  def get_interpreter_settings(self):
    import params
    import json

    self.copy_interpreter_from_HDFS_to_FS(params)
    interpreter_config = os.path.join(params.conf_dir, "interpreter.json")
    config_content = sudo.read_file(interpreter_config)
    config_data = json.loads(config_content)
    return config_data

  def set_interpreter_settings(self, config_data):
    import params
    import json

    interpreter_config = os.path.join(params.conf_dir, "interpreter.json")
    File(interpreter_config,
         group=params.zeppelin_group,
         owner=params.zeppelin_user,
         mode=0644,
         content=json.dumps(config_data, indent=2))

    if params.conf_stored_in_hdfs:
      #delete file from HDFS, as the `replace_existing_files` logic checks length of file which can remain same.
      params.HdfsResource(self.get_zeppelin_conf_FS(params),
                          type="file",
                          action="delete_on_execute")

      #recreate file in HDFS from LocalFS
      params.HdfsResource(self.get_zeppelin_conf_FS(params),
                          type="file",
                          action="create_on_execute",
                          source=interpreter_config,
                          owner=params.zeppelin_user,
                          recursive_chown=True,
                          recursive_chmod=True,
                          replace_existing_files=True)

  def update_kerberos_properties(self):
    import params
    config_data = self.get_interpreter_settings()
    interpreter_settings = config_data['interpreterSettings']
    for interpreter_setting in interpreter_settings:
      interpreter = interpreter_settings[interpreter_setting]
      if interpreter['group'] == 'livy':
        if params.security_enabled and params.zeppelin_kerberos_principal and params.zeppelin_kerberos_keytab:
          self.storePropertyToInterpreter(interpreter, 'zeppelin.livy.principal', 'string', params.zeppelin_kerberos_principal)
          self.storePropertyToInterpreter(interpreter, 'zeppelin.livy.keytab', 'string', params.zeppelin_kerberos_keytab)
        else:
          self.storePropertyToInterpreter(interpreter, 'zeppelin.livy.principal', 'string', "")
          self.storePropertyToInterpreter(interpreter, 'zeppelin.livy.keytab', 'string', "")
      elif interpreter['group'] == 'spark':
        if params.security_enabled and params.zeppelin_kerberos_principal and params.zeppelin_kerberos_keytab:
          self.storePropertyToInterpreter(interpreter, 'spark.yarn.principal', 'string', params.zeppelin_kerberos_principal)
          self.storePropertyToInterpreter(interpreter, 'spark.yarn.keytab', 'string', params.zeppelin_kerberos_keytab)
        else:
          self.storePropertyToInterpreter(interpreter, 'spark.yarn.principal', 'string', "")
          self.storePropertyToInterpreter(interpreter, 'spark.yarn.keytab', 'string', "")
      elif interpreter['group'] == 'jdbc':
        if params.security_enabled and params.zeppelin_kerberos_principal and params.zeppelin_kerberos_keytab:
          self.storePropertyToInterpreter(interpreter, 'zeppelin.jdbc.auth.type', 'string', "KERBEROS")
          self.storePropertyToInterpreter(interpreter, 'zeppelin.jdbc.principal', 'string', params.zeppelin_kerberos_principal)
          self.storePropertyToInterpreter(interpreter, 'zeppelin.jdbc.keytab.location', 'string', params.zeppelin_kerberos_keytab)
          if params.zookeeper_znode_parent \
              and params.hbase_zookeeper_quorum \
              and 'phoenix.url' in interpreter['properties'] \
              and 'value' in interpreter['properties']['phoenix.url'] \
              and params.zookeeper_znode_parent not in interpreter['properties']['phoenix.url']['value']:
            self.storePropertyToInterpreter(interpreter, 'phoenix.url', 'string', "jdbc:phoenix:" + \
                                                                                  params.hbase_zookeeper_quorum + ':' + \
                                                                                  params.zookeeper_znode_parent)
        else:
          self.storePropertyToInterpreter(interpreter, 'zeppelin.jdbc.auth.type', 'string', "SIMPLE")
          self.storePropertyToInterpreter(interpreter, 'zeppelin.jdbc.principal', 'string', "")
          self.storePropertyToInterpreter(interpreter, 'zeppelin.jdbc.keytab.location', 'string', "")

    self.set_interpreter_settings(config_data)

  def update_zeppelin_interpreter(self):
    import params
    config_data = self.get_interpreter_settings()
    interpreter_settings = config_data['interpreterSettings']

    exclude_interpreter_autoconfig_list = []
    exclude_interpreter_property_groups_map = {}

    if params.exclude_interpreter_autoconfig:
      excluded_interpreters = params.exclude_interpreter_autoconfig.strip().split(";")
      for interpreter in excluded_interpreters:
        if interpreter and interpreter.strip():
          splitted_line = interpreter.split('(')
          interpreter_name = splitted_line[0].strip()
          exclude_interpreter_autoconfig_list.append(interpreter_name)
          if len(splitted_line) > 1:
            property_groups_list = splitted_line[1].replace(')','').strip().split(',')
            if len(property_groups_list) > 0 and property_groups_list[0]:
              exclude_interpreter_property_groups_map[interpreter_name] = property_groups_list




    if params.zeppelin_interpreter:
      settings_to_delete = []
      for settings_key, interpreter in interpreter_settings.items():
        if interpreter['group'] not in params.zeppelin_interpreter:
          settings_to_delete.append(settings_key)

      for key in settings_to_delete:
        del interpreter_settings[key]

    hive_interactive_properties_key = 'hive_interactive'
    for setting_key in interpreter_settings.keys():
      interpreter = interpreter_settings[setting_key]
      if interpreter['group'] == 'jdbc' and interpreter['name'] == 'jdbc' and ('jdbc' not in exclude_interpreter_autoconfig_list
                                                               or 'jdbc' in exclude_interpreter_property_groups_map.keys()):
        interpreter['dependencies'] = []
        jdbc_property_groups = []
        if 'jdbc' in exclude_interpreter_property_groups_map.keys():
          jdbc_property_groups = exclude_interpreter_property_groups_map.get('jdbc')
        if not params.hive_server_host and params.hive_server_interactive_hosts:
          hive_interactive_properties_key = 'hive'

        if params.hive_server_host and 'hive-server' not in jdbc_property_groups:
          self.storePropertyToInterpreter(interpreter, 'hive.driver', 'string', 'org.apache.hive.jdbc.HiveDriver')
          self.storePropertyToInterpreter(interpreter, 'hive.user', 'string', 'hive')
          self.storePropertyToInterpreter(interpreter, 'hive.password', 'string', '')
          self.storePropertyToInterpreter(interpreter, 'hive.proxy.user.property', 'string', 'hive.server2.proxy.user')
          if params.hive_server2_support_dynamic_service_discovery:
            self.storePropertyToInterpreter(interpreter, 'hive.url', 'string', 'jdbc:hive2://' + \
                                                 params.hive_zookeeper_quorum + \
                                                 '/;' + 'serviceDiscoveryMode=' + params.discovery_mode + ';zooKeeperNamespace=' + \
                                                 params.hive_zookeeper_namespace)
          else:
            self.storePropertyToInterpreter(interpreter, 'hive.url', 'string', 'jdbc:hive2://' + \
                                                                               params.hive_server_host + \
                                                                               ':' + params.hive_server_port)
          if 'hive.splitQueries' not in interpreter['properties']:
            self.storePropertyToInterpreter(interpreter, "hive.splitQueries", 'string', "true")

        if params.hive_server_interactive_hosts and 'hive-interactive' not in jdbc_property_groups:
          self.storePropertyToInterpreter(interpreter, hive_interactive_properties_key + '.driver', 'string', 'org.apache.hive.jdbc.HiveDriver')
          self.storePropertyToInterpreter(interpreter, hive_interactive_properties_key + '.user', 'string', 'hive')
          self.storePropertyToInterpreter(interpreter, hive_interactive_properties_key + '.password', 'string', '')
          self.storePropertyToInterpreter(interpreter, hive_interactive_properties_key + '.proxy.user.property', 'string', 'hive.server2.proxy.user')
          if params.hive_server2_support_dynamic_service_discovery:
            self.storePropertyToInterpreter(interpreter, hive_interactive_properties_key + '.url', 'string', 'jdbc:hive2://' + \
                                                    params.hive_zookeeper_quorum + \
                                                    '/;' + 'serviceDiscoveryMode=' + params.discovery_mode + ';zooKeeperNamespace=' + \
                                                    params.hive_interactive_zookeeper_namespace)
          else:
            self.storePropertyToInterpreter(interpreter, hive_interactive_properties_key + '.url', 'string', 'jdbc:hive2://' + \
                                                    params.hive_server_interactive_hosts + \
                                                    ':' + params.hive_server_port)
          if hive_interactive_properties_key + '.splitQueries' not in interpreter['properties']:
            self.storePropertyToInterpreter(interpreter, hive_interactive_properties_key + '.splitQueries', 'string', "true")



        if params.spark2_thrift_server_hosts and 'spark2' not in jdbc_property_groups:
          self.storePropertyToInterpreter(interpreter, 'spark2.driver', 'string', 'org.apache.spark-project.org.apache.hive.jdbc.HiveDriver')
          self.storePropertyToInterpreter(interpreter, 'spark2.user', 'string', 'hive')
          self.storePropertyToInterpreter(interpreter, 'spark2.password', 'string', '')
          self.storePropertyToInterpreter(interpreter, 'spark2.proxy.user.property', 'string', 'hive.server2.proxy.user')
          self.storePropertyToInterpreter(interpreter, 'spark2.url', 'string', 'jdbc:hive2://' + \
                          params.spark2_thrift_server_hosts + ':' + params.spark2_hive_thrift_port + '/')

          if params.spark2_hive_principal:
            self.storePropertyToInterpreter(interpreter, 'spark2.url', 'string', ';principal=' + params.spark2_hive_principal, 'add')
          if params.spark2_transport_mode:
            self.storePropertyToInterpreter(interpreter, 'spark2.url', 'string', ';transportMode=' + params.spark2_transport_mode, 'add')
          if params.spark2_http_path:
            self.storePropertyToInterpreter(interpreter, 'spark2.url', 'string', ';httpPath=' + params.spark2_http_path, 'add')
          if params.spark2_ssl:
            self.storePropertyToInterpreter(interpreter, 'spark2.url', 'string', ';ssl=true', 'add')
          if 'spark2.splitQueries' not in interpreter['properties']:
            self.storePropertyToInterpreter(interpreter, 'spark2.splitQueries', 'string', "true")

        if params.zookeeper_znode_parent \
                and params.hbase_zookeeper_quorum and 'hbase' not in jdbc_property_groups:
            self.storePropertyToInterpreter(interpreter, 'phoenix.driver', 'string', 'org.apache.phoenix.jdbc.PhoenixDriver')
            if 'phoenix.hbase.client.retries.number' not in interpreter['properties']:
              self.storePropertyToInterpreter(interpreter, 'phoenix.hbase.client.retries.number', 'string', '1')
            if 'phoenix.phoenix.query.numberFormat' not in interpreter['properties']:
              self.storePropertyToInterpreter(interpreter, 'phoenix.phoenix.query.numberFormat', 'string', '#.#')
            if 'phoenix.user' not in interpreter['properties']:
              self.storePropertyToInterpreter(interpreter, 'phoenix.user', 'string', 'phoenixuser')
            if 'phoenix.password' not in interpreter['properties']:
              self.storePropertyToInterpreter(interpreter, 'phoenix.password', 'string', "")
            self.storePropertyToInterpreter(interpreter, 'phoenix.url', 'string', "jdbc:phoenix:" + \
                                                                                  params.hbase_zookeeper_quorum + ':' + \
                                                                                  params.zookeeper_znode_parent)

            if 'phoenix.splitQueries' not in interpreter['properties']:
              self.storePropertyToInterpreter(interpreter, 'phoenix.splitQueries', 'string', "true")


      elif interpreter['group'] == 'livy' and interpreter['name'] == 'livy2' and 'livy2' not in exclude_interpreter_autoconfig_list:
        # Honor this Zeppelin setting if it exists
        if 'zeppelin.livy.url' in params.config['configurations']['zeppelin-site']:
          interpreter['properties']['zeppelin.livy.url'] = params.config['configurations']['zeppelin-site']['zeppelin.livy.url']
        elif params.livy2_livyserver_host:
          self.storePropertyToInterpreter(interpreter, 'zeppelin.livy.url', 'string', params.livy2_livyserver_protocol + \
                                                                                      "://" + params.livy2_livyserver_host + \
                                                                                      ":" + params.livy2_livyserver_port)
        else:
          del interpreter_settings[setting_key]


      elif interpreter['group'] == 'spark' and interpreter['name'] == 'spark2' and 'spark2' not in exclude_interpreter_autoconfig_list:
        if 'spark2-env' in params.config['configurations']:
          self.storePropertyToInterpreter(interpreter, 'master', 'string', "yarn-client")
          self.storePropertyToInterpreter(interpreter, 'SPARK_HOME', 'string', "/usr/hdp/current/spark2-client/")
        else:
          del interpreter_settings[setting_key]

    self.set_interpreter_settings(config_data)
    self.update_kerberos_properties()

  def storePropertyToInterpreter(self, interpreter, property_name, property_type, property_value, mode='set'):
    if property_name in interpreter['properties'] and 'value' in interpreter['properties'][property_name]:
      if mode == 'set':
        interpreter['properties'][property_name]['value'] = property_value
      elif mode == 'add':
        interpreter['properties'][property_name]['value'] += property_value
    else:
      interpreter['properties'][property_name] = {'name' : property_name, 'type' : property_type, 'value' : property_value}

  def create_interpreter_json(self):
    import interpreter_json_template
    import params

    if not self.copy_interpreter_from_HDFS_to_FS(params):
      interpreter_json = interpreter_json_template.template
      File(format("{params.conf_dir}/interpreter.json"),
           content=interpreter_json,
           owner=params.zeppelin_user,
           group=params.zeppelin_group,
           mode=0664)

      if params.conf_stored_in_hdfs:
        params.HdfsResource(self.get_zeppelin_conf_FS(params),
                            type="file",
                            action="create_on_execute",
                            source=format("{params.conf_dir}/interpreter.json"),
                            owner=params.zeppelin_user,
                            recursive_chown=True,
                            recursive_chmod=True,
                            replace_existing_files=True)

  def get_zeppelin_spark_dependencies(self):
    import params
    return glob.glob(params.zeppelin_dir + '/interpreter/spark/dep/zeppelin-spark-dependencies*.jar')

if __name__ == "__main__":
  Master().execute()
