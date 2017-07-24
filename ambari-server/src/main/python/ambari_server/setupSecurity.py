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
import base64
import fileinput
import getpass
import stat
import tempfile
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import os
import re
import shutil
import urllib2
import time
import sys
import logging

from ambari_commons.exceptions import FatalException, NonFatalException
from ambari_commons.logging_utils import print_warning_msg, print_error_msg, print_info_msg, get_verbose
from ambari_commons.os_check import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from ambari_commons.os_utils import is_root, set_file_permissions, \
  run_os_command, search_file, is_valid_filepath, change_owner, get_ambari_repo_file_full_name, get_file_owner
from ambari_server.serverConfiguration import configDefaults, parse_properties_file, \
  encrypt_password, find_jdk, find_properties_file, get_alias_string, get_ambari_properties, get_conf_dir, \
  get_credential_store_location, get_is_persisted, get_is_secure, get_master_key_location, get_db_type, write_property, \
  get_original_master_key, get_value_from_properties, get_java_exe_path, is_alias_string, read_ambari_user, \
  read_passwd_for_alias, remove_password_file, save_passwd_for_alias, store_password_file, update_properties_2, \
  BLIND_PASSWORD, BOOTSTRAP_DIR_PROPERTY, IS_LDAP_CONFIGURED, JDBC_PASSWORD_FILENAME, JDBC_PASSWORD_PROPERTY, \
  JDBC_RCA_PASSWORD_ALIAS, JDBC_RCA_PASSWORD_FILE_PROPERTY, JDBC_USE_INTEGRATED_AUTH_PROPERTY, \
  LDAP_MGR_PASSWORD_ALIAS, LDAP_MGR_PASSWORD_FILENAME, LDAP_MGR_PASSWORD_PROPERTY, LDAP_MGR_USERNAME_PROPERTY, \
  LDAP_PRIMARY_URL_PROPERTY, SECURITY_IS_ENCRYPTION_ENABLED, SECURITY_KEY_ENV_VAR_NAME, SECURITY_KERBEROS_JASS_FILENAME, \
  SECURITY_PROVIDER_KEY_CMD, SECURITY_MASTER_KEY_FILENAME, SSL_TRUSTSTORE_PASSWORD_ALIAS, \
  SSL_TRUSTSTORE_PASSWORD_PROPERTY, SSL_TRUSTSTORE_PATH_PROPERTY, SSL_TRUSTSTORE_TYPE_PROPERTY, \
  SSL_API, SSL_API_PORT, DEFAULT_SSL_API_PORT, CLIENT_API_PORT, JDK_NAME_PROPERTY, JCE_NAME_PROPERTY, JAVA_HOME_PROPERTY, \
  get_resources_location, SECURITY_MASTER_KEY_LOCATION, SETUP_OR_UPGRADE_MSG, CHECK_AMBARI_KRB_JAAS_CONFIGURATION_PROPERTY
from ambari_server.serverUtils import is_server_runing, get_ambari_server_api_base
from ambari_server.setupActions import SETUP_ACTION, LDAP_SETUP_ACTION
from ambari_server.userInput import get_validated_string_input, get_prompt_default, read_password, get_YN_input, quit_if_has_answer
from ambari_server.serverClassPath import ServerClassPath
from ambari_server.dbConfiguration import DBMSConfigFactory, check_jdbc_drivers, \
  get_jdbc_driver_path, ensure_jdbc_driver_is_installed, LINUX_DBMS_KEYS_LIST

logger = logging.getLogger(__name__)

REGEX_IP_ADDRESS = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$"
REGEX_HOSTNAME = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]*[a-zA-Z0-9])\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\-]*[A-Za-z0-9])$"
REGEX_HOSTNAME_PORT = "^(.*:[0-9]{1,5}$)"
REGEX_TRUE_FALSE = "^(true|false)?$"
REGEX_SKIP_CONVERT = "^(skip|convert)?$"
REGEX_REFERRAL = "^(follow|ignore)?$"
REGEX_ANYTHING = ".*"
LDAP_TO_PAM_MIGRATION_HELPER_CMD = "{0} -cp {1} " + \
                                   "org.apache.ambari.server.security.authentication.LdapToPamMigrationHelper" + \
                                   " >> " + configDefaults.SERVER_OUT_FILE + " 2>&1"

CLIENT_SECURITY_KEY = "client.security"

AUTO_GROUP_CREATION = "auto.group.creation"

SERVER_API_LDAP_URL = 'ldap_sync_events'

PAM_CONFIG_FILE = 'pam.configuration'


