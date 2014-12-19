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

import socket
import string
import win32api

from ambari_commons.exceptions import *
from ambari_commons.logging_utils import print_warning_msg
from ambari_commons.os_utils import search_file
from ambari_commons.os_windows import *
from ambari_commons.str_utils import compress_backslashes, ensure_double_backslashes
from ambari_server.setupSecurity import SECURITY_IS_ENCRYPTION_ENABLED, encrypt_password, store_password_file
from serverConfiguration import *
from dbConfiguration import *
from userInput import get_validated_string_input

#Import the SQL Server libraries

# SQL Server settings
DBPATH = 'C:\\Program Files\\Microsoft SQL Server\\MSSQL12.SQLEXPRESS\\MSSQL\\DATA\\'
# DBPATH = 'C:\\Program Files\\Microsoft SQL Server\\MSSQL10_50.MSSQLSERVER\\MSSQL\\DATA\\'

DATABASE_DBMS = "sqlserver"
DATABASE_DRIVER_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
LOCAL_DATABASE_SERVER = "localhost\\SQLEXPRESS"
AMBARI_DATABASE_NAME = "ambari"

METRICS_DATABASE_NAME = "HadoopMetrics"


class DbPropKeys:
  def __init__(self, i_dbms_key, i_driver_key, i_server_key, i_port_key, i_db_name_key, i_db_url_key):
    self.reset(i_dbms_key, i_driver_key, i_server_key, i_port_key, i_db_name_key, i_db_url_key)
    pass

  def reset(self, i_dbms_key, i_driver_key, i_server_key, i_port_key, i_db_name_key, i_db_url_key):
    self.dbms_key = i_dbms_key
    self.driver_key = i_driver_key
    self.server_key = i_server_key
    self.port_key = i_port_key
    self.db_name_key = i_db_name_key
    self.db_url_key = i_db_url_key
    pass

class AuthenticationKeys:
  def __init__(self, i_integrated_auth_key, i_user_name_key, i_password_key, i_password_alias, i_password_filename):
    self.reset(i_integrated_auth_key, i_user_name_key, i_password_key, i_password_alias, i_password_filename)
    pass

  def reset(self, i_integrated_auth_key, i_user_name_key, i_password_key, i_password_alias, i_password_filename):
    self.integrated_auth_key = i_integrated_auth_key
    self.user_name_key = i_user_name_key
    self.password_key = i_password_key
    self.password_alias = i_password_alias
    self.password_filename = i_password_filename
    pass

