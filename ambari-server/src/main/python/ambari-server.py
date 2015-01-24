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
import json
import base64

from ambari_commons import OSCheck, OSConst
from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import get_verbose, set_verbose, get_silent, set_silent, get_debug_mode, \
  set_debug_mode, print_info_msg, print_warning_msg, print_error_msg, set_debug_mode_from_options
from ambari_commons.os_utils import is_root, run_os_command, search_file, copy_file, remove_file, \
  set_file_permissions
from ambari_server.BackupRestore import main as BackupRestore_main
from ambari_server.dbConfiguration import DATABASE_NAMES, DATABASE_FULL_NAMES
from ambari_server.properties import Properties
from ambari_server.resourceFilesKeeper import ResourceFilesKeeper, KeeperException
from ambari_server.serverConfiguration import AMBARI_PROPERTIES_FILE, configDefaults, \
  backup_file_in_temp, check_database_name_property, find_jdbc_driver, find_jdk, find_properties_file, get_ambari_classpath, \
  get_ambari_properties, get_conf_dir, get_full_ambari_classpath, get_value_from_properties, is_alias_string, \
  parse_properties_file, read_ambari_user, \
  BLIND_PASSWORD, SETUP_OR_UPGRADE_MSG, JDBC_RCA_PASSWORD_ALIAS, \
  JDBC_PASSWORD_PROPERTY, JDBC_PASSWORD_FILENAME, JDBC_RCA_PASSWORD_FILE_PROPERTY, \
  GET_FQDN_SERVICE_URL, get_stack_location, IS_LDAP_CONFIGURED, LDAP_PRIMARY_URL_PROPERTY, LDAP_MGR_PASSWORD_PROPERTY, \
  LDAP_MGR_PASSWORD_ALIAS, LDAP_MGR_PASSWORD_FILENAME, LDAP_MGR_USERNAME_PROPERTY, PID_NAME, \
  read_passwd_for_alias, get_credential_store_location, get_master_key_location, get_is_secure, get_is_persisted, \
  get_original_master_key, SECURITY_PROVIDER_PUT_CMD, get_java_exe_path, SECURITY_PROVIDER_KEY_CMD, \
  SECURITY_IS_ENCRYPTION_ENABLED, SECURITY_KERBEROS_JASS_FILENAME, SECURITY_KEY_ENV_VAR_NAME, \
  SECURITY_MASTER_KEY_FILENAME, SECURITY_MASTER_KEY_LOCATION, \
  SSL_TRUSTSTORE_PASSWORD_ALIAS, SSL_TRUSTSTORE_PASSWORD_PROPERTY, SSL_TRUSTSTORE_PATH_PROPERTY, SSL_TRUSTSTORE_TYPE_PROPERTY, \
  update_debug_mode
from ambari_server.serverSetup import reset, setup, is_server_runing
from ambari_server.serverUpgrade import upgrade, upgrade_stack

if not OSCheck.is_windows_family():
  from ambari_server.dbConfiguration_linux import PGConfig

from ambari_server.setupActions import SETUP_ACTION, START_ACTION, STOP_ACTION, RESET_ACTION, STATUS_ACTION, \
  UPGRADE_ACTION, UPGRADE_STACK_ACTION, LDAP_SETUP_ACTION, LDAP_SYNC_ACTION, SETUP_SECURITY_ACTION, \
  REFRESH_STACK_HASH_ACTION, BACKUP_ACTION, RESTORE_ACTION, ACTION_REQUIRE_RESTART
from ambari_server.setupSecurity import adjust_directory_permissions, read_password, store_password_file, \
  remove_password_file, encrypt_password, get_truststore_password
from ambari_server.userInput import get_YN_input, get_validated_string_input, get_validated_filepath_input, \
  get_prompt_default
from ambari_server.utils import check_exitcode, locate_file, \
  looking_for_pid, save_main_pid_ex, wait_for_pid

