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
from utils import get_property_value, get_unstructured_data



from stacks.utils.RMFTestCase import *

class TestKerberosClient(RMFTestCase):
  def test_configure_managed_kdc(self):
    json_data = use_cases.get_manged_kdc_use_case()

    self.executeScript("2.2/services/KERBEROS/package/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="configure",
                       config_dict=json_data)

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

  def test_configure_unmanaged_kdc(self):
    json_data = use_cases.get_unmanged_kdc_use_case()

    self.executeScript("2.2/services/KERBEROS/package/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="configure",
                       config_dict=json_data)

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

  def test_configure_unmanaged_ad(self):
    json_data = use_cases.get_unmanged_ad_use_case()

    self.executeScript("2.2/services/KERBEROS/package/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="configure",
                       config_dict=json_data)

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

  def test_configure_cross_realm_trust(self):
    json_data = use_cases.get_cross_realm_use_case()

    self.executeScript("2.2/services/KERBEROS/package/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="configure",
                       config_dict=json_data)

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


  def test_get_property(self):
    d = {
      'non_empty' : "Nonempty value",
      'unicode_non_empty' : u"Nonempty value",
      'number' : 33,
      'number_string' : "33",
      'empty' : "",
      'unicode_empty' : u"",
      'whitespace' : "    ",
      'unicode_whitespace' : u"    ",
      'none' : None,
      }

    self.assertEqual('Nonempty value', get_property_value(d, 'non_empty'))
    self.assertEqual('Nonempty value', get_property_value(d, 'non_empty', None, True, None))

    self.assertEqual('Nonempty value', get_property_value(d, 'unicode_non_empty'))
    self.assertEqual('Nonempty value', get_property_value(d, 'unicode_non_empty', None, True, None))

    self.assertEqual('33', get_property_value(d, 'number_string'))
    self.assertEqual('33', get_property_value(d, 'number_string', None, True, None))

    self.assertEqual(33, get_property_value(d, 'number'))
    self.assertEqual(33, get_property_value(d, 'number', None, True, None))

    self.assertEqual('', get_property_value(d, 'empty'))
    self.assertEqual("I'm empty", get_property_value(d, 'empty', None, True, "I'm empty"))
    self.assertEqual('', get_property_value(d, 'empty', None, False, "I'm empty"))

    self.assertEqual('', get_property_value(d, 'unicode_empty'))
    self.assertEqual("I'm empty", get_property_value(d, 'unicode_empty', None, True, "I'm empty"))
    self.assertEqual('', get_property_value(d, 'unicode_empty', None, False, "I'm empty"))

    self.assertEqual("    ", get_property_value(d, 'whitespace'))
    self.assertEqual("I'm empty", get_property_value(d, 'whitespace', None, True, "I'm empty"))
    self.assertEqual('    ', get_property_value(d, 'whitespace', None, False, "I'm empty"))

    self.assertEqual("    ", get_property_value(d, 'unicode_whitespace'))
    self.assertEqual("I'm empty", get_property_value(d, 'unicode_whitespace', None, True, "I'm empty"))
    self.assertEqual('    ', get_property_value(d, 'unicode_whitespace', None, False, "I'm empty"))

    self.assertEqual(None, get_property_value(d, 'none'))
    self.assertEqual("I'm empty", get_property_value(d, 'none', None, True, "I'm empty"))
    self.assertEqual(None, get_property_value(d, 'none', None, False, "I'm empty"))
    self.assertEqual("I'm empty", get_property_value(d, 'none', '', True, "I'm empty"))
    self.assertEqual("", get_property_value(d, 'none', '', False, "I'm empty"))