# SQL Server configuration and setup
class SQLServerConfig(DBMSConfig):
  def __init__(self, options, properties):
    super(SQLServerConfig, self).__init__(options, properties)

    """
    #Just load the defaults. The derived classes will be able to modify them later
    """
    self.dbms = DATABASE_DBMS
    self.driver_name = DATABASE_DRIVER_NAME

    # The values from options supersede the values from properties
    self.database_host = options.database_host if options.database_host is not None and options.database_host is not "" else \
        properties.get_property(self.dbPropKeys.server_key)
    try:
      if self.database_host is None or self.database_host is "":
        self.database_host = options.default_database_host
      else:
        self.database_host = compress_backslashes(self.database_host)
    except:
      self.database_host = "localhost\\SQLEXPRESS"
      pass
    self.database_port = options.database_port if options.database_port is not None and options.database_port is not "" else \
        properties.get_property(self.dbPropKeys.port_key)
    self.database_name = options.database_name if options.database_name is not None and options.database_name is not "" else \
        properties.get_property(self.dbPropKeys.db_name_key)

    self.use_windows_authentication = options.database_windows_auth if options.database_windows_auth is True else \
        properties.get_property(self.dbAuthKeys.integrated_auth_key)
    self.database_username = options.database_username if options.database_username is not None and options.database_username is not "" \
        else properties.get_property(self.dbAuthKeys.user_name_key)
    self.database_password = options.database_password if options.database_password is not None and options.database_password is not "" \
        else ""
    self.password_file = properties[self.dbAuthKeys.password_key]

    self.database_url = self._build_sql_server_connection_string()

    self.persistence_property = None

    self.db_title = ""

    self.env_var_db_name = ""
    self.env_var_db_log_name = ""
    self.env_var_db_owner = ""

    self.init_script_file = ""
    self.drop_tables_script_file = ""

  #
  # No local DB configuration supported
  #
  def _is_local_database(self):
    return False

  def _is_jdbc_driver_installed(self, properties):
    """
    #Attempt to load the sqljdbc4.jar and sqljdbc_auth.dll. This will automatically scan the PATH.
    :param None
    :rtype : bool
    """
    paths = "." + os.pathsep + os.environ["PATH"]

    # Find the jar by attempting to load it as a resource dll
    driver_path = search_file("sqljdbc4.jar", paths)
    if not driver_path:
      return 0

    auth_dll_path = search_file("sqljdbc_auth.dll", paths)
    if not auth_dll_path:
      return 0

    try:
      driver_path = properties[JDBC_DRIVER_PATH_PROPERTY]
      if driver_path is None or driver_path is "":
        return 0
    except Exception:
      # No such attribute set
      return 0

    return 1

  def get_jdbc_driver_path(self):
    paths = "." + os.pathsep + os.environ["PATH"]

    # Find the jar by attempting to load it as a resource dll
    driver_path = search_file("sqljdbc4.jar", paths)
    return driver_path

  def configure_database_password(showDefault=True):
    #No password needed, using SQL Server integrated authentication
    pass

  def _prompt_db_properties(self):
    if self.silent:
      # All the settings are supposed to be retrieved from the command-line parameters
      return True

    #prompt for SQL Server host and instance name
    hostname_prompt = "SQL Server host and instance for the {} database: ({}) ".format(self.db_title, self.database_host)
    self.database_host = get_validated_string_input(hostname_prompt, self.database_host, None, None, False, True)

    #prompt for SQL Server authentication method
    if (not self.use_windows_authentication is None and self.use_windows_authentication.lower() == "true") or \
            self.database_username is None or self.database_username == "":
      auth_option_default = '1'
    else:
      auth_option_default = '2'

    user_prompt = \
      "[1] - Use SQL Server integrated authentication\n[2] - Use username+password authentication\n" \
      "Enter choice ({}): ".format(auth_option_default)
    auth_option = get_validated_string_input(user_prompt,
                                             auth_option_default,
                                             "^[12]$",
                                             "Invalid number.",
                                             False
    )
    if str(auth_option) == '1':
      self.use_windows_authentication = True
      self.database_password = None
    else:
      self.use_windows_authentication = False

      user_prompt = "SQL Server user name for the {} database: ({}) ".format(self.db_title, self.database_username)
      username = get_validated_string_input(user_prompt, self.database_username, None, "User name", False,
                                            False)
      self.database_username = username

      user_prompt = "SQL Server password for the {} database: ".format(self.db_title)
      password = get_validated_string_input(user_prompt, "", None, "Password", True, False)
      self.database_password = password

    self.database_url = self._build_sql_server_connection_string()

    return True

  def _setup_remote_server(self, properties):
    properties.removeOldProp(self.dbPropKeys.port_key)
    properties.removeOldProp(self.dbAuthKeys.integrated_auth_key)
    properties.removeOldProp(self.dbAuthKeys.user_name_key)
    properties.removeOldProp(self.dbAuthKeys.password_key)

    properties.process_pair(self.persistence_property, 'remote')

    properties.process_pair(self.dbPropKeys.dbms_key, self.dbms)
    properties.process_pair(self.dbPropKeys.driver_key, self.driver_name)
    properties.process_pair(self.dbPropKeys.server_key, ensure_double_backslashes(self.database_host))
    if self.database_port is not None and self.database_port != "":
      properties.process_pair(self.dbPropKeys.port_key, self.database_port)
    properties.process_pair(self.dbPropKeys.db_name_key, self.database_name)

    self._store_db_auth_config(properties, self.dbAuthKeys)

    properties.process_pair(self.dbPropKeys.db_url_key, self.database_url)
    pass

  def _setup_remote_database(self):
    print 'Populating {} database structure...'.format(self.db_title)

    self._populate_database_structure()

  def _reset_remote_database(self):
    print 'Resetting {} database structure...'.format(self.db_title)

    self._populate_database_structure()

  def _prompt_jdbc_driver_install(self, properties):
    result = False
    msg = 'Before starting Ambari Server, you must install the SQL Server JDBC driver.'

    if not self.silent:
      print_warning_msg(msg)
      raw_input(PRESS_ENTER_MSG)
      result = self._is_jdbc_driver_installed(properties)
    return (result, msg)

  def _install_jdbc_driver(self, options, properties):
    try:
      driver_path = properties[JDBC_DRIVER_PATH_PROPERTY]
    except Exception:
      # No such attribute set
      driver_path = None

    if driver_path is None or driver_path == "":
      driver_path = self.get_jdbc_driver_path()

      properties.process_pair(JDBC_DRIVER_PATH_PROPERTY, driver_path)
      return True
    return False

  def ensure_dbms_is_running(self, options, properties, scmStatus=None):
    """
    :param scmStatus : SvcStatusCallback
    :rtype : None
    """

    db_host_components = self.database_host.split("\\")
    if len(db_host_components) == 1:
      db_machine = self.database_host
      sql_svc_name = "MSSQLServer"
    else:
      db_machine = db_host_components[0]
      sql_svc_name = "MSSQL$" + db_host_components[1]

    if db_machine == "localhost" or db_machine.lower() == os.getenv("COMPUTERNAME").lower() or \
      db_machine.lower() == socket.getfqdn().lower():
      #TODO: Configure the SQL Server service name in ambari.properties
      ret = WinServiceController.EnsureServiceIsStarted(sql_svc_name)
      if 0 != ret:
        raise FatalException(-1, "Error starting SQL Server: " + string(ret))

      if scmStatus is not None:
        scmStatus.reportStartPending()

      ret = WinServiceController.EnsureServiceIsStarted("SQLBrowser")  #The SQL Server JDBC driver needs this one
      if 0 != ret:
        raise FatalException(-1, "Error starting SQL Server Browser: " + string(ret))
    pass


  def _build_sql_server_connection_string(self):
    databaseUrl = "jdbc:sqlserver://{}".format(ensure_double_backslashes(self.database_host))
    if self.database_port is not None and self.database_port != "":
      databaseUrl += ":{}".format(self.database_port)
    databaseUrl += ";databaseName={}".format(self.database_name)
    if(self.use_windows_authentication):
      databaseUrl += ";integratedSecurity=true"
    #No need to append the username and password, the Ambari server adds them by itself when connecting to the database
    return databaseUrl

  def _store_db_auth_config(self, properties, keys):
    if (self.use_windows_authentication):
      properties.process_pair(keys.integrated_auth_key, "True")
      properties.removeProp(keys.password_key)
    else:
      properties.process_pair(keys.integrated_auth_key, "False")

      properties.process_pair(keys.user_name_key, self.database_username)

      if self.isSecure:
        encrypted_password = encrypt_password(keys.password_alias, self.database_password)
        if self.database_password != encrypted_password:
          properties.process_pair(keys.password_key, encrypted_password)
      else:
        passwordFile = store_password_file(self.database_password, keys.password_filename)
        properties.process_pair(keys.password_key, passwordFile)

  def _populate_database_structure(self):
    # Setup DB
    os.environ[self.env_var_db_name] = self.database_name
    os.environ[self.env_var_db_log_name] = self.database_name + '_log'
    os.environ[self.env_var_db_owner] = 'hadoop'

    # Don't create the database, assume it already exists. Just clear out the known tables structure
    SQLServerConfig._execute_db_script(self.database_host, self.drop_tables_script_file)

    # Init DB
    SQLServerConfig._execute_db_script(self.database_host, self.init_script_file)
    pass

  @staticmethod
  def _execute_db_script(databaseHost, databaseScript):
    dbCmd = 'sqlcmd -S {} -i {}'.format(databaseHost, databaseScript)
    retCode, outData, errData = run_os_command(['cmd', '/C', dbCmd])
    if not retCode == 0:
      err = 'Running database create script failed. Error output: {} Output: {} Exiting.'.format(errData, outData)
      raise FatalException(retCode, err)
    print_info_msg("sqlcmd output:")
    print_info_msg(outData)
    pass

