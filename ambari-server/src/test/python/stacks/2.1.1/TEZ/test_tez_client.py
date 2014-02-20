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

class TestTezClient(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.1.1/services/TEZ/package/scripts/tez_client.py",
                       classname = "TezClient",
                       command = "configure",
                       config_file="default.json"
    )

    self.assertResourceCalled('Directory', '/etc/tez/conf',
      owner = 'tez',
      group = 'hadoop',
      recursive = True
    )

    self.assertResourceCalled('XmlConfig', 'tez-site.xml',
      owner = 'tez',
      group = 'hadoop',
      conf_dir = '/etc/tez/conf',
      configurations = self.getConfig()['configurations']['tez-site'],
      mode = 0664
    )

    self.assertResourceCalled('TemplateConfig', '/etc/tez/conf/tez-env.sh',
      owner = 'tez'
    )

    self.assertResourceCalled('HdfsDirectory', '/apps/tez/',
                              action = ['create_delayed'],
                              mode = 0755,
                              owner = 'tez',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit"
    )

    self.assertResourceCalled('HdfsDirectory', '/apps/tez/lib/',
                              action = ['create_delayed'],
                              mode = 0755,
                              owner = 'tez',
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = "/usr/bin/kinit"
    )
    self.assertResourceCalled('HdfsDirectory', None,
                              security_enabled = False,
                              keytab = UnknownConfigurationMock(),
                              conf_dir = '/etc/hadoop/conf',
                              hdfs_user = 'hdfs',
                              kinit_path_local = '/usr/bin/kinit',
                              action = ['create']
                              )

    self.assertResourceCalled('ExecuteHadoop', 'fs -copyFromLocal /usr/lib/tez/tez*.jar /apps/tez/',
                              not_if = ' hadoop fs -ls /tmp/tez_jars_copied >/dev/null 2>&1',
                              user = 'tez',
                              conf_dir = '/etc/hadoop/conf',
                              ignore_failures=True
    )

    self.assertResourceCalled('ExecuteHadoop', 'fs -copyFromLocal /usr/lib/tez/lib/*.jar /apps/tez/lib/',
                              not_if = ' hadoop fs -ls /tmp/tez_jars_copied >/dev/null 2>&1',
                              user = 'tez',
                              conf_dir = '/etc/hadoop/conf',
                              ignore_failures=True
    )

    self.assertResourceCalled('ExecuteHadoop', 'dfs -touchz /tmp/tez_jars_copied',
                              user = 'tez',
                              conf_dir = '/etc/hadoop/conf'
    )

    self.assertNoMoreResources()


