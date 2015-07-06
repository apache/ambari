#!/usr/bin/env python

'''
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
'''

import logging
import json
from threading import RLock

logger = logging.getLogger()

class ApplicationMetricMap:
  """
  A data structure to buffer metrics in memory.
  The in-memory dict stores metrics as shown below:
  { application_id : { metric_id : { timestamp :  metric_value } } }
  application_id => uniquely identify the metrics for an application / host.
  metric_id      => identify the metric
  timestamp      => collection time
  metric_value   => numeric value
  """


  def __init__(self, hostname, ip_address):
    self.hostname = hostname
    self.ip_address = ip_address
    self.lock = RLock()
    self.app_metric_map = {}
  pass

  def put_metric(self, application_id, metric_id_to_value_map, timestamp):
    with self.lock:
      for metric_name, value in metric_id_to_value_map.iteritems():
      
        metric_map = self.app_metric_map.get(application_id)
        if not metric_map:
          metric_map = { metric_name : { timestamp : value } }
          self.app_metric_map[ application_id ] = metric_map
        else:
          metric_id_map = metric_map.get(metric_name)
          if not metric_id_map:
            metric_id_map = { timestamp : value }
            metric_map[ metric_name ] = metric_id_map
          else:
            metric_map[ metric_name ].update( { timestamp : value } )
          pass
        pass
  pass

  def delete_application_metrics(self, app_id):
    del self.app_metric_map[ app_id ]
  pass

  def flatten(self, application_id = None, clear_once_flattened = False):
    """
    Return flatten dict to caller in json format.
    Json format:
    {"metrics":[{"hostname":"a","metricname":"b","appid":"c",
    "instanceid":"d","starttime":"e","metrics":{"t":"v"}}]}
    """
    with self.lock:
      timeline_metrics = { "metrics" : [] }
      local_metric_map = {}
  
      if application_id:
        if self.app_metric_map.has_key(application_id):
          local_metric_map = { application_id : self.app_metric_map[application_id] }
        else:
          logger.info("application_id: {0}, not present in the map.".format(application_id))
      else:
        local_metric_map = self.app_metric_map.copy()
      pass
  
      for appId, metrics in local_metric_map.iteritems():
        for metricId, metricData in dict(metrics).iteritems():
          # Create a timeline metric object
          timeline_metric = {
            "hostname" : self.hostname,
            "metricname" : metricId,
            "appid" : "HOST",
            "instanceid" : "",
            "starttime" : self.get_start_time(appId, metricId),
            "metrics" : metricData
          }
          timeline_metrics[ "metrics" ].append( timeline_metric )
        pass
      pass
      json_data = json.dumps(timeline_metrics) if len(timeline_metrics[ "metrics" ]) > 0 else None

      if clear_once_flattened:
        self.app_metric_map.clear()
      pass

      return json_data
  pass

  def get_start_time(self, app_id, metric_id):
    with self.lock:
      if self.app_metric_map.has_key(app_id):
        if self.app_metric_map.get(app_id).has_key(metric_id):
          metrics = self.app_metric_map.get(app_id).get(metric_id)
          return min(metrics.iterkeys())
  pass

  def format_app_id(self, app_id, instance_id = None):
    return app_id + "_" + instance_id if instance_id else app_id
  pass

  def get_app_id(self, app_id):
    return app_id.split("_")[0]
  pass

  def get_instance_id(self, app_id):
    parts = app_id.split("_")
    return parts[1] if len(parts) > 1 else ''
  pass

  def clear(self):
    with self.lock:
      self.app_metric_map.clear()
  pass
