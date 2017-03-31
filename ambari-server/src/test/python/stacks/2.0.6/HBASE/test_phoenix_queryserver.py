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
import sys
import unittest

from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *


@patch("platform.linux_distribution", new = MagicMock(return_value = "Linux"))
@patch("os.path.exists", new = MagicMock(return_value = True))
class TestPhoenixQueryServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HBASE/0.96.0.2.0/package"
  STACK_VERSION = "2.3"
  TMP_PATH = "/hadoop"

  def test_configure_default(self):
    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/phoenix_queryserver.py",
      classname = "PhoenixQueryServer",
      command = "configure",
      config_file = "hbase_default.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, None)]
    )

    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/phoenix_queryserver.py",
      classname = "PhoenixQueryServer",
      command = "start",
      config_file = "hbase_default.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, None)]
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute',
      '/usr/hdp/current/phoenix-server/bin/queryserver.py start',
      environment = {'JAVA_HOME':'/usr/jdk64/jdk1.8.0_40',
      'HBASE_CONF_DIR':'/usr/hdp/current/hbase-regionserver/conf'},
      user = 'hbase'
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/phoenix_queryserver.py",
      classname = "PhoenixQueryServer",
      command = "stop",
      config_file = "hbase_default.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, None)]
    )

    self.assert_call_to_get_hadoop_conf_dir()

    self.assertResourceCalled('Execute',
      '/usr/hdp/current/phoenix-server/bin/queryserver.py stop',
      environment = {'JAVA_HOME':'/usr/jdk64/jdk1.8.0_40',
      'HBASE_CONF_DIR':'/usr/hdp/current/hbase-regionserver/conf'},
      user = 'hbase'
    )

    self.assertResourceCalled('File', '/var/run/hbase/phoenix-hbase-server.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/phoenix_queryserver.py",
      classname = "PhoenixQueryServer",
      command = "configure",
      config_file = "hbase_secure.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, None)]
    )

    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/phoenix_queryserver.py",
      classname = "PhoenixQueryServer",
      command = "start",
      config_file = "hbase_secure.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, None)]
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Execute',
      '/usr/hdp/current/phoenix-server/bin/queryserver.py start',
      environment = {'JAVA_HOME':'/usr/jdk64/jdk1.8.0_40',
      'HBASE_CONF_DIR':'/usr/hdp/current/hbase-regionserver/conf'},
      user = 'hbase'
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/phoenix_queryserver.py",
      classname = "PhoenixQueryServer",
      command = "stop",
      config_file = "hbase_secure.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, None)]
    )

    self.assert_call_to_get_hadoop_conf_dir()

    self.assertResourceCalled('Execute',
      '/usr/hdp/current/phoenix-server/bin/queryserver.py stop',
      environment = {'JAVA_HOME':'/usr/jdk64/jdk1.8.0_40',
      'HBASE_CONF_DIR':'/usr/hdp/current/hbase-regionserver/conf'},
      user = 'hbase'
    )

    self.assertResourceCalled('File', '/var/run/hbase/phoenix-hbase-server.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_start_default_24(self):
    if sys.version_info >= (2, 7):
      raise unittest.SkipTest("there's nothing to upgrade to yet")
    else:
      # skiptest functionality is not available with Python 2.6 unittest
      return

    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/phoenix_queryserver.py",
      classname = "PhoenixQueryServer",
      command = "start",
      config_file = "hbase-rs-2.4.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755)

    self.assertResourceCalled('Directory',
      '/usr/hdp/current/hbase-regionserver/conf',
      owner = 'hbase',
      group = 'hadoop',
      create_parents = True)

    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configuration_attributes'][
        'hbase-site'])
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes'][
        'core-site']
    )
    self.assertResourceCalled('File',
      '/usr/hdp/current/hbase-regionserver/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(
        self.getConfig()['configurations']['hbase-env']['content']),
      group = 'haoop',                          
    )

    self.assertResourceCalled('Directory', '/var/run/hbase',
      owner = 'hbase',
      create_parents = True)

    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      create_parents = True)

    self.assertResourceCalled('File',
      '/usr/lib/phoenix/bin/log4j.properties',
      mode = 0644,
      group = 'hadoop',
      owner = 'hbase',
      content = 'log4jproperties\nline2')

    self.assertResourceCalled('Execute',
      '/usr/hdp/current/phoenix-server/bin/queryserver.py start',
      not_if = 'ls /var/run/hbase/phoenix-hbase-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hbase/phoenix-hbase-server.pid` >/dev/null 2>&1',
      user = 'hbase')

    self.assertNoMoreResources()

  def assert_call_to_get_hadoop_conf_dir(self):
    # From call to conf_select.get_hadoop_conf_dir()
    self.assertResourceCalled("Execute", ("cp", "-R", "-p", "/etc/hadoop/conf", "/etc/hadoop/conf.backup"),
                              not_if = "test -e /etc/hadoop/conf.backup",
                              sudo = True)
    self.assertResourceCalled("Directory", "/etc/hadoop/conf",
                              action = ["delete"])
    self.assertResourceCalled("Link", "/etc/hadoop/conf", to="/etc/hadoop/conf.backup")

  def assert_configure_default(self):
    self.assert_call_to_get_hadoop_conf_dir()

    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755
    )
    self.assertResourceCalled('Directory',
      '/usr/hdp/current/hbase-regionserver/conf',
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
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configuration_attributes'][
        'hbase-site']
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes'][
        'core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes'][
        'hdfs-site']
    )
    self.assertResourceCalled('XmlConfig', 'hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hbase-policy'],
      configuration_attributes = self.getConfig()['configuration_attributes'][
        'hbase-policy']
    )
    self.assertResourceCalled('File',
      '/usr/hdp/current/hbase-regionserver/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(
        self.getConfig()['configurations']['hbase-env']['content']),
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
    self.assertResourceCalled('TemplateConfig',
      '/usr/hdp/current/hbase-regionserver/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-RS',
    )
    self.assertResourceCalled('TemplateConfig',
      '/usr/hdp/current/hbase-regionserver/conf/regionservers',
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
      '/usr/hdp/current/hbase-regionserver/conf/log4j.properties',
      mode = 0644,
      group = 'hadoop',
      owner = 'hbase',
      content = InlineTemplate('log4jproperties\nline2')
    )

  def assert_configure_secured(self):
    self.assert_call_to_get_hadoop_conf_dir()

    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755
    )
    self.assertResourceCalled('Directory',
      '/usr/hdp/current/hbase-regionserver/conf',
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
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configuration_attributes'][
        'hbase-site']
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes'][
        'core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes'][
        'hdfs-site']
    )
    self.assertResourceCalled('XmlConfig', 'hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-regionserver/conf',
      configurations = self.getConfig()['configurations']['hbase-policy'],
      configuration_attributes = self.getConfig()['configuration_attributes'][
        'hbase-policy']
    )
    self.assertResourceCalled('File',
      '/usr/hdp/current/hbase-regionserver/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(
        self.getConfig()['configurations']['hbase-env']['content']),
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
    self.assertResourceCalled('TemplateConfig',
      '/usr/hdp/current/hbase-regionserver/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-RS',
    )
    self.assertResourceCalled('TemplateConfig',
      '/usr/hdp/current/hbase-regionserver/conf/regionservers',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('TemplateConfig',
      '/usr/hdp/current/hbase-regionserver/conf/hbase_queryserver_jaas.conf',
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
      '/usr/hdp/current/hbase-regionserver/conf/log4j.properties',
      mode = 0644,
      group = 'hadoop',
      owner = 'hbase',
      content = InlineTemplate('log4jproperties\nline2')
    )

  def test_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.3/configs/hbase_default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    json_content['commandParams']['version'] = '2.3.0.0-1234'

    self.executeScript(
      self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/phoenix_queryserver.py",
      classname = "PhoenixQueryServer",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      call_mocks = [(0, "/etc/hbase/2.3.0.0-1234/0", ''), (0, None, None), (0, None, None)],
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Directory', '/etc/hbase/2.3.0.0-1234/0',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalledIgnoreEarlier('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'phoenix-server', '2.3.0.0-1234'), sudo=True)

    self.assertResourceCalled("Execute", ("cp", "-R", "-p", "/etc/hadoop/conf", "/etc/hadoop/conf.backup"),
                              not_if = "test -e /etc/hadoop/conf.backup",
                              sudo = True)
    self.assertResourceCalled("Directory", "/etc/hadoop/conf", action = ["delete"])
    self.assertResourceCalled("Link", "/etc/hadoop/conf", to="/etc/hadoop/conf.backup")
    self.assertNoMoreResources()
