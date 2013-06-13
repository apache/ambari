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
                 ">/var/log/ambari-server/ambari-server.out 2>&1"
SERVER_START_CMD_DEBUG="{0}" + os.sep + "bin" + os.sep +\
                       "java -server -XX:NewRatio=2 -XX:+UseConcMarkSweepGC " +\
                       ambari_provider_module_option +\
                       os.getenv('AMBARI_JVM_ARGS','-Xms512m -Xmx2048m') +\
                       " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,"\
                       "server=y,suspend=n -cp {1}"+ os.pathsep + "{2}" +\
                       " org.apache.ambari.server.controller.AmbariServer"

AMBARI_CONF_VAR="AMBARI_CONF_DIR"
AMBARI_SERVER_LIB="AMBARI_SERVER_LIB"
JAVA_HOME="JAVA_HOME"
PID_DIR="/var/run/ambari-server"
PID_NAME="ambari-server.pid"
AMBARI_PROPERTIES_FILE="ambari.properties"
AMBARI_PROPERTIES_RPMSAVE_FILE="ambari.properties.rpmsave"
RESOURCES_DIR_KEY="resources.dir"

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
JDBC_PASSWORD_FILE_PROPERTY = "server.jdbc.user.passwd"
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
    print err
    sys.exit(retcode)



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



# todo: check if the scheme is already exist

def write_property(key, value):
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  with open(conf_file, 'a') as ambariConf:
    ambariConf.write(key + "=" + value)
    ambariConf.write("\n")
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
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  passFilePath = os.path.join(os.path.dirname(conf_file),
    filename)

  with open(passFilePath, 'w+') as passFile:
    passFile.write(password)
    pass
  os.chmod(passFilePath, stat.S_IREAD | stat.S_IWRITE)

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


def upgrade_stack(args, stack_id):
  #password access to ambari-server and mapred
  configure_database_username_password(args)
  dbname = args.database_name
  file = args.upgrade_stack_script_file
  stack_name, stack_version = stack_id.split(STACK_NAME_VER_SEP)
  command = UPGRADE_STACK_CMD[:]
  command[-1] = command[-1].format(file, stack_name, stack_version)
  retcode, outdata, errdata = run_os_command(command)
  if not retcode == 0:
    print errdata
  return retcode

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
      se_mode = ''
      try:
        se_mode = re.search('(enforcing|permissive)', out).group(0)
      except AttributeError:
        print_error_msg("Error determining SELinux mode. Exiting.")
        sys.exit(0)
      print "SELinux mode is '" + se_mode + "'"
      if se_mode == SE_MODE_ENFORCING:
        print "Temporarily disabling SELinux"
        run_os_command(SE_SETENFORCE_CMD)
      print_warning_msg(
        "SELinux is set to 'permissive' mode and temporarily disabled."
        " You should disable SELinux permanently.")
      ok = get_YN_input("OK to continue [y/n] (y)? ", True)
      if ok == False:
        sys.exit(0)
      return 0
  except OSError:
    print_warning_msg("Could not run {0}: OK".format(GET_SE_LINUX_ST_CMD))
  return 0



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


def get_conf_dir():
  try:
    conf_dir = os.environ[AMBARI_CONF_VAR]
    return conf_dir
  except KeyError:
    default_conf_dir = "/etc/ambari-server/conf"
    print_info_msg(AMBARI_CONF_VAR + " is not set, using default "
                 + default_conf_dir)
    return default_conf_dir



