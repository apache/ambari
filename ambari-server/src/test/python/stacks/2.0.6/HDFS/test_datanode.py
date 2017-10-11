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
from stacks.utils.RMFTestCase import *
import json
from mock.mock import MagicMock, patch
from resource_management.libraries.script.script import Script
from resource_management.core import shell
import itertools
from resource_management.core.exceptions import Fail
import resource_management.libraries.functions.mounted_dirs_helper
from resource_management.libraries.functions import conf_select

@patch.object(resource_management.libraries.functions, 'check_process_status', new = MagicMock())
@patch.object(Script, 'format_package_name', new = MagicMock())
@patch.object(conf_select, "get_hadoop_conf_dir", new=MagicMock(return_value="/usr/hdp/current/hadoop-client/conf"))
class TestDatanode(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HDFS/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"

  CONFIG_OVERRIDES = {"serviceName":"HDFS", "role":"DATANODE"}

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "configure",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /usr/hdp/current/hadoop-client/conf start datanode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid",
    )
    self.assertNoMoreResources()

  @patch('time.sleep')
  @patch("os.path.exists", new = MagicMock(return_value=False))
  @patch("resource_management.core.shell.checked_call")
  def test_stop_default(self, checked_call_mock, time_mock):
    def side_effect(arg):
      if '-D ipc.client.connect.max.retries=5 -D ipc.client.connect.retry.interval=1000 -getDatanodeInfo' in arg :
        raise Fail()
      return
    checked_call_mock.side_effect = side_effect
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       checked_call_mocks = side_effect,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /usr/hdp/current/hadoop-client/conf stop datanode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid', action = ['delete'])

    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "configure",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid",
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /usr/hdp/current/hadoop-client/conf start datanode',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid",
    )
    self.assertNoMoreResources()

  def test_start_secured_HDP22_root(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_dict = secured_json,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured("2.3", snappy_enabled=False)
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid",
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/hdp/2.1.0.0-1234/hadoop/sbin/hadoop-daemon.sh --config /usr/hdp/current/hadoop-client/conf start datanode',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/hdp/2.1.0.0-1234/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid",
    )
    self.assertNoMoreResources()

  def test_start_secured_HDP22_non_root_https_only(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'
    secured_json['configurations']['hdfs-site']['dfs.http.policy']= 'HTTPS_ONLY'
    secured_json['configurations']['hdfs-site']['dfs.datanode.address']= '0.0.0.0:10000'
    secured_json['configurations']['hdfs-site']['dfs.datanode.https.address']= '0.0.0.0:50000'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "start",
                       config_dict = secured_json,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured("2.3", snappy_enabled=False)
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/hdp/2.1.0.0-1234/hadoop/sbin/hadoop-daemon.sh --config /usr/hdp/current/hadoop-client/conf start datanode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/hdp/2.1.0.0-1234/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid",
    )
    self.assertNoMoreResources()

  @patch('time.sleep')
  @patch("os.path.exists", new = MagicMock(return_value=False))
  @patch("resource_management.core.shell.checked_call")
  def test_stop_secured(self, checked_call_mock, time_mock):
    def side_effect(arg):
      if '-D ipc.client.connect.max.retries=5 -D ipc.client.connect.retry.interval=1000 -getDatanodeInfo' in arg :
        raise Fail()
      return
    checked_call_mock.side_effect = side_effect
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       checked_call_mocks = side_effect,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /usr/hdp/current/hadoop-client/conf stop datanode',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid', action = ['delete'])
    self.assertNoMoreResources()

  @patch('time.sleep')
  @patch("os.path.exists", new = MagicMock(return_value=False))
  @patch("resource_management.core.shell.checked_call")
  def test_stop_secured_HDP22_root(self, checked_call_mock, time_mock):
    def side_effect(arg):
      if '-D ipc.client.connect.max.retries=5 -D ipc.client.connect.retry.interval=1000 -getDatanodeInfo' in arg :
        raise Fail()
      return
    checked_call_mock.side_effect = side_effect
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_dict = secured_json,
                       stack_version = self.STACK_VERSION,
                       checked_call_mocks = side_effect,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E /usr/hdp/2.1.0.0-1234/hadoop/sbin/hadoop-daemon.sh --config /usr/hdp/current/hadoop-client/conf stop datanode',
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/hdp/2.1.0.0-1234/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid', action = ['delete'])
    self.assertNoMoreResources()

  @patch('time.sleep')
  @patch("os.path.exists", new = MagicMock(return_value=False))
  @patch("resource_management.core.shell.checked_call")
  def test_stop_secured_HDP22_non_root_https_only(self, checked_call_mock, time_mock):
    def side_effect(arg):
      if '-D ipc.client.connect.max.retries=5 -D ipc.client.connect.retry.interval=1000 -getDatanodeInfo' in arg :
        raise Fail()
      return
    checked_call_mock.side_effect = side_effect
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['hostLevelParams']['stack_version']= '2.2'
    secured_json['configurations']['hdfs-site']['dfs.http.policy']= 'HTTPS_ONLY'
    secured_json['configurations']['hdfs-site']['dfs.datanode.address']= '0.0.0.0:10000'
    secured_json['configurations']['hdfs-site']['dfs.datanode.https.address']= '0.0.0.0:50000'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "stop",
                       config_dict = secured_json,
                       stack_version = self.STACK_VERSION,
                       checked_call_mocks = side_effect,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/hdp/2.1.0.0-1234/hadoop/sbin/hadoop-daemon.sh --config /usr/hdp/current/hadoop-client/conf stop datanode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/hdp/2.1.0.0-1234/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-datanode.pid', action = ['delete'])
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-i386-32',
        create_parents = True,
    )
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-amd64-64',
        create_parents = True,
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-i386-32/libsnappy.so',
        to = '/usr/lib/hadoop/lib/libsnappy.so',
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-amd64-64/libsnappy.so',
        to = '/usr/lib/hadoop/lib64/libsnappy.so',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hdfs.conf',
                              content = Template('hdfs.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hadoop-client/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
                              )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hadoop-client/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
                              mode = 0644
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hadoop-client/conf/slaves',
                              content = Template('slaves.j2'),
                              owner = 'hdfs',
                              )

    self.assertResourceCalled('Directory', '/var/lib/hadoop-hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0751,
                              create_parents = True,
                              )
    self.assertResourceCalled('Directory', '/var/lib/ambari-agent/data/datanode',
                              mode = 0755,
                              create_parents = True
    )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/data',
                              owner = 'hdfs',
                              ignore_failures = True,
                              group = 'hadoop',
                              mode = 0750,
                              create_parents = True,
                              cd_access='a'
                              )
    content = resource_management.libraries.functions.mounted_dirs_helper.DIR_TO_MOUNT_HEADER
    self.assertResourceCalled('File', '/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
                              content = content
                              )

  def assert_configure_secured(self, stackVersion=STACK_VERSION, snappy_enabled=True):
    conf_dir = '/usr/hdp/current/hadoop-client/conf'
    if stackVersion != self.STACK_VERSION:
      conf_dir = '/usr/hdp/current/hadoop-client/conf'
    
    if snappy_enabled:
      self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-i386-32',
          create_parents = True,
      )
      self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-amd64-64',
          create_parents = True,
      )
      self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-i386-32/libsnappy.so',
          to = '/usr/lib/hadoop/lib/libsnappy.so',
      )
      self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-amd64-64/libsnappy.so',
          to = '/usr/lib/hadoop/lib64/libsnappy.so',
      )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hdfs.conf',
                              content = Template('hdfs.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', conf_dir + '/hdfs_dn_jaas.conf',
                              content = Template('hdfs_dn_jaas.conf.j2'),
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', conf_dir + '/hdfs_nn_jaas.conf',
                              content = Template('hdfs_nn_jaas.conf.j2'),
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = conf_dir,
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
                              )

    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = conf_dir,
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
                              mode = 0644
    )
    self.assertResourceCalled('File', conf_dir + '/slaves',
                              content = Template('slaves.j2'),
                              owner = 'root',
                              )

    self.assertResourceCalled('Directory', '/var/lib/hadoop-hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0751,
                              create_parents = True,
                              )
    self.assertResourceCalled('Directory', '/var/lib/ambari-agent/data/datanode',
                              mode = 0755,
                              create_parents = True
    )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/data',
                              owner = 'hdfs',
                              ignore_failures = True,
                              group = 'hadoop',
                              mode = 0750,
                              create_parents = True,
                              cd_access='a'
                              )
    content = resource_management.libraries.functions.mounted_dirs_helper.DIR_TO_MOUNT_HEADER
    self.assertResourceCalled('File', '/var/lib/ambari-agent/data/datanode/dfs_data_dir_mount.hist',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
                              content = content
                              )


  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hadoop-hdfs-datanode', version), sudo=True,)
    self.assertNoMoreResources()


  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_23(self, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)
    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hadoop-hdfs-datanode', version), sudo=True,)

    self.assertNoMoreResources()


  @patch("socket.gethostbyname")
  @patch('time.sleep')
  def test_post_upgrade_restart(self, time_mock, socket_gethostbyname_mock):
    shell_call_output = """
      Live datanodes (2):

      Name: 192.168.64.102:50010 (c6401.ambari.apache.org)
      Hostname: c6401.ambari.apache.org
      Decommission Status : Normal
      Configured Capacity: 524208947200 (488.21 GB)
      DFS Used: 193069056 (184.13 MB)
      Non DFS Used: 29264986112 (27.26 GB)
      DFS Remaining: 494750892032 (460.77 GB)
      DFS Used%: 0.04%
      DFS Remaining%: 94.38%
      Configured Cache Capacity: 0 (0 B)
      Cache Used: 0 (0 B)
      Cache Remaining: 0 (0 B)
      Cache Used%: 100.00%
      Cache Remaining%: 0.00%
      Xceivers: 2
      Last contact: Fri Dec 12 20:47:21 UTC 2014
    """
    mocks_dict = {}
    socket_gethostbyname_mock.return_value = "test_host"
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                       classname = "DataNode",
                       command = "post_upgrade_restart",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, shell_call_output)],
                       mocks_dict = mocks_dict
    )

    self.assertTrue(mocks_dict['call'].called)
    self.assertEqual(mocks_dict['call'].call_count,1)


  @patch("socket.gethostbyname")
  @patch('time.sleep')
  def test_post_upgrade_restart_datanode_not_ready(self, time_mock, socket_gethostbyname_mock):
    mocks_dict = {}
    socket_gethostbyname_mock.return_value = "test_host"
    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                         classname = "DataNode",
                         command = "post_upgrade_restart",
                         config_file = "default.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES,
                         call_mocks = [(0, 'There are no DataNodes here!')] * 30,
                         mocks_dict = mocks_dict
      )
      self.fail('Missing DataNode should have caused a failure')
    except Fail,fail:
      self.assertTrue(mocks_dict['call'].called)
      self.assertEqual(mocks_dict['call'].call_count,30)


  @patch("socket.gethostbyname")
  @patch('time.sleep')
  def test_post_upgrade_restart_bad_returncode(self, time_mock, socket_gethostbyname_mock):
    try:
      mocks_dict = {}
      socket_gethostbyname_mock.return_value = "test_host"
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                         classname = "DataNode",
                         command = "post_upgrade_restart",
                         config_file = "default.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES,
                         call_mocks = [(1, 'some')] * 30,
                         mocks_dict = mocks_dict
      )
      self.fail('Invalid return code should cause a failure')
    except Fail,fail:
      self.assertTrue(mocks_dict['call'].called)
      self.assertEqual(mocks_dict['call'].call_count,30)


  @patch("resource_management.core.shell.call")
  @patch('time.sleep')
  def test_stop_during_upgrade_not_shutdown(self, time_mock, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    call_mock_side_effects = [(0, ""), ]
    call_mock.side_effects = call_mock_side_effects
    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    mocks_dict={}
    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
        classname = "DataNode",
        command = "stop",
        config_dict = json_content,
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES,
        call_mocks = call_mock_side_effects,
        checked_call_mocks=itertools.cycle([(0, "OK.")]),
        mocks_dict = mocks_dict,
        command_args=["rolling"])

      raise Fail("Expected a fail since datanode didn't report a shutdown")
    except Exception, err:
      expected_message = "DataNode has not yet deregistered from the NameNode..."
      if str(err.message) != expected_message:
        self.fail("Expected this exception to be thrown. " + expected_message + ". Got this instead, " + str(err.message))

    self.assertEquals(
      ('hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -D ipc.client.connect.max.retries=5 -D ipc.client.connect.retry.interval=1000 -getDatanodeInfo 0.0.0.0:8010'),
      mocks_dict['checked_call'].call_args_list[0][0][0])

  @patch("resource_management.core.shell.call")
  @patch('time.sleep')
  def test_stop_during_upgrade_not_shutdown_ha(self, time_mock, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/ha_default.json"
    call_mock_side_effects = [(0, ""), ]
    call_mock.side_effects = call_mock_side_effects
    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    mocks_dict={}
    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                         classname = "DataNode",
                         command = "stop",
                         config_dict = json_content,
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES,
                         call_mocks = call_mock_side_effects,
                         checked_call_mocks=itertools.cycle([(0, "OK.")]),
                         mocks_dict = mocks_dict,
                         command_args=["rolling"])

      raise Fail("Expected a fail since datanode didn't report a shutdown")
    except Exception, err:
      expected_message = "DataNode has not yet deregistered from the NameNode..."
      if str(err.message) != expected_message:
        self.fail("Expected this exception to be thrown. " + expected_message + ". Got this instead, " + str(err.message))

    self.assertEquals(
      ('hdfs dfsadmin -fs hdfs://ns1 -D ipc.client.connect.max.retries=5 -D ipc.client.connect.retry.interval=1000 -getDatanodeInfo 0.0.0.0:8010'),
      mocks_dict['checked_call'].call_args_list[0][0][0])

  def test_reload_configs(self):
      with self.assertRaises(Fail):
          self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/datanode.py",
                             classname = "DataNode",
                             command = "reload_configs",
                             config_file = "default.json",
                             stack_version = self.STACK_VERSION,
                             target = RMFTestCase.TARGET_COMMON_SERVICES
                             )

          # self.assertResourceCalled('Execute', "hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -reconfig namenode c6401.ambari.apache.org:8020 start",
          #                       tries=115,
          #                       try_sleep=10,
          #                       user="hdfs",
          #                       logoutput=True
          #                       )