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
    # Both server and client
    self.assertResourceCalled('Directory', '/etc/atlas/conf',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0755
    )
    # Pid dir
    self.assertResourceCalled('Directory', '/var/run/atlas',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0755
    )
    self.assertResourceCalled('Directory', '/etc/atlas/conf/solr',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0755,
                              recursive_ownership = True
    )
    # Log dir
    self.assertResourceCalled('Directory', '/var/log/atlas',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0755
    )
    # Data dir
    self.assertResourceCalled('Directory', '/usr/hdp/current/atlas-server/data',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0644
    )
    # Expanded war dir
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
    app_props =  dict(self.getConfig()['configurations'][
                       'application-properties'])
    app_props['atlas.http.authentication.kerberos.name.rules'] = ' \\ \n'.join(app_props['atlas.http.authentication.kerberos.name.rules'].splitlines())
    app_props['atlas.server.bind.address'] = 'c6401.ambari.apache.org'

    self.assertResourceCalled('File', '/etc/atlas/conf/atlas-log4j.xml',
                          content=InlineTemplate(
                            self.getConfig()['configurations'][
                              'atlas-log4j']['content']),
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
    self.assertResourceCalled('File', '/etc/atlas/conf/solr/solrconfig.xml',
                              content=InlineTemplate(
                                  self.getConfig()['configurations'][
                                    'atlas-solrconfig']['content']),
                              owner='atlas',
                              group='hadoop',
                              mode=0644,
    )
    # application.properties file
    self.assertResourceCalled('PropertiesFile',
                              '/etc/atlas/conf/atlas-application.properties',
                              properties=app_props,
                              owner='atlas',
                              group='hadoop',
                              mode=0644,
                              )
    self.assertResourceCalled('Directory', '/var/log/ambari-logsearch-solr-client',
                              owner='solr',
                              group='hadoop',
                              create_parents=True,
                              cd_access='a',
                              mode=0755
    )
    self.assertResourceCalled('Directory', '/usr/lib/ambari-logsearch-solr-client',
                              owner='solr',
                              group='hadoop',
                              create_parents = True,
                              recursive_ownership = True,
                              cd_access='a',
                              mode=0755
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh',
                              content=StaticFile('/usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh'),
                              owner='solr',
                              group='hadoop',
                              mode=0755,
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-logsearch-solr-client/log4j.properties',
                              content=InlineTemplate(self.getConfig()['configurations'][
                                                       'logsearch-solr-client-log4j']['content']),
                              owner='solr',
                              group='hadoop',
                              mode=0644,
    )
    self.assertResourceCalled('File', '/var/log/ambari-logsearch-solr-client/solr-client.log',
                              owner='solr',
                              group='hadoop',
                              mode=0664,
                              content = ''
    )

    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/logsearch --download-config --config-dir /tmp/solr_config_basic_configs_0.[0-9]* --config-set basic_configs --retry 30 --interval 5')
    self.assertResourceCalledRegexp('^File$', '^/tmp/solr_config_basic_configs_0.[0-9]*',
                                    content=InlineTemplate(self.getConfig()['configurations']['atlas-solrconfig']['content']),
                                    owner='atlas',
                                    only_if='test -d /tmp/solr_config_basic_configs_0.[0-9]*')
    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/logsearch --upload-config --config-dir /tmp/solr_config_basic_configs_0.[0-9]* --config-set basic_configs --retry 30 --interval 5',
                                    only_if='test -d /tmp/solr_config_basic_configs_0.[0-9]*')
    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/logsearch --upload-config --config-dir /etc/atlas/conf/solr --config-set basic_configs --retry 30 --interval 5',
                                    not_if='test -d /tmp/solr_config_basic_configs_0.[0-9]*')
    self.assertResourceCalledRegexp('^Directory$', '^/tmp/solr_config_basic_configs_0.[0-9]*',
                                    action=['delete'],
                                    owner='atlas',
                                    create_parents=True)

    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/logsearch --create-collection --collection vertex_index --config-set basic_configs --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')
    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/logsearch --create-collection --collection edge_index --config-set basic_configs --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')
    self.assertResourceCalledRegexp('^Execute$', '^export JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/lib/ambari-logsearch-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/logsearch --create-collection --collection fulltext_index --config-set basic_configs --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')


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

