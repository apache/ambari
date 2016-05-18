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

from stacks.utils.RMFTestCase import RMFTestCase, Template, InlineTemplate, StaticFile
from resource_management.core.exceptions import ComponentIsNotRunning


class TestLogFeeder(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "LOGSEARCH/0.5.0/package"
  STACK_VERSION = "2.4"

  def configureResourcesCalled(self):
    self.assertResourceCalled('Directory', '/var/log/ambari-logsearch-logfeeder',
                              owner='logfeeder',
                              group='logfeeder',
                              create_parents=True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('Directory', '/var/run/ambari-logsearch-logfeeder',
                              owner='logfeeder',
                              group='logfeeder',
                              create_parents=True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-logfeeder',
                              owner='logfeeder',
                              group='logfeeder',
                              create_parents=True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('Directory', '/etc/ambari-logsearch-logfeeder/conf',
                              owner='logfeeder',
                              group='logfeeder',
                              create_parents=True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('Directory', '/etc/ambari-logsearch-logfeeder/conf/checkpoints',
                              owner='logfeeder',
                              group='logfeeder',
                              create_parents=True,
                              cd_access='a',
                              mode=0755
                              )

    self.assertResourceCalled('File', '/var/log/ambari-logsearch-logfeeder/logfeeder.out',
                              owner='logfeeder',
                              group='logfeeder',
                              mode=0644,
                              content=''
                              )
    self.assertResourceCalled('File', '/etc/ambari-logsearch-logfeeder/conf/logfeeder.properties',
                              owner='logfeeder',
                              content=Template('logfeeder.properties.j2')
                              )
    self.assertResourceCalled('File', '/etc/ambari-logsearch-logfeeder/conf/logfeeder-env.sh',
                              owner='logfeeder',
                              content=InlineTemplate(self.getConfig()['configurations']['logfeeder-env']['content'])
                              )
    self.assertResourceCalled('File', '/etc/ambari-logsearch-logfeeder/conf/log4j.xml',
                              owner='logfeeder',
                              content=InlineTemplate(self.getConfig()['configurations']['logfeeder-log4j']['content'])
                              )
    self.assertResourceCalled('File', '/etc/ambari-logsearch-logfeeder/conf/grok-patterns',
                              owner='logfeeder',
                              content=Template('grok-patterns.j2'),
                              encoding='utf-8'
                              )

    logfeeder_supported_services = ['accumulo', 'ambari', 'ams', 'atlas', 'falcon', 'hbase', 'hdfs', 'hive', 'kafka',
                                    'knox', 'logsearch', 'oozie', 'ranger', 'storm', 'yarn', 'zookeeper']

    logfeeder_config_file_names = ['global.config.json', 'output.config.json'] + ['input.config-%s.json' % (tag) for tag
                                                                                  in logfeeder_supported_services]

    for file_name in logfeeder_config_file_names:
      self.assertResourceCalled('File', '/etc/ambari-logsearch-logfeeder/conf/' + file_name,
                                owner='logfeeder',
                                content=Template(file_name + ".j2")
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
    self.assertResourceCalled('Execute', '/usr/lib/ambari-logsearch-logfeeder/run.sh',
                              environment={
                                'LOGFEEDER_INCLUDE': '/etc/ambari-logsearch-logfeeder/conf/logfeeder-env.sh'},
                              user='logfeeder'
                              )