def read_master_key(isReset=False, options = None):
  passwordPattern = ".*"
  passwordPrompt = "Please provide master key for locking the credential store: "
  passwordDescr = "Invalid characters in password. Use only alphanumeric or "\
                  "_ or - characters"
  passwordDefault = ""
  if isReset:
    passwordPrompt = "Enter new Master Key: "

  input = True
  while(input):
    masterKey = get_validated_string_input(passwordPrompt, passwordDefault, passwordPattern, passwordDescr,
                                           True, True, answer = options.master_key)

    if not masterKey:
      print "Master Key cannot be empty!"
      continue

    masterKey2 = get_validated_string_input("Re-enter master key: ", passwordDefault, passwordPattern, passwordDescr,
                                            True, True, answer = options.master_key)

    if masterKey != masterKey2:
      print "Master key did not match!"
      continue

    input = False

  return masterKey

def save_master_key(options, master_key, key_location, persist=True):
  if master_key:
    jdk_path = find_jdk()
    if jdk_path is None:
      print_error_msg("No JDK found, please run the \"setup\" "
                      "command to install a JDK automatically or install any "
                      "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
      return 1
    serverClassPath = ServerClassPath(get_ambari_properties(), options)
    command = SECURITY_PROVIDER_KEY_CMD.format(get_java_exe_path(),
      serverClassPath.get_full_ambari_classpath_escaped_for_shell(), master_key, key_location, persist)
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from credential provider save KEY: " +
                   str(retcode))
  else:
    print_error_msg("Master key cannot be None.")


def adjust_directory_permissions(ambari_user):
  properties = get_ambari_properties()

  bootstrap_dir = os.path.abspath(get_value_from_properties(properties, BOOTSTRAP_DIR_PROPERTY))
  print_info_msg("Cleaning bootstrap directory ({0}) contents...".format(bootstrap_dir))

  if os.path.exists(bootstrap_dir):
    shutil.rmtree(bootstrap_dir) #Ignore the non-existent dir error

  if not os.path.exists(bootstrap_dir):
    try:
      os.makedirs(bootstrap_dir)
    except Exception, ex:
      print_warning_msg("Failed recreating the bootstrap directory: {0}".format(str(ex)))
      pass
  else:
    print_warning_msg("Bootstrap directory lingering around after 5s. Unable to complete the cleanup.")
  pass

  # Add master key and credential store if exists
  keyLocation = get_master_key_location(properties)
  masterKeyFile = search_file(SECURITY_MASTER_KEY_FILENAME, keyLocation)
  if masterKeyFile:
    configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((masterKeyFile, configDefaults.MASTER_KEY_FILE_PERMISSIONS, "{0}", False))
  credStoreFile = get_credential_store_location(properties)
  if os.path.exists(credStoreFile):
    configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((credStoreFile, configDefaults.CREDENTIALS_STORE_FILE_PERMISSIONS, "{0}", False))
  trust_store_location = properties[SSL_TRUSTSTORE_PATH_PROPERTY]
  if trust_store_location:
    configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((trust_store_location, configDefaults.TRUST_STORE_LOCATION_PERMISSIONS, "{0}", False))

  # Update JDK and JCE permissions
  resources_dir = get_resources_location(properties)
  jdk_file_name = properties.get_property(JDK_NAME_PROPERTY)
  jce_file_name = properties.get_property(JCE_NAME_PROPERTY)
  java_home = properties.get_property(JAVA_HOME_PROPERTY)
  if jdk_file_name:
    jdk_file_path = os.path.abspath(os.path.join(resources_dir, jdk_file_name))
    if(os.path.exists(jdk_file_path)):
      configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((jdk_file_path, "644", "{0}", False))
  if jce_file_name:
    jce_file_path = os.path.abspath(os.path.join(resources_dir, jce_file_name))
    if(os.path.exists(jce_file_path)):
      configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((jce_file_path, "644", "{0}", False))
  if java_home:
    jdk_security_dir = os.path.abspath(os.path.join(java_home, configDefaults.JDK_SECURITY_DIR))
    if(os.path.exists(jdk_security_dir)):
      configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((jdk_security_dir + "/*", "644", "{0}", True))
      configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((jdk_security_dir, "755", "{0}", False))

  # Grant read permissions to all users. This is required when a non-admin user is configured to setup ambari-server.
  # However, do not change ownership of the repo file to ambari user.

  ambari_repo_file = get_ambari_repo_file_full_name()

  if ambari_repo_file:
    if (os.path.exists(ambari_repo_file)):
        ambari_repo_file_owner = get_file_owner(ambari_repo_file)
        configDefaults.NR_ADJUST_OWNERSHIP_LIST.append((ambari_repo_file, "644", ambari_repo_file_owner, False))


  print "Adjusting ambari-server permissions and ownership..."

  for pack in configDefaults.NR_ADJUST_OWNERSHIP_LIST:
    file = pack[0]
    mod = pack[1]
    user = pack[2].format(ambari_user)
    recursive = pack[3]
    print_info_msg("Setting file permissions: {0} {1} {2} {3}".format(file, mod, user, recursive))
    set_file_permissions(file, mod, user, recursive)

  for pack in configDefaults.NR_CHANGE_OWNERSHIP_LIST:
    path = pack[0]
    user = pack[1].format(ambari_user)
    recursive = pack[2]
    print_info_msg("Changing ownership: {0} {1} {2}".format(path, user, recursive))
    change_owner(path, user, recursive)

