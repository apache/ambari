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
from stacks.utils.RMFTestCase import *
import os

origin_exists = os.path.exists
@patch.object(os.path, "exists", new=MagicMock(
  side_effect=lambda *args: origin_exists(args[0])
  if args[0][-2:] == "j2" else True))
class TestServiceCheck(RMFTestCase):

  def test_service_check_default(self):

    self.executeScript("2.0.6/services/YARN/package/scripts/mapred_service_check.py",
                      classname="MapReduce2ServiceCheck",
                      command="service_check",
                      config_file="default.json"
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -rm -r -f /user/ambari-qa/mapredsmokeoutput /user/ambari-qa/mapredsmokeinput',
                      try_sleep = 5,
                      tries = 1,
                      user = 'ambari-qa',
                      bin_dir =  os.environ['PATH'] + os.pathsep + "/usr/bin" + os.pathsep + "/usr/lib/hadoop-yarn/bin",
                      conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -put /etc/passwd /user/ambari-qa/mapredsmokeinput',
                      try_sleep = 5,
                      tries = 1,
                      bin_dir =  os.environ['PATH'] + os.pathsep + "/usr/bin" + os.pathsep + "/usr/lib/hadoop-yarn/bin",
                      user = 'ambari-qa',
                      conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'jar /usr/lib/hadoop-mapreduce/hadoop-mapreduce-examples-2.*.jar wordcount /user/ambari-qa/mapredsmokeinput /user/ambari-qa/mapredsmokeoutput',
                      logoutput = True,
                      try_sleep = 5,
                      tries = 1,
                      bin_dir =  os.environ['PATH'] + os.pathsep + "/usr/bin" + os.pathsep + "/usr/lib/hadoop-yarn/bin",
                      user = 'ambari-qa',
                      conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /user/ambari-qa/mapredsmokeoutput',
                      user = 'ambari-qa',
                      bin_dir =  os.environ['PATH'] + os.pathsep + "/usr/bin" + os.pathsep + "/usr/lib/hadoop-yarn/bin",
                      conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()

  def test_service_check_secured(self):

    self.executeScript("2.0.6/services/YARN/package/scripts/mapred_service_check.py",
                      classname="MapReduce2ServiceCheck",
                      command="service_check",
                      config_file="secured.json"
    )
    self.assertResourceCalled('Execute', '/usr/bin/kinit -kt /etc/security/keytabs/smokeuser.headless.keytab ambari-qa;',
                      user = 'ambari-qa',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -rm -r -f /user/ambari-qa/mapredsmokeoutput /user/ambari-qa/mapredsmokeinput',
                      try_sleep = 5,
                      tries = 1,
                      user = 'ambari-qa',
                      bin_dir =  os.environ['PATH'] + os.pathsep + "/usr/bin" + os.pathsep + "/usr/lib/hadoop-yarn/bin",
                      conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -put /etc/passwd /user/ambari-qa/mapredsmokeinput',
                      try_sleep = 5,
                      tries = 1,
                      bin_dir =  os.environ['PATH'] + os.pathsep + "/usr/bin" + os.pathsep + "/usr/lib/hadoop-yarn/bin",
                      user = 'ambari-qa',
                      conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'jar /usr/lib/hadoop-mapreduce/hadoop-mapreduce-examples-2.*.jar wordcount /user/ambari-qa/mapredsmokeinput /user/ambari-qa/mapredsmokeoutput',
                      logoutput = True,
                      try_sleep = 5,
                      tries = 1,
                      bin_dir =  os.environ['PATH'] + os.pathsep + "/usr/bin" + os.pathsep + "/usr/lib/hadoop-yarn/bin",
                      user = 'ambari-qa',
                      conf_dir = '/etc/hadoop/conf',
    )
    self.assertResourceCalled('ExecuteHadoop', 'fs -test -e /user/ambari-qa/mapredsmokeoutput',
                      user = 'ambari-qa',
                      bin_dir =  os.environ['PATH'] + os.pathsep + "/usr/bin" + os.pathsep + "/usr/lib/hadoop-yarn/bin",
                      conf_dir = '/etc/hadoop/conf',
    )
    self.assertNoMoreResources()
