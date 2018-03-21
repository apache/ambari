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
import re
import signal
import socket
import sys
import time
import glob
from ambari_commons import subprocess32
import logging
import platform
from ambari_commons import OSConst,OSCheck
from ambari_commons.logging_utils import print_error_msg
from ambari_commons.exceptions import FatalException

logger = logging.getLogger(__name__)

# PostgreSQL settings
PG_STATUS_RUNNING_DEFAULT = "running"
PG_HBA_ROOT_DEFAULT = "/var/lib/pgsql/data"

#Environment
ENV_PATH_DEFAULT = ['/bin', '/usr/bin', '/sbin', '/usr/sbin']  # default search path
ENV_PATH = os.getenv('PATH', '').split(':') + ENV_PATH_DEFAULT

#Process
PROC_DIR = '/proc'
PROC_CMDLINE = 'cmdline'
PROC_EXEC = 'exe'

def get_pg_hba_init_files():
  if OSCheck.is_ubuntu_family():
    return '/etc/postgresql'
  elif OSCheck.is_redhat_family():
    return '/etc/rc.d/init.d/postgresql'
  elif OSCheck.is_suse_family():
    return '/etc/init.d/postgresql'
  else:
    raise Exception("Unsupported OS family '{0}'".format(OSCheck.get_os_family()))


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

def locate_all_file_paths(filename, default=''):
  """Locate command possible paths according to OS environment"""
  paths = []
  for path in ENV_PATH:
    path = os.path.join(path, filename)
    if os.path.isfile(path):
      paths.append(path)

  if not paths:
    if default != '':
      return [os.path.join(default, filename)]
    else:
      return [filename]

  return paths


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


def save_pid(pid, pidfile):
  """
    Save pid to pidfile.
  """
  try:
    pfile = open(pidfile, "w")
    pfile.write("%s\n" % pid)
  except IOError as e:
    logger.error("Failed to write PID to " + pidfile + " due to " + str(e))
    pass
  finally:
    try:
      pfile.close()
    except Exception as e:
      logger.error("Failed to close PID file " + pidfile + " due to " + str(e))
      pass


def save_main_pid_ex(pids, pidfile, exclude_list=[], skip_daemonize=False):
  """
    Saves and returns the first (and supposingly only) pid from the list of pids
    which is not included in the exclude_list.

    pidfile is the name of the file to save the pid to

    exclude_list contains list of full executable paths which should be excluded
  """
  pid_saved = False
  try:
    if pids:
      pfile = open(pidfile, "w")
      for item in pids:
        if pid_exists(item["pid"]) and (item["exe"] not in exclude_list):
          pfile.write("%s\n" % item["pid"])
          pid_saved = item["pid"]
          logger.info("Ambari server started with PID " + str(item["pid"]))
        if pid_exists(item["pid"]) and (item["exe"] in exclude_list) and not skip_daemonize:
          try:
            os.kill(int(item["pid"]), signal.SIGKILL)
          except:
            pass
  except IOError as e:
    logger.error("Failed to write PID to " + pidfile + " due to " + str(e))
    pass
  finally:
    try:
      pfile.close()
    except Exception as e:
      logger.error("Failed to close PID file " + pidfile + " due to " + str(e))
      pass
  return pid_saved

def get_live_pids_count(pids):
  """
    Check pids for existence
  """
  return len([pid for pid in pids if pid_exists(pid)])

def wait_for_ui_start(ambari_server_ui_port, pid, timeout=1):

  tstart = time.time()
  while int(time.time()-tstart) <= timeout:
    try:
      sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
      sock.settimeout(1)
      sock.connect(('localhost', ambari_server_ui_port))
      print "\nServer started listening on " + str(ambari_server_ui_port)
      return True
    except Exception as e:
      #print str(e)
      pass

    sys.stdout.write('.')
    sys.stdout.flush()
    if pid_exists(pid):
      time.sleep(1)
    else:
      break

  return False

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

  if os.path.isdir(get_pg_hba_init_files()):  # detect actual installed versions of PG and select a more new one
    postgre_ver = sorted(
      [fld for fld in os.listdir(get_pg_hba_init_files()) if
       os.path.isdir(os.path.join(get_pg_hba_init_files(), fld))],
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
  if OSCheck.is_ubuntu_family():
    # Like: /etc/postgresql/9.1/main/
    return os.path.join(get_pg_hba_init_files(), get_ubuntu_pg_version(),
                        "main")
  elif not glob.glob(get_pg_hba_init_files() + '*'): # this happens when the service file is of new format (/usr/lib/systemd/system/postgresql.service)
    return PG_HBA_ROOT_DEFAULT
  else:
    if not os.path.isfile(get_pg_hba_init_files()):
      # Link: /etc/init.d/postgresql --> /etc/init.d/postgresql-9.1
      os.symlink(glob.glob(get_pg_hba_init_files() + '*')[0],
                 get_pg_hba_init_files())

    pg_hba_init_basename = os.path.basename(get_pg_hba_init_files())
    # Get postgres_data location (default: /var/lib/pgsql/data)
    cmd = "alias basename='echo {0}; true' ; alias exit=return; source {1} status &>/dev/null; echo $PGDATA".format(pg_hba_init_basename, get_pg_hba_init_files())
    p = subprocess32.Popen(cmd,
                         stdout=subprocess32.PIPE,
                         stdin=subprocess32.PIPE,
                         stderr=subprocess32.PIPE,
                         shell=True)
    (PG_HBA_ROOT, err) = p.communicate()

    if PG_HBA_ROOT and len(PG_HBA_ROOT.strip()) > 0:
      return PG_HBA_ROOT.strip()
    else:
      return PG_HBA_ROOT_DEFAULT


def get_postgre_running_status():
  """Return postgre running status indicator"""
  if OSCheck.is_ubuntu_family():
    return os.path.join(get_ubuntu_pg_version(), "main")
  else:
    return PG_STATUS_RUNNING_DEFAULT


def compare_versions(version1, version2):
  """Compare two versions by digits. Ignore any alphanumeric characters after - and _ postfix.
  Return 1 if version1 is newer than version2
  Return -1 if version1 is older than version2
  Return 0 if two versions are the same
  """
  def normalize(v):
    v = str(v)
    v = re.sub(r'^\D+', '', v)
    v = re.sub(r'\D+$', '', v)
    v = v.strip(".-_")
    pos_under = v.find("_")
    pos_dash = v.find("-")
    if pos_under > 0 and pos_dash < 0:
      pos = pos_under
    elif pos_under < 0 and pos_dash > 0:
      pos = pos_dash
    else:
      pos = min(pos_under, pos_dash)
    if pos > 0:
      v = v[0:pos]
    return [int(x) for x in re.sub(r'(\.0+)*$', '', v).split(".")]
  return cmp(normalize(version1), normalize(version2))
  pass


def check_reverse_lookup():
  """
  Check if host fqdn resolves to current host ip
  """
  try:
    host_name = socket.gethostname().lower()
    host_ip = socket.gethostbyname(host_name)
    host_fqdn = socket.getfqdn().lower()
    fqdn_ip = socket.gethostbyname(host_fqdn)
    return host_ip == fqdn_ip
  except socket.error:
    pass
  return False

def on_powerpc():
  """ True if we are running on a Power PC platform."""
  return platform.processor() == 'powerpc' or \
         platform.machine().startswith('ppc')
