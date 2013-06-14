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

import optparse
from pprint import pprint
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
import datetime
import socket
import tempfile

# debug settings
VERBOSE = False
SILENT = False
SERVER_START_DEBUG = False

# action commands
SETUP_ACTION = "setup"
START_ACTION = "start"
STOP_ACTION = "stop"
RESET_ACTION = "reset"
UPGRADE_ACTION = "upgrade"
UPGRADE_STACK_ACTION = "upgradestack"
STATUS_ACTION = "status"
LDAP_SETUP_ACTION = "setupldap"
RESET_MASTER_KEY_ACTION = "resetmasterkey"

# selinux commands
GET_SE_LINUX_ST_CMD = "/usr/sbin/sestatus"
SE_SETENFORCE_CMD = "setenforce 0"
SE_STATUS_DISABLED = "disabled"
SE_STATUS_ENABLED = "enabled"
SE_MODE_ENFORCING = "enforcing"
SE_MODE_PERMISSIVE = "permissive"

# iptables commands
IP_TBLS_ST_CMD = "/sbin/service iptables status"
IP_TBLS_STOP_CMD = "/sbin/service iptables stop"
IP_TBLS_ENABLED = "Firewall is running"
IP_TBLS_DISABLED = "Firewall is stopped.\n"
IP_TBLS_SRVC_NT_FND = "iptables: unrecognized service"

# server commands
ambari_provider_module_option = ""
ambari_provider_module = os.environ.get('AMBARI_PROVIDER_MODULE')

# Non-root user setup commands
NR_USER_PROPERTY = "ambari.user"
NR_USER_COMMENT =  "Ambari user"
NR_GET_OWNER_CMD = 'stat -c "%U" {0}'
NR_USERADD_CMD = 'useradd -M -g {0} --comment "{1}" ' \
                 '--shell /sbin/nologin -d /var/lib/ambari-server/keys/ {0}'
NR_SET_USER_COMMENT_CMD = 'usermod -c "{0}" {1}'
NR_GROUPADD_CMD = 'groupadd {0}'
NR_ADD_USER_TO_GROUP = 'usermod -G {0} {0}'
NR_CHMOD_CMD = 'chmod {0} {1}'
NR_CHOWN_CMD = 'chown {0}:{1} {2}'

# constants
STACK_NAME_VER_SEP = "-"
JAVA_SHARE_PATH="/usr/share/java"

# terminal styles
BOLD_ON='\033[1m'
BOLD_OFF='\033[0m'

#Common messages
PRESS_ENTER_MSG="Press <enter> to continue."

if ambari_provider_module is not None:
  ambari_provider_module_option = "-Dprovider.module.class=" +\
                                  ambari_provider_module + " "

SERVER_START_CMD="{0}" + os.sep + "bin" + os.sep +\
                 "java -server -XX:NewRatio=3 "\
                 "-XX:+UseConcMarkSweepGC " +\
                 "-XX:-UseGCOverheadLimit -XX:CMSInitiatingOccupancyFraction=60 " +\
                 ambari_provider_module_option +\
                 os.getenv('AMBARI_JVM_ARGS','-Xms512m -Xmx2048m') +\
                 " -cp {1}"+ os.pathsep + "{2}" +\
                 " org.apache.ambari.server.controller.AmbariServer "\
                 ">/var/log/ambari-server/ambari-server.out 2>&1 &" \
                 " echo $! > {3}" # Writing pidfile
SERVER_START_CMD_DEBUG="{0}" + os.sep + "bin" + os.sep +\
                       "java -server -XX:NewRatio=2 -XX:+UseConcMarkSweepGC " +\
                       ambari_provider_module_option +\
                       os.getenv('AMBARI_JVM_ARGS','-Xms512m -Xmx2048m') +\
                       " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,"\
                       "server=y,suspend=n -cp {1}"+ os.pathsep + "{2}" +\
                       " org.apache.ambari.server.controller.AmbariServer &" \
                       " echo $! > {3}" # Writing pidfile

SECURITY_PROVIDER_GET_CMD="{0}" + os.sep + "bin" + os.sep + "java -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.security.encryption" +\
                          ".CredentialProvider GET {3} {4} {5} " +\
                          "> /var/log/ambari-server/ambari-server.out 2>&1"

SECURITY_PROVIDER_PUT_CMD="{0}" + os.sep + "bin" + os.sep + "java -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.security.encryption" +\
                          ".CredentialProvider PUT {3} {4} {5} " +\
                          "> /var/log/ambari-server/ambari-server.out 2>&1"

SECURITY_PROVIDER_KEY_CMD="{0}" + os.sep + "bin" + os.sep + "java -cp {1}" +\
                          os.pathsep + "{2} " +\
                          "org.apache.ambari.server.security.encryption" +\
                          ".MasterKeyServiceImpl {3} {4} {5} " +\
                          "> /var/log/ambari-server/ambari-server.out 2>&1"

SECURITY_KEYS_DIR = "security.server.keys_dir"
SECURITY_MASTER_KEY_LOCATION = "security.master.key.location"
SECURITY_KEY_IS_PERSISTED = "security.master.key.ispersisted"
SECURITY_KEY_ENV_VAR_NAME = "ambari.security.master.key"
SECURITY_MASTER_KEY_FILENAME = "master"

JDBC_RCA_PASSWORD_ALIAS = "ambari.db.password"
LDAP_MGR_PASSWORD_ALIAS = "ambari.ldap.manager.password"
LDAP_MGR_PASSWORD_PROPERTY = "authentication.ldap.managerPassword"


AMBARI_CONF_VAR="AMBARI_CONF_DIR"
AMBARI_SERVER_LIB="AMBARI_SERVER_LIB"
JAVA_HOME="JAVA_HOME"
PID_DIR="/var/run/ambari-server"
PID_NAME="ambari-server.pid"
AMBARI_PROPERTIES_FILE="ambari.properties"
AMBARI_PROPERTIES_RPMSAVE_FILE="ambari.properties.rpmsave"
RESOURCES_DIR_PROPERTY="resources.dir"

SETUP_DB_CMD = ['su', '-', 'postgres',
        '--command=psql -f {0} -v username=\'"{1}"\' -v password="\'{2}\'"']
UPGRADE_STACK_CMD = ['su', 'postgres',
        '--command=psql -f {0} -v stack_name="\'{1}\'"  -v stack_version="\'{2}\'"']
PG_ST_CMD = "/sbin/service postgresql status"
PG_INITDB_CMD = "/sbin/service postgresql initdb"
PG_START_CMD = "/sbin/service postgresql start"
PG_RESTART_CMD = "/sbin/service postgresql restart"
PG_STATUS_RUNNING = "running"
PG_HBA_DIR = "/var/lib/pgsql/data/"
PG_HBA_CONF_FILE = PG_HBA_DIR + "pg_hba.conf"
PG_HBA_CONF_FILE_BACKUP = PG_HBA_DIR + "pg_hba_bak.conf.old"
POSTGRESQL_CONF_FILE = PG_HBA_DIR + "postgresql.conf"
PG_HBA_RELOAD_CMD = "su postgres --command='pg_ctl -D {0} reload'"
PG_DEFAULT_PASSWORD = "bigdata"

JDBC_DATABASE_PROPERTY = "server.jdbc.database"
JDBC_HOSTNAME_PROPERTY = "server.jdbc.hostname"
JDBC_PORT_PROPERTY = "server.jdbc.port"
JDBC_SCHEMA_PROPERTY = "server.jdbc.schema"

JDBC_USER_NAME_PROPERTY = "server.jdbc.user.name"
JDBC_PASSWORD_PROPERTY = "server.jdbc.user.passwd"
JDBC_PASSWORD_FILENAME = "password.dat"
JDBC_RCA_PASSWORD_FILENAME = "rca_password.dat"

PERSISTENCE_TYPE_PROPERTY = "server.persistence.type"
JDBC_DRIVER_PROPERTY = "server.jdbc.driver"
JDBC_URL_PROPERTY = "server.jdbc.url"

JDBC_RCA_DATABASE_PROPERTY = "server.jdbc.database"
JDBC_RCA_HOSTNAME_PROPERTY = "server.jdbc.hostname"
JDBC_RCA_PORT_PROPERTY = "server.jdbc.port"
JDBC_RCA_SCHEMA_PROPERTY = "server.jdbc.schema"

JDBC_RCA_DRIVER_PROPERTY = "server.jdbc.rca.driver"
JDBC_RCA_URL_PROPERTY = "server.jdbc.rca.url"
JDBC_RCA_USER_NAME_PROPERTY = "server.jdbc.rca.user.name"
JDBC_RCA_PASSWORD_FILE_PROPERTY = "server.jdbc.rca.user.passwd"

CHECK_COMMAND_EXIST_CMD = "type {0}"