def search_file(filename, search_path, pathsep=os.pathsep):
  """ Given a search path, find file with requested name """
  for path in string.split(search_path, pathsep):
    candidate = os.path.join(path, filename)
    if os.path.exists(candidate): return os.path.abspath(candidate)
  return None



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
                      "are dwonloaded on all "
                      "hosts")
    write_property(JAVA_HOME_PROPERTY, args.java_home)
    return 0

  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg ("Error getting ambari properties")
    return -1
  conf_file = properties.fileName
  try:
    jdk_url = properties['jdk.url']
    resources_dir = properties['resources.dir']  
  except (KeyError), e:
    print 'Property ' + str(e) + ' is not defined at ' + conf_file
    return -1
  dest_file = resources_dir + os.sep + JDK_LOCAL_FILENAME
  if not os.path.exists(dest_file):
    print 'Downloading JDK from ' + jdk_url + ' to ' + dest_file
    try:
      size_command = JDK_DOWNLOAD_SIZE_CMD.format(jdk_url);
      #Get Header from url,to get file size then
      retcode, out, err = run_os_command(size_command)
      if out.find("Content-Length") == -1:
        print "Request header doesn't contain Content-Length";
        return -1
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
    except Exception, e:
      print_error_msg('Failed to download JDK: ' + str(e))
      return -1
    downloaded_size = os.stat(dest_file).st_size
    if downloaded_size != src_size or downloaded_size < JDK_MIN_FILESIZE:
      print_error_msg('Size of downloaded JDK distribution file is '
                    + str(downloaded_size) + ' bytes, it is probably \
                    damaged or incomplete')
      return -1
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
        if (ok == False):
           print "Unable to install JDK. Please remove JDK file found at "+ dest_file +" and re-run Ambari Server setup" 
           return -1
        else:
           track_jdk(JDK_LOCAL_FILENAME, jdk_url, dest_file)
           print 'Successfully re-downloaded JDK distribution to ' + dest_file 
           try:
               out, ok = install_jdk(dest_file)
               jdk_version = re.search('Creating (jdk.*)/jre', out).group(1)
           except Exception, e:
               print "Installation of JDK was failed: %s\n" % e.message
               print "Unable to install JDK. Please remove JDK, file found at "+ dest_file +" and re-run Ambari Server setup" 
               return -1              
  
     else:
         print "Unable to install JDK. File "+ dest_file +"does not exist, please re-run Ambari Server setup"
         return -1
  
  print "Successfully installed JDK to {0}/{1}".\
      format(JDK_INSTALL_DIR, jdk_version)
  write_property(JAVA_HOME_PROPERTY, "{0}/{1}".
      format(JDK_INSTALL_DIR, jdk_version))
  jce_download = download_jce_policy(properties, ok)
  if (jce_download == -1):
    print "JCE Policy files are required for secure HDP setup. Please ensure " \
          " all hosts have the JCE unlimited strength policy 6, files."
  return 0

def download_jce_policy(properties, accpeted_bcl):
  try:
    jce_url = properties['jce_policy.url']
    resources_dir = properties['resources.dir']
  except (KeyError), e:
    print 'Property ' + str(e) + ' is not defined in properties file'
    return -1
  dest_file = resources_dir + os.sep + JCE_POLICY_FILENAME
  if not os.path.exists(dest_file):
    print 'Downloading JCE Policy archive from ' + jce_url + ' to ' + dest_file
    try:
      size_command = JDK_DOWNLOAD_SIZE_CMD.format(jce_url);
      #Get Header from url,to get file size then
      retcode, out, err = run_os_command(size_command)
      if out.find("Content-Length") == -1:
        print "Request header doesn't contain Content-Length";
        return -1
      start_with = int(out.find("Content-Length") + len("Content-Length") + 2)
      end_with = out.find("\r\n", start_with)
      src_size = int(out[start_with:end_with])
      print_info_msg('JCE zip distribution size is ' + str(src_size) + 'bytes')
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
        if (accpeted_bcl == True):
          retcode, out, err = run_os_command(jce_download_cmd)
          if retcode == 0:
            print 'Successfully downloaded JCE Policy archive to ' + dest_file
          else:
            return -1
        else:
          ok = get_YN_input("To download the JCE Policy archive you must "
                            "accept the license terms found at "
                            "http://www.oracle.com/technetwork/java/javase"
                            "/terms/license/index.html"
                            "Not accepting might result in failure when "
                            "setting up HDP security. \nDo you accept the "
                            "Oracle Binary Code License Agreement [y/n] (y)? ", True)
          if (ok == True):
            retcode, out, err = run_os_command(jce_download_cmd)
            if retcode == 0:
              print 'Successfully downloaded JCE Policy archive to ' + dest_file
          else:
            return -1
    except Exception, e:
      print_error_msg('Failed to download JCE Policy archive: ' + str(e))
      return -1
    downloaded_size = os.stat(dest_file).st_size
    if downloaded_size != src_size or downloaded_size < JCE_MIN_FILESIZE:
      print_error_msg('Size of downloaded JCE Policy archive is '
                      + str(downloaded_size) + ' bytes, it is probably \
                    damaged or incomplete')
      return -1
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
  if (ok == False):
    return -1

  print "Installing JDK to {0}".format(JDK_INSTALL_DIR)
  retcode, out, err = run_os_command(CREATE_JDK_DIR_CMD)
  savedPath = os.getcwd()
  os.chdir(JDK_INSTALL_DIR)
  retcode, out, err = run_os_command(MAKE_FILE_EXECUTABLE_CMD.format(dest_file))
  retcode, out, err = run_os_command(dest_file + ' -noregister')
  os.chdir(savedPath)
  if (retcode != 0):
       raise RetCodeException("Installation JDK returned code %s" % retcode) 
  return out, ok

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
      time.sleep(5)
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


