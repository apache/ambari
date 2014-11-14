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
import getpass

import os
import pwd
import shlex
import subprocess

from logging_utils import *


NR_CHMOD_CMD = 'chmod {0} {1} {2}'
NR_CHOWN_CMD = 'chown {0} {1} {2}'

ULIMIT_CMD = "ulimit -n"


def run_os_command(cmd):
  print_info_msg('about to run command: ' + str(cmd))
  if type(cmd) == str:
    cmd = shlex.split(cmd)
  process = subprocess.Popen(cmd,
                             stdout=subprocess.PIPE,
                             stdin=subprocess.PIPE,
                             stderr=subprocess.PIPE
                             )
  (stdoutdata, stderrdata) = process.communicate()
  return process.returncode, stdoutdata, stderrdata

def os_change_owner(filePath, user):
  uid = pwd.getpwnam(user).pw_uid
  gid = pwd.getpwnam(user).pw_gid
  os.chown(filePath, uid, gid)

def os_is_root():
  '''
  Checks effective UUID
  Returns True if a program is running under root-level privileges.
  '''
  return os.geteuid() == 0

def os_set_file_permissions(file, mod, recursive, user):
  WARN_MSG = "Command {0} returned exit code {1} with message: {2}"
  if recursive:
    params = " -R "
  else:
    params = ""
  command = NR_CHMOD_CMD.format(params, mod, file)
  retcode, out, err = run_os_command(command)
  if retcode != 0:
    print_warning_msg(WARN_MSG.format(command, file, err))
  command = NR_CHOWN_CMD.format(params, user, file)
  retcode, out, err = run_os_command(command)
  if retcode != 0:
    print_warning_msg(WARN_MSG.format(command, file, err))

def os_set_open_files_limit(maxOpenFiles):
  command = "%s %s" % (ULIMIT_CMD, str(maxOpenFiles))
  run_os_command(command)


def os_getpass(prompt):
  return getpass.unix_getpass(prompt)
