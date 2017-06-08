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
from resource_management.core.base import Fail
from resource_management.core.resources import Directory
from resource_management.core.resources.system import Execute, File
from resource_management.core.source import InlineTemplate
from resource_management.core import sudo
from resource_management.core.logger import Logger
from resource_management.core.source import StaticFile
from resource_management.libraries import XmlConfig
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import conf_select
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions.decorator import retry
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.script.script import Script

class Master(Script):

  def get_component_name(self):
    return "zeppelin-server"

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

    params.HdfsResource(params.spark_jar_dir + "/" + spark_dep_file_name,
                        type="file",
                        action="create_on_execute",
                        source=spark_deps_full_path,
                        group=params.zeppelin_group,
                        owner=params.zeppelin_user,
                        mode=0444,
                        replace_existing_files=True,
                        )

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

    # write out zeppelin-site.xml
    XmlConfig("zeppelin-site.xml",
              conf_dir=params.conf_dir,
              configurations=params.config['configurations']['zeppelin-config'],
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

    if len(params.hbase_master_hosts) > 0 and params.is_hbase_installed:
      # copy hbase-site.xml
      XmlConfig("hbase-site.xml",
              conf_dir=params.external_dependency_conf,
              configurations=params.config['configurations']['hbase-site'],
              configuration_attributes=params.config['configuration_attributes']['hbase-site'],
              owner=params.zeppelin_user,
              group=params.zeppelin_group,
              mode=0644)

      XmlConfig("hdfs-site.xml",
                conf_dir=params.external_dependency_conf,
                configurations=params.config['configurations']['hdfs-site'],
                configuration_attributes=params.config['configuration_attributes']['hdfs-site'],
                owner=params.zeppelin_user,
                group=params.zeppelin_group,
                mode=0644)

      XmlConfig("core-site.xml",
                conf_dir=params.external_dependency_conf,
                configurations=params.config['configurations']['core-site'],
                configuration_attributes=params.config['configuration_attributes']['core-site'],
                owner=params.zeppelin_user,
                group=params.zeppelin_group,
                mode=0644)

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
    Execute(("chown", "-R", format("{zeppelin_user}") + ":" + format("{zeppelin_group}"),
             os.path.join(params.zeppelin_dir, "notebook")), sudo=True)

    if params.security_enabled:
        zeppelin_kinit_cmd = format("{kinit_path_local} -kt {zeppelin_kerberos_keytab} {zeppelin_kerberos_principal}; ")
        Execute(zeppelin_kinit_cmd, user=params.zeppelin_user)

    zeppelin_spark_dependencies = self.get_zeppelin_spark_dependencies()
    if zeppelin_spark_dependencies and os.path.exists(zeppelin_spark_dependencies[0]):
      self.create_zeppelin_dir(params)

    # if first_setup:
    if not glob.glob(params.conf_dir + "/interpreter.json") and \
      not os.path.exists(params.conf_dir + "/interpreter.json"):
      Execute(params.zeppelin_dir + '/bin/zeppelin-daemon.sh start >> '
              + params.zeppelin_log_file, user=params.zeppelin_user)
      self.check_zeppelin_server()
      self.update_zeppelin_interpreter()

    self.update_kerberos_properties()

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

  def get_interpreter_settings(self):
    import params
    import json

    interpreter_config = os.path.join(params.conf_dir, "interpreter.json")
    config_content = sudo.read_file(interpreter_config)
    config_data = json.loads(config_content)
    return config_data

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade pre-restart")
    import params
    env.set_params(params)

    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, format_stack_version(params.version)):
      conf_select.select(params.stack_name, "zeppelin", params.version)
      stack_select.select("zeppelin-server", params.version)

  def set_interpreter_settings(self, config_data):
    import params
    import json

    interpreter_config = os.path.join(params.conf_dir, "interpreter.json")
    File(interpreter_config,
         group=params.zeppelin_group,
         owner=params.zeppelin_user,
         content=json.dumps(config_data, indent=2)
         )

  def update_kerberos_properties(self):
    import params
    config_data = self.get_interpreter_settings()
    interpreter_settings = config_data['interpreterSettings']
    for interpreter_setting in interpreter_settings:
      interpreter = interpreter_settings[interpreter_setting]
      if interpreter['group'] == 'livy' and params.livy_livyserver_host:
        if params.zeppelin_kerberos_principal and params.zeppelin_kerberos_keytab and params.security_enabled:
          interpreter['properties']['zeppelin.livy.principal'] = params.zeppelin_kerberos_principal
          interpreter['properties']['zeppelin.livy.keytab'] = params.zeppelin_kerberos_keytab
        else:
          interpreter['properties']['zeppelin.livy.principal'] = ""
          interpreter['properties']['zeppelin.livy.keytab'] = ""
      elif interpreter['group'] == 'spark':
        if params.zeppelin_kerberos_principal and params.zeppelin_kerberos_keytab and params.security_enabled:
          interpreter['properties']['spark.yarn.principal'] = params.zeppelin_kerberos_principal
          interpreter['properties']['spark.yarn.keytab'] = params.zeppelin_kerberos_keytab
        else:
          interpreter['properties']['spark.yarn.principal'] = ""
          interpreter['properties']['spark.yarn.keytab'] = ""
      elif interpreter['group'] == 'jdbc':
        if params.zeppelin_kerberos_principal and params.zeppelin_kerberos_keytab and params.security_enabled:
          interpreter['properties']['zeppelin.jdbc.auth.type'] = "KERBEROS"
          interpreter['properties']['zeppelin.jdbc.principal'] = params.zeppelin_kerberos_principal
          interpreter['properties']['zeppelin.jdbc.keytab.location'] = params.zeppelin_kerberos_keytab
          if params.zookeeper_znode_parent \
              and params.hbase_zookeeper_quorum \
              and params.zookeeper_znode_parent not in interpreter['properties']['phoenix.url']:
            interpreter['properties']['phoenix.url'] = "jdbc:phoenix:" + \
                                                       params.hbase_zookeeper_quorum + ':' + \
                                                       params.zookeeper_znode_parent
        else:
          interpreter['properties']['zeppelin.jdbc.auth.type'] = ""
          interpreter['properties']['zeppelin.jdbc.principal'] = ""
          interpreter['properties']['zeppelin.jdbc.keytab.location'] = ""
      elif interpreter['group'] == 'sh':
        if params.zeppelin_kerberos_principal and params.zeppelin_kerberos_keytab and params.security_enabled:
          interpreter['properties']['zeppelin.shell.auth.type'] = "KERBEROS"
          interpreter['properties']['zeppelin.shell.principal'] = params.zeppelin_kerberos_principal
          interpreter['properties']['zeppelin.shell.keytab.location'] = params.zeppelin_kerberos_keytab
        else:
          interpreter['properties']['zeppelin.shell.auth.type'] = ""
          interpreter['properties']['zeppelin.shell.principal'] = ""
          interpreter['properties']['zeppelin.shell.keytab.location'] = ""

    self.set_interpreter_settings(config_data)

  def update_zeppelin_interpreter(self):
    import params
    config_data = self.get_interpreter_settings()
    interpreter_settings = config_data['interpreterSettings']

    if 'spark2-defaults' in params.config['configurations']:
      spark2_config = self.get_spark2_interpreter_config()
      config_id = spark2_config["id"]
      interpreter_settings[config_id] = spark2_config

    if params.livy2_livyserver_host:
      livy2_config = self.get_livy2_interpreter_config()
      config_id = livy2_config["id"]
      interpreter_settings[config_id] = livy2_config

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
      if interpreter['group'] == 'jdbc':
        interpreter['dependencies'] = []

        if not params.hive_server_host and params.hive_server_interactive_hosts:
          hive_interactive_properties_key = 'hive'

        if params.hive_server_host:
          interpreter['properties']['hive.driver'] = 'org.apache.hive.jdbc.HiveDriver'
          interpreter['properties']['hive.user'] = 'hive'
          interpreter['properties']['hive.password'] = ''
          if params.hive_server2_support_dynamic_service_discovery:
            interpreter['properties']['hive.url'] = 'jdbc:hive2://' + \
                                                 params.hive_zookeeper_quorum + \
                                                 '/;' + 'serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=' + \
                                                    params.hive_zookeeper_namespace
          else:
            interpreter['properties']['hive.url'] = 'jdbc:hive2://' + \
                                                 params.hive_server_host + \
                                                     ':' + params.hive_server_port
        if params.hive_server_interactive_hosts:
          interpreter['properties'][hive_interactive_properties_key + '.driver'] = 'org.apache.hive.jdbc.HiveDriver'
          interpreter['properties'][hive_interactive_properties_key + '.user'] = 'hive'
          interpreter['properties'][hive_interactive_properties_key + '.password'] = ''
          if params.hive_server2_support_dynamic_service_discovery:
            interpreter['properties'][hive_interactive_properties_key + '.url'] = 'jdbc:hive2://' + \
                                                    params.hive_zookeeper_quorum + \
                                                    '/;' + 'serviceDiscoveryMode=zooKeeper;zooKeeperNamespace=' + \
                                                    params.hive_interactive_zookeeper_namespace
          else:
            interpreter['properties'][hive_interactive_properties_key + '.url'] = 'jdbc:hive2://' + \
                                                    params.hive_server_interactive_hosts + \
                                                    ':' + params.hive_server_port


        if params.zookeeper_znode_parent \
                and params.hbase_zookeeper_quorum:
            interpreter['properties']['phoenix.driver'] = 'org.apache.phoenix.jdbc.PhoenixDriver'
            interpreter['properties']['phoenix.hbase.client.retries.number'] = '1'
            interpreter['properties']['phoenix.user'] = 'phoenixuser'
            interpreter['properties']['phoenix.password'] = ''
            interpreter['properties']['phoenix.url'] = "jdbc:phoenix:" + \
                                                    params.hbase_zookeeper_quorum + ':' + \
                                                    params.zookeeper_znode_parent

      elif interpreter['group'] == 'livy' and interpreter['name'] == 'livy':
        if params.livy_livyserver_host:
          interpreter['properties']['zeppelin.livy.url'] = "http://" + params.livy_livyserver_host + \
                                                           ":" + params.livy_livyserver_port
        else:
          del interpreter_settings[setting_key]

      elif interpreter['group'] == 'livy' and interpreter['name'] == 'livy2':
        if params.livy2_livyserver_host:
          interpreter['properties']['zeppelin.livy.url'] = "http://" + params.livy2_livyserver_host + \
                                                           ":" + params.livy2_livyserver_port
        else:
          del interpreter_settings[setting_key]


      elif interpreter['group'] == 'spark' and interpreter['name'] == 'spark':
        if 'spark-env' in params.config['configurations']:
          interpreter['properties']['master'] = "yarn-client"
          interpreter['properties']['SPARK_HOME'] = "/usr/hdp/current/spark-client/"
        else:
          del interpreter_settings[setting_key]

      elif interpreter['group'] == 'spark' and interpreter['name'] == 'spark2':
        if 'spark2-env' in params.config['configurations']:
          interpreter['properties']['master'] = "yarn-client"
          interpreter['properties']['SPARK_HOME'] = "/usr/hdp/current/spark2-client/"
        else:
          del interpreter_settings[setting_key]

    self.set_interpreter_settings(config_data)

  @retry(times=30, sleep_time=5, err_class=Fail)
  def check_zeppelin_server(self):
    import params
    path = params.conf_dir + "/interpreter.json"
    if os.path.exists(path) and os.path.getsize(path):
      Logger.info("interpreter.json found. Zeppelin server started.")
    else:
      raise Fail("interpreter.json not found. waiting for Zeppelin server to start...")

  def get_zeppelin_spark_dependencies(self):
    import params
    return glob.glob(params.zeppelin_dir + '/interpreter/spark/dep/zeppelin-spark-dependencies*.jar')

  def get_spark2_interpreter_config(self):
    import spark2_config_template
    import json

    return json.loads(spark2_config_template.template)

  def get_livy2_interpreter_config(self):
    import livy2_config_template
    import json

    return json.loads(livy2_config_template.template)

if __name__ == "__main__":
  Master().execute()
