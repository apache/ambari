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
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', '/usr/lib/falcon/bin/falcon-start -port 15000',
                              path = ['/usr/bin'],
                              user = 'falcon',
                              )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/lib/falcon/bin/falcon-stop',
                              path = ['/usr/bin'],
                              user = 'falcon',
                              )
    self.assertResourceCalled('File', '/var/run/falcon/falcon.pid',
                              action = ['delete'],
                              )
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
                              )
    self.assertResourceCalled('Directory', '/var/log/falcon',
                              owner = 'falcon',
                              recursive = True
                              )
    self.assertResourceCalled('Directory', '/var/lib/falcon/webapp',
                              owner = 'falcon',
                              )
    self.assertResourceCalled('Directory', '/usr/lib/falcon',
                              owner = 'falcon',
                              )
    self.assertResourceCalled('Directory', '/etc/falcon',
                              mode = 0755
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
    self.assertResourceCalled('HdfsResource', '/apps/falcon',
        security_enabled = False,
        hadoop_conf_dir = '/etc/hadoop/conf',
        keytab = UnknownConfigurationMock(),
        hadoop_fs = 'hdfs://c6401.ambari.apache.org:8020',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        owner = 'falcon',
        hadoop_bin_dir = '/usr/bin',
        type = 'directory',
        action = ['create_delayed'],
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', None,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        hadoop_fs = 'hdfs://c6401.ambari.apache.org:8020',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        action = ['execute'],
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Directory', '/hadoop/falcon',
                              owner = 'falcon',
                              recursive = True,
                              recursive_permission = True
                              )
    self.assertResourceCalled('Directory', '/hadoop/falcon/embeddedmq',
                              owner = 'falcon'
                              )
    self.assertResourceCalled('Directory', '/hadoop/falcon/embeddedmq/data',
                              owner = 'falcon',
                              recursive = True,
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
      path = ['/usr/hdp/current/hadoop-client/bin'], user='falcon')

    self.assertResourceCalled('File', '/var/run/falcon/falcon.pid',
      action = ['delete'])

    self.assertResourceCalled('Execute', 'hdp-select set falcon-server 2.2.1.0-2135')

    # 4 calls to tarfile.open (2 directories * read + write)
    self.assertTrue(tarfile_open_mock.called)
    self.assertEqual(tarfile_open_mock.call_count,4)

