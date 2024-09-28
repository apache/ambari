#!/usr/bin/env python3

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
import logging
import json
import subprocess
import time
import traceback

from resource_management.core import global_lock
from resource_management.core import shell
from resource_management.core.exceptions import Fail
from resource_management.core.resources import Execute
from resource_management.libraries.functions import format
from resource_management.libraries.functions import get_kinit_path
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions.check_process_status import check_process_status
from resource_management.core.exceptions import ComponentIsNotRunning
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl

CRITICAL_RESULT_CODE = 'CRITICAL'
OK_RESULT_CODE = 'OK'
UKNOWN_STATUS_CODE = 'UNKNOWN'

OK_MESSAGE = "The HBase application reported a '{0}' state in {1:.3f}s"
MESSAGE_WITH_STATE_AND_INSTANCES = "The application reported a '{0}' state in {1:.3f}s. [Live: {2}, Desired: {3}]"
CRITICAL_MESSAGE_WITH_STATE = "The HBase application reported a '{0}' state. Check took {1:.3f}s"
CRITICAL_MESSAGE = "ats-hbase service information could not be retrieved"


SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
STACK_ROOT = '{{cluster-env/stack_root}}'
STACK_ROOT_DEFAULT = Script.get_stack_root()


ATS_HBASE_PRINCIPAL_KEY = '{{yarn-hbase-site/hbase.master.kerberos.principal}}'
ATS_HBASE_PRINCIPAL_KEYTAB_KEY = '{{yarn-hbase-site/hbase.master.keytab.file}}'
ATS_HBASE_USER_KEY = '{{yarn-env/yarn_ats_user}}'
ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY = '{{yarn-hbase-env/is_hbase_system_service_launch}}'
USE_EXTERNAL_HBASE_KEY = '{{yarn-hbase-env/use_external_hbase}}'
ATS_HBASE_PID_DIR_PREFIX = '{{yarn-hbase-env/yarn_hbase_pid_dir_prefix}}'

ATS_HBASE_APP_NOT_FOUND_KEY = format("Service ats-hbase not found")

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'


CHECK_COMMAND_TIMEOUT_KEY = 'check.command.timeout'
CHECK_COMMAND_TIMEOUT_DEFAULT = 120.0


logger = logging.getLogger('ambari_alerts')


def get_tokens():
    """
    Returns a tuple of tokens in the format {{site/property}} that will be used
    to build the dictionary passed into execute
    """
    return (SECURITY_ENABLED_KEY, KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY, ATS_HBASE_PRINCIPAL_KEY, ATS_HBASE_PRINCIPAL_KEYTAB_KEY,
            ATS_HBASE_USER_KEY, STACK_ROOT, USE_EXTERNAL_HBASE_KEY, ATS_HBASE_PID_DIR_PREFIX, ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY)


