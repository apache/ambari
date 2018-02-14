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
import json
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *
from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestRangerAdmin(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "RANGER/0.4.0/package"
  STACK_VERSION = "2.6"

  @patch("os.path.isfile")    
  def test_start_default(self, isfile_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
      classname = "RangerAdmin",
      command = "start",
      config_file="ranger-admin-default.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()

    self.assertResourceCalled('Directory', '/var/log/ambari-infra-solr-client',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-infra-solr-client',
        cd_access = 'a',
        create_parents = True,
        mode = 0755,
        recursive_ownership = True,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-infra-solr-client/solrCloudCli.sh',
        content = StaticFile('/usr/lib/ambari-infra-solr-client/solrCloudCli.sh'),
        mode = 0755,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-infra-solr-client/log4j.properties',
        content = self.getConfig()['configurations']['infra-solr-client-log4j']['content'],
        mode = 0644,
    )
    self.assertResourceCalled('File', '/var/log/ambari-infra-solr-client/solr-client.log',
        content = '',
        mode = 0664,
    )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr --check-znode --retry 5 --interval 10',)

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/contrib/solr_for_audit_setup/conf/solrconfig.xml',
      owner = u'ranger',
      content = InlineTemplate(self.getConfig()['configurations']['ranger-solr-configuration']['content']),
      group = u'ranger',
      mode = 0644,
    )

    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --download-config --config-dir /tmp/solr_config_ranger_audits_0.[0-9]* --config-set ranger_audits --retry 30 --interval 5',only_if = 'ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --check-config --config-set ranger_audits --retry 30 --interval 5',)

    self.assertResourceCalledRegexp('^File$', '^/tmp/solr_config_ranger_audits_0.[0-9]*/solrconfig.xml',content = InlineTemplate(self.getConfig()['configurations']['ranger-solr-configuration']['content']),
                              only_if = 'test -d /tmp/solr_config_ranger_audits_0.[0-9]*',)

    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --upload-config --config-dir /tmp/solr_config_ranger_audits_0.[0-9]* --config-set ranger_audits --retry 30 --interval 5',
                                    only_if = 'test -d /tmp/solr_config_ranger_audits_0.[0-9]*',)

    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --upload-config --config-dir /usr/hdp/current/ranger-admin/contrib/solr_for_audit_setup/conf --config-set ranger_audits --retry 30 --interval 5')
    self.assertResourceCalledRegexp('^Directory$', '^/tmp/solr_config_ranger_audits_0.[0-9]*',
                                    action=['delete'],
                                    create_parents=True)
    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --create-collection --collection ranger_audits --config-set ranger_audits --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
      owner = 'ranger',
      properties = {'db_password': '_', 'db_root_password': '_'}
    )

    self.assertResourceCalled('Execute', '/usr/bin/ranger-admin-start',
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      not_if = 'ps -ef | grep proc_rangeradmin | grep -v grep',
      user = 'ranger',
    )

    self.assertTrue(isfile_mock.called)
    self.assertNoMoreResources()

  @patch("os.path.isfile")
  def test_start_secured(self, isfile_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/ranger_admin.py",
      classname = "RangerAdmin",
      command = "start",
      config_file="ranger-admin-secured.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()

    self.assertResourceCalled('Directory', '/var/log/ambari-infra-solr-client',
        create_parents = True,
        mode = 0755,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-infra-solr-client',
        cd_access = 'a',
        create_parents = True,
        mode = 0755,
        recursive_ownership = True,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-infra-solr-client/solrCloudCli.sh',
        content = StaticFile('/usr/lib/ambari-infra-solr-client/solrCloudCli.sh'),
        mode = 0755,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-infra-solr-client/log4j.properties',
        content = self.getConfig()['configurations']['infra-solr-client-log4j']['content'],
        mode = 0644,
    )
    self.assertResourceCalled('File', '/var/log/ambari-infra-solr-client/solr-client.log',
        content = '',
        mode = 0664,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/conf/ranger_solr_jaas.conf',
      content = Template('ranger_solr_jaas_conf.j2'),
      owner = 'ranger',
    )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr --check-znode --retry 5 --interval 10',)

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/contrib/solr_for_audit_setup/conf/solrconfig.xml',
                              owner = u'ranger',
                              content = InlineTemplate(self.getConfig()['configurations']['ranger-solr-configuration']['content']),
                              group = u'ranger',
                              mode = 0644,
                              )

    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --download-config --config-dir /tmp/solr_config_ranger_audits_0.[0-9]* --config-set ranger_audits --retry 30 --interval 5',only_if = 'ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --check-config --config-set ranger_audits --retry 30 --interval 5',)

    self.assertResourceCalledRegexp('^File$', '^/tmp/solr_config_ranger_audits_0.[0-9]*/solrconfig.xml',content = InlineTemplate(self.getConfig()['configurations']['ranger-solr-configuration']['content']),
                                    only_if = 'test -d /tmp/solr_config_ranger_audits_0.[0-9]*',)

    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --upload-config --config-dir /tmp/solr_config_ranger_audits_0.[0-9]* --config-set ranger_audits --retry 30 --interval 5',
                                    only_if = 'test -d /tmp/solr_config_ranger_audits_0.[0-9]*',)


    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --upload-config --config-dir /usr/hdp/current/ranger-admin/contrib/solr_for_audit_setup/conf --config-set ranger_audits --retry 30 --interval 5')
    self.assertResourceCalledRegexp('^Directory$', '^/tmp/solr_config_ranger_audits_0.[0-9]*',
                                    action=['delete'],
                                    create_parents=True)
    self.assertResourceCalled('Execute', "/usr/bin/kinit -kt /etc/security/keytabs/infra-solr.service.keytab infra-solr/c6401.ambari.apache.org@EXAMPLE.COM; curl -k -s --negotiate -u : http://c6401.ambari.apache.org:8886/solr/admin/authorization | grep authorization.enabled && /usr/bin/kinit -kt /etc/security/keytabs/infra-solr.service.keytab infra-solr/c6401.ambari.apache.org@EXAMPLE.COM; curl -H 'Content-type:application/json' -d '{\"set-user-role\": {\"rangeradmin@EXAMPLE.COM\": [\"ranger_user\", \"ranger_audit_user\", \"dev\"]}}' -s -o /dev/null -w'%{http_code}' --negotiate -u: -k http://c6401.ambari.apache.org:8886/solr/admin/authorization | grep 200",
                              logoutput = True, tries = 30, try_sleep = 10, user='infra-solr')
    self.assertResourceCalled('Execute', "/usr/bin/kinit -kt /etc/security/keytabs/infra-solr.service.keytab infra-solr/c6401.ambari.apache.org@EXAMPLE.COM; curl -k -s --negotiate -u : http://c6401.ambari.apache.org:8886/solr/admin/authorization | grep authorization.enabled && /usr/bin/kinit -kt /etc/security/keytabs/infra-solr.service.keytab infra-solr/c6401.ambari.apache.org@EXAMPLE.COM; curl -H \'Content-type:application/json\' -d "
                                         "\'{\"set-user-role\": {\"hbase@EXAMPLE.COM\": [\"ranger_audit_user\", \"dev\"], \"nn@EXAMPLE.COM\": [\"ranger_audit_user\", \"dev\"], \"knox@EXAMPLE.COM\": [\"ranger_audit_user\", \"dev\"], \"rangerkms@EXAMPLE.COM\": [\"ranger_audit_user\", \"dev\"], \"kafka@EXAMPLE.COM\": [\"ranger_audit_user\", \"dev\"], \"hive@EXAMPLE.COM\": [\"ranger_audit_user\", \"dev\"], \"nifi@EXAMPLE.COM\": [\"ranger_audit_user\", \"dev\"], \"storm@EXAMPLE.COM\": [\"ranger_audit_user\", \"dev\"], \"yarn@EXAMPLE.COM\": [\"ranger_audit_user\", \"dev\"]}}\' -s -o /dev/null -w\'%{http_code}\' --negotiate -u: -k http://c6401.ambari.apache.org:8886/solr/admin/authorization | grep 200",
                              logoutput = True, tries = 30, try_sleep = 10, user='infra-solr')

    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --create-collection --collection ranger_audits --config-set ranger_audits --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')

    self.assertResourceCalled('Execute','ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr/configs/ranger_audits --secure-znode --jaas-file /usr/hdp/current/ranger-admin/conf/ranger_solr_jaas.conf --sasl-users rangeradmin,infra-solr --retry 5 --interval 10')
    self.assertResourceCalled('Execute', 'ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr/collections/ranger_audits --secure-znode --jaas-file /usr/hdp/current/ranger-admin/conf/ranger_solr_jaas.conf --sasl-users rangeradmin,infra-solr --retry 5 --interval 10')

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
      owner = 'ranger',
      properties = {'db_password': '_', 'db_root_password': '_'}
    )

    self.assertResourceCalled('Execute', '/usr/bin/ranger-admin-start',
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      not_if = 'ps -ef | grep proc_rangeradmin | grep -v grep',
      user = 'ranger',
    )

    self.assertTrue(isfile_mock.called)
    self.assertNoMoreResources()


  def assert_setup_db(self):
    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java-old.jar',
                              action = ['delete'],
                              )

    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
                              content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar'),
                              mode = 0644
                              )

    self.assertResourceCalled('Execute', ('cp', '--remove-destination', '/tmp/mysql-connector-java.jar',
                                          '/usr/hdp/current/ranger-admin/ews/lib'),
                              sudo = True,
                              path = ['/bin', '/usr/bin/']
                              )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java.jar',
                              mode = 0644
                              )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
                              properties = self.getConfig()['configurations']['admin-properties'],
                              owner = 'ranger'
                              )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
                              owner = 'ranger',
                              properties = {'SQL_CONNECTOR_JAR':
                                              '/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java.jar'}
                              )
    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
                              owner = 'ranger',
                              properties = {'audit_store': 'solr'}
                              )

    self.assertResourceCalled('Execute', ('ambari-python-wrap /usr/hdp/current/ranger-admin/dba_script.py -q'),
                              user = 'ranger',
                              environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45',
                                             'RANGER_ADMIN_HOME': u'/usr/hdp/current/ranger-admin'},
                              logoutput = True
                              )

    self.assertResourceCalled('Execute', ('ambari-python-wrap /usr/hdp/current/ranger-admin/db_setup.py'),
                              user = 'ranger',
                              environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45',
                                             'RANGER_ADMIN_HOME': u'/usr/hdp/current/ranger-admin'},
                              logoutput = True
                              )

  def assert_configure_default(self):

    ### assert db setup
    self.assert_setup_db()

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-admin/conf',
      owner = 'ranger',
      group = 'ranger',
      create_parents = True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java-old.jar',
        action = ['delete'],
    )

    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
      content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar'),
      mode = 0644
    )

    self.assertResourceCalled('Execute', ('cp', '--remove-destination', '/tmp/mysql-connector-java.jar', '/usr/hdp/current/ranger-admin/ews/lib'),
      sudo = True,
      path = ['/bin', '/usr/bin/']
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java.jar',
      mode = 0644
    )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
      properties = self.getConfig()['configurations']['admin-properties'],
      owner = 'ranger'
    )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
      owner = 'ranger',
      properties = {'SQL_CONNECTOR_JAR': '/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java.jar'}
    )

    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
      content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
      mode = 0644
    )

    self.assertResourceCalled('Execute',
      '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java.jar:/usr/hdp/current/ranger-admin/ews/lib/* org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6401.ambari.apache.org:3306/ranger01\' rangeradmin01 rangeradmin01 com.mysql.jdbc.Driver',
      path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
      tries=5,
      try_sleep=10,
      environment = {}
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-admin/ews/webapp/WEB-INF/classes/conf', '/usr/hdp/current/ranger-admin/conf'),
      not_if = 'ls /usr/hdp/current/ranger-admin/conf',
      only_if = 'ls /usr/hdp/current/ranger-admin/ews/webapp/WEB-INF/classes/conf',
      sudo = True
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-admin/',
      owner='ranger',
      group='ranger',
      recursive_ownership = True
    )

    self.assertResourceCalled('Directory', '/var/run/ranger',
      mode=0755,
      owner = 'ranger',
      group = 'hadoop',
      cd_access = "a",
      create_parents=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/conf/ranger-admin-env-piddir.sh',
      content = 'export RANGER_PID_DIR_PATH=/var/run/ranger\nexport RANGER_USER=ranger',
      owner = 'ranger',
      group = 'ranger',
      mode = 0755
    )

    self.assertResourceCalled('Directory', '/var/log/ranger/admin',
      owner='ranger',
      group='ranger',
      create_parents = True,
      cd_access = 'a',
      mode = 0755
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/conf/ranger-admin-env-logdir.sh',
      content = 'export RANGER_ADMIN_LOG_DIR=/var/log/ranger/admin',
      owner = 'ranger',
      group = 'ranger',
      mode = 0755
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/conf/ranger-admin-default-site.xml',
      owner = 'ranger',
      group = 'ranger'
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/conf/security-applicationContext.xml',
      owner = 'ranger',
      group = 'ranger'
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-admin/ews/ranger-admin-services.sh', '/usr/bin/ranger-admin'),
      not_if = 'ls /usr/bin/ranger-admin',
      only_if = 'ls /usr/hdp/current/ranger-admin/ews/ranger-admin-services.sh',
      sudo = True
    )

    ranger_admin_site_copy = {}
    ranger_admin_site_copy.update(self.getConfig()['configurations']['ranger-admin-site'])
    for prop in ['ranger.jpa.jdbc.password', 'ranger.jpa.audit.jdbc.password', 'ranger.ldap.bind.password', 'ranger.ldap.ad.bind.password', 'ranger.service.https.attrib.keystore.pass', 'ranger.truststore.password']:
      if prop in ranger_admin_site_copy:
        ranger_admin_site_copy[prop] = "_"

    self.assertResourceCalled('XmlConfig', 'ranger-admin-site.xml',
      owner = 'ranger',
      group = 'ranger',
      conf_dir = '/usr/hdp/current/ranger-admin/conf',
      configurations = ranger_admin_site_copy,
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-admin-site'],
      mode = 0644
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-admin/conf/ranger_jaas',
      owner ='ranger',
      group ='ranger',
      mode = 0700
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/ews/webapp/WEB-INF/log4j.properties',
      owner = 'ranger',
      group = 'ranger',
      content = InlineTemplate(self.getConfig()['configurations']['admin-log4j']['content']),
      mode = 0644
    )

    self.assertResourceCalled('Execute', ('/usr/jdk64/jdk1.7.0_45/bin/java', '-cp', '/usr/hdp/current/ranger-admin/cred/lib/*', 'org.apache.ranger.credentialapi.buildks', 'create', 'rangeradmin', '-value', 'rangeradmin01', '-provider', 'jceks://file/etc/ranger/admin/rangeradmin.jceks'),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True,
      sudo = True
    )

    self.assertResourceCalled('File', '/etc/ranger/admin/rangeradmin.jceks',
      owner = 'ranger',
      group = 'ranger',
      mode = 0640
    )

    self.assertResourceCalled('Execute', ('/usr/jdk64/jdk1.7.0_45/bin/java', '-cp', '/usr/hdp/current/ranger-admin/cred/lib/*', 'org.apache.ranger.credentialapi.buildks', 'create', 'trustStoreAlias', '-value', 'changeit', '-provider', 'jceks://file/etc/ranger/admin/rangeradmin.jceks'),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True,
      sudo = True
    )

    self.assertResourceCalled('File', '/etc/ranger/admin/rangeradmin.jceks',
      owner = 'ranger',
      group = 'ranger',
      mode = 0640
    )

    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'ranger',
      group = 'ranger',
      conf_dir = '/usr/hdp/current/ranger-admin/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
      mode = 0644
    )

    self.assertResourceCalled('Execute', ('ambari-python-wrap /usr/hdp/current/ranger-admin/db_setup.py -javapatch'),
                              user = 'ranger',
                              environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45',
                                             'RANGER_ADMIN_HOME': u'/usr/hdp/current/ranger-admin'},
                              logoutput = True
                              )

  def assert_configure_secured(self):

    ### assert db setup
    self.assert_setup_db()

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-admin/conf',
      owner = 'ranger',
      group = 'ranger',
      create_parents = True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java-old.jar',
        action = ['delete'],
    )

    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
      content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar'),
      mode = 0644
    )

    self.assertResourceCalled('Execute', ('cp', '--remove-destination', '/tmp/mysql-connector-java.jar', '/usr/hdp/current/ranger-admin/ews/lib'),
      sudo = True,
      path = ['/bin', '/usr/bin/']
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java.jar',
      mode = 0644
    )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
      properties = self.getConfig()['configurations']['admin-properties'],
      owner = 'ranger'
    )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-admin/install.properties',
      owner = 'ranger',
      properties = {'SQL_CONNECTOR_JAR': '/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java.jar'}
    )

    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
      content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
      mode = 0644
    )

    self.assertResourceCalled('Execute',
      '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/ranger-admin/ews/lib/mysql-connector-java.jar:/usr/hdp/current/ranger-admin/ews/lib/* org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6401.ambari.apache.org:3306/ranger01\' rangeradmin01 rangeradmin01 com.mysql.jdbc.Driver',
      path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
      tries=5,
      try_sleep=10,
      environment = {}
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-admin/ews/webapp/WEB-INF/classes/conf', '/usr/hdp/current/ranger-admin/conf'),
      not_if = 'ls /usr/hdp/current/ranger-admin/conf',
      only_if = 'ls /usr/hdp/current/ranger-admin/ews/webapp/WEB-INF/classes/conf',
      sudo = True
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-admin/',
      owner='ranger',
      group='ranger',
      recursive_ownership = True
    )

    self.assertResourceCalled('Directory', '/var/run/ranger',
      mode=0755,
      owner = 'ranger',
      group = 'hadoop',
      cd_access = "a",
      create_parents=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/conf/ranger-admin-env-piddir.sh',
      content = 'export RANGER_PID_DIR_PATH=/var/run/ranger\nexport RANGER_USER=ranger',
      owner = 'ranger',
      group = 'ranger',
      mode = 0755
    )

    self.assertResourceCalled('Directory', '/var/log/ranger/admin',
      owner='ranger',
      group='ranger',
      create_parents = True,
      cd_access = 'a',
      mode = 0755
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/conf/ranger-admin-env-logdir.sh',
      content = 'export RANGER_ADMIN_LOG_DIR=/var/log/ranger/admin',
      owner = 'ranger',
      group = 'ranger',
      mode = 0755
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/conf/ranger-admin-default-site.xml',
      owner = 'ranger',
      group = 'ranger'
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/conf/security-applicationContext.xml',
      owner = 'ranger',
      group = 'ranger'
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-admin/ews/ranger-admin-services.sh', '/usr/bin/ranger-admin'),
      not_if = 'ls /usr/bin/ranger-admin',
      only_if = 'ls /usr/hdp/current/ranger-admin/ews/ranger-admin-services.sh',
      sudo = True
    )

    ranger_admin_site_copy = {}
    ranger_admin_site_copy.update(self.getConfig()['configurations']['ranger-admin-site'])
    for prop in ['ranger.jpa.jdbc.password', 'ranger.jpa.audit.jdbc.password', 'ranger.ldap.bind.password', 'ranger.ldap.ad.bind.password', 'ranger.service.https.attrib.keystore.pass', 'ranger.truststore.password']:
      if prop in ranger_admin_site_copy:
        ranger_admin_site_copy[prop] = "_"

    self.assertResourceCalled('XmlConfig', 'ranger-admin-site.xml',
      owner = 'ranger',
      group = 'ranger',
      conf_dir = '/usr/hdp/current/ranger-admin/conf',
      configurations = ranger_admin_site_copy,
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-admin-site'],
      mode = 0644
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-admin/conf/ranger_jaas',
      owner ='ranger',
      group ='ranger',
      mode = 0700
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-admin/ews/webapp/WEB-INF/log4j.properties',
      owner = 'ranger',
      group = 'ranger',
      content = InlineTemplate(self.getConfig()['configurations']['admin-log4j']['content']),
      mode = 0644
    )

    self.assertResourceCalled('Execute', ('/usr/jdk64/jdk1.7.0_45/bin/java', '-cp', '/usr/hdp/current/ranger-admin/cred/lib/*', 'org.apache.ranger.credentialapi.buildks', 'create', 'rangeradmin', '-value', 'rangeradmin01', '-provider', 'jceks://file/etc/ranger/admin/rangeradmin.jceks'),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True,
      sudo = True
    )

    self.assertResourceCalled('File', '/etc/ranger/admin/rangeradmin.jceks',
      owner = 'ranger',
      group = 'ranger',
      mode = 0640
    )

    self.assertResourceCalled('Execute', ('/usr/jdk64/jdk1.7.0_45/bin/java', '-cp', '/usr/hdp/current/ranger-admin/cred/lib/*', 'org.apache.ranger.credentialapi.buildks', 'create', 'trustStoreAlias', '-value', 'changeit', '-provider', 'jceks://file/etc/ranger/admin/rangeradmin.jceks'),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True,
      sudo = True
    )

    self.assertResourceCalled('File', '/etc/ranger/admin/rangeradmin.jceks',
      owner = 'ranger',
      group = 'ranger',
      mode = 0640
    )

    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'ranger',
      group = 'ranger',
      conf_dir = '/usr/hdp/current/ranger-admin/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
      mode = 0644
    )

    self.assertResourceCalled('Execute', ('ambari-python-wrap /usr/hdp/current/ranger-admin/db_setup.py -javapatch'),
                              user = 'ranger',
                              environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45',
                                             'RANGER_ADMIN_HOME': u'/usr/hdp/current/ranger-admin'},
                              logoutput = True
                              )
