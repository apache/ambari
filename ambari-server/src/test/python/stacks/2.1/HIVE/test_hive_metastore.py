#!/usr/bin/env python

"""
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
"""

import json
import os

from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
from resource_management.libraries.functions.constants import Direction, StackFeature

# used for faking out stack features when the config files used by unit tests use older stacks
def mock_stack_feature(stack_feature, stack_version):
  if stack_feature == StackFeature.ROLLING_UPGRADE:
    return True
  if stack_feature == StackFeature.CONFIG_VERSIONING:
    return True
  if stack_feature == StackFeature.HIVE_METASTORE_UPGRADE_SCHEMA:
    return True

  return False

@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch("resource_management.libraries.functions.get_user_call_output.get_user_call_output", new=MagicMock(return_value=(0,'123','')))
@patch("resource_management.libraries.functions.stack_features.check_stack_feature", new=MagicMock(side_effect=mock_stack_feature))
class TestHiveMetastore(RMFTestCase):

  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"

  CONFIG_OVERRIDES = { "serviceName" : "HIVE", "role" : "HIVE_METASTORE" }

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "configure",
                       config_file="../../2.1/configs/default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       config_file="../../2.1/configs/default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()
    self.assert_init_schema('aaa')

    self.assertResourceCalled('Execute', '/tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.err /var/run/hive/hive.pid /usr/hdp/current/hive-server2/conf/conf.server /var/log/hive',
        environment = {'HIVE_CMD': '/usr/hdp/current/hive-server2/bin/hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:/usr/hdp/2.2.1.0-2067/hadoop/bin'],
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive aaa com.mysql.jdbc.Driver',
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        tries = 5,
        try_sleep = 10,
    )

    self.assertNoMoreResources()

  @patch("os.umask")
  def test_start_default_umask_027(self, umask_mock):
    umask_mock.return_value = 027
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       config_file="../../2.1/configs/default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default()
    self.assert_init_schema('aaa')

    self.assertResourceCalled('Execute', '/tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.err /var/run/hive/hive.pid /usr/hdp/current/hive-server2/conf/conf.server /var/log/hive',
        environment = {'HIVE_CMD': '/usr/hdp/current/hive-server2/bin/hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:/usr/hdp/2.2.1.0-2067/hadoop/bin'],
    )
    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive aaa com.mysql.jdbc.Driver',
                              path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
                              tries = 5,
                              try_sleep = 10,
                              )

    self.assertNoMoreResources()

  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "stop",
                       config_file="../../2.1/configs/default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
        ignore_failures = True
    )
    self.assertResourceCalled('Execute', "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
        tries = 20,
        try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive.pid',
                              action = ['delete'],
                              )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "configure",
                       config_file="../../2.1/configs/secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "start",
                       config_file="../../2.1/configs/secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_secured()
    self.assert_init_schema('asd')
    self.assertResourceCalled('Execute', '/tmp/start_metastore_script /var/log/hive/hive.out /var/log/hive/hive.err /var/run/hive/hive.pid /usr/hdp/current/hive-server2/conf/conf.server /var/log/hive',
        environment = {'HIVE_CMD': '/usr/hdp/current/hive-server2/bin/hive',
           'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1",
        user = 'hive',
        path = ['/bin:/usr/hdp/current/hive-server2/bin:/usr/hdp/current/hadoop-client/bin'],
    )

    self.assertResourceCalled('Execute', '/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar org.apache.ambari.server.DBConnectionVerification \'jdbc:mysql://c6402.ambari.apache.org/hive?createDatabaseIfNotExist=true\' hive asd com.mysql.jdbc.Driver',
                              path=['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'], tries=5, try_sleep=10
    )

    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "stop",
                       config_file="../../2.1/configs/secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', "ambari-sudo.sh kill 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
    )
    self.assertResourceCalled('Execute', "ambari-sudo.sh kill -9 123",
        not_if = "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) || ( sleep 5 && ! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1) )",
        ignore_failures = True
    )
    self.assertResourceCalled('Execute', "! (ls /var/run/hive/hive.pid >/dev/null 2>&1 && ps -p 123 >/dev/null 2>&1)",
        tries = 20,
        try_sleep = 3,
    )
    self.assertResourceCalled('File', '/var/run/hive/hive.pid',
      action = ['delete'],
    )
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-server2/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hive-server2/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-default.xml.template',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-env.sh.template',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hive-server2/conf/conf.server',
                              mode = 0600,
                              configuration_attributes = {u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                     u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                     u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0600,
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content = Template('hive.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
        mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/usr/share/java/mysql-connector-java.jar',
     '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/hadoop-metrics2-hivemetastore.properties',
                              owner = 'hive',
                              group = 'hadoop',
                              content = Template('hadoop-metrics2-hivemetastore.properties.j2'),
                              mode = 0600,
                              )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
                              content = StaticFile('startMetastore.sh'),
                              mode = 0755,
                              )

  def assert_init_schema(self, password):
    self.assertResourceCalled('Execute', 'export HIVE_CONF_DIR=/usr/hdp/current/hive-server2/conf/conf.server ; /usr/hdp/current/hive-server2/bin/schematool -initSchema -dbType mysql -userName hive -passWord {password} -verbose'.format(password = password),
        not_if = "ambari-sudo.sh su hive -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]export HIVE_CONF_DIR=/usr/hdp/current/hive-server2/conf/conf.server ; /usr/hdp/current/hive-server2/bin/schematool -info -dbType mysql -userName hive -passWord {password} -verbose'".format(password = password),
        user = 'hive',
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/hive',
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-server2/conf',
                              owner = 'hive',
                              group = 'hadoop',
                              create_parents = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hive-server2/conf',
                              mode = 0644,
                              configuration_attributes = {u'final': {u'mapred.healthChecker.script.path': u'true',
                                                                     u'mapreduce.jobtracker.staging.root.dir': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-default.xml.template',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/hive-env.sh.template',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/hive-server2/conf/conf.server',
                              mode = 0600,
                              configuration_attributes = {u'final': {u'hive.optimize.bucketmapjoin.sortedmerge': u'true',
                                                                     u'javax.jdo.option.ConnectionDriverName': u'true',
                                                                     u'javax.jdo.option.ConnectionPassword': u'true'}},
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0600,
                              )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
                              owner = 'root',
                              group = 'root',
                              create_parents = True,
                              )
    self.assertResourceCalled('File', '/etc/security/limits.d/hive.conf',
                              content = Template('hive.conf.j2'),
                              owner = 'root',
                              group = 'root',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/zkmigrator_jaas.conf',
                              content = Template('zkmigrator_jaas.conf.j2'),
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-agent/DBConnectionVerification.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar'),
        mode = 0644,
    )
    self.assertResourceCalled('Directory', '/var/run/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/log/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Directory', '/var/lib/hive',
                              owner = 'hive',
                              group = 'hadoop',
                              mode = 0755,
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/usr/share/java/mysql-connector-java.jar',
     '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-server2/conf/conf.server/hadoop-metrics2-hivemetastore.properties',
                              owner = 'hive',
                              group = 'hadoop',
                              content = Template('hadoop-metrics2-hivemetastore.properties.j2'),
                              mode = 0600,
                              )

    self.assertResourceCalled('File', '/tmp/start_metastore_script',
                              content = StaticFile('startMetastore.sh'),
                              mode = 0755,
                              )

  def test_pre_upgrade_restart(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-metastore', version), sudo=True,)
    self.assertNoMoreResources()

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_23(self, call_mock, os_path__exists_mock):
    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    os_path__exists_mock.return_value = False
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-metastore', version), sudo=True,)
    self.assertNoMoreResources()

  @patch("resource_management.libraries.functions.stack_select.get_hadoop_dir", new = MagicMock(return_value="/usr/hdp/current/hadoop-client"))
  def test_pre_upgrade_restart_ims(self):
    """
    Tests the state of the init_metastore_schema property on update
    """
    config_file = self.get_src_folder() + "/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    # first try it with a normal, non-upgrade
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
      classname = "HiveMetastore",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, ''), (0, None)])

    self.assertEquals(True, RMFTestCase.env.config["params"]["init_metastore_schema"])

    self.config_dict = None
    config_file = self.get_src_folder() + "/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    json_content['commandParams']['version'] = '2.3.0.0-1234'
    json_content['commandParams']['upgrade_direction'] = Direction.UPGRADE
    json_content['hostLevelParams']['stack_version'] = '2.3.0.0-0'

    # now try it in an upgrade
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
      classname = "HiveMetastore",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, ''), (0, None)])

    self.assertEquals(False, RMFTestCase.env.config["params"]["init_metastore_schema"])

    self.config_dict = None
    config_file = self.get_src_folder() + "/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)

    json_content['commandParams']['upgrade_direction'] = Direction.DOWNGRADE

    # now try it in a downgrade
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
      classname = "HiveMetastore",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, ''), (0, None)])

    self.assertEquals(False, RMFTestCase.env.config["params"]["init_metastore_schema"])


  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch("resource_management.libraries.functions.get_stack_version")
  def test_upgrade_metastore_schema(self, get_stack_version_mock, call_mock, os_path_exists_mock):
    get_stack_version_mock.return_value = '2.4.0.0-1234'

    def side_effect(path):
      if path == "/usr/hdp/2.3.7.0-1234/hive-server2/lib/mysql-connector-java.jar":
        return True
      if ".j2" in path:
        return True
      return False

    os_path_exists_mock.side_effect = side_effect

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    # must be HDP 2.4+
    version = '2.4.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['commandParams']['upgrade_direction'] = Direction.UPGRADE
    json_content['hostLevelParams']['stack_version'] = "2.4"
    json_content["upgradeSummary"] = {
      "services":{
        "HIVE":{
          "sourceRepositoryId":1,
          "sourceStackId":"HDP-2.3",
          "sourceVersion":"2.3.7.0-1234",
          "targetRepositoryId":2,
          "targetStackId":"HDP-2.4",
          "targetVersion":version
        }
      },
      "direction":"UPGRADE",
      "type":"nonrolling_upgrade",
      "isRevert":False,
      "orchestration":"STANDARD"
    }

    # trigger the code to think it needs to copy the JAR
    json_content['configurations']['hive-site']['javax.jdo.option.ConnectionDriverName'] = "com.mysql.jdbc.Driver"
    json_content['configurations']['hive-env']['hive_database'] = "Existing"

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
      classname = "HiveMetastore",
      command = "pre_upgrade_restart",
      config_dict = json_content,
      config_overrides = self.CONFIG_OVERRIDES,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      mocks_dict = mocks_dict)

    self.assertResourceCalled('Execute', ('ambari-python-wrap',
     '/usr/bin/hdp-select',
     'set',
     'hive-metastore',
     '2.4.0.0-1234'),
        sudo = True)

    # we don't care about configure here - the strings are different anyway because this
    # is an upgrade, so just pop those resources off of the call stack
    self.assertResourceCalledIgnoreEarlier('Directory', '/var/lib/hive', owner = 'hive', group = 'hadoop',
      mode = 0755, create_parents = True, cd_access = 'a')

    self.assertResourceCalledIgnoreEarlier('Execute', ('rm', '-f', '/usr/hdp/current/hive-metastore/lib/ojdbc6.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True)

    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar'))

    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/tmp/mysql-connector-java.jar',
     '/usr/hdp/2.4.0.0-1234/hive/lib/mysql-connector-java.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True)

    self.assertResourceCalled('File', '/usr/hdp/2.4.0.0-1234/hive/lib/mysql-connector-java.jar',
        mode = 0644)
    self.assertResourceCalled('File', '/usr/hdp/current/hive-metastore/conf/conf.server/hadoop-metrics2-hivemetastore.properties',
        content = Template('hadoop-metrics2-hivemetastore.properties.j2'),
        owner = 'hive',
        group = 'hadoop',
        mode = 0600
    )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
        content = StaticFile('startMetastore.sh'),
        mode = 0755,
    )
    self.assertResourceCalled('Execute', ('rm', '-f', '/usr/hdp/current/hive-metastore/lib/ojdbc6.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/tmp/mysql-connector-java.jar',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar'),
    )
    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     u'/tmp/mysql-connector-java.jar',
     u'/usr/hdp/2.4.0.0-1234/hive/lib/mysql-connector-java.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/2.4.0.0-1234/hive/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('Execute', ('cp',
     '/usr/hdp/2.3.7.0-1234/hive/lib/mysql-connector-java.jar',
     '/usr/hdp/2.4.0.0-1234/hive/lib'),
        path = ['/bin', '/usr/bin/'],
        sudo = True)

    self.assertResourceCalled('File', '/usr/hdp/2.4.0.0-1234/hive/lib/mysql-connector-java.jar',
        mode = 0644)

    self.assertResourceCalled('Execute', '/usr/hdp/2.4.0.0-1234/hive/bin/schematool -dbType mysql -upgradeSchema',
         logoutput = True,
         environment = {'HIVE_CONF_DIR': '/usr/hdp/current/hive-metastore/conf/conf.server'},
         tries = 1,
         user = 'hive')

    self.assertNoMoreResources()

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch("resource_management.libraries.functions.get_stack_version")
  @patch("resource_management.libraries.functions.stack_select.get_hadoop_dir", new = MagicMock(return_value = "/usr/hdp/current/hadoop-client"))
  def test_upgrade_metastore_schema_using_new_db(self, get_stack_version_mock, call_mock, os_path_exists_mock):
    get_stack_version_mock.return_value = '2.3.0.0-1234'

    def side_effect(path):
      if path == "/usr/hdp/2.2.7.0-1234/hive-server2/lib/mysql-connector-java.jar":
        return True
      if path == "/usr/hdp/current/hive-client/lib/mysql-connector-java.jar":
        return True
      if ".j2" in path:
        return True
      return False

    os_path_exists_mock.side_effect = side_effect

    config_file = self.get_src_folder()+"/test/python/stacks/2.1/configs/hive-metastore-upgrade.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
      classname = "HiveMetastore",
      command = "upgrade_schema",
      config_dict = json_content,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES,
      call_mocks = [(0, None, ''), (0, None)],
      mocks_dict = mocks_dict)


    # we don't care about configure here - the strings are different anyway because this
    # is an upgrade, so just pop those resources off of the call stack
    self.assertResourceCalledIgnoreEarlier('Directory', '/var/lib/hive', owner = 'hive', group = 'hadoop',
      mode = 0755, create_parents = True, cd_access = 'a')

    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/usr/share/java/mysql-connector-java.jar',
     '/usr/hdp/2.3.2.0-2950/hive/lib/mysql-connector-java.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/2.3.2.0-2950/hive/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-metastore/conf/conf.server/hadoop-metrics2-hivemetastore.properties',
        content = Template('hadoop-metrics2-hivemetastore.properties.j2'),
        owner = 'hive',
        group = 'hadoop',
        mode = 0600
    )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
        content = StaticFile('startMetastore.sh'),
        mode = 0755,
    )

    self.assertResourceCalled('Directory', '/tmp/hive',
                              owner = 'hive',
                              create_parents = True,
                              mode=0777
                              )

    self.assertResourceCalled('Execute', ('cp',
     '--remove-destination',
     '/usr/share/java/mysql-connector-java.jar',
     u'/usr/hdp/2.3.2.0-2950/hive/lib/mysql-connector-java.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/2.3.2.0-2950/hive/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('Execute', ('cp',
     '/usr/hdp/2.3.0.0-2557/hive/lib/mysql-connector-java.jar',
     '/usr/hdp/2.3.2.0-2950/hive/lib'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/2.3.2.0-2950/hive/lib/mysql-connector-java.jar',
        mode = 0644,
    )
    self.assertResourceCalled('Execute', '/usr/hdp/2.3.2.0-2950/hive/bin/schematool -dbType mysql -upgradeSchema',
        logoutput = True,
        environment = {'HIVE_CONF_DIR': '/usr/hdp/current/hive-metastore/conf/conf.server'},
        tries = 1,
        user = 'hive',
    )

    self.assertNoMoreResources()

  @patch("os.path.exists")
  @patch("resource_management.core.shell.call")
  @patch("resource_management.libraries.functions.get_stack_version")
  def test_upgrade_sqla_metastore_schema_with_jdbc_download(self, get_stack_version_mock, call_mock, os_path_exists_mock):

    get_stack_version_mock.return_value = '2.3.0.0-1234'

    def side_effect(path):
      if path == "/usr/hdp/2.2.7.0-1234/hive-server2/lib/mysql-connector-java.jar":
        return True
      if ".j2" in path:
        return True
      return False

    os_path_exists_mock.side_effect = side_effect

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"

    with open(config_file, "r") as f:
      json_content = json.load(f)

    # must be HDP 2.3+
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version
    json_content['commandParams']['upgrade_direction'] = Direction.UPGRADE
    json_content['hostLevelParams']['stack_version'] = "2.3"

    # trigger the code to think it needs to copy the JAR
    json_content['configurations']['hive-site']['javax.jdo.option.ConnectionDriverName'] = "sap.jdbc4.sqlanywhere.IDriver"
    json_content['configurations']['hive-env']['hive_database'] = "Existing"
    json_content['configurations']['hive-env']['hive_database_type'] = "sqlanywhere"

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hive_metastore.py",
                       classname = "HiveMetastore",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hive-metastore', version), sudo=True,)


    # we don't care about configure here - the strings are different anyway because this
    # is an upgrade, so just pop those resources off of the call stack
    self.assertResourceCalledIgnoreEarlier('Directory', '/var/lib/hive', owner = 'hive', group = 'hadoop',
      mode = 0755, create_parents = True, cd_access = 'a')

    self.assertResourceCalled('Execute',
                              ('rm', '-f', '/usr/hdp/current/hive-metastore/lib/ojdbc6.jar'),
                              path=["/bin", "/usr/bin/"],
                              sudo = True)

    self.assertResourceCalled('File',
                              '/tmp/sqla-client-jdbc.tar.gz',
                              content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//sqla-client-jdbc.tar.gz')
                              )

    self.assertResourceCalled('Execute',
                              ('tar', '-xvf', '/tmp/sqla-client-jdbc.tar.gz', '-C', '/tmp'),
                              sudo = True)

    self.assertResourceCalled('Execute',
                              ('yes | ambari-sudo.sh cp /tmp/sqla-client-jdbc/java/* /usr/hdp/current/hive-metastore/lib'))

    self.assertResourceCalled('Directory',
                              '/usr/hdp/current/hive-metastore/lib/native/lib64',
                              create_parents = True)

    self.assertResourceCalled('Execute',
                              ('yes | ambari-sudo.sh cp /tmp/sqla-client-jdbc/native/lib64/* /usr/hdp/current/hive-metastore/lib/native/lib64'))

    self.assertResourceCalled('Execute',
                              ('ambari-sudo.sh chown -R hive:hadoop /usr/hdp/current/hive-metastore/lib/*'))

    self.assertResourceCalled('File', '/usr/hdp/2.3.0.0-1234/hive/lib/sqla-client-jdbc.tar.gz',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/hive-metastore/conf/conf.server/hadoop-metrics2-hivemetastore.properties',
        content = Template('hadoop-metrics2-hivemetastore.properties.j2'),
        owner = 'hive',
        group = 'hadoop',
        mode = 0600
    )
    self.assertResourceCalled('File', '/tmp/start_metastore_script',
        content = StaticFile('startMetastore.sh'),
        mode = 0755,
    )
    self.assertResourceCalled('Execute', ('rm', '-f', '/usr/hdp/current/hive-metastore/lib/ojdbc6.jar'),
        path = ['/bin', '/usr/bin/'],
        sudo = True,
    )
    self.assertResourceCalled('File', '/tmp/sqla-client-jdbc.tar.gz',
        content = DownloadSource('http://c6401.ambari.apache.org:8080/resources//sqla-client-jdbc.tar.gz'),
    )
    self.assertResourceCalled('Execute', ('tar', '-xvf', u'/tmp/sqla-client-jdbc.tar.gz', '-C', '/tmp'),
        sudo = True,
    )
    self.assertResourceCalled('Execute', 'yes | ambari-sudo.sh cp /tmp/sqla-client-jdbc/java/* /usr/hdp/current/hive-metastore/lib',)
    self.assertResourceCalled('Directory', '/usr/hdp/current/hive-metastore/lib/native/lib64',
        create_parents = True,
    )
    self.assertResourceCalled('Execute', 'yes | ambari-sudo.sh cp /tmp/sqla-client-jdbc/native/lib64/* /usr/hdp/current/hive-metastore/lib/native/lib64',)
    self.assertResourceCalled('Execute', 'ambari-sudo.sh chown -R hive:hadoop /usr/hdp/current/hive-metastore/lib/*',)
    self.assertResourceCalled('File', '/usr/hdp/2.3.0.0-1234/hive/lib/sqla-client-jdbc.tar.gz',
        mode = 0644,
    )
    self.assertResourceCalled('Execute',
                              ('yes | ambari-sudo.sh cp /usr/hdp/current/hive-metastore/lib/*.jar /usr/hdp/2.3.0.0-1234/hive/lib'))

    self.assertResourceCalled('Directory',
                              '/usr/hdp/2.3.0.0-1234/hive/lib/native/lib64',
                              create_parents = True)

    self.assertResourceCalled('Execute',
                              ('yes | ambari-sudo.sh cp /usr/hdp/current/hive-metastore/lib/native/lib64/* /usr/hdp/2.3.0.0-1234/hive/lib/native/lib64'))

    self.assertResourceCalled('Execute',
                              ('ambari-sudo.sh chown -R hive:hadoop /usr/hdp/current/hive-metastore/lib/*'))

    self.assertResourceCalled('File', '/usr/hdp/2.3.0.0-1234/hive/lib/sqla-client-jdbc.tar.gz',
                              mode = 0644,
                              )

    self.assertResourceCalled('Execute', "/usr/hdp/2.3.0.0-1234/hive/bin/schematool -dbType sqlanywhere -upgradeSchema",
                              logoutput = True, environment = {'HIVE_CONF_DIR': '/usr/hdp/current/hive-metastore/conf/conf.server'},
                              tries = 1, user = 'hive')


    self.assertNoMoreResources()
