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
import threading
from Queue import Queue
from threading import Timer
from application_metric_map import ApplicationMetricMap
from event_definition import HostMetricCollectEvent, ProcessMetricCollectEvent
from metric_collector import MetricsCollector
from emitter import Emitter
from host_info import HostInfo
from aggregator import Aggregator
from aggregator import AggregatorWatchdog


logger = logging.getLogger()

class Controller(threading.Thread):

  def __init__(self, config, stop_handler):
    # Process initialization code
    threading.Thread.__init__(self)
    logger.debug('Initializing Controller thread.')
    self.lock = threading.Lock()
    self.config = config
    self.metrics_config = config.getMetricGroupConfig()
    self.events_cache = []
    hostinfo = HostInfo(config)
    self.application_metric_map = ApplicationMetricMap(hostinfo.get_hostname(),
                                                       hostinfo.get_ip_address())
    self.event_queue = Queue(config.get_max_queue_size())
    self.metric_collector = MetricsCollector(self.event_queue, self.application_metric_map, hostinfo)
    self.sleep_interval = config.get_collector_sleep_interval()
    self._stop_handler = stop_handler
    self.initialize_events_cache()
    self.emitter = Emitter(self.config, self.application_metric_map, stop_handler)
    self._t = None
    self.aggregator = None
    self.aggregator_watchdog = None

  def run(self):
    logger.info('Running Controller thread: %s' % threading.currentThread().getName())

    self.start_emitter()
    if self.config.is_inmemory_aggregation_enabled():
      self.start_aggregator_with_watchdog()

    # Wake every 5 seconds to push events to the queue
    while True:
      if (self.event_queue.full()):
        logger.warn('Event Queue full!! Suspending further collections.')
      else:
        self.enqueque_events()
      # restart aggregator if needed
      if self.config.is_inmemory_aggregation_enabled() and not self.aggregator_watchdog.is_ok():
        logger.warning("Aggregator is not available. Restarting aggregator.")
        self.start_aggregator_with_watchdog()
      pass
      # Wait for the service stop event instead of sleeping blindly
      if 0 == self._stop_handler.wait(self.sleep_interval):
        logger.info('Shutting down Controller thread')
        break

    if not self._t is None:
      self._t.cancel()
      self._t.join(5)

    # The emitter thread should have stopped by now, just ensure it has shut
    # down properly
    self.emitter.join(5)

    if self.config.is_inmemory_aggregation_enabled():
      self.aggregator.stop()
      self.aggregator_watchdog.stop()
      self.aggregator.join(5)
      self.aggregator_watchdog.join(5)
    pass

  # TODO: Optimize to not use Timer class and use the Queue instead
  def enqueque_events(self):
    # Queue events for up to a minute
    for event in self.events_cache:
      self._t = Timer(event.get_collect_interval(), self.metric_collector.process_event, args=(event,))
      self._t.start()
    pass

  def initialize_events_cache(self):
    self.events_cache = []
    try:
      host_metrics_groups = self.metrics_config['host_metric_groups']
      process_metrics_groups = self.metrics_config['process_metric_groups']
    except KeyError, ke:
      logger.warn('Error loading metric groups.')
      raise ke
    pass

    if host_metrics_groups:
      for name, properties in host_metrics_groups.iteritems():
        event = HostMetricCollectEvent(properties, name)
        logger.info('Adding event to cache, {0} : {1}'.format(name, properties))
        self.events_cache.append(event)
      pass
    pass

    # if process_metrics_groups:
    #   for name, properties in process_metrics_groups.iteritems():
    #     event = ProcessMetricCollectEvent(properties, name)
    #     logger.info('Adding event to cache, {0} : {1}'.format(name, properties))
    #     #self.events_cache.append(event)
    #   pass
    # pass

  pass

  def start_emitter(self):
    self.emitter.start()

  # Start aggregator and watcher threads
  def start_aggregator_with_watchdog(self):
    if self.aggregator:
      self.aggregator.stop()
    if self.aggregator_watchdog:
      self.aggregator_watchdog.stop()
    self.aggregator = Aggregator(self.config, self._stop_handler)
    self.aggregator_watchdog = AggregatorWatchdog(self.config, self._stop_handler)
    self.aggregator.start()
    self.aggregator_watchdog.start()
