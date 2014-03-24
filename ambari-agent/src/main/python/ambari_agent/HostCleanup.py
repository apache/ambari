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

import os
import string
import subprocess
import logging
import shutil
import platform
import ConfigParser
import optparse
import shlex
import sys
import datetime
import AmbariConfig
from pwd import getpwnam
from common_functions import OSCheck

logger = logging.getLogger()
configFile = "/etc/ambari-agent/conf/ambari-agent.ini"

PACKAGE_ERASE_CMD_RHEL = "yum erase -y {0}"
PACKAGE_ERASE_CMD_SUSE = "zypper -n -q remove {0}"
USER_ERASE_CMD = "userdel -rf {0}"
GROUP_ERASE_CMD = "groupdel {0}"
PROC_KILL_CMD = "kill -9 {0}"
ALT_DISP_CMD = "alternatives --display {0}"
ALT_ERASE_CMD = "alternatives --remove {0} {1}"

REPO_PATH_RHEL = "/etc/yum.repos.d"
REPO_PATH_SUSE = "/etc/zypp/repos.d/"
SKIP_LIST = []
HOST_CHECK_FILE_NAME = "hostcheck.result"
OUTPUT_FILE_NAME = "hostcleanup.result"

PACKAGE_SECTION = "packages"
PACKAGE_KEY = "pkg_list"
USER_SECTION = "users"
USER_KEY = "usr_list"
USER_HOMEDIR_KEY = "usr_homedir_list"
USER_HOMEDIR_SECTION = "usr_homedir"
REPO_SECTION = "repositories"
REPOS_KEY = "repo_list"
DIR_SECTION = "directories"
ADDITIONAL_DIRS = "additional_directories"
DIR_KEY = "dir_list"
PROCESS_SECTION = "processes"
PROCESS_KEY = "proc_list"
ALT_SECTION = "alternatives"
ALT_KEYS = ["symlink_list", "target_list"]
HADOOP_GROUP = "hadoop"
FOLDER_LIST = ["/tmp"]
# Additional path patterns to find existing directory
DIRNAME_PATTERNS = [
    "/tmp/hadoop-", "/tmp/hsperfdata_"
]

# resources that should not be cleaned
REPOSITORY_BLACK_LIST = ["ambari.repo"]
PACKAGES_BLACK_LIST = ["ambari-server", "ambari-agent"]


