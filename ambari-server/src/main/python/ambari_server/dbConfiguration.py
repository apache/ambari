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

from ambari_commons import OSCheck
from ambari_commons.exceptions import FatalException
from ambari_commons.logging_utils import print_error_msg
from ambari_server.setupSecurity import SECURITY_IS_ENCRYPTION_ENABLED
from serverConfiguration import get_ambari_properties


#Database settings
DB_STATUS_RUNNING_DEFAULT = "running"

SETUP_DB_CONNECT_TIMEOUT = 5
SETUP_DB_CONNECT_ATTEMPTS = 3

DATABASE_INDEX = 0
USERNAME_PATTERN = "^[a-zA-Z_][a-zA-Z0-9_\-]*$"
PASSWORD_PATTERN = "^[a-zA-Z0-9_-]*$"
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
#
# Database configuration base class
#
class DBMSConfig(object):
  def __init__(self, options, properties):
    """
    #Just load the defaults. The derived classes will be able to modify them later
    """
    self.persistence_type = 'remote'
    self.dbms = ""
    self.driver_name = ""
    self.database_host = ""
    self.database_port = ""
    self.database_name = ""
    self.database_username = ""
    self.password_file = None

    self.silent = options.silent

    isSecureProp = properties.get_property(SECURITY_IS_ENCRYPTION_ENABLED)
    self.isSecure = True if isSecureProp and isSecureProp.lower() == 'true' else False
    pass


  @staticmethod
  # properties = property bag that will ultimately define the type of database. Since
  #   right now in Windows we only support SQL Server, this argument is not yet used.
  # dbId = additional information, that helps distinguish between various database connections
  #   (Ambari vs. Metrics is a prime example)
  def create(options, properties, dbId = "Ambari"):
    #if OSCheck.is_windows_os():
    if dbId == "Ambari":
      return SQLServerAmbariDBConfig(options, properties)
    elif dbId == "Metrics":
      return SQLServerMetricsDBConfig(options, properties)
    else:
      raise FatalException(-1, "Invalid database requested: " + str(dbId))
    #else:
    #  go the classic Linux way
    #return PGConfig(properties, dbId)
    #return MySQLConfig(properties, dbId)
    #return OracleConfig(properties, dbId)


  #
  # Public methods
  #

  #
  # Main method. Configures the database according to the options and the existing properties.
  #
  def configure_database(self, args, properties):
    result = self._prompt_db_properties()
    if result:
      #DB setup should be done last after doing any setup.
      if self._is_local_database():
        self._setup_local_server(properties)
      else:
        self._setup_remote_server(properties)
    return result

  def setup_database(self):
    print 'Configuring {} database...'.format(self.db_title)

    #DB setup should be done last after doing any setup.
    if self._is_local_database():
      self._setup_local_database()
    else:
      self._setup_remote_database()
    pass

  def reset_database(self):
    print 'Resetting {} database...'.format(self.db_title)

    if self._is_local_database():
      self._reset_local_database()
    else:
      self._reset_remote_database()
    pass

  def ensure_jdbc_driver_installed(self, args, properties):
    result = self._is_jdbc_driver_installed(properties)
    if result == -1:
      (result, msg) = self._prompt_jdbc_driver_install(properties)
      if result == -1:
        print_error_msg(msg)
        raise FatalException(-1, msg)

    if result != 1:
      if self._install_jdbc_driver(args, properties):
        return True
    return False


  #
  # Private implementation
  #

  #
  # Checks if options determine local DB configuration
  #
  def _is_local_database(self):
    return self.persistence_type == 'local'

  def _is_jdbc_driver_installed(self, properties):
    return 1

  def configure_database_password(showDefault=True):
    pass

  def _prompt_db_properties(self):
    #if WINDOWS
    #  prompt for SQL Server host and instance name
    #else
    #  go the classic Linux way
    #linux_prompt_db_properties(args)
    return False

  def _setup_local_server(self, properties):
    pass

  def _setup_local_database(self):
    pass

  def _reset_local_database(self):
    pass

  def _setup_remote_server(self, properties):
    pass

  def _setup_remote_database(self):
    pass

  def _reset_remote_database(self):
    pass

  def _prompt_jdbc_driver_install(self, properties):
    return (False, "")

  def _install_jdbc_driver(self, options, properties):
    return False

  def ensure_dbms_is_running(self, options, properties, scmStatus=None):
    pass

if OSCheck.is_windows_os():
  from ambari_server.dbConfiguration_windows import SQLServerAmbariDBConfig, SQLServerMetricsDBConfig
#else:
#  from ambari_server.dbConfiguration_linux import PostgreSQLConfig #and potentially MySQLConfig, OracleConfig