# SQL Server Ambari database configuration and setup
class SQLServerAmbariDBConfig(SQLServerConfig):
  def __init__(self, options, properties):
    self.dbPropKeys = DbPropKeys(
      JDBC_DATABASE_PROPERTY,
      JDBC_DRIVER_PROPERTY,
      JDBC_HOSTNAME_PROPERTY,
      JDBC_PORT_PROPERTY,
      JDBC_SCHEMA_PROPERTY,
      JDBC_URL_PROPERTY)
    self.dbAuthKeys = AuthenticationKeys(
      JDBC_USE_INTEGRATED_AUTH_PROPERTY,
      JDBC_USER_NAME_PROPERTY,
      JDBC_PASSWORD_PROPERTY,
      JDBC_RCA_PASSWORD_ALIAS,
      JDBC_PASSWORD_FILENAME
    )

    super(SQLServerAmbariDBConfig, self).__init__(options, properties)

    if self.database_name is None or self.database_name is "":
      self.database_name = AMBARI_DATABASE_NAME

    self.persistence_property = PERSISTENCE_TYPE_PROPERTY

    self.db_title = "ambari"

    self.env_var_db_name ='AMBARIDBNAME'
    self.env_var_db_log_name = 'AMBARIDBLOGNAME'
    self.env_var_db_owner = 'AMBARIDBOWNER'

    # The values from options supersede the values from properties
    if options.init_db_script_file is not None and options.init_db_script_file is not "":
      self.init_script_file = compress_backslashes(options.init_db_script_file)
    else:
      self.init_script_file = "resources" + os.path.sep + "Ambari-DDL-SQLServer-CREATE.sql"
    if options.cleanup_db_script_file is not None and options.cleanup_db_script_file is not "":
      self.drop_tables_script_file = compress_backslashes(options.cleanup_db_script_file)
    else:
      self.drop_tables_script_file = "resources" + os.path.sep + "Ambari-DDL-SQLServer-DROP.sql"
    pass

  def _setup_remote_server(self, properties):
    super(SQLServerAmbariDBConfig, self)._setup_remote_server(properties)

    properties.process_pair(JDBC_RCA_DRIVER_PROPERTY, self.driver_name)
    properties.process_pair(JDBC_RCA_HOSTNAME_PROPERTY, ensure_double_backslashes(self.database_host))
    if self.database_port is not None and self.database_port != "":
      properties.process_pair(JDBC_RCA_PORT_PROPERTY, self.database_port)
    properties.process_pair(JDBC_RCA_SCHEMA_PROPERTY, self.database_name)

    authKeys = AuthenticationKeys(
      JDBC_RCA_USE_INTEGRATED_AUTH_PROPERTY,
      JDBC_RCA_USER_NAME_PROPERTY,
      JDBC_RCA_PASSWORD_FILE_PROPERTY,
      JDBC_RCA_PASSWORD_ALIAS,
      JDBC_PASSWORD_FILENAME
    )
    self._store_db_auth_config(properties, authKeys)

    properties.process_pair(JDBC_RCA_URL_PROPERTY, self.database_url)
    pass


