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
import os
from mock.mock import MagicMock, call, patch
from stacks.utils.RMFTestCase import *
import datetime, sys, socket
import resource_management.libraries.functions

@patch.object(resource_management.libraries.functions, "get_unique_id_and_date", new = MagicMock(return_value=''))
@patch("socket.socket")
@patch("time.time", new=MagicMock(return_value=1431110511.43))
class TestServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "HIVE/0.12.0.2.0/package"
  STACK_VERSION = "2.0.6"
  DEFAULT_IMMUTABLE_PATHS = ['/apps/hive/warehouse', '/apps/falcon', '/mr-history/done', '/app-logs', '/tmp']


  def test_service_check_default(self, socket_mock):
    config_file = "default.json"

    base_path, configs_path = self._get_test_paths(RMFTestCase.TARGET_COMMON_SERVICES, self.STACK_VERSION)
    json_content = self.get_config_file(configs_path, config_file)

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                        classname="HiveServiceCheck",
                        command="service_check",
                        config_dict = json_content,
                        stack_version = self.STACK_VERSION,
                        target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', "! beeline -u 'jdbc:hive2://c6402.ambari.apache.org:10000/;transportMode=binary;auth=noSasl'  -e '' 2>&1| awk '{print}'|grep -i -e 'Connection refused' -e 'Invalid URL'",
                              path = ['/bin/', '/usr/bin/', '/usr/lib/hive/bin/', '/usr/sbin/'],
                              user = 'ambari-qa',
                              timeout = 30,
                              timeout_kill_strategy = 2,
                              )
    self.assertResourceCalled('File', '/tmp/hcatSmoke.sh',
                        content = StaticFile('hcatSmoke.sh'),
                        mode = 0755,
    )
    self.assertResourceCalled('Execute', 'env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/hcatSmoke.sh hcatsmoke prepare false',
        logoutput = True,
        path = ['/usr/sbin',
           '/usr/local/bin',
           '/bin',
           '/usr/bin',
           '/bin:/usr/hdp/current/hive-server2/bin:/usr/bin'],
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5,
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /apps/hive/warehouse/hcatsmoke',
        conf_dir = '/etc/hadoop/conf',
        logoutput = True,
        user = 'hdfs',
        bin_dir = '/bin:/usr/hdp/current/hive-server2/bin:/usr/bin',
    )
    self.assertResourceCalled('Execute', ' /tmp/hcatSmoke.sh hcatsmoke cleanup false',
        logoutput = True,
        path = ['/usr/sbin',
           '/usr/local/bin',
           '/bin',
           '/usr/bin',
           '/bin:/usr/hdp/current/hive-server2/bin:/usr/bin'],
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5,
    )
    self.assertResourceCalled('File', '/tmp/templetonSmoke.sh',
                              content = StaticFile('templetonSmoke.sh'),
                              mode = 0755,
                              )
    self.assertResourceCalled('File', '/tmp/idtest.ambari-qa.1431110511.43.pig',
        content = Template('templeton_smoke.pig.j2', templeton_test_input='/tmp/idtest.ambari-qa.1431110511.43.in', templeton_test_output='/tmp/idtest.ambari-qa.1431110511.43.out'),
        owner="hdfs"
    )
    self.assertResourceCalled('HdfsResource', '/tmp/idtest.ambari-qa.1431110511.43.pig',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        source = '/tmp/idtest.ambari-qa.1431110511.43.pig',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'missing_principal',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'file',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )
    self.assertResourceCalled('HdfsResource', '/tmp/idtest.ambari-qa.1431110511.43.in',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        source = '/etc/passwd',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'missing_principal',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'file',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = False,
        hadoop_bin_dir = '/usr/bin',
        keytab = UnknownConfigurationMock(),
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'missing_principal',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Execute', '/tmp/templetonSmoke.sh c6402.ambari.apache.org ambari-qa 50111 idtest.ambari-qa.1431110511.43.pig True no_keytab false /usr/bin/kinit no_principal /tmp',
        logoutput = True,
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        tries = 3,
        try_sleep = 5,
    )
    self.assertNoMoreResources()


  def test_service_check_secured(self, socket_mock):
    config_file = "secured.json"
    base_path, configs_path = self._get_test_paths(RMFTestCase.TARGET_COMMON_SERVICES, self.STACK_VERSION)
    json_content = self.get_config_file(configs_path, config_file)
    del json_content["commandParams"]["version"]

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                        classname="HiveServiceCheck",
                        command="service_check",
                        config_dict = json_content,
                        stack_version = self.STACK_VERSION,
                        target = RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM; ',
                              user = 'ambari-qa',
                              )
    self.assertResourceCalled('Execute', "! beeline -u 'jdbc:hive2://c6402.ambari.apache.org:10000/;transportMode=binary;principal=hive/_HOST@EXAMPLE.COM'  -e '' 2>&1| awk '{print}'|grep -i -e 'Connection refused' -e 'Invalid URL'",
                              path = ['/bin/', '/usr/bin/', '/usr/lib/hive/bin/', '/usr/sbin/'],
                              user = 'ambari-qa',
                              timeout = 30,
                              timeout_kill_strategy = 2,
                              )
    self.assertResourceCalled('File', '/tmp/hcatSmoke.sh',
                        content = StaticFile('hcatSmoke.sh'),
                        mode = 0755,
    )
    self.maxDiff = None
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM; env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/hcatSmoke.sh hcatsmoke prepare false',
        logoutput = True,
        path = ['/usr/sbin','/usr/local/bin','/bin','/usr/bin', '/bin:/usr/hdp/current/hive-server2/bin:/usr/bin'],
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5,
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/hdfs.headless.keytab hdfs',
        user = 'hdfs',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /apps/hive/warehouse/hcatsmoke',
        conf_dir = '/etc/hadoop/conf',
        logoutput = True,
        user = 'hdfs',
        bin_dir = '/bin:/usr/hdp/current/hive-server2/bin:/usr/bin',
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM;  /tmp/hcatSmoke.sh hcatsmoke cleanup false',
        logoutput = True,
        path = ['/usr/sbin',
           '/usr/local/bin',
           '/bin',
           '/usr/bin',
           '/bin:/usr/hdp/current/hive-server2/bin:/usr/bin'],
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5,
    )
    self.assertResourceCalled('File', '/tmp/templetonSmoke.sh',
                              content = StaticFile('templetonSmoke.sh'),
                              mode = 0755,
                              )

    self.assertResourceCalled('File', '/tmp/idtest.ambari-qa.1431110511.43.pig',
        content = Template('templeton_smoke.pig.j2', templeton_test_input='/tmp/idtest.ambari-qa.1431110511.43.in', templeton_test_output='/tmp/idtest.ambari-qa.1431110511.43.out'),
        owner = "hdfs"
    )
    self.assertResourceCalled('HdfsResource', '/tmp/idtest.ambari-qa.1431110511.43.pig',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        source = '/tmp/idtest.ambari-qa.1431110511.43.pig',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'file',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )
    self.assertResourceCalled('HdfsResource', '/tmp/idtest.ambari-qa.1431110511.43.in',
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        source = '/etc/passwd',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs',
        user = 'hdfs',
        dfs_type = '',
        owner = 'ambari-qa',
        hadoop_conf_dir = '/etc/hadoop/conf',
        type = 'file',
        action = ['create_on_execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
    )
    self.assertResourceCalled('HdfsResource', None,
        immutable_paths = self.DEFAULT_IMMUTABLE_PATHS,
        security_enabled = True,
        hadoop_bin_dir = '/usr/bin',
        keytab = '/etc/security/keytabs/hdfs.headless.keytab',
        default_fs = 'hdfs://c6401.ambari.apache.org:8020',
        hdfs_site = self.getConfig()['configurations']['hdfs-site'],
        kinit_path_local = '/usr/bin/kinit',
        principal_name = 'hdfs',
        user = 'hdfs',
        dfs_type = '',
        action = ['execute'], hdfs_resource_ignore_file='/var/lib/ambari-agent/data/.hdfs_resource_ignore',
        hadoop_conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('Execute', '/tmp/templetonSmoke.sh c6402.ambari.apache.org ambari-qa 50111 idtest.ambari-qa.1431110511.43.pig True /etc/security/keytabs/smokeuser.headless.keytab true /usr/bin/kinit ambari-qa@EXAMPLE.COM /tmp',
        logoutput = True,
        path = ['/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin'],
        tries = 3,
        try_sleep = 5,
    )
    self.assertNoMoreResources()


  def test_service_check_during_upgrade(self, socket_mock):
    config_file = self.get_src_folder() + "/test/python/stacks/2.2/configs/hive-upgrade.json"
    with open(config_file, 'r') as f:
      json_content = json.load(f)

    json_content['commandParams']['version'] = "2.3.0.0-1234"

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
      classname="HiveServiceCheck",
      command="service_check",
      config_dict = json_content,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Execute', "! beeline -u 'jdbc:hive2://c6402.ambari.apache.org:10010/;transportMode=binary'  -e '' 2>&1| awk '{print}'|grep -i -e 'Connection refused' -e 'Invalid URL'",
      path = ['/bin/', '/usr/bin/', '/usr/lib/hive/bin/', '/usr/sbin/'],
      timeout = 30,
      user = 'ambari-qa',
      timeout_kill_strategy = 2,
    )

    self.assertResourceCalled('File', '/tmp/hcatSmoke.sh',
      content = StaticFile('hcatSmoke.sh'),
      mode = 0755)

    self.assertResourceCalled('Execute', "env JAVA_HOME=/usr/jdk64/jdk1.7.0_45 /tmp/hcatSmoke.sh hcatsmoke prepare true",
        logoutput = True,
        path = ['/usr/sbin','/usr/local/bin','/bin','/usr/bin', '/bin:/usr/hdp/2.3.0.0-1234/hadoop/bin:/usr/hdp/2.3.0.0-1234/hive/bin'],
        tries = 3,
        user = 'ambari-qa',
        try_sleep = 5)


  def test_service_check_during_upgrade_for_llap(self, socket_mock):
    config_file = self.get_src_folder() + "/test/python/stacks/2.2/configs/hive-upgrade.json"
    with open(config_file, 'r') as f:
      json_content = json.load(f)

    # populate version and an LLAP instance to trigger the LLAP service check
    json_content['commandParams']['version'] = "2.3.0.0-1234"
    json_content['clusterHostInfo']['hive_server_interactive_hosts'] = ["c6402.ambari.apache.org"]
    json_content['configurations']['hive-interactive-env'] = {}
    json_content['configurations']['hive-interactive-env']['enable_hive_interactive'] = True


    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
      classname = "HiveServiceCheck",
      command = "service_check",
      config_dict = json_content,
      stack_version = self.STACK_VERSION,
      target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Execute',
      "! beeline -u 'jdbc:hive2://c6402.ambari.apache.org:10010/;transportMode=binary'  -e '' 2>&1| awk '{print}'|grep -i -e 'Connection refused' -e 'Invalid URL'",
      path = ['/bin/', '/usr/bin/', '/usr/lib/hive/bin/', '/usr/sbin/'],
      timeout = 30,
      user = 'ambari-qa',
      timeout_kill_strategy = 2,
    )

    self.assertResourceCalled('Execute',
      "! beeline -u 'jdbc:hive2://c6402.ambari.apache.org:10500/;transportMode=binary'  -e '' 2>&1| awk '{print}'|grep -i -e 'Connection refused' -e 'Invalid URL'",
      path = ['/bin/', '/usr/bin/', '/usr/lib/hive/bin/', '/usr/sbin/'],
      timeout = 30,
      user = 'ambari-qa',
      timeout_kill_strategy = 2,
    )

    # LLAP call
    self.assertResourceCalled('Execute',
      "! beeline -u 'jdbc:hive2://c6402.ambari.apache.org:10500/;transportMode=binary' --hiveconf \"hiveLlapServiceCheck=\" -f /usr/hdp/current/hive-server2-hive2/scripts/llap/sql/serviceCheckScript.sql -e '' 2>&1| awk '{print}'|grep -i -e 'Invalid status\|Invalid URL\|command not found\|Connection refused'",
      path = ['/usr/sbin', '/usr/local/bin', '/bin', '/usr/bin', '/bin:/usr/hdp/2.3.0.0-1234/hadoop/bin:/usr/hdp/current/hive-server2-hive2/bin'],
      tries = 1,
      stderr = -1,
      wait_for_finish = True,
      logoutput = True,
      user = 'hive')
