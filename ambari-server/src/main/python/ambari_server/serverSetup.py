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
import sys
import urllib2
from ambari_commons.inet_utils import force_download_file
from ambari_commons.logging_utils import print_warning_msg, print_error_msg

from serverConfiguration import *
from setupSecurity import adjust_directory_permissions, get_is_secure, store_password_file, encrypt_password, \
  get_is_persisted
from userInput import *
from utils import *

if OSCheck.is_windows_os():
  from serverSetup_windows import *
else:
  # MacOS not supported
  from serverSetup_linux import *


JDK_INDEX = 0

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


def check_ambari_user():
  try:
    user = read_ambari_user()
    create_user = False
    update_user_setting = False
    if user is not None:
      create_user = get_YN_input(NR_USER_CHANGE_PROMPT.format(user), False)
      update_user_setting = create_user  # Only if we will create another user
    else:  # user is not configured yet
      update_user_setting = True  # Write configuration anyway
      create_user = get_YN_input(NR_USER_CUSTOMIZE_PROMPT, False)
      if not create_user:
        user = NR_DEFAULT_USER

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

def create_custom_user():
  return os_create_custom_user()


# ## JDK ###

#
# Downloads and installs the JDK and the JCE policy archive
#
def _dowload_jdk(jdk_url, dest_file):
  jdk_download_fail_msg = " Failed to download JDK: {0}. Please check that Oracle " \
                          "JDK is available at {1}. Also you may specify JDK file " \
                          "location in local filesystem using --jdk-location command " \
                          "line argument.".format("{0}", jdk_url)
  try:
    force_download_file(jdk_url, dest_file)

    print 'Successfully downloaded JDK distribution to ' + dest_file
  except FatalException:
    raise
  except Exception, e:
    err = jdk_download_fail_msg.format(str(e))
    raise FatalException(1, err)


