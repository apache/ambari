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
import signal
import sys
import time
import glob
import subprocess
from ambari_commons import OSConst

# PostgreSQL settings
PG_STATUS_RUNNING_DEFAULT = "running"
PG_HBA_ROOT_DEFAULT = "/var/lib/pgsql/data"
PG_HBA_INIT_FILES = {'ubuntu': '/etc/postgresql',
                     'redhat': '/etc/rc.d/init.d/postgresql',
                     'suse': '/etc/init.d/postgresql'}

#Environment
ENV_PATH_DEFAULT = ['/bin', '/usr/bin', '/sbin', '/usr/sbin']  # default search path
ENV_PATH = os.getenv('PATH', '').split(':') + ENV_PATH_DEFAULT

#Process
PROC_DIR = '/proc'
PROC_CMDLINE = 'cmdline'
PROC_EXEC = 'exe'


  # ToDo: move that function to common-functions
def locate_file(filename, default=''):
  """Locate command path according to OS environment"""
  for path in ENV_PATH:
    path = os.path.join(path, filename)
    if os.path.isfile(path):
      return path
  if default != '':
    return os.path.join(default, filename)
  else:
    return filename


def check_exitcode(exitcode_file_path):
  """
    Return exitcode of application, which is stored in the exitcode_file_path
  """
  exitcode = -1
  if os.path.isfile(exitcode_file_path):
    try:
      f = open(exitcode_file_path, "rb")
      exitcode = int(f.read())
      f.close()
      os.remove(exitcode_file_path)
    except IOError:
      pass
  return exitcode


def save_main_pid_ex(pids, pidfile, exclude_list=[], kill_exclude_list=False):
  """
    Save pid which is not included to exclude_list to pidfile.
    If kill_exclude_list is set to true,  all processes in that
    list would be killed. It's might be useful to daemonize child process

    exclude_list contains list of full executable paths which should be excluded
  """
  try:
    pfile = open(pidfile, "w")
    for item in pids:
      if pid_exists(item["pid"]) and (item["exe"] not in exclude_list):
        pfile.write("%s\n" % item["pid"])
      if pid_exists(item["pid"]) and (item["exe"] in exclude_list):
        try:
          os.kill(int(item["pid"]), signal.SIGKILL)
        except:
          pass
  except IOError:
    pass
  finally:
    try:
      pfile.close()
    except:
      pass


def wait_for_pid(pids, timeout):
  """
    Check pid for existence during timeout
  """
  tstart = time.time()
  pid_live = 0
  while int(time.time()-tstart) <= timeout and len(pids) > 0:
    sys.stdout.write('.')
    sys.stdout.flush()
    pid_live = 0
    for item in pids:
      if pid_exists(item["pid"]):
        pid_live += 1
    time.sleep(1)
  return pid_live


def get_symlink_path(path_to_link):
  """
    Expand symlink to real file path
  """
  return os.path.normpath(os.path.join(
    os.path.dirname(path_to_link),
    os.readlink(path_to_link)
  ))


def looking_for_pid(pattern, wait_time=1):
  """
    Searching for pid according to given pattern of command line
    during wait_time.
    Wait time is required to give a time to process to be executed.

    Return list of PID Items, which match the pattern.
  """
  tstart = time.time()
  found_pids = []

  while int(time.time()-tstart) <= wait_time:
    sys.stdout.write('.')
    sys.stdout.flush()
    pids = [pid for pid in os.listdir(PROC_DIR) if pid.isdigit()]
    found_pids = []  # clear list
    for pid in pids:
      try:
        arg = open(os.path.join(PROC_DIR, pid, PROC_CMDLINE), 'rb').read()
        if pattern in arg:
          found_pids += [{
            "pid": pid,
            "exe": get_symlink_path(os.path.join(PROC_DIR, pid, PROC_EXEC)),
            "cmd": arg.replace('\x00', ' ').strip()
          }]
      except:
        pass
    if wait_time == 1:  # to support unit test
      break
    time.sleep(1)
  return found_pids


def pid_exists(pid):
  """
   Check if pid is exist
  """
  return os.path.exists(os.path.join(PROC_DIR, pid))


def get_ubuntu_pg_version():
  """Return installed version of postgre server. In case of several
  installed versions will be returned a more new one.
  """
  postgre_ver = ""

  if os.path.isdir(PG_HBA_INIT_FILES[
    'ubuntu']):  # detect actual installed versions of PG and select a more new one
    postgre_ver = sorted(
      [fld for fld in os.listdir(PG_HBA_INIT_FILES[OSConst.UBUNTU_FAMILY]) if
       os.path.isdir(os.path.join(PG_HBA_INIT_FILES[OSConst.UBUNTU_FAMILY], fld))],
      reverse=True)
    if len(postgre_ver) > 0:
      return postgre_ver[0]
  return postgre_ver


def get_postgre_hba_dir(OS_FAMILY):
  """Return postgre hba dir location depends on OS.
  Also depends on version of postgres creates symlink like postgresql-->postgresql-9.3
  1) /etc/rc.d/init.d/postgresql --> /etc/rc.d/init.d/postgresql-9.3
  2) /etc/init.d/postgresql --> /etc/init.d/postgresql-9.1
  """
  if OS_FAMILY == OSConst.UBUNTU_FAMILY:
    # Like: /etc/postgresql/9.1/main/
    return os.path.join(PG_HBA_INIT_FILES[OS_FAMILY], get_ubuntu_pg_version(),
                        "main")
  else:
    if not os.path.isfile(PG_HBA_INIT_FILES[OS_FAMILY]):
      # Link: /etc/init.d/postgresql --> /etc/init.d/postgresql-9.1
      os.symlink(glob.glob(PG_HBA_INIT_FILES[OS_FAMILY] + '*')[0],
                 PG_HBA_INIT_FILES[OS_FAMILY])

    # Get postgres_data location (default: /var/lib/pgsql/data)
    cmd = "alias exit=return; source " + PG_HBA_INIT_FILES[
      OS_FAMILY] + " status &>/dev/null; echo $PGDATA"
    p = subprocess.Popen(cmd,
                         stdout=subprocess.PIPE,
                         stdin=subprocess.PIPE,
                         stderr=subprocess.PIPE,
                         shell=True)
    (PG_HBA_ROOT, err) = p.communicate()

    if PG_HBA_ROOT and len(PG_HBA_ROOT.strip()) > 0:
      return PG_HBA_ROOT.strip()
    else:
      return PG_HBA_ROOT_DEFAULT


def get_postgre_running_status(OS_FAMILY):
  """Return postgre running status indicator"""
  if OS_FAMILY == OSConst.UBUNTU_FAMILY:
    return os.path.join(get_ubuntu_pg_version(), "main")
  else:
    return PG_STATUS_RUNNING_DEFAULT