class HostCleanup:
  def resolve_ambari_config(self):
    try:
      config = AmbariConfig.config
      if os.path.exists(configFile):
        config.read(configFile)
        AmbariConfig.setConfig(config)
      else:
        raise Exception("No config found, use default")

    except Exception, err:
      logger.warn(err)
    return config

  def get_additional_dirs(self):
    resultList = []
    dirList = set()
    for patern in DIRNAME_PATTERNS:
      dirList.add(os.path.dirname(patern))

    for folder in dirList:  
      for dirs in os.walk(folder):
        for dir in dirs:
          for patern in DIRNAME_PATTERNS:
            if patern in dir:
             resultList.append(dir)
    return resultList         

  def do_cleanup(self, argMap=None):
    if argMap:
      packageList = argMap.get(PACKAGE_SECTION)
      userList = argMap.get(USER_SECTION)
      homeDirList = argMap.get(USER_HOMEDIR_SECTION)
      dirList = argMap.get(DIR_SECTION)
      repoList = argMap.get(REPO_SECTION)
      procList = argMap.get(PROCESS_SECTION)
      alt_map = argMap.get(ALT_SECTION)
      additionalDirList = self.get_additional_dirs()

      if userList and not USER_SECTION in SKIP_LIST:
        userIds = self.get_user_ids(userList)
      if procList and not PROCESS_SECTION in SKIP_LIST:
        logger.info("\n" + "Killing pid's: " + str(procList) + "\n")
        self.do_kill_processes(procList)
      if packageList and not PACKAGE_SECTION in SKIP_LIST:
        logger.info("Deleting packages: " + str(packageList) + "\n")
        self.do_erase_packages(packageList)
      if userList and not USER_SECTION in SKIP_LIST:
        logger.info("\n" + "Deleting users: " + str(userList))
        self.do_delete_users(userList)
        self.do_erase_dir_silent(homeDirList)
        self.do_delete_by_owner(userIds, FOLDER_LIST)
      if dirList and not DIR_SECTION in SKIP_LIST:
        logger.info("\n" + "Deleting directories: " + str(dirList))
        self.do_erase_dir_silent(dirList)
      if additionalDirList and not ADDITIONAL_DIRS in SKIP_LIST:
        logger.info("\n" + "Deleting additional directories: " + str(dirList))
        self.do_erase_dir_silent(additionalDirList)        
      if repoList and not REPO_SECTION in SKIP_LIST:
        repoFiles = self.find_repo_files_for_repos(repoList)
        logger.info("\n" + "Deleting repo files: " + str(repoFiles))
        self.do_erase_files_silent(repoFiles)
      if alt_map and not ALT_SECTION in SKIP_LIST:
        logger.info("\n" + "Erasing alternatives:" + str(alt_map) + "\n")
        self.do_erase_alternatives(alt_map)

    return 0

  def read_host_check_file(self, config_file_path):
    propertyMap = {}
    try:
      with open(config_file_path, 'r'):
        pass
    except Exception, e:
      logger.error("Host check result not found at: " + str(config_file_path))
      return None

    try:
      config = ConfigParser.RawConfigParser()
      config.read(config_file_path)
    except Exception, e:
      logger.error("Cannot read host check result: " + str(e))
      return None

    # Initialize map from file
    try:
      if config.has_option(PACKAGE_SECTION, PACKAGE_KEY):
        propertyMap[PACKAGE_SECTION] = config.get(PACKAGE_SECTION, PACKAGE_KEY).split(',')
    except:
      logger.warn("Cannot read package list: " + str(sys.exc_info()[0]))

    try:
      if config.has_option(USER_SECTION, USER_KEY):
        propertyMap[USER_SECTION] = config.get(USER_SECTION, USER_KEY).split(',')
    except:
      logger.warn("Cannot read user list: " + str(sys.exc_info()[0]))

    try:
      if config.has_option(USER_SECTION, USER_HOMEDIR_KEY):
        propertyMap[USER_HOMEDIR_SECTION] = config.get(USER_SECTION, USER_HOMEDIR_KEY).split(',')
    except:
      logger.warn("Cannot read user homedir list: " + str(sys.exc_info()[0]))

    try:
      if config.has_option(REPO_SECTION, REPOS_KEY):
        propertyMap[REPO_SECTION] = config.get(REPO_SECTION, REPOS_KEY).split(',')
    except:
      logger.warn("Cannot read repositories list: " + str(sys.exc_info()[0]))

    try:
      if config.has_option(DIR_SECTION, DIR_KEY):
        propertyMap[DIR_SECTION] = config.get(DIR_SECTION, DIR_KEY).split(',')
    except:
      logger.warn("Cannot read dir list: " + str(sys.exc_info()[0]))

    process_items = []
    try:
      pids = [pid for pid in os.listdir('/proc') if pid.isdigit()]
      for pid in pids:
        cmd = open(os.path.join('/proc', pid, 'cmdline'), 'rb').read()
        cmd = cmd.replace('\0', ' ')
        if not 'AmbariServer' in cmd and not 'HostCleanup' in cmd:
          if 'java' in cmd and JAVA_HOME in cmd:
            process_items.append(int(pid))
    except:
      pass
    propertyMap[PROCESS_SECTION] = process_items

    try:
      alt_map = {}
      if config.has_option(ALT_SECTION, ALT_KEYS[0]):
        alt_map[ALT_KEYS[0]] = config.get(ALT_SECTION, ALT_KEYS[0]).split(',')
      if config.has_option(ALT_SECTION, ALT_KEYS[1]):
        alt_map[ALT_KEYS[1]] = config.get(ALT_SECTION, ALT_KEYS[1]).split(',')
      if alt_map:
        propertyMap[ALT_SECTION] = alt_map
    except:
      logger.warn("Cannot read alternates list: " + str(sys.exc_info()[0]))

    return propertyMap

  def get_alternatives_desc(self, alt_name):
    command = ALT_DISP_CMD.format(alt_name)
    out = None
    try:
      p1 = subprocess.Popen(shlex.split(command), stdout=subprocess.PIPE)
      p2 = subprocess.Popen(["grep", "priority"], stdin=p1.stdout, stdout=subprocess.PIPE)
      p1.stdout.close()
      out = p2.communicate()[0]
      logger.debug('alternatives --display ' + alt_name + '\n, out = ' + out)
    except:
      logger.warn('Cannot process alternative named: ' + alt_name + ',' + \
                  'error: ' + str(sys.exc_info()[0]))

    return out

  # Alternatives exist as a stack of symlinks under /var/lib/alternatives/$name
  # Script expects names of the alternatives as input
  # We find all the symlinks using command, #] alternatives --display $name
  # and delete them using command, #] alternatives --remove $name $path.
  def do_erase_alternatives(self, alt_map):
    if alt_map:
      alt_list = alt_map.get(ALT_KEYS[0])
      if alt_list:
        for alt_name in alt_list:
          if alt_name:
            out = self.get_alternatives_desc(alt_name)

            if not out:
              logger.warn('No alternatives found for: ' + alt_name)
              continue
            else:
              alternates = out.split('\n')
              if alternates:
                for entry in alternates:
                  if entry:
                    alt_path = entry.split()[0]
                    logger.debug('Erasing alternative named: ' + alt_name + ', ' \
                                                                            'path: ' + alt_path)

                    command = ALT_ERASE_CMD.format(alt_name, alt_path)
                    (returncode, stdoutdata, stderrdata) = self.run_os_command(command)
                    if returncode != 0:
                      logger.warn('Failed to remove alternative: ' + alt_name +
                                  ", path: " + alt_path + ", error: " + stderrdata)

      # Remove directories - configs
      dir_list = alt_map.get(ALT_KEYS[1])
      if dir_list:
        self.do_erase_dir_silent(dir_list)

    return 0

  def do_kill_processes(self, pidList):
    if pidList:
      for pid in pidList:
        if pid:
          command = PROC_KILL_CMD.format(pid)
          (returncode, stdoutdata, stderrdata) = self.run_os_command(command)
          if returncode != 0:
            logger.error("Unable to kill process with pid: " + pid + ", " + stderrdata)
    return 0

  def get_files_in_dir(self, dirPath):
    fileList = []
    if dirPath:
      if os.path.exists(dirPath):
        listdir = os.listdir(dirPath)
        if listdir:
          for link in listdir:
            path = dirPath + os.sep + link
            if not os.path.islink(path) and not os.path.isdir(path):
              fileList.append(path)

    return fileList

  def find_repo_files_for_repos(self, repoNames):
    repoFiles = []
    osType = self.get_os_type()
    repoNameList = []
    for repoName in repoNames:
      if len(repoName.strip()) > 0:
        repoNameList.append("[" + repoName + "]")
        repoNameList.append("name=" + repoName)
    if repoNameList:
      # get list of files
      if osType == 'suse':
        fileList = self.get_files_in_dir(REPO_PATH_SUSE)
      elif osType == "redhat":
        fileList = self.get_files_in_dir(REPO_PATH_RHEL)
      else:
        logger.warn("Unsupported OS type, cannot get repository location.")
        return []

      if fileList:
        for filePath in fileList:
          with open(filePath, 'r') as file:
            content = file.readline()
            while (content != "" ):
              for repoName in repoNameList:
                if content.find(repoName) == 0 and filePath not in repoFiles:
                  repoFiles.append(filePath)
                  break;
              content = file.readline()

    return repoFiles

  def do_erase_packages(self, packageList):
    packageStr = None
    if packageList:
      packageStr = ' '.join(packageList)
      logger.debug("Erasing packages: " + packageStr)
    if packageStr is not None and packageStr:
      os_name = self.get_os_type()
      command = ''
      if os_name == 'suse':
        command = PACKAGE_ERASE_CMD_SUSE.format(packageStr)
      elif os_name == 'redhat':
        command = PACKAGE_ERASE_CMD_RHEL.format(packageStr)
      else:
        logger.warn("Unsupported OS type, cannot remove package.")

      if command != '':
        logger.debug('Executing: ' + str(command))
        (returncode, stdoutdata, stderrdata) = self.run_os_command(command)
        if returncode != 0:
          logger.warn("Erasing packages failed: " + stderrdata)
        else:
          logger.info("Erased packages successfully.\n" + stdoutdata)
    return 0

  def do_erase_dir_silent(self, pathList):
    if pathList:
      for path in pathList:
        if path and os.path.exists(path):
          if os.path.isdir(path):
            try:
              shutil.rmtree(path)
            except:
              logger.warn("Failed to remove dir: " + path + ", error: " + str(sys.exc_info()[0]))
          else:
            logger.info(path + " is a file and not a directory, deleting file")
            self.do_erase_files_silent([path])
        else:
          logger.info("Path doesn't exists: " + path)
    return 0

  def do_erase_files_silent(self, pathList):
    if pathList:
      for path in pathList:
        if path and os.path.exists(path):
          try:
            os.remove(path)
          except:
            logger.warn("Failed to delete file: " + path + ", error: " + str(sys.exc_info()[0]))
        else:
          logger.info("File doesn't exists: " + path)
    return 0

  def do_delete_group(self):
    groupDelCommand = GROUP_ERASE_CMD.format(HADOOP_GROUP)
    (returncode, stdoutdata, stderrdata) = self.run_os_command(groupDelCommand)
    if returncode != 0:
      logger.warn("Cannot delete group : " + HADOOP_GROUP + ", " + stderrdata)
    else:
      logger.info("Successfully deleted group: " + HADOOP_GROUP)

  def do_delete_by_owner(self, userIds, folders):
    for folder in folders:
      for filename in os.listdir(folder):
        fileToCheck = os.path.join(folder, filename)
        stat = os.stat(fileToCheck)
        if stat.st_uid in userIds:
          self.do_erase_dir_silent([fileToCheck])
          logger.info("Deleting file/folder: " + fileToCheck)

  def get_user_ids(self, userList):
    userIds = []
    if userList:
      for user in userList:
        if user:
          try:
            userIds.append(getpwnam(user).pw_uid)
          except Exception:
            logger.warn("Cannot find user : " + user)
    return userIds

  def do_delete_users(self, userList):
    if userList:
      for user in userList:
        if user:
          command = USER_ERASE_CMD.format(user)
          (returncode, stdoutdata, stderrdata) = self.run_os_command(command)
          if returncode != 0:
            logger.warn("Cannot delete user : " + user + ", " + stderrdata)
          else:
            logger.info("Successfully deleted user: " + user)
      self.do_delete_group()
    return 0

  def is_current_user_root(self):
    return os.getuid() == 0

  def get_os_type(self):
    return OSCheck().get_os_family()


  # Run command as sudoer by default, if root no issues
  def run_os_command(self, cmd, runWithSudo=True):
    if runWithSudo:
      cmd = 'sudo ' + cmd
    logger.info('Executing command: ' + str(cmd))
    if type(cmd) == str:
      cmd = shlex.split(cmd)
    process = subprocess.Popen(cmd,
                               stdout=subprocess.PIPE,
                               stdin=subprocess.PIPE,
                               stderr=subprocess.PIPE
    )
    (stdoutdata, stderrdata) = process.communicate()
    return process.returncode, stdoutdata, stderrdata


  def search_file(self, filename, search_path, pathsep=os.pathsep):
    """ Given a search path, find file with requested name """
    for path in string.split(search_path, pathsep):
      candidate = os.path.join(path, filename)
      if os.path.exists(candidate): return os.path.abspath(candidate)
    return None

