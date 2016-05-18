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

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import os
import sys
import shutil
import base64
import urllib2
import re
import glob
import optparse

from ambari_commons.exceptions import FatalException
from ambari_commons.logging_utils import print_info_msg, print_warning_msg, print_error_msg, get_verbose
from ambari_commons.os_utils import is_root, run_os_command
from ambari_server.dbConfiguration import DBMSConfigFactory, check_jdbc_drivers, \
  get_jdbc_driver_path, ensure_jdbc_driver_is_installed, LINUX_DBMS_KEYS_LIST
from ambari_server.properties import Properties
from ambari_server.serverConfiguration import configDefaults, \
  check_database_name_property, get_ambari_properties, get_ambari_version, \
  get_java_exe_path, get_stack_location, parse_properties_file, read_ambari_user, update_ambari_properties, \
  update_database_name_property, get_admin_views_dir, get_views_dir, \
  AMBARI_PROPERTIES_FILE, IS_LDAP_CONFIGURED, LDAP_PRIMARY_URL_PROPERTY, RESOURCES_DIR_PROPERTY, \
  SETUP_OR_UPGRADE_MSG, update_krb_jaas_login_properties, AMBARI_KRB_JAAS_LOGIN_FILE, get_db_type, update_ambari_env, \
  AMBARI_ENV_FILE, JDBC_DATABASE_PROPERTY
from ambari_server.setupSecurity import adjust_directory_permissions, \
  generate_env, ensure_can_start_under_current_user
from ambari_server.utils import compare_versions
from ambari_server.serverUtils import is_server_runing, get_ambari_server_api_base
from ambari_server.userInput import get_validated_string_input, get_prompt_default, read_password, get_YN_input
from ambari_server.serverClassPath import ServerClassPath
from ambari_server.setupMpacks import replay_mpack_logs
from ambari_commons.logging_utils import get_debug_mode,   set_debug_mode_from_options

# constants
STACK_NAME_VER_SEP = "-"

SCHEMA_UPGRADE_HELPER_CMD = "{0} -cp {1} " + \
                            "org.apache.ambari.server.upgrade.SchemaUpgradeHelper" + \
                            " > " + configDefaults.SERVER_OUT_FILE + " 2>&1"

STACK_UPGRADE_HELPER_CMD = "{0} -cp {1} " + \
                           "org.apache.ambari.server.upgrade.StackUpgradeHelper" + \
                           " {2} {3} > " + configDefaults.SERVER_OUT_FILE + " 2>&1"

SCHEMA_UPGRADE_HELPER_CMD_DEBUG = "{0} " \
                         "-server -XX:NewRatio=2 " \
                         "-XX:+UseConcMarkSweepGC " + \
                         " -Xdebug -Xrunjdwp:transport=dt_socket,address=5005," \
                         "server=y,suspend={2} " \
                         "-cp {1} " + \
                         "org.apache.ambari.server.upgrade.SchemaUpgradeHelper" + \
                         " > " + configDefaults.SERVER_OUT_FILE + " 2>&1"

SCHEMA_UPGRADE_DEBUG = False

SUSPEND_START_MODE = False

#
# Stack upgrade
#

def upgrade_stack(args):
  if not is_root():
    err = 'Ambari-server upgradestack should be run with ' \
          'root-level privileges'
    raise FatalException(4, err)

  check_database_name_property()

  try:
    stack_id = args[1]
  except IndexError:
    #stack_id is mandatory
    raise FatalException("Invalid number of stack upgrade arguments")

  try:
    repo_url = args[2]
  except IndexError:
    repo_url = None

  try:
    repo_url_os = args[3]
  except IndexError:
    repo_url_os = None

  parser = optparse.OptionParser()
  parser.add_option("-d", type="int", dest="database_index")

  db = get_ambari_properties()[JDBC_DATABASE_PROPERTY]

  idx = LINUX_DBMS_KEYS_LIST.index(db)

  (options, opt_args) = parser.parse_args(["-d {0}".format(idx)])

  stack_name, stack_version = stack_id.split(STACK_NAME_VER_SEP)
  retcode = run_stack_upgrade(options, stack_name, stack_version, repo_url, repo_url_os)

  if not retcode == 0:
    raise FatalException(retcode, 'Stack upgrade failed.')

  return retcode