def update_ambari_properties():
  prev_conf_file = search_file(AMBARI_PROPERTIES_RPMSAVE_FILE, get_conf_dir())
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  old_properties = None
  new_properties = None

  try:
    old_properties = Properties()
    old_properties.load(open(prev_conf_file))
  except (Exception), e:
    print 'Could not read "%s": %s' % (prev_conf_file, e)
    return -1

  try:
    new_properties = Properties()
    new_properties.load(open(conf_file))

    for prop_key, prop_value in old_properties.getPropertyDict().items():
      new_properties.process_pair(prop_key,prop_value)

    new_properties.store(open(conf_file,'w'))

  except (Exception), e:
    print 'Could not write "%s": %s' % (conf_file, e)
    return -1

  timestamp = datetime.datetime.now()
  format = '%Y%m%d%H%M%S'
  os.rename(AMBARI_PROPERTIES_RPMSAVE_FILE, AMBARI_PROPERTIES_RPMSAVE_FILE +
    '.' + timestamp.strftime(format))

  return 0

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
        resources_dir = properties[RESOURCES_DIR_KEY]
      except KeyError:
        print_error_msg("There is no value for " + RESOURCES_DIR_KEY + "in " + AMBARI_PROPERTIES_FILE)
        return -1
      copy_files(result, resources_dir)
      break

  return 0

#

