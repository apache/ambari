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

import os, json, crypt
import  resource_management.libraries.functions

from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *

@patch.object(resource_management.libraries.functions, 'check_process_status', new = MagicMock())
class TestHawqMaster(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = 'HAWQ/2.0.0/package'
  STACK_VERSION = '2.3'
  GPADMIN = 'gpadmin'
  POSTGRES = 'postgres'
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']
  CONFIG_FILE = os.path.join(os.path.dirname(__file__), '../configs/hawq_default.json')
  HAWQ_CHECK_COMMAND = 'source /usr/local/hawq/greenplum_path.sh && hawq check -f /usr/local/hawq/etc/hawq_hosts --hadoop /usr/phd/current/hadoop-client --config /usr/local/hawq/etc/hawq_check.cnf '

  def setUp(self):
    try:
      with open(self.CONFIG_FILE, "r") as f:
        self.config_dict = json.load(f)
    except IOError:
      raise RuntimeError("Can not read config file: " + self.CONFIG_FILE)


  def __asserts_for_configure(self):

    self.assertResourceCalled('Group', self.GPADMIN,
        ignore_failures = True
        )

    self.assertResourceCalled('User', self.GPADMIN,
        gid = self.GPADMIN,
        groups = ['gpadmin', u'hadoop'],
        ignore_failures = True,
        password = crypt.crypt(self.config_dict['configurations']['hawq-env']['hawq_password'], "$1$salt$")
        )

    self.assertResourceCalled('Group', self.POSTGRES,
        ignore_failures = True
        )

    self.assertResourceCalled('User', self.POSTGRES,
        gid = self.POSTGRES,
        groups = [self.POSTGRES, u'hadoop'],
        ignore_failures = True
        )

    self.assertResourceCalled('Execute', 'chown -R gpadmin:gpadmin /usr/local/hawq/',
        timeout = 600
        )

    self.assertResourceCalled('XmlConfig', 'hdfs-client.xml',
        conf_dir = '/usr/local/hawq/etc/',
        configurations = self.getConfig()['configurations']['hdfs-client'],
        configuration_attributes = self.getConfig()['configuration_attributes']['hdfs-client'],
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('XmlConfig', 'yarn-client.xml',
        conf_dir = '/usr/local/hawq/etc/',
        configurations = self.getConfig()['configurations']['yarn-client'],
        configuration_attributes = self.getConfig()['configuration_attributes']['yarn-client'],
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('XmlConfig', 'hawq-site.xml',
        conf_dir = '/usr/local/hawq/etc/',
        configurations = self.getConfig()['configurations']['hawq-site'],
        configuration_attributes = self.getConfig()['configuration_attributes']['hawq-site'],
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('File', '/usr/local/hawq/etc/hawq_check.cnf',
        content = self.getConfig()['configurations']['hawq-check-env']['content'],
        owner = self.GPADMIN,
        group = self.GPADMIN,
        mode=0644
        )

    self.assertResourceCalled('File', '/usr/local/hawq/etc/slaves',
        content = InlineTemplate('c6401.ambari.apache.org\nc6402.ambari.apache.org\nc6403.ambari.apache.org\n\n'),
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('Directory', '/data/hawq/master',
        group = self.GPADMIN,
        owner = self.GPADMIN,
        create_parents = True
        )

    self.assertResourceCalled('Execute', 'chmod 700 /data/hawq/master',
        user = 'root',
        timeout =  600
        )

    self.assertResourceCalled('Directory', '/data/hawq/tmp/master',
        group = self.GPADMIN,
        owner = self.GPADMIN,
        create_parents = True
        )


  @patch ('hawqmaster.common.__set_osparams')
  def test_configure_default(self, set_osparams_mock):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'configure',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.__asserts_for_configure()
    self.assertNoMoreResources()


  @patch ('hawqmaster.common.__set_osparams')
  def test_install_default(self, set_osparams_mock):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'install',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.__asserts_for_configure()
    self.assertNoMoreResources()


  @patch ('hawqmaster.common.__set_osparams')
  def test_start_default(self, set_osparams_mock):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'start',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.__asserts_for_configure()

    self.assertResourceCalled('HdfsResource', '/hawq_default',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        default_fs = u'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        type = 'directory',
        action = ['create_on_execute'],
        owner = self.GPADMIN,
        group = self.GPADMIN,
        user = u'hdfs',
        mode = 493,
        security_enabled = False,
        kinit_path_local = '/usr/bin/kinit',
        recursive_chown = True,
        keytab = UnknownConfigurationMock(),
        principal_name = UnknownConfigurationMock(),
        )

    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        default_fs = u'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        action = ['execute'],
        user = u'hdfs',
        security_enabled = False,
        kinit_path_local = '/usr/bin/kinit',
        keytab = UnknownConfigurationMock(),
        principal_name = UnknownConfigurationMock()
        )

    self.assertResourceCalled('Execute', 'source /usr/local/hawq/greenplum_path.sh && hawq init master -a -v --ignore-bad-hosts',
        logoutput = True, 
        not_if = None, 
        only_if = None, 
        user = self.GPADMIN,
        timeout = 900
        )

    self.assertNoMoreResources()


  def __asserts_for_stop(self, componentCommand, expectedCommand):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = componentCommand,
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.assertResourceCalled('Execute', expectedCommand,
        logoutput = True,
        not_if = None,
        only_if = "netstat -tupln | egrep ':5432\\s' | egrep postgres",
        user = self.GPADMIN,
        timeout = 900
        )

    self.assertNoMoreResources()


  @patch ('hawqmaster.common.__set_osparams')
  @patch ('common.get_local_hawq_site_property_value')
  def test_stop_default(self, get_local_hawq_site_property_value_mock, set_osparams_mock):
    """ Run Stop HAWQMASTER """

    get_local_hawq_site_property_value_mock.return_value = 5432
    self.__asserts_for_stop('stop', 'source /usr/local/hawq/greenplum_path.sh && hawq stop master -M fast -a -v')


  @patch ('hawqmaster.common.__set_osparams')
  @patch ('common.get_local_hawq_site_property_value')
  def test_stop_cluster_immediate(self, get_local_hawq_site_property_value_mock, set_osparams_mock):
    """ Run Stop HAWQ Cluster Immediate Mode """

    get_local_hawq_site_property_value_mock.return_value = 5432
    self.__asserts_for_stop('immediate_stop_hawq_service','source /usr/local/hawq/greenplum_path.sh && hawq stop cluster -M immediate -a -v')


  def __asserts_for_hawq_check(self, expectedCommand):

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'run_hawq_check',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.assertResourceCalled('File', "/usr/local/hawq/etc/hawq_hosts",
        content = InlineTemplate("{% for host in hawq_all_hosts %}{{host}}\n{% endfor %}"),
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('Execute', expectedCommand,
        user=self.GPADMIN,
        timeout=600
        )

    self.assertNoMoreResources()


  def test_run_hawq_check_case1(self):
    """ Running HAWQ Check Case 1: Non HDFS-HA, Standalone Resource Management, Not Kerberized """

    expectedCommand = self.HAWQ_CHECK_COMMAND
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case2(self):
    """ Running HAWQ Check Case 2: Non HDFS-HA, Standalone Resource Management, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    expectedCommand = "{0}--kerberos".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case3(self):
    """ Running HAWQ Check Case 3: Non HDFS-HA, YARN Resource Management Non YARN_HA, Not Kerberized """

    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    expectedCommand = "{0}--yarn".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case4(self):
    """ Running HAWQ Check Case 4: Non HDFS-HA, YARN Resource Management Non YARN_HA, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    expectedCommand = "{0}--yarn --kerberos".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case5(self):
    """ Running HAWQ Check Case 5: Non HDFS-HA, YARN Resource Management YARN_HA, Not Kerberized """

    self.config_dict['configurations']['yarn-site']['yarn.resourcemanager.ha.enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    expectedCommand = "{0}--yarn-ha".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case6(self):
    """ Running HAWQ Check Case 6: Non HDFS-HA, YARN Resource Management YARN_HA, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    self.config_dict['configurations']['yarn-site']['yarn.resourcemanager.ha.enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    expectedCommand = "{0}--yarn-ha --kerberos".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case7(self):
    """ Running HAWQ Check Case 7: HDFS-HA, Standalone Resource Management, Not Kerberized """

    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}--hdfs-ha".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case8(self):
    """ Running HAWQ Check Case 8: HDFS-HA, Standalone Resource Management, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}--hdfs-ha --kerberos".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case9(self):
    """ Running HAWQ Check Case 9: HDFS-HA, YARN Resource Management Non YARN_HA, Not Kerberized """

    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}--hdfs-ha --yarn".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case10(self):
    """ Running HAWQ Check Case 10: HDFS-HA, YARN Resource Management Non YARN_HA, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}--hdfs-ha --yarn --kerberos".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case11(self):
    """ Running HAWQ Check Case 11: HDFS-HA, YARN Resource Management YARN_HA, Not Kerberized """

    self.config_dict['configurations']['yarn-site']['yarn.resourcemanager.ha.enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}--hdfs-ha --yarn-ha".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case12(self):
    """ Running HAWQ Check Case 12: HDFS-HA, YARN Resource Management YARN_HA, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    self.config_dict['configurations']['yarn-site']['yarn.resourcemanager.ha.enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}--hdfs-ha --yarn-ha --kerberos".format(self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_resync_hawq_standby(self):
    """ Run custom command Resync HAWQ Standby """

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'resync_hawq_standby',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.assertResourceCalled('Execute', 'source /usr/local/hawq/greenplum_path.sh && hawq init standby -n -a -v -M fast',
        user = self.GPADMIN,
        timeout = 900,
        not_if = None,
        only_if = None,
        logoutput = True
        )

    self.assertNoMoreResources()


  def test_remove_hawq_standby(self):
    """ Run custom command Remove HAWQ Standby """

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'remove_hawq_standby',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = RMFTestCase.TARGET_COMMON_SERVICES
        )

    self.assertResourceCalled('Execute', 'source /usr/local/hawq/greenplum_path.sh && hawq init standby -a -v -r --ignore-bad-hosts',
        user = self.GPADMIN,
        timeout = 900,
        not_if = None,
        only_if = None,
        logoutput = True
        )

    self.assertNoMoreResources()