def configure_ldap_password(options):
  passwordDefault = ""
  passwordPrompt = 'Enter Manager Password* : '
  passwordPattern = ".*"
  passwordDescr = "Invalid characters in password."
  password = read_password(passwordDefault, passwordPattern, passwordPrompt, passwordDescr, options.ldap_manager_password)

  return password

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


class LdapSyncOptions:
  def __init__(self, options):
    try:
      self.ldap_sync_all = options.ldap_sync_all
    except AttributeError:
      self.ldap_sync_all = False

    try:
      self.ldap_sync_existing = options.ldap_sync_existing
    except AttributeError:
      self.ldap_sync_existing = False

    try:
      self.ldap_sync_users = options.ldap_sync_users
    except AttributeError:
      self.ldap_sync_users = None

    try:
      self.ldap_sync_groups = options.ldap_sync_groups
    except AttributeError:
      self.ldap_sync_groups = None

    try:
      self.ldap_sync_admin_name = options.ldap_sync_admin_name
    except AttributeError:
      self.ldap_sync_admin_name = None

    try:
      self.ldap_sync_admin_password = options.ldap_sync_admin_password
    except AttributeError:
      self.ldap_sync_admin_password = None

  def no_ldap_sync_options_set(self):
    return not self.ldap_sync_all and not self.ldap_sync_existing and self.ldap_sync_users is None and self.ldap_sync_groups is None


#
# Sync users and groups with configured LDAP
#
def sync_ldap(options):
  logger.info("Sync users and groups with configured LDAP.")

  properties = get_ambari_properties()

  if get_value_from_properties(properties,CLIENT_SECURITY_KEY,"") == 'pam':
    err = "PAM is configured. Can not sync LDAP."
    raise FatalException(1, err)

  server_status, pid = is_server_runing()
  if not server_status:
    err = 'Ambari Server is not running.'
    raise FatalException(1, err)

  if properties == -1:
    raise FatalException(1, "Failed to read properties file.")

  ldap_configured = properties.get_property(IS_LDAP_CONFIGURED)
  if ldap_configured != 'true':
    err = "LDAP is not configured. Run 'ambari-server setup-ldap' first."
    raise FatalException(1, err)

  # set ldap sync options
  ldap_sync_options = LdapSyncOptions(options)

  if ldap_sync_options.no_ldap_sync_options_set():
    err = 'Must specify a sync option (all, existing, users or groups).  Please invoke ambari-server.py --help to print the options.'
    raise FatalException(1, err)

  admin_login = ldap_sync_options.ldap_sync_admin_name\
    if ldap_sync_options.ldap_sync_admin_name is not None and ldap_sync_options.ldap_sync_admin_name \
    else get_validated_string_input(prompt="Enter Ambari Admin login: ", default=None,
                                           pattern=None, description=None,
                                           is_pass=False, allowEmpty=False)
  admin_password = ldap_sync_options.ldap_sync_admin_password \
    if ldap_sync_options.ldap_sync_admin_password is not None and ldap_sync_options.ldap_sync_admin_password \
    else get_validated_string_input(prompt="Enter Ambari Admin password: ", default=None,
                                              pattern=None, description=None,
                                              is_pass=True, allowEmpty=False)

  url = get_ambari_server_api_base(properties) + SERVER_API_LDAP_URL
  admin_auth = base64.encodestring('%s:%s' % (admin_login, admin_password)).replace('\n', '')
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')

  if ldap_sync_options.ldap_sync_all:
    sys.stdout.write('Syncing all.')
    bodies = [{"Event":{"specs":[{"principal_type":"users","sync_type":"all"},{"principal_type":"groups","sync_type":"all"}]}}]
  elif ldap_sync_options.ldap_sync_existing:
    sys.stdout.write('Syncing existing.')
    bodies = [{"Event":{"specs":[{"principal_type":"users","sync_type":"existing"},{"principal_type":"groups","sync_type":"existing"}]}}]
  else:
    sys.stdout.write('Syncing specified users and groups.')
    bodies = [{"Event":{"specs":[]}}]
    body = bodies[0]
    events = body['Event']
    specs = events['specs']

    if ldap_sync_options.ldap_sync_users is not None:
      new_specs = [{"principal_type":"users","sync_type":"specific","names":""}]
      get_ldap_event_spec_names(ldap_sync_options.ldap_sync_users, specs, new_specs)
    if ldap_sync_options.ldap_sync_groups is not None:
      new_specs = [{"principal_type":"groups","sync_type":"specific","names":""}]
      get_ldap_event_spec_names(ldap_sync_options.ldap_sync_groups, specs, new_specs)

  if get_verbose():
    sys.stdout.write('\nCalling API ' + url + ' : ' + str(bodies) + '\n')

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

