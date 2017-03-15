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

from mock.mock import patch
from stacks.utils.RMFTestCase import InlineTemplate, UnknownConfigurationMock
from hawq_base_test_case import HawqBaseTestCase


class TestHawqMaster(HawqBaseTestCase):

  COMPONENT_TYPE = 'master'
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']
  HAWQ_CHECK_COMMAND = 'export PGHOST="c6403.ambari.apache.org" && hawq check -f /usr/local/hawq/etc/hawq_hosts --hadoop /usr/phd/current/hadoop-client --config /usr/local/hawq/etc/hawq_check.cnf '

  @patch ('common.__set_osparams')
  def test_configure_default(self, set_osparams_mock):

    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'configure',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.asserts_for_configure()
    self.assertNoMoreResources()


  @patch ('common.__set_osparams')
  def test_install_default(self, set_osparams_mock):

    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'install',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.asserts_for_configure()
    self.assertNoMoreResources()


  @patch ('common.__set_osparams')
  @patch ('utils.exec_psql_cmd')
  @patch ('common.__get_hdfs_dir_owner')
  def test_start_default(self, owner_mock, psql_mock, set_osparams_mock):

    owner_mock.return_value = 'postgres'
    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'start',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.asserts_for_configure()

    self.assertResourceCalled('HdfsResource', '/hawq_data',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        default_fs = u'hdfs://c6401.ambari.apache.org:8020',
        hadoop_bin_dir = '/usr/phd/current/hadoop-client/bin',
        hadoop_conf_dir = '/usr/phd/current/hadoop-client/conf',
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
        principal_name = UnknownConfigurationMock()
        )

    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        default_fs = u'hdfs://c6401.ambari.apache.org:8020',
        hadoop_bin_dir = '/usr/phd/current/hadoop-client/bin',
        hadoop_conf_dir = '/usr/phd/current/hadoop-client/conf',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        action = ['execute'],
        user = u'hdfs',
        security_enabled = False,
        kinit_path_local = '/usr/bin/kinit',
        keytab = UnknownConfigurationMock(),
        principal_name = UnknownConfigurationMock()
        )

    self.assertResourceCalled('Execute', self.SOURCE_HAWQ_SCRIPT + 'hawq init master -a -v --ignore-bad-hosts',
        logoutput = True,
        not_if = None,
        only_if = None,
        user = self.GPADMIN,
        timeout = 900
        )

    self.assertNoMoreResources()


  def asserts_for_stop(self, componentCommand, expectedCommand):

    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = componentCommand,
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.assertResourceCalled('Execute', expectedCommand,
        logoutput = True,
        not_if = None,
        only_if = "netstat -tupln | egrep ':5432\\s' | egrep postgres",
        user = self.GPADMIN,
        timeout = 900
        )

    self.assertNoMoreResources()


  @patch ('common.__set_osparams')
  @patch ('common.get_local_hawq_site_property_value')
  def test_stop_default(self, get_local_hawq_site_property_value_mock, set_osparams_mock):
    """ Run Stop HAWQMASTER """

    get_local_hawq_site_property_value_mock.return_value = 5432
    self.asserts_for_stop('stop', self.SOURCE_HAWQ_SCRIPT + 'hawq stop master -M fast -a -v')


  @patch ('common.__set_osparams')
  @patch ('common.get_local_hawq_site_property_value')
  def test_stop_cluster_immediate(self, get_local_hawq_site_property_value_mock, set_osparams_mock):
    """ Run Stop HAWQ Cluster Immediate Mode """

    get_local_hawq_site_property_value_mock.return_value = 5432
    self.asserts_for_stop('immediate_stop_hawq_service', self.SOURCE_HAWQ_SCRIPT + 'hawq stop cluster -M immediate -a -v')


  def __asserts_for_hawq_check(self, expectedCommand):

    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'run_hawq_check',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.assertResourceCalled('File', self.CONF_DIR + 'hawq_hosts',
        content = InlineTemplate("{% for host in hawq_all_hosts %}{{host}}\n{% endfor %}"),
        group = self.GPADMIN,
        owner = self.GPADMIN,
        mode = 0644
        )

    self.assertResourceCalled('Execute', expectedCommand,
        logoutput = True,
        not_if = None,
        only_if = None,
        user=self.GPADMIN,
        timeout=900
        )

    self.assertNoMoreResources()


  def test_run_hawq_check_case1(self):
    """ Running HAWQ Check Case 1: Non HDFS-HA, Standalone Resource Management, Not Kerberized """

    expectedCommand = self.SOURCE_HAWQ_SCRIPT + self.HAWQ_CHECK_COMMAND
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case2(self):
    """ Running HAWQ Check Case 2: Non HDFS-HA, Standalone Resource Management, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    expectedCommand = "{0}{1}--kerberos".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case3(self):
    """ Running HAWQ Check Case 3: Non HDFS-HA, YARN Resource Management Non YARN_HA, Not Kerberized """

    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    expectedCommand = "{0}{1}--yarn".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case4(self):
    """ Running HAWQ Check Case 4: Non HDFS-HA, YARN Resource Management Non YARN_HA, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    expectedCommand = "{0}{1}--yarn --kerberos".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case5(self):
    """ Running HAWQ Check Case 5: Non HDFS-HA, YARN Resource Management YARN_HA, Not Kerberized """

    self.config_dict['configurations']['yarn-site']['yarn.resourcemanager.ha.enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    expectedCommand = "{0}{1}--yarn-ha".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case6(self):
    """ Running HAWQ Check Case 6: Non HDFS-HA, YARN Resource Management YARN_HA, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    self.config_dict['configurations']['yarn-site']['yarn.resourcemanager.ha.enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    expectedCommand = "{0}{1}--yarn-ha --kerberos".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case7(self):
    """ Running HAWQ Check Case 7: HDFS-HA, Standalone Resource Management, Not Kerberized """

    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}{1}--hdfs-ha".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case8(self):
    """ Running HAWQ Check Case 8: HDFS-HA, Standalone Resource Management, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}{1}--hdfs-ha --kerberos".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case9(self):
    """ Running HAWQ Check Case 9: HDFS-HA, YARN Resource Management Non YARN_HA, Not Kerberized """

    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}{1}--hdfs-ha --yarn".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case10(self):
    """ Running HAWQ Check Case 10: HDFS-HA, YARN Resource Management Non YARN_HA, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}{1}--hdfs-ha --yarn --kerberos".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case11(self):
    """ Running HAWQ Check Case 11: HDFS-HA, YARN Resource Management YARN_HA, Not Kerberized """

    self.config_dict['configurations']['yarn-site']['yarn.resourcemanager.ha.enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}{1}--hdfs-ha --yarn-ha".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_run_hawq_check_case12(self):
    """ Running HAWQ Check Case 12: HDFS-HA, YARN Resource Management YARN_HA, Kerberized """

    self.config_dict['configurations']['cluster-env']['security_enabled'] = "true"
    self.config_dict['configurations']['yarn-site']['yarn.resourcemanager.ha.enabled'] = "true"
    self.config_dict['configurations']['hawq-site']['hawq_global_rm_type'] = "yarn"
    self.config_dict['configurations']['hdfs-site']['dfs.nameservices'] = "haservice"
    expectedCommand = "{0}{1}--hdfs-ha --yarn-ha --kerberos".format(self.SOURCE_HAWQ_SCRIPT, self.HAWQ_CHECK_COMMAND)
    self.__asserts_for_hawq_check(expectedCommand)


  def test_resync_hawq_standby(self):
    """ Run custom command Resync HAWQ Standby """

    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'resync_hawq_standby',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.assertResourceCalled('Execute', self.SOURCE_HAWQ_SCRIPT + 'export PGHOST="c6403.ambari.apache.org" && hawq init standby -n -a -v -M fast',
        user = self.GPADMIN,
        timeout = 900,
        not_if = None,
        only_if = None,
        logoutput = True
        )

    self.assertNoMoreResources()


  def test_remove_hawq_standby(self):
    """ Run custom command Remove HAWQ Standby """

    self.executeScript(self.HAWQ_PACKAGE_DIR + '/scripts/hawqmaster.py',
        classname = 'HawqMaster',
        command = 'remove_hawq_standby',
        config_dict = self.config_dict,
        stack_version = self.STACK_VERSION,
        target = self.TARGET_COMMON_SERVICES
        )

    self.assertResourceCalled('Execute', self.SOURCE_HAWQ_SCRIPT + 'export PGHOST="c6403.ambari.apache.org" && hawq init standby -a -v -r --ignore-bad-hosts',
        user = self.GPADMIN,
        timeout = 900,
        not_if = None,
        only_if = None,
        logoutput = True
        )

    self.assertNoMoreResources()