def execute(configurations={}, parameters={}, host_name=None):
    """
    Returns a tuple containing the result code and a pre-formatted result label

    Keyword arguments:
    configurations (dictionary): a mapping of configuration key to value
    parameters (dictionary): a mapping of script parameter key to value
    host_name (string): the name of this host where the alert is running
    """

    if configurations is None:
        return (UKNOWN_STATUS_CODE, ['There were no configurations supplied to the script.'])

    result_code = None

    try:
        use_external_hbase = False
        if USE_EXTERNAL_HBASE_KEY in configurations:
            use_external_hbase = str(configurations[USE_EXTERNAL_HBASE_KEY]).upper() == 'TRUE'

        if use_external_hbase:
            return (OK_RESULT_CODE, ['use_external_hbase set to true.'])

        is_hbase_system_service_launch = False
        if ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY in configurations:
            is_hbase_system_service_launch = str(configurations[ATS_HBASE_SYSTEM_SERVICE_LAUNCH_KEY]).upper() == 'TRUE'

        yarn_hbase_user = "yarn-ats"
        if ATS_HBASE_USER_KEY in configurations:
             yarn_hbase_user = configurations[ATS_HBASE_USER_KEY]

        if not is_hbase_system_service_launch:
            yarn_hbase_pid_dir_prefix = ""
            if ATS_HBASE_PID_DIR_PREFIX in configurations:
                yarn_hbase_pid_dir_prefix = configurations[ATS_HBASE_PID_DIR_PREFIX]
            else:
                return (UKNOWN_STATUS_CODE, ['The yarn_hbase_pid_dir_prefix is a required parameter.'])
            yarn_hbase_pid_dir = format("{yarn_hbase_pid_dir_prefix}/{yarn_hbase_user}")
            master_pid_file = format("{yarn_hbase_pid_dir}/hbase-{yarn_hbase_user}-master.pid")
            rs_pid_file = format("{yarn_hbase_pid_dir}/hbase-{yarn_hbase_user}-regionserver.pid")

            if host_name is None:
                host_name = socket.getfqdn()

            master_process_running = is_monitor_process_live(master_pid_file)
            rs_process_running = is_monitor_process_live(rs_pid_file)

            alert_state = OK_RESULT_CODE if master_process_running and rs_process_running else CRITICAL_RESULT_CODE

            alert_label = 'ATS embedded HBase is running on {0}' if master_process_running and rs_process_running else 'ATS embedded HBase is NOT running on {0}'
            alert_label = alert_label.format(host_name)

            return (alert_state, [alert_label])
        else:
            security_enabled = False
            if SECURITY_ENABLED_KEY in configurations:
                security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

            check_command_timeout = CHECK_COMMAND_TIMEOUT_DEFAULT
            if CHECK_COMMAND_TIMEOUT_KEY in configurations:
                check_command_timeout = int(parameters[CHECK_COMMAND_TIMEOUT_KEY])

            if security_enabled:
                if ATS_HBASE_PRINCIPAL_KEY in configurations:
                    ats_hbase_app_principal = configurations[ATS_HBASE_PRINCIPAL_KEY]
                    ats_hbase_app_principal = ats_hbase_app_principal.replace('_HOST',host_name.lower())

                if ATS_HBASE_PRINCIPAL_KEYTAB_KEY in configurations:
                    ats_hbase_app_keytab = configurations[ATS_HBASE_PRINCIPAL_KEYTAB_KEY]

                # Get the configured Kerberos executable search paths, if any
                if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
                    kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]
                else:
                    kerberos_executable_search_paths = None

                kinit_path_local = get_kinit_path(kerberos_executable_search_paths)
                kinitcmd=format("{kinit_path_local} -kt {ats_hbase_app_keytab} {ats_hbase_app_principal}; ")

                # prevent concurrent kinit
                kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
                kinit_lock.acquire()
                try:
                    Execute(kinitcmd, user=yarn_hbase_user,
                            path=["/bin/", "/usr/bin/", "/usr/sbin/"],
                            timeout=10)
                finally:
                    kinit_lock.release()

            start_time = time.time()
            ats_hbase_status_cmd = STACK_ROOT_DEFAULT + format("/current/hadoop-yarn-client/bin/yarn app -status ats-hbase")

            code, output, error = shell.checked_call(ats_hbase_status_cmd, user=yarn_hbase_user, stderr=subprocess.PIPE,
                                                 timeout=check_command_timeout,
                                                 logoutput=False)
            if code != 0:
                alert_label = traceback.format_exc()
                result_code = UKNOWN_STATUS_CODE
                return (result_code, [alert_label])


            # Call for getting JSON
            ats_hbase_app_info = make_valid_json(output)

            if ats_hbase_app_info is None:
                alert_label = CRITICAL_MESSAGE
                result_code = CRITICAL_RESULT_CODE
                return (result_code, [alert_label])

            if 'state' not in ats_hbase_app_info:
                alert_label = traceback.format_exc()
                result_code = UKNOWN_STATUS_CODE
                return (result_code, [alert_label])

            retrieved_ats_hbase_app_state = ats_hbase_app_info['state'].upper()

            if retrieved_ats_hbase_app_state in ['STABLE']:
                result_code = OK_RESULT_CODE
                total_time = time.time() - start_time
                alert_label = OK_MESSAGE.format(retrieved_ats_hbase_app_state, total_time)
            else:
                result_code = CRITICAL_RESULT_CODE
                total_time = time.time() - start_time
                alert_label = CRITICAL_MESSAGE_WITH_STATE.format(retrieved_ats_hbase_app_state, total_time)
    except:
        alert_label = traceback.format_exc()
        traceback.format_exc()
        result_code = CRITICAL_RESULT_CODE
    return (result_code, [alert_label])


def make_valid_json(output):
    splits = output.split("\n")
    ats_hbase_app_info = None
    json_element = None  # To detect where from to start reading for JSON data
    for idx, split in enumerate(splits):
        curr_elem = split.strip()
        if curr_elem.startswith( '{' ) and curr_elem.endswith( '}' ):
            json_element = curr_elem
            break
        elif ATS_HBASE_APP_NOT_FOUND_KEY in curr_elem:
            return ats_hbase_app_info

    # Remove extra logging from possible JSON output
    if json_element is None:
        raise Fail("Couldn't validate the received output for JSON parsing.")

    ats_hbase_app_info = json.loads(json_element)
    return ats_hbase_app_info

@OsFamilyFuncImpl(OsFamilyImpl.DEFAULT)
def is_monitor_process_live(pid_file):
    """
    Gets whether the Metrics Monitor represented by the specified file is running.
    :param pid_file: the PID file of the monitor to check
    :return: True if the monitor is running, False otherwise
    """
    live = False

    try:
        check_process_status(pid_file)
        live = True
    except ComponentIsNotRunning:
        pass

    return live