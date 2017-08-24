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

from resource_management import Script
from resource_management.core.logger import Logger
from resource_management.core.resources import File
from resource_management.core.resources.system import Directory
from resource_management.core.resources.system import Execute
from resource_management.core.source import InlineTemplate
from resource_management.core.source import Template
from resource_management.libraries.functions import StackFeature
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.show_logs import show_logs
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.resources.properties_file import PropertiesFile

class Superset(Script):

  def get_component_name(self):
    return format("superset")

  def install(self, env):
    self.install_packages(env)

  def configure(self, env, upgrade_type=None):
    import params
    Directory(
      [params.superset_pid_dir, params.superset_log_dir, params.superset_config_dir, params.superset_home_dir],
      mode=0755,
      cd_access='a',
      owner=params.superset_user,
      group=params.user_group,
      create_parents=True,
      recursive_ownership=True
    )

    File(format("{params.superset_config_dir}/superset-env.sh"),
         mode=0755,
         owner=params.superset_user,
         group=params.user_group,
         content=InlineTemplate(params.superset_env_sh_template)
         )

    File(os.path.join(params.superset_bin_dir, 'superset.sh'),
         owner=params.superset_user,
         group=params.user_group,
         mode=0755,
         content=Template("superset.sh")
         )
    superset_config =  mutable_config_dict(params.config["configurations"]["superset"])

    if params.superset_db_uri:
      superset_config["SQLALCHEMY_DATABASE_URI"] = params.superset_db_uri

    PropertiesFile("superset_config.py",
                   dir=params.superset_config_dir,
                   properties=quote_string_values(superset_config),
                   owner=params.superset_user,
                   group=params.user_group
                   )

    # Initialize DB and create admin user.
    Execute(format("source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/superset db upgrade"),
            user=params.superset_user)
    Execute(format("source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/fabmanager create-admin --app superset --username '{params.superset_admin_user}' --password '{params.superset_admin_password!p}' --firstname '{params.superset_admin_firstname}' --lastname '{params.superset_admin_lastname}' --email '{params.superset_admin_email}'"),
            user=params.superset_user)
    Execute(format("source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/superset init"),
            user=params.superset_user)

    # Configure Druid Cluster in superset DB
    if len(params.druid_coordinator_hosts) > 0 :
      Execute(format("source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/superset configure_druid_cluster --name druid-ambari --coordinator-host {params.druid_coordinator_host} --coordinator-port {params.druid_coordinator_port} --broker-host {params.druid_router_host} --broker-port {params.druid_router_port} --coordinator-endpoint druid/coordinator/v1/metadata --broker-endpoint druid/v2"),
            user=params.superset_user)

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing superset Upgrade pre-restart")
    import params

    env.set_params(params)

    if params.stack_version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.stack_version):
      stack_select.select_packages(params.version)

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    daemon_cmd = self.get_daemon_cmd(params, "start")
    try:
      Execute(daemon_cmd,
              user=params.superset_user
              )
    except:
      show_logs(params.superset_log_dir, params.superset_user)
      raise

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    self.configure(env, upgrade_type=upgrade_type)
    daemon_cmd = self.get_daemon_cmd(params, "stop")
    try:
      Execute(daemon_cmd,
              user=params.superset_user
              )
    except:
      show_logs(params.superset_log_dir, params.superset_user)
      raise

  def status(self, env):
    import status_params
    env.set_params(status_params)
    pid_file = status_params.superset_pid_dir + '/superset.pid'
    check_process_status(pid_file)

  def get_log_folder(self):
    import params
    return params.superset_log_dir

  def get_user(self):
    import params
    return params.superset_user

  def get_daemon_cmd(self, params=None, command=None):
    return format('source {params.superset_config_dir}/superset-env.sh ; {params.superset_bin_dir}/superset.sh {command}')

def mutable_config_dict(config):
  rv = {}
  for key, value in config.iteritems():
    rv[key] = value
  return rv

def quote_string_values(config):
  rv = {}
  for key, value in config.iteritems():
    rv[key] = quote_string_value(value)
  return rv

def quote_string_value(value):
  if value.lower() == "true" or value.lower() == "false" or value.isdigit():
    return value
  else:
    return "'{0}'".format(value)



if __name__ == "__main__":
  Superset().execute()
