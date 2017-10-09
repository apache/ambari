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
from datetime import datetime
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *
from only_for_platform import not_for_platform, PLATFORM_WINDOWS
from resource_management.libraries.functions.ranger_functions import Rangeradmin
from resource_management.libraries.functions.ranger_functions_v2 import RangeradminV2

@not_for_platform(PLATFORM_WINDOWS)
class TestRangerKMS(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "RANGER_KMS/0.5.0.2.3/package"
  STACK_VERSION = "2.5"

  @patch("os.path.isfile")
  def test_configure_default(self, isfile_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kms_server.py",
                   classname = "KmsServer",
                   command = "configure",
                   config_file="ranger-kms-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertTrue(isfile_mock.called)
    self.assertNoMoreResources()

  current_date = datetime.now()

  class DTMOCK(object):
    """
    Mock datetime to avoid test failures when test run a little bit slower than usuall.
    """
    def now(self):
      return TestRangerKMS.current_date

  @patch("resource_management.libraries.functions.ranger_functions.Rangeradmin.check_ranger_login_urllib2", new=MagicMock(return_value=200))
  @patch("resource_management.libraries.functions.ranger_functions.Rangeradmin.create_ambari_admin_user", new=MagicMock(return_value=200))
  @patch("kms.get_repo")
  @patch("kms.create_repo")
  @patch("os.path.isfile")
  @patch("kms.datetime", new=DTMOCK())
  def test_start_default(self, get_repo_mock, create_repo_mock, isfile_mock):
    get_repo_mock.return_value = True
    create_repo_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kms_server.py",
                   classname = "KmsServer",
                   command = "start",
                   config_file="ranger-kms-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()

    # TODO confirm repo call

    current_datetime = self.current_date.strftime("%Y-%m-%d %H:%M:%S")

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/ranger-security.xml',
      owner = 'kms',
      group = 'kms',
      content = '<ranger>\n<enabled>{0}</enabled>\n</ranger>'.format(current_datetime),
      mode = 0644
    )

    self.assertResourceCalled('Directory', '/etc/ranger/c1_kms',
      owner = 'kms',
      group = 'kms',
      mode = 0775,
      create_parents = True
    )

    self.assertResourceCalled('Directory', '/etc/ranger/c1_kms/policycache',
      owner = 'kms',
      group = 'kms',
      mode = 0775,
      create_parents = True
    )

    self.assertResourceCalled('File', '/etc/ranger/c1_kms/policycache/kms_c1_kms.json',
      owner = 'kms',
      group = 'kms',
      mode = 0644
    )

    plugin_audit_properties_copy = {}
    plugin_audit_properties_copy.update(self.getConfig()['configurations']['ranger-kms-audit'])

    if 'xasecure.audit.destination.db.password' in plugin_audit_properties_copy:
      plugin_audit_properties_copy['xasecure.audit.destination.db.password'] = "crypted"

    self.assertResourceCalled('XmlConfig', 'ranger-kms-audit.xml',
      mode = 0744,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = plugin_audit_properties_copy,
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-kms-audit']
    )

    self.assertResourceCalled('XmlConfig', 'ranger-kms-security.xml',
      mode = 0744,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = self.getConfig()['configurations']['ranger-kms-security'],
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-kms-security']
    )

    ranger_kms_policymgr_ssl_copy = {}
    ranger_kms_policymgr_ssl_copy.update(self.getConfig()['configurations']['ranger-kms-policymgr-ssl'])

    for prop in ['xasecure.policymgr.clientssl.keystore.password', 'xasecure.policymgr.clientssl.truststore.password']:
      if prop in ranger_kms_policymgr_ssl_copy:
        ranger_kms_policymgr_ssl_copy[prop] = "crypted"

    self.assertResourceCalled('XmlConfig', 'ranger-policymgr-ssl.xml',
      mode = 0744,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = ranger_kms_policymgr_ssl_copy,
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-kms-policymgr-ssl']
    )

    self.assertResourceCalled('Execute', ('/usr/hdp/current/ranger-kms/ranger_credential_helper.py', '-l', '/usr/hdp/current/ranger-kms/cred/lib/*', '-f', '/etc/ranger/c1_kms/cred.jceks', '-k', 'sslKeyStore', '-v', 'myKeyFilePassword', '-c', '1'),
     environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True, 
      sudo=True
    )

    self.assertResourceCalled('Execute', ('/usr/hdp/current/ranger-kms/ranger_credential_helper.py', '-l', '/usr/hdp/current/ranger-kms/cred/lib/*', '-f', '/etc/ranger/c1_kms/cred.jceks', '-k', 'sslTrustStore', '-v', 'changeit', '-c', '1'),
     environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True, 
      sudo=True
    )

    self.assertResourceCalled('File', '/etc/ranger/c1_kms/cred.jceks',
      owner = 'kms',
      group = 'kms',
      mode = 0640
    )

    self.assertResourceCalled('HdfsResource', '/ranger/audit',
                        type = 'directory',
                        action = ['create_on_execute'],
                        owner = 'hdfs',
                        group = 'hdfs',
                        mode = 0755,
                        recursive_chmod = True,
                        user = 'hdfs',
                        security_enabled = False,
                        keytab = None,
                        kinit_path_local = '/usr/bin/kinit',
                        hadoop_bin_dir = '/usr/hdp/2.5.0.0-777/hadoop/bin',
                        hadoop_conf_dir = '/usr/hdp/2.5.0.0-777/hadoop/conf',
                        principal_name = None,
                        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                        default_fs = 'hdfs://c6401.ambari.apache.org:8020'
    )

    self.assertResourceCalled('HdfsResource', '/ranger/audit/kms',
                        type = 'directory',
                        action = ['create_on_execute'],
                        owner = 'kms',
                        group = 'kms',
                        mode = 0750,
                        recursive_chmod = True,
                        user = 'hdfs',
                        security_enabled = False,
                        keytab = None,
                        kinit_path_local = '/usr/bin/kinit',
                        hadoop_bin_dir = '/usr/hdp/2.5.0.0-777/hadoop/bin',
                        hadoop_conf_dir = '/usr/hdp/2.5.0.0-777/hadoop/conf',
                        principal_name = None,
                        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                        default_fs = 'hdfs://c6401.ambari.apache.org:8020'
    )

    self.assertResourceCalled('HdfsResource', None,
                        action = ['execute'],
                        user = 'hdfs',
                        security_enabled = False,
                        keytab = None,
                        kinit_path_local = '/usr/bin/kinit',
                        hadoop_bin_dir = '/usr/hdp/2.5.0.0-777/hadoop/bin',
                        hadoop_conf_dir = '/usr/hdp/2.5.0.0-777/hadoop/conf',
                        principal_name = None,
                        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                        default_fs = 'hdfs://c6401.ambari.apache.org:8020'
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/hdfs-site.xml',
      action = ['delete'],
    )

    self.assertResourceCalled('Directory', '/tmp/jce_dir',
      create_parents = True,
    )

    self.assertResourceCalled('File', '/tmp/jce_dir/UnlimitedJCEPolicyJDK7.zip',
      content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//UnlimitedJCEPolicyJDK7.zip'),
      mode = 0644,
    )

    self.assertResourceCalled('File', '/usr/jdk64/jdk1.7.0_45/jre/lib/security/local_policy.jar',
      action = ["delete"]
    )

    self.assertResourceCalled('File', '/usr/jdk64/jdk1.7.0_45/jre/lib/security/US_export_policy.jar',
      action = ["delete"]
    )

    self.assertResourceCalled('Execute', ("unzip", "-o", "-j", "-q", "/tmp/jce_dir/UnlimitedJCEPolicyJDK7.zip", "-d", "/usr/jdk64/jdk1.7.0_45/jre/lib/security"),
      only_if = 'test -e /usr/jdk64/jdk1.7.0_45/jre/lib/security && test -f /tmp/jce_dir/UnlimitedJCEPolicyJDK7.zip',
      path=['/bin/', '/usr/bin'],
      sudo=True
    )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-kms/install.properties',
      owner = 'kms',
      properties = {'db_password': '_', 'KMS_MASTER_KEY_PASSWD': '_', 'REPOSITORY_CONFIG_PASSWORD': '_', 'db_root_password': '_'}
    )

    self.assertResourceCalled('Execute', '/usr/hdp/current/ranger-kms/ranger-kms start',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = 'ps -ef | grep proc_rangerkms | grep -v grep',
        user = 'kms'
    )

    self.assertTrue(isfile_mock.called)
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kms_server.py",
                   classname = "KmsServer",
                   command = "stop",
                   config_file="ranger-kms-default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/hdp/current/ranger-kms/ranger-kms stop',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        user = 'kms'
    )
    self.assertResourceCalled('File', '/var/run/ranger_kms/rangerkms.pid',
      action = ['delete']
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-kms/conf',
      owner = 'kms',
      group = 'kms',
      create_parents = True
    )

    self.assertResourceCalled('Directory', '/etc/security/serverKeys',
      create_parents = True,
      cd_access = "a",
    )

    self.assertResourceCalled('Directory', '/etc/ranger/kms',
      create_parents = True,
      cd_access = "a",
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/ews/webapp/lib/mysql-connector-java-old.jar',
        action = ['delete'],
    )

    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
      content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar'),
      mode = 0644
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-kms/ews/lib',
      mode = 0755
    )

    self.assertResourceCalled('Execute', ('cp', '--remove-destination', '/tmp/mysql-connector-java.jar',
      '/usr/hdp/current/ranger-kms/ews/webapp/lib'),
      path=['/bin', '/usr/bin/'],
      sudo=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/ews/webapp/lib/mysql-connector-java.jar',
      mode = 0644
    )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-kms/install.properties',
      properties = self.getConfig()['configurations']['kms-properties'],
      owner = 'kms'
    )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-kms/install.properties',
      properties = {'SQL_CONNECTOR_JAR': '/usr/hdp/current/ranger-kms/ews/webapp/lib/mysql-connector-java.jar'},
      owner = 'kms'
    )

    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
      content=DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
      mode=0644,
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/ranger-kms/ews/webapp/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6401.ambari.apache.org:3306/rangerkms01\' rangerkms01 rangerkms01 com.mysql.jdbc.Driver',
      path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10, environment = {}
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-kms/ews/webapp/WEB-INF/classes/lib',
      mode = 0755,
      owner = 'kms',
      group = 'kms'
    )

    self.assertResourceCalled('Execute', ('cp', '/usr/hdp/current/ranger-kms/ranger-kms-initd', '/etc/init.d/ranger-kms'),
      not_if=format('ls /etc/init.d/ranger-kms'),
      only_if=format('ls /usr/hdp/current/ranger-kms/ranger-kms-initd'),
      sudo=True
    )

    self.assertResourceCalled('File', '/etc/init.d/ranger-kms',
      mode=0755,
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-kms/',
      owner = 'kms',
      group = 'kms',
      recursive_ownership = True,
    )

    self.assertResourceCalled('Directory', '/var/run/ranger_kms',
      mode=0755,
      owner = 'kms',
      group = 'hadoop',
      cd_access = "a",
      create_parents=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/ranger-kms-env-piddir.sh',
      content = 'export RANGER_KMS_PID_DIR_PATH=/var/run/ranger_kms\nexport KMS_USER=kms',
      owner = 'kms',
      group = 'kms',
      mode = 0755
    )

    self.assertResourceCalled('Directory', '/var/log/ranger/kms',
      owner = 'kms',
      group = 'kms',
      cd_access = 'a',
      create_parents = True,
      mode = 0755
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/ranger-kms-env-logdir.sh',
      content = format("export RANGER_KMS_LOG_DIR=/var/log/ranger/kms"),
      owner = 'kms',
      group = 'kms',
      mode=0755
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-kms/ranger-kms', '/usr/bin/ranger-kms'),
      not_if=format('ls /usr/bin/ranger-kms'),
      only_if=format('ls /usr/hdp/current/ranger-kms/ranger-kms'),
      sudo=True
    )

    self.assertResourceCalled('File', '/usr/bin/ranger-kms',
      mode=0755
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-kms/ranger-kms', '/usr/bin/ranger-kms-services.sh'),
      not_if=format('ls /usr/bin/ranger-kms-services.sh'),
      only_if=format('ls /usr/hdp/current/ranger-kms/ranger-kms'),
      sudo=True
    )

    self.assertResourceCalled('File', '/usr/bin/ranger-kms-services.sh',
      mode=0755
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-kms/ranger-kms-initd', '/usr/hdp/current/ranger-kms/ranger-kms-services.sh'),
      not_if=format('ls /usr/hdp/current/ranger-kms/ranger-kms-services.sh'),
      only_if=format('ls /usr/hdp/current/ranger-kms/ranger-kms-initd'),
      sudo=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/ranger-kms-services.sh',
      mode=0755
    )

    self.assertResourceCalled('Directory', '/var/log/ranger/kms',
      owner = 'kms',
      group = 'kms',
      mode = 0775
    )

    self.assertResourceCalled('Execute', ('/usr/jdk64/jdk1.7.0_45/bin/java', '-cp', '/usr/hdp/current/ranger-kms/cred/lib/*', 'org.apache.ranger.credentialapi.buildks', 'create', 'ranger.ks.jdbc.password', '-value', 'rangerkms01', '-provider', 'jceks://file/etc/ranger/kms/rangerkms.jceks'),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True,
      sudo=True
    )

    self.assertResourceCalled('File', '/etc/ranger/kms/rangerkms.jceks',
      owner = 'kms',
      group = 'kms',
      mode = 0640
    )

    self.assertResourceCalled('Execute', ('/usr/jdk64/jdk1.7.0_45/bin/java', '-cp', '/usr/hdp/current/ranger-kms/cred/lib/*', 'org.apache.ranger.credentialapi.buildks', 'create', 'ranger.ks.masterkey.password', '-value', 'StrongPassword01', '-provider', 'jceks://file/etc/ranger/kms/rangerkms.jceks'),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True,
      sudo=True
    )

    self.assertResourceCalled('File', '/etc/ranger/kms/rangerkms.jceks',
      owner = 'kms',
      group = 'kms',
      mode = 0640
    )

    dbks_site_copy = {}
    dbks_site_copy.update(self.getConfig()['configurations']['dbks-site'])
    for prop in ['ranger.db.encrypt.key.password', 'ranger.ks.jpa.jdbc.password', 'ranger.ks.hsm.partition.password']:
      if prop in dbks_site_copy:
        dbks_site_copy[prop] = "_"

    self.assertResourceCalled('XmlConfig', 'dbks-site.xml',
      mode=0644,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = dbks_site_copy,
      configuration_attributes = self.getConfig()['configuration_attributes']['dbks-site']
    )

    self.assertResourceCalled('XmlConfig', 'ranger-kms-site.xml',
      mode = 0644,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = self.getConfig()['configurations']['ranger-kms-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-kms-site']
    )

    self.assertResourceCalled('XmlConfig', 'kms-site.xml',
      mode = 0644,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = self.getConfig()['configurations']['kms-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['kms-site']
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/kms-log4j.properties',
      mode = 0644,
      owner = 'kms',
      group = 'kms',
      content = InlineTemplate(self.getConfig()['configurations']['kms-log4j']['content'])
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/core-site.xml',
      action = ['delete'],
    )

  @patch("os.path.isfile")
  def test_configure_secured(self, isfile_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kms_server.py",
                   classname = "KmsServer",
                   command = "configure",
                   config_file="ranger-kms-secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertTrue(isfile_mock.called)
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.ranger_functions_v2.RangeradminV2.check_ranger_login_curl", new=MagicMock(return_value=(200, '', '')))
  @patch("resource_management.libraries.functions.ranger_functions_v2.RangeradminV2.get_repository_by_name_curl", new=MagicMock(return_value=({'name': 'c1_kms'})))
  @patch("resource_management.libraries.functions.ranger_functions_v2.RangeradminV2.create_repository_curl", new=MagicMock(return_value=({'name': 'c1_kms'})))
  @patch("os.path.isfile")
  def test_start_secured(self, isfile_mock):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/kms_server.py",
                   classname = "KmsServer",
                   command = "start",
                   config_file="ranger-kms-secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()

    # TODO repo call in secure

    current_datetime = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/ranger-security.xml',
      owner = 'kms',
      group = 'kms',
      content = '<ranger>\n<enabled>{0}</enabled>\n</ranger>'.format(current_datetime),
      mode = 0644
    )

    self.assertResourceCalled('Directory', '/etc/ranger/c1_kms',
      owner = 'kms',
      group = 'kms',
      mode = 0775,
      create_parents = True
    )

    self.assertResourceCalled('Directory', '/etc/ranger/c1_kms/policycache',
      owner = 'kms',
      group = 'kms',
      mode = 0775,
      create_parents = True
    )

    self.assertResourceCalled('File', '/etc/ranger/c1_kms/policycache/kms_c1_kms.json',
      owner = 'kms',
      group = 'kms',
      mode = 0644
    )

    plugin_audit_properties_copy = {}
    plugin_audit_properties_copy.update(self.getConfig()['configurations']['ranger-kms-audit'])

    if 'xasecure.audit.destination.db.password' in plugin_audit_properties_copy:
      plugin_audit_properties_copy['xasecure.audit.destination.db.password'] = "crypted"

    self.assertResourceCalled('XmlConfig', 'ranger-kms-audit.xml',
      mode = 0744,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = plugin_audit_properties_copy,
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-kms-audit']
    )

    self.assertResourceCalled('XmlConfig', 'ranger-kms-security.xml',
      mode = 0744,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = self.getConfig()['configurations']['ranger-kms-security'],
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-kms-security']
    )

    ranger_kms_policymgr_ssl_copy = {}
    ranger_kms_policymgr_ssl_copy.update(self.getConfig()['configurations']['ranger-kms-policymgr-ssl'])

    for prop in ['xasecure.policymgr.clientssl.keystore.password', 'xasecure.policymgr.clientssl.truststore.password']:
      if prop in ranger_kms_policymgr_ssl_copy:
        ranger_kms_policymgr_ssl_copy[prop] = "crypted"

    self.assertResourceCalled('XmlConfig', 'ranger-policymgr-ssl.xml',
      mode = 0744,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = ranger_kms_policymgr_ssl_copy,
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-kms-policymgr-ssl']
    )

    self.assertResourceCalled('Execute', ('/usr/hdp/current/ranger-kms/ranger_credential_helper.py', '-l', '/usr/hdp/current/ranger-kms/cred/lib/*', '-f', '/etc/ranger/c1_kms/cred.jceks', '-k', 'sslKeyStore', '-v', 'myKeyFilePassword', '-c', '1'),
     environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True, 
      sudo=True
    )

    self.assertResourceCalled('Execute', ('/usr/hdp/current/ranger-kms/ranger_credential_helper.py', '-l', '/usr/hdp/current/ranger-kms/cred/lib/*', '-f', '/etc/ranger/c1_kms/cred.jceks', '-k', 'sslTrustStore', '-v', 'changeit', '-c', '1'),
     environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True, 
      sudo=True
    )

    self.assertResourceCalled('File', '/etc/ranger/c1_kms/cred.jceks',
      owner = 'kms',
      group = 'kms',
      mode = 0640
    )

    self.assertResourceCalled('HdfsResource', '/ranger/audit',
                        type = 'directory',
                        action = ['create_on_execute'],
                        owner = 'hdfs',
                        group = 'hdfs',
                        mode = 0755,
                        recursive_chmod = True,
                        user = 'hdfs',
                        security_enabled = True,
                        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                        kinit_path_local = '/usr/bin/kinit',
                        hadoop_bin_dir = '/usr/hdp/2.5.0.0-777/hadoop/bin',
                        hadoop_conf_dir = '/usr/hdp/2.5.0.0-777/hadoop/conf',
                        principal_name = 'hdfs-cl1@EXAMPLE.COM',
                        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                        default_fs = 'hdfs://c6401.ambari.apache.org:8020'
    )

    self.assertResourceCalled('HdfsResource', '/ranger/audit/kms',
                        type = 'directory',
                        action = ['create_on_execute'],
                        owner = 'kms',
                        group = 'kms',
                        mode = 0750,
                        recursive_chmod = True,
                        user = 'hdfs',
                        security_enabled = True,
                        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                        kinit_path_local = '/usr/bin/kinit',
                        hadoop_bin_dir = '/usr/hdp/2.5.0.0-777/hadoop/bin',
                        hadoop_conf_dir = '/usr/hdp/2.5.0.0-777/hadoop/conf',
                        principal_name = 'hdfs-cl1@EXAMPLE.COM',
                        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                        default_fs = 'hdfs://c6401.ambari.apache.org:8020'
    )

    self.assertResourceCalled('HdfsResource', None,
                        action = ['execute'],
                        user = 'hdfs',
                        security_enabled = True,
                        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                        kinit_path_local = '/usr/bin/kinit',
                        hadoop_bin_dir = '/usr/hdp/2.5.0.0-777/hadoop/bin',
                        hadoop_conf_dir = '/usr/hdp/2.5.0.0-777/hadoop/conf',
                        principal_name = 'hdfs-cl1@EXAMPLE.COM',
                        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                        default_fs = 'hdfs://c6401.ambari.apache.org:8020'
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/hdfs-site.xml',
      action = ['delete'],
    )

    self.assertResourceCalled('Directory', '/tmp/jce_dir',
      create_parents = True,
    )

    self.assertResourceCalled('File', '/tmp/jce_dir/UnlimitedJCEPolicyJDK7.zip',
      content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//UnlimitedJCEPolicyJDK7.zip'),
      mode = 0644,
    )

    self.assertResourceCalled('File', '/usr/jdk64/jdk1.7.0_45/jre/lib/security/local_policy.jar',
      action = ["delete"]
    )

    self.assertResourceCalled('File', '/usr/jdk64/jdk1.7.0_45/jre/lib/security/US_export_policy.jar',
      action = ["delete"]
    )

    self.assertResourceCalled('Execute', ("unzip", "-o", "-j", "-q", "/tmp/jce_dir/UnlimitedJCEPolicyJDK7.zip", "-d", "/usr/jdk64/jdk1.7.0_45/jre/lib/security"),
      only_if = 'test -e /usr/jdk64/jdk1.7.0_45/jre/lib/security && test -f /tmp/jce_dir/UnlimitedJCEPolicyJDK7.zip',
      path=['/bin/', '/usr/bin'],
      sudo=True
    )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-kms/install.properties',
      owner = 'kms',
      properties = {'db_password': '_', 'KMS_MASTER_KEY_PASSWD': '_', 'REPOSITORY_CONFIG_PASSWORD': '_', 'db_root_password': '_'}
    )

    self.assertResourceCalled('Execute', '/usr/hdp/current/ranger-kms/ranger-kms start',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = 'ps -ef | grep proc_rangerkms | grep -v grep',
        user = 'kms'
    )

    self.assertTrue(isfile_mock.called)
    self.assertNoMoreResources()

  def assert_configure_secured(self):

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-kms/conf',
      owner = 'kms',
      group = 'kms',
      create_parents = True
    )

    self.assertResourceCalled('Directory', '/etc/security/serverKeys',
      create_parents = True,
      cd_access = "a",
    )

    self.assertResourceCalled('Directory', '/etc/ranger/kms',
      create_parents = True,
      cd_access = "a",
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/ews/webapp/lib/mysql-connector-java-old.jar',
        action = ['delete'],
    )

    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
      content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar'),
      mode = 0644
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-kms/ews/lib',
      mode = 0755
    )

    self.assertResourceCalled('Execute', ('cp', '--remove-destination', '/tmp/mysql-connector-java.jar',
      '/usr/hdp/current/ranger-kms/ews/webapp/lib'),
      path=['/bin', '/usr/bin/'],
      sudo=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/ews/webapp/lib/mysql-connector-java.jar',
      mode = 0644
    )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-kms/install.properties',
      properties = self.getConfig()['configurations']['kms-properties'],
      owner = 'kms'
    )

    self.assertResourceCalled('ModifyPropertiesFile', '/usr/hdp/current/ranger-kms/install.properties',
      properties = {'SQL_CONNECTOR_JAR': '/usr/hdp/current/ranger-kms/ews/webapp/lib/mysql-connector-java.jar'},
      owner = 'kms'
    )

    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
      content=DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
      mode=0644,
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/ranger-kms/ews/webapp/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6401.ambari.apache.org:3306/rangerkms01\' rangerkms01 rangerkms01 com.mysql.jdbc.Driver',
      path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10, environment = {}
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-kms/ews/webapp/WEB-INF/classes/lib',
      mode = 0755,
      owner = 'kms',
      group = 'kms'
    )

    self.assertResourceCalled('Execute', ('cp', '/usr/hdp/current/ranger-kms/ranger-kms-initd', '/etc/init.d/ranger-kms'),
      not_if=format('ls /etc/init.d/ranger-kms'),
      only_if=format('ls /usr/hdp/current/ranger-kms/ranger-kms-initd'),
      sudo=True
    )

    self.assertResourceCalled('File', '/etc/init.d/ranger-kms',
      mode=0755,
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/ranger-kms/',
      owner = 'kms',
      group = 'kms',
      recursive_ownership = True,
    )

    self.assertResourceCalled('Directory', '/var/run/ranger_kms',
      mode=0755,
      owner = 'kms',
      group = 'hadoop',
      cd_access = "a",
      create_parents=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/ranger-kms-env-piddir.sh',
      content = 'export RANGER_KMS_PID_DIR_PATH=/var/run/ranger_kms\nexport KMS_USER=kms',
      owner = 'kms',
      group = 'kms',
      mode = 0755
    )

    self.assertResourceCalled('Directory', '/var/log/ranger/kms',
      owner = 'kms',
      group = 'kms',
      cd_access = 'a',
      create_parents = True,
      mode = 0755
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/ranger-kms-env-logdir.sh',
      content = format("export RANGER_KMS_LOG_DIR=/var/log/ranger/kms"),
      owner = 'kms',
      group = 'kms',
      mode=0755
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-kms/ranger-kms', '/usr/bin/ranger-kms'),
      not_if=format('ls /usr/bin/ranger-kms'),
      only_if=format('ls /usr/hdp/current/ranger-kms/ranger-kms'),
      sudo=True
    )

    self.assertResourceCalled('File', '/usr/bin/ranger-kms',
      mode=0755
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-kms/ranger-kms', '/usr/bin/ranger-kms-services.sh'),
      not_if=format('ls /usr/bin/ranger-kms-services.sh'),
      only_if=format('ls /usr/hdp/current/ranger-kms/ranger-kms'),
      sudo=True
    )

    self.assertResourceCalled('File', '/usr/bin/ranger-kms-services.sh',
      mode=0755
    )

    self.assertResourceCalled('Execute', ('ln', '-sf', '/usr/hdp/current/ranger-kms/ranger-kms-initd', '/usr/hdp/current/ranger-kms/ranger-kms-services.sh'),
      not_if=format('ls /usr/hdp/current/ranger-kms/ranger-kms-services.sh'),
      only_if=format('ls /usr/hdp/current/ranger-kms/ranger-kms-initd'),
      sudo=True
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/ranger-kms-services.sh',
      mode=0755
    )

    self.assertResourceCalled('Directory', '/var/log/ranger/kms',
      owner = 'kms',
      group = 'kms',
      mode = 0775
    )

    self.assertResourceCalled('Execute', ('/usr/jdk64/jdk1.7.0_45/bin/java', '-cp', '/usr/hdp/current/ranger-kms/cred/lib/*', 'org.apache.ranger.credentialapi.buildks', 'create', 'ranger.ks.jdbc.password', '-value', 'rangerkms01', '-provider', 'jceks://file/etc/ranger/kms/rangerkms.jceks'),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True,
      sudo=True
    )

    self.assertResourceCalled('File', '/etc/ranger/kms/rangerkms.jceks',
      owner = 'kms',
      group = 'kms',
      mode = 0640
    )

    self.assertResourceCalled('Execute', ('/usr/jdk64/jdk1.7.0_45/bin/java', '-cp', '/usr/hdp/current/ranger-kms/cred/lib/*', 'org.apache.ranger.credentialapi.buildks', 'create', 'ranger.ks.masterkey.password', '-value', 'StrongPassword01', '-provider', 'jceks://file/etc/ranger/kms/rangerkms.jceks'),
      environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
      logoutput=True,
      sudo=True
    )

    self.assertResourceCalled('File', '/etc/ranger/kms/rangerkms.jceks',
      owner = 'kms',
      group = 'kms',
      mode = 0640
    )

    dbks_site_copy = {}
    dbks_site_copy.update(self.getConfig()['configurations']['dbks-site'])
    for prop in ['ranger.db.encrypt.key.password', 'ranger.ks.jpa.jdbc.password', 'ranger.ks.hsm.partition.password']:
      if prop in dbks_site_copy:
        dbks_site_copy[prop] = "_"

    self.assertResourceCalled('XmlConfig', 'dbks-site.xml',
      mode=0644,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = dbks_site_copy,
      configuration_attributes = self.getConfig()['configuration_attributes']['dbks-site']
    )

    self.assertResourceCalled('XmlConfig', 'ranger-kms-site.xml',
      mode = 0644,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = self.getConfig()['configurations']['ranger-kms-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['ranger-kms-site']
    )

    self.assertResourceCalled('XmlConfig', 'kms-site.xml',
      mode = 0644,
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = self.getConfig()['configurations']['kms-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['kms-site']
    )

    self.assertResourceCalled('File', '/usr/hdp/current/ranger-kms/conf/kms-log4j.properties',
      mode = 0644,
      owner = 'kms',
      group = 'kms',
      content = InlineTemplate(self.getConfig()['configurations']['kms-log4j']['content'])
    )

    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'kms',
      group = 'kms',
      conf_dir = '/usr/hdp/current/ranger-kms/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
      mode = 0644
    )

