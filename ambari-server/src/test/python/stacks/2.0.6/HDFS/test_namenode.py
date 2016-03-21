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
import json
import os
import tempfile
import time
from stacks.utils.RMFTestCase import *
from mock.mock import MagicMock, patch, call
from resource_management.libraries.script.script import Script
from resource_management.core import shell
from resource_management.core.exceptions import Fail


class TestNamenode(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HDFS/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "configure",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default_alt_fs(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file = "altfs_plus_hdfs.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0,"")],
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'ls /hadoop/hdfs/namenode | wc -l  | grep -q ^0$',)
    self.assertResourceCalled('Execute', 'yes Y | hdfs --config /etc/hadoop/conf namenode -format',
                              path = ['/usr/bin'],
                              user = 'hdfs',
                              )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/namenode/namenode-formatted/',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "hdfs dfsadmin -fs hdfs://c6405.ambari.apache.org:8020 -safemode get | grep 'Safe mode is OFF'",
        tries=115,
        try_sleep=10,
        user="hdfs",
        logoutput=True
    )
    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if=True,
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'wasb://abc@c6401.ambari.apache.org',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if=True,
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'wasb://abc@c6401.ambari.apache.org',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0770,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if=True,
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'wasb://abc@c6401.ambari.apache.org',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()

  def test_install_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "install",
                       config_file = "default_no_install.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       try_install=True
    )
    self.assert_configure_default()
    self.assertNoMoreResources()
    pass

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0,"")],
    )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'ls /hadoop/hdfs/namenode | wc -l  | grep -q ^0$',)
    self.assertResourceCalled('Execute', 'yes Y | hdfs --config /etc/hadoop/conf namenode -format',
        path = ['/usr/bin'],
        user = 'hdfs',
    )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/namenode/namenode-formatted/',
        recursive = True,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -safemode get | grep 'Safe mode is OFF'",
        tries=115,
        try_sleep=10,
        user="hdfs",
        logoutput=True
    )
    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = True,
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = True,
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0770,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = True,
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "stop",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop namenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid")
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',action = ['delete'])
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "configure",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()


  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0,"")],
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'ls /hadoop/hdfs/namenode | wc -l  | grep -q ^0$',)
    self.assertResourceCalled('Execute', 'yes Y | hdfs --config /etc/hadoop/conf namenode -format',
        path = ['/usr/bin'],
        user = 'hdfs',
    )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/namenode/namenode-formatted/',
        recursive = True,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs',
                              user='hdfs',
                              )
    self.assertResourceCalled('Execute', "hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -safemode get | grep 'Safe mode is OFF'",
        tries=115,
        try_sleep=10,
        user="hdfs",
        logoutput=True
    )
    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0777,
        only_if = True
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0770,
        only_if = True
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        only_if = True,
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        hadoop_bin_dir = '/usr/bin',
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "stop",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop namenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid")
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',action = ['delete'])
    self.assertNoMoreResources()

  def test_start_ha_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file = "ha_default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -safemode get | grep 'Safe mode is OFF'",
        tries=115,
        try_sleep=10,
        user="hdfs",
        logoutput=True
    )
    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0770,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()

  @patch.object(shell, "call")
  @patch.object(time, "sleep")
  def test_start_ha_default_active_with_retry(self, sleep_mock, call_mocks):
    call_mocks = MagicMock()
    call_mocks.side_effect = [(1, None), (1, None), (1, None), (1, None), (0, None)]

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file = "ha_default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = call_mocks
    )
    self.assert_configure_default()
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -safemode get | grep 'Safe mode is OFF'",
        tries=115,
        try_sleep=10,
        user="hdfs",
        logoutput=True
    )
    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0770,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()
    self.assertTrue(call_mocks.called)
    self.assertEqual(5, call_mocks.call_count)
    calls = [
        call("ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'"),
        call("ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn2 | grep active'"),
        call("ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'"),
        call("ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn2 | grep active'"),
        call("ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'")]
    call_mocks.assert_has_calls(calls)

  def test_start_ha_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file = "ha_secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs',
        user = 'hdfs',
    )
    self.assertResourceCalled('Execute', "hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -safemode get | grep 'Safe mode is OFF'",
        tries=115,
        try_sleep=10,
        user="hdfs",
        logoutput=True
    )
    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0770,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()

  # tests namenode start command when NameNode HA is enabled, and
  # the HA cluster is started initially, rather than using the UI Wizard
  def test_start_ha_bootstrap_active_from_blueprint(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file="ha_bootstrap_active_node.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()

    # verify that active namenode was formatted
    self.assertResourceCalled('Execute', 'ls /hadoop/hdfs/namenode | wc -l  | grep -q ^0$',)
    self.assertResourceCalled('Execute', 'yes Y | hdfs --config /etc/hadoop/conf namenode -format',
        path = ['/usr/bin'],
        user = 'hdfs',
    )
    self.assertResourceCalled('Directory', '/hadoop/hdfs/namenode/namenode-formatted/',
        recursive = True,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -safemode get | grep 'Safe mode is OFF'",
        tries=115,
        try_sleep=10,
        user="hdfs",
        logoutput=True
    )
    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0770,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn1 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()

  # tests namenode start command when NameNode HA is enabled, and
  # the HA cluster is started initially, rather than using the UI Wizard
  # this test verifies the startup of a "standby" namenode
  @patch.object(shell, "call")
  def test_start_ha_bootstrap_standby_from_blueprint(self, call_mocks):
    call_mocks = MagicMock(return_value=(0,""))
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file="ha_bootstrap_standby_node.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = call_mocks
    )
    self.assert_configure_default()

    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
    )

    # TODO: Using shell.call() to bootstrap standby which is patched to return status code '5' (i.e. already bootstrapped)
    # Need to update the test case to verify that the standby case is detected, and that the bootstrap
    # command is run before the namenode launches
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
    )
    self.assertResourceCalled('Execute', "hdfs dfsadmin -fs hdfs://c6402.ambari.apache.org:8020 -safemode get | grep 'Safe mode is OFF'",
        tries=115,
        try_sleep=10,
        user="hdfs",
        logoutput=True
    )
    self.assertResourceCalled('HdfsResource', '/tmp',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn2 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'hdfs',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0777,
    )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn2 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0770,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn2 | grep active'",
        keytab = UnknownConfigurationMock(),
        hadoop_bin_dir = '/usr/bin',
        default_fs = 'hdfs://ns1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = None,
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()
    self.assertTrue(call_mocks.called)
    self.assertEqual(2, call_mocks.call_count)
    calls = [
      call('hdfs namenode -bootstrapStandby -nonInteractive', logoutput=False, user=u'hdfs'),
      call("ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn2 | grep active'")]
    call_mocks.assert_has_calls(calls, any_order=False)

  # tests namenode start command when NameNode HA is enabled, and
  # the HA cluster is started initially, rather than using the UI Wizard
  # this test verifies the startup of a "standby" namenode
  @patch.object(shell, "call")
  def test_start_ha_bootstrap_standby_from_blueprint_initial_start(self, call_mocks):

    call_mocks = MagicMock()
    call_mocks.side_effect = [(1, None), (0, None), (0, None)]
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       config_file="ha_bootstrap_standby_node_initial_start.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = call_mocks
    )
    self.assert_configure_default()

    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
    )

    # TODO: Using shell.call() to bootstrap standby which is patched to return status code '5' (i.e. already bootstrapped)
    # Need to update the test case to verify that the standby case is detected, and that the bootstrap
    # command is run before the namenode launches
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid',
                              action = ['delete'],
                              not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
                              )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start namenode'",
                              environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
                              not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-namenode.pid",
                              )
    self.assertResourceCalled('Execute', "hdfs dfsadmin -fs hdfs://c6402.ambari.apache.org:8020 -safemode get | grep 'Safe mode is OFF'",
                              tries=115,
                              try_sleep=10,
                              user="hdfs",
                              logoutput=True
    )
    self.assertResourceCalled('HdfsResource', '/tmp',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn2 | grep active'",
                              keytab = UnknownConfigurationMock(),
                              hadoop_bin_dir = '/usr/bin',
                              default_fs = 'hdfs://ns1',
                              hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = None,
                              user = 'hdfs',
                              dfs_type = '',
                              owner = 'hdfs',
                              hadoop_conf_dir = '/etc/hadoop/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              mode = 0777,
                              )
    self.assertResourceCalled('HdfsResource', '/user/ambari-qa',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn2 | grep active'",
                              keytab = UnknownConfigurationMock(),
                              hadoop_bin_dir = '/usr/bin',
                              default_fs = 'hdfs://ns1',
                              hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = None,
                              user = 'hdfs',
                              dfs_type = '',
                              owner = 'ambari-qa',
                              hadoop_conf_dir = '/etc/hadoop/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              mode = 0770,
                              )
    self.assertResourceCalled('HdfsResource', None,
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              only_if = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn2 | grep active'",
                              keytab = UnknownConfigurationMock(),
                              hadoop_bin_dir = '/usr/bin',
                              default_fs = 'hdfs://ns1',
                              hdfs_site = self.getConfig()['configurations']['hdfs-site'],
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = None,
                              user = 'hdfs',
                              dfs_type = '',
                              action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              hadoop_conf_dir = '/etc/hadoop/conf',
                              )
    self.assertNoMoreResources()
    self.assertTrue(call_mocks.called)
    self.assertEqual(3, call_mocks.call_count)
    calls = [
      call("ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf haadmin -getServiceState nn2 | grep active'"),
      call('hdfs namenode -bootstrapStandby -nonInteractive -force', logoutput=False, user=u'hdfs'),
      call('hdfs namenode -bootstrapStandby -nonInteractive -force', logoutput=False, user=u'hdfs')]
    call_mocks.assert_has_calls(calls, any_order=True)

  def test_decommission_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "decommission",
                       config_file = "default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Execute', '', user = 'hdfs')
    self.assertResourceCalled('ExecuteHadoop', 'dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -refreshNodes',
                              user = 'hdfs',
                              conf_dir = '/etc/hadoop/conf',
                              bin_dir = '/usr/bin',
                              kinit_override = True)
    self.assertNoMoreResources()

  def test_decommission_update_exclude_file_only(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "decommission",
                       config_file = "default_update_exclude_file_only.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()


  def test_decommission_ha_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "decommission",
                       config_file = "ha_default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
                              owner = 'hdfs',
                              content = Template('exclude_hosts_list.j2'),
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Execute', '', user = 'hdfs')
    self.assertResourceCalled('ExecuteHadoop', 'dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -refreshNodes',
                              user = 'hdfs',
                              conf_dir = '/etc/hadoop/conf',
                              bin_dir = '/usr/bin',
                              kinit_override = True)
    self.assertNoMoreResources()


  def test_decommission_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "decommission",
                       config_file = "secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/dfs.exclude',
        owner = 'hdfs',
        content = Template('exclude_hosts_list.j2'),
        group = 'hadoop',
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/nn.service.keytab nn/c6401.ambari.apache.org@EXAMPLE.COM;',
        user = 'hdfs',
    )
    self.assertResourceCalled('ExecuteHadoop', 'dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -refreshNodes',
        bin_dir = '/usr/bin',
        conf_dir = '/etc/hadoop/conf',
        kinit_override = True,
        user = 'hdfs',
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-i386-32',
        recursive = True,
    )
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-amd64-64',
        recursive = True,
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-i386-32/libsnappy.so',
        to = '/usr/lib/hadoop/lib/libsnappy.so',
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-amd64-64/libsnappy.so',
        to = '/usr/lib/hadoop/lib64/libsnappy.so',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hdfs.conf',
                              content = Template('hdfs.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
                              )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
                              mode = 0644
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/slaves',
                              content = Template('slaves.j2'),
                              owner = 'hdfs',
                              )

    self.assertResourceCalled('Directory', '/hadoop/hdfs/namenode',
                              owner = 'hdfs',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              cd_access='a'
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-i386-32',
        recursive = True,
    )
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-amd64-64',
        recursive = True,
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-i386-32/libsnappy.so',
        to = '/usr/lib/hadoop/lib/libsnappy.so',
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/native/Linux-amd64-64/libsnappy.so',
        to = '/usr/lib/hadoop/lib64/libsnappy.so',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hdfs.conf',
                              content = Template('hdfs.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
                              )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['core-site'],
                              mode = 0644
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/slaves',
                              content = Template('slaves.j2'),
                              owner = 'root',
                              )

    self.assertResourceCalled('Directory', '/hadoop/hdfs/namenode',
                              owner = 'hdfs',
                              group = 'hadoop',
                              recursive = True,
                              mode = 0755,
                              cd_access='a'
                              )

  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_rebalance_hdfs(self, pso):
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                         classname = "NameNode",
                         command = "rebalancehdfs",
                         config_file = "rebalancehdfs_default.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
      self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin ; hdfs --config /etc/hadoop/conf balancer -threshold -1'",
          logoutput = False,
          on_new_line = FunctionMock('handle_new_line'),
      )
      self.assertNoMoreResources()

  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("os.system")
  def test_rebalance_secured_hdfs(self, pso, system_mock):

    system_mock.return_value = -1
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "rebalancehdfs",
                       config_file = "rebalancehdfs_secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks=[(1, "no kinit")]
    )
    tempdir = tempfile.gettempdir()
    ccache_path =  os.path.join(tempfile.gettempdir(), "hdfs_rebalance_cc_7add60ca651f1bd1ed909a6668937ba9")
    kinit_cmd = "/usr/bin/kinit -c {0} -kt /etc/security/keytabs/hdfs.headless.keytab hdfs@EXAMPLE.COM".format(ccache_path)
    rebalance_cmd = "ambari-sudo.sh su hdfs -l -s /bin/bash -c 'export  PATH=/bin:/usr/bin KRB5CCNAME={0} ; hdfs --config /etc/hadoop/conf balancer -threshold -1'".format(ccache_path)

    self.assertResourceCalled('Execute', kinit_cmd,
                              user = 'hdfs',
                              )
    self.assertResourceCalled('Execute', rebalance_cmd,
                              logoutput = False,
                              on_new_line = FunctionMock('handle_new_line'),
                              )
    self.assertResourceCalled('File', ccache_path,
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  @patch("os.path.isfile")
  def test_ranger_installed_missing_file(self, isfile_mock):
    """
    Tests that when Ranger is enabled for HDFS, that an exception is thrown
    if there is no install.properties found
    :return:
    """
    isfile_mock.return_value = False

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
        classname = "NameNode", command = "start", config_file = "ranger-namenode-start.json",
        hdp_stack_version = self.STACK_VERSION, target = RMFTestCase.TARGET_COMMON_SERVICES )

      self.fail("Expected a failure since the ranger install.properties was missing")
    except Fail, failure:
      pass

    self.assertTrue(isfile_mock.called)

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'core-site': {
        'hadoop.security.authentication': 'kerberos'
      },
      'hdfs-site': {
        'dfs.namenode.keytab.file': 'path/to/namenode/keytab/file',
        'dfs.namenode.kerberos.principal': 'namenode_principal'
      }
    }
    props_value_check = None
    props_empty_check = ['dfs.namenode.kerberos.internal.spnego.principal',
                       'dfs.namenode.keytab.file',
                       'dfs.namenode.kerberos.principal']
    props_read_check = ['dfs.namenode.keytab.file']

    result_issues = []

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('hdfs-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    cached_kinit_executor_mock.called_with('/usr/bin/kinit',
                                           self.config_dict['configurations']['hadoop-env']['hdfs_user'],
                                           security_params['hdfs-site']['dfs.namenode.keytab.file'],
                                           security_params['hdfs-site']['dfs.namenode.kerberos.principal'],
                                           self.config_dict['hostname'],
                                           '/tmp')

    # Testing when hadoop.security.authentication is simple
    security_params['core-site']['hadoop.security.authentication'] = 'simple'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})
    security_params['core-site']['hadoop.security.authentication'] = 'kerberos'

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                         classname = "NameNode",
                         command = "security_status",
                         config_file="secured.json",
                         hdp_stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains hdfs-site
    empty_security_params = {
      'core-site': {
        'hadoop.security.authentication': 'kerberos'
      }
    }
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'hdfs-site': "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "security_status",
                       config_file="secured.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})


  @patch("utils.get_namenode_states")
  def test_upgrade_restart(self, get_namenode_states_mock):
    #   Execution of nn_ru_lzo invokes a code path that invokes lzo installation, which
    #   was failing in RU case.  See hdfs.py and the lzo_enabled check that is in it.
    #   Just executing the script is enough to test the fix
    active_namenodes = [('nn1', 'c6401.ambari.apache.org:50070')]
    standby_namenodes = [('nn2', 'c6402.ambari.apache.org:50070')]
    unknown_namenodes = []

    get_namenode_states_mock.return_value = active_namenodes, standby_namenodes, unknown_namenodes
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "restart",
                       config_file = "nn_ru_lzo.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    unknown_namenodes = active_namenodes
    active_namenodes = []
    get_namenode_states_mock.return_value = active_namenodes, standby_namenodes, unknown_namenodes
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                     classname = "NameNode",
                     command = "restart",
                     config_file = "nn_ru_lzo.json",
                     hdp_stack_version = self.STACK_VERSION,
                     target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertFalse(0 == len(Script.structuredOut))
    self.assertTrue(Script.structuredOut.has_key("upgrade_type"))
    self.assertTrue(Script.structuredOut.has_key("direction"))
    self.assertEquals("rolling_upgrade", Script.structuredOut["upgrade_type"])
    self.assertEquals("UPGRADE", Script.structuredOut["direction"])


  @patch("utils.get_namenode_states")
  def test_upgrade_restart_eu(self, get_namenode_states_mock):
    active_namenodes = [('nn1', 'c6401.ambari.apache.org:50070')]
    standby_namenodes = [('nn2', 'c6402.ambari.apache.org:50070')]
    unknown_namenodes = []

    mocks_dict = {}
    get_namenode_states_mock.return_value = active_namenodes, standby_namenodes, unknown_namenodes
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "restart",
                       config_file = "nn_eu_standby.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict=mocks_dict)
    
    calls = mocks_dict['call'].call_args_list
    self.assertTrue(len(calls) >= 1)
    self.assertTrue(calls[0].startsWith("conf-select create-conf-dir --package hadoop --stack-version 2.3.2.0-2844 --conf-version 0"))


  @patch("hdfs_namenode.is_active_namenode")
  @patch("resource_management.libraries.functions.setup_ranger_plugin_xml.setup_ranger_plugin")
  @patch("utils.get_namenode_states")
  def test_upgrade_restart_eu_with_ranger(self, get_namenode_states_mock, setup_ranger_plugin_mock, is_active_nn_mock):
    is_active_nn_mock.return_value = True

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/nn_eu.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.4.0-1111'
    json_content['commandParams']['version'] = version

    active_namenodes = [('nn1', 'c6401.ambari.apache.org:50070')]
    standby_namenodes = [('nn2', 'c6402.ambari.apache.org:50070')]
    unknown_namenodes = []

    mocks_dict = {}
    get_namenode_states_mock.return_value = active_namenodes, standby_namenodes, unknown_namenodes
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "start",
                       command_args=["nonrolling"],
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict=mocks_dict)

    self.assertTrue(setup_ranger_plugin_mock.called)

    self.assertResourceCalledByIndex(7, 'Execute',
      ('mv', '/usr/hdp/2.3.4.0-1111/hadoop/conf/set-hdfs-plugin-env.sh', '/usr/hdp/2.3.4.0-1111/hadoop/conf/set-hdfs-plugin-env.sh.bak'),
      only_if='test -f /usr/hdp/2.3.4.0-1111/hadoop/conf/set-hdfs-plugin-env.sh',
      sudo=True)

  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('hdp-select', 'set', 'hadoop-hdfs-namenode', version), sudo=True)
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_23(self, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None), (0, None)],
                       mocks_dict = mocks_dict)
    self.assertResourceCalled('Execute', ('hdp-select', 'set', 'hadoop-hdfs-namenode', version), sudo=True)
    self.assertNoMoreResources()

  def test_post_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "post_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -report -live',
                              user='hdfs',
                              tries=60,
                              try_sleep=10
                              )
    self.assertNoMoreResources()

  def test_post_upgrade_ha_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/ha_default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "post_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://ns1 -report -live',
                              user='hdfs',
                              tries=60,
                              try_sleep=10
    )
    self.assertNoMoreResources()

  def test_prepare_rolling_upgrade__upgrade(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    json_content['commandParams']['upgrade_direction'] = 'upgrade'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
      classname = "NameNode",
      command = "prepare_rolling_upgrade",
      config_dict = json_content,
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, "Safe mode is OFF in c6401.ambari.apache.org")])
    
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs',
      logoutput = True, user = 'hdfs')
    
    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -rollingUpgrade prepare',
      logoutput = True, user = 'hdfs')

    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -rollingUpgrade query',
      logoutput = True, user = 'hdfs')
    
    self.assertNoMoreResources()

  def test_prepare_rolling_upgrade__upgrade(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/ha_secured.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    json_content['commandParams']['upgrade_direction'] = 'upgrade'

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "prepare_rolling_upgrade",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, "Safe mode is OFF in c6401.ambari.apache.org")])

    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs',
                              logoutput = True, user = 'hdfs')

    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://ns1 -rollingUpgrade prepare',
                              logoutput = True, user = 'hdfs')

    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://ns1 -rollingUpgrade query',
                              logoutput = True, user = 'hdfs')

    self.assertNoMoreResources()
  


  @patch.object(shell, "call")
  def test_prepare_rolling_upgrade__downgrade(self, shell_call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/secured.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    json_content['commandParams']['upgrade_direction'] = 'downgrade'

    # Mock safemode_check call
    shell_call_mock.return_value = 0, "Safe mode is OFF in c6401.ambari.apache.org"

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
      classname = "NameNode",
      command = "prepare_rolling_upgrade",
      config_dict = json_content,
      hdp_stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)
    
    self.assertResourceCalled('Execute', 
        '/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs',
        logoutput = True, user = 'hdfs')
        
    self.assertNoMoreResources()


  def test_finalize_rolling_upgrade(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "finalize_rolling_upgrade",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -rollingUpgrade query',
                              logoutput = True,
                              user = 'hdfs',
                              )
    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -rollingUpgrade finalize',
                              logoutput = True,
                              user = 'hdfs',
                              )
    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://c6401.ambari.apache.org:8020 -rollingUpgrade query',
                              logoutput = True,
                              user = 'hdfs',
                              )
    self.assertNoMoreResources()

  def test_finalize_ha_rolling_upgrade(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/ha_default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "finalize_rolling_upgrade",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://ns1 -rollingUpgrade query',
                              logoutput = True,
                              user = 'hdfs',
                              )
    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://ns1 -rollingUpgrade finalize',
                              logoutput = True,
                              user = 'hdfs',
                              )
    self.assertResourceCalled('Execute', 'hdfs dfsadmin -fs hdfs://ns1 -rollingUpgrade query',
                              logoutput = True,
                              user = 'hdfs',
                              )
    self.assertNoMoreResources()

  @patch.object(shell, "call")
  def test_pre_upgrade_restart_21_and_lower_params(self, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/nn_ru_lzo.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    json_content['hostLevelParams']['stack_name'] = 'HDP'
    json_content['hostLevelParams']['stack_version'] = '2.0'

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None), (0, None), (0, None), (0, None), (0, None), (0, None), (0, None)],
                       mocks_dict = mocks_dict)
    import sys
    self.assertEquals("/etc/hadoop/conf", sys.modules["params"].hadoop_conf_dir)
    self.assertEquals("/usr/lib/hadoop/libexec", sys.modules["params"].hadoop_libexec_dir)
    self.assertEquals("/usr/bin", sys.modules["params"].hadoop_bin_dir)
    self.assertEquals("/usr/lib/hadoop/sbin", sys.modules["params"].hadoop_bin)

  @patch.object(shell, "call")
  def test_pre_upgrade_restart_22_params(self, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/nn_ru_lzo.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.0.0-1234'
    del json_content['commandParams']['version']
    json_content['hostLevelParams']['stack_name'] = 'HDP'
    json_content['hostLevelParams']['stack_version'] = '2.2'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None), (0, None), (0, None), (0, None), (0, None), (0, None), (0, None)],
                       mocks_dict = mocks_dict)
    import sys
    self.assertEquals("/usr/hdp/current/hadoop-client/conf", sys.modules["params"].hadoop_conf_dir)
    self.assertEquals("/usr/hdp/{0}/hadoop/libexec".format(version), sys.modules["params"].hadoop_libexec_dir)
    self.assertEquals("/usr/hdp/{0}/hadoop/bin".format(version), sys.modules["params"].hadoop_bin_dir)
    self.assertEquals("/usr/hdp/{0}/hadoop/sbin".format(version), sys.modules["params"].hadoop_bin)

  @patch.object(shell, "call")
  def test_pre_upgrade_restart_23_params(self, call_mock):
    import itertools

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/nn_ru_lzo.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['commandParams']['upgrade_direction'] = 'upgrade'
    json_content['hostLevelParams']['stack_name'] = 'HDP'
    json_content['hostLevelParams']['stack_version'] = '2.3'

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/namenode.py",
                       classname = "NameNode",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = itertools.cycle([(0, None)]),
                       mocks_dict = mocks_dict)
    import sys
    self.assertEquals("/usr/hdp/2.3.0.0-1234/hadoop/conf", sys.modules["params"].hadoop_conf_dir)
    self.assertEquals("/usr/hdp/2.3.0.0-1234/hadoop/libexec", sys.modules["params"].hadoop_libexec_dir)
    self.assertEquals("/usr/hdp/2.3.0.0-1234/hadoop/bin", sys.modules["params"].hadoop_bin_dir)
    self.assertEquals("/usr/hdp/2.3.0.0-1234/hadoop/sbin", sys.modules["params"].hadoop_bin)



class Popen_Mock:
  return_value = 1
  lines = ['Time Stamp               Iteration#  Bytes Already Moved  Bytes Left To Move  Bytes Being Moved\n',
       'Jul 28, 2014 5:01:49 PM           0                  0 B             5.74 GB            9.79 GB\n',
       'Jul 28, 2014 5:03:00 PM           1                  0 B             5.58 GB            9.79 GB\n',
       '']
  def __call__(self, *args,**kwargs):
    popen = MagicMock()
    popen.returncode = Popen_Mock.return_value
    popen.stdout.readline = MagicMock(side_effect = Popen_Mock.lines)
    return popen
