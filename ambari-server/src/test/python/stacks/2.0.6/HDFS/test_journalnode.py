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
import os
from stacks.utils.RMFTestCase import *
from mock.mock import MagicMock, patch
from resource_management.libraries.script.script import Script

@patch.object(Script, 'format_package_name', new = MagicMock())
class TestJournalnode(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HDFS/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"
  UPGRADE_STACK_VERSION = "2.2"

  CONFIG_OVERRIDES = {"serviceName":"HDFS", "role":"JOURNALNODE"}

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
                       classname = "JournalNode",
                       command = "configure",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
                       classname = "JournalNode",
                       command = "start",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start journalnode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid",
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
                       classname = "JournalNode",
                       command = "stop",
                       config_file = "default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop journalnode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid', action = ['delete'])
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
                       classname = "JournalNode",
                       command = "configure",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
                       classname = "JournalNode",
                       command = "start",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start journalnode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid",
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
                       classname = "JournalNode",
                       command = "stop",
                       config_file = "secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop journalnode'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-journalnode.pid', action = ['delete'])
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/grid/0/hdfs/journal',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-i386-32',
        create_parents = True,
    )
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-amd64-64',
        create_parents = True,
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
                              create_parents = True,
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


  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/grid/0/hdfs/journal',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              cd_access='a'
                              )
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-i386-32',
        create_parents = True,
    )
    self.assertResourceCalled('Directory', '/usr/lib/hadoop/lib/native/Linux-amd64-64',
        create_parents = True,
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
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hdfs.conf',
                              content = Template('hdfs.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hdfs_dn_jaas.conf',
                              content = Template('hdfs_dn_jaas.conf.j2'),
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hdfs_nn_jaas.conf',
                              content = Template('hdfs_nn_jaas.conf.j2'),
                              owner = 'hdfs',
                              group = 'hadoop',
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


  @patch('time.sleep')
  def test_post_upgrade_restart(self, time_mock):
    # load the NN and JN JMX files so that the urllib2.urlopen mock has data
    # to return
    num_journalnodes = 3
    journalnode_jmx_file = os.path.join(RMFTestCase._getStackTestsFolder(),
      self.UPGRADE_STACK_VERSION, "configs", "journalnode-upgrade-jmx.json")

    namenode_jmx_file = os.path.join(RMFTestCase._getStackTestsFolder(),
      self.UPGRADE_STACK_VERSION, "configs", "journalnode-upgrade-namenode-jmx.json")

    namenode_status_active_file = os.path.join(RMFTestCase._getStackTestsFolder(),
      self.UPGRADE_STACK_VERSION, "configs", "journalnode-upgrade-namenode-status-active.json")

    namenode_status_standby_file = os.path.join(RMFTestCase._getStackTestsFolder(),
      self.UPGRADE_STACK_VERSION, "configs", "journalnode-upgrade-namenode-status-standby.json")

    journalnode_jmx = open(journalnode_jmx_file, 'r').read()
    namenode_jmx = open(namenode_jmx_file, 'r').read()
    namenode_status_active = open(namenode_status_active_file, 'r').read()
    namenode_status_standby = open(namenode_status_standby_file, 'r').read()

    import utils
    import urllib2
    from namenode_ha_state import NamenodeHAState

    url_stream_mock = MagicMock()
    url_stream_mock.read.side_effect = (num_journalnodes * [namenode_jmx, journalnode_jmx])
    urlopen_mock = MagicMock(return_value = url_stream_mock)
    #urlopen_mock.return_value = url_stream_mock
    curl_krb_request_mock = MagicMock(side_effect=(num_journalnodes * [(namenode_jmx, "", 1), (journalnode_jmx, "", 1)]))
    get_address_mock = MagicMock(return_value="c6406.ambari.apache.org")
    with patch.object(utils, "curl_krb_request", curl_krb_request_mock):
      with patch.object(urllib2, "urlopen", urlopen_mock):
       with patch.object(NamenodeHAState, "get_address", get_address_mock):
         self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
           classname = "JournalNode", command = "post_upgrade_restart",
           config_file = "journalnode-upgrade.json",
           checked_call_mocks = [(0, str(namenode_status_active)), (0, str(namenode_status_standby))],
           stack_version = self.UPGRADE_STACK_VERSION,
           target = RMFTestCase.TARGET_COMMON_SERVICES )

    # ensure that the mock was called with the http-style version of the URL
    urlopen_mock.assert_called
    urlopen_mock.assert_called_with("http://c6407.ambari.apache.org:8480/jmx")


    # Check the case where there is a journalnode installed but NOT part of the quorum.
    url_stream_mock = MagicMock()
    url_stream_mock.read.side_effect = (num_journalnodes * [namenode_jmx, journalnode_jmx])
    urlopen_mock = MagicMock(return_value = url_stream_mock)
    curl_krb_request_mock = MagicMock(side_effect=(num_journalnodes * [(namenode_jmx, "", 1), (journalnode_jmx, "", 1)]))

    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/journalnode-upgrade.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    # add one more host that is not part of the hdfs-site/dfs.namenode.shared.edits.dir property
    json_content['clusterHostInfo']['journalnode_hosts'] = ['c6406.ambari.apache.org',
      'c6407.ambari.apache.org', 'c6408.ambari.apache.org', 'c6409.ambari.apache.org' ]

    with patch.object(utils, "curl_krb_request", curl_krb_request_mock):
      with patch.object(urllib2, "urlopen", urlopen_mock):
       with patch.object(NamenodeHAState, "get_address", get_address_mock):
         self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
           classname = "JournalNode", command = "post_upgrade_restart",
           config_dict = json_content,
           checked_call_mocks = [(0, str(namenode_status_active)), (0, str(namenode_status_standby))],
           stack_version = self.UPGRADE_STACK_VERSION,
           target = RMFTestCase.TARGET_COMMON_SERVICES )

    # ensure that the mock was called with the http-style version of the URL
    urlopen_mock.assert_called
    urlopen_mock.assert_called_with("http://c6407.ambari.apache.org:8480/jmx")

    url_stream_mock.reset_mock()
    curl_krb_request_mock.reset_mock()
    get_address_mock.reset_mock()


    # now try with HDFS on SSL
    with patch.object(utils, "curl_krb_request", curl_krb_request_mock):
      with patch.object(urllib2, "urlopen", urlopen_mock):
        with patch.object(NamenodeHAState, "get_address", get_address_mock):
         self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
           classname = "JournalNode", command = "post_upgrade_restart",
           config_file = "journalnode-upgrade-hdfs-secure.json",
           checked_call_mocks = [(0, str(namenode_status_active)), (0, str(namenode_status_standby))],
           stack_version = self.UPGRADE_STACK_VERSION,
           target = RMFTestCase.TARGET_COMMON_SERVICES )

    # ensure that the mock was called with the http-style version of the URL
    curl_krb_request_mock.assert_called
    curl_krb_request_mock.assert_called_with("/tmp", "/etc/security/keytabs/smokeuser.headless.keytab",
                                             "ambari-qa@EXAMPLE.COM", "https://c6407.ambari.apache.org:8481/jmx",
                                             "jn_upgrade", "/usr/bin/kinit", False, None, "ambari-qa")

  @patch('time.sleep')
  @patch("urllib2.urlopen")
  def test_post_upgrade_restart_bad_jmx(self, urlopen_mock, time_mock):
    urlopen_mock_response = '{ "bad_data" : "gonna_mess_you_up" }'

    url_stream_mock = MagicMock()
    url_stream_mock.read.side_effect = [urlopen_mock_response]
    urlopen_mock.return_value = url_stream_mock

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
        classname = "JournalNode", command = "post_upgrade_restart",
        config_file = "journalnode-upgrade.json",
        stack_version = self.UPGRADE_STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES )

      self.fail("Expected a failure since the JMX JSON for JournalTransactionInfo was missing")
    except:
      pass

  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
                       classname = "JournalNode",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hadoop-hdfs-journalnode', version), sudo=True,)
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_23(self, call_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/journalnode.py",
                       classname = "JournalNode",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hadoop-hdfs-journalnode', version), sudo=True,)
    self.assertNoMoreResources()