def download_and_install_jdk(args):
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    raise FatalException(-1, err)

  conf_file = properties.fileName
  ok = False
  jcePolicyWarn = "JCE Policy files are required for configuring Kerberos security. If you plan to use Kerberos," \
                  "please make sure JCE Unlimited Strength Jurisdiction Policy Files are valid on all hosts."

  if args.java_home:
    if not os.path.exists(args.java_home) or not os.path.isfile(os.path.join(args.java_home, JAVA_EXE_SUBPATH)):
      err = "Path to java home " + args.java_home + " or java binary file does not exists"
      raise FatalException(1, err)

    print_warning_msg("JAVA_HOME " + args.java_home + " must be valid on ALL hosts")
    print_warning_msg(jcePolicyWarn)

    properties.process_pair(JAVA_HOME_PROPERTY, args.java_home)
    properties.removeOldProp(JDK_NAME_PROPERTY)
    properties.removeOldProp(JCE_NAME_PROPERTY)
    update_properties(properties)

    os_ensure_java_home_env_var_is_set(args.java_home)
    return 0
  else:
    global JDK_INDEX

    java_home_var = get_JAVA_HOME()
    if java_home_var:
      if args.silent:
        change_jdk = False
      else:
        change_jdk = get_YN_input("Do you want to change Oracle JDK [y/n] (n)? ", False)
      if not change_jdk:
        os_ensure_java_home_env_var_is_set(java_home_var)
        return 0
    #Handle silent JDK setup when args.silent is set
    elif args.silent:
      #No java_home_var set, detect if java is already installed
      if os.environ.has_key(JAVA_HOME):
        args.java_home = os.environ[JAVA_HOME]

        properties.process_pair(JAVA_HOME_PROPERTY, args.java_home)
        properties.removeOldProp(JDK_NAME_PROPERTY)
        properties.removeOldProp(JCE_NAME_PROPERTY)
        update_properties(properties)

        os_ensure_java_home_env_var_is_set(args.java_home)
        return 0
      else:
        #Continue with the normal setup, taking the first listed JDK version as the default option
        jdk_num = "1"
        (jdks, jdk_choice_prompt, jdk_valid_choices, custom_jdk_number) = populate_jdk_configs(properties, jdk_num)
    else:
      jdk_num = str(JDK_INDEX + 1)
      (jdks, jdk_choice_prompt, jdk_valid_choices, custom_jdk_number) = populate_jdk_configs(properties, jdk_num)

      jdk_num = get_validated_string_input(
        jdk_choice_prompt,
        jdk_num,
        jdk_valid_choices,
        "Invalid number.",
        False
      )

      java_bin = "java"
      if OSCheck.is_windows_os():
        java_bin = "java.exe"

      if jdk_num == str(custom_jdk_number):
        print_warning_msg("JDK must be installed on all hosts and JAVA_HOME must be valid on all hosts.")
        print_warning_msg(jcePolicyWarn)
        args.java_home = get_validated_string_input("Path to JAVA_HOME: ", None, None, None, False, False)
        if not os.path.exists(args.java_home) or not os.path.isfile(os.path.join(args.java_home, "bin", java_bin)):
          err = "Java home path or java binary file is unavailable. Please put correct path to java home."
          raise FatalException(1, err)
        print "Validating JDK on Ambari Server...done."

        properties.process_pair(JAVA_HOME_PROPERTY, args.java_home)
        properties.removeOldProp(JDK_NAME_PROPERTY)
        properties.removeOldProp(JCE_NAME_PROPERTY)
        update_properties(properties)

        os_ensure_java_home_env_var_is_set(args.java_home)
        return 0

    JDK_INDEX = int(jdk_num) - 1
    jdk_cfg = jdks[JDK_INDEX]

    try:
      resources_dir = properties[RESOURCES_DIR_PROPERTY]
    except (KeyError), e:
      err = 'Property ' + str(e) + ' is not defined at ' + conf_file
      raise FatalException(1, err)

    dest_file = os.path.abspath(os.path.join(resources_dir, jdk_cfg.dest_file))
    if os.path.exists(dest_file):
      print "JDK already exists, using " + dest_file
    else:
      if args.silent:
        print "Accepting the JDK license terms by default..."
      else:
        ok = get_YN_input("To download the Oracle JDK you must accept the "
                          "license terms found at "
                          "http://www.oracle.com/technetwork/java/javase/"
                          "terms/license/index.html and not accepting will "
                          "cancel the Ambari Server setup.\nDo you accept the "
                          "Oracle Binary Code License Agreement [y/n] (y)? ", True)
        if not ok:
          print 'Exiting...'
          sys.exit(1)

      jdk_url = jdk_cfg.url

      print 'Downloading JDK from ' + jdk_url + ' to ' + dest_file
      _dowload_jdk(jdk_url, dest_file)

    try:
      (retcode, out) = install_jdk(dest_file, jdk_cfg.inst_dir)
    except Exception, e:
      print "Installation of JDK has failed: %s\n" % e.message
      file_exists = os.path.isfile(dest_file)
      if file_exists:
        if args.silent:
          ok = False
        else:
          ok = get_YN_input("JDK found at " + dest_file + ". "
                                                        "Would you like to re-download the JDK [y/n] (y)? ", True)
        if not ok:
          err = "Unable to install JDK. Please remove JDK file found at " + \
                dest_file + " and re-run Ambari Server setup"
          raise FatalException(1, err)
        else:
          jdk_url = jdk_cfg.url

          print 'Re-downloading JDK from ' + jdk_url + ' to ' + dest_file
          _dowload_jdk(jdk_url, dest_file)
          print 'Successfully re-downloaded JDK distribution to ' + dest_file

          try:
            (retcode, out) = install_jdk(dest_file, jdk_cfg.inst_dir)
          except Exception, e:
            print "Installation of JDK was failed: %s\n" % e.message
            err = "Unable to install JDK. Please remove JDK, file found at " + \
                  dest_file + " and re-run Ambari Server setup"
            raise FatalException(1, err)

      else:
        err = "Unable to install JDK. File " + dest_file + " does not exist, " \
                                                           "please re-run Ambari Server setup"
        raise FatalException(1, err)

    properties.process_pair(JDK_NAME_PROPERTY, jdk_cfg.dest_file)
    properties.process_pair(JAVA_HOME_PROPERTY, jdk_cfg.inst_dir)

  try:
    download_jce_policy(jdk_cfg, resources_dir, properties)
  except FatalException, e:
    print "JCE Policy files are required for secure HDP setup. Please ensure " \
          " all hosts have the JCE unlimited strength policy 6, files."
    print_error_msg("Failed to download JCE policy files:")
    if e.reason is not None:
      print_error_msg("\nREASON: {0}".format(e.reason))
      # TODO: We don't fail installation if download_jce_policy fails. Is it OK?

  update_properties(properties)

  os_ensure_java_home_env_var_is_set(jdk_cfg.inst_dir)

  return 0


def download_jce_policy(jdk_cfg, resources_dir, properties):
  jcpol_url = jdk_cfg.jcpol_url
  dest_file = os.path.abspath(os.path.join(resources_dir, jdk_cfg.dest_jcpol_file))

  if not os.path.exists(dest_file):
    print 'Downloading JCE Policy archive from ' + jcpol_url + ' to ' + dest_file
    try:
      force_download_file(jcpol_url, dest_file)

      print 'Successfully downloaded JCE Policy archive to ' + dest_file
      properties.process_pair(JCE_NAME_PROPERTY, jdk_cfg.dest_jcpol_file)
    except FatalException:
      raise
    except Exception, e:
      err = 'Failed to download JCE Policy archive: ' + str(e)
      raise FatalException(1, err)
  else:
    print "JCE Policy archive already exists, using " + dest_file



