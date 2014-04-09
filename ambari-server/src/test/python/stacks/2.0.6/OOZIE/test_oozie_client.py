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

class TestOozieClient(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.0.6/services/OOZIE/package/scripts/oozie_client.py",
                       classname = "OozieClient",
                       command = "configure",
                       config_file="default.json"
    )
    # Hack for oozie.py changing conf on fly
    oozie_site = self.getConfig()['configurations']['oozie-site'].copy()
    oozie_site["oozie.services.ext"] = 'org.apache.oozie.service.JMSAccessorService,' + oozie_site["oozie.services.ext"]
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0o664,
                              conf_dir = '/etc/oozie/conf',
                              configurations = oozie_site,
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
        owner = 'oozie',
        group = 'hadoop',
        )
    self.assertResourceCalled('TemplateConfig', '/etc/oozie/conf/oozie-env.sh',
        owner = 'oozie',
        )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0o644,
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
    self.assertNoMoreResources()


  def test_configure_secured(self):
    self.executeScript("2.0.6/services/OOZIE/package/scripts/oozie_client.py",
                       classname = "OozieClient",
                       command = "configure",
                       config_file="secured.json"
    )
    # Hack for oozie.py changing conf on fly
    oozie_site = self.getConfig()['configurations']['oozie-site'].copy()
    oozie_site["oozie.services.ext"] = 'org.apache.oozie.service.JMSAccessorService,' + oozie_site["oozie.services.ext"]
    self.assertResourceCalled('XmlConfig', 'oozie-site.xml',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0o664,
                              conf_dir = '/etc/oozie/conf',
                              configurations = oozie_site,
                              )
    self.assertResourceCalled('Directory', '/etc/oozie/conf',
                              owner = 'oozie',
                              group = 'hadoop',
                              )
    self.assertResourceCalled('TemplateConfig', '/etc/oozie/conf/oozie-env.sh',
                              owner = 'oozie',
                              )
    self.assertResourceCalled('File', '/etc/oozie/conf/oozie-log4j.properties',
                              owner = 'oozie',
                              group = 'hadoop',
                              mode = 0o644,
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
    self.assertNoMoreResources()
