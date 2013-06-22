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
from unittest import TestCase
import sys
from mock.mock import patch
from mock.mock import MagicMock
from mock.mock import create_autospec
import os, errno, tempfile
import signal
import stat
# We have to use this import HACK because the filename contains a dash
ambari_server = __import__('ambari-server')


class TestAmbariServer(TestCase):

  @patch.object(ambari_server, 'configure_database_username_password')
  @patch.object(ambari_server, 'run_os_command')
  @patch('optparse.Values')
  def test_configure_pg_hba_ambaridb_users(self, OptParseValuesMock,
                                run_os_command_method,
                                configure_database_username_password_method):
    # Prepare mocks
    run_os_command_method.return_value = (0, "", "")
    opvm = OptParseValuesMock.return_value
    opvm.database_username = "ffdf"
    tf1 = tempfile.NamedTemporaryFile()
    ambari_server.PG_HBA_CONF_FILE = tf1.name
    # Run test
    ambari_server.configure_pg_hba_ambaridb_users()
    # Check results
    self.assertTrue(run_os_command_method.called)
    self.assertTrue(configure_database_username_password_method.called)
    string_expected = self.get_file_string(self
      .get_samples_dir("configure_pg_hba_ambaridb_users1"))
    string_actual = self.get_file_string(ambari_server.PG_HBA_CONF_FILE)
    self.assertEquals(string_expected, string_actual)
    pass



  def test_configure_pg_hba_postgres_user(self):
    tf1 = tempfile.NamedTemporaryFile()
    ambari_server.PG_HBA_CONF_FILE = tf1.name

    with open(ambari_server.PG_HBA_CONF_FILE, 'w') as fout:
      fout.write("\n")
      fout.write("local  all  all md5\n")
      fout.write("host  all   all 0.0.0.0/0  md5\n")
      fout.write("host  all   all ::/0 md5\n")

    ambari_server.configure_pg_hba_postgres_user()

    expected  = self.get_file_string(self.get_samples_dir(
      "configure_pg_hba_ambaridb_users2"))
    result = self.get_file_string(ambari_server.PG_HBA_CONF_FILE)
    self.assertEqual(expected, result, "pg_hba_conf not processed")

    mode = oct(os.stat(ambari_server.PG_HBA_CONF_FILE)[stat.ST_MODE])
    str_mode = str(mode)[-4:]
    self.assertEqual("0644", str_mode, "Wrong file permissions")



  @patch('__builtin__.raw_input')
  def test_get_choice_string_input(self, raw_input_method):

    out = StringIO.StringIO()
    sys.stdout = out
   

    prompt = "blablabla"
    default = "default blablabla"
    firstChoice = set(['yes','ye', 'y'])
    secondChoice = set(['no','n'])
    # test first input
    raw_input_method.return_value = "Y"

    result = ambari_server.get_choice_string_input(prompt, default,
        firstChoice, secondChoice)
    self.assertEquals(result, True)
    raw_input_method.reset_mock()
    # test second input

    raw_input_method.return_value = "N"

    result = ambari_server.get_choice_string_input(prompt, default,
        firstChoice, secondChoice)
    self.assertEquals(result, False)

    raw_input_method.reset_mock()

    # test enter pressed

    raw_input_method.return_value = ""

    result = ambari_server.get_choice_string_input(prompt, default,
        firstChoice, secondChoice)
    self.assertEquals(result, default)

    raw_input_method.reset_mock()

    # test wrong input
    list_of_return_values= ['yes', 'dsad', 'fdsfds']
    def side_effect(list):
      return list_of_return_values.pop()
    raw_input_method.side_effect = side_effect

    result = ambari_server.get_choice_string_input(prompt, default,
        firstChoice, secondChoice)
    self.assertEquals(result, True)
    self.assertEquals(raw_input_method.call_count, 3)

    sys.stdout = sys.__stdout__

    pass
  
  @patch('__builtin__.raw_input')
  def test_servicename_regex(self, raw_input_method):

    ''' Test to make sure the service name can contain digits '''
    ambari_server.SILENT = False
    raw_input_method.return_value = "OT100"
    result = ambari_server.get_validated_service_name("ambari", 1)
    self.assertEqual("OT100", result, "Not accepting digits")
    pass
    
  @patch('__builtin__.raw_input')
  def test_dbname_regex(self, raw_input_method):

    ''' Test to make sure the service name can contain digits '''
    ambari_server.SILENT = False
    raw_input_method.return_value = "OT100"
    result = ambari_server.get_validated_db_name("ambari")
    self.assertEqual("OT100", result, "Not accepting digits")


  @patch('re.search')
  @patch('__builtin__.raw_input')
  @patch('getpass.getpass')
  def test_get_validated_string_input(self, get_pass_method,
      raw_input_method, re_search_method):

  
    out = StringIO.StringIO()
    sys.stdout = out
    prompt = "blabla"
    default = "default_pass"
    pattern = "pattern_pp"
    description = "blabla2"
    # check password input
    is_pass = True
    get_pass_method.return_value = "dfdsfdsfds"
    
    result = ambari_server.get_validated_string_input(prompt, default,
        pattern, description, is_pass)

    self.assertEquals(get_pass_method.return_value, result)
    get_pass_method.assure_called_once(prompt)
    self.assertFalse(raw_input_method.called)

    # check raw input
    get_pass_method.reset_mock()
    raw_input_method.reset_mock()
    is_pass = False
    raw_input_method.return_value = "dkf90ewuf0"

    result = ambari_server.get_validated_string_input(prompt, default,
        pattern, description, is_pass)

    self.assertEquals(raw_input_method.return_value, result)
    self.assertFalse(get_pass_method.called)
    raw_input_method.assure_called_once(prompt)

    sys.stdout = sys.__stdout__



  def test_get_pass_file_path(self):

    result = ambari_server.get_pass_file_path("/etc/ambari/conf_file")
    self.assertEquals("/etc/ambari/password.dat", result)
    pass





  @patch.object(ambari_server, 'setup')
  @patch.object(ambari_server, 'start')
  @patch.object(ambari_server, 'stop')
  @patch.object(ambari_server, 'reset')
  @patch('optparse.OptionParser')
  def test_main_test_setup(self, OptionParserMock, reset_method, stop_method,
                           start_method, setup_method):

    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["setup"]
    opm.parse_args.return_value = (options, args)

    options.database=None
    options.sid_or_sname = "sid"
    ambari_server.main()

    self.assertTrue(setup_method.called)
    self.assertFalse(start_method.called)
    self.assertFalse(stop_method.called)
    self.assertFalse(reset_method.called)

    self.assertFalse(False, ambari_server.VERBOSE)
    self.assertFalse(False, ambari_server.SILENT)


  @patch.object(ambari_server, 'setup')
  @patch.object(ambari_server, 'start')
  @patch.object(ambari_server, 'stop')
  @patch.object(ambari_server, 'reset')
  @patch('optparse.OptionParser')
  def test_main_test_start(self, OptionParserMock, reset_method, stop_method,
                           start_method, setup_method):

    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["setup"]
    opm.parse_args.return_value = (options, args)
    options.database=None
    options.sid_or_sname = "sid"

    ambari_server.main()

    self.assertTrue(setup_method.called)
    self.assertFalse(start_method.called)
    self.assertFalse(stop_method.called)
    self.assertFalse(reset_method.called)

    self.assertFalse(False, ambari_server.VERBOSE)
    self.assertFalse(False, ambari_server.SILENT)



  @patch.object(ambari_server, 'setup')
  @patch.object(ambari_server, 'start')
  @patch.object(ambari_server, 'stop')
  @patch.object(ambari_server, 'reset')
  @patch('optparse.OptionParser')
  def test_main_test_stop(self, OptionParserMock, reset_method, stop_method,
                          start_method, setup_method):

    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["stop"]
    opm.parse_args.return_value = (options, args)

    options.database = None
    options.sid_or_sname = "sname"

    ambari_server.main()

    self.assertFalse(setup_method.called)
    self.assertFalse(start_method.called)
    self.assertTrue(stop_method.called)
    self.assertFalse(reset_method.called)

    self.assertFalse(False, ambari_server.VERBOSE)
    self.assertFalse(False, ambari_server.SILENT)



  @patch.object(ambari_server, 'setup')
  @patch.object(ambari_server, 'start')
  @patch.object(ambari_server, 'stop')
  @patch.object(ambari_server, 'reset')
  @patch('optparse.OptionParser')
  def test_main_test_reset(self, OptionParserMock, reset_method, stop_method,
                           start_method, setup_method):

    opm = OptionParserMock.return_value

    options = MagicMock()
    args = ["reset"]
    opm.parse_args.return_value = (options, args)
    options.database=None
    options.sid_or_sname = "sid"

    ambari_server.main()

    self.assertFalse(setup_method.called)
    self.assertFalse(start_method.called)
    self.assertFalse(stop_method.called)
    self.assertTrue(reset_method.called)

    self.assertFalse(False, ambari_server.VERBOSE)
    self.assertFalse(False, ambari_server.SILENT)



  def test_configure_postgresql_conf(self):

    tf1 = tempfile.NamedTemporaryFile()
    ambari_server.POSTGRESQL_CONF_FILE = tf1.name

    with open(ambari_server.POSTGRESQL_CONF_FILE, 'w') as f:
      f.write("#listen_addresses = '127.0.0.1'        #\n")
      f.write("#listen_addresses = '127.0.0.1'")

    ambari_server.configure_postgresql_conf()

    expected  = self.get_file_string(self.get_samples_dir(
      "configure_postgresql_conf1"))
    result = self.get_file_string(ambari_server.POSTGRESQL_CONF_FILE)
    self.assertEqual(expected, result, "postgresql.conf not updated")

    mode = oct(os.stat(ambari_server.POSTGRESQL_CONF_FILE)[stat.ST_MODE])
    str_mode = str(mode)[-4:]
    self.assertEqual("0644", str_mode, "Wrong file permissions")



  @patch.object(ambari_server, "restart_postgres")
  @patch.object(ambari_server, "get_postgre_status")
  @patch.object(ambari_server, "configure_postgresql_conf")
  @patch.object(ambari_server, "configure_pg_hba_ambaridb_users")
  @patch.object(ambari_server, "configure_pg_hba_postgres_user")
  def test_configure_postgres(self, configure_pg_hba_postgres_user_mock,
                              configure_pg_hba_ambaridb_users_mock,
                              configure_postgresql_conf_mock,
                              get_postgre_status_mock,
                              restart_postgres_mock):


    tf1 = tempfile.NamedTemporaryFile()
    tf2 = tempfile.NamedTemporaryFile()
    ambari_server.PG_HBA_CONF_FILE = tf1.name
    ambari_server.PG_HBA_CONF_FILE_BACKUP = tf2.name
    args = MagicMock()

    out = StringIO.StringIO()
    sys.stdout = out
    rcode = ambari_server.configure_postgres()
    sys.stdout = sys.__stdout__
    self.assertEqual(0, rcode)
    self.assertEqual("Backup for pg_hba found, reconfiguration not required\n",
      out.getvalue())

    ambari_server.PG_HBA_CONF_FILE_BACKUP = tempfile.mktemp()
    get_postgre_status_mock.return_value = ambari_server.PG_STATUS_RUNNING
    restart_postgres_mock.return_value = 0

    rcode = ambari_server.configure_postgres()

    self.assertTrue(os.path.isfile(ambari_server.PG_HBA_CONF_FILE_BACKUP),
      "postgresql.conf backup not created")
    self.assertTrue(configure_pg_hba_postgres_user_mock.called)
    self.assertTrue(configure_pg_hba_ambaridb_users_mock.called)
    mode = oct(os.stat(ambari_server.PG_HBA_CONF_FILE)[stat.ST_MODE])
    str_mode = str(mode)[-4:]
    self.assertEqual("0644", str_mode, "Wrong file permissions")
    self.assertTrue(configure_postgresql_conf_mock.called)
    self.assertEqual(0, rcode)

    os.unlink(ambari_server.PG_HBA_CONF_FILE_BACKUP)
    get_postgre_status_mock.return_value = "stopped"
    rcode = ambari_server.configure_postgres()
    self.assertEqual(0, rcode)
    os.unlink(ambari_server.PG_HBA_CONF_FILE_BACKUP)
    sys.stdout = sys.__stdout__



  @patch("time.sleep")
  @patch("subprocess.Popen")
  @patch.object(ambari_server, "run_os_command")
  @patch.object(ambari_server, "get_postgre_status")
  @patch.object(ambari_server, "print_info_msg")
  def test_restart_postgres(self, printInfoMsg_mock, get_postgre_status_mock,
                            run_os_command_mock, popenMock, sleepMock):

    out = StringIO.StringIO()
    sys.stdout = out
    p = MagicMock()
    p.poll.return_value = 0
    popenMock.return_value = p
    rcode = ambari_server.restart_postgres()
    self.assertEqual(0, rcode)

    p.poll.return_value = None
    get_postgre_status_mock.return_value = "stopped"
    run_os_command_mock.return_value = (1, None, None)
    rcode = ambari_server.restart_postgres()
    self.assertEqual(1, rcode)
    sys.stdout = sys.__stdout__



  @patch("shlex.split")
  @patch("subprocess.Popen")
  @patch.object(ambari_server, "print_info_msg")
  def test_run_os_command(self, printInfoMsg_mock, popenMock, splitMock):

    p = MagicMock()
    p.communicate.return_value = (None, None)
    p.returncode = 3
    popenMock.return_value = p

    # with list arg
    cmd = ["exec", "arg"]
    ambari_server.run_os_command(cmd)
    self.assertFalse(splitMock.called)

    # with str arg
    resp = ambari_server.run_os_command("runme")
    self.assertEqual(3, resp[0])
    self.assertTrue(splitMock.called)



  @patch.object(ambari_server, "get_conf_dir")
  @patch.object(ambari_server, "search_file")
  def test_write_property(self, search_file_mock, get_conf_dir_mock):


    expected_content = "key1=val1\n"
    tf1 = tempfile.NamedTemporaryFile()
    search_file_mock.return_value = tf1.name
    ambari_server.write_property("key1", "val1")
    result = tf1.read()
    self.assertEqual(expected_content, result)



  @patch.object(ambari_server, "configure_database_username_password")
  @patch.object(ambari_server, "run_os_command")
  def test_setup_db(self, run_os_command_mock,
                    configure_database_username_password_mock):

    run_os_command_mock.return_value = (0, None, None)
    result = ambari_server.setup_db(MagicMock())
    self.assertTrue(configure_database_username_password_mock.called)
    self.assertEqual(0, result)



  @patch.object(ambari_server, "get_YN_input")
  @patch.object(ambari_server, "run_os_command")
  def test_check_selinux(self, run_os_command_mock, getYNInput_mock):


    out = StringIO.StringIO()
    sys.stdout = out
    run_os_command_mock.return_value = (0, ambari_server.SE_STATUS_DISABLED,
                                        None)
    rcode = ambari_server.check_selinux()
    self.assertEqual(0, rcode)

    getYNInput_mock.return_value = True
    run_os_command_mock.return_value = (0,"enabled "
                                          + ambari_server.SE_MODE_ENFORCING,
                                        None)
    rcode = ambari_server.check_selinux()
    self.assertEqual(0, rcode)
    self.assertTrue(run_os_command_mock.called)
    self.assertTrue(getYNInput_mock.called)
    sys.stdout = sys.__stdout__



  @patch.object(ambari_server, "print_info_msg")
  def test_get_ambari_jars(self, printInfoMsg_mock):

    env = "/ambari/jars"
    os.environ[ambari_server.AMBARI_SERVER_LIB] = env
    result = ambari_server.get_ambari_jars()
    self.assertEqual(env, result)

    del os.environ[ambari_server.AMBARI_SERVER_LIB]
    result = ambari_server.get_ambari_jars()
    self.assertEqual("/usr/lib/ambari-server", result)
    self.assertTrue(printInfoMsg_mock.called)


  @patch("glob.glob")
  @patch.object(ambari_server, "print_info_msg")
  def test_get_share_jars(self, printInfoMsg_mock, globMock):

    globMock.return_value = ["one", "two"]
    expected = "one:two:one:two"
    result = ambari_server.get_share_jars()
    self.assertEqual(expected, result)
    globMock.return_value = []
    expected = ""
    result = ambari_server.get_share_jars()
    self.assertEqual(expected, result)


  @patch("glob.glob")
  @patch.object(ambari_server, "print_info_msg")
  def test_get_ambari_classpath(self, printInfoMsg_mock, globMock):

    globMock.return_value = ["one"]
    result = ambari_server.get_ambari_classpath()
    print result
    self.assertTrue(ambari_server.get_ambari_jars() in result)
    self.assertTrue(ambari_server.get_share_jars() in result)
    globMock.return_value = []
    result = ambari_server.get_ambari_classpath()
    print result
    self.assertTrue(ambari_server.get_ambari_jars() in result)
    self.assertFalse(":" in result)


  @patch.object(ambari_server, "print_info_msg")
  def test_get_conf_dir(self, printInfoMsg_mock):

    env = "/ambari/conf"
    os.environ[ambari_server.AMBARI_CONF_VAR] = env
    result = ambari_server.get_conf_dir()
    self.assertEqual(env, result)

    del os.environ[ambari_server.AMBARI_CONF_VAR]
    result = ambari_server.get_conf_dir()
    self.assertEqual("/etc/ambari-server/conf", result)
    self.assertTrue(printInfoMsg_mock.called)



  def test_search_file(self):

    path = os.path.dirname(__file__)
    result = ambari_server.search_file(__file__, path)
    expected = os.path.abspath(__file__)
    self.assertEqual(expected, result)

    result = ambari_server.search_file("non_existent_file", path)
    self.assertEqual(None, result)


  @patch.object(ambari_server, "run_os_command")
  def test_check_iptables(self, run_os_command_mock):

    out = StringIO.StringIO()
    sys.stdout = out

    run_os_command_mock.return_value = (1, "test", "")
    rcode, info = ambari_server.check_iptables()
    self.assertEqual(1, rcode)
    self.assertEqual("test", info)

    run_os_command_mock.return_value = (2, "",
                                        ambari_server.IP_TBLS_SRVC_NT_FND)
    rcode = ambari_server.check_iptables()
    self.assertEqual(0, rcode)

    sys.stdout = sys.__stdout__


  def test_dlprogress(self):

    out = StringIO.StringIO()
    sys.stdout = out
    ambari_server.dlprogress("filename", 10, 2, 100)
    sys.stdout = sys.__stdout__
    self.assertNotEqual("", out.getvalue())


  @patch("urllib2.urlopen")
  @patch("__builtin__.open")
  @patch.object(ambari_server, "dlprogress")
  def test_track_jdk(self, dlprogress_mock, openMock, urlopenMock):


    u = MagicMock()
    u.info.return_value = {"Content-Length":"24576"}
    chunks = [None, "second", "first"]
    def side_effect(*args, **kwargs):
      return chunks.pop()
    u.read.side_effect = side_effect
    urlopenMock.return_value = u

    f = MagicMock()
    openMock.return_value = f

    ambari_server.track_jdk("base", "url", "local")

    self.assertEqual(0, len(chunks))
    self.assertTrue(f.write.called)
    self.assertTrue(f.flush.called)
    self.assertTrue(f.close.called)
    self.assertEqual(2, len(dlprogress_mock.call_args_list))



  @patch("os.stat")
  @patch("os.path.isfile")
  @patch("os.path.exists")
  @patch.object(ambari_server, "track_jdk")
  @patch.object(ambari_server, "get_YN_input")
  @patch.object(ambari_server, "run_os_command")
  @patch.object(ambari_server, "write_property")
  @patch.object(ambari_server, "print_info_msg")
  @patch.object(ambari_server, "get_JAVA_HOME")
  @patch.object(ambari_server, "get_ambari_properties")
  def test_download_jdk(self, get_ambari_properties_mock,
                        get_JAVA_HOME_mock, print_info_msg_mock,
                        write_property_mock, run_os_command_mock,
                        get_YN_input_mock, track_jdk_mock, path_existsMock,
                        path_isfileMock, statMock):


    out = StringIO.StringIO()
    sys.stdout = out

    args = MagicMock()
    args.java_home = "somewhere"
    path_existsMock.return_value = False
    get_JAVA_HOME_mock.return_value = False
    get_ambari_properties_mock.return_value = -1

    rcode = ambari_server.download_jdk(args)

    self.assertEqual(-1, rcode)
    self.assertTrue(get_ambari_properties_mock.called)

    #search_file_mock.return_value = "something"
    get_JAVA_HOME_mock.return_value = True
    path_existsMock.return_value = True
    rcode = ambari_server.download_jdk(args)
    self.assertEqual(0, rcode)

    get_JAVA_HOME_mock.return_value = False
    rcode = ambari_server.download_jdk(args)
    self.assertEqual(0, rcode)
    self.assertTrue(write_property_mock.called)

    path_existsMock.return_value = False
    p = MagicMock()
    get_ambari_properties_mock.return_value = p
    p.__getitem__.side_effect = KeyError("test exception")
    rcode = ambari_server.download_jdk(args)
    self.assertEqual(-1, rcode)

    p.__getitem__.return_value = "somewhere"
    p.__getitem__.side_effect = None
    path_existsMock.return_value = False
    run_os_command_mock.return_value = (0, "Wrong out", None)
    rcode = ambari_server.download_jdk(args)
    self.assertEqual(-1, rcode)

    ambari_server.JDK_INSTALL_DIR = os.getcwd()
    get_YN_input_mock.return_value = True
    run_os_command_mock.return_value = (0, "Creating jdk-1.2/jre"
                                           "Content-Length: 32000\r\n"
                                           , None)
    statResult = MagicMock()
    statResult.st_size = 32000
    statMock.return_value = statResult
    rcode = ambari_server.download_jdk(args)
    self.assertEqual(0, rcode)

    sys.stdout = sys.__stdout__



  @patch.object(ambari_server, "run_os_command")
  def test_get_postgre_status(self, run_os_command_mock):

    run_os_command_mock.return_value = (1, "running", None)
    result = ambari_server.get_postgre_status()
    self.assertEqual("running", result)

    run_os_command_mock.return_value = (1, "wrong", None)
    result = ambari_server.get_postgre_status()
    self.assertEqual(None, result)


  @patch("subprocess.Popen")
  @patch.object(ambari_server, 'run_os_command')
  @patch.object(ambari_server, "get_postgre_status")
  def test_check_postgre_up(self, get_postgre_status_mock,
                            run_os_command_method, popen_mock):

    out = StringIO.StringIO()
    sys.stdout = out

    p = MagicMock()
    p.poll.return_value = 0
    popen_mock.return_value = p
    get_postgre_status_mock.return_value = ambari_server.PG_STATUS_RUNNING
    run_os_command_method.return_value = (0, None, None)

    rcode = ambari_server.check_postgre_up()
    self.assertEqual(0, rcode)

    get_postgre_status_mock.return_value = None
    p.poll.return_value = 4
    rcode = ambari_server.check_postgre_up()
    self.assertEqual(4, rcode)

    sys.stdout = sys.__stdout__



  @patch("platform.linux_distribution")
  @patch("platform.system")
  @patch.object(ambari_server, "print_info_msg")
  @patch.object(ambari_server, "print_error_msg")
  @patch.object(ambari_server, "get_ambari_properties")
  @patch.object(ambari_server, "write_property")
  @patch.object(ambari_server, "get_conf_dir")
  def test_configure_os_settings(self, get_conf_dir_mock, write_property_mock, get_ambari_properties_mock, print_error_msg_mock,
                                 print_info_msg_mock, systemMock, distMock):


    get_ambari_properties_mock.return_value = -1
    rcode = ambari_server.configure_os_settings()
    self.assertEqual(-1, rcode)
    
    p = MagicMock()
    p[ambari_server.OS_TYPE_PROPERTY] = 'somevalue'
    get_ambari_properties_mock.return_value = p
    rcode = ambari_server.configure_os_settings()
    self.assertEqual(0, rcode)

    p.__getitem__.return_value = ""
    systemMock.return_value = "NonLinux"
    rcode = ambari_server.configure_os_settings()
    self.assertEqual(-1, rcode)

    systemMock.return_value = "Linux"
    distMock.return_value = ("CentOS", "6.3", None)
    rcode = ambari_server.configure_os_settings()
    self.assertEqual(0, rcode)
    self.assertTrue(write_property_mock.called)



  @patch("__builtin__.open")
  @patch.object(ambari_server, "Properties")
  @patch.object(ambari_server, "search_file")
  @patch.object(ambari_server, "get_conf_dir")
  def test_get_JAVA_HOME(self, get_conf_dir_mock, search_file_mock,
                         Properties_mock, openMock):


    out = StringIO.StringIO()
    sys.stdout = out

    openMock.side_effect = Exception("exception")
    result = ambari_server.get_JAVA_HOME()
    self.assertEqual(None, result)

    expected = os.path.dirname(__file__)
    p = MagicMock()
    p.__getitem__.return_value = expected
    openMock.side_effect = None
    Properties_mock.return_value = p
    result = ambari_server.get_JAVA_HOME()
    self.assertEqual(expected, result)

    sys.stdout = sys.__stdout__



  @patch("glob.glob")
  @patch.object(ambari_server, "get_JAVA_HOME")
  def test_find_jdk(self, get_JAVA_HOME_mock, globMock):


    out = StringIO.StringIO()
    sys.stdout = out

    get_JAVA_HOME_mock.return_value = "somewhere"
    result = ambari_server.find_jdk()
    self.assertEqual("somewhere", result)

    get_JAVA_HOME_mock.return_value = None
    globMock.return_value = []
    result = ambari_server.find_jdk()
    self.assertEqual(None, result)

    globMock.return_value = ["one", "two"]
    result = ambari_server.find_jdk()
    self.assertNotEqual(None, result)

    sys.stdout = sys.__stdout__

  @patch.object(ambari_server, "get_YN_input")
  @patch.object(ambari_server, "configure_os_settings")
  @patch.object(ambari_server, "download_jdk")
  @patch.object(ambari_server, "configure_postgres")
  @patch.object(ambari_server, "setup_db")
  @patch.object(ambari_server, "check_postgre_up")
  @patch.object(ambari_server, "check_iptables")
  @patch.object(ambari_server, "check_selinux")
  @patch.object(ambari_server, "setup_remote_db")
  @patch.object(ambari_server, "store_remote_properties")
  def test_setup(self, store_remote_properties_mock, setup_remote_db_mock, check_selinux_mock, check_iptables_mock,
                 check_postgre_up_mock, setup_db_mock, configure_postgres_mock,
                 download_jdk_mock, configure_os_settings_mock, get_YN_input ):


    
    out = StringIO.StringIO()
    sys.stdout = out
    args = MagicMock()

    check_selinux_mock.return_value = 0
    check_iptables_mock.return_value = (0, "other")
    check_postgre_up_mock.return_value = 0
    setup_db_mock.return_value = 0
    get_YN_input.return_value = False
    setup_remote_db_mock.return_value = 0
    configure_postgres_mock.return_value = 0
    download_jdk_mock.return_value = 0
    configure_os_settings_mock.return_value = 0
    store_remote_properties_mock.return_value = 0
    result = ambari_server.setup(args)
    self.assertEqual(None, result)

    sys.stdout = sys.__stdout__
    

  @patch.object(ambari_server, "get_YN_input")
  @patch.object(ambari_server, "setup_db")
  @patch.object(ambari_server, "print_info_msg")
  @patch.object(ambari_server, "run_os_command")
  @patch.object(ambari_server, "configure_database_username_password")
  @patch.object(ambari_server, "parse_properties_file")
  def test_reset(self, parse_properties_file_mock, configure_database_username_password_mock,
                 run_os_command_mock, print_info_msg_mock,
                 setup_db_mock, get_YN_inputMock):


    out = StringIO.StringIO()
    sys.stdout = out
    parse_properties_file_mock.return_value = 0

    args = MagicMock()
    args.persistence_type = "local"
    get_YN_inputMock.return_value = False
    rcode = ambari_server.reset(args)
    self.assertEqual(-1, rcode)

    get_YN_inputMock.return_value = True
    run_os_command_mock.return_value = (1, None, None)
    rcode = ambari_server.reset(args)
    self.assertEqual(1, rcode)

    run_os_command_mock.return_value = (0, None, None)
    rcode = ambari_server.reset(args)
    self.assertEqual(None, rcode)
    self.assertTrue(setup_db_mock.called)

    sys.stdout = sys.__stdout__



  @patch.object(ambari_server, "setup_db")
  @patch.object(ambari_server, "print_info_msg")
  @patch.object(ambari_server, "run_os_command")
  @patch.object(ambari_server, "parse_properties_file")
  def test_silent_reset(self, parse_properties_file_mock,
                 run_os_command_mock, print_info_msg_mock,
                 setup_db_mock):

    out = StringIO.StringIO()
    sys.stdout = out

    args = MagicMock()
    ambari_server.SILENT = True
    self.assertTrue(ambari_server.SILENT)
    run_os_command_mock.return_value = (0, None, None)

    def signal_handler(signum, frame):
       self.fail("Timed out!")

    signal.signal(signal.SIGALRM, signal_handler)
    signal.alarm(5)
    rcode = ambari_server.reset(args)
    
    self.assertEqual(None, rcode)
    self.assertTrue(setup_db_mock.called)
    
    sys.stdout = sys.__stdout__



  @patch("os.kill")
  @patch("os.path.exists")
  @patch("__builtin__.open")
  @patch("subprocess.Popen")
  @patch.object(ambari_server, "print_info_msg")
  @patch.object(ambari_server, "get_conf_dir")
  @patch.object(ambari_server, "find_jdk")
  @patch.object(ambari_server, "print_error_msg")
  @patch.object(ambari_server, "check_postgre_up")
  @patch.object(ambari_server, "check_iptables")
  @patch.object(ambari_server, "parse_properties_file")
  def test_start(self, parse_properties_file_mock, check_iptables_mock, check_postgre_up_mock,
                 print_error_msg_mock, find_jdk_mock, get_conf_dir_mock,
                 print_info_msg_mock, popenMock, openMock, pexistsMock,
                 killMock):


    
    out = StringIO.StringIO()
    sys.stdout = out
    args = MagicMock()
    f = MagicMock()
    f.readline.return_value = 42
    openMock.return_value = f
    pexistsMock.return_value = True
    rcode = ambari_server.start(args)
    self.assertTrue(killMock.called)
    self.assertEqual(None, rcode)

    pexistsMock.return_value = False
    find_jdk_mock.return_value = None
    rcode = ambari_server.start(args)
    self.assertEqual(-1, rcode)

    find_jdk_mock.return_value = "somewhere"
    args.persistence_type="remote"
    check_postgre_up_mock.return_value = 0
    check_iptables_mock.return_value = (0, None)
    p = MagicMock()
    popenMock.return_value = p
    rcode = ambari_server.start(args)
    self.assertEqual(None, rcode)
    self.assertFalse(check_postgre_up_mock.called)
    self.assertTrue(f.write.called)

    args.persistence_type="local"
    rcode = ambari_server.start(args)
    self.assertEqual(None, rcode)
    self.assertTrue(check_postgre_up_mock.called)

    sys.stdout = sys.__stdout__



  @patch("__builtin__.open")
  @patch("os.path.exists")
  @patch("os.remove")
  @patch("os.killpg")
  @patch("os.getpgid")
  @patch.object(ambari_server, "print_info_msg")
  def test_stop(self, print_info_msg_mock, gpidMock, removeMock,
                killMock, pexistsMock, openMock):


    pexistsMock.return_value = True
    f = MagicMock()
    f.readline.return_value = "42"
    openMock.return_value = f

    out = StringIO.StringIO()
    sys.stdout = out

    ambari_server.stop(None)

    self.assertTrue(f.readline.called)
    self.assertTrue(killMock.called)
    self.assertTrue(killMock.called)
    self.assertTrue(f.close.called)
    self.assertTrue(removeMock.called)

    sys.stdout = sys.__stdout__



  def test_print_info_msg(self):


    out = StringIO.StringIO()
    sys.stdout = out

    ambari_server.VERBOSE = True
    ambari_server.print_info_msg("msg")
    self.assertNotEqual("", out.getvalue())

    sys.stdout = sys.__stdout__



  def test_print_error_msg(self):


    out = StringIO.StringIO()
    sys.stdout = out

    ambari_server.VERBOSE = True
    ambari_server.print_error_msg("msg")
    self.assertNotEqual("", out.getvalue())

    sys.stdout = sys.__stdout__



  def test_print_warning_msg(self):


    out = StringIO.StringIO()
    sys.stdout = out

    ambari_server.VERBOSE = True
    ambari_server.print_warning_msg("msg")
    self.assertNotEqual("", out.getvalue())

    sys.stdout = sys.__stdout__



  @patch.object(ambari_server, "get_choice_string_input")
  def test_get_YN_input(self, get_choice_string_input_mock):


    ambari_server.get_YN_input("prompt", "default")
    self.assertTrue(get_choice_string_input_mock.called)
    self.assertEqual(4, len(get_choice_string_input_mock.call_args_list[0][0]))

  @patch("sys.exit")
  @patch.object(ambari_server, "get_YN_input")
  @patch.object(ambari_server, "download_jdk")
  @patch.object(ambari_server, "get_db_cli_tool")
  @patch.object(ambari_server, "store_remote_properties")
  @patch.object(ambari_server, "is_local_database")
  @patch.object(ambari_server, "check_iptables")
  @patch.object(ambari_server, "check_jdbc_drivers")
  @patch('__builtin__.raw_input') 
  def test_setup_remote_db_wo_client(self, raw_input, check_jdbc_drivers_mock, check_iptables_mock, is_local_db_mock,
                                     store_remote_properties_mock, get_db_cli_tool_mock, download_jdk_mock, 
                                     get_YN_input, exit_mock):

    
    out = StringIO.StringIO()
    sys.stdout = out
    raw_input.return_value =""
    args = MagicMock()
    get_YN_input.return_value = False
    is_local_db_mock.return_value = False
    check_iptables_mock.return_value = (0, "other")
    store_remote_properties_mock.return_value = 0
    get_db_cli_tool_mock.return_value = None
    check_jdbc_drivers_mock.return_value=0

    failed = False
    result = None

    try:
      result = ambari_server.setup(args)
    except Exception:
      failed = True

    self.assertEqual(False, failed)
    self.assertEqual(None, result)
    self.assertEqual(True, exit_mock.called)

    sys.stdout = sys.__stdout__

  @patch.object(ambari_server, "parse_properties_file")
  @patch.object(ambari_server, "get_db_cli_tool")
  @patch.object(ambari_server, "print_error_msg")
  @patch.object(ambari_server, "get_YN_input")
  @patch.object(ambari_server, "setup_db")
  @patch.object(ambari_server, "run_os_command")
  def test_reset_remote_db_wo_client(self, run_os_command_mock, setup_db_mock,
                                     get_YN_inputMock, print_error_msg_mock, get_db_cli_tool_mock, parse_properties_file_mock):

    out = StringIO.StringIO()
    sys.stdout = out

    args = MagicMock()
    get_YN_inputMock.return_value = True
    run_os_command_mock.return_value = (0, None, None)
    args.persistence_type="remote"
    get_db_cli_tool_mock.return_value = None
    rcode = ambari_server.reset(args)
    self.assertEqual(-1, rcode)

    sys.stdout = sys.__stdout__


  @patch.object(ambari_server, "get_ambari_properties")
  @patch.object(ambari_server, "find_jdbc_driver")
  @patch.object(ambari_server, "copy_files")
  @patch.object(ambari_server, "print_error_msg")
  @patch.object(ambari_server, "print_warning_msg")
  @patch('__builtin__.raw_input')
  @patch("sys.exit")
  def test_check_jdbc_drivers(self, exit_mock, raw_input_mock, print_warning_msg, print_error_msg_mock, copy_files_mock,
                              find_jdbc_driver_mock, get_ambari_properties_mock):


    out = StringIO.StringIO()
    sys.stdout = out

    args = MagicMock()
    
    # Check positive scenario
    drivers_list = ['driver_file']
    resources_dir = '/tmp'
    
    get_ambari_properties_mock.return_value = {ambari_server.RESOURCES_DIR_KEY : resources_dir}
    find_jdbc_driver_mock.return_value = drivers_list
    
    args.database = "oracle"
    
    rcode = ambari_server.check_jdbc_drivers(args)
    
    self.assertEqual(0, rcode)
    copy_files_mock.assert_called_with(drivers_list, resources_dir)
    
    # Check negative scenarios
    # Silent option, no drivers
    ambari_server.SILENT = True
    
    find_jdbc_driver_mock.return_value = -1
    
    rcode = ambari_server.check_jdbc_drivers(args)
    
    self.assertTrue(print_error_msg_mock.called)
    self.assertTrue(exit_mock.called)
    
    # Non-Silent option, no drivers
    ambari_server.SILENT = False
    
    find_jdbc_driver_mock.return_value = -1
    
    rcode = ambari_server.check_jdbc_drivers(args)
    
    self.assertTrue(exit_mock.called)
    self.assertTrue(print_error_msg_mock.called)
    
    # Non-Silent option, no drivers at first ask, present drivers after that
    
    find_jdbc_driver_mock.side_effect = [-1, drivers_list]
    
    rcode = ambari_server.check_jdbc_drivers(args)
    
    self.assertEqual(0, rcode)
    copy_files_mock.assert_called_with(drivers_list, resources_dir)
    
    # Non-Silent option, no drivers at first ask, present drivers after that
    find_jdbc_driver_mock.reset()
    find_jdbc_driver_mock.side_effect = [-1, -1]
    
    rcode = ambari_server.check_jdbc_drivers(args)
    
    self.assertTrue(exit_mock.called)
    self.assertTrue(print_error_msg_mock.called)
    
    
    sys.stdout = sys.__stdout__
    
    
  @patch.object(ambari_server, "search_file")
  def test_get_ambari_properties(self, search_file_mock):


    search_file_mock.return_value = None
    rcode = ambari_server.get_ambari_properties()
    self.assertEqual(rcode, -1)
  
    tf1 = tempfile.NamedTemporaryFile()
    search_file_mock.return_value = tf1.name
    prop_name='name'
    prop_value='val'
    
    with open(tf1.name, 'w') as fout:
      fout.write(prop_name + '=' + prop_value)
    fout.close()

    properties = ambari_server.get_ambari_properties()
    
    self.assertEqual(properties[prop_name], prop_value)
    self.assertEqual(properties.fileName, os.path.abspath(tf1.name))
    
    sys.stdout = sys.__stdout__

  @patch.object(ambari_server, "search_file")
  def test_parse_properties_file(self, search_file_mock):

    
    tf1 = tempfile.NamedTemporaryFile()
    search_file_mock.return_value = tf1.name

    args = MagicMock()
    ambari_server.parse_properties_file(args)
    self.assertEquals(args.persistence_type, "local")


    with open(tf1.name, 'w') as fout:
      fout.write("\n")
      fout.write(ambari_server.PERSISTENCE_TYPE_PROPERTY+"=remote")

    args = MagicMock()

    ambari_server.parse_properties_file(args)
    self.assertEquals(args.persistence_type, "remote")

    sys.stdout = sys.__stdout__

  def test_prompt_db_properties_default(self):

    args = MagicMock()
    ambari_server.load_default_db_properties(args)
    ambari_server.prompt_db_properties(args)
    self.assertEqual(args.database, "postgres")
    self.assertEqual(args.database_host, "localhost")
    self.assertEqual(args.database_name, "ambari")
    self.assertEqual(args.database_port, "5432")

  @patch.object(ambari_server, "read_password")
  @patch.object(ambari_server, "get_validated_string_input")
  @patch.object(ambari_server, "get_YN_input")
  def test_prompt_db_properties_oracle_sname(self, gyni_mock, gvsi_mock, rp_mock):

    ambari_server.PROMPT_DATABASE_OPTIONS = True
    gyni_mock.return_value = True
    list_of_return_values= ["ambari-server", "ambari", "1", "1521", "localhost", "2"]
    def side_effect(*args, **kwargs):
      return list_of_return_values.pop()
    gvsi_mock.side_effect = side_effect
    rp_mock.return_value = "password"

    args = MagicMock()
    ambari_server.load_default_db_properties(args)
    ambari_server.prompt_db_properties(args)
    self.assertEqual(args.database, "oracle")
    self.assertEqual(args.database_port, "1521")
    self.assertEqual(args.database_host, "localhost")
    self.assertEqual(args.database_name, "ambari")
    self.assertEqual(args.database_username, "ambari-server")
    self.assertEqual(args.sid_or_sname, "sname")

  @patch.object(ambari_server, "read_password")
  @patch.object(ambari_server, "get_validated_string_input")
  @patch.object(ambari_server, "get_YN_input")
  def test_prompt_db_properties_oracle_sid(self, gyni_mock, gvsi_mock, rp_mock):

    ambari_server.PROMPT_DATABASE_OPTIONS = True
    gyni_mock.return_value = True
    list_of_return_values= ["ambari-server", "ambari", "2", "1521", "localhost", "2"]
    def side_effect(*args, **kwargs):
      return list_of_return_values.pop()
    gvsi_mock.side_effect = side_effect
    rp_mock.return_value = "password"

    args = MagicMock()
    ambari_server.load_default_db_properties(args)
    ambari_server.prompt_db_properties(args)
    self.assertEqual(args.database, "oracle")
    self.assertEqual(args.database_port, "1521")
    self.assertEqual(args.database_host, "localhost")
    self.assertEqual(args.database_name, "ambari")
    self.assertEqual(args.database_username, "ambari-server")
    self.assertEqual(args.sid_or_sname, "sid")

  @patch.object(ambari_server, "read_password")
  @patch.object(ambari_server, "get_validated_string_input")
  @patch.object(ambari_server, "get_YN_input")
  def test_prompt_db_properties_postgre_adv(self, gyni_mock, gvsi_mock, rp_mock):

    ambari_server.PROMPT_DATABASE_OPTIONS = True
    gyni_mock.return_value = True
    list_of_return_values= ["ambari-server", "ambari", "1"]
    def side_effect(*args, **kwargs):
      return list_of_return_values.pop()
    gvsi_mock.side_effect = side_effect
    rp_mock.return_value = "password"

    args = MagicMock()
    ambari_server.load_default_db_properties(args)
    ambari_server.prompt_db_properties(args)
    self.assertEqual(args.database, "postgres")
    self.assertEqual(args.database_port, "5432")
    self.assertEqual(args.database_host, "localhost")
    self.assertEqual(args.database_name, "ambari")
    self.assertEqual(args.database_username, "ambari-server")
    self.assertEqual(args.sid_or_sname, "sname")

  def get_sample(self, sample):
    """
    Returns sample file content as string with normalized line endings
    """
    path = self.get_samples_dir(sample)
    return self.get_file_string(path)

  def get_file_string(self, file):
    """
    Returns file content as string with normalized line endings
    """
    string = open(file, 'r').read()
    return self.normalize(string)



  def normalize(self, string):
    """
    Normalizes line ending in string according to platform-default encoding
    """
    return string.replace("\n", os.linesep)



  def get_samples_dir(self, sample):
    """
    Returns full file path by sample name
    """
    testdir = os.path.dirname(__file__)
    return os.path.dirname(testdir) + os.sep + "resources" + os.sep \
           + 'TestAmbaryServer.samples/' + sample