def load_stack_values(version, filename):
  import xml.etree.ElementTree as ET
  values = {}
  root = ET.parse(filename).getroot()
  for ostag in root:
    ostype = ostag.attrib['type']
    for repotag in ostag:
      reponametag = repotag.find('reponame')
      repoidtag = repotag.find('repoid')
      baseurltag = repotag.find('baseurl')
      if reponametag is not None and repoidtag is not None and baseurltag is not None:
        key = "repo:/" + reponametag.text
        key += "/" + version
        key += "/" + ostype
        key += "/" + repoidtag.text
        key += ":baseurl"
        values[key] = baseurltag.text

  return values


def run_stack_upgrade(args, stackName, stackVersion, repo_url, repo_url_os):
  jdk_path = get_java_exe_path()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + configDefaults.JDK_INSTALL_DIR)
    return 1
  stackId = {}
  stackId[stackName] = stackVersion
  if repo_url is not None:
    stackId['repo_url'] = repo_url
  if repo_url_os is not None:
    stackId['repo_url_os'] = repo_url_os

  serverClassPath = ServerClassPath(get_ambari_properties(), args)
  command = STACK_UPGRADE_HELPER_CMD.format(jdk_path, serverClassPath.get_full_ambari_classpath_escaped_for_shell(),
                                            "updateStackId",
                                            "'" + json.dumps(stackId) + "'")
  (retcode, stdout, stderr) = run_os_command(command)
  print_info_msg("Return code from stack upgrade command, retcode = " + str(retcode))
  if retcode > 0:
    print_error_msg("Error executing stack upgrade, please check the server logs.")
  return retcode

def run_metainfo_upgrade(args, keyValueMap=None):
  jdk_path = get_java_exe_path()
  if jdk_path is None:
    print_error_msg("No JDK found, please run the \"setup\" "
                    "command to install a JDK automatically or install any "
                    "JDK manually to " + configDefaults.JDK_INSTALL_DIR)

  retcode = 1
  if keyValueMap:
    serverClassPath = ServerClassPath(get_ambari_properties(), args)
    command = STACK_UPGRADE_HELPER_CMD.format(jdk_path, serverClassPath.get_full_ambari_classpath_escaped_for_shell(),
                                              'updateMetaInfo',
                                              "'" + json.dumps(keyValueMap) + "'")
    (retcode, stdout, stderr) = run_os_command(command)
    print_info_msg("Return code from stack upgrade command, retcode = " + str(retcode))
    if retcode > 0:
      print_error_msg("Error executing metainfo upgrade, please check the "
                      "server logs.")

  return retcode


#
# Repo upgrade
#

def change_objects_owner(args):
  print 'Fixing database objects owner'

  properties = Properties()   #Dummy, args contains the dbms name and parameters already

  factory = DBMSConfigFactory()
  dbms = factory.create(args, properties)

  dbms.change_db_files_owner()

def upgrade_local_repo(args):
  properties = get_ambari_properties()
  if properties == -1:
    print_error_msg("Error getting ambari properties")
    return -1

  stack_location = get_stack_location(properties)
  stack_root_local = os.path.join(stack_location, "HDPLocal")
  if not os.path.exists(stack_root_local):
    print_info_msg("HDPLocal stack directory does not exist, skipping")
    return

  stack_root = os.path.join(stack_location, "HDP")
  if not os.path.exists(stack_root):
    print_info_msg("HDP stack directory does not exist, skipping")
    return

  for stack_version_local in os.listdir(stack_root_local):
    repo_file_local = os.path.join(stack_root_local, stack_version_local, "repos", "repoinfo.xml.rpmsave")
    if not os.path.exists(repo_file_local):
      repo_file_local = os.path.join(stack_root_local, stack_version_local, "repos", "repoinfo.xml")

    repo_file = os.path.join(stack_root, stack_version_local, "repos", "repoinfo.xml")

    print_info_msg("Local repo file: " + repo_file_local)
    print_info_msg("Repo file: " + repo_file_local)

    metainfo_update_items = {}

    if os.path.exists(repo_file_local) and os.path.exists(repo_file):
      local_values = load_stack_values(stack_version_local, repo_file_local)
      repo_values = load_stack_values(stack_version_local, repo_file)
      for k, v in local_values.iteritems():
        if repo_values.has_key(k):
          local_url = local_values[k]
          repo_url = repo_values[k]
          if repo_url != local_url:
            metainfo_update_items[k] = local_url

    run_metainfo_upgrade(args, metainfo_update_items)

