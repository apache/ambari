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
    self.executeScript("2.0.6/services/OOZIE/package/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "configure",
                       config_file="default.json"
    )
    self.assert_configure_default()
    self.assertNoMoreResources()


  @patch("os.path.isfile")
  def test_start_default(self, isfile_mock):
    isfile_mock.return_value = True
    self.executeScript("2.0.6/services/OOZIE/package/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "start",
                         config_file="default.json"
        )
    self.assert_configure_default()
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/ooziedb.sh create -sqlfile oozie.sql -run',
        not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
        ignore_failures = True,
        user = 'oozie',
        )
    self.assertResourceCalled('Execute', ' hadoop --config /etc/hadoop/conf dfs -put /usr/lib/oozie/share /user/oozie ; hadoop --config /etc/hadoop/conf dfs -chmod -R 755 /user/oozie/share',
        not_if = " hadoop --config /etc/hadoop/conf dfs -ls /user/oozie/share | awk 'BEGIN {count=0;} /share/ {count++} END {if (count > 0) {exit 0} else {exit 1}}'",
        user = 'oozie',
        path = ['/usr/bin:/usr/bin'],
        )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-start.sh',
        not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
        user = 'oozie',
        )
    self.assertNoMoreResources()


  def test_stop_default(self):
    self.executeScript("2.0.6/services/OOZIE/package/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "stop",
                         config_file="default.json"
    )
    self.assertResourceCalled('Execute', "su -s /bin/bash - oozie -c  'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-stop.sh' && rm -f /var/run/oozie/oozie.pid",
      only_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
    )
    self.assertNoMoreResources()


  def test_configure_secured(self):
    self.executeScript("2.0.6/services/OOZIE/package/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertNoMoreResources()

  @patch("os.path.isfile")
  def test_start_secured(self, isfile_mock):
    isfile_mock.return_value = True
    self.executeScript("2.0.6/services/OOZIE/package/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "start",
                         config_file="secured.json"
    )
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/ooziedb.sh create -sqlfile oozie.sql -run',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              ignore_failures = True,
                              user = 'oozie',
                              )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/oozie.service.keytab oozie/c6402.ambari.apache.org@EXAMPLE.COM; hadoop --config /etc/hadoop/conf dfs -put /usr/lib/oozie/share /user/oozie ; hadoop --config /etc/hadoop/conf dfs -chmod -R 755 /user/oozie/share',
                              not_if = "/usr/bin/kinit -kt /etc/security/keytabs/oozie.service.keytab oozie/c6402.ambari.apache.org@EXAMPLE.COM; hadoop --config /etc/hadoop/conf dfs -ls /user/oozie/share | awk 'BEGIN {count=0;} /share/ {count++} END {if (count > 0) {exit 0} else {exit 1}}'",
                              user = 'oozie',
                              path = ['/usr/bin:/usr/bin'],
                              )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-start.sh',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              user = 'oozie',
                              )
    self.assertNoMoreResources()

  def test_stop_secured(self):
    self.executeScript("2.0.6/services/OOZIE/package/scripts/oozie_server.py",
                         classname = "OozieServer",
                         command = "stop",
                         config_file="secured.json"
    )
    self.assertResourceCalled('Execute', "su -s /bin/bash - oozie -c  'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-stop.sh' && rm -f /var/run/oozie/oozie.pid",
                              only_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              )
    self.assertNoMoreResources()


  def assert_configure_default(self):
    self.assertResourceCalled('HdfsDirectory', '/user/oozie',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0775,
                              owner = 'oozie',
                              bin_dir = '/usr/bin',
                              action = ['create'],
    )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursive = True
    )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0664,
                              conf_dir = '/etc/oozie/conf',
                              configurations = self.getConfig()['configurations']['oozie-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
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
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/conf',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server',
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
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && chown oozie:hadoop /var/tmp/oozie && mkdir -p /usr/lib/oozie/libext && cp /usr/share/HDP-oozie/ext-2.2.zip /usr/lib/oozie/libext && cp /usr/lib/falcon/oozie/ext/falcon-oozie-el-extension-*.jar /usr/lib/oozie/libext',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-setup.sh prepare-war',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              user = 'oozie',
                              )


  def assert_configure_secured(self):
    self.assertResourceCalled('HdfsDirectory', '/user/oozie',
                              security_enabled = True,
                              keytab = '/etc/security/keytabs/hdfs.headless.keytab',
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              mode = 0775,
                              owner = 'oozie',
                              bin_dir = '/usr/bin',
                              action = ['create'],
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursive = True
                              )
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0664,
                              conf_dir = '/etc/oozie/conf',
                              configurations = self.getConfig()['configurations']['oozie-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['oozie-site']
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
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server/conf',
                              owner = 'oozie',
                              recursive = True,
                              mode = 0755,
                              )
    self.assertResourceCalled('Directory', '/var/lib/oozie/oozie-server',
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
    self.assertResourceCalled('Execute', 'cd /usr/lib/oozie && chown oozie:hadoop /var/tmp/oozie && mkdir -p /usr/lib/oozie/libext && cp /usr/share/HDP-oozie/ext-2.2.zip /usr/lib/oozie/libext && cp /usr/lib/falcon/oozie/ext/falcon-oozie-el-extension-*.jar /usr/lib/oozie/libext',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              )
    self.assertResourceCalled('Execute', 'cd /var/tmp/oozie && /usr/lib/oozie/bin/oozie-setup.sh prepare-war',
                              not_if = 'ls /var/run/oozie/oozie.pid >/dev/null 2>&1 && ps `cat /var/run/oozie/oozie.pid` >/dev/null 2>&1',
                              user = 'oozie',
                              )

    def test_configure_default_hdp22(self):
      config_file = "stacks/2.0.6/configs/default.json"
      with open(config_file, "r") as f:
        default_json = json.load(f)

      default_json['hostLevelParams']['stack_version']= '2.2'
      self.executeScript("2.0.6/services/OOZIE/package/scripts/oozie_server.py",
                       classname = "OozieServer",
                       command = "configure",
                       config_file="default.json"
      )
      self.assert_configure_default()
      self.assertResourceCalled('Directory', '/etc/oozie/conf/action-conf/hive',
                              owner = 'oozie',
                              group = 'hadoop',
                              recursive = True
                              )
      self.assertResourceCalled('XmlConfig', 'hive-site',
                                owner = 'oozie',
                                group = 'hadoop',
                                mode = 0664,
                                conf_dir = '/etc/oozie/conf/action-conf/hive',
                                configurations = self.getConfig()['configurations']['hive-site'],
                                configuration_attributes = self.getConfig()['configuration_attributes']['hive-site']
      )
      self.assertResourceCalled('XmlConfig', 'tez-site',
                                owner = 'oozie',
                                group = 'hadoop',
                                mode = 0664,
                                conf_dir = '/etc/oozie/conf/action-conf/hive',
                                configurations = self.getConfig()['configurations']['tez-site'],
                                configuration_attributes = self.getConfig()['configuration_attributes']['tez-site']
      )
      self.assertNoMoreResources()



