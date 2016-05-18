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
import tempfile

__all__ = ["Script"]

import re
import os
import sys
import logging
import platform
import inspect
import tarfile
from optparse import OptionParser
import resource_management
from ambari_commons import OSCheck, OSConst
from ambari_commons.constants import UPGRADE_TYPE_NON_ROLLING, UPGRADE_TYPE_ROLLING
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.libraries.resources import XmlConfig
from resource_management.libraries.resources import PropertiesFile
from resource_management.core.resources import File, Directory
from resource_management.core.source import InlineTemplate
from resource_management.core.environment import Environment
from resource_management.core.logger import Logger
from resource_management.core.exceptions import Fail, ClientComponentHasNoStatus, ComponentIsNotRunning
from resource_management.core.resources.packaging import Package
from resource_management.libraries.functions.version_select_util import get_component_version
from resource_management.libraries.functions.version import compare_versions
from resource_management.libraries.functions.version import format_stack_version
from resource_management.libraries.functions import stack_tools
from resource_management.libraries.functions.constants import Direction
from resource_management.libraries.functions import packages_analyzer
from resource_management.libraries.script.config_dictionary import ConfigDictionary, UnknownConfiguration
from resource_management.core.resources.system import Execute
from contextlib import closing
from resource_management.libraries.functions.stack_features import check_stack_feature
from resource_management.libraries.functions.constants import StackFeature
from resource_management.libraries.functions.show_logs import show_logs

import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.

if OSCheck.is_windows_family():
  from resource_management.libraries.functions.install_windows_msi import install_windows_msi
  from resource_management.libraries.functions.reload_windows_env import reload_windows_env
  from resource_management.libraries.functions.zip_archive import archive_dir
  from resource_management.libraries.resources import Msi
else:
  from resource_management.libraries.functions.tar_archive import archive_dir

USAGE = """Usage: {0} <COMMAND> <JSON_CONFIG> <BASEDIR> <STROUTPUT> <LOGGING_LEVEL> <TMP_DIR>

<COMMAND> command type (INSTALL/CONFIGURE/START/STOP/SERVICE_CHECK...)
<JSON_CONFIG> path to command json file. Ex: /var/lib/ambari-agent/data/command-2.json
<BASEDIR> path to service metadata dir. Ex: /var/lib/ambari-agent/cache/common-services/HDFS/2.1.0.2.0/package
<STROUTPUT> path to file with structured command output (file will be created). Ex:/tmp/my.txt
<LOGGING_LEVEL> log level for stdout. Ex:DEBUG,INFO
<TMP_DIR> temporary directory for executable scripts. Ex: /var/lib/ambari-agent/tmp
"""

_PASSWORD_MAP = {"/configurations/cluster-env/hadoop.user.name":"/configurations/cluster-env/hadoop.user.password"}
STACK_VERSION_PLACEHOLDER = "${stack_version}"
COUNT_OF_LAST_LINES_OF_OUT_FILES_LOGGED = 100
OUT_FILES_MASK = "*.out"
AGENT_TASKS_LOG_FILE = "/var/log/ambari-agent/agent_tasks.log"

def get_path_from_configuration(name, configuration):
  subdicts = filter(None, name.split('/'))

  for x in subdicts:
    if x in configuration:
      configuration = configuration[x]
    else:
      return None

  return configuration

