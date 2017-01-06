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

import os

from stacks.utils.RMFTestCase import *

from mock.mock import MagicMock, patch
from resource_management.libraries import functions
from resource_management.libraries.functions import format
from resource_management.core.logger import Logger


@patch("resource_management.libraries.Script.get_tmp_dir", new=MagicMock(return_value=('/var/lib/ambari-agent/tmp')))
@patch.object(functions, "get_stack_version", new=MagicMock(return_value="2.0.0.0-1234"))
class TestDruid(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "DRUID/0.9.2/package"
  STACK_VERSION = "2.6"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  def setUp(self):
    Logger.logger = MagicMock()
    self.testDirectory = os.path.dirname(os.path.abspath(__file__))
    self.num_times_to_iterate = 3
    self.wait_time = 1

  def test_configure_overlord(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/overlord.py",
                       classname="DruidOverlord",
                       command="configure",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       config_overrides = { 'role' : 'DRUID_OVERLORD' },
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-overlord')
    self.assertNoMoreResources()

  def test_start_overlord(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/overlord.py",
                       classname="DruidOverlord",
                       command="start",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_OVERLORD' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-overlord')
    self.assertResourceCalled('Execute', format("/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/druid-overlord/extensions/* org.apache.ambari.server.DBConnectionVerification 'jdbc:mysql://my-db-host:3306/druid?createDatabaseIfNotExist=true' druid diurd com.mysql.jdbc.Driver"),
                              user='druid',
                              tries=5,
                              try_sleep=10
                              )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-overlord/conf/druid-env.sh ; /usr/hdp/current/druid-overlord/bin/node.sh overlord start'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_stop_overlord(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/overlord.py",
                       classname="DruidOverlord",
                       command="stop",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_OVERLORD' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-overlord/conf/druid-env.sh ; /usr/hdp/current/druid-overlord/bin/node.sh overlord stop'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_configure_coordinator(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/coordinator.py",
                       classname="DruidCoordinator",
                       command="configure",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       config_overrides = { 'role' : 'DRUID_COORDINATOR' },
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-coordinator')
    self.assertNoMoreResources()

  def test_start_coordinator(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/coordinator.py",
                       classname="DruidCoordinator",
                       command="start",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_COORDINATOR' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-coordinator')
    self.assertResourceCalled('Execute', format("/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/druid-coordinator/extensions/* org.apache.ambari.server.DBConnectionVerification 'jdbc:mysql://my-db-host:3306/druid?createDatabaseIfNotExist=true' druid diurd com.mysql.jdbc.Driver"),
                              user='druid',
                              tries=5,
                              try_sleep=10
                              )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-coordinator/conf/druid-env.sh ; /usr/hdp/current/druid-coordinator/bin/node.sh coordinator start'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_stop_coordinator(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/coordinator.py",
                       classname="DruidCoordinator",
                       command="stop",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_COORDINATOR' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-coordinator/conf/druid-env.sh ; /usr/hdp/current/druid-coordinator/bin/node.sh coordinator stop'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_configure_broker(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/broker.py",
                       classname="DruidBroker",
                       command="configure",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       config_overrides = { 'role' : 'DRUID_BROKER' },
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-broker')
    self.assertNoMoreResources()

  def test_start_broker(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/broker.py",
                       classname="DruidBroker",
                       command="start",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_BROKER' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-broker')
    self.assertResourceCalled('Execute', format("/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/druid-broker/extensions/* org.apache.ambari.server.DBConnectionVerification 'jdbc:mysql://my-db-host:3306/druid?createDatabaseIfNotExist=true' druid diurd com.mysql.jdbc.Driver"),
                              user='druid',
                              tries=5,
                              try_sleep=10
                              )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-broker/conf/druid-env.sh ; /usr/hdp/current/druid-broker/bin/node.sh broker start'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_stop_broker(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/broker.py",
                       classname="DruidBroker",
                       command="stop",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_BROKER' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-broker/conf/druid-env.sh ; /usr/hdp/current/druid-broker/bin/node.sh broker stop'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_configure_router(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/router.py",
                       classname="DruidRouter",
                       command="configure",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       config_overrides = { 'role' : 'DRUID_ROUTER' },
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-router')
    self.assertNoMoreResources()

  def test_start_router(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/router.py",
                       classname="DruidRouter",
                       command="start",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_ROUTER' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-router')
    self.assertResourceCalled('Execute', format("/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/druid-router/extensions/* org.apache.ambari.server.DBConnectionVerification 'jdbc:mysql://my-db-host:3306/druid?createDatabaseIfNotExist=true' druid diurd com.mysql.jdbc.Driver"),
                              user='druid',
                              tries=5,
                              try_sleep=10
                              )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-router/conf/druid-env.sh ; /usr/hdp/current/druid-router/bin/node.sh router start'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_stop_router(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/router.py",
                       classname="DruidRouter",
                       command="stop",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_ROUTER' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-router/conf/druid-env.sh ; /usr/hdp/current/druid-router/bin/node.sh router stop'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_configure_historical(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historical.py",
                       classname="DruidHistorical",
                       command="configure",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       config_overrides = { 'role' : 'DRUID_HISTORICAL' },
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-historical')
    self.assertNoMoreResources()

  def test_start_historical(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historical.py",
                       classname="DruidHistorical",
                       command="start",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_HISTORICAL' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-historical')
    self.assertResourceCalled('Execute', format("/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/druid-historical/extensions/* org.apache.ambari.server.DBConnectionVerification 'jdbc:mysql://my-db-host:3306/druid?createDatabaseIfNotExist=true' druid diurd com.mysql.jdbc.Driver"),
                              user='druid',
                              tries=5,
                              try_sleep=10
                              )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-historical/conf/druid-env.sh ; /usr/hdp/current/druid-historical/bin/node.sh historical start'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_stop_historical(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/historical.py",
                       classname="DruidHistorical",
                       command="stop",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_HISTORICAL' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-historical/conf/druid-env.sh ; /usr/hdp/current/druid-historical/bin/node.sh historical stop'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_configure_middleManager(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/middlemanager.py",
                       classname="DruidMiddleManager",
                       command="configure",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       config_overrides = { 'role' : 'DRUID_MIDDLEMANAGER' },
                       stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-middlemanager')
    self.assertNoMoreResources()

  def test_start_middleManager(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/middlemanager.py",
                       classname="DruidMiddleManager",
                       command="start",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_MIDDLEMANAGER' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assert_configure_default('druid-middlemanager')
    self.assertResourceCalled('Execute', format("/usr/jdk64/jdk1.7.0_45/bin/java -cp /usr/lib/ambari-agent/DBConnectionVerification.jar:/usr/hdp/current/druid-middlemanager/extensions/* org.apache.ambari.server.DBConnectionVerification 'jdbc:mysql://my-db-host:3306/druid?createDatabaseIfNotExist=true' druid diurd com.mysql.jdbc.Driver"),
                              user='druid',
                              tries=5,
                              try_sleep=10
                              )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-middlemanager/conf/druid-env.sh ; /usr/hdp/current/druid-middlemanager/bin/node.sh middleManager start'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def test_stop_middleManager(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/middlemanager.py",
                       classname="DruidMiddleManager",
                       command="stop",
                       config_file=self.get_src_folder() + "/test/python/stacks/2.6/configs/default.json",
                       stack_version=self.STACK_VERSION,
                       config_overrides = { 'role' : 'DRUID_MIDDLEMANAGER' },
                       target=RMFTestCase.TARGET_COMMON_SERVICES
                       )
    self.assertResourceCalled('Execute', format('source /usr/hdp/current/druid-middlemanager/conf/druid-env.sh ; /usr/hdp/current/druid-middlemanager/bin/node.sh middleManager stop'),
                              user='druid'
                              )
    self.assertNoMoreResources()

  def assert_configure_default(self, role):

    self.assertResourceCalled('Directory', '/var/log/druid',
                              mode=0755,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', '/var/run/druid',
                              mode=0755,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', format('/usr/hdp/current/{role}/conf'),
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', format('/usr/hdp/current/{role}/conf/_common'),
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', format('/usr/hdp/current/{role}/conf/coordinator'),
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', format('/usr/hdp/current/{role}/conf/broker'),
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', format('/usr/hdp/current/{role}/conf/middleManager'),
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', format('/usr/hdp/current/{role}/conf/historical'),
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', format('/usr/hdp/current/{role}/conf/overlord'),
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', format('/usr/hdp/current/{role}/conf/router'),
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', '/apps/druid/segmentCache/info_dir',
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', '/apps/druid/tasks',
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', '/apps/druid/segmentCache',
                              mode=0700,
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('File', format('/usr/hdp/current/{role}/conf/druid-env.sh'),
                              owner = 'druid',
                              content = InlineTemplate(self.getConfig()['configurations']['druid-env']['content']),
                              mode = 0700
                              )
    druid_common_config = mutable_config_dict(self.getConfig()['configurations']['druid-common'])
    druid_common_config['druid.host'] = 'c6401.ambari.apache.org'
    druid_common_config['druid.extensions.directory'] = format('/usr/hdp/current/{role}/extensions')
    druid_common_config['druid.extensions.hadoopDependenciesDir'] = format('/usr/hdp/current/{role}/hadoop-dependencies')
    druid_common_config['druid.selectors.indexing.serviceName'] = 'druid/overlord'
    druid_common_config['druid.selectors.coordinator.serviceName'] = 'druid/coordinator'

    self.assertResourceCalled('PropertiesFile', 'common.runtime.properties',
                              dir=format("/usr/hdp/current/{role}/conf/_common"),
                              properties=druid_common_config,
                              owner='druid',
                              group='hadoop',
                              mode = 0600
                              )

    self.assertResourceCalled('File', format('/usr/hdp/current/{role}/conf/_common/druid-log4j.xml'),
                              mode=0644,
                              owner = 'druid',
                              group = 'hadoop',
                              content = InlineTemplate(self.getConfig()['configurations']['druid-log4j']['content'])
                              )

    self.assertResourceCalled('File', '/etc/logrotate.d/druid',
                              mode=0644,
                              owner = 'root',
                              group = 'root',
                              content = InlineTemplate(self.getConfig()['configurations']['druid-logrotate']['content'])
                              )

    self.assertResourceCalled('PropertiesFile', "runtime.properties",
                              dir=format('/usr/hdp/current/{role}/conf/coordinator'),
                              properties=self.getConfig()['configurations']['druid-coordinator'],
                              owner='druid',
                              group='hadoop',
                              mode = 0600
                              )

    self.assertResourceCalled('File', format("/usr/hdp/current/{role}/conf/coordinator/jvm.config"),
                              owner='druid',
                              group='hadoop',
                              content=InlineTemplate("-server \n-Xms{{node_heap_memory}}m \n-Xmx{{node_heap_memory}}m \n-XX:MaxDirectMemorySize={{node_direct_memory}}m \n-Dlog4j.configurationFile={{log4j_config_file}} \n-Dlog4j.debug \n{{node_jvm_opts}}",
                                                     node_heap_memory=1024,
                                                     node_direct_memory=2048,
                                                     node_jvm_opts='-Duser.timezone=UTC -Dfile.encoding=UTF-8',
                                                     log4j_config_file=format('/usr/hdp/current/{role}/conf/_common/druid-log4j.xml')
                                                     )
                              )

    self.assertResourceCalled('PropertiesFile', "runtime.properties",
                              dir=format('/usr/hdp/current/{role}/conf/overlord'),
                              properties=self.getConfig()['configurations']['druid-overlord'],
                              owner='druid',
                              group='hadoop',
                              mode = 0600
                              )

    self.assertResourceCalled('File', format("/usr/hdp/current/{role}/conf/overlord/jvm.config"),
                              owner='druid',
                              group='hadoop',
                              content=InlineTemplate("-server \n-Xms{{node_heap_memory}}m \n-Xmx{{node_heap_memory}}m \n-XX:MaxDirectMemorySize={{node_direct_memory}}m \n-Dlog4j.configurationFile={{log4j_config_file}} \n-Dlog4j.debug \n{{node_jvm_opts}}",
                                                     node_heap_memory=1024,
                                                     node_direct_memory=2048,
                                                     node_jvm_opts='-Duser.timezone=UTC -Dfile.encoding=UTF-8',
                                                     log4j_config_file=format('/usr/hdp/current/{role}/conf/_common/druid-log4j.xml')
                                                     )
                              )

    self.assertResourceCalled('PropertiesFile', "runtime.properties",
                              dir=format('/usr/hdp/current/{role}/conf/historical'),
                              properties=self.getConfig()['configurations']['druid-historical'],
                              owner='druid',
                              group='hadoop',
                              mode = 0600
                              )

    self.assertResourceCalled('File', format("/usr/hdp/current/{role}/conf/historical/jvm.config"),
                            owner='druid',
                            group='hadoop',
                            content=InlineTemplate("-server \n-Xms{{node_heap_memory}}m \n-Xmx{{node_heap_memory}}m \n-XX:MaxDirectMemorySize={{node_direct_memory}}m \n-Dlog4j.configurationFile={{log4j_config_file}} \n-Dlog4j.debug \n{{node_jvm_opts}}",
                                                   node_heap_memory=1024,
                                                   node_direct_memory=2048,
                                                   node_jvm_opts='-Duser.timezone=UTC -Dfile.encoding=UTF-8',
                                                   log4j_config_file=format('/usr/hdp/current/{role}/conf/_common/druid-log4j.xml')
                                                   )
                            )


    self.assertResourceCalled('PropertiesFile', "runtime.properties",
                          dir=format('/usr/hdp/current/{role}/conf/broker'),
                          properties=self.getConfig()['configurations']['druid-broker'],
                          owner='druid',
                          group='hadoop',
                          mode = 0600
                          )

    self.assertResourceCalled('File', format("/usr/hdp/current/{role}/conf/broker/jvm.config"),
                          owner='druid',
                          group='hadoop',
                          content=InlineTemplate("-server \n-Xms{{node_heap_memory}}m \n-Xmx{{node_heap_memory}}m \n-XX:MaxDirectMemorySize={{node_direct_memory}}m \n-Dlog4j.configurationFile={{log4j_config_file}} \n-Dlog4j.debug \n{{node_jvm_opts}}",
                                                 node_heap_memory=1024,
                                                 node_direct_memory=2048,
                                                 node_jvm_opts='-Duser.timezone=UTC -Dfile.encoding=UTF-8',
                                                 log4j_config_file=format('/usr/hdp/current/{role}/conf/_common/druid-log4j.xml')
                                                 )
                          )


    self.assertResourceCalled('PropertiesFile', "runtime.properties",
                          dir=format('/usr/hdp/current/{role}/conf/middleManager'),
                          properties=self.getConfig()['configurations']['druid-middlemanager'],
                          owner='druid',
                          group='hadoop',
                          mode = 0600
                          )

    self.assertResourceCalled('File', format("/usr/hdp/current/{role}/conf/middleManager/jvm.config"),
                          owner='druid',
                          group='hadoop',
                          content=InlineTemplate("-server \n-Xms{{node_heap_memory}}m \n-Xmx{{node_heap_memory}}m \n-XX:MaxDirectMemorySize={{node_direct_memory}}m \n-Dlog4j.configurationFile={{log4j_config_file}} \n-Dlog4j.debug \n{{node_jvm_opts}}",
                                                 node_heap_memory=1024,
                                                 node_direct_memory=2048,
                                                 node_jvm_opts='-Duser.timezone=UTC -Dfile.encoding=UTF-8',
                                                 log4j_config_file=format('/usr/hdp/current/{role}/conf/_common/druid-log4j.xml')
                                                 )
                          )

    self.assertResourceCalled('PropertiesFile', "runtime.properties",
                              dir=format('/usr/hdp/current/{role}/conf/router'),
                              properties=self.getConfig()['configurations']['druid-router'],
                              owner='druid',
                              group='hadoop',
                              mode = 0600
                              )

    self.assertResourceCalled('File', format("/usr/hdp/current/{role}/conf/router/jvm.config"),
                              owner='druid',
                              group='hadoop',
                              content=InlineTemplate("-server \n-Xms{{node_heap_memory}}m \n-Xmx{{node_heap_memory}}m \n-XX:MaxDirectMemorySize={{node_direct_memory}}m \n-Dlog4j.configurationFile={{log4j_config_file}} \n-Dlog4j.debug \n{{node_jvm_opts}}",
                                                     node_heap_memory=1024,
                                                     node_direct_memory=2048,
                                                     node_jvm_opts='-Duser.timezone=UTC -Dfile.encoding=UTF-8',
                                                     log4j_config_file=format('/usr/hdp/current/{role}/conf/_common/druid-log4j.xml')
                                                     )
                              )

    self.assertResourceCalled('HdfsResource', '/user/druid',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              keytab = UnknownConfigurationMock(),
                              default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_site = {u'a': u'b'},
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = 'missing_principal',
                              user = 'hdfs',
                              owner = 'druid',
                              hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              dfs_type = '',
                              recursive_chown=True,
                              recursive_chmod=True
                              )

    self.assertResourceCalled('HdfsResource', '/user/druid/data',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              keytab = UnknownConfigurationMock(),
                              default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_site = {u'a': u'b'},
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = 'missing_principal',
                              user = 'hdfs',
                              owner = 'druid',
                              hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              dfs_type = '',
                              mode=0755
                              )

    self.assertResourceCalled('HdfsResource', '/tmp/druid-indexing',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              keytab = UnknownConfigurationMock(),
                              default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_site = {u'a': u'b'},
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = 'missing_principal',
                              user = 'hdfs',
                              owner = 'druid',
                              hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              dfs_type = '',
                              mode=0755
                              )

    self.assertResourceCalled('HdfsResource', '/user/druid/logs',
                              immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                              security_enabled = False,
                              hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
                              keytab = UnknownConfigurationMock(),
                              default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                              hdfs_site = {u'a': u'b'},
                              kinit_path_local = '/usr/bin/kinit',
                              principal_name = 'missing_principal',
                              user = 'hdfs',
                              owner = 'druid',
                              hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
                              type = 'directory',
                              action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                              dfs_type = '',
                              mode=0755
                              )

    self.assertResourceCalled('File', format("/usr/lib/ambari-agent/DBConnectionVerification.jar"),
                              content= DownloadSource('http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar')
                              )

    self.assertResourceCalled('File', format("/tmp/mysql-connector-java.jar"),
                              content= DownloadSource('http://c6401.ambari.apache.org:8080/resources//mysql-connector-java.jar')
                              )
    self.assertResourceCalled('Execute',
                              ('cp', '--remove-destination', '/tmp/mysql-connector-java.jar', format('/usr/hdp/current/{role}/extensions/mysql-metadata-storage/mysql-connector-java.jar')),
                              path =  ['/bin', '/usr/bin/'],
                              sudo =  True
                              )

    self.assertResourceCalled('File', format("/usr/hdp/current/{role}/extensions/mysql-metadata-storage/mysql-connector-java.jar"),
                              owner = "druid",
                              group = "hadoop"
                              )

    self.assertResourceCalled('Directory', format('/usr/hdp/current/{role}/extensions'),
                              mode=0755,
                              cd_access='a',
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Directory', format('/usr/hdp/current/{role}/hadoop-dependencies'),
                              mode=0755,
                              cd_access='a',
                              owner='druid',
                              group='hadoop',
                              create_parents=True,
                              recursive_ownership=True
                              )

    self.assertResourceCalled('Execute', format("source /usr/hdp/current/{role}/conf/druid-env.sh ; java -classpath '/usr/hdp/current/{role}/lib/*' -Ddruid.extensions.loadList=[] -Ddruid.extensions.directory=/usr/hdp/current/{role}/extensions -Ddruid.extensions.hadoopDependenciesDir=/usr/hdp/current/{role}/hadoop-dependencies io.druid.cli.Main tools pull-deps -c custom-druid-extension --no-default-hadoop -r http://custom-mvn-repo/public/release"),
                              user='druid'
                              )


def mutable_config_dict(config):
  rv = {}
  for key, value in config.iteritems():
    rv[key] = value
  return rv
