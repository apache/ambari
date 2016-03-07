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
import urllib2
import ambari_simplejson as json # simplejson is much faster comparing to Python 2.6 json module and has the same functions set.
import logging
import traceback

from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries.functions.curl_krb_request import DEFAULT_KERBEROS_KINIT_TIMER_MS
from resource_management.libraries.functions.curl_krb_request import KERBEROS_KINIT_TIMER_PARAMETER
from resource_management.core.environment import Environment

LABEL = 'Last Checkpoint: [{h} hours, {m} minutes, {tx} transactions]'

NN_HTTP_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.http-address}}'
NN_HTTPS_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.https-address}}'
NN_HTTP_POLICY_KEY = '{{hdfs-site/dfs.http.policy}}'
NN_CHECKPOINT_TX_KEY = '{{hdfs-site/dfs.namenode.checkpoint.txns}}'
NN_CHECKPOINT_PERIOD_KEY = '{{hdfs-site/dfs.namenode.checkpoint.period}}'

PERCENT_WARNING_KEY = 'checkpoint.time.warning.threshold'
PERCENT_WARNING_DEFAULT = 200

PERCENT_CRITICAL_KEY = 'checkpoint.time.critical.threshold'
PERCENT_CRITICAL_DEFAULT = 200

CHECKPOINT_TX_DEFAULT = 1000000
CHECKPOINT_PERIOD_DEFAULT = 21600

CONNECTION_TIMEOUT_KEY = 'connection.timeout'
CONNECTION_TIMEOUT_DEFAULT = 5.0

KERBEROS_KEYTAB = '{{hdfs-site/dfs.web.authentication.kerberos.keytab}}'
KERBEROS_PRINCIPAL = '{{hdfs-site/dfs.web.authentication.kerberos.principal}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEY = "{{cluster-env/smokeuser}}"
EXECUTABLE_SEARCH_PATHS = '{{kerberos-env/executable_search_paths}}'

logger = logging.getLogger('ambari_alerts')

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (NN_HTTP_ADDRESS_KEY, NN_HTTPS_ADDRESS_KEY, NN_HTTP_POLICY_KEY, EXECUTABLE_SEARCH_PATHS,
      NN_CHECKPOINT_TX_KEY, NN_CHECKPOINT_PERIOD_KEY, KERBEROS_KEYTAB, KERBEROS_PRINCIPAL, SECURITY_ENABLED_KEY, SMOKEUSER_KEY)
  

