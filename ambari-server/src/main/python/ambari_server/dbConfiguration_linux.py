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

import shutil

from ambari_commons import OSConst
from ambari_commons.logging_utils import *
from exceptions import *
from dbConfiguration import *
from utils import *

import utils

# PostgreSQL settings
PG_JDBC_CONNECTION_STRING = "jdbc:postgresql://{0}:{1}/{2}"
PG_JDBC_CONNECTION_STRING_ALT = "jdbc:postgresql://{0}:{1}/{2}"

UBUNTU_PG_HBA_ROOT = "/etc/postgresql"
PG_HBA_ROOT_DEFAULT = "/var/lib/pgsql/data"

SETUP_DB_CMD = ['su', '-', 'postgres',
        '--command=psql -f {0} -v username=\'"{1}"\' -v password="\'{2}\'" -v dbname="{3}"']
UPGRADE_STACK_CMD = ['su', 'postgres',
        '--command=psql -f {0} -v stack_name="\'{1}\'"  -v stack_version="\'{2}\'" -v dbname="{3}"']

CHANGE_OWNER_COMMAND = ['su', '-', 'postgres',
                        '--command=/var/lib/ambari-server/resources/scripts/change_owner.sh -d {0} -s {1} -o {2}']

PG_ERROR_BLOCKED = "is being accessed by other users"
PG_STATUS_RUNNING = get_running_status()
PG_DEFAULT_PASSWORD = "bigdata"
SERVICE_CMD = "/usr/bin/env service"
PG_SERVICE_NAME = "postgresql"
PG_HBA_DIR = utils.get_postgre_hba_dir()

PG_ST_CMD = "%s %s status" % (SERVICE_CMD, PG_SERVICE_NAME)
if os.path.isfile("/usr/bin/postgresql-setup"):
    PG_INITDB_CMD = "/usr/bin/postgresql-setup initdb"
else:
    PG_INITDB_CMD = "%s %s initdb" % (SERVICE_CMD, PG_SERVICE_NAME)

PG_START_CMD = "%s %s start" % (SERVICE_CMD, PG_SERVICE_NAME)
PG_RESTART_CMD = "%s %s restart" % (SERVICE_CMD, PG_SERVICE_NAME)
PG_HBA_RELOAD_CMD = "%s %s reload" % (SERVICE_CMD, PG_SERVICE_NAME)

PG_HBA_CONF_FILE = os.path.join(PG_HBA_DIR, "pg_hba.conf")
PG_HBA_CONF_FILE_BACKUP = os.path.join(PG_HBA_DIR, "pg_hba_bak.conf.old")
POSTGRESQL_CONF_FILE = os.path.join(PG_HBA_DIR, "postgresql.conf")


# Set database properties to default values
def load_default_db_properties(args):
  args.persistence_type = 'local'
  args.dbms = DATABASE_NAMES[DATABASE_INDEX]
  args.database_host = "localhost"
  args.database_port = DATABASE_PORTS[DATABASE_INDEX]
  args.database_name = DEFAULT_DB_NAME
  args.database_username = "ambari"
  args.database_password = "bigdata"
  args.sid_or_sname = "sname"
  pass

def configure_database_password(showDefault=True):
  passwordDefault = PG_DEFAULT_PASSWORD
  if showDefault:
    passwordPrompt = 'Enter Database Password (' + passwordDefault + '): '
  else:
    passwordPrompt = 'Enter Database Password: '
  passwordPattern = "^[a-zA-Z0-9_-]*$"
  passwordDescr = "Invalid characters in password. Use only alphanumeric or "\
                  "_ or - characters"

  password = read_password(passwordDefault, passwordPattern, passwordPrompt,
    passwordDescr)

  return password

