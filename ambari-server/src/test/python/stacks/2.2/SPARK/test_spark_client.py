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
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *

@patch("resource_management.libraries.functions.get_hdp_version", new=MagicMock(return_value="2.3.0.0-1597"))
class TestSparkClient(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.0.2.2/package"
  STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/spark_client.py",
                   classname = "SparkClient",
                   command = "configure",
                   config_file="default.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_default()
    self.assertNoMoreResources()
    
  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/spark_client.py",
                   classname = "SparkClient",
                   command = "configure",
                   config_file="secured.json",
                   hdp_stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  def assert_configure_default(self):
    self.assertResourceCalled('Directory', '/var/run/spark',
        owner = 'spark',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/spark',
        owner = 'spark',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('PropertiesFile', '/etc/spark/conf/spark-defaults.conf',
        key_value_delimiter = ' ',
        properties = self.getConfig()['configurations']['spark-defaults'],
    )
    self.assertResourceCalled('File', '/etc/spark/conf/spark-env.sh',
        content = InlineTemplate(self.getConfig()['configurations']['spark-env']['content']),
        owner = 'spark',
        group = 'spark',
    )
    self.assertResourceCalled('File', '/etc/spark/conf/log4j.properties',
        content = '\n# Set everything to be logged to the console\nlog4j.rootCategory=INFO, console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{1}: %m%n\n\n# Settings to quiet third party logs that are too verbose\nlog4j.logger.org.eclipse.jetty=WARN\nlog4j.logger.org.eclipse.jetty.util.component.AbstractLifeCycle=ERROR\nlog4j.logger.org.apache.spark.repl.SparkIMain$exprTyper=INFO\nlog4j.logger.org.apache.spark.repl.SparkILoop$SparkILoopInterpreter=INFO',
        owner = 'spark',
        group = 'spark',
    )
    self.assertResourceCalled('File', '/etc/spark/conf/metrics.properties',
        content = InlineTemplate(self.getConfig()['configurations']['spark-metrics-properties']['content']),
        owner = 'spark',
        group = 'spark',
    )
    self.assertResourceCalled('File', '/etc/spark/conf/java-opts',
        content = '  -Dhdp.version=2.3.0.0-1597',
        owner = 'spark',
        group = 'spark',
    )
      
  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/var/run/spark',
        owner = 'spark',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/spark',
        owner = 'spark',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('PropertiesFile', '/etc/spark/conf/spark-defaults.conf',
        key_value_delimiter = ' ',
        properties = self.getConfig()['configurations']['spark-defaults'],
    )
    self.assertResourceCalled('File', '/etc/spark/conf/spark-env.sh',
        content = InlineTemplate(self.getConfig()['configurations']['spark-env']['content']),
        owner = 'spark',
        group = 'spark',
    )
    self.assertResourceCalled('File', '/etc/spark/conf/log4j.properties',
        content = '\n# Set everything to be logged to the console\nlog4j.rootCategory=INFO, console\nlog4j.appender.console=org.apache.log4j.ConsoleAppender\nlog4j.appender.console.target=System.err\nlog4j.appender.console.layout=org.apache.log4j.PatternLayout\nlog4j.appender.console.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{1}: %m%n\n\n# Settings to quiet third party logs that are too verbose\nlog4j.logger.org.eclipse.jetty=WARN\nlog4j.logger.org.eclipse.jetty.util.component.AbstractLifeCycle=ERROR\nlog4j.logger.org.apache.spark.repl.SparkIMain$exprTyper=INFO\nlog4j.logger.org.apache.spark.repl.SparkILoop$SparkILoopInterpreter=INFO',
        owner = 'spark',
        group = 'spark',
    )
    self.assertResourceCalled('File', '/etc/spark/conf/metrics.properties',
        content = InlineTemplate(self.getConfig()['configurations']['spark-metrics-properties']['content']),
        owner = 'spark',
        group = 'spark',
    )
    self.assertResourceCalled('File', '/etc/spark/conf/java-opts',
        content = '  -Dhdp.version=2.3.0.0-1597',
        owner = 'spark',
        group = 'spark',
    )
