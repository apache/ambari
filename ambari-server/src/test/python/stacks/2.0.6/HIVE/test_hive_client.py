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

class TestHiveClient(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_client.py",
                       classname = "HiveClient",
                       command = "configure",
                       config_file="default_client.json"
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf.server',
        owner = 'hive',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
        group = 'hadoop',
        conf_dir = '/etc/hive/conf.server',
        mode = 0644,
        configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site'],
        owner = 'hive',
        configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-default.xml.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-exec-log4j.properties',
        content = 'log4jproperties\nline2',
        owner = 'hive',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-log4j.properties',
        content = 'log4jproperties\nline2',
        owner = 'hive',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
        owner = 'hive',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
        group = 'hadoop',
        conf_dir = '/etc/hive/conf',
        mode = 0644,
        configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site'],
        owner = 'hive',
        configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-exec-log4j.properties',
        content = 'log4jproperties\nline2',
        owner = 'hive',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-log4j.properties',
        content = 'log4jproperties\nline2',
        owner = 'hive',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf',
                              mode = 0644,
                              configuration_attributes = self.getConfig()['configuration_attributes']['hive-site'],
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Execute', '/bin/sh -c \'cd /usr/lib/ambari-agent/ && curl -kf -x "" --retry 5 http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar -o DBConnectionVerification.jar\'',
        environment = {'no_proxy': 'c6401.ambari.apache.org'},
        not_if = '[ -f DBConnectionVerification.jar]',
    )
    self.assertNoMoreResources()



  def test_configure_secured(self):
    self.executeScript("2.0.6/services/HIVE/package/scripts/hive_client.py",
                       classname = "HiveClient",
                       command = "configure",
                       config_file="secured_client.json"
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf.server',
        owner = 'hive',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
        group = 'hadoop',
        conf_dir = '/etc/hive/conf.server',
        mode = 0644,
        configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site'],
        owner = 'hive',
        configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-default.xml.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-env.sh.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-exec-log4j.properties',
        content = 'log4jproperties\nline2',
        owner = 'hive',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hive/conf.server/hive-log4j.properties',
        content = 'log4jproperties\nline2',
        owner = 'hive',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('Directory', '/etc/hive/conf',
        owner = 'hive',
        group = 'hadoop',
        recursive = True,
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
        group = 'hadoop',
        conf_dir = '/etc/hive/conf',
        mode = 0644,
        configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site'],
        owner = 'hive',
        configurations = self.getConfig()['configurations']['mapred-site'],
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-default.xml.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh.template',
        owner = 'hive',
        group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-exec-log4j.properties',
        content = 'log4jproperties\nline2',
        owner = 'hive',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-log4j.properties',
        content = 'log4jproperties\nline2',
        owner = 'hive',
        group = 'hadoop',
        mode = 0644,
    )
    self.assertResourceCalled('XmlConfig', 'hive-site.xml',
                              group = 'hadoop',
                              conf_dir = '/etc/hive/conf',
                              mode = 0644,
                              configuration_attributes = self.getConfig()['configuration_attributes']['hive-site'],
                              owner = 'hive',
                              configurations = self.getConfig()['configurations']['hive-site'],
                              )
    self.assertResourceCalled('File', '/etc/hive/conf/hive-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['hive-env']['content']),
                              owner = 'hive',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('Execute', '/bin/sh -c \'cd /usr/lib/ambari-agent/ && curl -kf -x "" --retry 5 http://c6401.ambari.apache.org:8080/resources/DBConnectionVerification.jar -o DBConnectionVerification.jar\'',
        environment = {'no_proxy': 'c6401.ambari.apache.org'},
        not_if = '[ -f DBConnectionVerification.jar]',
    )
    self.assertNoMoreResources()
