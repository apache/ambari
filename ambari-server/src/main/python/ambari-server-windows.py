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

from ambari_commons.ambari_service import AmbariService, ENV_PYTHON_PATH
from ambari_commons.logging_utils import *
from ambari_commons.os_utils import remove_file
from ambari_commons.os_windows import SvcStatusCallback

from ambari_server import utils
from ambari_server.dbConfiguration import DBMSConfig
from ambari_server.resourceFilesKeeper import ResourceFilesKeeper, KeeperException
from ambari_server.serverConfiguration import *
from ambari_server.serverSetup import setup, reset, is_server_running, upgrade
from ambari_server.setupActions import *
from ambari_server.setupSecurity import *
from ambari_server.serverSetup_windows import SERVICE_PASSWORD_KEY, SERVICE_USERNAME_KEY

# debug settings
SERVER_START_DEBUG = False
SUSPEND_START_MODE = False

# server commands
ambari_provider_module_option = ""
ambari_provider_module = os.environ.get('AMBARI_PROVIDER_MODULE')

#Common setup or upgrade message
SETUP_OR_UPGRADE_MSG = "- If this is a new setup, then run the \"ambari-server setup\" command to create the user\n" \
"- If this is an upgrade of an existing setup, run the \"ambari-server upgrade\" command.\n" \
"Refer to the Ambari documentation for more information on setup and upgrade."

AMBARI_SERVER_DIE_MSG = "Ambari Server java process died with exitcode {0}. Check {1} for more information."

if ambari_provider_module is not None:
  ambari_provider_module_option = "-Dprovider.module.class=" +\
                                  ambari_provider_module + " "

SERVER_START_CMD = \
                 "-server -XX:NewRatio=3 "\
                 "-XX:+UseConcMarkSweepGC " +\
                 "-XX:-UseGCOverheadLimit -XX:CMSInitiatingOccupancyFraction=60 " +\
                 ambari_provider_module_option +\
                 os.getenv('AMBARI_JVM_ARGS', '-Xms512m -Xmx2048m') +\
                 " -cp {0}" +\
                 " org.apache.ambari.server.controller.AmbariServer"
SERVER_START_CMD_DEBUG = \
                       "-server -XX:NewRatio=2 -XX:+UseConcMarkSweepGC " +\
                       ambari_provider_module_option +\
                       os.getenv('AMBARI_JVM_ARGS', '-Xms512m -Xmx2048m') +\
                       " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,"\
                       "server=y,suspend={1} -cp {0}" +\
                       " org.apache.ambari.server.controller.AmbariServer"
SERVER_SEARCH_PATTERN = "org.apache.ambari.server.controller.AmbariServer"

SERVER_INIT_TIMEOUT = 5
SERVER_START_TIMEOUT = 10

PID_NAME = "ambari-server.pid"
EXITCODE_NAME = "ambari-server.exitcode"

SERVER_VERSION_FILE_PATH = "server.version.file"

# linux open-file limit
ULIMIT_OPEN_FILES_KEY = 'ulimit.open.files'
ULIMIT_OPEN_FILES_DEFAULT = 10000


class AmbariServerService(AmbariService):
  AmbariService._svc_name_ = "Ambari Server"
  AmbariService._svc_display_name_ = "Ambari Server"
  AmbariService._svc_description_ = "Ambari Server"

  AmbariService._AdjustServiceVersion()

  # Adds the necessary script dir to the Python's modules path
  def _adjustPythonPath(self, current_dir):
    python_path = os.path.join(current_dir, "sbin")
    sys.path.insert(0, python_path)

  def SvcDoRun(self):
    scmStatus = SvcStatusCallback(self)

    properties = get_ambari_properties()
    self.options.verbose = get_value_from_properties(properties, VERBOSE_OUTPUT_KEY, self.options.verbose)
    self.options.debug = get_value_from_properties(properties, DEBUG_MODE_KEY, self.options.debug)
    self.options.suspend_start = get_value_from_properties(properties, SUSPEND_START_MODE_KEY, self.options.suspend_start)

    self.redirect_output_streams()

    childProc = server_process_main(self.options, scmStatus)

    if not self._StopOrWaitForChildProcessToFinish(childProc):
      return

    pid_file_path = PID_DIR + os.sep + PID_NAME
    remove_file(pid_file_path)
    pass

  def _InitOptionsParser(self):
    return init_options_parser()

  def redirect_output_streams(self):
    properties = get_ambari_properties()

    outFilePath = properties[SERVER_OUT_FILE_KEY]
    if (outFilePath is None or outFilePath == ""):
      outFilePath = SERVER_OUT_FILE

    self._RedirectOutputStreamsToFile(outFilePath)
    pass

