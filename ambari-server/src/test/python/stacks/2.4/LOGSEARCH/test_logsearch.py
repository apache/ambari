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
from mock.mock import MagicMock, patch
from resource_management.libraries.script.config_dictionary import UnknownConfiguration

class TestLogSearch(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "LOGSEARCH/0.5.0/package"
  STACK_VERSION = "2.4"

  def configureResourcesCalled(self):
    self.assertResourceCalled('Directory', '/var/log/ambari-logsearch-portal',
                              owner = 'logsearch',
                              group = 'logsearch',
                              create_parents = True,
                              cd_access = 'a',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/var/run/ambari-logsearch-portal',
                              owner = 'logsearch',
                              group = 'logsearch',
                              create_parents = True,
                              cd_access = 'a',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-portal',
                              owner = 'logsearch',
                              group = 'logsearch',
                              create_parents = True,
                              cd_access = 'a',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/etc/ambari-logsearch-portal/conf',
                              owner = 'logsearch',
                              group = 'logsearch',
                              create_parents = True,
                              cd_access = 'a',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/etc/ambari-logsearch-portal/conf/solr_configsets',
                              owner = 'logsearch',
                              group = 'logsearch',
                              create_parents = True,
                              cd_access = 'a',
                              mode = 0755
                              )

    self.assertResourceCalled('File', '/var/log/ambari-logsearch-portal/logsearch.out',
                              owner = 'logsearch',
                              group = 'logsearch',
                              mode = 0644,
                              content = ''
    )
    self.assertResourceCalled('File', '/etc/ambari-logsearch-portal/conf/logsearch.properties',
                              owner = 'logsearch',
                              content = Template('logsearch.properties.j2')
    )
    self.assertResourceCalled('File', '/etc/ambari-logsearch-portal/conf/log4j.xml',
                              owner = 'logsearch',
                              content = InlineTemplate(self.getConfig()['configurations']['logsearch-log4j']['content'])
    )
    self.assertResourceCalled('File', '/etc/ambari-logsearch-portal/conf/logsearch-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['logsearch-env']['content']),
                              mode = 0755,
                              owner = "logsearch"
    )
    self.assertResourceCalled('File', '/etc/ambari-logsearch-portal/conf/logsearch-admin.json',
                              owner = 'logsearch',
                              content = InlineTemplate(self.getConfig()['configurations']['logsearch-admin-json']['content'])
                              )
    self.assertResourceCalled('File', '/etc/ambari-logsearch-portal/conf/solr_configsets/hadoop_logs/conf/solrconfig.xml',
                              owner = 'logsearch',
                              content = InlineTemplate(self.getConfig()['configurations']['logsearch-service_logs-solrconfig']['content'])
    )
    self.assertResourceCalled('File', '/etc/ambari-logsearch-portal/conf/solr_configsets/audit_logs/conf/solrconfig.xml',
                              owner = 'logsearch',
                              content = InlineTemplate(self.getConfig()['configurations']['logsearch-audit_logs-solrconfig']['content'])
                              )

    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org:2181/logsearch --download-config -d /tmp/solr_config_hadoop_logs_0.[0-9]* -cs hadoop_logs -rt 5 -i 10')
    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org:2181/logsearch --upload-config -d /etc/ambari-logsearch-portal/conf/solr_configsets/hadoop_logs/conf -cs hadoop_logs -rt 5 -i 10')

    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org:2181/logsearch --download-config -d /tmp/solr_config_history_0.[0-9]* -cs history -rt 5 -i 10')
    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org:2181/logsearch --upload-config -d /etc/ambari-logsearch-portal/conf/solr_configsets/history/conf -cs history -rt 5 -i 10')

    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org:2181/logsearch --download-config -d /tmp/solr_config_audit_logs_0.[0-9]* -cs audit_logs -rt 5 -i 10')
    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org:2181/logsearch --upload-config -d /etc/ambari-logsearch-portal/conf/solr_configsets/audit_logs/conf -cs audit_logs -rt 5 -i 10')

    self.assertResourceCalled('Execute', ('chmod', '-R', 'ugo+r', '/etc/ambari-logsearch-portal/conf/solr_configsets'),
                              sudo=True
    )

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/logsearch.py",
                       classname = "LogSearch",
                       command = "configure",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.configureResourcesCalled()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/logsearch.py",
                       classname = "LogSearch",
                       command = "start",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.configureResourcesCalled()
    self.assertResourceCalled('Execute', "/usr/lib/ambari-logsearch-portal/run.sh",
                              environment = {'LOGSEARCH_INCLUDE': '/etc/ambari-logsearch-portal/conf/logsearch-env.sh'},
                              user = "logsearch"
    )
