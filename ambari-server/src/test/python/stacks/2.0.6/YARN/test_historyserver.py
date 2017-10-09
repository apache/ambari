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
from mock.mock import MagicMock, patch, call
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import version
from stacks.utils.RMFTestCase import *
import os
from resource_management.libraries import functions

origin_exists = os.path.exists
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch.object(os.path, "exists", new=MagicMock(
  side_effect=lambda *args: origin_exists(args[0])
  if args[0][-2:] == "j2" else True))
class TestHistoryServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "YARN/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  CONFIG_OVERRIDES = {"serviceName":"MAPREDUCE2", "role":"HISTORYSERVER"}

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()

    pid_check_cmd = 'ls /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid >/dev/null 2>&1 && ps -p `cat /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid` >/dev/null 2>&1'

    self.assertResourceCalled("HdfsResource", "/apps/tez/",
                          immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                          type="directory",
                          action=["create_on_execute"],
                          hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                          user=u"hdfs",
                          dfs_type = '',
                          owner=u"tez",
                          mode=493,
                          hadoop_bin_dir="/usr/bin",
                          hadoop_conf_dir="/etc/hadoop/conf",
                          hdfs_site=self.getConfig()["configurations"]["hdfs-site"],
                          default_fs=u'hdfs://c6401.ambari.apache.org:8020',
                          security_enabled=False,
                          kinit_path_local="/usr/bin/kinit",
                          keytab=UnknownConfigurationMock(),
                          principal_name=UnknownConfigurationMock()
                          )

    self.assertResourceCalled("HdfsResource", "/apps/tez/lib/",
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              type="directory",
                              action=["create_on_execute"],
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              user=u'hdfs',
                              owner=u"tez",
                              dfs_type = '',
                              mode=493,
                              hadoop_bin_dir="/usr/bin",
                              hadoop_conf_dir="/etc/hadoop/conf",
                              hdfs_site=self.getConfig()["configurations"]["hdfs-site"],
                              default_fs=u'hdfs://c6401.ambari.apache.org:8020',
                              security_enabled=False,
                              kinit_path_local="/usr/bin/kinit",
                              keytab=UnknownConfigurationMock(),
                              principal_name=UnknownConfigurationMock()
    )

    self.assertResourceCalled("HdfsResource", None,
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              action=['execute'],
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              user=u'hdfs',
                              hadoop_bin_dir="/usr/bin",
                              dfs_type = '',
                              hadoop_conf_dir="/etc/hadoop/conf",
                              hdfs_site=self.getConfig()["configurations"]["hdfs-site"],
                              default_fs=u'hdfs://c6401.ambari.apache.org:8020',
                              security_enabled=False,
                              kinit_path_local="/usr/bin/kinit",
                              keytab=UnknownConfigurationMock(),
                              principal_name=UnknownConfigurationMock()
                          )

    self.assertResourceCalled('File', '/var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
        action = ['delete'],
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
    )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited; export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-mapreduce/sbin/mr-jobhistory-daemon.sh --config /etc/hadoop/conf start historyserver',
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
        user = 'mapred',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
        tries = 5,
        try_sleep = 1,
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="stop",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-mapreduce/sbin/mr-jobhistory-daemon.sh --config /etc/hadoop/conf stop historyserver',
                              user='mapred')
    self.assertResourceCalled('File', '/var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
                              action=['delete'])
    self.assertNoMoreResources()

  def test_configure_secured(self):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="configure",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="start",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()

    self.assertResourceCalled('File', '/var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
        action = ['delete'],
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
    )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited; export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-mapreduce/sbin/mr-jobhistory-daemon.sh --config /etc/hadoop/conf start historyserver',
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
        user = 'mapred',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
        tries = 5,
        try_sleep = 1,
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname="HistoryServer",
                       command="stop",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-mapreduce/sbin/mr-jobhistory-daemon.sh --config /etc/hadoop/conf stop historyserver',
                              user='mapred')

    self.assertResourceCalled('File', '/var/run/hadoop-mapreduce/mapred/mapred-mapred-historyserver.pid',
                              action=['delete'])
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      ignore_failures = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('HdfsResource', '/app-logs',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_conf_dir = '/etc/hadoop/conf',
        keytab = UnknownConfigurationMock(),
        user = 'hdfs',
        dfs_type = '',
        kinit_path_local = '/usr/bin/kinit',
        recursive_chmod = True,
        owner = 'yarn',
        group = 'hadoop',
        hadoop_bin_dir = '/usr/bin',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 01777,
    )
    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0777
      )

    self.assertResourceCalled('HdfsResource', '/tmp/entity-file-history/active',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        owner = 'yarn',
        dfs_type = '',
        group = 'hadoop',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )
    self.assertResourceCalled('HdfsResource', '/mapred',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs', 
        dfs_type = '',
        owner = 'mapred',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
    )
    self.assertResourceCalled('HdfsResource', '/mapred/system',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
    )
    self.assertResourceCalled('HdfsResource', '/mr-history/done',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_conf_dir = '/etc/hadoop/conf',
        keytab = UnknownConfigurationMock(),
        change_permissions_for_parents = True,
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'mapred',
        group = 'hadoop',
        hadoop_bin_dir = '/usr/bin',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0777,
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
    self.assertResourceCalled('Directory', '/hadoop/mapreduce/jhs',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
      recursive_ownership = True,
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['yarn-site']
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
      configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['yarn-env']['content']),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/usr/lib/hadoop-yarn/bin/container-executor',
                              group = 'hadoop',
                              mode = 02050,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/container-executor.cfg',
                              content = Template('container-executor.cfg.j2'),
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('Directory', '/cgroups_test/cpu',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access="a"
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['mapred-env']['content']),
                              mode = 0755,
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-client.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-server.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      ignore_failures = True,
      cd_access = 'a',
    )

    self.assertResourceCalled('HdfsResource', '/app-logs',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_conf_dir = '/etc/hadoop/conf',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        user = 'hdfs',
        dfs_type = '',
        kinit_path_local = '/usr/bin/kinit',
        recursive_chmod = True,
        owner = 'yarn',
        group = 'hadoop',
        hadoop_bin_dir = '/usr/bin',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 01777,
    )

    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0777
    )
    self.assertResourceCalled('HdfsResource', '/tmp/entity-file-history/active',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs',
        dfs_type = '',
        user = 'hdfs',
        owner = 'yarn',
        group = 'hadoop',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )
    self.assertResourceCalled('HdfsResource', '/mapred',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'mapred',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
    )
    self.assertResourceCalled('HdfsResource', '/mapred/system',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
    )
    self.assertResourceCalled('HdfsResource', '/mr-history/done',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_conf_dir = '/etc/hadoop/conf',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        change_permissions_for_parents = True,
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'mapred',
        group = 'hadoop',
        hadoop_bin_dir = '/usr/bin',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Directory', '/hadoop/mapreduce/jhs',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
      recursive_ownership = True,
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['yarn-site']
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
      configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['yarn-env']['content']),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/usr/lib/hadoop-yarn/bin/container-executor',
      group = 'hadoop',
      mode = 06050,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/container-executor.cfg',
      content = Template('container-executor.cfg.j2'),
      group = 'hadoop',
      mode = 0644,
    )
    self.assertResourceCalled('Directory', '/cgroups_test/cpu',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access="a"
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['mapred-env']['content']),
                              mode = 0755,
                              owner = 'root',
                              )
    self.assertResourceCalled('File', '/usr/lib/hadoop/sbin/task-controller',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 06050,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn_jaas.conf',
                              content = Template('yarn_jaas.conf.j2'),
                              owner = 'yarn',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn_nm_jaas.conf',
                              content = Template('yarn_nm_jaas.conf.j2'),
                              owner = 'yarn',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred_jaas.conf',
                              content = Template('mapred_jaas.conf.j2'),
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-client.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-server.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )

  @patch.object(functions, "get_stack_version", new = MagicMock(return_value="2.3.0.0-1234"))
  @patch("resource_management.libraries.functions.copy_tarball.copy_to_hdfs")
  @patch("resource_management.core.sudo.path_isdir", new = MagicMock(return_value = True))
  def test_pre_upgrade_restart_23(self, copy_to_hdfs_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_version'] = '2.3'
    copy_to_hdfs_mock.return_value = True
    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historyserver.py",
                       classname = "HistoryServer",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None, None), (0, None, None), (0, None, None), (0, None, None)],
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hadoop-mapreduce-historyserver', version), sudo=True)
    self.assertTrue(call("tez", "hadoop", "hdfs", skip=False) in copy_to_hdfs_mock.call_args_list)
    self.assertTrue(call("slider", "hadoop", "hdfs", skip=False) in copy_to_hdfs_mock.call_args_list)

    # From call to conf_select.get_hadoop_conf_dir()

    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/2.3.0.0-1234/hadoop/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/usr/hdp/2.3.0.0-1234/hadoop/conf',
    )

    self.assertNoMoreResources()
