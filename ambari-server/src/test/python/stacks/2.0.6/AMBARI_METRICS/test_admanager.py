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
class TestADManager(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "AMBARI_METRICS/0.1.0/package/scripts"
  STACK_VERSION = "2.0.6"

  file_path = os.path.dirname(os.path.abspath(__file__))
  file_path = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(file_path)))))
  file_path = os.path.join(file_path, "main", "resources", "common-services", COMMON_SERVICES_PACKAGE_DIR)

  sys.path.append(file_path)
  def test_start(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/ams_admanager.py",
                       classname = "AmsADManager",
                       command = "start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.maxDiff=None
    self.assert_configure()
    self.assertResourceCalled('Execute', ('chown', u'-R', u'ams', '/etc/ambari-metrics-anomaly-detection/conf'),
                              sudo = True
                              )
    self.assertResourceCalled('Execute', ('chown', u'-R', u'ams', '/var/log/ambari-metrics-anomaly-detection'),
                              sudo = True
                              )
    self.assertResourceCalled('Execute', ('chown', u'-R', u'ams', '/var/lib/ambari-metrics-anomaly-detection'),
                              sudo = True
                              )
    self.assertResourceCalled('Execute', ('chown', u'-R', u'ams', '/var/run/ambari-metrics-anomaly-detection'),
                              sudo = True
                              )
    self.assertResourceCalled('Execute', '/usr/sbin/ambari-metrics-admanager start',
                              user = 'ams'
                              )
    self.assertNoMoreResources()

  def assert_configure(self):

    ams_admanager_directories = [
      '/etc/ambari-metrics-anomaly-detection/conf',
      '/var/log/ambari-metrics-anomaly-detection',
      '/var/lib/ambari-metrics-anomaly-detection',
      '/var/run/ambari-metrics-anomaly-detection'
    ]

    for ams_admanager_directory in ams_admanager_directories:
      self.assertResourceCalled('Directory', ams_admanager_directory,
                                owner = 'ams',
                                group = 'hadoop',
                                mode=0755,
                                create_parents = True,
                                recursive_ownership = True
                                )

    self.assertResourceCalled('File', '/etc/ambari-metrics-anomaly-detection/conf/ams-admanager-env.sh',
                              owner = 'ams',
                              group = 'hadoop',
                              content = InlineTemplate(self.getConfig()['configurations']['ams-admanager-env']['content'])
                              )

    self.assertResourceCalled('File', '/etc/ambari-metrics-anomaly-detection/conf/config.yaml',
                              owner = 'ams',
                              group = 'hadoop',
                              content = InlineTemplate(self.getConfig()['configurations']['ams-admanager-config']['content']),
                              )

    merged_ams_hbase_site = {}
    merged_ams_hbase_site.update(self.getConfig()['configurations']['ams-hbase-site'])

    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
                              owner = 'ams',
                              conf_dir = '/etc/ambari-metrics-anomaly-detection/conf',
                              configurations = merged_ams_hbase_site,
                              configuration_attributes = self.getConfig()['configuration_attributes']['ams-hbase-site']
                              )
    self.assertResourceCalled('File', '/etc/ambari-metrics-anomaly-detection/conf/log4j.properties',
                              owner = 'ams',
                              content = ''
                              )
