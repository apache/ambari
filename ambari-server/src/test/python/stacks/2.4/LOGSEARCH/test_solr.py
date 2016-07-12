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
from resource_management.libraries.script.config_dictionary import UnknownConfiguration

class TestSolr(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "LOGSEARCH/0.5.0/package"
  STACK_VERSION = "2.4"

  def configureResourcesCalled(self):
      self.assertResourceCalled('Directory', '/var/log/ambari-logsearch-solr',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                cd_access = 'a',
                                mode = 0755
      )
      self.assertResourceCalled('Directory', '/var/run/ambari-logsearch-solr',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                cd_access = 'a',
                                mode = 0755
      )
      self.assertResourceCalled('Directory', '/opt/logsearch_solr/data',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                cd_access = 'a',
                                mode = 0755
      )
      self.assertResourceCalled('Directory', '/opt/logsearch_solr/data/resources',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                cd_access = 'a',
                                mode = 0755
      )
      self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-solr',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                recursive_ownership = True,
                                cd_access = 'a',
                                mode = 0755
                                )
      self.assertResourceCalled('Directory', '/etc/ambari-logsearch-solr/conf',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                recursive_ownership = True,
                                cd_access = 'a',
                                mode = 0755
                                )
      
      self.assertResourceCalled('File', '/var/log/ambari-logsearch-solr/solr-install.log',
                                owner = 'solr',
                                group = 'hadoop',
                                mode = 0644,
                                content = ''
      )
      self.assertResourceCalled('File', '/etc/ambari-logsearch-solr/conf/logsearch-solr-env.sh',
                                owner = 'solr',
                                group='hadoop',
                                mode = 0755,
                                content = InlineTemplate(self.getConfig()['configurations']['logsearch-solr-env']['content'])
      )
      self.assertResourceCalled('File', '/opt/logsearch_solr/data/solr.xml',
                                owner = 'solr',
                                group='hadoop',
                                content = InlineTemplate(self.getConfig()['configurations']['logsearch-solr-xml']['content'])
      )
      self.assertResourceCalled('File', '/etc/ambari-logsearch-solr/conf/log4j.properties',
                                owner = 'solr',
                                group='hadoop',
                                content = InlineTemplate(self.getConfig()['configurations']['logsearch-solr-log4j']['content'])
      )
      self.assertResourceCalled('File', '/opt/logsearch_solr/data/zoo.cfg',
                                owner = 'solr',
                                group='hadoop',
                                content = Template('zoo.cfg.j2')
      )
      self.assertResourceCalled('Execute', 'export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /logsearch --create-znode --retry 5 --interval 10',
                                user = "solr")
      self.assertResourceCalled('Execute', 'export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/logsearch --cluster-prop --property-name urlScheme --property-value http',
                                user = "solr")
      self.assertResourceCalled('Execute', 'export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /logsearch --setup-kerberos-plugin',
                                user = "solr")

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/logsearch_solr.py",
                       classname = "LogsearchSolr",
                       command = "configure",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.configureResourcesCalled()
    self.assertNoMoreResources()
  
  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/logsearch_solr.py",
                       classname = "LogsearchSolr",
                       command = "start",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.configureResourcesCalled()
    self.assertResourceCalled('Execute', "/usr/lib/ambari-logsearch-solr/bin/solr start -cloud -noprompt -s /opt/logsearch_solr/data >> /var/log/ambari-logsearch-solr/solr-install.log 2>&1",
                              environment = {'SOLR_INCLUDE': '/etc/ambari-logsearch-solr/conf/logsearch-solr-env.sh'},
                              user = "solr"
    )
  
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/logsearch_solr.py",
                       classname = "LogsearchSolr",
                       command = "stop",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assertResourceCalled('Execute', '/usr/lib/ambari-logsearch-solr/bin/solr stop -all >> /var/log/ambari-logsearch-solr/solr-install.log',
                              environment = {'SOLR_INCLUDE': '/etc/ambari-logsearch-solr/conf/logsearch-solr-env.sh'},
                              user = "solr",
                              only_if = "test -f /var/run/ambari-logsearch-solr/solr-8886.pid"
    )
    self.assertResourceCalled('File', '/var/run/ambari-logsearch-solr/solr-8886.pid',
                              action = ['delete']
    )
