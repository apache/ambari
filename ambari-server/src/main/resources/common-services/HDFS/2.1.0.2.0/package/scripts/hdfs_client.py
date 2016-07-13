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
from resource_management.libraries.functions import conf_select, stack_select
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from hdfs import hdfs
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons import OSConst
from resource_management.core.exceptions import ClientComponentHasNoStatus

class HdfsClient(Script):

  def install(self, env):
    import params
    env.set_params(params)
    self.install_packages(env)
    self.configure(env)

  def configure(self, env):
    import params
    env.set_params(params)
    hdfs()

  def start(self, env, upgrade_type=None):
    import params
    env.set_params(params)

  def stop(self, env, upgrade_type=None):
    import params
    env.set_params(params)

  def status(self, env):
    raise ClientComponentHasNoStatus()

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HdfsClientDefault(HdfsClient):

  def get_component_name(self):
    return "hadoop-client"

  def pre_upgrade_restart(self, env, upgrade_type=None):
    import params
    env.set_params(params)
    if params.version and check_stack_feature(StackFeature.ROLLING_UPGRADE, params.version):
      conf_select.select(params.stack_name, "hadoop", params.version)
      stack_select.select("hadoop-client", params.version)

  def security_status(self, env):
    import status_params
    env.set_params(status_params)

    props_value_check = {"hadoop.security.authentication": "kerberos",
                         "hadoop.security.authorization": "true"}
    props_empty_check = ["hadoop.security.auth_to_local"]
    props_read_check = None
    core_site_expectations = build_expectations('core-site', props_value_check, props_empty_check,
                                                props_read_check)
    hdfs_expectations ={}
    hdfs_expectations.update(core_site_expectations)

    security_params = get_params_from_filesystem(status_params.hadoop_conf_dir,
                                                   {'core-site.xml': FILE_TYPE_XML})

    if 'core-site' in security_params and 'hadoop.security.authentication' in security_params['core-site'] and \
        security_params['core-site']['hadoop.security.authentication'].lower() == 'kerberos':
      result_issues = validate_security_config_properties(security_params, hdfs_expectations)
      if not result_issues: # If all validations passed successfully
        if status_params.hdfs_user_principal or status_params.hdfs_user_keytab:
          try:
            cached_kinit_executor(status_params.kinit_path_local,
                       status_params.hdfs_user,
                       status_params.hdfs_user_keytab,
                       status_params.hdfs_user_principal,
                       status_params.hostname,
                       status_params.tmp_dir)
            self.put_structured_out({"securityState": "SECURED_KERBEROS"})
          except Exception as e:
            self.put_structured_out({"securityState": "ERROR"})
            self.put_structured_out({"securityStateErrorInfo": str(e)})
        else:
          self.put_structured_out({"securityIssuesFound": "hdfs principal and/or keytab file is not specified"})
          self.put_structured_out({"securityState": "UNSECURED"})
      else:
        issues = []
        for cf in result_issues:
          issues.append("Configuration file %s did not pass the validation. Reason: %s" % (cf, result_issues[cf]))
        self.put_structured_out({"securityIssuesFound": ". ".join(issues)})
        self.put_structured_out({"securityState": "UNSECURED"})

    else:
      self.put_structured_out({"securityState": "UNSECURED"})

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HdfsClientWindows(HdfsClient):
  def install(self, env):
    import install_params
    self.install_packages(env)
    self.configure(env)

if __name__ == "__main__":
  HdfsClient().execute()
