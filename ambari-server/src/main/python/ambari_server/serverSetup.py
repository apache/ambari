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
import platform
import re
import shutil
import sys

from ambari_commons import OSCheck, OSConst, Firewall
from ambari_commons.exceptions import FatalException
from ambari_commons.inet_utils import force_download_file
from ambari_commons.logging_utils import print_error_msg, print_info_msg, print_warning_msg, SILENT
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl
from ambari_commons.os_utils import is_root, run_os_command
from ambari_commons.str_utils import compress_backslashes
from ambari_server.dbConfiguration import DBMSConfigFactory
from ambari_server.serverConfiguration import get_ambari_properties, read_ambari_user, update_database_name_property, \
  parse_properties_file, run_schema_upgrade, update_ambari_properties, update_properties, write_property, \
  get_full_ambari_classpath, get_JAVA_HOME, find_jdk, \
  configDefaults, JDKRelease, \
  OS_VERSION, OS_TYPE, OS_FAMILY, \
  NR_USER_PROPERTY, OS_TYPE_PROPERTY, AMBARI_PROPERTIES_FILE, \
  JAVA_HOME, JAVA_HOME_PROPERTY, JDK_NAME_PROPERTY, JCE_NAME_PROPERTY, \
  JDBC_URL_PROPERTY, JDBC_RCA_URL_PROPERTY, JDBC_METRICS_URL_PROPERTY, \
  RESOURCES_DIR_PROPERTY, JDK_RELEASES, VIEWS_DIR_PROPERTY, VIEW_EXTRACT_CMD, SETUP_OR_UPGRADE_MSG
from ambari_server.setupSecurity import get_is_secure, get_is_persisted, adjust_directory_permissions
from ambari_server.userInput import get_YN_input, get_validated_string_input
from ambari_server.utils import locate_file

### # Linux-specific # ###
# selinux commands
GET_SE_LINUX_ST_CMD = locate_file('sestatus', '/usr/sbin')
SE_SETENFORCE_CMD = "setenforce 0"
SE_STATUS_DISABLED = "disabled"
SE_STATUS_ENABLED = "enabled"
SE_MODE_ENFORCING = "enforcing"
SE_MODE_PERMISSIVE = "permissive"
### # End Linux-specific # ###

### # Windows-specific # ###
SERVICE_USERNAME_KEY = "TMP_AMBARI_USERNAME"
SERVICE_PASSWORD_KEY = "TMP_AMBARI_PASSWORD"
### # End Windows-specific # ###

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def get_message_error_not_root():
  return 'Ambari-server setup should be run with administrator-level privileges'

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_message_error_not_root():
  return 'Ambari-server setup should be run with root-level privileges'

MESSAGE_ERROR_NOT_ROOT = get_message_error_not_root()

def get_supported_dbms():
  factory = DBMSConfigFactory()
  return factory.get_supported_dbms()

JDBC_DB_OPTION_VALUES = get_supported_dbms()


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
# Security enhancements (Linux only)
#

#
# Checks SELinux
#
def _check_selinux():
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

# No security enhancements in Windows
@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def disable_security_enhancements():
  retcode = 0
  err = ''
  return (retcode, err)

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def disable_security_enhancements():
  print 'Checking SELinux...'
  err = ''
  retcode = _check_selinux()
  if not retcode == 0:
    err = 'Failed to disable SELinux. Exiting.'
  return (retcode, err)


#
# User account creation
#

