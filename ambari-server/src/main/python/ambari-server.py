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

if ambari_provider_module is not None:
  ambari_provider_module_option = "-Dprovider.module.class=" +\
                                  ambari_provider_module + " "

SERVER_START_CMD="{0}" + os.sep + "bin" + os.sep +\
                 "java -server -XX:NewRatio=2 "\
                 "-XX:+UseConcMarkSweepGC " +\
                 ambari_provider_module_option +\
                 os.getenv('AMBARI_JVM_ARGS','-Xms512m -Xmx2048m') +\
                 " -cp {1}"+ os.pathsep + "{2}" +\
                 "/* org.apache.ambari.server.controller.AmbariServer "\
                 ">/var/log/ambari-server/ambari-server.out 2>&1"
SERVER_START_CMD_DEBUG="{0}" + os.sep + "bin" + os.sep +\
                       "java -server -XX:NewRatio=2 -XX:+UseConcMarkSweepGC " +\
                       ambari_provider_module_option +\
                       os.getenv('AMBARI_JVM_ARGS','-Xms512m -Xmx2048m') +\
                       " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,"\
                       "server=y,suspend=n -cp {1}"+ os.pathsep + ".." +\
                       os.sep + "lib" + os.sep + "ambari-server" +\
                       os.sep +\
                       "* org.apache.ambari.server.controller.AmbariServer"

AMBARI_CONF_VAR="AMBARI_CONF_DIR"
AMBARI_SERVER_LIB="AMBARI_SERVER_LIB"
JAVA_HOME="JAVA_HOME"
PID_DIR="/var/run/ambari-server"
PID_NAME="ambari-server.pid"
AMBARI_PROPERTIES_FILE="ambari.properties"

SETUP_DB_CMD = ['su', 'postgres',
        '--command=psql -f {0} -v username=\'"{1}"\' -v password="\'{2}\'"']
PG_ST_CMD = "/sbin/service postgresql status"
PG_START_CMD = "/sbin/service postgresql start"
PG_RESTART_CMD = "/sbin/service postgresql restart"
PG_STATUS_RUNNING = "running"
PG_HBA_DIR = "/var/lib/pgsql/data/"
PG_HBA_CONF_FILE = PG_HBA_DIR + "pg_hba.conf"
PG_HBA_CONF_FILE_BACKUP = PG_HBA_DIR + "pg_hba_bak.conf.old"
POSTGRESQL_CONF_FILE = PG_HBA_DIR + "postgresql.conf"
PG_HBA_RELOAD_CMD = "su postgres --command='pg_ctl -D {0} reload'"
PG_DEFAULT_PASSWORD = "bigdata"
JDBC_USER_NAME_PROPERTY = "server.jdbc.user.name"
JDBC_PASSWORD_FILE_PROPERTY = "server.jdbc.user.passwd"
JDBC_PASSWORD_FILENAME = "password.dat"

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



