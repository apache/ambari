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

class TestOozieServer(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("1.3.2/services/OOZIE/package/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "configure",
                       config_file="default.json"
    )
    self.assertResourceCalled('HdfsDirectory', '/user/oozie',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0775,
                              owner = 'oozie',
                              action = ['create'],
    )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
      owner = 'oozie',
      group = 'hadoop',
      mode = 0664,
      conf_dir = '/etc/oozie/conf',
      configurations = self.getConfig()['configurations']['oozie-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
    )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-env.sh',
      owner = 'oozie',
      content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content']),
    )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              content = 'log4jproperties\nline2'
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/adminusers.txt',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/oozie/conf/hadoop-config.xml',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-default.xml',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/oozie/conf/action-conf/hive.xml',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/var/run/oozie/oozie.pid',
                              action=["delete"],
                              not_if="ls {pid_file} >/dev/null 2>&1 && !(ps `cat {pid_file}` >/dev/null 2>&1)"
    )
    self.assertResourceCalled('Directory', '/var/run/oozie',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/log/oozie',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/tmp/oozie',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/hadoop/oozie/data',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/webapps/',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && tar -xvf oozie-sharelib.tar.gz',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
    )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && mkdir -p /var/tmp/oozie',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
    )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && chown oozie:hadoop /var/tmp/oozie',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
    )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-setup.sh -hadoop 0.20.200 /usr/lib/hadoop/ -extjs /usr/share/HDP-oozie/ext.zip -jars `LZO_JARS=($(find /usr/lib/hadoop/lib/ -name "hadoop-lzo-*")); echo ${LZO_JARS[0]}`:',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
      user = 'oozie',
    )
    self.assertNoMoreResources()


  def test_start_default(self):
    self.executeScript("1.3.2/services/OOZIE/package/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "start",
                         config_file="default.json"
    )
    self.configure_default()
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/ooziedb.sh create -sqlfile oozie.sql -run',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
      ignore_failures = True,
      user = 'oozie',
    )
    self.assertResourceCalled('Execute', ' hadoop dfs -put /usr/lib/oozie/share /user/oozie ; hadoop dfs -chmod -R 755 /user/oozie/share',
      not_if = " hadoop dfs -ls /user/oozie/share | awk 'BEGIN {count=0;} /share/ {count++} END {if (count > 0) {exit 0} else {exit 1}}'",
      user = 'oozie',
    )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-start.sh',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
      user = 'oozie',
    )
    self.assertNoMoreResources()



  def test_stop_default(self):
    self.executeScript("1.3.2/services/OOZIE/package/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "stop",
                         config_file="default.json"
    )
    self.assertResourceCalled('Execute', "su -s /bin/bash - oozie -c  'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-stop.sh' && rm -f /var/run/oozie/oozie.pid",
      only_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
    )
    self.assertNoMoreResources()


  def test_configure_secured(self):
    self.executeScript("1.3.2/services/OOZIE/package/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assertResourceCalled('HdfsDirectory', '/user/oozie',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0775,
                              owner = 'oozie',
                              action = ['create'],
    )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
      owner = 'oozie',
      group = 'hadoop',
      mode = 0664,
      conf_dir = '/etc/oozie/conf',
      configurations = self.getConfig()['configurations']['oozie-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
    )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-env.sh',
      owner = 'oozie',
      content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content'])
    )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              content = 'log4jproperties\nline2'
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/adminusers.txt',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/oozie/conf/hadoop-config.xml',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-default.xml',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/oozie/conf/action-conf/hive.xml',
      owner = 'oozie',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/var/run/oozie/oozie.pid',
                              action=["delete"],
                              not_if="ls {pid_file} >/dev/null 2>&1 && !(ps `cat {pid_file}` >/dev/null 2>&1)"
    )
    self.assertResourceCalled('Directory', '/var/run/oozie',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/log/oozie',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/tmp/oozie',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/hadoop/oozie/data',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/webapps/',
      owner = 'oozie',
      recursive = True,
      mode = 0755,
    )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && tar -xvf oozie-sharelib.tar.gz',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
    )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && mkdir -p /var/tmp/oozie',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
    )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && chown oozie:hadoop /var/tmp/oozie',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
    )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-setup.sh -hadoop 0.20.200 /usr/lib/hadoop/ -extjs /usr/share/HDP-oozie/ext.zip -jars `LZO_JARS=($(find /usr/lib/hadoop/lib/ -name "hadoop-lzo-*")); echo ${LZO_JARS[0]}`:',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
      user = 'oozie',
    )
    self.assertNoMoreResources()


  def test_start_secured(self):
    self.executeScript("1.3.2/services/OOZIE/package/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "start",
                         config_file="secured.json"
    )
    self.configure_secured()
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/ooziedb.sh create -sqlfile oozie.sql -run',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
      ignore_failures = True,
      user = 'oozie',
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/oozie.service.keytab oozie/c6402.ambari.apache.org@EXAMPLE.COM; hadoop dfs -put /usr/lib/oozie/share /user/oozie ; hadoop dfs -chmod -R 755 /user/oozie/share',
      not_if = "/usr/bin/kinit -kt /etc/security/keytabs/oozie.service.keytab oozie/c6402.ambari.apache.org@EXAMPLE.COM; hadoop dfs -ls /user/oozie/share | awk 'BEGIN {count=0;} /share/ {count++} END {if (count > 0) {exit 0} else {exit 1}}'",
      user = 'oozie',
    )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-start.sh',
      not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
      user = 'oozie',
    )
    self.assertNoMoreResources()


  def test_stop_secured(self):
    self.executeScript("1.3.2/services/OOZIE/package/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "stop",
                         config_file="secured.json"
    )
    self.assertResourceCalled('Execute', "su -s /bin/bash - oozie -c  'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-stop.sh' && rm -f /var/run/oozie/oozie.pid",
      only_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
    )
    self.assertNoMoreResources()

  def configure_default(self):
    self.assertResourceCalled('HdfsDirectory', '/user/oozie',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0775,
                              owner = 'oozie',
                              action = ['create'],
                              )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0664,
                              conf_dir = '/etc/oozie/conf',
                              configurations = self.getConfig()['configurations']['oozie-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-env.sh',
                              owner = 'oozie',
                              content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content'])
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              content = 'log4jproperties\nline2'
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/adminusers.txt',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/hadoop-config.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-default.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/action-conf/hive.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/var/run/oozie/oozie.pid',
                              action=["delete"],
                              not_if="ls {pid_file} >/dev/null 2>&1 && !(ps `cat {pid_file}` >/dev/null 2>&1)"
    )
    self.assertResourceCalled('Directory', '/var/run/oozie',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/oozie',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/tmp/oozie',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/hadoop/oozie/data',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/webapps/',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && tar -xvf oozie-sharelib.tar.gz',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && mkdir -p /var/tmp/oozie',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && chown oozie:hadoop /var/tmp/oozie',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-setup.sh -hadoop 0.20.200 /usr/lib/hadoop/ -extjs /usr/share/HDP-oozie/ext.zip -jars `LZO_JARS=($(find /usr/lib/hadoop/lib/ -name "hadoop-lzo-*")); echo ${LZO_JARS[0]}`:',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              user = 'oozie',
                              )

  def configure_secured(self):
    self.assertResourceCalled('HdfsDirectory', '/user/oozie',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0775,
                              owner = 'oozie',
                              action = ['create'],
                              )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0664,
                              conf_dir = '/etc/oozie/conf',
                              configurations = self.getConfig()['configurations']['oozie-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-env.sh',
                              owner = 'oozie',
                              content = InlineTemplate(self.getConfig()['configurations']['oozie-env']['content'])
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0644,
                              content = 'log4jproperties\nline2'
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/adminusers.txt',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/hadoop-config.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-default.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/action-conf/hive.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('File', '/var/run/oozie/oozie.pid',
                              action=["delete"],
                              not_if="ls {pid_file} >/dev/null 2>&1 && !(ps `cat {pid_file}` >/dev/null 2>&1)"
    )
    self.assertResourceCalled('Directory', '/var/run/oozie',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/log/oozie',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/tmp/oozie',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/hadoop/oozie/data',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/webapps/',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && tar -xvf oozie-sharelib.tar.gz',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && mkdir -p /var/tmp/oozie',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && chown oozie:hadoop /var/tmp/oozie',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-setup.sh -hadoop 0.20.200 /usr/lib/hadoop/ -extjs /usr/share/HDP-oozie/ext.zip -jars `LZO_JARS=($(find /usr/lib/hadoop/lib/ -name "hadoop-lzo-*")); echo ${LZO_JARS[0]}`:',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              user = 'oozie',
                              )