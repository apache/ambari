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

#OS constants
OS_UBUNTU = 'ubuntu'
OS_FEDORA = 'fedora'
OS_OPENSUSE = 'opensuse'
OS_SUSE = 'suse'
OS_SUSE_ENTERPRISE = 'sles'

#PostgreSQL settings
UBUNTU_PG_HBA_ROOT = "/etc/postgresql"
PG_HBA_ROOT_DEFAULT = "/var/lib/pgsql/data"
PG_STATUS_RUNNING_DEFAULT = "running"

#Environment
ENV_PATH_DEFAULT = ['/bin', '/usr/bin', '/sbin', '/usr/sbin']  # default search path
ENV_PATH = os.getenv('PATH', '').split(':') + ENV_PATH_DEFAULT


  # ToDo: move that function to common-functions
def locate_file(filename, default=''):
  """Locate command path according to OS environment"""
  for path in ENV_PATH:
    path = "%s/%s" % (path, filename)
    if os.path.isfile(path):
      return path
  if default != '':
    return "%s/%s" % (default, filename)
  else:
    return filename


def get_ubuntu_pg_version():
  """Return installed version of postgre server. In case of several
  installed versions will be returned a more new one.
  """
  postgre_ver = ""

  if os.path.isdir(UBUNTU_PG_HBA_ROOT):  # detect actual installed versions of PG and select a more new one
    postgre_ver = sorted(
    [fld for fld in os.listdir(UBUNTU_PG_HBA_ROOT) if os.path.isdir(os.path.join(UBUNTU_PG_HBA_ROOT, fld))], reverse=True)
    if len(postgre_ver) > 0:
      return postgre_ver[0]
  return postgre_ver


def get_postgre_hba_dir(OS):
  """Return postgre hba dir location depends on OS"""
  if OS == OS_UBUNTU:
    return "%s/%s/main" % (UBUNTU_PG_HBA_ROOT, get_ubuntu_pg_version())
  else:
    return PG_HBA_ROOT_DEFAULT


def get_postgre_running_status(OS):
  """Return postgre running status indicator"""
  if OS == OS_UBUNTU:
    return "%s/main" % get_ubuntu_pg_version()
  else:
    return PG_STATUS_RUNNING_DEFAULT
