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
import glob
import optparse

import os
import re
import socket

from ambari_commons.exceptions import *
from ambari_commons.logging_utils import *
from ambari_commons.os_linux import run_os_command
from ambari_server.dbConfiguration_linux import SERVICE_CMD, PG_HBA_CONF_FILE_BACKUP
from ambari_server.serverConfiguration import *
from ambari_server.serverConfiguration_linux import JAVA_SHARE_PATH
from ambari_server.setupSecurity import *
from ambari_server.userInput import get_YN_input, get_validated_string_input
from ambari_server import utils

# selinux commands
GET_SE_LINUX_ST_CMD = utils.locate_file('sestatus', '/usr/sbin')
SE_SETENFORCE_CMD = "setenforce 0"
SE_STATUS_DISABLED = "disabled"
SE_STATUS_ENABLED = "enabled"
SE_MODE_ENFORCING = "enforcing"
SE_MODE_PERMISSIVE = "permissive"

# Non-root user setup commands
NR_USER_COMMENT = "Ambari user"
NR_GET_OWNER_CMD = 'stat -c "%U" {0}'
NR_USERADD_CMD = 'useradd -M --comment "{1}" ' \
                 '--shell %s -d /var/lib/ambari-server/keys/ {0}' % utils.locate_file('nologin', '/sbin')
NR_SET_USER_COMMENT_CMD = 'usermod -c "{0}" {1}'

NR_USER_CHANGE_PROMPT = "Ambari-server daemon is configured to run under user '{0}'. Change this setting [y/n] (n)? "
NR_USER_CUSTOMIZE_PROMPT = "Customize user account for ambari-server daemon [y/n] (n)? "
NR_DEFAULT_USER = "root"

# jdk commands
JDK_URL_PROPERTIES = ["jdk1.7.url", "jdk1.6.url"]
JCE_URL_PROPERTIES = ["jce_policy1.7.url", "jce_policy1.6.url"]
JDK_VERSION_REs = ["(jdk.*)/jre", "Creating (jdk.*)/jre"]
JDK_CHOICE_PROMPT = "[1] - Oracle JDK 1.7\n[2] - Oracle JDK 1.6\n[3] - Custom JDK\n==============================================================================\nEnter choice ({0}): "
JDK_VALID_CHOICES = "^[123]$"
CUSTOM_JDK_NUMBER = "3"
JDK_MIN_FILESIZE = 5000
CREATE_JDK_DIR_CMD = "/bin/mkdir -p {0}"
MAKE_FILE_EXECUTABLE_CMD = "chmod a+x {0}"

JDK_DOWNLOAD_CMD = "curl --create-dirs -o {0} {1}"
JDK_DOWNLOAD_SIZE_CMD = "curl -I {0}"

# use --no-same-owner when running as root to prevent uucp as the user (AMBARI-6478)
UNTAR_JDK_ARCHIVE = "tar --no-same-owner -xvf {0}"


#JDBC
DATABASE_INDEX = 0
USERNAME_PATTERN = "^[a-zA-Z_][a-zA-Z0-9_\-]*$"
DATABASE_NAMES = ["postgres", "oracle", "mysql"]
DATABASE_STORAGE_NAMES = ["Database", "Service", "Database"]
DATABASE_PORTS = ["5432", "1521", "3306"]
DATABASE_DRIVER_NAMES = ["org.postgresql.Driver", "oracle.jdbc.driver.OracleDriver", "com.mysql.jdbc.Driver"]
DATABASE_CONNECTION_STRINGS = [
                  "jdbc:postgresql://{0}:{1}/{2}",
                  "jdbc:oracle:thin:@{0}:{1}/{2}",
                  "jdbc:mysql://{0}:{1}/{2}"]
DATABASE_CONNECTION_STRINGS_ALT = [
                  "jdbc:postgresql://{0}:{1}/{2}",
                  "jdbc:oracle:thin:@{0}:{1}:{2}",
                  "jdbc:mysql://{0}:{1}/{2}"]
