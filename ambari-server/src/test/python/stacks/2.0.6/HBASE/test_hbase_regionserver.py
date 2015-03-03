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

@patch("os.path.exists", new = MagicMock(return_value=True))
class TestHbaseRegionServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HBASE/0.96.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "configure",
                   config_file="default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assert_configure_default()
    self.assertNoMoreResources()
    
  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "start",
                   config_file="default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assert_configure_default()
    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf start regionserver',
      not_if = 'ls /var/run/hbase/hbase-hbase-regionserver.pid >/dev/null 2>&1 && ps -p `cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1',
      user = 'hbase'
    )
    self.assertNoMoreResources()
    
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "stop",
                   config_file="default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf stop regionserver',
        on_timeout = '! ( ls /var/run/hbase/hbase-hbase-regionserver.pid >/dev/null 2>&1 && ps -p `cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1 ) || ambari-sudo.sh -H -E kill -9 `cat /var/run/hbase/hbase-hbase-regionserver.pid`',
        timeout = 30,
        user = 'hbase',
    )
    
    self.assertResourceCalled('Execute', 'rm -f /var/run/hbase/hbase-hbase-regionserver.pid',
    )
    self.assertNoMoreResources()
    
  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "configure",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assert_configure_secured()
    self.assertNoMoreResources()
    
  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "start",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf start regionserver',
      not_if = 'ls /var/run/hbase/hbase-hbase-regionserver.pid >/dev/null 2>&1 && ps -p `cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1',
      user = 'hbase',
    )
    self.assertNoMoreResources()
    
  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "stop",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf stop regionserver',
        on_timeout = '! ( ls /var/run/hbase/hbase-hbase-regionserver.pid >/dev/null 2>&1 && ps -p `cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1 ) || ambari-sudo.sh -H -E kill -9 `cat /var/run/hbase/hbase-hbase-regionserver.pid`',
        timeout = 30,
        user = 'hbase',
    )
    
    self.assertResourceCalled('Execute', 'rm -f /var/run/hbase/hbase-hbase-regionserver.pid',
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755
    )
    self.assertResourceCalled('Directory', '/etc/hbase/conf',
      owner = 'hbase',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/hbase',
      owner = 'hbase',
      mode=0775,
      recursive = True,
      cd_access='a'
    )
    self.assertResourceCalled('Directory', '/hadoop/hbase/local',
      owner = 'hbase',
      group = 'hadoop',
      mode=0775,
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/hbase/local/jars',
      owner = 'hbase',
      group = 'hadoop',
      mode=0775,
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hbase-site']
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(self.getConfig()['configurations']['hbase-env']['content']),
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-RS',
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/regionservers',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('Directory', '/var/run/hbase',
      owner = 'hbase',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      recursive = True,
    )
    self.assertResourceCalled('File',
                              '/etc/hbase/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hbase',
                              content='log4jproperties\nline2'
    )
    self.assertResourceCalled('HdfsDirectory', 'hdfs://c6401.ambari.apache.org:8020/apps/hbase/data',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              owner = 'hbase',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/apps/hbase/staging',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0711,
                              owner = 'hbase',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              bin_dir = '/usr/bin',
                              action = ['create'],
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755
    )
    self.assertResourceCalled('Directory', '/etc/hbase/conf',
      owner = 'hbase',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/hbase',
      owner = 'hbase',
      mode=0775,
      recursive = True,
      cd_access='a'
    )
    self.assertResourceCalled('Directory', '/hadoop/hbase/local',
      owner = 'hbase',
      group = 'hadoop',
      mode=0775,
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/hbase/local/jars',
      owner = 'hbase',
      group = 'hadoop',
      mode=0775,
      recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hbase-site']
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(self.getConfig()['configurations']['hbase-env']['content']),
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-RS',
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/regionservers',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hbase_regionserver_jaas.conf',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('Directory', '/var/run/hbase',
      owner = 'hbase',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      recursive = True,
    )
    self.assertResourceCalled('File',
                              '/etc/hbase/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hbase',
                              content='log4jproperties\nline2'
    )
    self.assertResourceCalled('HdfsDirectory', 'hdfs://c6401.ambari.apache.org:8020/apps/hbase/data',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              owner = 'hbase',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/apps/hbase/staging',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0711,
                              owner = 'hbase',
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

  def test_start_default_22(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "start",
                   config_file="hbase-rs-2.2.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES)
    
    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755)

    self.assertResourceCalled('Directory', '/etc/hbase/conf',
      owner = 'hbase',
      group = 'hadoop',
      recursive = True)

    self.assertResourceCalled('Directory', '/hadoop/hbase',
      owner = 'hbase',
      mode = 0775,
      recursive = True,
      cd_access='a')

    self.assertResourceCalled('Directory', '/hadoop/hbase/local',
      owner = 'hbase',
      group = 'hadoop',
      mode=0775,
      recursive = True)

    self.assertResourceCalled('Directory', '/hadoop/hbase/local/jars',
      owner = 'hbase',
      group = 'hadoop',
      mode=0775,
      recursive = True)

    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hbase-site'])
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hbase',
                              group = 'hadoop',
                              conf_dir = '/etc/hbase/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site'])

    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site'])

    self.assertResourceCalled('XmlConfig', 'hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hbase-policy'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hbase-policy'])

    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(self.getConfig()['configurations']['hbase-env']['content']))

    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-RS')

    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/regionservers',
      owner = 'hbase',
      template_tag = None)

    self.assertResourceCalled('Directory', '/var/run/hbase',
      owner = 'hbase',
      recursive = True)

    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      recursive = True)

    self.assertResourceCalled('File',
                              '/etc/hbase/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hbase',
                              content='log4jproperties\nline2')

    self.assertResourceCalled('HdfsDirectory', 'hdfs://nn1/apps/hbase/data',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              owner = 'hbase',
                              bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              action = ['create_delayed'])

    self.assertResourceCalled('HdfsDirectory', '/apps/hbase/staging',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              mode = 0711,
                              owner = 'hbase',
                              bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              action = ['create_delayed'])

    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit",
                              bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              action = ['create'])

    self.assertResourceCalled('Execute', '/usr/hdp/current/hbase-regionserver/bin/hbase-daemon.sh --config /etc/hbase/conf start regionserver',
      not_if = 'ls /var/run/hbase/hbase-hbase-regionserver.pid >/dev/null 2>&1 && ps -p `cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1',
      user = 'hbase')

    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'hbase-site': {
        'hbase.regionserver.keytab.file': '/path/to/hbase_keytab',
        'hbase.regionserver.kerberos.principal': 'hbase_principal'
      }
    }

    result_issues = []
    props_value_check = {"hbase.security.authentication": "kerberos",
                           "hbase.security.authorization": "true"}
    props_empty_check = ["hbase.regionserver.keytab.file",
                           "hbase.regionserver.kerberos.principal"]

    props_read_check = ["hbase.regionserver.keytab.file"]

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "security_status",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('hbase-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    cached_kinit_executor_mock.called_with('/usr/bin/kinit',
                                           self.config_dict['configurations']['hbase-env']['hbase_user'],
                                           security_params['hbase-site']['hbase.regionserver.keytab.file'],
                                           security_params['hbase-site']['hbase.regionserver.kerberos.principal'],
                                           self.config_dict['hostname'],
                                           '/tmp')

     # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "security_status",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains hbase-site
    empty_security_params = {}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "security_status",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'hbase-site' : "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "security_status",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "security_status",
                   config_file="default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})
