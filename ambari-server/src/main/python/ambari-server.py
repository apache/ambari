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

import optparse
import shlex
import sys
import os
import signal
import subprocess
import re
import string
import glob
import platform
import shutil
import stat
import fileinput
import urllib2
import time
import getpass
import socket
import datetime
import tempfile
import random
import pwd
from ambari_server.resourceFilesKeeper import ResourceFilesKeeper, KeeperException
import json
import base64
from threading import Thread
from ambari_commons import OSCheck, OSConst, Firewall
from ambari_server import utils

# debug settings
VERBOSE = False
SILENT = False
SERVER_START_DEBUG = False

# ldap settings
LDAP_SYNC_ALL = False
LDAP_SYNC_EXISTING = False
LDAP_SYNC_USERS = None
LDAP_SYNC_GROUPS = None

# OS info
OS_VERSION = OSCheck().get_os_major_version()
OS_TYPE = OSCheck.get_os_type()
OS_FAMILY = OSCheck.get_os_family()

# action commands
SETUP_ACTION = "setup"
START_ACTION = "start"
STOP_ACTION = "stop"
RESET_ACTION = "reset"
UPGRADE_ACTION = "upgrade"
UPGRADE_STACK_ACTION = "upgradestack"
STATUS_ACTION = "status"
SETUP_HTTPS_ACTION = "setup-https"
LDAP_SETUP_ACTION = "setup-ldap"
LDAP_SYNC_ACTION = "sync-ldap"
SETUP_GANGLIA_HTTPS_ACTION = "setup-ganglia-https"
SETUP_NAGIOS_HTTPS_ACTION = "setup-nagios-https"
ENCRYPT_PASSWORDS_ACTION = "encrypt-passwords"
SETUP_SECURITY_ACTION = "setup-security"
REFRESH_STACK_HASH_ACTION = "refresh-stack-hash"

ACTION_REQUIRE_RESTART = [RESET_ACTION, UPGRADE_ACTION, UPGRADE_STACK_ACTION,
                          SETUP_SECURITY_ACTION, LDAP_SETUP_ACTION]

# selinux commands
GET_SE_LINUX_ST_CMD = utils.locate_file('sestatus', '/usr/sbin')
SE_SETENFORCE_CMD = "setenforce 0"
SE_STATUS_DISABLED = "disabled"
SE_STATUS_ENABLED = "enabled"
SE_MODE_ENFORCING = "enforcing"
SE_MODE_PERMISSIVE = "permissive"

# server commands
ambari_provider_module_option = ""
ambari_provider_module = os.environ.get('AMBARI_PROVIDER_MODULE')

# Non-root user setup commands
NR_USER_PROPERTY = "ambari-server.user"
NR_USER_COMMENT = "Ambari user"
NR_GET_OWNER_CMD = 'stat -c "%U" {0}'
NR_USERADD_CMD = 'useradd -M --comment "{1}" ' \
                 '--shell %s -d /var/lib/ambari-server/keys/ {0}' % utils.locate_file('nologin', '/sbin')
NR_SET_USER_COMMENT_CMD = 'usermod -c "{0}" {1}'
NR_CHMOD_CMD = 'chmod {0} {1} {2}'
NR_CHOWN_CMD = 'chown {0} {1} {2}'

RECURSIVE_RM_CMD = 'rm -rf {0}'

SSL_PASSWORD_FILE = "pass.txt"
SSL_PASSIN_FILE = "passin.txt"

# openssl command
VALIDATE_KEYSTORE_CMD = "openssl pkcs12 -info -in '{0}' -password file:'{1}' -passout file:'{2}'"
EXPRT_KSTR_CMD = "openssl pkcs12 -export -in '{0}' -inkey '{1}' -certfile '{0}' -out '{4}' -password file:'{2}' -passin file:'{3}'"
CHANGE_KEY_PWD_CND = 'openssl rsa -in {0} -des3 -out {0}.secured -passout pass:{1}'
GET_CRT_INFO_CMD = 'openssl x509 -dates -subject -in {0}'

#keytool commands
KEYTOOL_IMPORT_CERT_CMD = "{0}" + os.sep + "bin" + os.sep + "keytool -import -alias '{1}' -storetype '{2}' -file '{3}' -storepass '{4}' -noprompt"
KEYTOOL_DELETE_CERT_CMD = "{0}" + os.sep + "bin" + os.sep + "keytool -delete -alias '{1}' -storepass '{2}' -noprompt"
KEYTOOL_KEYSTORE = " -keystore '{0}'"

# constants
STACK_NAME_VER_SEP = "-"
JAVA_SHARE_PATH = "/usr/share/java"
SERVER_OUT_FILE = "/var/log/ambari-server/ambari-server.out"
SERVER_LOG_FILE = "/var/log/ambari-server/ambari-server.log"
BLIND_PASSWORD = "*****"
ROOT_FS_PATH = "/"

# api properties
SERVER_API_HOST = '127.0.0.1'
SERVER_API_PROTOCOL = 'http'
SERVER_API_PORT = '8080'
SERVER_API_LDAP_URL = '/api/v1/ldap_sync_events'

# terminal styles
BOLD_ON = '\033[1m'
BOLD_OFF = '\033[0m'

#Common messages
PRESS_ENTER_MSG = "Press <enter> to continue."

#Common setup or upgrade message
SETUP_OR_UPGRADE_MSG = "- If this is a new setup, then run the \"ambari-server setup\" command to create the user\n" \
"- If this is an upgrade of an existing setup, run the \"ambari-server upgrade\" command.\n" \
"Refer to the Ambari documentation for more information on setup and upgrade."

AMBARI_SERVER_DIE_MSG = "Ambari Server java process died with exitcode {0}. Check {1} for more information."
#SSL certificate metainfo
COMMON_NAME_ATTR = 'CN'
NOT_BEFORE_ATTR = 'notBefore'
NOT_AFTER_ATTR = 'notAfter'

if ambari_provider_module is not None:
  ambari_provider_module_option = "-Dprovider.module.class=" +\
                                  ambari_provider_module + " "

SERVER_START_CMD = "{0}" + os.sep + "bin" + os.sep +\
                 "java -server -XX:NewRatio=3 "\
                 "-XX:+UseConcMarkSweepGC " +\
                 "-XX:-UseGCOverheadLimit -XX:CMSInitiatingOccupancyFraction=60 " +\
                 ambari_provider_module_option +\
                 os.getenv('AMBARI_JVM_ARGS', '-Xms512m -Xmx2048m') +\
                 " -cp {1}" + os.pathsep + "{2}" +\
                 " org.apache.ambari.server.controller.AmbariServer "\
                 ">" + SERVER_OUT_FILE + " 2>&1 || echo $? > {3} &"
SERVER_START_CMD_DEBUG = "{0}" + os.sep + "bin" + os.sep +\
                       "java -server -XX:NewRatio=2 -XX:+UseConcMarkSweepGC " +\
                       ambari_provider_module_option +\
                       os.getenv('AMBARI_JVM_ARGS', '-Xms512m -Xmx2048m') +\
                       " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,"\
                       "server=y,suspend=n -cp {1}" + os.pathsep + "{2}" +\
                       " org.apache.ambari.server.controller.AmbariServer"
SERVER_SEARCH_PATTERN = "org.apache.ambari.server.controller.AmbariServer"
SECURITY_PROVIDER_GET_CMD = "{0}" + os.sep + "bin" + os.sep + "java -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.security.encryption" +\
                          ".CredentialProvider GET {3} {4} {5} " +\
                          "> " + SERVER_OUT_FILE + " 2>&1"

SECURITY_PROVIDER_PUT_CMD = "{0}" + os.sep + "bin" + os.sep + "java -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.security.encryption" +\
                          ".CredentialProvider PUT {3} {4} {5} " +\
                          "> " + SERVER_OUT_FILE + " 2>&1"

SECURITY_PROVIDER_KEY_CMD = "{0}" + os.sep + "bin" + os.sep + "java -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.security.encryption" +\
                          ".MasterKeyServiceImpl {3} {4} {5} " +\
                          "> " + SERVER_OUT_FILE + " 2>&1"

SCHEMA_UPGRADE_HELPER_CMD = "{0}" + os.sep + "bin" + os.sep + "java -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.upgrade.SchemaUpgradeHelper" +\
                          " > " + SERVER_OUT_FILE + " 2>&1"

STACK_UPGRADE_HELPER_CMD = "{0}" + os.sep + "bin" + os.sep + "java -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.upgrade.StackUpgradeHelper" +\
                          " {3} {4} > " + SERVER_OUT_FILE + " 2>&1"


VIEW_EXTRACT_CMD = "{0}" + os.sep + "bin" + os.sep + "java -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.view.ViewRegistry extract {3} " +\
                          "> " + SERVER_OUT_FILE + " 2>&1"


ULIMIT_CMD = "ulimit -n"
SERVER_INIT_TIMEOUT = 5
SERVER_START_TIMEOUT = 10
SECURITY_KEYS_DIR = "security.server.keys_dir"
SECURITY_MASTER_KEY_LOCATION = "security.master.key.location"
SECURITY_KEY_IS_PERSISTED = "security.master.key.ispersisted"
SECURITY_KEY_ENV_VAR_NAME = "AMBARI_SECURITY_MASTER_KEY"
SECURITY_MASTER_KEY_FILENAME = "master"
SECURITY_IS_ENCRYPTION_ENABLED = "security.passwords.encryption.enabled"
SECURITY_KERBEROS_JASS_FILENAME = "krb5JAASLogin.conf"

SSL_KEY_DIR = 'security.server.keys_dir'
SSL_API_PORT = 'client.api.ssl.port'
SSL_API = 'api.ssl'
SSL_SERVER_CERT_NAME = 'client.api.ssl.cert_name'
SSL_SERVER_KEY_NAME = 'client.api.ssl.key_name'
SSL_CERT_FILE_NAME = "https.crt"
SSL_KEY_FILE_NAME = "https.key"
SSL_KEYSTORE_FILE_NAME = "https.keystore.p12"
SSL_KEY_PASSWORD_FILE_NAME = "https.pass.txt"
SSL_KEY_PASSWORD_LENGTH = 50
DEFAULT_SSL_API_PORT = 8443
SSL_DATE_FORMAT = '%b  %d %H:%M:%S %Y GMT'

GANGLIA_HTTPS = 'ganglia.https'
NAGIOS_HTTPS = 'nagios.https'

JDBC_RCA_PASSWORD_ALIAS = "ambari.db.password"
CLIENT_SECURITY_KEY = "client.security"

IS_LDAP_CONFIGURED = "ambari.ldap.isConfigured"
LDAP_MGR_PASSWORD_ALIAS = "ambari.ldap.manager.password"
LDAP_MGR_PASSWORD_PROPERTY = "authentication.ldap.managerPassword"
LDAP_MGR_PASSWORD_FILENAME = "ldap-password.dat"
LDAP_MGR_USERNAME_PROPERTY = "authentication.ldap.managerDn"
LDAP_PRIMARY_URL_PROPERTY = "authentication.ldap.primaryUrl"

SSL_TRUSTSTORE_PASSWORD_ALIAS = "ambari.ssl.trustStore.password"
SSL_TRUSTSTORE_PATH_PROPERTY = "ssl.trustStore.path"
SSL_TRUSTSTORE_PASSWORD_PROPERTY = "ssl.trustStore.password"
SSL_TRUSTSTORE_TYPE_PROPERTY = "ssl.trustStore.type"

AMBARI_CONF_VAR = "AMBARI_CONF_DIR"
AMBARI_SERVER_LIB = "AMBARI_SERVER_LIB"
JAVA_HOME = "JAVA_HOME"
PID_DIR = "/var/run/ambari-server"
BOOTSTRAP_DIR_PROPERTY = "bootstrap.dir"
PID_NAME = "ambari-server.pid"
EXITCODE_NAME = "ambari-server.exitcode"
AMBARI_PROPERTIES_FILE = "ambari.properties"
AMBARI_PROPERTIES_RPMSAVE_FILE = "ambari.properties.rpmsave"
RESOURCES_DIR_PROPERTY = "resources.dir"

SETUP_DB_CONNECT_TIMEOUT = 5
SETUP_DB_CONNECT_ATTEMPTS = 3
SETUP_DB_CMD = ['su', '-', 'postgres',
        '--command=psql -f {0} -v username=\'"{1}"\' -v password="\'{2}\'" -v dbname="{3}"']
UPGRADE_STACK_CMD = ['su', 'postgres',
        '--command=psql -f {0} -v stack_name="\'{1}\'"  -v stack_version="\'{2}\'" -v dbname="{3}"']

CHANGE_OWNER_COMMAND = ['su', '-', 'postgres',
                        '--command=/var/lib/ambari-server/resources/scripts/change_owner.sh -d {0} -s {1} -o {2}']

PG_ERROR_BLOCKED = "is being accessed by other users"
PG_STATUS_RUNNING = utils.get_postgre_running_status(OS_TYPE)
PG_DEFAULT_PASSWORD = "bigdata"
SERVICE_CMD = "/usr/bin/env service"
PG_SERVICE_NAME = "postgresql"
PG_HBA_DIR = utils.get_postgre_hba_dir(OS_FAMILY)

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

SERVER_VERSION_FILE_PATH = "server.version.file"

JDBC_DATABASE_PROPERTY = "server.jdbc.database"             # E.g., embedded|oracle|mysql|postgres
JDBC_DATABASE_NAME_PROPERTY = "server.jdbc.database_name"   # E.g., ambari
JDBC_HOSTNAME_PROPERTY = "server.jdbc.hostname"
JDBC_PORT_PROPERTY = "server.jdbc.port"
JDBC_POSTGRES_SCHEMA_PROPERTY = "server.jdbc.postgres.schema"   # Only for postgres, defaults to same value as DB name

VIEWS_DIR_PROPERTY = "views.dir"
DEFAULT_VIEWS_DIR = "/var/lib/ambari-server/resources/views"

JDBC_USER_NAME_PROPERTY = "server.jdbc.user.name"
JDBC_PASSWORD_PROPERTY = "server.jdbc.user.passwd"
JDBC_PASSWORD_FILENAME = "password.dat"
JDBC_RCA_PASSWORD_FILENAME = "rca_password.dat"

CLIENT_API_PORT_PROPERTY = "client.api.port"
CLIENT_API_PORT = "8080"

SRVR_TWO_WAY_SSL_PORT_PROPERTY = "security.server.two_way_ssl.port"
SRVR_TWO_WAY_SSL_PORT = "8441"

SRVR_ONE_WAY_SSL_PORT_PROPERTY = "security.server.one_way_ssl.port"
SRVR_ONE_WAY_SSL_PORT = "8440"

PERSISTENCE_TYPE_PROPERTY = "server.persistence.type"
JDBC_DRIVER_PROPERTY = "server.jdbc.driver"
JDBC_URL_PROPERTY = "server.jdbc.url"

JDBC_RCA_DRIVER_PROPERTY = "server.jdbc.rca.driver"
JDBC_RCA_URL_PROPERTY = "server.jdbc.rca.url"
JDBC_RCA_USER_NAME_PROPERTY = "server.jdbc.rca.user.name"
JDBC_RCA_PASSWORD_FILE_PROPERTY = "server.jdbc.rca.user.passwd"

CHECK_COMMAND_EXIST_CMD = "type {0}"

DATABASE_INDEX = 0
PROMPT_DATABASE_OPTIONS = False
USERNAME_PATTERN = "^[a-zA-Z_][a-zA-Z0-9_\-]*$"
PASSWORD_PATTERN = "^[a-zA-Z0-9_-]*$"
DATABASE_TYPES = ["postgres", "oracle", "mysql"]
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

REGEX_IP_ADDRESS = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
REGEX_HOSTNAME = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"
REGEX_HOSTNAME_PORT = "^(.*:[0-9]{1,5}$)"
REGEX_TRUE_FALSE = "^(true|false)?$"
REGEX_ANYTHING = ".*"


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


# jdk commands
JDK_NAMES = ["jdk-7u67-linux-x64.tar.gz", "jdk-6u31-linux-x64.bin"]
JDK_URL_PROPERTIES = ["jdk1.7.url", "jdk1.6.url"]
JCE_URL_PROPERTIES = ["jce_policy1.7.url", "jce_policy1.6.url"]
DEFAULT_JDK16_LOCATION = "/usr/jdk64/jdk1.6.0_31"
JDK_INDEX = 0
JDK_VERSION_REs = ["(jdk.*)/jre", "Creating (jdk.*)/jre"]
CUSTOM_JDK_NUMBER = "3"
JDK_MIN_FILESIZE = 5000
JDK_INSTALL_DIR = "/usr/jdk64"
CREATE_JDK_DIR_CMD = "/bin/mkdir -p " + JDK_INSTALL_DIR
MAKE_FILE_EXECUTABLE_CMD = "chmod a+x {0}"
JAVA_HOME_PROPERTY = "java.home"
JDK_NAME_PROPERTY = "jdk.name"
JCE_NAME_PROPERTY = "jce.name"
OS_TYPE_PROPERTY = "server.os_type"
GET_FQDN_SERVICE_URL = "server.fqdn.service.url"

JDK_DOWNLOAD_CMD = "curl --create-dirs -o {0} {1}"
JDK_DOWNLOAD_SIZE_CMD = "curl -I {0}"

# use --no-same-owner when running as root to prevent uucp as the user (AMBARI-6478)
UNTAR_JDK_ARCHIVE = "tar --no-same-owner -xvf {0}"

#JCE Policy files
JCE_POLICY_FILENAMES = ["UnlimitedJCEPolicyJDK7.zip", "jce_policy-6.zip"]
JCE_DOWNLOAD_CMD = "curl -o {0} {1}"
JCE_MIN_FILESIZE = 5000

DEFAULT_DB_NAME = "ambari"

# stack repo upgrade
STACK_LOCATION_KEY = 'metadata.path'
STACK_LOCATION_DEFAULT = '/var/lib/ambari-server/resources/stacks'

# linux open-file limit
ULIMIT_OPEN_FILES_KEY = 'ulimit.open.files'
ULIMIT_OPEN_FILES_DEFAULT = 10000

#Apache License Header
ASF_LICENSE_HEADER = '''
# Copyright 2011 The Apache Software Foundation
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
'''

def get_conf_dir():
  try:
    conf_dir = os.environ[AMBARI_CONF_VAR]
    return conf_dir
  except KeyError:
    default_conf_dir = "/etc/ambari-server/conf"
    print AMBARI_CONF_VAR + " is not set, using default " + default_conf_dir
    return default_conf_dir


def find_properties_file():
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  if conf_file is None:
    err = 'File %s not found in search path $%s: %s' % (AMBARI_PROPERTIES_FILE,
          AMBARI_CONF_VAR, get_conf_dir())
    print err
    raise FatalException(1, err)
  else:
    print_info_msg('Loading properties from ' + conf_file)
  return conf_file


