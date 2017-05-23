#!/usr/bin/env python

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

import os.path
import logging
import subprocess
from resource_management.core import shell
from resource_management.core.shell import call
from resource_management.core.exceptions import ExecuteTimeoutException, Fail
from ambari_commons.shell import shellRunner
from Facter import Facter
from ambari_commons.os_check import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from AmbariConfig import AmbariConfig
from resource_management.core.sudo import path_isfile

logger = logging.getLogger()


class Hardware:
  SSH_KEY_PATTERN = 'ssh.*key'
  WINDOWS_GET_DRIVES_CMD = "foreach ($drive in [System.IO.DriveInfo]::getdrives()){$available = $drive.TotalFreeSpace;$used = $drive.TotalSize-$drive.TotalFreeSpace;$percent = ($used*100)/$drive.TotalSize;$size = $drive.TotalSize;$type = $drive.DriveFormat;$mountpoint = $drive.RootDirectory.FullName;echo \"$available $used $percent% $size $type $mountpoint\"}"
  CHECK_REMOTE_MOUNTS_KEY = 'agent.check.remote.mounts'
  CHECK_REMOTE_MOUNTS_TIMEOUT_KEY = 'agent.check.mounts.timeout'
  CHECK_REMOTE_MOUNTS_TIMEOUT_DEFAULT = '10'
  IGNORE_ROOT_MOUNTS = ["proc", "dev", "sys"]
  IGNORE_DEVICES = ["proc", "tmpfs", "cgroup", "mqueue", "shm"]
  LINUX_PATH_SEP = "/"

  def __init__(self, config):
    logger.info("Initializing host system information.")
    self.hardware = {
      'mounts': Hardware.osdisks()
    }
    self.config = config
    self.hardware.update(Facter(self.config).facterInfo())
    logger.info("Host system information: %s", self.hardware)

  @classmethod
  def _parse_df_line(cls, line):
    """
      Initialize data-structure from string in specific 'df' command output format

      Expected string format:
       device fs_type disk_size used_size available_size capacity_used_percents mount_point

    :type line str
    """

    line_split = line.split()
    if len(line_split) != 7:
      return None

    titles = ["device", "type", "size", "used", "available", "percent", "mountpoint"]
    return dict(zip(titles, line_split))

  @classmethod
  def _get_mount_check_timeout(cls, config=None):
    """Return timeout for df call command"""
    if config and config.has_option(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY) \
      and config.get(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY) != "0":

      return config.get(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_KEY)

    return Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_DEFAULT

  @classmethod
  def _check_remote_mounts(cls, config=None):
    """Verify if remote mount allowed to be processed or not"""
    if config and config.has_option(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_KEY) and \
       config.get(AmbariConfig.AMBARI_PROPERTIES_CATEGORY, Hardware.CHECK_REMOTE_MOUNTS_KEY).lower() == "false":

      return False

    return True

  @classmethod
  def _is_mount_blacklisted(cls, blacklist, mount_point):
    """
    Verify if particular mount point is in the black list.

    :return True if mount_point or a part of mount point is in the blacklist, otherwise return False

     Example:
       Mounts: /, /mnt/my_mount, /mnt/my_mount/sub_mount
       Blacklist: /mnt/my_mount
       Result: /

    :type blacklist list
    :type mount_point str
    :rtype bool
    """

    if not blacklist or not mount_point:
      return False

    mount_point_elements = mount_point.split(cls.LINUX_PATH_SEP)

    for el in blacklist:
      el_list = el.split(cls.LINUX_PATH_SEP)
      # making patch elements comparision
      if el_list == mount_point_elements[:len(el_list)]:
        return True

    return False


  @classmethod
  @OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
  def osdisks(cls, config=None):
    """ Run df to find out the disks on the host. Only works on linux
    platforms. Note that this parser ignores any filesystems with spaces
    and any mounts with spaces. """
    timeout = cls._get_mount_check_timeout(config)
    command = ["timeout", timeout, "df", "-kPT"]
    blacklisted_mount_points = []

    if config:
      ignore_mount_value = config.get("agent", "ignore_mount_points", default="")
      blacklisted_mount_points = [item.strip() for item in ignore_mount_value.split(",")]

    if not cls._check_remote_mounts(config):
      command.append("-l")

    try:
      code, out, err = shell.call(command, stdout = subprocess.PIPE, stderr = subprocess.PIPE, timeout = int(timeout), quiet = True)
      dfdata = out
    except Exception as ex:
      logger.warn("Checking disk usage failed: " + str(ex))
      dfdata = ''

    mounts = [cls._parse_df_line(line) for line in dfdata.splitlines() if line]
    result_mounts = []
    ignored_mounts = []

    for mount in mounts:
      if not mount:
        continue

      """
      We need to filter mounts by several parameters:
       - mounted device is not in the ignored list
       - is accessible to user under which current process running
       - it is not file-mount (docker environment)
       - mount path or a part of mount path is not in the blacklist
      """
      if mount["device"] not in cls.IGNORE_DEVICES and\
         mount["mountpoint"].split("/")[0] not in cls.IGNORE_ROOT_MOUNTS and\
         cls._chk_writable_mount(mount['mountpoint']) and\
         not path_isfile(mount["mountpoint"]) and\
         not cls._is_mount_blacklisted(blacklisted_mount_points, mount["mountpoint"]):

        result_mounts.append(mount)
      else:
        ignored_mounts.append(mount)

    if len(ignored_mounts) > 0:
      ignore_list = [el["mountpoint"] for el in ignored_mounts]
      logger.info("Some mount points were ignored: {0}".format(', '.join(ignore_list)))

    return result_mounts

  @classmethod
  def _chk_writable_mount(cls, mount_point):
    if os.geteuid() == 0:
      return os.access(mount_point, os.W_OK)
    else:
      try:
        # test if mount point is writable for current user
        call_result = call(['test', '-w', mount_point],
                           sudo=True,
                           timeout=int(Hardware.CHECK_REMOTE_MOUNTS_TIMEOUT_DEFAULT) / 2,
                           quiet=not logger.isEnabledFor(logging.DEBUG))
        return call_result and call_result[0] == 0
      except ExecuteTimeoutException:
        logger.exception("Exception happened while checking mount {0}".format(mount_point))
        return False
      except Fail:
        logger.exception("Exception happened while checking mount {0}".format(mount_point))
        return False
    
  @classmethod
  @OsFamilyFuncImpl(OSConst.WINSRV_FAMILY)
  def osdisks(cls, config=None):
    mounts = []
    runner = shellRunner()
    command_result = runner.runPowershell(script_block=Hardware.WINDOWS_GET_DRIVES_CMD)
    if command_result.exitCode != 0:
      return mounts
    else:
      for drive in [line for line in command_result.output.split(os.linesep) if line != '']:
        available, used, percent, size, fs_type, mountpoint = drive.split(" ")
        mounts.append({"available": available,
                       "used": used,
                       "percent": percent,
                       "size": size,
                       "type": fs_type,
                       "mountpoint": mountpoint})

    return mounts

  def get(self):
    return self.hardware


def main():
  from resource_management.core.logger import Logger
  Logger.initialize_logger()

  config = None
  print Hardware(config).get()

if __name__ == '__main__':
  main()
