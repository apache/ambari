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
import os
import socket
import string

from _winreg import (OpenKey, EnumValue, HKEY_LOCAL_MACHINE, KEY_READ, CloseKey, KEY_WRITE, QueryValueEx, SetValueEx,
                     REG_EXPAND_SZ)

from ambari_commons import os_utils

from ambari_commons.exceptions import *
from ambari_commons.logging_utils import *
from ambari_commons.os_windows import run_os_command, UserHelper
from ambari_server.dbConfiguration import DBMSConfig
from ambari_server.serverConfiguration import *
from ambari_server.serverConfiguration_windows import OUT_DIR
from ambari_server.userInput import get_validated_string_input

# Non-root user setup commands
NR_USER_COMMENT = "Ambari user"
NR_GET_OWNER_CMD = 'stat -c "%U" {0}'
NR_USERADD_CMD = 'cmd /C net user {0} {1} /ADD'
NR_SET_USER_COMMENT_CMD = 'usermod -c "{0}" {1}'

NR_USER_CHANGE_PROMPT = "Ambari-server service is configured to run under user '{0}'. Change this setting [y/n] (n)? "
NR_USER_CUSTOMIZE_PROMPT = "Customize user account for ambari-server service [y/n] (n)? "
NR_DEFAULT_USER = "NT AUTHORITY\SYSTEM"

SERVICE_USERNAME_KEY = "TMP_AMBARI_USERNAME"
SERVICE_PASSWORD_KEY = "TMP_AMBARI_PASSWORD"

# JDK setup choices
JDK_DEFAULT_CONFIGS = [
  JDKRelease("jdk7.67", "Oracle JDK 7.67",
             "http://public-repo-1.hortonworks.com/ARTIFACTS/jdk-7u67-windows-x64.exe", "jdk-7u67-windows-x64.exe",
             "http://public-repo-1.hortonworks.com/ARTIFACTS/UnlimitedJCEPolicyJDK7.zip", "UnlimitedJCEPolicyJDK7.zip",
             None)
]

JDK_VERSION_REs = ["(jdk.*)/jre", "Creating (jdk.*)/jre"]
JDK_PROMPT = "[{0}] {1}\n"
JDK_CUSTOM_CHOICE_PROMPT = "[{0}] - Custom JDK\n==============================================================================\nEnter choice ({1}): "
JDK_VALID_CHOICES = "^[{0}{1:d}]$"
CUSTOM_JDK_NUMBER = "4"
JDK_MIN_FILESIZE = 5000
MAKE_FILE_EXECUTABLE_CMD = "chmod a+x {0}"

JDK_DOWNLOAD_CMD = "curl --create-dirs -o {0} {1}"
JDK_DOWNLOAD_SIZE_CMD = "curl -I {0}"

# use --no-same-owner when running as root to prevent uucp as the user (AMBARI-6478)
UNTAR_JDK_ARCHIVE = "tar --no-same-owner -xvf {0}"


#JDBC
USERNAME_PATTERN = "^[a-zA-Z_][a-zA-Z0-9_\-]*$"
DATABASE_DBMS = "sqlserver"
DATABASE_NAME = "ambari"
DATABASE_SERVER = "localhost\\\\SQLEXPRESS"
DATABASE_DRIVER_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver"

METRICS_DATABASE_NAME = "HadoopMetrics"

JDBC_PATTERNS = {"sqlserver": "sqljdbc*.jar"}
DATABASE_FULL_NAMES = {"sqlserver": "SQL Server"}
JDBC_DB_OPTION_VALUES = ["sqlserver"]
JDBC_DB_DEFAULT_DRIVER = {"sqlserver" : "sqljdbc4.jar"}


ERROR_NOT_ROOT = 'Ambari-server setup should be run with administrator-level privileges'

MESSAGE_CHECK_FIREWALL = 'Checking firewall status...'