def update_ambari_properties():
  prev_conf_file = search_file(AMBARI_PROPERTIES_RPMSAVE_FILE, get_conf_dir())
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())

  # Previous config file does not exist
  if (not prev_conf_file) or (prev_conf_file is None):
    print_warning_msg("Can not find ambari.properties.rpmsave file from previous version, skipping import of settings")
    return 0

  try:
    old_properties = Properties()
    old_properties.load(open(prev_conf_file))
  except Exception, e:
    print 'Could not read "%s": %s' % (prev_conf_file, e)
    return -1

  try:
    new_properties = Properties()
    new_properties.load(open(conf_file))

    for prop_key, prop_value in old_properties.getPropertyDict().items():
      if ("agent.fqdn.service.url" == prop_key):
        #BUG-7179 what is agent.fqdn property in ambari.props?
        new_properties.process_pair(GET_FQDN_SERVICE_URL, prop_value)
      elif ("server.os_type" == prop_key):
        new_properties.process_pair(OS_TYPE_PROPERTY, OS_FAMILY + OS_VERSION)
      else:
        new_properties.process_pair(prop_key, prop_value)

    # Adding custom user name property if it is absent
    # In previous versions without custom user support server was started as
    # "root" anyway so it's a reasonable default
    if not NR_USER_PROPERTY in new_properties.keys():
      new_properties.process_pair(NR_USER_PROPERTY, "root")

    isJDK16Installed = new_properties.get_property(JAVA_HOME_PROPERTY) == DEFAULT_JDK16_LOCATION
    if not JDK_NAME_PROPERTY in new_properties.keys() and isJDK16Installed:
      new_properties.process_pair(JDK_NAME_PROPERTY, JDK_NAMES[1])

    if not JCE_NAME_PROPERTY in new_properties.keys() and isJDK16Installed:
      new_properties.process_pair(JCE_NAME_PROPERTY, JCE_POLICY_FILENAMES[1])

    new_properties.store(open(conf_file, 'w'))

  except Exception, e:
    print 'Could not write "%s": %s' % (conf_file, e)
    return -1

  timestamp = datetime.datetime.now()
  format = '%Y%m%d%H%M%S'
  os.rename(prev_conf_file, prev_conf_file + '.' + timestamp.strftime(format))

  return 0


NR_CONF_DIR = get_conf_dir()

# ownership/permissions mapping
# path - permissions - user - group - recursive
# Rules are executed in the same order as they are listed
# {0} in user/group will be replaced by customized ambari-server username
NR_ADJUST_OWNERSHIP_LIST = [

  ("/var/log/ambari-server", "644", "{0}", True),
  ("/var/log/ambari-server", "755", "{0}", False),
  ("/var/run/ambari-server", "644", "{0}", True),
  ("/var/run/ambari-server", "755", "{0}", False),
  ("/var/run/ambari-server/bootstrap", "755", "{0}", False),
  ("/var/lib/ambari-server/ambari-env.sh", "700", "{0}", False),
  ("/var/lib/ambari-server/keys", "600", "{0}", True),
  ("/var/lib/ambari-server/keys", "700", "{0}", False),
  ("/var/lib/ambari-server/keys/db", "700", "{0}", False),
  ("/var/lib/ambari-server/keys/db/newcerts", "700", "{0}", False),
  ("/var/lib/ambari-server/keys/.ssh", "700", "{0}", False),
  ("/var/lib/ambari-server/resources/stacks/", "755", "{0}", True),
  ("/var/lib/ambari-server/resources/custom_actions/", "755", "{0}", True),
  ("/var/lib/ambari-server/resources/views", "644", "{0}", True),
  ("/var/lib/ambari-server/resources/views", "755", "{0}", False),
  ("/var/lib/ambari-server/resources/views/work", "755", "{0}", True),
  ("/etc/ambari-server/conf", "644", "{0}", True),
  ("/etc/ambari-server/conf", "755", "{0}", False),
  ("/etc/ambari-server/conf/password.dat", "640", "{0}", False),
  ("/var/lib/ambari-server/keys/pass.txt", "640", "{0}", False),
  ("/etc/ambari-server/conf/ldap-password.dat", "640", "{0}", False),
  ("/var/run/ambari-server/stack-recommendations/", "644", "{0}", True),
  ("/var/run/ambari-server/stack-recommendations/", "755", "{0}", False),
  ("/var/lib/ambari-server/data/tmp/", "644", "{0}", True),
  ("/var/lib/ambari-server/data/tmp/", "755", "{0}", False),
  # Also, /etc/ambari-server/conf/password.dat
  # is generated later at store_password_file
]

### System interaction ###


class FatalException(Exception):
    def __init__(self, code, reason):
      self.code = code
      self.reason = reason

    def __str__(self):
        return repr("Fatal exception: %s, exit code %s" % (self.reason, self.code))

class NonFatalException(Exception):
  def __init__(self, reason):
    self.reason = reason

  def __str__(self):
    return repr("NonFatal exception: %s" % self.reason)


def is_root():
  '''
  Checks effective UUID
  Returns True if a program is running under root-level privileges.
  '''
  return os.geteuid() == 0


def get_exec_path(cmd):
  cmd = 'which {0}'.format(cmd)
  ret, out, err = run_in_shell(cmd)
  if ret == 0:
    return out.strip()
  else:
    return None


def run_in_shell(cmd):
  print_info_msg('about to run command: ' + str(cmd))
  process = subprocess.Popen(cmd,
                             stdout=subprocess.PIPE,
                             stdin=subprocess.PIPE,
                             stderr=subprocess.PIPE,
                             shell=True
                             )
  (stdoutdata, stderrdata) = process.communicate()
  return process.returncode, stdoutdata, stderrdata


def run_os_command(cmd):
  print_info_msg('about to run command: ' + str(cmd))
  if type(cmd) == str:
    cmd = shlex.split(cmd)
  process = subprocess.Popen(cmd,
                             stdout=subprocess.PIPE,
                             stdin=subprocess.PIPE,
                             stderr=subprocess.PIPE
                             )
  (stdoutdata, stderrdata) = process.communicate()
  return process.returncode, stdoutdata, stderrdata


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


def read_ambari_user():
  '''
  Reads ambari user from properties file
  '''
  conf_file = find_properties_file()
  try:
    properties = Properties()
    properties.load(open(conf_file))
    user = properties[NR_USER_PROPERTY]
    if user:
      return user
    else:
      return None
  except Exception, e:
    print_error_msg('Could not read "%s": %s' % (conf_file, e))
    return None


def adjust_directory_permissions(ambari_user):
  properties = get_ambari_properties()
  bootstrap_dir = get_value_from_properties(properties, BOOTSTRAP_DIR_PROPERTY)
  print_info_msg("Cleaning bootstrap directory ({0}) contents...".format(bootstrap_dir))
  cmd = RECURSIVE_RM_CMD.format(bootstrap_dir)
  run_os_command(cmd)
  os.makedirs(bootstrap_dir)
  # Add master key and credential store if exists
  keyLocation = get_master_key_location(properties)
  masterKeyFile = search_file(SECURITY_MASTER_KEY_FILENAME, keyLocation)
  if masterKeyFile:
    NR_ADJUST_OWNERSHIP_LIST.append((masterKeyFile, "600", "{0}", "{0}", False))
  credStoreFile = get_credential_store_location(properties)
  if os.path.exists(credStoreFile):
    NR_ADJUST_OWNERSHIP_LIST.append((credStoreFile, "600", "{0}", "{0}", False))
  trust_store_location = properties[SSL_TRUSTSTORE_PATH_PROPERTY]
  if trust_store_location:
    NR_ADJUST_OWNERSHIP_LIST.append((trust_store_location, "600", "{0}", "{0}", False))
  print "Adjusting ambari-server permissions and ownership..."
  
  for pack in NR_ADJUST_OWNERSHIP_LIST:
    file = pack[0]
    mod = pack[1]
    user = pack[2].format(ambari_user)
    recursive = pack[3]
    set_file_permissions(file, mod, user, recursive)


def set_file_permissions(file, mod, user, recursive):
  WARN_MSG = "Command {0} returned exit code {1} with message: {2}"
  if recursive:
    params = " -R "
  else:
    params = ""
  if os.path.exists(file):
    command = NR_CHMOD_CMD.format(params, mod, file)
    retcode, out, err = run_os_command(command)
    if retcode != 0:
      print_warning_msg(WARN_MSG.format(command, file, err))
    command = NR_CHOWN_CMD.format(params, user, file)
    retcode, out, err = run_os_command(command)
    if retcode != 0:
      print_warning_msg(WARN_MSG.format(command, file, err))
  else:
    print_info_msg("File %s does not exist" % file)


def create_custom_user():
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

def check_reverse_lookup():
  """
  Check if host fqdn resolves to current host ip
  """
  try:
    host_name = socket.gethostname()
    host_ip = socket.gethostbyname(host_name)
    host_fqdn = socket.getfqdn()
    fqdn_ip = socket.gethostbyname(host_fqdn)
    return host_ip == fqdn_ip
  except socket.herror:
    pass
  return False

def check_ambari_user():
  try:
    user = read_ambari_user()
    create_user = False
    update_user_setting = False
    if user is not None:
      create_user = get_YN_input("Ambari-server daemon is configured to run under user '{0}'."
                        " Change this setting [y/n] (n)? ".format(user), False)
      update_user_setting = create_user  # Only if we will create another user
    else:  # user is not configured yet
      update_user_setting = True  # Write configuration anyway
      create_user = get_YN_input("Customize user account for ambari-server "
                   "daemon [y/n] (n)? ", False)
      if not create_user:
        user = "root"

    if create_user:
      (retcode, user) = create_custom_user()
      if retcode != 0:
        return retcode

    if update_user_setting:
      write_property(NR_USER_PROPERTY, user)

    adjust_directory_permissions(user)
  except OSError as e:
    print_error_msg("Failed: %s" % e.strerror)
    return 4
  except Exception as e:
    print_error_msg("Unexpected error %s" % e)
    return 1
  return 0

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


def write_property(key, value):
  conf_file = find_properties_file()
  properties = Properties()
  try:
    properties.load(open(conf_file))
  except Exception, e:
    print_error_msg('Could not read ambari config file "%s": %s' % (conf_file, e))
    return -1
  properties.process_pair(key, value)
  try:
    properties.store(open(conf_file, "w"))
  except Exception, e:
    print_error_msg('Could not write ambari config file "%s": %s' % (conf_file, e))
    return -1
  return 0


def remove_property(key):
  conf_file = find_properties_file()
  properties = Properties()
  try:
    properties.load(open(conf_file))
  except Exception, e:
    print_error_msg('Could not read ambari config file "%s": %s' % (conf_file, e))
    return -1
  properties.removeOldProp(key)
  try:
    properties.store(open(conf_file, "w"))
  except Exception, e:
    print_error_msg('Could not write ambari config file "%s": %s' % (conf_file, e))
    return -1
  return 0


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


def store_password_file(password, filename):
  conf_file = find_properties_file()
  passFilePath = os.path.join(os.path.dirname(conf_file),
    filename)

  with open(passFilePath, 'w+') as passFile:
    passFile.write(password)
  print_info_msg("Adjusting filesystem permissions")
  ambari_user = read_ambari_user()
  set_file_permissions(passFilePath, "660", ambari_user, False)

  return passFilePath


def remove_password_file(filename):
  conf_file = find_properties_file()
  passFilePath = os.path.join(os.path.dirname(conf_file),
    filename)

  if os.path.exists(passFilePath):
    try:
      os.remove(passFilePath)
    except Exception, e:
      print_warning_msg('Unable to remove password file: ' + str(e))
      return 1
  pass
  return 0


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

def get_validated_db_schema(postgres_schema):
    return get_validated_string_input(
        "Postgres schema (" + postgres_schema + "): ",
        postgres_schema,
        "^[a-zA-Z0-9_\-]*$",
        "Invalid schema name.",
        False, allowEmpty=True
    )

def get_validated_service_name(service_name, index):
  return get_validated_string_input(
            ORACLE_DB_ID_TYPES[index] + " (" + service_name + "): ",
            service_name,
            ".*",
            "Invalid " + ORACLE_DB_ID_TYPES[index] + ".",
            False
            )


def read_password(passwordDefault=PG_DEFAULT_PASSWORD,
                  passwordPattern=PASSWORD_PATTERN,
                  passwordPrompt=None,
                  passwordDescr=None):
  # setup password
  if passwordPrompt is None:
    passwordPrompt = 'Password (' + passwordDefault + '): '

  if passwordDescr is None:
    passwordDescr = "Invalid characters in password. Use only alphanumeric or " \
                    "_ or - characters"

  password = get_validated_string_input(passwordPrompt, passwordDefault,
                                        passwordPattern, passwordDescr, True)

  if not password:
    print 'Password cannot be blank.'
    return read_password(passwordDefault, passwordPattern, passwordPrompt,
                   passwordDescr)

  if password != passwordDefault:
    password1 = get_validated_string_input("Re-enter password: ",
                                           passwordDefault, passwordPattern, passwordDescr, True)
    if password != password1:
      print "Passwords do not match"
      return read_password(passwordDefault, passwordPattern, passwordPrompt,
                      passwordDescr)

  return password


def get_pass_file_path(conf_file):
  return os.path.join(os.path.dirname(conf_file),
                      JDBC_PASSWORD_FILENAME)


# Set database properties to default values
def load_default_db_properties(args):
  args.persistence_type = 'local'
  args.dbms = DATABASE_TYPES[DATABASE_INDEX]
  args.database_host = "localhost"
  args.database_port = DATABASE_PORTS[DATABASE_INDEX]
  args.database_name = DEFAULT_DB_NAME
  args.postgres_schema = DEFAULT_DB_NAME
  args.database_username = "ambari"
  args.database_password = "bigdata"
  args.sid_or_sname = "sname"
  pass


# Ask user for database conenction properties
def prompt_db_properties(args):
  global DATABASE_INDEX

  if PROMPT_DATABASE_OPTIONS:
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
      args.dbms = DATABASE_TYPES[args.database_index]

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

          if args.dbms in ["postgres", ]:
              args.postgres_schema = get_validated_db_schema(args.postgres_schema)
        else:
          # other DB types
          pass
        pass
      else:
        args.database_host = "localhost"
        args.database_port = DATABASE_PORTS[DATABASE_INDEX]

        args.database_name = get_validated_db_name(args.database_name)
        if args.dbms in ["postgres", ]:
            args.postgres_schema = get_validated_db_schema(args.postgres_schema)

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

  print_info_msg('Using database options: {database},{host},{port},{name},{schema},{user},{password}'.format(
    database=args.dbms,
    host=args.database_host,
    port=args.database_port,
    name=args.database_name,
    schema=args.postgres_schema,
    user=args.database_username,
    password=args.database_password
  ))

# extract the system views
def extract_views():
  jdk_path = find_jdk()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + JDK_INSTALL_DIR)
    return 1

  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  if not VIEWS_DIR_PROPERTY in properties.keys():
    vdir = DEFAULT_VIEWS_DIR
  else:
    vdir = properties.get_property(VIEWS_DIR_PROPERTY)

  files = [f for f in os.listdir(vdir) if os.path.isfile(os.path.join(vdir,f))]
  for f in files:

    command = VIEW_EXTRACT_CMD.format(jdk_path, get_conf_dir(),
      get_ambari_classpath(), os.path.join(vdir,f))

    retcode, stdout, stderr = run_os_command(command)
    if retcode == 0:
      sys.stdout.write(f + "\n")
    elif retcode == 2:
      sys.stdout.write("Error extracting " + f + "\n")
    else:
      sys.stdout.write(".")
      sys.stdout.flush()

    print_info_msg("Return code from extraction of view archive " + f + ": " +
                   str(retcode))

  sys.stdout.write("\n")
  return 0

