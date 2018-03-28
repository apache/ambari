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
import upgrade
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *

@patch.object(upgrade, 'check_process_status', new = MagicMock())
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch("os.path.exists", new = MagicMock(return_value=True))
class TestHbaseRegionServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HBASE/0.96.0.2.0/package"
  STACK_VERSION = "2.0.6"
  TMP_PATH = '/hadoop'

  CONFIG_OVERRIDES = {"serviceName":"HBASE", "role":"HBASE_REGIONSERVER"}

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "configure",
                   config_file="default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "start",
                   config_file="default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf start regionserver',
      not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-regionserver.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1',
      user = 'hbase'
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "stop",
                   config_file="default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf stop regionserver',
        only_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-regionserver.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1',
        on_timeout = '! ( ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-regionserver.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1 ) || ambari-sudo.sh -H -E kill -9 `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-regionserver.pid`',
        timeout = 30,
        user = 'hbase',
    )

    self.assertResourceCalled('File', '/var/run/hbase/hbase-hbase-regionserver.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "configure",
                   config_file="secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assertNoMoreResources()


  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "start",
                   config_file="secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf start regionserver',
      not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-regionserver.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1',
      user = 'hbase',
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "stop",
                   config_file="secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf stop regionserver',
        only_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-regionserver.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1',
        on_timeout = '! ( ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-regionserver.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1 ) || ambari-sudo.sh -H -E kill -9 `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-regionserver.pid`',
        timeout = 30,
        user = 'hbase',
    )

    self.assertResourceCalled('File', '/var/run/hbase/hbase-hbase-regionserver.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755
    )
    self.assertResourceCalled('Directory', '/etc/hbase/conf',
      owner = 'hbase',
      group = 'hadoop',
      create_parents = True,
    )
    self.assertResourceCalled('Directory', '/tmp',
      create_parents = True,
      mode = 0777
    )
    self.assertResourceCalled('Directory', '/hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Execute', ('chmod', '1777', u'/hadoop'),
      sudo = True,
    )
    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hbase-site']
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hdfs-site']
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(self.getConfig()['configurations']['hbase-env']['content']),
      group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
      owner = 'root',
      group = 'root',
      create_parents = True,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/hbase.conf',
      content = Template('hbase.conf.j2'),
      owner = 'root',
      group = 'root',
      mode = 0644,
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
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )
    self.assertResourceCalled('File',
                              '/etc/hbase/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hbase',
                              content=InlineTemplate('log4jproperties\nline2')
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755
    )
    self.assertResourceCalled('Directory', '/etc/hbase/conf',
      owner = 'hbase',
      group = 'hadoop',
      create_parents = True,
    )
    self.assertResourceCalled('Directory', '/tmp',
      create_parents = True,
      mode = 0777
    )
    self.assertResourceCalled('Directory', '/hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Execute', ('chmod', '1777', u'/hadoop'),
                              sudo = True,
                              )
    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hbase-site']
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hdfs-site']
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(self.getConfig()['configurations']['hbase-env']['content']),
      group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
        owner = 'root',
        group = 'root',
        create_parents = True,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/hbase.conf',
        content = Template('hbase.conf.j2'),
        owner = 'root',
        group = 'root',
        mode = 0644,
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
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )
    self.assertResourceCalled('File',
                              '/etc/hbase/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hbase',
                              content=InlineTemplate('log4jproperties\nline2')
    )

  def test_start_default_22(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "start",
                   config_file="hbase-rs-2.2.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755)

    self.assertResourceCalled('Directory', '/usr/hdp/current/hbase-regionserver/conf',
      owner = 'hbase',
      group = 'hadoop',
      create_parents = True)
    self.assertResourceCalled('Directory', '/tmp',
      create_parents = True,
      mode = 0777
    )

    self.assertResourceCalled('Directory', '/hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )

    self.assertResourceCalled('Execute', ('chmod', '1777', u'/hadoop'),
                              sudo = True,
                              )

    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hbase-site'])
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hbase',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configurationAttributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hdfs-site'])

    self.assertResourceCalled('XmlConfig', 'hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hbase-policy'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hbase-policy'])

    self.assertResourceCalled('File', '/usr/hdp/current/hbase-regionserver/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(self.getConfig()['configurations']['hbase-env']['content']),
      group = 'hadoop'
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
      owner = 'root',
      group = 'root',
      create_parents = True,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/hbase.conf',
      content = Template('hbase.conf.j2'),
      owner = 'root',
      group = 'root',
      mode = 0644,
    )

    self.assertResourceCalled('TemplateConfig', '/usr/hdp/current/hbase-regionserver/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-RS')

    self.assertResourceCalled('TemplateConfig', '/usr/hdp/current/hbase-regionserver/conf/regionservers',
      owner = 'hbase',
      template_tag = None)

    self.assertResourceCalled('Directory', '/var/run/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )

    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )

    self.assertResourceCalled('File',
                              '/usr/hdp/current/hbase-regionserver/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hbase',
                              content=InlineTemplate('log4jproperties\nline2'))
    self.assertResourceCalled('Execute', '/usr/hdp/current/hbase-regionserver/bin/hbase-daemon.sh --config /usr/hdp/current/hbase-regionserver/conf start regionserver',
      not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-regionserver.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1',
      user = 'hbase')

    self.assertNoMoreResources()

  def test_start_default_22_with_phoenix_enabled(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                   classname = "HbaseRegionServer",
                   command = "start",
                   config_file="hbase-rs-2.2-phoenix.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755)

    self.assertResourceCalled('Directory', '/usr/hdp/current/hbase-regionserver/conf',
      owner = 'hbase',
      group = 'hadoop',
      create_parents = True)
    self.assertResourceCalled('Directory', '/tmp',
      create_parents = True,
      mode = 0777
    )

    self.assertResourceCalled('Directory', '/hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )

    self.assertResourceCalled('Execute', ('chmod', '1777', u'/hadoop'),
                              sudo = True,
                              )

    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hbase-site'])
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hbase',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configurationAttributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hdfs-site'])

    self.assertResourceCalled('XmlConfig', 'hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hbase-policy'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hbase-policy'])

    self.assertResourceCalled('File', '/usr/hdp/current/hbase-regionserver/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(self.getConfig()['configurations']['hbase-env']['content']),
      group = 'hadoop'
    )

    self.assertResourceCalled('Directory', '/etc/security/limits.d',
      owner = 'root',
      group = 'root',
      create_parents = True,
    )

    self.assertResourceCalled('File', '/etc/security/limits.d/hbase.conf',
      content = Template('hbase.conf.j2'),
      owner = 'root',
      group = 'root',
      mode = 0644,
    )

    self.assertResourceCalled('TemplateConfig', '/usr/hdp/current/hbase-regionserver/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-RS')

    self.assertResourceCalled('TemplateConfig', '/usr/hdp/current/hbase-regionserver/conf/regionservers',
      owner = 'hbase',
      template_tag = None)

    self.assertResourceCalled('Directory', '/var/run/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )

    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )

    self.assertResourceCalled('File',
                              '/usr/hdp/current/hbase-regionserver/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hbase',
                              content=InlineTemplate('log4jproperties\nline2'))

    self.assertResourceCalled('Package', 'phoenix_2_2_*', retry_count=5, retry_on_repo_unavailability=False)

    self.assertResourceCalled('Execute', '/usr/hdp/current/hbase-regionserver/bin/hbase-daemon.sh --config /usr/hdp/current/hbase-regionserver/conf start regionserver',
      not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-regionserver.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-regionserver.pid` >/dev/null 2>&1',
      user = 'hbase')

    self.assertNoMoreResources()

  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                       classname = "HbaseRegionServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hbase-regionserver', version), sudo=True,)
    self.assertNoMoreResources()

  @patch('time.sleep')
  def test_post_rolling_restart(self, sleep_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                       classname = "HbaseRegionServer",
                       command = "post_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       call_mocks = [(0, "Dummy output c6401.ambari.apache.org:")],
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)
    self.assertTrue(mocks_dict['call'].called)
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  def test_upgrade_23(self, call_mock):
    call_mock.side_effects = [(0, None), (0, None)]

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_regionserver.py",
                       classname = "HbaseRegionServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hbase-regionserver', version), sudo=True)