# Setup the Ambari Server.
#
def setup(args):

  print 'Checking SELinux...'
  retcode = check_selinux()
  if not retcode == 0:
    print_error_msg ('Failed to disable SELinux. Exiting.')
    sys.exit(retcode)
   
  print 'Checking iptables...'
  retcode, out = check_iptables()
  if not retcode == 0 and out == IP_TBLS_ENABLED:
    print_error_msg ('Failed to stop iptables. Exiting.')
    sys.exit(retcode)

  print 'Checking JDK...'
  retcode = download_jdk(args)
  if not retcode == 0:
    print_error_msg ('Downloading or installing JDK failed. Exiting.')
    sys.exit(retcode)

  print 'Completing setup...'
  retcode = configure_os_settings()
  if not retcode == 0:
    print_error_msg ('Configure of OS settings in '
                   'ambari.properties failed. Exiting.')
    sys.exit(retcode)
    
  print 'Configuring database...'
  prompt_db_properties(args)
  
  #DB setup should be done last after doing any setup.
  
  if is_local_database(args):
    print 'Default properties detected. Using built-in database.'
    store_local_properties(args)

    print 'Checking PostgreSQL...'
    retcode = check_postgre_up()
    if not retcode == 0:
      print_error_msg ('Unable to start PostgreSQL server. Exiting')
      sys.exit(retcode)

    print 'Configuring database...'
    retcode = setup_db(args)
    if not retcode == 0:
      print_error_msg  ('Running database init script was failed. Exiting.')
      sys.exit(retcode)

    print 'Configuring PostgreSQL...'
    retcode = configure_postgres()
    if not retcode == 0:
      print_error_msg ('Unable to configure PostgreSQL server. Exiting')
      sys.exit(retcode)

  else:
    retcode = store_remote_properties(args)
    if retcode != 0:
      print_error_msg('Unable to save config file')
      sys.exit(retcode)

    
    check_jdbc_drivers(args)

    print 'Configuring remote database connection properties...'
    retcode = setup_remote_db(args)
    if retcode == -1:
      #means the cli was not found
      sys.exit(retcode)
      
    if not retcode == 0:
      print_error_msg ('Error while configuring connection properties. Exiting')
      sys.exit(retcode)


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
  okToRun = False
  choice = get_YN_input("**** WARNING **** You are about to reset and clear the "
                     "Ambari Server database. This will remove all cluster "
                     "host and configuration information from the database. "
                     "You will be required to re-configure the Ambari server "
                     "and re-run the cluster wizard. \n"
                     "Are you SURE you want to perform the reset "
                     "[yes/no]? ", True)
  okToRun = choice

  if not okToRun:
    print "Ambari Server 'reset' cancelled"
    return -1

  okToRun = False
  choice = get_YN_input("Confirm server reset [yes/no]? ", True)
  okToRun = choice

  if not okToRun:
    print "Ambari Server 'reset' cancelled"
    return -1

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
        print err
        return retcode

      retcode, out, err = execute_remote_script(args, DATABASE_INIT_SCRIPTS[DATABASE_INDEX])
      if not retcode == 0:
        if retcode == -1:
          print_warning_msg('Cannot find ' + client_desc + ' client in the path to reset the Ambari Server schema. To reset Ambari Server schema ' +
          'you must run the following DDL against the database to drop the schema:' + os.linesep + client_usage_cmd_drop + os.linesep +
          ', then you must run the following DDL against the database to create the schema ' + os.linesep + client_usage_cmd_init + os.linesep )
        print err
        return retcode

    else:
      print_warning_msg('Cannot find ' + client_desc + ' client in the path to reset the Ambari Server schema. To reset Ambari Server schema ' +
      'you must run the following DDL against the database to drop the schema:' + os.linesep + client_usage_cmd_drop + os.linesep +
      ', then you must run the following DDL against the database to create the schema ' + os.linesep + client_usage_cmd_init + os.linesep )
      return -1

  else:
    dbname = args.database_name
    filename = args.drop_script_file
    username = args.database_username
    password = args.database_password
    command = SETUP_DB_CMD[:]
    command[-1] = command[-1].format(filename, username, password)
    retcode, outdata, errdata = run_os_command(command)
    if not retcode == 0:
      print errdata
      return retcode

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
      print("Server is already running.")
      return
    except OSError, e:
      print_info_msg("Server is not running, continue.")

  conf_dir = get_conf_dir()
  jdk_path = find_jdk()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + JDK_INSTALL_DIR)
    return -1

  if args.persistence_type == "local":
    retcode = check_postgre_up()
    if not retcode == 0:
      print_error_msg("Unable to start PostgreSQL server. Exiting")
      sys.exit(retcode)

  print 'Checking iptables...'
  retcode, out = check_iptables()
  if not retcode == 0 and out == IP_TBLS_ENABLED:
    print_error_msg ("Failed to stop iptables. Exiting")
    sys.exit(retcode)

  command_base = SERVER_START_CMD_DEBUG if (SERVER_DEBUG_MODE or SERVER_START_DEBUG) else SERVER_START_CMD
  command = command_base.format(jdk_path, conf_dir, get_ambari_classpath())
  print "Running server: " + command
  server_process = subprocess.Popen(["/bin/sh", "-c", command])
  f = open(PID_DIR + os.sep + PID_NAME, "w")
  f.write(str(server_process.pid))
  f.close()



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
    print "Ambari Server not running"



