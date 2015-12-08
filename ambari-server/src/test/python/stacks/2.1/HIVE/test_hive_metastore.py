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

import json
import os

from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.script.script import Script

@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch("resource_management.libraries.functions.get_user_call_output.get_user_call_output", new=MagicMock(return_value=(0,'123','')))
class TestHiveMetastore(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "configure",
                       config_file="../../2.1/configs/default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       config_file="../../2.1/configs/default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('Execute', '/tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.log /var/run/hive/hive.pid /etc/hive/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr',
           'HIVE_BIN': 'hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/lib/hive/bin:/usr/bin'],
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive aaa com.mysql.jdbc.Driver',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        tries = 5,
        try_sleep = 10,
    )

    self.assertNoMoreResources()

  @patch("os.umask")
  def test_start_default_umask_027(self, umask_mock):
    umask_mock.return_value = 027
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       config_file="../../2.1/configs/default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('Execute', '/tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.log /var/run/hive/hive.pid /etc/hive/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr',
           'HIVE_BIN': 'hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/lib/hive/bin:/usr/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive aaa com.mysql.jdbc.Driver',
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries = 5,
                              try_sleep = 10,
                              )

    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "stop",
                       config_file="../../2.1/configs/default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
    )
    self.assertResourceCalled('Execute', "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
        tries = 20,
        try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "configure",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', '/tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.log /var/run/hive/hive.pid /etc/hive/conf.server /var/log/hive',
        environment = {'HADOOP_HOME': '/usr',
           'HIVE_BIN': 'hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/lib/hive/bin:/usr/bin'],
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive asd com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )

    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "stop",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
    )
    self.assertResourceCalled('Execute', "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
        tries = 20,
        try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive.pid',
      action = ['delete'],
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf.server',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                     u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                     u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content = Template('hive.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/lib/hive/lib//mysql-connector-java.jar'),
                              path = ['/bin', '/usr/bin/'],
                              sudo = True,
                              )
    self.assertResourceCalled('File', '/usr/lib/hive/lib//mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
        mode = 0644,
    )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
                              content = StaticFile('startMetastore.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', 'export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/lib/hive/bin/schematool -initSchema -dbType mysql -userName hive -passWord aaa',
        not_if = "ambari-sudo.sh su hive -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/lib/hive/bin/schematool -info -dbType mysql -userName hive -passWord aaa'",
        user = 'hive',
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access = 'a',
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf.server',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                     u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                     u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content = Template('hive.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/lib/hive/lib//mysql-connector-java.jar'),
                              path = ['/bin', '/usr/bin/'],
                              sudo = True,
                              )
    self.assertResourceCalled('File', '/usr/lib/hive/lib//mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
        mode = 0644,
    )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
                              content = StaticFile('startMetastore.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', 'export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/lib/hive/bin/schematool -initSchema -dbType mysql -userName hive -passWord asd',
        not_if = "ambari-sudo.sh su hive -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]export HIVE_CONF_DIR=/etc/hive/conf.server ; /usr/lib/hive/bin/schematool -info -dbType mysql -userName hive -passWord asd'",
        user = 'hive',
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access = 'a',
                              )

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'hive-site': {
        'hive.server2.authentication': "KERBEROS",
        'hive.metastore.sasl.enabled': "true",
        'hive.security.authorization.enabled': 'true',
        'hive.metastore.kerberos.keytab.file': 'path/to/keytab',
        'hive.metastore.kerberos.principal': 'principal'
      }
    }
    result_issues = []
    props_value_check = {
      'hive.server2.authentication': "KERBEROS",
      'hive.metastore.sasl.enabled': "true",
      'hive.security.authorization.enabled': 'true'
    }
    props_empty_check = [
      'hive.metastore.kerberos.keytab.file',
      'hive.metastore.kerberos.principal'
    ]
    props_read_check = [
      'hive.metastore.kerberos.keytab.file'
    ]

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    get_params_mock.assert_called_with("/etc/hive/conf", {'hive-site.xml': "XML"})
    build_exp_mock.assert_called_with('hive-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['hive-env']['hive_user'],
                                                  security_params['hive-site']['hive.metastore.kerberos.keytab.file'],
                                                  security_params['hive-site']['hive.metastore.kerberos.principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                         classname = "HiveMetastore",
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

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
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

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
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
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('hdp-select', 'set', 'hive-metastore', version), sudo=True,)
    self.assertNoMoreResources()

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_23(self, call_mock, os_path__exists_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    os_path__exists_mock.return_value = False
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Link', ('/etc/hive/conf'), to='/usr/hdp/current/hive-client/conf')
    self.assertResourceCalled('Execute',
                              ('hdp-select', 'set', 'hive-metastore', version), sudo=True,)
    self.assertNoMoreResources()

    self.assertEquals(1, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('conf-select', 'set-conf-dir', '--package', 'hive', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('conf-select', 'create-conf-dir', '--package', 'hive', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])

  def test_pre_upgrade_restart_ims(self):
    """
    Tests the state of the init_metastore_schema property on update
    """
    config_file = self.get_src_folder() + "/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    # first try it with a normal, non-upgrade
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
      classname = "HiveMetastore",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, ''), (0, None)])

    self.assertEquals(True, RMFTestCase.env.config["params"]["init_metastore_schema"])

    self.config_dict = None
    config_file = self.get_src_folder() + "/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    json_content['commandParams']['version'] = '2.3.0.0-1234'
    json_content['commandParams']['upgrade_direction'] = Direction.UPGRADE
    json_content['hostLevelParams']['stack_version'] = '2.3.0.0-0'

    # now try it in an upgrade
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
      classname = "HiveMetastore",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, ''), (0, None)])

    self.assertEquals(False, RMFTestCase.env.config["params"]["init_metastore_schema"])

    self.config_dict = None
    config_file = self.get_src_folder() + "/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    json_content['commandParams']['upgrade_direction'] = Direction.DOWNGRADE

    # now try it in a downgrade
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
      classname = "HiveMetastore",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, ''), (0, None)])

    self.assertEquals(False, RMFTestCase.env.config["params"]["init_metastore_schema"])


  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch("resource_management.libraries.functions.get_hdp_version")
  def test_upgrade_metastore_schema(self, get_hdp_version_mock, call_mock, os_path_exists_mock):
    get_hdp_version_mock.return_value = '2.3.0.0-1234'

    def side_effect(path):
      if path == "/usr/hdp/2.2.7.0-1234/hive-server2/lib/mysql-connector-java.jar":
        return True
      if ".j2" in path:
        return True
      return False

    os_path_exists_mock.side_effect = side_effect

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    # must be HDP 2.3+
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['commandParams']['upgrade_direction'] = Direction.UPGRADE
    json_content['hostLevelParams']['stack_version'] = "2.3"
    json_content['hostLevelParams']['current_version'] = "2.2.7.0-1234"


    # trigger the code to think it needs to copy the JAR
    json_content['configurations']['hive-site']['javax.jdo.option.ConnectionDriverName'] = "com.mysql.jdbc.Driver"
    json_content['configurations']['hive-env']['hive_database'] = "Existing"

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
      classname = "HiveMetastore",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, ''), (0, None)],
      mocks_dict = mocks_dict)

    # we don't care about configure here - the strings are different anyway because this
    # is an upgrade, so just pop those resources off of the call stack
    self.assertResourceCalledIgnoreEarlier('Directory', '/var/lib/hive', owner = 'hive', group = 'hadoop',
      mode = 0755, recursive = True, cd_access = 'a')

    self.assertResourceCalled('Execute', ('rm', '-f', '/usr/hdp/current/hive-server2/lib/ojdbc6.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )

    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-jdbc-driver.jar'),
    )
    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/tmp/mysql-connector-java.jar',
     '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )

    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('Execute', ('cp',
     '/usr/hdp/2.2.7.0-1234/hive/lib/mysql-connector-java.jar',
     '/usr/hdp/2.3.0.0-1234/hive/lib'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/2.3.0.0-1234/hive/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('Execute', '/usr/hdp/2.3.0.0-1234/hive/bin/schematool -dbType mysql -upgradeSchema',
        logoutput = True,
        environment = {'HIVE_CONF_DIR': '/etc/hive/conf.server'},
        tries = 1,
        user = 'hive',
    )
    self.assertResourceCalled('Link', ('/etc/hive/conf'), to='/usr/hdp/current/hive-client/conf')
    self.assertResourceCalled('Execute', ('hdp-select', 'set', 'hive-metastore', '2.3.0.0-1234'),
        sudo = True,
    )
    self.assertNoMoreResources()

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch("resource_management.libraries.functions.get_hdp_version")
  def test_upgrade_metastore_schema_using_new_db(self, get_hdp_version_mock, call_mock, os_path_exists_mock):
    get_hdp_version_mock.return_value = '2.3.0.0-1234'

    def side_effect(path):
      if path == "/usr/hdp/2.2.7.0-1234/hive-server2/lib/mysql-connector-java.jar":
        return True
      if path == "/usr/hdp/current/hive-client/lib/mysql-connector-java.jar":
        return True
      if ".j2" in path:
        return True
      return False

    os_path_exists_mock.side_effect = side_effect

    config_file = self.get_src_folder()+"/test/python/stacks/2.1/configs/hive-metastore-upgrade.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
      classname = "HiveMetastore",
      command = "upgrade_schema",
      config_dict = json_content,
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, ''), (0, None)],
      mocks_dict = mocks_dict)


    # we don't care about configure here - the strings are different anyway because this
    # is an upgrade, so just pop those resources off of the call stack
    self.assertResourceCalledIgnoreEarlier('Directory', '/var/lib/hive', owner = 'hive', group = 'hadoop',
      mode = 0755, recursive = True, cd_access = 'a')

    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/usr/share/java/mysql-connector-java.jar',
     '/usr/hdp/current/hive-metastore/lib/mysql-connector-java.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-metastore/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('Execute', ('cp',
     '/usr/hdp/2.3.0.0-2557/hive/lib/mysql-connector-java.jar',
     '/usr/hdp/2.3.2.0-2950/hive/lib'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/2.3.2.0-2950/hive/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('Execute', '/usr/hdp/2.3.2.0-2950/hive/bin/schematool -dbType mysql -upgradeSchema',
        logoutput = True,
        environment = {'HIVE_CONF_DIR': '/usr/hdp/current/hive-metastore/conf/conf.server'},
        tries = 1,
        user = 'hive',
    )

    self.assertNoMoreResources()

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch("resource_management.libraries.functions.get_hdp_version")
  def test_upgrade_sqla_metastore_schema_with_jdbc_download(self, get_hdp_version_mock, call_mock, os_path_exists_mock):

    get_hdp_version_mock.return_value = '2.3.0.0-1234'

    def side_effect(path):
      if path == "/usr/hdp/2.2.7.0-1234/hive-server2/lib/mysql-connector-java.jar":
        return True
      if ".j2" in path:
        return True
      return False

    os_path_exists_mock.side_effect = side_effect

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    # must be HDP 2.3+
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['commandParams']['upgrade_direction'] = Direction.UPGRADE
    json_content['hostLevelParams']['stack_version'] = "2.3"

    # trigger the code to think it needs to copy the JAR
    json_content['configurations']['hive-site']['javax.jdo.option.ConnectionDriverName'] = "sap.jdbc4.sqlanywhere.IDriver"
    json_content['configurations']['hive-env']['hive_database'] = "Existing"
    json_content['configurations']['hive-env']['hive_database_type'] = "sqlanywhere"

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict = mocks_dict)

    # we don't care about configure here - the strings are different anyway because this
    # is an upgrade, so just pop those resources off of the call stack
    self.assertResourceCalledIgnoreEarlier('Directory', '/var/lib/hive', owner = 'hive', group = 'hadoop',
      mode = 0755, recursive = True, cd_access = 'a')

    self.assertResourceCalled('Execute',
                              ('rm', '-f', '/usr/hdp/current/hive-server2/lib/ojdbc6.jar'),
                              path=["/bin", "/usr/bin/"],
                              sudo = True)

    self.assertResourceCalled('File',
                              '/tmp/sqla-client-jdbc.tar.gz',
                              content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//sqlanywhere-jdbc-driver.tar.gz')
                              )

    self.assertResourceCalled('Execute',
                              ('tar', '-xvf', '/tmp/sqla-client-jdbc.tar.gz', '-C', '/tmp'),
                              sudo = True)

    self.assertResourceCalled('Execute',
                              ('yes | ambari-sudo.sh cp /tmp/sqla-client-jdbc/java/* /usr/hdp/current/hive-server2/lib'))

    self.assertResourceCalled('Directory',
                              '/usr/hdp/current/hive-server2/lib/native/lib64',
                              recursive = True)

    self.assertResourceCalled('Execute',
                              ('yes | ambari-sudo.sh cp /tmp/sqla-client-jdbc/native/lib64/* /usr/hdp/current/hive-server2/lib/native/lib64'))

    self.assertResourceCalled('Execute',
                              ('ambari-sudo.sh chown -R hive:hadoop /usr/hdp/current/hive-server2/lib/*'))

    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/lib/sajdbc4.jar',
                              mode = 0644,
                              )

    self.assertResourceCalled('Execute',
                              ('yes | ambari-sudo.sh cp /usr/hdp/current/hive-server2/lib/*.jar /usr/hdp/2.3.0.0-1234/hive/lib'))

    self.assertResourceCalled('Directory',
                              '/usr/hdp/2.3.0.0-1234/hive/lib/native/lib64',
                              recursive = True)

    self.assertResourceCalled('Execute',
                              ('yes | ambari-sudo.sh cp /usr/hdp/current/hive-server2/lib/native/lib64/* /usr/hdp/2.3.0.0-1234/hive/lib/native/lib64'))

    self.assertResourceCalled('Execute',
                              ('ambari-sudo.sh chown -R hive:hadoop /usr/hdp/current/hive-server2/lib/*'))

    self.assertResourceCalled('File', '/usr/hdp/2.3.0.0-1234/hive/lib/sajdbc4.jar',
                              mode = 0644,
                              )

    self.assertResourceCalled('Execute', "/usr/hdp/2.3.0.0-1234/hive/bin/schematool -dbType sqlanywhere -upgradeSchema",
                              logoutput = True, environment = {'HIVE_CONF_DIR': '/usr/hdp/current/hive-server2/conf/conf.server'},
                              tries = 1, user = 'hive')

    self.assertResourceCalled('Link', ('/etc/hive/conf'), to='/usr/hdp/current/hive-client/conf')

    self.assertResourceCalled('Execute', ('hdp-select', 'set', 'hive-metastore', version), sudo=True,)

    self.assertNoMoreResources()
