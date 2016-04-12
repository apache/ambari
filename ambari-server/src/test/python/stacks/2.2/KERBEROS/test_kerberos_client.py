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

import json
from mock.mock import MagicMock, patch
import os
import sys
import use_cases
from stacks.utils.RMFTestCase import *

from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestKerberosClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "KERBEROS/1.10.3-10/package"
  STACK_VERSION = "2.2"

  def test_configure_managed_kdc(self):
    json_data = use_cases.get_manged_kdc_use_case()

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="configure",
                       config_dict=json_data,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', use_cases.get_krb5_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0755,
                              create_parents = True)

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

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="configure",
                       config_dict=json_data,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', use_cases.get_krb5_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0755,
                              create_parents = True)

    file_path = (use_cases.get_krb5_conf_dir(json_data) +
                 "/" +
                 use_cases.get_krb5_conf_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_krb5_conf_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0644)

  def test_configure_unmanaged_kdc_and_krb5conf(self):
    json_data = use_cases.get_unmanged_krb5conf_use_case()


    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="configure",
                       config_dict=json_data,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/lib/ambari-agent/tmp/curl_krb_cache', action=["delete"],
                              )
    self.assertResourceCalled('Directory', '/tmp/AMBARI-artifacts/',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/tmp/AMBARI-artifacts//UnlimitedJCEPolicyJDK7.zip',
                            content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//UnlimitedJCEPolicyJDK7.zip'),
                            )
    self.assertNoMoreResources()

  def test_configure_unmanaged_ad(self):
    json_data = use_cases.get_unmanged_ad_use_case()

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="configure",
                       config_dict=json_data,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', use_cases.get_krb5_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0755,
                              create_parents = True)

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

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="configure",
                       config_dict=json_data,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', use_cases.get_krb5_conf_dir(json_data),
                              owner='root',
                              group='root',
                              mode=0755,
                              create_parents = True)

    file_path = (use_cases.get_krb5_conf_dir(json_data) +
                 "/" +
                 use_cases.get_krb5_conf_file(json_data))
    self.assertResourceCalled('File', file_path,
                              content=InlineTemplate(use_cases.get_krb5_conf_template(json_data)),
                              owner='root',
                              group='root',
                              mode=0644)


  def test_get_property(self):
    package_dir = os.path.join(RMFTestCase._getCommonServicesFolder(), self.COMMON_SERVICES_PACKAGE_DIR)
    scripts_dir = os.path.join(package_dir, "scripts")
    sys.path += [scripts_dir]
    from utils import get_property_value

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

  def test_set_keytab(self):
    import base64

    config_file = "stacks/2.2/configs/default.json"
    with open(config_file, "r") as f:
      json_data = json.load(f)

    json_data['kerberosCommandParams'] = []
    json_data['kerberosCommandParams'].append({
      "keytab_file_configuration": "hdfs-site/dfs.web.authentication.kerberos.keytab",
      "service": "HDFS",
      "keytab_content_base64": "BQIAAABbAAIAC0VYQU1QTEUuQ09NAARIVFRQABdjNjU"
                               "wMS5hbWJhcmkuYXBhY2hlLm9yZwAAAAFUodgKAQASAC"
                               "A5N4gKUJsizCzwRD11Q/6sdZhJjlJmuuMeMKw/WefIb"
                               "gAAAFMAAgALRVhBTVBMRS5DT00ABEhUVFAAF2M2NTAx"
                               "LmFtYmFyaS5hcGFjaGUub3JnAAAAAVSh2AoBABAAGLA"
                               "3huUxDmRK2da5Z7WPZ+zTbdnBkXCrKgAAAEsAAgALRV"
                               "hBTVBMRS5DT00ABEhUVFAAF2M2NTAxLmFtYmFyaS5hc"
                               "GFjaGUub3JnAAAAAVSh2AoBABcAEIT0yzbx1fnhmuaG"
                               "5qtg444AAABDAAIAC0VYQU1QTEUuQ09NAARIVFRQABd"
                               "jNjUwMS5hbWJhcmkuYXBhY2hlLm9yZwAAAAFUodgKAQ"
                               "ADAAiov1LleuaMgwAAAEsAAgALRVhBTVBMRS5DT00AB"
                               "EhUVFAAF2M2NTAxLmFtYmFyaS5hcGFjaGUub3JnAAAA"
                               "AVSh2AoBABEAECBTe9uCaSiPxnoGRldhAks=",
      "keytab_file_group_access": "r",
      "hostname": "c6501.ambari.apache.org",
      "component": "NAMENODE",
      "keytab_file_owner_name": "root",
      "keytab_file_path": "/etc/security/keytabs/spnego.service.keytab",
      "principal_configuration": "hdfs-site/dfs.web.authentication.kerberos.principal",
      "keytab_file_owner_access": "r",
      "keytab_file_group_name": "hadoop",
      "principal": "HTTP/_HOST@EXAMPLE.COM"
    })

    json_data['kerberosCommandParams'].append({
      "keytab_file_configuration": "cluster-env/smokeuser_keytab",
      "service": "HDFS",
      "keytab_content_base64": "BQIAAABHAAEAC0VYQU1QTEUuQ09NAAlhbWJhcmktcWEAAAA"
                               "BVKHYCgEAEgAg3OBDOecGoznTHZiPwmlmK4TI6bdRdrl/6q"
                               "TV8Kml2TAAAAA/AAEAC0VYQU1QTEUuQ09NAAlhbWJhcmktc"
                               "WEAAAABVKHYCgEAEAAYzqEjkX/xDoO8ij0cJmc3ZG7Qfzgl"
                               "/SN2AAAANwABAAtFWEFNUExFLkNPTQAJYW1iYXJpLXFhAAA"
                               "AAVSh2AoBABcAEHzLG1kfqxhEoTe4erUldvQAAAAvAAEAC0"
                               "VYQU1QTEUuQ09NAAlhbWJhcmktcWEAAAABVKHYCgEAAwAIO"
                               "PK6UkwyUSMAAAA3AAEAC0VYQU1QTEUuQ09NAAlhbWJhcmkt"
                               "cWEAAAABVKHYCgEAEQAQVqISRJwXIQnG28lI34mfeA==",
      "keytab_file_group_access": "",
      "hostname": "c6501.ambari.apache.org",
      "component": "NAMENODE",
      "keytab_file_owner_name": "ambari-qa",
      "keytab_file_path": "/etc/security/keytabs/smokeuser.headless.keytab",
      "principal_configuration": "cluster-env/smokeuser_principal_name",
      "keytab_file_owner_access": "r",
      "keytab_file_group_name": "hadoop",
      "principal": "ambari-qa@EXAMPLE.COM"
    })

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="set_keytab",
                       config_dict=json_data,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', "/etc/security/keytabs",
                              owner='root',
                              group='root',
                              mode=0755,
                              create_parents = True)

    self.assertResourceCalled('File', "/etc/security/keytabs/spnego.service.keytab",
                              owner='root',
                              group='hadoop',
                              mode=0440,
                              content=CallFunctionMock(call_result=base64.b64decode("BQIAAABbAAIAC0VYQU1QTEUuQ09NAARIVFRQABdjNjU"
                                                       "wMS5hbWJhcmkuYXBhY2hlLm9yZwAAAAFUodgKAQASAC"
                                                       "A5N4gKUJsizCzwRD11Q/6sdZhJjlJmuuMeMKw/WefIb"
                                                       "gAAAFMAAgALRVhBTVBMRS5DT00ABEhUVFAAF2M2NTAx"
                                                       "LmFtYmFyaS5hcGFjaGUub3JnAAAAAVSh2AoBABAAGLA"
                                                       "3huUxDmRK2da5Z7WPZ+zTbdnBkXCrKgAAAEsAAgALRV"
                                                       "hBTVBMRS5DT00ABEhUVFAAF2M2NTAxLmFtYmFyaS5hc"
                                                       "GFjaGUub3JnAAAAAVSh2AoBABcAEIT0yzbx1fnhmuaG"
                                                       "5qtg444AAABDAAIAC0VYQU1QTEUuQ09NAARIVFRQABd"
                                                       "jNjUwMS5hbWJhcmkuYXBhY2hlLm9yZwAAAAFUodgKAQ"
                                                       "ADAAiov1LleuaMgwAAAEsAAgALRVhBTVBMRS5DT00AB"
                                                       "EhUVFAAF2M2NTAxLmFtYmFyaS5hcGFjaGUub3JnAAAA"
                                                       "AVSh2AoBABEAECBTe9uCaSiPxnoGRldhAks="))
    )

    self.assertResourceCalled('Directory', "/etc/security/keytabs",
                              owner='root',
                              group='root',
                              mode=0755,
                              create_parents = True)

    self.assertResourceCalled('File', "/etc/security/keytabs/smokeuser.headless.keytab",
                          owner='ambari-qa',
                          group='hadoop',
                          mode=0400,
                          content=CallFunctionMock(call_result=base64.b64decode("BQIAAABHAAEAC0VYQU1QTEUuQ09NAAlhbWJhcmktcWEAAAA"
                                                   "BVKHYCgEAEgAg3OBDOecGoznTHZiPwmlmK4TI6bdRdrl/6q"
                                                   "TV8Kml2TAAAAA/AAEAC0VYQU1QTEUuQ09NAAlhbWJhcmktc"
                                                   "WEAAAABVKHYCgEAEAAYzqEjkX/xDoO8ij0cJmc3ZG7Qfzgl"
                                                   "/SN2AAAANwABAAtFWEFNUExFLkNPTQAJYW1iYXJpLXFhAAA"
                                                   "AAVSh2AoBABcAEHzLG1kfqxhEoTe4erUldvQAAAAvAAEAC0"
                                                   "VYQU1QTEUuQ09NAAlhbWJhcmktcWEAAAABVKHYCgEAAwAIO"
                                                   "PK6UkwyUSMAAAA3AAEAC0VYQU1QTEUuQ09NAAlhbWJhcmkt"
                                                   "cWEAAAABVKHYCgEAEQAQVqISRJwXIQnG28lI34mfeA=="))
    )

  def test_delete_keytab(self):
    config_file = "stacks/2.2/configs/default.json"

    with open(config_file, "r") as f:
      json_data = json.load(f)

    json_data['kerberosCommandParams'] = []
    json_data['kerberosCommandParams'].append({
      "service": "HDFS",
      "hostname": "c6501.ambari.apache.org",
      "component": "NAMENODE",
      "keytab_file_path": "/etc/security/keytabs/spnego.service.keytab",
      "principal": "HTTP/_HOST@EXAMPLE.COM"
    })

    json_data['kerberosCommandParams'].append({
      "service": "HDFS",
      "hostname": "c6501.ambari.apache.org",
      "component": "NAMENODE",
      "keytab_file_path": "/etc/security/keytabs/smokeuser.headless.keytab",
      "principal": "ambari-qa@EXAMPLE.COM"
    })

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="remove_keytab",
                       config_dict=json_data,
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('File', "/etc/security/keytabs/spnego.service.keytab",
                              action=['delete'])
    self.assertResourceCalled('File', "/etc/security/keytabs/smokeuser.headless.keytab",
                              action=['delete'])

  def test_kdc_host_backwards_compatibility(self):
    json_data = use_cases.get_unmanged_kdc_use_case()

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kerberos_client.py",
                       classname="KerberosClient",
                       command="configure",
                       config_dict=json_data,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )

    # The kdc_hosts is expected to be taken from the JSON configuration data as-is
    self.assertEquals('c6401.ambari.apache.org, c6402.ambari.apache.org', sys.modules['params'].kdc_hosts)

    # The kdc_host is expected to generated using kdc_hosts, but only the first host is used since
    # previous versions only knew how to handle a single KDC host
    self.assertEquals('c6401.ambari.apache.org', sys.modules['params'].kdc_host)
