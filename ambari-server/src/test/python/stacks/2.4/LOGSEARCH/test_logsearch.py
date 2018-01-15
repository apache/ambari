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

from stacks.utils.RMFTestCase import RMFTestCase, Template, InlineTemplate

class TestLogSearch(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "LOGSEARCH/0.5.0/package"
  STACK_VERSION = "2.4"

  def configureResourcesCalled(self):
    self.assertResourceCalled('Directory', '/var/log/ambari-logsearch-portal',
                              owner = 'logsearch',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/var/run/ambari-logsearch-portal',
                              owner = 'logsearch',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-portal',
                              owner = 'logsearch',
                              group = 'hadoop',
                              create_parents = True,
                              recursive_ownership = True,
                              cd_access = 'a',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-portal/conf',
                              owner = 'logsearch',
                              group = 'hadoop',
                              create_parents = True,
                              recursive_ownership = True,
                              cd_access = 'a',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-portal/conf/solr_configsets',
                              owner = 'logsearch',
                              group = 'hadoop',
                              create_parents = True,
                              recursive_ownership = True,
                              cd_access = 'a',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-portal/conf/keys',
                              owner = 'logsearch',
                              group = 'hadoop',
                              cd_access = 'a',
                              mode = 0755
                              )

    self.assertResourceCalled('File', '/var/log/ambari-logsearch-portal/logsearch.out',
                              owner = 'logsearch',
                              group = 'hadoop',
                              mode = 0644,
                              content = ''
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-portal/conf/keys/ks_pass.txt',
                              action = ['delete']
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-portal/conf/keys/ts_pass.txt',
                              action = ['delete']
    )
    self.assertResourceCalled('PropertiesFile', '/usr/lib/ambari-logsearch-portal/conf/logsearch.properties',
                              properties = {'logsearch.login.credentials.file': 'logsearch-admin.json', u'logsearch.https.port': u'61888', 'logsearch.auth.file.enabled': 'true', u'logsearch.http.port': u'61888', 'logsearch.config.zk_connect_string': u'c6401.ambari.apache.org:2181', 'logsearch.solr.collection.history': 'history', 'logsearch.auth.ldap.enabled': 'false', u'logsearch.service.logs.split.interval.mins': u'1', u'logsearch.solr.metrics.collector.hosts': '', u'logsearch.solr.collection.audit.logs': u'audit_logs', u'common-property': u'common-value', u'logsearch.auth.external_auth.host_url': u'http://c6401.ambari.apache.org:8080', u'logsearch.audit.logs.split.interval.mins': u'1', 'logsearch.auth.simple.enabled': 'false', u'logsearch.auth.external_auth.login_url': u'/api/v1/users/$USERNAME/privileges?fields=*', 'logsearch.solr.audit.logs.zk_connect_string': u'c6401.ambari.apache.org:2181/infra-solr', u'logsearch.protocol': u'http', u'logsearch.auth.external_auth.enabled': u'false', u'logsearch.collection.audit.logs.replication.factor': u'1', u'logsearch.spnego.kerberos.host': u'localhost', 'logsearch.solr.jmx.port': u'1', u'logsearch.collection.service.logs.replication.factor': u'1', u'logsearch.solr.collection.service.logs': u'hadoop_logs', 'logsearch.solr.history.config.name': 'history', 'logsearch.solr.zk_connect_string': u'c6401.ambari.apache.org:2181/infra-solr', 'logsearch.collection.history.replication.factor': '1', 'hadoop.security.credential.provider.path': 'jceks://file/usr/lib/ambari-logsearch-portal/conf/logsearch.jceks', u'logsearch.roles.allowed': u'AMBARI.ADMINISTRATOR,CLUSTER.ADMINISTRATOR', u'logsearch.collection.service.logs.numshards': u'10', u'logsearch.collection.audit.logs.numshards': u'10'}
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-portal/conf/HadoopServiceConfig.json',
                              owner = 'logsearch',
                              group='hadoop',
                              content = Template('HadoopServiceConfig.json.j2')
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-portal/conf/log4j.xml',
                              owner = 'logsearch',
                              group='hadoop',
                              content = InlineTemplate(self.getConfig()['configurations']['logsearch-log4j']['content'])
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-portal/conf/logsearch-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['logsearch-env']['content']),
                              mode = 0755,
                              owner = "logsearch",
                              group='hadoop'
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-portal/conf/logsearch-admin.json',
                              owner = 'logsearch',
                              group='hadoop',
                              content = InlineTemplate(self.getConfig()['configurations']['logsearch-admin-json']['content'])
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-portal/conf/solr_configsets/hadoop_logs/conf/solrconfig.xml',
                              owner = 'logsearch',
                              group='hadoop',
                              content = InlineTemplate(self.getConfig()['configurations']['logsearch-service_logs-solrconfig']['content'])
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-portal/conf/solr_configsets/audit_logs/conf/solrconfig.xml',
                              owner = 'logsearch',
                              group='hadoop',
                              content = InlineTemplate(self.getConfig()['configurations']['logsearch-audit_logs-solrconfig']['content'])
                              )
    self.assertResourceCalled('Execute', ('chmod', '-R', 'ugo+r', '/usr/lib/ambari-logsearch-portal/conf/solr_configsets'),
                              sudo = True
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr --check-znode --retry 30 --interval 5')



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
    self.assertResourceCalled('Execute', "/usr/lib/ambari-logsearch-portal/bin/logsearch.sh start",
                              user = "logsearch"
    )