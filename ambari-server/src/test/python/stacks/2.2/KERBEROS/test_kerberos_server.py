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
import use_cases

from stacks.utils.RMFTestCase import *

class TestKerberosServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "KERBEROS/1.10.3-10/package"
  STACK_VERSION = "2.2"

  def test_configure_managed_kdc(self):
    json_data = use_cases.get_manged_kdc_use_case()

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_server.py",
                       classname="KerberosServer",
                       command="configure",
                       config_dict=json_data,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    # Validate krb5.conf file
    self.assertResourceCalled('Directory', use_cases.get_krb5_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0755,
                              recursive=True)

    file_path = (use_cases.get_krb5_conf_dir(json_data) +
                 "/" +
                 use_cases.get_krb5_conf_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=Template('krb5_conf.j2'),
                              owner='root',
                              group='root',
                              mode=0644)

    # Validate kdc.conf file
    self.assertResourceCalled('Directory', use_cases.get_kdc_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0700,
                              recursive=True)

    file_path = (use_cases.get_kdc_conf_dir(json_data) +
                 "/" +
                 use_cases.get_kdc_conf_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=Template('kdc_conf.j2'),
                              owner='root',
                              group='root',
                              mode=0600)

    # Validate kadm5.acl file
    self.assertResourceCalled('Directory', use_cases.get_kadm5_acl_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0700,
                              recursive=True)

    file_path = (use_cases.get_kadm5_acl_dir(json_data) +
                 "/" +
                 use_cases.get_kadm5_acl_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=Template('kadm5_acl.j2'),
                              owner='root',
                              group='root',
                              mode=0600)

  def test_configure_unmanaged_kdc(self):
    json_data = use_cases.get_unmanged_kdc_use_case()

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_server.py",
                       classname="KerberosServer",
                       command="configure",
                       config_dict=json_data,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    # Validate krb5.conf file
    self.assertResourceCalled('Directory', use_cases.get_krb5_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0755,
                              recursive=True)

    file_path = (use_cases.get_krb5_conf_dir(json_data) +
                 "/" +
                 use_cases.get_krb5_conf_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_krb5_conf_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0644)

    # Validate kdc.conf file
    self.assertResourceCalled('Directory', use_cases.get_kdc_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0700,
                              recursive=True)

    file_path = (use_cases.get_kdc_conf_dir(json_data) +
                 "/" +
                 use_cases.get_kdc_conf_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_kdc_conf_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0600)

    # Validate kadm5.acl file
    self.assertResourceCalled('Directory', use_cases.get_kadm5_acl_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0700,
                              recursive=True)

    file_path = (use_cases.get_kadm5_acl_dir(json_data) +
                 "/" +
                 use_cases.get_kadm5_acl_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_kadm5_acl_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0600)

  def test_configure_unmanaged_ad(self):
    json_data = use_cases.get_unmanged_ad_use_case()

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_server.py",
                       classname="KerberosServer",
                       command="configure",
                       config_dict=json_data,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    # Validate krb5.conf file
    self.assertResourceCalled('Directory', use_cases.get_krb5_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0755,
                              recursive=True)

    file_path = (use_cases.get_krb5_conf_dir(json_data) +
                 "/" +
                 use_cases.get_krb5_conf_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_krb5_conf_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0644)

    # Validate kdc.conf file
    self.assertResourceCalled('Directory', use_cases.get_kdc_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0700,
                              recursive=True)

    file_path = (use_cases.get_kdc_conf_dir(json_data) +
                 "/" +
                 use_cases.get_kdc_conf_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_kdc_conf_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0600)

    # Validate kadm5.acl file
    self.assertResourceCalled('Directory', use_cases.get_kadm5_acl_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0700,
                              recursive=True)

    file_path = (use_cases.get_kadm5_acl_dir(json_data) +
                 "/" +
                 use_cases.get_kadm5_acl_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_kadm5_acl_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0600)

  def test_configure_cross_realm_trust(self):
    json_data = use_cases.get_cross_realm_use_case()

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_server.py",
                       classname="KerberosServer",
                       command="configure",
                       config_dict=json_data,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    # Validate krb5.conf file
    self.assertResourceCalled('Directory', use_cases.get_krb5_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0755,
                              recursive=True)

    file_path = (use_cases.get_krb5_conf_dir(json_data) +
                 "/" +
                 use_cases.get_krb5_conf_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_krb5_conf_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0644)

    # Validate kdc.conf file
    self.assertResourceCalled('Directory', use_cases.get_kdc_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0700,
                              recursive=True)

    file_path = (use_cases.get_kdc_conf_dir(json_data) +
                 "/" +
                 use_cases.get_kdc_conf_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_kdc_conf_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0600)

    # Validate kadm5.acl file
    self.assertResourceCalled('Directory', use_cases.get_kadm5_acl_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0700,
                              recursive=True)

    file_path = (use_cases.get_kadm5_acl_dir(json_data) +
                 "/" +
                 use_cases.get_kadm5_acl_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_kadm5_acl_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0600)