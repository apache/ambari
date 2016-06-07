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
    self.assertResourceCalled('Directory', '/usr/hdp/current/atlas-server/data',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0644
                              )
    self.assertResourceCalled('Directory', '/usr/hdp/current/atlas-server/server/webapp',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0644
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/atlas-server/server/webapp/atlas.war',
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
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-solr-client',
                              owner='solr',
                              group='solr',
                              create_parents = True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('Directory', '/var/log/ambari-logsearch-solr-client',
                              owner='solr',
                              group='solr',
                              create_parents=True,
                              cd_access='a',
                              mode=0755
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh',
                              content=StaticFile('/usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh'),
                              owner='solr',
                              group='solr',
                              mode=0755,
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-solr-client/log4j.properties',
                              content=InlineTemplate(self.getConfig()['configurations'][
                                                       'logsearch-solr-client-log4j']['content']),
                              owner='solr',
                              group='solr',
                              mode=0644,
                              )
    self.assertResourceCalled('File', '/var/log/ambari-logsearch-solr-client/solr-client.log',
                              owner='solr',
                              group='solr',
                              mode=0644,
                              content = ''
                              )

    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org/logsearch --download-config -d /tmp/solr_config_basic_configs_0.[0-9]* -cs basic_configs -rt 5 -i 10')
    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org/logsearch --upload-config -d /usr/lib/ambari-logsearch-solr/server/solr/configsets/basic_configs/conf -cs basic_configs -rt 5 -i 10')

    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org/logsearch --create-collection -c vertex_index -cs basic_configs -s 1 -r 1 -m 1 -rt 5 -i 10')
    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org/logsearch --create-collection -c edge_index -cs basic_configs -s 1 -r 1 -m 1 -rt 5 -i 10')
    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh -z c6401.ambari.apache.org/logsearch --create-collection -c fulltext_index -cs basic_configs -s 1 -r 1 -m 1 -rt 5 -i 10')


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

