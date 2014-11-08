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

class TestKnoxGateway(RMFTestCase):

  def test_configure_default(self):
    self.executeScript("2.2/services/KNOX/package/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command = "configure",
                       config_file="default.json"
    )

    self.assertResourceCalled('Directory', '/etc/knox/conf',
                              owner = 'knox',
                              group = 'knox',
                              recursive = True
    )

    self.assertResourceCalled('XmlConfig', 'gateway-site.xml',
                              owner = 'knox',
                              group = 'knox',
                              conf_dir = '/etc/knox/conf',
                              configurations = self.getConfig()['configurations']['gateway-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['gateway-site']
    )

    self.assertResourceCalled('File', '/etc/knox/conf/gateway-log4j.properties',
                              mode=0644,
                              group='knox',
                              owner = 'knox',
                              content = self.getConfig()['configurations']['gateway-log4j']['content']
    )
    self.assertResourceCalled('File', '/etc/knox/conf/topologies/default.xml',
                              group='knox',
                              owner = 'knox',
                              content = InlineTemplate(self.getConfig()['configurations']['topology']['content'])
    )
    self.assertResourceCalled('Execute', 'chown -R knox:knox /var/lib/knox/data /var/log/knox /var/run/knox /etc/knox/conf'
    )
    self.assertResourceCalled('Execute', '/usr/lib/knox/bin/knoxcli.sh create-master --master sa',
                              user='knox',
                              environment={'JAVA_HOME': '/usr/jdk64/jdk1.7.0_45'},
                              not_if='test -f /var/lib/knox/data/security/master'
    )
    self.assertResourceCalled('Execute', '/usr/lib/knox/bin/knoxcli.sh create-cert --hostname c6401.ambari.apache.org',
                              user='knox',
                              environment={'JAVA_HOME': '/usr/jdk64/jdk1.7.0_45'},
                              not_if='test -f /var/lib/knox/data/security/keystores/gateway.jks'
    )
    self.assertResourceCalled('File', '/etc/knox/conf/ldap-log4j.properties',
                              mode=0644,
                              group='knox',
                              owner = 'knox',
                              content = self.getConfig()['configurations']['ldap-log4j']['content']
    )
    self.assertResourceCalled('File', '/etc/knox/conf/users.ldif',
                              mode=0644,
                              group='knox',
                              owner = 'knox',
                              content = self.getConfig()['configurations']['users-ldif']['content']
    )

    self.assertNoMoreResources()


