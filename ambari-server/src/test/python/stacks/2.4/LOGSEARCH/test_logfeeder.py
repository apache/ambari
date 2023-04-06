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

import grp
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import RMFTestCase, Template, InlineTemplate
from resource_management.libraries.functions.default import default

class TestLogFeeder(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "LOGSEARCH/0.5.0/package"
  STACK_VERSION = "2.4"

  def configureResourcesCalled(self):
    self.assertResourceCalled('Directory', '/var/log/ambari-logsearch-logfeeder',
                              create_parents=True,
                              cd_access='a',
                              mode=0o755
                              )
    self.assertResourceCalled('Directory', '/var/run/ambari-logsearch-logfeeder',
                              create_parents=True,
                              cd_access='a',
                              mode=0o755
                              )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-logfeeder/conf/checkpoints',
                              create_parents=True,
                              cd_access='a',
                              mode=0o755
                              )

    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-logfeeder',
                              create_parents=True,
                              recursive_ownership=True,
                              cd_access='a',
                              mode=0o755
                              )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-logfeeder/conf',
                              create_parents=True,
                              recursive_ownership=True,
                              cd_access='a',
                              mode=0o755
                              )

    self.assertResourceCalled('File', '/var/log/ambari-logsearch-logfeeder/logfeeder.out',
                              mode=0o644,
                              content=''
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-logfeeder/conf/keys/ks_pass.txt',
                              action = ['delete']
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-logfeeder/conf/keys/ts_pass.txt',
                              action = ['delete']
                              )
    self.assertResourceCalled('PropertiesFile', '/usr/lib/ambari-logsearch-logfeeder/conf/logfeeder.properties',
                              properties={'cluster.name': 'c1',
                                          'common-property': 'common-value',
                                          'hadoop.security.credential.provider.path': 'jceks://file/usr/lib/ambari-logsearch-logfeeder/conf/logfeeder.jceks',
                                          'logfeeder.checkpoint.folder': '/usr/lib/ambari-logsearch-logfeeder/conf/checkpoints',
                                          'logfeeder.config.dir': '/usr/lib/ambari-logsearch-logfeeder/conf',
                                          'logfeeder.config.files': 'output.config.json,global.config.json',
                                          'logfeeder.metrics.collector.hosts': '',
                                          'logfeeder.metrics.collector.path': '/ws/v1/timeline/metrics',
                                          'logfeeder.metrics.collector.port': '',
                                          'logfeeder.metrics.collector.protocol': '',
                                          'logfeeder.solr.core.config.name': 'history',
                                          'logfeeder.solr.zk_connect_string': 'c6401.ambari.apache.org:2181/infra-solr',
                                          'logsearch.config.zk_connect_string': 'c6401.ambari.apache.org:2181'
                                         }
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-logfeeder/conf/logfeeder-env.sh',
                              mode=0o755,
                              content=InlineTemplate(self.getConfig()['configurations']['logfeeder-env']['content'])
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-logfeeder/conf/log4j.xml',
                              content=InlineTemplate(self.getConfig()['configurations']['logfeeder-log4j']['content'])
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-logfeeder/conf/grok-patterns',
                              content=InlineTemplate('GP'),
                              encoding='utf-8'
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-logfeeder/conf/global.config.json',
                              content=Template('global.config.json.j2')
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-logfeeder/conf/input.config-ambari.json',
                              content=InlineTemplate('ambari-grok-filter'),
                              encoding='utf-8'
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-logfeeder/conf/output.config.json',
                              content=InlineTemplate('output-grok-filter'),
                              encoding='utf-8'
                              )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-logfeeder/conf',
                              create_parents = True,
                              cd_access = 'a',
                              mode = 0o755
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-logfeeder/conf/input.config-logsearch.json',
                            mode=0o644,
                            content = Template('input.config-logsearch.json.j2', extra_imports=[default])
                            )

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/logfeeder.py",
                       classname="LogFeeder",
                       command="configure",
                       config_file="default.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )

    self.configureResourcesCalled()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/logfeeder.py",
                       classname="LogFeeder",
                       command="start",
                       config_file="default.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )

    self.configureResourcesCalled()
    self.assertResourceCalled('Execute', ('/usr/lib/ambari-logsearch-logfeeder/bin/logfeeder.sh', "start"),
                              sudo=True
                              )