DATABASE_INDEX = 0
PROMPT_DATABASE_OPTIONS = False
USERNAME_PATTERN = "^[a-zA-Z_][a-zA-Z0-9_\-]*$"
PASSWORD_PATTERN = "^[a-zA-Z0-9_-]*$"
DATABASE_NAMES =["postgres", "oracle"]
DATABASE_STORAGE_NAMES =["Database","Service","Schema"]
DATABASE_PORTS =["5432", "1521", "3306"]
DATABASE_DRIVER_NAMES = ["org.postgresql.Driver", "oracle.jdbc.driver.OracleDriver", "com.mysql.jdbc.Driver"]
DATABASE_CONNECTION_STRINGS = ["jdbc:postgresql://{0}:{1}/{2}", "jdbc:oracle:thin:@{0}:{1}/{2}", "jdbc:mysql://{0}:{1}/{2}"]
DATABASE_CLI_TOOLS = [["psql"], ["sqlplus", "sqlplus64"], ["mysql"]]
DATABASE_CLI_TOOLS_DESC = ["psql", "sqlplus", "mysql"]
DATABASE_CLI_TOOLS_USAGE = ['su -postgres --command=psql -f {0} -v username=\'"{1}"\' -v password="\'{2}\'"',
                            'sqlplus {1}/{2} < {0} ',
                            'mysql --user={1} --password={2} {3}<{0}']

DATABASE_INIT_SCRIPTS = ['/var/lib/ambari-server/resources/Ambari-DDL-Postgres-REMOTE-CREATE.sql',
                         '/var/lib/ambari-server/resources/Ambari-DDL-Oracle-CREATE.sql',
                         '/var/lib/ambari-server/resources/Ambari-DDL-MySQL-CREATE.sql']
DATABASE_DROP_SCRIPTS = ['/var/lib/ambari-server/resources/Ambari-DDL-Postgres-REMOTE-DROP.sql',
                         '/var/lib/ambari-server/resources/Ambari-DDL-Oracle-DROP.sql',
                         '/var/lib/ambari-server/resources/Ambari-DDL-MySQL-DROP.sql']
DATABASE_URL_REGEX = ["jdbc:postgresql://([a-zA-Z0-9._]+):(\d+)/(.+)",
                     "jdbc:oracle:thin:@([a-zA-Z0-9._]+):(\d+)/(.+)",
                     "jdbc:mysql://([a-zA-Z0-9._]+):(\d*)/(.+)"]

REGEX_IP_ADDRESS = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
REGEX_HOSTNAME = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"

POSTGRES_EXEC_ARGS = "-h {0} -p {1} -d {2} -U {3} -f {4} -v username='\"{3}\"'"
ORACLE_EXEC_ARGS = "-S '{0}/{1}@(description=(address=(protocol=TCP)(host={2})(port={3}))(connect_data=(sid={4})))' @{5} {0}"
MYSQL_EXEC_ARGS = "--host={0} --port={1} --user={2} --password={3} {4} " \
                 "-e\"set @schema=\'{4}\'; set @username=\'{2}\'; source {5};\""

JDBC_PATTERNS = {"oracle":"*ojdbc*.jar", "mysql":"*mysql*.jar"}
DATABASE_FULL_NAMES = {"oracle":"Oracle", "mysql":"MySQL", "postgres":"PostgreSQL"}


# jdk commands
JDK_LOCAL_FILENAME = "jdk-6u31-linux-x64.bin"
JDK_MIN_FILESIZE = 5000
JDK_INSTALL_DIR = "/usr/jdk64"
CREATE_JDK_DIR_CMD = "/bin/mkdir -p " + JDK_INSTALL_DIR
MAKE_FILE_EXECUTABLE_CMD = "chmod a+x {0}"
JAVA_HOME_PROPERTY = "java.home"
JDK_URL_PROPERTY='jdk.url'
JCE_URL_PROPERTY='jce_policy.url'
OS_TYPE_PROPERTY = "server.os_type"

JDK_DOWNLOAD_CMD = "curl --create-dirs -o {0} {1}"
JDK_DOWNLOAD_SIZE_CMD = "curl -I {0}"

#JCE Policy files
JCE_POLICY_FILENAME = "jce_policy-6.zip"
JCE_DOWNLOAD_CMD = "curl -o {0} {1}"
JCE_MIN_FILESIZE = 5000

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
    print_info_msg ('Loading properties from ' + conf_file)
  return conf_file


def update_ambari_properties():
  prev_conf_file = search_file(AMBARI_PROPERTIES_RPMSAVE_FILE, get_conf_dir())
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())

  if not prev_conf_file: # Previous config file does not exist
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
      new_properties.process_pair(prop_key,prop_value)

    new_properties.store(open(conf_file,'w'))

  except Exception, e:
    print 'Could not write "%s": %s' % (conf_file, e)
    return -1

  timestamp = datetime.datetime.now()
  format = '%Y%m%d%H%M%S'
  os.rename(AMBARI_PROPERTIES_RPMSAVE_FILE, AMBARI_PROPERTIES_RPMSAVE_FILE +
                                            '.' + timestamp.strftime(format))

  return 0



# ownership/permissions mapping
# path - permissions - user - group
# {0} in user/group will be replaced by customized ambari-server username
NR_CONF_DIR = get_conf_dir()
NR_ADJUST_OWNERSHIP_LIST =[
  ( "/etc/ambari-server/conf", "755", "{0}", "{0}" ),
  ( "/etc/ambari-server/conf/ambari.properties", "644", "{0}", "{0}" ),
  ( "/etc/ambari-server/conf/log4j.properties", "644", "root", "root" ),
  ( "/var/lib/ambari-server/keys", "700", "{0}", "{0}" ),
  ( "/var/lib/ambari-server/keys/db", "700", "{0}", "{0}" ),
  ( "/var/lib/ambari-server/keys/db/index.txt", "700", "{0}", "{0}" ),
  ( "/var/lib/ambari-server/keys/db/serial", "700", "{0}", "{0}" ),
  ( "/var/lib/ambari-server/keys/db/newcerts", "700", "{0}", "{0}" ),
  ( "/var/run/ambari-server", "755", "{0}", "{0}" ),
  ( "/var/run/ambari-server/bootstrap", "755", "{0}", "{0}" ),
  ( "/var/log/ambari-server", "755", "{0}", "{0}" ),
  ( "/var/lib/ambari-server/ambari-env.sh", "770", "{0}", "root" ),
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

    def _get_message(self):
      return str(self)


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
        "SELinux is set to 'permissive' mode and temporarily disabled."
        " You should disable SELinux permanently.")
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
  print "adjusting directory permissions..."
  for pack in NR_ADJUST_OWNERSHIP_LIST:
    file = pack[0]
    mod = pack[1]
    user = pack[2].format(ambari_user)
    group = pack[3].format(ambari_user)
    set_file_permissions(file, mod, user, group)


def set_file_permissions(file, mod, user, group):
  WARN_MSG = "Command {0} returned exit code {1} with message: {2}"
  if os.path.exists(file):
    command = NR_CHMOD_CMD.format(mod, file)
    retcode, out, err = run_os_command(command)
    if retcode != 0 :
      print_warning_msg(WARN_MSG.format(command, file, err))
    command = NR_CHOWN_CMD.format(user, group, file)
    retcode, out, err = run_os_command(command)
    if retcode != 0 :
      print_warning_msg(WARN_MSG.format(command, file, err))
  else:
    print_warning_msg("File %s does not exist" % file)


def create_custom_user():
  user = get_validated_string_input(
    "Please choose user name for ambari-server process: ",
    "ambari",
    "^[a-z_][a-z0-9_-]{1,31}$",
    "Invalid username.",
    False
  )

  print_info_msg("Trying to create group {0}".format(user))
  command = NR_GROUPADD_CMD.format(user)
  retcode, out, err = run_os_command(command)
  if retcode == 9: # 9 = group already exists
    print_warning_msg("Group {0} already exists, "
                      "skipping group creation".format(user))
  elif retcode != 0: # fail:
    print_warning_msg("Can't create group {0}. Command {1} "
                      "finished with {2}: \n{3}".format(user, command, retcode, err))
    return  retcode, None

  print_info_msg("Trying to create user {0}".format(user))
  command = NR_USERADD_CMD.format(user, NR_USER_COMMENT)
  retcode, out, err = run_os_command(command)
  if retcode == 9: # 9 = username already in use
    print_warning_msg("User {0} already exists, "
                      "skipping user creation".format(user))

    print_info_msg("Trying to add user {0} to group {0}".format(user))
    command = NR_ADD_USER_TO_GROUP.format(user)

    retcode, out, err = run_os_command(command)
    if retcode != 0: # fail:
      print_warning_msg("Can't add user {0} to group {0}. Command {1} "
                        "finished with {2}: \n{3}".format(user, command, retcode, err))
      return  retcode, None
  elif retcode != 0: # fail
    print_warning_msg("Can't create user {0}. Command {1} "
                      "finished with {2}: \n{3}".format(user, command, retcode, err))
    return retcode, None



  print_info_msg("User/group configuration is done.")
  return 0, user


