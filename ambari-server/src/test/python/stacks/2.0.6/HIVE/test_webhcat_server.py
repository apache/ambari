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
from resource_management.libraries.functions import StackFeature

# used for faking out stack features when the config files used by unit tests use older stacks
def mock_stack_feature(stack_feature, stack_version):
  if stack_feature == StackFeature.ROLLING_UPGRADE:
    return True
  if stack_feature == StackFeature.CONFIG_VERSIONING:
    return True
  if stack_feature == StackFeature.HIVE_WEBHCAT_SPECIFIC_CONFIGS:
    return True

  return False

@patch("os.path.isfile", new = MagicMock(return_value=True))
@patch("glob.glob", new = MagicMock(return_value=["one", "two"]))
@patch("resource_management.libraries.functions.stack_features.check_stack_feature", new=MagicMock(side_effect=mock_stack_feature))
class TestWebHCatServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"

  CONFIG_OVERRIDES = {"serviceName":"HIVE", "role":"WEBHCAT_SERVER"}

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'cd /var/run/webhcat ; /usr/hdp/current/hive-webhcat/sbin/webhcat_server.sh start',
        not_if = "ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1",
        user = 'hcat',
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "stop",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', '/usr/hdp/current/hive-webhcat/sbin/webhcat_server.sh stop',
                              user = 'hcat',
                              )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `cat /var/run/webhcat/webhcat.pid`',
                              only_if = "ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1",
                              ignore_failures = True
    )

    self.assertResourceCalled('Execute', "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1)")

    self.assertResourceCalled('File', '/var/run/webhcat/webhcat.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

    def test_configure_secured(self):
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                         classname = "WebHCatServer",
                         command = "configure",
                         config_file="secured.json",
                         stack_version = self.STACK_VERSION,
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
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )

    self.assertResourceCalled('Execute', "find /var/log/webhcat -maxdepth 1 -type f -name '*' -exec echo '==> {} <==' \\; -exec tail -n 40 {} \\;",
        logoutput = True,
        ignore_failures = True,
        user = 'hcat',
    )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `cat /var/run/webhcat/webhcat.pid`',
                              only_if = "ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1",
                              ignore_failures = True
                              )

    self.assertResourceCalled('Execute', "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1)")

    self.assertResourceCalled('File', '/var/run/webhcat/webhcat.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "start",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'cd /var/run/webhcat ; /usr/hdp/current/hive-webhcat/sbin/webhcat_server.sh start',
        not_if = "ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1",
        user = 'hcat',
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "stop",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', '/usr/hdp/current/hive-webhcat/sbin/webhcat_server.sh stop',
                              user = 'hcat',
                              )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `cat /var/run/webhcat/webhcat.pid`',
                              only_if = "ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1",
                              ignore_failures = True
    )

    self.assertResourceCalled('Execute', "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1)")
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
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )

    self.assertResourceCalled('Execute', "find /var/log/webhcat -maxdepth 1 -type f -name '*' -exec echo '==> {} <==' \\; -exec tail -n 40 {} \\;",
        logoutput = True,
        ignore_failures = True,
        user = 'hcat',
    )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `cat /var/run/webhcat/webhcat.pid`',
                              only_if = "ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1",
                              ignore_failures = True
                              )

    self.assertResourceCalled('Execute', "! (ls /var/run/webhcat/webhcat.pid >/dev/null 2>&1 && ps -p `cat /var/run/webhcat/webhcat.pid` >/dev/null 2>&1)")
    self.assertResourceCalled('File', '/var/run/webhcat/webhcat.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-webhcat/etc/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a'
                              )
    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
                              owner = 'hcat',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hive-webhcat/etc/webhcat',
                              configurations = self.getConfig()['configurations']['webhcat-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['webhcat-site']
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-webhcat/etc/webhcat/webhcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['webhcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-webhcat/etc/webhcat',
        cd_access = 'a',
        create_parents = True
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-webhcat/etc/webhcat/webhcat-log4j.properties',
                              content = InlineTemplate('log4jproperties\nline2'),
                              owner = 'hcat',
                              group = 'hadoop',
                              mode = 0644,
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/var/run/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-webhcat/etc/webhcat',
                              owner = 'hcat',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a'
                              )
    self.assertResourceCalled('XmlConfig', 'webhcat-site.xml',
                              owner = 'hcat',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hive-webhcat/etc/webhcat',
                              configurations = self.getConfig()['configurations']['webhcat-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['webhcat-site']
    )

    self.assertResourceCalledIgnoreEarlier('File', '/usr/hdp/current/hive-webhcat/etc/webhcat/webhcat-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['webhcat-env']['content']),
                              owner = 'hcat',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-webhcat/etc/webhcat',
        cd_access = 'a',
        create_parents = True
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-webhcat/etc/webhcat/webhcat-log4j.properties',
                              content = InlineTemplate('log4jproperties\nline2'),
                              owner = 'hcat',
                              group = 'hadoop',
                              mode = 0644,
                              )

  @patch("resource_management.core.sudo.path_isdir", new = MagicMock(return_value = True))
  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_version'] = "2.3"
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/webhcat_server.py",
                       classname = "WebHCatServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-webhcat', version), sudo=True,)
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  @patch("resource_management.core.sudo.path_isdir", new = MagicMock(return_value = True))
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
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertTrue("params" in sys.modules)
    self.assertTrue(sys.modules["params"].webhcat_conf_dir is not None)
    self.assertTrue("/usr/hdp/current/hive-webhcat/etc/webhcat" == sys.modules["params"].webhcat_conf_dir)

    self.assertResourceCalledIgnoreEarlier('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-webhcat', version), sudo=True,)
    self.assertNoMoreResources()


  @patch("resource_management.core.shell.call")
  @patch("resource_management.core.sudo.path_isdir", new = MagicMock(return_value = True))
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
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None), (0, None)],
      mocks_dict = mocks_dict)


    self.assertResourceCalled('Directory', '/var/run/webhcat',
      owner = 'hcat',
      group = 'hadoop',
      create_parents = True,
      mode = 0755)

    self.assertResourceCalled('Directory', '/var/log/webhcat',
      owner = 'hcat',
      group = 'hadoop',
      create_parents = True,
      mode = 0755)

    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-webhcat/etc/webhcat',
      owner = 'hcat',
      group = 'hadoop',
      create_parents = True,
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
      create_parents = True)

    self.assertResourceCalled('File', '/usr/hdp/current/hive-webhcat/etc/webhcat/webhcat-log4j.properties',
                              content = InlineTemplate('log4jproperties\nline2'),
                              owner = 'hcat',
                              group = 'hadoop',
                              mode = 0644)

    self.assertNoMoreResources()