def install_jdk(java_inst_file, java_home_dir):
  return os_install_jdk(java_inst_file, java_home_dir)


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
      print_info_msg("os_type already set in the properties file")
      return 0
  except (KeyError):
    print_error_msg("os_type is not set in the properties file. Setting it now.")

  if OSCheck.is_windows_os():
    master_os_type = OS_TYPE + OS_VERSION
  else:
    # MacOS not supported
    master_os_type = OS_FAMILY + OS_VERSION

  write_property(OS_TYPE_PROPERTY, master_os_type)
  return 0


#
# JDBC
#

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

  os.symlink(os.path.join(resources_dir, jdbc_name), jdbc_symlink)
  print "JDBC driver was successfully initialized."

def check_jdbc_drivers(args):
  os_setup_jdbc_drivers(args)
  pass


# Ask user for database conenction properties
def prompt_db_properties(args):
  if not args.silent:
    def_option = 'y' if args.must_set_database_options else 'n'
    ok = get_YN_input("Enter advanced database configuration [y/n] ({})? ".format(def_option), args.must_set_database_options)
    if not ok:
      return False

  print 'Configuring database...'

  #TODO: Add here code for DBMS selection, in case we want to support other databases besides SQL Server

  return True


#
# Setup the Ambari Server.
#

def setup(options):
  retcode = verify_setup_allowed()
  if not retcode == 0:
    raise FatalException(1, None)

  if not is_root():
    err = MESSAGE_ERROR_NOT_ROOT
    raise FatalException(4, err)

  # proceed jdbc properties if they were set
  if os_check_jdbc_options(options):
    proceedJDBCProperties(options)
    return

  (retcode, err) = disable_security_enhancements()
  if not retcode == 0:
    raise FatalException(retcode, err)

  #Create ambari user, if needed
  retcode = check_ambari_user()
  if not retcode == 0:
    err = 'Failed to create user. Exiting.'
    raise FatalException(retcode, err)

  print MESSAGE_CHECK_FIREWALL
  os_check_firewall()

  # proceed jdbc properties if they were set
  if os_check_jdbc_options(options):
    proceedJDBCProperties(options)

  print 'Checking JDK...'
  try:
    download_and_install_jdk(options)
  except FatalException as e:
    err = 'Downloading or installing JDK failed: {0}. Exiting.'.format(e)
    raise FatalException(e.code, err)

  print 'Completing setup...'
  retcode = configure_os_settings()
  if not retcode == 0:
    err = 'Configure of OS settings in ambari.properties failed. Exiting.'
    raise FatalException(retcode, err)

  if prompt_db_properties(options):
    #DB setup should be done last after doing any setup.
    os_setup_database(options)

  check_jdbc_drivers(options)
  pass

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
    update_database_name_property()
  except FatalException:
    return -1

  parse_properties_file(args)

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


#
# Resets the Ambari Server.
#
def reset(options, serviceClass):
  if not is_root():
    err = 'Ambari-server reset should be run with ' \
          'administrator-level privileges'
    raise FatalException(4, err)

  status, stateDesc = is_server_running(serviceClass)
  if status:
    err = 'Ambari-server must be stopped to reset'
    raise FatalException(1, err)

  #force reset if silent option provided
  if not options.silent:
    choice = get_YN_input("**** WARNING **** You are about to reset and clear the "
                     "Ambari Server database. This will remove all cluster "
                     "host and configuration information from the database. "
                     "You will be required to re-configure the Ambari server "
                     "and re-run the cluster wizard. \n"
                     "Are you SURE you want to perform the reset "
                     "[yes/no] (no)? ", False)
    okToRun = choice
    if not okToRun:
      err = "Ambari Server 'reset' cancelled"
      raise FatalException(1, err)

  os_reset_database(options)
  pass


def is_server_running(serviceClass):
  statusStr = serviceClass.QueryStatus()
  if statusStr in(SERVICE_STATUS_STARTING, SERVICE_STATUS_RUNNING, SERVICE_STATUS_STOPPING):
    return True, ""
  elif statusStr == SERVICE_STATUS_STOPPED:
    return False, SERVICE_STATUS_STOPPED
  elif statusStr == SERVICE_STATUS_NOT_INSTALLED:
    return False, SERVICE_STATUS_NOT_INSTALLED
  else:
    return False, None
