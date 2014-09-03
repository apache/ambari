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

import StringIO
import sys
from unittest import TestCase
from mock.mock import patch, MagicMock


utils = __import__('ambari_server.utils').utils


class TestUtils(TestCase):
  @patch('os.listdir')
  @patch('os.path.isdir')
  def test_get_ubuntu_pg_version(self, path_isdir_mock, os_listdir_mock):
    path_isdir_mock.return_value = True
    os_listdir_mock.return_value = ['8.4', '9.1']

    self.assertEqual('9.1', utils.get_ubuntu_pg_version())

  @patch('ambari_server.utils.get_ubuntu_pg_version')
  @patch('os.path.isfile')
  @patch("subprocess.Popen")
  def test_get_postgre_hba_dir(self, popenMock, os_path_is_fine_mock,
                               get_ubuntu_pg_version_mock):
    p = MagicMock()
    utils.PG_HBA_INIT_FILES['ubuntu'] = '/tmp'
    get_ubuntu_pg_version_mock.return_value = '9.1'
    self.assertEqual('/tmp/9.1/main', utils.get_postgre_hba_dir('ubuntu'))

    # ## Tests depends on postgres version ###
    # 1) PGDATA=/var/lib/pgsql/data
    os_path_is_fine_mock.return_value = True
    utils.PG_HBA_ROOT_DEFAULT = '/def/dir'
    p.communicate.return_value = ('/my/new/location\n', None)
    p.returncode = 0
    popenMock.return_value = p
    self.assertEqual('/my/new/location', utils.get_postgre_hba_dir('redhat'))

    # 2) No value set
    os_path_is_fine_mock.return_value = True
    utils.PG_HBA_ROOT_DEFAULT = '/def/dir'
    p.communicate.return_value = ('\n', None)
    p.returncode = 0
    popenMock.return_value = p
    self.assertEqual('/def/dir', utils.get_postgre_hba_dir('redhat'))

    # 3) Value set - check diff systems
    os_path_is_fine_mock.return_value = True
    popenMock.reset()
    p.communicate.return_value = (None, None)
    utils.get_postgre_hba_dir('redhat')
    popenMock.assert_called_with('alias exit=return; source /etc/rc.d/init.d/postgresql status &>/dev/null; echo $PGDATA', shell=True, stdin=-1, stderr=-1, stdout=-1)

    popenMock.reset()
    p.communicate.return_value = (None, None)
    utils.get_postgre_hba_dir('suse')
    popenMock.assert_called_with('alias exit=return; source /etc/init.d/postgresql status &>/dev/null; echo $PGDATA', shell=True, stdin=-1, stderr=-1, stdout=-1)

  @patch('ambari_server.utils.get_ubuntu_pg_version')
  def test_get_postgre_running_status(self, get_ubuntu_pg_version_mock):
    utils.PG_STATUS_RUNNING_DEFAULT = "red_running"
    get_ubuntu_pg_version_mock.return_value = '9.1'

    self.assertEqual('9.1/main', utils.get_postgre_running_status('ubuntu'))
    self.assertEqual('red_running', utils.get_postgre_running_status('redhat'))

  @patch('os.path.isfile')
  def test_locate_file(self, isfile_mock):
    utils.ENV_PATH = ['/test']
    # File was found in the path
    isfile_mock.return_value = True
    self.assertEquals('/test/myfile', utils.locate_file('myfile'))
    # File not found in the path
    isfile_mock.return_value = False
    self.assertEquals('myfile', utils.locate_file('myfile'))
    # Testing default vaule
    isfile_mock.return_value = False
    self.assertEquals('/tmp/myfile', utils.locate_file('myfile', '/tmp'))

  @patch('os.path.exists')
  @patch('os.path.join')
  def test_pid_exists(self, path_join_mock, path_exists_mock):
    path_join_mock.return_value = '/test'
    path_exists_mock.return_value = True
    self.assertTrue(utils.pid_exists('1'))

  @patch('time.time')
  @patch('__builtin__.open')
  @patch('time.sleep')
  @patch('os.listdir')
  @patch('os.path.join')
  @patch.object(utils, 'get_symlink_path')
  def test_looking_for_pid(self, get_symlink_path_mock, path_join_mock,
                      listdir_mock, sleep_mock, open_mock, time_mock):
    def test_read():
      return "test args"

    def test_obj():
      pass

    test_obj.read = test_read
    path_join_mock.return_value = '/'
    open_mock.return_value = test_obj
    listdir_mock.return_value = ['1000']
    get_symlink_path_mock.return_value = "/symlinkpath"
    time_mock.side_effect = [0, 0, 0, 0, 0, 0, 6]

    out = StringIO.StringIO()
    sys.stdout = out
    r = utils.looking_for_pid("test args", 5)
    self.assertEqual(".....", out.getvalue())
    sys.stdout = sys.__stdout__

    self.assertEquals(len(r), 1)
    self.assertEquals(r[0], {
       "pid": "1000",
       "exe": "/symlinkpath",
       "cmd": "test args"
      })

  @patch('os.path.normpath')
  @patch('os.path.join')
  @patch('os.path.dirname')
  @patch('os.readlink')
  def test_get_symlink_path(self, readlink_mock, dirname_mock, join_mock,
                            normpath_mock):
    normpath_mock.return_value = "test value"
    self.assertEquals(utils.get_symlink_path("/"), "test value")

  @patch('time.time')
  @patch.object(utils, 'pid_exists')
  @patch('time.sleep')
  def test_wait_for_pid(self, sleep_mock, pid_exists_mock, time_mock):
    pid_exists_mock.return_value = True
    time_mock.side_effect = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 11]

    out = StringIO.StringIO()
    sys.stdout = out
    live_pids = utils.wait_for_pid([
                                   {"pid": "111",
                                    "exe": "",
                                    "cmd": ""
                                    },
                                   {"pid": "222",
                                    "exe": "",
                                    "cmd": ""
                                    },
                                   ], 10)
    self.assertEqual("..........", out.getvalue())
    sys.stdout = sys.__stdout__

    self.assertEquals(2, live_pids)

  @patch.object(utils, 'pid_exists')
  @patch('__builtin__.open')
  @patch('os.kill')
  def test_save_main_pid_ex(self, kill_mock, open_mock, pid_exists_mock):
    def test_write(data):
      self.assertEquals(data, "222\n")

    def test_close():
      pass

    def test_obj():
      pass

    test_obj.write = test_write
    test_obj.close = test_close
    open_mock.return_value = test_obj
    pid_exists_mock.return_value = True

    utils.save_main_pid_ex([{"pid": "111",
                             "exe": "/exe1",
                             "cmd": ""
                             },
                            {"pid": "222",
                             "exe": "/exe2",
                             "cmd": ""
                             },
                            ], "/pidfile", ["/exe1"], True)
    self.assertEquals(open_mock.call_count, 1)
    self.assertEquals(pid_exists_mock.call_count, 4)
    self.assertEquals(kill_mock.call_count, 1)

  @patch('os.path.isfile')
  @patch('__builtin__.open')
  @patch('os.remove')
  def test_check_exitcode(self, remove_mock, open_mock, isfile_mock):
    def test_read():
      return "777"

    def test_close():
      pass

    def test_obj():
      pass

    test_obj.read = test_read
    test_obj.close = test_close
    open_mock.return_value = test_obj
    isfile_mock.return_value = True

    self.assertEquals(utils.check_exitcode("/tmp/nofile"), 777)
