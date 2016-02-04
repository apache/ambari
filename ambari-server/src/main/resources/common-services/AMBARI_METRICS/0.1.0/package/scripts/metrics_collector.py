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

from resource_management import *
from resource_management.libraries.functions.security_commons import build_expectations, \
  cached_kinit_executor, get_params_from_filesystem, validate_security_config_properties, \
  FILE_TYPE_XML
from ams import ams
from ams_service import ams_service
from hbase import hbase
from status import check_service_status
from ambari_commons import OSConst
from ambari_commons.os_family_impl import OsFamilyImpl

class AmsCollector(Script):
  def install(self, env):
    self.install_packages(env, exclude_packages = ['ambari-metrics-grafana'])

  def configure(self, env, action = None):
    import params
    env.set_params(params)
    hbase('master', action)
    hbase('regionserver', action)
    ams(name='collector')

  def start(self, env):
    self.configure(env, action = 'start') # for security
    # stop hanging components before start
    ams_service('collector', action = 'stop')
    ams_service('collector', action = 'start')

  def stop(self, env):
    import params
    env.set_params(params)
    # Sometimes, stop() may be called before start(), in case restart() is initiated right after installation
    self.configure(env, action = 'stop') # for security
    ams_service('collector', action = 'stop')

  def status(self, env):
    import status_params
    env.set_params(status_params)
    check_service_status(name='collector')


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class AmsCollectorDefault(AmsCollector):
  def security_status(self, env):
    import status_params

    env.set_params(status_params)
    props_value_check = {"hbase.security.authentication": "kerberos",
                         "hbase.security.authorization": "true"}

    props_empty_check = ["hbase.zookeeper.property.authProvider.1",
                         "hbase.master.keytab.file",
                         "hbase.master.kerberos.principal",
                         "hbase.regionserver.keytab.file",
                         "hbase.regionserver.kerberos.principal"
                         ]
    props_read_check = ['hbase.master.keytab.file', 'hbase.regionserver.keytab.file']
    ams_hbase_site_expectations = build_expectations('hbase-site', props_value_check,
                                                     props_empty_check,
                                                     props_read_check)

    expectations = {}
    expectations.update(ams_hbase_site_expectations)

    security_params = get_params_from_filesystem(status_params.ams_hbase_conf_dir,
                                                 {'hbase-site.xml': FILE_TYPE_XML})

    is_hbase_distributed = security_params['hbase-site']['hbase.cluster.distributed']
    # for embedded mode, when HBase is backed by file, security state is SECURED_KERBEROS by definition when cluster is secured
    if status_params.security_enabled and not is_hbase_distributed:
      self.put_structured_out({"securityState": "SECURED_KERBEROS"})
      return

    result_issues = validate_security_config_properties(security_params, expectations)

    if not result_issues:  # If all validations passed successfully
      try:
        # Double check the dict before calling execute
        if ('hbase-site' not in security_params or
                'hbase.master.keytab.file' not in security_params['hbase-site'] or
                'hbase.master.kerberos.principal' not in security_params['hbase-site']):
          self.put_structured_out({"securityState": "UNSECURED"})
          self.put_structured_out(
            {"securityIssuesFound": "Keytab file or principal are not set property."})
          return

        cached_kinit_executor(status_params.kinit_path_local,
                              status_params.hbase_user,
                              security_params['hbase-site']['hbase.master.keytab.file'],
                              security_params['hbase-site']['hbase.master.kerberos.principal'],
                              status_params.hostname,
                              status_params.tmp_dir)
        self.put_structured_out({"securityState": "SECURED_KERBEROS"})
      except Exception as e:
        self.put_structured_out({"securityState": "ERROR"})
        self.put_structured_out({"securityStateErrorInfo": str(e)})
    else:
      issues = []
      for cf in result_issues:
        issues.append("Configuration file %s did not pass the validation. Reason: %s" % (
          cf, result_issues[cf]))
      self.put_structured_out({"securityIssuesFound": ". ".join(issues)})
      self.put_structured_out({"securityState": "UNSECURED"})


@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class AmsCollectorWindows(AmsCollector):
  def install(self, env):
    self.install_packages(env)
    self.configure(env) # for security

if __name__ == "__main__":
  AmsCollector().execute()
