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
import httplib
import locale
import json
import logging
import urllib
import time
import urllib2
import os
import ambari_commons.network as network

from resource_management import Environment
from ambari_commons.aggregate_functions import sample_standard_deviation, mean

from resource_management.libraries.functions.curl_krb_request import curl_krb_request
from resource_management.libraries.functions.curl_krb_request import DEFAULT_KERBEROS_KINIT_TIMER_MS
from resource_management.libraries.functions.curl_krb_request import KERBEROS_KINIT_TIMER_PARAMETER
from resource_management.libraries.functions.namenode_ha_utils import get_name_service_by_hostname
from ambari_commons.ambari_metrics_helper import select_metric_collector_for_sink
from ambari_agent.AmbariConfig import AmbariConfig

RESULT_STATE_OK = 'OK'
RESULT_STATE_CRITICAL = 'CRITICAL'
RESULT_STATE_WARNING = 'WARNING'
RESULT_STATE_UNKNOWN = 'UNKNOWN'
RESULT_STATE_SKIPPED = 'SKIPPED'

HDFS_NN_STATE_ACTIVE = 'active'
HDFS_NN_STATE_STANDBY = 'standby'

HDFS_SITE_KEY = '{{hdfs-site}}'
NAMESERVICE_KEY = '{{hdfs-site/dfs.internal.nameservices}}'
NN_HTTP_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.http-address}}'
NN_HTTPS_ADDRESS_KEY = '{{hdfs-site/dfs.namenode.https-address}}'
DFS_POLICY_KEY = '{{hdfs-site/dfs.http.policy}}'

KERBEROS_KEYTAB = '{{hdfs-site/dfs.web.authentication.kerberos.keytab}}'
KERBEROS_PRINCIPAL = '{{hdfs-site/dfs.web.authentication.kerberos.principal}}'
SECURITY_ENABLED_KEY = '{{cluster-env/security_enabled}}'
SMOKEUSER_KEY = '{{cluster-env/smokeuser}}'
EXECUTABLE_SEARCH_PATHS = '{{kerberos-env/executable_search_paths}}'

AMS_HTTP_POLICY = '{{ams-site/timeline.metrics.service.http.policy}}'
METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY = '{{ams-site/timeline.metrics.service.webapp.address}}'
METRICS_COLLECTOR_VIP_HOST_KEY = '{{cluster-env/metrics_collector_external_hosts}}'
METRICS_COLLECTOR_VIP_PORT_KEY = '{{cluster-env/metrics_collector_external_port}}'

CONNECTION_TIMEOUT_KEY = 'connection.timeout'
CONNECTION_TIMEOUT_DEFAULT = 5.0

MERGE_HA_METRICS_PARAM_KEY = 'mergeHaMetrics'
MERGE_HA_METRICS_PARAM_DEFAULT = False
METRIC_NAME_PARAM_KEY = 'metricName'
METRIC_NAME_PARAM_DEFAULT = ''
METRIC_UNITS_PARAM_KEY = 'metric.units'
METRIC_UNITS_DEFAULT = ''
APP_ID_PARAM_KEY = 'appId'
APP_ID_PARAM_DEFAULT = 'NAMENODE'

# the interval to check the metric (should be cast to int but could be a float)
INTERVAL_PARAM_KEY = 'interval'
INTERVAL_PARAM_DEFAULT = 60

# the default threshold to trigger a CRITICAL (should be cast to int but could a float)
DEVIATION_CRITICAL_THRESHOLD_KEY = 'metric.deviation.critical.threshold'
DEVIATION_CRITICAL_THRESHOLD_DEFAULT = 10

# the default threshold to trigger a WARNING (should be cast to int but could be a float)
DEVIATION_WARNING_THRESHOLD_KEY = 'metric.deviation.warning.threshold'
DEVIATION_WARNING_THRESHOLD_DEFAULT = 5
NAMENODE_SERVICE_RPC_PORT_KEY = ''

MINIMUM_VALUE_THRESHOLD_KEY = 'minimumValue'

AMS_METRICS_GET_URL = "/ws/v1/timeline/metrics?%s"

