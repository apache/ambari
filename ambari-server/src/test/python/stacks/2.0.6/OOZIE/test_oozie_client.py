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
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
from resource_management.libraries.script.script import Script
import json

@patch.object(Script, 'format_package_name', new = MagicMock())
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestOozieClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "OOZIE/4.0.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_client.py",
                       classname = "OozieClient",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True
    )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0664,
                              conf_dir = '/etc/oozie/conf',
                              configurations = self.getConfig()['configurations']['oozie-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-env.sh',
                              owner = 'oozie',
                              content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content']),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents=True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/oozie.conf',
                              owner = 'root',
                              group = 'root',
                              mode=0644,
                              content=Template("oozie.conf.j2"),
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              content = InlineTemplate('log4jproperties\nline2')
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/adminusers.txt',
        owner = 'oozie',
        group = 'hadoop',
        )
    self.assertResourceCalled('File', '/etc/oozie/conf/hadoop-config.xml',
        owner = 'oozie',
        group = 'hadoop',
        )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-default.xml',
        owner = 'oozie',
        group = 'hadoop',
        )
    self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf',
        owner = 'oozie',
        group = 'hadoop',
        )
    self.assertResourceCalled('File', '/etc/oozie/conf/action-conf/hive.xml',
        owner = 'oozie',
        group = 'hadoop',
        )
    self.assertNoMoreResources()


  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_client.py",
                       classname = "OozieClient",
                       command = "configure",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True
    )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0664,
                              conf_dir = '/etc/oozie/conf',
                              configurations = self.getConfig()['configurations']['oozie-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-env.sh',
                              owner = 'oozie',
                              content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content']),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents=True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/oozie.conf',
                              owner = 'root',
                              group = 'root',
                              mode=0644,
                              content=Template("oozie.conf.j2"),
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              content = InlineTemplate('log4jproperties\nline2')
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/adminusers.txt',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/hadoop-config.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-default.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/action-conf/hive.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()


  def test_configure_default_hdp22(self):
    config_file = "stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      default_json = json.load(f)


    default_json['hostLevelParams']['stack_version']= '2.2'
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_client.py",
                       classname = "OozieClient",
                       command = "configure",
                       config_dict=default_json,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/oozie-client/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True
    )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0664,
                              conf_dir = '/usr/hdp/current/oozie-client/conf',
                              configurations = self.getConfig()['configurations']['oozie-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
    )
    self.assertResourceCalled('File', '/usr/hdp/current/oozie-client/conf/oozie-env.sh',
                              owner = 'oozie',
                              content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content']),
                              group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents=True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/oozie.conf',
                              owner = 'root',
                              group = 'root',
                              mode=0644,
                              content=Template("oozie.conf.j2"),
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/oozie-client/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              content = InlineTemplate('log4jproperties\nline2')
    )
    self.assertResourceCalled('File', '/usr/hdp/current/oozie-client/conf/adminusers.txt',
                              content = Template('adminusers.txt.j2'),
                              owner = 'oozie',
                              group = 'hadoop',
                              mode=0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/oozie-client/conf/hadoop-config.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/oozie-client/conf/oozie-default.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/oozie-client/conf/action-conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/oozie-client/conf/action-conf/hive.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()

  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_client.py",
                       classname = "OozieClient",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'oozie-client', version), sudo=True)
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
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_client.py",
                       classname = "OozieClient",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Link', ('/etc/oozie/conf'), to='/usr/hdp/current/oozie-client/conf')
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'oozie-client', version), sudo=True)
    self.assertNoMoreResources()

    self.assertEquals(1, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'set-conf-dir', '--package', 'oozie', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'create-conf-dir', '--package', 'oozie', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])