def check_ambari_user():
  try:
    user = read_ambari_user()
    if user is not None:
      print_info_msg("Detected custom user {0}".format(user))
    else: # user is not configured yet or server is running under root
      ok = get_YN_input("Create a separate user for ambari-server "
                   "daemon [y/n] (n)? ", False)
      if ok:
        (retcode, user) = create_custom_user()
        if retcode != 0:
          return retcode
      else:
        user = "root"
      write_property(NR_USER_PROPERTY, user)
    adjust_directory_permissions(user)
  except OSError:
    print_error_msg("Failed: %s" % OSError.message)
    return 4
  except Exception as e:
    print_error_msg("Unexpected error %s" % e.message)
    return 1
  return 0





#
# Checks iptables
#
def check_iptables():
  # not used
  # retcode, out, err = run_os_command(IP_TBLS_ST_CMD)
  ''' This check doesn't work on CentOS 6.2 if firewall AND
  iptables service are running if out == IP_TBLS_ENABLED:
    print 'iptables is enabled now'
    print 'Stopping iptables service'
  '''
  retcode, out, err = run_os_command(IP_TBLS_STOP_CMD)
  print 'iptables is disabled now'

  if not retcode == 0 and err and len(err) > 0:
    print err

  if err.strip() == IP_TBLS_SRVC_NT_FND:
    return 0
  else:
    return retcode, out



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
  command = PG_HBA_RELOAD_CMD.format(PG_HBA_DIR)
  retcode, out, err = run_os_command(command)
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
      return 0
  configure_pg_hba_postgres_user()
  configure_pg_hba_ambaridb_users()
  os.chmod(PG_HBA_CONF_FILE, 0644)
  configure_postgresql_conf()
  #restart postgresql if already running
  pg_status = get_postgre_status()
  if pg_status == PG_STATUS_RUNNING:
    retcode = restart_postgres()
    return retcode
  return 0



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


# todo: check if the scheme is already exist

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


def setup_db(args):
  #password access to ambari-server and mapred
  configure_database_username_password(args)
  dbname = args.database_name
  scriptFile = args.init_script_file
  username = args.database_username
  password = args.database_password
  command = SETUP_DB_CMD[:]
  command[-1] = command[-1].format(scriptFile, username, password)
  retcode, outdata, errdata = run_os_command(command)
  if not retcode == 0:
    print errdata
  return retcode


def store_password_file(password, filename):
  conf_file = find_properties_file()
  passFilePath = os.path.join(os.path.dirname(conf_file),
    filename)

  with open(passFilePath, 'w+') as passFile:
    passFile.write(password)
  print_info_msg("Adjusting filesystem permissions")  
  ambari_user = read_ambari_user()
  set_file_permissions(passFilePath, "660", ambari_user, "root")

  return passFilePath


def execute_db_script(args, file):
  #password access to ambari-server and mapred
  configure_database_username_password(args)
  dbname = args.database_name
  username = args.database_username
  password = args.database_password
  command = SETUP_DB_CMD[:]
  command[-1] = command[-1].format(file, username, password)
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
  command[-1] = command[-1].format(file, username, password)
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
    pg_status = re.search('(stopped|running)', out).group(0)
  except AttributeError:
    pg_status = None
  return pg_status



def check_postgre_up():
  pg_status = get_postgre_status()
  if pg_status == PG_STATUS_RUNNING:
    print_info_msg ("PostgreSQL is running")
    return 0
  else:
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
      time.sleep(20)
      result = process.poll()
      print_info_msg("Result of postgres start cmd: " + str(result))
      if result is None:
        process.kill()
        pg_status = get_postgre_status()
        if pg_status == PG_STATUS_RUNNING:
          print_info_msg("Postgres process is running. Returning...")
          return 0
      else:
        retcode = result
    except (Exception), e:
      pg_status = get_postgre_status()
      if pg_status == PG_STATUS_RUNNING:
        return 0
      else:
        print_error_msg("Postgres start failed. " + str(e))
        return 1
    return retcode



def read_password(passwordDefault=PG_DEFAULT_PASSWORD,
                  passwordPattern=PASSWORD_PATTERN,
                  passwordPrompt=None,
                  passwordDescr=None):
  # setup password
  if passwordPrompt is None:
    passwordPrompt = 'Password [' + passwordDefault + ']: '

  if passwordDescr is None:
    passwordDescr = "Invalid characters in password. Use only alphanumeric or " \
                    "_ or - characters"

  password = get_validated_string_input(passwordPrompt, passwordDefault,
                                        passwordPattern, passwordDescr, True)
  if password != passwordDefault:
    password1 = get_validated_string_input("Re-enter password: ",
                                           passwordDefault, passwordPattern, passwordDescr, True)
    if password != password1:
      print "Passwords do not match"
      password = read_password()

  return password



def get_pass_file_path(conf_file):
  return os.path.join(os.path.dirname(conf_file),
                      JDBC_PASSWORD_FILENAME)


# Set database properties to default values
def load_default_db_properties(args):
  args.database=DATABASE_NAMES[DATABASE_INDEX]
  args.database_host = "localhost"
  args.database_port = DATABASE_PORTS[DATABASE_INDEX]
  args.database_name = "ambari"
  args.database_username = "ambari"
  args.database_password = "bigdata"
  pass


# Ask user for database conenction properties
def prompt_db_properties(args):
  global DATABASE_INDEX

  if PROMPT_DATABASE_OPTIONS:
    load_default_db_properties(args)
    ok = get_YN_input("Enter advanced database configuration [y/n] (n)? ", False)
    if ok:

      database_num = str(DATABASE_INDEX + 1)
      database_num = get_validated_string_input(
        "Select database:\n1 - Postgres(Embedded)\n2 - Oracle\n[" + database_num + "]:",
        database_num,
        "^[12]$",
        "Invalid number.",
        False
      )

      DATABASE_INDEX = int(database_num) - 1
      args.database = DATABASE_NAMES[DATABASE_INDEX]
      
      if args.database != "postgres" :
        args.database_host = get_validated_string_input(
          "Hostname [" + args.database_host + "]:",
          args.database_host,
          "^[a-zA-Z0-9.\-]*$",
          "Invalid hostname.",
          False
        )
  
        args.database_port=DATABASE_PORTS[DATABASE_INDEX]
        args.database_port = get_validated_string_input(
          "Port [" + args.database_port + "]:",
          args.database_port,
          "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$",
          "Invalid port.",
          False
        )
        pass
      else:
        args.database_host = "localhost"
        args.database_port = DATABASE_PORTS[DATABASE_INDEX]
        pass

      args.database_name = get_validated_string_input(
        DATABASE_STORAGE_NAMES[DATABASE_INDEX] + " Name [" + args.database_name + "]:",
        args.database_name,
        "^[a-zA-z\-\"]+$",
        "Invalid " + DATABASE_STORAGE_NAMES[DATABASE_INDEX] + " name.",
        False
      )

      args.database_username = get_validated_string_input(
        'Username [' + args.database_username + ']: ',
        args.database_username,
        USERNAME_PATTERN,
        "Invalid characters in username. Start with _ or alpha "
        "followed by alphanumeric or _ or - characters",
        False
      )
      (masterKey, isSecure, isPersisted) = setup_master_key()
      (password, passwordAlias) = configure_database_password(isSecure,
        masterKey, True)
      args.database_password =  password
      if passwordAlias:
        setattr(args, 'database_password_alias', passwordAlias) # Store alias if present


  print_info_msg('Using database options: {database},{host},{port},{schema},{user},{password}'.format(
    database=args.database,
    host=args.database_host,
    port=args.database_port,
    schema=args.database_name,
    user=args.database_username,
    password=args.database_password
  ))


# Store set of properties for remote database connection
def store_remote_properties(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg ("Error getting ambari properties")
    return -1

  properties.process_pair(PERSISTENCE_TYPE_PROPERTY, "remote")

  properties.process_pair(JDBC_DATABASE_PROPERTY, args.database)
  properties.process_pair(JDBC_HOSTNAME_PROPERTY, args.database_host)
  properties.process_pair(JDBC_PORT_PROPERTY, args.database_port)
  properties.process_pair(JDBC_SCHEMA_PROPERTY, args.database_name)

  properties.process_pair(JDBC_DRIVER_PROPERTY, DATABASE_DRIVER_NAMES[DATABASE_INDEX])
  # fully qualify the hostname to make sure all the other hosts can connect
  # to the jdbc hostname since its passed onto the agents for RCA
  jdbc_hostname = args.database_host
  if (args.database_host == "localhost"):
    jdbc_hostname = socket.getfqdn();
    
  properties.process_pair(JDBC_URL_PROPERTY, DATABASE_CONNECTION_STRINGS[DATABASE_INDEX].format(jdbc_hostname, args.database_port, args.database_name))
  properties.process_pair(JDBC_USER_NAME_PROPERTY, args.database_username)
  if not args.database_password_alias:
    properties.process_pair(JDBC_PASSWORD_PROPERTY,
      store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))
  else:
    properties.process_pair(JDBC_PASSWORD_PROPERTY, args.database_password_alias)

  properties.process_pair(JDBC_RCA_DRIVER_PROPERTY, DATABASE_DRIVER_NAMES[DATABASE_INDEX])
  properties.process_pair(JDBC_RCA_URL_PROPERTY, DATABASE_CONNECTION_STRINGS[DATABASE_INDEX].format(jdbc_hostname, args.database_port, args.database_name))
  properties.process_pair(JDBC_RCA_USER_NAME_PROPERTY, args.database_username)
  if not args.database_password_alias:
    properties.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY,
      store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))
  else:
    properties.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY,
      args.database_password_alias)


  conf_file = properties.fileName

  try:
    properties.store(open(conf_file, "w"))
  except Exception, e:
    print 'Could not write ambari config file "%s": %s' % (conf_file, e)
    return -1

  return 0