def setup_master_key(options):
  if not is_root():
    warn = 'ambari-server setup-https is run as ' \
          'non-root user, some sudo privileges might be required'
    print warn

  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, "Failed to read properties file.")

  db_windows_auth_prop = properties.get_property(JDBC_USE_INTEGRATED_AUTH_PROPERTY)
  db_sql_auth = False if db_windows_auth_prop and db_windows_auth_prop.lower() == 'true' else True
  db_password = properties.get_property(JDBC_PASSWORD_PROPERTY)
  # Encrypt passwords cannot be called before setup
  if db_sql_auth and not db_password:
    print 'Please call "setup" before "encrypt-passwords". Exiting...'
    return 1

  # Check configuration for location of master key
  isSecure = get_is_secure(properties)
  (isPersisted, masterKeyFile) = get_is_persisted(properties)

  # Read clear text DB password from file
  if db_sql_auth and not is_alias_string(db_password) and os.path.isfile(db_password):
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
    resetKey = True if options.security_option is not None else get_YN_input("Do you want to reset Master Key? [y/n] (n): ", False)

  # For encrypting of only unencrypted passwords without resetting the key ask
  # for master key if not persisted.
  if isSecure and not isPersisted and not resetKey:
    print "Master Key not persisted."
    masterKey = get_original_master_key(properties, options)
  pass

  # Make sure both passwords are clear-text if master key is lost
  if resetKey:
    if not isPersisted:
      print "Master Key not persisted."
      masterKey = get_original_master_key(properties, options)
      # Unable get the right master key or skipped question <enter>
      if not masterKey:
        print "To disable encryption, do the following:"
        print "- Edit " + find_properties_file() + \
              " and set " + SECURITY_IS_ENCRYPTION_ENABLED + " = " + "false."
        err = "{0} is already encrypted. Please call {1} to store unencrypted" \
              " password and call 'encrypt-passwords' again."
        if db_sql_auth and db_password and is_alias_string(db_password):
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
  if db_sql_auth  and db_password and is_alias_string(db_password):
    db_password = read_passwd_for_alias(JDBC_RCA_PASSWORD_ALIAS, masterKey)
  if ldap_password and is_alias_string(ldap_password):
    ldap_password = read_passwd_for_alias(LDAP_MGR_PASSWORD_ALIAS, masterKey)
  if ts_password and is_alias_string(ts_password):
    ts_password = read_passwd_for_alias(SSL_TRUSTSTORE_PASSWORD_ALIAS, masterKey)
  # Read master key, if non-secure or reset is true
  if resetKey or not isSecure:
    masterKey = read_master_key(resetKey, options)
    persist = get_YN_input("Do you want to persist master key. If you choose " \
                           "not to persist, you need to provide the Master " \
                           "Key while starting the ambari server as an env " \
                           "variable named " + SECURITY_KEY_ENV_VAR_NAME + \
                           " or the start will prompt for the master key."
                           " Persist [y/n] (y)? ", True, options.master_key_persist)
    if persist:
      save_master_key(options, masterKey, get_master_key_location(properties) + os.sep +
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

  update_properties_2(properties, propertyMap)

  # Since files for store and master are created we need to ensure correct
  # permissions
  ambari_user = read_ambari_user()
  if ambari_user:
    adjust_directory_permissions(ambari_user)

  return 0

def setup_ambari_krb5_jaas(options):
  jaas_conf_file = search_file(SECURITY_KERBEROS_JASS_FILENAME, get_conf_dir())
  if os.path.exists(jaas_conf_file):
    print 'Setting up Ambari kerberos JAAS configuration to access ' + \
          'secured Hadoop daemons...'
    principal = get_validated_string_input('Enter ambari server\'s kerberos '
                                 'principal name (ambari@EXAMPLE.COM): ', 'ambari@EXAMPLE.COM', '.*', '', False,
                                 False, answer = options.jaas_principal)
    keytab = get_validated_string_input('Enter keytab path for ambari '
                                 'server\'s kerberos principal: ',
                                 '/etc/security/keytabs/ambari.keytab', '.*', False, False,
                                  validatorFunction=is_valid_filepath, answer = options.jaas_keytab)

    for line in fileinput.FileInput(jaas_conf_file, inplace=1):
      line = re.sub('keyTab=.*$', 'keyTab="' + keytab + '"', line)
      line = re.sub('principal=.*$', 'principal="' + principal + '"', line)
      print line,

    write_property(CHECK_AMBARI_KRB_JAAS_CONFIGURATION_PROPERTY, "true")
  else:
    raise NonFatalException('No jaas config file found at location: ' +
                            jaas_conf_file)


class LdapPropTemplate:
  def __init__(self, properties, i_option, i_prop_name, i_prop_val_pattern, i_prompt_regex, i_allow_empty_prompt, i_prop_name_default=None):
    self.prop_name = i_prop_name
    self.option = i_option
    self.ldap_prop_name = get_value_from_properties(properties, i_prop_name, i_prop_name_default)
    self.ldap_prop_val_prompt = i_prop_val_pattern.format(get_prompt_default(self.ldap_prop_name))
    self.prompt_regex = i_prompt_regex
    self.allow_empty_prompt = i_allow_empty_prompt

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def init_ldap_properties_list_reqd(properties, options):
  # python2.x dict is not ordered
  ldap_properties = [
    LdapPropTemplate(properties, options.ldap_url, "authentication.ldap.primaryUrl", "Primary URL* {{host:port}} {0}: ", REGEX_HOSTNAME_PORT, False),
    LdapPropTemplate(properties, options.ldap_secondary_url, "authentication.ldap.secondaryUrl", "Secondary URL {{host:port}} {0}: ", REGEX_HOSTNAME_PORT, True),
    LdapPropTemplate(properties, options.ldap_ssl, "authentication.ldap.useSSL", "Use SSL* [true/false] {0}: ", REGEX_TRUE_FALSE, False, "false"),
    LdapPropTemplate(properties, options.ldap_user_attr, "authentication.ldap.usernameAttribute", "User name attribute* {0}: ", REGEX_ANYTHING, False, "uid"),
    LdapPropTemplate(properties, options.ldap_base_dn, "authentication.ldap.baseDn", "Base DN* {0}: ", REGEX_ANYTHING, False),
    LdapPropTemplate(properties, options.ldap_referral, "authentication.ldap.referral", "Referral method [follow/ignore] {0}: ", REGEX_REFERRAL, True),
    LdapPropTemplate(properties, options.ldap_bind_anonym, "authentication.ldap.bindAnonymously" "Bind anonymously* [true/false] {0}: ", REGEX_TRUE_FALSE, False, "false")
  ]
  return ldap_properties

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def init_ldap_properties_list_reqd(properties, options):
  ldap_properties = [
    LdapPropTemplate(properties, options.ldap_url, LDAP_PRIMARY_URL_PROPERTY, "Primary URL* {{host:port}} {0}: ", REGEX_HOSTNAME_PORT, False),
    LdapPropTemplate(properties, options.ldap_secondary_url, "authentication.ldap.secondaryUrl", "Secondary URL {{host:port}} {0}: ", REGEX_HOSTNAME_PORT, True),
    LdapPropTemplate(properties, options.ldap_ssl, "authentication.ldap.useSSL", "Use SSL* [true/false] {0}: ", REGEX_TRUE_FALSE, False, "false"),
    LdapPropTemplate(properties, options.ldap_user_class, "authentication.ldap.userObjectClass", "User object class* {0}: ", REGEX_ANYTHING, False, "posixAccount"),
    LdapPropTemplate(properties, options.ldap_user_attr, "authentication.ldap.usernameAttribute", "User name attribute* {0}: ", REGEX_ANYTHING, False, "uid"),
    LdapPropTemplate(properties, options.ldap_group_class, "authentication.ldap.groupObjectClass", "Group object class* {0}: ", REGEX_ANYTHING, False, "posixGroup"),
    LdapPropTemplate(properties, options.ldap_group_attr, "authentication.ldap.groupNamingAttr", "Group name attribute* {0}: ", REGEX_ANYTHING, False, "cn"),
    LdapPropTemplate(properties, options.ldap_member_attr, "authentication.ldap.groupMembershipAttr", "Group member attribute* {0}: ", REGEX_ANYTHING, False, "memberUid"),
    LdapPropTemplate(properties, options.ldap_dn, "authentication.ldap.dnAttribute", "Distinguished name attribute* {0}: ", REGEX_ANYTHING, False, "dn"),
    LdapPropTemplate(properties, options.ldap_base_dn, "authentication.ldap.baseDn", "Base DN* {0}: ", REGEX_ANYTHING, False),
    LdapPropTemplate(properties, options.ldap_referral, "authentication.ldap.referral", "Referral method [follow/ignore] {0}: ", REGEX_REFERRAL, True),
    LdapPropTemplate(properties, options.ldap_bind_anonym, "authentication.ldap.bindAnonymously", "Bind anonymously* [true/false] {0}: ", REGEX_TRUE_FALSE, False, "false"),
    LdapPropTemplate(properties, options.ldap_sync_username_collisions_behavior, "ldap.sync.username.collision.behavior", "Handling behavior for username collisions [convert/skip] for LDAP sync* {0}: ", REGEX_SKIP_CONVERT, False, "convert"),
  ]
  return ldap_properties

def setup_ldap(options):
  logger.info("Setup LDAP.")
  if not is_root():
    err = 'Ambari-server setup-ldap should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)

  properties = get_ambari_properties()

  if get_value_from_properties(properties,CLIENT_SECURITY_KEY,"") == 'pam':
    query = "PAM is currently configured, do you wish to use LDAP instead [y/n] (n)? "
    if get_YN_input(query, False):
      pass
    else:
      err = "PAM is configured. Can not setup LDAP."
      raise FatalException(1, err)

  isSecure = get_is_secure(properties)

  ldap_property_list_reqd = init_ldap_properties_list_reqd(properties, options)

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

  LDAP_MGR_DN_DEFAULT = get_value_from_properties(properties, ldap_property_list_opt[0])

  SSL_TRUSTSTORE_TYPE_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_TYPE_PROPERTY, "jks")
  SSL_TRUSTSTORE_PATH_DEFAULT = get_value_from_properties(properties, SSL_TRUSTSTORE_PATH_PROPERTY)

  ldap_property_value_map = {}
  for ldap_prop in ldap_property_list_reqd:
    input = get_validated_string_input(ldap_prop.ldap_prop_val_prompt, ldap_prop.ldap_prop_name, ldap_prop.prompt_regex,
                                       "Invalid characters in the input!", False, ldap_prop.allow_empty_prompt,
                                       answer = ldap_prop.option)
    if input is not None and input != "":
      ldap_property_value_map[ldap_prop.prop_name] = input

  bindAnonymously = ldap_property_value_map["authentication.ldap.bindAnonymously"]
  anonymous = (bindAnonymously and bindAnonymously.lower() == 'true')
  mgr_password = None
  # Ask for manager credentials only if bindAnonymously is false
  if not anonymous:
    username = get_validated_string_input("Manager DN* {0}: ".format(
     get_prompt_default(LDAP_MGR_DN_DEFAULT)), LDAP_MGR_DN_DEFAULT, ".*",
      "Invalid characters in the input!", False, False, answer = options.ldap_manager_dn)
    ldap_property_value_map[LDAP_MGR_USERNAME_PROPERTY] = username
    mgr_password = configure_ldap_password(options)
    ldap_property_value_map[LDAP_MGR_PASSWORD_PROPERTY] = mgr_password

  useSSL = ldap_property_value_map["authentication.ldap.useSSL"]
  ldaps = (useSSL and useSSL.lower() == 'true')
  ts_password = None

  if ldaps:
    truststore_default = "n"
    truststore_set = bool(SSL_TRUSTSTORE_PATH_DEFAULT)
    if truststore_set:
      truststore_default = "y"
    custom_trust_store = True if options.trust_store_path is not None and options.trust_store_path else False
    if not custom_trust_store:
      custom_trust_store = get_YN_input("Do you want to provide custom TrustStore for Ambari [y/n] ({0})?".
                                      format(truststore_default),
                                      truststore_set)
    if custom_trust_store:
      ts_type = get_validated_string_input("TrustStore type [jks/jceks/pkcs12] {0}:".format(get_prompt_default(SSL_TRUSTSTORE_TYPE_DEFAULT)),
        SSL_TRUSTSTORE_TYPE_DEFAULT, "^(jks|jceks|pkcs12)?$", "Wrong type", False, answer=options.trust_store_type)
      ts_path = None
      while True:
        ts_path = get_validated_string_input("Path to TrustStore file {0}:".format(get_prompt_default(SSL_TRUSTSTORE_PATH_DEFAULT)),
          SSL_TRUSTSTORE_PATH_DEFAULT, ".*", False, False, answer = options.trust_store_path)
        if os.path.exists(ts_path):
          break
        else:
          print 'File not found.'
          hasAnswer = options.trust_store_path is not None and options.trust_store_path
          quit_if_has_answer(hasAnswer)

      ts_password = read_password("", ".*", "Password for TrustStore:", "Invalid characters in password", options.trust_store_password)

      ldap_property_value_map[SSL_TRUSTSTORE_TYPE_PROPERTY] = ts_type
      ldap_property_value_map[SSL_TRUSTSTORE_PATH_PROPERTY] = ts_path
      ldap_property_value_map[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = ts_password
      pass
    elif properties.get_property(SSL_TRUSTSTORE_TYPE_PROPERTY):
      print 'The TrustStore is already configured: '
      print '  ' + SSL_TRUSTSTORE_TYPE_PROPERTY + ' = ' + properties.get_property(SSL_TRUSTSTORE_TYPE_PROPERTY)
      print '  ' + SSL_TRUSTSTORE_PATH_PROPERTY + ' = ' + properties.get_property(SSL_TRUSTSTORE_PATH_PROPERTY)
      print '  ' + SSL_TRUSTSTORE_PASSWORD_PROPERTY + ' = ' + properties.get_property(SSL_TRUSTSTORE_PASSWORD_PROPERTY)
      if get_YN_input("Do you want to remove these properties [y/n] (y)? ", True, options.trust_store_reconfigure):
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

  save_settings = True if options.ldap_save_settings is not None else get_YN_input("Save settings [y/n] (y)? ", True)

  if save_settings:
    ldap_property_value_map[CLIENT_SECURITY_KEY] = 'ldap'
    if isSecure:
      if mgr_password:
        encrypted_passwd = encrypt_password(LDAP_MGR_PASSWORD_ALIAS, mgr_password, options)
        if mgr_password != encrypted_passwd:
          ldap_property_value_map[LDAP_MGR_PASSWORD_PROPERTY] = encrypted_passwd
      pass
      if ts_password:
        encrypted_passwd = encrypt_password(SSL_TRUSTSTORE_PASSWORD_ALIAS, ts_password, options)
        if ts_password != encrypted_passwd:
          ldap_property_value_map[SSL_TRUSTSTORE_PASSWORD_PROPERTY] = encrypted_passwd
      pass
    pass

    # Persisting values
    ldap_property_value_map[IS_LDAP_CONFIGURED] = "true"
    if mgr_password:
      ldap_property_value_map[LDAP_MGR_PASSWORD_PROPERTY] = store_password_file(mgr_password, LDAP_MGR_PASSWORD_FILENAME)
    update_properties_2(properties, ldap_property_value_map)
    print 'Saving...done'

  return 0

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def generate_env(options, ambari_user, current_user):
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
      environ[SECURITY_KEY_ENV_VAR_NAME] = masterKey
      tempDir = tempfile.gettempdir()
      tempFilePath = tempDir + os.sep + "masterkey"
      save_master_key(options, masterKey, tempFilePath, True)
      if ambari_user != current_user:
        uid = pwd.getpwnam(ambari_user).pw_uid
        gid = pwd.getpwnam(ambari_user).pw_gid
        os.chown(tempFilePath, uid, gid)
      else:
        os.chmod(tempFilePath, stat.S_IREAD | stat.S_IWRITE)

      if tempFilePath is not None:
        environ[SECURITY_MASTER_KEY_LOCATION] = tempFilePath

  return environ

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def generate_env(options, ambari_user, current_user):
  return os.environ.copy()

@OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
def ensure_can_start_under_current_user(ambari_user):
  #Ignore the requirement to run as root. In Windows, by default the child process inherits the security context
  # and the environment from the parent process.
  return ""

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def ensure_can_start_under_current_user(ambari_user):
  current_user = getpass.getuser()
  if ambari_user is None:
    err = "Unable to detect a system user for Ambari Server.\n" + SETUP_OR_UPGRADE_MSG
    raise FatalException(1, err)
  if current_user != ambari_user and not is_root():
    err = "Unable to start Ambari Server as user {0}. Please either run \"ambari-server start\" " \
          "command as root, as sudo or as user \"{1}\"".format(current_user, ambari_user)
    raise FatalException(1, err)
  return current_user

class PamPropTemplate:
  def __init__(self, properties, i_option, i_prop_name, i_prop_val_pattern, i_prompt_regex, i_allow_empty_prompt, i_prop_name_default=None):
    self.prop_name = i_prop_name
    self.option = i_option
    self.pam_prop_name = get_value_from_properties(properties, i_prop_name, i_prop_name_default)
    self.pam_prop_val_prompt = i_prop_val_pattern.format(get_prompt_default(self.pam_prop_name))
    self.prompt_regex = i_prompt_regex
    self.allow_empty_prompt = i_allow_empty_prompt

def init_pam_properties_list_reqd(properties, options):
  properties = [
    PamPropTemplate(properties, options.pam_config_file, PAM_CONFIG_FILE, "PAM configuration file* {0}: ", REGEX_ANYTHING, False, "/etc/pam.d/ambari"),
    PamPropTemplate(properties, options.pam_auto_create_groups, AUTO_GROUP_CREATION, "Do you want to allow automatic group creation* [true/false] {0}: ", REGEX_TRUE_FALSE, False, "false"),
  ]
  return properties

def setup_pam(options):
  if not is_root():
    err = 'Ambari-server setup-pam should be run with root-level privileges'
    raise FatalException(4, err)

  properties = get_ambari_properties()

  if get_value_from_properties(properties,CLIENT_SECURITY_KEY,"") == 'ldap':
    query = "LDAP is currently configured, do you wish to use PAM instead [y/n] (n)? "
    if get_YN_input(query, False):
      pass
    else:
      err = "LDAP is configured. Can not setup PAM."
      raise FatalException(1, err)

  pam_property_list_reqd = init_pam_properties_list_reqd(properties, options)

  pam_property_value_map = {}
  pam_property_value_map[CLIENT_SECURITY_KEY] = 'pam'

  for pam_prop in pam_property_list_reqd:
    input = get_validated_string_input(pam_prop.pam_prop_val_prompt, pam_prop.pam_prop_name, pam_prop.prompt_regex,
                                       "Invalid characters in the input!", False, pam_prop.allow_empty_prompt,
                                       answer = pam_prop.option)
    if input is not None and input != "":
      pam_property_value_map[pam_prop.prop_name] = input

  # Verify that the PAM config file exists, else show warning...
  pam_config_file = pam_property_value_map[PAM_CONFIG_FILE]
  if not os.path.exists(pam_config_file):
    print_warning_msg("The PAM configuration file, {0} does not exist.  " \
                      "Please create it before restarting Ambari.".format(pam_config_file))

  update_properties_2(properties, pam_property_value_map)
  print 'Saving...done'
  return 0

#
# Migration of LDAP users & groups to PAM
#
def migrate_ldap_pam(args):
  properties = get_ambari_properties()

  if get_value_from_properties(properties,CLIENT_SECURITY_KEY,"") != 'pam':
    err = "PAM is not configured. Please configure PAM authentication first."
    raise FatalException(1, err)

  db_title = get_db_type(properties).title
  confirm = get_YN_input("Ambari Server configured for %s. Confirm "
                        "you have made a backup of the Ambari Server database [y/n] (y)? " % db_title, True)

  if not confirm:
    print_error_msg("Database backup is not confirmed")
    return 1

  jdk_path = get_java_exe_path()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
    return 1

  # At this point, the args does not have the ambari database information.
  # Augment the args with the correct ambari database information
  parse_properties_file(args)

  ensure_jdbc_driver_is_installed(args, properties)

  print 'Migrating LDAP Users & Groups to PAM'

  serverClassPath = ServerClassPath(properties, args)
  class_path = serverClassPath.get_full_ambari_classpath_escaped_for_shell()

  command = LDAP_TO_PAM_MIGRATION_HELPER_CMD.format(jdk_path, class_path)

  ambari_user = read_ambari_user()
  current_user = ensure_can_start_under_current_user(ambari_user)
  environ = generate_env(args, ambari_user, current_user)

  (retcode, stdout, stderr) = run_os_command(command, env=environ)
  print_info_msg("Return code from LDAP to PAM migration command, retcode = " + str(retcode))
  if stdout:
    print "Console output from LDAP to PAM migration command:"
    print stdout
    print
  if stderr:
    print "Error output from LDAP to PAM migration command:"
    print stderr
    print
  if retcode > 0:
    print_error_msg("Error executing LDAP to PAM migration, please check the server logs.")
  else:
    print_info_msg('LDAP to PAM migration completed')
  return retcode
