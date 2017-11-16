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
import tempfile

@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch.object(tempfile, "gettempdir", new=MagicMock(return_value="/tmp"))
class TestFalconServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "FALCON/0.5.0.2.1/package"
  STACK_VERSION = "2.1"
  UPGRADE_STACK_VERSION = "2.2"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  CONFIG_OVERRIDES = {"serviceName":"FALCON", "role":"FALCON_SERVER"}

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
      classname="FalconServer",
      command="start",
      config_file="default.json",
      config_overrides = self.CONFIG_OVERRIDES,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assert_configure_default()

    self.assertResourceCalled('Execute', '/usr/hdp/current/falcon-server/bin/falcon-config.sh server falcon',
      path = ['/usr/bin'],
      user = 'falcon',
      not_if = 'ls /var/run/falcon/falcon.pid && ps -p ',
    )

    self.assertResourceCalled('File', '/usr/hdp/current/falcon-server/server/webapp/falcon/WEB-INF/lib/je-5.0.73.jar',
      content=DownloadSource('http://c6401.ambari.apache.org:8080/resources//je-5.0.73.jar'),
      mode=0755
    )

    self.assertResourceCalled('Execute', '/usr/hdp/current/falcon-server/bin/falcon-start -port 15000',
      path = ['/usr/bin'],
      user = 'falcon',
      not_if = 'ls /var/run/falcon/falcon.pid && ps -p ',
    )

    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
      classname="FalconServer",
      command="stop",
      config_file="default.json",
      config_overrides = self.CONFIG_OVERRIDES,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Execute', '/usr/hdp/current/falcon-server/bin/falcon-stop',
      path = ['/usr/bin'],
      user = 'falcon')

    self.assertResourceCalled('File', '/var/run/falcon/falcon.pid',
      action = ['delete'])

    self.assertNoMoreResources()

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname="FalconServer",
                       command="configure",
                       config_file="default.json",
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/falcon',
                              owner = 'falcon',
                              create_parents = True,
                              cd_access = "a",
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/falcon',
                              owner = 'falcon',
                              create_parents = True,
                              cd_access = "a",
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/falcon-server/webapp',
                              owner = 'falcon',
                              create_parents = True
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/falcon-server',
                              owner = 'falcon',
                              create_parents = True
                              )
    self.assertResourceCalled('Directory', '/etc/falcon',
                              mode = 0755,
                              create_parents = True
    )

    self.assertResourceCalled('Directory', '/etc/falcon/conf',
                              owner = 'falcon',
                              create_parents = True
    )
    self.assertResourceCalled('File', '/etc/falcon/conf/falcon-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['falcon-env']['content']),
                              owner = 'falcon',
                              group = 'hadoop'
                              )

    self.assertResourceCalled('PropertiesFile', '/etc/falcon/conf/client.properties',
                              mode = 0644,
                              owner = 'falcon',
                              properties = {u'falcon.url': u'http://{{falcon_host}}:{{falcon_port}}'}
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

    self.assertResourceCalled('File', '/etc/falcon/conf/log4j.properties',
                          content=InlineTemplate(self.getConfig()['configurations']['falcon-log4j']['content']),
                          owner='falcon',
                          group='hadoop',
                          mode= 0644
                          )

    self.assertResourceCalled('Directory', '/hadoop/falcon/store',
                              owner = 'falcon',
                              create_parents = True
                              )
    self.assertResourceCalled('HdfsResource', '/apps/falcon',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'falcon',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/apps/data-mirroring',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'falcon',
        group='users',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        recursive_chown = True,
        recursive_chmod = True,
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0770,
        source='/usr/hdp/current/falcon-server/data-mirroring'
    )

    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Directory', '/hadoop/falcon',
                              owner = 'falcon',
                              create_parents = True,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/hadoop/falcon/embeddedmq',
                              owner = 'falcon',
                              create_parents = True
                              )
    self.assertResourceCalled('Directory', '/hadoop/falcon/embeddedmq/data',
                              owner = 'falcon',
                              create_parents = True
                              )

  @patch("os.path.isdir")
  @patch("os.path.exists")
  @patch("os.path.isfile")
  def test_upgrade(self, isfile_mock, exists_mock, isdir_mock):

    isdir_mock.return_value = True
    exists_mock.side_effect = [False,True, True, True]
    isfile_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
     classname = "FalconServer", command = "restart", config_file = "falcon-upgrade.json",
     config_overrides = self.CONFIG_OVERRIDES,
     stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES )

    self.assertResourceCalled('Execute',
      '/usr/hdp/current/falcon-server/bin/falcon-stop',
      path = ['/usr/hdp/2.2.1.0-2135/hadoop/bin'], user='falcon')

    self.assertResourceCalled('File', '/var/run/falcon/falcon.pid',
      action = ['delete'])

    self.assertResourceCalled('Execute', ('tar',
     '-zcvhf',
     '/tmp/falcon-upgrade-backup/falcon-local-backup.tar',
     u'/hadoop/falcon'),
        sudo = True, tries = 3, try_sleep = 1,
    )
    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'falcon-server', u'2.2.1.0-2135'),
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('tar',
     '-xf',
     '/tmp/falcon-upgrade-backup/falcon-local-backup.tar',
     '-C',
     u'/hadoop/falcon/'),
        sudo = True, tries = 3, try_sleep = 1,
    )
    self.assertResourceCalled('Directory', '/tmp/falcon-upgrade-backup',
        action = ['delete'],
    )
    self.assertResourceCalled('Directory', '/var/run/falcon',
        owner = 'falcon',
        create_parents = True,
        cd_access = "a",
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/log/falcon',
        owner = 'falcon',
        create_parents = True,
        cd_access = "a",
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/falcon-server/webapp',
        owner = 'falcon',
        create_parents = True,
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/falcon-server',
        owner = 'falcon',
        create_parents = True,
    )
    self.assertResourceCalled('Directory', '/etc/falcon',
        create_parents = True,
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/falcon-server/conf',
        owner = 'falcon',
        create_parents = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/falcon-server/conf/falcon-env.sh',
        owner = 'falcon',
        content = InlineTemplate(self.getConfig()['configurations']['falcon-env']['content']),
        group = 'hadoop'
    )
    self.assertResourceCalled('PropertiesFile', '/usr/hdp/current/falcon-server/conf/client.properties',
        owner = u'falcon',
        properties = {u'falcon.url': u'http://{{falcon_host}}:{{falcon_port}}'},
        mode = 0644,
    )
    self.assertResourceCalled('PropertiesFile', '/usr/hdp/current/falcon-server/conf/runtime.properties',
        owner = 'falcon',
        mode = 0644,
        properties = {u'*.domain': u'${falcon.app.type}',
           u'*.log.cleanup.frequency.days.retention': u'days(7)',
           u'*.log.cleanup.frequency.hours.retention': u'minutes(1)',
           u'*.log.cleanup.frequency.minutes.retention': u'hours(6)',
           u'*.log.cleanup.frequency.months.retention': u'months(3)'},
    )
    self.assertResourceCalled('PropertiesFile', '/usr/hdp/current/falcon-server/conf/startup.properties',
        owner = 'falcon',
        mode = 0644,
        properties = self.getConfig()['configurations']['falcon-startup.properties'],
    )

    self.assertResourceCalled('File', '/usr/hdp/current/falcon-server/conf/log4j.properties',
                          content=InlineTemplate(self.getConfig()['configurations']['falcon-log4j']['content']),
                          owner='falcon',
                          group='hadoop',
                          mode= 0644
                          )

    self.assertResourceCalled('Directory', '/hadoop/falcon/data/lineage/graphdb',
        owner = 'falcon',
        create_parents = True,
        group = 'hadoop',
        mode = 0775,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/hadoop/falcon/data/lineage',
        owner = 'falcon',
        create_parents = True,
        group = 'hadoop',
        mode = 0775,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/hadoop/falcon/store',
        owner = 'falcon',
        create_parents = True,
    )
    self.assertResourceCalled('HdfsResource', '/apps/falcon',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/2.2.1.0-2135/hadoop/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        dfs_type = '',
        owner = 'falcon',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/apps/data-mirroring',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/2.2.1.0-2135/hadoop/bin',
        keytab = UnknownConfigurationMock(),
        source = '/usr/hdp/current/falcon-server/data-mirroring',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        user = 'hdfs',
        dfs_type = '',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        recursive_chmod = True,
        recursive_chown = True,
        owner = 'falcon',
        group = 'users',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0770,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/2.2.1.0-2135/hadoop/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Directory', '/hadoop/falcon',
        owner = 'falcon',
        create_parents = True,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/hadoop/falcon/embeddedmq',
        owner = 'falcon',
        create_parents = True,
    )
    self.assertResourceCalled('Directory', '/hadoop/falcon/embeddedmq/data',
        owner = 'falcon',
        create_parents = True,
    )

    self.assertResourceCalled('Execute', '/usr/hdp/current/falcon-server/bin/falcon-config.sh server falcon',
        path = ['/usr/hdp/2.2.1.0-2135/hadoop/bin'],
        user = 'falcon',
        not_if = 'ls /var/run/falcon/falcon.pid && ps -p ',
    )

    self.assertResourceCalled('Execute', '/usr/hdp/current/falcon-server/bin/falcon-start -port 15000',
        path = ['/usr/hdp/2.2.1.0-2135/hadoop/bin'],
        user = 'falcon',
        not_if = 'ls /var/run/falcon/falcon.pid && ps -p ',
    )
    self.assertNoMoreResources()

  @patch('os.path.isfile', new=MagicMock(return_value=True))
  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/falcon-upgrade.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname = "FalconServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'falcon-server', version), sudo=True,)
    self.assertResourceCalled('Execute', ('tar',
                                          '-xf',
                                          '/tmp/falcon-upgrade-backup/falcon-local-backup.tar',
                                          '-C',
                                          u'/hadoop/falcon/'),
                              tries = 3,
                              sudo = True,
                              try_sleep = 1,
                              )
    self.assertResourceCalled('Directory', '/tmp/falcon-upgrade-backup',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  @patch('os.path.isfile', new=MagicMock(return_value=True))
  @patch.object(tarfile, 'open')
  @patch.object(shutil, 'rmtree')
  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_23(self, tarfile_open_mock, rmtree_mock, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/falcon-upgrade.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/falcon_server.py",
                       classname = "FalconServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'falcon-server', version), sudo=True,)

    self.assertResourceCalled('Execute', ('tar',
     '-xf',
     '/tmp/falcon-upgrade-backup/falcon-local-backup.tar',
     '-C',
     u'/hadoop/falcon/'),
        sudo = True, tries = 3, try_sleep = 1,
    )
    self.assertResourceCalled('Directory', '/tmp/falcon-upgrade-backup',
        action = ['delete'],
    )
    self.assertNoMoreResources()