def configure_pg_hba_ambaridb_users():
  args = optparse.Values()
  configure_postgres_username_password(args)

  with open(PG_HBA_CONF_FILE, "a") as pgHbaConf:
    pgHbaConf.write("\n")
    pgHbaConf.write("local  all  " + args.postgres_username +
                    ",mapred md5")
    pgHbaConf.write("\n")
    pgHbaConf.write("host  all   " + args.postgres_username +
                    ",mapred 0.0.0.0/0  md5")
    pgHbaConf.write("\n")
    pgHbaConf.write("host  all   " + args.postgres_username +
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
  configure_postgres_username_password(args)
  dbname = args.postgredbname
  file = args.init_script_file
  username = args.postgres_username
  password = args.postgres_password
  command = SETUP_DB_CMD[:]
  command[-1] = command[-1].format(file, username, password)
  retcode, outdata, errdata = run_os_command(command)
  if not retcode == 0:
    print errdata
  return retcode



def upgrade_db(args):
  #password access to ambari-server and mapred
  configure_postgres_username_password(args)
  dbname = args.postgredbname
  file = args.upgrade_script_file
  username = args.postgres_username
  password = args.postgres_password
  command = SETUP_DB_CMD[:]
  command[-1] = command[-1].format(file, username, password)
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
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  if conf_file is None:
    print 'File %s not found in search path $%s: %s' %\
          (AMBARI_PROPERTIES_FILE, AMBARI_CONF_VAR, get_conf_dir())
    return -1
  print_info_msg('Loading properties from ' + conf_file)

  if get_JAVA_HOME():
    return 0

  if args.java_home and os.path.exists(args.java_home):
    print_warning_msg("JAVA_HOME " + args.java_home
                    + " must be valid on ALL hosts")
    write_property(JAVA_HOME_PROPERTY, args.java_home)
    return 0

  properties = None
  try:
    properties = Properties()
    properties.load(open(conf_file))
  except (Exception), e:
    print 'Could not read "%s": %s' % (conf_file, e)
    return -1

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
        print "Request headr doesn't contain Content-Length";
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
  jdk_version = re.search('Creating (jdk.*)/jre', out).group(1)
  print "Successfully installed JDK to {0}/{1}".\
      format(JDK_INSTALL_DIR, jdk_version)
  write_property(JAVA_HOME_PROPERTY, "{0}/{1}".
      format(JDK_INSTALL_DIR, jdk_version))
  return 0



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
    print "About to start PostgreSQL"
    retcode, out, err = run_os_command(PG_START_CMD)
    return retcode



#
# Configures the OS settings in ambari properties.
#
def configure_os_settings():
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  if conf_file is None:
    print_error_msg ('File %s not found in search path $%s: %s'
                   % (AMBARI_PROPERTIES_FILE, AMBARI_CONF_VAR, get_conf_dir()))
    return -1
  print_info_msg ('Loading properties from ' + conf_file)
  properties = None
  try:
    properties = Properties()
    properties.load(open(conf_file))
  except (Exception), e:
    print_error_msg ('Could not read "%s": %s' % (conf_file, e))
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
  with open(conf_file, "a") as ambariConf:
    ambariConf.write(OS_TYPE_PROPERTY + "=" + master_os_type)
    ambariConf.write("\n")
    ambariConf.closed
  return 0



def get_JAVA_HOME():
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  properties = Properties()
  
  try:
    properties.load(open(conf_file))
    java_home = properties[JAVA_HOME_PROPERTY]
    if (not 0 == len(java_home)) and (os.path.exists(java_home)):
      return java_home
  except (Exception), e:
    print 'Could not read "%s": %s' % (conf_file, e)
  
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

  print "Ambari Server 'setup' finished successfully"



#
# Resets the Ambari Server.
#
def reset(args):
  okToRun = False
  choice = raw_input("**** WARNING **** You are about to reset and clear the "
                     "Ambari Server database. This will remove all cluster "
                     "host and configuration information from the database. "
                     "You will be required to re-configure the Ambari server "
                     "and re-run the cluster wizard. \n"
                     "Are you SURE you want to perform the reset "
                     "[yes/no]? ").lower()
  if choice in set(['yes']):
    okToRun = True

  if not okToRun:
    print "Ambari Server 'reset' cancelled"
    return -1

  okToRun = False
  choice = raw_input("Confirm server reset [yes/no]? ").lower()
  if choice in set(['yes']):
    okToRun = True

  if not okToRun:
    print "Ambari Server 'reset' cancelled"
    return -1

  print "Reseting the Server database..."

  configure_postgres_username_password(args)
  dbname = args.postgredbname
  filename = args.drop_script_file
  username = args.postgres_username
  password = args.postgres_password
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
  retcode = check_postgre_up()
  if not retcode == 0:
    print_error_msg("Unable to start PostgreSQL server. Exiting")
    sys.exit(retcode)

  print 'Checking iptables...'
  retcode, out = check_iptables()
  if not retcode == 0 and out == IP_TBLS_ENABLED:
    print_error_msg ("Failed to stop iptables. Exiting")
    sys.exit(retcode)

  command = SERVER_START_CMD.format(jdk_path, conf_dir, get_ambari_jars())
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

  print 'Checking PostgreSQL...'
  retcode = check_postgre_up()
  if not retcode == 0:
    printErrorMsg ('PostgreSQL server not running. Exiting')
    sys.exit(retcode)

  print 'Upgrading database...'
  retcode = upgrade_db(args)
  if not retcode == 0:
    printErrorMsg  ('Database upgrade script has failed. Exiting.')
    sys.exit(retcode)

  print "Ambari Server 'upgrade' finished successfully"

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
def print_warning_msg(msg):
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



def configure_postgres_password():
  # setup password
  passwordDefault = PG_DEFAULT_PASSWORD
  passwordPrompt = 'Password [' + passwordDefault + ']: '
  passwordPattern = "^[a-zA-Z0-9_-]*$"
  passwordDescr = "Invalid characters in password. Use only alphanumeric or " \
                  "_ or - characters"

  password = get_validated_string_input(passwordPrompt, passwordDefault,
      passwordPattern, passwordDescr, True)
  if password != passwordDefault:
    password1 = get_validated_string_input("Re-enter password: ",
      passwordDefault, passwordPattern, passwordDescr, True)
    if password != password1:
      print "Passwords do not match"
      password = configure_postgres_password()

  return password



def get_pass_file_path(conf_file):
  return os.path.join(os.path.dirname(conf_file),
    JDBC_PASSWORD_FILENAME)



def configure_postgres_username_password(args):
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())
  properties = Properties()

  try:
    properties.load(open(conf_file))
  except Exception, e:
    print 'Could not read "%s": %s' % (conf_file, e)
    return -1

  username = properties[JDBC_USER_NAME_PROPERTY]
  passFilePath = properties[JDBC_PASSWORD_FILE_PROPERTY]

  if username and passFilePath:
    print_info_msg("Database username + password already configured - skipping")
    args.postgres_username=username
    args.postgres_password = open(passFilePath).read()
    return 1

  # setup username
  usernameDefault = 'ambari-server'
  usernamePrompt = 'Username [' + usernameDefault + ']: '
  usernamePattern = "^[a-zA-Z_][a-zA-Z0-9_\-]*$"
  usernameDescr = "Invalid characters in username. Start with _ or alpha " \
                  "followed by alphanumeric or _ or - characters"
  username = usernameDefault

  # setup password
  password = PG_DEFAULT_PASSWORD

  ok = get_YN_input("Enter advanced database configuration [y/n] (n)? ", False)
  if ok:
    username = get_validated_string_input(usernamePrompt, usernameDefault,
        usernamePattern, usernameDescr, False)
    print "Database username set to: " + username
    password = configure_postgres_password()
        
  passFilePath = get_pass_file_path(conf_file)
  
  print_info_msg ("Database username set to: " + username)
  print_info_msg ("Database password set to: " + password)
    
  with open(passFilePath, 'w+') as passFile:
    passFile.write(password)
    pass
  os.chmod(passFilePath, stat.S_IREAD | stat.S_IWRITE)

  write_property(JDBC_USER_NAME_PROPERTY, username)
  write_property(JDBC_PASSWORD_FILE_PROPERTY,passFilePath)
  args.postgres_username=username
  args.postgres_password=password



