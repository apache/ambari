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

Ambari Agent

"""

from resource_management import *
from resource_management.libraries.functions import stack_select
from resource_management.libraries.functions.version import compare_versions, format_stack_version
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML

from yarn import yarn
from service import service
from setup_ranger_yarn import setup_ranger_yarn

class Resourcemanager(Script):

  def install(self, env):
    self.install_packages(env)

  def configure(self, env):
    import params

    env.set_params(params)
    yarn(name='resourcemanager')

  def pre_upgrade_restart(self, env, upgrade_type=None):
    Logger.info("Executing Stack Upgrade post-restart")
    import params
    env.set_params(params)

    if params.version and compare_versions(format_stack_version(params.version), '4.0.0.0') >= 0:
      stack_select.select_packages(params.version)
      #Execute(format("iop-select set hadoop-yarn-resourcemanager {version}"))

  def start(self, env, upgrade_type=None):
    import params

    env.set_params(params)
    self.configure(env) # FOR SECURITY
    if params.has_ranger_admin and params.is_supported_yarn_ranger:
      setup_ranger_yarn() #Ranger Yarn Plugin related calls
    service('resourcemanager',
            action='start'
    )

  def stop(self, env, upgrade_type=None):
    import params

    env.set_params(params)

    service('resourcemanager',
            action='stop'
    )

  def status(self, env):
    import status_params

    env.set_params(status_params)
    check_process_status(status_params.resourcemanager_pid_file)
    pass

  def security_status(self, env):
    import status_params
    env.set_params(status_params)
    if status_params.security_enabled:
      props_value_check = {"yarn.timeline-service.http-authentication.type": "kerberos",
                           "yarn.acl.enable": "true"}
      props_empty_check = ["yarn.resourcemanager.principal",
                           "yarn.resourcemanager.keytab",
                           "yarn.resourcemanager.webapp.spnego-principal",
                           "yarn.resourcemanager.webapp.spnego-keytab-file"]

      props_read_check = ["yarn.resourcemanager.keytab",
                          "yarn.resourcemanager.webapp.spnego-keytab-file"]
      yarn_site_props = build_expectations('yarn-site', props_value_check, props_empty_check,
                                           props_read_check)

      yarn_expectations ={}
      yarn_expectations.update(yarn_site_props)

      security_params = get_params_from_filesystem(status_params.hadoop_conf_dir,
                                                   {'yarn-site.xml': FILE_TYPE_XML})
      result_issues = validate_security_config_properties(security_params, yarn_site_props)
      if not result_issues: # If all validations passed successfully
        try:
          # Double check the dict before calling execute
          if ( 'yarn-site' not in security_params
               or 'yarn.resourcemanager.keytab' not in security_params['yarn-site']
               or 'yarn.resourcemanager.principal' not in security_params['yarn-site']) \
            or 'yarn.resourcemanager.webapp.spnego-keytab-file' not in security_params['yarn-site'] \
            or 'yarn.resourcemanager.webapp.spnego-principal' not in security_params['yarn-site']:
            self.put_structured_out({"securityState": "UNSECURED"})
            self.put_structured_out(
              {"securityIssuesFound": "Keytab file or principal are not set property."})
            return

          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.yarn_user,
                                security_params['yarn-site']['yarn.resourcemanager.keytab'],
                                security_params['yarn-site']['yarn.resourcemanager.principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
          cached_kinit_executor(status_params.kinit_path_local,
                                status_params.yarn_user,
                                security_params['yarn-site']['yarn.resourcemanager.webapp.spnego-keytab-file'],
                                security_params['yarn-site']['yarn.resourcemanager.webapp.spnego-principal'],
                                status_params.hostname,
                                status_params.tmp_dir)
          self.put_structured_out({"securityState": "SECURED_KERBEROS"})
        except Exception as e:
          self.put_structured_out({"securityState": "ERROR"})
          self.put_structured_out({"securityStateErrorInfo": str(e)})
      else:
        issues = []
        for cf in result_issues:
          issues.append("Configuration file %s did not pass the validation. Reason: %s" % (cf, result_issues[cf]))
        self.put_structured_out({"securityIssuesFound": ". ".join(issues)})
        self.put_structured_out({"securityState": "UNSECURED"})
    else:
      self.put_structured_out({"securityState": "UNSECURED"})

  def refreshqueues(self, env):
    import params

    self.configure(env)
    env.set_params(params)

    service('resourcemanager',
            action='refreshQueues'
    )

  def decommission(self, env):
    import params

    env.set_params(params)
    rm_kinit_cmd = params.rm_kinit_cmd
    yarn_user = params.yarn_user
    conf_dir = params.hadoop_conf_dir
    user_group = params.user_group

    yarn_refresh_cmd = format("{rm_kinit_cmd} yarn --config {conf_dir} rmadmin -refreshNodes")

    File(params.exclude_file_path,
         content=Template("exclude_hosts_list.j2"),
         owner=yarn_user,
         group=user_group
    )

    if params.update_exclude_file_only == False:
      Execute(yarn_refresh_cmd,
            environment= {'PATH' : params.execute_path },
            user=yarn_user)
      pass
    pass


if __name__ == "__main__":
  Resourcemanager().execute()
