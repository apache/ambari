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

from mock.mock import MagicMock, call, patch
from resource_management import *
from stacks.utils.RMFTestCase import *
import json

@patch("os.path.exists", new = MagicMock(return_value=True))
@patch.object(Hook, "run_custom_hook")
class TestHookBeforeStart(RMFTestCase):
  def test_hook_default(self, mockHook):

    self.executeScript("1.3.2/hooks/before-START/scripts/hook.py",
                       classname="BeforeStartHook",
                       command="hook",
                       config_file="default.json"
    )
    self.assertResourceCalled('Execute', '/bin/echo 0 > /selinux/enforce',
                              only_if = 'test -f /selinux/enforce',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0775,
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                            owner = 'root',
                            group = 'root',
                            recursive = True,
                            )
    self.assertResourceCalled('File', '/etc/hadoop/conf/commons-logging.properties',
                              content = Template('commons-logging.properties.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/health_check',
                              content = Template('health_check.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/log4j.properties',
                              content = 'log4jproperties\nline2log4jproperties\nline2\nambari.jobhistory.database=jdbc:postgresql://c6401.ambari.apache.org/ambarirca\nambari.jobhistory.driver=org.postgresql.Driver\nambari.jobhistory.user=mapred\nambari.jobhistory.password=mapred\nambari.jobhistory.logger=${hadoop.root.logger}\n\nlog4j.appender.JHA=org.apache.ambari.log4j.hadoop.mapreduce.jobhistory.JobHistoryAppender\nlog4j.appender.JHA.database=jdbc:postgresql://c6401.ambari.apache.org/ambarirca\nlog4j.appender.JHA.driver=org.postgresql.Driver\nlog4j.appender.JHA.user=mapred\nlog4j.appender.JHA.password=mapred\n\nlog4j.logger.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger=DEBUG,JHA\nlog4j.additivity.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger=true\n\n',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-metrics2.properties',
                              content = Template('hadoop-metrics2.properties.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/task-log4j.properties',
                              content = StaticFile('task-log4j.properties'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/hadoop-tools.jar',
                              to = '/usr/lib/hadoop/hadoop-tools.jar',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/configuration.xsl',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/masters',
      owner = 'hdfs',
      group = 'hadoop',
    )
    self.assertNoMoreResources()

  def test_hook_secured(self, mockHook):
    self.executeScript("1.3.2/hooks/before-START/scripts/hook.py",
                       classname="BeforeStartHook",
                       command="hook",
                       config_file="secured.json"
    )
    self.assertResourceCalled('Execute', '/bin/echo 0 > /selinux/enforce',
                              only_if = 'test -f /selinux/enforce',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0775,
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/health_check',
                              content = Template('health_check.j2'),
                              owner = 'root',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/log4j.properties',
                              content = 'log4jproperties\nline2',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-metrics2.properties',
                              content = Template('hadoop-metrics2.properties.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/task-log4j.properties',
                              content = StaticFile('task-log4j.properties'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/hadoop-tools.jar',
                              to = '/usr/lib/hadoop/hadoop-tools.jar',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/configuration.xsl',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/masters',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()

  def test_hook_default_hdfs(self, mockHook):
    config_file = "stacks/1.3.2/configs/default.json"
    with open(config_file, "r") as f:
      default_json = json.load(f)

    default_json['serviceName']= 'HDFS'
    self.executeScript("1.3.2/hooks/before-START/scripts/hook.py",
                       classname="BeforeStartHook",
                       command="hook",
                       config_dict=default_json
    )
    self.assertResourceCalled('Execute', '/bin/echo 0 > /selinux/enforce',
                              only_if = 'test -f /selinux/enforce',
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /usr/lib/hadoop/lib/native/Linux-i386-32; ln -sf /usr/lib/libsnappy.so /usr/lib/hadoop/lib/native/Linux-i386-32/libsnappy.so',
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /usr/lib/hadoop/lib/native/Linux-amd64-64; ln -sf /usr/lib64/libsnappy.so /usr/lib/hadoop/lib/native/Linux-amd64-64/libsnappy.so',
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop',
                              owner = 'root',
                              group = 'hadoop',
                              mode = 0775,
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/commons-logging.properties',
                              content = Template('commons-logging.properties.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/health_check',
                              content = Template('health_check.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/log4j.properties',
                              content = 'log4jproperties\nline2log4jproperties\nline2\nambari.jobhistory.database=jdbc:postgresql://c6401.ambari.apache.org/ambarirca\nambari.jobhistory.driver=org.postgresql.Driver\nambari.jobhistory.user=mapred\nambari.jobhistory.password=mapred\nambari.jobhistory.logger=${hadoop.root.logger}\n\nlog4j.appender.JHA=org.apache.ambari.log4j.hadoop.mapreduce.jobhistory.JobHistoryAppender\nlog4j.appender.JHA.database=jdbc:postgresql://c6401.ambari.apache.org/ambarirca\nlog4j.appender.JHA.driver=org.postgresql.Driver\nlog4j.appender.JHA.user=mapred\nlog4j.appender.JHA.password=mapred\n\nlog4j.logger.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger=DEBUG,JHA\nlog4j.additivity.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger=true\n\n',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-metrics2.properties',
                              content = Template('hadoop-metrics2.properties.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/task-log4j.properties',
                              content = StaticFile('task-log4j.properties'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/hadoop-tools.jar',
                              to = '/usr/lib/hadoop/hadoop-tools.jar',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/configuration.xsl',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/masters',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertNoMoreResources()
    
def test_that_jce_is_required_in_secured_cluster(self):
  try:
    self.executeScript("2.0.6/hooks/before-START/scripts/hook.py",
                       classname="BeforeStartHook",
                       command="hook",
                       config_file="secured_no_jce_name.json"
    )
    self.fail("Should throw an exception")
  except Fail:
    pass  # Expected