# Ask user for database connection properties
def prompt_linux_db_properties(args):
  global DATABASE_INDEX

  if args.must_set_database_options:
    load_default_db_properties(args)
    ok = get_YN_input("Enter advanced database configuration [y/n] (n)? ", False)
    if ok:

      print "=============================================================================="
      print "Choose one of the following options:"

      database_num = str(DATABASE_INDEX + 1)
      database_num = get_validated_string_input(
        "[1] - PostgreSQL (Embedded)\n[2] - Oracle\n[3] - MySQL\n[4] - PostgreSQL\n"
        "==============================================================================\n"
        "Enter choice (" + database_num + "): ",
        database_num,
        "^[1234]$",
        "Invalid number.",
        False
      )

      if int(database_num) == 1:
        args.persistence_type = 'local'
        args.database_index = 0
      else:
        args.persistence_type = 'remote'
        selected_db_option = int(database_num)

        if selected_db_option == 2:
          args.database_index = 1
        elif selected_db_option == 3:
          args.database_index = 2
        elif selected_db_option == 4:
          args.database_index = 0
        else:
          print_info_msg('Unknown db option, default to embbeded postgres.')
          args.database_index = 0
        pass
      pass

      DATABASE_INDEX = args.database_index
      args.dbms = DATABASE_NAMES[args.database_index]

      if args.persistence_type != 'local':
        args.database_host = get_validated_string_input(
          "Hostname (" + args.database_host + "): ",
          args.database_host,
          "^[a-zA-Z0-9.\-]*$",
          "Invalid hostname.",
          False
        )

        args.database_port = DATABASE_PORTS[DATABASE_INDEX]
        args.database_port = get_validated_string_input(
          "Port (" + args.database_port + "): ",
          args.database_port,
          "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$",
          "Invalid port.",
          False
        )

        if args.dbms == "oracle":
          # Oracle uses service name or service id
          idType = "1"
          idType = get_validated_string_input(
            "Select Oracle identifier type:\n1 - " + ORACLE_DB_ID_TYPES[0] +
            "\n2 - " + ORACLE_DB_ID_TYPES[1] + "\n(" + idType + "): ",
            idType,
            "^[12]$",
            "Invalid number.",
            False
          )

          if idType == "2":
            args.sid_or_sname = "sid"

          IDTYPE_INDEX = int(idType) - 1
          args.database_name = get_validated_service_name(args.database_name,
                                                          IDTYPE_INDEX)
        elif args.dbms in ["mysql", "postgres"]:
          args.database_name = get_validated_db_name(args.database_name)

        else:
          # other DB types
          pass
        pass
      else:
        args.database_host = "localhost"
        args.database_port = DATABASE_PORTS[DATABASE_INDEX]

        args.database_name = get_validated_db_name(args.database_name)
        pass

      # Username is common for Oracle/MySQL/Postgres
      args.database_username = get_validated_string_input(
        'Username (' + args.database_username + '): ',
        args.database_username,
        USERNAME_PATTERN,
        "Invalid characters in username. Start with _ or alpha "
        "followed by alphanumeric or _ or - characters",
        False
      )
      args.database_password = configure_database_password(True)

    print_info_msg('Using database options: {database},{host},{port},{schema},{user},{password}'.format(
      database=args.dbms,
      host=args.database_host,
      port=args.database_port,
      schema=args.database_name,
      user=args.database_username,
      password=args.database_password
    ))

