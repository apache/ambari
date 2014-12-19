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

Ambari Agent

"""

__all__ = ["get_unique_id_and_date"]
import datetime
from resource_management.core import shell
from ambari_commons import os_check, OSConst
from ambari_commons.os_family_impl import OsFamilyImpl, OsFamilyFuncImpl


@OsFamilyFuncImpl(os_family=OSConst.WINSRV_FAMILY)
def get_volume_serial_number():
  from ambari_commons.os_windows import os_run_os_command

  id = ""
  code, out, err = os_run_os_command("cmd /c vol C:")
  for line in out.splitlines():
    if line.startswith(" Volume Serial Number is"):
      id = line[25:]

  return id

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_volume_serial_number():
  out = shell.checked_call("hostid")[1].split('\n')[-1] # bugfix: take the lastline (stdin is not tty part cut)
  id = out.strip()
  return id

def get_unique_id_and_date():
  id = get_volume_serial_number()

  now = datetime.datetime.now()
  date = now.strftime("%M%d%y")

  return "id{id}_date{date}".format(id=id, date=date)
