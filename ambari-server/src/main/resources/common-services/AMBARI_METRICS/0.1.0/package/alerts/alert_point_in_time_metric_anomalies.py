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

import json
import urllib
import time
import os
import ambari_commons.network as network
import logging

from ambari_agent.AmbariConfig import AmbariConfig

RESULT_STATE_OK = 'OK'
RESULT_STATE_CRITICAL = 'CRITICAL'
RESULT_STATE_WARNING = 'WARNING'
RESULT_STATE_UNKNOWN = 'UNKNOWN'
RESULT_STATE_SKIPPED = 'SKIPPED'

AMS_HTTP_POLICY = '{{ams-site/timeline.metrics.service.http.policy}}'
METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY = '{{ams-site/timeline.metrics.service.webapp.address}}'
METRICS_COLLECTOR_VIP_HOST_KEY = '{{cluster-env/metrics_collector_vip_host}}'
METRICS_COLLECTOR_VIP_PORT_KEY = '{{cluster-env/metrics_collector_vip_port}}'

INTERVAL_PARAM_KEY = 'interval'
INTERVAL_PARAM_DEFAULT = 10

NUM_ANOMALIES_KEY = 'num_anomalies'
NUM_ANOMALIES_DEFAULT = 5

SENSITIVITY_KEY = 'sensitivity'
SENSITIVITY_DEFAULT = 5

AMS_METRICS_GET_URL = "/ws/v1/timeline/metrics/anomalies?%s"

logger = logging.getLogger()

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (METRICS_COLLECTOR_VIP_HOST_KEY, METRICS_COLLECTOR_VIP_PORT_KEY,
          METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY, AMS_HTTP_POLICY)


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  """
  Get ready with AMS GET url.
  Query AMS for point in time anomalies in the last 30mins. 
  Generate a message with anomalies.
  """
  if configurations is None:
    return (RESULT_STATE_UNKNOWN, ['There were no configurations supplied to the script.'])

  collector_host = host_name
  current_time = int(time.time()) * 1000

  interval = INTERVAL_PARAM_DEFAULT
  if INTERVAL_PARAM_KEY in parameters:
    interval = _coerce_to_integer(parameters[INTERVAL_PARAM_KEY])

  num_anomalies = NUM_ANOMALIES_DEFAULT
  if NUM_ANOMALIES_KEY in parameters:
    num_anomalies = _coerce_to_integer(parameters[NUM_ANOMALIES_KEY])

  sensitivity = SENSITIVITY_DEFAULT
  if SENSITIVITY_KEY in parameters:
    sensitivity = _coerce_to_integer(parameters[SENSITIVITY_KEY])

  if METRICS_COLLECTOR_VIP_HOST_KEY in configurations and METRICS_COLLECTOR_VIP_PORT_KEY in configurations:
    collector_host = configurations[METRICS_COLLECTOR_VIP_HOST_KEY]
    collector_port = int(configurations[METRICS_COLLECTOR_VIP_PORT_KEY])
  else:
    # ams-site/timeline.metrics.service.webapp.address is required
    if not METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY in configurations:
      return (RESULT_STATE_UNKNOWN,
              ['{0} is a required parameter for the script'.format(METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY)])
    else:
      collector_webapp_address = configurations[METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY].split(":")
      if valid_collector_webapp_address(collector_webapp_address):
        collector_port = int(collector_webapp_address[1])
      else:
        return (RESULT_STATE_UNKNOWN, ['{0} value should be set as "fqdn_hostname:port", but set to {1}'.format(
          METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY, configurations[METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY])])

  get_ema_anomalies_parameters = {
    "method": "ema",
    "startTime": current_time - interval * 60 * 1000,
    "endTime": current_time,
    "limit": num_anomalies + 1
  }

  encoded_get_metrics_parameters = urllib.urlencode(get_ema_anomalies_parameters)

  ams_collector_conf_dir = "/etc/ambari-metrics-collector/conf"
  metric_truststore_ca_certs = 'ca.pem'
  ca_certs = os.path.join(ams_collector_conf_dir,
                          metric_truststore_ca_certs)
  metric_collector_https_enabled = str(configurations[AMS_HTTP_POLICY]) == "HTTPS_ONLY"

  try:
    conn = network.get_http_connection(
      collector_host,
      int(collector_port),
      metric_collector_https_enabled,
      ca_certs,
      ssl_version=AmbariConfig.get_resolved_config().get_force_https_protocol_value()
    )
    conn.request("GET", AMS_METRICS_GET_URL % encoded_get_metrics_parameters)
    response = conn.getresponse()
    data = response.read()
    logger.info("Data read from metric anomaly endpoint")
    logger.info(data)
    conn.close()
  except Exception:
    return (RESULT_STATE_UNKNOWN, ["Unable to retrieve anomaly metrics from the Ambari Metrics service."])

  if response.status != 200:
    return (RESULT_STATE_UNKNOWN, ["Unable to retrieve anomaly metrics from the Ambari Metrics service."])

  data_json = json.loads(data)
  length = len(data_json["metrics"])
  logger.info("Number of anomalies returned : {0}".format(length))

  if length == 0:
    alert_state = RESULT_STATE_OK
    alert_label = 'No point in time anomalies in the last {0} minutes.'.format(interval)
    logger.info(alert_label)
  elif length <= 5:
    alert_state = RESULT_STATE_WARNING
    alert_label = "http://avijayan-ad-1.openstacklocal:3000/dashboard/script/scripted.js?anomalies=" + data
  else:
    alert_state = RESULT_STATE_CRITICAL
    alert_label = "http://avijayan-ad-1.openstacklocal:3000/dashboard/script/scripted.js?anomalies=" + data

  return (alert_state, [alert_label])


def valid_collector_webapp_address(webapp_address):
  if len(webapp_address) == 2 \
    and webapp_address[0] != '127.0.0.1' \
    and webapp_address[1].isdigit():
    return True

  return False


def _coerce_to_integer(value):
  """
  Attempts to correctly coerce a value to an integer. For the case of an integer or a float,
  this will essentially either NOOP or return a truncated value. If the parameter is a string,
  then it will first attempt to be coerced from a integer, and failing that, a float.
  :param value: the value to coerce
  :return: the coerced value as an integer
  """
  try:
    return int(value)
  except ValueError:
    return int(float(value))
