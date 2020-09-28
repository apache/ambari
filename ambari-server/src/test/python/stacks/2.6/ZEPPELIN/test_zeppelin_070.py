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
from ambari_commons import OSCheck
'''

import glob
import unittest

from mock.mock import MagicMock, patch
from resource_management.core import sudo
from stacks.utils.RMFTestCase import *

import interpreter_json_generated


@patch.object(glob, "glob", new=MagicMock(return_value=["/tmp"]))
@patch.object(sudo, "read_file",
              new=MagicMock(return_value=interpreter_json_generated.template))
class TestZeppelin070(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "ZEPPELIN/0.7.0/package"
  STACK_VERSION = "2.6"

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/log/zeppelin',
                              owner='zeppelin',
                              group='zeppelin',
                              create_parents=True,
                              mode=0755,
                              cd_access='a',
                              )
    self.assertResourceCalled('Directory', '/var/run/zeppelin',
                              owner='zeppelin',
                              create_parents=True,
                              group='zeppelin',
                              mode=0755,
                              cd_access='a',
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/zeppelin-server',
                              owner='zeppelin',
                              group='zeppelin',
                              create_parents=True,
                              mode=0755,
                              cd_access='a',
                              )
    self.assertResourceCalled('Execute', (
      'chown', '-R', u'zeppelin:zeppelin', '/var/run/zeppelin'), sudo=True)
    self.assertResourceCalled('XmlConfig', 'zeppelin-site.xml',
                              owner='zeppelin',
                              group='zeppelin',
                              conf_dir='/etc/zeppelin/conf',
                              configurations=self.getConfig()['configurations'][
                                'zeppelin-config'],
                              )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/zeppelin-env.sh',
                              owner='zeppelin',
                              content=InlineTemplate(
                                self.getConfig()['configurations'][
                                  'zeppelin-env']['zeppelin_env_content']),
                              group='zeppelin',
                              )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/shiro.ini',
                              owner='zeppelin',
                              content=InlineTemplate(
                                self.getConfig()['configurations'][
                                  'zeppelin-shiro-ini']['shiro_ini_content']),
                              group='zeppelin',
                              )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/log4j.properties',
                              owner=u'zeppelin',
                              content=u'log4j.rootLogger = INFO, dailyfile',
                              group=u'zeppelin',
                              )
    self.assertResourceCalled('Directory',
                              '/etc/zeppelin/conf/external-dependency-conf',
                              owner='zeppelin',
                              group='zeppelin',
                              create_parents=True,
                              mode=0755,
                              cd_access='a',
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/var/log/zeppelin',
                              owner='zeppelin',
                              group='zeppelin',
                              create_parents=True,
                              mode=0755,
                              cd_access='a',
                              )
    self.assertResourceCalled('Directory', '/var/run/zeppelin',
                              owner='zeppelin',
                              create_parents=True,
                              group='zeppelin',
                              mode=0755,
                              cd_access='a',
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/zeppelin-server',
                              owner='zeppelin',
                              group='zeppelin',
                              create_parents=True,
                              mode=0755,
                              cd_access='a',
                              )
    self.assertResourceCalled('Execute', (
      'chown', '-R', u'zeppelin:zeppelin', '/var/run/zeppelin'), sudo=True)
    self.assertResourceCalled('XmlConfig', 'zeppelin-site.xml',
                              owner='zeppelin',
                              group='zeppelin',
                              conf_dir='/etc/zeppelin/conf',
                              configurations=self.getConfig()['configurations'][
                                'zeppelin-config'],
                              )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/zeppelin-env.sh',
                              owner='zeppelin',
                              content=InlineTemplate(
                                self.getConfig()['configurations'][
                                  'zeppelin-env']['zeppelin_env_content']),
                              group='zeppelin',
                              )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/shiro.ini',
                              owner='zeppelin',
                              content=InlineTemplate(
                                self.getConfig()['configurations'][
                                  'zeppelin-shiro-ini']['shiro_ini_content']),
                              group='zeppelin',
                              )
    self.assertResourceCalled('File', '/etc/zeppelin/conf/log4j.properties',
                              owner=u'zeppelin',
                              content=u'log4j.rootLogger = INFO, dailyfile',
                              group=u'zeppelin',
                              )
    self.assertResourceCalled('Directory',
                              '/etc/zeppelin/conf/external-dependency-conf',
                              owner='zeppelin',
                              group='zeppelin',
                              create_parents=True,
                              mode=0755,
                              cd_access='a',
                              )

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname="Master",
                       command="configure",
                       config_file="default.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname="Master",
                       command="stop",
                       config_file="default.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assertResourceCalled('Directory', '/var/log/zeppelin',
                              owner='zeppelin',
                              group='zeppelin',
                              create_parents=True,
                              mode=0755,
                              cd_access='a',
                              )
    self.assertResourceCalled('Execute', (
      'chown', '-R', u'zeppelin:zeppelin', '/var/run/zeppelin'),
                              sudo=True,
                              )
    self.assertResourceCalled('Execute',
                              '/usr/hdp/current/zeppelin-server/bin/zeppelin-daemon.sh stop >> /var/log/zeppelin/zeppelin-setup.log',
                              user='zeppelin',
                              )
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname="Master",
                       command="start",
                       config_file="default.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', (
      'chown', '-R', u'zeppelin:zeppelin', '/etc/zeppelin'),
                              sudo=True,
                              )

  @patch('os.path.exists', return_value=True)
  @unittest.skip("Disabled for stabilization, check AMBARI-25561")
  def test_start_secured(self, os_path_exists_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/master.py",
                       classname="Master",
                       command="start",
                       config_file="default.json",
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', (
      'chown', '-R', u'zeppelin:zeppelin', '/etc/zeppelin'),
                              sudo=True,
                              )
    self.assertResourceCalled('Execute', ('chown', '-R', 'zeppelin:zeppelin',
                                          '/usr/hdp/current/zeppelin-server/notebook'),
                              sudo=True,
                              )
    self.assertResourceCalled('HdfsResource', '/user/zeppelin/notebook',
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              default_fs=u'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              keytab=UnknownConfigurationMock(),
                              kinit_path_local='/usr/bin/kinit',
                              user='hdfs',
                              owner='zeppelin',
                              principal_name=UnknownConfigurationMock(),
                              recursive_chown=True,
                              security_enabled=False,
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='directory',
                              action=['create_on_execute'],
                              recursive_chmod=True,
                              dfs_type='',
                              )
    self.assertResourceCalled('HdfsResource', '/user/zeppelin/notebook',
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              default_fs=u'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              keytab=UnknownConfigurationMock(),
                              kinit_path_local='/usr/bin/kinit',
                              user='hdfs',
                              owner='zeppelin',
                              principal_name=UnknownConfigurationMock(),
                              recursive_chown=True,
                              security_enabled=False,
                              source='/usr/hdp/current/zeppelin-server/notebook',
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='directory',
                              action=['create_on_execute'],
                              recursive_chmod=True,
                              dfs_type='',
                              )

    self.assertResourceCalled('HdfsResource', '/user/zeppelin',
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              default_fs=u'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              keytab=UnknownConfigurationMock(),
                              kinit_path_local='/usr/bin/kinit',
                              user='hdfs',
                              owner='zeppelin',
                              principal_name=UnknownConfigurationMock(),
                              recursive_chown=True,
                              security_enabled=False,
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='directory',
                              action=['create_on_execute'],
                              recursive_chmod=True,
                              dfs_type='',
                              )
    self.assertResourceCalled('HdfsResource', '/user/zeppelin/test',
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              default_fs=u'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              kinit_path_local='/usr/bin/kinit',
                              user='hdfs',
                              owner='zeppelin',
                              recursive_chown=True,
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='directory',
                              action=['create_on_execute'],
                              recursive_chmod=True,
                              keytab=UnknownConfigurationMock(),
                              principal_name=UnknownConfigurationMock(),
                              security_enabled=False,
                              dfs_type='',
                              )
    self.assertResourceCalled('HdfsResource', '/apps/zeppelin',
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              default_fs=u'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              kinit_path_local='/usr/bin/kinit',
                              user='hdfs',
                              owner='zeppelin',
                              recursive_chown=True,
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='directory',
                              action=['create_on_execute'],
                              recursive_chmod=True,
                              keytab=UnknownConfigurationMock(),
                              principal_name=UnknownConfigurationMock(),
                              security_enabled=False,
                              dfs_type='',
                              )
    self.assertResourceCalled('HdfsResource', '/apps/zeppelin/tmp',
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              source='/tmp',
                              default_fs=u'hdfs://c6401.ambari.apache.org:8020',
                              replace_existing_files=True,
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              kinit_path_local='/usr/bin/kinit',
                              user='hdfs',
                              owner='zeppelin',
                              group='zeppelin',
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='file',
                              action=['create_on_execute'],
                              mode=0444,
                              keytab=UnknownConfigurationMock(),
                              principal_name=UnknownConfigurationMock(),
                              security_enabled=False,
                              dfs_type='',
                              )
    self.assertResourceCalled('HdfsResource', None,
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              default_fs=u'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              kinit_path_local='/usr/bin/kinit',
                              user='hdfs',
                              action=['execute'],
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              keytab=UnknownConfigurationMock(),
                              principal_name=UnknownConfigurationMock(),
                              security_enabled=False,
                              dfs_type='',
                              )

    self.assertResourceCalled('HdfsResource',
                              'hdfs:///user/zeppelin/conf',
                              security_enabled=False,
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              keytab=UnknownConfigurationMock(),
                              default_fs='hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              kinit_path_local='/usr/bin/kinit',
                              principal_name=UnknownConfigurationMock(),
                              recursive_chown=True,
                              recursive_chmod=True,
                              user='hdfs',
                              owner='zeppelin',
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='directory',
                              action=['create_on_execute'],
                              dfs_type='',
                              )

    self.assertResourceCalled('File', '/etc/zeppelin/conf/interpreter.json',
                          content=interpreter_json_generated.template_after_base,
                          owner='zeppelin',
                          group='zeppelin',
                          mode=0644
                          )

    self.assertResourceCalled('HdfsResource',
                              'hdfs:///user/zeppelin/conf/interpreter.json',
                              security_enabled=False,
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              keytab=UnknownConfigurationMock(),
                              default_fs='hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              kinit_path_local='/usr/bin/kinit',
                              principal_name=UnknownConfigurationMock(),
                              user='hdfs',
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='file',
                              action=['delete_on_execute'],
                              dfs_type='',
                              )

    self.assertResourceCalled('HdfsResource',
                              'hdfs:///user/zeppelin/conf/interpreter.json',
                              security_enabled=False,
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              keytab=UnknownConfigurationMock(),
                              source='/etc/zeppelin/conf/interpreter.json',
                              default_fs='hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              kinit_path_local='/usr/bin/kinit',
                              principal_name=UnknownConfigurationMock(),
                              recursive_chown=True,
                              recursive_chmod=True,
                              user='hdfs',
                              owner='zeppelin',
                              replace_existing_files=True,
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='file',
                              action=['create_on_execute'],
                              dfs_type='',
                              )

    self.assertResourceCalled('File', '/etc/zeppelin/conf/interpreter.json',
                              content=interpreter_json_generated.template_after_without_spark_and_livy,
                              owner='zeppelin',
                              group='zeppelin',
                              mode=0644
                              )

    self.assertResourceCalled('HdfsResource',
                              'hdfs:///user/zeppelin/conf/interpreter.json',
                              security_enabled=False,
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              keytab=UnknownConfigurationMock(),
                              default_fs='hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              kinit_path_local='/usr/bin/kinit',
                              principal_name=UnknownConfigurationMock(),
                              user='hdfs',
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='file',
                              action=['delete_on_execute'],
                              dfs_type='',
                              )

    self.assertResourceCalled('HdfsResource',
                              'hdfs:///user/zeppelin/conf/interpreter.json',
                              security_enabled=False,
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              keytab=UnknownConfigurationMock(),
                              source='/etc/zeppelin/conf/interpreter.json',
                              default_fs='hdfs://c6401.ambari.apache.org:8020',
                              replace_existing_files=True,
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              kinit_path_local='/usr/bin/kinit',
                              principal_name=UnknownConfigurationMock(),
                              recursive_chmod=True,
                              recursive_chown=True,
                              user='hdfs',
                              owner='zeppelin',
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='file',
                              action=['create_on_execute'],
                              dfs_type='',
                              )

    self.assertResourceCalled('File', '/etc/zeppelin/conf/interpreter.json',
                              content=interpreter_json_generated.template_after_kerberos,
                              owner='zeppelin',
                              group='zeppelin',
                              mode=0644
                              )

    self.assertResourceCalled('HdfsResource',
                              'hdfs:///user/zeppelin/conf/interpreter.json',
                              security_enabled=False,
                              hadoop_bin_dir='/usr/hdp/2.5.0.0-1235/hadoop/bin',
                              keytab=UnknownConfigurationMock(),
                              default_fs='hdfs://c6401.ambari.apache.org:8020',
                              hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hdfs_site={u'a': u'b'},
                              kinit_path_local='/usr/bin/kinit',
                              principal_name=UnknownConfigurationMock(),
                              user='hdfs',
                              hadoop_conf_dir='/usr/hdp/2.5.0.0-1235/hadoop/conf',
                              type='file',
                              action=['delete_on_execute'],
                              dfs_type='',
                              )

    self.assertResourceCalled('HdfsResource', 'hdfs:///user/zeppelin/conf/interpreter.json',
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/2.5.0.0-1235/hadoop/bin',
        keytab = UnknownConfigurationMock(),
        source = '/etc/zeppelin/conf/interpreter.json',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_resource_ignore_file = '/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hdfs_site = {u'a': u'b'},
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        replace_existing_files = True,
        recursive_chown=True,
        recursive_chmod=True,
        user = 'hdfs',
        owner = 'zeppelin',
        hadoop_conf_dir = '/usr/hdp/2.5.0.0-1235/hadoop/conf',
        type = 'file',
        action = ['create_on_execute'],
        dfs_type='',
    )

    self.assertResourceCalled('Execute',
                              '/usr/hdp/current/zeppelin-server/bin/zeppelin-daemon.sh restart >> /var/log/zeppelin/zeppelin-setup.log',
                              user='zeppelin'
                              )

    self.assertNoMoreResources()
