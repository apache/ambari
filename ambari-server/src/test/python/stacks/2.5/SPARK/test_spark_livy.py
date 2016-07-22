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
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *

from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
@patch("resource_management.libraries.functions.get_stack_version", new=MagicMock(return_value="2.5.0.0-1597"))
class TestSparkClient(RMFTestCase):
    COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.1/package"
    STACK_VERSION = "2.5"
    DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']
    def test_configure_default(self):
        self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/livy_server.py",
                           classname = "LivyServer",
                           command = "start",
                           config_file="default.json",
                           stack_version = self.STACK_VERSION,
                           target = RMFTestCase.TARGET_COMMON_SERVICES
                           )
        self.assert_start_default()
        self.assertNoMoreResources()

    def assert_start_default(self):
        self.assertResourceCalled('Directory', '/var/run/livy',
                                  owner = 'livy',
                                  group = 'hadoop',
                                  create_parents = True,
                                  mode = 0775
                                  )
        self.assertResourceCalled('Directory', '/var/log/livy',
                                  owner = 'livy',
                                  group = 'hadoop',
                                  create_parents = True,
                                  mode = 0775
                                  )
        self.assertResourceCalled('HdfsResource', '/user/livy',
                                  immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                                  security_enabled = False,
                                  hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
                                  keytab = UnknownConfigurationMock(),
                                  default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                                  hdfs_site = {u'a': u'b'},
                                  kinit_path_local = '/usr/bin/kinit',
                                  principal_name = UnknownConfigurationMock(),
                                  user = 'hdfs',
                                  owner = 'livy',
                                  hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
                                  type = 'directory',
                                  action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                                  dfs_type = '',
                                  mode = 0775,
                                  )
        self.assertResourceCalled('HdfsResource', None,
                                  immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
                                  security_enabled = False,
                                  hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
                                  keytab = UnknownConfigurationMock(),
                                  default_fs = 'hdfs://c6401.ambari.apache.org:8020',
                                  hdfs_site = {u'a': u'b'},
                                  kinit_path_local = '/usr/bin/kinit',
                                  principal_name = UnknownConfigurationMock(),
                                  user = 'hdfs',
                                  action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
                                  dfs_type = '',
                                  hadoop_conf_dir = '/usr/hdp/current/hadoop-client/conf',
                                  )
        self.assertResourceCalled('File', '/usr/hdp/current/livy-server/conf/livy-env.sh',
                                  content = InlineTemplate(self.getConfig()['configurations']['livy-env']['content']),
                                  owner = 'livy',
                                  group = 'livy',
                                  mode = 0644,
                                  )
        self.assertResourceCalled('PropertiesFile', '/usr/hdp/current/livy-server/conf/livy.conf',
                                  owner = 'livy',
                                  key_value_delimiter = ' ',
                                  group = 'livy',
                                  properties = self.getConfig()['configurations']['livy-conf'],
                                  )
        self.assertResourceCalled('File', '/usr/hdp/current/livy-server/conf/log4j.properties',
                                  content = '\n            # Set everything to be logged to the console\n            log4j.rootCategory=INFO, console\n            log4j.appender.console=org.apache.log4j.ConsoleAppender\n            log4j.appender.console.target=System.err\n            log4j.appender.console.layout=org.apache.log4j.PatternLayout\n            log4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{1}: %m%n\n\n            log4j.logger.org.eclipse.jetty=WARN',
                                  owner = 'livy',
                                  group = 'livy',
                                  mode = 0644,
                                  )
        self.assertResourceCalled('File', '/usr/hdp/current/livy-server/conf/spark-blacklist.conf',
                                  content = self.getConfig()['configurations']['livy-spark-blacklist']['content'],
                                  owner = 'livy',
                                  group = 'livy',
                                  mode = 0644,
                                  )
        self.assertResourceCalled('Directory', '/usr/hdp/current/livy-server/logs',
                                  owner = 'livy',
                                  group = 'livy',
                                  mode = 0755,
                                  )
        self.assertResourceCalled('Execute', '/usr/hdp/current/livy-server/bin/livy-server start',
                                  environment = {'JAVA_HOME': '/usr/jdk64/jdk1.7.0_45'},
                                  not_if = 'ls /var/run/livy/livy-livy-server.pid >/dev/null 2>&1 && ps -p `cat /var/run/livy/livy-livy-server.pid` >/dev/null 2>&1',
                                  user = 'livy'
                                  )