# The variance for this alert is 27MB which is 27% of the 100MB average (20MB is the limit)
DEVIATION_THRESHOLD_MESSAGE = "The variance for this alert is {0}{1} which is {2:.0f}% of the {3}{4} average ({5}{6} is the limit)"

# The variance for this alert is 15MB which is within 20% of the 904ms average (20MB is the limit)
DEVIATION_OK_MESSAGE = "The variance for this alert is {0}{1} which is within {2:.0f}% of the {3}{4} average ({5}{6} is the limit)"

logger = logging.getLogger()

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (HDFS_SITE_KEY, NAMESERVICE_KEY, NN_HTTP_ADDRESS_KEY, DFS_POLICY_KEY,
          EXECUTABLE_SEARCH_PATHS, NN_HTTPS_ADDRESS_KEY, SMOKEUSER_KEY,
          KERBEROS_KEYTAB, KERBEROS_PRINCIPAL, SECURITY_ENABLED_KEY,
          METRICS_COLLECTOR_VIP_HOST_KEY, METRICS_COLLECTOR_VIP_PORT_KEY,
          METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY, AMS_HTTP_POLICY)

def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations : a mapping of configuration key to value
  parameters : a mapping of script parameter key to value
  host_name : the name of this host where the alert is running

  :type configurations dict
  :type parameters dict
  :type host_name str
  """
  hostnames = host_name
  current_time = int(time.time()) * 1000

  # parse script arguments
  connection_timeout = CONNECTION_TIMEOUT_DEFAULT
  if CONNECTION_TIMEOUT_KEY in parameters:
    connection_timeout = float(parameters[CONNECTION_TIMEOUT_KEY])

  merge_ha_metrics = MERGE_HA_METRICS_PARAM_DEFAULT
  if MERGE_HA_METRICS_PARAM_KEY in parameters:
    merge_ha_metrics = parameters[MERGE_HA_METRICS_PARAM_KEY].lower() == 'true'

  metric_name = METRIC_NAME_PARAM_DEFAULT
  if METRIC_NAME_PARAM_KEY in parameters:
    metric_name = parameters[METRIC_NAME_PARAM_KEY]

  metric_units = METRIC_UNITS_DEFAULT
  if METRIC_UNITS_PARAM_KEY in parameters:
    metric_units = parameters[METRIC_UNITS_PARAM_KEY]

  app_id = APP_ID_PARAM_DEFAULT
  if APP_ID_PARAM_KEY in parameters:
    app_id = parameters[APP_ID_PARAM_KEY]

  interval = INTERVAL_PARAM_DEFAULT
  if INTERVAL_PARAM_KEY in parameters:
    interval = _coerce_to_integer(parameters[INTERVAL_PARAM_KEY])

  warning_threshold = DEVIATION_WARNING_THRESHOLD_DEFAULT
  if DEVIATION_WARNING_THRESHOLD_KEY in parameters:
    warning_threshold = _coerce_to_integer(parameters[DEVIATION_WARNING_THRESHOLD_KEY])

  critical_threshold = DEVIATION_CRITICAL_THRESHOLD_DEFAULT
  if DEVIATION_CRITICAL_THRESHOLD_KEY in parameters:
    critical_threshold = _coerce_to_integer(parameters[DEVIATION_CRITICAL_THRESHOLD_KEY])

  minimum_value_threshold = None
  if MINIMUM_VALUE_THRESHOLD_KEY in parameters:
    minimum_value_threshold = _coerce_to_integer(parameters[MINIMUM_VALUE_THRESHOLD_KEY])

  #parse configuration
  if configurations is None:
    return (RESULT_STATE_UNKNOWN, ['There were no configurations supplied to the script.'])

  # hdfs-site is required
  if not HDFS_SITE_KEY in configurations:
    return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script'.format(HDFS_SITE_KEY)])

  if METRICS_COLLECTOR_VIP_HOST_KEY in configurations and METRICS_COLLECTOR_VIP_PORT_KEY in configurations:
    collector_host = configurations[METRICS_COLLECTOR_VIP_HOST_KEY].split(',')[0]
    collector_port = int(configurations[METRICS_COLLECTOR_VIP_PORT_KEY])
  else:
    # ams-site/timeline.metrics.service.webapp.address is required
    if not METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY in configurations:
      return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script'.format(METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY)])
    else:
      collector_webapp_address = configurations[METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY].split(":")
      if valid_collector_webapp_address(collector_webapp_address):
        collector_host = select_metric_collector_for_sink(app_id.lower())
        collector_port = int(collector_webapp_address[1])
      else:
        return (RESULT_STATE_UNKNOWN, ['{0} value should be set as "fqdn_hostname:port", but set to {1}'.format(
          METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY, configurations[METRICS_COLLECTOR_WEBAPP_ADDRESS_KEY])])

  namenode_service_rpc_address = None
  # hdfs-site is required
  if not HDFS_SITE_KEY in configurations:
    return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script'.format(HDFS_SITE_KEY)])

  hdfs_site = configurations[HDFS_SITE_KEY]

  if 'dfs.namenode.servicerpc-address' in hdfs_site:
    namenode_service_rpc_address = hdfs_site['dfs.namenode.servicerpc-address']

  # if namenode alert and HA mode
  if NAMESERVICE_KEY in configurations and app_id.lower() == 'namenode':
    # hdfs-site is required
    if not HDFS_SITE_KEY in configurations:
      return (RESULT_STATE_UNKNOWN, ['{0} is a required parameter for the script'.format(HDFS_SITE_KEY)])

    if SMOKEUSER_KEY in configurations:
      smokeuser = configurations[SMOKEUSER_KEY]

    executable_paths = None
    if EXECUTABLE_SEARCH_PATHS in configurations:
      executable_paths = configurations[EXECUTABLE_SEARCH_PATHS]

    # parse script arguments
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

    # determine whether or not SSL is enabled
    is_ssl_enabled = False
    if DFS_POLICY_KEY in configurations:
      dfs_policy = configurations[DFS_POLICY_KEY]
      if dfs_policy == "HTTPS_ONLY":
        is_ssl_enabled = True

    kinit_timer_ms = parameters.get(KERBEROS_KINIT_TIMER_PARAMETER, DEFAULT_KERBEROS_KINIT_TIMER_MS)

    name_service = get_name_service_by_hostname(hdfs_site, host_name)

    # look for dfs.ha.namenodes.foo
    nn_unique_ids_key = 'dfs.ha.namenodes.' + name_service
    if not nn_unique_ids_key in hdfs_site:
      return (RESULT_STATE_UNKNOWN, ['Unable to find unique NameNode alias key {0}'.format(nn_unique_ids_key)])

    namenode_http_fragment = 'dfs.namenode.http-address.{0}.{1}'
    jmx_uri_fragment = "http://{0}/jmx?qry=Hadoop:service=NameNode,name=*"

    if is_ssl_enabled:
      namenode_http_fragment = 'dfs.namenode.https-address.{0}.{1}'
      jmx_uri_fragment = "https://{0}/jmx?qry=Hadoop:service=NameNode,name=*"

    # now we have something like 'nn1,nn2,nn3,nn4'
    # turn it into dfs.namenode.[property].[dfs.nameservices].[nn_unique_id]
    # ie dfs.namenode.http-address.hacluster.nn1
    namenodes = []
    active_namenodes = []
    nn_unique_ids = hdfs_site[nn_unique_ids_key].split(',')
    for nn_unique_id in nn_unique_ids:
      key = namenode_http_fragment.format(name_service, nn_unique_id)

      if key in hdfs_site:
        # use str() to ensure that unicode strings do not have the u' in them
        value = str(hdfs_site[key])
        namenode = str(hdfs_site[key]).split(":")[0]

        namenodes.append(namenode)
        try:
          jmx_uri = jmx_uri_fragment.format(value)
          if kerberos_principal is not None and kerberos_keytab is not None and security_enabled:
            env = Environment.get_instance()

            # curl requires an integer timeout
            curl_connection_timeout = int(connection_timeout)
            state_response, error_msg, time_millis = curl_krb_request(env.tmp_dir,
              kerberos_keytab, kerberos_principal, jmx_uri,"ha_nn_health", executable_paths, False,
              "NameNode High Availability Health", smokeuser, connection_timeout=curl_connection_timeout,
              kinit_timer_ms = kinit_timer_ms)

            state = _get_ha_state_from_json(state_response)
          else:
            state = _get_state_from_jmx(jmx_uri, connection_timeout)

          if state == HDFS_NN_STATE_ACTIVE:
            active_namenodes.append(namenode)

            # Only check active NN
            nn_service_rpc_address_key = 'dfs.namenode.servicerpc-address.{0}.{1}'.format(name_service, nn_unique_id)
            if nn_service_rpc_address_key in hdfs_site:
              namenode_service_rpc_address = hdfs_site[nn_service_rpc_address_key]
          pass
        except:
          logger.exception("Unable to determine the active NameNode")
    pass

    if merge_ha_metrics:
      hostnames = ",".join(namenodes)
      # run only on active NN, no need to run the same requests from the standby
      if host_name not in active_namenodes:
        return (RESULT_STATE_SKIPPED, ['This alert will be reported by another host.'])
    pass

  # Skip service rpc alert if port is not enabled
  if not namenode_service_rpc_address and 'rpc.rpc.datanode' in metric_name:
    return (RESULT_STATE_SKIPPED, ['Service RPC port is not enabled.'])

  get_metrics_parameters = {
    "metricNames": metric_name,
    "appId": app_id,
    "hostname": hostnames,
    "startTime": current_time - interval * 60 * 1000,
    "endTime": current_time,
    "grouped": "true",
    }

  encoded_get_metrics_parameters = urllib.urlencode(get_metrics_parameters)

  ams_monitor_conf_dir = "/etc/ambari-metrics-monitor/conf"
  metric_truststore_ca_certs='ca.pem'
  ca_certs = os.path.join(ams_monitor_conf_dir,
                          metric_truststore_ca_certs)
  metric_collector_https_enabled = str(configurations[AMS_HTTP_POLICY]) == "HTTPS_ONLY"

  _ssl_version = _get_ssl_version()
  try:
    conn = network.get_http_connection(
      collector_host,
      int(collector_port),
      metric_collector_https_enabled,
      ca_certs,
      ssl_version=_ssl_version
    )
    conn.request("GET", AMS_METRICS_GET_URL % encoded_get_metrics_parameters)
    response = conn.getresponse()
    data = response.read()
    conn.close()
  except Exception, e:
    logger.info(str(e))
    return (RESULT_STATE_UNKNOWN, ["Unable to retrieve metrics from the Ambari Metrics service."])

  if response.status != 200:
    logger.info(str(data))
    return (RESULT_STATE_UNKNOWN, ["Unable to retrieve metrics from the Ambari Metrics service."])

  data_json = json.loads(data)
  metrics = []
  # will get large standard deviation for multiple hosts,
  # if host1 reports small local values, but host2 reports large local values
  for metrics_data in data_json["metrics"]:
    metrics += metrics_data["metrics"].values()
  pass

  if not metrics or len(metrics) < 2:
    number_of_data_points = len(metrics) if metrics else 0
    return (RESULT_STATE_SKIPPED, ["There are not enough data points to calculate the standard deviation ({0} sampled)".format(
      number_of_data_points)])

  minimum_value_multiplier = 1
  if 'dfs.FSNamesystem.CapacityUsed' in metric_name:
    minimum_value_multiplier = 1024 * 1024  # MB to bytes
  elif 'rpc.rpc.datanode' in metric_name or 'rpc.rpc.client' in metric_name:
    minimum_value_multiplier = 1000  # seconds to millis

  if minimum_value_threshold:
    # Filter out points below min threshold
    metrics = [metric for metric in metrics if metric > (minimum_value_threshold * minimum_value_multiplier)]
    if len(metrics) < 2:
      return (RESULT_STATE_OK, ['There were no data points above the minimum threshold of {0} seconds'.format(minimum_value_threshold)])

  mean_value = mean(metrics)
  stddev = sample_standard_deviation(metrics)

  try:
    deviation_percent = stddev / float(mean_value) * 100
  except ZeroDivisionError:
    # should not be a case for this alert
    return (RESULT_STATE_SKIPPED, ["Unable to calculate the standard deviation because the mean value is 0"])

  # log the AMS request
  if logger.isEnabledFor(logging.DEBUG):
    logger.debug("""
    AMS request parameters - {0}
    AMS response - {1}
    Mean - {2}
    Standard deviation - {3}
    Percentage standard deviation - {4}
    """.format(encoded_get_metrics_parameters, data_json, mean_value, stddev, deviation_percent))

  mean_value_localized = locale.format("%.0f", mean_value, grouping=True)

  variance_value = (deviation_percent / 100.0) * mean_value
  variance_value_localized = locale.format("%.0f", variance_value, grouping=True)

  # check for CRITICAL status
  if deviation_percent > critical_threshold:
    threshold_value = ((critical_threshold / 100.0) * mean_value)
    threshold_value_localized = locale.format("%.0f", threshold_value, grouping=True)

    message = DEVIATION_THRESHOLD_MESSAGE.format(variance_value_localized, metric_units, deviation_percent,
      mean_value_localized, metric_units, threshold_value_localized, metric_units)

    return (RESULT_STATE_CRITICAL,[message])

  # check for WARNING status
  if deviation_percent > warning_threshold:
    threshold_value = ((warning_threshold / 100.0) * mean_value)
    threshold_value_localized = locale.format("%.0f", threshold_value, grouping = True)

    message = DEVIATION_THRESHOLD_MESSAGE.format(variance_value_localized, metric_units, deviation_percent,
      mean_value_localized, metric_units, threshold_value_localized, metric_units)

    return (RESULT_STATE_WARNING, [message])

  # return OK status; use the warning threshold as the value to compare against
  threshold_value = ((warning_threshold / 100.0) * mean_value)
  threshold_value_localized = locale.format("%.0f", threshold_value, grouping = True)

  message = DEVIATION_OK_MESSAGE.format(variance_value_localized, metric_units, warning_threshold,
    mean_value_localized, metric_units, threshold_value_localized, metric_units)

  return (RESULT_STATE_OK,[message])


def valid_collector_webapp_address(webapp_address):
  if len(webapp_address) == 2 \
    and webapp_address[0] != '127.0.0.1' \
    and webapp_address[1].isdigit():
    return True

  return False


def _get_state_from_jmx(jmx_uri, connection_timeout):
  state_response = get_jmx(jmx_uri, connection_timeout)
  state = _get_ha_state_from_json(state_response)
  return state


def _get_ssl_version():
  return AmbariConfig.get_resolved_config().get_force_https_protocol_value()


def get_jmx(query, connection_timeout):
  response = None

  try:
    response = urllib2.urlopen(query, timeout=connection_timeout)
    json_data = response.read()
    return json_data
  except Exception:
    return {"beans": {}}
  finally:
    if response is not None:
      try:
        response.close()
      except:
        pass

def _get_ha_state_from_json(string_json):
  """
  Searches through the specified JSON string looking for HA state
  enumerations.
  :param string_json: the string JSON
  :return:  the value of the HA state (active, standby, etc)
  """
  json_data = json.loads(string_json)
  jmx_beans = json_data["beans"]

  # look for NameNodeStatus-State  first
  for jmx_bean in jmx_beans:
    if "name" not in jmx_bean:
      continue

    jmx_bean_name = jmx_bean["name"]
    if jmx_bean_name == "Hadoop:service=NameNode,name=NameNodeStatus" and "State" in jmx_bean:
      return jmx_bean["State"]

  # look for FSNamesystem-tag.HAState last
  for jmx_bean in jmx_beans:
    if "name" not in jmx_bean:
      continue

    jmx_bean_name = jmx_bean["name"]
    if jmx_bean_name == "Hadoop:service=NameNode,name=FSNamesystem":
      return jmx_bean["tag.HAState"]


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

