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
@patch("resource_management.libraries.functions.get_stack_version", new=MagicMock(return_value="2.3.0.0-1597"))
class TestSparkClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.1/package"
  STACK_VERSION = "2.2"

  CONFIG_OVERRIDES = {"serviceName":"SPARK", "role":"SPARK_CLIENT"}

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/spark_client.py",
                   classname = "SparkClient",
                   command = "configure",
                   config_file="default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/spark_client.py",
                   classname = "SparkClient",
                   command = "configure",
                   config_file="secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/spark',
        owner = 'spark',
        group = 'hadoop',
        create_parents = True,
        mode = 0o775,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/spark',
        owner = 'spark',
        group = 'hadoop',
        create_parents = True,
        mode = 0o775,
        cd_access = 'a',
    )
    self.assertResourceCalled('PropertiesFile', '/usr/hdp/current/spark-client/conf/spark-defaults.conf',
        owner = 'spark',
        key_value_delimiter = ' ',
        group = 'spark',
        properties = self.getConfig()['configurations']['spark-defaults'],
        mode = 0o644
    )
    self.assertResourceCalled('File', '/usr/hdp/current/spark-client/conf/spark-env.sh',
        content = InlineTemplate(self.getConfig()['configurations']['spark-env']['content']),
        owner = 'spark',
        group = 'spark',
        mode = 0o644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/spark-client/conf/log4j.properties',
        content = '\n# Set everything to be logged to the console\nlog4j.rootCategory=INFO, console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{1}: %m%n\n\n# Settings to quiet third party logs that are too verbose\nlog4j.logger.org.eclipse.jetty=WARN\nlog4j.logger.org.eclipse.jetty.util.component.AbstractLifeCycle=ERROR\nlog4j.logger.org.apache.spark.repl.SparkIMain$exprTyper=INFO\nlog4j.logger.org.apache.spark.repl.SparkILoop$SparkILoopInterpreter=INFO',
        owner = 'spark',
        group = 'spark',
        mode = 0o644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/spark-client/conf/metrics.properties',
        content = InlineTemplate(self.getConfig()['configurations']['spark-metrics-properties']['content']),
        owner = 'spark',
        group = 'spark',
        mode = 0o644
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/spark-client/logs',
        owner = 'spark',
        group = 'spark',
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/spark-client/conf/java-opts',
        content = InlineTemplate('  -Dhdp.version=None'),
        owner = 'spark',
        group = 'spark',
        mode = 0o644
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/var/run/spark',
        owner = 'spark',
        group = 'hadoop',
        create_parents = True,
        mode = 0o775,
        cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/spark',
        owner = 'spark',
        group = 'hadoop',
        create_parents = True,
        mode = 0o775,
        cd_access = 'a',
    )
    self.assertResourceCalled('PropertiesFile', '/usr/hdp/current/spark-client/conf/spark-defaults.conf',
        owner = 'spark',
        key_value_delimiter = ' ',
        group = 'spark',
        properties = self.getConfig()['configurations']['spark-defaults'],
        mode = 0o644
    )
    self.assertResourceCalled('File', '/usr/hdp/current/spark-client/conf/spark-env.sh',
        content = InlineTemplate(self.getConfig()['configurations']['spark-env']['content']),
        owner = 'spark',
        group = 'spark',
        mode = 0o644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/spark-client/conf/log4j.properties',
        content = '\n# Set everything to be logged to the console\nlog4j.rootCategory=INFO, console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{1}: %m%n\n\n# Settings to quiet third party logs that are too verbose\nlog4j.logger.org.eclipse.jetty=WARN\nlog4j.logger.org.eclipse.jetty.util.component.AbstractLifeCycle=ERROR\nlog4j.logger.org.apache.spark.repl.SparkIMain$exprTyper=INFO\nlog4j.logger.org.apache.spark.repl.SparkILoop$SparkILoopInterpreter=INFO',
        owner = 'spark',
        group = 'spark',
        mode = 0o644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/spark-client/conf/metrics.properties',
        content = InlineTemplate(self.getConfig()['configurations']['spark-metrics-properties']['content']),
        owner = 'spark',
        group = 'spark',
        mode = 0o644
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/spark-client/logs',
        owner = 'spark',
        group = 'spark',
        mode = 0o755,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/spark-client/conf/java-opts',
        content = InlineTemplate('  -Dhdp.version=None'),
        owner = 'spark',
        group = 'spark',
        mode = 0o644
    )

  def test_pre_upgrade_restart_23(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/spark_client.py",
                       classname = "SparkClient",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'spark-client', version), sudo=True)
    self.assertNoMoreResources()


  def test_stack_upgrade_save_new_config(self):
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/spark_client.py",
                       classname = "SparkClient",
                       command = "stack_upgrade_save_new_config",
                       config_dict = json_content,
                       config_overrides =  self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict = mocks_dict)
    # for now, it's enough to know the method didn't fail
