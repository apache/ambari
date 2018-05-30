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
from resource_management.libraries.functions.default import default

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
                              properties = {'common-property': 'common-value',
                                            'hadoop.security.credential.provider.path': 'jceks://file/usr/lib/ambari-logsearch-portal/conf/logsearch.jceks',
                                            'logsearch.audit.logs.split.interval.mins': '1',
                                            'logsearch.auth.external_auth.enabled': 'false',
                                            'logsearch.auth.external_auth.host_url': 'http://c6401.ambari.apache.org:8080',
                                            'logsearch.auth.external_auth.login_url': '/api/v1/users/$USERNAME/privileges?fields=*',
                                            'logsearch.auth.file.enabled': 'true',
                                            'logsearch.auth.ldap.enabled': 'false',
                                            'logsearch.auth.simple.enabled': 'false',
                                            'logsearch.collection.audit.logs.numshards': '10',
                                            'logsearch.collection.audit.logs.replication.factor': '1',
                                            'logsearch.collection.history.replication.factor': '2',
                                            'logsearch.collection.service.logs.numshards': '10',
                                            'logsearch.collection.service.logs.replication.factor': '1',
                                            'logsearch.config.zk_connect_string': 'c6401.ambari.apache.org:2181',
                                            'logsearch.login.credentials.file': 'logsearch-admin.json',
                                            'logsearch.protocol': 'http',
                                            'logsearch.http.port': '61888',
                                            'logsearch.https.port': '61888',
                                            'logsearch.roles.allowed': 'AMBARI.ADMINISTRATOR,CLUSTER.ADMINISTRATOR',
                                            'logsearch.service.logs.split.interval.mins': '1',
                                            'logsearch.solr.audit.logs.zk_connect_string': 'c6401.ambari.apache.org:2181/infra-solr',
                                            'logsearch.solr.collection.audit.logs': 'audit_logs',
                                            'logsearch.solr.collection.history': 'history',
                                            'logsearch.solr.collection.service.logs': 'hadoop_logs',
                                            'logsearch.solr.history.config.name': 'history',
                                            'logsearch.solr.metrics.collector.hosts': '',
                                            'logsearch.solr.jmx.port': '1',
                                            'logsearch.solr.zk_connect_string': 'c6401.ambari.apache.org:2181/infra-solr',
                                            'logsearch.spnego.kerberos.host': 'localhost'
                              }
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

    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-logfeeder/conf',
                              create_parents = True,
                              cd_access = 'a',
                              mode = 0755
                              )

    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-logfeeder/conf/input.config-logsearch.json',
                              mode=0644,
                              content = Template('input.config-logsearch.json.j2', extra_imports=[default])
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