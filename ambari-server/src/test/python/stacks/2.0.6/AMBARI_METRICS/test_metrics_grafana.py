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

@patch("os.path.exists", new = MagicMock(return_value=True))
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestMetricsGrafana(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "AMBARI_METRICS/0.1.0/package"
  STACK_VERSION = "2.0.6"

  def test_start(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metrics_grafana.py",
                       classname = "AmsGrafana",
                       command = "start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.maxDiff=None
    self.assert_configure()
    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-grafana stop',
                              user = 'ams'
                              )
    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-grafana start',
                              user = 'ams'
                              )
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
                              content = InlineTemplate(self.getConfig()['configurations']['ams-env']['content'])
                              )