# Initialize remote database schema
def setup_remote_db(args):

  not_found_msg = "Cannot find {0} {1} client in the path to load the Ambari Server schema.\
 Before starting Ambari Server, you must run the following DDL against the database to create \
the schema ".format(DATABASE_NAMES[DATABASE_INDEX], str(DATABASE_CLI_TOOLS_DESC[DATABASE_INDEX]))
  client_usage_cmd = DATABASE_CLI_TOOLS_USAGE[DATABASE_INDEX].format(DATABASE_INIT_SCRIPTS[DATABASE_INDEX], args.database_username,
                                                     args.database_password, args.database_name)

  retcode, out, err = execute_remote_script(args, DATABASE_INIT_SCRIPTS[DATABASE_INDEX])
  if retcode != 0:
    if retcode == -1:
      print_warning_msg(not_found_msg + os.linesep + client_usage_cmd)
      if not SILENT:
        raw_input(PRESS_ENTER_MSG)
      return retcode

    print err
    print_error_msg('Database bootstrap failed. Please, provide correct connection properties.')
    return retcode

  return 0

# Get database client executable path
def get_db_cli_tool(args):
  for tool in DATABASE_CLI_TOOLS[DATABASE_INDEX]:
    cmd =CHECK_COMMAND_EXIST_CMD.format(tool)
    ret, out, err = run_in_shell(cmd)
    if ret == 0:
      return get_exec_path(tool)

  return None


#execute SQL script on remote database
def execute_remote_script(args, scriptPath):
  tool = get_db_cli_tool(args)
  if not tool:
    args.warnings.append('{0} not found. Please, run DDL script manually'.format(DATABASE_CLI_TOOLS[DATABASE_INDEX]))
    if VERBOSE:
      print_warning_msg('{0} not found'.format(DATABASE_CLI_TOOLS[DATABASE_INDEX]))
    return -1, "Client wasn't found", "Client wasn't found"

  if args.database == "postgres":

    os.environ["PGPASSWORD"] = args.database_password
    retcode, out, err = run_in_shell('{0} {1}'.format(tool,  POSTGRES_EXEC_ARGS.format(
      args.database_host,
      args.database_port,
      args.database_name,
      args.database_username,
      scriptPath
    )))
    return retcode, out, err
  elif args.database == "oracle":
    retcode, out, err = run_in_shell('{0} {1}'.format(tool, ORACLE_EXEC_ARGS.format(
      args.database_username,
      args.database_password,
      args.database_host,
      args.database_port,
      args.database_name,
      scriptPath
    )))
    return retcode, out, err
  elif args.database=="mysql":
    retcode, out, err = run_in_shell('{0} {1}'.format(tool, MYSQL_EXEC_ARGS.format(
      args.database_host,
      args.database_port,
      args.database_username,
      args.database_password,
      args.database_name,
      scriptPath
    )))
    return retcode, out, err

  return -2, "Wrong database", "Wrong database"


def configure_database_password(isSecure=False, masterKey=None, showDefault=True):
  passwordDefault = PG_DEFAULT_PASSWORD
  if showDefault:
    passwordPrompt = 'Enter Database Password [' + passwordDefault + ']: '
  else:
    passwordPrompt = 'Enter Database Password: '
  passwordPattern = "^[a-zA-Z0-9_-]*$"
  passwordDescr = "Invalid characters in password. Use only alphanumeric or "\
                  "_ or - characters"

  password = read_password(passwordDefault, passwordPattern, passwordPrompt,
    passwordDescr)

  aliasStr = None
  if isSecure:
    retCode = save_passwd_for_alias(JDBC_RCA_PASSWORD_ALIAS, password, masterKey)
    if retCode != 0:
      print 'Saving secure database password failed.'
      return password, aliasStr
    return password, get_alias_string(JDBC_RCA_PASSWORD_ALIAS)

  return password, aliasStr


