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

from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.default import default
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions import get_kinit_path
from ambari_commons.constants import AMBARI_SUDO_BINARY
import jnbg_helpers as helpers

# Server configurations
config = Script.get_config()
stack_root = Script.get_stack_root()
cluster_configs = config['clusterHostInfo']

# Notebook service configs
user = config['configurations']['jnbg-env']['notebook_user']
group = config['configurations']['jnbg-env']['notebook_group']
log_dir = config['configurations']['jnbg-env']['jkg_log_dir']
jkg_pid_dir = config['configurations']['jnbg-env']['jkg_pid_dir_prefix']
jkg_host = str(cluster_configs['kernel_gateway_hosts'][0])
jkg_port = str(config['configurations']['jnbg-env']['jkg_port'])
jkg_loglevel = str(config['configurations']['jnbg-env']['jkg_loglevel'])
jkg_max_kernels = config['configurations']['jnbg-env']['max_kernels']
jkg_cull_period = config['configurations']['jnbg-env']['cull_idle_kernel_period']
jkg_cull_interval = config['configurations']['jnbg-env']['cull_idle_kernel_interval']
py_executable = config['configurations']['jnbg-env']['python_interpreter_path']
py_venv_pathprefix = config['configurations']['jnbg-env']['python_virtualenv_path_prefix']
py_venv_restrictive = config['configurations']['jnbg-env']['python_virtualenv_restrictive']
spark_sql_warehouse_dir = config['configurations']['jnbg-env']['spark_sql_warehouse_dir']
pythonpath = config['configurations']['jnbg-env']['pythonpath']
spark_home = format("{stack_root}/current/spark2-client")
security_enabled = config['configurations']['cluster-env']['security_enabled']
#ui_ssl_enabled = config['configurations']['jnbg-env']['jnbg.ssl']
ui_ssl_enabled = False
spark_opts = str(config['configurations']['jnbg-env']['kernel_spark_opts'])
modified_spark_opts = format("{spark_opts} --conf spark.sql.warehouse.dir={spark_sql_warehouse_dir}")
modified_spark_opts = "'{0}'".format(modified_spark_opts)
toree_opts = str(config['configurations']['jnbg-env']['toree_opts'])
toree_opts = "'{0}'".format(toree_opts)
kinit_path_local = get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
jkg_log_formatter_cmd = format("%(asctime)s,%(msecs)03d %(levelname)s %(name)s: %(message)s")
jkg_log_formatter_cmd = "'{0}'".format(jkg_log_formatter_cmd)
venv_owner="root" if py_venv_restrictive else user
spark_config_dir = config['configurations']['jnbg-env']['spark_conf_dir']
interpreters = "Scala"

jnbg_kinit_cmd = ""
if security_enabled:
  _hostname_lowercase = config['hostname'].lower()
  jnbg_kerberos_keytab = config['configurations']['jnbg-env']['jnbg.service.kerberos.keytab']
  jnbg_kerberos_principal = config['configurations']['jnbg-env']['jnbg.service.kerberos.principal']
  jnbg_kerberos_principal = jnbg_kerberos_principal.replace('_HOST',_hostname_lowercase)
  jnbg_kinit_cmd = format("{kinit_path_local} -kt {jnbg_kerberos_keytab} {jnbg_kerberos_principal}; ")

jnbg_kinit_arg = "'{0}'".format(jnbg_kinit_cmd)

ambarisudo = AMBARI_SUDO_BINARY
home_dir = format("/home/{user}")
hdfs_home_dir = format("/user/{user}")
jkg_pid_file = format("{jkg_pid_dir}/jupyter_kernel_gateway.pid")
dirs = [(hdfs_home_dir, "0775"), (spark_sql_warehouse_dir, "01770")]
package_dir = helpers.package_dir()
sh_scripts_dir = format("{package_dir}files/")
sh_scripts = ['jkg_install.sh',
              'toree_install.sh',
              'log4j_setup.sh',
              'toree_configure.sh',
              'pyspark_configure.sh',
              'pythonenv_setup.sh']
sh_scripts_user = ['jkg_start.sh']

# Sequence of commands to be executed for JKG installation
jkg_commands = []
cmd_file_name = "jkg_install.sh"
cmd_file_path = format("{package_dir}files/{cmd_file_name}")

jkg_commands.append(ambarisudo + ' ' +
                    cmd_file_path + ' ' +
                    py_executable + ' ' +
                    py_venv_pathprefix + ' ' +
                    venv_owner + ' ' +
                    jnbg_kinit_arg)

# Sequence of commands executed for Toree installation
toree_commands = []
cmd_file_name = "toree_install.sh"
cmd_file_path = format("{package_dir}files/{cmd_file_name}")

toree_commands.append(ambarisudo + ' ' +
                      cmd_file_path + ' ' +
                      py_executable + ' ' +
                      py_venv_pathprefix + ' ' +
                      venv_owner + ' ' +
                      jnbg_kinit_arg + ' ' +
                      spark_home + ' ' +
                      modified_spark_opts)

# Sequence of commands executed for Toree configuration
toree_configure_commands = []
cmd_file_name = "toree_configure.sh"
cmd_file_path = format("{package_dir}files/{cmd_file_name}")

toree_configure_commands.append(ambarisudo + ' ' +
                                cmd_file_path + ' ' +
                                user + ' ' +
                                py_executable + ' ' +
                                py_venv_pathprefix + ' ' +
                                venv_owner + ' ' +
                                jnbg_kinit_arg + ' ' +
                                spark_home + ' ' +
                                interpreters + ' ' +
                                toree_opts + ' ' +
                                modified_spark_opts)

# Sequence of commands executed for PySpark kernel configuration
pyspark_configure_commands = []
cmd_file_name = "pyspark_configure.sh"
cmd_file_path = format("{package_dir}files/{cmd_file_name}")

pyspark_configure_commands.append(ambarisudo + ' ' +
                                  cmd_file_path + ' ' +
                                  py_executable + ' ' +
                                  py_venv_pathprefix + ' ' +
                                  venv_owner + ' ' +
                                  jnbg_kinit_arg + ' ' +
                                  spark_home + ' ' +
                                  pythonpath + ' ' +
                                  modified_spark_opts)

log4j_setup_commands = []
cmd_file_name = "log4j_setup.sh"
cmd_file_path = format("{package_dir}files/{cmd_file_name}")

log4j_setup_commands.append(ambarisudo + ' ' +
                            cmd_file_path + ' ' +
                            spark_config_dir)

# JKG startup command
start_args = ['"jupyter kernelgateway' +
              ' --ip=' + '0.0.0.0' +
              ' --port=' + jkg_port +
              ' --port_retries=' + '0' +
              ' --log-level=' + jkg_loglevel +
              ' --KernelGatewayApp.max_kernels=' + jkg_max_kernels,
              ' --KernelGatewayApp.cull_idle_kernel_period=' + jkg_cull_period,
              ' --KernelGatewayApp.cull_idle_kernel_interval=' + jkg_cull_interval,
              ' --KernelSpecManager.ensure_native_kernel=' + 'False',
              ' --KernelGatewayApp.log_format=' + jkg_log_formatter_cmd,
              ' --JupyterWebsocketPersonality.list_kernels=' + 'True "',
              spark_home,
              py_executable,
              py_venv_pathprefix,
              jnbg_kinit_arg,
              log_dir + "/jupyter_kernel_gateway.log",
              jkg_pid_file]

cmd_file_name = "jkg_start.sh"
cmd_file_path = format("{package_dir}files/{cmd_file_name}")
start_command = cmd_file_path + ' ' + ' '.join(start_args)
