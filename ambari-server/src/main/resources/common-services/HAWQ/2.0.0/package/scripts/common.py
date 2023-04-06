"""
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
"""
import crypt
import filecmp
import os
import re
import time
import xml.etree.ElementTree as ET

import custom_params
import hawq_constants
import utils
from resource_management.core.exceptions import Fail
from resource_management.core.logger import Logger
from resource_management.core.resources.accounts import Group, User
from resource_management.core.resources.system import Execute, Directory, File
from resource_management.core.shell import call
from resource_management.core.system import System
from resource_management.libraries.functions.default import default


def setup_user():
  """
  Creates HAWQ user home directory and sets up the correct ownership.
  """
  __create_hawq_user()
  __create_hawq_user_secured()
  __set_home_dir_ownership()


def __create_hawq_user():
  """
  Creates HAWQ user with password and default group.
  """
  import params
  Group(hawq_constants.hawq_group, ignore_failures=True)

  User(hawq_constants.hawq_user,
       gid=hawq_constants.hawq_group,
       password=crypt.crypt(params.hawq_password, "$1$salt$"),
       groups=[hawq_constants.hawq_group, params.user_group],
       ignore_failures=True)

def __create_hawq_user_secured():
  """
  Creates HAWQ secured headless user belonging to hadoop group.
  """
  import params
  Group(hawq_constants.hawq_group_secured, ignore_failures=True)

  User(hawq_constants.hawq_user_secured,
       gid=hawq_constants.hawq_group_secured,
       groups=[hawq_constants.hawq_group_secured, params.user_group],
       ignore_failures=True)

def create_master_dir(dir_path):
  """
  Creates the master directory (hawq_master_dir or hawq_segment_dir) for HAWQ
  """
  utils.create_dir_as_hawq_user(dir_path)
  Execute("chmod 700 {0}".format(dir_path), user=hawq_constants.root_user, timeout=hawq_constants.default_exec_timeout)

def create_temp_dirs(dir_paths):
  """
  Creates the temp directories (hawq_master_temp_dir or hawq_segment_temp_dir) for HAWQ
  """
  for path in dir_paths.split(','):
    if path != "":
      utils.create_dir_as_hawq_user(path)

def __set_home_dir_ownership():
  """
  Updates the HAWQ user home directory to be owned by gpadmin:gpadmin.
  """
  command = "chown -R {0}:{1} {2}".format(hawq_constants.hawq_user, hawq_constants.hawq_group, hawq_constants.hawq_home_dir)
  Execute(command, timeout=hawq_constants.default_exec_timeout)


def setup_common_configurations():
  """
  Sets up the config files common to master, standby and segment nodes.
  """
  import params

  params.XmlConfig("hdfs-client.xml",
                   configurations=params.hdfs_client,
                   configuration_attributes=params.config_attrs['hdfs-client'])

  params.XmlConfig("yarn-client.xml",
                   configurations=params.yarn_client,
                   configuration_attributes=params.config_attrs['yarn-client'])

  params.XmlConfig("hawq-site.xml",
                   configurations=params.hawq_site,
                   configuration_attributes=params.config_attrs['hawq-site'])
  __set_osparams()


def __set_osparams():
  """
  Updates parameters in sysctl.conf and limits.conf required by HAWQ.
  """
  # Create a temp scratchpad directory
  utils.create_dir_as_hawq_user(hawq_constants.hawq_tmp_dir)

  # Suse doesn't supports loading values from files in /etc/sysctl.d
  # So we will have to directly edit the sysctl file
  if System.get_instance().os_family == "suse":
    # Update /etc/sysctl.conf
    __update_sysctl_file_suse()
  else:
    # Update /etc/sysctl.d/hawq.conf
    __update_sysctl_file()

  __update_limits_file()


def __update_limits_file():
  """
  Updates /etc/security/limits.d/hawq.conf file with the HAWQ parameters.
  """
  import params
  # Ensure limits directory exists
  Directory(hawq_constants.limits_conf_dir, create_parents = True, owner=hawq_constants.root_user, group=hawq_constants.root_user)

  # Generate limits for hawq user
  limits_file_content = "#### HAWQ Limits Parameters  ###########\n"
  for key, value in params.hawq_limits.items():
    if not __valid_input(value):
      raise Exception("Value {0} for parameter {1} contains non-numeric characters which are not allowed (except whitespace), please fix the value and retry".format(value, key))
    """
    Content of the file to be written should be of the format
    gpadmin soft nofile 290000
    gpadmin hard nofile 290000
    key used in the configuration is of the format soft_nofile, thus strip '_' and replace with 'space'
    """
    limits_file_content += "{0} {1} {2}\n".format(hawq_constants.hawq_user, re.sub("_", " ", key), value.strip())
  File('{0}/{1}.conf'.format(hawq_constants.limits_conf_dir, hawq_constants.hawq_user), content=limits_file_content,
       owner=hawq_constants.hawq_user, group=hawq_constants.hawq_group)


