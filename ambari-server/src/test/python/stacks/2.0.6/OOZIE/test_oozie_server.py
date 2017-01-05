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
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
from resource_management.libraries.script.script import Script
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.libraries import functions
from resource_management.libraries.providers.hdfs_resource import WebHDFSUtil
import tempfile

@patch.object(Script, 'format_package_name', new = MagicMock())
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch.object(WebHDFSUtil, "run_command", new=MagicMock(return_value={}))
@patch.object(tempfile, "gettempdir", new=MagicMock(return_value="/tmp"))
@patch("resource_management.libraries.functions.get_user_call_output.get_user_call_output", new=MagicMock(return_value=(0, 'ext-2.2.zip', '')))
class TestOozieServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "OOZIE/4.0.0.2.0/package"
  STACK_VERSION = "2.0.6"
  UPGRADE_STACK_VERSION = "2.2"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def setUp(self):
    self.maxDiff = None

  @patch.object(shell, "call")
  @patch('os.path.exists', new=MagicMock(side_effect = [False, True, False, True]))
  def test_configure_default(self, call_mocks):
    call_mocks = MagicMock(return_value=(0, "New Oozie WAR file with added"))
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = call_mocks
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  @patch.object(shell, "call")
  @patch('os.path.exists', new=MagicMock(side_effect = [False, True, False, False, True]))
  def test_configure_default_mysql(self, call_mocks):
    call_mocks = MagicMock(return_value=(0, "New Oozie WAR file with added"))
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "configure",
                       config_file="default_oozie_mysql.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = call_mocks
    )
    self.assertResourceCalled('HdfsResource', '/user/oozie',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'oozie',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0775,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/oozie/conf',
                              mode = 0664,
                              configuration_attributes = {u'final': {u'oozie.service.CallableQueueService.queue.size': u'true',
                                                                     u'oozie.service.PurgeService.purge.interval': u'true'}},
                              owner = 'oozie',
                              configurations = self.getConfig()['configurations']['oozie-site'],
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content']),
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents=True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/oozie.conf',
                              owner = 'root',
                              group = 'root',
                              mode=0644,
                              content=Template("oozie.conf.j2"),
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              content = InlineTemplate('log4jproperties\nline2'),
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/adminusers.txt',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
                              content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/hadoop-config.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-default.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/action-conf/hive.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/var/run/oozie/oozie.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('Directory', '/usr/lib/oozie//var/tmp/oozie',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/run/oozie',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/oozie',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/tmp/oozie',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/hadoop/oozie/data',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/webapps/',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/conf',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server',
                              owner = 'oozie',
                              create_parents = True,
                              group = 'hadoop',
                              mode = 0755,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/usr/lib/oozie/libext',
                              create_parents = True,
                              )
    self.assertResourceCalled('Execute', ('tar', '-xvf', '/usr/lib/oozie/oozie-sharelib.tar.gz', '-C', '/usr/lib/oozie'),
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1' || test -f /usr/lib/oozie/.hashcode && test -d /usr/lib/oozie/share",
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('cp', '/usr/share/HDP-oozie/ext-2.2.zip', '/usr/lib/oozie/libext'),
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('chown', 'oozie:hadoop', '/usr/lib/oozie/libext/ext-2.2.zip'),
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        sudo = True,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursion_follow_links = True,
                              recursive_ownership = True,
    )
    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar'),
    )
    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/tmp/mysql-connector-java.jar',
     '/usr/lib/oozie/libext/mysql-connector-java.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/lib/oozie/libext/mysql-connector-java.jar',
        owner = 'oozie',
        group = 'hadoop',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh cp /usr/lib/falcon/oozie/ext/falcon-oozie-el-extension-*.jar /usr/lib/oozie/libext',
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh chown oozie:hadoop /usr/lib/oozie/libext/falcon-oozie-el-extension-*.jar',
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
    )

    self.assertResourceCalled('File', '/usr/lib/oozie/.prepare_war_cmd',
                              content = 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-setup.sh prepare-war',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/oozie/.war_libext_content',
                              content = 'ext-2.2.zip',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/oozie/.hashcode',
                              mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursive_ownership = True,
    )

  @patch.object(shell, "call")
  @patch('os.path.exists', new=MagicMock(side_effect = [False, True, False, False, True]))
  def test_configure_existing_sqla(self, call_mocks):
    call_mocks = MagicMock(return_value=(0, "New Oozie WAR file with added"))
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "configure",
                       config_file="oozie_existing_sqla.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = call_mocks
    )
    self.assertResourceCalled('HdfsResource', '/user/oozie',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/bin',
                              keytab = UnknownConfigurationMock(),
                              default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = UnknownConfigurationMock(),
                              user = 'hdfs',
                              dfs_type = '',
                              owner = 'oozie',
                              hadoop_conf_dir = '/etc/hadoop/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              mode = 0775,
                              )
    self.assertResourceCalled('HdfsResource', None,
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/bin',
                              keytab = UnknownConfigurationMock(),
                              default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = UnknownConfigurationMock(),
                              user = 'hdfs',
                              dfs_type = '',
                              action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hadoop_conf_dir = '/etc/hadoop/conf',
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/oozie/conf',
                              mode = 0664,
                              configuration_attributes = {u'final': {u'oozie.service.CallableQueueService.queue.size': u'true',
                                                                     u'oozie.service.PurgeService.purge.interval': u'true'}},
                              owner = 'oozie',
                              configurations = self.getConfig()['configurations']['oozie-site'],
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content']),
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents=True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/oozie.conf',
                              owner = 'root',
                              group = 'root',
                              mode=0644,
                              content=Template("oozie.conf.j2"),
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              content = InlineTemplate('log4jproperties\nline2'),
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/adminusers.txt',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/hadoop-config.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-default.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/action-conf/hive.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/var/run/oozie/oozie.pid',
                              action = ['delete'],
                              not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
                              )
    self.assertResourceCalled('Directory', '/usr/lib/oozie//var/tmp/oozie',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/run/oozie',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/oozie',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/tmp/oozie',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/hadoop/oozie/data',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/webapps/',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/conf',
                              owner = 'oozie',
                              cd_access = 'a',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server',
                              owner = 'oozie',
                              create_parents = True,
                              group = 'hadoop',
                              mode = 0755,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/usr/lib/oozie/libext',
                              create_parents = True,
                              )
    self.assertResourceCalled('Execute', ('tar', '-xvf', '/usr/lib/oozie/oozie-sharelib.tar.gz', '-C', '/usr/lib/oozie'),
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1' || test -f /usr/lib/oozie/.hashcode && test -d /usr/lib/oozie/share",
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('cp', '/usr/share/HDP-oozie/ext-2.2.zip', '/usr/lib/oozie/libext'),
                              not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
                              sudo = True,
                              )
    self.assertResourceCalled('Execute', ('chown', u'oozie:hadoop', '/usr/lib/oozie/libext/ext-2.2.zip'),
                              not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
                              sudo = True,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursion_follow_links = True,
                              recursive_ownership = True,
                              )
    self.assertResourceCalled('File', '/tmp/sqla-client-jdbc.tar.gz',
                              content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//sqla-client-jdbc.tar.gz'),
                              )
    self.assertResourceCalled('Execute', ('tar', '-xvf', '/tmp/sqla-client-jdbc.tar.gz', '-C', '/tmp'),
                              sudo = True,
                              )
    self.assertResourceCalled('Execute', 'yes | ambari-sudo.sh cp /tmp/sqla-client-jdbc/java/* /usr/lib/oozie/libext')
    self.assertResourceCalled('Directory', '/usr/lib/oozie/libext/native/lib64',
                              create_parents = True,
                              )
    self.assertResourceCalled('Execute', 'yes | ambari-sudo.sh cp /tmp/sqla-client-jdbc/native/lib64/* /usr/lib/oozie/libext/native/lib64')
    self.assertResourceCalled('Execute', 'ambari-sudo.sh chown -R oozie:hadoop /usr/lib/oozie/libext/*')
    self.assertResourceCalled('File', '/usr/lib/oozie/libext/sqla-client-jdbc.tar.gz',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh cp /usr/lib/falcon/oozie/ext/falcon-oozie-el-extension-*.jar /usr/lib/oozie/libext',
                              not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
                              )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh chown oozie:hadoop /usr/lib/oozie/libext/falcon-oozie-el-extension-*.jar',
                              not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
                              )
    self.assertResourceCalled('File', '/usr/lib/oozie/.prepare_war_cmd',
                              content = 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-setup.sh prepare-war',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/oozie/.war_libext_content',
                              content = 'ext-2.2.zip',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/oozie/.hashcode',
                              mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursive_ownership = True,
    )
    self.assertNoMoreResources()

  @patch.object(shell, "call")
  @patch("os.path.isfile")
  @patch('os.path.exists', new=MagicMock(side_effect = [False, True, False, True]))
  def test_start_default(self, isfile_mock, call_mocks):
    self._test_start(isfile_mock, call_mocks)

  def _test_start(self, isfile_mock, call_mocks):
    isfile_mock.return_value = True
    call_mocks = MagicMock(return_value=(0, "New Oozie WAR file with added"))
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "start",
                         config_file="default.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES,
                         call_mocks = call_mocks
        )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/ooziedb.sh create -sqlfile oozie.sql -run',
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        ignore_failures = True,
        user = 'oozie',
    )
    self.assertResourceCalled('Execute', 'hadoop --config /etc/hadoop/conf dfs -put /usr/lib/oozie/share /user/oozie',
        path = ['/usr/bin:/usr/bin'],
        user = 'oozie',
    )
    self.assertResourceCalled('HdfsResource', '/user/oozie/share',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        user = 'hdfs',
        dfs_type = '',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        recursive_chmod = True,
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        mode = 0755,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-start.sh',
        environment = {'OOZIE_CONFIG': '/etc/oozie/conf'},
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        user = 'oozie',
    )
    self.assertNoMoreResources()

  @patch.object(WebHDFSUtil, 'is_webhdfs_available', return_value=False)
  @patch.object(shell, "call")
  @patch("os.path.isfile")
  @patch('os.path.exists', new=MagicMock(side_effect = [False, True, False, True]))
  def test_start_no_webhdfs(self, webhdfsutil_mock, isfile_mock, call_mocks):
    self._test_start(isfile_mock, call_mocks)

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "stop",
                         config_file="default.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/tmp/oozie',
        owner = 'oozie',
        create_parents = True,
    )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozied.sh stop 60 -force',
        environment = {'OOZIE_CONFIG': '/etc/oozie/conf'},
        only_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        user = 'oozie',
    )
    self.assertResourceCalled('File', '/var/run/oozie/oozie.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  @patch.object(shell, "call")
  @patch('os.path.exists', new=MagicMock(side_effect = [False, True, False, True]))
  def test_configure_secured(self, call_mocks):
    call_mocks = MagicMock(return_value=(0, "New Oozie WAR file with added"))
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "configure",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = call_mocks
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  @patch.object(shell, "call")
  @patch('os.path.exists', new=MagicMock(side_effect = [False, True, False, True]))
  def test_configure_secured_ha(self, call_mocks):
    call_mocks = MagicMock(return_value=(0, "New Oozie WAR file with added"))

    config_file = "stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      secured_json = json.load(f)

    secured_json['configurations']['oozie-site']['oozie.ha.authentication.kerberos.principal'] = "*"
    secured_json['configurations']['oozie-site']['oozie.ha.authentication.kerberos.keytab'] = "/etc/security/keytabs/oozie_ha.keytab"

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "configure",
                       config_dict = secured_json,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = call_mocks
    )

    # Update the config data to see if
    #  * configurations/oozie-site/oozie.authentication.kerberos.principal == configurations/oozie-site/oozie.ha.authentication.kerberos.principal
    #  * configurations/oozie-site/oozie.authentication.kerberos.keytab == configurations/oozie-site/oozie.ha.authentication.kerberos.keytab
    expected_oozie_site = dict(self.getConfig()['configurations']['oozie-site'])
    expected_oozie_site['oozie.authentication.kerberos.principal'] = expected_oozie_site['oozie.ha.authentication.kerberos.principal']
    expected_oozie_site['oozie.authentication.kerberos.keytab'] = expected_oozie_site['oozie.ha.authentication.kerberos.keytab']

    self.assert_configure_secured(expected_oozie_site)
    self.assertNoMoreResources()

  @patch.object(shell, "call")
  @patch("os.path.isfile")
  @patch('os.path.exists', new=MagicMock(side_effect = [False, True, False, True]))
  def test_start_secured(self, isfile_mock, call_mocks):
    isfile_mock.return_value = True
    call_mocks = MagicMock(return_value=(0, "New Oozie WAR file with added"))
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "start",
                         config_file="secured.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES,
                         call_mocks = call_mocks
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/ooziedb.sh create -sqlfile oozie.sql -run',
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        ignore_failures = True,
        user = 'oozie',
    )

    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/oozie.service.keytab oozie/c6402.ambari.apache.org@EXAMPLE.COM;',
        user = 'oozie',
    )
    self.assertResourceCalled('Execute', 'hadoop --config /etc/hadoop/conf dfs -put /usr/lib/oozie/share /user/oozie',
        path = ['/usr/bin:/usr/bin'],
        user = 'oozie',
    )
    self.assertResourceCalled('HdfsResource', '/user/oozie/share',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        user = 'hdfs',
        dfs_type = '',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs',
        recursive_chmod = True,
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        mode = 0755,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-start.sh',
        environment = {'OOZIE_CONFIG': '/etc/oozie/conf'},
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        user = 'oozie',
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "stop",
                         config_file="secured.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Directory', '/var/tmp/oozie',
        owner = 'oozie',
        create_parents = True,
    )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozied.sh stop 60 -force',
        environment = {'OOZIE_CONFIG': '/etc/oozie/conf'},
        only_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        user = 'oozie',
    )
    self.assertResourceCalled('File', '/var/run/oozie/oozie.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('HdfsResource', '/user/oozie',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_conf_dir = '/etc/hadoop/conf',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'oozie',
        hadoop_bin_dir = '/usr/bin',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0775,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True
    )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0664,
                              conf_dir = '/etc/oozie/conf',
                              configurations = self.getConfig()['configurations']['oozie-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-env.sh',
                              owner = 'oozie',
                              content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content']),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents=True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/oozie.conf',
                              owner = 'root',
                              group = 'root',
                              mode=0644,
                              content=Template("oozie.conf.j2"),
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              content = InlineTemplate('log4jproperties\nline2')
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/adminusers.txt',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/hadoop-config.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-default.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/action-conf/hive.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/var/run/oozie/oozie.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('Directory', '/usr/lib/oozie//var/tmp/oozie',
        owner = 'oozie',
        group = 'hadoop',
        create_parents = True,
        mode = 0755,
        cd_access='a'
    )
    self.assertResourceCalled('Directory', '/var/run/oozie',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/log/oozie',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/tmp/oozie',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/hadoop/oozie/data',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/webapps/',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/usr/lib/oozie/libext',
        create_parents = True,
    )
    self.assertResourceCalled('Execute', ('tar', '-xvf', '/usr/lib/oozie/oozie-sharelib.tar.gz', '-C', '/usr/lib/oozie'),
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1' || test -f /usr/lib/oozie/.hashcode && test -d /usr/lib/oozie/share",
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('cp', '/usr/share/HDP-oozie/ext-2.2.zip', '/usr/lib/oozie/libext'),
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('chown', 'oozie:hadoop', '/usr/lib/oozie/libext/ext-2.2.zip'),
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        sudo = True,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursion_follow_links = True,
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh cp /usr/lib/falcon/oozie/ext/falcon-oozie-el-extension-*.jar /usr/lib/oozie/libext',
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh chown oozie:hadoop /usr/lib/oozie/libext/falcon-oozie-el-extension-*.jar',
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
    )

    self.assertResourceCalled('File', '/usr/lib/oozie/.prepare_war_cmd',
                              content = 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-setup.sh prepare-war',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/oozie/.war_libext_content',
                              content = 'ext-2.2.zip',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/oozie/.hashcode',
                              mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursive_ownership = True,
    )

  def assert_configure_secured(self, expected_oozie_site = None):
    self.assertResourceCalled('HdfsResource', '/user/oozie',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_conf_dir = '/etc/hadoop/conf',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'oozie',
        hadoop_bin_dir = '/usr/bin',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0775,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True
                              )

    if expected_oozie_site is None:
      expected_oozie_site = self.getConfig()['configurations']['oozie-site']

    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0664,
                              conf_dir = '/etc/oozie/conf',
                              configurations = expected_oozie_site,
                              configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-env.sh',
                              owner = 'oozie',
                              content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content']),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents=True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/oozie.conf',
                              owner = 'root',
                              group = 'root',
                              mode=0644,
                              content=Template("oozie.conf.j2"),
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              content = InlineTemplate('log4jproperties\nline2')
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/adminusers.txt',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/hadoop-config.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-default.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/action-conf/hive.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/var/run/oozie/oozie.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('Directory', '/usr/lib/oozie//var/tmp/oozie',
        owner = 'oozie',
        group = 'hadoop',
        create_parents = True,
        mode = 0755,
        cd_access='a'
    )
    self.assertResourceCalled('Directory', '/var/run/oozie',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/log/oozie',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/tmp/oozie',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/hadoop/oozie/data',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/webapps/',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server',
                              owner = 'oozie',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/usr/lib/oozie/libext',
        create_parents = True,
    )
    self.assertResourceCalled('Execute', ('tar', '-xvf', '/usr/lib/oozie/oozie-sharelib.tar.gz', '-C', '/usr/lib/oozie'),
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1' || test -f /usr/lib/oozie/.hashcode && test -d /usr/lib/oozie/share",
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('cp', '/usr/share/HDP-oozie/ext-2.2.zip', '/usr/lib/oozie/libext'),
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('chown', 'oozie:hadoop', '/usr/lib/oozie/libext/ext-2.2.zip'),
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
        sudo = True,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursion_follow_links = True,
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh cp /usr/lib/falcon/oozie/ext/falcon-oozie-el-extension-*.jar /usr/lib/oozie/libext',
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh chown oozie:hadoop /usr/lib/oozie/libext/falcon-oozie-el-extension-*.jar',
        not_if = "ambari-sudo.sh su oozie -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps -p `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1'",
    )

    self.assertResourceCalled('File', '/usr/lib/oozie/.prepare_war_cmd',
                              content = 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-setup.sh prepare-war -secure',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/oozie/.war_libext_content',
                              content = 'ext-2.2.zip',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/lib/oozie/.hashcode',
                              mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursive_ownership = True,
    )

  @patch.object(shell, "call")
  @patch('os.path.exists', new=MagicMock(side_effect = [False, True, False, True]))
  def test_configure_default_hdp22(self, call_mocks):
    call_mocks = MagicMock(return_value=(0, "New Oozie WAR file with added"))
    config_file = "stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      default_json = json.load(f)

    default_json['hostLevelParams']['stack_version']= '2.2'
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                     classname = "OozieServer",
                     command = "configure",
                     config_file="default.json",
                     stack_version = self.STACK_VERSION,
                     target = RMFTestCase.TARGET_COMMON_SERVICES,
                     call_mocks = call_mocks
    )
    self.assert_configure_default()

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters
    security_params = {
      "oozie-site": {
        "oozie.authentication.type": "kerberos",
        "oozie.service.AuthorizationService.security.enabled": "true",
        "oozie.service.HadoopAccessorService.kerberos.enabled": "true",
        "local.realm": "EXAMPLE.COM",
        "oozie.authentication.kerberos.principal": "principal",
        "oozie.authentication.kerberos.keytab": "/path/to_keytab",
        "oozie.service.HadoopAccessorService.kerberos.principal": "principal",
        "oozie.service.HadoopAccessorService.keytab.file": "/path/to_keytab"}
    }

    result_issues = []
    props_value_check = {"oozie.authentication.type": "kerberos",
                         "oozie.service.AuthorizationService.security.enabled": "true",
                         "oozie.service.HadoopAccessorService.kerberos.enabled": "true"}
    props_empty_check = [ "local.realm",
                          "oozie.authentication.kerberos.principal",
                          "oozie.authentication.kerberos.keytab",
                          "oozie.service.HadoopAccessorService.kerberos.principal",
                          "oozie.service.HadoopAccessorService.keytab.file"]
    props_read_check = None

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    get_params_mock.assert_called_with("/etc/oozie/conf", {'oozie-site.xml': 'XML'})
    build_exp_mock.assert_called_with('oozie-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['oozie-env']['oozie_user'],
                                                  security_params['oozie-site']['oozie.service.HadoopAccessorService.keytab.file'],
                                                  security_params['oozie-site']['oozie.service.HadoopAccessorService.kerberos.principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "security_status",
                         config_file="secured.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains oozie-site
    empty_security_params = {}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'oozie-site': "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "security_status",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})


  @patch("os.path.isdir")
  @patch("os.path.exists")
  @patch("os.path.isfile")
  @patch("os.remove")
  @patch("shutil.rmtree", new = MagicMock())
  @patch("glob.iglob")
  @patch("shutil.copy2", new = MagicMock())
  def test_upgrade(self, glob_mock, remove_mock,
      isfile_mock, exists_mock, isdir_mock):

    def exists_mock_side_effect(path):
      if path == '/tmp/oozie-upgrade-backup/oozie-conf-backup.tar':
        return True

      return False

    exists_mock.side_effect = exists_mock_side_effect
    isdir_mock.return_value = True
    isfile_mock.return_value = True
    glob_mock.return_value = ["/usr/hdp/2.2.1.0-2187/hadoop/lib/hadoop-lzo-0.6.0.2.2.1.0-2187.jar"]

    prepare_war_stdout = """INFO: Adding extension: libext/mysql-connector-java.jar
    New Oozie WAR file with added 'JARs' at /var/lib/oozie/oozie-server/webapps/oozie.war"""

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
     classname = "OozieServer", command = "pre_upgrade_restart", config_file = "oozie-upgrade.json",
     stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES,
     call_mocks = [(0, prepare_war_stdout)])
    
    self.assertTrue(isfile_mock.called)
    self.assertEqual(isfile_mock.call_count,2)
    isfile_mock.assert_called_with('/usr/share/HDP-oozie/ext-2.2.zip')

    self.assertTrue(glob_mock.called)
    self.assertEqual(glob_mock.call_count,1)
    glob_mock.assert_called_with('/usr/hdp/2.2.1.0-2135/hadoop/lib/hadoop-lzo*.jar')

    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'oozie-server', u'2.2.1.0-2135'),
      sudo = True )

    self.assertResourceCalled('Directory', '/usr/hdp/current/oozie-server/libext', mode = 0777)
    self.assertResourceCalled('Execute', ('cp', '/usr/share/HDP-oozie/ext-2.2.zip', '/usr/hdp/current/oozie-server/libext'), sudo=True)
    self.assertResourceCalled('Execute', ('chown', 'oozie:hadoop', '/usr/hdp/current/oozie-server/libext/ext-2.2.zip'), sudo=True)
    self.assertResourceCalled('File', '/usr/hdp/current/oozie-server/libext/ext-2.2.zip', mode = 0644)
    self.assertNoMoreResources()

  @patch("os.path.isdir")
  @patch("os.path.exists")
  @patch("os.path.isfile")
  @patch("os.remove")
  @patch("shutil.rmtree", new = MagicMock())
  @patch("glob.iglob")
  @patch("shutil.copy2", new = MagicMock())
  def test_upgrade_23(self, glob_mock, remove_mock,
      isfile_mock, exists_mock, isdir_mock):

    def exists_mock_side_effect(path):
      if path == '/tmp/oozie-upgrade-backup/oozie-conf-backup.tar':
        return True

      return False

    isdir_mock.return_value = True
    exists_mock.side_effect = exists_mock_side_effect
    isfile_mock.return_value = True
    glob_mock.return_value = ["/usr/hdp/2.2.1.0-2187/hadoop/lib/hadoop-lzo-0.6.0.2.2.1.0-2187.jar"]

    prepare_war_stdout = """INFO: Adding extension: libext/mysql-connector-java.jar
    New Oozie WAR file with added 'JARs' at /var/lib/oozie/oozie-server/webapps/oozie.war"""

    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/oozie-upgrade.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
     classname = "OozieServer", command = "pre_upgrade_restart", config_dict = json_content,
     stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES,
     call_mocks = [(0, None, ''), (0, prepare_war_stdout)],
     mocks_dict = mocks_dict)

    self.assertTrue(isfile_mock.called)
    self.assertEqual(isfile_mock.call_count,2)
    isfile_mock.assert_called_with('/usr/share/HDP-oozie/ext-2.2.zip')

    self.assertTrue(glob_mock.called)
    self.assertEqual(glob_mock.call_count,1)
    glob_mock.assert_called_with('/usr/hdp/2.3.0.0-1234/hadoop/lib/hadoop-lzo*.jar')

    self.assertResourceCalled('Link', '/etc/oozie/conf',
                              to = '/usr/hdp/current/oozie-client/conf',
    )
    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'oozie-server', '2.3.0.0-1234'), sudo = True)

    self.assertResourceCalled('Directory', '/usr/hdp/current/oozie-server/libext', mode = 0777)

    self.assertResourceCalled('Execute', ('cp', '/usr/share/HDP-oozie/ext-2.2.zip', '/usr/hdp/current/oozie-server/libext'), sudo=True)
    self.assertResourceCalled('Execute', ('chown', 'oozie:hadoop', '/usr/hdp/current/oozie-server/libext/ext-2.2.zip'), sudo=True)
    self.assertResourceCalled('File', '/usr/hdp/current/oozie-server/libext/ext-2.2.zip', mode = 0644)
    self.assertNoMoreResources()

    self.assertEquals(1, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)

    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'set-conf-dir', '--package', 'oozie', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])

    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'create-conf-dir', '--package', 'oozie', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])


  @patch("os.path.isdir")
  @patch("os.path.exists")
  @patch("os.path.isfile")
  @patch("os.remove")
  @patch("shutil.rmtree", new = MagicMock())
  @patch("shutil.copy2", new = MagicMock())
  def test_downgrade_no_compression_library_copy(self, remove_mock,
      isfile_mock, exists_mock, isdir_mock):

    isdir_mock.return_value = True
    exists_mock.return_value = False
    isfile_mock.return_value = True

    prepare_war_stdout = """INFO: Adding extension: libext/mysql-connector-java.jar
    New Oozie WAR file with added 'JARs' at /var/lib/oozie/oozie-server/webapps/oozie.war"""

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
     classname = "OozieServer", command = "pre_upgrade_restart", config_file = "oozie-downgrade.json",
     stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES,
     call_mocks = [(0, prepare_war_stdout)])

    self.assertTrue(isfile_mock.called)
    self.assertEqual(isfile_mock.call_count,1)
    isfile_mock.assert_called_with('/usr/share/HDP-oozie/ext-2.2.zip')

    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'oozie-server', u'2.2.0.0-0000'), sudo = True)

    self.assertResourceCalled('Directory', '/usr/hdp/current/oozie-server/libext',mode = 0777)
    self.assertResourceCalled('Execute', ('cp', '/usr/share/HDP-oozie/ext-2.2.zip', '/usr/hdp/current/oozie-server/libext'), sudo=True)
    self.assertResourceCalled('Execute', ('chown', 'oozie:hadoop', '/usr/hdp/current/oozie-server/libext/ext-2.2.zip'), sudo=True)
    self.assertResourceCalled('File', '/usr/hdp/current/oozie-server/libext/ext-2.2.zip',mode = 0644)
    self.assertNoMoreResources()


  def test_upgrade_database_sharelib(self):
    """
    Tests that the upgrade script runs the proper commands before the
    actual upgrade begins.
    :return:
    """
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/oozie-upgrade.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_name'] = "HDP"

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server_upgrade.py",
      classname = "OozieUpgrade", command = "upgrade_oozie_database_and_sharelib",
      config_dict = json_content,
      stack_version = self.UPGRADE_STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES )

    self.assertResourceCalled('Execute', '/usr/hdp/2.3.0.0-1234/oozie/bin/ooziedb.sh upgrade -run',
      user = 'oozie', logoutput = True )

    self.assertResourceCalled('HdfsResource', '/user/oozie/share',
      immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
      security_enabled = False,
      hadoop_bin_dir = '/usr/hdp/2.3.0.0-1234/hadoop/bin',
      keytab = UnknownConfigurationMock(),
      default_fs = 'hdfs://c6401.ambari.apache.org:8020',
      user = 'hdfs',
      dfs_type = '',
      hdfs_site = UnknownConfigurationMock(),
      kinit_path_local = '/usr/bin/kinit',
      principal_name = UnknownConfigurationMock(),
      recursive_chmod = True,
      owner = 'oozie',
      group = 'hadoop',
      hadoop_conf_dir = '/usr/hdp/2.3.0.0-1234/hadoop/conf',
      type = 'directory',
      action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
      mode = 0755 )

    self.assertResourceCalled('HdfsResource', None,
      immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
      security_enabled = False,
      hadoop_bin_dir = '/usr/hdp/2.3.0.0-1234/hadoop/bin',
      keytab = UnknownConfigurationMock(),
      default_fs = 'hdfs://c6401.ambari.apache.org:8020',
      hdfs_site = UnknownConfigurationMock(),
      kinit_path_local = '/usr/bin/kinit',
      principal_name = UnknownConfigurationMock(),
      user = 'hdfs', 
      dfs_type = '',
      action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
      hadoop_conf_dir = '/usr/hdp/2.3.0.0-1234/hadoop/conf' )

    self.assertResourceCalled('Execute', '/usr/hdp/2.3.0.0-1234/oozie/bin/oozie-setup.sh sharelib create -fs hdfs://c6401.ambari.apache.org:8020',
      user='oozie', logoutput = True)


  def test_upgrade_database_sharelib_existing_mysql(self):
    """
    Tests that the upgrade script runs the proper commands before the
    actual upgrade begins when Oozie is using and external database. This
    should ensure that the JDBC JAR is copied.
    :return:
    """
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/oozie-upgrade.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['hostLevelParams']['stack_name'] = "HDP"

    # use mysql external database
    json_content['configurations']['oozie-site']['oozie.service.JPAService.jdbc.driver'] = "com.mysql.jdbc.Driver"

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server_upgrade.py",
      classname = "OozieUpgrade", command = "upgrade_oozie_database_and_sharelib",
      config_dict = json_content,
      stack_version = self.UPGRADE_STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES )

    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
      content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar') )

    self.assertResourceCalled('Execute', ('cp', '--remove-destination', '/tmp/mysql-connector-java.jar',
      '/usr/hdp/2.3.0.0-1234/oozie/libext/mysql-connector-java.jar'),
      path = ['/bin', '/usr/bin/'], sudo = True)

    self.assertResourceCalled('File', '/usr/hdp/2.3.0.0-1234/oozie/libext/mysql-connector-java.jar',
      owner = 'oozie', group = 'hadoop' )

    self.assertResourceCalled('Execute', '/usr/hdp/2.3.0.0-1234/oozie/bin/ooziedb.sh upgrade -run',
      user = 'oozie', logoutput = True )

    self.assertResourceCalled('HdfsResource', '/user/oozie/share',
      immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
      security_enabled = False,
      hadoop_bin_dir = '/usr/hdp/2.3.0.0-1234/hadoop/bin',
      keytab = UnknownConfigurationMock(),
      default_fs = 'hdfs://c6401.ambari.apache.org:8020',
      user = 'hdfs',
      dfs_type = '',
      hdfs_site = UnknownConfigurationMock(),
      kinit_path_local = '/usr/bin/kinit',
      principal_name = UnknownConfigurationMock(),
      recursive_chmod = True,
      owner = 'oozie',
      group = 'hadoop',
      hadoop_conf_dir = '/usr/hdp/2.3.0.0-1234/hadoop/conf',
      type = 'directory',
      action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
      mode = 0755 )

    self.assertResourceCalled('HdfsResource', None,
      immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
      security_enabled = False,
      hadoop_bin_dir = '/usr/hdp/2.3.0.0-1234/hadoop/bin',
      keytab = UnknownConfigurationMock(),
      default_fs = 'hdfs://c6401.ambari.apache.org:8020',
      hdfs_site = UnknownConfigurationMock(),
      kinit_path_local = '/usr/bin/kinit',
      principal_name = UnknownConfigurationMock(),
      user = 'hdfs',
      dfs_type = '',
      action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
      hadoop_conf_dir = '/usr/hdp/2.3.0.0-1234/hadoop/conf' )

    self.assertResourceCalled('Execute', '/usr/hdp/2.3.0.0-1234/oozie/bin/oozie-setup.sh sharelib create -fs hdfs://c6401.ambari.apache.org:8020',
      user='oozie', logoutput = True)

  @patch("os.path.isdir")
  @patch("os.path.exists")
  @patch("os.path.isfile")
  @patch("os.remove")
  @patch("shutil.rmtree", new = MagicMock())
  @patch("glob.iglob")
  @patch("shutil.copy2", new = MagicMock())
  def test_upgrade_23_ensure_falcon_copied(self, glob_mock, remove_mock,
      isfile_mock, exists_mock, isdir_mock):

    def exists_mock_side_effect(path):
      if path == '/tmp/oozie-upgrade-backup/oozie-conf-backup.tar':
        return True

      return False

    isdir_mock.return_value = True
    exists_mock.side_effect = exists_mock_side_effect
    isfile_mock.return_value = True
    glob_mock.return_value = ["/usr/hdp/2.2.1.0-2187/hadoop/lib/hadoop-lzo-0.6.0.2.2.1.0-2187.jar"]

    prepare_war_stdout = """INFO: Adding extension: libext/mysql-connector-java.jar
    New Oozie WAR file with added 'JARs' at /var/lib/oozie/oozie-server/webapps/oozie.war"""

    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/oozie-upgrade.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['clusterHostInfo']['falcon_server_hosts'] = ['c6401.ambari.apache.org']

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/oozie_server.py",
     classname = "OozieServer", command = "pre_upgrade_restart", config_dict = json_content,
     stack_version = self.UPGRADE_STACK_VERSION,
     target = RMFTestCase.TARGET_COMMON_SERVICES,
     call_mocks = [(0, None, ''), (0, prepare_war_stdout)],
     mocks_dict = mocks_dict)

    self.assertTrue(isfile_mock.called)
    self.assertEqual(isfile_mock.call_count,2)
    isfile_mock.assert_called_with('/usr/share/HDP-oozie/ext-2.2.zip')

    self.assertTrue(glob_mock.called)
    self.assertEqual(glob_mock.call_count,1)
    glob_mock.assert_called_with('/usr/hdp/2.3.0.0-1234/hadoop/lib/hadoop-lzo*.jar')

    self.assertResourceCalled('Link', '/etc/oozie/conf', to = '/usr/hdp/current/oozie-client/conf')
    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'oozie-server', '2.3.0.0-1234'), sudo = True)

    self.assertResourceCalled('Directory', '/usr/hdp/current/oozie-server/libext', mode = 0777)

    self.assertResourceCalled('Execute', ('cp', '/usr/share/HDP-oozie/ext-2.2.zip', '/usr/hdp/current/oozie-server/libext'), sudo=True)
    self.assertResourceCalled('Execute', ('chown', 'oozie:hadoop', '/usr/hdp/current/oozie-server/libext/ext-2.2.zip'), sudo=True)

    self.assertResourceCalled('File', '/usr/hdp/current/oozie-server/libext/ext-2.2.zip',
        mode = 0644,
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh cp /usr/hdp/2.3.0.0-1234/falcon/oozie/ext/falcon-oozie-el-extension-*.jar /usr/hdp/current/oozie-server/libext')
    self.assertResourceCalled('Execute', 'ambari-sudo.sh chown oozie:hadoop /usr/hdp/current/oozie-server/libext/falcon-oozie-el-extension-*.jar')

    self.assertNoMoreResources()

    self.assertEquals(1, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)

    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'set-conf-dir', '--package', 'oozie', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])

    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'create-conf-dir', '--package', 'oozie', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])
