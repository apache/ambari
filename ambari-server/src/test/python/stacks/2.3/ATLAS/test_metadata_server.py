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
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import get_kinit_path
from stacks.utils.RMFTestCase import *
import json
import sys

from only_for_platform import not_for_platform, PLATFORM_WINDOWS

@not_for_platform(PLATFORM_WINDOWS)
class TestMetadataServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "ATLAS/0.1.0.2.3/package"
  STACK_VERSION = "2.3"
  stack_root = Script.get_stack_root()
  conf_dir = stack_root + "/current/atlas-server/conf"
  def configureResourcesCalled(self):
      # Both server and client
      self.assertResourceCalled('Directory', self.conf_dir,
                                owner='atlas',
                                group='hadoop',
                                create_parents = True,
                                cd_access='a',
                                mode=0755)

      # Pid dir
      self.assertResourceCalled('Directory', '/var/run/atlas',
                                owner='atlas',
                                group='hadoop',
                                create_parents = True,
                                cd_access='a',
                                mode=0755
      )
      self.assertResourceCalled('Directory', self.conf_dir + "/solr",
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
      self.assertResourceCalled('Directory', self.stack_root+"/current/atlas-server/data",
                                owner='atlas',
                                group='hadoop',
                                create_parents = True,
                                cd_access='a',
                                mode=0644
      )
      # Expanded war dir
      self.assertResourceCalled('Directory', self.stack_root+'/current/atlas-server/server/webapp',
                                owner='atlas',
                                group='hadoop',
                                create_parents = True,
                                cd_access='a',
                                mode=0644
      )
      self.assertResourceCalled('File', self.stack_root+'/current/atlas-server/server/webapp/atlas.war',
          content = StaticFile(self.stack_root+'/current/atlas-server/server/webapp/atlas.war'),
      )
      host_name = u"c6401.ambari.apache.org"
      app_props =  dict(self.getConfig()['configurations']['application-properties'])
      app_props['atlas.server.bind.address'] = host_name

      metadata_protocol = "https" if app_props["atlas.enableTLS"] is True else "http"
      metadata_port = app_props["atlas.server.https.port"] if metadata_protocol == "https" else app_props["atlas.server.http.port"]
      app_props["atlas.rest.address"] = u'%s://%s:%s' % (metadata_protocol, host_name, metadata_port)
      app_props["atlas.server.ids"] = "id1"
      app_props["atlas.server.address.id1"] = u"%s:%s" % (host_name, metadata_port)
      app_props["atlas.server.ha.enabled"] = "false"

      self.assertResourceCalled('File', str(self.conf_dir + "/atlas-log4j.xml"),
                          content=InlineTemplate(
                            self.getConfig()['configurations'][
                              'atlas-log4j']['content']),
                          owner='atlas',
                          group='hadoop',
                          mode=0644,
      )
      self.assertResourceCalled('File', str(self.conf_dir + "/atlas-env.sh"),
                                content=InlineTemplate(
                                    self.getConfig()['configurations'][
                                        'atlas-env']['content']),
                                owner='atlas',
                                group='hadoop',
                                mode=0755,
      )
      self.assertResourceCalled('File', str(self.conf_dir + "/solr/solrconfig.xml"),
                                content=InlineTemplate(
                                    self.getConfig()['configurations'][
                                      'atlas-solrconfig']['content']),
                                owner='atlas',
                                group='hadoop',
                                mode=0644,
      )
      # application.properties file
      self.assertResourceCalled('PropertiesFile',str(self.conf_dir + "/application.properties"),
                                properties=app_props,
                                owner=u'atlas',
                                group=u'hadoop',
                                mode=0644,
      )
      self.assertResourceCalled('Directory', '/var/log/ambari-infra-solr-client',
                                create_parents = True,
                                cd_access='a',
                                mode=0755
      )
      self.assertResourceCalled('Directory', '/usr/lib/ambari-infra-solr-client',
                                create_parents = True,
                                recursive_ownership = True,
                                cd_access='a',
                                mode=0755
      )
      self.assertResourceCalled('File', '/usr/lib/ambari-infra-solr-client/solrCloudCli.sh',
                                content=StaticFile('/usr/lib/ambari-infra-solr-client/solrCloudCli.sh'),
                                mode=0755,
                                )
      self.assertResourceCalled('File', '/usr/lib/ambari-infra-solr-client/log4j.properties',
                                content=self.getConfig()['configurations']['infra-solr-client-log4j']['content'],
                                mode=0644,
      )
      self.assertResourceCalled('File', '/var/log/ambari-infra-solr-client/solr-client.log',
                                mode=0664,
                                content=''
      )
      self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr --check-znode --retry 5 --interval 10')
      self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --download-config --config-dir /tmp/solr_config_atlas_configs_0.[0-9]* --config-set atlas_configs --retry 30 --interval 5')
      self.assertResourceCalledRegexp('^File$', '^/tmp/solr_config_atlas_configs_0.[0-9]*',
                                      content=InlineTemplate(self.getConfig()['configurations']['atlas-solrconfig']['content']),
                                      only_if='test -d /tmp/solr_config_atlas_configs_0.[0-9]*')
      self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --upload-config --config-dir /tmp/solr_config_atlas_configs_0.[0-9]* --config-set atlas_configs --retry 30 --interval 5',
                                      only_if='test -d /tmp/solr_config_atlas_configs_0.[0-9]*')
      self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --upload-config --config-dir {0}/solr --config-set atlas_configs --retry 30 --interval 5'.format(self.conf_dir),
                                      not_if='test -d /tmp/solr_config_atlas_configs_0.[0-9]*')
      self.assertResourceCalledRegexp('^Directory$', '^/tmp/solr_config_atlas_configs_0.[0-9]*',
                                      action=['delete'],
                                      create_parents=True)

      self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --create-collection --collection vertex_index --config-set atlas_configs --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')
      self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --create-collection --collection edge_index --config-set atlas_configs --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')
      self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --create-collection --collection fulltext_index --config-set atlas_configs --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')

  def configureResourcesCalledSecure(self):
    # Both server and client
    self.assertResourceCalled('Directory', self.conf_dir,
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
    self.assertResourceCalled('Directory', self.conf_dir + "/solr",
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
    self.assertResourceCalled('Directory', self.stack_root+'/current/atlas-server/data',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0644
    )
    # Expanded war dir
    self.assertResourceCalled('Directory', self.stack_root+'/current/atlas-server/server/webapp',
                              owner='atlas',
                              group='hadoop',
                              create_parents = True,
                              cd_access='a',
                              mode=0644
    )
    self.assertResourceCalled('File', self.stack_root+'/current/atlas-server/server/webapp/atlas.war',
                              content = StaticFile(self.stack_root+'/current/atlas-server/server/webapp/atlas.war'),
                              )
    host_name = u"c6401.ambari.apache.org"
    app_props =  dict(self.getConfig()['configurations']['application-properties'])
    app_props['atlas.server.bind.address'] = host_name

    metadata_protocol = "https" if app_props["atlas.enableTLS"] is True else "http"
    metadata_port = app_props["atlas.server.https.port"] if metadata_protocol == "https" else app_props["atlas.server.http.port"]
    app_props["atlas.rest.address"] = u'%s://%s:%s' % (metadata_protocol, host_name, metadata_port)
    app_props["atlas.server.ids"] = "id1"
    app_props["atlas.server.address.id1"] = u"%s:%s" % (host_name, metadata_port)
    app_props["atlas.server.ha.enabled"] = "false"

    self.assertResourceCalled('File', self.conf_dir + "/atlas-log4j.xml",
                              content=InlineTemplate(
                                self.getConfig()['configurations'][
                                  'atlas-log4j']['content']),
                              owner='atlas',
                              group='hadoop',
                              mode=0644,
                              )
    self.assertResourceCalled('File', self.conf_dir + "/atlas-env.sh",
                              content=InlineTemplate(
                                self.getConfig()['configurations'][
                                  'atlas-env']['content']),
                              owner='atlas',
                              group='hadoop',
                              mode=0755,
                              )
    self.assertResourceCalled('File', self.conf_dir+"/solr/solrconfig.xml",
                              content=InlineTemplate(
                                self.getConfig()['configurations'][
                                  'atlas-solrconfig']['content']),
                              owner='atlas',
                              group='hadoop',
                              mode=0644,
                              )
    # application.properties file
    self.assertResourceCalled('PropertiesFile',self.conf_dir + "/application.properties",
                              properties=app_props,
                              owner=u'atlas',
                              group=u'hadoop',
                              mode=0644,
                              )

    self.assertResourceCalled('TemplateConfig', self.conf_dir+"/atlas_jaas.conf",
                              owner = 'atlas',
                              )

    self.assertResourceCalled('Directory', '/var/log/ambari-infra-solr-client',
                              create_parents = True,
                              cd_access='a',
                              mode=0755
    )

    self.assertResourceCalled('Directory', '/usr/lib/ambari-infra-solr-client',
                              create_parents = True,
                              recursive_ownership = True,
                              cd_access='a',
                              mode=0755
    )
    self.assertResourceCalled('File', '/usr/lib/ambari-infra-solr-client/solrCloudCli.sh',
                              content=StaticFile('/usr/lib/ambari-infra-solr-client/solrCloudCli.sh'),
                              mode=0755,
                              )
    self.assertResourceCalled('File', '/usr/lib/ambari-infra-solr-client/log4j.properties',
                              content=self.getConfig()['configurations']['infra-solr-client-log4j']['content'],
                              mode=0644,
                              )
    self.assertResourceCalled('File', '/var/log/ambari-infra-solr-client/solr-client.log',
                              mode=0664,
                              content=''
    )
    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr --check-znode --retry 5 --interval 10')
    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --download-config --config-dir /tmp/solr_config_atlas_configs_0.[0-9]* --config-set atlas_configs --retry 30 --interval 5')
    self.assertResourceCalledRegexp('^File$', '^/tmp/solr_config_atlas_configs_0.[0-9]*',
                                    content=InlineTemplate(self.getConfig()['configurations']['atlas-solrconfig']['content']),
                                    only_if='test -d /tmp/solr_config_atlas_configs_0.[0-9]*')
    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --upload-config --config-dir /tmp/solr_config_atlas_configs_0.[0-9]* --config-set atlas_configs --retry 30 --interval 5',
                                    only_if='test -d /tmp/solr_config_atlas_configs_0.[0-9]*')
    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --upload-config --config-dir {0}/solr --config-set atlas_configs --retry 30 --interval 5'.format(self.conf_dir),
                                    not_if='test -d /tmp/solr_config_atlas_configs_0.[0-9]*')
    self.assertResourceCalledRegexp('^Directory$', '^/tmp/solr_config_atlas_configs_0.[0-9]*',
                                    action=['delete'],
                                    create_parents=True)
    kinit_path_local = get_kinit_path()
    self.assertResourceCalled('Execute', kinit_path_local + " -kt /etc/security/keytabs/ambari-infra-solr.keytab infra-solr/c6401.ambari.apache.org@EXAMPLE.COM; curl -k -s --negotiate -u : http://c6401.ambari.apache.org:8886/solr/admin/authorization | grep authorization.enabled && "
                              + kinit_path_local +" -kt /etc/security/keytabs/ambari-infra-solr.keytab infra-solr/c6401.ambari.apache.org@EXAMPLE.COM; curl -H 'Content-type:application/json' -d '{\"set-user-role\": {\"atlas@EXAMPLE.COM\": [\"atlas_user\", \"ranger_audit_user\", \"dev\"]}}' -s -o /dev/null -w'%{http_code}' --negotiate -u: -k http://c6401.ambari.apache.org:8886/solr/admin/authorization | grep 200",
                              logoutput = True, tries = 30, try_sleep = 10, user='solr')

    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --create-collection --collection vertex_index --config-set atlas_configs --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')
    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --create-collection --collection edge_index --config-set atlas_configs --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')
    self.assertResourceCalledRegexp('^Execute$', '^ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181/infra-solr --create-collection --collection fulltext_index --config-set atlas_configs --shards 1 --replication 1 --max-shards 1 --retry 5 --interval 10')

    self.assertResourceCalled('Execute', "ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr/configs/atlas_configs --secure-znode --jaas-file /usr/hdp/current/atlas-server/conf/atlas_jaas.conf --sasl-users atlas,infra-solr --retry 5 --interval 10")
    self.assertResourceCalled('Execute', "ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr/collections/vertex_index --secure-znode --jaas-file /usr/hdp/current/atlas-server/conf/atlas_jaas.conf --sasl-users atlas,infra-solr --retry 5 --interval 10")
    self.assertResourceCalled('Execute', "ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr/collections/edge_index --secure-znode --jaas-file /usr/hdp/current/atlas-server/conf/atlas_jaas.conf --sasl-users atlas,infra-solr --retry 5 --interval 10")
    self.assertResourceCalled('Execute', "ambari-sudo.sh JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /usr/lib/ambari-infra-solr-client/solrCloudCli.sh --zookeeper-connect-string c6401.ambari.apache.org:2181 --znode /infra-solr/collections/fulltext_index --secure-znode --jaas-file /usr/hdp/current/atlas-server/conf/atlas_jaas.conf --sasl-users atlas,infra-solr --retry 5 --interval 10")

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metadata_server.py",
                       classname = "MetadataServer",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.configureResourcesCalled()

    self.assertResourceCalled('File', '/tmp/atlas_hbase_setup.rb',
                              owner = "hbase",
                              group = "hadoop",
                              content=Template("atlas_hbase_setup.rb.j2"))

    self.assertResourceCalled('File', str(self.conf_dir+"/hdfs-site.xml"),action = ['delete'],)
    self.assertResourceCalled('Directory',self.stack_root + '/current/atlas-server/', owner = 'atlas', group = 'hadoop', recursive_ownership = True, )
    self.assertNoMoreResources()

  def test_configure_secure(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metadata_server.py",
                       classname = "MetadataServer",
                       command = "configure",
                       config_file="secure.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
                       )

    self.configureResourcesCalledSecure()

    self.assertResourceCalled('File', '/tmp/atlas_hbase_setup.rb',
                              owner = "hbase",
                              group = "hadoop",
                              content=Template("atlas_hbase_setup.rb.j2"))

    self.assertResourceCalled('File', str(self.conf_dir+"/hdfs-site.xml"),action = ['delete'],)

    self.assertResourceCalled('Directory',self.stack_root + '/current/atlas-server/', owner = 'atlas', group = 'hadoop', recursive_ownership = True, )
    self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metadata_server.py",
                       classname = "MetadataServer",
                       command = "start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.configureResourcesCalled()

    self.assertResourceCalled('File', '/tmp/atlas_hbase_setup.rb',
                              owner = "hbase",
                              group = "hadoop",
                              content=Template("atlas_hbase_setup.rb.j2"))

    self.assertResourceCalled('File', str(self.conf_dir+"/hdfs-site.xml"),action = ['delete'],)
    self.assertResourceCalled('Directory',self.stack_root + '/current/atlas-server/', owner = 'atlas', group = 'hadoop', recursive_ownership = True, )

    self.assertResourceCalled('Execute', 'source {0}/atlas-env.sh ; {1}/current/atlas-server/bin/atlas_start.py'.format(self.conf_dir,self.stack_root),
                              not_if = 'ls /var/run/atlas/atlas.pid >/dev/null 2>&1 && ps -p `cat /var/run/atlas/atlas.pid` >/dev/null 2>&1',
                              user = 'atlas',
    )

  @patch('os.path.isdir')
  def test_stop_default(self, is_dir_mock):
    is_dir_mock.return_value = True

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/metadata_server.py",
                       classname = "MetadataServer",
                       command = "stop",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', 'source {0}/atlas-env.sh; {1}/current/atlas-server/bin/atlas_stop.py'.format(self.conf_dir,self.stack_root),
                              user = 'atlas',
    )
    self.assertResourceCalled('File', '/var/run/atlas/atlas.pid',
        action = ['delete'],
    )