# SQL Server Metrics database configuration and setup
class SQLServerMetricsDBConfig(SQLServerConfig):
  def __init__(self, options, properties):
    self.dbPropKeys = DbPropKeys(
      JDBC_METRICS_DATABASE_PROPERTY,
      JDBC_METRICS_DRIVER_PROPERTY,
      JDBC_METRICS_HOSTNAME_PROPERTY,
      JDBC_METRICS_PORT_PROPERTY,
      JDBC_METRICS_SCHEMA_PROPERTY,
      JDBC_METRICS_URL_PROPERTY)
    self.dbAuthKeys = AuthenticationKeys(
      JDBC_METRICS_USE_INTEGRATED_AUTH_PROPERTY,
      JDBC_METRICS_USER_NAME_PROPERTY,
      JDBC_METRICS_PASSWORD_PROPERTY,
      JDBC_METRICS_PASSWORD_ALIAS,
      JDBC_METRICS_PASSWORD_FILENAME
    )

    super(SQLServerMetricsDBConfig, self).__init__(options, properties)

    self.database_name = METRICS_DATABASE_NAME

    self.persistence_property = METRICS_PERSISTENCE_TYPE_PROPERTY

    self.db_title = "metrics"

    self.env_var_db_name ='METRICSDBNAME'
    self.env_var_db_log_name = 'METRICSDBLOGNAME'
    self.env_var_db_owner = 'METRICSDBOWNER'

    if options.init_metrics_db_script_file is not None and options.init_metrics_db_script_file is not "":
      self.init_script_file = compress_backslashes(options.init_db_script_file)
    else:
      self.init_script_file = "resources" + os.sep + "Hadoop-Metrics-SQLServer-CREATE.sql"
    if options.cleanup_metrics_db_script_file is not None and options.cleanup_metrics_db_script_file is not "":
      self.drop_tables_script_file = compress_backslashes(options.cleanup_db_script_file)
    else:
      self.drop_tables_script_file = "resources" + os.sep + "Hadoop-Metrics-SQLServer-DROP.sql"
    pass


# SQL Server database
class SQLServerDatabase:
  def __init__(self):
    #Init the database connection here
    pass

  def get_running_status(self):
    #if the connection is active, return running
    #else return stopped
    return DB_STATUS_RUNNING_DEFAULT