# debug settings
SERVER_START_DEBUG = False
SUSPEND_START_MODE = False

# ldap settings
LDAP_SYNC_ALL = False
LDAP_SYNC_EXISTING = False
LDAP_SYNC_USERS = None
LDAP_SYNC_GROUPS = None

# server commands
ambari_provider_module_option = ""
ambari_provider_module = os.environ.get('AMBARI_PROVIDER_MODULE')


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

# api properties
SERVER_API_HOST = '127.0.0.1'
SERVER_API_PROTOCOL = 'http'
SERVER_API_PORT = '8080'
SERVER_API_LDAP_URL = '/api/v1/ldap_sync_events'

AMBARI_SERVER_DIE_MSG = "Ambari Server java process died with exitcode {0}. Check {1} for more information."

#SSL certificate metainfo
COMMON_NAME_ATTR = 'CN'
NOT_BEFORE_ATTR = 'notBefore'
NOT_AFTER_ATTR = 'notAfter'

if ambari_provider_module is not None:
  ambari_provider_module_option = "-Dprovider.module.class=" +\
                                  ambari_provider_module + " "

SERVER_START_CMD = "{0} -server -XX:NewRatio=3 "\
                 "-XX:+UseConcMarkSweepGC " +\
                 "-XX:-UseGCOverheadLimit -XX:CMSInitiatingOccupancyFraction=60 " +\
                 ambari_provider_module_option +\
                 os.getenv('AMBARI_JVM_ARGS', '-Xms512m -Xmx2048m') +\
                 " -cp {1}" + os.pathsep + "{2}" +\
                 " org.apache.ambari.server.controller.AmbariServer "\
                 ">" + configDefaults.SERVER_OUT_FILE + " 2>&1 || echo $? > {3} &"
SERVER_START_CMD_DEBUG = "{0} -server -XX:NewRatio=2 -XX:+UseConcMarkSweepGC " +\
                       ambari_provider_module_option +\
                       os.getenv('AMBARI_JVM_ARGS', '-Xms512m -Xmx2048m') +\
                       " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005,"\
                       "server=y,suspend={4} -cp {1}" + os.pathsep + "{2}" +\
                       " org.apache.ambari.server.controller.AmbariServer" \
                       ">" + configDefaults.SERVER_OUT_FILE + " 2>&1 || echo $? > {3} &"
SERVER_SEARCH_PATTERN = "org.apache.ambari.server.controller.AmbariServer"


ULIMIT_CMD = "ulimit -n"
SERVER_INIT_TIMEOUT = 5
SERVER_START_TIMEOUT = 10

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

CLIENT_SECURITY_KEY = "client.security"

EXITCODE_NAME = "ambari-server.exitcode"

SRVR_TWO_WAY_SSL_PORT_PROPERTY = "security.server.two_way_ssl.port"
SRVR_TWO_WAY_SSL_PORT = "8441"

SRVR_ONE_WAY_SSL_PORT_PROPERTY = "security.server.one_way_ssl.port"
SRVR_ONE_WAY_SSL_PORT = "8440"

REGEX_IP_ADDRESS = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
REGEX_HOSTNAME = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"
REGEX_HOSTNAME_PORT = "^(.*:[0-9]{1,5}$)"
REGEX_TRUE_FALSE = "^(true|false)?$"
REGEX_ANYTHING = ".*"

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

### System interaction ###