def ctrlHandler(ctrlType):
  AmbariServerService.DefCtrlCHandler()
  return True

def svcsetup():
  AmbariServerService.set_ctrl_c_handler(ctrlHandler)
  # we don't save password between 'setup' runs, so we can't run Install every time. We run 'setup' only if user and
  # password provided or if service not installed
  if (SERVICE_USERNAME_KEY in os.environ and SERVICE_PASSWORD_KEY in os.environ):
    AmbariServerService.Install(username=os.environ[SERVICE_USERNAME_KEY], password=os.environ[SERVICE_PASSWORD_KEY])
  elif AmbariServerService.QueryStatus() == "not installed":
    AmbariServerService.Install()
  pass

#
# Starts the Ambari Server as a standalone process.
# args:
#  <no arguments> = start the server as a process. For now, there is no restrictions for the number of server instances
#     that can run like this.
#  -s, --single-instance = Reserved for future use. When starting the server as a process, ensure only one instance of the process is running.
#     If this is the second instance of the process, the function fails.
#
def start(options):
  AmbariServerService.set_ctrl_c_handler(ctrlHandler)

  #Run as a normal process. Invoke the ServiceMain directly.
  childProc = server_process_main(options)

  childProc.wait()

  pid_file_path = PID_DIR + os.sep + PID_NAME
  remove_file(pid_file_path)

#
# Starts the Ambari Server as a service.
# Start the server in normal mode, as a Windows service. If the Ambari server is
#     not registered as a service, the function fails. By default, only one instance of the service can
#     possibly run.
#
def svcstart():
  AmbariServerService.Start()
  pass