#
# Upgrades the Ambari Server.
#
def upgrade(args):
  print 'Updating properties in ' + AMBARI_PROPERTIES_FILE + ' ...'
  retcode = update_ambari_properties()
  if not retcode == 0:
    print_error_msg(AMBARI_PROPERTIES_FILE + ' file can\'t be updated. Exiting')
    sys.exit(retcode)

  parse_properties_file(args)
  if args.persistence_type == "remote":


    pass
  else:
    print 'Checking PostgreSQL...'
    retcode = check_postgre_up()
    if not retcode == 0:
      print_error_msg('PostgreSQL server not running. Exiting')
      sys.exit(retcode)

    file = args.upgrade_script_file
    print 'Upgrading database...'
    retcode = execute_db_script(args, file)
    if not retcode == 0:
      print_error_msg('Database upgrade script has failed. Exiting.')
      sys.exit(retcode)

    print 'Checking database integrity...'
    check_file = file[:-3] + "Check" + file[-4:]
    retcode = check_db_consistency(args, check_file)

    if not retcode == 0:
      print 'Found inconsistency. Trying to fix...'
      fix_file = file[:-3] + "Fix" + file[-4:]
      retcode = execute_db_script(args, fix_file)

      if not retcode == 0:
        print_error_msg('Database cannot be fixed. Exiting.')
        sys.exit(retcode)
    else:
      print 'Database is consistent.'


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



def read_password(passwordDefault = PG_DEFAULT_PASSWORD):
  # setup password
  passwordPrompt = 'Password [' + passwordDefault + ']: '
  passwordPattern = PASSWORD_PATTERN
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

      args.database_password =  read_password(args.database_password)


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
  properties.process_pair(JDBC_PASSWORD_FILE_PROPERTY, store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))

  properties.process_pair(JDBC_RCA_DRIVER_PROPERTY, DATABASE_DRIVER_NAMES[DATABASE_INDEX])
  properties.process_pair(JDBC_RCA_URL_PROPERTY, DATABASE_CONNECTION_STRINGS[DATABASE_INDEX].format(jdbc_hostname, args.database_port, args.database_name))
  properties.process_pair(JDBC_RCA_USER_NAME_PROPERTY, args.database_username)
  properties.process_pair(JDBC_RCA_PASSWORD_FILE_PROPERTY, store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))


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
  pass

  return 0

# Get database client executable path
def get_db_cli_tool(args):
  for tool in DATABASE_CLI_TOOLS[DATABASE_INDEX]:
    cmd =CHECK_COMMAND_EXIST_CMD.format(tool)
    ret, out, err = run_in_shell(cmd)
    if ret == 0:
      return get_exec_path(tool)

  return None

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

def configure_database_username_password(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg ("Error getting ambari properties")
    return -1

  username = properties[JDBC_USER_NAME_PROPERTY]
  passFilePath = properties[JDBC_PASSWORD_FILE_PROPERTY]

  if username and passFilePath:
    print_info_msg("Database username + password already configured - skipping")
    args.database_username=username
    args.database_password = open(passFilePath).read()
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
  properties.process_pair(JDBC_PASSWORD_FILE_PROPERTY, store_password_file(args.database_password, JDBC_PASSWORD_FILENAME))
  
  conf_file = properties.fileName

  try:
    properties.store(open(conf_file, "w"))
  except Exception, e:
    print 'Could not write ambari config file "%s": %s' % (conf_file, e)
    return -1

  return 0
  
# Load ambari properties and return dict with values
def get_ambari_properties():
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  if conf_file is None:
    print 'File %s not found in search path $%s: %s' %\
          (AMBARI_PROPERTIES_FILE, AMBARI_CONF_VAR, get_conf_dir())
    return -1
  print_info_msg('Loading properties from ' + conf_file)

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
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
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

  args.database_password_file = properties[JDBC_PASSWORD_FILE_PROPERTY]
  if args.database_password_file:
    args.database_password = open(properties[JDBC_PASSWORD_FILE_PROPERTY]).read()

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
    pass

  #correct database
  if options.database is not None and options.database not in DATABASE_NAMES:
    print "Unsupported Database " + options.database
    parser.print_help()
    exit(-1)
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
      print "Incorrect database port " + options.database_port
      parser.print_help()
      exit(-1)
      
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
  else:
    parser.error("Invalid action")



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
