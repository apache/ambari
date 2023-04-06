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

import logging

from resource_management.libraries.functions.simulate_perf_cluster_alert_behaviour import simulate_perf_cluster_alert_behaviour

ALERT_BEHAVIOUR_TYPE = "{{zk-alert-config/alert.behavior.type}}"

ALERT_SUCCESS_PERCENTAGE = "{{zk-alert-config/alert.success.percentage}}"

ALERT_TIMEOUT_RETURN_VALUE = "{{zk-alert-config/alert.timeout.return.value}}"
ALERT_TIMEOUT_SECS = "{{zk-alert-config/alert.timeout.secs}}"

ALERT_FLIP_INTERVAL_MINS = "{{zk-alert-config/alert.flip.interval.mins}}"

logger = logging.getLogger('ambari_alerts')

alert_behaviour_properties = {"alert_behaviour_type" : ALERT_BEHAVIOUR_TYPE, "alert_success_percentage" : ALERT_SUCCESS_PERCENTAGE,
                              "alert_timeout_return_value" : ALERT_TIMEOUT_RETURN_VALUE, "alert_timeout_secs" : ALERT_TIMEOUT_SECS,
                              "alert_flip_interval_mins" : ALERT_FLIP_INTERVAL_MINS}

def get_tokens():
  """
  Returns a tuple of tokens in the format {{site/property}} that will be used
  to build the dictionary passed into execute
  """
  return (ALERT_BEHAVIOUR_TYPE, ALERT_SUCCESS_PERCENTAGE, ALERT_TIMEOUT_RETURN_VALUE, ALERT_TIMEOUT_SECS,
          ALERT_FLIP_INTERVAL_MINS)


def execute(configurations={}, parameters={}, host_name=None):
  """
  Returns a tuple containing the result code and a pre-formatted result label

  Keyword arguments:
  configurations (dictionary): a mapping of configuration key to value
  parameters (dictionary): a mapping of script parameter key to value
  host_name (string): the name of this host where the alert is running
  """

  return simulate_perf_cluster_alert_behaviour(alert_behaviour_properties, configurations)