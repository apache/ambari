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
from stacks.utils.RMFTestCase import *

@patch("os.path.exists", new = MagicMock(return_value=True))
class TestHookBeforeStart(RMFTestCase):
  def test_hook_default(self):
    self.executeScript("1.3.2/hooks/before-START/scripts/hook.py",
                       classname="BeforeConfigureHook",
                       command="hook",
                       config_file="default.json"
    )
    self.assertResourceCalled('Execute', 'mkdir -p /tmp/HDP-artifacts/ ; curl -kf --retry 10 http://c6401.ambari.apache.org:8080/resources//jdk-7u45-linux-x64.tar.gz -o /tmp/HDP-artifacts//jdk-7u45-linux-x64.tar.gz',
                              not_if = 'test -e /usr/jdk64/jdk1.7.0_45/bin/java',
                              path = ['/bin', '/usr/bin/'],
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /usr/jdk64 ; cd /usr/jdk64 ; tar -xf /tmp/HDP-artifacts//jdk-7u45-linux-x64.tar.gz > /dev/null 2>&1',
                              not_if = 'test -e /usr/jdk64/jdk1.7.0_45/bin/java',
                              path = ['/bin', '/usr/bin/'],
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /tmp/HDP-artifacts/; curl -kf --retry 10 http://c6401.ambari.apache.org:8080/resources//UnlimitedJCEPolicyJDK7.zip -o /tmp/HDP-artifacts//UnlimitedJCEPolicyJDK7.zip',
                              not_if = 'test -e /tmp/HDP-artifacts//UnlimitedJCEPolicyJDK7.zip',
                              ignore_failures = True,
                              path = ['/bin', '/usr/bin/'],
                              )
    self.assertResourceCalled('Execute', '/bin/echo 0 > /selinux/enforce',
                              only_if = 'test -f /selinux/enforce',
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /usr/lib/hadoop/lib/native/Linux-i386-32; ln -sf /usr/lib/libsnappy.so /usr/lib/hadoop/lib/native/Linux-i386-32/libsnappy.so',
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /usr/lib/hadoop/lib/native/Linux-amd64-64; ln -sf /usr/lib64/libsnappy.so /usr/lib/hadoop/lib/native/Linux-amd64-64/libsnappy.so',
                              )
    self.assertResourceCalled('Directory', '/etc/hadoop/conf',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
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
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-env.sh',
                              content = Template('hadoop-env.sh.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/commons-logging.properties',
                              content = Template('commons-logging.properties.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/slaves',
                              content = Template('slaves.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/health_check',
                              content = Template('health_check.j2'),
                              owner = 'hdfs',
                              )
    rca_properties = format('''

log4j.appender.JHA=org.apache.ambari.log4j.hadoop.mapreduce.jobhistory.JobHistoryAppender
log4j.appender.JHA.database=jdbc:postgresql://c6401.ambari.apache.org/ambarirca
log4j.appender.JHA.driver=org.postgresql.Driver
log4j.appender.JHA.user=mapred
log4j.appender.JHA.password=mapred

log4j.logger.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger=DEBUG,JHA
log4j.additivity.org.apache.hadoop.mapred.JobHistory$JobHistoryLogger=true

''')
    self.maxDiff = None
    self.assertResourceCalled('File',
                              '/etc/hadoop/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hdfs',
                              content='log4jproperties\nline2log4jproperties\nline2'+rca_properties
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-metrics2.properties',
      content = Template('hadoop-metrics2.properties.j2'),
      owner = 'hdfs',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-queue-acls.xml',
      owner = 'mapred',
      group = 'hadoop',
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'mapred',
      group = 'hadoop',
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/task-log4j.properties',
      content = StaticFile('task-log4j.properties'),
      mode = 0755,
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
    )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/hadoop-tools.jar',
      to = '/usr/lib/hadoop/hadoop-tools.jar',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/configuration.xsl',
      owner = 'hdfs',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
      owner = 'mapred',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/masters',
      owner = 'hdfs',
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
    self.assertResourceCalled('File', '/etc/snmp/snmpd.conf',
                              content = Template('snmpd.conf.j2'),
                              )
    self.assertResourceCalled('Execute', 'service snmpd start; chkconfig snmpd on',
                              path = ['/usr/local/bin/:/bin/:/sbin/'],
                              )
    self.assertNoMoreResources()

  def test_hook_secured(self):
    self.executeScript("1.3.2/hooks/before-START/scripts/hook.py",
                       classname="BeforeConfigureHook",
                       command="hook",
                       config_file="secured.json"
    )
    self.assertResourceCalled('Execute', 'mkdir -p /tmp/HDP-artifacts/ ; curl -kf --retry 10 http://c6401.ambari.apache.org:8080/resources//jdk-7u45-linux-x64.tar.gz -o /tmp/HDP-artifacts//jdk-7u45-linux-x64.tar.gz',
                              not_if = 'test -e /usr/jdk64/jdk1.7.0_45/bin/java',
                              path = ['/bin', '/usr/bin/'],
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /usr/jdk64 ; cd /usr/jdk64 ; tar -xf /tmp/HDP-artifacts//jdk-7u45-linux-x64.tar.gz > /dev/null 2>&1',
                              not_if = 'test -e /usr/jdk64/jdk1.7.0_45/bin/java',
                              path = ['/bin', '/usr/bin/'],
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /tmp/HDP-artifacts/; curl -kf --retry 10 http://c6401.ambari.apache.org:8080/resources//UnlimitedJCEPolicyJDK7.zip -o /tmp/HDP-artifacts//UnlimitedJCEPolicyJDK7.zip',
                              not_if = 'test -e /tmp/HDP-artifacts//UnlimitedJCEPolicyJDK7.zip',
                              ignore_failures = True,
                              path = ['/bin', '/usr/bin/'],
                              )
    self.assertResourceCalled('Execute', 'rm -f local_policy.jar; rm -f US_export_policy.jar; unzip -o -j -q /tmp/HDP-artifacts//UnlimitedJCEPolicyJDK7.zip',
                              path = ['/bin/', '/usr/bin'],
                              only_if = 'test -e /usr/jdk64/jdk1.7.0_45/jre/lib/security && test -f /tmp/HDP-artifacts//UnlimitedJCEPolicyJDK7.zip',
                              cwd = '/usr/jdk64/jdk1.7.0_45/jre/lib/security',
                              )
    self.assertResourceCalled('Execute', '/bin/echo 0 > /selinux/enforce',
                              only_if = 'test -f /selinux/enforce',
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /usr/lib/hadoop/lib/native/Linux-i386-32; ln -sf /usr/lib/libsnappy.so /usr/lib/hadoop/lib/native/Linux-i386-32/libsnappy.so',
                              )
    self.assertResourceCalled('Execute', 'mkdir -p /usr/lib/hadoop/lib/native/Linux-amd64-64; ln -sf /usr/lib64/libsnappy.so /usr/lib/hadoop/lib/native/Linux-amd64-64/libsnappy.so',
                              )
    self.assertResourceCalled('Directory', '/etc/hadoop/conf',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/log/hadoop',
                              owner = 'root',
                              group = 'root',
                              recursive = True,
                              )
    self.assertResourceCalled('Directory', '/var/run/hadoop',
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
    self.assertResourceCalled('File', '/usr/lib/hadoop/bin/task-controller',
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
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-env.sh',
                              content = Template('hadoop-env.sh.j2'),
                              owner = 'root',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/commons-logging.properties',
                              content = Template('commons-logging.properties.j2'),
                              owner = 'root',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/slaves',
                              content = Template('slaves.j2'),
                              owner = 'root',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/health_check',
                              content = Template('health_check.j2'),
                              owner = 'root',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/log4j.properties',
                              owner = 'hdfs',
                              group = 'hadoop',
                              mode = 0644,
                              content = 'log4jproperties\nline2'
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/hadoop-metrics2.properties',
                              content = Template('hadoop-metrics2.properties.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-queue-acls.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['core-site'],
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/task-log4j.properties',
                              content = StaticFile('task-log4j.properties'),
                              mode = 0755,
                              )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['hdfs-site'],
                              )
    self.assertResourceCalled('Link', '/usr/lib/hadoop/lib/hadoop-tools.jar',
                              to = '/usr/lib/hadoop/hadoop-tools.jar',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/configuration.xsl',
                              owner = 'hdfs',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/masters',
                              owner = 'hdfs',
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
    self.assertResourceCalled('File', '/etc/snmp/snmpd.conf',
                              content = Template('snmpd.conf.j2'),
                              )
    self.assertResourceCalled('Execute', 'service snmpd start; chkconfig snmpd on',
                              path = ['/usr/local/bin/:/bin/:/sbin/'],
                             )
    self.assertNoMoreResources()