def server_process_main(options, scmStatus=None):
  # set verbose
  try:
    global VERBOSE
    VERBOSE = options.verbose
  except AttributeError:
    pass

  # set silent
  try:
    global SILENT
    SILENT = options.silent
  except AttributeError:
    pass

  # debug mode
  try:
    global DEBUG_MODE
    DEBUG_MODE = options.debug
  except AttributeError:
    pass

  # stop Java process at startup?
  try:
    global SUSPEND_START_MODE
    SUSPEND_START_MODE = options.suspend_start
  except AttributeError:
    pass

  if not utils.check_reverse_lookup():
    print_warning_msg("The hostname was not found in the reverse DNS lookup. "
                      "This may result in incorrect behavior. "
                      "Please check the DNS setup and fix the issue.")

  properties = get_ambari_properties()

  print_info_msg("Ambari Server is not running...")

  conf_dir = get_conf_dir()
  jdk_path = find_jdk()
  if jdk_path is None:
    err = "No JDK found, please run the \"ambari-server setup\" " \
                    "command to install a JDK automatically or install any " \
                    "JDK manually to " + JDK_INSTALL_DIR
    raise FatalException(1, err)

  # Preparations

  result = ensure_dbms_is_running(options, properties, scmStatus)
  if result == -1:
    raise FatalException(-1, "Unable to connect to the database")

  if scmStatus is not None:
    scmStatus.reportStartPending()

  ensure_resources_are_organized(properties)

  if scmStatus is not None:
    scmStatus.reportStartPending()

  environ = os.environ.copy()
  ensure_server_security_is_configured(properties, environ)

  if scmStatus is not None:
    scmStatus.reportStartPending()

  conf_dir = os.path.abspath(conf_dir) + os.pathsep + get_ambari_classpath()
  if conf_dir.find(' ') != -1:
    conf_dir = '"' + conf_dir + '"'

  java_exe = jdk_path + os.sep + JAVA_EXE_SUBPATH
  pidfile = PID_DIR + os.sep + PID_NAME
  command_base = SERVER_START_CMD_DEBUG if (DEBUG_MODE or SERVER_START_DEBUG) else SERVER_START_CMD
  suspend_mode = 'y' if SUSPEND_START_MODE else 'n'
  command = command_base.format(conf_dir, suspend_mode)
  if not os.path.exists(PID_DIR):
    os.makedirs(PID_DIR, 0755)

  set_open_files_limit(get_ulimit_open_files());

  #Ignore the requirement to run as root. In Windows, by default the child process inherits the security context
  # and the environment from the parent process.
  param_list = java_exe + " " + command

  print_info_msg("Running server: " + str(param_list))
  procJava = subprocess.Popen(param_list, env=environ)

  #wait for server process for SERVER_START_TIMEOUT seconds
  print "Waiting for server start..."

  pidJava = procJava.pid
  if pidJava <= 0:
    procJava.terminate()
    exitcode = procJava.returncode
    exitfile = os.path.join(PID_DIR, EXITCODE_NAME)
    utils.save_pid(exitcode, exitfile)

    if scmStatus is not None:
      scmStatus.reportStopPending()

    raise FatalException(-1, AMBARI_SERVER_DIE_MSG.format(exitcode, SERVER_OUT_FILE))
  else:
    utils.save_pid(pidJava, pidfile)
    print "Server PID at: "+pidfile
    print "Server out at: "+SERVER_OUT_FILE
    print "Server log at: "+SERVER_LOG_FILE

  if scmStatus is not None:
    scmStatus.reportStarted()

  return procJava

#Check the JDBC driver status
#If not found abort
#Get SQL Server service status from SCM
#If 'stopped' then start it
#Wait until the status is 'started' or a configured timeout elapses
#If the timeout has been reached, bail out with exception
def ensure_dbms_is_running(options, properties, scmStatus):
  dbms = DBMSConfig.create(options, properties, "Ambari")
  if not dbms._is_jdbc_driver_installed(properties):
    raise FatalException(-1, "JDBC driver is not installed. Run ambari-server setup and try again.")

  dbms.ensure_dbms_is_running(options, properties, scmStatus)

  dbms2 = DBMSConfig.create(options, properties, "Metrics")
  if dbms2.database_host.lower() != dbms.database_host.lower():
    dbms2.ensure_dbms_is_running(options, properties, scmStatus)
  pass

def ensure_resources_are_organized(properties):
  resources_location = get_resources_location(properties)
  resource_files_keeper = ResourceFilesKeeper(resources_location)
  try:
    print "Organizing resource files at {0}...".format(resources_location,
                                                       verbose=VERBOSE)
    resource_files_keeper.perform_housekeeping()
  except KeeperException, ex:
    msg = "Can not organize resource files at {0}: {1}".format(
      resources_location, str(ex))
    raise FatalException(-1, msg)


def ensure_server_security_is_configured(properties, environ):
  pass


#
# Stops the Ambari Server.
#
def svcstop():
  AmbariServerService.Stop()


### Stack upgrade ###

#def upgrade_stack(args, stack_id, repo_url=None, repo_url_os=None):


def get_resources_location(properties):
  res_location = properties[RESOURCES_DIR_PROPERTY]
  if res_location is None:
    res_location = RESOURCES_DIR_DEFAULT
  return res_location
#  pass

def get_stack_location(properties):
  stack_location = properties[STACK_LOCATION_KEY]
  if stack_location is None:
    stack_location = STACK_LOCATION_DEFAULT
  return stack_location