ORACLE_SID_PATTERN = "jdbc:oracle:thin:@.+:.+/.+"
ORACLE_SNAME_PATTERN = "jdbc:oracle:thin:@.+:.+:.+"

DATABASE_CLI_TOOLS = [["psql"], ["sqlplus", "sqlplus64"], ["mysql"]]
DATABASE_CLI_TOOLS_DESC = ["psql", "sqlplus", "mysql"]
DATABASE_CLI_TOOLS_USAGE = ['su -postgres --command=psql -f {0} -v username=\'"{1}"\' -v password="\'{2}\'"',
                            'sqlplus {1}/{2} < {0} ',
                            'mysql --user={1} --password={2} {3}<{0}']

MYSQL_INIT_SCRIPT = '/var/lib/ambari-server/resources/Ambari-DDL-MySQL-CREATE.sql'
DATABASE_INIT_SCRIPTS = ['/var/lib/ambari-server/resources/Ambari-DDL-Postgres-CREATE.sql',
                         '/var/lib/ambari-server/resources/Ambari-DDL-Oracle-CREATE.sql',
                         MYSQL_INIT_SCRIPT]
DATABASE_DROP_SCRIPTS = ['/var/lib/ambari-server/resources/Ambari-DDL-Postgres-DROP.sql',
                         '/var/lib/ambari-server/resources/Ambari-DDL-Oracle-DROP.sql',
                         '/var/lib/ambari-server/resources/Ambari-DDL-MySQL-DROP.sql']

JDBC_PROPERTIES_PREFIX = "server.jdbc.properties."
DATABASE_JDBC_PROPERTIES = [
                         [ ],
                         [
                           ["oracle.net.CONNECT_TIMEOUT", "2000"], # socket level timeout
                           ["oracle.net.READ_TIMEOUT", "2000"], # socket level timeout
                           ["oracle.jdbc.ReadTimeout", "8000"] # query fetch timeout
                         ],
                         [ ]
                        ]

POSTGRES_EXEC_ARGS = "-h {0} -p {1} -d {2} -U {3} -f {4} -v username='{3}'"
ORACLE_EXEC_ARGS = "-S -L '{0}/{1}@(description=(address=(protocol=TCP)(host={2})(port={3}))(connect_data=({6}={4})))' @{5} {0}"
MYSQL_EXEC_ARGS_WITH_USER_VARS = "--host={0} --port={1} --user={2} --password={3} {4} " \
                 "-e\"set @schema=\'{4}\'; set @username=\'{2}\'; source {5};\""
MYSQL_EXEC_ARGS_WO_USER_VARS = "--force --host={0} --port={1} --user={2} --password={3} --database={4} < {5} 2> /dev/null"
MYSQL_UPGRADE_STACK_ARGS = "--host={0} --port={1} --user={2} --password={3} --database={4} " \
                 "-e\"set @stackName=\'{6}\'; set @stackVersion=\'{7}\'; source {5};\""

ORACLE_UPGRADE_STACK_ARGS = "-S -L '{0}/{1}@(description=(address=(protocol=TCP)(host={2})(port={3}))(connect_data=({6}={4})))' @{5} {7} {8}"

JDBC_PATTERNS = {"oracle": "*ojdbc*.jar", "mysql": "*mysql*.jar"}
DATABASE_FULL_NAMES = {"oracle": "Oracle", "mysql": "MySQL", "postgres": "PostgreSQL"}
JDBC_DB_OPTION_VALUES = ["postgres", "mysql", "oracle"]
JDBC_DB_DEFAULT_DRIVER = {"postgresql" : "postgresql-jdbc.jar", "mysql" : "mysql-connector-java.jar", "oracle" : "ojdbc6.jar"}
ORACLE_DB_ID_TYPES = ["Service Name", "SID"]


DEFAULT_DB_NAME = "ambari"


