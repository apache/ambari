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
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *

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
    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh start',
                              not_if = 'ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1',
                              user = 'hcat'
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

    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh stop',
                              user = 'hcat',
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

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "start",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh start',
                              not_if = 'ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1',
                              user = 'hcat'
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

    self.assertResourceCalled('Execute', 'env HADOOP_HOME=/usr /usr/lib/hcatalog/sbin/webhcat_server.sh stop',
                              user = 'hcat',
                              )
    self.assertResourceCalled('File', '/var/run/webhcat/webhcat.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('HdfsDirectory', '/apps/webhcat',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 0755,
                              owner = 'hcat',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/user/hcat',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 0755,
                              owner = 'hcat',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              bin_dir = '/usr/bin',
                              action = ['create'],
                              )
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
    self.assertResourceCalled('Directory', '/etc/hcatalog/conf',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('CopyFromLocal', '/usr/lib/hadoop-mapreduce/hadoop-streaming-*.jar',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/pig.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/hive.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='',
                              hadoop_bin_dir='/usr/bin',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/sqoop*.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='',
                              hadoop_bin_dir='/usr/bin',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
                              owner = 'hcat',
                              group = 'hadoop',
                              conf_dir = '/etc/hcatalog/conf',
                              configurations = self.getConfig()['configurations']['webhcat-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['webhcat-site']
    )
    self.assertResourceCalled('File', '/etc/hcatalog/conf/webhcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['webhcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hcatalog/conf/webhcat-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hcat',
                              group = 'hadoop',
                              mode = 0644,
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('HdfsDirectory', '/apps/webhcat',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0755,
                              owner = 'hcat',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/user/hcat',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0755,
                              owner = 'hcat',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              bin_dir = '/usr/bin',
                              action = ['create'],
                              )
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
    self.assertResourceCalled('Directory', '/etc/hcatalog/conf',
                              owner = 'hcat',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              path = ['/bin'],
                              user = 'hcat',
                              )
    self.assertResourceCalled('CopyFromLocal', '/usr/lib/hadoop-mapreduce/hadoop-streaming-*.jar',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/pig.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/hive.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('CopyFromLocal', '/usr/share/HDP-webhcat/sqoop*.tar.gz',
                              owner='hcat',
                              mode=0755,
                              dest_dir='/apps/webhcat',
                              kinnit_if_needed='/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs;',
                              hadoop_conf_dir='/etc/hadoop/conf',
                              hadoop_bin_dir='/usr/bin',
                              hdfs_user='hdfs'
    )
    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
                              owner = 'hcat',
                              group = 'hadoop',
                              conf_dir = '/etc/hcatalog/conf',
                              configurations = self.getConfig()['configurations']['webhcat-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['webhcat-site']
    )
    self.assertResourceCalled('File', '/etc/hcatalog/conf/webhcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['webhcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hcatalog/conf/webhcat-log4j.properties',
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