class AmbariUserChecks(object):
  def __init__(self):
    self.NR_USER_CHANGE_PROMPT = ""
    self.NR_USER_CUSTOMIZE_PROMPT = ""
    self.NR_DEFAULT_USER = ""
    self.NR_USER_COMMENT = "Ambari user"

  def do_checks(self):
    try:
      user = read_ambari_user()
      create_user = False
      update_user_setting = False
      if user is not None:
        create_user = get_YN_input(self.NR_USER_CHANGE_PROMPT.format(user), False)
        update_user_setting = create_user  # Only if we will create another user
      else:  # user is not configured yet
        update_user_setting = True  # Write configuration anyway
        create_user = get_YN_input(self.NR_USER_CUSTOMIZE_PROMPT, False)
        if not create_user:
          user = self.NR_DEFAULT_USER

      if create_user:
        (retcode, user) = self._create_custom_user()
        if retcode != 0:
          return retcode

      if update_user_setting:
        write_property(NR_USER_PROPERTY, user)

      adjust_directory_permissions(user)
    except OSError as e:
      print_error_msg("Failed: %s" % str(e))
      return 4
    except Exception as e:
      print_error_msg("Unexpected error %s" % str(e))
      return 1
    return 0

  def _create_custom_user(self):
    pass

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class AmbariUserChecksWindows(AmbariUserChecks):
  def __init__(self):
    super(AmbariUserChecksWindows, self).__init__()

    self.NR_USER_CHANGE_PROMPT = "Ambari-server service is configured to run under user '{0}'. Change this setting [y/n] (n)? "
    self.NR_USER_CUSTOMIZE_PROMPT = "Customize user account for ambari-server service [y/n] (n)? "
    self.NR_DEFAULT_USER = "NT AUTHORITY\SYSTEM"

  def _create_custom_user(self):
    user = get_validated_string_input(
      "Enter user account for ambari-server service ({0}):".format(self.NR_DEFAULT_USER),
      self.NR_DEFAULT_USER, None,
      "Invalid username.",
      False
    )
    if user == self.NR_DEFAULT_USER:
      return 0, user
    password = get_validated_string_input("Enter password for user {0}:".format(user), "", None, "Password", True, False)

    from ambari_commons.os_windows import UserHelper

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

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class AmbariUserChecksLinux(AmbariUserChecks):
  def __init__(self):
    super(AmbariUserChecksLinux, self).__init__()

    self.NR_USER_CHANGE_PROMPT = "Ambari-server daemon is configured to run under user '{0}'. Change this setting [y/n] (n)? "
    self.NR_USER_CUSTOMIZE_PROMPT = "Customize user account for ambari-server daemon [y/n] (n)? "
    self.NR_DEFAULT_USER = "root"

    self.NR_USERADD_CMD = 'useradd -M --comment "{1}" ' \
                          '--shell %s -d /var/lib/ambari-server/keys/ {0}' % locate_file('nologin', '/sbin')

  def _create_custom_user(self):
    user = get_validated_string_input(
      "Enter user account for ambari-server daemon (root):",
      "root",
      "^[a-z_][a-z0-9_-]{1,31}$",
      "Invalid username.",
      False
    )

    print_info_msg("Trying to create user {0}".format(user))
    command = self.NR_USERADD_CMD.format(user, self.NR_USER_COMMENT)
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

def check_ambari_user():
  return AmbariUserChecks().do_checks()


# ## Firewall ###

def check_firewall():
  firewall_obj = Firewall().getFirewallObject()
  print firewall_obj.MESSAGE_CHECK_FIREWALL
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


# ## JDK ###