# Store set of properties for remote database connection
def store_remote_properties(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  isSecure = get_is_secure(properties)

  properties.process_pair(PERSISTENCE_TYPE_PROPERTY, "remote")

  properties.process_pair(JDBC_DATABASE_PROPERTY, args.dbms)
  properties.process_pair(JDBC_HOSTNAME_PROPERTY, args.database_host)
  properties.process_pair(JDBC_PORT_PROPERTY, args.database_port)
  properties.process_pair(JDBC_DATABASE_NAME_PROPERTY, args.database_name)
  if args.dbms == "postgres":
    properties.process_pair(JDBC_POSTGRES_SCHEMA_PROPERTY, args.postgres_schema)
  properties.process_pair(JDBC_DRIVER_PROPERTY, DATABASE_DRIVER_NAMES[DATABASE_INDEX])
  # fully qualify the hostname to make sure all the other hosts can connect
  # to the jdbc hostname since its passed onto the agents for RCA
  jdbc_hostname = args.database_host
  if (args.database_host == "localhost"):
    jdbc_hostname = socket.getfqdn()

  connectionStringFormat = DATABASE_CONNECTION_STRINGS
  if args.sid_or_sname == "sid":
    connectionStringFormat = DATABASE_CONNECTION_STRINGS_ALT
  properties.process_pair(JDBC_URL_PROPERTY, connectionStringFormat[DATABASE_INDEX].format(jdbc_hostname, args.database_port, args.database_name))
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
  properties.process_pair(JDBC_RCA_URL_PROPERTY, connectionStringFormat[DATABASE_INDEX].format(jdbc_hostname, args.database_port, args.database_name))
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


# Initialize remote database schema
def setup_remote_db(args):

  setup_msg = "Before starting Ambari Server, you must run the following DDL " \
              "against the database to create the schema: {0}".format(DATABASE_INIT_SCRIPTS[DATABASE_INDEX])

  print_warning_msg(setup_msg)

  proceed = get_YN_input("Proceed with configuring remote database connection properties [y/n] (y)? ", True)
  retCode = 0 if proceed else -1

  return retCode


# Get database client executable path
def get_db_cli_tool(args):
  for tool in DATABASE_CLI_TOOLS[DATABASE_INDEX]:
    cmd = CHECK_COMMAND_EXIST_CMD.format(tool)
    ret, out, err = run_in_shell(cmd)
    if ret == 0:
      return get_exec_path(tool)

  return None

#execute SQL script on remote database: Deprecated
def execute_remote_script(args, scriptPath):
  print_warning_msg("Deprecated method called.")
  tool = get_db_cli_tool(args)
  if not tool:
    # args.warnings.append('{0} not found. Please, run DDL script manually'.format(DATABASE_CLI_TOOLS[DATABASE_INDEX]))
    if VERBOSE:
      print_warning_msg('{0} not found'.format(DATABASE_CLI_TOOLS[DATABASE_INDEX]))
    return -1, "Client wasn't found", "Client wasn't found"
  CMD = get_remote_script_line(args, scriptPath, False)
  if CMD:
    retcode, out, err = run_in_shell(CMD)
    return retcode, out, err
  else:
    return -2, "Wrong database", "Wrong database"


def get_remote_script_line(args, scriptPath, forPrint=True):
  tool = get_db_cli_tool(args)
  if not tool:
    # args.warnings.append('{0} not found. Please, run DDL script manually'.format(DATABASE_CLI_TOOLS[DATABASE_INDEX]))
    if VERBOSE or args.persistence_type == "remote":
      print_warning_msg('{0} not found'.format(DATABASE_CLI_TOOLS[DATABASE_INDEX]))
    return None
  if args.dbms == "postgres":
    os.environ["PGPASSWORD"] = args.database_password
    return '{0} {1}'.format(tool, POSTGRES_EXEC_ARGS.format(
      args.database_host,
      args.database_port,
      args.database_name,
      args.database_username,
      scriptPath
    ))
  elif args.dbms == "oracle":
    sid_or_sname = "sid"
    if (hasattr(args, 'sid_or_sname') and args.sid_or_sname == "sname") or \
        (hasattr(args, 'jdbc_url') and args.jdbc_url and re.match(ORACLE_SNAME_PATTERN, args.jdbc_url)):
      print_info_msg("using SERVICE_NAME instead of SID for Oracle")
      sid_or_sname = "service_name"

    return '{0} {1}'.format(tool, ORACLE_EXEC_ARGS.format(
      args.database_username,
      args.database_password if not forPrint else BLIND_PASSWORD,
      args.database_host,
      args.database_port,
      args.database_name,
      scriptPath,
      sid_or_sname
    ))
  elif args.dbms == "mysql":
    MYSQL_EXEC_ARGS = MYSQL_EXEC_ARGS_WO_USER_VARS if MYSQL_INIT_SCRIPT == scriptPath else MYSQL_EXEC_ARGS_WITH_USER_VARS
    return '{0} {1}'.format(tool, MYSQL_EXEC_ARGS.format(
      args.database_host,
      args.database_port,
      args.database_username,
      args.database_password if not forPrint else BLIND_PASSWORD,
      args.database_name,
      scriptPath
    ))

  return None


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


def get_ambari_version(properties):
  """
  :param properties: Ambari properties
  :return: Return a string of the ambari version. When comparing versions, please use "compare_versions" function.
  """
  version = None
  try:
    server_version_file_path = properties[SERVER_VERSION_FILE_PATH]
    if server_version_file_path and os.path.exists(server_version_file_path):
      with open(server_version_file_path, 'r') as file:
        version = file.read().strip()
  except:
    print_error_msg("Error getting ambari version")
  return version


def get_db_type(properties):
  db_type = None
  if properties[JDBC_URL_PROPERTY]:
    jdbc_url = properties[JDBC_URL_PROPERTY].lower()
    if "postgres" in jdbc_url:
      db_type = "postgres"
    elif "oracle" in jdbc_url:
      db_type = "oracle"
    elif "mysql" in jdbc_url:
      db_type = "mysql"
    elif "derby" in jdbc_url:
      db_type = "derby"

  return db_type


def check_database_name_property(args, upgrade=False):
  """
  :param upgrade: If Ambari is being upgraded.
  :return:
  """
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  version = get_ambari_version(properties)
  if upgrade and compare_versions(version, "1.7.0") >= 0:

    # This code exists for historic reasons in which property names changed from Ambari 1.6.1 to 1.7.0
    persistence_type = properties[PERSISTENCE_TYPE_PROPERTY]
    if persistence_type == "remote":
      db_name = properties["server.jdbc.schema"]  # this was a property in Ambari 1.6.1, but not after 1.7.0
      if db_name:
        write_property(JDBC_DATABASE_NAME_PROPERTY, db_name)

      # If DB type is missing, attempt to reconstruct it from the JDBC URL
      db_type = properties[JDBC_DATABASE_PROPERTY]
      if db_type is None or db_type.strip().lower() not in ["postgres", "oracle", "mysql", "derby"]:
        db_type = get_db_type(properties)
        if db_type:
          write_property(JDBC_DATABASE_PROPERTY, db_type)

      properties = get_ambari_properties()
    elif persistence_type == "local":
      # Ambari 1.6.1, had "server.jdbc.database" as the DB name, and the
      # DB type was assumed to be "postgres" if was embedded ("local")
      db_name = properties[JDBC_DATABASE_PROPERTY]
      if db_name:
        write_property(JDBC_DATABASE_NAME_PROPERTY, db_name)
        write_property(JDBC_DATABASE_PROPERTY, "postgres")
        properties = get_ambari_properties()

  dbname = properties[JDBC_DATABASE_NAME_PROPERTY]
  if dbname is None or dbname == "":
    err = "DB Name property not set in config file.\n" + SETUP_OR_UPGRADE_MSG
    raise FatalException(-1, err)


def configure_database_username_password(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  username = properties[JDBC_USER_NAME_PROPERTY]
  passwordProp = properties[JDBC_PASSWORD_PROPERTY]
  dbname = properties[JDBC_DATABASE_NAME_PROPERTY]

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


# Store local database connection properties
def store_local_properties(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  isSecure = get_is_secure(properties)

  properties.removeOldProp(JDBC_DATABASE_PROPERTY)
  properties.removeOldProp(JDBC_DATABASE_NAME_PROPERTY)
  properties.removeOldProp(JDBC_POSTGRES_SCHEMA_PROPERTY)
  properties.removeOldProp(JDBC_HOSTNAME_PROPERTY)
  properties.removeOldProp(JDBC_RCA_DRIVER_PROPERTY)
  properties.removeOldProp(JDBC_RCA_URL_PROPERTY)
  properties.removeOldProp(JDBC_PORT_PROPERTY)
  properties.removeOldProp(JDBC_DRIVER_PROPERTY)
  properties.removeOldProp(JDBC_URL_PROPERTY)

  # Store the properties
  properties.process_pair(PERSISTENCE_TYPE_PROPERTY, "local")
  properties.process_pair(JDBC_DATABASE_PROPERTY, args.dbms)
  properties.process_pair(JDBC_DATABASE_NAME_PROPERTY, args.database_name)
  if args.dbms == "postgres":
    properties.process_pair(JDBC_POSTGRES_SCHEMA_PROPERTY, args.postgres_schema)
  properties.process_pair(JDBC_USER_NAME_PROPERTY, args.database_username)
  properties.process_pair(JDBC_PASSWORD_PROPERTY,
      store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))

  if isSecure:
    encrypted_password = encrypt_password(JDBC_RCA_PASSWORD_ALIAS, args.database_password)
    if args.database_password != encrypted_password:
      properties.process_pair(JDBC_PASSWORD_PROPERTY, encrypted_password)
    pass
  pass

  conf_file = properties.fileName

  try:
    properties.store(open(conf_file, "w"))
  except Exception, e:
    print 'Unable to write ambari.properties configuration file "%s": %s' % (conf_file, e)
    return -1
  return 0


# Load ambari properties and return dict with values
def get_ambari_properties():
  conf_file = find_properties_file()

  properties = None
  try:
    properties = Properties()
    properties.load(open(conf_file))
  except (Exception), e:
    print 'Could not read "%s": %s' % (conf_file, e)
    return -1
  return properties


# Load database connection properties from conf file
def parse_properties_file(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  args.server_version_file_path = properties[SERVER_VERSION_FILE_PATH]
  args.persistence_type = properties[PERSISTENCE_TYPE_PROPERTY]
  args.jdbc_url = properties[JDBC_URL_PROPERTY]

  args.dbms = properties[JDBC_DATABASE_PROPERTY]
  if not args.persistence_type:
    args.persistence_type = "local"

  if args.persistence_type == 'remote':
    args.database_host = properties[JDBC_HOSTNAME_PROPERTY]
    args.database_port = properties[JDBC_PORT_PROPERTY]
    global DATABASE_INDEX
    try:
      DATABASE_INDEX = DATABASE_TYPES.index(args.dbms)
    except ValueError:
      pass

  args.database_name = properties[JDBC_DATABASE_NAME_PROPERTY]
  args.postgres_schema = properties[JDBC_POSTGRES_SCHEMA_PROPERTY] \
      if JDBC_POSTGRES_SCHEMA_PROPERTY in properties.propertyNames() else None
  args.database_username = properties[JDBC_USER_NAME_PROPERTY]
  args.database_password_file = properties[JDBC_PASSWORD_PROPERTY]
  if args.database_password_file:
    if not is_alias_string(args.database_password_file):
      args.database_password = open(properties[JDBC_PASSWORD_PROPERTY]).read()
    else:
      args.database_password = args.database_password_file

  return 0


### JDK ###


def get_ambari_jars():
  try:
    conf_dir = os.environ[AMBARI_SERVER_LIB]
    return conf_dir
  except KeyError:
    default_jar_location = "/usr/lib/ambari-server"
    print_info_msg(AMBARI_SERVER_LIB + " is not set, using default "
                 + default_jar_location)
    return default_jar_location


def get_share_jars():
  share_jars = ""
  file_list = []
  file_list.extend(glob.glob(JAVA_SHARE_PATH + os.sep + "*mysql*"))
  file_list.extend(glob.glob(JAVA_SHARE_PATH + os.sep + "*ojdbc*"))
  if len(file_list) > 0:
    share_jars = string.join(file_list, os.pathsep)
  return share_jars


def get_ambari_classpath():
  ambari_cp = get_ambari_jars() + os.sep + "*"
  share_cp = get_share_jars()
  if len(share_cp) > 0:
    ambari_cp = ambari_cp + os.pathsep + share_cp
  return ambari_cp


def search_file(filename, search_path, pathsep=os.pathsep):
  """ Given a search path, find file with requested name """
  for path in string.split(search_path, pathsep):
    candidate = os.path.join(path, filename)
    if os.path.exists(candidate):
      return os.path.abspath(candidate)
  return None


def dlprogress(base_name, count, blockSize, totalSize):
  percent = int(count * blockSize * 100 / totalSize)

  if (totalSize < blockSize):
    sys.stdout.write("\r" + base_name + "... %d%%" % (100))
  else:
    sys.stdout.write("\r" + base_name + "... %d%% (%.1f MB of %.1f MB)" % (
      percent, count * blockSize / 1024 / 1024.0, totalSize / 1024 / 1024.0))

  if (percent == 100 or totalSize < blockSize):
    sys.stdout.write("\n")
  sys.stdout.flush()


def track_jdk(base_name, url, local_name):
  u = urllib2.urlopen(url)
  h = u.info()
  totalSize = int(h["Content-Length"])
  fp = open(local_name, "wb")
  blockSize = 8192
  count = 0
  while True:
    chunk = u.read(blockSize)
    if not chunk:
      break
    fp.write(chunk)
    count += 1

    dlprogress(base_name, count, blockSize, totalSize)

  fp.flush()
  fp.close()


def install_jce_manualy(args):
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    raise FatalException(-1, err)
  if args.jce_policy:
    if os.path.exists(args.jce_policy):
      if os.path.isdir(args.jce_policy):
        err = "JCE Policy path is a directory: " + args.jce_policy
        raise FatalException(-1, err)
      jce_destination = os.path.join(properties[RESOURCES_DIR_PROPERTY], JCE_POLICY_FILENAMES[JDK_INDEX])
      try:
        shutil.copy(args.jce_policy, jce_destination)
      except Exception, e:
        err = "Can not copy file {0} to {1} due to: {2}. Please check file " \
              "permissions and free disk space.".format(args.jce_policy, jce_destination, e)
        raise FatalException(-1, err)
      print "JCE policy copied from " + args.jce_policy + " to " + jce_destination
      return 0
    else:
      err = "JCE Policy path " + args.jce_policy + " doesn't exists."
      raise FatalException(-1, err)
  else:
    return 1


#
# Downloads the JDK
#
def download_jdk(args):
  global JDK_INDEX
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    raise FatalException(-1, err)
  conf_file = properties.fileName
  ok = False
  jcePolicyWarn = "JCE Policy files are required for configuring Kerberos security. If you plan to use Kerberos," \
         "please make sure JCE Unlimited Strength Jurisdiction Policy Files are valid on all hosts."
  if args.java_home:
    if not os.path.exists(args.java_home) or not os.path.isfile(os.path.join(args.java_home, "bin", "java")):
      err = "Path to java home " + args.java_home + " or java binary file does not exists"
      raise FatalException(1, err)

    print_warning_msg("JAVA_HOME " + args.java_home + " must be valid on ALL hosts")
    print_warning_msg(jcePolicyWarn)
    write_property(JAVA_HOME_PROPERTY, args.java_home)
    remove_property(JDK_NAME_PROPERTY)
    remove_property(JCE_NAME_PROPERTY)
    return 0
  else:
    if get_JAVA_HOME():
      change_jdk = get_YN_input("Do you want to change Oracle JDK [y/n] (n)? ", False)
      if not change_jdk:
        return 0

    try:
      resources_dir = properties[RESOURCES_DIR_PROPERTY]
    except (KeyError), e:
      err = 'Property ' + str(e) + ' is not defined at ' + conf_file
      raise FatalException(1, err)
    jdk_num = str(JDK_INDEX + 1)
    jdk_num = get_validated_string_input(
"""[1] - Oracle JDK 1.7 + Java Cryptography Extension (JCE) Policy Files 7
[2] - Oracle JDK 1.6 + Java Cryptography Extension (JCE) Policy Files 6
[3] - Custom JDK
==============================================================================
Enter choice (""" + jdk_num + "):",
      jdk_num,
      "^[123]$",
      "Invalid number.",
      False
    )

    if jdk_num == CUSTOM_JDK_NUMBER:
      print_warning_msg("JDK must be installed on all hosts and JAVA_HOME must be valid on all hosts.")
      print_warning_msg(jcePolicyWarn)
      args.java_home = get_validated_string_input("Path to JAVA_HOME: ", None, None, None, False, False)
      if not os.path.exists(args.java_home) or not os.path.isfile(os.path.join(args.java_home, "bin", "java")):
        err = "Java home path or java binary file is unavailable. Please put correct path to java home."
        raise FatalException(1, err)
      print "Validating JDK on Ambari Server...done."
      write_property(JAVA_HOME_PROPERTY, args.java_home)
      remove_property(JDK_NAME_PROPERTY)
      remove_property(JCE_NAME_PROPERTY)
      return 0

    JDK_INDEX = int(jdk_num) - 1
    JDK_FILENAME = JDK_NAMES[JDK_INDEX]
    JDK_URL_PROPERTY = JDK_URL_PROPERTIES[JDK_INDEX]

    try:
      jdk_url = properties[JDK_URL_PROPERTY]
    except (KeyError), e:
      err = 'Property ' + str(e) + ' is not defined at ' + conf_file
      raise FatalException(1, err)
    dest_file = resources_dir + os.sep + JDK_FILENAME
    if os.path.exists(dest_file):
      print "JDK already exists, using " + dest_file
    else:
      ok = get_YN_input("To download the Oracle JDK and the Java Cryptography Extension (JCE) "
                        "Policy Files you must accept the "
                        "license terms found at "
                        "http://www.oracle.com/technetwork/java/javase/"
                        "terms/license/index.html and not accepting will "
                        "cancel the Ambari Server setup and you must install the JDK and JCE "
                        "files manually.\nDo you accept the "
                        "Oracle Binary Code License Agreement [y/n] (y)? ", True)
      if not ok:
        print 'Exiting...'
        sys.exit(1)

      print 'Downloading JDK from ' + jdk_url + ' to ' + dest_file
      jdk_download_fail_msg = " Failed to download JDK: {0}. Please check that Oracle " \
        "JDK is available at {1}. Also you may specify JDK file " \
        "location in local filesystem using --jdk-location command " \
        "line argument.".format("{0}", jdk_url)
      try:
        size_command = JDK_DOWNLOAD_SIZE_CMD.format(jdk_url)
        #Get Header from url,to get file size then
        retcode, out, err = run_os_command(size_command)
        if out.find("Content-Length") == -1:
          err = jdk_download_fail_msg.format("Request header doesn't contain Content-Length")
          raise FatalException(1, err)
        start_with = int(out.find("Content-Length") + len("Content-Length") + 2)
        end_with = out.find("\r\n", start_with)
        src_size = int(out[start_with:end_with])
        print 'JDK distribution size is ' + str(src_size) + ' bytes'
        file_exists = os.path.isfile(dest_file)
        file_size = -1
        if file_exists:
          file_size = os.stat(dest_file).st_size
        if file_exists and file_size == src_size:
          print_info_msg("File already exists")
        else:
          track_jdk(JDK_FILENAME, jdk_url, dest_file)
          print 'Successfully downloaded JDK distribution to ' + dest_file
      except FatalException:
        raise
      except Exception, e:
        err = jdk_download_fail_msg.format(str(e))
        raise FatalException(1, err)
      downloaded_size = os.stat(dest_file).st_size
      if downloaded_size != src_size or downloaded_size < JDK_MIN_FILESIZE:
        err = 'Size of downloaded JDK distribution file is ' \
                      + str(downloaded_size) + ' bytes, it is probably \
                      damaged or incomplete'
        raise FatalException(1, err)

    try:
       out = install_jdk(dest_file)
       jdk_version = re.search(JDK_VERSION_REs[JDK_INDEX], out).group(1)
       write_property(JDK_NAME_PROPERTY, JDK_FILENAME)
    except Exception, e:
       print "Installation of JDK has failed: %s\n" % e
       file_exists = os.path.isfile(dest_file)
       if file_exists:
          ok = get_YN_input("JDK found at "+dest_file+". "
                      "Would you like to re-download the JDK [y/n] (y)? ", True)
          if not ok:
             err = "Unable to install JDK. Please remove JDK file found at " + \
                   dest_file + " and re-run Ambari Server setup"
             raise FatalException(1, err)
          else:
             track_jdk(JDK_FILENAME, jdk_url, dest_file)
             print 'Successfully re-downloaded JDK distribution to ' + dest_file
             try:
                 out = install_jdk(dest_file)
                 jdk_version = re.search(JDK_VERSION_REs[JDK_INDEX], out).group(1)
                 write_property(JDK_NAME_PROPERTY, JDK_FILENAME)
             except Exception, e:
               print "Installation of JDK was failed: %s\n" % e
               err = "Unable to install JDK. Please remove JDK, file found at " + \
                     dest_file + " and re-run Ambari Server setup"
               raise FatalException(1, err)

       else:
           err = "Unable to install JDK. File " + dest_file + " does not exist, " \
                                        "please re-run Ambari Server setup"
           raise FatalException(1, err)

    print "Successfully installed JDK to {0}/{1}".\
        format(JDK_INSTALL_DIR, jdk_version)
    write_property(JAVA_HOME_PROPERTY, "{0}/{1}".
        format(JDK_INSTALL_DIR, jdk_version))

  try:
    download_jce_policy(properties, ok)
  except FatalException as e:
    print "JCE Policy files are required for secure HDP setup. Please ensure " \
            " all hosts have the JCE unlimited strength policy 6, files."
    print_error_msg("Failed to download JCE policy files:")
    if e.reason is not None:
      print_error_msg("\nREASON: {0}".format(e.reason))
    # TODO: We don't fail installation if download_jce_policy fails. Is it OK?
  return 0


def download_jce_policy(properties, accpeted_bcl):
  JCE_URL_PROPERTY = JCE_URL_PROPERTIES[JDK_INDEX]
  JCE_POLICY_FILENAME = JCE_POLICY_FILENAMES[JDK_INDEX]
  try:
    jce_url = properties[JCE_URL_PROPERTY]
    resources_dir = properties[RESOURCES_DIR_PROPERTY]
  except KeyError, e:
    err = 'Property ' + str(e) + ' is not defined in properties file'
    raise FatalException(1, err)
  dest_file = resources_dir + os.sep + JCE_POLICY_FILENAME
  if not os.path.exists(dest_file):
    print 'Downloading JCE Policy archive from ' + jce_url + ' to ' + dest_file
    jce_download_fail_msg = " Failed to download JCE Policy archive : {0}. " \
        "Please check that JCE Policy archive is available at {1} . "
    try:
      size_command = JDK_DOWNLOAD_SIZE_CMD.format(jce_url)
      #Get Header from url,to get file size then
      retcode, out, err = run_os_command(size_command)
      if out.find("Content-Length") == -1:
        err = jce_download_fail_msg.format(
            "Request header doesn't contain Content-Length")
        raise FatalException(1, err)
      start_with = int(out.find("Content-Length") + len("Content-Length") + 2)
      end_with = out.find("\r\n", start_with)
      src_size = int(out[start_with:end_with])
      print_info_msg('JCE zip distribution size is ' + str(src_size) + ' bytes')
      file_exists = os.path.isfile(dest_file)
      file_size = -1
      if file_exists:
        file_size = os.stat(dest_file).st_size
      if file_exists and file_size == src_size:
        print_info_msg("File already exists")
      else:
        #BCL license before download
        jce_download_cmd = JCE_DOWNLOAD_CMD.format(dest_file, jce_url)
        print_info_msg("JCE download cmd: " + jce_download_cmd)
        if accpeted_bcl:
          retcode, out, err = run_os_command(jce_download_cmd)
          if retcode == 0:
            write_property(JCE_NAME_PROPERTY, JCE_POLICY_FILENAME)
            print 'Successfully downloaded JCE Policy archive to ' + dest_file
          else:
            raise FatalException(1, err)
        else:
          ok = get_YN_input("To download the JCE Policy files you must "
                            "accept the license terms found at "
                            "http://www.oracle.com/technetwork/java/javase"
                            "/terms/license/index.html"
                            "Not accepting will result in errors when "
                            "configuring Kerberos security. \nDo you accept the "
                            "Oracle Binary Code License Agreement [y/n] (y)? ", True)
          if ok:
            retcode, out, err = run_os_command(jce_download_cmd)
            if retcode == 0:
              write_property(JCE_NAME_PROPERTY, JCE_POLICY_FILENAME)
              print 'Successfully downloaded JCE Policy archive to ' + dest_file
          else:
            raise FatalException(1, None)
    except FatalException:
        raise
    except Exception, e:
      err = 'Failed to download JCE Policy archive: ' + str(e)
      raise FatalException(1, err)
    downloaded_size = os.stat(dest_file).st_size
    if downloaded_size != src_size or downloaded_size < JCE_MIN_FILESIZE:
      err = 'Size of downloaded JCE Policy archive is ' \
                      + str(downloaded_size) + ' bytes, it is probably \
                    damaged or incomplete'
      raise FatalException(1, err)
  else:
    write_property(JCE_NAME_PROPERTY, JCE_POLICY_FILENAME)
    print "JCE Policy archive already exists, using " + dest_file


class RetCodeException(Exception):
  pass


def install_jdk(dest_file):
  print "Installing JDK to {0}".format(JDK_INSTALL_DIR)
  retcode, out, err = run_os_command(CREATE_JDK_DIR_CMD)
  savedPath = os.getcwd()
  os.chdir(JDK_INSTALL_DIR)

  if dest_file.endswith(".bin"):
    retcode, out, err = run_os_command(MAKE_FILE_EXECUTABLE_CMD.format(dest_file))
    retcode, out, err = run_os_command(dest_file + ' -noregister')
  elif dest_file.endswith(".gz"):
    retcode, out, err = run_os_command(UNTAR_JDK_ARCHIVE.format(dest_file))
  else:
    err = "JDK installation failed.Unknown file mask."
    raise FatalException(1, err)

  os.chdir(savedPath)
  if retcode != 0:
    err = "Installation of JDK returned exit code %s" % retcode
    raise FatalException(retcode, err)
  return out


#
# Configures the OS settings in ambari properties.
#
def configure_os_settings():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1
  try:
    conf_os_type = properties[OS_TYPE_PROPERTY]
    if conf_os_type != '':
      print_info_msg("os_type already setting in properties file")
      return 0
  except (KeyError):
    print_error_msg("os_type is not set in properties file")

  os_system = platform.system()
  if os_system != 'Linux':
    print_error_msg("Non-Linux systems are not supported")
    return -1

  master_os_type = OS_FAMILY + OS_VERSION
  write_property(OS_TYPE_PROPERTY, master_os_type)
  return 0


def get_JAVA_HOME():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return None

  java_home = properties[JAVA_HOME_PROPERTY]

  if (not 0 == len(java_home)) and (os.path.exists(java_home)):
    return java_home

  return None


#
# Checks jdk path for correctness
#
def validate_jdk(jdk_path):
  if jdk_path:
    return os.path.exists(jdk_path) and os.path.exists(
      jdk_path + os.sep + 'bin' + os.sep + 'java')
  else:
    return False


#
# Finds the available JDKs.
#
def find_jdk():
  jdkPath = get_JAVA_HOME()
  if jdkPath:
    if validate_jdk(jdkPath):
      return jdkPath
  print "Looking for available JDKs at " + JDK_INSTALL_DIR
  jdks = glob.glob(JDK_INSTALL_DIR + os.sep + "jdk*")
  jdks.sort()
  print "Found: " + str(jdks)
  if len(jdks) == 0:
    return
  for jdkPath in jdks:
    print "Trying to use JDK {0}".format(jdkPath)
    if validate_jdk(jdkPath):
      print "Selected JDK {0}".format(jdkPath)
      return jdkPath
    else:
      print "JDK {0} is invalid".format(jdkPath)
  return


#
# Checks if options determine local DB configuration
#
def is_local_database(args):
  return hasattr(args, 'persistence_type') and args.persistence_type == 'local'


#Check if required jdbc drivers present
def find_jdbc_driver(args):
  if args.dbms in JDBC_PATTERNS.keys():
    drivers = []
    drivers.extend(glob.glob(JAVA_SHARE_PATH + os.sep + JDBC_PATTERNS[args.dbms]))
    if drivers:
      return drivers
    return -1
  return 0


def copy_file(src, dest_file):
  try:
    shutil.copyfile(src, dest_file)
  except Exception, e:
    err = "Can not copy file {0} to {1} due to: {2} . Please check file " \
              "permissions and free disk space.".format(src, dest_file, e)
    raise FatalException(1, err)


def remove_file(filePath):
  if os.path.exists(filePath):
    try:
      os.remove(filePath)
    except Exception, e:
      print_warning_msg('Unable to remove file: ' + str(e))
      return 1
  pass
  return 0


def copy_files(files, dest_dir):
  if os.path.isdir(dest_dir):
    for filepath in files:
      shutil.copy(filepath, dest_dir)
    return 0
  else:
    return -1

#
# Wait for Popen process and returns exit code if process was finished in
# timeout seconds, otherwise returns None
#
def wait_popen(popen, timeout=0):
  begin = time.time()
  while popen.poll() is None and not time.time() - begin > timeout:
    time.sleep(1)
  return popen.poll()

def check_jdbc_drivers(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  result = find_jdbc_driver(args)

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
      result = find_jdbc_driver(args)
      if result == -1:
        print_error_msg(msg)
        raise FatalException(-1, msg)

  # Check if selected RDBMS requires drivers to copy
  if type(result) is not int:
    print 'Copying JDBC drivers to server resources...'
    try:
      resources_dir = properties[RESOURCES_DIR_PROPERTY]
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

  return 0


def verify_setup_allowed():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)
  if isSecure and not isPersisted and SILENT:
    print "ERROR: Cannot run silent 'setup' with password encryption enabled " \
          "and Master Key not persisted."
    print "Ambari Server 'setup' exiting."
    return 1
  return 0


#
# Setup the Ambari Server.
#
def setup(args):
  retcode = verify_setup_allowed()
  if not retcode == 0:
    raise FatalException(1, None)

  if not is_root():
    err = 'Ambari-server setup should be run with '\
                     'root-level privileges'
    raise FatalException(4, err)

  # proceed jdbc properties if they were set
  if args.jdbc_driver is not None and args.jdbc_db is not None:
    proceedJDBCProperties(args)
    return

  print 'Checking SELinux...'
  retcode = check_selinux()
  if not retcode == 0:
    err = 'Failed to disable SELinux. Exiting.'
    raise FatalException(retcode, err)

  # Create ambari user, if needed
  retcode = check_ambari_user()
  
  if not retcode == 0:
    err = 'Failed to create user. Exiting.'
    raise FatalException(retcode, err)

  print 'Checking firewall...'
  firewall_obj = Firewall().getFirewallObject()
  firewall_on = firewall_obj.check_iptables()
  if firewall_obj.stderrdata and len(firewall_obj.stderrdata) > 0:
    print firewall_obj.stderrdata
  if firewall_on:
    print_warning_msg("%s is running. Confirm the necessary Ambari ports are accessible. " %
                      firewall_obj.FIREWALL_SERVICE_NAME +
                      "Refer to the Ambari documentation for more details on ports.")
    ok = get_YN_input("OK to continue [y/n] (y)? ", True)
    if not ok:
      raise FatalException(1, None)




  # proceed jdbc properties if they were set
  if args.jdbc_driver is not None and args.jdbc_db is not None:
    proceedJDBCProperties(args)

  print 'Checking JDK...'
  try:
    download_jdk(args)
  except FatalException as e:
    err = 'Downloading or installing JDK failed: {0}. Exiting.'.format(e)
    raise FatalException(e.code, err)

  print 'Completing setup...'
  retcode = configure_os_settings()
  if not retcode == 0:
    err = 'Configure of OS settings in ambari.properties failed. Exiting.'
    raise FatalException(retcode, err)

  print 'Configuring database...'
  prompt_db_properties(args)

  #DB setup should be done last after doing any setup.

  if is_local_database(args):
    #check if jdbc user is changed
    is_user_changed = is_jdbc_user_changed(args)

    print 'Default properties detected. Using built-in database.'
    store_local_properties(args)

    print 'Checking PostgreSQL...'
    pg_status, retcode, out, err = check_postgre_up()
    if not retcode == 0:
      err = 'Unable to start PostgreSQL server. Status {0}. {1}.' \
            ' Exiting'.format(pg_status, err)
      raise FatalException(retcode, err)

    print 'Configuring local database...'
    retcode, outdata, errdata = setup_db(args)
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

  else:
    retcode = store_remote_properties(args)
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

  print 'Extracting system views...'
  retcode = extract_views()
  if not retcode == 0:
    err = 'Error while extracting system views. Exiting'
    raise FatalException(retcode, err)
  
  # we've already done this, but new files were created so run it one time.
  adjust_directory_permissions(read_ambari_user())


def proceedJDBCProperties(args):
  if not os.path.isfile(args.jdbc_driver):
    err = "File {0} does not exist!".format(args.jdbc_driver)
    raise FatalException(1, err)

  if args.jdbc_db not in JDBC_DB_OPTION_VALUES:
    err = "Unsupported database name {0}. Please see help for more information.".format(args.jdbc_db)
    raise FatalException(1, err)

  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    raise FatalException(-1, err)
  conf_file = properties.fileName

  try:
    resources_dir = properties[RESOURCES_DIR_PROPERTY]
  except (KeyError), e:
    err = 'Property ' + str(e) + ' is not defined at ' + conf_file
    raise FatalException(1, err)

  symlink_name = args.jdbc_db + "-jdbc-driver.jar"
  jdbc_symlink = os.path.join(resources_dir, symlink_name)
  path, jdbc_name = os.path.split(args.jdbc_driver)

  if os.path.lexists(jdbc_symlink):
    os.remove(jdbc_symlink)

  if not os.path.isfile(os.path.join(resources_dir, jdbc_name)):
    try:
      shutil.copy(args.jdbc_driver, resources_dir)
    except Exception, e:
      err = "Can not copy file {0} to {1} due to: {2} . Please check file " \
            "permissions and free disk space.".format(args.jdbc_driver, resources_dir, e)
      raise FatalException(1, err)

  os.symlink(os.path.join(resources_dir,jdbc_name), jdbc_symlink)
  print "JDBC driver was successfully initialized ."


#
# Resets the Ambari Server.
#
def reset(args):
  #force reset if silent option provided
  global SILENT
  if SILENT:
    default = "yes"
  else:
    default = "no"

  if not is_root():
    err = 'Ambari-server reset should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)
  status, pid = is_server_runing()
  if status:
    err = 'Ambari-server must be stopped to reset'
    raise FatalException(1, err)
  choice = get_YN_input("**** WARNING **** You are about to reset and clear the "
                     "Ambari Server database. This will remove all cluster "
                     "host and configuration information from the database. "
                     "You will be required to re-configure the Ambari server "
                     "and re-run the cluster wizard. \n"
                     "Are you SURE you want to perform the reset "
                     "[yes/no] ({0})? ".format(default), SILENT)
  okToRun = choice

  if not okToRun:
    err = "Ambari Server 'reset' cancelled"
    raise FatalException(1, err)

  check_database_name_property(args)
  parse_properties_file(args)

  if args.persistence_type == "remote":
    client_usage_cmd_drop = get_remote_script_line(args, DATABASE_DROP_SCRIPTS[DATABASE_INDEX])
    client_usage_cmd_init = get_remote_script_line(args, DATABASE_INIT_SCRIPTS[DATABASE_INDEX])
    if not client_usage_cmd_drop or not client_usage_cmd_init:
      raise NonFatalException("Could`t create command lines for {0} DB".format(args.dbms))

    print_warning_msg('To reset Ambari Server schema ' +
                      'you must run the following DDL against the database to '
                      + 'drop the schema:' + os.linesep + client_usage_cmd_drop
                      + os.linesep + 'Then you must run the following DDL ' +
                      'against the database to create the schema: ' + os.linesep +
                      client_usage_cmd_init + os.linesep)
    if args.dbms == "postgres":
      raise NonFatalException("Please set DB password to PGPASSWORD env variable before running DDL`s!")
    else:
      raise NonFatalException("Please replace '*' symbols with password before running DDL`s!")
  else:
    # Run automatic reset only for embedded DB
    okToRun = get_YN_input("Confirm server reset [yes/no]({0})? ".format(default), SILENT)

    if not okToRun:
      err = "Ambari Server 'reset' cancelled"
      raise FatalException(1, err)

    print "Resetting the Server database..."

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
    pass
  pass


#
# Starts the Ambari Server.
#
def start(args):
  if not check_reverse_lookup():
    print_warning_msg("The hostname was not found in the reverse DNS lookup. "
                      "This may result in incorrect behavior. "
                      "Please check the DNS setup and fix the issue.")
  current_user = getpass.getuser()
  ambari_user = read_ambari_user()
  if ambari_user is None:
    err = "Unable to detect a system user for Ambari Server.\n" + SETUP_OR_UPGRADE_MSG
    raise FatalException(1, err)
  if current_user != ambari_user and not is_root():
    err = "Unable to start Ambari Server as user {0}. Please either run \"ambari-server start\" " \
          "command as root, as sudo or as user \"{1}\"".format(current_user, ambari_user)
    raise FatalException(1, err)

  check_database_name_property(args)
  parse_properties_file(args)

  status, pid = is_server_runing()
  if status:
      err = "Ambari Server is already running."
      raise FatalException(1, err)

  print_info_msg("Ambari Server is not running...")

  conf_dir = get_conf_dir()
  jdk_path = find_jdk()
  if jdk_path is None:
    err = "No JDK found, please run the \"ambari-server setup\" " \
                    "command to install a JDK automatically or install any " \
                    "JDK manually to " + JDK_INSTALL_DIR
    raise FatalException(1, err)

  if args.persistence_type == 'remote':
    result = find_jdbc_driver(args)
    msg = 'Before starting Ambari Server, ' \
          'you must copy the {0} JDBC driver JAR file to {1}.'.format(
          DATABASE_FULL_NAMES[args.dbms],
          JAVA_SHARE_PATH)
    if result == -1:
      raise FatalException(-1, msg)

  # Preparations

  if is_root():
    print "Ambari Server running with 'root' privileges."

    if args.persistence_type == "local":
      pg_status, retcode, out, err = check_postgre_up()
      if not retcode == 0:
        err = 'Unable to start PostgreSQL server. Status {0}. {1}. Exiting'.format(pg_status, err)
        raise FatalException(retcode, err)

  else:  # Skipping actions that require root permissions
    print "Unable to check iptables status when starting "\
      "without root privileges."
    print "Please do not forget to disable or adjust iptables if needed"
    if args.persistence_type == "local":
      print "Unable to check PostgreSQL server status when starting " \
            "without root privileges."
      print "Please do not forget to start PostgreSQL server."

  refresh_stack_hash()

  properties = get_ambari_properties()

  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)
  environ = os.environ.copy()
  # Need to handle master key not persisted scenario
  if isSecure and not masterKeyFile:
    prompt = False
    masterKey = environ.get(SECURITY_KEY_ENV_VAR_NAME)

    if masterKey is not None and masterKey != "":
      pass
    else:
      keyLocation = environ.get(SECURITY_MASTER_KEY_LOCATION)

      if keyLocation is not None:
        try:
          # Verify master key can be read by the java process
          with open(keyLocation, 'r'):
            pass
        except IOError:
          print_warning_msg("Cannot read Master key from path specified in "
                            "environemnt.")
          prompt = True
      else:
        # Key not provided in the environment
        prompt = True

    if prompt:
      masterKey = get_original_master_key(properties)
      tempDir = tempfile.gettempdir()
      tempFilePath = tempDir + os.sep + "masterkey"
      save_master_key(masterKey, tempFilePath, True)
      if ambari_user != current_user:
        uid = pwd.getpwnam(ambari_user).pw_uid
        gid = pwd.getpwnam(ambari_user).pw_gid
        os.chown(tempFilePath, uid, gid)
      else:
        os.chmod(tempFilePath, stat.S_IREAD | stat.S_IWRITE)

      if tempFilePath is not None:
        environ[SECURITY_MASTER_KEY_LOCATION] = tempFilePath

  pidfile = PID_DIR + os.sep + PID_NAME
  command_base = SERVER_START_CMD_DEBUG if (SERVER_DEBUG_MODE or SERVER_START_DEBUG) else SERVER_START_CMD
  command = "%s %s; %s" % (ULIMIT_CMD, str(get_ulimit_open_files()),
                           command_base.format(jdk_path,
                                               conf_dir,
                                               get_ambari_classpath(),
                                               os.path.join(PID_DIR, EXITCODE_NAME))
                           )
  if not os.path.exists(PID_DIR):
    os.makedirs(PID_DIR, 0755)

  # required to start properly server instance
  os.chdir(ROOT_FS_PATH)

  #For properly daemonization server should be started using shell as parent
  if is_root() and ambari_user != "root":
    # To inherit exported environment variables (especially AMBARI_PASSPHRASE),
    # from subprocess, we have to skip --login option of su command. That's why
    # we change dir to / (otherwise subprocess can face with 'permission denied'
    # errors while trying to list current directory
    param_list = [utils.locate_file('su', '/bin'), ambari_user, "-s", utils.locate_file('sh', '/bin'), "-c", command]
  else:
    param_list = [utils.locate_file('sh', '/bin'), "-c", command]

  print_info_msg("Running server: " + str(param_list))
  subprocess.Popen(param_list, env=environ)

  print "Server PID at: "+pidfile
  print "Server out at: "+SERVER_OUT_FILE
  print "Server log at: "+SERVER_LOG_FILE

  #wait for server process for SERVER_START_TIMEOUT seconds
  sys.stdout.write('Waiting for server start...')
  sys.stdout.flush()

  pids = utils.looking_for_pid(SERVER_SEARCH_PATTERN, SERVER_INIT_TIMEOUT)
  found_pids = utils.wait_for_pid(pids, SERVER_START_TIMEOUT)

  sys.stdout.write('\n')
  sys.stdout.flush()

  if found_pids <= 0:
    exitcode = utils.check_exitcode(os.path.join(PID_DIR, EXITCODE_NAME))
    raise FatalException(-1, AMBARI_SERVER_DIE_MSG.format(exitcode, SERVER_OUT_FILE))
  else:
    utils.save_main_pid_ex(pids, pidfile, [utils.locate_file('sh', '/bin'),
                                 utils.locate_file('bash', '/bin')], True)


#
# Stops the Ambari Server.
#
def stop(args):
  if (args != None):
    args.exit_message = None

  status, pid = is_server_runing()

  if status:
    try:
      os.killpg(os.getpgid(pid), signal.SIGKILL)
    except OSError, e:
      print_info_msg("Unable to stop Ambari Server - " + str(e))
      return
    pid_file_path = PID_DIR + os.sep + PID_NAME
    os.remove(pid_file_path)
    print "Ambari Server stopped"
  else:
    print "Ambari Server is not running"


### Stack upgrade ###

def upgrade_stack(args, stack_id, repo_url=None, repo_url_os=None):
  if not is_root():
    err = 'Ambari-server upgradestack should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)
  check_database_name_property(args)

  stack_name, stack_version = stack_id.split(STACK_NAME_VER_SEP)
  retcode = run_stack_upgrade(stack_name, stack_version, repo_url, repo_url_os)

  if not retcode == 0:
    raise FatalException(retcode, 'Stack upgrade failed.')

  return retcode



def load_stack_values(version, filename):
  import xml.etree.ElementTree as ET
  values = {}
  root = ET.parse(filename).getroot()
  for ostag in root:
    ostype = ostag.attrib['type']
    for repotag in ostag:
      reponametag = repotag.find('reponame')
      repoidtag = repotag.find('repoid')
      baseurltag = repotag.find('baseurl')
      if reponametag is not None and repoidtag is not None and baseurltag is not None:
        key = "repo:/" + reponametag.text
        key += "/" + version
        key += "/" + ostype
        key += "/" + repoidtag.text
        key += ":baseurl"
        values[key] = baseurltag.text

  return values


def get_stack_location(properties):
  stack_location = properties[STACK_LOCATION_KEY]
  if stack_location is None:
    stack_location = STACK_LOCATION_DEFAULT
  return stack_location


def upgrade_local_repo(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  stack_location = get_stack_location(properties)
  stack_root_local = os.path.join(stack_location, "HDPLocal")
  if not os.path.exists(stack_root_local):
    print_info_msg("HDPLocal stack directory does not exist, skipping")
    return

  stack_root = os.path.join(stack_location, "HDP")
  if not os.path.exists(stack_root):
    print_info_msg("HDP stack directory does not exist, skipping")
    return

  for stack_version_local in os.listdir(stack_root_local):
    repo_file_local = os.path.join(stack_root_local, stack_version_local, "repos", "repoinfo.xml.rpmsave")
    if not os.path.exists(repo_file_local):
      repo_file_local = os.path.join(stack_root_local, stack_version_local, "repos", "repoinfo.xml")

    repo_file = os.path.join(stack_root, stack_version_local, "repos", "repoinfo.xml")

    print_info_msg("Local repo file: " + repo_file_local)
    print_info_msg("Repo file: " + repo_file_local)

    metainfo_update_items = {}

    if os.path.exists(repo_file_local) and os.path.exists(repo_file):
      local_values = load_stack_values(stack_version_local, repo_file_local)
      repo_values = load_stack_values(stack_version_local, repo_file)
      for k, v in local_values.iteritems():
        if repo_values.has_key(k):
          local_url = local_values[k]
          repo_url = repo_values[k]
          if repo_url != local_url:
            metainfo_update_items[k] = local_url

    run_metainfo_upgrade(metainfo_update_items)


def change_objects_owner(args):
  print 'Fixing database objects owner'
  database_name = args.database_name
  new_owner = args.database_username
  if '"' not in new_owner:
    #wrap to allow old username "ambari-server", postgres only
    new_owner = '\'"{0}"\''.format(new_owner)
    pass

  command = CHANGE_OWNER_COMMAND[:]
  command[-1] = command[-1].format(database_name, 'ambari', new_owner)
  retcode, stdout, stderr = run_os_command(command)
  if not retcode == 0:
    if VERBOSE:
      if stdout:
        print_error_msg(stdout.strip())
      if stderr:
        print_error_msg(stderr.strip())
    raise FatalException(20, 'Unable to change owner of database objects')


def compare_versions(version1, version2):
  def normalize(v):
    return [int(x) for x in re.sub(r'(\.0+)*$', '', v).split(".")]
  return cmp(normalize(version1), normalize(version2))
  pass


def run_schema_upgrade():
  jdk_path = find_jdk()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + JDK_INSTALL_DIR)
    return 1
  command = SCHEMA_UPGRADE_HELPER_CMD.format(jdk_path, get_conf_dir(), get_ambari_classpath())
  (retcode, stdout, stderr) = run_os_command(command)
  print_info_msg("Return code from schema upgrade command, retcode = " + str(retcode))
  if retcode > 0:
    print_error_msg("Error executing schema upgrade, please check the server logs.")
  return retcode


def run_stack_upgrade(stackName, stackVersion, repo_url, repo_url_os):
  jdk_path = find_jdk()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + JDK_INSTALL_DIR)
    return 1
  stackId = {}
  stackId[stackName] = stackVersion
  if repo_url is not None:
    stackId['repo_url'] = repo_url
  if repo_url_os is not None:
    stackId['repo_url_os'] = repo_url_os

  command = STACK_UPGRADE_HELPER_CMD.format(jdk_path, get_conf_dir(), get_ambari_classpath(),
                                             "updateStackId",
                                            "'" + json.dumps(stackId) + "'")
  (retcode, stdout, stderr) = run_os_command(command)
  print_info_msg("Return code from stack upgrade command, retcode = " + str(retcode))
  if retcode > 0:
    print_error_msg("Error executing stack upgrade, please check the server logs.")
  return retcode


def run_metainfo_upgrade(keyValueMap=None):
  jdk_path = find_jdk()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + JDK_INSTALL_DIR)

  retcode = 1
  if keyValueMap:
    command = STACK_UPGRADE_HELPER_CMD.format(jdk_path, get_conf_dir(), get_ambari_classpath(),
                                              'updateMetaInfo',
                                              "'" + json.dumps(keyValueMap) + "'")
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from stack upgrade command, retcode = " + str(retcode))
    if retcode > 0:
      print_error_msg("Error executing metainfo upgrade, please check the "
                      "server logs.")

  return retcode


