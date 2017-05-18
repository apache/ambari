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
import socket
from resource_management import *
from resource_management.libraries.functions import format
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.libraries.script.script import Script
from resource_management.core.resources import Execute
from resource_management.core.logger import Logger
from resource_management.core import global_lock
from resource_management.libraries.functions import get_kinit_path


OK_MESSAGE = "TCP OK - {0:.3f}s response on port {1}"
CRITICAL_MESSAGE = "Connection failed on host {0}:{1} ({2})"

logger = logging.getLogger('ambari_alerts')

LIVY_SERVER_PORT_KEY = '{{livy-conf/livy.server.port}}'

LIVYUSER_DEFAULT = 'livy'

CHECK_COMMAND_TIMEOUT_KEY = 'check.command.timeout'
CHECK_COMMAND_TIMEOUT_DEFAULT = 60.0

SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEYTAB_KEY = '{{cluster-env/smokeuser_keytab}}'
SMOKEUSER_PRINCIPAL_KEY = '{{cluster-env/smokeuser_principal_name}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'
LIVY_SSL_ENABLED_KEY = '{{livy-conf/livy.keystore}}'

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'


@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_tokens():
    """
    Returns a tuple of tokens in the format {{site/property}} that will be used
    to build the dictionary passed into execute
    """
    return (LIVY_SERVER_PORT_KEY,LIVYUSER_DEFAULT,SECURITY_ENABLED_KEY,SMOKEUSER_KEYTAB_KEY,SMOKEUSER_PRINCIPAL_KEY,SMOKEUSER_KEY,LIVY_SSL_ENABLED_KEY)

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

    LIVY_PORT_DEFAULT = 8998

    port = LIVY_PORT_DEFAULT
    if LIVY_SERVER_PORT_KEY in configurations:
        port = int(configurations[LIVY_SERVER_PORT_KEY])

    if host_name is None:
        host_name = socket.getfqdn()

    livyuser = configurations[SMOKEUSER_KEY]

    security_enabled = False
    if SECURITY_ENABLED_KEY in configurations:
        security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

    smokeuser_kerberos_keytab = None
    if SMOKEUSER_KEYTAB_KEY in configurations:
        smokeuser_kerberos_keytab = configurations[SMOKEUSER_KEYTAB_KEY]

    if host_name is None:
        host_name = socket.getfqdn()

    smokeuser_principal = None
    if SMOKEUSER_PRINCIPAL_KEY in configurations:
        smokeuser_principal = configurations[SMOKEUSER_PRINCIPAL_KEY]
        smokeuser_principal = smokeuser_principal.replace('_HOST',host_name.lower())

    # Get the configured Kerberos executable search paths, if any
    if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
        kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]
    else:
        kerberos_executable_search_paths = None

    kinit_path_local = get_kinit_path(kerberos_executable_search_paths)

    if security_enabled:
        kinitcmd = format("{kinit_path_local} -kt {smokeuser_kerberos_keytab} {smokeuser_principal}; ")
        # prevent concurrent kinit
        kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
        kinit_lock.acquire()
        try:
            Execute(kinitcmd, user=livyuser)
        finally:
            kinit_lock.release()

    http_scheme = 'https' if LIVY_SSL_ENABLED_KEY in configurations else 'http'
    result_code = None
    try:
        start_time = time.time()
        try:
            livy_livyserver_host = str(host_name)

            livy_cmd = format("curl -s -o /dev/null -w'%{{http_code}}' --negotiate -u: -k {http_scheme}://{livy_livyserver_host}:{port}/sessions | grep 200 ")

            Execute(livy_cmd,
                    tries=3,
                    try_sleep=1,
                    logoutput=True,
                    user=livyuser
                    )

            total_time = time.time() - start_time
            result_code = 'OK'
            label = OK_MESSAGE.format(total_time, port)
        except:
            result_code = 'CRITICAL'
            label = CRITICAL_MESSAGE.format(host_name, port, traceback.format_exc())
    except:
        label = traceback.format_exc()
        result_code = 'UNKNOWN'

    return (result_code, [label])
