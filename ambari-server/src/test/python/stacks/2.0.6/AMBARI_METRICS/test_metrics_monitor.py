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
class TestMetricsMonitor(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "AMBARI_METRICS/0.1.0/package"
  STACK_VERSION = "2.0.6"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def test_start_default_with_aggregator_https(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metrics_monitor.py",
                       classname = "AmsMonitor",
                       command = "start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.maxDiff=None
    self.assert_ams(inmemory_aggregation = False)
    self.assertResourceCalled('Execute', 'ambari-sudo.sh /usr/jdk64/jdk1.7.0_45/bin/keytool -importkeystore -srckeystore /etc/security/clientKeys/all.jks -destkeystore /some_tmp_dir/truststore.p12 -srcalias c6402.ambari.apache.org -deststoretype PKCS12 -srcstorepass bigdata -deststorepass bigdata',
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh openssl pkcs12 -in /some_tmp_dir/truststore.p12 -out /etc/ambari-metrics-monitor/conf/ca.pem -cacerts -nokeys -passin pass:bigdata',
                              )
    self.assertResourceCalled('Execute', ('chown', u'ams:hadoop', '/etc/ambari-metrics-monitor/conf/ca.pem'),
                              sudo=True
                              )
    self.assertResourceCalled('Execute', ('chmod', '644', '/etc/ambari-metrics-monitor/conf/ca.pem'),
                              sudo=True)
    self.assertResourceCalled('Execute', 'ambari-sudo.sh rm -rf /some_tmp_dir',
                              )
    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-monitor --config /etc/ambari-metrics-monitor/conf start',
                              user = 'ams'
                              )
    self.assertNoMoreResources()

  def test_start_inmemory_aggregator(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metrics_monitor.py",
                       classname = "AmsMonitor",
                       command = "start",
                       config_file="default_ams_embedded.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.maxDiff=None
    self.assert_ams(inmemory_aggregation = True)

    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-monitor --config /etc/ambari-metrics-monitor/conf start',
                              user = 'ams'
                              )
    self.assertNoMoreResources()

  def assert_ams(self, inmemory_aggregation=False):
    self.assertResourceCalled('Directory', '/etc/ambari-metrics-monitor/conf',
                              owner = 'ams',
                              group = 'hadoop',
                              create_parents = True
                              )

    self.assertResourceCalled('Directory', '/var/log/ambari-metrics-monitor',
                              owner = 'ams',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True
                              )

    if inmemory_aggregation:
      self.assertResourceCalled('File', '/etc/ambari-metrics-monitor/conf/log4j.properties',
                                owner = 'ams',
                                group = 'hadoop',
                                content = InlineTemplate(self.getConfig()['configurations']['ams-log4j']['content']),
                                mode=0644,
                                )
      self.assertResourceCalled('XmlConfig', 'ams-site.xml',
                                owner = 'ams',
                                group = 'hadoop',
                                conf_dir = '/etc/ambari-metrics-monitor/conf',
                                configurations = self.getConfig()['configurations']['ams-site'],
                                configuration_attributes = self.getConfig()['configurationAttributes']['ams-site']
                                )

      self.assertResourceCalled('XmlConfig', 'ssl-server.xml',
                              owner = 'ams',
                              group = 'hadoop',
                              conf_dir = '/etc/ambari-metrics-monitor/conf',
                              configurations = self.getConfig()['configurations']['ams-ssl-server'],
                              configuration_attributes = self.getConfig()['configurationAttributes']['ams-ssl-server']
                              )
      pass

    self.assertResourceCalled('Execute', 'ambari-sudo.sh chown -R ams:hadoop /var/log/ambari-metrics-monitor')
    self.assertResourceCalled('Directory', '/var/run/ambari-metrics-monitor',
                              owner = 'ams',
                              group = 'hadoop',
                              mode = 0755,
                              cd_access = 'a',
                              create_parents = True
                              )
    self.assertResourceCalled('Directory', '/usr/lib/python2.6/site-packages/resource_monitoring/psutil/build',
                              owner = 'ams',
                              group = 'hadoop',
                              cd_access = 'a',
                              create_parents = True
                              )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh chown -R ams:hadoop /usr/lib/python2.6/site-packages/resource_monitoring')
    self.assertResourceCalled('TemplateConfig', '/etc/ambari-metrics-monitor/conf/metric_monitor.ini',
                              owner = 'ams',
                              group = 'hadoop',
                              template_tag = None,
                              )
    self.assertResourceCalled('TemplateConfig', '/etc/ambari-metrics-monitor/conf/metric_groups.conf',
                              owner = 'ams',
                              group = 'hadoop',
                              template_tag = None,
                              )
    self.assertResourceCalled('File', '/etc/ambari-metrics-monitor/conf/ams-env.sh',
                              owner = 'ams',
                              content = InlineTemplate(self.getConfig()['configurations']['ams-env']['content'])
                              )