#  pass


#
# The Ambari Server status.
#
def svcstatus(options):
  options.exit_message = None

  statusStr = AmbariServerService.QueryStatus()
  print "Ambari Server is " + statusStr


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


def init_options_parser():
  parser = optparse.OptionParser(usage="usage: %prog action [options] [stack_id os]", )
  #parser.add_option('-i', '--create-db-script-file', dest="create_db_script_file",
  #                  default="resources" + os.sep + "Ambari-DDL-SQLServer-CREATELOCAL.sql",
  #                  help="File with database creation script")
  parser.add_option('-f', '--init-script-file', dest="init_db_script_file",
                    default="resources" + os.sep + "Ambari-DDL-SQLServer-CREATE.sql",
                    help="File with database setup script")
  parser.add_option('-r', '--drop-script-file', dest="cleanup_db_script_file",
                    default="resources" + os.sep + "Ambari-DDL-SQLServer-DROP.sql",
                    help="File with database cleanup script")
  parser.add_option('-j', '--java-home', dest="java_home", default=None,
                    help="Use specified java_home.  Must be valid on all hosts")
  parser.add_option("-v", "--verbose",
                    action="store_true", dest="verbose", default=False,
                    help="Print verbose status messages")
  parser.add_option("-s", "--silent",
                    action="store_true", dest="silent", default=False,
                    help="Silently accepts default prompt values")
  parser.add_option('-g', '--debug', action="store_true", dest='debug', default=False,
                    help="Start ambari-server in debug mode")
  parser.add_option('-y', '--suspend-start', action="store_true", dest='suspend_start', default=False,
                    help="Freeze ambari-server Java process at startup in debug mode")

  parser.add_option('-a', '--databasehost', dest="database_host", default=None,
                    help="Hostname of database server")
  parser.add_option('-n', '--databaseport', dest="database_port", default=None,
                    help="Database server listening port")
  parser.add_option('-d', '--databasename', dest="database_name", default=None,
                    help="Database/Schema/Service name or ServiceID")
  parser.add_option('-w', '--windowsauth', action="store_true", dest="database_windows_auth", default=None,
                    help="Integrated Windows authentication")
  parser.add_option('-u', '--databaseusername', dest="database_username", default=None,
                    help="Database user login")
  parser.add_option('-p', '--databasepassword', dest="database_password", default=None,
                    help="Database user password")

  parser.add_option('-t', '--init-metrics-script-file', dest="init_metrics_db_script_file", default=None,
                    help="File with metrics database setup script")
  parser.add_option('-c', '--drop-metrics-script-file', dest="cleanup_metrics_db_script_file", default=None,
                    help="File with metrics database cleanup script")

  parser.add_option('-m', '--metricsdatabasehost', dest="metrics_database_host", default=None,
                    help="Hostname of metrics database server")
  parser.add_option('-o', '--metricsdatabaseport', dest="metrics_database_port", default=None,
                    help="Metrics database server listening port")
  parser.add_option('-e', '--metricsdatabasename', dest="metrics_database_name", default=None,
                    help="Metrics database/Schema/Service name or ServiceID")
  parser.add_option('-z', '--metricswindowsauth', action="store_true", dest="metrics_database_windows_auth", default=None,
                    help="Integrated Windows authentication for the metrics database")
  parser.add_option('-q', '--metricsdatabaseusername', dest="metrics_database_username", default=None,
                    help="Metrics database user login")
  parser.add_option('-l', '--metricsdatabasepassword', dest="metrics_database_password", default=None,
                    help="Metrics database user password")

  parser.add_option('--jdbc-driver', default=None, dest="jdbc_driver",
                    help="Specifies the path to the JDBC driver JAR file for the " \
                         "database type specified with the --jdbc-db option. Used only with --jdbc-db option.")
  # -b, -i, -k and -x the remaining available short options
  # -h reserved for help
  return parser

