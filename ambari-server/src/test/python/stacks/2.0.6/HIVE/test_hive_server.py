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
import socket
import subprocess

from mock.mock import MagicMock, patch
from resource_management.core import shell
from resource_management.libraries.functions import dynamic_variable_interpretation
from stacks.utils.RMFTestCase import *

class TestHiveServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"
  UPGRADE_STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  @patch.object(shell, "call", new=MagicMock(return_value=(0, '')))
  @patch.object(subprocess,"Popen")
  @patch("socket.socket")
  @patch.object(dynamic_variable_interpretation, "copy_tarballs_to_hdfs", new=MagicMock())
  def test_start_default(self, socket_mock, popen_mock):
    s = socket_mock.return_value
    
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                         classname = "HiveServer",
                         command = "start",
                         config_file="default.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'hive --config /etc/hive/conf.server --service metatool -updateLocation hdfs://c6401.ambari.apache.org:8020/apps/hive/warehouse ',
        environment = {'PATH' : "/bin:/usr/lib/hive/bin:/usr/bin"},
        user = 'hive',
    )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.log /var/run/hive/hive-server.pid /etc/hive/conf.server /var/log/hive',
                              not_if = 'ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1',
                              environment = {'HADOOP_HOME' : '/usr', 'JAVA_HOME':'/usr/jdk64/jdk1.7.0_45'},
                              path = ["/bin:/usr/lib/hive/bin:/usr/bin"],
                              user = 'hive'
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )

    self.assertNoMoreResources()
    self.assertTrue(socket_mock.called)
    self.assertTrue(s.close.called)

  @patch("socket.socket")
  @patch.object(dynamic_variable_interpretation, "copy_tarballs_to_hdfs", new=MagicMock())
  def test_stop_default(self, socket_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill `cat /var/run/hive/hive-server.pid`',
      not_if = '! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1)',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `cat /var/run/hive/hive-server.pid`',
      not_if = '! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1) )',
    )
    self.assertResourceCalled('Execute', '! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1)',
      tries = 20,
      try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive-server.pid',
      action = ['delete'],
    )
    
    self.assertNoMoreResources()
    self.assertFalse(socket_mock.called)

    
  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "configure",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  @patch("hive_service.check_fs_root")
  @patch("socket.socket")
  def test_start_secured(self, socket_mock, check_fs_root_mock):
    s = socket_mock.return_value

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "start",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hive.service.keytab hive/c6401.ambari.apache.org@EXAMPLE.COM; ',
                              user = 'hive',
                              )
    self.assertResourceCalled('Execute', '/tmp/start_hiveserver2_script /var/log/hive/hive-server2.out /var/log/hive/hive-server2.log /var/run/hive/hive-server.pid /etc/hive/conf.server /var/log/hive',
                              not_if = 'ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1',
                              environment = {'HADOOP_HOME' : '/usr', 'JAVA_HOME': '/usr/jdk64/jdk1.7.0_45'},
                              path = ["/bin:/usr/lib/hive/bin:/usr/bin"],
                              user = 'hive'
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/lib/hive/lib//mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive \'!`"\'"\'"\' 1\' com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM; ',
                              user = 'ambari-qa',
                              )
    self.assertResourceCalled('Execute', "! beeline -u 'jdbc:hive2://c6401.ambari.apache.org:10000/;principal=hive/_HOST@EXAMPLE.COM' -e '' 2>&1| awk '{print}'|grep -i -e 'Connection refused' -e 'Invalid URL'",
                              path = ['/bin/', '/usr/bin/', '/usr/lib/hive/bin/', '/usr/sbin/'],
                              user = 'ambari-qa',
                              timeout = 30,
                              )
    self.assertNoMoreResources()
    self.assertTrue(check_fs_root_mock.called)

  @patch("socket.socket")
  def test_stop_secured(self, socket_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "stop",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill `cat /var/run/hive/hive-server.pid`',
      not_if = '! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1)',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh kill -9 `cat /var/run/hive/hive-server.pid`',
      not_if = '! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1) )',
    )
    self.assertResourceCalled('Execute', '! (ls /var/run/hive/hive-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/hive/hive-server.pid` >/dev/null 2>&1)',
      tries = 20,
      try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive-server.pid',
      action = ['delete'],
    )
    
    self.assertNoMoreResources()
    self.assertFalse(socket_mock.called)

  def assert_configure_default(self):
    self.assertResourceCalled('HdfsDirectory', '/apps/hive/warehouse',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0777,
                              owner = 'hive',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/user/hive',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0700,
                              owner = 'hive',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              action = ['create'],
                              bin_dir = '/usr/bin',
                              )
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-exec-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf.server',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                     u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                     u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content = Template('hive.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/lib/hive/lib//mysql-connector-java.jar'),
                              path = ['/bin', '/usr/bin/'],
                              sudo = True,
                              )
    self.assertResourceCalled('File', '/usr/lib/hive/lib//mysql-connector-java.jar',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
                              content = DownloadSource('http://c6401.ambari.apache.org:8080/resources'
                                                       '/DBConnectionVerification.jar'),
                              )
    self.assertResourceCalled('File', '/tmp/start_hiveserver2_script',
                              content = Template('startHiveserver2.sh.j2'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner = 'hive',
                              mode = 0755,
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner = 'hive',
                              mode = 0755,
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner = 'hive',
                              mode = 0755,
                              group = 'hadoop',
                              recursive = True,
                              cd_access = 'a',
                              )


  def assert_configure_secured(self):
    self.assertResourceCalled('HdfsDirectory', '/apps/hive/warehouse',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0777,
                              owner = 'hive',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', '/user/hive',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0700,
                              owner = 'hive',
                              bin_dir = '/usr/bin',
                              action = ['create_delayed'],
                              )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              action = ['create'],
                              bin_dir = '/usr/bin',
                              )
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              recursive = True,
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-exec-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf.server',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                     u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                     u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content = Template('hive.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('Execute', ('cp',
                                          '--remove-destination',
                                          '/usr/share/java/mysql-connector-java.jar',
                                          '/usr/lib/hive/lib//mysql-connector-java.jar'),
                              path = ['/bin', '/usr/bin/'],
                              sudo = True,
                              )
    self.assertResourceCalled('File', '/usr/lib/hive/lib//mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
    )
    self.assertResourceCalled('File', '/tmp/start_hiveserver2_script',
                              content = Template('startHiveserver2.sh.j2'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              recursive = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hive',
        owner = 'hive',
        group = 'hadoop',
        mode = 0755,
        recursive = True,
        cd_access = 'a',
    )

  @patch("hive_service.check_fs_root")
  @patch("time.time")
  @patch("socket.socket")
  def test_socket_timeout(self, socket_mock, time_mock, check_fs_root_mock):
    s = socket_mock.return_value
    s.connect = MagicMock()    
    s.connect.side_effect = socket.error("")
    
    time_mock.side_effect = [0, 1000, 2000, 3000, 4000]
    
    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                           classname = "HiveServer",
                           command = "start",
                           config_file="default.json",
                           hdp_stack_version = self.STACK_VERSION,
                           target = RMFTestCase.TARGET_COMMON_SERVICES
      )
      
      self.fail("Script failure due to socket error was expected")
    except:
      self.assert_configure_default()
      self.assertFalse(socket_mock.called)
      self.assertFalse(s.close.called)


  @patch("hive_server.HiveServer.pre_rolling_restart")
  @patch("hive_server.HiveServer.start")
  @patch.object(shell, "call", new=MagicMock(return_value=(0,"hive-server2 - 2.2.0.0-2041")))
  def test_stop_during_upgrade(self, hive_server_start_mock,
    hive_server_pre_rolling_mock):
    
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
     classname = "HiveServer", command = "restart", config_file = "hive-upgrade.json",
     hdp_stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES )

    self.assertResourceCalled('Execute', 'hive --service hiveserver2 --deregister 2.2.0.0-2041',
      path=['/bin:/usr/hdp/current/hive-server2/bin:/usr/hdp/current/hadoop-client/bin'],
      tries=1, user='hive')

    self.assertResourceCalled('Execute', 'hdp-select set hive-server2 2.2.1.0-2065',)


  @patch("hive_server.HiveServer.pre_rolling_restart")
  @patch("hive_server.HiveServer.start")
  @patch.object(shell, "call", new=MagicMock(return_value=(0,"BAD VERSION")))
  def test_stop_during_upgrade_bad_hive_version(self, hive_server_start_mock,
    hive_server_pre_rolling_mock):
    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
       classname = "HiveServer", command = "restart", config_file = "hive-upgrade.json",
       hdp_stack_version = self.UPGRADE_STACK_VERSION,
       target = RMFTestCase.TARGET_COMMON_SERVICES )

      self.fail("Invalid hive version should have caused an exception")
    except:
      pass

    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'hive-site': {
        "hive.server2.authentication": "KERBEROS",
        "hive.metastore.sasl.enabled": "true",
        "hive.security.authorization.enabled": "true",
        "hive.server2.authentication.kerberos.keytab": "path/to/keytab",
        "hive.server2.authentication.kerberos.principal": "principal",
        "hive.server2.authentication.spnego.keytab": "path/to/spnego_keytab",
        "hive.server2.authentication.spnego.principal": "spnego_principal"
      }
    }
    result_issues = []
    props_value_check = {"hive.server2.authentication": "KERBEROS",
                         "hive.metastore.sasl.enabled": "true",
                         "hive.security.authorization.enabled": "true"}
    props_empty_check = ["hive.server2.authentication.kerberos.keytab",
                         "hive.server2.authentication.kerberos.principal",
                         "hive.server2.authentication.spnego.principal",
                         "hive.server2.authentication.spnego.keytab"]

    props_read_check = ["hive.server2.authentication.kerberos.keytab",
                        "hive.server2.authentication.spnego.keytab"]

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    get_params_mock.assert_called_with('/etc/hive/conf', {'hive-site.xml': "XML"})
    build_exp_mock.assert_called_with('hive-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['hive-env']['hive_user'],
                                                  security_params['hive-site']['hive.server2.authentication.spnego.keytab'],
                                                  security_params['hive-site']['hive.server2.authentication.spnego.principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                         classname = "HiveServer",
                         command = "security_status",
                         config_file="../../2.1/configs/secured.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains startup
    empty_security_params = {}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {}
    result_issues_with_params['hive-site']="Something bad happened"

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "security_status",
                       config_file="../../2.1/configs/secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_server.py",
                       classname = "HiveServer",
                       command = "security_status",
                       config_file="../../2.1/configs/default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})
