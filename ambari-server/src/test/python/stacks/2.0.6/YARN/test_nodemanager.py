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
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
from resource_management.core.exceptions import Fail
from resource_management.core import shell
import resource_management.libraries.functions

origin_exists = os.path.exists
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch.object(os.path, "exists", new=MagicMock(
  side_effect=lambda *args: origin_exists(args[0])
  if args[0][-2:] == "j2" else True))
class TestNodeManager(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "YARN/2.1.0.2.0/package"
  STACK_VERSION = "2.0.6"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()

    self.assertResourceCalled('File', '/var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid',
        action = ['delete'],
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid',
    )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited; export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-yarn/sbin/yarn-daemon.sh --config /etc/hadoop/conf start nodemanager',
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid',
        user = 'yarn',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid',
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid',
        tries = 5,
        try_sleep = 1,
    )
    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="stop",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-yarn/sbin/yarn-daemon.sh --config /etc/hadoop/conf stop nodemanager',
                              user='yarn')
    self.assertNoMoreResources()

  def test_configure_secured(self):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="configure",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="start",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()

    self.assertResourceCalled('File', '/var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid',
        action = ['delete'],
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid',
    )
    self.assertResourceCalled('Execute', 'ulimit -c unlimited; export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-yarn/sbin/yarn-daemon.sh --config /etc/hadoop/conf start nodemanager',
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid',
        user = 'yarn',
    )
    self.assertResourceCalled('Execute', 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid',
        not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid && ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E pgrep -F /var/run/hadoop-yarn/yarn/yarn-yarn-nodemanager.pid',
        tries = 5,
        try_sleep = 1,
    )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="stop",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'export HADOOP_LIBEXEC_DIR=/usr/lib/hadoop/libexec && /usr/lib/hadoop-yarn/sbin/yarn-daemon.sh --config /etc/hadoop/conf stop nodemanager',
                              user='yarn')
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/lib/ambari-agent/data/yarn',
        create_parents = True,
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/log',
        group = 'hadoop',
        cd_access = 'a',
        create_parents = True,
        ignore_failures = True,
        mode = 0775,
        owner = 'yarn',
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/log1',
        group = 'hadoop',
        cd_access = 'a',
        create_parents = True,
        ignore_failures = True,
        mode = 0775,
        owner = 'yarn',
    )
    self.assertResourceCalled('File', '/var/lib/ambari-agent/data/yarn/yarn_log_dir_mount.hist',
        content = '\n# This file keeps track of the last known mount-point for each dir.\n# It is safe to delete, since it will get regenerated the next time that the component of the service starts.\n# However, it is not advised to delete this file since Ambari may\n# re-create a dir that used to be mounted on a drive but is now mounted on the root.\n# Comments begin with a hash (#) symbol\n# dir,mount_point\n',
        owner = 'hdfs',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/lib/ambari-agent/data/yarn',
        create_parents = True,
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/local',
                              owner = 'yarn',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              ignore_failures = True,
                              cd_access='a',
                              recursive_mode_flags = {'d': 'a+rwx', 'f': 'a+rw'},
                              )
    self.assertResourceCalled('Directory', '/hadoop/yarn/local1',
                              owner = 'yarn',
                              create_parents = True,
                              group = 'hadoop',
                              ignore_failures = True,
                              mode = 0755,
                              cd_access='a',
                              recursive_mode_flags = {'d': 'a+rwx', 'f': 'a+rw'}
                              )
    self.assertResourceCalled('File', '/var/lib/ambari-agent/data/yarn/yarn_local_dir_mount.hist',
        content = '\n# This file keeps track of the last known mount-point for each dir.\n# It is safe to delete, since it will get regenerated the next time that the component of the service starts.\n# However, it is not advised to delete this file since Ambari may\n# re-create a dir that used to be mounted on a drive but is now mounted on the root.\n# Comments begin with a hash (#) symbol\n# dir,mount_point\n',
        owner = 'hdfs',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      ignore_failures = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['yarn-site']
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
      configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['yarn-env']['content']),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/usr/lib/hadoop-yarn/bin/container-executor',
                              group = 'hadoop',
                              mode = 02050,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/container-executor.cfg',
                              content = Template('container-executor.cfg.j2'),
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('Directory', '/cgroups_test/cpu',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access="a"
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['mapred-env']['content']),
                              mode = 0755,
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-client.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-server.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/hadoop/yarn/local',
                              action = ['delete']
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/log',
                              action = ['delete']
    )
    self.assertResourceCalled('Directory', '/var/lib/hadoop-yarn',)
    self.assertResourceCalled('File', '/var/lib/hadoop-yarn/nm_security_enabled',
                              content= 'Marker file to track first start after enabling/disabling security. During first start yarn local, log dirs are removed and recreated'
    )
    self.assertResourceCalled('Directory', '/var/lib/ambari-agent/data/yarn',
        create_parents = True,
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/log',
                              owner = 'yarn',
                              group = 'hadoop',
                              create_parents = True,
                              ignore_failures = True,
                              mode = 0775,
                              cd_access='a',
                              )
    self.assertResourceCalled('File', '/var/lib/ambari-agent/data/yarn/yarn_log_dir_mount.hist',
        content = '\n# This file keeps track of the last known mount-point for each dir.\n# It is safe to delete, since it will get regenerated the next time that the component of the service starts.\n# However, it is not advised to delete this file since Ambari may\n# re-create a dir that used to be mounted on a drive but is now mounted on the root.\n# Comments begin with a hash (#) symbol\n# dir,mount_point\n',
        owner = 'hdfs',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/lib/ambari-agent/data/yarn',
        create_parents = True,
        mode = 0755,
    )
    self.assertResourceCalled('Directory', '/hadoop/yarn/local',
                              owner = 'yarn',
                              group = 'hadoop',
                              create_parents = True,
                              ignore_failures = True,
                              mode = 0755,
                              cd_access='a',
                              recursive_mode_flags = {'d': 'a+rwx', 'f': 'a+rw'},
                              )
    self.assertResourceCalled('File', '/var/lib/ambari-agent/data/yarn/yarn_local_dir_mount.hist',
        content = '\n# This file keeps track of the last known mount-point for each dir.\n# It is safe to delete, since it will get regenerated the next time that the component of the service starts.\n# However, it is not advised to delete this file since Ambari may\n# re-create a dir that used to be mounted on a drive but is now mounted on the root.\n# Comments begin with a hash (#) symbol\n# dir,mount_point\n',
        owner = 'hdfs',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      create_parents = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      group = 'hadoop',
      create_parents = True,
      ignore_failures = True,
      cd_access = 'a',
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-site']
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['yarn-site']
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
      configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['yarn-env']['content'] +
                               '\nYARN_OPTS="$YARN_OPTS -Dzookeeper.sasl.client=true -Dzookeeper.sasl.client.username=zookeeper -Djava.security.auth.login.config=/usr/hdp/current/hadoop-client/conf/yarn_jaas.conf -Dzookeeper.sasl.clientconfig=Client"\n'),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/usr/lib/hadoop-yarn/bin/container-executor',
      group = 'hadoop',
      mode = 06050,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/container-executor.cfg',
      content = Template('container-executor.cfg.j2'),
      group = 'hadoop',
      mode = 0644,
    )
    self.assertResourceCalled('Directory', '/cgroups_test/cpu',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              cd_access="a"
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['mapred-env']['content']),
                              mode = 0755,
                              owner = 'root',
                              )
    self.assertResourceCalled('File', '/usr/lib/hadoop/sbin/task-controller',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 06050,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn_jaas.conf',
                              content = Template('yarn_jaas.conf.j2'),
                              owner = 'yarn',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-client.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/ssl-server.xml.example',
                              owner = 'mapred',
                              group = 'hadoop',
                              )

  @patch("socket.gethostbyname")
  @patch('time.sleep')
  @patch.object(resource_management.libraries.functions, "get_stack_version", new = MagicMock(return_value='2.3.0.0-1234'))
  def test_post_upgrade_restart(self, time_mock, socket_gethostbyname_mock):
    process_output = """
      c6401.ambari.apache.org:45454  RUNNING  c6401.ambari.apache.org:8042  0
    """
    mocks_dict = {}
    socket_gethostbyname_mock.return_value = "test_host"

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
      classname = "Nodemanager",
      command = "post_upgrade_restart",
      config_file = "default.json",
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      checked_call_mocks = [(0, process_output)],
      mocks_dict = mocks_dict
    )

    self.assertTrue(mocks_dict['checked_call'].called)
    self.assertEqual(mocks_dict['checked_call'].call_count,1)

    self.assertEquals(
      "yarn node -list -states=RUNNING",
       mocks_dict['checked_call'].call_args_list[0][0][0])

    self.assertEquals( {'user': u'yarn'}, mocks_dict['checked_call'].call_args_list[0][1])


  @patch('time.sleep')
  def test_post_upgrade_restart_nodemanager_not_ready(self, time_mock):
    process_output = """
      c9999.ambari.apache.org:45454  RUNNING  c9999.ambari.apache.org:8042  0
    """
    mocks_dict = {}

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                         classname="Nodemanager",
                         command = "post_upgrade_restart",
                         config_file="default.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES,
                         call_mocks = [(0, process_output)],
                         mocks_dict = mocks_dict,
      )
      self.fail('Missing NodeManager should have caused a failure')
    except Fail,fail:
      self.assertTrue(mocks_dict['call'].called)
      self.assertEqual(mocks_dict['call'].call_count,12)


  @patch('time.sleep')
  def test_post_upgrade_restart_nodemanager_not_ready(self, time_mock):
    process_output = """
      c6401.ambari.apache.org:45454  RUNNING  c6401.ambari.apache.org:8042  0
    """
    mocks_dict = {}
    
    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                         classname="Nodemanager",
                         command = "post_upgrade_restart",
                         config_file="default.json",
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES,
                         call_mocks = [(999, process_output)],
                         mocks_dict = mocks_dict,
      )
      self.fail('Invalid return code should cause a failure')
    except Fail,fail:
      self.assertTrue(mocks_dict['call'].called)
      self.assertEqual(mocks_dict['call'].call_count,1)

  @patch("resource_management.libraries.functions.security_commons.build_expectations")
  @patch("resource_management.libraries.functions.security_commons.get_params_from_filesystem")
  @patch("resource_management.libraries.functions.security_commons.validate_security_config_properties")
  @patch("resource_management.libraries.functions.security_commons.cached_kinit_executor")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def test_security_status(self, put_structured_out_mock, cached_kinit_executor_mock, validate_security_config_mock, get_params_mock, build_exp_mock):
    # Test that function works when is called with correct parameters

    security_params = {
      'yarn-site': {
        'yarn.nodemanager.keytab': 'path/to/nodemanager/keytab',
        'yarn.nodemanager.principal': 'nodemanager_principal',
        'yarn.nodemanager.webapp.spnego-keytab-file': 'path/to/nodemanager/webapp/keytab',
        'yarn.nodemanager.webapp.spnego-principal': 'nodemanager_webapp_principal'
      }
    }
    result_issues = []
    props_value_check = {"yarn.timeline-service.http-authentication.type": "kerberos",
                         "yarn.acl.enable": "true"}
    props_empty_check = ["yarn.nodemanager.principal",
                         "yarn.nodemanager.keytab",
                         "yarn.nodemanager.webapp.spnego-principal",
                         "yarn.nodemanager.webapp.spnego-keytab-file"]

    props_read_check = ["yarn.nodemanager.keytab",
                        "yarn.nodemanager.webapp.spnego-keytab-file"]

    get_params_mock.return_value = security_params
    validate_security_config_mock.return_value = result_issues

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    build_exp_mock.assert_called_with('yarn-site', props_value_check, props_empty_check, props_read_check)
    put_structured_out_mock.assert_called_with({"securityState": "SECURED_KERBEROS"})
    self.assertTrue(cached_kinit_executor_mock.call_count, 2)
    cached_kinit_executor_mock.assert_called_with('/usr/bin/kinit',
                                                  self.config_dict['configurations']['yarn-env']['yarn_user'],
                                                  security_params['yarn-site']['yarn.nodemanager.webapp.spnego-keytab-file'],
                                                  security_params['yarn-site']['yarn.nodemanager.webapp.spnego-principal'],
                                                  self.config_dict['hostname'],
                                                  '/tmp')

    # Testing that the exception throw by cached_executor is caught
    cached_kinit_executor_mock.reset_mock()
    cached_kinit_executor_mock.side_effect = Exception("Invalid command")

    try:
          self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
          )
    except:
      self.assertTrue(True)

    # Testing with a security_params which doesn't contains yarn-site
    empty_security_params = {}
    cached_kinit_executor_mock.reset_mock()
    get_params_mock.reset_mock()
    put_structured_out_mock.reset_mock()
    get_params_mock.return_value = empty_security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityIssuesFound": "Keytab file or principal are not set property."})

    # Testing with not empty result_issues
    result_issues_with_params = {
      'yarn-site': "Something bad happened"
    }

    validate_security_config_mock.reset_mock()
    get_params_mock.reset_mock()
    validate_security_config_mock.return_value = result_issues_with_params
    get_params_mock.return_value = security_params

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="security_status",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

    # Testing with security_enable = false
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname="Nodemanager",
                       command="security_status",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    put_structured_out_mock.assert_called_with({"securityState": "UNSECURED"})

  
  @patch.object(resource_management.libraries.functions, "get_stack_version", new = MagicMock(return_value='2.3.0.0-1234'))
  def test_pre_upgrade_restart_23(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/nodemanager.py",
                       classname = "Nodemanager",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hadoop-yarn-nodemanager', version), sudo=True)
    self.assertNoMoreResources()

    self.assertEquals(1, mocks_dict['call'].call_count)
    self.assertEquals(1, mocks_dict['checked_call'].call_count)
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'set-conf-dir', '--package', 'hadoop', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['checked_call'].call_args_list[0][0][0])
    self.assertEquals(
      ('ambari-python-wrap', '/usr/bin/conf-select', 'create-conf-dir', '--package', 'hadoop', '--stack-version', '2.3.0.0-1234', '--conf-version', '0'),
       mocks_dict['call'].call_args_list[0][0][0])
