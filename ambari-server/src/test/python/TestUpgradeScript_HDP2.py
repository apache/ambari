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
import subprocess
from mock.mock import MagicMock, call, patch
from unittest import TestCase
from mock.mock import create_autospec
import sys
import unittest
import UpgradeHelper_HDP2
import StringIO
import logging


class TestUpgradeHDP2Script(TestCase):
  def setUp(self):
    UpgradeHelper_HDP2.logger = MagicMock()
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    sys.stdout = sys.__stdout__


  @patch.object(UpgradeHelper_HDP2, 'backup_single_config_type')
  def test_backup_configs(self, backup_config_mock):
    UpgradeHelper_HDP2.backup_configs(None)
    self.assertTrue(backup_config_mock.called)

  @patch.object(UpgradeHelper_HDP2, 'update_config')
  @patch.object(UpgradeHelper_HDP2, 'get_config')
  @patch('optparse.Values')
  def test_update_with_append(self, optparse_mock, get_config_mock, update_config_mock):
    opm = optparse_mock.return_value
    update_config_mock.return_value = None
    options = MagicMock()
    args = ["save-configs"]
    opm.parse_args.return_value = (options, args)
    get_config_mock.return_value = {"a1": "va1", "a2": "va2", "b1": "vb1", "b2": "vb2", "c1": "vc1", "d1": "d1"}
    site_template = {"y1": "vy1", "a1": "REPLACE_WITH_", "a2": "REPLACE_WITH_", "nb1": "REPLACE_WITH_b1",
                     "nb2": "REPLACE_WITH_b2", "d1": "DELETE_OLD", "b1" : "DELETE_OLD","c1": "vc2"}
    expected_site = {"y1": "vy1", "a1": "va1", "a2": "va2", "nb1": "vb1", "nb2": "vb2", "c1": "vc2"}
    UpgradeHelper_HDP2.update_config_using_existing(opm, "global", site_template)
    get_config_mock.assert_called_once_with(opm, "global")
    update_config_mock.assert_called_once_with(opm, expected_site, "global")
    pass

  @patch.object(UpgradeHelper_HDP2, 'update_config')
  @patch.object(UpgradeHelper_HDP2, 'get_config')
  @patch('optparse.Values')
  def test_update_with_appen_II(self, optparse_mock, get_config_mock, update_config_mock):
    opm = optparse_mock.return_value
    update_config_mock.return_value = None
    options = MagicMock()
    args = ["save-configs"]
    opm.parse_args.return_value = (options, args)
    get_config_mock.return_value = {"a1": "va1", "a2": "va2", "b1": "vb1", "b2": "vb2", "c1": "vc1", "x1": "x1",
                                    "X1": "X1"}
    site_template = {"y1": "vy1", "a1": "REPLACE_WITH_", "a2": "REPLACE_WITH_", "nb1": "REPLACE_WITH_b1",
                     "nb2": "REPLACE_WITH_b2", "x1": "DELETE_OLD", "X1": "DELETE"}
    expected_site = {"y1": "vy1", "a1": "va1", "a2": "va2", "nb1": "vb1", "nb2": "vb2", "c1": "vc1","X1": "DELETE"}
    UpgradeHelper_HDP2.update_config_using_existing(opm, "global", site_template)
    get_config_mock.assert_called_once_with(opm, "global")
    update_config_mock.assert_called_once_with(opm, expected_site, "global")
    pass

  @patch.object(logging.FileHandler, 'setFormatter')
  @patch.object(logging, 'basicConfig')
  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, 'write_config')
  @patch.object(UpgradeHelper_HDP2, 'get_config_resp')
  @patch("os.remove")
  @patch("shutil.copyfile")
  @patch("os.path.exists")
  @patch('optparse.OptionParser')
  def test_save_configs(self, option_parser_mock, path_exists_mock, shutil_copy_mock, os_remove_mock,
                        get_config_resp_mock, write_config_mock, file_handler_mock, logging_mock, set_formatter_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    opm = option_parser_mock.return_value
    path_exists_mock.return_value = True
    shutil_copy_mock = MagicMock()
    options = self.get_mock_options()
    args = ["backup-configs"]
    opm.parse_args.return_value = (options, args)

    def get_config_resp_side_effect(ops, type, error_if_na):
      if type == "global":
        return ("version1", "")
      elif type == "core-site":
        return ("version1", "")
      elif type == "hdfs-site":
        return ("version1", "")
      elif type == "mapred-site":
        return ("version2", "")
      elif type == "hbase-site":
        return ("version2", "")
      else:
        return (None, None)

    get_config_resp_mock.side_effect = get_config_resp_side_effect
    UpgradeHelper_HDP2.main()
    shutil_copy_mock.assert_called_once()
    os_remove_mock.assert_called_once_with("logfile")
    write_expected = [call("", "global", "version1"), call("", "core-site", "version1"),
                      call("", "hdfs-site", "version1"), call("", "mapred-site", "version2"),
                      call("", "hbase-site", "version2")]
    write_config_mock.assert_has_calls(write_expected, any_order=True)
    get_config_expected = [call(options, "global", True), call(options, "core-site", True),
                           call(options, "hdfs-site", True), call(options, "mapred-site", True),
                           call(options, "hbase-site", False), call(options, "oozie-site", False),
                           call(options, "webhcat-site", False), call(options, "hive-site", False)]
    get_config_resp_mock.assert_has_calls(get_config_expected, any_order=True)
    pass


  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, "backup_file")
  @patch.object(UpgradeHelper_HDP2, 'write_mapping')
  @patch("json.loads")
  @patch.object(UpgradeHelper_HDP2, 'curl')
  @patch('optparse.OptionParser')
  def test_save_mr_mapping(self, option_parser_mock, curl_mock, json_loads_mock, write_mapping_mock,
                           backup_file_mock, file_handler_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    opm = option_parser_mock.return_value
    options = self.get_mock_options()
    args = ["save-mr-mapping"]
    opm.parse_args.return_value = (options, args)
    curl_mock.side_effect = ['"href" : "', '"href" : "', '"href" : "', '"href" : "']
    json_loads_mock.return_value = {"host_components": [{"HostRoles": {"host_name": "host1"}}]}
    UpgradeHelper_HDP2.main()
    expected_curl_calls = [
      call(False, "-u", "admin:admin",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE/components/MAPREDUCE_CLIENT"),
      call(False, "-u", "admin:admin",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE/components/TASKTRACKER"),
      call(False, "-u", "admin:admin",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE/components/JOBTRACKER")]
    curl_mock.assert_has_calls(expected_curl_calls, any_order=True)
    self.assertTrue(write_mapping_mock.called)
    write_call = write_mapping_mock.call_args
    args, kargs = write_call
    self.assertTrue('MAPREDUCE_CLIENT' in args[0].keys())
    self.assertTrue(["host1"] == args[0]['MAPREDUCE_CLIENT'])
    self.assertTrue('TASKTRACKER' in args[0].keys())
    self.assertTrue('TASKTRACKER' in args[0].keys())
    pass


  @patch.object(UpgradeHelper_HDP2, "get_YN_input")
  @patch.object(UpgradeHelper_HDP2, "read_mapping")
  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, "backup_file")
  @patch.object(UpgradeHelper_HDP2, 'curl')
  @patch('optparse.OptionParser')
  def test_delete_mr(self, option_parser_mock, curl_mock,
                     backup_file_mock, file_handler_mock, read_mapping_mock, get_yn_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    opm = option_parser_mock.return_value
    options = self.get_mock_options()
    args = ["delete-mr"]
    opm.parse_args.return_value = (options, args)
    curl_mock.return_value = ''
    get_yn_mock.return_value = True
    read_mapping_mock.return_value = {
      "TASKTRACKER": ["c6401", "c6402"],
      "JOBTRACKER": ["c6401"],
      "MAPREDUCE_CLIENT": ["c6401"]}
    UpgradeHelper_HDP2.main()
    expected_curl_calls = [
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "PUT", "-d", """{"HostRoles": {"state": "DISABLED"}}""",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6401/host_components/TASKTRACKER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "PUT", "-d", """{"HostRoles": {"state": "DISABLED"}}""",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6402/host_components/TASKTRACKER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "PUT", "-d", """{"HostRoles": {"state": "DISABLED"}}""",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6401/host_components/JOBTRACKER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "DELETE",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE")]
    curl_mock.assert_has_calls(expected_curl_calls, any_order=True)
    pass


  @patch.object(UpgradeHelper_HDP2, "has_component_in_stack_def")
  @patch.object(UpgradeHelper_HDP2, "get_cluster_stackname")
  @patch.object(UpgradeHelper_HDP2, "read_mapping")
  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, "backup_file")
  @patch.object(UpgradeHelper_HDP2, 'curl')
  @patch('optparse.OptionParser')
  def test_add_yarn_mr_with_ATS(self, option_parser_mock, curl_mock,
                       backup_file_mock, file_handler_mock, read_mapping_mock, get_stack_mock,  has_comp_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    opm = option_parser_mock.return_value
    options = self.get_mock_options()
    args = ["add-yarn-mr2"]
    opm.parse_args.return_value = (options, args)
    curl_mock.return_value = ''
    has_comp_mock.return_value = True
    read_mapping_mock.return_value = {
      "TASKTRACKER": ["c6401", "c6402"],
      "JOBTRACKER": ["c6401"],
      "HISTORYSERVER": ["c6401"],
      "MAPREDUCE_CLIENT": ["c6403"]}
    UpgradeHelper_HDP2.main()
    expected_curl_calls = [
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/YARN"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE2"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE2/components/HISTORYSERVER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE2/components/MAPREDUCE2_CLIENT"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/YARN/components/NODEMANAGER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/YARN/components/YARN_CLIENT"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/YARN/components/RESOURCEMANAGER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/YARN/components/APP_TIMELINE_SERVER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6401/host_components/NODEMANAGER"),
      call(False, '-u', 'admin:admin', '-H', 'X-Requested-By: ambari', '-X', 'POST',
           'http://localhost:8080/api/v1/clusters/c1/hosts/c6401/host_components/HISTORYSERVER'),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6402/host_components/NODEMANAGER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6403/host_components/YARN_CLIENT"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6403/host_components/MAPREDUCE2_CLIENT"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6401/host_components/RESOURCEMANAGER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6401/host_components/APP_TIMELINE_SERVER")]
    curl_mock.assert_has_calls(expected_curl_calls, any_order=True)
    pass
  
  @patch.object(UpgradeHelper_HDP2, "has_component_in_stack_def")
  @patch.object(UpgradeHelper_HDP2, "get_cluster_stackname")
  @patch.object(UpgradeHelper_HDP2, "read_mapping")
  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, "backup_file")
  @patch.object(UpgradeHelper_HDP2, 'curl')
  @patch('optparse.OptionParser')
  def test_add_yarn_mr_without_ATS(self, option_parser_mock, curl_mock,
                       backup_file_mock, file_handler_mock, read_mapping_mock, get_stack_mock,  has_comp_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    opm = option_parser_mock.return_value
    options = self.get_mock_options()
    args = ["add-yarn-mr2"]
    opm.parse_args.return_value = (options, args)
    curl_mock.return_value = ''
    has_comp_mock.return_value = False
    read_mapping_mock.return_value = {
      "TASKTRACKER": ["c6401", "c6402"],
      "JOBTRACKER": ["c6401"],
      "HISTORYSERVER": ["c6401"],
      "MAPREDUCE_CLIENT": ["c6403"]}
    UpgradeHelper_HDP2.main()
    expected_curl_calls = [
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/YARN"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE2"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE2/components/HISTORYSERVER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE2/components/MAPREDUCE2_CLIENT"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/YARN/components/NODEMANAGER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/YARN/components/YARN_CLIENT"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/services/YARN/components/RESOURCEMANAGER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6401/host_components/NODEMANAGER"),
      call(False, '-u', 'admin:admin', '-H', 'X-Requested-By: ambari', '-X', 'POST',
           'http://localhost:8080/api/v1/clusters/c1/hosts/c6401/host_components/HISTORYSERVER'),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6402/host_components/NODEMANAGER"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6403/host_components/YARN_CLIENT"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6403/host_components/MAPREDUCE2_CLIENT"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
           "http://localhost:8080/api/v1/clusters/c1/hosts/c6401/host_components/RESOURCEMANAGER"),
      ]
    curl_mock.assert_has_calls(expected_curl_calls, any_order=True)
    
    # assert no ATS was added
    self.assert_(not call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
        "http://localhost:8080/api/v1/clusters/c1/hosts/c6401/host_components/APP_TIMELINE_SERVER") in curl_mock.call_args_list, "ATS should not be added if it's not present!")     
    self.assert_(not call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "POST",
        "http://localhost:8080/api/v1/clusters/c1/services/YARN/components/APP_TIMELINE_SERVER") in curl_mock.call_args_list, "ATS should not be added if it's not present!")  
    pass


  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, "backup_file")
  @patch.object(UpgradeHelper_HDP2, 'curl')
  @patch('optparse.OptionParser')
  def test_install_yarn_mr2(self, option_parser_mock, curl_mock,
                            backup_file_mock, file_handler_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    opm = option_parser_mock.return_value
    options = self.get_mock_options()
    args = ["install-yarn-mr2"]
    opm.parse_args.return_value = (options, args)
    curl_mock.return_value = '"href" : "'
    UpgradeHelper_HDP2.main()
    expected_curl_calls = [
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "PUT", "-d",
           """{"RequestInfo":{"context":"Install MapReduce2"}, "Body":{"ServiceInfo": {"state":"INSTALLED"}}}""",
           "http://localhost:8080/api/v1/clusters/c1/services/MAPREDUCE2"),
      call(False, "-u", "admin:admin", '-H', 'X-Requested-By: ambari', "-X", "PUT", "-d",
           """{"RequestInfo":{"context":"Install YARN"}, "Body":{"ServiceInfo": {"state":"INSTALLED"}}}""",
           "http://localhost:8080/api/v1/clusters/c1/services/YARN")]
    curl_mock.assert_has_calls(expected_curl_calls, any_order=False)
    pass


  @patch.object(UpgradeHelper_HDP2, "get_config")
  @patch.object(UpgradeHelper_HDP2, "rename_all_properties")
  @patch.object(UpgradeHelper_HDP2, "update_config_using_existing_properties")
  @patch.object(UpgradeHelper_HDP2, "read_mapping")
  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, "backup_file")
  @patch.object(UpgradeHelper_HDP2, 'curl')
  @patch('optparse.OptionParser')
  def test_update_single_configs(self, option_parser_mock, curl_mock,
                                 backup_file_mock, file_handler_mock, read_mapping_mock,
                                 update_config_mock, rename_all_prop_mock, get_config_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    opm = option_parser_mock.return_value
    options = MagicMock()
    args = ["update-configs", "hdfs-site"]
    opm.parse_args.return_value = (options, args)
    curl_mock.side_effect = ['', '', '', '', '', '', '']
    read_mapping_mock.return_value = {"JOBTRACKER": ["c6401"]}
    update_config_mock.side_effect = [None]
    get_config_mock.return_value = {}
    prop_to_move = {"dfs.namenode.checkpoint.edits.dir": "a1",
                    "dfs.namenode.checkpoint.dir": "a2",
                    "dfs.namenode.checkpoint.period": "a3"}
    rename_all_prop_mock.side_effect = [
      prop_to_move,
      {}, {}]
    UpgradeHelper_HDP2.main()
    self.assertTrue(update_config_mock.call_count == 1)
    args, kargs = update_config_mock.call_args_list[0]
    self.assertEqual("hdfs-site", args[1])
    for key in prop_to_move.keys():
      self.assertEqual(prop_to_move[key], args[3][key])
    pass


  @patch.object(UpgradeHelper_HDP2, "get_config_resp")
  @patch.object(UpgradeHelper_HDP2, "get_config")
  @patch.object(UpgradeHelper_HDP2, "read_mapping")
  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, "backup_file")
  @patch.object(UpgradeHelper_HDP2, 'curl')
  @patch('optparse.OptionParser')
  def test_no_hbase(self, option_parser_mock, curl_mock,
                          backup_file_mock, file_handler_mock, read_mapping_mock,
                          get_config_mock, get_config_resp_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    opm = option_parser_mock.return_value
    options = MagicMock()
    args = ["update-configs"]
    opm.parse_args.return_value = (options, args)
    options.logfile = "logfile"
    options.user = "admin"
    options.password = "admin"
    options.hostname = "localhost"
    options.clustername = "c1"
    curl_mock.side_effect = ['', '', '', '', '', '', '']
    read_mapping_mock.return_value = {
      "TASKTRACKER": ["c6401", "c6402"],
      "JOBTRACKER": ["c6401"],
      "MAPREDUCE_CLIENT": ["c6403"]}
    get_config_resp_mock.return_value = "hbase-site", None
    get_config_mock.return_value = {
      "mapred.hosts": "an_old_value",
      "mapred.hosts.exclude": "an_old_value",
      "mapred.jobtracker.maxtasks.per.job": "an_old_value",
      "mapred.jobtracker.taskScheduler": "an_old_value",
      "dfs.df.interval": "an_old_value",
      "mapred.userlog.retain.hours": "will_not_be_stored",
      "global1": "global11"
    }
    UpgradeHelper_HDP2.main()
    self.assertEqual(6, len(curl_mock.call_args_list))


  @patch.object(UpgradeHelper_HDP2, "get_config_resp")
  @patch.object(UpgradeHelper_HDP2, "get_config")
  @patch.object(UpgradeHelper_HDP2, "read_mapping")
  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, "backup_file")
  @patch.object(UpgradeHelper_HDP2, 'curl')
  @patch('optparse.OptionParser')
  def test_update_configs(self, option_parser_mock, curl_mock,
                          backup_file_mock, file_handler_mock, read_mapping_mock,
                          get_config_mock, get_config_resp_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    opm = option_parser_mock.return_value
    options = MagicMock()
    args = ["update-configs"]
    opm.parse_args.return_value = (options, args)
    options.logfile = "logfile"
    options.user = "admin"
    options.password = "admin"
    options.hostname = "localhost"
    options.clustername = "c1"
    curl_mock.side_effect = ['', '', '', '', '', '', '', '']
    read_mapping_mock.return_value = {
      "TASKTRACKER": ["c6401", "c6402"],
      "JOBTRACKER": ["c6401"],
      "MAPREDUCE_CLIENT": ["c6403"]}
    get_config_resp_mock.return_value = "hbase-site", {}
    site_properties = {
      "mapred.hosts": "an_old_value",
      "mapred.hosts.exclude": "an_old_value",
      "mapred.jobtracker.maxtasks.per.job": "an_old_value",
      "hbase.rpc.engine": "an_old_value",
      "dfs.df.interval": "an_old_value",
      "mapred.userlog.retain.hours": "will_not_be_stored",
      "global1": "global11"
    }
    get_config_mock.side_effect = [
      site_properties.copy(), site_properties.copy(), site_properties.copy(),
      site_properties.copy(), site_properties.copy(), site_properties.copy()]

    saved_global = UpgradeHelper_HDP2.GLOBAL
    saved_hdfs = UpgradeHelper_HDP2.HDFS_SITE
    saved_core = UpgradeHelper_HDP2.CORE_SITE
    saved_habse = UpgradeHelper_HDP2.HBASE_SITE
    saved_hive = UpgradeHelper_HDP2.HIVE_SITE
    saved_mapred = UpgradeHelper_HDP2.MAPRED_SITE
    try:
      UpgradeHelper_HDP2.GLOBAL = {"global2": "REPLACE_WITH_global1"}
      UpgradeHelper_HDP2.HDFS_SITE = {"global2": "REPLACE_WITH_global1"}
      UpgradeHelper_HDP2.CORE_SITE = {"global2": "REPLACE_WITH_global1"}
      UpgradeHelper_HDP2.main()
    finally:
      UpgradeHelper_HDP2.GLOBAL = saved_global
      UpgradeHelper_HDP2.HDFS_SITE = saved_hdfs
      UpgradeHelper_HDP2.CORE_SITE = saved_core
      UpgradeHelper_HDP2.MAPRED_SITE = saved_mapred

    self.assertEqual(8, len(curl_mock.call_args_list))
    self.validate_update_config_call(curl_mock.call_args_list[0], "capacity-scheduler")
    self.validate_update_config_call(curl_mock.call_args_list[1], "yarn-site")
    self.validate_update_config_call(curl_mock.call_args_list[3], "mapred-site")
    self.validate_update_config_call(curl_mock.call_args_list[2], "global")
    self.validate_config_replacememt(curl_mock.call_args_list[1], "yarn-site")
    self.validate_config_replacememt(curl_mock.call_args_list[2], "global")
    self.validate_config_replacememt(curl_mock.call_args_list[3], "mapred-site")
    self.validate_config_replacememt(curl_mock.call_args_list[4], "hdfs-site")
    self.validate_config_replacememt(curl_mock.call_args_list[5], "core-site")
    self.validate_config_replacememt(curl_mock.call_args_list[6], "hbase-site")
    pass

  @patch.object(UpgradeHelper_HDP2, "read_mapping")
  @patch("subprocess.Popen")
  @patch.object(UpgradeHelper_HDP2, "get_YN_input")
  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, "backup_file")
  @patch.object(UpgradeHelper_HDP2, 'curl')
  @patch('optparse.OptionParser')
  def test_print_only(self, option_parser_mock, curl_mock,
                      backup_file_mock, file_handler_mock, get_yn_mock, popen_mock, read_mapping_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    options = self.get_mock_options(True)
    get_yn_mock.return_value = True
    read_mapping_mock.return_value = {
      "TASKTRACKER": ["c6401", "c6402"]}
    UpgradeHelper_HDP2.delete_mr(options)
    self.assertFalse(popen_mock.called)
    pass

  @patch.object(logging, 'FileHandler')
  @patch.object(UpgradeHelper_HDP2, "backup_file")
  @patch.object(UpgradeHelper_HDP2, 'curl')
  def test_get_and_parse_properties(self, curl_mock,
                                    backup_file_mock, file_handler_mock):
    file_handler_mock.return_value = logging.FileHandler('') # disable creating real file
    options = self.get_mock_options()
    curl_mock.side_effect = [
      """{ "href" : "http://localhost:8080/api/v1/clusters/c1",
           "Clusters" : {
              "cluster_id" : 2,
              "cluster_name" : "c1",
              "version" : "HDP-2.0.5",
              "desired_configs" : {
                "capacity-scheduler" : {
                  "user" : "admin",
                  "tag" : "version137"
                },
                "core-site" : {
                   "user" : "admin",
                   "tag" : "version138"
                }}}}""",
      """{
           "href" : "http://localhost:8080/api/v1/clusters/c1/configurations?type=core-site&tag=version138",
           "items" : [
             {
               "href" : "http://localhost:8080/api/v1/clusters/c1/configurations?type=core-site&tag=version138",
               "tag" : "version1378850572477",
               "type" : "core-site",
               "Config" : {
                 "cluster_name" : "c1"
               },
               "properties" : {
                 "name1" : "value1",
                 "name2" : "value2",
                 "name3" : "value3"
               }}]}""",
      """{ "href" : "http://localhost:8080/api/v1/clusters/c1",
           "Clusters" : {
              "desired_configs" : {
                "capacity-scheduler" : {
                  "user" : "admin",
                  "tag" : "version137"
                }}}}"""]
    properties = UpgradeHelper_HDP2.get_config(options, "core-site")
    self.assertTrue(len(properties.keys()) == 3)
    self.assertTrue(properties["name1"] == "value1")
    self.assertTrue(properties["name2"] == "value2")
    self.assertTrue(properties["name3"] == "value3")
    try:
      UpgradeHelper_HDP2.get_config(options, "hdfs-site")
    except Exception, e:
      self.assertTrue('Unable to get the current version for config type hdfs-site' in e.reason)
      pass
    pass

  def test_rename_all_properties(self):
    site_properties = {
      "mapred.task.is.map": "mapreduce.task.ismap",
      "mapred.task.partition": "mapreduce.task.partition",
      "mapred.task.profile": "mapreduce.task.profile",
      "abc": "abc"
    }
    site_properties = \
      UpgradeHelper_HDP2.rename_all_properties(site_properties, UpgradeHelper_HDP2.PROPERTY_MAPPING)
    for key in site_properties.keys():
      self.assertEqual(key, site_properties[key])
    self.assertEqual(4, len(site_properties))
    pass

  def test_tags_count(self):
    def count_tags(template):
      deleted = 0
      replaced = 0
      for key in template.keys():
        value = template[key]
        if value == UpgradeHelper_HDP2.DELETE_OLD_TAG:
          deleted += 1
          continue
        if value.find(UpgradeHelper_HDP2.REPLACE_WITH_TAG) == 0:
          replaced += 1
          continue
        pass
      return deleted, replaced

    deleted, replaced = count_tags(UpgradeHelper_HDP2.GLOBAL)
    self.assertEqual(0, replaced)
    self.assertEqual(88, deleted)

    deleted, replaced = count_tags(UpgradeHelper_HDP2.MAPRED_SITE)
    self.assertEqual(0, replaced)
    self.assertEqual(95, deleted)

    deleted, replaced = count_tags(UpgradeHelper_HDP2.CORE_SITE)
    self.assertEqual(0, replaced)
    self.assertEqual(2, deleted)

    deleted, replaced = count_tags(UpgradeHelper_HDP2.HDFS_SITE)
    self.assertEqual(0, replaced)
    self.assertEqual(8, deleted)
    pass

  def validate_update_config_call(self, call, type):
    args, kargs = call
    self.assertTrue(args[6] == 'PUT')
    self.assertTrue(type in args[8])
    pass

  def validate_config_replacememt(self, call, type):
    args, kargs = call
    self.assertFalse("REPLACE_WITH_" in args[8])
    self.assertFalse("REPLACE_JH_HOST" in args[8])
    self.assertFalse("REPLACE_RM_HOST" in args[8])
    if type == "yarn-site":
      self.assertTrue("c6401" in args[8])
      self.assertFalse("an_old_value" in args[8])
    elif type == "mapred-site":
      self.assertFalse("will_not_be_stored" in args[8])
      self.assertTrue("fs.df.interval" in args[8])
      self.assertFalse("dfs.df.interval" in args[8])
    elif type == "global":
      self.assertTrue("global11" in args[8])
      self.assertTrue("an_old_value" in args[8])
      self.assertTrue("mapred.hosts.exclude" in args[8])
    elif (type == "core-site") or (type == "hdfs-site"):
      self.assertTrue("global11" in args[8])
      self.assertTrue("global2" in args[8])
      self.assertTrue("hbase.rpc.engine" in args[8])
    elif type == "hbase-site":
      self.assertTrue("global11" in args[8])
      self.assertTrue("hbase.hstore.blockingStoreFiles" in args[8])
      self.assertTrue("dfs.df.interval" in args[8])
      self.assertFalse("hbase.rpc.engine" in args[8])
    pass

  def get_mock_options(self, printonly=False):
    options = MagicMock()
    options.logfile = "logfile"
    options.user = "admin"
    options.password = "admin"
    options.hostname = "localhost"
    options.clustername = "c1"
    options.printonly = printonly
    return options


if __name__ == "__main__":
  unittest.main()
