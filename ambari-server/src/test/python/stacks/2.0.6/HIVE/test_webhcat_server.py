#!/usr/bin/env python

'''
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
'''
import json
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *
from resource_management.core.exceptions import Fail

@patch("os.path.isfile", new = MagicMock(return_value=True))
@patch("glob.glob", new = MagicMock(return_value=["one", "two"]))
class TestWebHCatServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'cd /var/run/webhcat ; /usr/lib/hive-hcatalog/sbin/webhcat_server.sh start',
        environment = {'HADOOP_HOME': '/usr'},
        not_if = "ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1'",
        user = 'hcat',
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', '/usr/lib/hive-hcatalog/sbin/webhcat_server.sh stop',
                              user = 'hcat',
                              environment = {'HADOOP_HOME': '/usr' }
                              )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `ambari-sudo.sh su hcat -l -s /bin/bash -c \'[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid\'`',
                              not_if = "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1) || ( sleep 10 && ! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1) )"
    )

    self.assertResourceCalled('Execute', "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1)",
                              tries=20,
                              try_sleep=3,
    )

    self.assertResourceCalled('File', '/var/run/webhcat/webhcat.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

    def test_configure_secured(self):
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                         classname = "WebHCatServer",
                         command = "configure",
                         config_file="secured.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )

      self.assert_configure_secured()
      self.assertNoMoreResources()

  @patch("webhcat_service.graceful_stop", new = MagicMock(side_effect=Fail))
  def test_stop_graceful_stop_failed(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `ambari-sudo.sh su hcat -l -s /bin/bash -c \'[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid\'`',
                              not_if = "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1) || ( sleep 10 && ! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1) )"
                              )

    self.assertResourceCalled('Execute', "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1)",
                              tries=20,
                              try_sleep=3,
                              )

    self.assertResourceCalled('File', '/var/run/webhcat/webhcat.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "start",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'cd /var/run/webhcat ; /usr/lib/hive-hcatalog/sbin/webhcat_server.sh start',
        environment = {'HADOOP_HOME': '/usr'},
        not_if = "ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1'",
        user = 'hcat',
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "stop",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', '/usr/lib/hive-hcatalog/sbin/webhcat_server.sh stop',
                              user = 'hcat',
                              environment = {'HADOOP_HOME': '/usr' }
                              )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `ambari-sudo.sh su hcat -l -s /bin/bash -c \'[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid\'`',
                              not_if = "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1) || ( sleep 10 && ! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1) )"
    )

    self.assertResourceCalled('Execute', "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1)",
                              tries=20,
                              try_sleep=3,
    )
    self.assertResourceCalled('File', '/var/run/webhcat/webhcat.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  @patch("webhcat_service.graceful_stop", new = MagicMock(side_effect=Fail))
  def test_stop_secured_graceful_stop_failed(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "stop",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `ambari-sudo.sh su hcat -l -s /bin/bash -c \'[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid\'`',
                              not_if = "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1) || ( sleep 10 && ! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1) )"
                              )

    self.assertResourceCalled('Execute', "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `ambari-sudo.sh su hcat -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]cat /var/run/webhcat/webhcat.pid'` >/dev/null 2>&1)",
                              tries=20,
                              try_sleep=3,
                              )
    self.assertResourceCalled('File', '/var/run/webhcat/webhcat.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/etc/hive-webhcat/conf',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a'
                              )
    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
                              owner = 'hcat',
                              group = 'hadoop',
                              conf_dir = '/etc/hive-webhcat/conf',
                              configurations = self.getConfig()['configurations']['webhcat-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['webhcat-site']
    )
    self.assertResourceCalled('File', '/etc/hive-webhcat/conf/webhcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['webhcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/hive-webhcat/conf',
        cd_access = 'a',
        recursive=True
    )
    self.assertResourceCalled('File', '/etc/hive-webhcat/conf/webhcat-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hcat',
                              group = 'hadoop',
                              mode = 0644,
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/var/run/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/etc/hive-webhcat/conf',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a'
                              )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              path = ['/bin'],
                              user = 'hcat',
                              )
    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
                              owner = 'hcat',
                              group = 'hadoop',
                              conf_dir = '/etc/hive-webhcat/conf',
                              configurations = self.getConfig()['configurations']['webhcat-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['webhcat-site']
    )
    self.assertResourceCalled('File', '/etc/hive-webhcat/conf/webhcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['webhcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/hive-webhcat/conf',
        cd_access = 'a',
        recursive=True
    )
    self.assertResourceCalled('File', '/etc/hive-webhcat/conf/webhcat-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hcat',
                              group = 'hadoop',
                              mode = 0644,
                              )

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'webhcat-site': {
        "templeton.kerberos.secret": "secret",
        "templeton.kerberos.keytab": 'path/to/keytab',
        "templeton.kerberos.principal": "principal"
      },
      "hive-site": {
        "hive.server2.authentication": "KERBEROS",
        "hive.metastore.sasl.enabled": "true",
        "hive.security.authorization.enabled": "true"
      }
    }
    result_issues = []
    webhcat_props_value_check = {"templeton.kerberos.secret": "secret"}
    webhcat_props_empty_check = ["templeton.kerberos.keytab",
                         "templeton.kerberos.principal"]
    webhcat_props_read_check = ["templeton.kerberos.keytab"]

    hive_props_value_check = {"hive.server2.authentication": "KERBEROS",
                         "hive.metastore.sasl.enabled": "true",
                         "hive.security.authorization.enabled": "true"}
    hive_props_empty_check = None
    hive_props_read_check = None

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('hive-site', hive_props_value_check, hive_props_empty_check, hive_props_read_check)
    # get_params_mock.assert_called_with(status_params.hive_conf_dir, {'hive-site.xml': "XML"})
    get_params_mock.assert_called_with('/etc/hive-webhcat/conf', {'webhcat-site.xml': "XML"})
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['hive-env']['webhcat_user'],
                                                  security_params['webhcat-site']['templeton.kerberos.keytab'],
                                                  security_params['webhcat-site']['templeton.kerberos.principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                         classname = "WebHCatServer",
                         command = "security_status",
                         config_file="../../2.1/configs/secured.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains startup
    empty_security_params = {}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'hive-site': "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "security_status",
                       config_file="../../2.1/configs/default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})


  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('hdp-select', 'set', 'hive-webhcat', version), sudo=True,)
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_23(self, call_mock):
    import sys

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_version'] = "2.3"

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None, '')],
                       mocks_dict = mocks_dict)

    self.assertTrue("params" in sys.modules)
    self.assertTrue(sys.modules["params"].webhcat_conf_dir is not None)
    self.assertTrue("/usr/hdp/current/hive-webhcat/etc/webhcat" == sys.modules["params"].webhcat_conf_dir)

    self.assertResourceCalledIgnoreEarlier('Execute',
                              ('hdp-select', 'set', 'hive-webhcat', version), sudo=True,)
    self.assertNoMoreResources()

    self.assertEquals(2, mocks_dict['call'].call_count)
    self.assertEquals(2, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('conf-select', 'set-conf-dir', '--package', 'hive-hcatalog', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('conf-select', 'create-conf-dir', '--package', 'hive-hcatalog', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])
    self.assertEquals(
      ('conf-select', 'set-conf-dir', '--package', 'hadoop', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[1][0][0])
    self.assertEquals(
      ('conf-select', 'create-conf-dir', '--package', 'hadoop', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[1][0][0])

  @patch("resource_management.core.shell.call")
  def test_rolling_restart_configure(self, call_mock):
    import sys

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_version'] = "2.3"

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
      classname = "WebHCatServer",
      command = "configure",
      config_dict = json_content,
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None), (0, None)],
      mocks_dict = mocks_dict)


    self.assertResourceCalled('Directory', '/var/run/webhcat',
      owner = 'hcat',
      group = 'hadoop',
      recursive = True,
      mode = 0755)

    self.assertResourceCalled('Directory', '/var/log/webhcat',
      owner = 'hcat',
      group = 'hadoop',
      recursive = True,
      mode = 0755)

    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-webhcat/etc/webhcat',
      owner = 'hcat',
      group = 'hadoop',
      recursive = True,
      cd_access = 'a',)

    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
      owner = 'hcat',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hive-webhcat/etc/webhcat',
      configurations = self.getConfig()['configurations']['webhcat-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['webhcat-site'])

    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
        owner = 'hive',
        group = 'hadoop',
        conf_dir = '/usr/hdp/2.3.0.0-1234/hive/conf',
        configuration_attributes = {u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                      u'javax.jdo.option.ConnectionDriverName': u'true',
                      u'javax.jdo.option.ConnectionPassword': u'true'}},
        configurations = self.getConfig()['configurations']['hive-site'],
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
        owner = 'yarn',
        group = 'hadoop',
        conf_dir = '/usr/hdp/2.3.0.0-1234/hadoop/conf',
        configuration_attributes = {u'final': {u'yarn.nodemanager.container-executor.class': u'true',
                      u'yarn.nodemanager.disk-health-checker.min-healthy-disks': u'true',
                      u'yarn.nodemanager.local-dirs': u'true'}},
        configurations = self.getConfig()['configurations']['yarn-site'],
    )
    
    self.assertResourceCalled('File', '/usr/hdp/current/hive-webhcat/etc/webhcat/webhcat-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['webhcat-env']['content']),
      owner = 'hcat',
      group = 'hadoop')

    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-webhcat/etc/webhcat',
      cd_access = 'a',
      recursive=True)

    self.assertResourceCalled('File', '/usr/hdp/current/hive-webhcat/etc/webhcat/webhcat-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hcat',
                              group = 'hadoop',
                              mode = 0644)

    self.assertNoMoreResources()