class JDKSetup(object):
  def __init__(self):
    self.JDK_DEFAULT_CONFIGS = []

    self.JDK_PROMPT = "[{0}] {1}\n"
    self.JDK_CUSTOM_CHOICE_PROMPT = "[{0}] - Custom JDK\n==============================================================================\nEnter choice ({1}): "
    self.JDK_VALID_CHOICES = "^[{0}{1:d}]$"
    self.JDK_MIN_FILESIZE = 5000
    self.JAVA_BIN = ""

    self.jdk_index = 0

  #
  # Downloads and installs the JDK and the JCE policy archive
  #
  def download_and_install_jdk(self, args):
    properties = get_ambari_properties()
    if properties == -1:
      err = "Error getting ambari properties"
      raise FatalException(-1, err)

    conf_file = properties.fileName
    ok = False
    jcePolicyWarn = "JCE Policy files are required for configuring Kerberos security. If you plan to use Kerberos," \
                    "please make sure JCE Unlimited Strength Jurisdiction Policy Files are valid on all hosts."
    jdk_num = "1"

    if args.java_home:
      if not os.path.exists(args.java_home) or not os.path.isfile(os.path.join(args.java_home, configDefaults.JAVA_EXE_SUBPATH)):
        err = "Path to java home " + args.java_home + " or java binary file does not exists"
        raise FatalException(1, err)

      print_warning_msg("JAVA_HOME " + args.java_home + " must be valid on ALL hosts")
      print_warning_msg(jcePolicyWarn)

      properties.process_pair(JAVA_HOME_PROPERTY, args.java_home)
      properties.removeOldProp(JDK_NAME_PROPERTY)
      properties.removeOldProp(JCE_NAME_PROPERTY)
      update_properties(properties)

      self._ensure_java_home_env_var_is_set(args.java_home)
      return 0
    else:
      java_home_var = get_JAVA_HOME()

      if args.silent:
        if not java_home_var:
          #No java_home_var set, detect if java is already installed
          if os.environ.has_key(JAVA_HOME):
            args.java_home = os.environ[JAVA_HOME]

            properties.process_pair(JAVA_HOME_PROPERTY, args.java_home)
            properties.removeOldProp(JDK_NAME_PROPERTY)
            properties.removeOldProp(JCE_NAME_PROPERTY)
            update_properties(properties)

            self._ensure_java_home_env_var_is_set(args.java_home)
            return 0
          else:
            # For now, changing the existing JDK to make sure we use a supported one
            pass

        #Continue with the normal setup, taking the first listed JDK version as the default option
        jdk_num = "1"
        (jdks, jdk_choice_prompt, jdk_valid_choices, custom_jdk_number) = self._populate_jdk_configs(properties, jdk_num)
      else:
        if java_home_var:
          change_jdk = get_YN_input("Do you want to change Oracle JDK [y/n] (n)? ", False)
          if not change_jdk:
            self._ensure_java_home_env_var_is_set(java_home_var)
            return 0

        #Continue with the normal setup
        jdk_num = str(self.jdk_index + 1)
        (jdks, jdk_choice_prompt, jdk_valid_choices, custom_jdk_number) = self._populate_jdk_configs(properties, jdk_num)

        jdk_num = get_validated_string_input(
          jdk_choice_prompt,
          jdk_num,
          jdk_valid_choices,
          "Invalid number.",
          False
        )

        if jdk_num == str(custom_jdk_number):
          print_warning_msg("JDK must be installed on all hosts and JAVA_HOME must be valid on all hosts.")
          print_warning_msg(jcePolicyWarn)
          args.java_home = get_validated_string_input("Path to JAVA_HOME: ", None, None, None, False, False)
          if not os.path.exists(args.java_home) or not os.path.isfile(os.path.join(args.java_home, "bin", self.JAVA_BIN)):
            err = "Java home path or java binary file is unavailable. Please put correct path to java home."
            raise FatalException(1, err)
          print "Validating JDK on Ambari Server...done."

          properties.process_pair(JAVA_HOME_PROPERTY, args.java_home)
          properties.removeOldProp(JDK_NAME_PROPERTY)
          properties.removeOldProp(JCE_NAME_PROPERTY)
          update_properties(properties)

          self._ensure_java_home_env_var_is_set(args.java_home)
          return 0

      self.jdk_index = int(jdk_num) - 1
      jdk_cfg = jdks[self.jdk_index]

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
        self._download_jdk(jdk_url, dest_file)

      try:
        (retcode, out, java_home_dir) = self._install_jdk(dest_file, jdk_cfg)
      except Exception, e:
        print "Installation of JDK has failed: %s\n" % str(e)
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
            self._download_jdk(jdk_url, dest_file)
            print 'Successfully re-downloaded JDK distribution to ' + dest_file

            try:
              (retcode, out) = self._install_jdk(dest_file, jdk_cfg)
            except Exception, e:
              print "Installation of JDK was failed: %s\n" % str(e)
              err = "Unable to install JDK. Please remove JDK, file found at " + \
                    dest_file + " and re-run Ambari Server setup"
              raise FatalException(1, err)

        else:
          err = "Unable to install JDK. File " + dest_file + " does not exist, " \
                                                             "please re-run Ambari Server setup"
          raise FatalException(1, err)

      properties.process_pair(JDK_NAME_PROPERTY, jdk_cfg.dest_file)
      properties.process_pair(JAVA_HOME_PROPERTY, java_home_dir)

    try:
      self._download_jce_policy(jdk_cfg, resources_dir, properties)
    except FatalException, e:
      print "JCE Policy files are required for secure HDP setup. Please ensure " \
            " all hosts have the JCE unlimited strength policy 6, files."
      print_error_msg("Failed to download JCE policy files:")
      if e.reason is not None:
        print_error_msg("\nREASON: {0}".format(e.reason))
        # TODO: We don't fail installation if _download_jce_policy fails. Is it OK?

    update_properties(properties)

    self._ensure_java_home_env_var_is_set(java_home_dir)

    return 0

  def _populate_jdk_configs(self, properties, jdk_num):
    if properties.has_key(JDK_RELEASES):
      jdk_names = properties[JDK_RELEASES].split(',')
      jdks = []
      for jdk_name in jdk_names:
        jdkR = JDKRelease.from_properties(properties, jdk_name)
        jdks.append(jdkR)
    else:
      jdks = self.JDK_DEFAULT_CONFIGS

    n_config = 1
    jdk_choice_prompt = ''
    jdk_choices = ''
    for jdk in jdks:
      jdk_choice_prompt += self.JDK_PROMPT.format(n_config, jdk.desc)
      jdk_choices += str(n_config)
      n_config += 1

    jdk_choice_prompt += self.JDK_CUSTOM_CHOICE_PROMPT.format(n_config, jdk_num)
    jdk_valid_choices = self.JDK_VALID_CHOICES.format(jdk_choices, n_config)

    return (jdks, jdk_choice_prompt, jdk_valid_choices, n_config)

  def _download_jdk(self, jdk_url, dest_file):
    jdk_download_fail_msg = " Failed to download JDK: {0}. Please check that the " \
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

  def _download_jce_policy(self, jdk_cfg, resources_dir, properties):
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

  # Base implementation, overriden in the subclasses
  def _install_jdk(self, java_inst_file, java_home_dir):
    pass

  # Base implementation, overriden in the subclasses
  def _ensure_java_home_env_var_is_set(self, java_home_dir):
    pass

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class JDKSetupWindows(JDKSetup):
  def __init__(self):
    super(JDKSetupWindows, self).__init__()
    self.JDK_DEFAULT_CONFIGS = [
      JDKRelease("jdk7.67", "Oracle JDK 1.7.67",
                 "http://public-repo-1.hortonworks.com/ARTIFACTS/jdk-7u67-windows-x64.exe", "jdk-7u67-windows-x64.exe",
                 "http://public-repo-1.hortonworks.com/ARTIFACTS/UnlimitedJCEPolicyJDK7.zip", "UnlimitedJCEPolicyJDK7.zip",
                 "C:\\jdk1.7.0_67",
                 "Creating (jdk.*)/jre")
    ]

    self.JAVA_BIN = "java.exe"

  def _install_jdk(self, java_inst_file, jdk_cfg):
    jdk_inst_dir = jdk_cfg.inst_dir
    print "Installing JDK to {0}".format(jdk_inst_dir)

    if not os.path.exists(jdk_inst_dir):
      os.makedirs(jdk_inst_dir)

    if java_inst_file.endswith(".exe"):
      (dirname, filename) = os.path.split(java_inst_file)
      installLogFilePath = os.path.join(configDefaults.OUT_DIR, filename + "-install.log")
      #jre7u67.exe /s INSTALLDIR=<dir> STATIC=1 WEB_JAVA=0 /L \\var\\log\\ambari-server\\jre7u67.exe-install.log
      installCmd = [
        java_inst_file,
        "/s",
        "INSTALLDIR=" + jdk_inst_dir,
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
      print "JDK already installed in {0}".format(jdk_inst_dir)
      retcode = 0
    else:
      if retcode != 0:
        err = "Installation of JDK returned exit code %s" % retcode
        raise FatalException(retcode, err)

      print "Successfully installed JDK to {0}".format(jdk_inst_dir)

    # Don't forget to adjust the JAVA_HOME env var

    return (retcode, out, jdk_inst_dir)

  def _ensure_java_home_env_var_is_set(self, java_home_dir):
    if not os.environ.has_key(JAVA_HOME) or os.environ[JAVA_HOME] != java_home_dir:
      java_home_dir_unesc = compress_backslashes(java_home_dir)
      retcode, out, err = run_os_command("SETX {0} {1} /M".format(JAVA_HOME, java_home_dir_unesc))
      if retcode != 0:
        print_warning_msg("SETX output: " + out)
        print_warning_msg("SETX error output: " + err)
        err = "Setting JAVA_HOME failed. Exit code={0}".format(retcode)
        raise FatalException(1, err)

      os.environ[JAVA_HOME] = java_home_dir

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class JDKSetupLinux(JDKSetup):
  def __init__(self):
    super(JDKSetupLinux, self).__init__()
    self.JDK_DEFAULT_CONFIGS = [
      JDKRelease("jdk6.31", "Oracle JDK 1.6",
                 "http://public-repo-1.hortonworks.com/ARTIFACTS/jdk-6u31-linux-x64.bin", "jdk-6u31-linux-x64.bin",
                 "http://public-repo-1.hortonworks.com/ARTIFACTS/jce_policy-6.zip", "jce_policy-6.zip",
                 "/usr/jdk64/jdk1.6.0_31",
                 "Creating (jdk.*)/jre")
    ]

    self.JAVA_BIN = "java"

    self.CREATE_JDK_DIR_CMD = "/bin/mkdir -p {0}"
    self.MAKE_FILE_EXECUTABLE_CMD = "chmod a+x {0}"

    # use --no-same-owner when running as root to prevent uucp as the user (AMBARI-6478)
    self.UNTAR_JDK_ARCHIVE = "tar --no-same-owner -xvf {0}"

  def _install_jdk(self, java_inst_file, jdk_cfg):
    jdk_inst_dir = jdk_cfg.inst_dir
    print "Installing JDK to {0}".format(jdk_inst_dir)

    retcode, out, err = run_os_command(self.CREATE_JDK_DIR_CMD.format(jdk_inst_dir))
    savedPath = os.getcwd()
    os.chdir(jdk_inst_dir)

    try:
      if java_inst_file.endswith(".bin"):
        retcode, out, err = run_os_command(self.MAKE_FILE_EXECUTABLE_CMD.format(java_inst_file))
        retcode, out, err = run_os_command(java_inst_file + ' -noregister')
      elif java_inst_file.endswith(".gz"):
        retcode, out, err = run_os_command(self.UNTAR_JDK_ARCHIVE.format(java_inst_file))
      else:
        err = "JDK installation failed.Unknown file mask."
        raise FatalException(1, err)
    finally:
      os.chdir(savedPath)

    if retcode != 0:
      err = "Installation of JDK returned exit code %s" % retcode
      raise FatalException(retcode, err)

    jdk_version = re.search(jdk_cfg.reg_exp, out).group(1)
    java_home_dir = os.path.join(jdk_inst_dir, jdk_version)

    print "Successfully installed JDK to {0}".format(jdk_inst_dir)
    return (retcode, out, java_home_dir)

  def _ensure_java_home_env_var_is_set(self, java_home_dir):
    #No way to do this in Linux. Best we can is to set the process environment variable.
    os.environ[JAVA_HOME] = java_home_dir

def download_and_install_jdk(options):
  jdk_setup_obj = JDKSetup()
  return jdk_setup_obj.download_and_install_jdk(options)


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def get_master_os_type():
  return OS_TYPE + OS_VERSION

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_master_os_type():
  return OS_FAMILY + OS_VERSION

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

  os_system = platform.system()
  if os_system != 'Linux' and os_system != 'Windows':
    print_error_msg("Only Linux and Windows systems are supported")
    return -1

  master_os_type = get_master_os_type()

  write_property(OS_TYPE_PROPERTY, master_os_type)
  return 0


#
# JDBC
#

def _check_jdbc_options(options):
  return (options.jdbc_driver is not None and options.jdbc_db is not None)

def proceedJDBCProperties(args):
  if not os.path.isfile(args.jdbc_driver):
    err = "File {0} does not exist!".format(args.jdbc_driver)
    raise FatalException(1, err)

  if args.jdbc_db not in JDBC_DB_OPTION_VALUES:
    err = "Unsupported database name {0}. Please see help for more information.".format(args.jdbc_db)
    raise FatalException(1, err)

  _cache_jdbc_driver(args)

# No JDBC driver caching in Windows at this point. Will cache it along with the integrated authentication dll into a
#  zip archive at a later moment.
@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def _cache_jdbc_driver(args):
  pass

#TODO JDBC driver caching duplicates the LinuxDBMSConfig._install_jdbc_driver() functionality
@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def _cache_jdbc_driver(args):
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
            "permissions and free disk space.".format(args.jdbc_driver, resources_dir, str(e))
      raise FatalException(1, err)

  os.symlink(os.path.join(resources_dir, jdbc_name), jdbc_symlink)
  print "JDBC driver was successfully initialized."

def check_jdbc_drivers(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  factory = DBMSConfigFactory()

  #Only support SQL Server
  dbms = factory.create(args, properties)
  if dbms.ensure_jdbc_driver_installed(properties):
    # Now save the properties file
    return update_properties(properties)



# Ask user for database connection properties
def prompt_db_properties(options):
  if not options.silent:
    def_option = 'y' if options.must_set_database_options else 'n'
    ok = get_YN_input("Enter advanced database configuration [y/n] ({0})?".format(def_option), options.must_set_database_options)
    if not ok:
      #TODO Display the database options
      return False

  print 'Configuring database...'

  factory = DBMSConfigFactory()

  options.dbms_index = factory.select_dbms(options)

  return True

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def _setup_database(options):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(-1, "Error getting ambari properties")

  #Ensure the default database host is set
  options.default_database_host = "localhost\\SQLEXPRESS"

  factory = DBMSConfigFactory()

  #Only support SQL Server
  dbmsAmbari = factory.create(options, properties, "Ambari")
  resultA = dbmsAmbari.configure_database(options, properties)

  #By default, use the same server for Metrics
  options.default_database_host = dbmsAmbari.database_host

  dbmsMetrics = factory.create(options, properties, "Metrics")
  resultM = dbmsMetrics.configure_database(options, properties)

  # Now save the properties file
  if resultA or resultM:
    update_properties(properties)

    dbmsAmbari.setup_database()
    dbmsMetrics.setup_database()

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def _setup_database(options):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(-1, "Error getting ambari properties")

  factory = DBMSConfigFactory()

  dbmsAmbari = factory.create(options, properties, "Ambari")
  resultA = dbmsAmbari.configure_database(options, properties)

  # Now save the properties file
  if resultA:
    update_properties(properties)

    dbmsAmbari.setup_database()

def _createDefDbFactory(options):
  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(-1, "Error getting ambari properties")
  if not (properties.getPropertyDict().has_key(JDBC_URL_PROPERTY) and
            properties.getPropertyDict().has_key(JDBC_RCA_URL_PROPERTY) and
            properties.getPropertyDict().has_key(JDBC_METRICS_URL_PROPERTY)):
    raise FatalException(-1, "Ambari Server not set up yet. Nothing to reset.")

  empty_options = optparse.Values()
  empty_options.silent = options.silent
  empty_options.dbms_index = options.dbms_index
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

  factory = DBMSConfigFactory()

  return empty_options, factory, properties

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def _reset_database(options):
  empty_options, factory, properties = _createDefDbFactory(options)

  dbmsAmbari = factory.create(empty_options, properties, "Ambari")
  dbmsAmbari.reset_database()

  dbmsMetrics = factory.create(empty_options, properties, "Metrics")
  dbmsMetrics.reset_database()

@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def _reset_database(options):
  #TODO Ensure the correct DBMS is constructed, with the correct server, port, user, pwd
  empty_options, factory, properties = _createDefDbFactory(options)

  dbmsAmbari = factory.create(empty_options, properties)
  dbmsAmbari.reset_database()

#
# Extract the system views
#
def extract_views():
  jdk_path = find_jdk()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
    return 1

  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  if not VIEWS_DIR_PROPERTY in properties.keys():
    vdir = configDefaults.DEFAULT_VIEWS_DIR
  else:
    vdir = properties.get_property(VIEWS_DIR_PROPERTY)

  files = [f for f in os.listdir(vdir) if os.path.isfile(os.path.join(vdir,f))]
  for f in files:

    command = VIEW_EXTRACT_CMD.format(jdk_path, get_full_ambari_classpath(), os.path.join(vdir,f))

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
  if _check_jdbc_options(options):
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

  check_firewall()

  # proceed jdbc properties if they were set
  if _check_jdbc_options(options):
    proceedJDBCProperties(options)

  print 'Checking JDK...'
  try:
    download_and_install_jdk(options)
  except FatalException as e:
    err = 'Downloading or installing JDK failed: {0}. Exiting.'.format(str(e))
    raise FatalException(e.code, err)

  print 'Completing setup...'
  retcode = configure_os_settings()
  if not retcode == 0:
    err = 'Configure of OS settings in ambari.properties failed. Exiting.'
    raise FatalException(retcode, err)

  if prompt_db_properties(options):
    #DB setup should be done last after doing any setup.
    _setup_database(options)

  check_jdbc_drivers(options)

  print 'Extracting system views...'
  retcode = extract_views()
  if not retcode == 0:
    err = 'Error while extracting system views. Exiting'
    raise FatalException(retcode, err)

  # we've already done this, but new files were created so run it one time.
  adjust_directory_permissions(read_ambari_user())

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

  _reset_database(options)
  pass


def is_server_running(serviceClass):
  from ambari_commons.os_windows import SERVICE_STATUS_STARTING, SERVICE_STATUS_RUNNING, SERVICE_STATUS_STOPPING, \
    SERVICE_STATUS_STOPPED, SERVICE_STATUS_NOT_INSTALLED

  statusStr = serviceClass.QueryStatus()
  if statusStr in(SERVICE_STATUS_STARTING, SERVICE_STATUS_RUNNING, SERVICE_STATUS_STOPPING):
    return True, ""
  elif statusStr == SERVICE_STATUS_STOPPED:
    return False, SERVICE_STATUS_STOPPED
  elif statusStr == SERVICE_STATUS_NOT_INSTALLED:
    return False, SERVICE_STATUS_NOT_INSTALLED
  else:
    return False, None
