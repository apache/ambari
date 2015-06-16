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


@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestOozieClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "AMBARI_METRICS/0.1.0/package"
  STACK_VERSION = "2.0.6"

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metrics_collector.py",
                       classname = "AmsCollector",
                       command = "start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.maxDiff=None
    self.assert_hbase_configure('master')
    self.assert_hbase_configure('regionserver')
    self.assert_ams('collector')
    self.assertResourceCalled('Execute', '/usr/lib/ams-hbase/bin/hbase-daemon.sh --config /etc/ams-hbase/conf start zookeeper',
                              not_if = 'ls /var/run/ambari-metrics-collector//hbase-ams-zookeeper.pid >/dev/null 2>&1 && ps `cat /var/run/ambari-metrics-collector//hbase-ams-zookeeper.pid` >/dev/null 2>&1',
                              user = 'ams'
    )
    self.assertResourceCalled('Execute', '/usr/lib/ams-hbase/bin/hbase-daemon.sh --config /etc/ams-hbase/conf start master',
                              not_if = 'ls /var/run/ambari-metrics-collector//hbase-ams-master.pid >/dev/null 2>&1 && ps `cat /var/run/ambari-metrics-collector//hbase-ams-master.pid` >/dev/null 2>&1',
                              user = 'ams'
    )
    self.assertResourceCalled('Execute', '/usr/lib/ams-hbase/bin/hbase-daemon.sh --config /etc/ams-hbase/conf start regionserver',
                              not_if = 'ls /var/run/ambari-metrics-collector//hbase-ams-regionserver.pid >/dev/null 2>&1 && ps `cat /var/run/ambari-metrics-collector//hbase-ams-regionserver.pid` >/dev/null 2>&1',
                              user = 'ams'
    )
    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-collector --config /etc/ambari-metrics-collector/conf --distributed start',
                              user = 'ams'
    )
    self.assertNoMoreResources()

  def assert_ams(self, name=None):
    self.assertResourceCalled('Directory', '/etc/ambari-metrics-collector/conf',
                              owner = 'ams',
                              group = 'hadoop',
                              recursive = True
    )
    self.assertResourceCalled('Directory', '/var/lib/ambari-metrics-collector/checkpoint',
                              owner = 'ams',
                              group = 'hadoop',
                              cd_access = 'a',
                              recursive = True
    )
    self.assertResourceCalled('XmlConfig', 'ams-site.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              conf_dir = '/etc/ambari-metrics-collector/conf',
                              configurations = self.getConfig()['configurations']['ams-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['ams-hbase-site']
    )
    merged_ams_hbase_site = {}
    merged_ams_hbase_site.update(self.getConfig()['configurations']['ams-hbase-site'])
    merged_ams_hbase_site['phoenix.query.maxGlobalMemoryPercentage'] = '25'
    merged_ams_hbase_site['phoenix.spool.directory'] = '/tmp'

    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              conf_dir = '/etc/ambari-metrics-collector/conf',
                              configurations = merged_ams_hbase_site,
                              configuration_attributes = self.getConfig()['configuration_attributes']['ams-hbase-site']
    )
    self.assertResourceCalled('File', '/etc/ambari-metrics-collector/conf/log4j.properties',
                              owner = 'ams',
                              group = 'hadoop',
                              content = "\n",
                              mode=0644,
    )
    self.assertResourceCalled('File', '/etc/ambari-metrics-collector/conf/ams-env.sh',
                              owner = 'ams',
                              content = InlineTemplate(self.getConfig()['configurations']['ams-env']['content'])
    )
    self.assertResourceCalled('Directory', '/var/log/ambari-metrics-collector',
                              owner = 'ams',
                              group = 'hadoop',
                              cd_access = 'a',
                              recursive = True
    )
    self.assertResourceCalled('Directory', '/var/run/ambari-metrics-collector',
                              owner = 'ams',
                              cd_access = 'a',
                              group = 'hadoop',
                              recursive = True
    )
    self.assertResourceCalled('File', '/usr/lib/ams-hbase/bin/hadoop',
                              owner = 'ams',
                              mode=0755
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/ams.conf',
                              owner='root',
                              group='root',
                              mode=0644,
                              content=Template("ams.conf.j2")
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              mode=0644,
                              conf_dir = '/etc/ambari-metrics-collector/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              mode=0644,
                              conf_dir = '/etc/ams-hbase/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              mode=0644,
                              conf_dir = '/etc/ambari-metrics-collector/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              mode=0644,
                              conf_dir = '/etc/ams-hbase/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )

  def assert_hbase_configure(self, name=None):
    self.assertResourceCalled('Directory', '/etc/ams-hbase/conf',
                              owner = 'ams',
                              group = 'hadoop',
                              recursive = True
    )
    self.assertResourceCalled('Directory', 'hdfs://localhost:8020/apps/hbase/data/tmp',
                              owner = 'ams',
                              cd_access = 'a',
                              recursive = True
    )
    self.assertResourceCalled('Directory', 'hdfs://localhost:8020/apps/hbase/data/tmp/local/jars',
                              owner = 'ams',
                              cd_access = 'a',
                              group = 'hadoop',
                              mode = 0775,
                              recursive = True
    )
    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              conf_dir = '/etc/ams-hbase/conf',
                              configurations = self.getConfig()['configurations']['ams-hbase-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['ams-hbase-site']
                              )
    self.assertResourceCalled('Directory', 'hdfs://localhost:8020/apps/hbase/data/tmp/phoenix-spool',
                              owner = 'ams',
                              cd_access = 'a',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True
    )
    self.assertResourceCalled('XmlConfig', 'hbase-policy.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              conf_dir = '/etc/ams-hbase/conf',
                              configurations = self.getConfig()['configurations']['ams-hbase-policy'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['ams-hbase-site']
    )
    self.assertResourceCalled('File', '/etc/ams-hbase/conf/hbase-env.sh',
                              owner = 'ams',
                              content = InlineTemplate(self.getConfig()['configurations']['ams-hbase-env']['content'])
                              )
    self.assertResourceCalled('File', '/etc/ams-hbase/conf/hadoop-metrics2-hbase.properties',
                              owner = 'ams',
                              group = 'hadoop',
                              content = Template('hadoop-metrics2-hbase.properties.j2')
                              )
    self.assertResourceCalled('TemplateConfig', '/etc/ams-hbase/conf/regionservers',
                              owner = 'ams',
                              template_tag = None,
                              )
    if name == 'master':
      self.assertResourceCalled('HdfsResource', 'hdfs://localhost:8020/apps/hbase/data',
                                security_enabled = False,
                                hadoop_bin_dir = '/usr/bin',
                                keytab = UnknownConfigurationMock(),
                                kinit_path_local = '/usr/bin/kinit',
                                user = 'hdfs',
                                owner = 'ams',
                                mode = 0775,
                                hadoop_conf_dir = '/etc/hadoop/conf',
                                type = 'directory',
                                action = ['create_on_execute'],
                                hdfs_site=self.getConfig()['configurations']['hdfs-site'],
                                principal_name=UnknownConfigurationMock(),
                                default_fs='hdfs://c6401.ambari.apache.org:8020',
                                )
      self.assertResourceCalled('HdfsResource', '/amshbase/staging',
                                security_enabled = False,
                                hadoop_bin_dir = '/usr/bin',
                                keytab = UnknownConfigurationMock(),
                                kinit_path_local = '/usr/bin/kinit',
                                user = 'hdfs',
                                owner = 'ams',
                                mode = 0711,
                                hadoop_conf_dir = '/etc/hadoop/conf',
                                type = 'directory',
                                action = ['create_on_execute'],
                                hdfs_site=self.getConfig()['configurations']['hdfs-site'],
                                principal_name=UnknownConfigurationMock(),
                                default_fs='hdfs://c6401.ambari.apache.org:8020',
                                )
      self.assertResourceCalled('HdfsResource', None,
                                security_enabled = False,
                                hadoop_bin_dir = '/usr/bin',
                                keytab = UnknownConfigurationMock(),
                                kinit_path_local = '/usr/bin/kinit',
                                user = 'hdfs',
                                hadoop_conf_dir = '/etc/hadoop/conf',
                                action = ['execute'],
                                hdfs_site=self.getConfig()['configurations']['hdfs-site'],
                                principal_name=UnknownConfigurationMock(),
                                default_fs='hdfs://c6401.ambari.apache.org:8020',
                                )
    self.assertResourceCalled('Directory', '/var/run/ambari-metrics-collector/',
                              owner = 'ams',
                              recursive = True
    )
    self.assertResourceCalled('Directory', '/var/log/ambari-metrics-collector',
                              owner = 'ams',
                              recursive = True
    )
    self.assertResourceCalled('File', '/etc/ams-hbase/conf/log4j.properties',
                              owner = 'ams',
                              group = 'hadoop',
                              mode = 0644,
                              content = "\n"
    )