def check_reverse_lookup():
  """
  Check if host fqdn resolves to current host ip
  """
  try:
    host_name = socket.gethostname().lower()
    host_ip = socket.gethostbyname(host_name)
    host_fqdn = socket.getfqdn().lower()
    fqdn_ip = socket.gethostbyname(host_fqdn)
    return host_ip == fqdn_ip
  except socket.error:
    pass
  return False


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

  check_database_name_property()
  parse_properties_file(args)

  update_debug_mode()

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
                    "JDK manually to " + configDefaults.JDK_INSTALL_DIR
    raise FatalException(1, err)

  if args.persistence_type == 'remote':
    result = find_jdbc_driver(args)
    msg = 'Before starting Ambari Server, ' \
          'you must copy the {0} JDBC driver JAR file to {1}.'.format(
          DATABASE_FULL_NAMES[args.dbms],
          configDefaults.JAVA_SHARE_PATH)
    if result == -1:
      raise FatalException(-1, msg)

  # Preparations

  if is_root():
    print "Ambari Server running with 'root' privileges."

    if args.persistence_type == "local":
      pg_status, retcode, out, err = PGConfig._check_postgre_up()
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
      import pwd

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

  debug_mode = get_debug_mode()
  debug_start = (debug_mode & 1) or SERVER_START_DEBUG
  suspend_start = 'y' if ((debug_mode & 2) or SUSPEND_START_MODE) else 'n'

  pidfile = os.path.join(configDefaults.PID_DIR, PID_NAME)
  command_base = SERVER_START_CMD_DEBUG if debug_start else SERVER_START_CMD
  command = "%s %s; %s" % (ULIMIT_CMD, str(get_ulimit_open_files()),
                           command_base.format(get_java_exe_path(),
                                               conf_dir,
                                               get_ambari_classpath(),
                                               os.path.join(configDefaults.PID_DIR, EXITCODE_NAME),
                                               suspend_start)
                           )
  if not os.path.exists(configDefaults.PID_DIR):
    os.makedirs(configDefaults.PID_DIR, 0755)

  # required to start properly server instance
  os.chdir(configDefaults.ROOT_FS_PATH)

  #For properly daemonization server should be started using shell as parent
  if is_root() and ambari_user != "root":
    # To inherit exported environment variables (especially AMBARI_PASSPHRASE),
    # from subprocess, we have to skip --login option of su command. That's why
    # we change dir to / (otherwise subprocess can face with 'permission denied'
    # errors while trying to list current directory
    param_list = [locate_file('su', '/bin'), ambari_user, "-s", locate_file('sh', '/bin'), "-c", command]
  else:
    param_list = [locate_file('sh', '/bin'), "-c", command]

  print_info_msg("Running server: " + str(param_list))
  subprocess.Popen(param_list, env=environ)

  print "Server PID at: "+pidfile
  print "Server out at: "+configDefaults.SERVER_OUT_FILE
  print "Server log at: "+configDefaults.SERVER_LOG_FILE

  #wait for server process for SERVER_START_TIMEOUT seconds
  sys.stdout.write('Waiting for server start...')
  sys.stdout.flush()

  pids = looking_for_pid(SERVER_SEARCH_PATTERN, SERVER_INIT_TIMEOUT)
  found_pids = wait_for_pid(pids, SERVER_START_TIMEOUT)

  sys.stdout.write('\n')
  sys.stdout.flush()

  if found_pids <= 0:
    exitcode = check_exitcode(os.path.join(configDefaults.PID_DIR, EXITCODE_NAME))
    raise FatalException(-1, AMBARI_SERVER_DIE_MSG.format(exitcode, configDefaults.SERVER_OUT_FILE))
  else:
    save_main_pid_ex(pids, pidfile, [locate_file('sh', '/bin'),
                                 locate_file('bash', '/bin')], True)


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
    pid_file_path = os.path.join(configDefaults.PID_DIR, PID_NAME)
    os.remove(pid_file_path)
    print "Ambari Server stopped"
  else:
    print "Ambari Server is not running"


def compare_versions(version1, version2):
  def normalize(v):
    return [int(x) for x in re.sub(r'(\.0+)*$', '', v).split(".")]
  return cmp(normalize(version1), normalize(version2))
  pass


