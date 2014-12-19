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

from stacks.utils.RMFTestCase import *

import StringIO
import re
from unittest import TestCase
import sys
from mock.mock import patch, MagicMock, create_autospec
import os, errno, tempfile
import signal
import stat
import datetime
import operator
import json
from optparse import OptionParser
import platform
import shutil
from pwd import getpwnam
from ambari_server.resourceFilesKeeper import ResourceFilesKeeper, KeeperException
from ambari_server import BackupRestore

from ambari_commons import OSCheck, OSConst, Firewall, FirewallChecks
from ambari_commons.exceptions import *
from ambari_commons.os_linux import *
from ambari_commons.os_utils import *
from ambari_commons.inet_utils import *

# We have to use this import HACK because the filename contains a dash
with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  with patch("os.symlink"):
    with patch("__builtin__.open"):
      with patch("glob.glob", return_value = ['/etc/init.d/postgresql-9.3']):
        ambari__server = __import__('ambari-server')

with patch("platform.linux_distribution", return_value = ('Suse','11','Final')):
  with patch("os.symlink"):
    with patch("__builtin__.open"):
      with patch("glob.glob", return_value = ['/etc/init.d/postgresql-9.3']):
        from ambari_server.dbConfiguration import *
        from ambari_server.dbConfiguration_linux import *
        from ambari_server.properties import *
        from ambari_server.serverConfiguration import *
        from ambari_server.serverSetup import *
        from ambari_server.userInput import *

CURR_AMBARI_VERSION = "2.0.0"

db_YN_Inputs_test_ambariServerSetupWithCustomDbName_call_count = 0

def db_YN_Inputs_ret_test_ambariServerSetupWithCustomDbName(*args, **kwargs):
  global db_YN_Inputs_test_ambariServerSetupWithCustomDbName_call_count
  if db_YN_Inputs_test_ambariServerSetupWithCustomDbName_call_count == 0:
    db_YN_Inputs_test_ambariServerSetupWithCustomDbName_call_count += 1
    return False
  else:
    return True


