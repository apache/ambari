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

import os
import socket
import time
import logging
import traceback
from resource_management.libraries.functions import format
from ambari_commons.os_family_impl import OsFamilyFuncImpl, OsFamilyImpl
from resource_management.libraries.script.script import Script
from resource_management.libraries.functions import get_kinit_path
from resource_management.core.resources import Execute
from resource_management.core import global_lock
from resource_management.core.signal_utils import TerminateStrategy


stack_root = Script.get_stack_root()

OK_MESSAGE = "TCP OK - {0:.3f}s response on port {1}"
CRITICAL_MESSAGE = "Connection failed on host {0}:{1} ({2})"

HIVE_SERVER_THRIFT_PORT_KEY = '{{spark2-hive-site-override/hive.server2.thrift.port}}'
HIVE_SERVER_THRIFT_HTTP_PORT_KEY = '{{spark2-hive-site-override/hive.server2.thrift.http.port}}'

HIVE_SERVER_TRANSPORT_MODE_KEY = '{{spark2-hive-site-override/hive.server2.transport.mode}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'

HIVE_SERVER2_AUTHENTICATION_KEY = '{{hive-site/hive.server2.authentication}}'
HIVE_SERVER2_KERBEROS_KEYTAB = '{{hive-site/hive.server2.authentication.kerberos.keytab}}'
HIVE_SERVER2_PRINCIPAL_KEY = '{{hive-site/hive.server2.authentication.kerberos.principal}}'

SPARK_SSL_ENABLED = '{{spark2-defaults/spark.ssl.enabled}}'
SPARK_TRUST_STORE_PATH = '{{spark2-defaults/spark.ssl.trustStore}}'
SPARK_TRUST_STORE_PASS = '{{spark2-defaults/spark.ssl.trustStorePassword}}'

# The configured Kerberos executable search paths, if any
KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY = '{{kerberos-env/executable_search_paths}}'

THRIFT_PORT_DEFAULT = 10002
HIVE_SERVER_TRANSPORT_MODE_DEFAULT = 'binary'

HIVEUSER_DEFAULT = 'hive'

CHECK_COMMAND_TIMEOUT_KEY = 'check.command.timeout'
CHECK_COMMAND_TIMEOUT_DEFAULT = 60.0

logger = logging.getLogger('ambari_alerts')

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def get_tokens():
    """
    Returns a tuple of tokens in the format {{site/property}} that will be used
    to build the dictionary passed into execute
    """
    return (HIVE_SERVER_THRIFT_PORT_KEY, HIVE_SERVER_TRANSPORT_MODE_KEY, SECURITY_ENABLED_KEY, KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY,
            HIVEUSER_DEFAULT, HIVE_SERVER2_KERBEROS_KEYTAB, HIVE_SERVER2_PRINCIPAL_KEY, SPARK_SSL_ENABLED, SPARK_TRUST_STORE_PATH, SPARK_TRUST_STORE_PASS, HIVE_SERVER_THRIFT_HTTP_PORT_KEY)

@OsFamilyFuncImpl(os_family=OsFamilyImpl.DEFAULT)
def execute(configurations={}, parameters={}, host_name=None):
    """
    Returns a tuple containing the result code and a pre-formatted result label

    Keyword arguments:
    configurations (dictionary): a mapping of configuration key to value
    parameters (dictionary): a mapping of script parameter key to value
    host_name (string): the name of this host where the alert is running
    """

    spark_home = os.path.join(stack_root, "current", 'spark2-client')

    if configurations is None:
        return ('UNKNOWN', ['There were no configurations supplied to the script.'])

    transport_mode = HIVE_SERVER_TRANSPORT_MODE_DEFAULT
    if HIVE_SERVER_TRANSPORT_MODE_KEY in configurations:
        transport_mode = configurations[HIVE_SERVER_TRANSPORT_MODE_KEY]

    port = THRIFT_PORT_DEFAULT
    if transport_mode.lower() == 'binary' and HIVE_SERVER_THRIFT_PORT_KEY in configurations:
        port = int(configurations[HIVE_SERVER_THRIFT_PORT_KEY])
    elif transport_mode.lower() == 'http' and HIVE_SERVER_THRIFT_HTTP_PORT_KEY in configurations:
    	port = int(configurations[HIVE_SERVER_THRIFT_HTTP_PORT_KEY])
    
    security_enabled = False
    if SECURITY_ENABLED_KEY in configurations:
        security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

    hive_kerberos_keytab = None
    if HIVE_SERVER2_KERBEROS_KEYTAB in configurations:
        hive_kerberos_keytab = configurations[HIVE_SERVER2_KERBEROS_KEYTAB]

    if host_name is None:
        host_name = socket.getfqdn()

    hive_principal = None
    if HIVE_SERVER2_PRINCIPAL_KEY in configurations:
        hive_principal = configurations[HIVE_SERVER2_PRINCIPAL_KEY]
        hive_principal = hive_principal.replace('_HOST',host_name.lower())

    # Get the Trust store and pass
    spark_truststore_path = None
    spark_truststore_pass = None
    spark_ssl_enabled = False
    if SPARK_SSL_ENABLED in configurations:
         spark_truststore_path = configurations[SPARK_TRUST_STORE_PATH]
         spark_truststore_pass = configurations[SPARK_TRUST_STORE_PASS]
         spark_ssl_enabled = str(configurations[SPARK_SSL_ENABLED]).upper() == 'TRUE'

    # Get the configured Kerberos executable search paths, if any
    if KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY in configurations:
        kerberos_executable_search_paths = configurations[KERBEROS_EXECUTABLE_SEARCH_PATHS_KEY]
    else:
        kerberos_executable_search_paths = None

    kinit_path_local = get_kinit_path(kerberos_executable_search_paths)

    hiveruser = HIVEUSER_DEFAULT

    if security_enabled:
        kinitcmd = format("{kinit_path_local} -kt {hive_kerberos_keytab} {hive_principal}; ")
        # prevent concurrent kinit
        kinit_lock = global_lock.get_lock(global_lock.LOCK_TYPE_KERBEROS)
        kinit_lock.acquire()
        try:
            Execute(kinitcmd, user=hiveruser)
        finally:
            kinit_lock.release()

    result_code = None
    try:
        if host_name is None:
            host_name = socket.getfqdn()

        beeline_url = ["jdbc:hive2://{host_name}:{port}/default", "transportMode={transport_mode}"]
        if security_enabled:
            beeline_url.append("principal={hive_principal}")
        if transport_mode == "http":
            beeline_url.append("httpPath=cliservice")
            if spark_ssl_enabled:
                beeline_url.extend(["ssl=true", "sslTrustStore={spark_truststore_path}", "trustStorePassword={spark_truststore_pass!p}"])

        # append url according to used transport

        beeline_cmd = os.path.join(spark_home, "bin", "beeline")
        cmd = "! beeline -u '%s'  -e '' 2>&1| awk '{print}'|grep -i -e 'Connection refused' -e 'Invalid URL'" % \
              (format(";".join(beeline_url)))

        start_time = time.time()
        try:
            Execute(cmd,
                    user=hiveruser,
                    path=[beeline_cmd],
                    timeout=CHECK_COMMAND_TIMEOUT_DEFAULT,
                    timeout_kill_strategy=TerminateStrategy.KILL_PROCESS_TREE
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
