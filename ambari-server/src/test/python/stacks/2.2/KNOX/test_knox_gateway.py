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
import json
import tempfile
from resource_management import *
from stacks.utils.RMFTestCase import *
from mock.mock import patch
from mock.mock import MagicMock

@patch.object(tempfile, "gettempdir", new=MagicMock(return_value="/tmp"))
@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
class TestKnoxGateway(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "KNOX/0.5.0.2.2/package"
  STACK_VERSION = "2.2"

  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command = "configure",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Directory', '/usr/hdp/current/knox-server/data/',
                              owner = 'knox',
                              group = 'knox',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Directory', '/var/log/knox',
                              owner = 'knox',
                              group = 'knox',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Directory', '/var/run/knox',
                              owner = 'knox',
                              group = 'knox',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/knox-server/conf',
                              owner = 'knox',
                              group = 'knox',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/knox-server/conf/topologies',
                              owner = 'knox',
                              group = 'knox',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              recursive_ownership = True,
    )

    self.assertResourceCalled('XmlConfig', 'gateway-site.xml',
                              owner = 'knox',
                              group = 'knox',
                              conf_dir = '/usr/hdp/current/knox-server/conf',
                              configurations = self.getConfig()['configurations']['gateway-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['gateway-site']
    )

    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/gateway-log4j.properties',
                              mode=0644,
                              group='knox',
                              owner = 'knox',
                              content = InlineTemplate(self.getConfig()['configurations']['gateway-log4j']['content'])
    )
    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/topologies/default.xml',
                              group='knox',
                              owner = 'knox',
                              content = InlineTemplate(self.getConfig()['configurations']['topology']['content'])
    )
    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/topologies/admin.xml',
                              group='knox',
                              owner = 'knox',
                              content = InlineTemplate(self.getConfig()['configurations']['admin-topology']['content'])
    )
    self.assertResourceCalled('Execute', '/usr/hdp/current/knox-server/bin/knoxcli.sh create-master --master sa',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ambari-sudo.sh su knox -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]test -f /usr/hdp/current/knox-server/data/security/master'",
        user = 'knox',
    )
    self.assertResourceCalled('Execute', '/usr/hdp/current/knox-server/bin/knoxcli.sh create-cert --hostname c6401.ambari.apache.org',
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        not_if = "ambari-sudo.sh su knox -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]test -f /usr/hdp/current/knox-server/data/security/keystores/gateway.jks'",
        user = 'knox',
    )
    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/ldap-log4j.properties',
        content = InlineTemplate('\n        # Licensed to the Apache Software Foundation (ASF) under one\n        # or more contributor license agreements.  See the NOTICE file\n        # distributed with this work for additional information\n        # regarding copyright ownership.  The ASF licenses this file\n        # to you under the Apache License, Version 2.0 (the\n        # "License"); you may not use this file except in compliance\n        # with the License.  You may obtain a copy of the License at\n        #\n        #     http://www.apache.org/licenses/LICENSE-2.0\n        #\n        # Unless required by applicable law or agreed to in writing, software\n        # distributed under the License is distributed on an "AS IS" BASIS,\n        # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n        # See the License for the specific language governing permissions and\n        # limitations under the License.\n        #testing\n\n        app.log.dir=${launcher.dir}/../logs\n        app.log.file=${launcher.name}.log\n\n        log4j.rootLogger=ERROR, drfa\n        log4j.logger.org.apache.directory.server.ldap.LdapServer=INFO\n        log4j.logger.org.apache.directory=WARN\n\n        log4j.appender.stdout=org.apache.log4j.ConsoleAppender\n        log4j.appender.stdout.layout=org.apache.log4j.PatternLayout\n        log4j.appender.stdout.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss} %p %c{2}: %m%n\n\n        log4j.appender.drfa=org.apache.log4j.DailyRollingFileAppender\n        log4j.appender.drfa.File=${app.log.dir}/${app.log.file}\n        log4j.appender.drfa.DatePattern=.yyyy-MM-dd\n        log4j.appender.drfa.layout=org.apache.log4j.PatternLayout\n        log4j.appender.drfa.layout.ConversionPattern=%d{ISO8601} %-5p %c{2} (%F:%M(%L)) - %m%n'),
        owner = 'knox',
        group = 'knox',
        mode = 0644,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/users.ldif',
        content = '\n            # Licensed to the Apache Software Foundation (ASF) under one\n            # or more contributor license agreements.  See the NOTICE file\n            # distributed with this work for additional information\n            # regarding copyright ownership.  The ASF licenses this file\n            # to you under the Apache License, Version 2.0 (the\n            # "License"); you may not use this file except in compliance\n            # with the License.  You may obtain a copy of the License at\n            #\n            #     http://www.apache.org/licenses/LICENSE-2.0\n            #\n            # Unless required by applicable law or agreed to in writing, software\n            # distributed under the License is distributed on an "AS IS" BASIS,\n            # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n            # See the License for the specific language governing permissions and\n            # limitations under the License.\n\n            version: 1\n\n            # Please replace with site specific values\n            dn: dc=hadoop,dc=apache,dc=org\n            objectclass: organization\n            objectclass: dcObject\n            o: Hadoop\n            dc: hadoop\n\n            # Entry for a sample people container\n            # Please replace with site specific values\n            dn: ou=people,dc=hadoop,dc=apache,dc=org\n            objectclass:top\n            objectclass:organizationalUnit\n            ou: people\n\n            # Entry for a sample end user\n            # Please replace with site specific values\n            dn: uid=guest,ou=people,dc=hadoop,dc=apache,dc=org\n            objectclass:top\n            objectclass:person\n            objectclass:organizationalPerson\n            objectclass:inetOrgPerson\n            cn: Guest\n            sn: User\n            uid: guest\n            userPassword:guest-password\n\n            # entry for sample user admin\n            dn: uid=admin,ou=people,dc=hadoop,dc=apache,dc=org\n            objectclass:top\n            objectclass:person\n            objectclass:organizationalPerson\n            objectclass:inetOrgPerson\n            cn: Admin\n            sn: Admin\n            uid: admin\n            userPassword:admin-password\n\n            # entry for sample user sam\n            dn: uid=sam,ou=people,dc=hadoop,dc=apache,dc=org\n            objectclass:top\n            objectclass:person\n            objectclass:organizationalPerson\n            objectclass:inetOrgPerson\n            cn: sam\n            sn: sam\n            uid: sam\n            userPassword:sam-password\n\n            # entry for sample user tom\n            dn: uid=tom,ou=people,dc=hadoop,dc=apache,dc=org\n            objectclass:top\n            objectclass:person\n            objectclass:organizationalPerson\n            objectclass:inetOrgPerson\n            cn: tom\n            sn: tom\n            uid: tom\n            userPassword:tom-password\n\n            # create FIRST Level groups branch\n            dn: ou=groups,dc=hadoop,dc=apache,dc=org\n            objectclass:top\n            objectclass:organizationalUnit\n            ou: groups\n            description: generic groups branch\n\n            # create the analyst group under groups\n            dn: cn=analyst,ou=groups,dc=hadoop,dc=apache,dc=org\n            objectclass:top\n            objectclass: groupofnames\n            cn: analyst\n            description:analyst  group\n            member: uid=sam,ou=people,dc=hadoop,dc=apache,dc=org\n            member: uid=tom,ou=people,dc=hadoop,dc=apache,dc=org\n\n\n            # create the scientist group under groups\n            dn: cn=scientist,ou=groups,dc=hadoop,dc=apache,dc=org\n            objectclass:top\n            objectclass: groupofnames\n            cn: scientist\n            description: scientist group\n            member: uid=sam,ou=people,dc=hadoop,dc=apache,dc=org',
        owner = 'knox',
        group = 'knox',
        mode = 0644,
    )
    self.assertNoMoreResources()

  @patch("os.path.isdir")
  def test_pre_upgrade_restart(self, isdir_mock):
    isdir_mock.return_value = True
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/knox_upgrade.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Execute', ('tar',
     '-zchf',
     '/tmp/knox-upgrade-backup/knox-data-backup.tar',
     '-C',
     '/usr/hdp/current/knox-server/data',
     '.'),
        sudo = True, tries = 3, try_sleep = 1,
    )
    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'knox-server', '2.2.1.0-3242'),
        sudo = True,
    )
    self.assertNoMoreResources()

  @patch("os.remove")
  @patch("os.path.exists")
  @patch("os.path.isdir")
  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_to_hdp_2300(self, call_mock, isdir_mock, path_exists_mock, remove_mock):
    """
    In HDP 2.3.0.0, Knox was using a data dir of /var/lib/knox/data
    """
    isdir_mock.return_value = True
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/knox_upgrade.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = "2.3.0.0-1234"
    json_content['commandParams']['version'] = version

    path_exists_mock.return_value = True
    mocks_dict = {}

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Execute', ('tar',
     '-zchf',
     '/tmp/knox-upgrade-backup/knox-data-backup.tar',
     '-C',
     '/usr/hdp/current/knox-server/data',
     '.'),
        sudo = True,  tries = 3, try_sleep = 1,
    )
    self.assertResourceCalledIgnoreEarlier('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'knox-server', version),sudo = True)
    self.assertNoMoreResources()


  @patch("os.remove")
  @patch("os.path.exists")
  @patch("os.path.isdir")
  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_from_hdp_2300_to_2320(self, call_mock, isdir_mock, path_exists_mock, remove_mock):
    """
    In RU from HDP 2.3.0.0 to 2.3.2.0, should backup the data dir used by the source version, which
    is /var/lib/knox/data
    """
    isdir_mock.return_value = True
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/knox_upgrade.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    source_version = "2.3.0.0-1234"
    version = "2.3.2.0-5678"
    # This is an RU from 2.3.0.0 to 2.3.2.0
    json_content['commandParams']['version'] = version

    json_content["upgradeSummary"] = {
      "services":{
        "KNOX":{
          "sourceRepositoryId":1,
          "sourceStackId":"HDP-2.2",
          "sourceVersion":source_version,
          "targetRepositoryId":2,
          "targetStackId":"HDP-2.3",
          "targetVersion":version
        }
      },
      "direction":"UPGRADE",
      "type":"nonrolling_upgrade",
      "isRevert":False,
      "orchestration":"STANDARD"
    }

    path_exists_mock.return_value = True
    mocks_dict = {}

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Execute', ('tar',
     '-zchf',
     '/tmp/knox-upgrade-backup/knox-data-backup.tar',
     '-C',
     '/usr/hdp/current/knox-server/data',
     '.'),
        sudo = True, tries = 3, try_sleep = 1,
    )
    self.assertResourceCalledIgnoreEarlier('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'knox-server', version),sudo = True)

    self.assertResourceCalled('Execute', ("cp", "-R", "-p", "-f", "/usr/hdp/2.3.0.0-1234/knox/data/.", "/usr/hdp/current/knox-server/data"), sudo = True)

    self.assertNoMoreResources()


  @patch("os.remove")
  @patch("os.path.exists")
  @patch("os.path.isdir")
  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart_from_hdp_2320(self, call_mock, isdir_mock, path_exists_mock, remove_mock):
    """
    In RU from HDP 2.3.2 to anything higher, should backup the data dir used by the source version, which
    is /var/lib/knox/data_${source_version}
    """
    isdir_mock.return_value = True
    config_file = self.get_src_folder()+"/test/python/stacks/2.2/configs/knox_upgrade.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    source_version = "2.3.2.0-1000"
    version = "2.3.2.0-1001"
    # This is an RU from 2.3.2.0 to 2.3.2.1
    json_content['commandParams']['version'] = version

    json_content["upgradeSummary"] = {
      "services":{
        "KNOX":{
          "sourceRepositoryId":1,
          "sourceStackId":"HDP-2.2",
          "sourceVersion":source_version,
          "targetRepositoryId":2,
          "targetStackId":"HDP-2.3",
          "targetVersion":version
        }
      },
      "direction":"UPGRADE",
      "type":"rolling_upgrade",
      "isRevert":False,
      "orchestration":"STANDARD"
    }

    path_exists_mock.return_value = True
    mocks_dict = {}

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       call_mocks = [(0, None, ''), (0, None)],
                       mocks_dict = mocks_dict)

    self.assertResourceCalled('Execute', ('tar',
     '-zchf',
     '/tmp/knox-upgrade-backup/knox-data-backup.tar',
     '-C',
     "/usr/hdp/current/knox-server/data",
     '.'),
        sudo = True,  tries = 3, try_sleep = 1,
    )

    '''
    self.assertResourceCalled('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'knox-server', version),
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('cp',
     '/tmp/knox-upgrade-backup/knox-conf-backup.tar',
     '/usr/hdp/current/knox-server/conf/knox-conf-backup.tar'),
        sudo = True,
    )
    self.assertResourceCalled('Execute', ('tar',
     '-xf',
     '/tmp/knox-upgrade-backup/knox-conf-backup.tar',
     '-C',
     '/usr/hdp/current/knox-server/conf/',
     '.'),
        sudo = True,
    )
    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/knox-conf-backup.tar',
        action = ['delete'],
    )
    self.assertNoMoreResources()

    '''

  @patch("os.path.islink")
  def test_start_default(self, islink_mock):


    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/knox_gateway.py",
                       classname = "KnoxGateway",
                       command = "start",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)


    self.assertResourceCalled('Directory', '/usr/hdp/current/knox-server/data/',
                              owner = 'knox',
                              group = 'knox',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Directory', '/var/log/knox',
                              owner = 'knox',
                              group = 'knox',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Directory', '/var/run/knox',
                              owner = 'knox',
                              group = 'knox',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/knox-server/conf',
                              owner = 'knox',
                              group = 'knox',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              recursive_ownership = True,
    )
    self.assertResourceCalled('Directory', '/usr/hdp/current/knox-server/conf/topologies',
                              owner = 'knox',
                              group = 'knox',
                              create_parents = True,
                              mode = 0755,
                              cd_access = "a",
                              recursive_ownership = True,
    )

    self.assertResourceCalled('XmlConfig', 'gateway-site.xml',
                              owner = 'knox',
                              group = 'knox',
                              conf_dir = '/usr/hdp/current/knox-server/conf',
                              configurations = self.getConfig()['configurations']['gateway-site'],
                              configuration_attributes = self.getConfig()['configuration_attributes']['gateway-site']
    )

    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/gateway-log4j.properties',
                              mode=0644,
                              group='knox',
                              owner = 'knox',
                              content = InlineTemplate(self.getConfig()['configurations']['gateway-log4j']['content'])
    )
    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/topologies/default.xml',
                              group='knox',
                              owner = 'knox',
                              content = InlineTemplate(self.getConfig()['configurations']['topology']['content'])
    )
    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/topologies/admin.xml',
                              group='knox',
                              owner = 'knox',
                              content = InlineTemplate(self.getConfig()['configurations']['admin-topology']['content'])
    )
    self.assertResourceCalled('Execute', '/usr/hdp/current/knox-server/bin/knoxcli.sh create-master --master sa',
                              environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
                              not_if = "ambari-sudo.sh su knox -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]test -f /usr/hdp/current/knox-server/data/security/master'",
                              user = 'knox',
                              )
    self.assertResourceCalled('Execute', '/usr/hdp/current/knox-server/bin/knoxcli.sh create-cert --hostname c6401.ambari.apache.org',
                              environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
                              not_if = "ambari-sudo.sh su knox -l -s /bin/bash -c '[RMF_EXPORT_PLACEHOLDER]test -f /usr/hdp/current/knox-server/data/security/keystores/gateway.jks'",
                              user = 'knox',
                              )
    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/ldap-log4j.properties',
                              mode=0644,
                              group='knox',
                              owner = 'knox',
                              content = InlineTemplate(self.getConfig()['configurations']['ldap-log4j']['content'])
    )
    self.assertResourceCalled('File', '/usr/hdp/current/knox-server/conf/users.ldif',
                              mode=0644,
                              group='knox',
                              owner = 'knox',
                              content = self.getConfig()['configurations']['users-ldif']['content']
    )
    self.assertResourceCalled('Link', '/usr/hdp/current/knox-server/pids',
        to = '/var/run/knox',
    )
    self.assertResourceCalled('Directory', '/var/log/knox',
                              owner = 'knox',
                              mode = 0755,
                              group = 'knox',
                              create_parents = True,
                              cd_access = 'a',
                              recursive_ownership = True,
                              )
    self.assertResourceCalled("Execute", "/usr/hdp/current/knox-server/bin/gateway.sh start",
                              environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
                              not_if = u'ls /var/run/knox/gateway.pid >/dev/null 2>&1 && ps -p `cat /var/run/knox/gateway.pid` >/dev/null 2>&1',
                              user = u'knox',)
    self.assertTrue(islink_mock.called)
    self.assertNoMoreResources()