MESSAGE_ERROR_NOT_ROOT = 'Ambari-server setup should be run with root-level privileges'

MESSAGE_CHECK_FIREWALL = 'Checking iptables...'

class FirewallChecks(object):
  def __init__(self):

    self.FIREWALL_SERVICE_NAME = "iptables"
    self.SERVICE_CMD = SERVICE_CMD
    self.SERVICE_SUBCMD = "status"

  def get_command(self):
    return "%s %s %s" % (self.SERVICE_CMD, self.FIREWALL_SERVICE_NAME, self.SERVICE_SUBCMD)

  def check_result(self, retcode, out, err):
      return retcode == 0

  def check_iptables(self):
    retcode, out, err = run_os_command(self.get_command())
    if err and len(err) > 0:
      print err
    if self.check_result(retcode, out, err):
      print_warning_msg("%s is running. Confirm the necessary Ambari ports are accessible. " %
                        self.FIREWALL_SERVICE_NAME +
                        "Refer to the Ambari documentation for more details on ports.")
      ok = get_YN_input("OK to continue [y/n] (y)? ", True)
      if not ok:
        raise FatalException(1, None)

  def get_running_result(self):
    # To support test code.  Expected ouput from run_os_command.
    return (0, "", "")

  def get_stopped_result(self):
    # To support test code.  Expected output from run_os_command.
    return (3, "", "")


class UbuntuFirewallChecks(FirewallChecks):
  def __init__(self):
    super(UbuntuFirewallChecks, self).__init__()

    self.FIREWALL_SERVICE_NAME = "ufw"
    self.SERVICE_CMD = utils.locate_file('service', '/usr/sbin')

  def check_result(self, retcode, out, err):
    # On ubuntu, the status command returns 0 whether running or not
    return out and len(out) > 0 and out.strip() != "ufw stop/waiting"

  def get_running_result(self):
    # To support test code.  Expected ouput from run_os_command.
    return (0, "ufw start/running", "")

  def get_stopped_result(self):
    # To support test code.  Expected output from run_os_command.
    return (0, "ufw stop/waiting", "")


class Fedora18FirewallChecks(FirewallChecks):
  def __init__(self):
    self.FIREWALL_SERVICE_NAME = "firewalld.service"

  def get_command(self):
    return "systemctl is-active firewalld.service"


class OpenSuseFirewallChecks(FirewallChecks):
  def __init__(self):
    self.FIREWALL_SERVICE_NAME = "SuSEfirewall2"

  def get_command(self):
    return "/sbin/SuSEfirewall2 status"


def get_firewall_object():
  if OS_TYPE == OSConst.OS_UBUNTU:
    return UbuntuFirewallChecks()
  elif OS_TYPE == OSConst.OS_FEDORA and int(OS_VERSION) >= 18:
    return Fedora18FirewallChecks()
  elif OS_TYPE == OSConst.OS_OPENSUSE:
    return OpenSuseFirewallChecks()
  else:
    return FirewallChecks()


def get_firewall_object_types():
  # To support test code, so tests can loop through the types
  return (FirewallChecks,
          UbuntuFirewallChecks,
          Fedora18FirewallChecks,
          OpenSuseFirewallChecks)


def os_check_firewall():
  return get_firewall_object().check_iptables()


#
# Checks SELinux
#
def check_selinux():
  try:
    retcode, out, err = run_os_command(GET_SE_LINUX_ST_CMD)
    se_status = re.search('(disabled|enabled)', out).group(0)
    print "SELinux status is '" + se_status + "'"
    if se_status == SE_STATUS_DISABLED:
      return 0
    else:
      try:
        se_mode = re.search('(enforcing|permissive)', out).group(0)
      except AttributeError:
        err = "Error determining SELinux mode. Exiting."
        raise FatalException(1, err)
      print "SELinux mode is '" + se_mode + "'"
      if se_mode == SE_MODE_ENFORCING:
        print "Temporarily disabling SELinux"
        run_os_command(SE_SETENFORCE_CMD)
      print_warning_msg(
        "SELinux is set to 'permissive' mode and temporarily disabled.")
      ok = get_YN_input("OK to continue [y/n] (y)? ", True)
      if not ok:
        raise FatalException(1, None)
      return 0
  except OSError:
    print_warning_msg("Could not run {0}: OK".format(GET_SE_LINUX_ST_CMD))
  return 0

