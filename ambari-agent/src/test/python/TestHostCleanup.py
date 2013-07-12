#!/usr/bin/env python2.6

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

from unittest import TestCase
import unittest
from mock.mock import patch, MagicMock, call, create_autospec
from ambari_agent import HostCleanup
import StringIO
import sys
import tempfile
import os.path

PACKAGE_SECTION = "packages"
PACKAGE_KEY = "pkg_list"
USER_SECTION = "users"
USER_KEY = "usr_list"
REPO_SECTION = "repositories"
REPOS_KEY = "pkg_list"
DIR_SECTION = "directories"
DIR_KEY = "dir_list"
PROCESS_SECTION = "processes"
PROCESS_KEY = "proc_list"
ALT_SECTION = "alternatives"
ALT_KEYS = ["symlink_list", "target_list"]
ALT_ERASE_CMD = "alternatives --remove {0} {1}"

class TestHostCleanup(TestCase):

  def setUp(self):
    HostCleanup.logger = MagicMock()
    self.hostcleanup = HostCleanup.HostCleanup()
    # disable stdout
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    # enable stdout
    sys.stdout = sys.__stdout__

  def test_read_host_check_file_with_content(self):
    out = StringIO.StringIO()
    sys.stdout = out
    tmpfile = tempfile.mktemp()
    f = open(tmpfile,'w')
    fileContent = """[processes]
proc_list = 323,434

[users]
usr_list = rrdcached,ambari-qa,hive,oozie,hbase,hcat,mysql,mapred,hdfs,zookeeper,sqoop,nagios

[repositories]
repo_list = HDP-1.3.0,HDP-epel

[directories]
dir_list = /etc/hadoop,/etc/hbase,/etc/hcatalog,/tmp/hive,/tmp/nagios,/var/nagios

[alternatives]
symlink_list = hcatalog-conf,hadoop-default,hadoop-log,oozie-conf
target_list = /etc/hcatalog/conf.dist,/usr/share/man/man1/hadoop.1.gz,/etc/oozie/conf.dist,/usr/lib/hadoop

[packages]
pkg_list = sqoop.noarch,hadoop-libhdfs.x86_64,rrdtool.x86_64,ganglia-gmond.x86_64

[metadata]
created = 2013-07-02 20:39:22.162757"""
    f.write(fileContent)
    f.close()

    propMap = self.hostcleanup.read_host_check_file(tmpfile)
    self.assertTrue("323" in propMap["processes"])
    self.assertTrue("mysql" in propMap["users"])
    self.assertTrue("HDP-epel" in propMap["repositories"])
    self.assertTrue("/etc/hadoop" in propMap["directories"])
    self.assertTrue("hcatalog-conf" in propMap["alternatives"]["symlink_list"])
    self.assertTrue("/etc/oozie/conf.dist" in propMap["alternatives"]["target_list"])
    self.assertTrue("hadoop-libhdfs.x86_64" in propMap["packages"])
    sys.stdout = sys.__stdout__


  @patch.object(HostCleanup.HostCleanup, 'do_erase_alternatives')
  @patch.object(HostCleanup.HostCleanup, 'find_repo_files_for_repos')
  @patch.object(HostCleanup.HostCleanup, 'get_os_type')
  @patch.object(HostCleanup.HostCleanup, 'do_kill_processes')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_files_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_dir_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_delete_users')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_packages')
  def test_do_cleanup_all(self, do_erase_packages_method, do_delete_users_method,
                      do_erase_dir_silent_method,
                      do_erase_files_silent_method, do_kill_processes_method,
                      get_os_type_method, find_repo_files_for_repos_method,
                      do_erase_alternatives_method):
    global SKIP_LIST
    oldSkipList = HostCleanup.SKIP_LIST
    HostCleanup.SKIP_LIST = []
    out = StringIO.StringIO()
    sys.stdout = out
    propertyMap = {PACKAGE_SECTION:['abcd', 'pqrst'], USER_SECTION:['abcd', 'pqrst'],
                   REPO_SECTION:['abcd', 'pqrst'], DIR_SECTION:['abcd', 'pqrst'],
                   PROCESS_SECTION:['abcd', 'pqrst'],
                   ALT_SECTION:{ALT_KEYS[0]:['alt1','alt2'], ALT_KEYS[1]:[
                     'dir1']}}
    get_os_type_method.return_value = 'redhat'
    find_repo_files_for_repos_method.return_value = ['abcd', 'pqrst']

    self.hostcleanup.do_cleanup(propertyMap)

    self.assertTrue(do_delete_users_method.called)
    self.assertTrue(do_erase_dir_silent_method.called)
    self.assertTrue(do_erase_files_silent_method.called)
    self.assertTrue(do_erase_packages_method.called)
    self.assertTrue(do_kill_processes_method.called)
    self.assertTrue(do_erase_alternatives_method.called)
    do_erase_dir_silent_method.assert_called_once_with(['abcd', 'pqrst'])
    do_erase_packages_method.assert_called_once_with(['abcd', 'pqrst'])
    do_erase_files_silent_method.assert_called_once_with(['abcd', 'pqrst'])
    do_delete_users_method.assert_called_once_with(['abcd', 'pqrst'])
    do_kill_processes_method.assert_called_once_with(['abcd', 'pqrst'])
    do_erase_alternatives_method.assert_called_once_with({ALT_KEYS[0]:['alt1',
                                              'alt2'], ALT_KEYS[1]:['dir1']})

    HostCleanup.SKIP_LIST = oldSkipList
    sys.stdout = sys.__stdout__


  @patch.object(HostCleanup.HostCleanup, 'do_erase_alternatives')
  @patch.object(HostCleanup.HostCleanup, 'find_repo_files_for_repos')
  @patch.object(HostCleanup.HostCleanup, 'get_os_type')
  @patch.object(HostCleanup.HostCleanup, 'do_kill_processes')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_files_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_dir_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_delete_users')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_packages')
  def test_do_cleanup_default(self, do_erase_packages_method, do_delete_users_method,
                      do_erase_dir_silent_method,
                      do_erase_files_silent_method, do_kill_processes_method,
                      get_os_type_method, find_repo_files_for_repos_method,
                      do_erase_alternatives_method):

    out = StringIO.StringIO()
    sys.stdout = out
    propertyMap = {PACKAGE_SECTION:['abcd', 'pqrst'], USER_SECTION:['abcd', 'pqrst'],
                   REPO_SECTION:['abcd', 'pqrst'], DIR_SECTION:['abcd', 'pqrst'],
                   PROCESS_SECTION:['abcd', 'pqrst'],
                   ALT_SECTION:{ALT_KEYS[0]:['alt1','alt2'], ALT_KEYS[1]:[
                     'dir1']}}
    get_os_type_method.return_value = 'redhat'
    find_repo_files_for_repos_method.return_value = ['abcd', 'pqrst']

    self.hostcleanup.do_cleanup(propertyMap)

    self.assertFalse(do_delete_users_method.called)
    self.assertTrue(do_erase_dir_silent_method.called)
    self.assertTrue(do_erase_files_silent_method.called)
    self.assertTrue(do_erase_packages_method.called)
    self.assertTrue(do_kill_processes_method.called)
    self.assertTrue(do_erase_alternatives_method.called)
    sys.stdout = sys.__stdout__

  @patch.object(HostCleanup.HostCleanup, 'find_repo_files_for_repos')
  @patch.object(HostCleanup.HostCleanup, 'get_os_type')
  @patch.object(HostCleanup.HostCleanup, 'do_kill_processes')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_files_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_dir_silent')
  @patch.object(HostCleanup.HostCleanup, 'do_delete_users')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_packages')
  def test_do_cleanup_with_skip(self, do_erase_packages_method,
                      do_delete_users_method,
                      do_erase_dir_silent_method,
                      do_erase_files_silent_method, do_kill_processes_method,
                      get_os_type_method, find_repo_files_for_repos_method):

    out = StringIO.StringIO()
    sys.stdout = out
    propertyMap = {PACKAGE_SECTION:['abcd', 'pqrst'], USER_SECTION:['abcd', 'pqrst'],
                   REPO_SECTION:['abcd', 'pqrst'], DIR_SECTION:['abcd', 'pqrst'],
                   PROCESS_SECTION:['abcd', 'pqrst']}
    get_os_type_method.return_value = 'redhat'
    find_repo_files_for_repos_method.return_value = ['abcd', 'pqrst']
    HostCleanup.SKIP_LIST = [PACKAGE_SECTION, REPO_SECTION]

    self.hostcleanup.do_cleanup(propertyMap)

    self.assertTrue(do_delete_users_method.called)
    self.assertTrue(do_erase_dir_silent_method.called)
    self.assertFalse(do_erase_files_silent_method.called)
    self.assertFalse(do_erase_packages_method.called)
    self.assertTrue(do_kill_processes_method.called)
    do_erase_dir_silent_method.assert_called_once_with(['abcd', 'pqrst'])
    do_delete_users_method.assert_called_once_with(['abcd', 'pqrst'])
    do_kill_processes_method.assert_called_once_with(['abcd', 'pqrst'])

    sys.stdout = sys.__stdout__


  @patch("ConfigParser.RawConfigParser")
  @patch("__builtin__.open")
  def test_read_host_check_file(self, openMock, readMock):
    out = StringIO.StringIO()
    sys.stdout = out
    f = MagicMock()
    openMock.return_value = f

    propertyMap = self.hostcleanup.read_host_check_file('test')

    self.assertTrue(openMock.called)
    self.assertTrue(readMock.called)
    self.assertTrue(propertyMap.has_key(PACKAGE_SECTION))
    self.assertTrue(propertyMap.has_key(REPO_SECTION))
    self.assertTrue(propertyMap.has_key(USER_SECTION))
    self.assertTrue(propertyMap.has_key(DIR_SECTION))
    self.assertTrue(propertyMap.has_key(PROCESS_SECTION))

    sys.stdout = sys.__stdout__


  @patch.object(HostCleanup.HostCleanup, 'run_os_command')
  @patch.object(HostCleanup.HostCleanup, 'get_os_type')
  def test_do_earse_packages(self, get_os_type_method, run_os_command_method):
    out = StringIO.StringIO()
    sys.stdout = out

    get_os_type_method.return_value = 'redhat'
    run_os_command_method.return_value = (0, 'success', 'success')

    retval = self.hostcleanup.do_erase_packages(['abcd', 'wxyz'])

    self.assertTrue(get_os_type_method.called)
    self.assertTrue(run_os_command_method.called)
    run_os_command_method.assert_called_with("yum erase -y {0}".format(' '
    .join(['abcd', 'wxyz'])))
    self.assertEquals(0, retval)

    get_os_type_method.reset()
    run_os_command_method.reset()

    get_os_type_method.return_value = 'suse'
    run_os_command_method.return_value = (0, 'success', 'success')

    retval = self.hostcleanup.do_erase_packages(['abcd', 'wxyz'])

    self.assertTrue(get_os_type_method.called)
    self.assertTrue(run_os_command_method.called)
    run_os_command_method.assert_called_with("zypper -n -q remove {0}"
    .format(' '.join(['abcd', 'wxyz'])))
    self.assertEquals(0, retval)

    sys.stdout = sys.__stdout__

  @patch.object(HostCleanup.HostCleanup, 'get_files_in_dir')
  @patch.object(HostCleanup.HostCleanup, 'get_os_type')
  def test_find_repo_files_for_repos(self, get_os_type_method,
                                    get_files_in_dir_method):
    out = StringIO.StringIO()
    sys.stdout = out

    tmpfile = tempfile.mktemp()
    fileContent = """[###]
[aass]
[$$]
444]saas[333
1122[naas]2222
name=sd des derft 3.1
"""
    with open(tmpfile,'w') as file:
      file.write(fileContent)
    get_os_type_method.return_value = 'redhat'
    get_files_in_dir_method.return_value = [ tmpfile ]

    repoFiles = self.hostcleanup.find_repo_files_for_repos(['aass'])
    self.assertTrue(get_files_in_dir_method.called)
    self.assertTrue(get_os_type_method.called)
    self.assertEquals(repoFiles, [ tmpfile ])

    repoFiles = self.hostcleanup.find_repo_files_for_repos(['sd des derft 3.1'])
    self.assertTrue(get_files_in_dir_method.called)
    self.assertTrue(get_os_type_method.called)
    self.assertEquals(repoFiles, [ tmpfile ])

    repoFiles = self.hostcleanup.find_repo_files_for_repos(['sd des derft 3.1', 'aass'])
    self.assertEquals(repoFiles, [ tmpfile ])

    repoFiles = self.hostcleanup.find_repo_files_for_repos(['saas'])
    self.assertEquals(repoFiles, [])

    repoFiles = self.hostcleanup.find_repo_files_for_repos([''])
    self.assertEquals(repoFiles, [])

    sys.stdout = sys.__stdout__


  @patch.object(HostCleanup.HostCleanup, 'run_os_command')
  @patch.object(HostCleanup.HostCleanup, 'do_erase_dir_silent')
  @patch.object(HostCleanup.HostCleanup, 'get_alternatives_desc')
  def test_do_erase_alternatives(self, get_alternatives_desc_mock,
                    do_erase_dir_silent_mock, run_os_command_mock):
    out = StringIO.StringIO()
    sys.stdout = out

    get_alternatives_desc_mock.return_value = 'somepath to alternative\n'
    run_os_command_mock.return_value = (0, None, None)

    alt_map = {ALT_KEYS[0]:['alt1'], ALT_KEYS[1]:['dir1']}

    self.hostcleanup.do_erase_alternatives(alt_map)

    self.assertTrue(get_alternatives_desc_mock.called)
    get_alternatives_desc_mock.called_once_with('alt1')
    self.assertTrue(run_os_command_mock.called)
    run_os_command_mock.called_once_with(ALT_ERASE_CMD.format('alt1', 'somepath'))
    self.assertTrue(do_erase_dir_silent_mock.called)
    do_erase_dir_silent_mock.called_once_with(['dir1'])

    sys.stdout = sys.__stdout__

if __name__ == "__main__":
  unittest.main(verbosity=2)