# Copy file and save with file.# (timestamp)
def backup_file(filePath):
  if filePath is not None and os.path.exists(filePath):
    timestamp = datetime.datetime.now()
    format = '%Y%m%d%H%M%S'
    try:
      shutil.copyfile(filePath, filePath + "." + timestamp.strftime(format))
    except (Exception), e:
      logger.warn('Could not backup file "%s": %s' % (str(filePath, e)))
  return 0


def get_YN_input(prompt, default):
  yes = set(['yes', 'ye', 'y'])
  no = set(['no', 'n'])
  return get_choice_string_input(prompt, default, yes, no)


def get_choice_string_input(prompt, default, firstChoice, secondChoice):
  choice = raw_input(prompt).lower()
  if choice in firstChoice:
    return True
  elif choice in secondChoice:
    return False
  elif choice is "": # Just enter pressed
    return default
  else:
    print "input not recognized, please try again: "
    return get_choice_string_input(prompt, default, firstChoice, secondChoice)
  pass


def main():
  h = HostCleanup()
  config = h.resolve_ambari_config()
  hostCheckFileDir = config.get('agent', 'prefix')
  hostCheckFilePath = os.path.join(hostCheckFileDir, HOST_CHECK_FILE_NAME)
  hostCheckResultPath = os.path.join(hostCheckFileDir, OUTPUT_FILE_NAME)

  parser = optparse.OptionParser()
  parser.add_option("-v", "--verbose", dest="verbose", action="store_false",
                    default=False, help="output verbosity.")
  parser.add_option("-f", "--file", dest="inputfile",
                    default=hostCheckFilePath,
                    help="host check result file to read.", metavar="FILE")
  parser.add_option("-o", "--out", dest="outputfile",
                    default=hostCheckResultPath,
                    help="log file to store results.", metavar="FILE")
  parser.add_option("-k", "--skip", dest="skip",
                    help="(packages|users|directories|repositories|processes|alternatives)." + \
                         " Use , as separator.")
  parser.add_option("-s", "--silent",
                    action="store_true", dest="silent", default=False,
                    help="Silently accepts default prompt values")
  parser.add_option('-j', '--java-home', default="/usr/jdk64/jdk1.6.0_31", dest="java_home",
                    help="Use specified java_home.")


  (options, args) = parser.parse_args()
  # set output file
  backup_file(options.outputfile)
  global logger
  logger = logging.getLogger('HostCleanup')
  handler = logging.FileHandler(options.outputfile)
  formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
  handler.setFormatter(formatter)
  logger.addHandler(handler)

  # set java_home
  global JAVA_HOME
  JAVA_HOME = options.java_home

  # set verbose
  if options.verbose:
    logging.basicConfig(level=logging.DEBUG)
  else:
    logging.basicConfig(level=logging.INFO)

  if options.skip is not None:
    global SKIP_LIST
    SKIP_LIST = options.skip.split(',')

  is_root = h.is_current_user_root()
  if not is_root:
    raise RuntimeError('HostCleanup needs to be run as root.')

  if not options.silent:
    if "users" not in SKIP_LIST:
      delete_users = get_YN_input('You have elected to remove all users as well. If it is not intended then use '
                               'option --skip \"users\". Do you want to continue [y/n] (y)', True)
      if not delete_users:
        print 'Exiting. Use option --skip="users" to skip deleting users'
        sys.exit(1)

  hostcheckfile = options.inputfile
  propMap = h.read_host_check_file(hostcheckfile)

  if propMap:
    h.do_cleanup(propMap)

  logger.info('Clean-up completed. The output is at %s' % (str(options.outputfile)))


if __name__ == '__main__':
  main()
