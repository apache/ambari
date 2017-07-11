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
import json

LABEL = 'Last Checkpoint: [{h} hours, {m} minutes, {tx} transactions]'

NN_HTTP_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.http-address}}'
NN_HTTPS_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.https-address}}'
NN_HTTP_POLICY_KEY = '{{hdfs-site/dfs.http.policy}}'
NN_CHECKPOINT_TX_KEY = '{{hdfs-site/dfs.namenode.checkpoint.txns}}'
NN_CHECKPOINT_PERIOD_KEY = '{{hdfs-site/dfs.namenode.checkpoint.period}}'

PERCENT_WARNING = 200
PERCENT_CRITICAL = 200

CHECKPOINT_TX_DEFAULT = 1000000
CHECKPOINT_PERIOD_DEFAULT = 21600

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (NN_HTTP_ADDRESS_KEY, NN_HTTPS_ADDRESS_KEY, NN_HTTP_POLICY_KEY,
      NN_CHECKPOINT_TX_KEY, NN_CHECKPOINT_PERIOD_KEY)


def execute(parameters=None, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  parameters (dictionary): a mapping of parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  if parameters is None:
    return (('UNKNOWN', ['There were no parameters supplied to the script.']))

  uri = None
  scheme = 'http'
  http_uri = None
  https_uri = None
  http_policy = 'HTTP_ONLY'
  percent_warning = PERCENT_WARNING
  percent_critical = PERCENT_CRITICAL
  checkpoint_tx = CHECKPOINT_TX_DEFAULT
  checkpoint_period = CHECKPOINT_PERIOD_DEFAULT

  if NN_HTTP_ADDRESS_KEY in parameters:
    http_uri = parameters[NN_HTTP_ADDRESS_KEY]

  if NN_HTTPS_ADDRESS_KEY in parameters:
    https_uri = parameters[NN_HTTPS_ADDRESS_KEY]

  if NN_HTTP_POLICY_KEY in parameters:
    http_policy = parameters[NN_HTTP_POLICY_KEY]

  if NN_CHECKPOINT_TX_KEY in parameters:
    checkpoint_tx = parameters[NN_CHECKPOINT_TX_KEY]

  if NN_CHECKPOINT_PERIOD_KEY in parameters:
    checkpoint_period = parameters[NN_CHECKPOINT_PERIOD_KEY]

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
    last_checkpoint_time = int(get_value_from_jmx(last_checkpoint_time_qry,"LastCheckpointTime"))
    journal_transaction_info = get_value_from_jmx(journal_transaction_info_qry,"JournalTransactionInfo")
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

  except Exception, e:
    label = str(e)
    result_code = 'UNKNOWN'

  return ((result_code, [label]))

def get_time(delta):
  h = int(delta/3600)
  m = int((delta % 3600)/60)
  return {'h':h, 'm':m}


def get_value_from_jmx(query, jmx_property):
  response = None

  try:
    response = urllib2.urlopen(query)
    data = response.read()

    data_dict = json.loads(data)
    return data_dict["beans"][0][jmx_property]
  finally:
    if response is not None:
      try:
        response.close()
      except:
        pass