#
# Upgrades the Ambari Server.
#
def upgrade(args):
  if not is_root():
    err = 'Ambari-server upgrade should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)

  print 'Updating properties in ' + AMBARI_PROPERTIES_FILE + ' ...'
  retcode = update_ambari_properties()
  if not retcode == 0:
    err = AMBARI_PROPERTIES_FILE + ' file can\'t be updated. Exiting'
    raise FatalException(retcode, err)

  try:
    check_database_name_property(args, upgrade=True)
  except FatalException:
    properties = get_ambari_properties()
    if properties == -1:
      print_error_msg("Error getting ambari properties")
      return -1
    print_warning_msg(JDBC_DATABASE_NAME_PROPERTY + " property isn't set in " +
    AMBARI_PROPERTIES_FILE + ". Setting it to default value - " + DEFAULT_DB_NAME)
    properties.process_pair(JDBC_DATABASE_NAME_PROPERTY, DEFAULT_DB_NAME)
    conf_file = find_properties_file()
    try:
      properties.store(open(conf_file, "w"))
    except Exception, e:
      print_error_msg('Could not write ambari config file "%s": %s' % (conf_file, e))
      return -1

  parse_properties_file(args)
  #TODO check database version
  if args.persistence_type == 'local':
    change_objects_owner(args)

  retcode = run_schema_upgrade()
  if not retcode == 0:
    print_error_msg("Ambari server upgrade failed. Please look at /var/log/ambari-server/ambari-server.log, for more details.")
    raise FatalException(11, 'Schema upgrade failed.')

  user = read_ambari_user()
  if user is None:
    warn = "Can not determine custom ambari user.\n" + SETUP_OR_UPGRADE_MSG
    print_warning_msg(warn)
  else:
    adjust_directory_permissions(user)

  # local repo
  upgrade_local_repo(args)

  # create jdbc symlinks if jdbc drivers are available in resources
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    print_error_msg(err)
    raise FatalException(-1, err)
  conf_file = properties.fileName

  try:
    resources_dir = properties[RESOURCES_DIR_PROPERTY]
  except (KeyError), e:
    err = 'Property ' + str(e) + ' is not defined at ' + conf_file
    raise FatalException(1, err)

  for db_name in list(JDBC_DB_DEFAULT_DRIVER):
    if os.path.isfile(os.path.join(resources_dir, JDBC_DB_DEFAULT_DRIVER[db_name])):
      symlink_name = db_name + "-jdbc-driver.jar"
      jdbc_symlink = os.path.join(resources_dir, symlink_name)
      if os.path.lexists(jdbc_symlink):
        os.remove(jdbc_symlink)
      os.symlink(os.path.join(resources_dir,JDBC_DB_DEFAULT_DRIVER[db_name]), jdbc_symlink)
  
  # check if ambari has obsolete LDAP configuration
  if properties.get_property(LDAP_PRIMARY_URL_PROPERTY) and not properties.get_property(IS_LDAP_CONFIGURED):
    args.warnings.append("Existing LDAP configuration is detected. You must run the \"ambari-server setup-ldap\" command to adjust existing LDAP configuration.")


