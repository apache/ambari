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
import os, sys

@patch("tempfile.mkdtemp", new = MagicMock(return_value='/some_tmp_dir'))
@patch("os.path.exists", new = MagicMock(return_value=True))
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestMetricsGrafana(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "AMBARI_METRICS/0.1.0/package/scripts"
  STACK_VERSION = "2.0.6"

  file_path = os.path.dirname(os.path.abspath(__file__))
  file_path = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(file_path)))))
  file_path = os.path.join(file_path, "main", "resources", "common-services", COMMON_SERVICES_PACKAGE_DIR)

  sys.path.append(file_path)
  global metrics_grafana_util

  @patch("metrics_grafana_util.create_ams_datasource")
  @patch("metrics_grafana_util.create_ams_dashboards")
  def test_start(self, create_ams_datasource_mock, create_ams_dashboards_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/metrics_grafana.py",
                       classname = "AmsGrafana",
                       command = "start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.maxDiff=None
    self.assert_configure()
    self.assertResourceCalled('Execute', ('chown', u'-R', u'ams', '/etc/ambari-metrics-grafana/conf'),
                              sudo = True
                              )
    self.assertResourceCalled('Execute', ('chown', u'-R', u'ams', '/var/log/ambari-metrics-grafana'),
                              sudo = True
                              )
    self.assertResourceCalled('Execute', ('chown', u'-R', u'ams', '/var/lib/ambari-metrics-grafana'),
                              sudo = True
                              )
    self.assertResourceCalled('Execute', ('chown', u'-R', u'ams', '/var/run/ambari-metrics-grafana'),
                              sudo = True
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh /usr/jdk64/jdk1.7.0_45/bin/keytool -importkeystore -srckeystore /etc/security/clientKeys/all.jks -destkeystore /some_tmp_dir/truststore.p12 -deststoretype PKCS12 -srcstorepass bigdata -deststorepass bigdata',
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh openssl pkcs12 -in /some_tmp_dir/truststore.p12 -out /etc/ambari-metrics-grafana/conf/ca.pem -cacerts -nokeys -passin pass:bigdata',
    )
    self.assertResourceCalled('Execute', ('chown', u'ams', '/etc/ambari-metrics-grafana/conf/ca.pem'),
                              sudo = True
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh rm -rf /some_tmp_dir',
                              )
    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-grafana start',
        not_if = "ambari-sudo.sh su ams -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]test -f /var/run/ambari-metrics-grafana/grafana-server.pid && ps -p `cat /var/run/ambari-metrics-grafana/grafana-server.pid`'",
        user = 'ams',
    )
    create_ams_datasource_mock.assertCalled()
    create_ams_dashboards_mock.assertCalled()
    self.assertNoMoreResources()

  def assert_configure(self):

    ams_grafana_directories = [
      '/etc/ambari-metrics-grafana/conf',
      '/var/log/ambari-metrics-grafana',
      '/var/lib/ambari-metrics-grafana',
      '/var/run/ambari-metrics-grafana'
    ]

    for ams_grafana_directory in ams_grafana_directories:
      self.assertResourceCalled('Directory', ams_grafana_directory,
                              owner = 'ams',
                              group = 'hadoop',
                              mode=0755,
                              recursive_ownership = True
                              )

    self.assertResourceCalled('File', '/etc/ambari-metrics-grafana/conf/ams-grafana-env.sh',
                              owner = 'ams',
                              group = 'hadoop',
                              content = InlineTemplate(self.getConfig()['configurations']['ams-env']['content'])
                              )

    self.assertResourceCalled('File', '/etc/ambari-metrics-grafana/conf/ams-grafana.ini',
                              owner = 'ams',
                              group = 'hadoop',
                              content = InlineTemplate(self.getConfig()['configurations']['ams-env']['content']),
                              mode = 0600
                              )
