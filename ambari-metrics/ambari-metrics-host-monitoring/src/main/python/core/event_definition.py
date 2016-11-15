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

DEFAULT_COLLECT_INTERVAL = 10

logger = logging.getLogger()

class Event:
  def __init__(self):
    self._classname = self.__class__.__name__

  def get_classname(self):
    return self._classname

  def get_collect_interval(self):
    return DEFAULT_COLLECT_INTERVAL


class EmmitEvent(Event):

  def __init__(self, application_metric_map):
    Event.__init__(self)
    self.application_metric_map = application_metric_map

  def get_emmit_payload(self):
    return self.application_metric_map.flatten()


class HostMetricCollectEvent(Event):

  def __init__(self, group_config, group_name):
    Event.__init__(self)
    self.group_config = group_config
    self.group_name = group_name
    try:
      self.group_interval = group_config['collect_every']
      self.metrics = group_config['metrics']
    except KeyError, ex:
      logger.warn('Unable to create event from metric group. {0}'.format(
        group_config))
      raise ex

  def get_metric_value_thresholds(self):
    metric_value_thresholds = {}

    for metric in self.metrics:
      try:
        metric_value_thresholds[metric['name']] = metric['value_threshold']
      except:
        logger.warn('Error parsing metric configuration. {0}'.format(metric))
    pass

    return metric_value_thresholds

  def get_group_name(self):
    return self.group_name

  def get_collect_interval(self):
    return int(self.group_interval if self.group_interval else DEFAULT_COLLECT_INTERVAL)

class ProcessMetricCollectEvent:

  def __init__(self, group_config, group_name):
    # Initialize the Process metric event
    pass