#
# Schema upgrade
#

def run_schema_upgrade(args):
  db_title = get_db_type(get_ambari_properties()).title
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

  ensure_jdbc_driver_is_installed(args, get_ambari_properties())

  print 'Upgrading database schema'

  serverClassPath = ServerClassPath(get_ambari_properties(), args)
  class_path = serverClassPath.get_full_ambari_classpath_escaped_for_shell(validate_classpath=True)

  set_debug_mode_from_options(args)
  debug_mode = get_debug_mode()
  debug_start = (debug_mode & 1) or SCHEMA_UPGRADE_DEBUG
  suspend_start = (debug_mode & 2) or SUSPEND_START_MODE
  suspend_mode = 'y' if suspend_start else 'n'
  command = SCHEMA_UPGRADE_HELPER_CMD_DEBUG.format(jdk_path, class_path, suspend_mode) if debug_start else SCHEMA_UPGRADE_HELPER_CMD.format(jdk_path, class_path)

  ambari_user = read_ambari_user()
  current_user = ensure_can_start_under_current_user(ambari_user)
  environ = generate_env(args, ambari_user, current_user)

  (retcode, stdout, stderr) = run_os_command(command, env=environ)
  print_info_msg("Return code from schema upgrade command, retcode = " + str(retcode))
  if stdout:
    print "Console output from schema upgrade command:"
    print stdout
    print
  if stderr:
    print "Error output from schema upgrade command:"
    print stderr
    print
  if retcode > 0:
    print_error_msg("Error executing schema upgrade, please check the server logs.")
  else:
    print_info_msg('Schema upgrade completed')
  return retcode


#
# Upgrades the Ambari Server.
#
def move_user_custom_actions():
  print_info_msg('Moving *.py files from custom_actions to custom_actions/scripts')
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    print_error_msg(err)
    raise FatalException(-1, err)

  try:
    resources_dir = properties[RESOURCES_DIR_PROPERTY]
  except (KeyError), e:
    conf_file = properties.fileName
    err = 'Property ' + str(e) + ' is not defined at ' + conf_file
    print_error_msg(err)
    raise FatalException(1, err)

  custom_actions_dir_path = os.path.join(resources_dir, 'custom_actions')
  custom_actions_scripts_dir_path = os.path.join(custom_actions_dir_path, 'scripts')
  print_info_msg('Moving *.py files from %s to %s' % (custom_actions_dir_path, custom_actions_scripts_dir_path))

  try:
    for custom_action_file_name in os.listdir(custom_actions_dir_path):
      custom_action_file_path = os.path.join(custom_actions_dir_path, custom_action_file_name)
      if os.path.isfile(custom_action_file_path) and custom_action_file_path.endswith('.py'):
        print_info_msg('Moving %s to %s' % (custom_action_file_path, custom_actions_scripts_dir_path))
        shutil.move(custom_action_file_path, custom_actions_scripts_dir_path)
  except (OSError, shutil.Error) as e:
    err = 'Upgrade failed. Can not move *.py files from %s to %s. ' % (custom_actions_dir_path, custom_actions_scripts_dir_path) + str(e)
    print_error_msg(err)
    raise FatalException(1, err)

