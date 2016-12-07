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

class TestInfraSolr(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "AMBARI_INFRA/0.1.0/package"
  STACK_VERSION = "2.4"

  def configureResourcesCalled(self):
      self.assertResourceCalled('Directory', '/var/log/ambari-infra-solr',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                cd_access = 'a',
                                mode = 0755
      )
      self.assertResourceCalled('Directory', '/var/run/ambari-infra-solr',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                cd_access = 'a',
                                mode = 0755
      )
      self.assertResourceCalled('Directory', '/opt/ambari_infra_solr/data',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                cd_access = 'a',
                                mode = 0755
      )
      self.assertResourceCalled('Directory', '/opt/ambari_infra_solr/data/resources',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                cd_access = 'a',
                                mode = 0755
      )
      self.assertResourceCalled('Directory', '/usr/lib/ambari-infra-solr',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                recursive_ownership = True,
                                cd_access = 'a',
                                mode = 0755
                                )
      self.assertResourceCalled('Directory', '/etc/ambari-infra-solr/conf',
                                owner = 'solr',
                                group = 'hadoop',
                                create_parents = True,
                                recursive_ownership = True,
                                cd_access = 'a',
                                mode = 0755
                                )
      
      self.assertResourceCalled('File', '/var/log/ambari-infra-solr/solr-install.log',
                                owner = 'solr',
                                group = 'hadoop',
                                mode = 0644,
                                content = ''
      )
      self.assertResourceCalled('File', '/etc/ambari-infra-solr/conf/infra-solr-env.sh',
                                owner = 'solr',
                                group='hadoop',
                                mode = 0755,
                                content = InlineTemplate(self.getConfig()['configurations']['infra-solr-env']['content'])
      )
      self.assertResourceCalled('File', '/opt/ambari_infra_solr/data/solr.xml',
                                owner = 'solr',
                                group='hadoop',
                                content = InlineTemplate(self.getConfig()['configurations']['infra-solr-xml']['content'])
      )
      self.assertResourceCalled('File', '/etc/ambari-infra-solr/conf/log4j.properties',
                                owner = 'solr',
                                group='hadoop',
                                content = InlineTemplate(self.getConfig()['configurations']['infra-solr-log4j']['content'])
      )

      self.assertResourceCalled('Execute', 'ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr --create-znode --retry 30 --interval 5')
      self.assertResourceCalled('Execute', 'ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --cluster-prop --property-name urlScheme --property-value http')
      self.assertResourceCalled('Execute', 'ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr --setup-kerberos-plugin')

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/infra_solr.py",
                       classname = "InfraSolr",
                       command = "configure",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.configureResourcesCalled()
    self.assertNoMoreResources()
  
  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/infra_solr.py",
                       classname = "InfraSolr",
                       command = "start",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.configureResourcesCalled()
    self.assertResourceCalled('Execute', "/usr/lib/ambari-infra-solr/bin/solr start -cloud -noprompt -s /opt/ambari_infra_solr/data >> /var/log/ambari-infra-solr/solr-install.log 2>&1",
                              environment = {'SOLR_INCLUDE': '/etc/ambari-infra-solr/conf/infra-solr-env.sh'},
                              user = "solr"
    )
  
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/infra_solr.py",
                       classname = "InfraSolr",
                       command = "stop",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assertResourceCalled('Execute', '/usr/lib/ambari-infra-solr/bin/solr stop -all >> /var/log/ambari-infra-solr/solr-install.log',
                              environment = {'SOLR_INCLUDE': '/etc/ambari-infra-solr/conf/infra-solr-env.sh'},
                              user = "solr",
                              only_if = "test -f /var/run/ambari-infra-solr/solr-8886.pid"
    )
    self.assertResourceCalled('File', '/var/run/ambari-infra-solr/solr-8886.pid',
                              action = ['delete']
    )
