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
import resource_management.libraries.functions.get_unique_id_and_date

@patch("os.path.exists", new = MagicMock(return_value=True))
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestZookeeperServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "ZOOKEEPER/3.4.5.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "configure",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "start",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'source /etc/zookeeper/conf/zookeeper-env.sh ; env ZOOCFGDIR=/etc/zookeeper/conf ZOOCFG=zoo.cfg /usr/lib/zookeeper/bin/zkServer.sh start',
                    not_if = 'ls /var/run/zookeeper/zookeeper_server.pid >/dev/null 2>&1 && ps -p `cat /var/run/zookeeper/zookeeper_server.pid` >/dev/null 2>&1',
                    user = 'zookeeper'
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "stop",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', 'source /etc/zookeeper/conf/zookeeper-env.sh ; env ZOOCFGDIR=/etc/zookeeper/conf ZOOCFG=zoo.cfg /usr/lib/zookeeper/bin/zkServer.sh stop',
      user = 'zookeeper',
    )
    self.assertResourceCalled('Execute', 'rm -f /var/run/zookeeper/zookeeper_server.pid')
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "configure",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "start",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'source /etc/zookeeper/conf/zookeeper-env.sh ; env ZOOCFGDIR=/etc/zookeeper/conf ZOOCFG=zoo.cfg /usr/lib/zookeeper/bin/zkServer.sh start',
                  not_if = 'ls /var/run/zookeeper/zookeeper_server.pid >/dev/null 2>&1 && ps -p `cat /var/run/zookeeper/zookeeper_server.pid` >/dev/null 2>&1',
                  user = 'zookeeper'
    )
    self.assertResourceCalled('Execute', "/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM;",
                              user="ambari-qa")
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "stop",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', 'source /etc/zookeeper/conf/zookeeper-env.sh ; env ZOOCFGDIR=/etc/zookeeper/conf ZOOCFG=zoo.cfg /usr/lib/zookeeper/bin/zkServer.sh stop',
                  user = 'zookeeper',
    )

    self.assertResourceCalled('Execute', 'rm -f /var/run/zookeeper/zookeeper_server.pid')
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/zookeeper/conf',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper-env.sh',
      owner = 'zookeeper',
      content = InlineTemplate(self.getConfig()['configurations']['zookeeper-env']['content']),
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo.cfg',
      owner = 'zookeeper',
      content = Template('zoo.cfg.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/configuration.xsl',
      owner = 'zookeeper',
      content = Template('configuration.xsl.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('Directory', '/var/run/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/log/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/hadoop/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
      cd_access='a',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/hadoop/zookeeper/myid',
      content = '1',
      mode = 0644,
    )
    self.assertResourceCalled('File',
                              '/etc/zookeeper/conf/log4j.properties',
                              content='log4jproperties\nline2',
                              mode=0644,
                              group='hadoop',
                              owner='zookeeper'
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo_sample.cfg',
      owner = 'zookeeper',
      group = 'hadoop',
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/zookeeper/conf',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper-env.sh',
      owner = 'zookeeper',
      content = InlineTemplate(self.getConfig()['configurations']['zookeeper-env']['content']),
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo.cfg',
      owner = 'zookeeper',
      content = Template('zoo.cfg.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/configuration.xsl',
      owner = 'zookeeper',
      content = Template('configuration.xsl.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('Directory', '/var/run/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/log/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/hadoop/zookeeper',
      owner = 'zookeeper',
      group = 'hadoop',
      recursive = True,
      cd_access='a',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/hadoop/zookeeper/myid',
      content = '1',
      mode = 0644,
    )
    self.assertResourceCalled('File',
                              '/etc/zookeeper/conf/log4j.properties',
                              content='log4jproperties\nline2',
                              mode=0644,
                              group='hadoop',
                              owner='zookeeper'
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper_jaas.conf',
      owner = 'zookeeper',
      content = Template('zookeeper_jaas.conf.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zookeeper_client_jaas.conf',
      owner = 'zookeeper',
      content = Template('zookeeper_client_jaas.conf.j2'),
      group = 'hadoop',
      mode = None,
    )
    self.assertResourceCalled('File', '/etc/zookeeper/conf/zoo_sample.cfg',
      owner = 'zookeeper',
      group = 'hadoop',
    )

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'zookeeper_jaas': {
        'Server': {
          'keyTab': 'path/to/zookeeper/service/keytab',
          'principal': 'zookeeper_keytab'
        }
      }
    }
    result_issues = []
    props_value_check = None
    props_empty_check = ['Server/keyTab', 'Server/principal']
    props_read_check = ['Server/keyTab']

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "security_status",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('zookeeper_jaas', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['zookeeper-env']['zk_user'],
                                                  security_params['zookeeper_jaas']['Server']['keyTab'],
                                                  security_params['zookeeper_jaas']['Server']['principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "security_status",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains zookeeper_jaas
    empty_security_params = {}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "security_status",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'zookeeper_jaas': "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "security_status",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "security_status",
                       config_file = "default.json",
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
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('hdp-select', 'set', 'zookeeper-server', version), sudo=True)
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_23(self, call_mock):
    call_mock.side_effects = [(0, None), (0, None)]

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-3242'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Link', ('/etc/zookeeper/conf'), to='/etc/zookeeper/conf.backup')
    self.assertResourceCalled('Execute',
                              ('hdp-select', 'set', 'zookeeper-server', version), sudo=True)

    self.assertEquals(1, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('conf-select', 'set-conf-dir', '--package', 'zookeeper', '--stack-version', '2.3.0.0-3242', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('conf-select', 'create-conf-dir', '--package', 'zookeeper', '--stack-version', '2.3.0.0-3242', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])

    self.assertNoMoreResources()

  @patch.object(resource_management.libraries.functions, "get_unique_id_and_date")
  def test_post_upgrade_restart(self, get_unique_id_and_date_mock):
    unique_value = "unique1"
    get_unique_id_and_date_mock.return_value = unique_value
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zookeeper_server.py",
                       classname = "ZookeeperServer",
                       command = "post_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [
                         (0, 'Created'),
                         (0, '[ Unique %s]' % unique_value),
                         (0, 'stdout'),
                         (0, 'stdout')
                       ],
                       mocks_dict = mocks_dict)
    self.assertEqual(mocks_dict['call'].call_count, 4)
    self.assertNoMoreResources()