#
# Main.
#
def main():
  parser = optparse.OptionParser(usage="usage: %prog [options] action",)
  parser.add_option('-d', '--postgredbname', default='ambari',
                      help="Database name in postgresql")
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
                              "Ambari-DDL-Postgres-UPGRADE-1.2.1.sql",
                      help="File with upgrade script")
  parser.add_option('-j', '--java-home', default=None,
                  help="Use specified java_home.  Must be valid on all hosts")
  parser.add_option("-v", "--verbose",
                  action="store_true", dest="verbose", default=False,
                  help="Print verbose status messages")
  parser.add_option("-s", "--silent",
                  action="store_true", dest="silent", default=False,
                  help="Silently accepts default prompt values")

  (options, args) = parser.parse_args()

  # set verbose
  global VERBOSE
  VERBOSE = options.verbose

  # set silent
  global SILENT
  SILENT = options.silent

  if not len(args) == 1:
    print parser.print_help()
    parser.error("Invalid number of arguments")
	
  action = args[0]

  if action == SETUP_ACTION:
    setup(options)
  elif action == START_ACTION:
    start(options)
  elif action == STOP_ACTION:
    stop(options)
  elif action == RESET_ACTION:
    reset(options)
  elif action == UPGRADE_ACTION:
    upgrade(options)
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

  def load(self, stream):
    if type(stream) is not file:
      raise TypeError, 'Argument should be a file object!'
    if stream.mode != 'r':
      raise ValueError, 'Stream should be opened in read-only mode!'
    try:
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


if __name__ == "__main__":
  main()