def disable_security_enhancements():
  print 'Checking SELinux...'
  err = ''
  retcode = check_selinux()
  if not retcode == 0:
    err = 'Failed to disable SELinux. Exiting.'
  return (retcode, err)


#
# User account creation
#

def os_create_custom_user():
  user = get_validated_string_input(
    "Enter user account for ambari-server daemon (root):",
    "root",
    "^[a-z_][a-z0-9_-]{1,31}$",
    "Invalid username.",
    False
  )

  print_info_msg("Trying to create user {0}".format(user))
  command = NR_USERADD_CMD.format(user, NR_USER_COMMENT)
  retcode, out, err = run_os_command(command)
  if retcode == 9:  # 9 = username already in use
    print_info_msg("User {0} already exists, "
                      "skipping user creation".format(user))

  elif retcode != 0:  # fail
    print_warning_msg("Can't create user {0}. Command {1} "
                      "finished with {2}: \n{3}".format(user, command, retcode, err))
    return retcode, None

  print_info_msg("User configuration is done.")
  return 0, user


#
# JDK Setup
#

def os_install_jdk(java_inst_file, java_home_dir):
  print "Installing JDK to {0}".format(java_home_dir)
  retcode, out, err = run_os_command(CREATE_JDK_DIR_CMD.format(java_home_dir))
  savedPath = os.getcwd()
  os.chdir(java_home_dir)

  if java_inst_file.endswith(".bin"):
    retcode, out, err = run_os_command(MAKE_FILE_EXECUTABLE_CMD.format(java_inst_file))
    retcode, out, err = run_os_command(java_inst_file + ' -noregister')
  elif java_inst_file.endswith(".gz"):
    retcode, out, err = run_os_command(UNTAR_JDK_ARCHIVE.format(java_inst_file))
  else:
    err = "JDK installation failed.Unknown file mask."
    raise FatalException(1, err)

  os.chdir(savedPath)

  if retcode != 0:
    err = "Installation of JDK returned exit code %s" % retcode
    raise FatalException(retcode, err)

  print "Successfully installed JDK to {0}".format(java_home_dir)
  return (retcode, out)

def os_ensure_java_home_env_var_is_set(java_home_var):
  if not os.environ.has_key(JAVA_HOME) or os.environ[JAVA_HOME] != java_home_var:
    os.system("SETX {0} {1} /M".format(JAVA_HOME, java_home_var))
    os.environ[JAVA_HOME] = java_home_var
    pass


#
# JDBC Setup
#

def os_check_jdbc_options(options):
  return (options.jdbc_driver is not None and options.jdbc_db is not None)

#Check if required jdbc drivers present
def os_find_jdbc_driver(args):
  if args.dbms in JDBC_PATTERNS.keys():
    drivers = []
    drivers.extend(glob.glob(JAVA_SHARE_PATH + os.sep + JDBC_PATTERNS[args.dbms]))
    if drivers:
      return drivers
    return -1
  return 0