#
# The Ambari Server status.
#
def status(args):
  args.exit_message = None
  status, pid = is_server_runing()
  pid_file_path = os.path.join(configDefaults.PID_DIR, PID_NAME)
  if status:
    print "Ambari Server running"
    print "Found Ambari Server PID: " + str(pid) + " at: " + pid_file_path
  else:
    print "Ambari Server not running. Stale PID File at: " + pid_file_path


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

  if get_verbose():
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
        new_spec['names'] = names.replace('\n', '').replace('\t', '')
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
                        "authentication.ldap.dnAttribute",
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
  LDAP_DN_ATT_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[8], "dn")
  LDAP_BASE_DN_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[9])
  LDAP_BIND_DEFAULT = get_value_from_properties(properties, ldap_property_list_reqd[10], "false")
  LDAP_MGR_DN_DEFAULT = get_value_from_properties(properties, ldap_property_list_opt[0])
  SSL_TRUSTSTORE_TYPE_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_TYPE_PROPERTY, "jks")
  SSL_TRUSTSTORE_PATH_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_PATH_PROPERTY)


  ldap_properties_map_reqd =\
  {
    ldap_property_list_reqd[0]:(LDAP_PRIMARY_URL_DEFAULT, "Primary URL* {{host:port}} {0}: ".format(get_prompt_default(LDAP_PRIMARY_URL_DEFAULT)), False),
    ldap_property_list_reqd[1]:(LDAP_SECONDARY_URL_DEFAULT, "Secondary URL {{host:port}} {0}: ".format(get_prompt_default(LDAP_SECONDARY_URL_DEFAULT)), True),
    ldap_property_list_reqd[2]:(LDAP_USE_SSL_DEFAULT, "Use SSL* [true/false] {0}: ".format(get_prompt_default(LDAP_USE_SSL_DEFAULT)), False),
    ldap_property_list_reqd[3]:(LDAP_USER_CLASS_DEFAULT, "User object class* {0}: ".format(get_prompt_default(LDAP_USER_CLASS_DEFAULT)), False),
    ldap_property_list_reqd[4]:(LDAP_USER_ATT_DEFAULT, "User name attribute* {0}: ".format(get_prompt_default(LDAP_USER_ATT_DEFAULT)), False),
    ldap_property_list_reqd[5]:(LDAP_GROUP_CLASS_DEFAULT, "Group object class* {0}: ".format(get_prompt_default(LDAP_GROUP_CLASS_DEFAULT)), False),
    ldap_property_list_reqd[6]:(LDAP_GROUP_ATT_DEFAULT, "Group name attribute* {0}: ".format(get_prompt_default(LDAP_GROUP_ATT_DEFAULT)), False),
    ldap_property_list_reqd[7]:(LDAP_GROUP_MEMBER_DEFAULT, "Group member attribute* {0}: ".format(get_prompt_default(LDAP_GROUP_MEMBER_DEFAULT)), False),
    ldap_property_list_reqd[8]:(LDAP_DN_ATT_DEFAULT, "Distinguished name attribute* {0}: ".format(get_prompt_default(LDAP_DN_ATT_DEFAULT)), False),
    ldap_property_list_reqd[9]:(LDAP_BASE_DN_DEFAULT, "Base DN* {0}: ".format(get_prompt_default(LDAP_BASE_DN_DEFAULT)), False),
    ldap_property_list_reqd[10]:(LDAP_BIND_DEFAULT, "Bind anonymously* [true/false] {0}: ".format(get_prompt_default(LDAP_BIND_DEFAULT)), False),
  }

  ldap_property_value_map = {}
  for idx, key in enumerate(ldap_property_list_reqd):
    if idx in [0, 1]:
      pattern = REGEX_HOSTNAME_PORT
    elif idx in [2, 10]:
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


def get_alias_string(alias):
  return "${alias=" + alias + "}"


def get_alias_from_alias_string(aliasStr):
  return aliasStr[8:-1]