def __valid_input(value):
  """
  Validate if input value contains number (whitespaces allowed), return true if found else false
  """
  return re.search("^ *[0-9][0-9 ]*$", value)


def __convert_sysctl_dict_to_text():
  """
  Convert sysctl configuration dict to text with each property value pair separated on new line
  """
  import params
  sysctl_file_content = "### HAWQ System Parameters ###########\n"
  for key, value in params.hawq_sysctl.items():
    if not __valid_input(value):
      raise Exception("Value {0} for parameter {1} contains non-numeric characters which are not allowed (except whitespace), please fix the value and retry".format(value, key))
    sysctl_file_content += "{0} = {1}\n".format(key, value)
  return sysctl_file_content


def __update_sysctl_file():
  """
  Updates /etc/sysctl.d/hawq_sysctl.conf file with the HAWQ parameters on CentOS/RHEL.
  """
  # Ensure sys ctl sub-directory exists
  Directory(hawq_constants.sysctl_conf_dir, create_parents = True, owner=hawq_constants.root_user, group=hawq_constants.root_user)

  # Generate temporary file with kernel parameters needed by hawq
  File(hawq_constants.hawq_sysctl_tmp_file, content=__convert_sysctl_dict_to_text(), owner=hawq_constants.hawq_user,
       group=hawq_constants.hawq_group)

  is_changed = True
  if os.path.exists(hawq_constants.hawq_sysctl_tmp_file) and os.path.exists(hawq_constants.hawq_sysctl_file):
    is_changed = not filecmp.cmp(hawq_constants.hawq_sysctl_file, hawq_constants.hawq_sysctl_tmp_file)

  if is_changed:
    # Generate file with kernel parameters needed by hawq, only if something
    # has been changed by user
    Execute("cp -p {0} {1}".format(hawq_constants.hawq_sysctl_tmp_file, hawq_constants.hawq_sysctl_file))

    # Reload kernel sysctl parameters from hawq file.
    Execute("sysctl -e -p {0}".format(hawq_constants.hawq_sysctl_file), timeout=hawq_constants.default_exec_timeout)

  # Wipe out temp file
  File(hawq_constants.hawq_sysctl_tmp_file, action='delete')


def __update_sysctl_file_suse():
  """
  Updates /etc/sysctl.conf file with the HAWQ parameters on SUSE.
  """
  # Backup file
  backup_file_name = hawq_constants.sysctl_backup_file.format(str(int(time.time())))
  try:
    # Generate file with kernel parameters needed by hawq to temp file
    File(hawq_constants.hawq_sysctl_tmp_file, content=__convert_sysctl_dict_to_text(), owner=hawq_constants.hawq_user,
        group=hawq_constants.hawq_group)

    sysctl_file_dict = utils.read_file_to_dict(hawq_constants.sysctl_suse_file)
    sysctl_file_dict_original = sysctl_file_dict.copy()
    hawq_sysctl_dict = utils.read_file_to_dict(hawq_constants.hawq_sysctl_tmp_file)

    # Merge common system file with hawq specific file
    sysctl_file_dict.update(hawq_sysctl_dict)

    if sysctl_file_dict_original != sysctl_file_dict:
      # Backup file
      Execute("cp {0} {1}".format(hawq_constants.sysctl_suse_file, backup_file_name), timeout=hawq_constants.default_exec_timeout)
      # Write merged properties to file
      utils.write_dict_to_file(sysctl_file_dict, hawq_constants.sysctl_suse_file)
      # Reload kernel sysctl parameters from /etc/sysctl.conf
      Execute("sysctl -e -p", timeout=hawq_constants.default_exec_timeout)

  except Exception as e:
    Logger.error("Error occurred while updating sysctl.conf file, reverting the contents" + str(e))
    Execute("cp {0} {1}".format(hawq_constants.sysctl_suse_file, hawq_constants.hawq_sysctl_tmp_file))
    Execute("mv {0} {1}".format(backup_file_name, hawq_constants.sysctl_suse_file), timeout=hawq_constants.default_exec_timeout)
    Logger.error("Please execute `sysctl -e -p` on the command line manually to reload the contents of file {0}".format(
      hawq_constants.hawq_sysctl_tmp_file))
    raise Fail("Failed to update sysctl.conf file ")


def get_local_hawq_site_property_value(property_name):
  """
  Fetches the value of the property specified, from the local hawq-site.xml.
  """
  hawq_site_path = None
  try:
    hawq_site_path = os.path.join(hawq_constants.hawq_config_dir, "hawq-site.xml")
    hawq_site_root = ET.parse(hawq_site_path).getroot()
    for property in hawq_site_root.findall("property"):
      for item in property:
        if item.tag == 'name':
          current_property_name = item.text.strip() if item and item.text else item.text
        elif item.tag == 'value':
          current_property_value = item.text.strip() if item and item.text else item.text
      if property_name == current_property_name:
          return current_property_value
    raise #If property has not been found
  except Exception:
    raise Fail("Unable to read property {0} from local {1}".format(property_name, hawq_site_path))

