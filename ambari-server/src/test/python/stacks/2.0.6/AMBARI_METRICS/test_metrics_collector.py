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

@patch("tempfile.mkdtemp", new = MagicMock(return_value='/some_tmp_dir'))
@patch("os.path.exists", new = MagicMock(return_value=True))
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestMetricsCollector(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "AMBARI_METRICS/0.1.0/package"
  STACK_VERSION = "2.0.6"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def test_start_default_distributed(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metrics_collector.py",
                       classname = "AmsCollector",
                       command = "start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.maxDiff=None
    self.assert_hbase_configure('master', distributed=True)
    self.assert_hbase_configure('regionserver', distributed=True)
    self.assert_ams('collector', distributed=True)
    self.assertResourceCalled('Execute', 'ambari-sudo.sh /usr/jdk64/jdk1.7.0_45/bin/keytool -importkeystore -srckeystore /etc/security/clientKeys/all.jks -destkeystore /some_tmp_dir/truststore.p12 -deststoretype PKCS12 -srcstorepass bigdata -deststorepass bigdata',
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh openssl pkcs12 -in /some_tmp_dir/truststore.p12 -out /etc/ambari-metrics-collector/conf/ca.pem -cacerts -nokeys -passin pass:bigdata',
                              )
    self.assertResourceCalled('Execute', ('chown', u'ams', '/etc/ambari-metrics-collector/conf/ca.pem'),
                              sudo=True
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh rm -rf /some_tmp_dir',
                              )
    self.assertResourceCalled('Execute', '/usr/lib/ams-hbase/bin/hbase-daemon.sh --config /etc/ams-hbase/conf stop regionserver',
                              on_timeout = 'ls /var/run/ambari-metrics-collector//hbase-ams-regionserver.pid >/dev/null 2>&1 && ps `cat /var/run/ambari-metrics-collector//hbase-ams-regionserver.pid` >/dev/null 2>&1 && ambari-sudo.sh -H -E kill -9 `ambari-sudo.sh cat /var/run/ambari-metrics-collector//hbase-ams-regionserver.pid`',
                              timeout = 30,
                              user = 'ams'
                              )
    self.assertResourceCalled('File', '/var/run/ambari-metrics-collector//hbase-ams-regionserver.pid',
                              action = ['delete']
                              )
    self.assertResourceCalled('Execute', '/usr/lib/ams-hbase/bin/hbase-daemon.sh --config /etc/ams-hbase/conf stop master',
                              on_timeout = 'ls /var/run/ambari-metrics-collector//hbase-ams-master.pid >/dev/null 2>&1 && ps `cat /var/run/ambari-metrics-collector//hbase-ams-master.pid` >/dev/null 2>&1 && ambari-sudo.sh -H -E kill -9 `ambari-sudo.sh cat /var/run/ambari-metrics-collector//hbase-ams-master.pid`',
                              timeout = 30,
                              user = 'ams'
    )
    self.assertResourceCalled('File', '/var/run/ambari-metrics-collector//hbase-ams-master.pid',
                              action = ['delete']
    )
    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-collector --config /etc/ambari-metrics-collector/conf --distributed stop',
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
    self.assertResourceCalled('Execute', 'ambari-sudo.sh rm -rf /var/lib/ambari-metrics-collector/hbase-tmp/*.tmp',
    )
    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-collector --config /etc/ambari-metrics-collector/conf --distributed start',
                              user = 'ams'
    )
    self.assertNoMoreResources()

  def test_start_default_embedded(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metrics_collector.py",
                       classname = "AmsCollector",
                       command = "start",
                       config_file="default_ams_embedded.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.maxDiff=None
    self.assert_hbase_configure('master')
    self.assert_hbase_configure('regionserver')
    self.assert_ams('collector')
    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-collector --config /etc/ambari-metrics-collector/conf stop',
                              user = 'ams'
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh rm -rf /var/lib/ambari-metrics-collector/hbase-tmp/*.tmp',
    )
    self.assertResourceCalled('Directory', '/var/lib/ambari-metrics-collector/hbase-tmp/zookeeper',
                              action = ['delete']
    )

    self.assertResourceCalled('File', '/etc/ambari-metrics-collector/conf/core-site.xml',
                                owner = 'ams',
                                action = ['delete']
                                )

    self.assertResourceCalled('File', '/etc/ambari-metrics-collector/conf/hdfs-site.xml',
                              owner = 'ams',
                              action = ['delete']
                              )

    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-collector --config /etc/ambari-metrics-collector/conf start',
                              user = 'ams'
    )
    self.assertNoMoreResources()

  def assert_ams(self, name=None, distributed=False):
    self.assertResourceCalled('Directory', '/etc/ambari-metrics-collector/conf',
                              owner = 'ams',
                              group = 'hadoop',
                              recursive = True
    )

    self.assertResourceCalled('Execute', ('chown','-R', u'ams', '/etc/ambari-metrics-collector/conf'),
                              sudo=True)

    self.assertResourceCalled('Directory', '/var/lib/ambari-metrics-collector/checkpoint',
                              owner = 'ams',
                              group = 'hadoop',
                              cd_access = 'a',
                              recursive = True
    )

    self.assertResourceCalled('Execute', ('chown','-R', u'ams', '/var/lib/ambari-metrics-collector/checkpoint'),
                              sudo=True)

    self.assertResourceCalled('XmlConfig', 'ams-site.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              conf_dir = '/etc/ambari-metrics-collector/conf',
                              configurations = self.getConfig()['configurations']['ams-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['ams-hbase-site']
    )

    self.assertResourceCalled('XmlConfig', 'ssl-server.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              conf_dir = '/etc/ambari-metrics-collector/conf',
                              configurations = self.getConfig()['configurations']['ams-ssl-server'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['ams-ssl-server']
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
                              recursive = True,
                              mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/run/ambari-metrics-collector',
                              owner = 'ams',
                              cd_access = 'a',
                              group = 'hadoop',
                              recursive = True,
                              mode=0755,
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
    if distributed:
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

  def assert_hbase_configure(self, name=None, distributed=False):
    self.assertResourceCalled('Directory', '/etc/ams-hbase/conf',
                              owner = 'ams',
                              group = 'hadoop',
                              recursive = True
    )

    self.assertResourceCalled('Execute', ('chown','-R', u'ams', '/etc/ams-hbase/conf'),
                                sudo=True)

    self.assertResourceCalled('Directory', '/var/lib/ambari-metrics-collector/hbase-tmp',
                              owner = 'ams',
                              cd_access = 'a',
                              recursive = True
    )

    self.assertResourceCalled('Execute', ('chown','-R', u'ams', '/var/lib/ambari-metrics-collector/hbase-tmp'),
                            sudo=True)

    self.assertResourceCalled('Directory', '/var/lib/ambari-metrics-collector/hbase-tmp/local/jars',
                              owner = 'ams',
                              cd_access = 'a',
                              group = 'hadoop',
                              mode = 0775,
                              recursive = True
    )

    if not distributed:
      self.assertResourceCalled('File', '/etc/ams-hbase/conf/core-site.xml',
                                owner = 'ams',
                                action = ['delete']
                                )

      self.assertResourceCalled('File', '/etc/ams-hbase/conf/hdfs-site.xml',
                                owner = 'ams',
                                action = ['delete']
                                )

    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              conf_dir = '/etc/ams-hbase/conf',
                              configurations = self.getConfig()['configurations']['ams-hbase-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['ams-hbase-site']
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
    self.assertResourceCalled('Directory', '/var/run/ambari-metrics-collector/',
                              owner = 'ams',
                              recursive = True,
                              mode = 0755,
                              cd_access = "a",
    )
    self.assertResourceCalled('Directory', '/var/log/ambari-metrics-collector',
                              owner = 'ams',
                              recursive = True,
                              mode = 0755,
                              cd_access = "a",
    )

    if name == 'master':
      if distributed:
        self.assertResourceCalled('HdfsResource', 'hdfs://localhost:8020/apps/hbase/data',
                                  immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                                  security_enabled = False,
                                  hadoop_bin_dir = '/usr/bin',
                                  keytab = UnknownConfigurationMock(),
                                  kinit_path_local = '/usr/bin/kinit',
                                  user = 'hdfs',
                                  dfs_type = '',
                                  owner = 'ams',
                                  mode = 0775,
                                  hadoop_conf_dir = '/etc/hadoop/conf',
                                  type = 'directory',
                                  action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                                  hdfs_site=self.getConfig()['configurations']['hdfs-site'],
                                  principal_name=UnknownConfigurationMock(),
                                  default_fs='hdfs://c6401.ambari.apache.org:8020',
                                  )
        self.assertResourceCalled('HdfsResource', '/amshbase/staging',
                                  immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                                  security_enabled = False,
                                  hadoop_bin_dir = '/usr/bin',
                                  keytab = UnknownConfigurationMock(),
                                  kinit_path_local = '/usr/bin/kinit',
                                  user = 'hdfs',
                                  dfs_type = '',
                                  owner = 'ams',
                                  mode = 0711,
                                  hadoop_conf_dir = '/etc/hadoop/conf',
                                  type = 'directory',
                                  action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                                  hdfs_site=self.getConfig()['configurations']['hdfs-site'],
                                  principal_name=UnknownConfigurationMock(),
                                  default_fs='hdfs://c6401.ambari.apache.org:8020',
                                  )
        self.assertResourceCalled('HdfsResource', None,
                                  immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                                  security_enabled = False,
                                  hadoop_bin_dir = '/usr/bin',
                                  keytab = UnknownConfigurationMock(),
                                  kinit_path_local = '/usr/bin/kinit',
                                  user = 'hdfs',
                                  hadoop_conf_dir = '/etc/hadoop/conf',
                                  action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                                  hdfs_site=self.getConfig()['configurations']['hdfs-site'],
                                  principal_name=UnknownConfigurationMock(),
                                  default_fs='hdfs://c6401.ambari.apache.org:8020',
                                  )
        self.assertResourceCalled('File', '/var/run/ambari-metrics-collector//distributed_mode', action=["create"],
                                  mode=0644, owner='ams')
      else:
        self.assertResourceCalled('Directory', '/var/lib/ambari-metrics-collector/hbase',
                                  owner = 'ams',
                                  cd_access="a",
                                  recursive = True
        )

      if (not distributed):
        self.assertResourceCalled('Execute', ('chown','-R','ams', '/var/lib/ambari-metrics-collector/hbase'),
                                  sudo=True)


        self.assertResourceCalled('File', '/var/run/ambari-metrics-collector//distributed_mode',
                                  owner = 'ams',
                                  action = ['delete']
        )
    self.assertResourceCalled('File', '/etc/ams-hbase/conf/log4j.properties',
                              owner = 'ams',
                              group = 'hadoop',
                              mode = 0644,
                              content = "\n"
    )