def os_setup_jdbc_drivers(args):
  result = os_find_jdbc_driver(args)

  msg = 'Before starting Ambari Server, ' \
        'you must copy the {0} JDBC driver JAR file to {1}.'.format(
        DATABASE_FULL_NAMES[args.dbms],
        JAVA_SHARE_PATH)

  if result == -1:
    if SILENT:
      print_error_msg(msg)
      raise FatalException(-1, msg)
    else:
      print_warning_msg(msg)
      raw_input(PRESS_ENTER_MSG)
      result = os_find_jdbc_driver(args)
      if result == -1:
        print_error_msg(msg)
        raise FatalException(-1, msg)

  # Check if selected RDBMS requires drivers to copy
  if type(result) is not int:
    print 'Copying JDBC drivers to server resources...'
    try:
      resources_dir = args[RESOURCES_DIR_PROPERTY]
    except KeyError:
      print_error_msg("There is no value for " + RESOURCES_DIR_PROPERTY + "in " + AMBARI_PROPERTIES_FILE)
      return -1

    db_name = DATABASE_FULL_NAMES[args.dbms].lower()
    jdbc_symlink = os.path.join(resources_dir, db_name + "-jdbc-driver.jar")
    db_default_driver_path = os.path.join(JAVA_SHARE_PATH, JDBC_DB_DEFAULT_DRIVER[db_name])

    if os.path.lexists(jdbc_symlink):
      os.remove(jdbc_symlink)

    copy_status = copy_files(result, resources_dir)

    if not copy_status == 0:
      raise FatalException(-1, "Failed to copy JDBC drivers to server resources")

    if db_default_driver_path in result:
      os.symlink(os.path.join(resources_dir, JDBC_DB_DEFAULT_DRIVER[db_name]), jdbc_symlink)

def os_load_default_db_properties(args):
  args.persistence_type = 'local'
  args.dbms = DATABASE_NAMES[DATABASE_INDEX]
  args.database_host = "localhost"
  args.database_port = DATABASE_PORTS[DATABASE_INDEX]
  args.database_name = DEFAULT_DB_NAME
  args.database_username = "ambari"
  args.database_password = "bigdata"
  args.sid_or_sname = "sname"
  pass