class TestAmbariServer(TestCase):
  def setUp(self):
    out = StringIO.StringIO()
    sys.stdout = out


  def tearDown(self):
    sys.stdout = sys.__stdout__

  @patch("ambari_server.dbConfiguration_linux.run_os_command")
  def test_configure_pg_hba_ambaridb_users(self, run_os_command_method):
    # Prepare mocks
    run_os_command_method.return_value = (0, "", "")
    database_username = "ffdf"
    tf1 = tempfile.NamedTemporaryFile()
    # Run test
    PGConfig._configure_pg_hba_ambaridb_users(tf1.name, database_username)
    # Check results
    self.assertTrue(run_os_command_method.called)
    string_expected = self.get_file_string(self.get_samples_dir("configure_pg_hba_ambaridb_users1"))
    string_actual = self.get_file_string(tf1.name)
    self.assertEquals(string_expected, string_actual)
    pass

  @patch('__builtin__.raw_input')
  def test_servicename_regex(self, raw_input_method):

    ''' Test to make sure the service name can contain digits '''
    ambari__server.SILENT = False
    raw_input_method.return_value = "OT100"
    result = OracleConfig._get_validated_service_name("ambari", 1)
    self.assertEqual("OT100", result, "Not accepting digits")
    pass

  @patch('__builtin__.raw_input')
  def test_dbname_regex(self, raw_input_method):

    ''' Test to make sure the service name can contain digits '''
    ambari__server.SILENT = False
    raw_input_method.return_value = "OT100"
    result = LinuxDBMSConfig._get_validated_db_name("Database", "ambari")
    self.assertEqual("OT100", result, "Not accepting digits")


  def test_configure_pg_hba_postgres_user(self):

    tf1 = tempfile.NamedTemporaryFile()
    PGConfig.PG_HBA_CONF_FILE = tf1.name

    with open(PGConfig.PG_HBA_CONF_FILE, 'w') as fout:
      fout.write("\n")
      fout.write("local  all  all md5\n")
      fout.write("host  all   all 0.0.0.0/0  md5\n")
      fout.write("host  all   all ::/0 md5\n")

    PGConfig._configure_pg_hba_postgres_user()

    expected = self.get_file_string(self.get_samples_dir(
      "configure_pg_hba_ambaridb_users2"))
    result = self.get_file_string(PGConfig.PG_HBA_CONF_FILE)
    self.assertEqual(expected, result, "pg_hba_conf not processed")

    mode = oct(os.stat(PGConfig.PG_HBA_CONF_FILE)[stat.ST_MODE])
    str_mode = str(mode)[-4:]
    self.assertEqual("0644", str_mode, "Wrong file permissions")


  @patch('__builtin__.raw_input')
  def get_choice_string_input(self, raw_input_method):
    prompt = "blablabla"
    default = "default blablabla"
    firstChoice = set(['yes', 'ye', 'y'])
    secondChoice = set(['no', 'n'])
    # test first input
    raw_input_method.return_value = "Y"

    result = ambari__server.get_choice_string_input(prompt, default,
                                                   firstChoice, secondChoice)
    self.assertEquals(result, True)
    raw_input_method.reset_mock()
    # test second input

    raw_input_method.return_value = "N"

    result = ambari__server.get_choice_string_input(prompt, default,
                                                   firstChoice, secondChoice)
    self.assertEquals(result, False)

    raw_input_method.reset_mock()

    # test enter pressed

    raw_input_method.return_value = ""

    result = ambari__server.get_choice_string_input(prompt, default,
                                                   firstChoice, secondChoice)
    self.assertEquals(result, default)

    raw_input_method.reset_mock()

    # test wrong input
    list_of_return_values = ['yes', 'dsad', 'fdsfds']

    def side_effect(list):
      return list_of_return_values.pop()

    raw_input_method.side_effect = side_effect

    result = ambari__server.get_choice_string_input(prompt, default,
                                                   firstChoice, secondChoice)
    self.assertEquals(result, True)
    self.assertEquals(raw_input_method.call_count, 3)

    pass


  @patch('re.search')
  @patch('__builtin__.raw_input')
  @patch('getpass.getpass')
  def get_validated_string_input(self, get_pass_method,
                                 raw_input_method, re_search_method):
    prompt = "blabla"
    default = "default_pass"
    pattern = "pattern_pp"
    description = "blabla2"
    # check password input
    self.assertFalse(False, ambari__server.SILENT)
    is_pass = True
    get_pass_method.return_value = "dfdsfdsfds"

    result = ambari__server.get_validated_string_input(prompt, default,
                                                      pattern, description, is_pass)

    self.assertEquals(get_pass_method.return_value, result)
    get_pass_method.assure_called_once(prompt)
    self.assertFalse(raw_input_method.called)

    # check raw input
    get_pass_method.reset_mock()
    raw_input_method.reset_mock()
    is_pass = False
    raw_input_method.return_value = "dkf90ewuf0"

    result = ambari__server.get_validated_string_input(prompt, default,
                                                      pattern, description, is_pass)

    self.assertEquals(raw_input_method.return_value, result)
    self.assertFalse(get_pass_method.called)
    raw_input_method.assure_called_once(prompt)


  def test_get_pass_file_path(self):
    result = ambari__server.get_pass_file_path("/etc/ambari/conf_file")
    self.assertEquals("/etc/ambari/password.dat", result)
    pass


  @patch.object(ambari__server, 'setup_security')
  @patch('optparse.OptionParser')
  def test_main_test_setup_security(self, OptionParserMock,
                                    setup_security_method):
    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["setup-security"]
    opm.parse_args.return_value = (options, args)
    options.dbms = None
    options.sid_or_sname = "sid"
    setup_security_method.return_value = None

    ambari__server.main()

    ambari__server.main()
    self.assertTrue(setup_security_method.called)
    self.assertFalse(False, ambari__server.VERBOSE)
    self.assertFalse(False, ambari__server.SILENT)

  @patch.object(ambari__server, 'setup_ambari_krb5_jaas')
  @patch.object(ambari__server, 'setup_master_key')
  @patch.object(ambari__server, 'setup_component_https')
  @patch.object(ambari__server, 'setup_https')
  @patch.object(ambari__server, "get_validated_string_input")
  def test_setup_security(self, get_validated_string_input_mock, setup_https,
                          setup_component_https, setup_master_key,
                          setup_ambari_krb5_jaas):

    args = {}
    get_validated_string_input_mock.return_value = '1'
    ambari__server.setup_security(args)
    self.assertTrue(setup_https.called)

    get_validated_string_input_mock.return_value = '2'
    ambari__server.setup_security(args)
    self.assertTrue(setup_component_https.called)
    setup_component_https.assert_called_with("Ganglia", "setup-ganglia-https",
                          ambari__server.GANGLIA_HTTPS, "ganglia_cert")

    get_validated_string_input_mock.return_value = '3'
    ambari__server.setup_security(args)
    self.assertTrue(setup_master_key.called)

    get_validated_string_input_mock.return_value = '4'
    ambari__server.setup_security(args)
    self.assertTrue(setup_ambari_krb5_jaas.called)


  @patch('re.sub')
  @patch('fileinput.FileInput')
  @patch("ambari_server.userInput.get_validated_string_input")
  @patch.object(ambari__server, "search_file")
  @patch('os.path.exists')
  def test_setup_ambari_krb5_jaas(self, exists_mock, search_mock,
                                  get_validated_string_input_mock,
                                  fileinput_mock, re_sub_mock):

    search_mock.return_value = 'filepath'
    exists_mock.return_value = False

    # Negative case
    try:
      ambari__server.setup_ambari_krb5_jaas()
      self.fail("Should throw exception")
    except NonFatalException as fe:
      # Expected
      self.assertTrue("No jaas config file found at location" in fe.reason)
      pass

    # Positive case
    exists_mock.reset_mock()
    exists_mock.return_value = True
    get_validated_string_input_mock.side_effect = ['aaa@aaa.cnn',
                                                   'pathtokeytab']

    fileinput_mock.return_value = [ 'keyTab=xyz', 'principal=xyz' ]

    ambari__server.setup_ambari_krb5_jaas()

    self.assertTrue(fileinput_mock.called)
    self.assertTrue(re_sub_mock.called)
    self.assertTrue(re_sub_mock.call_args_list, [('aaa@aaa.cnn'),
                                                 ('pathtokeytab')])

  @patch.object(ambari__server, 'setup')
  @patch.object(ambari__server, 'start')
  @patch.object(ambari__server, 'stop')
  @patch.object(ambari__server, 'reset')
  @patch('optparse.OptionParser')
  def test_main_test_setup(self, OptionParserMock, reset_method, stop_method,
                           start_method, setup_method):
    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["setup"]
    opm.parse_args.return_value = (options, args)

    options.dbms = None
    options.sid_or_sname = "sid"
    ambari__server.main()

    self.assertTrue(setup_method.called)
    self.assertFalse(start_method.called)
    self.assertFalse(stop_method.called)
    self.assertFalse(reset_method.called)

    self.assertFalse(False, ambari__server.VERBOSE)
    self.assertFalse(False, ambari__server.SILENT)


  @patch.object(ambari__server, 'setup')
  @patch.object(ambari__server, 'start')
  @patch.object(ambari__server, 'stop')
  @patch.object(ambari__server, 'reset')
  @patch('optparse.OptionParser')
  def test_main_test_start(self, OptionParserMock, reset_method, stop_method,
                           start_method, setup_method):
    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["setup"]
    opm.parse_args.return_value = (options, args)

    options.dbms = None
    options.sid_or_sname = "sname"
    ambari__server.main()

    self.assertTrue(setup_method.called)
    self.assertFalse(start_method.called)
    self.assertFalse(stop_method.called)
    self.assertFalse(reset_method.called)

    self.assertFalse(False, ambari__server.VERBOSE)
    self.assertFalse(False, ambari__server.SILENT)


  @patch.object(ambari__server, 'setup')
  @patch.object(ambari__server, 'start')
  @patch.object(ambari__server, 'stop')
  @patch.object(ambari__server, 'reset')
  @patch('optparse.OptionParser')
  def test_main_test_start_debug_short(self, OptionParserMock, reset_method, stop_method,
                                       start_method, setup_method):
    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["start", "-g"]
    opm.parse_args.return_value = (options, args)

    options.dbms = None
    options.sid_or_sname = "sid"

    ambari__server.main()

    self.assertFalse(setup_method.called)
    self.assertTrue(start_method.called)
    self.assertFalse(stop_method.called)
    self.assertFalse(reset_method.called)

    self.assertTrue(ambari__server.SERVER_DEBUG_MODE)


  @patch.object(ambari__server, 'setup')
  @patch.object(ambari__server, 'start')
  @patch.object(ambari__server, 'stop')
  @patch.object(ambari__server, 'reset')
  @patch('optparse.OptionParser')
  def test_main_test_start_debug_long(self, OptionParserMock, reset_method, stop_method,
                                      start_method, setup_method):
    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["start", "--debug"]
    opm.parse_args.return_value = (options, args)
    options.dbms = None
    options.sid_or_sname = "sid"

    ambari__server.main()

    self.assertFalse(setup_method.called)
    self.assertTrue(start_method.called)
    self.assertFalse(stop_method.called)
    self.assertFalse(reset_method.called)

    self.assertTrue(ambari__server.SERVER_DEBUG_MODE)

  @patch.object(ambari__server, 'setup')
  @patch.object(ambari__server, 'start')
  @patch.object(ambari__server, 'stop')
  @patch.object(ambari__server, 'reset')
  @patch.object(ambari__server, 'backup')
  @patch.object(ambari__server, 'restore')
  @patch('optparse.OptionParser')
  def test_main_test_backup(self, OptionParserMock, restore_mock, backup_mock, reset_method, stop_method,
                           start_method, setup_method):
    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["backup"]
    opm.parse_args.return_value = (options, args)

    options.dbms = None
    options.sid_or_sname = "sname"
    ambari__server.main()

    self.assertTrue(backup_mock.called)
    self.assertFalse(restore_mock.called)
    self.assertFalse(setup_method.called)
    self.assertFalse(start_method.called)
    self.assertFalse(stop_method.called)
    self.assertFalse(reset_method.called)

    self.assertFalse(False, ambari__server.VERBOSE)
    self.assertFalse(False, ambari__server.SILENT)

  @patch.object(ambari__server, 'setup')
  @patch.object(ambari__server, 'start')
  @patch.object(ambari__server, 'stop')
  @patch.object(ambari__server, 'reset')
  @patch.object(ambari__server, 'backup')
  @patch.object(ambari__server, 'restore')
  @patch('optparse.OptionParser')
  def test_main_test_restore(self, OptionParserMock, restore_mock, backup_mock, reset_method, stop_method,
                            start_method, setup_method):
    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["restore"]
    opm.parse_args.return_value = (options, args)

    options.dbms = None
    options.sid_or_sname = "sname"
    ambari__server.main()

    self.assertTrue(restore_mock.called)
    self.assertFalse(backup_mock.called)
    self.assertFalse(setup_method.called)
    self.assertFalse(start_method.called)
    self.assertFalse(stop_method.called)
    self.assertFalse(reset_method.called)

    self.assertFalse(False, ambari__server.VERBOSE)
    self.assertFalse(False, ambari__server.SILENT)

  @patch.object(ambari__server, 'setup')
  @patch.object(ambari__server, 'start')
  @patch.object(ambari__server, 'stop')
  @patch.object(ambari__server, 'reset')
  @patch('optparse.OptionParser')
  def test_main_test_stop(self, OptionParserMock, reset_method, stop_method,
                          start_method, setup_method):
    opm = OptionParserMock.return_value
    options = MagicMock()
    args = ["stop"]
    opm.parse_args.return_value = (options, args)

    options.dbms = None
    options.sid_or_sname = "sid"

    ambari__server.main()

    self.assertFalse(setup_method.called)
    self.assertFalse(start_method.called)
    self.assertTrue(stop_method.called)
    self.assertFalse(reset_method.called)

    self.assertFalse(False, ambari__server.VERBOSE)
    self.assertFalse(False, ambari__server.SILENT)


  @patch.object(ambari__server, 'setup')
  @patch.object(ambari__server, 'start')
  @patch.object(ambari__server, 'stop')
  @patch.object(ambari__server, 'reset')
  @patch('optparse.OptionParser')
  def test_main_test_reset(self, OptionParserMock, reset_method, stop_method,
                           start_method, setup_method):
    opm = OptionParserMock.return_value

    options = MagicMock()
    args = ["reset"]
    opm.parse_args.return_value = (options, args)
    options.dbms = None
    options.sid_or_sname = "sid"

    ambari__server.main()

    self.assertFalse(setup_method.called)
    self.assertFalse(start_method.called)
    self.assertFalse(stop_method.called)
    self.assertTrue(reset_method.called)

    self.assertFalse(False, ambari__server.VERBOSE)
    self.assertFalse(False, ambari__server.SILENT)

  def test_configure_postgresql_conf(self):
    tf1 = tempfile.NamedTemporaryFile()
    PGConfig.POSTGRESQL_CONF_FILE = tf1.name

    with open(PGConfig.POSTGRESQL_CONF_FILE, 'w') as f:
      f.write("#listen_addresses = '127.0.0.1'        #\n")
      f.write("#listen_addresses = '127.0.0.1'")

    PGConfig._configure_postgresql_conf()

    expected = self.get_file_string(self.get_samples_dir(
      "configure_postgresql_conf1"))
    result = self.get_file_string(PGConfig.POSTGRESQL_CONF_FILE)
    self.assertEqual(expected, result, "postgresql.conf not updated")

    mode = oct(os.stat(PGConfig.POSTGRESQL_CONF_FILE)[stat.ST_MODE])
    str_mode = str(mode)[-4:]
    self.assertEqual("0644", str_mode, "Wrong file permissions")


  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.dbConfiguration_linux.PGConfig._restart_postgres")
  @patch("ambari_server.dbConfiguration_linux.PGConfig._get_postgre_status")
  @patch("ambari_server.dbConfiguration_linux.PGConfig._configure_postgresql_conf")
  @patch("ambari_server.dbConfiguration_linux.run_os_command")
  def test_configure_postgres(self,
                              run_os_command_mock,
                              configure_postgresql_conf_mock,
                              get_postgre_status_mock,
                              restart_postgres_mock,
                              get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    args = MagicMock()
    properties = Properties()

    args.dbms_index = 0

    del args.dbms
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password
    del args.silent

    factory = DBMSConfigFactory()
    dbConfig = factory.create(args, properties)

    self.assertTrue(dbConfig.dbms, "postgres")
    self.assertTrue(dbConfig.persistence_type, "local")

    tf1 = tempfile.NamedTemporaryFile()
    tf2 = tempfile.NamedTemporaryFile()

    PGConfig.PG_HBA_CONF_FILE = tf1.name
    PGConfig.PG_HBA_CONF_FILE_BACKUP = tf2.name

    out = StringIO.StringIO()
    sys.stdout = out
    retcode, out1, err = dbConfig._configure_postgres()
    sys.stdout = sys.__stdout__
    self.assertEqual(0, retcode)
    self.assertEqual("Backup for pg_hba found, reconfiguration not required\n",
                     out.getvalue())
    tf2.close()

    get_postgre_status_mock.return_value = PGConfig.PG_STATUS_RUNNING, 0, "", ""
    run_os_command_mock.return_value = 0, "", ""
    restart_postgres_mock.return_value = 0, "", ""

    rcode, out, err = dbConfig._configure_postgres()

    self.assertTrue(os.path.isfile(PGConfig.PG_HBA_CONF_FILE_BACKUP),
                    "postgresql.conf backup not created")
    self.assertTrue(run_os_command_mock.called)
    mode = oct(os.stat(PGConfig.PG_HBA_CONF_FILE)[stat.ST_MODE])
    str_mode = str(mode)[-4:]
    self.assertEqual("0644", str_mode, "Wrong file permissions")
    self.assertTrue(configure_postgresql_conf_mock.called)
    self.assertEqual(0, rcode)

    os.unlink(PGConfig.PG_HBA_CONF_FILE_BACKUP)

    get_postgre_status_mock.return_value = "stopped", 0, "", ""
    rcode, out, err = dbConfig._configure_postgres()
    self.assertEqual(0, rcode)
    os.unlink(PGConfig.PG_HBA_CONF_FILE_BACKUP)
    sys.stdout = sys.__stdout__


  @patch("time.sleep")
  @patch("subprocess.Popen")
  @patch("ambari_server.dbConfiguration_linux.run_os_command")
  @patch("ambari_server.dbConfiguration_linux.PGConfig._get_postgre_status")
  @patch("ambari_server.dbConfiguration_linux.print_info_msg")
  def test_restart_postgres(self, printInfoMsg_mock, get_postgre_status_mock,
                            run_os_command_mock, popenMock, sleepMock):
    p = MagicMock()
    p.poll.return_value = 0
    popenMock.return_value = p
    retcode, out, err = PGConfig._restart_postgres()
    self.assertEqual(0, retcode)

    p.poll.return_value = None
    get_postgre_status_mock.return_value = "stopped", 0, "", ""
    run_os_command_mock.return_value = (1, None, None)
    retcode, out, err = PGConfig._restart_postgres()
    self.assertEqual(1, retcode)


  @patch("shlex.split")
  @patch("subprocess.Popen")
  @patch("ambari_commons.os_linux.print_info_msg")
  def test_run_os_command(self, printInfoMsg_mock, popenMock, splitMock):

    p = MagicMock()
    p.communicate.return_value = (None, None)
    p.returncode = 3
    popenMock.return_value = p

    # with list arg
    cmd = ["exec", "arg"]
    run_os_command(cmd)
    self.assertFalse(splitMock.called)

    # with str arg
    resp = run_os_command("runme")
    self.assertEqual(3, resp[0])
    self.assertTrue(splitMock.called)


  @patch.object(ambari__server, "get_conf_dir")
  @patch.object(ambari__server, "search_file")
  def test_write_property(self, search_file_mock, get_conf_dir_mock):

    expected_content = "key1=val1\n"

    tf1 = tempfile.NamedTemporaryFile()
    search_file_mock.return_value = tf1.name
    ambari__server.write_property("key1", "val1")
    result = tf1.read()
    self.assertTrue(expected_content in result)


  #TODO Deprecate
  @patch.object(ambari__server, "configure_database_username_password")
  @patch.object(ambari__server, 'run_os_command')
  def test_setup_db_old(self, run_os_command_mock,
                    configure_database_username_password_mock):

    run_os_command_mock.return_value = (0, None, None)
    result = ambari__server.setup_db(MagicMock())
    self.assertTrue(configure_database_username_password_mock.called)
    self.assertEqual((0, None, None), result)

  #TODO Deprecate
  @patch.object(ambari__server, "configure_database_username_password")
  @patch("time.sleep")
  @patch.object(ambari__server, 'run_os_command')
  def test_setup_db_connect_attempts_fail_old(self, run_os_command_mock,
                                          sleep_mock, config_db_mock):
    run_os_command_mock.side_effect = [(1, "error", "error"), (1, "error", "error"),
                                       (1, "error", "error")]
    result = ambari__server.setup_db(MagicMock())
    self.assertTrue(run_os_command_mock.called)
    self.assertEqual((1, 'error', 'error') , result)
    self.assertEqual(2, sleep_mock.call_count)
    pass

  #TODO Deprecate
  @patch.object(ambari__server, "configure_database_username_password")
  @patch("time.sleep")
  @patch.object(ambari__server, 'run_os_command')
  def test_setup_db_connect_attempts_success_old(self, run_os_command_mock, sleep_mock,
                                             config_db_mock):
    run_os_command_mock.side_effect = [(1, "error", "error"), (0, None, None),
                                       (0, None, None)]
    result = ambari__server.setup_db(MagicMock())
    self.assertTrue(run_os_command_mock.called)
    self.assertEqual((0, None, None) , result)
    self.assertEqual(1, sleep_mock.call_count)
    pass


  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("time.sleep")
  @patch("ambari_server.dbConfiguration_linux.run_os_command")
  def test_setup_db(self, run_os_command_mock, sleep_mock,
                    get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    args = MagicMock()
    properties = Properties()

    args.dbms_index = 0

    del args.dbms
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password
    del args.silent

    run_os_command_mock.return_value = (0, None, None)

    factory = DBMSConfigFactory()
    dbConfig = factory.create(args, properties)

    self.assertTrue(dbConfig.dbms, "postgres")
    self.assertTrue(dbConfig.persistence_type, "local")

    result = dbConfig._setup_db()
    self.assertEqual((0, None, None), result)

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("time.sleep")
  @patch("ambari_server.dbConfiguration_linux.run_os_command")
  def test_setup_db_connect_attempts_fail(self, run_os_command_mock, sleep_mock,
                                          get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    args = MagicMock()
    properties = Properties()

    args.dbms_index = 0

    del args.dbms
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password
    del args.silent

    run_os_command_mock.side_effect = [(1, "error", "error"), (1, "error", "error"),
                                       (1, "error", "error")]

    factory = DBMSConfigFactory()
    dbConfig = factory.create(args, properties)

    self.assertTrue(dbConfig.dbms, "postgres")
    self.assertTrue(dbConfig.persistence_type, "local")

    result = dbConfig._setup_db()
    self.assertTrue(run_os_command_mock.called)
    self.assertEqual((1, 'error', 'error') , result)
    self.assertEqual(2, sleep_mock.call_count)
    pass

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("time.sleep")
  @patch("ambari_server.dbConfiguration_linux.run_os_command")
  def test_setup_db_connect_attempts_success(self, run_os_command_mock, sleep_mock,
                                             get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    args = MagicMock()
    properties = Properties()

    args.dbms_index = 0

    del args.dbms
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password
    del args.silent

    run_os_command_mock.side_effect = [(1, "error", "error"), (0, None, None),
                                       (0, None, None)]

    factory = DBMSConfigFactory()
    dbConfig = factory.create(args, properties)

    self.assertTrue(dbConfig.dbms, "postgres")
    self.assertTrue(dbConfig.persistence_type, "local")

    result = dbConfig._setup_db()
    self.assertTrue(run_os_command_mock.called)
    self.assertEqual((0, None, None) , result)
    self.assertEqual(1, sleep_mock.call_count)
    pass

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.serverSetup.get_YN_input")
  @patch("ambari_server.serverSetup.run_os_command")
  def test_disable_security_enhancements(self, run_os_command_mock, getYNInput_mock,
                                         get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    run_os_command_mock.return_value = (0, ambari__server.SE_STATUS_DISABLED,
                                        None)
    (rcode, err) = disable_security_enhancements()
    self.assertEqual(0, rcode)

    getYNInput_mock.return_value = True
    run_os_command_mock.return_value = (0, "enabled "
                                           + ambari__server.SE_MODE_ENFORCING,
                                        None)
    (rcode, err) = disable_security_enhancements()
    self.assertEqual(0, rcode)
    self.assertTrue(run_os_command_mock.called)
    self.assertTrue(getYNInput_mock.called)


  @patch("ambari_server.serverConfiguration.print_info_msg")
  def test_get_ambari_jars(self, printInfoMsg_mock):

    env = "/ambari/jars"
    os.environ[ambari__server.AMBARI_SERVER_LIB] = env
    result = get_ambari_jars()
    self.assertEqual(env, result)

    del os.environ[ambari__server.AMBARI_SERVER_LIB]
    result = get_ambari_jars()
    self.assertEqual("/usr/lib/ambari-server", result)
    self.assertTrue(printInfoMsg_mock.called)


  @patch("glob.glob")
  @patch("ambari_server.serverConfiguration.print_info_msg")
  def test_get_share_jars(self, printInfoMsg_mock, globMock):
    globMock.return_value = ["one", "two"]
    expected = "one:two:one:two"
    result = get_share_jars()
    self.assertEqual(expected, result)
    globMock.return_value = []
    expected = ""
    result = get_share_jars()
    self.assertEqual(expected, result)


  @patch("glob.glob")
  @patch("ambari_server.serverConfiguration.get_ambari_properties")
  @patch("ambari_server.serverConfiguration.print_info_msg")
  def test_get_ambari_classpath(self, printInfoMsg_mock, get_ambari_properties_mock, globMock):
    props = {}
    props[JDBC_DRIVER_PATH_PROPERTY] = "/driver"
    get_ambari_properties_mock.return_value = props
    globMock.return_value = ["one"]
    result = get_ambari_classpath()
    self.assertTrue(get_ambari_jars() in result)
    self.assertTrue("/driver" in result)
    self.assertTrue(get_share_jars() in result)
    globMock.return_value = []
    props[JDBC_DRIVER_PATH_PROPERTY] = ""
    result = get_ambari_classpath()
    self.assertTrue(get_ambari_jars() in result)
    self.assertFalse(":" in result)


  @patch("glob.glob")
  @patch("ambari_server.serverConfiguration.get_ambari_properties")
  @patch("ambari_server.serverConfiguration.print_info_msg")
  def test_get_full_ambari_classpath(self, printInfoMsg_mock, get_ambari_properties_mock, globMock):
    props = {}
    props[JDBC_DRIVER_PATH_PROPERTY] = "/driver"
    get_ambari_properties_mock.return_value = props
    globMock.return_value = ["one"]
    result = get_full_ambari_classpath()
    self.assertTrue(get_ambari_jars() in result)
    self.assertTrue(get_share_jars() in result)
    props[JDBC_DRIVER_PATH_PROPERTY] = "/driver dir"
    result = get_full_ambari_classpath()
    self.assertEqual(result[0], '"')
    self.assertEqual(result[len(result) - 1], '"')


  @patch("ambari_server.serverConfiguration.print_info_msg")
  def test_get_conf_dir(self, printInfoMsg_mock):
    env = "/dummy/ambari/conf"
    os.environ[ambari__server.AMBARI_CONF_VAR] = env
    result = get_conf_dir()
    self.assertEqual(env, result)

    del os.environ[ambari__server.AMBARI_CONF_VAR]
    result = get_conf_dir()
    self.assertEqual("/etc/ambari-server/conf", result)


  def test_search_file(self):

    path = os.path.dirname(__file__)
    result = search_file(__file__, path)
    expected = os.path.abspath(__file__)
    self.assertEqual(expected, result)

    result = search_file("non_existent_file", path)
    self.assertEqual(None, result)


  @patch("ambari_server.serverConfiguration.search_file")
  def test_find_properties_file(self, search_file_mock):
    # Testing case when file is not found
    search_file_mock.return_value = None
    try:
      find_properties_file()
      self.fail("File not found'")
    except FatalException:
      # Expected
      pass
    self.assertTrue(search_file_mock.called)

    # Testing case when file is found
    value = MagicMock()
    search_file_mock.return_value = value
    result = find_properties_file()
    self.assertTrue(result is value)


  @patch.object(ambari__server, "find_properties_file")
  @patch("__builtin__.open")
  @patch.object(ambari__server, "Properties")
  def test_read_ambari_user(self, properties_mock, open_mock, find_properties_file_mock):
    open_mock.return_value = "dummy"
    find_properties_file_mock.return_value = "dummy"
    # Testing with defined user
    properties_mock.return_value.__getitem__.return_value = "dummy_user"
    user = ambari__server.read_ambari_user()
    self.assertEquals(user, "dummy_user")
    # Testing with undefined user
    properties_mock.return_value.__getitem__.return_value = None
    user = ambari__server.read_ambari_user()
    self.assertEquals(user, None)


  @patch("os.path.exists")
  @patch("ambari_server.setupSecurity.set_file_permissions")
  @patch("ambari_server.setupSecurity.run_os_command")
  @patch("ambari_server.setupSecurity.get_ambari_properties")
  @patch("ambari_server.setupSecurity.get_value_from_properties")
  @patch("shutil.rmtree")
  @patch("os.makedirs")
  def test_adjust_directory_permissions(self, mkdir_mock, rmtree_mock,
                                        get_value_from_properties_mock, get_ambari_properties_mock,
                                        run_os_command_mock, set_file_permissions_mock, exists_mock):
    # Testing boostrap dir wipe
    properties_mock = MagicMock()
    get_value_from_properties_mock.return_value = "dummy_bootstrap_dir"
    adjust_directory_permissions("user")
    exists_mock.return_value = False
    self.assertTrue(set_file_permissions_mock.called)
    self.assertTrue(rmtree_mock.called)
    self.assertTrue(mkdir_mock.called)

    set_file_permissions_mock.reset_mock()
    # Test recursive calls
    old_list = configDefaults.NR_ADJUST_OWNERSHIP_LIST

    configDefaults.NR_ADJUST_OWNERSHIP_LIST = [
      ( "/etc/ambari-server/conf", "755", "{0}", True ),
      ( "/etc/ambari-server/conf/ambari.properties", "644", "{0}", False )
    ]

    adjust_directory_permissions("user")
    self.assertTrue(len(set_file_permissions_mock.call_args_list) ==
                    len(configDefaults.NR_ADJUST_OWNERSHIP_LIST))
    self.assertEquals(set_file_permissions_mock.call_args_list[0][0][3], True)
    self.assertEquals(set_file_permissions_mock.call_args_list[1][0][3], False)

    configDefaults.NR_ADJUST_OWNERSHIP_LIST = old_list


  @patch("os.path.exists")
  @patch("ambari_commons.os_linux.os_run_os_command")
  @patch("ambari_commons.os_linux.print_warning_msg")
  @patch("ambari_commons.os_utils.print_info_msg")
  def test_set_file_permissions(self, print_info_msg_mock, print_warning_msg_mock,
                                run_os_command_mock, exists_mock):

    # Testing not existent file scenario
    exists_mock.return_value = False
    set_file_permissions("dummy-file", "dummy-mod",
                                       "dummy-user", False)
    self.assertFalse(run_os_command_mock.called)
    self.assertTrue(print_info_msg_mock.called)

    run_os_command_mock.reset_mock()
    print_warning_msg_mock.reset_mock()

    # Testing OK scenario
    exists_mock.return_value = True
    run_os_command_mock.side_effect = [(0, "", ""), (0, "", "")]
    set_file_permissions("dummy-file", "dummy-mod",
                                       "dummy-user", False)
    self.assertTrue(len(run_os_command_mock.call_args_list) == 2)
    self.assertFalse(print_warning_msg_mock.called)

    run_os_command_mock.reset_mock()
    print_warning_msg_mock.reset_mock()

    # Testing first command fail
    run_os_command_mock.side_effect = [(1, "", ""), (0, "", "")]
    set_file_permissions("dummy-file", "dummy-mod",
                                       "dummy-user", False)
    self.assertTrue(len(run_os_command_mock.call_args_list) == 2)
    self.assertTrue(print_warning_msg_mock.called)

    run_os_command_mock.reset_mock()
    print_warning_msg_mock.reset_mock()

    # Testing second command fail
    run_os_command_mock.side_effect = [(0, "", ""), (1, "", "")]
    set_file_permissions("dummy-file", "dummy-mod",
                                       "dummy-user", False)
    self.assertTrue(len(run_os_command_mock.call_args_list) == 2)
    self.assertTrue(print_warning_msg_mock.called)

    run_os_command_mock.reset_mock()
    print_warning_msg_mock.reset_mock()

    # Testing recursive operation

    exists_mock.return_value = True
    run_os_command_mock.side_effect = [(0, "", ""), (0, "", "")]
    set_file_permissions("dummy-file", "dummy-mod",
                                       "dummy-user", True)
    self.assertTrue(len(run_os_command_mock.call_args_list) == 2)
    self.assertTrue("-R" in run_os_command_mock.call_args_list[0][0][0])
    self.assertTrue("-R" in run_os_command_mock.call_args_list[1][0][0])
    self.assertFalse(print_warning_msg_mock.called)

    run_os_command_mock.reset_mock()
    print_warning_msg_mock.reset_mock()

    # Testing non-recursive operation

    exists_mock.return_value = True
    run_os_command_mock.side_effect = [(0, "", ""), (0, "", "")]
    set_file_permissions("dummy-file", "dummy-mod",
                                       "dummy-user", False)
    self.assertTrue(len(run_os_command_mock.call_args_list) == 2)
    self.assertFalse("-R" in run_os_command_mock.call_args_list[0][0][0])
    self.assertFalse("-R" in run_os_command_mock.call_args_list[1][0][0])
    self.assertFalse(print_warning_msg_mock.called)

    run_os_command_mock.reset_mock()
    print_warning_msg_mock.reset_mock()


  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.serverSetup.get_validated_string_input")
  @patch("ambari_server.serverSetup.print_info_msg")
  @patch("ambari_server.serverSetup.print_warning_msg")
  @patch("ambari_server.serverSetup.run_os_command")
  def test_create_custom_user(self, run_os_command_mock, print_warning_msg_mock,
                              print_info_msg_mock, get_validated_string_input_mock,
                              get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    user = "dummy-user"
    get_validated_string_input_mock.return_value = user

    checks = AmbariUserChecks()

    # Testing scenario: absent user
    run_os_command_mock.side_effect = [(0, "", "")]
    result = checks._create_custom_user()
    self.assertFalse(print_warning_msg_mock.called)
    self.assertEquals(result, (0, user))

    print_info_msg_mock.reset_mock()
    print_warning_msg_mock.reset_mock()
    run_os_command_mock.reset_mock()

    # Testing scenario: existing user
    run_os_command_mock.side_effect = [(9, "", "")]
    result = checks._create_custom_user()
    self.assertTrue("User dummy-user already exists" in str(print_info_msg_mock.call_args_list[1][0]))
    self.assertEquals(result, (0, user))

    print_info_msg_mock.reset_mock()
    print_warning_msg_mock.reset_mock()
    run_os_command_mock.reset_mock()

    # Testing scenario: os command fail
    run_os_command_mock.side_effect = [(1, "", "")]
    result = checks._create_custom_user()
    self.assertTrue(print_warning_msg_mock.called)
    self.assertEquals(result, (1, None))


  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.serverSetup.read_ambari_user")
  @patch("ambari_server.serverSetup.get_YN_input")
  @patch("ambari_server.serverSetup.AmbariUserChecksLinux._create_custom_user")
  @patch("ambari_server.serverSetup.write_property")
  @patch("ambari_server.serverSetup.adjust_directory_permissions")
  @patch("ambari_server.serverSetup.print_error_msg")
  def test_check_ambari_user(self, print_error_msg_mock,
                             adjust_directory_permissions_mock, write_property_mock,
                             create_custom_user_mock, get_YN_input_mock, read_ambari_user_mock,
                             get_os_major_version_mock, get_os_type_mock, get_os_family_mock):

    # Scenario: user is already defined, user does not want to reconfigure it
    read_ambari_user_mock.return_value = "dummy-user"
    get_YN_input_mock.return_value = False
    result = check_ambari_user()
    self.assertTrue(get_YN_input_mock.called)
    self.assertFalse(write_property_mock.called)
    self.assertFalse(create_custom_user_mock.called)
    self.assertTrue(adjust_directory_permissions_mock.called)
    self.assertEqual(result, 0)

    get_YN_input_mock.reset_mock()
    write_property_mock.reset_mock()
    adjust_directory_permissions_mock.reset_mock()
    create_custom_user_mock.reset_mock()

    # Scenario: user is already defined, but user wants to reconfigure it

    read_ambari_user_mock.return_value = "dummy-user"
    create_custom_user_mock.return_value = (0, "new-dummy-user")
    get_YN_input_mock.return_value = True
    result = check_ambari_user()
    self.assertTrue(get_YN_input_mock.called)
    self.assertTrue(write_property_mock.called)
    self.assertTrue(write_property_mock.call_args[0][1] == "new-dummy-user")
    self.assertTrue(create_custom_user_mock.called)
    self.assertTrue(adjust_directory_permissions_mock.called)
    self.assertEqual(result, 0)

    get_YN_input_mock.reset_mock()
    write_property_mock.reset_mock()
    adjust_directory_permissions_mock.reset_mock()
    create_custom_user_mock.reset_mock()

    # Negative scenario: user is already defined, but user wants
    # to reconfigure it, user creation failed

    read_ambari_user_mock.return_value = "dummy-user"
    create_custom_user_mock.return_value = (1, None)
    get_YN_input_mock.return_value = True
    result = check_ambari_user()
    self.assertTrue(get_YN_input_mock.called)
    self.assertTrue(create_custom_user_mock.called)
    self.assertFalse(write_property_mock.called)
    self.assertFalse(adjust_directory_permissions_mock.called)
    self.assertEqual(result, 1)

    get_YN_input_mock.reset_mock()
    create_custom_user_mock.reset_mock()
    write_property_mock.reset_mock()
    adjust_directory_permissions_mock.reset_mock()

    # Scenario: user is not defined (setup process)
    read_ambari_user_mock.return_value = None
    get_YN_input_mock.return_value = True
    create_custom_user_mock.return_value = (0, "dummy-user")
    result = check_ambari_user()
    self.assertTrue(get_YN_input_mock.called)
    self.assertTrue(create_custom_user_mock.called)
    self.assertTrue(write_property_mock.called)
    self.assertTrue(write_property_mock.call_args[0][1] == "dummy-user")
    self.assertTrue(adjust_directory_permissions_mock.called)
    self.assertEqual(result, 0)

    get_YN_input_mock.reset_mock()
    create_custom_user_mock.reset_mock()
    write_property_mock.reset_mock()
    adjust_directory_permissions_mock.reset_mock()

    # Scenario: user is not defined (setup process), user creation failed

    read_ambari_user_mock.return_value = None
    get_YN_input_mock.return_value = True
    create_custom_user_mock.return_value = (1, None)
    result = check_ambari_user()
    self.assertTrue(get_YN_input_mock.called)
    self.assertTrue(create_custom_user_mock.called)
    self.assertFalse(write_property_mock.called)
    self.assertFalse(adjust_directory_permissions_mock.called)
    self.assertEqual(result, 1)

    get_YN_input_mock.reset_mock()
    create_custom_user_mock.reset_mock()
    write_property_mock.reset_mock()
    adjust_directory_permissions_mock.reset_mock()

    # negative scenario: user is not defined (setup process), user creation failed

    read_ambari_user_mock.return_value = None
    get_YN_input_mock.return_value = True
    create_custom_user_mock.return_value = (1, None)
    result = check_ambari_user()
    self.assertTrue(get_YN_input_mock.called)
    self.assertTrue(create_custom_user_mock.called)
    self.assertFalse(write_property_mock.called)
    self.assertFalse(adjust_directory_permissions_mock.called)
    self.assertEqual(result, 1)

    get_YN_input_mock.reset_mock()
    create_custom_user_mock.reset_mock()
    write_property_mock.reset_mock()
    adjust_directory_permissions_mock.reset_mock()

    # Scenario: user is not defined and left to be root
    read_ambari_user_mock.return_value = None
    get_YN_input_mock.return_value = False
    result = check_ambari_user()
    self.assertTrue(get_YN_input_mock.called)
    self.assertFalse(create_custom_user_mock.called)
    self.assertTrue(write_property_mock.called)
    self.assertTrue(write_property_mock.call_args[0][1] == "root")
    self.assertTrue(adjust_directory_permissions_mock.called)
    self.assertEqual(result, 0)


  @patch("__builtin__.open")
  @patch.object(ambari__server, "search_file")
  @patch.object(ambari__server, "set_file_permissions")
  @patch.object(ambari__server, "read_ambari_user")
  def test_store_password_file(self, read_ambari_user_mock, set_file_permissions_mock,
                               search_file_mock, open_mock):
    search_file_mock.return_value = "/etc/ambari-server/conf/ambari.properties"
    open_mock.return_value = MagicMock()
    ambari__server.store_password_file("password", "passfile")
    self.assertTrue(set_file_permissions_mock.called)

  @patch("subprocess.Popen")
  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  def test_check_iptables_is_running(self, get_os_major_version_mock, get_os_type_mock, get_os_family_mock, popen_mock):

    get_os_major_version_mock.return_value = 18
    get_os_type_mock.return_value = OSConst.OS_FEDORA
    get_os_family_mock.return_value = OSConst.REDHAT_FAMILY

    firewall_obj = Firewall().getFirewallObject()
    p = MagicMock()
    p.communicate.return_value = ("active", "err")
    p.returncode = 0
    popen_mock.return_value = p
    self.assertEqual("Fedora18FirewallChecks", firewall_obj.__class__.__name__)
    self.assertTrue(firewall_obj.check_iptables())
    p.communicate.return_value = ("", "err")
    p.returncode = 3
    self.assertFalse(firewall_obj.check_iptables())
    self.assertEqual("err", firewall_obj.stderrdata)


    get_os_type_mock.return_value = OSConst.OS_UBUNTU
    get_os_family_mock.return_value = OSConst.UBUNTU_FAMILY

    firewall_obj = Firewall().getFirewallObject()
    p.communicate.return_value = ("Status: active", "err")
    p.returncode = 0
    self.assertEqual("UbuntuFirewallChecks", firewall_obj.__class__.__name__)
    self.assertTrue(firewall_obj.check_iptables())
    p.communicate.return_value = ("Status: inactive", "err")
    p.returncode = 0
    self.assertFalse(firewall_obj.check_iptables())
    self.assertEqual("err", firewall_obj.stderrdata)

    get_os_type_mock.return_value = ""
    get_os_family_mock.return_value = OSConst.SUSE_FAMILY

    firewall_obj = Firewall().getFirewallObject()
    p.communicate.return_value = ("### iptables", "err")
    p.returncode = 0
    self.assertEqual("SuseFirewallChecks", firewall_obj.__class__.__name__)
    self.assertTrue(firewall_obj.check_iptables())
    p.communicate.return_value = ("SuSEfirewall2 not active", "err")
    p.returncode = 0
    self.assertFalse(firewall_obj.check_iptables())
    self.assertEqual("err", firewall_obj.stderrdata)

    get_os_type_mock.return_value = ""
    get_os_family_mock.return_value = OSConst.REDHAT_FAMILY

    firewall_obj = Firewall().getFirewallObject()
    p.communicate.return_value = ("Table: filter", "err")
    p.returncode = 0
    self.assertEqual("FirewallChecks", firewall_obj.__class__.__name__)
    self.assertTrue(firewall_obj.check_iptables())
    p.communicate.return_value = ("", "err")
    p.returncode = 3
    self.assertFalse(firewall_obj.check_iptables())
    self.assertEqual("err", firewall_obj.stderrdata)

  def test_download_progress(self):

    out = StringIO.StringIO()
    sys.stdout = out
    download_progress("filename", 10, 2, 100)
    sys.stdout = sys.__stdout__
    self.assertNotEqual("", out.getvalue())


  @patch("os.makedirs")
  @patch("os.path.exists")
  @patch("os.rename")
  @patch("os.stat")
  @patch("urllib2.urlopen")
  @patch("__builtin__.open")
  @patch("ambari_commons.inet_utils.download_progress")
  @patch("ambari_commons.inet_utils.print_info_msg")
  def test_track_jdk(self, print_info_msg_mock, download_progress_mock, openMock,
                     urlopenMock, os_stat_mock, os_rename_mock, os_path_exists_mock, os_makedirs_mock):
    os_path_exists_mock.return_value = False

    fsize = MagicMock()
    fsize.st_size = 24576
    os_stat_mock.return_value = fsize

    inf = MagicMock()
    inf.getheaders.return_value = ["bytes 0-24575/24576"]

    u = MagicMock()
    u.info.return_value = inf
    chunks = [None, "second", "first"]

    def side_effect(*args, **kwargs):
      return chunks.pop()

    u.read.side_effect = side_effect
    urlopenMock.return_value = u

    f = MagicMock()
    openMock.return_value = f

    force_download_file("base", "url", progress_func=download_progress_mock)

    self.assertEqual(0, len(chunks))
    self.assertTrue(f.write.called)
    self.assertTrue(f.close.called)
    self.assertEqual(2, len(download_progress_mock.call_args_list))
    self.assertTrue(print_info_msg_mock.called)

  @patch("shutil.copy")
  @patch("os.path.join")
  @patch("os.path.exists")
  @patch("os.path.isdir")
  @patch.object(ambari__server, "get_ambari_properties")
  def test_install_jce_manualy(self, get_ambari_properties_mock, \
                               os_path_isdir_mock, os_path_exists_mock, os_path_join_mock, \
                               shutil_copy_mock):
    args = MagicMock()
    args.jce_policy = "somewhere"
    p = MagicMock()
    get_ambari_properties_mock.return_value = p
    p.__getitem__.side_effect = None
    p.__getitem__.return_value = "somewhere"

    # Case when JCE path doesn't exists
    os_path_exists_mock.return_value = False
    try:
      ambari__server.install_jce_manualy(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertTrue("JCE Policy path" in fe.reason)
      pass
    os_path_exists_mock.reset()

    # Case when JCE is a directory
    os_path_exists_mock.return_value = True
    os_path_isdir_mock.return_value = True
    try:
      ambari__server.install_jce_manualy(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertTrue("JCE Policy path is a directory" in fe.reason)
      pass
    os_path_isdir_mock.reset()

    os_path_isdir_mock.return_value = False
    os_path_join_mock.return_value = \
      "/var/lib/ambari-server/resources/jce_policy-6.zip"
    ambari__server.install_jce_manualy(args)
    self.assertTrue(shutil_copy_mock.called)

    shutil_copy_mock.side_effect = Exception("exception")
    try:
      ambari__server.install_jce_manualy(args)
      self.fail("Should throw exception because of not found jce_policy-6.zip")
    except Exception:
    # Expected
      self.assertTrue(shutil_copy_mock.called)
      pass

    shutil_copy_mock.side_effect = None
    args.jce_policy = None
    ambari__server.install_jce_manualy(args)

  @patch.object(ambari__server, "get_validated_filepath_input")
  @patch.object(ambari__server, 'run_os_command')
  @patch.object(ambari__server, "get_truststore_type")
  @patch("__builtin__.open")
  @patch.object(ambari__server, "find_properties_file")
  @patch.object(ambari__server, "run_component_https_cmd")
  @patch.object(ambari__server, "get_delete_cert_command")
  @patch.object(ambari__server, "get_truststore_password")
  @patch.object(ambari__server, "get_truststore_path")
  @patch.object(ambari__server, "get_YN_input")
  @patch.object(ambari__server, "get_ambari_properties")
  @patch.object(ambari__server, "find_jdk")
  def test_setup_component_https(self, find_jdk_mock, get_ambari_properties_mock, get_YN_input_mock,
                                 get_truststore_path_mock, get_truststore_password_mock,
                                 get_delete_cert_command_mock, run_component_https_cmd_mock,
                                 find_properties_file_mock, open_mock,
                                 get_truststore_type_mock, run_os_command_mock,
                                 get_validated_filepath_input_mock):
    out = StringIO.StringIO()
    sys.stdout = out
    component = "component"
    command = "command"
    property = "use_ssl"
    alias = "alias"
    #Silent mode
    ambari__server.SILENT = True
    ambari__server.setup_component_https(component, command, property, alias)
    self.assertEqual('command is not enabled in silent mode.\n', out.getvalue())
    sys.stdout = sys.__stdout__
    #Verbouse mode and jdk_path is None
    ambari__server.SILENT = False
    p = get_ambari_properties_mock.return_value
    # Use ssl
    p.get_property.side_effect = ["true"]
    # Dont disable ssl
    get_YN_input_mock.side_effect = [False]
    ambari__server.setup_component_https(component, command, property, alias)
    self.assertTrue(p.get_property.called)
    self.assertTrue(get_YN_input_mock.called)
    p.get_property.reset_mock()
    get_YN_input_mock.reset_mock()
    # Dont use ssl
    p.get_property.side_effect = ["false"]
    # Dont enable ssl
    get_YN_input_mock.side_effect = [False]
    ambari__server.setup_component_https(component, command, property, alias)
    self.assertTrue(p.get_property.called)
    self.assertTrue(get_YN_input_mock.called)
    p.get_property.reset_mock()
    get_YN_input_mock.reset_mock()
    # Cant find jdk
    find_jdk_mock.return_value = None
    try:
        ambari__server.setup_component_https(component, command, property, alias)
        self.fail("Should throw exception")
    except FatalException as fe:
        # Expected
        self.assertTrue('No JDK found, please run the "ambari-server setup" command to install a' +
                        ' JDK automatically or install any JDK manually to ' in fe.reason)
        pass
    #Verbouse mode and jdk_path is not None (use_https = true)
    find_jdk_mock.return_value = "/jdk_path"
    p.get_property.side_effect = ["true"]
    get_YN_input_mock.side_effect = [True]
    get_truststore_path_mock.return_value = "/truststore_path"
    get_truststore_password_mock.return_value = "/truststore_password"
    get_delete_cert_command_mock.return_value = "rm -f"
    ambari__server.setup_component_https(component, command, property, alias)

    self.assertTrue(p.process_pair.called)
    self.assertTrue(get_truststore_path_mock.called)
    self.assertTrue(get_truststore_password_mock.called)
    self.assertTrue(get_delete_cert_command_mock.called)
    self.assertTrue(find_properties_file_mock.called)
    self.assertTrue(open_mock.called)
    self.assertTrue(p.store.called)
    self.assertTrue(run_component_https_cmd_mock.called)

    p.process_pair.reset_mock()
    get_truststore_path_mock.reset_mock()
    get_truststore_password_mock.reset_mock()
    get_delete_cert_command_mock.reset_mock()
    find_properties_file_mock.reset_mock()
    open_mock.reset_mock()
    p.store.reset_mock()
    #Verbouse mode and jdk_path is not None (use_https = false) and import cert
    p.get_property.side_effect = ["false"]
    get_YN_input_mock.side_effect = [True]
    ambari__server.setup_component_https(component, command, property, alias)

    self.assertTrue(p.process_pair.called)
    self.assertTrue(get_truststore_type_mock.called)
    self.assertTrue(get_truststore_path_mock.called)
    self.assertTrue(get_truststore_password_mock.called)
    self.assertTrue(get_delete_cert_command_mock.called)
    self.assertTrue(find_properties_file_mock.called)
    self.assertTrue(open_mock.called)
    self.assertTrue(p.store.called)
    self.assertTrue(run_component_https_cmd_mock.called)
    self.assertTrue(run_os_command_mock.called)
    self.assertTrue(get_validated_filepath_input_mock.called)

    p.process_pair.reset_mock()
    get_truststore_type_mock.reset_mock()
    get_truststore_path_mock.reset_mock()
    get_truststore_password_mock.reset_mock()
    get_delete_cert_command_mock.reset_mock()
    find_properties_file_mock.reset_mock()
    open_mock.reset_mock()
    p.store.reset_mock()
    run_os_command_mock.reset_mock()
    get_validated_filepath_input_mock.reset_mock()

  @patch.object(ambari__server, "adjust_directory_permissions")
  @patch.object(ambari__server, "read_ambari_user")
  @patch.object(ambari__server, "get_validated_string_input")
  @patch.object(ambari__server, "find_properties_file")
  @patch.object(ambari__server, "get_ambari_properties")
  @patch.object(ambari__server, "is_server_runing")
  @patch.object(ambari__server, "import_cert_and_key_action")
  @patch.object(ambari__server, "get_YN_input")
  @patch("__builtin__.open")
  @patch("ambari_server.properties.Properties")
  @patch.object(ambari__server, "is_root")
  @patch.object(ambari__server, "is_valid_cert_host")
  @patch.object(ambari__server, "is_valid_cert_exp")
  def test_setup_https(self, is_valid_cert_exp_mock, is_valid_cert_host_mock, \
                       is_root_mock, Properties_mock, open_Mock, get_YN_input_mock, \
                       import_cert_and_key_action_mock,
                       is_server_runing_mock, get_ambari_properties_mock, \
                       find_properties_file_mock, \
                       get_validated_string_input_mock, read_ambari_user_method, \
                       adjust_directory_permissions_mock):

    is_valid_cert_exp_mock.return_value = True
    is_valid_cert_host_mock.return_value = True
    args = MagicMock()
    open_Mock.return_value = file
    p = get_ambari_properties_mock.return_value

    # Testing call under non-root
    is_root_mock.return_value = False
    try:
      ambari__server.setup_https(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertTrue("root-level" in fe.reason)
      pass

    # Testing call under root
    is_root_mock.return_value = True
    read_ambari_user_method.return_value = "user"
    #Case #1: if client ssl is on and user didnt choose
    #disable ssl option and choose import certs and keys
    p.get_property.side_effect = ["key_dir", "5555", "6666", "true"]
    get_YN_input_mock.side_effect = [False, True]
    get_validated_string_input_mock.side_effect = ["4444"]
    get_property_expected = "[call('security.server.keys_dir'),\n" + \
                            " call('client.api.ssl.port'),\n" + \
                            " call('client.api.ssl.port'),\n call('api.ssl')]"
    process_pair_expected = "[call('client.api.ssl.port', '4444')]"
    ambari__server.SILENT = False
    ambari__server.setup_https(args)

    self.assertTrue(p.process_pair.called)
    self.assertTrue(p.get_property.call_count == 4)
    self.assertEqual(str(p.get_property.call_args_list), get_property_expected)
    self.assertEqual(str(p.process_pair.call_args_list), process_pair_expected)
    self.assertTrue(p.store.called)
    self.assertTrue(import_cert_and_key_action_mock.called)

    p.process_pair.reset_mock()
    p.get_property.reset_mock()
    p.store.reset_mock()
    import_cert_and_key_action_mock.reset_mock()

    #Case #2: if client ssl is on and user choose to disable ssl option
    p.get_property.side_effect = ["key_dir", "", "true"]
    get_YN_input_mock.side_effect = [True]
    get_validated_string_input_mock.side_effect = ["4444"]
    get_property_expected = "[call('security.server.keys_dir'),\n" + \
                            " call('client.api.ssl.port'),\n call('api.ssl')]"
    process_pair_expected = "[call('api.ssl', 'false')]"
    ambari__server.setup_https(args)

    self.assertTrue(p.process_pair.called)
    self.assertTrue(p.get_property.call_count == 3)
    self.assertEqual(str(p.get_property.call_args_list), get_property_expected)
    self.assertEqual(str(p.process_pair.call_args_list), process_pair_expected)
    self.assertTrue(p.store.called)
    self.assertFalse(import_cert_and_key_action_mock.called)

    p.process_pair.reset_mock()
    p.get_property.reset_mock()
    p.store.reset_mock()
    import_cert_and_key_action_mock.reset_mock()

    #Case #3: if client ssl is off and user choose option
    #to import cert and keys
    p.get_property.side_effect = ["key_dir", "", None]
    get_YN_input_mock.side_effect = [True, True]
    get_validated_string_input_mock.side_effect = ["4444"]
    get_property_expected = "[call('security.server.keys_dir'),\n" + \
                            " call('client.api.ssl.port'),\n call('api.ssl')]"
    process_pair_expected = "[call('client.api.ssl.port', '4444')]"
    ambari__server.setup_https(args)

    self.assertTrue(p.process_pair.called)
    self.assertTrue(p.get_property.call_count == 3)
    self.assertEqual(str(p.get_property.call_args_list), get_property_expected)
    self.assertEqual(str(p.process_pair.call_args_list), process_pair_expected)
    self.assertTrue(p.store.called)
    self.assertTrue(import_cert_and_key_action_mock.called)

    p.process_pair.reset_mock()
    p.get_property.reset_mock()
    p.store.reset_mock()
    import_cert_and_key_action_mock.reset_mock()

    #Case #4: if client ssl is off and
    #user did not choose option to import cert and keys
    p.get_property.side_effect = ["key_dir", "", None]
    get_YN_input_mock.side_effect = [False]
    get_validated_string_input_mock.side_effect = ["4444"]
    get_property_expected = "[call('security.server.keys_dir'),\n" + \
                            " call('client.api.ssl.port'),\n call('api.ssl')]"
    process_pair_expected = "[]"
    ambari__server.setup_https(args)

    self.assertFalse(p.process_pair.called)
    self.assertTrue(p.get_property.call_count == 3)
    self.assertEqual(str(p.get_property.call_args_list), get_property_expected)
    self.assertEqual(str(p.process_pair.call_args_list), process_pair_expected)
    self.assertFalse(p.store.called)
    self.assertFalse(import_cert_and_key_action_mock.called)

    p.process_pair.reset_mock()
    p.get_property.reset_mock()
    p.store.reset_mock()
    import_cert_and_key_action_mock.reset_mock()

    #Case #5: if cert must be imported but didnt imported
    p.get_property.side_effect = ["key_dir", "", "false"]
    get_YN_input_mock.side_effect = [True]
    import_cert_and_key_action_mock.side_effect = [False]
    get_validated_string_input_mock.side_effect = ["4444"]
    get_property_expected = "[call('security.server.keys_dir'),\n" + \
                            " call('client.api.ssl.port'),\n call('api.ssl')]"
    process_pair_expected = "[call('client.api.ssl.port', '4444')]"
    self.assertFalse(ambari__server.setup_https(args))
    self.assertTrue(p.process_pair.called)
    self.assertTrue(p.get_property.call_count == 3)
    self.assertEqual(str(p.get_property.call_args_list), get_property_expected)
    self.assertEqual(str(p.process_pair.call_args_list), process_pair_expected)
    self.assertFalse(p.store.called)
    self.assertTrue(import_cert_and_key_action_mock.called)

    p.process_pair.reset_mock()
    p.get_property.reset_mock()
    p.store.reset_mock()
    import_cert_and_key_action_mock.reset_mock()

    #Case #6: if silent mode is enabled
    ambari__server.SILENT = True
    try:
      ambari__server.setup_https(args)
      self.fail("Should throw exception")
    except NonFatalException as fe:
      self.assertTrue("setup-https is not enabled in silent mode" in fe.reason)

    p.process_pair.reset_mock()
    p.get_property.reset_mock()
    p.store.reset_mock()
    import_cert_and_key_action_mock.reset_mock()

    #Case #7: read property throw exception
    ambari__server.SILENT = False
    find_properties_file_mock.return_value = "propertyFile"
    p.get_property.side_effect = KeyError("Failed to read property")
    try:
        ambari__server.setup_https(args)
        self.fail("Should throw exception")
    except FatalException as fe:
        self.assertTrue("Failed to read property" in fe.reason)



  @patch.object(ambari__server, "import_cert_and_key")
  def test_import_cert_and_key_action(self, import_cert_and_key_mock):
    import_cert_and_key_mock.return_value = True
    properties = MagicMock()
    properties.get_property.side_effect = ["key_dir", "5555", "6666", "true"]
    properties.process_pair = MagicMock()
    expect_process_pair = "[call('client.api.ssl.cert_name', 'https.crt'),\n" + \
                          " call('client.api.ssl.key_name', 'https.key'),\n" + \
                          " call('api.ssl', 'true')]"
    ambari__server.import_cert_and_key_action("key_dir", properties)

    self.assertEqual(str(properties.process_pair.call_args_list), \
                     expect_process_pair)

  @patch.object(ambari__server, "remove_file")
  @patch.object(ambari__server, "copy_file")
  @patch.object(ambari__server, "read_ambari_user")
  @patch.object(ambari__server, "set_file_permissions")
  @patch.object(ambari__server, "import_file_to_keystore")
  @patch("__builtin__.open")
  @patch.object(ambari__server, 'run_os_command')
  @patch("os.path.join")
  @patch("os.path.isfile")
  @patch('__builtin__.raw_input')
  @patch.object(ambari__server, "get_validated_string_input")
  @patch.object(ambari__server, "is_valid_cert_host")
  @patch.object(ambari__server, "is_valid_cert_exp")
  def test_import_cert_and_key(self, is_valid_cert_exp_mock, \
                               is_valid_cert_host_mock, \
                               get_validated_string_input_mock, \
                               raw_input_mock, \
                               os_path_isfile_mock, \
                               os_path_join_mock, run_os_command_mock, \
                               open_mock, import_file_to_keystore_mock, \
                               set_file_permissions_mock, read_ambari_user_mock, copy_file_mock, \
                               remove_file_mock):
    is_valid_cert_exp_mock.return_value = True
    is_valid_cert_host_mock.return_value = True
    os_path_isfile_mock.return_value = True
    get_validated_string_input_mock.return_value = "password"
    raw_input_mock.side_effect = \
      ["cert_file_path", "key_file_path"]
    os_path_join_mock.side_effect = ["keystore_file_path", "keystore_file_path_tmp", \
                                     "pass_file_path", "pass_file_path_tmp", \
                                     "passin_file_path", "password_file_path", \
                                     "keystore_cert_file_path", \
                                     "keystore_cert_key_file_path", ]
    run_os_command_mock.return_value = (0, "", "")
    om = open_mock.return_value
    expect_import_file_to_keystore = "[call('keystore_file_path_tmp'," + \
                                     " 'keystore_file_path'),\n" + \
                                     " call('pass_file_path_tmp'," + \
                                     " 'pass_file_path'),\n" + \
                                     " call('cert_file_path'," + \
                                     " 'keystore_cert_file_path'),\n" + \
                                     " call('key_file_path'," + \
                                     " 'keystore_cert_key_file_path')]"

    ambari__server.import_cert_and_key("key_dir")
    self.assertTrue(raw_input_mock.call_count == 2)
    self.assertTrue(get_validated_string_input_mock.called)
    self.assertEqual(os_path_join_mock.call_count, 8)
    self.assertTrue(set_file_permissions_mock.call_count == 1)
    self.assertEqual(str(import_file_to_keystore_mock.call_args_list), \
                     expect_import_file_to_keystore)

  @patch.object(ambari__server, "remove_file")
  @patch.object(ambari__server, "copy_file")
  @patch.object(ambari__server, "generate_random_string")
  @patch.object(ambari__server, "read_ambari_user")
  @patch.object(ambari__server, "set_file_permissions")
  @patch.object(ambari__server, "import_file_to_keystore")
  @patch("__builtin__.open")
  @patch.object(ambari__server, 'run_os_command')
  @patch("os.path.join")
  @patch.object(ambari__server, "get_validated_filepath_input")
  @patch.object(ambari__server, "get_validated_string_input")
  @patch.object(ambari__server, "is_valid_cert_host")
  @patch.object(ambari__server, "is_valid_cert_exp")
  def test_import_cert_and_key_with_empty_password(self, \
                                                   is_valid_cert_exp_mock, is_valid_cert_host_mock,
                                                   get_validated_string_input_mock, get_validated_filepath_input_mock, \
                                                   os_path_join_mock, run_os_command_mock, open_mock, \
                                                   import_file_to_keystore_mock, set_file_permissions_mock,
                                                   read_ambari_user_mock, generate_random_string_mock, copy_file_mock, \
                                                   remove_file_mock):

    is_valid_cert_exp_mock.return_value = True
    is_valid_cert_host_mock.return_value = True
    get_validated_string_input_mock.return_value = ""
    get_validated_filepath_input_mock.side_effect = \
      ["cert_file_path", "key_file_path"]
    os_path_join_mock.side_effect = ["keystore_file_path", "keystore_file_path_tmp", \
                                     "pass_file_path", "pass_file_path_tmp", \
                                     "passin_file_path", "password_file_path", \
                                     "keystore_cert_file_path", \
                                     "keystore_cert_key_file_path", ]
    run_os_command_mock.return_value = (0, "", "")

    expect_import_file_to_keystore = "[call('keystore_file_path_tmp'," + \
                                     " 'keystore_file_path'),\n" + \
                                     " call('pass_file_path_tmp'," + \
                                     " 'pass_file_path'),\n" + \
                                     " call('cert_file_path'," + \
                                     " 'keystore_cert_file_path'),\n" + \
                                     " call('key_file_path.secured'," + \
                                     " 'keystore_cert_key_file_path')]"

    ambari__server.import_cert_and_key("key_dir")
    self.assertEquals(get_validated_filepath_input_mock.call_count, 2)
    self.assertTrue(get_validated_string_input_mock.called)
    self.assertEquals(os_path_join_mock.call_count, 8)
    self.assertEquals(set_file_permissions_mock.call_count, 1)
    self.assertEqual(str(import_file_to_keystore_mock.call_args_list), \
                     expect_import_file_to_keystore)
    self.assertTrue(generate_random_string_mock.called)

  @patch("__builtin__.open")
  @patch.object(ambari__server, "copy_file")
  @patch.object(ambari__server, "is_root")
  @patch.object(ambari__server, "read_ambari_user")
  @patch.object(ambari__server, "set_file_permissions")
  @patch.object(ambari__server, "import_file_to_keystore")
  @patch.object(ambari__server, 'run_os_command')
  @patch("os.path.join")
  @patch.object(ambari__server, "get_validated_filepath_input")
  @patch.object(ambari__server, "get_validated_string_input")
  def test_import_cert_and_key_with_incorrect_password(self,
                                                       get_validated_string_input_mock, \
                                                       get_validated_filepath_input_mock, \
                                                       os_path_join_mock, \
                                                       run_os_command_mock, \
                                                       import_file_to_keystore_mock, \
                                                       set_file_permissions_mock, \
                                                       read_ambari_user_mock, \
                                                       is_root_mock, \
                                                       copy_file_mock, \
                                                       open_mock):
    get_validated_string_input_mock.return_value = "incorrect_password"
    get_validated_filepath_input_mock.return_value = 'filename'
    open_mock.return_value = MagicMock()

    os_path_join_mock.return_value = ''
    is_root_mock.return_value = True


    #provided password doesn't match, openssl command returns an error
    run_os_command_mock.return_value = (1, "", "Some error message")

    self.assertFalse(ambari__server.import_cert_and_key_action(*["key_dir", None]))
    self.assertFalse(ambari__server.import_cert_and_key("key_dir"))

  def test_is_valid_cert_exp(self):

    #No data in certInfo
    certInfo = {}
    is_valid = ambari__server.is_valid_cert_exp(certInfo)
    self.assertFalse(is_valid)

    #Issued in future
    issuedOn = (datetime.datetime.now() + datetime.timedelta(hours=1000)).strftime(ambari__server.SSL_DATE_FORMAT)
    expiresOn = (datetime.datetime.now() + datetime.timedelta(hours=2000)).strftime(ambari__server.SSL_DATE_FORMAT)
    certInfo = {ambari__server.NOT_BEFORE_ATTR: issuedOn,
                ambari__server.NOT_AFTER_ATTR: expiresOn}
    is_valid = ambari__server.is_valid_cert_exp(certInfo)
    self.assertFalse(is_valid)

    #Was expired
    issuedOn = (datetime.datetime.now() - datetime.timedelta(hours=2000)).strftime(ambari__server.SSL_DATE_FORMAT)
    expiresOn = (datetime.datetime.now() - datetime.timedelta(hours=1000)).strftime(ambari__server.SSL_DATE_FORMAT)
    certInfo = {ambari__server.NOT_BEFORE_ATTR: issuedOn,
                ambari__server.NOT_AFTER_ATTR: expiresOn}
    is_valid = ambari__server.is_valid_cert_exp(certInfo)
    self.assertFalse(is_valid)

    #Valid
    issuedOn = (datetime.datetime.now() - datetime.timedelta(hours=2000)).strftime(ambari__server.SSL_DATE_FORMAT)
    expiresOn = (datetime.datetime.now() + datetime.timedelta(hours=1000)).strftime(ambari__server.SSL_DATE_FORMAT)
    certInfo = {ambari__server.NOT_BEFORE_ATTR: issuedOn,
                ambari__server.NOT_AFTER_ATTR: expiresOn}
    is_valid = ambari__server.is_valid_cert_exp(certInfo)
    self.assertTrue(is_valid)

  @patch.object(ambari__server, "get_fqdn")
  def test_is_valid_cert_host(self, get_fqdn_mock):

    #No data in certInfo
    certInfo = {}
    is_valid = ambari__server.is_valid_cert_host(certInfo)
    self.assertFalse(is_valid)

    #Failed to get FQDN
    get_fqdn_mock.return_value = None
    is_valid = ambari__server.is_valid_cert_host(certInfo)
    self.assertFalse(is_valid)

    #FQDN and Common name in certificated don't correspond
    get_fqdn_mock.return_value = 'host1'
    certInfo = {ambari__server.COMMON_NAME_ATTR: 'host2'}
    is_valid = ambari__server.is_valid_cert_host(certInfo)
    self.assertFalse(is_valid)

    #FQDN and Common name in certificated correspond
    get_fqdn_mock.return_value = 'host1'
    certInfo = {ambari__server.COMMON_NAME_ATTR: 'host1'}
    is_valid = ambari__server.is_valid_cert_host(certInfo)
    self.assertTrue(is_valid)


  @patch.object(ambari__server, "get_ambari_properties")
  def test_is_valid_https_port(self, get_ambari_properties_mock):

    #No ambari.properties
    get_ambari_properties_mock.return_value = -1
    is_valid = ambari__server.is_valid_https_port(1111)
    self.assertEqual(is_valid, False)

    #User entered port used by one way auth
    portOneWay = "1111"
    portTwoWay = "2222"
    validPort = "3333"
    get_ambari_properties_mock.return_value = {ambari__server.SRVR_ONE_WAY_SSL_PORT_PROPERTY: portOneWay,
                                               ambari__server.SRVR_TWO_WAY_SSL_PORT_PROPERTY: portTwoWay}
    is_valid = ambari__server.is_valid_https_port(portOneWay)
    self.assertEqual(is_valid, False)

    #User entered port used by two way auth
    is_valid = ambari__server.is_valid_https_port(portTwoWay)
    self.assertEqual(is_valid, False)

    #User entered valid port
    get_ambari_properties_mock.return_value = {ambari__server.SRVR_ONE_WAY_SSL_PORT_PROPERTY: portOneWay,
                                               ambari__server.SRVR_TWO_WAY_SSL_PORT_PROPERTY: portTwoWay}
    is_valid = ambari__server.is_valid_https_port(validPort)
    self.assertEqual(is_valid, True)

  @patch("socket.getfqdn")
  @patch("urllib2.urlopen")
  @patch.object(ambari__server, "get_ambari_properties")
  def test_get_fqdn(self, get_ambari_properties_mock, url_open_mock, getfqdn_mock):

    #No ambari.properties
    get_ambari_properties_mock.return_value = -1
    fqdn = ambari__server.get_fqdn()
    self.assertEqual(fqdn, None)

    #Check mbari_server.GET_FQDN_SERVICE_URL property name (AMBARI-2612)
    #property name should be server.fqdn.service.url
    self.assertEqual(ambari__server.GET_FQDN_SERVICE_URL, "server.fqdn.service.url")

    #Read FQDN from service
    p = MagicMock()
    p[ambari__server.GET_FQDN_SERVICE_URL] = 'someurl'
    get_ambari_properties_mock.return_value = p

    u = MagicMock()
    host = 'host1.domain.com'
    u.read.return_value = host
    url_open_mock.return_value = u

    fqdn = ambari__server.get_fqdn()
    self.assertEqual(fqdn, host)

    #Failed to read FQDN from service, getting from socket
    u.reset_mock()
    u.side_effect = Exception("Failed to read FQDN from service")
    getfqdn_mock.return_value = host
    fqdn = ambari__server.get_fqdn()
    self.assertEqual(fqdn, host)


  @patch.object(ambari__server, "find_properties_file")
  def test_get_ulimit_open_files(self, find_properties_file_mock):

    # 1 - No ambari.properties
    find_properties_file_mock.return_value = None
    open_files = ambari__server.get_fqdn()
    self.assertEqual(open_files, None)

    # 2 - With ambari.properties - ok
    tf1 = tempfile.NamedTemporaryFile()
    prop_value = 65000
    with open(tf1.name, 'w') as fout:
      fout.write(ambari__server.ULIMIT_OPEN_FILES_KEY + '=' + str(prop_value))
    fout.close()
    find_properties_file_mock.return_value = tf1.name
    open_files = ambari__server.get_ulimit_open_files()
    self.assertEqual(open_files, 65000)

    # 2 - With ambari.properties - default
    tf1 = tempfile.NamedTemporaryFile()
    prop_value = 0
    with open(tf1.name, 'w') as fout:
      fout.write(ambari__server.ULIMIT_OPEN_FILES_KEY + '=' + str(prop_value))
    fout.close()
    find_properties_file_mock.return_value = tf1.name
    open_files = ambari__server.get_ulimit_open_files()
    self.assertEqual(open_files, ambari__server.ULIMIT_OPEN_FILES_DEFAULT)


  @patch.object(ambari__server, 'run_os_command')
  def test_get_cert_info(self, run_os_command_mock):
    # Error running openssl command
    path = 'path/to/certificate'
    run_os_command_mock.return_value = -1, None, None
    cert_info = ambari__server.get_cert_info(path)
    self.assertEqual(cert_info, None)

    #Empty result of openssl command
    run_os_command_mock.return_value = 0, None, None
    cert_info = ambari__server.get_cert_info(path)
    self.assertEqual(cert_info, None)

    #Positive scenario
    notAfter = 'Jul  3 14:12:57 2014 GMT'
    notBefore = 'Jul  3 14:12:57 2013 GMT'
    attr1_key = 'A'
    attr1_value = 'foo'
    attr2_key = 'B'
    attr2_value = 'bar'
    attr3_key = 'CN'
    attr3_value = 'host.domain.com'
    subject_pattern = '/{attr1_key}={attr1_value}/{attr2_key}={attr2_value}/{attr3_key}={attr3_value}'
    subject = subject_pattern.format(attr1_key=attr1_key, attr1_value=attr1_value,
                                     attr2_key=attr2_key, attr2_value=attr2_value,
                                     attr3_key=attr3_key, attr3_value=attr3_value)
    out_pattern = """
notAfter={notAfter}
notBefore={notBefore}
subject={subject}
-----BEGIN CERTIFICATE-----
MIIFHjCCAwYCCQDpHKOBI+Lt0zANBgkqhkiG9w0BAQUFADBRMQswCQYDVQQGEwJV
...
5lqd8XxOGSYoMOf+70BLN2sB
-----END CERTIFICATE-----
    """
    out = out_pattern.format(notAfter=notAfter, notBefore=notBefore, subject=subject)
    run_os_command_mock.return_value = 0, out, None
    cert_info = ambari__server.get_cert_info(path)
    self.assertEqual(cert_info['notAfter'], notAfter)
    self.assertEqual(cert_info['notBefore'], notBefore)
    self.assertEqual(cert_info['subject'], subject)
    self.assertEqual(cert_info[attr1_key], attr1_value)
    self.assertEqual(cert_info[attr2_key], attr2_value)
    self.assertEqual(cert_info[attr3_key], attr3_value)


  @patch('__builtin__.raw_input')
  def test_get_validated_string_input(self, raw_input_mock):
    prompt = 'prompt'
    default_value = 'default'
    description = 'desc'
    validator = MagicMock()
    validator.return_value = True
    inputed_value1 = 'val1'
    inputed_value2 = 'val2'
    raw_input_mock.return_value = inputed_value1
    input = ambari__server.get_validated_string_input(prompt, default_value, None,
                                                     description, False, False, validator)
    self.assertTrue(validator.called)
    self.assertEqual(inputed_value1, input)

    validator.side_effect = [False, True]
    raw_input_mock.side_effect = [inputed_value1, inputed_value2]
    input = ambari__server.get_validated_string_input(prompt, default_value, None,
                                                     description, False, False, validator)
    self.assertEqual(inputed_value2, input)


  @patch.object(ambari__server, 'run_os_command')
  @patch("__builtin__.open")
  @patch("os.path.exists")
  def test_is_server_runing(self, os_path_exists_mock, open_mock, \
                            run_os_command_mock):
    os_path_exists_mock.return_value = True
    f = open_mock.return_value
    f.readline.return_value = "111"
    run_os_command_mock.return_value = 0, "", ""
    status, pid = ambari__server.is_server_runing()
    self.assertTrue(status)
    self.assertEqual(111, pid)
    os_path_exists_mock.return_value = False
    status, pid = ambari__server.is_server_runing()
    self.assertFalse(status)


  @patch.object(ambari__server, 'run_os_command')
  @patch("__builtin__.open")
  @patch("os.path.exists")
  def test_is_server_runing_bad_file(self, os_path_exists_mock, open_mock, \
                            run_os_command_mock):
    os_path_exists_mock.return_value = True
    f = open_mock.return_value
    f.readline.return_value = "" # empty file content
    run_os_command_mock.return_value = 0, "", ""

    try:
      ambari__server.is_server_runing()
      self.fail("Should throw NonFatalException")
    except NonFatalException as fe:
      pass

    open_mock.side_effect = IOError('[Errno 13] Permission denied: /var/run/ambari-server/ambari-server.pid')

    try:
      ambari__server.is_server_runing()
      self.fail("Should throw FatalException")
    except FatalException as fe:
      pass


  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("os.chdir")
  @patch("ambari_commons.os_linux.os_run_os_command")
  def test_install_jdk(self, run_os_command_mock, os_chdir_mock,
                       get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    jdk_rel = JDKRelease("jdk1", "JDK name", "http://somewhere/somewhere.tar.gz", "somewhere.tar.gz",
                         "http://somewhere/some-jcpol.tar.gz", "some-jcpol.tar.gz", "/jdk1", "(jdk.*)/jre")
    run_os_command_mock.return_value = 1, "Creating jdk1/jre", ""
    failed = False
    try:
      jdk_setup_obj = JDKSetup()
      jdk_setup_obj._install_jdk("/java.tar.gz", jdk_rel)
      self.fail("Exception was not rised!")
    except FatalException:
      failed = True
    self.assertTrue(failed)


  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("os.stat")
  @patch("os.path.isfile")
  @patch("os.path.exists")
  @patch("os.chdir")
  @patch("ambari_server.serverSetup.force_download_file")
  @patch("ambari_server.serverSetup.get_YN_input")
  @patch("ambari_server.serverSetup.run_os_command")
  @patch("ambari_server.serverSetup.update_properties")
  @patch("ambari_server.serverSetup.get_validated_string_input")
  @patch("ambari_server.serverSetup.print_info_msg")
  @patch("ambari_server.serverSetup.get_JAVA_HOME")
  @patch("ambari_server.serverSetup.get_ambari_properties")
  @patch("shutil.copyfile")
  @patch("sys.exit")
  def test_download_jdk(self, exit_mock, copyfile_mock, get_ambari_properties_mock, get_JAVA_HOME_mock, \
                        print_info_msg_mock, get_validated_string_input_mock, update_properties_mock, \
                        run_os_command_mock, get_YN_input_mock, force_download_file_mock,
                        os_chdir_mock, path_existsMock, path_isfileMock, statMock,
                        get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    args = MagicMock()
    args.java_home = "somewhere"
    args.silent = False

    p = Properties()
    p.process_pair("java.releases", "jdk1")
    p.process_pair("jdk1.desc", "JDK name")
    p.process_pair("jdk1.url", "http://somewhere/somewhere.tar.gz")
    p.process_pair("jdk1.dest-file", "somewhere.tar.gz")
    p.process_pair("jdk1.jcpol-url", "http://somewhere/some-jcpol.tar.gz")
    p.process_pair("jdk1.jcpol-file", "some-jcpol.tar.gz")
    p.process_pair("jdk1.home", "/jdk1")
    p.process_pair("jdk1.re", "(jdk.*)/jre")

    path_existsMock.return_value = False
    get_JAVA_HOME_mock.return_value = False
    get_ambari_properties_mock.return_value = p
    # Test case: ambari.properties not found
    try:
      download_and_install_jdk(args)
      self.fail("Should throw exception because of not found ambari.properties")
    except FatalException:
      # Expected
      self.assertTrue(get_ambari_properties_mock.called)
      pass

    # Test case: JDK already exists
    args.java_home = None
    args.jdk_location = None
    get_JAVA_HOME_mock.return_value = "some_jdk"
    get_YN_input_mock.return_value = False
    path_existsMock.side_effect = [False,False]
    run_os_command_mock.return_value = 0, "", ""
    rcode = download_and_install_jdk(args)
    self.assertEqual(0, rcode)

    # Test case: java home setup
    args.java_home = "somewhere"
    path_existsMock.side_effect = [True,False,False]
    get_JAVA_HOME_mock.return_value = None
    rcode = download_and_install_jdk(args)
    self.assertEqual(0, rcode)
    self.assertTrue(update_properties_mock.called)

    # Test case: JDK file does not exist, property not defined
    path_existsMock.side_effect = None
    path_existsMock.return_value = False
    get_ambari_properties_mock.return_value = p
    p.removeProp("jdk1.url")
    try:
      download_and_install_jdk(args)
      self.fail("Should throw exception")
    except FatalException:
      # Expected
      pass

    # Test case: JDK file does not exist, HTTP response does not
    # contain Content-Length
    p.process_pair("jdk1.url", "http://somewhere/somewhere.tar.gz")
    path_existsMock.return_value = False
    get_YN_input_mock.return_value = True
    get_validated_string_input_mock.return_value = "1"
    run_os_command_mock.return_value = (0, "Wrong out", None)
    try:
      download_and_install_jdk(args)
      self.fail("Should throw exception")
    except FatalException:
      # Expected
      pass

    # Successful JDK download
    args.java_home = None
    path_isfileMock.return_value = False
    args.jdk_location = None
    run_os_command_mock.return_value = (0, "Creating jdk1/jre" , None)
    statResult = MagicMock()
    statResult.st_size = 32000
    statMock.return_value = statResult
    rcode = download_and_install_jdk(args)
    self.assertEqual(0, rcode)

    # Test case: not accept the license"
    get_YN_input_mock.return_value = False
    download_and_install_jdk(args)
    self.assertTrue(exit_mock.called)

    # Test case: jdk is already installed, ensure that JCE check is skipped if -j option is not supplied.
    args.jdk_location = None
    get_JAVA_HOME_mock.return_value = "some_jdk"
    get_YN_input_mock.return_value = False
    path_existsMock.return_value = False
    force_download_file_mock.reset_mock()
    with patch("ambari_server.serverSetup.JDKSetup._download_jce_policy") as download_jce_policy_mock:
      rcode = download_and_install_jdk(args)
      self.assertFalse(download_jce_policy_mock.called)
      self.assertFalse(force_download_file_mock.called)

    # Test case: Update JAVA_HOME location using command: ambari-server setup -j %NEW_LOCATION%
    update_properties_mock.reset_mock()
    args.java_home = "somewhere"
    path_existsMock.side_effect = [True,False,False]
    get_JAVA_HOME_mock.return_value = "some_jdk"
    path_isfileMock.return_value = True
    download_and_install_jdk(args)
    self.assertTrue(update_properties_mock.call_count == 1)

    # Test case: Negative test case JAVA_HOME location should not be updated if -j option is supplied and
    # jce_policy file already exists in resources dir.
    #write_property_mock.reset_mock()
    #args.java_home = "somewhere"
    #path_existsMock.side_effect = None
    #path_existsMock.return_value = True
    #get_JAVA_HOME_mock.return_value = "some_jdk"
    #try:
    #  download_and_install_jdk(args)
    #  self.fail("Should throw exception")
    #except FatalException as fe:
      # Expected
    #  self.assertFalse(write_property_mock.called)

    # Test case: Setup ambari-server first time, Custom JDK selected, JDK exists
    args.java_home = None
    args.jdk_location = None
    update_properties_mock.reset_mock()
    path_existsMock.side_effect = None
    path_existsMock.return_value = True
    get_validated_string_input_mock.return_value = "2"
    get_JAVA_HOME_mock.return_value = None
    rcode = download_and_install_jdk(args)
    self.assertEqual(0, rcode)
    self.assertTrue(update_properties_mock.called)

    # Test case: Setup ambari-server first time, Custom JDK selected, JDK not exists
    update_properties_mock.reset_mock()
    path_existsMock.side_effect = [False,False,False]
    get_validated_string_input_mock.return_value = "2"
    get_JAVA_HOME_mock.return_value = None
    try:
      download_and_install_jdk(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      pass

    # Test when custom java home exists but java binary file doesn't exist
    args.java_home = None
    path_isfileMock.return_value = False
    update_properties_mock.reset_mock()
    path_existsMock.side_effect = [True,False,False]
    get_validated_string_input_mock.return_value = "2"
    get_JAVA_HOME_mock.return_value = None
    flag = False
    try:
      download_and_install_jdk(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      flag = True
      pass
    self.assertTrue(flag)

    #Test case: Setup ambari-server with java home passed. Path to java home doesn't not exists
    args.java_home = "somewhere"
    path_existsMock.return_value = False
    try:
      download_and_install_jdk(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      self.assertTrue("Path to java home somewhere or java binary file does not exists" in fe.reason)
      pass


  @patch("ambari_server.dbConfiguration_linux.run_os_command")
  def test_get_postgre_status(self, run_os_command_mock):

    run_os_command_mock.return_value = (1, "running", None)
    pg_status, retcode, out, err = PGConfig._get_postgre_status()
    self.assertEqual("running", pg_status)

    run_os_command_mock.return_value = (1, "wrong", None)
    pg_status, retcode, out, err = PGConfig._get_postgre_status()
    self.assertEqual(None, pg_status)


  @patch("time.sleep")
  @patch("subprocess.Popen")
  @patch("ambari_commons.os_linux.os_run_os_command")
  @patch.object(ambari__server, "get_postgre_status")
  def test_check_postgre_up(self, get_postgre_status_mock, run_os_command_mock,
                            popen_mock, sleep_mock):
    p = MagicMock()
    p.poll.return_value = 0
    p.communicate.return_value = (None, None)
    p.returncode = 0
    popen_mock.return_value = p
    get_postgre_status_mock.return_value = "running", 0, "", ""
    run_os_command_mock.return_value = (0, None, None)
    pg_status, retcode, out, err = ambari__server.check_postgre_up()
    self.assertEqual(0, retcode)

    ambari__server.OS_TYPE = OSConst.OS_SUSE
    p.poll.return_value = 4
    get_postgre_status_mock.return_value = "stopped", 0, "", ""
    pg_status, retcode, out, err = ambari__server.check_postgre_up()
    self.assertEqual(4, retcode)


  @patch("platform.linux_distribution")
  @patch("platform.system")
  @patch("ambari_commons.logging_utils.print_info_msg")
  @patch("ambari_commons.logging_utils.print_error_msg")
  @patch("ambari_server.serverSetup.get_ambari_properties")
  @patch("ambari_server.serverSetup.write_property")
  @patch("ambari_server.serverConfiguration.get_conf_dir")
  def test_configure_os_settings(self, get_conf_dir_mock, write_property_mock, get_ambari_properties_mock,
                                 print_error_msg_mock, print_info_msg_mock,
                                 systemMock, distMock):
    get_ambari_properties_mock.return_value = -1
    rcode = configure_os_settings()
    self.assertEqual(-1, rcode)

    p = MagicMock()
    p[OS_TYPE_PROPERTY] = 'somevalue'
    get_ambari_properties_mock.return_value = p
    rcode = configure_os_settings()
    self.assertEqual(0, rcode)

    p.__getitem__.return_value = ""
    systemMock.return_value = "NonLinux"
    rcode = configure_os_settings()
    self.assertEqual(-1, rcode)

    systemMock.return_value = "Linux"
    distMock.return_value = ("CentOS", "6.3", None)
    rcode = configure_os_settings()
    self.assertEqual(0, rcode)
    self.assertTrue(write_property_mock.called)


  @patch("__builtin__.open")
  @patch("ambari_server.serverConfiguration.Properties")
  @patch("ambari_server.serverConfiguration.search_file")
  @patch("ambari_server.serverConfiguration.get_conf_dir")
  def test_get_JAVA_HOME(self, get_conf_dir_mock, search_file_mock,
                         Properties_mock, openMock):
    openMock.side_effect = Exception("exception")
    result = get_JAVA_HOME()
    self.assertEqual(None, result)

    expected = os.path.dirname(__file__)
    p = MagicMock()
    p.__getitem__.return_value = expected
    openMock.side_effect = None
    Properties_mock.return_value = p
    result = get_JAVA_HOME()
    self.assertEqual(expected, result)

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  def test_prompt_db_properties_default(self,
                                        get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    args = MagicMock()
    properties = Properties()

    args.dbms_index = 0
    args.silent = True

    del args.dbms
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password

    factory = DBMSConfigFactory()
    dbConfig = factory.create(args, properties)
    dbConfig._prompt_db_properties()

    self.assertEqual(dbConfig.dbms, "postgres")
    self.assertEqual(dbConfig.database_host, "localhost")
    self.assertEqual(dbConfig.database_name, "ambari")
    self.assertEqual(dbConfig.database_port, "5432")

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.dbConfiguration_linux.read_password")
  @patch("ambari_server.dbConfiguration_linux.get_validated_string_input")
  @patch("ambari_server.dbConfiguration_linux.get_YN_input")
  def test_prompt_db_properties_oracle_sname(self, gyni_mock, gvsi_mock, rp_mock,
                                             get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    gyni_mock.return_value = True
    list_of_return_values = ["ambari-server", "ambari", "1", "1521", "localhost"]

    def side_effect(*args, **kwargs):
      return list_of_return_values.pop()

    gvsi_mock.side_effect = side_effect
    rp_mock.return_value = "password"

    args = MagicMock()
    properties = Properties()

    args.dbms_index = 1
    args.silent = False

    del args.dbms
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password

    factory = DBMSConfigFactory()
    dbConfig = factory.create(args, properties)
    dbConfig._prompt_db_properties()

    self.assertEqual(dbConfig.dbms, "oracle")
    self.assertEqual(dbConfig.database_port, "1521")
    self.assertEqual(dbConfig.database_host, "localhost")
    self.assertEqual(dbConfig.database_name, "ambari")
    self.assertEqual(dbConfig.database_username, "ambari-server")
    self.assertEqual(dbConfig.sid_or_sname, "sname")

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.dbConfiguration_linux.read_password")
  @patch("ambari_server.dbConfiguration_linux.get_validated_string_input")
  @patch("ambari_server.dbConfiguration_linux.get_YN_input")
  def test_prompt_db_properties_oracle_sid(self, gyni_mock, gvsi_mock, rp_mock,
                                           get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    gyni_mock.return_value = True
    list_of_return_values = ["ambari-server", "ambari", "2", "1521", "localhost"]

    def side_effect(*args, **kwargs):
      return list_of_return_values.pop()

    gvsi_mock.side_effect = side_effect
    rp_mock.return_value = "password"

    args = MagicMock()
    properties = Properties()

    args.dbms_index = 1
    args.silent = False

    del args.dbms
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password

    factory = DBMSConfigFactory()
    dbConfig = factory.create(args, properties)
    dbConfig._prompt_db_properties()

    self.assertEqual(dbConfig.dbms, "oracle")
    self.assertEqual(dbConfig.database_port, "1521")
    self.assertEqual(dbConfig.database_host, "localhost")
    self.assertEqual(dbConfig.database_name, "ambari")
    self.assertEqual(dbConfig.database_username, "ambari-server")
    self.assertEqual(dbConfig.sid_or_sname, "sid")

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.dbConfiguration_linux.read_password")
  @patch("ambari_server.dbConfiguration_linux.get_validated_string_input")
  @patch("ambari_server.dbConfiguration_linux.get_YN_input")
  def test_prompt_db_properties_postgre_adv(self, gyni_mock, gvsi_mock, rp_mock,
                                            get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    gyni_mock.return_value = True
    list_of_return_values = ["ambari-server", "ambari", "ambari"]

    def side_effect(*args, **kwargs):
      return list_of_return_values.pop()

    gvsi_mock.side_effect = side_effect
    rp_mock.return_value = "password"

    args = MagicMock()
    properties = Properties()

    args.dbms_index = 0
    args.silent = False

    del args.dbms
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password

    factory = DBMSConfigFactory()
    dbConfig = factory.create(args, properties)
    dbConfig._prompt_db_properties()

    self.assertEqual(dbConfig.dbms, "postgres")
    self.assertEqual(dbConfig.database_port, "5432")
    self.assertEqual(dbConfig.database_host, "localhost")
    self.assertEqual(dbConfig.database_name, "ambari")
    self.assertEqual(dbConfig.database_username, "ambari-server")
    self.assertEqual(dbConfig.sid_or_sname, "sname")

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.dbConfiguration_linux.store_password_file")
  @patch("ambari_server.dbConfiguration_linux.read_password")
  @patch("ambari_server.dbConfiguration_linux.get_validated_string_input")
  @patch("ambari_server.dbConfiguration_linux.get_YN_input")
  def test_prompt_db_properties_for_each_database_type(self, gyni_mock, gvsi_mock, rp_mock, spf_mock,
                                                       get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    """
    :return: Validates that installation for each database type correctly stores the database type, database name,
    and optionally the postgres schema name.
    """
    gyni_mock.return_value = True
    rp_mock.return_value = "password"
    spf_mock.return_value = "encrypted password"

    # Values to use while installing several database types
    hostname = "localhost"
    db_name = "db_ambari"
    postgres_schema = "sc_ambari"
    port = "1234"
    oracle_service = "1"
    oracle_service_name = "ambari"
    user_name = "ambari"

    # Input values
    postgres_embedded_values = [db_name, postgres_schema, hostname]
    oracle_values = [hostname, port, oracle_service, oracle_service_name, user_name]
    mysql_values = [hostname, port, db_name, user_name]
    postgres_external_values = [hostname, port, db_name, postgres_schema, user_name]

    list_of_return_values = postgres_embedded_values + oracle_values + mysql_values + postgres_external_values
    list_of_return_values = list_of_return_values[::-1]       # Reverse the list since the input will be popped

    def side_effect(*args, **kwargs):
      return list_of_return_values.pop()
    gvsi_mock.side_effect = side_effect

    if AMBARI_CONF_VAR in os.environ:
      del os.environ[AMBARI_CONF_VAR]

    tempdir = tempfile.gettempdir()
    os.environ[AMBARI_CONF_VAR] = tempdir

    for i in range(0, 4):
        # Use the expected path of the ambari.properties file to delete it if it exists, and then create a new one
        # during each use case.
        prop_file = os.path.join(tempdir, "ambari.properties")
        if os.path.exists(prop_file):
          os.remove(prop_file)
        with open(prop_file, "w") as f:
          f.write("server.jdbc.database_name=oldDBName")
        f.close()
        ambari__server.AMBARI_PROPERTIES_FILE = prop_file

        args = MagicMock()
        properties = Properties()

        args.dbms_index = i
        args.silent = False

        del args.dbms
        del args.database_host
        del args.database_port
        del args.database_name
        del args.database_username
        del args.database_password

        factory = DBMSConfigFactory()
        dbConfig = factory.create(args, properties)
        dbConfig._prompt_db_properties()

        if dbConfig._is_local_database():
          dbConfig._setup_local_server(properties)
        else:
          dbConfig._setup_remote_server(properties)

        if i == 0:
          # Postgres Embedded
          self.assertEqual(properties[JDBC_DATABASE_PROPERTY], "postgres")
          self.assertEqual(properties[JDBC_DATABASE_NAME_PROPERTY], db_name)
          self.assertEqual(properties[JDBC_POSTGRES_SCHEMA_PROPERTY], postgres_schema)
          self.assertEqual(properties[PERSISTENCE_TYPE_PROPERTY], "local")
        elif i == 1:
          # Oracle
          self.assertEqual(properties[JDBC_DATABASE_PROPERTY], "oracle")
          self.assertFalse(JDBC_POSTGRES_SCHEMA_PROPERTY in properties.propertyNames())
          self.assertEqual(properties[PERSISTENCE_TYPE_PROPERTY], "remote")
        elif i == 2:
          # MySQL
          self.assertEqual(properties[JDBC_DATABASE_PROPERTY], "mysql")
          self.assertFalse(JDBC_POSTGRES_SCHEMA_PROPERTY in properties.propertyNames())
          self.assertEqual(properties[PERSISTENCE_TYPE_PROPERTY], "remote")
        elif i == 3:
          # Postgres External
          self.assertEqual(properties[JDBC_DATABASE_PROPERTY], "postgres")
          self.assertEqual(properties[JDBC_DATABASE_NAME_PROPERTY], db_name)
          self.assertEqual(properties[JDBC_POSTGRES_SCHEMA_PROPERTY], postgres_schema)
          self.assertEqual(properties[PERSISTENCE_TYPE_PROPERTY], "remote")

  @patch.object(os.path, "exists")
  def test_validate_jdk(self, exists_mock):
    exists_mock.side_effect = [False]
    result = ambari__server.validate_jdk("path")
    self.assertFalse(result)

    exists_mock.side_effect = [True, False]
    result = ambari__server.validate_jdk("path")
    self.assertFalse(result)

    exists_mock.side_effect = [True, True]
    result = ambari__server.validate_jdk("path")
    self.assertTrue(result)

  @patch("glob.glob")
  @patch.object(ambari__server, "get_JAVA_HOME")
  @patch.object(ambari__server, "validate_jdk")
  def test_find_jdk(self, validate_jdk_mock, get_JAVA_HOME_mock, globMock):
    get_JAVA_HOME_mock.return_value = "somewhere"
    validate_jdk_mock.return_value = True
    result = ambari__server.find_jdk()
    self.assertEqual("somewhere", result)

    get_JAVA_HOME_mock.return_value = None
    globMock.return_value = []
    result = ambari__server.find_jdk()
    self.assertEqual(None, result)

    globMock.return_value = ["one", "two"]
    result = ambari__server.find_jdk()
    self.assertNotEqual(None, result)

    globMock.return_value = ["one", "two"]
    validate_jdk_mock.side_effect = [False, True]
    result = ambari__server.find_jdk()
    self.assertEqual(result, "two")

  @patch.object(FirewallChecks, "run_os_command")
  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("os.path.exists")
  @patch("os.path.isfile")
  @patch("ambari_commons.os_utils.remove_file")
  @patch("ambari_server.dbConfiguration_linux.LinuxDBMSConfig.ensure_jdbc_driver_installed")
  @patch("ambari_server.dbConfiguration_linux.PGConfig._setup_local_database")
  @patch("ambari_server.dbConfiguration_linux.get_YN_input")
  @patch("ambari_server.serverSetup.update_properties")
  @patch("ambari_server.dbConfiguration_linux.get_ambari_properties")
  @patch("ambari_server.dbConfiguration_linux.store_password_file")
  @patch("ambari_server.dbConfiguration_linux.PGConfig._is_jdbc_user_changed")
  @patch("ambari_server.serverSetup.verify_setup_allowed")
  @patch("ambari_server.dbConfiguration_linux.read_password")
  @patch("ambari_server.dbConfiguration_linux.get_validated_string_input")
  @patch("ambari_server.dbConfiguration.get_validated_string_input")
  @patch("ambari_server.serverSetup.get_YN_input")
  @patch("ambari_server.serverSetup.get_ambari_properties")
  @patch("ambari_server.serverSetup.configure_os_settings")
  @patch("ambari_server.serverSetup.download_and_install_jdk")
  # @patch.object(ambari__server, "configure_postgres")
  # @patch.object(ambari__server, "setup_db")
  # @patch.object(ambari__server, "check_postgre_up")
  @patch("ambari_server.serverSetup.check_ambari_user")
  @patch("ambari_server.serverSetup.check_jdbc_drivers")
  @patch("ambari_server.serverSetup.disable_security_enhancements")
  # @patch.object(ambari__server, "setup_remote_db")
  # @patch.object(ambari__server, "store_remote_properties")
  # @patch.object(ambari__server, "is_local_database")
  # @patch.object(ambari__server, "store_local_properties")
  @patch("ambari_server.serverSetup.is_root")
  # @patch.object(ambari__server, 'is_server_runing')
  @patch("ambari_server.serverSetup.proceedJDBCProperties")
  @patch("ambari_server.serverSetup.extract_views")
  @patch("ambari_server.serverSetup.adjust_directory_permissions")
  @patch("ambari_server.serverSetup.read_ambari_user")
  def test_setup(self, read_ambari_user_mock, adjust_dirs_mock, extract_views_mock, proceedJDBCProperties_mock, is_root_mock,
                 disable_security_enhancements_mock, check_jdbc_drivers_mock, check_ambari_user_mock,
                 download_jdk_mock, configure_os_settings_mock, get_ambari_properties_mock,
                 get_YN_input_mock, gvsi_mock, gvsi_1_mock,
                 read_password_mock, verify_setup_allowed_method, is_jdbc_user_changed_mock,
                 store_password_file_mock, get_ambari_properties_1_mock, update_properties_mock,
                 get_YN_input_1_mock, setup_local_database_mock, ensure_jdbc_driver_installed_mock,
                 remove_file_mock, isfile_mock, exists_mock,
                 get_os_major_version_mock, get_os_type_mock,get_os_family_mock, run_os_command_mock):
    hostname = "localhost"
    db_name = "db_ambari"
    postgres_schema = "sc_ambari"
    port = "1234"
    oracle_service = "1"
    oracle_service_name = "ambari"
    user_name = "ambari"

    args = MagicMock()

    del args.dbms
    del args.dbms_index
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password

    args.silent = False

    failed = False
    properties = Properties()

    get_YN_input_mock.return_value = False
    isfile_mock.return_value = False
    verify_setup_allowed_method.return_value = 0
    exists_mock.return_value = False
    remove_file_mock.return_value = 0
    get_os_type_mock.return_value = ""
    get_os_family_mock.return_value = OSConst.REDHAT_FAMILY
    run_os_command_mock.return_value = 3,"",""
    extract_views_mock.return_value = 0
    read_ambari_user_mock.return_value = "ambari"
    read_password_mock.return_value = "bigdata2"
    get_ambari_properties_mock.return_value = properties
    get_ambari_properties_1_mock.return_value = properties
    store_password_file_mock.return_value = "encrypted_bigdata2"
    ensure_jdbc_driver_installed_mock.return_value = True

    def reset_mocks():
      is_jdbc_user_changed_mock.reset_mock()
      is_root_mock.reset_mock()
      disable_security_enhancements_mock.reset_mock()
      check_jdbc_drivers_mock.reset_mock()
      check_ambari_user_mock.reset_mock()
      run_os_command_mock.reset_mock()
      configure_os_settings_mock.reset_mock()
      setup_local_database_mock.reset_mock()
      get_YN_input_1_mock.reset_mock()
      update_properties_mock.reset_mock()
      pass


    # Testing call under non-root
    is_root_mock.return_value = False
    try:
      setup(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertTrue("root-level" in fe.reason)
      pass

    # Testing calls under root
    # remote case
    is_root_mock.return_value = True
    disable_security_enhancements_mock.return_value = (0, "")
    check_ambari_user_mock.return_value = 0
    check_jdbc_drivers_mock.return_value = 0
    download_jdk_mock.return_value = 0
    configure_os_settings_mock.return_value = 0
    args.jdbc_driver= None
    args.jdbc_db = None

    result = setup(args)

    self.assertEqual(None, result)
    self.assertTrue(check_ambari_user_mock.called)
    self.assertEqual(1, run_os_command_mock.call_count)

    #Local case
    reset_mocks()

    # Input values
    db_selection_values = ["1"]
    postgres_values = [db_name, postgres_schema, hostname]

    postgres_values = postgres_values[::-1]       # Reverse the list since the input will be popped

    def side_effect(*args, **kwargs):
      return db_selection_values.pop()
    gvsi_mock.side_effect = side_effect

    def side_effect_1(*args, **kwargs):
      return postgres_values.pop()
    gvsi_1_mock.side_effect = side_effect_1

    get_YN_input_mock.return_value = True
    # is_local_database_mock.return_value = True
    is_jdbc_user_changed_mock.return_value = False

    try:
      result = setup(args)
    except FatalException:
      self.fail("Setup should be successful")
    self.assertEqual(None, result)
    self.assertTrue(is_jdbc_user_changed_mock.called)
    self.assertTrue(update_properties_mock.called)
    self.assertTrue(setup_local_database_mock.called)
    self.assertFalse(remove_file_mock.called)

    #if DB user name was changed
    reset_mocks()

    # is_local_database_mock.return_value = True
    is_jdbc_user_changed_mock.return_value = True

    db_selection_values = ["1"]
    postgres_values = [db_name, postgres_schema, hostname]

    postgres_values = postgres_values[::-1]       # Reverse the list since the input will be popped

    try:
      result = setup(args)
    except FatalException:
      self.fail("Setup should be successful")
    self.assertEqual(None, result)
    self.assertTrue(is_jdbc_user_changed_mock.called)
    self.assertTrue(update_properties_mock.called)
    self.assertTrue(setup_local_database_mock.called)
    self.assertFalse(remove_file_mock.called)

    #negative case
    reset_mocks()

    # Use remote database
    get_YN_input_1_mock.return_value = False
    db_selection_values = ["4"]
    postgres_values = [hostname, port, db_name, postgres_schema, user_name]

    postgres_values = postgres_values[::-1]       # Reverse the list since the input will be popped

    try:
      result = setup(args)
      self.fail("Should throw exception")
    except NonFatalException as fe:
      self.assertTrue("Remote database setup aborted." in fe.reason)

    self.assertFalse(setup_local_database_mock.called)

    # test not run setup if ambari-server setup executed with jdbc properties
    reset_mocks()
    # is_server_runing_mock.return_value = (False, 1)
    args.jdbc_driver= "path/to/driver"
    args.jdbc_db = "test_db_name"


    setup(args)
    self.assertTrue(proceedJDBCProperties_mock.called)
    self.assertFalse(disable_security_enhancements_mock.called)
    self.assertFalse(check_ambari_user_mock.called)

  @patch.object(ambari__server, 'get_remote_script_line')
  @patch.object(ambari__server, 'is_server_runing')
  @patch.object(ambari__server, "get_YN_input")
  @patch.object(ambari__server, "setup_db")
  @patch.object(ambari__server, "print_info_msg")
  @patch.object(ambari__server, "run_os_command")
  @patch.object(ambari__server, "parse_properties_file")
  @patch.object(ambari__server, "is_root")
  @patch.object(ambari__server, "check_database_name_property")
  def test_reset(self, check_database_name_property_mock, is_root_mock,
                 parse_properties_file_mock,
                 run_os_command_mock, print_info_msg_mock,
                 setup_db_mock, get_YN_inputMock, is_server_running_mock,
                 get_remote_script_line_mock):

    parse_properties_file_mock.return_value = 0
    args = MagicMock()
    args.persistence_type = "local"
    get_YN_inputMock.return_value = False
    is_server_running_mock.return_value = (False, 0)
    setup_db_mock.side_effect = [(0,None, None),(0,None, "ERROR: database 'ambari' is being accessed by other users"), (0, None, "ERROR: user 'mapred' already exist")]

    # Testing call under non-root
    is_root_mock.return_value = False
    try:
      ambari__server.reset(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertTrue("root-level" in fe.reason)
      pass

    # Testing calls under root
    is_root_mock.return_value = True
    try:
      ambari__server.reset(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertFalse("root-level" in fe.reason)
      pass

    get_YN_inputMock.return_value = True
    run_os_command_mock.return_value = (1, None, None)
    try:
      ambari__server.reset(args)
      self.fail("Should throw exception")
    except FatalException:
      # Expected
      pass

    run_os_command_mock.return_value = (0, None, None)
    ambari__server.reset(args)
    self.assertTrue(setup_db_mock.called)

    # Database errors cases
    is_server_running_mock.side_effect = [(True, 123), (False, 0), (False, 0), (False, 0), (False, 0)]

    try:
      ambari__server.reset(args)
      self.fail("Should throw exception")
    except FatalException:
      # Expected
      pass

    try:
      ambari__server.reset(args)
      self.fail("Should throw exception")
    except NonFatalException:
      # Expected
      pass

    get_remote_script_line_mock.return_value = None
    try:
      #remote db case
      args.persistence_type = "remote"
      ambari__server.reset(args)
      self.fail("Should throw exception")
    except NonFatalException:
      # Expected
      pass

    get_remote_script_line_mock.reset_mock()
    get_remote_script_line_mock.side_effect = ["drop", "create"]
    try:
      #remote db case (not Postgres)
      args.persistence_type = "remote"
      rcode = ambari__server.reset(args)
      self.fail("Should throw exception")
    except NonFatalException:
      # Expected
      self.assertTrue(get_remote_script_line_mock.called)
      pass

  @patch.object(ambari__server, "get_YN_input")
  @patch("__builtin__.raw_input")
  @patch.object(ambari__server, "is_root")
  def test_reset_default(self, is_root_mock, raw_input_mock, get_YN_inputMock):
    is_root_mock.return_value=True
    get_YN_inputMock.return_value = False
    raw_input_mock.return_value=""
    args = MagicMock()

    try:
      ambari__server.reset(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertTrue(fe.code == 1)
      pass

    pass


  @patch.object(ambari__server, "setup_db")
  @patch.object(ambari__server, "print_info_msg")
  @patch.object(ambari__server, "run_os_command")
  @patch.object(ambari__server, "parse_properties_file")
  @patch.object(ambari__server, "is_root")
  @patch.object(ambari__server, "check_database_name_property")
  @patch.object(ambari__server, 'is_server_runing')
  def test_silent_reset(self, is_server_runing_mock, check_database_name_property_mock, is_root_mock, parse_properties_file_mock,
                        run_os_command_mock, print_info_msg_mock,
                        setup_db_mock):
    is_root_mock.return_value = True

    args = MagicMock()
    ambari__server.SILENT = True
    self.assertTrue(ambari__server.SILENT)
    setup_db_mock.return_value = (0, None, None)
    run_os_command_mock.return_value = (0, None, None)
    is_server_runing_mock.return_value = (False, 0)

    def signal_handler(signum, frame):
      self.fail("Timed out!")

    signal.signal(signal.SIGALRM, signal_handler)
    signal.alarm(5)
    rcode = ambari__server.reset(args)
    signal.alarm(0)
    self.assertEqual(None, rcode)
    self.assertTrue(setup_db_mock.called)

  @patch("ambari_server.utils.looking_for_pid")
  @patch("ambari_server.utils.wait_for_pid")
  @patch("ambari_server.utils.save_main_pid_ex")
  @patch("ambari_server.utils.check_exitcode")
  @patch('os.makedirs')
  @patch("ambari_server.utils.locate_file")
  @patch.object(ambari__server, 'is_server_runing')
  @patch("os.chown")
  @patch("pwd.getpwnam")
  @patch.object(ambari__server, 'get_master_key_location')
  @patch.object(ambari__server, 'save_master_key')
  @patch('os.chmod', autospec=True)
  @patch.object(ambari__server, "get_validated_string_input")
  @patch("os.environ")
  @patch.object(ambari__server, "get_ambari_properties")
  @patch("os.path.exists")
  @patch("__builtin__.open")
  @patch("subprocess.Popen")
  @patch.object(ambari__server, "print_info_msg")
  @patch.object(ambari__server, "search_file")
  @patch.object(ambari__server, "find_jdk")
  @patch.object(ambari__server, "print_error_msg")
  @patch.object(ambari__server, "check_postgre_up")
  @patch.object(ambari__server, "parse_properties_file")
  @patch.object(ambari__server, "read_ambari_user")
  @patch.object(ambari__server, "is_root")
  @patch.object(ambari__server, "find_jdbc_driver")
  @patch("getpass.getuser")
  @patch("os.chdir")
  @patch.object(ResourceFilesKeeper, "perform_housekeeping")
  def test_start(self, perform_housekeeping_mock, chdir_mock, getuser_mock,
                 find_jdbc_driver_mock, is_root_mock, read_ambari_user_mock,
                 parse_properties_file_mock, check_postgre_up_mock,
                 print_error_msg_mock, find_jdk_mock, search_file_mock,
                 print_info_msg_mock, popenMock, openMock, pexistsMock,
                 get_ambari_properties_mock, os_environ_mock,
                 get_validated_string_input_method, os_chmod_method,
                 save_master_key_method, get_master_key_location_method,
                 getpwnam_mock, os_chown_mock, is_server_running_mock, locate_file_mock,
                 os_makedirs_mock, check_exitcode_mock, save_main_pid_ex_mock,
                 wait_for_pid_mock, looking_for_pid_mock):
     args = MagicMock()
     locate_file_mock.side_effect = lambda *args: '/bin/su' if args[0] == 'su' else '/bin/sh'
     f = MagicMock()
     f.readline.return_value = 42
     openMock.return_value = f

     looking_for_pid_mock.return_value = [{
          "pid": "777",
          "exe": "/test",
          "cmd": "test arg"
     }]
     wait_for_pid_mock.return_value = 1
     check_exitcode_mock.return_value = 0

     p = get_ambari_properties_mock.return_value
     p.get_property.return_value = 'False'
     search_file_mock.return_value = None
     is_server_running_mock.return_value = (True, 123)
     pw = MagicMock()
     pw.setattr('pw_uid', 0)
     pw.setattr('pw_gid', 0)
     getpwnam_mock.return_value = pw
     os_chown_mock.return_value = None
     # Checking "server is running"
     pexistsMock.return_value = True
     try:
       ambari__server.start(args)
       self.fail("Should fail with 'Server is running'")
     except FatalException:
       # Expected
       pass

     parse_properties_file_mock.reset_mock()

     pexistsMock.return_value = False

     # Checking situation when ambari user is not set up
     read_ambari_user_mock.return_value = None
     try:
       ambari__server.start(args)
       self.fail("Should fail with 'Can not detect a system user for Ambari'")
     except FatalException as e:
       # Expected
       self.assertTrue('Unable to detect a system user for Ambari Server.' in e.reason)

     parse_properties_file_mock.reset_mock()

     # Checking start from non-root when current user is not the same as a
     # custom user
     read_ambari_user_mock.return_value = "dummy-user"
     getuser_mock.return_value = "non_custom_user"
     is_root_mock.return_value = False
     try:
       ambari__server.start(args)
       self.fail("Should fail with 'Can not start ambari-server as user...'")
     except FatalException as e:
       # Expected
       self.assertTrue('Unable to start Ambari Server as user' in e.reason)
       self.assertFalse(parse_properties_file_mock.called)

     parse_properties_file_mock.reset_mock()

     # Checking "jdk not found"
     is_root_mock.return_value = True
     find_jdk_mock.return_value = None
     is_server_running_mock.return_value = (False, 0)

     try:
       ambari__server.start(args)
       self.fail("Should fail with 'No JDK found'")
     except FatalException as e:
       # Expected
       self.assertTrue('No JDK found' in e.reason)

     find_jdk_mock.return_value = "somewhere"

     parse_properties_file_mock.reset_mock()

     ## Testing workflow under root
     is_root_mock.return_value = True

     # Remote DB
     args.persistence_type = "remote"
     args.dbms = "oracle"

     # Case when jdbc driver is not used
     find_jdbc_driver_mock.return_value = -1
     try:
       ambari__server.start(args)
       self.fail("Should fail with exception")
     except FatalException as e:
       self.assertTrue('Before starting Ambari Server' in e.reason)

     find_jdbc_driver_mock.reset_mock()
     find_jdbc_driver_mock.return_value = 0
     try:
       ambari__server.start(args)
     except FatalException as e:
       # Ignored
       pass

     # Test exception handling on resource files housekeeping
     perform_housekeeping_mock.reset_mock()
     perform_housekeeping_mock.side_effect = KeeperException("some_reason")
     try:
       ambari__server.start(args)
       self.fail("Should fail with exception")
     except FatalException as e:
       self.assertTrue('some_reason' in e.reason)
     self.assertTrue(perform_housekeeping_mock.called)
     perform_housekeeping_mock.side_effect = lambda *v, **kv : None
     perform_housekeeping_mock.reset_mock()

     self.assertFalse('Unable to start PostgreSQL server' in e.reason)
     self.assertFalse(check_postgre_up_mock.called)

     check_postgre_up_mock.reset_mock()
     parse_properties_file_mock.reset_mock()

     # Local DB
     args.persistence_type = "local"

     # case: postgres failed to start
     check_postgre_up_mock.return_value = None, 1, "Unable to start PostgreSQL serv", "error"
     try:
       ambari__server.start(args)
       self.fail("Should fail with 'Unable to start PostgreSQL server'")
     except FatalException as e:
       # Expected
       self.assertTrue('Unable to start PostgreSQL server' in e.reason)
       self.assertTrue(check_postgre_up_mock.called)

     parse_properties_file_mock.reset_mock()

     check_postgre_up_mock.return_value = "running", 0, "success", ""

     # Case: custom user is "root"
     read_ambari_user_mock.return_value = "root"
     ambari__server.start(args)
     self.assertTrue(popenMock.called)
     popen_arg = popenMock.call_args[0][0]
     self.assertTrue(popen_arg[0] == "/bin/sh")
     self.assertTrue(perform_housekeeping_mock.called)
     perform_housekeeping_mock.reset_mock()
     popenMock.reset_mock()

     parse_properties_file_mock.reset_mock()

     # Case: custom user is  not "root"
     read_ambari_user_mock.return_value = "not-root-user"
     ambari__server.start(args)
     self.assertTrue(chdir_mock.called)
     self.assertTrue(popenMock.called)
     popen_arg = popenMock.call_args_list[0][0][0]
     self.assertTrue(popen_arg[0] == "/bin/su")
     self.assertTrue(perform_housekeeping_mock.called)
     check_postgre_up_mock.reset_mock()

     popenMock.reset_mock()
     parse_properties_file_mock.reset_mock()

     ## Testing workflow under non-root
     is_root_mock.return_value = False
     read_ambari_user_mock.return_value = "not-root-user"
     getuser_mock.return_value = read_ambari_user_mock.return_value

     parse_properties_file_mock.reset_mock()

     # Local DB
     args.persistence_type = "local"

     ambari__server.start(args)

     self.assertFalse(check_postgre_up_mock.called)

     parse_properties_file_mock.reset_mock()

     # Remote DB
     args.persistence_type = "remote"

     ambari__server.start(args)

     self.assertFalse(check_postgre_up_mock.called)

     parse_properties_file_mock.reset_mock()

     # Checking call
     ambari__server.start(args)
     self.assertTrue(popenMock.called)
     popen_arg = popenMock.call_args[0][0]
     self.assertTrue(popen_arg[0] == "/bin/sh")

     parse_properties_file_mock.reset_mock()

     # Test start under wrong user
     read_ambari_user_mock.return_value = "not-root-user"
     getuser_mock.return_value = "non_custom_user"
     try:
       ambari__server.start(args)
       self.fail("Can not start ambari-server as user non_custom_user.")
     except FatalException as e:
       # Expected
       self.assertTrue('Unable to start Ambari Server as user' in e.reason)

     parse_properties_file_mock.reset_mock()

     # Check environ master key is set
     popenMock.reset_mock()
     os_environ_mock.copy.return_value = {"a": "b",
                                          ambari__server.SECURITY_KEY_ENV_VAR_NAME: "masterkey"}
     args.persistence_type = "local"
     read_ambari_user_mock.return_value = "root"
     is_root_mock.return_value = True

     ambari__server.start(args)

     self.assertFalse(get_validated_string_input_method.called)
     self.assertFalse(save_master_key_method.called)
     popen_arg = popenMock.call_args[1]['env']
     self.assertEquals(os_environ_mock.copy.return_value, popen_arg)

     parse_properties_file_mock.reset_mock()

     # Check environ master key is not set
     popenMock.reset_mock()
     os_environ_mock.reset_mock()
     p.get_property.return_value = 'True'
     os_environ_mock.copy.return_value = {"a": "b"}
     args.persistence_type = "local"
     read_ambari_user_mock.return_value = "root"
     is_root_mock.return_value = True
     get_validated_string_input_method.return_value = "masterkey"
     os_chmod_method.return_value = None

     ambari__server.start(args)

     self.assertTrue(get_validated_string_input_method.called)
     self.assertTrue(save_master_key_method.called)
     popen_arg = popenMock.call_args[1]['env']
     self.assertEquals(os_environ_mock.copy.return_value, popen_arg)


  @patch.object(ambari__server, 'is_server_runing')
  @patch("os.remove")
  @patch("os.killpg")
  @patch("os.getpgid")
  @patch("ambari_commons.logging_utils.print_info_msg")
  def test_stop(self, print_info_msg_mock, gpidMock, removeMock,
                killMock, isServerRuningMock):
    isServerRuningMock.return_value = (True, 123)

    ambari__server.stop(None)

    self.assertTrue(killMock.called)
    self.assertTrue(removeMock.called)

  @patch.object(BackupRestore, "main")
  def test_backup(self, bkrestore_mock):
    ambari__server.backup("/some/path/file.zip")
    self.assertTrue(bkrestore_mock.called)

  @patch.object(BackupRestore, "main")
  def test_backup_no_path(self, bkrestore_mock):
    ambari__server.backup(None)
    self.assertTrue(bkrestore_mock.called)

  @patch.object(BackupRestore, "main")
  def test_restore(self, bkrestore_mock):
    ambari__server.restore("/some/path/file.zip")
    self.assertTrue(bkrestore_mock.called)

  @patch.object(BackupRestore, "main")
  def test_restore_no_path(self, bkrestore_mock):
    ambari__server.restore(None)
    self.assertTrue(bkrestore_mock.called)

  @patch.object(ambari__server, "is_root")
  @patch.object(ambari__server, "check_database_name_property")
  @patch.object(ambari__server, "run_stack_upgrade")
  def test_upgrade_stack(self, run_stack_upgrade_mock,
                         check_database_name_property_mock, is_root_mock):
    args = MagicMock()
    args.persistence_type = "local"

    # Testing call under non-root
    is_root_mock.return_value = False
    try:
      ambari__server.upgrade_stack(args, 'HDP-2.0')
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertTrue("root-level" in fe.reason)
      pass

    # Testing calls under root
    is_root_mock.return_value = True
    run_stack_upgrade_mock.return_value = 0
    ambari__server.upgrade_stack(args, 'HDP-2.0')

    self.assertTrue(run_stack_upgrade_mock.called)
    run_stack_upgrade_mock.assert_called_with("HDP", "2.0", None, None)

  @patch.object(ambari__server, 'get_conf_dir')
  @patch.object(ambari__server, 'get_ambari_classpath')
  @patch.object(ambari__server, 'run_os_command')
  @patch.object(ambari__server, 'find_jdk')
  def test_run_stack_upgrade(self, jdk_path_mock, run_os_command_mock,
                             get_ambari_classpath_mock, get_conf_dir_mock):
    jdk_path_mock.return_value = "/usr/lib/java"
    run_os_command_mock.return_value = (0, None, None)
    get_ambari_classpath_mock.return_value = 'test:path12'
    get_conf_dir_mock.return_value = '/etc/conf'
    stackIdMap = {'HDP' : '2.0'}

    ambari__server.run_stack_upgrade('HDP', '2.0', None, None)

    self.assertTrue(jdk_path_mock.called)
    self.assertTrue(get_ambari_classpath_mock.called)
    self.assertTrue(get_conf_dir_mock.called)
    self.assertTrue(run_os_command_mock.called)
    run_os_command_mock.assert_called_with('/usr/lib/java/bin/java -cp /etc/conf:test:path12 '
                                          'org.apache.ambari.server.upgrade.StackUpgradeHelper '
                                          'updateStackId ' + "'" + json.dumps(stackIdMap) + "'" +
                                          ' > /var/log/ambari-server/ambari-server.out 2>&1')

  @patch.object(ambari__server, 'get_conf_dir')
  @patch.object(ambari__server, 'get_ambari_classpath')
  @patch.object(ambari__server, 'run_os_command')
  @patch.object(ambari__server, 'find_jdk')
  def test_run_stack_upgrade_with_url(self, jdk_path_mock, run_os_command_mock,
                             get_ambari_classpath_mock, get_conf_dir_mock):
    jdk_path_mock.return_value = "/usr/lib/java"
    run_os_command_mock.return_value = (0, None, None)
    get_ambari_classpath_mock.return_value = 'test:path12'
    get_conf_dir_mock.return_value = '/etc/conf'
    stackIdMap = {'HDP' : '2.0', 'repo_url' : 'http://test.com'}

    ambari__server.run_stack_upgrade('HDP', '2.0', 'http://test.com', None)

    self.assertTrue(jdk_path_mock.called)
    self.assertTrue(get_ambari_classpath_mock.called)
    self.assertTrue(get_conf_dir_mock.called)
    self.assertTrue(run_os_command_mock.called)
    run_os_command_mock.assert_called_with('/usr/lib/java/bin/java -cp /etc/conf:test:path12 '
                                          'org.apache.ambari.server.upgrade.StackUpgradeHelper '
                                          'updateStackId ' + "'" + json.dumps(stackIdMap) + "'" +
                                          ' > /var/log/ambari-server/ambari-server.out 2>&1')

  @patch.object(ambari__server, 'get_conf_dir')
  @patch.object(ambari__server, 'get_ambari_classpath')
  @patch.object(ambari__server, 'run_os_command')
  @patch.object(ambari__server, 'find_jdk')
  def test_run_stack_upgrade_with_url_os(self, jdk_path_mock, run_os_command_mock,
                             get_ambari_classpath_mock, get_conf_dir_mock):
    jdk_path_mock.return_value = "/usr/lib/java"
    run_os_command_mock.return_value = (0, None, None)
    get_ambari_classpath_mock.return_value = 'test:path12'
    get_conf_dir_mock.return_value = '/etc/conf'
    stackIdMap = {'HDP' : '2.0', 'repo_url': 'http://test.com', 'repo_url_os': 'centos5,centos6'}

    ambari__server.run_stack_upgrade('HDP', '2.0', 'http://test.com', 'centos5,centos6')

    self.assertTrue(jdk_path_mock.called)
    self.assertTrue(get_ambari_classpath_mock.called)
    self.assertTrue(get_conf_dir_mock.called)
    self.assertTrue(run_os_command_mock.called)
    run_os_command_mock.assert_called_with('/usr/lib/java/bin/java -cp /etc/conf:test:path12 '
                                          'org.apache.ambari.server.upgrade.StackUpgradeHelper '
                                          'updateStackId ' + "'" + json.dumps(stackIdMap) + "'" +
                                          ' > /var/log/ambari-server/ambari-server.out 2>&1')


  @patch.object(ambari__server, 'get_conf_dir')
  @patch.object(ambari__server, 'get_ambari_classpath')
  @patch.object(ambari__server, 'run_os_command')
  @patch.object(ambari__server, 'find_jdk')
  def test_run_schema_upgrade(self, jdk_path_mock, run_os_command_mock,
                              get_ambari_classpath_mock, get_conf_dir_mock):
    jdk_path_mock.return_value = "/usr/lib/java"
    run_os_command_mock.return_value = (0, None, None)
    get_ambari_classpath_mock.return_value = 'test:path12'
    get_conf_dir_mock.return_value = '/etc/conf'

    ambari__server.run_schema_upgrade()

    self.assertTrue(jdk_path_mock.called)
    self.assertTrue(get_ambari_classpath_mock.called)
    self.assertTrue(get_conf_dir_mock.called)
    self.assertTrue(run_os_command_mock.called)
    run_os_command_mock.assert_called_with('/usr/lib/java/bin/java -cp /etc/conf:test:path12 '
                                           'org.apache.ambari.server.upgrade.SchemaUpgradeHelper '
                                           '> /var/log/ambari-server/ambari-server.out 2>&1')


  @patch.object(ambari__server, 'get_conf_dir')
  @patch.object(ambari__server, 'get_ambari_classpath')
  @patch.object(ambari__server, 'run_os_command')
  @patch.object(ambari__server, 'find_jdk')
  def test_run_metainfo_upgrade(self, jdk_path_mock, run_os_command_mock,
                                get_ambari_classpath_mock, get_conf_dir_mock):
    jdk_path_mock.return_value = "/usr/lib/java"
    run_os_command_mock.return_value = (0, None, None)
    get_ambari_classpath_mock.return_value = 'test:path12'
    get_conf_dir_mock.return_value = '/etc/conf'

    json_map = {'a': 'http://newurl'}
    ambari__server.run_metainfo_upgrade(json_map)

    self.assertTrue(jdk_path_mock.called)
    self.assertTrue(get_ambari_classpath_mock.called)
    self.assertTrue(get_conf_dir_mock.called)
    self.assertTrue(run_os_command_mock.called)
    run_os_command_mock.assert_called_with('/usr/lib/java/bin/java -cp /etc/conf:test:path12 '
                                           'org.apache.ambari.server.upgrade.StackUpgradeHelper updateMetaInfo ' +
                                           "'" + json.dumps(json_map) + "'" +
                                           ' > /var/log/ambari-server/ambari-server.out 2>&1')


  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("os.path.isfile")
  @patch("ambari_server.serverSetup.get_ambari_properties")
  @patch("os.path.lexists")
  @patch("os.remove")
  @patch("os.symlink")
  @patch("shutil.copy")
  def test_proceedJDBCProperties(self, copy_mock, os_symlink_mock, os_remove_mock, lexists_mock,
                                 get_ambari_properties_mock, isfile_mock,
                                 get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    args = MagicMock()

    # test incorrect path to jdbc-driver
    isfile_mock.return_value = False
    args.jdbc_driver = "test jdbc"
    fail = False

    try:
      proceedJDBCProperties(args)
    except FatalException as e:
      self.assertEquals("File test jdbc does not exist!", e.reason)
      fail = True
    self.assertTrue(fail)

    # test incorrect jdbc-db
    isfile_mock.return_value = True
    args.jdbc_db = "incorrect db"
    fail = False

    try:
      proceedJDBCProperties(args)
    except FatalException as e:
      self.assertEquals("Unsupported database name incorrect db. Please see help for more information.", e.reason)
      fail = True
    self.assertTrue(fail)

    # test getAmbariProperties failed
    args.jdbc_db = "mysql"
    get_ambari_properties_mock.return_value = -1
    fail = False

    try:
      proceedJDBCProperties(args)
    except FatalException as e:
      self.assertEquals("Error getting ambari properties", e.reason)
      fail = True
    self.assertTrue(fail)

    # test get resource dir param failed
    args.jdbc_db = "oracle"
    p = MagicMock()
    get_ambari_properties_mock.return_value = p
    p.__getitem__.side_effect = KeyError("test exception")
    fail = False

    try:
      proceedJDBCProperties(args)
    except FatalException as e:
      fail = True
    self.assertTrue(fail)

    # test copy jdbc failed and symlink exists
    lexists_mock.return_value = True
    args.jdbc_db = "postgres"
    get_ambari_properties_mock.return_value = MagicMock()
    isfile_mock.side_effect = [True, False]
    fail = False

    def side_effect():
      raise Exception(-1, "Failed to copy!")

    copy_mock.side_effect = side_effect

    try:
      proceedJDBCProperties(args)
    except FatalException as e:
      fail = True
    self.assertTrue(fail)
    self.assertTrue(os_remove_mock.called)

    # test success symlink creation
    get_ambari_properties_mock.reset_mock()
    os_remove_mock.reset_mock()
    p = MagicMock()
    get_ambari_properties_mock.return_value = p
    p.__getitem__.side_effect = None
    p.__getitem__.return_value = "somewhere"
    copy_mock.reset_mock()
    copy_mock.side_effect = None
    isfile_mock.side_effect = [True, False]

    proceedJDBCProperties(args)
    self.assertTrue(os_remove_mock.called)
    self.assertTrue(os_symlink_mock.called)
    self.assertTrue(copy_mock.called)
    self.assertEquals(os_symlink_mock.call_args_list[0][0][0], os.path.join("somewhere","test jdbc"))
    self.assertEquals(os_symlink_mock.call_args_list[0][0][1], os.path.join("somewhere","postgres-jdbc-driver.jar"))


  @patch.object(ambari__server, "write_property")
  @patch.object(ambari__server, "find_properties_file")
  @patch.object(ambari__server, "is_root")
  @patch.object(ambari__server, "get_ambari_version")
  @patch.object(ambari__server, "get_ambari_properties")
  def test_upgrade_from_161(self, get_ambari_properties_mock, get_ambari_version_mock, is_root_mock, find_properties_file_mock,
                            write_property_mock):
    args = MagicMock()
    args.dbms = "postgres"
    is_root_mock.return_value = True
    get_ambari_version_mock.return_value = "1.7.0"

    # Local Postgres
    # In Ambari 1.6.1 for an embedded postgres database, the "server.jdbc.database" property stored the DB name,
    # and the DB type was assumed to be "postgres" if the "server.persistence.type" property was "local"
    properties = Properties()
    properties.process_pair(ambari__server.PERSISTENCE_TYPE_PROPERTY, "local")
    properties.process_pair(ambari__server.JDBC_DATABASE_PROPERTY, "ambari")
    get_ambari_properties_mock.return_value = properties

    try:
      ambari__server.upgrade(args)
    except FatalException as fe:
      self.fail("Did not expect failure: " + str(fe))
    else:
      self.assertTrue(write_property_mock.called)

    # External Postgres
    # In Ambari 1.6.1 for an external postgres database, the "server.jdbc.database" property stored the
    # DB type ("postgres"), and the "server.jdbc.schema" property stored the DB name.
    write_property_mock.reset_mock()
    properties = Properties()
    properties.process_pair(ambari__server.PERSISTENCE_TYPE_PROPERTY, "remote")
    properties.process_pair(ambari__server.JDBC_DATABASE_PROPERTY, "postgres")
    properties.process_pair("server.jdbc.schema", "ambari")
    properties.process_pair(ambari__server.JDBC_URL_PROPERTY, "jdbc:postgresql://c6410.ambari.apache.org:5432/ambari")

    get_ambari_properties_mock.return_value = properties
    try:
      ambari__server.upgrade(args)
    except FatalException as fe:
      self.fail("Did not expect failure: " + str(fe))
    else:
      self.assertTrue(write_property_mock.called)

    # External Postgres missing DB type, so it should be set based on the JDBC URL.
    write_property_mock.reset_mock()
    properties = Properties()
    properties.process_pair(ambari__server.PERSISTENCE_TYPE_PROPERTY, "remote")
    properties.process_pair("server.jdbc.schema", "ambari")
    properties.process_pair(ambari__server.JDBC_URL_PROPERTY, "jdbc:postgresql://c6410.ambari.apache.org:5432/ambari")

    get_ambari_properties_mock.return_value = properties
    try:
      ambari__server.upgrade(args)
    except FatalException as fe:
      self.fail("Did not expect failure: " + str(fe))
    else:
      self.assertTrue(write_property_mock.call_count == 2)

    # External MySQL
    # In Ambari 1.6.1 for an external MySQL database, the "server.jdbc.database" property stored the DB type ("mysql"),
    # And the "server.jdbc.schema" property stored the DB name.
    write_property_mock.reset_mock()
    properties = Properties()
    properties.process_pair(ambari__server.PERSISTENCE_TYPE_PROPERTY, "remote")
    properties.process_pair(ambari__server.JDBC_DATABASE_PROPERTY, "mysql")
    properties.process_pair("server.jdbc.schema", "ambari")
    properties.process_pair(ambari__server.JDBC_URL_PROPERTY, "jdbc:mysql://c6409.ambari.apache.org:3306/ambari")
    get_ambari_properties_mock.return_value = properties
    try:
      ambari__server.upgrade(args)
    except FatalException as fe:
      self.fail("Did not expect failure: " + str(fe))
    else:
      self.assertTrue(write_property_mock.called)

    # External MySQL missing DB type, so it should be set based on the JDBC URL.
    write_property_mock.reset_mock()
    properties = Properties()
    properties.process_pair(ambari__server.PERSISTENCE_TYPE_PROPERTY, "remote")
    properties.process_pair("server.jdbc.schema", "ambari")
    properties.process_pair(ambari__server.JDBC_URL_PROPERTY, "jdbc:mysql://c6409.ambari.apache.org:3306/ambari")

    get_ambari_properties_mock.return_value = properties
    try:
      ambari__server.upgrade(args)
    except FatalException as fe:
      self.fail("Did not expect failure: " + str(fe))
    else:
      self.assertTrue(write_property_mock.call_count == 2)


  @patch("__builtin__.open")
  @patch("os.path.isfile")
  @patch("os.path.lexists")
  @patch("os.remove")
  @patch("os.symlink")
  @patch.object(ambari__server.Properties, "store")
  @patch.object(ambari__server, "find_properties_file")
  @patch.object(ambari__server, "adjust_directory_permissions")
  @patch.object(ambari__server, "print_warning_msg")
  @patch.object(ambari__server, "read_ambari_user")
  @patch.object(ambari__server, "run_schema_upgrade")
  @patch.object(ambari__server, "update_ambari_properties")
  @patch.object(ambari__server, "parse_properties_file")
  @patch.object(ambari__server, "get_ambari_version")
  @patch.object(ambari__server, "is_root")
  @patch.object(ambari__server, "get_ambari_properties")
  @patch.object(ambari__server, "upgrade_local_repo")
  def test_upgrade(self, upgrade_local_repo_mock,
                   get_ambari_properties_mock, is_root_mock, get_ambari_version_mock,
                   parse_properties_file_mock,
                   update_ambari_properties_mock, run_schema_upgrade_mock,
                   read_ambari_user_mock, print_warning_msg_mock,
                   adjust_directory_permissions_mock,
                   find_properties_file_mock, properties_store_mock,
                   os_symlink_mock, os_remove_mock, lexists_mock, isfile_mock, open_mock):

    args = MagicMock()
    check_database_name_property_mock = MagicMock()
    update_ambari_properties_mock.return_value = 0
    run_schema_upgrade_mock.return_value = 0
    isfile_mock.return_value = False
    get_ambari_version_mock.return_value = CURR_AMBARI_VERSION

    # Testing call under non-root
    is_root_mock.return_value = False
    try:
      ambari__server.upgrade(args)
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertTrue("root-level" in fe.reason)
      pass

    # Testing calls under root
    is_root_mock.return_value = True

    # Testing with undefined custom user
    read_ambari_user_mock.return_value = None
    run_schema_upgrade_mock.return_value = 0
    ambari__server.upgrade(args)
    self.assertTrue(print_warning_msg_mock.called)
    warning_args = print_warning_msg_mock.call_args[0][0]
    self.assertTrue("custom ambari user" in warning_args)
    self.assertTrue(upgrade_local_repo_mock.called)

    # Testing with defined custom user
    read_ambari_user_mock.return_value = "ambari-custom-user"
    ambari__server.upgrade(args)
    self.assertTrue(adjust_directory_permissions_mock.called)

    properties = Properties()
    get_ambari_properties_mock.return_value = properties
    run_schema_upgrade_mock.return_value = 0
    parse_properties_file_mock.called = False
    retcode = ambari__server.upgrade(args)
    self.assertTrue(get_ambari_properties_mock.called)

    self.assertNotEqual(-1, retcode)
    self.assertTrue(parse_properties_file_mock.called)
    self.assertTrue(run_schema_upgrade_mock.called)

    # test getAmbariProperties failed
    get_ambari_properties_mock.return_value = -1
    fail = False

    try:
      ambari__server.upgrade(args)
    except FatalException as e:
      self.assertEquals("Error getting ambari properties", e.reason)
      fail = True
    self.assertTrue(fail)

    # test get resource dir param failed
    p = MagicMock()
    get_ambari_properties_mock.reset_mock()
    get_ambari_properties_mock.return_value = p
    p.__getitem__.side_effect = ["something", "something", KeyError("test exception")]
    fail = False

    try:
      ambari__server.upgrade(args)
    except FatalException as e:
      fail = True
    self.assertTrue(fail)

    # test if some drivers are available in resources, and symlink available too
    p.reset_mock()
    p.__getitem__.side_effect = ["something", "something", "resources"]
    lexists_mock.return_value = True
    isfile_mock.side_effect = [True, False, False]
    ambari__server.upgrade(args)
    self.assertTrue(os_remove_mock.called)
    self.assertEquals(os_remove_mock.call_count, 1)
    self.assertEquals(os_remove_mock.call_args[0][0], os.path.join("resources", "oracle-jdbc-driver.jar"))
    self.assertEquals(os_symlink_mock.call_count, 1)
    self.assertEquals(os_symlink_mock.call_args[0][0], os.path.join("resources", "ojdbc6.jar"))
    self.assertEquals(os_symlink_mock.call_args[0][1], os.path.join("resources", "oracle-jdbc-driver.jar"))


  def test_print_info_msg(self):
    out = StringIO.StringIO()
    sys.stdout = out

    ambari__server.VERBOSE = True
    ambari__server.print_info_msg("msg")
    self.assertNotEqual("", out.getvalue())

    sys.stdout = sys.__stdout__


  def test_print_error_msg(self):

    out = StringIO.StringIO()
    sys.stdout = out

    ambari__server.VERBOSE = True
    ambari__server.print_error_msg("msg")
    self.assertNotEqual("", out.getvalue())

    sys.stdout = sys.__stdout__


  def test_print_warning_msg(self):

    out = StringIO.StringIO()
    sys.stdout = out

    ambari__server.VERBOSE = True
    ambari__server.print_warning_msg("msg")
    self.assertNotEqual("", out.getvalue())

    sys.stdout = sys.__stdout__


  @patch.object(ambari__server, "get_choice_string_input")
  def test_get_YN_input(self, get_choice_string_input_mock):

    ambari__server.get_YN_input("prompt", "default")
    self.assertTrue(get_choice_string_input_mock.called)
    self.assertEqual(4, len(get_choice_string_input_mock.call_args_list[0][0]))


  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  def test_db_config_factory(self, get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    args = MagicMock()
    properties = Properties()

    del args.dbms
    del args.dbms_index
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password
    del args.silent

    factory = DBMSConfigFactory()

    dbConfig = factory.create(args, properties)

    self.assertEquals(dbConfig.dbms, "postgres")
    self.assertEquals(dbConfig.database_host, "localhost")
    self.assertEquals(dbConfig.database_port, "5432")
    self.assertEquals(dbConfig.database_name, "ambari")
    self.assertEquals(dbConfig.database_username, "ambari")
    self.assertEquals(dbConfig.database_password, "bigdata")

    args = MagicMock()
    args.dbms = "mysql"

    del args.dbms_index
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password
    del args.silent

    dbConfig = factory.create(args, properties)
    self.assertEquals(dbConfig.dbms, "mysql")
    self.assertEquals(dbConfig.database_port, "3306")

    args = MagicMock()
    args.dbms_index = 1

    del args.dbms
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password
    del args.silent

    dbConfig = factory.create(args, properties)
    self.assertEquals(dbConfig.dbms, "oracle")
    self.assertEquals(dbConfig.database_port, "1521")

    failed = False
    args = MagicMock()
    args.dbms_index = 999

    del args.dbms
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password
    del args.silent

    try:
      dbConfig = factory.create(args, properties)
    except:
      failed = True
    self.assertTrue(failed)

  pass

  @patch.object(ambari__server, 'setup')
  def test_main_db_options(self, setup_mock):
    base_args = ["ambari-server.py", "setup"]
    db_args = ["--database", "postgres", "--databasehost", "somehost.net", "--databaseport", "12345",
               "--databasename", "ambari", "--databaseusername", "ambari", "--databasepassword", "bigdata"]

    #test no args
    failed = False
    sys.argv = list(base_args)

    try:
      ambari__server.main()
    except SystemExit:
      failed = True
      pass

    self.assertFalse(failed)
    self.assertTrue(setup_mock.called)

    setup_mock.reset_mock()

    # test embedded option
    failed = False
    sys.argv = list(base_args)
    sys.argv.extend(db_args[-10:])
    sys.argv.extend(["--database", "embedded"])

    try:
      ambari__server.main()
    except SystemExit:
      failed = True
      pass

    self.assertFalse(failed)
    self.assertTrue(setup_mock.called)

    setup_mock.reset_mock()

    #test full args
    sys.argv = list(base_args)
    sys.argv.extend(db_args)

    try:
      ambari__server.main()
    except SystemExit:
      failed = True
      pass

    self.assertTrue(ambari__server.PROMPT_DATABASE_OPTIONS)
    self.assertFalse(failed)
    self.assertTrue(setup_mock.called)

    setup_mock.reset_mock()

    #test not full args
    sys.argv = list(base_args)
    sys.argv.extend(["--database", "postgres"])

    try:
      ambari__server.main()
    except SystemExit:
      failed = True
      pass

    self.assertFalse(setup_mock.called)
    self.assertTrue(failed)

    setup_mock.reset_mock()

    #test wrong database
    failed = False
    sys.argv = list(base_args)
    sys.argv.extend(["--database", "unknown"])
    sys.argv.extend(db_args[2:])

    try:
      ambari__server.main()
    except SystemExit:
      failed = True
      pass

    self.assertTrue(failed)
    self.assertFalse(setup_mock.called)

    setup_mock.reset_mock()

    #test wrong port check
    failed = False
    sys.argv = list(base_args)
    sys.argv.extend(["--databaseport", "unknown"])
    sys.argv.extend(db_args[:4])
    sys.argv.extend(db_args[6:])

    try:
      ambari__server.main()
    except SystemExit:
      failed = True
      pass

    self.assertTrue(failed)
    self.assertFalse(setup_mock.called)
    pass


  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.serverSetup.get_YN_input")
  @patch("ambari_server.dbConfiguration.get_validated_string_input")
  def test_prompt_db_properties(self, get_validated_string_input_mock, get_YN_input_mock,
                                get_os_major_version_mock, get_os_type_mock, get_os_family_mock):
    args = MagicMock()

    args.silent = True
    args.must_set_database_options = True
    args.dbms = "postgres"

    del args.dbms_index

    #test not prompt
    prompt_db_properties(args)

    self.assertFalse(get_validated_string_input_mock.called)
    self.assertFalse(get_YN_input_mock.called)

    self.assertTrue(args.dbms_index == 0)

    get_validated_string_input_mock.reset_mock()
    get_YN_input_mock.reset_mock()

    #test prompt
    args.silent = False

    get_YN_input_mock.return_value = False

    prompt_db_properties(args)

    self.assertTrue(get_YN_input_mock.called)
    self.assertFalse(get_validated_string_input_mock.called)

    self.assertTrue(args.dbms_index == 0)

    get_validated_string_input_mock.reset_mock()
    get_YN_input_mock.reset_mock()

    #test prompt advanced
    get_YN_input_mock.return_value = True
    get_validated_string_input_mock.return_value = "4"

    prompt_db_properties(args)
    self.assertTrue(get_YN_input_mock.called)
    self.assertTrue(get_validated_string_input_mock.called)

    self.assertEquals(args.dbms_index, 3)

    get_validated_string_input_mock.reset_mock()
    get_YN_input_mock.reset_mock()


  pass

  @patch.object(ambari__server, "get_db_cli_tool")
  @patch.object(ambari__server, "run_in_shell")
  def test_execute_remote_script(self, run_in_shell_mock, get_db_cli_tool_mock):
    args = MagicMock()
    script = "script"


    #success
    run_in_shell_mock.return_value = (0, None, None)
    get_db_cli_tool_mock.return_value = "tool"
    args.dbms = "postgres"
    args.database_password = "some_password"

    retcode, out, err = ambari__server.execute_remote_script(args, script)
    self.assertEquals(retcode, 0)
    self.assertTrue(get_db_cli_tool_mock.called)
    self.assertTrue(run_in_shell_mock.called)

    #fail no cli tool
    get_db_cli_tool_mock.return_value = None
    get_db_cli_tool_mock.called = False
    run_in_shell_mock.called = False

    retcode, out, err = ambari__server.execute_remote_script(args, script)
    self.assertEquals(retcode, -1)
    self.assertTrue(get_db_cli_tool_mock.called)
    self.assertFalse(run_in_shell_mock.called)

    #fail execute
    errcode = 1
    get_db_cli_tool_mock.return_value = "tool"
    run_in_shell_mock.return_value = (errcode, None, None)
    get_db_cli_tool_mock.called = False
    run_in_shell_mock.called = False

    retcode, out, err = ambari__server.execute_remote_script(args, script)
    self.assertEquals(retcode, errcode)
    self.assertTrue(get_db_cli_tool_mock.called)
    self.assertTrue(run_in_shell_mock.called)

    #fail wrong database - case should be completely impossible
    get_db_cli_tool_mock.return_value = "tool"
    run_in_shell_mock.return_value = (0, None, None)
    get_db_cli_tool_mock.called = False
    run_in_shell_mock.called = False
    args.dbms = "unknown"

    retcode, out, err = ambari__server.execute_remote_script(args, script)
    self.assertEquals(retcode, -2)
    self.assertTrue(get_db_cli_tool_mock.called)
    self.assertFalse(run_in_shell_mock.called)
    pass

  @patch.object(ambari__server, "get_conf_dir")
  def test_update_ambari_properties(self, get_conf_dir_mock):

    properties = ["server.jdbc.user.name=ambari-server\n",
                  "server.jdbc.user.passwd=/etc/ambari-server/conf/password.dat\n",
                  "java.home=/usr/jdk64/jdk1.6.0_31\n",
                  "server.jdbc.database_name=ambari\n",
                  "ambari-server.user=ambari\n",
                  "agent.fqdn.service.url=URL\n"]

    NEW_PROPERTY = 'some_new_property=some_value\n'
    JDK_NAME_PROPERTY = 'jdk.name=jdk-6u31-linux-x64.bin\n'
    JCE_NAME_PROPERTY = 'jce.name=jce_policy-6.zip\n'
    CHANGED_VALUE_PROPERTY = 'server.jdbc.database_name=should_not_overwrite_value\n'

    get_conf_dir_mock.return_value = '/etc/ambari-server/conf'

    (tf1, fn1) = tempfile.mkstemp()
    (tf2, fn2) = tempfile.mkstemp()
    ambari__server.AMBARI_PROPERTIES_RPMSAVE_FILE = fn1
    ambari__server.AMBARI_PROPERTIES_FILE = fn2

    with open(ambari__server.AMBARI_PROPERTIES_FILE, "w") as f:
      f.write(NEW_PROPERTY)
      f.write(CHANGED_VALUE_PROPERTY)

    with open(ambari__server.AMBARI_PROPERTIES_RPMSAVE_FILE, 'w') as f:
      for line in properties:
        f.write(line)

    #Call tested method
    ambari__server.update_ambari_properties()

    timestamp = datetime.datetime.now()
    #RPMSAVE_FILE wasn't found
    self.assertFalse(os.path.exists(ambari__server.AMBARI_PROPERTIES_RPMSAVE_FILE))
    #Renamed RPMSAVE_FILE exists
    self.assertTrue(os.path.exists(ambari__server.AMBARI_PROPERTIES_RPMSAVE_FILE
                                   + '.' + timestamp.strftime('%Y%m%d%H%M%S')))

    with open(ambari__server.AMBARI_PROPERTIES_FILE, 'r') as f:
      ambari_properties_content = f.readlines()

    for line in properties:
      if (line == "agent.fqdn.service.url=URL\n"):
        if (not ambari__server.GET_FQDN_SERVICE_URL + "=URL\n" in ambari_properties_content) and (
          line in ambari_properties_content):
          self.fail()
      else:
        if not line in ambari_properties_content:
          self.fail()

    if not NEW_PROPERTY in ambari_properties_content:
      self.fail()

    if not JDK_NAME_PROPERTY in ambari_properties_content:
      self.fail()

    if not JCE_NAME_PROPERTY in ambari_properties_content:
      self.fail()

    if CHANGED_VALUE_PROPERTY in ambari_properties_content:
      self.fail()

    # Command should not fail if *.rpmsave file is missing
    result = ambari__server.update_ambari_properties()
    self.assertEquals(result, 0)

    os.unlink(fn2)

    #if ambari.properties file is absent then "ambari-server upgrade" should
    # fail
    (tf, fn) = tempfile.mkstemp()
    ambari__server.AMBARI_PROPERTIES_RPMSAVE_FILE = fn

    result = ambari__server.update_ambari_properties()
    self.assertNotEquals(result, 0)

  @patch.object(Properties, '__init__')
  @patch.object(ambari__server, 'search_file')
  def test_update_ambari_properties_negative_case(self, search_file_mock, properties_mock):
    search_file_mock.return_value = None
    #Call tested method
    self.assertEquals(0, ambari__server.update_ambari_properties())
    self.assertFalse(properties_mock.called)

    search_file_mock.return_value = False
    #Call tested method
    self.assertEquals(0, ambari__server.update_ambari_properties())
    self.assertFalse(properties_mock.called)

    search_file_mock.return_value = ''
    #Call tested method
    self.assertEquals(0, ambari__server.update_ambari_properties())
    self.assertFalse(properties_mock.called)


  @patch.object(ambari__server, "get_conf_dir")
  def test_update_ambari_properties_without_user_property(self, get_conf_dir_mock):
    '''
      Checks: update_ambari_properties call should add ambari-server.user property if
      it's absent
    '''
    properties = ["server.jdbc.user.name=ambari-server\n",
                  "server.jdbc.user.passwd=/etc/ambari-server/conf/password.dat\n",
                  "java.home=/usr/jdk64/jdk1.6.0_31\n",
                  "server.os_type=redhat6\n"]

    get_conf_dir_mock.return_value = '/etc/ambari-server/conf'

    (tf1, fn1) = tempfile.mkstemp()
    (tf2, fn2) = tempfile.mkstemp()
    ambari__server.AMBARI_PROPERTIES_RPMSAVE_FILE = fn1
    ambari__server.AMBARI_PROPERTIES_FILE = fn2

    with open(ambari__server.AMBARI_PROPERTIES_RPMSAVE_FILE, 'w') as f:
      for line in properties:
        f.write(line)

    #Call tested method
    ambari__server.update_ambari_properties()

    ambari_properties = Properties()
    ambari_properties.load(open(fn2))

    self.assertTrue(ambari__server.NR_USER_PROPERTY in ambari_properties.keys())
    value = ambari_properties[ambari__server.NR_USER_PROPERTY]
    self.assertEqual(value, "root")

    os.unlink(fn2)



  @patch.object(FirewallChecks, "run_os_command")
  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.serverSetup.verify_setup_allowed")
  @patch("sys.exit")
  @patch("ambari_server.serverSetup.get_YN_input")
  @patch("ambari_server.dbConfiguration.get_validated_string_input")
  @patch("ambari_server.dbConfiguration_linux.get_YN_input")
  @patch("ambari_server.dbConfiguration_linux.get_validated_string_input")
  @patch("ambari_server.dbConfiguration_linux.PGConfig._store_remote_properties")
  @patch("ambari_server.dbConfiguration_linux.LinuxDBMSConfig.ensure_jdbc_driver_installed")
  @patch("ambari_server.dbConfiguration_linux.read_password")
  @patch("ambari_server.serverSetup.check_jdbc_drivers")
  @patch("ambari_server.serverSetup.is_root")
  @patch("ambari_server.serverSetup.check_ambari_user")
  @patch("ambari_server.serverSetup.download_and_install_jdk")
  @patch("ambari_server.serverSetup.configure_os_settings")
  @patch('__builtin__.raw_input')
  @patch("ambari_server.serverSetup.disable_security_enhancements")
  def test_setup_remote_db_wo_client(self, check_selinux_mock, raw_input, configure_os_settings_mock,
                                     download_jdk_mock, check_ambari_user_mock, is_root_mock, check_jdbc_drivers_mock,
                                     read_password_mock, ensure_jdbc_driver_installed_mock, store_remote_properties_mock,
                                     get_validated_string_input_0_mock, get_YN_input_0_mock,
                                     get_validated_string_input_mock, get_YN_input,
                                     exit_mock, verify_setup_allowed_method,
                                     get_os_major_version_mock, get_os_type_mock,get_os_family_mock, run_os_command_mock):
    args = MagicMock()

    args.jdbc_driver = None
    args.jdbc_db = None
    args.silent = False

    del args.dbms
    del args.dbms_index
    del args.database_host
    del args.database_port
    del args.database_name
    del args.database_username
    del args.database_password

    raw_input.return_value = ""
    is_root_mock.return_value = True
    check_selinux_mock.return_value = (0, "")
    get_os_type_mock.return_value = ""
    get_os_family_mock.return_value = OSConst.REDHAT_FAMILY
    run_os_command_mock.return_value = 3,"",""
    store_remote_properties_mock.return_value = 0
    get_YN_input.return_value = True
    get_validated_string_input_mock.side_effect = ["4"]
    get_validated_string_input_0_mock.side_effect = ["localhost", "5432", "ambari", "ambari", "admin"]
    get_YN_input_0_mock.return_value = False
    read_password_mock.return_value = "encrypted_bigdata"
    ensure_jdbc_driver_installed_mock.return_value = True
    check_jdbc_drivers_mock.return_value = 0
    check_ambari_user_mock.return_value = 0
    download_jdk_mock.return_value = 0
    configure_os_settings_mock.return_value = 0
    verify_setup_allowed_method.return_value = 0

    try:
      setup(args)
      self.fail("Should throw exception")
    except NonFatalException as fe:
      # Expected
      self.assertTrue("Remote database setup aborted." in fe.reason)

  @patch.object(FirewallChecks, "run_os_command")
  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("sys.exit")
  @patch("ambari_server.userInput.get_YN_input")
  @patch("ambari_commons.os_utils.is_root")
  @patch("ambari_server.dbConfiguration_linux.store_password_file")
  @patch('__builtin__.raw_input')
  def test_store_remote_properties(self, raw_input_mock, store_password_file_mock,
                                   is_root_mock,
                                   get_YN_input, exit_mock,
                                   get_os_major_version_mock, get_os_type_mock,get_os_family_mock, run_os_command_mock
  ):

    raw_input_mock.return_value = ""
    is_root_mock.return_value = True
    get_YN_input.return_value = False
    get_os_type_mock.return_value = ""
    get_os_family_mock.return_value = OSConst.REDHAT_FAMILY
    run_os_command_mock.return_value = 3,"",""
    store_password_file_mock.return_value = "encrypted_bigdata"

    import optparse

    args = optparse.Values()
    args.dbms = "oracle"
    args.database_host = "localhost"
    args.database_port = "1234"
    args.database_name = "ambari"
    args.postgres_schema = "ambari"
    args.sid_or_sname = "foo"
    args.database_username = "foo"
    args.database_password = "foo"

    properties0 = Properties()
    properties = Properties()

    factory = DBMSConfigFactory()
    dbConfig = factory.create(args, properties0)

    dbConfig._store_remote_properties(properties)

    found = False
    for n in properties.propertyNames():
      if not found and n.startswith("server.jdbc.properties"):
        found = True

    self.assertTrue(found)


  @patch("ambari_server.serverConfiguration.find_properties_file")
  def test_get_ambari_properties(self, find_properties_file_mock):

    find_properties_file_mock.return_value = None
    rcode = get_ambari_properties()
    self.assertEqual(rcode, -1)

    tf1 = tempfile.NamedTemporaryFile()
    find_properties_file_mock.return_value = tf1.name
    prop_name = 'name'
    prop_value = 'val'

    with open(tf1.name, 'w') as fout:
      fout.write(prop_name + '=' + prop_value)
    fout.close()

    properties = get_ambari_properties()

    self.assertEqual(properties[prop_name], prop_value)


  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.serverConfiguration.get_ambari_properties")
  @patch("ambari_server.serverConfiguration.find_jdbc_driver")
  @patch("ambari_commons.os_utils.copy_files")
  @patch("ambari_commons.logging_utils.print_error_msg")
  @patch("ambari_commons.logging_utils.print_warning_msg")
  @patch('__builtin__.raw_input')
  @patch("sys.exit")
  def test_check_jdbc_drivers(self, exit_mock, raw_input_mock, print_warning_msg, print_error_msg_mock, copy_files_mock,
                         find_jdbc_driver_mock, get_ambari_properties_mock,
                         get_os_major_version_mock, get_os_type_mock, get_os_family_mock):

    out = StringIO.StringIO()
    sys.stdout = out

    args = MagicMock()

    # Check positive scenario
    drivers_list = ['driver_file']
    resources_dir = '/tmp'

    get_ambari_properties_mock.return_value = {ambari__server.RESOURCES_DIR_PROPERTY: resources_dir}
    find_jdbc_driver_mock.return_value = drivers_list

    args.dbms = "oracle"

    rcode = check_jdbc_drivers(args)

    self.assertEqual(0, rcode)
    copy_files_mock.assert_called_with(drivers_list, resources_dir)

    # Check negative scenarios
    # Silent option, no drivers
    ambari__server.SILENT = True

    find_jdbc_driver_mock.return_value = -1

    rcode = check_jdbc_drivers(args)

    self.assertTrue(print_error_msg_mock.called)
    self.assertTrue(exit_mock.called)

    # Non-Silent option, no drivers
    ambari__server.SILENT = False

    find_jdbc_driver_mock.return_value = -1

    rcode = check_jdbc_drivers(args)

    self.assertTrue(exit_mock.called)
    self.assertTrue(print_error_msg_mock.called)

    # Non-Silent option, no drivers at first ask, present drivers after that

    find_jdbc_driver_mock.side_effect = [-1, drivers_list]

    rcode = check_jdbc_drivers(args)

    self.assertEqual(0, rcode)
    copy_files_mock.assert_called_with(drivers_list, resources_dir)

    # Non-Silent option, no drivers at first ask, present drivers after that
    find_jdbc_driver_mock.reset()
    find_jdbc_driver_mock.side_effect = [-1, -1]

    rcode = check_jdbc_drivers(args)

    self.assertTrue(exit_mock.called)
    self.assertTrue(print_error_msg_mock.called)

    sys.stdout = sys.__stdout__


  @patch("ambari_server.serverConfiguration.find_properties_file")
  def test_get_ambari_properties(self, find_properties_file):

    find_properties_file.return_value = None
    rcode = ambari__server.get_ambari_properties()
    self.assertEqual(rcode, -1)

    tf1 = tempfile.NamedTemporaryFile()
    find_properties_file.return_value = tf1.name
    prop_name = 'name'
    prop_value = 'val'

    with open(tf1.name, 'w') as fout:
      fout.write(prop_name + '=' + prop_value)
    fout.close()

    properties = get_ambari_properties()

    self.assertEqual(properties[prop_name], prop_value)
    self.assertEqual(properties.fileName, os.path.abspath(tf1.name))

    sys.stdout = sys.__stdout__


  @patch("os.path.exists")
  @patch("os.remove")
  @patch("ambari_commons.logging_utils.print_warning_msg")
  def test_remove_file(self, printWarningMsgMock, removeMock, pathExistsMock):
    def side_effect():
      raise Exception(-1, "Failed to delete!")

    removeMock.side_effect = side_effect
    pathExistsMock.return_value = 1

    res = remove_file("/someNonExsistantDir/filename")
    self.assertEquals(res, 1)

    removeMock.side_effect = None
    res = remove_file("/someExsistantDir/filename")
    self.assertEquals(res, 0)

  @patch("shutil.copyfile")
  def test_copy_file(self, shutilCopyfileMock):
    def side_effect():
      raise Exception(-1, "Failed to copy!")

    shutilCopyfileMock.side_effect = side_effect

    try:
      copy_file("/tmp/psswd", "/someNonExsistantDir/filename")
      self.fail("Exception on file not copied has not been thrown!")
    except FatalException:
      # Expected
      pass

    self.assertTrue(shutilCopyfileMock.called)

    shutilCopyfileMock.side_effect = None
    try:
      copy_file("/tmp/psswd", "/root/psswd")
    except FatalException:
      self.fail("Exception on file copied should not be thrown!")

    self.assertTrue(shutilCopyfileMock.called)

  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.serverSetup.get_ambari_properties")
  @patch("ambari_server.dbConfiguration_linux.OracleConfig._is_jdbc_driver_installed")
  @patch("ambari_server.dbConfiguration_linux.copy_files")
  @patch("ambari_server.dbConfiguration.print_error_msg")
  @patch("ambari_server.dbConfiguration.print_warning_msg")
  @patch('__builtin__.raw_input')
  @patch("os.path.lexists")
  @patch("os.remove")
  @patch("os.symlink")
  def test_check_jdbc_drivers(self,os_symlink_mock, os_remove_mock, lexists_mock,
                              raw_input_mock, print_warning_msg, print_error_msg_mock,
                              copy_files_mock, is_jdbc_driver_installed_mock, get_ambari_properties_mock,
                              get_os_major_version_mock, get_os_type_mock, get_os_family_mock):

    out = StringIO.StringIO()
    sys.stdout = out

    args = MagicMock()

    # Check positive scenario
    drivers_list = ['driver_file',os.path.join(os.sep,'usr','share','java','ojdbc6.jar')]
    resources_dir = '/tmp'

    get_ambari_properties_mock.return_value = {ambari__server.RESOURCES_DIR_PROPERTY: resources_dir}
    is_jdbc_driver_installed_mock.return_value = drivers_list
    copy_files_mock.return_value = 0

    args.dbms = "oracle"
    lexists_mock.return_value = True

    del args.dbms_index

    rcode = check_jdbc_drivers(args)

    self.assertEquals(os_symlink_mock.call_count, 1)
    self.assertEquals(os_symlink_mock.call_args_list[0][0][0], os.path.join(os.sep,'tmp','ojdbc6.jar'))
    self.assertEquals(os_symlink_mock.call_args_list[0][0][1], os.path.join(os.sep,'tmp','oracle-jdbc-driver.jar'))
    self.assertEqual(0, rcode)
    copy_files_mock.assert_called_with(drivers_list, resources_dir)

    get_ambari_properties_mock.reset_mock()
    is_jdbc_driver_installed_mock.reset_mock()
    copy_files_mock.reset_mock()
    print_error_msg_mock.reset_mock()
    print_warning_msg.reset_mock()
    raw_input_mock.reset_mock()


    # Check negative scenarios
    # Silent option, no drivers
    args.silent = True

    is_jdbc_driver_installed_mock.return_value = -1

    failed = False

    try:
      rcode = check_jdbc_drivers(args)
    except FatalException:
      failed = True

    self.assertTrue(print_error_msg_mock.called)
    self.assertTrue(failed)

    get_ambari_properties_mock.reset_mock()
    is_jdbc_driver_installed_mock.reset_mock()
    copy_files_mock.reset_mock()
    print_error_msg_mock.reset_mock()
    print_warning_msg.reset_mock()
    raw_input_mock.reset_mock()

    # Non-Silent option, no drivers
    args.silent = False

    is_jdbc_driver_installed_mock.return_value = -1

    failed = False

    try:
      rcode = check_jdbc_drivers(args)
    except FatalException:
      failed = True

    self.assertTrue(failed)
    self.assertTrue(print_error_msg_mock.called)

    get_ambari_properties_mock.reset_mock()
    is_jdbc_driver_installed_mock.reset_mock()
    copy_files_mock.reset_mock()
    print_error_msg_mock.reset_mock()
    print_warning_msg.reset_mock()
    raw_input_mock.reset_mock()


    # Non-Silent option, no drivers at first ask, present drivers after that

    is_jdbc_driver_installed_mock.side_effect = [-1, drivers_list]

    rcode = check_jdbc_drivers(args)

    self.assertEqual(0, rcode)
    copy_files_mock.assert_called_with(drivers_list, resources_dir)

    get_ambari_properties_mock.reset_mock()
    is_jdbc_driver_installed_mock.reset_mock()
    copy_files_mock.reset_mock()
    print_error_msg_mock.reset_mock()
    print_warning_msg.reset_mock()
    raw_input_mock.reset_mock()


    # Non-Silent option, no drivers at first ask, no drivers after that
    is_jdbc_driver_installed_mock.side_effect = [-1, -1]

    failed = False

    try:
      rcode = check_jdbc_drivers(args)
    except FatalException:
      failed = True

    self.assertTrue(failed)
    self.assertTrue(print_error_msg_mock.called)

    get_ambari_properties_mock.reset_mock()
    is_jdbc_driver_installed_mock.reset_mock()
    copy_files_mock.reset_mock()
    print_error_msg_mock.reset_mock()
    print_warning_msg.reset_mock()
    raw_input_mock.reset_mock()


    # Failed to copy_files

    is_jdbc_driver_installed_mock.side_effect = [drivers_list]
    try:
      rcode = check_jdbc_drivers(args)
    except FatalException:
      failed = True

    self.assertTrue(failed)

    sys.stdout = sys.__stdout__


  @patch("ambari_server.serverConfiguration.find_properties_file")
  def test_get_ambari_properties(self, find_properties_file_mock):

    find_properties_file_mock.return_value = None
    rcode = get_ambari_properties()
    self.assertEqual(rcode, -1)

    tf1 = tempfile.NamedTemporaryFile()
    find_properties_file_mock.return_value = tf1.name
    prop_name = 'name'
    prop_value = 'val'

    with open(tf1.name, 'w') as fout:
      fout.write(prop_name + '=' + prop_value)
    fout.close()

    properties = get_ambari_properties()

    self.assertEqual(properties[prop_name], prop_value)
    self.assertEqual(properties.fileName, os.path.abspath(tf1.name))

    sys.stdout = sys.__stdout__

  @patch.object(ambari__server, "check_database_name_property")
  @patch("ambari_server.serverConfiguration.find_properties_file")
  def test_parse_properties_file(self, find_properties_file_mock, check_database_name_property_mock):

    check_database_name_property_mock.return_value = 1

    tf1 = tempfile.NamedTemporaryFile(mode='r')
    find_properties_file_mock.return_value = tf1.name

    args = MagicMock()
    parse_properties_file(args)
    self.assertEquals(args.persistence_type, "local")

    with open(tf1.name, 'w') as fout:
      fout.write("\n")
      fout.write(ambari__server.PERSISTENCE_TYPE_PROPERTY + "=remote")

    args = MagicMock()

    parse_properties_file(args)
    self.assertEquals(args.persistence_type, "remote")


  @patch.object(ambari__server, 'decrypt_password_for_alias')
  @patch.object(ambari__server, 'is_alias_string')
  @patch.object(ambari__server, 'get_ambari_properties')
  def test_configure_database_username_password_masterkey_persisted(self,
                                                                    get_ambari_properties_method,
                                                                    is_alias_string_method,
                                                                    decrypt_password_for_alias_method):

    out = StringIO.StringIO()
    sys.stdout = out

    configs = {ambari__server.JDBC_USER_NAME_PROPERTY: "fakeuser",
               ambari__server.JDBC_PASSWORD_PROPERTY: "${alias=somealias}",
               ambari__server.JDBC_DATABASE_NAME_PROPERTY: "fakedbname",
               ambari__server.SECURITY_KEY_IS_PERSISTED: "True"}

    get_ambari_properties_method.return_value = configs

    is_alias_string_method.return_value = True
    decrypt_password_for_alias_method.return_value = "falepasswd"
    args = MagicMock()
    args.master_key = None

    ambari__server.configure_database_username_password(args)

    self.assertTrue(decrypt_password_for_alias_method.called)
    self.assertTrue(is_alias_string_method.called)
    self.assertEquals("fakeuser", args.database_username)
    self.assertEquals("falepasswd", args.database_password)

    sys.stdout = sys.__stdout__


  @patch("ambari_server.dbConfiguration_linux.read_password")
  def test_configure_database_password(self, read_password_method):

    out = StringIO.StringIO()
    sys.stdout = out

    read_password_method.return_value = "fakepasswd"

    result = LinuxDBMSConfig._configure_database_password(True)
    self.assertTrue(read_password_method.called)
    self.assertEquals("fakepasswd", result)

    result = LinuxDBMSConfig._configure_database_password(True)
    self.assertEquals("fakepasswd", result)

    result = LinuxDBMSConfig._configure_database_password(True)
    self.assertEquals("fakepasswd", result)

    sys.stdout = sys.__stdout__


  @patch("os.path.exists")
  @patch.object(ambari__server, 'get_is_secure')
  @patch.object(ambari__server, 'get_is_persisted')
  @patch.object(ambari__server, 'remove_password_file')
  @patch.object(ambari__server, 'save_passwd_for_alias')
  @patch.object(ambari__server, 'read_master_key')
  @patch.object(ambari__server, 'read_ambari_user')
  @patch.object(ambari__server, 'get_master_key_location')
  @patch.object(ambari__server, 'update_properties')
  @patch.object(ambari__server, 'save_master_key')
  @patch.object(ambari__server, "get_YN_input")
  @patch.object(ambari__server, 'search_file')
  @patch.object(ambari__server, 'get_ambari_properties')
  @patch.object(ambari__server, "is_root")
  def test_setup_master_key_not_persist(self, is_root_method,
                                        get_ambari_properties_method, search_file_message,
                                        get_YN_input_method, save_master_key_method,
                                        update_properties_method, get_master_key_location_method,
                                        read_ambari_user_method, read_master_key_method,
                                        save_passwd_for_alias_method, remove_password_file_method,
                                        get_is_persisted_method, get_is_secure_method, exists_mock):

    is_root_method.return_value = True
    p = get_ambari_properties_method.return_value
    p.get_property.side_effect = ["fakepasswd", "fakepasswd", "fakepasswd", "fakepasswd"]
    read_master_key_method.return_value = "aaa"
    get_YN_input_method.return_value = False
    read_ambari_user_method.return_value = None
    save_passwd_for_alias_method.return_value = 0
    get_is_persisted_method.return_value = (True, "filepath")
    get_is_secure_method.return_value = False
    exists_mock.return_value = False

    ambari__server.setup_master_key()

    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(read_master_key_method.called)
    self.assertTrue(read_ambari_user_method.called)
    self.assertTrue(update_properties_method.called)
    self.assertFalse(save_master_key_method.called)
    self.assertTrue(save_passwd_for_alias_method.called)
    self.assertEquals(3, save_passwd_for_alias_method.call_count)
    self.assertTrue(remove_password_file_method.called)

    result_expected = {ambari__server.JDBC_PASSWORD_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.JDBC_RCA_PASSWORD_ALIAS),
                       ambari__server.JDBC_RCA_PASSWORD_FILE_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.JDBC_RCA_PASSWORD_ALIAS),
                       ambari__server.LDAP_MGR_PASSWORD_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.LDAP_MGR_PASSWORD_ALIAS),
                       ambari__server.SSL_TRUSTSTORE_PASSWORD_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.SSL_TRUSTSTORE_PASSWORD_ALIAS),
                       ambari__server.SECURITY_IS_ENCRYPTION_ENABLED: 'true'}

    sorted_x = sorted(result_expected.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)


  @patch.object(ambari__server, 'save_passwd_for_alias')
  @patch("os.path.exists")
  @patch.object(ambari__server, 'get_is_secure')
  @patch.object(ambari__server, 'get_is_persisted')
  @patch.object(ambari__server, 'read_master_key')
  @patch.object(ambari__server, 'read_ambari_user')
  @patch.object(ambari__server, 'get_master_key_location')
  @patch.object(ambari__server, 'update_properties')
  @patch.object(ambari__server, 'save_master_key')
  @patch.object(ambari__server, "get_YN_input")
  @patch.object(ambari__server, 'search_file')
  @patch.object(ambari__server, 'get_ambari_properties')
  @patch.object(ambari__server, "is_root")
  def test_setup_master_key_persist(self, is_root_method,
                                    get_ambari_properties_method, search_file_message,
                                    get_YN_input_method, save_master_key_method,
                                    update_properties_method, get_master_key_location_method,
                                    read_ambari_user_method, read_master_key_method,
                                    get_is_persisted_method, get_is_secure_method, exists_mock,
                                    save_passwd_for_alias_method):

    is_root_method.return_value = True
    p = get_ambari_properties_method.return_value
    p.get_property.side_effect = ["fakepasswd", None, None, None]
    read_master_key_method.return_value = "aaa"
    get_YN_input_method.side_effect = [True, False]
    read_ambari_user_method.return_value = None
    get_is_persisted_method.return_value = (True, "filepath")
    get_is_secure_method.return_value = False
    exists_mock.return_value = False
    save_passwd_for_alias_method.return_value = 0

    ambari__server.setup_master_key()

    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(read_master_key_method.called)
    self.assertTrue(read_ambari_user_method.called)
    self.assertTrue(update_properties_method.called)
    self.assertTrue(save_master_key_method.called)

    result_expected = {ambari__server.JDBC_PASSWORD_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.JDBC_RCA_PASSWORD_ALIAS),
                       ambari__server.SECURITY_IS_ENCRYPTION_ENABLED: 'true'}

    sorted_x = sorted(result_expected.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)


  @patch.object(ambari__server, 'read_master_key')
  @patch.object(ambari__server, 'remove_password_file')
  @patch("os.path.exists")
  @patch.object(ambari__server, 'read_ambari_user')
  @patch.object(ambari__server, 'get_master_key_location')
  @patch("ambari_server.properties.Properties")
  @patch.object(ambari__server, 'save_passwd_for_alias')
  @patch.object(ambari__server, 'read_passwd_for_alias')
  @patch.object(ambari__server, 'update_properties')
  @patch.object(ambari__server, 'save_master_key')
  @patch.object(ambari__server, "get_validated_string_input")
  @patch.object(ambari__server, "get_YN_input")
  @patch.object(ambari__server, 'search_file')
  @patch.object(ambari__server, 'get_ambari_properties')
  @patch.object(ambari__server, "is_root")
  def test_reset_master_key_persisted(self, is_root_method,
                                      get_ambari_properties_method, search_file_message,
                                      get_YN_input_method, get_validated_string_input_method,
                                      save_master_key_method, update_properties_method,
                                      read_passwd_for_alias_method, save_passwd_for_alias_method,
                                      Properties_mock, get_master_key_location_method,
                                      read_ambari_user_method, exists_mock,
                                      remove_password_file_method, read_master_key_method):

    # Testing call under non-root
    is_root_method.return_value = False
    try:
      ambari__server.setup_master_key()
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertTrue("root-level" in fe.reason)
      pass

    # Testing call under root
    is_root_method.return_value = True

    search_file_message.return_value = "filepath"
    read_ambari_user_method.return_value = None
    p = get_ambari_properties_method.return_value
    p.get_property.side_effect = ['true', '${alias=fakealias}',
                                  '${alias=fakealias}',
                                  '${alias=fakealias}', '${alias=fakealias}']

    get_YN_input_method.side_effect = [True, True]
    read_master_key_method.return_value = "aaa"
    read_passwd_for_alias_method.return_value = "fakepassword"
    save_passwd_for_alias_method.return_value = 0
    exists_mock.return_value = False

    ambari__server.setup_master_key()

    self.assertTrue(save_master_key_method.called)
    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(read_master_key_method.called)
    self.assertTrue(update_properties_method.called)
    self.assertTrue(read_passwd_for_alias_method.called)
    self.assertTrue(3, read_passwd_for_alias_method.call_count)
    self.assertTrue(3, save_passwd_for_alias_method.call_count)

    result_expected = {ambari__server.JDBC_PASSWORD_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.JDBC_RCA_PASSWORD_ALIAS),
                       ambari__server.JDBC_RCA_PASSWORD_FILE_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.JDBC_RCA_PASSWORD_ALIAS),
                       ambari__server.LDAP_MGR_PASSWORD_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.LDAP_MGR_PASSWORD_ALIAS),
                       ambari__server.SSL_TRUSTSTORE_PASSWORD_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.SSL_TRUSTSTORE_PASSWORD_ALIAS),
                       ambari__server.SECURITY_IS_ENCRYPTION_ENABLED: 'true'}

    sorted_x = sorted(result_expected.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)


  @patch.object(ambari__server, 'get_is_persisted')
  @patch.object(ambari__server, 'get_is_secure')
  @patch.object(ambari__server, 'remove_password_file')
  @patch("os.path.exists")
  @patch.object(ambari__server, 'read_ambari_user')
  @patch.object(ambari__server, 'get_master_key_location')
  @patch("ambari_server.properties.Properties")
  @patch.object(ambari__server, 'save_passwd_for_alias')
  @patch.object(ambari__server, 'read_passwd_for_alias')
  @patch.object(ambari__server, 'update_properties')
  @patch.object(ambari__server, 'save_master_key')
  @patch.object(ambari__server, "get_validated_string_input")
  @patch.object(ambari__server, "get_YN_input")
  @patch.object(ambari__server, 'search_file')
  @patch.object(ambari__server, 'get_ambari_properties')
  @patch.object(ambari__server, "is_root")
  def test_reset_master_key_not_persisted(self, is_root_method,
                                          get_ambari_properties_method,
                                          search_file_message, get_YN_input_method,
                                          get_validated_string_input_method, save_master_key_method,
                                          update_properties_method, read_passwd_for_alias_method,
                                          save_passwd_for_alias_method, Properties_mock,
                                          get_master_key_location_method, read_ambari_user_method,
                                          exists_mock, remove_password_file_method, get_is_secure_method,
                                          get_is_persisted_method):

    is_root_method.return_value = True
    search_file_message.return_value = False
    read_ambari_user_method.return_value = None
    p = get_ambari_properties_method.return_value
    p.get_property.side_effect = ['${alias=fakealias}', '${alias=fakealias}',
                                  '${alias=fakealias}', '${alias=fakealias}']

    get_YN_input_method.side_effect = [True, False]
    get_validated_string_input_method.return_value = "aaa"
    read_passwd_for_alias_method.return_value = "fakepassword"
    save_passwd_for_alias_method.return_value = 0
    exists_mock.return_value = False
    get_is_secure_method.return_value = True
    get_is_persisted_method.return_value = (True, "filePath")

    ambari__server.setup_master_key()

    self.assertFalse(save_master_key_method.called)
    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(get_validated_string_input_method.called)
    self.assertTrue(update_properties_method.called)
    self.assertTrue(read_passwd_for_alias_method.called)
    self.assertTrue(3, read_passwd_for_alias_method.call_count)
    self.assertTrue(3, save_passwd_for_alias_method.call_count)
    self.assertFalse(save_master_key_method.called)

    result_expected = {ambari__server.JDBC_PASSWORD_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.JDBC_RCA_PASSWORD_ALIAS),
                       ambari__server.JDBC_RCA_PASSWORD_FILE_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.JDBC_RCA_PASSWORD_ALIAS),
                       ambari__server.LDAP_MGR_PASSWORD_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.LDAP_MGR_PASSWORD_ALIAS),
                       ambari__server.SSL_TRUSTSTORE_PASSWORD_PROPERTY:
                         ambari__server.get_alias_string(ambari__server.SSL_TRUSTSTORE_PASSWORD_ALIAS),
                       ambari__server.SECURITY_IS_ENCRYPTION_ENABLED: 'true'}

    sorted_x = sorted(result_expected.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)

  @patch('__builtin__.raw_input')
  @patch.object(ambari__server, 'get_is_secure')
  @patch.object(ambari__server, "get_YN_input")
  @patch.object(ambari__server, 'update_properties')
  @patch.object(ambari__server, 'search_file')
  @patch.object(ambari__server, 'get_ambari_properties')
  @patch.object(ambari__server, "is_root")
  def test_setup_ldap_invalid_input(self, is_root_method, get_ambari_properties_method,
                                    search_file_message,
                                    update_properties_method,
                                    get_YN_input_method,
                                    get_is_secure_method,
                                    raw_input_mock):
    out = StringIO.StringIO()
    sys.stdout = out
    is_root_method.return_value = True
    search_file_message.return_value = "filepath"

    configs = {ambari__server.SECURITY_MASTER_KEY_LOCATION: "filepath",
               ambari__server.SECURITY_KEYS_DIR: tempfile.gettempdir(),
               ambari__server.SECURITY_IS_ENCRYPTION_ENABLED: "true"
    }

    get_ambari_properties_method.return_value = configs
    raw_input_mock.side_effect = ['a:3', 'b:b', 'hody', 'b:2', 'false', 'user', 'uid', 'group', 'cn', 'member', 'base', 'true']
    ambari__server.SILENT = False
    get_YN_input_method.return_value = True

    ambari__server.setup_ldap()

    ldap_properties_map = \
      {
        "authentication.ldap.primaryUrl": "a:3",
        "authentication.ldap.secondaryUrl": "b:2",
        "authentication.ldap.useSSL": "false",
        "authentication.ldap.userObjectClass": "user",
        "authentication.ldap.usernameAttribute": "uid",
        "authentication.ldap.groupObjectClass": "group",
        "authentication.ldap.groupNamingAttr": "cn",
        "authentication.ldap.groupMembershipAttr": "member",
        "authentication.ldap.baseDn": "base",
        "authentication.ldap.bindAnonymously": "true",
        "client.security": "ldap",
        "ambari.ldap.isConfigured": "true"
      }

    sorted_x = sorted(ldap_properties_map.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)
    self.assertTrue(get_YN_input_method.called)
    self.assertTrue(8, raw_input_mock.call_count)

    raw_input_mock.reset_mock()
    raw_input_mock.side_effect = ['a:3', '', 'b:2', 'false', 'user', 'uid', 'group', 'cn', 'member', 'base', 'true']

    ambari__server.setup_ldap()

    ldap_properties_map = \
      {
        "authentication.ldap.primaryUrl": "a:3",
        "authentication.ldap.useSSL": "false",
        "authentication.ldap.userObjectClass": "user",
        "authentication.ldap.usernameAttribute": "uid",
        "authentication.ldap.groupObjectClass": "group",
        "authentication.ldap.groupNamingAttr": "cn",
        "authentication.ldap.groupMembershipAttr": "member",
        "authentication.ldap.baseDn": "base",
        "authentication.ldap.bindAnonymously": "true",
        "client.security": "ldap",
        "ambari.ldap.isConfigured": "true"
      }

    sorted_x = sorted(ldap_properties_map.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)
    self.assertTrue(5, raw_input_mock.call_count)

    sys.stdout = sys.__stdout__

  @patch.object(ambari__server, 'get_is_secure')
  @patch.object(ambari__server, 'encrypt_password')
  @patch.object(ambari__server, 'save_passwd_for_alias')
  @patch.object(ambari__server, "get_YN_input")
  @patch.object(ambari__server, 'update_properties')
  @patch.object(ambari__server, 'configure_ldap_password')
  @patch.object(ambari__server, "get_validated_string_input")
  @patch.object(ambari__server, 'setup_master_key')
  @patch.object(ambari__server, 'search_file')
  @patch.object(ambari__server, 'get_ambari_properties')
  @patch.object(ambari__server, "is_root")
  @patch.object(ambari__server, 'read_password')
  @patch("os.path.exists")
  def test_setup_ldap(self, exists_method, read_password_method, is_root_method, get_ambari_properties_method,
                      search_file_message, setup_master_key_method,
                      get_validated_string_input_method,
                      configure_ldap_password_method, update_properties_method,
                      get_YN_input_method, save_passwd_for_alias_method,
                      encrypt_password_method, get_is_secure_method):
    out = StringIO.StringIO()
    sys.stdout = out


    # Testing call under non-root
    is_root_method.return_value = False
    try:
      ambari__server.setup_ldap()
      self.fail("Should throw exception")
    except FatalException as fe:
      # Expected
      self.assertTrue("root-level" in fe.reason)
      pass

    # Testing call under root
    is_root_method.return_value = True

    search_file_message.return_value = "filepath"

    configs = {ambari__server.SECURITY_MASTER_KEY_LOCATION: "filepath",
               ambari__server.SECURITY_KEYS_DIR: tempfile.gettempdir(),
               ambari__server.SECURITY_IS_ENCRYPTION_ENABLED: "true"
    }

    get_ambari_properties_method.return_value = configs
    configure_ldap_password_method.return_value = "password"
    setup_master_key_method.return_value = (None, True, True)
    save_passwd_for_alias_method.return_value = 0
    encrypt_password_method.return_value = ambari__server.get_alias_string(
      ambari__server.LDAP_MGR_PASSWORD_ALIAS)

    def yn_input_side_effect(*args, **kwargs):
      if 'TrustStore' in args[0]:
        return False
      else:
        return True

    #get_YN_input_method.side_effect = yn_input_side_effect()
    get_YN_input_method.side_effect = [True, ]

    def valid_input_side_effect(*args, **kwargs):
      if 'Bind anonymously' in args[0]:
        return 'false'
      if args[1] == "true" or args[1] == "false":
        return args[1]
      else:
        return "test"

    get_validated_string_input_method.side_effect = valid_input_side_effect

    ambari__server.setup_ldap()

    ldap_properties_map = \
      {
        "authentication.ldap.primaryUrl": "test",
        "authentication.ldap.secondaryUrl": "test",
        "authentication.ldap.useSSL": "false",
        "authentication.ldap.userObjectClass": "test",
        "authentication.ldap.usernameAttribute": "test",
        "authentication.ldap.baseDn": "test",
        "authentication.ldap.bindAnonymously": "false",
        "authentication.ldap.managerDn": "test",
        "authentication.ldap.groupObjectClass": "test",
        "authentication.ldap.groupMembershipAttr": "test",
        "authentication.ldap.groupNamingAttr": "test",
        "client.security": "ldap", \
        ambari__server.LDAP_MGR_PASSWORD_PROPERTY: "ldap-password.dat",
        "ambari.ldap.isConfigured": "true"
      }

    sorted_x = sorted(ldap_properties_map.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))
    self.assertEquals(sorted_x, sorted_y)
    self.assertTrue(update_properties_method.called)
    self.assertTrue(configure_ldap_password_method.called)
    self.assertTrue(get_validated_string_input_method.called)
    self.assertTrue(get_YN_input_method.called)

    # truststore not found case

    def os_path_exists(*args, **kwargs):
      if "bogus" in args[0]:
        return False
      else:
        return True
      pass

    def input_enable_ssl(*args, **kwargs):
      if 'Bind anonymously' in args[0]:
        return 'false'
      if "SSL" in args[0]:
        return "true"
      if "Path to TrustStore file" in args[0]:
        if input_enable_ssl.path_counter < 2:
          input_enable_ssl.path_counter += 1
          return "bogus"
        else:
          return "valid"
      if args[1] == "true" or args[1] == "false":
        return args[1]
      else:
        return "test"
      pass

    input_enable_ssl.path_counter = 0

    exists_method.side_effect = os_path_exists
    get_validated_string_input_method.side_effect = input_enable_ssl
    read_password_method.return_value = "password"
    get_YN_input_method.reset_mock()
    get_YN_input_method.side_effect = [True, True]
    update_properties_method.reset_mock()

    ambari__server.setup_ldap()

    self.assertTrue(read_password_method.called)

    ldap_properties_map = \
      {
        "authentication.ldap.primaryUrl": "test",
        "authentication.ldap.secondaryUrl": "test",
        "authentication.ldap.useSSL": "true",
        "authentication.ldap.usernameAttribute": "test",
        "authentication.ldap.baseDn": "test",
        "authentication.ldap.bindAnonymously": "false",
        "authentication.ldap.managerDn": "test",
        "client.security": "ldap",
        "ssl.trustStore.type": "test",
        "ssl.trustStore.path": "valid",
        "ssl.trustStore.password": "password",
        ambari__server.LDAP_MGR_PASSWORD_PROPERTY: ambari__server.get_alias_string( \
          ambari__server.LDAP_MGR_PASSWORD_ALIAS)
      }

    sorted_x = sorted(ldap_properties_map.iteritems(), key=operator.itemgetter(0))
    sorted_y = sorted(update_properties_method.call_args[0][1].iteritems(),
                      key=operator.itemgetter(0))

    sys.stdout = sys.__stdout__

  @patch("urllib2.urlopen")
  @patch.object(ambari__server, "get_validated_string_input")
  @patch.object(ambari__server, "get_ambari_properties")
  @patch.object(ambari__server, "is_server_runing")
  @patch.object(ambari__server, "is_root")
  def test_ldap_sync_all(self, is_root_method, is_server_runing_mock, get_ambari_properties_mock,
      get_validated_string_input_mock, urlopen_mock):

    is_root_method.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    properties = ambari__server.Properties()
    properties.process_pair(ambari__server.IS_LDAP_CONFIGURED, 'true')
    get_ambari_properties_mock.return_value = properties
    get_validated_string_input_mock.side_effect = ['admin', 'admin']

    response = MagicMock()
    response.getcode.side_effect = [201, 200, 200]
    response.read.side_effect = ['{"resources" : [{"href" : "http://c6401.ambari.apache.org:8080/api/v1/ldap_sync_events/16","Event" : {"id" : 16}}]}',
                          '{"Event":{"status" : "RUNNING","summary" : {"groups" : {"created" : 0,"removed" : 0,"updated" : 0},"memberships" : {"created" : 0,"removed" : 0},"users" : {"created" : 0,"removed" : 0,"updated" : 0}}}}',
                          '{"Event":{"status" : "COMPLETE","summary" : {"groups" : {"created" : 1,"removed" : 0,"updated" : 0},"memberships" : {"created" : 5,"removed" : 0},"users" : {"created" : 5,"removed" : 0,"updated" : 0}}}}']

    urlopen_mock.return_value = response

    ambari__server.LDAP_SYNC_ALL = True

    ambari__server.sync_ldap()

    ambari__server.LDAP_SYNC_ALL = False

    self.assertTrue(response.getcode.called)
    self.assertTrue(response.read.called)

  @patch("urllib2.urlopen")
  @patch.object(ambari__server, "get_validated_string_input")
  @patch.object(ambari__server, "get_ambari_properties")
  @patch.object(ambari__server, "is_server_runing")
  @patch.object(ambari__server, "is_root")
  def test_ldap_sync_existing(self, is_root_method, is_server_runing_mock, get_ambari_properties_mock,
                         get_validated_string_input_mock, urlopen_mock):

      is_root_method.return_value = True
      is_server_runing_mock.return_value = (True, 0)
      properties = Properties()
      properties.process_pair(ambari__server.IS_LDAP_CONFIGURED, 'true')
      get_ambari_properties_mock.return_value = properties
      get_validated_string_input_mock.side_effect = ['admin', 'admin']

      response = MagicMock()
      response.getcode.side_effect = [201, 200, 200]
      response.read.side_effect = ['{"resources" : [{"href" : "http://c6401.ambari.apache.org:8080/api/v1/ldap_sync_events/16","Event" : {"id" : 16}}]}',
                                   '{"Event":{"status" : "RUNNING","summary" : {"groups" : {"created" : 0,"removed" : 0,"updated" : 0},"memberships" : {"created" : 0,"removed" : 0},"users" : {"created" : 0,"removed" : 0,"updated" : 0}}}}',
                                   '{"Event":{"status" : "COMPLETE","summary" : {"groups" : {"created" : 1,"removed" : 0,"updated" : 0},"memberships" : {"created" : 5,"removed" : 0},"users" : {"created" : 5,"removed" : 0,"updated" : 0}}}}']

      urlopen_mock.return_value = response

      ambari__server.LDAP_SYNC_EXISTING = True

      ambari__server.sync_ldap()

      ambari__server.LDAP_SYNC_EXISTING = False

      self.assertTrue(response.getcode.called)
      self.assertTrue(response.read.called)

  @patch("urllib2.urlopen")
  @patch.object(ambari__server, "get_validated_string_input")
  @patch.object(ambari__server, "get_ambari_properties")
  @patch.object(ambari__server, "is_server_runing")
  @patch.object(ambari__server, "is_root")
  def test_ldap_sync_no_sync_mode(self, is_root_method, is_server_runing_mock, get_ambari_properties_mock,
                     get_validated_string_input_mock, urlopen_mock):

      is_root_method.return_value = True
      is_server_runing_mock.return_value = (True, 0)
      properties = Properties()
      properties.process_pair(ambari__server.IS_LDAP_CONFIGURED, 'true')
      get_ambari_properties_mock.return_value = properties
      get_validated_string_input_mock.side_effect = ['admin', 'admin']

      response = MagicMock()
      response.getcode.side_effect = [201, 200, 200]
      response.read.side_effect = ['{"resources" : [{"href" : "http://c6401.ambari.apache.org:8080/api/v1/ldap_sync_events/16","Event" : {"id" : 16}}]}',
                                   '{"Event":{"status" : "RUNNING","summary" : {"groups" : {"created" : 0,"removed" : 0,"updated" : 0},"memberships" : {"created" : 0,"removed" : 0},"users" : {"created" : 0,"removed" : 0,"updated" : 0}}}}',
                                   '{"Event":{"status" : "COMPLETE","summary" : {"groups" : {"created" : 1,"removed" : 0,"updated" : 0},"memberships" : {"created" : 5,"removed" : 0},"users" : {"created" : 5,"removed" : 0,"updated" : 0}}}}']

      urlopen_mock.return_value = response

      try:
          ambari__server.sync_ldap()
          self.fail("Should fail with exception")
      except FatalException as e:
          pass

  @patch("urllib2.urlopen")
  @patch.object(ambari__server, "get_validated_string_input")
  @patch.object(ambari__server, "get_ambari_properties")
  @patch.object(ambari__server, "is_server_runing")
  @patch.object(ambari__server, 'is_root')
  def test_ldap_sync_error_status(self, is_root_method, is_server_runing_mock, get_ambari_properties_mock,
      get_validated_string_input_mock, urlopen_mock):

    is_root_method.return_value = True
    is_server_runing_mock.return_value = (True, 0)
    properties = Properties()
    properties.process_pair(ambari__server.IS_LDAP_CONFIGURED, 'true')
    get_ambari_properties_mock.return_value = properties
    get_validated_string_input_mock.side_effect = ['admin', 'admin']

    response = MagicMock()
    response.getcode.side_effect = [201, 200]
    response.read.side_effect = ['{"resources" : [{"href" : "http://c6401.ambari.apache.org:8080/api/v1/ldap_sync_events/16","Event" : {"id" : 16}}]}',
                          '{"Event":{"status" : "ERROR","status_detail" : "Error!!","summary" : {"groups" : {"created" : 0,"removed" : 0,"updated" : 0},"memberships" : {"created" : 0,"removed" : 0},"users" : {"created" : 0,"removed" : 0,"updated" : 0}}}}']

    urlopen_mock.return_value = response

    try:
      ambari__server.sync_ldap()
      self.fail("Should fail with exception")
    except FatalException as e:
      pass

  @patch("urllib2.urlopen")
  @patch("urllib2.Request")
  @patch("base64.encodestring")
  @patch.object(ambari__server, 'is_root')
  @patch.object(ambari__server, 'is_server_runing')
  @patch.object(ambari__server, 'get_ambari_properties')
  @patch.object(ambari__server, "get_validated_string_input")
  def test_sync_ldap_forbidden(self, get_validated_string_input_method, get_ambari_properties_method,
                                is_server_runing_method, is_root_method,
                                encodestring_method, request_constructor, urlopen_method):

    is_root_method.return_value = False
    try:
      ambari__server.sync_ldap()
      self.fail("Should throw exception if not root")
    except FatalException as fe:
      # Expected
      self.assertTrue("root-level" in fe.reason)
      pass
    is_root_method.return_value = True

    is_server_runing_method.return_value = (None, None)
    try:
      ambari__server.sync_ldap()
      self.fail("Should throw exception if ambari is stopped")
    except FatalException as fe:
      # Expected
      self.assertTrue("not running" in fe.reason)
      pass
    is_server_runing_method.return_value = (True, None)

    configs = MagicMock()
    configs.get_property.return_value = None
    get_ambari_properties_method.return_value = configs
    try:
      ambari__server.sync_ldap()
      self.fail("Should throw exception if ldap is not configured")
    except FatalException as fe:
      # Expected
      self.assertTrue("not configured" in fe.reason)
      pass
    configs.get_property.return_value = 'true'

    get_validated_string_input_method.return_value = 'admin'
    encodestring_method.return_value = 'qwe123'

    requestMocks = [MagicMock()]
    request_constructor.side_effect = requestMocks
    response = MagicMock()
    response.getcode.return_value = 403
    urlopen_method.return_value = response

    try:
      ambari__server.sync_ldap()
      self.fail("Should throw exception if return code != 200")
    except FatalException as fe:
      # Expected
      self.assertTrue("status code" in fe.reason)
      pass

  @patch.object(ambari__server, 'is_root')
  def test_sync_ldap_ambari_stopped(self, is_root_method):
    is_root_method.return_value = False
    try:
      ambari__server.sync_ldap()
      self.fail("Should throw exception if not root")
    except FatalException as fe:
      # Expected
      self.assertTrue("root-level" in fe.reason)
      pass

  @patch.object(ambari__server, 'is_root')
  @patch.object(ambari__server, 'is_server_runing')
  def test_sync_ldap_ambari_stopped(self, is_server_runing_method, is_root_method):
    is_root_method.return_value = True
    is_server_runing_method.return_value = (None, None)
    try:
      ambari__server.sync_ldap()
      self.fail("Should throw exception if ambari is stopped")
    except FatalException as fe:
      # Expected
      self.assertTrue("not running" in fe.reason)
      pass

  @patch.object(ambari__server, 'is_root')
  @patch.object(ambari__server, 'is_server_runing')
  @patch.object(ambari__server, 'get_ambari_properties')
  def test_sync_ldap_not_configured(self, get_ambari_properties_method,
                     is_server_runing_method, is_root_method):
    is_root_method.return_value = True
    is_server_runing_method.return_value = (True, None)

    configs = MagicMock()
    configs.get_property.return_value = None
    get_ambari_properties_method.return_value = configs
    try:
      ambari__server.sync_ldap()
      self.fail("Should throw exception if ldap is not configured")
    except FatalException as fe:
      # Expected
      self.assertTrue("not configured" in fe.reason)
      pass

  @patch.object(ambari__server, 'read_password')
  def test_configure_ldap_password(self, read_password_method):
    out = StringIO.StringIO()
    sys.stdout = out
    read_password_method.return_value = "blah"

    ambari__server.configure_ldap_password()

    self.assertTrue(read_password_method.called)

    sys.stdout = sys.__stdout__

  @patch.object(ambari__server, "get_validated_string_input")
  def test_read_password(self, get_validated_string_input_method):
    out = StringIO.StringIO()
    sys.stdout = out

    passwordDefault = ""
    passwordPrompt = 'Enter Manager Password* : '
    passwordPattern = ".*"
    passwordDescr = "Invalid characters in password."

    get_validated_string_input_method.side_effect = ['', 'aaa', 'aaa']
    password = ambari__server.read_password(passwordDefault, passwordPattern,
                                           passwordPrompt, passwordDescr)
    self.assertTrue(3, get_validated_string_input_method.call_count)
    self.assertEquals('aaa', password)

    get_validated_string_input_method.reset_mock()
    get_validated_string_input_method.side_effect = ['aaa', 'aaa']
    password = ambari__server.read_password(passwordDefault, passwordPattern,
                                           passwordPrompt, passwordDescr)
    self.assertTrue(2, get_validated_string_input_method.call_count)
    self.assertEquals('aaa', password)

    get_validated_string_input_method.reset_mock()
    get_validated_string_input_method.side_effect = ['aaa']
    password = ambari__server.read_password('aaa', passwordPattern,
                                           passwordPrompt, passwordDescr)
    self.assertTrue(1, get_validated_string_input_method.call_count)
    self.assertEquals('aaa', password)

    sys.stdout = sys.__stdout__

  def test_generate_random_string(self):
    random_str_len = 100
    str1 = ambari__server.generate_random_string(random_str_len)
    self.assertTrue(len(str1) == random_str_len)

    str2 = ambari__server.generate_random_string(random_str_len)
    self.assertTrue(str1 != str2)

  @patch("__builtin__.open")
  @patch.object(ambari__server, "search_file")
  @patch.object(ambari__server, "backup_file_in_temp")
  def test_update_properties(self, backup_file_in_temp_mock, search_file_mock, open_mock):
    conf_file = "ambari.properties"
    propertyMap = {"1": "1", "2": "2"}
    properties = MagicMock()
    f = MagicMock(name="file")
    # f.__enter__.return_value = f #mimic file behavior
    search_file_mock.return_value = conf_file
    open_mock.return_value = f

    ambari__server.update_properties(properties, propertyMap)

    properties.store.assert_called_with(f.__enter__.return_value)
    backup_file_in_temp_mock.assert_called_with(conf_file)
    self.assertEquals(2, properties.removeOldProp.call_count)
    self.assertEquals(2, properties.process_pair.call_count)

    properties = MagicMock()
    backup_file_in_temp_mock.reset_mock()
    open_mock.reset_mock()

    ambari__server.update_properties(properties, None)
    properties.store.assert_called_with(f.__enter__.return_value)
    backup_file_in_temp_mock.assert_called_with(conf_file)
    self.assertFalse(properties.removeOldProp.called)
    self.assertFalse(properties.process_pair.called)

    pass


  def test_regexps(self):
    res = re.search(ambari__server.REGEX_HOSTNAME_PORT, "")
    self.assertTrue(res is None)
    res = re.search(ambari__server.REGEX_HOSTNAME_PORT, "ddd")
    self.assertTrue(res is None)
    res = re.search(ambari__server.REGEX_HOSTNAME_PORT, "gg:ff")
    self.assertTrue(res is None)
    res = re.search(ambari__server.REGEX_HOSTNAME_PORT, "gg:55444325")
    self.assertTrue(res is None)
    res = re.search(ambari__server.REGEX_HOSTNAME_PORT, "gg:555")
    self.assertTrue(res is not None)

    res = re.search(ambari__server.REGEX_TRUE_FALSE, "")
    self.assertTrue(res is not None)
    res = re.search(ambari__server.REGEX_TRUE_FALSE, "t")
    self.assertTrue(res is None)
    res = re.search(ambari__server.REGEX_TRUE_FALSE, "trrrr")
    self.assertTrue(res is None)
    res = re.search(ambari__server.REGEX_TRUE_FALSE, "true|false")
    self.assertTrue(res is None)
    res = re.search(ambari__server.REGEX_TRUE_FALSE, "true")
    self.assertTrue(res is not None)
    res = re.search(ambari__server.REGEX_TRUE_FALSE, "false")
    self.assertTrue(res is not None)

    res = re.search(ambari__server.REGEX_ANYTHING, "")
    self.assertTrue(res is not None)
    res = re.search(ambari__server.REGEX_ANYTHING, "t")
    self.assertTrue(res is not None)
    res = re.search(ambari__server.REGEX_ANYTHING, "trrrr")
    self.assertTrue(res is not None)


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


  @patch("ambari_server.dbConfiguration_linux.get_ambari_properties")
  def test_is_jdbc_user_changed(self, get_ambari_properties_mock):
    previous_user = "previous_user"
    new_user = "new_user"

    props = Properties()
    props.process_pair(JDBC_USER_NAME_PROPERTY, previous_user)
    get_ambari_properties_mock.return_value = props

    #check if users are different
    result = PGConfig._is_jdbc_user_changed(new_user)
    self.assertTrue(result)

    #check if users are equal
    result = PGConfig._is_jdbc_user_changed(previous_user)
    self.assertFalse(result)

    #check if one of users is None
    result = PGConfig._is_jdbc_user_changed(None)
    self.assertEqual(None, result)

  @patch.object(ambari__server, "get_ambari_properties")
  def test_check_database_name_property(self, get_ambari_properties_mock):
    parser = OptionParser()
    parser.add_option('--database', default=None, help="Database to use embedded|oracle|mysql|postgres", dest="dbms")
    args = parser.parse_args()

    # negative case
    get_ambari_properties_mock.return_value = {ambari__server.JDBC_DATABASE_NAME_PROPERTY: ""}
    try:
      result = ambari__server.check_database_name_property(args)
      self.fail("Should fail with exception")
    except FatalException as e:
      self.assertTrue('DB Name property not set in config file.' in e.reason)

    # positive case
    dbname = "ambari"
    get_ambari_properties_mock.reset_mock()
    get_ambari_properties_mock.return_value = {ambari__server.JDBC_DATABASE_NAME_PROPERTY: dbname}
    try:
      result = ambari__server.check_database_name_property(args)
    except FatalException:
      self.fail("Setup should be successful")

  @patch.object(FirewallChecks, "run_os_command")
  @patch.object(OSCheck, "get_os_family")
  @patch.object(OSCheck, "get_os_type")
  @patch.object(OSCheck, "get_os_major_version")
  @patch("ambari_server.serverSetup.prompt_db_properties")
  @patch("ambari_server.serverSetup.verify_setup_allowed")
  @patch("ambari_server.serverSetup.get_YN_input", side_effect=db_YN_Inputs_ret_test_ambariServerSetupWithCustomDbName)
  @patch("ambari_server.serverSetup.configure_os_settings")
  @patch("ambari_server.serverSetup.download_and_install_jdk")
  @patch("ambari_server.serverSetup._check_jdbc_options")
  @patch("ambari_server.serverSetup.check_ambari_user")
  @patch("ambari_server.serverSetup.check_jdbc_drivers")
  @patch("ambari_server.serverSetup._check_selinux")
  @patch("ambari_server.serverSetup.proceedJDBCProperties")
  @patch("ambari_server.serverSetup.is_root")
  @patch("ambari_server.serverSetup.read_ambari_user")
  @patch("ambari_server.dbConfiguration_linux.read_password")
  @patch("ambari_server.dbConfiguration_linux.store_password_file")
  @patch("ambari_server.serverSetup.extract_views")
  @patch("ambari_server.serverSetup.adjust_directory_permissions")
  @patch("ambari_server.setupSecurity.adjust_directory_permissions")
  @patch("sys.exit")
  @patch('__builtin__.raw_input')
  def test_ambariServerSetupWithCustomDbName(self, raw_input, exit_mock, adjust_dirs_mock, adjust_dirs_2_mock,
                                             extract_views_mock, store_password_file_mock, read_password_mock,
                                             read_ambari_user_mock, is_root_mock, proceedJDBCProperties_mock,
                                             check_selinux_mock, check_jdbc_drivers_mock, check_ambari_user_mock, check_jdbc_options_mock,
                                             download_jdk_mock, configure_os_settings_mock, get_YN_input,
                                             verify_setup_allowed_method, prompt_db_properties_mock,
                                             get_os_major_version_mock, get_os_type_mock,
                                             get_os_family_mock, run_os_command_mock):

    args = MagicMock()

    raw_input.return_value = ""
    get_YN_input.return_value = False
    verify_setup_allowed_method.return_value = 0
    is_root_mock.return_value = True
    check_selinux_mock.return_value = 0
    check_jdbc_options_mock.return_value = False
    check_ambari_user_mock.return_value = 0
    check_jdbc_drivers_mock.return_value = 0
    prompt_db_properties_mock.return_value = True
    download_jdk_mock.return_value = 0
    configure_os_settings_mock.return_value = 0
    #setup_db_mock.return_value = (0, None, None)
    read_ambari_user_mock.return_value = "username1"
    read_password_mock.return_value = "bigdata"
    store_password_file_mock.return_value = "password"
    extract_views_mock.return_value = 0
    get_os_type_mock.return_value = ""
    get_os_family_mock.return_value = OSConst.REDHAT_FAMILY
    run_os_command_mock.return_value = 3,"",""

    new_db = "newDBName"
    args.dbms = "postgres"
    args.dbms_index = 3
    args.database_name = new_db
    args.postgres_schema = new_db
    args.database_username = "user"
    args.database_password = "password"
    args.jdbc_driver= None
    args.jdbc_db = None
    args.silent = False

    tempdir = tempfile.gettempdir()
    prop_file = os.path.join(tempdir, "ambari.properties")
    with open(prop_file, "w") as f:
      f.write("server.jdbc.database_name=oldDBName")
    f.close()

    os.environ[AMBARI_CONF_VAR] = tempdir

    try:
      result = setup(args)
    except FatalException as ex:
      self.fail("Setup should be successful")

    properties = get_ambari_properties()

    self.assertTrue(JDBC_DATABASE_NAME_PROPERTY in properties.keys())
    value = properties[JDBC_DATABASE_NAME_PROPERTY]
    self.assertEqual(value, new_db)

    del os.environ[AMBARI_CONF_VAR]
    os.remove(prop_file)


  def test_is_valid_filepath(self):
    temp_dir = tempfile.gettempdir()
    temp_file = tempfile.NamedTemporaryFile(mode='r')

    # Correct path to an existing file
    self.assertTrue(temp_file)
    # Correct path to an existing directory
    self.assertFalse(is_valid_filepath(temp_dir), \
      'is_valid_filepath(path) should return False is path is a directory')
    # Incorrect path
    self.assertFalse(is_valid_filepath(''))

  @patch.object(ambari__server, "search_file")
  @patch.object(ambari__server, "get_validated_string_input")
  def test_setup_ambari_krb5_jaas(self, get_validated_string_input_mock,
                                  search_file_mock):
    search_file_mock.return_value = ''

    # Should raise exception if jaas_conf_file isn't an existing file
    self.assertRaises(NonFatalException, ambari__server.setup_ambari_krb5_jaas)

    temp_file = tempfile.NamedTemporaryFile(mode='r')
    search_file_mock.return_value = temp_file.name
    get_validated_string_input_mock.side_effect = ['adm@EXAMPLE.COM', temp_file]

    # setup_ambari_krb5_jaas() should return None if everything is OK
    self.assertEqual(None, ambari__server.setup_ambari_krb5_jaas())
    self.assertTrue(get_validated_string_input_mock.called)
    self.assertEqual(get_validated_string_input_mock.call_count, 2)

  @patch("os.listdir")
  @patch("os.path.exists")
  @patch.object(ambari__server, "load_stack_values")
  @patch.object(ambari__server, "get_ambari_properties")
  @patch.object(ambari__server, "run_metainfo_upgrade")
  def test_upgrade_local_repo(self,
                           run_metainfo_upgrade_mock,
                           get_ambari_properties_mock,
                           load_stack_values_mock,
                           os_path_exists_mock,
                           os_listdir_mock):

    from mock.mock import call
    args = MagicMock()
    args.persistence_type = "local"

    def load_values_side_effect(*args, **kwargs):
      res = {}
      res['a'] = 'http://oldurl'
      if -1 != args[1].find("HDPLocal"):
        res['a'] = 'http://newurl'
      return res

    load_stack_values_mock.side_effect = load_values_side_effect

    properties = Properties()
    get_ambari_properties_mock.return_value = properties
    os_path_exists_mock.return_value = 1
    os_listdir_mock.return_value = ['1.1']

    ambari__server.upgrade_local_repo(args)

    self.assertTrue(get_ambari_properties_mock.called)
    self.assertTrue(load_stack_values_mock.called)
    self.assertTrue(run_metainfo_upgrade_mock.called)
    run_metainfo_upgrade_mock.assert_called_with({'a': 'http://newurl'})

  @patch("os.listdir")
  @patch("os.path.exists")
  @patch.object(ambari__server, "load_stack_values")
  @patch.object(ambari__server, "get_ambari_properties")
  @patch.object(ambari__server, "run_metainfo_upgrade")
  def test_upgrade_local_repo_nochange(self,
                         run_metainfo_upgrade_mock,
                         get_ambari_properties_mock,
                         load_stack_values_mock,
                         os_path_exists_mock,
                         os_listdir_mock):

    from mock.mock import call
    args = MagicMock()
    args.persistence_type = "local"

    def load_values_side_effect(*args, **kwargs):
      res = {}
      res['a'] = 'http://oldurl'
      return res

    load_stack_values_mock.side_effect = load_values_side_effect

    properties = Properties()
    get_ambari_properties_mock.return_value = properties
    os_path_exists_mock.return_value = 1
    os_listdir_mock.return_value = ['1.1']

    ambari__server.upgrade_local_repo(args)

    self.assertTrue(get_ambari_properties_mock.called)
    self.assertTrue(load_stack_values_mock.called)
    self.assertTrue(run_metainfo_upgrade_mock.called)
    run_metainfo_upgrade_mock.assert_called_with({})

  @patch.object(ambari__server, "get_ambari_properties")
  @patch.object(ResourceFilesKeeper, "perform_housekeeping")
  def test_refresh_stack_hash(self,
    perform_housekeeping_mock,
    get_ambari_properties_mock):

    properties = Properties()
    get_ambari_properties_mock.return_value = properties

    ambari__server.refresh_stack_hash()

    self.assertTrue(perform_housekeeping_mock.called)

  @patch.object(ambari__server, 'run_os_command')
  @patch.object(ambari__server, "print_error_msg")
  def test_change_objects_owner_both(self,
                                     print_error_msg_mock,
                                     run_os_command_mock):
    args = MagicMock()
    stdout = " stdout "
    stderr = " stderr "
    run_os_command_mock.return_value = 1, stdout, stderr

    ambari__server.VERBOSE = True
    self.assertRaises(FatalException, ambari__server.change_objects_owner, args)
    print_error_msg_mock.assert_any_call("stderr")
    print_error_msg_mock.assert_any_call("stdout")

  @patch.object(ambari__server, 'run_os_command')
  @patch.object(ambari__server, "print_error_msg")
  def test_change_objects_owner_only_stdout(self,
                                            print_error_msg_mock,
                                            run_os_command_mock):
    args = MagicMock()
    stdout = " stdout "
    stderr = ""
    run_os_command_mock.return_value = 1, stdout, stderr

    ambari__server.VERBOSE = True
    self.assertRaises(FatalException, ambari__server.change_objects_owner, args)
    print_error_msg_mock.assert_called_once_with("stdout")

  @patch.object(ambari__server, 'run_os_command')
  @patch.object(ambari__server, "print_error_msg")
  def test_change_objects_owner_only_stderr(self,
                                            print_error_msg_mock,
                                            run_os_command_mock):
    args = MagicMock()
    stdout = ""
    stderr = " stderr "
    run_os_command_mock.return_value = 1, stdout, stderr

    ambari__server.VERBOSE = True
    self.assertRaises(FatalException, ambari__server.change_objects_owner, args)
    print_error_msg_mock.assert_called_once_with("stderr")