#
# The Ambari Server status.
#
def status(args):
  args.exit_message = None
  status, pid = is_server_runing()
  if status:
    print "Ambari Server running"
    print "Found Ambari Server PID: " + str(pid) + " at: " + PID_DIR + os.sep + PID_NAME
  else:
    print "Ambari Server not running. Stale PID File at: " + PID_DIR + os.sep + PID_NAME


#
# Prints an "info" messsage.
#
def print_info_msg(msg):
  if VERBOSE:
    print("INFO: " + msg)


#
# Prints an "error" messsage.
#
def print_error_msg(msg):
  print("ERROR: " + msg)


#
# Prints a "warning" messsage.
#
def print_warning_msg(msg, bold=False):
  if bold:
    print(BOLD_ON + "WARNING: " + msg + BOLD_OFF)
  else:
    print("WARNING: " + msg)


#
# Gets the y/n input.
#
# return True if 'y' or False if 'n'
#
def get_YN_input(prompt, default):
  yes = set(['yes', 'ye', 'y'])
  no = set(['no', 'n'])
  return get_choice_string_input(prompt, default, yes, no)


def get_choice_string_input(prompt, default, firstChoice, secondChoice):
  if SILENT:
    print(prompt)
    return default
  choice = raw_input(prompt).lower()
  if choice in firstChoice:
    return True
  elif choice in secondChoice:
    return False
  elif choice is "":  # Just enter pressed
    return default
  else:
    print "input not recognized, please try again: "
    return get_choice_string_input(prompt, default, firstChoice, secondChoice)


def get_validated_string_input(prompt, default, pattern, description,
                               is_pass, allowEmpty=True, validatorFunction=None):

  input = ""
  while not input:
    if SILENT:
      print (prompt)
      input = default
    elif is_pass:
      input = getpass.getpass(prompt)
    else:
      input = raw_input(prompt)
    if not input.strip():
      # Empty input - if default available use default
      if not allowEmpty and not default:
        print 'Property cannot be blank.'
        input = ""
        continue
      else:
        input = default
        if validatorFunction:
          if not validatorFunction(input):
            input = ""
            continue
        break  # done here and picking up default
    else:
      if not pattern == None and not re.search(pattern, input.strip()):
        print description
        input = ""

      if validatorFunction:
        if not validatorFunction(input):
          input = ""
          continue
  return input


def get_value_from_properties(properties, key, default=""):
  try:
    value = properties.get_property(key)
    if not value:
      value = default
  except:
    return default
  return value


def get_prompt_default(defaultStr=None):
  if not defaultStr or defaultStr == "":
    return ""
  else:
    return '(' + defaultStr + ')'


#
# Sync users and groups with configured LDAP
#
def sync_ldap():
  if not is_root():
    err = 'Ambari-server sync-ldap should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)

  server_status, pid = is_server_runing()
  if not server_status:
    err = 'Ambari Server is not running.'
    raise FatalException(1, err)

  ldap_configured = get_ambari_properties().get_property(IS_LDAP_CONFIGURED)
  if ldap_configured != 'true':
    err = "LDAP is not configured. Run 'ambari-server setup-ldap' first."
    raise FatalException(1, err)

  if not LDAP_SYNC_ALL and not LDAP_SYNC_EXISTING and LDAP_SYNC_USERS is None and LDAP_SYNC_GROUPS is None:
    err = 'Must specify a sync option.  Please see help for more information.'
    raise FatalException(1, err)

  admin_login = get_validated_string_input(prompt="Enter Ambari Admin login: ", default=None,
                                           pattern=None, description=None,
                                           is_pass=False, allowEmpty=False)
  admin_password = get_validated_string_input(prompt="Enter Ambari Admin password: ", default=None,
                                              pattern=None, description=None,
                                              is_pass=True, allowEmpty=False)

  url = '{0}://{1}:{2!s}{3}'.format(SERVER_API_PROTOCOL, SERVER_API_HOST, SERVER_API_PORT, SERVER_API_LDAP_URL)
  admin_auth = base64.encodestring('%s:%s' % (admin_login, admin_password)).replace('\n', '')
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')

  if LDAP_SYNC_ALL:
    sys.stdout.write('Syncing all.')
    bodies = [{"Event":{"specs":[{"principal_type":"users","sync_type":"all"},{"principal_type":"groups","sync_type":"all"}]}}]
  elif LDAP_SYNC_EXISTING:
    sys.stdout.write('Syncing existing.')
    bodies = [{"Event":{"specs":[{"principal_type":"users","sync_type":"existing"},{"principal_type":"groups","sync_type":"existing"}]}}]
  else:
    sys.stdout.write('Syncing specified users and groups.')
    bodies = [{"Event":{"specs":[]}}]
    body = bodies[0]
    events = body['Event']
    specs = events['specs']

    if LDAP_SYNC_USERS is not None:
      new_specs = [{"principal_type":"users","sync_type":"specific","names":""}]
      get_ldap_event_spec_names(LDAP_SYNC_USERS, specs, new_specs)
    if LDAP_SYNC_GROUPS is not None:
      new_specs = [{"principal_type":"groups","sync_type":"specific","names":""}]
      get_ldap_event_spec_names(LDAP_SYNC_GROUPS, specs, new_specs)

  if VERBOSE:
    sys.stdout.write('\nCalling API ' + SERVER_API_LDAP_URL + ' : ' + str(bodies) + '\n')

  request.add_data(json.dumps(bodies))
  request.get_method = lambda: 'POST'

  try:
    response = urllib2.urlopen(request)
  except Exception as e:
    err = 'Sync event creation failed. Error details: %s' % e
    raise FatalException(1, err)

  response_status_code = response.getcode()
  if response_status_code != 201:
    err = 'Error during syncing. Http status code - ' + str(response_status_code)
    raise FatalException(1, err)
  response_body = json.loads(response.read())

  url = response_body['resources'][0]['href']
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')
  body = [{"LDAP":{"synced_groups":"*","synced_users":"*"}}]
  request.add_data(json.dumps(body))
  request.get_method = lambda: 'GET'
  request_in_progress = True

  while request_in_progress:

    sys.stdout.write('.')
    sys.stdout.flush()

    try:
      response = urllib2.urlopen(request)
    except Exception as e:
      request_in_progress = False
      err = 'Sync event check failed. Error details: %s' % e
      raise FatalException(1, err)

    response_status_code = response.getcode()
    if response_status_code != 200:
      err = 'Error during syncing. Http status code - ' + str(response_status_code)
      raise FatalException(1, err)
    response_body = json.loads(response.read())
    sync_info = response_body['Event']

    if sync_info['status'] == 'ERROR':
      raise FatalException(1, str(sync_info['status_detail']))
    elif sync_info['status'] == 'COMPLETE':
      print '\n\nCompleted LDAP Sync.'
      print 'Summary:'
      for principal_type, summary in sync_info['summary'].iteritems():
        print '  {0}:'.format(principal_type)
        for action, amount in summary.iteritems():
          print '    {0} = {1!s}'.format(action, amount)
      request_in_progress = False
    else:
      time.sleep(1)

  sys.stdout.write('\n')
  sys.stdout.flush()