def upgrade(args):
  if not is_root():
    err = configDefaults.MESSAGE_ERROR_UPGRADE_NOT_ROOT
    raise FatalException(4, err)
  print 'Updating properties in ' + AMBARI_PROPERTIES_FILE + ' ...'
  retcode = update_ambari_properties()
  if not retcode == 0:
    err = AMBARI_PROPERTIES_FILE + ' file can\'t be updated. Exiting'
    raise FatalException(retcode, err)

  retcode = update_ambari_env()
  if not retcode == 0:
    err = AMBARI_ENV_FILE + ' file can\'t be updated. Exiting'
    raise FatalException(retcode, err)

  retcode = update_krb_jaas_login_properties()
  if retcode == -2:
    pass  # no changes done, let's be silent
  elif retcode == 0:
    print 'File ' + AMBARI_KRB_JAAS_LOGIN_FILE + ' updated.'
  elif not retcode == 0:
    err = AMBARI_KRB_JAAS_LOGIN_FILE + ' file can\'t be updated. Exiting'
    raise FatalException(retcode, err)

  restore_custom_services()
  replay_mpack_logs()
  try:
    update_database_name_property(upgrade=True)
  except FatalException:
    return -1

  # Ignore the server version & database options passed via command-line arguments
  parse_properties_file(args)

  #TODO check database version
  change_objects_owner(args)

  retcode = run_schema_upgrade(args)
  if not retcode == 0:
    print_error_msg("Ambari server upgrade failed. Please look at {0}, for more details.".format(configDefaults.SERVER_LOG_FILE))
    raise FatalException(11, 'Schema upgrade failed.')

  user = read_ambari_user()
  if user is None:
    warn = "Can not determine custom ambari user.\n" + SETUP_OR_UPGRADE_MSG
    print_warning_msg(warn)
  else:
    adjust_directory_permissions(user)

  # local repo
  upgrade_local_repo(args)

  # create jdbc symlinks if jdbc drivers are available in resources
  check_jdbc_drivers(args)

  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    print_error_msg(err)
    raise FatalException(-1, err)

  # Move *.py files from custom_actions to custom_actions/scripts
  # This code exists for historic reasons in which custom action python scripts location changed from Ambari 1.7.0 to 2.0.0
  ambari_version = get_ambari_version(properties)
  if ambari_version is None:
    args.warnings.append("*.py files were not moved from custom_actions to custom_actions/scripts.")
  elif compare_versions(ambari_version, "2.0.0") == 0:
    move_user_custom_actions()

  # Remove ADMIN_VIEW directory for upgrading Admin View on Ambari upgrade from 1.7.0 to 2.0.0
  admin_views_dirs = get_admin_views_dir(properties)
  for admin_views_dir in admin_views_dirs:
    shutil.rmtree(admin_views_dir)

  # Remove ambari views directory for the rest of the jars, at the time of upgrade. At restart all jars present in Ambari will be extracted into work directory
  views_dir =  get_views_dir(properties)
  for views in views_dir:
    shutil.rmtree(views)

  # check if ambari has obsolete LDAP configuration
  if properties.get_property(LDAP_PRIMARY_URL_PROPERTY) and not properties.get_property(IS_LDAP_CONFIGURED):
    args.warnings.append("Existing LDAP configuration is detected. You must run the \"ambari-server setup-ldap\" command to adjust existing LDAP configuration.")