def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if configurations is None:
    return (('UNKNOWN', ['There were no configurations supplied to the script.']))
  
  uri = None
  scheme = 'http'  
  http_uri = None
  https_uri = None
  http_policy = 'HTTP_ONLY'
  checkpoint_tx = CHECKPOINT_TX_DEFAULT
  checkpoint_period = CHECKPOINT_PERIOD_DEFAULT
  
  if NN_HTTP_ADDRESS_KEY in configurations:
    http_uri = configurations[NN_HTTP_ADDRESS_KEY]

  if NN_HTTPS_ADDRESS_KEY in configurations:
    https_uri = configurations[NN_HTTPS_ADDRESS_KEY]

  if NN_HTTP_POLICY_KEY in configurations:
    http_policy = configurations[NN_HTTP_POLICY_KEY]

  if NN_CHECKPOINT_TX_KEY in configurations:
    checkpoint_tx = configurations[NN_CHECKPOINT_TX_KEY]

  if NN_CHECKPOINT_PERIOD_KEY in configurations:
    checkpoint_period = configurations[NN_CHECKPOINT_PERIOD_KEY]
    
  if SMOKEUSER_KEY in configurations:
    smokeuser = configurations[SMOKEUSER_KEY]

  executable_paths = None
  if EXECUTABLE_SEARCH_PATHS in configurations:
    executable_paths = configurations[EXECUTABLE_SEARCH_PATHS]

  security_enabled = False
  if SECURITY_ENABLED_KEY in configurations:
    security_enabled = str(configurations[SECURITY_ENABLED_KEY]).upper() == 'TRUE'

  kerberos_keytab = None
  if KERBEROS_KEYTAB in configurations:
    kerberos_keytab = configurations[KERBEROS_KEYTAB]

  kerberos_principal = None
  if KERBEROS_PRINCIPAL in configurations:
    kerberos_principal = configurations[KERBEROS_PRINCIPAL]
    kerberos_principal = kerberos_principal.replace('_HOST', host_name)

  # parse script arguments
  connection_timeout = CONNECTION_TIMEOUT_DEFAULT
  if CONNECTION_TIMEOUT_KEY in parameters:
    connection_timeout = float(parameters[CONNECTION_TIMEOUT_KEY])

  percent_warning = PERCENT_WARNING_DEFAULT
  if PERCENT_WARNING_KEY in parameters:
    percent_warning = float(parameters[PERCENT_WARNING_KEY]) * 100

  percent_critical = PERCENT_CRITICAL_DEFAULT
  if PERCENT_CRITICAL_KEY in parameters:
    percent_critical = float(parameters[PERCENT_CRITICAL_KEY]) * 100

  kinit_timer_ms = parameters.get(KERBEROS_KINIT_TIMER_PARAMETER, DEFAULT_KERBEROS_KINIT_TIMER_MS)

  # determine the right URI and whether to use SSL
  uri = http_uri
  if http_policy == 'HTTPS_ONLY':
    scheme = 'https'
    
    if https_uri is not None:
      uri = https_uri 
  
  current_time = int(round(time.time() * 1000))

  last_checkpoint_time_qry = "{0}://{1}/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem".format(scheme,uri)
  journal_transaction_info_qry = "{0}://{1}/jmx?qry=Hadoop:service=NameNode,name=NameNodeInfo".format(scheme,uri)

  # start out assuming an OK status
  label = None
  result_code = "OK"

  try:
    if kerberos_principal is not None and kerberos_keytab is not None and security_enabled:
      env = Environment.get_instance()

      # curl requires an integer timeout
      curl_connection_timeout = int(connection_timeout)

      last_checkpoint_time_response, error_msg, time_millis = curl_krb_request(env.tmp_dir, kerberos_keytab,
        kerberos_principal, last_checkpoint_time_qry,"checkpoint_time_alert", executable_paths, False,
        "NameNode Last Checkpoint", smokeuser, connection_timeout=curl_connection_timeout,
        kinit_timer_ms = kinit_timer_ms)

      last_checkpoint_time_response_json = json.loads(last_checkpoint_time_response)
      last_checkpoint_time = int(last_checkpoint_time_response_json["beans"][0]["LastCheckpointTime"])

      journal_transaction_info_response, error_msg, time_millis = curl_krb_request(env.tmp_dir, kerberos_keytab,
        kerberos_principal, journal_transaction_info_qry,"checkpoint_time_alert", executable_paths,
        False, "NameNode Last Checkpoint", smokeuser, connection_timeout=curl_connection_timeout,
        kinit_timer_ms = kinit_timer_ms)

      journal_transaction_info_response_json = json.loads(journal_transaction_info_response)
      journal_transaction_info = journal_transaction_info_response_json["beans"][0]["JournalTransactionInfo"]
    else:
      last_checkpoint_time = int(get_value_from_jmx(last_checkpoint_time_qry,
      "LastCheckpointTime", connection_timeout))

      journal_transaction_info = get_value_from_jmx(journal_transaction_info_qry,
      "JournalTransactionInfo", connection_timeout)

    journal_transaction_info_dict = json.loads(journal_transaction_info)
  
    last_tx = int(journal_transaction_info_dict['LastAppliedOrWrittenTxId'])
    most_recent_tx = int(journal_transaction_info_dict['MostRecentCheckpointTxId'])
    transaction_difference = last_tx - most_recent_tx
    
    delta = (current_time - last_checkpoint_time)/1000

    label = LABEL.format(h=get_time(delta)['h'], m=get_time(delta)['m'], tx=transaction_difference)
    
    if (transaction_difference > int(checkpoint_tx)) and (float(delta) / int(checkpoint_period)*100 >= int(percent_critical)):
      result_code = 'CRITICAL'
    elif (transaction_difference > int(checkpoint_tx)) and (float(delta) / int(checkpoint_period)*100 >= int(percent_warning)):
      result_code = 'WARNING'

  except:
    label = traceback.format_exc()
    result_code = 'UNKNOWN'
        
  return ((result_code, [label]))

def get_time(delta):
  h = int(delta/3600)
  m = int((delta % 3600)/60)
  return {'h':h, 'm':m}


def get_value_from_jmx(query, jmx_property, connection_timeout):
  response = None
  
  try:
    response = urllib2.urlopen(query, timeout=connection_timeout)
    data = response.read()

    data_dict = json.loads(data)
    return data_dict["beans"][0][jmx_property]
  finally:
    if response is not None:
      try:
        response.close()
      except:
        pass