class Script(object):
  """
  Executes a command for custom service. stdout and stderr are written to
  tmpoutfile and to tmperrfile respectively.
  Script instances share configuration as a class parameter and therefore
  different Script instances can not be used from different threads at
  the same time within a single python process

  Accepted command line arguments mapping:
  1 command type (START/STOP/...)
  2 path to command json file
  3 path to service metadata dir (Directory "package" inside service directory)
  4 path to file with structured command output (file will be created)
  """
  config = None
  stack_version_from_distro_select = None
  structuredOut = {}
  command_data_file = ""
  basedir = ""
  stroutfile = ""
  logging_level = ""

  # Class variable
  tmp_dir = ""
 
  def load_structured_out(self):
    Script.structuredOut = {}
    if os.path.exists(self.stroutfile):
      if os.path.getsize(self.stroutfile) > 0:
        with open(self.stroutfile, 'r') as fp:
          try:
            Script.structuredOut = json.load(fp)
          except Exception:
            errMsg = 'Unable to read structured output from ' + self.stroutfile
            Logger.logger.exception(errMsg)
            pass

    # version is only set in a specific way and should not be carried
    if "version" in Script.structuredOut:
      del Script.structuredOut["version"]
    # reset security issues and errors found on previous runs
    if "securityIssuesFound" in Script.structuredOut:
      del Script.structuredOut["securityIssuesFound"]
    if "securityStateErrorInfo" in Script.structuredOut:
      del Script.structuredOut["securityStateErrorInfo"]

  def put_structured_out(self, sout):
    Script.structuredOut.update(sout)
    try:
      with open(self.stroutfile, 'w') as fp:
        json.dump(Script.structuredOut, fp)
    except IOError, err:
      Script.structuredOut.update({"errMsg" : "Unable to write to " + self.stroutfile})
      
  def get_component_name(self):
    """
    To be overridden by subclasses.
     Returns a string with the component name used in selecting the version.
    """
    pass

  def save_component_version_to_structured_out(self):
    """
    :param stack_name: One of HDP, HDPWIN, PHD, BIGTOP.
    :return: Append the version number to the structured out.
    """
    stack_name = Script.get_stack_name()
    component_name = self.get_component_name()
    
    if component_name and stack_name:
      component_version = get_component_version(stack_name, component_name)

      if component_version:
        self.put_structured_out({"version": component_version})

        # if repository_version_id is passed, pass it back with the version
        from resource_management.libraries.functions.default import default
        repo_version_id = default("/hostLevelParams/repository_version_id", None)
        if repo_version_id:
          self.put_structured_out({"repository_version_id": repo_version_id})


  def should_expose_component_version(self, command_name):
    """
    Analyzes config and given command to determine if stack version should be written
    to structured out. Currently only HDP stack versions >= 2.2 are supported.
    :param command_name: command name
    :return: True or False
    """
    from resource_management.libraries.functions.default import default
    stack_version_unformatted = str(default("/hostLevelParams/stack_version", ""))
    stack_version_formatted = format_stack_version(stack_version_unformatted)
    if stack_version_formatted and check_stack_feature(StackFeature.ROLLING_UPGRADE, stack_version_formatted):
      if command_name.lower() == "status":
        request_version = default("/commandParams/request_version", None)
        if request_version is not None:
          return True
      else:
        # Populate version only on base commands
        return command_name.lower() == "start" or command_name.lower() == "install" or command_name.lower() == "restart"
    return False

  def execute(self):
    """
    Sets up logging;
    Parses command parameters and executes method relevant to command type
    """
    parser = OptionParser()
    parser.add_option("-o", "--out-files-logging", dest="log_out_files", action="store_true",
                      help="use this option to enable outputting *.out files of the service pre-start")
    (self.options, args) = parser.parse_args()
    
    self.log_out_files = self.options.log_out_files
    
    # parse arguments
    if len(args) < 6:
     print "Script expects at least 6 arguments"
     print USAGE.format(os.path.basename(sys.argv[0])) # print to stdout
     sys.exit(1)
     
    self.command_name = str.lower(sys.argv[1])
    self.command_data_file = sys.argv[2]
    self.basedir = sys.argv[3]
    self.stroutfile = sys.argv[4]
    self.load_structured_out()
    self.logging_level = sys.argv[5]
    Script.tmp_dir = sys.argv[6]
    
    logging_level_str = logging._levelNames[self.logging_level]
    Logger.initialize_logger(__name__, logging_level=logging_level_str)

    # on windows we need to reload some of env variables manually because there is no default paths for configs(like
    # /etc/something/conf on linux. When this env vars created by one of the Script execution, they can not be updated
    # in agent, so other Script executions will not be able to access to new env variables
    if OSCheck.is_windows_family():
      reload_windows_env()

    try:
      with open(self.command_data_file) as f:
        pass
        Script.config = ConfigDictionary(json.load(f))
        # load passwords here(used on windows to impersonate different users)
        Script.passwords = {}
        for k, v in _PASSWORD_MAP.iteritems():
          if get_path_from_configuration(k, Script.config) and get_path_from_configuration(v, Script.config):
            Script.passwords[get_path_from_configuration(k, Script.config)] = get_path_from_configuration(v, Script.config)

    except IOError:
      Logger.logger.exception("Can not read json file with command parameters: ")
      sys.exit(1)

    # Run class method depending on a command type
    try:
      method = self.choose_method_to_execute(self.command_name)
      with Environment(self.basedir, tmp_dir=Script.tmp_dir) as env:
        env.config.download_path = Script.tmp_dir
        
        if self.command_name == "start" and not self.is_hook():
          self.pre_start()
        
        method(env)
    finally:
      if self.should_expose_component_version(self.command_name):
        self.save_component_version_to_structured_out()
        
  def is_hook(self):
    from resource_management.libraries.script.hook import Hook
    return (Hook in self.__class__.__bases__)
        
  def get_log_folder(self):
    return ""
  
  def get_user(self):
    return ""
        
  def pre_start(self):
    if self.log_out_files:
      log_folder = self.get_log_folder()
      user = self.get_user()
      
      if log_folder == "":
        Logger.logger.warn("Log folder for current script is not defined")
        return
      
      if user == "":
        Logger.logger.warn("User for current script is not defined")
        return
      
      show_logs(log_folder, user, lines_count=COUNT_OF_LAST_LINES_OF_OUT_FILES_LOGGED, mask=OUT_FILES_MASK)

  def choose_method_to_execute(self, command_name):
    """
    Returns a callable object that should be executed for a given command.
    """
    self_methods = dir(self)
    if not command_name in self_methods:
      raise Fail("Script '{0}' has no method '{1}'".format(sys.argv[0], command_name))
    method = getattr(self, command_name)
    return method
  
  def get_stack_version_before_packages_installed(self):
    """
    This works in a lazy way (calculates the version first time and stores it). 
    If you need to recalculate the version explicitly set:
    
    Script.stack_version_from_distro_select = None
    
    before the call. However takes a bit of time, so better to avoid.

    :return: stack version including the build number. e.g.: 2.3.4.0-1234.
    """
    # preferred way is to get the actual selected version of current component
    component_name = self.get_component_name()
    if not Script.stack_version_from_distro_select and component_name:
      from resource_management.libraries.functions import stack_select
      Script.stack_version_from_distro_select = stack_select.get_stack_version_before_install(component_name)
      
    # If <stack-selector-tool> has not yet been done (situations like first install),
    # we can use <stack-selector-tool> version itself.
    if not Script.stack_version_from_distro_select:
      Script.stack_version_from_distro_select = packages_analyzer.getInstalledPackageVersion(
              stack_tools.get_stack_tool_package(stack_tools.STACK_SELECTOR_NAME))
      
    return Script.stack_version_from_distro_select
  
  def format_package_name(self, name):
    from resource_management.libraries.functions.default import default
    """
    This function replaces ${stack_version} placeholder into actual version.  If the package
    version is passed from the server, use that as an absolute truth.
    """

    # two different command types put things in different objects.  WHY.
    # package_version is the form W_X_Y_Z_nnnn
    package_version = default("roleParams/package_version", None)
    if not package_version:
      package_version = default("hostLevelParams/package_version", None)

    package_delimiter = '-' if OSCheck.is_ubuntu_family() else '_'

    if package_version:
      stack_version_package_formatted = package_version
      if OSCheck.is_ubuntu_family():
        stack_version_package_formatted = package_version.replace('_', package_delimiter)
    else:
      stack_version_package_formatted = self.get_stack_version_before_packages_installed().replace('.', package_delimiter).replace('-', package_delimiter) if STACK_VERSION_PLACEHOLDER in name else name

    package_name = name.replace(STACK_VERSION_PLACEHOLDER, stack_version_package_formatted)
    
    return package_name

  @staticmethod
  def get_config():
    """
    HACK. Uses static field to store configuration. This is a workaround for
    "circular dependency" issue when importing params.py file and passing to
     it a configuration instance.
    """
    return Script.config

  @staticmethod
  def get_password(user):
    return Script.passwords[user]

  @staticmethod
  def get_tmp_dir():
    """
    HACK. Uses static field to avoid "circular dependency" issue when
    importing params.py.
    """
    return Script.tmp_dir

  @staticmethod
  def get_component_from_role(role_directory_map, default_role):
    """
    Gets the <stack-root>/current/<component> component given an Ambari role,
    such as DATANODE or HBASE_MASTER.
    :return:  the component name, such as hbase-master
    """
    from resource_management.libraries.functions.default import default

    command_role = default("/role", default_role)
    if command_role in role_directory_map:
      return role_directory_map[command_role]
    else:
      return role_directory_map[default_role]

  @staticmethod
  def get_stack_name():
    """
    Gets the name of the stack from hostLevelParams/stack_name.
    :return: a stack name or None
    """
    from resource_management.libraries.functions.default import default
    return default("/hostLevelParams/stack_name", "HDP")

  @staticmethod
  def get_stack_root():
    """
    Get the stack-specific install root directory
    :return: stack_root
    """
    from resource_management.libraries.functions.default import default
    stack_name = Script.get_stack_name()
    return default("/configurations/cluster-env/stack_root", "/usr/{0}".format(stack_name.lower()))

  @staticmethod
  def get_stack_version():
    """
    Gets the normalized version of the stack in the form #.#.#.# if it is
    present on the configurations sent.
    :return: a normalized stack version or None
    """
    config = Script.get_config()
    if 'hostLevelParams' not in config or 'stack_version' not in config['hostLevelParams']:
      return None

    stack_version_unformatted = str(config['hostLevelParams']['stack_version'])

    if stack_version_unformatted is None or stack_version_unformatted == '':
      return None

    return format_stack_version(stack_version_unformatted)

  @staticmethod
  def in_stack_upgrade():
    from resource_management.libraries.functions.default import default

    upgrade_direction = default("/commandParams/upgrade_direction", None)
    return upgrade_direction is not None and upgrade_direction in [Direction.UPGRADE, Direction.DOWNGRADE]


  @staticmethod
  def is_stack_greater(stack_version_formatted, compare_to_version):
    """
    Gets whether the provided stack_version_formatted (normalized)
    is greater than the specified stack version
    :param stack_version_formatted: the version of stack to compare
    :param compare_to_version: the version of stack to compare to
    :return: True if the command's stack is greater than the specified version
    """
    if stack_version_formatted is None or stack_version_formatted == "":
      return False

    return compare_versions(stack_version_formatted, compare_to_version) > 0

  @staticmethod
  def is_stack_greater_or_equal(compare_to_version):
    """
    Gets whether the hostLevelParams/stack_version, after being normalized,
    is greater than or equal to the specified stack version
    :param compare_to_version: the version to compare to
    :return: True if the command's stack is greater than or equal the specified version
    """
    return Script.is_stack_greater_or_equal_to(Script.get_stack_version(), compare_to_version)

  @staticmethod
  def is_stack_greater_or_equal_to(stack_version_formatted, compare_to_version):
    """
    Gets whether the provided stack_version_formatted (normalized)
    is greater than or equal to the specified stack version
    :param stack_version_formatted: the version of stack to compare
    :param compare_to_version: the version of stack to compare to
    :return: True if the command's stack is greater than or equal to the specified version
    """
    if stack_version_formatted is None or stack_version_formatted == "":
      return False

    return compare_versions(stack_version_formatted, compare_to_version) >= 0

  @staticmethod
  def is_stack_less_than(compare_to_version):
    """
    Gets whether the hostLevelParams/stack_version, after being normalized,
    is less than the specified stack version
    :param compare_to_version: the version to compare to
    :return: True if the command's stack is less than the specified version
    """
    stack_version_formatted = Script.get_stack_version()

    if stack_version_formatted is None:
      return False

    return compare_versions(stack_version_formatted, compare_to_version) < 0

  def install(self, env):
    """
    Default implementation of install command is to install all packages
    from a list, received from the server.
    Feel free to override install() method with your implementation. It
    usually makes sense to call install_packages() manually in this case
    """
    self.install_packages(env)

  def install_packages(self, env):
    """
    List of packages that are required< by service is received from the server
    as a command parameter. The method installs all packages
    from this list
    
    exclude_packages - list of regexes (possibly raw strings as well), the
    packages which match the regex won't be installed.
    NOTE: regexes don't have Python syntax, but simple package regexes which support only * and .* and ?
    """
    config = self.get_config()

    if 'host_sys_prepped' in config['hostLevelParams']:
      # do not install anything on sys-prepped host
      if config['hostLevelParams']['host_sys_prepped'] == True:
        Logger.info("Node has all packages pre-installed. Skipping.")
        return
      pass
    try:
      package_list_str = config['hostLevelParams']['package_list']
      agent_stack_retry_on_unavailability = bool(config['hostLevelParams']['agent_stack_retry_on_unavailability'])
      agent_stack_retry_count = int(config['hostLevelParams']['agent_stack_retry_count'])

      if isinstance(package_list_str, basestring) and len(package_list_str) > 0:
        package_list = json.loads(package_list_str)
        for package in package_list:
          if Script.check_package_condition(package):
            name = self.format_package_name(package['name'])
            # HACK: On Windows, only install ambari-metrics packages using Choco Package Installer
            # TODO: Update this once choco packages for hadoop are created. This is because, service metainfo.xml support
            # <osFamily>any<osFamily> which would cause installation failure on Windows.
            if OSCheck.is_windows_family():
              if "ambari-metrics" in name:
                Package(name)
            else:
              Package(name,
                      retry_on_repo_unavailability=agent_stack_retry_on_unavailability,
                      retry_count=agent_stack_retry_count)
    except KeyError:
      pass  # No reason to worry

    if OSCheck.is_windows_family():
      #TODO hacky install of windows msi, remove it or move to old(2.1) stack definition when component based install will be implemented
      hadoop_user = config["configurations"]["cluster-env"]["hadoop.user.name"]
      install_windows_msi(config['hostLevelParams']['jdk_location'],
                          config["hostLevelParams"]["agentCacheDir"], ["hdp-2.3.0.0.winpkg.msi", "hdp-2.3.0.0.cab", "hdp-2.3.0.0-01.cab"],
                          hadoop_user, self.get_password(hadoop_user),
                          str(config['hostLevelParams']['stack_version']))
      reload_windows_env()
      
  @staticmethod
  def check_package_condition(package):
    from resource_management.libraries.functions import package_conditions
    condition = package['condition']
    name = package['name']
    
    if not condition:
      return True
    
    try:
      chooser_method = getattr(package_conditions, condition)
    except AttributeError:
      raise Fail("Condition with name '{0}', when installing package {1}. Please check package_conditions.py.".format(condition, name))

    return chooser_method()
      
  @staticmethod
  def matches_any_regexp(string, regexp_list):
    for regex in regexp_list:
      # we cannot use here Python regex, since * will create some troubles matching plaintext names. 
      package_regex = '^' + re.escape(regex).replace('\\.\\*','.*').replace("\\?", ".").replace("\\*", ".*") + '$'
      if re.match(package_regex, string):
        return True
    return False

  @staticmethod
  def fail_with_error(message):
    """
    Prints error message and exits with non-zero exit code
    """
    print("Error: " + message)
    sys.stderr.write("Error: " + message)
    sys.exit(1)


  def start(self, env, upgrade_type=None):
    """
    To be overridden by subclasses
    """
    self.fail_with_error("start method isn't implemented")

  def stop(self, env, upgrade_type=None):
    """
    To be overridden by subclasses
    """
    self.fail_with_error("stop method isn't implemented")

  # TODO, remove after all services have switched to pre_upgrade_restart
  def pre_rolling_restart(self, env):
    """
    To be overridden by subclasses
    """
    pass

  def restart(self, env):
    """
    Default implementation of restart command is to call stop and start methods
    Feel free to override restart() method with your implementation.
    For client components we call install
    """
    config = self.get_config()
    componentCategory = None
    try:
      componentCategory = config['roleParams']['component_category']
    except KeyError:
      pass

    restart_type = ""
    direction = None
    if config is not None:
      command_params = config["commandParams"] if "commandParams" in config else None
      if command_params is not None:
        restart_type = command_params["restart_type"] if "restart_type" in command_params else ""
        direction = command_params["upgrade_direction"] if "upgrade_direction" in command_params else None

    upgrade_type = None
    if restart_type.lower() == "rolling_upgrade":
      upgrade_type = UPGRADE_TYPE_ROLLING
    elif restart_type.lower() == "nonrolling_upgrade":
      upgrade_type = UPGRADE_TYPE_NON_ROLLING

    is_stack_upgrade = upgrade_type is not None

    # need this before actually executing so that failures still report upgrade info
    if is_stack_upgrade:
      upgrade_info = {"upgrade_type": restart_type}
      if direction is not None:
        upgrade_info["direction"] = direction.upper()

      Script.structuredOut.update(upgrade_info)

    if componentCategory and componentCategory.strip().lower() == 'CLIENT'.lower():
      if is_stack_upgrade:
        # Remain backward compatible with the rest of the services that haven't switched to using
        # the pre_upgrade_restart method. Once done. remove the else-block.
        if "pre_upgrade_restart" in dir(self):
          self.pre_upgrade_restart(env, upgrade_type=upgrade_type)
        else:
          self.pre_rolling_restart(env)

      self.install(env)
    else:
      # To remain backward compatible with older stacks, only pass upgrade_type if available.
      # TODO, remove checking the argspec for "upgrade_type" once all of the services support that optional param.
      if "upgrade_type" in inspect.getargspec(self.stop).args:
        self.stop(env, upgrade_type=upgrade_type)
      else:
        if is_stack_upgrade:
          self.stop(env, rolling_restart=(restart_type == "rolling_upgrade"))
        else:
          self.stop(env)

      if is_stack_upgrade:
        # Remain backward compatible with the rest of the services that haven't switched to using
        # the pre_upgrade_restart method. Once done. remove the else-block.
        if "pre_upgrade_restart" in dir(self):
          self.pre_upgrade_restart(env, upgrade_type=upgrade_type)
        else:
          self.pre_rolling_restart(env)

      try:
        self.status(env)
        raise Fail("Stop command finished but process keep running.")
      except ComponentIsNotRunning as e:
        pass  # expected
      except ClientComponentHasNoStatus as e:
        pass  # expected

      # To remain backward compatible with older stacks, only pass upgrade_type if available.
      # TODO, remove checking the argspec for "upgrade_type" once all of the services support that optional param.
      self.pre_start()
      if "upgrade_type" in inspect.getargspec(self.start).args:
        self.start(env, upgrade_type=upgrade_type)
      else:
        if is_stack_upgrade:
          self.start(env, rolling_restart=(restart_type == "rolling_upgrade"))
        else:
          self.start(env)

      if is_stack_upgrade:
        # Remain backward compatible with the rest of the services that haven't switched to using
        # the post_upgrade_restart method. Once done. remove the else-block.
        if "post_upgrade_restart" in dir(self):
          self.post_upgrade_restart(env, upgrade_type=upgrade_type)
        else:
          self.post_rolling_restart(env)

    if self.should_expose_component_version("restart"):
      self.save_component_version_to_structured_out()


  # TODO, remove after all services have switched to post_upgrade_restart
  def post_rolling_restart(self, env):
    """
    To be overridden by subclasses
    """
    pass

  def configure(self, env, upgrade_type=None):
    """
    To be overridden by subclasses
    """
    self.fail_with_error('configure method isn\'t implemented')

  def security_status(self, env):
    """
    To be overridden by subclasses to provide the current security state of the component.
    Implementations are required to set the "securityState" property of the structured out data set
    to one of the following values:

      UNSECURED        - If the component is not configured for any security protocol such as
                         Kerberos
      SECURED_KERBEROS - If the component is configured for Kerberos
      UNKNOWN          - If the security state cannot be determined
      ERROR            - If the component is supposed to be secured, but there are issues with the
                         configuration.  For example, if the component is configured for Kerberos
                         but the configured principal and keytab file fail to kinit
    """
    self.put_structured_out({"securityState": "UNKNOWN"})

  def generate_configs_get_template_file_content(self, filename, dicts):
    config = self.get_config()
    content = ''
    for dict in dicts.split(','):
      if dict.strip() in config['configurations']:
        try:
          content += config['configurations'][dict.strip()]['content']
        except Fail:
          # 'content' section not available in the component client configuration
          pass

    return content

  def generate_configs_get_xml_file_content(self, filename, dict):
    config = self.get_config()
    return {'configurations':config['configurations'][dict],
            'configuration_attributes':config['configuration_attributes'][dict]}
    
  def generate_configs_get_xml_file_dict(self, filename, dict):
    config = self.get_config()
    return config['configurations'][dict]

  def generate_configs(self, env):
    """
    Generates config files and stores them as an archive in tmp_dir
    based on xml_configs_list and env_configs_list from commandParams
    """
    import params
    env.set_params(params)
    
    config = self.get_config()

    xml_configs_list = config['commandParams']['xml_configs_list']
    env_configs_list = config['commandParams']['env_configs_list']
    properties_configs_list = config['commandParams']['properties_configs_list']

    Directory(self.get_tmp_dir(), create_parents = True)

    conf_tmp_dir = tempfile.mkdtemp(dir=self.get_tmp_dir())
    output_filename = os.path.join(self.get_tmp_dir(), config['commandParams']['output_file'])

    try:
      for file_dict in xml_configs_list:
        for filename, dict in file_dict.iteritems():
          XmlConfig(filename,
                    conf_dir=conf_tmp_dir,
                    mode=0644,
                    **self.generate_configs_get_xml_file_content(filename, dict)
          )
      for file_dict in env_configs_list:
        for filename,dicts in file_dict.iteritems():
          File(os.path.join(conf_tmp_dir, filename),
               mode=0644,
               content=InlineTemplate(self.generate_configs_get_template_file_content(filename, dicts)))

      for file_dict in properties_configs_list:
        for filename, dict in file_dict.iteritems():
          PropertiesFile(os.path.join(conf_tmp_dir, filename),
                         mode=0644,
                         properties=self.generate_configs_get_xml_file_dict(filename, dict)
          )
      with closing(tarfile.open(output_filename, "w:gz")) as tar:
        try:
          tar.add(conf_tmp_dir, arcname=os.path.basename("."))
        finally:
          tar.close()

    finally:
      Directory(conf_tmp_dir, action="delete")