# Check if jdbc user is changed
def is_jdbc_user_changed(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return None

  previos_user = properties[JDBC_USER_NAME_PROPERTY]
  new_user = args.database_username

  if previos_user and new_user:
    if previos_user != new_user:
      return True
    else:
      return False

  return None

def os_setup_database(options):
  if is_local_database(options):
    os_setup_local_database(options)
  else:
    os_setup_remote_database(options)

def os_setup_local_database(options):
  #check if jdbc user is changed
  is_user_changed = is_jdbc_user_changed(options)

  print 'Default properties detected. Using built-in database.'
  os_store_local_properties(options)

  print 'Checking PostgreSQL...'
  pg_status, retcode, out, err = check_postgre_up()
  if not retcode == 0:
    err = 'Unable to start PostgreSQL server. Status {0}. {1}.' \
          ' Exiting'.format(pg_status, err)
    raise FatalException(retcode, err)

  print 'Configuring local database...'
  retcode, outdata, errdata = setup_db(options)
  if not retcode == 0:
    err = 'Running database init script was failed. {0}. Exiting.'.format(errdata)
    raise FatalException(retcode, err)

  if is_user_changed:
    #remove backup for pg_hba in order to reconfigure postgres
    remove_file(PG_HBA_CONF_FILE_BACKUP)

  print 'Configuring PostgreSQL...'
  retcode, out, err = configure_postgres()
  if not retcode == 0:
    err = 'Unable to configure PostgreSQL server. {0} Exiting'.format(err)
    raise FatalException(retcode, err)
  pass

def os_setup_remote_database(options):
  retcode = os_store_remote_properties(options)
  if retcode != 0:
    err = 'Unable to save config file'
    raise FatalException(retcode, err)

    os_setup_jdbc_drivers(options)

  print 'Configuring remote database connection properties...'
  retcode = setup_remote_db(options)
  if retcode == -1:
    err = "Remote database setup aborted."
    raise NonFatalException(err)

  if not retcode == 0:
    err = 'Error while configuring connection properties. Exiting'
    raise FatalException(retcode, err)
  pass

def os_reset_database(options):
  pass


### Postgres ###


def configure_pg_hba_ambaridb_users():
  args = optparse.Values()
  configure_database_username_password(args)

  with open(PG_HBA_CONF_FILE, "a") as pgHbaConf:
    pgHbaConf.write("\n")
    pgHbaConf.write("local  all  " + args.database_username +
                    ",mapred md5")
    pgHbaConf.write("\n")
    pgHbaConf.write("host  all   " + args.database_username +
                    ",mapred 0.0.0.0/0  md5")
    pgHbaConf.write("\n")
    pgHbaConf.write("host  all   " + args.database_username +
                    ",mapred ::/0 md5")
    pgHbaConf.write("\n")
  retcode, out, err = run_os_command(PG_HBA_RELOAD_CMD)
  if not retcode == 0:
    raise FatalException(retcode, err)


def configure_pg_hba_postgres_user():
  postgresString = "all   postgres"
  for line in fileinput.input(PG_HBA_CONF_FILE, inplace=1):
    print re.sub('all\s*all', postgresString, line),
  os.chmod(PG_HBA_CONF_FILE, 0644)


def configure_postgresql_conf():
  listenAddress = "listen_addresses = '*'        #"
  for line in fileinput.input(POSTGRESQL_CONF_FILE, inplace=1):
    print re.sub('#+listen_addresses.*?(#|$)', listenAddress, line),
  os.chmod(POSTGRESQL_CONF_FILE, 0644)


def configure_postgres():
  if os.path.isfile(PG_HBA_CONF_FILE):
    if not os.path.isfile(PG_HBA_CONF_FILE_BACKUP):
      shutil.copyfile(PG_HBA_CONF_FILE, PG_HBA_CONF_FILE_BACKUP)
    else:
      #Postgres has been configured before, must not override backup
      print "Backup for pg_hba found, reconfiguration not required"
      return 0, "", ""
  configure_pg_hba_postgres_user()
  configure_pg_hba_ambaridb_users()
  os.chmod(PG_HBA_CONF_FILE, 0644)
  configure_postgresql_conf()
  #restart postgresql if already running
  pg_status, retcode, out, err = get_postgre_status()
  if pg_status == PG_STATUS_RUNNING:
    retcode, out, err = restart_postgres()
    return retcode, out, err
  return 0, "", ""


def restart_postgres():
  print "Restarting PostgreSQL"
  process = subprocess.Popen(PG_RESTART_CMD.split(' '),
                            stdout=subprocess.PIPE,
                            stdin=subprocess.PIPE,
                            stderr=subprocess.PIPE
                             )
  time.sleep(5)
  result = process.poll()
  if result is None:
    print_info_msg("Killing restart PostgresSQL process")
    process.kill()
    pg_status, retcode, out, err = get_postgre_status()
    # SUSE linux set status of stopped postgresql proc to unused
    if pg_status == "unused" or pg_status == "stopped":
      print_info_msg("PostgreSQL is stopped. Restarting ...")
      retcode, out, err = run_os_command(PG_START_CMD)
      return retcode, out, err
  return 0, "", ""


# todo: check if the scheme is already exist


def setup_db(args):
  #password access to ambari-server and mapred
  configure_database_username_password(args)
  dbname = args.database_name
  scriptFile = args.init_script_file
  username = args.database_username
  password = args.database_password

  #setup DB
  command = SETUP_DB_CMD[:]
  command[-1] = command[-1].format(scriptFile, username, password, dbname)

  for i in range(SETUP_DB_CONNECT_ATTEMPTS):
    sys.stdout.write('Connecting to local database...')
    retcode, outdata, errdata = run_os_command(command)
    if retcode == 0:
      print 'done.'
      return retcode, outdata, errdata
    timeOutMsg = 'connection timed out'
    if (i+1) < SETUP_DB_CONNECT_ATTEMPTS:
      timeOutMsg += '...retrying (%d)' % (i+1)
      print timeOutMsg
      time.sleep(SETUP_DB_CONNECT_TIMEOUT)

  print 'unable to connect to database'
  print_error_msg(errdata)
  return retcode, outdata, errdata


def execute_db_script(args, file):
  #password access to ambari-server and mapred
  configure_database_username_password(args)
  dbname = args.database_name
  username = args.database_username
  password = args.database_password
  command = SETUP_DB_CMD[:]
  command[-1] = command[-1].format(file, username, password, dbname)
  retcode, outdata, errdata = run_os_command(command)
  if not retcode == 0:
    print errdata
  return retcode


def check_db_consistency(args, file):
  #password access to ambari-server and mapred
  configure_database_username_password(args)
  dbname = args.database_name
  username = args.database_username
  password = args.database_password
  command = SETUP_DB_CMD[:]
  command[-1] = command[-1].format(file, username, password, dbname)
  retcode, outdata, errdata = run_os_command(command)
  if not retcode == 0:
    print errdata
    return retcode
  else:
    # Assumes that the output is of the form ...\n<count>
    print_info_msg("Parsing output: " + outdata)
    lines = outdata.splitlines()
    if (lines[-1] == '3' or lines[-1] == '0'):
      return 0
  return -1


def get_postgre_status():
  retcode, out, err = run_os_command(PG_ST_CMD)
  try:
    pg_status = re.search('(stopped|running)', out, re.IGNORECASE).group(0).lower()
  except AttributeError:
    pg_status = None
  return pg_status, retcode, out, err


def check_postgre_up():
  pg_status, retcode, out, err = get_postgre_status()
  if pg_status == PG_STATUS_RUNNING:
    print_info_msg("PostgreSQL is running")
    return pg_status, 0, out, err
  else:
    # run initdb only on non ubuntu systems as ubuntu does not have initdb cmd.
    if OS_TYPE != OSConst.OS_UBUNTU:
      print "Running initdb: This may take upto a minute."
      retcode, out, err = run_os_command(PG_INITDB_CMD)
      if retcode == 0:
        print out
    print "About to start PostgreSQL"
    try:
      process = subprocess.Popen(PG_START_CMD.split(' '),
                                 stdout=subprocess.PIPE,
                                 stdin=subprocess.PIPE,
                                 stderr=subprocess.PIPE
                                 )
      if OS_TYPE == OSConst.OS_SUSE:
        time.sleep(20)
        result = process.poll()
        print_info_msg("Result of postgres start cmd: " + str(result))
        if result is None:
          process.kill()
          pg_status, retcode, out, err = get_postgre_status()
        else:
          retcode = result
      else:
        out, err = process.communicate()
        retcode = process.returncode
      if pg_status == PG_STATUS_RUNNING:
        print_info_msg("Postgres process is running. Returning...")
        return pg_status, 0, out, err
    except (Exception), e:
      pg_status, retcode, out, err = get_postgre_status()
      if pg_status == PG_STATUS_RUNNING:
        return pg_status, 0, out, err
      else:
        print_error_msg("Postgres start failed. " + str(e))
    return pg_status, retcode, out, err

def get_validated_db_name(database_name):
  return get_validated_string_input(
        DATABASE_STORAGE_NAMES[DATABASE_INDEX] + " Name ("
        + database_name + "): ",
        database_name,
        ".*",
        "Invalid " + DATABASE_STORAGE_NAMES[DATABASE_INDEX] + " name.",
        False
        )

def get_validated_service_name(service_name, index):
  return get_validated_string_input(
            ORACLE_DB_ID_TYPES[index] + " (" + service_name + "): ",
            service_name,
            ".*",
            "Invalid " + ORACLE_DB_ID_TYPES[index] + ".",
            False
            )

def get_pass_file_path(conf_file):
  return os.path.join(os.path.dirname(conf_file),
                      JDBC_PASSWORD_FILENAME)

# Store local database connection properties
def os_store_local_properties(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  isSecure = get_is_secure(properties)

  properties.removeOldProp(JDBC_SCHEMA_PROPERTY)
  properties.removeOldProp(JDBC_HOSTNAME_PROPERTY)
  properties.removeOldProp(JDBC_RCA_DRIVER_PROPERTY)
  properties.removeOldProp(JDBC_RCA_URL_PROPERTY)
  properties.removeOldProp(JDBC_PORT_PROPERTY)
  properties.removeOldProp(JDBC_DRIVER_PROPERTY)
  properties.removeOldProp(JDBC_URL_PROPERTY)
  properties.process_pair(PERSISTENCE_TYPE_PROPERTY, "local")
  properties.process_pair(JDBC_DATABASE_PROPERTY, args.database_name)
  properties.process_pair(JDBC_USER_NAME_PROPERTY, args.database_username)
  properties.process_pair(JDBC_PASSWORD_PROPERTY,
                          store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))

  if isSecure:
    encrypted_password = encrypt_password(JDBC_RCA_PASSWORD_ALIAS, args.database_password)
    if args.database_password != encrypted_password:
      properties.process_pair(JDBC_PASSWORD_PROPERTY, encrypted_password)
    pass
  pass

  return 0


# Store set of properties for remote database connection
def os_store_remote_properties(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  isSecure = get_is_secure(properties)

  properties.process_pair(PERSISTENCE_TYPE_PROPERTY, "remote")

  properties.process_pair(JDBC_DATABASE_PROPERTY, args.dbms)
  properties.process_pair(JDBC_HOSTNAME_PROPERTY, args.database_host)
  properties.process_pair(JDBC_PORT_PROPERTY, args.database_port)
  properties.process_pair(JDBC_SCHEMA_PROPERTY, args.database_name)

  properties.process_pair(JDBC_DRIVER_PROPERTY, DATABASE_DRIVER_NAMES[DATABASE_INDEX])
  # fully qualify the hostname to make sure all the other hosts can connect
  # to the jdbc hostname since its passed onto the agents for RCA
  jdbc_hostname = args.database_host
  if (args.database_host == "localhost"):
    jdbc_hostname = socket.getfqdn()

  connectionStringFormat = DATABASE_CONNECTION_STRINGS
  if args.sid_or_sname == "sid":
    connectionStringFormat = DATABASE_CONNECTION_STRINGS_ALT
  properties.process_pair(JDBC_URL_PROPERTY,
                          connectionStringFormat[DATABASE_INDEX].format(jdbc_hostname, args.database_port,
                                                                        args.database_name))
  properties.process_pair(JDBC_USER_NAME_PROPERTY, args.database_username)
  properties.process_pair(JDBC_PASSWORD_PROPERTY,
                          store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))

  # save any other defined properties to pass to JDBC
  if DATABASE_INDEX < len(DATABASE_JDBC_PROPERTIES):
    for pair in DATABASE_JDBC_PROPERTIES[DATABASE_INDEX]:
      properties.process_pair(JDBC_PROPERTIES_PREFIX + pair[0], pair[1])

  if isSecure:
    encrypted_password = encrypt_password(JDBC_RCA_PASSWORD_ALIAS, args.database_password)
    if encrypted_password != args.database_password:
      properties.process_pair(JDBC_PASSWORD_PROPERTY, encrypted_password)
  pass

  properties.process_pair(JDBC_RCA_DRIVER_PROPERTY, DATABASE_DRIVER_NAMES[DATABASE_INDEX])
  properties.process_pair(JDBC_RCA_URL_PROPERTY,
                          connectionStringFormat[DATABASE_INDEX].format(jdbc_hostname, args.database_port,
                                                                        args.database_name))
  properties.process_pair(JDBC_RCA_USER_NAME_PROPERTY, args.database_username)
  properties.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY,
                          store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))
  if isSecure:
    encrypted_password = encrypt_password(JDBC_RCA_PASSWORD_ALIAS, args.database_password)
    if encrypted_password != args.database_password:
      properties.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY, encrypted_password)
  pass

  return 0