def validate_configuration():
  """
  Validates if YARN is present in the configuration when the user specifies YARN as HAWQ's resource manager.
  """
  import params

  # At this point, hawq should be included.
  if 'hawq-site' not in params.config['configurations']:
    raise Fail("Configurations does not contain hawq-site. Please include HAWQ")

  # If HAWQ is set to use YARN and YARN is not configured, error.
  rm_type = params.config["configurations"]["hawq-site"].get("hawq_global_rm_type")
  if rm_type == "yarn" and "yarn-site" not in params.config["configurations"]:
    raise Fail("HAWQ is set to use YARN but YARN is not deployed. " + 
               "hawq_global_rm_type property in hawq-site is set to 'yarn' but YARN is not configured. " + 
               "Please deploy YARN before starting HAWQ or change the value of hawq_global_rm_type property to 'none'")

def start_component(component_name, port, data_dir):
  """
  If data directory exists start the component, else initialize the component.
  Initialization starts the component
  """
  import params

  __check_dfs_truncate_enforced()
  if component_name == hawq_constants.MASTER:
    # Check the owner for hawq_data directory
    data_dir_owner = hawq_constants.hawq_user_secured if params.security_enabled else hawq_constants.hawq_user
    # Change owner recursively (if needed)
    if __get_hdfs_dir_owner() != data_dir_owner:
      params.HdfsResource(params.hawq_hdfs_data_dir,
                          type="directory",
                          action="create_on_execute",
                          owner=data_dir_owner,
                          group=hawq_constants.hawq_group,
                          recursive_chown=True,
                          mode=0o755)
      params.HdfsResource(None, action="execute")

  options_str = "{0} -a -v".format(component_name)
  if os.path.exists(os.path.join(data_dir, hawq_constants.postmaster_opts_filename)):
    return utils.exec_hawq_operation(hawq_constants.START, options_str,
                                     not_if=utils.generate_hawq_process_status_cmd(component_name, port))

  # Initialize HAWQ
  if component_name == hawq_constants.MASTER:
    utils.exec_hawq_operation(hawq_constants.INIT, options_str + " --ignore-bad-hosts")
    utils.exec_psql_cmd('create database {0};'.format(hawq_constants.hawq_user),
                        params.hawqmaster_host, params.hawq_master_address_port, ignore_error=True)
  else:
    utils.exec_hawq_operation(hawq_constants.INIT, options_str)

def stop_component(component_name, mode):
  """
  Stops the component
  Unlike start_component, port is obtained from local hawq-site.xml as Ambari pontentially have a new value through UI.
  """
  port_property_name = hawq_constants.COMPONENT_ATTRIBUTES_MAP[component_name]['port_property']
  port_number = get_local_hawq_site_property_value(port_property_name)
  utils.exec_hawq_operation(hawq_constants.STOP,
                            "{0} -M {1} -a -v".format(component_name, mode),
                            only_if=utils.generate_hawq_process_status_cmd(component_name, port_number))

def __get_hdfs_dir_owner():
  import params

  # Check the owner for hawq_data directory
  kinit_cmd = "{0} -kt {1} {2};".format(params.kinit_path_local, params.hdfs_user_keytab, params.hdfs_principal_name) if params.security_enabled else ""
  cmd = kinit_cmd + "hdfs dfs -ls {0} | sed '1d;s/  */ /g' | cut -d\\  -f3".format(params.hawq_hdfs_data_dir)
  returncode, stdout = call(cmd, user=params.hdfs_superuser, timeout=300)
  if returncode:
    raise Fail("Unable to determine the ownership for HDFS dir {0}".format(params.hawq_hdfs_data_dir))
  return stdout.strip()

def __check_dfs_truncate_enforced():
  """
  If enforce_hdfs_truncate is set to True:
    throw an ERROR, HAWQ components start should fail
  Else:
    throw a WARNING,
  """
  DFS_ALLOW_TRUNCATE_WARNING_MSG = "It is recommended to set dfs.allow.truncate as true in hdfs-site.xml configuration file, currently it is set to false. Please review HAWQ installation guide for more information."

  # Check if dfs.allow.truncate exists in hdfs-site.xml and throw appropriate exception if not set to True
  dfs_allow_truncate = default("/configurations/hdfs-site/dfs.allow.truncate", None)
  if dfs_allow_truncate is None or str(dfs_allow_truncate).lower() != 'true':
    if custom_params.enforce_hdfs_truncate:
      raise Fail("**ERROR**: {0}".format(DFS_ALLOW_TRUNCATE_WARNING_MSG))
    else:
      Logger.error("**WARNING**: {0}".format(DFS_ALLOW_TRUNCATE_WARNING_MSG))