#
# Set current cluster version (run Finalize during manual RU)
#
def set_current(options):
  server_status, pid = is_server_runing()
  if not server_status:
    err = 'Ambari Server is not running.'
    raise FatalException(1, err)

  finalize_options = SetCurrentVersionOptions(options)

  if finalize_options.no_finalize_options_set():
    err = 'Must specify --cluster-name and --version-display-name. Please invoke ambari-server.py --help to print the options.'
    raise FatalException(1, err)

  admin_login = get_validated_string_input(prompt="Enter Ambari Admin login: ", default=None,
                                           pattern=None, description=None,
                                           is_pass=False, allowEmpty=False)
  admin_password = get_validated_string_input(prompt="Enter Ambari Admin password: ", default=None,
                                              pattern=None, description=None,
                                              is_pass=True, allowEmpty=False)

  properties = get_ambari_properties()
  if properties == -1:
    raise FatalException(1, "Failed to read properties file.")

  base_url = get_ambari_server_api_base(properties)
  url = base_url + "clusters/{0}/stack_versions".format(finalize_options.cluster_name)
  admin_auth = base64.encodestring('%s:%s' % (admin_login, admin_password)).replace('\n', '')
  request = urllib2.Request(url)
  request.add_header('Authorization', 'Basic %s' % admin_auth)
  request.add_header('X-Requested-By', 'ambari')

  data = {
    "ClusterStackVersions": {
      "repository_version": finalize_options.desired_repo_version,
      "state": "CURRENT",
      "force": finalize_options.force_repo_version
    }
  }

  if get_verbose():
    sys.stdout.write('\nCalling API ' + url + ' : ' + str(data) + '\n')

  request.add_data(json.dumps(data))
  request.get_method = lambda: 'PUT'

  try:
    response = urllib2.urlopen(request)
  except urllib2.HTTPError, e:
    code = e.getcode()
    content = e.read()
    err = 'Error during setting current version. Http status code - {0}. \n {1}'.format(
      code, content)
    raise FatalException(1, err)
  except Exception as e:
    err = 'Setting current version failed. Error details: %s' % e
    raise FatalException(1, err)

  sys.stdout.write('\nCurrent version successfully updated to ' + finalize_options.desired_repo_version)

  sys.stdout.write('\n')
  sys.stdout.flush()

#
# Search for folders with custom services and restore them from backup
#
def restore_custom_services():
  properties = get_ambari_properties()
  if properties == -1:
    err = "Error getting ambari properties"
    print_error_msg(err)
    raise FatalException(-1, err)

  try:
    resources_dir = properties[RESOURCES_DIR_PROPERTY]
  except (KeyError), e:
    conf_file = properties.fileName
    err = 'Property ' + str(e) + ' is not defined at ' + conf_file
    print_error_msg(err)
    raise FatalException(1, err)

  services = glob.glob(os.path.join(resources_dir,"stacks","*","*","services","*"))
  managed_services = []
  for service in services:
    if os.path.isdir(service) and not os.path.basename(service) in managed_services:
      managed_services.append(os.path.basename(service))
  # add deprecated managed services
  managed_services.extend(["NAGIOS","GANGLIA","MAPREDUCE","WEBHCAT"])

  stack_backup_dirs = glob.glob(os.path.join(resources_dir,"stacks_*.old"))
  if stack_backup_dirs:
    last_backup_dir = max(stack_backup_dirs, key=os.path.getctime)
    backup_services = glob.glob(os.path.join(last_backup_dir,"*","*","services","*"))

    regex = re.compile(r'/stacks.*old/')
    for backup_service in backup_services:
      backup_base_service_dir = os.path.dirname(backup_service)
      current_base_service_dir = regex.sub('/stacks/', backup_base_service_dir)
      # if services dir does not exists, we do not manage this stack
      if not os.path.exists(current_base_service_dir):
        continue

      # process dirs only
      if os.path.isdir(backup_service):
        service_name = os.path.basename(backup_service)
        if not service_name in managed_services:
          shutil.copytree(backup_service, os.path.join(current_base_service_dir,service_name))



class SetCurrentVersionOptions:
  def __init__(self, options):
    try:
      self.cluster_name = options.cluster_name
    except AttributeError:
      self.cluster_name = None

    try:
      self.desired_repo_version = options.desired_repo_version
    except AttributeError:
      self.desired_repo_version = None

    try:
      self.force_repo_version = options.force_repo_version
    except AttributeError:
      self.force_repo_version = False

    if not self.force_repo_version:
      self.force_repo_version = False

  def no_finalize_options_set(self):
    return self.cluster_name is None or self.desired_repo_version is None