# PostgreSQL configuration and setup
class PGConfig(DBMSConfig):
  def __init__(self):
    #Init the database configuration data here, if any
    pass

  def configure_database_password(showDefault=True):
    passwordDefault = PG_DEFAULT_PASSWORD
    if showDefault:
      passwordPrompt = 'Enter Database Password (' + passwordDefault + '): '
    else:
      passwordPrompt = 'Enter Database Password: '
    passwordPattern = "^[a-zA-Z0-9_-]*$"
    passwordDescr = "Invalid characters in password. Use only alphanumeric or "\
                    "_ or - characters"

    password = read_password(passwordDefault, passwordPattern, passwordPrompt,
      passwordDescr)

    return password

  #
  # Private implementation
  #
  def _change_db_files_owner(args):
    print 'Fixing database objects owner'
    database_name = args.database_name
    new_owner = args.database_username
    if '"' not in new_owner:
      #wrap to allow old username "ambari-server", postgres only
      new_owner = '\'"{0}"\''.format(new_owner)
      pass

    command = CHANGE_OWNER_COMMAND[:]
    command[-1] = command[-1].format(database_name, 'ambari', new_owner)
    return run_os_command(command)

  def _configure_pg_hba_ambaridb_users(self):
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

  def _configure_pg_hba_postgres_user(self):
    postgresString = "all   postgres"
    for line in fileinput.input(PG_HBA_CONF_FILE, inplace=1):
      print re.sub('all\s*all', postgresString, line),
    os.chmod(PG_HBA_CONF_FILE, 0644)

  def _configure_postgresql_conf(self):
    listenAddress = "listen_addresses = '*'        #"
    for line in fileinput.input(POSTGRESQL_CONF_FILE, inplace=1):
      print re.sub('#+listen_addresses.*?(#|$)', listenAddress, line),
    os.chmod(POSTGRESQL_CONF_FILE, 0644)

  # Store set of properties for remote database connection
  def _store_remote_properties(args):
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

    properties.process_pair(JDBC_DRIVER_PROPERTY, DBCN.get_driver_name())
    # fully qualify the hostname to make sure all the other hosts can connect
    # to the jdbc hostname since its passed onto the agents for RCA
    jdbc_hostname = args.database_host
    if (args.database_host == "localhost"):
      jdbc_hostname = socket.getfqdn()

    #TODO: Implement the DBCN connection string generation
    #connectionStringFormat = DATABASE_CONNECTION_STRINGS
    #if args.sid_or_sname == "sid":
    #  connectionStringFormat = DATABASE_CONNECTION_STRINGS_ALT
    #properties.process_pair(JDBC_URL_PROPERTY, connectionStringFormat[DATABASE_INDEX].format(jdbc_hostname, args.database_port, args.database_name))
    properties.process_pair(JDBC_URL_PROPERTY, DBCN.get_connection_string())
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

    properties.process_pair(JDBC_RCA_DRIVER_PROPERTY, DBCN.get_driver_name())
    properties.process_pair(JDBC_RCA_URL_PROPERTY, DBCN.get_connection_string())
    properties.process_pair(JDBC_RCA_USER_NAME_PROPERTY, args.database_username)
    properties.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY,
        store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))
    if isSecure:
      encrypted_password = encrypt_password(JDBC_RCA_PASSWORD_ALIAS, args.database_password)
      if encrypted_password != args.database_password:
        properties.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY, encrypted_password)
    pass

    conf_file = properties.fileName

    try:
      properties.store(open(conf_file, "w"))
    except Exception, e:
      print 'Could not write ambari config file "%s": %s' % (conf_file, e)
      return -1

    return 0

  #
  # Public methods
  #
  def configure_postgres(self):
    if os.path.isfile(PG_HBA_CONF_FILE):
      if not os.path.isfile(PG_HBA_CONF_FILE_BACKUP):
        shutil.copyfile(PG_HBA_CONF_FILE, PG_HBA_CONF_FILE_BACKUP)
      else:
        #Postgres has been configured before, must not override backup
        print "Backup for pg_hba found, reconfiguration not required"
        return 0
    self._configure_pg_hba_postgres_user()
    self._configure_pg_hba_ambaridb_users()
    os.chmod(PG_HBA_CONF_FILE, 0644)
    self._configure_postgresql_conf()
    #restart postgresql if already running
    pg_status = get_postgre_status()
    if pg_status == PG_STATUS_RUNNING:
      retcode = restart_postgres()
      return retcode
    return 0

  def configure_database(self, args):
    prompt_db_properties(args)

    #DB setup should be done last after doing any setup.

    if is_local_database(args):
      #check if jdbc user is changed
      is_user_changed = is_jdbc_user_changed(args)

      print 'Default properties detected. Using built-in database.'
      store_local_properties(args)

      print 'Checking PostgreSQL...'
      retcode = check_postgre_up()
      if not retcode == 0:
        err = 'Unable to start PostgreSQL server. Exiting'
        raise FatalException(retcode, err)

      print 'Configuring local database...'
      retcode, outdata, errdata = setup_db(args)
      if not retcode == 0:
        err = 'Running database init script was failed. Exiting.'
        raise FatalException(retcode, err)

      if is_user_changed:
        #remove backup for pg_hba in order to reconfigure postgres
        remove_file(PG_HBA_CONF_FILE_BACKUP)

      print 'Configuring PostgreSQL...'
      retcode = configure_postgres()
      if not retcode == 0:
        err = 'Unable to configure PostgreSQL server. Exiting'
        raise FatalException(retcode, err)

    else:
      retcode = self._store_remote_properties(args)
      if retcode != 0:
        err = 'Unable to save config file'
        raise FatalException(retcode, err)

      check_jdbc_drivers(args)

      print 'Configuring remote database connection properties...'
      retcode = setup_remote_db(args)
      if retcode == -1:
        err = "Remote database setup aborted."
        raise NonFatalException(err)

      if not retcode == 0:
        err = 'Error while configuring connection properties. Exiting'
        raise FatalException(retcode, err)
      check_jdbc_drivers(args)


  def configure_database_username_password(self, args):
    properties = get_ambari_properties()
    if properties == -1:
      print_error_msg("Error getting ambari properties")
      return -1

    username = properties[JDBC_USER_NAME_PROPERTY]
    passwordProp = properties[JDBC_PASSWORD_PROPERTY]
    dbname = properties[JDBC_DATABASE_PROPERTY]

    if username and passwordProp and dbname:
      print_info_msg("Database username + password already configured")
      args.database_username = username
      args.database_name = dbname
      if is_alias_string(passwordProp):
        args.database_password = decrypt_password_for_alias(JDBC_RCA_PASSWORD_ALIAS)
      else:
        if os.path.exists(passwordProp):
          with open(passwordProp, 'r') as file:
            args.database_password = file.read()

      return 1
    else:
      print_error_msg("Connection properties not set in config file.")

  def setup_db(self, args):
    self.configure_database_username_password(args)

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
    utils.print_error_msg(errdata)
    return retcode, outdata, errdata

  # Initialize remote database schema
  def setup_remote_db(args):

    setup_msg = "Before starting Ambari Server, you must run the following DDL " \
                "against the database to create the schema: {0}".format(DATABASE_INIT_SCRIPTS[DATABASE_INDEX])

    print_warning_msg(setup_msg)

    proceed = get_YN_input("Proceed with configuring remote database connection properties [y/n] (y)? ", True)
    retCode = 0 if proceed else -1

    return retCode

  def change_db_files_owner(self, args):
    if args.persistence_type == 'local':
      retcode, stdout, stderr = self._change_db_files_owner(args)
      if not retcode == 0:
        raise FatalException(20, 'Unable to change owner of database objects')

  def reset_remote_db(self, args):
    client_usage_cmd_drop = DATABASE_CLI_TOOLS_USAGE[DATABASE_INDEX].format(DATABASE_DROP_SCRIPTS[DATABASE_INDEX], args.database_username,
                                                     BLIND_PASSWORD, args.database_name)
    client_usage_cmd_init = DATABASE_CLI_TOOLS_USAGE[DATABASE_INDEX].format(DATABASE_INIT_SCRIPTS[DATABASE_INDEX], args.database_username,
                                                     BLIND_PASSWORD, args.database_name)

    print_warning_msg('To reset Ambari Server schema ' +
                      'you must run the following DDL against the database to '
                      + 'drop the schema:' + os.linesep + client_usage_cmd_drop
                      + os.linesep + 'Then you must run the following DDL ' +
                      'against the database to create the schema: ' + os.linesep +
                      client_usage_cmd_init + os.linesep)

  def reset_local_db(args):
    dbname = args.database_name
    filename = args.drop_script_file
    username = args.database_username
    password = args.database_password
    command = SETUP_DB_CMD[:]
    command[-1] = command[-1].format(filename, username, password, dbname)
    drop_retcode, drop_outdata, drop_errdata = run_os_command(command)
    if not drop_retcode == 0:
      raise FatalException(1, drop_errdata)
    if drop_errdata and PG_ERROR_BLOCKED in drop_errdata:
      raise FatalException(1, "Database is in use. Please, make sure all connections to the database are closed")
    if drop_errdata and VERBOSE:
      print_warning_msg(drop_errdata)
    print_info_msg("About to run database setup")
    retcode, outdata, errdata = setup_db(args)
    if errdata and VERBOSE:
      print_warning_msg(errdata)
    if (errdata and 'ERROR' in errdata.upper()) or (drop_errdata and 'ERROR' in drop_errdata.upper()):
      if not VERBOSE:
        raise NonFatalException("Non critical error in DDL, use --verbose for more information")
      else:
        raise NonFatalException("Non critical error in DDL")

