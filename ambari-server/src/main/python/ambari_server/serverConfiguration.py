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

import datetime
import glob
import os
import re
import shutil
import string
import tempfile

from ambari_commons.exceptions import FatalException
from ambari_commons.os_check import OSCheck, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl
from ambari_commons.os_utils import run_os_command, search_file
from ambari_commons.logging_utils import print_warning_msg, print_info_msg, print_error_msg
from properties import Properties


OS_VERSION = OSCheck().get_os_major_version()
OS_TYPE = OSCheck.get_os_type()
OS_FAMILY = OSCheck.get_os_family()


# Non-root user setup commands
NR_USER_PROPERTY = "ambari-server.user"

# constants
STACK_NAME_VER_SEP = "-"
BLIND_PASSWORD = "*****"

# Common messages
PRESS_ENTER_MSG = "Press <enter> to continue."

OS_FAMILY_PROPERTY = "server.os_family"
OS_TYPE_PROPERTY = "server.os_type"

BOOTSTRAP_DIR_PROPERTY = "bootstrap.dir"

AMBARI_CONF_VAR = "AMBARI_CONF_DIR"
AMBARI_PROPERTIES_FILE = "ambari.properties"
AMBARI_PROPERTIES_BACKUP_FILE = "ambari.properties.backup"

GET_FQDN_SERVICE_URL = "server.fqdn.service.url"

SERVER_OUT_FILE_KEY = "ambari.output.file.path"
VERBOSE_OUTPUT_KEY = "ambari.output.verbose"

DEBUG_MODE_KEY = "ambari.server.debug"
SUSPEND_START_MODE_KEY = "ambari.server.debug.suspend.start"

# Environment variables
AMBARI_SERVER_LIB = "AMBARI_SERVER_LIB"
JAVA_HOME = "JAVA_HOME"

AMBARI_VERSION_VAR = "AMBARI_VERSION_VAR"

# JDK
JAVA_HOME_PROPERTY = "java.home"
JDK_NAME_PROPERTY = "jdk.name"
JCE_NAME_PROPERTY = "jce.name"

DEFAULT_JDK16_LOCATION = "/usr/jdk64/jdk1.6.0_31"
JDK_NAMES = ["jdk-7u67-linux-x64.tar.gz", "jdk-6u31-linux-x64.bin"]

#JCE Policy files
JCE_POLICY_FILENAMES = ["UnlimitedJCEPolicyJDK7.zip", "jce_policy-6.zip"]
JCE_DOWNLOAD_CMD = "curl -o {0} {1}"
JCE_MIN_FILESIZE = 5000

# JDBC
#TODO property used incorrectly in local case, it was meant to be dbms name, not postgres database name,
# has workaround for now, as we don't need dbms name if persistence_type=local
JDBC_DATABASE_PROPERTY = "server.jdbc.database"                 # E.g., embedded|oracle|mysql|postgres|sqlserver
JDBC_DATABASE_NAME_PROPERTY = "server.jdbc.database_name"       # E.g., ambari. Not used on Windows.
JDBC_HOSTNAME_PROPERTY = "server.jdbc.hostname"
JDBC_PORT_PROPERTY = "server.jdbc.port"
JDBC_POSTGRES_SCHEMA_PROPERTY = "server.jdbc.postgres.schema"   # Only for postgres, defaults to same value as DB name

JDBC_USER_NAME_PROPERTY = "server.jdbc.user.name"
JDBC_PASSWORD_PROPERTY = "server.jdbc.user.passwd"
JDBC_PASSWORD_FILENAME = "password.dat"
JDBC_RCA_PASSWORD_FILENAME = "rca_password.dat"

CLIENT_API_PORT_PROPERTY = "client.api.port"
CLIENT_API_PORT = "8080"

PERSISTENCE_TYPE_PROPERTY = "server.persistence.type"
JDBC_DRIVER_PROPERTY = "server.jdbc.driver"
JDBC_DRIVER_PATH_PROPERTY = "server.jdbc.driver.path"
JDBC_URL_PROPERTY = "server.jdbc.url"

JDBC_RCA_DATABASE_PROPERTY = "server.jdbc.database"
JDBC_RCA_HOSTNAME_PROPERTY = "server.jdbc.hostname"
JDBC_RCA_PORT_PROPERTY = "server.jdbc.port"
JDBC_RCA_SCHEMA_PROPERTY = "server.jdbc.schema"

