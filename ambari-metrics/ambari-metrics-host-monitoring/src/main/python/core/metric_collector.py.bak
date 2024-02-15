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
from time import time
from host_info import HostInfo
from event_definition import HostMetricCollectEvent, ProcessMetricCollectEvent

logger = logging.getLogger()

DEFAULT_HOST_APP_ID = '_HOST'

class MetricsCollector():
  """
  The main Reader thread that dequeues events from the event queue and
  submits a metric record to the emit buffer. Implementation of dequeue is
  not required if Timer class is used for metric groups.
  """

  def __init__(self, emit_queue, application_metric_map, host_info):
    self.emit_queue = emit_queue
    self.application_metric_map = application_metric_map
    self.host_info = host_info
  pass

  def process_event(self, event):
    if event.get_classname() == HostMetricCollectEvent.__name__:
      self.process_host_collection_event(event)
    elif event.get_classname() == ProcessMetricCollectEvent.__name__:
      self.process_process_collection_event(event)
    else:
      logger.warn('Unknown event in queue')
    pass

  def process_host_collection_event(self, event):
    startTime = int(round(time() * 1000))
    metrics = None

    if 'cpu' in event.get_group_name():
      metrics = self.host_info.get_cpu_times()

    elif 'disk' in event.get_group_name():
      metrics = self.host_info.get_combined_disk_usage()
      metrics.update(self.host_info.get_combined_disk_io_counters())
      metrics.update(self.host_info.get_disk_io_counters_per_disk())

    elif 'network' in event.get_group_name():
      metrics = self.host_info.get_network_info()

    elif 'mem' in event.get_group_name():
      metrics = self.host_info.get_mem_info()

    elif 'process' in event.get_group_name():
      metrics = self.host_info.get_process_info()

    elif 'all' in event.get_group_name():
      metrics = {}
      metrics.update(self.host_info.get_cpu_times())
      metrics.update(self.host_info.get_combined_disk_usage())
      metrics.update(self.host_info.get_network_info())
      metrics.update(self.host_info.get_mem_info())
      metrics.update(self.host_info.get_process_info())
      metrics.update(self.host_info.get_combined_disk_io_counters())
      metrics.update(self.host_info.get_disk_io_counters_per_disk())

    else:
      logger.warn('Unknown metric group.')
    pass

    if metrics:
      self.application_metric_map.put_metric(DEFAULT_HOST_APP_ID, metrics, startTime)
    pass

  def process_process_collection_event(self, event):
    """
    Collect Process level metrics and update the application metric map
    """
    pass
