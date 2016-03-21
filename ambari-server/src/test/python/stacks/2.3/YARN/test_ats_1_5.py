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
import os
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
from resource_management.libraries.functions import version
from resource_management.libraries.script.script import Script
from resource_management.libraries import functions

origin_exists = os.path.exists
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch.object(os.path, "exists", new=MagicMock(
  side_effect=lambda *args: origin_exists(args[0])
  if args[0][-2:] == "j2" else True))

@patch.object(Script, "is_hdp_stack_greater_or_equal", new = MagicMock(return_value=False))
@patch.object(functions, "get_hdp_version", new = MagicMock(return_value="2.0.0.0-1234"))
class TestAts(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "YARN/2.1.0.2.0/package"
  STACK_VERSION = "2.3"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/application_timeline_server.py",
                       classname="ApplicationTimelineServer",
                       command="configure",
                       config_file="ats_1_5.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn',
                              owner = 'yarn',
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
                              owner = 'yarn',
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
                              owner = 'yarn',
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce',
                              owner = 'mapred',
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
                              owner = 'mapred',
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce',
                              owner = 'mapred',
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
                              owner = 'mapred',
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
                              owner = 'yarn',
                              ignore_failures = True,
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'hadoop.proxyuser.hive.groups': u'true',
                                                                     u'hadoop.proxyuser.oozie.hosts': u'true',
                                                                     u'webinterface.private.actions': u'true'}},
                              owner = 'hdfs',
                              configurations = self.getConfig()['configurations']['core-site'],
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'dfs.cluster.administrators': u'true',
                                                                     u'dfs.support.append': u'true',
                                                                     u'dfs.web.ugi': u'true'}},
                              owner = 'hdfs',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner = 'yarn',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'yarn.nodemanager.container-executor.class': u'true',
                                                                     u'yarn.nodemanager.disk-health-checker.min-healthy-disks': u'true',
                                                                     u'yarn.nodemanager.local-dirs': u'true'}},
                              owner = 'yarn',
                              configurations = self.getConfig()['configurations']['yarn-site'],
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'yarn.scheduler.capacity.node-locality-delay': u'true'}},
                              owner = 'yarn',
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/timeline',
                              owner = 'yarn',
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('HdfsResource', '/ats',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/bin',
                              keytab = UnknownConfigurationMock(),
                              default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = UnknownConfigurationMock(),
                              user = 'hdfs',
                              change_permissions_for_parents = True,
                              owner = 'yarn',
                              group = 'hadoop',
                              hadoop_conf_dir = '/etc/hadoop/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              mode = 0755,
                              dfs_type = ''
                              )
    self.assertResourceCalled('HdfsResource', '/ats/done',
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
                              group = 'hadoop',
                              hadoop_conf_dir = '/etc/hadoop/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              mode = 0700,
                              dfs_type = ''
                              )
    self.assertResourceCalled('HdfsResource', '/ats',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/bin',
                              keytab = UnknownConfigurationMock(),
                              default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = UnknownConfigurationMock(),
                              user = 'hdfs',
                              change_permissions_for_parents = True,
                              owner = 'yarn',
                              group = 'hadoop',
                              hadoop_conf_dir = '/etc/hadoop/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              mode = 0755,
                              dfs_type = ''
                              )
    self.assertResourceCalled('HdfsResource', '/ats/active',
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
                              group = 'hadoop',
                              hadoop_conf_dir = '/etc/hadoop/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              mode = 01777,
                              dfs_type = ''
                              )
    self.assertResourceCalled('HdfsResource', None,
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/bin',
                              keytab = UnknownConfigurationMock(),
                              default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = UnknownConfigurationMock(),
                              user = 'hdfs',
                              action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hadoop_conf_dir = '/etc/hadoop/conf',
                              dfs_type = ''
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn.exclude',
                              owner = 'yarn',
                              group = 'hadoop',
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
                              mode = 0755,
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
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
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configuration_attributes = {u'final': {u'yarn.scheduler.capacity.node-locality-delay': u'true'}},
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
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