def os_check_firewall():
  out = run_powershell_script(CHECK_FIREWALL_SCRIPT)
  if out[0] != 0:
    print_warning_msg("Unable to check firewall status:{0}".format(out[2]))
    return False
  profiles_status = [i for i in out[1].split("\n") if not i == ""]
  if "1" in profiles_status:
    enabled_profiles = []
    if profiles_status[0] == "1":
      enabled_profiles.append("DomainProfile")
    if profiles_status[1] == "1":
      enabled_profiles.append("StandardProfile")
    if profiles_status[2] == "1":
      enabled_profiles.append("PublicProfile")
    print_warning_msg("Following firewall profiles enabled:{0}. Make sure that firewall properly configured.".format(",".join(enabled_profiles)))
    return False
  return True

# No security enhancements in Windows
def disable_security_enhancements():
  retcode = 0
  err = ''
  return (retcode, err)


#
# User account creation
#

def os_create_custom_user():
  user = get_validated_string_input(
    "Enter user account for ambari-server service ({0}):".format(NR_DEFAULT_USER),
    NR_DEFAULT_USER, None,
    "Invalid username.",
    False
  )
  if user == NR_DEFAULT_USER:
    return 0, user
  password = get_validated_string_input("Enter password for user {0}:".format(user), "", None, "Password", True, False)

  uh = UserHelper()

  status, message = uh.create_user(user,password)
  if status == UserHelper.USER_EXISTS:
    print_info_msg("User {0} already exists, make sure that you typed correct password for user, "
                      "skipping user creation".format(user))

  elif status == UserHelper.ACTION_FAILED:  # fail
    print_warning_msg("Can't create user {0}. Failed with message {1}".format(user, message))
    return UserHelper.ACTION_FAILED, None

  # setting SeServiceLogonRight to user

  status, message = uh.add_user_privilege(user, 'SeServiceLogonRight')
  if status == UserHelper.ACTION_FAILED:
    print_warning_msg("Can't add SeServiceLogonRight to user {0}. Failed with message {1}".format(user, message))
    return UserHelper.ACTION_FAILED, None

  print_info_msg("User configuration is done.")
  print_warning_msg("When using non SYSTEM user make sure that your user have read\write access to log directories and "
                    "all server directories. In case of integrated authentication for SQL Server make sure that your "
                    "user properly configured to use ambari and metric database.")
  #storing username and password in os.environ temporary to pass them to service
  os.environ[SERVICE_USERNAME_KEY] = user
  os.environ[SERVICE_PASSWORD_KEY] = password
  return 0, user


#
# JDK Setup
#
def populate_jdk_configs(properties, jdk_num):
  if properties.has_key(JDK_RELEASES):
    jdk_names = properties[JDK_RELEASES].split(',')
    jdks = []
    for jdk_name in jdk_names:
      jdkR = JDKRelease.from_properties(properties, jdk_name)
      jdks.append(jdkR)
  else:
    jdks = JDK_DEFAULT_CONFIGS

  n_config = 1
  jdk_choice_prompt = ''
  jdk_choices = ''
  for jdk in jdks:
    jdk_choice_prompt += JDK_PROMPT.format(n_config, jdk.desc)
    jdk_choices_tmp = '{0}{1:d}'.format(jdk_choices, n_config)
    jdk_choices = jdk_choices_tmp
    n_config += 1

  jdk_choice_prompt += JDK_CUSTOM_CHOICE_PROMPT.format(n_config, jdk_num)
  jdk_valid_choices = JDK_VALID_CHOICES.format(jdk_choices, n_config)

  return (jdks, jdk_choice_prompt, jdk_valid_choices, n_config)