JDBC_RCA_DRIVER_PROPERTY = "server.jdbc.rca.driver"
JDBC_RCA_URL_PROPERTY = "server.jdbc.rca.url"
JDBC_RCA_USER_NAME_PROPERTY = "server.jdbc.rca.user.name"
JDBC_RCA_PASSWORD_FILE_PROPERTY = "server.jdbc.rca.user.passwd"

JDBC_RCA_PASSWORD_ALIAS = "ambari.db.password"

#Windows-specific settings

JDBC_USE_INTEGRATED_AUTH_PROPERTY = "server.jdbc.use.integrated.auth"

JDBC_RCA_USE_INTEGRATED_AUTH_PROPERTY = "server.jdbc.rca.use.integrated.auth"


# resources repo configuration
RESOURCES_DIR_PROPERTY = "resources.dir"
RESOURCES_DIR_DEFAULT = "resources"

# stack repo upgrade
STACK_LOCATION_KEY = 'metadata.path'
STACK_LOCATION_DEFAULT = "resources" + os.sep + "stacks"

# JDK
JDK_RELEASES="java.releases"

#Common setup or upgrade message
SETUP_OR_UPGRADE_MSG = "- If this is a new setup, then run the \"ambari-server setup\" command to create the user\n" \
                       "- If this is an upgrade of an existing setup, run the \"ambari-server upgrade\" command.\n" \
                       "Refer to the Ambari documentation for more information on setup and upgrade."

DEFAULT_DB_NAME = "ambari"


class ServerConfigDefaults(object):
  def __init__(self):
    self.JAVA_SHARE_PATH = "/usr/share/java"
    self.OUT_DIR = os.sep + os.path.join("var", "log", "ambari-server")
    self.SERVER_OUT_FILE = os.path.join(self.OUT_DIR, "ambari-server.out")
    self.SERVER_LOG_FILE = os.path.join(self.OUT_DIR, "ambari-server.log")
    self.ROOT_FS_PATH = os.sep

    self.JDK_INSTALL_DIR = ""
    self.JDK_SEARCH_PATTERN = ""
    self.JAVA_EXE_SUBPATH = ""
    self.JDK_SECURITY_DIR = "jre/lib/security"
    self.SERVER_RESOURCES_DIR = "/var/lib/ambari-server/resources"

    # Configuration defaults
    self.DEFAULT_CONF_DIR = ""
    self.PID_DIR = os.sep + os.path.join("var", "run", "ambari-server")
    self.DEFAULT_LIBS_DIR = ""

    # ownership/permissions mapping
    # path - permissions - user - group - recursive
    # Rules are executed in the same order as they are listed
    # {0} in user/group will be replaced by customized ambari-server username
    self.NR_ADJUST_OWNERSHIP_LIST = []

    self.MASTER_KEY_FILE_PERMISSIONS = "600"
    self.CREDENTIALS_STORE_FILE_PERMISSIONS = "600"
    self.TRUST_STORE_LOCATION_PERMISSIONS = "600"

    self.DEFAULT_DB_NAME = "ambari"

    self.DEFAULT_VIEWS_DIR = ""

    #keytool commands
    self.keytool_bin = ""

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class ServerConfigDefaultsWindows(ServerConfigDefaults):
  def __init__(self):
    super(ServerConfigDefaultsWindows, self).__init__()
    self.JDK_INSTALL_DIR = "C:\\"
    self.JDK_SEARCH_PATTERN = "j[2se|dk|re]*"
    self.JAVA_EXE_SUBPATH = "bin\\java.exe"

    # Configuration defaults
    self.DEFAULT_CONF_DIR = "conf"
    self.DEFAULT_LIBS_DIR = "lib"

    # ownership/permissions mapping
    # path - permissions - user - group - recursive
    # Rules are executed in the same order as they are listed
    # {0} in user/group will be replaced by customized ambari-server username
    # The permissions are icacls
    self.NR_ADJUST_OWNERSHIP_LIST = [
      (self.OUT_DIR, "M", "{0}", True),  #0110-0100-0100 rw-r-r
      (self.OUT_DIR, "F", "{0}", False), #0111-0101-0101 rwx-rx-rx
      (self.PID_DIR, "M", "{0}", True),
      (self.PID_DIR, "F", "{0}", False),
      ("bootstrap", "F", "{0}", False),
      ("ambari-env.cmd", "F", "{0}", False),
      ("keystore", "M", "{0}", True),
      ("keystore", "F", "{0}", False),
      ("keystore\\db", "700", "{0}", False),
      ("keystore\\db\\newcerts", "700", "{0}", False),
      ("resources\\stacks", "755", "{0}", True),
      ("resources\\custom_actions", "755", "{0}", True),
      ("conf", "644", "{0}", True),
      ("conf", "755", "{0}", False),
      ("conf\\password.dat", "640", "{0}", False),
      # Also, /etc/ambari-server/conf/password.dat
      # is generated later at store_password_file
    ]

    self.DEFAULT_VIEWS_DIR = "resources\\views"

    #keytool commands
    self.keytool_bin = "keytool.exe"

