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
import resource_management.core.source
import os


class TestFlumeHandler(RMFTestCase):
  COMMON_SERVICES_PACKAGE_DIR = "FLUME/1.4.0.2.0/package"
  STACK_VERSION = "2.0.6"
  
  def test_configure_default(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assert_configure_default()
    self.assertNoMoreResources()

  @patch("os.path.isfile")
  @patch("flume.cmd_target_names")
  @patch("flume._set_desired_state")
  def test_start_default(self, set_desired_mock, cmd_target_names_mock, os_path_isfile_mock):
    # 1st call is to check if the conf file is there - that should be True
    # 2nd call is to check if the process is live - that should be False
    os_path_isfile_mock.side_effect = [True, False]
    cmd_target_names_mock.return_value = ["a1"]

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "start",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assert_configure_default()

    self.assertTrue(set_desired_mock.called)
    self.assertTrue(set_desired_mock.call_args[0][0] == 'STARTED')


    self.assertResourceCalled('Execute', "ambari-sudo.sh su flume -l -s /bin/bash -c 'export  PATH=/bin JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/bin/flume-ng agent --name a1 --conf /etc/flume/conf/a1 --conf-file /etc/flume/conf/a1/flume.conf -Dflume.monitoring.type=ganglia -Dflume.monitoring.hosts=c6401.ambari.apache.org:8655 > /var/log/flume/a1.out 2>&1' &",
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        wait_for_finish = False,
    )
    self.assertResourceCalled('Execute', 'pgrep -o -u flume -f ^/usr/jdk64/jdk1.7.0_45.*a1.* > /var/run/flume/a1.pid',
      logoutput = True,
      tries = 20,
      try_sleep = 10)

    self.assertNoMoreResources()

  @patch("glob.glob")
  @patch("flume._set_desired_state")
  def test_stop_default(self, set_desired_mock, glob_mock):
    glob_mock.side_effect = [['/var/run/flume/a1/pid'], ['/etc/flume/conf/a1/ambari-meta.json']]

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "stop",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertTrue(glob_mock.called)

    self.assertTrue(set_desired_mock.called)
    self.assertTrue(set_desired_mock.call_args[0][0] == 'INSTALLED')

    self.assertResourceCalled('Execute', 'kill `cat /var/run/flume/a1.pid` > /dev/null 2>&1',
      ignore_failures = True)

    self.assertResourceCalled('File', '/var/run/flume/a1.pid', action = ['delete'])

    self.assertNoMoreResources()

  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("sys.exit")
  def test_status_default(self, sys_exit_mock, structured_out_mock):
    
    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "status",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    except:
      # expected since ComponentIsNotRunning gets raised
      pass
    
    # test that the method was called with empty processes
    self.assertTrue(structured_out_mock.called)
    structured_out_mock.assert_called_with({'processes': []})
    self.assertNoMoreResources()

  def test_struct_out(self):
   from resource_management.libraries.script import Script

   configs_path = os.path.join(RMFTestCase._getSrcFolder(),
     "test/python/stacks", self.STACK_VERSION, "configs")

   script = Script()
   script.stroutfile = os.path.join(configs_path, "structured-out-status.json")
   script.load_structured_out()

   self.assertFalse("version" in script.structuredOut)
    

  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("glob.glob")
  @patch("sys.exit")
  def test_status_with_result(self, sys_exit_mock, glob_mock, structured_out_mock):
    glob_mock.return_value = ['/etc/flume/conf/a1/ambari-meta.json']

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "status",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)
    except:
      # expected since ComponentIsNotRunning gets raised
      pass
    
    self.assertTrue(structured_out_mock.called)

    # call_args[0] is a tuple, whose first element is the actual call argument
    struct_out = structured_out_mock.call_args[0][0]
    self.assertTrue(struct_out.has_key('processes'))

    self.assertNoMoreResources()
    
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("glob.glob")
  @patch("sys.exit")
  def test_status_no_agents(self, sys_exit_mock, glob_mock, structured_out_mock):
    glob_mock.return_value = []

    try:
      self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
       classname = "FlumeHandler",
       command = "status",
       config_file="default.json",
       hdp_stack_version = self.STACK_VERSION,
       target = RMFTestCase.TARGET_COMMON_SERVICES)
    except:
      # expected since ComponentIsNotRunning gets raised
      pass
      
    self.assertTrue(structured_out_mock.called)

    # call_args[0] is a tuple, whose first element is the actual call argument
    struct_out = structured_out_mock.call_args[0][0]
    self.assertTrue(struct_out.has_key('processes'))
    self.assertNoMoreResources()    

  def assert_configure_default(self):

    self.assertResourceCalled('Directory', '/etc/flume/conf', recursive=True)

    self.assertResourceCalled('Directory', '/var/log/flume', owner = 'flume')

    self.assertResourceCalled('Directory', '/etc/flume/conf/a1')

    self.assertResourceCalled('PropertiesFile', '/etc/flume/conf/a1/flume.conf',
      mode = 0644,
      properties = build_flume(
        self.getConfig()['configurations']['flume-conf']['content'])['a1'])

    self.assertResourceCalled('File',
      '/etc/flume/conf/a1/log4j.properties',
      content = Template('log4j.properties.j2', agent_name = 'a1'),
      mode = 0644)

    self.assertResourceCalled('File',
      '/etc/flume/conf/a1/ambari-meta.json',
      content='{"channels_count": 1, "sinks_count": 1, "sources_count": 1}',
      mode = 0644)

    self.assertResourceCalled('File', "/etc/flume/conf/a1/flume-env.sh",
                              owner="flume",
                              content=InlineTemplate(self.getConfig()['configurations']['flume-env']['content'])
    )

  def assert_configure_many(self):

    self.assertResourceCalled('Directory', '/etc/flume/conf', recursive=True)

    self.assertResourceCalled('Directory', '/var/log/flume', owner = 'flume')

    top = build_flume(self.getConfig()['configurations']['flume-conf']['content'])

    # a1
    self.assertResourceCalled('Directory', '/etc/flume/conf/a1')
    self.assertResourceCalled('PropertiesFile', '/etc/flume/conf/a1/flume.conf',
      mode = 0644,
      properties = top['a1'])
    self.assertResourceCalled('File',
      '/etc/flume/conf/a1/log4j.properties',
      content = Template('log4j.properties.j2', agent_name = 'a1'),
      mode = 0644)
    self.assertResourceCalled('File',
      '/etc/flume/conf/a1/ambari-meta.json',
      content='{"channels_count": 1, "sinks_count": 1, "sources_count": 1}',
      mode = 0644)

    self.assertResourceCalled('File', "/etc/flume/conf/a1/flume-env.sh",
                              owner="flume",
                              content=InlineTemplate(self.getConfig()['configurations']['flume-env']['content'])
    )

    # b1
    self.assertResourceCalled('Directory', '/etc/flume/conf/b1')
    self.assertResourceCalled('PropertiesFile', '/etc/flume/conf/b1/flume.conf',
      mode = 0644,
      properties = top['b1'])
    self.assertResourceCalled('File',
      '/etc/flume/conf/b1/log4j.properties',
      content = Template('log4j.properties.j2', agent_name = 'b1'),
      mode = 0644)
    self.assertResourceCalled('File',
      '/etc/flume/conf/b1/ambari-meta.json',
      content='{"channels_count": 1, "sinks_count": 1, "sources_count": 1}',
      mode = 0644)
    self.assertResourceCalled('File', "/etc/flume/conf/b1/flume-env.sh",
                              owner="flume",
                              content=InlineTemplate(self.getConfig()['configurations']['flume-env']['content'])
    )


  @patch("os.path.isfile")
  def test_start_single(self, os_path_isfile_mock):
    # 1st call is to check if the conf file is there - that should be True
    # 2nd call is to check if the process is live - that should be False
    os_path_isfile_mock.side_effect = [True, False]

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "start",
                       config_file="flume_target.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assert_configure_many()

    self.assertResourceCalled('Execute', format('su -s /bin/bash flume -c "export JAVA_HOME=/usr/jdk64/jdk1.7.0_45; /usr/bin/flume-ng agent '
      '--name b1 '
      '--conf /etc/flume/conf/b1 '
      '--conf-file /etc/flume/conf/b1/flume.conf '
      '-Dflume.monitoring.type=ganglia '
      '-Dflume.monitoring.hosts=c6401.ambari.apache.org:8655"'),
      wait_for_finish = False)

    self.assertResourceCalled('Execute', 'pgrep -o -u flume -f ^/usr/jdk64/jdk1.7.0_45.*b1.* > /var/run/flume/b1.pid',
      logoutput = True,
      tries = 10,
      try_sleep = 6)

    self.assertNoMoreResources()

  @patch("os.path.isfile")
  def test_start_single(self, os_path_isfile_mock):
    # 1st call is to check if the conf file is there - that should be True
    # 2nd call is to check if the process is live - that should be False
    os_path_isfile_mock.side_effect = [True, False]

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "start",
                       config_file="flume_target.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assert_configure_many()


    self.assertResourceCalled('Execute', "ambari-sudo.sh su flume -l -s /bin/bash -c 'export  PATH=/bin JAVA_HOME=/usr/jdk64/jdk1.7.0_45 ; /usr/bin/flume-ng agent --name b1 --conf /etc/flume/conf/b1 --conf-file /etc/flume/conf/b1/flume.conf -Dflume.monitoring.type=ganglia -Dflume.monitoring.hosts=c6401.ambari.apache.org:8655 > /var/log/flume/b1.out 2>&1' &",
        environment = {'JAVA_HOME': u'/usr/jdk64/jdk1.7.0_45'},
        wait_for_finish = False,
    )

    self.assertResourceCalled('Execute', 'pgrep -o -u flume -f ^/usr/jdk64/jdk1.7.0_45.*b1.* > /var/run/flume/b1.pid',
      logoutput = True,
      tries = 20,
      try_sleep = 10)

    self.assertNoMoreResources()

  @patch("glob.glob")
  def test_stop_single(self, glob_mock):
    glob_mock.return_value = ['/var/run/flume/b1.pid']

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "stop",
                       config_file="flume_target.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertTrue(glob_mock.called)

    self.assertResourceCalled('Execute', 'kill `cat /var/run/flume/b1.pid` > /dev/null 2>&1',
      ignore_failures = True)

    self.assertResourceCalled('File', '/var/run/flume/b1.pid', action = ['delete'])

    self.assertNoMoreResources()

  @patch("flume.find_expected_agent_names")
  @patch("os.unlink")
  def test_configure_with_existing(self, os_unlink_mock, expected_names_mock):
    expected_names_mock.return_value = ["x1"]

    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertTrue(os_unlink_mock.called)
    os_unlink_mock.assert_called_with('/etc/flume/conf/x1/ambari-meta.json')

    self.assert_configure_default()
    self.assertNoMoreResources()


  def test_flume_env_not_22(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "configure",
                       config_file="default.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Directory', '/etc/flume/conf', recursive=True)

    self.assertResourceCalled('Directory', '/var/log/flume', owner = 'flume')

    self.assertResourceCalled('Directory', '/etc/flume/conf/a1')

    self.assertResourceCalled('PropertiesFile', '/etc/flume/conf/a1/flume.conf',
      mode = 0644,
      properties = build_flume(
        self.getConfig()['configurations']['flume-conf']['content'])['a1'])

    self.assertResourceCalled('File',
      '/etc/flume/conf/a1/log4j.properties',
      content = Template('log4j.properties.j2', agent_name = 'a1'),
      mode = 0644)

    self.assertResourceCalled('File',
      '/etc/flume/conf/a1/ambari-meta.json',
      content='{"channels_count": 1, "sinks_count": 1, "sources_count": 1}',
      mode = 0644)

    content = InlineTemplate(self.getConfig()['configurations']['flume-env']['content'])

    self.assertTrue(content.get_content().find('/usr/lib/hive') > -1)

    self.assertResourceCalled('File', "/etc/flume/conf/a1/flume-env.sh",
                              owner="flume",
                              content=content)

  def test_flume_env_with_22(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "configure",
                       config_file="flume_22.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled('Directory', '/etc/flume/conf', recursive=True)

    self.assertResourceCalled('Directory', '/var/log/flume', owner = 'flume')

    self.assertResourceCalled('Directory', '/etc/flume/conf/a1')

    self.assertResourceCalled('PropertiesFile', '/etc/flume/conf/a1/flume.conf',
      mode = 0644,
      properties = build_flume(
        self.getConfig()['configurations']['flume-conf']['content'])['a1'])

    self.assertResourceCalled('File',
      '/etc/flume/conf/a1/log4j.properties',
      content = Template('log4j.properties.j2', agent_name = 'a1'),
      mode = 0644)

    self.assertResourceCalled('File',
      '/etc/flume/conf/a1/ambari-meta.json',
      content='{"channels_count": 1, "sinks_count": 1, "sources_count": 1}',
      mode = 0644)

    content = InlineTemplate(self.getConfig()['configurations']['flume-env']['content'])

    self.assertTrue(content.get_content().find('/usr/hdp/current/hive-metastore') > -1)

    self.assertResourceCalled('File', "/etc/flume/conf/a1/flume-env.sh",
                              owner="flume",
                              content=content)

  def test_pre_rolling_restart(self):
    self.executeScript(self.COMMON_SERVICES_PACKAGE_DIR + "/scripts/flume_handler.py",
                       classname = "FlumeHandler",
                       command = "pre_rolling_restart",
                       config_file="flume_22.json",
                       hdp_stack_version = self.STACK_VERSION,
                       target = RMFTestCase.TARGET_COMMON_SERVICES)

    self.assertResourceCalled("Execute", "hdp-select set flume-server 2.2.1.0-2067")


def build_flume(content):
  result = {}
  agent_names = []

  for line in content.split('\n'):
    rline = line.strip()
    if 0 != len(rline) and not rline.startswith('#'):
      pair = rline.split('=')
      lhs = pair[0].strip()
      rhs = pair[1].strip()

      part0 = lhs.split('.')[0]

      if lhs.endswith(".sources"):
        agent_names.append(part0)

      if not result.has_key(part0):
        result[part0] = {}

      result[part0][lhs] = rhs

  # trim out non-agents
  for k in result.keys():
    if not k in agent_names:
      del result[k]


  return result
