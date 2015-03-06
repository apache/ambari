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
import sys
import os
from mock.mock import MagicMock, patch

from stacks.utils.RMFTestCase import *
from resource_management.core import shell
from resource_management.libraries.functions import dynamic_variable_interpretation

class TestJobHistoryServer(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "SPARK/1.2.0.2.2/package"
  STACK_VERSION = "2.2"

  def setUp(self):
    sys.path.insert(0, os.path.join(os.getcwd(),
      "../../main/resources/common-services", self.COMMON_SERVICES_PACKAGE_DIR,
      "scripts"))

  @patch.object(shell, "call")
  @patch("setup_spark.create_file")
  @patch("setup_spark.write_properties_to_file")
  @patch.object(dynamic_variable_interpretation, "copy_tarballs_to_hdfs")
  def test_start(self, copy_tarball_mock, write_properties_to_file_mock, create_file_mock, call_mock):
    hdp_version = "2.2.2.0-2538"
    call_mock.return_value = (0, hdp_version)
    copy_tarball_mock.return_value = 0

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/job_history_server.py",
                         classname="JobHistoryServer",
                         command="start",
                         config_file="spark-job-history-server.json",
                         hdp_stack_version=self.STACK_VERSION,
                         target=RMFTestCase.TARGET_COMMON_SERVICES
    )

    self.assertTrue(create_file_mock.called)
    self.assertTrue(write_properties_to_file_mock.called)


    self.assertResourceCalled("Directory", "/var/run/spark",
                              owner="spark",
                              group="hadoop",
                              recursive=True
    )
    self.assertResourceCalled("Directory", "/var/log/spark",
                              owner="spark",
                              group="hadoop",
                              recursive=True
    )
    self.assertResourceCalled("HdfsDirectory", "/user/spark",
                              security_enabled=False,
                              keytab=UnknownConfigurationMock(),
                              conf_dir="/etc/hadoop/conf",
                              hdfs_user="hdfs",
                              kinit_path_local="/usr/bin/kinit",
                              mode=509,
                              owner="spark",
                              bin_dir="/usr/hdp/current/hadoop-client/bin",
                              action=["create"]
    )
    self.assertResourceCalled("File", "/etc/spark/conf/spark-env.sh",
                              owner="spark",
                              group="spark",
                              content=InlineTemplate(self.getConfig()['configurations']['spark-env']['content'])
    )
    self.assertResourceCalled("File", "/etc/spark/conf/log4j.properties",
                              owner="spark",
                              group="spark",
                              content=self.getConfig()['configurations']['spark-log4j-properties']['content']
    )
    self.assertResourceCalled("File", "/etc/spark/conf/metrics.properties",
                              owner="spark",
                              group="spark",
                              content=InlineTemplate(self.getConfig()['configurations']['spark-metrics-properties']['content'])
    )
    self.assertResourceCalled("File", "/etc/spark/conf/java-opts",
                              owner="spark",
                              group="spark",
                              content="  -Dhdp.version=" + hdp_version
    )

    copy_tarball_mock.assert_called_with("tez", "spark-historyserver", "spark", "hdfs", "hadoop")
    
    self.assertResourceCalled("Execute", "/usr/hdp/current/spark-historyserver/sbin/start-history-server.sh",
                              not_if="ls /var/run/spark/spark-spark-org.apache.spark.deploy.history.HistoryServer-1.pid >/dev/null 2>&1 && ps -p `cat /var/run/spark/spark-spark-org.apache.spark.deploy.history.HistoryServer-1.pid` >/dev/null 2>&1",
                              environment={'JAVA_HOME': '/usr/jdk64/jdk1.7.0_67'},
                              user="spark"
    )