@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class ServerConfigDefaultsLinux(ServerConfigDefaults):
  def __init__(self):
    super(ServerConfigDefaultsLinux, self).__init__()
    # JDK
    self.JDK_INSTALL_DIR = "/usr/jdk64"
    self.JDK_SEARCH_PATTERN = "jdk*"
    self.JAVA_EXE_SUBPATH = "bin/java"

    # Configuration defaults
    self.DEFAULT_CONF_DIR = "/etc/ambari-server/conf"
    self.DEFAULT_LIBS_DIR = "/usr/lib/ambari-server"

    # ownership/permissions mapping
    # path - permissions - user - group - recursive
    # Rules are executed in the same order as they are listed
    # {0} in user/group will be replaced by customized ambari-server username
    self.NR_ADJUST_OWNERSHIP_LIST = [
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
      ("/var/lib/ambari-server/resources/host_scripts/", "755", "{0}", True),
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

    self.DEFAULT_VIEWS_DIR = "/var/lib/ambari-server/resources/views"

    #keytool commands
    self.keytool_bin = "keytool"

configDefaults = ServerConfigDefaults()


SCHEMA_UPGRADE_HELPER_CMD = "{0} -cp {1} " + \
                            "org.apache.ambari.server.upgrade.SchemaUpgradeHelper" + \
                            " > " + configDefaults.SERVER_OUT_FILE + " 2>&1"


def get_conf_dir():
  try:
    conf_dir = os.environ[AMBARI_CONF_VAR]
    return conf_dir
  except KeyError:
    default_conf_dir = configDefaults.DEFAULT_CONF_DIR
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

def get_value_from_properties(properties, key, default=""):
  try:
    value = properties.get_property(key)
    if not value:
      value = default
  except:
    return default
  return value

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


def is_alias_string(passwdStr):
  regex = re.compile("\$\{alias=[\w\.]+\}")
  # Match implies string at beginning of word
  r = regex.match(passwdStr)
  if r is not None:
    return True
  else:
    return False


def update_ambari_properties():
  prev_conf_file = search_file(AMBARI_PROPERTIES_BACKUP_FILE, get_conf_dir())
  conf_file = search_file(AMBARI_PROPERTIES_FILE, get_conf_dir())

  # Previous config file does not exist
  if (not prev_conf_file) or (prev_conf_file is None):
    print_warning_msg("Can not find ambari.properties.backup file from previous version, skipping import of settings")
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

    for key in properties.keys():
      if not propertyMap.has_key(key):
        properties.removeOldProp(key)

    with open(conf_file, 'w') as file:
      properties.store_ordered(file)

  return 0

def update_properties_2(properties, propertyMap):
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

#
# Checks if options determine local DB configuration
#
def is_local_database(args):
  return hasattr(args, 'persistence_type') and args.persistence_type == 'local'

#
### JDK ###
#

#
# Describes the JDK configuration data, necessary for download and installation
#
class JDKRelease:
  name = ""
  desc = ""
  url = ""
  dest_file = ""
  jcpol_url = "http://public-repo-1.hortonworks.com/ARTIFACTS/UnlimitedJCEPolicyJDK7.zip"
  dest_jcpol_file = ""
  inst_dir = ""

  def __init__(self, i_name, i_desc, i_url, i_dest_file, i_jcpol_url, i_dest_jcpol_file, i_inst_dir):
    if i_name is None or i_name is "":
      raise FatalException(-1, "Invalid JDK name: " + (i_desc or ""))
    self.name = i_name
    if i_desc is None or i_desc is "":
      self.desc = self.name
    else:
      self.desc = i_desc
    if i_url is None or i_url is "":
      raise FatalException(-1, "Invalid URL for JDK " + i_name)
    self.url = i_url
    if i_dest_file is None or i_dest_file is "":
      self.dest_file = i_name + ".exe"
    else:
      self.dest_file = i_dest_file
    if not (i_jcpol_url is None or i_jcpol_url is ""):
      self.jcpol_url = i_jcpol_url
    if i_dest_jcpol_file is None or i_dest_jcpol_file is "":
      self.dest_jcpol_file = "jcpol-" + i_name + ".zip"
    else:
      self.dest_jcpol_file = i_dest_jcpol_file
    if i_inst_dir is None or i_inst_dir is "":
      self.inst_dir = "C:\\" + i_desc
    else:
      self.inst_dir = i_inst_dir

  @classmethod
  def from_properties(cls, properties, section_name):
    (desc, url, dest_file, jcpol_url, jcpol_file, inst_dir) = JDKRelease.__load_properties(properties, section_name)
    cls = JDKRelease(section_name, desc, url, dest_file, jcpol_url, jcpol_file, inst_dir)
    return cls

  @staticmethod
  def __load_properties(properties, section_name):
    if section_name is None or section_name is "":
      raise FatalException(-1, "Invalid properties section: " + ("(empty)" if section_name is None else ""))
    if(properties.has_key(section_name + ".desc")):   #Not critical
      desc = properties[section_name + ".desc"]
    else:
      desc = section_name
    if not properties.has_key(section_name + ".url"):
      raise FatalException(-1, "Invalid JDK URL in the properties section: " + section_name)
    url = properties[section_name + ".url"]      #Required
    if(properties.has_key(section_name + ".dest-file")):   #Not critical
      dest_file = properties[section_name + ".dest-file"]
    else:
      dest_file = section_name + ".exe"
    if(properties.has_key(section_name + ".jcpol-url")):   #Not critical
      jcpol_url = properties[section_name + ".jcpol-url"]
    else:
      jcpol_url = None
    if(properties.has_key(section_name + ".jcpol-file")):   #Not critical
      jcpol_file = properties[section_name + ".jcpol-file"]
    else:
      jcpol_file = None
    if(properties.has_key(section_name + ".home")):   #Not critical
      inst_dir = properties[section_name + ".home"]
    else:
      inst_dir = "C:\\" + section_name
    return (desc, url, dest_file, jcpol_url, jcpol_file, inst_dir)
  pass

def get_ambari_jars():
  try:
    conf_dir = os.environ[AMBARI_SERVER_LIB]
    return conf_dir
  except KeyError:
    default_jar_location = configDefaults.DEFAULT_LIBS_DIR
    print_info_msg(AMBARI_SERVER_LIB + " is not set, using default "
                 + default_jar_location)
    return default_jar_location

def get_share_jars():
  share_jars = ""
  file_list = []
  file_list.extend(glob.glob(configDefaults.JAVA_SHARE_PATH + os.sep + "*mysql*"))
  file_list.extend(glob.glob(configDefaults.JAVA_SHARE_PATH + os.sep + "*ojdbc*"))
  if len(file_list) > 0:
    share_jars = string.join(file_list, os.pathsep)
  return share_jars

def get_jdbc_cp():
  jdbc_jar_path = ""
  properties = get_ambari_properties()
  if properties != -1:
    jdbc_jar_path = properties[JDBC_DRIVER_PATH_PROPERTY]
  return jdbc_jar_path

def get_ambari_classpath():
  ambari_cp = os.path.abspath(get_ambari_jars() + os.sep + "*")
  jdbc_cp = get_jdbc_cp()
  if len(jdbc_cp) > 0:
    ambari_cp = ambari_cp + os.pathsep + jdbc_cp
  share_cp = get_share_jars()
  if len(share_cp) > 0:
    ambari_cp = ambari_cp + os.pathsep + share_cp
  return ambari_cp

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
    if os.path.exists(jdk_path):
      java_exe_path = os.path.join(jdk_path, configDefaults.JAVA_EXE_SUBPATH)
      if os.path.exists(java_exe_path) and os.path.isfile(java_exe_path):
        return True
  return False

#
# Finds the available JDKs.
#
def find_jdk():
  jdkPath = get_JAVA_HOME()
  if jdkPath:
    if validate_jdk(jdkPath):
      return jdkPath
  print "Looking for available JDKs at " + configDefaults.JDK_INSTALL_DIR
  jdks = glob.glob(os.path.join(configDefaults.JDK_INSTALL_DIR, configDefaults.JDK_SEARCH_PATTERN))
  #[fbarca] Use the newest JDK
  jdks.sort(None, None, True)
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
