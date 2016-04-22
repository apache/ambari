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

from stacks.utils.RMFTestCase import *

from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestAtlasServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "ATLAS/0.1.0.2.3/package"
  STACK_VERSION = "2.5"

  def configureResourcesCalled(self):
    self.assertResourceCalled('Directory', '/var/run/atlas',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('Directory', '/etc/atlas/conf',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('Directory', '/var/log/atlas',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/atlas-server/hbase/logs',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/atlas-server/data',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('Directory', '/var/lib/atlas/data',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0644
                              )
    self.assertResourceCalled('Directory', '/var/lib/atlas/server/webapp',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0644
                              )
    self.assertResourceCalled('File', '/var/lib/atlas/server/webapp/atlas.war',
                              content = StaticFile('/usr/hdp/current/atlas-server/server/webapp/atlas.war'),
                              )
    appprops =  dict(self.getConfig()['configurations'][
                       'application-properties'])
    appprops['atlas.http.authentication.kerberos.name.rules'] = ' \\ \n'.join(appprops['atlas.http.authentication.kerberos.name.rules'].splitlines())
    appprops['atlas.server.bind.address'] = 'c6401.ambari.apache.org'

    self.assertResourceCalled('PropertiesFile',
                              '/etc/atlas/conf/atlas-application.properties',
                              properties=appprops,
                              owner='atlas',
                              group='hadoop',
                              mode=0644,
                              )
    self.assertResourceCalled('File', '/etc/atlas/conf/atlas-env.sh',
                              content=InlineTemplate(
                                  self.getConfig()['configurations'][
                                    'atlas-env']['content']),
                              owner='atlas',
                              group='hadoop',
                              mode=0755,
                              )
    self.assertResourceCalled('File', '/etc/atlas/conf/atlas-log4j.xml',
                              content=InlineTemplate(
                                  self.getConfig()['configurations'][
                                    'atlas-log4j']['content']),
                              owner='atlas',
                              group='hadoop',
                              mode=0644,
                              )
    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
                              owner = 'atlas',
                              group = 'hadoop',
                              conf_dir = '/usr/hdp/current/atlas-server/hbase/conf',
                              configurations = self.getConfig()['configurations']['atlas-hbase-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['atlas-hbase-site']
                              )

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metadata_server.py",
                       classname = "MetadataServer",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )

    self.configureResourcesCalled()
    self.assertNoMoreResources()