# PostgreSQL database
class PGDatabase:
  _driverName = ''
  _connectionString = ''

  def __init__(self):
    #Init the database connection here, if any
    pass

  #
  # Private implementation
  #

  # Get database client executable path
  def get_db_cli_tool(self, args):
    for tool in DATABASE_CLI_TOOLS[DATABASE_INDEX]:
      cmd = CHECK_COMMAND_EXIST_CMD.format(tool)
      ret, out, err = run_in_shell(cmd)
      if ret == 0:
        return get_exec_path(tool)

    return None

  #
  # Public interface
  #
  def get_driver_name(self):
    return self._driverName

  def get_connection_string(self):
    return self._connectionString

  def connect(self, args):
    if args.persistence_type == "local":
      return self.check_postgre_up()
    else:
      return 0

  def get_running_status(self):
    """Return postgre running status indicator"""
    if OS_TYPE == OSConst.OS_UBUNTU:
      return "%s/main" % PGDatabase.get_ubuntu_db_version()
    else:
      return DB_STATUS_RUNNING_DEFAULT

  @staticmethod
  def get_hba_dir():
    """Return postgre hba dir location depends on OS"""
    if OS_TYPE == OSConst.OS_UBUNTU:
      return "%s/%s/main" % (UBUNTU_PG_HBA_ROOT, PGDatabase.get_ubuntu_db_version())
    else:
      return PG_HBA_ROOT_DEFAULT

  @staticmethod
  def get_ubuntu_db_version():
    """Return installed version of postgre server. In case of several
    installed versions will be returned a more new one.
    """
    postgre_ver = ""

    if os.path.isdir(UBUNTU_PG_HBA_ROOT):  # detect actual installed versions of PG and select a more new one
      postgre_ver = sorted(
      [fld for fld in os.listdir(UBUNTU_PG_HBA_ROOT) if os.path.isdir(os.path.join(UBUNTU_PG_HBA_ROOT, fld))], reverse=True)
      if len(postgre_ver) > 0:
        return postgre_ver[0]
    return postgre_ver


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
      pg_status = get_postgre_status()
      # SUSE linux set status of stopped postgresql proc to unused
      if pg_status == "unused" or pg_status == "stopped":
        print_info_msg("PostgreSQL is stopped. Restarting ...")
        retcode, out, err = run_os_command(PG_START_CMD)
        return retcode
    return 0

  def execute_db_script(self, args, file):
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

  def execute_remote_script(self, args, scriptPath):
    print_warning_msg("Deprecated method called.")
    tool = get_db_cli_tool(args)
    if not tool:
      # args.warnings.append('{0} not found. Please, run DDL script manually'.format(DATABASE_CLI_TOOLS[DATABASE_INDEX]))
      if VERBOSE:
        print_warning_msg('{0} not found'.format(DATABASE_CLI_TOOLS[DATABASE_INDEX]))
      return -1, "Client wasn't found", "Client wasn't found"

    os.environ["PGPASSWORD"] = args.database_password
    retcode, out, err = run_in_shell('{0} {1}'.format(tool, POSTGRES_EXEC_ARGS.format(
      args.database_host,
      args.database_port,
      args.database_name,
      args.database_username,
      scriptPath
    )))
    return retcode, out, err

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
    return pg_status


  def check_postgre_up():
    pg_status = get_postgre_status()
    if pg_status == PG_STATUS_RUNNING:
      print_info_msg("PostgreSQL is running")
      return 0
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
            pg_status = get_postgre_status()
          else:
            retcode = result
        else:
          out, err = process.communicate()
          retcode = process.returncode
        if pg_status == PG_STATUS_RUNNING:
          print_info_msg("Postgres process is running. Returning...")
          return 0
      except (Exception), e:
        pg_status = get_postgre_status()
        if pg_status == PG_STATUS_RUNNING:
          return 0
        else:
          print_error_msg("Postgres start failed. " + str(e))
          return 1
      return retcode


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