def are_cmd_line_db_args_blank(options):
  if (options.database_host is None \
    and options.database_name is None \
    and options.database_windows_auth is None \
    and options.database_username is None \
    and options.database_password is None \
    and options.metrics_database_host is None \
    and options.metrics_database_name is None \
    and options.metrics_database_windows_auth is None \
    and options.metrics_database_username is None \
    and options.metrics_database_password is None):
    return True
  return False


def are_db_auth_options_ok(db_windows_auth, db_username, db_password):
  if db_windows_auth is True:
    return True
  else:
    if db_username is not None and db_username is not "" and db_password is not None and db_password is not "":
      return True
  return False

def are_cmd_line_db_args_valid(options):
  if (options.database_host is not None and options.database_host is not "" \
      #and options.database_name is not None \         # ambari by default is ok
      and are_db_auth_options_ok(options.database_windows_auth,
                               options.database_username,
                               options.database_password) \
    and options.metrics_database_host is not None and options.metrics_database_host is not "" \
    #and options.metrics_database_name is not None \  # HadoopMetrics by default is ok
    and are_db_auth_options_ok(options.metrics_database_windows_auth,
                               options.metrics_database_username,
                               options.metrics_database_password)):
    return True
  return False


def setup_security(args):
  need_restart = True
  #Print menu options
  print '=' * 75
  print 'Choose one of the following options: '
  print '  [1] Enable HTTPS for Ambari server.'
  print '  [2] Encrypt passwords stored in ambari.properties file.'
  print '  [3] Setup Ambari kerberos JAAS configuration.'
  print '=' * 75
  choice = get_validated_string_input('Enter choice, (1-3): ', '0', '[1-3]',
                                      'Invalid choice', False, False)

  if choice == '1':
    need_restart = setup_https(args)
  elif choice == '2':
    setup_master_key()
  elif choice == '3':
    setup_ambari_krb5_jaas()
  else:
    raise FatalException('Unknown option for setup-security command.')

  return need_restart

#
# Main.
#
def main():
  parser = init_options_parser()
  (options, args) = parser.parse_args()

  #perform checks
  options.warnings = []
  options.must_set_database_options = False

  if are_cmd_line_db_args_blank(options):
    options.must_set_database_options = True
    #TODO Silent is invalid here, right?

  elif not are_cmd_line_db_args_valid(options):
    parser.error('All database options should be set. Please see help for the options.')

  ## jdbc driver and db options validation
  #if options.jdbc_driver is None and options.jdbc_db is not None:
  #  parser.error("Option --jdbc-db is used only in pair with --jdbc-driver")
  #elif options.jdbc_driver is not None and options.jdbc_db is None:
  #  parser.error("Option --jdbc-driver is used only in pair with --jdbc-db")

  if options.debug:
    sys.frozen = 'windows_exe' # Fake py2exe so we can debug

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
      svcsetup()
    elif action == START_ACTION:
      svcstart()
    elif action == PSTART_ACTION:
      start(options)
    elif action == STOP_ACTION:
      svcstop()
    elif action == RESET_ACTION:
      reset(options, AmbariServerService)
    elif action == STATUS_ACTION:
      svcstatus(options)
    elif action == UPGRADE_ACTION:
      upgrade(options)
#    elif action == UPGRADE_STACK_ACTION:
#      stack_id = args[1]
#      repo_url = None
#      repo_url_os = None
#
#      if len(args) > 2:
#        repo_url = args[2]
#      if len(args) > 3:
#        repo_url_os = args[3]
#
#      upgrade_stack(options, stack_id, repo_url, repo_url_os)
    elif action == LDAP_SETUP_ACTION:
      setup_ldap()
    elif action == SETUP_SECURITY_ACTION:
      need_restart = setup_security(options)
    else:
      parser.error("Invalid action")

    if action in ACTION_REQUIRE_RESTART and need_restart:
      status, stateDesc = is_server_running(AmbariServerService)
      if status:
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


if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)