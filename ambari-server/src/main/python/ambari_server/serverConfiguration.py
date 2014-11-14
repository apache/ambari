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
import re

from ambari_commons.os_utils import *
from ambari_commons.logging_utils import print_warning_msg, print_info_msg, print_error_msg
from properties import Properties

if OSCheck.is_windows_os():
  from serverConfiguration_windows import *
else:
  # MacOS not supported
  from serverConfiguration_linux import *


# Non-root user setup commands
NR_USER_PROPERTY = "ambari-server.user"

# constants
STACK_NAME_VER_SEP = "-"
BLIND_PASSWORD = "*****"

# Common messages
PRESS_ENTER_MSG = "Press <enter> to continue."

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

#JCE Policy files
JCE_POLICY_FILENAMES = ["UnlimitedJCEPolicyJDK7.zip", "jce_policy-6.zip"]
JCE_DOWNLOAD_CMD = "curl -o {0} {1}"
JCE_MIN_FILESIZE = 5000

# JDBC
#TODO property used incorrectly in local case, it was meant to be dbms name, not postgres database name,
# has workaround for now, as we don't need dbms name if persistence_type=local
JDBC_DATABASE_PROPERTY = "server.jdbc.database"
JDBC_HOSTNAME_PROPERTY = "server.jdbc.hostname"
JDBC_PORT_PROPERTY = "server.jdbc.port"
JDBC_SCHEMA_PROPERTY = "server.jdbc.schema"

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

# resources repo configuration
RESOURCES_DIR_PROPERTY = "resources.dir"
RESOURCES_DIR_DEFAULT = "resources"

# stack repo upgrade
STACK_LOCATION_KEY = 'metadata.path'
STACK_LOCATION_DEFAULT = "resources" + os.sep + "stacks"

# JDK
JDK_RELEASES="java.releases"

# configuration backup
back_up_file_path = None


def get_conf_dir():
  try:
    conf_dir = os.environ[AMBARI_CONF_VAR]
    return conf_dir
  except KeyError:
    default_conf_dir = DEFAULT_CONF_DIR
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

def get_prompt_default(defaultStr=None):
  if not defaultStr or defaultStr == "":
    return ""
  else:
    return '(' + defaultStr + ')'

# Copy file to /tmp and save with file.# (largest # is latest file)
def backup_file_in_temp(filePath):
  global back_up_file_path
  if filePath is not None and back_up_file_path is None:
    tmpDir = tempfile.gettempdir()
    back_up_file_count = len(glob.glob1(tmpDir, AMBARI_PROPERTIES_FILE + "*"))
    back_up_file_path = os.path.join(tmpDir, AMBARI_PROPERTIES_FILE + "." + str(back_up_file_count + 1))
    try:
      shutil.copyfile(filePath, back_up_file_path)
    except (Exception), e:
      print_error_msg('Could not backup file in temp "%s": %s' % (str(
        back_up_file_count, e)))
  return 0

def check_database_name_property():
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  dbname = properties[JDBC_DATABASE_PROPERTY]
  if dbname is None or dbname == "":
    err = "DB Name property not set in config file.\n" + SETUP_OR_UPGRADE_MSG
    raise FatalException(-1, err)

def update_database_name_property():
  try:
    check_database_name_property()
  except FatalException:
    properties = get_ambari_properties()
    if properties == -1:
      err = "Error getting ambari properties"
      raise FatalException(-1, err)
    print_warning_msg(JDBC_DATABASE_PROPERTY + " property isn't set in " +
                      AMBARI_PROPERTIES_FILE + ". Setting it to default value - " + DEFAULT_DB_NAME)
    properties.process_pair(JDBC_DATABASE_PROPERTY, DEFAULT_DB_NAME)
    conf_file = find_properties_file()
    try:
      properties.store(open(conf_file, "w"))
    except Exception, e:
      err = 'Could not write ambari config file "%s": %s' % (conf_file, e)
      raise FatalException(-1, err)


def is_alias_string(passwdStr):
  regex = re.compile("\$\{alias=[\w\.]+\}")
  # Match implies string at beginning of word
  r = regex.match(passwdStr)
  if r is not None:
    return True
  else:
    return False


# Load database connection properties from conf file
def parse_properties_file(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  # args.server_version_file_path = properties[SERVER_VERSION_FILE_PATH]
  args.persistence_type = properties[PERSISTENCE_TYPE_PROPERTY]
  args.jdbc_url = properties[JDBC_URL_PROPERTY]

  if not args.persistence_type:
    args.persistence_type = "local"

  if args.persistence_type == 'remote':
    args.dbms = properties[JDBC_DATABASE_PROPERTY]
    args.database_host = properties[JDBC_HOSTNAME_PROPERTY]
    args.database_port = properties[JDBC_PORT_PROPERTY]
    args.database_name = properties[JDBC_SCHEMA_PROPERTY]
  else:
    #TODO incorrect property used!! leads to bunch of troubles. Workaround for now
    args.database_name = properties[JDBC_DATABASE_PROPERTY]

  args.database_username = properties[JDBC_USER_NAME_PROPERTY]
  args.database_password_file = properties[JDBC_PASSWORD_PROPERTY]
  if args.database_password_file:
    if not is_alias_string(args.database_password_file):
      args.database_password = open(properties[JDBC_PASSWORD_PROPERTY]).read()
    else:
      args.database_password = args.database_password_file
  return 0


def run_schema_upgrade():
  jdk_path = find_jdk()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + JDK_INSTALL_DIR)
    return 1
  command = SCHEMA_UPGRADE_HELPER_CMD.format(jdk_path, get_conf_dir(), get_ambari_classpath())
  (retcode, stdout, stderr) = run_os_command(command)
  print_info_msg("Return code from schema upgrade command, retcode = " + str(retcode))
  if retcode > 0:
    print_error_msg("Error executing schema upgrade, please check the server logs.")
  return retcode


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
    default_jar_location = DEFAULT_LIBS_DIR
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
      java_exe_path = os.path.join(jdk_path, JAVA_EXE_SUBPATH)
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
  print "Looking for available JDKs at " + JDK_INSTALL_DIR
  jdks = glob.glob(JDK_INSTALL_DIR + os.sep + JDK_SEARCH_PATTERN)
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