#
# Get the principal names from the given CSV file and set them on the given LDAP event specs.
#
def get_ldap_event_spec_names(file, specs, new_specs):

  try:
    if os.path.exists(file):
      new_spec = new_specs[0]
      with open(file, 'r') as names_file:
        names = names_file.read()
        new_spec['names'] = ''.join(names.split())
        names_file.close()
        specs += new_specs
    else:
      err = 'Sync event creation failed. File ' + file + ' not found.'
      raise FatalException(1, err)
  except Exception as exception:
      err = 'Caught exception reading file ' + file + ' : ' + str(exception)
      raise FatalException(1, err)


def setup_ldap():
  if not is_root():
    err = 'Ambari-server setup-ldap should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)

  properties = get_ambari_properties()
  isSecure = get_is_secure(properties)
  # python2.x dict is not ordered
  ldap_property_list_reqd = [LDAP_PRIMARY_URL_PROPERTY,
                        "authentication.ldap.secondaryUrl",
                        "authentication.ldap.useSSL",
                        "authentication.ldap.userObjectClass",
                        "authentication.ldap.usernameAttribute",
                        "authentication.ldap.groupObjectClass",
                        "authentication.ldap.groupNamingAttr",
                        "authentication.ldap.groupMembershipAttr",
                        "authentication.ldap.baseDn",
                        "authentication.ldap.bindAnonymously"]

  ldap_property_list_opt = ["authentication.ldap.managerDn",
                             LDAP_MGR_PASSWORD_PROPERTY,
                             SSL_TRUSTSTORE_TYPE_PROPERTY,
                             SSL_TRUSTSTORE_PATH_PROPERTY,
                             SSL_TRUSTSTORE_PASSWORD_PROPERTY]

  ldap_property_list_truststore=[SSL_TRUSTSTORE_TYPE_PROPERTY,
                                 SSL_TRUSTSTORE_PATH_PROPERTY,
                                 SSL_TRUSTSTORE_PASSWORD_PROPERTY]

  ldap_property_list_passwords=[LDAP_MGR_PASSWORD_PROPERTY,
                                SSL_TRUSTSTORE_PASSWORD_PROPERTY]

  LDAP_PRIMARY_URL_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[0])
  LDAP_SECONDARY_URL_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[1])
  LDAP_USE_SSL_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[2], "false")
  LDAP_USER_CLASS_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[3], "posixAccount")
  LDAP_USER_ATT_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[4], "uid")
  LDAP_GROUP_CLASS_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[5], "posixGroup")
  LDAP_GROUP_ATT_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[6], "cn")
  LDAP_GROUP_MEMBER_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[7], "memberUid")
  LDAP_BASE_DN_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[8])
  LDAP_BIND_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[9], "false")
  LDAP_MGR_DN_DEFAULT = get_value_from_properties(properties, ldap_property_list_opt[0])
  SSL_TRUSTSTORE_TYPE_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_TYPE_PROPERTY, "jks")
  SSL_TRUSTSTORE_PATH_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_PATH_PROPERTY)


  ldap_properties_map_reqd =\
  {
    ldap_property_list_reqd[0]:(LDAP_PRIMARY_URL_DEFAULT, "Primary URL* {{host:port}} {0}: ".format(get_prompt_default(LDAP_PRIMARY_URL_DEFAULT)), False),\
    ldap_property_list_reqd[1]:(LDAP_SECONDARY_URL_DEFAULT, "Secondary URL {{host:port}} {0}: ".format(get_prompt_default(LDAP_SECONDARY_URL_DEFAULT)), True),\
    ldap_property_list_reqd[2]:(LDAP_USE_SSL_DEFAULT, "Use SSL* [true/false] {0}: ".format(get_prompt_default(LDAP_USE_SSL_DEFAULT)), False),\
    ldap_property_list_reqd[3]:(LDAP_USER_CLASS_DEFAULT, "User object class* {0}: ".format(get_prompt_default(LDAP_USER_CLASS_DEFAULT)), False),\
    ldap_property_list_reqd[4]:(LDAP_USER_ATT_DEFAULT, "User name attribute* {0}: ".format(get_prompt_default(LDAP_USER_ATT_DEFAULT)), False),\
    ldap_property_list_reqd[5]:(LDAP_GROUP_CLASS_DEFAULT, "Group object class* {0}: ".format(get_prompt_default(LDAP_GROUP_CLASS_DEFAULT)), False),\
    ldap_property_list_reqd[6]:(LDAP_GROUP_ATT_DEFAULT, "Group name attribute* {0}: ".format(get_prompt_default(LDAP_GROUP_ATT_DEFAULT)), False),\
    ldap_property_list_reqd[7]:(LDAP_GROUP_MEMBER_DEFAULT, "Group member attribute* {0}: ".format(get_prompt_default(LDAP_GROUP_MEMBER_DEFAULT)), False),\
    ldap_property_list_reqd[8]:(LDAP_BASE_DN_DEFAULT, "Base DN* {0}: ".format(get_prompt_default(LDAP_BASE_DN_DEFAULT)), False),\
    ldap_property_list_reqd[9]:(LDAP_BIND_DEFAULT, "Bind anonymously* [true/false] {0}: ".format(get_prompt_default(LDAP_BIND_DEFAULT)), False),\
  }

  ldap_property_value_map = {}
  for idx, key in enumerate(ldap_property_list_reqd):
    if idx in [0, 1]:
      pattern = REGEX_HOSTNAME_PORT
    elif idx in [2, 9]:
      pattern = REGEX_TRUE_FALSE
    else:
      pattern = REGEX_ANYTHING
    input = get_validated_string_input(ldap_properties_map_reqd[key][1],
      ldap_properties_map_reqd[key][0], pattern,
      "Invalid characters in the input!", False, ldap_properties_map_reqd[key][2])
    if input is not None and input != "":
      ldap_property_value_map[key] = input

  bindAnonymously = ldap_property_value_map["authentication.ldap.bindAnonymously"]
  anonymous = (bindAnonymously and bindAnonymously.lower() == 'true')
  mgr_password = None
  # Ask for manager credentials only if bindAnonymously is false
  if not anonymous:
    username = get_validated_string_input("Manager DN* {0}: ".format(
      get_prompt_default(LDAP_MGR_DN_DEFAULT)), LDAP_MGR_DN_DEFAULT, ".*",
                "Invalid characters in the input!", False, False)
    ldap_property_value_map[LDAP_MGR_USERNAME_PROPERTY] = username
    mgr_password = configure_ldap_password()
    ldap_property_value_map[LDAP_MGR_PASSWORD_PROPERTY] = mgr_password

  useSSL = ldap_property_value_map["authentication.ldap.useSSL"]
  ldaps = (useSSL and useSSL.lower() == 'true')
  ts_password = None

  if ldaps:
    truststore_default = "n"
    truststore_set = bool(SSL_TRUSTSTORE_PATH_DEFAULT)
    if truststore_set:
      truststore_default = "y"
    custom_trust_store = get_YN_input("Do you want to provide custom TrustStore for Ambari [y/n] ({0})?".
                                      format(truststore_default),
                                      truststore_set)
    if custom_trust_store:
      ts_type = get_validated_string_input(
        "TrustStore type [jks/jceks/pkcs12] {0}:".format(get_prompt_default(SSL_TRUSTSTORE_TYPE_DEFAULT)),
        SSL_TRUSTSTORE_TYPE_DEFAULT,
        "^(jks|jceks|pkcs12)?$", "Wrong type", False)
      ts_path = None
      while True:
        ts_path = get_validated_string_input(
          "Path to TrustStore file {0}:".format(get_prompt_default(SSL_TRUSTSTORE_PATH_DEFAULT)),
          SSL_TRUSTSTORE_PATH_DEFAULT,
          ".*", False, False)
        if os.path.exists(ts_path):
          break
        else:
          print 'File not found.'

      ts_password = read_password("", ".*", "Password for TrustStore:", "Invalid characters in password")

      ldap_property_value_map[SSL_TRUSTSTORE_TYPE_PROPERTY] = ts_type
      ldap_property_value_map[SSL_TRUSTSTORE_PATH_PROPERTY] = ts_path
      ldap_property_value_map[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = ts_password
      pass
    else:
      properties.removeOldProp(SSL_TRUSTSTORE_TYPE_PROPERTY)
      properties.removeOldProp(SSL_TRUSTSTORE_PATH_PROPERTY)
      properties.removeOldProp(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
    pass
  pass

  print '=' * 20
  print 'Review Settings'
  print '=' * 20
  for property in ldap_property_list_reqd:
    if property in ldap_property_value_map:
      print("%s: %s" % (property, ldap_property_value_map[property]))

  for property in ldap_property_list_opt:
    if ldap_property_value_map.has_key(property):
      if property not in ldap_property_list_passwords:
        print("%s: %s" % (property, ldap_property_value_map[property]))
      else:
        print("%s: %s" % (property, BLIND_PASSWORD))

  save_settings = get_YN_input("Save settings [y/n] (y)? ", True)

  if save_settings:
    ldap_property_value_map[CLIENT_SECURITY_KEY] = 'ldap'
    if isSecure:
      if mgr_password:
        encrypted_passwd = encrypt_password(LDAP_MGR_PASSWORD_ALIAS, mgr_password)
        if mgr_password != encrypted_passwd:
          ldap_property_value_map[LDAP_MGR_PASSWORD_PROPERTY] = encrypted_passwd
      pass
      if ts_password:
        encrypted_passwd = encrypt_password(SSL_TRUSTSTORE_PASSWORD_ALIAS, ts_password)
        if ts_password != encrypted_passwd:
          ldap_property_value_map[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = encrypted_passwd
      pass
    pass

    # Persisting values
    ldap_property_value_map[IS_LDAP_CONFIGURED] = "true"
    if mgr_password:
      ldap_property_value_map[LDAP_MGR_PASSWORD_PROPERTY] = store_password_file(mgr_password, LDAP_MGR_PASSWORD_FILENAME)
    update_properties(properties, ldap_property_value_map)
    print 'Saving...done'

  return 0


def read_master_key(isReset=False):
  passwordPattern = ".*"
  passwordPrompt = "Please provide master key for locking the credential store: "
  passwordDescr = "Invalid characters in password. Use only alphanumeric or "\
                  "_ or - characters"
  passwordDefault = ""
  if isReset:
    passwordPrompt = "Enter new Master Key: "

  masterKey = get_validated_string_input(passwordPrompt, passwordDefault,
                            passwordPattern, passwordDescr, True, True)

  if not masterKey:
    print "Master Key cannot be empty!"
    return read_master_key()

  masterKey2 = get_validated_string_input("Re-enter master key: ",
      passwordDefault, passwordPattern, passwordDescr, True, True)

  if masterKey != masterKey2:
    print "Master key did not match!"
    return read_master_key()

  return masterKey


def encrypt_password(alias, password):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, None)
  return get_encrypted_password(alias, password, properties)


def get_encrypted_password(alias, password, properties):
  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)
  if isSecure:
    masterKey = None
    if not masterKeyFile:
      # Encryption enabled but no master key file found
      masterKey = get_original_master_key(properties)

    retCode = save_passwd_for_alias(alias, password, masterKey)
    if retCode != 0:
      print 'Failed to save secure password!'
      return password
    else:
      return get_alias_string(alias)

  return password


def decrypt_password_for_alias(alias):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, None)

  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)
  if isSecure:
    masterKey = None
    if not masterKeyFile:
      # Encryption enabled but no master key file found
      masterKey = get_original_master_key(properties)

    return read_passwd_for_alias(alias, masterKey)
  else:
    return alias


def get_original_master_key(properties):
  try:
    masterKey = get_validated_string_input('Enter current Master Key: ',
                                             "", ".*", "", True, False)
  except KeyboardInterrupt:
    print 'Exiting...'
    sys.exit(1)

  # Find an alias that exists
  alias = None
  property = properties.get_property(JDBC_PASSWORD_PROPERTY)
  if property and is_alias_string(property):
    alias = JDBC_RCA_PASSWORD_ALIAS

  if not alias:
    property = properties.get_property(LDAP_MGR_PASSWORD_PROPERTY)
    if property and is_alias_string(property):
      alias = LDAP_MGR_PASSWORD_ALIAS

  if not alias:
    property = properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
    if property and is_alias_string(property):
      alias = SSL_TRUSTSTORE_PASSWORD_ALIAS

  # Decrypt alias with master to validate it, if no master return
  if alias and masterKey:
    password = read_passwd_for_alias(alias, masterKey)
    if not password:
      print "ERROR: Master key does not match."
      return get_original_master_key(properties)

  return masterKey


def get_is_secure(properties):
  isSecure = properties.get_property(SECURITY_IS_ENCRYPTION_ENABLED)
  isSecure = True if isSecure and isSecure.lower() == 'true' else False
  return isSecure


def get_is_persisted(properties):
  keyLocation = get_master_key_location(properties)
  masterKeyFile = search_file(SECURITY_MASTER_KEY_FILENAME, keyLocation)
  isPersisted = True if masterKeyFile else False

  return (isPersisted, masterKeyFile)


def setup_master_key():
  if not is_root():
    err = 'Ambari-server setup should be run with '\
                     'root-level privileges'
    raise FatalException(4, err)

  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, "Failed to read properties file.")

  db_password = properties.get_property(JDBC_PASSWORD_PROPERTY)
  # Encrypt passwords cannot be called before setup
  if not db_password:
    print 'Please call "setup" before "encrypt-passwords". Exiting...'
    return 1

  # Check configuration for location of master key
  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)

  # Read clear text DB password from file
  if not is_alias_string(db_password) and os.path.isfile(db_password):
    with open(db_password, 'r') as passwdfile:
      db_password = passwdfile.read()

  ldap_password = properties.get_property(LDAP_MGR_PASSWORD_PROPERTY)

  if ldap_password:
    # Read clear text LDAP password from file
    if not is_alias_string(ldap_password) and os.path.isfile(ldap_password):
      with open(ldap_password, 'r') as passwdfile:
        ldap_password = passwdfile.read()
  
  ts_password = properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
  resetKey = False
  masterKey = None

  if isSecure:
    print "Password encryption is enabled."
    resetKey = get_YN_input("Do you want to reset Master Key? [y/n] (n): ", False)

  # For encrypting of only unencrypted passwords without resetting the key ask
  # for master key if not persisted.
  if isSecure and not isPersisted and not resetKey:
    print "Master Key not persisted."
    masterKey = get_original_master_key(properties)
  pass

  # Make sure both passwords are clear-text if master key is lost
  if resetKey:
    if not isPersisted:
      print "Master Key not persisted."
      masterKey = get_original_master_key(properties)
      # Unable get the right master key or skipped question <enter>
      if not masterKey:
        print "To disable encryption, do the following:"
        print "- Edit " + find_properties_file() + \
              " and set " + SECURITY_IS_ENCRYPTION_ENABLED + " = " + "false."
        err = "{0} is already encrypted. Please call {1} to store unencrypted" \
              " password and call 'encrypt-passwords' again."
        if db_password and is_alias_string(db_password):
          print err.format('- Database password', "'" + SETUP_ACTION + "'")
        if ldap_password and is_alias_string(ldap_password):
          print err.format('- LDAP manager password', "'" + LDAP_SETUP_ACTION + "'")
        if ts_password and is_alias_string(ts_password):
          print err.format('TrustStore password', "'" + LDAP_SETUP_ACTION + "'")

        return 1
      pass
    pass
  pass

  # Read back any encrypted passwords
  if db_password and is_alias_string(db_password):
    db_password = read_passwd_for_alias(JDBC_RCA_PASSWORD_ALIAS, masterKey)
  if ldap_password and is_alias_string(ldap_password):
    ldap_password = read_passwd_for_alias(LDAP_MGR_PASSWORD_ALIAS, masterKey)
  if ts_password and is_alias_string(ts_password):
    ts_password = read_passwd_for_alias(SSL_TRUSTSTORE_PASSWORD_ALIAS, masterKey)
  # Read master key, if non-secure or reset is true
  if resetKey or not isSecure:
    masterKey = read_master_key(resetKey)
    persist = get_YN_input("Do you want to persist master key. If you choose "\
                           "not to persist, you need to provide the Master "\
                           "Key while starting the ambari server as an env "\
                           "variable named " + SECURITY_KEY_ENV_VAR_NAME +\
                           " or the start will prompt for the master key."
                           " Persist [y/n] (y)? ", True)
    if persist:
      save_master_key(masterKey, get_master_key_location(properties) + os.sep +
                                 SECURITY_MASTER_KEY_FILENAME, persist)
    elif not persist and masterKeyFile:
      try:
        os.remove(masterKeyFile)
        print_info_msg("Deleting master key file at location: " + str(
          masterKeyFile))
      except Exception, e:
        print 'ERROR: Could not remove master key file. %s' % e
    # Blow up the credential store made with previous key, if any
    store_file = get_credential_store_location(properties)
    if os.path.exists(store_file):
      try:
        os.remove(store_file)
      except:
        print_warning_msg("Failed to remove credential store file.")
      pass
    pass
  pass

  propertyMap = {SECURITY_IS_ENCRYPTION_ENABLED: 'true'}
  # Encrypt only un-encrypted passwords
  if db_password and not is_alias_string(db_password):
    retCode = save_passwd_for_alias(JDBC_RCA_PASSWORD_ALIAS, db_password, masterKey)
    if retCode != 0:
      print 'Failed to save secure database password.'
    else:
      propertyMap[JDBC_PASSWORD_PROPERTY] = get_alias_string(JDBC_RCA_PASSWORD_ALIAS)
      remove_password_file(JDBC_PASSWORD_FILENAME)
      if properties.get_property(JDBC_RCA_PASSWORD_FILE_PROPERTY):
        propertyMap[JDBC_RCA_PASSWORD_FILE_PROPERTY] = get_alias_string(JDBC_RCA_PASSWORD_ALIAS)
  pass

  if ldap_password and not is_alias_string(ldap_password):
    retCode = save_passwd_for_alias(LDAP_MGR_PASSWORD_ALIAS, ldap_password, masterKey)
    if retCode != 0:
      print 'Failed to save secure LDAP password.'
    else:
      propertyMap[LDAP_MGR_PASSWORD_PROPERTY] = get_alias_string(LDAP_MGR_PASSWORD_ALIAS)
      remove_password_file(LDAP_MGR_PASSWORD_FILENAME)
  pass

  if ts_password and not is_alias_string(ts_password):
    retCode = save_passwd_for_alias(SSL_TRUSTSTORE_PASSWORD_ALIAS, ts_password, masterKey)
    if retCode != 0:
      print 'Failed to save secure TrustStore password.'
    else:
      propertyMap[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = get_alias_string(SSL_TRUSTSTORE_PASSWORD_ALIAS)
  pass

  update_properties(properties, propertyMap)

  # Since files for store and master are created we need to ensure correct
  # permissions
  ambari_user = read_ambari_user()
  if ambari_user:
    adjust_directory_permissions(ambari_user)

  return 0


def get_credential_store_location(properties):
  store_loc = properties[SECURITY_KEYS_DIR]
  if store_loc is None or store_loc == "":
    store_loc = "/var/lib/ambari-server/keys/credentials.jceks"
  else:
    store_loc += os.sep + "credentials.jceks"
  return store_loc


def get_master_key_location(properties):
  keyLocation = properties[SECURITY_MASTER_KEY_LOCATION]
  if keyLocation is None or keyLocation == "":
    keyLocation = properties[SECURITY_KEYS_DIR]
  return keyLocation


def is_alias_string(passwdStr):
  regex = re.compile("\$\{alias=[\w\.]+\}")
  # Match implies string at beginning of word
  r = regex.match(passwdStr)
  if r is not None:
    return True
  else:
    return False


def get_alias_string(alias):
  return "${alias=" + alias + "}"


def get_alias_from_alias_string(aliasStr):
  return aliasStr[8:-1]


def read_passwd_for_alias(alias, masterKey=""):
  if alias:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to " + JDK_INSTALL_DIR)
      return 1

    tempFileName = "ambari.passwd"
    passwd = ""
    tempDir = tempfile.gettempdir()
    #create temporary file for writing
    tempFilePath = tempDir + os.sep + tempFileName
    file = open(tempFilePath, 'w+')
    os.chmod(tempFilePath, stat.S_IREAD | stat.S_IWRITE)
    file.close()

    if masterKey is None or masterKey == "":
      masterKey = "None"

    command = SECURITY_PROVIDER_GET_CMD.format(jdk_path,
      get_conf_dir(), get_ambari_classpath(), alias, tempFilePath, masterKey)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider get passwd: " +
                   str(retcode))
    if retcode != 0:
      print 'ERROR: Unable to read password from store. alias = ' + alias
    else:
      passwd = open(tempFilePath, 'r').read()
      # Remove temporary file
    os.remove(tempFilePath)
    return passwd
  else:
    print_error_msg("Alias is unreadable.")


