# !/usr/bin/env python

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
import socket
from resource_management import Script,ConfigDictionary
from mock.mock import patch
from mock.mock import MagicMock
from unittest import TestCase

check_host = __import__('check_host')
from check_host import CheckHost

class TestCheckHost(TestCase):

  @patch("os.path.isfile")
  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def testJavaHomeAvailableCheck(self, structured_out_mock, get_tmp_dir_mock, mock_config, os_isfile_mock):
    # test, java home exists
    os_isfile_mock.return_value = True
    get_tmp_dir_mock.return_value = "/tmp"
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "java_home_check",
                                                   "java_home" : "test_java_home"}}

    checkHost = CheckHost()
    checkHost.actionexecute(None)

    self.assertEquals(os_isfile_mock.call_args[0][0], 'test_java_home/bin/java')
    self.assertEquals(structured_out_mock.call_args[0][0], {'java_home_check': {'message': 'Java home exists!',
                                                                                'exit_code': 0}})
    # test, java home doesn't exist
    os_isfile_mock.reset_mock()
    os_isfile_mock.return_value = False

    checkHost.actionexecute(None)

    self.assertEquals(os_isfile_mock.call_args[0][0], 'test_java_home/bin/java')
    self.assertEquals(structured_out_mock.call_args[0][0], {'java_home_check': {"message": "Java home doesn't exist!",
                                                                                "exit_code" : 1}})


  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch("check_host.Execute")
  @patch("resource_management.libraries.script.Script.put_structured_out")
  @patch("subprocess.Popen")
  @patch("check_host.format")
  @patch("os.path.isfile")
  def testDBConnectionCheck(self, isfile_mock, format_mock, popenMock, structured_out_mock, execute_mock, get_tmp_dir_mock, mock_config):
    # test, download DBConnectionVerification.jar failed
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "db_connection_check",
                                                   "java_home" : "test_java_home",
                                                   "ambari_server_host" : "test_host",
                                                   "jdk_location" : "test_jdk_location",
                                                   "db_name" : "mysql",
                                                   "db_connection_url" : "test_db_connection_url",
                                                   "user_name" : "test_user_name",
                                                   "user_passwd" : "test_user_passwd",
                                                   "jdk_name" : "test_jdk_name"}}
    get_tmp_dir_mock.return_value = "/tmp"
    execute_mock.side_effect = Exception("test exception")
    isfile_mock.return_value = True
    checkHost = CheckHost()
    checkHost.actionexecute(None)

    self.assertEquals(structured_out_mock.call_args[0][0], {'db_connection_check': {'message': 'Error downloading ' \
                     'DBConnectionVerification.jar from Ambari Server resources. Check network access to Ambari ' \
                     'Server.\ntest exception', 'exit_code': 1}})
    
    self.assertEquals(format_mock.call_args_list[2][0][0], "/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf " \
                      "--retry 5 {jdk_location}{check_db_connection_jar_name} -o {check_db_connection_jar_name}'")
    
    self.assertEquals(format_mock.call_args_list[3][0][0], "[ -f /usr/lib/ambari-agent/{check_db_connection_jar_name}]")

    # test, download jdbc driver failed
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "db_connection_check",
                                                   "java_home" : "test_java_home",
                                                   "ambari_server_host" : "test_host",
                                                   "jdk_location" : "test_jdk_location",
                                                   "db_name" : "oracle",
                                                   "db_connection_url" : "test_db_connection_url",
                                                   "user_name" : "test_user_name",
                                                   "user_passwd" : "test_user_passwd",
                                                   "jdk_name" : "test_jdk_name"}}
    format_mock.reset_mock()
    execute_mock.reset_mock()
    p = MagicMock()
    execute_mock.side_effect = [p, Exception("test exception")]

    checkHost.actionexecute(None)

    self.assertEquals(format_mock.call_args[0][0], 'Error: Ambari Server cannot download the database JDBC driver '
                  'and is unable to test the database connection. You must run ambari-server setup '
                  '--jdbc-db={db_name} --jdbc-driver=/path/to/your/{db_name}/driver.jar on the Ambari '
                  'Server host to make the JDBC driver available for download and to enable testing '
                  'the database connection.\n')
    self.assertEquals(structured_out_mock.call_args[0][0]['db_connection_check']['exit_code'], 1)
    self.assertEquals(format_mock.call_args_list[4][0][0], "/bin/sh -c 'cd /usr/lib/ambari-agent/ && curl -kf " \
                                                            "--retry 5 {jdbc_url} -o {jdbc_name}'")
    
    self.assertEquals(format_mock.call_args_list[5][0][0], "[ -f /usr/lib/ambari-agent/{jdbc_name}]")

    # test, no connection to remote db
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "db_connection_check",
                                                   "java_home" : "test_java_home",
                                                   "ambari_server_host" : "test_host",
                                                   "jdk_location" : "test_jdk_location",
                                                   "db_name" : "postgresql",
                                                   "db_connection_url" : "test_db_connection_url",
                                                   "user_name" : "test_user_name",
                                                   "user_passwd" : "test_user_passwd",
                                                   "jdk_name" : "test_jdk_name"}}
    format_mock.reset_mock()
    execute_mock.reset_mock()
    execute_mock.side_effect = [p, p]
    s = MagicMock()
    s.communicate.return_value = ("test message", "")
    s.returncode = 1
    popenMock.return_value = s

    checkHost.actionexecute(None)

    self.assertEquals(structured_out_mock.call_args[0][0], {'db_connection_check': {'message': 'test message',
                                                                                    'exit_code': 1}})
    self.assertEquals(format_mock.call_args[0][0],'{java64_home}/bin/java -cp /usr/lib/ambari-agent/{check_db_' \
                                                'connection_jar_name}:/usr/lib/ambari-agent/{jdbc_name} org.' \
                                                'apache.ambari.server.DBConnectionVerification \'{db_connection_url}\' ' \
                                                '{user_name} {user_passwd!p} {jdbc_driver}')

    # test, db connection success
    execute_mock.reset_mock()
    execute_mock.side_effect = [p, p]
    s.returncode = 0

    checkHost.actionexecute(None)

    self.assertEquals(structured_out_mock.call_args[0][0], {'db_connection_check':
                                        {'message': 'DB connection check completed successfully!', 'exit_code': 0}})

    #test jdk_name and java home are not available
    mock_config.return_value = {"commandParams" : {"check_execute_list" : "db_connection_check",
                                                   "java_home" : "test_java_home",
                                                   "ambari_server_host" : "test_host",
                                                   "jdk_location" : "test_jdk_location",
                                                   "db_connection_url" : "test_db_connection_url",
                                                   "user_name" : "test_user_name",
                                                   "user_passwd" : "test_user_passwd",
                                                   "db_name" : "postgresql"}}

    isfile_mock.return_value = False
    checkHost.actionexecute(None)
    self.assertEquals(structured_out_mock.call_args[0][0], {'db_connection_check': {'message': 'Custom java is not ' \
            'available on host. Please install it. Java home should be the same as on server. \n', 'exit_code': 1}})



  @patch("socket.gethostbyname")
  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def testHostResolution(self, structured_out_mock, get_tmp_dir_mock, mock_config, mock_socket):
    mock_socket.return_value = "192.168.1.1"    
    jsonFilePath = os.path.join("../resources/custom_actions", "check_host_ip_addresses.json")
    
    with open(jsonFilePath, "r") as jsonFile:
      jsonPayload = json.load(jsonFile)
 
    mock_config.return_value = ConfigDictionary(jsonPayload)
    get_tmp_dir_mock.return_value = "/tmp"

    checkHost = CheckHost()
    checkHost.actionexecute(None)
    
    # ensure the correct function was called
    self.assertTrue(structured_out_mock.called)
    structured_out_mock.assert_called_with({'host_resolution_check': 
      {'failures': [], 
       'message': 'All hosts resolved to an IP address.', 
       'failed_count': 0, 
       'success_count': 5, 
       'exit_code': 0}})
    
    # try it now with errors
    mock_socket.side_effect = socket.error
    checkHost.actionexecute(None)
    
    structured_out_mock.assert_called_with({'host_resolution_check': 
      {'failures': [
                    {'cause': (), 'host': u'c6401.ambari.apache.org', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'c6402.ambari.apache.org', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'c6403.ambari.apache.org', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'foobar', 'type': 'FORWARD_LOOKUP'}, 
                    {'cause': (), 'host': u'!!!', 'type': 'FORWARD_LOOKUP'}], 
       'message': 'There were 5 host(s) that could not resolve to an IP address.', 
       'failed_count': 5, 'success_count': 0, 'exit_code': 0}})
    
  @patch.object(Script, 'get_config')
  @patch.object(Script, 'get_tmp_dir')
  @patch("resource_management.libraries.script.Script.put_structured_out")
  def testInvalidCheck(self, structured_out_mock, get_tmp_dir_mock, mock_config):
    jsonFilePath = os.path.join("../resources/custom_actions", "invalid_check.json")
    
    with open(jsonFilePath, "r") as jsonFile:
      jsonPayload = json.load(jsonFile)
 
    mock_config.return_value = ConfigDictionary(jsonPayload)
    get_tmp_dir_mock.return_value = "tmp"

    checkHost = CheckHost()
    checkHost.actionexecute(None)
    
    # ensure the correct function was called
    self.assertTrue(structured_out_mock.called)
    structured_out_mock.assert_called_with({})