def configure_database_username_password(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg ("Error getting ambari properties")
    return -1

  username = properties[JDBC_USER_NAME_PROPERTY]
  passwordProp = properties[JDBC_PASSWORD_PROPERTY]
  isPersisted = get_master_key_ispersisted(properties)

  if username and passwordProp:
    print_info_msg("Database username + password already configured")
    args.database_username=username
    if is_alias_string(passwordProp):
      if isPersisted:
        # No need to prompt for key
        args.postgres_password = read_passwd_for_alias(JDBC_RCA_PASSWORD_ALIAS)
      else:
        (masterKey, isSecure, isPersisted) = setup_master_key()
        args.postgres_password = read_passwd_for_alias(
          JDBC_RCA_PASSWORD_ALIAS, masterKey)
    else:
      # User might want to encrypt this time around
      (masterKey, isSecure, isPersisted) = setup_master_key()
      if isSecure:
        (password, passwordAlias) = configure_database_password(isSecure,
          masterKey, False)
        update_properties({JDBC_PASSWORD_PROPERTY : passwordAlias})
        args.postgres_password = password
      else:
        args.postgres_password = open(passwordProp).read()

    return 1
  else:
    print_error_msg("Connection properties not set in config file.")


# Store local database connection properties
def store_local_properties(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg ("Error getting ambari properties")
    return -1

  properties.removeOldProp(JDBC_SCHEMA_PROPERTY)
  properties.removeOldProp(JDBC_HOSTNAME_PROPERTY)
  properties.removeOldProp(JDBC_DATABASE_PROPERTY)
  properties.removeOldProp(JDBC_RCA_DRIVER_PROPERTY)
  properties.removeOldProp(JDBC_RCA_URL_PROPERTY)
  properties.removeOldProp(JDBC_PORT_PROPERTY)
  properties.removeOldProp(JDBC_PORT_PROPERTY)
  properties.removeOldProp(JDBC_DRIVER_PROPERTY)
  properties.removeOldProp(JDBC_URL_PROPERTY)
  properties.removeOldProp(JDBC_DATABASE_PROPERTY)
  properties.process_pair(PERSISTENCE_TYPE_PROPERTY, "local")
  properties.process_pair(JDBC_USER_NAME_PROPERTY, args.database_username)
  if not args.database_password_alias:
    properties.process_pair(JDBC_PASSWORD_PROPERTY,
      store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))
  else:
    properties.process_pair(JDBC_PASSWORD_PROPERTY,
      args.database_password_alias)

  conf_file = properties.fileName

  try:
    properties.store(open(conf_file, "w"))
  except Exception, e:
    print 'Could not write ambari config file "%s": %s' % (conf_file, e)
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
  conf_file = find_properties_file()
  properties = Properties()

  try:
    properties.load(open(conf_file))
  except Exception, e:
    print 'Could not read ambari config file "%s": %s' % (conf_file, e)
    return -1

  args.persistence_type = properties[PERSISTENCE_TYPE_PROPERTY]

  if not args.persistence_type:
    args.persistence_type = "local"

  if args.persistence_type == 'remote':
    args.database = properties[JDBC_DATABASE_PROPERTY]
    args.database_host = properties[JDBC_HOSTNAME_PROPERTY]
    args.database_port = properties[JDBC_PORT_PROPERTY]
    args.database_name = properties[JDBC_SCHEMA_PROPERTY]
    global DATABASE_INDEX
    try:
      DATABASE_INDEX = DATABASE_NAMES.index(args.database)
    except ValueError:
      pass

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
    if os.path.exists(candidate): return os.path.abspath(candidate)
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
  percent = 0
  while True:
    chunk = u.read(blockSize)
    if not chunk:
      break
    fp.write(chunk)
    count += 1

    dlprogress(base_name, count, blockSize, totalSize)

  fp.flush()
  fp.close()



#
# Downloads the JDK
#
def download_jdk(args):
  if get_JAVA_HOME():
    return 0
  if args.java_home and os.path.exists(args.java_home):
    print_warning_msg("JAVA_HOME " + args.java_home
                    + " must be valid on ALL hosts")
    print_warning_msg("Please make sure the JCE Unlimited Strength "
                      "Jurisdiction Policy Files 6, "
                      "are downloaded on all "
                      "hosts")
    write_property(JAVA_HOME_PROPERTY, args.java_home)
    return 0

  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    raise FatalException(-1, err)

  conf_file = properties.fileName
  try:
    jdk_url = properties[JDK_URL_PROPERTY]
    resources_dir = properties[RESOURCES_DIR_PROPERTY]
  except (KeyError), e:
    err = 'Property ' + str(e) + ' is not defined at ' + conf_file
    raise FatalException(1, err)
  dest_file = resources_dir + os.sep + JDK_LOCAL_FILENAME
  if not os.path.exists(dest_file):
    print 'Downloading JDK from ' + jdk_url + ' to ' + dest_file
    try:
      size_command = JDK_DOWNLOAD_SIZE_CMD.format(jdk_url);
      #Get Header from url,to get file size then
      retcode, out, err = run_os_command(size_command)
      if out.find("Content-Length") == -1:
        err = "Request header doesn't contain Content-Length"
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
        track_jdk(JDK_LOCAL_FILENAME, jdk_url, dest_file)
        print 'Successfully downloaded JDK distribution to ' + dest_file
    except FatalException:
      raise
    except Exception, e:
      err = 'Failed to download JDK: ' + str(e)
      raise FatalException(1, err)
    downloaded_size = os.stat(dest_file).st_size
    if downloaded_size != src_size or downloaded_size < JDK_MIN_FILESIZE:
      err = 'Size of downloaded JDK distribution file is ' \
                    + str(downloaded_size) + ' bytes, it is probably \
                    damaged or incomplete'
      raise FatalException(1, err)
  else:
    print "JDK already exists using " + dest_file
  
  try:
     out, ok = install_jdk(dest_file)
     jdk_version = re.search('Creating (jdk.*)/jre', out).group(1)
  except Exception, e:
     print "Installation of JDK has failed: %s\n" % e.message
     file_exists = os.path.isfile(dest_file)
     if file_exists:
        ok = get_YN_input("JDK found at "+dest_file+". "
                    "Would you like to re-download the JDK [y/n] (y)? ", True)
        if not ok:
           err = "Unable to install JDK. Please remove JDK file found at "+ dest_file +" and re-run Ambari Server setup"
           raise FatalException(1, err)
        else:
           track_jdk(JDK_LOCAL_FILENAME, jdk_url, dest_file)
           print 'Successfully re-downloaded JDK distribution to ' + dest_file 
           try:
               out, ok = install_jdk(dest_file)
               jdk_version = re.search('Creating (jdk.*)/jre', out).group(1)
           except Exception, e:
             print "Installation of JDK was failed: %s\n" % e.message
             err = "Unable to install JDK. Please remove JDK, file found at "+ dest_file +" and re-run Ambari Server setup"
             raise FatalException(1, err)
  
     else:
         err = "Unable to install JDK. File "+ dest_file +" does not exist, please re-run Ambari Server setup"
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
      print_error_msg("Reason: {0}".format(e.reason))
    # TODO: We don't fail installation if download_jce_policy fails. Is it OK?
  return 0


def download_jce_policy(properties, accpeted_bcl):
  try:
    jce_url = properties[JCE_URL_PROPERTY]
    resources_dir = properties[RESOURCES_DIR_PROPERTY]
  except KeyError, e:
    err = 'Property ' + str(e) + ' is not defined in properties file'
    raise FatalException(1, err)
  dest_file = resources_dir + os.sep + JCE_POLICY_FILENAME
  if not os.path.exists(dest_file):
    print 'Downloading JCE Policy archive from ' + jce_url + ' to ' + dest_file
    try:
      size_command = JDK_DOWNLOAD_SIZE_CMD.format(jce_url);
      #Get Header from url,to get file size then
      retcode, out, err = run_os_command(size_command)
      if out.find("Content-Length") == -1:
        err = "Request header doesn't contain Content-Length";
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
            print 'Successfully downloaded JCE Policy archive to ' + dest_file
          else:
            raise FatalException(1, err)
        else:
          ok = get_YN_input("To download the JCE Policy archive you must "
                            "accept the license terms found at "
                            "http://www.oracle.com/technetwork/java/javase"
                            "/terms/license/index.html"
                            "Not accepting might result in failure when "
                            "setting up HDP security. \nDo you accept the "
                            "Oracle Binary Code License Agreement [y/n] (y)? ", True)
          if ok:
            retcode, out, err = run_os_command(jce_download_cmd)
            if retcode == 0:
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
    print "JCE Policy archive already exists, using " + dest_file

class RetCodeException(Exception): pass

def install_jdk(dest_file):
  ok = get_YN_input("To install the Oracle JDK you must accept the "
                    "license terms found at "
                    "http://www.oracle.com/technetwork/java/javase/"
                  "downloads/jdk-6u21-license-159167.txt. Not accepting will "
                  "cancel the Ambari Server setup.\nDo you accept the "
                  "Oracle Binary Code License Agreement [y/n] (y)? ", True)
  if not ok:
    raise FatalException(1, None)

  print "Installing JDK to {0}".format(JDK_INSTALL_DIR)
  retcode, out, err = run_os_command(CREATE_JDK_DIR_CMD)
  savedPath = os.getcwd()
  os.chdir(JDK_INSTALL_DIR)
  retcode, out, err = run_os_command(MAKE_FILE_EXECUTABLE_CMD.format(dest_file))
  retcode, out, err = run_os_command(dest_file + ' -noregister')
  os.chdir(savedPath)
  if retcode != 0:
    err = "Installation of JDK returned exit code %s" % retcode
    raise FatalException(retcode, err)
  return out, ok




#
# Configures the OS settings in ambari properties.
#
def configure_os_settings():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg ("Error getting ambari properties")
    return -1
  try:
    conf_os_type = properties[OS_TYPE_PROPERTY]
    if conf_os_type != '':
      print_info_msg ("os_type already setting in properties file")
      return 0
  except (KeyError), e:
    print_error_msg ("os_type is not set in properties file")

  os_system = platform.system()
  if os_system != 'Linux':
    print_error_msg ("Non-Linux systems are not supported")
    return -1

  os_info = platform.linux_distribution(
    None, None, None, ['SuSE', 'redhat' ], 0
  )
  os_name = os_info[0].lower()
  if os_name == 'suse':
    os_name = 'sles'
  os_version = os_info[1].split('.', 1)[0]
  master_os_type = os_name + os_version    
  write_property(OS_TYPE_PROPERTY, master_os_type)
  return 0



def get_JAVA_HOME():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg ("Error getting ambari properties")
    return None
    
  java_home = properties[JAVA_HOME_PROPERTY]
  if (not 0 == len(java_home)) and (os.path.exists(java_home)):
    return java_home

  return None

#
# Finds the available JDKs.
#
def find_jdk():
  if get_JAVA_HOME():
    return get_JAVA_HOME()
  print "Looking for available JDKs at " + JDK_INSTALL_DIR
  jdks = glob.glob(JDK_INSTALL_DIR + os.sep + "jdk*")
  jdks.sort()
  print "Found: " + str(jdks)
  count = len(jdks)
  if count == 0:
    return
  jdkPath = jdks[count - 1]
  print "Selected JDK {0}".format(jdkPath)
  return jdkPath

#
# Checks if options determine local DB configuration
#
def is_local_database(options):
  if options.database == DATABASE_NAMES[0] \
    and options.database_host == "localhost" \
    and options.database_port == DATABASE_PORTS[0] \
    and options.database_name == "ambari":
    return True
  return False

#Check if required jdbc drivers present
def find_jdbc_driver(args):
  if args.database in JDBC_PATTERNS.keys():
    drivers = []
    drivers.extend(glob.glob(JAVA_SHARE_PATH + os.sep + JDBC_PATTERNS[args.database]))
    if drivers:
      return drivers
    return -1
  return 0

def copy_files(files, dest_dir):
  if os.path.isdir(dest_dir):
    for filepath in files:
      shutil.copy(filepath, dest_dir)
    return 0
  else:
    return -1

def check_jdbc_drivers(args):
  properties = get_ambari_properties()
  
  ## ask user twice
  for i in range(0,2):
    result = find_jdbc_driver(args)
    if result == -1:
      msg = 'WARNING: Before starting Ambari Server, ' \
            'the {0} JDBC driver JAR file must be copied to {1}. Press enter to continue.'.format(
        DATABASE_FULL_NAMES[args.database],
        JAVA_SHARE_PATH
      )
      if not SILENT:
        raw_input(msg)
      else:
        print_warning_msg(msg)

    # check if user provided drivers
    result = find_jdbc_driver(args)

    if type(result) is not int:
      print 'Copying JDBC drivers to server resources...'
      try:
        resources_dir = properties[RESOURCES_DIR_PROPERTY]
      except KeyError:
        print_error_msg("There is no value for " + RESOURCES_DIR_PROPERTY + "in " + AMBARI_PROPERTIES_FILE)
        return -1
      copy_files(result, resources_dir)
      break

  return 0

#

# Setup the Ambari Server.
#
def setup(args):
  if not is_root():
    err = 'Ambari-server setup should be run with '\
                     'root-level privileges'
    raise FatalException(4, err)

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

  print 'Checking iptables...'
  retcode, out = check_iptables()
  if not retcode == 0 and out == IP_TBLS_ENABLED:
    err = 'Failed to stop iptables. Exiting.'
    raise FatalException(retcode, err)

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
    print 'Default properties detected. Using built-in database.'
    store_local_properties(args)

    print 'Checking PostgreSQL...'
    retcode = check_postgre_up()
    if not retcode == 0:
      err = 'Unable to start PostgreSQL server. Exiting'
      raise FatalException(retcode, err)

    print 'Configuring database...'
    retcode = setup_db(args)
    if not retcode == 0:
      err = 'Running database init script was failed. Exiting.'
      raise FatalException(retcode, err)

    print 'Configuring PostgreSQL...'
    retcode = configure_postgres()
    if not retcode == 0:
      err = 'Unable to configure PostgreSQL server. Exiting'
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
      err =  "The cli was not found"
      raise FatalException(retcode, err)

    if not retcode == 0:
      err = 'Error while configuring connection properties. Exiting'
      raise FatalException(retcode, err)
    check_jdbc_drivers(args)

  if args.warnings:
    print "Ambari Server 'setup' finished with warnings:"
    for warning in args.warnings:
      print warning
  else:
    print "Ambari Server 'setup' finished successfully"



#
# Resets the Ambari Server.
#
def reset(args):
  if not is_root():
    err = 'Ambari-server reset should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)
  choice = get_YN_input("**** WARNING **** You are about to reset and clear the "
                     "Ambari Server database. This will remove all cluster "
                     "host and configuration information from the database. "
                     "You will be required to re-configure the Ambari server "
                     "and re-run the cluster wizard. \n"
                     "Are you SURE you want to perform the reset "
                     "[yes/no]? ", True)
  okToRun = choice

  if not okToRun:
    err =  "Ambari Server 'reset' cancelled"
    raise FatalException(1, err)

  okToRun = get_YN_input("Confirm server reset [yes/no]? ", True)

  if not okToRun:
    err =  "Ambari Server 'reset' cancelled"
    raise FatalException(1, err)

  print "Resetting the Server database..."

  parse_properties_file(args)

  # configure_database_username_password(args)
  if args.persistence_type=="remote":
    client_desc = DATABASE_NAMES[DATABASE_INDEX] + ' ' + DATABASE_CLI_TOOLS_DESC[DATABASE_INDEX]
    client_usage_cmd_drop = DATABASE_CLI_TOOLS_USAGE[DATABASE_INDEX].format(DATABASE_DROP_SCRIPTS[DATABASE_INDEX], args.database_username,
                                                     args.database_password, args.database_name)
    client_usage_cmd_init = DATABASE_CLI_TOOLS_USAGE[DATABASE_INDEX].format(DATABASE_INIT_SCRIPTS[DATABASE_INDEX], args.database_username,
                                                     args.database_password, args.database_name)    

    if get_db_cli_tool(args) != -1:
      retcode, out, err = execute_remote_script(args, DATABASE_DROP_SCRIPTS[DATABASE_INDEX])
      if not retcode == 0:
        if retcode == -1:
          print_warning_msg('Cannot find ' + client_desc + ' client in the path to reset the Ambari Server schema. To reset Ambari Server schema ' +
          'you must run the following DDL against the database to drop the schema:' + os.linesep + client_usage_cmd_drop + os.linesep +
          ', then you must run the following DDL against the database to create the schema ' + os.linesep + client_usage_cmd_init + os.linesep )
        raise FatalException(retcode, err)

      retcode, out, err = execute_remote_script(args, DATABASE_INIT_SCRIPTS[DATABASE_INDEX])
      if not retcode == 0:
        if retcode == -1:
          print_warning_msg('Cannot find ' + client_desc + ' client in the path to reset the Ambari Server schema. To reset Ambari Server schema ' +
          'you must run the following DDL against the database to drop the schema:' + os.linesep + client_usage_cmd_drop + os.linesep +
          ', then you must run the following DDL against the database to create the schema ' + os.linesep + client_usage_cmd_init + os.linesep )
        raise FatalException(retcode, err)

    else:
      err = 'Cannot find ' + client_desc + ' client in the path to reset the Ambari Server schema. To reset Ambari Server schema ' + \
      'you must run the following DDL against the database to drop the schema:' + os.linesep + client_usage_cmd_drop + os.linesep +   \
      ', then you must run the following DDL against the database to create the schema ' + os.linesep + client_usage_cmd_init + os.linesep
      raise FatalException(-1, err)

  else:
    dbname = args.database_name
    filename = args.drop_script_file
    username = args.database_username
    password = args.database_password
    command = SETUP_DB_CMD[:]
    command[-1] = command[-1].format(filename, username, password)
    retcode, outdata, errdata = run_os_command(command)
    if not retcode == 0:
      raise FatalException(1, errdata)

    print_info_msg ("About to run database setup")
    setup_db(args)

  print "Ambari Server 'reset' complete"



#
# Starts the Ambari Server.
#
def start(args):
  parse_properties_file(args)
  if os.path.exists(PID_DIR + os.sep + PID_NAME):
    f = open(PID_DIR + os.sep + PID_NAME, "r")
    pid = int(f.readline())
    f.close()
    try:
      os.kill(pid, 0)
      err = "Server is already running."
      raise FatalException(1, err)
    except OSError as e:
      print_info_msg("Server is not running...")

  current_user = getpass.getuser()
  ambari_user = read_ambari_user()
  if ambari_user is None:
    err = "Can not detect a system user for Ambari. " \
                    "Please run \"setup\" command to create such user "
    raise FatalException(1, err)
  if current_user != ambari_user and not is_root():
    err = "Can not start ambari-server as user {0}. Please either run \"start\" " \
          "command as root or as user {1}".format(current_user, ambari_user)
    raise FatalException(1, err)

  conf_dir = get_conf_dir()
  jdk_path = find_jdk()
  if jdk_path is None:
    err = "No JDK found, please run the \"setup\" " \
                    "command to install a JDK automatically or install any " \
                    "JDK manually to " + JDK_INSTALL_DIR
    raise FatalException(1, err)

  # Preparations

  if is_root():
    print "Have root privileges."

    if args.persistence_type == "local":
      retcode = check_postgre_up()
      if not retcode == 0:
        err = "Unable to start PostgreSQL server. Exiting"
        raise FatalException(retcode, err)

    print 'Checking iptables...'
    retcode, out = check_iptables()
    if not retcode == 0 and out == IP_TBLS_ENABLED:
      err = "Failed to stop iptables. Exiting"
      raise FatalException(retcode, err)
  else: # Skipping actions that require root permissions
    print "Can not check iptables status when starting "\
      "without root privileges."
    print "Please don't forget to disable or adjust iptables if needed"
    if args.persistence_type == "local":
      print "Can not check PostgreSQL server status when starting " \
            "without root privileges."
      print "Please don't forget to start PostgreSQL server."


  properties = get_ambari_properties()
  persist = get_master_key_ispersisted(properties)
  environ = os.environ.copy()
  # Need to handle master key not persisted scenario
  if persist is not None and not persist:
    prompt = False
    masterKey = environ[SECURITY_KEY_ENV_VAR_NAME]
    if masterKey is not None and masterKey != "":
      pass
    else:
      keyLocation = environ[SECURITY_MASTER_KEY_LOCATION]
      if keyLocation is not None:
        try:
          # Verify master key can be read by the java process
          with open(keyLocation, 'r') : pass
        except IOError:
          print_warning_msg("Cannot read Master key from path specified in "
                            "environemnt.")
          prompt = True

    if prompt:
      masterKey = get_validated_string_input("Please provide master key " +\
                    "for unlocking credential store: ", "", ".*", "", False)
      tempDir = tempfile.gettempdir()
      tempFilePath = tempDir + os.sep + "ambari.passwd"
      with open(tempFilePath, 'w+') as file:
        file.write(masterKey)
      os.chmod(tempFilePath, stat.S_IREAD | stat.S_IWRITE)

      if tempFilePath is not None:
        environ[SECURITY_MASTER_KEY_LOCATION] = tempFilePath


  pidfile = PID_DIR + os.sep + PID_NAME
  command_base = SERVER_START_CMD_DEBUG if (SERVER_DEBUG_MODE or SERVER_START_DEBUG) else SERVER_START_CMD
  command = command_base.format(jdk_path, conf_dir, get_ambari_classpath(), pidfile)
  if is_root() and ambari_user != "root":
    # To inherit exported environment variables (especially AMBARI_PASSPHRASE),
    # from subprocess, we have to skip --login option of su command. That's why
    # we change dir to / (otherwise subprocess can face with 'permission denied'
    # errors while trying to list current directory
    os.chdir("/")
    param_list = ["/bin/su", ambari_user, "-s", "/bin/sh", "-c", command]
  else:
    param_list = ["/bin/sh", "-c", command]
  print "Running server: " + str(param_list)
  server_process = subprocess.Popen(param_list, env=environ)
  print "done."


#
# Stops the Ambari Server.
#
def stop(args):
  if os.path.exists(PID_DIR + os.sep + PID_NAME):
    f = open(PID_DIR + os.sep + PID_NAME, "r")
    pid = int(f.readline())
    try:
      os.killpg(os.getpgid(pid), signal.SIGKILL)
    except OSError, e:
      print_info_msg( "Unable to stop Ambari Server - " + str(e) )
      return
    f.close()
    os.remove(f.name)
    print "Ambari Server stopped"
  else:
    print "Ambari Server is not running"



### Stack upgrade ###


def upgrade_stack(args, stack_id):
  if not is_root():
    err = 'Ambari-server upgradestack should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)
  #password access to ambari-server and mapred
  configure_database_username_password(args)
  dbname = args.database_name
  file = args.upgrade_stack_script_file
  stack_name, stack_version = stack_id.split(STACK_NAME_VER_SEP)
  command = UPGRADE_STACK_CMD[:]
  command[-1] = command[-1].format(file, stack_name, stack_version)
  retcode, outdata, errdata = run_os_command(command)
  if not retcode == 0:
    raise FatalException(retcode, errdata)
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

  parse_properties_file(args)
  if args.persistence_type == "remote":
    pass
  else:
    print 'Checking PostgreSQL...'
    retcode = check_postgre_up()
    if not retcode == 0:
      err = 'PostgreSQL server not running. Exiting'
      raise FatalException(retcode, err)

    file = args.upgrade_script_file
    print 'Upgrading database...'
    retcode = execute_db_script(args, file)
    if not retcode == 0:
      err = 'Database upgrade script has failed. Exiting.'
      raise FatalException(retcode, err)


    print 'Checking database integrity...'
    check_file = file[:-3] + "Check" + file[-4:]
    retcode = check_db_consistency(args, check_file)

    if not retcode == 0:
      print 'Found inconsistency. Trying to fix...'
      fix_file = file[:-3] + "Fix" + file[-4:]
      retcode = execute_db_script(args, fix_file)

      if not retcode == 0:
        err = 'Database cannot be fixed. Exiting.'
        raise FatalException(retcode, err)
    else:
      print 'Database is consistent.'

  user = read_ambari_user()
  if user is None:
    warn = 'Can not determine custom ambari user. Please run ' \
           '"ambari-server setup" before starting server'
    print_warning_msg(warn)
  else:
    adjust_directory_permissions(user)
  print "Ambari Server 'upgrade' finished successfully"


#
# The Ambari Server status.
#
def status(args):
  if os.path.exists(PID_DIR + os.sep + PID_NAME):
    f = open(PID_DIR + os.sep + PID_NAME, "r")
    pid = int(f.readline())
    print "Found Ambari Server PID: '" + str(pid) + "'"
    f.close()
    retcode, out, err = run_os_command("ps -p " + str(pid))
    if retcode == 0:
      print "Ambari Server running"
      print "Ambari Server PID at: " + PID_DIR + os.sep + PID_NAME
    else:
      print "Ambari Server not running. Stale PID File at: " + PID_DIR + os.sep + PID_NAME
  else:
    print "Ambari Server not running"



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
def get_YN_input(prompt,default):
  yes = set(['yes','ye', 'y'])
  no = set(['no','n'])
  return get_choice_string_input(prompt,default,yes,no)



def get_choice_string_input(prompt,default,firstChoice,secondChoice):
  if SILENT:
    print(prompt)
    return default
  choice = raw_input(prompt).lower()
  if choice in firstChoice:
    return True
  elif choice in secondChoice:
    return False
  elif choice is "": # Just enter pressed
    return default
  else:
    print "input not recognized, please try again: "
    return get_choice_string_input(prompt,default,firstChoice,secondChoice)



def get_validated_string_input(prompt, default, pattern, description, is_pass):
  input =""
  while not input:
    if SILENT:
      print (prompt)
      input = default
    elif is_pass:
      input = getpass.getpass(prompt)
    else:
      input = raw_input(prompt)
    if not input.strip():
      input = default
      break #done here and picking up default
    else:
      if not re.search(pattern,input.strip()):
        print description
        input=""
  return input


def get_value_from_properties(properties, key):
  try:
    value = properties[key]
  except KeyError:
    return ""
  return value

def setup_ldap():
  properties = get_ambari_properties()

  # Setup secure key
  (masterKey, isSecure, isPersisted) = setup_master_key(False)

  LDAP_PRIMARY_URL_DEFAULT = get_value_from_properties(properties,
    "authentication.ldap.primaryUrl")
  LDAP_SECONDARY_URL_DEFAULT = get_value_from_properties(properties,
    "authentication.ldap.secondaryUrl")
  LDAP_BASE_DN_DEFAULT = get_value_from_properties(properties,
    "authentication.ldap.baseDn")
  LDAP_BIND_DEFAULT = get_value_from_properties(properties,
    "authentication.ldap.bindAnonymously")
  LDAP_USER_ATT_DEFAULT = get_value_from_properties(properties,
    "authentication.ldap.usernameAttribute")
  LDAP_GROUP_BASE_DEFAULT = get_value_from_properties(properties,
    "authorization.ldap.groupBase")
  LDAP_GROUP_OBJ_DEFAULT = get_value_from_properties(properties,
    "authorization.ldap.groupObjectClass")
  LDAP_GROUP_NAME_DEFAULT = get_value_from_properties(properties,
    "authorization.ldap.groupNamingAttr")
  LDAP_GROUP_MEM_DEFAULT = get_value_from_properties(properties,
    "authorization.ldap.groupMembershipAttr")
  LDAP_GROUP_MAP_DEFAULT = get_value_from_properties(properties,
    "authorization.ldap.adminGroupMappingRules")
  LDAP_GROUP_SEARCH_DEFAULT = get_value_from_properties(properties,
    "authorization.ldap.groupSearchFilter")
  LDAP_USER_ROLE_DEFAULT = get_value_from_properties(properties,
    "authorization.userRoleName")
  LDAP_ADMIN_ROLE_DEFAULT = get_value_from_properties(properties,
    "authorization.adminRoleName")
  LDAP_MGR_DN_DEFAULT = get_value_from_properties(properties,
    "authentication.ldap.managerDn")

  ldap_properties_map =\
  {
    "authentication.ldap.primaryUrl":(LDAP_PRIMARY_URL_DEFAULT, "Primary URL: "),\
    "authentication.ldap.secondaryUrl":(LDAP_SECONDARY_URL_DEFAULT, "Secondary URL: "),\
    "authentication.ldap.baseDn":(LDAP_BASE_DN_DEFAULT, "Base DN: "),\
    "authentication.ldap.bindAnonymously":(LDAP_BIND_DEFAULT, "Bind anonymously? [true/alse]?: "),\
    "authentication.ldap.usernameAttribute":(LDAP_USER_ATT_DEFAULT, "User name attribute uid): "),\
    "authorization.ldap.groupBase":(LDAP_GROUP_BASE_DEFAULT, "Group base ou=groups,dc=ambari): "),\
    "authorization.ldap.groupObjectClass":(LDAP_GROUP_OBJ_DEFAULT, "Group object class group): "),\
    "authorization.ldap.groupNamingAttr":(LDAP_GROUP_NAME_DEFAULT, "Group name attribute cn): "),\
    "authorization.ldap.groupMembershipAttr":(LDAP_GROUP_MEM_DEFAULT, "Group membership ttribute (member): "),\
    "authorization.ldap.adminGroupMappingRules":(LDAP_GROUP_MAP_DEFAULT, "Admin group apping rules: "),\
    "authorization.ldap.groupSearchFilter":(LDAP_GROUP_SEARCH_DEFAULT, "Group search filter: "),\
    "authorization.userRoleName":(LDAP_USER_ROLE_DEFAULT, "User role name (user): "),\
    "authorization.adminRoleName":(LDAP_ADMIN_ROLE_DEFAULT, "Admin role name (admin): "),
    "authentication.ldap.managerDn":(LDAP_MGR_DN_DEFAULT, "Manager DN: ")
  }
  print "Input LDAP properties. Hit [Enter] to skip property."
  ldap_property_value_map = {}
  for key in ldap_properties_map.keys():
    input = get_validated_string_input(ldap_properties_map[key][1],
      ldap_properties_map[key][0], ".*", "", False)
    if input is not None and input != "":
      ldap_property_value_map[key] = input

  ldap_property_value_map[LDAP_MGR_PASSWORD_PROPERTY] =\
  configure_ldap_password(isSecure, masterKey)
  # Persisting values
  update_properties(ldap_property_value_map)
  return 0

def reset_master_key():
  setup_master_key(resetKey=True)

def setup_master_key(resetKey=False):
  properties = get_ambari_properties()
  passwordPattern = "^[a-zA-Z0-9_-]*$"
  passwordDescr = "Invalid characters in password. Use only alphanumeric or "\
                  "_ or - characters"
  passwordDefault = "hadooprocks!"
  # check configuration for location of master key
  keyLocation = get_master_key_location(properties)
  persist = get_master_key_ispersisted(properties)
  masterKeyFile = search_file(SECURITY_MASTER_KEY_FILENAME, keyLocation)

  if persist is not None:
    if persist and masterKeyFile is not None and not resetKey:
      return None, True, True       # setup is secure and key persisted
    elif not persist and not resetKey:
      masterKey = get_validated_string_input("Please provide master key " +\
                                             "for unlocking credential store: ", "", ".*", "", False)
      return masterKey, True, False # return master key for saving passwords
  else:
    if masterKeyFile is not None:
      print_info_msg("Master key file exists. Updating property...")
      update_properties({SECURITY_KEY_IS_PERSISTED : True})
      return None, True, True

  enable_ok = True
  if not resetKey:
    enable_ok = get_YN_input("Do you want encryption enabled for saving " +\
                             "passwords [y/n] (n)? ", False)
  if not enable_ok:
    return None, False, None

  key = None
  if masterKeyFile is None or resetKey:
    key = get_validated_string_input(
      "Please provide master key for the credential store: ",
      passwordDefault, passwordPattern, passwordDescr, True)
    if key != passwordDefault:
      key = get_validated_string_input("Please re-enter master key: ",
        passwordDefault, passwordPattern, passwordDescr, True)

    persist = get_YN_input("Do you want to persist master key. If you choose "\
                           "not to persist, you need to provide the master "\
                           "key while starting the ambari server as a env "\
                           "variable named " + SECURITY_KEY_ENV_VAR_NAME +\
                           " or the start will prompt for the master key."
                           " Persist [y/n] (y)? ", True)
    if persist:
      save_master_key(key, keyLocation + os.sep + SECURITY_MASTER_KEY_FILENAME,
        persist)
    elif not persist and masterKeyFile is not None:
      try:
        #os.remove(masterKeyFile)
        print_warning_msg("Master key exists although security " +\
                          "is disabled. location: " + str(masterKeyFile))
      except Exception, e:
        print 'Could not remove master key file. %s' % e

  if persist is not None:
    update_properties({SECURITY_KEY_IS_PERSISTED : persist})

  if resetKey:
    # Encrypt the passwords with new key
    try:
      db_password_alias = args.database_password
      ldap_password_alias = args.ldap_mgr_password
    except (KeyError), e:
      print_warning_msg("KeyError: " + str(e))

    if db_password_alias is not None and is_alias_string(db_password_alias):
      configure_database_password(True, False)

    if ldap_password_alias is not None and is_alias_string(ldap_password_alias):
      configure_ldap_password(True)

  return key, True, persist

def get_master_key_location(properties):
  keyLocation = properties[SECURITY_MASTER_KEY_LOCATION]
  if keyLocation is None or keyLocation == "":
    keyLocation = properties[SECURITY_KEYS_DIR]
  return keyLocation

def get_master_key_ispersisted(properties):
  try:
    isPersisted = properties[SECURITY_KEY_IS_PERSISTED]
  except (KeyError), e:
    return None

  if isPersisted is not None and isPersisted != "":
    return isPersisted == 'true' or isPersisted == 'TRUE' or\
           isPersisted == 'True'

  return None


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

    if masterKey is None:
      masterKey = "None"

    command = SECURITY_PROVIDER_GET_CMD.format(jdk_path,
      get_conf_dir(), get_ambari_classpath(), alias, tempFilePath, masterKey)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider get passwd: " +
                   str(retcode))
    if retcode != 0:
      print 'Unable to read password from store. alias = ' + alias
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

    if masterKey is None:
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