def save_passwd_for_alias(alias, passwd, masterKey=""):
  if alias and passwd:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to " + JDK_INSTALL_DIR)
      return 1

    if masterKey is None or masterKey == "":
      masterKey = "None"

    command = SECURITY_PROVIDER_PUT_CMD.format(jdk_path, get_conf_dir(),
      get_ambari_classpath(), alias, passwd, masterKey)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider save passwd: " +
                   str(retcode))
    return retcode
  else:
    print_error_msg("Alias or password is unreadable.")


def save_master_key(master_key, key_location, persist=True):
  if master_key:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to " + JDK_INSTALL_DIR)
      return 1
    command = SECURITY_PROVIDER_KEY_CMD.format(jdk_path,
      get_ambari_classpath(), get_conf_dir(), master_key, key_location, persist)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider save KEY: " +
                   str(retcode))
  else:
    print_error_msg("Master key cannot be None.")


def configure_ldap_password():
  passwordDefault = ""
  passwordPrompt = 'Enter Manager Password* : '
  passwordPattern = ".*"
  passwordDescr = "Invalid characters in password."

  password = read_password(passwordDefault, passwordPattern, passwordPrompt,
    passwordDescr)

  return password


# Copy file to /tmp and save with file.# (largest # is latest file)
def backup_file_in_temp(filePath):
  if filePath is not None:
    tmpDir = tempfile.gettempdir()
    back_up_file_count = len(glob.glob1(tmpDir, AMBARI_PROPERTIES_FILE + "*"))
    try:
      shutil.copyfile(filePath, tmpDir + os.sep +
                                AMBARI_PROPERTIES_FILE + "." + str(back_up_file_count + 1))
    except (Exception), e:
      print_error_msg('Could not backup file in temp "%s": %s' % (str(
        back_up_file_count, e)))
  return 0


# update properties in a section-less properties file
# Cannot use ConfigParser due to bugs in version 2.6
def update_properties(propertyMap):
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  backup_file_in_temp(conf_file)
  if propertyMap is not None and conf_file is not None:
    properties = Properties()
    try:
      with open(conf_file, 'r') as file:
        properties.load(file)
    except (Exception), e:
      print_error_msg('Could not read "%s": %s' % (conf_file, e))
      return -1

    #for key in propertyMap.keys():
      #properties[key] = propertyMap[key]
    for key in propertyMap.keys():
      properties.removeOldProp(key)
      properties.process_pair(key, str(propertyMap[key]))

    with open(conf_file, 'w') as file:
      properties.store(file)

  return 0


def update_properties(properties, propertyMap):
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  backup_file_in_temp(conf_file)
  if conf_file is not None:
    if propertyMap is not None:
      for key in propertyMap.keys():
        properties.removeOldProp(key)
        properties.process_pair(key, str(propertyMap[key]))
      pass

    with open(conf_file, 'w') as file:
      properties.store(file)
    pass
  pass


def setup_https(args):
  if not is_root():
    err = 'ambari-server setup-https should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)
  args.exit_message = None
  if not SILENT:
    properties = get_ambari_properties()
    try:
      security_server_keys_dir = properties.get_property(SSL_KEY_DIR)
      client_api_ssl_port = DEFAULT_SSL_API_PORT if properties.get_property(SSL_API_PORT) in ("")\
                            else properties.get_property(SSL_API_PORT)
      api_ssl = properties.get_property(SSL_API) in ['true']
      cert_was_imported = False
      cert_must_import = True
      if api_ssl:
       if get_YN_input("Do you want to disable HTTPS [y/n] (n)? ", False):
        properties.process_pair(SSL_API, "false")
        cert_must_import=False
       else:
        properties.process_pair(SSL_API_PORT, \
                                get_validated_string_input(\
                                "SSL port ["+str(client_api_ssl_port)+"] ? ",\
                                str(client_api_ssl_port),\
                                "^[0-9]{1,5}$", "Invalid port.", False, validatorFunction = is_valid_https_port))
        cert_was_imported = import_cert_and_key_action(security_server_keys_dir, properties)
      else:
       if get_YN_input("Do you want to configure HTTPS [y/n] (y)? ", True):
        properties.process_pair(SSL_API_PORT,\
        get_validated_string_input("SSL port ["+str(client_api_ssl_port)+"] ? ",\
        str(client_api_ssl_port), "^[0-9]{1,5}$", "Invalid port.", False, validatorFunction = is_valid_https_port))
        cert_was_imported = import_cert_and_key_action(security_server_keys_dir, properties)
       else:
        return False

      if cert_must_import and not cert_was_imported:
        print 'Setup of HTTPS failed. Exiting.'
        return False

      conf_file = find_properties_file()
      f = open(conf_file, 'w')
      properties.store(f, "Changed by 'ambari-server setup-https' command")

      ambari_user = read_ambari_user()
      if ambari_user:
        adjust_directory_permissions(ambari_user)
      return True
    except (KeyError), e:
      err = 'Property ' + str(e) + ' is not defined'
      raise FatalException(1, err)
  else:
    warning = "setup-https is not enabled in silent mode."
    raise NonFatalException(warning)


def is_server_runing():
  pid_file_path = PID_DIR + os.sep + PID_NAME

  if os.path.exists(pid_file_path):
    try:
      f = open(pid_file_path, "r")
    except IOError, ex:
      raise FatalException(1, str(ex))

    pid = f.readline().strip()

    if not pid.isdigit():
      err = "%s is corrupt. Removing" % (pid_file_path)
      f.close()
      run_os_command("rm -f " + pid_file_path)
      raise NonFatalException(err)

    f.close()
    retcode, out, err = run_os_command("ps -p " + pid)
    if retcode == 0:
      return True, int(pid)
    else:
      return False, None
  else:
    return False, None


def setup_component_https(component, command, property, alias):

  if not SILENT:

    jdk_path = find_jdk()
    if jdk_path is None:
      err = "No JDK found, please run the \"ambari-server setup\" " \
                      "command to install a JDK automatically or install any " \
                      "JDK manually to " + JDK_INSTALL_DIR
      raise FatalException(1, err)

    properties = get_ambari_properties()

    use_https = properties.get_property(property) in ['true']

    if use_https:
      if get_YN_input("Do you want to disable HTTPS for " + component + " [y/n] (n)? ", False):

        truststore_path = get_truststore_path(properties)
        truststore_password = get_truststore_password(properties)

        run_component_https_cmd(get_delete_cert_command(jdk_path, alias, truststore_path, truststore_password))

        properties.process_pair(property, "false")

      else:
        return
    else:
      if get_YN_input("Do you want to configure HTTPS for " + component + " [y/n] (y)? ", True):

        truststore_type = get_truststore_type(properties)
        truststore_path = get_truststore_path(properties)
        truststore_password = get_truststore_password(properties)

        run_os_command(get_delete_cert_command(jdk_path, alias, truststore_path, truststore_password))

        import_cert_path = get_validated_filepath_input(\
                          "Enter path to " + component + " Certificate: ",\
                          "Certificate not found")

        run_component_https_cmd(get_import_cert_command(jdk_path, alias, truststore_type, import_cert_path, truststore_path, truststore_password))

        properties.process_pair(property, "true")

      else:
        return

    conf_file = find_properties_file()
    f = open(conf_file, 'w')
    properties.store(f, "Changed by 'ambari-server " + command + "' command")

  else:
    print command + " is not enabled in silent mode."


def get_truststore_type(properties):

  truststore_type = properties.get_property(SSL_TRUSTSTORE_TYPE_PROPERTY)
  if not truststore_type:
    SSL_TRUSTSTORE_TYPE_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_TYPE_PROPERTY, "jks")

    truststore_type = get_validated_string_input(
      "TrustStore type [jks/jceks/pkcs12] {0}:".format(get_prompt_default(SSL_TRUSTSTORE_TYPE_DEFAULT)),
      SSL_TRUSTSTORE_TYPE_DEFAULT,
      "^(jks|jceks|pkcs12)?$", "Wrong type", False)

    if truststore_type:
      properties.process_pair(SSL_TRUSTSTORE_TYPE_PROPERTY, truststore_type)

  return truststore_type


def get_truststore_path(properties):

  truststore_path = properties.get_property(SSL_TRUSTSTORE_PATH_PROPERTY)
  if not truststore_path:
    SSL_TRUSTSTORE_PATH_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_PATH_PROPERTY)

    while not truststore_path:
      truststore_path = get_validated_string_input(
        "Path to TrustStore file {0}:".format(get_prompt_default(SSL_TRUSTSTORE_PATH_DEFAULT)),
        SSL_TRUSTSTORE_PATH_DEFAULT,
        ".*", False, False)

    if truststore_path:
      properties.process_pair(SSL_TRUSTSTORE_PATH_PROPERTY, truststore_path)

  return truststore_path


def get_truststore_password(properties):
  truststore_password = properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
  isSecure = get_is_secure(properties)
  if truststore_password:
    if isSecure:
      truststore_password = decrypt_password_for_alias(SSL_TRUSTSTORE_PASSWORD_ALIAS)
  else:
    truststore_password = read_password("", ".*", "Password for TrustStore:", "Invalid characters in password")
    if truststore_password:
      encrypted_password = get_encrypted_password(SSL_TRUSTSTORE_PASSWORD_ALIAS, truststore_password, properties)
      properties.process_pair(SSL_TRUSTSTORE_PASSWORD_PROPERTY, encrypted_password)

  return truststore_password


def run_component_https_cmd(cmd):
  retcode, out, err = run_os_command(cmd)

  if not retcode == 0:
    err = 'Error occured during truststore setup ! :' + out + " : " + err
    raise FatalException(1, err)


def get_delete_cert_command(jdk_path, alias, truststore_path, truststore_password):
  cmd = KEYTOOL_DELETE_CERT_CMD.format(jdk_path, alias, truststore_password)
  if truststore_path:
    cmd += KEYTOOL_KEYSTORE.format(truststore_path)
  return cmd


def get_import_cert_command(jdk_path, alias, truststore_type, import_cert_path, truststore_path, truststore_password):
  cmd = KEYTOOL_IMPORT_CERT_CMD.format(jdk_path, alias, truststore_type, import_cert_path, truststore_password)
  if truststore_path:
    cmd += KEYTOOL_KEYSTORE.format(truststore_path)
  return cmd


def import_cert_and_key_action(security_server_keys_dir, properties):
  if import_cert_and_key(security_server_keys_dir):
   properties.process_pair(SSL_SERVER_CERT_NAME, SSL_CERT_FILE_NAME)
   properties.process_pair(SSL_SERVER_KEY_NAME, SSL_KEY_FILE_NAME)
   properties.process_pair(SSL_API, "true")
   return True
  else:
   return False


def import_cert_and_key(security_server_keys_dir):
  import_cert_path = get_validated_filepath_input(\
                    "Enter path to Certificate: ",\
                    "Certificate not found")
  import_key_path  =  get_validated_filepath_input(\
                      "Enter path to Private Key: ", "Private Key not found")
  pem_password = get_validated_string_input("Please enter password for Private Key: ", "", None, None, True)

  certInfoDict = get_cert_info(import_cert_path)

  if not certInfoDict:
    print_warning_msg('Unable to get Certificate information')
  else:
    #Validate common name of certificate
    if not is_valid_cert_host(certInfoDict):
      print_warning_msg('Unable to validate Certificate hostname')

    #Validate issue and expirations dates of certificate
    if not is_valid_cert_exp(certInfoDict):
      print_warning_msg('Unable to validate Certificate issue and expiration dates')

  #jetty requires private key files with non-empty key passwords
  retcode = 0
  err = ''
  if not pem_password:
    print 'Generating random password for HTTPS keystore...done.'
    pem_password = generate_random_string()
    retcode, out, err = run_os_command(CHANGE_KEY_PWD_CND.format(
      import_key_path, pem_password))
    import_key_path += '.secured'

  if retcode == 0:
    keystoreFilePath = os.path.join(security_server_keys_dir,\
                                    SSL_KEYSTORE_FILE_NAME)
    keystoreFilePathTmp = os.path.join(tempfile.gettempdir(),\
                                       SSL_KEYSTORE_FILE_NAME)
    passFilePath = os.path.join(security_server_keys_dir,\
                                SSL_KEY_PASSWORD_FILE_NAME)
    passFilePathTmp = os.path.join(tempfile.gettempdir(),\
      SSL_KEY_PASSWORD_FILE_NAME)
    passinFilePath = os.path.join(tempfile.gettempdir(),\
                                   SSL_PASSIN_FILE)
    passwordFilePath = os.path.join(tempfile.gettempdir(),\
                                   SSL_PASSWORD_FILE)

    with open(passFilePathTmp, 'w+') as passFile:
      passFile.write(pem_password)
      passFile.close
      pass

    set_file_permissions(passFilePath, "660", read_ambari_user(), False)

    copy_file(passFilePathTmp, passinFilePath)
    copy_file(passFilePathTmp, passwordFilePath)

    retcode, out, err = run_os_command(EXPRT_KSTR_CMD.format(import_cert_path,\
    import_key_path, passwordFilePath, passinFilePath, keystoreFilePathTmp))
  if retcode == 0:
   print 'Importing and saving Certificate...done.'
   import_file_to_keystore(keystoreFilePathTmp, keystoreFilePath)
   import_file_to_keystore(passFilePathTmp, passFilePath)

   import_file_to_keystore(import_cert_path, os.path.join(\
                          security_server_keys_dir, SSL_CERT_FILE_NAME))
   import_file_to_keystore(import_key_path, os.path.join(\
                          security_server_keys_dir, SSL_KEY_FILE_NAME))

   #Validate keystore
   retcode, out, err = run_os_command(VALIDATE_KEYSTORE_CMD.format(keystoreFilePath,\
   passwordFilePath, passinFilePath))

   remove_file(passinFilePath)
   remove_file(passwordFilePath)

   if not retcode == 0:
     print 'Error during keystore validation occured!:'
     print err
     return False

   return True
  else:
   print_error_msg('Could not import Certificate and Private Key.')
   print 'SSL error on exporting keystore: ' + err.rstrip() + \
         '.\nPlease ensure that provided Private Key password is correct and ' +\
         're-import Certificate.'

   return False


def import_file_to_keystore(source, destination):
  shutil.copy(source, destination)
  set_file_permissions(destination, "660", read_ambari_user(), False)


def generate_random_string(length=SSL_KEY_PASSWORD_LENGTH):
  chars = string.digits + string.ascii_letters
  return ''.join(random.choice(chars) for x in range(length))


def get_validated_filepath_input(prompt, description, default=None):
  input = False
  while not input:
    if SILENT:
      print (prompt)
      return default
    else:
      input = raw_input(prompt)
      if not input == None:
        input = input.strip()
      if not input == None and not "" == input and os.path.isfile(input):
        return input
      else:
        print description
        input = False


def get_cert_info(path):
  retcode, out, err = run_os_command(GET_CRT_INFO_CMD.format(path))

  if retcode != 0:
    print 'Error getting Certificate info'
    print err
    return None

  if out:
    certInfolist = out.split(os.linesep)
  else:
    print 'Empty Certificate info'
    return None

  notBefore = None
  notAfter = None
  subject = None

  for item in range(len(certInfolist)):

    if certInfolist[item].startswith('notAfter='):
      notAfter = certInfolist[item].split('=')[1]

    if certInfolist[item].startswith('notBefore='):
      notBefore = certInfolist[item].split('=')[1]

    if certInfolist[item].startswith('subject='):
      subject = certInfolist[item].split('=', 1)[1]

  #Convert subj to dict
  pattern = re.compile(r"[A-Z]{1,2}=[\w.-]{1,}")
  if subject:
    subjList = pattern.findall(subject)
    keys = [item.split('=')[0] for item in subjList]
    values = [item.split('=')[1] for item in subjList]
    subjDict = dict(zip(keys, values))

    result = subjDict
    result['notBefore'] = notBefore
    result['notAfter'] = notAfter
    result['subject'] = subject

    return result
  else:
    return {}


