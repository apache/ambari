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

import imp
import json
import logging
import re
import urllib2
import uuid
from alerts.base_alert import BaseAlert
from resource_management.libraries.functions.get_port_from_url import get_port_from_url

logger = logging.getLogger()

class MetricAlert(BaseAlert):
  
  def __init__(self, alert_meta, alert_source_meta):
    super(MetricAlert, self).__init__(alert_meta, alert_source_meta)
 
    self.metric_info = None    
    if 'jmx' in alert_source_meta:
      self.metric_info = JmxMetric(alert_source_meta['jmx'])

    # extract any lookup keys from the URI structure
    self.uri_property_keys = self._lookup_uri_property_keys(alert_source_meta['uri'])
      
  def _collect(self):
    if self.metric_info is None:
      raise Exception("Could not determine result. Specific metric collector is not defined.")
    
    if self.uri_property_keys is None:
      raise Exception("Could not determine result. URL(s) were not defined.")

    # use the URI lookup keys to get a final URI value to query
    alert_uri = self._get_uri_from_structure(self.uri_property_keys)      
    
    logger.debug("Calculated metric URI to be {0} (ssl={1})".format(alert_uri.uri, 
        str(alert_uri.is_ssl_enabled)))

    host = BaseAlert.get_host_from_url(alert_uri.uri)
    if host is None:
      host = self.host_name

    port = 80 # probably not very realistic
    try:      
      port = int(get_port_from_url(alert_uri.uri))
    except:
      pass

    collect_result = None
    check_value = None
    value_list = []

    if isinstance(self.metric_info, JmxMetric):
      value_list.extend(self._load_jmx(alert_uri.is_ssl_enabled, host, port, self.metric_info))
      check_value = self.metric_info.calculate(value_list)
      value_list.append(check_value)
      
      collect_result = self.__get_result(value_list[0] if check_value is None else check_value)

    logger.debug("Resolved value list is: {0}".format(str(value_list)))
    
    return ((collect_result, value_list))

  
  def __get_result(self, value):
    ok_value = self.__find_threshold('ok')
    warn_value = self.__find_threshold('warning')
    crit_value = self.__find_threshold('critical')
    
    # critical values are higher
    crit_direction_up = crit_value >= warn_value
    
    if crit_direction_up: 
      # critcal values are higher
      if value > crit_value:
        return self.RESULT_CRITICAL
      elif value > warn_value:
        return self.RESULT_WARNING
      else:
        if ok_value is not None:
          if value > ok_value and value <= warn_value:
            return self.RESULT_OK
          else:
            return self.RESULT_UNKNOWN
        else:
          return self.RESULT_OK
    else:
      # critical values are lower
      if value < crit_value:
        return self.RESULT_CRITICAL
      elif value < warn_value:
        return self.RESULT_WARNING
      else:
        if ok_value is not None:
          if value < ok_value and value >= warn_value:
            return self.RESULT_OK
          else:
            return self.RESULT_UNKNOWN
        else:
          return self.RESULT_OK

    return None

    
  def __find_threshold(self, reporting_type):
    """ find the defined thresholds for alert values """
    
    if not 'reporting' in self.alert_source_meta:
      return None
      
    if not reporting_type in self.alert_source_meta['reporting']:
      return None
      
    if not 'value' in self.alert_source_meta['reporting'][reporting_type]:
      return None
      
    return self.alert_source_meta['reporting'][reporting_type]['value']

    
  def _load_jmx(self, ssl, host, port, jmx_metric):
    """ creates a JmxMetric object that holds info about jmx-based metrics """
    
    logger.debug(str(jmx_metric.property_map))
    
    value_list = []

    for k, v in jmx_metric.property_map.iteritems():
      url = "{0}://{1}:{2}/jmx?qry={3}".format(
        "https" if ssl else "http", host, str(port), k)
        
      response = urllib2.urlopen(url)
      json_response = json.loads(response.read())
      json_data = json_response['beans'][0]
      
      for attr in v:
        value_list.append(json_data[attr])
        
    return value_list

    
class JmxMetric:
  def __init__(self, jmx_info):
    self.custom_module = None
    self.property_list = jmx_info['property_list']
    self.property_map = {}
    
    if 'value' in jmx_info:
      realcode = re.sub('(\{(\d+)\})', 'args[\g<2>]', jmx_info['value'])
      
      self.custom_module =  imp.new_module(str(uuid.uuid4()))
      code = 'def f(args):\n  return ' + realcode
      exec code in self.custom_module.__dict__
    
    for p in self.property_list:
      parts = p.split('/')
      if not parts[0] in self.property_map:
        self.property_map[parts[0]] = []
      self.property_map[parts[0]].append(parts[1])

      
  def calculate(self, args):
    if self.custom_module is not None:
      return self.custom_module.f(args)
    return None
    
      
    
  
    
