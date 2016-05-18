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

import time
import logging
import traceback
import json
import subprocess

from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from ambari_commons.os_check import OSConst
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.core import shell
from resource_management.core.resources import Execute
from resource_management.core import global_lock


OK_MESSAGE = "APP is in : '{0}' state. Check took {1:.3f}s"
MESSAGE_WITH_STATE_AND_INSTANCES = "APP is in : '{0}' state. Instances 'live' : {1}, 'desired' : {2}. Check took {3:.3f}s"
CRITICAL_MESSAGE_WITH_STATE = "APP is in : '{0}' state. Check took {1:.3f}s"
CRITICAL_MESSAGE = "APP information couldn't be retrieved. Check took {0:.3f}s"

# results codes
CRITICAL_RESULT_CODE = 'CRITICAL'
OK_RESULT_CODE = 'OK'
UKNOWN_STATUS_CODE = 'UNKNOWN'


SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'

HIVE_PRINCIPAL_KEY = '{{hive-interactive-site/hive.llap.zk.sm.principal}}'
HIVE_PRINCIPAL_DEFAULT = 'default.hive.principal'

HIVE_PRINCIPAL_KEYTAB_KEY = '{{hive-interactive-site/hive.llap.zk.sm.keytab.file}}'
HIVE_PRINCIPAL_KEYTAB_DEFAULT = 'default.hive.keytab'

HIVE_AUTHENTICATION_DEFAULT = 'NOSASL'

HIVE_USER_KEY = '{{hive-env/hive_user}}'
HIVE_USER_DEFAULT = 'default.smoke.user'

STACK_ROOT = '{{cluster-env/stack_root}}'
STACK_ROOT_DEFAULT = "/usr/hdp"

LLAP_APP_NAME_KEY = '{{hive-interactive-env/llap_app_name}}'
LLAP_APP_NAME_DEFAULT = 'llap0'

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'


CHECK_COMMAND_TIMEOUT_KEY = 'check.command.timeout'
CHECK_COMMAND_TIMEOUT_DEFAULT = 15.0



logger = logging.getLogger('ambari_alerts')

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (SECURITY_ENABLED_KEY, KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY, HIVE_PRINCIPAL_KEY, HIVE_PRINCIPAL_KEYTAB_KEY,
          HIVE_USER_KEY, STACK_ROOT, LLAP_APP_NAME_KEY)


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return ('UNKNOWN', ['There were no configurations supplied to the script.'])

  result_code = None

  try:
    security_enabled = False
    if SECURITY_ENABLED_KEY in configurations:
      security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

    check_command_timeout = CHECK_COMMAND_TIMEOUT_DEFAULT
    if CHECK_COMMAND_TIMEOUT_KEY in configurations:
      check_command_timeout = int(parameters[CHECK_COMMAND_TIMEOUT_KEY])

    hive_user = HIVE_USER_DEFAULT
    if HIVE_USER_KEY in configurations:
      hive_user = configurations[HIVE_USER_KEY]

    llap_app_name = LLAP_APP_NAME_DEFAULT
    if LLAP_APP_NAME_KEY in configurations:
      llap_app_name = configurations[LLAP_APP_NAME_KEY]

    if security_enabled:
      llap_principal = HIVE_PRINCIPAL_DEFAULT
      if HIVE_PRINCIPAL_KEY in configurations:
        llap_principal = configurations[HIVE_PRINCIPAL_KEY]

      llap_keytab = HIVE_PRINCIPAL_KEYTAB_DEFAULT
      if HIVE_PRINCIPAL_KEYTAB_KEY in configurations:
        llap_keytab = configurations[HIVE_PRINCIPAL_KEYTAB_KEY]

      # Get the configured Kerberos executable search paths, if any
      if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
        kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]
      else:
        kerberos_executable_search_paths = None

      kinit_path_local = get_kinit_path(kerberos_executable_search_paths)
      kinitcmd=format("{kinit_path_local} -kt {llap_keytab} {llap_principal}; ")

      # prevent concurrent kinit
      kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
      kinit_lock.acquire()
      try:
        Execute(kinitcmd, user=hive_user,#status_params.hive_user,
                path=["/bin/", "/usr/bin/", "/usr/lib/hive/bin/", "/usr/sbin/"],
                timeout=10)
      finally:
        kinit_lock.release()



    start_time = time.time()
    if STACK_ROOT in configurations:
      llap_status_cmd = configurations[STACK_ROOT] + format("/current/hive-server2-hive2/bin/hive --service llapstatus --name {llap_app_name}")
    else:
      llap_status_cmd = format("/usr/hdp/current/hive-server2-hive2/bin/hive --service llapstatus --name {llap_app_name}")

    code, output, error = shell.checked_call(llap_status_cmd, user=hive_user, stderr=subprocess.PIPE,
                                             timeout=check_command_timeout,
                                             logoutput=False)
    llap_app_info = json.loads(output)

    if llap_app_info is None or 'state' not in llap_app_info:
      alert_label = traceback.format_exc()
      result_code = UKNOWN_STATUS_CODE
      return (result_code, [alert_label])

    if llap_app_info['state'].upper() in ['RUNNING_ALL']:
      result_code = OK_RESULT_CODE
      total_time = time.time() - start_time
      alert_label = OK_MESSAGE.format(llap_app_info['state'], total_time)
    elif llap_app_info['state'].upper() in ['RUNNING_PARTIAL']:
      live_instances = 0
      desired_instances = 0
      percentInstancesUp = 0
      percent_desired_instances_to_be_up = 80
      # Get 'live' and 'desired' instances
      if 'liveInstances' not in llap_app_info or 'desiredInstances' not in llap_app_info:
        result_code = CRITICAL_RESULT_CODE
        total_time = time.time() - start_time
        alert_label = CRITICAL_MESSAGE_WITH_STATE.format(llap_app_info['state'], total_time)
        return (result_code, [alert_label])

      live_instances = llap_app_info['liveInstances']
      desired_instances = llap_app_info['desiredInstances']
      if live_instances < 0 or desired_instances <= 0:
        result_code = CRITICAL_RESULT_CODE
        total_time = time.time() - start_time
        alert_label = MESSAGE_WITH_STATE_AND_INSTANCES.format(llap_app_info['state'], total_time)
        return (result_code, [alert_label])

      percentInstancesUp = float(live_instances) / desired_instances * 100
      if percentInstancesUp >= percent_desired_instances_to_be_up:
        result_code = OK_RESULT_CODE
        total_time = time.time() - start_time
        alert_label = MESSAGE_WITH_STATE_AND_INSTANCES.format(llap_app_info['state'],
                                                              llap_app_info['liveInstances'],
                                                              llap_app_info['desiredInstances'],
                                                              total_time)
      else:
        result_code = CRITICAL_RESULT_CODE
        total_time = time.time() - start_time
        alert_label = MESSAGE_WITH_STATE_AND_INSTANCES.format(llap_app_info['state'],
                                                              llap_app_info['liveInstances'],
                                                              llap_app_info['desiredInstances'],
                                                              total_time)
    else:
      result_code = CRITICAL_RESULT_CODE
      total_time = time.time() - start_time
      alert_label = CRITICAL_MESSAGE_WITH_STATE.format(llap_app_info['state'], total_time)
  except:
    alert_label = traceback.format_exc()
    traceback.format_exc()
    result_code = UKNOWN_STATUS_CODE
  return (result_code, [alert_label])