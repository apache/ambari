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
from resource_management.libraries.script.script import Script
import os

origin_exists = os.path.exists
@patch.object(os.path, "exists", new=MagicMock(
  side_effect=lambda *args: origin_exists(args[0])
  if args[0][-2:] == "j2" else True))
class TestYarnClient(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.0.6/services/YARN/package/scripts/yarn_client.py",
                       classname = "YarnClient",
                       command = "configure",
                       config_file="default.json"
    )

    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['yarn-site']
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
      configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn.exclude',
      owner = 'yarn',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['yarn-env']['content']),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['mapred-env']['content']),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
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
    self.assertNoMoreResources()

  def test_configure_secured(self):

    self.executeScript("2.0.6/services/YARN/package/scripts/yarn_client.py",
                       classname = "YarnClient",
                       command = "configure",
                       config_file="secured.json"
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['yarn-site']
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
      configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn.exclude',
      owner = 'yarn',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['yarn-env']['content']),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/usr/lib/hadoop-yarn/bin/container-executor',
      group = 'hadoop',
      mode = 06050,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/container-executor.cfg',
      content = Template('container-executor.cfg.j2'),
      group = 'hadoop',
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['mapred-env']['content']),
                              owner = 'root',
                              )
    self.assertResourceCalled('File', '/usr/lib/hadoop/sbin/task-controller',
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
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
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
    self.assertNoMoreResources()

  def test_restart_client(self):
    self.executeScript("2.0.6/services/YARN/package/scripts/yarn_client.py",
                       classname = "YarnClient",
                       command = "restart",
                       config_file="default.json",
                       config_overrides = { 'roleParams' : { "component_category": "CLIENT" } }
    )

    self.assertResourceCalled('Directory', '/var/run/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn/yarn',
      owner = 'yarn',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/run/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-mapreduce/mapred',
      owner = 'mapred',
      group = 'hadoop',
      recursive = True,
    )
    self.assertResourceCalled('Directory', '/var/log/hadoop-yarn',
      owner = 'yarn',
      recursive = True,
      ignore_failures = True,
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hdfs',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['mapred-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
    )
    self.assertResourceCalled('XmlConfig', 'yarn-site.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['yarn-site'],
      configuration_attributes = self.getConfig()['configuration_attributes']['yarn-site']
    )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
      owner = 'yarn',
      group = 'hadoop',
      mode = 0644,
      conf_dir = '/etc/hadoop/conf',
      configurations = self.getConfig()['configurations']['capacity-scheduler'],
      configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn.exclude',
      owner = 'yarn',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/yarn.conf',
      content = Template('yarn.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/mapreduce.conf',
      content = Template('mapreduce.conf.j2'),
      mode = 0644,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/yarn-env.sh',
      content = InlineTemplate(self.getConfig()['configurations']['yarn-env']['content']),
      owner = 'yarn',
      group = 'hadoop',
      mode = 0755,
    )
    self.assertResourceCalled('File', '/etc/hadoop/conf/mapred-env.sh',
                              content = InlineTemplate(self.getConfig()['configurations']['mapred-env']['content']),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/taskcontroller.cfg',
                              content = Template('taskcontroller.cfg.j2'),
                              owner = 'hdfs',
                              )
    self.assertResourceCalled('XmlConfig', 'mapred-site.xml',
                              owner = 'mapred',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['mapred-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['mapred-site']
                              )
    self.assertResourceCalled('XmlConfig', 'capacity-scheduler.xml',
                              owner = 'hdfs',
                              group = 'hadoop',
                              conf_dir = '/etc/hadoop/conf',
                              configurations = self.getConfig()['configurations']['capacity-scheduler'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['capacity-scheduler']
                              )
    self.assertResourceCalled('File', '/etc/hadoop/conf/fair-scheduler.xml',
                              owner = 'mapred',
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
    self.assertNoMoreResources()


