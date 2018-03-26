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
from mock.mock import MagicMock, patch
from stacks.utils.RMFTestCase import *

@patch("platform.linux_distribution", new = MagicMock(return_value="Linux"))
@patch("os.path.exists", new = MagicMock(return_value=True))
class TestHBaseMaster(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HBASE/0.96.0.2.0/package"
  STACK_VERSION = "2.0.6"
  TMP_PATH = "/hadoop"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']

  CONFIG_OVERRIDES = {"serviceName":"HBASE", "role":"HBASE_MASTER"}

  def test_install_hbase_master_default_no_phx(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                       classname = "HbaseMaster",
                       command = "install",
                       config_file="hbase_no_phx.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       try_install=True,
                       checked_call_mocks = [(0, "OK.", ""),(0, "OK.", "")],
    )
    self.assertResourceCalled('Package', 'hbase_2_3_*',
                              retry_count=5,
                              retry_on_repo_unavailability=False)

    self.assertNoMoreResources()
  
  
  def test_install_hbase_master_default_with_phx(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                       classname = "HbaseMaster",
                       command = "install",
                       config_file="hbase_with_phx.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       try_install=True,
                       checked_call_mocks = [(0, "OK.", ""),(0, "OK.", "")],
    )
    self.assertResourceCalled('Package', 'hbase_2_3_*',
                              retry_count=5,
                              retry_on_repo_unavailability=False)
    self.assertResourceCalled('Package', 'phoenix_2_3_*',
                              retry_count=5,
                              retry_on_repo_unavailability=False)

    self.assertNoMoreResources()

  @patch("ambari_commons.repo_manager.ManagerFactory.get")
  def test_install_hbase_master_with_version(self, get_provider):
    from ambari_commons.os_check import OSConst
    from ambari_commons.repo_manager import ManagerFactory

    pkg_manager = ManagerFactory.get_new_instance(OSConst.REDHAT_FAMILY)

    with patch.object(pkg_manager, "all_packages") as all_packages,\
         patch.object(pkg_manager, "available_packages") as available_packages, \
         patch.object(pkg_manager, "installed_packages") as installed_packages:
      all_packages.return_value = [["hbase_2_3_0_1_1234", "1.0", "testrepo"]]
      available_packages.return_value = [["hbase_2_3_0_1_1234", "1.0", "testrepo"]]
      installed_packages.return_value = [["hbase_2_3_0_1_1234", "1.0", "testrepo"]]

      get_provider.return_value = pkg_manager

      config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/hbase_with_phx.json"
      with open(config_file, "r") as f:
        json_content = json.load(f)
      version = '2.3.0.1-1234'

      # the json file is not a "well formed" install command
      json_content['roleCommand'] = 'INSTALL'
      json_content['commandParams']['version'] = version
      json_content['commandParams']['package_list'] = "[{\"name\":\"hbase_${stack_version}\",\"condition\":\"\",\"skipUpgrade\":false}]"

      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                         classname = "HbaseMaster",
                         command = "install",
                         config_dict = json_content,
                         stack_version = self.STACK_VERSION,
                         target = RMFTestCase.TARGET_COMMON_SERVICES,
                         try_install=True,
                         os_type=('Redhat', '6.4', 'Final'),
                         checked_call_mocks = [(0, "OK.", "")],
                         available_packages_in_repos = ['hbase_2_3_0_1_1234'],
                         )


      # only assert that the correct package is trying to be installed
      self.assertResourceCalled('Package', 'hbase_2_3_0_1_1234',
                                retry_count=5,
                                retry_on_repo_unavailability=False)


    def test_configure_default(self):
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                     classname = "HbaseMaster",
                     command = "configure",
                     config_file="default.json",
                     stack_version = self.STACK_VERSION,
                     target = RMFTestCase.TARGET_COMMON_SERVICES
      )
    
      self.assert_configure_default()
      self.assertNoMoreResources()

  def test_start_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                   classname = "HbaseMaster",
                   command = "start",
                   config_file="default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assert_configure_default()
    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf start master',
      not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-master.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-master.pid` >/dev/null 2>&1',
      user = 'hbase'
    )
    self.assertNoMoreResources()
    pass

  def test_start_default_bucketcache(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                       classname = "HbaseMaster",
                       command = "start",
                       config_file="default_with_bucket.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assert_configure_default(bucketcache_ioengine_as_file=True)
    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf start master',
                              not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-master.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-master.pid` >/dev/null 2>&1',
                              user = 'hbase'
    )
    self.assertNoMoreResources()
    pass
    
  def test_stop_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                   classname = "HbaseMaster",
                   command = "stop",
                   config_file="default.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf stop master',
        only_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-master.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-master.pid` >/dev/null 2>&1',
        on_timeout = '! ( ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-master.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-master.pid` >/dev/null 2>&1 ) || ambari-sudo.sh -H -E kill -9 `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-master.pid`',
        timeout = 30,
        user = 'hbase',
    )
    
    self.assertResourceCalled('File', '/var/run/hbase/hbase-hbase-master.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_decom_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                       classname = "HbaseMaster",
                       command = "decommission",
                       config_file="default.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('File', '/usr/lib/hbase/bin/draining_servers.rb',
                              content = StaticFile('draining_servers.rb'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', ' HBASE_OPTS="$HBASE_OPTS " /usr/lib/hbase/bin/hbase --config /etc/hbase/conf org.jruby.Main /usr/lib/hbase/bin/draining_servers.rb add host1',
                              logoutput = True,
                              user = 'hbase',
                              )
    self.assertResourceCalled('Execute', ' HBASE_OPTS="$HBASE_OPTS " /usr/lib/hbase/bin/hbase --config /etc/hbase/conf org.jruby.Main /usr/lib/hbase/bin/region_mover.rb unload host1',
                              logoutput = True,
                              user = 'hbase',
                              )
    self.assertResourceCalled('Execute', ' HBASE_OPTS="$HBASE_OPTS " /usr/lib/hbase/bin/hbase --config /etc/hbase/conf org.jruby.Main /usr/lib/hbase/bin/draining_servers.rb add host2',
                              logoutput = True,
                              user = 'hbase',
                              )
    self.assertResourceCalled('Execute', ' HBASE_OPTS="$HBASE_OPTS " /usr/lib/hbase/bin/hbase --config /etc/hbase/conf org.jruby.Main /usr/lib/hbase/bin/region_mover.rb unload host2',
                              logoutput = True,
                              user = 'hbase',
                              )
    self.assertNoMoreResources()

  def test_decom_default_draining_only(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                       classname = "HbaseMaster",
                       command = "decommission",
                       config_file="default.hbasedecom.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('File', '/usr/lib/hbase/bin/draining_servers.rb',
                              content = StaticFile('draining_servers.rb'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', ' HBASE_OPTS="$HBASE_OPTS " /usr/lib/hbase/bin/hbase --config /etc/hbase/conf org.jruby.Main /usr/lib/hbase/bin/draining_servers.rb remove host1',
                              logoutput = True,
                              user = 'hbase',
                              )
    self.assertNoMoreResources()

  def test_configure_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                   classname = "HbaseMaster",
                   command = "configure",
                   config_file="secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assert_configure_secured()
    self.assertNoMoreResources()
    
  def test_start_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                   classname = "HbaseMaster",
                   command = "start",
                   config_file="secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    
    self.assert_configure_secured()
    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf start master',
      not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-master.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-master.pid` >/dev/null 2>&1',
      user = 'hbase',
    )
    self.assertNoMoreResources()
    
  def test_stop_secured(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                   classname = "HbaseMaster",
                   command = "stop",
                   config_file="secured.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('Execute', '/usr/lib/hbase/bin/hbase-daemon.sh --config /etc/hbase/conf stop master',
        only_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-master.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-master.pid` >/dev/null 2>&1',
        on_timeout = '! ( ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-master.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-master.pid` >/dev/null 2>&1 ) || ambari-sudo.sh -H -E kill -9 `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-master.pid`',
        timeout = 30,
        user = 'hbase',
    )

    self.assertResourceCalled('File', '/var/run/hbase/hbase-hbase-master.pid',
        action = ['delete'],
    )
    self.assertNoMoreResources()

  def test_decom_secure(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                       classname = "HbaseMaster",
                       command = "decommission",
                       config_file="secured.json",
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertResourceCalled('File', '/usr/lib/hbase/bin/draining_servers.rb',
                              content = StaticFile('draining_servers.rb'),
                              mode = 0755,
                              )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hbase.service.keytab hbase/c6401.ambari.apache.org@EXAMPLE.COM; HBASE_OPTS="$HBASE_OPTS -Djava.security.auth.login.config=/etc/hbase/conf/hbase_master_jaas.conf" /usr/lib/hbase/bin/hbase --config /etc/hbase/conf org.jruby.Main /usr/lib/hbase/bin/draining_servers.rb add host1',
                              logoutput = True,
                              user = 'hbase',
                              )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hbase.service.keytab hbase/c6401.ambari.apache.org@EXAMPLE.COM; HBASE_OPTS="$HBASE_OPTS -Djava.security.auth.login.config=/etc/hbase/conf/hbase_master_jaas.conf" /usr/lib/hbase/bin/hbase --config /etc/hbase/conf org.jruby.Main /usr/lib/hbase/bin/region_mover.rb unload host1',
                              logoutput = True,
                              user = 'hbase',
                              )
    self.assertNoMoreResources()

  def assert_configure_default(self, bucketcache_ioengine_as_file=False):
    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755
    )
    self.assertResourceCalled('Directory', '/etc/hbase/conf',
      owner = 'hbase',
      group = 'hadoop',
      create_parents = True,
    )
    self.assertResourceCalled('Directory', '/tmp',
      create_parents = True,
      mode = 0777
    )
    if bucketcache_ioengine_as_file:
      self.assertResourceCalled('Directory', '/mnt/bucket',
                                create_parents = True,
                                owner = 'hbase',
                                group = 'hadoop',
                                mode = 0755
      )
      pass
    self.assertResourceCalled('Directory', '/hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Execute', ('chmod', '1777', u'/hadoop'),
                              sudo = True,
                              )
    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hbase-site']
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hdfs-site']
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop'
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(self.getConfig()['configurations']['hbase-env']['content']),
      group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
      owner = 'root',
      group = 'root',
      create_parents = True,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/hbase.conf',
      content = Template('hbase.conf.j2'),
      owner = 'root',
      group = 'root',
      mode = 0644,
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-MASTER',
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/regionservers',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('Directory', '/var/run/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )
    self.assertResourceCalled('File',
                              '/etc/hbase/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hbase',
                              content=InlineTemplate('log4jproperties\nline2')
    )

    self.assertResourceCalled('HdfsResource', 'hdfs://c6401.ambari.apache.org:8020/apps/hbase/data',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hbase',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
    )
    self.assertResourceCalled('HdfsResource', '/apps/hbase/staging',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_conf_dir = '/etc/hadoop/conf',
        keytab = UnknownConfigurationMock(),
        
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hbase',
        hadoop_bin_dir = '/usr/bin',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0711,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name=UnknownConfigurationMock(), default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )

  def assert_configure_secured(self):
    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755
    )
    self.assertResourceCalled('Directory', '/etc/hbase/conf',
      owner = 'hbase',
      group = 'hadoop',
      create_parents = True,
    )
    self.assertResourceCalled('Directory', '/tmp',
      create_parents = True,
      mode = 0777
    )
    self.assertResourceCalled('Directory', '/hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )
    self.assertResourceCalled('Execute', ('chmod', '1777', u'/hadoop'),
                              sudo = True,
                              )
    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hbase-site']
    )
    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['core-site']
    )
    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/etc/hbase/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hdfs-site']
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
    )
    self.assertResourceCalled('File', '/etc/hbase/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(self.getConfig()['configurations']['hbase-env']['content']),
      group = 'hadoop',
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
      owner = 'root',
      group = 'root',
      create_parents = True,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/hbase.conf',
      content = Template('hbase.conf.j2'),
      owner = 'root',
      group = 'root',
      mode = 0644,
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-MASTER',
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/regionservers',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('TemplateConfig', '/etc/hbase/conf/hbase_master_jaas.conf',
      owner = 'hbase',
      template_tag = None,
    )
    self.assertResourceCalled('Directory', '/var/run/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )
    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )
    self.assertResourceCalled('File',
                              '/etc/hbase/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hbase',
                              content=InlineTemplate('log4jproperties\nline2')
    )
    self.assertResourceCalled('HdfsResource', 'hdfs://c6401.ambari.apache.org:8020/apps/hbase/data',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hbase',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
    )
    self.assertResourceCalled('HdfsResource', '/apps/hbase/staging',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_conf_dir = '/etc/hadoop/conf',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        owner = 'hbase',
        hadoop_bin_dir = '/usr/bin',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        mode = 0711,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        
        kinit_path_local = '/usr/bin/kinit',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore', hdfs_site=self.getConfig()['configurations']['hdfs-site'], principal_name='hdfs', default_fs='hdfs://c6401.ambari.apache.org:8020',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )

  def test_start_default_22(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                   classname = "HbaseMaster",
                   command = "start",
                   config_file="hbase-2.2.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES)
    
    self.assertResourceCalled('Directory', '/etc/hbase',
      mode = 0755)

    self.assertResourceCalled('Directory', '/usr/hdp/current/hbase-master/conf',
      owner = 'hbase',
      group = 'hadoop',
      create_parents = True)
    self.assertResourceCalled('Directory', '/tmp',
      create_parents = True,
      mode = 0777
    )
    self.assertResourceCalled('Directory', '/hadoop',
                              create_parents = True,
                              cd_access = 'a',
                              )

    self.assertResourceCalled('Execute', ('chmod', '1777', u'/hadoop'),
                              sudo = True,
                              )

    self.assertResourceCalled('XmlConfig', 'hbase-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-master/conf',
      configurations = self.getConfig()['configurations']['hbase-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hbase-site'])

    self.assertResourceCalled('XmlConfig', 'core-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-master/conf',
      configurations = self.getConfig()['configurations']['core-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['core-site'])

    self.assertResourceCalled('XmlConfig', 'hdfs-site.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-master/conf',
      configurations = self.getConfig()['configurations']['hdfs-site'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hdfs-site'])

    self.assertResourceCalled('XmlConfig', 'hbase-policy.xml',
      owner = 'hbase',
      group = 'hadoop',
      conf_dir = '/usr/hdp/current/hbase-master/conf',
      configurations = self.getConfig()['configurations']['hbase-policy'],
      configuration_attributes = self.getConfig()['configurationAttributes']['hbase-policy'])

    self.assertResourceCalled('File', '/usr/hdp/current/hbase-master/conf/hbase-env.sh',
      owner = 'hbase',
      content = InlineTemplate(self.getConfig()['configurations']['hbase-env']['content']),
      group = 'hadoop'
    )
    self.assertResourceCalled('Directory', '/etc/security/limits.d',
      owner = 'root',
      group = 'root',
      create_parents = True,
    )
    self.assertResourceCalled('File', '/etc/security/limits.d/hbase.conf',
      content = Template('hbase.conf.j2'),
      owner = 'root',
      group = 'root',
      mode = 0644,
    )
    self.assertResourceCalled('TemplateConfig', '/usr/hdp/current/hbase-master/conf/hadoop-metrics2-hbase.properties',
      owner = 'hbase',
      template_tag = 'GANGLIA-MASTER')

    self.assertResourceCalled('TemplateConfig', '/usr/hdp/current/hbase-master/conf/regionservers',
      owner = 'hbase',
      template_tag = None)

    self.assertResourceCalled('Directory', '/var/run/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )

    self.assertResourceCalled('Directory', '/var/log/hbase',
      owner = 'hbase',
      create_parents = True,
      mode = 0755,
      cd_access = 'a',
    )

    self.assertResourceCalled('File',
                              '/usr/hdp/current/hbase-master/conf/log4j.properties',
                              mode=0644,
                              group='hadoop',
                              owner='hbase',
                              content=InlineTemplate('log4jproperties\nline2'))

    self.assertResourceCalled('HdfsResource', 'hdfs://nn1/apps/hbase/data',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://nn1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        dfs_type = '',
        owner = 'hbase',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )
    self.assertResourceCalled('HdfsResource', '/apps/hbase/staging',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://nn1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        dfs_type = '',
        owner = 'hbase',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'directory',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        mode = 0711,
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/hdp/current/hadoop-client/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://nn1',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = UnknownConfigurationMock(),
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )

    self.assertResourceCalled('Execute', '/usr/hdp/current/hbase-master/bin/hbase-daemon.sh --config /usr/hdp/current/hbase-master/conf start master',
      not_if = 'ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E test -f /var/run/hbase/hbase-hbase-master.pid && ps -p `ambari-sudo.sh [RMF_ENV_PLACEHOLDER] -H -E cat /var/run/hbase/hbase-hbase-master.pid` >/dev/null 2>&1',
      user = 'hbase')

    self.assertNoMoreResources()

  def test_upgrade_backup(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_upgrade.py",
                   classname = "HbaseMasterUpgrade",
                   command = "take_snapshot",
                   config_file="hbase-preupgrade.json",
                   stack_version = self.STACK_VERSION,
                   target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Execute', " echo 'snapshot_all' | /usr/hdp/current/hbase-client/bin/hbase shell",
      user = 'hbase')
  
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  def test_pre_upgrade_restart(self, call_mock):
    call_mock.side_effects = [(0, None), (0, None)]

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.2.1.0-3242'
    json_content['commandParams']['version'] = version
    
    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                       classname = "HbaseMaster",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)
    self.assertResourceCalled('Execute',
                              ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hbase-master', version), sudo=True,)
    self.assertFalse(call_mock.called)
    self.assertNoMoreResources()

  @patch("resource_management.core.shell.call")
  def test_upgrade_23(self, call_mock):
    call_mock.side_effects = [(0, None), (0, None)]

    config_file = self.get_src_folder()+"/test/python/stacks/2.0.6/configs/default.json"
    with open(config_file, "r") as f:
      json_content = json.load(f)
    version = '2.3.0.0-1234'
    json_content['commandParams']['version'] = version

    mocks_dict = {}
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/hbase_master.py",
                       classname = "HbaseMaster",
                       command = "pre_upgrade_restart",
                       config_dict = json_content,
                       config_overrides = self.CONFIG_OVERRIDES,
                       stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES,
                       mocks_dict = mocks_dict)

    self.assertResourceCalledIgnoreEarlier('Execute', ('ambari-python-wrap', '/usr/bin/hdp-select', 'set', 'hbase-master', version), sudo=True)