def save_passwd_for_alias(alias, passwd, masterKey=""):
  if alias and passwd:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
      return 1

    if masterKey is None or masterKey == "":
      masterKey = "None"

    command = SECURITY_PROVIDER_PUT_CMD.format(get_java_exe_path(),
      get_full_ambari_classpath(), alias, passwd, masterKey)
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
                      "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
      return 1
    command = SECURITY_PROVIDER_KEY_CMD.format(get_java_exe_path(),
      get_full_ambari_classpath(), master_key, key_location, persist)
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
  if not get_silent():
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


def setup_component_https(component, command, property, alias):

  if not get_silent():

    jdk_path = find_jdk()
    if jdk_path is None:
      err = "No JDK found, please run the \"ambari-server setup\" " \
                      "command to install a JDK automatically or install any " \
                      "JDK manually to " + configDefaults.JDK_INSTALL_DIR
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
    return socket.getfqdn().lower()


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
  print '  [3] Encrypt passwords stored in ambari.properties file.'
  print '  [4] Setup Ambari kerberos JAAS configuration.'
  print '=' * 75
  choice = get_validated_string_input('Enter choice, (1-4): ', '0', '[1-4]',
                                      'Invalid choice', False, False)

  if choice == '1':
    need_restart = setup_https(args)
  elif choice == '2':
    setup_component_https("Ganglia", "setup-ganglia-https", GANGLIA_HTTPS,
                         "ganglia_cert")
  elif choice == '3':
    setup_master_key()
  elif choice == '4':
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
                                                       verbose=get_verbose())
    resource_files_keeper.perform_housekeeping()
  except KeeperException, ex:
    msg = "Can not organize resource files at {0}: {1}".format(
                                                resources_location, str(ex))
    raise FatalException(-1, msg)

def backup(path):
  print "Backup requested."
  backup_command = ["BackupRestore", 'backup']
  if not path is None:
    backup_command.append(path)

  BackupRestore_main(backup_command)

def restore(path):
  print "Restore requested."
  restore_command = ["BackupRestore", 'restore']
  if not path is None:
    restore_command.append(path)

  BackupRestore_main(restore_command)

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
  parser.add_option('-y', '--suspend-start', action="store_true", dest='suspend_start', default=False,
                    help="Freeze ambari-server Java process at startup in debug mode")

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
  set_verbose(options.verbose)

  # set silent
  set_silent(options.silent)

  # debug mode
  set_debug_mode_from_options(options)

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

  #perform checks

  options.warnings = []

  if options.dbms is None \
    and options.database_host is None \
    and options.database_port is None \
    and options.database_name is None \
    and options.database_username is None \
    and options.database_password is None:

    options.must_set_database_options = True

  elif not (options.dbms is not None
    and options.database_host is not None
    and options.database_port is not None
    and options.database_name is not None
    and options.database_username is not None
    and options.database_password is not None):
    parser.error('All database options should be set. Please see help for the options.')

  else:
    options.must_set_database_options = False

  #correct database
  if options.dbms == 'embedded':
    print "WARNING: HostName for postgres server " + options.database_host + \
          " will be ignored: using localhost."
    options.database_host = "localhost"
    options.dbms = 'postgres'
    options.persistence_type = 'local'
    options.database_index = 0
    pass
  elif options.dbms is not None and options.dbms not in DATABASE_NAMES:
    parser.print_help()
    parser.error("Unsupported Database " + options.dbms)
  elif options.dbms is not None:
    options.dbms = options.dbms.lower()

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
  elif action == BACKUP_ACTION or action == RESTORE_ACTION:
    possible_args_numbers = [1,2]
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
    elif action == BACKUP_ACTION:
      if len(args) == 2:
        path = args[1]
      else:
        path = None
      backup(path)
    elif action == RESTORE_ACTION:
      if len(args) == 2:
        path = args[1]
      else:
        path = None
      restore(path)
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


if __name__ == "__main__":
  try:
    main()
  except (KeyboardInterrupt, EOFError):
    print("\nAborting ... Keyboard Interrupt.")
    sys.exit(1)
