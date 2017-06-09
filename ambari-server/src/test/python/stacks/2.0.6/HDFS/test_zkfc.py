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
from stacks.utils.RMFTestCase import *
from ambari_commons import OSCheck
from mock.mock import MagicMock, patch
from resource_management.core import shell

class TestZkfc(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HDFS/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zkfc_slave.py",
                       classname = "ZkfcSlave",
                       command = "start",
                       config_file = "ha_default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
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

    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
        owner = 'hdfs',
        group = 'hadoop',
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              group = 'hadoop'
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              group = 'hadoop'
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start zkfc'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid",
    )
    self.assertNoMoreResources()


  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zkfc_slave.py",
                       classname = "ZkfcSlave",
                       command = "stop",
                       config_file = "ha_default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop zkfc'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid', action = ['delete'])
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zkfc_slave.py",
                       classname = "ZkfcSlave",
                       command = "start",
                       config_file = "ha_secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
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
    self.assertResourceCalled('File', '/etc/hadoop/conf/hdfs_jn_jaas.conf',
                              content = Template('hdfs_jn_jaas.conf.j2'),
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

    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
        owner = 'hdfs',
        group = 'hadoop',
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              group = 'hadoop'
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              group = 'hadoop'
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start zkfc'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid",
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zkfc_slave.py",
                       classname = "ZkfcSlave",
                       command = "stop",
                       config_file = "ha_secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf stop zkfc'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        only_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid")

    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid', action = ['delete'])
    self.assertNoMoreResources()

  def test_start_with_ha_active_namenode_bootstrap(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zkfc_slave.py",
                       classname = "ZkfcSlave",
                       command = "start",
                       config_file="ha_bootstrap_active_node.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
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

    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
    )
    # TODO: verify that the znode initialization occurs prior to ZKFC startup
    self.assertResourceCalled('Directory', '/var/run/hadoop',
        owner = 'hdfs',
        group = 'hadoop',
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              group = 'hadoop'
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              group = 'hadoop'
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start zkfc'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid",
    )
    self.assertNoMoreResources()

  def test_start_with_ha_standby_namenode_bootstrap(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/zkfc_slave.py",
                       classname = "ZkfcSlave",
                       command = "start",
                       config_file="ha_bootstrap_standby_node.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
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

    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0755
    )
    # TODO: verify that the znode initialization occurs prior to ZKFC startup
    self.assertResourceCalled('Directory', '/var/run/hadoop',
        owner = 'hdfs',
        group = 'hadoop',
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop/hdfs',
                              owner = 'hdfs',
                              create_parents = True,
                              group = 'hadoop'
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop/hdfs',
                              owner = 'hdfs',
                              group = 'hadoop',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid',
        action = ['delete'],
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh su hdfs -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]ulimit -c unlimited ;  /usr/lib/hadoop/sbin/hadoop-daemon.sh --config /etc/hadoop/conf start zkfc'",
        environment = {'HADOOP_LIBEXEC_DIR': '/usr/lib/hadoop/libexec'},
        not_if = "ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop/hdfs/hadoop-hdfs-zkfc.pid",
    )
    self.assertNoMoreResources()
