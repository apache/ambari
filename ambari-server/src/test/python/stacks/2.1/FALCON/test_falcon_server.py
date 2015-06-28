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
import shutil
from stacks.utils.RMFTestCase import *
import tarfile


@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestFalconServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "FALCON/0.5.0.2.1/package"
  STACK_VERSION = "2.1"
  UPGRADE_STACK_VERSION = "2.2"

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
      classname="FalconServer",
      command="start",
      config_file="default.json",
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assert_configure_default()

    self.assertResourceCalled('Execute', '/usr/lib/falcon/bin/falcon-start -port 15000',
      path = ['/usr/bin'],
      user = 'falcon',
      environment = {'HADOOP_HOME': '/usr/lib/hadoop'})

    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
      classname="FalconServer",
      command="stop",
      config_file="default.json",
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Execute', '/usr/lib/falcon/bin/falcon-stop',
      path = ['/usr/bin'],
      user = 'falcon',
      environment = {'HADOOP_HOME': '/usr/lib/hadoop'})

    self.assertResourceCalled('File', '/var/run/falcon/falcon.pid',
      action = ['delete'])

    self.assertNoMoreResources()

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/falcon',
                              owner = 'falcon',
                              recursive = True
                              )
    self.assertResourceCalled('Directory', '/var/log/falcon',
                              owner = 'falcon',
                              recursive = True
                              )
    self.assertResourceCalled('Directory', '/var/lib/falcon/webapp',
                              owner = 'falcon',
                              recursive = True
                              )
    self.assertResourceCalled('Directory', '/usr/lib/falcon',
                              owner = 'falcon',
                              recursive = True
                              )
    self.assertResourceCalled('Directory', '/etc/falcon',
                              mode = 0755,
                              recursive = True
    )
    self.assertResourceCalled('Directory', '/etc/falcon/conf',
                              owner = 'falcon',
                              recursive = True
    )
    self.assertResourceCalled('File', '/etc/falcon/conf/falcon-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['falcon-env']['content']),
                              owner = 'falcon'
                              )
    self.assertResourceCalled('File', '/etc/falcon/conf/client.properties',
                              content = Template('client.properties.j2'),
                              mode = 0644,
                              owner = 'falcon'
                              )
    self.assertResourceCalled('PropertiesFile', '/etc/falcon/conf/runtime.properties',
                              mode = 0644,
                              properties = self.getConfig()['configurations']['falcon-runtime.properties'],
                              owner = 'falcon'
                              )
    self.assertResourceCalled('PropertiesFile', '/etc/falcon/conf/startup.properties',
                              mode = 0644,
                              properties = self.getConfig()['configurations']['falcon-startup.properties'],
                              owner = 'falcon'
                              )
    self.assertResourceCalled('Directory', '/hadoop/falcon/store',
                              owner = 'falcon',
                              recursive = True
                              )
    self.assertResourceCalled('HdfsResource', '/apps/falcon',
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        owner = 'falcon',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0777,
    )
    self.assertResourceCalled('Directory', '/hadoop/falcon/store',
        owner = 'falcon',
        recursive = True,
    )
    self.assertResourceCalled('HdfsResource', '/apps/data-mirroring',
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        owner = 'falcon',
        group='users',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        recursive_chown = True,
        recursive_chmod = True,
        action = ['create_on_execute'], hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0770,
        source='/usr/hdp/current/falcon-server/data-mirroring'
    )

    self.assertResourceCalled('HdfsResource', None,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        action = ['execute'], hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Directory', '/hadoop/falcon',
                              owner = 'falcon',
                              recursive = True,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/hadoop/falcon/embeddedmq',
                              owner = 'falcon',
                              recursive = True
                              )
    self.assertResourceCalled('Directory', '/hadoop/falcon/embeddedmq/data',
                              owner = 'falcon',
                              recursive = True
                              )


  @patch("shutil.rmtree", new = MagicMock())
  @patch("tarfile.open")
  @patch("os.path.isdir")
  @patch("os.path.exists")
  @patch("os.path.isfile")
  def test_upgrade(self, isfile_mock, exists_mock, isdir_mock,
      tarfile_open_mock):

    isdir_mock.return_value = True
    exists_mock.side_effect = [False,False,True]
    isfile_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
     classname = "FalconServer", command = "restart", config_file = "falcon-upgrade.json",
     hdp_stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES )

    self.assertResourceCalled('Execute',
      '/usr/hdp/current/falcon-server/bin/falcon-stop',
      path = ['/usr/hdp/current/hadoop-client/bin'], user='falcon',
      environment = {'HADOOP_HOME': '/usr/hdp/current/hadoop-client'})

    self.assertResourceCalled('File', '/var/run/falcon/falcon.pid',
      action = ['delete'])

    self.assertResourceCalled('Execute', ('hdp-select', 'set', 'falcon-server', '2.2.1.0-2135'), sudo=True)

    # 4 calls to tarfile.open (2 directories * read + write)
    self.assertTrue(tarfile_open_mock.called)
    self.assertEqual(tarfile_open_mock.call_count,4)

    # check the call args for creation of the tarfile
    call_args = tarfile_open_mock.call_args_list[0]
    call_argument_tarfile = call_args[0][0]
    call_kwargs = call_args[1]

    self.assertTrue("falcon-upgrade-backup/falcon-conf-backup.tar" in call_argument_tarfile)
    self.assertEquals(True, call_kwargs["dereference"])
    
  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'startup': {
        '*.falcon.service.authentication.kerberos.keytab': 'path/to/falcon/service/keytab',
        '*.falcon.service.authentication.kerberos.principal': 'falcon_service_keytab',
        '*.falcon.http.authentication.kerberos.keytab': 'path/to/falcon/http/keytab',
        '*.falcon.http.authentication.kerberos.principal': 'falcon_http_principal'
      }
    }
    result_issues = []
    props_value_check = {"*.falcon.authentication.type": "kerberos",
                           "*.falcon.http.authentication.type": "kerberos"}
    props_empty_check = ["*.falcon.service.authentication.kerberos.principal",
                           "*.falcon.service.authentication.kerberos.keytab",
                           "*.falcon.http.authentication.kerberos.principal",
                           "*.falcon.http.authentication.kerberos.keytab"]

    props_read_check = ["*.falcon.service.authentication.kerberos.keytab",
                          "*.falcon.http.authentication.kerberos.keytab"]

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    get_params_mock.assert_called_with('/etc/falcon/conf', {'startup.properties': 'PROPERTIES'})
    build_exp_mock.assert_called_with('startup', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['falcon-env']['falcon_user'],
                                                  security_params['startup']['*.falcon.http.authentication.kerberos.keytab'],
                                                  security_params['startup']['*.falcon.http.authentication.kerberos.principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="security_status",
                       config_file="secured.json",
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

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'startup': "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="security_status",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

  @patch('os.path.isfile', new=MagicMock(return_value=True))
  @patch.object(tarfile, 'open')
  @patch.object(shutil, 'rmtree')
  def test_pre_rolling_restart(self, tarfile_open_mock, rmtree_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/falcon-upgrade.json"
    tarfile_open_mock.return_value = MagicMock()

    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname = "FalconServer",
                       command = "pre_rolling_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('hdp-select', 'set', 'falcon-server', version), sudo=True,)
    self.assertTrue(tarfile_open_mock.called)
    self.assertTrue(rmtree_mock.called)
    self.printResources()

  @patch('os.path.isfile', new=MagicMock(return_value=True))
  @patch.object(tarfile, 'open')
  @patch.object(shutil, 'rmtree')
  @patch("resource_management.core.shell.call")
  def test_pre_rolling_restart_23(self, tarfile_open_mock, rmtree_mock, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/falcon-upgrade.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname = "FalconServer",
                       command = "pre_rolling_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None), (0, None)],
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Execute',
                              ('hdp-select', 'set', 'falcon-server', version), sudo=True,)
    self.assertNoMoreResources()

    self.assertEquals(2, mocks_dict['call'].call_count)
    self.assertEquals(
      "conf-select create-conf-dir --package falcon --stack-version 2.3.0.0-1234 --conf-version 0",
       mocks_dict['call'].call_args_list[0][0][0])
    self.assertEquals(
      "conf-select set-conf-dir --package falcon --stack-version 2.3.0.0-1234 --conf-version 0",
       mocks_dict['call'].call_args_list[1][0][0])