def os_install_jdk(java_inst_file, java_home_dir):
  print "Installing JDK to {0}".format(java_home_dir)

  if not os.path.exists(java_home_dir):
    os.makedirs(java_home_dir)

  if java_inst_file.endswith(".exe"):
    (dirname, filename) = os.path.split(java_inst_file)
    installLogFilePath = os.path.join(OUT_DIR, filename + "-install.log")
    #jre7u67.exe /s INSTALLDIR=<dir> STATIC=1 WEB_JAVA=0 /L \\var\\log\\ambari-server\\jre7u67.exe-install.log
    installCmd = [
      java_inst_file,
      "/s",
      "INSTALLDIR=" + java_home_dir,
      "STATIC=1",
      "WEB_JAVA=0",
      "/L",
      installLogFilePath
    ]
    retcode, out, err = run_os_command(installCmd)
  #TODO: support .msi file installations
    #msiexec.exe jre.msi /s INSTALLDIR=<dir> STATIC=1 WEB_JAVA=0 /L \\var\\log\\ambari-server\\jre7u67-install.log ?
  else:
    err = "JDK installation failed.Unknown file mask."
    raise FatalException(1, err)

  if retcode == 1603:
    # JDK already installed
    print "JDK already installed in {0}".format(java_home_dir)
    retcode = 0
  else:
    if retcode != 0:
      err = "Installation of JDK returned exit code %s" % retcode
      raise FatalException(retcode, err)

    print "Successfully installed JDK to {0}".format(java_home_dir)

  # Don't forget to adjust the JAVA_HOME env var

  return (retcode, out)

def os_ensure_java_home_env_var_is_set(java_home_var):
  if not os.environ.has_key(JAVA_HOME) or os.environ[JAVA_HOME] != java_home_var:
    java_home_var_val = java_home_var.replace('\\\\', '\\')
    os.system("SETX {0} {1} /M".format(JAVA_HOME, java_home_var_val))
    os.environ[JAVA_HOME] = java_home_var
    pass

#
# JDBC Setup
#

def os_check_jdbc_options(options):
  #Only SQL Server supported, no point in checking options.jdbc_db
  return (options.jdbc_driver is not None)

def os_setup_jdbc_drivers(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  #Only support SQL Server
  dbms = DBMSConfig.create(args, properties)
  if dbms.ensure_jdbc_driver_installed(args, properties):
    # Now save the properties file
    update_properties(properties)
  pass

def os_setup_database(options):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(-1, "Error getting ambari properties")

  #Ensure the default database host is set
  options.default_database_host = "localhost\\SQLEXPRESS"

  #Only support SQL Server
  dbmsAmbari = DBMSConfig.create(options, properties, "Ambari")
  resultA = dbmsAmbari.configure_database(options, properties)

  #By default, use the same server for Metrics
  options.default_database_host = dbmsAmbari.database_host

  dbmsMetrics = DBMSConfig.create(options, properties, "Metrics")
  resultM = dbmsMetrics.configure_database(options, properties)

  # Now save the properties file
  if resultA or resultM:
    update_properties(properties)

    dbmsAmbari.setup_database()
    dbmsMetrics.setup_database()

def os_reset_database(options):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(-1, "Error getting ambari properties")

  if not (properties.getPropertyDict().has_key(JDBC_URL_PROPERTY) and
            properties.getPropertyDict().has_key(JDBC_RCA_URL_PROPERTY) and
            properties.getPropertyDict().has_key(JDBC_METRICS_URL_PROPERTY)):
    raise FatalException(-1, "Ambari Server not set up yet. Nothing to reset.")

  empty_options = optparse.Values()
  empty_options.silent = options.silent
  empty_options.database_host = ""
  empty_options.database_port = ""
  empty_options.database_name = ""
  empty_options.database_windows_auth = False
  empty_options.database_username = ""
  empty_options.database_password = ""
  empty_options.init_db_script_file = ""
  empty_options.cleanup_db_script_file = ""
  empty_options.init_metrics_db_script_file = ""
  empty_options.cleanup_metrics_db_script_file = ""

  #Only support SQL Server
  dbmsAmbari = DBMSConfig.create(empty_options, properties, "Ambari")
  dbmsAmbari.reset_database()

  dbmsMetrics = DBMSConfig.create(empty_options, properties, "Metrics")
  dbmsMetrics.reset_database()
  pass
