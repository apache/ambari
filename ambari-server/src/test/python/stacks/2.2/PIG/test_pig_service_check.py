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
from mock.mock import patch, MagicMock

from stacks.utils.RMFTestCase import *
from resource_management.libraries.functions import dynamic_variable_interpretation


class TestPigServiceCheck(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "PIG/0.12.0.2.0/package"
  STACK_VERSION = "2.2"

  @patch.object(dynamic_variable_interpretation, "copy_tarballs_to_hdfs")
  def test_service_check_secure(self, copy_tarball_mock):
    copy_tarball_mock.return_value = 0

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/service_check.py",
                       classname="PigServiceCheck",
                       command="service_check",
                       config_file="pig-service-check-secure.json",
                       hdp_stack_version=self.STACK_VERSION,
                       target=RMFTestCase.TARGET_COMMON_SERVICES
    )
    self.assertResourceCalled("ExecuteHadoop", "dfs -rmr pigsmoke.out passwd; hadoop --config /etc/hadoop/conf dfs -put /etc/passwd passwd ",
      try_sleep=5,
      tries=3,
      user="ambari-qa",
      conf_dir="/etc/hadoop/conf",
      security_enabled=True,
      principal="ambari-qa@EXAMPLE.COM",
      keytab="/etc/security/keytabs/smokeuser.headless.keytab",
      bin_dir="/usr/hdp/current/hadoop-client/bin",
      kinit_path_local="/usr/bin/kinit"
    )

    self.assertResourceCalled("File", "/tmp/pigSmoke.sh",
      content=StaticFile("pigSmoke.sh"),
      mode=0755
    )

    self.assertResourceCalled("Execute", "pig /tmp/pigSmoke.sh",
      path=["/usr/hdp/current/pig-client/bin:/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin"],
      tries=3,
      user="ambari-qa",
      try_sleep=5
    )

    self.assertResourceCalled("ExecuteHadoop", "fs -test -e pigsmoke.out",
      user="ambari-qa",
      bin_dir="/usr/hdp/current/hadoop-client/bin",
      conf_dir="/etc/hadoop/conf"
    )

    # Specific to HDP 2.2 and kerberized cluster
    self.assertResourceCalled("ExecuteHadoop", "dfs -rmr pigsmoke.out passwd; hadoop --config /etc/hadoop/conf dfs -put /etc/passwd passwd ",
      tries=3,
      try_sleep=5,
      user="ambari-qa",
      conf_dir="/etc/hadoop/conf",
      keytab="/etc/security/keytabs/smokeuser.headless.keytab",
      principal="ambari-qa@EXAMPLE.COM",
      security_enabled=True,
      kinit_path_local="/usr/bin/kinit",
      bin_dir="/usr/hdp/current/hadoop-client/bin"
    )

    copy_tarball_mock.assert_called_once_with("tez", "hadoop-client", "ambari-qa", "hdfs", "hadoop")

    self.assertResourceCalled("Execute", "/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa@EXAMPLE.COM;",
      user="ambari-qa")

    self.assertResourceCalled("Execute", "pig -x tez /tmp/pigSmoke.sh",
      tries=3,
      try_sleep=5,
      path=["/usr/hdp/current/pig-client/bin:/usr/sbin:/sbin:/usr/local/bin:/bin:/usr/bin"],
      user="ambari-qa"
    )

    self.assertResourceCalled("ExecuteHadoop", "fs -test -e pigsmoke.out",
      user="ambari-qa",
      bin_dir="/usr/hdp/current/hadoop-client/bin",
      conf_dir="/etc/hadoop/conf"
    )
    self.assertNoMoreResources()