def is_valid_cert_exp(certInfoDict):
  if certInfoDict.has_key(NOT_BEFORE_ATTR):
    notBefore = certInfoDict[NOT_BEFORE_ATTR]
  else:
    print_warning_msg('There is no Not Before value in Certificate')
    return False

  if certInfoDict.has_key(NOT_AFTER_ATTR):
    notAfter = certInfoDict['notAfter']
  else:
    print_warning_msg('There is no Not After value in Certificate')
    return False

  notBeforeDate = datetime.datetime.strptime(notBefore, SSL_DATE_FORMAT)
  notAfterDate = datetime.datetime.strptime(notAfter, SSL_DATE_FORMAT)

  currentDate = datetime.datetime.now()

  if currentDate > notAfterDate:
    print_warning_msg('Certificate expired on: ' + str(notAfterDate))
    return False

  if currentDate < notBeforeDate:
    print_warning_msg('Certificate will be active from: ' + str(notBeforeDate))
    return False

  return True


def is_valid_cert_host(certInfoDict):
  if certInfoDict.has_key(COMMON_NAME_ATTR):
   commonName = certInfoDict[COMMON_NAME_ATTR]
  else:
    print_warning_msg('There is no Common Name in Certificate')
    return False

  fqdn = get_fqdn()

  if not fqdn:
    print_warning_msg('Failed to get server FQDN')
    return False

  if commonName != fqdn:
    print_warning_msg('Common Name in Certificate: ' + commonName + ' does not match the server FQDN: ' + fqdn)
    return False

  return True


def is_valid_https_port(port):
  properties = get_ambari_properties()
  if properties == -1:
    print "Error getting ambari properties"
    return False

  one_way_port = properties[SRVR_ONE_WAY_SSL_PORT_PROPERTY]
  if not one_way_port:
    one_way_port = SRVR_ONE_WAY_SSL_PORT

  two_way_port = properties[SRVR_TWO_WAY_SSL_PORT_PROPERTY]
  if not two_way_port:
    two_way_port = SRVR_TWO_WAY_SSL_PORT

  if port.strip() == one_way_port.strip():
    print "Port for https can't match the port for one way authentication port(" + one_way_port + ")"
    return False

  if port.strip() == two_way_port.strip():
    print "Port for https can't match the port for two way authentication port(" + two_way_port + ")"
    return False

  return True


def get_fqdn():
  properties = get_ambari_properties()
  if properties == -1:
    print "Error reading ambari properties"
    return None

  get_fqdn_service_url = properties[GET_FQDN_SERVICE_URL]
  try:
    handle = urllib2.urlopen(get_fqdn_service_url, '', 2)
    str = handle.read()
    handle.close()
    return str
  except Exception:
    return socket.getfqdn()


def get_ulimit_open_files():
  properties = get_ambari_properties()
  if properties == -1:
    print "Error reading ambari properties"
    return None

  open_files = int(properties[ULIMIT_OPEN_FILES_KEY])
  if open_files > 0:
    return open_files
  else:
    return ULIMIT_OPEN_FILES_DEFAULT


def is_valid_filepath(filepath):
  if not filepath or not os.path.exists(filepath) or os.path.isdir(filepath):
    print 'Invalid path, please provide the absolute file path.'
    return False
  else:
    return True


def setup_ambari_krb5_jaas():
  jaas_conf_file = search_file(SECURITY_KERBEROS_JASS_FILENAME, get_conf_dir())
  if os.path.exists(jaas_conf_file):
    print 'Setting up Ambari kerberos JAAS configuration to access ' +\
          'secured Hadoop daemons...'
    principal = get_validated_string_input('Enter ambari server\'s kerberos '
                  'principal name (ambari@EXAMPLE.COM): ', 'ambari@EXAMPLE.COM', '.*', '', False,
                  False)
    keytab = get_validated_string_input('Enter keytab path for ambari '
                  'server\'s kerberos principal: ',
                  '/etc/security/keytabs/ambari.keytab', '.*', False, False,
                  validatorFunction=is_valid_filepath)

    for line in fileinput.FileInput(jaas_conf_file, inplace=1):
      line = re.sub('keyTab=.*$', 'keyTab="' + keytab + '"', line)
      line = re.sub('principal=.*$', 'principal="' + principal + '"', line)
      print line,

  else:
    raise NonFatalException('No jaas config file found at location: ' +
                            jaas_conf_file)


def setup_security(args):
  need_restart = True
  #Print menu options
  print '=' * 75
  print 'Choose one of the following options: '
  print '  [1] Enable HTTPS for Ambari server.'
  print '  [2] Enable HTTPS for Ganglia service.'
  print '  [3] Enable HTTPS for Nagios service.'
  print '  [4] Encrypt passwords stored in ambari.properties file.'
  print '  [5] Setup Ambari kerberos JAAS configuration.'
  print '=' * 75
  choice = get_validated_string_input('Enter choice, (1-5): ', '0', '[1-5]',
                                      'Invalid choice', False, False)

  if choice == '1':
    need_restart = setup_https(args)
  elif choice == '2':
    setup_component_https("Ganglia", "setup-ganglia-https", GANGLIA_HTTPS,
                         "ganglia_cert")
  elif choice == '3':
    setup_component_https("Nagios", "setup-nagios-https", NAGIOS_HTTPS,
                          "nagios_cert")
  elif choice == '4':
    setup_master_key()
  elif choice == '5':
    setup_ambari_krb5_jaas()
  else:
    raise FatalException('Unknown option for setup-security command.')

  return need_restart

def refresh_stack_hash():
  properties = get_ambari_properties()
  stack_location = get_stack_location(properties)
  # Hack: we determine resource dir as a parent dir for stack_location
  resources_location = os.path.dirname(stack_location)
  resource_files_keeper = ResourceFilesKeeper(resources_location)

  try:
    print "Organizing resource files at {0}...".format(resources_location,
                                                       verbose=VERBOSE)
    resource_files_keeper.perform_housekeeping()
  except KeeperException, ex:
    msg = "Can not organize resource files at {0}: {1}".format(
                                                resources_location, str(ex))
    raise FatalException(-1, msg)


#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] action [stack_id os]",)

  parser.add_option('-f', '--init-script-file',
                      default='/var/lib/ambari-server/'
                              'resources/Ambari-DDL-Postgres-EMBEDDED-CREATE.sql',
                      help="File with setup script")
  parser.add_option('-r', '--drop-script-file', default="/var/lib/"
                              "ambari-server/resources/"
                              "Ambari-DDL-Postgres-EMBEDDED-DROP.sql",
                      help="File with drop script")
  parser.add_option('-u', '--upgrade-script-file', default="/var/lib/"
                              "ambari-server/resources/upgrade/ddl/"
                              "Ambari-DDL-Postgres-UPGRADE-1.3.0.sql",
                      help="File with upgrade script")
  parser.add_option('-t', '--upgrade-stack-script-file', default="/var/lib/"
                              "ambari-server/resources/upgrade/dml/"
                              "Ambari-DML-Postgres-UPGRADE_STACK.sql",
                      help="File with stack upgrade script")
  parser.add_option('-j', '--java-home', default=None,
                  help="Use specified java_home.  Must be valid on all hosts")
  parser.add_option("-v", "--verbose",
                  action="store_true", dest="verbose", default=False,
                  help="Print verbose status messages")
  parser.add_option("-s", "--silent",
                  action="store_true", dest="silent", default=False,
                  help="Silently accepts default prompt values")
  parser.add_option('-g', '--debug', action="store_true", dest='debug', default=False,
                    help="Start ambari-server in debug mode")

  parser.add_option('--all', action="store_true", default=False, help="LDAP sync all Ambari users and groups", dest="ldap_sync_all")
  parser.add_option('--existing', action="store_true", default=False, help="LDAP sync existing Ambari users and groups only", dest="ldap_sync_existing")
  parser.add_option('--users', default=None, help="Specifies the path to the LDAP sync users CSV file.", dest="ldap_sync_users")
  parser.add_option('--groups', default=None, help="Specifies the path to the LDAP sync groups CSV file.", dest="ldap_sync_groups")

  parser.add_option('--database', default=None, help="Database to use embedded|oracle|mysql|postgres", dest="dbms")
  parser.add_option('--databasehost', default=None, help="Hostname of database server", dest="database_host")
  parser.add_option('--databaseport', default=None, help="Database port", dest="database_port")
  parser.add_option('--databasename', default=None, help="Database/Service name or ServiceID",
                    dest="database_name")
  parser.add_option('--postgresschema', default=None, help="Postgres database schema name",
                    dest="postgres_schema")
  parser.add_option('--databaseusername', default=None, help="Database user login", dest="database_username")
  parser.add_option('--databasepassword', default=None, help="Database user password", dest="database_password")
  parser.add_option('--sidorsname', default="sname", help="Oracle database identifier type, Service ID/Service "
                                                         "Name sid|sname", dest="sid_or_sname")
  parser.add_option('--jdbc-driver', default=None, help="Specifies the path to the JDBC driver JAR file for the " \
                            "database type specified with the --jdbc-db option. Used only with --jdbc-db option.",
                    dest="jdbc_driver")
  parser.add_option('--jdbc-db', default=None, help="Specifies the database type [postgres|mysql|oracle] for the " \
            "JDBC driver specified with the --jdbc-driver option. Used only with --jdbc-driver option.", dest="jdbc_db")
  (options, args) = parser.parse_args()

  # set verbose
  global VERBOSE
  VERBOSE = options.verbose

  # set silent
  global SILENT
  SILENT = options.silent

  # debug mode
  global SERVER_DEBUG_MODE
  SERVER_DEBUG_MODE = options.debug

  # set ldap_sync_all
  global LDAP_SYNC_ALL
  LDAP_SYNC_ALL = options.ldap_sync_all

  # set ldap_sync_existing
  global LDAP_SYNC_EXISTING
  LDAP_SYNC_EXISTING = options.ldap_sync_existing

  # set ldap_sync_users
  global LDAP_SYNC_USERS
  LDAP_SYNC_USERS = options.ldap_sync_users

  # set ldap_sync_groups
  global LDAP_SYNC_GROUPS
  LDAP_SYNC_GROUPS = options.ldap_sync_groups

  global DATABASE_INDEX
  global PROMPT_DATABASE_OPTIONS
  #perform checks

  options.warnings = []

  if options.dbms is None \
    and options.database_host is None \
    and options.database_port is None \
    and options.database_name is None \
    and options.database_username is None \
    and options.database_password is None:

    PROMPT_DATABASE_OPTIONS = True

  elif not (options.dbms is not None
    and options.database_host is not None
    and options.database_port is not None
    and options.database_name is not None
    and options.database_username is not None
    and options.database_password is not None):
    parser.error('All database options should be set. Please see help for the options.')

  #correct database
  if options.dbms == 'embedded':
    print "WARNING: HostName for postgres server " + options.database_host + \
          " will be ignored: using localhost."
    options.database_host = "localhost"
    options.dbms = 'postgres'
    options.persistence_type = 'local'
    options.database_index = 0
    DATABASE_INDEX = 0
    pass
  elif options.dbms is not None and options.dbms not in DATABASE_TYPES:
    parser.print_help()
    parser.error("Unsupported Database " + options.dbms)
  elif options.dbms is not None:
    options.dbms = options.dbms.lower()
    DATABASE_INDEX = DATABASE_TYPES.index(options.dbms)

  #correct port
  if options.database_port is not None:
    correct = False
    try:
      port = int(options.database_port)
      if 65536 > port > 0:
        correct = True
    except ValueError:
      pass
    if not correct:
      parser.print_help()
      parser.error("Incorrect database port " + options.database_port)

  # jdbc driver and db options validation
  if options.jdbc_driver is None and options.jdbc_db is not None:
    parser.error("Option --jdbc-db is used only in pair with --jdbc-driver")
  elif options.jdbc_driver is not None and options.jdbc_db is None:
    parser.error("Option --jdbc-driver is used only in pair with --jdbc-db")

  if options.sid_or_sname.lower() not in ["sid", "sname"]:
    print "WARNING: Valid values for sid_or_sname are 'sid' or 'sname'. Use 'sid' if the db identifier type is " \
          "Service ID. Use 'sname' if the db identifier type is Service Name"
    parser.print_help()
    exit(-1)
  else:
    options.sid_or_sname = options.sid_or_sname.lower()

  if len(args) == 0:
    print parser.print_help()
    parser.error("No action entered")

  action = args[0]

  if action == UPGRADE_STACK_ACTION:
    possible_args_numbers = [2,4] # OR
  else:
    possible_args_numbers = [1]

  matches = 0
  for args_number_required in possible_args_numbers:
    matches += int(len(args) == args_number_required)

  if matches == 0:
    print parser.print_help()
    possible_args = ' or '.join(str(x) for x in possible_args_numbers)
    parser.error("Invalid number of arguments. Entered: " + str(len(args)) + ", required: " + possible_args)

  options.exit_message = "Ambari Server '%s' completed successfully." % action
  need_restart = True
  try:
    if action == SETUP_ACTION:
      setup(options)
    elif action == START_ACTION:
      start(options)
    elif action == STOP_ACTION:
      stop(options)
    elif action == RESET_ACTION:
      reset(options)
    elif action == STATUS_ACTION:
      status(options)
    elif action == UPGRADE_ACTION:
      upgrade(options)
    elif action == UPGRADE_STACK_ACTION:
      stack_id = args[1]
      repo_url = None
      repo_url_os = None

      if len(args) > 2:
        repo_url = args[2]
      if len(args) > 3:
        repo_url_os = args[3]

      upgrade_stack(options, stack_id, repo_url, repo_url_os)
    elif action == LDAP_SETUP_ACTION:
      setup_ldap()
    elif action == LDAP_SYNC_ACTION:
      sync_ldap()
    elif action == SETUP_SECURITY_ACTION:
      need_restart = setup_security(options)
    elif action == REFRESH_STACK_HASH_ACTION:
      refresh_stack_hash()
    else:
      parser.error("Invalid action")

    if action in ACTION_REQUIRE_RESTART and need_restart:
      pstatus, pid = is_server_runing()
      if pstatus:
        print 'NOTE: Restart Ambari Server to apply changes' + \
              ' ("ambari-server restart|stop|start")'

    if options.warnings:
      for warning in options.warnings:
        print_warning_msg(warning)
        pass
      options.exit_message = "Ambari Server '%s' completed with warnings." % action
      pass
  except FatalException as e:
    if e.reason is not None:
      print_error_msg("Exiting with exit code {0}. \nREASON: {1}".format(e.code, e.reason))
    sys.exit(e.code)
  except NonFatalException as e:
    options.exit_message = "Ambari Server '%s' completed with warnings." % action
    if e.reason is not None:
      print_warning_msg(e.reason)

  if options.exit_message is not None:
    print options.exit_message


# A Python replacement for java.util.Properties
# Based on http://code.activestate.com/recipes
# /496795-a-python-replacement-for-javautilproperties/
class Properties(object):
  def __init__(self, props=None):
    self._props = {}
    self._origprops = {}
    self._keymap = {}

    self.othercharre = re.compile(r'(?<!\\)(\s*\=)|(?<!\\)(\s*\:)')
    self.othercharre2 = re.compile(r'(\s*\=)|(\s*\:)')
    self.bspacere = re.compile(r'\\(?!\s$)')

  def __parse(self, lines):
    lineno = 0
    i = iter(lines)
    for line in i:
      lineno += 1
      line = line.strip()
      if not line:
        continue
      if line[0] == '#':
        continue
      escaped = False
      sepidx = -1
      flag = 0
      m = self.othercharre.search(line)
      if m:
        first, last = m.span()
        start, end = 0, first
        flag = 1
        wspacere = re.compile(r'(?<![\\\=\:])(\s)')
      else:
        if self.othercharre2.search(line):
          wspacere = re.compile(r'(?<![\\])(\s)')
        start, end = 0, len(line)
      m2 = wspacere.search(line, start, end)
      if m2:
        first, last = m2.span()
        sepidx = first
      elif m:
        first, last = m.span()
        sepidx = last - 1
      while line[-1] == '\\':
        nextline = i.next()
        nextline = nextline.strip()
        lineno += 1
        line = line[:-1] + nextline
      if sepidx != -1:
        key, value = line[:sepidx], line[sepidx + 1:]
      else:
        key, value = line, ''
      self.process_pair(key, value)

  def process_pair(self, key, value):
    """
    Adds or overrides the property with the given key.
    """
    oldkey = key
    oldvalue = value
    keyparts = self.bspacere.split(key)
    strippable = False
    lastpart = keyparts[-1]
    if lastpart.find('\\ ') != -1:
      keyparts[-1] = lastpart.replace('\\', '')
    elif lastpart and lastpart[-1] == ' ':
      strippable = True
    key = ''.join(keyparts)
    if strippable:
      key = key.strip()
      oldkey = oldkey.strip()
    oldvalue = self.unescape(oldvalue)
    value = self.unescape(value)
    self._props[key] = None if value is None else value.strip()
    if self._keymap.has_key(key):
      oldkey = self._keymap.get(key)
      self._origprops[oldkey] = None if oldvalue is None else oldvalue.strip()
    else:
      self._origprops[oldkey] = None if oldvalue is None else oldvalue.strip()
      self._keymap[key] = oldkey

  def unescape(self, value):
    newvalue = value
    if not value is None:
     newvalue = value.replace('\:', ':')
     newvalue = newvalue.replace('\=', '=')
    return newvalue

  def removeOldProp(self, key):
    if self._origprops.has_key(key):
      del self._origprops[key]
    pass

  def load(self, stream):
    if type(stream) is not file:
      raise TypeError, 'Argument should be a file object!'
    if stream.mode != 'r':
      raise ValueError, 'Stream should be opened in read-only mode!'
    try:
      self.fileName = os.path.abspath(stream.name)
      lines = stream.readlines()
      self.__parse(lines)
    except IOError:
      raise

  def get_property(self, key):
    return self._props.get(key, '')

  def propertyNames(self):
    return self._props.keys()

  def getPropertyDict(self):
    return self._props

  def __getitem__(self, name):
    return self.get_property(name)

  def __getattr__(self, name):
    try:
      return self.__dict__[name]
    except KeyError:
      if hasattr(self._props, name):
        return getattr(self._props, name)

  def store(self, out, header=""):
    """ Write the properties list to the stream 'out' along
    with the optional 'header'
    This function will attempt to close the file handler once it's done.
    """
    if out.mode[0] != 'w':
      raise ValueError, 'Steam should be opened in write mode!'
    try:
      out.write(''.join(('#', ASF_LICENSE_HEADER, '\n')))
      out.write(''.join(('#', header, '\n')))
      # Write timestamp
      tstamp = time.strftime('%a %b %d %H:%M:%S %Z %Y', time.localtime())
      out.write(''.join(('#', tstamp, '\n')))
      # Write properties from the pristine dictionary
      for prop, val in self._origprops.items():
        if val is not None:
          out.write(''.join((prop, '=', val, '\n')))
    except IOError:
      raise
    finally:
      if out:
        out.close()

if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
