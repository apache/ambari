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
from resource_management.core.resources import Directory
from resource_management.core.resources.system import Execute, File
from resource_management.core.source import InlineTemplate
from resource_management.libraries import XmlConfig
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.format import format
from resource_management.libraries.script.script import Script

class Master(Script):
  def install(self, env):
    import params
    env.set_params(params)

    Execute('chmod a+x ' + os.path.join(params.service_packagedir, "scripts/setup_snapshot.sh"))
    self.install_packages(env)

    # create the pid and zeppelin dirs
    Directory([params.zeppelin_pid_dir, params.zeppelin_dir],
              owner=params.zeppelin_user,
              group=params.zeppelin_group,
              cd_access="a",
              create_parents=True,
              mode=0755
              )

    # update the configs specified by user
    self.configure(env)

    Execute('echo spark_version:' + params.spark_version + ' detected for spark_home: '
            + params.spark_home + ' >> ' + params.zeppelin_log_file, user=params.zeppelin_user)

    # run setup_snapshot.sh
    Execute(format("{service_packagedir}/scripts/setup_snapshot.sh {zeppelin_dir} "
                   "{hive_metastore_host} {hive_metastore_port} {hive_server_port} "
                   "{zeppelin_host} {zeppelin_port} {setup_view} {service_packagedir} "
                   "{java64_home} >> {zeppelin_log_file}"),
            user=params.zeppelin_user)

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

    spark_deps_full_path = glob.glob(params.zeppelin_dir + '/interpreter/spark/dep/zeppelin-spark-dependencies-*.jar')[0]
    spark_dep_file_name = os.path.basename(spark_deps_full_path);

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

  def configure(self, env):
    import params
    import status_params
    env.set_params(params)
    env.set_params(status_params)
    self.create_zeppelin_log_dir(env)

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

  def stop(self, env):
    import params
    self.create_zeppelin_log_dir(env)
    Execute(params.zeppelin_dir + '/bin/zeppelin-daemon.sh stop >> ' + params.zeppelin_log_file,
            user=params.zeppelin_user)

  def start(self, env):
    import params
    import status_params
    import time
    self.configure(env)

    if params.security_enabled:
        spark_kinit_cmd = format("{kinit_path_local} -kt {zeppelin_kerberos_keytab} {zeppelin_kerberos_principal}; ")
        Execute(spark_kinit_cmd, user=params.zeppelin_user)

    if glob.glob(
            params.zeppelin_dir + '/interpreter/spark/dep/zeppelin-spark-dependencies-*.jar') and os.path.exists(
      glob.glob(params.zeppelin_dir + '/interpreter/spark/dep/zeppelin-spark-dependencies-*.jar')[0]):
      self.create_zeppelin_dir(params)

    # if first_setup:
    if not glob.glob(params.conf_dir + "/interpreter.json") and \
      not os.path.exists(params.conf_dir + "/interpreter.json"):
      Execute(params.zeppelin_dir + '/bin/zeppelin-daemon.sh start >> '
              + params.zeppelin_log_file, user=params.zeppelin_user)
    time.sleep(20)
    self.update_zeppelin_interpreter()

    Execute(params.zeppelin_dir + '/bin/zeppelin-daemon.sh restart >> '
            + params.zeppelin_log_file, user=params.zeppelin_user)
    pidfile = glob.glob(status_params.zeppelin_pid_dir
                        + '/zeppelin-' + params.zeppelin_user + '*.pid')[0]
    Execute('echo pid file is: ' + pidfile, user=params.zeppelin_user)
    contents = open(pidfile).read()
    Execute('echo pid is ' + contents, user=params.zeppelin_user)

  def status(self, env):
    import status_params
    env.set_params(status_params)

    try:
        pid_file = glob.glob(status_params.zeppelin_pid_dir + '/zeppelin-' +
                             status_params.zeppelin_user + '*.pid')[0]
    except IndexError:
        pid_file = ''
    check_process_status(pid_file)

  def update_zeppelin_interpreter(self):
    import params
    import json

    interpreter_config = params.conf_dir + "/interpreter.json"
    interpreter_config_file = open(interpreter_config, "r")
    config_data = json.load(interpreter_config_file)
    interpreter_config_file.close()
    interpreter_settings = config_data['interpreterSettings']

    for notebooks in interpreter_settings:
        notebook = interpreter_settings[notebooks]
        if notebook['group'] == 'jdbc' and params.hive_server_host:
            notebook['properties']['default.url'] = 'jdbc:hive2://' +\
                                                             params.hive_server_host +\
                                                             ':' + params.hive_server_port
            notebook['properties']['default.driver'] = "org.apache.hive.jdbc.HiveDriver"
            notebook['dependencies'] = []
            notebook['dependencies'].append(
              {"groupArtifactVersion": "org.apache.hive:hive-jdbc:2.0.1", "local": "false"})
            notebook['dependencies'].append(
              {"groupArtifactVersion": "org.apache.hadoop:hadoop-common:2.7.2", "local": "false"})
        elif notebook['group'] == 'phoenix' and params.zookeeper_znode_parent \
                and params.hbase_zookeeper_quorum:
            notebook['properties']['phoenix.jdbc.url'] = "jdbc:phoenix:" +\
                                                         params.hbase_zookeeper_quorum + ':' +\
                                                         params.zookeeper_znode_parent
        elif notebook['group'] == 'livy' and params.livy_livyserver_host:
            notebook['properties']['livy.spark.master'] = "yarn-cluster"
            notebook['properties']['zeppelin.livy.principal'] = params.zeppelin_kerberos_principal
            notebook['properties']['zeppelin.livy.keytab'] = params.zeppelin_kerberos_keytab
            notebook['properties']['zeppelin.livy.url'] = "http://" + params.livy_livyserver_host +\
                                                          ":" + params.livy_livyserver_port
        elif notebook['group'] == 'spark':
            notebook['properties']['master'] = "yarn-cluster"
            notebook['properties']['spark.yarn.principal'] = params.zeppelin_kerberos_principal
            notebook['properties']['spark.yarn.keytab'] = params.zeppelin_kerberos_keytab

    interpreter_config_file = open(interpreter_config, "w+")
    interpreter_config_file.write(json.dumps(config_data, indent=2))
    interpreter_config_file.close()

if __name__ == "__main__":
  Master().execute()