def configure_ldap_password(isSecure=False, masterKey=None):
  passwordDefault = ""
  passwordPrompt = 'Enter LDAP Password: '
  passwordPattern = ".*"
  passwordDescr = "Invalid characters in password."

  password = read_password(passwordDefault, passwordPattern, passwordPrompt,
    passwordDescr)

  if isSecure:
    retCode = save_passwd_for_alias(LDAP_MGR_PASSWORD_ALIAS, password, masterKey)
    if retCode != 0:
      print 'Saving secure ldap password failed.'
      return password
    return get_alias_string(LDAP_MGR_PASSWORD_ALIAS)

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
      print_error_msg ('Could not read "%s": %s' % (conf_file, e))
      return -1

    #for key in propertyMap.keys():
      #properties[key] = propertyMap[key]
    for key in propertyMap.keys():
      properties.removeOldProp(key)
      properties.process_pair(key, str(propertyMap[key]))

    with open(conf_file, 'w') as file:
      properties.store(file)

  return 0


#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] action [stack_id]",)

  parser.add_option('-f', '--init-script-file',
                      default='/var/lib/ambari-server/'
                              'resources/Ambari-DDL-Postgres-CREATE.sql',
                      help="File with setup script")
  parser.add_option('-r', '--drop-script-file', default="/var/lib/"
                              "ambari-server/resources/"
                              "Ambari-DDL-Postgres-DROP.sql",
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

  parser.add_option('--database', default=None, help ="Database to use postgres|oracle", dest="database")
  parser.add_option('--databasehost', default=None, help="Hostname of database server", dest="database_host")
  parser.add_option('--databaseport', default=None, help="Database port", dest="database_port")
  parser.add_option('--databasename', default=None, help="Database/Schema/Service name", dest="database_name")
  parser.add_option('--databaseusername', default=None, help="Database user login", dest="database_username")
  parser.add_option('--databasepassword', default=None, help="Database user password", dest="database_password")

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

  global DATABASE_INDEX
  global PROMPT_DATABASE_OPTIONS
  #perform checks

  options.warnings = []

  if options.database is None \
    and options.database_host is None \
    and options.database_port is None \
    and options.database_name is None \
    and options.database_username is None \
    and options.database_password is None:

    PROMPT_DATABASE_OPTIONS = True

  elif not (options.database is not None
    and options.database_host is not None
    and options.database_port is not None
    and options.database_name is not None
    and options.database_username is not None
    and options.database_password is not None):
    parser.error('All database options should be set. Please see help for the options.')

  #correct database
  if options.database is not None and options.database not in DATABASE_NAMES:
    parser.print_help()
    print "Unsupported Database " + options.database
  elif options.database is not None:
    options.database = options.database.lower()
    DATABASE_INDEX = DATABASE_NAMES.index(options.database)

  #correct port
  if options.database_port is not None:
    correct=False
    try:
      port = int(options.database_port)
      if 65536 > port > 0:
        correct = True
    except ValueError:
      pass
    if not correct:
      parser.print_help()
      parser.error("Incorrect database port " + options.database_port)

  if options.database is not None and options.database == "postgres":
    print "WARNING: HostName for postgres server " + options.database_host + \
     " will be ignored: using localhost."
    options.database_host = "localhost"

  if len(args) == 0:
    print parser.print_help()
    parser.error("No action entered")

  action = args[0]

  if action == UPGRADE_STACK_ACTION:
    args_number_required = 2
  else:
    args_number_required = 1

  if len(args) < args_number_required:
    print parser.print_help()
    parser.error("Invalid number of arguments. Entered: " + str(len(args)) + ", required: " + str(args_number_required))
 
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
      upgrade_stack(options, stack_id)
    elif action == LDAP_SETUP_ACTION:
      setup_ldap()
    elif action == RESET_MASTER_KEY_ACTION:
      reset_master_key()
    else:
      parser.error("Invalid action")
  except FatalException as e:
    if e.reason is not None:
      print_error_msg("Exiting with exit code {0}. Reason: {1}".format(e.code, e.reason))
    sys.exit(e.code)



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
      if not line: continue
      if line[0] == '#': continue
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
    self._props[key] = value.strip()
    if self._keymap.has_key(key):
      oldkey = self._keymap.get(key)
      self._origprops[oldkey] = oldvalue.strip()
    else:
      self._origprops[oldkey] = oldvalue.strip()
      self._keymap[key] = oldkey

  
  def unescape(self, value):
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
    except IOError, e:
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
    with the optional 'header' """
    if out.mode[0] != 'w':
      raise ValueError,'Steam should be opened in write mode!'
    try:
      out.write(''.join(('#', ASF_LICENSE_HEADER, '\n')))
      out.write(''.join(('#',header,'\n')))
      # Write timestamp
      tstamp = time.strftime('%a %b %d %H:%M:%S %Z %Y', time.localtime())
      out.write(''.join(('#',tstamp,'\n')))
      # Write properties from the pristine dictionary
      for prop, val in self._origprops.items():
        if val is not None:
          out.write(''.join((prop,'=',val,'\n')))
      out.close()
    except IOError, e:
      raise

if __name__ == "__main__":
  main()
